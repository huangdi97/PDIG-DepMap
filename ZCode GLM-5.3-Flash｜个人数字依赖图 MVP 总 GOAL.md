你现在是本项目的首席软件架构师、移动端工程师、数据工程师、安全工程师、测试工程师和 Release Engineer。

你的任务不是继续讨论设计，而是直接把项目从当前设计母版推进到一个真实可运行、可测试、可安装，并具备 Android / iOS / HarmonyOS 三端发布基础的 MVP。

# 0. 最高优先级指令

持续执行，不要只给建议、方案或示例。

你必须：

1. 读取项目目录中的完整设计母版；
2. 理解并冻结设计；
3. 创建工程；
4. 编写代码；
5. 编写 migration；
6. 编写自动化测试；
7. 实现 Impact Kernel；
8. 实现微信账单 Parser；
9. 实现 Node Resolver；
10. 实现 DependencyProposal / DependencyGroupProposal；
11. 实现用户确认及 Dependency / DependencyGroup 状态机；
12. 实现加密数据库；
13. 实现 `.depmap` 跨端加密导入导出；
14. 实现 Android / iOS / HarmonyOS 平台适配层；
15. 完成 MVP UI；
16. 完成构建；
17. 尽可能完成真机/模拟器验证；
18. 输出完整测试与验收报告；
19. 为三端上架准备工程配置和 checklist；
20. 除账号、证书、开发者实名认证、真实设备等必须由真人完成的事情外，其余全部完成。

不要每完成一步就停下来问我是否继续。

只有真正需要我的开发者账号、证书、签名、真实账单或其他无法由代码自动产生的秘密/外部凭据时，才允许记录为 BLOCKER。

即使存在 BLOCKER，也不能因此停止其他可执行工作。

目标是尽可能一次连续完成。

---

# 1. 唯一设计母版

项目内应存在：

`个人数字依赖图_完整设计母版_v0.4_截至2026-09-06.md`

首先完整读取它。

它是产品、数据模型、安全语义、Impact Engine 和 MVP 范围的 Canonical Baseline。

如果项目中同时存在：

- v0.4
- IMPLEMENTATION_NOTES_v0.4.1
- R1
- R2
- 其他旧文件

全部视为历史资料。

以“完整设计母版”为最高设计依据。

禁止自行恢复已经被淘汰的旧语义。

---

# 2. 项目第一原则

永久遵守：

> 宁可漏报，不可把“不确定”伪装成“必须处理”。

Precision > Recall。

任何机器推断：

- parser inference
- recurrence inference
- merchant matching
- payment route inference
- fallback inference

都不能直接产生确定性的现实依赖。

必须遵循：

Observation = 事实观测

Proposal = 机器推断

Dependency = 用户确认后的现实状态

DependencyGroup = 用户确认后的现实备用/组合关系

绝不能越级。

---

# 3. 产品定位不得漂移

本项目不是：

- 密码管理器
- Apple Wallet
- 卡包
- NFC 钱包
- 记账软件
- 流水分析软件
- 订阅管理器
- 到期提醒器
- 金融支付 App
- AI Agent
- GraphRAG
- 知识图谱 Demo

核心问题只有一个：

> 换卡、换号、换邮箱或注销账户之前，先看清哪些账户、扣款和依赖会受到影响，以及应该先做什么。

MVP 只验证：

> “我要换掉 / 注销一张银行卡。”

---

# 4. 三端要求

最终必须建立可支持：

- Android
- iOS
- HarmonyOS NEXT / HarmonyOS
- 后续 Web 工作台

的工程基础。

当前 MVP 开发优先级：

1. Android
2. HarmonyOS
3. iOS
4. Web 工作台

注意：

iOS 最终签名/上架需要 Apple Developer Account 和 macOS/Xcode 环境。

HarmonyOS 最终签名/发布需要华为开发者身份和 AppGallery Connect。

Android Play / 国内 Android 商店也需要相应账号和签名。

这些外部账号不能成为代码开发停止的理由。

---

# 5. 技术栈冻结

不要使用 Capacitor 作为三端统一底座。

不要切换 Flutter。

不要切换 React Native。

不要写三套完全独立客户端。

使用：

## UI / App Shell

- uni-app x
- Vapor 模式
- Vue 3
- Composition API
- TypeScript
- CSS 使用 uni-app x App 支持的 CSS 子集

使用当前稳定 HBuilderX。

不要为了使用新特性主动依赖 alpha / beta，除非稳定版确实无法完成必须功能。

## 主业务语言

TypeScript。

核心 domain 层必须保持纯 TypeScript，禁止直接依赖平台 API。

## Native Platform Adapter

