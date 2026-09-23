#!/usr/bin/env node
/**
 * PDIG 全仓质量 Gate（单命令） — 2026-09-23 (v0.1.0 round)
 *
 * 运行：  node scripts/quality/check-quality.mjs
 * 退出码：0 = 全部 PASS；1 = 任一 FAIL。
 *
 * 覆盖（C1 合同要求）：
 *   1. production 文件 > 300 行（注册例外除外）
 *   2. 大型 @Composable > 200 行
 *   3. 裸 TODO/FIXME/HACK（case-sensitive；XXX/TEMP 为合法 sentinel/环境变量，见审计）
 *   4. forbidden suppressions/ignores（@Suppress / @ts-ignore / @ts-expect-error / type: ignore / noqa）
 *   5. 依赖环（Kotlin package 级 import 图，DFS 检测）
 *   6. secret 模式（私钥块 / 硬编码口令 / api key / token）
 *   7. 敏感日志（Log./println 携带 password/secret/token/decrypted 等）
 *   8. generated-code 例外识别（generated/ 路径 + 生成器头注释 → 自动豁免）
 *   9. Kotlin 不安全点（!! / lateinit / GlobalScope / 裸 catch / unchecked cast）逐点分类
 *
 * 作用域（active production scope，见 GLOBAL_ARCHITECTURE_AUDIT.md のscope）：
 *   android/app/src/main、android/core/src/main、android/conformance/src/main、desktop（星号通配子目录）
 *   iOS/Harmony/legacy/core-TS 为只读静态审计范围（PAUSED/BLOCKED track），不参与 gating。
 */
 import fs from "node:fs";
 import path from "node:path";

const ROOT = path.resolve(import.meta.dirname, "../..");
const EXC = JSON.parse(fs.readFileSync(path.join(ROOT, "scripts/quality/EXCEPTIONS.json"), "utf8"));

const SCOPE_DIRS = ["android/app/src/main", "android/core/src/main", "android/conformance/src/main", "desktop"];
const EXT = new Set([".kt", ".ts", ".mjs", ".js"]);
const GENERATED_PATH = /[\\/]generated[\\/]|codegen|generate\.mjs/;
const GENERATED_HEADER = /Generated from canonical|DO NOT EDIT|codegen output/i;

function inScope(p) {
  const r = path.relative(ROOT, p).replace(/\\/g, "/");
  return SCOPE_DIRS.some((d) => r.startsWith(d + "/")) && EXT.has(path.extname(p));
}
function rel(p) {
  return path.relative(ROOT, p).replace(/\\/g, "/");
}
function walk(dir, out = []) {
  if (!fs.existsSync(dir)) return out;
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, e.name);
    if (e.isDirectory()) {
      if (["build", ".gradle", "node_modules", ".kotlin"].includes(e.name)) continue;
      walk(full, out);
    } else if (inScope(full)) out.push(full);
  }
  return out;
}
function isGenerated(p) {
  if (GENERATED_PATH.test(p)) return true;
  try {
    return GENERATED_HEADER.test(fs.readFileSync(p, "utf8").slice(0, 400));
  } catch {
    return false;
  }
}
const lineCount = (p) => fs.readFileSync(p, "utf8").split("\n").length;

const results = [];
const report = (check, counter, ok, detail) => results.push({ check, counter, ok, detail });
const matches = (line, re) => { re.lastIndex = 0; return re.test(line); };

// ---------------------------------------------------------------- 1. file > 300
{
  const over = [];
  const exempted = [];
  for (const f of walk(ROOT)) {
    if (!f.endsWith(".kt") && !f.endsWith(".ts")) continue;
    const n = lineCount(f);
    if (n <= 300) continue;
    const r = rel(f);
    if (isGenerated(f)) exempted.push(`${r} (generated, ${n} lines)`);
    else {
      const entry = EXC.fileSizeOver300.find((x) => x.path === r);
      if (entry) exempted.push(`${r} (${entry.category}, ${n} lines)`);
      else over.push(`${r} (${n} lines)`);
    }
  }
  report("file-size", "UNJUSTIFIED_PRODUCTION_FILE_GT_300", over.length === 0,
    over.length ? `\n  ${over.join("\n  ")}` : `OK (exempted: ${exempted.length})`);
}

