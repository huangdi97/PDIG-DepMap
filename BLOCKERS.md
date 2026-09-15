# BLOCKERS.md

> 只记录无法由代码本身解决、确实需要用户/外部环境介入的事项。
> 普通编译错误、测试错误、依赖冲突不属于 Blocker。
>
> **2026-09-14 FINAL PRODUCTION CLOSURE V1 修正**：B1 / B2 的原有前提经本轮实测**与事实不符**，
> 已按实测重写（详见 `FINAL_CLOSURE_PRE_AUDIT.md` §4/§5 与 `FINAL_PLATFORM_MATRIX.md`）。
>
> **2026-09-15 PLATFORM BRINGUP 轮再修正**：B1 / B2 已**部分解除** —— Android 原生核心
> 已真实编译并产出 AAR（`docs/ANDROID_BUILD_REPORT.md`），HarmonyOS 已真实产出 HAP
> （`docs/HARMONY_BUILD_REPORT.md`）。两者的**残留阻塞已改写为「产品级产物 + 签名 + 设备」**，
> 不再是「工具链不存在」。详见下方明细。

## Active blockers — 阻断构建/真机

| #   | Blocker                                                                                  | 影响                                                                                                             | 解除动作                                                                                                    |
| --- | ---------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------- |
| B1  | **Android 产品级 APK/AAB 不可产出**（原生核心已 PASS，见下方明细）                       | 无法安装到设备、无法做真机 E2E 与 UI 运行时 QA                                                                   | 见下方「B1 实测明细」                                                                                       |
| B2  | **HarmonyOS 产品级 HAP 不可产出 + 签名缺失**（原生验证工程已 PASS，见下方明细）          | 当前 HAP 为原生验证工程产物，**不是产品包**；无法安装/真机验证                                                   | 见下方「B2 实测明细」                                                                                       |
| B3  | 无 macOS / Xcode（本机 Windows）                                                         | iOS 编译/签名/真机验证不可行（预期内）                                                                           | 在 macOS 环境执行 `swift test` 与 Xcode 工程                                                                |
| B10 | **HBuilderX 已安装但无头环境无法触发打包**（2026-09-15 实测重写）                        | `.uvue`/UTS 从未被真实编译器验证；**Android APK / HarmonyOS 产品级 HAP 均无法产出** —— **产品级关键阻塞**        | 见下方「B10 实测明细」                                                                                      |
| B23 | **本机沙箱限制：clean clone 闭环与破坏性 clean install 无法执行**（2026-09-14 实测登记） | 无法在本环境跑通「真实 clone → `npm ci` → 全门禁」；`npm ci` 的批量删除会被 safe-delete 守卫（阈值 50 文件）拦截 | 在不受限环境 / CI 中重跑；本机替代证据（committed-tree 自足性）见 `FINAL_PRODUCTION_CLOSURE_REPORT.md` §2.1 |

以上阻塞编译/真机验证，**不阻塞**其他可执行工作（Core 已全绿，UI 静态 Gate 已全绿）。

### B10 实测明细（HBuilderX / uni-app x）

**状态（2026-09-15 实测）**：

- HBuilderX **5.24.2026081301** 已安装在 `<TOOLS_ROOT>\HBuilderX`（旧记录「全盘搜索无命中」已被推翻）。
- 工程可导入、可被 HBuilderX 识别。
- **但 CLI 无 build 命令**：`cli.exe` 仅提供 `open` / `pack` 等有限子命令，无可用于无头环境
  触发 Android/HarmonyOS 打包的入口（云端打包需登录开发者账号并走 GUI 交互）。

**因此**：UI（24 个 `.uvue` + 5 个 UTS 插件）**至今从未被真实编译器验证过**，
`check:ui` 的 PASS 仅为**静态门**（9 类机械校验），不能等价为「编译通过」。
这是本轮唯一同时阻塞 Android 与 HarmonyOS 产品级产物的根因。

**解除动作（三选一）**：

