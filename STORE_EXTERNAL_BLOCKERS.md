# STORE_EXTERNAL_BLOCKERS.md — PDIG 外部 Blocker 与解除手册

> 只记录**无法由代码解决**、必须用户/外部环境介入的事项。
> 每项给出：谁来提供 / 提供后执行什么 / 影响哪个 Gate。

---

## 一、编译与工具链（阻断平台构建）

| #       | Blocker          | 现状（实测）                                                                | 谁来提供     | 提供后执行                                                                                                                                                                                                           | 影响 Gate                                            |
| ------- | ---------------- | --------------------------------------------------------------------------- | ------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------- |
| **B1**  | Android 工具链   | `java 1.8.0_441`（需 17+）；无 Android SDK / cmdline-tools / Gradle wrapper | 用户安装     | 按 `docs/ANDROID_TOOLCHAIN_SETUP.md`：装 JDK17 → cmdline-tools → `sdkmanager "platforms;android-35" "build-tools;35.0.0"` → `cd platforms/android && gradle :core:test`（含 Golden 互操作）→ `assembleDebug/Release` | ANDROID_BUILD_READY / DEVICE_VERIFIED / STORE_READY  |
| **B2**  | HarmonyOS 工具链 | 无 DevEco Studio / HarmonyOS SDK / hvigor                                   | 用户安装     | 按 `docs/HARMONY_TOOLCHAIN_SETUP.md`：装 DevEco → 导入 `platforms/harmonyos` → `hvigorw assembleHap`                                                                                                                 | HARMONY_BUILD_READY / DEVICE_VERIFIED / STORE_READY  |
| **B3**  | macOS / Xcode    | 本机 win32，无 Xcode                                                        | 用户提供 Mac | 按 `docs/IOS_RELEASE_HANDOFF.md`：`swift package resolve` → `swift test` → `xcodebuild archive` → 真机 → TestFlight                                                                                                  | IOS_BUILD_READY / DEVICE_VERIFIED / TESTFLIGHT_READY |
| **B10** | uni-app x 工具链 | 无 HBuilderX                                                                | 用户安装     | 装 HBuilderX → 导入 `app/` → 运行到手机基座 / 自定义基座 → 编译 Android 基座                                                                                                                                         | UI_BUILD_READY / UI 真机验收 / Visual Regression     |

---

## 二、账号与签名（阻断商店提交）

| #      | Blocker                                    | 谁来提供                    | 提供后执行                                                                       | 影响 Gate                                      |
| ------ | ------------------------------------------ | --------------------------- | -------------------------------------------------------------------------------- | ---------------------------------------------- |
| **B4** | Android release keystore                   | 用户生成                    | `keytool -genkeypair` → 放 `signing/`（已 gitignore）→ 配置 Gradle signingConfig | ANDROID_SIGNING_READY / STORE_SUBMISSION_READY |
| **B5** | Google Play Developer Account              | 用户注册（付费）            | 建应用 → 上传 AAB → 填 listing / 隐私问卷                                        | ANDROID_STORE_READY / STORE_SUBMITTED          |
| **B6** | Huawei Developer / AppGallery Connect 身份 | 用户注册                    | 建应用 → 配置签名 → 上传 App Pack                                                | HARMONY_STORE_READY / STORE_SUBMITTED          |
| **B7** | HarmonyOS release signing                  | 用户在 AGC 生成             | 下载证书/Profile → 配置 DevEco signing                                           | HARMONY_SIGNING_READY                          |
| **B8** | Apple Developer Account                    | 用户注册（付费）            | 建 App ID → 证书/Profile                                                         | IOS_SIGNING_READY                              |
| **B9** | iOS signing / provisioning                 | 用户在 Apple Developer 生成 | 配置 Xcode 签名 → archive → TestFlight                                           | IOS_TESTFLIGHT_READY / APPSTORE_READY          |

---

## 三、身份与元数据

| #        | Blocker           | 现状                                                   | 谁来提供                                         | 影响 Gate                                                  |
| -------- | ----------------- | ------------------------------------------------------ | ------------------------------------------------ | ---------------------------------------------------------- |
| **B11**  | 最终应用标识      | 现为占位 `com.example.depmap`                          | 用户决策                                         | 所有 STORE_* 与 SIGNING（标识变更会导致签名/商店身份漂移） |
| **B12**  | 隐私政策 URL      | 无正式站点（草案已备：`docs/PRIVACY_POLICY_DRAFT.md`） | 用户部署                                         | STORE_SUBMISSION_READY                                     |
| **B12b** | 支持 URL          | 无正式站点                                             | 用户部署                                         | STORE_SUBMISSION_READY                                     |
| **B14**  | 正式产品名 / 品牌 | 当前工程名 PDIG；中文工作名"个人数字依赖图"            | 用户决策（**BRAND_NAME = NEEDS_USER_DECISION**） | STORE_METADATA_READY / icon / listing                      |

