# FINAL_CODE_QUALITY_REPORT.md

> PDIG / DepMap — FINAL PRODUCTION CLOSURE V1，PHASE B。
> 范围：`core/`（TypeScript 生产代码 + 测试 + 脚本）、`app/`（uni-app x 源码）、`platforms/`（原生源码）。
> 全部结论对应可复现命令；无「代码看起来正确」式判定。

---

## 1. 汇总

| 维度           | 结果                            | 证据                                                                         |
| -------------- | ------------------------------- | ---------------------------------------------------------------------------- |
| FORMAT（core） | **PASS**                        | `npm run format:check` EXIT=0                                                |
| FORMAT（docs） | **PASS**                        | `npm run format:docs:check` EXIT=0（**本轮新增**）                           |
| LINT           | **PASS**（0 error / 0 warning） | `npm run lint` EXIT=0                                                        |
| TYPECHECK      | **PASS**                        | `tsc --noEmit` EXIT=0                                                        |
| TYPE_ESCAPE    | **PASS**（0）                   | `any`=0 / `as any`=0 / `unknown as`=0 / 非空断言=0                           |
| RULE_DISABLE   | **PASS**（0）                   | `eslint-disable*` / `@ts-ignore` / `@ts-nocheck` / `@ts-expect-error` 全仓 0 |
| DEAD_CODE      | **PASS**（清理 3 项）           | 零消费者导出扫描 + 删除                                                      |
| DUPLICATION    | **PASS（可接受）**              | 见 §7                                                                        |
| COMPLEXITY     | **PASS（可接受）**              | 见 §8                                                                        |
| ARCHITECTURE   | **PASS**                        | `check:architecture` 48 files，circular = 0                                  |
| CIRCULAR_DEPS  | **PASS**（0）                   | 同上                                                                         |
| NAMING         | **PASS**（清理 18 处）          | `obj` → `record`                                                             |
| DOC_FORMAT     | **PASS**                        | 见 §3                                                                        |
| TODO           | **PASS**（0）                   | `core/src` 与 `app/` 均 0                                                    |
| **RESULT**     | **CODE_STYLE_READY = PASS**     | —                                                                            |

---

## 2. 格式化

### 2.1 core/

`prettier --check .`（配置 `core/.prettierrc`：`semi:false`、`singleQuote:true`、`printWidth:100`、`trailingComma:all`、`arrowParens:always`、`endOfLine:lf`）→ **EXIT=0**。

### 2.2 docs / 根目录控制文档 / store（本轮新增门禁）

