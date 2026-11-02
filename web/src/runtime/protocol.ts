/**
 * PDIG Web Runtime Protocol（UI ⇄ Domain Worker）。
 *
 * 铁律：
 * - UI 不认识 SQL、不认识表名、不认识 Core Repository；只消费 projection DTO。
 * - 所有 Truth（continuity / stale / readiness / findings / verification / rebase）
 *   只能由 Worker 内的 Core 计算。
 * - After（计划投影）永远不等于 Reality；DTO 上带 `projection` 显式标记。
 */
import type {
  Capability,
  Criticality,
  DepNode,
  Dependency,
  DependencyGroup,
  DependencyGroupProposal,
  DependencyProposal,
  NodeKind,
  Relation,
} from '@core/domain/types.ts'
import type {
  ChangePlan,
  ChangePlanWorkflowState,
  ImpactSnapshot,
  PlanAction,
  PlanEffectiveStatus,
  PlanReadiness,
  PlanRebaseDiff,
} from '@core/domain/change-plan.ts'
import type { InfrastructureFinding } from '@core/domain/infrastructure-findings.ts'
import type { SourceInstance } from '@core/domain/source.ts'
import type { MappingProfile } from '@core/sources/types.ts'
import type { DiscoveryCandidate } from '@core/repositories/discovery-candidate-repository.ts'
import type { RealityDrift } from '@core/repositories/reality-drift-repository.ts'

// ---------------------------------------------------------------------------
// 错误模型（内部错误码；用户文案由 UI 映射，不暴露底层细节）
// ---------------------------------------------------------------------------

export type WebErrorCode =
  | 'AUTH_FAILED'
  | 'TAMPERED'
  | 'UNSUPPORTED_FORMAT'
  | 'CORRUPT_VAULT'
  | 'KDF_BOUNDS'
  | 'STORAGE_FULL'
  | 'STORAGE_ERROR'
  | 'LEASE_LOST'
  | 'STATE_LOCKED'
  | 'VALIDATION'
  | 'NOT_FOUND'
  | 'CONFLICT'
  | 'IMPORT_FAILED'
  | 'FILE_TOO_LARGE'
  | 'UNKNOWN'

export interface RpcError {
  code: WebErrorCode
  message: string
  recoverable: boolean
}

// ---------------------------------------------------------------------------
// 生命周期
// ---------------------------------------------------------------------------

export type VaultLifecycle =
  | 'no_vault'
  | 'locked'
  | 'unlocking'
  | 'unlocked'
  | 'locking'
  | 'auth_failed'
  | 'corrupt_vault'
  | 'unsupported_vault'
  | 'storage_error'
  | 'lease_denied'

export interface StorageFacts {
  /** navigator.storage.persisted()；false = 浏览器可能清理本地数据。 */
  persisted: boolean
  /** 已请求 persist，返回 false 表示被拒绝。 */
  persistRequested: boolean
  usageBytes: number | null
  quotaBytes: number | null
}

export interface VaultStatus {
  lifecycle: VaultLifecycle
  hasVault: boolean
  graphRevision: number
  schemaVersion: number
  autoLockMinutes: number
  lastSavedAt: string | null
  /** 最近一次 mutation 的持久化是否成功。false = 变更未安全保存。 */
  persisted: boolean
  storage: StorageFacts
  demoMode: boolean
  errorCode: WebErrorCode | null
}

// ---------------------------------------------------------------------------
// 投影 DTO
// ---------------------------------------------------------------------------

export interface NodeSummary {
  id: string
  kind: NodeKind
  name: string
  templateId: string | null
  issuer: string | null
  last4: string | null
  archived: boolean
  dependencyCount: number
  unknownCriticalityCount: number
}

export interface DependencyView {
  id: string
  from: string
  fromName: string
  relation: Relation
  to: string
  toName: string
  capability: Capability
  criticality: Criticality
  groupId: string | null
  state: 'active' | 'retired'
  origin: 'manual' | 'proposal'
  confirmedAt: string
  lastVerifiedAt: string
  retiredAt: string | null
  verificationBasisType: string | null
}

export interface GroupView extends DependencyGroup {
  memberEdges: DependencyView[]
}

export interface FindingView extends InfrastructureFinding {
  relatedNodeIds: string[]
}

export interface ActionView {
  action: PlanAction
  /** 前置动作未完成 → 该动作当前不可被标记完成。 */
  blockedByPrerequisites: string[]
  /** 稳定拓扑序位（1-based）。 */
  order: number
}

export interface PlanSummary {
  id: string
  title: string
  scenario: string
  templateId: string | null
  workflowState: ChangePlanWorkflowState
  effectiveStatus: PlanEffectiveStatus
  readiness: PlanReadiness
  targetNodeId: string | null
  targetName: string | null
  lastAnalyzedGraphRevision: number
  currentGraphRevision: number
  stale: boolean
  actionCount: number
  doneCount: number
  verifiedCount: number
  pendingVerificationCount: number
  effectiveDate: string | null
  updatedAt: string
}

