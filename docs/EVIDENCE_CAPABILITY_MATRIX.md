# EVIDENCE_CAPABILITY_MATRIX.md — 三端「能拿到什么级别的证据」盘点

> 目的：回答一个具体问题 —— **对某一端的某条结论，我们手上到底有什么级别的证据？
> 再往上一个级别需要什么？那个缺口是代码能解决的，还是外部（账号/设备/工具链）？**
>
> 本文不评价功能是否完整（见 `NATIVE_PARITY_MATRIX.md`），也不评价是否可发布
> （见 `PLATFORM_RELEASE_MATRIX.md`），只盘点**证据能力**。
>
> 盘点日期：2026-11-14 · 依据：仓库内 workflow 定义、`ci.yml` 的诚实范围声明、
> `artifacts/runtime-evidence/**` 内已入库的报告与帧。

---

## 0. 证据阶梯（固定口径）

每一端同一件事都要能定位到**一格**，不得跨格引用：

| 级别 | 名称                | 判定依据（必须能指到东西）                                    |
| ---- | ------------------- | ------------------------------------------------------------- |
| T0   | `SOURCE`            | 源码存在且被 spec 覆盖                                        |
| T1   | `COMPILED`          | CI 或本机真实构建命令产出的日志/产物                          |
| T2   | `TESTED`            | 单测/本地测试真实执行的计数                                   |
| T3   | `CONFORMANCE`       | 对 canonical fixtures 的逐用例 diff 报告（`<platform>.json`） |
| T4   | `STATIC_RENDER`     | 渲染器产出的真实画面（非 mock、非手工拼图）                   |
| T5   | `SIMULATOR_RUNTIME` | 模拟器/仿真设备上真实跑起来的交互证据                         |
| T6   | `DEVICE_RUNTIME`    | 真机上的运行证据                                              |
| T7   | `STORE_READY`       | 签名、商店素材、政策项齐备                                    |

铁律：**T4 不提升 T5，T5 不提升 T6**。模拟器截图不能当"真机已验证"，
macOS 渲染不能当"App 跑起来了"。

---

## 1. 能力矩阵（当前事实）

| 级别                   | Android                                                                                                                                                       | iOS                                                                                                                                                                     | HarmonyOS                                                                                                                                                                                 |
| ---------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| T0 `SOURCE`            | ✅                                                                                                                                                            | ✅                                                                                                                                                                      | ✅（74 个 `.ets`）                                                                                                                                                                        |
| T1 `COMPILED`          | ✅ CI `job android-app`：`:app:assembleDebug`                                                                                                                 | ✅ CI `ios.yml`：Swift build（PDIGCore + PDIGConformance + PDIGApp）                                                                                                    | ⚠️ **仅本机**：`HARMONY_COMPILE_REACHABILITY_LOCAL = PASS`（DevEco hvigor clean assembleHap + modules.abc 取证 + 负向 probe）。CI 端 `HARMONY_GITHUB_HOSTED_NATIVE_BUILD = NOT_AVAILABLE` |
| T2 `TESTED`            | ✅ CI：`:core:test` + `:app:testDebugUnitTest`                                                                                                                | ✅ CI：canonical 91+37、core smoke、`PDIGAppTests`                                                                                                                      | ⚠️ **仅本机**：`HARMONY_HOST_CONFORMANCE_LOCAL = PASS 89/89`（canonical 85/91 + 3 元测试 + 1 域自检）                                                                                     |
| T3 `CONFORMANCE`       | ✅ CI 产出 `android.json`，由 `canonical` job 下载后逐用例 diff（`needs` 串行，避免恒为 NOT_RUN）                                                             | ✅ **161/161**，报告已入库 `artifacts/runtime-evidence/2026-11-12-ios-conformance/ios.json`                                                                             | ❌ `NOT_RUN`：不产出 `harmony.json`，orchestrator 如实报 NOT_RUN                                                                                                                          |
| T4 `STATIC_RENDER`     | ⚠️ 只有**本机 AVD 真截图**，不是静态渲染器；`FLAG_SECURE` 帧按设计全黑                                                                                        | ✅ 60 张（30 屏 × light/dark），macOS SwiftUI `ImageRenderer`，逐文件 `MACOS_RENDER`                                                                                    | — 无渲染通道                                                                                                                                                                              |
| T5 `SIMULATOR_RUNTIME` | ❌ CI `ANDROID_EMULATOR_E2E = NOT_RUN`（hosted Linux runner 无 KVM）；**本机 AVD** 有 post-freeze 取证（19/19 × 多种 fontScale、88 帧语义证据 + 12 张真截图） | ✅ **8/8**，`run 43572807640` / `43606405863` 两次独立整条 success；iPhone 16 Pro Max + iPad 两条腿；35 帧/腿已由 `xcresulttool export attachments` 导出为独立 artifact | ❌ `NOT_RUN`                                                                                                                                                                              |
| T6 `DEVICE_RUNTIME`    | ❌ `NOT_RUN`（无真机证据）                                                                                                                                    | ❌ `NOT_RUN`（无签名、无真机）                                                                                                                                          | ❌ `HARMONY_DEVICE_RUNTIME = NOT_RUN`                                                                                                                                                     |
| T7 `STORE_READY`       | ❌ 外部 blocker（签名/素材）                                                                                                                                  | ❌ 外部 blocker（Apple Developer）                                                                                                                                      | ❌ 外部 blocker（Huawei 身份/签名）                                                                                                                                                       |
| 人眼验收               | `HUMAN_VISUAL_ACCEPTANCE = NOT_RUN`                                                                                                                           | `IOS_HUMAN_VISUAL_ACCEPTANCE = NOT_RUN`                                                                                                                                 | 无材料                                                                                                                                                                                    |

