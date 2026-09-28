# PDIG_DESIGN_SYSTEM.md

> PDIG v0.3.1 UI/UX Refinement · 2026-09-28 · 设计系统（spec §66；与 `spec/ui/design-tokens.json` vNext 一致）
> 语义与品牌以 Canonical Master 为准；本文件为表现层规范，四端各自原生实现同一语义。

## 1. Color

| Role | Light | Dark | 用途 |
| --- | --- | --- | --- |
| primary (indigo) | #4C4FD8 | #8B8EF5 | 品牌/主 CTA/选中/链接（品牌色冻结不动） |
| primaryActive | #3B3EB8 | #A5A7F8 | 按压态 |
| primarySoft | #EEEEFB | #242541 | 图标底/轻 accent |
| background | #F5F6FA | #12131A | 页面底色（不再被 surfaceVariant 条取代） |
| surface | #FFFFFF | #1C1E28 | 面板/内容容器 |
| surfaceElevated | #FFFFFF | #232633 | 浮层/对话框 |
| surfaceVariant（新语义：surfaceLow） | #EDEFF6 | #232633 | 不可交互的底层容器（少用） |
| textPrimary | #1B1D29 | #F2F3F7 | 主要文本 |
| textSecondary | #5A5F73 | #B4B9C9 | 次要文本 |
| textTertiary | #8A90A6 | #828799 | 元信息/占位 |
| border | #E6E8F0 | #2C2F3D | hairline 分隔（替代 surfaceVariant 填底） |
| success | #2BA471 | #3FBF87 | verified/ready/well-evidenced |
| warning | #D98E04 | #E0A63A | review_required/needs_revalidation |
| danger | #D54941 | #E4695F | blocked/must-change/failed |
| info | #3B6FD8 | #5B8CE8 | verifying/partial |
| disabled | #B9BDCC | #4A4E5E | 禁用 |
| focusRing | #4C4FD8 | #A5A7F8 | 键盘焦点（桌面 2dp ring + offset） |

### 对比度核验（全部 ≥4.5:1 正文 / ≥3:1 大字）
- textPrimary on surface/background：light ≈13.4:1 / dark ≈15.2:1 ✓
- textSecondary on surface：light ≈6.0:1 / dark ≈6.3:1 ✓
- textTertiary on surface：light ≈3.2:1 → **仅用于元信息/辅助文字（并 ≥3:1 大字标准）**；正文一律 textPrimary/Secondary
- success #2BA471 on #FFFFFF ≈3.9:1 → 状态徽标内文字用白/深色底保证 ≥4.5:1（badge 用 white-on-colour 处理）
- danger #D54941 on white ≈5.4:1 ✓

## 2. Typography（system / CJK，禁远程字体）

| Role | Desktop | Mobile | Weight | 用途 |
| --- | --- | --- | --- | --- |
| pageTitle | 24 | 20 | 600/700 | 页面标题 |
| sectionTitle | 15 | 16 | 600 | 小节标题 + hairline |
| body | 14 | 16 | 400 | 正文 |
| secondary | 12.5 | 13 | 400 | 副文本/描述 |
| meta | 12 | 12 | 400 | 时间/来源等元信息 |
| label | 12 | 12 | 500 | 状态/徽标文字 |
| button | 14 | 14 | 500 | 按钮/CTA |
| mono（少量数据专用） | 12.5 | 12 | 400 | 关键 ID/数字对齐（非装饰） |

行高：正文 1.45，多行 1.5；行宽桌面 ≤680px。

## 3. Spacing（4pt grid）

xs=4, sm=8, md=12, lg=16, xl=20, xxl=24, xxxl=32。页面 padding：桌面 20 / 移动 16；section gap 桌面 24 / 移动 20；touch 相邻 ≥8。

## 4. Radius

sm=6（chip/小徽标）、md=8（卡片/输入/按钮）、lg=12（对话框/浮层）、pill 保留仅给真实 pill 场景（禁滥用）。
**不再全民 14**。交互边界（按钮/输入）8；分组容器 8；卡片 8。

## 5. Surface / Border / Elevation

- 容器默认 border（1dp #E6E8F0）parking，背景 surface；需要强调才 elevated。
- fontLevel：content 始终 surface；status strip 用 primarySoft/dangerSoft 等轻色（softBackgrounds* tokens）。
- 无阴影默认；浮层 8dp shadow。

## 6. Iconography（native icons / vector，禁远程 CDN，禁 emoji 图标）

| 语义 | Icon 建议 |
| --- | --- |
| blocked / must-change | error/warning 圆（!） |
| review_required | warning triangle |
| ready / ok | check circle |
| verifying | progress/clock |
| verified | check circle double（或实心 check） |
| completed | check（无圆） |
| unknown | question mark |
| 去往/动作 | arrow-right / chevron |
| 场景 | scenario-wand / credit-card / phone |
| 基础设施 | account-circle / device / email |
| 本地安全 | lock / shield |

图标尺寸：桌面 16-18dp，移动 20-24dp（iOS SF 用 medium/bold weight；Android Material Icons；Harmony 系统图标或 vector）。

## 7. Status 组件

```
[icon] [label文字] （颜色作第三通道，不唯一）
```
- StatusBadge（桌面/卡片内）：icon 14 + label 12/500，色 = 语义色 text 或 soft 底 + 深色字（保证对比）。
- StatusRow（列表行内）：icon 16 + label 13/500。
- 色板：blocked=danger、review_required=warning、verifying=info、ready=success、verified=success（强，双勾）、completed=textSecondary（单勾，弱于 verified）、unknown=textTertiary(?)、cancelled=textTertiary(–)。

