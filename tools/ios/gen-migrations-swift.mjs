// 从 android/core/.../schema/Migrations.kt 生成 ios/Sources/PDIGCore/Schema/Migrations.swift。
//
// 为什么用生成器而不是手抄：
//   迁移 DDL 是**三端必须逐字一致**的冻结契约（spec §42 / MIG 原则）。
//   手抄 200+ 行 SQL 会引入缩进、模板变量、转义三类中不可避免的漂移，
//   而这种漂移在 conformance 里表现为"某个平台建表成功但字段不同"——
//   等到那时才发现代价极高。这里做机械转换，保证 Swift 常量与 Kotlin 源同源。
//
// 转换规则（逐条对应 Kotlin 语义）：
//   1. `"""..."""` 原始串 → 先做 Kotlin `$IDENT` 模板替换，再 `trimIndent()`
//   2. `"..."` 普通串 → 原样（本文件里不含转义）
//   3. 输出为 Swift raw string `#"..."#`，避免 `\(`、`\` 被重新解释

import { readFileSync, writeFileSync } from 'node:fs'

const SRC = process.argv[2]
const OUT = process.argv[3]

const text = readFileSync(SRC, 'utf8')

/** Kotlin `trimIndent()`：去掉所有非空行的最小公共缩进；首尾空行去掉。 */
function trimIndent(s) {
  const lines = s.split('\n')
  while (lines.length && lines[0].trim() === '') lines.shift()
  while (lines.length && lines[lines.length - 1].trim() === '') lines.pop()
  let min = Infinity
  for (const l of lines) {
    if (l.trim() === '') continue
    let n = 0
    while (n < l.length && (l[n] === ' ' || l[n] === '\t')) n++
    min = Math.min(min, n)
  }
  if (!isFinite(min)) min = 0
  return lines.map((l) => (l.trim() === '' ? '' : l.slice(min))).join('\n')
}

/** 提取 `val NAME: List<String> = listOf(...)` 内的字符串字面量序列。 */
function extractList(name) {
  const start = text.indexOf(`val ${name}: List<String> = listOf(`)
  if (start < 0) throw new Error(`cannot find ${name}`)
  const open = text.indexOf('listOf(', start) + 'listOf('.length
  // 括号配平（跳过字符串内容）
  let i = open
  let depth = 1
  const items = []
  let cur = null // {kind:'raw'|'plain', buf:[]}
  while (i < text.length && depth > 0) {
    const c = text[i]
    if (cur === null) {
      if (c === '"') {
        if (text.startsWith('"""', i)) {
          cur = { kind: 'raw', buf: [] }
          i += 3
          continue
        }
        cur = { kind: 'plain', buf: [] }
        i += 1
        continue
      }
      if (c === '(') depth++
      else if (c === ')') depth--
      i++
      continue
    }
    if (cur.kind === 'raw') {
      if (text.startsWith('"""', i)) {
        items.push({ kind: 'raw', value: cur.buf.join('') })
        cur = null
        i += 3
        continue
      }
      cur.buf.push(c)
      i++
      continue
    }
    // plain
    if (c === '\\') {
      cur.buf.push(c, text[i + 1])
      i += 2
      continue
    }
    if (c === '"') {
      items.push({ kind: 'plain', value: cur.buf.join('') })
      cur = null
      i += 1
      continue
    }
    cur.buf.push(c)
    i++
  }
  if (cur !== null) throw new Error(`unterminated string in ${name}`)
  return items
}

const CONSTS = {
  LEGACY_WECHAT_SOURCE_INSTANCE_ID: 'legacy-wechat-statement',
  LEGACY_WECHAT_ADAPTER_ID: 'wechat_statement',
  LEGACY_WECHAT_ADAPTER_VERSION: '1',
}

function resolveTemplates(s) {
  return s.replace(/\$([A-Z_][A-Z0-9_]*)/g, (m, name) => {
    if (!(name in CONSTS)) throw new Error(`unknown Kotlin template $${name}`)
    return CONSTS[name]
  })
}

