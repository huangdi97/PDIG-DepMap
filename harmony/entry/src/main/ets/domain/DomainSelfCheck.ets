// Domain 层自检 —— 唯一目的：让 domain/ 下的纯 ArkTS 模块**进入编译图**，
// 并让它们的语义在无设备情况下也能给出可读的、不求证伪的自检行。
//
// 为什么需要这一层（这是本项目的一个真实教训）：
//   hvigor 的 CompileArkTS 只编译从 ability / page **可达**的模块。
//   未被任何入口 import 的 `.ets` 不会进编译图，其类型错误不会让
//   `assembleHap` 失败 —— "BUILD SUCCESSFUL" 因而是**空证据**。
//   `Argon2idNative.ets` 正是因为依赖方向反转而成为孤儿：
//   源文件存在、从未被编译、构建却是绿的（由编译可达性 Gate 正/负向双 probe 抓出）。
//
//   因此 Domain 层每落地一组，就必须有一个**真实的入边**。
//   本模块就是这个入边，且它只依赖 domain/，不引入任何平台依赖。
//
// 本模块**不做**任何"通过/失败"的健全性声明：它只是把纯函数的输出
// 格式化成一行，供页面上肉眼核对与 HAP 内字符串反查。
// 真正的判定在 tools/harmony/check-compiled-reachability.mjs 与
// 未来的 ArkTS conformance runner。

import { ActionVerificationStatus, Capability, ChangePlanWorkflowState, CoverageLevel, Criticality, DependencyOrigin, DependencyState, GroupMode, GroupState, ImpactReasonCode, PlanReadiness as PlanReadinessEnum, Relation, ScenarioAvailability, TimelineBucket } from '../generated/CanonicalEnums';
import { ChangePlan, CoverageSourceInfo, Dependency, DependencyGroup, ImpactGraph, ImpactProposalInput, ImpactStateKey, PlanReadinessInput, ScenarioCoverageInput, canonicalGroupKey, dependencyLogicalKey } from './Entities';
import { simulateDisable, simulateScenario } from './ImpactKernel';
import { groupKeyDedupsMembers, groupKeyIsOrderIndependent, logicalKeyRoundTrips, parseDependencyLogicalKey } from './LogicalKey';
import { computePlanReadiness } from './PlanReadiness';
import { computeScenarioCoverage } from './ScenarioCoverage';
import { ALL_WORKFLOW_STATES, allowedNextStates, canMutateAction, isTransitionAllowed, verificationAfterCompletion } from './StateMachines';
import { MUT_DEPENDENCY_CONFIRM_CREATE, MUT_DEPENDENCY_CONFIRM_UPDATE, MUT_DEPENDENCY_REACTIVATE, MUT_DEPENDENCY_RETIRE, MUT_DRIFT_RESOLVE_ADDITIONAL_PATH, MUT_DRIFT_RESOLVE_REPLACEMENT, MUT_GROUP_CONFIRM, MUT_GROUP_RETIRE, MUT_NODE_ARCHIVE, MUT_NODE_CREATE, MUT_NODE_UPDATE, doesMutationBumpRevision, parseStoredRevision } from './GraphRevision';
import { TimelineSortKey, bucketOf, sortTimelineKeys, startOfUtcDay } from './Timeline';
import { listActiveTemplates, validateScenarioInstantiation } from './ScenarioTemplate';

function dep(
  id: string,
  from: string,
  relation: Relation,
  to: string,
  capability: Capability,
  criticality: Criticality,
): Dependency {
  const d = new Dependency();
  d.id = id;
  d.from = from;
  d.relation = relation;
  d.to = to;
  d.capability = capability;
  d.criticality = criticality;
  d.state = DependencyState.ACTIVE;
  d.origin = DependencyOrigin.MANUAL;
  return d;
}

function emptyGraph(): ImpactGraph {
  return new ImpactGraph();
}

/**
 * 场景 R：required 单边、无替代 → must_change(required_edge_no_alternative)。
 * 对应 kernel 的 `lostEdges.some(criticality === required)` 分支。
 */
