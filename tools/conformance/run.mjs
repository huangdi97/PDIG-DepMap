#!/usr/bin/env node
/**
 * PDIG Conformance Harness
 *
 * 统一入口：把 spec / codegen / fixtures / 三端报告 串成一条可重复执行的 Gate。
 *
 *   node tools/conformance/run.mjs
 *
 * 执行顺序：
 *   1. CODEGEN GATE        spec → generated 一致性（手改 generated 会失败）
 *   2. FIXTURE INTEGRITY   每个 fixture 的 sha256 必须与 manifest 一致
 *   3. ORACLE SELFCHECK    冻结的 TS oracle 必须逐字节复现全部 fixture
 *   4. PLATFORM REPORTS    读取 conformance/reports/<platform>.json 并逐用例 diff
 *   5. SUMMARY             汇总矩阵 + 退出码
 *
 * 平台报告契约（各端 runner 产出）：
 *   conformance/reports/<platform>.json
 *   {
 *     "platform": "android" | "harmony" | "ios",
 *     "specVersion": "1.0.0",
 *     "generatedAt": "<ISO8601>",
 *     "results": {
 *       "<caseId>": { "status": "PASS"|"FAIL"|"NOT_IMPLEMENTED", "actual": <any> }
 *     }
 *   }
 *
 * 退出码：0 = 全部可判定项 PASS；1 = 存在 FAIL / 完整性错误。
 * 「平台报告缺失」不算 FAIL，记为 NOT_RUN（诚实口径，不虚报）。
 */

import { createHash } from 'node:crypto'
import { existsSync, readFileSync, mkdirSync, writeFileSync } from 'node:fs'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import { spawnSync } from 'node:child_process'

const HERE = dirname(fileURLToPath(import.meta.url))
const ROOT = resolve(HERE, '..', '..')
const NODE = process.execPath

const CONF = join(ROOT, 'conformance')
const MANIFEST = join(CONF, 'CONFORMANCE_MANIFEST.json')
const REPORTS = join(CONF, 'reports')

const PLATFORMS = ['android', 'harmony', 'ios']

let hardFailures = 0
const summary = {
  generatedAt: new Date().toISOString(),
  gates: {},
  fixtures: { total: 0, integrityPass: 0, integrityFail: [] },
  oracleSelfcheck: 'NOT_RUN',
  platforms: {},
}

function hr(title) {
  console.log('')
  console.log('─'.repeat(70))
  console.log(title)
  console.log('─'.repeat(70))
}

// ---------------------------------------------------------------------------
// 1. CODEGEN GATE
// ---------------------------------------------------------------------------

hr('1. CODEGEN GATE (spec → generated)')
{
  const r = spawnSync(NODE, [join(ROOT, 'tools', 'codegen', 'generate.mjs'), '--check'], {
    cwd: ROOT,
    encoding: 'utf8',
  })
  process.stdout.write(r.stdout ?? '')
  process.stderr.write(r.stderr ?? '')
  const ok = r.status === 0
  summary.gates.codegen = ok ? 'PASS' : 'FAIL'
  if (!ok) hardFailures++
}

// ---------------------------------------------------------------------------
// 2. FIXTURE INTEGRITY
// ---------------------------------------------------------------------------

hr('2. FIXTURE INTEGRITY (sha256 vs manifest)')
if (!existsSync(MANIFEST)) {
  console.error('FAIL  conformance/CONFORMANCE_MANIFEST.json is missing')
  console.error('Generate it with: cd core && node --experimental-strip-types scripts/generate-conformance.ts')
  summary.gates.fixtureIntegrity = 'FAIL'
  hardFailures++
} else {
  const manifest = JSON.parse(readFileSync(MANIFEST, 'utf8'))
  summary.specVersion = manifest.specVersion
  summary.oracle = manifest.oracle
  summary.fixtures.total = manifest.fixtures.length

  for (const f of manifest.fixtures) {
    const p = join(ROOT, f.path)
    if (!existsSync(p)) {
      summary.fixtures.integrityFail.push({ path: f.path, reason: 'missing' })
      continue
    }
    const h = createHash('sha256').update(readFileSync(p)).digest('hex')
    if (h !== f.sha256) {
      summary.fixtures.integrityFail.push({ path: f.path, reason: 'sha256 mismatch' })
    } else {
      summary.fixtures.integrityPass++
    }
  }

  const importDir = join(ROOT, manifest.importFixtures.directory)
  let importOk = 0
  let importFail = 0
  for (const [name, expected] of Object.entries(manifest.importFixtures.sha256)) {
    const p = join(importDir, name)
    if (!existsSync(p)) {
      importFail++
      continue
    }
    const h = createHash('sha256').update(readFileSync(p)).digest('hex')
    if (h === expected) importOk++
    else importFail++
  }
  summary.fixtures.importTotal = manifest.importFixtures.count
  summary.fixtures.importIntegrityPass = importOk
  summary.fixtures.importIntegrityFail = importFail

  const ok = summary.fixtures.integrityFail.length === 0 && importFail === 0
  console.log(`cases:   ${summary.fixtures.integrityPass}/${summary.fixtures.total} ok`)
  console.log(`imports: ${importOk}/${manifest.importFixtures.count} ok`)
  for (const f of summary.fixtures.integrityFail) console.error(`FAIL  ${f.path}: ${f.reason}`)
  summary.gates.fixtureIntegrity = ok ? 'PASS' : 'FAIL'
  if (!ok) hardFailures++
}

