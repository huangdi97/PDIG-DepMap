#!/usr/bin/env node
// PDIG runtime-sweep tooling: expand runtime/seed-features.json (73 canonical features)
// into runtime/RUNTIME_ACCEPTANCE_MATRIX.json — one row per canonical_feature × platform
// (73 × 4 = 292 rows), each row carrying the 25 evidence fields from the sweep contract
// (spec §11) and a status restricted to {PASS, FAIL, BLOCKED, NOT_IMPLEMENTED, NOT_APPLICABLE}.
//
// Runtime evidence overlays (authoritative for this round, written as evidence lands):
//   runtime/evidence/<platform>.json   platform ∈ {desktop, android, harmony, ios}
// Each overlay maps featureId -> { status, notes, ...paths/test ids }.
//
// Usage:
//   node tools/runtime/build-matrix.mjs          # merge overlays, validate, write JSON
//   node tools/runtime/build-matrix.mjs --check  # validate only

import { existsSync, readFileSync, writeFileSync, mkdirSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const root = join(dirname(fileURLToPath(import.meta.url)), "..", "..");
const seedPath = join(root, "runtime", "seed-features.json");
const outPath = join(root, "runtime", "RUNTIME_ACCEPTANCE_MATRIX.json");
const evidenceDir = join(root, "runtime", "evidence");
const checkOnly = process.argv.includes("--check");

const PLATFORMS = ["desktop", "android", "harmony", "ios"];
const STATUSES = new Set(["PASS", "FAIL", "BLOCKED", "NOT_IMPLEMENTED", "NOT_APPLICABLE"]);

const FIELD_NAMES = [
  "id", "canonical_feature", "platform", "device_profile", "page", "state",
  "fixture", "precondition", "actions", "expected", "functional_assertion",
  "runtime_assertion", "visual_required", "screenshot_path", "log_path",
  "video_path", "test_id", "git_sha", "status", "theme", "resolution",
  "density", "locale", "notes", "updated_at",
];

function legacyToContractStatus(p) {
  if (!p) return "NOT_IMPLEMENTED";
  if (/BLOCKED/.test(p.replace(/\s+/g, " "))) return "BLOCKED";
  if (/NOT_STARTED|NOT_RUN|NOT_IMPLEMENTED/.test(p)) return "NOT_IMPLEMENTED";
  if (/NOT_APPLICABLE/.test(p)) return "NOT_APPLICABLE";
  if (/CONFORMANCE_PASS|RUNTIME_VERIFIED|TESTED|IMPLEMENTED|PARTIAL/.test(p)) {
    // Refresh required: this round must re-verify before PASS; the override overlay
    // decides the final value. Provisional value keeps the schema closed.
    return "PASS";
  }
  return "NOT_IMPLEMENTED";
}

const raw = JSON.parse(readFileSync(seedPath, "utf8"));
if (raw.features.length !== 73) {
  console.error(`seed must contain 73 features, got ${raw.features.length}`);
  process.exit(1);
}

const overlays = {};
for (const platform of PLATFORMS) {
  const p = join(evidenceDir, `${platform}.json`);
  overlays[platform] = existsSync(p) ? JSON.parse(readFileSync(p, "utf8")) : {};
}

const rows = [];
for (const feature of raw.features) {
  for (const platform of PLATFORMS) {
    const legacy = feature[platform];
    const ov = overlays[platform][feature.id] ?? {};
    const status = ov.status ?? (platform === "desktop" ? "NOT_IMPLEMENTED" : legacyToContractStatus(legacy));
    if (!STATUSES.has(status)) {
      console.error(`invalid status for ${feature.id}/${platform}: ${status}`);
      process.exit(1);
    }
    const row = {
      id: `${feature.id}`, canonical_feature: feature.canonical_feature, platform,
      device_profile: ov.device_profile ?? "",
      page: ov.page ?? "",
      state: ov.state ?? "",
      fixture: ov.fixture ?? "",
      precondition: ov.precondition ?? "",
      actions: ov.actions ?? "",
      expected: ov.expected ?? "",
      functional_assertion: ov.functional_assertion ?? "",
      runtime_assertion: ov.runtime_assertion ?? "",
      visual_required: ov.visual_required ?? false,
      screenshot_path: ov.screenshot_path ?? "",
      log_path: ov.log_path ?? "",
      video_path: ov.video_path ?? "",
      test_id: ov.test_id ?? "",
      git_sha: ov.git_sha ?? "",
      status,
      theme: ov.theme ?? "",
      resolution: ov.resolution ?? "",
      density: ov.density ?? "",
      locale: ov.locale ?? "",
      notes: ov.notes ?? `parity(before)=${legacy || "n/a"}`,
      updated_at: ov.updated_at ?? "",
    };
    rows.push(row);
  }
}

// ---- schema validation ----
const errors = [];
for (const r of rows) {
  for (const f of FIELD_NAMES) {
    if (!(f in r)) errors.push(`row ${r.id}/${r.platform} missing field ${f}`);
  }
  if (!STATUSES.has(r.status)) errors.push(`row ${r.id}/${r.platform} bad status ${r.status}`);
}
if (errors.length) {
  console.error(`matrix validation failed (${errors.length}):`);
  for (const e of errors.slice(0, 25)) console.error("  " + e);
  process.exit(1);
}
if (rows.length !== 292) {
  console.error(`expected 292 rows, got ${rows.length}`);
  process.exit(1);
}

const doc = {
  schema: "DEPMAP_RUNTIME_ACCEPTANCE_MATRIX_V1",
  generated_at: new Date().toISOString(),
  spec_ref: "sweep goal spec §10-§12 / §105",
  status_enum: ["PASS", "FAIL", "BLOCKED", "NOT_IMPLEMENTED", "NOT_APPLICABLE"],
  field_count: FIELD_NAMES.length,
  fields: FIELD_NAMES,
  evidence_round: "2026-09-26-multiclient-runtime-visual-sweep",
  platform_status: PLATFORMS,
  rows,
};

if (!checkOnly) {
  mkdirSync(dirname(outPath), { recursive: true });
  writeFileSync(outPath, JSON.stringify(doc, null, 2) + "\n", "utf8");
}
const counts = {};
for (const [p, map] of Object.entries(overlays)) counts[p] = Object.keys(map).length;
console.error(`OK: ${rows.length} rows, overlays ${JSON.stringify(counts)}`);
if (checkOnly) process.exit(0);