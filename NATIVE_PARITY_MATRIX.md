# NATIVE_PARITY_MATRIX.md

> 每格：`NOT_STARTED` → `IMPLEMENTED` → `TESTED` → `CONFORMANCE_PASS` → `RUNTIME_VERIFIED`
> 写状态时不得跳过等级，也不得把"理论支持"写成"已验证"。

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

| 能力                       | Android | Harmony | iOS |
| -------------------------- | ------- | ------- | --- |
| Canonical 枚举（codegen）  | **CONFORMANCE_PASS** | IMPLEMENTED（codegen 产物） | IMPLEMENTED（codegen 产物） |
| Node / Dependency / Group  | IMPLEMENTED | NOT_STARTED | NOT_STARTED |
| logical key `from\\|relation\\|to\\|capability` | **CONFORMANCE_PASS** | NOT_STARTED | NOT_STARTED |
| canonical groupKey         | IMPLEMENTED | NOT_STARTED | NOT_STARTED |
| RelationDefinitionRegistry | **CONFORMANCE_PASS** | IMPLEMENTED（`entry/src/main/ets/domain/Relations.ets`，已编译进 HAP；未做用例级验证） | NOT_STARTED |
| Impact Kernel              | **CONFORMANCE_PASS** | NOT_STARTED | NOT_STARTED |
| PlanReadiness（三值）      | **CONFORMANCE_PASS** | NOT_STARTED | NOT_STARTED |
| ScenarioCoverage（四级）   | **CONFORMANCE_PASS** | NOT_STARTED | NOT_STARTED |
| ChangePlan 状态机          | **CONFORMANCE_PASS** | NOT_STARTED | NOT_STARTED |
| RealityDrift 状态机        | **CONFORMANCE_PASS** | NOT_STARTED | NOT_STARTED |
| DiscoveryCandidate 状态机  | **CONFORMANCE_PASS** | NOT_STARTED | NOT_STARTED |
| Verification 状态机        | **CONFORMANCE_PASS** | NOT_STARTED | NOT_STARTED |
| GraphRevision 策略         | **CONFORMANCE_PASS** | NOT_STARTED | NOT_STARTED |
| ScenarioTemplate（3 active）| **CONFORMANCE_PASS** | NOT_STARTED | NOT_STARTED |
| Timeline 分桶（7 桶） | **CONFORMANCE_PASS** | NOT_STARTED | NOT_STARTED |
| 显式 resolution（readiness）| **CONFORMANCE_PASS** | NOT_STARTED | NOT_STARTED |

> 未完成的 2 格（Node/Dependency/Group、canonical groupKey）处于 `IMPLEMENTED`：
> 有实现，但没有把"实体/分组"本身作为独立对象做跨端用例级验证，
> 因此**不跳等级**到 `TESTED`。

---

## 2. 持久化与迁移（10 格，已完成 10）

| 能力                        | Android | Harmony | iOS |
| --------------------------- | ------- | ------- | --- |
| 逻辑 Schema v3 | **CONFORMANCE_PASS** | NOT_STARTED | NOT_STARTED |
| SQLCipher / ArkData 加密库 | **RUNTIME_VERIFIED**（密文库拉出后 `sqlite3` → `file is not a database`；明文不可读） | NOT_STARTED | NOT_STARTED |
| Migration v1→v2→v3 | **CONFORMANCE_PASS** | NOT_STARTED | NOT_STARTED |
| 迁移失败回滚（同事务） | **RUNTIME_VERIFIED**（设备内：失败后不推进版本号、不留半迁移状态） | NOT_STARTED | NOT_STARTED |
| Graph payload 导出/导入 | **CONFORMANCE_PASS** | NOT_STARTED | NOT_STARTED |
| payload v1/v2 → v3 内存迁移 | **RUNTIME_VERIFIED**（设备内：两种 payload 迁到同一 v3 图） | NOT_STARTED | NOT_STARTED |
| 未来版本拒绝（fail closed）  | **RUNTIME_VERIFIED**（设备内：未来 `schema_version` 明确拒绝、库不被清空） | NOT_STARTED | NOT_STARTED |
| Fingerprint 作用域隔离 | **CONFORMANCE_PASS** | NOT_STARTED | NOT_STARTED |
| SourceInstance | **CONFORMANCE_PASS** | NOT_STARTED | NOT_STARTED |
| Evidence 多源分流 | **CONFORMANCE_PASS** | NOT_STARTED | NOT_STARTED |

