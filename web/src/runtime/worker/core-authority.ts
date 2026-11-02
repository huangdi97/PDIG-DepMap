/**
 * CoreAuthority —— Worker 内的唯一 Truth 权威。
 *
 * 所有 projection / command 都经真实 PDIG Core（Repository + Service + Kernel）。
 * 此处不允许出现任何“UI 级推断”：
 * - 不写 SQL 语义规则（只用 Core Repository / Service 的公开方法）
 * - 不计算 readiness / stale / verification（用 Core 的纯函数）
 * - findings 由 Core `generateInfrastructureFindings` 产出，输入只来自已确认 Reality
 */
import type { SqliteDriver } from '@core/db/driver.ts'
import { LATEST_SCHEMA_VERSION, currentSchemaVersion, migrate } from '@core/schema/migrations.ts'
import { checkGraphIntegrity, exportGraph, importGraph } from '@core/services/graph-serialize.ts'
import { getGraphRevision } from '@core/repositories/graph-revision.ts'
import { NodeRepository } from '@core/repositories/node-repository.ts'
import { DependencyRepository } from '@core/repositories/dependency-repository.ts'
import { DependencyGroupRepository } from '@core/repositories/group-repository.ts'
import { DependencyProposalRepository } from '@core/repositories/proposal-repository.ts'
import { DependencyGroupProposalRepository } from '@core/repositories/group-proposal-repository.ts'
import { DiscoveryCandidateRepository } from '@core/repositories/discovery-candidate-repository.ts'
import { RealityDriftRepository } from '@core/repositories/reality-drift-repository.ts'
import { ChangePlanRepository } from '@core/repositories/change-plan-repository.ts'
import { SourceInstanceRepository } from '@core/repositories/source-instance-repository.ts'
import { ImportSessionRepository } from '@core/repositories/import-session-repository.ts'
import { ChangePlanService } from '@core/services/change-plan-service.ts'
import {
  analyzePlanImpact,
  claimUnassignedImpacts,
  rebasePlan,
} from '@core/services/plan-analysis.ts'
import { computePlanReadiness } from '@core/services/plan-readiness.ts'
import { ConfirmationService } from '@core/services/confirmation-service.ts'
import { DiscoveryService } from '@core/services/discovery-service.ts'
import { RealityDriftService } from '@core/services/reality-drift-service.ts'
import { ImportCoordinator } from '@core/services/import-coordinator.ts'
import { buildTimeline } from '@core/services/timeline.ts'
import { instantiateScenario, listActiveTemplates } from '@core/scenarios/registry.ts'
import { generateInfrastructureFindings } from '@core/domain/infrastructure-findings.ts'
import { detectRecoveryCycles } from '@core/impact/recovery-cycle.ts'
import { validateActionDag } from '@core/domain/action-dag.ts'
import { evaluateMakeBeforeBreak } from '@core/domain/make-before-break.ts'
import { effectiveStatus, isPlanStale } from '@core/domain/change-plan.ts'
import { WeChatStatementAdapter } from '@core/sources/wechat/adapter.ts'
import { GenericCsvAdapter } from '@core/sources/generic-csv/adapter.ts'
import { OfxQfxAdapter } from '@core/sources/ofx/adapter.ts'
import type { EvidenceSourceAdapter } from '@core/sources/types.ts'
import type {
  Capability,
  DepNode,
  Dependency,
  DependencyGroup,
  NodeKind,
} from '@core/domain/types.ts'
import { dependencyLogicalKey } from '@core/domain/types.ts'
import type { ChangePlan, PlanRebaseDiff } from '@core/domain/change-plan.ts'
import type {
  ActionView,
  DependencyView,
  DiagnosticsProjection,
  FindingView,
  FindingsProjection,
  GroupView,
  ImportStateProjection,
  InfrastructureProjection,
  NodeDetailProjection,
  NodeSummary,
  NowProjection,
  PlanDetailView,
  PlanSummary,
  PlansProjection,
  RecordView,
  RecordsProjection,
  SearchProjection,
  WebCommand,
  WebQuery,
} from '../protocol.ts'

const NODE_KIND_LABELS: Array<{ kind: NodeKind; label: string }> = [
  { kind: 'identity_anchor', label: '身份锚点' },
  { kind: 'payment_instrument', label: '支付工具' },
  { kind: 'account', label: '账户' },
  { kind: 'service', label: '服务' },
  { kind: 'membership', label: '会员' },
  { kind: 'device', label: '设备' },
  { kind: 'custom', label: '自定义' },
]

const CHANGE_BOUNDARY_BY_KIND: Partial<Record<NodeKind, string>> = {
  account: '账户变更尚未开放正式可执行模板；不会用通用步骤替代真实变更语义。',
  service: '服务迁移尚未开放正式可执行模板；不会把关系数量当作迁移计划。',
  membership: '会员变更尚未开放正式可执行模板；影响不会由界面自行推断。',
  device: '设备变更尚未开放正式可执行模板；不会伪造设备关系。',
  custom: '此对象暂不能直接创建变更计划。',
}

const MBB_CAPABILITIES: Capability[] = ['access', 'authentication', 'recovery', 'communication']
const STALE_DEPENDENCY_DAYS = 90

export class AuthorityError extends Error {
  readonly code:
    | 'VALIDATION'
    | 'NOT_FOUND'
    | 'CONFLICT'
    | 'STATE_LOCKED'
    | 'IMPORT_FAILED'
    | 'UNSUPPORTED_FORMAT'
    | 'FILE_TOO_LARGE'

  constructor(code: AuthorityError['code'], message: string) {
    super(message)
    this.name = 'AuthorityError'
    this.code = code
  }
}

export interface AuthorityHooks {
  /** 每次图变更后：exportGraph 的结果交给加密持久化层。失败必须抛出。 */
  persist: (payloadJson: string, graphRevision: number) => Promise<void>
}

