# 个人数字依赖图 · 完整设计母版

> 版本：v0.4 · 完整整合母版（截至 2026-09-06；三端工程基线修订）  
> 状态：**Canonical Baseline / 设计冻结 / 可开工**  
> 前身：《个人账户关系图》v0.1–v0.3、《个人数字依赖图》v0.4、IMPLEMENTATION_NOTES v0.4.1 / R1 / R2  
> 本文件已把后续评审、实现红线、Schema 澄清和安全/互操作修正全部吸收进正文。**工程实现只以本文件为准；旧版全部视为 SUPERSEDED。**  
> 三端工程修订：为满足 Android / iOS / HarmonyOS 原生上架要求，统一应用层改为 **uni-app x Vapor + Vue 3 + TypeScript/UTS**；平台原生适配分别使用 Kotlin / Swift / ArkTS。该修订只替换端工程技术路线，不改变产品、Schema、Impact、安全与隐私语义。

---

## 0. 文档目的、修改门槛与工程第一原则

### 0.1 这份文档解决什么

本项目从 v0.1–v0.3 的“个人账户关系图”演进为“个人数字依赖图”。v0.4 完成了产品定位与模型重构；随后几轮评审进一步修正了 Observation / Proposal / Dependency 的语义边界、DependencyGroup 的确认机制、Impact Kernel 的 capability-aware 传播、账单去重与 Evidence 生命周期、SQLCipher 与 `.depmap` 加密协议、Web 无状态工作台、Node Resolver、Schema 唯一性和 UPSERT 规则。

本母版的目标不是继续追求理论完备，而是给出一份**足以直接写 Schema、migration、parser、tests 和 kernel 的统一工程设计**。

### 0.2 从本版起只接受两类修改

1. **Correctness 修复**：语义、状态机或算法会导致错误结论。
2. **Security / interoperability 修复**：存在安全漏洞、数据泄露风险或 Android / iOS / Web 跨端不兼容。

不再接受“为了完整而增加业务能力”的改动。新想法统一进入 v0.5 Backlog，不修改本母版。

### 0.3 工程第一原则

> **宁可漏报，不可把“不确定”伪装成“必须处理”。**

这一原则高于功能完整度和界面流畅度。对于依赖关系、备用路径、未来自动扣款等现实状态：机器可以提出，用户才能确认；缺数据不能被当成“现实中不存在”。

### 0.4 核心语义铁律

- **Observation ≠ Dependency**：账单只证明发生过什么，不证明未来持续依赖什么。
- **Proposal ≠ Reality**：机器推断再高分，也只是待确认建议。
- **Recorded absence ≠ real-world absence**：图里没记录第二条路径，不代表现实没有第二条路径。
- **Reachability ≠ failure propagation**：能连到不等于失效一定传播。
- **Node failure ≠ all capabilities failure**：第一版按 `(Node, Capability)` 传播，不把节点整体判死。
- **Graph 是模型，不是主界面**：用户要的是“我该怎么办”，不是一团节点连线。
- **Evidence ≠ Transaction History**：保留可解释性，但不偷偷做账本。

---

## 1. 产品定位

### 1.1 一句话

> **换卡、换号、换邮箱之前，先看清哪些账户、扣款和恢复路径会跟着受影响。**

这句话描述的是用户价值，而不是“记录卡和线”这种数据结构。

### 1.2 核心 Job-to-be-Done

| 既有产品回答 | 本产品回答 |
|---|---|
| 卡包：我有什么卡？ | |
| 密码管理器：我怎么登录？ | |
| 订阅工具：我在付什么钱？ | |
| 到期提醒：什么快过期？ | |
| | **如果我把这张卡、这个手机号、这个邮箱换掉，会发生什么？我该先处理哪几件事？** |

第一阶段只验证一个垂直 Job：

> **“我要换掉 / 注销这张银行卡。”**

### 1.3 三层分工：秘密 / 出示 / 档案依赖

| 层 | 归谁 | 本 App 的做法 |
|---|---|---|
| **秘密**：完整卡号、CVV、密码、PUK、私钥 | 密码管理器 | 仅存 `vaultRef` 指向条目 |
| **出示**：条码、NFC、支付凭证 | 系统钱包 | 仅存 `walletRef` 标注位置 |
| **档案、依赖、时钟、影响分析** | 本 App | 自己管理 |

核心原则是**指针而不是复制**。本产品不保存完整卡号、CVV、支付密码、助记词等真正秘密，也不重造 Apple Wallet / Google Wallet 的系统级出示能力。

### 1.4 为什么不做 NFC / 卡包主入口

Apple 已提供 NFC & SE Platform，但其使用需要 entitlement、商业协议、行业资质或与持牌机构合作，且当前适用地域不包括中国大陆；Android HCE 虽技术开放，但真实银行卡模拟需要 tokenization 和卡组织合作。因此“不做”是基于资质、地域和产品边界，而不是“技术接口不存在”。

条码展示可以做成详情页附属功能，但不做独立卡包主入口；系统钱包在锁屏调出、侧边键、位置感知等场景具有结构性优势。

### 1.5 竞品与相邻品类

#### 四个成熟品类

| 品类 | 代表 | 与本项目边界 |
|---|---|---|
| 卡包 / 钱包 | Apple Wallet、Google Wallet、微信卡包、Catima | 管“要出示的凭证”，不理解跨账户依赖 |
| 密码管理器 | Bitwarden、1Password、KeePassXC | 管秘密和登录，不管时间、支付路径和变更影响 |
| 到期提醒 | 有期、到期清单等 | 管日期，不管关系 |
| 订阅追踪 | Rocket Money、Bobby、SubTracker、Monarch 等 | 管订阅与支出，不理解“哪张卡—哪个支付账户—哪个服务”的依赖链 |

#### 直接相邻产品

- **My Account Map**：概念上高度相邻，强调账户连接、变更影响、继承/交接，但主要是手工维护。
- **Etoolio Digital Dependency Map**：覆盖 recovery / verifies 类依赖、blast radius、recovery loop，但更像一次性自测工具，不覆盖支付账单与自动建边。
- **AccountMap.org**：偏账户登录方式和连接可视化。
- **OpsLocker**：B 端形态参考；local-first、依赖、影响分析、成本与责任关系，证明“依赖图 + 变更影响”这一产品形态成立。

### 1.6 竞争判断的严谨表述

不写“没有人做”。采用可证伪表述：

> **在目前复核到的直接竞品和相邻产品中，尚未发现把“中国账单解析 → 支付路径抽取 → 用户确认 → 依赖图 → 变更影响分析”完整串起来的 C 端产品。**

