# BLOCKERS.md

> 只记录无法由代码本身解决、确实需要用户/外部环境介入的事项。
> 普通编译错误、测试错误、依赖冲突不属于 Blocker。
>
> **2026-09-14 FINAL PRODUCTION CLOSURE V1 修正**：B1 / B2 的原有前提经本轮实测**与事实不符**，
> 已按实测重写（详见 `FINAL_CLOSURE_PRE_AUDIT.md` §4/§5 与 `FINAL_PLATFORM_MATRIX.md`）。

## Active blockers — 阻断构建/真机

| #   | Blocker                                                            | 影响                                                                                 | 解除动作                                     |
| --- | ------------------------------------------------------------------ | ------------------------------------------------------------------------------------ | -------------------------------------------- |
| B1  | **Android 真实构建不可执行**（工具链大部分已具备）                 | 无法出 APK/AAB；Kotlin golden 互操作测试无法运行                                     | 见下方「B1 实测明细」                        |
| B2  | **HarmonyOS 真实构建不可执行**（工具链已具备且 hvigor 已真实运行） | ArkTS 适配器无法编译验证                                                             | 见下方「B2 实测明细」                        |
| B3  | 无 macOS / Xcode（本机 Windows）                                   | iOS 编译/签名/真机验证不可行（预期内）                                               | 在 macOS 环境执行 `swift test` 与 Xcode 工程 |
| B10 | **无 HBuilderX / uni-app x 编译工具链**（全盘搜索无命中）          | `.uvue`/UTS 无法编译、无法出 Android 基座；UI 从未被编译器验证 —— **产品级关键阻塞** | 安装 HBuilderX → 导入 `app/` → 自定义基座    |

以上阻塞编译/真机验证，**不阻塞**其他可执行工作（Core 已全绿，UI 静态 Gate 已全绿）。

### B1 实测明细（Android）

**已具备（旧记录称「无」）**：

- JDK **17.0.12**（`<DEVECO_HOME>\jbr\bin\javac.exe`）
- JDK **21.0.10**（`<ANDROID_STUDIO_HOME>\jbr`）
- Android SDK `<ANDROID_SDK_ROOT>`：platforms `android-36.1` / `android-37.0`；build-tools `36.1.0` / `37.0.0`；cmdline-tools `latest`；**licenses 全部已接受**
- Android Studio `AI-253.32098.37.2534.15232325`
- `adb.exe` 可用

**实际阻塞**：

1. **无可用 Gradle 发行版** —— `~/.gradle/wrapper/dists/gradle-9.3.1-bin/` 仅 0 字节 `.part`/`.lck`；Android Studio 内无完整发行版；工程 `platforms/android/` 内无 `gradlew` / `gradle-wrapper.properties`
2. **构建依赖 CDN 不可达** —— `curl https://services.gradle.org/distributions/gradle-8.9-bin.zip` → exit 7；`https://downloads.gradle.org/...` → exit 7（沙箱内）。AGP 8.5.2 / Kotlin 2.0.0 / androidx / SQLCipher / BouncyCastle 均需联网解析
3. `platforms/android/core/build.gradle.kts` 声明 `compileSdk = 34`，已装 platform 为 36.1 / 37.0（34 未安装）
4. 无真机（`adb devices` 空）
5. 无 release keystore

**解除动作**：获取可用 Gradle 发行版（或在工程内新增 gradle wrapper）→ 允许 `google()` / `mavenCentral()` 制品下载 → `compileSdk` 对齐已装 platform（**改动前需评审**）→ 连接真机/emulator → 提供 keystore 与正式 applicationId。

### B2 实测明细（HarmonyOS）

**已具备（旧记录称「无」）**：

- DevEco Studio **5.0.5.310**（`<DEVECO_HOME>`）
- HarmonyOS / OpenHarmony SDK：`sdk/default/{openharmony,hms}`，`apiVersion 13`、`version 5.0.1.115`、`metaVersion 3.0.0`
- hvigor **5.13.2** + `@ohos/hvigor-ohos-plugin 5.13.2`
- ohpm **5.0.10**（实测可执行）；registry `https://ohpm.openharmony.cn/ohpm/`
- 工程模板齐备

**已真实执行**：构造最小 Stage 工程后调用
`node ".../tools/hvigor/bin/hvigorw.js" assembleHap --mode module -p product=default -p buildMode=debug --no-daemon`
→ hvigor **成功引导 pnpm（`Pnpm install success.`）并解析工程模型**，最终报：

```
hvigor ERROR: Unable to find the following components:
        toolchains:13 / ArkTS:13 / js:13 / native:13 / previewer:13
Solution: 1.Go to File > Settings > OpenHarmony SDK, download the components, and sync the project.
```