export interface PlanDetailView {
  plan: ChangePlan
  summary: PlanSummary
  actions: ActionView[]
  /** Current：当前 Reality 的模拟（Confirmed Now）。 */
  current: ImpactSnapshot | null
  /** After：计划投影，未发生（Plan Projection）。 */
  projection: ImpactSnapshot | null
  /** 最近一次 rebase 的真实差异（非硬编码）。 */
  lastRebase: { at: string; fromRevision: number; toRevision: number; diff: PlanRebaseDiff } | null
  dagViolations: string[]
  readiness: PlanReadiness
}

export interface RecordView {
  id: string
  kind:
    | 'action_completion'
    | 'action_verification'
    | 'change_plan'
    | 'reality_drift'
    | 'node_expiry'
    | 'source_freshness'
  title: string
  subtitle: string
  /** 现实发生时间：Core 不记录时为 null（UI 必须显示「发生时间未记录」）。 */
  occurredAt: string | null
  /** 记录时间：用户/系统写入记录的时间。 */
  recordedAt: string | null
  /** 验证时间：独立验证通过时间。 */
  verifiedAt: string | null
  status: string
  sourceType: string
  sourceId: string
  planId: string | null
  actionId: string | null
  evidenceRefs: string[]
}

export interface NowProjection {
  counts: {
    nodes: number
    dependencies: number
    archivedNodes: number
    unknownCriticality: number
    unknownBoundaries: number
    pendingVerifications: number
    stalePlans: number
    activePlans: number
  }
  byKind: Array<{ kind: NodeKind; count: number }>
  keyInfrastructure: NodeSummary[]
  changes: PlanSummary[]
  attention: FindingView[]
  pendingVerification: Array<{
    planId: string
    planTitle: string
    actionId: string
    actionTitle: string
    doneAt: string | null
  }>
  nextSteps: Array<{ id: string; label: string; detail: string; route: string }>
}

export interface InfrastructureProjection {
  counts: Array<{ kind: NodeKind; count: number }>
  archivedCount: number
  nodes: NodeSummary[]
  templates: Array<{ kind: NodeKind; label: string }>
}

export interface NodeDetailProjection {
  node: DepNode
  outgoing: DependencyView[]
  incoming: DependencyView[]
  groups: GroupView[]
  findings: FindingView[]
  plans: PlanSummary[]
  records: RecordView[]
  changeTemplates: Array<{ id: string; title: string; requiredInputs: string[] }>
  changeBoundary: string | null
}

export interface FindingsProjection {
  findings: FindingView[]
  counts: Record<string, number>
}

export interface PlansProjection {
  plans: PlanSummary[]
  templates: Array<{
    id: string
    title: string
    description: string
    requiredInputs: Array<{ key: string; description: string }>
    supportedCapabilities: Capability[]
  }>
  graphRevision: number
}

export interface RecordsProjection {
  records: RecordView[]
}

export interface ImportStateProjection {
  sources: SourceInstance[]
  candidates: DiscoveryCandidate[]
  proposals: Array<DependencyProposal & { fromName: string; toName: string }>
  groupProposals: DependencyGroupProposal[]
  drifts: Array<RealityDrift & { targetName: string; fromName: string }>
  adapters: Array<{ id: string; label: string; requiresMapping: boolean }>
  lastSession: {
    id: string
    adapterId: string | null
    startedAt: string
    completedAt: string | null
    rawCount: number
    newUniqueCount: number
    duplicateCount: number
    proposalCount: number
    errorCount: number
  } | null
}

export interface SearchResult {
  type: 'node' | 'plan' | 'finding' | 'record' | 'source'
  id: string
  title: string
  detail: string
  route: string
}

export interface SearchProjection {
  query: string
  results: SearchResult[]
}

export interface DiagnosticsProjection {
  graphRevision: number
  schemaVersion: number
  counts: Record<string, number>
  integrityOrphans: Record<string, number>
  templates: string[]
  lastSavedAt: string | null
  storage: StorageFacts
}

// ---------------------------------------------------------------------------
// Commands / Queries
// ---------------------------------------------------------------------------

export interface NodeCreatePayload {
  kind: NodeKind
  name: string
  templateId?: string | null
  issuer?: string | null
  last4?: string | null
  fields?: Record<string, unknown>
}

export interface NodePatchPayload {
  name?: string
  issuer?: string | null
  last4?: string | null
  fields?: Record<string, unknown>
  archived?: boolean
}

