# DepMap / 个人数字依赖图

> 换卡、换号、换邮箱之前，先看清哪些账户、扣款和恢复路径会跟着受影响。

内部代号 **DepMap**。MVP 唯一核心 Job：**模拟更换 / 注销一张银行卡**，输出 Action Checklist。

## 当前状态（真实，不虚报）

| 部分                                                                                                                                                                  | 状态                                                                                                                                                                           |
| --------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| 共享 Core（schema v3/repositories/migration/impact/parser/resolver/proposal/crypto/sources/**living graph**）                                                         | **TESTED — 453 tests PASS，typecheck 干净（Node 22）**                                                                                                                         |
| Living Graph（graphRevision / ChangePlan+Rebase / PlanReadiness / ScenarioCoverage / RealityDrift / DiscoveryCandidate / ScenarioTemplate / Timeline / Verification） | **TESTED**（docs/LIVING_GRAPH.md；MVP03_FINAL_REPORT.md）                                                                                                                      |
| 工程基线 Engineering Baseline V1                                                                                                                                      | **PASS**（format/lint/typecheck/architecture+circular=0/network=0/secrets/invariants/contract/property/mutation baseline/clean install/clean clone，见 `QUALITY_GATES_V1.md`） |
| `.depmap` V1 加密容器 + Golden Vector                                                                                                                                 | **TESTED（Node 侧）**；Android/iOS 侧测试代码就绪未运行                                                                                                                        |
| uni-app x 中文 UI（24 页 + 5 个 `dp-*` 组件 + 5 个 UTS 安全插件 + 设计 token；含场景库/计划/Timeline/Drift）                                                          | IMPLEMENTED（源码完成，静态 Gate `check:ui` U1–U9 PASS）；未编译（无 HBuilderX）                                                                                               |
| Android Kotlin 安全层 + Gradle 工程                                                                                                                                   | IMPLEMENTED；未编译（无 JDK17/SDK）                                                                                                                                            |
| HarmonyOS ArkTS 适配 + 工程文件                                                                                                                                       | IMPLEMENTED；未编译（无 DevEco）                                                                                                                                               |
| iOS Swift 适配 + SPM                                                                                                                                                  | IMPLEMENTED；未编译（无 macOS/Xcode）                                                                                                                                          |
| 真实数据双 Gate                                                                                                                                                       | **NOT_RUN**（无真实账单）                                                                                                                                                      |

平台矩阵与证据：`WORK_STATUS.md` / `FINAL_REPORT.md`；外部依赖：`BLOCKERS.md`。

## 产品

- **是什么**：本地加密的“数字依赖图”。账单 → 支付路径 → 你确认 → 依赖图 → 变更影响清单。
- **不是什么**：密码管理器 / 支付钱包 / 记账软件 / 订阅管理器 / 云服务。
- **第一原则**：宁可漏报，不可把“不确定”伪装成“必须处理”。机器提出，用户确认现实。

### 已支持（MVP03）

- 多源 payment evidence（微信账单 / 通用 CSV / OFX-QFX）与 confirmed dependency graph
- graphRevision 活图谱维护 + Reality Drift 复盘（仅正向证据触发）
- 支付场景模板（更换银行卡 / 即将到期 / 注销银行卡）→ ChangePlan → 影响分析 → Rebase → PlanReadiness
- Upcoming Timeline（到期 / 计划 / 验证 / 来源刷新）与 Action Verification（done ≠ verified）

未支持（不虚报）：手机号/邮箱 capability、GitHub/Domain/Cloud 图、Open Banking、AI 自动管理。

## 第一次启动（ZCode）

先读 `START_HERE.md`，然后把 `ZCODE_FIRST_PROMPT.txt` 内容发给 ZCode。

## 本地开发

```bash
cd core
npm ci              # 受支持唯一安装方式（lockfile 锁定；engines: node >= 22.5）
npm test            # 453 tests / 43 文件（unit/contract/invariant/property/migration/crypto/living-graph/perf smoke）
npm run typecheck
npm run check       # 快速 Gate：format+lint+typecheck+tests+architecture(circular=0)+network+secrets
npm run check:full  # 完整 Gate：check + db-integrity + coverage + perf + deps/license
```

工程规范：`docs/ENGINEERING_STANDARDS.md`；完成定义：`docs/DEFINITION_OF_DONE.md`；
Agent 开发协议：`docs/AGENT_DEVELOPMENT_PROTOCOL.md`；变更风险分级：`docs/CHANGE_RISK_POLICY.md`。

真实账单验证（账单放 `local_private/`，不入库、不联网）：

```bash
cd core
node --experimental-strip-types scripts/validate-real-bill.ts --file ../local_private/<bill>.csv
```

## 目录

- `core/` — 纯 TypeScript 共享核心 + vitest（domain/schema/impact/parser/resolver/proposal/crypto）
- `app/` — uni-app x（pages + uni_modules UTS 插件 + manifest/pages.json）
- `platforms/` — Android Gradle / HarmonyOS / iOS SPM 原生工程与安全源码
- `docs/` — 工程基线：ENGINEERING_STANDARDS / ARCHITECTURE_RULES / TEST_STRATEGY / COVERAGE_POLICY / FAIL_CLOSED_MATRIX / SECURITY_PRIVACY_REGRESSION_MATRIX / DEFINITION_OF_DONE / AGENT_DEVELOPMENT_PROTOCOL / COMMIT_CONVENTION / CHANGE_RISK_POLICY / FIXTURE_POLICY / LOGGING_POLICY / MONEY_CURRENCY_RULES / DATE_TIME_RULES / MEMORY_DATA_LIFETIME / PERFORMANCE_BASELINE / DEPENDENCY_POLICY / TYPE_SAFETY_BASELINE / DEAD_CODE_AUDIT；设计：ARCHITECTURE / SCHEMA_V1 / SCHEMA_V2 / MIGRATION_V1_V2 / SOURCE_ARCHITECTURE / SOURCE_INSTANCE / GENERIC_CSV_ADAPTER / OFX_QFX_ADAPTER / MULTISOURCE_EVIDENCE / TEST_MATRIX_MVP02 / IMPACT_ENGINE / PARSER_WECHAT / CRYPTO_PROTOCOL / SECURITY_MODEL / PLATFORM_ADAPTERS / REAL_DATA_VALIDATION / STORE_RELEASE_CHECKLIST

## 设计与目标（控制文件）

- `CANONICAL_DESIGN.md` — 唯一产品/Schema/Impact 母版
- `GOAL_MVP01.md` — MVP01 执行目标（PHASE 0–15）
- `GOAL_MVP02_GLOBAL_SOURCE.md` — MVP02 执行目标（Global Source Abstraction，PHASE 0–25）
- `WORK_STATUS.md` / `BLOCKERS.md` / `MVP_ACCEPTANCE.md` / `MVP02_ACCEPTANCE.md` / `MVP03_ACCEPTANCE.md` — 状态与验收
- `FINAL_REPORT.md` / `MVP02_FINAL_REPORT.md` — 轮次结论

## 解析器覆盖矩阵

| 来源                                                                                                 | 状态                                                 |
| ---------------------------------------------------------------------------------------------------- | ---------------------------------------------------- |
| 微信支付账单（UTF-8/BOM/GB18030/说明行/退款/坏行）                                                   | 已实现并测试（synthetic）                            |
| 通用 CSV 导入（Generic CSV Adapter；用户显式列映射：US/EU/debit-credit/BOM/CRLF 等；不预设特定银行） | 已实现并测试（synthetic）                            |
| OFX / QFX 银行对账单（FITID/DTPOSTED/TRNAMT/NAME/MEMO）                                              | 已实现并测试（synthetic）                            |
| 支付宝专属解析器                                                                                     | 未实现（FUTURE.md；可先用 Generic CSV 显式映射导入） |

## 已知限制

- Impact 仅 payment capability；其他 capability 仅存边
- Group 仅 ANY/ALL；无 N-of-M
- 三端原生工程尚未经过编译验证（外部工具链缺失，见 BLOCKERS.md）

## 隐私

无后端 · 无账号 · 无 analytics · 无广告 · 无云同步。
原始账单只在导入会话内存处理；不持久化任何单笔交易；数据库全库加密；`.depmap` 备份口令加密。

## 禁止

不要把旧 IMPLEMENTATION_NOTES 放回 Workspace。
不要在 MVP01 引入 LLM / Agent / GraphRAG / Neo4j。
