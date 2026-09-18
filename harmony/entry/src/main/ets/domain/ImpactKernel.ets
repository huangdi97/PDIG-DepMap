// DO NOT EDIT 之外的语义 — 语义必须与 Android/TS 冻结基准一致
// 端口自 core/src/impact/kernel.ts（MVP payment domain, CANONICAL_DESIGN §7）
// 参考 Android 端口：android/core/src/main/kotlin/com/pdig/core/impact/ImpactKernel.kt
//
// 分层约束：本模块属于 domain 层，**不得**依赖 ArkUI / Ability / relationalStore /
// HUKS / NAPI / cryptoFramework。当前导入集合仅含 `./Entities` 与
// `../generated/CanonicalEnums`。
//
// 铁律（原文照录，逐条在代码中有对应实现）：
//  - 状态键是 (nodeId, capability)，不是 nodeId（禁止 visited: Set<nodeId>）
//  - 图允许有环：wave-BFS + visited key，每 key 最多处理一次
//  - Proposal 任何 confidence 都不参与确定性失效传播（最多 needs_review）
//  - confirmed Group 未满足性判定只在同 capability 内
//  - must_change 必须可追溯到 confirmed reality（false positive = 0）

import { Capability, Criticality, DependencyState, GroupMode, GroupState, ImpactReasonCode } from '../generated/CanonicalEnums';
import { Dependency, DependencyGroup, ImpactGraph, ImpactProposalInput, ImpactStateKey, dependencyLogicalKey } from './Entities';

export type ImpactLevel =
  'must_change' | 'backup_path' | 'degraded' | 'needs_review' | 'unaffected' | 'target_operation';

export class ImpactTargetResult {
  nodeId: string = '';
  nodeName: string = '';
  capability: Capability = Capability.PAYMENT;
  status: string = 'unaffected';
  available: boolean = true;
  redundancyDegraded: boolean = false;
  /** 失效传播深度（0 = 初始 unavailable 自身不产生 target；受影响目标从 1 开始） */
  depth: number = 0;
  reasonCode: string = '';
  reasonText: string = '';
  edgeKeys: string[] = [];
  groupKeys: string[] = [];
  proposalKeys: string[] = [];
}

export class ImpactChecklistItem {
  level: string = 'needs_review';
  nodeId: string | null = null;
  capability: Capability | null = null;
  title: string = '';
  detail: string = '';
}

export class ImpactResult {
  unavailable: ImpactStateKey[] = [];
  /** 最终失效键集合（含初始 unavailable），每个 key 至多出现一次 */
  lostKeys: ImpactStateKey[] = [];
  /** 按 (depth, nodeId, capability) 确定性排序 */
  targets: ImpactTargetResult[] = [];
  /** Action Checklist；原始注销/停用动作强制最后 */
  checklist: ImpactChecklistItem[] = [];
  /** 处理过的 key（顺序记录，无重复）— 用于环终止测试 */
  processedKeys: string[] = [];
}

const PAYMENT: Capability = Capability.PAYMENT;

function keyStr(k: ImpactStateKey): string {
  return `${k.nodeId}|${k.capability}`;
}

/** 按 (nodeId, capability) 升序 —— 与原实现同一比较顺序，保证跨端确定性一致。 */
function cloneSortedKeys(keys: ImpactStateKey[]): ImpactStateKey[] {
  const out: ImpactStateKey[] = [];
  for (let i = 0; i < keys.length; i++) {
    out.push(new ImpactStateKey(keys[i].nodeId, keys[i].capability));
  }
  out.sort((a: ImpactStateKey, b: ImpactStateKey): number => {
    if (a.nodeId < b.nodeId) {
      return -1;
    }
    if (a.nodeId > b.nodeId) {
      return 1;
    }
    if (a.capability < b.capability) {
      return -1;
    }
    if (a.capability > b.capability) {
      return 1;
    }
    return 0;
  });
  return out;
}

function byIdAsc(aId: string, bId: string): number {
  if (aId < bId) {
    return -1;
  }
  if (aId > bId) {
    return 1;
  }
  return 0;
}

/** 确定性去重 + 升序（用于 edgeKeys / groupKeys / proposalKeys 合并）。 */
function dedupSorted(values: string[]): string[] {
  const out: string[] = [];
  for (let i = 0; i < values.length; i++) {
    const v = values[i];
    let seen = false;
    for (let j = 0; j < out.length; j++) {
      if (out[j] === v) {
        seen = true;
        break;
      }
    }
    if (!seen) {
      out.push(v);
    }
  }
  out.sort();
  return out;
}