export class CoreAuthority {
  readonly driver: SqliteDriver
  private readonly nodes: NodeRepository
  private readonly deps: DependencyRepository
  private readonly groups: DependencyGroupRepository
  private readonly proposals: DependencyProposalRepository
  private readonly groupProposals: DependencyGroupProposalRepository
  private readonly candidates: DiscoveryCandidateRepository
  private readonly drifts: RealityDriftRepository
  private readonly plans: ChangePlanRepository
  private readonly sources: SourceInstanceRepository
  private readonly sessions: ImportSessionRepository
  private readonly changePlans: ChangePlanService
  private readonly confirmations: ConfirmationService
  private readonly discovery: DiscoveryService
  private readonly driftService: RealityDriftService
  private readonly hooks: AuthorityHooks
  private readonly now: () => string

  private importPending: {
    coordinator: ImportCoordinator
    sessionId: string
    adapterId: string
  } | null = null
  private lastImportSummary: ImportStateProjection['lastSession'] = null
  private readonly lastRebase = new Map<
    string,
    { at: string; fromRevision: number; toRevision: number; diff: PlanRebaseDiff }
  >()

  constructor(driver: SqliteDriver, hooks: AuthorityHooks, now: () => string) {
    this.driver = driver
    this.hooks = hooks
    this.now = now
    this.nodes = new NodeRepository(driver)
    this.deps = new DependencyRepository(driver)
    this.groups = new DependencyGroupRepository(driver)
    this.proposals = new DependencyProposalRepository(driver)
    this.groupProposals = new DependencyGroupProposalRepository(driver)
    this.candidates = new DiscoveryCandidateRepository(driver)
    this.drifts = new RealityDriftRepository(driver)
    this.plans = new ChangePlanRepository(driver)
    this.sources = new SourceInstanceRepository(driver)
    this.sessions = new ImportSessionRepository(driver)
    this.changePlans = new ChangePlanService(driver)
    this.confirmations = new ConfirmationService(driver)
    this.discovery = new DiscoveryService(driver)
    this.driftService = new RealityDriftService(driver)
  }

  /** 新建空图（迁移到最新 schema）。 */
  static initialize(driver: SqliteDriver, hooks: AuthorityHooks, now: () => string): CoreAuthority {
    migrate(driver, now(), LATEST_SCHEMA_VERSION)
    return new CoreAuthority(driver, hooks, now)
  }

  /** 从 payload 恢复（restore / unlock 后重建内存图）。 */
  static fromPayload(
    driver: SqliteDriver,
    payloadJson: string,
    hooks: AuthorityHooks,
    now: () => string,
  ): CoreAuthority {
    migrate(driver, now(), LATEST_SCHEMA_VERSION)
    importGraph(driver, payloadJson)
    return new CoreAuthority(driver, hooks, now)
  }

  schemaVersion(): number {
    return currentSchemaVersion(this.driver)
  }

  graphRevision(): number {
    return getGraphRevision(this.driver)
  }

  exportPayload(): string {
    return exportGraph(this.driver).payloadJson
  }

  /** 持久化当前图（persist-before-ack 的落盘步骤）。 */
  async persistNow(): Promise<void> {
    const payload = this.exportPayload()
    await this.hooks.persist(payload, this.graphRevision())
  }

  // -------------------------------------------------------------------------
  // Commands
  // -------------------------------------------------------------------------

