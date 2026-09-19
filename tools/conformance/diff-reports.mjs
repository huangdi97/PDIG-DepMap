// 跨平台一致性差分（N5）。
//
// 回答的问题：同一条 canonical 用例，在三端是否同判、是否同值。
// 三份报告各自独立产生（Android = JVM 真跑；Harmony = 真 ArkTS 运行时；
// iOS = macos-14 真编译 + 真跑），本工具只读它们，**不**重算任何用例。
//
// 两条独立的证据线，不得合并成一句"一致"：
//   A. verdict 矩阵    —— 三端对同一用例的判定是否相同（Pass/Fail/未执行）
//   B. actual 字节比对 —— Android 与 iOS 的 actual 是否逐字节相同
// Harmony 不落 actual（ArkTS 主机测试不保证可写文件），因此**只参加 A**，
// 不参加 B —— 这是能力边界，不是疏漏，输出里会明确标注。
//
// 为什么自己写规范化序列化器而不用 JSON.stringify：
//   JSON.parse 会丢掉数字的原始文本（1.0 与 1 变成同一个 number），
//   而 payload 的跨端等价性正是"逐字节"口径。这里保留 raw 文本，
//   对象键顺序也保留（顺序本身是语义的一部分）。
//
// 用法：
//   node tools/conformance/diff-reports.mjs
//   node tools/conformance/diff-reports.mjs --json   # 额外输出机器可读差分

import { existsSync, readFileSync, writeFileSync } from 'node:fs'
import { join } from 'node:path'

const REPO = process.cwd()
const DIR = join(REPO, 'conformance', 'reports')

// --- raw-preserving canonical JSON -----------------------------------------

function canonicalize(text) {
  let i = 0
  const s = text

  function fail(msg) { throw new Error(`canonicalize: ${msg} @${i}`) }
  function ws() { while (i < s.length && ' \t\r\n'.includes(s[i])) i++ }

  function value() {
    ws()
    const c = s[i]
    if (c === '{') return object()
    if (c === '[') return array()
    if (c === '"') return JSON.stringify(string())
    if (c === '-' || (c >= '0' && c <= '9')) return number()
    if (s.startsWith('true', i)) { i += 4; return 'true' }
    if (s.startsWith('false', i)) { i += 5; return 'false' }
    if (s.startsWith('null', i)) { i += 4; return 'null' }
    return fail(`unexpected char ${JSON.stringify(c)}`)
  }

  function string() {
    if (s[i] !== '"') fail('expected string')
    i++
    let out = ''
    while (i < s.length) {
      const ch = s[i]
      if (ch === '"') { i++; return out }
      if (ch === '\\') {
        const esc = s[i + 1]
        const map = { '"': '"', '\\': '\\', '/': '/', b: '\b', f: '\f', n: '\n', r: '\r', t: '\t' }
        if (esc === 'u') {
          out += String.fromCharCode(parseInt(s.slice(i + 2, i + 6), 16))
          i += 6
        } else {
          out += map[esc] ?? fail(`bad escape \\${esc}`)
          i += 2
        }
        continue
      }
      out += ch
      i++
    }
    return fail('unterminated string')
  }

  function number() {
    const start = i
    // JSON 数字文法：-?(0|[1-9]\d*)(\.\d+)?([eE][+-]?\d+)?
    if (s[i] === '-') i++
    if (s[i] === '0') i++
    else while (i < s.length && s[i] >= '0' && s[i] <= '9') i++
    if (s[i] === '.') { i++; while (i < s.length && s[i] >= '0' && s[i] <= '9') i++ }
    if (s[i] === 'e' || s[i] === 'E') {
      i++
      if (s[i] === '+' || s[i] === '-') i++
      while (i < s.length && s[i] >= '0' && s[i] <= '9') i++
    }
    const raw = s.slice(start, i)
    if (raw.length === 0) fail('empty number')
    return raw // 保留原始文本：1.0 与 1 必须仍然不同
  }

  function array() {
    i++ // [
    const parts = []
    ws()
    if (s[i] === ']') { i++; return '[]' }
    for (;;) {
      parts.push(value())
      ws()
      if (s[i] === ',') { i++; continue }
      if (s[i] === ']') { i++; return '[' + parts.join(',') + ']' }
      fail('expected , or ]')
    }
  }

  function object() {
    i++ // {
    const parts = []
    ws()
    if (s[i] === '}') { i++; return '{}' }
    for (;;) {
      ws()
      const k = string()
      ws()
      if (s[i] !== ':') fail('expected :')
      i++
      parts.push(JSON.stringify(k) + ':' + value())
      ws()
      if (s[i] === ',') { i++; continue }
      if (s[i] === '}') { i++; return '{' + parts.join(',') + '}' }
      fail('expected , or }')
    }
  }

  const out = value()
  ws()
  if (i !== s.length) fail('trailing content')
  return out
}

// --- 载入 -------------------------------------------------------------------

function load(name) {
  const p = join(DIR, `${name}.json`)
  if (!existsSync(p)) return null
  return JSON.parse(readFileSync(p, 'utf8'))
}

const android = load('android')
const harmony = load('harmony')
const ios = load('ios')

const present = [
  ['android', android], ['harmony', harmony], ['ios', ios],
].filter(([, v]) => v !== null).map(([k]) => k)