恢复依赖并非空白，因此第一阶段**弱化恢复图，强化支付图**。

### 1.7 技术复利，而不是“壁垒”

真正会随时间累积的是整条 pipeline：

```text
多源解析
→ Observation
→ CanonicalEvent / 去重关联
→ Node Resolution / 商户归一
→ 支付路径识别
→ DependencyProposal
→ 用户确认
→ Dependency / DependencyGroup
→ Impact Analysis
→ Action Checklist
```

支持来源越多、alias 越全、误判案例越少、Schema 越稳定，系统越有价值。单独“能解析 CSV”不构成优势。

### 1.8 目标用户

第一用户是作者本人。MVP 不做增长设计、不做复杂用户分群，以真实自用数据验证价值和正确性。

---

## 2. 核心概念模型

### 2.1 总体架构

```text
                    个人数字依赖图
                           │
          ┌────────────────┼────────────────┐
          │                │                │
        Nodes        Dependencies        Clocks
          │                │                │
          └────────┬───────┴───────┬────────┘
                   │               │
               Evidence        Impact Engine
                   ▲               │
                   │               ▼
            DependencyProposal  Action Plan
                   ▲
          ┌────────┼────────┐
          │        │        │
        微信     支付宝     银行
       Parser    Parser    Parser
                   ▲
             Observation
```

R2 之后必须再加一层严格语义：

```text
Observation        = 事实：发生过什么
CanonicalEvent     = 导入会话内“这些观测可能是同一经济事件”的关联
DependencyProposal = 机器推断：这些事实可能意味着什么
Dependency         = 用户确认：现实中当前存在的依赖
DependencyGroup    = 用户确认：多条依赖如何共同满足某一 capability
Impact             = 反事实：如果某 capability 失效，会发生什么
```

### 2.2 Anchor Nodes

不再使用“根节点”。个人数字关系天然可成环，例如手机号、邮箱、Apple ID 之间互为验证或恢复方式。

Anchor Nodes 是“控制大量关系、失效代价高”的基础节点，如主手机号、主邮箱、主要支付账户、Apple ID、Google 账号、身份证等。长期可根据图结构识别；MVP 不需要做 Anchor 算法。

### 2.3 Capability 是传播单位

一个节点不是只有“活 / 死”两态。例如微信可能：

```text
WeChat
├── access     正常
├── recovery   正常
└── payment    失效
```

因此 Impact 状态键必须是：

```text
ImpactStateKey = (nodeId, capability)
```

MVP 只支持：

```text
MVP Impact Domain = payment
```

`access`、`recovery`、`identity` 等在支付切片通过后再扩展；第一版**不得把 node-level failure 当成通用 capability failure**。

---

## 3. 产品范围与明确不做

### 3.1 MVP 首发节点

首发只需要五类：

- 银行卡
- 手机号
- 邮箱
- 支付账户（微信 / 支付宝）
- 服务 / 订阅

真正参与第一条 payment-domain 切片的核心其实是：银行卡、支付账户、服务；手机号和邮箱保留是为了 Schema 不返工，但不要求第一版做跨 capability Impact。

### 3.2 稳定 `kind` + 可扩展 `templateId`

| `kind` | 示例 |
|---|---|
| `identity_anchor` | 手机号、邮箱、身份证 |
| `payment_instrument` | 银行卡 |
| `account` | 微信、支付宝、银行账户、Apple ID |
| `service` | 腾讯视频、Netflix、iCloud |
| `membership` | 酒店、航空、健身、储值卡 |
| `device` | 手机、U 盾 |
| `custom` | 其他 |

信用卡 / 储蓄卡等具体差异放在 `templateId`，例如 `builtin.bank_card.credit`，不继续膨胀 `kind`。

### 3.3 微信 / 支付宝是 Account，不是 Channel

“渠道”是边中的角色，不是实体类型：

```text
银行卡 ──funding_source/payment──→ 微信账户
微信账户 ──merchant_agreement/payment──→ 腾讯视频
手机号 ──verifies/access──→ 微信账户
```

同一 Node 可以在不同 Dependency 中扮演不同角色。

### 3.4 一个东西是否纳入的判断标准

1. 是否有身份或账户属性；
2. 是否存在依赖边，换掉会牵连别的东西；
3. 是否存在周期、到期、考核等时钟。

三条全中是核心节点，中两条可作为模板，只中一条原则上不做。

### 3.5 会员卡筛选闸门

| 类型 | 时钟 | 边 | 结论 |
|---|---|---|---|
| 健身年卡 | 到期 | 可能自动续费 | 可做 |
| 储值卡 | 余额 / 有效期 | 可能自动充值 | 可做 |
| 航空里程 | 过期 | 边弱 | 后续 |
| 酒店会员等级 | 保级周期 | 边弱 | 后续 |
| 超市积分卡 | 过期 | 无明显边 | 待定 |
| 优惠券 | 到期 | 无 | 不做 |
| 门票 / 登机牌 | 一次性 | 无 | 不做 |

### 3.6 明确不做

| 不做 | 原因 |
|---|---|
| 助记词、私钥、支付密码、U 盾 PIN | 属于密码管理器 / 密钥管理范围 |
| 完整卡号、CVV | 只存后四位 |
| NFC / 刷卡支付 / 主卡包入口 / `.pkpass` | 资质、地域和场景不适合 |
| 记账与长期流水 | 会把产品拉成第二个记账 App |
| 自动读短信 | 权限和商店风险高 |
| 银行卡 OCR | 只录后四位时价值低 |
| 账号体系 | 与 local-first / 无云账户定位冲突 |
| 支付、代扣、支付 SDK | 会进入金融资质和安全责任域 |
| 实时优惠活动数据库 | 维护成本和责任过高 |
| “该刷哪张卡”推荐 | 需要大规模实时活动库 |
| 没有边的纯到期物 | 到期提醒红海 |
| Neo4j / GraphRAG / LLM / Agent / GNN | MVP 属于过度工程 |
| N-of-M Group、跨 capability 推理、SPOF、dominator | v0.5 以后再讨论 |
| Event sourcing、CRDT | MVP 不需要 |

---

## 4. 技术架构与端策略

### 4.1 三端技术栈

本项目要求最终可在 **Android、iOS、HarmonyOS** 三个平台独立打包和上架。主业务代码保持一套，原生安全能力通过平台适配器隔离。

