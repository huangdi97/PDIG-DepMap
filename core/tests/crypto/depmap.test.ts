import { describe, expect, it } from 'vitest'
import { jcsStringify } from '../../src/crypto/jcs.ts'
import {
  createDepmapContainer,
  openDepmapContainer,
  parseDepmapHeader,
  validateDepmapBounds,
  computeAad,
  DepmapError,
  DEPMAP_V1_DEFAULTS,
  fromBase64,
} from '../../src/crypto/depmap.ts'
import {
  createGoldenContainer,
  GOLDEN_EXPECTED,
  GOLDEN_PASSWORD,
  GOLDEN_PLAINTEXT,
  GOLDEN_SALT_HEX,
  GOLDEN_NONCE_HEX,
} from '../../src/crypto/golden.ts'

// --------------------------------------------------------------------- JCS

describe('JCS (RFC 8785 restricted domain)', () => {
  it('sorts keys by UTF-16 code unit order', () => {
    expect(jcsStringify({ b: 1, a: 2, A: 3, Z: 4, z: 5 })).toBe('{"A":3,"Z":4,"a":2,"b":1,"z":5}')
  })

  it('escapes strings per RFC 8785 (shortest form, lowercase hex)', () => {
    expect(jcsStringify('a"b\\c\nd\te\u0001f')).toBe('"a\\"b\\\\c\\nd\\te\\u0001f"')
    expect(jcsStringify('中文')).toBe('"中文"')
  })

  it('rejects floats and bigint (restricted domain)', () => {
    expect(() => jcsStringify({ x: 0.5 })).toThrowError(/restricted domain/)
    expect(() => jcsStringify({ x: 1n })).toThrowError(/restricted domain/)
  })

  it('nested arrays and objects are canonical', () => {
    const obj = { cipher: { algorithm: 'AES-256-GCM', z: 1 }, a: [3, 1, 2] }
    expect(jcsStringify(obj)).toBe('{"a":[3,1,2],"cipher":{"algorithm":"AES-256-GCM","z":1}}')
  })
})

// ------------------------------------------------------------------ bounds

describe('DEPMAP V1 bounds validation (before Argon2)', () => {
  function headerWith(over: Record<string, unknown>): string {
    const base = JSON.parse(
      JSON.stringify({
        format: 'depmap',
        formatVersion: 1,
        kdf: {
          algorithm: 'argon2id',
          version: 19,
          salt: 'ABEiM0RVZneImaq7zN3u/w==',
          memoryKiB: 65536,
          iterations: 3,
          parallelism: 1,
        },
        cipher: { algorithm: 'AES-256-GCM', nonce: 'obLD1OX2BxgpOktc' },
        ciphertext: 'AAAA',
        tag: '5qpABhovPbNet1q2GNEhkg==',
      }),
    )
    for (const [k, v] of Object.entries(over)) {
      if (k.includes('.')) {
        const [parent, child] = k.split('.')
        ;(base[parent!] as Record<string, unknown>)[child!] = v
      } else {
        ;(base as Record<string, unknown>)[k] = v
      }
    }
    return JSON.stringify(base)
  }

  it('valid header passes bounds', () => {
    expect(() => validateDepmapBounds(parseDepmapHeader(headerWith({})))).not.toThrow()
  })

  it.each([
    ['kdf.memoryKiB', 16383],
    ['kdf.memoryKiB', 262145],
    ['kdf.memoryKiB', 1000000000],
    ['kdf.iterations', 0],
    ['kdf.iterations', 11],
    ['kdf.parallelism', 0],
    ['kdf.parallelism', 5],
    ['formatVersion', 2],
    ['kdf.version', 16],
  ])('rejects %s = %s', (field, value) => {
    try {
      validateDepmapBounds(parseDepmapHeader(headerWith({ [field]: value })))
      expect.unreachable(`${field}=${value} should fail`)
    } catch (e) {
      expect(e).toBeInstanceOf(DepmapError)
      expect((e as DepmapError).code).toBe('bounds')
    }
  })

  it('rejects wrong salt/nonce/tag byte lengths', () => {
    for (const [field, v] of [
      ['kdf.salt', 'AAAA'],
      ['cipher.nonce', 'AAAA'],
      ['tag', 'AAAA'],
    ] as const) {
      expect(() =>
        validateDepmapBounds(parseDepmapHeader(headerWith({ [field]: v }))),
      ).toThrowError(DepmapError)
    }
  })

  it('malicious container with huge memoryKiB is rejected BEFORE KDF (fast)', () => {
    const t0 = Date.now()
    expect(() =>
      validateDepmapBounds(parseDepmapHeader(headerWith({ 'kdf.memoryKiB': 1_000_000_000 }))),
    ).toThrowError(/memoryKiB/)
    // 边界检查是纯同步校验，不应花费明显时间（KDF 至少数百毫秒）
    expect(Date.now() - t0).toBeLessThan(200)
  })
})

// ------------------------------------------------------- create / open