  async command(command: WebCommand): Promise<unknown> {
    switch (command.type) {
      case 'create_node': {
        const node = this.nodes.create({
          kind: command.payload.kind,
          name: command.payload.name,
          templateId: command.payload.templateId ?? null,
          issuer: command.payload.issuer ?? null,
          last4: command.payload.last4 ?? null,
          fields: command.payload.fields ?? {},
        })
        await this.commit()
        return { id: node.id }
      }
      case 'update_node': {
        const node = this.nodes.update(command.id, command.patch)
        await this.commit()
        return { id: node.id }
      }
      case 'archive_node': {
        const node = this.nodes.update(command.id, { archived: command.archived })
        await this.commit()
        return { id: node.id }
      }
      case 'confirm_dependency': {
        const result = this.confirmations.addManualDependency({
          from: command.from,
          relation: command.relation,
          to: command.to,
          capability: command.capability,
          criticality: command.criticality,
        })
        await this.commit()
        return { id: result.id, reactivated: false, verified: false }
      }
      case 'retire_dependency': {
        this.assertMakeBeforeBreak(command.id)
        const dependency = this.confirmations.retireDependency(command.id)
        await this.commit()
        return { id: dependency.id }
      }
      case 'create_group': {
        if (command.memberEdgeIds.length < 2) {
          throw new AuthorityError('VALIDATION', '备用路径组合至少需要两条已确认关系')
        }
        const members = command.memberEdgeIds.map((id) => {
          const edge = this.deps.getById(id)
          if (!edge) throw new AuthorityError('NOT_FOUND', `dependency not found: ${id}`)
          return edge
        })
        for (const edge of members) {
          if (edge.to !== command.targetNodeId || edge.capability !== command.capability) {
            throw new AuthorityError('VALIDATION', '组合成员必须与目标对象和 capability 一致')
          }
        }
        const result = this.groups.confirm(
          {
            targetNodeId: command.targetNodeId,
            capability: command.capability,
            mode: command.mode,
            memberEdgeIds: command.memberEdgeIds,
          },
          members.map((edge) =>
            dependencyLogicalKey({
              from: edge.from,
              relation: edge.relation,
              to: edge.to,
              capability: edge.capability,
            }),
          ),
        )
        for (const edge of members) this.deps.setGroupId(edge.id, result.group.id)
        await this.commit()
        return { id: result.group.id, reactivated: result.reactivated }
      }
      case 'retire_group': {
        const group = this.groups.retire(command.id)
        await this.commit()
        return { id: group.id }
      }
      case 'accept_proposal': {
        const result = this.confirmations.acceptProposal(command.key, command.criticality)
        await this.commit()
        return { id: result.dependency.id, created: result.created }
      }
      case 'reject_proposal': {
        this.confirmations.rejectProposal(command.key)
        await this.commit()
        return { ok: true }
      }
      case 'detect_group_proposals': {
        const keys = this.confirmations.detectGroupProposals()
        await this.commit()
        return { keys }
      }
      case 'accept_group_proposal': {
        const group = this.confirmations.acceptGroupProposal(command.key)
        await this.commit()
        return { id: group.id }
      }
      case 'reject_group_proposal': {
        this.confirmations.rejectGroupProposal(command.key)
        await this.commit()
        return { ok: true }
      }
      case 'create_plan': {
        const plan = instantiateScenario(this.driver, command.templateId, command.inputs)
        this.analyze(plan.id)
        await this.commit()
        return { id: plan.id }
      }
      case 'analyze_plan': {
        const result = this.analyze(command.planId)
        await this.commit()
        return {
          id: command.planId,
          revisionChanged: result.revisionChanged,
          addedImpacts: result.diff.addedImpacts.length,
        }
      }
      case 'transition_plan': {
        const plan = this.changePlans.transition(command.planId, command.state)
        await this.commit()
        return { id: plan.id, state: plan.workflowState }
      }
      case 'complete_action': {
        this.assertPlanActionsAllowed(command.planId)
        const plan = this.changePlans.completeAction(command.planId, command.actionId)
        await this.commit()
        return { id: plan.id }
      }
      case 'verify_action': {
        this.assertPlanActionsAllowed(command.planId)
        const plan = this.changePlans.verifyActionManually(command.planId, command.actionId)
        await this.commit()
        return { id: plan.id }
      }
      case 'import_file': {
        const result = await this.importFile(command)
        await this.commit()
        return result
      }
      case 'resolve_merchant': {
        if (!this.importPending) throw new AuthorityError('CONFLICT', '没有正在进行的导入')
        this.importPending.coordinator.resolveMerchant(command.merchantRaw, command.nodeId)
        return { ok: true }
      }
      case 'finalize_import': {
        if (!this.importPending) throw new AuthorityError('CONFLICT', '没有正在进行的导入')
        const pending = this.importPending
        this.importPending = null
        const outcome = await pending.coordinator.finalize()
        this.lastImportSummary = {
          id: outcome.session.id,
          adapterId: outcome.session.adapterId,
          startedAt: outcome.session.startedAt,
          completedAt: outcome.session.completedAt,
          rawCount: outcome.session.rawCount,
          newUniqueCount: outcome.newUniqueCount,
          duplicateCount: outcome.duplicateCount,
          proposalCount: outcome.proposalKeys.length,
          errorCount: outcome.errorCount,
        }
        await this.commit()
        return {
          sessionId: outcome.session.id,
          newUniqueCount: outcome.newUniqueCount,
          duplicateCount: outcome.duplicateCount,
          proposalKeys: outcome.proposalKeys,
          unresolvedMerchants: outcome.unresolvedMerchants,
        }
      }
      case 'accept_candidate': {
        const result = this.discovery.accept(command.id)
        await this.commit()
        return { nodeId: result.nodeId }
      }
      case 'dismiss_candidate': {
        this.discovery.dismiss(command.id)
        await this.commit()
        return { ok: true }
      }
      case 'resolve_drift': {
        if (command.resolution === 'dismiss') {
          this.driftService.dismiss(command.driftId)
        } else if (command.resolution === 'replacement') {
          this.driftService.resolveAsReplacement(command.driftId)
        } else {
          this.driftService.resolveAsAdditionalPath(command.driftId)
        }
        await this.commit()
        return { ok: true }
      }
      default: {
        const unknown = command as { type: string }
        throw new AuthorityError('VALIDATION', `unsupported command: ${unknown.type}`)
      }
    }
  }

  private async commit(): Promise<void> {
    await this.persistNow()
  }

  /**
   * 分析 / 重新分析：
   * - 从未分析过（snapshot=null 且 revision 未变）→ Core `analyzePlanImpact` + `claimUnassignedImpacts`
   * - 图已变化 → Core `rebasePlan`（真实 diff）
   * - 都无变化 → no-op（不伪造 diff）
   */
  private analyze(planId: string): { revisionChanged: boolean; diff: PlanRebaseDiff } {
    const plan = this.plans.getExisting(planId)
    const currentRevision = getGraphRevision(this.driver)
    if (isPlanStale(plan, currentRevision)) {
      const result = rebasePlan(this.driver, this.impactRepos(), planId)
      this.lastRebase.set(planId, {
        at: this.now(),
        fromRevision: plan.lastAnalyzedGraphRevision,
        toRevision: currentRevision,
        diff: result.diff,
      })
      return { revisionChanged: result.revisionChanged, diff: result.diff }
    }
    if (plan.impactSnapshot === null) {
      const snapshot = analyzePlanImpact(this.driver, this.impactRepos(), plan)
      const actions = claimUnassignedImpacts(plan.actions, snapshot, plan.scenario)
      this.plans.updateAnalysis(plan.id, snapshot, currentRevision, actions)
      const diff: PlanRebaseDiff = {
        addedImpacts: snapshot.targets.map((target) => ({
          nodeId: target.nodeId,
          capability: target.capability,
          status: target.status,
          reasonCode: target.reasonCode,
        })),
        removedImpacts: [],
        changedImpacts: [],
        addedActions: [],
        removedActions: [],
        changedActions: [],
      }
      this.lastRebase.set(planId, {
        at: this.now(),
        fromRevision: plan.lastAnalyzedGraphRevision,
        toRevision: currentRevision,
        diff,
      })
      return { revisionChanged: false, diff }
    }
    return {
      revisionChanged: false,
      diff: {
        addedImpacts: [],
        removedImpacts: [],
        changedImpacts: [],
        addedActions: [],
        removedActions: [],
        changedActions: [],
      },
    }
  }