| 层 | 方案 |
|---|---|
| 主业务语言 | **TypeScript** |
| UI / 应用壳 | **uni-app x Vapor + Vue 3 + Composition API** |
| 跨端原生桥接 | **UTS 插件** |
| Android 原生实现 | Kotlin |
| iOS 原生实现 | Swift |
| HarmonyOS 原生实现 | ArkTS |
| Domain / Impact / Parser | 尽可能使用纯 TypeScript，保持 deterministic / testable / platform-independent |
| Android / iOS 本地数据库 | SQLite + SQLCipher，密钥进入系统安全区 |
| HarmonyOS 本地数据库 | ArkData relationalStore 加密能力 + HUKS；通过同一 Repository / SecureDatabaseAdapter 暴露逻辑 Schema |
| 原生身份验证 | Android 系统 biometric/device credential；iOS LocalAuthentication / Keychain；HarmonyOS 官方用户认证能力 |
| `.depmap` 文件加密 | Argon2id + AES-256-GCM + RFC 8785 JCS AAD |
| 解析器 | 本地纯解析，账单仅在导入会话内存处理 |
| Web 工作台 | 后续可做无状态工作台，不作为三端 App MVP 的阻塞项 |

**禁止使用 跨端应用层 作为三端统一底座。** 不写三套完全独立 App；业务代码不得散落大量 platform conditional，平台差异必须通过 Adapter / UTS 插件隔离。

### 4.2 端优先级

1. **Android**：优先完成 Core、SQLCipher 真机安全 Spike、安装包与 release 配置。
2. **HarmonyOS**：在同一业务层之上完成 ArkTS / ArkData / HUKS 适配与 DevEco 构建。
3. **iOS**：完成 Swift / Keychain / LocalAuthentication 适配；若当前开发机不是 macOS，则代码与工程配置完成后，把最终 Xcode build / signing 记录为外部环境 Blocker。
4. **Web 工作台**：不阻塞 MVP；三端核心跑通后再做。

### 4.3 Web 不是完整持久化客户端，而是无状态工作台

```text
拖入加密 .depmap
→ 输入口令
→ Argon2id WASM 派生 fileEncryptionKey
→ WebCrypto AES-GCM 解密到内存
→ 编辑 / 解析账单 / 补资料
→ 重新加密导出
→ 会话结束
```

Web 工作台：

- 不写 IndexedDB；
- 不写 localStorage；
- 不加载远程字体；
- 不使用 analytics；
- 依赖全部 bundle；
- 严格 CSP；
- 默认可离线。

安全承诺采用保守措辞：

> Web 工作台不主动持久化任何解密后的用户数据；关闭会话后不保留可由应用重新恢复的解密状态。

### 4.4 小程序暂缓

小程序无法提供可靠本地通知；订阅消息依赖服务端且长期订阅权限受限，同时用户清理微信存储可能直接清除本地数据。因此不作为 MVP 主端。未来若做，只作为查看/录入辅助端，不要求与 App 功能完全对等。

---

## 5. Schema v1：数据模型

### 5.1 通用约定

- 所有持久化实体带 `id`、`createdAt`、`updatedAt`（下方示例可省略展示）。
- 顶层 `schemaVersion = 1`。
- 业务对象尽量 retired / archived，不做无痕删除。
- 导出格式、同步格式、导入格式保持同一逻辑 Schema。
- 关系与状态必须支持 UPSERT，不把“同一现实关系重新激活”误建成第二条边。

### 5.2 Node

```json
{
  "id": "uuid",
  "kind": "payment_instrument",
  "templateId": "builtin.bank_card.credit",
  "name": "招行经典白",
  "issuer": "招商银行",
  "last4": "4417",
  "owner": "self",
  "archived": false,
  "fields": {},
  "vaultRef": "Bitwarden: 招行经典白",
  "walletRef": null
}
```

关键模板示例：

**bank_card**

```json
{
  "subtype": "credit | debit",
  "billDay": 5,
  "dueDay": 25,
  "annualFee": 580,
  "annualFeeDate": "2027-03-10",
  "annualFeeWaiver": "刷满6笔"
}
```

**phone**

```json
{
  "carrier": "中国移动",
  "plan": "花卡 39元 30G",
  "monthlyFee": 39,
  "promoEndDate": "2027-06-30",
  "postPromoFee": 59,
  "isPrimary": true
}
```

**email**

```json
{
  "address": "xxx@example.com",
  "provider": "Gmail",
  "purpose": "主力 | 工作 | 垃圾 | 历史",
  "status": "active | at_risk | dead"
}
```

**subscription / service**

```json
{
  "merchant": "腾讯视频",
  "amount": 25,
  "cycle": "monthly",
  "lastObservedAt": "2026-09-15",
  "expectedNextAt": "2026-10-15",
  "predictionConfidence": 0.72,
  "managePath": "微信 → 服务 → 钱包 → 支付设置"
}
```

### 5.3 Observation：只存在于导入会话

Observation 是 parser 产生的事实记录，**不持久化**。

典型字段：

```json
{
  "source": "wechat",
  "sourceTxnId": "...",
  "occurredAt": "2026-09-15T10:23:00+08:00",
  "merchantRaw": "腾讯视频",
  "description": "VIP会员连续包月",
  "amount": 25.00,
  "direction": "out",
  "paymentMethodRaw": "招商银行信用卡(4417)"
}
```

它回答的是：**“这次发生了什么？”**

### 5.4 CanonicalEvent：会话内关联，不物理去重

多来源中疑似同一笔经济事件：

```text
Observation A（微信） ┐
                      ├── CanonicalEvent X, sameEventConfidence=0.91
Observation B（银行） ┘
```

不删除任何 Observation，不进行不可逆物理合并。CanonicalEvent 同样只活在导入会话。

### 5.5 ObservationFingerprint：只解决“以前处理过没有”

持久化结构：

```json
{
  "fingerprint": "hmac-sha256-output",
  "source": "wechat",
  "fingerprintVersion": 1,
  "importSessionId": "uuid"
}
```

唯一约束：

```text
UNIQUE(source, fingerprint)
```

优先使用稳定交易号：

```text
HMAC-SHA256(fpSecret, sourceId + ":" + sourceTxnId)
```

没有稳定交易号时构造 canonical row：

```text
canonicalRow =
    dateTime
  + signedAmount
  + normalizedDescription
  + counterparty
  + balance(if available)
  + transactionType
```

若同一文件内出现完全相同 canonicalRow，只在这些完全重复行内部增加 occurrence ordinal：`#1`、`#2`。

去重策略偏 precision：允许 false negative，尽量避免 false positive。

