/**
 * DepMap domain types — Schema v1 (CANONICAL_DESIGN.md §5)
 *
 * 语义铁律 (AGENTS.md §3 / CANONICAL_DESIGN §0.4):
 * - Observation ≠ Dependency
 * - Proposal ≠ Reality
 * - Dependency 存在即用户确认
 * - DependencyGroup 只能由用户确认产生
 */

// ---------------------------------------------------------------------------
// Node
// ---------------------------------------------------------------------------

export type NodeKind =
  | 'identity_anchor'
  | 'payment_instrument'
  | 'account'
  | 'service'
  | 'membership'
  | 'device'
  | 'custom'

export interface DepNode {
  id: string
  kind: NodeKind
  templateId: string | null
  name: string
  issuer: string | null
  last4: string | null
  owner: string
  archived: boolean
  fields: Record<string, unknown>
  vaultRef: string | null
  walletRef: string | null
  createdAt: string
  updatedAt: string
}

// ---------------------------------------------------------------------------
// Dependency
// ---------------------------------------------------------------------------

export type Relation =
  'funding_source' | 'merchant_agreement' | 'verifies' | 'recovers' | 'bound_to'

/** MVP Impact domain 只支持 payment；其他 capability 仅允许存边、不参与传播。 */
export type Capability = 'payment' | 'access' | 'recovery' | 'identity'

/** MVP criticality 只有 required | unknown；禁止机器自动产生 required。 */
export type Criticality = 'required' | 'unknown'

export type DependencyState = 'active' | 'retired'
export type DependencyOrigin = 'manual' | 'proposal'

export interface Dependency {
  id: string
  from: string
  relation: Relation
  to: string
  capability: Capability
  criticality: Criticality
  groupId: string | null
  state: DependencyState
  origin: DependencyOrigin
  confirmedAt: string
  lastVerifiedAt: string
  retiredAt: string | null
  evidenceRefs: string[]
  createdAt: string
  updatedAt: string
}

export interface DependencyLogicalKeyParts {
  from: string
  relation: Relation
  to: string
  capability: Capability
}

/** logical key: `from|relation|to|capability` — 数据库 UNIQUE。 */
export function dependencyLogicalKey(d: DependencyLogicalKeyParts): string {
  return `${d.from}|${d.relation}|${d.to}|${d.capability}`
}

// ---------------------------------------------------------------------------
// DependencyGroup
// ---------------------------------------------------------------------------

export type GroupMode = 'ANY' | 'ALL'
export type GroupState = 'active' | 'retired'

export interface DependencyGroup {
  id: string
  groupKey: string
  targetNodeId: string
  capability: Capability
  mode: GroupMode
  memberEdgeIds: string[]
  state: GroupState
  confirmedAt: string
  lastVerifiedAt: string
  createdAt: string
  updatedAt: string
}

/**
 * deterministic groupKey:
 * `target|capability|mode|sorted(memberLogicalKeys...)`
 */
export function canonicalGroupKey(
  targetNodeId: string,
  capability: Capability,
  mode: GroupMode,
  memberLogicalKeys: string[],
): string {
  const sorted = [...new Set(memberLogicalKeys)].sort((a, b) => (a < b ? -1 : a > b ? 1 : 0))
  return [targetNodeId, capability, mode, ...sorted].join('|')
}

// ---------------------------------------------------------------------------
// Proposal (非图实体；每条 suggestion 独立一行)
// ---------------------------------------------------------------------------

export type ProposalDecision = 'pending' | 'accepted' | 'rejected'

export interface DependencyProposal {
  id: string
  /** 全局唯一: `from|relation|to|capability` */
  key: string
  from: string
  relation: Relation
  to: string
  capability: Capability
  /** MVP: 'recurring_payment_route' */
  proposalType: string
  /** MVP: 'statement' */
  source: string
  parserId: string
  parserVersion: number
  confidenceScore: number
  /** UI 上下文路径，如 [card, wechat, service] */
  path: string[]
  evidenceId: string | null
  decision: ProposalDecision
  decidedAt: string | null
  /** 用户确认关系时可同时决定 criticality；null = 未明确决定（默认按 unknown 处理） */
  criticalityDecision: Criticality | null
  /** 该 key 当前累计 evidence observationCount（冗余存储，便于重提判断） */
  observationCount: number
  rejectedAt: string | null
  rejectedAtObservationCount: number | null
  createdAt: string
  updatedAt: string
}

export interface DependencyGroupProposal {
  id: string
  /** `group|target|capability|MODE|sortedMemberKeys...` */
  key: string
  targetNodeId: string
  capability: Capability
  mode: GroupMode
  memberDependencyKeys: string[]
  decision: ProposalDecision
  decidedAt: string | null
  rejectedAt: string | null
  rejectedAtObservationCount: number | null
  createdAt: string
  updatedAt: string
}

export function groupProposalKey(
  targetNodeId: string,
  capability: Capability,
  mode: GroupMode,
  memberLogicalKeys: string[],
): string {
  return `group|${canonicalGroupKey(targetNodeId, capability, mode, memberLogicalKeys)}`
}

// ---------------------------------------------------------------------------
// Evidence / Fingerprint / ImportSession
// ---------------------------------------------------------------------------

/** Evidence 是某个 Proposal key 的累计证据摘要，不是交易史。 */
export interface Evidence {
  id: string
  /** 对应 DependencyProposal.key */
  proposalKey: string
  sourceType: string
  parserId: string
  parserVersion: number
  lastImportSessionId: string
  firstObservedAt: string
  lastObservedAt: string
  /** 只累计新 unique observations */
  observationCount: number
  createdAt: string
  updatedAt: string
}

export interface ObservationFingerprintRecord {
  fingerprint: string
  source: string
  fingerprintVersion: number
  importSessionId: string
  firstSeenAt: string
}

export interface ImportSession {
  id: string
  sourceType: string
  parserId: string
  parserVersion: number
  startedAt: string
  completedAt: string | null
  rawCount: number
  newUniqueCount: number
  duplicateCount: number
  proposalCount: number
  errorCount: number
}

// ---------------------------------------------------------------------------
// Observation — 仅导入会话内存，绝不持久化 (CANONICAL_DESIGN §5.3)
// ---------------------------------------------------------------------------

export type ObservationDirection = 'in' | 'out' | 'neutral'

export interface Observation {
  source: string
  sourceTxnId: string | null
  merchantTxnId: string | null
  occurredAt: string
  merchantRaw: string
  description: string
  /** 正数金额，方向由 direction 表达 */
  amount: number
  currency: string
  direction: ObservationDirection
  paymentMethodRaw: string
  status: string
  note: string
}
