#!/usr/bin/env node
/**
 * PDIG Codegen — spec/domain/domain.json  →  Kotlin / Swift / ArkTS
 *
 * 用法：
 *   node tools/codegen/generate.mjs           # 写入生成文件
 *   node tools/codegen/generate.mjs --check   # 只校验（不一致 → exit 1）
 *
 * 规则（spec/README.md §2）：
 *  - 三端禁止手写会漂移的静态枚举；枚举一律由本脚本生成
 *  - 生成文件头必须带 DO NOT EDIT
 *  - 手改 generated 文件 → --check 失败（Gate 失败）
 */

import { readFileSync, writeFileSync, mkdirSync, existsSync } from 'node:fs'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const HERE = dirname(fileURLToPath(import.meta.url))
const ROOT = resolve(HERE, '..', '..')

const SPEC_PATH = join(ROOT, 'spec', 'domain', 'domain.json')
const ERROR_PATH = join(ROOT, 'spec', 'errors', 'error-codes.json')
const DEPMAP_PATH = join(ROOT, 'spec', 'security', 'depmap-container-v1.json')

const spec = JSON.parse(readFileSync(SPEC_PATH, 'utf8'))
const errors = JSON.parse(readFileSync(ERROR_PATH, 'utf8'))
const depmap = JSON.parse(readFileSync(DEPMAP_PATH, 'utf8'))

// ---------------------------------------------------------------------------
// 标识符规范化（确定性；三端共用同一规则，避免平台各自为政）
// ---------------------------------------------------------------------------

/**
 * wire value → 枚举常量名（UPPER_SNAKE）。
 * 规则：
 *  - 非字母数字 → '_'
 *  - 首字符为数字 → 前缀 'V'（Swift/ArkTS 不允许数字开头）
 *  - 结果为空或仅下划线（如 ',' ';' '\t' '|'）→ 'U' + 各码点十六进制
 */
export function enumConstName(value) {
  const cleaned = value.replace(/[^A-Za-z0-9]+/g, '_').replace(/^_+|_+$/g, '')
  if (cleaned.length === 0) {
    const cps = [...value].map((c) => c.codePointAt(0).toString(16).toUpperCase().padStart(2, '0'))
    return 'U' + cps.join('_')
  }
  let s = cleaned.toUpperCase()
  if (/^[0-9]/.test(s)) s = 'V' + s
  return s
}

/** UPPER_SNAKE → lowerCamel（Swift case 名） */
export function lowerCamel(upperSnake) {
  const parts = upperSnake.split('_')
  return parts
    .map((p, i) => {
      if (i === 0) return p.toLowerCase()
      if (p.length === 0) return ''
      // 保留 V7D 这类前缀数字形式为小写开头
      return p.charAt(0).toUpperCase() + p.slice(1).toLowerCase()
    })
    .join('')
}

