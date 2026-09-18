/**
 * libpdiargon2.so 的 ArkTS 类型声明。
 *
 * 用途：让 `import nativeArgon2 from 'libpdiargon2.so'` 有明确契约，
 * 消除 SDK 的
 *   "Currently module for 'libpdiargon2.so' is not verified ... make sure the
 *    corresponding .d.ts file is provided and the napis are correctly declared."
 *
 * 可信边界（刻意最小化，与 pdi_argon2.cpp 一一对应）：
 *   本原生层**只**做 Argon2id KDF。
 *   它不接收 JSON 容器、不解析 .depmap、不做 AES、不做 JCS。
 *   其余职责全部留在 ArkTS（见 DepmapContainerV1.ets）。
 *
 * 参数不做隐式替换：parallelism 与 version 逐值透传。
 *   桥接层不得 hardcode p=1 / v=19 —— 由 Argon2idNative.verifyNativeParameterPassthrough()
 *   在设备上以 p=1..4 产生 4 个互异密钥来证明。
 */

/**
 * @param password   口令的**精确 UTF-8 字节**（调用方负责不做 Unicode 归一化）
 * @param salt       盐，DEPMAP_CONTAINER_V1 固定 16 字节
 * @param memoryKiB  内存开销（KiB），接受 16384..262144
 * @param iterations 迭代次数，接受 1..10
 * @param parallelism 并行度，接受 1..4
 * @param outputLength 派生密钥长度（字节），V1 固定 32
 * @param version    Argon2 版本，19 (0x13) 或 16 (0x10)
 * @returns 派生密钥；任何参数越界或 KDF 失败均抛出异常（fail closed）
 */
export const deriveArgon2id: (
  password: Uint8Array,
  salt: Uint8Array,
  memoryKiB: number,
  iterations: number,
  parallelism: number,
  outputLength: number,
  version: number,
) => Uint8Array;

/** 链接进来的 libargon2 报告的版本号。V1 期望 19。 */
export const argon2Version: () => number;
