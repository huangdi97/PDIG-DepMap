// RecoveryCycle — Canonical v0.3.0（port of core/src/impact/recovery-cycle.ts）。
//
// 铁律：
// - 只有 Confirmed Reality 上的 active 边才能产生 confirmed_cycle（V030-RC-01）。
// - Proposal / Candidate 最多产生 potential_cycle。
// - retired 边不参与（如同 Impact kernel 的 retired 不传播）。
// - capability 分开检测（mixed capability 不产生 confirmed cycle）。
//
// 分层约束：本模块是 impact 层纯逻辑，**零 @ohos 依赖**。环检测为确定性输出：
// 环被归一化为「字典序最小的旋转 + 首节点最小」后去重排序。

import { Capability, DependencyState, RecoveryCycleStatus } from '../generated/CanonicalEnums';
import { Dependency } from '../domain/Entities';

/** 由 pending Proposal / DiscoveryCandidate 暗示的边。 */
export class HintedEdge {
  from: string = '';
  to: string = '';
}

/** detectRecoveryCycles 的输入。 */
export class RecoveryCycleInput {
  capability: Capability = Capability.PAYMENT;
  /** 已确认 Reality 的 active 边（同一 capability）。 */
  dependencies: Dependency[] = [];
  /** pending Proposal / DiscoveryCandidate 暗示的边（同一 capability）。 */
  hintedEdges: HintedEdge[] = [];
  /** 检测上限，防止超长路径爆栈。 */
  maxPathLength: number | null = null;
}

/** detectRecoveryCycles 的确定性结果。 */
export class RecoveryCycleResult {
  status: RecoveryCycleStatus = RecoveryCycleStatus.NO_CYCLE;
  /** confirmed 环的节点路径（若有；确定性排序）。 */
  confirmedCycles: string[][] = [];
  /** 仅由 proposal/candidate 支撑的潜在环（用户需确认）。 */
  potentialCycles: string[][] = [];
  /** 参与检测的 capability。 */
  capability: Capability = Capability.PAYMENT;
}

/** 内部边表示。 */
class CycleEdge {
  from: string = '';
  to: string = '';
}

const DEFAULT_MAX_PATH_LENGTH = 16;

function stringLess(a: string, b: string): boolean {
  return a < b;
}

/** 环归一化：找字典序最小的旋转（并要求该旋转首节点最小）。 */
function normalizeCycle(path: string[]): string {
  const rotations: string[] = [];
  for (let i = 0; i < path.length; i++) {
    const rotated: string[] = [];
    for (let j = i; j < path.length; j++) {
      rotated.push(path[j]);
    }
    for (let j = 0; j < i; j++) {
      rotated.push(path[j]);
    }
    rotations.push(rotated.join('|'));
  }
  rotations.sort();
  return rotations.length > 0 ? rotations[0] : path.join('|');
}

/**
 * DFS 的递归步进（作为模块级私有函数而非闭包，规避 ArkTS 对嵌套函数声明的限制）。
 * 参数携带全部状态：stack / visited / start / maxLen。
 */
function walkCycle(
  current: string,
  start: string,
  stack: string[],
  visited: Set<string>,
  adj: Map<string, string[]>,
  maxLen: number,
  cycles: Set<string>,
): void {
  const nexts = adj.get(current);
  if (nexts === undefined) {
    return;
  }
  for (let i = 0; i < nexts.length; i++) {
    const next = nexts[i];
    if (next === start) {
      if (stack.length >= 2 && stack.length <= maxLen) {
        cycles.add(normalizeCycle(stack));
      }
      continue;
    }
    if (visited.has(next)) {
      continue;
    }
    if (stack.length >= maxLen) {
      continue;
    }
    visited.add(next);
    stack.push(next);
    walkCycle(next, start, stack, visited, adj, maxLen, cycles);
    stack.pop();
    visited.delete(next);
  }
}

/**
 * 在「边集」内找有向环。确定性：邻接表按目标排序，起点按字典序，
 * 环串去重后按字典序（长度升序）排序。
 */
