# GOAL_MVP01_RC_AUDIT.md

> 执行模型：ZCode + GLM-5.3-Flash
> 类型：MVP01 DEV CLOSEOUT / RC-AUDIT
> 本轮真实数据 Gate：**NOT_RUN**
> 目标：不加新功能，把当前 MVP01 的代码风格、静态检查、测试、安全、依赖、构建、文档和发布前审计全部跑到当前环境允许的最高程度。

## 0. 先读

按顺序读取并遵守：

1. `AGENTS.md`
2. `CANONICAL_DESIGN.md`
3. `MVP_ACCEPTANCE.md`
4. `WORK_STATUS.md`
5. `BLOCKERS.md`
6. `RC_AUDIT_RULES.md`
7. `QUALITY_GATES.md`
8. `RC_ACCEPTANCE.md`
9. 本文件

产品/Schema/安全语义仍以 Canonical Design 为准；本文件只定义本轮收口范围。

## 1. 已知现状

当前 Core、Schema/migration、Impact、Proposal/GroupProposal、WeChat Parser、Fingerprint、synthetic E2E、`.depmap` reference/Node 测试和 UI 源码已经完成。三端适配源码存在，但 Android/Harmony/iOS 的实际编译、跨端 crypto、真机安全验证仍取决于本地工具链。本轮不运行真实微信账单。

固定：

- `REAL_DATA_CORRECTNESS_GATE = NOT_RUN`
- `REAL_DATA_VALUE_GATE = NOT_RUN`

禁止 synthetic fixture 冒充 real data。

## 2. 本轮禁止扩展

不要进入 MVP02 / PDIG v1.1；不要新增支付宝、CSV/OFX、SourceInstance、RealityDrift、IncidentPlan、Browser Discovery、Open Banking、云同步、LLM/Agent/GraphRAG、后端或账号系统。

本轮只有一个目标：**质量收口。**

## 3. 最终成功定义

当前环境能执行的下列 Gate 全部完成后，才允许：
`MVP01_DEV_CLOSEOUT = PASS`

必须覆盖：

- repo audit
- format
- lint
- typecheck
- unit/integration/parser/crypto/impact/migration/proposal tests
- synthetic E2E
- determinism
- idempotency/replay
- negative/error-path tests
- security/privacy audit
- secret scan
- dependency/license audit
- architecture boundary audit
- TODO/dead-code audit
- clean install
- clean clone simulation
- docs command verification
- UI source audit
- platform source audit
- 当前环境允许的实际 compile/test
- final RC report

平台若无工具链，只能写 `COMPILED=NO` 和 exact blocker，不得伪完成。

---

# PHASE A — RC_PRE_AUDIT

先不改代码，生成 `RC_PRE_AUDIT.md`，记录：

- git branch/HEAD/status
- Node/package manager/lockfile
- TypeScript/test/lint/format 版本
- 项目结构
- test files / test cases 数量
- `.uvue` 页面数量
- Android/Harmony/iOS 原生模块
- TODO/FIXME/HACK 数量
- `any`、`@ts-ignore`、`@ts-expect-error`、eslint-disable 数量
- console/logging 调用
- build scripts
- 当前 blockers

不得删除失败测试或大规模无关重构。

# PHASE B — Repository Hygiene

确保 `.editorconfig`、`.gitignore`、`.gitattributes`、package/lockfile、tsconfig、lint/format config 完整。
源码默认 UTF-8；保持 GBK/GB18030 fixture 原编码；规范 LF，避免 Windows CRLF 污染。

# PHASE C — Format

建立并跑：

- `npm run format:check`
- `npm run format`

至少覆盖 TS/JS/JSON/MD/YAML。`.uvue/.uts` 只有在工具明确兼容时自动格式化，禁止 formatter 破坏 uni-app x。Kotlin/Swift/ArkTS 有可用工具则跑，没有则静态审计。
最终 `format:check = PASS`。

# PHASE D — Lint

建立/运行 `npm run lint`。
目标：0 errors；warnings 逐条审计。
重点：unused、unreachable、floating promise、async misuse、switch fallthrough、unsafe any、silent catch、ignored rejection。
禁止文件级全局关闭规则，除非生成代码且说明原因。

# PHASE E — TypeScript Strict

`core/` 至少：

- `strict=true`
- `noImplicitAny=true`
- `noImplicitReturns=true`
- `noFallthroughCasesInSwitch=true`

