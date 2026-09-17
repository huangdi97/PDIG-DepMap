# NATIVE_PARITY_MATRIX.md

> 每格：`NOT_STARTED` → `IMPLEMENTED` → `TESTED` → `CONFORMANCE_PASS` → `RUNTIME_VERIFIED`
> 写状态时不得跳过等级，也不得把"理论支持"写成"已验证"。

更新时间：2026-09-17（Android Final Blocker Closure 轮，**重新计算**）
> 本轮相对 2026-09-16 的变更：导入相关 4 格由 `RUNTIME_VERIFIED` 回退为 `PARTIAL`
> （D-16：外部文件选择器触发回锁 → 向导状态与 ActivityResult 一起丢失），
> 安全格补上设备凭据判定的取证，Android 合计 **56 / 73**。

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

## 1. 领域 / 语义层（16 格，已完成 13）

| 能力                       | Android | Harmony | iOS |
| -------------------------- | ------- | ------- | --- |
| Canonical 枚举（codegen）  | **CONFORMANCE_PASS** | IMPLEMENTED（codegen 产物） | IMPLEMENTED（codegen 产物） |
| Node / Dependency / Group  | IMPLEMENTED | NOT_STARTED | NOT_STARTED |
| logical key `from\\|relation\\|to\\|capability` | **CONFORMANCE_PASS** | NOT_STARTED | NOT_STARTED |
| canonical groupKey         | IMPLEMENTED | NOT_STARTED | NOT_STARTED |
| RelationDefinitionRegistry | **CONFORMANCE_PASS** | NOT_STARTED | NOT_STARTED |
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
| `.depmap` 容器（V1）      | **CONFORMANCE_PASS** | NOT_STARTED | NOT_STARTED |
| Argon2id + AES-256-GCM    | **CONFORMANCE_PASS** | NOT_STARTED | NOT_STARTED |
| JCS (RFC 8785) 受限域     | **CONFORMANCE_PASS** | NOT_STARTED | NOT_STARTED |
| UTF-8 口令不做归一化       | **CONFORMANCE_PASS** | NOT_STARTED | NOT_STARTED |
| 恶意容器 bounds 前置校验   | **CONFORMANCE_PASS** | NOT_STARTED | NOT_STARTED |
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

## 5. UI（§63 / §91 / §109）（22 格，已完成 14）

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
| Import | **PARTIAL**（页面与选择在真机走过；**端到端完成不了**，见下方 D-16 说明） | NOT_STARTED | NOT_STARTED |
| Import Mapping | **PARTIAL**（显式映射在链路中生效；终态受 D-16 影响无法走完） | NOT_STARTED | NOT_STARTED |
| Import Review | **PARTIAL**（Node Resolution 预览在 App Lock 接线前已真机走过；**本轮重跑拿不到**） | NOT_STARTED | NOT_STARTED |
| Backup | **RUNTIME_VERIFIED**（导出经 MediaStore 落盘，不经外部文件选择器，因此不受 D-16 影响） | NOT_STARTED | NOT_STARTED |
| Restore | **PARTIAL**（恢复要经 SAF 选文件，与 Import 同受 D-16 影响） | NOT_STARTED | NOT_STARTED |

> **D-16（2026-09-17 实测）**：外部文件选择器（DocumentsUI）是**独立任务**，
> 会触发 `MainActivity.onStop` → `LockGate.lockNow()` → NavHost 离开组合树 →
> 向导里所有 `remember` 状态与 ActivityResult 待投递结果一起丢失。
> 用户选完文件回来再解锁，看到的是首页，导入没有发生（对象数仍为 0）。
> 因此 Import / Import Mapping / Import Review / Restore 四格**不能继续写 RUNTIME_VERIFIED**。
> `Backup` 不受影响：导出走 MediaStore，不拉起外部 Activity。
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

## 6. 工程 / 发布（8 格，已完成 3）

| 能力                | Android | Harmony | iOS |
| ------------------- | ------- | ------- | --- |
| 真实 Build | **RUNTIME_VERIFIED**（`assembleDebug` / `assembleRelease` / `bundleRelease` 全部实跑；非生产签名链路已验证） | NOT_STARTED | **BLOCKED_BY_MACOS** |
| 单测 / 集成测试      | **TESTED**（本轮 206 个可执行用例 = 71 `:core` JVM + 91 conformance + 44 设备内 androidTest） | NOT_STARTED | NOT_STARTED |
| 设备 E2E            | {{E2E_CELL}} | NOT_RUN | NOT_RUN |
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
| Android | **56 / 73**   | **73** | 分母与计数口径已重新定义（旧的 58/62 作废） |
| Harmony | 0        | 73   | 仅 codegen 产物                        |
| iOS     | 0        | 73   | 仅 codegen 产物；build BLOCKED_BY_MACOS |

Android 侧 27 个 Gate 的逐项结论见 `ANDROID_N1_N2_FINAL_CLOSURE_REPORT_V2.md`。
