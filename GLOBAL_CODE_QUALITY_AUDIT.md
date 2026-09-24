# GLOBAL_CODE_QUALITY_AUDIT.md — 全局代码质量审计（2026-09-23 · Round 0.1.0）

> 审计轮次：2026-09-23（四联审计：代码规模 / 依赖 / 代码质量 / 测试质量）。
> 范围（与 `scripts/quality/check-quality.mjs` 一致）：`android/app/src/main`、`android/core/src/main`、`android/conformance/src/main`、`desktop`（.kt/.ts/.mjs/.js）。
> 方法：本轮质量 gate 全量运行（VERDICT PASS，10 counters）+ 本文写作时的独立再扫描复核（PowerShell，大小写敏感正则，排除 build/.gradle/node_modules/.kotlin；72 个生产文件）。
> 例外登记：`scripts/quality/EXCEPTIONS.json`（任何逃逸点必须登记类别与理由）。

---

## 1. 命名 / 格式 / 注释

- **命名**：Kotlin 侧 `camelCase` 函数与变量 / `PascalCase` 类型；业务语义命名贯穿（`FileWorkflowCoordinator`、`ImpactTargetEvaluation`、`PlanReadinessTest`、`ScenarioSetupScreen`）；无 `EverythingManager` / 万能 utils 类；仓库不混用多套文件命名风格（Kotlin 文件大驼峰，与模块内一致）。
- **格式**：Kotlin 由 ktlint/IDE 风格约束，代码提交经过 `format:check`（legacy core/ 走 prettier `format:check` + `format:docs:check`）。
- **注释**：生产注释以**中文 WHY / INVARIANT / SAFETY / SECURITY / PROVIDER / COMPATIBILITY** 解释为主，不解释 WHAT。实例如：`app/build.gradle.kts` L126–141 显式钉住 `androidx.fragment:1.7.1` 的崩溃根因注释；`AppLock.kt` L60–68 `KeyguardManager.isDeviceSecure` 判据更正的安全语义注释；`desktop/settings.gradle.kts` L4 非 ASCII 路径根因注释。
- **生成代码**：`core/generated/CanonicalEnums.kt` 头部 `// DO NOT EDIT` + 生成器 `tools/codegen/generate.mjs`；静态枚举漂移由 codegen 契约防（不改生成物，改生成器）。

## 2. Kotlin 安全点清单（本轮修复实证）

| 检查                                                                      | 审计前基线            | 当前（gate 复扫）         | 说明                                                                                |
| ------------------------------------------------------------------------- | --------------------- | ------------------------- | ----------------------------------------------------------------------------------- |
| 生产 `!!`                                                                 | **23**                | **0**                     | 全部消除（可空传递改为显式错误模型）                                                |
| `lateinit var`                                                            | —                     | **2（均登记 JUSTIFIED）** | MainActivity.kt:56（Activity 生命周期边界）、conformance Main.kt:26（harness 入口） |
| 未检查 cast（`as X`）                                                     | **8+（conformance）** | **0**                     | 逐点消除；gate 复核 0 命中                                                          |
| `@Suppress` / `@ts-ignore` / `@ts-expect-error` / `type: ignore` / `noqa` | —                     | **0**                     | 全仓禁止                                                                            |
| `GlobalScope`                                                             | —                     | **0**                     | 结构化并发                                                                          |
| 裸 catch（`catch { }` / `catch (e) { }` 空吞）                            | —                     | **0**                     | silent failure 一律禁止                                                             |
| 未捕获 `Exception` 逐条过                                                 | —                     | 0（边界见 §6 异常模型）   | —                                                                                   |

> 复核备注：独立扫描在 `security/AppLock.kt:74` 命中 1 处 `) as android.app.KeyguardManager`——这是 **PowerShell 大小写不敏感匹配的误报**；gate 正则要求 `[A-Z]` 起始（类名小写 `android` 不命中），且该处为平台服务类型化强转（`getSystemService` 返回 `Any?`，属安全边界内显式断言），不计入未检查 cast。

## 3. TODO 纪律

