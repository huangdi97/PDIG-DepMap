import {
  CanonicalSpec,
  RegionFacet,
  RegionFactState,
  VerificationBasisType,
  regionFacetFromWire,
  regionFactStateFromWire,
  verificationBasisTypeFromWire,
} from '../generated/CanonicalEnums';

/**
 * R39 governed RegionFact decoder.
 *
 * It consumes only fields_json.region_facts. Currency, provider/name strings,
 * phone prefixes, locale, timezone, IP and presentation coordinates never create
 * geographic Reality here.
 */
export class ConfirmedRegionFact {
  id: string = '';
  facet: RegionFacet = RegionFacet.USER_CONFIRMED_CONTEXT;
  territoryCode: string = '';
  subdivisionCode?: string;
  state: RegionFactState = RegionFactState.CONFIRMED;
  verificationBasisType: VerificationBasisType = VerificationBasisType.USER_CONFIRMED;
  confirmedAt: string = '';
  evidenceRefs: string[] = [];
  validFrom?: string;
  validUntil?: string;
  retiredAt?: string;
}

export enum RegionLensSelectionStatus {
  SELECTED = 'selected',
  NEEDS_REVIEW = 'needs_review',
  UNKNOWN = 'unknown',
}

export class RegionLensSelection {
  status: RegionLensSelectionStatus = RegionLensSelectionStatus.UNKNOWN;
  territoryCode?: string;
  facet?: RegionFacet;
}

export function governedRegionFacts(fieldsJson: string): ConfirmedRegionFact[] {
  let parsed: Object;
  try {
    parsed = JSON.parse(fieldsJson) as Object;
  } catch (e) {
    return [];
  }
  if (parsed === null || typeof parsed !== 'object' || parsed instanceof Array) {
    return [];
  }

  const root = parsed as Record<string, Object>;
  const rawContainer: Object = root['region_facts'];
  if (
    rawContainer === undefined ||
    rawContainer === null ||
    typeof rawContainer !== 'object' ||
    rawContainer instanceof Array
  ) {
    return [];
  }
  const container = rawContainer as Record<string, Object>;
  const version: Object = container['version'];
  if (typeof version !== 'number' || (version as number) !== 1) {
    return [];
  }

  const rawItems: Object = container['items'];
  if (!(rawItems instanceof Array)) {
    return [];
  }

  const out: ConfirmedRegionFact[] = [];
  for (const rawItem of rawItems as Array<Object>) {
    if (
      rawItem === null ||
      typeof rawItem !== 'object' ||
      rawItem instanceof Array
    ) {
      continue;
    }
    const decoded = decodeRegionFact(rawItem as Record<string, Object>);
    if (decoded !== undefined) {
      out.push(decoded);
    }
  }
  return out;
}

export function currentConfirmedRegionFacts(fieldsJson: string): ConfirmedRegionFact[] {
  const all = governedRegionFacts(fieldsJson);
  const out: ConfirmedRegionFact[] = [];
  for (const fact of all) {
    if (fact.state === RegionFactState.CONFIRMED) {
      out.push(fact);
    }
  }
  return out;
}

export function defaultRegionLensSelection(fieldsJson: string): RegionLensSelection {
  const facts = currentConfirmedRegionFacts(fieldsJson);
  const precedence: RegionFacet[] = [
    RegionFacet.NUMBERING_TERRITORY,
    RegionFacet.ISSUANCE_JURISDICTION,
    RegionFacet.SERVICE_MARKET,
    RegionFacet.PROVIDER_JURISDICTION,
    RegionFacet.USER_CONFIRMED_CONTEXT,
  ];

  for (const facet of precedence) {
    const territories: string[] = [];
    for (const fact of facts) {
      if (fact.facet !== facet) {
        continue;
      }
      if (territories.indexOf(fact.territoryCode) < 0) {
        territories.push(fact.territoryCode);
      }
    }
    territories.sort();
    if (territories.length === 0) {
      continue;
    }
    const result = new RegionLensSelection();
    result.facet = facet;
    if (territories.length === 1) {
      result.status = RegionLensSelectionStatus.SELECTED;
      result.territoryCode = territories[0];
    } else {
      result.status = RegionLensSelectionStatus.NEEDS_REVIEW;
    }
    return result;
  }

  return new RegionLensSelection();
}

