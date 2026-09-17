// 冻结证据哈希采集器 —— 只读，不修改任何文件。
// 用法: node tools/freeze/hash-evidence.mjs
//
// ⚠ 重要：本工程路径含非 ASCII，android/settings.gradle.kts 会把 Gradle 构建输出
//   重定向到 <user.home>/pdig-build/<module>。**因此 android/**/build/** 是自重定向
//   生效之日起的过期残留，不能作为取证来源。** 默认根目录即为此处重定向后的真实根；
//   可用 PDIG_ASCII_BUILD_ROOT 覆盖（与 settings.gradle.kts 同名环境变量保持一致）。
import { createHash } from 'node:crypto'
import { readFileSync, readdirSync, statSync, existsSync } from 'node:fs'
import { join, relative, posix } from 'node:path'
import { execFileSync } from 'node:child_process'
import { homedir } from 'node:os'

const ROOT = process.cwd()

const BUILD_ROOT = process.env.PDIG_ASCII_BUILD_ROOT?.trim() ||
  posix.join(homedir().replace(/\\/g, '/'), 'pdig-build')

function sha256File(abs) {
  return createHash('sha256').update(readFileSync(abs)).digest('hex')
}

/** 目录树确定性哈希：相对 POSIX 路径排序后逐条 hash(path)+hash(content) 聚合 */
function treeSha256(dir) {
  const entries = []
  const walk = (d) => {
    for (const name of readdirSync(d).sort()) {
      if (name === 'node_modules' || name === '.git' || name === 'build') continue
      const abs = join(d, name)
      const st = statSync(abs)
      if (st.isDirectory()) walk(abs)
      else if (st.isFile()) entries.push([posix.normalize(relative(ROOT, abs).split('\\').join('/')), abs])
    }
  }
  walk(dir)
  entries.sort((a, b) => (a[0] < b[0] ? -1 : a[0] > b[0] ? 1 : 0))
  const agg = createHash('sha256')
  agg.update(`TREE-V1\n${entries.length}\n`)
  for (const [rel, abs] of entries) {
    agg.update(rel)
    agg.update('\u0000')
    agg.update(sha256File(abs))
    agg.update('\n')
  }
  return { hash: agg.digest('hex'), count: entries.length }
}

function git(args) {
  try {
    return execFileSync('git', args, { cwd: ROOT, encoding: 'utf8' }).trim()
  } catch {
    return ''
  }
}

/** 统计 Gradle JUnit XML 结果目录 */
function testResults(dir) {
  const out = { xml: 0, tests: 0, failures: 0, errors: 0, skipped: 0, mtime: null }
  const walk = (d) => {
    for (const e of readdirSync(d, { withFileTypes: true })) {
      const abs = join(d, e.name)
      if (e.isDirectory()) walk(abs)
      else if (e.name.endsWith('.xml')) {
        out.xml++
        const g = /<testsuite[^>]*>/.exec(readFileSync(abs, 'utf8').slice(0, 1500))
        if (!g) continue
        const get = (k) => {
          const m = new RegExp(k + '="(\\d+)"').exec(g[0])
          return m ? +m[1] : 0
        }
        out.tests += get('tests')
        out.skipped += get('skipped')
        out.failures += get('failures')
        out.errors += get('errors')
        const mt = statSync(abs).mtime.toISOString()
        if (!out.mtime || mt > out.mtime) out.mtime = mt
      }
    }
  }
  if (existsSync(dir)) walk(dir)
  else out.missing = true
  return out
}

const out = {
  generatedAt: new Date().toISOString(),
  canonicalHead: git(['rev-parse', 'HEAD']),
  canonicalHeadShort: git(['rev-parse', '--short', 'HEAD']),
  branch: git(['rev-parse', '--abbrev-ref', 'HEAD']),
  treeHashOfHead: git(['rev-parse', 'HEAD^{tree}']),
  androidSourceCommit: git(['log', '-1', '--format=%H', '--', 'android']),
  androidSourceCommitShort: git(['log', '-1', '--format=%h', '--', 'android']),
  untracked: git(['status', '--short', '-uall']).split('\n').filter(Boolean),
  buildRoot: BUILD_ROOT,
}

out.spec = existsSync(join(ROOT, 'spec')) ? treeSha256(join(ROOT, 'spec')) : null
out.fixtures = existsSync(join(ROOT, 'fixtures')) ? treeSha256(join(ROOT, 'fixtures')) : null
out.conformanceManifest = existsSync(join(ROOT, 'conformance/CONFORMANCE_MANIFEST.json'))
  ? {
      sha256: sha256File(join(ROOT, 'conformance/CONFORMANCE_MANIFEST.json')),
      gitBlob: git(['rev-parse', 'HEAD:conformance/CONFORMANCE_MANIFEST.json']),
    }
  : null

// 真实构建产物（重定向后的根目录）
out.artifacts = []
for (const rel of [
  'app/outputs/apk/debug/app-debug.apk',
  'app/outputs/apk/release/app-release-unsigned.apk',
  'app/outputs/bundle/release/app-release.aab',
]) {
  const abs = join(BUILD_ROOT, rel)
  if (existsSync(abs)) {
    out.artifacts.push({
      path: `<build-root>/${rel}`,
      bytes: statSync(abs).size,
      mtime: statSync(abs).mtime.toISOString(),
      sha256: sha256File(abs),
    })
  } else {
    out.artifacts.push({ path: `<build-root>/${rel}`, missing: true })
  }
}

out.testSuites = {
  ':core:test': testResults(join(BUILD_ROOT, 'core/test-results/test')),
  ':app:testDebugUnitTest': testResults(join(BUILD_ROOT, 'app/test-results/testDebugUnitTest')),
}

out.codegen = {}
for (const rel of [
  'android/core/src/main/kotlin/com/pdig/core/generated/CanonicalEnums.kt',
  'harmony/entry/src/main/ets/generated/CanonicalEnums.ets',
  'ios/Sources/PDIGCore/Generated/CanonicalEnums.swift',
]) {
  const abs = join(ROOT, rel)
  out.codegen[rel] = existsSync(abs) ? sha256File(abs) : null
}

console.log(JSON.stringify(out, null, 2))
