# TEST_REPORT_RC.md — 全量测试报告（RC PHASE G）

> 执行：2026-09-12 · `cd core && npm test`（vitest 3.2.7 · Node v22.15.0 · win32-x64）
> 结果：**166 tests / 166 PASS / 0 failed / 0 skipped** · 15 个测试文件

## 按套件

| 套件 | 文件 | 用例 | 覆盖 Gate |
|---|---|---|---|
| Migration | tests/repository/migration.test.ts | 8 | migration、UNIQUE/CHECK |
| Repositories | tests/repository/repositories.test.ts | 8 | repository、UPSERT/复活、group canonical |
| Proposal lifecycle | tests/repository/proposal-lifecycle.test.ts | 7 | proposal lifecycle、evidence |
| Impact kernel | tests/impact/kernel.test.ts | 17 | impact T1–T12 + 附加 |
| Crypto（含 golden） | tests/crypto/depmap.test.ts | 28 | crypto 全项 |
| Parser+指纹+周期 | tests/parser/wechat.test.ts | 18 | parser 10 fixtures + 指纹 |
| Resolver | tests/unit/resolver.test.ts | 11 | resolver 顺序与支付方式 |
| Integration E2E | tests/integration/pipeline.test.ts | 9 | synthetic E2E |
| Determinism（新） | tests/unit/determinism.test.ts | 4 | determinism（50 次重复） |
| Idempotency（新） | tests/unit/idempotency.test.ts | 5 | migration×50 / 重复导入 / accepted 重放 / retire 循环 / export-import 等价 |
| Negative（新） | tests/unit/negative.test.ts | 12 | parser/resolver/proposal/impact 负向 |
| Crypto negative（新） | tests/unit/crypto-negative.test.ts | 10 | crypto 负向 + 容器 mutation fuzz |
| Property/Fuzz（新） | tests/unit/property-fuzz.test.ts | 6 | 随机图性质 / CSV mutation / 指纹性质 |
| DB integrity（新） | tests/unit/db-integrity.test.ts | 6 | 孤儿检测 / rollback / 导入事务性 |
| Performance smoke（新） | tests/perf/performance-smoke.test.ts | 9 | L/AF（另见 PERFORMANCE_SMOKE.md） |

## 规则遵守

- 无 skip、无删除测试、无降低断言（RC_AUDIT_RULES）
- 新增测试均为先行失败后实现/修复（修复见 git log：日历校验、CR 换行、CHECK 加固、kdf 错误包装）

## 复现

```bash
cd core && npm test     # 166/166 PASS
npm run test:perf       # 性能 smoke 子集
npm run test:coverage   # 覆盖率（COVERAGE_REPORT.md）
```
