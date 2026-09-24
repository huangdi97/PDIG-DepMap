# HARMONY_CONTAINER_V1_POC — DEPMAP_CONTAINER_V1 在 Harmony 上的 AES / JCS / container 验证

日期：2026-09-18
阶段：PART B · Harmony N3（承接 `HARMONY_ARGON2_FEASIBILITY.md`）
Android：CORE_FROZEN（不改动） · iOS N4：未进入

---

## 0. 结论

| 项                                      | 结论                           | 证据级别                                                  |
| --------------------------------------- | ------------------------------ | --------------------------------------------------------- |
| Harmony 是否具备 AES-256-GCM + AAD      | **具备**                       | SDK 契约（`GcmParamsSpec{iv,aad,authTag}`）+ 官方指导文档 |
| Harmony 是否具备 RFC 4648 带填充 Base64 | **具备**                       | `util.Base64Helper`，默认 `Type.BASIC`                    |
| JCS（RFC 8785 受限域）能否在 ArkTS 实现 | **能**                         | 已实现并在编译图中通过 ArkTS 严格检查                     |
| AAD / 容器字节是否与冻结向量一致        | **一致**（主机侧独立实现复现） | `tools/harmony/verify-container-golden.mjs` → 5/5 PASS    |
| ArkTS 代码是否真被编译                  | **是**（此前不是，见 §4）      | `modules.abc` 符号级取证                                  |
| ArkTS 运行时是否验证                    | **NOT_RUN**                    | 无设备 / 无系统镜像                                       |
| Argon2id 是否绑定                       | **未绑定**                     | KDF 以 `Argon2idDeriver` 注入，原生绑定待依赖策略 Gate    |
| `.depmap` 协议是否被改动                | **否**                         | 常量与边界逐项对齐 `core/src/crypto/depmap.ts`            |

**一句话**：规范层（JCS + AAD + GCM 参数）已被锁定并可复现；ArkTS 侧实现已完成且**确实进入编译**；
但"Harmony 容器可用"尚未成立 —— 缺 Argon2id 绑定与设备运行时验证，两者都不能用编译成功冒充。

---

## 1. 冻结契约（未做任何协议改动）

直接对齐 `core/src/crypto/depmap.ts` 与 `platforms/android/kotlin/.../DepmapContainerV1.kt`：

- `format = "depmap"`, `formatVersion = 1`
- `kdf.algorithm = "argon2id"`, `kdf.version = 19`
- `cipher.algorithm = "AES-256-GCM"`
- 边界：`memoryKiB ∈ [16384, 262144]`、`iterations ∈ [1,10]`、`parallelism ∈ [1,4]`、
  `salt=16B`、`nonce=12B`、`tag=16B`、`ciphertext ∈ (0, 64 MiB]`
- `AAD = UTF8(JCS({format, formatVersion, kdf, cipher}))`，`ciphertext` / `tag` 不进 AAD
- 解密顺序：`structure → bounds → KDF → GCM auth`（恶意容器不得触发超大内存 KDF）

黄金向量（跨端共用，来自 `core/src/crypto/golden.ts`）：

```
password  = depmap-test
salt      = 00112233445566778899aabbccddeeff
nonce     = a1b2c3d4e5f60718293a4b5c
plaintext = {"app":"depmap","schemaVersion":1,"nodes":[],"dependencies":[]}
kdf       = argon2id v19, 65536 KiB / 3 / 1

derivedKey = 66c4bec7f5e98856747d7b41d0a021bdc092d5e12d492852bd80647bd0ff0c86
ciphertext = KNSpbKK6waj4En3ADgeBB74H96Q7GCoBmNffeOG5QSQ9XJwx4LoJCQ0j8lEinA7GN85U6JwaVMqhAkqWDdG7
tag        = 5qpABhovPbNet1q2GNEhkg==
```

---

## 2. Harmony 平台能力取证

SDK：HarmonyOS API 13（5.0.1.115），`compatibleSdkVersion = 5.0.0(12)`，DevEco Studio 5.0.5.310 / hvigor 5.13.2。

### 2.1 cryptoFramework（`sdk/default/openharmony/ets/api/@ohos.security.cryptoFramework.d.ts`）

抽取到的实际契约（去注释后原文）：

```
interface GcmParamsSpec extends ParamsSpec { iv: DataBlob; aad: DataBlob; authTag: DataBlob; }
interface Cipher {
  initSync(opMode: CryptoMode, key: Key, params: ParamsSpec | null): void;
  updateSync(data: DataBlob): DataBlob;
  doFinalSync(data: DataBlob | null): DataBlob;
  ...
}
interface SymKeyGenerator { convertKeySync(key: DataBlob): SymKey; generateSymKeySync(): SymKey; ... }
function createSymKeyGenerator(algName: string): SymKeyGenerator;
function createCipher(transformation: string): Cipher;
```

GCM 的 tag 语义由官方《加解密算法库框架开发指导》给出：

> GCM 的 authTag 在加密时从 doFinal 结果中获取，在解密时填入 init 函数的 params 参数中

