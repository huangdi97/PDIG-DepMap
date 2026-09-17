# GOAL_PDIG_NATIVE_MIGRATION.md

> 项目：**PDIG — Personal Digital Infrastructure Graph**（个人数字基础设施图谱）
> 内部旧代号：DepMap
> 本文件是 **PDIG Native Migration** 的总纲与唯一执行顺序。

## 0. 最终技术决策（已冻结）

**彻底退出 uni-app x / UTS / DCloud Production 技术栈，切换为三端原生。**

| 平台      | 语言 / UI         | 导航                  | 并发            | 存储                | 密钥       | 认证                  |
| --------- | ----------------- | --------------------- | --------------- | ------------------- | ---------- | --------------------- |
| Android   | Kotlin / Compose  | Navigation Compose    | Coroutines/Flow | SQLite / SQLCipher  | Keystore   | BiometricPrompt       |
| iOS       | Swift / SwiftUI   | NavigationStack       | async/await     | SQLite / SQLCipher  | Keychain   | LocalAuthentication   |
| HarmonyOS | ArkTS / ArkUI     | Stage Model 导航      | ArkTS 并发      | ArkData relationalStore | HUKS   | Harmony 用户认证      |

**Production 中禁止依赖**：uni-app、uni-app x、`.uvue` runtime、UTS runtime、
HBuilderX build pipeline、DCloud Cloud Build、DCloud Native SDK、DCloud 登录、
DCloud 云打包、DCloud 运行时。

**旧实现保留为 `LEGACY_REFERENCE / BEHAVIOR_ORACLE`**，在 Cutover 完成前**绝不删除**。

---

## 1. 执行顺序（强制，不得跳步）

```
SPEC → FIXTURE → TEST → PLATFORM IMPLEMENTATION → CONFORMANCE → PARITY → CUTOVER
```

Android 不得自己设计一套业务规则、iOS 再猜一套、Harmony 再抄一套。
**唯一业务真相源 = `spec/`。**

---

## 2. 阶段划分

| Phase | 名称                                  | 关键产出                                              |
| ----- | ------------------------------------- | ----------------------------------------------------- |
| **N0**  | Canonical Specification             | `spec/` + `fixtures/` + codegen + conformance harness |
| **N1**  | Android Vertical Slice               | fresh install → import → proposal → scenario → plan → backup → restore |
| **N2**  | Android Full Parity（MVP01–03）      | 全部功能 + Conformance PASS                            |
| **N3**  | HarmonyOS Full Parity                | 与 Android 功能 parity                                  |
| **N4**  | iOS Full Parity / Source Parity      | 无 macOS 时先 Source Parity                             |
| **N5**  | Cross-platform Conformance           | Domain / StateMachine / Impact / Migration / Crypto / DEPMap / UI |
| **N6**  | Legacy Cutover                       | 移除 DCloud / UTS / uni-app 的 Production 依赖          |
| **N7**  | Native Production RC                 | Build / Runtime / Security / Store Readiness / Final Report |

### N0 完成判据（§174）

- Canonical Spec 完成（机器可读）
- Codegen 完成 + Gate 通过
- Golden Fixtures 完成
- Legacy Oracle 冻结（tag + manifest）
- Conformance harness 完成

---

## 3. 范围边界

**属于本轮**：MVP01–MVP03 现有语义的跨端原生化。

**不属于本轮（§215 禁止）**：

- MVP04 International Payments
- 任何新业务 Domain
- RealityDrift / IncidentPlan / Browser Discovery / Open Banking / AI-LLM / 云同步
  （NEXT_BACKLOG 项）

**Capability**：当前只有 `payment`。`access` / `recovery` / `identity` 只可存边，不参与传播。

**Relation**：runtime 只有 `funding_source` / `merchant_agreement`。
`card_on_file` / `direct_debit_mandate` / `payout_destination` / `wallet_funding`
仅作为 Future Spec，**本轮不启用**。

**Scenario**：只有 `replace_payment_card` / `expiring_payment_card` / `close_payment_instrument`
三个 active。

---

## 4. 不可协商的正确性铁律

