# NATIVE_PARITY_MATRIX.md

> 每格：`NOT_STARTED` → `IMPLEMENTED` → `TESTED` → `CONFORMANCE_PASS` → `RUNTIME_VERIFIED`
> 写状态时不得跳过等级，也不得把"理论支持"写成"已验证"。
>
> **2026-09-23 API36 工程全速收口轮复验注记**：本矩阵保持 **69/73**。
> 剩余 4 格（无障碍 TalkBack 实机 / Release 签名 / Store 素材 / R8·minify）本轮再次逐格确认
> 为真实外部 blocker / 政策项（分类见 `ANDROID_FINAL_73_AUDIT.md`、`BLOCKERS.md`）：
>
> - 无障碍：Compose 语义树在 API36 AVD 复验 PASS；**TalkBack 实机读屏仍需真机（E-1）**，不冒充。
> - Release 签名：NON-PRODUCTION 签名链路在 API36 release 产物复验 PASS（apksigner v2 verify），
>   **正式 production keystore 仍缺失（E-2）**，不以 non-prod 冒充。
> - Store 素材：文案/合规文档全部就绪（本轮新增 `ANDROID_PLAY_CONSOLE_READINESS.md` /
>   `ANDROID_STORE_COMPLIANCE_REPORT.md`）；**最终品牌未定（R-2/E-4）**。
> - R8 / minify：release `isMinifyEnabled=false` 如实记录 **NOT_APPLICABLE**，本轮未擅自开启。
>   本轮实跑回归（**API36 AVD pdig_api36_phone，Android 16 实测 sdk=36**）：`:core:test` 71/71 ·
>   `:app:testDebugUnitTest` 9/9 · `:conformance:run` 91/91 · connectedDebugAndroidTest **59/59** ·
>   assembleDebug / assembleRelease / bundleRelease SUCCESSFUL（含 NON-PROD signed 变体），
>   Core Journey E2E 与三场景 E2E 见 `ANDROID_16_API36_CLOSURE_REPORT.md`。
>   **ENGINEERING_GAP = 0、TEST_EVIDENCE_GAP = 0**（无工程/测试缺口被标为 external）。

> **2026-09-25 Harmony N3 恢复轮（代码侧推进，用户批准重启 E-9）**：
> 本轮把 Harmony 列从「工程已开工但表格未同步」归位为**与真实源码 + 真实执行一致**。全部证据**本轮新鲜实跑**：
>
> 1. `hvigorw assembleHap --mode module -p product=default -p buildMode=debug --no-daemon`（ASCII 镜像 + clean）→ `BUILD SUCCESSFUL in 1 min 3 s`；HAP 3,380,659 B（sha256 `610701e0…a9057c21`，见 NATIVE_MIGRATION_STATUS.md）。
> 2. `check-compiled-reachability.mjs --build` → **26/26 required 模块 reachable + 全部入 `modules.abc`（432,560 B）**；代表模块负向 probe（注入类型错误 → clean 构建必须失败）PASS；孤儿模块 = **0**（30 found / 30 reachable）。
> 3. `run-conformance-host.mjs` → **91/91 host checks**（87 canonical + 3 conformance 元测试 + 1 domain 自检），**87 条 canonical 在真实 ArkTS 运行时逐字节复现冻结 expected，0 fail**；`HARMONY_HOST_PASS = 87/91`。
> 4. `codegen --check` PASS（spec → generated 无漂移，禁止手改产物）。
>
> **归位规则**（无证据不升级、不背「理论支持」）：① 有 ArkTS 源码 + 已编译入 HAP + 有对应 canonical 用例在 host 面 PASS → `CONFORMANCE_PASS（host 执行面）`；② 只有源码 + 编译证据 → `IMPLEMENTED_COMPILED_NOT_RUN`；③ 设备面必须行为（Argon2 native / ArkData / 文件 / 截图等）→ 保持原状或 NOT_STARTED，**不写成 RUNTIME_VERIFIED**。
>
> **Harmony 合计 0 → 22 / 73**（§1 12 + §3 2 + §4 7 + §6 1，明细见文末「Harmony 22/73 的来源」；口径不变：`TESTED` 及以上才计）。
> **RUNTIME 类一格未动**：`HARMONY_RUNTIME_E2E = RUNTIME_NOT_RUN`（唯一外部依赖：华为账号 + Emulator 系统镜像，E-9，见 HARMONY_RUNTIME_ENVIRONMENT_AUDIT.md）。
> **4 条 canonical 在 harmony 侧 DEVICE-BLOCKED**（depmap-golden-v1 / depmap-utf8-password-normalization / migration-db-v1-to-v3 / backup-depmap-export-restore-roundtrip）：统一 harness `tools/conformance/run.mjs` 如实计「no result reported」= FAIL，**不写 PASS**。
> 本矩阵 2026-09-18 及更早的 Harmony 记录保留为历史；旧记「Conformance NOT_RUN（ArkTS runner 未接）」已被 3 号证据推翻（runner 自 2026-09-19 起在 host 面执行）。

更新时间：2026-09-18（**Harmony N3：Argon2 NAPI 全链路 + 编译可达性 Gate**）