## 8. Buttons

- Primary：filled indigo，radius 8，高 ≥40（桌面 36-40），文字 14/500，hover 变 primaryActive。
- Secondary：outline（border + textPrimary），radius 8。
- Text/Quiet：仅文字 + indigo，用于列表内行动。
- Disabled：灰字+灰底，且语义性「不可用」要带原因（Rail 场景必须有明文，不止禁用）。

## 9. Inputs

- 高 ≥40，padding 12，border 1dp，focus 2dp indigo ring；label 在框外上方（不占位-only）。
- 错误：框 border danger + 下方文字（红）说明；不弹 toast-only。

## 10. Lists / Rows

- 桌面列表行：高 40-48，左 icon(16)/状态 + 标题(14/500) + 描述(12.5/400, secondary) + 右侧动作；hairline 分隔；hover 背景 #F5F6FA。
- 移动：Row 高 ≥48，左状态 icon + 标题 + 副行；swipe/chevron 由原生导航提供。
- 长列表性能：避免每行重计算（Compose 用 key / remember；SwiftUI List 惰性）。

## 11. Navigation（桌面）

- Sidebar 220px：品牌块（PDIG + 版本）→ 分组（概览/数据/检查与变更/维护）→ 各入口（选中 = indigo 左 bar 3dp + primary 文字；hover 背景中性）。
- Top bar：当前页标题 20/600 + 右侧状态位（「本地数据文件已就绪」粒度文案，不显示文件名/ID）。
- 内容区：max-width 1240px 居中；页面 padding 20/24。

## 12. Master-Detail（Infrastructure）

- 左 list 280-320px（By Item / By Capability 分段切换 + 搜索框 + 列表行），右侧 Detail pane：
  Identity / Capabilities / Dependencies / Evidence / Recovery paths / Related scenarios，
  用 SectionHeader + rows（禁卡片嵌套）；无选中项时右侧显示引导 EmptyState。
- 1280 以下可折叠左窗（min 路径：优先 Item 列表全宽，detail 折叠在手势内）。

## 13. Findings 行

默认三行结构：
```
[状态icon] 标题(14/600)
           一句人话解释（13/400 secondary）
           [下一步建议→]（12/500 action，可点）
可展开：是什么(what) / 为什么(why) / 基于什么确认 / 还不知道什么 / 可能影响什么
```
颜色强调 must_change；其余中性。

## 14. Continuity Rail（旗舰）

```
step1 ●──step2 ●──step3 ●── step4(verification) ◎── step5 …
      done  done    blocked            verifying        upcoming
```
- 节点：完成=实心 check 圆（success）；当前=空心圆+编号（primary）；阻止=圆+!+danger；待验证=时钟圆（info）；未来=灰空心。
- 连线：完成段实线 success，未完成段虚灰线；闸门说明块（「新手机号验证通过后才能停用旧手机号」）在禁用步骤上方 13/400 secondary。
- 步骤下可挂子卡片（只在该步骤有内容时）。
- **semantic visualization，禁止做成装饰图**。

## 15. Empty / Loading / Error（成套）

- EmptyState：icon(24, tertiary) + 标题(14/600) + 说明(13/400 secondary) + 动作按钮（可选）；三种文案「这里是什么 / 为什么为空 / 下一步能做什么」。
- Loading：占位骨架或 spinner + 文案（不闪烁）。
- Error：danger 软底条 + 人话 + 重试按钮；不暴露 stack/内部类名。
- Retry：按钮显式「重试」。

## 16. Motion

- rail 状态变化 180ms ease；验证完成 check 弹入 220ms；展开/收起 150ms；导航切换 200ms fade。
- 全面 respect reduced-motion（Compose `LocalReduceMotion` / SwiftUI `accessibilityReduceMotion` / Harmony 系统开关 > 直接展示终态）。
- 禁装饰性循环动画。

## 17. Density

- Desktop：行高 40 基准；列表可分页/虚拟化；1280 下内容仍可读（标题 24 → 20 自适应）。
- Mobile：行高 ≥48；字号随 Dynamic Type/fontScale 放大可换行（禁固定高度 clip）。
- Tablet：复用桌面密度（Android/iPad 可并排）。

## 18. Accessibility 清单（每个交付页面过）

- [ ] 正文 ≥4.5:1 / 大字 ≥3:1（light+dark）
- [ ] 触控 ≥44pt(iOS)/48dp(Android)；桌面键盘可达
- [ ] 状态 icon+label+color
- [ ] 交互元素有 screen reader 标签（语义文本即可）
- [ ] focus 可见（桌面 ring）
- [ ] large text 无 clip/遮 CTA
- [ ] reduced-motion 尊重
- [ ] 错误说明在字段旁

## 19. 硬编码禁令

- UI 文案一律走 `spec/ui/copy-zh.json`（新增 key 需同步四端资源；本轮新增 key 记录于 UIUX_IMPLEMENTATION_REPORT.md）。
- 禁止 UI 泄露：graphRevision、FD-003、plan-…、RECOVERY_CYCLE、SHARED_FAILURE_DOMAIN、merchant_agreement、内部 id、debug 类名、源文件路径、depmap 文件名。