function severity(s: string): number {
  if (s === 'must_change') {
    return 3;
  }
  if (s === 'backup_path' || s === 'degraded') {
    return 2;
  }
  if (s === 'needs_review') {
    return 1;
  }
  return 0;
}

export function simulateDisable(
  graph: ImpactGraph,
  nodeId: string,
  capability: Capability = PAYMENT,
): ImpactResult {
  const set: ImpactStateKey[] = [new ImpactStateKey(nodeId, capability)];
  return simulateScenario(graph, set);
}

/**
 * evaluateTarget 的求值上下文。
 *
 * 为什么抽成显式上下文对象（ArkTS 约束驱动，不是风格偏好）：
 *   ArkTS 禁止嵌套函数声明（`arkts-no-nested-funcs`）——
 *   原 TS 实现里的 `function evaluateTarget(T, depth)` 直接搬到 .ets 会编译失败：
 *     ImpactKernel.ets:257:3 Nested functions are not supported
 *   而该函数捕获了 7 个局部量（activePaymentDeps / activePaymentGroups /
 *   paymentProposals / unavailableSetHas / uncertainSetHas / nodeName）。
 *   把它们显式收进一个上下文类，既满足 ArkTS，也让依赖关系可见 ——
 *   比让一个 200 行的闭包隐式捕获更可读、更可测。
 */
class EvalContext {
  activePaymentDeps: Dependency[] = [];
  activePaymentGroups: DependencyGroup[] = [];
  paymentProposals: ImpactProposalInput[] = [];
  nodeNames: Map<string, string> = new Map<string, string>();
  unavailableSeen: string[] = [];
  uncertainSeen: string[] = [];

  hasUnavailable(k: string): boolean {
    for (let i = 0; i < this.unavailableSeen.length; i++) {
      if (this.unavailableSeen[i] === k) {
        return true;
      }
    }
    return false;
  }

  hasUncertain(nodeId: string): boolean {
    for (let i = 0; i < this.uncertainSeen.length; i++) {
      if (this.uncertainSeen[i] === nodeId) {
        return true;
      }
    }
    return false;
  }

  nodeName(id: string): string {
    const v = this.nodeNames.get(id);
    return v === undefined ? id : v;
  }
}

function edgeKeysOf2(ds: Dependency[]): string[] {
  const out: string[] = [];
  for (let i = 0; i < ds.length; i++) {
    out.push(dependencyLogicalKey(ds[i].from, ds[i].relation, ds[i].to, ds[i].capability));
  }
  return out.sort();
}

/**
 * 评估目标节点 T 的 payment 状态（基于当前 unavailable/uncertain 快照）。
 *
 * 迁移注意：本函数是 `core/src/impact/kernel.ts` 中同名嵌套函数的**逐分支搬运**。
 * 每条 return 分支的 status / available / redundancyDegraded / reasonCode /
 * reasonText / edgeKeys / groupKeys / proposalKeys 都必须与原实现一一对应，
 * 不得因为"看起来更合理"而改动 —— 这些取值是跨端 conformance 的比较对象。
 */