> 本轮把 4 格从 `IMPLEMENTED` 升到 `RUNTIME_VERIFIED`：依据是设备内
> `PersistenceEvidenceTest` / `DepmapRuntimeEvidenceTest` 的真实运行时证据，
> 不是"实现了就算"。

---

## 3. 安全 / 密钥 / 认证（10 格，已完成 9）

| 能力                     | Android | Harmony | iOS |
| ------------------------ | ------- | ------- | --- |
| `.depmap` 容器（V1）      | **CONFORMANCE_PASS** | IMPLEMENTED_COMPILED_NOT_RUN（容器逻辑已实现并真实编译；未达 TESTED，**不计入** 0/73） | NOT_STARTED |
| Argon2id + AES-256-GCM    | **CONFORMANCE_PASS** | IMPLEMENTED_COMPILED_NOT_RUN（AES-256-GCM 走 `cryptoFramework`，规格经主机黄金校验 5/5；Argon2id **未绑定**） | NOT_STARTED |
| JCS (RFC 8785) 受限域     | **CONFORMANCE_PASS** | IMPLEMENTED_COMPILED_NOT_RUN（`Jcs.ets` 已实现并真实编译；设备自检 `ContainerSelfCheck` 就绪但未执行） | NOT_STARTED |
| UTF-8 口令不做归一化       | **CONFORMANCE_PASS** | IMPLEMENTED_COMPILED_NOT_RUN（`util.TextEncoder` 精确 UTF-8 字节；未达 TESTED） | NOT_STARTED |
| 恶意容器 bounds 前置校验   | **CONFORMANCE_PASS** | IMPLEMENTED_COMPILED_NOT_RUN（`validateDepmapBounds` 全项先于 KDF；未达 TESTED） | NOT_STARTED |
| 平台密钥库（Keystore/HUKS/Keychain） | **RUNTIME_VERIFIED**（原始密钥不落盘；prefs 中只有包裹后的值；重启后可重新派生） | NOT_STARTED | NOT_STARTED |
| 数据库加密 | **RUNTIME_VERIFIED**（明文 `sqlite3` 读不出来） | NOT_STARTED | NOT_STARTED |
| 生物认证 / App Lock | **PARTIAL（App Lock 接线已完成且可独立验证；设备凭据判定本轮修好并取证；生物识别匹配仍受环境限制）** | NOT_STARTED | NOT_STARTED |
| 截图保护                   | **RUNTIME_VERIFIED**（6 路由双证据：窗口 flag 含 `SECURE` + `screencap` 被抹黑，非敏感页不受影响） | NOT_STARTED | NOT_STARTED |
| 日志脱敏（release） | **RUNTIME_VERIFIED**（按 PID/UID 归属扫描，敏感关键字命中全 0；错误码化） | NOT_STARTED | NOT_STARTED |

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

| 能力                | Android | Harmony | iOS |
| ------------------- | ------- | ------- | --- |
| WeChat Statement | **CONFORMANCE_PASS** | NOT_STARTED | NOT_STARTED |
| Generic CSV | **CONFORMANCE_PASS** | NOT_STARTED | NOT_STARTED |
| OFX / QFX | **CONFORMANCE_PASS** | NOT_STARTED | NOT_STARTED |
| BOM / CRLF / CR-only | **CONFORMANCE_PASS** | NOT_STARTED | NOT_STARTED |
| GB18030 编码 | **CONFORMANCE_PASS** | NOT_STARTED | NOT_STARTED |
| 借贷列 / 多币种 / 分号分隔 | **CONFORMANCE_PASS** | NOT_STARTED | NOT_STARTED |
| 坏行保守拒绝 | **CONFORMANCE_PASS** | NOT_STARTED | NOT_STARTED |

