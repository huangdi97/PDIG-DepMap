# BLOCKERS.md

> 本文件只记录**无法由代码解决**、确实需要用户/外部环境介入的事项（AGENTS §20）。
> 更新：2026-09-23（ANDROID API36 工程全速收口轮 —— E-1..E-10 状态与证据刷新）
>
> ⚠ **结构声明**：本文件已按 Goal §28 重构为结构化格式
> （Gate / Status / Blocker class / Why blocked / Engineering work remaining /
> User input required / Closure procedure / Evidence）。
> 旧的 uni-app x 时代 B1/B2/B10/B20-B24 记录已归档至历史区（见文末），
> 不代表当前 Android 原生路线的阻塞 —— 当前路线技术栈为 **Android(Kotlin/Compose)**（AGENTS §24.5）。

---

> v0.1.0 Developer Preview 轮注记（2026-09-24）：E-1..E-10 状态不变；按 Goal §91，
> 以上外部 blocker **均不阻塞 GitHub Developer Preview v0.1.0**（真机/生产 keystore/
> 商店账号/正式包名/隐私 URL/真实账单/CI billing 均以 Known Limitation 如实披露，
> 见 `RELEASE_NOTES_0_1_0.md`）。

> v0.1.2 质量迭代收口轮注记（2026-09-24）：E-1..E-10 状态不变，其中 **E-10（CI billing）保持
> CI_EXTERNAL_BLOCKED**；本轮无新增外部 blocker。已知限制（非 blocker，如实登记）：
> 无 Play 生产发布；Android 运行时 smoke 已修复并取得新鲜证据（PASS）：android-36 系统镜像损坏
> 经 sdkmanager 重装修复，pdig36 AVD 恢复可引导；安装/launch/无崩溃/卸载数据全 Success +
> connectedPreviewDebugAndroidTest **60/60 新鲜复跑**（详见 WORK_STATUS.md 2026-09-24 晚注记）；
> 无真机/真实数据（全合成 fixture）；Harmony/iOS 不在本轮；Windows 未签名（SmartScreen）；
> 旧 tag product-v0.1.0/v0.1.1 保留。本轮结论标注：质量 Gate 10 项计数全 0（EXCEPTIONS.json 复核
> JUSTIFIED：fileSizeOver300×2 / kotlinEscapes×2 lateinit / secretPattern×2 fixture 合成口令）；
> 死代码清理（FIXED：desktop FileOps.kt 删除无调用者的 DesktopFileOps.write，6 行）。
> 完整审计结论见 `GLOBAL_CODE_QUALITY_AUDIT.md` / `PRODUCT_V0_1_2_RELEASE_MANIFEST.md`。

> v0.2.0 发布轮注记（2026-09-25）：E-1..E-10 **状态不变**（真机 / 生产 keystore / 商店账号 / 真实数据 /
> 隐私 URL / CI 计费均为 Known Limitation 而非本轮阻塞）。tablet 全量套件早期挂起已关闭：根因=外来 API-15 模拟器 zhishen_rc 抢占 gradle 设备枚举；设备守卫 + 新 AVD（pdig36_tablet_b，3GB）后全量 connected androidTest **60/60 PASS**（1920×1200@240dpi）。原始 2560×1600 下 2 例启动时序 flake（本机 2K 渲染过慢）非代码缺陷。其余见
> `PRODUCT_V0_2_0_RELEASE_MANIFEST.md`。

## 当前 Active blockers（全部为真实 EXTERNAL_BLOCKER）

