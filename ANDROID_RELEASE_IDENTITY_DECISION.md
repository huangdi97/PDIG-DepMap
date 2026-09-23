# ANDROID_RELEASE_IDENTITY_DECISION.md
>
> 生成时间：2026-09-22（ANDROID_CANONICAL_FREEZE → Production/Reality Closure 轮）
> **更新：2026-09-23（ANDROID_2026_PRODUCTION_REALITY_CLOSURE · API36 工程全速收口轮）**
> —— TARGET_SDK / compileSdk 已由 `b13f2f7` 提升到 **36**（本文件 §4 同步更新）；
>    applicationId / 品牌 / 版本 / Signing / 渠道（R-1..R-5）仍 **OPEN**，本轮**不冻结**、
>    不创建 Play App、不上传 AAB（保持 BLOCKED_BY_USER_DECISION）。
> 状态：**DECISION PACKAGE READY / DECISIONS OPEN（需用户裁决）**
> 对应 Gate：`ANDROID_RELEASE_IDENTITY_READY = PARTIAL（包就绪，身份未定）`
>
> 本文件把发布身份各字段的 **当前值 / 建议值 / 理由 / 可否后改 / 改动代价 / 必须决定的时间点** 一次性列清。
> 原则（AGENTS §16 / §21）：**不替用户做不可逆决定**，尤其 applicationId 一旦上架即成为应用身份证。

---

## 0. 结论先行

| 项 | 当前值 | 建议值（供参考，不默认执行） | 可否后改 | 上架前必须决定 |
|----|--------|------------------------------|----------|----------------|
| APPLICATION_ID | `com.pdig.app` | 见 §1（需用户确认） | **❌ 上架后不可改** | ✅ |
| APP_DISPLAY_NAME_ZH | `个人数字依赖图`（工作名） | 见 §2 | 可改，但影响品牌一致性 | ✅（最终品牌） |
| APP_DISPLAY_NAME_EN | `PDIG`（内部代号） | 见 §2 | 可改，但影响品牌一致性 | ✅（最终品牌） |
| VERSION_NAME | `0.1.0-milestone` | 对外首发建议 `1.0.0` | 可改（每次发布可变化） | ✅（策略决定） |
| VERSION_CODE | `1` | `1`（首发起点，后续单调递增） | **❌ 已发布版本不可复用** | ✅（首次即定） |
| MIN_SDK | `26` | `26` | 可提高，不可降低 | 已确定，无需决策 |
| TARGET_SDK | `36` | `36`（随 Play 政策演进） | 可提高 | 已确定为 36（2026-09-23 提升） |
| PACKAGE/NAMESPACE POLICY | `namespace == applicationId == com.pdig.app` | 保持 namespace == applicationId | namespace 可改（未上架时） | 随 applicationId 一起定 |

---

## 1. APPLICATION_ID

| 维度 | 内容 |
|------|------|
| **CURRENT VALUE** | `com.pdig.app`（`android/app/build.gradle.kts` 中 `applicationId = "com.pdig.app"`） |
| **RECOMMENDED VALUE** | 用户决定。若持有自有域名 `example.com`，建议 `com.example.<product>` 或 `io.github.<owner>.<product>`；无域名时可用与 GitHub 组织一致的命名空间。**仅建议，绝不擅自改** |
| **WHY** | applicationId 是设备/商店视角的应用身份：决定更新安装路径、与签名证书共同构成"同一应用"判定。内部代号 `pdig` 作为对外 id 并不理想，但可接受 |
| **CAN_CHANGE_LATER?** | **否（上架后不可改）**。Play Console 创建应用后 applicationId 不可变更；改动 = 全新应用，旧用户无法收到更新 |
| **COST_OF_CHANGE** | 上架前：低（改一行 + 全量回归 + 重新签名）。上架后：**不可逆**，需全新上架并迁移用户 |
| **MUST_DECIDE_BEFORE** | **创建 Play Console 应用 / 第一次上传 AAB 之前**（Gate `ANDROID_STORE_SUBMISSION_READY` 的前置） |
| **旁证** | `AndroidManifest.xml` 中 `package` 属性与 build 的 `namespace` 均与 applicationId 一致；`res/values/strings.xml` 无包名引用；备份文件 `.depmap` 不包含包名，故改包名不影响既有备份兼容（§4 改动影响面） |

---

## 2. APP_DISPLAY_NAME（桌面图标名 / 商店名称）

| 维度 | 内容 |
|------|------|
| **CURRENT VALUE（ZH）** | `个人数字依赖图`（`app_name_full`，工作名） |
| **CURRENT VALUE（EN）** | `PDIG`（`app_name`，内部代号，同时是当前桌面短名） |
| **RECOMMENDED VALUE** | 最终品牌由用户决定。风格建议：短、可读、避开"基础设施/图谱"等高概念词（如 `数字依赖图`）；EN 名与 ZH 名语义一致（如 `Dependency Map` / `PDIG`）。**仅建议** |
| **WHY** | 商店列表名 ≤30 字符；桌面名影响首屏认知；与隐私政策/品牌素材必须一致 |
| **CAN_CHANGE_LATER?** | ✅ 可以（Play 允许改名，但频繁改名损害品牌信任） |
| **COST_OF_CHANGE** | 低（strings.xml + 商店列表 + 截图素材 + feature graphic 文案同步）；但最终品牌素材依赖此决策 |
| **MUST_DECIDE_BEFORE** | 生成最终品牌素材（launcher icon / splash / feature graphic / Play 截图）之前（Gate `ANDROID_STORE_ASSETS_READY`） |

---

## 3. VERSION_NAME / VERSION_CODE

