#!/usr/bin/env node
/**
 * check:web-generated —— 部署产物必须与源码同步（§8）。
 *
 * 做法：重新构建一次，然后用 git 判断 website/app/ 是否出现差异。
 * 有差异 = 有人手改了产物，或产物未随源码更新。
 */
import { spawnSync } from 'node:child_process'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const HERE = dirname(fileURLToPath(import.meta.url))
const ROOT = resolve(HERE, '..')

const build = spawnSync(process.execPath, [resolve(ROOT, 'scripts/build-web.mjs')], {
  cwd: ROOT,
  stdio: 'inherit',
})
if (build.status !== 0) process.exit(build.status ?? 1)

const diff = spawnSync('git', ['diff', '--stat', '--', 'website/app'], {
  cwd: resolve(ROOT, '..'),
  encoding: 'utf8',
})
if (diff.status !== 0) {
  console.error('git diff failed')
  process.exit(1)
}
if (diff.stdout.trim() !== '') {
  console.error('WEB GENERATED GATE: FAIL — website/app 与源码不同步或被手改')
  console.error(diff.stdout)
  process.exit(1)
}
console.log('WEB GENERATED GATE: PASS (website/app 与源码构建一致)')
