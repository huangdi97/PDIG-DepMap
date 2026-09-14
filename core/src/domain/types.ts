import type { VerificationBasis } from './source.ts'

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
  /** Schema v2：Reality 如何被验证。MVP02 恒为 user_confirmed（三个 Adapter 均为 event_stream）。 */
  verificationBasis: VerificationBasis | null
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
  /** Schema v2：Group 也必须记录验证基础（MVP02 恒 user_confirmed）。 */
  verificationBasis: VerificationBasis | null
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
  /** Schema v2：多源 provenance（EvidenceSummary id 集合，跨 SourceInstance 可多个） */
  evidenceRefs: string[]
  decision: ProposalDecision
  decidedAt: string | null
  /** 用户确认关系时可同时决定 criticality；null = 未明确决定（默认按 unknown 处理） */
  criticalityDecision: Criticality | null
  /** 全源累计 observationCount（仅展示；重提判断禁止跨流相加） */
  observationCount: number
  rejectedAt: string | null
  /** Schema v2：拒绝时每个 Evidence stream 的计数快照（单流阈值判定用） */
  rejectedAtStreamCounts: Record<string, number> | null
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
  /** Schema v2：Evidence 按 SourceInstance 分流（同一 proposal 多个 provenance） */
  sourceInstanceId: string
  adapterId: string
  adapterVersion: number
  /** MVP02: 'transaction_stream' */
  evidenceKind: string
  /** 展示用来源类型（legacy: 'wechat_bill'） */
  sourceType: string
  parserId: string
  parserVersion: number
  lastImportSessionId: string
  firstObservedAt: string
  lastObservedAt: string
  /** 本 stream 内累计的 unique observation 数（禁止跨 stream 相加驱动决策） */
  observationCount: number
  createdAt: string
  updatedAt: string
}

export interface ImportSession {
  id: string
  sourceType: string
  parserId: string
  parserVersion: number
  /** Schema v2：来源实例与 Adapter provenance */
  sourceInstanceId: string | null
  adapterId: string | null
  adapterVersion: number | null
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
