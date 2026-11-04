#!/usr/bin/env node
/**
 * check:web-boundary —— UI 不得自己计算 Truth（§9）。
 *
 * 分层定义（与 src 目录一致）：
 *   UI 层            : app/ components/ features/ privacy/   → 不得 import @core，不得出现 SQL/Core 计算符号
 *   Runtime/Adapter : runtime/ crypto/ storage/             → 可以 import @core（JCS、类型），但不得写 SQL
 *
 * 另外全仓禁止 dangerouslySetInnerHTML（用户输入一律按文本渲染）。
 */
import { readdirSync, readFileSync } from 'node:fs'
import { join, relative, resolve, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'

const HERE = dirname(fileURLToPath(import.meta.url))
const SRC = resolve(HERE, '../src')

function walk(dir) {
  const out = []
  for (const entry of readdirSync(dir, { withFileTypes: true })) {
    const full = join(dir, entry.name)
    if (entry.isDirectory()) out.push(...walk(full))
    else if (/\.(ts|tsx)$/.test(entry.name)) out.push(full)
  }
  return out
}

const files = walk(SRC)
const failures = []
const UI_DIRS = ['app', 'components', 'features', 'privacy']
const SQL_PATTERNS = [
  /\bSELECT\s+.+\s+FROM\b/i,
  /\bINSERT\s+INTO\b/i,
  /\bUPDATE\s+\w+\s+SET\b/i,
  /\bDELETE\s+FROM\b/i,
  /\bCREATE\s+TABLE\b/i,
]
const CORE_SYMBOLS = [
  'NodeRepository',
  'DependencyRepository',
  'ChangePlanRepository',
  'CoreAuthority',
  'simulateScenario',
  'generateInfrastructureFindings',
  'rebasePlan',
  'computePlanReadiness',
]

for (const file of files) {
  const rel = relative(SRC, file).replace(/\\/g, '/')
  const text = readFileSync(file, 'utf8')
  const isUi = UI_DIRS.some((dir) => rel.startsWith(`${dir}/`))
  const importsCore = /from\s+['"]@core\//.test(text)

  if (isUi) {
    if (importsCore) {
      failures.push(`${rel}: UI 层不得 import Core（Truth 只能来自 Worker 的 projection）`)
    }
    for (const pattern of SQL_PATTERNS) {
      if (pattern.test(text)) failures.push(`${rel}: UI 层出现 SQL 语句 ${pattern}`)
    }
    for (const name of CORE_SYMBOLS) {
      if (text.includes(name)) failures.push(`${rel}: UI 层出现 Core 计算符号 ${name}`)
    }
  } else {
    for (const pattern of SQL_PATTERNS) {
      if (pattern.test(text) && rel.startsWith('runtime/worker/') === false) {
        failures.push(`${rel}: 非 Worker 的 runtime 层不得出现 SQL 语句 ${pattern}`)
      }
    }
  }
  if (text.includes('dangerouslySetInnerHTML')) {
    failures.push(`${rel}: 禁止 dangerouslySetInnerHTML（用户输入一律按文本渲染）`)
  }
}

if (failures.length > 0) {
  console.error('WEB BOUNDARY GATE: FAIL')
  for (const failure of failures) console.error(`  - ${failure}`)
  process.exit(1)
}
console.log(`WEB BOUNDARY GATE: PASS (${files.length} files scanned; UI 0 Core imports, 0 SQL)`)
