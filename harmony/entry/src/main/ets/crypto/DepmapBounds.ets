// DEPMAP_CONTAINER_V1 —— **纯 ArkTS** 的「解析 → 结构 → 边界」层。
//
// 为什么从 DepmapContainerV1.ets 里拆出来
// ---------------------------------------------------------------------------
// 1. **协议顺序要求它先跑完**：容 cryptanalysis 之前必须完成 structure → bounds，
//    恶意容器（超大 memoryKiB / 超长 ciphertext）必须**在**触发 KDF 之前被拒。
//    这条防线如果和 AES / Argon2 的实现写在同一个模块里，就只能在有 crypto runtime
//    的地方被执行 —— 于是"这条规则是否被真的执行过"随平台能力而变，那不是我们想要的。
// 2. **DepmapContainerV1.ets import 了 @kit.CryptoArchitectureKit 与 @kit.ArkTS**：
//    它在模块顶层就 `new util.Base64Helper()`。任何 pull 到它的模块都会带上 @ohos 依赖，
//    conformance runner 一旦 import 它，主机执行面（本地单元测试无 @ohos 实现）立即失效。
//    把它拆出来，"边界是否在 KDF 之前完成"这件事才第一次变得**可被主机验证**。
// 3. **常量此前有两份**：这里手抄了 `depmap` / `65536` / `16384…262144` / `64 MiB` 等，
//    而 generated/CanonicalEnums.ets 的 `DepmapContainerV1` 类早就从
//    spec/security/depmap-container-v1.json 生成了同一批值。
//    手抄副本不一致时没有任何 Gate 会发现 —— 那是典型的第二真相源。
//    现在本模块全部从 generated 取值，只有一处能改动。
//
// 本模块允许被 domain / conformance 层引用（零 @ohos 依赖）。

import { DepmapContainerV1 } from '../generated/CanonicalEnums';

// ---------------------------------------------------------------------------
// 常量（全部派生自 generated，本文件不写死任何魔法数字）
// ---------------------------------------------------------------------------

export const DEPMAP_FORMAT: string = DepmapContainerV1.FORMAT;
export const DEPMAP_FORMAT_VERSION: number = DepmapContainerV1.FORMAT_VERSION;
export const ARGON2_ALGORITHM: string = DepmapContainerV1.KDF_ALGORITHM;
export const ARGON2_VERSION_19: number = DepmapContainerV1.KDF_VERSION;
export const CIPHER_ALGORITHM: string = DepmapContainerV1.CIPHER_ALGORITHM;

export const SALT_LEN: number = DepmapContainerV1.SALT_BYTES;
export const NONCE_LEN: number = DepmapContainerV1.NONCE_BYTES;
export const TAG_LEN: number = DepmapContainerV1.TAG_BYTES;
export const KEY_LEN: number = DepmapContainerV1.KEY_BYTES;

export const MEMORY_KIB_DEFAULT: number = DepmapContainerV1.MEMORY_KIB;
export const ITERATIONS_DEFAULT: number = DepmapContainerV1.ITERATIONS;
export const PARALLELISM_DEFAULT: number = DepmapContainerV1.PARALLELISM;

export const MEMORY_KIB_MIN: number = DepmapContainerV1.MEMORY_KIB_MIN;
export const MEMORY_KIB_MAX: number = DepmapContainerV1.MEMORY_KIB_MAX;
export const ITERATIONS_MIN: number = DepmapContainerV1.ITERATIONS_MIN;
export const ITERATIONS_MAX: number = DepmapContainerV1.ITERATIONS_MAX;
export const PARALLELISM_MIN: number = DepmapContainerV1.PARALLELISM_MIN;
export const PARALLELISM_MAX: number = DepmapContainerV1.PARALLELISM_MAX;
export const CIPHERTEXT_MAX_BYTES: number = DepmapContainerV1.CIPHERTEXT_MAX_BYTES;

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

