# DESKTOP_V0_1_0_SCOPE_ADDENDUM

> 更新：2026-09-24（v0.1.0 Developer Preview 轮）
> 分支：`feat/global-quality-v0.1.0` → `release/product-v0.1.0`
> 依据：Goal（v0.1.0 全仓工程治理收尾 + Windows/Android 双端 GitHub 发布）§41–§59

## 1. 定位声明（重要）

**Desktop 不是新的 Canonical Truth Source。**

当前产品 Canonical Master（Android 产品优先收口与 Native Migration 整合版 v2.1-R1）的
Production target 仍是 Android / iOS / HarmonyOS。Windows Desktop 是本轮**新增发行目标**
（v0.1.0 Developer Preview），不改变、不旁路、不降级任何 Canonical 语义。

Desktop 与三端共享**同一套**：

- Canonical Spec（`spec/`）
- Schema v3（`spec/schema/persistence-contract.md`）
- `DEPMAP_CONTAINER_V1`（Argon2id v19 / AES-256-GCM / RFC 8785 JCS，见 `spec/security/security-policy.md`）
- Canonical Fixtures（`fixtures/` + `conformance/expected/`）
- Conformance 语义（`android/conformance`）

## 2. 平台范围

| 项            | v0.1.0 决策                                                                                                   |
| ------------- | ------------------------------------------------------------------------------------------------------------- |
| 目标平台      | **Windows x64**（最低、必须真实验证的目标）                                                                   |
| macOS / Linux | 不自动扩展；代码保持跨平台可编译的边界（`DesktopSecurityPort` 接口化），但 v0.1.0 只在 Windows x64 验证与发布 |
| 运行环境      | JDK 21 (Temurin) + Compose Desktop（Kotlin/JVM）                                                              |

## 3. 技术路线

优先复用 platform-neutral Kotlin Core：

```
desktop/app  (Compose Desktop UI / 文件对话框 / 打包)
      ↓
android/:repos   (SQL / 事务 / graphRevision —— 与 Android :app 同一实现)
      ↓
android/:core    (Domain / Impact / Readiness / Parser / Proposal / Crypto 容器)
      ↓
android/:conformance (Canonical 91/91 harness, JdbcSqliteDriver)
```

Desktop 通过 `desktop/settings.gradle.kts` 以 `projectDir` 直接复用
`android/core`、`android/conformance`、`android/repos` 三个模块**逐字不变**——
不出现 `desktopImpactEngine` / `desktopReadinessRule` / `desktopRelationRegistry` 等平行实现。

## 4. 共享与平台差异清单

**共享（同一份实现，禁止平行发展）**：

- Domain 实体与业务规则（`:core`）
- Impact Kernel（`:core` impact）
- Readiness / ScenarioTemplate / ChangePlan 语义（`:core`）
- Parser：WechatParser / Generic CSV / OfxParser / Manual（`:core` sources）
- Canonical 模型与生成枚举（`:core/generated`，由 `tools/codegen` 生成，只读）
- Repository / 事务 / revision 语义（`:repos`）
- Conformance 与 fixture（`:conformance` + `conformance/expected/`）

**平台差异（只允许这些）**：

- UI 层（Compose Desktop `ui/`）
- 文件选择（`io/FileOps.kt` → AwtDesktopFileOps）
- 平台密钥通道（`security/DesktopSecurityPort.kt` → Windows DPAPI）
- 持久化适配（`persist/DepmapFileStore.kt`：.depmap 加密容器文件通道）
- 窗口生命周期（`Main.kt`）
- 打包（installer + portable zip）

## 5. 产品范围（v0.1.0）

覆盖冻结 MVP01–MVP03 核心用户链，屏幕清单（`desktop/app/src/main/kotlin/com/pdig/desktop/ui/`）：

Home / Attention、Sources、Import（微信 CSV / 通用 CSV / OFX-QFX / Manual）、Mapping、Review、
Proposal Review、Candidate Review、Drift Review、Infrastructure、Node Detail、
Scenario Center、Scenario Setup、Impact、ChangePlan、Action、Verification、Timeline、
Backup、Restore、Settings、Security、About、Gate（App Lock）。

**Graph 不是首页**。Graph 路由保持"已注册非入口"的诚实状态（与 Android 一致）。

三个 Scenario 必须分别闭环（Setup → Impact → Plan → Action → Verification），
并持续执行 `done != verified`：

- `replace_payment_card`
- `expiring_payment_card`
- `close_payment_instrument`

## 6. Import 支持

至少支持（复用 Canonical Parser，不重写第二套规则）：