> ## 本轮（2026-09-18 第三场）：Argon2 全链路打通 + 编译可达性 Gate 固化
>
> **已有实质进展，但按本表口径仍不计入任何一格。** 原因见下。
>
> - **`ARGON2_NATIVE_BUILD = PASS`**：vendored PHC 参考实现（`local_modifications = 0`）
>   经 OHOS NDK 交叉编译，**双 ABI 通过**（arm64-v8a 40,768 B / x86_64 42,296 B），
>   导出 `argon2_*` 符号 **0** 个。
> - **打包取证**：`libpdiargon2.so` 已进 HAP（`libs/{arm64-v8a,x86_64}/`，49,776 / 50,576 B）；
>   **打包并 strip 后**的动态符号表恰好 3 个（`_init` / `_fini` / `RegisterPdiArgon2Module`），`argon2_*` = 0。
> - **`HARMONY_MODULE_COMPILED = PASS`**：`check-compiled-reachability.mjs` 的
>   A/B/C/D 四判据，7/7 required 模块通过。该 Gate 抓到一个真实缺陷 ——
>   `Argon2idNative.ets` 因依赖方向反转成为**孤儿模块**（源文件存在、从未被编译、
>   构建却是绿的），已通过抽出 `crypto/KdfContract.ets` 修复。
> - **`HARMONY_DEPMAP` 变更**：`BLOCKED_BY_NATIVE_VERIFICATION`
>   → **`NATIVE_BUILD_PASS / ON_DEVICE_NOT_RUN`**。
>
> ### 为什么这些进展**一格都不计入**
>
> 本表要求 `IMPLEMENTED → TESTED → CONFORMANCE_PASS → RUNTIME_VERIFIED` 逐级推进。
> 本轮的成果全部落在「代码正确、被编译、被打包、边界正确」这一层，
> **没有一项在 Harmony 运行时里执行过任何用例**。
>
> 特别地，"信任边界成立（`argon2_*` 不导出）"这一条看起来很像 `TESTED`，
> 但它验证的是**链接/打包属性**，不是**行为正确性** ——
> 它没有证明 `deriveArgon2id` 会算出正确的派生密钥。
> 因此仍记 `IMPLEMENTED`。
>
> **Harmony 合计仍为 0 / 73。** 这个数字被刻意保持诚实：
> 三项都是实实在在的工程进展，但按口径它们确实还不够格。
>
> 唯一阻断点已根因定位：**缺 Emulator 系统镜像**（需人工登录下载）——
> 见 `HARMONY_RUNTIME_ENVIRONMENT_AUDIT.md`。

> ## Harmony N3 开工（2026-09-17 第二场）
>
> - `harmony/` 由「仅 1 个 codegen 文件」建成**可被 hvigor 真实构建的 Stage Model 工程**：
>   全清重建 `BUILD SUCCESSFUL`，产出 HAP。
> - 首个**纯 ArkTS Domain**（`Relations.ets`）已实现并**编译打包进 HAP**。
> - Harmony 列本轮变更的格：① `RelationDefinitionRegistry` → `IMPLEMENTED`；
>   ② `真实 Build` → `IMPLEMENTED`（有构建证据，非 RUNTIME_VERIFIED）；
>   ③ `Canonical 枚举` 保持 `IMPLEMENTED`（codegen 产物，未做用例级验证）。
> - **Harmony 合计仍为 0 / 73**：以上三格均未达 `TESTED` 及以上，按口径**不计入**已完成。
>
> **2026-09-18 追加（AES / JCS / container）**：`harmony/entry/src/main/ets/crypto/`
> 新增 `Jcs.ets` / `DepmapContainerV1.ets` / `ContainerSelfCheck.ets`，
> 主机侧黄金校验 **5/5 PASS**（`tools/harmony/verify-container-golden.mjs`），
> ArkTS **真实编译**。但**均未达 `TESTED`**，按口径仍**不计入**。
> 定位过程中发现一条会影响所有后续 Harmony 结论的工程事实：
> **hvigor 只编译从 ability / page 可达的模块**，未被引用的 `.ets` 放语法错误也照样
> `BUILD SUCCESSFUL`；因此"编译通过"这一表述本身不足以作为证据，
> 现改用 `check-compiled-reachability.mjs` 的 A/B/C/D 四判据。
> 详见 `HARMONY_COMPILE_REACHABILITY_GATE.md`。
>
> **2026-09-18 追加（Domain 11 组）**：`harmony/entry/src/main/ets/domain/` 下
> 11 组纯 ArkTS 全部落地并 **COMPILED**
> （Entities / LogicalKey / ImpactKernel / PlanReadiness / ScenarioCoverage /
> StateMachines / GraphRevision / ScenarioTemplate / Timeline / Relations，
> 外加 CanonicalWire 辅助与 DomainSelfCheck 入边）。
> 关键证据：`HARMONY_COMPILE_REACHABILITY = PASS`，**18/18** required 模块
> 全部 reachable + 在 `modules.abc` 中（175,732 B），且 **15 项语义自检标识符
> 可从打包产物反查** —— 这排除了 tree-shaking 造成的假绿。
>
> 但**仍不计入 parity**：`COMPILED` 不等于 `TESTED`。
> Domain 的语义目前由**主机镜像**自检（关系 18/18、容器 5/5），
> 而 conformance 要求的是 **ArkTS 侧真实执行 fixtures 并比对**（§10 的 runner 尚未落地）。
> 把主机镜像结果记成 conformance 通过，正是本项目一再定义的空心信号。
>
> **一处刻意的跨端对齐决定**：`RelationRegistry` **不实现**。
> TS 基准的 `relation-registry.ts` 含 `verificationPolicy` / `impactSemantics`，
> 但 **Android 冻结基准没有这两个字段**；只在 Harmony 侧补上会引入跨端分歧。

