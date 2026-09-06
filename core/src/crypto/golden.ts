import { createDepmapContainer, type DepmapContainerResult } from './depmap.ts'
import { toBase64 } from './depmap.ts'

/**
 * DEPMAP_CONTAINER_V1 Golden Test Vector (GOAL_MVP01 §8.2)
 *
 * 固定输入：
 * - password  = depmap-test（exact UTF-8 bytes，无 Unicode 归一化）
 * - salt      = 00112233445566778899aabbccddeeff（16 bytes）
 * - nonce     = a1b2c3d4e5f60718293a4b5c（12 bytes）
 * - plaintext = {"app":"depmap","schemaVersion":1,"nodes":[],"dependencies":[]}（UTF-8）
 * - kdf       = argon2id v19, 65536 KiB / 3 / 1
 *
 * 冻结输出：不同实现（Node reference / Android Kotlin / Web / iOS）必须产出
 * 完全一致的 derivedKey / ciphertext / tag，并用本容器互相解密（互操作）。
 */

export const GOLDEN_PASSWORD = 'depmap-test'
export const GOLDEN_SALT_HEX = '00112233445566778899aabbccddeeff'
export const GOLDEN_NONCE_HEX = 'a1b2c3d4e5f60718293a4b5c'
export const GOLDEN_PLAINTEXT = '{"app":"depmap","schemaVersion":1,"nodes":[],"dependencies":[]}'

export const GOLDEN_EXPECTED = {
  derivedKeyHex: '66c4bec7f5e98856747d7b41d0a021bdc092d5e12d492852bd80647bd0ff0c86',
  ciphertextBase64: 'KNSpbKK6waj4En3ADgeBB74H96Q7GCoBmNffeOG5QSQ9XJwx4LoJCQ0j8lEinA7GN85U6JwaVMqhAkqWDdG7',
  tagBase64: '5qpABhovPbNet1q2GNEhkg=='
} as const

export function goldenSalt(): Uint8Array {
  return Uint8Array.from(Buffer.from(GOLDEN_SALT_HEX, 'hex'))
}

export function goldenNonce(): Uint8Array {
  return Uint8Array.from(Buffer.from(GOLDEN_NONCE_HEX, 'hex'))
}

export async function createGoldenContainer(): Promise<DepmapContainerResult> {
  return createDepmapContainer(new TextEncoder().encode(GOLDEN_PLAINTEXT), GOLDEN_PASSWORD, {
    salt: goldenSalt(),
    nonce: goldenNonce()
  })
}

/** 供文档/跨端测试导出的固定容器 JSON（canonical JCS 序列化）。 */
export const GOLDEN_CONTAINER_JSON =
  '{"cipher":{"algorithm":"AES-256-GCM","nonce":"' +
  toBase64(goldenNonce()) +
  '","ciphertext":"' +
  GOLDEN_EXPECTED.ciphertextBase64 +
  '","format":"depmap","formatVersion":1,"kdf":{"algorithm":"argon2id","iterations":3,"memoryKiB":65536,"parallelism":1,"salt":"' +
  toBase64(goldenSalt()) +
  '","version":19},"tag":"' +
  GOLDEN_EXPECTED.tagBase64 +
  '"}'

void GOLDEN_CONTAINER_JSON
