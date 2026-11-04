/**
 * 浏览器 node:crypto 垫片必须与 Node 输出逐字节一致（否则 fingerprint 会漂移，
 * 导致同一账单在不同平台被去重成不同集合）。
 * 这里用公开测试向量（RFC 6234 / RFC 4231），不依赖 Node 模块。
 */
import { describe, expect, it } from 'vitest'
import { createHash, createHmac, randomBytes, createCipheriv } from '../../src/shims/node-crypto.ts'

describe('sha256 shim', () => {
  it('hashes the empty input', () => {
    expect(createHash('sha256').digest('hex')).toBe(
      'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',
    )
  })

  it('hashes "abc"', () => {
    expect(createHash('sha256').update('abc').digest('hex')).toBe(
      'ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad',
    )
  })

  it('supports multi-part updates and UTF-8', () => {
    const oneShot = createHash('sha256').update('中文 observation').digest('hex')
    const multi = createHash('sha256').update('中文 ').update('observation').digest('hex')
    expect(multi).toBe(oneShot)
  })
})

describe('hmac-sha256 shim', () => {
  it('matches RFC 4231 test case 2', () => {
    const key = new TextEncoder().encode('Jefe')
    const digest = createHmac('sha256', key).update('what do ya want for nothing?').digest('hex')
    expect(digest).toBe('5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843')
  })
})

describe('unsupported primitives', () => {
  it('never silently degrades createCipheriv', () => {
    expect(() => createCipheriv()).toThrow(/WebCrypto AES-GCM/)
  })

  it('produces 16 random bytes', () => {
    const a = randomBytes(16)
    const b = randomBytes(16)
    expect(a.length).toBe(16)
    expect(Array.from(a)).not.toEqual(Array.from(b))
  })
})