function evaluateTarget(ctx: EvalContext, T: string, depth: number): ImpactTargetResult {
  const incoming: Dependency[] = [];
  for (let i = 0; i < ctx.activePaymentDeps.length; i++) {
    if (ctx.activePaymentDeps[i].to === T) {
      incoming.push(ctx.activePaymentDeps[i]);
    }
  }

  const lostEdges: Dependency[] = [];
  const uncertainEdges: Dependency[] = [];
  for (let i = 0; i < incoming.length; i++) {
    const d = incoming[i];
    const fromKey = `${d.from}|${d.capability}`;
    if (ctx.hasUnavailable(fromKey)) {
      lostEdges.push(d);
    } else if (ctx.hasUncertain(d.from)) {
      uncertainEdges.push(d);
    }
  }

  const r = new ImpactTargetResult();
  r.nodeId = T;
  r.nodeName = ctx.nodeName(T);
  r.capability = PAYMENT;
  r.depth = depth;

  if (lostEdges.length === 0) {
    // 无 confirmed 入边失效；检查上游不确定性与 proposal
    const propKeys: string[] = [];
    for (let i = 0; i < ctx.paymentProposals.length; i++) {
      const p = ctx.paymentProposals[i];
      if (p.to === T && ctx.hasUnavailable(`${p.from}|${p.capability}`)) {
        propKeys.push(p.key);
      }
    }
    propKeys.sort();

    if (uncertainEdges.length > 0) {
      r.status = 'needs_review';
      r.available = true;
      r.redundancyDegraded = false;
      r.reasonCode = ImpactReasonCode.UPSTREAM_UNCERTAIN;
      r.reasonText = `上游支付能力存在未确认风险，${ctx.nodeName(T)} 的支付是否受影响需人工核实`;
      r.edgeKeys = edgeKeysOf2(uncertainEdges);
      r.groupKeys = [];
      r.proposalKeys = [];
      return r;
    }
    if (propKeys.length > 0) {
      r.status = 'needs_review';
      r.available = true;
      r.redundancyDegraded = false;
      r.reasonCode = ImpactReasonCode.PROPOSAL_ONLY;
      r.reasonText = `检测到未确认的支付关系建议（置信度不改变结论），需人工核实 ${ctx.nodeName(T)} 的支付是否受影响`;
      r.edgeKeys = [];
      r.groupKeys = [];
      r.proposalKeys = propKeys;
      return r;
    }
    // 与场景无关（不应被调用）
    r.status = 'unaffected';
    r.available = true;
    r.redundancyDegraded = false;
    r.reasonCode = ImpactReasonCode.CRITICALITY_UNKNOWN;
    r.reasonText = '未发现受影响的已确认支付关系';
    r.edgeKeys = [];
    r.groupKeys = [];
    r.proposalKeys = [];
    return r;
  }

  const lostEdgeIds: string[] = [];
  for (let i = 0; i < lostEdges.length; i++) {
    lostEdgeIds.push(lostEdges[i].id);
  }
  const isLostEdgeId = (id: string): boolean => {
    for (let i = 0; i < lostEdgeIds.length; i++) {
      if (lostEdgeIds[i] === id) {
        return true;
      }
    }
    return false;
  };

  const coveringGroups: DependencyGroup[] = [];
  for (let i = 0; i < ctx.activePaymentGroups.length; i++) {
    const g = ctx.activePaymentGroups[i];
    if (g.targetNodeId !== T) {
      continue;
    }
    let touchesLost = false;
    for (let j = 0; j < g.memberEdgeIds.length; j++) {
      if (isLostEdgeId(g.memberEdgeIds[j])) {
        touchesLost = true;
        break;
      }
    }
    if (touchesLost) {
      coveringGroups.push(g);
    }
  }

  if (coveringGroups.length > 0) {
    let allFailed = true;
    for (let i = 0; i < coveringGroups.length; i++) {
      const g = coveringGroups[i];
      const memberEdges: Dependency[] = [];
      for (let j = 0; j < ctx.activePaymentDeps.length; j++) {
        const d = ctx.activePaymentDeps[j];
        let inGroup = false;
        for (let m = 0; m < g.memberEdgeIds.length; m++) {
          if (g.memberEdgeIds[m] === d.id) {
            inGroup = true;
            break;
          }
        }
        if (inGroup) {
          memberEdges.push(d);
        }
      }
      let availableCount = 0;
      for (let j = 0; j < memberEdges.length; j++) {
        const d = memberEdges[j];
        if (!ctx.hasUnavailable(`${d.from}|${d.capability}`)) {
          availableCount += 1;
        }
      }
      const satisfied =
        g.mode === GroupMode.ANY
          ? availableCount > 0
          : availableCount === memberEdges.length;
      if (satisfied) {
        allFailed = false;
      }
    }

    const groupKeys = dedupSorted(coveringGroups.map((g: DependencyGroup): string => g.groupKey));

    if (allFailed) {
      r.status = 'must_change';
      r.available = false;
      r.redundancyDegraded = false;
      r.reasonCode = ImpactReasonCode.CONFIRMED_GROUP_FAILED;
      r.reasonText =
        `已确认的支付来源组合（${coveringGroups.map((g: DependencyGroup): string => g.mode).join('/')}）全部失效，${ctx.nodeName(T)} 的支付能力将失效`;
      r.edgeKeys = edgeKeysOf2(lostEdges);
      r.groupKeys = groupKeys;
      r.proposalKeys = [];
      return r;
    }
    r.status = 'backup_path';
    r.available = true;
    r.redundancyDegraded = true;
    r.reasonCode = ImpactReasonCode.CONFIRMED_GROUP_COVERED;
    r.reasonText = `已确认存在替代支付来源，${ctx.nodeName(T)} 的支付可继续，但冗余度下降（能力降级）`;
    r.edgeKeys = edgeKeysOf2(lostEdges);
    r.groupKeys = groupKeys;
    r.proposalKeys = [];
    return r;
  }

  const otherEdges: Dependency[] = [];
  for (let i = 0; i < incoming.length; i++) {
    if (!isLostEdgeId(incoming[i].id)) {
      otherEdges.push(incoming[i]);
    }
  }
  if (otherEdges.length > 0) {
    r.status = 'needs_review';
    r.available = true;
    r.redundancyDegraded = false;
    r.reasonCode = ImpactReasonCode.UNCONFIRMED_ALTERNATIVE_EXISTS;
    r.reasonText = `检测到其他支付来源，但未确认备用组合可自动接管，需人工核实 ${ctx.nodeName(T)} 的支付路径`;
    r.edgeKeys = edgeKeysOf2(incoming);
    r.groupKeys = [];
    r.proposalKeys = [];
    return r;
  }

  let anyRequired = false;
  for (let i = 0; i < lostEdges.length; i++) {
    if (lostEdges[i].criticality === Criticality.REQUIRED) {
      anyRequired = true;
      break;
    }
  }
  if (anyRequired) {
    r.status = 'must_change';
    r.available = false;
    r.redundancyDegraded = false;
    r.reasonCode = ImpactReasonCode.REQUIRED_EDGE_NO_ALTERNATIVE;
    r.reasonText = `已确认 ${ctx.nodeName(T)} 的支付能力依赖此关系（required），且无其他已记录来源`;
    r.edgeKeys = edgeKeysOf2(lostEdges);
    r.groupKeys = [];
    r.proposalKeys = [];
    return r;
  }

  r.status = 'needs_review';
  r.available = true;
  r.redundancyDegraded = false;
  r.reasonCode = ImpactReasonCode.CRITICALITY_UNKNOWN;
  r.reasonText = `该支付关系未确认是否必需（criticality=unknown），需人工核实 ${ctx.nodeName(T)} 是否受影响`;
  r.edgeKeys = edgeKeysOf2(lostEdges);
  r.groupKeys = [];
  r.proposalKeys = [];
  return r;
}