export type WebCommand =
  | { type: 'create_vault'; password: string }
  | { type: 'unlock'; password: string }
  | { type: 'lock' }
  | { type: 'create_node'; payload: NodeCreatePayload }
  | { type: 'update_node'; id: string; patch: NodePatchPayload }
  | { type: 'archive_node'; id: string; archived: boolean }
  | {
      type: 'confirm_dependency'
      from: string
      relation: Relation
      to: string
      capability: Capability
      criticality: Criticality
    }
  | { type: 'retire_dependency'; id: string }
  | {
      type: 'create_group'
      targetNodeId: string
      capability: Capability
      mode: 'ANY' | 'ALL'
      memberEdgeIds: string[]
    }
  | { type: 'retire_group'; id: string }
  | { type: 'accept_proposal'; key: string; criticality: Criticality }
  | { type: 'reject_proposal'; key: string }
  | { type: 'detect_group_proposals' }
  | { type: 'accept_group_proposal'; key: string }
  | { type: 'reject_group_proposal'; key: string }
  | { type: 'create_plan'; templateId: string; inputs: Record<string, string> }
  | { type: 'analyze_plan'; planId: string }
  | { type: 'transition_plan'; planId: string; state: ChangePlanWorkflowState }
  | { type: 'complete_action'; planId: string; actionId: string }
  | { type: 'verify_action'; planId: string; actionId: string }
  | {
      type: 'import_file'
      fileName: string
      dataBase64: string
      adapterId: string
      /** Generic CSV 必须由用户显式给出字段映射（禁止自动猜测）。 */
      mapping?: MappingProfile
    }
  | { type: 'resolve_merchant'; merchantRaw: string; nodeId: string }
  | { type: 'finalize_import' }
  | { type: 'accept_candidate'; id: string }
  | { type: 'dismiss_candidate'; id: string }
  | { type: 'resolve_drift'; driftId: string; resolution: 'replacement' | 'additional' | 'dismiss' }
  | { type: 'export_backup'; password: string | null }
  | { type: 'restore_backup'; fileText: string; password: string; newVaultPassword: string | null }
  | { type: 'reset_local_data' }
  | { type: 'seed_demo' }

export type WebQuery =
  | { type: 'get_status' }
  | { type: 'get_now' }
  | { type: 'get_infrastructure' }
  | { type: 'get_node_detail'; id: string }
  | { type: 'get_findings' }
  | { type: 'get_plans' }
  | { type: 'get_plan_detail'; id: string }
  | { type: 'get_records' }
  | { type: 'get_import_state' }
  | { type: 'search'; query: string }
  | { type: 'get_diagnostics' }

export interface CommandResultMap {
  create_vault: VaultStatus
  unlock: VaultStatus
  lock: VaultStatus
  create_node: { id: string }
  update_node: { id: string }
  archive_node: { id: string }
  confirm_dependency: { id: string; reactivated: boolean; verified: boolean }
  retire_dependency: { id: string }
  create_group: { id: string; reactivated: boolean }
  retire_group: { id: string }
  accept_proposal: { id: string; created: boolean }
  reject_proposal: { ok: true }
  detect_group_proposals: { keys: string[] }
  accept_group_proposal: { id: string }
  reject_group_proposal: { ok: true }
  create_plan: { id: string }
  analyze_plan: { id: string; revisionChanged: boolean; addedImpacts: number }
  transition_plan: { id: string; state: ChangePlanWorkflowState }
  complete_action: { id: string }
  verify_action: { id: string }
  import_file: {
    sessionId: string
    rawCount: number
    candidateCount: number
    unresolvedCount: number
    errorCount: number
    merchants: Array<{
      merchantRaw: string
      resolvedNodeId: string | null
      observationCount: number
      resolution: string
    }>
  }
  resolve_merchant: { ok: true }
  finalize_import: {
    sessionId: string
    newUniqueCount: number
    duplicateCount: number
    proposalKeys: string[]
    unresolvedMerchants: string[]
  }
  accept_candidate: { nodeId: string | null }
  dismiss_candidate: { ok: true }
  resolve_drift: { ok: true }
  export_backup: { fileName: string; containerJson: string; formatVersion: number }
  restore_backup: { graphRevision: number; counts: Record<string, number> }
  reset_local_data: { ok: true }
  seed_demo: { ok: true }
}

export interface QueryResultMap {
  get_status: VaultStatus
  get_now: NowProjection
  get_infrastructure: InfrastructureProjection
  get_node_detail: NodeDetailProjection
  get_findings: FindingsProjection
  get_plans: PlansProjection
  get_plan_detail: PlanDetailView
  get_records: RecordsProjection
  get_import_state: ImportStateProjection
  search: SearchProjection
  get_diagnostics: DiagnosticsProjection
}

export type RequestBody =
  | { id: number; kind: 'command'; command: WebCommand }
  | { id: number; kind: 'query'; query: WebQuery }

export type ResponseBody =
  { id: number; ok: true; data: unknown } | { id: number; ok: false; error: RpcError }

export type { ImpactSnapshot } from '@core/domain/change-plan.ts'

export type CommandType = WebCommand['type']
export type QueryType = WebQuery['type']

export function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}
