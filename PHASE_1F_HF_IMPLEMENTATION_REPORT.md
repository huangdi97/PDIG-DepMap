# PHASE_1F_HF_IMPLEMENTATION_REPORT.md

> PDIG UI vNext — PHASE 1F-HF：Human Final Acceptance Fix（brief 2026-10-02，16 节）
> 目标：关闭 Human Final Review 暴露的真实视觉 blocker + 修复 real-window 证据完整性，
> 让 Desktop 到达可交由 Human Final Freeze 的状态，然后 STOP。

## 状态

```
PHASE_1F_HF_IMPLEMENTATION = PASS
DESKTOP_REFERENCE_CANDIDATE_FINAL = READY
DESKTOP_VISUAL_REFERENCE = NEEDS_HUMAN_FINAL_ACCEPTANCE
VISUAL_CRAFT = NEEDS_HUMAN_FINAL_ACCEPTANCE（No-Vision Agent 永不自行 PASS）
DOMAIN_REGRESSION = 0
CANONICAL_REGRESSION = 0
SECURITY_REGRESSION = 0
ANDROID_UI_VNEXT = HOLD
IOS_UI_VNEXT = HOLD
HARMONY_UI_VNEXT = HOLD
```

## 1. Git

- branch：`feat/pdig-ui-vnext`
- starting HEAD：`e9fdaf48a53cb1fe15a862982420feb7b694a1fe`（= origin，pre-flight 验证一致）
- ending HEAD：`caef922e6d36ead8e28bf496aa6a648b960e3b0c`（本轮 commit 序列末尾；push 后 = origin/feat/pdig-ui-vnext）
- commits：见 §11
- 禁令遵守：无 force push / reset --hard / rebase / merge main / tag 移动 / PHASE 1G / v0.4

## 2. Changed files（production 表现层 + evidence harness，PresentationProfile only）

| 文件 | 变更 |
| --- | --- |
| `desktop/app/src/main/kotlin/com/pdig/uivnext/ui/components/CardFace.kt` | §4：CardFaceThumbnail 加 `Modifier.clip` + 高度 104→112dp；`drawCardFaceBackdrop` 内 artwork/material 以 `clipRect` 包裹（本地 bounds 契约） |
| `desktop/app/src/main/kotlin/com/pdig/uivnext/ui/components/CardIdentityDrawing.kt` | §4/§6：glow/starfield/sweep/contour 半径受本地 bounds 约束（`boundsSafeRadius`）；ICBC RED_LINE 加微弱拉丝（避免「深色矩形 + 一条线」） |
| `desktop/app/src/main/kotlin/com/pdig/uivnext/ui/components/NumberFaceKit.kt` | §4/§7：NumberFaceThumbnail 加 clip + 高度 84→96dp；backdrop clipRect；新增通信信号母题（信号条 + 拨号弧，本地 bounds） |
| `desktop/app/src/main/kotlin/com/pdig/uivnext/ui/screens/StudioFrame.kt` | §5.A：LEFT 对象列表与 Theme grid 各自独立滚动区（weight 0.44/0.56），互不覆盖 |
| `desktop/app/src/main/kotlin/com/pdig/uivnext/ui/screens/StudioKit.kt` | §5.B：preview stage 增强（背板带 + spotlight 微调 + floor light + 接触阴影） |
| `desktop/app/src/main/kotlin/com/pdig/uivnext/ui/screens/CardsScreen.kt` | §9：Cards Empty 居中于 content stage（Box + Center），非贴左上角 |
| `desktop/app/src/main/kotlin/com/pdig/uivnext/ui/components/EmptyStates.kt` | §9：空态 480–600px 宽、语义插画 44→64dp + 背光圆盘、motif 按 size 缩放 |
| `desktop/app/src/main/kotlin/com/pdig/uivnext/layout/Phase1FLayout.kt` | §8：路径层级 2.0/1.5/1.0（migrated ≤2 / secondary ≤1.5 / ghost ≤1）；service node 0.085→0.095 |
| `desktop/app/src/main/kotlin/com/pdig/uivnext/ui/screens/ContinuitySceneDrawing.kt` | §8：路径 alpha/宽度降低、control 点更紧凑（减少穿越）、引用 Phase1FLayout 常量 |
| `desktop/app/src/main/kotlin/com/pdig/uivnext/ui/screens/ContinuitySceneNodes.kt` | §8：服务节点强化（glyph 19 / 名称 14sp / role 11sp / pill 加大，node state > line style） |
| `desktop/app/src/main/kotlin/com/pdig/desktop/Main.kt` | 新增 CLI：`--vnext-shots-1f-hf` / `--vnext-window-smoke-1f-hf`（既有 1f 参数保留） |
| `desktop/app/src/main/kotlin/com/pdig/uivnext/evidence/VNextPhaseEvidence1FHF.kt` | 新：12 张主集 + mechanical + empty + probe（§4/§8/§11 新增检查） |
| `desktop/app/src/main/kotlin/com/pdig/uivnext/evidence/VNextWindowSmoke1FHF.kt` | 新：真实窗口 smoke —— target-bound 截图 + 像素校验 + stale 帧防线 + `REAL_WINDOW_TARGET_VALIDATION.json` |
| `desktop/app/src/main/kotlin/com/pdig/uivnext/evidence/Win32WindowCapture.kt` | 新：JNA Win32 定向窗口捕获（FindWindowW/GetWindowRect/SetWindowPos TOPMOST/ShowWindow/PrintWindow/InvalidateRect） |
| `desktop/app/src/test/kotlin/com/pdig/uivnext/ui/ThemeThumbnailBoundsContractTest.kt` | 新：渲染级 bounds 契约测试（Card+Number themes，gap 像素断言） |
| `desktop/app/src/test/kotlin/com/pdig/uivnext/layout/Phase1FLayoutContractTest.kt` | 契约值随 §8 更新（2.0/1.5/1.0） |

