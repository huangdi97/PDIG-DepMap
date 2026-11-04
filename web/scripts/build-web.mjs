#!/usr/bin/env node
/**
 * build:web —— 从 web/ 源码重建部署产物 website/app/，并生成 SW 预缓存清单。
 *
 * website/app/ 是构建产物，不是源码真源：不要手改生成后的 app.js/app.css。
 */
import { spawnSync } from 'node:child_process'
import { createHash } from 'node:crypto'
import { readdirSync, readFileSync, statSync, writeFileSync } from 'node:fs'
import { dirname, join, relative, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const HERE = dirname(fileURLToPath(import.meta.url))
const ROOT = resolve(HERE, '..')
const OUT = resolve(ROOT, '../website/app')

const build = spawnSync(
  process.execPath,
  [resolve(ROOT, 'node_modules/vite/bin/vite.js'), 'build'],
  {
    cwd: ROOT,
    stdio: 'inherit',
  },
)
if (build.status !== 0) process.exit(build.status ?? 1)

function walk(dir) {
  const out = []
  for (const entry of readdirSync(dir, { withFileTypes: true })) {
    const full = join(dir, entry.name)
    if (entry.isDirectory()) out.push(...walk(full))
    else out.push(full)
  }
  return out
}

const files = walk(OUT)
  .map((file) => relative(OUT, file).replace(/\\/g, '/'))
  .filter((file) => file !== 'sw-manifest.json' && file !== 'sw.js')
  .sort()

const TEXT_EXT = /\.(html|svg|webmanifest|json|js|css|txt)$/

// 跨平台确定性：把文本产物统一为 LF。
// 否则 Windows 工作区（autocrlf）会写出 CRLF，而 CI（Linux）是 LF，
// BUILD_SHA256SUMS 就会因 EOL 不同而不同，导致“产物与源码同步”门禁假红/假绿。
for (const file of files) {
  if (!TEXT_EXT.test(file)) continue
  const target = join(OUT, file)
  const raw = readFileSync(target, 'utf8')
  if (raw.includes('\r\n')) writeFileSync(target, raw.replace(/\r\n/g, '\n'), 'utf8')
}

const manifest = files.map((file) => `./${file}`)
writeFileSync(join(OUT, 'sw-manifest.json'), `${JSON.stringify(manifest, null, 2)}\n`, 'utf8')

const hashes = files.map((file) => {
  const hash = createHash('sha256')
    .update(readFileSync(join(OUT, file)))
    .digest('hex')
  return `${hash}  ${file}`
})
writeFileSync(join(OUT, 'BUILD_SHA256SUMS.txt'), `${hashes.join('\n')}\n`, 'utf8')

const total = files.reduce((sum, file) => sum + statSync(join(OUT, file)).size, 0)
console.log(`\n[build:web] ${files.length} files, ${(total / 1024).toFixed(1)} KiB → website/app/`)
console.log('[build:web] wrote sw-manifest.json + BUILD_SHA256SUMS.txt')
