# GLOBE_TECH_INVENTORY.md — 程序化 2.5D 交互球体技术盘点

> 2026-09-29 · feat/pdig-ui-vnext · Stage 0 盘点 + Stage 8 Globe Spike Gate 证据

## 1. 结论

- 四端（Desktop Compose / Android Compose / iOS SwiftUI / Harmony ArkUI）**均无内置 3D 引擎**；
  引入第三方 3D/地图框架违反 Goal 边界（不新增第三方 UI/3D 库，除非 inventory+spike 证明 native 不足）。
- 采用路线：**程序化 2.5D 正交投影球体** —— 各端原生 Canvas/Skia/SwiftUI Canvas/ArkUI Canvas 绘制，
  **零新增依赖、offline、无纹理、无远程 tiles**。
- Desktop 已完成 spike 并全量落地（见 §5）；Android/iOS/Harmony 由并行平台 fixer 按同一 spatial spec 落地。

## 2. 技术基础盘点

| 项 | 现状 | 结论 |
| --- | --- | --- |
| 仓库依赖清单 | 无 3D 引擎 / 无地图 SDK / 无 WebView 依赖（core+desktop+android+ios+harmony） | 零新增 3D 依赖成立 |
| Desktop 渲染栈 | Compose Desktop 1.6.11（Compose BOM 2024.09.02），Skia-backed Canvas | 用既有 `Canvas` + `drawCircle/drawLine/drawPath` 即可实现 2.5D |
| Android 渲染栈 | Compose `Canvas`（Skia） | 同一套矢量绘制原语 |
| iOS 渲染栈 | SwiftUI `Canvas`（Core Graphics） | 同一套绘制原语 |
| Harmony 渲染栈 | ArkUI `Canvas`（自绘） | 同一套绘制原语 |
| WebView / 本地 JS 包 | 未引入、不需要 | 无需渲染桥 |
| GPU 支持 | Compose 底层由平台合成器负责；离屏渲染 ImageComposeScene 可确定性出图 | 已实测（90 帧） |

## 3. RENDERER_LIMITATION（如实声明）

程序化 2.5D 球体**不提供**：

- 真实纹理 / 真实海岸线 / 卫星图；
- 立体光照模型（仅径向渐变深度着色）；
- 投影地图等价精度（经纬网格为示意性质）。

因此表达策略（已实现于 VNextGlobe.kt）：

1. **深度着色**：球体径向渐变（高光偏移 → surfaceRaised → canvasDeep），营造空间感；
2. **经纬网格（graticule）**：按深度淡出的前半球弧线（30° 平行线 + 子午线采样），给出"球面"几何提示；
3. **地区锚点**：CN/HK/GB/US 投影到前半球（zDepth>0），悬浮/选中放大 + 光环（regionNodeHi）；
4. **跨区弧线**：只来自**真实跨区关系**（funding/authenticates/twoFA 派生），绝不装饰性连线；
5. **大气辉光**：atmosphereInner/Outer 径向渐变，L0 环境层。

**弧线铁律**：`arcCountPolicy = "only real cross-region relations or region infrastructure summary; never decorative"`
（DESIGN_TOKENS.json globe.arcCountPolicy）。

## 4. 为什么不是其它路线（拒绝记录）

| 候选路线 | 拒绝原因 |
| --- | --- |
| 静态地球图片 | Goal 明令禁止（"禁止退化成静态地球图片"） |
| WebView + Three.js/Mapbox | 引入 WebView 运行时与远程依赖，违反 offline-first / 零新增依赖 |
| 第三方 3D 库（Filament/lwjgl 等） | license + 体积 + 四端不可共用，超出 MVP 需要 |
| embedding/LLM 地理解析 | AGENTS §22 禁令清单 |

## 5. Globe Spike Gate（桌面证据）

| 能力 | 契约 | 实现/证据 |
| --- | --- | --- |
| offline render | 无网络可渲染 | 纯程序化绘制，无任何网络调用；90 帧离屏渲染为证据 |
| drag 旋转 | 水平=经度、垂直=纬度（clamp ±60°） | detectDragGestures → camera.yawDeg/pitchDeg（VNextGlobe.kt） |
| scroll 缩放 | clamp 0.7–1.9 | PointerEventType.Scroll → zoom（0.7–1.9，与 token globe.maxZoom/minZoom 一致） |
| hover 锚点 | 高亮 + tooltip 数据（name+counts） | hoveredRegion → anchor 放大 + 光环 + REGION_HOVER 状态 |
| click 聚焦 | flyTo（620ms 语义）+ 锚点激活 + 区域过滤 + 上下文抽屉 | focusRegion() → REGION_SELECTED；区域过滤驱动卡片/号码列表 |
| 锚点/弧线 | 只来自真实数据与真实跨区关系 | RegionPresentation + arcingPairs（fixture 派生） |
| 可复现相机 | `--globe-camera=global\|cn\|hk\|gb\|us` 冻结视角 | VNextShotDriver cameraOverride；5 预设 × overview/now 各 5 帧 |
| 启动可接受 | 无重型加载 | 无纹理/无外部资源，首帧即出 |

结论：`GLOBE_SPIKE = PASS`（桌面），`GLOBE_OFFLINE = PASS`。

## 6. 引用契约与代码位置

- 契约：spec/ui-vnext/DESIGN_TOKENS.json `globe.*`、LAYOUT_CONTRACT.json `overview1920.globeStage`、INTERACTION_CONTRACT.md §1–§4
- 实现：desktop/app/src/main/kotlin/com/pdig/uivnext/globe/（VNextGlobe.kt 285 行、GlobeMath.kt）
- 证据：artifacts/runtime-evidence/2026-09-29-ui-vnext/profiles/**（5 相机 × 分辨率）
