#!/usr/bin/env node
import { readFileSync, existsSync } from 'node:fs'
import { resolve, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'

const HERE = dirname(fileURLToPath(import.meta.url))
const ROOT = resolve(HERE, '..')
const PLAN_PATH = resolve(ROOT, 'spec/roadmap/canonical-v4-expansion-plan.json')

function fail(message) {
  console.error('CANONICAL_V4_EXPANSION_PLAN=FAIL')
  console.error(message)
  process.exit(1)
}

const plan = JSON.parse(readFileSync(PLAN_PATH, 'utf8'))
const packages = plan.packages ?? []
const byId = new Map()

for (const pkg of packages) {
  if (!pkg.id || typeof pkg.id !== 'string') fail('Every package needs a string id')
  if (byId.has(pkg.id)) fail(`Duplicate package id: ${pkg.id}`)
  byId.set(pkg.id, pkg)
}

const requiredTop = [
  'planVersion',
  'currentBaseline',
  'permanentInvariants',
  'activationStages',
  'packages',
  'activationRules',
]
for (const key of requiredTop) {
  if (!(key in plan)) fail(`Missing top-level key: ${key}`)
}

if (plan.currentBaseline.appSchemaVersion !== 3) {
  fail(`Plan baseline must match current appSchemaVersion=3, got ${plan.currentBaseline.appSchemaVersion}`)
}
if (plan.currentBaseline.graphPayloadVersion !== 3) {
  fail(`Plan baseline must match current graphPayloadVersion=3, got ${plan.currentBaseline.graphPayloadVersion}`)
}

const expectedPrimary = ['NOW', 'INFRASTRUCTURE', 'CHANGE', 'RECORDS', 'ME']
if (JSON.stringify(plan.currentBaseline.primaryNavigation) !== JSON.stringify(expectedPrimary)) {
  fail('Primary navigation must remain exactly NOW/INFRASTRUCTURE/CHANGE/RECORDS/ME')
}

const stageIds = new Set()
let lastStage = -1
for (const stage of plan.activationStages) {
  if (!Number.isInteger(stage.stage)) fail(`Invalid stage number: ${JSON.stringify(stage)}`)
  if (stage.stage <= lastStage) fail('activationStages must be strictly increasing')
  if (!stage.id || stageIds.has(stage.id)) fail(`Invalid/duplicate stage id: ${stage.id}`)
  lastStage = stage.stage
  stageIds.add(stage.id)
}
const validStages = new Set(plan.activationStages.map((s) => s.stage))

for (const pkg of packages) {
  if (!validStages.has(pkg.stage)) fail(`Package ${pkg.id} references unknown stage ${pkg.stage}`)
  if (pkg.newPrimaryDestination !== false) {
    fail(`Package ${pkg.id} attempts to create a new primary destination`)
  }
  if (!Array.isArray(pkg.hardDependsOn) || !Array.isArray(pkg.optionalDependsOn)) {
    fail(`Package ${pkg.id} dependencies must be arrays`)
  }
  if (!Array.isArray(pkg.canonicalArtifacts) || pkg.canonicalArtifacts.length === 0) {
    fail(`Package ${pkg.id} must declare canonicalArtifacts`)
  }
  if (!pkg.proposal || typeof pkg.proposal !== 'string') {
    fail(`Package ${pkg.id} must point to a proposal/contract`)
  }
  const proposalPath = resolve(ROOT, pkg.proposal)
  if (!existsSync(proposalPath)) {
    fail(`Package ${pkg.id} proposal does not exist: ${pkg.proposal}`)
  }

  for (const dep of [...pkg.hardDependsOn, ...pkg.optionalDependsOn]) {
    if (!byId.has(dep)) fail(`Package ${pkg.id} references unknown dependency ${dep}`)
    if (dep === pkg.id) fail(`Package ${pkg.id} cannot depend on itself`)
  }
  for (const dep of pkg.hardDependsOn) {
    if (byId.get(dep).stage > pkg.stage) {
      fail(`Hard dependency order invalid: ${pkg.id}(stage ${pkg.stage}) -> ${dep}(stage ${byId.get(dep).stage})`)
    }
  }
}

// Hard-dependency graph must remain acyclic.
const visiting = new Set()
const visited = new Set()
function visit(id, path = []) {
  if (visiting.has(id)) fail(`Hard dependency cycle: ${[...path, id].join(' -> ')}`)
  if (visited.has(id)) return
  visiting.add(id)
  for (const dep of byId.get(id).hardDependsOn) visit(dep, [...path, id])
  visiting.delete(id)
  visited.add(id)
}
for (const id of byId.keys()) visit(id)

// Future/non-baseline packages cannot claim implementation without the full Canonical chain.
for (const pkg of packages) {
  if (pkg.stage > 0 && /IMPLEMENTED_CANONICAL|RUNTIME_VERIFIED/.test(pkg.state)) {
    fail(`Future package ${pkg.id} overclaims state: ${pkg.state}`)
  }
}

// Permanent invariants must keep the key product safety boundaries explicit.
const invariantText = plan.permanentInvariants.join('\n').toLowerCase()
for (const required of [
  'proposal != reality',
  'unknown != safe',
  'done != verified',
  'path count != independent path count',
  'new capability != new primary tab',
]) {
  if (!invariantText.includes(required)) fail(`Missing permanent invariant: ${required}`)
}

console.log(`CANONICAL_V4_EXPANSION_PLAN=PASS packages=${packages.length} stages=${plan.activationStages.length}`)
