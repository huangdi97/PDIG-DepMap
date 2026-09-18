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

import { Capability, Criticality, DependencyOrigin, DependencyState, GroupMode, GroupState, ImpactReasonCode, Relation } from '../generated/CanonicalEnums';
import { Dependency, DependencyGroup, ImpactGraph, ImpactProposalInput, ImpactStateKey, canonicalGroupKey, dependencyLogicalKey } from './Entities';
import { simulateDisable, simulateScenario } from './ImpactKernel';
import { groupKeyDedupsMembers, groupKeyIsOrderIndependent, logicalKeyRoundTrips, parseDependencyLogicalKey } from './LogicalKey';

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
  ];
  return `domain: ${parts.join(' ')}`;
}

/** 供页面/测试引用的稳定入口，避免页面直接依赖多个内部模块。 */
export { simulateDisable, simulateScenario };
export { ImpactStateKey } from './Entities';