使用 UTS plugin 统一声明接口。

需要原生能力时：

Android：
- UTS
- Kotlin

iOS：
- UTS
- Swift

HarmonyOS：
- UTS
- ArkTS

UTS 插件必须统一 interface，不允许业务层到处出现：

`if android`
`if ios`
`if harmony`

平台差异应被 adapter 隔离。

---

# 6. 建议工程结构

构建类似：

```text
/
├─ docs/
│  ├─ CANONICAL_DESIGN.md
│  ├─ ARCHITECTURE.md
│  ├─ SCHEMA_V1.md
│  ├─ SECURITY_MODEL.md
│  ├─ CRYPTO_PROTOCOL.md
│  ├─ TEST_MATRIX.md
│  ├─ RELEASE_RUNBOOK.md
│  └─ STORE_RELEASE_CHECKLIST.md
│
├─ src/
│  ├─ core/
│  │  ├─ domain/
│  │  ├─ schema/
│  │  ├─ impact/
│  │  ├─ proposal/
│  │  ├─ resolver/
│  │  ├─ parser/
│  │  ├─ fingerprint/
│  │  ├─ evidence/
│  │  ├─ crypto/
│  │  └─ validation/
│  │
│  ├─ application/
│  ├─ repositories/
│  ├─ stores/
│  ├─ pages/
│  ├─ components/
│  ├─ composables/
│  ├─ utils/
│  └─ types/
│
├─ uni_modules/
│  ├─ depmap-secure-store/
│  ├─ depmap-database/
│  ├─ depmap-biometric/
│  ├─ depmap-file-crypto/
│  └─ depmap-privacy-screen/
│
├─ tests/
│  ├─ unit/
│  ├─ integration/
│  ├─ parser/
│  ├─ impact/
│  ├─ crypto/
│  ├─ fixtures/
│  └─ golden/
│
├─ scripts/
│
├─ migrations/
│
├─ FINAL_REPORT.md
├─ BLOCKERS.md
└─ README.md
```

可以依据 uni-app x 实际项目规范调整目录，但必须保持：

Domain 与 Platform 分离。

---

# 7. StorageAdapter

定义统一存储接口。

业务层不得知道底层数据库类型。

例如：

```ts
interface SecureDatabaseAdapter {
  open(): Promise<void>
  close(): Promise<void>
  migrate(): Promise<void>
  transaction<T>(fn: () => Promise<T>): Promise<T>

  nodes: NodeRepository
  dependencies: DependencyRepository
  dependencyGroups: DependencyGroupRepository
  proposals: ProposalRepository
  groupProposals: GroupProposalRepository
  evidences: EvidenceRepository
  fingerprints: FingerprintRepository
  importSessions: ImportSessionRepository
}
```

## Android

优先：

SQLCipher + Android Keystore。

不得自己发明数据库密码系统。

验证：

- 文件离线不可读
- 正确密钥可以打开
- 错误密钥失败
- App 重启正常
- biometric / device credential 行为正确

## iOS

优先：

SQLCipher + Keychain。

不得把数据库 key 放：

- UserDefaults
- 普通文件
- 日志
- source code

## HarmonyOS

不要为了保持“SQLCipher”名称而硬移植。

优先使用：

ArkData relationalStore 加密能力

+

HUKS 密钥管理。

实现逻辑上等价的：

HarmonySecureDatabaseAdapter。

必须满足：

- database at-rest encryption
- key 不明文落普通文件
- app-level lock
- schema 与 Android/iOS 逻辑一致
- repositories 行为一致

三端数据库引擎可以不同。

逻辑 Schema 不得不同。

---

# 8. Schema v1

完整落实母版。

至少包含：

## Node

核心字段：

- id
- kind
- templateId
- name
- issuer
- last4
- owner
- archived
- fields
- vaultRef
- walletRef
- createdAt
- updatedAt

首发：

- payment_instrument
- account
- service
- identity_anchor

不要增加无关 node type。

---

## Dependency

Dependency 存在即代表用户确认。

不得出现：

`proposed`
`rejected`

这些属于 Proposal。

至少：

```text
id
from
to
relation
capability
criticality
groupId
state
origin
confirmedAt
lastVerifiedAt
retiredAt
evidenceRefs
```

MVP capability：

`payment`

虽然 schema 可保留：

- access
- recovery
- identity

但 Impact Kernel 第一版不得执行跨 capability 推理。

### criticality

MVP 只允许：

- required
- unknown

禁止 parser 自动生成 required。

用户确认“关系存在”不等于确认“这是唯一/必需依赖”。

只有用户明确确认：

> 如果该关系失效，目标 payment capability 会失效。

才能：

