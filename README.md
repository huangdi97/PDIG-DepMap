# PDIG / DepMap

**Local-first Personal Digital Infrastructure Graph**

> 当银行卡、账户、支付工具或个人数字基础设施发生变化时，
> **哪些地方真的需要处理？**

PDIG（Personal Digital Infrastructure Graph，内部代号 **DepMap**）是一个**本地优先**的
个人数字基础设施图谱。它把「我有哪些卡 / 账户 / 服务」与「它们之间谁依赖谁」建成一张图，
然后在你要**换卡、注销、到期、迁移**时，算出真正受影响的下游，并给出可执行的变更计划。

它不是密码管理器，不是记账软件，不是支付钱包，不是订阅管理器，也不是云服务。
**没有后端、没有账号、没有 analytics、没有广告、没有云同步。**

---

## 目录

- [核心原则](#核心原则)
- [当前范围](#当前范围)
- [架构总览](#架构总览)
- [仓库结构](#仓库结构)
- [构建与验证](#构建与验证)
- [Conformance](#conformance)
- [安全模型](#安全模型)
- [`.depmap` 容器格式](#depmap-容器格式)
- [当前平台状态](#当前平台状态)
- [已知限制](#已知限制)
- [开发状态与路线](#开发状态与路线)
- [文档索引（GitHub 入口）](#文档索引github-入口)
- [License](#license)
- [贡献](#贡献)
- [安全报告](#安全报告)

---

## 核心原则

这些是硬约束，不是设计偏好。任何实现都必须遵守：

| 原则 | 含义 |
| --- | --- |
| **Observation ≠ Reality** | 从账单/文件里"看到"的只是观测；只有用户确认后才成为现实 |
| **Proposal ≠ Reality** | 机器只能提出候选，不能替用户确认现实 |
| **Candidate ≠ Node** | 候选实体不进入图谱、不参与影响计算、不 bump revision |
| **Drift ≠ Reality mutation** | 现实漂移只做记录与提示，绝不静默改写已确认的现实 |
| **done ≠ verified** | 完成动作不等于验证通过；验证是独立的第二段 |
| **Coverage ≠ Readiness** | 场景覆盖度不代表变更计划已就绪；两者是不同的判定 |
| **Graph is a state model, not a homepage** | 图谱是状态模型的持久化表达，不是展示页 |
| **宁可漏报，不可把不确定伪装成必须处理** | 第一原则。`criticality=required` **只能由用户设置**，机器永不产生 |

---

## 当前范围

| 里程碑 | 名称 | 状态 |
| --- | --- | --- |
| **MVP01** | Payment Reality Kernel | ✅ 完成（见 `MVP01_RC_AUDIT_REPORT.md`） |
| **MVP02** | Global Source Abstraction | ✅ 完成（见 `MVP02_FINAL_REPORT.md`） |
| **MVP03** | Living Graph & Change Safety | ✅ 完成（见 `MVP03_FINAL_REPORT.md`） |
| **Native Migration** | 三端原生重写（Android / HarmonyOS / iOS） | 🚧 **进行中**（见下方平台状态） |

明确**不在**当前范围：Open Banking、云同步、AI/LLM 自动决策、手机号/邮箱 capability、
GitHub/Domain/Cloud 图谱、支付宝专属解析器。详见 `NEXT_BACKLOG.md` 与 `FUTURE.md`。

---

## 架构总览

```
                    ┌──────────────────────────────────────┐
                    │  Canonical Layer（平台中立，唯一真源）│
                    │  spec/  fixtures/  conformance/       │
                    └───────────────┬──────────────────────┘
                                    │  codegen + conformance
              ┌─────────────────────┼─────────────────────┐
              ▼                     ▼                     ▼
     Android (Kotlin)      HarmonyOS (ArkTS)        iOS (Swift)
      Compose UI              ArkUI                 SwiftUI
      Room+SQLCipher          ArkData               (N4 未开工)
      Android Keystore        HUKS                  (N4 未开工)
              │                     │                     │
              └─────────────────────┴─────────────────────┘
                                    │
                        DEPMAP_CONTAINER_V1
                    （跨平台加密备份/恢复契约）
```

**Canonical Layer 是契约层**：三端不允许各自定义枚举、关系语义或容器格式。
`spec/` 定义 → `tools/codegen` 生成三端枚举 → `fixtures/` 提供平台中立用例 →
`tools/conformance/run.mjs` 判定三端是否与 Canonical 一致。

**Legacy（uni-app x / UTS / DCloud）** 保留在仓库中，**仅作为 Behavior Oracle**，
用于对照与回归，不进入生产依赖路径：

> **Production dependency target：`DCloud = 0` · `UTS = 0` · `uni-app = 0`**

---

## 仓库结构

```
spec/          Canonical Spec（domain / schema / state-machines / errors /
               security / ui / migration）—— 平台中立的唯一真源
fixtures/      平台中立 conformance 用例（91 个）+ import 原始输入（CSV/OFX/QFX）
conformance/   CONFORMANCE_MANIFEST.json（含 sha256 与 oracle 提交）
tools/         codegen / conformance runner / freeze 取证 / harmony 构建镜像

android/       Android 原生工程（Kotlin + Compose）—— N1/N2 完成，CORE_FROZEN
harmony/       HarmonyOS 原生工程（ArkTS + ArkUI Stage Model）—— N3 进行中
ios/           iOS 原生（Swift）—— 仅 codegen 产物，N4 BLOCKED_BY_MACOS

legacy/        Legacy 定位声明（Behavior Oracle 说明）
core/          Legacy TypeScript Core —— 冻结的 Behavior Oracle（453 tests）
app/           Legacy uni-app x UI（24 页）—— Behavior Oracle
platforms/     Legacy 三端工程与产物 —— Behavior Oracle

docs/          99 份工程文档（设计 / 基线 / 审计 / 报告 / 平台手册）
```

> 本轮**不做 repo-wide restructure**。以上结构与历史一致，未为 GitHub 观感移动目录。

---

## 构建与验证

### 0. 前置

| 工具 | 版本 |
| --- | --- |
| Node.js | ≥ 22.5（Canonical 层与 Legacy oracle） |
| JDK | **21（Android，硬要求）** — CI 亦使用 JDK 21 |
| Android SDK | 通过 `android/local.properties` 指向（该文件已 gitignore） |
| DevEco Studio + HarmonyOS SDK | 5.0.5.310 / API 13（Harmony） |
| Xcode + macOS | iOS N4 需要，当前 `BLOCKED_BY_MACOS` |

### 1. Canonical 层（任何平台都需要先跑通）

```bash
node tools/codegen/generate.mjs --check    # spec → 三端 generated 一致性
node tools/conformance/run.mjs             # codegen → fixture 完整性 → oracle → 平台报告
```

### 2. Legacy Behavior Oracle

```bash
cd core
npm ci                 # 唯一受支持的安装方式（lockfile 锁定）
npm test               # 453 tests / 43 文件
npm run check          # format + lint + typecheck + tests + architecture + secrets
```

### 3. Android

**JDK 21 是硬要求。** Gradle 默认会拿 PATH 上第一个 `java`；本机那通常是 JDK 8，
于是 Android Gradle Plugin 会以
`Dependency requires at least JVM runtime version 11` /
`Cannot find a Java installation ... languageVersion=21` 这种间接错误失败，而不是告诉你"版本不对"。
因此在跑 gradlew 前必须显式指定 `JAVA_HOME`（jbr 目录随 IDE 安装，勿与其他 JDK 混用）：

```bash
cd android
JAVA_HOME="<JDK21_HOME>" ./gradlew --no-daemon :core:test :conformance:run
JAVA_HOME="<JDK21_HOME>" ./gradlew --no-daemon :app:assembleDebug
```

`<JDK21_HOME>` 指向一个 **JDK 21** 安装根目录（内含 `bin/javac`）。
版本不对时 `android/build.gradle.kts` 会前置抛出 `Requires JDK 21 ...`（列出实际检测到的版本与 `java.home`）——
这条检查存在的唯一目的，就是让人不需要再去猜第二次。

> 非 ASCII 工程路径（如含中文目录）会被 `settings.gradle.kts` 自动检测并把构建输出
> 重定向到 ASCII 路径，无需额外环境变量；需要显式指定时用
> `PDIG_ASCII_BUILD_ROOT=<ASCII_BUILD_ROOT>/pdig-build`。

### 4. HarmonyOS

```bash
node tools/harmony/build-ascii-mirror.mjs        # 镜像 + assembleHap
node tools/harmony/build-ascii-mirror.mjs --clean
```

> hvigor 5.13.2 **拒绝含非 ASCII 的工程路径**，因此必须先做 ASCII 镜像再构建
> （镜像目录默认 `%USERPROFILE%/pdig-harmony-build`，可用 `PDIG_HARMONY_BUILD_ROOT` 覆盖）。
> 依赖通过 junction 挂接 DevEco 内置 `@ohos/hvigor*`（公共 registry 无 5.13.2）。

---

## Conformance

Conformance 是**跨平台一致性的唯一判据**，91 个平台中立用例：

```
impact 13 · readiness 16 · coverage 6 · relations 18 · depmap 3 · jcs 1
scenario 1 · migration 2 · state-machine 5 · parser 22 · timeline 3 · backup 1   = 91
```

判据链（严格顺序）：

1. **CODEGEN GATE** — 手改 generated 文件即失败
2. **FIXTURE INTEGRITY** — 每个 fixture 的 sha256 必须与 manifest 一致
3. **ORACLE SELFCHECK** — 冻结的 TS oracle 必须逐字节复现全部 fixture
4. **PLATFORM REPORTS** — 逐用例 diff `conformance/reports/<platform>.json`
5. **SUMMARY** — 汇总矩阵 + 退出码（0 = 全部可判定项 PASS）

诚实口径：**平台报告缺失记为 `NOT_RUN`，不算 PASS，也不算 FAIL。**

```bash
node tools/conformance/run.mjs
```

详见 `docs/CONFORMANCE.md`。

---

## 安全模型

| 维度 | 实现 |
| --- | --- |
| 数据存储 | 本地 SQLCipher 全库加密（Android 已落地） |
| 密钥保护 | 平台密钥库：Android Keystore / Harmony HUKS；**原始密钥不落盘、不进日志** |
| 备份导出 | `.depmap` 容器：Argon2id KDF + AES-256-GCM |
| 会话保护 | App Lock；敏感页 `FLAG_SECURE` 防截图 |
| 文件访问 | SAF / picker 最小权限、只读、用完归还 URI grant |
| 进程死亡 | 保守恢复：只保留 metadata，文件结果作废 → `INTERRUPTED` |
| 网络 | **零网络原语**（`docs/NETWORK_AUDIT.md`：0 命中） |
| 日志 | 敏感字段零输出（`docs/LOGGING_AUDIT.md`） |

深度文档：`docs/SECURITY_MODEL.md` · `spec/security/security-policy.md` ·
`docs/CRYPTO_PROTOCOL.md` · `docs/FAIL_CLOSED_MATRIX.md` · `docs/PRIVACY_DATAFLOW_AUDIT.md`。

> **重要**：安全结论一律以**实跑取证**为准，不以"实现存在"为准。
> 无运行时证据的项在状态文档中一律标 `SOURCE_READY` / `BUILD_READY`，**不标 PASS**。

---

## `.depmap` 容器格式

`DEPMAP_CONTAINER_V1` 是**跨平台契约**，Android / HarmonyOS / iOS 必须完全一致：

| 项 | 值 |
| --- | --- |
| KDF | **Argon2id**，version **19** |
| 对称加密 | **AES-256-GCM**，16 字节 tag、12 字节 nonce |
| Salt | 16 字节 |
| AAD | `UTF8(JCS({format, formatVersion, kdf, cipher}))` —— **ciphertext / tag 不进 AAD** |
| 序列化 | **JCS（RFC 8785）** 确定性键序 |
| Base64 | RFC 4648 标准带填充 |
| 迁移 | payload v1 / v2 可迁移；未来版本必须 reject（不静默降级） |

Golden Vector 定义在 `spec/security/depmap-container-v1.json`，
三端必须产出**逐字节一致**的 `derivedKey`。

> **Argon2id 是 Compatibility Gate，不是实现建议。**
> 任何一端不得为图方便改用 PBKDF2，也不得使用独立的容器格式。

详见 `docs/DEPMAP_FORMAT.md` / `docs/CRYPTO_PROTOCOL.md`。

---

## 当前平台状态

> 2026-09-17 口径。**只记录实跑结论，不虚报。**

| 平台 | 阶段 | 状态 |
| --- | --- | --- |
| **Android** | N1 / N2 完成 | `ANDROID_NATIVE_CORE_HANDOFF = PASS`；`N1 = PASS`；`N2 = PARTIAL_WITH_REPORT (62/73)`；Conformance **91/91**；`ANDROID_PRODUCTION_RELEASE_READY = BLOCKED_BY_PRODUCTION_SIGNING`。已进入 **CORE_FROZEN / MAINTENANCE_ONLY** |
| **HarmonyOS** | N3 进行中 | `HARMONY_BUILD = PASS`（hvigor 全清重建 → HAP 60,133 B）；`HARMONY_DOMAIN` / `HARMONY_ARKUI` = PARTIAL；**`HARMONY_DEPMAP = BLOCKED`**（cryptoFramework 无 Argon2）；**`HARMONY_RUNTIME_E2E = NOT_RUN`**（无模拟器镜像 / 无设备） |
| **iOS** | N4 未开工 | `BLOCKED_BY_MACOS`（无 macOS / Xcode） |
| **Legacy** | 冻结 | uni-app x / UTS / DCloud，仅作 **Behavior Oracle** |

外部 blocker 全表见 `BLOCKERS.md`、`NATIVE_EXTERNAL_BLOCKERS.md`。

---

## 已知限制

- **Impact 仅覆盖 payment capability**；其他 capability 只存边，不参与影响计算
- **Group 仅支持 ANY / ALL**，不支持 N-of-M
- **账单导入与加密备份在 Legacy 路线上不可用**（B20 / B21）；Android 原生侧已具备真实实现与设备内证据
- **Harmony `.depmap` 未实现**（Argon2 blocker）；**iOS 全部未验证**
- **Harmony / iOS 无运行时证据**（无模拟器镜像 / 无 macOS）
- **尚未发布生产版本**：缺生产签名 keystore、商店账号、正式包名与隐私政策 URL
- 真实账单双 Gate 保持 **NOT_RUN**（无真实数据）

---

## 开发状态与路线

| 关注面 | 状态 |
| --- | --- |
| Native Migration | 进行中（Android 已冻结；Harmony N3 进行中；iOS N4 阻塞） |
| Android N1 / N2 | 见 `ANDROID_N1_N2_FINAL_CLOSURE_REPORT_V2.md`、`ANDROID_NATIVE_CORE_FREEZE.md` |
| Harmony N3 | 见 `HARMONY_N3_IMPLEMENTATION_STATUS.md`、`HARMONY_N3_CONFORMANCE_REPORT.md`、`HARMONY_N3_RUNTIME_REPORT.md` |
| Cross-platform Conformance | 见 `CROSS_PLATFORM_CONFORMANCE_MATRIX.md` |
| Legacy Cutover | **NOT_STARTED**（Cutover 条件未满足；Legacy 保留为 Behavior Oracle） |

工程规范：`docs/ENGINEERING_STANDARDS.md` · 完成定义：`docs/DEFINITION_OF_DONE.md` ·
Agent 协议：`docs/AGENT_DEVELOPMENT_PROTOCOL.md` · 变更风险分级：`docs/CHANGE_RISK_POLICY.md`。

---

## 文档索引（GitHub 入口）

| 文档 | 内容 |
| --- | --- |
| [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) | 分层架构、Canonical 层与三端原生边界 |
| [`docs/SECURITY_MODEL.md`](docs/SECURITY_MODEL.md) | 本地优先与失败即关闭的安全模型 |
| [`docs/CONFORMANCE.md`](docs/CONFORMANCE.md) | 一致性链路五阶段与 `NOT_RUN ≠ PASS` 口径 |
| [`docs/DEPMAP_FORMAT.md`](docs/DEPMAP_FORMAT.md) | `.depmap` 容器格式与密码学参数 |
| [`docs/NATIVE_MIGRATION.md`](docs/NATIVE_MIGRATION.md) | 迁移阶段 N0–N7 与三端现状 |

发布与合规记录：`GITHUB_SECRET_PRIVACY_AUDIT.md` · `GITHUB_HISTORY_SANITIZATION_REPORT.md` ·
`GITHUB_REPOSITORY_SIZE_AUDIT.md` · `TAG_REWRITE_MAP.md` · `LICENSE_DECISION.md`。

---

## License

**License: TBD**

本项目尚未最终选定开源许可证，候选与权衡见 **[`LICENSE_DECISION.md`](LICENSE_DECISION.md)**。
在用户明确选定之前，仓库保持 **PRIVATE**，README 不声称任何默认授权。

> 注意：**没有 LICENSE 文件不等于 MIT，也不等于任何默认授权。**
> 在 License 确定前，除本仓库所有者外的任何复制、分发、再授权都不被授权。

---

## 贡献

见 **[`CONTRIBUTING.md`](CONTRIBUTING.md)**。简要约定：

- 先读 `CANONICAL_DESIGN.md` 与 `docs/ENGINEERING_STANDARDS.md`
- 任何跨端行为变更**必须**先改 `spec/`，再改实现，再补 fixture
- **不允许**为通过门禁删除或跳过测试；**不允许**降低 strict / lint 等级
- **不允许**用 synthetic 数据冒充真实数据取证
- 提交前跑通 `node tools/conformance/run.mjs`
- 提交信息遵循 `docs/COMMIT_CONVENTION.md`

---

## 安全报告

见 **[`SECURITY.md`](SECURITY.md)**。

**请勿在公开 issue 中粘贴**：真实账单、银行卡号、口令、`.depmap` 明文、数据库文件或含个人数据的截图。
请提供脱敏日志与 **synthetic fixture** 的最小复现。
