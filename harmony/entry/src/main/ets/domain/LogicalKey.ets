// DO NOT EDIT 之外的语义 — 语义必须与 TS 冻结基准一致
// 端口自 core/src/domain/types.ts（dependencyLogicalKey / canonicalGroupKey /
// groupProposalKey）与 core/src/services/confirmation-service.ts 中的解析校验。
//
// 为什么单独成模块（而不是留在 Entities 里）：
//   logical key 是**跨端存储与比较的外部契约**（DB UNIQUE 约束、proposal 去重、
//   group 成员匹配都基于它的字符串形态）。把它集中在一处，
//   可以让「生成本模块的 key」与「解析本模块的 key」始终成对出现 ——
//   一旦序列化格式变更，round-trip 检查会立刻失败，而不是等到两端数据对不上。
//
// 分层约束：只依赖 `./Entities` 与 `../generated/CanonicalEnums`。

import { Capability, GroupMode, Relation } from '../generated/CanonicalEnums';
import { DependencyLogicalKeyParts, canonicalGroupKey, dependencyLogicalKey, groupProposalKey } from './Entities';
import { capabilityFromWire, relationFromWire } from './CanonicalWire';
import { getRelationDefinition } from './Relations';

/**
 * 一次 logical key 解析的结果。
 *
 * 设计取舍：用 `{ ok, parts }` 而不是「返回 undefined 或抛异常」。
 * 理由：解析发生在数据边界（DB 行、导入的 proposal、用户确认的成员列表），
 * 调用方**必须**显式处理失败；抛异常容易被 catch 成"没这条边"而静默丢数据。
 */
export class LogicalKeyParseResult {
  ok: boolean = false;
  /** 解析失败时给出可读原因；供错误码映射与日志。 */
  reason: string = '';
  from: string = '';
  relation: string = '';
  to: string = '';
  capability: string = '';
}

/**
 * 解析 `from|relation|to|capability`。
 *
 * 与 TS 基准同一套 fail-closed 规则（confirmation-service.ts）：
 *   1. 分段数必须恰好为 4（!= 4 → 拒绝）
 *   2. 任一分段为空 → 拒绝
 *   3. relation / capability 必须能被 canonical 词表识别 → 否则拒绝
 *
 * 第 3 条是本端**额外**的收紧（TS 侧把 relation 交给 registry 校验）。
 * 保留它是因为：解析出 'bound_to' 这类 legacy 词表会让下游
 * RelationRegistry 拒绝，错误发生在更远处、更难定位。
 * relate：LEGACY_BEHAVIOR_CORRECTIONS.md LC-003。
 */
export function parseDependencyLogicalKey(key: string): LogicalKeyParseResult {
  const r = new LogicalKeyParseResult();
  const parts: string[] = key.split('|');
  if (parts.length !== 4) {
    r.reason = `malformed member dependency key: ${key}`;
    return r;
  }
  const from = parts[0];
  const relation = parts[1];
  const to = parts[2];
  const capability = parts[3];
  if (from.length === 0 || relation.length === 0 || to.length === 0 || capability.length === 0) {
    r.reason = `malformed member dependency key: ${key}`;
    return r;
  }
  if (relationFromWire(relation) === undefined) {
    r.reason = `unknown relation in logical key: ${relation}`;
    return r;
  }
  // ⚠ 2026-09-18 修正：这里原先只做 `relationFromWire`（canonical 枚举成员）判定，
  // 但上面的注释明确写着第 3 条要求是"拒绝 runtime registry 不承认的 relation"
  // （LC-003：registry 锁定为 funding_source / merchant_agreement 两值）。
  // 枚举里存在 `bound_to`，于是 `bound_to` 会被**放行** —— 实现与自己的契约不符，
  // 下游 registry 才拒绝，错误被推得更远、更难定位。
  // 现改为按 runtime registry 判定，与注释所述意图一致。
  //
  // 注意：这是本端**比 TS 基准更严**的一处（TS 侧把 relation 交给 registry 校验）。
  // 该收紧是刻意保留并记录在案的，不是疏漏 —— 但它是**跨端行为差异**，
  // 已记入 HARMONY_N3_CONFORMANCE_REPORT.md 的待复核项。
  if (getRelationDefinition(relation) === undefined) {
    r.reason = `relation not in runtime registry: ${relation}`;
    return r;
  }
  if (capabilityFromWire(capability) === undefined) {
    r.reason = `unknown capability in logical key: ${capability}`;
    return r;
  }
  r.ok = true;
  r.from = from;
  r.relation = relation;
  r.to = to;
  r.capability = capability;
  return r;
}

/** 由已解析分段重新生成 key（round-trip 的一半）。 */
export function formatDependencyLogicalKey(p: DependencyLogicalKeyParts): string {
  return dependencyLogicalKey(p.from, p.relation, p.to, p.capability);
}

/**
 * group proposal key：`group|<target|capability|mode|sorted(dedup(members...))>`
 * 与 core/src/domain/types.ts 的 groupProposalKey 同口径。
 */
export function formatGroupProposalKey(
  targetNodeId: string,
  capability: Capability,
  mode: GroupMode,
  memberLogicalKeys: string[],
): string {
  return groupProposalKey(targetNodeId, capability, mode, memberLogicalKeys);
}

// ---------------------------------------------------------------------------
// round-trip 与稳定性自检
// ---------------------------------------------------------------------------

/** round-trip：format → parse → format 必须与原串逐字节相同。 */
export function logicalKeyRoundTrips(
  from: string,
  relation: Relation,
  to: string,
  capability: Capability,
): boolean {
  const formatted = dependencyLogicalKey(from, relation, to, capability);
  const parsed = parseDependencyLogicalKey(formatted);
  if (!parsed.ok) {
    return false;
  }
  const again = dependencyLogicalKey(
    parsed.from,
    relationFromWire(parsed.relation) as Relation,
    parsed.to,
    capabilityFromWire(parsed.capability) as Capability,
  );
  return again === formatted;
}

/**
 * canonicalGroupKey 的成员顺序无关性：打乱成员顺序必须得到同一 key。
 * 这是 spec §4 的明确要求，也是跨端数据能对上的前提。
 */
export function groupKeyIsOrderIndependent(
  targetNodeId: string,
  capability: Capability,
  mode: GroupMode,
  members: string[],
): boolean {
  const a = canonicalGroupKey(targetNodeId, capability, mode, members);
  const shuffled = members.slice().reverse();
  const b = canonicalGroupKey(targetNodeId, capability, mode, shuffled);
  return a === b;
}

/** canonicalGroupKey 的成员去重性：重复成员不得改变 key。 */
export function groupKeyDedupsMembers(
  targetNodeId: string,
  capability: Capability,
  mode: GroupMode,
  members: string[],
): boolean {
  const a = canonicalGroupKey(targetNodeId, capability, mode, members);
  const withDup = members.concat(members);
  const b = canonicalGroupKey(targetNodeId, capability, mode, withDup);
  return a === b;
}
