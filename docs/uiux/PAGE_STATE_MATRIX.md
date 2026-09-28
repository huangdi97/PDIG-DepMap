# PAGE_STATE_MATRIX.md

> PDIG v0.3.1 UI/UX Refinement · 2026-09-28 · 每页状态矩阵（spec §75）
> 状态列：first use / empty / loading / content / attention / blocked / verification / error / retry / dark / large text。
> N/A + reason 表示该状态对该页面不适用。实现按冻结方向（PDIG_DESIGN_SYSTEM §15 Empty/Loading/Error 成套）落地。

## Desktop（Compose Desktop）

| Page | first use | empty | loading | content | attention | blocked | verification | error | retry | dark | large text |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Home | Healthy Brief（无文件不可达——Gate 先行） | Healthy Brief + 检查范围/未知范围 | N/A（内存态无异步加载） | 六段 Briefing | 需要你处理段置顶 + danger icon | N/A | N/A | ErrorStrip | ErrorStrip 内「重试」由动作按钮承载 | token dark scheme | 标题/正文可换行，无固定高度裁剪 |
| Attention | EmptyState（无事项） | 同 first use | N/A | 事项列表行 | 顶部即 attention 列表 | N/A | N/A | ErrorStrip | 同上 | dark | 可换行 |
| Findings | EmptyState（无薄弱点）+ 主动准备提示 | 同 first use | N/A | FindingCard what/why/next | must_change 类 danger 图标突出 | N/A | N/A | ErrorStrip | 同上 | dark | 可换行 |
| Infrastructure | EmptyState（去导入） | 同 first use | N/A（同步读） | Master-Detail（By Item/By Capability） | 选中项高亮 | N/A | N/A | ErrorStrip | 同上 | dark | 左列/右窗可滚动 |
| Scenario Center | 两分类卡片（支付/身份） | 无 active 场景时各分类空 | N/A | 场景卡片（何时用/检查什么/步骤） | N/A | N/A | N/A | ErrorStrip | 同上 | dark | 可换行 |
| Scenario Setup | EmptyState（去确认对象） | 无可选对象 EmptyState+下一步 | N/A | 场景信息 + 选择 + CTA | Replace Phone：Continuity Rail 全链 | 停用旧号步骤 blocked + 明文原因 | 验证步骤 verifying | ErrorStrip | 同上 | dark | Rail 文字换行 |
| Impact | EmptyState（未选节点） | 四类分组全部「无」态 | N/A（同步读） | 四类分组 + 检查范围 | must_change danger 行 | N/A | N/A | ErrorStrip | 同上 | dark | 可换行 |
| Plan | EmptyState（未找到计划） | 计划无动作 | N/A | 状态卡 + Continuity Rail 步骤 | 未解决 must 计数 danger | 停用旧步骤 blocked + 原因 | 验证步骤 verifying/verified | ErrorStrip | 同上 | dark | Rail 内容换行 |
| Actions | EmptyState（未选/无动作） | 同 first use | N/A | 步骤轨道 + 完成/验证按钮 | blocked 步骤 danger | 同上 | verified 双勾 | ErrorStrip | 同上 | dark | 同上 |
| Verification | EmptyState（无计划） | 计划无动作 | N/A | 每动作验证状态行 | verified 强于 completed | failed danger | verified/verifying/failed 三态 | ErrorStrip | 同上 | dark | 可换行 |
| Timeline | EmptyState（无事项） | 同 first use | N/A | 桶分组列表 | attention 桶置顶 | N/A | N/A | ErrorStrip | 同上 | dark | 可换行 |
| Sources | EmptyState（去导入） | 同 first use | N/A | 来源卡片 + 入口 | error 来源 danger chip | N/A | N/A | ErrorStrip | 同上 | dark | 可换行 |
| Import | 向导 idle | N/A（向导本身是流程） | 解析中 loading | stage 0/2/3 | 解析错误明细 | N/A | N/A | 解析失败文案+重试 | 显式重试按钮 | dark | 表单可滚动 |
| Backup | N/A（无数据也可备份空文件） | 无文件时按钮说明 | 导出中 spinner | 文件信息 + 导出/导入 | N/A | N/A | N/A | 口令错误/文件被改文案 | 重试 | dark | 可换行 |
| Restore | EmptyState（未选文件） | 同 first use | 恢复中 spinner | 文件信息 + 口令 | N/A | N/A | N/A | 密码错误文案 | 重试 | dark | 可换行 |
| Settings | N/A（静态入口） | N/A | N/A | 入口列表 | 危险操作双步确认 | 危险操作需 confirmMode | N/A | ErrorStrip | 同上 | dark | 可换行 |
| Security | N/A（静态） | N/A | N/A | DPAPI 开关 + 说明 | N/A | N/A | N/A | ErrorStrip | 同上 | dark | 可换行 |
| About | N/A（静态） | N/A | N/A | 版本/构建/发布说明 | N/A | N/A | N/A | N/A | N/A | dark | 可换行 |