**实际阻塞**：

1. SDK 组件（API 13）未被 hvigor 本地组件加载器识别 → 需 DevEco **SDK Manager 图形化同步**（人工步骤）
2. `platforms/harmonyos/` 是**适配器片段**（仅 `app.json5` + 1 个 `.ets` + `module.json5`），非可构建 Stage 工程
3. **产品级 HarmonyOS 产物由 HBuilderX（uni-app x）产出，非 DevEco 直接产出** → 仍受 B10 约束
4. 无 HarmonyOS 签名材料与正式 bundleName

**解除动作**：DevEco 中执行 SDK Manager 同步 → （如需独立验证适配器）补全可构建 Stage 工程 → 解除 B10 以获得产品级产物 → 提供签名材料与正式 bundleName。

## Active blockers — 阻断产品完整可用（需工程投入，非用户可解）

| #   | Blocker                                                                                                      | 影响                                                                                              | 解除动作                                                                                                                  |
| --- | ------------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------- |
| B20 | App 端「账单解析」桥接缺失：`core/src/sources/**`、`core/src/parser/**` 为 Node 侧纯 TS，未编译进 UTS/原生层 | **导入账单在设备上不可用**；`pages/import/*` 已如实标注「暂未接入」，不写入任何数据               | 以 UTS 重写/桥接解析与指纹层，或改为原生插件；需 B10 提供编译验证环境                                                     |
| B21 | App 端「`.depmap` 加解密」桥接缺失：`core/src/crypto/**`（Argon2id + AES-256-GCM）未编译进 UTS/原生层        | **加密备份导出/恢复在设备上不可用**；`pages/backup/*` 已如实标注                                  | 同 B20；Argon2id 需原生实现（Kotlin/Swift/ArkTS），并跑通 Golden Test Vector 互操作                                       |
| B22 | 完整 Impact Kernel / Rebase / Drift 检测 / Proposal 生成在设备端不可运行（App 侧为语义镜像）                 | 影响分析、计划 Rebase、Drift、Proposal 在设备上以「App 侧等价实现」运行，与 Core 存在双份真相风险 | 终局为 Core→UTS 代码生成；当前以「单一边界 + 语义镜像 + Core 冻结测试」降低风险，见 `docs/FRONTEND_ARCHITECTURE_AUDIT.md` |

> 说明：B20/B21/B22 **不是用户可解的外部阻塞**，而是工程缺口。之所以登记在此，
> 是因为其解除依赖 B10（工具链）——在没有编译器的前提下继续堆叠未经验证的原生桥接代码，
> 违反本 GOAL「不为上线加入大量未经验证的新功能」。
> 本轮采取的处置：**如实告知 + 提供可用的替代路径（手动声明支付关系）**，而非伪造功能。

## 产品可用性现状（诚实口径）

- **可用**：手动建立对象 → 手动声明支付关系 → 影响模拟 → 创建变更计划 → 执行/验证 → 时间轴 → 数据清空。
- **不可用（已标注）**：账单导入、加密备份导出/恢复。
- 因此当前构建**可以真实安装并真实使用**，但覆盖范围小于 MVP01 完整设计。

## 可能后续需要用户提供

（编号与 `STORE_EXTERNAL_BLOCKERS.md` 保持一致）

- [ ] Apple Developer Account（B8）
- [ ] iOS signing / provisioning（B9）
- [ ] Google Play Developer Account（B5）
- [ ] Android release keystore（B4；生成后放 `signing/`，已 gitignore）
- [ ] Huawei Developer / AppGallery Connect 身份（B6）
- [ ] HarmonyOS release signing（B7）
- [ ] 最终 Android applicationId / iOS bundle id / HarmonyOS bundleName（B11，现占位 `com.example.depmap`）
- [ ] 隐私政策 URL（B12；草稿见 `docs/PRIVACY_POLICY_DRAFT.md`）
- [ ] 正式支持 URL（B12b）
- [ ] 正式产品名 / 品牌名（B14；现为工作名「个人数字依赖图」）
- [ ] 应用图标正式资产（B15；规格见 `docs/APP_ICON_ASSET_SPEC.md`）
- [ ] 启动图 / Splash 正式资产（B16）
- [ ] 商店截图（B17；方案见 `store/SCREENSHOT_PLAN.md`）
- [ ] 真实微信账单（B13；仅 REAL_DATA Gate 时，放 `local_private/`）
- [ ] 真实 Android / HarmonyOS 设备用于真机验证（B18）
- [ ] 是否要求真实数据验证的决策（B19）

## 已解除

（无）

## 禁止记录

不要在这里写：

- 密码
- API key
- keystore 密码
- signing secret
- 真实账单内容
- 银行卡号
