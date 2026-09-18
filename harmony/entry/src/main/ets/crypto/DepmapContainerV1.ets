import { cryptoFramework } from '@kit.CryptoArchitectureKit';
import { util } from '@kit.ArkTS';

import { JcsObject, jcsStringify } from './Jcs';
import { Argon2idDeriver } from './KdfContract';
// 结构 / 边界层：纯 ArkTS，且所有常量派生自 generated —— 本模块**不得**再写死一份。
import {
  ARGON2_VERSION_19,
  CIPHER_ALGORITHM,
  DEPMAP_FORMAT,
  DEPMAP_FORMAT_VERSION,
  ITERATIONS_DEFAULT,
  KEY_LEN,
  MEMORY_KIB_DEFAULT,
  NONCE_LEN,
  PARALLELISM_DEFAULT,
  SALT_LEN,
  TAG_LEN,
  DepmapError,
  // 这两个**必须**在 import 列表里，不能只靠下方的 `export { ... } from`：
  // re-export 只是把名字转发给下游，**不在本模块作用域内绑定**，
  // 于是 `kdfJcs(kdf: DepmapKdfHeader)` 这类注解会报 "Cannot find name"。
  // 分层清理时只补了 re-export，hap 构建（--clean assembleHap）才会暴露 ——
  // 主机 test 构建的编译图不覆盖本文件，因此一度被漏掉。
  DepmapCipherHeader,
  DepmapHeader,
  DepmapKdfHeader,
  parseDepmapHeader,
  validateDepmapBounds,
} from './DepmapBounds';

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
//
// 所有 protocol 常量统一由 ./DepmapBounds 提供（后者派生自 generated）。
// 这里只保留**平台相关**的部分：cryptoFramework 的算法名与 transformation。
// ---------------------------------------------------------------------------

/** cryptoFramework transformation。 */
export const AES_GCM_TRANSFORMATION: string = 'AES256|GCM|PKCS7';
/** cryptoFramework 对称密钥算法名（32 字节 = AES-256）。 */
export const SYM_KEY_ALG_NAME: string = 'AES256';
export const GCM_PARAMS_ALG_NAME: string = 'GcmParamsSpec';

// ---------------------------------------------------------------------------

// ---------------------------------------------------------------------------
// 错误 / 结构 / 结构校验 / 边界校验
//
// 全部迁至 ./DepmapBounds（纯 ArkTS），那里才是它们的单一家。
// 本模块对其做 re-export，是为了不让既有调用方（ContainerSelfCheck 等）
// 因为一次分层清理而被迫改签 —— re-export 不产生第二份定义。
// ---------------------------------------------------------------------------

export {
  ARGON2_ALGORITHM,
  ARGON2_VERSION_19,
  CIPHERTEXT_MAX_BYTES,
  CIPHER_ALGORITHM,
  DEPMAP_FORMAT,
  DEPMAP_FORMAT_VERSION,
  DepmapError,
  DepmapCipherHeader,
  DepmapHeader,
  DepmapKdfHeader,
  ITERATIONS_DEFAULT,
  ITERATIONS_MAX,
  ITERATIONS_MIN,
  KEY_LEN,
  MEMORY_KIB_DEFAULT,
  MEMORY_KIB_MAX,
  MEMORY_KIB_MIN,
  NONCE_LEN,
  PARALLELISM_DEFAULT,
  PARALLELISM_MAX,
  PARALLELISM_MIN,
  SALT_LEN,
  TAG_LEN,
  parseDepmapHeader,
  validateDepmapBounds,
} from './DepmapBounds';

/**
 * Argon2id 派生器。passwordUtf8 必须是**精确 UTF-8 字节**（不做 Unicode 归一化），
 * 否则三端 Golden Vector 无法互操作。
 *
 * 该类型**声明**在 ./KdfContract（见文件顶部 import），此处 re-export 以保持本模块
 * 公开 API 不变。之所以不在此处声明：声明在此会让 Argon2idNative 反过来 import
 * 本模块，使依赖方向反转并导致 Argon2idNative 脱离编译图（详见 KdfContract.ets 顶部）。
 *
 * 注（两条实测约束，勿回退）：
 *   1. ArkTS 不接受 `export type { X } from './Y'` 单行 re-export；
 *   2. ArkTS 的 `import` 必须位于所有其它语句之前（arkts-no-misplaced-imports），
 *      因此 import 放在文件顶部、export 留在原声明位置。
 */
export { Argon2idDeriver };

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
