# GOAL_MVP01.md — DepMap 从空目录到三端 MVP 的连续执行目标

> 执行模型：ZCode + GLM-5.3-Flash  
> 当前 Workspace：从空目录开始  
> 目标：尽可能一次连续推进到 Core 完整、Android/HarmonyOS 可运行构建、iOS 工程可构建/可签名前状态，并完成三端上架基础资料。  
> 原则：不要停在方案；实际创建文件、写代码、运行测试、修复错误、构建并生成报告。

---

## 0. 启动时必须读取

按顺序完整读取：

1. `AGENTS.md`
2. `CANONICAL_DESIGN.md`
3. `PLATFORM_DECISION.md`
4. `GOAL_MVP01.md`
5. `WORK_STATUS.md`
6. `BLOCKERS.md`
7. `.gitignore`

不要只读摘要。

---

## 1. 总任务

从当前空目录建立 DepMap MVP。

你承担：

- 软件架构
- TypeScript/Vue 工程
- Android 工程
- HarmonyOS 工程
- iOS 工程
- 数据模型
- SQLite/ArkData 适配
- 密码学容器
- Parser
- Impact Engine
- 自动化测试
- 安全审计
- Release engineering
- 文档

不要只输出设计建议。

实际执行：

- mkdir / 初始化 repo
- 创建工程
- 写代码
- 写测试
- 跑测试
- 修错误
- build
- 生成 reports

只有真人账号、签名、macOS/Xcode、真实账单等不可由代码解决的事项才可成为外部 Blocker。

---

## 2. 空目录初始化

若 `.git` 不存在：

- `git init`
- 创建合理 `.gitignore`
- 创建首个 baseline commit（若工具允许）

首先生成：

- `PRE_IMPLEMENTATION_AUDIT.md`
- `WORK_STATUS.md` 更新
- `docs/ARCHITECTURE.md`
- `docs/SCHEMA_V1.md`
- `docs/TEST_MATRIX.md`

审计必须确认：

- Workspace 原本无业务代码
- Canonical Design 已读取
- 当前主栈为 uni-app x + Vue3 + TypeScript/UTS
- 旧 Capacitor 路线不执行

---

## 3. 工程组织

建立一套共享业务 Core + 三端 Adapter 的结构。

目标结构可按实际 uni-app x 项目规范调整，但职责必须类似：

```text
/
├─ AGENTS.md
├─ CANONICAL_DESIGN.md
├─ PLATFORM_DECISION.md
├─ GOAL_MVP01.md
├─ WORK_STATUS.md
├─ BLOCKERS.md
├─ FUTURE.md
├─ README.md
├─ docs/
│  ├─ ARCHITECTURE.md
│  ├─ SCHEMA_V1.md
│  ├─ IMPACT_ENGINE.md
│  ├─ PARSER_WECHAT.md
│  ├─ CRYPTO_PROTOCOL.md
│  ├─ SECURITY_MODEL.md
│  ├─ PLATFORM_ADAPTERS.md
│  ├─ TEST_MATRIX.md
│  ├─ REAL_DATA_VALIDATION.md
│  └─ STORE_RELEASE_CHECKLIST.md
├─ src/
│  ├─ core/
│  │  ├─ domain/
│  │  ├─ schema/
│  │  ├─ impact/
│  │  ├─ parser/
│  │  ├─ resolver/
│  │  ├─ proposal/
│  │  ├─ evidence/
│  │  ├─ fingerprint/
│  │  ├─ crypto/
│  │  └─ validation/
│  ├─ application/
│  ├─ repositories/
│  ├─ stores/
│  ├─ pages/
│  ├─ components/
│  └─ utils/
├─ uni_modules/
│  ├─ depmap-secure-database/
│  ├─ depmap-secure-key/
│  ├─ depmap-biometric/
│  ├─ depmap-file-crypto/
│  └─ depmap-privacy-screen/
├─ migrations/
├─ tests/
│  ├─ unit/
│  ├─ integration/
│  ├─ impact/
│  ├─ parser/
│  ├─ crypto/
│  ├─ repository/
│  ├─ fixtures/
│  └─ golden/
└─ local_private/
```

不要求机械照搬目录名；要求职责隔离。

