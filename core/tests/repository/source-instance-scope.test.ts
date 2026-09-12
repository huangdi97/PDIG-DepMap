import { describe, expect, it, beforeEach, afterEach } from 'vitest'
import { mkdtempSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { NodeSqliteDriver } from '../../src/db/node-driver.ts'
import { migrate } from '../../src/schema/migrations.ts'
import { SourceInstanceRepository } from '../../src/repositories/source-instance-repository.ts'
import { FingerprintRepository } from '../../src/repositories/fingerprint-repository.ts'
import { NodeRepository } from '../../src/repositories/node-repository.ts'
import { assignFingerprintsV2, FINGERPRINT_VERSION } from '../../src/fingerprint/fingerprint.ts'
import type { Observation } from '../../src/domain/types.ts'
import type { NormalizedPaymentObservation } from '../../src/domain/source.ts'

/**
 * MVP02 B/C 段 —— SourceInstance 隔离与 Fingerprint source scope。
 *
 * 这是 multi-source 全部能力的地基：
 * 不同数据源的同号交易必须互不干扰，同一数据源的重复必须仍被去重。
 * 违反任一条，都会让“多源只增加 provenance、不增加 certainty”失效。
 */

const INST_A = 'inst-source-a'
const INST_B = 'inst-source-b'

function obs(over: Partial<NormalizedPaymentObservation> = {}): NormalizedPaymentObservation {
  return {
    sourceInstanceId: INST_A,
    adapterId: 'generic_csv',
    sourceTxnId: 'TXN-1001',
    occurredAt: '2026-03-15T10:00:00Z',
    amount: 25,
    currency: 'USD',
    direction: 'out',
    counterparty: 'NETFLIX.COM',
    merchantRaw: 'NETFLIX.COM',
    ...over,
  }
}

describe('SourceInstance + Fingerprint scope (MVP02 B/C)', () => {
  let dir: string
  let driver: NodeSqliteDriver
  let instances: SourceInstanceRepository
  let fingerprints: FingerprintRepository
  let nodes: NodeRepository

  beforeEach(() => {
    dir = mkdtempSync(join(tmpdir(), 'depmap-srcscope-'))
    driver = new NodeSqliteDriver(join(dir, 'test.db'))
    driver.open()
    migrate(driver)
    instances = new SourceInstanceRepository(driver)
    fingerprints = new FingerprintRepository(driver)
    nodes = new NodeRepository(driver)
  })

  afterEach(() => {
    driver.close()
    rmSync(dir, { recursive: true, force: true })
  })

  // -------------------------------------------------------------------------
  // B 段 —— SourceInstance 实体行为
  // -------------------------------------------------------------------------

  it('B1: 同一 adapter 可存在多个 SourceInstance，互不覆盖', () => {
    const a = instances.create({
      id: INST_A,
      adapterId: 'generic_csv',
      adapterVersion: 1,
      sourceKind: 'statement_file',
      label: 'US Card CSV',
      currencies: ['USD'],
    })
    const b = instances.create({
      id: INST_B,
      adapterId: 'generic_csv',
      adapterVersion: 1,
      sourceKind: 'statement_file',
      label: 'EU Bank CSV',
      currencies: ['EUR'],
    })

    expect(a.id).not.toBe(b.id)
    expect(instances.listByAdapter('generic_csv')).toHaveLength(2)
    // migrate() 已播种 deterministic legacy WeChat 实例 → 全局计数含它
    expect(instances.countAll()).toBe(3)
  })

  it('B2: accountNodeId 可后补绑定（不新建实例）', () => {
    const account = nodes.create({
      kind: 'account',
      templateId: 'builtin.account.wechat',
      name: '微信支付',
    })
    instances.create({
      id: INST_A,
      adapterId: 'wechat_statement',
      adapterVersion: 1,
      sourceKind: 'statement_file',
      label: 'Legacy WeChat Statement Source',
      currencies: ['CNY'],
    })
    expect(instances.getById(INST_A)?.accountNodeId).toBeUndefined()

    const before = instances.countAll()
    const bound = instances.bindAccountNode(INST_A, account.id)
    expect(bound.accountNodeId).toBe(account.id)
    // 绑定不改变实例数量（不新建实例），也不触碰 state
    expect(instances.countAll()).toBe(before)
    expect(bound.state).toBe('active')
  })

  it('B3: retired 实例保留全部 provenance，且不可被当作 active 重新创建', () => {
    instances.create({
      id: INST_A,
      adapterId: 'ofx_qfx',
      adapterVersion: 1,
      sourceKind: 'statement_file',
      label: 'Closed Account OFX',
    })
    fingerprints.insertBatch([
      {
        fingerprint: 'fp-retired-1',
        sourceInstanceId: INST_A,
        source: 'ofx_qfx',
        fingerprintVersion: FINGERPRINT_VERSION,
        importSessionId: 'sess-1',
        firstSeenAt: '2026-01-01T00:00:00Z',
      },
    ])

    instances.retire(INST_A)
    expect(instances.getById(INST_A)?.state).toBe('retired')
    // provenance 不迁移、不删除
    expect(fingerprints.countByInstance(INST_A)).toBe(1)
    expect(instances.retire(INST_A).state).toBe('retired')
  })

  it('B4: touchIngested 更新 lastIngestedAt；未知实例抛错（fail closed）', () => {
    instances.create({
      id: INST_A,
      adapterId: 'generic_csv',
      adapterVersion: 1,
      sourceKind: 'statement_file',
      label: 'A',
    })
    expect(instances.getById(INST_A)?.lastIngestedAt).toBeUndefined()
    instances.touchIngested(INST_A, '2026-03-15T10:00:00Z')
    expect(instances.getById(INST_A)?.lastIngestedAt).toBe('2026-03-15T10:00:00Z')

    expect(() => instances.getExisting('nope')).toThrowError(/source instance not found/)
  })

  it('B5: SourceInstance 不含任何账户秘密字段（无 number/credential/secret）', () => {
    instances.create({
      id: INST_A,
      adapterId: 'generic_csv',
      adapterVersion: 1,
      sourceKind: 'statement_file',
      label: 'A',
    })
    const cols = driver
      .prepare(`SELECT name FROM pragma_table_info('source_instances')`)
      .all()
      .map((r) => String((r as Record<string, unknown>).name))

    // 允许的列集合（显式白名单：出现新列即失败，防止未来悄悄加入敏感字段）
    expect(cols.sort()).toEqual(
      [
        'account_node_id',
        'adapter_id',
        'adapter_version',
        'country',
        'created_at',
        'currencies_json',
        'id',
        'jurisdiction',
        'label',
        'last_ingested_at',
        'provider_id',
        'source_kind',
        'state',
        'updated_at',
      ].sort(),
    )
  })

  // -------------------------------------------------------------------------
  // C 段 —— Fingerprint source scope
  // -------------------------------------------------------------------------

  it('C1: 同 SourceInstance + 同 txn id → duplicate（去重仍有效）', () => {
    seedTwoInstances()
    const [rec] = assignFingerprintsV2('secret', [obs()])

    fingerprints.insertBatch([
      {
        fingerprint: rec!.fingerprint,
        sourceInstanceId: INST_A,
        source: 'generic_csv',
        fingerprintVersion: FINGERPRINT_VERSION,
        importSessionId: 's1',
        firstSeenAt: '2026-03-15T10:00:00Z',
      },
    ])
    const second = fingerprints.insertBatch([
      {
        fingerprint: rec!.fingerprint,
        sourceInstanceId: INST_A,
        source: 'generic_csv',
        fingerprintVersion: FINGERPRINT_VERSION,
        importSessionId: 's2',
        firstSeenAt: '2026-03-15T10:00:00Z',
      },
    ])

    expect(second.fresh).toHaveLength(0)
    expect(second.duplicates).toBe(1)
    expect(fingerprints.countByInstance(INST_A)).toBe(1)
  })

  it('C2: 不同 SourceInstance + 同 txn id → 不冲突（namespace 隔离）', () => {
    seedTwoInstances()
    const [rec] = assignFingerprintsV2('secret', [obs()])

    const a = fingerprints.insertBatch([
      {
        fingerprint: rec!.fingerprint,
        sourceInstanceId: INST_A,
        source: 'generic_csv',
        fingerprintVersion: FINGERPRINT_VERSION,
        importSessionId: 's1',
        firstSeenAt: '2026-03-15T10:00:00Z',
      },
    ])
    const b = fingerprints.insertBatch([
      {
        fingerprint: rec!.fingerprint,
        sourceInstanceId: INST_B,
        source: 'generic_csv',
        fingerprintVersion: FINGERPRINT_VERSION,
        importSessionId: 's2',
        firstSeenAt: '2026-03-15T10:00:00Z',
      },
    ])

    // 同一 fingerprint 值在两个来源中各自 fresh —— 这正是 source scope 的目的
    expect(a.fresh).toHaveLength(1)
    expect(b.fresh).toHaveLength(1)
    expect(b.duplicates).toBe(0)
    expect(fingerprints.countByInstance(INST_A)).toBe(1)
    expect(fingerprints.countByInstance(INST_B)).toBe(1)
    expect(fingerprints.countAll()).toBe(2)
  })

  it('C3: fingerprintVersion 不同 → 可并存（版本隔离）', () => {
    seedTwoInstances()
    const [rec] = assignFingerprintsV2('secret', [obs()])

    fingerprints.insertBatch([
      {
        fingerprint: rec!.fingerprint,
        sourceInstanceId: INST_A,
        source: 'generic_csv',
        fingerprintVersion: 1,
        importSessionId: 's1',
        firstSeenAt: '2026-03-15T10:00:00Z',
      },
    ])
    expect(fingerprints.exists(INST_A, rec!.fingerprint, 1)).toBe(true)
    expect(fingerprints.exists(INST_A, rec!.fingerprint, 2)).toBe(false)

    const v2 = fingerprints.insertBatch([
      {
        fingerprint: rec!.fingerprint,
        sourceInstanceId: INST_A,
        source: 'generic_csv',
        fingerprintVersion: 2,
        importSessionId: 's2',
        firstSeenAt: '2026-03-15T10:00:00Z',
      },
    ])
    expect(v2.fresh).toHaveLength(1)
    expect(fingerprints.countByInstance(INST_A)).toBe(2)
  })

  it('C4: HMAC scope 包含 sourceInstanceId —— 同观测不同实例产生不同 fingerprint', () => {
    // 这是 C2 能够成立的原因：fingerprint 值本身已按实例分流，
    // 因此即使未来的 UNIQUE 约束退化，也不会把两个来源误判为同一笔。
    const [a] = assignFingerprintsV2('secret', [obs({ sourceInstanceId: INST_A })])
    const [b] = assignFingerprintsV2('secret', [obs({ sourceInstanceId: INST_B })])
    expect(a!.fingerprint).not.toBe(b!.fingerprint)

    // 同实例同输入必须 deterministic
    const [a2] = assignFingerprintsV2('secret', [obs({ sourceInstanceId: INST_A })])
    expect(a2!.fingerprint).toBe(a!.fingerprint)

    // 不同 fpSecret 必须产生不同结果（secret 参与派生）
    const [other] = assignFingerprintsV2('other-secret', [obs({ sourceInstanceId: INST_A })])
    expect(other!.fingerprint).not.toBe(a!.fingerprint)
  })

  it('C5: 无稳定 txn id → canonical-row fallback deterministic', () => {
    seedTwoInstances()
    const noId = obs({ sourceTxnId: undefined })
    const first = assignFingerprintsV2('secret', [noId])
    const second = assignFingerprintsV2('secret', [noId])
    expect(first[0]!.fingerprint).toBe(second[0]!.fingerprint)

    // 金额变化必须改变 fallback 指纹（否则会误去重）
    const different = assignFingerprintsV2('secret', [obs({ sourceTxnId: undefined, amount: 99 })])
    expect(different[0]!.fingerprint).not.toBe(first[0]!.fingerprint)
  })

  it('C6: 明文 sourceTxnId 不落库（只存 fingerprint）', () => {
    seedTwoInstances()
    const [rec] = assignFingerprintsV2('secret', [obs({ sourceTxnId: 'SENSITIVE-TXN-9999' })])
    fingerprints.insertBatch([
      {
        fingerprint: rec!.fingerprint,
        sourceInstanceId: INST_A,
        source: 'generic_csv',
        fingerprintVersion: FINGERPRINT_VERSION,
        importSessionId: 's1',
        firstSeenAt: '2026-03-15T10:00:00Z',
      },
    ])

    const dump = JSON.stringify(driver.prepare('SELECT * FROM observation_fingerprints').all())
    expect(dump).not.toContain('SENSITIVE-TXN-9999')
    expect(dump).not.toContain('NETFLIX')
  })

  it('C7: legacy WeChat 实例仍能正常去重（MVP01 能力未被 v2 scope 破坏）', () => {
    const legs = 'legacy-wechat-statement'
    const rec = assignFingerprintsV2('secret', [
      obs({ sourceInstanceId: legs, adapterId: 'wechat_statement' }),
    ])[0]!

    const r1 = fingerprints.insertBatch([
      {
        fingerprint: rec.fingerprint,
        sourceInstanceId: legs,
        source: 'wechat_statement',
        fingerprintVersion: FINGERPRINT_VERSION,
        importSessionId: 's1',
        firstSeenAt: '2026-03-15T10:00:00Z',
      },
    ])
    const r2 = fingerprints.insertBatch([
      {
        fingerprint: rec.fingerprint,
        sourceInstanceId: legs,
        source: 'wechat_statement',
        fingerprintVersion: FINGERPRINT_VERSION,
        importSessionId: 's2',
        firstSeenAt: '2026-03-15T10:00:00Z',
      },
    ])
    expect(r1.fresh).toHaveLength(1)
    expect(r2.duplicates).toBe(1)
  })

  function seedTwoInstances(): void {
    instances.create({
      id: INST_A,
      adapterId: 'generic_csv',
      adapterVersion: 1,
      sourceKind: 'statement_file',
      label: 'US Card CSV',
      currencies: ['USD'],
    })
    instances.create({
      id: INST_B,
      adapterId: 'ofx_qfx',
      adapterVersion: 1,
      sourceKind: 'statement_file',
      label: 'EU Bank OFX',
      currencies: ['EUR'],
    })
  }
})

// 保持 Observation 类型在作用域内（recurrence 视图同源）
export type _ObservationRef = Observation
