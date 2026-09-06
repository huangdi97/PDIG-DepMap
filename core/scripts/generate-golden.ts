/**
 * Golden vector generator — 一次性工具（node --experimental-strip-types scripts/generate-golden.ts）
 * 输出冻结值用于 tests/golden 与跨端互操作（Android Kotlin / Web 实现据此对齐）。
 */
import { createDepmapContainer } from '../src/crypto/depmap.ts'

const salt = Uint8Array.from(Buffer.from('00112233445566778899aabbccddeeff', 'hex'))
const nonce = Uint8Array.from(Buffer.from('a1b2c3d4e5f60718293a4b5c', 'hex'))
const plaintext = new TextEncoder().encode('{"app":"depmap","schemaVersion":1,"nodes":[],"dependencies":[]}')

const r = await createDepmapContainer(plaintext, 'depmap-test', { salt, nonce })
console.log(
  JSON.stringify(
    {
      derivedKeyHex: r.derivedKeyHex,
      ciphertextBase64: r.header.ciphertext,
      tagBase64: r.header.tag,
      containerJson: r.json
    },
    null,
    2
  )
)