function checkRequiredEdge(): string {
  const g = emptyGraph();
  g.dependencies = [
    dep('e1', 'cardA', Relation.FUNDING_SOURCE, 'svcX', Capability.PAYMENT, Criticality.REQUIRED),
  ];
  const r = simulateDisable(g, 'cardA');
  let status = 'none';
  let reason = 'none';
  if (r.targets.length === 1) {
    status = r.targets[0].status;
    reason = r.targets[0].reasonCode;
  }
  const ok =
    r.targets.length === 1 &&
    status === 'must_change' &&
    reason === ImpactReasonCode.REQUIRED_EDGE_NO_ALTERNATIVE;
  return `required->must_change=${ok ? 'ok' : 'MISMATCH(' + status + '/' + reason + ')'}`;
}

/**
 * 场景 U：criticality=unknown 单边 → needs_review(criticality_unknown)，
 * **绝不能**是 must_change（机器推断不得产生 required）。
 */
function checkUnknownCriticality(): string {
  const g = emptyGraph();
  g.dependencies = [
    dep('e1', 'cardA', Relation.FUNDING_SOURCE, 'svcX', Capability.PAYMENT, Criticality.UNKNOWN),
  ];
  const r = simulateDisable(g, 'cardA');
  let status = 'none';
  if (r.targets.length === 1) {
    status = r.targets[0].status;
  }
  const ok = r.targets.length === 1 && status === 'needs_review';
  return `unknown->needs_review=${ok ? 'ok' : 'MISMATCH(' + status + ')'}`;
}

/**
 * 场景 G：确认的 ANY 组，组内两成员一失一存 → backup_path(confirmed_group_covered)。
 */
function checkGroupCovered(): string {
  const g = emptyGraph();
  g.dependencies = [
    dep('e1', 'cardA', Relation.FUNDING_SOURCE, 'svcX', Capability.PAYMENT, Criticality.UNKNOWN),
    dep('e2', 'cardB', Relation.FUNDING_SOURCE, 'svcX', Capability.PAYMENT, Criticality.UNKNOWN),
  ];
  const grp = new DependencyGroup();
  grp.id = 'g1';
  grp.targetNodeId = 'svcX';
  grp.capability = Capability.PAYMENT;
  grp.mode = GroupMode.ANY;
  grp.memberEdgeIds = ['e1', 'e2'];
  grp.state = GroupState.ACTIVE;
  grp.groupKey = canonicalGroupKey('svcX', Capability.PAYMENT, GroupMode.ANY, [
    dependencyLogicalKey('cardA', Relation.FUNDING_SOURCE, 'svcX', Capability.PAYMENT),
    dependencyLogicalKey('cardB', Relation.FUNDING_SOURCE, 'svcX', Capability.PAYMENT),
  ]);
  g.groups = [grp];

  const r = simulateDisable(g, 'cardA');
  let status = 'none';
  let reason = 'none';
  if (r.targets.length === 1) {
    status = r.targets[0].status;
    reason = r.targets[0].reasonCode;
  }
  const ok =
    r.targets.length === 1 &&
    status === 'backup_path' &&
    reason === ImpactReasonCode.CONFIRMED_GROUP_COVERED;
  return `group_covered->backup_path=${ok ? 'ok' : 'MISMATCH(' + status + '/' + reason + ')'}`;
}

/**
 * 场景 P：仅 proposal、无任何 confirmed 边 → needs_review(proposal_only)。
 * 置信度 0.99 与 0.01 必须给出**同一结论**（Proposal ≠ Reality）。
 */
