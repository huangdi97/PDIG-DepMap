# PDIG Android R46 — 参考设计 ↔ 真实运行 ↔ Production Reality 对照审计

> 日期：2026-10-10  
> 适用分支：`feat/android-ui-vnext-translation`  
> 产品规范：PDIG v2.3-R1；Android 五入口决定优先于母版内旧四入口快照  
> 实际截图基线：`4a9669ef6024a763075b98a1cc1446f4bfb3b7f5` · GitHub Actions API36 Pixel 7 · **不是当前最新 HEAD 的验收截图**  
> 人工参考图：用户提供的三张设计展板；仓库参考 `spec/ui-vnext/references/android/PDIG_ANDROID_LIGHT_VISUAL_REFERENCE_2026-10-05.jpg`  
> **状态：SOURCE REVIEW / MECHANICAL PARTIAL PROOF / HUMAN VISUAL ACCEPTANCE HOLD**

## A. 设计目标和不可变边界

```text
Personal Digital Infrastructure & Continuity
Inventory → Typed Dependency → Continuity Analysis → Change Orchestration
现在 / 基础设施 / 变更 / 记录 / 我
```

- Android 亮色优先；第三张深色展板是另一种材质与桌面参考，不是 Android 切暗的授权。
- 首页是 *Living Spatial Context*；地球纹理、大气、昼夜、城市灯光、真实摄像机投影、地区标签构成一个空间舞台。地理标签连接线不等于 dependency edge。
- 卡片表达 *Financial Asset Identity*，号码表达 *Communication Identity*。外观与本机别名从不代替经治理 Reality。
- Current / Transition / After 必须分开；After 是 Plan Projection。完成 != 验证，恢复用途 != 唯一恢复路径，关系数量 != 独立恢复路径。
- 五个正式一级入口包括「我」，在 compact 底栏与 expanded rail 必须同时成立。
- 参考图只表达体验目标；不得将示例的 18/3/2、地区、银行或手机服务填入加密 Production。
- Production VNext 只能通过 AppContainer authoritative projection/action gateway；Production release 默认 Legacy + 双钥 cutover HOLD。

## B. 参考 → 真实截图检查矩阵

标记：**OBSERVED** = 已实际读取 GitHub Actions 生成的截图；**SOURCE** = 已核对实际 Compose 源码；**GATE** = 仍须从完整最新 HEAD 运行后人工复核。

| 页面 | 参考图意图 | 4a9669e 实际观察与源码 | 缺口/下一验收 |
| --- | --- | --- | --- |
| Phone Now | 球体占据视觉重心，真实地区悬浮，资产入口与待办连贯 | **OBSERVED** `01-now.png`：light sky + GPU Earth + China/Singapore camera-projected glass labels + four mini asset entry cards；5 根导航直见；Greeting 按设备时刻 | 真实展示已有；标题/工具与空间还不如参考融合，证据仅 4a，当前 HEAD 需重新截图 |
| Phone Infrastructure | 8 类对象入口、地区筛选、资产上下文 | **OBSERVED** `02-infrastructure.png`、`02a-infrastructure-selected-us.png`、`02b-infrastructure-global-restored.png`；存在地区 selection/reset | 参考是更明显的信息层次与区域分布视觉；需宽屏与空态检查 |
| Phone Cards | 不同发行方可识别的卡面图像、状态/账期/年费、筛选 | **OBSERVED** `03-cards.png`、`03a-card-expiry-filter.png`：已具真实视觉身份缩略卡、筛选/到期；**SOURCE** Production 2/3/4 列真实 payment asset gallery | 产品感依然弱于参考的高密度卡片摄影/材质；不得猜 network/type，生产生命周期仅 confirmed maintenance |
| Card Detail | 大卡身份 + 基本资料 + 关联服务 + Impact + 变更 | **OBSERVED** `04-card-detail.png`、`04b-card-services.png`、`04c-card-statements.png`、`04d-card-risk.png`、`04de-card-impact-facts.png`；真实手机滚动后确认 `已确认依赖` 和 `未确认关系` | 不能要求 heading/facts/CTA 同在一屏；实际已发现取证脚本错误并修正；后续必须跑到 Change Card |
| Card appearance | 仅本机小功能、可选图片，不膨胀成 Studio | **SOURCE** R10/R19 消费者卡面微操作，Production private image import | 需要 exact-head 图片选择后 list/detail 一致和权限安全测试 |
| Numbers/Number Detail | 通信身份、用户别名、保号、依赖、恢复未知区分 | **SOURCE** governed identity subtype/identifier + Profile presentation + maintenance；新 Production relation constellation 只使用确认的 edge | 手机截图尚未在已通过的 4a Pixel journey 覆盖；不得从手机号前缀推地区/运营商 |
| Change Phone/Card | 当前→过渡→完成后，依赖对象、操作进度、阻断事项 | **SOURCE** ChangePlan authority + Current/Transition/After read-only summary；实际 Preview Phone journey 仍未全流程走完 | 需要 exact-head 动作、验证、变更前后画面与 Android 截图；After 仍非 Reality |
| Records | 可追溯的完成/验证/证据 | **SOURCE** authoritative Production records projection；区分 done/verified | 新截图与生产 secure rehearsal 未完成 |
| Me | 正式第五一级入口，个人数字生活而非 Settings | **OBSERVED** `01a-me-from-primary-nav.png` 确证通过底栏点击进入；**SOURCE** medium/expanded adaptive Me | compact/medium/expanded 信息密度、面板滚动及隐私状态需 runtime 人工确认 |
| Wide tablet/desktop | 平板复用 GPU globe，但布局是独立响应式组织 | **SOURCE** 3 个 breakpoint，R19AdaptiveWorldScene GPU family | R46 exact-head API36 tablet pixel + Human Acceptance 未完成；不可拿手机像素证明平板 |
| Production | 用真实加密 Reality，未知不渲染为 0/免费/安全 | **SOURCE** RegionFact, governed identity, MaintenanceProfile, Impact/Findings + object constellation | 加密环境真实 write/readback/lock gate 仍须 runtime；不能以 fixture 测试模拟发布许可 |