即：加密 `ciphertext = updateSync(plain)`，`tag = doFinalSync(null)`；
解密 `initSync(DECRYPT_MODE, key, {iv, aad, authTag: tag})` 后 `updateSync(ct)`，`doFinalSync(null)` 触发认证。
这与容器把 `ciphertext` 与 `tag` 分列两个字段的结构**天然对应**，
无需像 Node 侧那样手工拼接 `ct||tag`。

### 2.2 未使用 HUKS

`huks.d.ts` 中 `argon2` 命中数 = 0（见 `HARMONY_ARGON2_FEASIBILITY.md`），
且 HUKS 的密钥受设备根密钥保护、不可导出为 32 字节裸密钥。
容器格式要求"口令派生出的确定性裸密钥"用于跨端互操作，HUKS 语义不匹配，故不采用。

### 2.3 编码

- `util.Base64Helper`：`encodeToStringSync(src: Uint8Array, options?: Type)` /
  `decodeSync(src, options?)`，默认 `Type.BASIC` = RFC 4648 标准带填充 —— 与 Node `Buffer.toString('base64')` 一致。
- `util.TextEncoder.encodeInto(s)` / `util.TextDecoder.decodeToString(bytes)`。

---

## 3. 主机侧 PoC（先证明"移植规格"是对的）

在把算法写进 ArkTS 之前，先用一份**不复用 `core/src/crypto`** 的独立实现复现冻结向量，
避免"把同一个错误写两遍"。

已固化为仓库内工具，无第三方依赖：

```
node tools/harmony/verify-container-golden.mjs
```

实测输出：

```
AAD_SHA256=d3312192f01787c4325e23c1d3a45f5d2ee697d37ab0ac64f1c742b756c1a552
AAD_LEN=228
PASS  aad.jcsString
PASS  aad.utf8Length
PASS  aad.sha256
PASS  gcm.ciphertextBase64
PASS  gcm.tagBase64
VERDICT=PASS
```

其中 `aad.jcsString` 比较的是这条冻结 AAD（228 字节）：

```json
{"cipher":{"algorithm":"AES-256-GCM","nonce":"obLD1OX2BxgpOktc"},"format":"depmap","formatVersion":1,"kdf":{"algorithm":"argon2id","iterations":3,"memoryKiB":65536,"parallelism":1,"salt":"ABEiM0RVZneImaq7zN3u/w==","version":19}}
```

它与 `DepmapContainerV1.kt` 中 `buildAad()` 的硬编码键序**逐字符相同** ——
即 Core Node 的通用 JCS 排序结果、Android 的硬编码键序、本工具的独立 JCS 实现，三者在 V1 固定 schema 上收敛到同一字节串。

**这份工具证明的是**：JCS 键序 + 转义 + AAD 构造 + GCM 参数这份"移植规格"正确。
它**不**证明 ArkTS 运行时执行正确（那需要设备）。

---

## 4. 编译取证：一个必须先纠正的假信号

### 4.1 发现

首次把 `crypto/Jcs.ets` 与 `crypto/DepmapContainerV1.ets` 加进工程后，
`assembleHap` 直接 `BUILD SUCCESSFUL`。随后做了负向对照：

| 对照        | 放入的错误                           | assembleHap 结果     |
| ----------- | ------------------------------------ | -------------------- |
| #1 类型错误 | `const n: number = s`（s 为 string） | **BUILD SUCCESSFUL** |
| #2 语法错误 | `return n ;;; }}}`                   | **BUILD SUCCESSFUL** |

结论：这些文件**根本没有被编译**。

符号级取证（`modules.abc` 全文检索）证实：编译产物只含 4 个模块
`domain/Relations`、`entryability/EntryAbility`、`generated/CanonicalEnums`、`pages/Index`；
`jcsStringify` / `DepmapContainerV1`（crypto 版）/ `gateProbe` 命中数均为 0。
（abc 中的 `DepmapContainerV1` 来自 `CanonicalEnums` 的同名 conformance 枚举成员，不是新文件。）

即 **hvigor 的 CompileArkTS 只编译从 ability / page 可达的模块**，
未被 `import` 的 `.ets` 不进入编译图。此时"BUILD SUCCESSFUL"是**空证据**。

### 4.2 纠正

按工程既有惯例（`Index.ets` 已用 `validateRelationUse` 探针确保 domain 真被打进 HAP），
新增 `crypto/ContainerSelfCheck.ets` 并由 `Index.ets` 引用，建立真实 import 边：

```
Index.ets → ContainerSelfCheck.ets → DepmapContainerV1.ets → Jcs.ets
```

### 4.3 纠正后的正/负证据

- **负**（门有效）：重新构建立即报出两条真实 ArkTS 错误并被拦截
  ```
  ERROR File: .../crypto/DepmapContainerV1.ets:345:4
    Object literals cannot be used as type declarations (arkts-no-obj-literals-as-types)
  ERROR File: .../crypto/DepmapContainerV1.ets:355:10
    Object literal must correspond to some explicitly declared class or interface (arkts-no-untyped-obj-literals)
  COMPILE RESULT:FAIL {ERROR:3 WARN:1}
  ```
  → 改为显式 `export interface AesGcmSealed { ciphertext: Uint8Array; tag: Uint8Array }` 后通过。
