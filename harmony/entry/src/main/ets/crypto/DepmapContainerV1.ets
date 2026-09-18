import { cryptoFramework } from '@kit.CryptoArchitectureKit';
import { util } from '@kit.ArkTS';

import { JcsObject, jcsStringify } from './Jcs';

/**
 * DEPMAP_CONTAINER_V1 —— Harmony (ArkTS) 口令派生文件加密实现。
 *
 * 契约（CANONICAL_DESIGN §8.5-§8.9，与 Core Node / Android Kotlin 完全一致）：
 *   password --Argon2id(v19)--> fileEncryptionKey(32B) --AES-256-GCM--> ciphertext + tag
 *   AAD = UTF8(JCS({format, formatVersion, kdf, cipher}))，ciphertext / tag 不进 AAD。
 *   解密前 header 一律不可信：structure -> bounds -> KDF -> GCM auth。
 *
 * 与 Android 实现的关键差异（平台能力差异，非协议差异）：
 * - Argon2id：Harmony cryptoFramework / HUKS 均不提供 Argon2（见
 *   HARMONY_ARGON2_FEASIBILITY.md，cryptoFramework.d.ts 与 huks.d.ts 中 argon2 命中数 = 0）。
 *   因此 KDF 以 Argon2idDeriver 注入；当前由 NAPI 原生实现提供（Argon2idNative）。
 * - AES-256-GCM：使用 cryptoFramework 的同步接口，transformation 为 'AES256|GCM|PKCS7'，
 *   参数 GcmParamsSpec{iv, aad, authTag}。加密时 tag 由 doFinalSync(null) 返回，
 *   解密时 tag 通过 initSync 的 params 传入 —— 与容器把 ciphertext / tag 分列两个字段
 *   的结构天然对应，无需像 Node 侧那样手工拼接 ct||tag。
 * - Base64：util.Base64Helper（默认 Type.BASIC，RFC 4648 标准带填充），
 *   与 Core Node Buffer.toString('base64') 一致。
 *
 * 状态：IMPLEMENTED（AES / JCS / 容器逻辑）。
 *   KDF 绑定：PENDING_NATIVE_INTEGRATION（.so 尚未打进 HAP）。
 *   运行时执行：NOT_RUN —— 本机无 Harmony 设备 / 系统镜像，不得记为 PASS。
 */

// ---------------------------------------------------------------------------
// 常量
// ---------------------------------------------------------------------------

export const DEPMAP_FORMAT: string = 'depmap';
export const DEPMAP_FORMAT_VERSION: number = 1;
export const ARGON2_VERSION_19: number = 19;
export const CIPHER_ALGORITHM: string = 'AES-256-GCM';

/** cryptoFramework transformation。 */
export const AES_GCM_TRANSFORMATION: string = 'AES256|GCM|PKCS7';
/** cryptoFramework 对称密钥算法名（32 字节 = AES-256）。 */
export const SYM_KEY_ALG_NAME: string = 'AES256';
export const GCM_PARAMS_ALG_NAME: string = 'GcmParamsSpec';

export const SALT_LEN: number = 16;
export const NONCE_LEN: number = 12;
export const TAG_LEN: number = 16;
export const KEY_LEN: number = 32;

export const MEMORY_KIB_DEFAULT: number = 65536;
export const ITERATIONS_DEFAULT: number = 3;
export const PARALLELISM_DEFAULT: number = 1;

export const MEMORY_KIB_MIN: number = 16384;
export const MEMORY_KIB_MAX: number = 262144;
export const ITERATIONS_MIN: number = 1;
export const ITERATIONS_MAX: number = 10;
export const PARALLELISM_MIN: number = 1;
export const PARALLELISM_MAX: number = 4;
export const CIPHERTEXT_MAX_BYTES: number = 64 * 1024 * 1024;

// ---------------------------------------------------------------------------
// 错误
// ---------------------------------------------------------------------------

export type DepmapErrorCode =
  | 'invalid_json'
  | 'invalid_structure'
  | 'bounds'
  | 'kdf'
  | 'auth_failed';

export class DepmapError extends Error {
  readonly code: DepmapErrorCode;

  constructor(code: DepmapErrorCode, message: string) {
    super(message);
    this.name = 'DepmapError';
    this.code = code;
  }
}

// ---------------------------------------------------------------------------
// 结构
// ---------------------------------------------------------------------------

export interface DepmapKdfHeader {
  algorithm: string;
  version: number;
  salt: string;
  memoryKiB: number;
  iterations: number;
  parallelism: number;
}

export interface DepmapCipherHeader {
  algorithm: string;
  nonce: string;
}

export interface DepmapHeader {
  format: string;
  formatVersion: number;
  kdf: DepmapKdfHeader;
  cipher: DepmapCipherHeader;
  ciphertext: string;
  tag: string;
}

