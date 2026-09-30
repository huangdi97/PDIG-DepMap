# BLIND_AGENT_ACCEPTANCE.md — PHASE 1 Desktop 参考实现 Gate 矩阵

> 2026-10-01 · feat/pdig-ui-vnext · 依据 Human Visual Review（2026-09-30）。
> 每个 PASS 必须有本机证据；PENDING / 外部门禁如实标注，**禁止 NOT_RUN → PASS**。
> PHASE 1 = Desktop only；Android/iOS/Harmony 冻结至 Human/Vision 批准 Desktop 参考实现（Review §T）。

## 0. 声明

- Human Visual Review（2026-09-30）对旧工程原型判定：`VISUAL_DIRECTION = FAIL`、`VISUAL_CONTRACT_IMPLEMENTATION = NOT_ACCEPTED`。
- 旧截图 = BEFORE / REJECTED 基线，**保留不删**：`artifacts/runtime-evidence/2026-09-29-ui-vnext/`（`VISUAL_STATUS.json` = `REJECTED_ENGINEERING_PROTOTYPE`）。
- 本轮（PHASE 1）只重做 Desktop PRESENTATION / LAYOUT / SPATIAL UI；Domain / Core / 状态机 / 数据模型 / 测试 / 导航能力全部保留。
- 4 张参考图 = **HUMAN-APPROVED VISUAL TARGET**（`spec/ui-vnext/references/`，SHA256 见 `REFERENCE_MANIFEST.json`，humanApproved=true）；参考图 ≠ pixel golden，golden 须等 Human/Vision 批准生产截图后建立（Review §R）。
- 视觉自判（"好看 / 还原 / 一致"）= FORBIDDEN；`VISUAL_CRAFT` 永远 = `NEEDS_HUMAN_OR_VISION_REVIEW`（Review §V）。

## 1. Gate 矩阵（PHASE 1 Desktop）

| Gate | 状态 | 证据 |
| --- | --- | --- |
| NO_VISION_MODE | **HONEST** | 本 agent 无视觉通道；全轮只写契约/几何/token/证据型结论；视觉判定由人/Vision 模型执行 |
| HUMAN_REVIEW_INPUT | **PASS** | 4 张参考图已枚举 + SHA256 + 尺寸（1672×941）写入 `REFERENCE_MANIFEST.json`（humanApproved=true）；Review A–W 全部执行 |
| WORKSPACE_JAIL | **PASS** | 所有改动限于 `E:\AI\号卡管理`；临时脚本在会话 scratch 目录；未触碰 product-v0.3.1 tag / main / Release |
| UNNECESSARY_DOWNLOADS | **0** | 零新增依赖/零下载；Globe = bundled 简化海岸线 + 程序化城市灯光/大气/明暗（离线、无 tile CDN、无 Google/Mapbox、无 analytics） |
| TOKENS | **PASS** | `spec/ui-vnext/DESIGN_TOKENS.json` v2.1.0（放大字阶、earth tokens、glassRule、globe 52–64%×64–78%、primaryOnly nav）；`node tools/codegen/generate.mjs --check` = CODEGEN GATE PASS，四端 Generated 零漂移 |
| LAYOUT_CONTRACT | **PASS** | UI_LAYOUT_PROBE.json（nav rail / top bar / globe stage 60%w×70%h@probe / data panel）；100 帧（5 档案 × 20）指标全绿（0 error / 0 empty bbox / 0 near-black；meanLum 0.18 优于旧 0.08–0.13） |
| FUNCTIONAL_INTERACTION | **PASS** | 9 屏导航/过滤/选择/抽屉/Escape 回退/自定义编辑全部可交互代码保留；屏幕矩阵生成 100 帧无异常 |
| GLOBE_INTERACTION | **PASS** | 状态机 GLOBAL→REGION_HOVER→REGION_SELECTED→REGION_DETAIL；drag/zoom/hover/click focus/anchors/arcs；`--vnext-camera=global\|cn\|hk\|gb\|us` 5 预设复现；plain sphere 仅保留为 LOW_POWER_FALLBACK（非默认） |
| GLOBE_OFFLINE | **PASS** | 纯本地绘制：WorldCoastlines.kt（bundled 简化大陆轮廓）+ WorldCityLights.kt（~90 城市灯光）+ 程序化大气/明暗/星空；无网络调用 |
| NAV_PRIMARY_RAIL | **PASS** | 一级仅 现在/基础设施/变更/记录（+ 数据源/设置）；基础设施 8 个二级项进 context subnav/flyout，不与一级同权占满侧栏（Review §G） |
| CARDS_UI | **PASS（桌面实现+契约）** | CardFace.kt：1.586 比例；issuer/nickname/masked number/network/category 显式位；region/currency/type/status metadata 层；physical/virtual 克制区分；7 个不同视觉预设（非全蓝渐变）；PresentationProfile<Card> 驱动（material/theme/background/accent/layout/nickname/logo/network/last4/region/currency/privacy mask） |
| CARD_DETAIL | **PASS（桌面实现+契约）** | 身份列 + 信息区；缺失数据显"未设置/未知" |
| NUMBERS_UI | **PASS（桌面实现+契约）** | Numbers list 高密度 + inspector（Review §J 允许） |
| NUMBER_DETAIL | **PASS（桌面实现+契约）** | NUMBER IDENTITY FACE（region/flag 视觉/masked/nickname/carrier/SIM-eSIM/role/usage/recovery role/status）+ PresentationProfile<PhoneNumber> |
| CARD_CUSTOMIZATION | **PASS（桌面实现+契约）** | StudioFrame 三栏 22/46/32（对象库 / 大尺寸实时预览 / 分组编辑器：卡面设计·内容信息·样式·高级）；每次修改 live preview；保存 = PresentationProfile 本地偏好，绝不写 .depmap/PersonalReality |
| NUMBER_CUSTOMIZATION | **PASS（桌面实现+契约）** | 同上三栏；Preset Country/City/Minimal/Banking/Travel/Recovery/Work/Private；visual preset ≠ semantic role |
| OVERVIEW_SPATIAL | **PASS（桌面实现+契约）** | 先答"基础设施分布在哪"：region anchors（如 HK · 2 卡 · 1 号码）+ 计数 → 点击 focus/filter → 区域抽屉（Cards/Numbers/Accounts/Services/Attention + 查看全部/卡片/号码） |
| NOW_HOME | **PASS（桌面实现+契约）** | Globe Context + Need Attention（≤3–5 核心项）+ Active Change（更换手机号 2/6 · 下一步）+ Upcoming；无小型 KPI 网格 |
| CHANGE_PHONE | **PASS（桌面实现+契约）** | flagship scene：顶部 6-stage progress；中央 旧号→migration→新号；左 仍依赖旧号 / 右 已迁移·等待验证；状态 migrated/waiting/manual/blocked/not-started 同屏可辨 |
| TYPOGRAPHY | **PASS（token 级）** | v2.1.0 字阶：pageTitle 36、majorNumber 30、sectionTitle 20、body 16、secondary 14、meta 12（≤12 仅 metadata）；行高/呼吸/分组间距放大 |
| GLASS_RULE | **PASS（token/实现级）** | Glass 仅限 nav/floating controls/region node/transient inspector/overlay；关键数据区 solid 高对比 |
| SCREENSHOTS_GENERATED | **PASS** | 100 帧（`artifacts/runtime-evidence/2026-10-01-ui-vnext-phase1/`）：9 核心屏 + overview/now × 5 相机 + card/number customization before+customized 变体 + 5 分辨率档案；IMAGE_METRICS.json + EVIDENCE_SHA256SUMS.txt + UI_LAYOUT_PROBE.json；gallery（docs/ui-vnext/gallery/index.html + SCREENSHOTS.md）0 missing |
| VISUAL_CRAFT | **NEEDS_HUMAN_OR_VISION_REVIEW** | **永不自行改 PASS**（Review §V）。待 Human/Vision 对照 `spec/ui-vnext/references/` 给出 verdict |
| CORE_REGRESSION | **0** | core `npm run check` 全绿（487 tests / format / lint / typecheck / architecture circular=0 / network 0 / secrets 0） |
| CANONICAL_REGRESSION | **0** | spec/（非 ui-vnext）零语义改动；conformance 由既有套件守护 |
| SECURITY_REGRESSION | **0** | secret scan 0；无远程资源/无 analytics/无 telemetry；真实数据零入库 |
| VISUAL_SELF_APPROVAL | **FORBIDDEN** | 本矩阵不含任何审美判断 |