describe('DEPMAP container create/open', () => {
  it('roundtrip: open(create(pt, pwd)) returns plaintext', async () => {
    const pt = new TextEncoder().encode('{"hello":"依赖图"}')
    const { json } = await createDepmapContainer(pt, 'correct horse')
    const { plaintext } = await openDepmapContainer(json, 'correct horse')
    expect(new TextDecoder().decode(plaintext)).toBe('{"hello":"依赖图"}')
  })

  it('wrong password fails with auth_failed', async () => {
    const pt = new TextEncoder().encode('secret')
    const { json } = await createDepmapContainer(pt, 'right-password')
    await expect(openDepmapContainer(json, 'wrong-password')).rejects.toMatchObject({
      code: 'auth_failed',
    })
  })

  it('tag tamper fails authentication', async () => {
    const pt = new TextEncoder().encode('secret')
    const { json } = await createDepmapContainer(pt, 'pwd')
    const header = JSON.parse(json)
    const tagBytes = fromBase64(header.tag)
    tagBytes[0] = (tagBytes[0]! + 1) % 256
    header.tag = Buffer.from(tagBytes).toString('base64')
    await expect(openDepmapContainer(JSON.stringify(header), 'pwd')).rejects.toMatchObject({
      code: 'auth_failed',
    })
  })

  it('ciphertext tamper fails authentication (AAD + GCM)', async () => {
    const pt = new TextEncoder().encode('secret payload for tamper test')
    const { json } = await createDepmapContainer(pt, 'pwd')
    const header = JSON.parse(json)
    const ct = fromBase64(header.ciphertext)
    ct[0] = (ct[0]! + 1) % 256
    header.ciphertext = Buffer.from(ct).toString('base64')
    await expect(openDepmapContainer(JSON.stringify(header), 'pwd')).rejects.toMatchObject({
      code: 'auth_failed',
    })
  })

  it('header tamper (in-bounds kdf change) fails via AAD mismatch', async () => {
    const pt = new TextEncoder().encode('secret')
    const { json } = await createDepmapContainer(pt, 'pwd')
    const header = JSON.parse(json)
    header.kdf.iterations = 4 // 仍在合法边界内，但 AAD 改变
    await expect(openDepmapContainer(JSON.stringify(header), 'pwd')).rejects.toMatchObject({
      code: 'auth_failed',
    })
  })

  it('AAD excludes ciphertext/tag and is stable JCS', async () => {
    const { header } = await createGoldenContainer()
    const aad1 = computeAad(header)
    const aad2 = computeAad(JSON.parse(JSON.stringify(header)))
    expect(Buffer.from(aad1).toString('hex')).toBe(Buffer.from(aad2).toString('hex'))
    expect(new TextDecoder().decode(aad1)).not.toContain('ciphertext')
    expect(new TextDecoder().decode(aad1)).not.toContain('tag')
  })

  it('uses exact UTF-8 bytes of password (no unicode normalization)', async () => {
    // 'é' 的 NFC/NFD 字节不同；两个口令必须产生不同密文
    const pt = new TextEncoder().encode('x')
    const a = await createDepmapContainer(pt, 'café', {
      salt: fromBase64('ABEiM0RVZneImaq7zN3u/w=='),
      nonce: fromBase64('obLD1OX2BxgpOktc'),
    })
    const b = await createDepmapContainer(pt, 'cafe\u0301', {
      salt: fromBase64('ABEiM0RVZneImaq7zN3u/w=='),
      nonce: fromBase64('obLD1OX2BxgpOktc'),
    })
    expect(a.header.ciphertext).not.toBe(b.header.ciphertext)
  })

  it('default params are 65536/3/1 with 16-byte salt and 12-byte nonce', async () => {
    const { header } = await createDepmapContainer(new TextEncoder().encode('x'), 'p')
    expect(header.kdf.memoryKiB).toBe(DEPMAP_V1_DEFAULTS.memoryKiB)
    expect(header.kdf.iterations).toBe(DEPMAP_V1_DEFAULTS.iterations)
    expect(header.kdf.parallelism).toBe(DEPMAP_V1_DEFAULTS.parallelism)
    expect(fromBase64(header.kdf.salt)).toHaveLength(16)
    expect(fromBase64(header.cipher.nonce)).toHaveLength(12)
  })
})

// ---------------------------------------------------------- golden vector

describe('Golden Test Vector (frozen)', () => {
  it('reproduces frozen derivedKey / ciphertext / tag', async () => {
    const r = await createGoldenContainer()
    expect(r.derivedKeyHex).toBe(GOLDEN_EXPECTED.derivedKeyHex)
    expect(r.header.ciphertext).toBe(GOLDEN_EXPECTED.ciphertextBase64)
    expect(r.header.tag).toBe(GOLDEN_EXPECTED.tagBase64)
  })

  it('golden container opens with golden password and plaintext matches', async () => {
    const { json } = await createGoldenContainer()
    const { plaintext } = await openDepmapContainer(json, GOLDEN_PASSWORD)
    expect(new TextDecoder().decode(plaintext)).toBe(GOLDEN_PLAINTEXT)
  })

  it('golden password wrong byte fails', async () => {
    const { json } = await createGoldenContainer()
    await expect(openDepmapContainer(json, 'depmap-tesT')).rejects.toMatchObject({
      code: 'auth_failed',
    })
  })

  it('salt/nonce fixture lengths', () => {
    expect(GOLDEN_SALT_HEX).toHaveLength(32)
    expect(GOLDEN_NONCE_HEX).toHaveLength(24)
  })
})