  private impactRepos(): {
    deps: DependencyRepository
    groups: DependencyGroupRepository
    proposals: DependencyProposalRepository
    plans: ChangePlanRepository
  } {
    return { deps: this.deps, groups: this.groups, proposals: this.proposals, plans: this.plans }
  }

  /** stale 计划 fail closed：禁止完成/验证动作。 */
  private assertPlanActionsAllowed(planId: string): void {
    const plan = this.plans.getExisting(planId)
    if (isPlanStale(plan, getGraphRevision(this.driver))) {
      throw new AuthorityError(
        'CONFLICT',
        '基础设施已发生变化，必须先重新分析计划，才能继续记录或验证动作',
      )
    }
  }

  /**
   * Make-Before-Break gate（§49）：当存在针对该对象的未结变更计划时，
   * recovery/access/authentication/communication 旧路径的 retire 必须等新路径验证通过。
   */
  private assertMakeBeforeBreak(dependencyId: string): void {
    const edge = this.deps.getById(dependencyId)
    if (!edge || edge.state !== 'active') return
    if (!MBB_CAPABILITIES.includes(edge.capability)) return
    const related = this.plans
      .listAll()
      .filter(
        (plan) =>
          plan.workflowState !== 'completed' &&
          plan.workflowState !== 'cancelled' &&
          (plan.targetNodeId === edge.to || plan.targetNodeId === edge.from),
      )
    if (related.length === 0) return
    const actions = related.flatMap((plan) => plan.actions)
    const changeActions = actions.filter((action) => action.phase === 'change')
    const verifyActionIds = new Set(
      actions.filter((action) => action.phase === 'verify').map((action) => action.id),
    )
    // 「停用旧路径」类动作以「验证动作」为前置（BREAK 必须发生在 VERIFY 之后），
    // 它们不是“建立新路径”的动作，不能参与 make-before-break 的 make 判定。
    const newPathActions = changeActions.filter(
      (action) => !(action.prerequisiteActionIds ?? []).some((id) => verifyActionIds.has(id)),
    )
    const result = evaluateMakeBeforeBreak({
      newPathActions,
      verificationActions: actions.filter((action) => action.phase === 'verify'),
      retireActionId: dependencyId,
      retireAlreadyDone: false,
    })
    if (result.status === 'blocked') {
      throw new AuthorityError(
        'CONFLICT',
        `新路径尚未完成真实验证，旧路径仍受保护（${result.reason}）`,
      )
    }
  }

  // -------------------------------------------------------------------------
  // Import
  // -------------------------------------------------------------------------

  private adapterFor(adapterId: string): EvidenceSourceAdapter {
    if (adapterId === 'wechat_statement') return new WeChatStatementAdapter(this.driver)
    if (adapterId === 'generic_csv') return new GenericCsvAdapter()
    if (adapterId === 'ofx_qfx') return new OfxQfxAdapter()
    throw new AuthorityError('VALIDATION', `不支持的数据来源: ${adapterId}`)
  }

  private ensureSourceInstance(adapter: EvidenceSourceAdapter): string {
    const existing = this.sources
      .listByAdapter(adapter.id)
      .filter((source) => source.state === 'active')
    const found = existing[0]
    if (found) return found.id
    const created = this.sources.create({
      adapterId: adapter.id,
      adapterVersion: adapter.version,
      sourceKind: adapter.sourceKind,
      label: adapter.id,
    })
    return created.id
  }

  private async importFile(command: Extract<WebCommand, { type: 'import_file' }>): Promise<{
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
  }> {
    const adapter = this.adapterFor(command.adapterId)
    const data = base64ToBytes(command.dataBase64)
    if (data.length > 32 * 1024 * 1024) {
      throw new AuthorityError('FILE_TOO_LARGE', '导入文件超过 32 MiB 上限')
    }
    const sourceInstanceId = this.ensureSourceInstance(adapter)
    const coordinator = new ImportCoordinator(this.driver)
    const input =
      command.mapping === undefined
        ? { data, fileName: command.fileName }
        : { data, fileName: command.fileName, mapping: command.mapping }
    const begin = await coordinator.begin(sourceInstanceId, adapter, input)
    this.importPending = { coordinator, sessionId: begin.session.id, adapterId: adapter.id }
    this.lastImportSummary = {
      id: begin.session.id,
      adapterId: adapter.id,
      startedAt: begin.session.startedAt,
      completedAt: null,
      rawCount: begin.rawCount,
      newUniqueCount: 0,
      duplicateCount: 0,
      proposalCount: 0,
      errorCount: begin.errors.length,
    }
    const unresolved = begin.candidates.filter((candidate) => candidate.resolvedNodeId === null)
    return {
      sessionId: begin.session.id,
      rawCount: begin.rawCount,
      candidateCount: begin.candidates.length,
      unresolvedCount: unresolved.length,
      errorCount: begin.errors.length,
      merchants: begin.candidates.map((candidate) => ({
        merchantRaw: candidate.merchantRaw,
        resolvedNodeId: candidate.resolvedNodeId,
        observationCount: candidate.observationCount,
        resolution: candidate.resolution,
      })),
    }
  }

  // -------------------------------------------------------------------------
  // Queries
  // -------------------------------------------------------------------------

  query(query: WebQuery): unknown {
    switch (query.type) {
      case 'get_status':
        return {
          graphRevision: this.graphRevision(),
          schemaVersion: this.schemaVersion(),
          counts: exportGraph(this.driver).counts,
        }
      case 'get_now':
        return this.nowProjection()
      case 'get_infrastructure':
        return this.infrastructureProjection()
      case 'get_node_detail':
        return this.nodeDetail(query.id)
      case 'get_findings':
        return this.findings()
      case 'get_plans':
        return this.plansProjection()
      case 'get_plan_detail':
        return this.planDetail(query.id)
      case 'get_records':
        return this.records()
      case 'get_import_state':
        return this.importState()
      case 'search':
        return this.search(query.query)
      case 'get_diagnostics':
        return this.diagnostics()
      default: {
        const unknown = query as { type: string }
        throw new AuthorityError('VALIDATION', `unsupported query: ${unknown.type}`)
      }
    }
  }

