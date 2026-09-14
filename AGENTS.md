# AGENTS.md — DepMap 项目长期工程指令

## 0. 文件优先级

每次任务开始先读取：

1. `AGENTS.md`
2. `CANONICAL_DESIGN.md`
3. `GOAL_MVP01.md`
4. `WORK_STATUS.md`
5. `BLOCKERS.md`

冲突处理：

- Correctness / Security / Privacy：以 `AGENTS.md` 与 `CANONICAL_DESIGN.md` 的更严格约束为准。
- 产品、Schema、Impact 语义：以 `CANONICAL_DESIGN.md` 为准。
- 当前交付范围与执行顺序：以 `GOAL_MVP01.md` 为准。
- 不得自行恢复旧版 IMPLEMENTATION_NOTES 或 Capacitor 方案。

## 1. 项目身份

内部代号：`DepMap`
中文工作名：`个人数字依赖图`

核心问题：

> 在换卡、换号、换邮箱、注销账户之前，告诉用户哪些账户、支付路径和依赖会受到影响，以及应该先处理什么。

当前 MVP 唯一核心 Job：

> 模拟更换 / 注销一张银行卡。

## 2. 第一原则

> **宁可漏报，不可把“不确定”伪装成“必须处理”。**

Precision > Recall。

机器推断不能直接变成现实事实：

- Observation = 发生过什么
- Proposal = 机器认为可能意味着什么
- Dependency = 用户确认的当前现实依赖
- DependencyGroup = 用户确认的组合/备用关系

## 3. 永久语义铁律

- Observation ≠ Dependency
- Proposal ≠ Reality
- Recorded absence ≠ real-world absence
- Reachability ≠ failure propagation
- Node failure ≠ all capabilities failure
- Evidence ≠ Transaction History
- Graph 是模型，不是主界面

## 4. 产品边界

本项目不是：

- 密码管理器
- NFC / 支付钱包
- 记账软件
- 长期流水数据库
- 单纯订阅管理器
- AI Agent
- GraphRAG
- 知识图谱 Demo
- 金融支付 App

MVP 未通过双 Gate 前，不增加新业务能力。

## 5. 技术栈冻结

主业务语言：**TypeScript**

应用层：

- uni-app x
- Vapor
- Vue 3
- Composition API
- TypeScript

原生桥接：

- UTS

平台：

- Android → Kotlin
- iOS → Swift
- HarmonyOS → ArkTS

Domain / Impact / Parser / Proposal 尽可能保持纯 TypeScript、deterministic、testable、platform-independent。

禁止未经明确批准切换到：

- Flutter
- React Native
- Capacitor 作为三端统一底座
- 三套完全独立客户端

## 6. 平台适配规则

业务核心不得直接依赖平台 API。

必须通过统一 Adapter 隔离：

- SecureDatabaseAdapter
- SecureKeyAdapter
- BiometricAdapter
- FileCryptoAdapter
- PrivacyScreenAdapter
- FilePicker/ShareAdapter

业务代码不得到处散落 Android/iOS/Harmony 条件分支。

## 7. 存储与密钥

Android / iOS：

- SQLite + SQLCipher
- Android Keystore / iOS Keychain
- 系统 biometric / device credential

HarmonyOS：

- ArkData relationalStore 加密能力
- HUKS
- 官方用户认证能力

三端逻辑 Schema 必须一致，底层数据库引擎可以不同。

不得：

- 明文数据库
- 明文备份
- 自制 6 位 PIN 作为数据库密钥根
- 密钥写源码 / 日志 / 普通配置文件

## 8. `.depmap`

必须兼容 `DEPMAP_CONTAINER_V1`：

- Argon2id v19
- 32-byte derived key
- AES-256-GCM
- RFC 8785 JCS AAD
- 参数边界预校验
- Golden Test Vector
- 跨实现互操作测试

V1 不得静默改变。协议变化必须升级 formatVersion。

## 9. Dependency

Dependency 存在即代表用户确认。

MVP criticality：

- required
- unknown

禁止机器自动产生 required。

logical key：
`from|relation|to|capability`

同一 logical dependency：

- 首次 → INSERT
- 已 active → UPSERT / verify
- retired 后重新成立 → re-activate 原记录
- 不生成第二条逻辑边

## 10. DependencyGroup

Group 也是现实断言，只能由用户确认。

机器只能生成 `DependencyGroupProposal`。

MVP mode：

- ANY
- ALL

禁止 N-of-M。

Group 必须 capability-scoped。

## 11. Proposal 生命周期

Parser 不创建 Dependency / DependencyGroup。

Parser 只能产生 Proposal。

同 key Proposal 必须 UPSERT：

- pending → 继续累计 evidence
- accepted → 不重复问
- rejected → 有足够新 evidence 才允许重提

rejected 不是永久为假。

## 12. Observation / Evidence

Observation / CanonicalEvent：

- 仅导入会话内存
- 会话结束销毁
- 不持久化单笔交易

持久化只允许：

