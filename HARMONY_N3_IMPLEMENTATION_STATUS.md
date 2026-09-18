# HARMONY_N3_IMPLEMENTATION_STATUS.md

> Harmony N3 实施状态。生成时间：2026-09-17；**最近更新：2026-09-18**
> 上游冻结基准：`ANDROID_NATIVE_CORE_FREEZE.md`
> 工程审计：`HARMONY_N3_BASELINE_AUDIT.md`
> 配套文档：`HARMONY_COMPILE_REACHABILITY_GATE.md`、`HARMONY_ARGON2_INTEGRATION_REPORT.md`、
> `HARMONY_RUNTIME_ENVIRONMENT_AUDIT.md`

> **口径声明**：本报告只写**当前事实**。
> 已过期的叙述见 `WORK_STATUS.md` 的 Historical 区，原样保留不改写。
> 任何未在真实环境执行过的项，一律记 `NOT_RUN`。
> 特别地：`HARMONY_BUILD = PASS` **不等于**「ArkTS 都编译了」——
> 后者由 `HARMONY_MODULE_COMPILED` 独立判定（见 §2.1）。

---

## 0. 一句话结论

> N3 已是**唯一活跃主线**。此前 `cryptoFramework` 无 Argon2 造成的
> `HARMONY_DEPMAP = BLOCKED` **已解除** —— 改走**自建 NAPI + vendored PHC 参考实现**，
> 全链路已打通至「打包进 HAP 且符号表只导出 NAPI 入口」。
>
> 本轮同时固化了**编译可达性 Gate**，并据此抓到并修复了一个真实的孤儿模块
> （`Argon2idNative.ets` 因依赖方向反转而从未被编译，构建却是绿的）。
>
> 剩余唯一硬 blocker 是**外部资源**：缺 Emulator 系统镜像，
> 需人工登录华为账号下载。见 `HARMONY_RUNTIME_ENVIRONMENT_AUDIT.md`。
>
> 因此：**Domain 层（§9）才是当前应为之事**，它不依赖运行时。

---

## 1. 本轮实际落地的内容

| # | 内容 | 位置 | 验证方式 | 状态 |
| --- | --- | --- | --- | --- |
| 1 | **编译可达性 Gate** | `tools/harmony/check-compiled-reachability.mjs` | A/B/C/D 四判据实跑 | **PASS** |
| 2 | import 拓扑诊断工具 | `tools/harmony/dump-import-graph.mjs` | 实跑 | PASS |
| 3 | **KDF 契约中立化** | `ets/crypto/KdfContract.ets` | 消除依赖反转 | **COMPILED** |
| 4 | **Argon2 NAPI 正式绑定** | `ets/crypto/Argon2idNative.ets` | 进 `modules.abc` + 负向 probe | **COMPILED** |
| 5 | NAPI 类型声明 | `cpp/types/libpdiargon2/{index.d.ts,oh-package.json5}` | 构建无警告 | PASS |
| 6 | Argon2 vendoring | `third_party/argon2/`（16 文件） | 哈希 Gate 16/16 | **PASS** |
| 7 | 交叉编译 Gate | `tools/harmony/check-argon2-native-build.mjs` | arm64 + x86_64 | **PASS** |
| 8 | 第三方完整性 Gate | `tools/harmony/check-third-party-hashes.mjs` | 16/16，undeclared=0 | **PASS** |
| 9 | 镜像补齐 `third_party/` | `tools/harmony/build-ascii-mirror.mjs` | CMake 不再 FATAL_ERROR | PASS |
| 10 | Stage Model 工程骨架 | `harmony/**` | hvigor 真实构建 | PASS |
| 11 | 纯 ArkTS Domain：Relations | `ets/domain/Relations.ets` | 进 `modules.abc` | COMPILED |
| 12 | DEPMAP_CONTAINER_V1 | `ets/crypto/{Jcs,DepmapContainerV1,ContainerSelfCheck}.ets` | 进 `modules.abc` + 黄金校验 | **COMPILED** |

### 分层遵守情况（§G）

| 层 | 目录 | 状态 |
| --- | --- | --- |
| presentation | `entry/src/main/ets/pages/` | 骨架（1 页 + 3 条探针） |
| application | — | 未开工 |
| **domain** | `entry/src/main/ets/domain/` | **仅 Relations** |
| data | — | 未开工 |
| platform | — | 未开工 |
| security | — | 未开工 |

Domain 层**不依赖** ArkUI / Ability / ArkData / HUKS / NAPI / cryptoFramework ——
已通过导入集合核实（`Relations.ets` 仅依赖 `../generated/CanonicalEnums`）。

---

## 2. N3 Gate 状态

状态取值：`PASS` / `FAIL` / `BLOCKED` / `NOT_RUN` / `PARTIAL_WITH_REPORT` / `COMPILED`。

