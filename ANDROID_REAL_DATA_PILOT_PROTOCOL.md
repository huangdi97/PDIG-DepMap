# ANDROID_REAL_DATA_PILOT_PROTOCOL.md

> 生成时间：2026-09-20（Android Product Finalization）
> 状态：**PROTOCOL READY / NOT EXECUTED** —— 未经用户授权不使用任何真实账单（AGENTS §16 / §20）。
> 当前：`REAL_DATA_CORRECTNESS = BLOCKED_BY_REAL_DATA`、`REAL_DATA_VALUE = BLOCKED_BY_REAL_DATA`。

---

## 1. 目的

用**用户授权的真实账单**验证产品在真实数据上的正确性与价值，而不只是 synthetic fixture。
本轮没有授权数据，因此只准备协议；任何 synthetic 数据**不得冒充** real data 结论。

## 2. 授权前提（不可跳过）

- [ ] 用户本人提供（或授权）**自己的**一张账单文件（微信账单导出 CSV / 银行 OFX-QFX / 通用 CSV）
- [ ] 用户明确知晓：文件仅在本机解析，不上传；解析结果只用于本次验证
- [ ] 文件落地到 `local_private/`（gitignored）后**立即从状态追踪中排除**，绝不入库
- [ ] 验证报告中只出现脱敏结论（服务名可保留、金额/卡号/账号**必须脱敏**）

## 3. 执行步骤

1. 在设备（emulator 或真机）全新安装 app。
2. 导入授权账单（真实 SAF 选择）。
3. 记录：解析行数、错误行数、Node Resolution 结果、生成的 Proposal 数。
4. 走完一个场景（如 `replace_payment_card`）：影响面 → 计划 → 完成 → 验证。
5. 用户复核每一条机器结论（Proposal 是否有用、是否误报、是否遗漏）。
6. 汇总成 `ANDROID_REAL_DATA_ACCEPTANCE.md` 中的指标。

## 4. 指标（对照 ANDROID_REAL_DATA_ACCEPTANCE.md）

| 指标                     | 定义                                            | 目标                                         |
| ------------------------ | ----------------------------------------------- | -------------------------------------------- |
| Precision（must_change） | 用户确认的 must_change / 机器标出的 must_change | 追求无假阳性（confirmed false positive = 0） |
| false must_change 数     | 机器标 must_change 但用户说不对                 | **0**（硬目标）                              |
| Node Resolution 正确率   | 正确识别支付方式/收款对象比例                   | ≥95%（synthetic 参考线）                     |
| Proposal 有用率          | 用户确认/采纳的 Proposal 比例                   | ≥80%                                         |
| 遗漏 dependency          | 用户补充确认了机器没发现的依赖                  | 记录并分类                                   |
| 完成一次换卡排查时间     | 从导入到 verified 的端到端时间                  | 记录，不预设                                 |

## 5. 禁止

- Synthetic fixture 结果写入 real-data 报告。
- 展示真实金额/卡号/账号到任何报告、截图、log。
- 把「未执行」写成「已验证」。

---

## 配套文档

- `ANDROID_REAL_DATA_PRIVACY.md` —— 数据边界与脱敏规则
- `ANDROID_REAL_DATA_ACCEPTANCE.md` —— 验收门禁与当前状态
