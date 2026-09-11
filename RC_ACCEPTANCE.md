# RC_ACCEPTANCE.md — RC 收口验收（2026-09-12 更新）

> `[x]` 仅代表有可复现证据。真实数据 Gate 本轮固定 NOT_RUN。

## A. Static Quality

- [x] format PASS（`npm run format:check`，prettier 3.9.6，0 issues）
- [x] lint PASS（`npm run lint`，eslint 10 typed rules，**0 errors / 0 warnings**）
- [x] typecheck PASS（`npm run typecheck`，strict + noUncheckedIndexedAccess + noImplicitReturns，0 errors）
- [x] architecture PASS（`npm run check:architecture`，27 files，0 violations）
- [x] TODO release blockers = 0（docs/TODO_AUDIT.md）

## B. Tests

- [x] all existing tests PASS（166/166，0 skip——见 docs/TEST_REPORT_RC.md）
- [x] determinism PASS（Impact/Parser/Proposal/Checklist 各 50 次重复一致）
- [x] idempotency PASS（migration×50、重复导入、accepted 重放、retire×5、export/import 深度等价）
- [x] negative/error paths PASS（parser/resolver/proposal/repository/impact/crypto 共 22 负向用例）
- [x] property/fuzz smoke PASS（随机图性质×3、CSV mutation×100、指纹性质×20、容器 mutation×30）
- [x] performance smoke PASS（docs/PERFORMANCE_SMOKE.md，9 项含 AF 大规模）

## C. Security / Privacy

- [x] secret scan PASS（check:secrets，0 production secrets；docs/SECRET_SCAN_REPORT.md）
- [x] logging audit PASS（docs/LOGGING_AUDIT.md：console 0、敏感模式 0）
- [x] crypto RC audit PASS（docs/CRYPTO_RC_AUDIT.md：重跑全绿 + 2 项发现已处置/记录）
- [x] privacy dataflow audit PASS（docs/PRIVACY_DATAFLOW_AUDIT.md）
- [x] fail-closed audit PASS（docs/FAIL_CLOSED_AUDIT.md，9 项逐条证据）
- [x] network audit PASS（docs/NETWORK_AUDIT.md：business calls=0，无 INTERNET 权限）

## D. Data / DB

- [x] migration replay PASS（×50）
- [x] DB integrity PASS（tests/unit/db-integrity.test.ts：foreign_keys/孤儿/组成员/rollback）
- [x] no orphan logical refs（checkGraphIntegrity 正反用例）
- [x] import transactionality PASS（AE 注入失败→零残留→重试安全）
- [x] backup/import rollback PASS（graph-serialize 原子导入 + unsupported version 拒绝）

## E. Dependencies / License

- [x] dependency tree valid（npm ls）
- [x] lockfile clean（npm ci 一致）
- [x] vulnerability audit executed（npm audit：3 moderate 全在 vitest 开发工具链，runtime 0；记录不升级，docs/DEPENDENCY_AUDIT.md）
- [x] THIRD_PARTY_NOTICES updated

## F. Clean Environment

- [x] clean install PASS（rm node_modules + npm ci）
- [x] full check after clean install PASS（npm run check 全链路）
- [x] clean clone simulation PASS（docs/CLEAN_CLONE_REPORT.md）

## G. UI

- [x] 11 pages source audited（docs/UI_SOURCE_AUDIT.md）
- [x] no production mock data
- [x] empty/loading/error paths reviewed
- [x] Proposal/Reality distinction preserved（数据源分离）
- [x] destructive final action last
- [x] accessibility source audit complete（SOURCE_AUDITED；无真机不标 DEVICE_VERIFIED）

## H. Platform

### Android
- [x] STATIC_AUDITED（manifest/permission/crypto 常量/DDL 逐项）
- [ ] COMPILED（Blocker B1：无 JDK17/javac/SDK/Gradle）
- [ ] TESTED（golden 测试 TEST READY 未运行）
- [ ] DEVICE_VERIFIED（需设备）

### HarmonyOS
- [x] STATIC_AUDITED
- [ ] COMPILED（Blocker B2：无 DevEco/hvigor/SDK）
- [ ] TESTED
- [ ] DEVICE_VERIFIED

### iOS
- [x] STATIC_AUDITED
- [x] Mac handoff complete（docs/IOS_MAC_HANDOFF.md）
- [ ] COMPILED（Blocker B3：无 macOS/Xcode）
- [ ] DEVICE_VERIFIED

工具链/设备缺失项保持未勾选并写 Blocker。

## I. Real Data

- Correctness Gate: **NOT_RUN**
- Value Gate: **NOT_RUN**

## J. Final

- [x] WORK_STATUS updated
- [x] BLOCKERS updated
- [x] MVP_ACCEPTANCE updated truthfully
- [x] MVP01_RC_AUDIT_REPORT generated
- [x] git diff --check PASS
- [x] production secrets = 0
- [x] real user data tracked = 0
- [x] MVP01_DEV_CLOSEOUT verdict written
