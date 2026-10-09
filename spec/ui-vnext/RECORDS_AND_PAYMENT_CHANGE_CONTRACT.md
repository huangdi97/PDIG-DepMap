# PDIG Records & Payment Change Contract

> Date: 2026-10-09  
> Status: **DESIGN_FROZEN / R21 REFERENCE CONTRACT**
>
> Scope:
> - 一级「记录」的产品边界；
> - `replace_payment_card` 在 Android UI vNext 的消费者连续性表达；
> - Preview 与 Production Reality 的严格边界。
>
> Canonical schema change: **NONE**.

## 1. Records 的唯一问题

「记录」回答：

> **发生过什么、验证过什么、依据是什么？**

它不是第二个「现在」。

因此以下信息的默认归属是：

```text
需要关注 / 待处理
→ 现在

未来到期 / 保号 / 年费节点
→ 现在 / Maintain

正在准备或执行的变更
→ 变更

已经发生、完成、验证、确认、导入、审核的事实
→ 记录
```

Records 可以提供“关联计划 → 继续处理”的上下文链接，但这个链接本身不是一条历史事件。

## 2. Records truth grammar

记录中的每一项至少需要区分：

```text
recorded_complete
verified
pending_verification
confirmed_review
imported
```

最重要的永久规则：

```text
done != verified
```

允许：

```text
计划步骤标记为 completed
→ Records: 已记录完成
```

只有存在显式验证状态/证据时才允许：

```text
→ Records: 已验证
```

禁止：

```text
completed
→ UI 自动显示 verified

时间经过
→ verified

打开页面
→ verified

进入下一阶段
→ verified
```

## 3. R21 Reference Records

R21 Preview 的 Records 数据来自已有 synthetic ChangeStage。

它只投影：

- 已记录完成阶段；
- 正在等待验证的阶段。

它不把：

- `not_started`；
- future `blocked`；
- Attention；
- Upcoming；

伪装成已经发生的历史。

当前 reference fixture 中：

```text
stage 1 completed
stage 2 completed
stage 3 verifying
```

因此 R21 Records summary 应为：

```text
已记录完成 = 2
已验证 = 0
待验证 = 1
```

这里的 0 是由当前 reference trace 明确计算得到，不是“真实用户没有验证历史”的断言。

## 4. Production Records binding

生产版 Records 必须从真实域读：

```text
AppContainer.timeline()
ChangePlan / PlanDetailView
Review decisions
Verification events
Evidence references
Source ingestion/import events
future canonical Maintenance events
```

生产 UI 不应从 Attention 列表“反推历史”。

推荐的生产 read model：

```text
RecordTraceItem
  eventId
  eventKind
  targetRefs[]
  happenedAt / recordedAt
  completionState?
  verificationState?
  evidenceRefs[]
  sourceRef?
  planId?
  actionId?
  provenance
```

同一事件可以：

```text
completed = true
verification = pending
```

这必须可表达。

R21 Android source now implements the first authoritative production trace over
`PlanDetailView` actions:

```text
done + no verification             → RECORDED_COMPLETE
verification = verified            → VERIFIED
verification = pending/suggested   → PENDING_VERIFICATION
verification = failed              → VERIFICATION_FAILED
untouched future action            → excluded
```

Verification `evidenceRefs` are preserved as references. The current Android
`PlanAction` model does not expose `doneAt/verifiedAt`, so
`VNextProductionRecordItem.occurredAt` intentionally remains null. The plan's
`effectiveDate` is **not** reused as a fake event timestamp.

This is still a source/read-model seam; production UI binding remains gated.

## 5. Payment Change 为什么现在可以进入 UI

Canonical/production ScenarioRegistry 已有 active：

```text
replace_payment_card
expiring_payment_card
close_payment_instrument
```

其中 `replace_payment_card` 的执行语义明确包含：

```text
1. 检查依赖
2. 迁移必要支付关系
3. 验证关键支付路径
```

所以 Android 不再需要把“更换银行卡”停留在纯概念提示。

但是 Preview 仍然不能自己执行生产计划。

## 6. Card Change reference hierarchy

```text
Selected Card
→ Confirmed recorded payment relations
→ Optional replacement-card target
→ Continuity stages
→ Impact Lens
→ Current / Transition / After projection
```

视觉模型：

```text
OLD CARD
   ↓ / →
recorded payment relations
   ↓ / →
NEW CARD (optional plan target)
```

替代卡片是可选计划输入，不是算法推荐。

UI 可以列出其他已记录 active 卡片供用户选择，但必须明确：

```text
candidate object != recommended replacement
candidate object != independent backup path
candidate object != already migrated
```