/**
 * Argon2id 派生器。passwordUtf8 必须是**精确 UTF-8 字节**（不做 Unicode 归一化），
 * 否则三端 Golden Vector 无法互操作。
 */
export type Argon2idDeriver = (
  passwordUtf8: Uint8Array,
  salt: Uint8Array,
  memoryKiB: number,
  iterations: number,
  parallelism: number,
) => Uint8Array;

// ---------------------------------------------------------------------------
// 基础编码
// ---------------------------------------------------------------------------

const BASE64: util.Base64Helper = new util.Base64Helper();

export function toBase64(bytes: Uint8Array): string {
  return BASE64.encodeToStringSync(bytes);
}

export function fromBase64(s: string): Uint8Array {
  return BASE64.decodeSync(s);
}

export function utf8Bytes(s: string): Uint8Array {
  return new util.TextEncoder().encodeInto(s);
}

export function utf8String(bytes: Uint8Array): string {
  return new util.TextDecoder().decodeToString(bytes);
}

export function randomBytes(n: number): Uint8Array {
  return cryptoFramework.createRandom().generateRandomSync(n).data;
}

// ---------------------------------------------------------------------------
// AAD / 容器 JSON
// ---------------------------------------------------------------------------

function kdfJcs(kdf: DepmapKdfHeader): JcsObject {
  const o: JcsObject = new JcsObject();
  o.set('algorithm', kdf.algorithm);
  o.set('version', kdf.version);
  o.set('salt', kdf.salt);
  o.set('memoryKiB', kdf.memoryKiB);
  o.set('iterations', kdf.iterations);
  o.set('parallelism', kdf.parallelism);
  return o;
}

function cipherJcs(cipher: DepmapCipherHeader): JcsObject {
  const o: JcsObject = new JcsObject();
  o.set('algorithm', cipher.algorithm);
  o.set('nonce', cipher.nonce);
  return o;
}

/** AAD 的 JCS 字符串形式（便于自检逐字符比对）。 */
export function computeAadString(header: DepmapHeader): string {
  const root: JcsObject = new JcsObject();
  root.set('format', header.format);
  root.set('formatVersion', header.formatVersion);
  root.set('kdf', kdfJcs(header.kdf));
  root.set('cipher', cipherJcs(header.cipher));
  return jcsStringify(root);
}

/** AAD = UTF8(JCS({format, formatVersion, kdf, cipher}))。 */
export function computeAad(header: DepmapHeader): Uint8Array {
  return utf8Bytes(computeAadString(header));
}

/** 容器文件本身也用 JCS 序列化 → 确定性输出。 */
export function serializeContainer(header: DepmapHeader): string {
  const root: JcsObject = new JcsObject();
  root.set('cipher', cipherJcs(header.cipher));
  root.set('ciphertext', header.ciphertext);
  root.set('format', header.format);
  root.set('formatVersion', header.formatVersion);
  root.set('kdf', kdfJcs(header.kdf));
  root.set('tag', header.tag);
  return jcsStringify(root);
}

// ---------------------------------------------------------------------------
// 结构 / 边界校验（全部在 KDF 之前完成）
// ---------------------------------------------------------------------------

function requireString(v: Object, field: string): string {
  if (typeof v !== 'string') {
    throw new DepmapError('invalid_structure', `header field ${field} must be a string`);
  }
  return v as string;
}

function requireInt(v: Object, field: string): number {
  if (typeof v !== 'number' || !Number.isSafeInteger(v)) {
    throw new DepmapError('invalid_structure', `header field ${field} must be an integer`);
  }
  return v as number;
}

function requireObject(v: Object, field: string): Object {
  if (typeof v !== 'object' || v === null) {
    throw new DepmapError('invalid_structure', `header field ${field} must be an object`);
  }
  return v;
}

/** 解析 + 结构校验（不执行 KDF）。 */
export function parseDepmapHeader(json: string): DepmapHeader {
  let raw: Object;
  try {
    const parsed: Object = JSON.parse(json) as Object;
    if (parsed === null || typeof parsed !== 'object') {
      throw new DepmapError('invalid_structure', 'container must be a JSON object');
    }
    raw = parsed;
  } catch (e) {
    if (e instanceof DepmapError) {
      throw e;
    }
    throw new DepmapError('invalid_json', 'container is not valid JSON');
  }
  const rec: Record<string, Object> = raw as Record<string, Object>;
  const kdfRaw: Object = requireObject(rec['kdf'], 'kdf');
  const cipherRaw: Object = requireObject(rec['cipher'], 'cipher');
  const kdf: Record<string, Object> = kdfRaw as Record<string, Object>;
  const cipher: Record<string, Object> = cipherRaw as Record<string, Object>;
  return {
    format: requireString(rec['format'], 'format'),
    formatVersion: requireInt(rec['formatVersion'], 'formatVersion'),
    kdf: {
      algorithm: requireString(kdf['algorithm'], 'kdf.algorithm'),
      version: requireInt(kdf['version'], 'kdf.version'),
      salt: requireString(kdf['salt'], 'kdf.salt'),
      memoryKiB: requireInt(kdf['memoryKiB'], 'kdf.memoryKiB'),
      iterations: requireInt(kdf['iterations'], 'kdf.iterations'),
      parallelism: requireInt(kdf['parallelism'], 'kdf.parallelism'),
    },
    cipher: {
      algorithm: requireString(cipher['algorithm'], 'cipher.algorithm'),
      nonce: requireString(cipher['nonce'], 'cipher.nonce'),
    },
    ciphertext: requireString(rec['ciphertext'], 'ciphertext'),
    tag: requireString(rec['tag'], 'tag'),
  };
}