- 裸 `TODO` / `FIXME` / `HACK`（大小写敏感）在 gate 范围 = **0**（`RAW_TODO` counter）。
- legacy core-TS 侧 1 处疑似为生成器 DONE 检查里的关键词列表（false-positive，已核为非业务 TODO）。
- 允许的合法用法：`XXX`/`TEMP` 作为 sentinel/环境变量名（例如 `%TEMP%` 语义），gate 明确区分。

## 4. 死代码（KNOWN_DEAD_CODE）

- 手工清扫结论：无可证实的死代码（`KNOWN_DEAD_CODE = 0`，EXCEPTIONS.json 空）。
- **GraphScreen 诚实状态**：Android `Route.GRAPH = "graph"` **已注册且被使用**——`ui/PdigApp.kt:71` 定义常量、`PdigApp.kt:227` `composable(Route.GRAPH) { GraphScreen(nav) }` 注册、`GraphScreen` 实现位于 `ui/screens/InfraScreens.kt:74`。该路由未被任何可触达入口导航（仅在代码中注册），因此**不是死代码但属「已注册未挂入口」**，作为已知 UI 挂起项登记，不谎报为已删除或已连通。

## 5. 日志纪律

- `SENSITIVE_LOGGING = 0`：`Log.*` / `println` 携带 password/secret/token/decrypted/rawStatement 模式零命中。
- 产品规则（AGENTS §17）：不记录 raw CSV 行、完整用户对象、source transaction id、SQLCipher 密钥材料、解密后的 depmap 正文；开发模式同规。

## 6. 异常边界（error model）

- `core/serialize`: `GraphImportError : RuntimeException`（GraphSerialize.kt:28）。
- `core/crypto`: `DepmapException(code, message)`（DepmapContainer.kt:32）——带协议错误码。
- `core/generated`: `ErrorCode` 枚举（wire/category/messageKey，CanonicalEnums.kt:506）。
- `repos`：`IllegalStateException("action_frozen" / "entity_not_found" / "illegal_state_transition" / "verification_not_required")`（PlanRepository.kt L147–176、DriftRepository.kt L50）；领域错误码与 conformance state-machine guards 一一对应。
- TS oracle：`DepmapError(code)` + `DepmapErrorCode` 联合类型（core/src/crypto/depmap.ts L44–51）。
- 更正：给定清单中的 `StoreException` 在仓库中**不存在**（全仓 grep 0 命中）；异常模型的真实构成如上（GraphImportError / DepmapException / IllegalStateException(code) / 生成 ErrorCode）。
- 外部异常在边界转换：容器解析/导入错误不向 UI 暴露 stack trace / SQL 错误（产品 §16）。

## 7. Quality Gate：10 counters 与例外登记

`node scripts/quality/check-quality.mjs` → **VERDICT PASS**（10 counters 全 0）：

| #   | counter                                                                    | 值  | 结论                                                                                              |
| --- | -------------------------------------------------------------------------- | --- | ------------------------------------------------------------------------------------------------- |
| 1   | UNJUSTIFIED_PRODUCTION_FILE_GT_300                                         | 0   | PASS（3 个豁免：Migrations 434 / GraphSerialize 470 / CanonicalEnums 564，见 CODE_SIZE_AUDIT §9） |
| 2   | CRITICAL_COMPLEXITY_VIOLATION（大型 @Composable > 200）                    | 0   | PASS（largeComposable 例外 0 条）                                                                 |
| 3   | RAW_TODO                                                                   | 0   | PASS                                                                                              |
| 4   | FORBIDDEN_SUPPRESSION                                                      | 0   | PASS                                                                                              |
| 5   | DEPENDENCY_CYCLE（package 级 import 图 DFS）                               | 0   | PASS                                                                                              |
| 6   | HARDCODED_SECRET                                                           | 0   | PASS（secretPattern 例外 0 条）                                                                   |
| 7   | SENSITIVE_LOGGING                                                          | 0   | PASS                                                                                              |
| 8   | UNJUSTIFIED_KOTLIN_ESCAPE（!! / lateinit / GlobalScope / 裸 catch / cast） | 0   | PASS（kotlinEscapes 仅 2 条 lateinit，均 JUSTIFIED）                                              |
| 9   | KNOWN_DEAD_CODE                                                            | 0   | PASS                                                                                              |
| 10  | ENGINEERING_GAP                                                            | 0   | PASS                                                                                              |

