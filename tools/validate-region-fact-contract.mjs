#!/usr/bin/env node
import { readFileSync } from 'node:fs'
import { resolve, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'

const HERE = dirname(fileURLToPath(import.meta.url))
const ROOT = resolve(HERE, '..')
const DOMAIN = JSON.parse(readFileSync(resolve(ROOT, 'spec/domain/domain.json'), 'utf8'))
const V4 = JSON.parse(readFileSync(resolve(ROOT, 'spec/schema/logical-schema-v4.json'), 'utf8'))

function fail(message) {
  console.error('REGION_FACT_CONTRACT=FAIL')
  console.error(message)
  process.exit(1)
}

const expectedFacets = [
  'issuance_jurisdiction',
  'numbering_territory',
  'provider_jurisdiction',
  'service_market',
  'physical_location',
  'user_confirmed_context',
]
const expectedStates = ['confirmed', 'retired']

if (JSON.stringify(DOMAIN.enums?.RegionFacet?.values) !== JSON.stringify(expectedFacets)) {
  fail('RegionFacet vocabulary drifted')
}
if (JSON.stringify(DOMAIN.enums?.RegionFactState?.values) !== JSON.stringify(expectedStates)) {
  fail('RegionFactState must be exactly confirmed,retired; proposal is not confirmed Node Reality')
}

const codes = DOMAIN.constants?.iso3166Alpha2TerritoryCodes
if (!Array.isArray(codes) || codes.length !== 249) {
  fail('iso3166Alpha2TerritoryCodes must contain exactly 249 alpha-2 codes')
}
if (new Set(codes).size !== codes.length) fail('duplicate territory code')
for (const code of codes) {
  if (!/^[A-Z]{2}$/.test(code)) fail('invalid alpha-2 code: ' + code)
}
for (const required of ['CN', 'HK', 'MO', 'GB', 'US', 'SG']) {
  if (!codes.includes(required)) fail('missing required territory code: ' + required)
}

const contract = DOMAIN.entities?.Node?.structuredFieldContracts?.all_nodes?.region_facts
if (!contract) fail('Node fields.region_facts contract is missing')
if (contract.physicalStorage !== 'nodes.fields_json.region_facts') {
  fail('unexpected RegionFact physical storage: ' + contract.physicalStorage)
}
if (contract.containerVersion !== 1) fail('region_facts container version must be 1')
if (contract.facetType !== 'RegionFacet') fail('region_facts must use RegionFacet')
if (contract.stateType !== 'RegionFactState') fail('region_facts must use RegionFactState')
if (contract.verificationBasisType !== 'VerificationBasisType') {
  fail('region_facts must reuse VerificationBasisType')
}
if (contract.territoryCodeSet !== 'constants.iso3166Alpha2TerritoryCodes') {
  fail('region_facts territory code set must be canonical')
}
for (const field of [
  'id',
  'facet',
  'territory_code',
  'state',
  'verification_basis_type',
  'confirmed_at',
]) {
  if (!(contract.requiredItemFields ?? []).includes(field)) {
    fail('missing RegionFact required field: ' + field)
  }
}
if ((contract.currentLensStates ?? []).join(',') !== 'confirmed') {
  fail('only confirmed RegionFacts may participate in current Region Lens')
}
if (!String(contract.proposalPolicy ?? '').includes('MUST NOT')) {
  fail('Region proposal/Reality separation is not explicit')
}

const forbidden = contract.forbiddenInference ?? []
for (const rule of [
  'currency',
  'provider_or_issuer_name',
  'object_display_name',
  'app_locale',
  'device_locale',
  'timezone',
  'ip_geolocation',
  'vpn_endpoint',
  'current_device_location',
  'presentation_centroid',
]) {
  if (!forbidden.includes(rule)) fail('missing forbidden region inference: ' + rule)
}

const nodeTable = V4.tables?.find((t) => t.name === 'nodes')
if (!nodeTable) fail('logical-schema-v4 nodes table missing')
const v4 = nodeTable.governedStructuredFields?.region_facts
if (!v4) fail('logical-schema-v4 governed region_facts missing')
if (v4.storagePath !== 'fields_json.region_facts') fail('logical storage drift')
if (v4.containerVersion !== 1) fail('logical region_facts version drift')
if (JSON.stringify(v4.item?.facetEnum) !== JSON.stringify(expectedFacets)) {
  fail('logical RegionFacet vocabulary drift')
}
if (JSON.stringify(v4.item?.stateEnum) !== JSON.stringify(expectedStates)) {
  fail('logical RegionFactState vocabulary drift')
}
if (v4.item?.territoryCodeAuthority !== 'spec/domain/domain.json constants.iso3166Alpha2TerritoryCodes') {
  fail('logical territory authority drift')
}
if (!String(v4.item?.proposalPolicy ?? '').includes('MUST NOT')) {
  fail('logical schema must keep proposals outside RegionFact Reality')
}
if (!String(v4.currentLens ?? '').includes('state=confirmed')) {
  fail('logical current Region Lens does not explicitly require confirmed state')
}

// R39 is an additive governed structured-field contract over the existing
// cross-platform Node.fields envelope. A version bump must be a separate explicit
// migration decision; it must never happen incidentally in this contract change.
if (DOMAIN.appSchemaVersion !== 3 || DOMAIN.graphPayload?.version !== 3) {
  fail('R39 RegionFact registration must not silently bump schema/payload version')
}

console.log(
  `REGION_FACT_CONTRACT=PASS facets=${expectedFacets.length} states=${expectedStates.join(',')} territories=${codes.length} storage=${v4.storagePath}`,
)