  // -------------------------------------------------------------------------
  // Projections
  // -------------------------------------------------------------------------

  private allNodes(includeArchived = true): DepNode[] {
    return this.nodes.list(includeArchived ? {} : { archived: false })
  }

  private nodeName(id: string): string {
    const node = this.nodes.getById(id)
    return node ? node.name : id
  }

  private dependencyView(edge: Dependency): DependencyView {
    return {
      id: edge.id,
      from: edge.from,
      fromName: this.nodeName(edge.from),
      relation: edge.relation,
      to: edge.to,
      toName: this.nodeName(edge.to),
      capability: edge.capability,
      criticality: edge.criticality,
      groupId: edge.groupId,
      state: edge.state,
      origin: edge.origin,
      confirmedAt: edge.confirmedAt,
      lastVerifiedAt: edge.lastVerifiedAt,
      retiredAt: edge.retiredAt,
      verificationBasisType: edge.verificationBasis?.type ?? null,
    }
  }

  private nodeSummary(node: DepNode): NodeSummary {
    const edges = this.deps
      .listAll()
      .filter((edge) => edge.state === 'active' && (edge.from === node.id || edge.to === node.id))
    return {
      id: node.id,
      kind: node.kind,
      name: node.name,
      templateId: node.templateId,
      issuer: node.issuer,
      last4: node.last4,
      archived: node.archived,
      dependencyCount: edges.length,
      unknownCriticalityCount: edges.filter((edge) => edge.criticality === 'unknown').length,
    }
  }

  private groupView(group: DependencyGroup): GroupView {
    const edges = group.memberEdgeIds
      .map((id) => this.deps.getById(id))
      .filter((edge): edge is Dependency => edge !== null)
      .map((edge) => this.dependencyView(edge))
    return { ...group, memberEdges: edges }
  }

  private findings(): FindingsProjection {
    const nowIso = this.now()
    const activeDeps = this.deps.listActive()
    const activeGroups = this.groups.listAllActive()
    const coveredKeys = new Set(
      activeGroups.map((group) => `${group.targetNodeId}|${group.capability}|${group.mode}`),
    )
    const activePlans = this.plans
      .listAll()
      .filter((plan) => plan.workflowState !== 'completed' && plan.workflowState !== 'cancelled')
    const planTargets = new Set(activePlans.map((plan) => plan.targetNodeId ?? ''))

    const singleSourceKeys: Array<{
      nodeId: string
      capability: Capability
      sourceEdgeId: string
    }> = []
    for (const edge of activeDeps) {
      if (edge.criticality !== 'required') continue
      const incoming = this.deps.listActiveIncomingTo(edge.to, edge.capability)
      if (incoming.length !== 1) continue
      if (coveredKeys.has(`${edge.to}|${edge.capability}|ANY`)) continue
      singleSourceKeys.push({ nodeId: edge.to, capability: edge.capability, sourceEdgeId: edge.id })
    }

    const unknownCriticalPaths = activeDeps
      .filter((edge) => edge.criticality === 'unknown' && planTargets.has(edge.to))
      .map((edge) => ({
        what: `「${this.nodeName(edge.to)}」的 ${edge.capability} 路径关键性尚未确认`,
        evidenceRefs: [edge.id],
        capability: edge.capability,
      }))

    const staleThreshold = Date.parse(nowIso) - STALE_DEPENDENCY_DAYS * 86_400_000
    const staleRecoveryItems = activeDeps
      .filter(
        (edge) =>
          (edge.capability === 'recovery' || edge.capability === 'authentication') &&
          Date.parse(edge.lastVerifiedAt) < staleThreshold,
      )
      .map((edge) => ({
        what: `「${this.nodeName(edge.to)}」的 ${edge.capability} 关系超过 ${STALE_DEPENDENCY_DAYS} 天未重新确认`,
        evidenceRefs: [edge.id],
        capability: edge.capability,
      }))

    const pendingVerifications: Array<{
      what: string
      evidenceRefs: string[]
      capability: Capability
    }> = []
    for (const plan of activePlans) {
      for (const action of plan.actions) {
        if (!action.done) continue
        const status = action.verification?.status
        if (status !== 'pending' && status !== 'evidence_suggested') continue
        pendingVerifications.push({
          what: `「${plan.title}」中的动作「${action.title}」仍未验证`,
          evidenceRefs: [plan.id, action.id],
          capability: 'payment',
        })
      }
    }

    const recoveryDeps = activeDeps.filter((edge) => edge.capability === 'recovery')
    const pendingProposals = this.proposals
      .listByDecision('pending')
      .filter((proposal) => proposal.capability === 'recovery')
    const cycles = detectRecoveryCycles({
      capability: 'recovery',
      dependencies: recoveryDeps,
      hintedEdges: pendingProposals.map((proposal) => ({ from: proposal.from, to: proposal.to })),
    })
    const recoveryCycles =
      cycles.status === 'confirmed_cycle'
        ? cycles.confirmedCycles.map((cycle) => ({
            what: `恢复关系形成环：${cycle.map((id) => this.nodeName(id)).join(' → ')}`,
            evidenceRefs: [...cycle],
            capability: 'recovery' as Capability,
          }))
        : []
    const unconfirmedFallbacks = cycles.potentialCycles.map((cycle) => ({
      what: `可能存在恢复环（尚未确认）：${cycle.map((id) => this.nodeName(id)).join(' → ')}`,
      evidenceRefs: [...cycle],
      capability: 'recovery' as Capability,
    }))

    const raw = generateInfrastructureFindings({
      now: nowIso,
      criticalPaths: singleSourceKeys.map((key) => ({
        nodeId: key.nodeId,
        capability: key.capability,
      })),
      singleSourceKeys,
      sharedFailureDomains: [],
      recoveryCycles,
      unconfirmedFallbacks,
      staleRecoveryItems,
      unknownCriticalPaths,
      pendingVerifications,
    })

    const views: FindingView[] = raw.map((finding) => ({
      ...finding,
      relatedNodeIds: this.relatedNodesForFinding(finding.evidenceRefs),
    }))
    const counts: Record<string, number> = {}
    for (const finding of views) counts[finding.type] = (counts[finding.type] ?? 0) + 1
    return { findings: views, counts }
  }