| 项 | 维度 | 内容 |
|----|------|------|
| VERSION_NAME | **CURRENT** | `0.1.0-milestone` |
| VERSION_NAME | **RECOMMENDED** | 对外首发建议 `1.0.0`（或用户市场策略决定的其他正式版本名） |
| VERSION_NAME | **WHY** | `0.1.0-milestone` 是内部里程碑名；对商店用户，0.x 会传递"未完成"信号 |
| VERSION_NAME | **CAN_CHANGE_LATER?** | ✅ 每个发布版可变化（semver 语义化） |
| VERSION_NAME | **COST_OF_CHANGE** | 低（gradle + About 页文案；需从 BuildConfig 读取避免硬编码） |
| VERSION_NAME | **MUST_DECIDE_BEFORE** | 首次 AAB 上传前 |
| VERSION_CODE | **CURRENT / RECOMMENDED** | `1`（首发起点） |
| VERSION_CODE | **WHY** | Play 要求每次更新单调递增；从 1 开始是惯例 |
| VERSION_CODE | **CAN_CHANGE_LATER?** | **❌ 已上传的版本号不可复用**；未上传前可任意定起点 |
| VERSION_CODE | **COST_OF_CHANGE** | 上架前零成本；上架后仅能递增 |
| VERSION_CODE | **MUST_DECIDE_BEFORE** | 首次上传前（当前 `1` 已满足） |

---

## 4. MIN_SDK / TARGET_SDK

| 项 | CURRENT | RECOMMENDED | WHY | CAN_CHANGE_LATER? | COST_OF_CHANGE | MUST_DECIDE_BEFORE |
|----|---------|-------------|-----|-------------------|----------------|--------------------|
| MIN_SDK | `26`（Android 8.0） | `26`（**已确定**） | SQLCipher / AES-256-GCM / BiometricPrompt / FileProvider 的能力下限；无需用户决策 | 只能提高（放弃更老设备），不能降低 | 提高 = 排除老设备 + 重测 | 无需（已确定） |
| TARGET_SDK | `36` | `36`（当前合规线） | Android 16 目标（2026-09-23 由 `b13f2f7` 提升）；Play 政策要求 targetSdk 跟随 | ✅ 可提高 | 提高需回归重验 + 处理新行为变更 | 已确定（36） |
| compileSdk | `36` | `36` | 与 targetSdk 一致；AGP 8.5.2 上限 34 已显式声明 `suppressUnsupportedCompileSdk=36` 并在 API36 全量验证 | ✅ | 提高需升级依赖 | 已确定（36） |

---

## 5. PACKAGE / NAMESPACE POLICY

| 维度 | 内容 |
|------|------|
| **CURRENT** | `namespace = "com.pdig.app"`；`applicationId = "com.pdig.app"`；`package`（manifest）同值。三者当前一致 |
| **RECOMMENDED** | **保持 `namespace == applicationId`**。这是 AGP 推荐做法，避免 Kotlin 源码包名与运行时包名分裂 |
| **WHY** | namespace 决定 Kotlin R 类/代码引用包路径；applicationId 决定设备/商店身份。两者分离（历史上曾用于避免上架后无法改 namespace）只在需要保留旧包名时才有意义，本项目无此需求 |
| **CAN_CHANGE_LATER?** | ✅ namespace 在未上架时可改（需全量回归：R 类 import、manifest 引用、androidTest）；上架后 namespace 可以保持旧值而仅改 applicationId（不推荐） |
| **COST_OF_CHANGE** | 上架前：中（源码 import 引用 + 测试 + 重构建）。上架后：**与 applicationId 绑定**，成本高 |
| **MUST_DECIDE_BEFORE** | 与 APPLICATION_ID 同时决定（首次上传前） |

---

## 6. 决策项汇总（等待用户）

| # | 决策 | 当前值 | 建议（参考） | 上架后是否可逆 |
|---|------|--------|--------------|----------------|
| R-1 | APPLICATION_ID | `com.pdig.app` | 用户定 | ❌ 不可逆 |
| R-2 | APP_DISPLAY_NAME_ZH/EN | `个人数字依赖图` / `PDIG` | 用户定（品牌） | 可改但伤品牌 |
| R-3 | 对外 VERSION_NAME | `0.1.0-milestone` | `1.0.0` | 可改 |
| R-4 | 是否启用 Play App Signing | 未定 | **推荐启用**（见 PLAY_APP_SIGNING_DECISION.md） | 首次上传前可定 |
| R-5 | 首发渠道 | 未定 | Google Play 首发（或用户定） | 渠道策略可调整 |

> **红线**：R-1 与 R-3/R-4 在「创建 Play Console 应用 + 第一次上传 AAB」之前必须定案。
> 保持 `com.pdig.app` / `0.1.0-milestone` 直接发布会造成不可逆的对外身份问题。

---

## 7. 状态标记

```text
ANDROID_RELEASE_IDENTITY_READY = PARTIAL（决策包已就绪；R-1..R-5 等待用户）
  必需外部输入：正式 APPLICATION_ID / APP_NAME / VERSION 策略 / Play App Signing 选择
  阻塞类别：PRODUCT_DECISION_REQUIRED（+ 关联 STORE_ACCOUNT_REQUIRED / FINAL_BRAND_REQUIRED）
```

## 配套文档

- `ANDROID_RELEASE_DECISIONS_REQUIRED.md`（决策清单总表）
- `ANDROID_PRODUCTION_SIGNING_ACCEPTANCE.md` / `ANDROID_PRODUCTION_SIGNING_RUNBOOK.md`（签名链路）
- `PLAY_APP_SIGNING_DECISION.md`（Play App Signing 专项）
- `ANDROID_BRAND_ASSET_SPEC.md`（品牌素材规格，依赖 R-2）