// ---------------------------------------------------------------------------
// Base64 解码（标准 base64，非加密原语）
//
// 为什么这里自己实现而不是用 util.Base64Helper：`@kit.ArkTS` 在主机执行面不可用，
// 而"边界必须在 KDF 之前"这条规则要能被主机验证就必须在那里能跑。
// 编码方向（toBase64）仍留在依赖平台 runtime 的 DepmapContainerV1.ets ——
// 两个方向互为逆运算且由 RFC 4648 单一定义，不构成两份可漂移的规则。
// ---------------------------------------------------------------------------

const BASE64_ALPHABET: string =
  'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/';

function base64Value(ch: string): number {
  for (let i = 0; i < BASE64_ALPHABET.length; i++) {
    if (BASE64_ALPHABET.charAt(i) === ch) {
      return i;
    }
  }
  return -1;
}

/**
 * 标准 base64 → 字节。
 *
 * 故意**不**容忍非法输入（缺 padding / 非法字符 → `invalid_structure`）：
 * 一个无法解码的 salt 不是"长度不对"，而是容器结构已损坏，
 * 把它降级成长度错误会让定位从"这个字段坏了吗"退化成"为什么 KDF 参数不对"。
 */
export function decodeBase64(s: string): Uint8Array {
  const digits: number[] = [];
  for (let i = 0; i < s.length; i++) {
    const ch: string = s.charAt(i);
    if (ch === '=') {
      continue;
    }
    if (ch === '\n' || ch === '\r' || ch === ' ' || ch === '\t') {
      continue;
    }
    const v: number = base64Value(ch);
    if (v < 0) {
      throw new DepmapError('invalid_structure', 'not a base64 string');
    }
    digits.push(v);
  }
  // 长度必须按**尾部残缺组**补齐，不能只算完整 4 字符组。
  // 反例（真踩到过）：csv-eu-bank-semicolon.csv 长度 mod 3 == 1，
  // base64 末尾是 2 个有效字符 + "=="，仍编码 1 个真实字节。
  // 若按 (n/4|0)*3 分配，那一字节会被静默截断 —— 表现为最后一行的
  // "EUR" 变成 "EU"，且只错最后一行，极易被误判成 CSV 解析 bug。
  const rem: number = digits.length % 4;
  if (rem === 1) {
    throw new DepmapError('invalid_structure', 'truncated base64 group');
  }
  const outLength: number = (digits.length >> 2) * 3 + (rem === 0 ? 0 : rem - 1);
  const out: Uint8Array = new Uint8Array(outLength);
  let o: number = 0;
  let acc: number = 0;
  let bits: number = 0;
  for (let i = 0; i < digits.length; i++) {
    acc = (acc << 6) | digits[i];
    bits += 6;
    if (bits >= 8) {
      bits -= 8;
      if (o < outLength) {
        out[o] = (acc >> bits) & 0xff;
        o++;
      }
    }
  }
  return out;
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
    throw new DepmapError('bounds', `formatVersion must be ${DEPMAP_FORMAT_VERSION}`);
  }
  if (header.kdf.algorithm !== ARGON2_ALGORITHM) {
    throw new DepmapError('bounds', `kdf.algorithm must be ${ARGON2_ALGORITHM}`);
  }
  if (header.kdf.version !== ARGON2_VERSION_19) {
    throw new DepmapError('bounds', `kdf.version must be ${ARGON2_VERSION_19}`);
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
  if (decodeBase64(header.kdf.salt).length !== SALT_LEN) {
    throw new DepmapError('bounds', `kdf.salt must decode to ${SALT_LEN} bytes`);
  }
  if (decodeBase64(header.cipher.nonce).length !== NONCE_LEN) {
    throw new DepmapError('bounds', `cipher.nonce must decode to ${NONCE_LEN} bytes`);
  }
  if (decodeBase64(header.tag).length !== TAG_LEN) {
    throw new DepmapError('bounds', `tag must decode to ${TAG_LEN} bytes`);
  }
  const ctLen: number = decodeBase64(header.ciphertext).length;
  if (ctLen === 0) {
    throw new DepmapError('bounds', 'ciphertext must not be empty');
  }
  if (ctLen > CIPHERTEXT_MAX_BYTES) {
    throw new DepmapError('bounds', 'ciphertext exceeds 64 MiB limit');
  }
}
