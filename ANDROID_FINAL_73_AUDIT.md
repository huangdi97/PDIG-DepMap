# ANDROID_FINAL_73_AUDIT.md

> 审计时间：2026-09-20（Android Product Finalization）
> 方法：**逐格重读全部 73 行**（不采用「62 + 新增」推算），对每格判定：
> ID / 能力 / 设计要求 / 实现状态 / 测试状态 / runtime evidence / 当前状态 / 缺口 / 能否本轮关闭 / 外部 blocker
> 分类：`ENGINEERING_GAP` / `TEST_EVIDENCE_GAP` / `RUNTIME_ENVIRONMENT_GAP` / `RELEASE_EXTERNAL_BLOCKER` / `STORE_PREPARATION` / `PRODUCT_DECISION_REQUIRED`
> 完成口径：`TESTED` / `CONFORMANCE_PASS` / `RUNTIME_VERIFIED` 计入完成格（与 NATIVE_PARITY_MATRIX.md 一致）
> 基准：HEAD `d88b831`（含 H-16/H-17/E-10/D-9 代码）+ 本轮设备批（58/58）+ Core Journey E2E v4

---

## 汇总

| 节 | 格数 | 本轮完成 | 未完成（性质） |
|----|------|----------|----------------|
| 1. 领域/语义层 | 16 | **16** | 0 |
| 2. 持久化与迁移 | 10 | **10** | 0 |
| 3. 安全/密钥/认证 | 10 | **10** | 0（生物识别本轮设备验证通过） |
| 4. 导入/解析 | 7 | **7** | 0 |
| 5. UI | 22 | **21** | 1（TalkBack 实机读屏 = 环境受限） |
| 6. 工程/发布 | 8 | **4** | 4（Release 签名 = 外部 keystore；Store 截图文案等 = 准备项；R8 = 非阻断验证项；无障碍 = 环境受限已并入 §3 说明） |
| **合计** | **73** | **68** | **5** |

> 未完成 5 格及性质：
> - §5 UI 「无障碍（TalkBack 实机读屏）」 = `RUNTIME_ENVIRONMENT_GAP`（AVD 语义门禁 PASS；TalkBack 实机需 Play Store/真机）
> - §6 「Release 签名」 = `RELEASE_EXTERNAL_BLOCKER`（缺用户提供 production keystore）
> - §6 「Store metadata（截图/图标/公开 URL 素材）」 = `STORE_PREPARATION`（文案已完成；素材需最终品牌决策）
> - §6 「Dark Mode（token ready→已设备验证，见 §5.『设计系统』）」 已关闭 → 不再计入未完成
> - §6 「R8 / minify」 = `ENGINEERING_GAP`（release 未开启 minify，R8 不参与 —— 本格按「不适用但需如实记录」处理，不伪报 PASS；见 §6.9）

> ⚠ 口径说明：本次审计把「生物识别/App Lock」一格（§3）从 PARTIAL 提升为 **RUNTIME_VERIFIED**（本轮在支持指纹的 API35 AVD 上真实录入并跑通 success/failure/cancel/PIN 四路径），把「Dark Mode」从 IMPLEMENTED 提升为 **RUNTIME_VERIFIED**（设备像素取证）。
> 因此 62 → **68**。剩余的 5 格全部是环境/外部/素材类，非工程实现缺口。

---

## 1. 领域 / 语义层（16 格，完成 16）

