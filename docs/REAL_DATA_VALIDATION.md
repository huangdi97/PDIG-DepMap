# REAL_DATA_VALIDATION.md — 真实数据双 Gate

## 状态：NOT_RUN

> 未见真实微信账单。`local_private/` 目前只有 README。
> **禁止**用 synthetic 测试冒充 real-data PASS（MVP_ACCEPTANCE §G）。

## 用户操作流程（账单到手后）

1. 将真实账单 CSV 放入 `local_private/`（该目录被 .gitignore 排除，禁止入库）
2. 运行本地验证 CLI（不联网）：

```bash
cd core
node --experimental-strip-types scripts/validate-real-bill.ts --file ../local_private/<账单>.csv
```

3. CLI 执行 pipeline：账单 → parse → fingerprint → resolver → proposal → confirmation → graph → simulate
4. 人工审计输出

## Correctness Gate

- 所有「必须处理（must_change）」逐条人工核实为真依赖
- 指标：confirmed false positive = 0
- 未确认备用关系不得展示为“安全可切换”
- 失败 → 如实记录 FAIL + 案例，回修 kernel/parser

## Value Gate（至少满足一条）

- 找到至少一件用户原本容易漏掉的依赖（如：某自动续费走的是已注销卡的资金通道）
- 明显缩短“销卡前要检查什么”的完整排查时间（记录人工对照时间）

## 体检报告原则（CANONICAL §6.8）

只陈述“事实 + 不确定性”（如“3 项持续扣费超过半年，建议复核”），
绝不输出账单无法支持的结论（如“半年未使用”）。

## 结果记录（占位）

```
Correctness Gate: NOT_RUN
Value Gate:       NOT_RUN
样本量：0 笔 / 0 项建议 / 0 项 must_change
```
