# HARMONY_N3_RUNTIME_REPORT.md

> Harmony 运行时报告。生成时间：2026-09-17（Asia/Shanghai）
> **最近更新：2026-09-18**
> 口径：§N —— **不得用 ArkTS 编译成功代替 runtime PASS**。编译 ≠ 运行。
> 另见：`HARMONY_RUNTIME_ENVIRONMENT_AUDIT.md`（阻断点根因）

---

## 0. 结论（按 §N 强制拆分）

```
HARMONY_SOURCE_READY       = READY
HARMONY_BUILD_READY        = PASS
HARMONY_MODULE_COMPILED    = PASS      (2026-09-18 新增，四判据)
HARMONY_NATIVE_BUILD       = PASS      (2026-09-18 新增，2 ABI)
ON_DEVICE_EXECUTION        = NOT_RUN
HARMONY_RUNTIME_E2E        = RUNTIME_NOT_RUN
```

`HARMONY_RUNTIME_E2E` 不是"跑了失败"，而是**根本没有可跑的目标**。

> 2026-09-18 新增的两行是刻意分开的：
> `HARMONY_MODULE_COMPILED` / `HARMONY_NATIVE_BUILD` 证明「代码真的被编译、被打包」，
> 但它们**一个字都没有说明**代码在 OHOS 上跑起来的行为。
> 把它们与 `RUNTIME_E2E` 混在一起报，正是 §N 要禁止的那种混淆。

---

## 1. 运行时环境实测

| 项 | 命令 / 位置 | 实测结果 |
| --- | --- | --- |
| hdc 可执行文件 | `.../sdk/default/openharmony/toolchains/hdc.exe` | 存在 |
| **已连接目标** | `hdc list targets` | **`[Empty]`** |
| Emulator 二进制 | `DevEco Studio/tools/emulator/Emulator.exe` | 存在（38 个条目，含 `emulator.json` 机型模板） |
| **模拟器系统镜像** | 检索 SDK 内 `images` 目录 | **不存在**；SDK 下只有 `toolchains/ previewer/ ets/ native/` |

**blocker 性质**：模拟器系统镜像只能通过 **DevEco Studio GUI 下载**（需华为账号登录）。
这与 Android 侧"需在 SDK Manager 安装 system-image"（B18）同类，属**用户侧外部闸门**，
不是工程缺口 —— 因此**不用代码去填**，按 §N 如实拆分报告。

**工程侧已为"镜像就位"做好准备**：`build-profile.json5` 的 `abiFilters`
同时含 `arm64-v8a`（真机）与 `x86_64`（模拟器镜像所需），两者均已实测编译通过并打进 HAP。
**运行时一旦可用，无需再改构建配置。**

---

## 2. BUILD（已真实发生）

`HARMONY_BUILD_READY = PASS`。

```
> hvigor Finished :entry:default@BuildNativeWithCmake... after 4 s 97 ms
> hvigor Finished :entry:default@BuildNativeWithNinja... after 1 s 172 ms
> hvigor Finished :entry:default@CompileArkTS... after 10 s 429 ms
> hvigor BUILD SUCCESSFUL in 19 s 625 ms
```

| 项 | 值 |
| --- | --- |
| 产物 | `entry-default-unsigned.hap` |
| bytes | **2,774,411** |
| 条目数 | 11 |
| native | `libs/arm64-v8a/libpdiargon2.so` (49,776) / `libs/x86_64/libpdiargon2.so` (50,576) |
| ArkTS | `ets/modules.abc` (75,076) |
| 签名 | **未签名**（无 signingConfig —— 与 Android 侧缺生产 keystore 同类的外部闸门） |
| 打包后导出符号 | **3**（`_init`/`_fini`/`RegisterPdiArgon2Module`），`argon2_*` = 0 |

> **`BUILD SUCCESSFUL` 的历史陷阱**：hvigor 的 `CompileArkTS` 只编译从
> ability/page **可达**的模块。未被 import 的 `.ets` 根本不进编译图，
> 在那种文件里放类型错误，构建依然成功。
> 本轮据此抓到真实缺陷：`Argon2idNative.ets` 因依赖方向反转成为孤儿，
> 从未被编译，而构建是绿的。
> **故 `BUILD SUCCESSFUL` 必须与 `HARMONY_MODULE_COMPILED` 的 A/B/C/D 四判据一起看。**
> 详见 `HARMONY_COMPILE_REACHABILITY_GATE.md`。

> **HAP 非确定性**：两次全清重建字节数相同或接近，但 sha256 不同
> （包内嵌时间戳类字段）。做哈希取证时必须意识到这一点。

---

## 3. E2E 行程（§N 清单）—— 全部 NOT_RUN

以下每一项都**没有在 Harmony 上跑过**：