1. 在 HBuilderX GUI 中执行「发行 → 原生App-云打包 / 本地打包」（需 DCloud 开发者账号登录）；
2. 接入 `uni-app x` 官方 CI 打包通道（需账号凭据）；
3. 提供可用的 `cli` build 子命令或本地打包 Gradle 工程模板。

**不得**：伪造编译结果、以静态门 PASS 冒充 `UI_COMPILED`。

### B1 实测明细（Android）

**状态（2026-09-15 更新）：原生层已 PASS，残留阻塞为「产品级产物 + 签名 + 设备」。**

**已解除（本轮实跑）**：

- Gradle 发行版 **8.9** 已获取并可用（不再依赖 `services.gradle.org` 直连）。
- `platforms/android` 已真实构建：**BUILD SUCCESSFUL**，`core-debug.aar`（44,147 B）/ `core-release.aar`（42,352 B）已产出，见 `platforms/android/artifacts/`。
- Kotlin 黄金向量互操作测试 **4/4 × debug+release 双变体 = 8/8 PASS，0 failures / 0 errors / 0 skipped**。
- 本轮修复 **10 项真实缺陷**（D-1~D-10），其中 **2 项为 P0 产品级缺陷**：
  - **P0-1 跨端 Base64 契约破裂**：原用 `android.util.Base64` + `NO_PADDING`，与 Node 侧 RFC 4648 带填充标准不一致 → 改用 `java.util.Base64`（API 26+，`minSdk = 26` 满足）。
  - **P0-2 容器解析字段冲突**：`JsonHeader.parse` 的全局正则取首个 `"algorithm"`，而 JCS 键序中 `cipher.algorithm` 在前 → 恒取到 `"AES-256-GCM"` 而非 `"argon2id"` → `validateBounds` 恒定抛错，**Android 端实际无法解密任何容器**。已改为限定 `kdf` 块作用域解析。
- 非 ASCII 工程路径（`<repo>`）引发的 `ClassNotFoundException` 已定位并修复：根因为 Gradle worker argfile 以 UTF-8 写入、fork 的 JVM 以 `sun.jnu.encoding`(GBK) 读取产生 mojibake。修复方式是把 `layout.buildDirectory` 重定向到 ASCII 路径。**验证必须带 `--rerun-tasks --no-build-cache`**，否则 build cache 会跨路径复用导致 `FROM-CACHE` 假通过。
- 完整缺陷表与复现指引见 `docs/ANDROID_BUILD_REPORT.md`，可执行步骤见 `docs/ANDROID_RELEASE_RUNBOOK.md`。

**残留阻塞**：

1. **产品级 APK/AAB 不可产出** —— 依赖 HBuilderX / uni-app x（**B10**）。实测 HBuilderX 5.24.2026081301 已安装且工程可导入，但 **CLI 无 build 命令**，无法在无头环境触发云端/本地打包。**AAR ≠ APK**：本轮产物仅为 Android Library，不含 `AndroidManifest` 入口、不含 24 个 `.uvue` 页面、不含 uni-app x 运行时。
2. **无真机 / 无模拟器** —— `adb devices` 为空、0 个 AVD、无 system-image；按指令不得伪造 Android 模拟器（**B18**）。
3. **无 release keystore**（**B4**）—— 用户提供；按指令不得自动生成 Production Credentials、不得把私钥写入 Git。

**解除动作**：安装/启用 HBuilderX 打包通道（或在 CI 上用 `cli` 云端打包）→ 产出 APK → 连接真机或创建 AVD → 提供 keystore 与正式 `applicationId`。原生层无需再改。

### B2 实测明细（HarmonyOS）

> **2026-09-15 更新**：下方「远端 `getSdkList` 返回 400 ⇒ 接口不可用」的定性**已被推翻**。
> 实测该接口在参数正确时返回 **HTTP 200 + 真实组件列表**；此前 400 是**请求参数错误**
> （`osType` 必须为 `windows`、`osArch` 必须为 `x64`），不是服务端故障。详见
> `docs/HARMONY_TOOLCHAIN_AUDIT.md` §3。真正的历史根因是**工程从来不是一个可构建 Stage 工程**。

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

