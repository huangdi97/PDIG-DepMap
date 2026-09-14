# FINAL_TYPE_SAFETY_AUDIT.md

> PDIG / DepMap — FINAL PRODUCTION CLOSURE V1，第 10 节「Type Escape Audit」。
> 全部数字为 2026-09-14 本轮实测（`core/`）。

---

## 1. 结论

| 项                                                | 结果           |
| ------------------------------------------------- | -------------- |
| `any`（`core/src`）                               | **0**          |
| `as any`（`core/src` + `core/tests`）             | **0**          |
| `unknown as`（`core/src`）                        | **0**          |
| `as unknown`（`core/src`）                        | **0**          |
| `@ts-ignore` / `@ts-expect-error` / `@ts-nocheck` | **0**          |
| 非空断言（`x!.` 精确扫描）                        | **0**          |
| `tsc --noEmit`                                    | **EXIT=0**     |
| **关键正确性路径是否靠 `any` 绕过**               | **否（0 处）** |

**TYPE_SAFETY_READY = PASS**

---

## 2. 严格性开关（`core/tsconfig.json`）

`strict`、`noImplicitAny`、`noImplicitReturns`、`noImplicitOverride`、`noFallthroughCasesInSwitch`、`noUncheckedIndexedAccess`、`exactOptionalPropertyTypes`、`verbatimModuleSyntax`、`isolatedModules`、`forceConsistentCasingInFileNames` 全部 `true`；`useUnknownInCatchVariables` 由 `strict` 隐含开启。

**未为 UTS / 平台互操作降低 Core 标准。** `app/`（`.uvue` / `.uts`）与 `platforms/` 不使用该 tsconfig，其约束由 `check:ui`（U1–U9）与平台契约文档承担。

---

## 3. 关键模块逐项审计

| 模块               | 文件                                    | `any` / `as any` / `unknown as` / `@ts-*` |
| ------------------ | --------------------------------------- | ----------------------------------------- |
| Impact             | `src/impact/`                           | **0**                                     |
| PlanReadiness      | `src/services/plan-readiness.ts`        | **0**                                     |
| GraphRevision      | `src/repositories/graph-revision.ts`    | **0**                                     |
| PlanRebase         | `src/services/plan-analysis.ts`         | **0**                                     |
| RealityDrift       | `src/services/reality-drift-service.ts` | **0**                                     |
| ChangePlan         | `src/services/change-plan-service.ts`   | **0**                                     |
| Migration          | `src/schema/migrations.ts`              | **0**                                     |
| Crypto             | `src/crypto/`（3 文件）                 | **0**                                     |
| Fingerprint        | `src/fingerprint/`                      | **0**                                     |
| Proposal           | `src/domain/proposal.ts`                | **0**                                     |
| RelationRegistry   | `src/domain/relation-registry.ts`       | **0**                                     |
| Adapters / Sources | `src/sources/`（4 文件）                | **0**                                     |
| Repository         | `src/repositories/`（14 文件）          | **0**                                     |
| Verification       | `src/services/`（含 verification 路径） | **0**                                     |

---

## 4. 类型断言的分布与性质（非逃逸）

`core/src` 共 **97** 处 `as <Type>` 断言。分布：

| 目录               | 数量 |
| ------------------ | ---- |
| `src/repositories` | 71   |
| `src/services`     | 20   |
| `src/domain`       | 3    |
| `src/db`           | 2    |
| `src/crypto`       | 1    |

目标类型频次（前 10）：

| 目标                                          | 次数 | 性质                                                                                                         |
| --------------------------------------------- | ---- | ------------------------------------------------------------------------------------------------------------ |
| `Record`                                      | 18   | JSON 解析产物收窄（`parsed as Record<string, unknown>`），随后逐字段 `unknownToString` / `assertString` 校验 |
| `Array`                                       | 9    | `JSON.parse` 结果数组收窄                                                                                    |
| `DependencyProposal`                          | 8    | SQLite 行 → 领域类型映射                                                                                     |
| `Dependency`                                  | 6    | 同上                                                                                                         |
| `DependencyGroupProposal` / `DependencyGroup` | 各 5 | 同上                                                                                                         |
| `Capability`                                  | 5    | 列值 → 字面量联合收窄                                                                                        |
| `SourceInstance`                              | 4    | SQLite 行映射                                                                                                |
| `DiscoveryCandidate`                          | 4    | 同上                                                                                                         |
| `Map`                                         | 3    | 反序列化容器收窄                                                                                             |

**判定**：全部位于 **I/O 边界**（SQLite 行、`JSON.parse` 产物）。此类收窄在 TypeScript 中不可消除（驱动返回 `unknown`/无类型行），且：

- 所有收窄后均经显式校验（`assertString` / `assertInt` / `unknownToString` / `CHECK` 约束）；
- 数据库层有 `CHECK` 约束（如 `criticality CHECK(required,unknown)`、`state CHECK(active,retired)`、`origin CHECK(manual,proposal)`）；
- 关键语义（`required` 只能由用户确认设置）由 invariant 套件守护，不依赖断言。

**未发现**以 `as` 绕过正确性判断的情况（如 `as` 后直接参与 Impact / Readiness 判定而无校验）。

---

## 5. 低信息命名清理（与类型安全相关的可读性）

`core/src` 内 `obj` 命名 **18 处** → 重命名为 `record`（词边界安全替换，`'object'` 未被误伤）。

验证：`\bobj\b` 残余 **0**；`'object'` 4 处完好；无 `recordect` 损坏；`tsc` / `eslint` / `prettier` / 聚焦测试（`tests/crypto` + `tests/repository` + `tests/contract` = 123 passed）全部 PASS。

---

## 6. 死导出清理（与类型面相关的 API 收缩）

| 符号                           | 类型      | 处置                                               |
| ------------------------------ | --------- | -------------------------------------------------- |
| `ObservationFingerprintRecord` | interface | 删除（零消费者）                                   |
| `ParsedCsvCell`                | interface | 删除（零消费者，`parseCsvLine` 返回 `string[]`）   |
| `planEffectiveStatus`          | function  | 删除（零消费者的转发包装；`effectiveStatus` 保留） |

保留 4 个零消费者导出 `BiometricAdapter` / `FileCryptoAdapter` / `PrivacyScreenAdapter` / `SecureDatabaseAdapter` —— 它们是**平台适配器契约面**，由 Kotlin / Swift / ArkTS 实现消费，不构成死代码。

---

## 7. 复验命令

```
cd core
npx tsc --noEmit          # EXIT=0
npm run lint              # EXIT=0
npm run format:check      # EXIT=0
npm test                  # 453 passed / 43 files
```
