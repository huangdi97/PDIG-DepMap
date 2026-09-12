# MONEY_CURRENCY_RULES.md — 金额 / 货币工程规则（Engineering Baseline V1）

> 本项目不是记账/财务资产管理工具（AGENTS.md §4）。金额仅作为 evidence 元数据参与处理。

## 现状审计结论（2026-09-13）

1. **存储**：`amount` 一律为非负 number（方向由 `direction: 'in'|'out'|'neutral'` 表达），
   Adapter 层负责取绝对值（OFX 负金额 → out + abs；CSV 按 `amountSignMode` 显式判定）。
2. **参与比较的场景**：仅 `detectRecurrence` 的「金额稳定度」
   （`src/parser/wechat/recurring.ts`：`1 - max|a - median| / median`，相对偏差，非相等比较）。
   该值只进入 recurrence `confidence`（展示元数据），**不参与**：
   - Dependency / Proposal 状态机决策
   - Impact `must_change` 判定
   - Fingerprint 去重
3. **不存在**：余额聚合、报表合计、金额相等判定、跨币种换算。
4. **浮点语义**：解析使用 `Number` + `Number.isFinite` 校验（`parseAmount`/`parseOfxAmount`），
   显示/稳定性计算允许浮点近似。因为不做聚合与相等比较，未引入 decimal 库（记录为决策）。
5. **跨币种边界（已声明限制）**：`detectRecurrence` 按 `merchantRaw` 分组，未按币种二次分区。
   缓解：(a) recurrence 输出仅为元数据（见第 2 条）；(b) WeChat 流单币种（CNY）；
   (c) Generic CSV 多币种账单的同名商户若币种混排，recurrence 置信度不可信 —— 这是
   已知边界而非正确性缺陷。若未来 recurrence 参与任何决策，必须先按币种分区。

## 规则（后续变更必须遵守）

- R1 新增任何金额**相等/聚合/比较**逻辑前，必须先定义 rounding/tolerance/decimal 语义并补 property 测试。
- R2 不同 `currency` 的观测不得被任何稳定性/重复性算法直接混合比较。
- R3 金额方向必须来自显式声明（signed+positiveDirection / debit_credit / outward_positive），
  禁止猜测；零金额 → `neutral`（MVP02 生产修复 #2 的回归口径）。
- R4 缺失金额不得伪造为 0（MVP02 生产修复 #3：`parseOfxAmount('') → null`）。
