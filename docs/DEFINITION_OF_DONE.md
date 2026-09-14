# DEFINITION_OF_DONE.md — 完成定义（Engineering Baseline V1）

> 任何 Agent（ZCode / WorkBuddy / Codex / 其他）**不得宣称「完成」**，除非以下全部成立
> 并在 WORK_STATUS 留下证据。口头 PASS = 未完成。

## 代码类任务（每次交付）

| 门                  | 要求                                                                                    |
| ------------------- | --------------------------------------------------------------------------------------- |
| IMPLEMENTED         | 功能实现，无 stub 冒充                                                                  |
| FORMAT PASS         | `npm run format:check`                                                                  |
| LINT PASS           | `npm run lint`（0 errors / 0 warnings）                                                 |
| TYPECHECK PASS      | `npm run typecheck`                                                                     |
| TEST PASS           | `npm test`（全量，0 skip；新增测试先行）                                                |
| REGRESSION PASS     | `npm run check` 全绿                                                                    |
| ARCHITECTURE PASS   | `check:architecture`（含 circular = 0）                                                 |
| SECURITY CHECK PASS | `check:secrets` + `check:network`；涉密改动另跑 SECURITY_PRIVACY_REGRESSION_MATRIX 对照 |
| DOCS UPDATED        | WORK_STATUS.md + 受影响 docs                                                            |

## 按变更类型的附加门

| 变更类型              | 附加门                                                            |
| --------------------- | ----------------------------------------------------------------- |
| Schema（任意 N）      | MIGRATION TESTED：`N-1 → N` + oldest→N + 回滚 + ×50 幂等          |
| Source Adapter        | CONTRACT TEST PASS（adapter-contract 注册 + 全用例）              |
| 持久层实现            | RepositoryContract（DRIVERS 注册）                                |
| Crypto                | GOLDEN VECTOR PASS + fail-closed 矩阵（container-mutation）不回归 |
| 影响语义 / 关键不变量 | INV + property 套件通过，MUTATION 报告有对应 killed 证据          |
| 性能可见路径          | test:perf 对照 PERFORMANCE_BASELINE                               |
| 里程碑（milestone）   | `npm run check:full` 全绿 + QUALITY_GATES_V1 刷新                 |

## 平台类任务

`IMPLEMENTED` / `COMPILED` / `TESTED` / `DEVICE_VERIFIED` / `STORE_READY` **分别声明**，
不得把「理论支持」「源码就绪」写成「已验证」。

## 状态声明词汇表（唯一合法口径）

PASS / FAIL / BLOCKED（外部原因 + blocker 编号）/ NOT_RUN（未执行 + 原因）/ PARTIAL_WITH_REPORT。
不存在「基本完成」「应该没问题」。
