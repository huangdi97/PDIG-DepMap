import { argon2id } from 'hash-wasm'
import { createCipheriv, createDecipheriv } from 'node:crypto'
import { jcsStringify } from './jcs.ts'

/**
 * DEPMAP_CONTAINER_V1 — 口令派生文件加密 (CANONICAL_DESIGN §8.5-§8.9)
 *
 * password → Argon2id(v19) → fileEncryptionKey(32B) → AES-256-GCM
 * AAD = UTF8(RFC8785-JCS({format, formatVersion, kdf, cipher}))，ciphertext/tag 不进 AAD。
 * 解密前 header 一律不可信：structural → bounds → Argon2id → GCM auth。
 */

// ---------------------------------------------------------------------------
// 常量与边界
// ---------------------------------------------------------------------------

export const DEPMAP_FORMAT = 'depmap'
export const DEPMAP_FORMAT_VERSION = 1
export const ARGON2_VERSION_19 = 19

export const DEPMAP_V1_DEFAULTS = {
  memoryKiB: 65536,
  iterations: 3,
  parallelism: 1,
  keyLen: 32,
  saltLen: 16,
  nonceLen: 12,
  tagLen: 16
} as const

export const DEPMAP_V1_BOUNDS = {
  memoryKiBMin: 16384,
  memoryKiBMax: 262144,
  iterationsMin: 1,
  iterationsMax: 10,
  parallelismMin: 1,
  parallelismMax: 4,
  saltLen: 16,
  nonceLen: 12,
  tagLen: 16,
  ciphertextMaxBytes: 64 * 1024 * 1024
} as const

export type DepmapErrorCode = 'invalid_json' | 'invalid_structure' | 'bounds' | 'kdf' | 'auth_failed'

export class DepmapError extends Error {
  readonly code: DepmapErrorCode
  constructor(code: DepmapErrorCode, message: string) {
    super(message)
    this.name = 'DepmapError'
    this.code = code
  }
}

export interface DepmapHeader {
  format: string
  formatVersion: number
  kdf: {
    algorithm: string
    version: number
    salt: string
    memoryKiB: number
    iterations: number
    parallelism: number
  }
  cipher: {
    algorithm: string
    nonce: string
  }
  ciphertext: string
  tag: string
}

// ---------------------------------------------------------------------------
// base64 (RFC 4648 standard with padding)
// ---------------------------------------------------------------------------

export function toBase64(bytes: Uint8Array): string {
  return Buffer.from(bytes).toString('base64')
}

export function fromBase64(s: string): Uint8Array {
  return new Uint8Array(Buffer.from(s, 'base64'))
}

export function toHex(bytes: Uint8Array): string {
  return Buffer.from(bytes).toString('hex')
}

// ---------------------------------------------------------------------------
// AAD
// ---------------------------------------------------------------------------

export function computeAad(header: Pick<DepmapHeader, 'format' | 'formatVersion' | 'kdf' | 'cipher'>): Uint8Array {
  const aadSource = {
    format: header.format,
    formatVersion: header.formatVersion,
    kdf: {
      algorithm: header.kdf.algorithm,
      version: header.kdf.version,
      salt: header.kdf.salt,
      memoryKiB: header.kdf.memoryKiB,
      iterations: header.kdf.iterations,
      parallelism: header.kdf.parallelism
    },
    cipher: {
      algorithm: header.cipher.algorithm,
      nonce: header.cipher.nonce
    }
  }
  return new TextEncoder().encode(jcsStringify(aadSource))
}

// ---------------------------------------------------------------------------
// 结构与边界校验（认证前全部不可信）
// ---------------------------------------------------------------------------

function isPlainObject(v: unknown): v is Record<string, unknown> {
  return typeof v === 'object' && v !== null && !Array.isArray(v)
}

function assertInt(v: unknown, field: string): number {
  if (typeof v !== 'number' || !Number.isSafeInteger(v)) {
    throw new DepmapError('invalid_structure', `header field ${field} must be an integer`)
  }
  return v
}

function assertString(v: unknown, field: string): string {
  if (typeof v !== 'string') {
    throw new DepmapError('invalid_structure', `header field ${field} must be a string`)
  }
  return v
}