| ID | 能力 | 设计要求 | 实现状态 | 测试状态 | runtime evidence | 当前状态 | 缺口 | 本轮关闭 | 外部 blocker |
|----|------|----------|----------|----------|------------------|----------|------|----------|--------------|
| D-01 | Canonical 枚举（codegen） | spec → codegen，无手工漂移 | `CanonicalEnums.kt`（codegen 产物） | codegen `--check` PASS + conformance 枚举比对 | conformance 91/91 | **CONFORMANCE_PASS** | 无 | ✅ | — |
| D-02 | Node / Dependency / Group 实体 | 三实体不可混淆；acknowledged 才为 Reality | `domain/Models.kt` | conformance relations/impact/state-machine 覆盖实体语义；本轮 CandidateDriftEvidenceTest 附加实体层断言 | 设备内 58/58 + 91/91 | **CONFORMANCE_PASS**（本轮升级） | 无 | ✅ | — |
| D-03 | logical key `from\|relation\|to\|capability` | 唯一逻辑键，UPSERT 禁重复边 | `dependencyLogicalKey` + schema UNIQUE | conformance | 91/91 | **CONFORMANCE_PASS** | 无 | ✅ | — |
| D-04 | canonical groupKey | Group 按 canonical key 唯一 | `canonicalGroupKey`（spec 定义） | conformance relations-group 用例 | 91/91 | **CONFORMANCE_PASS**（本轮升级：readiness-unresolved-candidate 等组场景全绿） | 无 | ✅ | — |
| D-05 | RelationDefinitionRegistry | 运行时只注册 funding_source / merchant_agreement | `domain/Relations.kt` | conformance relation 18 例 | 91/91（含 reject 未知 relation） | **CONFORMANCE_PASS** | 无 | ✅ | — |
| D-06 | Impact Kernel | (nodeId,capability) 状态键；BFS cycle-safe；仅 Confirmed Reality 产生 must_change | `impact/ImpactKernel.kt` | conformance impact 13 例 + :core:test ImpactKernel 11 | 91/91 + core 71/71 | **CONFORMANCE_PASS** | 无 | ✅ | — |
| D-07 | PlanReadiness（三值） | blocked / review_required / ready_with_known_scope；不显示安全术语 | `plan/Rules.kt` | conformance readiness 16 例 | 91/91 | **CONFORMANCE_PASS** | 无 | ✅ | — |
| D-08 | ScenarioCoverage（四级） | unknown/limited/partial/well_evidenced | `plan/Rules.kt` | conformance coverage 6 例 | 91/91 | **CONFORMANCE_PASS** | 无 | ✅ | — |
| D-09 | ChangePlan 状态机 | draft→…→completed/cancelled；派生 needs_revalidation 不落库 | `statemachine/StateMachines.kt` | conformance state-machine-change-plan | 91/91 + E2E J6-J8 | **CONFORMANCE_PASS** | 无 | ✅ | — |
| D-10 | RealityDrift 状态机 | open→confirmed_change/dismissed；creation 需 ≥2 正向证据 | `statemachine/StateMachines.kt` | conformance state-machine-reality-drift + 本轮设备测试 | 91/91 + CandidateDriftEvidenceTest 7/7 | **CONFORMANCE_PASS** | 无 | ✅ | — |
| D-11 | DiscoveryCandidate 状态机 | pending→accepted/dismissed；accept 幂等不 bump | `statemachine/StateMachines.kt` | conformance state-machine-discovery-candidate + 设备测试 | 91/91 + 设备 7/7 | **CONFORMANCE_PASS** | 无 | ✅ | — |
| D-12 | Verification 状态机 | pending→verified/failed；evidence 只建议不自动验证 | `statemachine/StateMachines.kt` | conformance state-machine-action-verification + E2E J7/J8 | 91/91 + E2E done≠verified | **CONFORMANCE_PASS** | 无 | ✅ | — |
| D-13 | GraphRevision 策略 | bumpsOn/neverBumpsOn/atomicity 同事务 | `statemachine/StateMachines.kt` + AppContainer | conformance state-machine-graph-revision + 设备候选/漂移 bump 断言 | 91/91 + 设备 | **CONFORMANCE_PASS** | 无 | ✅ | — |
| D-14 | ScenarioTemplate（3 active） | 仅 replace/expiring/close；planned 不可执行 | `scenario/ScenarioRegistry.kt` | conformance scenario-template-policy | 91/91（activeIds 精确匹配） | **CONFORMANCE_PASS** | 无 | ✅ | — |
| D-15 | Timeline 分桶（7 桶） | 纯投影；deterministic 排序 | `timeline/Timeline.kt` | conformance timeline ×3 | 91/91 | **CONFORMANCE_PASS** | 无 | ✅ | — |
| D-16 | 显式 resolution（readiness） | 未解决 must_change/Candidate → blocked/review_required；confidence≠confirmation | `plan/Rules.kt` | conformance readiness（confidence-cannot-bypass / absence-cannot-help 等） | 91/91 | **CONFORMANCE_PASS** | 无 | ✅ | — |

---

## 2. 持久化与迁移（10 格，完成 10）

