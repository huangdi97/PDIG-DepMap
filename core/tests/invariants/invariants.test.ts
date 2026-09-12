import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { mkdtempSync, readFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import {
  checkGraphIntegrity,
  ConfirmationService,
  DependencyGroupRepository,
  DependencyProposalRepository,
  DependencyRepository,
  GenericCsvAdapter,
  getRelationDefinition,
  ImportCoordinator,
  migrate,
  NodeRepository,
  NodeSqliteDriver,
  SCHEMA_VERSION,
  SourceInstanceRepository,
  currentSchemaVersion,
  simulateDisable,
  validateRelationGroupUse,
  validateRelationUse,
  type ImpactGraph,
  type MappingProfile,
} from '../../src/index.ts'

/**
 * Engineering Baseline V1 — 集中不变量套件（INV1–INV14）。
 *
 * 与分散在 unit/integration 的单点断言互补：本套件在「真实导入管线产生的
 * populated DB」上集中验证全部跨模块 invariant，作为 `npm run check:invariants` 的载体。
 *
 * 语义铁律映射：
 *   INV1/2  duplicate logical key / groupKey = 0
 *   INV3    Dependency 引用合法 Node
 *   INV4    Evidence 引用合法 SourceInstance
 *   INV5    Proposal relation 必须在 registry 注册
 *   INV6    Group 成员存在且 Group 用法合法
 *   INV7    Fingerprint 作用域唯一性
 *   INV8    accepted Proposal 重放不产生重复 Dependency
 *   INV9    retired Dependency 不传播 Impact（Reactivate 后恢复传播）
 *   INV10   event_stream absence 不 retire Reality
 *   INV11   Proposal 永远不能直接 must_change
 *   INV12   unknown criticality 永远不能直接 must_change
 *   INV13   图完整性（无 orphan / dangling）
 *   INV14   Schema 版本一致
 */

const FX = (name: string) =>
  new Uint8Array(
    readFileSync(join(dirname(fileURLToPath(import.meta.url)), '..', 'fixtures', name)),
  )

function usProfile(): MappingProfile {
  return {
    columns: {
      dateTime: 'Transaction Date',
      counterparty: 'Description',
      amount: 'Amount',
      transactionType: 'Type',
    },
    options: {
      delimiter: ',',
      dateFormats: ['MM/DD/YYYY'],
      decimalSeparator: '.',
      amountSignMode: 'signed',
      positiveDirection: 'out',
      hasHeaderRow: true,
    },
  }
}

const PAYROLL_ONLY_CSV = `Transaction Date,Posting Date,Description,Amount,Type
03/01/2026,03/02/2026,PAYROLL DEPOSIT ACME CORP,-5200.00,Credit
`

interface World {
  driver: NodeSqliteDriver
  nodes: NodeRepository
  deps: DependencyRepository
  groups: DependencyGroupRepository
  proposals: DependencyProposalRepository
  confirm: ConfirmationService
  card: ReturnType<NodeRepository['create']>
  wechat: ReturnType<NodeRepository['create']>
  tencent: ReturnType<NodeRepository['create']>
  spotify: ReturnType<NodeRepository['create']>
  proposalKeys: string[]
}

function buildWorld(): World {
  const dir = mkdtempSync(join(tmpdir(), 'depmap-inv-'))
  const driver = new NodeSqliteDriver(join(dir, 'test.db'))
  driver.open()
  migrate(driver)

  const nodes = new NodeRepository(driver)
  const deps = new DependencyRepository(driver)
  const groups = new DependencyGroupRepository(driver)
  const proposals = new DependencyProposalRepository(driver)
  const confirm = new ConfirmationService(driver)

  const card = nodes.create({
    kind: 'payment_instrument',
    name: '招行经典白',
    issuer: '招商银行',
    last4: '4417',
  })
  const wechat = nodes.create({
    kind: 'account',
    templateId: 'builtin.account.wechat',
    name: '微信支付',
  })
  const tencent = nodes.create({ kind: 'service', name: 'TENCENT VIDEO VIP' })
  const spotify = nodes.create({ kind: 'service', name: 'SPOTIFY AB' })

  return {
    driver,
    nodes,
    deps,
    groups,
    proposals,
    confirm,
    card,
    wechat,
    tencent,
    spotify,
    proposalKeys: [],
  }
}

/** 通过真实导入管线（GenericCsvAdapter + ImportCoordinator）产生 proposal/evidence/fingerprint。 */
async function importStatement(
  world: World,
  instanceLabel: string,
  data: Uint8Array,
): Promise<string[]> {
  const instances = new SourceInstanceRepository(world.driver)
  const inst = instances.create({
    adapterId: 'generic_csv',
    adapterVersion: 1,
    sourceKind: 'statement_file',
    accountNodeId: world.card.id,
    label: instanceLabel,
  })
  const co = new ImportCoordinator(world.driver)
  const begin = await co.begin(inst.id, new GenericCsvAdapter(), { data, mapping: usProfile() })
  for (const c of begin.candidates) {
    const target =
      c.merchantRaw === 'TENCENT VIDEO VIP'
        ? world.tencent
        : c.merchantRaw === 'SPOTIFY AB'
          ? world.spotify
          : null
    if (target) co.resolveMerchant(c.merchantRaw, target.id)
  }
  const outcome = await co.finalize()
  return outcome.proposalKeys
}

function buildImpactGraph(world: World): ImpactGraph {
  return {
    dependencies: world.deps.listAll(),
    groups: world.groups.listAllActive(),
    proposals: world.proposals
      .listAll()
      .filter((p) => p.decision === 'pending')
      .map((p) => ({ key: p.key, from: p.from, to: p.to, capability: p.capability })),
  }
}

describe('Invariants（集中不变量套件，Engineering Baseline V1）', () => {
  let world: World

  beforeEach(() => {
    world = buildWorld()
  })

  afterEach(() => {
    world.driver.close()
  })

  it('INV1/INV8: 逻辑键唯一 —— accept 重放 ×3 + retire→reactivate 后重复逻辑键 = 0', async () => {
    world.proposalKeys.push(...(await importStatement(world, 's1', FX('csv-us-credit-card.csv'))))
    const key = world.proposalKeys[0]!
    const a1 = world.confirm.acceptProposal(key, 'required')
    const a2 = world.confirm.acceptProposal(key, 'required')
    const a3 = world.confirm.acceptProposal(key, 'required')
    expect(a2.created).toBe(false)
    expect(a3.created).toBe(false)

    world.deps.retire(a1.dependency.id)
    world.confirm.acceptProposal(key, 'required')

    const dupRows = world.driver
      .prepare(
        `SELECT from_node, relation, to_node, capability, COUNT(*) AS c
         FROM dependencies GROUP BY from_node, relation, to_node, capability HAVING c > 1`,
      )
      .all()
    expect(dupRows).toEqual([])
    expect(world.deps.countAll()).toBe(1)
  })

  it('INV2: groupKey 唯一 —— 重复 confirm 同一 Group 只有一行且 key canonical', () => {
    const d1 = world.deps.confirm({
      from: world.card.id,
      relation: 'funding_source',
      to: world.wechat.id,
      capability: 'payment',
    })
    const d2 = world.deps.confirm({
      from: 'n-bank-b',
      relation: 'funding_source',
      to: world.wechat.id,
      capability: 'payment',
    })
    const mk = () => ({
      targetNodeId: world.wechat.id,
      capability: 'payment' as const,
      mode: 'ANY' as const,
      memberEdgeIds: [d1.dependency.id, d2.dependency.id],
    })
    const keys = [`k-${d1.dependency.id}`, `k-${d2.dependency.id}`]
    const g1 = world.groups.confirm(mk(), keys).group
    const g2 = world.groups.confirm(mk(), [...keys].reverse()).group
    expect(g2.id).toBe(g1.id)

    const dupRows = world.driver
      .prepare(
        `SELECT group_key, COUNT(*) AS c FROM dependency_groups GROUP BY group_key HAVING c > 1`,
      )
      .all()
    expect(dupRows).toEqual([])
  })

  it('INV3/INV4/INV5: Dependency→Node、Evidence→SourceInstance、Proposal relation 已注册', async () => {
    world.proposalKeys.push(...(await importStatement(world, 's1', FX('csv-us-credit-card.csv'))))
    world.confirm.acceptProposal(world.proposalKeys[0]!, 'required')

    const danglingDeps = world.driver
      .prepare(
        `SELECT d.id FROM dependencies d
         LEFT JOIN nodes fn ON fn.id = d.from_node
         LEFT JOIN nodes tn ON tn.id = d.to_node
         WHERE fn.id IS NULL OR tn.id IS NULL`,
      )
      .all()
    expect(danglingDeps).toEqual([])

    const danglingEvidence = world.driver
      .prepare(
        `SELECT e.id FROM evidence e
         LEFT JOIN source_instances si ON si.id = e.source_instance_id
         WHERE e.source_instance_id IS NOT NULL AND si.id IS NULL`,
      )
      .all()
    expect(danglingEvidence).toEqual([])

    for (const p of world.proposals.listAll()) {
      expect(getRelationDefinition(p.relation)).not.toBeNull()
      const fromNode = world.nodes.getExisting(p.from)
      const toNode = world.nodes.getExisting(p.to)
      const result = validateRelationUse(fromNode.kind, p.relation, toNode.kind, p.capability)
      expect(result.ok).toBe(true)
    }
    expect(world.proposals.listAll().length).toBeGreaterThan(0)
  })

  it('INV6: Group 成员存在且 registry 校验通过（ANY funding_source）', () => {
    const d1 = world.deps.confirm({
      from: world.card.id,
      relation: 'funding_source',
      to: world.wechat.id,
      capability: 'payment',
    })
    const d2 = world.deps.confirm({
      from: 'n-bank-b',
      relation: 'funding_source',
      to: world.wechat.id,
      capability: 'payment',
    })
    const g = world.groups.confirm(
      {
        targetNodeId: world.wechat.id,
        capability: 'payment',
        mode: 'ANY',
        memberEdgeIds: [d1.dependency.id, d2.dependency.id],
      },
      [`k-${d1.dependency.id}`, `k-${d2.dependency.id}`],
    ).group

    for (const edgeId of g.memberEdgeIds) {
      expect(world.deps.getById(edgeId)).not.toBeNull()
    }
    expect(validateRelationGroupUse('funding_source', 'ANY').ok).toBe(true)
  })

  it('INV7: Fingerprint 作用域唯一 —— (source_instance_id, fingerprint_version, fingerprint) 全表无重复', async () => {
    world.proposalKeys.push(...(await importStatement(world, 's1', FX('csv-us-credit-card.csv'))))
    // 重复导入同内容（新实例同文件）→ 指纹仍按作用域隔离
    await importStatement(world, 's2', FX('csv-us-credit-card.csv'))

    const row = world.driver
      .prepare(
        `SELECT COUNT(*) AS total, COUNT(DISTINCT source_instance_id || ':' || fingerprint_version || ':' || fingerprint) AS distinct_
         FROM observation_fingerprints`,
      )
      .get() as { total: number; distinct_: number }
    expect(Number(row.total)).toBeGreaterThan(0)
    expect(Number(row.total)).toBe(Number(row.distinct_))
  })

  it('INV9: retired Dependency 不传播 Impact；re-activate 同一 id 后恢复传播', async () => {
    world.proposalKeys.push(...(await importStatement(world, 's1', FX('csv-us-credit-card.csv'))))
    const { dependency } = world.confirm.acceptProposal(world.proposalKeys[0]!, 'required')

    const before = simulateDisable(buildImpactGraph(world), world.card.id)
    expect(before.checklist.some((c) => c.level === 'must_change')).toBe(true)

    world.deps.retire(dependency.id)
    const afterRetire = simulateDisable(buildImpactGraph(world), world.card.id)
    expect(afterRetire.checklist.some((c) => c.level === 'must_change')).toBe(false)

    const revive = world.deps.confirm({
      from: dependency.from,
      relation: dependency.relation,
      to: dependency.to,
      capability: dependency.capability,
    })
    expect(revive.dependency.id).toBe(dependency.id)
    const afterRevive = simulateDisable(buildImpactGraph(world), world.card.id)
    expect(afterRevive.checklist.some((c) => c.level === 'must_change')).toBe(true)
  })

  it('INV10: event_stream absence 不 retire Reality —— 后续流缺少该商户，Dependency 仍 active', async () => {
    world.proposalKeys.push(...(await importStatement(world, 's1', FX('csv-us-credit-card.csv'))))
    const { dependency } = world.confirm.acceptProposal(world.proposalKeys[0]!, 'required')
    const decisionsBefore = new Map(world.proposals.listAll().map((p) => [p.key, p.decision]))

    const newKeys = await importStatement(
      world,
      's2-payroll-only',
      new TextEncoder().encode(PAYROLL_ONLY_CSV),
    )
    expect(newKeys).toEqual([])

    const after = world.deps.getById(dependency.id)
    expect(after?.state).toBe('active')
    // absence 不改变任何 proposal 决策状态
    for (const p of world.proposals.listAll()) {
      expect(p.decision).toBe(decisionsBefore.get(p.key))
    }
  })

  it('INV11: Proposal-only 图 —— 永远不产生 must_change', async () => {
    world.proposalKeys.push(...(await importStatement(world, 's1', FX('csv-us-credit-card.csv'))))
    expect(world.proposalKeys.length).toBeGreaterThan(0)

    const result = simulateDisable(buildImpactGraph(world), world.card.id)
    expect(result.checklist.some((c) => c.level === 'must_change')).toBe(false)
  })

  it('INV12: unknown criticality —— 只 needs_review，绝不 must_change', () => {
    world.deps.confirm({
      from: world.card.id,
      relation: 'funding_source',
      to: world.wechat.id,
      capability: 'payment',
    })
    const result = simulateDisable(buildImpactGraph(world), world.card.id)
    const wechatTargets = result.targets.filter((t) => t.nodeId === world.wechat.id)
    expect(wechatTargets.length).toBeGreaterThan(0)
    for (const t of wechatTargets) {
      expect(t.status).not.toBe('must_change')
      expect(['needs_review', 'unaffected']).toContain(t.status)
    }
  })

  it('INV13: 全程图完整性 —— 导入/确认/retire/reactivate 后无 orphan / dangling', async () => {
    world.proposalKeys.push(...(await importStatement(world, 's1', FX('csv-us-credit-card.csv'))))
    const { dependency } = world.confirm.acceptProposal(world.proposalKeys[0]!, 'required')
    world.deps.retire(dependency.id)
    world.deps.confirm({
      from: dependency.from,
      relation: dependency.relation,
      to: dependency.to,
      capability: dependency.capability,
    })

    const report = checkGraphIntegrity(world.driver)
    expect(report.orphanDependencies).toEqual([])
    expect(report.orphanGroups).toEqual([])
    expect(report.danglingGroupMembers).toEqual([])
    expect(report.orphanEvidence).toEqual([])
    expect(report.orphanFingerprints).toEqual([])
  })

  it('INV14: Schema 版本一致 —— currentSchemaVersion === SCHEMA_VERSION === 2', () => {
    expect(SCHEMA_VERSION).toBe(2)
    expect(currentSchemaVersion(world.driver)).toBe(SCHEMA_VERSION)
  })
})
