// DEPMAP_CONTAINER_V1 —— 规范化层与 GCM 参数的无依赖黄金校验（仅用 node:crypto）
//
// 作用：把 harmony/entry/src/main/ets/crypto/ 里移植的「JCS 键序 + AAD 构造 +
// AES-256-GCM 参数」规格，用一份**独立实现**在主机上复现冻结黄金向量。
//
// 为什么需要它：
// - Harmony 侧目前无设备 / 无系统镜像，ArkTS 运行时执行 = NOT_RUN；
// - 编译通过只证明语法与类型，不能证明字节正确；
// - 因此用这份脚本锁住"移植规格本身是对的"，把设备验证要回答的问题收敛为
//   「ArkTS 运行时是否忠实执行了这份规格」。
//
// 冻结向量来源（跨端共用，不可单方面修改）：
//   core/src/crypto/golden.ts
//   platforms/android/test/com/depmap/core/crypto/DepmapContainerV1GoldenTest.kt
//
// 用法：node tools/harmony/verify-container-golden.mjs
import { createCipheriv, createHash } from 'node:crypto'

// ---- 冻结输入（与 golden.ts 一致）----
const SALT_HEX = '00112233445566778899aabbccddeeff'
const NONCE_HEX = 'a1b2c3d4e5f60718293a4b5c'
const PLAINTEXT = '{"app":"depmap","schemaVersion":1,"nodes":[],"dependencies":[]}'
const MEMORY_KIB = 65536
const ITERATIONS = 3
const PARALLELISM = 1

// ---- 冻结输出 ----
const EXPECT_KEY_HEX =
  '66c4bec7f5e98856747d7b41d0a021bdc092d5e12d492852bd80647bd0ff0c86'
const EXPECT_CIPHERTEXT_B64 =
  'KNSpbKK6waj4En3ADgeBB74H96Q7GCoBmNffeOG5QSQ9XJwx4LoJCQ0j8lEinA7GN85U6JwaVMqhAkqWDdG7'
const EXPECT_TAG_B64 = '5qpABhovPbNet1q2GNEhkg=='

/**
 * 冻结 AAD（JCS 字符串）。
 * 与下列两处必须逐字符一致：
 *   - platforms/android/.../DepmapContainerV1.kt  buildAad()
 *   - harmony/entry/src/main/ets/crypto/ContainerSelfCheck.ets  GOLDEN_AAD_EXPECTED
 */
const EXPECT_AAD =
  '{"cipher":{"algorithm":"AES-256-GCM","nonce":"obLD1OX2BxgpOktc"},' +
  '"format":"depmap","formatVersion":1,"kdf":{"algorithm":"argon2id",' +
  '"iterations":3,"memoryKiB":65536,"parallelism":1,' +
  '"salt":"ABEiM0RVZneImaq7zN3u/w==","version":19}}'

// ---- JCS（RFC 8785 受限域）独立实现 ----
class JcsError extends Error {}

function jcsEscapeString(s) {
  let out = '"'
  for (const ch of s) {
    const code = ch.codePointAt(0)
    if (code === undefined) continue
    if (ch === '"') out += '\\"'
    else if (ch === '\\') out += '\\\\'
    else if (code === 0x08) out += '\\b'
    else if (code === 0x09) out += '\\t'
    else if (code === 0x0a) out += '\\n'
    else if (code === 0x0c) out += '\\f'
    else if (code === 0x0d) out += '\\r'
    else if (code < 0x20) out += '\\u00' + code.toString(16).padStart(2, '0')
    else out += ch
  }
  return out + '"'
}

function jcsStringify(value) {
  if (value === null) return 'null'
  if (value === true) return 'true'
  if (value === false) return 'false'
  const t = typeof value
  if (t === 'string') return jcsEscapeString(value)
  if (t === 'number') {
    if (!Number.isSafeInteger(value)) throw new JcsError(`not a safe integer: ${value}`)
    return String(value)
  }
  if (Array.isArray(value)) return '[' + value.map(jcsStringify).join(',') + ']'
  if (t === 'object') {
    // ArkTS 侧用 JcsObject（排序后输出）；主机侧等价写法
    let entries
    if (value instanceof Map) entries = [...value.entries()]
    else entries = Object.keys(value).map((k) => [k, value[k]])
    entries.sort((a, b) => (a[0] < b[0] ? -1 : a[0] > b[0] ? 1 : 0))
    return '{' + entries.map(([k, v]) => `${jcsEscapeString(k)}:${jcsStringify(v)}`).join(',') + '}'
  }
  throw new JcsError(`unsupported type ${t}`)
}

// ---- 构造 AAD ----
const saltB64 = Buffer.from(SALT_HEX, 'hex').toString('base64')
const nonceB64 = Buffer.from(NONCE_HEX, 'hex').toString('base64')

const aadSource = new Map()
aadSource.set('format', 'depmap')
aadSource.set('formatVersion', 1)
aadSource.set(
  'kdf',
  new Map([
    ['algorithm', 'argon2id'],
    ['version', 19],
    ['salt', saltB64],
    ['memoryKiB', MEMORY_KIB],
    ['iterations', ITERATIONS],
    ['parallelism', PARALLELISM],
  ]),
)
aadSource.set(
  'cipher',
  new Map([
    ['algorithm', 'AES-256-GCM'],
    ['nonce', nonceB64],
  ]),
)

const aadString = jcsStringify(aadSource)
const aad = Buffer.from(aadString, 'utf8')

// ---- GCM（ciphertext 与 tag 分离）----
const key = Buffer.from(EXPECT_KEY_HEX, 'hex')
const nonce = Buffer.from(NONCE_HEX, 'hex')
const cipher = createCipheriv('aes-256-gcm', key, nonce, { authTagLength: 16 })
cipher.setAAD(aad)
const ciphertext = Buffer.concat([cipher.update(Buffer.from(PLAINTEXT, 'utf8')), cipher.final()])
const tag = cipher.getAuthTag()

// ---- 断言 ----
const checks = [
  ['aad.jcsString', aadString === EXPECT_AAD],
  ['aad.utf8Length', aad.length === 228],
  ['aad.sha256', createHash('sha256').update(aad).digest('hex') ===
    'd3312192f01787c4325e23c1d3a45f5d2ee697d37ab0ac64f1c742b756c1a552'],
  ['gcm.ciphertextBase64', ciphertext.toString('base64') === EXPECT_CIPHERTEXT_B64],
  ['gcm.tagBase64', tag.toString('base64') === EXPECT_TAG_B64],
]

console.log('AAD_SHA256=' + createHash('sha256').update(aad).digest('hex'))
console.log('AAD_LEN=' + aad.length)
for (const [name, ok] of checks) {
  console.log(`${ok ? 'PASS' : 'FAIL'}  ${name}`)
}
const allOk = checks.every(([, ok]) => ok)
console.log('VERDICT=' + (allOk ? 'PASS' : 'FAIL'))
process.exit(allOk ? 0 : 1)