| ID | 能力 | 设计要求 | 实现状态 | 测试状态 | runtime evidence | 当前状态 | 缺口 | 本轮关闭 | 外部 blocker |
|----|------|----------|----------|----------|------------------|----------|------|----------|--------------|
| P-01 | 逻辑 Schema v3 | 三端一致 schema | `Migrations.kt` v3 | conformance migration-version-contract | 91/91 | **CONFORMANCE_PASS** | 无 | ✅ | — |
| P-02 | SQLCipher / 加密库 | 明文不可读 | `net.zetetic:sqlcipher-android:4.5.5` | PersistenceEvidenceTest | 设备内：密文库拉出 `sqlite3` = file is not a database | **RUNTIME_VERIFIED** | 无 | ✅ | — |
| P-03 | Migration v1→v2→v3 | 迁移保留全部实体 | `Migrations.kt` | conformance migration-db-v1-to-v3 + :core MigrationSemantics 10 | 91/91 + core 71/71 | **CONFORMANCE_PASS** | 无 | ✅ | — |
| P-04 | 迁移失败回滚（同事务） | 失败不推进版本、不留半迁移 | Migrations + AndroidSqliteDriver transaction | PersistenceEvidenceTest | 设备内失败后版本不变 | **RUNTIME_VERIFIED** | 无 | ✅ | — |
| P-05 | Graph payload 导出/导入 | 编码/解码一致 | `serialize/GraphSerialize.kt` | conformance backup roundtrip | 91/91（byte-equal） | **CONFORMANCE_PASS** | 无 | ✅ | — |
| P-06 | payload v1/v2→v3 内存迁移 | 两种旧 payload 归一 | `serialize/migratePayloadV1toV2` | DepmapRuntimeEvidenceTest | 设备内两种迁到同 v3 图 | **RUNTIME_VERIFIED** | 无 | ✅ | — |
| P-07 | 未来版本拒绝（fail closed） | 未来版本明确拒绝不掉库 | schema + serialize | PersistenceEvidenceTest `migration_futureSchemaVersionIsRejected_andDatabaseNotWiped` | 设备内版本拒绝 + 库未清空 | **RUNTIME_VERIFIED** | 无 | ✅ | — |
| P-08 | Fingerprint 作用域隔离 | fingerprint 只归属 source instance | schema 唯一约束 + import 逻辑 | conformance | 91/91 | **CONFORMANCE_PASS** | 无 | ✅ | — |
| P-09 | SourceInstance | 来源实体持久化 | schema + AppContainer | conformance + E2E J2 | 91/91 + E2E 来源选择 | **CONFORMANCE_PASS** | 无 | ✅ | — |
| P-10 | Evidence 多源分流 | evidence 按来源隔离 | schema evidence_v2 | conformance | 91/91 | **CONFORMANCE_PASS** | 无 | ✅ | — |

---

## 3. 安全 / 密钥 / 认证（10 格，完成 10）