/** wire value → 'X' 形式（单引号转义） */
function quote(s) {
  return "'" + String(s).replace(/\\/g, '\\\\').replace(/'/g, "\\'") + "'"
}

/**
 * wire value → 'X' 形式（双引号转义）。
 *
 * 2026-09-19 修正：**控制字符必须转义**。此前 `dq()` 只处理 `\` 与 `"`，
 * 于是 `CsvDelimiter.U_0009` 的 wire（一个裸 TAB）被原样写进源码，
 * Swift 报 `unprintable ASCII character found in source file`。
 * 转义成 `\t` / `\n` / `\r` / `\uXXXX` 在 Kotlin / Swift / ArkTS 里语义相同。
 */
function dq(s) {
  const escaped = String(s)
    .replace(/\\/g, '\\\\')
    .replace(/"/g, '\\"')
    .replace(/\t/g, '\\t')
    .replace(/\n/g, '\\n')
    .replace(/\r/g, '\\r')
    .replace(/[\u0000-\u001f]/g, (c) => '\\u' + c.charCodeAt(0).toString(16).padStart(4, '0'))
  return '"' + escaped + '"'
}

/**
 * Swift 保留字作为 case 名必须加反引号。
 *
 * 2026-09-19 修正：spec 里有 `in`（ObservationDirection）、`any`（GroupMode）、
 * `open`（DriftStatus），直接输出 `case in = "in"` 会让 Swift 报
 * `keyword 'in' cannot be used as an identifier here`。
 */
const SWIFT_KEYWORDS = new Set([
  'in', 'any', 'open', 'default', 'self', 'static', 'do', 'is', 'as', 'nil',
  'true', 'false', 'repeat', 'where', 'defer', 'guard', 'import', 'init',
  'deinit', 'subscript', 'protocol', 'extension', 'internal', 'public',
  'private', 'fileprivate', 'inout', 'operator', 'precedence', 'type',
  'some', 'none', 'for', 'while', 'switch', 'case', 'break', 'continue',
  'fallthrough', 'return', 'throw', 'try', 'catch', 'async', 'await',
])

function swiftIdent(name) {
  return SWIFT_KEYWORDS.has(name) ? '`' + name + '`' : name
}

// ---------------------------------------------------------------------------
// 生成头
// ---------------------------------------------------------------------------

function header(comment) {
  return [
    `${comment} DO NOT EDIT`,
    `${comment} Generated from canonical PDIG spec (spec/domain/domain.json)`,
    `${comment} specVersion: ${spec.specVersion} | appSchemaVersion: ${spec.appSchemaVersion}`,
    `${comment} Generator: tools/codegen/generate.mjs — run \`node tools/codegen/generate.mjs\``,
    '',
  ].join('\n')
}

/** 枚举条目：{ name, wire }[]，若 runtimeValues 存在则额外产出一组 */
function enumEntries(name) {
  const def = spec.enums[name]
  const all = def.values.map((v) => ({ name: enumConstName(v), wire: v }))
  const result = [{ enumName: name, entries: all, scope: 'all' }]
  if (Array.isArray(def.runtimeValues)) {
    const allowed = new Set(def.runtimeValues)
    // 保持 spec 声明顺序，但只保留 runtime 值
    const runtime = def.values.filter((v) => allowed.has(v)).map((v) => ({ name: enumConstName(v), wire: v }))
    result.push({ enumName: `${name}Runtime`, entries: runtime, scope: 'runtime' })
  }
  return result
}

// ---------------------------------------------------------------------------
// Kotlin
// ---------------------------------------------------------------------------

function emitKotlin() {
  const L = []
  L.push(header('//'))
  L.push('package com.pdig.core.generated')
  L.push('')
  L.push('/** PDIG canonical enums — Kotlin (Android native). */')
  L.push('public object CanonicalSpec {')
  L.push(`    public const val SPEC_VERSION: String = ${dq(spec.specVersion)}`)
  L.push(`    public const val APP_SCHEMA_VERSION: Int = ${spec.appSchemaVersion}`)
  L.push(`    public const val GRAPH_PAYLOAD_KIND: String = ${dq(spec.graphPayload.kind)}`)
  L.push(`    public const val GRAPH_PAYLOAD_VERSION: Int = ${spec.graphPayload.version}`)
  L.push('}')
  L.push('')

  for (const name of Object.keys(spec.enums)) {
    for (const { enumName, entries } of enumEntries(name)) {
      L.push(`public enum class ${enumName}(public val wire: String) {`)
      entries.forEach((e, i) => {
        const sep = i === entries.length - 1 ? '' : ','
        L.push(`    ${e.name}(${dq(e.wire)})${sep}`)
      })
      L.push('    ;')
      L.push('')
      L.push('    public companion object {')
      L.push(`        public fun fromWire(value: String): ${enumName}? = entries.firstOrNull { it.wire == value }`)
      L.push('    }')
      L.push('}')
      L.push('')
    }
  }

  // ErrorCode
  L.push('public enum class ErrorCode(public val wire: String, public val category: String, public val messageKey: String) {')
  errors.codes.forEach((c, i) => {
    const sep = i === errors.codes.length - 1 ? '' : ','
    L.push(`    ${enumConstName(c.code)}(${dq(c.code)}, ${dq(c.category)}, ${dq(c.messageKey)})${sep}`)
  })
  L.push('    ;')
  L.push('')
  L.push('    public companion object {')
  L.push('        public fun fromWire(value: String): ErrorCode? = entries.firstOrNull { it.wire == value }')
  L.push('    }')
  L.push('}')
  L.push('')

  // Depmap protocol constants
  L.push('public object DepmapContainerV1 {')
  L.push(`    public const val FORMAT: String = ${dq(depmap.format)}`)
  L.push(`    public const val FORMAT_VERSION: Int = ${depmap.formatVersion}`)
  L.push(`    public const val KDF_ALGORITHM: String = ${dq(depmap.kdf.algorithm)}`)
  L.push(`    public const val KDF_VERSION: Int = ${depmap.kdf.version}`)
  L.push(`    public const val SALT_BYTES: Int = ${depmap.kdf.saltBytes}`)
  L.push(`    public const val MEMORY_KIB: Int = ${depmap.kdf.memoryKiB}`)
  L.push(`    public const val ITERATIONS: Int = ${depmap.kdf.iterations}`)
  L.push(`    public const val PARALLELISM: Int = ${depmap.kdf.parallelism}`)
  L.push(`    public const val KEY_BYTES: Int = ${depmap.kdf.derivedKeyBytes}`)
  L.push(`    public const val CIPHER_ALGORITHM: String = ${dq(depmap.cipher.algorithm)}`)
  L.push(`    public const val NONCE_BYTES: Int = ${depmap.cipher.nonceBytes}`)
  L.push(`    public const val TAG_BYTES: Int = ${depmap.cipher.tagBytes}`)
  L.push(`    public const val MEMORY_KIB_MIN: Int = ${depmap.bounds.memoryKiB.min}`)
  L.push(`    public const val MEMORY_KIB_MAX: Int = ${depmap.bounds.memoryKiB.max}`)
  L.push(`    public const val ITERATIONS_MIN: Int = ${depmap.bounds.iterations.min}`)
  L.push(`    public const val ITERATIONS_MAX: Int = ${depmap.bounds.iterations.max}`)
  L.push(`    public const val PARALLELISM_MIN: Int = ${depmap.bounds.parallelism.min}`)
  L.push(`    public const val PARALLELISM_MAX: Int = ${depmap.bounds.parallelism.max}`)
  L.push(`    public const val CIPHERTEXT_MAX_BYTES: Long = ${depmap.bounds.ciphertextMaxBytes}L`)
  L.push('}')
  L.push('')
  return L.join('\n')
}

// ---------------------------------------------------------------------------
// Swift
// ---------------------------------------------------------------------------

function emitSwift() {
  const L = []
  L.push(header('//'))
  L.push('import Foundation')
  L.push('')
  L.push('/// PDIG canonical enums — Swift (iOS native).')
  L.push('public enum CanonicalSpec {')
  L.push(`    public static let specVersion = ${dq(spec.specVersion)}`)
  L.push(`    public static let appSchemaVersion = ${spec.appSchemaVersion}`)
  L.push(`    public static let graphPayloadKind = ${dq(spec.graphPayload.kind)}`)
  L.push(`    public static let graphPayloadVersion = ${spec.graphPayload.version}`)
  L.push('}')
  L.push('')

  for (const name of Object.keys(spec.enums)) {
    for (const { enumName, entries } of enumEntries(name)) {
      // Equatable 必须**显式声明**：raw-value enum 不会自动获得 `==`，
      // 而领域模型（Dependency / DepNode …）声明了 Equatable 并持有这些枚举，
      // 少了它会让整片模型的合成失败。
      L.push(`public enum ${enumName}: String, CaseIterable, Sendable, Equatable {`)
      for (const e of entries) {
        L.push(`    case ${swiftIdent(lowerCamel(e.name))} = ${dq(e.wire)}`)
      }
      L.push('')
      L.push(`    public var wire: String { rawValue }`)
      L.push('}')
      L.push('')
    }
  }

  L.push('public enum ErrorCode: String, CaseIterable, Sendable, Equatable {')
  for (const c of errors.codes) {
    L.push(`    case ${swiftIdent(lowerCamel(enumConstName(c.code)))} = ${dq(c.code)}`)
  }
  L.push('')
  L.push('    public var wire: String { rawValue }')
  L.push('')
  L.push('    public var category: String {')
  L.push('        switch self {')
  for (const c of errors.codes) {
    L.push(`        case .${swiftIdent(lowerCamel(enumConstName(c.code)))}: return ${dq(c.category)}`)
  }
  L.push('        }')
  L.push('    }')
  L.push('')
  L.push('    public var messageKey: String {')
  L.push('        switch self {')
  for (const c of errors.codes) {
    L.push(`        case .${swiftIdent(lowerCamel(enumConstName(c.code)))}: return ${dq(c.messageKey)}`)
  }
  L.push('        }')
  L.push('    }')
  L.push('}')
  L.push('')

  L.push('public enum DepmapContainerV1 {')
  L.push(`    public static let format = ${dq(depmap.format)}`)
  L.push(`    public static let formatVersion = ${depmap.formatVersion}`)
  L.push(`    public static let kdfAlgorithm = ${dq(depmap.kdf.algorithm)}`)
  L.push(`    public static let kdfVersion = ${depmap.kdf.version}`)
  L.push(`    public static let saltBytes = ${depmap.kdf.saltBytes}`)
  L.push(`    public static let memoryKiB = ${depmap.kdf.memoryKiB}`)
  L.push(`    public static let iterations = ${depmap.kdf.iterations}`)
  L.push(`    public static let parallelism = ${depmap.kdf.parallelism}`)
  L.push(`    public static let keyBytes = ${depmap.kdf.derivedKeyBytes}`)
  L.push(`    public static let cipherAlgorithm = ${dq(depmap.cipher.algorithm)}`)
  L.push(`    public static let nonceBytes = ${depmap.cipher.nonceBytes}`)
  L.push(`    public static let tagBytes = ${depmap.cipher.tagBytes}`)
  L.push(`    public static let memoryKiBMin = ${depmap.bounds.memoryKiB.min}`)
  L.push(`    public static let memoryKiBMax = ${depmap.bounds.memoryKiB.max}`)
  L.push(`    public static let iterationsMin = ${depmap.bounds.iterations.min}`)
  L.push(`    public static let iterationsMax = ${depmap.bounds.iterations.max}`)
  L.push(`    public static let parallelismMin = ${depmap.bounds.parallelism.min}`)
  L.push(`    public static let parallelismMax = ${depmap.bounds.parallelism.max}`)
  L.push(`    public static let ciphertextMaxBytes = ${depmap.bounds.ciphertextMaxBytes}`)
  L.push('}')
  L.push('')
  return L.join('\n')
}

// ---------------------------------------------------------------------------
// ArkTS
// ---------------------------------------------------------------------------

/** 首字母小写（ArkTS `fromWire` 函数命名：`NodeKind` → `nodeKindFromWire`）。 */
function lowerFirst(s) {
  return s.charAt(0).toLowerCase() + s.slice(1)
}

/**
 * 为每个 enum 生成 `xxxFromWire`。
 *
 * 为什么必须生成而不是手写（单一真相源）：
 *   Kotlin / Swift emitter 早就随 enum 一起产出 `fromWire`（Kotlin 走 `entries`，
 *   Swift 走 `init?(rawValue:)`），唯独 ArkTS emitter 只产纯 enum，于是 Harmony 侧
 *   曾经在 `domain/CanonicalWire.ets` 里**手抄了一份 wire→成员表** —— 那正是
 *   「同一个合法集合被维护两遍」的典型第二真相源：spec 加一个枚举值，
 *   generated 文件会自动更新，手抄表不会，且不会被任何 Gate 抓住。
 *
 * 现在由生成物直接提供，删除手抄表；此后新增枚举值只有一处需要改（spec）。
 */
function emitArkTSFromWire(enumName, entries) {
  const fn = `${lowerFirst(enumName)}FromWire`
  const L = []
  L.push(`export function ${fn}(value: string): ${enumName} | undefined {`)
  for (const e of entries) {
    L.push(`  if (value === ${enumName}.${e.name}) {`)
    L.push(`    return ${enumName}.${e.name};`)
    L.push('  }')
  }
  L.push('  return undefined;')
  L.push('}')
  L.push('')
  return L
}

function emitArkTS() {
  const L = []
  L.push(header('//'))
  L.push('/** PDIG canonical enums — ArkTS (HarmonyOS native). */')
  L.push('export class CanonicalSpec {')
  L.push(`  static readonly SPEC_VERSION: string = ${quote(spec.specVersion)}`)
  L.push(`  static readonly APP_SCHEMA_VERSION: number = ${spec.appSchemaVersion}`)
  L.push(`  static readonly GRAPH_PAYLOAD_KIND: string = ${quote(spec.graphPayload.kind)}`)
  L.push(`  static readonly GRAPH_PAYLOAD_VERSION: number = ${spec.graphPayload.version}`)
  L.push('}')
  L.push('')

  for (const name of Object.keys(spec.enums)) {
    for (const { enumName, entries } of enumEntries(name)) {
      L.push(`export enum ${enumName} {`)
      entries.forEach((e, i) => {
        const sep = i === entries.length - 1 ? '' : ','
        L.push(`  ${e.name} = ${quote(e.wire)}${sep}`)
      })
      L.push('}')
      L.push('')
      L.push(...emitArkTSFromWire(enumName, entries))
    }
  }

  L.push('export enum ErrorCode {')
  errors.codes.forEach((c, i) => {
    const sep = i === errors.codes.length - 1 ? '' : ','
    L.push(`  ${enumConstName(c.code)} = ${quote(c.code)}${sep}`)
  })
  L.push('}')
  L.push('')
  L.push(
    ...emitArkTSFromWire(
      'ErrorCode',
      errors.codes.map((c) => ({ name: enumConstName(c.code), wire: c.code })),
    ),
  )

  L.push('export class DepmapContainerV1 {')
  L.push(`  static readonly FORMAT: string = ${quote(depmap.format)}`)
  L.push(`  static readonly FORMAT_VERSION: number = ${depmap.formatVersion}`)
  L.push(`  static readonly KDF_ALGORITHM: string = ${quote(depmap.kdf.algorithm)}`)
  L.push(`  static readonly KDF_VERSION: number = ${depmap.kdf.version}`)
  L.push(`  static readonly SALT_BYTES: number = ${depmap.kdf.saltBytes}`)
  L.push(`  static readonly MEMORY_KIB: number = ${depmap.kdf.memoryKiB}`)
  L.push(`  static readonly ITERATIONS: number = ${depmap.kdf.iterations}`)
  L.push(`  static readonly PARALLELISM: number = ${depmap.kdf.parallelism}`)
  L.push(`  static readonly KEY_BYTES: number = ${depmap.kdf.derivedKeyBytes}`)
  L.push(`  static readonly CIPHER_ALGORITHM: string = ${quote(depmap.cipher.algorithm)}`)
  L.push(`  static readonly NONCE_BYTES: number = ${depmap.cipher.nonceBytes}`)
  L.push(`  static readonly TAG_BYTES: number = ${depmap.cipher.tagBytes}`)
  L.push(`  static readonly MEMORY_KIB_MIN: number = ${depmap.bounds.memoryKiB.min}`)
  L.push(`  static readonly MEMORY_KIB_MAX: number = ${depmap.bounds.memoryKiB.max}`)
  L.push(`  static readonly ITERATIONS_MIN: number = ${depmap.bounds.iterations.min}`)
  L.push(`  static readonly ITERATIONS_MAX: number = ${depmap.bounds.iterations.max}`)
  L.push(`  static readonly PARALLELISM_MIN: number = ${depmap.bounds.parallelism.min}`)
  L.push(`  static readonly PARALLELISM_MAX: number = ${depmap.bounds.parallelism.max}`)
  L.push(`  static readonly CIPHERTEXT_MAX_BYTES: number = ${depmap.bounds.ciphertextMaxBytes}`)
  L.push('}')
  L.push('')
  return L.join('\n')
}

// ---------------------------------------------------------------------------
// ArkTS — RelationDefinitionRegistry（从 spec/domain/domain.json 的 `relations` 生成）
// ---------------------------------------------------------------------------

/**
 * Relation 治理定义表。
 *
 * 为什么要生成本文件（而不是在 `domain/Relations.ets` 里手写）：
 *   `spec/domain/domain.json` 的 `relations` 段本身就是权威定义，
 *   Harmony 侧此前手抄了一份 `RELATION_DEFINITIONS` —— 内容与 spec 一致但漂移无 Gate 可抓。
 *   属第二真相源，故改为生成。
 *
 * 字段集合的选择（重要）：spec 里每个 relation 还带 `verificationPolicy` /
 * `impactSemantics`，而 **Android 冻结基准（ANDROID_NATIVE_CORE_FREEZE.md）没有这两个字段**。
 * 此处只发射 Android 冻结版那 7 个字段 —— 目的是 parity，不是超前。
 * 两个字段的差异仍记录在审计报告中。
 */
function emitArkTSRelations() {
  const L = []
  L.push(header('//'))
  L.push(`import { Capability, Criticality, GroupMode, NodeKind, Relation } from './CanonicalEnums';`)
  L.push('')
  L.push('/** single relation definition — 字段集合对齐 Android 冻结版。 */')
  L.push('export class CanonicalRelationDefinition {')
  L.push('  id: Relation;')
  L.push('  fromKinds: NodeKind[];')
  L.push('  toKinds: NodeKind[];')
  L.push('  capability: Capability;')
  L.push('  allowsGroup: boolean;')
  L.push('  allowedGroupModes: GroupMode[];')
  L.push('  defaultCriticality: Criticality;')
  L.push('')
  L.push('  constructor(')
  L.push('    id: Relation,')
  L.push('    fromKinds: NodeKind[],')
  L.push('    toKinds: NodeKind[],')
  L.push('    capability: Capability,')
  L.push('    allowsGroup: boolean,')
  L.push('    allowedGroupModes: GroupMode[],')
  L.push('    defaultCriticality: Criticality,')
  L.push('  ) {')
  L.push('    this.id = id;')
  L.push('    this.fromKinds = fromKinds;')
  L.push('    this.toKinds = toKinds;')
  L.push('    this.capability = capability;')
  L.push('    this.allowsGroup = allowsGroup;')
  L.push('    this.allowedGroupModes = allowedGroupModes;')
  L.push('    this.defaultCriticality = defaultCriticality;')
  L.push('  }')
  L.push('}')
  L.push('')

  const ids = Object.keys(spec.relations)
  L.push('/** spec.relations 的 runtime 注册表（futureRelations **不在**其中）。 */')
  L.push('export const CANONICAL_RELATION_DEFINITIONS: CanonicalRelationDefinition[] = [')
  ids.forEach((wire, i) => {
    const d = spec.relations[wire]
    const kinds = (list) => list.map((k) => `NodeKind.${enumConstName(k)}`).join(', ')
    const modes = (d.allowedGroupModes ?? []).map((m) => `GroupMode.${enumConstName(m)}`).join(', ')
    L.push(`  new CanonicalRelationDefinition(`)
    L.push(`    Relation.${enumConstName(wire)},`)
    L.push(`    [${kinds(d.fromKinds)}],`)
    L.push(`    [${kinds(d.toKinds)}],`)
    L.push(`    Capability.${enumConstName(d.capability)},`)
    L.push(`    ${d.allowsGroup ? 'true' : 'false'},`)
    L.push(`    [${modes}],`)
    L.push(`    Criticality.${enumConstName(d.defaultCriticality)},`)
    L.push(`  )${i === ids.length - 1 ? '' : ','}`)
  })
  L.push('];')
  L.push('')
  return L.join('\n')
}

// ---------------------------------------------------------------------------
// 目标文件表
// ---------------------------------------------------------------------------

const TARGETS = [
  { path: join(ROOT, 'android/core/src/main/kotlin/com/pdig/core/generated/CanonicalEnums.kt'), content: emitKotlin() },
  { path: join(ROOT, 'ios/Sources/PDIGCore/Generated/CanonicalEnums.swift'), content: emitSwift() },
  { path: join(ROOT, 'harmony/entry/src/main/ets/generated/CanonicalEnums.ets'), content: emitArkTS() },
  { path: join(ROOT, 'harmony/entry/src/main/ets/generated/CanonicalRelations.ets'), content: emitArkTSRelations() },
]

// ---------------------------------------------------------------------------
// main
// ---------------------------------------------------------------------------

const checkMode = process.argv.includes('--check')
let failures = 0
let written = 0

for (const t of TARGETS) {
  const rel = t.path.slice(ROOT.length + 1).replace(/\\/g, '/')
  const existing = existsSync(t.path) ? readFileSync(t.path, 'utf8') : null
  if (checkMode) {
    if (existing === null) {
      console.error(`FAIL  missing generated file: ${rel}`)
      failures++
    } else if (existing !== t.content) {
      console.error(`FAIL  generated file is out of date or hand-edited: ${rel}`)
      failures++
    } else {
      console.log(`ok    ${rel}`)
    }
  } else {
    mkdirSync(dirname(t.path), { recursive: true })
    writeFileSync(t.path, t.content, 'utf8')
    console.log(`write ${rel}`)
    written++
  }
}

if (checkMode) {
  if (failures > 0) {
    console.error(`\nCODEGEN GATE: FAIL (${failures} file(s))`)
    console.error('Run: node tools/codegen/generate.mjs')
    process.exit(1)
  }
  console.log('\nCODEGEN GATE: PASS')
} else {
  console.log(`\nwrote ${written} file(s)`)
}