/** V1 边界校验：恶意容器不得触发超大内存 KDF。 */
export function validateDepmapBounds(header: DepmapHeader): void {
  if (header.format !== DEPMAP_FORMAT) {
    throw new DepmapError('bounds', `format must be "${DEPMAP_FORMAT}"`);
  }
  if (header.formatVersion !== DEPMAP_FORMAT_VERSION) {
    throw new DepmapError('bounds', 'formatVersion must be 1');
  }
  if (header.kdf.algorithm !== 'argon2id') {
    throw new DepmapError('bounds', 'kdf.algorithm must be argon2id');
  }
  if (header.kdf.version !== ARGON2_VERSION_19) {
    throw new DepmapError('bounds', 'kdf.version must be 19');
  }
  if (header.cipher.algorithm !== CIPHER_ALGORITHM) {
    throw new DepmapError('bounds', `cipher.algorithm must be ${CIPHER_ALGORITHM}`);
  }
  const memoryKiB: number = header.kdf.memoryKiB;
  if (memoryKiB < MEMORY_KIB_MIN || memoryKiB > MEMORY_KIB_MAX) {
    throw new DepmapError(
      'bounds',
      `kdf.memoryKiB out of bounds [${MEMORY_KIB_MIN},${MEMORY_KIB_MAX}]`,
    );
  }
  if (header.kdf.iterations < ITERATIONS_MIN || header.kdf.iterations > ITERATIONS_MAX) {
    throw new DepmapError(
      'bounds',
      `kdf.iterations out of bounds [${ITERATIONS_MIN},${ITERATIONS_MAX}]`,
    );
  }
  if (header.kdf.parallelism < PARALLELISM_MIN || header.kdf.parallelism > PARALLELISM_MAX) {
    throw new DepmapError(
      'bounds',
      `kdf.parallelism out of bounds [${PARALLELISM_MIN},${PARALLELISM_MAX}]`,
    );
  }
  if (fromBase64(header.kdf.salt).length !== SALT_LEN) {
    throw new DepmapError('bounds', 'kdf.salt must decode to 16 bytes');
  }
  if (fromBase64(header.cipher.nonce).length !== NONCE_LEN) {
    throw new DepmapError('bounds', 'cipher.nonce must decode to 12 bytes');
  }
  if (fromBase64(header.tag).length !== TAG_LEN) {
    throw new DepmapError('bounds', 'tag must decode to 16 bytes');
  }
  const ctLen: number = fromBase64(header.ciphertext).length;
  if (ctLen === 0) {
    throw new DepmapError('bounds', 'ciphertext must not be empty');
  }
  if (ctLen > CIPHERTEXT_MAX_BYTES) {
    throw new DepmapError('bounds', 'ciphertext exceeds 64 MiB limit');
  }
}

// ---------------------------------------------------------------------------
// AES-256-GCM
// ---------------------------------------------------------------------------

function importAesKey(keyBytes: Uint8Array): cryptoFramework.SymKey {
  const generator: cryptoFramework.SymKeyGenerator =
    cryptoFramework.createSymKeyGenerator(SYM_KEY_ALG_NAME);
  return generator.convertKeySync({ data: keyBytes });
}

/** GCM 加密产物：ciphertext 与 tag 分离，与容器字段一一对应。 */
export interface AesGcmSealed {
  ciphertext: Uint8Array;
  tag: Uint8Array;
}

function gcmParams(
  nonce: Uint8Array,
  aad: Uint8Array,
  authTag: Uint8Array,
): cryptoFramework.GcmParamsSpec {
  return {
    iv: { data: nonce },
    aad: { data: aad },
    authTag: { data: authTag },
    algName: GCM_PARAMS_ALG_NAME,
  };
}