| # | 行程 | 状态 |
| --- | --- | --- |
| 1 | fresh install | NOT_RUN |
| 2 | launch | NOT_RUN |
| 3 | lock / unlock | NOT_RUN |
| 4 | import | NOT_RUN |
| 5 | proposal | NOT_RUN |
| 6 | confirm reality | NOT_RUN |
| 7 | impact | NOT_RUN |
| 8 | changeplan | NOT_RUN |
| 9 | done ≠ verified | NOT_RUN |
| 10 | verification | NOT_RUN |
| 11 | restart（进程重启后数据仍在） | NOT_RUN |
| 12 | backup | NOT_RUN |
| 13 | restore | NOT_RUN |

### 3.1 三条探针的实际状态

`pages/Index.ets` 的 `aboutToAppear()` 中已埋三条探针，
**代码已编译、已打包**，但**从未在设备上执行**：

| 探针 | 期望读数 | 状态 |
| --- | --- | --- |
| relations | `relations: funding_source=allow, bound_to=reject` | **NOT_RUN** |
| container | AAD 自检行（长度 228） | **NOT_RUN** |
| argon2 | `argon2: native-ok(p=1..4 distinct, lib v19)` | **NOT_RUN** |

argon2 探针失败时会如实返回 `argon2: FAIL(native load/exec: ...)` ——
**该分支的存在就是为了让失败可见且不可伪装**。

---

## 4. 已提前规避的一个坑（§M，D-16 教训）

Harmony 侧已按 §M 设计方向写进代码注释与分层约束：

- 外部文件选择器（`@ohos.file.picker`）会运行在**独立任务 / 独立 UIAbility**，
  等价于 Android 的 `Activity.onStop → LockGate.lockNow()`。
- 因此 N3 **不得**把 Import / Restore 的待投递结果只挂在页面级组件状态里；
  协调职责由 **application 层 `FileWorkflowCoordinator`** 承担（尚未实现）。
- `EntryAbility.ets` 已在注释中固化该约束，避免后续实现者重演 D-16。

**现状**：该 coordinator 尚未实现（application 层 = 未开工），故此项为**设计已定、实现未落地**。
属 §15 的 `HARMONY_D16_AVOIDANCE`，当前记 **NOT_STARTED**。

---

## 5. 无设备下**已**取得的替代证据及其边界

| 结论 | 工具 | 结果 |
| --- | --- | --- |
| ArkTS 模块真的被编译 | `check-compiled-reachability.mjs --build` | **PASS**（7/7 required，含负向 probe） |
| 原生库真的被交叉编译 | `check-argon2-native-build.mjs` | **PASS**（arm64-v8a / x86_64） |
| 原生库真的进了产物 | HAP 内容枚举 | **PASS** |
| 信任边界真的成立 | 打包 strip 后读动态符号表 | **PASS**（`argon2_*` = 0） |
| 第三方源码真的未改 | `check-third-party-hashes.mjs` | **PASS**（16/16） |
| 容器格式与 Android 一致 | `verify-container-golden.mjs` | **PASS**（5/5，主机侧） |

**边界必须说清**：这套证据证明「代码正确、被编译、被打包、边界正确」，
**不证明**「代码在 OHOS 运行时会给出正确结果」。后者只能由设备侧执行证明。

### 5.1 负向证据（同样重要）

D 判据提供负向取证：向 `CanonicalEnums.ets` 注入类型错误后，构建**真的失败**：

```
> hvigor ERROR: Failed :entry:default@CompileArkTS...
1 ERROR: ArkTS:ERROR File: .../generated/CanonicalEnums.ets:350:7
COMPILE RESULT:FAIL {ERROR:2 WARN:1}
> hvigor ERROR: BUILD FAILED in 15 s 56 ms
```

价值：排除**沉默失效**。如果构建无论如何都成功，
「BUILD SUCCESSFUL」就完全不能说明任何事。负向 probe 确认了构建信号**有区分度**。

---

## 6. 解除 RUNTIME_NOT_RUN 的前置条件

1. **在 DevEco Studio 中下载并启动一个系统镜像**（**用户操作**，需登录）；或接入真机。
   → 建议优先 **x86_64** 镜像（构建 profile 已备好该 ABI）。
2. 解决 HAP 签名（当前 unsigned，等同 Android 侧缺生产 keystore 的外部闸门）。
3. 实现 §L 的核心纵向链页面与 §M 的 workflow coordinator（依赖 Domain 层）。

满足 1 之后，才可能产出 `HARMONY_RUNTIME_E2E` 的真实 PASS / FAIL 判定。

**在此之前可做且应当做的**：推进 Domain 11 组（§9）——
它不依赖运行时，是 conformance 与 ArkUI 的共同前置。