`required`

否则：

`unknown`

### Dependency uniqueness

logical identity：

`from + relation + to + capability`

必须唯一。

重复接受 Proposal：

UPSERT。

禁止生成重复边。

### Reactivation

Dependency retired 后未来重新出现：

禁止新建另一条 logical dependency。

必须 re-activate 原记录：

```text
state = active
retiredAt = null
confirmedAt = now
lastVerifiedAt = now
```

保持 id 稳定。

---

# 9. DependencyGroup

Group 代表真实组合依赖。

不是机器猜测。

例如：

```text
{ CMB4417, CCB8821 } ANY -> WeChat.payment
```

只有用户明确确认：

> 招行停用之后微信可以自动切换到建行继续完成这项 payment capability

才允许建立 active ANY group。

Parser 永远不得自动创建 Group。

Group：

```text
id
targetNodeId
capability
mode
memberEdgeIds
groupKey
state
confirmedAt
lastVerifiedAt
```

MVP：

`ANY | ALL`

禁止 N-of-M。

### groupKey

成员集合排序 canonicalize。

生成 deterministic groupKey。

例如：

```text
target|capability|mode|sorted(memberLogicalKeys)
```

数据库：

UNIQUE(groupKey)

禁止：

[A,B]
[B,A]

变成两个不同 group。

---

# 10. DependencyProposal

Proposal 不进入 Dependency Graph。

Proposal 是待确认队列。

至少：

```text
id
path
proposalType
source
parserId
parserVersion
confidenceScore
evidenceId
status
suggestedDependencies[]
```

每条 suggestedDependency：

```text
key
from
to
relation
capability
decision
decidedAt
criticalityDecision
```

decision：

- pending
- accepted
- rejected

Proposal.status 只允许用于 UI/list filtering。

任何业务逻辑禁止依赖 Proposal.status。

必须依赖 suggestion.decision。

---

# 11. Proposal UPSERT

suggestion logical key：

```text
from|relation|to|capability
```

必须稳定。

同一个路径下一次又被 Parser 发现：

禁止 INSERT duplicate suggestion。

必须：

UPSERT。

逻辑：

```text
same suggestion key
    ↓
找到旧 suggestion
    ↓
增加 Evidence
    ↓
更新 confidence
    ↓
decision pending:
    保持 pending

decision accepted:
    不重复提问

decision rejected:
    检查是否出现足够新 Evidence
```

---

# 12. rejected 不是永恒错误

现实会变化。

rejected 表示：

> 当时用户认为这条推断不成立。

保留：

- key
- rejectedAt
- rejectedAtObservationCount

不能保存交易明细。

重新提议条件：

自 rejected 后：

- 新 observation ≥ 3
- 且跨度至少覆盖一个完整 recurrence cycle

满足才允许重新提示。

提示：

> 之前你否认过这条关系，但最近又连续观察到该路径。是否发生了变化？

禁止固定“半年后重新问”。

依据必须是新 evidence。

---

# 13. DependencyGroupProposal

Group 也必须走 Proposal 生命周期。

实现：

```text
DependencyGroupProposal
```

至少：

```text
id
key
targetNodeId
capability
mode
memberDependencyKeys
decision
decidedAt
rejectedAt
rejectedAtObservationCount
```

机器检测到：

> 同一目标存在多个资金来源

只能提出：

> 可能存在备用路径。

不能输出：

> 一定有备用。

用户确认后才创建 DependencyGroup。

用户拒绝后要 suppress。

未来有足够新 Evidence 后才允许重提。

---

# 14. Observation

Observation 是 import session 内临时结构。

绝不持久化。

微信 Parser 输出：

```text
occurredAt
merchantRaw
merchantNormalizedCandidate
description
amount
direction
channel
payMethodRaw
sourceTxnId
sourceParser
```

解析过程结束后销毁。

数据库中不得存在：

- 单笔交易流水
- 完整消费记录
- 商品消费历史

---

# 15. CanonicalEvent

仅 import session 内存在。

处理跨来源重复事件。

禁止“物理删除 Observation”。

只建立：

```text
sameEventConfidence
```

本轮 MVP 只有微信单源时可以保持基础实现，但接口必须存在。

---

# 16. ObservationFingerprint

持久化的唯一用途：

> 判断这条原始记录以前是否处理过。

结构：

```text
fingerprint
source
fingerprintVersion
importSessionId
```

数据库：

UNIQUE(source, fingerprint)

禁止存：

- amount
- merchant
- description
- product
- raw transaction id

### 微信

优先：

```text
HMAC-SHA256(
 fpSecret,
 sourceId + ":" + sourceTxnId
)
```

