# ANDROID_UI_VNEXT_CURRENT_REALITY.md

> A0 Reality Audit — Android UI vNext 翻译前的现状审计（只读，不改代码）。
> 依据：`ANDROID_UI_VNEXT_TRANSLATION` 任务书 §36；HEAD = `39ef755b4c3160c1d5481ecc271d7cff81c90190`（branch `feat/pdig-ui-vnext`，与远端一致）。
> 状态分类：IMPLEMENTED / PARTIAL / PLACEHOLDER / LEGACY / NOT_CONNECTED / NOT_IMPLEMENTED。

## 1. 总述

Android 端并非空白工程：`android/app/src/main/kotlin/com/pdig/uivnext/**` 已存在一套完整的演示壳
（VNextApp 入口、VAppState 状态机、VNextShell 自适应壳、VNextNavKit 导航、11 个屏幕文件、Globe
程序化渲染、Demo Fixture、PdigV2Theme 全 token 映射），并在 `MainActivity` 中以 `vnext_demo` debug
intent extra 并行接入（不经过锁门；壳内无真实数据）。

但该实现与冻结 Desktop 参考之间仍有明确差距，且若干任务书硬性要求（Variant Truth、Current/
Transition/After、空态、Search/Command、tablet 证据、纹理地球）尚未覆盖。逐项如下。

## 2. 逐项状态

### 2.1 Screens

| Screen | 状态 | 说明 |
| --- | --- | --- |
| Now | IMPLEMENTED | Globe hero + 需要处理/进行中/即将到来；WIDE 三列，COMPACT 纵向堆叠。缺空态（无 attention / 无变更 / 无即将到来）。 |
| Infrastructure Overview | IMPLEMENTED | Globe Stage + 活动轨（Region List 非视觉替代 + 需要处理）+ 快速入口。缺 region-selected 空态。 |
| Cards | IMPLEMENTED | Grid/List 切换、地区过滤、AssetCard 卡面系统。缺 Cards Empty 空态（过滤后为空时无语义空态）。 |
| Card Detail | IMPLEMENTED | 大屏两栏（Identity 33% + Info 67%），手机纵向堆叠。语义完整（未知 != 安全）。 |
| Card Studio (Customization) | IMPLEMENTED | Preview-first；大屏三栏 / 手机纵向；PresentationProfile 编辑，保存仅本地 flag。 |
| Numbers | IMPLEMENTED | List + Inspector 两栏（宽）/ 堆叠（窄）；dial code/carrier/role/recovery 可见。 |
| Number Detail | IMPLEMENTED | NumberFace + 关联服务 + 登录/2FA/恢复 + 风险 + 历史。 |
| Number Studio | PARTIAL | 结构存在，但 **NumberFace 为普通 Surface**，不含 communication identity 语言（signal bars / dial arc），且 preset 不改变面视觉 → 无法满足 variant truth（country != travel）。 |
| Change Phone | PARTIAL | 单投影（transition 形态）；ContinuityRail + 旧→服务→新 + make-before-break 闸门。**缺 Current / After 两种投影**；After = Plan Projection 的显式"计划投影 ≠ 现实"标记部分存在但未按投影切换。 |
| Records（记录） | PLACEHOLDER | 路由到 PlaceholderScreen；无变更历史屏。 |
| Accounts / Emails / Devices / Services / Weaknesses（二级） | PLACEHOLDER | 导航入口完整（rail/chips），但 5 个二级屏全部落入 PlaceholderScreen。 |
| Personalization | IMPLEMENTED | 本地偏好管理；P0 提示（必处理模块不可隐藏）。 |
| Search / Command | NOT_IMPLEMENTED | TopCommandBar 只有"搜索 / 命令 + Ctrl K"视觉提示，**不可点击**，无搜索屏/命令面。 |

### 2.2 Navigation / Shell

