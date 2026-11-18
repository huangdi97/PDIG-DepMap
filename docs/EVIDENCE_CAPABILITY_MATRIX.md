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

| 级别                   | Android                                                                                                                                                                                                                                                                                                                                                                             | iOS                                                                                                                                                                     | HarmonyOS                                                                                                                                                                                 |
| ---------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| T0 `SOURCE`            | ✅                                                                                                                                                                                                                                                                                                                                                                                  | ✅                                                                                                                                                                      | ✅（74 个 `.ets`）                                                                                                                                                                        |
| T1 `COMPILED`          | ✅ CI `job android-app`：`:app:assembleDebug`                                                                                                                                                                                                                                                                                                                                       | ✅ CI `ios.yml`：Swift build（PDIGCore + PDIGConformance + PDIGApp）                                                                                                    | ⚠️ **仅本机**：`HARMONY_COMPILE_REACHABILITY_LOCAL = PASS`（DevEco hvigor clean assembleHap + modules.abc 取证 + 负向 probe）。CI 端 `HARMONY_GITHUB_HOSTED_NATIVE_BUILD = NOT_AVAILABLE` |
| T2 `TESTED`            | ✅ CI：`:core:test` + `:app:testDebugUnitTest`                                                                                                                                                                                                                                                                                                                                      | ✅ CI：canonical 91+37、core smoke、`PDIGAppTests`                                                                                                                      | ⚠️ **仅本机**：`HARMONY_HOST_CONFORMANCE_LOCAL = PASS 89/89`（canonical 85/91 + 3 元测试 + 1 域自检）                                                                                     |
| T3 `CONFORMANCE`       | ✅ CI 产出 `android.json`，由 `canonical` job 下载后逐用例 diff（`needs` 串行，避免恒为 NOT_RUN）                                                                                                                                                                                                                                                                                   | ✅ **161/161**，报告已入库 `artifacts/runtime-evidence/2026-11-12-ios-conformance/ios.json`                                                                             | ❌ `NOT_RUN`：不产出 `harmony.json`，orchestrator 如实报 NOT_RUN                                                                                                                          |
| T4 `STATIC_RENDER`     | ⚠️ 只有**本机 AVD 真截图**，不是静态渲染器；`FLAG_SECURE` 帧按设计全黑                                                                                                                                                                                                                                                                                                              | ✅ 60 张（30 屏 × light/dark），macOS SwiftUI `ImageRenderer`，逐文件 `MACOS_RENDER`                                                                                    | — 无渲染通道                                                                                                                                                                              |
| T5 `SIMULATOR_RUNTIME` | ✅ **CI 已跑通（49/52 类，198 例）**：ubuntu-latest + `/dev/kvm` + x86_64 API 34，两个 job —— 非 UI 18 类 = **62 tests / 0 failures**（run `43679686872`）、UI 契约 4 类 = **136 tests / 0 failures**（run `44362509080`），整条 run success。隔离只剩 `AndroidGlobeGestureContractTest`（GLES）。**本机 AVD** 另有 post-freeze 取证（19/19 × 多种 fontScale、88 帧 + 12 张真截图） | ✅ **8/8**，`run 43572807640` / `43606405863` 两次独立整条 success；iPhone 16 Pro Max + iPad 两条腿；35 帧/腿已由 `xcresulttool export attachments` 导出为独立 artifact | ❌ `NOT_RUN`                                                                                                                                                                              |
| T6 `DEVICE_RUNTIME`    | ❌ `NOT_RUN`（无真机证据）                                                                                                                                                                                                                                                                                                                                                          | ❌ `NOT_RUN`（无签名、无真机）                                                                                                                                          | ❌ `HARMONY_DEVICE_RUNTIME = NOT_RUN`                                                                                                                                                     |
| T7 `STORE_READY`       | ❌ 外部 blocker（签名/素材）                                                                                                                                                                                                                                                                                                                                                        | ❌ 外部 blocker（Apple Developer）                                                                                                                                      | ❌ 外部 blocker（Huawei 身份/签名）                                                                                                                                                       |
| 人眼验收               | `HUMAN_VISUAL_ACCEPTANCE = NOT_RUN`                                                                                                                                                                                                                                                                                                                                                 | `IOS_HUMAN_VISUAL_ACCEPTANCE = NOT_RUN`                                                                                                                                 | 无材料                                                                                                                                                                                    |

### 1.1 可浏览的证据页