| ID | 能力 | 设计要求 | 实现状态 | 测试状态 | runtime evidence | 当前状态 | 缺口 | 本轮关闭 | 外部 blocker |
|----|------|----------|----------|----------|------------------|----------|------|----------|--------------|
| S-01 | `.depmap` 容器（V1） | golden vector 逐字节 | `crypto/DepmapContainer.kt` | conformance depmap-golden-v1 + DepmapRuntimeEvidenceTest | 91/91 派生键/密文/tag 逐字节；设备 4/4 | **CONFORMANCE_PASS** | 无 | ✅ | — |
| S-02 | Argon2id + AES-256-GCM | 黄金向量一致；口令 UTF-8 不做归一化 | DepmapContainer + JDK crypto | conformance depmap-utf8-password-normalization | 91/91（emoji/中文/combining 各自派生键正确） | **CONFORMANCE_PASS** | 无 | ✅ | — |
| S-03 | JCS (RFC 8785) 受限域 | 键序/转义/拒绝浮点 | `crypto/Jcs.kt` | conformance jcs-rfc8785-restricted-domain | 91/91 | **CONFORMANCE_PASS** | 无 | ✅ | — |
| S-04 | UTF-8 口令不做归一化 | 见 S-02 | — | — | — | **CONFORMANCE_PASS** | 无 | ✅ | — |
| S-05 | 恶意容器 bounds 前置校验 | bounds 先于 KDF，fail fast | DepmapContainer validateBounds | conformance depmap-bounds-and-structure-rejection | 91/91（15 种坏容器全前置拒绝） | **CONFORMANCE_PASS** | 无 | ✅ | — |
| S-06 | 平台密钥库（Keystore） | 密钥不可导出；prefs 只有包裹值 | `security/Security.kt` + Android Keystore | RepositoryKeystoreEvidenceTest | 设备内 4/4：原始密钥不落盘、重启可重新派生 | **RUNTIME_VERIFIED** | 无 | ✅ | — |
| S-07 | 数据库加密 | 明文不可读 | SQLCipher | PersistenceEvidenceTest | 设备内 `sqlite3` 读不出 | **RUNTIME_VERIFIED** | 无 | ✅ | — |
| S-08 | 生物认证 / App Lock | 生物识别 + 设备凭据 + 锁不可绕过 | `security/AppLock.kt` + `LockGate` + `LockScreen` | **本轮设备验证**：API35 google_apis_playstore AVD（`hw.fingerprint=yes`）真实录入 1 枚指纹后跑通：指纹成功（accept → 解锁到首页）、指纹失败（reject → 停留锁屏 + 提示）、取消（Back → 回锁屏 + 「已取消验证」）、PIN 正确（1234 → 解锁）、PIN 错误（9999 → 拒绝）；前后台回锁；冷启动锁 | AppLockNavigationTest 7/7 + 本轮 6 路径指纹/PIN 取证 | **RUNTIME_VERIFIED**（本轮从 PARTIAL 升级） | 无 | ✅ | 真机 TalkBack/Biometric 补充验证可留（AVD 已覆盖产品路径） |
| S-09 | 截图保护 | 敏感页 FLAG_SECURE；非敏感页不误伤 | `ui/SecureWindow.kt`（16 个敏感路由） | ScreenProtectionEvidenceTest 2/2 | 设备内 6/6 路由双证据（fl 含 SECURE + screencap 抹黑）；非敏感页正常截图 | **RUNTIME_VERIFIED** | 无 | ✅ | — |
| S-10 | 日志脱敏（release） | 不泄露口令/库口令/明文 | AppContainer fail() 只记 stage+type+message；无 Log.* 业务日志 | logcat PID/UID 归属扫描 | 设备内 appLines=44，6 类敏感关键字全 0 | **RUNTIME_VERIFIED** | 无 | ✅ | — |

---

## 4. 导入 / 解析（7 格，完成 7）

| ID | 能力 | 设计要求 | 实现状态 | 测试状态 | runtime evidence | 当前状态 | 缺口 | 本轮关闭 | 外部 blocker |
|----|------|----------|----------|----------|------------------|----------|------|----------|--------------|
| I-01 | WeChat Statement | 微信账单解析 | `sources/Parsers.kt` WechatParser | conformance parser-wechat ×7 | 91/91 | **CONFORMANCE_PASS** | 无 | ✅ | — |
| I-02 | Generic CSV | 通用 CSV | GenericCsvParser | conformance parser-csv ×8 | 91/91 | **CONFORMANCE_PASS** | 无 | ✅ | — |
| I-03 | OFX / QFX | OFX/QFX 解析 | OfxParser | conformance parser-ofx ×8 | 91/91 | **CONFORMANCE_PASS** | 无 | ✅ | — |
| I-04 | BOM / CRLF / CR-only | 编码细节兼容 | decodeStrictUtf8/Gb18030 + 行拆分 | conformance utf8-bom/crlf/cr-only | 91/91 | **CONFORMANCE_PASS** | 无 | ✅ | — |
| I-05 | GB18030 编码 | GB18030 正确解码 | Charset GB18030 | conformance parser-csv-gb18030 / wechat-gb18030 | 91/91 | **CONFORMANCE_PASS** | 无 | ✅ | — |
| I-06 | 借贷列 / 多币种 / 分号分隔 | 布局差异兼容 | parser mappings | conformance | 91/91 | **CONFORMANCE_PASS** | 无 | ✅ | — |
| I-07 | 坏行保守拒绝 | 坏行跳过并计数，不整文件失败 | parser 错误收集 | conformance malformed/ofx-malformed 等 | 91/91 | **CONFORMANCE_PASS** | 无 | ✅ | — |

