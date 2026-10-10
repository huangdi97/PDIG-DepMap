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
export class ConfirmedIdentityIdentifier {
  value: string = '';
  verificationBasisType: VerificationBasisType = VerificationBasisType.USER_CONFIRMED;
  confirmedAt: string = '';
  evidenceRefs: string[] = [];
}

export class ConfirmedIdentityAnchorProfile {
  subtype: IdentityAnchorSubtype = IdentityAnchorSubtype.OTHER_IDENTITY;
  verificationBasisType: VerificationBasisType = VerificationBasisType.USER_CONFIRMED;
  confirmedAt: string = '';
  evidenceRefs: string[] = [];
  identifier?: ConfirmedIdentityIdentifier;
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
  out.identifier = confirmedIdentityIdentifier(profile, subtype);
  return out;
}

function confirmedIdentityIdentifier(
  profile: Record<string, Object>,
  subtype: IdentityAnchorSubtype,
): ConfirmedIdentityIdentifier | undefined {
  if (
    subtype !== IdentityAnchorSubtype.PHONE_NUMBER &&
    subtype !== IdentityAnchorSubtype.EMAIL_ADDRESS
  ) {
    return undefined;
  }

  const rawIdentifier: Object = profile['identifier'];
  if (
    rawIdentifier === undefined ||
    rawIdentifier === null ||
    typeof rawIdentifier !== 'object' ||
    rawIdentifier instanceof Array
  ) {
    return undefined;
  }
  const identifier = rawIdentifier as Record<string, Object>;

  const rawValue: Object = identifier['value'];
  const rawBasis: Object = identifier['verification_basis_type'];
  const rawConfirmed: Object = identifier['confirmed_at'];
  if (
    typeof rawValue !== 'string' ||
    typeof rawBasis !== 'string' ||
    typeof rawConfirmed !== 'string'
  ) {
    return undefined;
  }

  const value = (rawValue as string).trim();
  const confirmedAt = (rawConfirmed as string).trim();
  if (value.length === 0 || value.length > 320 || confirmedAt.length === 0) {
    return undefined;
  }
  for (let i = 0; i < value.length; i++) {
    const code = value.charCodeAt(i);
    if (code < 0x20 || code === 0x7F) {
      return undefined;
    }
  }

  const basis = verificationBasisTypeFromWire(rawBasis as string);
  if (basis === undefined) {
    return undefined;
  }

  const refs: string[] = [];
  const rawRefs: Object = identifier['evidence_refs'];
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

  const out = new ConfirmedIdentityIdentifier();
  out.value = value;
  out.verificationBasisType = basis;
  out.confirmedAt = confirmedAt;
  out.evidenceRefs = refs;
  return out;
}