/** 解析 + 结构校验（不执行 KDF）。 */
export function parseDepmapHeader(json: string): DepmapHeader {
  let raw: unknown
  try {
    raw = JSON.parse(json)
  } catch {
    throw new DepmapError('invalid_json', 'container is not valid JSON')
  }
  if (!isPlainObject(raw)) throw new DepmapError('invalid_structure', 'container must be a JSON object')
  const obj = raw as Record<string, unknown>
  const kdf = obj.kdf
  const cipher = obj.cipher
  if (!isPlainObject(kdf) || !isPlainObject(cipher)) {
    throw new DepmapError('invalid_structure', 'kdf/cipher must be objects')
  }
  return {
    format: assertString(obj.format, 'format'),
    formatVersion: assertInt(obj.formatVersion, 'formatVersion'),
    kdf: {
      algorithm: assertString(kdf.algorithm, 'kdf.algorithm'),
      version: assertInt(kdf.version, 'kdf.version'),
      salt: assertString(kdf.salt, 'kdf.salt'),
      memoryKiB: assertInt(kdf.memoryKiB, 'kdf.memoryKiB'),
      iterations: assertInt(kdf.iterations, 'kdf.iterations'),
      parallelism: assertInt(kdf.parallelism, 'kdf.parallelism')
    },
    cipher: {
      algorithm: assertString(cipher.algorithm, 'cipher.algorithm'),
      nonce: assertString(cipher.nonce, 'cipher.nonce')
    },
    ciphertext: assertString(obj.ciphertext, 'ciphertext'),
    tag: assertString(obj.tag, 'tag')
  }
}

/** V1 边界校验：恶意 container 不得触发超大内存 KDF。 */
export function validateDepmapBounds(header: DepmapHeader): void {
  const B = DEPMAP_V1_BOUNDS
  const fail = (m: string): never => {
    throw new DepmapError('bounds', m)
  }
  if (header.format !== DEPMAP_FORMAT) fail(`format must be "${DEPMAP_FORMAT}"`)
  if (header.formatVersion !== DEPMAP_FORMAT_VERSION) fail('formatVersion must be 1')
  if (header.kdf.algorithm !== 'argon2id') fail('kdf.algorithm must be argon2id')
  if (header.kdf.version !== ARGON2_VERSION_19) fail('kdf.version must be 19')
  const { memoryKiB, iterations, parallelism } = header.kdf
  if (memoryKiB < B.memoryKiBMin || memoryKiB > B.memoryKiBMax) {
    fail(`kdf.memoryKiB out of bounds [${B.memoryKiBMin},${B.memoryKiBMax}]`)
  }
  if (iterations < B.iterationsMin || iterations > B.iterationsMax) {
    fail(`kdf.iterations out of bounds [${B.iterationsMin},${B.iterationsMax}]`)
  }
  if (parallelism < B.parallelismMin || parallelism > B.parallelismMax) {
    fail(`kdf.parallelism out of bounds [${B.parallelismMin},${B.parallelismMax}]`)
  }
  if (fromBase64(header.kdf.salt).length !== B.saltLen) fail('kdf.salt must decode to 16 bytes')
  if (fromBase64(header.cipher.nonce).length !== B.nonceLen) fail('cipher.nonce must decode to 12 bytes')
  if (fromBase64(header.tag).length !== B.tagLen) fail('tag must decode to 16 bytes')
  const ct = fromBase64(header.ciphertext)
  if (ct.length === 0) fail('ciphertext must not be empty')
  if (ct.length > B.ciphertextMaxBytes) fail('ciphertext exceeds 64 MiB limit')
  if (header.cipher.algorithm !== 'AES-256-GCM') fail('cipher.algorithm must be AES-256-GCM')
}

// ---------------------------------------------------------------------------
// Argon2id 派生
// ---------------------------------------------------------------------------