---

## 4. 共享 Core 的独立测试能力

即使 HBuilderX / DevEco / Xcode 暂时不可用，也必须能先运行共享 Core 测试。

为纯 TypeScript Core 建立：

- package.json
- TypeScript
- test runner（优先 Vitest，若与当前环境冲突可选择等价方案）
- lint
- typecheck

核心：

- domain
- impact
- parser
- resolver
- proposal lifecycle
- crypto reference tests

必须能在普通 Node 环境独立测试。

UI / 平台 SDK 不得阻塞 Core。

---

## 5. Schema v1

严格按 Canonical Design 实现。

至少持久化：

- Node
- Dependency
- DependencyGroup
- DependencyProposal
- DependencyGroupProposal
- Evidence
- ObservationFingerprint
- ImportSession
- app metadata / schema version
- fpSecret（受加密数据库保护）

### 5.1 Dependency

Dependency 存在即用户确认。

MVP：

- capability = payment
- criticality = required | unknown
- state = active | retired
- origin = manual | proposal
- confirmedAt
- lastVerifiedAt
- retiredAt
- evidenceRefs

logical key：
`from|relation|to|capability`

数据库 UNIQUE。

### 5.2 Reactivation / UPSERT

同一 logical dependency：

- 不存在 → INSERT
- active → UPDATE lastVerifiedAt/evidence
- retired → re-activate 同一 id

禁止重复建边。

### 5.3 DependencyGroup

只允许用户确认产生。

字段至少：

- id
- targetNodeId
- capability
- mode ANY|ALL
- memberEdgeIds
- groupKey
- state
- confirmedAt
- lastVerifiedAt

`groupKey` 必须 deterministic：
`target|capability|mode|sorted(memberLogicalKeys)`

UNIQUE(groupKey)

### 5.4 DependencyProposal

Proposal 非图实体。

每条 suggestion 独立：

- key
- from
- to
- relation
- capability
- decision pending|accepted|rejected
- decidedAt
- criticalityDecision required|unknown|null

Proposal.status 仅 UI 过滤，不参与业务判断。

### 5.5 DependencyGroupProposal

必须有独立生命周期。

至少：

- id
- key
- targetNodeId
- capability
- mode
- memberDependencyKeys
- decision
- decidedAt
- rejectedAt
- rejectedAtObservationCount

---

## 6. Migration

实现 SchemaVersion = 1。

要求：

- transaction
- rollback
- idempotent
- 重启重复执行安全
- 失败不得留下半迁移 DB

为 migration 写测试。

---

## 7. Impact Kernel — 先写 failing tests

第一版只支持 `payment`。

```ts
type ImpactStateKey = {
  nodeId: string
  capability: 'payment'
}
```

底层 API：

```ts
simulateScenario(unavailable: Set<ImpactStateKey>)
```

wrapper：

```ts
simulateDisable(nodeId, capability='payment')
```

图允许有环。

使用 queue/BFS + visited ImpactStateKey。

禁止 DAG 假设。

### 7.1 判定原则

1. active confirmed group 存在 → 按 group 计算。
2. 有其他 active dependency，但 group 未确认 → needs_review。
3. 没有 confirmed alternative，criticality=required → capability_lost。
4. criticality=unknown → needs_review。
5. Proposal 不进 Graph，不传播确定失效。
6. absence of recorded dependency != absence of real-world dependency。

### 7.2 至少 12 个测试

T1 required A→B；disable A => B.payment lost

T2 A,B ∈ confirmed ANY→C；disable A => C available + redundancy degraded

T3 A,B ∈ confirmed ALL→C；disable A => C lost

T4 proposal confidence .999；不得 must_change

T5 A→B→C；失效传播

T6 A→B→C→A；必须终止，每个 ImpactStateKey 最多处理一次

T7 A,B→C 但 group 未确认；disable A => needs_review，NOT backup_path

T8 A→C criticality unknown；disable A => needs_review

T9 retired dependency 不参与传播

T10 retired dependency reactivated 后重新参与

T11 scenario 同时 disable A,B；confirmed ANY group => C lost

T12 payment lost 不得错误传播到 recovery/access

再补：

- deterministic output order
- target operation always last in Action Checklist