---

## 四、资产

| #       | Blocker           | 现状                                                 | 谁来提供            | 影响 Gate          |
| ------- | ----------------- | ---------------------------------------------------- | ------------------- | ------------------ |
| **B15** | App Icon（三端）  | 无正式 asset（规格见 `docs/APP_ICON_ASSET_SPEC.md`） | 设计/用户           | STORE_ASSETS_READY |
| **B16** | Splash / 启动图   | 无                                                   | 设计/用户           | STORE_ASSETS_READY |
| **B17** | Store Screenshots | 无法生成（无运行环境 B10/B1）                        | 依赖 B10 或 B1 解除 | STORE_ASSETS_READY |

---

## 五、工程缺口（**非用户可解**，登记以便不遗忘）

> 这三项不是"用户提供即可解决"的外部事项，而是必须投入工程实现的缺口。
> 之所以列在这里，是因为它们的**解除依赖 B10（uni-app x 工具链）**：
> 在没有编译器的前提下继续堆叠未经验证的原生桥接代码，属于"为上线加入未经验证的功能"。

| #       | 缺口                                | 现状                                                                           | 影响                                                                    | 解除动作                                                                    |
| ------- | ----------------------------------- | ------------------------------------------------------------------------------ | ----------------------------------------------------------------------- | --------------------------------------------------------------------------- |
| **B20** | App 端账单解析桥接                  | `core/src/sources/**`、`core/src/parser/**` 为 Node 侧纯 TS，未进入 UTS/原生层 | **导入账单在设备上不可用**；`pages/import/*` 已如实标注，不写入任何数据 | 以 UTS 重写解析 + 指纹层（或原生插件）；在 B10 环境下编译并跑通合成样例回归 |
| **B21** | App 端 `.depmap` 加解密桥接         | `core/src/crypto/**`（Argon2id v19 + AES-256-GCM + JCS AAD）未进入 UTS/原生层  | **加密备份导出/恢复在设备上不可用**；`pages/backup/*` 已如实标注        | 三端原生实现 Argon2id 与 GCM，跑通 **Golden Test Vector** 跨实现互操作      |
| **B22** | 设备端 Impact/Rebase/Drift/Proposal | App 侧为 Core 语义的**运行时镜像**（`app/services/*.uts`）                     | 存在双份真相风险；Core 侧 453 测试不能直接覆盖 App 侧                   | 终局为 Core→UTS 代码生成；中期靠语义镜像 + 静态 Gate（U1–U9）约束           |

**本轮已采取的处置**：如实告知 + 提供**可用的替代路径**（手动声明支付关系），
使应用在不依赖导入的前提下仍然可以真实安装、真实使用。

---

## 六、数据与决策

| #       | Blocker                    | 现状                                 | 谁来提供                | 影响 Gate                                  |
| ------- | -------------------------- | ------------------------------------ | ----------------------- | ------------------------------------------ |
| **B13** | 真实微信账单               | 未提供；`validate-real-bill.ts` 就绪 | 用户放 `local_private/` | REAL_DATA_CORRECTNESS / REAL_DATA_VALUE    |
| **B18** | 真实物理设备               | 无 Android/HarmonyOS/iOS 设备接入    | 用户                    | DEVICE_VERIFIED 全平台                     |
| **B19** | 发布前是否需要真实数据验证 | 未决策                               | 用户确认                | STORE_SUBMISSION_READY 的 Release Decision |

---

## 七、解除后无需再次大规模开发

工具链与账号类 Blocker（B1–B12b、B14–B18）全部解除后，剩余动作仅为：

```
build → sign → device verify → upload → store review
```

**不需要**重新设计产品、重写 UI 或重做 Core。这是本轮"能上线"定义的验收点。

B20–B22 属于**功能完整性**缺口：不解除也可以构建、签名、上架（应用可正常使用，
只是导入与备份两条路径不可用且已如实标注）；解除后产品覆盖范围才回到 MVP01 完整设计。
