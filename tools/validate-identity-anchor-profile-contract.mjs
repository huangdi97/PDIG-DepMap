#!/usr/bin/env node
import { readFileSync } from 'node:fs'
import { resolve, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'

const HERE = dirname(fileURLToPath(import.meta.url))
const ROOT = resolve(HERE, '..')
const DOMAIN = JSON.parse(readFileSync(resolve(ROOT, 'spec/domain/domain.json'), 'utf8'))
const V4 = JSON.parse(readFileSync(resolve(ROOT, 'spec/schema/logical-schema-v4.json'), 'utf8'))

function fail(message) {
  console.error('IDENTITY_ANCHOR_PROFILE_CONTRACT=FAIL')
  console.error(message)
  process.exit(1)
}

const expectedSubtype = ['phone_number', 'email_address', 'other_identity']
const actualSubtype = DOMAIN.enums?.IdentityAnchorSubtype?.values
if (JSON.stringify(actualSubtype) !== JSON.stringify(expectedSubtype)) {
  fail(`IdentityAnchorSubtype mismatch: ${JSON.stringify(actualSubtype)}`)
}

const domainContract =
  DOMAIN.entities?.Node?.structuredFieldContracts?.identity_anchor?.identity_anchor_profile
if (!domainContract) fail('Node identity_anchor_profile structured field contract is missing')

if (domainContract.physicalStorage !== 'nodes.fields_json.identity_anchor_profile') {
  fail(`Unexpected physical storage: ${domainContract.physicalStorage}`)
}
if (domainContract.profileVersion !== 1) fail('identity_anchor_profile version must be 1')
if (domainContract.subtypeType !== 'IdentityAnchorSubtype') {
  fail('identity_anchor_profile must reference IdentityAnchorSubtype')
}
if (domainContract.verificationBasisType !== 'VerificationBasisType') {
  fail('identity_anchor_profile must reuse VerificationBasisType')
}

const requiredConfirmed = domainContract.requiredConfirmedFields ?? []
for (const field of ['version', 'subtype', 'verification_basis_type', 'confirmed_at']) {
  if (!requiredConfirmed.includes(field)) fail(`Missing confirmed profile field: ${field}`)
}

const forbidden = domainContract.forbiddenInference ?? []
for (const rule of [
  'name_regex',
  'phone_prefix_or_country_code',
  'email_shaped_string',
  'legacy_bare_fields_json_subtype',
  'dependency_relation',
  'scenario_entry',
]) {
  if (!forbidden.includes(rule)) fail(`Missing forbidden subtype inference: ${rule}`)
}

const nodeTable = V4.tables?.find((t) => t.name === 'nodes')
if (!nodeTable) fail('logical-schema-v4 nodes table missing')
const v4Contract = nodeTable.governedStructuredFields?.identity_anchor_profile
if (!v4Contract) fail('logical-schema-v4 governed identity_anchor_profile missing')

if (v4Contract.storagePath !== 'fields_json.identity_anchor_profile') {
  fail(`logical-schema-v4 storage path mismatch: ${v4Contract.storagePath}`)
}
if (JSON.stringify(v4Contract.subtypeEnum) !== JSON.stringify(expectedSubtype)) {
  fail('logical-schema-v4 subtype enum drifted from domain.json')
}
if (!String(v4Contract.legacyBareSubtypePolicy ?? '').includes('MUST NOT')) {
  fail('logical-schema-v4 must explicitly reject bare fields_json.subtype as authority')
}

const subtypeNote = String(nodeTable.v4Subtype ?? '')
if (!subtypeNote.includes('supersedes') || !subtypeNote.includes('identity_anchor_profile')) {
  fail('logical-schema-v4 v4Subtype note does not mark the old bare prototype superseded')
}

// The current physical envelope remains v3; this gate is intentionally semantic.
// A future version bump may be added by a separate approved release train.
if (DOMAIN.appSchemaVersion !== 3 || DOMAIN.graphPayload?.version !== 3) {
  fail('R37 profile registration must not silently activate a schema/payload version bump')
}

console.log(
  `IDENTITY_ANCHOR_PROFILE_CONTRACT=PASS values=${actualSubtype.join(',')} storage=${v4Contract.storagePath}`,
)