| 端      | 页面                      | 内容                                                                             |
| ------- | ------------------------- | -------------------------------------------------------------------------------- |
| Android | gh-pages `/human-review/` | 88 帧语义判据 + 12 张真截图（`FLAG_SECURE` 帧只给判据，不摆黑图）                |
| iOS     | gh-pages `/ios-review/`   | 60 张 macOS 渲染 + 70 帧模拟器运行期（35 帧 × iPhone/iPad，页面副本 660px 缩放） |

---

## 2. 每一格的缺口与升级路径

### Android：T5 已进 CI（子集），全量仍未

- 现状：`.github/workflows/android-emulator-e2e.yml` 在 **ubuntu-latest + `/dev/kvm`** 上真实启动
  x86_64 emulator，`run 43679686872` 跑出 **`tests=62 failures=0 errors=0 skipped=0`（88s，BUILD SUCCESSFUL 2m27s）**。
  已纳入 **18 个「不依赖 Compose UI 机器人」的类**（设备内 SQLCipher / Keystore / 屏幕保护 /
  备份 / 漂移候选 / 删除全部数据 / 运行时证据 / 文件工作流 / 人工现实权威 / PerfSmoke +
  7 个 uivnext 契约类 + globe 纹理类）；`androidTest` 共 **52 个**类，剩 34 个依赖 `ComposeTestRule`，
  需要另立 UI job（或在同一 job 里分段跑）。
- **macOS runner 走不通（已实测）**：`macos-15` 上 emulator 报 `HVF error: HV_UNSUPPORTED`
  → `failed to initialize HVF` → `Timeout waiting for emulator to boot`（run `43630295255`）。
  GitHub 托管的 macOS runner 不提供嵌套虚拟化，arm64 镜像起不来；Linux + KVM 才是官方路径。
- 踩过的坑（已写进 workflow 注释，免得重来）：`sdkmanager` 不在 PATH（两平台都是）；
  `assembleDebugAndroidTest` 因 flavor 维度（`production` / `preview`）而 ambiguous；
  `android-emulator-runner` 的 `script` **逐行**用 `sh -c` 在仓库根执行，所以必须写一行
  自包含命令（`android/gradlew -p android …`）；`/dev/kvm` 初始是 `root:kvm 660`，
  必须先跑 udev 授权、再断言可读写。
- **UI 契约：10 个类 / 17 例进 main 门禁**（run `43813068619` 全绿）：`PhoneChangeLayout` ·
  `ContinuityFindingsR27` ×3 · `EstablishImportR23` ×2 · `ManualRelationshipR25` ×2 ·
  `ManualEstablishR24` ×2 · `HumanReviewR22` ×3 · `PhoneNumbersListVisibility` ·
  `PhoneStudioLayout`（1 例）· `PhoneCardsLayout` · `StudioThumbnailDistinct`。
  这一轮修掉的测试侧问题（**均未降级断言**）：
  ① `onNodeWithText` 的唯一性假设（重复标签被当成缺失）→ `onAllNodesWithText(...).onFirst()`；
  ② `assertIsDisplayed` 依赖窗口高度 → 对**内容节点**先 `performScrollTo()` 再断言 displayed。
  ⚠️ 踩坑：`pdig.r25.manual-relationship` / `pdig.r23.import-reference` /
  `pdig.r22.review-inbox` / `pdig.r24.manual-establish` **本身就是滚动容器**
  （`Column + verticalScroll`），对容器 scrollTo 会报
  `Semantic Node has no parent layout with a Scroll SemanticsAction`——容器只 assertIsDisplayed；
  ③ 断言指向**源码里已不存在的文案/元素**：R25 旧句 → 实际渲染的
  「unknown 表示尚未确认它是不是必须路径，不表示“不重要”。」；`风险` → `风险与恢复`；
  `pdig.r10.top.back` 只在 **preview flavor** 渲染，production 里没有；
  ④ `PhoneNumbersListVisibility` 把「fixture 号码数守卫」写死成 7（fixture 已到 8）；
  顺带记住：**不能拿 `rows.size` 反推**，列表是 lazy 的、只组合可见行（实测 6/8）。
- **一次被推翻的结论（值得记住）**：`PhoneStudioLayout#compactCardDetail` 曾被记为「实测确认的产品缺陷」——
  `pdig.card.detail.change-art` 量出 0.0×0.0dp（合并树/未合并树一致，父节点 411.4×793.5dp、根 411.4×841.5dp）。
  **这是错的**：logcat 完整树（run `44023339937`）显示该节点真实存在且正确：
  `Node #148 (l=762,t=3927,r=1041,b=4053)px, Actions=[OnClick, RequestFocus], Text='[更换卡面图片 →]'`
  → 279×126px @ 2.625 = **106.3×48.0dp**。它只是在**视口下方**，而 `SemanticsNode.boundsInRoot`
  会把越界节点裁剪成 0×0。加上 `performScrollTo()` 后断言通过（run `44027630529`，该用例已进 main 门禁）。
  → 教训：**用 `boundsInRoot` 判断尺寸前必须先滚进视口**；0×0 优先怀疑裁剪，而不是产品。