## Android（Compose/M3）

| Screen | first use | empty | loading | content | attention | blocked | verification | error | retry | dark | large text |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Home | Healthy Brief | 同 first use | LoadingState（IO 读） | 六段 Briefing | 需要你处理段 | N/A | N/A | ErrorState | 重试按钮 | token dark | 可滚动不裁剪 CTA |
| Findings | EmptyState | 同 first use | LoadingState | Finding 卡 | must_change 突出 | N/A | N/A | ErrorState | 重试 | dark | 同上 |
| Scenario Center | 两分类 | 空分类 | LoadingState | 场景卡 | N/A | N/A | N/A | ErrorState | 重试 | dark | 同上 |
| Scenario Setup | EmptyState | 无可选对象 | LoadingState | 选择 + CTA | Replace Phone 步骤轨道 | 停用旧号 blocked+原因 | verifying | ErrorState | 重试 | dark | 同上 |
| Impact | EmptyState | 四类「无」态 | LoadingState | 四类分组 | must_change | N/A | N/A | ErrorState | 重试 | dark | 同上 |
| ChangePlan | EmptyState | 无动作 | LoadingState | 步骤轨道 | blocked 步骤 | 同上 | verified>completed | ErrorState | 重试 | dark | 同上 |
| Verification | EmptyState | 无动作 | LoadingState | 验证状态行 | failed | N/A | 三态 | ErrorState | 重试 | dark | 同上 |
| Infrastructure | EmptyState | 同 first use | LoadingState | 搜索+分组+分段 | 选中高亮 | N/A | N/A | ErrorState | 重试 | dark | 同上 |
| Settings | N/A | N/A | N/A | 入口列表 | 删除双步确认 | confirmMode | N/A | ErrorState | 重试 | dark | 同上 |

## iOS（SwiftUI）

| Screen | first use | empty | loading | content | attention | blocked | verification | error | retry | dark | large text |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Home | Healthy Brief | 同 first use | N/A（当前同步投影） | 六段 | 需要你处理段 | N/A | N/A | 错误文案（人话） | 重试 | system dark | Dynamic Type 不裁剪 |
| Findings | EmptyState | 同 first use | N/A | Finding 卡 | must_change | N/A | N/A | 同上 | 重试 | dark | 同上 |
| Scenario | 两分类 | 空分类 | N/A | 场景卡 | N/A | N/A | N/A | 同上 | 重试 | dark | 同上 |
| Scenario Flow | EmptyState | 无对象 | N/A | 8 步向导 | 当前步 | 停用旧号 blocked+原因 | verifying | 同上 | 重试 | dark | 同上 |
| ChangePlan/Verify | EmptyState | 无动作 | N/A | 步骤+验证 | failed | 同上 | verified>completed | 同上 | 重试 | dark | 同上 |
| Infrastructure | EmptyState | 同 first use | N/A | 分段+列表 | 选中 | N/A | N/A | 同上 | 重试 | dark | 同上 |
| Timeline | EmptyState | 同 first use | N/A | 桶分组 | attention | N/A | N/A | 同上 | 重试 | dark | 同上 |
| Backup | N/A | 无文件说明 | 导出中 | 导出/恢复 | N/A | N/A | N/A | 失败人话 | 重试 | dark | 同上 |

## Harmony（ArkTS/ArkUI）

| Page | first use | empty | loading | content | attention | blocked | verification | error | retry | dark | large text |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Index（本轮） | Briefing 骨架 | Healthy 文案 | N/A（同步 probe） | 六 section + 系统自检 | 需要你处理段 | 停用旧号 blocked 占位 | verifying 占位 | 自检失败行（人话） | 重试占位 | token dark（若系统支持） | 可滚动不裁剪 |

## 备注

- Desktop loading：当前为同步内存态，无异步加载；后续若引入 IO 读，统一用 LoadingState 骨架。
- Error 一律 ErrorStrip/ErrorState + 人话，不暴露 stack/内部类名（spec §16、§90）。
- Dark：各平台走 token dark scheme（design-tokens dark 值）；large text 走系统缩放，约束=不裁剪 CTA/关键语义（spec §72）。