- Fingerprint
- Evidence Summary
- Proposal 状态
- 用户确认后的图实体
- ImportSession Summary

不得把系统偷偷演化成记账数据库。

## 13. Impact Kernel

MVP 只支持：
`payment`

状态键：
`(nodeId, capability)`

不得使用 `visited: Set<nodeId>`。

底层 API：
`simulateScenario(unavailable: Set<ImpactStateKey>)`

图允许有环：

- BFS/queue
- visited
- cycle-safe

禁止把当前 MVP 宣称为通用跨 capability 引擎。

## 14. Impact 输出正确性

任何 `必须处理` 必须来自已确认现实状态。

不得由以下直接产生：

- Proposal
- confidenceScore
- fuzzy merchant match
- 单笔 Observation
- 未确认 Group
- 缺失数据

目标：
`confirmed false positive = 0`

## 15. Node Resolver

Parser 不得直接假设商户对应哪个服务节点。

顺序：

1. builtin alias exact
2. normalized exact
3. conservative fuzzy
4. 用户确认

未完成 resolution：
不得生成 DependencyProposal。

禁止 embedding / LLM / vector DB。

## 16. 网络与隐私

MVP 默认：

- NO BACKEND
- NO ACCOUNT
- NO ANALYTICS
- NO TELEMETRY
- NO ADS
- NO CLOUD SYNC

真实账单、密钥、开发者证书、用户数据不得提交 Git。

## 17. 日志

禁止日志输出：

- raw CSV row
- full user object dump
- source transaction id
- password
- SQLCipher secret
- HUKS/Keychain material
- fileEncryptionKey
- fpSecret
- decrypted depmap
- 真实账单内容

开发模式也遵守。

## 18. 测试方式

默认 test-first。

核心模块未绿之前不做 UI。

不得通过以下方式制造 PASS：

- 删除测试
- 降低断言
- skip 失败测试
- 捕获异常后静默忽略
- 写假实现并宣称完成

## 19. 状态声明

任何平台能力必须区分：

- IMPLEMENTED
- COMPILED
- TESTED
- DEVICE_VERIFIED
- STORE_READY

不得把“理论支持”写成“已验证”。

## 20. 外部 Blocker

只有无法由代码解决的事项才进入 BLOCKERS，例如：

- Apple Developer / signing / macOS Xcode
- Google Play developer account
- Huawei developer identity / AppGallery signing
- 最终 bundle/application id
- 真实微信账单
- 最终隐私政策 URL

普通编译错误、测试错误、依赖冲突不是用户 blocker。

## 21. Git

禁止提交：

- `.env` secrets
- keystore
- p12/p8
- provisioning profile
- signing password
- 真实账单
- local user DB
- decrypted `.depmap`

保持小步 commit。

## 22. 禁止的 MVP 扩展

当前不实现：

- Neo4j
- GraphRAG
- LLM
- Agent
- embedding
- vector DB
- GNN
- probability graph
- Bayesian network
- SAT solver
- rule DSL
- dominator / SPOF 高级分析
- N-of-M
- cross-capability inference
- event sourcing
- CRDT
- WebDAV / cloud sync
- 小程序
- NFC
- payment SDK
- bank crawler
- 自动读短信
- Accessibility 自动操作

想到新能力写入 `FUTURE.md`，不要实现。

## 23. 长任务执行

不要每一步停下来问是否继续。

持续：
读取 → 修改 → 测试 → 修复 → 更新状态 → 下一阶段。

只有真正外部 blocker 才记录并继续其他可执行工作。

## 24. 每次任务结束前

必须：

1. 运行适用测试
2. 检查 git diff
3. 检查 secret / real data
4. 更新 `WORK_STATUS.md`
5. 更新 `BLOCKERS.md`
6. 更新相关 docs / `FINAL_REPORT.md`
7. 明确写出测试证据和平台状态

不要只说“完成了”。

## 25. Engineering Baseline V1（2026-09-13 起长期生效）

任何 Agent 结束任务前必须：

- `npm run check`（core/ 下）：format:check + lint + typecheck + 全部测试 +
  architecture（含 circular deps = 0）+ network gate + secret scan 全绿。
- 重大 milestone（新 MVP 轮收口 / Schema 变更 / 平台收口）前必须 `npm run check:full`。

必须遵守并引用：

- `docs/ENGINEERING_STANDARDS.md`（命名/格式/lint/类型/错误模型总纲）
- `docs/DEFINITION_OF_DONE.md`（完成定义：不满足不得宣称完成）
- `QUALITY_GATES_V1.md`（Q0–Q19 Gate 清单与状态）
- `docs/AGENT_DEVELOPMENT_PROTOCOL.md`（会话开始顺序、协作纪律、禁止项）
- `docs/CHANGE_RISK_POLICY.md`（修改前先定风险级别）

统一命令（core/）：
`npm run check` / `check:full` / `check:invariants` / `check:contract` /
`check:property` / `check:db-integrity` / `check:architecture` / `check:network` /
`check:secrets` / `check:deps` / `test:perf` / `test:stability`。
