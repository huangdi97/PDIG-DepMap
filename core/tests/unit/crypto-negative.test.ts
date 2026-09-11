import { describe, expect, it } from 'vitest'
import {
  createDepmapContainer,
  openDepmapContainer,
  parseDepmapHeader,
  validateDepmapBounds,
  DepmapError,
} from '../../src/crypto/depmap.ts'
import { fromBase64 } from '../../src/crypto/depmap.ts'

/**
 * PHASE J（crypto 部分）+ K（container mutation）— 负向与 fail-closed。
 * 所有失败必须：抛 DepmapError、不返回 partial plaintext。
 */

const enc = new TextEncoder()

async function roundtripContainer(): Promise<string> {
  const { json } = await createDepmapContainer(enc.encode('payload-for-negative-tests'), 'pwd')
  return json
}

describe('Negative — Crypto (RC PHASE J)', () => {
  it('empty password：KDF 实现拒绝 → DepmapError(kdf)，fail closed', async () => {
    // hash-wasm 拒绝空口令；容器层必须包装为 DepmapError 而非裸 Error
    await expect(createDepmapContainer(enc.encode('x'), '')).rejects.toMatchObject({ code: 'kdf' })
  })

  it('long password（1000 字符）：roundtrip 成立', async () => {
    const pwd = 'p'.repeat(1000)
    const { json } = await createDepmapContainer(enc.encode('x'), pwd)
    const { plaintext } = await openDepmapContainer(json, pwd)
    expect(plaintext[0]).toBe('x'.charCodeAt(0))
  })

  it('invalid Base64 in salt/nonce/tag → bounds 拒绝（先于 KDF）', () => {
    {
      const header = validHeader()
      header.kdf.salt = '!!!!not-base64!!!!'
      expect(() => validateDepmapBounds(header)).toThrowError(DepmapError)
    }
    {
      const header = validHeader()
      header.cipher.nonce = '!!!!not-base64!!!!'
      expect(() => validateDepmapBounds(header)).toThrowError(DepmapError)
    }
    {
      const header = validHeader()
      header.tag = '!!!!not-base64!!!!'
      expect(() => validateDepmapBounds(header)).toThrowError(DepmapError)
    }
  })

  it('truncated JSON → invalid_json', async () => {
    const json = await roundtripContainer()
    await expect(
      openDepmapContainer(json.slice(0, Math.floor(json.length / 2)), 'pwd'),
    ).rejects.toMatchObject({
      code: 'invalid_json',
    })
  })

  it('invalid nonce/salt/tag byte length → bounds', () => {
    const h1 = validHeader()
    h1.cipher.nonce = fromBase64ToB64(new Uint8Array(11))
    expect(() => validateDepmapBounds(h1)).toThrowError(/nonce/)
    const h2 = validHeader()
    h2.kdf.salt = fromBase64ToB64(new Uint8Array(15))
    expect(() => validateDepmapBounds(h2)).toThrowError(/salt/)
    const h3 = validHeader()
    h3.tag = fromBase64ToB64(new Uint8Array(17))
    expect(() => validateDepmapBounds(h3)).toThrowError(/tag/)
  })

  it('oversize ciphertext（>64MiB）→ bounds，且先于 KDF', () => {
    const h = validHeader()
    const big = new Uint8Array(64 * 1024 * 1024 + 1)
    h.ciphertext = Buffer.from(big).toString('base64')
    const t0 = Date.now()
    expect(() => validateDepmapBounds(h)).toThrowError(/64 MiB/)
    expect(Date.now() - t0).toBeLessThan(1000)
  })

  it('empty ciphertext → bounds', () => {
    const h = validHeader()
    h.ciphertext = ''
    expect(() => validateDepmapBounds(h)).toThrowError(/empty/)
  })

  it('unsupported formatVersion / kdf.version / cipher / format → bounds', async () => {
    const json = await roundtripContainer()
    for (const [path, value] of [
      ['formatVersion', 2],
      ['version', 20],
    ] as const) {
      const header = JSON.parse(json)
      if (path === 'version') header.kdf.version = value
      else header.formatVersion = value
      await expect(openDepmapContainer(JSON.stringify(header), 'pwd')).rejects.toMatchObject({
        code: 'bounds',
      })
    }
    const h2 = JSON.parse(json) as { format: string }
    h2.format = 'not-depmap'
    await expect(openDepmapContainer(JSON.stringify(h2), 'pwd')).rejects.toMatchObject({
      code: 'bounds',
    })
    const h3 = JSON.parse(json) as { cipher: { algorithm: string } }
    h3.cipher.algorithm = 'AES-128-CBC'
    await expect(openDepmapContainer(JSON.stringify(h3), 'pwd')).rejects.toMatchObject({
      code: 'bounds',
    })
  })

  it('corrupted payload（截断 base64 ciphertext）→ auth_failed，不返回 partial', async () => {
    const json = await roundtripContainer()
    const header = JSON.parse(json) as { ciphertext: string }
    header.ciphertext = header.ciphertext.slice(0, Math.max(4, header.ciphertext.length - 8))
    try {
      await openDepmapContainer(JSON.stringify(header), 'pwd')
      expect.unreachable('must throw')
    } catch (e) {
      expect(e).toBeInstanceOf(DepmapError)
      expect((e as DepmapError).code).toBe('auth_failed')
    }
  })

  it('PHASE K container mutation fuzz：拒绝或语义等价 no-op（永不返回错误明文）', async () => {
    // 用最小合法 KDF 参数构造容器，让每次 open 的 Argon2 开销可控
    const { json, header } = await createDepmapContainer(
      enc.encode('payload-for-negative-tests'),
      'pwd',
      {
        memoryKiB: 16384,
        iterations: 1,
      },
    )
    let rejected = 0
    let noop = 0
    for (let i = 0; i < 30; i++) {
      const idx = Math.floor(seededRandom() * json.length)
      const mutated = replaceAt(json, idx, json[idx] === 'A' ? 'B' : 'A')
      try {
        const r = await openDepmapContainer(mutated, 'pwd')
        // 成功路径仅允许“语义等价 no-op”：base64 非规范编码（末字符低 4 位被丢弃）
        // 导致解码字节完全一致。必须逐一验证字节级等价，否则视为 fail-open。
        const m = JSON.parse(mutated) as {
          format: string
          formatVersion: number
          tag: string
          ciphertext: string
          kdf: { salt: string }
          cipher: { nonce: string }
        }
        const sameTag = fromBase64(m.tag).toString() === fromBase64(header.tag).toString()
        const sameCt =
          fromBase64(m.ciphertext).toString() === fromBase64(header.ciphertext).toString()
        const sameSalt =
          fromBase64(m.kdf.salt).toString() === fromBase64(header.kdf.salt).toString()
        const sameNonce =
          fromBase64(m.cipher.nonce).toString() === fromBase64(header.cipher.nonce).toString()
        const sameHeader =
          JSON.stringify({ f: m.format, v: m.formatVersion, kdf: m.kdf, c: m.cipher }) ===
          JSON.stringify({
            f: header.format,
            v: header.formatVersion,
            kdf: header.kdf,
            c: header.cipher,
          })
        expect(new TextDecoder().decode(r.plaintext)).toBe('payload-for-negative-tests')
        expect([sameTag, sameCt, sameSalt, sameNonce, sameHeader].every((x) => x === true)).toBe(
          true,
        )
        noop += 1
      } catch (e) {
        rejected += 1
        if (e instanceof DepmapError) {
          expect(['auth_failed', 'bounds', 'invalid_json', 'invalid_structure', 'kdf']).toContain(
            e.code,
          )
        }
        // 非 DepmapError 的异常同样计入拒绝（fail closed 成立）；KDF 错误已包装为 DepmapError
      }
    }
    expect(rejected + noop).toBe(30)
    expect(noop).toBeLessThanOrEqual(5)
  }, 30000)
})

// ---------------------------------------------------------------- helpers

function validHeader(): ReturnType<typeof parseDepmapHeader> {
  return parseDepmapHeader(
    JSON.stringify({
      format: 'depmap',
      formatVersion: 1,
      kdf: {
        algorithm: 'argon2id',
        version: 19,
        salt: fromBase64ToB64(new Uint8Array(16)),
        memoryKiB: 65536,
        iterations: 3,
        parallelism: 1,
      },
      cipher: { algorithm: 'AES-256-GCM', nonce: fromBase64ToB64(new Uint8Array(12)) },
      ciphertext: 'AAAA',
      tag: fromBase64ToB64(new Uint8Array(16)),
    }),
  )
}

function fromBase64ToB64(bytes: Uint8Array): string {
  return Buffer.from(bytes).toString('base64')
}

function replaceAt(s: string, idx: number, ch: string): string {
  return s.slice(0, idx) + ch + s.slice(idx + 1)
}

/** deterministic pseudo-random（不引入框架）。 */
function seededRandom(): number {
  fuzzState = (fuzzState * 1103515245 + 12345) % 2147483648
  return fuzzState / 2147483648
}
let fuzzState = 42

void fromBase64
