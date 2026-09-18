// 诊断工具：打印 Harmony ets 目录内每个模块的 import 出边，以及从入口出发的可达集。
// 用途：当 check-compiled-reachability.mjs 报 FAIL 时，用它定位断边/悬空模块。
//
// 用法： node tools/harmony/dump-import-graph.mjs
import { readdirSync, readFileSync } from 'node:fs'
import { join, relative, posix } from 'node:path'

const REPO = process.cwd()
const ETS_ROOT = join(REPO, 'harmony', 'entry', 'src', 'main', 'ets')
const ENTRY_ROOTS = ['entryability/EntryAbility.ets', 'pages/Index.ets']

function walk(dir, out = []) {
  for (const e of readdirSync(dir, { withFileTypes: true })) {
    const full = join(dir, e.name)
    if (e.isDirectory()) walk(full, out)
    else if (e.isFile() && e.name.endsWith('.ets')) out.push(full)
  }
  return out
}

function importsOf(src) {
  const out = []
  const re = /(?:import|export)\s[^'"]*?from\s*['"]([^'"]+)['"]/g
  let m
  while ((m = re.exec(src)) !== null) if (m[1].startsWith('.')) out.push(m[1])
  const dyn = /import\s*\(\s*['"]([^'"]+)['"]\s*\)/g
  while ((m = dyn.exec(src)) !== null) if (m[1].startsWith('.')) out.push(m[1])
  return out
}

const files = walk(ETS_ROOT)
const modules = files.map((f) => relative(ETS_ROOT, f).split('\\').join('/').replace(/\.ets$/, ''))
const moduleSet = new Set(modules)

const graph = new Map()
for (const mod of modules) {
  const src = readFileSync(join(ETS_ROOT, mod + '.ets'), 'utf8')
  const deps = importsOf(src).map((spec) => {
    const t = posix.normalize(posix.join(posix.dirname(mod), spec)).replace(/\.(ets|ts|js)$/, '')
    return moduleSet.has(t) ? t : t + ' **UNRESOLVED**'
  })
  graph.set(mod, deps)
}

console.log('=== import out-edges ===')
for (const mod of modules) {
  const deps = graph.get(mod)
  console.log(mod.padEnd(30), '->', deps.length ? deps.join(', ') : '(none)')
}

const reached = new Set()
const queue = []
for (const root of ENTRY_ROOTS) {
  const id = root.replace(/\.ets$/, '')
  if (moduleSet.has(id) && !reached.has(id)) { reached.add(id); queue.push(id) }
}
while (queue.length) {
  const cur = queue.shift()
  for (const d of graph.get(cur) ?? []) {
    if (d.endsWith('**UNRESOLVED**')) continue
    if (!reached.has(d)) { reached.add(d); queue.push(d) }
  }
}

console.log('')
console.log('=== reachability from entry roots ===')
for (const mod of modules) console.log((reached.has(mod) ? 'REACHABLE  ' : 'ORPHAN     ') + mod)

const orphans = modules.filter((m) => !reached.has(m))
console.log('')
console.log('orphan count:', orphans.length, orphans.length ? '-> ' + orphans.join(', ') : '')

const unresolved = [...graph.entries()].flatMap(([m, ds]) =>
  ds.filter((d) => d.endsWith('**UNRESOLVED**')).map((d) => `${m} -> ${d}`))
if (unresolved.length) {
  console.log('')
  console.log('=== unresolved specifiers (source or path bug) ===')
  for (const u of unresolved) console.log(' ', u)
}