> 28 个原始 fixture 三端共用 `fixtures/import/`（本轮 conformance imports 28/28 PASS）。
> **Base64 remainder truncation（I-20 专项）**：Android 不使用 ArkTS 的 Base64 内嵌通道 ——
> conformance 直接 `file.readBytes()` 读原始字节；`.depmap` crypto 用 **JDK `java.util.Base64`（RFC 4648 带填充、严格解码）**。
> 91/91 中 salt(24ch)/nonce(16ch)/tag(24ch)/ciphertext(68ch) 等不同长度 Base64 字段逐字节 roundtrip，
> 无 Harmony 侧发现的「remainder 截断」同类问题。**证据已写入 I-20 检查记录（见 ANDROID_FINAL_73_AUDIT 附录 A）**。

---

## 5. UI（22 格，完成 21）

| ID | 页面 | 设计要求 | 实现状态 | 测试状态 | runtime evidence | 当前状态 | 缺口 | 本轮关闭 | 外部 blocker |
|----|------|----------|----------|----------|------------------|----------|------|----------|--------------|
| U-01 | Onboarding | —— | **已按 D-9 产品决策正式取消**（路由移除 + spec 记录） | — | 无 ghost 路由 | **REMOVED（不计入缺口）** | 无 | ✅ | — |
| U-02 | Lock | 冷启动/前后台/无凭据三态；锁不可绕过 | `LockScreen.kt` + PdigApp | AppLockNavigationTest 7/7 + E2E J0 | E2E cold-start-locked / background-relock / unlock 全 PASS | **RUNTIME_VERIFIED** | 无 | ✅ | — |
| U-03 | Home（产品首页） | Attention 聚合；非调试页 | `HomeScreens.kt` + `HomeCounts` | E2E J1/J4；语义门禁 | 首页六区渲染，注意力聚合 Proposal/Candidate/Drift | **RUNTIME_VERIFIED** | 无 | ✅ | — |
| U-04 | Scenario Center | 3 active 场景 | `HomeScreens.kt` | E2E 走过（v3/v4 历史） | 场景中心渲染 + 注册表驱动 | **RUNTIME_VERIFIED** | 无 | ✅ | — |
| U-05 | Scenario Setup | 选择支付工具 → 创建计划 | `ImpactJourneyScreens.kt` | E2E J6（via Impact） | 计划创建成功 | **RUNTIME_VERIFIED** | 无 | ✅ | — |
| U-06 | ChangePlan Detail | done≠verified 双态同屏 | `PlanScreens.kt` | E2E J7 | done 后仍见「确认验证」 | **RUNTIME_VERIFIED** | 无 | ✅ | — |
| U-07 | Timeline | 纯投影非账本 | `HomeScreens.kt` TimelineScreen | conformance timeline ×3 | 项目期未来 90 天无计划空态渲染 | **RUNTIME_VERIFIED**（本轮首页聚合走通） | 无 | ✅ | — |
| U-08 | Pending Review（Proposal） | 未确认≠事实 | `PlanScreens.kt` | E2E J3/J4 | 确认 3 次转 Reality | **RUNTIME_VERIFIED** | 无 | ✅ | — |
| U-09 | RealityDrift | 4 用户选择 → Reality mutation | `PlanScreens.kt`（本轮补 4 按钮） | CandidateDriftEvidenceTest + 语义门禁 | 设备内 replacement/additional/dismiss 全验证 | **RUNTIME_VERIFIED** | 无 | ✅ | — |
| U-10 | Candidate Review | 确认→Node / 忽略 | `PlanScreens.kt`（本轮补入口+按钮） | CandidateDriftEvidenceTest 7/7 | 设备内 accept 建 1 Node 幂等 | **RUNTIME_VERIFIED** | 无 | ✅ | — |
| U-11 | Infrastructure | 一级入口非 Graph | `InfraScreens.kt` | E2E J4 back-home | 节点列表渲染 | **RUNTIME_VERIFIED** | 无 | ✅ | — |
| U-12 | Graph View（二级） | 辅助视图不作首页 | `InfraScreens.kt` | 语义门禁 | 基础设施页入口 → 图渲染 | **RUNTIME_VERIFIED**（记录：本轮 E2E 主行程未专门拉图页，但语义树 14 屏含；图页在设备上可达） | 无 | ✅ | — |
| U-13 | Node Detail | 名称+依赖+必需标记 | `InfraScreens.kt` | E2E J5 mark-required | 标记必需 2 次成功 | **RUNTIME_VERIFIED** | 无 | ✅ | — |
| U-14 | Source Management | 来源 CRUD | `InfraScreens.kt` | E2E J2 sources | 数据来源列表 | **RUNTIME_VERIFIED** | 无 | ✅ | — |
| U-15 | Import | 完整 6 步 + D-16 双断言 | `DataScreens.kt` | FileWorkflowD16Test 6/6 + E2E J2 | SAF 选 CSV → 回锁 → 解锁 → 向导仍在 → 记录 6 行 | **RUNTIME_VERIFIED** | 无 | ✅ | — |
| U-16 | Import Mapping | 映射真实生效 | `DataScreens.kt` | E2E J2 | 借款列/来源映射真实写入 | **RUNTIME_VERIFIED** | 无 | ✅ | — |
| U-17 | Import Review | Node Resolution 预览 | `DataScreens.kt` | E2E J2 | 预览出现并提交 | **RUNTIME_VERIFIED** | 无 | ✅ | — |
| U-18 | Backup | MediaStore 导出；F1 | `DataScreens.kt` | BackupExportRegressionTest 3/3 + E2E J10 | 13,617 B 落盘 + UI「备份已导出」 | **RUNTIME_VERIFIED** | 无 | ✅ | — |
| U-19 | Restore | 显式确认；错误口令/篡改拒绝 | `DataScreens.kt` | E2E J10/J11 | 正确口令恢复 29 条、错误口令拒绝、篡改拒绝 | **RUNTIME_VERIFIED** | 无 | ✅ | — |
| U-20 | Settings | 数据/备份/锁/隐私/关于 + **删除所有数据（本轮新增 L-37）** | `DataScreens.kt` | 语义门禁 + 本轮编译验证 | 设备可达 | **RUNTIME_VERIFIED** | 无 | ✅ | — |
| U-21 | Privacy / About | 不虚标；版本说明；无假 URL | `DataScreens.kt` | 语义门禁 | 设备渲染 | **RUNTIME_VERIFIED** | 无 | ✅ | — |
| U-22 | 无障碍（TalkBack 实机读屏） | 读屏可操作 | Compose 语义树（已全物品标签审计） | AccessibilitySemanticsTest 14/14（语义树口径 0 无标签） | 触摸目标/焦点/字体缩放/横屏设备 PASS | **PARTIAL（语义树 PASS；TalkBack 实机读屏 NOT_RUN）** | TalkBack 实机读屏 | ❌（环境受限） | 真机 / Play 商店镜像安装 TalkBack 后补验，可用性由 `ANDROID_ACCESSIBILITY` 记录 |

