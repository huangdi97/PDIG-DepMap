# WORK_STATUS.md

> 本文件由 ZCode/GLM 在执行过程中持续更新。不要删除历史关键结论。
> 2026-09-12 起由 WorkBuddy 接力（ZCode → WorkBuddy handoff），分支 `feat/mvp02-global-source`。

## Current

- Phase: **MVP02 Global Source Abstraction**（WorkBuddy→ZCode 接力收口轮）
- Status: Core 全部技术 Gate PASS（259/259）；进行中文档收口；MVP01_DEV_CLOSEOUT = PASS 保持
- Real Data Gate: NOT_RUN（固定）
- 详细报告：MVP01_RC_AUDIT_REPORT.md / WORKBUDDY_HANDOFF_AUDIT.md / **ZCODE_REHANDOFF_AUDIT.md** / MVP02_FINAL_REPORT.md

## Current quality state（ZCode 接力轮 2026-09-13 实跑）

- format:check PASS（prettier 3.9.6）
- lint PASS（eslint 10 typed，0 errors/0 warnings）
- typecheck PASS（strict 全开，0 errors）
- tests：**259/259 PASS，0 skip**（21 文件）
  - 253 基线 → +3（migration T4/T4b/T5 A 段补测）→ +3（MVP02 性能 smoke：
    10k CSV parse 64ms / 10k OFX parse 67ms / 3 SourceInstance 并发 2k×3 ≈4.6s，
    指纹命名空间隔离断言）
- architecture check PASS（35 files）；secret scan PASS（238 files，0 production secrets）
- coverage（RC 轮基线）：crypto 98.7% / impact 92.4% / parser 96.9% / repos 93.3% / services 93.6% / schema 100%

## MVP02 进度（Gate 级）

| 段 | 内容 | 状态 |
|---|---|---|
| A/B/C | Schema v2 迁移 + SourceInstance 隔离 | PASS（14 + 12 用例） |
| D | EvidenceSourceAdapter 契约 | PASS（H0/H0b） |
| E | Generic CSV Adapter（10 fixture） | PASS（20 用例） |
| F | OFX/QFX Adapter（8 fixture） | PASS（15 用例） |
| G | Multi-source Evidence provenance | PASS（K1b/K1c/K2） |
| H | Coverage Semantics（absence 不否定现实） | PASS（6 用例） |
| J | `.depmap` payload v2 + v1 in-memory migrate | PASS（17 用例） |
| K | multi-source synthetic E2E 全链路 | PASS（9 用例） |
| L | quality gates 全绿 | PASS |
| A 段补测 | T4（有数据库 ×50）/T4b（重启后 ×50）/T5（legacy dedupe 语义） | PASS（本轮，7a68887） |
| 性能 smoke | 10k CSV / 10k OFX / 3 SourceInstance 并发 | PASS（本轮，7a68887） |
| — | `MVP02_ACCEPTANCE.md` 按证据勾选 | 本轮完成 |
| — | `MVP02_FINAL_REPORT.md` | 本轮完成 |
| — | docs/ 八份 MVP02 文档 | 本轮完成 |

## 本轮修复的生产缺陷（均有回归测试）

1. `generic-csv/adapter.ts` — `matchFormat()` 永远无法匹配任何日期格式。
   token 替换成 `(\d{4})` 后又对整个字符串做正则转义，捕获组被破坏为
   `\(\d\{4\}\)`。所有映射 CSV 的每一行都静默变成 bad date，产出 0 条观测。
   改为先切分字面量/token、只转义字面量、再拼装。
2. `generic-csv/adapter.ts` — `positiveDirection` 声明了但从未生效。
   `signed` 模式硬编码符号判定，导致"消费记为正数"的发卡行导出一律方向反转。
   现按显式声明判定；零金额记为 `neutral`，绝不猜。
3. `ofx/adapter.ts` — `parseOfxAmount('')` 返回 0。
   `Number('') === 0` 且通过 `Number.isFinite`，缺失金额被伪造为 0 元交易。
4. `services/import-coordinator.ts` — 批内重复指纹触发假冲突。
   `insertBatch` 在批内去重，但 preview 只查 DB；含重复 FITID 的真实账单
   会整体导入失败。preview 现严格对齐 insertBatch 语义。

## 仓库运维注意（重要）

`feat/mvp02-global-source` 分支的 loose ref 文件（`.git/refs/heads/feat/`）
在本工作区会被外部进程反复删除，导致 git 把已有提交误判为 root commit、
分支看似"无提交"。所有 commit 对象本身始终完好。
**规避方式：分支 ref 固化在 `.git/packed-refs`（单文件，不受影响）。**
若再次出现"branch has no commits"，从 reflog 找回哈希后重写 packed-refs 即可，
**不要**执行任何 `git reset --hard` / `git clean`。