> 28 个原始输入 fixture 已在 `fixtures/import/`，三端共用（§244）。

---

## 5. UI（§63 / §91 / §109）（22 格，已完成 **18**）

| 页面                     | Android | Harmony | iOS |
| ------------------------ | ------- | ------- | --- |
| Onboarding | IMPLEMENTED（**本轮仍不可达**，见下方说明） | NOT_STARTED | NOT_STARTED |
| Lock | **RUNTIME_VERIFIED**（冷启动 / 前后台 / 无凭据三态均在真机走过） | NOT_STARTED | NOT_STARTED |
| Home（回答式首页） | **RUNTIME_VERIFIED** | NOT_STARTED | NOT_STARTED |
| Scenario Center | **RUNTIME_VERIFIED**（设备内渲染 + 语义门禁） | NOT_STARTED | NOT_STARTED |
| Scenario Setup | **RUNTIME_VERIFIED** | NOT_STARTED | NOT_STARTED |
| ChangePlan Detail | **RUNTIME_VERIFIED** | NOT_STARTED | NOT_STARTED |
| Timeline | IMPLEMENTED（本轮未被真机走过） | NOT_STARTED | NOT_STARTED |
| Pending Review | **RUNTIME_VERIFIED** | NOT_STARTED | NOT_STARTED |
| RealityDrift | **RUNTIME_VERIFIED**（设备内渲染 + 语义门禁） | NOT_STARTED | NOT_STARTED |
| Candidate Review | IMPLEMENTED（本轮未被真机走过；且**无任何入口导航到它**） | NOT_STARTED | NOT_STARTED |
| Infrastructure Overview | **RUNTIME_VERIFIED** | NOT_STARTED | NOT_STARTED |
| Graph View（二级） | IMPLEMENTED（本轮未被真机走过） | NOT_STARTED | NOT_STARTED |
| Node Detail | **RUNTIME_VERIFIED** | NOT_STARTED | NOT_STARTED |
| Source Management | **RUNTIME_VERIFIED** | NOT_STARTED | NOT_STARTED |
| Import | **RUNTIME_VERIFIED**（D-16 已关闭：E2E v4 中 SAF 选真实 CSV → 回锁 → 解锁 → 向导仍在 → Node Resolution → 提交「记录 6 行」） | NOT_STARTED | NOT_STARTED |
| Import Mapping | **RUNTIME_VERIFIED**（同上链路端到端走通，映射在真实写入中生效） | NOT_STARTED | NOT_STARTED |
| Import Review | **RUNTIME_VERIFIED**（Node Resolution 预览在 E2E v4 中真实出现并被提交） | NOT_STARTED | NOT_STARTED |
| Backup | **RUNTIME_VERIFIED**（导出经 MediaStore 落盘，不经外部文件选择器，因此不受 D-16 影响；E2E v4 中 13,617 B 落盘且 UI 文案一致） | NOT_STARTED | NOT_STARTED |
| Restore | **RUNTIME_VERIFIED**（D-16 已关闭：SAF 选 .depmap → 回锁 → 解锁 → 工作流恢复 → 重新输入口令 → 显式确认 → 恢复成功；错误密码与篡改容器均被拒） | NOT_STARTED | NOT_STARTED |

