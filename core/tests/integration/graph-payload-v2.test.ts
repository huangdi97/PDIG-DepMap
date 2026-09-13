import { describe, expect, it, beforeEach, afterEach } from 'vitest'
import { mkdtempSync, rmSync, readFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'
import { NodeSqliteDriver } from '../../src/db/node-driver.ts'
import {
  migrate,
  LEGACY_WECHAT_SOURCE_INSTANCE_ID,
  SCHEMA_VERSION,
} from '../../src/schema/migrations.ts'
import { NodeRepository } from '../../src/repositories/node-repository.ts'
import { ImportFlow } from '../../src/services/import-pipeline.ts'
import { ConfirmationService } from '../../src/services/confirmation-service.ts'
import {
  exportGraph,
  importGraph,
  migratePayloadV1toV2,
  checkGraphIntegrity,
  GraphImportError,
  GRAPH_PAYLOAD_KIND,
  GRAPH_PAYLOAD_VERSION,
} from '../../src/services/graph-serialize.ts'

/**
 * MVP02 J 段 —— `.depmap` payload v2 export/import + v1 payload in-memory migrate。
 *
 * 覆盖 GOAL §15：
 * - v2 往返深度等价（幂等：import 后可再次 import 得到同一 payload）
 * - v1 payload 在**不触碰 DB** 的前提下 in-memory 迁移到 v2
 * - 不支持的 schemaVersion / payloadVersion / payloadKind 明确拒绝
 * - 迁移失败时目标 DB 保持原样（绝不半写入）
 * - crypto 容器 formatVersion（V1）与 payload schemaVersion（2）互相独立
 */

const FX = (name: string) =>
  new Uint8Array(
    readFileSync(join(dirname(fileURLToPath(import.meta.url)), '..', 'fixtures', name)),
  )

interface V1PayloadOpts {
  withEvidence?: boolean
  withFingerprints?: boolean
}

/**
 * 用真实 v2 export 反推一份 v1 payload：删除所有 v2 独有字段，
 * 还原 v1 的列形状（evidence_id 内联、无 source_instance_id、payloadVersion=1）。
 * 这样迁移测试验证的是「真实历史文件」而非凭空捏造的结构。
 */
function downgradeToV1(v2Json: string, opts: V1PayloadOpts = {}): string {
  const v2 = JSON.parse(v2Json) as Record<string, unknown>
  const v1: Record<string, unknown> = {
    payloadKind: v2['payloadKind'],
    payloadVersion: 1,
    // v1 无 schemaVersion 概念
  }

  // nodes / dependency_groups / dependencies / dependency_group_proposals 形状兼容
  for (const t of [
    'nodes',
    'dependency_groups',
    'dependencies',
    'dependency_group_proposals',
    'meta',
  ]) {
    if (t in v2) v1[t] = v2[t]
  }

  // dependency_proposals：v1 内联 evidence_id，且无 rejected_at_stream_counts_json
  v1['dependency_proposals'] = (
    (v2['dependency_proposals'] as Array<Record<string, unknown>>) ?? []
  ).map((r) => {
    const out: Record<string, unknown> = { ...r }
    delete out['rejected_at_stream_counts_json']
    // v1 用 evidence 表里的单条 evidence_id 表示来源；取该 proposal 的首条 ref
    const refs = (v2['proposal_evidence_refs'] as Array<Record<string, unknown>>) ?? []
    const mine = refs.filter((x) => x['proposal_key'] === r['key'])
    out['evidence_id'] = mine.length > 0 ? mine[0]!['evidence_id'] : null
    return out
  })

  // evidence：v1 无 source_instance_id / adapter_id / adapter_version / evidence_kind
  if (opts.withEvidence !== false) {
    v1['evidence'] = ((v2['evidence'] as Array<Record<string, unknown>>) ?? []).map((r) => {
      const out: Record<string, unknown> = { ...r }
      delete out['source_instance_id']
      delete out['adapter_id']
      delete out['adapter_version']
      delete out['evidence_kind']
      return out
    })
  } else {
    v1['evidence'] = []
  }

  // fingerprints：v1 无 source_instance_id
  if (opts.withFingerprints !== false) {
    v1['observation_fingerprints'] = (
      (v2['observation_fingerprints'] as Array<Record<string, unknown>>) ?? []
    ).map((r) => {
      const out: Record<string, unknown> = { ...r }
      delete out['source_instance_id']
      return out
    })
  } else {
    v1['observation_fingerprints'] = []
  }

  // import_sessions：v1 无 source_instance_id / adapter_id / adapter_version
  v1['import_sessions'] = ((v2['import_sessions'] as Array<Record<string, unknown>>) ?? []).map(
    (r) => {
      const out: Record<string, unknown> = { ...r }
      delete out['source_instance_id']
      delete out['adapter_id']
      delete out['adapter_version']
      return out
    },
  )

  // v1 没有 source_instances 表
  return JSON.stringify(v1)
}

describe('graph payload v2 / v1 migration (MVP02 J)', () => {
  let dir: string
  let driver: NodeSqliteDriver

  beforeEach(() => {
    dir = mkdtempSync(join(tmpdir(), 'depmap-payload-'))
    driver = new NodeSqliteDriver(join(dir, 'test.db'))
    driver.open()
    migrate(driver)
  })

  afterEach(() => {
    driver.close()
    rmSync(dir, { recursive: true, force: true })
  })

  /** 构造一个含真实导入、确认、Group 的图，返回值便于断言。 */
  async function seedRealGraph(source: NodeSqliteDriver) {
    const n = new NodeRepository(source)
    const confirm = new ConfirmationService(source)
    const flow = new ImportFlow(source)
    n.create({
      kind: 'payment_instrument',
      name: '招行经典白',
      issuer: '招商银行',
      last4: '4417',
    })
    const tencent = n.create({ kind: 'service', name: '腾讯视频' })
    await flow.begin(FX('recurring-monthly.csv'))
    flow.resolveMerchant('腾讯视频', tencent.id)
    const outcome = await flow.finalize()
    for (const k of outcome.proposalKeys) confirm.acceptProposal(k)
    const wechat = n.list({ kind: 'account' })[0]
    if (!wechat) throw new Error('wechat account node missing')
    const ccb = n.create({
      kind: 'payment_instrument',
      name: '建行龙卡',
      issuer: '建设银行',
      last4: '8821',
    })
    confirm.addManualDependency({
      from: ccb.id,
      relation: 'funding_source',
      to: wechat.id,
      capability: 'payment',
    })
    for (const k of confirm.detectGroupProposals()) confirm.acceptGroupProposal(k)
    return { tencent, wechat, ccb }
  }

  // -------------------------------------------------------------------------
  // J0 —— payload 头字段
  // -------------------------------------------------------------------------

  it('J0: export payload 含 payloadKind / payloadVersion=2 / schemaVersion=SCHEMA_VERSION', () => {
    const out = exportGraph(driver)
    const p = JSON.parse(out.payloadJson) as Record<string, unknown>
    expect(p['payloadKind']).toBe(GRAPH_PAYLOAD_KIND)
    expect(p['payloadVersion']).toBe(2)
    expect(p['schemaVersion']).toBe(SCHEMA_VERSION)
    expect(p['schemaVersion']).toBe(3)
    // 容器 formatVersion 与 payload schemaVersion 是两套独立版本号
    expect(GRAPH_PAYLOAD_VERSION).toBe(2)
  })

  it('J0b: payload 包含全部 v2 表集合（含 source_instances / proposal_evidence_refs）', () => {
    const out = exportGraph(driver)
    const p = JSON.parse(out.payloadJson) as Record<string, unknown>
    for (const t of [
      'meta',
      'nodes',
      'dependencies',
      'dependency_groups',
      'dependency_proposals',
      'dependency_group_proposals',
      'proposal_evidence_refs',
      'evidence',
      'observation_fingerprints',
      'import_sessions',
      'source_instances',
    ]) {
      expect(p, `payload 必须包含表 ${t}`).toHaveProperty(t)
    }
  })

  // -------------------------------------------------------------------------
  // J1 —— v2 往返与幂等
  // -------------------------------------------------------------------------

  it('J1: export → import 到新库 → export 逐字节相同（深度等价）', async () => {
    await seedRealGraph(driver)
    const e1 = exportGraph(driver)
    expect(e1.counts['dependencies']).toBeGreaterThan(0)

    const d2 = new NodeSqliteDriver(join(dir, 'r1.db'))
    d2.open()
    migrate(d2)
    try {
      importGraph(d2, e1.payloadJson)
      const e2 = exportGraph(d2)
      expect(e2.payloadJson).toBe(e1.payloadJson)
      expect(e2.counts).toEqual(e1.counts)
    } finally {
      d2.close()
    }
  })

  it('J1b: import 后再次 import 同一 payload → 幂等（逐字节不变）', async () => {
    await seedRealGraph(driver)
    const e1 = exportGraph(driver)

    const d2 = new NodeSqliteDriver(join(dir, 'r2.db'))
    d2.open()
    migrate(d2)
    try {
      importGraph(d2, e1.payloadJson)
      const e2 = exportGraph(d2)
      // 第二次导入同一 payload 到已导入的库
      importGraph(d2, e1.payloadJson)
      const e3 = exportGraph(d2)
      expect(e3.payloadJson).toBe(e2.payloadJson)
      expect(e3.payloadJson).toBe(e1.payloadJson)
    } finally {
      d2.close()
    }
  })

  it('J1c: 恢复到新库后图完整性无孤儿（SourceInstance refs 全部有效）', async () => {
    await seedRealGraph(driver)
    const e1 = exportGraph(driver)
    const d2 = new NodeSqliteDriver(join(dir, 'r3.db'))
    d2.open()
    migrate(d2)
    try {
      importGraph(d2, e1.payloadJson)
      const orphan = checkGraphIntegrity(d2)
      expect(orphan.orphanDependencies).toHaveLength(0)
      expect(orphan.orphanGroups).toHaveLength(0)
      expect(orphan.danglingGroupMembers).toHaveLength(0)
      expect(orphan.orphanEvidence).toHaveLength(0)
      expect(orphan.orphanFingerprints).toHaveLength(0)
    } finally {
      d2.close()
    }
  })

  // -------------------------------------------------------------------------
  // J2 —— v1 payload in-memory migrate
  // -------------------------------------------------------------------------

  it('J2: v1 payload 迁移到 v2 —— 版本号提升且生成 legacy SourceInstance 归属', async () => {
    await seedRealGraph(driver)
    const v1Json = downgradeToV1(exportGraph(driver).payloadJson)

    const migrated = migratePayloadV1toV2(v1Json)
    const m = JSON.parse(migrated) as Record<string, unknown>
    expect(m['payloadVersion']).toBe(2)
    expect(m['schemaVersion']).toBe(SCHEMA_VERSION)

    // legacy 实例被生成
    const instances = m['source_instances'] as Array<Record<string, unknown>>
    expect(instances).toHaveLength(1)
    expect(instances[0]!['id']).toBe(LEGACY_WECHAT_SOURCE_INSTANCE_ID)
    expect(instances[0]!['adapter_id']).toBe('wechat_statement')
    expect(instances[0]!['source_kind']).toBe('statement_file')

    // 所有 evidence / fingerprint / session 归属 legacy 实例
    for (const e of m['evidence'] as Array<Record<string, unknown>>) {
      expect(e['source_instance_id']).toBe(LEGACY_WECHAT_SOURCE_INSTANCE_ID)
      expect(e['evidence_kind']).toBe('transaction_stream')
      expect(e['adapter_id']).toBe('wechat_statement')
    }
    for (const f of m['observation_fingerprints'] as Array<Record<string, unknown>>) {
      expect(f['source_instance_id']).toBe(LEGACY_WECHAT_SOURCE_INSTANCE_ID)
    }
    for (const s of m['import_sessions'] as Array<Record<string, unknown>>) {
      expect(s['source_instance_id']).toBe(LEGACY_WECHAT_SOURCE_INSTANCE_ID)
    }
  })

  it('J2b: v1 的 evidence_id 内联被转换为 proposal_evidence_refs', async () => {
    await seedRealGraph(driver)
    const v2 = JSON.parse(exportGraph(driver).payloadJson) as Record<string, unknown>
    // 前置条件：v2 中存在 evidence refs，v1 中应为内联 evidence_id
    expect((v2['proposal_evidence_refs'] as unknown[]).length).toBeGreaterThan(0)

    const v1Json = downgradeToV1(JSON.stringify(v2))
    const v1 = JSON.parse(v1Json) as Record<string, unknown>
    const v1Proposals = v1['dependency_proposals'] as Array<Record<string, unknown>>
    expect(v1Proposals.some((p) => p['evidence_id'] !== null)).toBe(true)
    expect(v1).not.toHaveProperty('proposal_evidence_refs')

    const m = JSON.parse(migratePayloadV1toV2(v1Json)) as Record<string, unknown>
    const refs = m['proposal_evidence_refs'] as Array<Record<string, unknown>>
    expect(refs.length).toBeGreaterThan(0)
    for (const r of refs) {
      expect(r['proposal_key']).toBeTruthy()
      expect(r['evidence_id']).toBeTruthy()
    }
    // v1 的内联字段被清除，不再残留
    for (const p of m['dependency_proposals'] as Array<Record<string, unknown>>) {
      expect(p).not.toHaveProperty('evidence_id')
      // 空快照：legacy 单流计数以 evidence 流计数为准
      expect(p).toHaveProperty('rejected_at_stream_counts_json')
    }
  })

  it('J2c: 迁移后的 payload 可被 importGraph 直接消费（端到端真正落地）', async () => {
    await seedRealGraph(driver)
    const v1Json = downgradeToV1(exportGraph(driver).payloadJson)

    const d2 = new NodeSqliteDriver(join(dir, 'v1restore.db'))
    d2.open()
    migrate(d2)
    try {
      // importGraph 内部自动识别 payloadVersion=1 并先迁移
      const result = importGraph(d2, v1Json)
      expect(result.imported['nodes']).toBeGreaterThan(0)
      expect(result.imported['dependencies']).toBeGreaterThan(0)

      // 迁移结果落到 v2 形状
      const e2 = exportGraph(d2)
      const p2 = JSON.parse(e2.payloadJson) as Record<string, unknown>
      expect(p2['payloadVersion']).toBe(2)
      expect(p2['schemaVersion']).toBe(SCHEMA_VERSION)

      // legacy 实例存在且被引用
      const inst = p2['source_instances'] as Array<Record<string, unknown>>
      expect(inst.map((i) => i['id'])).toContain(LEGACY_WECHAT_SOURCE_INSTANCE_ID)
      expect(checkGraphIntegrity(d2).orphanEvidence).toHaveLength(0)
      expect(checkGraphIntegrity(d2).orphanFingerprints).toHaveLength(0)
    } finally {
      d2.close()
    }
  })

  it('J2d: 迁移是纯内存操作 —— 不修改传入字符串，也不触碰任何 DB', () => {
    const v1Json = JSON.stringify({
      payloadKind: GRAPH_PAYLOAD_KIND,
      payloadVersion: 1,
      meta: [],
      nodes: [],
      dependencies: [],
      dependency_groups: [],
      dependency_proposals: [],
      dependency_group_proposals: [],
      evidence: [],
      observation_fingerprints: [],
      import_sessions: [],
    })
    const before = v1Json
    const out1 = migratePayloadV1toV2(v1Json)
    const out2 = migratePayloadV1toV2(v1Json)
    expect(v1Json).toBe(before) // 入参未被改动
    expect(out1).toBe(out2) // deterministic
    // 空图迁移仍生成 legacy 实例（保证 FK 完整性）
    const m = JSON.parse(out1) as Record<string, unknown>
    expect((m['source_instances'] as unknown[]).length).toBe(1)
  })

  // -------------------------------------------------------------------------
  // J3 —— 拒绝策略
  // -------------------------------------------------------------------------

  it('J3: 不支持的 payloadVersion 被拒绝，且目标 DB 保持原样', async () => {
    await seedRealGraph(driver)
    const good = exportGraph(driver)
    const before = good.payloadJson

    for (const bad of [0, 3, 99]) {
      const json = JSON.stringify({ ...JSON.parse(good.payloadJson), payloadVersion: bad })
      expect(() => importGraph(driver, json)).toThrow(GraphImportError)
      // DB 未被触碰
      expect(exportGraph(driver).payloadJson).toBe(before)
    }
  })

  it('J3b: schemaVersion 高于支持版本被拒绝（向前不兼容的库不被接受）', async () => {
    await seedRealGraph(driver)
    const good = exportGraph(driver)
    const before = good.payloadJson
    const json = JSON.stringify({
      ...JSON.parse(good.payloadJson),
      schemaVersion: SCHEMA_VERSION + 1,
    })
    expect(() => importGraph(driver, json)).toThrow(/newer than supported/)
    expect(exportGraph(driver).payloadJson).toBe(before)
  })

  it('J3c: payloadKind 不匹配被拒绝；非 JSON 被拒绝', async () => {
    await seedRealGraph(driver)
    const before = exportGraph(driver).payloadJson

    const wrongKind = JSON.stringify({
      ...JSON.parse(before),
      payloadKind: 'some-other-app',
    })
    expect(() => importGraph(driver, wrongKind)).toThrow(GraphImportError)

    expect(() => importGraph(driver, 'not json at all')).toThrow(/not valid JSON/)
    expect(() => migratePayloadV1toV2('{')).toThrow(/not valid JSON/)
    expect(() => migratePayloadV1toV2('[]')).toThrow(/must be an object/)
    expect(() => migratePayloadV1toV2('null')).toThrow(/must be an object/)
    expect(exportGraph(driver).payloadJson).toBe(before)
  })

  it('J3d: migratePayloadV1toV2 只接受 payloadVersion=1（v2 输入立即报错）', async () => {
    await seedRealGraph(driver)
    const v2 = exportGraph(driver).payloadJson
    expect(() => migratePayloadV1toV2(v2)).toThrow(/expects payloadVersion 1/)
  })

  it('J3e: schemaVersion 非整数被拒绝', async () => {
    await seedRealGraph(driver)
    const good = exportGraph(driver)
    const before = good.payloadJson
    for (const bad of ['2', 2.5, null, true]) {
      const json = JSON.stringify({ ...JSON.parse(good.payloadJson), schemaVersion: bad })
      expect(() => importGraph(driver, json)).toThrow(GraphImportError)
    }
    expect(exportGraph(driver).payloadJson).toBe(before)
  })

  // -------------------------------------------------------------------------
  // J4 —— 失败原子性
  // -------------------------------------------------------------------------

  it('J4: 缺失必需表 → 导入失败，DB 保持原样（原子替换）', async () => {
    await seedRealGraph(driver)
    const before = exportGraph(driver).payloadJson
    const broken = JSON.parse(before) as Record<string, unknown>
    delete broken['dependencies']
    expect(() => importGraph(driver, JSON.stringify(broken))).toThrow(/must be an array/)
    expect(exportGraph(driver).payloadJson).toBe(before)
  })

  it('J4b: 表中某行缺列 → 导入失败且不半写入', async () => {
    await seedRealGraph(driver)
    const before = exportGraph(driver).payloadJson
    const broken = JSON.parse(before) as Record<string, unknown>
    const deps = broken['dependencies'] as Array<Record<string, unknown>>
    if (deps.length === 0) throw new Error('fixture requires at least one dependency')
    delete deps[0]!['relation']
    expect(() => importGraph(driver, JSON.stringify(broken))).toThrow(/missing column/)
    expect(exportGraph(driver).payloadJson).toBe(before)
  })

  it('J4c: v1 payload 缺表 → 迁移后校验同样拒绝，DB 保持原样', async () => {
    await seedRealGraph(driver)
    const before = exportGraph(driver).payloadJson
    const v1 = JSON.parse(downgradeToV1(before)) as Record<string, unknown>
    delete v1['nodes']
    expect(() => importGraph(driver, JSON.stringify(v1))).toThrow(/must be an array/)
    expect(exportGraph(driver).payloadJson).toBe(before)
  })
})