## C. 机械证据，不是审美判定

`4a9669e` 手机上的真实 API36 像素与运行日志：

- 地球图元状态：`R15_GPU_TEXTURES_READY`；`R16_GEO_HERO_GEOMETRY=PASS`；`R16_GEO_LABEL_ORBIT=PASS`，标签跟随实际摄像机而非固定屏幕坐标。
- 光照探针：中心区域平均亮度 **213.1**，低亮像素占比 **0.0008**。这只能排除明显黑球回归，不等于光照/大气/材质已达到参考图。
- 手机上的 RegionTag 预算上限 4；title/asset rail 未发生覆盖的机械检查已通过。
- 4a Phone Pixel 作业后续未通过，是卡片变更入口的真实滚动/点击取证缺口；不能从上述先前阶段 PASS 推断完整手机 journey PASS。
- GitHub CI/Preview APK 在 `4a9669e` 成功；下一轮更高 HEAD 必须重新执行，不得复用旧 SHA 的通过状态。

## D. R46 本轮工程完成与尚未获准

源码进展：

1. 手机与 Production 卡面紧凑排版，防止 2 列时大字号覆盖发行方/尾号。
2. 一致的 R15 GPU Earth、R16 投影几何扩大；仅改变空间表现，不改变已确认地区事实。
3. 正式详情对象关系星座：只连 confirmed Dependency，未知对象不凭 cached name 生造，UI 支持局部遮蔽；原始关系列表仍全部保留。
4. 原 Instrumentation case 对新增重复可见对象名和新的 RegionFact 用语进行了回归改造；保留 UI 隐私状态、不削弱真实业务约束。
5. 真正 API36 Phone Pixel 脚本拆分可见 heading/facts/CTA；Tablet/Production emulator-runner 改为单 Bash 进程，避免 action 按行调用 `sh` 造成换目录/重定向失效。

**严格状态：**

```text
REFERENCE_DESIGN             = HUMAN PROVIDED / INTERPRETED
SOURCE_IMPLEMENTED_R46       = YES (SUBJECT TO EXACT-HEAD CI)
CI_PASS                      = EXACT HEAD ONLY
RUNTIME_VERIFIED             = NOT YET ON ALL SCREENS
HUMAN_VISUAL_ACCEPTED        = HOLD
ANDROID_REFERENCE_FREEZE     = HOLD
PRODUCTION_CUTOVER           = HOLD
```

## E. 应按以下顺序闭环，不得跳 Gate

1. CI：精确当前 HEAD 的 core/canonical/Harmony/iOS/Android App JVM/Preview APK。
2. Phone：API36 全行程、Globe 拖动缩放复位、5 一级入口、卡片筛选、Detail/Impact、Change、Me，强制截失败现场。
3. Tablet：正式响应式断点、Now/Overview/卡面/Number/Change/Me；测试必须锁定具体 `testTag`，不可把地球与地区列表两处「中国大陆」当唯一文案。
4. Production：真实 LockGate，AppContainer/SQLCipher 的 write/readback，保号完成与下一 occurrence，Change done/verify；无交互断言的合成 fixture 不能冒充这一步。
5. Human：逐屏审查 Reference → Current Screenshot，包括亮色主次层级、资产 collection、地球真实大气与光照、品牌卡面、变更 choreography，出显式 ACCEPT/REJECT。
6. Human ACCEPT 后 Android Reference Freeze；**只有安全双钥、回滚方案和所有确证 Gates 齐备**才讨论正式 Cutover。

该报告是当前迭代的 SOURCE/EVIDENCE 事实索引，不是上架批准书。