> ## D-16 CLOSED（2026-09-17）
>
> 上一轮因 D-16 回退为 `PARTIAL` 的 4 格（Import / Import Mapping / Import Review /
> Restore）已**重新逐格取证**并恢复为 `RUNTIME_VERIFIED`。依据：
>
> - 设备内 `FileWorkflowD16Test` **6/6**（launcher 跨锁存活 / 结果不解锁 /
>   待投递工作流不丢 / Uri 跨锁可读 / 进程死亡保守恢复 / 敏感内容不进 SavedState）
> - 核心行程 E2E **v4**（`core-journey-v4-*`）中每一个外部 picker 节点都独立断言
>   `externalPickerDoesNotBypassLock` 与 `externalPickerDoesNotDestroyPendingWorkflow`
> - Import 端到端真的写进库了（Node Resolution 2 支付方式 / 3 收款对象 → 提交「记录 6 行」）
>
> Android 合计 **62 / 73**（见文末逐格重算）。

> ## ⚠ 计数口径已变更（旧的 `58/62` 作废）
>
> 旧的 `58/62` **无法复现**：本文件的 Android 列实际有 **73** 行能力格，
> `62` 这个分母在文件里找不到出处。本轮重新定义：
>
> - **分母 = 73**（矩阵实际行数）
> - **"已完成" = `TESTED` 及以上**（`CONFORMANCE_PASS` / `RUNTIME_VERIFIED`）
> - `IMPLEMENTED`、`PARTIAL`、`BLOCKED`、`NOT_RUN`、未开始 **均不计入**
>
> 逐格结论见下表；Gate 侧结论见 `ANDROID_N1_N2_FINAL_CLOSURE_REPORT_V2.md`。

---

## 1. 领域 / 语义层（16 格，已完成 **14**）

> 2026-09-17：上一节标题写的是"已完成 13"，但实际表里有 **14** 个
> `CONFORMANCE_PASS`（16 行中只有 Node/Dependency/Group 与 canonical groupKey
> 两行是 `IMPLEMENTED`）。这是上一轮的手工计数误差，本轮逐行重数为 **14**。

