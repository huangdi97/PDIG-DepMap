# DESIGN_SYSTEM.md — PDIG 设计系统 v1

> 本文件是 UI 的**唯一视觉真相源**；`app/theme/tokens.uts` 是其可执行形式，
> `core/scripts/check-ui.mjs` 依据它机械校验页面（非 token 颜色直接 FAIL）。
>
> 本设计系统为 PDIG 自有，**不复制**任何参考产品的品牌、颜色、图标、排版与页面结构。
> 仅吸收通用产品模式：场景化入口、卡片信息层级、Upcoming 列表。

---

## 1. 设计原则

| 原则 | 含义 |
|---|---|
| **Answer-oriented** | 界面回答"我需要做什么"，不是展示数据库。 |
| **不恐吓** | 用"需要处理 / 建议确认 / 可能受影响"，不用"危险 / 高危 / 严重风险"。 |
| **不假装确定** | 不用分数、百分比、"安全"字样。依据等级只说"我们了解多少"。 |
| **一个边界** | UI 只经 Application Service 取数；不直连数据库、不复算领域规则。 |
| **文字优先** | 状态永远有文字，不单独依赖颜色。 |

---

## 2. 颜色（Design Tokens）

来源：`app/theme/tokens.uts` → `DP_COLORS`。

| Token | 值 | 用途 |
|---|---|---|
| `primary` | `#4C4FD8` | 主操作、选中态 |
| `primaryActive` | `#3B3EB8` | 按下态 |
| `primarySoft` | `#EEEEFB` | 主色浅底 |
| `background` | `#F5F6FA` | 页面底色 |
| `surface` | `#FFFFFF` | 卡片 |
| `surfaceElevated` | `#FFFFFF` | 浮层 |
| `textPrimary` | `#1B1D29` | 主文本 |
| `textSecondary` | `#5A5F73` | 次文本 |
| `textTertiary` | `#8A90A6` | 辅助文本 |
| `border` | `#E6E8F0` | 分隔线 / 描边 |
| `success` | `#2BA471` | 可以继续 / 已验证 |
| `warning` | `#D98E04` | 需要确认 / 需要重新检查 |
| `danger` | `#D54941` | 必须处理 / 破坏性操作 |
| `info` | `#3B6FD8` | 提示 |
| `disabled` | `#B9BDCC` | 不可用 |

状态浅底（`statusSoftColor`）：`#FCEDEC` / `#FCF4E3` / `#E8F6F0` / `#EAF1FC` / `#F0F1F5`。

**规则**：页面 `<style>` 中出现的任何 hex 颜色必须属于上述集合（含浅底），否则 `check:ui` FAIL。

---

## 3. 深色模式

`DP_DARK_OVERRIDES` 已定义完整的深色取值。当前 App 默认 light；启用时只需在启动阶段按系统主题选择对应表，**页面代码零改动**。

> 决策：不在本轮为深色模式延迟 RC（§46）。Token 层已就绪，UI 层无需返工。

---

## 4. 字体与层级

系统字体，中文不做远程加载。

| 层级 | 字号 | 用途 |
|---|---|---|
| Display | 26px | 品牌标题（解锁页） |
| H1 | 22px | 页面主标题 |
| H2 | 18px | 次级标题 |
| Section | 16px | 卡片标题 |
| Body | 15px | 正文 / 列表项 |
| Secondary | 13px | 说明文字 |
| Caption | 12px | 状态标签 |
| Button | 15px | 按钮 |

---

## 5. 间距与圆角

间距 scale：`4 / 8 / 12 / 16 / 20 / 24 / 32`
圆角：`sm 6` · `md 10`（卡片、按钮）· `lg 14`（浮层）· `pill 999`（状态标签）

卡片统一：`margin 12` · `padding 16` · `radius 10`。
触控最小高度：**44px**（`DP_TOUCH_MIN`）。

---

## 6. 组件库

位置：`app/components/<name>/<name>.uvue`（uni-app x easycom）。

