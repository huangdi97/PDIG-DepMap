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

function dq(s) {
  return '"' + String(s).replace(/\\/g, '\\\\').replace(/"/g, '\\"') + '"'
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
      L.push(`public enum ${enumName}: String, CaseIterable, Sendable {`)
      for (const e of entries) {
        L.push(`    case ${lowerCamel(e.name)} = ${dq(e.wire)}`)
      }
      L.push('')
      L.push(`    public var wire: String { rawValue }`)
      L.push('}')
      L.push('')
    }
  }

  L.push('public enum ErrorCode: String, CaseIterable, Sendable {')
  for (const c of errors.codes) {
    L.push(`    case ${lowerCamel(enumConstName(c.code))} = ${dq(c.code)}`)
  }
  L.push('')
  L.push('    public var wire: String { rawValue }')
  L.push('')
  L.push('    public var category: String {')
  L.push('        switch self {')
  for (const c of errors.codes) {
    L.push(`        case .${lowerCamel(enumConstName(c.code))}: return ${dq(c.category)}`)
  }
  L.push('        }')
  L.push('    }')
  L.push('')
  L.push('    public var messageKey: String {')
  L.push('        switch self {')
  for (const c of errors.codes) {
    L.push(`        case .${lowerCamel(enumConstName(c.code))}: return ${dq(c.messageKey)}`)
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
    }
  }

  L.push('export enum ErrorCode {')
  errors.codes.forEach((c, i) => {
    const sep = i === errors.codes.length - 1 ? '' : ','
    L.push(`  ${enumConstName(c.code)} = ${quote(c.code)}${sep}`)
  })
  L.push('}')
  L.push('')

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
// 目标文件表
// ---------------------------------------------------------------------------

const TARGETS = [
  { path: join(ROOT, 'android/core/src/main/kotlin/com/pdig/core/generated/CanonicalEnums.kt'), content: emitKotlin() },
  { path: join(ROOT, 'ios/Sources/PDIGCore/Generated/CanonicalEnums.swift'), content: emitSwift() },
  { path: join(ROOT, 'harmony/entry/src/main/ets/generated/CanonicalEnums.ets'), content: emitArkTS() },
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
