# TYPE_SAFETY_BASELINE.md — Type Escape 审计基线（Engineering Baseline V1）

> 审计时间：2026-09-13（Engineering Baseline V1 轮）。方法论：全量 grep + eslint typed rules 复核。

## 结论

| 指标                              | src（业务核心）            | tests            | scripts(.ts)     |
| --------------------------------- | -------------------------- | ---------------- | ---------------- |
| `: any` / `as any`                | **0**                      | 0                | 0                |
| `unknown as X`                    | **0**                      | 0                | 0                |
| `@ts-ignore` / `@ts-expect-error` | **0**                      | 0                | 0                |
| 非空断言 `!`                      | **0**（eslint error 禁止） | 允许（断言场景） | 0                |
| `eslint-disable`（文件级/行级）   | **0**                      | 0                | 0                |
| `console.*`                       | 0                          | 允许             | 允许（CLI 工具） |

## 分类登记

- **justified**：无。
- **platform interoperability**：无（src 无平台条件分支；平台桥接在 `platforms/` Kotlin/ArkTS/Swift 与 `app/` UTS，不受本基线约束，由 CROSS_PLATFORM_CONTRACT_AUDIT 覆盖）。
- **test-only**：`tests/**/*.ts` 允许非空断言与部分 unsafe 规则（eslint flat config 显式声明，理由：测试构造合法性场景）。
- **should-fix**：无。
- **generated**：无生成代码。

## 自动化保障

- eslint `no-explicit-any` / `no-unsafe-*` / `no-non-null-assertion` 全部 error 级（见 `core/eslint.config.js`）。
- tsc strict 全开 + `noUncheckedIndexedAccess` + **`exactOptionalPropertyTypes`（本轮实测开启）**：
  开启暴露 8 处真实问题（数据记录类型的 optional 字段被显式 `undefined` 赋值），已全部修复
  （domain/source.ts、fingerprint.ts 输入类型、ofx OfxTransaction、evidence 输入），修复后 324 tests 全绿。
  `useUnknownInCatchVariables` 由 strict 蕴含已开启。

## 关键路径 type-safe 声明

Impact / Crypto / Migration / Fingerprint / Evidence / Proposal / Dependency / RelationRegistry / Parser / SourceAdapter / Repository / Validation —— 全部 0 unsafe escape，由 eslint 强制。历史审计：`docs/TYPE_SAFETY_AUDIT.md`（RC 轮）。