| 组件 | 职责 |
|---|---|
| `dp-card` | 信息分组容器（title / subtitle / flat） |
| `dp-row` | 列表行（title / description / value / valueTone / arrow） |
| `dp-chip` | 状态标签（语义色 + 中文标签，文字与颜色同时表达） |
| `dp-button` | 按钮（primary / secondary / danger；disabled / loading 防重复提交） |
| `dp-state` | 空态 / 错误态 / 加载态（empty / error / loading + 可选行动） |

**禁止**：页面自建卡片/按钮样式；同一视觉重复实现。

---

## 7. 图标

当前**不引入图标资源**（无远程 CDN，避免 license 风险）。
可用字符：`›`（进入）、`✓`（已完成）、`○`（未完成）、`·`（列表）。

> 待办：正式图标集与 tabBar 图标见 `docs/APP_ICON_ASSET_SPEC.md`。
> 一旦引入，必须统一风格（线性 / 填充二选一），不得混用 emoji 与图标。

---

## 8. 动效

只做轻量：页面进入、卡片反馈、状态变化、loading。
不使用炫技动画、3D、高耗电背景。平台支持时尊重 reduced motion。

---

## 9. 中文文案映射（§58）

| 内部概念 | 用户可见文案 |
|---|---|
| ChangePlan | 变更计划 |
| RealityDrift | 可能发生了变化 |
| ScenarioCoverage | 本次分析依据 |
| DiscoveryCandidate | 待确认服务 |
| Verification | 验证 |
| `needs_revalidation` | 需要重新检查 |
| `blocked` | 还有必须处理的事项 |
| `review_required` | 还有信息需要确认 |
| `ready_with_known_scope` | 基于当前信息，可以继续 |
| `criticality = unknown` | 需要确认 |
| `criticality = required` | 必须使用 |
| `funding_source` | 资金来源 |
| `merchant_agreement` | 自动扣款 / 订阅 |

**禁止**在用户界面出现：`ChangePlan` `ScenarioCoverage` `RealityDrift` `GraphRevision`
`DiscoveryCandidate` `PlanReadiness` `must_change` `confidence` `UUID` 等（`check:ui` 强制）。

---

## 10. 状态表达（§33）

依据等级只输出四级，且必须可解释：

| 等级 | 文案 | 含义 |
|---|---|---|
| `unknown` | 还不了解 | 没有任何来源，也没有已确认依赖 |
| `limited` | 依据有限 | 来源过期，或没有已确认直接依赖 |
| `partial` | 部分依据 | 有未决信号（待确认 / 未解析 / 关键度未确认） |
| `well_evidenced` | 依据较充分 | 无未决信号 |

**禁止**：分数、"94% 安全"、"all clear"、"完全安全"。

---

## 11. 可访问性基线

- 触控区 ≥ 44×44
- 状态不单独依赖颜色（`dp-chip` 始终带文字）
- 错误信息必须用户可见（禁止只用 `console.error`）
- 关键异步有 loading / disabled，防止重复提交
- 破坏性操作必须确认；不可恢复操作二次确认
- 长中文文本允许换行，不截断关键信息

---

## 12. 验收方式（无编译器条件下的机械校验）

`npm run check:ui` 执行 9 类校验：

| 编号 | 校验 |
|---|---|
| U1 | `pages.json` 中每个 page 都有对应 `.uvue` |
| U2 | tabBar 完整（2–5 项、pagePath 有效） |
| U3 | 用户可见文案无工程词 |
| U4 | 页面不直接操作 SQLite |
| U5 | 样式颜色必须来自 token |
| U6 | 无未声明标识符 |
| U7 | `dp-*` 组件必须存在 |
| U8 | 数据驱动页面必须有空/错/加载态 |
| U9 | 模板中 `v-for` 别名访问的属性必须在本文件已声明的 interface 中（捕获字段名写错导致渲染为空） |

当前实测：**30 `.uvue` / 24 pages / 5 components / token 34 色 / 0 命中 → PASS**。

> U9 已用「注入探针」验证有效：临时写入一个引用未声明字段（`dep.peerName`）的页面 → 门禁正确报
> `FAIL U9 ... 访问了未声明字段: peerName`；探针已移除。
>
> 该 Gate 不能替代真实编译。`UI_BUILD_READY` 仍为 **BLOCKED（B10）**。
