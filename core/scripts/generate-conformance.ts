/**
 * PDIG Conformance Fixture Generator
 *
 * 用途：把被冻结的 TS Core 当作 BEHAVIOR ORACLE，产出**平台中立 JSON fixture**
 * 与期望输出，供 Kotlin / Swift / ArkTS 三端复现。
 *
 * 运行：
 *   cd core && node --experimental-strip-types scripts/generate-conformance.ts
 *
 * 产出：
 *   ../fixtures/<category>/<caseId>.json     每个用例一个文件（input + expected）
 *   ../conformance/CONFORMANCE_MANIFEST.json 用例清单 + 哈希 + oracle 提交
 *
 * 规则（spec/README.md §1）：
 *   - fixture 一旦生成并确认，不得随意重新生成 expected
 *   - 期望行为变更必须先改 spec/
 */
import { createHash } from 'node:crypto'
import { mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync, existsSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { execFileSync } from 'node:child_process'

import {
  simulateScenario,
  computePlanReadiness,
  computeScenarioCoverage,
  validateRelationUse,
  validateRelationGroupUse,
  buildTimeline,
  exportGraph,
  importGraph,
  checkGraphIntegrity,
  NodeSqliteDriver,
  migrate,
  SCHEMA_V1_STATEMENTS,
  LEGACY_WECHAT_SOURCE_INSTANCE_ID,
  WeChatStatementAdapter,
  GenericCsvAdapter,
  OfxQfxAdapter,
  computePathIndependence,
  detectRecoveryCycles,
  validateActionDag,
  evaluateMakeBeforeBreak,
  classifyTemporalPhase,
  interpretProviderCapability,
  inferProviderPolicyState,
  type Capability,
  type ChangePlan,
  type Criticality,
  type Dependency,
  type DependencyGroup,
  type ImpactGraph,
  type ImpactStateKey,
  type MappingProfile,
  type Observation,
  type PlanAction,
  type PlanReadinessInput,
  type ScenarioCoverageInput,
  type ProviderPolicy,
  type TemporalChangeWindow,
} from '../src/index.ts'
import { createDepmapContainer, openDepmapContainer } from '../src/crypto/depmap.ts'
import { jcsStringify } from '../src/crypto/jcs.ts'

const HERE = dirname(fileURLToPath(import.meta.url))
const CORE = join(HERE, '..')
const ROOT = join(CORE, '..')
const FIX = join(ROOT, 'fixtures')
const CONF = join(ROOT, 'conformance')

const T0 = '2026-09-13T00:00:00.000Z'

// ---------------------------------------------------------------------------
// deterministic builders
// ---------------------------------------------------------------------------

function dep(
  id: string,
  from: string,
  to: string,
  opts: {
    relation?: 'funding_source' | 'merchant_agreement'
    capability?: Capability
    criticality?: Criticality
    state?: 'active' | 'retired'
    groupId?: string | null
  } = {},
): Dependency {
  return {
    id,
    from,
    relation: opts.relation ?? 'funding_source',
    to,
    capability: opts.capability ?? 'payment',
    criticality: opts.criticality ?? 'unknown',
    groupId: opts.groupId ?? null,
    state: opts.state ?? 'active',
    origin: 'manual',
    confirmedAt: T0,
    lastVerifiedAt: T0,
    retiredAt: opts.state === 'retired' ? T0 : null,
    evidenceRefs: [],
    verificationBasis: { type: 'user_confirmed', verifiedAt: T0 },
    createdAt: T0,
    updatedAt: T0,
  }
}

function grp(
  id: string,
  targetNodeId: string,
  mode: 'ANY' | 'ALL',
  memberEdgeIds: string[],
): DependencyGroup {
  const sorted = [...new Set(memberEdgeIds)].sort()
  return {
    id,
    groupKey: [targetNodeId, 'payment', mode, ...sorted].join('|'),
    targetNodeId,
    capability: 'payment',
    mode,
    memberEdgeIds,
    state: 'active',
    confirmedAt: T0,
    lastVerifiedAt: T0,
    verificationBasis: { type: 'user_confirmed', verifiedAt: T0 },
    createdAt: T0,
    updatedAt: T0,
  }
}

function action(
  id: string,
  phase: 'prepare' | 'change' | 'verify',
  resolvesImpactKeys: string[],
  done = false,
): PlanAction {
  return {
    id,
    title: `${phase} ${id}`,
    detail: 'fixture',
    phase,
    done,
    doneAt: done ? T0 : null,
    verification: null,
    resolvesImpactKeys,
  }
}

function plan(overrides: Partial<ChangePlan> = {}): ChangePlan {
  const base: ChangePlan = {
    id: 'plan-1',
    templateId: 'replace_payment_card',
    scenario: 'replace_payment_card',
    title: '更换银行卡',
    workflowState: 'analyzed',
    baselineGraphRevision: 3,
    lastAnalyzedGraphRevision: 3,
    targetNodeId: 'card-a',
    effectiveDate: null,
    params: {},
    impactSnapshot: null,
    actions: [],
    createdAt: T0,
    updatedAt: T0,
  }
  return { ...base, ...overrides }
}

// ---------------------------------------------------------------------------
// collection
// ---------------------------------------------------------------------------

interface Case {
  id: string
  category: string
  description: string
  input: unknown
  expected: unknown
}

const cases: Case[] = []

function add(
  id: string,
  category: string,
  description: string,
  input: unknown,
  expected: unknown,
): void {
  cases.push({ id, category, description, input, expected })
}

// ---------------------------------------------------------------------------
// 1. IMPACT — simulateScenario
// ---------------------------------------------------------------------------

const NODES: Record<string, string> = {
  'card-a': '招行信用卡',
  'card-b': '建行储蓄卡',
  wechat: '微信支付',
  tencent: '腾讯视频',
  spotify: 'Spotify',
}

function impactGraph(
  deps: Dependency[],
  groups: DependencyGroup[],
  proposals: ImpactGraph['proposals'] = [],
): ImpactGraph {
  return { dependencies: deps, groups, proposals, nodeNames: NODES }
}

function runImpact(
  id: string,
  description: string,
  graph: ImpactGraph,
  unavailable: ImpactStateKey[],
): void {
  const result = simulateScenario(graph, new Set(unavailable))
  add(id, 'impact', description, { graph, unavailable }, result)
}

// IMP-001 required 边无替代 → must_change
runImpact(
  'impact-required-edge-loss',
  'required 边失效且无其他来源 → must_change / required_edge_no_alternative，并向下游级联',
  impactGraph(
    [
      dep('d1', 'card-a', 'wechat', { criticality: 'required' }),
      dep('d2', 'wechat', 'tencent', { criticality: 'required' }),
    ],
    [],
  ),
  [{ nodeId: 'card-a', capability: 'payment' }],
)

// IMP-002 unknown criticality → needs_review，绝不 must_change
runImpact(
  'impact-unknown-criticality',
  'unknown criticality 只产生 needs_review，绝不 must_change（INV12）',
  impactGraph([dep('d1', 'card-a', 'wechat', { criticality: 'unknown' })], []),
  [{ nodeId: 'card-a', capability: 'payment' }],
)

// IMP-003 ANY group 覆盖 → backup_path
runImpact(
  'impact-any-group-covered',
  '已确认 ANY 组合仍有可用成员 → backup_path / confirmed_group_covered，冗余度下降',
  impactGraph(
    [
      dep('d1', 'card-a', 'wechat', { criticality: 'required', groupId: 'g1' }),
      dep('d2', 'card-b', 'wechat', { criticality: 'required', groupId: 'g1' }),
    ],
    [grp('g1', 'wechat', 'ANY', ['d1', 'd2'])],
  ),
  [{ nodeId: 'card-a', capability: 'payment' }],
)

// IMP-004 ANY group 全部失效 → must_change
runImpact(
  'impact-any-group-failed',
  '已确认 ANY 组合全部成员失效 → must_change / confirmed_group_failed',
  impactGraph(
    [
      dep('d1', 'card-a', 'wechat', { criticality: 'required', groupId: 'g1' }),
      dep('d2', 'card-b', 'wechat', { criticality: 'required', groupId: 'g1' }),
    ],
    [grp('g1', 'wechat', 'ANY', ['d1', 'd2'])],
  ),
  [
    { nodeId: 'card-a', capability: 'payment' },
    { nodeId: 'card-b', capability: 'payment' },
  ],
)

// IMP-005 ALL group 缺一 → 不满足 → must_change
runImpact(
  'impact-all-group-unsatisfied',
  '已确认 ALL 组合缺任一成员即不满足 → must_change',
  impactGraph(
    [
      dep('d1', 'card-a', 'wechat', { criticality: 'required', groupId: 'g1' }),
      dep('d2', 'card-b', 'wechat', { criticality: 'required', groupId: 'g1' }),
    ],
    [grp('g1', 'wechat', 'ALL', ['d1', 'd2'])],
  ),
  [{ nodeId: 'card-a', capability: 'payment' }],
)

// IMP-006 环：cycle-safe 终止且 processedKeys 唯一
runImpact(
  'impact-cycle-safe',
  '图允许有环；状态键 (nodeId,capability) 保证终止，processedKeys 无重复（IMP-05/06）',
  impactGraph(
    [
      dep('d1', 'card-a', 'wechat', { criticality: 'required' }),
      dep('d2', 'wechat', 'tencent', { criticality: 'required' }),
      dep('d3', 'tencent', 'wechat', { criticality: 'required' }),
    ],
    [],
  ),
  [{ nodeId: 'card-a', capability: 'payment' }],
)

// IMP-006b 真环级联：A→B→C→A（每点唯一入边）→ must_change 沿环级联一次后因状态键终止
runImpact(
  'impact-cycle-cascade',
  '真环 A→B→C→A 且每节点唯一入边：must_change 沿环级联传播，回到已不可用节点时终止（不重复处理）',
  impactGraph(
    [
      dep('d1', 'card-a', 'wechat', { criticality: 'required' }),
      dep('d2', 'wechat', 'tencent', { criticality: 'required' }),
      dep('d3', 'tencent', 'card-a', { criticality: 'required' }),
    ],
    [],
  ),
  [{ nodeId: 'card-a', capability: 'payment' }],
)

// IMP-006c 稠密环：每节点都有替代来源 → 不确定性传播，但绝不升级为 must_change（IMP-07）
runImpact(
  'impact-dense-cycle-uncertainty-only',
  '稠密环中每节点都有其他来源：只传播不确定性，永不产生 must_change（IMP-03/07）',
  impactGraph(
    [
      dep('d1', 'card-a', 'wechat', { criticality: 'required' }),
      dep('d2', 'wechat', 'tencent', { criticality: 'required' }),
      dep('d3', 'tencent', 'spotify', { criticality: 'required' }),
      dep('d4', 'spotify', 'wechat', { criticality: 'required' }),
    ],
    [],
  ),
  [{ nodeId: 'card-a', capability: 'payment' }],
)

// IMP-007 retired 边不传播
runImpact(
  'impact-retired-edge',
  'retired 边不传播 Impact（IMP-04 / INV9）',
  impactGraph([dep('d1', 'card-a', 'wechat', { criticality: 'required', state: 'retired' })], []),
  [{ nodeId: 'card-a', capability: 'payment' }],
)

// IMP-008 proposal-only → needs_review，绝不 must_change
runImpact(
  'impact-proposal-only',
  '仅存在 pending Proposal → needs_review / proposal_only；confidence 不改变结论（IMP-03）',
  impactGraph(
    [],
    [],
    [
      {
        key: 'card-a|funding_source|wechat|payment',
        from: 'card-a',
        to: 'wechat',
        capability: 'payment',
        confidenceScore: 0.999,
      },
    ],
  ),
  [{ nodeId: 'card-a', capability: 'payment' }],
)

// IMP-009 多替代来源未确认组合 → needs_review
runImpact(
  'impact-multiple-alternatives',
  '存在其他已记录来源但未确认备用组合 → needs_review / unconfirmed_alternative_exists',
  impactGraph(
    [
      dep('d1', 'card-a', 'wechat', { criticality: 'required' }),
      dep('d2', 'card-b', 'wechat', { criticality: 'required' }),
    ],
    [],
  ),
  [{ nodeId: 'card-a', capability: 'payment' }],
)

// IMP-010 确定性排序 + 多目标
runImpact(
  'impact-deterministic-ordering',
  '多目标输出按 (depth, nodeId, capability) 确定性排序；原始操作强制最后（IMP-08/09）',
  impactGraph(
    [
      dep('d1', 'card-a', 'wechat', { criticality: 'required' }),
      dep('d2', 'card-a', 'spotify', { criticality: 'required' }),
      dep('d3', 'wechat', 'tencent', { criticality: 'required' }),
    ],
    [],
  ),
  [{ nodeId: 'card-a', capability: 'payment' }],
)

// IMP-011 非 payment 初始键被忽略
runImpact(
  'impact-non-payment-capability-ignored',
  'MVP 只传播 payment；非 payment 初始键被忽略（AGENTS §13）',
  impactGraph(
    [dep('d1', 'card-a', 'wechat', { criticality: 'required', capability: 'access' })],
    [],
  ),
  [{ nodeId: 'card-a', capability: 'payment' }],
)

// ---------------------------------------------------------------------------
// 2. READINESS — computePlanReadiness（纯规则）
// ---------------------------------------------------------------------------

function readinessInput(overrides: Partial<PlanReadinessInput> = {}): PlanReadinessInput {
  return {
    plan: plan(),
    currentGraphRevision: 3,
    pendingMustChange: 0,
    pendingNeedsReview: 0,
    unresolvedCandidates: 0,
    pendingRelevantProposals: 0,
    staleRelevantDependencies: 0,
    unfinishedChangeActions: 0,
    ...overrides,
  }
}

function addReadiness(id: string, description: string, input: PlanReadinessInput): void {
  add(id, 'readiness', description, input, computePlanReadiness(input))
}

addReadiness(
  'readiness-one-target-multiple-actions',
  'one target / multiple required actions —— 全部声明动作完成才算 resolved（FR-READ-001）',
  readinessInput({
    pendingMustChange: 0,
    plan: plan({ actions: [action('c1', 'change', ['wechat|payment'], true)] }),
  }),
)

addReadiness(
  'readiness-one-target-multiple-actions-partial',
  'one target / 两个声明动作只完成其一 → blocked（禁止减法近似，RD-01/02）',
  readinessInput({
    pendingMustChange: 1,
    plan: plan({
      actions: [
        action('c1', 'change', ['wechat|payment'], true),
        action('c2', 'change', ['wechat|payment'], false),
      ],
    }),
  }),
)

addReadiness(
  'readiness-multiple-targets-shared-action',
  'multiple targets / one shared action 全部声明且完成 → 不 blocked（FR-READ-002）',
  readinessInput({
    pendingMustChange: 0,
    plan: plan({ actions: [action('c1', 'change', ['wechat|payment', 'alipay|payment'], true)] }),
  }),
)

addReadiness(
  'readiness-unrelated-completed-action',
  '完成无关动作不解决任何 requirement → blocked（FR-READ-003 / RD-03）',
  readinessInput({
    pendingMustChange: 1,
    plan: plan({ actions: [action('c1', 'change', ['other|payment'], true)] }),
  }),
)

addReadiness(
  'readiness-unresolved-must-change',
  'pendingMustChange > 0 → blocked（FR-READ-004）',
  readinessInput({ pendingMustChange: 2 }),
)

addReadiness(
  'readiness-unknown-criticality-review',
  'unknown criticality（needs_review）→ review_required（FR-READ-005）',
  readinessInput({ pendingNeedsReview: 1 }),
)

addReadiness(
  'readiness-pending-proposal',
  'pending Proposal → review_required（FR-READ-006）',
  readinessInput({ pendingRelevantProposals: 1 }),
)

addReadiness(
  'readiness-unresolved-candidate',
  'unresolved Candidate → review_required（FR-READ-007）',
  readinessInput({ unresolvedCandidates: 1 }),
)

addReadiness(
  'readiness-revision-mismatch',
  'revision 落后 → review_required（派生 needs_revalidation，FR-READ-008 / RD-09）',
  readinessInput({ currentGraphRevision: 9, plan: plan({ lastAnalyzedGraphRevision: 3 }) }),
)

addReadiness(
  'readiness-stale-dependency',
  '长期未验证的相关依赖 → review_required（FR / plan-readiness-coverage #10）',
  readinessInput({ staleRelevantDependencies: 1 }),
)

addReadiness(
  'readiness-unfinished-change-action',
  '未完成的 change 阶段动作 → review_required（FR-READ-014 / RD-08）',
  readinessInput({ unfinishedChangeActions: 1 }),
)

addReadiness(
  'readiness-all-resolved',
  '全部已知要求处理完毕 → ready_with_known_scope（禁止 safe / 100%）',
  readinessInput({ plan: plan({ actions: [action('c1', 'change', ['wechat|payment'], true)] }) }),
)

addReadiness(
  'readiness-confidence-cannot-bypass',
  '输入结构中不存在 confidence 通道：conf .999 也无法绕过 review（FR-READ-010 / RD-06）',
  readinessInput({ pendingRelevantProposals: 1 }),
)

addReadiness(
  'readiness-absence-cannot-help',
  '输入结构中不存在 absence 通道：absence 不能提高 readiness（FR-READ-011）',
  readinessInput({ pendingMustChange: 1 }),
)

addReadiness(
  'readiness-blocked-beats-review',
  'blocked 优先于 review_required（FR-READ-012 / RD-05）',
  readinessInput({ pendingMustChange: 1, pendingNeedsReview: 5, unresolvedCandidates: 3 }),
)

addReadiness(
  'readiness-completed-empty-claim-not-retroactive',
  '已完成的空声明动作不得事后追认 key → blocked（FR-READ-017 / RD-04）',
  readinessInput({
    pendingMustChange: 1,
    plan: plan({
      actions: [
        action('a1', 'change', [], true),
        action('a2', 'change', ['wechat|payment'], false),
      ],
    }),
  }),
)

// ---------------------------------------------------------------------------
// 3. COVERAGE — computeScenarioCoverage（纯规则）
// ---------------------------------------------------------------------------

function covInput(overrides: Partial<ScenarioCoverageInput> = {}): ScenarioCoverageInput {
  return {
    scenarioId: 'replace_payment_card',
    sources: [],
    confirmedDirectDependencies: 0,
    confirmedIndirectDependencies: 0,
    pendingProposals: 0,
    unresolvedCandidates: 0,
    staleDependencies: 0,
    unknownCriticalityCount: 0,
    unverifiedActions: 0,
    freshnessThresholdDays: 45,
    now: T0,
    ...overrides,
  }
}

function addCoverage(id: string, description: string, input: ScenarioCoverageInput): void {
  add(id, 'coverage', description, input, computeScenarioCoverage(input))
}

addCoverage('coverage-unknown', '无来源且无已确认直接依赖 → unknown', covInput())

addCoverage(
  'coverage-limited-stale-sources',
  '有来源但全部超过新鲜度阈值 → limited（且可解释）',
  covInput({
    sources: [{ id: 's1', label: '微信账单', lastIngestedAt: '2026-01-01T00:00:00.000Z' }],
    confirmedDirectDependencies: 3,
  }),
)

addCoverage(
  'coverage-limited-no-direct-deps',
  '有新鲜来源但无任何已确认直接依赖 → limited',
  covInput({
    sources: [{ id: 's1', label: '微信账单', lastIngestedAt: '2026-09-12T00:00:00.000Z' }],
  }),
)

addCoverage(
  'coverage-partial-pending-proposal',
  '有新来源但有 pending proposal / unknown → partial，且可解释',
  covInput({
    sources: [{ id: 's1', label: '微信账单', lastIngestedAt: '2026-09-12T00:00:00.000Z' }],
    confirmedDirectDependencies: 5,
    confirmedIndirectDependencies: 2,
    pendingProposals: 2,
    unknownCriticalityCount: 1,
  }),
)

addCoverage(
  'coverage-well-evidenced',
  '新来源 + 关键关系全部确认且无未决信号 → well_evidenced（仍不等于 ready / safe）',
  covInput({
    sources: [
      { id: 's1', label: '微信账单', lastIngestedAt: '2026-09-12T00:00:00.000Z' },
      { id: 's2', label: '通用 CSV', lastIngestedAt: '2026-09-11T00:00:00.000Z' },
    ],
    confirmedDirectDependencies: 5,
    confirmedIndirectDependencies: 2,
  }),
)

addCoverage(
  'coverage-never-ingested-is-stalest',
  '从未导入（lastIngestedAt=null）视为最陈旧 → 不会计入新鲜来源',
  covInput({
    sources: [{ id: 's1', label: '从未导入', lastIngestedAt: null }],
    confirmedDirectDependencies: 1,
  }),
)

// ---------------------------------------------------------------------------
// 4. RELATIONS — RelationDefinitionRegistry
// ---------------------------------------------------------------------------

const relationCases: Array<{
  id: string
  fromKind: string | null
  relation: string
  toKind: string | null
  capability: string
}> = [
  {
    id: 'funding-source-ok',
    fromKind: 'payment_instrument',
    relation: 'funding_source',
    toKind: 'account',
    capability: 'payment',
  },
  {
    id: 'funding-source-ok-account-from',
    fromKind: 'account',
    relation: 'funding_source',
    toKind: 'payment_instrument',
    capability: 'payment',
  },
  {
    id: 'funding-source-ok-service-from',
    fromKind: 'service',
    relation: 'funding_source',
    toKind: 'account',
    capability: 'payment',
  },
  {
    id: 'merchant-agreement-ok',
    fromKind: 'payment_instrument',
    relation: 'merchant_agreement',
    toKind: 'service',
    capability: 'payment',
  },
  {
    id: 'merchant-agreement-ok-membership',
    fromKind: 'account',
    relation: 'merchant_agreement',
    toKind: 'membership',
    capability: 'payment',
  },
  {
    id: 'reject-non-runtime-relation',
    fromKind: 'payment_instrument',
    relation: 'bound_to',
    toKind: 'account',
    capability: 'payment',
  },
  {
    id: 'reject-non-runtime-relation-verifies',
    fromKind: 'account',
    relation: 'verifies',
    toKind: 'account',
    capability: 'payment',
  },
  {
    id: 'reject-unknown-relation',
    fromKind: 'account',
    relation: 'nope',
    toKind: 'account',
    capability: 'payment',
  },
  {
    id: 'reject-capability-mismatch',
    fromKind: 'payment_instrument',
    relation: 'funding_source',
    toKind: 'account',
    capability: 'access',
  },
  {
    id: 'reject-from-kind',
    fromKind: 'device',
    relation: 'funding_source',
    toKind: 'account',
    capability: 'payment',
  },
  {
    id: 'reject-from-kind-identity-anchor',
    fromKind: 'identity_anchor',
    relation: 'merchant_agreement',
    toKind: 'service',
    capability: 'payment',
  },
  {
    id: 'reject-to-kind',
    fromKind: 'payment_instrument',
    relation: 'funding_source',
    toKind: 'device',
    capability: 'payment',
  },
  {
    id: 'reject-to-kind-custom',
    fromKind: 'account',
    relation: 'merchant_agreement',
    toKind: 'custom',
    capability: 'payment',
  },
  {
    id: 'allow-null-kinds',
    fromKind: null,
    relation: 'funding_source',
    toKind: null,
    capability: 'payment',
  },
]

// LC-003 Canonical correction 的可追溯标记（2026-09-18）：
// Legacy 某路径曾暴露/接受 `bound_to`，但 runtime registry 未注册 ⇒
// bound_to != valid canonical relation。此处 Markdown-fixture 的作用是把这条
// **Canonical correction** 永久固化成跨端契约（Android / Harmony / iOS 同跑同批 fixture），
// 而不是描述某一端的历史行为。冻结的 Legacy 行为不修改，差异经 LEGACY_BEHAVIOR_CORRECTIONS 记录。
const LC003_NOTE =
  ' — LC-003 Canonical correction: Legacy exposed/accepted this relation, but it is NOT registered in the runtime registry, therefore it is not a valid canonical relation (frozen Legacy behaviour is unchanged; divergence recorded in LEGACY_BEHAVIOR_CORRECTIONS.md LC-003)'

for (const c of relationCases) {
  const note = c.relation === 'bound_to' ? LC003_NOTE : ''
  add(
    `relation-${c.id}`,
    'relations',
    `validateRelationUse(${c.fromKind ?? 'null'}, ${c.relation}, ${c.toKind ?? 'null'}, ${c.capability})${note}`,
    c,
    validateRelationUse(c.fromKind as never, c.relation, c.toKind as never, c.capability),
  )
}

for (const c of [
  { relation: 'funding_source', mode: 'ANY' as const },
  { relation: 'funding_source', mode: 'ALL' as const },
  { relation: 'merchant_agreement', mode: 'ANY' as const },
  { relation: 'bound_to', mode: 'ANY' as const },
]) {
  const note = c.relation === 'bound_to' ? LC003_NOTE : ''
  add(
    `relation-group-${c.relation}-${c.mode}`,
    'relations',
    `validateRelationGroupUse(${c.relation}, ${c.mode})${note}`,
    c,
    validateRelationGroupUse(c.relation, c.mode),
  )
}

// ---------------------------------------------------------------------------
// 5. DEPMAP — golden vector + cross-implementation open
// ---------------------------------------------------------------------------

const GOLDEN_SALT = Uint8Array.from(Buffer.from('00112233445566778899aabbccddeeff', 'hex'))
const GOLDEN_NONCE = Uint8Array.from(Buffer.from('a1b2c3d4e5f60718293a4b5c', 'hex'))
const GOLDEN_PLAINTEXT = '{"app":"depmap","schemaVersion":1,"nodes":[],"dependencies":[]}'

{
  const r = await createDepmapContainer(new TextEncoder().encode(GOLDEN_PLAINTEXT), 'depmap-test', {
    salt: GOLDEN_SALT,
    nonce: GOLDEN_NONCE,
  })
  const opened = await openDepmapContainer(r.json, 'depmap-test')
  const firstFailure = (e: unknown) => (e as { code?: string }).code ?? 'error'
  const wrong = await openDepmapContainer(r.json, 'wrong-password').then(
    () => 'opened',
    firstFailure,
  )
  // 篡改：正确密码 + 被改动的 ciphertext 也必须认证失败（fail closed）
  const tampered = r.json.replace(/"ciphertext":"[^"]+"/, '"ciphertext":"AAAAAAAAAAAAAAAAAAAAAA=="')
  const tamperedOutcome = await openDepmapContainer(tampered, 'depmap-test').then(
    () => 'opened',
    firstFailure,
  )
  add(
    'depmap-golden-v1',
    'depmap',
    'DEPMAP_CONTAINER_V1 固定向量：三端必须产出完全相同的 derivedKey / ciphertext / tag 与相同容器 JSON',
    {
      protocol: 'DEPMAP_CONTAINER_V1',
      password: 'depmap-test',
      saltHex: '00112233445566778899aabbccddeeff',
      nonceHex: 'a1b2c3d4e5f60718293a4b5c',
      plaintext: GOLDEN_PLAINTEXT,
      kdf: { memoryKiB: 65536, iterations: 3, parallelism: 1 },
    },
    {
      derivedKeyHex: r.derivedKeyHex,
      ciphertextBase64: r.header.ciphertext,
      tagBase64: r.header.tag,
      containerJson: r.json,
      reopenedPlaintext: new TextDecoder().decode(opened.plaintext),
      wrongPasswordOutcome: wrong,
      tamperedCiphertextOutcome: tamperedOutcome,
    },
  )
}

// UTF-8 语义（不做 Unicode 归一化）
{
  const utf8Cases = [
    { id: 'ascii', password: 'depmap-test' },
    { id: 'chinese', password: '银行卡密码测试' },
    { id: 'emoji', password: '\u{1F510}\u{1F511}' },
    { id: 'combining', password: 'e\u0301cole' },
    { id: 'nfc', password: '\u00e9cole' },
  ]
  const observed: Record<string, string> = {}
  for (const c of utf8Cases) {
    const r = await createDepmapContainer(new TextEncoder().encode(GOLDEN_PLAINTEXT), c.password, {
      salt: GOLDEN_SALT,
      nonce: GOLDEN_NONCE,
    })
    observed[c.id] = r.derivedKeyHex
  }
  add(
    'depmap-utf8-password-normalization',
    'depmap',
    'password 使用精确 UTF-8 字节，不做 Unicode 归一化：combining 与 NFC 必须派生出不同密钥',
    { cases: utf8Cases, plaintext: GOLDEN_PLAINTEXT },
    {
      derivedKeyHexByCase: observed,
      combiningDiffersFromNfc: observed['combining'] !== observed['nfc'],
    },
  )
}

// 边界拒绝（恶意容器不得触发超大 KDF）
{
  const base = {
    format: 'depmap',
    formatVersion: 1,
    kdf: {
      algorithm: 'argon2id',
      version: 19,
      salt: 'ABEiM0RVZneImaq7zN3u/w==',
      memoryKiB: 65536,
      iterations: 3,
      parallelism: 1,
    },
    cipher: { algorithm: 'AES-256-GCM', nonce: 'obLD1OX2BxgpOktc' },
    ciphertext: 'AAAA',
    tag: 'AAAAAAAAAAAAAAAAAAAAAA==',
  }
  const mutations: Array<{ id: string; patch: (h: typeof base) => unknown }> = [
    { id: 'not-json', patch: () => 'nope' },
    { id: 'json-array', patch: () => [] },
    { id: 'bad-format', patch: (h) => ({ ...h, format: 'other' }) },
    { id: 'bad-format-version', patch: (h) => ({ ...h, formatVersion: 2 }) },
    { id: 'bad-kdf-algorithm', patch: (h) => ({ ...h, kdf: { ...h.kdf, algorithm: 'pbkdf2' } }) },
    { id: 'bad-kdf-version', patch: (h) => ({ ...h, kdf: { ...h.kdf, version: 16 } }) },
    { id: 'memory-too-large', patch: (h) => ({ ...h, kdf: { ...h.kdf, memoryKiB: 1048576 } }) },
    { id: 'memory-too-small', patch: (h) => ({ ...h, kdf: { ...h.kdf, memoryKiB: 8 } }) },
    { id: 'iterations-too-large', patch: (h) => ({ ...h, kdf: { ...h.kdf, iterations: 99 } }) },
    { id: 'parallelism-too-large', patch: (h) => ({ ...h, kdf: { ...h.kdf, parallelism: 9 } }) },
    { id: 'bad-salt-length', patch: (h) => ({ ...h, kdf: { ...h.kdf, salt: 'AAAA' } }) },
    { id: 'bad-nonce-length', patch: (h) => ({ ...h, cipher: { ...h.cipher, nonce: 'AAAA' } }) },
    { id: 'bad-tag-length', patch: (h) => ({ ...h, tag: 'AAAA' }) },
    { id: 'empty-ciphertext', patch: (h) => ({ ...h, ciphertext: '' }) },
    {
      id: 'bad-cipher-algorithm',
      patch: (h) => ({ ...h, cipher: { ...h.cipher, algorithm: 'AES-128-CBC' } }),
    },
    { id: 'float-param', patch: (h) => ({ ...h, kdf: { ...h.kdf, memoryKiB: 65536.5 } }) },
  ]

  const outcomes: Record<string, string> = {}
  for (const m of mutations) {
    const json =
      typeof m.patch(base) === 'string' ? (m.patch(base) as string) : JSON.stringify(m.patch(base))
    outcomes[m.id] = await openDepmapContainer(json, 'depmap-test').then(
      () => 'OPENED_UNEXPECTEDLY',
      (e: unknown) => (e as { code?: string }).code ?? 'error',
    )
  }
  add(
    'depmap-bounds-and-structure-rejection',
    'depmap',
    '解析 → 结构 → 边界 全部在 KDF 之前完成；恶意容器必须 fail closed',
    {
      base,
      mutations: mutations.map((m) => ({
        id: m.id,
        json: typeof m.patch(base) === 'string' ? m.patch(base) : JSON.stringify(m.patch(base)),
      })),
    },
    outcomes,
  )
}

// ---------------------------------------------------------------------------
// 6. JCS — RFC 8785 受限域
// ---------------------------------------------------------------------------

{
  const jcsCases = [
    { id: 'key-order', input: { b: 1, a: 2 } },
    { id: 'nested', input: { z: { b: true, a: false }, a: [1, 2] } },
    { id: 'escape', input: { k: 'a"b\\c\nd' } },
    { id: 'control-char', input: { k: '\u0001' } },
    { id: 'empty-object', input: {} },
    { id: 'unicode-pass-through', input: { k: '\u94f6\u884c' } },
    { id: 'numbers', input: { i: 0, j: 1234567890 } },
    { id: 'null-bool', input: { n: null, t: true, f: false } },
    { id: 'array-strings', input: ['a', 'b'] },
  ]
  const expected: Record<string, string> = {}
  for (const c of jcsCases) expected[c.id] = jcsStringify(c.input)

  const rejections: Record<string, string> = {}
  // 注意：NaN **不能**作为 reject case —— JSON 无法表达 NaN，
  // `JSON.stringify({f: NaN})` 会写成 `{"f":null}`，平台解析后不会抛错，
  // 造成 "expected=Error / actual=序列化成功" 的假失败。
  // JCS 受限域的浮点拒绝用 1.5 表达即可覆盖同一分支。
  const rejectCases: Array<{ id: string; input: unknown }> = [{ id: 'float', input: { f: 1.5 } }]
  for (const c of rejectCases) {
    rejections[c.id] = (() => {
      try {
        jcsStringify(c.input)
        return 'SERIALIZED_UNEXPECTEDLY'
      } catch (e) {
        return e instanceof Error ? e.name : 'error'
      }
    })()
  }

  add(
    'jcs-rfc8785-restricted-domain',
    'jcs',
    'RFC 8785 JCS（受限值域）：对象键排序、转义、整数最短形式；浮点必须拒绝而非静默序列化',
    { cases: jcsCases, rejectCases },
    { canonical: expected, rejections },
  )
}

// ---------------------------------------------------------------------------
// 7. SCENARIOS — 模板政策 gate（静态）
// ---------------------------------------------------------------------------

{
  const specDomain = JSON.parse(readFileSync(join(ROOT, 'spec', 'domain', 'domain.json'), 'utf8'))
  add(
    'scenario-template-policy',
    'scenario',
    'active 模板必须是 payment capability 且可执行；planned 无 factory 必须被拒绝；注册表不得含日常生活提醒类模板',
    { excludedDomains: specDomain.scenarioTemplates.policy.excludedDomains },
    {
      activeIds: specDomain.scenarioTemplates.active.map((t: { id: string }) => t.id),
      activeCategories: specDomain.scenarioTemplates.active.map(
        (t: { category: string }) => t.category,
      ),
      plannedIds: specDomain.scenarioTemplates.planned.map((t: { id: string }) => t.id),
      plannedExecutable: specDomain.scenarioTemplates.planned.map(
        (t: { executable: boolean }) => t.executable,
      ),
      leadingTimeByTemplate: Object.fromEntries(
        specDomain.scenarioTemplates.active.map(
          (t: { id: string; recommendedLeadTimeDays: number | null }) => [
            t.id,
            t.recommendedLeadTimeDays,
          ],
        ),
      ),
    },
  )
}

// ---------------------------------------------------------------------------
// 8. MIGRATION — schema 与 payload 版本契约（静态）
// ---------------------------------------------------------------------------

{
  const specSchema = JSON.parse(
    readFileSync(join(ROOT, 'spec', 'schema', 'logical-schema.json'), 'utf8'),
  )
  add(
    'migration-version-contract',
    'migration',
    'Schema 迁移链与 payload 版本契约；未来版本必须被明确拒绝而非猜测兼容',
    { logicalSchemaVersion: specSchema.logicalSchemaVersion, migrations: specSchema.migrations },
    {
      migrations: specSchema.migrations,
      payloadKind: specSchema.payloadNote.includes('depmap-logical-graph')
        ? 'depmap-logical-graph'
        : null,
      payloadTables: specSchema.payloadTables,
      rejectedSchemaVersions: [0, 4, 99, 100],
      rejectedPayloadVersions: [0, 4, 99],
      graphRevisionNeverBumpedBy: [
        'evidence_record',
        'proposal_upsert',
        'proposal_decision',
        'candidate_upsert',
        'candidate_dismiss',
        'candidate_accept',
        'drift_detect',
        'drift_dismiss',
        'timeline_build',
        'import_session_record',
        'plan_create',
        'plan_rebase',
        'plan_transition',
        'action_complete',
        'action_verify',
      ],
    },
  )
}

// ---------------------------------------------------------------------------
// 9. STATE MACHINES — 从 spec 导出为 fixture（保证三端与 spec 同源）
// ---------------------------------------------------------------------------

/**
 * 规范化形状（**不是** spec 文档回显）：
 * 只声明"平台必须实现并可被断言"的字段，避免把整份 spec 文档当 fixture，
 * 否则三端只是在比谁复制得快，而不是在实现语义。
 */
{
  const cm = JSON.parse(
    readFileSync(join(ROOT, 'spec', 'state-machines', 'change-plan.json'), 'utf8'),
  )
  // `JSON.parse` 返回 any：这里立刻收敛成递归索引类型，避免 any 沿 machine()
  // 调用链扩散（eslint no-unsafe-return）。spec 里 machine 的字段是「嵌套对象
  // + 标量」混合，递归索引类型既保住 `drift.transitionEffects.accept` 这类访问，
  // 又让返回值不再是 any。
  // 本脚本用到的字段在类型层面声明为**必选**（spec 缺字段时生成的 fixture 会
  // 与冻结的 CONFORMANCE_MANIFEST 不一致，差分会捕获），其余字段走索引签名。
  // 这样既去掉 any，又不撒谎成"字段可能不存在"（那会让 fixture 静默变 undefined）。
  type Machine = {
    machineId: string
    initial: unknown
    transitions: unknown
    creationRule: unknown
    guards: unknown
    transitionEffects: { accept: unknown; dismiss: unknown }
    [k: string]: unknown
  }
  const sm = JSON.parse(
    readFileSync(join(ROOT, 'spec', 'state-machines', 'state-machines.json'), 'utf8'),
  ) as { machines: Machine[] }
  const machine = (id: string): Machine => {
    const m = sm.machines.find((x) => x.machineId === id)
    if (m === undefined) throw new Error(`state-machines.json: missing machineId=${id}`)
    return m
  }

  add(
    'state-machine-change-plan',
    'state-machine',
    'ChangePlan 合法迁移表（终态不可迁出）；派生 needs_revalidation 规则',
    { fields: ['workflowState', 'currentGraphRevision', 'lastAnalyzedGraphRevision'] },
    {
      transitions: cm.transitions,
      terminal: cm.terminal,
      derivedStatus: cm.derivedStatus,
      guards: cm.guards,
    },
  )

  const drift = machine('RealityDriftStatus')
  add(
    'state-machine-reality-drift',
    'state-machine',
    'RealityDrift：非 open 不可再处理；创建/忽略不改 Reality 与 revision',
    { fields: ['status', 'observationCount', 'evidenceRefs'] },
    {
      initial: drift.initial,
      transitions: drift.transitions,
      creationRule: drift.creationRule,
      guards: drift.guards,
    },
  )

  const cand = machine('DiscoveryCandidateStatus')
  add(
    'state-machine-discovery-candidate',
    'state-machine',
    'DiscoveryCandidate：accept 幂等且不 bump revision；dismiss 后需 ≥2 新观测才回到 pending',
    { fields: ['status', 'observationCount'] },
    {
      initial: cand.initial,
      transitions: cand.transitions,
      accept: cand.transitionEffects.accept,
      dismiss: cand.transitionEffects.dismiss,
      guards: cand.guards,
    },
  )

  const verif = machine('ActionVerificationStatus')
  add(
    'state-machine-action-verification',
    'state-machine',
    'Verification：done ≠ verified；verified/failed/not_required 不可被 evidence suggestion 覆盖',
    { fields: ['verification.status', 'verification.method', 'done'] },
    {
      initial: verif.initial,
      terminal: verif.terminal,
      transitions: verif.transitions,
      evidenceSignalRule: verif.evidenceSignalRule,
      guards: verif.guards,
    },
  )

  const rev = machine('GraphRevision')
  add(
    'state-machine-graph-revision',
    'state-machine',
    'GraphRevision：只有 Confirmed Reality Mutation 才 bump；与 mutation 同事务',
    { fields: ['meta.graph_revision'] },
    {
      initial: rev.initial,
      monotonic: rev.monotonic,
      atomicity: rev.atomicity,
      bumpsOn: rev.bumpsOn,
      neverBumpsOn: rev.neverBumpsOn,
    },
  )
}

// ---------------------------------------------------------------------------
// 10. IMPORT — 原始输入文件（复制，平台中立）
// ---------------------------------------------------------------------------

const importFiles = [
  'normal-wechat.csv',
  'utf8-bom.csv',
  'gbk.csv',
  'header-offset.csv',
  'refund.csv',
  'duplicate-import.csv',
  'same-amount-twice.csv',
  'malformed.csv',
  'recurring-monthly.csv',
  'non-recurring.csv',
  'dup-jan-jun.csv',
  'dup-jan-aug.csv',
  'csv-cr-only.csv',
  'csv-crlf.csv',
  'csv-debit-credit-columns.csv',
  'csv-eu-bank-semicolon.csv',
  'csv-gb18030.csv',
  'csv-multi-currency.csv',
  'csv-quoted-comma.csv',
  'csv-us-credit-card.csv',
  'ofx-basic.qfx',
  'qfx-basic.qfx',
  'ofx-multiple.ofx',
  'ofx-missing-fitid.ofx',
  'ofx-invalid-date.ofx',
  'ofx-negative-positive.ofx',
  'ofx-malformed.ofx',
  'qfx-duplicate-fitid.ofx',
]

const importDir = join(FIX, 'import')
mkdirSync(importDir, { recursive: true })
const importHashes: Record<string, string> = {}
for (const f of importFiles) {
  const src = join(CORE, 'tests', 'fixtures', f)
  if (!existsSync(src)) {
    console.error(`MISSING source fixture: ${f}`)
    continue
  }
  const buf = readFileSync(src)
  writeFileSync(join(importDir, f), buf)
  importHashes[f] = createHash('sha256').update(buf).digest('hex')
}

// ---------------------------------------------------------------------------
// 11. PARSER — WeChat / Generic CSV / OFX-QFX
//
// 隐私纪律：Observation.sourceTxnId 是"仅会话内存"字段，
// **不写入 fixture**（否则等于把交易号持久化进仓库）。
// fixture 只比对可持久化的解析语义：时间 / 金额 / 币种 / 方向 / 商户 / 状态。
// ---------------------------------------------------------------------------

{
  const ctx = {
    sourceInstance: {
      id: 'fx-src',
      adapterId: 'generic_csv',
      adapterVersion: 1,
      sourceKind: 'statement_file' as const,
      label: 'fixture',
      state: 'active' as const,
      createdAt: T0,
      updatedAt: T0,
    },
  }

  const simpleCsv: MappingProfile = {
    columns: {
      dateTime: 'date',
      counterparty: 'counterparty',
      amount: 'amount',
      currency: 'currency',
    },
    options: {
      delimiter: ',',
      dateFormats: ['YYYY-MM-DD'],
      decimalSeparator: '.',
      amountSignMode: 'outward_positive',
      hasHeaderRow: true,
    },
  }
  // csv-quoted-comma.csv 只有 date,counterparty,amount（无 currency 列）
  const simpleCsvNoCurrency: MappingProfile = {
    columns: { dateTime: 'date', counterparty: 'counterparty', amount: 'amount' },
    options: { ...simpleCsv.options },
  }
  const simpleCsvDesc: MappingProfile = {
    columns: {
      dateTime: 'date',
      description: 'description',
      amount: 'amount',
      currency: 'currency',
    },
    options: { ...simpleCsv.options },
  }
  const euBank: MappingProfile = {
    columns: {
      dateTime: 'Buchungstag',
      counterparty: 'Verwendungszweck',
      amount: 'Betrag',
      currency: 'Waehrung',
    },
    options: {
      delimiter: ';',
      dateFormats: ['DD.MM.YYYY'],
      decimalSeparator: ',',
      amountSignMode: 'outward_positive',
      hasHeaderRow: true,
    },
  }
  const debitCredit: MappingProfile = {
    columns: {
      dateTime: 'Date',
      counterparty: 'Description',
      debit: 'Debit',
      credit: 'Credit',
      balance: 'Balance',
    },
    options: {
      delimiter: ',',
      dateFormats: ['YYYY-MM-DD'],
      decimalSeparator: '.',
      amountSignMode: 'debit_credit',
      hasHeaderRow: true,
    },
  }
  const usCard: MappingProfile = {
    columns: { dateTime: 'Transaction Date', counterparty: 'Description', amount: 'Amount' },
    options: {
      delimiter: ',',
      dateFormats: ['MM/DD/YYYY'],
      decimalSeparator: '.',
      amountSignMode: 'signed',
      positiveDirection: 'out',
      hasHeaderRow: true,
    },
  }
  const gb18030Csv: MappingProfile = {
    ...simpleCsv,
    options: { ...simpleCsv.options, encoding: 'gb18030' },
  }

  const parserCases: Array<{
    id: string
    adapter: 'wechat' | 'generic_csv' | 'ofx_qfx'
    file: string
    mapping?: MappingProfile
  }> = [
    { id: 'wechat-normal', adapter: 'wechat', file: 'normal-wechat.csv' },
    { id: 'wechat-utf8-bom', adapter: 'wechat', file: 'utf8-bom.csv' },
    { id: 'wechat-gb18030', adapter: 'wechat', file: 'gbk.csv' },
    { id: 'wechat-header-offset', adapter: 'wechat', file: 'header-offset.csv' },
    { id: 'wechat-malformed', adapter: 'wechat', file: 'malformed.csv' },
    { id: 'wechat-refund', adapter: 'wechat', file: 'refund.csv' },
    { id: 'csv-crlf', adapter: 'generic_csv', file: 'csv-crlf.csv', mapping: simpleCsv },
    { id: 'csv-cr-only', adapter: 'generic_csv', file: 'csv-cr-only.csv', mapping: simpleCsv },
    {
      id: 'csv-quoted-comma',
      adapter: 'generic_csv',
      file: 'csv-quoted-comma.csv',
      mapping: simpleCsvNoCurrency,
    },
    {
      id: 'csv-multi-currency',
      adapter: 'generic_csv',
      file: 'csv-multi-currency.csv',
      mapping: simpleCsvDesc,
    },
    {
      id: 'csv-eu-semicolon',
      adapter: 'generic_csv',
      file: 'csv-eu-bank-semicolon.csv',
      mapping: euBank,
    },
    {
      id: 'csv-debit-credit',
      adapter: 'generic_csv',
      file: 'csv-debit-credit-columns.csv',
      mapping: debitCredit,
    },
    {
      id: 'csv-us-signed',
      adapter: 'generic_csv',
      file: 'csv-us-credit-card.csv',
      mapping: usCard,
    },
    { id: 'csv-gb18030', adapter: 'generic_csv', file: 'csv-gb18030.csv', mapping: gb18030Csv },
    { id: 'ofx-basic', adapter: 'ofx_qfx', file: 'ofx-basic.qfx' },
    { id: 'ofx-xml-multiple', adapter: 'ofx_qfx', file: 'ofx-multiple.ofx' },
    { id: 'ofx-missing-fitid', adapter: 'ofx_qfx', file: 'ofx-missing-fitid.ofx' },
    { id: 'ofx-invalid-date', adapter: 'ofx_qfx', file: 'ofx-invalid-date.ofx' },
    { id: 'ofx-negative-positive', adapter: 'ofx_qfx', file: 'ofx-negative-positive.ofx' },
    { id: 'ofx-malformed', adapter: 'ofx_qfx', file: 'ofx-malformed.ofx' },
    { id: 'qfx-basic', adapter: 'ofx_qfx', file: 'qfx-basic.qfx' },
    { id: 'qfx-duplicate-fitid', adapter: 'ofx_qfx', file: 'qfx-duplicate-fitid.ofx' },
  ]

  // WeChatStatementAdapter 的构造需要一个 driver（它内部持有 NodeRepository）。
  // 这里给一个一次性临时库，仅为满足构造契约：本段 22 条 parser 用例只断言
  // `parse()` 的观测输出与坏行保守拒绝，读取结果与库内容无关。
  const wechatDb = freshDb('parser-wechat')
  for (const c of parserCases) {
    const data = new Uint8Array(readFileSync(join(CORE, 'tests', 'fixtures', c.file)))
    const adapter =
      c.adapter === 'wechat'
        ? new WeChatStatementAdapter(wechatDb.driver)
        : c.adapter === 'ofx_qfx'
          ? new OfxQfxAdapter()
          : new GenericCsvAdapter()
    const observations: Observation[] = await (
      adapter as { parse: (i: unknown, ctx: unknown) => Promise<Observation[]> }
    ).parse({ data, fileName: c.file, mapping: c.mapping }, ctx)
    const errors = adapter.lastParseErrors?.() ?? []

    add(
      `parser-${c.id}`,
      'parser',
      `${c.adapter} 解析 ${c.file}：观测语义 + 坏行保守拒绝（sourceTxnId 不进 fixture）`,
      { adapterId: c.adapter, file: c.file, mapping: c.mapping ?? null },
      {
        observationCount: observations.length,
        observations: observations.map((o) => ({
          occurredAt: o.occurredAt,
          amount: o.amount,
          currency: o.currency,
          direction: o.direction,
          merchantRaw: o.merchantRaw,
          status: o.status,
        })),
        errorCount: errors.length,
        errors: errors.map((e) => ({ line: e.line, reason: e.reason })),
      },
    )
  }
  closeDb(wechatDb.driver, wechatDb.dir)
}

// ---------------------------------------------------------------------------
// 12. TIMELINE — 纯投影（不写库、可重建、排序确定）
// ---------------------------------------------------------------------------

const TIMELINE_NOW = '2026-09-13T00:00:00.000Z'

function freshDb(tag: string): { driver: NodeSqliteDriver; dir: string } {
  const dir = mkdtempSync(join(tmpdir(), `depmap-cf-${tag}-`))
  const driver = new NodeSqliteDriver(join(dir, 'fx.db'))
  driver.open()
  migrate(driver, T0)
  return { driver, dir }
}

function closeDb(driver: NodeSqliteDriver, dir: string): void {
  driver.close()
  try {
    rmSync(dir, { recursive: true, force: true })
  } catch {
    /* best effort */
  }
}

function insertNode(
  driver: NodeSqliteDriver,
  id: string,
  kind: string,
  name: string,
  fields = '{}',
): void {
  driver
    .prepare(
      `INSERT INTO nodes (id, kind, template_id, name, issuer, last4, owner, archived, fields_json, vault_ref, wallet_ref, created_at, updated_at)
       VALUES (?, ?, NULL, ?, NULL, NULL, 'self', 0, ?, NULL, NULL, ?, ?)`,
    )
    .run(id, kind, name, fields, T0, T0)
}

function insertPlan(
  driver: NodeSqliteDriver,
  id: string,
  title: string,
  effectiveDate: string | null,
  lastAnalyzed: number,
  workflowState = 'analyzed',
): void {
  driver
    .prepare(
      `INSERT INTO change_plans (id, template_id, scenario, title, workflow_state, baseline_graph_revision, last_analyzed_graph_revision, target_node_id, effective_date, params_json, impact_snapshot_json, action_items_json, created_at, updated_at)
       VALUES (?, 'replace_payment_card', 'replace_payment_card', ?, ?, 0, ?, NULL, ?, '{}', NULL, '[]', ?, ?)`,
    )
    .run(id, title, workflowState, lastAnalyzed, effectiveDate, T0, T0)
}

{
  // (a) 分桶边界与确定性排序
  const { driver, dir } = freshDb('tl-buckets')
  insertNode(driver, 'n-card', 'payment_instrument', '招行 4417')
  const dates = ['2026-01-01', '2026-09-13', '2026-09-15', '2026-09-25', '2026-10-20', '2027-06-01']
  dates.forEach((d, i) => insertPlan(driver, `plan-${i + 1}`, `计划 ${i + 1}`, d, 5))
  const items = buildTimeline(driver, TIMELINE_NOW)
  const again = buildTimeline(driver, TIMELINE_NOW)
  add(
    'timeline-buckets-and-ordering',
    'timeline',
    '7 个分桶边界 + 确定性排序（bucket → priority 降序 → scheduledAt → id）；同一状态两次构建必须完全相同',
    { now: TIMELINE_NOW, plans: dates, graphRevision: 5 },
    {
      buckets: items.map((i) => i.bucket),
      ids: items.map((i) => i.id),
      kinds: items.map((i) => i.kind),
      deterministic: JSON.stringify(items) === JSON.stringify(again),
      planCountUnchanged: driver.prepare('SELECT COUNT(*) AS c FROM change_plans').get()?.c,
    },
  )
  closeDb(driver, dir)
}

{
  // (b) drift / freshness / expiry / attention
  const { driver, dir } = freshDb('tl-signals')
  insertNode(driver, 'n-card', 'payment_instrument', '招行 4417', '{"expiryDate":"2026-10-05"}')
  insertNode(driver, 'n-wechat', 'account', '微信支付')
  driver
    .prepare(
      `INSERT INTO reality_drifts (id, kind, target_node_id, capability, candidate_from, candidate_relation, related_dependency_ids_json, evidence_refs_json, proposal_keys_json, observation_count, detected_at, updated_at, status)
       VALUES ('drift-1', 'possible_replacement', 'n-wechat', 'payment', 'n-card', 'funding_source', '[]', '[]', '[]', 2, ?, ?, 'open')`,
    )
    .run(T0, T0)
  driver
    .prepare(
      `INSERT INTO source_instances (id, adapter_id, adapter_version, source_kind, provider_id, account_node_id, label, country, jurisdiction, currencies_json, state, created_at, updated_at, last_ingested_at)
       VALUES ('inst-stale', 'generic_csv', 1, 'statement_file', NULL, NULL, '陈旧来源', NULL, NULL, '["CNY"]', 'active', ?, ?, '2026-01-01T00:00:00.000Z')`,
    )
    .run(T0, T0)
  insertPlan(driver, 'plan-stale', '落后计划', null, 0)
  const items = buildTimeline(driver, TIMELINE_NOW)
  add(
    'timeline-attention-signals',
    'timeline',
    '开放 Drift → drift_review(attention)；过期来源 → freshness_review(attention)；到期 → expiration；revision 落后 → needs_attention',
    { now: TIMELINE_NOW, graphRevision: 5, freshnessThresholdDays: 45 },
    {
      kinds: items.map((i) => i.kind),
      buckets: items.map((i) => i.bucket),
      sourceTypes: items.map((i) => i.sourceType),
      priorities: items.map((i) => i.priority),
      statuses: items.map((i) => i.status),
      allTraceable: items.every((i) => i.sourceId.length > 0),
    },
  )
  closeDb(driver, dir)
}

{
  // (c) completed / cancelled 计划不产生 timeline 项
  const { driver, dir } = freshDb('tl-terminal')
  insertNode(driver, 'n-card', 'payment_instrument', '招行 4417')
  insertPlan(driver, 'plan-done', '已完成', '2026-09-20', 5, 'completed')
  insertPlan(driver, 'plan-cancel', '已取消', '2026-09-21', 5, 'cancelled')
  const items = buildTimeline(driver, TIMELINE_NOW)
  add(
    'timeline-terminal-plans-excluded',
    'timeline',
    'completed / cancelled 计划不产生 timeline 项（TL-007）',
    { now: TIMELINE_NOW, graphRevision: 5 },
    { count: items.length, kinds: items.map((i) => i.kind) },
  )
  closeDb(driver, dir)
}

// ---------------------------------------------------------------------------
// 13. MIGRATION — DB v1 → v3（事务 / 回滚 / 幂等 / ID 保留）
// ---------------------------------------------------------------------------

{
  const { driver, dir } = freshDb('mig')
  // 手工构造 v1 库
  driver.close()
  rmSync(join(dir, 'fx.db'), { force: true })
  const d1 = new NodeSqliteDriver(join(dir, 'v1.db'))
  d1.open()
  for (const sql of SCHEMA_V1_STATEMENTS) d1.exec(sql)
  d1.prepare(`INSERT INTO meta (key, value) VALUES ('schema_version', '1')`).run()
  d1.prepare(
    `INSERT INTO nodes (id, kind, name, owner, archived, fields_json, created_at, updated_at)
     VALUES ('node-1', 'payment_instrument', '招行 4417', 'self', 0, '{}', ?, ?)`,
  ).run(T0, T0)
  d1.prepare(
    `INSERT INTO dependencies (id, from_node, relation, to_node, capability, criticality, state, origin, confirmed_at, last_verified_at, evidence_refs_json, created_at, updated_at)
     VALUES ('dep-1', 'node-1', 'funding_source', 'node-1', 'payment', 'required', 'active', 'manual', ?, ?, '[]', ?, ?)`,
  ).run(T0, T0, T0, T0)
  d1.prepare(
    `INSERT INTO evidence (id, proposal_key, source_type, parser_id, parser_version, last_import_session_id, first_observed_at, last_observed_at, observation_count, created_at, updated_at)
     VALUES ('ev-1', 'k1', 'wechat_bill', 'wechat', 1, 'sess-1', ?, ?, 3, ?, ?)`,
  ).run(T0, T0, T0, T0)
  d1.prepare(
    `INSERT INTO observation_fingerprints (fingerprint, source, fingerprint_version, import_session_id, first_seen_at)
     VALUES ('fp-abc', 'wechat', 1, 'sess-1', ?)`,
  ).run(T0)
  d1.prepare(
    `INSERT INTO dependency_proposals (id, key, from_node, relation, to_node, capability, proposal_type, source, parser_id, parser_version, confidence_score, path_json, evidence_id, decision, observation_count, created_at, updated_at)
     VALUES ('prop-1', 'k1', 'node-1', 'funding_source', 'node-1', 'payment', 'recurring_payment_route', 'statement', 'wechat', 1, 0.9, '[]', 'ev-1', 'pending', 3, ?, ?)`,
  ).run(T0, T0)
  d1.prepare(
    `INSERT INTO import_sessions (id, source_type, parser_id, parser_version, started_at, completed_at, raw_count, new_unique_count, duplicate_count, proposal_count, error_count)
     VALUES ('sess-1', 'wechat_bill', 'wechat', 1, ?, ?, 6, 6, 0, 1, 0)`,
  ).run(T0, T0)

  const finalVersion = migrate(d1, T0)
  const tables = d1
    .prepare(
      `SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name`,
    )
    .all()
    .map((r) => String((r as Record<string, unknown>).name))
  const dep = d1
    .prepare('SELECT id, state, criticality FROM dependencies WHERE id = ?')
    .get('dep-1')
  const node = d1.prepare('SELECT id, name FROM nodes WHERE id = ?').get('node-1')
  const ev = d1
    .prepare(
      'SELECT id, proposal_key, source_instance_id, evidence_kind FROM evidence WHERE id = ?',
    )
    .get('ev-1')
  const fp = d1
    .prepare(
      'SELECT fingerprint, source_instance_id FROM observation_fingerprints WHERE fingerprint = ?',
    )
    .get('fp-abc')
  const prop = d1
    .prepare('SELECT id, decision FROM dependency_proposals WHERE id = ?')
    .get('prop-1')
  const legacyCount = d1
    .prepare('SELECT COUNT(*) AS c FROM source_instances WHERE id = ?')
    .get(LEGACY_WECHAT_SOURCE_INSTANCE_ID)

  // 幂等：重复 migrate 50 次不漂移
  let idempotent = true
  for (let i = 0; i < 50; i++) if (migrate(d1, T0) !== finalVersion) idempotent = false

  add(
    'migration-db-v1-to-v3',
    'migration',
    'DB v1 → v3：版本到达 3、legacy 数据归属 legacy SourceInstance、ID 与决策保留、指纹作用域重建、重复执行严格 no-op',
    {
      from: 1,
      to: finalVersion,
      seeded: ['node-1', 'dep-1', 'ev-1', 'fp-abc', 'prop-1', 'sess-1'],
    },
    {
      finalSchemaVersion: finalVersion,
      tables,
      legacySourceInstanceId: LEGACY_WECHAT_SOURCE_INSTANCE_ID,
      legacySourceInstanceRows: Number((legacyCount as Record<string, unknown>).c),
      preserved: { node, dependency: dep, evidence: ev, fingerprint: fp, proposal: prop },
      idempotentAfter50: idempotent,
    },
  )
  closeDb(d1, dir)
}

// ---------------------------------------------------------------------------
// 14. BACKUP — .depmap 导出 / 恢复往返（Cutover 核心要求）
// ---------------------------------------------------------------------------

{
  const { driver, dir } = freshDb('bak')
  insertNode(driver, 'b-card', 'payment_instrument', '招行 4417')
  insertNode(driver, 'b-wechat', 'account', '微信支付')
  driver
    .prepare(
      `INSERT INTO dependencies (id, from_node, relation, to_node, capability, criticality, state, origin, confirmed_at, last_verified_at, evidence_refs_json, verification_basis_type, created_at, updated_at)
       VALUES ('b-dep', 'b-card', 'funding_source', 'b-wechat', 'payment', 'required', 'active', 'manual', ?, ?, '[]', 'user_confirmed', ?, ?)`,
    )
    .run(T0, T0, T0, T0)

  const exported = exportGraph(driver)
  const BACKUP_SALT = Uint8Array.from(Buffer.from('00112233445566778899aabbccddeeff', 'hex'))
  const BACKUP_NONCE = Uint8Array.from(Buffer.from('a1b2c3d4e5f60718293a4b5c', 'hex'))
  const container = await createDepmapContainer(
    new TextEncoder().encode(exported.payloadJson),
    'depmap-backup',
    { salt: BACKUP_SALT, nonce: BACKUP_NONCE },
  )
  const reopened = await openDepmapContainer(container.json, 'depmap-backup')
  const decrypted = new TextDecoder().decode(reopened.plaintext)

  // 恢复到全新库
  const dir2 = mkdtempSync(join(tmpdir(), 'depmap-cf-restore-'))
  const d2 = new NodeSqliteDriver(join(dir2, 'restore.db'))
  d2.open()
  migrate(d2, T0)
  const imported = importGraph(d2, decrypted)
  const reExported = exportGraph(d2)
  const integrity = checkGraphIntegrity(d2)

  add(
    'backup-depmap-export-restore-roundtrip',
    'backup',
    '导出 payload → 加密为 .depmap → 解密 → 恢复到新库 → 再次导出必须逐字节相同；且无孤儿引用',
    {
      password: 'depmap-backup',
      saltHex: '00112233445566778899aabbccddeeff',
      nonceHex: 'a1b2c3d4e5f60718293a4b5c',
      seededNodes: ['b-card', 'b-wechat'],
      seededDependencies: ['b-dep'],
    },
    {
      payloadJson: exported.payloadJson,
      counts: exported.counts,
      containerJson: container.json,
      decryptedEqualsPayload: decrypted === exported.payloadJson,
      imported: imported.imported,
      roundtripPayloadJson: reExported.payloadJson,
      roundtripEqual: reExported.payloadJson === exported.payloadJson,
      integrity,
    },
  )

  closeDb(driver, dir)
  closeDb(d2, dir2)
}

// ---------------------------------------------------------------------------
// 15. V0.3.0 — FAILURE DOMAIN / PATH INDEPENDENCE（Canonical vNext）
// ---------------------------------------------------------------------------

{
  function fdDep(
    id: string,
    from: string,
    to: string,
    capability: 'recovery' | 'authentication' | 'access' = 'recovery',
    state: 'active' | 'retired' = 'active',
  ): Dependency {
    return {
      id,
      from,
      relation: 'recovers' as const,
      to,
      capability,
      criticality: 'unknown',
      groupId: null,
      state,
      origin: 'manual',
      confirmedAt: T0,
      lastVerifiedAt: T0,
      retiredAt: state === 'retired' ? T0 : null,
      evidenceRefs: [],
      verificationBasis: { type: 'user_confirmed', verifiedAt: T0 },
      createdAt: T0,
      updatedAt: T0,
    }
  }

  function fdInput(
    dependencies: Dependency[],
    confirmedDomains: Array<{ key: string; id: string }>,
    suspectedDomains: Array<{ key: string; id: string }>,
    edgeToDomainKeys: Record<string, string[]>,
  ) {
    return {
      targetNodeId: 'wechat',
      capability: 'recovery',
      dependencies,
      confirmedDomains,
      suspectedDomains,
      edgeToDomainKeys,
    }
  }

  function fdRun(
    id: string,
    description: string,
    deps: Dependency[],
    confirmed: Array<{ key: string; id: string }>,
    suspected: Array<{ key: string; id: string }>,
    edgeToDomain: Record<string, string[]>,
  ) {
    const confirmedMap = new Map(confirmed.map((d) => [d.key, d.id]))
    const suspectedMap = new Map(suspected.map((d) => [d.key, d.id]))
    const edgeMap = new Map(Object.entries(edgeToDomain))
    const result = computePathIndependence({
      targetNodeId: 'wechat',
      capability: 'recovery',
      dependencies: deps,
      confirmedDomains: confirmedMap,
      suspectedDomains: suspectedMap,
      edgeToDomainKeys: edgeMap,
    })
    add(
      id,
      'failure-domain',
      description,
      fdInput(deps, confirmed, suspected, edgeToDomain),
      result,
    )
  }

  // FD-01 两条路径同一台手机 → independent 1
  fdRun(
    'fd-2-paths-same-phone-independent-1',
    'SMS 恢复与 Passkey 恢复都依赖 phone-a → pathCount=2, independentPathCount=1（V030-IP-01）',
    [fdDep('d1', 'phone-a', 'wechat'), fdDep('d2', 'phone-a', 'wechat')],
    [{ key: 'DEVICE|phone-a', id: 'fd-device-1' }],
    [],
    { d1: ['DEVICE|phone-a'], d2: ['DEVICE|phone-a'] },
  )

  // FD-02 两条路径不同设备 → independent 2
  fdRun(
    'fd-2-paths-different-devices-independent-2',
    'SMS 恢复依赖 phone-a、Passkey 依赖 phone-b → pathCount=2, independentPathCount=2',
    [fdDep('d1', 'phone-a', 'wechat'), fdDep('d2', 'phone-b', 'wechat')],
    [
      { key: 'DEVICE|phone-a', id: 'fd-device-a' },
      { key: 'DEVICE|phone-b', id: 'fd-device-b' },
    ],
    [],
    { d1: ['DEVICE|phone-a'], d2: ['DEVICE|phone-b'] },
  )

  // FD-03 共享 provider 未确认 → needs_review（不自动 confirmed）
  fdRun(
    'fd-shared-provider-unconfirmed-needs-review',
    '两条路径同属 ChinaMobile，但 provider 共享未确认 → sharedFailureDomains 空、unresolvedAssumptions 非空',
    [fdDep('d1', 'phone-a', 'wechat'), fdDep('d2', 'phone-b', 'wechat')],
    [],
    [{ key: 'PROVIDER|ChinaMobile', id: 'fd-provider-1' }],
    { d1: ['PROVIDER|ChinaMobile'], d2: ['PROVIDER|ChinaMobile'] },
  )

  // FD-04 共享 provider 已确认 → sharedFailureDomains 非空
  fdRun(
    'fd-shared-provider-confirmed',
    '两条路径同属 ChinaMobile 且 provider 共享已确认 → sharedFailureDomains=[fd-provider-1]',
    [fdDep('d1', 'phone-a', 'wechat'), fdDep('d2', 'phone-b', 'wechat')],
    [{ key: 'PROVIDER|ChinaMobile', id: 'fd-provider-1' }],
    [],
    { d1: ['PROVIDER|ChinaMobile'], d2: ['PROVIDER|ChinaMobile'] },
  )

  // FD-05 retired 边排除
  fdRun(
    'fd-retired-edge-excluded',
    'retired 边不参与路径统计 → pathCount=0, independentPathCount=0',
    [fdDep('d1', 'phone-a', 'wechat', 'recovery', 'retired')],
    [],
    [],
    { d1: ['DEVICE|phone-a'] },
  )

  // FD-06 混合 capability：authentication 边不参与 recovery 统计
  fdRun(
    'fd-mixed-capability-excluded',
    '认证边（authentication）不参与 recovery 路径统计 → pathCount=0',
    [fdDep('d1', 'phone-a', 'wechat', 'authentication')],
    [],
    [],
    { d1: ['DEVICE|phone-a'] },
  )
}

// ---------------------------------------------------------------------------
// 16. V0.3.0 — RECOVERY CYCLE（Canonical vNext）
// ---------------------------------------------------------------------------

{
  function rcDep(
    id: string,
    from: string,
    to: string,
    capability: 'recovery' | 'access' | 'authentication' = 'recovery',
    state: 'active' | 'retired' = 'active',
  ): Dependency {
    return {
      id,
      from,
      relation: 'recovers' as const,
      to,
      capability,
      criticality: 'unknown',
      groupId: null,
      state,
      origin: 'manual',
      confirmedAt: T0,
      lastVerifiedAt: T0,
      retiredAt: state === 'retired' ? T0 : null,
      evidenceRefs: [],
      verificationBasis: { type: 'user_confirmed', verifiedAt: T0 },
      createdAt: T0,
      updatedAt: T0,
    }
  }

  function rcRun(
    id: string,
    description: string,
    capability: 'recovery',
    deps: Dependency[],
    hinted: Array<{ from: string; to: string }>,
  ) {
    const result = detectRecoveryCycles({ capability, dependencies: deps, hintedEdges: hinted })
    add(
      id,
      'recovery-cycle',
      description,
      { capability, dependencies: deps, hintedEdges: hinted },
      result,
    )
  }

  rcRun(
    'rc-simple-cycle',
    '简单环 account-a→phone-a→account-a（confirmed）→ confirmed_cycle',
    'recovery',
    [rcDep('d1', 'account-a', 'phone-a'), rcDep('d2', 'phone-a', 'account-a')],
    [],
  )

  rcRun(
    'rc-multihop-cycle',
    '多跳环 account-a→phone-a→email-a→account-a → confirmed_cycle',
    'recovery',
    [
      rcDep('d1', 'account-a', 'phone-a'),
      rcDep('d2', 'phone-a', 'email-a'),
      rcDep('d3', 'email-a', 'account-a'),
    ],
    [],
  )

  rcRun('rc-no-cycle', '无环 → no_cycle', 'recovery', [rcDep('d1', 'phone-a', 'account-a')], [])

  rcRun(
    'rc-unconfirmed-edge-potential-only',
    '未确认边（proposal hint）只产生 potential_cycle，绝不 confirmed',
    'recovery',
    [rcDep('d1', 'account-a', 'phone-a')],
    [{ from: 'phone-a', to: 'account-a' }],
  )

  rcRun(
    'rc-retired-edge-excluded',
    'retired 边不参与检测 → no_cycle',
    'recovery',
    [
      rcDep('d1', 'account-a', 'phone-a', 'recovery', 'retired'),
      rcDep('d2', 'phone-a', 'account-a', 'recovery', 'retired'),
    ],
    [],
  )

  rcRun(
    'rc-mixed-capability-no-cycle',
    '混合 capability 不产生 confirmed cycle → no_cycle',
    'recovery',
    [
      rcDep('d1', 'account-a', 'phone-a', 'recovery'),
      rcDep('d2', 'phone-a', 'account-a', 'access'),
    ],
    [],
  )

  rcRun(
    'rc-potential-cycle-only',
    '只有 hint 边的环 → potential_cycle only（confirmedCycles 空）',
    'recovery',
    [],
    [
      { from: 'account-a', to: 'phone-a' },
      { from: 'phone-a', to: 'account-a' },
    ],
  )
}

// ---------------------------------------------------------------------------
// 17. V0.3.0 — ACTION DAG（prerequisiteActionIds）
// ---------------------------------------------------------------------------

{
  function dagAction(id: string, prereqs: string[] = []): PlanAction {
    return {
      id,
      title: id,
      detail: '',
      phase: 'change',
      done: false,
      doneAt: null,
      verification: null,
      resolvesImpactKeys: [],
      prerequisiteActionIds: prereqs,
    }
  }

  function dagRun(id: string, description: string, actions: PlanAction[]) {
    const result = validateActionDag({ actions })
    add(id, 'action-dag', description, { actions }, result)
  }

  dagRun('dag-linear-chain', '线性链 a→b→c：stable topo order [a,b,c]', [
    dagAction('c', ['b']),
    dagAction('b', ['a']),
    dagAction('a'),
  ])

  dagRun('dag-parallel-actions', '并行动作 + 汇合点：stable topo order 确定', [
    dagAction('b'),
    dagAction('a'),
    dagAction('c', ['a', 'b']),
  ])

  dagRun('dag-cycle-reject', '环 a→b→a 必须被拒绝', [dagAction('a', ['b']), dagAction('b', ['a'])])

  dagRun('dag-missing-prerequisite-reject', '引用不存在的前置动作 → missing_prerequisite 拒绝', [
    dagAction('a', ['ghost']),
  ])

  dagRun('dag-completed-prerequisite-ok', '前置已完成仍合法（校验不关心 done 状态）', [
    dagAction('b', ['a']),
    dagAction('a'),
  ])

  dagRun('dag-verification-prerequisite', '验证动作作为前置：verify 必须在 retire 之前', [
    dagAction('retire', ['verify']),
    dagAction('verify'),
  ])

  dagRun('dag-stable-topo-order', '稳定拓扑顺序：相同输入两次输出相同', [
    dagAction('z'),
    dagAction('y'),
    dagAction('x', ['z']),
    dagAction('w', ['y', 'x']),
  ])
}

// ---------------------------------------------------------------------------
// 18. V0.3.0 — MAKE-BEFORE-BREAK
// ---------------------------------------------------------------------------

{
  function mbbAction(id: string, verified: boolean, done = true): PlanAction {
    return {
      id,
      title: id,
      detail: '',
      phase: 'change',
      done,
      doneAt: done ? T0 : null,
      verification: verified
        ? { method: 'manual_confirmation', status: 'verified', verifiedAt: T0, evidenceRefs: [] }
        : null,
      resolvesImpactKeys: [],
    }
  }

  function mbbRun(
    id: string,
    description: string,
    newPaths: PlanAction[],
    verifications: PlanAction[],
    retireDone = false,
  ) {
    const result = evaluateMakeBeforeBreak({
      newPathActions: newPaths,
      verificationActions: verifications,
      retireActionId: 'rpn-change-2',
      retireAlreadyDone: retireDone,
    })
    add(id, 'make-before-break', description, { newPaths, verifications, retireDone }, result)
  }

  mbbRun(
    'mbb-new-path-not-verified-retire-blocked',
    '新路径未建立/未验证 → retire blocked（BREAK_BEFORE_MAKE = FORBIDDEN）',
    [mbbAction('rpn-change-1', false, false)],
    [mbbAction('rpn-verify-1', false, false)],
  )

  mbbRun(
    'mbb-new-path-verified-retire-allowed',
    '新路径已建立且已验证 → retire allowed',
    [mbbAction('rpn-change-1', false, true)],
    [mbbAction('rpn-verify-1', true, true)],
  )

  mbbRun(
    'mbb-done-not-verified-blocked',
    '新路径 done 但未 verified → 仍 blocked（done ≠ verified）',
    [mbbAction('rpn-change-1', false, true)],
    [mbbAction('rpn-verify-1', false, true)],
  )
}

// ---------------------------------------------------------------------------
// 19. V0.3.0 — TEMPORAL CHANGE（transition window）
// ---------------------------------------------------------------------------

{
  function tcRun(
    id: string,
    description: string,
    window: TemporalChangeWindow,
    now: string,
    verified: boolean,
  ) {
    const result = classifyTemporalPhase(window, now, verified)
    add(id, 'temporal-change', description, { window, now, verified }, result)
  }

  tcRun(
    'tc-before-phase',
    'now < effectiveAt → before；retire 未开放',
    {
      effectiveAt: '2026-10-01T00:00:00.000Z',
      verificationNotBefore: null,
      verificationDueAt: null,
      retireOldPathAfter: null,
    },
    '2026-09-20T00:00:00.000Z',
    false,
  )

  tcRun(
    'tc-transition-phase-verified-retire-allowed',
    'transition 中 + 全部新路径已验证 → retireAllowedAt 给出',
    {
      effectiveAt: '2026-10-01T00:00:00.000Z',
      verificationNotBefore: '2026-10-01T00:00:00.000Z',
      verificationDueAt: '2026-10-15T00:00:00.000Z',
      retireOldPathAfter: '2026-10-20T00:00:00.000Z',
    },
    '2026-10-21T00:00:00.000Z',
    true,
  )

  tcRun(
    'tc-transition-unverified-blocked',
    'transition 中 + 新路径未验证 → retire blocked 即使时间已到',
    {
      effectiveAt: '2026-10-01T00:00:00.000Z',
      verificationNotBefore: null,
      verificationDueAt: null,
      retireOldPathAfter: '2026-10-20T00:00:00.000Z',
    },
    '2026-10-21T00:00:00.000Z',
    false,
  )

  tcRun(
    'tc-invalid-order',
    'verificationNotBefore < effectiveAt → 时间窗顺序不合法',
    {
      effectiveAt: '2026-10-02T00:00:00.000Z',
      verificationNotBefore: '2026-10-01T00:00:00.000Z',
      verificationDueAt: null,
      retireOldPathAfter: null,
    },
    '2026-09-20T00:00:00.000Z',
    false,
  )
}

// ---------------------------------------------------------------------------
// 20. V0.3.0 — PROVIDER POLICY（Provider Knowledge Plane）
// ---------------------------------------------------------------------------

{
  function ppRun(
    id: string,
    description: string,
    policy: ProviderPolicy | null,
    userConfigured: boolean,
  ) {
    const interpretation = interpretProviderCapability(policy, userConfigured)
    const state = policy
      ? policy.state
      : inferProviderPolicyState({
          provider: 'unknown',
          policyType: 'recovery',
          sourceUrl: null,
          retrievedAt: T0,
          lastVerifiedAt: null,
          policyRevision: 0,
        })
    add(
      id,
      'provider-policy',
      description,
      { policy, userConfigured },
      { ...interpretation, state },
    )
  }

  const effectivePolicy: ProviderPolicy = {
    provider: 'ChinaMobile',
    policyType: 'recovery',
    sourceUrl: 'https://www.10086.cn/policy',
    retrievedAt: T0,
    lastVerifiedAt: T0,
    effectiveFrom: null,
    effectiveTo: null,
    jurisdiction: 'CN',
    accountTypeScope: null,
    parameters: { sms_recovery: true },
    policyRevision: 3,
    state: 'effective',
  }

  ppRun(
    'pp-supports-but-not-configured',
    'Provider 支持 SMS 恢复但用户未配置 → suggestion',
    effectivePolicy,
    false,
  )
  ppRun(
    'pp-supports-and-configured',
    'Provider 支持且用户已配置 → interpretation',
    effectivePolicy,
    true,
  )
  ppRun(
    'pp-unverifiable-needs-review',
    '无法核实的 policy → needs_review，不自动影响建议',
    null,
    true,
  )

  const needsReviewPolicy: ProviderPolicy = { ...effectivePolicy, state: 'needs_review' }
  ppRun(
    'pp-state-needs-review-no-auto-influence',
    'needs_review policy 不产生 suggestion',
    needsReviewPolicy,
    false,
  )
}

// ---------------------------------------------------------------------------
// 21. V0.3.0 — IDENTITY & RECOVERY RELATIONS（recovers/authenticates/controls）
// ---------------------------------------------------------------------------

{
  function relRun(
    id: string,
    description: string,
    fromKind: string | null,
    relation: string,
    toKind: string | null,
    capability: string,
  ) {
    const result = validateRelationUse(fromKind as never, relation, toKind as never, capability)
    add(id, 'identity-relations', description, { fromKind, relation, toKind, capability }, result)
  }

  relRun(
    'rel-recovers-phone-to-account',
    'recovers：identity_anchor(phone) → account, recovery 放行',
    'identity_anchor',
    'recovers',
    'account',
    'recovery',
  )
  relRun(
    'rel-recovers-rejects-payment-capability',
    'recovers 要求 recovery；payment 被拒',
    'identity_anchor',
    'recovers',
    'account',
    'payment',
  )
  relRun(
    'rel-authenticates-device-to-account',
    'authenticates：device → account, authentication 放行',
    'device',
    'authenticates',
    'account',
    'authentication',
  )
  relRun(
    'rel-authenticates-rejects-recovery-capability',
    'authenticates 要求 authentication；recovery 被拒',
    'device',
    'authenticates',
    'account',
    'recovery',
  )
  relRun(
    'rel-controls-account-to-service',
    'controls：account → service, access 放行',
    'account',
    'controls',
    'service',
    'access',
  )
  relRun(
    'rel-verifies-still-not-runtime',
    'verifies 仍不进入 runtime registry（v0.3.0 保持）',
    'account',
    'verifies',
    'account',
    'payment',
  )
}

// ---------------------------------------------------------------------------
// write fixtures
// ---------------------------------------------------------------------------

const verifyMode = process.argv.includes('--verify')
const GENERATED_CATEGORIES = [
  'impact',
  'readiness',
  'coverage',
  'relations',
  'depmap',
  'jcs',
  'scenario',
  'migration',
  'state-machine',
  'parser',
  'timeline',
  'backup',
  'failure-domain',
  'recovery-cycle',
  'action-dag',
  'make-before-break',
  'temporal-change',
  'provider-policy',
  'identity-relations',
]

if (verifyMode) {
  // Oracle 自检：重算全部用例并与磁盘上的 fixture 逐字节比较。
  // 用途：作为 conformance harness 的第一步，确认 oracle 与 fixture 未漂移。
  let drift = 0
  for (const c of cases) {
    const p = join(FIX, c.category, `${c.id}.json`)
    const payload = {
      id: c.id,
      category: c.category,
      specVersion: '1.0.0',
      appSchemaVersion: 3,
      oracle: 'core (TypeScript reference) @ LEGACY_REFERENCE_MANIFEST.md',
      description: c.description,
      input: c.input,
      expected: c.expected,
    }
    const text = JSON.stringify(payload, null, 2) + '\n'
    if (!existsSync(p)) {
      console.error(`DRIFT missing fixture: fixtures/${c.category}/${c.id}.json`)
      drift++
      continue
    }
    if (readFileSync(p, 'utf8') !== text) {
      console.error(`DRIFT oracle output differs: fixtures/${c.category}/${c.id}.json`)
      drift++
    }
  }
  if (drift > 0) {
    console.error(`\nORACLE SELFCHECK: FAIL (${drift} case(s) drifted)`)
    console.error('If the new behaviour is intended, change spec/ first, then regenerate fixtures.')
    process.exit(1)
  }
  console.log(`ORACLE SELFCHECK: PASS (${cases.length} cases reproduce exactly)`)
  process.exit(0)
}

const written: Array<{
  id: string
  category: string
  path: string
  sha256: string
  description: string
}> = []

if (existsSync(FIX)) {
  // 只清理本次生成的类别目录，避免误删手工 fixture
  for (const c of GENERATED_CATEGORIES) {
    rmSync(join(FIX, c), { recursive: true, force: true })
  }
}

for (const c of cases) {
  const dir = join(FIX, c.category)
  mkdirSync(dir, { recursive: true })
  const payload = {
    id: c.id,
    category: c.category,
    specVersion: '1.0.0',
    appSchemaVersion: 3,
    oracle: 'core (TypeScript reference) @ LEGACY_REFERENCE_MANIFEST.md',
    description: c.description,
    input: c.input,
    expected: c.expected,
  }
  const text = JSON.stringify(payload, null, 2) + '\n'
  const p = join(dir, `${c.id}.json`)
  writeFileSync(p, text, 'utf8')
  written.push({
    id: c.id,
    category: c.category,
    path: `fixtures/${c.category}/${c.id}.json`,
    sha256: createHash('sha256').update(text).digest('hex'),
    description: c.description,
  })
}

// ---------------------------------------------------------------------------
// manifest
// ---------------------------------------------------------------------------

let oracleCommit = 'unknown'
try {
  oracleCommit = execFileSync('git', ['rev-parse', 'HEAD'], { cwd: ROOT, encoding: 'utf8' }).trim()
} catch {
  /* git unavailable */
}

const byCategory: Record<string, number> = {}
for (const w of written) byCategory[w.category] = (byCategory[w.category] ?? 0) + 1

const manifest = {
  $comment:
    'PDIG Conformance Manifest. Every fixture here is generated from the frozen TypeScript oracle. Expected outputs for the same input MUST be reproduced byte-for-byte by the Kotlin/Swift/ArkTS implementations. Do NOT regenerate expected values without changing spec/ first.',
  manifestVersion: 1,
  specVersion: '1.0.0',
  appSchemaVersion: 3,
  generator: 'core/scripts/generate-conformance.ts',
  oracle: {
    kind: 'frozen-typescript-reference',
    commit: oracleCommit,
    manifest: 'LEGACY_REFERENCE_MANIFEST.md',
    frozenTag: 'v0.3.0-uniapp-reference',
    baseline: 'Test Files 43 passed, Tests 453 passed',
  },
  counts: { cases: written.length, byCategory },
  importFixtures: {
    directory: 'fixtures/import/',
    count: Object.keys(importHashes).length,
    sha256: importHashes,
  },
  fixtures: written,
}

mkdirSync(CONF, { recursive: true })
writeFileSync(
  join(CONF, 'CONFORMANCE_MANIFEST.json'),
  JSON.stringify(manifest, null, 2) + '\n',
  'utf8',
)

console.log(`conformance fixtures: ${written.length} cases written`)
console.log(`import fixtures: ${Object.keys(importHashes).length} files copied`)
console.log(`manifest: conformance/CONFORMANCE_MANIFEST.json (oracle ${oracleCommit})`)