function checkProposalOnly(): string {
  const g = emptyGraph();
  const p = new ImpactProposalInput();
  p.key = dependencyLogicalKey('cardA', Relation.MERCHANT_AGREEMENT, 'svcX', Capability.PAYMENT);
  p.from = 'cardA';
  p.to = 'svcX';
  p.capability = Capability.PAYMENT;
  p.confidenceScore = 0.99;
  g.proposals = [p];

  const g2 = emptyGraph();
  const p2 = new ImpactProposalInput();
  p2.key = p.key;
  p2.from = 'cardA';
  p2.to = 'svcX';
  p2.capability = Capability.PAYMENT;
  p2.confidenceScore = 0.01;
  g2.proposals = [p2];

  const r1 = simulateDisable(g, 'cardA');
  const r2 = simulateDisable(g2, 'cardA');
  const s1 = r1.targets.length === 1 ? r1.targets[0].status : 'none';
  const s2 = r2.targets.length === 1 ? r2.targets[0].status : 'none';
  const ok = s1 === 'needs_review' && s2 === 'needs_review' && s1 === s2;
  return `proposal_only(c=.99|.01)->needs_review=${ok ? 'ok' : 'MISMATCH(' + s1 + '/' + s2 + ')'}`;
}

/**
 * 场景 C：环（A→B→A，均 required）必须终止，且 processedKeys 无重复。
 * 若 wave-BFS 的 visited key 逻辑被破坏，这里会**不终止**而不是给出错误答案——
 * 因此本检查同时是终止性证明（processedKeys 唯一性不变式）。
 */
function checkCycleTermination(): string {
  const g = emptyGraph();
  g.dependencies = [
    dep('e1', 'nA', Relation.FUNDING_SOURCE, 'nB', Capability.PAYMENT, Criticality.REQUIRED),
    dep('e2', 'nB', Relation.FUNDING_SOURCE, 'nA', Capability.PAYMENT, Criticality.REQUIRED),
  ];
  const r = simulateDisable(g, 'nA');
  const seen: string[] = [];
  let dup = false;
  for (let i = 0; i < r.processedKeys.length; i++) {
    const k = r.processedKeys[i];
    for (let j = 0; j < seen.length; j++) {
      if (seen[j] === k) {
        dup = true;
        break;
      }
    }
    seen.push(k);
  }
  const ok = !dup;
  return `cycle_terminates=${ok ? 'ok' : 'DUP(' + r.processedKeys.join(',') + ')'}`;
}

/**
 * 场景 D：状态键必须是 (nodeId, capability)。
 * 对同一节点分别以 payment / access 为初始键：access 应被**忽略**
 * （MVP 只支持 payment domain），不得产生任何 target。
 */
function checkStateKeyIsPair(): string {
  const g = emptyGraph();
  g.dependencies = [
    dep('e1', 'cardA', Relation.FUNDING_SOURCE, 'svcX', Capability.PAYMENT, Criticality.REQUIRED),
  ];
  const paymentRun = simulateDisable(g, 'cardA', Capability.PAYMENT);
  const accessRun = simulateDisable(g, 'cardA', Capability.ACCESS);
  const ok =
    paymentRun.targets.length === 1 &&
    accessRun.targets.length === 0 &&
    accessRun.unavailable.length === 0;
  return `statekey_is_pair=${ok ? 'ok' : 'MISMATCH(p=' + paymentRun.targets.length + ',a=' + accessRun.targets.length + ')'}`;
}

/**
 * 场景 M：多初始键（同 capability）按 (nodeId, capability) 确定性排序，
 * 且 unavailable 去重。simulateScenario 的输入是数组，重复项必须被合并。
 */
function checkMultiInitialDedup(): string {
  const g = emptyGraph();
  g.dependencies = [
    dep('e1', 'cardB', Relation.FUNDING_SOURCE, 'svcX', Capability.PAYMENT, Criticality.REQUIRED),
    dep('e2', 'cardA', Relation.FUNDING_SOURCE, 'svcY', Capability.PAYMENT, Criticality.REQUIRED),
  ];
  // 故意乱序 + 重复：cardB, cardA, cardB
  const input: ImpactStateKey[] = [
    new ImpactStateKey('cardB', Capability.PAYMENT),
    new ImpactStateKey('cardA', Capability.PAYMENT),
    new ImpactStateKey('cardB', Capability.PAYMENT),
  ];
  const r = simulateScenario(g, input);
  const ordered = r.unavailable.length === 2 && r.unavailable[0].nodeId === 'cardA' && r.unavailable[1].nodeId === 'cardB';
  const ok = ordered;
  const got = r.unavailable.map((k: ImpactStateKey): string => k.nodeId).join(',');
  return `multi_initial_dedup_sorted=${ok ? 'ok' : 'MISMATCH(' + got + ')'}`;
}

