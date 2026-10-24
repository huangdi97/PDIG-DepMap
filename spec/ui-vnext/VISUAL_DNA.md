# VISUAL_DNA.md — PDIG UI vNext「Global Digital Infrastructure」

> 状态：**Human Direction 冻结 → Visual Contract**（No-Vision 模式，2026-09-29，feature branch `feat/pdig-ui-vnext`）
> 本文件是视觉方向的机器可执行转译源；实现以本文件 + `DESIGN_TOKENS.json` + `LAYOUT_CONTRACT.json` 为准，
> 不以任何参考图为实现指令（spec §16）。
>
> **Android R10/R19 translation override（2026-10-09）**：本文的 deep-navy / dark-spatial
> palette 继续作为 Desktop/跨端空间语义参考，但 Android consumer UI 已由人工方向明确为
> **light-first**。Android 不得把本文 L0 深空画布机械复制成全局暗色主题；应使用浅色 Canvas /
> 白色数据 Surface / sky-ice 空间背景，并仅在 Globe 本体、卡片身份面等需要深度的局部保留暗色。
> 五个一级目的地固定为「现在 / 基础设施 / 变更 / 记录 / 我」。详见
> `spec/ui-vnext/r10/CONSUMER_PRODUCT_DIRECTION_2026-10-08.md`、
> `spec/ui-vnext/FIVE_PRIMARY_NAVIGATION_DECISION.md` 与 Android mapping。

## 1. 定位一句话

**Personal Digital Infrastructure · Change & Continuity Management** —— 用户先管理「全球卡片 + 全球手机号」。
最强入口 = 全球卡片、全球号码；世界地图（Globe）是 Global Infrastructure Navigator，不是装饰。

## 2. Visual Direction（冻结）

`global · immersive · precise · trustworthy · premium · futuristic · spatial · financial-grade · communication-grade · calm`

## 3. 明确不是（Anti-DNA）

- 普通 SaaS dashboard / 传统 admin panel / 纯白表格软件
- cyberpunk hacker / 赌场 neon / 大面积随机渐变 / 廉价游戏 HUD
- 所有内容都是 card（沿用 v0.3.1 Card Policy：只有独立可操作对象才是卡）
- Graph 可视化作为主界面（Answer-oriented，CANONICAL_DESIGN §9.1）
- 玻璃效果覆盖数据可读性

## 4. 视觉层（L0–L6）

| 层 | 内容 | 允许 | 禁止 |
| --- | --- | --- | --- |
| L0 Environment | 画布 / 深空背景 | canvas `#061225`、canvasDeep `#030A18`、大气辉光 | 纯黑、噪点扫描线 |
| L1 Globe / Spatial Stage | 交互地球、锚点、弧线 | 程序化 2.5D 球体、深度着色、offline 纹理/海岸线 | 静态地球图、远程 tiles、装饰性弧线 |
| L2 Navigation / Control Chrome | 导航轨、顶部指令条 | glass、blur、空间感 | 闪烁、高饱和渐变 |
| L3 Data Surfaces | 面板、表格、列表 | 稳定高可读、border 分离 | 玻璃模糊于正文之下、卡片套卡片 |
| L4 Asset Identity | 卡面、号码身份面 | 品牌色 accent、沉浸卡面（可定制） | 远程图片、霓虹描边 |
| L5 Status / Actions | 状态徽标、CTA | icon+label+color 三通道、主CTA primary | 纯色状态、隐藏 disabled 原因 |
| L6 Transient Interaction | 抽屉、聚焦动画、气泡 | 短动画、reduce-motion 尊重 | 装饰性循环动画、滚动劫持 |

**总规则**：L0–L2 可以有空间感/glass/blur；L3–L5 必须清楚、稳定、高可读。

## 5. 色彩 DNA

- 基调：**deep navy**（不是纯黑），canvas `#061225` → surface `#0B1A33` → raised `#102340`
- Accent：primary `#4D74FF`（Indigo-Blue），primaryBright `#67A7FF`（发光/高亮），绝不铺满
- 语义色严格三通道：positive / warning / critical / unknown
- Status 只用语义 token；页面其它颜色只用 palette/语义 token（沿用 v0.3.1 colorRule）

## 6. 形态 DNA

- 数据优先：表格 / 列表 / 分割线 / 行 > 卡片；卡片只给「可以独立操作的对象」（卡片、号码身份面、资产）
- 空间层级靠 border 分离 + 分层底色，不靠阴影堆叠
- 圆角：sm 6 / md 10 / lg 14 / xl 18；不全民 pill
- 桌面密度：列表行 40–48、紧凑可密度、master-detail；移动 48+ 触控

## 7. 签名元素

1. **Interactive Globe**（Global Infrastructure Navigator）：地区资产分布 → 点地区聚焦 → 区域资产 → 变更理解
2. **Asset Card / Number Face**：视觉身份 = 可定制卡面/号码面（Presentation Layer）
3. **Continuity Rail**（沿用 v0.3.1 旗舰语义）：换号/换卡流程的结构化表达，make-before-break 闸门
4. **Privacy Mask**：全局干扰项遮蔽（last4/号码/账户名），用于截图/演示/公共场所

## 8. 动效 DNA

- fast 140ms / normal 200ms / globe focus 620ms / idle rotation ≤1°/s
- 用户交互后暂停 idle；Reduce Motion = 全静态
- 有意义、可中断、原生、短

## 9. 诚实表达

- 无数据地区 = 不显示/显示 0；**不伪造 UK 等节点充数**
- confidence 不是事实；proposal 不是确认；unknown ≠ required
- 不出现安全分；healthy 表达「检查范围 + 未知范围」而非「0 issues」

## 10. 来源（Human Design → Token 映射）

| 设计意图 | Token | 说明 |
| --- | --- | --- |
| 深空画布 | colors.canvas / canvasDeep | L0 |
| glass chrome | colors.surfaceGlass | L2 |
| 品牌 accent | colors.primary / primaryBright | CTA、选中、地区锚点亮 |
| 数据可读 | surface / surfaceRaised / textPrimary / textSecondary / textMuted | L3–L5 |
| 状态 | positive / warning / critical / unknown | 三通道 |
| 弧线 | arcActive / arcQuiet | 只有真实关系才显示 |