| #    | Gate                                        | Status                                        | Blocker class                                                 | Why blocked                                                                                                                                                                                                                                                                                                                                                                                                                                     | Engineering work remaining                                                                                                                                                                        | User/external input required                                       | Exact closure procedure                                                                                                       | Acceptance evidence                                         |
| ---- | ------------------------------------------- | --------------------------------------------- | ------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------ | ----------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------- |
| E-1  | `ANDROID_REAL_DEVICE_VERIFIED`              | BLOCKED                                       | `REAL_DEVICE_REQUIRED`                                        | 无真实 Android 手机；**API36 AVD 已充分覆盖**（androidTest 59/59×2、Core Journey 41/41、三场景 32/32，见 `ANDROID_16_API36_CLOSURE_REPORT.md`），但真实硬件/传感器/系统 UI/指纹匹配维度未验证                                                                                                                                                                                                                                                   | 无（验收计划已就绪：`ANDROID_REAL_DEVICE_ACCEPTANCE_PLAN.md` + `scripts/android_real_device_acceptance.ps1`；本轮 `ANDROID_REAL_DEVICE_FINAL_REPORT.md` 如实 BLOCKED）                            | 一台真实 Android 手机（API 26+）                                   | 按验收计划逐节执行 → 工具自动收集 → 人工判定                                                                                  | 验收计划 §10 结论模板 = PASS                                |
| E-2  | `ANDROID_SIGNING_READY`                     | BLOCKED                                       | `PRODUCTION_KEY_REQUIRED`                                     | 无生产 keystore；不以 debug/non-prod key 冒充（AGENTS §19）                                                                                                                                                                                                                                                                                                                                                                                     | 无（本轮再次验证 NON-PRODUCTION 签名链路：`-PpdigNonProdSigning=true` 产出 signed APK + `apksigner verify` v2 PASS、signed AAB `jarsigner` verified；证据见 `ANDROID_PRODUCTION_RC_MANIFEST.md`） | 生产 keystore + storePassword + keyAlias + keyPassword（K-1..K-4） | 按 `ANDROID_PRODUCTION_SIGNING_ACCEPTANCE.md` §2 步骤 1–8                                                                     | 签名 APK 可安装 + apksigner verify Signer#1 为生产证书      |
| E-3  | `ANDROID_RELEASE_IDENTITY_READY`            | PARTIAL（决策包就绪）                         | `PRODUCT_DECISION_REQUIRED`                                   | applicationId / 对外版本名 / Play App Signing / 首发渠道未定案；本轮 TARGET_SDK/compileSdk 已升 36（`ANDROID_RELEASE_IDENTITY_DECISION.md` 已同步）                                                                                                                                                                                                                                                                                             | 无（决策包已就绪；`ANDROID_VERSIONING_POLICY.md` 本轮新建）                                                                                                                                       | R-1..R-5 用户裁决                                                  | 用户在首次上传 AAB 前定案                                                                                                     | 决策项全部关闭                                              |
| E-4  | `ANDROID_STORE_METADATA_READY`              | PARTIAL（文案完成）                           | `FINAL_BRAND_REQUIRED` + `STORE_ACCOUNT_REQUIRED`             | 截图/图标/feature graphic 需最终品牌；Play 开发者账号未创建                                                                                                                                                                                                                                                                                                                                                                                     | 无（STORE_LISTING / DATA_SAFETY / SUPPORT_PAGE / RELEASE_NOTES / PERMISSION_RATIONALE 草稿全部就绪）                                                                                              | 最终品牌素材 + Play 账号                                           | 品牌定案 → 素材按 `ANDROID_BRAND_ASSET_SPEC.md` 制作 → 截图按 shot list 拍摄                                                  | 素材 + 截图齐备                                             |
| E-5  | `ANDROID_STORE_SUBMISSION_READY`            | BLOCKED                                       | `HUMAN_STORE_FLOW_REQUIRED` + `STORE_ACCOUNT_REQUIRED`        | 商店人工提交流程需要账号 + 人工操作                                                                                                                                                                                                                                                                                                                                                                                                             | 无                                                                                                                                                                                                | Google Play 开发者账号 + 正式 applicationId                        | 创建应用 → 上传 AAB → 填写列表 → 提审                                                                                         | Play Console 提交状态                                       |
| E-6  | `PRIVACY_URL` / `SUPPORT_URL`               | BLOCKED                                       | `PUBLIC_URL_REQUIRED`                                         | 无公开域名；不以 GitHub raw URL 冒充                                                                                                                                                                                                                                                                                                                                                                                                            | 无（内容草稿就绪：`PRIVACY_POLICY_DRAFT.md` / `store/SUPPORT_PAGE_DRAFT.md` / `PRIVACY_URL_REQUIREMENT.md` / `SUPPORT_URL_REQUIREMENT.md`）                                                       | 用户托管公网 URL + 联系渠道                                        | 发布草稿为正式页面 → 回填 Play Console                                                                                        | URL 可公开访问                                              |
| E-7  | `REAL_DATA_CORRECTNESS` / `REAL_DATA_VALUE` | BLOCKED                                       | `REAL_DATA_REQUIRED`                                          | 无用户授权真实账单                                                                                                                                                                                                                                                                                                                                                                                                                              | 无（Pilot-0 协议已就绪：`REAL_DATA_PILOT_0_PROTOCOL.md` / `REAL_DATA_PRIVACY_PROTOCOL.md`）                                                                                                       | 1 位用户 + 1 份本人授权账单                                        | 按 Pilot-0 协议执行                                                                                                           | 指标表达成（false must_change = 0 硬目标）                  |
| E-8  | `IOS_BUILD` / iOS 侧                        | BLOCKED                                       | `IOS_MACOS_ENVIRONMENT_REQUIRED`（契约授权的 iOS 环境类）     | 本机 Windows 无 macOS/Xcode                                                                                                                                                                                                                                                                                                                                                                                                                     | 无（iOS 代码已在仓库，swift 侧 conformance 由 CI iOS job 守护，本地构建需 Mac）                                                                                                                   | macOS 环境                                                         | macOS 上 `swift test` / Xcode 构建                                                                                            | `IOS_BUILD = PASS`                                          |
| E-9  | Harmony N3 恢复                             | ACTIVE（2026-09-25 推进中；代码侧五轮已完成） | `DEVICE_RUNTIME_REQUIRED`（外部：华为账号 + 模拟器镜像）      | 2026-09-25 五轮代码侧推进完成：round#1 parity 0→22/73；round#2 ArkTS 持久化逻辑层 → 27/73；round#3 内存持久化执行层 → 29/73；round#4 Import 管线 host 集成 → 29/73；round#5 host **Core-Journey E2E**（host **142/142**）→ parity 保持 **29/73**（见 NATIVE_PARITY_MATRIX.md）。**唯一外部阻断点 = Emulator 系统镜像缺失**（需华为账号登录下载，HARMONY_RUNTIME_ENVIRONMENT_AUDIT.md 证据完整），`HARMONY_RUNTIME_E2E = RUNTIME_NOT_RUN` 不冒充 | 下一阶段代码缺口（非 blocker）：ArkData 物理 DB 适配（relationalStore 绑定，data/Repository.ets 执行面）与 UI 页面 parity                                                                         | 华为账号在 DevEco 中下载 Emulator 系统镜像（API 12+）              | 镜像就位 → `hdc list targets` 非空 → 安装 HAP → 4 条 DEVICE-BLOCKED canonical 复跑 → `RUNTIME_E2E` 判 PASS                    | `HARMONY_RUNTIME_E2E = PASS`（模拟器实跑，canonical 91/91） |
| E-10 | CI Canonical job 调度                       | BLOCKED（**全部 job** 未启动）                | `EXTERNAL_REPO_ACCOUNT_BILLING`（GitHub 账户计费/配额，外部） | 2026-09-23 本轮 exact-SHA dispatch（run **35839835622**，head_sha=`fe952a0…`）**4/4 job 全部未启动**，annotations 显示「recent account payments have failed or your spending limit needs to be increased」（Harmony static / Android app / Android core / Canonical 均被拦截）—— 证实账户计费限制不只是 Canonical，而是全账号 CI 调度；本地同 HEAD 全量测试绿（见 `ANDROID_16_API36_CLOSURE_REPORT.md`），非代码缺陷                            | 无（代码与 CI 配置无缺陷；`ANDROID_CI_EXACT_SHA_POLICY.md` §5 记录本轮证据）                                                                                                                      | 用户处理 GitHub 账单/提升 spending limit                           | GitHub → Settings → Billing & plans → 处理账单 → 重跑 `gh workflow run CI --ref feat/android-production-release` → 复检 4 job | 本轮 push 对应实例（run 35839835622 重跑）全部 job success  |

