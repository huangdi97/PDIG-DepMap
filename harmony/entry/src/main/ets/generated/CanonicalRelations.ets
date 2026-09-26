// DO NOT EDIT
// Generated from canonical PDIG spec (spec/domain/domain.json)
// specVersion: 1.0.0 | appSchemaVersion: 3
// Generator: tools/codegen/generate.mjs — run `node tools/codegen/generate.mjs`

import { Capability, Criticality, GroupMode, NodeKind, Relation } from './CanonicalEnums';

/** single relation definition — 字段集合对齐 Android 冻结版。 */
export class CanonicalRelationDefinition {
  id: Relation;
  fromKinds: NodeKind[];
  toKinds: NodeKind[];
  capability: Capability;
  allowsGroup: boolean;
  allowedGroupModes: GroupMode[];
  defaultCriticality: Criticality;

  constructor(
    id: Relation,
    fromKinds: NodeKind[],
    toKinds: NodeKind[],
    capability: Capability,
    allowsGroup: boolean,
    allowedGroupModes: GroupMode[],
    defaultCriticality: Criticality,
  ) {
    this.id = id;
    this.fromKinds = fromKinds;
    this.toKinds = toKinds;
    this.capability = capability;
    this.allowsGroup = allowsGroup;
    this.allowedGroupModes = allowedGroupModes;
    this.defaultCriticality = defaultCriticality;
  }
}

/** spec.relations 的 runtime 注册表（futureRelations **不在**其中）。 */
export const CANONICAL_RELATION_DEFINITIONS: CanonicalRelationDefinition[] = [
  new CanonicalRelationDefinition(
    Relation.FUNDING_SOURCE,
    [NodeKind.PAYMENT_INSTRUMENT, NodeKind.ACCOUNT, NodeKind.SERVICE, NodeKind.MEMBERSHIP],
    [NodeKind.ACCOUNT, NodeKind.PAYMENT_INSTRUMENT],
    Capability.PAYMENT,
    true,
    [GroupMode.ANY],
    Criticality.UNKNOWN,
  ),
  new CanonicalRelationDefinition(
    Relation.MERCHANT_AGREEMENT,
    [NodeKind.ACCOUNT, NodeKind.PAYMENT_INSTRUMENT],
    [NodeKind.SERVICE, NodeKind.MEMBERSHIP, NodeKind.ACCOUNT],
    Capability.PAYMENT,
    false,
    [],
    Criticality.UNKNOWN,
  ),
  new CanonicalRelationDefinition(
    Relation.RECOVERS,
    [NodeKind.IDENTITY_ANCHOR, NodeKind.ACCOUNT, NodeKind.DEVICE, NodeKind.SERVICE],
    [NodeKind.ACCOUNT, NodeKind.SERVICE, NodeKind.IDENTITY_ANCHOR],
    Capability.RECOVERY,
    true,
    [GroupMode.ANY],
    Criticality.UNKNOWN,
  ),
  new CanonicalRelationDefinition(
    Relation.AUTHENTICATES,
    [NodeKind.IDENTITY_ANCHOR, NodeKind.ACCOUNT, NodeKind.DEVICE],
    [NodeKind.ACCOUNT, NodeKind.SERVICE],
    Capability.AUTHENTICATION,
    true,
    [GroupMode.ANY],
    Criticality.UNKNOWN,
  ),
  new CanonicalRelationDefinition(
    Relation.CONTROLS,
    [NodeKind.ACCOUNT, NodeKind.DEVICE],
    [NodeKind.ACCOUNT, NodeKind.SERVICE, NodeKind.IDENTITY_ANCHOR],
    Capability.ACCESS,
    false,
    [],
    Criticality.UNKNOWN,
  )
];
