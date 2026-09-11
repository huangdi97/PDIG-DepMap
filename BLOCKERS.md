# BLOCKERS.md

> 只记录无法由代码本身解决、确实需要用户/外部环境介入的事项。
> 普通编译错误、测试错误、依赖冲突不属于 Blocker。

## Active blockers

| # | Blocker | 影响 | 解除动作 |
|---|---|---|---|
| B1 | 无 JDK17+ / Android SDK / Gradle（本机仅 JRE 1.8 + adb.exe） | Android Kotlin 核心与 golden 互操作测试无法运行 | 安装 JDK17 + Android cmdline-tools + platform/build-tools |
| B2 | 无 DevEco Studio / HarmonyOS SDK | ArkTS 适配与工程无法编译验证 | 安装 DevEco Studio |
| B3 | 无 macOS / Xcode（本机 Windows） | iOS 编译/签名/真机验证不可行（预期内） | 在 macOS 环境执行 `swift test` 与 Xcode 工程 |
| B10 | 无 HBuilderX / uni-app x 编译工具链 | `.uvue`/UTS 无法编译、无法出 Android 基座 | 安装 HBuilderX → 导入 `app/` → 自定义基座 |

以上阻塞编译/真机验证，**不阻塞**其他可执行工作（Core 已全绿）。

## 可能后续需要用户提供

- [ ] Apple Developer Account（B8）
- [ ] iOS signing / provisioning（B9）
- [ ] Google Play Developer Account（B5）
- [ ] Android release keystore（B4；生成后放 `signing/`，已 gitignore）
- [ ] Huawei Developer / AppGallery Connect 身份（B6）
- [ ] HarmonyOS release signing（B7）
- [ ] 最终 Android applicationId / iOS bundle id / HarmonyOS bundleName（B11，现占位 `com.example.depmap`）
- [ ] 隐私政策 URL（B12）
- [ ] 真实微信账单（B13；仅 REAL_DATA Gate 时，放 `local_private/`）

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