//   3. 输出为 Swift raw string：
//      单行 → `#"..."#`；**多行 → 必须用 `#"""..."""#`**。
//      2026-09-19 实测：Swift 的单行 raw string 不允许内含裸换行，
//      用 `#"..."#` 承载多行 DDL 会报 `unterminated string literal`。
//      raw string 同时让 `\(`、`\` 不被重新解释。
function swiftRawString(s, indent = '    ') {
  if (s.includes('"#')) throw new Error('statement contains "#" — raw string delimiter would break')
  if (!s.includes('\n')) return '#"' + s + '"#'
  const body = s
    .split('\n')
    .map((line) => indent + line)
    .join('\n')
  // 多行 raw string：闭合 `"""#` 的缩进会从每行剥离，
  // 因此正文与闭合符用**同一**缩进，产出与源 SQL 逐字一致（含内部缩进）。
  if (s.includes('"""')) throw new Error('statement contains triple quote')
  return '#"""\n' + body + '\n' + indent + '"""#'
}

function emit(name, items) {
  // 常量落在 `enum Migrations` 内，必须是 `static`（caseless enum 不能有实例存储属性）。
  const body = items
    .map((it) => {
      const v = it.kind === 'raw' ? trimIndent(resolveTemplates(it.value)) : resolveTemplates(it.value)
      return '        ' + swiftRawString(v, '        ')
    })
    .join(',\n')
  return `    public static let ${name}: [String] = [\n${body},\n    ]`
}

const v1 = extractList('SCHEMA_V1_STATEMENTS')
const v2 = extractList('SCHEMA_V2_STATEMENTS')
const v3 = extractList('SCHEMA_V3_STATEMENTS')

const header = `// PDIG 逻辑 Schema 迁移（v1 → v2 → v3）—— **由生成器产出，不要手改**。
//
// 生成源：android/core/src/main/kotlin/com/pdig/core/schema/Migrations.kt
// 生成器：tools/ios/gen-migrations-swift.mjs
//
// 三端迁移行为必须一致：
//  - transactional：每个版本在独立事务内执行
//  - failure rollback：失败回滚，绝不留下半迁移 DB
//  - idempotent：重复执行严格 no-op
//  - IDs preserved / Proposal decisions preserved / Evidence preserved /
//    Group preserved / SourceInstance preserved
//  - 未来 schema version 明确 reject，绝不猜测兼容
//
// 逻辑 Schema 必须跨端一致；物理 DDL 可以不同
// （Android/iOS = SQLCipher，HarmonyOS = ArkData relationalStore）。
// 本文件的 DDL 逐字来自 Android 冻结源，SQLCipher 与 SQLite DDL 兼容。

import Foundation

public enum Migrations {

    public static let schemaVersion = 3

    /// deterministic legacy WeChat SourceInstance（重复 migration 不得创建第二个）。
    public static let legacyWechatSourceInstanceId = "legacy-wechat-statement"
    public static let legacyWechatAdapterId = "wechat_statement"
    public static let legacyWechatAdapterVersion = 1

    public struct Migration: Equatable, Sendable {
        public let version: Int
        public let statements: [String]
        public init(version: Int, statements: [String]) {
            self.version = version
            self.statements = statements
        }
    }
`

const footer = `
    public static let migrations: [Migration] = [
        Migration(version: 1, statements: schemaV1Statements),
        Migration(version: 2, statements: schemaV2Statements),
        Migration(version: 3, statements: schemaV3Statements),
    ]

    public static let migrationChain: [Int] = migrations.map { $0.version }

    /// 幂等迁移的纯判定部分：当前版本是否已达到目标（true 则 no-op）。
    /// 真正的 SQL 执行由平台持久层在**独立事务**内完成。
    public static func isUpToDate(_ current: Int) -> Bool { current >= schemaVersion }

    /// 未来版本一律拒绝：不猜测兼容（spec §42）。
    public static func accepts(_ current: Int) -> Bool { current <= schemaVersion }
}
`

const swift =
  header +
  '\n' +
  emit('schemaV1Statements', v1) +
  '\n\n' +
  emit('schemaV2Statements', v2) +
  '\n\n' +
  emit('schemaV3Statements', v3) +
  '\n' +
  footer

writeFileSync(OUT, swift, 'utf8')
console.log(
  `generated ${OUT}: v1=${v1.length} v2=${v2.length} v3=${v3.length} statements`,
)