/**
 * 模拟一组不可用 (nodeId, capability) 的失效传播。
 *
 * 输入用数组而非 Set：ArkTS 对 Set 的迭代/相等语义支持有限，
 * 而原 TS 实现本身就是「按 keyStr 值级去重」，用数组 + 显式去重
 * 与原文语义等价，且不依赖任何平台特性。
 */
export function simulateScenario(graph: ImpactGraph, unavailable: ImpactStateKey[]): ImpactResult {
  // MVP 只支持 payment domain：非 payment 初始键忽略（记录在文档）。
  // 按 keyStr 值级去重，保证 unavailable/lostKeys/processedKeys 输出无重复
  // （invariant：processedKeys 唯一）。
  const initialSeen: string[] = [];
  const initialCandidates: ImpactStateKey[] = [];
  for (let i = 0; i < unavailable.length; i++) {
    const k = unavailable[i];
    if (k.capability !== PAYMENT) {
      continue;
    }
    const s = keyStr(k);
    let seen = false;
    for (let j = 0; j < initialSeen.length; j++) {
      if (initialSeen[j] === s) {
        seen = true;
        break;
      }
    }
    if (seen) {
      continue;
    }
    initialSeen.push(s);
    initialCandidates.push(k);
  }
  const initial = cloneSortedKeys(initialCandidates);

  const activePaymentDeps: Dependency[] = [];
  for (let i = 0; i < graph.dependencies.length; i++) {
    const d = graph.dependencies[i];
    if (d.state === DependencyState.ACTIVE && d.capability === PAYMENT) {
      activePaymentDeps.push(d);
    }
  }
  activePaymentDeps.sort((a: Dependency, b: Dependency): number => byIdAsc(a.id, b.id));

  const activePaymentGroups: DependencyGroup[] = [];
  for (let i = 0; i < graph.groups.length; i++) {
    const g = graph.groups[i];
    if (g.state === GroupState.ACTIVE && g.capability === PAYMENT) {
      activePaymentGroups.push(g);
    }
  }
  activePaymentGroups.sort((a: DependencyGroup, b: DependencyGroup): number => byIdAsc(a.id, b.id));

  const paymentProposals: ImpactProposalInput[] = [];
  for (let i = 0; i < graph.proposals.length; i++) {
    const p = graph.proposals[i];
    if (p.capability === PAYMENT) {
      paymentProposals.push(p);
    }
  }

  const unavailableSeen: string[] = [];
  const uncertainSeen: string[] = [];
  const processedSeen: string[] = [];

  // evaluateTarget 的上下文：**共享同一批数组引用**，不是拷贝。
  // 这一点至关重要 —— wave-BFS 过程中 unavailableSeen / uncertainSeen 会持续增长，
  // evaluateTarget 每次调用都必须看到最新的集合。若在此处做浅拷贝，
  // 后续新增的不可用键对求值不可见，传播会在第一轮后停住。
  const ctx = new EvalContext();
  ctx.activePaymentDeps = activePaymentDeps;
  ctx.activePaymentGroups = activePaymentGroups;
  ctx.paymentProposals = paymentProposals;
  ctx.nodeNames = graph.nodeNames;
  ctx.unavailableSeen = unavailableSeen;
  ctx.uncertainSeen = uncertainSeen;

  const unavailableSetHas = (k: string): boolean => ctx.hasUnavailable(k);
  const uncertainSetHas = (nodeId: string): boolean => ctx.hasUncertain(nodeId);

  const lostKeys: ImpactStateKey[] = [];
  const processedKeys: string[] = [];
  for (let i = 0; i < initial.length; i++) {
    const s = keyStr(initial[i]);
    unavailableSeen.push(s);
    lostKeys.push(new ImpactStateKey(initial[i].nodeId, initial[i].capability));
    processedKeys.push(s);
    processedSeen.push(s);
  }

  const resultKeys: string[] = [];
  const resultValues: ImpactTargetResult[] = [];

  const findResult = (k: string): number => {
    for (let i = 0; i < resultKeys.length; i++) {
      if (resultKeys[i] === k) {
        return i;
      }
    }
    return -1;
  };


  // wave-BFS：unavailable / uncertain 只增长 → 天然防环终止
  // lost 传播确定性失效；needs_review（不确定）也向下游传播"不确定性"，
  // 但永不产生 must_change（CANONICAL §7 场景 A：WeChat needs_review → TencentVideo needs_review）
  const initialNodeIds: string[] = [];
  for (let i = 0; i < initial.length; i++) {
    const n = initial[i].nodeId;
    let seen = false;
    for (let j = 0; j < initialNodeIds.length; j++) {
      if (initialNodeIds[j] === n) {
        seen = true;
        break;
      }
    }
    if (!seen) {
      initialNodeIds.push(n);
    }
  }
  const isInitialNode = (n: string): boolean => {
    for (let i = 0; i < initialNodeIds.length; i++) {
      if (initialNodeIds[i] === n) {
        return true;
      }
    }
    return false;
  };

  let frontier: string[] = [];
  for (let i = 0; i < initial.length; i++) {
    frontier.push(initial[i].nodeId);
  }
  let depth = 0;
  let guard = 0;
  const maxGuard = activePaymentDeps.length * 2 + initial.length + 8;

  const markProcessed = (k: string): void => {
    let seen = false;
    for (let i = 0; i < processedSeen.length; i++) {
      if (processedSeen[i] === k) {
        seen = true;
        break;
      }
    }
    if (!seen) {
      processedSeen.push(k);
      processedKeys.push(k);
    }
  };

  while (frontier.length > 0 && guard <= maxGuard) {
    guard += 1;
    depth += 1;

    // 收集本轮需要评估的目标：frontier 的 active payment 出边目标 +
    // 初始 unavailable 节点的 pending proposal 目标（proposal 不驱动下游级联）
    const pending: string[] = [];
    const addPending = (n: string): void => {
      for (let i = 0; i < pending.length; i++) {
        if (pending[i] === n) {
          return;
        }
      }
      pending.push(n);
    };
    for (let i = 0; i < frontier.length; i++) {
      const n = frontier[i];
      for (let j = 0; j < activePaymentDeps.length; j++) {
        const d = activePaymentDeps[j];
        if (d.from === n && !unavailableSetHas(`${d.to}|${d.capability}`)) {
          addPending(d.to);
        }
      }
      if (isInitialNode(n)) {
        for (let j = 0; j < paymentProposals.length; j++) {
          const p = paymentProposals[j];
          if (p.from === n && !unavailableSetHas(`${p.to}|${p.capability}`)) {
            addPending(p.to);
          }
        }
      }
    }
    const orderedPending = pending.sort();

    const nextFrontier: string[] = [];
    let grew = false;

    for (let i = 0; i < orderedPending.length; i++) {
      const T = orderedPending[i];
      const k = `${T}|${PAYMENT}`;
      if (unavailableSetHas(k)) {
        continue;
      }
      const result = evaluateTarget(ctx, T, depth);
      const idx = findResult(k);
      if (idx < 0) {
        resultKeys.push(k);
        resultValues.push(result);
      } else {
        const prev = resultValues[idx];
        const sevNew = severity(result.status);
        const sevPrev = severity(prev.status);
        // 状态合并：更差结果覆盖（must_change > backup_path > needs_review > unaffected）
        if (sevNew > sevPrev) {
          resultValues[idx] = result;
        } else if (sevNew === sevPrev) {
          prev.edgeKeys = dedupSorted(prev.edgeKeys.concat(result.edgeKeys));
          prev.groupKeys = dedupSorted(prev.groupKeys.concat(result.groupKeys));
          prev.proposalKeys = dedupSorted(prev.proposalKeys.concat(result.proposalKeys));
        }
      }

      if (result.status === 'must_change' && !unavailableSetHas(k)) {
        unavailableSeen.push(k);
        lostKeys.push(new ImpactStateKey(T, PAYMENT));
        markProcessed(k);
        // uncertainSet.delete(T)：以删除语义处理，保持与原文一致
        for (let j = uncertainSeen.length - 1; j >= 0; j--) {
          if (uncertainSeen[j] === T) {
            uncertainSeen.splice(j, 1);
          }
        }
        nextFrontier.push(T);
        grew = true;
      } else if (result.status === 'needs_review' && !uncertainSetHas(T)) {
        // 不确定性传播：needs_review 下游继续 needs_review，但不参与失效传播
        uncertainSeen.push(T);
        markProcessed(k);
        nextFrontier.push(T);
        grew = true;
      }
      // backup_path / unaffected：不传播（支付可继续 / 无关）
    }

    if (!grew) {
      break;
    }
    frontier = nextFrontier;
  }

  resultValues.sort((a: ImpactTargetResult, b: ImpactTargetResult): number => {
    if (a.depth !== b.depth) {
      return a.depth - b.depth;
    }
    if (a.nodeId < b.nodeId) {
      return -1;
    }
    if (a.nodeId > b.nodeId) {
      return 1;
    }
    if (a.capability < b.capability) {
      return -1;
    }
    return 1;
  });

  const checklist: ImpactChecklistItem[] = [];
  for (let i = 0; i < resultValues.length; i++) {
    const t = resultValues[i];
    if (t.status === 'unaffected') {
      continue;
    }
    const item = new ImpactChecklistItem();
    item.nodeId = t.nodeId;
    item.capability = t.capability;
    item.detail = t.reasonText;
    if (t.status === 'must_change') {
      item.level = 'must_change';
      item.title = `必须处理：${t.nodeName} 的支付能力将失效`;
    } else if (t.status === 'backup_path') {
      item.level = 'backup_path';
      item.title = `有备用路径：${t.nodeName} 可切换（能力降级）`;
    } else {
      item.level = 'needs_review';
      item.title = `建议检查：${t.nodeName}`;
    }
    checklist.push(item);
  }
  // 原始注销/停用动作强制最后
  for (let i = 0; i < initial.length; i++) {
    const k = initial[i];
    const item = new ImpactChecklistItem();
    item.level = 'target_operation';
    item.nodeId = k.nodeId;
    item.capability = k.capability;
    item.title = `最后一步：注销/停用 ${ctx.nodeName(k.nodeId)}（原始操作）`;
    item.detail = '以上事项处理完成后再执行原始操作。';
    checklist.push(item);
  }

  return {
    unavailable: initial,
    lostKeys: lostKeys,
    targets: resultValues,
    checklist: checklist,
    processedKeys: processedKeys,
  };
}
