# PHASE_1F_IMPLEMENTATION_REPORT.md

> PDIG UI vNext — Desktop Final Craft, Reference Freeze & Acceptance Candidate（brief 2026-10-02，61 节）
> 执行方式：No-Vision 盲实现（契约/证据驱动）；§49 真实窗口运行时 smoke 已在本机执行。

## 状态

```
PHASE_1F_IMPLEMENTATION          = PASS
DESKTOP_REFERENCE_CANDIDATE_FINAL = READY
DESKTOP_VISUAL_REFERENCE         = NEEDS_HUMAN_FINAL_ACCEPTANCE（Human 裁决）
VISUAL_CRAFT                     = NEEDS_HUMAN_FINAL_ACCEPTANCE（盲实现不自判 PASS）
DOMAIN_REGRESSION                = 0
CANONICAL_REGRESSION             = 0
SECURITY_REGRESSION              = 0
ANDROID_UI_VNEXT                 = HOLD
IOS_UI_VNEXT                     = HOLD
HARMONY_UI_VNEXT                 = HOLD
```

## HEAD / branch / worktree

- branch: `feat/pdig-ui-vnext`（未新建第二个 UI branch）
- worktree: `E:\AI\号卡管理`（REPO_ROOT；临时证据仅 `artifacts/runtime-evidence/2026-10-02-ui-vnext-phase1f/`）

## 本轮 What changed（按 brief 主问题）

| 领域                   | 变更                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| ---------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| §6–11 Card Identity    | 新增 `CardIdentitySystem`（CardIdentity.kt 模型/解析 + CardIdentityDrawing.kt 绘制）：每张 demo 卡 = palette 色对 + glow + accent + motif（CITY_NIGHT / CONTOUR / BRUSHED / SWEEP / STARFIELD / COPPER_RING / RED_LINE / CORAL_BAND / CHROMATIC）。`resolveCardIdentity(profile, card)`：默认视觉 → §9 issuer 身份（CMB 暖深酒红+铜环 / ICBC 石墨+红线 / BOC 冷石墨拉丝 / HSBC 香港夜景 / BOCHK 轮廓 / Monzo 深炭+珊瑚带 / Revolut 玻璃色散 / Chase 拉丝海军蓝 / Capital One 午夜+红 sweep / DBS 新加坡夜景）；Studio 定制 → 主题身份（编辑可见）。identityElementCount 契约：每张 demo 卡 ≥3 个身份要素；无 near-black 空占位（GRAPHITE/CHARCOAL/MIDNIGHT 均提亮）。删除旧 CardArtwork 单一 artwork 层。 |
| §7/§8 最小契约         | `CardIdentityContractTest`：5 项（要素≥3 / §9 issuer motif 冻结 / 非近黑 / Studio 定制改变身份 / 8 主题缩略图非空 artwork）。                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| §12–13 Card Detail     | 下区改为平衡两栏工作区：LEFT 绑定服务/备用支付、RIGHT 影响与风险/变更历史；低数据 section 用紧凑空态（尚未确认绑定服务 / 尚未确认备用支付方式 / 尚未记录更多变更）；健康文案诚实（目前没有需要立即处理的已确认事项 + 仍可能存在尚未记录或确认的关系）。                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| §14–20 Card Studio     | 预览卡宽 660–740px（`Phase1FLayout.studioPreviewWidthPx`，fillMaxWidth(0.72).widthIn(660..740)）；舞台加微妙 vignette + 地板光（StudioKit.drawPreviewStage）；右 inspector 默认展开「材质」（卡）/「背景」（号）一个分组；自定义背景消费者流程：选择本地图片 / 替换 / 移除 + 缩略图（StudioInspector BackgroundThumbnail）；theme 缩略图 84→104dp；CARD_THEME_PRESETS 增加 matte。                                                                                                                                                                                                                                                                                                                        |
| §21–27 Number          | NumberIdentitySurface 改为通信语法：左上 region 标记+国家名、主身份大号区号 +86（36sp 最强元素）、号码 mono 24sp、运营商/SIM、角色簇（主号/银行验证/2FA/注册）、唯一恢复路径、小号 continuity ring（36dp）；确认非卡语法、非 1.586 比例。Number Studio 默认展开「背景」。                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| §28–36 Continuity      | 新布局：OLD/NEW 250–300px（0.155/0.845 位）、服务节点 130–170px（0.085W×0.30H 盒：glyph 圆+名称+关系+状态 pill+状态色边框）、可用高 430px（420–480）；路径层级 2.5/2/1.5px（§32，无更强辉光）；waiting=虚线 amber 框、blocked=红框+断点 X；After：old 0.25 alpha + ghost 路径、new 主导、未迁移保持可见未解决；投影徽标文案「计划完成后的预期状态 / 不代表已经完成或验证」；风险提示宽 72%（非全宽 banner）。几何常量抽到 `Phase1FLayout.continuityScene()`（probe/测试共用防漂移）。                                                                                                                                                                                                                     |
| §37–40 Empty/Healthy   | EmptyState 重建为紧凑组合（max 600px、语义剪影 motif CARD/NUMBER/REGION/CHECK/LIST、primary + secondary CTA）→ EmptyStates.kt；六个空态完成：Cards / Numbers / Region（选中地区无资产）/ No Active Change / No Attention / No Known Dependencies；文案契约化 `Phase1FEmptyCopy`（含禁词断言）。                                                                                                                                                                                                                                                                                                                                                                                                           |
| §44–46 Copy            | rail 品牌副标 vNext→个人数字基础设施；Personalization「深空 · 系统跟随」；窗口标题 PDIG Preview；Placeholder 文案去 vNext。                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| §47–48 A11y/Responsive | `Phase1FKeyboardContractTest`：4 项（空态/Studio/投影选择器所在屏键盘可达 + palette 中文检索/执行到 Change Phone）；VNextShell 启动即请求焦点（真实窗口 Ctrl+K 可达性增强，仍在 Human gate 复核）；mechanical 5 档渲染成功。                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
| §42/§49 真实运行时     | `VNextWindowSmoke1F`：真实 Compose Desktop 窗口启动 + 16 步交互旅程 + Robot 窗口级截图（14/16 capture=true，真实像素）；Ctrl+K 两步因本会话无法把 OS 输入焦点可靠授给窗口 → 如实记 `REAL_WINDOW_KEYBOARD_HUMAN_GATE`；中文 IME 组合输入 → `IME_RUNTIME_HUMAN_GATE`（不伪造 PASS）。in-process journey（17 步）+ 键盘证据已覆盖键盘功能。                                                                                                                                                                                                                                                                                                                                                                  |

