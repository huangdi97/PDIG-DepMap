# MVP01_RC_AUDIT_REPORT.md — MVP01 DEV CLOSEOUT / RC 审计报告

> 执行：ZCode / GLM-5.3-Flash · 2026-09-12
> 依据：GOAL_MVP01_RC_AUDIT.md · RC_AUDIT_RULES.md · QUALITY_GATES.md · RC_ACCEPTANCE.md · AGENTS.md
> 本轮类型：质量收口（无新功能）；Real Data Gate 固定 NOT_RUN

## 最终判定

```text
MVP01_DEV_CLOSEOUT = PASS        （当前环境可执行 Gate 全部完成；平台编译类如实 NO）
CORE_QUALITY       = PASS        （format/lint/typecheck/architecture/TODO 全绿）
FORMAT             = PASS        （prettier 3.9.6，TS/JS/JSON/MD/YAML；.uvue/.uts 排除）
LINT               = PASS        （eslint 10 typed，0 errors / 0 warnings）
TYPECHECK          = PASS        （tsc strict+noUncheckedIndexedAccess+noImplicitReturns，0 errors）
TESTS              = PASS        （166/166，0 failed，0 skipped）
SECURITY_AUDIT     = PASS        （logging/crypto/privacy/fail-closed/network 五项审计全绿）
SECRET_SCAN        = PASS        （0 production secrets，160+ files）
CLEAN_INSTALL      = PASS        （rm node_modules → npm ci → npm run check 全绿）
CLEAN_CLONE        = PASS        （git clone → npm ci → npm run check 全绿）
ANDROID            = IMPLEMENTED / STATIC_AUDITED / COMPILED=NO / TESTED=NO / DEVICE_VERIFIED=NO / STORE_READY=NO（B1/B4/B5）
HARMONY            = IMPLEMENTED / STATIC_AUDITED / COMPILED=NO / TESTED=NO / DEVICE_VERIFIED=NO / STORE_READY=NO（B2/B6/B7）
IOS                = IMPLEMENTED / STATIC_AUDITED / Mac handoff complete / COMPILED=NO / TESTED=NO / DEVICE_VERIFIED=NO / STORE_READY=NO（B3/B8/B9）
REAL_DATA          = NOT_RUN     （Correctness Gate=NOT_RUN，Value Gate=NOT_RUN）
```

## 精确计数

| 项                       | 数值                                                  |
| ------------------------ | ----------------------------------------------------- |
| 涉及文件（secrets 扫描） | 181                                                   |
| 架构扫描文件             | 27（core/src）                                        |
| 测试文件 / 测试用例      | 15 / **166**（pass 166 / fail 0 / skip 0）            |
| lint errors / warnings   | 0 / 0                                                 |
| type errors              | 0                                                     |
| TODO release blockers    | 0                                                     |
| production secrets       | 0                                                     |
| 直接依赖                 | 10（runtime 1：hash-wasm；dev 9）                     |
| 本轮 commits（RC 轮）    | 7（9e0a9cc…fce1e9d 区间内 7 个 RC 提交；仓库总计 18） |
| 敏感日志命中             | 0                                                     |
| 网络调用（业务）         | 0                                                     |

## 逐 Gate 证据索引