export async function deriveFileEncryptionKey(
  password: string,
  salt: Uint8Array,
  memoryKiB: number,
  iterations: number,
  parallelism: number
): Promise<Uint8Array> {
  // password 使用精确 UTF-8 字节，不做 Unicode 归一化
  return argon2id({
    password: new TextEncoder().encode(password),
    salt,
    parallelism,
    iterations,
    memorySize: memoryKiB,
    hashLength: DEPMAP_V1_DEFAULTS.keyLen,
    outputType: 'binary'
  })
}

// ---------------------------------------------------------------------------
// 创建 / 打开容器
// ---------------------------------------------------------------------------

export interface CreateDepmapOptions {
  salt?: Uint8Array
  nonce?: Uint8Array
  memoryKiB?: number
  iterations?: number
  parallelism?: number
}

export interface DepmapContainerResult {
  json: string
  header: DepmapHeader
  derivedKeyHex: string
}

export async function createDepmapContainer(
  plaintext: Uint8Array,
  password: string,
  opts: CreateDepmapOptions = {}
): Promise<DepmapContainerResult> {
  const salt = opts.salt ?? randomBytes(DEPMAP_V1_DEFAULTS.saltLen)
  const nonce = opts.nonce ?? randomBytes(DEPMAP_V1_DEFAULTS.nonceLen)
  const memoryKiB = opts.memoryKiB ?? DEPMAP_V1_DEFAULTS.memoryKiB
  const iterations = opts.iterations ?? DEPMAP_V1_DEFAULTS.iterations
  const parallelism = opts.parallelism ?? DEPMAP_V1_DEFAULTS.parallelism

  if (salt.length !== DEPMAP_V1_BOUNDS.saltLen) throw new DepmapError('bounds', 'salt must be 16 bytes')
  if (nonce.length !== DEPMAP_V1_BOUNDS.nonceLen) throw new DepmapError('bounds', 'nonce must be 12 bytes')

  const key = await deriveFileEncryptionKey(password, salt, memoryKiB, iterations, parallelism)
  const header: DepmapHeader = {
    format: DEPMAP_FORMAT,
    formatVersion: DEPMAP_FORMAT_VERSION,
    kdf: {
      algorithm: 'argon2id',
      version: ARGON2_VERSION_19,
      salt: toBase64(salt),
      memoryKiB,
      iterations,
      parallelism
    },
    cipher: { algorithm: 'AES-256-GCM', nonce: toBase64(nonce) },
    ciphertext: '',
    tag: ''
  }
  const aad = computeAad(header)
  const cipher = createCipheriv('aes-256-gcm', key, nonce, { authTagLength: 16 })
  cipher.setAAD(aad)
  const ciphertext = Buffer.concat([cipher.update(plaintext), cipher.final()])
  const tag = cipher.getAuthTag()

  const full: DepmapHeader = { ...header, ciphertext: toBase64(ciphertext), tag: toBase64(tag) }
  // 容器文件本身也用 JCS 序列化 → deterministic 输出
  return { json: jcsStringify(full), header: full, derivedKeyHex: toHex(key) }
}

export interface OpenDepmapResult {
  plaintext: Uint8Array
  header: DepmapHeader
}

export async function openDepmapContainer(json: string, password: string): Promise<OpenDepmapResult> {
  const header = parseDepmapHeader(json)
  validateDepmapBounds(header) // KDF 之前完成全部边界检查
  const key = await deriveFileEncryptionKey(
    password,
    fromBase64(header.kdf.salt),
    header.kdf.memoryKiB,
    header.kdf.iterations,
    header.kdf.parallelism
  )
  const aad = computeAad(header)
  let plaintext: Buffer
  try {
    const decipher = createDecipheriv('aes-256-gcm', key, fromBase64(header.cipher.nonce), {
      authTagLength: 16
    })
    decipher.setAAD(aad)
    decipher.setAuthTag(fromBase64(header.tag))
    plaintext = Buffer.concat([decipher.update(fromBase64(header.ciphertext)), decipher.final()])
  } catch {
    throw new DepmapError('auth_failed', 'decryption authentication failed (wrong password or tampered container)')
  }
  return { plaintext: new Uint8Array(plaintext), header }
}

function randomBytes(n: number): Uint8Array {
  return new Uint8Array(crypto.getRandomValues(new Uint8Array(n)))
}