**问题**：`format:check` 在 `core/` 下执行 `prettier --check .`，**只覆盖 core/**；根目录 63 个 `.md`、`docs/` 88 个、`store/` 5 个、`assets/` 1 个、`.codebuddy/**/*.mdc` 均无格式门禁。

**处置**（本轮实施）：

1. 新增根级 `.prettierrc.json`：
   - 与 core 一致的代码风格；
   - `proseWrap: "preserve"`（**禁止重排正文换行**）；
   - `embeddedLanguageFormatting: "off"`（**禁止重排代码块内嵌代码**）；
   - `overrides: [{ files: "*.mdc", options: { parser: "markdown" } }]`。
2. 新增脚本：
   - `format:docs` → `prettier --write "../*.md" "../docs/**/*.md" "../store/**/*.md" "../assets/**/*.md" "../.codebuddy/**/*.mdc"`
   - `format:docs:check` → 同上 `--check`
3. 将 `format:docs:check` 纳入 `npm run check` 链（`format:check` 之后）。

**关键决策（安全性论证）**：

首次尝试仅设 `proseWrap: preserve` 时，`git diff -w` 显示 **134 文件存在非空白改动**，其中包含代码块内嵌代码被重排的真实语义风险，例如：

- golden JSON 示例 `"amount": 25.00,` → `"amount": 25.0,`
- `simulateDisable(nodeId, capability='payment')` → `simulateDisable(nodeId, (capability = 'payment'))`
- `type SourceKind =\n  | 'statement_file' | ...` → 单行合并

这违反「不得改写历史 Snapshot 语义」。**因此回滚后追加 `embeddedLanguageFormatting: "off"` 并重跑。**

**最终改动性质（证据化分类）**：134 个 `.md`/`.mdc` 文件，`git diff -w` 残余全量分类：

| 类别                                                  | 行数  | 性质                                      |
| ----------------------------------------------------- | ----- | ----------------------------------------- |
| 表格分隔行对齐（`\|---\|---\|` → `\| --- \| --- \|`） | 473   | 纯空白                                    |
| 空行插入（列表前 / 段落间）                           | 487   | 纯空白                                    |
| 项目符号规范化（`+` → `-`）                           | 5 处  | 渲染等价                                  |
| blockquote 惰性续行（去掉多余 `>`）                   | 1 处  | 渲染等价（CommonMark 语义相同）           |
| `repositories/*` → `repositories/\*` 转义             | 1 处  | 渲染等价（渲染结果同为 `repositories/*`） |
| **代码块内嵌代码改动**                                | **0** | —                                         |
| **语义/内容改动**                                     | **0** | —                                         |

**幂等性**：连续两次 `format:docs` 第二次全部 `(unchanged)`；`format:docs:check` EXIT=0。

---

## 3. Lint

`npm run lint`（`eslint .`，`eslint.config.js` + `typescript-eslint`）→ **EXIT=0，0 error / 0 warning**。

重点规则方向（`unused import` / `unused variable` / `unreachable code` / `floating promise` / `async misuse` / `switch fallthrough` / `duplicate branch` / `shadow variable` / `useless catch` / `ignored rejection` / `unsafe cast` / `accidental mutation`）由 `typescript-eslint` 推荐集覆盖，无命中。

---

## 4. 规则禁用指令审计

扫描范围：`core/src`、`core/tests`、`core/scripts`、`app/`、`platforms/`。

| 指令                                                                  | 命中数 |
| --------------------------------------------------------------------- | ------ |
| `eslint-disable` / `eslint-disable-next-line` / `eslint-disable-line` | **0**  |
| `@ts-ignore`                                                          | **0**  |
| `@ts-nocheck`                                                         | **0**  |
| `@ts-expect-error`                                                    | **0**  |

**整文件 disable = 0。** 无需分类（required / platform interop / test / should fix / generated 均为 0）。

---

## 5. TypeScript 严格性

`core/tsconfig.json` 实测启用：

| 选项                               | 状态                 |
| ---------------------------------- | -------------------- |
| `strict`                           | `true`               |
| `noImplicitAny`                    | `true`               |
| `noImplicitReturns`                | `true`               |
| `noImplicitOverride`               | `true`               |
| `noFallthroughCasesInSwitch`       | `true`               |
| `noUncheckedIndexedAccess`         | `true`               |
| `exactOptionalPropertyTypes`       | `true`               |
| `useUnknownInCatchVariables`       | 隐含开启（`strict`） |
| `forceConsistentCasingInFileNames` | `true`               |
| `verbatimModuleSyntax`             | `true`               |
| `isolatedModules`                  | `true`               |

`tsc --noEmit` → **EXIT=0**。**未为 UTS 降低 Core 标准**；`app/`（`.uvue`/`.uts`）不纳入该 tsconfig，由 `check:ui` 静态门禁覆盖。

---

## 6. 类型逃逸审计

| 模式                        | `core/src` 命中 | `core/tests` 命中 |
| --------------------------- | --------------- | ----------------- |
| `any`                       | **0**           | —                 |
| `as any`                    | **0**           | **0**             |
| `unknown as`                | **0**           | —                 |
| 非空断言 `x!`（启发式扫描） | **0**           | —                 |

**关键正确性路径（Impact / PlanReadiness / GraphRevision / PlanRebase / RealityDrift / Verification / Migration / Crypto / Fingerprint / Proposal / RelationRegistry / Adapters / Repository）均无 `any` 绕过。** 详见 `docs/FINAL_TYPE_SAFETY_AUDIT.md`。

---

## 7. 死代码审计

方法：对 `core/src` 全部 `export` 声明（含 `export {}` 列表）做符号名消费者分析，消费者范围 = `core/src` + `core/tests` + `core/scripts`。

**零消费者导出 = 7**：

| 符号                           | 文件                            | 判定     | 处置                                                                                     |
| ------------------------------ | ------------------------------- | -------- | ---------------------------------------------------------------------------------------- |
| `BiometricAdapter`             | `src/adapters/interfaces.ts`    | **保留** | 平台适配器契约面（由 Kotlin/Swift/ArkTS 实现消费，非 TS 消费）；该文件已在覆盖率排除列表 |
| `FileCryptoAdapter`            | 同上                            | **保留** | 同上                                                                                     |
| `PrivacyScreenAdapter`         | 同上                            | **保留** | 同上                                                                                     |
| `SecureDatabaseAdapter`        | 同上                            | **保留** | 同上                                                                                     |
| `planEffectiveStatus`          | `src/services/plan-analysis.ts` | **删除** | 纯转发包装，`effectiveStatus` 本身有消费者；删除后同步移除失效 import                    |
| `ObservationFingerprintRecord` | `src/domain/types.ts`           | **删除** | 无任何引用                                                                               |
| `ParsedCsvCell`                | `src/parser/wechat/parser.ts`   | **删除** | `parseCsvLine` 返回 `string[]`，该 interface 从未使用                                    |

**验证**：删除后 `tsc --noEmit` EXIT=0、`eslint .` EXIT=0、`prettier --check` EXIT=0、聚焦测试 `tests/crypto + tests/repository + tests/contract` → **123 passed / 123**；随后全量 `npm run check` → **453 passed / 453，EXIT=0**。

无未使用文件、无 legacy migration helper 残留、无 dead feature flag（`core/src` 内 `TODO/FIXME/HACK/PLACEHOLDER` = **0**；`app/` 内 = **0**）。

> 注：`grep XXX` 命中 6 处，均为 **ISO 4217 货币码 `'XXX'`**（表示「无货币」），非任务标记。

---

## 8. 重复代码与复杂度

**重复代码（可接受，未做抽象层爆炸式重构）**：

| 模式                                                                       | 观察                                | 判定                                                                                 |
| -------------------------------------------------------------------------- | ----------------------------------- | ------------------------------------------------------------------------------------ |
| JSON 解析中间层（`parsed as Record<string, unknown>` + `unknownToString`） | 4 文件、结构相似                    | **保留**：位于 Repository 边界，各文件字段语义不同，抽取会引入参数化抽象并降低可读性 |
| 适配器校验（WeChat / CSV / OFX）                                           | 各自独立                            | **保留**：三者契约相同但字段/编码/日期规则不同，共享代码会迫使分支内聚               |
| 日期 / 金额解析                                                            | 已分别收敛至 `utils`                | 无重复                                                                               |
| UI 卡片 / 状态样式                                                         | 已收敛至 `dp-*` 组件 + `tokens.uts` | 无重复                                                                               |
| 平台常量                                                                   | 由 `check:ui` + 平台契约文档约束    | 见 `FINAL_PLATFORM_MATRIX.md`                                                        |

**复杂度**：

- `core/src` 无巨型 switch、无 God service、无 Mega repository 迹象；`check-architecture.mjs`（48 files）通过分层与循环依赖检查。
- 未进行「为漂亮而重构」的改动。本轮唯一结构改动为 §7 的死导出删除。

---

## 9. 命名

低信息命名扫描（`tmp` / `data2` / `final2` / `test1` / `newData` / `foo` / `bar` / `xx` / `obj` / `result2`）于 `core/src`：

- 命中 **18 处，全部为 `obj`**，分布于 `crypto/depmap.ts`、`crypto/jcs.ts`、`repositories/dependency-repository.ts`、`repositories/group-repository.ts`。
- **处置**：使用词边界安全替换（`\bobj\b`，不匹配 `'object'`）重命名为 `record`。
- **验证**：`\bobj\b` 残余 0；`'object'` 4 处完好；无 `recordect` 之类损坏；`tsc` / `eslint` / `prettier` / 聚焦测试全部 PASS。
- 测试内临时变量按规则豁免，未改动。

文件命名 / 目录命名 / Domain 命名 / 测试文件命名 / fixture 命名：均遵循既有约定（`kebab-case` 文件、`*.test.ts` 测试、`csv-*`/`ofx-*` fixture），无违规。

---

## 10. 架构与循环依赖

`node scripts/check-architecture.mjs` → `architecture check PASS (48 files scanned, circular dependencies = 0)`。

保持的分层约束（本轮未破坏）：Domain 不依赖 UI；Core 不依赖 native platform；UI 不直接 SQLite；UI 不实现 PlanReadiness / Impact；UI 不检测 Drift；UI 不直接改 graphRevision；Adapter 不创建 Reality；Timeline 不写 Graph；ScenarioTemplate 不写 Dependency；Candidate 不进入 Impact；Verification suggestion 不改 Reality；Crypto 不依赖 UI；Migration 不依赖 UI。

UI 侧另由 `check-ui.mjs` U4（页面禁止直连 SQLite）与 U3（用户可见文案禁工程词）双重约束。

---

## 11. TODO / 占位符审计

| 范围                                   | `TODO` / `FIXME` / `HACK` / `PLACEHOLDER` / `not implemented` / `coming soon` |
| -------------------------------------- | ----------------------------------------------------------------------------- |
| `core/src`                             | **0**                                                                         |
| `app/`                                 | **0**                                                                         |
| `core/src` + `app/` 内 `mock` / `demo` | **0**                                                                         |

无 Release 阻断级 TODO 残留；无需隐藏项。

---

## 12. 结论

**CODE_STYLE_READY = PASS**
**TYPE_SAFETY_READY = PASS**
**ARCHITECTURE_READY = PASS**

本轮代码质量侧实施的真实改动共 5 类：

1. 新增 docs 格式门禁（`.prettierrc.json` + `format:docs` / `format:docs:check` + 纳入 `check` 链），并以 `git diff -w` 分类证明零语义改动；
2. 删除 3 个死导出；
3. 重命名 18 处 `obj` → `record`；
4. `.gitignore` 补齐 Gradle/Android 本地状态条目；
5. 上述改动全部经 `npm run check` → **453/453，EXIT=0** 复验。