## 3. Card Studio overflow —— root cause & exact fix

- **Root cause**：`drawBehind { drawCardFaceBackdrop(...) }` 未裁剪；艺术层几何使用 **w 相对大圆半径**
  （glow `w*0.72`、matte vignette `w*0.7`、starfield `w*0.5`），在 104dp 高的短缩略图上垂直越出
  大量像素；`drawBehind` 无 clip，越界内容直接覆盖相邻 tile / 下方区域。PHASE 1E 引入、1F 未关闭。
- **Fix**：
  1. `CardFaceThumbnail`：`Modifier.clip(RoundedCornerShape)` 置于 `drawBehind` **之前**（形状裁剪）；
  2. `drawCardFaceBackdrop`：identity art + material 全部包在 `clipRect { }`（渲染器级裁剪）；
  3. `drawCardIdentity` 径向半径改为 `minOf(w*f, h*f)`（本地 bounds 约束，宽高比安全）。
- **验证**：`ThemeThumbnailBoundsContractTest`（渲染级，修复前会 FAIL：gap 像素泄漏；
  修复后 5 个测试全 PASS）；机械档渲染 + IMAGE_METRICS 无异常。

## 4. Number Studio overflow —— root cause & exact fix

- **Root cause**：`NumberFaceThumbnail` 同型缺陷：`drawBehind` 未裁剪，Travel 主题
  `w*0.32/w*0.24` 大圆在 84dp tile 上越界（右上/左下溢出），tile 间距内出现泄漏。
- **Fix**：同 §3：`Modifier.clip` + backdrop `clipRect` + travel 圆半径 `minOf(w*f, h*f)`；
  并新增通信身份母题（信号条 + 拨号弧），tile 高度 84→96dp。
- **验证**：`numberThemeThumbnailsDoNotLeakArtworkBeyondTileBounds` 等 PASS。

## 5. real-window wrong-target —— root cause & exact fix

- **Root cause（PHASE 1F 遗留）**：旧 harness 用 `Window.getWindows().firstOrNull{showing Frame}`
  拿 bounds 后直接 `Robot.createScreenCapture(bounds)`，**未验证目标窗口身份，也未校验捕获像素**
  —— 前台是 Chrome/其他应用时照样写 `capture=true`，导致 real-window PNG 实为浏览器页面。
- **Fix（VNextWindowSmoke1FHF + Win32WindowCapture）**：
  1. target 绑定：AWT 枚举 + `FindWindowW` 真实 HWND（标题必须 `PDIG*`）；
  2. 捕获前记录 processId / windowTitle / HWND / windowBounds（GetWindowRect 设备像素）/ screen / method；
  3. 捕获前 `SetWindowPos(HWND_TOPMOST)` + `ShowWindow(SW_RESTORE)` + `SetForegroundWindow` +
     `InvalidateRect` + `UpdateWindow`（Windows 前台锁下 AWT toFront 无效）；
  4. 捕获后像素校验带 `isDarkTheme`（meanLuma ∈ [0.03, 0.45]，topBand ≤0.60）—— 拒绝
     Chrome/白色页（亮）与 PrintWindow 纯黑空白（过低）；
  5. **stale 帧防线**：screen 变化但捕获帧与上一步逐字节相同 → `capture=false` 且删除 PNG；
  6. 全部写入 `REAL_WINDOW_TARGET_VALIDATION.json`（每步一条）。