  private relatedNodesForFinding(evidenceRefs: string[]): string[] {
    const ids = new Set<string>()
    for (const ref of evidenceRefs) {
      const edge = this.deps.getById(ref)
      if (edge) {
        ids.add(edge.from)
        ids.add(edge.to)
        continue
      }
      const node = this.nodes.getById(ref)
      if (node) ids.add(node.id)
    }
    return [...ids].sort()
  }

  private planSummary(plan: ChangePlan): PlanSummary {
    const currentRevision = this.graphRevision()
    const doneCount = plan.actions.filter((action) => action.done).length
    const verifiedCount = plan.actions.filter(
      (action) => action.verification?.status === 'verified',
    ).length
    const pendingVerificationCount = plan.actions.filter(
      (action) =>
        action.done &&
        (action.verification?.status === 'pending' ||
          action.verification?.status === 'evidence_suggested'),
    ).length
    return {
      id: plan.id,
      title: plan.title,
      scenario: plan.scenario,
      templateId: plan.templateId,
      workflowState: plan.workflowState,
      effectiveStatus: effectiveStatus(plan, currentRevision),
      readiness: this.changePlans.getReadiness(plan.id),
      targetNodeId: plan.targetNodeId,
      targetName: plan.targetNodeId ? this.nodeName(plan.targetNodeId) : null,
      lastAnalyzedGraphRevision: plan.lastAnalyzedGraphRevision,
      currentGraphRevision: currentRevision,
      stale: isPlanStale(plan, currentRevision),
      actionCount: plan.actions.length,
      doneCount,
      verifiedCount,
      pendingVerificationCount,
      effectiveDate: plan.effectiveDate,
      updatedAt: plan.updatedAt,
    }
  }

  private actionViews(plan: ChangePlan): ActionView[] {
    const dag = validateActionDag({ actions: plan.actions })
    const order = new Map<string, number>()
    dag.topologicalOrder.forEach((id, index) => order.set(id, index + 1))
    return plan.actions.map((action) => {
      const blockedByPrerequisites = (action.prerequisiteActionIds ?? []).filter((id) => {
        const prerequisite = plan.actions.find((candidate) => candidate.id === id)
        return !prerequisite || !prerequisite.done
      })
      return { action, blockedByPrerequisites, order: order.get(action.id) ?? 0 }
    })
  }

  private nowProjection(): NowProjection {
    const nodes = this.allNodes(false)
    const summaries = nodes.map((node) => this.nodeSummary(node))
    const activeDeps = this.deps.listActive()
    const plans = this.plans.listAll().map((plan) => this.planSummary(plan))
    const openPlans = plans.filter(
      (plan) => plan.workflowState !== 'completed' && plan.workflowState !== 'cancelled',
    )
    const findings = this.findings().findings
    const pendingVerification = this.plans
      .listAll()
      .filter((plan) => plan.workflowState !== 'completed' && plan.workflowState !== 'cancelled')
      .flatMap((plan) =>
        plan.actions
          .filter(
            (action) =>
              action.done &&
              (action.verification?.status === 'pending' ||
                action.verification?.status === 'evidence_suggested'),
          )
          .map((action) => ({
            planId: plan.id,
            planTitle: plan.title,
            actionId: action.id,
            actionTitle: action.title,
            doneAt: action.doneAt,
          })),
      )

    const byKind = NODE_KIND_LABELS.map((entry) => ({
      kind: entry.kind,
      count: summaries.filter((summary) => summary.kind === entry.kind).length,
    })).filter((entry) => entry.count > 0)

    const nextSteps: NowProjection['nextSteps'] = []
    const stalePlan = openPlans.find((plan) => plan.stale)
    if (stalePlan) {
      nextSteps.push({
        id: `reanalyze-${stalePlan.id}`,
        label: `重新分析「${stalePlan.title}」`,
        detail: '基础设施已发生变化，旧影响快照不能继续作为当前依据。',
        route: `/change/${stalePlan.id}`,
      })
    }
    const pending = pendingVerification[0]
    if (pending) {
      nextSteps.push({
        id: `verify-${pending.actionId}`,
        label: `验证「${pending.actionTitle}」`,
        detail: '动作已记录完成，但尚未通过独立验证（done ≠ verified）。',
        route: `/change/${pending.planId}`,
      })
    }
    const attention = findings[0]
    if (attention && nextSteps.length < 3) {
      nextSteps.push({
        id: `finding-${attention.id}`,
        label: attention.recommendedNextAction,
        detail: attention.what,
        route: '/findings',
      })
    }
    if (nextSteps.length === 0 && nodes.length === 0) {
      nextSteps.push({
        id: 'first-node',
        label: '添加第一个基础设施对象',
        detail: '从手机号、邮箱、银行卡或账户开始。',
        route: '/infrastructure',
      })
    }

    return {
      counts: {
        nodes: nodes.length,
        dependencies: activeDeps.length,
        archivedNodes: this.allNodes(true).filter((node) => node.archived).length,
        unknownCriticality: activeDeps.filter((edge) => edge.criticality === 'unknown').length,
        unknownBoundaries: new Set(
          activeDeps.filter((edge) => edge.criticality === 'unknown').map((edge) => edge.to),
        ).size,
        pendingVerifications: pendingVerification.length,
        stalePlans: openPlans.filter((plan) => plan.stale).length,
        activePlans: openPlans.length,
      },
      byKind,
      keyInfrastructure: [...summaries]
        .sort((a, b) => b.dependencyCount - a.dependencyCount || (a.name < b.name ? -1 : 1))
        .slice(0, 6),
      changes: openPlans,
      attention: findings.slice(0, 4),
      pendingVerification,
      nextSteps: nextSteps.slice(0, 3),
    }
  }