> **D-16 —— CLOSED（2026-09-17）**
>
> 缺陷原样：外部文件选择器（DocumentsUI）是**独立任务**，会触发
> `MainActivity.onStop` → `LockGate.lockNow()` → NavHost 离开组合树 →
> 向导里所有 `remember` 状态与 `ActivityResult` 待投递结果一起丢失。
> 用户选完文件回来再解锁，看到的是首页，导入没有发生。
>
> 修复（人工决策 D-16 方案 A，**方案 B/C 明确禁止**）：
>
>  1. 工作流状态提升到 **Activity 作用域** `FileWorkflowCoordinator`
>     （application/presentation workflow state，**不进** Domain Reality Graph）；
>  2. `ActivityResult` launcher 注册在 `MainActivity.onCreate`（稳定层），
>     不随 NavHost uncompose 注销；
>  3. 文件选择改用 `OpenDocument` + `takePersistableUriPermission(READ)`，
>     只申请读权限，流程结束即归还；
>  4. **拿到文件绝不解锁、绝不自动提交**：Import 在解锁后于页面中解析，
>     Restore 必须重新输入口令并显式点「开始恢复」；
>  5. 进程死亡保守恢复：意图保留、文件作废（`INTERRUPTED`），绝不半恢复；
>  6. 敏感内容（原始账单 / 口令 / 解密内容）**从不**进 `SavedStateHandle`。
>
> **未采用方案 B/C**，因此 LOCKED 时敏感 NavHost 仍**不参与组合**，
> 且不存在"外部系统 Activity = 绕过重新认证"的例外通道。
>
> `Backup` 本来就不受影响：导出走 MediaStore，不拉起外部 Activity。
| Settings | **RUNTIME_VERIFIED** | NOT_STARTED | NOT_STARTED |
| Privacy | **RUNTIME_VERIFIED**（设备内渲染 + 语义门禁） | NOT_STARTED | NOT_STARTED |
| About | **RUNTIME_VERIFIED**（设备内渲染 + 语义门禁） | NOT_STARTED | NOT_STARTED |

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

| 能力                | Android | Harmony | iOS |
| ------------------- | ------- | ------- | --- |
| 真实 Build | **RUNTIME_VERIFIED**（`assembleDebug` / `assembleRelease` / `bundleRelease` 全部实跑；非生产签名链路已验证） | IMPLEMENTED（hvigor 全清重建 `BUILD SUCCESSFUL`，产出 `entry-default-unsigned.hap` 60,133 B；**未签名**、未安装到设备） | **BLOCKED_BY_MACOS** |
| 单测 / 集成测试      | **TESTED**（本轮 **222** 个可执行用例 = 71 `:core` JVM + 91 conformance + **9** `:app` JVM（本轮新增，此前 NO-SOURCE）+ **51** 设备内 androidTest） | NOT_STARTED | NOT_STARTED |
| 设备 E2E            | **RUNTIME_VERIFIED**（核心行程 **v4** 全新 run：36 PASS / 1 FAIL，崩溃 0；每个外部 picker 节点都跑 D-16 双断言） | NOT_RUN | NOT_RUN |
| 性能 smoke          | **TESTED**（10,000 行 CSV 全解析、0 错误，强断言通过；旧数字已作废） | NOT_RUN | NOT_RUN |
| 无障碍 | **PARTIAL**（Compose 语义树口径：14 屏 0 个无标签可交互节点；触摸目标/焦点顺序/字体缩放/横屏 PASS；**TalkBack 实机读屏 NOT_RUN** —— 镜像未预装且无 Play 商店） | NOT_STARTED | NOT_STARTED |
| Dark Mode（token ready） | IMPLEMENTED（`spec/ui/design-tokens.json` 含 dark 覆盖，未做设备级验证） | 同上 | 同上 |
| Release 签名        | **BLOCKED_BY_MISSING_PRODUCTION_KEYSTORE**（流水线本身已验证可用：非生产密钥签名后 `apksigner verify` 通过；但**没有生产 keystore**） | BLOCKED（AGC） | BLOCKED（Apple 账号） |
| Store metadata      | **PARTIAL**（`ANDROID_STORE_METADATA.md` 文案草稿；截图 / 图标 / 公开隐私政策链接 NOT_STARTED） | NOT_STARTED | NOT_STARTED |