`fpSecret` 存在加密数据库内并随 `.depmap` 迁移，不绑定单台设备。

### 5.6 Node Resolver：Proposal 之前必须先解决实体

Parser 输出原始商户名后：

```text
Parser merchantRaw
→ Node Resolver
   ├─ alias exact 唯一命中 → 使用已有 nodeId
   ├─ fuzzy 多候选        → 用户选择
   └─ 无候选              → 用户确认后创建 service node
→ resolution 完成
→ 才允许生成 Proposal
```

MVP 只使用 alias + exact/fuzzy + 人工确认；不使用 embedding、向量库或 LLM。

### 5.7 Evidence：聚合证据摘要，不是交易史

Evidence 不保存金额、商户明文、商品说明或单笔时间序列。

MVP 将 Evidence 定义为**某一 Proposal key 当前累计的证据摘要**：

```json
{
  "id": "uuid",
  "sourceType": "wechat_bill",
  "parserId": "wechat",
  "parserVersion": 1,
  "lastImportSessionId": "uuid",
  "firstObservedAt": "2026-04-15",
  "lastObservedAt": "2026-09-15",
  "observationCount": 6
}
```

跨多次导入时，仅把 fingerprint 尚未出现的新 Observation 累计进去：

```text
observationCount += newUniqueObservationCount
firstObservedAt = min(old, new)
lastObservedAt  = max(old, new)
lastImportSessionId = currentSession
```

这样 `rejectedAtObservationCount` 可以直接与当前 `observationCount` 比较。

### 5.8 DependencyProposal：机器推断，不进依赖图

Proposal 是待确认队列，不是现实边。

```json
{
  "id": "uuid",
  "path": ["card_4417", "wechat", "tencent_video"],
  "proposalType": "recurring_payment_route",
  "source": "statement",
  "parserId": "wechat",
  "parserVersion": 1,
  "confidenceScore": 0.96,
  "evidenceId": "evidence_xxx",
  "status": "pending | partial | accepted | rejected",
  "suggestedDependencies": [
    {
      "key": "card_4417|funding_source|wechat|payment",
      "from": "card_4417",
      "to": "wechat",
      "relation": "funding_source",
      "capability": "payment",
      "decision": "pending | accepted | rejected",
      "decidedAt": null
    },
    {
      "key": "wechat|merchant_agreement|tencent_video|payment",
      "from": "wechat",
      "to": "tencent_video",
      "relation": "merchant_agreement",
      "capability": "payment",
      "decision": "pending | accepted | rejected",
      "decidedAt": null
    }
  ]
}
```

`status` 只是派生字段，用于列表过滤；业务逻辑读每条 suggestion 的 `decision`。

Proposal Item 的 `key` 全局唯一：

```text
from|relation|to|capability
```

同 key 再次被观察到时必须 UPSERT：更新 Evidence / confidence，不创建第二个 suggestion。

#### rejected 的重提

`rejected` 表示“当时该推断被用户否认”，不是永久为假。保存：

- `key`
- `rejectedAt`
- `rejectedAtObservationCount`

自拒绝之后同一 key 的**新观测数 ≥ 3**，且覆盖至少一个完整识别周期，允许软性重提。

### 5.9 Dependency：只有用户确认后才存在

正式 Dependency 不再有 `proposed / rejected / confidenceScore`。

```json
{
  "id": "uuid",
  "from": "node_id",
  "to": "node_id",
  "relation": "funding_source | merchant_agreement | verifies | recovers | bound_to",
  "capability": "payment | access | recovery | identity",
  "criticality": "required | unknown",
  "groupId": "group_id | null",
  "state": "active | retired",
  "origin": "manual | proposal",
  "confirmedAt": "2026-09-06",
  "lastVerifiedAt": "2026-09-06",
  "retiredAt": null,
  "evidenceRefs": ["evidence_id"]
}
```

#### criticality 的最终规则

MVP 只使用：

```text
required | unknown
```

不在第一版使用 `preferred / backup` 作为影响推理语义；备用关系交给 DependencyGroup 表达。

确认“关系存在”不等于确认“这是唯一必需关系”。机器不得自动把 `unknown` 升成 `required`。

用户确认一条 Dependency 时，如果没有足够事实证明“失去它必然失去目标 capability”，默认：

```text
criticality = unknown
```

只有用户明确确认“没有这条关系，目标 payment 能力会失效”时才为 `required`。

#### Dependency 唯一性与 reactivation

逻辑唯一键：

```text
UNIQUE(from, relation, to, capability)
```

UPSERT 规则：

- 不存在 → INSERT；
- 已 active → 更新 `lastVerifiedAt`、Evidence；
- 已 retired → **复用同一 id re-activate**，清 `retiredAt`，更新 `confirmedAt / lastVerifiedAt`。

不为同一现实关系反复创建新 ID。

### 5.10 DependencyGroup：用户确认的依赖逻辑

Group 与 Dependency 同级，都是现实断言，**Parser 不得直接生成**。

```json
{
  "id": "uuid",
  "groupKey": "canonical-string",
  "targetNodeId": "wechat",
  "capability": "payment",
  "mode": "ANY | ALL",
  "memberEdgeIds": ["e1", "e2"],
  "state": "active | retired",
  "confirmedAt": "2026-09-06",
  "lastVerifiedAt": "2026-09-06"
}
```

Group 是 capability-scoped；`WeChat.payment` 的 ANY Group 不影响 `WeChat.access`。

#### groupKey

成员集合必须先 canonicalize；`[A,B]` 与 `[B,A]` 应视为同一组。

```text
groupKey =
  targetNodeId + "|" + capability + "|" + mode + "|" +
  sort(memberDependencyLogicalKeys).join("|")
```

数据库直接 `UNIQUE(groupKey)`。

### 5.11 DependencyGroupProposal：备用路径也必须有确认生命周期

系统看到同一目标存在多条 funding source，只能提出“可能有备用”，不能直接建立 ANY Group。

```json
{
  "id": "uuid",
  "key": "group|wechat|payment|ANY|<sorted-member-keys>",
  "targetNodeId": "wechat",
  "capability": "payment",
  "mode": "ANY",
  "memberDependencyKeys": ["...", "..."],
  "decision": "pending | accepted | rejected",
  "decidedAt": null,
  "rejectedAtObservationCount": null
}
```

用户确认后才生成 active `DependencyGroup`。拒绝记录同样不是永久封杀；有显著新 Evidence 后允许重提。

### 5.12 Clock

现阶段保留五类：

```text
task | assessment | benefit | decay | obligation
```