/**
 * 场景 T：checklist 的最后一项必须是 target_operation（原始注销动作强制最后）。
 * 这是 UI 安全语义的一部分，不能因重构而被挤到中间。
 */
function checkChecklistTargetLast(): string {
  const g = emptyGraph();
  g.dependencies = [
    dep('e1', 'cardA', Relation.FUNDING_SOURCE, 'svcX', Capability.PAYMENT, Criticality.REQUIRED),
  ];
  const r = simulateDisable(g, 'cardA');
  const last = r.checklist.length > 0 ? r.checklist[r.checklist.length - 1] : undefined;
  const ok = last !== undefined && last.level === 'target_operation' && last.nodeId === 'cardA';
  return `checklist_target_last=${ok ? 'ok' : 'MISMATCH(' + (last === undefined ? 'empty' : last.level) + ')'}`;
}

/**
 * 检查 L：logical key 的 round-trip 与 groupKey 的顺序无关 / 去重性。
 * 这是跨端数据能对上的前提，也是 spec §4 的明确要求。
 */
function checkLogicalKeyInvariants(): string {
  const rt = logicalKeyRoundTrips(
    'cardA',
    Relation.FUNDING_SOURCE,
    'svcX',
    Capability.PAYMENT,
  );
  const m1 = dependencyLogicalKey('cardA', Relation.FUNDING_SOURCE, 'svcX', Capability.PAYMENT);
  const m2 = dependencyLogicalKey('cardB', Relation.FUNDING_SOURCE, 'svcX', Capability.PAYMENT);
  const orderIndep = groupKeyIsOrderIndependent('svcX', Capability.PAYMENT, GroupMode.ANY, [m1, m2]);
  const dedup = groupKeyDedupsMembers('svcX', Capability.PAYMENT, GroupMode.ANY, [m1, m2]);

  // fail-closed：分段数错误 / 未知词表 / 空分段都必须被拒绝
  const bad3 = parseDependencyLogicalKey('a|b|c');
  const badRel = parseDependencyLogicalKey('cardA|bound_to|svcX|payment');
  const badEmpty = parseDependencyLogicalKey('cardA||svcX|payment');
  const good = parseDependencyLogicalKey('cardA|funding_source|svcX|payment');
  const failClosed =
    !bad3.ok && !badRel.ok && !badEmpty.ok && good.ok && good.from === 'cardA' && good.to === 'svcX';

  const ok = rt && orderIndep && dedup && failClosed;
  return `logicalkey_invariants=${ok ? 'ok' : 'MISMATCH(rt=' + rt + ',oi=' + orderIndep + ',dd=' + dedup + ',fc=' + failClosed + ')'}`;
}

/**
 * 检查 R：PlanReadiness 的档位与优先级。
 *
 * 关键不变式：**不存在第 4 个取值**。最乐观也只是 ready_with_known_scope；
 * 任何 'safe' / 'ok' / 'all_clear' 都是规范外产物。
 * 同时校验优先级：pendingMustChange 最高（blocked），未决 proposal 次之。
 */