function decodeRegionFact(
  item: Record<string, Object>,
): ConfirmedRegionFact | undefined {
  const id = requiredText(item, 'id');
  if (
    id === undefined ||
    id.length > 160 ||
    containsControl(id)
  ) {
    return undefined;
  }

  const rawFacet: Object = item['facet'];
  if (typeof rawFacet !== 'string') {
    return undefined;
  }
  const facet = regionFacetFromWire(rawFacet as string);
  if (facet === undefined) {
    return undefined;
  }

  const territory = requiredText(item, 'territory_code');
  if (
    territory === undefined ||
    territory !== territory.toUpperCase() ||
    CanonicalSpec.ISO_3166_ALPHA2_TERRITORY_CODES.indexOf(territory) < 0
  ) {
    return undefined;
  }

  let subdivision: string | undefined = undefined;
  const rawSubdivision: Object = item['subdivision_code'];
  if (rawSubdivision !== undefined) {
    subdivision = requiredText(item, 'subdivision_code');
    if (
      subdivision === undefined ||
      !validSubdivision(territory, subdivision)
    ) {
      return undefined;
    }
  }

  const rawState: Object = item['state'];
  if (typeof rawState !== 'string') {
    return undefined;
  }
  const state = regionFactStateFromWire(rawState as string);
  if (state === undefined) {
    return undefined;
  }

  const rawBasis: Object = item['verification_basis_type'];
  if (typeof rawBasis !== 'string') {
    return undefined;
  }
  const basis = verificationBasisTypeFromWire(rawBasis as string);
  if (basis === undefined) {
    return undefined;
  }

  const confirmedAt = requiredText(item, 'confirmed_at');
  if (confirmedAt === undefined || containsControl(confirmedAt)) {
    return undefined;
  }

  const evidenceRefs = stringArray(item, 'evidence_refs');
  if (evidenceRefs === undefined) {
    return undefined;
  }
  for (const ref of evidenceRefs) {
    if (ref.length > 512 || containsControl(ref)) {
      return undefined;
    }
  }

  const validFrom = strictOptionalText(item, 'valid_from');
  if (validFrom.invalid) {
    return undefined;
  }
  const validUntil = strictOptionalText(item, 'valid_until');
  if (validUntil.invalid) {
    return undefined;
  }
  const retiredAt = strictOptionalText(item, 'retired_at');
  if (retiredAt.invalid) {
    return undefined;
  }

  const out = new ConfirmedRegionFact();
  out.id = id;
  out.facet = facet;
  out.territoryCode = territory;
  out.subdivisionCode = subdivision;
  out.state = state;
  out.verificationBasisType = basis;
  out.confirmedAt = confirmedAt;
  out.evidenceRefs = evidenceRefs;
  out.validFrom = validFrom.value;
  out.validUntil = validUntil.value;
  out.retiredAt = retiredAt.value;
  return out;
}

class OptionalTextResult {
  invalid: boolean = false;
  value?: string;
}

function strictOptionalText(
  obj: Record<string, Object>,
  key: string,
): OptionalTextResult {
  const result = new OptionalTextResult();
  const raw: Object = obj[key];
  if (raw === undefined) {
    return result;
  }
  if (typeof raw !== 'string') {
    result.invalid = true;
    return result;
  }
  const value = (raw as string).trim();
  if (value.length === 0 || containsControl(value)) {
    result.invalid = true;
    return result;
  }
  result.value = value;
  return result;
}

function requiredText(
  obj: Record<string, Object>,
  key: string,
): string | undefined {
  const raw: Object = obj[key];
  if (typeof raw !== 'string') {
    return undefined;
  }
  const value = (raw as string).trim();
  return value.length > 0 ? value : undefined;
}

function stringArray(
  obj: Record<string, Object>,
  key: string,
): string[] | undefined {
  const raw: Object = obj[key];
  if (raw === undefined) {
    return [];
  }
  if (!(raw instanceof Array)) {
    return undefined;
  }
  const out: string[] = [];
  for (const item of raw as Array<Object>) {
    if (typeof item !== 'string') {
      return undefined;
    }
    out.push(item as string);
  }
  return out;
}

function validSubdivision(territory: string, value: string): boolean {
  const prefix = territory + '-';
  if (!value.startsWith(prefix)) {
    return false;
  }
  const suffix = value.substring(prefix.length);
  if (suffix.length < 1 || suffix.length > 3) {
    return false;
  }
  for (let i = 0; i < suffix.length; i++) {
    const code = suffix.charCodeAt(i);
    const upperAlpha = code >= 65 && code <= 90;
    const digit = code >= 48 && code <= 57;
    if (!upperAlpha && !digit) {
      return false;
    }
  }
  return true;
}

function containsControl(value: string): boolean {
  for (let i = 0; i < value.length; i++) {
    const code = value.charCodeAt(i);
    if (code < 0x20 || code === 0x7F) {
      return true;
    }
  }
  return false;
}
