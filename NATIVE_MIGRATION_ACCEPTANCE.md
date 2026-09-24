# NATIVE_MIGRATION_ACCEPTANCE.md

> PDIG Native Migration 的**完成定义**。
> 任何一项为 NO / FAIL / NOT_RUN，都不得宣称 `NATIVE_MIGRATION = PASS`。

更新时间：2026-09-15

---

## 1. N0 Canonical Specification

| 判据                                     | 状态    | 证据                                        |
| ---------------------------------------- | ------- | ------------------------------------------- |
| `spec/` 存在且声明为唯一业务真相源       | **YES** | `spec/README.md`                            |
| 机器可读 Domain Spec（枚举/实体/关系）   | **YES** | `spec/domain/domain.json`                   |
| Relation Spec（runtime 2 个 + Future 4） | **YES** | `domain.json → relations / futureRelations` |
| Capability Spec（payment only）          | **YES** | `domain.json → enums.Capability`            |
| Criticality Spec（required/unknown）     | **YES** | `domain.json`                               |
| 状态机机器可读（≥6 台）                  | **YES** | `spec/state-machines/`（7 台）              |
| Error Code Spec                          | **YES** | `spec/errors/error-codes.json`              |
| Schema Spec（逻辑 v3）                   | **YES** | `spec/schema/logical-schema.json`           |
| Persistence Contract                     | **YES** | `spec/schema/persistence-contract.md`       |
| Security Policy                          | **YES** | `spec/security/security-policy.md`          |
| DEPMAP_CONTAINER_V1 冻结                 | **YES** | `spec/security/depmap-container-v1.json`    |
| UI Design Tokens                         | **YES** | `spec/ui/design-tokens.json`                |
| UI Copy（中文统一）                      | **YES** | `spec/ui/copy-zh.json`                      |
| Migration Spec                           | **YES** | `spec/migration/migration-spec.md`          |

**N0 = PASS**

---

## 2. Codegen

| 判据                                | 状态     | 证据                                      |
| ----------------------------------- | -------- | ----------------------------------------- |
| 由 spec 生成 Kotlin / Swift / ArkTS | **YES**  | `tools/codegen/generate.mjs`              |
| 生成文件带 DO NOT EDIT 头           | **YES**  | 三端 generated 文件头部                   |
| 一致性 Gate（手改即失败）           | **PASS** | `node tools/codegen/generate.mjs --check` |
| 三端没有手写会漂移的静态枚举        | **YES**  | 枚举全部来自 codegen                      |

**CODEGEN = PASS**

---

## 3. Golden Fixtures

| 判据                                             | 状态     | 证据                                    |
| ------------------------------------------------ | -------- | --------------------------------------- |
| 平台中立 JSON fixture                            | **YES**  | `fixtures/`（64 用例）                  |
| 原始输入 fixture 三端共用，不复制三份            | **YES**  | `fixtures/import/`（28 文件）           |
| Conformance Manifest（ID/类别/哈希/oracle 提交） | **YES**  | `conformance/CONFORMANCE_MANIFEST.json` |
| fixture 完整性校验                               | **PASS** | 64 + 28 sha256 全匹配                   |
| Oracle 自检（冻结 TS 复现全部 fixture）          | **PASS** | `ORACLE SELFCHECK: PASS (64 cases)`     |

**GOLDEN_FIXTURES = PASS**

---

## 4. Legacy

| 判据                                 | 状态    | 证据                                                            |
| ------------------------------------ | ------- | --------------------------------------------------------------- |
| Legacy 未删除                        | **YES** | `core/` `app/` `platforms/` 在原位                              |
| `LEGACY_REFERENCE_MANIFEST.md`       | **YES** | 含 HEAD / Schema / DEPMAP / 测试数                              |
| 本地 tag `v0.3.0-uniapp-reference`   | **YES** | → `7bc0ed3`                                                     |
| Legacy README（为什么存在/如何使用） | **YES** | `legacy/README.md`                                              |
| 行为修正记录                         | **YES** | `LEGACY_BEHAVIOR_CORRECTIONS.md`（6 条，含 2 条真实功能性缺陷） |
| Legacy 不参与 Production build       | **NO**  | **尚未 Cutover**                                                |

