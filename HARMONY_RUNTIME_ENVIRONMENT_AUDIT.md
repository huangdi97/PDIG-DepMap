# HARMONY RUNTIME ENVIRONMENT AUDIT

> 目的：把「为什么 Harmony 侧至今 `RUNTIME_NOT_RUN`」从一句结论
> 拆解成**可复核的事实清单**，并明确区分
> 「本机缺什么」/「需要用户做什么」/「工程侧还差什么」。
>
> 结论先行：**缺的是 Emulator 系统镜像（需要人工登录华为账号下载），
> 不是工程缺口，也不是配置错误。** 在镜像就位前，任何
> `HARMONY_RUNTIME_E2E = PASS` 的说法都不成立。

- 审计时间：2026-09-18
- 审计方式：直接读取 DevEco 安装目录结构 + 调用真实 `hdc` 二进制，不采信任何既有报告
- 相关文档：`HARMONY_ARGON2_FEASIBILITY.md`、`HARMONY_N3_RUNTIME_REPORT.md`

---

## 1. 环境定位

DevEco Studio 安装根由环境变量提供（仓库不保存本机绝对路径）：

```
PDIG_DEVECO_HOME=<DEVECO_HOME>
```

SDK 目录结构（实测，深度 ≤ 2）：

```
sdk/default/
  hms/
    ets/          native/      previewer/    toolchains/
  openharmony/
    ets/    js/   native/      previewer/    toolchains/
```

**关键发现：SDK 下只有工具链 / 预览器 / 头文件，没有任何系统镜像目录。**
既不存在 `sdk/default/openharmony/images`，也不存在 `sdk/images`。

---

## 2. 逐项审计

| 项 | 期望 | 实测 | 判定 |
| --- | --- | --- | --- |
| DevEco Studio 安装 | 存在 | `$PDIG_DEVECO_HOME` 可用（9 个顶层条目） | **满足** |
| OpenHarmony SDK | 存在 | `sdk/default/openharmony`（5 个子目录） | **满足** |
| HMS SDK | 存在 | `sdk/default/hms`（4 个子目录） | **满足** |
| OHOS NDK / clang | 存在 | `sdk/default/openharmony/native/llvm/bin/clang 15.0.4` 可用，已完成 arm64-v8a + x86_64 交叉编译 | **满足** |
| hvigor | 存在 | `tools/hvigor/hvigor/bin/hvigor.js` 可用，clean assembleHap 成功 | **满足** |
| Emulator 可执行文件 | 存在 | `tools/emulator/Emulator.exe`（38 个条目），`emulator.json` 定义机型模板（Huawei_Phone API11/12/13 等） | **满足** |
| **Emulator 系统镜像** | 存在 | **不存在**：`images` 目录缺失，机型模板只有元数据、无镜像实体 | **缺失（阻断点）** |
| `hdc` 可执行文件 | 存在 | `sdk/default/openharmony/toolchains/hdc.exe` | **满足** |
| **`hdc` 可用目标** | ≥1 | `hdc list targets` → **`[Empty]`** | **缺失** |

---

## 3. 阻断点定性

阻断点**只有一个**，且链条清晰：

```
无系统镜像  →  模拟器无法启动  →  hdc list targets = [Empty]
           →  无法 install HAP / 无 hilog / 无 on-device 执行
           →  HARMONY_RUNTIME_E2E = RUNTIME_NOT_RUN
```

### 为什么这是「需要人工介入」而不是「工程能自己解决」

Emulator 系统镜像**不随 SDK 分发**，必须通过 DevEco Studio 的
Device Manager 以**已登录的华为开发者账号**下载。这涉及：

1. 账号登录（凭据只应由用户本人输入）；
2. 许可协议接受（法律行为，不能由 Agent 代按）；
3. 数 GB 级下载（需要用户的网络与磁盘决策）。

因此本轮在**明确交互点**停止，不伪造任何运行时结论。

### 明确的候选路径（供用户选择，非本轮已执行）

| 路径 | 前置 | 代价 | 备注 |
| --- | --- | --- | --- |
| A. 本地模拟器 | 登录华为账号 + 下载系统镜像 | 数 GB 磁盘 + 下载时间 | 最标准；`x86_64` ABI 已在构建 profile 中备好 |
| B. 远程模拟器 / 云真机 | 登录 + 配额 | 取决于账号权限 | 依赖账号可用性 |
| C. 真实设备 | 一台 HarmonyOS 手机 + USB 调试 | 需要硬件 | 最快拿到真机证据；`arm64-v8a` 已备好 |
| D. 暂不提供运行时 | —— | —— | 保持 `RUNTIME_NOT_RUN`，继续推进不依赖运行时的部分 |

**工程侧已为该选择做好准备的证据**：`build-profile.json5` 的 `abiFilters`
**同时**包含 `arm64-v8a`（真机/标准目标）与 `x86_64`（模拟器系统镜像所需），
两个 ABI 均已实测交叉编译通过并打进 HAP。也就是说
**运行时一旦就位，无需再改构建配置**。

---

## 4. 运行时缺失不影响什么（已完成的替代验证）

必须强调：`RUNTIME_NOT_RUN` 不等于「什么都没验证」。下列结论**不依赖设备**且已取得实证：

