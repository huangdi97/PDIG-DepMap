# PLATFORM_UIUX_PARITY_MATRIX.md

> PDIG v0.3.1 UI/UX Refinement · 2026-09-28 · 平台体验一致性矩阵（spec §76）
> 检查维度：semantic parity / information parity / CTA parity / state parity / native adaptation。
> 不要求 pixel parity（spec §53）：品牌/信息架构/语义/术语/优先级/CTA 意义/状态意义/任务进度必须一致；组件形状/spacing/导航 chrome 不必一致。

图例：✅ 已落地（本轮实现） · ◐ 部分（有明确原因） · 🔒 EXTERNAL_GATE（本机无法运行，如实标记）

## 核心能力

| 能力                                                     | Desktop                      | Android                                | iOS                         | Harmony                 | 语义一致性说明                                                        |
| -------------------------------------------------------- | ---------------------------- | -------------------------------------- | --------------------------- | ----------------------- | --------------------------------------------------------------------- |
| Home = Personal Infrastructure Briefing（六段顺序）      | ✅ 六段 Briefing + healthy   | ✅ 六段 Briefing + healthy             | ◐ 六段 Briefing（同步投影） | ✅ Index 骨架六 section | 顺序：需要你处理→薄弱点→可能变化→即将到来→常用场景→我的基础设施       |
| Healthy 不显示 0 分/安全分                               | ✅                           | ✅                                     | ✅                          | ✅                      | 「当前没有需要立即处理的事项 + 检查范围 + 未知范围 + 可准备场景」     |
| Findings：what/why/next + 展开 evidence/unknown/affected | ✅ FindingCard               | ✅ Finding 卡                          | ◐ Finding 卡 + 六问         | ◐ 薄弱点 section 行     | must_change 用 icon+label+danger                                      |
| Infrastructure：By Item / By Capability 分段             | ✅ master-detail + segmented | ✅ 分段切换                            | ◐ segmented Picker（已有）  | ◐ 我的基础设施 section  | 分组文案统一「身份与恢复/访问与认证/支付/设备/关键服务」              |
| Scenario Center 按意图分组（支付/身份与恢复）            | ✅                           | ✅                                     | ✅                          | ◐ 常用场景 section      | 每场景回答 何时用/检查什么/大约步骤                                   |
| Replace Phone flagship：Continuity Rail 全链             | ✅                           | ✅ 步骤轨道                            | ◐ 步骤向导                  | ◐ 场景 section 占位     | 全链 9 步：选旧号→影响→恢复路径→共享故障点→建新→验证→迁移→再验证→停用 |
| Make-Before-Break：停用旧路径 = blocked + 明文原因       | ✅                           | ✅                                     | ◐                           | ◐                       | 「新手机号验证通过后才能停用旧手机号（先建立新路径，再移除旧路径）」  |
| Impact 四类分组（必须/需确认/较小/暂无）                 | ✅                           | ✅                                     | ◐                           | ◐                       | must_change 视觉突出；needs_review 是 warning 非 error                |
| ChangePlan = 步骤轨道（verified > completed）            | ✅ ContinuityRail            | ✅ 步骤轨道                            | ◐ 步骤+验证                 | ◐                       | verified=双勾 success 强；completed=单勾 secondary 弱                 |
| Verification：done ≠ verified                            | ✅                           | ✅                                     | ◐                           | ◐                       | 待验证/发现证据/已验证/验证失败 三态可辨，文字可读                    |
| 状态三通道（icon+label+color）                           | ✅ StatusBadge               | ✅ StatusChip（label+color，图标待补） | ◐ StatusBadge 新增          | ◐ 图标+文字+色          | 永不 color-only                                                       |
| EmptyState 三要素（是什么/为何空/下一步）                | ✅                           | ✅                                     | ◐                           | ◐                       | 成套 Empty/Loading/Error                                              |
| 错误人话（不暴露 stack/内部类名/原始 Error）             | ✅                           | ✅                                     | ◐（原 Error 上屏已修）      | ◐ 自检失败行            | 用户可理解 + 重试                                                     |
| 不泄漏技术标识（id/wire/文件名）                         | ✅（本轮 5 处已修）          | ◐（注释级仅存）                        | ◐（CopyZh 集中）            | ◐                       | plan-…/graphRevision/FD-003 等不上屏                                  |
| 版本号一致                                               | ✅ 0.3.1                     | ✅ 0.3.1                               | ◐ v0.3.1                    | —                       | Desktop Gate/About 已统一                                             |
| 键盘（Desktop）                                          | ✅ focusable + 导航顺序      | N/A                                    | N/A                         | N/A                     | Tab/Shift+Tab/Enter/Space/Escape                                      |
| 触控目标                                                 | Desktop ≥36-40               | ≥48dp                                  | ≥44pt                       | ≥44vp                   | 平台规范                                                              |
| light / dark                                             | ✅ token dark scheme         | ✅ token dark                          | ✅ system dark              | ◐ token 预留            | design-tokens dark 值                                                 |
| large text 不裁剪 CTA                                    | ✅                           | ✅                                     | ✅（Dynamic Type）          | ◐                       | 关键语义不隐藏                                                        |

## 原生适应（native adaptation，不要求 pixel 相同）

| 维度        | Desktop                            | Android                               | iOS                                          | Harmony             |
| ----------- | ---------------------------------- | ------------------------------------- | -------------------------------------------- | ------------------- |
| 导航 chrome | 分组 sidebar + top bar + max-width | AppBar + 返回栈（现有）               | RootView phase 分流 + 手写 route（本轮保留） | 单页 Index（本轮）  |
| 容器        | 卡片/列表行/分割线                 | Card/Row                              | Card（新增 PdigCard）                        | Row/List            |
| 交互        | hover/focus/键盘                   | touch ripple                          | SF 触摸                                      | 系统触摸            |
| 平台状态    | Windows DPAPI 解锁                 | LockGate/生物识别                     | LocalAuthentication                          | 系统认证（未接入）  |
| 运行验证    | ✅ 本机 ProfileDriver 80 帧 PASS   | ✅ AVD instrumentation + 截图（本轮） | 🔒 macOS CI（本机 Windows）                  | 🔒 无 DevEco/模拟器 |

## 结论

- 语义/信息/CTA/状态 parity：Desktop、Android 已达成；iOS 以新增 token 层 + 状态组件落地为主，运行验证走既有 macOS CI（IOS_RUNTIME_EXTERNAL_GATE）；Harmony 本轮完成 Index 首页工程面（HARMONY_RUNTIME_EXTERNAL_GATE）。
- 无 pixel parity 要求；各平台原生实现，品牌/术语/优先级/CTA 意义一致。
- 剩余外部门禁（真实，非代码缺陷）：iOS 构建/截图需 macOS CI；Harmony 需 DevEco/模拟器镜像；两者均按 spec §9/§89 如实记录。
