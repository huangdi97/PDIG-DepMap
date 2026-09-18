/**
 * KDF 契约 —— 容器层与原生桥接层之间的**唯一**共享面。
 *
 * 为什么单独成文件（而不是留在 DepmapContainerV1.ets 里）：
 *   `Argon2idDeriver` 原本声明在 DepmapContainerV1.ets 中，导致
 *   Argon2idNative.ets 必须 `import ... from './DepmapContainerV1'` 才能拿到该类型。
 *   那条 import 让依赖方向**反转**成
 *       Argon2idNative → DepmapContainerV1
 *   而逻辑上真正的依赖是
 *       DepmapContainerV1 → Argon2idNative
 *   反转的后果是实测出来的：没有任何模块 import Argon2idNative，
 *   于是它不进入 hvigor 的 CompileArkTS 可达图，源文件存在但**从未被编译**，
 *   而 assembleHap 依然 BUILD SUCCESSFUL —— 一个纯假信号。
 *
 * 现在两个方向都只依赖本文件，构成无环的 DAG：
 *       KdfContract  ←  DepmapContainerV1
 *       KdfContract  ←  Argon2idNative
 *   Argon2idNative 由 pages/Index.ets 直接 import 进入编译图，
 *   由 tools/harmony/check-compiled-reachability.mjs 的正向 + 负向 probe 守住。
 *
 * 契约稳定性：本文件的类型签名与 DEPMAP_CONTAINER_V1 冻结格式绑定，
 * 修改它等同于修改协议，必须走 CANONICAL_DESIGN / DECISION_LOG 流程。
 */

/**
 * Argon2id 派生器。passwordUtf8 必须是**精确 UTF-8 字节**（不做 Unicode 归一化），
 * 否则三端 Golden Vector 无法互操作。
 *
 * 返回固定 KEY_LEN = 32 字节派生密钥。
 */
export type Argon2idDeriver = (
  passwordUtf8: Uint8Array,
  salt: Uint8Array,
  memoryKiB: number,
  iterations: number,
  parallelism: number,
) => Uint8Array;