| 项 | 状态 | 说明 |
| --- | --- | --- |
| Adaptive shell | IMPLEMENTED | BoxWithConstraints：≥600dp → NavigationRail + TopCommandBar；<600dp → TopCommandBar + InfraChipRow + BottomNav。 |
| 一级导航 | IMPLEMENTED | 现在/基础设施/变更/记录，BottomNav（手机）+ Rail（大屏）。 |
| 基础设施二级 | IMPLEMENTED（导航） | 总览/卡片/号码/账户/邮箱/设备/服务/薄弱点：rail 内嵌小节 + 手机 chip 行。 |
| Window Size Class | PARTIAL | 枚举为 COMPACT/MEDIUM/WIDE；任务书要求命名 COMPACT/MEDIUM/**EXPANDED**，且 EXPANDED = NavigationRail + list-detail。需改名对齐 + 细化。 |
| Back 导航 | NOT_IMPLEMENTED | 无 BackHandler / back stack；system back 在详情/Studio/搜索屏会直接退出 app。 |
| State restoration | NOT_IMPLEMENTED | 旋转/Activity 重建后状态不恢复；未定义恢复策略文档。 |

### 2.3 State / Fixture / Data binding

| 项 | 状态 | 说明 |
| --- | --- | --- |
| VAppState | IMPLEMENTED | screen / regionFilter / privacyMask / reduceMotion / railExpanded / globe controller。 |
| Change projection | NOT_IMPLEMENTED | 无 current/transition/after 三态字段与渲染。 |
| Demo fixture | IMPLEMENTED | `UiVNextDemoFixture`（synthetic，port 自 spec/UIVNextDemoFixture.json）：regions/cards/numbers/services/relations/attention/changes/stages/migrations。 |
| Repository binding | NOT_CONNECTED | 演示壳不触碰 domain repos（任务书允许 Reference Fixture Mode；生产数据绑定 = EXISTING 于 com.pdig.app，vNext 壳未连接）。 |
| PresentationProfile 持久化 | IMPLEMENTED（边界正确） | 仅本地演示 flag，不写 .depmap；未接真实 preferences（本轮保持，不引入持久化以隔离证据 fixture）。 |

### 2.4 Globe

| 项 | 状态 | 说明 |
| --- | --- | --- |
| 交互 | IMPLEMENTED | drag 旋转 / tap 选区 / tap 二次打开抽屉 / scroll 缩放 / hover；触控不依赖 hover。 |
| 语义 | IMPLEMENTED | 真实跨区关系弧线（非装饰）、region anchor、REGION_SELECTED/REGION_DETAIL 状态机、Region List 非视觉替代。 |
| 视觉 | PARTIAL | **程序化渐变球体 + 经纬网格**（VectorEarthFallback 水平）。冻结 Desktop 参考 = **bundled 真实地球纹理**（earth_albedo_2048 / earth_night_lights_2048 / cloud_2048，均已在 `spec/ui-vnext/assets/` 提交）。按任务书 §11，"generic gradient sphere" 属于禁止项 → 需移植纹理地球渲染。 |
| 性能 | PARTIAL | 未测；纹理逐像素渲染需缓存 + 质量档位（HIGH/BALANCED/LOW）。 |

### 2.5 Tests / Evidence

| 项 | 状态 | 说明 |
| --- | --- | --- |
| 既有 screenshot tests | IMPLEMENTED | `UiScreenshotEvidenceTest`：production 21 屏×2 主题 + vNext 10 手机屏 + 2 wide + testTag geometry probe（captureToImage → MediaStore Downloads）。 |
| Variant Truth (Android) | NOT_IMPLEMENTED | 无 Android 版 VisualVariantEvidenceContract（glass!=city、country!=travel、current!=transition!=after、global!=region-selected、SHA 互异、expected==actual）。 |
| 14 张 Human Main Set | NOT_IMPLEMENTED | 现有 10 屏不含 studio-glass/studio-city/change-current/change-after/cards-empty/search 等。 |
| Tablet 证据 | NOT_IMPLEMENTED | 本机 AVD 只有手机（main=API36 Pixel7 1080×2400；zhishen_rc=API35 同 profile）；无 tablet AVD（已获用户批准：用本机 android-36 系统镜像新建 1 个 tablet AVD）。 |
| Screenshot manifest | NOT_IMPLEMENTED | 无 ANDROID_UI_VNEXT_SCREENSHOT_MANIFEST.json（device/api/viewport/density/orientation/expectedState/actualState/stateValidation/sha256/commit）。 |
| Accessibility 测试（vNext） | NOT_IMPLEMENTED | 既有 AccessibilitySemanticsTest 覆盖 production app；vNext 壳无专项语义测试。 |

### 2.6 Quality gates / 文档

| 项 | 状态 | 说明 |
| --- | --- | --- |
| Desktop Freeze Guard | NOT_IMPLEMENTED | `tools/freeze/hash-evidence.mjs` 是通用取证哈希，不是 12-SHA 冻结守卫；需建立 DESKTOP_REFERENCE_FREEZE_GUARD。 |
| 翻译文档 | NOT_IMPLEMENTED | ANDROID_REFERENCE_MAPPING.md / ANDROID_VISUAL_CONTRACT.md / 翻译报告 / screenshots doc 均不存在。 |
| 空态 | NOT_IMPLEMENTED | 无 EmptyState 组件（semantic illustration + title + description + CTA；honest unknown 语义）。 |

## 3. 结论（Gap → 行动）

| Desktop frozen feature | Existing Android | Gap | Required translation action |
| --- | --- | --- | --- |
| 真实地球纹理 Globe | 程序化渐变球 | 视觉身份不符（禁止 generic sphere） | 移植 TextureEarthRenderer 到 Android（bundled 资产 + 缓存 + 质量档） |
| Primary/Secondary IA | 完整导航 | MEDIUM/EXPANDED 命名与 list-detail 细化 | 改名 EXPANDED；壳细化 |
| Card Studio glass/city 等变体 | 卡面 brush 已按 preset 差异 | 无 variant truth 证据 | Android 版 VisualVariantEvidenceContractTest + 证据参数注入 |
| Number Studio 变体 | NumberFace 不随 preset 变化 | 视觉身份 + variant truth 缺失 | NumberFace communication identity（signal bars / dial arc / preset 背景） |
| Continuity 三态 | 只有 transition | current/after 缺失 | changeProjection 三态 + After 计划投影语义 |
| 空态 6 类 | 无 | 空态全缺 | EmptyState 组件 + 6 类接线 + 证据模式 |
| Command Palette | 不可点击提示 | 搜索/命令缺失 | Search/Command 屏 + 触控入口 |
| Back / 状态恢复 | 无 | back 直接退 app | BackHandler + 轻量 back stack + 恢复策略 |
| 手机+平板 runtime 证据 | 仅手机 10 屏 | 缺 14 屏主集 + tablet + manifest | 证据测试 + 新建 tablet AVD + manifest 生成 |
| Desktop 冻结守卫 | 无 12-SHA 守卫 | 缺自动 gate | guard 脚本 |

## 4. 未改动项声明

- 本轮 A0 只读：除本文件外未改任何代码。
- 预存在未跟踪文件 `artifacts/runtime-evidence/2026-09-29-ui-vnext/IMAGE_METRICS.json` 保持原样，不纳入本轮 commit。
- Domain / Canonical 零漂移目标不变；上述行动全部落在 presentation 层。