  private infrastructureProjection(): InfrastructureProjection {
    const nodes = this.allNodes(true)
    return {
      counts: NODE_KIND_LABELS.map((entry) => ({
        kind: entry.kind,
        count: nodes.filter((node) => node.kind === entry.kind && !node.archived).length,
      })),
      archivedCount: nodes.filter((node) => node.archived).length,
      nodes: nodes.map((node) => this.nodeSummary(node)),
      templates: NODE_KIND_LABELS,
    }
  }

  private changeTemplatesFor(
    node: DepNode,
  ): Array<{ id: string; title: string; requiredInputs: string[] }> {
    return listActiveTemplates()
      .filter((template) => {
        if (template.id === 'replace_phone_number') {
          return node.kind === 'identity_anchor' && node.fields['subtype'] === 'phone'
        }
        return node.kind === 'payment_instrument'
      })
      .map((template) => ({
        id: template.id,
        title: template.title,
        requiredInputs: template.requiredInputs.map((input) => input.key),
      }))
  }

  private nodeDetail(id: string): NodeDetailProjection {
    const node = this.nodes.getById(id)
    if (!node) throw new AuthorityError('NOT_FOUND', `对象不存在: ${id}`)
    const allDeps = this.deps.listAll()
    const outgoing = allDeps
      .filter((edge) => edge.from === id)
      .map((edge) => this.dependencyView(edge))
    const incoming = allDeps
      .filter((edge) => edge.to === id)
      .map((edge) => this.dependencyView(edge))
    const groups = this.groups
      .listAllActive()
      .filter((group) => group.targetNodeId === id)
      .map((group) => this.groupView(group))
    const findings = this.findings().findings.filter((finding) =>
      finding.relatedNodeIds.includes(id),
    )
    const plans = this.plans
      .listAll()
      .filter((plan) => plan.targetNodeId === id)
      .map((plan) => this.planSummary(plan))
    const relatedRecords = this.records().records.filter(
      (record) =>
        record.sourceId === id ||
        (record.planId !== null && plans.some((p) => p.id === record.planId)),
    )
    const templates = this.changeTemplatesFor(node)
    const boundary =
      templates.length === 0
        ? node.kind === 'identity_anchor'
          ? '邮箱/其他身份锚点变更尚未开放正式可执行模板；恢复与认证影响不会由界面自行猜测。'
          : (CHANGE_BOUNDARY_BY_KIND[node.kind] ?? '此对象暂不能直接创建变更计划。')
        : null
    return {
      node,
      outgoing,
      incoming,
      groups,
      findings,
      plans,
      records: relatedRecords.slice(0, 20),
      changeTemplates: templates,
      changeBoundary: boundary,
    }
  }

  private plansProjection(): PlansProjection {
    const templates = listActiveTemplates().map((template) => ({
      id: template.id,
      title: template.title,
      description: template.description,
      requiredInputs: template.requiredInputs.map((input) => ({
        key: input.key,
        description: input.description,
      })),
      supportedCapabilities: [...template.supportedCapabilities],
    }))
    return {
      plans: this.plans.listAll().map((plan) => this.planSummary(plan)),
      templates,
      graphRevision: this.graphRevision(),
    }
  }

  private planDetail(id: string): PlanDetailView {
    const plan = this.plans.getExisting(id)
    const current = plan.targetNodeId
      ? analyzePlanImpact(this.driver, this.impactRepos(), plan)
      : null
    const dag = validateActionDag({ actions: plan.actions })
    const readinessInput = this.changePlans.assembleReadinessInput(plan)
    return {
      plan,
      summary: this.planSummary(plan),
      actions: this.actionViews(plan),
      current,
      projection: plan.impactSnapshot,
      lastRebase: this.lastRebase.get(id) ?? null,
      dagViolations: dag.ok ? [] : [dag.violation],
      readiness: computePlanReadiness(readinessInput),
    }
  }