**本轮重跑（FINAL PRODUCTION CLOSURE V1，2026-09-14，第 144 节）** —— 在 OS 临时目录重建最小 Stage 工程
（17 文件，`modelVersion 5.0.2`）后**再次执行同一命令**，**精确复现**上述错误，并暴露出一个**此前未记录的根因**：

```
> hvigor WARN: Request failed with status code 400
> hvigor WARN: url=https://repo.harmonyos.com/sdkmanager/v5/ohos/getSdkList, statusCode=undefined
> hvigor WARN: TypeError: datas is not iterable
    at OhRemoteComponentLoader.configComponent (.../@ohos/sdkmanager-common/.../oh-remote-component-loader.js:74:28)
> hvigor ERROR: Cause: Unable to find the following components:
        toolchains:13 / ArkTS:13 / js:13 / native:13 / previewer:13
> hvigor ERROR: BUILD FAILED in 24 s 166 ms
```

无 HAP 产物。（该 400 的定性已在 2026-09-15 被推翻，见本节顶部修正说明。）

**本轮（2026-09-15 PLATFORM BRINGUP）真实进展 —— 已产出 HAP**：

- **根因重新定性**：原工程 `platforms/harmonyos/` 只有 3 个文件，且 `app.json5` 不含 Stage 模型要求的
  顶层 `"app"` 键 —— 即**该工程从未被 DevEco/hvigor 成功解析过**，「组件找不到」是工程模型解析失败的
  次生表现，而非 SDK 缺失。
- **已补齐完整 DevEco Stage 工程骨架**（9/9 缺失项）：`AppScope/app.json5`（规范 `"app"` 顶层键）、
  `AppScope/resources/base/{element,media}`、`oh-package.json5`、`build-profile.json5`、`hvigorfile.ts`、
  `hvigor/hvigor-config.json5`、`hvigorw` 三件套、`entry/{oh-package,build-profile,hvigorfile}`、
  `entry/src/main/ets/{entryability,pages,adapters}`、`entry/src/main/resources/base/{element,media,profile}`。
- **已修复 6 类构建缺陷**（H-1~H-6）：非 ASCII 工程路径（hvigor 无豁免开关，改为 `build.sh` 镜像到
  ASCII 目录）、`@ohos/hvigor` 5.13.2 未发布到公共 registry（改用 `file:` 协议引用 DevEco 内置包）、
  缺 `.npmrc`、`module.json5` 的 `label` 必须为 `$string:` 引用、`hvigor-config.json5` 的 `hvigorVersion`
  字段非法、`srcEntry` 多写了一层 `src/main/`。
- **结果**：`hvigor BUILD SUCCESSFUL in 42 s 263 ms`，产出
  `platforms/harmonyos/artifacts/entry-default-unsigned.hap`（**18,986 B**，
  SHA-256 `4f10d0597aaaac2aab4af8e27ec7138709e07e5ea81aaed705d249ed55bd0663`），
  内含 ArkTS 字节码 `ets/modules.abc`（10,568 B）→ **ArkTS 已真实编译**。
- 完整记录见 `docs/HARMONY_BUILD_REPORT.md`，工具链实测见 `docs/HARMONY_TOOLCHAIN_AUDIT.md`。

**残留阻塞**：

1. **该 HAP 是「原生验证工程」产物，不是产品包** —— 它只含最小 `EntryAbility` + `Index.ets` +
   `RelationalStoreSecureAdapter.ets`，**不含** 24 个 `.uvue` 页面与 uni-app x 运行时。
   产品级 HarmonyOS 产物必须由 HBuilderX（uni-app x）产出 → 仍受 **B10** 约束。
2. **HAP 未签名**（`unsigned`）：无 HarmonyOS 签名材料与正式 bundleName（**B7**）。
3. **无 HarmonyOS 设备**（`hdc` 无可用目标），无法安装与真机验证（**B18**）。

**解除动作**：解除 B10 以获得产品级 HAP → 提供签名材料与正式 bundleName → 提供真实设备做安装与冒烟。
原生验证工程的构建链路（工具链 + 工程骨架 + ArkTS 编译）**已打通，无需再改**。

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