若可安全开启 `noUncheckedIndexedAccess` / `exactOptionalPropertyTypes`，开启并修复；若与 UTS 类型系统冲突，则 Core 单独严格，不降低 Core 标准。
最终 `npm run typecheck = PASS`。

# PHASE F — Type Escape Audit

扫描 `any / as any / unknown as / @ts-ignore / @ts-expect-error / non-null !`。
生成 `docs/TYPE_SAFETY_AUDIT.md`。
Impact/crypto/parser/fingerprint/proposal/repository/migration 关键路径不得靠 unsafe cast 绕过正确性。

# PHASE G — Full Test Suite

运行全部已有测试并记录命令、数量、耗时：

- unit
- integration
- parser
- crypto
- impact
- migration
- proposal lifecycle
- synthetic E2E

生成 `docs/TEST_REPORT_RC.md`。
禁止 skip、删测试、降断言。

# PHASE H — Determinism

新增/运行 deterministic tests：

- Impact 同 fixture 重复 50 次，输出一致
- Parser 同输入重复 50 次，NormalizedObservation 内容/顺序一致
- Proposal 同 observation set logical keys 一致
- 同一 graph revision + scenario 的 Checklist 顺序一致
  生成 `determinism.test` 或等价测试。

# PHASE I — Idempotency / Replay

验证：

- migration 重复 50 次安全
- 相同 statement 重复导入不重复 Evidence/Proposal/Dependency/Group
- accepted Proposal 重放不产生第二条 Dependency
- retire/reactivate 多轮 ID 稳定
- export/import 后 logical graph 等价

# PHASE J — Negative / Error Paths

补齐至少：
Parser：空文件、缺列、非法日期/金额、emoji、混合换行、重复 header、10k rows。
Resolver：alias 冲突、fuzzy 同分、空 merchant、unknown。
Proposal：missing node、invalid relation/capability、duplicate suggestion。
Repository：FK violation、rollback、duplicate logical key。
Impact：empty graph、empty unavailable、nonexistent node、all retired、deep chain、large cycle。
Crypto：empty/long password、invalid Base64、truncated JSON、invalid nonce/salt/tag、oversize、unsupported version、corrupted payload。

# PHASE K — Property/Fuzz-style Smoke

不必引入重量框架；可用 deterministic pseudo-random。

- 随机 0–100 node graph：必须终止、retired 不传播、Proposal 永不 must_change、输出 deterministic
- Golden container mutation：不 crash、fail closed、恶意 KDF 在昂贵操作前拒绝
- CSV 行 mutation：坏行计数，好行不污染

# PHASE L — Performance Smoke

只防异常退化，不做 premature optimization。
记录：

- 10k rows parse
- 10k fingerprint
- Proposal generation
- 1k-node Impact graph
- 100-node cycle
- malicious KDF rejection
  生成 `docs/PERFORMANCE_SMOKE.md`。

# PHASE M — Architecture Boundary

增加 `npm run check:architecture` 或等价脚本。
至少禁止：

- `core -> app/UI`
- `core -> Android/iOS/Harmony native`
- `domain -> concrete repository implementation`
- `crypto protocol -> UI`

# PHASE N — Dependency / License

若网络可用：

- `npm outdated`
- `npm audit`
- `npm ls`
  若网络不可用：至少 `npm ls` + lockfile consistency，并如实记录。
  生成 `docs/DEPENDENCY_AUDIT.md` 与 `THIRD_PARTY_NOTICES.md`。
  记录 direct dependency、版本、用途、license、runtime/dev；只写实际查询到的安全告警，不猜。

# PHASE O — Secret Scan

优先 gitleaks；没有则 regex+filename fallback。
扫描 tracked + working tree：
API key、private key、password/token/secret、`.env`、DB、真实账单、`.depmap` 明文、签名材料。
Golden 测试口令 `depmap-test` allowlist。
生成 `docs/SECRET_SCAN_REPORT.md`。
目标：production secret findings = 0。

# PHASE P — Logging Audit

扫描 TS/JS/Kotlin/Swift/ArkTS 日志。
不得输出 raw bill、transaction id、merchant history、DB key、fpSecret、password、fileEncryptionKey、decrypted depmap、full graph。
生成 `docs/LOGGING_AUDIT.md`。

# PHASE Q — Privacy/Dataflow

生成 `docs/PRIVACY_DATAFLOW_AUDIT.md`，证明：

