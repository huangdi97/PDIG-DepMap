import { describe, expect, it } from 'vitest'
import {
  createDepmapContainer,
  DepmapError,
  fromBase64,
  openDepmapContainer,
  toBase64,
  type DepmapHeader,
} from '../../src/crypto/depmap.ts'
import { goldenNonce, goldenSalt } from '../../src/crypto/golden.ts'

/**
 * Engineering Baseline V1 — DEPMAP_CONTAINER_V1 结构化变异 / Fuzz 套件。
 *
 * 对合法容器做系统性 mutate（header 各字段 / salt / nonce / tag / ciphertext /
 * base64 / JSON 结构 / KDF 参数 / formatVersion），断言 fail-closed 契约：
 *   F1 不 crash（只允许 DepmapError）
 *   F2 不 fail-open（解密成功 ⇒ 明文逐字节等于原文，绝无 partial plaintext）
 *   F3 恶意 KDF 参数在昂贵 Argon2 之前被拒绝（bounds）
 *   F4 wrong password 与密文篡改同为 auth_failed（不可区分）
 *
 * 既有覆盖：crypto/depmap.test.ts（AAD/golden/负向）、unit/crypto-negative.test.ts
 * （30 轮单字符变异 fuzz）、unit/property-fuzz.test.ts（KDF 参数 fuzz）。
 * 本套件补齐「逐字段结构化变异」矩阵。
 */

const PASSWORD = 'mutation-test-password'
const PLAINTEXT = new TextEncoder().encode(JSON.stringify({ graph: 'mutation-test', v: 1 }))
const FIXED_SALT = goldenSalt()
const FIXED_NONCE = goldenNonce()

async function makeContainer(): Promise<{ json: string; header: DepmapHeader }> {
  const res = await createDepmapContainer(PLAINTEXT, PASSWORD, {
    salt: Uint8Array.from(FIXED_SALT),
    nonce: Uint8Array.from(FIXED_NONCE),
  })
  return { json: res.json, header: res.header }
}

function flipBitInBase64(value: string, bitIndex: number): string {
  const bytes = Uint8Array.from(fromBase64(value))
  const idx = bitIndex % bytes.length
  bytes[idx] = (bytes[idx] ?? 0) ^ (1 << (bitIndex % 8))
  return toBase64(bytes)
}

type Mutator = (header: DepmapHeader) => Record<string, unknown>

function cloneHeader(h: DepmapHeader): Record<string, unknown> {
  return JSON.parse(JSON.stringify(h)) as Record<string, unknown>
}

function setPath(h: DepmapHeader, path: string[], value: unknown): Record<string, unknown> {
  const clone = cloneHeader(h)
  let cur: Record<string, unknown> = clone
  for (const seg of path.slice(0, -1)) {
    cur = (cur[seg] ?? {}) as Record<string, unknown>
  }
  cur[path[path.length - 1] ?? ''] = value
  return clone
}

function deleteProp(h: DepmapHeader, key: string): Record<string, unknown> {
  const clone = cloneHeader(h)
  delete clone[key]
  return clone
}

