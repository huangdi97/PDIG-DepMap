# Android UI vNext R8：参考图逐屏翻译、真实像素及发布事实（2026-10-08）

> 判定规则：**SOURCE PRESENT != BUILD PASS != SCREEN OPENED != PIXEL ACCEPTED != REAL DEVICE ACCEPTED**。
> 用户首次安装的真实截图已推翻历史上的 `ANDROID_VISUAL_REFERENCE=ACCEPTED` 断言。
> 不得从旧 Round5 58 张图或 GitHub Release success 推出今日用户视觉体验合格。
> Source of truth: current branch `feat/android-ui-vnext-translation`, current CI HEAD,
> full-resolution user-provided nine-panel **PDIG 数字基础设施产品展示板**。
> 现存 `spec/ui-vnext/references/android/PDIG_ANDROID_LIGHT_VISUAL_REFERENCE_2026-10-05.jpg`
> 为缩略参考；它不能代替高分辨率逐屏比对。

## 约束与正确性

- 首屏浅色（白 / 冷蓝 / 浅灰），金融卡片有对象身份，号码独立为通信身份。
- Globe 是真正空间上下文：地区与连线取现有 fixture / PersonalReality，无关联不画关联。
- 用户选中某个地区进入基础设施概览；地区不会凭空成为安全关系。
- PresentationProfile 不写入 `.depmap` / Canonical / PersonalReality。
- Change: Current / Transition / After，After 始终是 Plan Projection，未知不自动判安全。
- Android 手机与平板采用不同密度 / 交互模式，不做桌面暗色版的像素复制。
- Preview 可从独立 SHA 应用标识识别版本；不同 SHA 的临时签名隔离安装，
  不可把旧 `com.pdig.app.preview` 当作本次 `com.pdig.app.preview.p<sha7>`。

## 原始设计板的八个实际画面 + 手机上必须实现的对应交互

| 原始参考板 | 路由/入口 | 主要形态验收 | 状态 |
|---|---|---|---|
| 1 手机·现在 | 四 tab / Now | 自然地球与地区标签占主视觉，标题无遮挡，底部四资产统计不压住 Globe，优先任务突出 | R8 source updated; pixel **PENDING** |
| 2 手机·基础设施总览 | Infrastructure / Overview | 4x2 彩色小图标 + 搜索 + 地区分布；不做巨型 Material 管理台 | R7 source updated; pixel **PENDING** |
| 3 手机·卡片列表 | Infrastructure / Cards | issuer 不同真实材质、卡片缩略图与状态；地区/种类筛选可用 | R7 source updated; pixel **PENDING** |
| 4 手机·卡片详情 | Cards / Card Detail | 全尺寸卡面、状态、概览/关联/账单/风险切换与服务身份，不以未导入账单虚构金额 | R8 source updated; pixel **PENDING** |
| 6 手机·更换手机号步骤1 | Change / Change Phone | 当前/迁移中/完成后区别明显，号码中心 + 已记录服务环绕，六步进度真实 | R7 source updated; pixel **PENDING** |
| 7 平板/桌面·现在 | adaptive layout | 大面积世界舞台与资产数据对齐，保留平板语义与主导航 | Historical source only; fresh medium/tablet regression **PENDING** |
| 8 桌面·基础设施卡片 | expanded Card workspace | 密集展示物件卡面，不把 Desktop Dark Reference 翻为 Phone | Historical source only; regression **PENDING** |
| 9 桌面·更换手机号 | expanded Change workspace | 旧号码→服务→新号码并列，未来计划不能呈现为完成事实 | Historical source only; regression **PENDING** |

## 为什么过去频繁出现「源码改了，安装却没变化」

1. 单看 GitHub Release 或 assemble success，无法证明真实屏幕。
2. 先前 CI 的截图脚本曾在根页面未成功切换时把首页误标为 Change。
3. 地球存在 async 纹理渲染阶段，深色占位图可能在 screenshot 窗口冒充成品。
4. GitHub 临时 debug 签名更换后，相同包名无法可靠覆盖安装；用户会打开旧 app。

因此在 R8 CI 中：

- 新版本采用 SHA 短码独立 applicationId + 可见应用名称，host-side checks 校验。
- `BuildConfig.GIT_SHA` 显示于 Preview 根屏幕；捕获 PNG 与 UI XML 必须回显同一 SHA。
- `01-now` 和 `02-infrastructure` 必须在真实 `TEXTURE_READY` 状态取证。
- 真正进入 Cards、CardDetail、ChangePhone、Records，未命中 UI 文本则 **FAIL**。
- Card Detail 的服务/账单/风险三个标签与 Change 的 After Projection 均应实际点击。
- 运行 Pixel Proof 是机器取证，不意味着与原始设计图的**人工视觉接受**。

## 需要逐张人工观察的 P0 问题（2026-10-08 用户反馈）

1. **Globe Hero 清晰边界**：旧版 1.61 倍放大导致大地球压标题、香港/新加坡标签重叠、指标压图。
   R8 将容器拆为固定 header / region stage / metrics 三层；标签定位在 body 内；
   等待实际运行图确认。
2. **卡片 Detail 服务**：旧详情的关联服务为 plain text，R8 通过现有 service 名称
   派生独立 monogram 小卡；不绘制虚构 logo、余额、风险状态。
3. **跨设备**：要在 393×~851 dp phone 与现有平板证据视口检查行间距、截断、
   刘海/状态栏、底部导航与字体放大。
4. **品牌视觉差距**：参考板内金融机构品牌色、卡面网络、空间浅蓝光照细节仍需真实
   PNG 判断。人工未接受前一律 HOLD。

## 严格验收状态

```ini
ANDROID_SOURCE_CODE_COMMITTED = TRUE
ANDROID_VISUAL_REFERENCE_DESIGN = TARGET_SELECTED
ANDROID_REAL_PIXEL_HUMAN_REVIEW = PENDING
ANDROID_REAL_PHONE_ACCEPTANCE = PENDING
ANDROID_REFERENCE_FREEZE = HOLD
ANDROID_V23_CANONICAL_ARCHITECTURE = DESIGN_ONLY_UNLESS_GATED
```

## 评审和发布流程

1. 读取 GitHub 分支 HEAD，确认已涵盖具体 UI 修改。
2. 查 `Android UI vNext Preview APK` 是否对 exact HEAD 成功，核验 prerelease asset。
3. 查 `Android UI vNext Phone Pixel Proof` 同一 SHA 是否 success。
4. 下载工件 `android-phone-actual-pixels-<sha>`，人工逐屏比对全分辨率九屏参考；
   对有严重差距的页面继续修改并重跑。截屏可见但未人工检查绝不称 PASS。
5. 使用真实 Android 手机安装 `PDIG Preview <sha7>`，反馈交互/截断/字体/颜色；
   反馈关闭前 `ANDROID_REFERENCE_FREEZE` 不提升。
6. 当前 Preview 是 synthetic-reference UI，不代表正式账户、账单、服务接入已落地。

### 参考的 Android 工程原则

- Compose 主流布局与自定义布局： https://developer.android.com/develop/ui/compose/layouts
- 安卓官方建议以 golden 比对验证 Compose 视觉： https://developer.android.com/training/testing/ui-tests/screenshot
- R8 的图片是真正 emulator screencap，不是仅 source-level preview。