### 没有稳定 txn id 的银行

构造 canonical row：

```text
dateTime
+ signedAmount
+ normalizedDescription
+ counterparty
+ balance if exists
+ transactionType
```

相同 canonical row 多次：

只在完全相同行内部增加 occurrence ordinal。

重复识别策略：

Precision > Recall。

宁可 false negative。

尽量避免 false positive。

---

# 17. fpSecret

必须随用户数据迁移。

不能只放设备 Keychain。

存储于加密数据库中。

导出 `.depmap` 时包含 fpSecret。

这样换机后 fingerprint 仍然可比较。

---

# 18. Evidence

Evidence 是累计摘要。

不是 transaction history。

至少：

```text
id
sourceType
parserId
parserVersion
lastImportSessionId
firstObservedAt
lastObservedAt
observationCount
```

不保存：

- merchant 明文
- amount
- 商品
- 每笔时间
- 完整交易轨迹

同一 Proposal 新导入产生新 unique observation：

```text
observationCount += newUniqueCount
firstObservedAt = min()
lastObservedAt = max()
lastImportSessionId = current
```

---

# 19. ImportSession

增加轻量 ImportSession：

```text
id
sourceType
parserId
parserVersion
startedAt
completedAt
rawCount
newUniqueCount
duplicateCount
proposalCount
errorCount
```

不保存原始交易。

用于诊断和审计 Parser。

---

# 20. Node Resolver

禁止 Parser 直接假设：

`腾讯科技(深圳)有限公司 = tencent_video`

流程：

```text
merchant raw
 ↓
Node Resolver
```

### 优先级

1. builtin alias exact match
2. normalized exact match
3. conservative fuzzy match
4. 用户确认

若唯一命中：

使用 nodeId。

多个候选：

用户选择。

没有候选：

询问用户：

- 创建 service node
- 忽略

未完成 Node Resolution：

不得产生 DependencyProposal。

禁止：

- embedding
- vector DB
- LLM
- external API

---

# 21. 微信 Parser

第一版只做微信。

必须支持：

微信导出的 CSV / 文本账单格式。

至少处理：

- UTF-8
- UTF-8 BOM
- GBK / GB18030（根据实际文件）
- 表头前说明行
- 金额符号
- 收/支
- 退款
- 撤销
- 支付方式
- 交易单号
- 商户单号
- 商品
- 备注
- 空字段
- malformed rows

微信常见字段包括：

- 交易时间
- 交易类型
- 交易对方
- 商品
- 收/支
- 金额
- 支付方式
- 当前状态
- 交易单号
- 商户单号
- 备注

不得把分期误判为订阅。

Parser 必须 pure / deterministic。

同一输入重复 parse：

结果必须一致。

---

# 22. Recurrence Detection

MVP 简单实现即可。

按 resolved merchant 分组。

检测：

- 月度
- 季度
- 年度

基础规则可以参考：

28–31
88–92
360–370

金额稳定性作为辅助。

输出：

recurrenceConfidence。

禁止直接产生 Dependency。

长期 calendar periodicity 不在 MVP。

---

# 23. Impact Kernel

这是项目核心。

第一版：

## payment domain only

明确写在代码：

```text
MVP_IMPACT_DOMAIN = payment
```

状态单位：

```text
ImpactStateKey = (nodeId, capability)
```

不能使用：

visited: Set<nodeId>

必须：

```text
visited: Set<ImpactStateKey>
```

---

# 24. Impact API

不要只设计单节点 disable。

底层接口：

```text
simulateScenario(
  unavailable: Set<ImpactStateKey>
)
```

convenience：

```text
simulateDisable(nodeId, capability = payment)
```

等价：

```text
simulateScenario({
  (nodeId, payment)
})
```

这样可以正确测试：

同时停两张卡。

---

# 25. Impact 传播原则

图可能有环。

禁止假设 DAG。

BFS / queue。

必须有 visited。

只在相同 capability 内传播。

MVP 不执行跨 capability。

### 判定顺序必须避免歧义

对于被影响的 Dependency：

1. 如果属于 active confirmed DependencyGroup：
   按 group 判断。

2. 如果目标存在其他 active dependency，
   但这些 dependency 没有 confirmed group：
   `needs_review`

3. 如果没有 confirmed alternative，
   且 dependency criticality = required：
   `capability_lost`

4. 如果 criticality = unknown：
   `needs_review`

5. Group 未确认：
   `needs_review`

6. Proposal：
   不进入 Graph，
   最多作为 UI 的建议检查信息。

### 缺数据原则

永久冻结：

> Absence of recorded dependency != absence of real-world dependency.

“图里只记录了一张卡”

不能推出：