| 结论 | 证据 | 状态 |
| --- | --- | --- |
| ArkTS 模块真的被编译 | `check-compiled-reachability.mjs` 的 A/B/C/D 四判据，含负向 probe 真失败 | **PASS** |
| 原生库真的被交叉编译 | `check-argon2-native-build.mjs`：arm64-v8a `AArch64` 40768B / x86_64 `Advanced Micro Devices X86-64` 42296B | **PASS** |
| 原生库真的进了产物 | HAP 内 `libs/arm64-v8a/libpdiargon2.so` + `libs/x86_64/libpdiargon2.so` + `ets/modules.abc` | **PASS** |
| 信任边界真的成立 | 打包并 strip 后的动态符号表**恰好 3 个**：`_init`/`_fini`/`RegisterPdiArgon2Module`，`argon2_*` = 0 | **PASS** |
| 第三方源码真的未改 | `check-third-party-hashes.mjs`：16/16 文件哈希一致，`local_modifications = 0` | **PASS** |
| 容器格式与 Android 一致 | 主机侧黄金校验 5/5 | **PASS** |

**唯一无法在无设备下完成的是**：Argon2 派生在真实 OHOS 运行时上执行、
以及 `p=1..4` 参数透传的设备侧取证。
二者均已在代码中准备好设备可运行的自检入口（`verifyNativeParameterPassthrough()`），
并在 UI 上如实显示结果（失败时显示 `argon2: FAIL(...)`，不伪装成通过）。

---

## 5. 结论与下一步

- **BLOCKER 定性**：外部资源缺失（系统镜像需人工下载），**不是**工程缺口。
  对应 `BLOCKERS.md` 中的真实外部 blocker 类别。
- **本轮动作**：在明确交互点停止，输出本审计；
  **未**尝试绕过、**未**伪造运行时结论、**未**把 `RUNTIME_NOT_RUN` 记作 PASS。
- **需要用户做的唯一一件事**（任选其一）：
  1. 在 DevEco Studio → Device Manager 登录并下载一个 **x86_64** 系统镜像；或
  2. 接入一台开启 USB 调试的 HarmonyOS 真机；或
  3. 明确选择「暂不提供运行时」，我方保持 `RUNTIME_NOT_RUN` 继续推进主机侧工作。
- **用户就绪后的第一步**（工程侧已备好，无需改配置）：

  ```bash
  # 1. 确认设备可见
  "$PDIG_DEVECO_HOME/sdk/default/openharmony/toolchains/hdc.exe" list targets
  # 2. + 3. 构建并以镜像目录为准安装（路径由 Device Manager 决定）
  PDIG_DEVECO_HOME="<DEVECO_HOME>" node tools/harmony/build-ascii-mirror.mjs --clean assembleHap
  # 4. 查看 hilog 中 argon2Probe 一行：
  #    "argon2: native-ok(p=1..4 distinct, lib v19)"  -> 运行时 PASS
  #    "argon2: FAIL(native load/exec: ...)"         -> 如实失败，据此定位
  ```

---

## 7. 主机侧执行面审计（2026-09-18 第四轮新增）

本节回答一个此前**没有被真正验证过**的问题：
「没有设备，是否就真的没有 **任何** 执行面？」

结论：**不是。存在第二个执行面，而且它执行的是真实 ArkTS。**
这一节把它与两个不可用的候选路径一起记录，避免以后重复摸索。

### 7.1 三个候选路径的实测结论

| 候选 | 实测 | 判定 |
| ---- | ---- | ---- |
| `ark_js_vm`（ArkTS 独立虚拟机） | **不存在**。`sdk/default/openharmony/toolchains/` 只有 `es2abc.exe`（编译器）、`ark_disasm.exe`（反汇编器）等，**无运行时** | ❌ 不可用 |
| `Previewer.exe` | 存在（`sdk/default/openharmony/previewer/common/bin/`）。实测需 `-j <app path>` + 与 IDE 的 socket/trace 管道（`LocalSocket::ConnectToServer`），且 `@ohos.*` 是**预览桩** | ❌ 作为证据通道不成立 |
| **hvigor `test` 任务（本地单元测试）** | ✅ **可用**。编译 ArkTS → 在主机执行 → 落 `test_result.txt` | ✅ **采用** |

### 7.2 为什么弃用 Previewer（不是"试不通就放弃"）

它**能**跑，但作为 conformance 的证据通道**不成立**：
预览器的 `@ohos.*` 行为与真实实现不同（是给 UI 预览用的桩），
用它算出来的 PASS **无法归因到 ArkTS 实现本身**。
一个结论不能建立在"看起来像运行"的通道上。

### 7.3 采用路径的真实能力与边界

采用路径（本地单元测试）**真的执行 ArkTS**：`.ets` 由真 ArkTS 编译器产出 JS，
再由主机引擎执行，且**可以读回逐用例结果**：

```
# <mirror>/harmony/entry/.test/default/intermediates/test/coverage_data/test_result.txt
class=harmonyConformanceHost
test=impact/impact-required-edge-loss
result=Success
...
Tests run: 61, Failure: 0, Error: 0, Pass: 61, Ignore: 0
```

**边界（三条，都必须记住）**：

1. 主机对 `@ohos.*` 只提供**不可调用的桩** —— 实测
   `fs.readTextSync(...)` → `is not callable`。
   因此 fixture 文本必须以**数据**形式编译进去（`FixtureBundle.ets`），
   不能走文件读取。
2. 因此 **28 个 @ohos 依赖用例在主机上仍是 `BLOCKED_BY_RUNTIME`**。
   主机执行面**不**覆盖它们。
3. **它仍然不是设备运行时。** `HARMONY_RUNTIME_E2E` 与
   `HARMONY_CONFORMANCE`（设备口径）都不因它改变，
   本节第 1–6 节的结论（缺 Emulator 系统镜像）**依然成立**。

### 7.4 这一条对"阻塞"叙事的修正

前面几节把 `RUNTIME_NOT_RUN` 归因于"缺设备"。那仍然正确，
但**不足以推出"完全没有执行面"** —— 第四轮据此把 57 个运行时无关用例真跑通了。
教训：一个看起来不可绕过的阻塞，值得再问一次"真的吗"。

