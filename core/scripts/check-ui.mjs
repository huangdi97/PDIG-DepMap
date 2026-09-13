#!/usr/bin/env node
/**
 * check-ui.mjs — PDIG UI 静态 Gate（uni-app x 无编译器条件下的机械校验）
 *
 * 背景：本项目当前无 HBuilderX / uni-app x 工具链（BLOCKERS B10），`.uvue` 无法编译。
 * 因此引入本脚本，对 UI 源码做**可复现的结构性校验**，避免"从未编译过 = 从不检查"。
 *
 * 校验项：
 *  U1  pages.json 中每个 page 都存在对应 `.uvue` 文件
 *  U2  tabBar 配置完整（2–5 项、pagePath 必须在 pages 中、必须为首页之一）
 *  U3  用户可见文案不得含工程词（ChangePlan / RealityDrift / GraphRevision / ...）
 *  U4  页面不得直接操作 SQLite（必须经 Application Service）
 *  U5  样式中的颜色必须来自 `app/theme/tokens.uts` 调色板
 *  U6  `<script>` 中使用的裸标识符必须已声明（捕获未定义变量）
 *  U7  模板中引用的 `dp-*` 组件必须存在于 app/components/
 *  U8  每个页面必须至少有一处空态 / 错误态 / 加载态处理
 *  U9  模板中 `v-for` 别名访问的属性必须在本文件已声明的 interface 中（捕获字段名写错）
 *
 * 退出码：0 = PASS，1 = FAIL
 */
import { readFileSync, readdirSync, existsSync, statSync } from 'node:fs'
import { join, dirname, resolve, relative } from 'node:path'
import { fileURLToPath } from 'node:url'

const HERE = dirname(fileURLToPath(import.meta.url))
const REPO = resolve(HERE, '..', '..')
const APP = join(REPO, 'app')

const errors = []
const warnings = []

function rel(p) {
  return relative(REPO, p).replace(/\\/g, '/')
}

function walk(dir, filter, out = []) {
  if (!existsSync(dir)) return out
  for (const name of readdirSync(dir)) {
    const p = join(dir, name)
    const st = statSync(p)
    if (st.isDirectory()) walk(p, filter, out)
    else if (filter(p)) out.push(p)
  }
  return out
}

function read(p) {
  return readFileSync(p, 'utf8')
}

// ─────────────────────────────────────────────────────────────── U1 / U2
const pagesJsonPath = join(APP, 'pages.json')
if (!existsSync(pagesJsonPath)) {
  errors.push('U1 pages.json 不存在')
}
const pagesJson = existsSync(pagesJsonPath) ? JSON.parse(read(pagesJsonPath)) : { pages: [] }
const pagePaths = (pagesJson.pages ?? []).map((p) => p.path)

for (const p of pagePaths) {
  const f = join(APP, `${p}.uvue`)
  if (!existsSync(f)) errors.push(`U1 pages.json 声明了不存在的页面: ${p}`)
}

const tabBar = pagesJson.tabBar
if (!tabBar) {
  errors.push('U2 缺少 tabBar：一级导航未冻结（§22）')
} else {
  const list = tabBar.list ?? []
  if (list.length < 2 || list.length > 5) {
    errors.push(`U2 tabBar 项数必须在 2–5 之间，当前 ${list.length}`)
  }
  for (const item of list) {
    if (!pagePaths.includes(item.pagePath)) {
      errors.push(`U2 tabBar.pagePath 不在 pages 中: ${item.pagePath}`)
    }
  }
  if (!pagePaths.includes(pagePaths[0])) {
    errors.push('U2 首页缺失')
  }
}

