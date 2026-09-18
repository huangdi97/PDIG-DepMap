// 第三方源码完整性 Gate。
//
// 目的：让"vendored 源码是否被悄悄改动"变成可自动发现的事实，而不是靠信任。
// 每一条都拿 third_party/argon2/VENDOR.json 里登记的 SHA-256 与磁盘上的
// 实际字节对账。任何不一致都直接失败 —— 包括**未登记的新文件**。
//
// 用法：
//   node tools/harmony/check-third-party-hashes.mjs
//
// 退出码：0 = 全部一致；1 = 有漂移或有未登记文件 / 登记文件缺失。
import { createHash } from 'node:crypto'
import { readFileSync, readdirSync, statSync, existsSync } from 'node:fs'
import { join, relative, posix } from 'node:path'

const REPO = process.cwd()
const ARGON2 = join(REPO, 'third_party', 'argon2')
const MANIFEST = join(ARGON2, 'VENDOR.json')

if (!existsSync(MANIFEST)) {
  console.error('[third-party] manifest not found:', MANIFEST)
  process.exit(1)
}

const manifest = JSON.parse(readFileSync(MANIFEST, 'utf8'))

function sha256(path) {
  return createHash('sha256').update(readFileSync(path)).digest('hex')
}

function walk(dir, base = dir, out = []) {
  for (const entry of readdirSync(dir, { withFileTypes: true })) {
    const full = join(dir, entry.name)
    if (entry.isDirectory()) walk(full, base, out)
    else if (entry.isFile()) out.push(relative(base, full).split('\\').join('/'))
  }
  return out
}

let failures = 0
const declared = new Set()

console.log(`[third-party] ${manifest.component} @ ${manifest.upstream.tag} (${manifest.upstream.commit})`)

for (const f of manifest.vendored_files) {
  declared.add(f.path)
  const full = join(ARGON2, f.path)
  if (!existsSync(full)) {
    console.log(`  MISSING    ${f.path}`)
    failures++
    continue
  }
  const actual = sha256(full)
  const bytes = statSync(full).size
  const hashOk = actual === f.sha256.toLowerCase()
  const sizeOk = bytes === f.bytes
  if (hashOk && sizeOk) {
    console.log(`  OK         ${f.path}`)
  } else {
    console.log(`  DRIFT      ${f.path}`)
    if (!hashOk) console.log(`             sha256 declared=${f.sha256} actual=${actual}`)
    if (!sizeOk) console.log(`             bytes declared=${f.bytes} actual=${bytes}`)
    failures++
  }
}

// 未登记文件也要报 —— 否则新加的源码会绕过完整性对账。
const onDisk = walk(ARGON2).filter((p) => p !== 'VENDOR.json')
const undeclared = onDisk.filter((p) => !declared.has(p))
if (undeclared.length > 0) {
  console.log('\n  UNDECLARED FILES (must be added to VENDOR.json vendored_files or removed):')
  for (const p of undeclared) console.log(`    ${p}`)
  failures += undeclared.length
}

const declaredCount = manifest.vendored_files.length
console.log(`\n  declared=${declaredCount} onDisk=${onDisk.length} undeclared=${undeclared.length}`)
console.log(`  local_modifications declared as: ${manifest.local_modifications.count}`)
console.log('\nTHIRD_PARTY_INTEGRITY=' + (failures === 0 ? 'PASS' : 'FAIL'))
process.exit(failures === 0 ? 0 : 1)
