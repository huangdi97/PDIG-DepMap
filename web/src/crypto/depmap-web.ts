/**
 * DEPMAP_CONTAINER_V1 的浏览器实现（WebCrypto AES-256-GCM + hash-wasm Argon2id）。
 *
 * 与 `core/src/crypto/depmap.ts`（Node）**逐字节兼容**：
 * Argon2id v19 / AES-256-GCM / AAD = RFC8785-JCS(header 子集) / RFC4648 base64（带 padding）。
 *
 * 为什么另写一份而不是直接引 Core：Core 使用 `node:crypto` 的同步
 * createCipheriv/createDecipheriv，浏览器没有等价同步 API。此处保持协议常量与
 * 结构校验逻辑镜像，并由 tests/unit/depmap-web-golden.test.ts 断言：
 * 1) 常量与 Core 完全一致；2) 冻结 Golden Vector 的 derivedKey / ciphertext / tag 精确匹配。
 */
import { argon2id } from 'hash-wasm'
import { jcsStringify } from '@core/crypto/jcs.ts'

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
  tagLen: 16,
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
  ciphertextMaxBytes: 64 * 1024 * 1024,
} as const

export type DepmapErrorCode =
  'invalid_json' | 'invalid_structure' | 'bounds' | 'kdf' | 'auth_failed' | 'unsupported'

export class DepmapWebError extends Error {
  readonly code: DepmapErrorCode

  constructor(code: DepmapErrorCode, message: string) {
    super(message)
    this.name = 'DepmapWebError'
    this.code = code
  }
}

export interface KdfParams {
  memoryKiB: number
  iterations: number
  parallelism: number
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
  cipher: { algorithm: string; nonce: string }
  ciphertext: string
  tag: string
}

// ---------------------------------------------------------------------------
// base64（RFC 4648 standard with padding）
// ---------------------------------------------------------------------------

const B64 = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/'

export function toBase64(bytes: Uint8Array): string {
  let out = ''
  for (let i = 0; i < bytes.length; i += 3) {
    const b0 = bytes[i]!
    const b1 = bytes[i + 1]
    const b2 = bytes[i + 2]
    out += B64[b0 >> 2]
    out += B64[((b0 & 0x03) << 4) | ((b1 ?? 0) >> 4)]
    out += b1 === undefined ? '=' : B64[((b1 & 0x0f) << 2) | ((b2 ?? 0) >> 6)]
    out += b2 === undefined ? '=' : B64[b2 & 0x3f]
  }
  return out
}

export function fromBase64(value: string): Uint8Array {
  const clean = value.replace(/[^A-Za-z0-9+/]/g, '')
  const codeOf = (char: string | undefined): number => (char === undefined ? -1 : B64.indexOf(char))
  const bytes: number[] = []
  for (let i = 0; i < clean.length; i += 4) {
    const c0 = codeOf(clean[i])
    const c1 = codeOf(clean[i + 1])
    const c2 = codeOf(clean[i + 2])
    const c3 = codeOf(clean[i + 3])
    if (c0 < 0 || c1 < 0) throw new DepmapWebError('invalid_structure', 'invalid base64 padding')
    bytes.push((c0 << 2) | (c1 >> 4))
    if (c2 >= 0) bytes.push(((c1 & 0x0f) << 4) | (c2 >> 2))
    if (c3 >= 0) bytes.push(((c2 & 0x03) << 6) | c3)
  }
  return new Uint8Array(bytes)
}

export function toHex(bytes: Uint8Array): string {
  let out = ''
  for (const b of bytes) out += b.toString(16).padStart(2, '0')
  return out
}

export function fromHex(hex: string): Uint8Array {
  const out = new Uint8Array(hex.length / 2)
  for (let i = 0; i < out.length; i++) out[i] = Number.parseInt(hex.slice(i * 2, i * 2 + 2), 16)
  return out
}

// ---------------------------------------------------------------------------
// AAD
// ---------------------------------------------------------------------------

export function computeAad(
  header: Pick<DepmapHeader, 'format' | 'formatVersion' | 'kdf' | 'cipher'>,
): Uint8Array {
  const aadSource = {
    format: header.format,
    formatVersion: header.formatVersion,
    kdf: {
      algorithm: header.kdf.algorithm,
      version: header.kdf.version,
      salt: header.kdf.salt,
      memoryKiB: header.kdf.memoryKiB,
      iterations: header.kdf.iterations,
      parallelism: header.kdf.parallelism,
    },
    cipher: { algorithm: header.cipher.algorithm, nonce: header.cipher.nonce },
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
    throw new DepmapWebError('invalid_structure', `header field ${field} must be an integer`)
  }
  return v
}

function assertString(v: unknown, field: string): string {
  if (typeof v !== 'string') {
    throw new DepmapWebError('invalid_structure', `header field ${field} must be a string`)
  }
  return v
}