- 微信账单 CSV（`WechatParser`）
- 通用 CSV（字段映射：日期 / 对方 / 金额 / 币种）
- OFX / QFX（`OfxParser`）
- Manual（表单录入）

验证路径见 `desktop/app/src/main/kotlin/com/pdig/desktop/SmokeRunner.kt`
（`--smoke` 无头真实执行 import-wechat-fixture 等步骤）。

## 7. 安全底线（v0.1.0 Desktop 必须满足）

- 敏感数据持久化**只**经 `DepmapFileStore` → `DEPMAP_CONTAINER_V1`（Argon2id v19 /
  AES-256-GCM / RFC 8785 JCS AAD）；任何明文数据库 / 明文 JSON 图 / 明文口令落盘 = FAIL。
- 平台密钥通道：`DesktopSecurityPort`（Windows DPAPI CurrentUser scope + 固定 entropy），
  密钥绑定当前 Windows 用户，不落盘明文密钥；`unprotect` 失败绝不 fallback 明文。
- 备份 = 复制**已加密**容器文件，不产生明文中间件。
- 未来 schema 拒绝（`future_schema`）、错误口令 GCM 认证失败（`auth_failed`）、
  单文件大小守卫（120 MiB）、原子写入（tmp + rename）。
- `InMemorySecurityPort` 仅测试契约用，**禁止注入生产路径**（生产一律
  `WindowsDpapiSecurityPort`）。
- 不自行实现新密码算法。

## 8. 打包与签名状态

- v0.1.0 产出：Windows x64 **installer + portable zip**（NSIS 安装器 + jpackage app-image，
  见 `desktop/app/build.gradle.kts` 的 `collectRuntimeForJpackage` 与 release 脚本）。
- `WINDOWS_CODE_SIGNING = BLOCKED_BY_MISSING_CODE_SIGNING_CERTIFICATE`
  （无正式 Windows Code Signing 证书；不伪造、不用 self-signed 冒充 publisher）。
- Release Notes 必须说明：**Windows 可能显示 SmartScreen 警告**。

## 9. 验收证据（引用）

- Desktop JVM 测试：`desktop/app/src/test`（DesktopSessionTest / DepmapFileStoreTest 等）全绿。
- Headless runtime smoke：`:app:run --args="--smoke"` → `VERDICT: PASS`，
  覆盖 fresh launch / create-open / import / proposal / candidate / drift /
  三 scenario / backup-restore-reopen-delete / wrong-password / tampered / future-schema。
- Conformance：Desktop 复用同一 `:core`，`android/conformance` 91/91（当前总数）全绿。

## 10. 禁止事项（本轮）

- 不得为 Desktop 修改 Canonical 语义 / Impact / Readiness / crypto / .depmap / relation；
  发现的语义问题单独登记（`BLOCKERS.md` / 审计报告），不就地改协议。
- 不得降低安全要求（明文持久化 / debug APK 冒充 Preview / synthetic 冒充真实验证）。
- 不得新建第二套 Domain 语义。

## 11. 0.1.1 增补：共享发现引擎（DiscoveryRepository）

- `android/repos/.../DiscoveryRepository.kt`：Android 与 Desktop **共用**的
  DiscoveryCandidate / RealityDrift 保守生成/累计引擎，在 `commitImport` 事务内运行。
- 规则（spec §9–§10 + invariants DR-01..09 / PC-01..06）：只接受支出型正证据；
  <2 观测不新建；已确认来源忽略；同 key 单一 open 行累计；跨导入 evidence 去重；
  dismiss 后 ≥2 条新观测才回到 pending；accepted/superseded 不再打扰。
- SAFETY：引擎不引用 GraphRepository → 结构上不可能 bump revision；只写
  `discovery_candidates` / `reality_drifts`；机器不自动 accept / resolve / set required。
- 证据链：Desktop smoke（engine-generates-candidate-from-import /
  engine-generates-drift-never-auto-resolves）+ Android 仪器化
  （importGeneratesCandidateAndDrift_conservatively_noAutoResolve）全绿。
- Desktop 窗口/键盘/无障碍验证（v0.1.1）：默认 1100×720、1280×720、1920×1080、
  最大化/恢复、最小 420×320、键盘 Tab/Shift+Tab/Enter/Escape 无崩溃；
  高 DPI（系统 120%）窗口按逻辑尺寸正确缩放；截图 + 日志证据在发布收口报告。
  Compose Desktop 的 UIA 只暴露窗口级元素（已登记为已知限制）。
- Desktop v0.1.0 是 Developer Preview，不是 Stable Production。
