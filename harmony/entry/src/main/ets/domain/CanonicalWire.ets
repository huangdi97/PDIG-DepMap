// Domain 层 wire 解析辅助 —— 手写，**不属于** codegen 产物。
//
// 为什么需要这个模块：
//   tools/codegen/generate.mjs 的 ArkTS emitter 生成的是**纯 enum**
//   （见 emitArkTS()：只有 `NAME = 'wire'`，没有 companion / fromWire）。
//   Kotlin 与 Swift 的 emitter 会附带 `fromWire(value): T?`，ArkTS 不会。
//   因此 `CanonicalEnums.ets` 是 DO NOT EDIT 的生成物，
//   不能为了补 fromWire 而手改它 —— 那会让 codegen check 漂移。
//
// 设计约束：
//   1. 本模块只依赖 `../generated/CanonicalEnums`，保持 domain 层零平台依赖。
//   2. 解析失败一律返回 undefined，**不猜测、不 fallback 到默认值** ——
//      调用方必须显式决定如何降级（Android 侧在 DB 读取处 `?: 默认值`，
//      这是 data 层决策，不该由 domain 层替他做）。
//   3. 逐 enum 手写比较，而不是反射遍历：ArkTS 对动态属性访问限制严格，
//      且显式列举能让「新增 enum 但忘记加解析」在 review 时可见。

import { Capability, Criticality, DependencyOrigin, DependencyState, GroupMode, GroupState, NodeKind, Relation, RelationRuntime, CapabilityRuntime } from '../generated/CanonicalEnums';

/** 通用：在 wire 值数组中定位下标；未命中返回 -1。 */
function indexOfWire(wires: string[], value: string): number {
  for (let i = 0; i < wires.length; i++) {
    if (wires[i] === value) {
      return i;
    }
  }
  return -1;
}

const NODE_KIND_WIRES: string[] = [
  NodeKind.IDENTITY_ANCHOR,
  NodeKind.PAYMENT_INSTRUMENT,
  NodeKind.ACCOUNT,
  NodeKind.SERVICE,
  NodeKind.MEMBERSHIP,
  NodeKind.DEVICE,
  NodeKind.CUSTOM,
];

const NODE_KIND_VALUES: NodeKind[] = [
  NodeKind.IDENTITY_ANCHOR,
  NodeKind.PAYMENT_INSTRUMENT,
  NodeKind.ACCOUNT,
  NodeKind.SERVICE,
  NodeKind.MEMBERSHIP,
  NodeKind.DEVICE,
  NodeKind.CUSTOM,
];

const RELATION_WIRES: string[] = [
  Relation.FUNDING_SOURCE,
  Relation.MERCHANT_AGREEMENT,
  Relation.VERIFIES,
  Relation.RECOVERS,
  Relation.BOUND_TO,
];

const RELATION_VALUES: Relation[] = [
  Relation.FUNDING_SOURCE,
  Relation.MERCHANT_AGREEMENT,
  Relation.VERIFIES,
  Relation.RECOVERS,
  Relation.BOUND_TO,
];

const CAPABILITY_WIRES: string[] = [
  Capability.PAYMENT,
  Capability.ACCESS,
  Capability.RECOVERY,
  Capability.IDENTITY,
];

const CAPABILITY_VALUES: Capability[] = [
  Capability.PAYMENT,
  Capability.ACCESS,
  Capability.RECOVERY,
  Capability.IDENTITY,
];

const CRITICALITY_WIRES: string[] = [Criticality.REQUIRED, Criticality.UNKNOWN];
const CRITICALITY_VALUES: Criticality[] = [Criticality.REQUIRED, Criticality.UNKNOWN];

const DEPENDENCY_STATE_WIRES: string[] = [DependencyState.ACTIVE, DependencyState.RETIRED];
const DEPENDENCY_STATE_VALUES: DependencyState[] = [DependencyState.ACTIVE, DependencyState.RETIRED];

const DEPENDENCY_ORIGIN_WIRES: string[] = [DependencyOrigin.MANUAL, DependencyOrigin.PROPOSAL];
const DEPENDENCY_ORIGIN_VALUES: DependencyOrigin[] = [DependencyOrigin.MANUAL, DependencyOrigin.PROPOSAL];

const GROUP_MODE_WIRES: string[] = [GroupMode.ANY, GroupMode.ALL];
const GROUP_MODE_VALUES: GroupMode[] = [GroupMode.ANY, GroupMode.ALL];

const GROUP_STATE_WIRES: string[] = [GroupState.ACTIVE, GroupState.RETIRED];
const GROUP_STATE_VALUES: GroupState[] = [GroupState.ACTIVE, GroupState.RETIRED];

export function nodeKindFromWire(value: string): NodeKind | undefined {
  const i = indexOfWire(NODE_KIND_WIRES, value);
  return i < 0 ? undefined : NODE_KIND_VALUES[i];
}

export function relationFromWire(value: string): Relation | undefined {
  const i = indexOfWire(RELATION_WIRES, value);
  return i < 0 ? undefined : RELATION_VALUES[i];
}

export function capabilityFromWire(value: string): Capability | undefined {
  const i = indexOfWire(CAPABILITY_WIRES, value);
  return i < 0 ? undefined : CAPABILITY_VALUES[i];
}

export function criticalityFromWire(value: string): Criticality | undefined {
  const i = indexOfWire(CRITICALITY_WIRES, value);
  return i < 0 ? undefined : CRITICALITY_VALUES[i];
}

export function dependencyStateFromWire(value: string): DependencyState | undefined {
  const i = indexOfWire(DEPENDENCY_STATE_WIRES, value);
  return i < 0 ? undefined : DEPENDENCY_STATE_VALUES[i];
}

export function dependencyOriginFromWire(value: string): DependencyOrigin | undefined {
  const i = indexOfWire(DEPENDENCY_ORIGIN_WIRES, value);
  return i < 0 ? undefined : DEPENDENCY_ORIGIN_VALUES[i];
}

export function groupModeFromWire(value: string): GroupMode | undefined {
  const i = indexOfWire(GROUP_MODE_WIRES, value);
  return i < 0 ? undefined : GROUP_MODE_VALUES[i];
}

export function groupStateFromWire(value: string): GroupState | undefined {
  const i = indexOfWire(GROUP_STATE_WIRES, value);
  return i < 0 ? undefined : GROUP_STATE_VALUES[i];
}

// ---------------------------------------------------------------------------
// 运行时受限词表（RelationRuntime / CapabilityRuntime）
// ---------------------------------------------------------------------------

const RELATION_RUNTIME_WIRES: string[] = [
  RelationRuntime.FUNDING_SOURCE,
  RelationRuntime.MERCHANT_AGREEMENT,
];

const RELATION_RUNTIME_VALUES: RelationRuntime[] = [
  RelationRuntime.FUNDING_SOURCE,
  RelationRuntime.MERCHANT_AGREEMENT,
];

const CAPABILITY_RUNTIME_WIRES: string[] = [CapabilityRuntime.PAYMENT];
const CAPABILITY_RUNTIME_VALUES: CapabilityRuntime[] = [CapabilityRuntime.PAYMENT];

export function relationRuntimeFromWire(value: string): RelationRuntime | undefined {
  const i = indexOfWire(RELATION_RUNTIME_WIRES, value);
  return i < 0 ? undefined : RELATION_RUNTIME_VALUES[i];
}

export function capabilityRuntimeFromWire(value: string): CapabilityRuntime | undefined {
  const i = indexOfWire(CAPABILITY_RUNTIME_WIRES, value);
  return i < 0 ? undefined : CAPABILITY_RUNTIME_VALUES[i];
}