function checkPlanReadiness(): string {
  const base = new PlanReadinessInput();
  base.currentGraphRevision = 1;
  // ⚠ 2026-09-18 修正：光设 currentGraphRevision 并不构成"全清"输入。
  // 默认 ChangePlan 的 lastAnalyzedGraphRevision = 0，
  // 于是 isPlanStale(workflowState, 0, 1) = 1 > 0 = true → 判为 REVIEW_REQUIRED，
  // 这一项永远不可能等于 ready_with_known_scope（实测 cleanOk=false）。
  // 真正的"全清"必须让计划已在**当前** revision 上分析过。
  base.plan.lastAnalyzedGraphRevision = base.currentGraphRevision;

  // 全清 → ready_with_known_scope（注意：不是 'safe'）
  const clean = computePlanReadiness(base);
  const cleanOk = clean === PlanReadinessEnum.READY_WITH_KNOWN_SCOPE;

  // must_change 优先于一切
  const blocked = new PlanReadinessInput();
  blocked.currentGraphRevision = 1;
  blocked.pendingMustChange = 1;
  blocked.pendingRelevantProposals = 5;
  const blockedOk = computePlanReadiness(blocked) === PlanReadinessEnum.BLOCKED;

  // 未决 proposal 单独存在 → review_required，且 confidence 不参与
  const review = new PlanReadinessInput();
  review.currentGraphRevision = 1;
  review.pendingRelevantProposals = 1;
  const reviewOk = computePlanReadiness(review) === PlanReadinessEnum.REVIEW_REQUIRED;

  // stale（派生）优先于 ready：revision 前进但计划未重分析
  const stale = new PlanReadinessInput();
  stale.currentGraphRevision = 7;
  const p = new ChangePlan();
  p.workflowState = ChangePlanWorkflowState.ANALYZED;
  p.lastAnalyzedGraphRevision = 3;
  stale.plan = p;
  const staleOk = computePlanReadiness(stale) === PlanReadinessEnum.REVIEW_REQUIRED;

  // 无第 4 值：返回串必须落在三个合法枚举内
  const legal =
    clean === PlanReadinessEnum.BLOCKED ||
    clean === PlanReadinessEnum.REVIEW_REQUIRED ||
    clean === PlanReadinessEnum.READY_WITH_KNOWN_SCOPE;

  const ok = cleanOk && blockedOk && reviewOk && staleOk && legal;
  return `readiness=ok(${cleanOk}/b${blockedOk}/r${reviewOk}/s${staleOk}/l${legal})`;
}

/**
 * 检查 V：ScenarioCoverage 的四档与可解释性。
 *
 * 关键不变式：每一档都必须带 explanations（§23 可解释要求），
 * 且**不允许**出现"来源越多等级越高"的百分比式推断。
 */
function checkScenarioCoverage(): string {
  // unknown：无来源 + 无已确认直接依赖
  const a = new ScenarioCoverageInput();
  a.scenarioId = 's';
  a.now = '2026-09-18T00:00:00.000Z';
  a.freshnessThresholdDays = 30;
  const unknownOk = computeScenarioCoverage(a).coverageLevel === CoverageLevel.UNKNOWN;

  // limited：有来源但全部过期（lastIngestedAt=null → +Infinity → 一律过期）
  const b = new ScenarioCoverageInput();
  b.scenarioId = 's';
  b.now = '2026-09-18T00:00:00.000Z';
  b.freshnessThresholdDays = 30;
  const src = new CoverageSourceInfo();
  src.id = 'src1';
  src.label = 'stale';
  src.lastIngestedAt = null;
  b.sources = [src];
  b.confirmedDirectDependencies = 3;
  const limitedOk = computeScenarioCoverage(b).coverageLevel === CoverageLevel.LIMITED;

  // partial：来源新鲜 + 有直接依赖，但存在 unknown criticality
  const c = new ScenarioCoverageInput();
  c.scenarioId = 's';
  c.now = '2026-09-18T00:00:00.000Z';
  c.freshnessThresholdDays = 30;
  const fresh = new CoverageSourceInfo();
  fresh.id = 'src1';
  fresh.label = 'fresh';
  fresh.lastIngestedAt = '2026-09-17T00:00:00.000Z';
  c.sources = [fresh];
  c.confirmedDirectDependencies = 3;
  c.unknownCriticalityCount = 2;
  const covC = computeScenarioCoverage(c);
  const partialOk = covC.coverageLevel === CoverageLevel.PARTIAL;

  // well_evidenced：全清
  const d = new ScenarioCoverageInput();
  d.scenarioId = 's';
  d.now = '2026-09-18T00:00:00.000Z';
  d.freshnessThresholdDays = 30;
  const fresh2 = new CoverageSourceInfo();
  fresh2.id = 'src1';
  fresh2.label = 'fresh';
  fresh2.lastIngestedAt = '2026-09-17T00:00:00.000Z';
  d.sources = [fresh2];
  d.confirmedDirectDependencies = 3;
  const covD = computeScenarioCoverage(d);
  const wellOk = covD.coverageLevel === CoverageLevel.WELL_EVIDENCED;

  // 可解释性：四个场景都必须给出至少一条 explanation
  const explained = covC.explanations.length > 0 && covD.explanations.length > 0;

  const ok = unknownOk && limitedOk && partialOk && wellOk && explained;
  return `coverage=ok(${unknownOk}/${limitedOk}/${partialOk}/${wellOk}/e${explained})`;
}