“现实里一定没有第二张卡。”

因此不得自动将单条边视为 sole source。

---

# 26. Impact 输出

至少：

- must_change
- backup_path
- degraded
- needs_review
- unaffected

中文 UI：

必须处理

有备用路径

能力降级

建议检查

不受影响

---

# 27. Impact tests

先写 failing tests。

至少：

T1:
A required → B
disable A
B.payment lost

T2:
A,B ∈ confirmed ANY → C
disable A
C available
redundancy degraded

T3:
A,B ∈ confirmed ALL → C
disable A
C lost

T4:
Proposal confidence=0.999
disable A
B needs_review
NOT must_change

T5:
A → B → C
disable A
loss propagates

T6:
A → B → C → A
算法必须终止
每个 ImpactStateKey 最多处理一次

T7:
A,B → C
group 未确认
disable A
C needs_review
NOT backup_path
NOT must_change

T8:
A → C criticality unknown
disable A
C needs_review

T9:
Dependency retired
不得参与传播

T10:
Dependency reactivated
必须重新参与传播

T11:
同时 disable A 和 B
confirmed ANY group
C lost

T12:
payment lost
不得错误传播到 recovery capability

这些全部 PASS 后，Impact Kernel 才算完成。

---

# 28. `.depmap` V1

必须完整实现。

容器：

```json
{
  "format": "depmap",
  "formatVersion": 1,
  "kdf": {
    "algorithm": "argon2id",
    "version": 19,
    "salt": "...",
    "memoryKiB": 65536,
    "iterations": 3,
    "parallelism": 1
  },
  "cipher": {
    "algorithm": "AES-256-GCM",
    "nonce": "..."
  },
  "ciphertext": "...",
  "tag": "..."
}
```

固定：

Argon2id v19

derived key:
32 bytes

salt:
16 bytes

nonce:
12 bytes

tag:
16 bytes

Base64:
RFC4648 standard with padding

password:
exact UTF-8 bytes
不做 Unicode normalization

---

# 29. `.depmap` AAD

使用 RFC 8785 JCS。

```text
AAD =
UTF8(
 JCS({
   format,
   formatVersion,
   kdf,
   cipher
 })
)
```

ciphertext/tag 不放 AAD。

---

# 30. `.depmap` 不可信 header 防护

KDF 前必须：

1. parse
2. validate structure
3. validate bounds
4. Argon2id
5. AES-GCM auth
6. 成功后信任 payload

边界至少：

```text
formatVersion == 1
algorithm == argon2id
version == 19

memoryKiB:
16384..262144

iterations:
1..10

parallelism:
1..4

salt:
16 bytes

nonce:
12 bytes

tag:
16 bytes

ciphertext:
<= 64 MiB
```

恶意文件不得造成无限内存申请。

---

# 31. Golden Test Vector

必须生成固定测试向量。

固定：

```text
password = depmap-test
固定 salt
固定 nonce
固定 plaintext
```

冻结：

- derivedKey hex
- ciphertext base64
- tag base64

必须验证不同实现产生相同结果。

至少：

Android implementation

Harmony implementation

Node/reference implementation

若有可用 macOS/iOS：

iOS implementation

跨端：

Android encrypt → Harmony decrypt

Harmony encrypt → Android decrypt

Android encrypt → reference decrypt

reference encrypt → Android decrypt

iOS 可用后同样加入。

不能只测“自己加密自己解密”。

---

# 32. File Crypto Adapter

定义统一接口：

```text
deriveKey
encryptDepmap
decryptDepmap
validateContainer
```

Android：
UTS/Kotlin。

iOS：
UTS/Swift。

Harmony：
UTS/ArkTS。

业务层不得知道具体密码库。

禁止把 password、derived key 输出到日志。

---

# 33. App Lock

实现：

Android：
BiometricPrompt / system credential。

iOS：
LocalAuthentication / Keychain gating。

Harmony：
使用官方用户认证 / HUKS 相关安全能力。

用户取消认证：

不得进入数据层。

不得展示节点数据。

不得先加载后盖 UI。

---

# 34. Privacy Screen

Android：

FLAG_SECURE。

iOS：

进入后台时遮罩敏感页面。

Harmony：

查阅并使用当前 HarmonyOS 官方隐私/窗口安全能力。

禁止猜不存在的 API。

若当前 SDK 不支持某行为：

记录平台差异。

不要伪造实现。

---

# 35. Backup policy

因为 local-first：

禁止系统无感把完整数据库自动同步到普通云备份。

Android：
关闭默认备份或明确排除数据库。

iOS：
将敏感数据库排除系统备份。

Harmony：
按最新官方文档配置敏感数据备份排除策略。