`obligation` 用于还款日、扣款日、月租等周期性义务。

```json
{
  "id": "uuid",
  "nodeId": "node_id",
  "clockType": "task | assessment | benefit | decay | obligation",
  "title": "刷满6笔免年费",
  "target": 6,
  "current": 4,
  "unit": "笔",
  "deadline": "2027-03-10",
  "resetCycle": "none | monthly | quarterly | yearly",
  "penalty": "扣年费 580",
  "autoSource": {"enabled": true, "rule": "count_transactions"},
  "remindBefore": [30, 7, 1]
}
```

mechanics / semantics 双层重构是合理方向，但推迟到真实案例足够后，不阻塞 MVP。

---

## 6. 账单解析、关系抽取与体检

### 6.1 目标：不是导入流水，而是提取依赖事实

账单解析的目标不是形成账本，而是输出：

- 周期性扣款候选；
- 支付路径观测；
- Service Node 候选；
- DependencyProposal；
- Evidence Summary；
- ObservationFingerprint。

### 6.2 数据获取路径

**支付宝**：通过账单 / 交易流水证明导出文件，用户自行下载并提供给本地解析器。

**微信**：通过“钱包 → 账单 → 下载账单”等官方路径导出，用户自行解压后交给本地解析。

国内存在开放银行/API 标准和银行开放平台，但并不存在类似 Plaid / PSD2 那种面向独立 C 端开发者、跨银行统一、消费者授权即可读取个人交易数据的通用聚合入口，因此 MVP 采用官方账单导出文件。

### 6.3 明确排除的自动化方案

- 模拟登录 / 爬虫；
- Android 无障碍自动操作支付 App；
- 自动读短信；
- 高权限通知监听作为主通道；
- IMAP 读取完整邮箱。

> **摩擦是隐私的价格。** MVP 追求“一年几次账单体检”，不是实时银行同步。

### 6.4 Parser 接口

```text
Parser {
  id
  version
  detect(raw) -> confidence
  parse(raw) -> Observation[]
}
```

微信 / 支付宝的字段特征用于自动识别来源。Parser 必须处理：编码、BOM、说明行、金额格式、退款撤销、分期、表头漂移等。

### 6.5 微信是 MVP 第一解析器

微信“支付方式”字段能直接提供某次交易所使用的银行卡，是第一条 payment-route vertical slice 最有价值的数据来源。

第一版只要求：

1. 正确解析微信账单；
2. 找到周期性服务候选；
3. 解析支付方式中的卡机构 / 尾号；
4. 经 Node Resolver 匹配银行卡、微信账户、Service；
5. 生成 `recurring_payment_route` Proposal；
6. 让用户分别确认 `funding_source` 和 `merchant_agreement`。

### 6.6 周期识别

MVP 规则：按解析后的服务候选分组，使用时间间隔和金额稳定度寻找：

- 28–31 天；
- 88–92 天；
- 360–370 天。

输出 `confidenceScore`，不直接建立现实依赖。

长期再升级 calendar periodicity、描述相似度、支付路径稳定性、金额变异系数等，不在 MVP 做复杂模型。

### 6.7 商户 / 服务归一

MVP 只做：

- 内置 alias；
- exact；
- 基础 fuzzy；
- 人工确认。

未完成 Node Resolution 的记录不进入 Proposal。

### 6.8 体检报告

账单导入结束后展示“事实 + 不确定性”，例如：

```text
本月固定支出：¥847
周期扣款：13 项
3 项持续扣费超过半年，建议复核是否仍需要
2 项识别为疑似遗忘
1 笔跨来源疑似同一经济事件
4 条新的支付关系建议待确认
```

绝不写“半年未使用”这类账单无法支持的结论。

### 6.9 预测的诚实表达

```text
腾讯视频 ¥25/月
最后观测：2026-03-15（连续 6 次）
当前：预测中
置信度：72%
已 5 个月未重新体检
```

预测不是现实确认；随着数据陈旧，界面要显式降低确定性。

---

## 7. Impact Engine：MVP Payment-Domain Kernel

### 7.1 目标

输入一个反事实场景，例如：

> “如果招行 4417 的 payment capability 不可用，会怎样？”

输出：

1. 必须处理；
2. 有确认备用路径；
3. 能力降级；
4. 需要核对 / 无法确认；
5. 最终 Action Checklist。

### 7.2 API：从一开始支持 Scenario

核心 API：

```text
simulateScenario(
  unavailable: Set<(nodeId, capability)>
)
```

便捷包装：

```text
simulateDisable(nodeId, capability=payment)
= simulateScenario({(nodeId, capability)})
```

这样单卡、双卡同时失效、未来更复杂的反事实都不会出现“第二次调用不知道第一次状态”的歧义。

### 7.3 传播状态

```text
ImpactStateKey = (nodeId, capability)
visited: Set<ImpactStateKey>
```

MVP 限定 `payment`，所有 group 满足性也只在同 capability 内判断。

### 7.4 Dependency / Group 规则优先级

影响判断顺序：

1. **受影响 Dependency 属于 active confirmed Group**：按 Group 的 ANY / ALL 判断。
2. 同目标存在其他 active Dependency，但没有 confirmed Group：**needs_review**，不得自动宣称备用路径。
3. 没有其他已记录候选，且该 Dependency `criticality=required`：目标 capability lost。
4. `criticality=unknown`：needs_review。
5. Proposal 无论置信度多高：最多 needs_review，不参与确定性失效传播。

### 7.5 核心铁律

```text
active Dependency + confirmed Group + group failed
→ must_change / capability lost

active Dependency + confirmed Group + group still satisfied
→ alternate_path / redundancy degraded

active Dependency + Group 未确认
→ needs_review

Dependency criticality = required，且不存在已知替代候选
→ capability lost

Dependency criticality = unknown
→ needs_review

Proposal（任何 confidenceScore）
→ 最多 needs_review
```

尤其：

> **有两条边 ≠ 有自动备用路径。**

### 7.6 图有环：BFS 必须防环

```text
simulateScenario(unavailable):
  1. 初始化 unavailable state keys
  2. visited = set(unavailable)
  3. BFS queue = unavailable
  4. 对每个 (N, payment)：读取 active outgoing dependencies
  5. 根据 Group / criticality 计算目标 payment 状态
  6. capability lost 的目标若未 visited，则入队
  7. needs_review 不进入确定性失效传播
  8. 生成分类结果与 depth
  9. 按 depth 组织处理动作；原始注销动作强制最后
```

不使用普通拓扑排序，因为图天然可能有环。

### 7.7 输出语言

