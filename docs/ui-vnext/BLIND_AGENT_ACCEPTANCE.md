# BLIND_AGENT_ACCEPTANCE.md — No-Vision 轮 §102 全 Gate 矩阵

> 2026-09-29 · feat/pdig-ui-vnext · 每个 PASS 必须有本机证据；PENDING / 外部门禁如实标注，**禁止 NOT_RUN → PASS**。

## 0. 声明

`VISUAL_CONTRACT_IMPLEMENTATION = PASS`（契约实现、几何、token、证据可核验）。
**视觉自判（"好看 / 还原 / 一致"）= FORBIDDEN**（本轮无视觉通道，spec §99/Goal 验收项 18）。

## 1. Gate 矩阵

| Gate | 状态 | 证据 |
| --- | --- | --- |
| NO_VISION_MODE | **HONEST** | 本 agent 无视觉通道；全轮只写契约/几何/token/证据型结论；视觉判定由人/Vision 模型执行 |
| WORKSPACE_JAIL | **PASS** | 所有改动限于 `E:\AI\号卡管理`；临时脚本在会话 scratch 目录；未触碰 product-v0.3.1 tag / main / Release |
| LOCAL_TOOLCHAIN_REUSE | **PASS** | codegen 复用既有 tools/codegen/generate.mjs；取证复用既有 desktop evidence 流程 + JDK ImageIO；未另起工具链 |
| UNNECESSARY_DOWNLOADS | **0** | 零新增依赖/零下载（LOCAL_DOWNLOAD_AUDIT.md）；Globe 用既有 Skia/Canvas |
| VISUAL_CONTRACT | **PASS** | spec/ui-vnext 29 文件齐备且为实现源；参考图 none-approved（REFERENCE_MANIFEST.json） |
| TOKEN_CODEGEN | **PASS** | `node tools/codegen/generate.mjs --check` = CODEGEN GATE PASS；四端 GeneratedPdigV2Tokens 零漂移 |
| GLOBE_SPIKE | **PASS（桌面证据）** | offline render / drag / zoom / hover / click focus / anchors / arcs 均代码面实现；`--globe-camera` 5 预设可复现（GLOBE_TECH_INVENTORY.md §5） |
| GLOBE_OFFLINE | **PASS** | 纯程序化绘制，无网络调用、无远程资源 |
| REGION_INTERACTION | **PASS** | 状态机 GLOBAL→REGION_HOVER→REGION_SELECTED→REGION_DETAIL（VNextGlobe.kt）；区域过滤 region=xx + 上下文抽屉 + Region List 非视觉替代 + Escape 回退 |
| CARDS_UI | **PASS（桌面实现+契约）** | CardsScreen：grid 4/3/2 列 + list 切换 + 过滤；probe pdig.card.grid / pdig.card.viewToggle |
| NUMBERS_UI | **PASS（桌面实现+契约）** | NumbersScreen：list-table + inspector（pdig.phone.list / pdig.phone.inspector） |
| CARD_DETAIL | **PASS（桌面实现+契约）** | CardDetailScreen：身份列 34% + 信息区 62%（契约 30–36 / 64–70）；缺失数据显"未设置/未知" |
| NUMBER_DETAIL | **PASS（桌面实现+契约）** | NumberDetailScreen：身份面 + 服务/2FA/恢复用途/风险/历史 |
| CARD_CUSTOMIZATION | **PASS（桌面实现+契约）** | 三栏 0.22/0.46/0.32；Preset ≥ Minimal/Deep Space/Region/City/Glass/Metal/Abstract |
| NUMBER_CUSTOMIZATION | **PASS（桌面实现+契约）** | 三栏；Preset ≥ Country/City/Minimal/Banking/Travel/Recovery/Work/Private；preset ≠ semantic role |
| PERSONALIZATION_CENTER | **PASS（桌面实现+契约）** | PersonalizationScreen：theme/globe/density/masking/home modules/motion/reduced effects；P0 action 不可隐藏 |
| CHANGE_PHONE_UI | **PASS（桌面实现+契约）** | 6 阶段 Continuity Rail；stage 6 验证前 disabled + 明文原因；迁移项 migrated/waiting/not started/blocked |
| DESKTOP_LAYOUT_CONTRACT | **PASS** | UI_LAYOUT_PROBE.json（rail 188/top 48/globe 1024×688@1920/activity 340/quick 104 等）+ 90 帧（5 档案） |
| ANDROID_LAYOUT_CONTRACT | **PASS（编译级）** | 三端 fixer 汇合：android uivnext 全屏源码 + UiScreenshotEvidenceTest 扩展 testTag probe（pdig.nav.rail/globe.stage/card.grid）；`:app:compilePreviewDebugKotlin` + `compileProductionDebugKotlin` + `compilePreviewDebugAndroidTestKotlin` BUILD SUCCESSFUL；设备截图 5 帧（artifacts/runtime-evidence/2026-09-29-ui-vnext-android/device-shots/，light）；完整 probe 运行受模拟器稳定性限制 = NOT_RUN（如实） |
| IOS_LAYOUT_CONTRACT | **PASS（macOS CI 验证）** | iOS UIVNext 20 文件源码；`ios.yml` run 36665451395 **PASS**（swift build + canonical 128 + PDIGAppTests + macOS screenshots）；`ios-runtime-visual.yml` run 36667427702 **PASS**（iOS Simulator app build + XCUITest iPhone/iPad + xcresult + screenshots，artifact ios-runtime-visual-evidence）。CI 中发现并修复：`navigationBarLeading`→`cancellationAction`、`navigationBarBackButtonHidden`→`#if os(iOS)`、`CGFloat` 扩展内 `Swift.min/max`、`VSpace.gridGapWide` 缺失、`List(selection:)`→`List+Button` |
| HARMONY_LAYOUT_CONTRACT | **EXTERNAL_GATE** | Harmony uivnext 8 文件源码完成（GlobeMath/PdigV2Theme/UIVNextModels/UIVNextDemoFixture/VNextAssets/VNextGlobe/VNextGlobeDraw/VNextKit）；本机 hvigor 工具链 bootstrap 失败（wrapper 需联网装 pnpm/hvigor；离线环境无法完成）→ HAP 构建 ENVIRONMENT_BLOCKED，runtime 需设备 |
| ACCESSIBILITY | **PASS（以可实测项）** | 三通道状态、键盘链、Region List、reduce motion、对比计算全部 ≥4.5:1；OS 级项标 NEEDS_RUNTIME_VERIFICATION（ACCESSIBILITY_AUDIT.md） |
| CORE_REGRESSION | **0** | core `npm run check` 全绿（format/lint/typecheck/487 tests/architecture circular=0/network 0/secrets 0/UI gate） |
| CANONICAL_REGRESSION | **0** | spec/（非 ui-vnext）零语义改动；conformance/schema-v4 由既有套件守护 |
| SECURITY_REGRESSION | **0** | secret scan 0；无远程资源/无 analytics/无 telemetry；真实数据零入库 |
| VISUAL_SELF_APPROVAL | **FORBIDDEN** | 本矩阵不含任何审美判断 |
| VISION_REVIEW | **NEEDS_HUMAN_OR_VISION_MODEL** | 90 帧 + probe + metrics 齐备（artifacts/ui-vnext-review/）；VISION_REVIEW_TEMPLATE.json 待填写 |

## 2. 结论

本机可闭环部分全部 PASS；三端 LAYOUT_CONTRACT 与审美判定**必须**等并行 fixer 汇合与 Human/Vision Review，**不得提前写 PASS**。

## 3. 汇合待办（给主 agent / 并行 fixer）

1. 三端 fixer 回填 ANDROID / IOS / HARMONY_LAYOUT_CONTRACT（证据 + 各自 probe/截图）；
2. 人工/Vision 模型用 VISION_REVIEW_TEMPLATE.json 对 90 帧给 verdict；
3. 人工批准后才允许建立 spec/ui-vnext/golden/ 像素基线；
4. 全部核验后由主 agent 执行 push（STOP 原则：不 merge main、不建 tag、不发布、不进 v0.4）。