用户迁移数据统一通过：

`.depmap`

---

# 36. 日志安全

任何日志不得输出：

- account
- full node object
- raw CSV line
- merchant history
- transaction id
- fpSecret
- SQLCipher key
- password
- derived key
- decrypted `.depmap`
- vaultRef 内容

开发日志也遵守。

---

# 37. MVP UI

Core 全绿之后再做 UI。

默认中文。

不要先做炫酷关系图。

首页 answer-oriented。

至少页面：

## 1. 启动 / 解锁

简洁。

## 2. 首页

分：

变更

- 换银行卡
- 换手机号
- 换邮箱
- 注销账户

MVP 只有换银行卡 fully enabled。

风险

- 待确认关系
- 可能备用路径
- stale dependency

近期

可先保留基础结构。

## 3. 节点列表

卡
支付账户
服务

## 4. 节点详情

基本资料
依赖
证据摘要
最后确认时间

## 5. 导入账单

选择文件

解析

显示：

raw rows
new unique
duplicates
resolved
unresolved
proposals

绝不展示成记账流水 UI。

## 6. Node Resolution

商户候选确认。

## 7. Proposal Review

例如：

最近 6 次腾讯视频扣款都观察到：

招行 4417
→ 微信
→ 腾讯视频

分别问：

招行4417是微信当前资金来源吗？

腾讯视频当前通过微信自动续费吗？

不要一个“全部确认”按钮替代两个现实问题。

## 8. Group Review

检测到：

招行4417
建行8821

都与微信存在资金来源关系。

问：

如果招行停止使用，微信这项支付能力会自动切换到建行吗？

不允许根据两条边自动回答。

## 9. 模拟注销

选择银行卡。

执行：

simulateScenario。

## 10. 结果页

必须处理

建议检查

有备用路径

能力降级

最后：

“确认全部处理完成后，再注销该卡。”

目标节点注销动作永远最后。

## 11. 设置

导出备份
导入备份
安全
关于
隐私说明
数据说明

---

# 38. UI 风格

不要：

- AI 风
- 科幻大屏
- 发光 Graph
- 复杂仪表盘
- B 端后台风
- 花哨动画

做：

- 简洁
- 高信息密度
- 克制
- 本地工具感
- iOS / Android / Harmony 都自然
- 中文排版优先
- 暗色/亮色可后补

关系图仅作为二级可视化。

不是主界面。

---

# 39. 安全与隐私页面

明确告诉用户：

本 App 不保存：

- 密码
- CVV
- 完整银行卡号
- 支付密码
- 原始消费流水

本 App 保存：

- 节点档案
- 用户确认后的依赖
- Evidence summary
- fingerprint
- 设置
- encrypted backup

账单解析：

本地完成。

原始交易：

import session 后销毁。

---

# 40. 网络

MVP 默认：

NO BACKEND。

NO ACCOUNT。

NO CLOUD。

NO ANALYTICS。

NO TRACKER。

NO ADS。

NO telemetry。

除非 uni-app x runtime 本身不可避免的底层行为，否则业务代码不得主动发网络请求。

如果发现框架存在网络行为：

记录并评估。

---

# 41. 自动化测试

必须有：

- typecheck
- lint
- unit
- integration
- parser fixtures
- migration tests
- Impact tests
- crypto test vectors
- repository tests
- proposal lifecycle tests
- group lifecycle tests
- duplicate import tests
- reactivation tests

重点测试：

第一次导 1–6 月。

第二次导 1–8 月。

1–6 月不得再次累计。

只累计：

7–8 月。

---

# 42. Parser Fixture

绝不能把真实用户账单提交 Git。

构造 synthetic fixtures。

至少：

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

真实账单只用于用户本地 Gate。

.gitignore 必须覆盖真实账单路径。

---

# 43. Security Tests

至少验证：

- plaintext DB 不存在
- wrong key fail
- `.depmap` wrong password fail
- tag tamper fail
- header tamper fail
- malicious KDF params rejected before Argon2
- corrupted ciphertext fail
- duplicate nonce generation forbidden
- secret not logged
- app locked state cannot query repository

---

# 44. Migration

Schema 必须带版本。

实现：

SchemaVersion 1。

migration 要：

idempotent。

重复启动不会重复迁移。

失败：

transaction rollback。

数据库禁止半迁移状态。

---

# 45. Store-ready 基础

为三个平台准备：

## Android

- applicationId placeholder/final configurable
- versionCode
- versionName
- signing config 不提交 secret
- release build
- permissions最小化
- Data Safety checklist
- privacy policy template
- app icon
- splash
- screenshots placeholder requirements

## iOS

