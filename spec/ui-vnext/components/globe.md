# Globe — 2.5D 程序化交互球体（Global Infrastructure Navigator）

> Visual Contract · spec/ui-vnext · 2026-09-29 · No-Vision 模式（实现以此契约为准，非参考图）

## 1. 职责与位置

- **职责**：签名元素「Interactive Globe」（VISUAL_DNA.md §7.1）——地区资产分布 → 点地区聚焦 → 区域资产 → 变更理解的导航入口（LAYER L1 Globe / Spatial Stage）。它是 Global Infrastructure Navigator，**不是装饰**（VISUAL_DNA.md §1）。
- **使用位置**：Now 页 `globe-context` 块（testId `pdig.now.globe`，LAYOUT_CONTRACT.json `now.testIds.globe`）与 Infrastructure Overview 主舞台（testId `pdig.globe.stage`）。绘制画布 = `pdig.globe.canvas`，填满 stage。
- **非唯一导航**：Globe 只是导航增强（IA.md §7）；必须存在 Region List 等非视觉替代（INTERACTION_CONTRACT.md §4），不允许「只能点地球才能到香港资产」。
- **领域边界**：只渲染呈现层聚合数据（RegionPresentation），不读取 / 不修改任何 node identity / dependency / evidence / confirmation。
- **渲染路线**：程序化 2.5D（各端原生 Canvas/Skia/SwiftUI Canvas/ArkUI Canvas），零新增 3D 依赖、offline-first（goal §8 §43）。

## 2. 数据契约

| 输入 | 键名 | 来源 | 契约 |
| --- | --- | --- | --- |
| 地区锚点 | `RegionPresentation.regionCode / displayName / latitude / longitude / cardCount / phoneCount / accountCount / serviceCount / attentionCount` | IA.md §5 | 值只由现有真实数据计算；无数据 = 不显示 / 0（IA.md §5、VISUAL_DNA.md §9） |
| 支持地区 | `CN / HK / MO / GB / US / SG` | IA.md §5 | 未列地区不渲染锚点，**禁止伪造节点充数** |
| 弧线策略 | `DESIGN_TOKENS.json globe.arcCountPolicy` | DESIGN_TOKENS.json | `"only real cross-region relations or region infrastructure summary; never decorative"` |
| 相机预置 | `DESIGN_TOKENS.json globe.cameraPresets = ["global", "cn", "hk", "gb", "us"]` | DESIGN_TOKENS.json | 支持 `--globe-camera=global|cn|hk|gb|us` 确定性复现 |

- 弧线数据源仅限：真实跨区依赖关系（region→region 的依赖连边）或该地区的资产摘要（数量聚合）。**禁止装饰性连线**（VISUAL_DNA.md §4 L1 禁止行、goal §43）。
- **建议文案**（不修改 copy-zh.json）：tooltip 由计数拼装，格式参考 INTERACTION_CONTRACT.md §4 示例「香港，2 张卡，1 个号码」；新增 copy key：`uiVNext.region.tooltip = "{displayName}，{cardCount} 张卡，{phoneCount} 个号码"`；名词复用 copy-zh.json `identity.phoneNumber`（手机号）。

## 3. 几何

引用 LAYOUT_CONTRACT.json `overview1920.globeStage`（DESIGN_TOKENS.json `components.overview` 同值）：

| 项 | 键名 | 数值 |
| --- | --- | --- |
| Stage 宽度占比 | `overview1920.globeStage.widthRatio` | `{min:0.55, max:0.65}` |
| Stage 高度占比 | `overview1920.globeStage.heightRatio` | `{min:0.65, max:0.78}` |
| Stage 最小尺寸 | `overview1920.globeStage.minWidth / minHeight` | `860 × 650` |
| Stage testId | `overview1920.globeStage.testId` | `pdig.globe.stage` |
| 球体直径下限 | `DESIGN_TOKENS.json globe.preferredSphereDiameterMin` | `520` |
| 缩放范围 | `globe.minZoom / globe.maxZoom` | `0.7 – 1.9` |
| 锚点直径 | `globe.regionAnchorDiameter` | `14` |
| 锚点 active 直径 | `globe.regionAnchorActiveDiameter` | `22` |
| 触控热区 | `components.touchTarget.android / .ios` | `48 / 44`（热区透明扩大，视觉直径不变） |

## 4. 视觉

只允许以下 token（值来自 DESIGN_TOKENS.json，禁止出现任何新颜色值）：

| 元素 | token 键 | token 值 |
| --- | --- | --- |
| 深空画布 | `colors.canvas` | `#061225` |
| 深空最深 | `colors.canvasDeep` | `#030A18` |
| 球体深度着色 | `colors.canvas → colors.surface → colors.canvasDeep` 程序化渐变（L0） | 禁照片纹理 |
| 大气辉光 | `colors.atmosphereInner → colors.atmosphereOuter` | `rgba(77,116,255,0.18) → rgba(6,18,37,0.0)` |
| 经纬网格 | `colors.borderSubtle` | `rgba(148,180,234,0.13)` |
| 锚点 hover/active | `colors.regionNodeHi` | `#67A7FF` |
| 锚点 quiet | `colors.regionNodeLo` | `rgba(103,167,255,0.35)` |
| 弧线 active | `colors.arcActive` | `rgba(103,167,255,0.55)` |
| 弧线 quiet | `colors.arcQuiet` | `rgba(115,131,163,0.28)` |
| 地区标签 | `typography.displayGlobe`（size 15 / weight 600 / lineHeight 1.3） | DESIGN_TOKENS.json |
| 聚焦 ring | `semantic.focus = colors.primaryBright` | `#67A7FF` |
| Tooltip 面板 | `semantic.data.panelRaised` + `semantic.data.divider` | `#102340` + `rgba(148,180,234,0.13)` |

