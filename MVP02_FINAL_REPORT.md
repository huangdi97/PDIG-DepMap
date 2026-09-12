# MVP02_FINAL_REPORT.md

> 生成：2026-09-13（ZCode 接力收口轮）
> 分支：`feat/mvp02-global-source`；基线 `482e545`（WorkBuddy 253 tests）→
> 本轮 `7a68887`（A 段补测 + 性能 smoke）→ 文档/报告 commit
> 执行者链：ZCode → WorkBuddy → ZCode
> 结论口径：只能 PASS / FAIL / BLOCKED / NOT_RUN

## 总判定

**MVP02_GLOBAL_SOURCE_ABSTRACTION = PASS**（Core/Node 范围；
Real Data 与三端编译按既定口径保持 NOT_RUN/BLOCKED，不影响本判定）

## Gate 结果

| Gate | 判定 | 证据 |
|---|---|---|
| MVP02_GLOBAL_SOURCE_ABSTRACTION | **PASS** | 本报告全部子项 |
| SCHEMA_V2 | **PASS** | migration.test.ts 17 用例（T1–T6、回滚、MIGRATIONS=[1,2]）；`meta.schema_version=2` |
| V1_TO_V2_MIGRATION | **PASS** | T3/T4/T4b（空库/有数据/重启后 ×50 零漂移）；注入失败回滚；legacy WeChat 确定性实例；无 ID 丢失 |
| SOURCEINSTANCE | **PASS** | source-instance-scope.test.ts 12 用例：同 adapter 多实例、retired 保留 provenance、lastIngestedAt、provider/account 元数据、无 secret 字段 |
| FINGERPRINT_SCOPE | **PASS** | UNIQUE(source_instance_id, fingerprint_version, fingerprint)（T5 DDL 断言）；HMAC(adapterId:sourceInstanceId:sourceTxnId)；同实例重复/跨实例隔离；legacy WeChat dedupe 保持（T5） |
| MULTISOURCE_EVIDENCE | **PASS** | evidenceRefs[] join 表；流级 provenance/count/时间范围；计数不相加；单流自足重提阈值（K1b/K1c） |
| SOURCE_ADAPTER_CONTRACT | **PASS** | types.ts 契约 + assertFileAdapterContract 强制 event_stream/authoritativeFor=[]/statement_file；H0/H0b |
| WECHAT_REGRESSION | **PASS** | WeChat 为普通 Adapter（src/sources/wechat/adapter.ts）；domain/impact/schema 无 source==wechat 业务分支；MVP01 WeChat 全部用例 PASS |
| GENERIC_CSV | **PASS** | 20 用例/10 fixtures（US/EU/debit-credit/BOM/quoted/CRLF/CR-only/bad rows/missing mapping/multi-currency/×50）；显式 mapping，无 LLM；10k rows 64ms |
| OFX_QFX | **PASS** | 15 用例/8 fixtures（FITID/fallback/invalid date/malformed/QFX/同 FITID 跨实例/×50）；10k 67ms |
| RELATION_REGISTRY | **PASS** | src/domain/relation-registry.ts：runtime 仅 funding_source/merchant_agreement；fromKinds/toKinds/capability/group 模式/default criticality=unknown/verificationPolicy=user_only；Proposal/Dependency 写前校验 |
| MULTISOURCE_E2E | **PASS** | multi-source-e2e.test.ts 9 用例：CSV+OFX → 1 Proposal/2 refs → 确认一次 → 1 Dependency（verificationBasis=user_confirmed 回读）→ Impact → 清单；Group/backup 仅用户确认；×20 确定性 |
| DEPMAP_CONTAINER_V1_COMPAT | **PASS** | 容器协议不变（golden/负向/tamper 全 PASS）；payload v2 往返/幂等/原子失败；v1 payload in-memory migrate（J2）；不支持版本 fail-closed |
| MVP01_REGRESSION | **PASS** | 全量 259/259（MVP01 存量 166 全部在内）：Schema/Impact/WeChat Parser/Fingerprint/Proposal/Group/Synthetic E2E/Crypto |
| QUALITY_GATES | **PASS** | format:check / lint（0 err）/ typecheck（strict 0 err）/ test 259/259 / architecture（35 files）/ secret scan（239 files, 0 secrets）/ clean install（npm ci→check 全绿）/ clean clone（temp clone→npm ci→check 全绿） |
| SECURITY_PRIVACY | **PASS** | raw CSV/OFX 不持久化；sourceTxnId 不明文（仅 HMAC 指纹）；日志无 raw 行；Evidence 仅聚合；SourceInstance 无 secret；`.depmap` 加密不变；业务网络调用 0 |
| REAL_DATA | **NOT_RUN** | 等真实账单（B13）；core/scripts/validate-real-bill.ts 就绪 |

