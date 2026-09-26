#!/usr/bin/env node
// PDIG runtime-sweep tooling: extract the 73 feature rows from NATIVE_PARITY_MATRIX.md
// into a machine-readable JSON seed (single source of truth for RUNTIME_ACCEPTANCE_MATRIX).
//
// Usage: node tools/runtime/extract-parity.mjs  ->  runtime/seed-features.json
//
// The markdown matrix is a human doc; this extractor turns its 6 feature tables
// (including the Settings/Privacy/About rows inside a blockquote) into stable IDs.
// It does not judge statuses — it only extracts names/ids so the sweep matrix can
// be seeded from the same 73 rows the project already counts.

import { readFileSync, writeFileSync, mkdirSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const root = join(dirname(fileURLToPath(import.meta.url)), "..", "..");
const matrixPath = join(root, "NATIVE_PARITY_MATRIX.md");
const outPath = join(root, "runtime", "seed-features.json");

const text = readFileSync(matrixPath, "utf8");
// Strip blockquote marker at line start so blockquote tables parse like normal ones.
const unquoted = text
  .split(/\r?\n/)
  .map((l) => l.replace(/^>\s?/, ""))
  .join("\n");

const SECTION_ORDER = [
  { id: "domain", title: "1. 领域 / 语义层" },
  { id: "persistence", title: "2. 持久化与迁移" },
  { id: "security", title: "3. 安全 / 密钥 / 认证" },
  { id: "import", title: "4. 导入 / 解析" },
  { id: "ui", title: "5. UI" },
  { id: "engineering", title: "6. 工程 / 发布" },
];

function splitRow(line) {
  const trimmed = line.trim();
  if (!trimmed.startsWith("|")) return null;
  if (!trimmed.endsWith("|")) return null;
  let body = trimmed.slice(1, -1);
  body = body.replace(/\\\|/g, "«PIPE»");
  const cells = body.split("|").map((c) => c.trim().replace(/«PIPE»/g, "|"));
  return cells;
}

function isHeaderRow(cells) {
  return cells.every((c) => /^:?-{2,}:?$/.test(c) || c === "" || c === "---");
}

const features = [];
let currentSection = null;

for (const raw of unquoted.split(/\r?\n/)) {
  const line = raw.trim();
  const headerMatch = line.match(/^##\s+\d\.\s+(.+)$/);
  if (headerMatch) {
    const title = line.replace(/^##\s+/, "").trim();
    const sec = SECTION_ORDER.find((s) => title.startsWith(s.title));
    currentSection = sec ?? null;
    continue;
  }
  if (!line.includes("|")) continue;
  const cells = splitRow(line);
  if (!cells || cells.length < 3) continue;
  if (isHeaderRow(cells)) continue;
  if (!currentSection) continue;
  const [capability] = cells;
  if (!capability) continue;
  // Skip the summary/breakdown tables (they have no capability column or are outside sections)
  if (/^\s*\|?平台|已完成|节|合计|仍未完成的|格|#|能力\s*$/.test(capability) && cells.length <= 4) continue;

  const idSuffix = features.filter((f) => f.section === currentSection.id).length + 1;
  features.push({
    id: `${sectionLetter(currentSection.id)}${String(idSuffix).padStart(2, "0")}`,
    section: currentSection.id,
    canonical_feature: capability.replace(/\s+/g, " "),
    android: cells[1]?.split("（")[0]?.trim() ?? "",
    harmony: cells[2]?.split("（")[0]?.trim() ?? "",
    ios: cells[3]?.split("（")[0]?.trim() ?? "",
  });
}

function sectionLetter(id) {
  const map = { domain: "D", persistence: "P", security: "S", import: "I", ui: "U", engineering: "E" };
  return map[id] ?? "X";
}

if (features.length !== 73) {
  console.error(`Expected 73 features, extracted ${features.length}`);
  process.exit(1);
}

mkdirSync(dirname(outPath), { recursive: true });
writeFileSync(outPath, JSON.stringify(features, null, 2) + "\n", "utf8");
console.log(`Wrote ${features.length} features -> runtime/seed-features.json`);
const perSection = Object.groupBy(features, (f) => f.section);
for (const [k, v] of Object.entries(perSection)) console.log(`  ${k}: ${v.length}`);