| 状态 | 标签 | 示例 |
|---|---|---|
| 高确定性 | **必须处理** | 已确认该支付能力依赖此关系，且无确认备用路径 |
| 确认 Group 仍满足 | **有备用路径 / 能力降级** | 已确认另一资金来源可接管，但冗余度下降 |
| Group 未确认 / criticality unknown / Proposal | **建议检查** | 检测到可能替代路径，但未确认会自动切换 |
| 只有旧观测 | **历史观察** | 曾观察到该路径，目前可能变化 |

### 7.8 必跑测试

至少包含：

```text
T1 required 单边：A → B；disable A => B.payment lost
T2 confirmed ANY：A/B → C；disable A => C available + redundancy degraded
T3 confirmed ALL：A/B → C；disable A => C lost
T4 Proposal 0.999：disable A => B needs_review，NOT must_change
T5 链式：A → B → C；disable A => B lost => C 传播
T6 环：A → B → C → A；必须终止，每个 state key 只处理一次
T7 两条边但 Group 未确认：disable A => C needs_review，不能说有备用
T8 criticality unknown 且无 Group：disable A => B needs_review，不能说 lost
T9 multi-unavailable scenario：同时 disable CMB/CCB => WeChat.payment lost
```

### 7.9 固定夹具

```text
Nodes:
  CMB4417
  CCB8821
  WeChat
  TencentVideo

Dependencies:
  CMB4417 --funding_source/payment--> WeChat
  CCB8821 --funding_source/payment--> WeChat
  WeChat  --merchant_agreement/payment--> TencentVideo
```

场景 A：Group 未确认

```text
simulateDisable(CMB4417,payment)
→ WeChat needs_review
→ TencentVideo needs_review
```

场景 B：confirmed ANY Group

```text
{CMB4417, CCB8821} ANY → WeChat.payment

simulateDisable(CMB4417,payment)
→ WeChat available, redundancy degraded
→ TencentVideo available

simulateScenario({CMB4417.payment, CCB8821.payment})
→ WeChat.payment lost
→ TencentVideo must_change
```

这个 fixture 未通过前不写 UI。

---

## 8. 安全与隐私

### 8.1 威胁模型

整张依赖图属于敏感数据，因为它可能暴露：主邮箱、主手机号、银行关系、支付路径、订阅、恢复结构和潜在单点依赖。安全设计按泄露后果而不是“只有几十条数据”决定。

### 8.2 本地数据库：三端加密存储适配

业务层只依赖统一 `SecureDatabaseAdapter` / Repository，不依赖具体数据库引擎。

**Android / iOS**

```text
随机数据库密钥
→ SQLCipher
→ Android Keystore / iOS Keychain（或经验证的安全存储实现）
→ 系统生物识别 / 设备凭证 gating
```

**HarmonyOS**

```text
ArkData relationalStore 加密数据库
→ HUKS 保护密钥材料
→ HarmonyOS 官方用户认证能力
```

三端逻辑 Schema 与 Repository 行为必须一致，但底层数据库实现不要求强行相同。

不使用 App 自己的 6 位 PIN 作为数据库密钥根；不做字段级选择性加密；不自行发明密码学封装。需要具体平台 API 时以当前官方 SDK / 文档和真实编译结果为准。

### 8.3 真机安全 Spike

工程早期必须验证可获得的平台：

**Android**
- 数据库文件离线打开不可读；
- App 重启可正常解锁；
- 生物识别取消不能进入数据层；
- 设备凭证 fallback 正常；
- secret change / clear 行为符合预期；
- 升级、重启、进程杀死后不会丢 key。

**HarmonyOS**
- ArkData 加密库落盘不可直接读取；
- HUKS 密钥生命周期与失败路径正确；
- 用户认证取消后不得进入数据层；
- 重启、升级和进程杀死后数据可恢复且密钥不明文落盘。

**iOS**
- 在 macOS / Xcode 可用时验证 SQLCipher、Keychain、LocalAuthentication 与后台隐私遮罩；
- 若当前机器无法运行 Xcode，只能标记为 `IMPLEMENTED/CONFIGURED`，不得写成 `DEVICE_VERIFIED`。

### 8.4 账单数据红线

- 原始账单仅在内存处理；
- Observation / CanonicalEvent 会话结束销毁；
- 不保存完整流水；
- 仅保存 Fingerprint、Evidence Summary、用户确认后的图实体；
- UI 明确告知用户源文件不会被产品长期保存。

### 8.5 `.depmap`：口令派生文件加密，不叫 Envelope Encryption

本项目的文件加密：

```text
password
→ Argon2id
→ fileEncryptionKey
→ AES-256-GCM
→ ciphertext
```

没有随机 DEK / wrapped DEK，因此不使用 Envelope Encryption 术语。真正多设备 WebDAV 需要密钥轮换时再引入 envelope 模式。

### 8.6 DEPMAP_CONTAINER_V1

```json
{
  "format": "depmap",
  "formatVersion": 1,
  "kdf": {
    "algorithm": "argon2id",
    "version": 19,
    "salt": "base64",
    "memoryKiB": 65536,
    "iterations": 3,
    "parallelism": 1
  },
  "cipher": {
    "algorithm": "AES-256-GCM",
    "nonce": "base64"
  },
  "ciphertext": "base64",
  "tag": "base64"
}
```

固定协议参数：

```text
Argon2id version = 19 / 0x13
derived key      = 32 bytes
salt             = 16 bytes
AES-GCM nonce    = 12 bytes
tag              = 16 bytes
Base64           = RFC 4648 standard with padding
password bytes   = exact UTF-8, no Unicode normalization
```

### 8.7 AAD 与 JSON Canonicalization

AAD 固定为：

```text
UTF8(
  RFC8785-JCS({format, formatVersion, kdf, cipher})
)
```

`ciphertext` 和 `tag` 不进入 AAD。

### 8.8 解密前参数边界检查

Header 在认证通过前一律视为不可信输入。流程：

```text
parse
→ structural validation
→ bounds validation
→ Argon2id
→ AES-GCM authentication
→ 成功后才信任 payload
```

V1 边界：

```text
formatVersion == 1
kdf.algorithm == argon2id
kdf.version == 19
memoryKiB ∈ [16384,262144]
iterations ∈ [1,10]
parallelism ∈ [1,4]
salt == 16 bytes
nonce == 12 bytes
tag == 16 bytes
ciphertext <= 64 MiB（MVP 上限，可版本化）
```

生产导出固定 65536 / 3 / 1。

### 8.9 Golden Test Vector

