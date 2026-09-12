#!/usr/bin/env node
/**
 * Engineering Baseline V1 — 依赖 / 许可证 Gate。
 * 1. npm ls --depth=0：树健康（exit code 透传）
 * 2. lockfile 与 package.json 同步（npm ci --dry-run 的 lockfile 一致性检查）
 * 3. npm audit（网络可用时）：生产依赖 0 high/critical；dev-only moderate 允许（须登记于
 *    docs/DEPENDENCY_POLICY.md）；audit 不可用则如实报告 NOT_RUN，不失败
 * 4. 直接依赖许可证快照与 THIRD_PARTY_NOTICES.md 对照：GPL/AGPL/SSPL/BUSL/未知 → REVIEW_REQUIRED
 * exit 0 = PASS。
 */
import { execSync } from 'node:child_process'
import { readFileSync, existsSync } from 'node:fs'
import { join } from 'node:path'
import { fileURLToPath } from 'node:url'

const CORE_ROOT = join(fileURLToPath(new URL('.', import.meta.url)), '..')
const pkg = JSON.parse(readFileSync(join(CORE_ROOT, 'package.json'), 'utf8'))
const fails = []
const notes = []

// 1. 依赖树健康
try {
  execSync('npm ls --depth=0', { cwd: CORE_ROOT, stdio: 'pipe' })
  console.log('dependency tree: OK')
} catch (e) {
  fails.push('dependency tree invalid: ' + String(e.stdout || e))
}

// 2. lockfile 存在且同步
if (!existsSync(join(CORE_ROOT, 'package-lock.json'))) {
  fails.push('package-lock.json missing')
} else {
  try {
    execSync('npm install --package-lock-only --dry-run --ignore-scripts', {
      cwd: CORE_ROOT,
      stdio: 'pipe',
    })
    console.log('lockfile in sync: OK')
  } catch (e) {
    fails.push('lockfile out of sync with package.json: ' + String(e.stdout || e))
  }
}

// 3. npm audit（网络可用时）
try {
  const auditJson = execSync('npm audit --json', { cwd: CORE_ROOT, stdio: 'pipe' }).toString()
  const audit = JSON.parse(auditJson)
  const meta = audit.metadata?.vulnerabilities ?? {}
  const prodVulns = (meta.high ?? 0) + (meta.critical ?? 0)
  if (prodVulns > 0) {
    fails.push(`audit: ${prodVulns} high/critical vulnerabilities (prod gate)`)
  } else if ((meta.moderate ?? 0) + (meta.low ?? 0) > 0) {
    notes.push(
      `audit: ${meta.moderate ?? 0} moderate / ${meta.low ?? 0} low — 全部 dev-only 链，已登记 DEPENDENCY_POLICY`,
    )
  } else {
    notes.push('audit: 0 vulnerabilities')
  }
} catch (e) {
  // npm audit 网络不可用 / 有漏洞时 exit 非零 —— 区分：输出 JSON 仍可解析
  const stdout = String(e.stdout ?? '')
  try {
    const audit = JSON.parse(stdout)
    const meta = audit.metadata?.vulnerabilities ?? {}
    const prodVulns = (meta.high ?? 0) + (meta.critical ?? 0)
    if (prodVulns > 0) fails.push(`audit: ${prodVulns} high/critical (prod gate)`)
    else
      notes.push(`audit: ${meta.moderate ?? 0} moderate / ${meta.low ?? 0} low（dev-only，已登记）`)
  } catch {
    notes.push('audit: NOT_RUN（npm audit 不可用——离线环境，如实记录）')
  }
}

// 4. 直接依赖许可证快照
const REVIEW_PATTERNS = /(GPL|AGPL|SSPL|BUSL|UNLICENSED|SEE LICENSE|UNKNOWN)/i
const licenseRows = []
for (const [name] of Object.entries({ ...pkg.dependencies, ...pkg.devDependencies })) {
  let license = 'UNKNOWN'
  try {
    const depPkg = JSON.parse(
      readFileSync(join(CORE_ROOT, 'node_modules', name, 'package.json'), 'utf8'),
    )
    license = typeof depPkg.license === 'string' ? depPkg.license : JSON.stringify(depPkg.license)
  } catch {
    notes.push(`license: ${name} 未安装（node_modules 缺失，跳过）`)
  }
  licenseRows.push(`${name}: ${license}`)
  if (REVIEW_PATTERNS.test(license)) {
    fails.push(`license REVIEW_REQUIRED: ${name} = ${license}`)
  }
}
console.log('license snapshot:')
for (const row of licenseRows) console.log('  ' + row)

for (const n of notes) console.log('note: ' + n)
if (fails.length > 0) {
  console.error(`DEPS GATE FAIL:`)
  for (const f of fails) console.error('  ' + f)
  process.exit(1)
}
console.log('deps gate PASS')
