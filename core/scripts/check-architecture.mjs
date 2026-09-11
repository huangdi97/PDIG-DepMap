#!/usr/bin/env node
/**
 * RC PHASE M — Architecture boundary check。
 * 规则（GOAL_MVP01_RC_AUDIT §M）：
 *   1. core 禁止依赖 app/UI 或平台原生目录
 *   2. domain 禁止依赖具体 repository/service/impact/crypto 实现
 *   3. crypto 协议层禁止依赖 repositories/services/impact（UI 在 core 中不存在）
 *   4. node:sqlite 只允许出现在 src/db/
 * exit 0 = PASS；exit 1 = 违规清单。
 */
import { readFileSync, readdirSync, statSync } from 'node:fs'
import { dirname, join, relative, resolve, sep } from 'node:path'

const ROOT = decodeURIComponent(new URL('..', import.meta.url).pathname).replace(
  /^\/([A-Za-z]:)/,
  '$1',
)
const SRC = join(ROOT, 'src')

function walk(dir) {
  const out = []
  for (const name of readdirSync(dir)) {
    const p = join(dir, name)
    if (statSync(p).isDirectory()) out.push(...walk(p))
    else if (name.endsWith('.ts')) out.push(p)
  }
  return out
}

const violations = []
const files = walk(SRC)

for (const file of files) {
  const rel = relative(SRC, file).replaceAll(sep, '/')
  const text = readFileSync(file, 'utf8')
  const imports = [...text.matchAll(/from\s+['"]([^'"]+)['"]/g)].map((m) => m[1])

  for (const spec of imports) {
    // 1. core -> app / platforms：解析相对导入后必须仍在 core/src 内
    if (spec.startsWith('.')) {
      const resolved = resolve(dirname(file), spec)
      const relResolved = relative(SRC, resolved).replaceAll(sep, '/')
      if (relResolved.startsWith('..')) {
        violations.push(`[core->outside] ${rel}: ${spec}`)
      }
    } else if (/(^|\/)(app|platforms)\//.test(spec)) {
      violations.push(`[core->app/native] ${rel}: ${spec}`)
    }
    // 2. domain 纯净性
    if (rel.startsWith('domain/')) {
      if (/(repositories|services|impact|crypto|db)\//.test(spec)) {
        violations.push(`[domain->impl] ${rel}: ${spec}`)
      }
    }
    // 3. crypto 不依赖业务层
    if (rel.startsWith('crypto/')) {
      if (/(repositories|services|impact)\//.test(spec)) {
        violations.push(`[crypto->business] ${rel}: ${spec}`)
      }
    }
    // 4. node:sqlite 隔离在 db/
    if (spec.includes('node:sqlite') && !rel.startsWith('db/')) {
      violations.push(`[node:sqlite leak] ${rel}: ${spec}`)
    }
  }
}

if (violations.length > 0) {
  console.error(`ARCHITECTURE VIOLATIONS (${violations.length}):`)
  for (const v of violations) console.error('  ' + v)
  process.exit(1)
}
console.log(`architecture check PASS (${files.length} files scanned)`)