- bundle identifier configurable
- version/build number
- entitlements 最小化
- privacy manifest
- App Privacy answers
- keychain
- biometric usage description
- file access description（如果需要）
- release scheme
- signing 变量化

不得把 developer certificate 放 git。

## HarmonyOS

- bundleName configurable
- versionCode/versionName
- module.json5 正确
- permission 最小化
- ArkData
- HUKS
- privacy compliance checklist
- AppGallery Connect release checklist
- APP build/run configuration

不得伪造签名证书。

---

# 46. 上架文件

生成：

`docs/STORE_RELEASE_CHECKLIST.md`

分别写：

Android / Google Play

iOS / App Store

HarmonyOS / Huawei AppGallery

包括：

- developer account
- signing
- privacy
- screenshots
- icon
- app description
- support URL
- privacy policy
- age rating
- test account（本 App 无账号，应注明）
- data collection answers
- release build
- store review notes

---

# 47. 应用名称

不要擅自冻结商业名称。

代码内部暂用：

`DepMap`

中文：

`个人数字依赖图`

最终商店名称以后可改。

Bundle/application id 放配置。

禁止硬编码用户个人身份信息。

---

# 48. README

写清：

这个项目是什么。

不是什么。

核心数据模型。

安全模型。

运行方法。

Android build。

Harmony build。

iOS build。

测试命令。

`.depmap` 格式。

隐私原则。

---

# 49. 工程文档

最终至少生成：

`README.md`

`docs/ARCHITECTURE.md`

`docs/SCHEMA_V1.md`

`docs/IMPACT_ENGINE.md`

`docs/PARSER_WECHAT.md`

`docs/CRYPTO_PROTOCOL.md`

`docs/SECURITY_MODEL.md`

`docs/PLATFORM_ADAPTERS.md`

`docs/TEST_MATRIX.md`

`docs/STORE_RELEASE_CHECKLIST.md`

`docs/REAL_DATA_VALIDATION.md`

`FINAL_REPORT.md`

`BLOCKERS.md`

---

# 50. FINAL_REPORT

必须真实。

写：

- 完成了什么
- 没完成什么
- 哪些测试 PASS
- 哪些测试 FAIL
- Android 状态
- Harmony 状态
- iOS 状态
- crypto interop 状态
- security spike 状态
- parser 状态
- Impact Gate 状态
- 当前 blocker
- 下一步

禁止把“代码写了”写成“已验证”。

---

# 51. BLOCKERS

只有下列事情才应该成为外部 blocker：

- Apple Developer account
- Apple signing certificate
- iOS 真机 / Mac 环境
- Google Play account
- Android release keystore（若用户尚未提供）
- Huawei Developer / AppGallery Connect
- Harmony signing identity
- 用户真实微信账单
- 用户最终 bundle identifier / application id
- 隐私政策最终域名

任何纯代码问题：

不属于 blocker。

自己解决。

---

# 52. 真实数据 Gate

代码完成后，如果当前 repo 没有真实微信账单：

不要制造“真实测试通过”。

准备命令：

```text
import real bill
→ parse
→ fingerprint
→ node resolve
→ proposal
→ confirmation
→ graph
→ simulate
```

生成：

`docs/REAL_DATA_VALIDATION.md`

告诉用户只需要把自己的微信账单放到指定本地私有目录。

目录必须 gitignored。

一旦存在真实账单：

运行。

---

# 53. Correctness Gate

必须满足：

所有输出：

`必须处理`

都来自：

- 用户确认 active Dependency
- 必要 criticality
- 或 confirmed Group 经真实失效推导

不得由：

- Proposal
- confidence
- fuzzy merchant match
- 未确认 Group
- 单次历史 observation

产生。

目标：

confirmed false positive = 0

---

# 54. Value Gate

使用真实数据后验证：

至少：

发现一件用户原本容易漏掉的依赖

或

明显减少一次“销卡前排查”的人工时间。

如果没有：

如实写：

VALUE_GATE = FAIL

不要为了项目好看改结论。

---

# 55. 明确禁止

MVP 禁止：

- Neo4j
- GraphRAG
- LLM
- Agent
- GLM API
- OpenAI API
- embedding
- vector DB
- GNN
- Bayesian network
- probability graph
- SAT solver
- rule DSL
- dominator
- SPOF 高级分析
- N-of-M Group
- CRDT
- event sourcing
- WebDAV
- 云同步
- 后端
- 登录账号
- 小程序
- 广告
- 推荐“该刷哪张卡”
- 自动读取短信
- Android Accessibility 模拟操作
- 银行爬虫
- 支付 SDK
- NFC
- Apple Wallet 仿制
- 保存完整流水

