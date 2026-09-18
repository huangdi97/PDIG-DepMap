// Harmony 编译产物符号取证
//
// 为什么需要它：hvigor 的 CompileArkTS **只编译从 ability / page 可达的模块**，
// 未被 import 的 .ets 文件不进入编译图 —— 那种情况下 assembleHap 的
// "BUILD SUCCESSFUL" 不构成任何证据（在该文件里放语法错误也照样成功）。
// 因此每次声称"ArkTS 编译通过"，都必须同时给出 modules.abc 的符号取证。
//
// 用法：
//   node tools/harmony/probe-abc-symbols.mjs [modules.abc 路径]
//   PDIG_HARMONY_BUILD_ROOT=<目录> node tools/harmony/probe-abc-symbols.mjs
//
// 默认路径：<PDIG_HARMONY_BUILD_ROOT>/harmony/entry/build/default/intermediates/loader_out/default/ets/modules.abc
import { readFileSync, existsSync } from 'node:fs'
import { join, posix } from 'node:path'
import { homedir } from 'node:os'

const BUILD_ROOT = process.env.PDIG_HARMONY_BUILD_ROOT?.trim() ||
  posix.join(homedir().replace(/\\/g, '/'), 'pdig-harmony-build')
const DEFAULT_ABC = join(
  BUILD_ROOT, 'harmony', 'entry', 'build', 'default', 'intermediates',
  'loader_out', 'default', 'ets', 'modules.abc',
)
const P = process.argv[2] ?? DEFAULT_ABC

if (!existsSync(P)) {
  console.error('[abc] not found:', P)
  console.error('[abc] run a build first, or pass the path explicitly / set PDIG_HARMONY_BUILD_ROOT')
  process.exit(1)
}

const b = readFileSync(P)
console.log('abc=' + P)
console.log('bytes=' + b.length)

// .abc 是二进制，但模块路径与函数名以 ASCII/UTF-8 字符串形式驻留，
// 用 latin1 抽出可打印串即可做符号级确认。
const s = b.toString('latin1')
const tokens = [...new Set((s.match(/[ -~]{6,}/g) || []))]

function show(title, pred, limit = 60) {
  const hits = tokens.filter(pred)
  console.log(`\n--- ${title} (${hits.length}) ---`)
  hits.slice(0, limit).forEach((x) => console.log('  ' + x))
  return hits
}

show('模块声明', (x) => /^.{0,3}Lcom\.pdig\.depmap\/entry\/ets\//.test(x))
show('含 crypto 的串', (x) => /crypto/i.test(x), 80)
show('含 Jcs/jcs 的串', (x) => /Jcs|jcs/i.test(x), 40)

// 显式判定：目标模块是否真的进了产物
const TARGETS = [
  'entry/ets/crypto/Jcs',
  'entry/ets/crypto/DepmapContainerV1',
  'entry/ets/crypto/ContainerSelfCheck',
]
console.log('\n--- 目标模块命中 ---')
let all = true
for (const t of TARGETS) {
  const ok = tokens.some((x) => x.includes(t))
  if (!ok) all = false
  console.log(`  ${ok ? 'PRESENT' : 'MISSING'}  ${t}`)
}
console.log('\nABC_VERDICT=' + (all ? 'PRESENT' : 'MISSING'))
process.exit(all ? 0 : 1)