| ID      | 铁律                                                |
| ------- | --------------------------------------------------- |
| OBS-01  | Observation ≠ Reality                               |
| OBS-02  | Proposal ≠ Reality                                  |
| OBS-03  | Candidate ≠ Node                                    |
| OBS-04  | Drift ≠ Reality Change                              |
| OBS-05  | Evidence ≠ History                                  |
| OBS-06  | absence ≠ non-existence                             |
| OBS-07  | done ≠ verified                                     |
| OBS-08  | coverage ≠ readiness                                |
| OBS-09  | Timeline ≠ Truth                                    |
| IMP-01  | `must_change` 只来自 confirmed reality；false positive = 0 |
| IMP-02  | 机器推断**永不**设置 `criticality = required`         |
| GR-01   | 只有 confirmed reality mutation 才 bump graphRevision |
| GR-02   | mutation 与 revision increment **同事务**             |
| DR-01   | Drift 只接受 positive evidence                       |
| PC-01   | Candidate 不进 Impact、不 bump revision              |
| VF-01   | future evidence 最多 `evidence_suggested`            |
| RD-01   | Readiness 用显式 resolution 映射，**禁止减法**        |
| SRC-01  | 不同 SourceInstance 不共享 fingerprint 命名空间        |
| SRC-02  | 多源 evidence 增加 provenance，不增加确定性             |
| DEP-01  | `DEPMAP_CONTAINER_V1` 三端字节级兼容                   |

完整清单见 `spec/domain/invariants.md`。

---

## 5. 状态声明口径（§246 禁止自夸）

枚举：`PASS` / `FAIL` / `BLOCKED` / `NOT_RUN` / `PARTIAL_WITH_REPORT`

- 没有 macOS → `IOS_BUILD = BLOCKED_BY_MACOS`，**不得**写 PASS
- 没有真机 → `*_RUNTIME = NOT_RUN`，**不得**写 PASS
- 没有商店账号 → `STORE_READY = NO`

**外部 Blocker 只允许**（§202）：
macOS/Xcode、开发者账号、签名证书、物理设备、商店账号、正式品牌、真实数据、外部服务。

**可解决的问题一律不得 BLOCKED**（§203）：Gradle、Kotlin/Swift/ArkTS 编译错误、
SDK、依赖、测试、UI、迁移、crypto 集成 —— 全部自己解决。

---

## 6. Git 纪律

**禁止**：`git reset --hard`、`git clean -fd`、`git checkout .`、`git restore .`、
force push、history rewrite。

**本工作区已知缺陷**：`.git/refs/heads/**` loose ref 会被外部进程回收，
`git commit` 可能返回 0 却不推进 HEAD。
规避：`git add` → `git write-tree` → `git commit-tree -p <parent>` →
写入 `.git/packed-refs`；每次提交后校验 `git rev-parse HEAD`。

**默认 Local Commit，不自动 push**（§205）。

---

## 7. 最终 Cutover 条件（§211）

只有以下**全部**满足，才能 `NATIVE_MIGRATION = PASS`：

1. Canonical Spec = SINGLE SOURCE OF TRUTH
2. Android parity PASS
3. Harmony parity PASS
4. iOS source parity PASS
5. 跨端 Conformance PASS
6. Crypto Golden PASS
7. Migration PASS
8. Backup/Restore PASS
9. UI critical flow parity PASS
10. Security PASS
11. **Production build 不再依赖 DCloud / UTS / uni-app**

---

## 8. 关键文件索引

| 文件                                    | 作用                                    |
| --------------------------------------- | --------------------------------------- |
| `spec/README.md`                        | Canonical Spec 的法律地位               |
| `spec/domain/domain.json`               | 机器可读枚举 / 实体 / 关系 / 场景 / 常量 |
| `spec/state-machines/`                  | 状态机（机器可读）                      |
| `spec/security/depmap-container-v1.json`| `.depmap` 冻结协议 + Golden Vector      |
| `spec/schema/`                          | 逻辑 Schema + 持久化契约                |
| `spec/ui/`                              | 设计 token + 中文文案                   |
| `fixtures/`                             | 平台中立输入 + 期望输出（64 用例）      |
| `conformance/CONFORMANCE_MANIFEST.json` | 用例清单 + 哈希 + oracle 提交           |
| `tools/codegen/generate.mjs`            | spec → Kotlin/Swift/ArkTS 枚举          |
| `tools/conformance/run.mjs`             | 统一 Conformance Gate                   |
| `LEGACY_REFERENCE_MANIFEST.md`          | Legacy 冻结身份卡                       |
| `LEGACY_BEHAVIOR_CORRECTIONS.md`        | 旧实现真实缺陷                          |
| `NATIVE_MIGRATION_STATUS.md`            | 当前进度（每次运行后更新）              |
| `NATIVE_PARITY_MATRIX.md`               | 功能 parity 矩阵                        |
| `CROSS_PLATFORM_CONFORMANCE_MATRIX.md`  | 跨端 Conformance 矩阵                   |
| `NATIVE_EXTERNAL_BLOCKERS.md`           | 真正的外部 blocker                      |