## 7. Card Change three projections

### Current

表示：

- 当前旧卡；
- 当前已记录依赖；
- 尚未执行迁移。

阶段：

```text
检查支付依赖          not_started
迁移支付关系          not_started
验证支付路径          not_started
```

### Transition

有替代卡：

```text
检查支付依赖          completed
迁移支付关系          in progress / verifying UI state
验证支付路径          blocked
```

没有替代卡：

```text
检查支付依赖          completed
迁移支付关系          blocked
验证支付路径          blocked
```

### After

全部是 **Plan Projection**：

```text
检查支付依赖          plan
迁移支付关系          plan
验证支付路径          plan
```

禁止将 After 渲染成实际完成状态。

## 8. Payment verification

支付关系迁移不能只以“用户点了完成”作为最终成功。

生产 ScenarioRegistry 已明确：

> 用一次真实支付/账单证据确认关键路径可用。

因此：

```text
migration action done
!=
payment path verified
```

生产 VNext 必须复用：

```text
createPlanForScenario("replace_payment_card")
→ completeAction()
→ verifyAction()
→ planDetail()
```

而不是 Compose 内部自己保存 done/verified。

## 9. Card Detail CTA

因为 `replace_payment_card` 是 active production scenario，Card Detail 可以显示：

> **分析更换此卡的影响**

这不是 ghost capability。

CTA 进入 Change Card reference/production plan preparation。

Card art / PresentationProfile 仍是完全不同的功能：

```text
更换卡面图片
!=
更换银行卡
```

两者不得混淆。

## 10. Change Center

一级「变更」是工作中心，不是“更换手机号”的别名。

至少可见：

```text
正在进行的变更
准备改变
  更换手机号
  更换银行卡
维护与核对
```

进入 Change Center 本身不产生计划。

### Phone

可以进入 `replace_phone_number` reference flow。

### Card

Change Center 先让用户选具体卡片；
Card Detail 的 Impact Lens 再进入该卡的 Change Card。

不要在没有 target card 的情况下偷偷创建 production plan。

## 11. Navigation

新增 focused route：

```text
/change/card
```

Hierarchy：

```text
Change Card → Up → Change
```

System Back：

```text
Card Detail → Change Card
system Back → Card Detail
```

因为 Back 是真实访问历史，Up 是产品 hierarchy。

一级 Change 在以下页面保持选中：

```text
Change Center
Change Phone
Change Card
```

## 12. Search

允许搜索：

```text
更换银行卡
换卡
支付迁移
```

结果进入 Change Card route only when a target card has already been selected.

否则搜索结果应进入：

```text
Cards / Change Center
```

并要求用户选择目标。

**R21 当前实现中 Search command 只暴露 Change Card 能力名称；真正 target selection 仍由 Card Detail / Cards 完成。**

## 13. Privacy

Card Change 必须继续使用：

- workspace masking；
- per-card PresentationProfile masking。

不得在 migration service rows、日志或测试 artifact 中输出完整卡号。

Replacement candidate thumbnail 也遵守 masking。

## 14. Cross-width translation

Compact：

```text
OLD
 ↓
relations
 ↓
NEW
```

Medium / Expanded：

```text
OLD → relations → NEW
```

语义相同，布局不同。

三投影、Impact Lens、计划边界不随宽度变化。

## 15. Runtime acceptance

R21 runtime pack 至少需要：

```text
Records summary
Records trace
Records done != verified copy

Card Change Current
Card Change Transition without replacement (blocked)
Card Change Transition with replacement
Card Change After (plan projection)

phone
tablet / medium
expanded source contract
```

必须证明：

- 五个一级 Tab 仍存在；
- Change root 仍选中；
- Card Change Up → Change；
- System Back 返回真实前页；
- After 不冒充 Reality；
- Records 不重新出现 Attention / Upcoming feed；
- no full card number leakage。

## 16. Production stop line

```text
R21_REFERENCE_RECORDS = IMPLEMENTED_SOURCE
R21_REFERENCE_CARD_CHANGE = IMPLEMENTED_SOURCE

PRODUCTION_RECORDS_READ_MODEL = SOURCE_IMPLEMENTED
PRODUCTION_CARD_CHANGE_GATEWAY = SOURCE_AVAILABLE
PRODUCTION_CARD_CHANGE_SCREEN_BINDING = HOLD

ANDROID_REFERENCE_FREEZE = HOLD
PRODUCTION_VNEXT_CUTOVER = HOLD
```

R21 关闭的是产品/交互设计断层，不越过 Runtime / Production Authority gate。