**LEGACY_REFERENCE_FROZEN = PASS**
**LEGACY_PRODUCTION_DEPENDENCY = 尚未清零（NOT_RUN）**

---

## 5. Android

| 判据                | 状态        | 证据                               |
| ------------------- | ----------- | ---------------------------------- |
| ANDROID_DOMAIN      | **PASS**    | 纯 Kotlin，无 Compose/Context 依赖 |
| ANDROID_CRYPTO      | **PASS**    | 黄金向量逐字节一致                 |
| ANDROID_CONFORMANCE | **PASS**    | 64/64（harness 独立复核）          |
| ANDROID_PERSISTENCE | **NOT_RUN** | 未开始                             |
| ANDROID_UI          | **NOT_RUN** | 0 页面                             |
| ANDROID_SECURITY    | **NOT_RUN** | 未开始                             |
| ANDROID_BUILD       | **NOT_RUN** | 无 app 模块                        |
| ANDROID_RUNTIME     | **NOT_RUN** | 无 APK / 无设备                    |
| ANDROID_PARITY      | **NOT_RUN** | 见 `NATIVE_PARITY_MATRIX.md`       |

---

## 6. Harmony / iOS

| 判据                 | 状态                 | 证据                   |
| -------------------- | -------------------- | ---------------------- |
| HARMONY_*（全部）    | **NOT_RUN**          | 仅 codegen 产物        |
| IOS_DOMAIN/UI/CRYPTO | **NOT_RUN**          | 仅 codegen 产物        |
| IOS_BUILD            | **BLOCKED_BY_MACOS** | 当前 Windows，无 Xcode |

---

## 7. 跨端

| 判据                       | 状态                    | 证据                                     |
| -------------------------- | ----------------------- | ---------------------------------------- |
| CROSS_PLATFORM_CONFORMANCE | **PARTIAL_WITH_REPORT** | Android PASS，两端 NOT_RUN               |
| DEPMAP_CROSS_PLATFORM      | **PARTIAL_WITH_REPORT** | Android↔Node 已验证；无 Harmony/iOS 报告 |
| SCHEMA_MIGRATION           | **NOT_RUN**             | 无平台实现                               |
| UI_PARITY                  | **NOT_RUN**             | 0 页面                                   |
| SECURITY_PARITY            | **NOT_RUN**             |                                          |
| BACKUP_RESTORE             | **NOT_RUN**             |                                          |

---

## 8. 依赖清零审计

| 项                           | 当前                          |
| ---------------------------- | ----------------------------- |
| DCLOUD_PRODUCTION_DEPENDENCY | **未清零**（Legacy 仍在主线） |
| UTS_PRODUCTION_DEPENDENCY    | **未清零**                    |
| UNIAPP_PRODUCTION_DEPENDENCY | **未清零**                    |
| NATIVE_PRODUCTION_RC         | **NO**                        |

完整审计见 `LEGACY_REMOVAL_AUDIT.md`（待 Cutover 前生成）。

---

## 9. 结论

```
NATIVE_MIGRATION = NOT_RUN / 未完成
CANONICAL_SPEC = SINGLE_SOURCE_OF_TRUTH   (PASS)
CODEGEN = PASS
GOLDEN_FIXTURES = PASS
CONFORMANCE_HARNESS = PASS
ANDROID_DOMAIN/CONFORMANCE/CRYPTO = PASS
其余 = NOT_RUN 或 BLOCKED_BY_MACOS
REAL_DATA = NOT_RUN
STORE_SUBMITTED = NO
```

**不得**因为 N0 与 Android 领域层 PASS 就宣称迁移完成。
三端 UI / 持久化 / 安全 / Build / Runtime 尚未开始。