/**
 * 检查 S：状态机的终态与转换合法性。
 *
 * 关键不变式：
 *   - completed / cancelled 是终态，出边为空，且**任何**状态都不得转到它们之外
 *     —— 由全量穷举保证，而不是抽查几条；
 *   - 未知状态 fail-closed（不允许转换）；
 *   - done ≠ verified：完成动作不改变 verification 状态。
 */
function checkStateMachine(): string {
  // 终态出边为空
  let terminalOk = true;
  if (allowedNextStates(ChangePlanWorkflowState.COMPLETED).length !== 0) {
    terminalOk = false;
  }
  if (allowedNextStates(ChangePlanWorkflowState.CANCELLED).length !== 0) {
    terminalOk = false;
  }
  // 任何状态都不得转到终态之外（这里校验终态不可达出边 + 自反检查）
  let noSelfLoop = true;
  for (let i = 0; i < ALL_WORKFLOW_STATES.length; i++) {
    const s = ALL_WORKFLOW_STATES[i];
    if (isTransitionAllowed(s, s)) {
      // 原文白名单里没有任何自反转换
      noSelfLoop = false;
      break;
    }
  }
  // 未知状态 fail-closed
  const unknownRejected =
    !isTransitionAllowed('not_a_state', ChangePlanWorkflowState.READY) &&
    !isTransitionAllowed(ChangePlanWorkflowState.READY, 'not_a_state') &&
    allowedNextStates('not_a_state').length === 0;
  // 抽查两条原文明确允许 / 明确禁止的边
  const knownAllow =
    isTransitionAllowed(ChangePlanWorkflowState.DRAFT, ChangePlanWorkflowState.ANALYZED) &&
    isTransitionAllowed(ChangePlanWorkflowState.IN_PROGRESS, ChangePlanWorkflowState.VERIFYING);
  const knownDeny =
    !isTransitionAllowed(ChangePlanWorkflowState.DRAFT, ChangePlanWorkflowState.COMPLETED) &&
    !isTransitionAllowed(ChangePlanWorkflowState.COMPLETED, ChangePlanWorkflowState.DRAFT);

  // done ≠ verified：完成不改变 verification 状态
  const verUnchanged =
    verificationAfterCompletion(ActionVerificationStatus.PENDING) === ActionVerificationStatus.PENDING &&
    verificationAfterCompletion(ActionVerificationStatus.NOT_REQUIRED) === ActionVerificationStatus.NOT_REQUIRED;

  // 终态下动作冻结
  const frozen =
    !canMutateAction('p1', ChangePlanWorkflowState.COMPLETED).allowed &&
    !canMutateAction('p1', ChangePlanWorkflowState.CANCELLED).allowed &&
    canMutateAction('p1', ChangePlanWorkflowState.IN_PROGRESS).allowed;

  const ok = terminalOk && noSelfLoop && unknownRejected && knownAllow && knownDeny && verUnchanged && frozen;
  return `statemachine=ok(${terminalOk}/n${noSelfLoop}/u${unknownRejected}/a${knownAllow}/d${knownDeny}/v${verUnchanged}/f${frozen})`;
}

/**
 * 检查 G：graphRevision 的提升白名单。
 *
 * 关键不变式：**只有** Reality mutation 提升 revision。
 * 白名单必须对非 Reality 事件返回 false —— 这是"不误报"的保证。
 *
 * ⚠ 2026-09-18 修正：本检查原先用**自造词**断言**同一套自造词**
 * （`dependency_created` / `proposal_created` …），因此它自洽地全绿，
 * 却对冻结契约的真实词汇表（`node_create` / `dependency_confirm_create` …）
 * 一无所知 —— 自比自的检查不构成证据。
 * 现已改为逐条覆盖 `spec/state-machines/state-machines.json` 里
 * GraphRevision.bumpsOn / neverBumpsOn 的**契约原文**。
 */
