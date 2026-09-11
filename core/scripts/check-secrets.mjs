#!/usr/bin/env node
/**
 * RC PHASE O — Secret scan（regex + filename fallback，无 gitleaks 依赖）。
 * 扫描对象：git tracked + working tree 未跟踪文件（不含 node_modules/二进制）。
 * allowlist：golden 测试口令 `depmap-test`（公开测试向量，非生产秘密）、
 * 测试用 fpSecret 字面量（secret/fuzz-secret/perf-secret/test-fp-secret）。
 * 目标：production secret findings = 0。
 */
import { execSync } from 'node:child_process'
import { readFileSync, statSync } from 'node:fs'
import { join } from 'node:path'

const ROOT = decodeURIComponent(new URL('../..', import.meta.url).pathname).replace(
  /^\/([A-Za-z]:)/,
  '$1',
)

let files = []
try {
  files = execSync('git ls-files --cached --others --exclude-standard', {
    cwd: ROOT,
    encoding: 'utf8',
  })
    .split('\n')
    .filter(Boolean)
} catch (e) {
  console.error('git ls-files failed:', e.message)
  process.exit(2)
}

const BINARY_EXT =
  /\.(png|jpe?g|ico|woff2?|zip|gz|keystore|jks|p12|p8|cer|der|mobileprovision|so|dll|exe)$/i
const SECRET_FILE_PATTERNS = [
  [/\.env$/i, 'env file'],
  [/\.env\.\w+/i, 'env variant file'],
  [/(password|secret|token|credential)s?\.(txt|json|ya?ml|cfg|ini)$/i, 'secret-named file'],
  [/\.(keystore|jks|p12|p8|mobileprovision)$/i, 'signing material'],
  // 真实账单只可能是数据文件（CLI 工具脚本不算）
  [/(real[_-]?bill|真实账单).*(\.csv|\.ofx|\.pdf|\.xlsx?)$/i, 'possible real bill'],
]

const CONTENT_PATTERNS = [
  [/-----BEGIN (RSA |EC |OPENSSH |PGP )?PRIVATE KEY-----/, 'private key block'],
  [/AKIA[0-9A-Z]{16}/, 'AWS access key'],
  [/ghp_[A-Za-z0-9]{30,}/, 'GitHub token'],
  [/xox[bpars]-[A-Za-z0-9-]{10,}/, 'Slack token'],
  [/(api[_-]?key|apikey)\s*[:=]\s*['"][A-Za-z0-9_\-]{20,}['"]/i, 'api key literal'],
  [/(aws_secret_access_key|private[_-]?token)\s*[:=]/i, 'cloud credential field'],
  [/mysql:\/\/[^\s]+:[^\s]+@/, 'DB connection string with password'],
]

// 已知非秘密（测试向量/测试夹具），允许出现：
const ALLOWLIST = [
  /depmap-test/,
  /['"](secret|fuzz-secret|perf-secret|test-fp-secret|fixed-secret|s)['"]/,
]

const findings = []
for (const rel of files) {
  const abs = join(ROOT, rel)
  let stat
  try {
    stat = statSync(abs)
  } catch {
    continue
  }
  if (!stat.isFile()) continue
  for (const [re, label] of SECRET_FILE_PATTERNS) {
    if (re.test(rel)) findings.push(`[file:${label}] ${rel}`)
  }
  if (BINARY_EXT.test(rel)) continue
  let text
  try {
    text = readFileSync(abs, 'utf8')
  } catch {
    continue
  }
  const lines = text.split(/\r?\n/)
  lines.forEach((line, i) => {
    for (const [re, label] of CONTENT_PATTERNS) {
      if (re.test(line) && !ALLOWLIST.some((a) => a.test(line))) {
        findings.push(`[content:${label}] ${rel}:${i + 1}`)
      }
    }
  })
}

if (findings.length > 0) {
  console.error(`SECRET FINDINGS (${findings.length}):`)
  for (const f of findings) console.error('  ' + f)
  process.exit(1)
}
console.log(`secret scan PASS (${files.length} files scanned, 0 production secrets)`)