/** 逐字段结构化变异矩阵。 */
const MUTATIONS: Array<{ name: string; mutate: Mutator; expectFastRejection?: boolean }> = [
  // ---- KDF 参数（必须在昂贵 Argon2 前拒绝） ----
  {
    name: 'kdf.memoryKiB = 0（过小）',
    mutate: (h) => setPath(h, ['kdf', 'memoryKiB'], 0),
    expectFastRejection: true,
  },
  {
    name: 'kdf.memoryKiB = 16383（低于下界）',
    mutate: (h) => setPath(h, ['kdf', 'memoryKiB'], 16383),
    expectFastRejection: true,
  },
  {
    name: 'kdf.memoryKiB = 262145（高于上界）',
    mutate: (h) => setPath(h, ['kdf', 'memoryKiB'], 262145),
    expectFastRejection: true,
  },
  {
    name: 'kdf.memoryKiB = 1e9（恶意巨大）',
    mutate: (h) => setPath(h, ['kdf', 'memoryKiB'], 1_000_000_000),
    expectFastRejection: true,
  },
  {
    name: 'kdf.iterations = 0',
    mutate: (h) => setPath(h, ['kdf', 'iterations'], 0),
    expectFastRejection: true,
  },
  {
    name: 'kdf.iterations = 11（高于上界）',
    mutate: (h) => setPath(h, ['kdf', 'iterations'], 11),
    expectFastRejection: true,
  },
  {
    name: 'kdf.iterations = 1e9（恶意巨大）',
    mutate: (h) => setPath(h, ['kdf', 'iterations'], 1_000_000_000),
    expectFastRejection: true,
  },
  {
    name: 'kdf.parallelism = 0',
    mutate: (h) => setPath(h, ['kdf', 'parallelism'], 0),
    expectFastRejection: true,
  },
  {
    name: 'kdf.parallelism = 5（高于上界）',
    mutate: (h) => setPath(h, ['kdf', 'parallelism'], 5),
    expectFastRejection: true,
  },
  {
    name: 'kdf.version = 16（非 v19）',
    mutate: (h) => setPath(h, ['kdf', 'version'], 16),
    expectFastRejection: true,
  },
  {
    name: 'kdf.algorithm 大小写篡改',
    mutate: (h) => setPath(h, ['kdf', 'algorithm'], 'Argon2id'),
    expectFastRejection: true,
  },
  // ---- 协议字段 ----
  {
    name: 'formatVersion = 0',
    mutate: (h) => ({ ...h, formatVersion: 0 }),
    expectFastRejection: true,
  },
  {
    name: 'formatVersion = 2（未来版本拒绝）',
    mutate: (h) => ({ ...h, formatVersion: 2 }),
    expectFastRejection: true,
  },
  { name: 'format 篡改', mutate: (h) => ({ ...h, format: 'depmapX' }), expectFastRejection: true },
  {
    name: 'cipher.algorithm 大小写篡改',
    mutate: (h) => setPath(h, ['cipher', 'algorithm'], 'aes-256-gcm'),
    expectFastRejection: true,
  },
  // ---- 长度违约 ----
  {
    name: 'salt 短 1 字节',
    mutate: (h) => ({
      ...h,
      kdf: { ...h.kdf, salt: toBase64(fromBase64(h.kdf.salt).slice(0, 15)) },
    }),
    expectFastRejection: true,
  },
  {
    name: 'salt 多 1 字节',
    mutate: (h) => ({
      ...h,
      kdf: { ...h.kdf, salt: toBase64(new Uint8Array([...fromBase64(h.kdf.salt), 0])) },
    }),
    expectFastRejection: true,
  },
  {
    name: 'nonce 短 1 字节',
    mutate: (h) => ({
      ...h,
      cipher: { ...h.cipher, nonce: toBase64(fromBase64(h.cipher.nonce).slice(0, 11)) },
    }),
    expectFastRejection: true,
  },
  {
    name: 'nonce 多 1 字节',
    mutate: (h) => ({
      ...h,
      cipher: { ...h.cipher, nonce: toBase64(new Uint8Array([...fromBase64(h.cipher.nonce), 0])) },
    }),
    expectFastRejection: true,
  },
  {
    name: 'tag 短 1 字节',
    mutate: (h) => ({ ...h, tag: toBase64(fromBase64(h.tag).slice(0, 15)) }),
    expectFastRejection: true,
  },
  {
    name: 'tag 多 1 字节',
    mutate: (h) => ({ ...h, tag: toBase64(new Uint8Array([...fromBase64(h.tag), 0])) }),
    expectFastRejection: true,
  },
  // ---- 无效 Base64 / JSON ----
  {
    name: 'salt 非法 Base64',
    mutate: (h) => setPath(h, ['kdf', 'salt'], '@@@@'),
    expectFastRejection: true,
  },
  {
    name: 'ciphertext 非法 Base64',
    mutate: (h) => ({ ...h, ciphertext: '####' }),
    expectFastRejection: true,
  },
  { name: '缺失 kdf 字段', mutate: (h) => deleteProp(h, 'kdf'), expectFastRejection: true },
  { name: '缺失 cipher 字段', mutate: (h) => deleteProp(h, 'cipher'), expectFastRejection: true },
  { name: '缺失 tag 字段', mutate: (h) => deleteProp(h, 'tag'), expectFastRejection: true },
  {
    name: '未知顶层字段（不得 fail-open 篡改语义）',
    mutate: (h) => ({ ...h, injected: 'attacker' }),
  },
  // ---- 认证层（走完整 Argon2 → GCM） ----
  { name: 'tag 1-bit 翻转', mutate: (h) => ({ ...h, tag: flipBitInBase64(h.tag, 3) }) },
  {
    name: 'ciphertext 1-bit 翻转',
    mutate: (h) => ({ ...h, ciphertext: flipBitInBase64(h.ciphertext, 7) }),
  },
  {
    name: 'salt 1-bit 翻转',
    mutate: (h) => ({ ...h, kdf: { ...h.kdf, salt: flipBitInBase64(h.kdf.salt, 5) } }),
  },
  {
    name: 'nonce 1-bit 翻转',
    mutate: (h) => ({ ...h, cipher: { ...h.cipher, nonce: flipBitInBase64(h.cipher.nonce, 2) } }),
  },
  {
    name: 'ciphertext 截断',
    mutate: (h) => ({ ...h, ciphertext: toBase64(fromBase64(h.ciphertext).slice(0, 8)) }),
  },
]