## 测试证据

- `desktop :app:test`：**51 项全部 PASS**（含新增 CardIdentityContractTest 5 / Phase1FEmptyCopyContractTest 4 / Phase1FLayoutContractTest 4 / Phase1FKeyboardContractTest 4 + 既有 34 项）。
- Journey：`VNextJourney1F` 17 步 keyb=false → keyboard=true, failures=0；持久化（编辑→保存→重开保留 glass/emblem/imported ✓）、自定义背景导入 ✓、IME_LOG 如实记录。

## 证据（artifact）

- `artifacts/runtime-evidence/2026-10-02-ui-vnext-phase1f/`
  - `profiles/1920x1080@1.0/`：恰好 12 张 Human Review 主截图（§50，Privacy Mask ON；无 Globe 特写 / 无 Command Palette）
  - `mechanical/`：1280 / 2560 / 1.25 / 1.5 五档代表性屏幕 + `empty-states/` 4 帧
  - `journey/`：17 步 + custom-background.png；`INTERACTION_LOG.txt` / `KEYBOARD_LOG.txt` / `IME_LOG.txt`
  - `real-window/`：16 张真实窗口截图 + `WINDOW_SMOKE_LOG.txt`
  - `IMAGE_METRICS.json`：12 帧 meanLum 0.061–0.218，0 error / 0 empty / 0 near-black
  - `UI_LAYOUT_PROBE.json`：scene 430px / OLD-NEW 252.6px / node 143.14px / path 2.5-2-1.5 / studio preview 668px 全部 passed=true
  - `PROFILE_PERSISTENCE_EVIDENCE.json`；`EVIDENCE_SHA256SUMS.txt`
- 画廊：`docs/ui-vnext/gallery-phase1f/index.html`（仅 5 屏 PHASE1E→PHASE1F 并排）

## 剩余视觉限制（如实）

1. 无视觉能力：视觉结论全部来自 contract/probe/geometry/metrics；样张请 Human 直接查看。
2. `REAL_WINDOW_KEYBOARD_HUMAN_GATE`：Robot 无法在本会话把 OS 输入焦点可靠授给窗口，Ctrl+K 未能在真实窗口自动验证（in-process 已覆盖；建议 Human 复核）。
3. `IME_RUNTIME_HUMAN_GATE`：中文 IME 组合输入（银行卡/手机号/更换手机号）需 Human 在真实窗口验证。
4. Light theme：`LIGHT = FUNCTIONAL_SUPPORTED / VISUAL_REFINEMENT_LATER`（未做 full parity，不谎称）。
5. 自定义背景缩略图经 importer 路径验证；真实文件对话框未在 OS 层执行（in-process 标准，同 1E）。
6. 1280 下限与 font scaling 以 mechanical probe 覆盖，未做跨字体渲染回归。

## Git（本轮小步 commit，`feat/pdig-ui-vnext`）

1. `feat(ui-vnext): PHASE 1F groundwork — CardIdentitySystem (§9 synthetic per-issuer identities), CardDetail two-column, studio preview 660-740 + inspector default-open + custom background consumer UI`（合并为若干语义 commit，见 git log）
2. `feat(ui-vnext): PHASE 1F number dial-code identity + continuity scene scale/weight retune`
3. `feat(ui-vnext): PHASE 1F empty/healthy states + copy contract + copy cleanup + a11y focus`
4. `test(ui-vnext): PHASE 1F contract tests (card identity / empty copy / layout / keyboard)`
5. `feat(ui-vnext): PHASE 1F evidence harness — 12 shots + mechanical + journey + real-window smoke`
6. `docs(ui-vnext): PHASE 1F final docs + gallery + work status + blockers`

push origin：完成后执行（见 Git 输出）。禁令遵守：无 force push / rebase / reset --hard / tag 移动 / merge main / 创建 PHASE 1G / 进入 v0.4 / 触碰 product-v0.3.1。

## 结束语

按 brief §60/§61：本轮只写 `PHASE_1F_IMPLEMENTATION = PASS`、`DESKTOP_REFERENCE_CANDIDATE_FINAL = READY`、`DESKTOP_VISUAL_REFERENCE = NEEDS_HUMAN_FINAL_ACCEPTANCE`、`VISUAL_CRAFT = NEEDS_HUMAN_FINAL_ACCEPTANCE`；不进入 Android/iOS/Harmony，不 merge main，不开始 v0.4。等待 Human 决定 `DESKTOP_VISUAL_REFERENCE = ACCEPTED` 或 `TARGETED_SCREEN_FIX_REQUIRED`。
