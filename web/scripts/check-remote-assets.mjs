#!/usr/bin/env node
/**
 * check:remote-assets —— 生产 runtime 不得有远程依赖（§67 / §110）。
 *
 * 只判定“真正会发起网络请求”的引用：
 * - HTML 属性 src= / href= 指向 http(s)
 * - CSS url(http...)
 * - fetch / importScripts / 动态 import 指向 http(s)
 *
 * 不判定注释或错误消息里的文档链接（例如 React 错误页 URL）——它们不会被请求。
 */
import { readdirSync, readFileSync } from 'node:fs'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const HERE = dirname(fileURLToPath(import.meta.url))
const OUT = resolve(HERE, '../../website/app')

function walk(dir) {
  const out = []
  for (const entry of readdirSync(dir, { withFileTypes: true })) {
    const full = join(dir, entry.name)
    if (entry.isDirectory()) out.push(...walk(full))
    else out.push(full)
  }
  return out
}

const RULES = [
  { name: 'html-src', re: /\b(?:src|href)\s*=\s*["']https?:\/\/[^"']+/gi },
  { name: 'css-url', re: /url\(\s*["']?https?:\/\/[^)"']+/gi },
  { name: 'fetch', re: /\bfetch\(\s*["'`]https?:\/\//gi },
  { name: 'import-scripts', re: /\bimportScripts\(\s*["'`]https?:\/\//gi },
  { name: 'dynamic-import', re: /\bimport\(\s*["'`]https?:\/\//gi },
  { name: 'sendBeacon', re: /sendBeacon\(/gi },
  { name: 'xhr-open', re: /\.open\(\s*["'](?:GET|POST|PUT|DELETE)["']\s*,\s*["']https?:\/\//gi },
]

const files = walk(OUT).filter((file) => /\.(html|js|css|webmanifest)$/.test(file))
const failures = []

for (const file of files) {
  const text = readFileSync(file, 'utf8')
  for (const rule of RULES) {
    const matches = text.match(rule.re)
    if (!matches) continue
    for (const match of matches.slice(0, 3)) {
      failures.push(`${file.replace(/\\/g, '/')}: [${rule.name}] ${match.slice(0, 90)}`)
    }
  }
}

if (failures.length > 0) {
  console.error('REMOTE ASSET GATE: FAIL')
  for (const failure of failures.slice(0, 20)) console.error(`  - ${failure}`)
  process.exit(1)
}
console.log(
  `REMOTE ASSET GATE: PASS (${files.length} built files scanned, 0 remote runtime dependencies)`,
)