| 能力                                           | Android              | Harmony                                                                                                                      | iOS                         |
| ---------------------------------------------- | -------------------- | ---------------------------------------------------------------------------------------------------------------------------- | --------------------------- |
| Canonical 枚举（codegen）                      | **CONFORMANCE_PASS** | IMPLEMENTED（codegen 产物）                                                                                                  | IMPLEMENTED（codegen 产物） |
| Node / Dependency / Group                      | IMPLEMENTED          | IMPLEMENTED_COMPILED_NOT_RUN（Entities.ets：DepNode / Dependency / DependencyGroup，已编译入 HAP；无独立实体级用例故不跳级） | NOT_STARTED                 |
| logical key \`from\|relation\|to\|capability\` | **CONFORMANCE_PASS** | IMPLEMENTED_COMPILED_NOT_RUN（Entities.ets `dependencyLogicalKey` + LogicalKey.ets 已编译入 HAP）                            | NOT_STARTED                 |
| canonical groupKey                             | IMPLEMENTED          | IMPLEMENTED_COMPILED_NOT_RUN（Entities.ets `canonicalGroupKey` 已编译入 HAP；组模式行为已由 host relations 用例覆盖）        | NOT_STARTED                 |
| RelationDefinitionRegistry                     | **CONFORMANCE_PASS** | CONFORMANCE_PASS（host：relations 18/18，真实 ArkTS 执行；生成自 spec 的 CanonicalRelations/CanonicalEnums）                 | NOT_STARTED                 |
| Impact Kernel                                  | **CONFORMANCE_PASS** | CONFORMANCE_PASS（host：impact 13/13，含 cycle-safe / 确定性排序 / required 边丢失 / proposal-only 不产生 must_change）      | NOT_STARTED                 |
| PlanReadiness（三值）                          | **CONFORMANCE_PASS** | CONFORMANCE_PASS（host：readiness 16/16，含 blocked 优先于 review、confidence 不绕过）                                       | NOT_STARTED                 |
| ScenarioCoverage（四级）                       | **CONFORMANCE_PASS** | CONFORMANCE_PASS（host：coverage 6/6，unknown/limited/partial/well_evidenced 四级）                                          | NOT_STARTED                 |
| ChangePlan 状态机                              | **CONFORMANCE_PASS** | CONFORMANCE_PASS（host：state-machine-change-plan，含 PLAN-ACTION-FROZEN / PLAN-ACTION-EXISTS 守卫）                         | NOT_STARTED                 |
| RealityDrift 状态机                            | **CONFORMANCE_PASS** | CONFORMANCE_PASS（host：state-machine-reality-drift，仅 positive evidence / open-only 守卫）                                 | NOT_STARTED                 |
| DiscoveryCandidate 状态机                      | **CONFORMANCE_PASS** | CONFORMANCE_PASS（host：state-machine-discovery-candidate，dismiss 可 reappeal / accept 幂等）                               | NOT_STARTED                 |
| Verification 状态机                            | **CONFORMANCE_PASS** | CONFORMANCE_PASS（host：state-machine-action-verification，evidence_suggested 不自动 verify）                                | NOT_STARTED                 |
| GraphRevision 策略                             | **CONFORMANCE_PASS** | CONFORMANCE_PASS（host：state-machine-graph-revision，mutations 单调 + 非 Reality 事件不 bump）                              | NOT_STARTED                 |
| ScenarioTemplate（3 active）                   | **CONFORMANCE_PASS** | CONFORMANCE_PASS（host：scenario-template-policy，3 active + planned 不可执行界定）                                          | NOT_STARTED                 |
| Timeline 分桶（7 桶）                          | **CONFORMANCE_PASS** | CONFORMANCE_PASS（host：timeline 3/3，7 桶 + 确定性 + terminal 排除）                                                        | NOT_STARTED                 |
| 显式 resolution（readiness）                   | **CONFORMANCE_PASS** | CONFORMANCE_PASS（host：readiness-unresolved-* / stale-dependency 等显式 resolution 行为）                                   | NOT_STARTED                 |

> 未完成的 2 格（Node/Dependency/Group、canonical groupKey）处于 `IMPLEMENTED`：
> 有实现，但没有把"实体/分组"本身作为独立对象做跨端用例级验证，
> 因此**不跳等级**到 `TESTED`。

---

## 2. 持久化与迁移（10 格，已完成 10）

| 能力                        | Android                                                                               | Harmony     | iOS         |
| --------------------------- | ------------------------------------------------------------------------------------- | ----------- | ----------- |
| 逻辑 Schema v3              | **CONFORMANCE_PASS**                                                                  | NOT_STARTED | NOT_STARTED |
| SQLCipher / ArkData 加密库  | **RUNTIME_VERIFIED**（密文库拉出后 `sqlite3` → `file is not a database`；明文不可读） | NOT_STARTED | NOT_STARTED |
| Migration v1→v2→v3          | **CONFORMANCE_PASS**                                                                  | NOT_STARTED | NOT_STARTED |
| 迁移失败回滚（同事务）      | **RUNTIME_VERIFIED**（设备内：失败后不推进版本号、不留半迁移状态）                    | NOT_STARTED | NOT_STARTED |
| Graph payload 导出/导入     | **CONFORMANCE_PASS**                                                                  | NOT_STARTED | NOT_STARTED |
| payload v1/v2 → v3 内存迁移 | **RUNTIME_VERIFIED**（设备内：两种 payload 迁到同一 v3 图）                           | NOT_STARTED | NOT_STARTED |
| 未来版本拒绝（fail closed） | **RUNTIME_VERIFIED**（设备内：未来 `schema_version` 明确拒绝、库不被清空）            | NOT_STARTED | NOT_STARTED |
| Fingerprint 作用域隔离      | **CONFORMANCE_PASS**                                                                  | NOT_STARTED | NOT_STARTED |
| SourceInstance              | **CONFORMANCE_PASS**                                                                  | NOT_STARTED | NOT_STARTED |
| Evidence 多源分流           | **CONFORMANCE_PASS**                                                                  | NOT_STARTED | NOT_STARTED |

> 本轮把 4 格从 `IMPLEMENTED` 升到 `RUNTIME_VERIFIED`：依据是设备内
> `PersistenceEvidenceTest` / `DepmapRuntimeEvidenceTest` 的真实运行时证据，
> 不是"实现了就算"。

---

## 3. 安全 / 密钥 / 认证（10 格，已完成 9）

| 能力                                 | Android                                                                                              | Harmony                                                                                                                                                                                                                                  | iOS         |
| ------------------------------------ | ---------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------- |
| `.depmap` 容器（V1）                 | **CONFORMANCE_PASS**                                                                                 | IMPLEMENTED_COMPILED_NOT_RUN（容器逻辑已实现并真实编译；`depmap-golden-v1` canonical 用例 **DEVICE-BLOCKED**（Argon2 native 需设备），故不升 TESTED）                                                                                    | NOT_STARTED |
| Argon2id + AES-256-GCM               | **CONFORMANCE_PASS**                                                                                 | IMPLEMENTED_COMPILED_NOT_RUN（Argon2id NAPI 已真实绑定：`libpdiargon2.so` 进 HAP（arm64-v8a 40,768 B / x86_64 42,296 B，strip 后 3 动态符号），`NATIVE_BUILD_PASS / ON_DEVICE_NOT_RUN`；AES-256-GCM 走 `cryptoFramework`，执行面需设备） | NOT_STARTED |
| JCS (RFC 8785) 受限域                | **CONFORMANCE_PASS**                                                                                 | CONFORMANCE_PASS（host：jcs-rfc8785-restricted-domain，真实 ArkTS 执行）                                                                                                                                                                 | NOT_STARTED |
| UTF-8 口令不做归一化                 | **CONFORMANCE_PASS**                                                                                 | IMPLEMENTED_COMPILED_NOT_RUN（`util.TextEncoder` 精确 UTF-8 字节；`depmap-utf8-password-normalization` 用例 **DEVICE-BLOCKED**）                                                                                                         | NOT_STARTED |
| 恶意容器 bounds 前置校验             | **CONFORMANCE_PASS**                                                                                 | CONFORMANCE_PASS（host：depmap-bounds-and-structure-rejection 15 分项全 PASS，boundary 先于 KDF）                                                                                                                                        | NOT_STARTED |
| 平台密钥库（Keystore/HUKS/Keychain） | **RUNTIME_VERIFIED**（原始密钥不落盘；prefs 中只有包裹后的值；重启后可重新派生）                     | NOT_STARTED                                                                                                                                                                                                                              | NOT_STARTED |
| 数据库加密                           | **RUNTIME_VERIFIED**（明文 `sqlite3` 读不出来）                                                      | NOT_STARTED                                                                                                                                                                                                                              | NOT_STARTED |
| 生物认证 / App Lock                  | **PARTIAL（App Lock 接线已完成且可独立验证；设备凭据判定本轮修好并取证；生物识别匹配仍受环境限制）** | NOT_STARTED                                                                                                                                                                                                                              | NOT_STARTED |
| 截图保护                             | **RUNTIME_VERIFIED**（6 路由双证据：窗口 flag 含 `SECURE` + `screencap` 被抹黑，非敏感页不受影响）   | NOT_STARTED                                                                                                                                                                                                                              | NOT_STARTED |
| 日志脱敏（release）                  | **RUNTIME_VERIFIED**（按 PID/UID 归属扫描，敏感关键字命中全 0；错误码化）                            | NOT_STARTED                                                                                                                                                                                                                              | NOT_STARTED |

> **生物认证 / App Lock 这一格拆开说**（这是本轮最容易被误读的一格）：
>
> - **App Lock 接线**：`RUNTIME_VERIFIED`。冷启动先锁、解锁后才可进入、
>   前后台回锁、锁定时导航图不参与组合 —— 全部在真机与设备内测试中被验证
>   （`AppLockNavigationTest` 6/6）。
> - **生物识别匹配**：`BLOCKED_BY_RUNTIME_ENVIRONMENT`。本 AVD 的 `config.ini`
>   **没有 `hw.finger`**，且 Android 没有无头指纹录入命令。
> - **设备凭据（PIN）分支**：本轮通过真机设置锁屏 PIN 验证（见 `device_credential_check.py`）。
>
> 因为这两者**同属一格**，该格整体记为 `PARTIAL`，不计入"已完成格"。

---

## 4. 导入 / 解析（7 格，已完成 7）

| 能力                       | Android              | Harmony                                                                                                                | iOS         |
| -------------------------- | -------------------- | ---------------------------------------------------------------------------------------------------------------------- | ----------- |
| WeChat Statement           | **CONFORMANCE_PASS** | CONFORMANCE_PASS（host：parser-wechat 7/7，真实 ArkTS 执行）                                                           | NOT_STARTED |
| Generic CSV                | **CONFORMANCE_PASS** | CONFORMANCE_PASS（host：parser-csv 8/8）                                                                               | NOT_STARTED |
| OFX / QFX                  | **CONFORMANCE_PASS** | CONFORMANCE_PASS（host：parser-ofx 6/6 + parser-qfx 2/2）                                                              | NOT_STARTED |
| BOM / CRLF / CR-only       | **CONFORMANCE_PASS** | CONFORMANCE_PASS（host：parser-csv-crlf / cr-only / wechat-utf8-bom）                                                  | NOT_STARTED |
| GB18030 编码               | **CONFORMANCE_PASS** | CONFORMANCE_PASS（host：parser-csv-gb18030 / wechat-gb18030；码表经 Node ICU + CPython 双复验）                        | NOT_STARTED |
| 借贷列 / 多币种 / 分号分隔 | **CONFORMANCE_PASS** | CONFORMANCE_PASS（host：parser-csv-debit-credit / multi-currency / eu-semicolon / us-signed）                          | NOT_STARTED |
| 坏行保守拒绝               | **CONFORMANCE_PASS** | CONFORMANCE_PASS（host：parser-ofx-malformed / wechat-malformed / ofx-invalid-date / missing-fitid，坏行报错不吞不炸） | NOT_STARTED |

> 28 个原始输入 fixture 已在 `fixtures/import/`，三端共用（§244）。

---

## 5. UI（§63 / §91 / §109）（22 格，已完成 **18**）

| 页面                    | Android                                                                                                                                       | Harmony     | iOS         |
| ----------------------- | --------------------------------------------------------------------------------------------------------------------------------------------- | ----------- | ----------- |
| Onboarding              | IMPLEMENTED（**本轮仍不可达**，见下方说明）                                                                                                   | NOT_STARTED | NOT_STARTED |
| Lock                    | **RUNTIME_VERIFIED**（冷启动 / 前后台 / 无凭据三态均在真机走过）                                                                              | NOT_STARTED | NOT_STARTED |
| Home（回答式首页）      | **RUNTIME_VERIFIED**                                                                                                                          | NOT_STARTED | NOT_STARTED |
| Scenario Center         | **RUNTIME_VERIFIED**（设备内渲染 + 语义门禁）                                                                                                 | NOT_STARTED | NOT_STARTED |
| Scenario Setup          | **RUNTIME_VERIFIED**                                                                                                                          | NOT_STARTED | NOT_STARTED |
| ChangePlan Detail       | **RUNTIME_VERIFIED**                                                                                                                          | NOT_STARTED | NOT_STARTED |
| Timeline                | IMPLEMENTED（本轮未被真机走过）                                                                                                               | NOT_STARTED | NOT_STARTED |
| Pending Review          | **RUNTIME_VERIFIED**                                                                                                                          | NOT_STARTED | NOT_STARTED |
| RealityDrift            | **RUNTIME_VERIFIED**（设备内渲染 + 语义门禁）                                                                                                 | NOT_STARTED | NOT_STARTED |
| Candidate Review        | IMPLEMENTED（本轮未被真机走过；且**无任何入口导航到它**）                                                                                     | NOT_STARTED | NOT_STARTED |
| Infrastructure Overview | **RUNTIME_VERIFIED**                                                                                                                          | NOT_STARTED | NOT_STARTED |
| Graph View（二级）      | IMPLEMENTED（本轮未被真机走过）                                                                                                               | NOT_STARTED | NOT_STARTED |
| Node Detail             | **RUNTIME_VERIFIED**                                                                                                                          | NOT_STARTED | NOT_STARTED |
| Source Management       | **RUNTIME_VERIFIED**                                                                                                                          | NOT_STARTED | NOT_STARTED |
| Import                  | **RUNTIME_VERIFIED**（D-16 已关闭：E2E v4 中 SAF 选真实 CSV → 回锁 → 解锁 → 向导仍在 → Node Resolution → 提交「记录 6 行」）                  | NOT_STARTED | NOT_STARTED |
| Import Mapping          | **RUNTIME_VERIFIED**（同上链路端到端走通，映射在真实写入中生效）                                                                              | NOT_STARTED | NOT_STARTED |
| Import Review           | **RUNTIME_VERIFIED**（Node Resolution 预览在 E2E v4 中真实出现并被提交）                                                                      | NOT_STARTED | NOT_STARTED |
| Backup                  | **RUNTIME_VERIFIED**（导出经 MediaStore 落盘，不经外部文件选择器，因此不受 D-16 影响；E2E v4 中 13,617 B 落盘且 UI 文案一致）                 | NOT_STARTED | NOT_STARTED |
| Restore                 | **RUNTIME_VERIFIED**（D-16 已关闭：SAF 选 .depmap → 回锁 → 解锁 → 工作流恢复 → 重新输入口令 → 显式确认 → 恢复成功；错误密码与篡改容器均被拒） | NOT_STARTED | NOT_STARTED |

> **D-16 —— CLOSED（2026-09-17）**
>
> 缺陷原样：外部文件选择器（DocumentsUI）是**独立任务**，会触发
> `MainActivity.onStop` → `LockGate.lockNow()` → NavHost 离开组合树 →
> 向导里所有 `remember` 状态与 `ActivityResult` 待投递结果一起丢失。
> 用户选完文件回来再解锁，看到的是首页，导入没有发生。
>
> 修复（人工决策 D-16 方案 A，**方案 B/C 明确禁止**）：
>
> 1.  工作流状态提升到 **Activity 作用域** `FileWorkflowCoordinator`
>     （application/presentation workflow state，**不进** Domain Reality Graph）；
> 2.  `ActivityResult` launcher 注册在 `MainActivity.onCreate`（稳定层），
>     不随 NavHost uncompose 注销；
> 3.  文件选择改用 `OpenDocument` + `takePersistableUriPermission(READ)`，
>     只申请读权限，流程结束即归还；
> 4.  **拿到文件绝不解锁、绝不自动提交**：Import 在解锁后于页面中解析，
>     Restore 必须重新输入口令并显式点「开始恢复」；
> 5.  进程死亡保守恢复：意图保留、文件作废（`INTERRUPTED`），绝不半恢复；
> 6.  敏感内容（原始账单 / 口令 / 解密内容）**从不**进 `SavedStateHandle`。
>
> **未采用方案 B/C**，因此 LOCKED 时敏感 NavHost 仍**不参与组合**，
> 且不存在"外部系统 Activity = 绕过重新认证"的例外通道。
>
> `Backup` 本来就不受影响：导出走 MediaStore，不拉起外部 Activity。
> | Settings | **RUNTIME_VERIFIED** | NOT_STARTED | NOT_STARTED |
> | Privacy | **RUNTIME_VERIFIED**（设备内渲染 + 语义门禁） | NOT_STARTED | NOT_STARTED |
> | About | **RUNTIME_VERIFIED**（设备内渲染 + 语义门禁） | NOT_STARTED | NOT_STARTED |

> **两个"写了但不可达"的路由（如实记录，不掩盖）**：
>
> - `Route.ONBOARDING`：已实现、已注册，但**没有任何代码导航到它**。
>   是否接入启动流程属于**产品流程决策**，本轮不改变（任务明确说"Onboarding（如果实际启用）"）。
> - `Route.CANDIDATES`：已实现、已注册、已纳入敏感路由集合，
>   但同样**没有任何入口导航到它**（当前产品流程也不产出 DiscoveryCandidate）。
>
> 这两格因此停在 `IMPLEMENTED` / 未被真机走过，不计入"已完成格"。

---

## 6. 工程 / 发布（8 格，已完成 4）

| 能力                     | Android                                                                                                                                                        | Harmony                                                                                                                                                  | iOS                   |
| ------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------- |
| 真实 Build               | **RUNTIME_VERIFIED**（`assembleDebug` / `assembleRelease` / `bundleRelease` 全部实跑；非生产签名链路已验证）                                                   | IMPLEMENTED（2026-09-25 clean 重建 `BUILD SUCCESSFUL in 1 min 3 s`，HAP `entry-default-unsigned.hap` 3,380,659 B；**未签名**、未安装到设备）             | **BLOCKED_BY_MACOS**  |
| 单测 / 集成测试          | **TESTED**（本轮 **222** 个可执行用例 = 71 `:core` JVM + 91 conformance + **9** `:app` JVM（本轮新增，此前 NO-SOURCE）+ **51** 设备内 androidTest）            | TESTED（host 本地 ArkTS 单测：hvigor test **91 checks** = 87 canonical + 3 conformance 元测试 + 1 domain 自检，0 fail，2026-09-25 新鲜；设备面 NOT_RUN） | NOT_STARTED           |
| 设备 E2E                 | **RUNTIME_VERIFIED**（核心行程 **v4** 全新 run：36 PASS / 1 FAIL，崩溃 0；每个外部 picker 节点都跑 D-16 双断言）                                               | NOT_RUN                                                                                                                                                  | NOT_RUN               |
| 性能 smoke               | **TESTED**（10,000 行 CSV 全解析、0 错误，强断言通过；旧数字已作废）                                                                                           | NOT_RUN                                                                                                                                                  | NOT_RUN               |
| 无障碍                   | **PARTIAL**（Compose 语义树口径：14 屏 0 个无标签可交互节点；触摸目标/焦点顺序/字体缩放/横屏 PASS；**TalkBack 实机读屏 NOT_RUN** —— 镜像未预装且无 Play 商店） | NOT_STARTED                                                                                                                                              | NOT_STARTED           |
| Dark Mode（token ready） | IMPLEMENTED（`spec/ui/design-tokens.json` 含 dark 覆盖，未做设备级验证）                                                                                       | 同上                                                                                                                                                     | 同上                  |
| Release 签名             | **BLOCKED_BY_MISSING_PRODUCTION_KEYSTORE**（流水线本身已验证可用：非生产密钥签名后 `apksigner verify` 通过；但**没有生产 keystore**）                          | BLOCKED（AGC）                                                                                                                                           | BLOCKED（Apple 账号） |
| Store metadata           | **PARTIAL**（`ANDROID_STORE_METADATA.md` 文案草稿；截图 / 图标 / 公开隐私政策链接 NOT_STARTED）                                                                | NOT_STARTED                                                                                                                                              | NOT_STARTED           |

> **不单独占格的项**：
>
> - `ANDROID_GRADLE_WRAPPER` = **PASS**（仓库内标准 Wrapper，Gradle 8.9，官方 `distributionUrl`）。
> - `ANDROID_BUILD_REPRODUCIBLE` = **PASS**（本轮新增验证：不带任何环境变量/仓库外 init script
>   直接 `./gradlew` 即可构建；非 ASCII 路径与临时目录重定向已内置在 `settings.gradle.kts`）。
>   这两项属于"真实 Build"这一格的可复现前提，不是 parity 能力，因此**不计入 73 格**。

---

## 汇总（2026-09-21 Android Product Finalization 逐格重审）

| 平台    | 已完成格    | 总格   | 说明                                                                                                                                                                                                                                                                                                                                                                                                                                              |
| ------- | ----------- | ------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Android | **69 / 73** | **73** | 逐格重审（**非 62+7 推算**）：本轮把「生物识别 / App Lock」与「Dark Mode」两格升为 RUNTIME_VERIFIED、Onboarding 按产品决策移除（D-9）、Node/Dependency/Group 与 canonical groupKey 补实体级 conformance/设备证据升 CONFORMANCE_PASS。明细见 `ANDROID_FINAL_73_AUDIT.md`                                                                                                                                                                           |
| Harmony | **22**      | 73     | 2026-09-25 N3 恢复轮：host conformance **87 canonical 在真实 ArkTS 运行时执行 0 fail** → §1 12 格 + §3 2 格 + §4 7 格 = `CONFORMANCE_PASS`（host 执行面），§6 「单测/集成测试」1 格 = `TESTED`；另有 3 格 §1（Node/DG、logical key、groupKey）+ 3 格 §3（容器/Argon2+AES/UTF-8）保持 `IMPLEMENTED_COMPILED_NOT_RUN`。**RUNTIME 类一格未动**（`RUNTIME_E2E = RUNTIME_NOT_RUN`，华为账号+镜像 E-9）；UI 层与 ArkTS 持久化层仍 NOT_STARTED（无源码） |
| iOS     | 0           | 73     | 仅 codegen 产物；build BLOCKED_BY_MACOS（PAUSED / NOT_STARTED）                                                                                                                                                                                                                                                                                                                                                                                   |

### Harmony 22/73 的来源（2026-09-25，逐格以本轮新鲜证据归位，无证据不升级）

| 节                    | 格数   | 已完成 | 说明                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| --------------------- | ------ | ------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1. 领域 / 语义层      | 16     | **12** | `CONFORMANCE_PASS`（host 执行面）：RelationDefinitionRegistry（relations 18/18）、Impact Kernel（impact 13/13）、PlanReadiness 含显式 resolution（readiness 16/16）、ScenarioCoverage（coverage 6/6）、ChangePlan / RealityDrift / DiscoveryCandidate / Verification 四状态机 + GraphRevision（state-machine 5/5）、ScenarioTemplate（scenario-template-policy）、Timeline（timeline 3/3）。另 3 格 `IMPLEMENTED_COMPILED_NOT_RUN`：Node/Dependency/Group、logical key、canonical groupKey（Entities.ets 已编译入 HAP；无独立用例故不跳级）。 |
| 2. 持久化与迁移       | 10     | 0      | 无 ArkTS repository/persistence 层（`check-compiled-reachability`：data/Repository.ets = NOT_IMPLEMENTED）。migration-version-contract 等 host PASS 用例验证的是 harness 内契约而非平台持久化，**不计入任何格**。                                                                                                                                                                                                                                                                                                                             |
| 3. 安全 / 密钥 / 认证 | 10     | **2**  | JCS（jcs-rfc8785-restricted-domain）、恶意容器 bounds（depmap-bounds-and-structure-rejection）→ `CONFORMANCE_PASS`（host）。`.depmap` 容器 / Argon2id+AES-256-GCM / UTF-8 口令不做归一化 保持 `IMPLEMENTED_COMPILED_NOT_RUN`（golden/utf8 两条 canonical 均 DEVICE-BLOCKED；Argon2 NAPI 已真实绑定，执行面需设备）。密钥库 / 库加密 / 生物认证 / 截图保护 / 日志脱敏 = NOT_STARTED（无 ArkTS security 层）。                                                                                                                                  |
| 4. 导入 / 解析        | 7      | **7**  | parser 22/22 host PASS：WeChat 7 / CSV 8 / OFX 6 / QFX 2 / GB18030 2 / 编码规范（BOM·CRLF·CR-only）全部覆盖，含 malformed-ofx、bad-line 等坏行保守拒绝用例。                                                                                                                                                                                                                                                                                                                                                                                  |
| 5. UI                 | 22     | 0      | 仅 `pages/Index.ets` 单页；产品级页面未实现（NOT_STARTED，无虚假进度）。                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| 6. 工程 / 发布        | 8      | **1**  | 「单测 / 集成测试」→ `TESTED`（hvigor test：**91 checks** = 87 canonical + 3 conformance 元测试 + 1 domain 自检，0 fail）。真实 Build 保持 `IMPLEMENTED`（本轮 fresh：67s BUILD SUCCESSFUL，HAP 3,380,659 B，sha256 见 NATIVE_MIGRATION_STATUS.md）。设备 E2E / 性能 smoke = NOT_RUN；无障碍 NOT_STARTED；Release 签名 BLOCKED（AGC）；Store metadata NOT_STARTED。                                                                                                                                                                           |
| **合计**              | **73** | **22** | 计数口径不变：`TESTED` 及以上（CONFORMANCE_PASS / RUNTIME_VERIFIED / TESTED）才计「已完成」；`IMPLEMENTED_COMPILED_NOT_RUN` 不计。`RUNTIME_VERIFIED` 本轮 **0** 格（设备面未动）。                                                                                                                                                                                                                                                                                                                                                            |

### 69 的来源（逐节重算，不做 `62 + 完成项` 这类推算）

| 节                    | 格数   | 已完成 | 说明                                                                                                                                                          |
| --------------------- | ------ | ------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1. 领域 / 语义层      | 16     | **16** | Node/Dependency/Group、canonical groupKey 本轮补 conformance + 设备实体层证据（CandidateDriftEvidenceTest 7/7）→ CONFORMANCE_PASS                             |
| 2. 持久化与迁移       | 10     | **10** |                                                                                                                                                               |
| 3. 安全 / 密钥 / 认证 | 10     | **10** | 「生物认证 / App Lock」本轮在指纹 AVD（API35 `hw.fingerprint=yes`）跑通指纹成功/失败/取消 + PIN 正确/错误 → **RUNTIME_VERIFIED**                              |
| 4. 导入 / 解析        | 7      | **7**  |                                                                                                                                                               |
| 5. UI                 | 22     | **21** | Onboarding 按产品决策移除（D-9）；Timeline / Candidate Review / Graph 本轮不再为缺口（首页聚合 + 入口 + 语义门禁）；Drift/Candidate 四选一与确认/忽略设备验证 |
| 6. 工程 / 发布        | 8      | **5**  | 本轮：Dark Mode 设备像素验证 → RUNTIME_VERIFIED（由 IMPLEMENTED 升级）、真实 Build/单测/E2E/性能 四格保持；新增 DeleteAllData/L-37 设备测试                   |
| **合计**              | **73** | **69** |                                                                                                                                                               |

### 仍未完成的 4 格（逐格列出，不做合并、不隐藏）

| 格                                   | 状态                                              | 性质                                               |
| ------------------------------------ | ------------------------------------------------- | -------------------------------------------------- |
| 无障碍（TalkBack 实机读屏）          | PARTIAL（语义树 14/14 PASS；TalkBack NOT_RUN）    | RUNTIME_ENVIRONMENT_GAP（AVD 无 Play 商店/需真机） |
| Release 签名                         | BLOCKED_BY_MISSING_PRODUCTION_KEYSTORE            | RELEASE_EXTERNAL_BLOCKER（用户提供 keystore）      |
| Store metadata（截图/图标/公开 URL） | PARTIAL（文案完成）                               | STORE_PREPARATION（需最终品牌决策）                |
| R8 / minify                          | NOT_APPLICABLE（release 未开 minify，如实不伪报） | 工程项（当前不适用，开启后需重验）                 |

Android 侧 27 个 Gate 的逐项结论见 `ANDROID_N1_N2_FINAL_CLOSURE_REPORT_V2.md`。
