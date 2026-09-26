// FailureDomain — Canonical v0.3.0（port of core/src/domain/failure-domain.ts）。
//
// 铁律：
// - FailureDomain 本身服从 Reality Boundary：只有用户确认或权威证据才允许 status=confirmed；
//   机器推断最多产生 needs_review（V030-FD-01 / FD-CONFIRM-GATE）。
// - 两个恢复/认证路径共享同一故障域 → 不独立（V030-IP-01）。
// - pathCount ≠ independentPathCount：路径数多不代表独立路径多。
//
// 分层约束：本模块是 domain 层纯逻辑，**零 @ohos 依赖**，只 import generated 枚举与
// domain/Entities 的 Dependency。结果字段全部确定性排序，供 conformance 逐字节比对。

import { Capability, DependencyState } from '../generated/CanonicalEnums';
import { Dependency } from './Entities';

/** computePathIndependence 的确定性结果。 */
export class PathIndependenceResult {
  targetNodeId: string = '';
  capability: Capability = Capability.PAYMENT;
  /** 候选恢复/认证路径总数（confirmed active edges 指向 target 的边）。 */
  pathCount: number = 0;
  /** 互不共享故障域的路径数。 */
  independentPathCount: number = 0;
  /** 被确认共享的故障域（dedup + 确定性排序）。 */
  sharedFailureDomains: string[] = [];
  /** 机器无法自行判定的假设（needs_review 的共享可能）。 */
  unresolvedAssumptions: string[] = [];
}

/** computePathIndependence 的输入。 */
export class PathIndependenceInput {
  targetNodeId: string = '';
  capability: Capability = Capability.PAYMENT;
  /** 只使用 active + 指定 capability 的边（retired 排除）。 */
  dependencies: Dependency[] = [];
  /** 已确认的故障域。key = `${kind}|${subjectRef}`，value 为 id。 */
  confirmedDomains: Map<string, string> = new Map<string, string>();
  /** 机器推断的可能故障域（needs_review）。 */
  suspectedDomains: Map<string, string> = new Map<string, string>();
  /** 边 id → 它所属的故障域 key 列表。 */
  edgeToDomainKeys: Map<string, string[]> = new Map<string, string[]>();
}

/** buildEdgeToDomainIndex 的边引用（本函数不读取 from）。 */
export class DomainEdgeRef {
  id: string = '';
  from: string = '';
}

/** 去重后按字典序排序（TS `[...new Set(x)].sort()` 的等价实现）。 */
export function sortUnique(items: string[]): string[] {
  const dedup: string[] = [];
  for (let i = 0; i < items.length; i++) {
    let seen = false;
    for (let j = 0; j < dedup.length; j++) {
      if (dedup[j] === items[i]) {
        seen = true;
        break;
      }
    }
    if (!seen) {
      dedup.push(items[i]);
    }
  }
  dedup.sort();
  return dedup;
}

/**
 * 计算 target 的恢复/认证路径独立程度。
 * 确定性：结果字段全部排序；共享故障域取 union 后排序。
 */
export function computePathIndependence(input: PathIndependenceInput): PathIndependenceResult {
  const edges: Dependency[] = [];
  for (let i = 0; i < input.dependencies.length; i++) {
    const d = input.dependencies[i];
    if (
      d.to === input.targetNodeId &&
      d.capability === input.capability &&
      d.state === DependencyState.ACTIVE
    ) {
      edges.push(d);
    }
  }
  const pathCount = edges.length;

  const sharedKeys: string[] = [];
  for (let i = 0; i < edges.length; i++) {
    const keys = input.edgeToDomainKeys.get(edges[i].id);
    if (keys === undefined) {
      continue;
    }
    for (let j = 0; j < keys.length; j++) {
      sharedKeys.push(keys[j]);
    }
  }
  const uniqueSharedKeys = sortUnique(sharedKeys);

  const sharedFailureDomains: string[] = [];
  const unresolvedAssumptions: string[] = [];
  for (let i = 0; i < uniqueSharedKeys.length; i++) {
    const confirmedId = input.confirmedDomains.get(uniqueSharedKeys[i]);
    if (confirmedId !== undefined) {
      sharedFailureDomains.push(confirmedId);
    }
    const suspectedId = input.suspectedDomains.get(uniqueSharedKeys[i]);
    if (suspectedId !== undefined) {
      unresolvedAssumptions.push(suspectedId);
    }
  }

  // independentPathCount：在 confirmed 故障域上去重后的有效路径数。
  // 每个 confirmed 故障域内的多余路径只算 1 条（same phone 2 条 → 1；不同设备 → 2）。
  const domainCounts = new Map<string, number>();
  for (let i = 0; i < edges.length; i++) {
    const keys = input.edgeToDomainKeys.get(edges[i].id);
    if (keys === undefined) {
      continue;
    }
    for (let j = 0; j < keys.length; j++) {
      if (input.confirmedDomains.has(keys[j])) {
        const prev = domainCounts.get(keys[j]);
        domainCounts.set(keys[j], (prev === undefined ? 0 : prev) + 1);
      }
    }
  }
  let redundancy = 0;
  for (const count of domainCounts.values()) {
    if (count > 1) {
      redundancy += count - 1;
    }
  }
  const independentPathCount = Math.max(0, pathCount - redundancy);

  const result = new PathIndependenceResult();
  result.targetNodeId = input.targetNodeId;
  result.capability = input.capability;
  result.pathCount = pathCount;
  result.independentPathCount = independentPathCount;
  result.sharedFailureDomains = sortUnique(sharedFailureDomains);
  result.unresolvedAssumptions = sortUnique(unresolvedAssumptions);
  return result;
}

/**
 * 构建 `edgeToDomainKeys`：把「故障域 subject → 边」的映射变成边 → 故障域 key 的反向索引。
 * subject 由调用方按节点 kind/subtype 推导提供。
 */
export function buildEdgeToDomainIndex(
  edges: DomainEdgeRef[],
  subjectToEdges: Map<string, string[]>,
): Map<string, string[]> {
  const index = new Map<string, string[]>();
  for (const subset of subjectToEdges) {
    const subject = subset[0];
    const edgeIds = subset[1];
    for (let j = 0; j < edgeIds.length; j++) {
      const existing = index.get(edgeIds[j]);
      if (existing === undefined) {
        const list: string[] = [];
        list.push(subject);
        index.set(edgeIds[j], list);
      } else {
        existing.push(subject);
      }
    }
  }
  return index;
}