> **不单独占格的项**：
> - `ANDROID_GRADLE_WRAPPER` = **PASS**（仓库内标准 Wrapper，Gradle 8.9，官方 `distributionUrl`）。
> - `ANDROID_BUILD_REPRODUCIBLE` = **PASS**（本轮新增验证：不带任何环境变量/仓库外 init script
>   直接 `./gradlew` 即可构建；非 ASCII 路径与临时目录重定向已内置在 `settings.gradle.kts`）。
> 这两项属于"真实 Build"这一格的可复现前提，不是 parity 能力，因此**不计入 73 格**。

---

## 汇总

| 平台   | 已完成格 | 总格 | 说明                                  |
| ------ | -------- | ---- | ------------------------------------- |
| Android | **62 / 73**   | **73** | 分母与计数口径已重新定义（旧的 58/62 作废）；上一轮记录为 56，本轮 +6 |
| Harmony | **0**    | 73   | N3 已开工：工程可构建、Relations Domain 已编译；但均未达 `TESTED`，故仍 0。Conformance NOT_RUN、Runtime NOT_RUN、depmap BLOCKED |
| iOS     | 0        | 73   | 仅 codegen 产物；build BLOCKED_BY_MACOS |

### 62 的来源（逐节重算，不做 `56 + 4 = 60` 这类推算）

| 节 | 格数 | 已完成 | 说明 |
| -- | ---- | ------ | ---- |
| 1. 领域 / 语义层 | 16 | **14** | 两格 `IMPLEMENTED`（Node/Dependency/Group、canonical groupKey）不计；上轮标题误写 13，本轮逐行重数为 14 |
| 2. 持久化与迁移 | 10 | **10** | |
| 3. 安全 / 密钥 / 认证 | 10 | **9** | 「生物认证 / App Lock」整格记 `PARTIAL`：App Lock 接线 RUNTIME_VERIFIED，但生物识别 `BLOCKED_BY_RUNTIME_ENVIRONMENT` |
| 4. 导入 / 解析 | 7 | **7** | |
| 5. UI | 22 | **18** | D-16 关闭后 Import / Import Mapping / Import Review / Restore 由 PARTIAL 升为 RUNTIME_VERIFIED（14 → 18） |
| 6. 工程 / 发布 | 8 | **4** | 真实 Build、单测/集成、设备 E2E、性能 smoke；无障碍 PARTIAL、Dark Mode IMPLEMENTED、Release 签名 BLOCKED、Store metadata PARTIAL |
| **合计** | **73** | **62** | |

### 仍未完成的 11 格（逐格列出，不做合并、不隐藏）

| 格 | 状态 | 性质 |
| -- | ---- | ---- |
| Node / Dependency / Group | IMPLEMENTED | ENGINEERING_PARITY |
| canonical groupKey | IMPLEMENTED | ENGINEERING_PARITY |
| 生物认证 / App Lock | PARTIAL | ENGINEERING_PARITY（接线 PASS）+ RUNTIME_ENVIRONMENT_BLOCKED（生物识别） |
| Onboarding | IMPLEMENTED（无入口可达） | ENGINEERING_PARITY |
| Timeline | IMPLEMENTED（未被真机走过） | ENGINEERING_PARITY |
| Candidate Review | IMPLEMENTED（无入口可达） | ENGINEERING_PARITY |
| Graph View（二级） | IMPLEMENTED（未被真机走过） | ENGINEERING_PARITY |
| 无障碍 | PARTIAL（TalkBack NOT_RUN） | RUNTIME_ENVIRONMENT_BLOCKED |
| Dark Mode（token ready） | IMPLEMENTED | ENGINEERING_PARITY |
| Release 签名 | BLOCKED_BY_MISSING_PRODUCTION_KEYSTORE | RELEASE_READINESS |
| Store metadata | PARTIAL（截图/图标/公开隐私政策 NOT_STARTED） | RELEASE_READINESS |

Android 侧 27 个 Gate 的逐项结论见 `ANDROID_N1_N2_FINAL_CLOSURE_REPORT_V2.md`。