  private records(): RecordsProjection {
    const records: RecordView[] = []
    for (const plan of this.plans.listAll()) {
      records.push({
        id: `plan-${plan.id}`,
        kind: 'change_plan',
        title: plan.title,
        subtitle: `变更计划（${plan.scenario}）已记录`,
        occurredAt: null,
        recordedAt: plan.createdAt,
        verifiedAt: null,
        status: plan.workflowState,
        sourceType: 'change_plan',
        sourceId: plan.id,
        planId: plan.id,
        actionId: null,
        evidenceRefs: [],
      })
      for (const action of plan.actions) {
        if (action.done) {
          records.push({
            id: `done-${plan.id}-${action.id}`,
            kind: 'action_completion',
            title: action.title,
            subtitle: `「${plan.title}」中的动作已记录完成（未验证不等于已验证）`,
            occurredAt: null,
            recordedAt: action.doneAt,
            verifiedAt: action.verification?.verifiedAt ?? null,
            status: 'done',
            sourceType: 'change_plan',
            sourceId: plan.id,
            planId: plan.id,
            actionId: action.id,
            evidenceRefs: action.verification?.evidenceRefs ?? [],
          })
        }
        if (action.verification?.status === 'verified') {
          records.push({
            id: `verified-${plan.id}-${action.id}`,
            kind: 'action_verification',
            title: action.title,
            subtitle: `独立验证通过（方法：${action.verification.method}）`,
            occurredAt: null,
            recordedAt: action.doneAt,
            verifiedAt: action.verification.verifiedAt,
            status: 'verified',
            sourceType: 'change_plan',
            sourceId: plan.id,
            planId: plan.id,
            actionId: action.id,
            evidenceRefs: action.verification.evidenceRefs,
          })
        }
      }
    }

    for (const drift of this.drifts.listAll()) {
      records.push({
        id: `drift-${drift.id}`,
        kind: 'reality_drift',
        title: `可能发生了变化：${this.nodeName(drift.targetNodeId)}`,
        subtitle: '证据显示可能出现新路径，Reality 尚未改变',
        occurredAt: null,
        recordedAt: drift.updatedAt,
        verifiedAt: null,
        status: drift.status,
        sourceType: 'reality_drift',
        sourceId: drift.id,
        planId: null,
        actionId: null,
        evidenceRefs: drift.evidenceRefs ?? [],
      })
    }

    for (const item of buildTimeline(this.driver, this.now())) {
      if (item.sourceType === 'change_plan' || item.sourceType === 'action_verification') continue
      records.push({
        id: `timeline-${item.id}`,
        kind: item.sourceType === 'node_expiry' ? 'node_expiry' : 'source_freshness',
        title: item.title,
        subtitle: item.subtitle,
        occurredAt: item.scheduledAt,
        recordedAt: null,
        verifiedAt: null,
        status: item.status,
        sourceType: item.sourceType,
        sourceId: item.sourceId,
        planId: null,
        actionId: null,
        evidenceRefs: [],
      })
    }

    records.sort((a, b) => {
      const aTime = a.verifiedAt ?? a.recordedAt ?? a.occurredAt ?? ''
      const bTime = b.verifiedAt ?? b.recordedAt ?? b.occurredAt ?? ''
      return aTime < bTime ? 1 : aTime > bTime ? -1 : 0
    })
    return { records }
  }

  private importState(): ImportStateProjection {
    const sources = this.sources.listAll()
    const candidates = this.candidates.listAll()
    const proposals = this.proposals.listAll().map((proposal) => ({
      ...proposal,
      fromName: this.nodeName(proposal.from),
      toName: this.nodeName(proposal.to),
    }))
    const groupProposals = this.groupProposals.listAll()
    const drifts = this.drifts.listAll().map((drift) => ({
      ...drift,
      targetName: this.nodeName(drift.targetNodeId),
      fromName: drift.candidateFrom === null ? '（未知来源）' : this.nodeName(drift.candidateFrom),
    }))
    const last = this.lastImportSummary
    return {
      sources,
      candidates,
      proposals,
      groupProposals,
      drifts,
      adapters: [
        { id: 'wechat_statement', label: '微信账单（CSV）', requiresMapping: false },
        { id: 'generic_csv', label: '通用 CSV（需显式字段映射）', requiresMapping: true },
        { id: 'ofx_qfx', label: 'OFX / QFX', requiresMapping: false },
      ],
      lastSession: last
        ? {
            id: last.id,
            adapterId: last.adapterId,
            startedAt: last.startedAt,
            completedAt: last.completedAt,
            rawCount: last.rawCount,
            newUniqueCount: last.newUniqueCount,
            duplicateCount: last.duplicateCount,
            proposalCount: last.proposalCount,
            errorCount: last.errorCount,
          }
        : null,
    }
  }

  private search(query: string): SearchProjection {
    const normalized = query.trim().toLowerCase()
    if (normalized === '') return { query, results: [] }
    const results: SearchProjection['results'] = []
    for (const node of this.allNodes(true)) {
      const haystack = `${node.name} ${node.issuer ?? ''} ${node.last4 ?? ''}`.toLowerCase()
      if (!haystack.includes(normalized)) continue
      results.push({
        type: 'node',
        id: node.id,
        title: node.name,
        detail: `对象 · ${NODE_KIND_LABELS.find((entry) => entry.kind === node.kind)?.label ?? node.kind}`,
        route: `/infrastructure/${node.id}`,
      })
    }
    for (const plan of this.plans.listAll()) {
      if (!plan.title.toLowerCase().includes(normalized)) continue
      results.push({
        type: 'plan',
        id: plan.id,
        title: plan.title,
        detail: `变更 · ${plan.workflowState}`,
        route: `/change/${plan.id}`,
      })
    }
    for (const finding of this.findings().findings) {
      if (!finding.what.toLowerCase().includes(normalized)) continue
      results.push({
        type: 'finding',
        id: finding.id,
        title: finding.what,
        detail: `发现 · ${finding.type}`,
        route: '/findings',
      })
    }
    for (const record of this.records().records) {
      if (!record.title.toLowerCase().includes(normalized)) continue
      results.push({
        type: 'record',
        id: record.id,
        title: record.title,
        detail: `记录 · ${record.status}`,
        route: '/records',
      })
    }
    for (const source of this.sources.listAll()) {
      if (!source.label.toLowerCase().includes(normalized)) continue
      results.push({
        type: 'source',
        id: source.id,
        title: source.label,
        detail: `来源 · ${source.adapterId}`,
        route: '/import',
      })
    }
    return { query, results: results.slice(0, 40) }
  }

  private diagnostics(): DiagnosticsProjection {
    const orphans = checkGraphIntegrity(this.driver)
    const counts: Record<string, number> = {}
    for (const [key, value] of Object.entries(orphans)) {
      counts[key] = typeof value === 'number' ? value : Array.isArray(value) ? value.length : 0
    }
    return {
      graphRevision: this.graphRevision(),
      schemaVersion: this.schemaVersion(),
      counts: exportGraph(this.driver).counts,
      integrityOrphans: counts,
      templates: listActiveTemplates().map((template) => template.id),
      lastSavedAt: null,
      storage: { persisted: false, persistRequested: false, usageBytes: null, quotaBytes: null },
    }
  }
}

function base64ToBytes(value: string): Uint8Array {
  const binary = atob(value)
  const bytes = new Uint8Array(binary.length)
  for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i)
  return bytes
}