所有测试 PASS 才实现 UI。

---

## 8. `.depmap` V1

按 Canonical Design 完整实现。

容器：

- format depmap
- formatVersion 1
- Argon2id v19
- salt 16 bytes
- derived key 32 bytes
- memory 65536 KiB
- iterations 3
- parallelism 1
- AES-256-GCM
- nonce 12 bytes
- tag 16 bytes
- RFC4648 Base64 with padding
- password exact UTF-8 bytes, no Unicode normalization

AAD：
`UTF8(JCS({format, formatVersion, kdf, cipher}))`

### 8.1 不可信 header

Argon2 前校验：

- exact algorithms/version
- memoryKiB 16384..262144
- iterations 1..10
- parallelism 1..4
- salt 16
- nonce 12
- tag 16
- ciphertext <= 64MiB

恶意 container 不得触发超大内存 KDF。

### 8.2 Golden Vector

固定：

- password = depmap-test
- 固定 salt
- 固定 nonce
- 固定 plaintext

冻结：

- derived key hex
- ciphertext base64
- tag base64

建立 Node/reference implementation。

要求不同实现互操作，而不是只测自己解自己。

---

## 9. 安全数据库 Adapter

定义统一接口。

```ts
interface SecureDatabaseAdapter {
  open(): Promise<void>
  close(): Promise<void>
  migrate(): Promise<void>
  transaction<T>(fn: () => Promise<T>): Promise<T>
}
```

Repository 行为必须平台一致。

### Android

实现/验证：

- SQLCipher
- Keystore / 经验证安全密钥方案
- biometric / system credential
- wrong key fail
- DB file offline unreadable
- process kill/restart
- key lifecycle

### HarmonyOS

实现/验证：

- ArkData relationalStore encryption
- HUKS
- 官方用户认证
- encrypted at rest
- process kill/restart
- key lifecycle

不得为了“叫 SQLCipher”硬移植不必要方案。

### iOS

实现：

- SQLCipher
- Keychain
- LocalAuthentication
- privacy screen

如果当前开发机无 macOS/Xcode：

- 完成代码、接口、工程配置与测试可做部分
- `COMPILED/DEVICE_VERIFIED` 明确标 no
- 记录 BLOCKER
- 继续其他工作

---

## 10. 真机 Security Spike

尽早做，不等 UI。

每个平台状态必须写：

- IMPLEMENTED
- COMPILED
- TESTED
- DEVICE_VERIFIED

不得模糊。

---

## 11. 微信 Parser

MVP 只做微信。

Parser 必须 pure/deterministic。

处理：

- UTF-8
- UTF-8 BOM
- GBK/GB18030（按实际格式）
- 表头说明行
- 金额
- 收支
- 支付方式
- 退款/撤销
- 交易单号
- 商户单号
- 商品
- 备注
- 空值
- malformed row

禁止保存原始交易到 DB。

合成 fixtures：

- normal-wechat.csv
- utf8-bom.csv
- gbk.csv
- header-offset.csv
- refund.csv
- duplicate-import.csv
- same-amount-twice.csv
- malformed.csv
- recurring-monthly.csv
- non-recurring.csv

真实账单禁止提交 Git。

---

## 12. ObservationFingerprint

优先 sourceTxnId：

`HMAC-SHA256(fpSecret, sourceId + ":" + sourceTxnId)`

无稳定交易号：
canonical row。

字段：

- fingerprint
- source
- fingerprintVersion
- importSessionId

UNIQUE(source, fingerprint)

不保存 raw transaction id / merchant / amount / description。

测试重点：
第一次导 1–6 月；
第二次导 1–8 月；
只累计 7–8 月新记录。

---

## 13. Evidence / ImportSession

Evidence 是累计摘要，不是账本。

Evidence：

- sourceType
- parserId
- parserVersion
- lastImportSessionId
- firstObservedAt
- lastObservedAt
- observationCount

只累计新 unique observations。

ImportSession：

- sourceType
- parserId/version
- startedAt/completedAt
- rawCount
- newUniqueCount
- duplicateCount
- proposalCount
- errorCount

不得保存 raw rows。

---

## 14. Node Resolver

顺序：

