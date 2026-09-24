# REAL_DATA_PILOT_0_PROTOCOL.md

> 生成时间：2026-09-22（ANDROID_CANONICAL_FREEZE → Production/Reality Closure 轮）
> 状态：**PROTOCOL READY / NOT EXECUTED** —— 未经用户授权不使用任何真实账单（AGENTS §16 / §20）。
> 当前：`REAL_DATA_CORRECTNESS = BLOCKED_BY_REAL_DATA`、`REAL_DATA_VALUE = BLOCKED_BY_REAL_DATA`。
>
> ⚠ **命名口径**：这是 **Pilot-0（First Real Data Closure）** —— 1 位用户、1 份本人授权账单，
> 只用于回答「spec 在真实数据上是否产生有用结果」。**不得**据此宣称产品价值已广泛验证。

---

## 1. 目的与范围

- Synthetic fixture 已证明：**代码符合 spec**（conformance 91/91、core 71/71、设备内 59/59）。
- Pilot-0 要证明的是：**spec 在真实数据上是否有用** —— 即 Node Resolution / Proposal / Impact
  对一份真实账单是否给出用户认可的结论。
- 本轮没有授权数据，因此**只准备协议**；任何 synthetic 数据不得冒充 real data 结论。

## 2. 授权前提（不可跳过）

- [ ] 用户本人提供（或授权）**自己的**一份账单文件（微信账单导出 CSV / 银行 OFX-QFX / 通用 CSV）
- [ ] 用户明确知晓：文件仅在本机解析，不上传；解析结果只用于本次验证
- [ ] 文件落地到 `local_private/`（gitignored）后**立即从版本管理中排除**，绝不入库
- [ ] 验证报告中只出现脱敏结论（服务名可保留、金额/卡号/账号**必须脱敏**）

## 3. Parser 验证

| 检查项                | 记录                                   |
| --------------------- | -------------------------------------- |
| row parse correctness | 解析行数 / 错误行数 / 跳过行数         |
| date                  | 日期字段解析正确率（真实格式 vs spec） |
| amount                | 金额解析正确率（含负值/币种）          |
| currency              | 币种识别（多币种账单）                 |
| description           | 描述字段保留完整性                     |
| transaction identity  | 交易身份（去重/防重）判定              |

> 目标：与 synthetic 参考线一致（conformance 覆盖的全部格式族）；差异逐条记录原因。

## 4. Merchant / Node Resolution 验证

按 spec 顺序（**builtin alias exact → normalized exact → conservative fuzzy → 用户确认**；
禁止 embedding / LLM / vector DB）：

| 统计               | 定义                               |
| ------------------ | ---------------------------------- |
| correct resolution | 机器正确解析到服务节点（用户认可） |
| ambiguous          | 多个候选，需用户选择               |
| false merge        | 机器把两个不同服务合并成一个节点   |
| false split        | 机器把同一服务拆成多个节点         |
| unresolved         | 无法解析，留在未确认状态           |

> 未完成 resolution 的商户**不得**生成 DependencyProposal（spec / AGENTS §15）。

## 5. Proposal 验证

| 统计            | 定义                                   |
| --------------- | -------------------------------------- |
| total proposals | 机器产生的 Proposal 总数               |
| accepted        | 用户确认采纳                           |
| rejected        | 用户拒绝                               |
| uncertain       | 用户无法判断                           |
| useful          | 用户认为有用（含 rejected 但信息有用） |
| obvious noise   | 用户认为纯噪音                         |

同 key Proposal 生命周期（UPSERT）：pending → 继续累计 evidence；accepted → 不重复问；
rejected → 有新 evidence 才重提（AGENTS §11）。

## 6. Reality Boundary（不可跨越）

- **Observation 不得自动升级为 Confirmed Reality** —— 解析结果只产生 Observation/Proposal。
- **Proposal 不得自动创建 `required` Dependency** —— `required` 只能用户确认（AGENTS §9/§11）。
- **Recorded absence ≠ real-world absence** —— 机器没发现 ≠ 现实不存在。
- **confidence ≠ confirmation**；**Coverage ≠ Readiness**。

## 7. Impact 验证（重点）

| 检查项                | 定义                                   | 严重度                   |
| --------------------- | -------------------------------------- | ------------------------ |
| **false must_change** | 机器标 must_change 但用户确认无需处理  | **高严重度，硬目标 = 0** |
| false unaffected      | 机器标 unaffected 但实际受影响（漏报） | 记录并分类               |
| false backup_path     | 误判为可替换路径                       | 中                       |
| needs_review quality  | needs_review 项是否对用户有指导价值    | 记录                     |

> `must_change` 只能由 Confirmed Reality 中的高确定性条件产生（AGENTS §13/§14）。

## 8. 指标表与目标

| 指标                     | 定义                                            | 目标                                     |
| ------------------------ | ----------------------------------------------- | ---------------------------------------- |
| Precision（must_change） | 用户确认的 must_change / 机器标出的 must_change | 无假阳性（confirmed false positive = 0） |
| false must_change 数     | 机器标 must_change 但用户说不对                 | **0**（硬目标）                          |
| Node Resolution 正确率   | 正确识别支付方式/收款对象比例                   | ≥95%（synthetic 参考线）                 |
| Proposal 有用率          | 用户确认/采纳的 Proposal 比例                   | ≥80%（参考线，非硬门禁）                 |
| 遗漏 dependency          | 用户补充确认了机器没发现的依赖                  | 记录并分类                               |
| 换卡排查时间             | 从导入到 verified 的端到端时间                  | 记录，不预设                             |

## 9. 执行步骤

1. 在设备（emulator 或真机）全新安装 app。
2. 导入授权账单（真实 SAF 选择）。
3. 记录：解析行数、错误行数、Node Resolution 结果、生成的 Proposal 数。
4. 走完一个场景（如 `replace_payment_card`）：影响面 → 计划 → 完成 → 验证。
5. 用户复核每一条机器结论（Proposal 是否有用、是否误报、是否遗漏）。
6. 汇总成 `ANDROID_REAL_DATA_ACCEPTANCE.md` 中的指标。

## 10. 禁止

- Synthetic fixture 结果写入 real-data 报告。
- 展示真实金额/卡号/账号到任何报告、截图、log。
- 把「未执行」写成「已验证」；把 Pilot-0 写成「广泛验证」。

---

## 配套文档

- `REAL_DATA_PRIVACY_PROTOCOL.md` —— 数据边界与脱敏规则
- `ANDROID_REAL_DATA_ACCEPTANCE.md` —— 验收门禁与当前状态
