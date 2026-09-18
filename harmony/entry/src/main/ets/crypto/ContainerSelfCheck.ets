import {
  computeAad,
  computeAadString,
  utf8String,
  DEPMAP_FORMAT,
  DEPMAP_FORMAT_VERSION,
  ARGON2_VERSION_19,
  CIPHER_ALGORITHM,
  MEMORY_KIB_DEFAULT,
  ITERATIONS_DEFAULT,
  PARALLELISM_DEFAULT,
  type DepmapHeader,
} from './DepmapContainerV1';

/**
 * DEPMAP_CONTAINER_V1 规范化自检（JCS / AAD）。
 *
 * 为什么单独存在：
 * 1. 提供一条**真实的 import 边** —— hvigor 的 CompileArkTS 只编译从
 *    ability / page 可达的模块；未被引用的 .ets 文件根本不进入编译
 *    （实测：在一个未被引用的文件里放语法错误，assembleHap 依然 BUILD SUCCESSFUL）。
 *    因此 crypto/ 下的代码必须由 Index.ets 引用，"编译通过"才是有效证据。
 * 2. 提供一个**设备上可运行的确定性自检** —— AAD 只依赖 JCS 与 header，
 *    不依赖 Argon2id、不依赖 AES 结果。即便 KDF 尚未绑定原生实现，
 *    也能先验证"规范化层"在真机上的逐字节正确性。
 *
 * 冻结依据：
 * - 输入取自 core/src/crypto/golden.ts 的黄金向量（salt / nonce / kdf 参数）；
 * - 期望 AAD 取自同一向量的主机侧参考实现输出（见 HARMONY_CONTAINER_V1_POC.md），
 *   且与 platforms/android/.../DepmapContainerV1.kt 中 buildAad() 的硬编码键序一致。
 *
 * 覆盖范围（诚实标注）：本自检只证明 JCS 键序 + 转义 + UTF-8 编解码正确，
 * **不**覆盖 Argon2id 派生与 AES-GCM —— 那两项需原生 KDF 绑定后另做设备验证。
 */

export const GOLDEN_SALT_BASE64: string = 'ABEiM0RVZneImaq7zN3u/w==';
export const GOLDEN_NONCE_BASE64: string = 'obLD1OX2BxgpOktc';

/**
 * 冻结 AAD（JCS 字符串，UTF-8 编码后 228 字节）。
 * 与 Android buildAad() 的输出逐字符一致。
 */
export const GOLDEN_AAD_EXPECTED: string =
  '{"cipher":{"algorithm":"AES-256-GCM","nonce":"obLD1OX2BxgpOktc"},' +
  '"format":"depmap","formatVersion":1,"kdf":{"algorithm":"argon2id",' +
  '"iterations":3,"memoryKiB":65536,"parallelism":1,' +
  '"salt":"ABEiM0RVZneImaq7zN3u/w==","version":19}}';

export const GOLDEN_AAD_LENGTH: number = 228;

export interface AadSelfCheckResult {
  ok: boolean;
  length: number;
  expectedLength: number;
  /** 不一致时给出原因，便于在设备上看结果。 */
  reason: string;
}

function goldenHeader(): DepmapHeader {
  return {
    format: DEPMAP_FORMAT,
    formatVersion: DEPMAP_FORMAT_VERSION,
    kdf: {
      algorithm: 'argon2id',
      version: ARGON2_VERSION_19,
      salt: GOLDEN_SALT_BASE64,
      memoryKiB: MEMORY_KIB_DEFAULT,
      iterations: ITERATIONS_DEFAULT,
      parallelism: PARALLELISM_DEFAULT,
    },
    cipher: { algorithm: CIPHER_ALGORITHM, nonce: GOLDEN_NONCE_BASE64 },
    ciphertext: '',
    tag: '',
  };
}

/**
 * 计算黄金向量的 AAD 并与冻结值比对。
 * 两级检查：JCS 字符串逐字符相等 + UTF-8 字节长度相等。
 * 第二级单独做，是因为字符串相等但字节不等（编码路径出错）也必须被抓到。
 */
export function runAadSelfCheck(): AadSelfCheckResult {
  const header: DepmapHeader = goldenHeader();
  const actualString: string = computeAadString(header);
  const bytes: Uint8Array = computeAad(header);

  if (actualString !== GOLDEN_AAD_EXPECTED) {
    return {
      ok: false,
      length: bytes.length,
      expectedLength: GOLDEN_AAD_LENGTH,
      reason: 'aad JCS string differs from frozen value',
    };
  }
  if (bytes.length !== GOLDEN_AAD_LENGTH) {
    return {
      ok: false,
      length: bytes.length,
      expectedLength: GOLDEN_AAD_LENGTH,
      reason: `aad utf8 length ${bytes.length} != ${GOLDEN_AAD_LENGTH}`,
    };
  }
  // UTF-8 往返：解码回来必须仍是同一串，否则 TextEncoder 路径有问题
  if (utf8String(bytes) !== GOLDEN_AAD_EXPECTED) {
    return {
      ok: false,
      length: bytes.length,
      expectedLength: GOLDEN_AAD_LENGTH,
      reason: 'aad utf8 round-trip mismatch',
    };
  }
  return { ok: true, length: bytes.length, expectedLength: GOLDEN_AAD_LENGTH, reason: '' };
}

/** 给 UI 用的一行结果。 */
export function aadSelfCheckLine(): string {
  const r: AadSelfCheckResult = runAadSelfCheck();
  if (r.ok) {
    return `container: aad=jcs-ok(${r.length}B)`;
  }
  return `container: aad=FAIL(${r.reason})`;
}