1. builtin alias exact
2. normalized exact
3. conservative fuzzy
4. 用户确认

唯一匹配 → nodeId

多个 → 待用户选择

无 → 用户确认创建 service node 或忽略

未 resolution：
不得 Proposal。

禁止 LLM / embedding / vector DB。

---

## 15. Recurrence / Path Proposal

简单 MVP recurrence 即可：

- monthly
- quarterly
- yearly
- confidence

机器只产生 Proposal。

不得直接产生：

- Dependency
- DependencyGroup
- required criticality

---

## 16. Proposal UPSERT / Reproposal

same suggestion key：
UPSERT。

accepted：
不重复问。

rejected：
保存拒绝状态。

重新提议必须基于新 evidence：

- new observations >= 3
- 覆盖至少一个完整 recurrence cycle

不是“半年后自动再问”。

GroupProposal 同理。

---

## 17. 应用 UI — Core 全绿后再做

默认中文。

不要先做关系图大屏。

页面至少：

1. 启动/解锁
2. 首页
3. 节点列表
4. 节点详情
5. 导入微信账单
6. Node Resolution
7. Dependency Proposal Review
8. DependencyGroup Proposal Review
9. 模拟注销银行卡
10. Impact Result / Action Checklist
11. 设置 / 导入导出 / 隐私

首页 answer-oriented：

- 换银行卡
- 待确认
- 必须处理
- 建议检查
- 有备用路径
- 能力降级

Graph 只做二级视图，MVP 可不做。

---

## 18. Action Checklist

必须把原始注销动作放最后。

输出等级：

- must_change / 必须处理
- backup_path / 有备用路径
- degraded / 能力降级
- needs_review / 建议检查
- unaffected / 不受影响

任何 must_change 都必须可追溯到 confirmed reality。

---

## 19. 应用隐私

本 App 不保存：

- 密码
- CVV
- 完整银行卡号
- 支付密码
- 原始长期消费流水

本 App 保存：

- 节点档案
- 用户确认 Dependency/Group
- Evidence Summary
- Fingerprint
- Proposal 状态
- 加密备份
- 配置

账单只本地处理。

---

## 20. 日志与 Git 安全

禁止日志：

- raw CSV
- secrets
- crypto keys
- transaction id
- decrypted depmap

`.gitignore` 必须覆盖：

- local_private
- real bills
- signing files
- keystore
- provisioning
- env secrets
- local DB
- build outputs

提交前扫描一次。

---

## 21. 三端工程与上架准备

### Android

完成：

- applicationId 可配置
- versionCode/name
- minimal permissions
- debug build
- release build config
- signing 不进 git
- privacy/data safety checklist
- icon/splash placeholders
- store description draft

### HarmonyOS

完成：

- bundleName 可配置
- module 配置
- minimal permissions
- ArkData/HUKS
- debug/release build 尽可能
- signing 不进 git
- AppGallery checklist
- privacy draft

### iOS

完成：

- bundle identifier 可配置
- version/build
- minimal entitlements
- Keychain
- biometric usage strings
- privacy manifest
- App Privacy draft
- release scheme/config
- signing 不进 git

无 macOS 时不得虚报 build。

---

## 22. 文档输出

最终至少生成：

- README.md
- PRE_IMPLEMENTATION_AUDIT.md
- docs/ARCHITECTURE.md
- docs/SCHEMA_V1.md
- docs/IMPACT_ENGINE.md
- docs/PARSER_WECHAT.md
- docs/CRYPTO_PROTOCOL.md
- docs/SECURITY_MODEL.md
- docs/PLATFORM_ADAPTERS.md
- docs/TEST_MATRIX.md
- docs/REAL_DATA_VALIDATION.md
- docs/STORE_RELEASE_CHECKLIST.md
- FINAL_REPORT.md
- BLOCKERS.md
- WORK_STATUS.md

---

## 23. REAL DATA Gate

没有真实账单时：

- synthetic tests 全部完成
- 生成本地验证命令/流程
- 不得写 REAL_DATA PASS

真实账单只放 `local_private/`。

跑：
账单 → parse → fingerprint → resolver → proposal → confirmation → graph → simulate

### Correctness Gate

所有“必须处理”人工审计必须为真。
false positive = 0