| Gate | 状态 | 依据 |
| --- | --- | --- |
| `HARMONY_BUILD` | **PASS** | clean `assembleHap` 成功（含 native 编译） |
| `HARMONY_MODULE_COMPILED` | **PASS** | A/B/C/D 四判据，7/7 required 模块；见 §2.1 |
| `HARMONY_DEPMAP` | **NATIVE_BUILD_PASS / ON_DEVICE_NOT_RUN** | 全链路打通至 HAP；设备执行未验证 |
| `HARMONY_CRYPTO` | **COMPILED** | JCS / AAD / AES-256-GCM 已进 `modules.abc`，主机黄金校验 5/5 |
| `HARMONY_DOMAIN` | **PARTIAL_WITH_REPORT** | 仅 Relations；其余 10 组未开工 |
| `HARMONY_CONFORMANCE` | **NOT_RUN** | 无设备/模拟器；仍未接本地测试框架。**不得**写 PASS |
| `HARMONY_ARKDATA` | **NOT_STARTED** | — |
| `HARMONY_MIGRATION` | **NOT_STARTED** | — |
| `HARMONY_REPOSITORY` | **NOT_STARTED** | — |
| `HARMONY_HUKS` | **NOT_STARTED** | — |
| `HARMONY_AUTH` | **NOT_STARTED** | — |
| `HARMONY_ARKUI` | **PARTIAL_WITH_REPORT** | Stage Model + 1 占位页；§L 的 17 页未开工 |
| `HARMONY_IMPORT` | **NOT_STARTED** | — |
| `HARMONY_BACKUP_RESTORE` | **NOT_STARTED** | — |
| `HARMONY_RUNTIME_E2E` | **RUNTIME_NOT_RUN** | 无模拟器系统镜像；`hdc list targets` = `[Empty]` |
| `HARMONY_PRIVACY_SECURITY` | **NOT_STARTED** | — |
| `HARMONY_NATIVE_CORE_HANDOFF` | **FAIL**（未达成） | 见 §2.2 |
| `N3_HARMONY_FULL_PARITY` | **ACTIVE**（parity 仍 0/73） | 见 `NATIVE_PARITY_MATRIX.md` |

### 2.1 `HARMONY_MODULE_COMPILED` 的独立判定（本轮新立，重要）

**`HARMONY_BUILD = PASS` 曾经是一个近乎空心信号**：hvigor 的 `CompileArkTS`
只编译从 ability/page **可达**的模块，未被 import 的 `.ets` 根本不进编译图，
在那种文件里放类型错误，`assembleHap` 依然 `BUILD SUCCESSFUL`。

本轮据此**抓到并修复了一个真实缺陷**：`Argon2idNative.ets` 因
`import { Argon2idDeriver } from './DepmapContainerV1'` 造成依赖方向反转，
成为无人 import 的孤儿 —— 源文件存在、从未被编译、构建却是绿的。

现由 `check-compiled-reachability.mjs` 以 A/B/C/D 四判据守住，实跑：

```
CanonicalEnums      reachable=true  inAbc=true       negative=PASS (via CanonicalEnums)  PASS
Relations           reachable=true  inAbc=true       negative=PASS (via CanonicalEnums)  PASS
Jcs                 reachable=true  inAbc=true       negative=PASS (via CanonicalEnums)  PASS
KdfContract         reachable=true  inAbc=TYPE_ONLY  negative=NOT_RUN                    PASS
DepmapContainerV1   reachable=true  inAbc=true       negative=PASS (via CanonicalEnums)  PASS
ContainerSelfCheck  reachable=true  inAbc=true       negative=PASS (via CanonicalEnums)  PASS
Argon2idNative      reachable=true  inAbc=true       negative=PASS (via CanonicalEnums)  PASS

HARMONY_COMPILE_REACHABILITY=PASS
```

设计细节（含为何纯类型模块豁免 C、为何 D 必须 `--clean`、
为何只跑一个代表模块、以及残留自愈）见 `HARMONY_COMPILE_REACHABILITY_GATE.md`。

### 2.2 `HARMONY_NATIVE_CORE_HANDOFF` 仍未达成

18 个 gate 中：6 PASS / 1 COMPILED×3 / 3 NOT_STARTED / 1 BLOCKED / 1 NOT_RUN / 2 PARTIAL。
**距离达成还有实质工作**，尤其 Domain 与 Conformance。

---

## 3. P0 Blocker 变更记录：Argon2（原 BLOCKED → 已解除）

**原事实**（`BASELINE_AUDIT.md` §3，直接读 SDK 类型声明）：

```
@ohos.security.cryptoFramework.d.ts
KdfSpec ├── PBKDF2Spec └── HKDFSpec
全文检索 Argon2|ARGON|argon2 → 0 命中
```

K 节禁止自研原语，故当时记 `BLOCKED`。

**解除路径（已落地）**：自建 **NAPI 桥接 + vendored PHC 参考实现**，
而非等待平台 API 或引入未审计的 ohpm 包。

排除的绕路（保持排除）：NDK 无 openssl 头/库、无 libsodium/argon2 产物、
**不改用 PBKDF2**（那是降级协议，被明确禁止）。