// ---------------------------------------------------------------- 2. @Composable > 200
function findClosingBrace(lines, startIdx, openCol) {
  let depth = 1;
  for (let i = startIdx; i < lines.length; i++) {
    for (let c = i === startIdx ? openCol + 1 : 0; c < lines[i].length; c++) {
      const ch = lines[i][c];
      if (ch === "{") depth++;
      else if (ch === "}") {
        depth--;
        if (depth === 0) return i;
      }
    }
  }
  return -1;
}
{
  const big = [];
  for (const f of walk(ROOT)) {
    if (!f.endsWith(".kt")) continue;
    const lines = fs.readFileSync(f, "utf8").split("\n");
    for (let i = 0; i < lines.length; i++) {
      if (!lines[i].includes("@Composable")) continue;
      let j = i;
      while (j < lines.length && !lines[j].match(/^\s*fun\s/) && !lines[j].includes(" fun ")) j++;
      if (j >= lines.length) continue;
      const open = lines[j].indexOf("{");
      if (open < 0) continue;
      const closing = findClosingBrace(lines, j, open);
      if (closing < 0) continue;
      const size = closing - j + 1;
      if (size > 200) {
        const r = rel(f);
        const funLine = lines[j].trim();
        if (EXC.largeComposable.find((x) => x.path === r && x.fun === funLine)) continue;
        big.push(`${r}:${j + 1} ${funLine.slice(0, 80)} (${size} lines)`);
      }
      i = closing;
    }
  }
  report("composable", "CRITICAL_COMPLEXITY_VIOLATION", big.length === 0,
    big.length ? `\n  ${big.join("\n  ")}` : "OK");
}

// ---------------------------------------------------------------- 3. bare TODO
{
  const re = /\b(TODO|FIXME|HACK)\b/;
  const hits = [];
  for (const f of walk(ROOT)) {
    if (isGenerated(f)) continue;
    const lines = fs.readFileSync(f, "utf8").split("\n");
    lines.forEach((l, i) => { if (matches(l, re)) hits.push(`${rel(f)}:${i + 1}`); });
  }
  report("todo", "RAW_TODO", hits.length === 0, hits.length ? `\n  ${hits.join("\n  ")}` : "OK");
}