### 1.1 可浏览的证据页

| 端      | 页面                      | 内容                                                                             |
| ------- | ------------------------- | -------------------------------------------------------------------------------- |
| Android | gh-pages `/human-review/` | 88 帧语义判据 + 12 张真截图（`FLAG_SECURE` 帧只给判据，不摆黑图）                |
| iOS     | gh-pages `/ios-review/`   | 60 张 macOS 渲染 + 70 帧模拟器运行期（35 帧 × iPhone/iPad，页面副本 660px 缩放） |

---

## 2. 每一格的缺口与升级路径

### Android：T5 卡在"没有可用的模拟器执行环境"

- 现状：`androidTest` 有 **52 个**测试类，但在 CI 里一个都没跑（`ci.yml` 明确不建 emulator job，
  因为 hosted Linux runner 没有 KVM 硬件加速）。
- 已有替代：本机 AVD 的 post-freeze 取证（`artifacts/runtime-evidence/2026-09-27-closure-android/` 等），
  口径必须是 **LOCAL_EMULATOR_RUNTIME**，不得写成 CI 通过。
- 升级到 CI 内 T5 的可行路径（按代价排序）：
  1. GitHub `macos-15` runner 跑 arm64 Android emulator（有硬件加速，代价是 macOS 分钟数）；
  2. self-hosted Linux runner + `/dev/kvm`；
  3. 接受现状，只保留本机 AVD 证据。
- 结论：**代码可解，代价是基础镜像/runner 选择**，不是外部 blocker。

### iOS：T6 只差一个签名账号

- 现状：T3/T4/T5 全部 PASS 且有入库证据；T6 空白是因为没有 Apple Developer 账号与真机。
- 升级路径：签名（自动签名 + 一台真机）→ 把 `ios-runtime-visual.yml` 的 destination 换成真机，
  XCUITest 用例不需要改动（已经是 `XCUIApplication` 通用写法）。
- 结论：**外部 blocker（账号/设备）**，不是工程缺口。

### HarmonyOS：T1/T2 只有本机，T3 无报告

- 现状：74 个 `.ets`，本机 DevEco 能 clean 构建、能跑 89 个本地单测；但 CI 不建 Harmony job
  （hvigor 5.13.2 只以 `file:` 协议引用 DevEco 内置包，公共 registry 无此版本）。
- 后果：orchestrator 对 harmony 恒报 `NOT_RUN`，这是**诚实结论**，不是遗漏。
- 升级路径：
  1. self-hosted Windows runner + 本机 DevEco → 可建立真实构建 job（T1/T2 进 CI）；
  2. 在该 runner 上跑 conformance 产出 `harmony.json` → T3 才能进 CI；
  3. 真机/模拟器（DevEco Previewer 不算 device）→ T5/T6。
- 结论：**前置是自托管 runner + DevEco**（外部环境），代码侧已经具备可执行的部分。

---

## 3. 容易误读的三处（写报告时必须避免）

1. **`conformance/reports/SUMMARY.json` 不是真相源。**
   `conformance/reports/` 被 `.gitignore` 排除，仓库里不存在该文件；CI 每次运行时生成，
   `android.json` 由 `android-core` job 上传、`canonical` job 下载后比对。
   已入库的只有 iOS 的一份：`artifacts/runtime-evidence/2026-11-12-ios-conformance/ios.json`。
2. **`ci.yml` 顶部的诚实范围声明里，iOS 那两条已经过时。**
   原文写 `IOS_CI = NOT_ESTABLISHED`（理由是"N4 尚未开工"），而现实是
   `ios.yml` + `ios-runtime-visual.yml` 都在 `macos-15` 上跑，且 XCUITest 已 8/8。
   本轮已就地更正，避免后人引用到旧结论。
3. **`IOS_VISUAL` / `IOS_VNEXT_DEMO` 这类旧标签不代表运行时。**
   `evidence-index.json` 的 `labels` 里写着 `MACOS_RENDER — not simulator, not device`；
   运行时读数只看 `IOS_XCUITEST` 与 `ios-xcuitest-screenshots`。

---

## 4. 下一步（按"能否由代码解决"排序）

| #   | 动作                                                       | 级别提升            | 阻塞性质                    |
| --- | ---------------------------------------------------------- | ------------------- | --------------------------- |
| 1   | 自托管 Windows runner + DevEco，建立 Harmony 构建/单测 job | Harmony T1/T2 进 CI | 外部环境（runner + 工具链） |
| 2   | 在 1 之上产出 `harmony.json`                               | Harmony T3          | 依赖 1                      |
| 3   | Android emulator 进 CI（macOS arm64 或 KVM runner）        | Android T5 进 CI    | 代码/runner 选择            |
| 4   | Apple 签名 + 真机                                          | iOS T6              | 外部 blocker                |
| 5   | owner 判定 Android / iOS 人眼验收                          | 人眼验收            | 只归 owner                  |

**不做的事**：不为填格子把本机结论写成 CI 结论，不把模拟器写成真机，
不因为 Harmony 没有 CI 就假装它"已验证"。