- Observation/CanonicalEvent 不持久化
- raw statement 不进入 DB
- Evidence 不保存交易流水
- local_private ignored
- export 默认 encrypted
- no analytics/telemetry/ads/backend
  把 source→memory→fingerprint/evidence→proposal→confirmed graph 的每一步标注 memory/persistent/encrypted/discarded。

# PHASE R — Crypto RC Audit

重跑：
Golden vector、wrong password、tag/header tamper、KDF bounds、invalid base64/truncated/unsupported version。
审计：
CSPRNG、nonce、secret logging、authenticate-before-trust、decrypt 后 schema validation。
生成 `docs/CRYPTO_RC_AUDIT.md`。
不得无必要修改 DEPMAP V1。

# PHASE S — Database RC Audit

检查：

- foreign_keys
- transaction rollback
- schema version
- UNIQUE/indexes
- retirement/reactivation
- group member integrity
- orphan detection
  增加 `db:integrity-test` 或等价。
  如果发现 correctness 问题，可修；不扩业务范围。

# PHASE T — UI Source Audit

不加功能，审查现有 11 页：
中文默认、无 lorem/placeholder/假生产数据、loading/empty/error/disabled、破坏性动作确认、导航、import failure、Proposal partial decision、Group reject、lock、Impact explanation、final change 最后。
严禁 UI 把 Proposal 显示为 confirmed reality。
生成 `docs/UI_SOURCE_AUDIT.md`。

# PHASE U — Accessibility Source Audit

静态检查：
可操作项可读标签、不只靠颜色、长中文换行、错误可理解、触控目标合理、破坏性动作明确。
只能标 `SOURCE_AUDITED`，没有真机不得标 DEVICE_VERIFIED。

# PHASE V — Android Toolchain Attempt

先检测：
`java/javac/adb/sdkmanager/gradle`。
若缺失，优先可复现安装；若需要 GUI/admin/license interaction，写 `docs/ANDROID_TOOLCHAIN_SETUP.md` 并继续其他 Gate。
若工具链可用，实际运行：

- Gradle tests
- Android golden crypto test
- debug build
- release compile（不要求真实 signing）
- manifest/permission audit
  只有真实执行后才更新 COMPILED/TESTED。

# PHASE W — HarmonyOS Toolchain Attempt

检测 DevEco/hvigor/Harmony SDK。
可用则 compile、ArkTS check、platform tests、crypto interop。
不可用则写 `docs/HARMONY_TOOLCHAIN_SETUP.md`，继续其他 Gate。

# PHASE X — iOS Source Audit

非 macOS：不伪编译。
审查 Swift/SPM/XCTest/Keychain/LocalAuthentication/privacy strings/entitlements，生成 `docs/IOS_MAC_HANDOFF.md`，要求拿到 Mac 后可直接按步骤 build/test。
若当前是 macOS，则实际执行。

# PHASE Y — Cross-platform Contract

生成 `docs/CROSS_PLATFORM_CONTRACT_AUDIT.md`。
确保三端：
DB adapter interface、crypto constants、schema constants、enum vocabulary、error codes、payload version、Base64、UTF-8/password semantics 一致。

# PHASE Z — Docs Verification

README 里的可运行命令逐条执行。不存在的补 script 或修 README。
目标：`README_COMMANDS_VERIFIED = PASS`。
平台命令若依赖外部工具链必须明确标记。

# PHASE AA — Unified Dev Commands

建立：
`npm run check`
至少组合：
format:check + lint + typecheck + test + architecture + lite secret check。

建立 `npm run check:full`：
增加 coverage/dependency/docs/performance smoke 等。

# PHASE AB — Coverage

生成 `docs/COVERAGE_REPORT.md`。
重点看 impact/crypto/fingerprint/parser/proposal/repository/migration。
禁止通过排除核心源码制造高覆盖率；不要为了数字写无意义测试。

# PHASE AC — TODO/FIXME

扫描 TODO/FIXME/HACK/XXX/TEMP/PLACEHOLDER/mock/not implemented。
生成 `docs/TODO_AUDIT.md`。
Release blocker 清零；future 移入 FUTURE.md。

# PHASE AD — Error Model / Fail Closed

审查 parser/crypto/DB/validation/platform errors。
核心错误不要靠字符串比较；不得泄密。
生成 `docs/FAIL_CLOSED_AUDIT.md` 并验证：
crypto fail 不返回 partial plaintext；auth cancel 不开数据层；migration fail rollback；import fail 不提交半成品；unresolved 不建 Dependency；未确认 Group 不推断 backup；unknown criticality => needs_review；unsupported version 明确失败。

