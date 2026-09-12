# ENGINEERING_STANDARDS.md — 工程规范总纲（Engineering Baseline V1）

> 本文件是长期规范的索引与正文。与其他控制文件冲突时：Correctness/Security/Privacy 以
> `AGENTS.md`、`CANONICAL_DESIGN.md` 更严格者为准。

## 1. 命名

- Type / Interface / Class：`PascalCase`（`DependencyProposal`、`NodeSqliteDriver`）。
- function / variable：`camelCase`；常量：**本仓惯例 = `SCREAMING_SNAKE_CASE`**
  （`DEPMAP_V1_BOUNDS`、`REPROPOSAL_MIN_NEW_OBSERVATIONS`、`SCHEMA_VERSION`）。
- 文件名：现有惯例（`kebab-case.ts`：`relation-registry.ts`、`import-coordinator.ts`）。
- 测试：`*.test.ts`；fixture：自描述场景名（见 `docs/FIXTURE_POLICY.md`）。
- 禁止 `data2` / `tmp` / `xx` / `newData` / `test1` / `final2` 进入正式代码。

## 2. Domain 词汇（禁止模糊化）

`Observation`、`NormalizedPaymentObservation`、`DiscoveryCandidate`、`Proposal`、
`Dependency`、`DependencyGroup`、`EvidenceSummary`、`SourceInstance`、`ImportSession`、
`ChangePlan`、`Impact` —— 必须使用上述领域名词；禁止 `item` / `record` / `thing` /
`relationData` 替代领域概念（跨层传递的局部变量除外，但类型名必须准确）。

## 3. 格式

- UTF-8 / LF / final newline / trim trailing whitespace：`.editorconfig` + `.gitattributes` 强制。
- Windows 开发不得引入 CRLF 污染（`* text=auto eol=lf`；`.bat` 显式 CRLF；fixtures `-text`）。
- `npm run format` / `format:check`（prettier）。GBK fixture 永不转码（FIXTURE_POLICY §特殊编码保护）。

## 4. Lint / 类型

- eslint typed（flat config）：0 errors，warnings 必须审计归零。重点已启用：
  unused / floating promises / misused promises / unsafe any 全家 / non-null assertion /
  no-empty-catch / switch fallthrough / eqeqeq / no-console。
- 整文件 eslint-disable 禁止（无生成代码；无文件级豁免）。
- tsc：strict 全开 + `noUncheckedIndexedAccess` + `noImplicitReturns` + `noFallthroughCasesInSwitch`。
  `exactOptionalPropertyTypes` 评估结论：false（见 TYPE_SAFETY_BASELINE）。
- 测试代码允许非空断言与部分 unsafe 规则（断言构造场景），其余同标准。

## 5. 错误模型

- typed error + 稳定 code（`DepmapError`，code ∈ invalid_json/invalid_structure/bounds/kdf/auth_failed）。
- 禁止大范围字符串匹配判错；错误 message 不含敏感载荷（LOGGING_POLICY §硬规则 2）。
- fail-closed 矩阵：`docs/FAIL_CLOSED_MATRIX.md`（新失败路径先补证据测试）。

## 6. 测试（详见 docs/TEST_STRATEGY.md）

- test-first；核心模块未绿不做 UI。
- 禁止删除/降断言/skip/retry 掩盖制造 PASS。
- 契约注册表强制挂接（新 Adapter / 持久实现 / crypto 实现）。
- 稳定性：`npm run test:stability`（×3）零失败。

## 7. 安全 / 隐私（详见 docs/SECURITY_PRIVACY_REGRESSION_MATRIX.md）

- 业务网络 = 0（`check:network`）；src 零 console；secrets/真实数据永不入库。
- Observation/raw bill 只活会话内存（`docs/MEMORY_DATA_LIFETIME.md`）。

## 8. 日期 / 金额

`docs/DATE_TIME_RULES.md`、`docs/MONEY_CURRENCY_RULES.md`。

## 9. 提交 / 分支

`docs/COMMIT_CONVENTION.md`；小步 commit；禁止提交签名材料、真实账单、解密产物。

## 10. 性能 / 覆盖率

`docs/PERFORMANCE_BASELINE.md`（回归判定流程）、`docs/COVERAGE_POLICY.md`（双层 Gate）。

## 11. 统一命令（唯一入口）

```
npm run check        # 快速 Gate：format:check + lint + typecheck + 全部测试 + architecture(+circular) + network + secrets
npm run check:full   # check + db-integrity + coverage + perf + deps/license 审计
```

定向入口：`check:invariants` / `check:contract` / `check:property` / `check:db-integrity` /
`check:architecture` / `check:network` / `check:secrets` / `check:deps` / `test:perf` / `test:stability`。