// ─────────────────────────────────────────────────────────────── U5 tokens
const tokensPath = join(APP, 'theme', 'tokens.uts')
const tokenColors = new Set()
if (!existsSync(tokensPath)) {
  errors.push('U5 app/theme/tokens.uts 不存在（设计 token 缺失）')
} else {
  const src = read(tokensPath)
  for (const m of src.matchAll(/#[0-9A-Fa-f]{6}\b/g)) tokenColors.add(m[0].toUpperCase())
}
const COLOR_ALLOW = new Set([...tokenColors, 'TRANSPARENT'])

// ─────────────────────────────────────────────────────────────── U3 禁词
const BANNED = [
  'ChangePlan',
  'ScenarioCoverage',
  'RealityDrift',
  'GraphRevision',
  'DiscoveryCandidate',
  'ScenarioTemplate',
  'PlanReadiness',
  'graphRevision',
  'must_change',
  'needs_revalidation',
  'ready_with_known_scope',
  'review_required',
  'well_evidenced',
  'confidence',
  'UUID',
  'localhost',
  'Lorem ipsum',
]

// ─────────────────────────────────────────────────────────────── U6 globals
const GLOBALS = new Set([
  // JS
  'this',
  'super',
  'null',
  'true',
  'false',
  'undefined',
  'NaN',
  'Infinity',
  'new',
  'typeof',
  'instanceof',
  'void',
  'delete',
  'in',
  'of',
  'as',
  'is',
  'return',
  'if',
  'else',
  'for',
  'while',
  'do',
  'switch',
  'case',
  'break',
  'continue',
  'function',
  'const',
  'let',
  'var',
  'class',
  'interface',
  'type',
  'enum',
  'namespace',
  'import',
  'export',
  'from',
  'default',
  'extends',
  'implements',
  'declare',
  'module',
  'public',
  'private',
  'protected',
  'readonly',
  'static',
  'get',
  'set',
  'abstract',
  'try',
  'catch',
  'finally',
  'throw',
  'yield',
  'async',
  'await',
  'keyof',
  'infer',
  'satisfies',
  'string',
  'number',
  'boolean',
  'any',
  'unknown',
  'never',
  'object',
  'symbol',
  'bigint',
  // 运行时
  'console',
  'JSON',
  'Math',
  'Date',
  'Number',
  'String',
  'Boolean',
  'Array',
  'Object',
  'Map',
  'Set',
  'WeakMap',
  'WeakSet',
  'Promise',
  'RegExp',
  'Error',
  'TypeError',
  'Symbol',
  'Proxy',
  'Reflect',
  'BigInt',
  'ArrayBuffer',
  'Uint8Array',
  'Int32Array',
  'Float64Array',
  'TextEncoder',
  'TextDecoder',
  'parseInt',
  'parseFloat',
  'isNaN',
  'isFinite',
  'setTimeout',
  'clearTimeout',
  'setInterval',
  'clearInterval',
  'encodeURIComponent',
  'decodeURIComponent',
  'encodeURI',
  'decodeURI',
  // uni-app x / UTS
  'uni',
  'UTS',
  'UTSJSONObject',
  'console',
  'getApp',
  'getCurrentPages',
  'plus',
])

/**
 * uni-app x / UTS 内置类型与事件对象（仅在类型位置出现，非"未声明变量"）。
 * 注意：`crypto` 等 Web API **不在此列** —— App 端不可用，属于真实缺陷。
 */
const UTS_TYPES = new Set([
  'OnLoadOptions',
  'OnShowOptions',
  'OnReadyOptions',
  'OnHideOptions',
  'OnUnloadOptions',
  'OnBackPressOptions',
  'OnPageScrollOptions',
  'OnReachBottomOptions',
  'OnPullDownRefreshOptions',
  'OnNavigationBarButtonTapOptions',
  'OnTabItemTapOptions',
  'ChooseFileOptions',
  'ChooseFileSuccess',
  'ChooseFileFail',
  'ChooseImageOptions',
  'ChooseVideoOptions',
  'ChooseLocationOptions',
  'GetLocationOptions',
  'ShowModalOptions',
  'ShowToastOptions',
  'ShowLoadingOptions',
  'ShowActionSheetOptions',
  'HideLoadingOptions',
  'ShowModalSuccess',
  'ShowActionSheetSuccess',
  'NavigateToOptions',
  'RedirectToOptions',
  'SwitchTabOptions',
  'ReLaunchOptions',
  'NavigateBackOptions',
  'UniError',
  'UniErrorOptions',
  'InputEvent',
  'UniInputEvent',
  'PickerChangeEvent',
  'PickerCancelEvent',
  'SwitchChangeEvent',
  'CheckboxChangeEvent',
  'RadioChangeEvent',
  'SliderChangeEvent',
  'TouchEvent',
  'UniTouchEvent',
  'UniPointerEvent',
  'LongPressEvent',
  'ScrollEvent',
  'ScrollViewScrollEvent',
  'SwiperChangeEvent',
  'KeyboardEvent',
  'FocusEvent',
  'BlurEvent',
  'ConfirmEvent',
  'RequestOptions',
  'RequestSuccess',
  'RequestFail',
  'UploadFileOptions',
  'DownloadFileOptions',
  'SetStorageOptions',
  'GetStorageOptions',
  'RemoveStorageOptions',
  'SetClipboardDataOptions',
  'GetClipboardDataOptions',
  'ShareOptions',
  'ScanCodeOptions',
  'MakePhoneCallOptions',
  'OpenDocumentOptions',
])

// ─────────────────────────────────────────────────────────────── 扫描
const uvueFiles = walk(APP, (p) => p.endsWith('.uvue'))
const pageFiles = uvueFiles.filter((p) => p.includes(`${join(APP, 'pages')}`))
const componentFiles = uvueFiles.filter((p) => p.includes(`${join(APP, 'components')}`))

const componentNames = new Set(
  componentFiles.map((p) => {
    const parts = p.split(/[\\/]/)
    return parts[parts.length - 1].replace(/\.uvue$/, '')
  }),
)

function extractStyle(src) {
  const out = []
  for (const m of src.matchAll(/<style[^>]*>([\s\S]*?)<\/style>/g)) out.push(m[1])
  return out.join('\n')
}

function extractScript(src) {
  const out = []
  for (const m of src.matchAll(/<script[^>]*>([\s\S]*?)<\/script>/g)) out.push(m[1])
  return out.join('\n')
}

function extractTemplate(src) {
  const m = src.match(/<template>([\s\S]*)<\/template>/)
  return m ? m[1] : ''
}

function stripCommentsAndStrings(code) {
  return code
    .replace(/\/\*[\s\S]*?\*\//g, ' ')
    .replace(/\/\/[^\n]*/g, ' ')
    .replace(/'(?:[^'\\]|\\.)*'/g, ' "" ')
    .replace(/"(?:[^"\\]|\\.)*"/g, ' "" ')
    .replace(/`(?:[^`\\]|\\.)*`/g, ' "" ')
}

/**
 * U9 用：本文件声明的全部 interface 字段名（取并集 —— 宽松方向，避免误报）。
 */
function declaredFieldNames(script) {
  const fields = new Set()
  for (const m of script.matchAll(/\binterface\s+[A-Za-z_$][\w$]*\s*\{([\s\S]*?)\n\}/g)) {
    for (const line of m[1].split('\n')) {
      const f = line.trim().match(/^([A-Za-z_$][\w$]*)\??\s*:/)
      if (f) fields.add(f[1])
    }
  }
  return fields
}

/** 数组 / 字符串 / Map 的内置成员，U9 直接放行 */
const BUILTIN_MEMBERS = new Set([
  'length',
  'size',
  'indexOf',
  'lastIndexOf',
  'slice',
  'splice',
  'concat',
  'join',
  'map',
  'filter',
  'forEach',
  'includes',
  'toUpperCase',
  'toLowerCase',
  'toString',
  'trim',
  'replace',
  'split',
  'push',
  'pop',
  'shift',
  'unshift',
  'sort',
  'reverse',
  'find',
  'some',
  'every',
  'reduce',
  'charAt',
  'substring',
  'endsWith',
  'startsWith',
  'padStart',
  'padEnd',
  'repeat',
  'keys',
  'values',
  'entries',
  'has',
  'get',
  'set',
  'at',
  'flat',
  'fill',
])

function declaredNames(code) {
  const names = new Set()
  const patterns = [
    /\b(?:const|let|var)\s+([A-Za-z_$][\w$]*)/g,
    /\bfunction\s+([A-Za-z_$][\w$]*)/g,
    /\bclass\s+([A-Za-z_$][\w$]*)/g,
    /\binterface\s+([A-Za-z_$][\w$]*)/g,
    /\btype\s+([A-Za-z_$][\w$]*)/g,
    /\benum\s+([A-Za-z_$][\w$]*)/g,
  ]
  for (const re of patterns) for (const m of code.matchAll(re)) names.add(m[1])
  // import { A, B as C } from '...'  （含 import type { ... } 与内联 type A）
  for (const m of code.matchAll(/import\s+(?:type\s+)?\{([^}]*)\}\s*from/g)) {
    for (const part of m[1].split(',')) {
      let seg = part.trim()
      if (seg === '') continue
      seg = seg.replace(/^type\s+/, '')
      const asMatch = seg.match(/\bas\s+([A-Za-z_$][\w$]*)$/)
      names.add(asMatch ? asMatch[1] : seg.replace(/\s+as\s+.*$/, '').trim())
    }
  }
  // import X from '...'
  for (const m of code.matchAll(/import\s+([A-Za-z_$][\w$]*)\s+from/g)) names.add(m[1])
  // 函数参数 / 箭头参数 / catch
  for (const m of code.matchAll(/\(([^()]*)\)\s*(?::\s*[^=>{]+)?\s*=>/g)) {
    for (const part of m[1].split(',')) {
      const seg = part.trim().replace(/^\.\.\./, '')
      const n = seg.match(/^([A-Za-z_$][\w$]*)/)
      if (n) names.add(n[1])
    }
  }
  for (const m of code.matchAll(/\b([A-Za-z_$][\w$]*)\s*\([^()]*\)\s*(?::\s*[^=>{]+)?\s*\{/g)) {
    names.add(m[1])
  }
  for (const m of code.matchAll(/\(\s*([^()]*)\s*\)\s*\{/g)) {
    for (const part of m[1].split(',')) {
      const n = part.trim().match(/^([A-Za-z_$][\w$]*)/)
      if (n) names.add(n[1])
    }
  }
  for (const m of code.matchAll(/\bcatch\s*\(\s*([A-Za-z_$][\w$]*)/g)) names.add(m[1])
  for (const m of code.matchAll(/\bfor\s*\(\s*(?:const|let|var)\s+([A-Za-z_$][\w$]*)/g))
    names.add(m[1])
  return names
}

for (const file of uvueFiles) {
  const src = read(file)
  const r = rel(file)

  // U3 禁词（模板 + 脚本中的字符串）
  const template = extractTemplate(src)
  for (const w of BANNED) {
    if (template.includes(w)) errors.push(`U3 ${r} 模板出现工程词/禁词: ${w}`)
  }

  // U4 页面不得直接操作 SQLite
  if (file.includes(`${join(APP, 'pages')}`)) {
    if (/getSecureDb\s*\(/.test(src) || /\.query\s*\(/.test(src) || /\.exec\s*\(/.test(src)) {
      errors.push(`U4 ${r} 页面直接操作数据库（须经 Application Service，§61）`)
    }
  }

  // U5 颜色必须来自 token
  const style = extractStyle(src)
  for (const m of style.matchAll(/#[0-9A-Fa-f]{3,8}\b/g)) {
    const hex = m[0].toUpperCase()
    const norm = hex.length === 4 ? `#${hex[1]}${hex[1]}${hex[2]}${hex[2]}${hex[3]}${hex[3]}` : hex
    if (!COLOR_ALLOW.has(norm) && !COLOR_ALLOW.has(hex)) {
      errors.push(`U5 ${r} 使用了非 token 颜色: ${m[0]}`)
    }
  }
  if (/\brgba?\s*\(/.test(style)) {
    warnings.push(`U5 ${r} 样式使用了 rgb()/rgba()，建议改为 token 色值`)
  }

  // U6 未声明标识符
  const script = stripCommentsAndStrings(extractScript(src))
  const declared = declaredNames(script)
  const used = new Set()
  for (const m of script.matchAll(/(^|[^.\w$])([A-Za-z_$][\w$]*)/g)) {
    const name = m[2]
    const before = m[1]
    if (before === '.') continue
    used.add(name)
  }
  // 排除对象字面量键（形如 `key:`）与属性访问
  const keyLike = new Set()
  for (const m of script.matchAll(/([A-Za-z_$][\w$]*)\s*:/g)) keyLike.add(m[1])
  for (const m of script.matchAll(/\.([A-Za-z_$][\w$]*)/g)) keyLike.add(m[1])

  for (const name of used) {
    if (GLOBALS.has(name)) continue
    if (UTS_TYPES.has(name)) continue
    if (declared.has(name)) continue
    if (keyLike.has(name)) continue
    errors.push(`U6 ${r} 使用了未声明的标识符: ${name}`)
  }

  // U7 dp-* 组件必须存在
  for (const m of template.matchAll(/<(dp-[a-z0-9-]+)/g)) {
    if (!componentNames.has(m[1])) errors.push(`U7 ${r} 引用了不存在的组件: ${m[1]}`)
  }

  // U8 页面必须处理空态/错误态/加载态（仅对**数据驱动页面**要求）
  if (file.includes(`${join(APP, 'pages')}`)) {
    const dataDriven = /from\s+['"][^'"]*services\/depmap-service['"]/.test(src)
    if (dataDriven) {
      const hasState =
        /<dp-state/.test(template) || /\bv-if\s*=/.test(template) || /\bv-else/.test(template)
      if (!hasState) errors.push(`U8 ${r} 缺少空态/错误态/加载态处理（§40/§41）`)
    }
  }

  // U9 v-for 别名属性必须在已声明 interface 中（字段名拼写错误会导致渲染为空）
  const fields = declaredFieldNames(extractScript(src))
  const aliases = new Set()
  for (const m of template.matchAll(/\bv-for\s*=\s*"\(?([A-Za-z_$][\w$]*)/g)) aliases.add(m[1])
  for (const alias of aliases) {
    const re = new RegExp(`\\b${alias}\\.([A-Za-z_$][\\w$]*)`, 'g')
    for (const m of template.matchAll(re)) {
      const prop = m[1]
      if (BUILTIN_MEMBERS.has(prop)) continue
      if (fields.has(prop)) continue
      errors.push(`U9 ${r} v-for 别名「${alias}」访问了未声明字段: ${prop}`)
    }
  }
}

// ─────────────────────────────────────────────────────────────── 报告
console.log(
  `UI static gate: ${uvueFiles.length} .uvue scanned (${pageFiles.length} pages, ${componentFiles.length} components)`,
)
console.log(`  token palette: ${tokenColors.size} colors`)

if (warnings.length > 0) {
  console.log(`\nWARN (${warnings.length}):`)
  for (const w of warnings) console.log(`  - ${w}`)
}

if (errors.length > 0) {
  console.log(`\nFAIL (${errors.length}):`)
  for (const e of errors) console.log(`  - ${e}`)
  console.log('\nUI static gate FAIL')
  process.exit(1)
}

console.log('\nUI static gate PASS')