# PHASE AE — Import Transactionality

完整 synthetic import 中途注入失败，验证无 partial persisted state、retry/duplicate retry 安全。若不 atomic，在不改业务语义前提下修。

# PHASE AF — Large Synthetic Smoke

构造非真实数据：
10k observations、500 nodes、1k dependencies、100 proposals、50 groups。
跑 import/proposal/simulate/export，记录性能与失败。

# PHASE AG — Version Matrix

生成 `docs/VERSION_MATRIX.md`：
App / schema / DEPMAP / WeChat parser / Android / Harmony / iOS 版本分别记录，不混用。

# PHASE AH — Git Audit

运行：

- `git status`
- `git diff --check`
- secret/real-data scan
  清除 temp/build junk、误入二进制、绝对本机路径。
  合理小步 commit，不强行 squash 用户历史。

# PHASE AI — Clean Install

确认 Git 工作安全后：

- 删除 node_modules
- `npm ci`（或 lockfile 对应命令）
- `npm run check`
  证明不依赖脏本机环境。

# PHASE AJ — Clean Clone Simulation

在临时目录复制/clone tracked tree：

- clean install
- `npm run check`
  确认没有漏提交必要文件、绝对路径、local_private、IDE cache 依赖。
  生成 `docs/CLEAN_CLONE_REPORT.md`。

# PHASE AK — Permission / Network / Release Config

生成：

- `docs/PERMISSION_AUDIT.md`
- `docs/NETWORK_AUDIT.md`

每个 Android/iOS/Harmony 权限都说明必要性，无用途删除。
扫描 fetch/axios/uni.request/URLSession/OkHttp/ArkTS 网络。
目标：business network calls=0，analytics=0，telemetry=0，ads=0。
Release 配置不得启用 debug menu、test key/test DB/synthetic demo data。

# PHASE AL — Backup/Restore Code Audit

验证：

- export 完整 authenticated container
- import 先完整校验再 DB mutation
- schemaVersion checked
- failure rollback
- temp plaintext 不长期留存
  条件允许采用 temp→close/fsync→rename 安全写入策略；平台 API 未验证则如实记录。

# PHASE AM — Final Acceptance

逐项更新：

- `RC_ACCEPTANCE.md`
- `QUALITY_GATES.md`
- `MVP_ACCEPTANCE.md`
- `WORK_STATUS.md`
- `BLOCKERS.md`

最终生成 `MVP01_RC_AUDIT_REPORT.md`。

报告必须包含：

```text
MVP01_DEV_CLOSEOUT =
CORE_QUALITY =
FORMAT =
LINT =
TYPECHECK =
TESTS =
SECURITY_AUDIT =
SECRET_SCAN =
CLEAN_INSTALL =
CLEAN_CLONE =
ANDROID =
HARMONY =
IOS =
REAL_DATA = NOT_RUN
```

并给出 exact counts：
files/tests/pass/fail/skip/lint errors/type errors/TODO blockers/secrets/direct deps/实际 commits。

平台矩阵必须分别写：
IMPLEMENTED / STATIC_AUDITED / COMPILED / TESTED / DEVICE_VERIFIED / STORE_READY。

# 自动执行规则

不要每个 Phase 停下来问用户。
持续：
`audit → fix → test → re-test → report → next`

只有 GUI 安装、管理员权限、SDK license、DevEco、macOS/Xcode、设备、开发者账号、签名材料等真正外部事项允许记 BLOCKER；记完继续其他工作。

允许本地 commit；不要自动发布商店、公开 Release、上传真实数据或向不确定 remote push。

如果上下文变长，更新 `WORK_STATUS.md` 后 compact/续任务；新任务先读 WORK_STATUS、RC_ACCEPTANCE，从第一个未完成 Gate 继续。

## 现在开始

立即执行：

1. 写 `RC_PRE_AUDIT.md`
2. 检查 format/lint/typecheck/test scripts
3. 从 Repo Hygiene 开始
4. 所有能修的错误修到绿
5. 完成所有代码质量/安全/隐私/依赖/架构/clean-room Gate
6. 尝试当前环境可用的平台编译
7. 输出 `MVP01_RC_AUDIT_REPORT.md`

不要再讨论设计，实际执行。
