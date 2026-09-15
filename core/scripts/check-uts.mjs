#!/usr/bin/env node
/**
 * UTS 编译门禁 —— 用 DCloud 官方 UTS 编译器把 5 个 UTS 插件的三端实现各自降级为目标语言：
 *
 *   app-android/index.uts → Kotlin
 *   app-ios/index.uts     → Swift
 *   app-harmony/index.uts → ArkTS
 *
 * 背景：此前多轮报告只能声明「UTS 从未被任何真实编译器验证过」（B10）。实际 DCloud 把
 * UTS 编译器（Rust 实现，napi 暴露）公开发布在 npm 上，可在 Windows/无头环境下直接调用，
 * 与 HBuilderX 的 App 打包（仍需账号）相互独立。
 *
 * 该门禁**不**进入 `npm run check`：它需要联网安装编译器，若强制纳入会让干净克隆失败。
 * 请在需要验证 UTS 层时显式运行。
 *
 * 安装编译器（一次性）：
 *   node -e "process.chdir('<repo>/.tmp_audit/uts-cli')"   # 或任意 ASCII 目录
 *   npm i @dcloudio/uts@3.0.0-alpha-5020620260914001 \
 *         @dcloudio/uts-win32-x64-msvc@3.0.0-alpha-5020620260914001
 *
 * 编译器位置解析顺序：
 *   1) 环境变量 DEPMAP_UTS_COMPILER（指向 @dcloudio/uts 包目录）
 *   2) <repo>/.tmp_audit/uts-cli/node_modules/@dcloudio/uts
 *
 * 未找到编译器时输出 SKIPPED 并以 0 退出（不阻断），找到时任何失败都以 1 退出。
 */
import { createRequire } from 'node:module'
import { existsSync, mkdirSync, rmSync, statSync } from 'node:fs'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const HERE = dirname(fileURLToPath(import.meta.url))
const REPO = resolve(HERE, '..', '..')
const PLUGINS_DIR = join(REPO, 'app', 'uni_modules')
const OUT_DIR = join(REPO, '.tmp_audit', 'uts-gate-out')

const PLUGINS = [
  'depmap-biometric',
  'depmap-file-crypto',
  'depmap-privacy-screen',
  'depmap-secure-database',
  'depmap-secure-key',
]

/** 平台 → [源目录, 目标扩展名, 编译器 API 名] */
const TARGETS = [
  ['app-android', 'kt', 'toKotlin'],
  ['app-ios', 'swift', 'toSwift'],
  ['app-harmony', 'ets', 'toArkTS'],
]

function locateCompiler() {
  const candidates = [
    process.env.DEPMAP_UTS_COMPILER,
    join(REPO, '.tmp_audit', 'uts-cli', 'node_modules', '@dcloudio', 'uts'),
  ].filter(Boolean)
  for (const c of candidates) {
    if (existsSync(join(c, 'package.json'))) return c
  }
  return null
}

const norm = (p) => p.replace(/\\/g, '/')

async function main() {
  const compilerDir = locateCompiler()
  if (!compilerDir) {
    console.log('UTS compile gate: SKIPPED（未找到 @dcloudio/uts 编译器）')
    console.log(
      '  install: npm i @dcloudio/uts@3.0.0-alpha-5020620260914001 @dcloudio/uts-win32-x64-msvc@3.0.0-alpha-5020620260914001',
    )
    console.log('  或设置 DEPMAP_UTS_COMPILER=<path to @dcloudio/uts>')
    return 0
  }

  const require = createRequire(join(compilerDir, 'package.json'))
  const uts = require(compilerDir)
  rmSync(OUT_DIR, { recursive: true, force: true })
  mkdirSync(OUT_DIR, { recursive: true })

  console.log(`UTS compile gate — compiler: ${compilerDir}`)
  const failures = []
  let checked = 0

  for (const [platform, ext, api] of TARGETS) {
    console.log(`\n[${platform}]`)
    for (const plugin of PLUGINS) {
      const dir = join(PLUGINS_DIR, plugin, 'utssdk', platform)
      const src = join(dir, 'index.uts')
      if (!existsSync(src)) {
        console.log(`  ${plugin.padEnd(26)} MISSING_SOURCE`)
        failures.push(`${platform}/${plugin}: source missing`)
        continue
      }
      const outFile = join(OUT_DIR, `${platform}-${plugin}.${ext}`)
      const options = {
        input: { root: norm(dir), filename: norm(src), comments: true },
        output: {
          outDir: norm(OUT_DIR),
          outFilename: `${platform}-${plugin}.${ext}`,
          extname: ext,
          removeImports: true,
          package: 'com.depmap.plugins.' + plugin.replace(/^depmap-/, '').replace(/-/g, ''),
          imports: [],
        },
      }
      let result
      try {
        result = await uts[api](options)
      } catch (e) {
        failures.push(`${platform}/${plugin}: ${String(e && e.message).split('\n')[0]}`)
        console.log(
          `  ${plugin.padEnd(26)} FAIL  ${String(e && e.message)
            .split('\n')[0]
            .slice(0, 120)}`,
        )
        continue
      }
      if (existsSync(outFile)) {
        checked += 1
        console.log(`  ${plugin.padEnd(26)} PASS  ${statSync(outFile).size} B`)
      } else {
        const detail = JSON.stringify(result && result.error).slice(0, 160)
        failures.push(`${platform}/${plugin}: no output ${detail}`)
        console.log(`  ${plugin.padEnd(26)} FAIL  ${detail}`)
      }
    }
  }

  console.log(`\nUTS compile gate: ${checked}/${TARGETS.length * PLUGINS.length} compiled`)
  if (failures.length > 0) {
    console.log('FAILURES:')
    for (const f of failures) console.log('  - ' + f)
    return 1
  }
  console.log('UTS compile gate PASS')
  return 0
}

main().then(
  (code) => process.exit(code),
  (err) => {
    console.error('UTS compile gate: FATAL', err)
    process.exit(1)
  },
)
