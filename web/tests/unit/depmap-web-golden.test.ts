/**
 * DEPMAP_CONTAINER_V1 浏览器实现 vs 冻结 Golden Vector。
 *
 * 这是 Web 端 backup ready 的前提：
 * derivedKey / ciphertext / tag 必须与 Node reference、Kotlin、Swift、ArkTS 完全一致。
 */
import { describe, expect, it } from 'vitest'
import {
  ARGON2_VERSION_19,
  DEPMAP_FORMAT,
  DEPMAP_FORMAT_VERSION,
  DEPMAP_V1_BOUNDS,
  DEPMAP_V1_DEFAULTS,
  DepmapWebError,
  createDepmapContainer,
  fromBase64,
  fromHex,
  openDepmapContainer,
  parseDepmapHeader,
  toBase64,
  validateDepmapBounds,
} from '../../src/crypto/depmap-web.ts'
import {
  DEPMAP_FORMAT as CORE_FORMAT,
  DEPMAP_FORMAT_VERSION as CORE_FORMAT_VERSION,
  ARGON2_VERSION_19 as CORE_ARGON2_VERSION,
  DEPMAP_V1_BOUNDS as CORE_BOUNDS,
  DEPMAP_V1_DEFAULTS as CORE_DEFAULTS,
} from '@core/crypto/depmap.ts'
import {
  GOLDEN_EXPECTED,
  GOLDEN_NONCE_HEX,
  GOLDEN_PASSWORD,
  GOLDEN_PLAINTEXT,
  GOLDEN_SALT_HEX,
} from '@core/crypto/golden.ts'

describe('DEPMAP_CONTAINER_V1 constants mirror Core exactly', () => {
  it('keeps the frozen protocol constants identical', () => {
    expect(DEPMAP_FORMAT).toBe(CORE_FORMAT)
    expect(DEPMAP_FORMAT_VERSION).toBe(CORE_FORMAT_VERSION)
    expect(ARGON2_VERSION_19).toBe(CORE_ARGON2_VERSION)
    expect(DEPMAP_V1_DEFAULTS).toEqual(CORE_DEFAULTS)
    expect(DEPMAP_V1_BOUNDS).toEqual(CORE_BOUNDS)
  })
})

describe('golden vector', () => {
  it('reproduces derivedKey / ciphertext / tag byte-for-byte', async () => {
    const result = await createDepmapContainer(
      new TextEncoder().encode(GOLDEN_PLAINTEXT),
      GOLDEN_PASSWORD,
      {
        salt: fromHex(GOLDEN_SALT_HEX),
        nonce: fromHex(GOLDEN_NONCE_HEX),
      },
    )
    expect(result.derivedKeyHex).toBe(GOLDEN_EXPECTED.derivedKeyHex)
    expect(result.header.ciphertext).toBe(GOLDEN_EXPECTED.ciphertextBase64)
    expect(result.header.tag).toBe(GOLDEN_EXPECTED.tagBase64)
  })

  it('opens a container created by the browser implementation', async () => {
    const created = await createDepmapContainer(
      new TextEncoder().encode(GOLDEN_PLAINTEXT),
      GOLDEN_PASSWORD,
      {
        salt: fromHex(GOLDEN_SALT_HEX),
        nonce: fromHex(GOLDEN_NONCE_HEX),
      },
    )
    const opened = await openDepmapContainer(created.json, GOLDEN_PASSWORD)
    expect(new TextDecoder().decode(opened.plaintext)).toBe(GOLDEN_PLAINTEXT)
    expect(opened.key.length).toBe(32)
  })

  it('fails closed on wrong password', async () => {
    const created = await createDepmapContainer(
      new TextEncoder().encode(GOLDEN_PLAINTEXT),
      GOLDEN_PASSWORD,
      {
        salt: fromHex(GOLDEN_SALT_HEX),
        nonce: fromHex(GOLDEN_NONCE_HEX),
      },
    )
    await expect(openDepmapContainer(created.json, 'wrong-password')).rejects.toMatchObject({
      code: 'auth_failed',
    })
  })

  it('fails closed on tampered ciphertext (AAD/GCM auth)', async () => {
    const created = await createDepmapContainer(
      new TextEncoder().encode(GOLDEN_PLAINTEXT),
      GOLDEN_PASSWORD,
      {
        salt: fromHex(GOLDEN_SALT_HEX),
        nonce: fromHex(GOLDEN_NONCE_HEX),
      },
    )
    const parsed = JSON.parse(created.json) as Record<string, unknown>
    const cipher = Buffer.from(String(parsed['ciphertext']), 'base64')
    cipher[0] = (cipher[0] ?? 0) ^ 0xff
    parsed['ciphertext'] = cipher.toString('base64')
    await expect(
      openDepmapContainer(JSON.stringify(parsed), GOLDEN_PASSWORD),
    ).rejects.toMatchObject({
      code: 'auth_failed',
    })
  })

  it('rejects KDF bounds before running Argon2', () => {
    const header = parseDepmapHeader(
      JSON.stringify({
        format: 'depmap',
        formatVersion: 1,
        kdf: {
          algorithm: 'argon2id',
          version: 19,
          salt: 'AAAAAAAAAAAAAAAAAAAAAA==',
          memoryKiB: DEPMAP_V1_BOUNDS.memoryKiBMax * 4,
          iterations: 3,
          parallelism: 1,
        },
        cipher: { algorithm: 'AES-256-GCM', nonce: 'AAAAAAAAAAAAAAAA' },
        ciphertext: 'AAAA',
        tag: 'AAAAAAAAAAAAAAAAAAAAAA==',
      }),
    )
    expect(() => validateDepmapBounds(header)).toThrow(DepmapWebError)
  })

  it('rejects future formatVersion (fail closed)', async () => {
    const created = await createDepmapContainer(
      new TextEncoder().encode(GOLDEN_PLAINTEXT),
      GOLDEN_PASSWORD,
      {
        salt: fromHex(GOLDEN_SALT_HEX),
        nonce: fromHex(GOLDEN_NONCE_HEX),
      },
    )
    const parsed = JSON.parse(created.json) as Record<string, unknown>
    parsed['formatVersion'] = 2
    await expect(
      openDepmapContainer(JSON.stringify(parsed), GOLDEN_PASSWORD),
    ).rejects.toMatchObject({
      code: 'unsupported',
    })
  })
})

describe('base64 is RFC4648 with padding', () => {
  it('round-trips all byte lengths', () => {
    for (const length of [0, 1, 2, 3, 4, 15, 16, 17, 64, 255]) {
      const bytes = new Uint8Array(length).map((_, index) => (index * 37) % 256)
      expect(Array.from(fromBase64(toBase64(bytes)))).toEqual(Array.from(bytes))
    }
  })

  it('matches Node Buffer for padding cases', () => {
    for (const length of [1, 2, 3, 4, 5]) {
      const bytes = new Uint8Array(length).fill(0x41)
      expect(toBase64(bytes)).toBe(Buffer.from(bytes).toString('base64'))
    }
  })
})