## 2. 非本轮范围（冻结，等待 Desktop 批准）

| 平台 | 状态 | 说明 |
| --- | --- | --- |
| ANDROID | **NOT_IN_PHASE_1（编译保留）** | 既有 compile 证据保留；vNext Android 视觉传播冻结至 PHASE 3（Review §T），不提前复制 Desktop 视觉 |
| IOS | **NOT_IN_PHASE_1（既有 CI PASS 保留）** | ios.yml run 36665451395 PASS、ios-runtime-visual run 36667427702 PASS（artifact 需登录）；视觉传播冻结至 PHASE 4 |
| HARMONY | **NOT_IN_PHASE_1（HAP 构建 PASS 保留）** | ASCII mirror HAP 3,758,625 B（sha256 E37A5A03…）；runtime 需真机（EXTERNAL_GATE）；视觉传播冻结至 PHASE 5 |

## 3. 结论

PHASE 1 Desktop 参考实现：契约/几何/token/交互/证据类 Gate 全部 PASS；
**VISUAL_CRAFT = NEEDS_HUMAN_OR_VISION_REVIEW** —— 由 Human/Vision 对照参考图给 verdict；
批准后才进入 PHASE 2（golden 建立）→ PHASE 3–5（Android/iOS/Harmony 传播）。

## 4. 待办（给 Human / Vision Reviewer）

1. 用本页截图（docs/ui-vnext/SCREENSHOTS.md 或 gallery/index.html）+ `spec/ui-vnext/references/`（4 PNG）对照评审；
2. 重点核对：Globe 大陆/海洋/夜间灯光/大气/明暗/锚点/弧线；9 屏空间层级；卡片资产身份；号码身份面；两个定制工作室前后对比；Change Phone 5 状态；
3. 批准后建立 `spec/ui-vnext/golden/` 像素基线（在此之前不存在 golden）；
4. 批准后才启动 PHASE 3–5；全程不 merge main、不建 tag、不发布、不进 v0.4。