export function parseDepmapHeader(json: string): DepmapHeader {
  let raw: unknown
  try {
    raw = JSON.parse(json)
  } catch {
    throw new DepmapWebError('invalid_json', 'container is not valid JSON')
  }
  if (!isPlainObject(raw))
    throw new DepmapWebError('invalid_structure', 'container must be an object')
  const kdf = raw['kdf']
  const cipher = raw['cipher']
  if (!isPlainObject(kdf) || !isPlainObject(cipher)) {
    throw new DepmapWebError('invalid_structure', 'kdf/cipher must be objects')
  }
  return {
    format: assertString(raw['format'], 'format'),
    formatVersion: assertInt(raw['formatVersion'], 'formatVersion'),
    kdf: {
      algorithm: assertString(kdf['algorithm'], 'kdf.algorithm'),
      version: assertInt(kdf['version'], 'kdf.version'),
      salt: assertString(kdf['salt'], 'kdf.salt'),
      memoryKiB: assertInt(kdf['memoryKiB'], 'kdf.memoryKiB'),
      iterations: assertInt(kdf['iterations'], 'kdf.iterations'),
      parallelism: assertInt(kdf['parallelism'], 'kdf.parallelism'),
    },
    cipher: {
      algorithm: assertString(cipher['algorithm'], 'cipher.algorithm'),
      nonce: assertString(cipher['nonce'], 'cipher.nonce'),
    },
    ciphertext: assertString(raw['ciphertext'], 'ciphertext'),
    tag: assertString(raw['tag'], 'tag'),
  }
}

/** 未来版本 fail closed：绝不尝试“尽量读取”。 */
export function assertSupportedVersion(header: DepmapHeader): void {
  if (header.format !== DEPMAP_FORMAT) {
    throw new DepmapWebError('unsupported', `unsupported container format: ${header.format}`)
  }
  if (header.formatVersion > DEPMAP_FORMAT_VERSION) {
    throw new DepmapWebError(
      'unsupported',
      `container formatVersion ${header.formatVersion} is newer than supported ${DEPMAP_FORMAT_VERSION}`,
    )
  }
}

