# PRODUCT_TERMINOLOGY_V0_2.md

> PDIG / DepMap v0.2.0 双端统一用户术语词典。
> 目的：Android 与 Desktop **对外** 使用同一用户语义；内部工程术语（wire 字段、枚举名）一律不上屏。
> 用法：所有 UI 文案、无障碍标签、用户文档必须使用本词典「用户语义」一列；
> 工程侧仍可用内部字段，但**任何**引入新内部术语上屏的改动都必须先改这里（goal §66/§69）。
> 更新：2026-09-25（v0.2.0）。

## 1. 概念级术语（最重要）

| 内部术语           | 用户语义（双端统一） | 禁止的旧说法                              |
| ------------------ | -------------------- | ----------------------------------------- |
| RealityDrift       | 可能发生了变化       | 现实漂移 / drift                          |
| DiscoveryCandidate | 待确认服务           | 候选对象（Desktop 旧标题）/ candidate     |
| DependencyProposal | 待确认关系           | Proposal 确认（Desktop 旧标题）/ proposal |
| ScenarioCoverage   | 本次分析依据         | coverage / 覆盖率（不带安全含义）         |
| PlanReadiness      | 是否可以继续         | readiness / ready 度                      |
| ChangePlan         | 变更计划             | plan / 就绪度 + wire 值                   |
| ActionVerification | 验证                 | verification 英文裸展示                   |
| Dependency         | 依赖关系（已确认的） | edge / 边                                 |
| Node               | 对象                 | node / 节点                               |
| Payment instrument | 支付方式 / 这张卡    | instrument                                |
| Source             | 数据来源             | source / adapter                          |
| Impact 分析        | 影响分析             | 模拟 / simulate                           |
| Graph（图）        | 依赖图（高级视图）   | graph viewer                              |
| Evidence           | 依据 / 证据          | evidence 裸展示                           |
| Confirmed reality  | 你确认的信息         | reality / truth                           |

## 2. Impact 结果类别（§35）

| 内部枚举               | 用户语义             |
| ---------------------- | -------------------- |
| must_change            | 必须处理             |
| backup_path / degraded | 可能还有其他可用方式 |
| needs_review           | 需要确认             |
| unaffected             | 当前未发现影响       |

内部值如 `must_change` 严禁直接显示。

## 3. Plan 状态与 CTA（§41）

| 内部状态               | 用户语义               | 主按钮（CTA） |
| ---------------------- | ---------------------- | ------------- |
| blocked                | 需要先处理必须事项     | 处理必须事项  |
| review_required        | 需要继续确认           | 继续确认      |
| needs_revalidation     | 需要重新检查           | 重新检查      |
| ready_with_known_scope | 基于当前信息，可以继续 | 继续下一步    |
| verifying              | 正在验证               | 查看验证      |
| completed              | 已完成                 | 查看结果      |

## 4. Verification（§43-44）

| 内部状态           | 用户语义             |
| ------------------ | -------------------- |
| NOT_REQUIRED       | 无需验证             |
| PENDING            | 待验证               |
| EVIDENCE_SUGGESTED | 发现新的依据，请确认 |
| VERIFIED           | 已验证               |
| FAILED             | 验证失败             |

铁律：`我做完了 ≠ 系统已确认生效`；「发现新的依据」不等于自动 verified。

## 5. 其他固定短语（双端必须一致）

| 场景           | 用户语义                                                                                                       |
| -------------- | -------------------------------------------------------------------------------------------------------------- |
| 更换银行卡     | 换卡前检查支付、订阅和钱包依赖                                                                                 |
| 银行卡即将到期 | 到期前看看还有哪些支付关系依赖这张卡                                                                           |
| 注销银行卡     | 注销前确认哪些支付关系需要迁移                                                                                 |
| Candidate 动作 | 确认 / 忽略 / 稍后处理                                                                                         |
| Drift 动作     | 已经换成新的来源 / 两个都在使用 / 没有变化 / 稍后确认                                                          |
| Proposal 说明  | 我们发现了一条可能的关系，但还需要你确认。                                                                     |
| 导入隐私声明   | 文件只在本机解析，不会上传。完整流水不会保存为账本，仅保留分析所需的摘要、证据和你确认的信息。                 |
| 删除全部数据   | 将永久删除这台设备上的全部记录（数据库、密钥、缓存与工作流状态），此操作无法撤销；不会删除你已导出的备份文件。 |
| Backup 说明    | 备份 = 复制一份加密的 .depmap 容器；备份内容用你设置的密码加密，密码无法找回。                                 |
| Restore 说明   | 恢复会用备份中的内容替换这台设备上当前的全部数据；不会留下半恢复状态。                                         |

## 6. 能力（capability）映射

| 内部值        | 用户语义 |
| ------------- | -------- |
| payment       | 支付     |
| （其他/未知） | 未分类   |

## 7. 关系（relation）映射

| 内部值             | 用户语义     |
| ------------------ | ------------ |
| funding_source     | 资金来源     |
| merchant_agreement | 服务付费关系 |
| （其他）           | 关系         |

## 8. 对象种类（kind）映射

| 内部值             | 用户语义 |
| ------------------ | -------- |
| payment_instrument | 支付方式 |
| counterparty       | 收款对象 |
| （其他）           | 其他对象 |

## 9. 关键性（criticality）

| 内部值   | 用户语义       |
| -------- | -------------- |
| required | 必需           |
| unknown  | 重要程度未确认 |

`required` 只能来自用户确认（机器永不自动产生）。

## 10. 强制项核对清单

1. 任何 UI 文本若出现：drift / proposal / candidate / coverage / readiness / edge / node（裸） / capability / criticality / wire / graphRevision / mustChangeKeys / fieldJson / scenarioTemplateId / unavailableSet —— 均为违规，按本词典替换。
2. 双端每个概念必须选中本词典且**唯一**的用户语义（允许平台差异的只是布局/交互，不是语义）。
3. 用户文档（docs/user/*）与本词典一致。