容器冻结的前提是有确定性 test vector：

- 固定 password；
- 固定 16-byte salt；
- 固定 12-byte nonce；
- 固定 plaintext；
- 冻结 `expectedDerivedKey`；
- 冻结 `expectedCiphertext`；
- 冻结 `expectedTag`。

跨端矩阵：

```text
Android encrypt → Web decrypt PASS
Web encrypt → Android decrypt PASS
iOS 接入后双向 PASS
```

### 8.10 App 层防护

- Android `FLAG_SECURE`；
- iOS 切后台遮罩；
- Android 关闭系统自动备份；
- iOS 数据文件排除 iCloud 自动备份；
- 启动锁使用系统生物识别 / 设备凭证；
- 首页明确显示最近一次用户主动导出备份时间。

---

## 9. UI / UX 原则

### 9.1 Answer-oriented，不是 Graph-oriented

首页优先展示行动：

```text
变更
  换银行卡   换手机号   换邮箱   注销账户

风险 / 待核对
  3 条新支付关系待确认
  1 个备用路径尚未确认
  2 条关系长时间未复核

近期
  招行 6 天后还款
  移动优惠 28 天后到期
```

完整关系图只作为二级视图，用来解释结果，而不是首页主角。

### 9.2 变更模拟页

以“注销招行 4417”为例：

```text
必须处理
- ……

有备用路径
- ……

建议检查
- 微信检测到另一个资金来源，但未确认可自动切换
- 腾讯视频最近 6 次走微信，但当前签约方式尚未确认

最后一步
- 注销招行 4417
```

### 9.3 渐进式录入

第一级 10 秒完成：类型、名称、尾号；其他字段可空。

第二级“待补资料”逐步补账单日、还款日、年费规则、优惠结束日等，避免第一次就要求完整填写。

### 9.4 Proposal 确认界面

机器一次路径推断要拆成多个现实问题：

```text
最近 6 次腾讯视频扣款均出现：招行 4417 → 微信 → 腾讯视频

1. 招行 4417 是微信当前资金来源吗？
   [是] [不是] [不确定]

2. 腾讯视频当前通过微信自动续费吗？
   [是] [不是] [不确定]
```

只有“是”生成 Dependency；“不确定”保持 Proposal；“不是”进入 rejected 状态并等待未来新证据。

### 9.5 Group 确认界面

```text
检测到微信还有建行 8821 这一资金来源。
如果招行 4417 停用，微信的自动扣款会切到建行 8821 吗？

[确认会切换]
[不会]
[不确定 / 稍后检查]
```

不得仅因存在两张卡就显示“有备用路径”。

---

## 10. 提醒、活动与后续功能

### 10.1 提醒调度

iOS 待处理本地通知有数量上限，因此采用滚动调度：每次 App 启动重新计算未来 60 天，排序后只排安全数量（例如前 50 条）。通知权限在用户第一次开启提醒时再申请。

### 10.2 活动追踪

数据来源：

1. 手动 +1；
2. 账单自动计算；
3. 纯时间型。

持卡净值、年费免除进度、权益使用等属于后续期，不影响 MVP。

### 10.3 其他基础功能

后续可包括：剩余天数、搜索置顶、归档、一键复制后四位、应急挂失面板、时间轴、携号转网提示、membership 节点等。

---

## 11. MVP 与工程实施路线

### 11.1 为什么 MVP 必须是垂直切片

如果第一版只做银行卡档案、手机号档案、到期天数，无法验证项目最核心的假设：**“理解依赖并做变更影响分析”是否真的有价值。**

所以 MVP 只打通：

> **“我要换掉这张银行卡。”**

### 11.2 MVP 范围

必须有：

- Schema v1；
- SQLCipher local DB；
- 一份真实微信账单解析；
- Observation / Fingerprint / Evidence；
- 最小 Node Resolver；
- DependencyProposal；
- DependencyGroupProposal；
- 用户确认流；
- payment-domain Impact Kernel；
- `.depmap` 加密导入导出；
- 系统级启动锁；
- 真实数据双 Gate。

明确不做：提醒、活动、支付宝、多银行、图可视化、会员卡、LLM/Agent。

### 11.3 开工顺序

```text
1. Schema v1 + constraints
2. SQLite migration
3. Impact failing tests（T1–T9）
4. payment-domain Impact Kernel
5. .depmap golden vectors + Android/Web interoperability
6. Android 真机 SQLCipher security spike
7. 微信 Parser
8. ObservationFingerprint / Evidence aggregation
9. Node Resolver
10. DependencyProposal + Proposal UPSERT
11. Dependency / DependencyGroup confirmation + reactivation
12. DependencyGroupProposal
13. 第一份真实账单
14. Correctness Gate + Value Gate
15. UI
```

前十四步尽量 CLI / test-driven；核心正确性没跑通前，不投入漂亮 UI。

### 11.4 Schema v1 数据库不变量

| 对象 | 不变量 / 约束 |
|---|---|
| Dependency | `UNIQUE(from, relation, to, capability)` |
| Dependency | 只有用户确认后才能存在；`criticality` 默认 `unknown` |
| Dependency | retired 后重新确认必须 re-activate 同一行 |
| DependencyGroup | 只有用户确认后产生 |
| DependencyGroup | `groupKey` canonical + UNIQUE |
| Proposal item | `key=from|relation|to|capability` 唯一，重复观测 UPSERT |
| Group Proposal | canonical key 唯一，拒绝可被新 Evidence 重提 |
| ObservationFingerprint | `UNIQUE(source,fingerprint)` + `fingerprintVersion` |
| Evidence | `parserId/parserVersion` 必须存在；只聚合新 fingerprint |
| Node | `kind` 稳定、`templateId` 扩展 |

### 11.5 双 Gate

**Correctness Gate**

- 手工审计真实数据；
- 所有“必须处理”必须是现实中的真依赖；
- 不允许 false positive；
- 未确认备用关系不得被展示成“安全可切换”。

**Value Gate**

满足至少一条：

- 找到至少一件用户原本可能忘记处理的依赖；
- 明显缩短“销卡前我到底要检查什么”的完整排查时间。

如果双 Gate 不通过，停止继续做外围功能，优先判断产品 thesis 是否成立。

---

## 12. 后续路线

### 第二期

- 提醒调度；
- 支付宝 Parser；
- 跨来源 CanonicalEvent；
- 分享菜单接收；
- 体检报告 UI；
- 更完整的 stale / lastVerifiedAt 规则。

### 第三期