- **资源禁令**：无远程 tiles / texture / terrain / CDN / 静态地球图片；只允许 bundled world 纹理 / 简化海岸线 / 本地区域坐标集 / 程序化大气（goal §43、VISUAL_DNA.md §4 L1）。

## 5. 交互/状态

状态机（INTERACTION_CONTRACT.md §1）：

```
GLOBAL ──hover(region)──▶ REGION_HOVER
REGION_HOVER ──click──▶ REGION_SELECTED
REGION_SELECTED ──click(selected)/双击──▶ REGION_DETAIL（Region Drawer 展开）
REGION_DETAIL ──Escape/关闭──▶ REGION_SELECTED
REGION_SELECTED / REGION_DETAIL ──Escape──▶ GLOBAL
任何状态 ──click(empty space)──▶ GLOBAL
```

输入映射（INTERACTION_CONTRACT.md §2）：

| 输入 | 行为 |
| --- | --- |
| drag | 旋转球体（水平=经度，垂直=纬度，纬度 clamp ±60°）；交互期间暂停 idle rotation |
| scroll / pinch | 缩放（clamp 0.7–1.9） |
| hover 锚点 | → REGION_HOVER：高亮该 node + tooltip（name + counts） |
| click 锚点 | → REGION_SELECTED：focus/flyTo（620ms ease）+ 该锚点 active + 其它锚点降强调 + 区域过滤 `region=xx` + 上下文抽屉出现 |
| click 已选锚点 / 双击 | → REGION_DETAIL：全屏 Region Drawer（Cards / Numbers / Accounts / Services + 查看全部 / 查看卡片 / 查看号码） |
| Escape | 回 Global（所有状态，每按一次回退一级） |
| click 空白 | 回 Global |

- 区域聚焦副作用（INTERACTION_CONTRACT.md §3，5 条全量）：1) camera/focus 移向地区坐标；2) 锚点 active（大、亮、标签常显）；3) 其它锚点降强调（`regionNodeLo`）；4) 基础设施查询过滤 `region = 该地区`；5) 上下文抽屉出现。**不能只 `navigate("/hongkong")`**——必须保持 globe 舞台 + 区域上下文。
- Motion：`globeFocusMs 620`（MOTION_CONTRACT.json `tokens.globeFocusMs`）；idle rotation = `globe.idleRotationDegPerSec 0.8`（DESIGN_TOKENS.json）、`globe.idleRotationPeriodMs 90000`，用户交互后暂停（MOTION_CONTRACT.json `globe.idleRotation`）；禁止 fast continuous spin（MOTION_CONTRACT.json `globe.forbidden`）。
- arc 动画：segment-draw 620ms（MOTION_CONTRACT.json `globe.arcAnimation`）；reduce motion 下 static。
- 截图确定性：按 MOTION_CONTRACT.json rules「screenshots freeze globe animation or set deterministic camera state」，配合 5 个相机预置视角。

## 6. 无障碍与隐私

- **非视觉替代（Region List）**：INTERACTION_CONTRACT.md §4——Globe 旁/下，键盘可达、screen reader 可读，行文案形如「香港，2 张卡，1 个号码」；与锚点等价驱动 region filter 与 drawer。testId `pdig.region.list` / `pdig.region.item`。
- **键盘**（INTERACTION_CONTRACT.md §5）：Tab/Shift+Tab 焦点循环；Enter/Space 激活；Escape 回退；Arrow 微调视角；Ctrl/Cmd+K 打开命令条；焦点 ring = `semantic.focus`。
- **触控**：热区 ≥ `components.touchTarget`（Android 48 / iOS 44）。
- **隐私**：只渲染聚合计数与地区坐标，不渲染卡号/号码/账户名；Privacy Mask（DESIGN_TOKENS.json `privacyMask.maskCardLast4 / maskPhoneNumbers / maskAccountNames`，默认 true）开启时 tooltip 摘要不变，任何敏感明细一律遮蔽；日志不含真实对象标识（goal §44）。

## 7. 验收自检

- [ ] testId 存在：`pdig.globe.stage`、`pdig.globe.canvas`、`pdig.globe.region`（锚点）、`pdig.region.list` / `pdig.region.item`（非视觉替代）。
- [ ] 几何符合：stage 55–65% 宽 × 65–78% 高且 ≥860×650；球体直径 ≥520；zoom clamp 0.7–1.9；锚点 14/22。
- [ ] 状态机四级 + Escape 逐级回退完整可实现（§5 表逐条对照）。
- [ ] 无远程资源：无 tiles/texture/terrain/CDN；弧线只来自真实跨区关系或地区资产摘要。
- [ ] 无真实数据：只渲染聚合计数与地区坐标；mask 默认开。
- [ ] 非视觉替代存在且能驱动 filter/drawer。
- [ ] 文案来自 `spec/ui/copy-zh.json` 或标注「新增 copy key：uiVNext.*」。