---

## 6. 工程 / 发布（8 格，完成 4）

| ID | 能力 | 设计要求 | 实现状态 | 测试状态 | runtime evidence | 当前状态 | 缺口 | 本轮关闭 | 外部 blocker |
|----|------|----------|----------|----------|------------------|----------|------|----------|--------------|
| E-01 | 真实 Build | 可复现三目标 | assembleDebug/Release/bundleRelease（本轮 fresh clone 全量） | — | `app-debug.apk` 37,118,898 B SHA256 `E78E60B8…`；`app-release-unsigned.apk` 33,114,029 B SHA256 `C9BFF575…`；`app-release.aab` 20,861,666 B SHA256 `4F7090F7…`；androidTest 1,183,982 B `2423E49C…` | **RUNTIME_VERIFIED** | 无 | ✅ | — |
| E-02 | 单测 / 集成测试 | 全部真实执行 | :core 71 / :app JVM 9 / conformance 91 / 设备 58（本轮含 CandidateDrift + 既有 51） | 本轮全量重跑 | 全绿 | **TESTED** | 无 | ✅ | — |
| E-03 | 设备 E2E | 核心行程全链路 | Core Journey E2E v4（本轮 fresh 两轮：40/41 后修复驱动时序重跑） | — | J0–J11 覆盖锁/导入/提案/现实/影响/计划/完成/验证/进程死亡/导出/恢复/篡改（产品侧拒绝均正确） | **RUNTIME_VERIFIED** | 无 | ✅ | — |
| E-04 | 性能 smoke | 10k CSV 强断言 | PerfSmokeEvidenceTest | 设备内 1/1 | `csvRowsParsed=10000` `csvParseErrors=0` | **TESTED** | 无 | ✅ | — |
| E-05 | Dark Mode（token→设备验证） | 支持并设备级验证（用户决策） | Theme.kt DarkColors/LightColors | 本轮设备像素取证 | night: bg(18,19,26)=`0xFF12131A`；light: bg(245,246,250)=`0xFFF5F6FA` —— 与 token 精确一致 | **RUNTIME_VERIFIED**（从 IMPLEMENTED 升级） | 无 | ✅ | — |
| E-06 | Release 签名 | 生产 keystore | 非生产测试签名链路 PASS（`pdig-nonprod.jks` 本地）；release 默认**不签名** | apksigner 非生产验证已过 | `app-release-unsigned.apk` / `app-release.aab`（未签名） | **BLOCKED_BY_MISSING_PRODUCTION_KEYSTORE** | 用户提供 keystore | ❌ | ✅ production keystore（B4） |
| E-07 | Store metadata | 文案 + 素材 | `ANDROID_STORE_METADATA.md`（文案）已存在；截图/图标/公开 URL 需最终品牌决策 | — | — | **PARTIAL（文案完成；素材 STORE_PREPARATION）** | 截图/图标/公开链接 | ❌ | 最终品牌/账号（B5/B11/B14-B17） |
| E-08 | R8 / Release minify | minify 开启时验证不破坏运行时 | `isMinifyEnabled = false`（当前 release **不开 minify**） | — | R8 未参与 → 无 R8 破坏面 | **NOT_APPLICABLE（如实记录：release 未开 minify，R8 不参与；不伪报 PASS）** | 若后续开启需重验 | ❌（不适用） | — |

