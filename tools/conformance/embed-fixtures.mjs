// 把冻结的 conformance fixtures 内嵌成一个 ArkTS 模块 —— 供**主机执行面**使用。
//
// 为什么需要它（2026-09-18 实测）：
//   DevEco 的本地单元测试能在无设备主机上执行真实 ArkTS 代码，但 @ohos.file.fs
//   在那边只是个不可调用的桩（实测 `fs.readTextSync` → "is not callable"）。
//   因此主机侧拿不到 fs，fixture 文本只能作为**数据**编译进去。
//
// 边界必须说清楚，否则这会像一个"绕过运行时"的后门：
//   * 被测**代码**是真实 ArkTS（ConformanceRunner 由真 ArkTS 编译器产出），
//     不是另写一份 Node 实现 —— 这正是 §10 与 check-relations-semantics.mjs 的区别。
//   * 被喂**数据**是冻结的 fixtures 原文，逐字节内嵌，且由本脚本的 --check
//     模式保证内嵌副本与磁盘上的冻结原件一致（漂移会被门禁抓住）。
//   * 这不产生设备运行时结论。@ohos 依赖的 28 个用例在主机上依然是
//     BLOCKED_BY_RUNTIME —— 主机执行面**不能**替代设备执行面。
//
// 用法：
//   node tools/conformance/embed-fixtures.mjs           # 生成
//   node tools/conformance/embed-fixtures.mjs --check   # 校验内嵌副本未漂移
import { readFileSync, writeFileSync, existsSync, mkdirSync } from 'node:fs'
import { join, dirname } from 'node:path'

const REPO = process.cwd()
const MANIFEST = join(REPO, 'conformance', 'CONFORMANCE_MANIFEST.json')
const OUT = join(REPO, 'harmony', 'entry', 'src', 'test', 'fixtures', 'FixtureBundle.ets')
const CHECK = process.argv.includes('--check')

if (!existsSync(MANIFEST)) {
  console.error('[embed-fixtures] manifest 不存在:', MANIFEST)
  process.exit(1)
}

const manifestText = readFileSync(MANIFEST, 'utf8')
const manifest = JSON.parse(manifestText)
const fixtures = manifest.fixtures ?? []
if (fixtures.length === 0) {
  console.error('[embed-fixtures] manifest 里没有 fixtures')
  process.exit(1)
}

const paths = []
const texts = []
const hashes = []
const missing = []
for (const f of fixtures) {
  // manifest 里的 path 是相对**仓库根**的（形如 fixtures/impact/xxx.json）。
  const abs = join(REPO, f.path)
  if (!existsSync(abs)) {
    missing.push(f.path)
    continue
  }
  paths.push(f.path)
  texts.push(readFileSync(abs, 'utf8'))
  hashes.push(f.sha256 ?? '')
}
if (missing.length > 0) {
  console.error('[embed-fixtures] 有 ' + missing.length + ' 个 fixture 文件缺失:')
  for (const m of missing.slice(0, 5)) console.error('  ' + m)
  process.exit(1)
}

// JSON.stringify 产出的是**无损** JS 字符串字面量（含 \uXXXX 转义）：
// 引擎读回来后与原文本逐字符相同。这里刻意不做任何美化/改写，
// 否则比对基准就被我们污染了。
const lit = (s) => JSON.stringify(s)

const lines = []
lines.push('// ⚠ 本文件由 tools/conformance/embed-fixtures.mjs 自动生成，请勿手改。')
lines.push('//')
lines.push('// 用途：把冻结的 conformance fixtures 作为**数据**内嵌，供主机本地单元测试')
lines.push('// （无设备、无 @ohos.file.fs）驱动真实 ArkTS 的 ConformanceRunner。')
lines.push('//')
lines.push('// 为什么这不是"绕过运行时"：')
lines.push('//   * 被测代码是真实 ArkTS（runner 由真 ArkTS 编译器产出），不是 Node 复刻实现。')
lines.push('//   * 内嵌文本来自冻结 fixtures，逐字节一致；由 `embed-fixtures.mjs --check`')
lines.push('//     作为门禁保证不漂移。任一侧改动而未重跑生成器 → 门禁失败。')
lines.push('//   * @ohos 依赖的用例在主机上仍记 BLOCKED_BY_RUNTIME —— 主机执行面')
lines.push('//     不替代设备执行面，也**不**产生 91/91 的结论。')
lines.push('//')
lines.push('// fixture 数: ' + fixtures.length + ' · 生成自 manifest: conformance/CONFORMANCE_MANIFEST.json')
lines.push('')
lines.push('export const FIXTURE_COUNT: number = ' + fixtures.length + ';')
lines.push('')
lines.push('export const MANIFEST_TEXT: string = ' + lit(manifestText) + ';')
lines.push('')
lines.push('export const FIXTURE_PATHS: string[] = [')
for (const p of paths) lines.push('  ' + lit(p) + ',')
lines.push('];')
lines.push('')
lines.push('/** 与 FIXTURE_PATHS 一一对应的原文。仅供 runner 作为输入数据使用。 */')
lines.push('export const FIXTURE_TEXTS: string[] = [')
for (const t of texts) lines.push('  ' + lit(t) + ',')
lines.push('];')
lines.push('')
lines.push('/** manifest 里声明的 sha256，便于宿主侧门禁核对（ArkTS 侧不实现哈希）。 */')
lines.push('export const FIXTURE_SHA256: string[] = [')
for (const h of hashes) lines.push('  ' + lit(h) + ',')
lines.push('];')
lines.push('')

const out = lines.join('\n')

if (CHECK) {
  if (!existsSync(OUT)) {
    console.error('[embed-fixtures] 内嵌文件不存在，需先生成:', OUT)
    process.exit(1)
  }
  const cur = readFileSync(OUT, 'utf8')
  if (cur !== out) {
    console.error('[embed-fixtures] DRIFT: 内嵌副本与冻结 fixtures 不一致')
    console.error('  重跑: node tools/conformance/embed-fixtures.mjs')
    process.exit(1)
  }
  console.log('[embed-fixtures] OK (' + fixtures.length + ' fixtures, 内嵌副本无漂移)')
  process.exit(0)
}

mkdirSync(dirname(OUT), { recursive: true })
writeFileSync(OUT, out)
console.log('[embed-fixtures] wrote ' + OUT)
console.log('[embed-fixtures] fixtures=' + fixtures.length + ' bytes=' + out.length)