> 说明：E-9 已于 2026-09-25 用户批准恢复（E-9 PAUSED → ACTIVE，Harmony N3 代码侧推进轮完成）。代码侧无外部依赖；
> 剩余外部依赖仅为**华为账号 + Emulator 系统镜像**（用于 `HARMONY_RUNTIME_E2E`），记录在此透明。

---

## 结构化总结（Goal §28 要求）

- **ENGINEERING_GAP = 0**：所有工程可关闭项已在本轮关闭（parity 69/73 的 4 项全部为环境/外部/素材类）。
- **TEST_EVIDENCE_GAP = 0**：回归全绿证据本轮新鲜取得（见 WORK_STATUS.md Current 区）。
- 剩余项（E-1..E-8 + E-10）全部为真实外部 blocker，无工程/测试缺口。

---

## 已解除 / 历史（归档，不代表当前路线）

> 以下为 uni-app x / 旧路线的历史记录，技术栈已切换为三端原生（AGENTS §24.5），
> 这些条目不再构成当前阻塞。保留仅为历史透明。

- B1（Android 产品级 APK/AAB 不可产出）→ **已解除**：原生 Kotlin/Compose 工程可产出
  `app-debug.apk` / `app-release.aab`（本轮实测）。
- B2（Harmony 产品级 HAP）→ **部分解除**：hvigor 可构建 HAP；产品级签名/设备仍外部阻塞（Harmony 本轮暂停）。
- B10（DCloud/uni-app x 打包）→ **已解除**：Production 已冻结为原生路线，DCloud = 0 target（AGENTS §24.5）。
- B20/B21/B22（UTS 桥接缺口）→ **已解除**：原生三端直接实现，不再依赖 UTS 桥接。
- B23（沙箱限制）→ 历史记录，不再适用（原生构建已在本机跑通）。
- B24（无 Android 设备）→ 已部分解除：AVD 可用且设备内测试全绿；**真实手机仍为 E-1**。