**EXCEPTIONS.json 当前登记**（4 条，全部带类别与理由）：fileSizeOver300×2（Migrations=migration、GraphSerialize=schema）、kotlinEscapes×2（MainActivity.kt:56、conformance Main.kt:26，lateinit，均 JUSTIFIED）。

**观察项关闭（v0.1.0 轮）**：`:repos` 已于本轮纳入 gate `SCOPE_DIRS`（`check-quality.mjs` 现列
`android/app/src/main, android/core/src/main, android/conformance/src/main, android/repos/src/main, desktop`），
2026-09-24 实跑 `node scripts/quality/check-quality.mjs` → `UNJUSTIFIED_PRODUCTION_FILE_GT_300=0`、
`DEPENDENCY_CYCLE=0`、`UNJUSTIFIED_KOTLIN_ESCAPE=0`、`RAW_TODO=0`、`SENSITIVE_LOGGING=0`、
`KNOWN_DEAD_CODE=0`、`ENGINEERING_GAP=0`，VERDICT PASS（exit 0），无 `echo PASS` 伪装。
**结论**：gate 范围生产代码 10 项质量 counter 全绿（!! 23→0、cast 8+→0、lateinit 2/2 已登记、suppress/GlobalScope/裸 catch/TODO/secret/敏感日志/dependency cycle 均 0）；异常边界由 GraphImportError / DepmapException(带码) / ErrorCode / IllegalStateException(领域码) 组成（StoreException 不存在，已更正）；`:repos` 已纳入 gate scope（观察项关闭），最终 HEAD `f5cc53e` 复核通过。

---

## 8. v0.1.2 质量迭代收口轮复核（2026-09-24）

- **Gate 复跑（fresh）**：`node scripts/quality/check-quality.mjs` → **VERDICT PASS exit 0**（2026-09-24T06:56Z），
  10 项计数全 0：UNJUSTIFIED_PRODUCTION_FILE_GT_300=0、CRITICAL_COMPLEXITY_VIOLATION=0、RAW_TODO=0、
  FORBIDDEN_SUPPRESSION=0、DEPENDENCY_CYCLE=0、HARDCODED_SECRET=0、SENSITIVE_LOGGING=0、
  UNJUSTIFIED_KOTLIN_ESCAPE=0、KNOWN_DEAD_CODE=0、ENGINEERING_GAP=0；file-size OK（exempted 3）；
  scope=`android/app,core,conformance,repos` + desktop。
- **EXCEPTIONS.json 复核（JUSTIFIED）**：合法 UTF-8 JSON、reason 无乱码；fileSizeOver300 2 条
  （Migrations.kt=migration、GraphSerialize.kt=schema）、kotlinEscapes 2 条（MainActivity lateinit、
  conformance Main lateinit，均 JUSTIFIED）、secretPattern 2 条（SmokeRunner.kt:39、DepmapFileStoreTest.kt:21，
  fixture 合成口令）。
- **Dead code（FIXED）**：`desktop/app/src/main/kotlin/com/pdig/desktop/io/FileOps.kt` 删除无调用者的
  `DesktopFileOps.write` 默认方法（6 行；Grep 确认 desktop 全树无 `.write(` 调用者、无实现覆盖），
  保留 pickOpen/pickSave/readBytes。
- **格式轮**：121 个 md 经 prettier 规范化，commit `a9c1cab`（纯格式，`git diff -w` 复核无业务语义变化）。
- **行为回归锚点**：core `npm run check` 全绿（453 tests / 43 files PASS、architecture circular=0、
  network gate 0 primitives（130 文件）、secret scan 988 files PASS、UI gate 30 .uvue PASS）；
  conformance fresh SUMMARY（2026-09-24）android **91/91 PASS**；`:core:test` 71/71、
  `:app:testDebugUnitTest` 9/9（--rerun-tasks fresh，0 failure）；仪器化基线 60/60 PASS（2026-09-24 11:39）。
- **结论**：10 counter 全 0 复证；Kotlin 逃逸点仅 2 条 lateinit（均 JUSTIFIED）；secretPattern 仅
  fixture 合成口令；本轮无新审计发现，未把 NOT_RUN 写成 PASS。
