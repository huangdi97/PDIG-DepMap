// Argon2 native build gate —— 交叉编译 libargon2 + NAPI 桥接，并对产物做符号整改自查。
//
// 为什么需要它：
//   1. 证明 vendored 源码**真的**能在 OHOS 工具链上编译（不是"文档说能"）；
//   2. 证明 `-fvisibility=hidden` + `A2_VISCTL=1` 生效 —— argon2_* 不得出现在
//      动态符号表里，否则 native 信任面会无谓扩大；
//   3. 证明 x86_64 与 arm64-v8a 两个 ABI 都能过（emulator 需要 x86_64）；
//   4. 固化在 CI 里，避免"某次改动悄悄破坏 native 构建"而无人发现。
//
// ⚠ 本机（Windows）已知环境缺陷 —— 必须经由 Python 驱动：
//   OHOS clang 15.0.4 在**优化开启**时，若父进程链是 node.exe，
//   会以 0xE06D7363 崩溃（LLVM crash backtrace），崩溃栈里是第三方注入 DLL
//   （D:\...\MToolBox_v2.0.2\x64\MToolExtend.dll）。
//   对照实验（tools/harmony/probe-clang-node-spawn.mjs，实测结果）：
//     - 父进程 = PowerShell / cmd  → 编译成功
//     - 父进程 = python            → 编译成功
//     - 父进程 = node              → CRASH（无论是否精简 env / 是否 windowsHide）
//   因此本脚本把 OHOS 编译委托给 tools/harmony/ohos-clang-build.py（Python 驱动器），
//   由它再派生子进程。这不是绕开门禁，而是绕开一个**与源码无关的宿主环境缺陷**；
//   编译仍在同一 clang + 同一 sysroot + 同一 flags 下真实执行。
//
// 用法:
//   PDIG_DEVECO_HOME=<DevEco 根目录> node tools/harmony/check-argon2-native-build.mjs
//   （未设置 PDIG_DEVECO_HOME 时以 SKIPPED 退出 0，便于无 SDK 的 CI runner）
import { existsSync, mkdirSync, rmSync } from 'node:fs'
import { join, posix } from 'node:path'
import { execFileSync } from 'node:child_process'
import { homedir } from 'node:os'

const REPO = process.cwd()
const ARGON2 = join(REPO, 'third_party', 'argon2')
const BRIDGE = join(REPO, 'harmony', 'entry', 'src', 'main', 'cpp', 'pdi_argon2.cpp')

const DEVECO = (process.env.PDIG_DEVECO_HOME ?? '').trim()
if (!DEVECO) {
  console.log('[argon2-native] PDIG_DEVECO_HOME not set — SKIPPED (no OHOS toolchain available)')
  process.exit(0)
}

const OHOS = join(DEVECO, 'sdk', 'default', 'openharmony')
const READELF = join(OHOS, 'native', 'llvm', 'bin', 'llvm-readelf.exe')
const NM = join(OHOS, 'native', 'llvm', 'bin', 'llvm-nm.exe')

for (const p of [READELF, NM, ARGON2, BRIDGE]) {
  if (!existsSync(p)) { console.error('[argon2-native] missing:', p); process.exit(1) }
}

const OUT = process.env.PDIG_HARMONY_BUILD_ROOT?.trim() ||
  posix.join(homedir().replace(/\\/g, '/'), 'pdig-harmony-build')
const WORK = join(OUT, 'argon2-native')
rmSync(WORK, { recursive: true, force: true })
mkdirSync(WORK, { recursive: true })

// Python 驱动器（绕开 node→clang 的宿主缺陷，见文件头注释）。
const PY_CANDIDATES = [
  join(homedir(), '.workbuddy', 'binaries', 'python', 'envs', 'default', 'Scripts', 'python.exe'),
  join(homedir(), '.workbuddy', 'binaries', 'python', 'envs', 'default', 'bin', 'python'),
]
const driver = join(REPO, 'tools', 'harmony', 'ohos-clang-build.py')
if (!existsSync(driver)) { console.error('[argon2-native] missing driver:', driver); process.exit(1) }