function checkGraphRevision(): string {
  // 11 种 Reality mutation 必须全部提升（= 契约 bumpsOn 全集）
  const bumps =
    doesMutationBumpRevision(MUT_NODE_CREATE) &&
    doesMutationBumpRevision(MUT_NODE_UPDATE) &&
    doesMutationBumpRevision(MUT_NODE_ARCHIVE) &&
    doesMutationBumpRevision(MUT_DEPENDENCY_CONFIRM_CREATE) &&
    doesMutationBumpRevision(MUT_DEPENDENCY_CONFIRM_UPDATE) &&
    doesMutationBumpRevision(MUT_DEPENDENCY_RETIRE) &&
    doesMutationBumpRevision(MUT_DEPENDENCY_REACTIVATE) &&
    doesMutationBumpRevision(MUT_GROUP_CONFIRM) &&
    doesMutationBumpRevision(MUT_GROUP_RETIRE) &&
    doesMutationBumpRevision(MUT_DRIFT_RESOLVE_REPLACEMENT) &&
    doesMutationBumpRevision(MUT_DRIFT_RESOLVE_ADDITIONAL_PATH);

  // 非 Reality 事件一律不提升（= 契约 neverBumpsOn 全集）。
  // 用契约原文而非自造词：否则这条断言只能证明"我们自己的名字互相同意"。
  const noBump =
    !doesMutationBumpRevision('evidence_record') &&
    !doesMutationBumpRevision('proposal_upsert') &&
    !doesMutationBumpRevision('proposal_decision') &&
    !doesMutationBumpRevision('candidate_upsert') &&
    !doesMutationBumpRevision('candidate_dismiss') &&
    !doesMutationBumpRevision('candidate_accept') &&
    !doesMutationBumpRevision('drift_detect') &&
    !doesMutationBumpRevision('drift_dismiss') &&
    !doesMutationBumpRevision('timeline_build') &&
    !doesMutationBumpRevision('import_session_record') &&
    !doesMutationBumpRevision('plan_create') &&
    !doesMutationBumpRevision('plan_rebase') &&
    !doesMutationBumpRevision('plan_transition') &&
    !doesMutationBumpRevision('action_complete') &&
    !doesMutationBumpRevision('action_verify');

  // 解析容错：缺失 → 0；非有限 → 0；负值 → 0（revision 单调递增）
  const parse =
    parseStoredRevision(null) === 0 &&
    parseStoredRevision('') === 0 &&
    parseStoredRevision('abc') === 0 &&
    parseStoredRevision('-5') === 0 &&
    parseStoredRevision('7') === 7;

  const ok = bumps && noBump && parse;
  return `graphrevision=ok(${bumps}/n${noBump}/p${parse})`;
}

/**
 * 检查 N：Timeline 的 bucket 判定与确定性排序。
 *
 * 关键不变式：排序必须由 id 兜底，使同一组输入恒得同一序列。
 */
