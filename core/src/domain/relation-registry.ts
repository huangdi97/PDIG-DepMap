import type { Capability, NodeKind } from './types.ts'

/**
 * RelationDefinitionRegistry — 把 relation 从裸字符串升级成治理定义（GOAL §12）。
 *
 * MVP02 runtime 只正式支持 funding_source / merchant_agreement；
 * Future 词表（verifies/recovers/bound_to）不进入 runtime validation。
 * Proposal / Dependency write / import payload 都必须通过 registry validation。
 */

export interface RelationDefinition {
  id: RelationId
  fromKinds: readonly NodeKind[]
  toKinds: readonly NodeKind[]
  capability: Capability
  allowsGroup: boolean
  allowedGroupModes: readonly ('ANY' | 'ALL')[]
  defaultCriticality: 'unknown'
  verificationPolicy: 'user_only' | 'user_or_authoritative'
  impactSemantics: 'dependency'
}

export type RelationId =
  'funding_source' | 'merchant_agreement' | 'recovers' | 'authenticates' | 'controls'

const PAYMENT_KINDS: readonly NodeKind[] = [
  'payment_instrument',
  'account',
  'service',
  'membership',
]

export const RELATION_DEFINITIONS: ReadonlyArray<RelationDefinition> = [
  {
    id: 'funding_source',
    fromKinds: PAYMENT_KINDS,
    toKinds: ['account', 'payment_instrument'],
    capability: 'payment',
    allowsGroup: true,
    allowedGroupModes: ['ANY'],
    defaultCriticality: 'unknown',
    verificationPolicy: 'user_only',
    impactSemantics: 'dependency',
  },
  {
    id: 'merchant_agreement',
    fromKinds: ['account', 'payment_instrument'],
    toKinds: ['service', 'membership', 'account'],
    capability: 'payment',
    allowsGroup: false,
    allowedGroupModes: [],
    defaultCriticality: 'unknown',
    verificationPolicy: 'user_only',
    impactSemantics: 'dependency',
  },
  {
    id: 'recovers',
    fromKinds: ['identity_anchor', 'account', 'device', 'service'],
    toKinds: ['account', 'service', 'identity_anchor'],
    capability: 'recovery',
    allowsGroup: true,
    allowedGroupModes: ['ANY'],
    defaultCriticality: 'unknown',
    verificationPolicy: 'user_only',
    impactSemantics: 'dependency',
  },
  {
    id: 'authenticates',
    fromKinds: ['identity_anchor', 'account', 'device'],
    toKinds: ['account', 'service'],
    capability: 'authentication',
    allowsGroup: true,
    allowedGroupModes: ['ANY'],
    defaultCriticality: 'unknown',
    verificationPolicy: 'user_only',
    impactSemantics: 'dependency',
  },
  {
    id: 'controls',
    fromKinds: ['account', 'device'],
    toKinds: ['account', 'service', 'identity_anchor'],
    capability: 'access',
    allowsGroup: false,
    allowedGroupModes: [],
    defaultCriticality: 'unknown',
    verificationPolicy: 'user_only',
    impactSemantics: 'dependency',
  },
]

const BY_ID = new Map<RelationId, RelationDefinition>(RELATION_DEFINITIONS.map((d) => [d.id, d]))

export function getRelationDefinition(id: string): RelationDefinition | null {
  return (BY_ID as Map<string, RelationDefinition>).get(id) ?? null
}

export function listRuntimeRelationIds(): RelationId[] {
  return [...BY_ID.keys()]
}

export interface RelationValidationResult {
  ok: boolean
  reason?: string
}

/** 校验一次 relation 使用（from kind / relation / to kind / capability）。 */
export function validateRelationUse(
  fromKind: NodeKind | null,
  relation: string,
  toKind: NodeKind | null,
  capability: string,
): RelationValidationResult {
  const def = (BY_ID as Map<string, RelationDefinition>).get(relation)
  if (!def) {
    return {
      ok: false,
      reason: `relation '${relation}' is not in the runtime registry`,
    }
  }
  if (capability !== def.capability) {
    return {
      ok: false,
      reason: `relation '${relation}' requires capability '${def.capability}', got '${capability}'`,
    }
  }
  if (fromKind && !def.fromKinds.includes(fromKind)) {
    return {
      ok: false,
      reason: `relation '${relation}' does not allow fromKind '${fromKind}'`,
    }
  }
  if (toKind && !def.toKinds.includes(toKind)) {
    return {
      ok: false,
      reason: `relation '${relation}' does not allow toKind '${toKind}'`,
    }
  }
  return { ok: true }
}

/** 校验 Group 使用（relation 是否允许 Group 及模式）。 */
export function validateRelationGroupUse(
  relation: string,
  mode: 'ANY' | 'ALL',
): RelationValidationResult {
  const def = (BY_ID as Map<string, RelationDefinition>).get(relation)
  if (!def) {
    return { ok: false, reason: `relation '${relation}' is not in the runtime registry` }
  }
  if (!def.allowsGroup) {
    return { ok: false, reason: `relation '${relation}' does not allow groups` }
  }
  if (!def.allowedGroupModes.includes(mode)) {
    return {
      ok: false,
      reason: `relation '${relation}' does not allow group mode '${mode}'`,
    }
  }
  return { ok: true }
}