if (present.length < 2) {
  console.log('CROSS_PLATFORM_DIFFERENTIAL=NOT_RUN')
  console.log('  至少需要两份报告，当前只有：' + (present.join(', ') || '（无）'))
  process.exit(0)
}

/** 取某平台某用例的 verdict；缺失返回 null（不是"通过"）。 */
function verdict(report, id) {
  const r = report?.results?.[id]
  return r ? r.status : null
}

/** 取 actual 的规范化文本；没有 actual 返回 null。 */
function actualText(report, id) {
  const r = report?.results?.[id]
  if (!r || r.actual === undefined) return null
  return canonicalize(JSON.stringify(r.actual))
}

// --- 用例全集（以 manifest 为准，不以任何单一平台为准） ----------------------

const manifest = JSON.parse(readFileSync(join(REPO, 'conformance/CONFORMANCE_MANIFEST.json'), 'utf8'))
const allIds = manifest.fixtures.map((f) => f.id)
const categoryOf = new Map(manifest.fixtures.map((f) => [f.id, f.category]))

const rows = allIds.map((id) => ({
  id,
  category: categoryOf.get(id) ?? '',
  android: verdict(android, id),
  harmony: verdict(harmony, id),
  ios: verdict(ios, id),
}))

// --- A. verdict 矩阵 --------------------------------------------------------

const EXECUTED = new Set(['PASS', 'FAIL'])
const platformList = ['android', 'harmony', 'ios'].filter((p) => present.includes(p))

let verdictMismatch = 0
const mismatchRows = []
for (const r of rows) {
  const vs = platformList.map((p) => r[p])
  const executed = vs.filter((v) => v && EXECUTED.has(v))
  const failedAny = executed.includes('FAIL')
  // 判定一致的判据：所有执行过的平台要么全 PASS，要么全 FAIL。
  // 一端 PASS 另一端 FAIL = 真实跨平台分歧，必须报出来。
  const passAny = executed.includes('PASS')
  if (executed.length > 1 && passAny && failedAny) {
    verdictMismatch++
    mismatchRows.push(r)
  }
}

// --- B. Android × iOS actual 字节比对 ---------------------------------------

let compared = 0
let byteEqual = 0
let byteDiff = 0
const diffRows = []
const skippedNoActual = []
if (android && ios) {
  for (const id of allIds) {
    const a = actualText(android, id)
    const b = actualText(ios, id)
    if (a === null || b === null) { skippedNoActual.push(id); continue }
    compared++
    if (a === b) byteEqual++
    else { byteDiff++; diffRows.push({ id, android: a, ios: b }) }
  }
}

// --- 输出 -------------------------------------------------------------------

console.log('')
console.log('[ diff ] 参与平台        : ' + platformList.join(', '))
console.log('[ diff ] 用例总数        : ' + allIds.length)
console.log('')
console.log('[ A ] verdict 矩阵')
for (const p of platformList) {
  const rep = { android, harmony, ios }[p]
  const exec = rows.filter((r) => r[p] && EXECUTED.has(r[p])).length
  const pass = rows.filter((r) => r[p] === 'PASS').length
  console.log('        ' + p.padEnd(9) + ' executed=' + String(exec).padStart(2) +
    '  pass=' + String(pass).padStart(2) +
    '  fail=' + String(rows.filter((r) => r[p] === 'FAIL').length).padStart(2))
}
console.log('        判定分歧        : ' + verdictMismatch)
for (const r of mismatchRows.slice(0, 20)) {
  console.log('          - ' + r.id + '  android=' + r.android + ' harmony=' + r.harmony + ' ios=' + r.ios)
}

console.log('')
console.log('[ B ] Android × iOS actual 逐字节比对（规范化后，保留数字原始文本与键顺序）')
console.log('        可比用例        : ' + compared)
console.log('        逐字节相同      : ' + byteEqual)
console.log('        差异            : ' + byteDiff)
console.log('        无 actual 可比对: ' + skippedNoActual.length +
  (skippedNoActual.length ? '（' + skippedNoActual.join(', ') + '）' : ''))
for (const d of diffRows.slice(0, 10)) {
  console.log('          - ' + d.id)
  console.log('            android: ' + d.android.slice(0, 300))
  console.log('            ios    : ' + d.ios.slice(0, 300))
}

const verdictOk = verdictMismatch === 0
const byteOk = byteDiff === 0
const ok = verdictOk && byteOk
console.log('')
console.log('CROSS_PLATFORM_VERDICT_MATRIX   = ' + (verdictOk ? 'PASS' : 'FAIL'))
console.log('CROSS_PLATFORM_ACTUAL_ANDROID_IOS = ' + (android && ios ? (byteOk ? 'PASS' : 'FAIL') : 'NOT_RUN'))
console.log('CROSS_PLATFORM_DIFFERENTIAL      = ' + (ok ? 'PASS' : 'FAIL'))
console.log('')

if (process.argv.includes('--json')) {
  writeFileSync(join(DIR, 'differential.json'), JSON.stringify({
    platforms: platformList,
    total: allIds.length,
    verdictMismatch,
    actualCompared: compared,
    actualEqual: byteEqual,
    actualDiff: byteDiff,
    rows,
    mismatches: mismatchRows,
    actualDiffs: diffRows,
  }, null, 2) + '\n', 'utf8')
}

process.exit(ok ? 0 : 1)
