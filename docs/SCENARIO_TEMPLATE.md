# SCENARIO_TEMPLATE.md — 场景模板（MVP03 §35–§41）

## 定义

ScenarioTemplate 是**静态产品模板/配置**（`src/scenarios/registry.ts`），不是 Graph Reality；
不得直接修改 Dependency —— 只能通过 `instantiateScenario` 产出 ChangePlan（记录 templateId）。

```
Template → 用户选择目标对象 → 少量参数 → ChangePlan（draft）
         → simulateScenario → ImpactSnapshot → Rebase/Readiness → Action → Verification
```

## Active 模板（MVP03，payment-domain only）

| id                         | 标题           | 说明                                   | 建议提前量 |
| -------------------------- | -------------- | -------------------------------------- | ---------- |
| `replace_payment_card`     | 更换银行卡     | 换卡前检查支付钱包、自动扣款和订阅关系 | —          |
| `expiring_payment_card`    | 银行卡即将到期 | 到期前检查仍依赖这张卡的支付路径       | 30 天      |
| `close_payment_instrument` | 注销银行卡     | 注销前确认哪些支付关系需要迁移         | 14 天      |

requiredInputs：`targetPaymentInstrumentId`；optional：`replacementPaymentInstrumentId` /
`effectiveDate`。`recommendedLeadTimeDays` 只是产品建议，不是法律/金融保证，用户可调整。

## 结构

```ts
{ id, category, title, description, supportedCapabilities, requiredInputs, optionalInputs,
  recommendedLeadTimeDays, availability: 'active'|'planned', scenarioFactory }
```

**planned 模板 factory 恒为 null** —— 不可执行是类型级表达；`instantiateScenario` 对
planned / 缺 required input / 未知 id 一律抛错（ST-002/004）。production UI 只展示 active
（场景库页面无 planned 占位）。

## 默认动作链

每模板产出 prepare / change / verify 三段动作；change/verify 带 `future_observation`
verification（pending）。创建/Rebase 不自动完成任何动作（PRB-008）。

## 测试证据（ST-001..005，tests/services/scenario-timeline.test.ts）

3 active 模板 + capability gate / planned 不可执行 / factory 产出（templateId、baseline、
默认动作、不自动完成）/ required input 校验 / 政策 gate（无日常生活提醒类模板）。