/** GCM 加密；返回分离的 ciphertext 与 tag（与容器字段一一对应）。 */
function aesGcmEncrypt(
  keyBytes: Uint8Array,
  nonce: Uint8Array,
  aad: Uint8Array,
  plaintext: Uint8Array,
): AesGcmSealed {
  const cipher: cryptoFramework.Cipher = cryptoFramework.createCipher(AES_GCM_TRANSFORMATION);
  cipher.initSync(
    cryptoFramework.CryptoMode.ENCRYPT_MODE,
    importAesKey(keyBytes),
    gcmParams(nonce, aad, new Uint8Array(TAG_LEN)),
  );
  const ciphertext: Uint8Array = cipher.updateSync({ data: plaintext }).data;
  // GCM 加密时 authTag 由 doFinal 返回（官方指导：doFinal 传入空数据取 tag）
  const tag: Uint8Array = cipher.doFinalSync(null).data;
  return { ciphertext: ciphertext, tag: tag };
}

/** GCM 解密；认证失败抛 auth_failed。 */
function aesGcmDecrypt(
  keyBytes: Uint8Array,
  nonce: Uint8Array,
  aad: Uint8Array,
  ciphertext: Uint8Array,
  tag: Uint8Array,
): Uint8Array {
  const cipher: cryptoFramework.Cipher = cryptoFramework.createCipher(AES_GCM_TRANSFORMATION);
  cipher.initSync(
    cryptoFramework.CryptoMode.DECRYPT_MODE,
    importAesKey(keyBytes),
    gcmParams(nonce, aad, tag),
  );
  const plaintext: Uint8Array = cipher.updateSync({ data: ciphertext }).data;
  // 解密时 doFinal 触发认证校验；失败抛异常
  cipher.doFinalSync(null);
  return plaintext;
}

// ---------------------------------------------------------------------------
// 创建 / 打开容器
// ---------------------------------------------------------------------------

export interface CreateDepmapOptions {
  salt?: Uint8Array;
  nonce?: Uint8Array;
  memoryKiB?: number;
  iterations?: number;
  parallelism?: number;
}

export function createDepmapContainer(
  plaintext: Uint8Array,
  password: string,
  deriver: Argon2idDeriver,
  opts: CreateDepmapOptions = {},
): string {
  const salt: Uint8Array = opts.salt ?? randomBytes(SALT_LEN);
  const nonce: Uint8Array = opts.nonce ?? randomBytes(NONCE_LEN);
  const memoryKiB: number = opts.memoryKiB ?? MEMORY_KIB_DEFAULT;
  const iterations: number = opts.iterations ?? ITERATIONS_DEFAULT;
  const parallelism: number = opts.parallelism ?? PARALLELISM_DEFAULT;

  if (salt.length !== SALT_LEN) {
    throw new DepmapError('bounds', 'salt must be 16 bytes');
  }
  if (nonce.length !== NONCE_LEN) {
    throw new DepmapError('bounds', 'nonce must be 12 bytes');
  }

  const key: Uint8Array = deriver(utf8Bytes(password), salt, memoryKiB, iterations, parallelism);
  if (key.length !== KEY_LEN) {
    throw new DepmapError('kdf', `derived key must be ${KEY_LEN} bytes, got ${key.length}`);
  }

  const header: DepmapHeader = {
    format: DEPMAP_FORMAT,
    formatVersion: DEPMAP_FORMAT_VERSION,
    kdf: {
      algorithm: 'argon2id',
      version: ARGON2_VERSION_19,
      salt: toBase64(salt),
      memoryKiB: memoryKiB,
      iterations: iterations,
      parallelism: parallelism,
    },
    cipher: { algorithm: CIPHER_ALGORITHM, nonce: toBase64(nonce) },
    ciphertext: '',
    tag: '',
  };
  const aad: Uint8Array = computeAad(header);
  const sealed = aesGcmEncrypt(key, nonce, aad, plaintext);
  header.ciphertext = toBase64(sealed.ciphertext);
  header.tag = toBase64(sealed.tag);
  return serializeContainer(header);
}

export function openDepmapContainer(
  json: string,
  password: string,
  deriver: Argon2idDeriver,
): Uint8Array {
  const header: DepmapHeader = parseDepmapHeader(json);
  validateDepmapBounds(header); // 边界校验全部先于 KDF
  const key: Uint8Array = deriver(
    utf8Bytes(password),
    fromBase64(header.kdf.salt),
    header.kdf.memoryKiB,
    header.kdf.iterations,
    header.kdf.parallelism,
  );
  const aad: Uint8Array = computeAad(header);
  try {
    return aesGcmDecrypt(
      key,
      fromBase64(header.cipher.nonce),
      aad,
      fromBase64(header.ciphertext),
      fromBase64(header.tag),
    );
  } catch {
    throw new DepmapError(
      'auth_failed',
      'decryption authentication failed (wrong password or tampered container)',
    );
  }
}