export function validateDepmapBounds(header: DepmapHeader): void {
  const B = DEPMAP_V1_BOUNDS
  const fail = (m: string): never => {
    throw new DepmapWebError('bounds', m)
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
  if (fromBase64(header.cipher.nonce).length !== B.nonceLen)
    fail('cipher.nonce must decode to 12 bytes')
  if (fromBase64(header.tag).length !== B.tagLen) fail('tag must decode to 16 bytes')
  const ct = fromBase64(header.ciphertext)
  if (ct.length === 0) fail('ciphertext must not be empty')
  if (ct.length > B.ciphertextMaxBytes) fail('ciphertext exceeds 64 MiB limit')
  if (header.cipher.algorithm !== 'AES-256-GCM') fail('cipher.algorithm must be AES-256-GCM')
}

// ---------------------------------------------------------------------------
// Argon2id
// ---------------------------------------------------------------------------

export async function deriveFileEncryptionKey(
  password: string,
  salt: Uint8Array,
  params: KdfParams,
): Promise<Uint8Array> {
  try {
    return await argon2id({
      password: new TextEncoder().encode(password),
      salt,
      parallelism: params.parallelism,
      iterations: params.iterations,
      memorySize: params.memoryKiB,
      hashLength: DEPMAP_V1_DEFAULTS.keyLen,
      outputType: 'binary',
    })
  } catch (e) {
    const detail = e instanceof Error ? e.message : String(e)
    throw new DepmapWebError('kdf', `argon2id key derivation failed: ${detail}`)
  }
}

// ---------------------------------------------------------------------------
// AES-256-GCM（WebCrypto）
// ---------------------------------------------------------------------------

async function importAesKey(key: Uint8Array): Promise<CryptoKey> {
  if (key.length !== 32) throw new DepmapWebError('kdf', 'derived key must be 32 bytes')
  return crypto.subtle.importKey('raw', toArrayBuffer(key), { name: 'AES-GCM' }, false, [
    'encrypt',
    'decrypt',
  ])
}

function toArrayBuffer(bytes: Uint8Array): ArrayBuffer {
  const out = new ArrayBuffer(bytes.byteLength)
  new Uint8Array(out).set(bytes)
  return out
}

function randomBytes(n: number): Uint8Array {
  return crypto.getRandomValues(new Uint8Array(n))
}

export interface SealOptions {
  salt: Uint8Array
  kdf: KdfParams
  nonce?: Uint8Array
}

/** 用已派生的 key 加密 payload，产出完整 V1 container JSON（fresh nonce）。 */
export async function sealWithKey(
  key: Uint8Array,
  plaintext: Uint8Array,
  options: SealOptions,
): Promise<{ json: string; header: DepmapHeader }> {
  const nonce = options.nonce ?? randomBytes(DEPMAP_V1_DEFAULTS.nonceLen)
  if (nonce.length !== DEPMAP_V1_BOUNDS.nonceLen) {
    throw new DepmapWebError('bounds', 'nonce must be 12 bytes')
  }
  const header: DepmapHeader = {
    format: DEPMAP_FORMAT,
    formatVersion: DEPMAP_FORMAT_VERSION,
    kdf: {
      algorithm: 'argon2id',
      version: ARGON2_VERSION_19,
      salt: toBase64(options.salt),
      memoryKiB: options.kdf.memoryKiB,
      iterations: options.kdf.iterations,
      parallelism: options.kdf.parallelism,
    },
    cipher: { algorithm: 'AES-256-GCM', nonce: toBase64(nonce) },
    ciphertext: '',
    tag: '',
  }
  const aad = computeAad(header)
  const cryptoKey = await importAesKey(key)
  const sealed = new Uint8Array(
    await crypto.subtle.encrypt(
      {
        name: 'AES-GCM',
        iv: toArrayBuffer(nonce),
        additionalData: toArrayBuffer(aad),
        tagLength: 128,
      },
      cryptoKey,
      toArrayBuffer(plaintext),
    ),
  )
  const ciphertext = sealed.slice(0, sealed.length - DEPMAP_V1_BOUNDS.tagLen)
  const tag = sealed.slice(sealed.length - DEPMAP_V1_BOUNDS.tagLen)
  const full: DepmapHeader = { ...header, ciphertext: toBase64(ciphertext), tag: toBase64(tag) }
  return { json: jcsStringify(full), header: full }
}

export async function createDepmapContainer(
  plaintext: Uint8Array,
  password: string,
  opts: Partial<KdfParams> & { salt?: Uint8Array; nonce?: Uint8Array } = {},
): Promise<{ json: string; header: DepmapHeader; derivedKeyHex: string }> {
  const salt = opts.salt ?? randomBytes(DEPMAP_V1_DEFAULTS.saltLen)
  if (salt.length !== DEPMAP_V1_BOUNDS.saltLen)
    throw new DepmapWebError('bounds', 'salt must be 16 bytes')
  const kdf: KdfParams = {
    memoryKiB: opts.memoryKiB ?? DEPMAP_V1_DEFAULTS.memoryKiB,
    iterations: opts.iterations ?? DEPMAP_V1_DEFAULTS.iterations,
    parallelism: opts.parallelism ?? DEPMAP_V1_DEFAULTS.parallelism,
  }
  const key = await deriveFileEncryptionKey(password, salt, kdf)
  const sealed = await sealWithKey(key, plaintext, {
    salt,
    kdf,
    ...(opts.nonce === undefined ? {} : { nonce: opts.nonce }),
  })
  return { json: sealed.json, header: sealed.header, derivedKeyHex: toHex(key) }
}

export interface OpenDepmapResult {
  plaintext: Uint8Array
  header: DepmapHeader
  key: Uint8Array
  salt: Uint8Array
  kdf: KdfParams
}

export async function openDepmapContainer(
  json: string,
  password: string,
): Promise<OpenDepmapResult> {
  const header = parseDepmapHeader(json)
  assertSupportedVersion(header)
  validateDepmapBounds(header) // KDF 之前完成全部边界检查
  const salt = fromBase64(header.kdf.salt)
  const kdf: KdfParams = {
    memoryKiB: header.kdf.memoryKiB,
    iterations: header.kdf.iterations,
    parallelism: header.kdf.parallelism,
  }
  const key = await deriveFileEncryptionKey(password, salt, kdf)
  const aad = computeAad(header)
  let plaintext: Uint8Array
  try {
    const cryptoKey = await importAesKey(key)
    const ciphertextBytes = fromBase64(header.ciphertext)
    const tagBytes = fromBase64(header.tag)
    const combined = new Uint8Array(ciphertextBytes.length + tagBytes.length)
    combined.set(ciphertextBytes)
    combined.set(tagBytes, ciphertextBytes.length)
    plaintext = new Uint8Array(
      await crypto.subtle.decrypt(
        {
          name: 'AES-GCM',
          iv: toArrayBuffer(fromBase64(header.cipher.nonce)),
          additionalData: toArrayBuffer(aad),
          tagLength: 128,
        },
        cryptoKey,
        toArrayBuffer(combined),
      ),
    )
  } catch {
    throw new DepmapWebError(
      'auth_failed',
      'decryption authentication failed (wrong password or tampered container)',
    )
  }
  return { plaintext, header, key, salt, kdf }
}
