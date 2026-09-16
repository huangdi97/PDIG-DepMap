# PDIG Canonical Spec — 唯一业务真相源

> **PDIG** = Personal Digital Infrastructure Graph（个人数字基础设施图谱）

本目录是 PDIG 的 **SINGLE SOURCE OF TRUTH**。

## 0. 本目录的法律地位

| 角色                        | 权威性                                                     |
| --------------------------- | ---------------------------------------------------------- |
| `spec/`（本目录）           | **最高**。三端实现不得自行改变 Domain Semantics            |
| `conformance/expected/`     | 由 spec 派生的期望输出，冻结后不得随平台需要而修改          |
| `fixtures/`                 | 输入样本，平台中立 JSON                                     |
| `android/` `harmony/` `ios/` | 实现。**不得**定义 spec 中没有的枚举值、状态或迁移         |
| `spec-legacy/`（旧 TS Core） | 仅作 **BEHAVIOR ORACLE / FIXTURE GENERATOR**，不再是产品未来 |

## 1. 变更顺序（强制）

任何业务规则变化，必须按此顺序，**不得跳步、不得倒序**：

```
1. 修改 Canonical Spec      (spec/)
2. 重新生成 Golden Fixtures (fixtures/ + conformance/expected/)
3. 三端实现                 (android/ harmony/ ios/)
4. 跑 Conformance           (tools/conformance)
5. 更新 Parity Matrix
```

**禁止**：Android 自己设计一套业务规则、iOS 再猜一套、Harmony 再抄一套。

## 2. 唯一性规则（硬约束）

- 三端 **禁止手写** 会漂移的静态枚举。枚举一律由 `spec/domain/domain.json`
  经 `tools/codegen` 生成到各端 `generated/` 目录。
- 生成文件头必须带：

  ```
  DO NOT EDIT
  Generated from canonical PDIG spec (spec/domain/domain.json)
  ```

- `tools/codegen/check.mjs` 会校验 `spec → generated` 一致性；
  手改 generated 文件 → **Gate 失败**。

## 3. 明确禁止的解释

三端实现 **不得**：

- 引入 `safe` / `fully_safe` / `100%` / `all_clear` 一类 readiness 文案或状态
- 用 `target 数量 − 完成动作数` 判断 ready（见 `spec/state-machines/change-plan.json`）
- 让机器推断设置 `criticality = required`
- 让 absence（账单里没出现）产生任何现实否定、Drift 或 readiness 提升
- 让 Proposal confidence 参与 `must_change` 或 readiness 判定
- 让 DiscoveryCandidate 进入 Impact 或 bump graphRevision
- 把 `done` 当作 `verified`
- 让 Drift 创建或 dismiss 改变 Reality / graphRevision
- 让不同 SourceInstance 共享 fingerprint 命名空间
- 把跨 stream 的 observationCount 相加用于重提阈值判定

## 4. 目录结构

```
spec/
  README.md                       ← 本文件
  domain/
    domain.json                   ← 机器可读：枚举 / 实体 / 关系 / 场景 / 常量 / 不变量
    entities.md                   ← 实体与语义叙事
    invariants.md                 ← 不变量与针对性反例
  schema/
    logical-schema.json           ← 逻辑 Schema v3（跨端一致）
    persistence-contract.md       ← 逻辑键 / 唯一性 / 事务 / 时间戳语义
  state-machines/
    change-plan.json              ← ChangePlan 状态机（含派生 needs_revalidation）
    state-machines.json           ← Drift / Candidate / Verification / Dependency / Group / Revision
  errors/
    error-codes.json              ← 统一 ErrorCode + 用户文案 key
  security/
    depmap-container-v1.json      ← DEPMAP_CONTAINER_V1 冻结协议 + Golden Vector
    security-policy.md            ← 密钥 / 锁 / 备份 / 认证 / 日志 / 网络策略
  migration/
    migration-spec.md             ← DB v1→v3 与 payload v1→v3
  ui/
    design-tokens.json            ← 品牌 Token（colors / spacing / radius / typography）
    copy-zh.json                  ← 统一中文文案（避免三端说法漂移）
```

## 5. 平台差异是被允许的，语义差异不是

| 维度                | 是否必须跨端一致 | 说明                                          |
| ------------------- | ---------------- | --------------------------------------------- |
| Domain semantics    | **必须一致**     | 由本目录定义                                  |
| Enum 值 / 状态机    | **必须一致**     | Codegen 生成                                  |
| Logical keys / 唯一性 | **必须一致**   | 见 persistence-contract                       |
| Migration 行为      | **必须一致**     | 同一 fixture 同结果                           |
| `.depmap` 字节      | **必须一致**     | Golden Vector 覆盖                            |
| 物理 DDL            | 可不同           | Android/iOS SQLCipher、Harmony ArkData 各有实现 |
| UI 呈现             | 可不同           | 必须原生平台感；信息架构与用户流程一致         |
| 用户文案            | 中文一致         | 走 `copy-zh.json`，可平台化排版               |

## 6. 状态声明口径（不得夸大）

任何平台能力只能声明为：

`NOT_STARTED` → `IMPLEMENTED` → `TESTED` → `CONFORMANCE_PASS` → `RUNTIME_VERIFIED`

- 没有 macOS → `IOS_BUILD = BLOCKED_BY_MACOS`，**不得**写 PASS
- 没有真机 → `*_RUNTIME = NOT_RUN`，**不得**写 PASS
- 没有 store 账号 → `STORE_READY = NO`，**不得**声称已上架

## 7. 与旧 TS Core 的关系

`core/`（TypeScript）是被冻结的 **REFERENCE IMPLEMENTATION / BEHAVIOR ORACLE**。
它可以：生成 fixture、回答语义歧义、跑 differential test。
它不可以：作为 Production runtime 的一部分。

详见 `LEGACY_REFERENCE_MANIFEST.md` 与 `docs/ADR_NATIVE_MIGRATION.md`。