- **正**（代码在产物里）：`modules.abc` 由 42,916 B 增至 69,036 B，符号检索确认包含
  `entry/ets/crypto/Jcs`、`entry/ets/crypto/DepmapContainerV1`、`entry/ets/crypto/ContainerSelfCheck`
  三个模块及其全部函数（`jcsStringify` / `computeAad` / `aesGcmEncrypt` / `runAadSelfCheck` …），
  并含 `@ohos:security.cryptoFramework` 导入与冻结 AAD 片段。

**这条教训应当被记住**：在本工程里，"ArkTS 编译通过"只有在文件处于
ability/page 可达图中时才构成证据；否则必须同时给出 `modules.abc` 的符号取证。

---

## 5. ArkTS 实现

| 文件                                                       | 内容                                                                    |
| ---------------------------------------------------------- | ----------------------------------------------------------------------- |
| `harmony/entry/src/main/ets/crypto/Jcs.ets`                | RFC 8785 受限域 JCS；`JcsObject` + `jcsStringify` + `jcsEscapeString`   |
| `harmony/entry/src/main/ets/crypto/DepmapContainerV1.ets`  | 常量 / 边界 / header 解析 / AAD / 容器序列化 / AES-256-GCM / 加解密入口 |
| `harmony/entry/src/main/ets/crypto/ContainerSelfCheck.ets` | AAD 规范化自检（设备可运行）+ 建立编译图 import 边                      |
| `harmony/entry/src/main/ets/pages/Index.ets`               | 增加 `containerProbe` 一行，引用自检                                    |

为适配 ArkTS strict 所做的**非协议性**设计取舍：

1. **对象用 `JcsObject` 而非对象字面量**：ArkTS 不做索引签名 / 动态属性访问，
   `Object.keys` 亦不可用；`JcsObject` 显式持有键值对，排序与序列化完全可控。
2. **返回值必须是显式 interface**：`arkts-no-obj-literals-as-types` /
   `arkts-no-untyped-obj-literals` 两条规则（已在 §4.3 实际触发并被修复）。
3. **KDF 以 `Argon2idDeriver` 注入**：Harmony 无 Argon2（`HARMONY_ARGON2_FEASIBILITY.md`），
   把派生器做成参数，容器逻辑本身不依赖任何未定稿的原生绑定，也不允许用占位实现"看起来能跑"。
4. **Base64 统一走 `util.Base64Helper`**：不自己写，避免填充差异破坏跨端互操作。

---

## 6. 状态与未解决项

| 项                           | 状态                               | 说明                                                                           |
| ---------------------------- | ---------------------------------- | ------------------------------------------------------------------------------ |
| JCS / AAD 规格               | **PASS**（主机侧）                 | §3，5/5                                                                        |
| ArkTS 代码                   | **COMPILED**（真实编译，非假信号） | §4.3                                                                           |
| ArkTS 运行时（JCS/AAD 自检） | **NOT_RUN**                        | `ContainerSelfCheck` 已就绪，等设备上跑                                        |
| AES-256-GCM 运行时           | **NOT_RUN**                        | 依赖 cryptoFramework，需设备                                                   |
| Argon2id 绑定                | **PENDING**                        | 待依赖策略 / 第三方声明 / vendoring 决策（`HARMONY_ARGON2_FEASIBILITY.md` §6） |
| 恶意容器边界校验运行时       | **NOT_RUN**                        | `validateDepmapBounds` 已实现，未执行                                          |
| `HARMONY_RUNTIME_E2E`        | **RUNTIME_NOT_RUN**                | 无设备 / 无镜像，不记为 PASS                                                   |

**明确不做**：不因为编译成功就把 `HARMONY_DEPMAP` 标为 PASS；
不用 PBKDF2 / HKDF 等平台自带 KDF 替代 Argon2id（那会改变 `.depmap` 协议并使跨端互操作失效）。

---

## 7. 复现命令

```bash
# 1) 主机侧黄金校验（无第三方依赖）
node tools/harmony/verify-container-golden.mjs

# 2) Harmony 编译（ASCII 镜像构建；仓库路径含中文，hvigor 拒绝非 ASCII 路径）
PDIG_DEVECO_HOME=<DEVECO_HOME> node tools/harmony/build-ascii-mirror.mjs assembleHap

# 3) 编译产物符号取证（确认 crypto 模块真被打进 modules.abc）
node tools/harmony/probe-abc-symbols.mjs
```

---

## 8. 下一步（按 PART B 顺序）

1. Argon2id 绑定的前置 Gate：依赖策略登记、License 选择（CC0-1.0 或 Apache-2.0）、vendoring 决策。
2. hvigor/CMake 集成 + `-fvisibility=hidden`，`p = 1..4` 重新验证。
3. 设备上跑 `ContainerSelfCheck`（先证明规范化层），再跑 Argon2id 黄金向量，最后跑完整容器加解密。
4. Domain（11 组）→ Harmony conformance 向 91/91 → ArkData → HUKS → ArkUI。
5. `HARMONY_RUNTIME_ENVIRONMENT_AUDIT.md`。