// ---------------------------------------------------------------------------
// 3. ORACLE SELFCHECK
// ---------------------------------------------------------------------------

hr('3. ORACLE SELFCHECK (frozen TS reference reproduces every fixture)')
{
  const script = join(ROOT, 'core', 'scripts', 'generate-conformance.ts')
  const r = spawnSync(NODE, ['--experimental-strip-types', script, '--verify'], {
    cwd: join(ROOT, 'core'),
    encoding: 'utf8',
  })
  const out = (r.stdout ?? '') + (r.stderr ?? '')
  const line = out
    .split(/\r?\n/)
    .filter((l) => l.startsWith('ORACLE SELFCHECK'))
    .join(' ')
  console.log(line || '(no verdict line)')
  const ok = r.status === 0
  summary.oracleSelfcheck = ok ? 'PASS' : 'FAIL'
  summary.gates.oracleSelfcheck = summary.oracleSelfcheck
  if (!ok) hardFailures++
}

// ---------------------------------------------------------------------------
// 4. PLATFORM REPORTS
// ---------------------------------------------------------------------------

hr('4. PLATFORM REPORTS')
const manifest = existsSync(MANIFEST) ? JSON.parse(readFileSync(MANIFEST, 'utf8')) : null
// 期望值必须从 fixture 文件本体读取 —— manifest 只登记 id/path/sha256，不含 expected。
const expectedById = new Map()
if (manifest) {
  for (const f of manifest.fixtures) {
    const p = join(ROOT, f.path)
    if (!existsSync(p)) continue
    const parsed = JSON.parse(readFileSync(p, 'utf8'))
    expectedById.set(f.id, parsed.expected)
  }
}

for (const platform of PLATFORMS) {
  const p = join(REPORTS, `${platform}.json`)
  if (!existsSync(p)) {
    summary.platforms[platform] = {
      status: 'NOT_RUN',
      reason: 'no conformance report produced by this platform yet',
      pass: 0,
      fail: 0,
      total: expectedById.size,
    }
    console.log(`${platform.padEnd(8)} NOT_RUN  (conformance/reports/${platform}.json absent)`)
    continue
  }

  const rep = JSON.parse(readFileSync(p, 'utf8'))
  const results = rep.results ?? {}
  let pass = 0
  let fail = 0
  const mismatches = []

  for (const [id, expExpected] of expectedById) {
    const got = results[id]
    if (!got) {
      fail++
      mismatches.push({ id, reason: 'no result reported' })
      continue
    }
    if (got.status === 'NOT_IMPLEMENTED') {
      // 诚实口径：未实现不是 FAIL，但也不算 PASS
      continue
    }
    if (got.status === 'FAIL') {
      fail++
      mismatches.push({ id, reason: 'platform reported FAIL' })
      continue
    }
    if (got.status !== 'PASS') {
      fail++
      mismatches.push({ id, reason: `unknown status ${JSON.stringify(got.status)}` })
      continue
    }
    // 独立比对：若平台返回了 actual，则 harness 亲自 diff，避免"自我宣称 PASS"
    if (got.actual !== undefined) {
      const a = JSON.stringify(got.actual)
      const b = JSON.stringify(expExpected)
      if (a !== b) {
        fail++
        mismatches.push({ id, reason: 'actual != expected (harness diff)' })
        continue
      }
    }
    pass++
  }

  summary.platforms[platform] = {
    status: fail === 0 ? 'PASS' : 'FAIL',
    reportedAt: rep.generatedAt ?? null,
    pass,
    fail,
    total: expectedById.size,
    mismatches: mismatches.slice(0, 25),
  }
  console.log(`${platform.padEnd(8)} ${fail === 0 ? 'PASS' : 'FAIL'}  ${pass} pass / ${fail} fail / ${expectedById.size} total`)
  if (fail > 0) {
    hardFailures++
    for (const m of mismatches.slice(0, 10)) console.error(`  FAIL ${m.id}: ${m.reason}`)
  }
}

// ---------------------------------------------------------------------------
// 5. SUMMARY
// ---------------------------------------------------------------------------

hr('SUMMARY')
summary.verdict = hardFailures === 0 ? 'PASS' : 'FAIL'
summary.platformsImplemented = Object.entries(summary.platforms)
  .filter(([, v]) => v.status !== 'NOT_RUN')
  .map(([k]) => k)

mkdirSync(REPORTS, { recursive: true })
const summaryPath = join(REPORTS, 'SUMMARY.json')
writeFileSync(summaryPath, JSON.stringify(summary, null, 2) + '\n', 'utf8')

console.log(`codegen          : ${summary.gates.codegen ?? 'NOT_RUN'}`)
console.log(`fixtureIntegrity : ${summary.gates.fixtureIntegrity ?? 'NOT_RUN'}`)
console.log(`oracleSelfcheck  : ${summary.oracleSelfcheck}`)
for (const [p, v] of Object.entries(summary.platforms)) {
  console.log(`platform ${p.padEnd(8)}: ${v.status}${v.reason ? ` (${v.reason})` : ''}`)
}
console.log('')
console.log(`VERDICT: ${summary.verdict}`)
console.log(`summary written to conformance/reports/SUMMARY.json`)
console.log('')
console.log('NOTE: platform NOT_RUN is an honest "not yet implemented" — it is not reported as PASS.')

process.exit(hardFailures === 0 ? 0 : 1)