function findCycles(edges: CycleEdge[], maxLen: number): string[][] {
  const adj = new Map<string, string[]>();
  for (let i = 0; i < edges.length; i++) {
    const existing = adj.get(edges[i].from);
    if (existing === undefined) {
      const list: string[] = [];
      list.push(edges[i].to);
      adj.set(edges[i].from, list);
    } else {
      existing.push(edges[i].to);
    }
  }
  for (const list of adj.values()) {
    list.sort();
  }

  const cycles = new Set<string>();
  const nodes: string[] = [];
  for (const entry of adj) {
    nodes.push(entry[0]);
  }
  nodes.sort();

  for (let n = 0; n < nodes.length; n++) {
    const start = nodes[n];
    const stack: string[] = [];
    stack.push(start);
    const visited = new Set<string>();
    visited.add(start);
    walkCycle(start, start, stack, visited, adj, maxLen, cycles);
  }

  const result: string[][] = [];
  const cycleStrs = Array.from(cycles);
  for (let i = 0; i < cycleStrs.length; i++) {
    result.push(cycleStrs[i].split('|'));
  }
  result.sort((a: string[], b: string[]): number => {
    const n = a.length < b.length ? a.length : b.length;
    for (let i = 0; i < n; i++) {
      if (a[i] !== b[i]) {
        return stringLess(a[i], b[i]) ? -1 : 1;
      }
    }
    return a.length - b.length;
  });
  return result;
}

/** 判断环是否至少用了一条 hinted 边（否则它就是 confirmed 环）。 */
function cycleUsesHint(cycle: string[], hinted: HintedEdge[]): boolean {
  for (let i = 0; i < cycle.length; i++) {
    const from = cycle[i];
    const to = cycle[(i + 1) % cycle.length];
    for (let j = 0; j < hinted.length; j++) {
      if (hinted[j].from === from && hinted[j].to === to) {
        return true;
      }
    }
  }
  return false;
}

export function detectRecoveryCycles(input: RecoveryCycleInput): RecoveryCycleResult {
  const maxLen = input.maxPathLength === null ? DEFAULT_MAX_PATH_LENGTH : input.maxPathLength;
  const confirmedEdges: CycleEdge[] = [];
  for (let i = 0; i < input.dependencies.length; i++) {
    const d = input.dependencies[i];
    if (d.capability === input.capability && d.state === DependencyState.ACTIVE) {
      const e = new CycleEdge();
      e.from = d.from;
      e.to = d.to;
      confirmedEdges.push(e);
    }
  }
  // 自环无意义（恢复自依赖需要另一跳才成环）
  const hinted: HintedEdge[] = [];
  for (let i = 0; i < input.hintedEdges.length; i++) {
    if (input.hintedEdges[i].from !== input.hintedEdges[i].to) {
      hinted.push(input.hintedEdges[i]);
    }
  }

  const confirmedCycles = findCycles(confirmedEdges, maxLen);
  if (confirmedCycles.length > 0) {
    const result = new RecoveryCycleResult();
    result.status = RecoveryCycleStatus.CONFIRMED_CYCLE;
    result.confirmedCycles = confirmedCycles;
    result.potentialCycles = [];
    result.capability = input.capability;
    return result;
  }

  // 潜在环：confirmed 边 + hinted 边的并集上存在环，且该环必须用到至少一条 hinted 边，
  // 否则它就是 confirmed 环（上一分支会抓住）。
  const merged: CycleEdge[] = [];
  for (let i = 0; i < confirmedEdges.length; i++) {
    merged.push(confirmedEdges[i]);
  }
  for (let i = 0; i < hinted.length; i++) {
    const e = new CycleEdge();
    e.from = hinted[i].from;
    e.to = hinted[i].to;
    merged.push(e);
  }
  const allCycles = findCycles(merged, maxLen);
  const potentialCycles: string[][] = [];
  for (let i = 0; i < allCycles.length; i++) {
    if (cycleUsesHint(allCycles[i], hinted)) {
      potentialCycles.push(allCycles[i]);
    }
  }

  const result = new RecoveryCycleResult();
  result.capability = input.capability;
  if (potentialCycles.length > 0) {
    result.status = RecoveryCycleStatus.POTENTIAL_CYCLE;
    result.confirmedCycles = [];
    result.potentialCycles = potentialCycles;
  } else {
    result.status = RecoveryCycleStatus.NO_CYCLE;
    result.confirmedCycles = [];
    result.potentialCycles = [];
  }
  return result;
}