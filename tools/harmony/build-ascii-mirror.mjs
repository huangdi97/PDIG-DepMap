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
// 镜像目录可用 PDIG_HARMONY_BUILD_ROOT 覆盖（默认 <用户主目录>/pdig-harmony-build，
// 由 os.homedir() 推导；此处刻意不写任何真实机器路径）。
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
const ARGV = process.argv.slice(2)
// --clean 曾经只在用法注释里存在、代码里从未实现，
// 导致「想做干净构建」的人拿到的是一堆 UP-TO-DATE 的增量结果（假绿）。
// 这里补上真实实现：构建前先删除镜像目录。
const CLEAN = ARGV.includes('--clean')
const TARGETS = ARGV.filter(a => !a.startsWith('--'))

function copyTree(from, to) {
  mkdirSync(to, { recursive: true })
  for (const e of readdirSync(from, { withFileTypes: true })) {
    const f = join(from, e.name)
    const t = join(to, e.name)
    if (e.name === 'node_modules' || e.name === 'oh_modules' || e.name === '.hvigor' ||
        e.name === 'build' || e.name === '.preview' || e.name === 'local.properties') continue
    // Dirent.isDirectory()/isFile() 对符号链接**都返回 false**，直接 if/else 会把链接
    // 静默丢弃。ohpm 恰好在 oh_modules 内用符号链接组织包
    // （entry/oh_modules/@ohos/hypium -> oh_modules/.ohpm/@ohos+hypium@1.0.24/...），
    // 因此早期版本复制出来的 oh_modules 是个空壳，表现为
    //   Failed to resolve OhmUrl for "@ohos/hypium"
    // 这里对链接取 statSync（跟随链接）来判断真实类型，并**解引用复制内容**。
    let isDir = e.isDirectory()
    let isFile = e.isFile()
    if (e.isSymbolicLink()) {
      try {
        const st = statSync(f)
        isDir = st.isDirectory()
        isFile = st.isFile()
      } catch {
        continue   // 悬空链接：跳过（不该出现在依赖目录里）
      }
    }
    if (isDir) copyTree(f, t)
    else if (isFile) copyFileSync(f, t)
  }
}

console.log('[harmony] repo      :', REPO)
console.log('[harmony] mirror    :', MIRROR)
console.log('[harmony] hvigor    :', HVIGOR_JS)

if (!existsSync(SRC)) { console.error('缺少 harmony/ 工程'); process.exit(1) }
if (!existsSync(HVIGOR_JS)) { console.error('未找到 DevEco hvigor:', HVIGOR_JS); process.exit(1) }

// 1. 同步源码到镜像
if (CLEAN && existsSync(BUILD_ROOT)) {
  rmSync(BUILD_ROOT, { recursive: true, force: true })
  console.log('[harmony] mirror cleaned:', BUILD_ROOT)
}
copyTree(SRC, MIRROR)
console.log('[harmony] source mirrored')

// 1a. third_party/ 必须一起镜像。
//
// CMakeLists.txt 里 ARGON2_ROOT 是相对路径（../../../../../third_party/argon2），
// 从 harmony/entry/src/main/cpp 向上五级正好落到「镜像根」。
// 因此镜像若只 copy harmony/，CMake 会在 <build-root>/third_party/argon2 找不到
// include/argon2.h 并 FATAL_ERROR —— 实测报错：
//   CMake Error at CMakeLists.txt:23 (message): vendored argon2 not found at ...
// 注意：这里镜像的是**构建输入**，不是构建产物；third_party 内容由
// tools/harmony/check-third-party-hashes.mjs 按 VENDOR.json 校验完整性。
{
  const tpSrc = join(REPO, 'third_party')
  const tpDst = join(BUILD_ROOT, 'third_party')
  if (!existsSync(tpSrc)) {
    console.error('[harmony] 缺少 third_party/ —— native 模块无法构建')
    process.exit(1)
  }
  copyTree(tpSrc, tpDst)
  console.log('[harmony] third_party mirrored:', tpDst)
}

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