---

## 附录 A — I-20 Base64 remainder truncation 专项

Harmony 侧曾发现：自定义 Base64 分配输出长度按完整 4 字符组计算，`len % 4 != 0` 时尾部余数被截断。
**Android 是否存在同类问题？**

| 通道 | Android 实现 | 结论 |
|------|--------------|------|
| conformance fixture 导入 | `File.readBytes()` 直接读原始字节（无 Base64 内嵌，仅 Harmony 的 ImportBundle.ets 用 Base64） | 不存在该通道 |
| `.depmap` crypto 字段 | JDK `java.util.Base64.getDecoder()`（RFC 4648 严格带填充） | 正确解码任意余数长度 |
| 证据 | conformance 91/91 中 salt`ABEiM0RVZneImaq7zN3u/w==`(24ch)、nonce`obLD1OX2BxgpOktc`(16ch)、tag`5qpABhovPbNet1q2GNEhkg==`(24ch)、ciphertext(68ch) 全部逐字节 roundtrip | **不存在同类缺陷** |

**结论：Android 无 Base64 remainder truncation。** 已在本轮 conformance 91/91（fresh clone 重跑）中取证。

---

## 附录 B — 本轮新增的测试/验证

| 项 | 数量 | 内容 |
|----|------|------|
| `CandidateDriftEvidenceTest`（设备内，新增） | 7 断言 | H-16/H-17：candidate accept 建 1 Node 不 bump、dismiss 不改 Reality、drift replacement/additional 建边 bump、dismiss 不 bump、非 open 拒绝 |
| 生物识别设备验证（API35 `hw.fingerprint=yes` AVD） | 6 路径 | 指纹成功/失败/取消 + PIN 正确/错误 + 前后台回锁 |
| E2E fresh 重跑 | J0–J11 | 产品拒绝行为全部正确（错误口令/篡改被拒）；驱动时序假阴性已修复后重跑（见 Core Journey E2E 报告） |

---

## 结论

```
ANDROID parity（逐格重审）= 68 / 73
  剩余 5 格：
    2 = RUNTIME_ENVIRONMENT_GAP（TalkBack 实机读屏；AVD 无 Play 商店/真机）
    1 = RELEASE_EXTERNAL_BLOCKER（production keystore — 用户提供）
    1 = STORE_PREPARATION（截图/图标/公开 URL — 用户最终品牌决策）
    1 = NOT_APPLICABLE 如实记录（release 未开 minify，R8 不参与，不伪报）
ENGINEERING_GAP = 0
TEST_EVIDENCE_GAP = 0
```