- 更多银行解析器；
- 通用 CSV 字段映射；
- 扩充 alias / Entity Resolution；
- Clock mechanics / semantics 重构；
- 活动进度自动计算。

### 第四期

- 持卡净值；
- 时间轴；
- membership；
- 条码附属显示；
- 更完整的 recovery / access capability。

### 更以后

- BYOC / WebDAV 加密同步；
- 多主体 / 家庭空间；
- 桌面小组件；
- N-of-M Group（只有真实案例需要才做）；
- 复杂图分析仅在明确产品价值出现后考虑。

---

## 13. 开源与分发

### 13.1 License

默认 GPL-3.0；代码可开源，产品名称 / 商标单独声明不随代码 License 授权。

### 13.2 渠道优先级

| 渠道 | 策略 |
|---|---|
| GitHub Releases | MVP 首发 APK |
| F-Droid | 后续，保持依赖可开源审计 |
| App Store | iOS 功能稳定后 |
| Google Play | 满足测试与账号要求后 |
| 国内安卓商店 | MVP 不优先 |

README 维护解析器覆盖矩阵与已知限制，既展示能力，也作为开源贡献入口。

---

## 14. 待定但不阻塞的问题

以下问题不再阻塞开工：

1. Clock 是否拆 mechanics / semantics；
2. ANY / ALL 是否最终需要 N-of-M；
3. 周期识别的长期模型；
4. 持卡净值的主观估值；
5. Entity Resolution 要做到多深；
6. 恢复 / access capability 的完整模型；
7. 多主体用 `owner` 还是独立空间；
8. 超市积分卡等边界对象是否纳入；
9. stale 的具体时间阈值；
10. WebDAV 多端冲突解决模型。

这些只在真实切片产生证据后再定。

---

## 15. 设计决策备忘：避免未来漂移

- 产品核心是**理解依赖并把 Graph 变成行动**，不是记录卡片。
- 支付图优先于恢复图。
- 订阅是依赖图上的一种 service，不把产品定位成订阅管理器。
- “机器提出、用户确认现实”是自动化边界。
- 账单只作为 Fuel；不保存完整消费史。
- **有两条边不等于有备用路径**。
- **图里没记录不等于现实没有**。
- Impact 第一版只跑 payment domain。
- Graph 可以有环，不假设 DAG。
- 默认导出必须加密。
- Web 是无状态工作台，不是持久化钱包。
- 不把安全承诺说得比实际能力更强。
- 不为了“高级感”引入 LLM、Agent、知识图谱数据库或复杂概率推理。
- 修改只允许 correctness / security / interoperability。

---

## 附录 A：最终数据流

```text
                 原始账单
                    │
                    ▼
              Observation（内存）
                    │
                    ▼
             CanonicalEvent（内存）
                    │
          ┌─────────┼─────────┐
          ▼         ▼         ▼
       周期识别   路径识别   Node Resolver
          └─────────┼─────────┘
                    ▼
            DependencyProposal
                    │
                 用户确认
          ┌─────────┴──────────┐
          ▼                    ▼
     Dependency        DependencyGroupProposal
          │                    │
          │                 用户确认
          │                    ▼
          └──────────── DependencyGroup
                    │
                    ▼
             Dependency Graph
                    │
             Evidence Summary
                    │
       simulateScenario(payment)
                    ▼
              Impact Engine
                    ▼
             Action Checklist
```

`ObservationFingerprint` 在旁路只回答：**“这条原始记录以前处理过没有？”**，不参与业务语义。

---

## 附录 B：v0.3 → 当前母版的关键修正

| 旧设计 | 当前最终设计 |
|---|---|
| C 端没有产品管“线” | 已有相邻产品；差异聚焦“账单 → 支付依赖 → 确认 → Impact”完整链路 |
| 壁垒 = 关系图 + 中国账单解析 | 技术复利在整个 pipeline，不把 CSV parser 当壁垒 |
| 国内没有 Open Banking | 改为“没有面向独立 C 端的统一跨银行消费者数据聚合能力” |
| iOS SE 只给 Apple Pay | API 已开放但大陆 / 资质不适用，结论仍是不做 |
| Edge 一条边就算影响 | Dependency + capability + confirmed Group |
| confidence=confirmed/inferred | Proposal confidence 与 Reality confirmation 完全分离 |
| Parser 直接建边 | Parser 只生成 Proposal |
| recurring route 可升级 merchant agreement | 路径与边分离，逐条 suggestion 确认 |
| 两张卡自动等于备用路径 | Group 必须用户确认 |
| Dependency 有 proposed/rejected | proposed/rejected 全归 Proposal；Dependency 存在即已确认 |
| preferred/backup 直接参与 MVP | MVP criticality 只用 required / unknown；备用由 Group 表达 |
| node-level Impact | `(node, capability)`，MVP 限 payment |
| 普通拓扑序 | BFS + visited 防环；Scenario API |
| 相似交易物理合并 | Observation / CanonicalEvent 会话内关联，不物理去重 |
| 原账单持久化 | 原账单 / 单笔 Observation 全不落盘 |
| merchantHash | 删除；低熵裸 hash 无隐私价值 |
| Evidence 每次导入一份明细 | Evidence 聚合新 Fingerprint 的摘要 |
| seqInDay fingerprint | stable txn id 优先；fallback canonical row + duplicate ordinal |
| 明文 JSON 导出 | `.depmap` Argon2id + AES-256-GCM |
| Envelope Encryption | 正名为 Password-Based File Encryption |
| “canonical header”模糊 | RFC 8785 JCS AAD + golden vectors |
| Web IndexedDB 持久化 | Web 无状态工作台 |
| SQLCipher 按数据量判断 | 整图按敏感数据威胁建模 |
| MVP 卡片档案 | MVP = “换掉一张真实银行卡”的完整 vertical slice |

---

## 附录 C：MVP 停止条件

满足以下任一情况，应停止增加外围功能，回到核心假设：

1. Impact Kernel 在手工真值集上频繁产生 false positive；
2. 用户无法可靠确认 Proposal / Group，导致维护成本高于价值；
3. 微信账单无法稳定抽取足够多真实支付关系；
4. 真实变更清单没有发现任何“原本容易漏掉”的事情，也没有明显节省排查时间；
5. 安全 / 加密 / 跨端导入无法达到可接受可靠性。

通过双 Gate 后，才进入提醒、更多 Parser、会员卡、时间轴等扩展。

---

**最终状态：本母版已足够开工。下一步是 Schema v1、migration 和 failing tests，不再进行非 Correctness / Security / Interoperability 类型的纸面扩展。**
