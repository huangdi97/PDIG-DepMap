import {
  IdentityAnchorSubtype,
  NodeKind,
  VerificationBasisType,
  identityAnchorSubtypeFromWire,
  verificationBasisTypeFromWire,
} from '../generated/CanonicalEnums';

/**
 * Canonical R37 identity-anchor profile decoder.
 *
 * A bare fields_json.subtype key is deliberately ignored. Only the governed
 * identity_anchor_profile with explicit confirmation provenance can activate a
 * typed identity surface.
 */
export class ConfirmedIdentityAnchorProfile {
  subtype: IdentityAnchorSubtype = IdentityAnchorSubtype.OTHER_IDENTITY;
  verificationBasisType: VerificationBasisType = VerificationBasisType.USER_CONFIRMED;
  confirmedAt: string = '';
  evidenceRefs: string[] = [];
}

export function confirmedIdentityAnchorProfile(
  kind: NodeKind,
  fieldsJson: string,
): ConfirmedIdentityAnchorProfile | undefined {
  if (kind !== NodeKind.IDENTITY_ANCHOR) {
    return undefined;
  }

  let parsed: Object;
  try {
    parsed = JSON.parse(fieldsJson) as Object;
  } catch (e) {
    return undefined;
  }
  if (parsed === null || typeof parsed !== 'object' || parsed instanceof Array) {
    return undefined;
  }

  const root = parsed as Record<string, Object>;
  const rawProfile: Object = root['identity_anchor_profile'];
  if (
    rawProfile === undefined ||
    rawProfile === null ||
    typeof rawProfile !== 'object' ||
    rawProfile instanceof Array
  ) {
    return undefined;
  }
  const profile = rawProfile as Record<string, Object>;

  const version: Object = profile['version'];
  if (typeof version !== 'number' || (version as number) !== 1) {
    return undefined;
  }

  const subtypeRaw: Object = profile['subtype'];
  if (typeof subtypeRaw !== 'string') {
    return undefined;
  }
  const subtype = identityAnchorSubtypeFromWire(subtypeRaw as string);
  if (subtype === undefined) {
    return undefined;
  }

  const basisRaw: Object = profile['verification_basis_type'];
  if (typeof basisRaw !== 'string') {
    return undefined;
  }
  const basis = verificationBasisTypeFromWire(basisRaw as string);
  if (basis === undefined) {
    return undefined;
  }

  const confirmedRaw: Object = profile['confirmed_at'];
  if (typeof confirmedRaw !== 'string') {
    return undefined;
  }
  const confirmedAt = (confirmedRaw as string).trim();
  if (confirmedAt.length === 0) {
    return undefined;
  }

  const refs: string[] = [];
  const rawRefs: Object = profile['evidence_refs'];
  if (rawRefs !== undefined) {
    if (!(rawRefs instanceof Array)) {
      return undefined;
    }
    for (const item of rawRefs as Array<Object>) {
      if (typeof item !== 'string') {
        return undefined;
      }
      refs.push(item as string);
    }
  }

  const out = new ConfirmedIdentityAnchorProfile();
  out.subtype = subtype;
  out.verificationBasisType = basis;
  out.confirmedAt = confirmedAt;
  out.evidenceRefs = refs;
  return out;
}