| Gate                  | 证据                                                                                                                                  |
| --------------------- | ------------------------------------------------------------------------------------------------------------------------------------- |
| repo audit            | RC_PRE_AUDIT.md + git diff --check PASS                                                                                               |
| format/lint/typecheck | `npm run check` 全链路（prettier/eslint/tsc 输出 0 issue）                                                                            |
| 全部测试              | docs/TEST_REPORT_RC.md（15 文件 ×166 用例明细）                                                                                       |
| determinism           | tests/unit/determinism.test.ts（4×50 次重复）                                                                                         |
| idempotency/replay    | tests/unit/idempotency.test.ts（migration×50 等 5 组）                                                                                |
| negative/error paths  | tests/unit/negative.test.ts + crypto-negative.test.ts                                                                                 |
| security/privacy      | LOGGING/PRIVACY_DATAFLOW/FAIL_CLOSED/NETWORK/CRYPTO_RC 五份审计                                                                       |
| secret scan           | docs/SECRET_SCAN_REPORT.md（脚本 check:secrets）                                                                                      |
| dependency/license    | docs/DEPENDENCY_AUDIT.md + THIRD_PARTY_NOTICES.md（npm outdated/audit/ls 实跑）                                                       |
| architecture          | core/scripts/check-architecture.mjs（domain 纯净/无 app-平台依赖/node:sqlite 隔离）                                                   |
| TODO/dead-code        | docs/TODO_AUDIT.md（0 blocking）                                                                                                      |
| clean install / clone | docs/CLEAN_CLONE_REPORT.md（两者均 166/166 复现）                                                                                     |
| docs verification     | README 全部命令实跑（含 validate-real-bill CLI 合成账单）                                                                             |
| UI/platform source    | docs/UI_SOURCE_AUDIT.md（T+U）、PERMISSION_AUDIT、NETWORK_AUDIT、CROSS_PLATFORM_CONTRACT_AUDIT                                        |
| 平台编译尝试          | docs/ANDROID_TOOLCHAIN_SETUP.md、HARMONY_TOOLCHAIN_SETUP.md、IOS_MAC_HANDOFF.md                                                       |
| 性能                  | docs/PERFORMANCE_SMOKE.md（L+AF，9 项）                                                                                               |
| 覆盖率                | docs/COVERAGE_REPORT.md（crypto 98.7% / impact 92.4% / parser 96.9% / repos 93.3% / services 93.6% / schema 100% / fingerprint 100%） |

## 本轮发现并修复（correctness/security/interoperability，RC 规则允许）

1. **解析器日历校验缺失**：`2026-02-30` 曾被接受 → UTC 往返校验修复（负向测试覆盖）。
2. **CR-only 换行不解析**：split 改为 `\r\n|\r|\n`（混合换行用例）。
3. **导入非事务性**：finalize 的指纹/建议/证据写入包进单事务；注入失败用例证明零残留 + 重试安全。
4. **Schema v1 加固**：dependencies/proposals/groups 补 capability/relation CHECK（CHECK 曾可绕过）。
5. **KDF 错误包装**：空口令等底层错误 → `DepmapError('kdf')`（fail-closed 错误模型）。
6. **TS 参数属性重构**：12 处 constructor 参数属性 → 显式赋值，使 README 的
   `node --experimental-strip-types` 命令真实可跑（docs command verification 发现）。
7. **base64 非规范编码 no-op 发现**（fuzz）：tag 末位字符低 4 位不影响解码字节 →
   语义恒等，非 fail-open；记录为 V2 候选加固（拒绝非规范 base64）。
8. **npm audit**：3 moderate 全部在 vitest 开发工具链（GHSA-82fw-gwwq-j7x9），
   runtime 依赖 0 影响；修复需 vitest 5 breaking 升级 → 记录，MVP02 处理。

## 平台矩阵（分别声明）

| 平台         | IMPLEMENTED | STATIC_AUDITED | COMPILED     | TESTED       | DEVICE_VERIFIED | STORE_READY |
| ------------ | ----------- | -------------- | ------------ | ------------ | --------------- | ----------- |
| Android      | YES         | YES            | **NO**（B1） | **NO**（B1） | NO              | NO          |
| HarmonyOS    | YES         | YES            | **NO**（B2） | **NO**（B2） | NO              | NO          |
| iOS          | YES         | YES            | **NO**（B3） | **NO**（B3） | NO              | NO          |
| Core（Node） | YES         | YES            | YES          | **YES**      | N/A             | N/A         |

## 阻断项（exact blockers，均在 BLOCKERS.md）

- B1：无 JDK17/javac/Android SDK/Gradle（安装步骤：docs/ANDROID_TOOLCHAIN_SETUP.md）
- B2：无 DevEco Studio/HarmonyOS SDK（docs/HARMONY_TOOLCHAIN_SETUP.md）
- B3：无 macOS/Xcode（docs/IOS_MAC_HANDOFF.md，含 1 天接入计划）
- B10：无 HBuilderX（uni-app x UI 编译）
- B4–B9/B11–B13：签名/账号/最终包名/隐私 URL/真实账单（发布前用户输入）

## 下一步（解锁顺序）

1. B1 → `platforms/android` golden 测试 + debug/release 编译 → COMPILED/TESTED
2. B10 → HBuilderX 基座 → UI 编译 + Android 真机 security spike → DEVICE_VERIFIED
3. B3 → swift test + 互操作 → iOS COMPILED/TESTED
4. B13 → 真实账单 → Correctness/Value Gate（本轮 NOT_RUN 的两项）