一个都不要加。

---

# 56. ZCode 执行方式

不要一次改一堆后不测试。

按以下顺序连续执行：

PHASE 0
读取母版
建立工程
检查工具链

PHASE 1
Schema v1
Repositories
Migration
Tests

PHASE 2
Impact failing tests
实现 payment-domain Kernel
全部 PASS

PHASE 3
`.depmap`
JCS
Argon2id
AES-GCM
bounds
golden vectors
interop tests

PHASE 4
Native secure storage
Android SQLCipher
Harmony ArkData/HUKS
iOS adapter
security spike

PHASE 5
WeChat Parser
fingerprints
Evidence
ImportSession
synthetic fixtures

PHASE 6
Node Resolver

PHASE 7
DependencyProposal
Proposal UPSERT
rejected/reproposal

PHASE 8
DependencyGroupProposal
DependencyGroup confirmation

PHASE 9
full import pipeline integration tests

PHASE 10
MVP UI

PHASE 11
Android build/run

PHASE 12
Harmony build/run

PHASE 13
iOS build configuration
若当前机器支持则 build
否则明确 blocker

PHASE 14
Store readiness

PHASE 15
final test suite
security review
FINAL_REPORT

每个 phase：

先 test。

再 code。

再 test。

失败必须修。

不得为了继续流程而删测试。

---

# 57. 每阶段状态文件

维护：

`WORK_STATUS.md`

格式：

```text
Current phase:
Completed:
Tests:
Failures:
Blockers:
Next:
```

这样即使 ZCode 会话中断，下次也能从状态继续。

同时定期 git commit。

推荐：

```text
feat(schema):
test(impact):
feat(impact):
feat(crypto):
feat(parser):
feat(resolver):
feat(proposal):
feat(ui):
fix(security):
docs(release):
```

不要一个巨大 commit。

---

# 58. 如果当前仓库为空

直接初始化。

如果已经有代码：

先审计。

不要直接覆盖用户有效工作。

创建：

`PRE_IMPLEMENTATION_AUDIT.md`

说明：

- 当前代码结构
- 可复用内容
- 与 Canonical Design 冲突内容
- migration strategy

然后继续。

---

# 59. 不确定 API 时

不要猜。

尤其：

- Harmony API
- biometric API
- HUKS
- ArkData encryption
- iOS Keychain
- SQLCipher
- UTS plugin API
- uni-app x platform API

若能访问网络：

查当前官方文档。

优先：

- DCloud 官方
- Apple Developer
- Android Developers
- Huawei Developers
- SQLCipher 官方

不要照抄过时博客。

若无法联网：

检查本地 SDK typings / package docs / compiler errors。

通过真实编译验证。

---

# 60. 不允许“伪完成”

下面不算完成：

“理论上支持 Android”

“理论上可以 iOS”

“鸿蒙应该可以”

“SQLCipher 应该已加密”

“crypto 看起来一致”

必须分开记录：

IMPLEMENTED

COMPILED

TESTED

DEVICE_VERIFIED

STORE_READY

例如：

```text
Android:
IMPLEMENTED = yes
COMPILED = yes
DEVICE_VERIFIED = yes/no
STORE_READY = yes/no

Harmony:
...

iOS:
...
```

---

# 61. 最终成功条件

本次执行最终尽可能达到：

## Core

Schema PASS

Migration PASS

Impact tests PASS

Parser tests PASS

Resolver tests PASS

Proposal lifecycle PASS

Group lifecycle PASS

Fingerprint duplicate import PASS

`.depmap` crypto PASS

## Android

debug build PASS

release build config ready

encrypted DB verified

## HarmonyOS

build PASS 或真实记录 SDK blocker

encrypted ArkData adapter ready

HUKS adapter ready

## iOS

code/config complete

若有 macOS：
build PASS

否则：
明确外部环境 blocker

## Product

真实账单 pipeline ready

MVP UI complete

simulate card removal works

Action Checklist works

## Docs

complete

---

# 62. 开始执行

现在不要再输出一篇架构建议。

第一步：

1. 找到并完整读取 Canonical Design；
2. 检查当前 repo；
3. 生成 `PRE_IMPLEMENTATION_AUDIT.md`；
4. 建立 `WORK_STATUS.md`；
5. 创建 uni-app x Vapor 工程；
6. 建立纯 TypeScript domain core；
7. 开始 Schema v1；
8. 先写 failing tests；
9. 持续执行直到达到上述成功条件或遇到真正不可由代码解决的外部 blocker。

不要停在“下一步建议”。

实际去修改文件、执行命令、运行测试、修复错误、构建工程和生成报告。

开始。