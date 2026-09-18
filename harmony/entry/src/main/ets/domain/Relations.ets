// DO NOT EDIT — 语义必须与 Android 冻结基准一致
// 端口自 android/core/src/main/kotlin/com/pdig/core/domain/Relations.kt
// 冻结依据：ANDROID_NATIVE_CORE_FREEZE.md @ HEAD 6053f3c

import { Capability, Criticality, GroupMode, NodeKind, Relation } from '../generated/CanonicalEnums';

/**
 * RelationDefinitionRegistry —— relation 的治理定义。
 *
 * MVP runtime 只正式支持 funding_source / merchant_agreement。
 * future / legacy 词表（verifies / recovers / bound_to 等）**不进入 runtime validation**，
 * 即便 DB 层允许它们存在。
 *
 * 这是一条真实的历史缺陷防线：legacy UI 的 DECLARABLE_RELATIONS 曾暴露 `bound_to`，
 * 而 registry 并不承认它 —— 用户选了会被后端拒绝（见 LEGACY_BEHAVIOR_CORRECTIONS.md LC-003）。
 * Harmony 端不重演。
 */
export interface RelationDefinition {
  id: Relation;
  fromKinds: NodeKind[];
  toKinds: NodeKind[];
  capability: Capability;
  allowsGroup: boolean;
  allowedGroupModes: GroupMode[];
  defaultCriticality: Criticality;
}

const PAYMENT_KINDS: NodeKind[] = [
  NodeKind.PAYMENT_INSTRUMENT,
  NodeKind.ACCOUNT,
  NodeKind.SERVICE,
  NodeKind.MEMBERSHIP,
];

export const RELATION_DEFINITIONS: RelationDefinition[] = [
  {
    id: Relation.FUNDING_SOURCE,
    fromKinds: PAYMENT_KINDS,
    toKinds: [NodeKind.ACCOUNT, NodeKind.PAYMENT_INSTRUMENT],
    capability: Capability.PAYMENT,
    allowsGroup: true,
    allowedGroupModes: [GroupMode.ANY],
    defaultCriticality: Criticality.UNKNOWN,
  },
  {
    id: Relation.MERCHANT_AGREEMENT,
    fromKinds: [NodeKind.ACCOUNT, NodeKind.PAYMENT_INSTRUMENT],
    toKinds: [NodeKind.SERVICE, NodeKind.MEMBERSHIP, NodeKind.ACCOUNT],
    capability: Capability.PAYMENT,
    allowsGroup: false,
    allowedGroupModes: [],
    defaultCriticality: Criticality.UNKNOWN,
  },
];

export function getRelationDefinition(id: string): RelationDefinition | undefined {
  for (let i = 0; i < RELATION_DEFINITIONS.length; i++) {
    if (RELATION_DEFINITIONS[i].id === id) {
      return RELATION_DEFINITIONS[i];
    }
  }
  return undefined;
}

export function listRuntimeRelationIds(): string[] {
  const out: string[] = [];
  for (let i = 0; i < RELATION_DEFINITIONS.length; i++) {
    out.push(RELATION_DEFINITIONS[i].id);
  }
  return out;
}

export interface RelationValidationResult {
  ok: boolean;
  reason: string;
}

function reject(reason: string): RelationValidationResult {
  return { ok: false, reason: reason };
}

function allow(): RelationValidationResult {
  return { ok: true, reason: '' };
}

function containsWire(list: string[], value: string): boolean {
  for (let i = 0; i < list.length; i++) {
    if (list[i] === value) {
      return true;
    }
  }
  return false;
}

function nodeKindWires(list: NodeKind[]): string[] {
  const out: string[] = [];
  for (let i = 0; i < list.length; i++) {
    out.push(list[i]);
  }
  return out;
}

function groupModeWires(list: GroupMode[]): string[] {
  const out: string[] = [];
  for (let i = 0; i < list.length; i++) {
    out.push(list[i]);
  }
  return out;
}

/** 校验一次 relation 使用（fromKind / relation / toKind / capability）。 */
export function validateRelationUse(
  fromKind: string | null,
  relation: string,
  toKind: string | null,
  capability: string,
): RelationValidationResult {
  const def: RelationDefinition | undefined = getRelationDefinition(relation);
  if (def === undefined) {
    return reject(`relation '${relation}' is not in the runtime registry`);
  }
  if (capability !== def.capability) {
    return reject(`relation '${relation}' requires capability '${def.capability}', got '${capability}'`);
  }
  if (fromKind !== null && fromKind !== '' && !containsWire(nodeKindWires(def.fromKinds), fromKind)) {
    return reject(`relation '${relation}' does not allow fromKind '${fromKind}'`);
  }
  if (toKind !== null && toKind !== '' && !containsWire(nodeKindWires(def.toKinds), toKind)) {
    return reject(`relation '${relation}' does not allow toKind '${toKind}'`);
  }
  return allow();
}

/** 校验 Group 使用（relation 是否允许 Group 及模式）。 */
export function validateRelationGroupUse(relation: string, mode: string): RelationValidationResult {
  const def: RelationDefinition | undefined = getRelationDefinition(relation);
  if (def === undefined) {
    return reject(`relation '${relation}' is not in the runtime registry`);
  }
  if (!def.allowsGroup) {
    return reject(`relation '${relation}' does not allow groups`);
  }
  if (!containsWire(groupModeWires(def.allowedGroupModes), mode)) {
    return reject(`relation '${relation}' does not allow group mode '${mode}'`);
  }
  return allow();
}

/**
 * 把校验结果规约为 conformance 期望的形状（JSON 字符串）。
 * fixture 的 expected 结构：
 *   ok=true  → { "ok": true }
 *   ok=false → { "ok": false, "reason": "..." }
 * 键序固定为 ok → reason，与 Android / Core 侧输出一致，便于逐字节比对。
 */
export function normalizeRelationResult(r: RelationValidationResult): string {
  if (r.ok) {
    return '{"ok":true}';
  }
  return `{"ok":false,"reason":${JSON.stringify(r.reason)}}`;
}