## 本轮（ZCode 接力）完成

1. 恢复 WorkBuddy 现场并生成 `ZCODE_REHANDOFF_AUDIT.md`（A–G 分节实跑审计）
2. 验证两个未提交测试文件并修复 3 类质量门错误（prettier/eslint ×2/
   no-base-to-string/tsc NodeKind），提交 `7a68887`：
   - migration.test.ts +T4/T4b/T5（A 段补测闭环；T6 原已存在、T10=J2）
   - performance-smoke.test.ts +3 MVP02 性能 smoke（Next 第 4 项闭环）
3. verificationBasis 调查闭环：写入回读断言已存在（e2e:287 + migration:265）
4. `MVP02_ACCEPTANCE.md` 按证据勾选；`WORK_STATUS.md` 更新
5. docs/ 八份 MVP02 文档（SOURCE_ARCHITECTURE / SOURCE_INSTANCE / SCHEMA_V2 /
   MIGRATION_V1_V2 / GENERIC_CSV_ADAPTER / OFX_QFX_ADAPTER /
   MULTISOURCE_EVIDENCE / TEST_MATRIX_MVP02）

## 累计测试资产

259/259 PASS（21 文件，0 skip）= MVP01 存量 166 → SourceInstance 12 →
Coverage 7 → Generic CSV 20 → OFX/QFX 15 → payload v2/v1 17 →
multi-source E2E 9 → A 段补测 3 → MVP02 perf 3 → （perf/迁移既有用例含其中）。
另：4 个生产缺陷修复均带回归测试（matchFormat 转义、positiveDirection、
parseOfxAmount('')→0、批内重复指纹假冲突）。

## 遗留与边界

- Real Data Correctness / Value Gate：**NOT_RUN**（B13，等用户提供真实账单；
  禁止 synthetic 冒充）。
- 三端编译：B1（Android/JDK17/SDK）、B2（HarmonyOS/DevEco）、B3（iOS/macOS/Xcode）、
  B10（HBuilderX）保持 BLOCKED —— 外部工具链，不阻塞 Core MVP02。
- 性能 smoke 数值为本机记录（10k CSV 64ms / 10k OFX 67ms / 3 实例 4.6s），
  阈值宽松仅防退化（docs/PERFORMANCE_SMOKE.md 口径）。
- 未进入 NEXT_BACKLOG（MVP03 / PDIG v1.1 / ChangePlan rebase 等）。

## 平台状态声明（AGENTS §19）

| 平台 | IMPLEMENTED | STATIC_AUDITED | COMPILED | TESTED | DEVICE_VERIFIED | STORE_READY |
|---|---|---|---|---|---|---|
| Android | YES | YES | NO（B1） | NO（B1） | NO | NO |
| HarmonyOS | YES | YES | NO（B2） | NO（B2） | NO | NO |
| iOS | YES | YES | NO（B3） | NO（B3） | NO | NO |
| Core（Node） | YES | YES | YES | **YES（259/259）** | N/A | N/A |
