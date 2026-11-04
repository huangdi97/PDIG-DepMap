/**
 * 浏览器 `node:crypto` 垫片。
 *
 * 为什么需要：Core 的 ObservationFingerprint 使用同步 `createHash('sha256')` /
 * `createHmac('sha256', secret)`。WebCrypto 只有异步 digest，而 Core 的
 * Repository 链路是同步的（SQLite 同步驱动），因此这里提供纯 TypeScript 的
 * 同步 SHA-256 / HMAC-SHA256 实现。
 *
 * 边界：
 * - 只实现 Core 实际使用的子集（sha256 hex）。
 * - 必须与 Node 输出逐字节一致（tests/unit/node-crypto-shim.test.ts 对照 crypto 模块）。
 * - 未实现的分支（createCipheriv 等）显式抛错，绝不静默降级。
 */

const K = new Uint32Array([
  0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
  0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
  0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
  0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
  0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
  0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
  0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
  0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208, 0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2,
])

function rotr(x: number, n: number): number {
  return (x >>> n) | (x << (32 - n))
}

function sha256Bytes(message: Uint8Array): Uint8Array {
  const bitLength = message.length * 8
  const withPadding = ((message.length + 9 + 63) >> 6) << 6
  const buffer = new Uint8Array(withPadding)
  buffer.set(message)
  buffer[message.length] = 0x80
  const view = new DataView(buffer.buffer)
  // 长度用 64bit 大端表示；本实现只支持 < 2^32 位的输入（远超浏览器内图数据规模）。
  view.setUint32(withPadding - 8, Math.floor(bitLength / 0x100000000), false)
  view.setUint32(withPadding - 4, bitLength >>> 0, false)

  const H = new Uint32Array([
    0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a, 0x510e527f, 0x9b05688c, 0x1f83d9ab, 0x5be0cd19,
  ])
  const W = new Uint32Array(64)

  for (let offset = 0; offset < withPadding; offset += 64) {
    for (let i = 0; i < 16; i++) W[i] = view.getUint32(offset + i * 4, false)
    for (let i = 16; i < 64; i++) {
      const w15 = W[i - 15] as number
      const w2 = W[i - 2] as number
      const s0 = rotr(w15, 7) ^ rotr(w15, 18) ^ (w15 >>> 3)
      const s1 = rotr(w2, 17) ^ rotr(w2, 19) ^ (w2 >>> 10)
      W[i] = (W[i - 16]! + s0 + W[i - 7]! + s1) >>> 0
    }
    let a = H[0]!
    let b = H[1]!
    let c = H[2]!
    let d = H[3]!
    let e = H[4]!
    let f = H[5]!
    let g = H[6]!
    let h = H[7]!
    for (let i = 0; i < 64; i++) {
      const S1 = rotr(e, 6) ^ rotr(e, 11) ^ rotr(e, 25)
      const ch = (e & f) ^ (~e & g)
      const temp1 = (h + S1 + ch + K[i]! + W[i]!) >>> 0
      const S0 = rotr(a, 2) ^ rotr(a, 13) ^ rotr(a, 22)
      const maj = (a & b) ^ (a & c) ^ (b & c)
      const temp2 = (S0 + maj) >>> 0
      h = g
      g = f
      f = e
      e = (d + temp1) >>> 0
      d = c
      c = b
      b = a
      a = (temp1 + temp2) >>> 0
    }
    H[0] = (H[0]! + a) >>> 0
    H[1] = (H[1]! + b) >>> 0
    H[2] = (H[2]! + c) >>> 0
    H[3] = (H[3]! + d) >>> 0
    H[4] = (H[4]! + e) >>> 0
    H[5] = (H[5]! + f) >>> 0
    H[6] = (H[6]! + g) >>> 0
    H[7] = (H[7]! + h) >>> 0
  }

  const out = new Uint8Array(32)
  const outView = new DataView(out.buffer)
  for (let i = 0; i < 8; i++) outView.setUint32(i * 4, H[i]!, false)
  return out
}

export function toHex(bytes: Uint8Array): string {
  let s = ''
  for (const b of bytes) s += b.toString(16).padStart(2, '0')
  return s
}

function toBytes(data: string | Uint8Array): Uint8Array {
  return typeof data === 'string' ? new TextEncoder().encode(data) : data
}

function concat(parts: Uint8Array[]): Uint8Array {
  const total = parts.reduce((n, p) => n + p.length, 0)
  const out = new Uint8Array(total)
  let offset = 0
  for (const p of parts) {
    out.set(p, offset)
    offset += p.length
  }
  return out
}

type DigestEncoding = 'hex' | 'base64'

function encodeDigest(bytes: Uint8Array, encoding: DigestEncoding): string {
  if (encoding === 'hex') return toHex(bytes)
  if (encoding === 'base64') {
    let binary = ''
    for (const b of bytes) binary += String.fromCharCode(b)
    return btoa(binary)
  }
  throw new Error(`unsupported digest encoding: ${String(encoding)}`)
}

class Sha256Hash {
  private readonly chunks: Uint8Array[] = []
  private finalized = false

  update(data: string | Uint8Array): this {
    this.chunks.push(toBytes(data))
    return this
  }

  digest(encoding: DigestEncoding = 'hex'): string {
    if (this.finalized) throw new Error('digest already called')
    this.finalized = true
    return encodeDigest(sha256Bytes(concat(this.chunks)), encoding)
  }
}

class Sha256Hmac {
  private readonly key: Uint8Array
  private readonly chunks: Uint8Array[] = []
  private finalized = false

  constructor(key: string | Uint8Array) {
    const raw = toBytes(key)
    this.key = raw.length > 64 ? sha256Bytes(raw) : raw
  }

  update(data: string | Uint8Array): this {
    this.chunks.push(toBytes(data))
    return this
  }

  digest(encoding: DigestEncoding = 'hex'): string {
    if (this.finalized) throw new Error('digest already called')
    this.finalized = true
    const block = new Uint8Array(64)
    block.set(this.key)
    const inner = new Uint8Array(64)
    const outer = new Uint8Array(64)
    for (let i = 0; i < 64; i++) {
      inner[i] = (block[i]! ^ 0x36) & 0xff
      outer[i] = (block[i]! ^ 0x5c) & 0xff
    }
    const innerHash = sha256Bytes(concat([inner, ...this.chunks]))
    return encodeDigest(sha256Bytes(concat([outer, innerHash])), encoding)
  }
}

export function createHash(algorithm: string): Sha256Hash {
  if (algorithm !== 'sha256') throw new Error(`unsupported hash algorithm: ${algorithm}`)
  return new Sha256Hash()
}

export function createHmac(algorithm: string, key: string | Uint8Array): Sha256Hmac {
  if (algorithm !== 'sha256') throw new Error(`unsupported hmac algorithm: ${algorithm}`)
  return new Sha256Hmac(key)
}

export function randomBytes(size: number): Uint8Array {
  const out = new Uint8Array(size)
  crypto.getRandomValues(out)
  return out
}

export function createCipheriv(): never {
  throw new Error(
    'createCipheriv is not available in the browser build; use web/src/crypto/depmap-web.ts (WebCrypto AES-GCM)',
  )
}

export function createDecipheriv(): never {
  throw new Error(
    'createDecipheriv is not available in the browser build; use web/src/crypto/depmap-web.ts (WebCrypto AES-GCM)',
  )
}
