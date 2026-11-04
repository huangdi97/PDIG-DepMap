#!/usr/bin/env node
import { readFileSync } from 'node:fs'
import { resolve, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'

const HERE = dirname(fileURLToPath(import.meta.url))
const ROOT = resolve(HERE, '..')
const DOMAIN = JSON.parse(readFileSync(resolve(ROOT, 'spec/domain/domain.json'), 'utf8'))
const V4 = JSON.parse(readFileSync(resolve(ROOT, 'spec/schema/logical-schema-v4.json'), 'utf8'))

function fail(message) {
  console.error('MAINTENANCE_PROFILE_CONTRACT=FAIL')
  console.error(message)
  process.exit(1)
}

const expectedFactKinds = [
  'card_annual_fee_amount',
  'card_annual_fee_currency',
  'card_billing_day',
  'card_payment_due_day',
  'card_autopay_mode',
  'number_billing_mode',
  'number_plan_cost',
  'number_plan_currency',
  'number_renewal_method',
]
const expectedScheduleKinds = [
  'card_annual_fee_checkpoint',
  'card_billing_checkpoint',
  'card_payment_due_checkpoint',
  'number_keep_alive',
  'number_plan_renewal',
  'fact_freshness_review',
  'custom_maintenance',
]
const expectedFactStates = ['confirmed', 'retired']
const expectedScheduleStates = ['active', 'paused', 'needs_review', 'retired']
const expectedValueTypes = ['decimal_string', 'currency_code', 'integer', 'text', 'boolean']
const expectedCadenceKinds = ['one_time', 'monthly_day', 'yearly_month_day', 'interval_days', 'manual_only']
const expectedOverflow = ['clamp_to_last_day', 'skip_occurrence', 'user_confirm']

for (const [name, expected] of [
  ['MaintenanceFactKind', expectedFactKinds],
  ['MaintenanceScheduleKind', expectedScheduleKinds],
  ['MaintenanceFactState', expectedFactStates],
  ['MaintenanceScheduleState', expectedScheduleStates],
  ['MaintenanceValueType', expectedValueTypes],
  ['MaintenanceCadenceKind', expectedCadenceKinds],
  ['MaintenanceOverflowPolicy', expectedOverflow],
]) {
  const actual = DOMAIN.enums?.[name]?.values
  if (JSON.stringify(actual) !== JSON.stringify(expected)) {
    fail(name + ' vocabulary drifted: ' + JSON.stringify(actual))
  }
}

const contract = DOMAIN.entities?.Node?.structuredFieldContracts?.all_nodes?.maintenance_profile
if (!contract) fail('Node fields.maintenance_profile contract is missing')
if (contract.physicalStorage !== 'nodes.fields_json.maintenance_profile') {
  fail('unexpected maintenance_profile physical storage')
}
if (contract.containerVersion !== 1) fail('maintenance_profile version must be 1')
if (contract.cardExpiryOwner !== 'Node.expiryDate') {
  fail('Node.expiryDate must remain the single card-expiry owner')
}
if (!String(contract.installmentPolicy ?? '').includes('not Canonical v1')) {
  fail('installment scope boundary is missing')
}
if (contract.verificationBasisType !== 'VerificationBasisType') {
  fail('maintenance_profile must reuse VerificationBasisType')
}
for (const field of ['id', 'kind', 'value_type', 'value', 'state', 'verification_basis_type', 'confirmed_at']) {
  if (!(contract.facts?.requiredFields ?? []).includes(field)) fail('missing fact field: ' + field)
}
for (const field of ['id', 'kind', 'state', 'cadence', 'verification_basis_type', 'confirmed_at']) {
  if (!(contract.schedules?.requiredFields ?? []).includes(field)) fail('missing schedule field: ' + field)
}
if (!String(contract.facts?.proposalPolicy ?? '').includes('MUST NOT')) {
  fail('maintenance fact proposal/Reality boundary is not explicit')
}
if (!String(contract.schedules?.proposalPolicy ?? '').includes('MUST')) {
  fail('maintenance schedule proposal/Reality boundary is not explicit')
}
if (!String(contract.applicability?.numberAuthority ?? '').includes('phone_number')) {
  fail('number lifecycle authority must require governed phone_number subtype')
}
for (const rule of [
  'provider_policy_without_review',
  'statement_date_pattern_without_review',
  'transaction_pattern',
  'elapsed_time_as_completion',
  'notification_delivery_as_completion',
  'reference_fixture_value',
]) {
  if (!(contract.forbiddenInference ?? []).includes(rule)) {
    fail('missing forbidden maintenance inference: ' + rule)
  }
}

const nodeTable = V4.tables?.find((t) => t.name === 'nodes')
if (!nodeTable) fail('logical-schema-v4 nodes table missing')
const logical = nodeTable.governedStructuredFields?.maintenance_profile
if (!logical) fail('logical-schema-v4 maintenance_profile missing')
if (logical.storagePath !== 'fields_json.maintenance_profile') fail('logical storage drift')
if (logical.containerVersion !== 1) fail('logical version drift')
if (JSON.stringify(logical.facts?.kindEnum) !== JSON.stringify(expectedFactKinds)) fail('logical fact kind drift')
if (JSON.stringify(logical.schedules?.kindEnum) !== JSON.stringify(expectedScheduleKinds)) fail('logical schedule kind drift')
if (JSON.stringify(logical.cadence?.kindEnum) !== JSON.stringify(expectedCadenceKinds)) fail('logical cadence kind drift')
if (JSON.stringify(logical.cadence?.overflowPolicyEnum) !== JSON.stringify(expectedOverflow)) fail('logical overflow policy drift')
if (!String(logical.numberAuthority ?? '').includes('phone_number')) fail('logical phone authority boundary missing')
if (!String(logical.proposalPolicy ?? '').includes('MUST')) fail('logical proposal boundary missing')
if (!String(logical.temporalPolicy ?? '').includes('never auto-completes')) fail('temporal completion boundary missing')

// R40 is additive inside the existing cross-platform fields envelope.
// A physical version bump requires a separate approved migration train.
if (DOMAIN.appSchemaVersion !== 3 || DOMAIN.graphPayload?.version !== 3) {
  fail('R40 maintenance registration must not silently bump schema/payload version')
}

console.log(
  `MAINTENANCE_PROFILE_CONTRACT=PASS facts=${expectedFactKinds.length} schedules=${expectedScheduleKinds.length} storage=${logical.storagePath}`,
)