## Platform Matrix

| Platform | IMPLEMENTED | STATIC_AUDITED | COMPILED | TESTED | DEVICE_VERIFIED | STORE_READY |
|---|---|---|---|---|---|---|
| Android | YES | YES | NO（B1） | NO（B1） | NO | NO |
| HarmonyOS | YES | YES | NO（B2） | NO（B2） | NO | NO |
| iOS | YES | YES | NO（B3） | NO（B3） | NO | NO |
| Core（Node） | YES | YES | YES | YES | N/A | N/A |

## Core Gates

- Schema v1 / Migration: TESTED（幂等 ×50、回滚、CHECK 加固）
- Impact Kernel: TESTED（T1–T12 + determinism + fuzz + 1k 节点性能）
- `.depmap` V1: TESTED（Node 侧 38 crypto 用例含 golden/负向/mutation fuzz）；Android/iOS 侧 TEST READY 未运行
- WeChat Parser: TESTED（18 fixtures 用例 + mutation fuzz + 日历校验）
- Fingerprint: TESTED（HMAC/canonical-row/ordinal/跨会话去重）
- Node Resolver: TESTED
- DependencyProposal / GroupProposal: TESTED（UPSERT/重提/确认/复活）
- Synthetic E2E: TESTED（9 用例）+ 导入事务性（AE 注入失败用例）
- Correctness Gate: **NOT_RUN**；Value Gate: **NOT_RUN**

## Last completed（ZCode 接力收口轮 2026-09-13）

- 恢复 WorkBuddy 现场并生成 `ZCODE_REHANDOFF_AUDIT.md`（A–G 分节，含实跑证据）
- 验证并修复两个未提交测试文件中的 3 个质量门错误（prettier ×2、eslint no-unused-vars ×2、
  no-base-to-string ×1、tsc `kind:'merchant'`→`'service'` ×1），随 `7a68887` 提交
- A 段补测闭环：T4（已迁移且有数据的库 ×50 零漂移）/ T4b（重启后 ×50）/ T5
  （legacy 去重作用域语义 + 表级 UNIQUE DDL 断言）；T6 原已在 migration.test.ts:102、
  T10 即 J 段 J2（v1 payload in-memory migrate），无缺口
- verificationBasis 调查闭环：写入回读断言已存在（multi-source-e2e.test.ts:287
  user_confirmed + migration.test.ts:265 列默认值），无需新增
- `MVP02_ACCEPTANCE.md` 按测试证据勾选；`MVP02_FINAL_REPORT.md` 生成；
  docs/ 八份 MVP02 文档落地

## Last completed（MVP02 接力轮 WorkBuddy）

- K 段：multi-source synthetic E2E（9 用例，含 ×20 确定性、单流重提阈值、多源不产生 must_change）
- J 段：payload v2 往返/幂等/原子失败 + v1 in-memory migrate（17 用例，变异测试验证非空断言）
- 修复 4 个生产缺陷（见上）；E 段 20 + F 段 15 用例；新增 18 份 fixture
- 变异测试验证：J/K 用例在人为破坏实现后全部变红，证明非空测试

## Last completed（RC 轮）

- PHASE AM：MVP01_RC_AUDIT_REPORT.md + RC_ACCEPTANCE/QUALITY_GATES/MVP_ACCEPTANCE 更新
- PHASE AI/AJ：clean install、clean clone 模拟 PASS
- PHASE Z：README 命令实跑验证（发现并修复参数属性与 strip-types 不兼容）
- PHASE V/W/X：工具链检测（B1/B2/B3 exact blocker）+ 三份 setup/handoff 文档
- PHASE M–R：architecture/secret 脚本 + 六份安全审计文档
- 修复：日历校验、CR 换行、导入事务化、Schema CHECK、kdf 错误包装、非空断言清零

## Current failures

无失败测试。未执行项全部为外部工具链 Blocker（B1–B3/B10），不虚报。

## External blockers

见 `BLOCKERS.md`（B1–B3、B10 阻断编译；B4–B9/B11–B13 发布材料）。

## Next

1. **Core MVP02 已收口**。剩余全部为外部 Blocker 项或 Real Data：
2. B1 → Android golden 测试 + 编译（docs/ANDROID_TOOLCHAIN_SETUP.md）
3. B10 → HBuilderX 基座 → UI 编译 + 真机 spike
4. B3 → docs/IOS_MAC_HANDOFF.md
5. B13 → 真实账单双 Gate（core/scripts/validate-real-bill.ts 已验证可用；Real Data 保持 NOT_RUN 直到用户提供）
6. NEXT_BACKLOG（MVP03/PDIG v1.1）不在本轮范围