关键结果：两个 ABI 均编译通过并打进 HAP；打包并 strip 后动态符号表
**恰好 3 个**（`_init` / `_fini` / `RegisterPdiArgon2Module`），`argon2_*` = 0。

详见 `HARMONY_ARGON2_INTEGRATION_REPORT.md`。

---

## 4. HARMONY_BUILD 证据（本轮实跑）

```
> hvigor Finished :entry:default@BuildNativeWithCmake... after 4 s 97 ms
> hvigor Finished :entry:default@BuildNativeWithNinja... after 1 s 172 ms
> hvigor WARN: ArkTS:WARN File: .../crypto/Argon2idNative.ets:24:26
> hvigor BUILD SUCCESSFUL in 19 s 625 ms
```

唯一的 WARN 是 SDK 对 `.so` import 的提示（要求提供 `.d.ts`），
**非错误**；补 `cpp/types/libpdiargon2/index.d.ts` 后语义已显式化。

| 项 | 值 |
| --- | --- |
| 产物 | `entry-default-unsigned.hap` |
| bytes | **2,774,411** |
| 条目数 | 11 |
| native | `libs/arm64-v8a/libpdiargon2.so` (49,776) / `libs/x86_64/libpdiargon2.so` (50,576) |
| ArkTS | `ets/modules.abc` (75,076) |
| 打包后导出符号 | **3**（`_init`/`_fini`/`RegisterPdiArgon2Module`），`argon2_*` = 0 |

> **两点必须记住的坑**：
>
> 1. **hvigor 拒绝非 ASCII 工程路径**，且校验 `process.cwd()`，
>    无环境变量绕过 → 必须做 ASCII 镜像（`tools/harmony/build-ascii-mirror.mjs`）。
>    `mklink /J` **无效**：Node 会把 cwd 解析回真实路径。
> 2. **镜像必须包含 `third_party/`**：`CMakeLists.txt` 用相对路径
>    `../../../../../third_party/argon2` 定位 vendored 源码，
>    只 copy `harmony/` 会导致 CMake `FATAL_ERROR`（本轮已修）。

---

## 5. 关于 Conformance 的诚实口径

`HARMONY_CONFORMANCE = NOT_RUN`。

- 无设备、无模拟器镜像 → ArkTS conformance runner 无法执行。
- **仍未**接 DevEco 本地测试框架 → 无替代执行面。
- `tools/harmony/check-relations-semantics.mjs` 的 18/18 是**源码语义镜像**
  （Node 侧等价实现 vs 同一批 fixtures），**不是 ArkTS 运行时执行结果**，
  因此**不写入** `HARMONY_CONFORMANCE`。这个界限必须继续保持。
- 目标仍是 **91 / 91**；建议顺序：
  relations(18) → impact(13) → readiness(16) → coverage(6) → parser(22) → 其余。

---

## 6. 停止点与下一步

本轮**未触发新的 stop condition**（§21 的 A–E 均未新增达成），
Argon2 已推进至「无设备可验证」的上限。

**唯一硬 blocker** 仍是外部资源：缺 Emulator 系统镜像（需人工下载）。

**下一轮唯一推荐动作：落地 Domain 11 组纯 ArkTS（§9）。**

理由：Argon2（§3–§6）与 ContainerSelfCheck（§8）已达无设备验证上限；
运行时（§16）是外部阻塞；而 Domain 层**不依赖运行时**，
且是 Conformance（§10/§11）与 ArkUI（§14）的前置。

| 优先级 | 事项 | 前置 |
| --- | --- | --- |
| **P0** | **Domain 11 组纯 ArkTS**（Entities → RelationRegistry → LogicalKey → ImpactKernel → PlanReadiness → ScenarioCoverage → StateMachines → GraphRevision → ScenarioTemplate → Timeline） | 无 |
| P1 | ArkTS conformance runner（脱离设备可跑者优先） | Domain |
| P1 | ArkData / Repository / migration | Domain + 运行时（RUNTIME_VERIFIED 需运行时） |
| P2 | ArkUI 纵向链路（§14，**不做 Graph View 优先**） | Domain |
| P2 | HUKS + 用户认证 | 运行时 |
| — | 打通 `HARMONY_RUNTIME_E2E` | **用户操作**（登录 + 下载镜像） |

**约束提醒（不可违反）**：不得实现 RealityDrift / IncidentPlan /
Browser Discovery / Open Banking / AI-LLM / 云同步；
**未进入 iOS N4。**

---

## 7. 复现

```bash
PDIG_DEVECO_HOME="<DEVECO_HOME>"

# 1. 第三方完整性
node tools/harmony/check-third-party-hashes.mjs

# 2. 原生交叉编译
node tools/harmony/check-argon2-native-build.mjs

# 3. 编译可达性（A+B+C+D）
node tools/harmony/check-compiled-reachability.mjs --build

# 4. 全清构建
node tools/harmony/build-ascii-mirror.mjs --clean assembleHap

# 5. 诊断：import 拓扑 / 孤儿模块
node tools/harmony/dump-import-graph.mjs
```
