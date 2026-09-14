# PLAN_READINESS.md — 计划就绪度（MVP03 §17–§20）

## 三值口径（永久）

| 值                       | 文案                                           | 语义                                                                                                                     |
| ------------------------ | ---------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------ |
| `blocked`                | 「还有必须处理的事项。」                       | 存在未处理的 must_change                                                                                                 |
| `review_required`        | 「还有信息需要确认。」                         | needs_review / unknown criticality / 未解析候选 / 待确认 proposal / stale 依赖 / needs_revalidation / 未完成 change 动作 |
| `ready_with_known_scope` | 「基于当前已知并确认的信息，可以继续下一步。」 | 已知范围内全部处理完毕                                                                                                   |

**永久禁止**：safe / 100% safe / all clear / 保证安全。ready 必须伴随免责声明
「基于当前已知并确认的数字基础设施」。

## 纯规则引擎（禁止项为结构性）

`src/services/plan-readiness.ts` 的 `computePlanReadiness`：

- **无 LLM**（无任何模型调用面）；
- **confidence 不参与**：输入类型中不存在 confidence 字段（PI-3 property 断言）；
- **absence 不参与**：没有「没看到交易」之类的输入通道（结构性，非运行时检查）；
- source 数量不映射 ready。

`pendingMustChange` 口径：影响中 must_change 目标数 − 已完成 change 阶段动作数
（影响告诉你必须改什么；完成对应动作 = 已处理，与 PRB-008/VF 语义一致）。

## 测试证据

§20 八条用例 + stale 依赖用例（tests/services/plan-readiness-coverage.test.ts）+
PI-1（mismatch ⇒ 永不 ready，fast-check ×200）+ INV-16（真实 DB 链路）。
人工变异 M-R2（blocked 规则失效）= KILLED。

## UI

Plan Detail 顶部显示 readiness 与标准文案；needs_revalidation 时显示重新检查横幅 +
「重新分析」按钮（§48/§49）。