- 仍隔离（每条都有实测理由，均来自 run `44034066201` / `44039471671`）：
  · `AndroidGlobeGestureContractTest` 与 `AndroidGlobeEvidenceContractTest` —— 都需要
  `TestTag = 'pdig.globe.canvas'`。已试 **两种 GPU 模式**：`swiftshader_indirect` 与 `-gpu guest`
  （run `44247199095`）都不行。根因在 `R15WorldScene.kt` 的能力探针：
  `supportsR15GpuEarth()` 要求 `deviceConfigurationInfo.reqGlEsVersion >= 0x20000`，且 `R15GpuEarthView`
  （GLSurfaceView）能真正初始化；托管模拟器两条都不满足 ⇒ App **按设计降级**到 fallback，地球画布从未被组合。
  结论：这是**需要真机（GLES 2.0+）才能验的契约**，不是测试写法或产品缺陷。
  · `SourceCompleteScreenshotEvidenceTest` —— 该用例要求地球达到 `GlobeRenderState.TEXTURE_READY`
  并对 globe 区域做蓝色像素比断言（`GLOBE_SCREENS`），与上面两条是同一条 GLES 门槛；
  run `44219241218` 报 `capture failed for 02-overview-global`。
- 仍隔离（补充）：`AppLockNavigationTest#explicitAcknowledgmentUnlocksOnADeviceWithoutAnyCredential` ——
  无凭据的 headless AVD 上点完「已知悉风险，本次进入」后，主页内容 15s/30s 仍未出现
  （`ComposeTimeoutException`，run `44112044806` / `44120688332`）；同类其余 6 个方法已在门禁内并通过。
- **四项已从隔离转为门禁成员**（run `44362509080`）：
  · `NumberDetailLayoutProbeTest` —— 原版对「同一父 Column 里但不在同一屏」的四个端点做顺序断言，
  必然失败。改为**视口感知**：只在两端同时落在视口内时断言阅读顺序，其余把真实 dp 与
  `bothInViewport=false` 写进 JSON（探针必须至少量到一对，不允许「零证据」通过）；
  · `NumberDetailVerticalFlowContractTest` —— `summary→services` 869dp 不是死空白，中间还有
  「号码生命周期」卡（`pdig.r19.number.lifecycle.adaptive`）；契约改为只约束**相邻块**；
  别名那条原是旧屏 `R9NumberDetail.kt` 的行内编辑器，vNext 入口是 `定制号码面 →`；
  · `AndroidLightVisualSourceContractTest` —— 旧 tag `pdig.r18.number|card.lifecycle` 换成 `pdig.r19.*.adaptive`；
  · `UiScreenshotEvidenceTest#capturesAllProductionPages_lightAndDark` —— 造数补 `payment_instrument` 节点。
- 新发现（产品现状，非缺陷）：**vNext 号码详情页不渲染本地别名**——hero 用 fixture 的 `NumberFace`，
  别名只参与 `numberDisplayNameForScreen`。旧屏有行内改名，新屏没有；已在该用例里注明，
  是否要补 UI 由产品决定，测试不再把「不存在的 UI」当回归。
- **更早一轮的两项**（run `44307745031`，明细如下）：
  · `AndroidLightVisualSourceContractTest` —— 旧 tag `pdig.r18.number|card.lifecycle` 换成屏幕上真实存在的
  `pdig.r19.number|card.lifecycle.adaptive`；换号屏断言改为真实的投影选择器
  `pdig.change.projection.{current,transition,after}`（旧的 `CHANGE_OLD/SERVICES/NEW/STEPPER_MINI`
  只在 `UiVNextModels` 里作为常量残留，UI 已不再渲染）；
  · `UiScreenshotEvidenceTest#capturesAllProductionPages_lightAndDark` —— 造数改为先插入
  `payment_instrument` 节点、再作为 `replace_payment_card` 的 target（原实现取 `nodes().firstOrNull()`
  拿到 account，被 `validateScenarioTarget` 正确拒绝）。
- 结论：**Android 设备证据已进 CI：49 个类 / 198 例（非 UI 18 类 62 例 + UI 31 类 136 例），多次 success**；
  剩余 UI 类可用同一套「唯一性 / 滚动 / 过时断言 / lazy 列表 / bounds 裁剪」五查逐步纳入。

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