// ---------------------------------------------------------------- 4. forbidden suppressions
{
  const re = /@Suppress\(|@ts-ignore|@ts-expect-error|type:\s*ignore|\bnoqa\b/;
  const hits = [];
  for (const f of walk(ROOT)) {
    if (isGenerated(f)) continue;
    const lines = fs.readFileSync(f, "utf8").split("\n");
    lines.forEach((l, i) => { if (matches(l, re)) hits.push(`${rel(f)}:${i + 1} ${l.trim().slice(0, 80)}`); });
  }
  report("suppress", "FORBIDDEN_SUPPRESSION", hits.length === 0, hits.length ? `\n  ${hits.join("\n  ")}` : "OK");
}

// ---------------------------------------------------------------- 5. dependency cycle
{
  const edges = new Set();
  const nodes = new Set();
  for (const f of walk(ROOT)) {
    if (!f.endsWith(".kt")) continue;
    const src = fs.readFileSync(f, "utf8");
    const pkgM = src.match(/^package\s+([\w.]+)/m);
    if (!pkgM) continue;
    const from = pkgM[1];
    nodes.add(from);
    for (const m of src.matchAll(/^import\s+(com\.pdig[\w.]*)/gm)) {
      const to = m[1].split(".").slice(0, 3).join(".");
      if (to !== from) { edges.add(`${from} -> ${to}`); nodes.add(to); }
    }
  }
  const adj = {};
  for (const e of edges) {
    const [a, b] = e.split(" -> ");
    (adj[a] ||= new Set()).add(b);
  }
  const GRAY = 1, BLACK = 2;
  const color = {};
  const cycle = [];
  const stack = [];
  function dfs(n) {
    color[n] = GRAY;
    stack.push(n);
    for (const m of adj[n] || []) {
      if (color[m] === GRAY) {
        const idx = stack.indexOf(m);
        cycle.push(stack.slice(idx).concat(m).join(" -> "));
      } else if (!color[m]) dfs(m);
    }
    stack.pop();
    color[n] = BLACK;
  }
  for (const n of nodes) if (!color[n]) dfs(n);
  report("cycle", "DEPENDENCY_CYCLE", cycle.length === 0, cycle.length ? `\n  ${[...new Set(cycle)].join("\n  ")}` : "OK");
}

// ---------------------------------------------------------------- 6. secret patterns
{
  const re = /-----BEGIN [A-Z ]*PRIVATE KEY-----|password\s*=\s*["'][^"']+["']|storePassword\s*=\s*["'][^"']+["']|api[_-]?key\s*=\s*["'][^"']+["']|token\s*=\s*["'][A-Za-z0-9_-]{12,}["']/;
  const hits = [];
  for (const f of walk(ROOT)) {
    if (isGenerated(f)) continue;
    const lines = fs.readFileSync(f, "utf8").split("\n");
    lines.forEach((l, i) => {
      if (matches(l, re)) {
        const r = rel(f);
        if (EXC.secretPattern.find((x) => x.path === r && x.line === i + 1)) return;
        hits.push(`${r}:${i + 1} ${l.trim().slice(0, 90)}`);
      }
    });
  }
  report("secret", "HARDCODED_SECRET", hits.length === 0, hits.length ? `\n  ${hits.join("\n  ")}` : "OK");
}

// ---------------------------------------------------------------- 7. sensitive logging
{
  const re = /(Log\.[diew]|println)\s*\([^)]*(password|passphrase|secret|token|keyPassword|storePassword|db_passphrase|decrypted|rawStatement)/i;
  const hits = [];
  for (const f of walk(ROOT)) {
    if (!f.endsWith(".kt") && !f.endsWith(".ts")) continue;
    if (isGenerated(f)) continue;
    const lines = fs.readFileSync(f, "utf8").split("\n");
    lines.forEach((l, i) => { if (matches(l, re)) hits.push(`${rel(f)}:${i + 1} ${l.trim().slice(0, 90)}`); });
  }
  report("senslog", "SENSITIVE_LOGGING", hits.length === 0, hits.length ? `\n  ${hits.join("\n  ")}` : "OK");
}

// ---------------------------------------------------------------- 8. Kotlin escapes
{
  const prod = walk(ROOT).filter((p) => p.endsWith(".kt") && p.includes(`${path.sep}main${path.sep}`));
  const kinds = [
    { name: "bangbang", re: /!!/, label: "UNJUSTIFIED_BANGBANG" },
    { name: "lateinit", re: /lateinit\s+var/, label: "UNJUSTIFIED_LATEINIT" },
    { name: "globalscope", re: /GlobalScope/, label: "UNJUSTIFIED_GLOBALSCOPE" },
    { name: "barecatch", re: /catch\s*\{|catch\s*\([^:)]*\)\s*\{/, label: "UNJUSTIFIED_BARE_CATCH" },
    { name: "uncheckedcast", re: /\)\s+as\s+[A-Z][A-Za-z0-9_<>?,.\s]*$/, label: "UNJUSTIFIED_CAST" },
  ];
  let total = 0;
  const details = [];
  for (const k of kinds) {
    const hits = [];
    for (const f of prod) {
      if (isGenerated(f)) continue;
      const lines = fs.readFileSync(f, "utf8").split("\n");
      lines.forEach((l, i) => {
        if (matches(l, k.re)) {
          const r = rel(f);
          const exc = EXC.kotlinEscapes.find((x) => x.path === r && x.line === i + 1 && x.kind === k.name);
          if (!exc) hits.push(`${r}:${i + 1} ${l.trim().slice(0, 80)}`);
        }
      });
    }
    total += hits.length;
    if (hits.length) details.push(`\n[${k.label}] ${hits.length}\n  ${hits.join("\n  ")}`);
  }
  report("kotlin-escape", "UNJUSTIFIED_KOTLIN_ESCAPE", total === 0, total === 0 ? "OK" : details.join(""));
}

// ---------------------------------------------------------------- 9. known dead code / engineering gap (manual register)
{
  const dead = EXC.knownDeadCode || [];
  const gaps = EXC.engineeringGaps || [];
  report("deadcode", "KNOWN_DEAD_CODE", dead.length === 0, dead.length ? `\n  ${dead.join("\n  ")}` : "OK");
  report("gap", "ENGINEERING_GAP", gaps.length === 0, gaps.length ? `\n  ${gaps.join("\n  ")}` : "OK");
}

// ---------------------------------------------------------------- verdict
const failed = results.filter((r) => !r.ok);
const counters = results.map((r) => `${r.counter}=${r.ok ? 0 : "VIOLATION"}`).join(" ; ");
console.log(`PDIG QUALITY GATE — ${new Date().toISOString()}`);
console.log(`scope: ${SCOPE_DIRS.join(", ")}`);
console.log(`counters: ${counters}`);
for (const r of results) {
  console.log(`[${r.ok ? "PASS" : "FAIL"}] ${r.check}${r.detail === "OK" ? "" : r.detail}`);
}
if (failed.length) {
  console.log(`VERDICT: FAIL (${failed.length} check(s))`);
  process.exit(1);
} else {
  console.log("VERDICT: PASS");
  process.exit(0);
}