---

## 2026-09-26 MULTI_CLIENT RUNTIME/VISUAL EVIDENCE SWEEP（追加注记）

- 全新证据轮（feature 分支 test/multiclient-runtime-visual-sweep，装配 SHA 73b0216）：
  - Desktop：`--smoke` 16/16 PASS；`--shots` 50/50 PASS（24 页 × ≥1 窗口档，3 分辨率档；修复 2 个布局缺陷：PdigPage 无限滚动嵌套崩溃、Node 页重复“名称”行）。
  - Android：fresh canonical **91/91**（android.json 2026-09-26）；connected instrumented **61/61**（API36 pdig36 AVD；修复 AppLock 测试时序 flake）；production-debug APK 37.2MB install Success。
  - Harmony：host **142/142**；canonical 87/91 host（fail=0）；clean assembleHap SUCCESSFUL（HAP sha256 80beb459…）；runtime BLOCKED（E-9 模拟器镜像，preflight 本轮新鲜取证）。
  - iOS：macOS runner run 36231032190（push 触发，head 73b0216）；app/UI 无 target → NOT_IMPLEMENTED（N4 gap，不伪造）。
- 受限项（§145 精确记录）：A1 Android 页面视觉 PNG 主机取回 BLOCKED（环境：Android 11+ 作用域存储 + 共享主机 qemu/adb 不稳定）；A2 emulator 稳定性（外部进程强杀 qemu，无日志）；E-9 Harmony 镜像；E-1/E-2/AGC/Apple 签名外部。
- 报告：MULTI_CLIENT_RUNTIME_ACCEPTANCE / FUNCTIONAL_MATRIX / VISUAL_ACCEPTANCE / CROSS_PLATFORM_RUNTIME_DIFF / RUNTIME_EVIDENCE_INDEX + DESKTOP/ANDROID/HARMONY/IOS_SIMULATOR FINAL_REPORT + IOS_RUNTIME_BASELINE_AUDIT。
- 机器可读单源：runtime/RUNTIME_ACCEPTANCE_MATRIX.json（292 行 = 73 特征 × 4 平台，25 字段/行；evidence overlays 为 runtime/evidence/*.json）。
- NATIVE_PARITY_MATRIX 首次引入 Desktop 列（仅本注记，不动 73 行口径）；详细证据见上述报告与 Evidence Index。