### Value Gate

至少发现一个原本易漏依赖，或明显减少排查时间。

失败就如实 FAIL。

---

## 24. 工作阶段

### PHASE 0 — Bootstrap

读取全部控制文件
审计空目录
初始化 git / Node core tests / uni-app x 工程
更新 WORK_STATUS

### PHASE 1 — Schema

Domain types
Schema v1
repositories
migration
tests

### PHASE 2 — Impact

先写 failing tests
实现 payment-domain kernel
全部 PASS

### PHASE 3 — Crypto

depmap V1
JCS
Argon2id
AES-GCM
bounds
golden vectors
reference tests

### PHASE 4 — Platform Security

Android secure DB
Harmony secure DB
iOS adapter
早期 security spikes

### PHASE 5 — Parser

Wechat parser
fixtures
fingerprint
evidence/import sessions

### PHASE 6 — Resolver

Node Resolver
alias/fuzzy/manual resolution

### PHASE 7 — Proposal

DependencyProposal
UPSERT
rejected/reproposal
confirmation

### PHASE 8 — Group Proposal

DependencyGroupProposal
confirmation
groupKey
lifecycle

### PHASE 9 — Integration

完整 synthetic import pipeline
duplicate import
reactivation
scenario tests

### PHASE 10 — UI

中文 MVP UI
全流程

### PHASE 11 — Android

build/run/security evidence

### PHASE 12 — HarmonyOS

build/run/security evidence

### PHASE 13 — iOS

工程/代码/build（视 macOS 环境）

### PHASE 14 — Store readiness

三端 checklist
privacy text
release config

### PHASE 15 — Final audit

所有测试
secret scan
git diff
FINAL_REPORT

---

## 25. 执行节奏

每个 Phase：

1. 写/更新测试
2. 实现
3. 运行测试
4. 修复
5. 更新 WORK_STATUS
6. 小步 commit
7. 继续下一 Phase

不要每步问用户是否继续。

---

## 26. 不允许伪完成

FINAL_REPORT 对每个平台写：

```text
IMPLEMENTED =
COMPILED =
TESTED =
DEVICE_VERIFIED =
STORE_READY =
```

并附证据。

“理论上应该可以”不算 PASS。

---

## 27. BLOCKER 处理

外部 blocker 可以记录，但必须继续其他工作。

可接受：

- 缺 Xcode/macOS
- 缺开发者账号
- 缺签名
- 缺真实账单
- 缺最终 bundle id / privacy URL

不可接受：

- TypeScript error
- test failure
- dependency conflict
- ordinary compile issue

这些自己解决。

---

## 28. 禁止扩展

MVP 不增加：
Neo4j、GraphRAG、LLM、Agent、embedding、vector DB、GNN、概率图、Bayes、SAT、rule DSL、dominator、SPOF 高级分析、N-of-M、跨 capability、event sourcing、CRDT、WebDAV、cloud sync、小程序、NFC、payment SDK、bank crawler、自动短信读取、Accessibility 自动点击。

写进 FUTURE.md，不实现。

---

## 29. 完成标准

### Core

- schema tests PASS
- migration PASS
- impact PASS
- parser PASS
- fingerprint PASS
- resolver PASS
- proposal lifecycle PASS
- group lifecycle PASS
- crypto golden vector PASS

### Android

尽可能：

- build PASS
- encrypted DB verified
- lock verified

### HarmonyOS

尽可能：

- build PASS
- ArkData encryption verified
- HUKS/auth verified

### iOS

- code/config complete
- 若 macOS 可用则 build/test
- 否则明确 blocker

### Product

- synthetic end-to-end PASS
- real-data pipeline ready
- UI complete
- simulate card removal → Action Checklist

### Docs

完整且真实。

---

## 30. 现在开始

不要回复“方案如下”。

实际开始执行：

1. 完整读取控制文件。
2. 写 `PRE_IMPLEMENTATION_AUDIT.md`。
3. 初始化 git 和共享 TypeScript test harness。
4. 初始化 uni-app x / 三端应用工程。
5. 更新 `WORK_STATUS.md`。
6. 写 Schema v1 failing tests。
7. 持续执行，直到达到成功条件或遇到真正外部 blocker。
