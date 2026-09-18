/**
 * Argon2id Native —— ArkTS 侧对 NAPI 桥接的正式绑定。
 *
 * 链路：ArkTS → NAPI(libpdiargon2.so) → libargon2(PHC 参考实现) → derivedKey
 *
 * 可信边界（刻意最小化）：
 *   原生层**只**做 Argon2id KDF。它不接收 JSON 容器、不解析 .depmap、
 *   不做 AES、不做 JCS。这些全部留在 ArkTS（见 DepmapContainerV1.ets），
 *   因为 ArkTS 侧有编译 Gate 与 conformance runner 覆盖，
 *   而原生层每多一项职责就多一份不可验证的信任面。
 *
 * 原生层不做参数隐式替换：
 *   parallelism 与 version 逐值透传，桥接层不得偷偷 hardcode p=1 / v=19。
 *   `verifyNativeParameterPassthrough()` 正是为了在设备上证明这一点
 *   （p=1..4 必须产生 4 个互不相同的派生密钥；若被 hardcode 成 p=1，四者会相等）。
 *
 * 状态：
 *   SOURCE_READY / COMPILED —— 本源代码进入 CompileArkTS 编译图，
 *     并由 CMake 交叉编译 arm64-v8a + x86_64 两个 ABI（见 build Profile）。
 *   ON_DEVICE_EXECUTION = NOT_RUN —— 无 Harmony 设备与模拟器镜像
 *     （`hdc list targets = [Empty]`，见 HARMONY_RUNTIME_ENVIRONMENT_AUDIT.md）。
 *   因此**不得**把本模块标为运行时验证通过。
 */
import nativeArgon2 from 'libpdiargon2.so';

import { Argon2idDeriver } from './KdfContract';

/** Argon2 RFC 9106 / PHC 版本号：0x13 = 19。 */
export const ARGON2_VERSION_13: number = 19;
/** 兼容性最低版本：0x10 = 16。V1 容器只用 19，但桥接层需能表达 16 以便测试。 */
export const ARGON2_VERSION_10: number = 16;

/** 派生密钥长度（字节）。DEPMAP_CONTAINER_V1 固定 32。 */
export const DERIVED_KEY_LENGTH: number = 32;

/**
 * native 模块的形状。ArkTS 下不允许无类型对象字面量，
 * 因此必须显式声明 interface。
 */
export interface PdiArgon2Native {
  deriveArgon2id(
    password: Uint8Array,
    salt: Uint8Array,
    memoryKiB: number,
    iterations: number,
    parallelism: number,
    outputLength: number,
    version: number,
  ): Uint8Array;
  argon2Version(): number;
}

/** 原生层返回的 Argon2 库版本（应为 19）。 */
export function nativeArgon2Version(): number {
  const native: PdiArgon2Native = nativeArgon2 as PdiArgon2Native;
  return native.argon2Version();
}

/**
 * 正式派生入口：满足 `Argon2idDeriver` 契约，可直接注入容器层。
 *
 * 密码以**精确 UTF-8 字节**传入，基线层外的调用方负责不做 Unicode 归一化
 * （DepmapContainerV1 用 util.TextEncoder 直接编码，不做 NFC/NFD）。
 */
export const deriveArgon2id: Argon2idDeriver = (
  passwordUtf8: Uint8Array,
  salt: Uint8Array,
  memoryKiB: number,
  iterations: number,
  parallelism: number,
): Uint8Array => {
  const native: PdiArgon2Native = nativeArgon2 as PdiArgon2Native;
  return native.deriveArgon2id(
    passwordUtf8,
    salt,
    memoryKiB,
    iterations,
    parallelism,
    DERIVED_KEY_LENGTH,
    ARGON2_VERSION_13,
  );
};

// ---------------------------------------------------------------------------
// 设备可运行自检：参数透传（防止原生层 hardcode p=1）
// ---------------------------------------------------------------------------

export interface ParallelismPassthroughResult {
  ok: boolean;
  /** 四档 p 各自派生出的密钥（hex），供设备上肉眼比对 / 日志外取证。 */
  keys: string[];
  reason: string;
}

const PARALLELISM_SELFTEST_SALT_HEX: string = '00112233445566778899aabbccddeeff';

function hexToBytes(hex: string): Uint8Array {
  const out: Uint8Array = new Uint8Array(hex.length / 2);
  for (let i = 0; i < out.length; i++) {
    out[i] = parseInt(hex.substr(i * 2, 2), 16);
  }
  return out;
}

function bytesToHex(b: Uint8Array): string {
  let s: string = '';
  for (let i = 0; i < b.length; i++) {
    s += b[i].toString(16).padStart(2, '0');
  }
  return s;
}

/**
 * 用固定 password/salt、仅改变 parallelism（1→4）派生四次。
 *
 * 判定：四个结果必须**两两不同**。
 *   - 若原生层把 p hardcode 成 1，则四次结果相同 → FAIL（这正是要抓的缺陷）。
 *   - memoryKiB 取最小值以控制设备上耗时：p 只影响 lane 划分，不影响内存量级。
 */
export function verifyNativeParameterPassthrough(): ParallelismPassthroughResult {
  const password: Uint8Array = new Uint8Array([0x64, 0x65, 0x70, 0x6d, 0x61, 0x70, 0x2d, 0x74, 0x65, 0x73, 0x74]); // "depmap-test"
  const salt: Uint8Array = hexToBytes(PARALLELISM_SELFTEST_SALT_HEX);
  const memoryKiB: number = 16384; // 规范下界，控制耗时
  const iterations: number = 1;

  const keys: string[] = [];
  for (let p = 1; p <= 4; p++) {
    const k: Uint8Array = deriveArgon2id(password, salt, memoryKiB, iterations, p);
    if (k.length !== DERIVED_KEY_LENGTH) {
      return { ok: false, keys: keys, reason: `p=${p} returned ${k.length} bytes, expected ${DERIVED_KEY_LENGTH}` };
    }
    keys.push(bytesToHex(k));
  }

  for (let i = 0; i < keys.length; i++) {
    for (let j = i + 1; j < keys.length; j++) {
      if (keys[i] === keys[j]) {
        return {
          ok: false,
          keys: keys,
          reason: `p=${i + 1} and p=${j + 1} produced identical keys — native layer is ignoring parallelism`,
        };
      }
    }
  }

  const libVersion: number = nativeArgon2Version();
  if (libVersion !== ARGON2_VERSION_13) {
    return { ok: false, keys: keys, reason: `linked libargon2 reports version ${libVersion}, expected ${ARGON2_VERSION_13}` };
  }

  return { ok: true, keys: keys, reason: '' };
}

/** 给 UI 用的一行结果。 */
export function argon2SelfCheckLine(): string {
  try {
    const r: ParallelismPassthroughResult = verifyNativeParameterPassthrough();
    if (r.ok) {
      return `argon2: native-ok(p=1..4 distinct, lib v${ARGON2_VERSION_13})`;
    }
    return `argon2: FAIL(${r.reason})`;
  } catch (e) {
    // 原生库缺失 / 加载失败：如实报告，不伪装成通过。
    return `argon2: FAIL(native load/exec: ${e})`;
  }
}