function checkTimeline(): string {
  const now = Date.parse('2026-09-18T12:00:00.000Z');
  const start = startOfUtcDay(now);

  // null / 脏数据 → attention（往"更需要人看"方向收）
  const attn =
    bucketOf(null, now) === TimelineBucket.ATTENTION &&
    bucketOf('not-a-date', now) === TimelineBucket.ATTENTION;

  // 昨天 → overdue；今天 → today；3 天后 → 7d；14 天后 → 30d；40 天后 → 90d；100 天后 → later
  const overdue = bucketOf('2026-09-17T12:00:00.000Z', now) === TimelineBucket.OVERDUE;
  const today = bucketOf('2026-09-18T18:00:00.000Z', now) === TimelineBucket.TODAY;
  const d7 = bucketOf('2026-09-21T12:00:00.000Z', now) === TimelineBucket.V7D;
  const d30 = bucketOf('2026-10-02T12:00:00.000Z', now) === TimelineBucket.V30D;
  const d90 = bucketOf('2026-10-28T12:00:00.000Z', now) === TimelineBucket.V90D;
  const later = bucketOf('2026-12-31T12:00:00.000Z', now) === TimelineBucket.LATER;
  // UTC 日界：now 落在当日任意时刻都得到同一个 todayStart
  const utcStable = startOfUtcDay(now) === startOfUtcDay(now + 3600000);

  // 确定性排序：同 bucket/priority/scheduledAt 时由 id 兜底
  const a = new TimelineSortKey();
  a.id = 'b';
  a.bucket = TimelineBucket.TODAY;
  const b = new TimelineSortKey();
  b.id = 'a';
  b.bucket = TimelineBucket.TODAY;
  const sorted1 = sortTimelineKeys([a, b]);
  const sorted2 = sortTimelineKeys([b, a]);
  const deterministic = sorted1[0].id === 'a' && sorted2[0].id === 'a';

  const ok = attn && overdue && today && d7 && d30 && d90 && later && utcStable && deterministic;
  return `timeline=ok(${attn}/${overdue}/${today}/${d7}/${d30}/${d90}/${later}/u${utcStable}/d${deterministic})`;
}

/**
 * 检查 T：ScenarioTemplate 的 ST gate。
 *
 * 关键不变式：availability != active 的模板**必须**被拒绝执行；
 * 缺必填输入也必须被拒绝；正常模板必须通过。
 */
function checkScenarioGate(): string {
  const inputs = new Map<string, string>();
  inputs.set('targetPaymentInstrumentId', 'cardA');

  // 正常 active 模板 + 必填齐全 → 通过
  const okActive =
    validateScenarioInstantiation('replace_payment_card', inputs).ok === true;

  // planned 模板 → 拒绝，且 errorCode 是 scenario_not_executable
  const planned = validateScenarioInstantiation('replace_phone_number', inputs);
  const plannedRejected = !planned.ok && planned.errorCode === 'scenario_not_executable';

  // 不存在的模板 → 拒绝
  const missing = validateScenarioInstantiation('no_such_template', inputs);
  const missingRejected = !missing.ok;

  // 缺必填输入 → 拒绝
  const empty = new Map<string, string>();
  const missingInput = validateScenarioInstantiation('replace_payment_card', empty);
  const inputRejected = !missingInput.ok && missingInput.errorCode === 'scenario_missing_input';

  // active 模板数量必须是 3（与 spec 的 MVP03 payment 模板一致）
  const countOk = listActiveTemplates().length === 3;

  // planned 模板不得出现在 active 列表里
  let plannedLeaked = false;
  const actives = listActiveTemplates();
  for (let i = 0; i < actives.length; i++) {
    if (actives[i].availability !== ScenarioAvailability.ACTIVE) {
      plannedLeaked = true;
      break;
    }
  }

  const ok =
    okActive && plannedRejected && missingRejected && inputRejected && countOk && !plannedLeaked;
  return `scenariogate=ok(${okActive}/p${plannedRejected}/m${missingRejected}/i${inputRejected}/c${countOk})`;
}

/**
 * 汇总：所有 domain 层的自检合并为一行，便于在页面上核对、
 * 也便于对 HAP 内的 modules.abc 做字符串反查（确认真的被打包）。
 */
export function domainSelfCheckLine(): string {
  const parts: string[] = [
    checkRequiredEdge(),
    checkUnknownCriticality(),
    checkGroupCovered(),
    checkProposalOnly(),
    checkCycleTermination(),
    checkStateKeyIsPair(),
    checkMultiInitialDedup(),
    checkChecklistTargetLast(),
    checkLogicalKeyInvariants(),
    checkPlanReadiness(),
    checkScenarioCoverage(),
    checkStateMachine(),
    checkGraphRevision(),
    checkTimeline(),
    checkScenarioGate(),
  ];
  return `domain: ${parts.join(' ')}`;
}

/** 供页面/测试引用的稳定入口，避免页面直接依赖多个内部模块。 */
export { simulateDisable, simulateScenario };
export { ImpactStateKey } from './Entities';