let buildReport
try {
  const raw = execFileSync('python', [driver, DEVECO, WORK, ARGON2, BRIDGE], {
    stdio: 'pipe', encoding: 'utf8', timeout: 20 * 60 * 1000,
  })
  buildReport = JSON.parse(raw)
} catch (e) {
  // 有些环境 `python` 不在 PATH 上：逐个候选重试。
  let ok = false
  for (const py of PY_CANDIDATES) {
    if (!existsSync(py)) continue
    try {
      const raw = execFileSync(py, [driver, DEVECO, WORK, ARGON2, BRIDGE], {
        stdio: 'pipe', encoding: 'utf8', timeout: 20 * 60 * 1000,
      })
      buildReport = JSON.parse(raw)
      ok = true
      break
    } catch (e2) {
      buildReport = { error: String(e2.stdout || e2.message).slice(0, 4000) }
      ok = true
    }
  }
  if (!ok) {
    console.error('[argon2-native] could not run python driver:', e.message)
    process.exit(1)
  }
}

if (buildReport.error) {
  console.error('[argon2-native] driver error:', buildReport.error)
  process.exit(1)
}

let failures = 0
const results = []

for (const t of buildReport.targets) {
  if (t.build !== 'OK') {
    failures++
    results.push({ target: t.target, build: 'FAIL', detail: (t.detail || '').slice(0, 2000) })
    continue
  }

  const so = t.soPath
  const elf = execFileSync(READELF, ['-h', so], { encoding: 'utf8' })
  const dyn = execFileSync(READELF, ['--dyn-syms', so], { encoding: 'utf8' })
  const needed = execFileSync(READELF, ['-d', so], { encoding: 'utf8' })
  const nmD = execFileSync(NM, ['-D', so], { encoding: 'utf8' })

  // 注意：llvm-readelf 的输出是 "Machine:" + 多个空格 + 值，不要用单空格匹配。
  const machineOk = new RegExp(`Machine:\\s+${t.machine}\\b`).test(elf)
  // 动态符号表不得导出任何 argon2_* —— 那是静态链接 + hidden visibility 的目的。
  // 有导出就说明有人重新引入了 -DA2_VISCTL=1（会把 ARGON2_PUBLIC 变成 default）。
  // 只认 llvm-nm -D 的符号行（地址 + 类型 + 名称），并排除 NAPI 入口本身
  // （它名字里带 "Argon2"，但正是**唯一允许**导出的符号）。
  const exportedArgon2 = nmD
    .split('\n')
    .filter((l) => /^[0-9a-fA-F]{8,}\s+[A-Za-z]\s+\S+/i.test(l))
    .filter((l) => /argon2/i.test(l))
    .filter((l) => !/RegisterPdiArgon2Module/.test(l))
  // NAPI 入口必须存在且可见，否则 ArkTS 无法加载模块。
  const hasModuleRegister = /RegisterPdiArgon2Module/.test(nmD)
  const neededLibs = [...needed.matchAll(/Shared library: \[(.+?)\]/g)].map((m) => m[1])

  const ok = machineOk && exportedArgon2.length === 0 && hasModuleRegister
  if (!ok) failures++

  results.push({
    target: t.target,
    build: 'OK',
    elfMachine: machineOk ? t.machine : 'UNEXPECTED',
    exportedArgon2Symbols: exportedArgon2.length,
    napiEntryExported: hasModuleRegister,
    neededLibs,
    bytes: t.bytes,
    verdict: ok ? 'PASS' : 'FAIL',
  })
}

console.log('[argon2-native] vendored source:', ARGON2)
console.log('[argon2-native] workdir:', WORK)
console.log('[argon2-native] driver: python (OHOS clang cannot be spawned safely from node on this host)')
for (const r of results) console.log('  ' + JSON.stringify(r))
console.log('\nARGON2_NATIVE_BUILD=' + (failures === 0 ? 'PASS' : 'FAIL'))
process.exit(failures === 0 ? 0 : 1)
