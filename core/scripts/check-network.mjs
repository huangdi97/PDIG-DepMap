#!/usr/bin/env node
/**
 * Engineering Baseline V1 — Network Zero Gate。
 * MVP 原则：业务代码网络调用 = 0（NO BACKEND / NO ANALYTICS / NO TELEMETRY / NO SYNC）。
 * 扫描业务源码（core/src + app + platforms 源文件）中的网络原语。
 * 命中即 exit 1（新增业务网络调用必须显式评审并更新 NETWORK_AUDIT 后才能放行）。
 * exit 0 = PASS。
 */
import { readFileSync, readdirSync, statSync } from 'node:fs'
import { join, relative, sep } from 'node:path'
import { fileURLToPath } from 'node:url'

const REPO_ROOT = join(fileURLToPath(new URL('.', import.meta.url)), '..', '..')

// 业务源码目录（相对仓库根）。node_modules / fixtures / 文档不入扫描。
const SCAN_DIRS = ['core/src', 'app', 'platforms']
const EXTS = new Set(['.ts', '.uts', '.uvue', '.kt', '.kts', '.swift', '.ets', '.json5'])

// 网络原语模式（按语言通用的字符串匹配，宁多勿漏）
const PATTERNS = [
  { re: /\bfetch\s*\(/, label: 'fetch()' },
  { re: /\baxios\b/, label: 'axios' },
  { re: /\buni\.request\b/, label: 'uni.request' },
  { re: /\bXMLHttpRequest\b/, label: 'XMLHttpRequest' },
  { re: /\bURLSession\b/, label: 'URLSession' },
  { re: /\bOkHttp\b/, label: 'OkHttp' },
  { re: /\bHttpURLConnection\b/, label: 'HttpURLConnection' },
  { re: /\bnode:(https?|http2|net|dgram)\b/, label: 'node network module' },
  { re: /\brequire\(\s*['"](https?|http2|net|dgram)['"]\s*\)/, label: 'require network module' },
  { re: /\bfrom\s+['"](https?|http2|net|dgram)['"]/, label: 'import network module' },
  { re: /\bnew\s+WebSocket\b/, label: 'WebSocket' },
  { re: /\bEventSource\b/, label: 'EventSource' },
  { re: /\@ohos\.net\.http\b/, label: 'ArkTS @ohos.net.http' },
  { re: /\bremote Networking|request\(.*url\s*:/i, label: 'generic http request' },
]

function walk(dir, out = []) {
  let entries
  try {
    entries = readdirSync(dir)
  } catch {
    return out
  }
  for (const name of entries) {
    const p = join(dir, name)
    let st
    try {
      st = statSync(p)
    } catch {
      continue
    }
    if (st.isDirectory()) {
      if (name === 'node_modules' || name === '.git' || name === 'fixtures' || name === 'unpackage')
        continue
      walk(p, out)
    } else if (EXTS.has(name.slice(name.lastIndexOf('.')))) {
      out.push(p)
    }
  }
  return out
}

const files = SCAN_DIRS.flatMap((d) => walk(join(REPO_ROOT, d)))
const findings = []

for (const file of files) {
  const rel = relative(REPO_ROOT, file).replaceAll(sep, '/')
  const lines = readFileSync(file, 'utf8').split(/\r?\n/)
  lines.forEach((line, i) => {
    // 跳过纯注释行（规范禁止靠注释豁免真实调用，但注释中的示例词不算违规）
    const trimmed = line.trim()
    if (trimmed.startsWith('//') || trimmed.startsWith('*') || trimmed.startsWith('/*')) return
    for (const { re, label } of PATTERNS) {
      if (re.test(line)) {
        findings.push(`${rel}:${i + 1}  [${label}]  ${trimmed.slice(0, 120)}`)
        break
      }
    }
  })
}

if (findings.length > 0) {
  console.error(`NETWORK GATE FAIL (${findings.length} findings):`)
  for (const f of findings) console.error('  ' + f)
  process.exit(1)
}
console.log(`network gate PASS (${files.length} business source files, 0 network primitives)`)