// 3a. oh_modules（由 `ohpm install` 在仓库内生成）必须一并带进镜像。
//
// copyTree 故意排除 oh_modules（它是依赖产物，不该被复制成第二份真实副本），
// 但 hvigor 的 ArkTS 编译要靠它解析 `@ohos/hypium` 等 import ——
// 缺了它，任何测试代码都会报 "Cannot find module '@ohos/hypium'"。
//
// **必须是真实复制，不能做 junction**：实测 junction 指向仓库时，
// hvigor 会顺着链接把模块物理路径解析回仓库（非 ASCII 且不在镜像工程根内），
// OhmUrl 解析随之失败：
//   ArkTS:ERROR Failed to resolve OhmUrl.
//   ... for "E:\AI\号卡管理\harmony\oh_modules\.ohpm\@ohos+hypium@...\index.js"
// 复制成真实目录后物理路径落在镜像内，解析正常。
//
// **两处都要**：ohpm 会分别在工程根（harmony/oh_modules）与
// 声明了 devDependencies 的模块下（harmony/entry/oh_modules）落盘。
// 只复制根目录那份时，entry 的测试代码仍报
//   "has dependency which is not installed at its oh-package.json5" +
//   Failed to resolve OhmUrl for "@ohos/hypium"。
{
  const OH_DIRS = ['oh_modules', join('entry', 'oh_modules')]
  let copied = 0
  for (const rel of OH_DIRS) {
    const ohSrc = join(SRC, rel)
    if (!existsSync(ohSrc)) continue
    const ohDst = join(MIRROR, rel)
    // 先删掉可能残留的 junction / 旧副本，否则 copyFileSync 会写到链接目标上。
    if (existsSync(ohDst)) rmSync(ohDst, { recursive: true, force: true })
    copyTree(ohSrc, ohDst)
    copied++
  }
  if (copied === 0) {
    console.log('[harmony] oh_modules absent (先跑 ohpm install 才能编译测试代码)')
  } else {
    console.log('[harmony] oh_modules copied into mirror (' + copied + ' location(s))')
  }
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
// 超时取 4 分钟而不是 15 分钟，并允许对**超时**重试一次。
//
// 实测：hvigor 在本机会**间歇性永久挂起** —— 同样一条命令，有时 40 秒完成，
// 有时卡在 `BuildUnitTestHook` 之后再无进展，直到被强杀（exit 124）。
// 挂起发生在测试执行之前的构建阶段，与被测代码无关；重跑一次通常即通过。
//
// 为什么只重试「超时」而不重试「失败」：
//   编译失败是真实信号（例如 re-export 不产生局部绑定这类错误），
//   重试只会让人多等几分钟然后看到同一个错误，还会把首次失败的输出冲掉。
//   超时则相反 —— 它没有诊断价值，只有成本。
//
// 重试必须**可见**：静默重试会把"这个工具链不稳定"这个事实藏起来。
const BUILD_TIMEOUT_MS = 4 * 60 * 1000
const MAX_ATTEMPTS = 2

let out = ''
for (let attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
  try {
    out = execFileSync(process.execPath, [HVIGOR_JS, ...args], {
      cwd: MIRROR, env, encoding: 'utf8', stdio: 'pipe', timeout: BUILD_TIMEOUT_MS,
    })
    process.stdout.write(out)
    console.log('\n[harmony] BUILD SUCCESSFUL')
    break
  } catch (e) {
    process.stdout.write(e.stdout || '')
    process.stderr.write(e.stderr || '')
    const timedOut = e.status === null || e.signal === 'SIGTERM' || e.killed === true
    if (!timedOut || attempt === MAX_ATTEMPTS) {
      console.error('\n[harmony] BUILD FAILED' +
        (timedOut ? ' (hvigor 超时 ' + BUILD_TIMEOUT_MS / 1000 + 's，已重试 ' +
          (attempt - 1) + ' 次)' : ''))
      process.exit(1)
    }
    console.error('\n[harmony] hvigor 超时挂起（本机已知间歇性问题），重试 ' +
      attempt + '/' + (MAX_ATTEMPTS - 1) + ' ...')
  }
}