- **本机实测结果**：窗口身份全部命中（title/bounds/HWND/screen 记录齐全）；但本机桌面会话
  （GPU/DComp 合成 + Skiko 窗口不响应 WM_PRINT）下 GDI BitBlt 只返回窗口**首帧**——
  `01-now.png` 为真实 PDIG 首帧（暗色、Now 内容、已验证），02–14 因 stale 帧被**如实拒绝**
  （capture=false，未写证据）。这与 PHASE 1F「截到 Chrome 却写 capture=true」形成对照：
  harness 现在能发现并拒绝错误/陈旧 target —— 证据完整性缺陷已闭环。

## 6. §5–§9 视觉收口

- §5 Studio：三栏架构不变；LEFT 双滚动区（对象/主题互不覆盖）、tile 112dp（108–120 区间）；
  CENTER 预览 660–740px + 有节制舞台（spotlight/floor/contact shadow）；RIGHT 默认展开「材质」。
- §6 Card Identity：CardIdentitySystem 未重设计；ICBC 强化（拉丝 + 红线 + emblem 布局），
  双尺度（Grid/Detail）同渲染器；≥3 身份要素契约保持；无真实商标素材。
- §7 Number Studio：通信身份语言（信号条 + 拨号弧），无 CardFace/1.586/chip 语法；
  Travel/Banking/Recovery 保持 visual preset 边界；Number Detail 基础布局未动。
- §8 Continuity：语义不变；node 强化（状态 pill > line style）、路径 2.0/1.5/1.0 + alpha 降低、
  control 紧凑减少穿越、路径在 node 背后；After 保留 ghost OLD / 主角 NEW / 未迁移 unresolved +
  计划投影徽标。
- §9 Empty：Cards Empty 居中 480–600px 紧凑构图（语义插画 64dp + title + desc + 双 CTA）；
  其余空态复查；Unknown 语义与禁词契约保持（Phase1FEmptyCopyContractTest 仍绿）。

## 7. 测试证据

- `desktop :app:test`：**56 项全 PASS**（含新增 ThemeThumbnailBoundsContractTest 5 项；
  既有 CardIdentity/EmptyCopy/Layout/Keyboard/Interaction/Persist/Importer 等全绿）。
- `core npm run check`：**全绿** —— 45 test files / **487 tests passed**；architecture
  circular = 0；network gate 0 primitives；secret scan 0；UI static gate PASS；format/lint/typecheck 通过。
- DOMAIN_REGRESSION = 0 / CANONICAL_REGRESSION = 0 / SECURITY_REGRESSION = 0
  （core 零改动、desktop 仅表现层 + evidence harness；git diff 无 spec/canonical/schema/.depmap 改动）。

## 8. 证据（artifacts/runtime-evidence/2026-10-02-ui-vnext-phase1f-hf/）

- `profiles/1920x1080@1.0/`：**恰好 12 张** Human Final Review 主集（Privacy Mask ON）：
  1 `vnext__now__1920x1080@1.0.png`
  2 `vnext__overview-global__1920x1080@1.0.png`
  3 `vnext__cards__1920x1080@1.0.png`
  4 `vnext__card-detail__1920x1080@1.0.png`
  5 `vnext__card-studio-glass__1920x1080@1.0.png`
  6 `vnext__card-studio-city__1920x1080@1.0.png`
  7 `vnext__numbers__1920x1080@1.0.png`
  8 `vnext__number-detail__1920x1080@1.0.png`
  9 `vnext__number-studio-travel__1920x1080@1.0.png`
  10 `vnext__change-transition__1920x1080@1.0.png`
  11 `vnext__change-after__1920x1080@1.0.png`
  12 `vnext__cards-empty__1920x1080@1.0.png`
- `mechanical/`：1280×720@1.0 / 2560×1440@1.0 / 1920×1080@1.25 / 1920×1080@1.5（每档 5 屏 = 20 帧）
  + `empty-states/` 4 帧。
