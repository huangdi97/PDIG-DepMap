# BLOCKERS.md

> 只记录无法由代码本身解决、确实需要用户/外部环境介入的事项。
> 普通编译错误、测试错误、依赖冲突不属于 Blocker。

## Active blockers — 阻断构建/真机

| # | Blocker | 影响 | 解除动作 |
|---|---|---|---|
| B1 | 无 JDK17+ / Android SDK / Gradle（本机仅 JRE 1.8 + adb.exe） | Android Kotlin 核心与 golden 互操作测试无法运行；无法出 APK | 安装 JDK17 + Android cmdline-tools + platform/build-tools |
| B2 | 无 DevEco Studio / HarmonyOS SDK | ArkTS 适配与工程无法编译验证 | 安装 DevEco Studio |
| B3 | 无 macOS / Xcode（本机 Windows） | iOS 编译/签名/真机验证不可行（预期内） | 在 macOS 环境执行 `swift test` 与 Xcode 工程 |
| B10 | 无 HBuilderX / uni-app x 编译工具链 | `.uvue`/UTS 无法编译、无法出 Android 基座；UI 从未被编译器验证 | 安装 HBuilderX → 导入 `app/` → 自定义基座 |

以上阻塞编译/真机验证，**不阻塞**其他可执行工作（Core 已全绿，UI 静态 Gate 已全绿）。

## Active blockers — 阻断产品完整可用（需工程投入，非用户可解）

| # | Blocker | 影响 | 解除动作 |
|---|---|---|---|
| B20 | App 端「账单解析」桥接缺失：`core/src/sources/**`、`core/src/parser/**` 为 Node 侧纯 TS，未编译进 UTS/原生层 | **导入账单在设备上不可用**；`pages/import/*` 已如实标注「暂未接入」，不写入任何数据 | 以 UTS 重写/桥接解析与指纹层，或改为原生插件；需 B10 提供编译验证环境 |
| B21 | App 端「`.depmap` 加解密」桥接缺失：`core/src/crypto/**`（Argon2id + AES-256-GCM）未编译进 UTS/原生层 | **加密备份导出/恢复在设备上不可用**；`pages/backup/*` 已如实标注 | 同 B20；Argon2id 需原生实现（Kotlin/Swift/ArkTS），并跑通 Golden Test Vector 互操作 |
| B22 | 完整 Impact Kernel / Rebase / Drift 检测 / Proposal 生成在设备端不可运行（App 侧为语义镜像） | 影响分析、计划 Rebase、Drift、Proposal 在设备上以「App 侧等价实现」运行，与 Core 存在双份真相风险 | 终局为 Core→UTS 代码生成；当前以「单一边界 + 语义镜像 + Core 冻结测试」降低风险，见 `docs/FRONTEND_ARCHITECTURE_AUDIT.md` |

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