/** fail-closed 契约：要么 DepmapError 拒绝，要么明文逐字节等于原文（无 partial plaintext）。 */
async function expectFailClosed(mutatedJson: string): Promise<void> {
  try {
    const res = await openDepmapContainer(mutatedJson, PASSWORD)
    expect(Buffer.from(res.plaintext).equals(Buffer.from(PLAINTEXT))).toBe(true)
  } catch (e) {
    expect(e).toBeInstanceOf(DepmapError)
  }
}

describe('DEPMAP_CONTAINER_V1 结构化变异 fail-closed（Engineering Baseline V1）', () => {
  it('F1/F2: 全部变异矩阵 —— 不 crash / 不 fail-open / 无 partial plaintext', async () => {
    const { json } = await makeContainer()
    for (const m of MUTATIONS) {
      const mutated = JSON.stringify(m.mutate(JSON.parse(json) as DepmapHeader))
      await expectFailClosed(mutated)
    }
  })

  it('F3: 恶意 KDF 参数在昂贵 Argon2 之前被拒绝（快速路径）', async () => {
    const { json } = await makeContainer()
    const fast = MUTATIONS.filter((m) => m.expectFastRejection)
    expect(fast.length).toBeGreaterThanOrEqual(15)
    const start = Date.now()
    for (const m of fast) {
      const mutated = JSON.stringify(m.mutate(JSON.parse(json) as DepmapHeader))
      try {
        await openDepmapContainer(mutated, PASSWORD)
        throw new Error(`mutation should have been rejected: ${m.name}`)
      } catch (e) {
        expect(e).toBeInstanceOf(DepmapError)
        // bounds 与 invalid_structure 都在 Argon2 之前（结构/边界预校验）
        expect(['bounds', 'invalid_structure']).toContain((e as DepmapError).code)
      }
    }
    // 全部快速拒绝；若真的跑了 Argon2（65536KiB × N）绝不可能 < 5s
    expect(Date.now() - start).toBeLessThan(5000)
  })

  it('F4: wrong password 与密文篡改同为 auth_failed（不可区分）', async () => {
    const { json } = await makeContainer()
    const openError = async (containerJson: string, password: string): Promise<unknown> => {
      try {
        await openDepmapContainer(containerJson, password)
        return null
      } catch (e) {
        return e
      }
    }
    const wrongPw = await openError(json, 'wrong-password')
    const tampered = await openError(
      JSON.stringify({
        ...(JSON.parse(json) as object),
        ciphertext: flipBitInBase64((JSON.parse(json) as DepmapHeader).ciphertext, 11),
      }),
      PASSWORD,
    )
    expect(wrongPw).toBeInstanceOf(DepmapError)
    expect((wrongPw as DepmapError).code).toBe('auth_failed')
    expect(tampered).toBeInstanceOf(DepmapError)
    expect((tampered as DepmapError).code).toBe('auth_failed')
  })

  it('F5: 无效 JSON / 非法 UTF-8 payload → invalid_json，不 crash', async () => {
    await expect(openDepmapContainer('{not-json', PASSWORD)).rejects.toMatchObject({
      code: 'invalid_json',
    })
    const badUtf8 = Buffer.from([0x7b, 0xff, 0xfe, 0x7d]).toString('latin1')
    await expect(openDepmapContainer(badUtf8, PASSWORD)).rejects.toMatchObject({
      code: 'invalid_json',
    })
  })

  it('F6: Golden Vector 不受变异轮影响（回归锚点不变）', async () => {
    const { createGoldenContainer, GOLDEN_PASSWORD, GOLDEN_PLAINTEXT } =
      await import('../../src/crypto/golden.ts')
    const golden = await createGoldenContainer()
    const opened = await openDepmapContainer(golden.json, GOLDEN_PASSWORD)
    expect(Buffer.from(opened.plaintext).toString()).toBe(GOLDEN_PLAINTEXT)
  })
})