- `IMAGE_METRICS.json`：12 帧 meanLum 0.082–0.108，0 error / 0 empty / 0 near-black。
- `UI_LAYOUT_PROBE.json`：§4/§8/§11 新增检查 12 项（theme tile height/clipped、themeGrid noOverlap、
  stage、inspector defaultOpen、number tile/communication、path ≤2/1.5/1、continuity labels、
  empty.cards 480–600 + centered）全部 `passed=true`（另有既有 probe 项）。
- `EVIDENCE_SHA256SUMS.txt`：37 个 PNG 全部哈希。
- `REAL_WINDOW_TARGET_VALIDATION.json`：16 步逐条记录（processId / windowTitle / hwnd /
  windowBounds / captureMethod / targetValidation / screen / sha256）；01 capture=true
  （PDIG 窗口首帧，暗色验证通过），02–14 capture=false（stale-capture-surface），15/16 键盘 Human Gate。
- `real-window/`：`01-now.png`（真实 PDIG 窗口首帧）+ `WINDOW_SMOKE_LOG.txt`。
- 旧证据 `2026-10-02-ui-vnext-phase1f/` 原样保留（BEFORE；其中 real-window 的
  错误-target 截图问题已由本轮 P0 修复并在 HF 证据中如实标注）。

## 9. Real-window target-validation 结果

- 每次捕获都记录：processId（如 24016）、windowTitle（PDIG Preview — PHASE 1F-HF real-window smoke）、
  HWND（Win32 获取，记录为 n/a 当 JNA 不可用时 / 数字值可用）、windowBounds（GetWindowRect 设备像素，
  如 48,48,800,600）、captureMethod、targetScreen（\Display0）、targetValidation{titleMatch, showing,
  boundsOnScreen, pixelsDark, meanLuma, topBandLuma}、sha256。
- 01：titleMatch=true, showing=true, boundsOnScreen=true, pixelsDark=true（meanLuma 0.08）→ capture=true。
- 02–14：身份均命中，但帧与上一步逐字节相同（本机合成器不向 GDI 暴露实时帧）→ capture=false
  `stale-capture-surface`，PNG 未写入 —— 无任何记录对非 PDIG 窗口声称 capture=true。
- 15/16：`REAL_WINDOW_KEYBOARD_HUMAN_GATE`（保留）；`IME_RUNTIME_HUMAN_GATE`（保留）。

## 10. Remaining Human Gates（如实）

1. `DESKTOP_VISUAL_REFERENCE` / `VISUAL_CRAFT`：由 Human / Vision Reviewer 判定
   （ACCEPTED 或继续 TARGETED_SCREEN_FIX）。
2. `REAL_WINDOW_KEYBOARD_HUMAN_GATE`：真实窗口 Ctrl+K 由 Human 复核。
3. `IME_RUNTIME_HUMAN_GATE`：真实窗口中文 IME 由 Human 复核。
4. `REAL_WINDOW_MULTI_FRAME_ENVIRONMENT_GATE`（本轮新增，环境性）：本机桌面会话的
   GDI BitBlt 仅返回窗口首帧且 Skiko 窗口不响应 WM_PRINT，无法自动取得跨屏实时窗口帧；
   harness 已如实拒绝（capture=false）而非伪造。换用暴露实时合成像素的环境（如正常本地
   交互桌面/直连显示器）后可在同一 harness 上重跑取得完整 01–14 真实窗口证据。
5. Light theme：`LIGHT = FUNCTIONAL_SUPPORTED / VISUAL_REFINEMENT_LATER`（不谎称 full parity）。

## 11. Commits（本轮）

见 `git log`：按语义拆分（fix overflow + studio craft / continuity weighting + empty states /
evidence harness + Win32 capture / contract tests + probe / docs + status）。push 至
`origin feat/pdig-ui-vnext`（fast-forward only）。

## 12. 结束语

按 brief §15/§16：本轮只写
`PHASE_1F_HF_IMPLEMENTATION = PASS`、`DESKTOP_REFERENCE_CANDIDATE_FINAL = READY`、
`DESKTOP_VISUAL_REFERENCE = NEEDS_HUMAN_FINAL_ACCEPTANCE`、`VISUAL_CRAFT = NEEDS_HUMAN_FINAL_ACCEPTANCE`；
不进入 Android/iOS/Harmony，不 merge main，不 tag，不开始 v0.4，不创建 PHASE 1G。
等待 Human / Vision Reviewer 最终裁决。
