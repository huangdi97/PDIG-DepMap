// Harmony ASCII 镜像构建器
//
// 为什么需要它：hvigor 5.13.2 会校验工程路径，**拒绝含非 ASCII 的路径**
//   > hvigor ERROR: Invalid project path.
//     Detail: Please move the project to a valid path
// 本仓库位于 <repo>（含中文），直接在原位构建必然失败。
// 与 Android 侧不同 —— Android 可以在 settings.gradle.kts 里把「构建输出」重定向到 ASCII 路径，
// hvigor 没有这个开关，因此只能对整个工程做 ASCII 镜像后再构建。
//
// 用法:
//   node tools/harmony/build-ascii-mirror.mjs            # 镜像 + assembleHap
//   node tools/harmony/build-ascii-mirror.mjs --clean    # 先清空镜像目录
//
// 镜像目录可用 PDIG_HARMONY_BUILD_ROOT 覆盖（默认 C:/Users/<user>/pdig-harmony-build）。
import { existsSync, mkdirSync, rmSync, readdirSync, statSync, copyFileSync, readFileSync, writeFileSync, symlinkSync, lstatSync } from 'node:fs'
import { join, posix } from 'node:path'
import { execFileSync } from 'node:child_process'
import { homedir } from 'node:os'

const REPO = process.cwd()
const SRC = join(REPO, 'harmony')
const BUILD_ROOT = process.env.PDIG_HARMONY_BUILD_ROOT?.trim() ||
  posix.join(homedir().replace(/\\/g, '/'), 'pdig-harmony-build')
const MIRROR = join(BUILD_ROOT, 'harmony')

// 仓库不保存本机绝对路径：DevEco 安装位置必须由环境变量提供。
//   PDIG_DEVECO_HOME=<你的 DevEco Studio 安装根目录>
const DEVECO = (process.env.PDIG_DEVECO_HOME ?? '').trim()
if (!DEVECO) {
  console.error('[harmony] PDIG_DEVECO_HOME is not set.')
  console.error('[harmony] Set it to your DevEco Studio install root, e.g.')
  console.error('[harmony]   PDIG_DEVECO_HOME=<DEVECO_HOME> node tools/harmony/build-ascii-mirror.mjs')
  process.exit(1)
}
const HVIGOR_JS = join(DEVECO, 'tools/hvigor/hvigor/bin/hvigor.js')
const DEPS = {
  'hvigor': join(DEVECO, 'tools/hvigor/hvigor'),
  'hvigor-ohos-plugin': join(DEVECO, 'tools/hvigor/hvigor-ohos-plugin'),
}
const TARGETS = process.argv.slice(2).filter(a => !a.startsWith('--'))

function copyTree(from, to) {
  mkdirSync(to, { recursive: true })
  for (const e of readdirSync(from, { withFileTypes: true })) {
    const f = join(from, e.name)
    const t = join(to, e.name)
    if (e.name === 'node_modules' || e.name === 'oh_modules' || e.name === '.hvigor' ||
        e.name === 'build' || e.name === '.preview' || e.name === 'local.properties') continue
    if (e.isDirectory()) copyTree(f, t)
    else if (e.isFile()) copyFileSync(f, t)
  }
}

console.log('[harmony] repo      :', REPO)
console.log('[harmony] mirror    :', MIRROR)
console.log('[harmony] hvigor    :', HVIGOR_JS)

if (!existsSync(SRC)) { console.error('缺少 harmony/ 工程'); process.exit(1) }
if (!existsSync(HVIGOR_JS)) { console.error('未找到 DevEco hvigor:', HVIGOR_JS); process.exit(1) }

// 1. 同步源码到镜像
copyTree(SRC, MIRROR)
console.log('[harmony] source mirrored')

// 1b. hvigor-config.json5 —— 仓库内保存的是占位符 <DEVECO_HOME>，
//     这里用真实 DevEco 路径替换（镜像目录不在版本控制内，不受影响）。
{
  const cfgPath = join(MIRROR, 'hvigor', 'hvigor-config.json5')
  if (existsSync(cfgPath)) {
    const cfg = readFileSync(cfgPath, 'utf8')
    writeFileSync(cfgPath, cfg.split('<DEVECO_HOME>').join(DEVECO.replace(/\\/g, '/')))
  }
}

// 2. local.properties —— SDK 路径（镜像内需要，仓库内已有同名文件但被 gitignore）
writeFileSync(join(MIRROR, 'local.properties'),
  'sdk.dir=' + join(DEVECO, 'sdk').replace(/:/g, '\\:').replace(/\//g, '\\') + '\n')

// 3. 依赖解析：把 DevEco 内置的 @ohos/hvigor* 以 junction 挂进 node_modules
//    （等价于 ohpm/npm install file: 的效果；hvigor 的 require 由此命中）
const nm = join(MIRROR, 'node_modules/@ohos')
mkdirSync(nm, { recursive: true })
for (const [name, target] of Object.entries(DEPS)) {
  const link = join(nm, name)
  let st = null
  try { st = lstatSync(link) } catch { /* not exists */ }
  if (!st) symlinkSync(target, link, 'junction')
}

// 4. 构建
const args = ['--mode', 'module', '-p', 'product=default',
  ...(TARGETS.length ? TARGETS : ['assembleHap']), '--no-daemon']
console.log('[harmony] exec:', HVIGOR_JS, args.join(' '))

const env = {
  ...process.env,
  NODE_PATH: join(MIRROR, 'node_modules'),
  // hvigor 优先读该环境变量；缺失时报
  //   "Unable to find 'DEVECO_SDK_HOME' in the system environment path."
  DEVECO_SDK_HOME: join(DEVECO, 'sdk'),
}
try {
  const out = execFileSync(process.execPath, [HVIGOR_JS, ...args], {
    cwd: MIRROR, env, encoding: 'utf8', stdio: 'pipe', timeout: 15 * 60 * 1000,
  })
  process.stdout.write(out)
  console.log('\n[harmony] BUILD SUCCESSFUL')
} catch (e) {
  process.stdout.write(e.stdout || '')
  process.stderr.write(e.stderr || '')
  console.error('\n[harmony] BUILD FAILED')
  process.exit(1)
}
