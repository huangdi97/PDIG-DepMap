# DESKTOP_REFERENCE_FREEZE_CANDIDATE.md

> PDIG UI vNext — Desktop Reference Freeze Closure（2026-10-02）
> 依据 brief（16 节）执行**证据 / 状态 / 溯源收口**，不是新的设计 Phase。

## 状态（Agent 只允许写到这一档，禁止自行宣布 ACCEPTED）

```text
DESKTOP_REFERENCE_FREEZE_CANDIDATE = READY
DESKTOP_VISUAL_REFERENCE = NEEDS_HUMAN_FINAL_ACCEPTANCE
VISUAL_CRAFT = NEEDS_HUMAN_FINAL_ACCEPTANCE（No-Vision Agent 永不自行 PASS）
PHASE_1F_HF_IMPLEMENTATION = PASS（HF 轮结论；本轮仅收口）
DESKTOP_REFERENCE_FREEZE = HOLD（等待 Human 明确写入）
```

Human 明确回复 `DESKTOP_VISUAL_REFERENCE = ACCEPTED` 后，才允许启动
Desktop Reference Freeze → Platform Translation Contract → Android → iOS → Harmony。

## 1. Git（exact）

- branch：`feat/pdig-ui-vnext`；remote：`huangdi97/PDIG-DepMap`
- **starting remote HEAD**：`21de0bfe8e48ec4dba25c7053fdb9a54e92c3a56`（pre-flight `git fetch origin` 后一致）
- **ending remote HEAD**：本轮 push 后的 docs commit（见 §2 commit chain；push 后以
  `git rev-parse origin/feat/pdig-ui-vnext` 为准，最终值记录于本轮最终交付报告与
  `PROVENANCE_VALIDATION.txt` post-push 追加段）
- `origin/main` = `cbe99d5c8862f90b25a463585cfd7ad146e4fb82`（main 是 feat 分支祖先，
  `git merge-base` 验证）
- 禁令遵守：无 force push / rebase / reset --hard / merge main / tag / release。

## 2. Commit chain（本轮，真实 history，全部 `git cat-file -e <sha>^{commit}` 验证）

1. `858ef648a8db3930613045a91f03c296e0f02a3a` fix(ui-vnext): make studio evidence variants
   state-truthful — isolated deterministic profile store + expected/actual gate + FINAL_SCREENSHOT_MANIFEST.json
2. `4862cb42715d45a4325aef6c4d237f8b9182226b` test(ui-vnext): add VisualVariantEvidenceContractTest —
   variant semantic state + render SHA256 truth（card glass/city、number country/travel/recovery、
   change current/transition/after、region global/HK）
3. `4163340e3d606bde40ebbef54074ed0b03663057` feat(ui-vnext): regenerate Final Human Review evidence —
   12-shot hf2-final pack（glass != city）、manifest/metrics/probe/sums、CLI outRoot switch
4. `docs(ui-vnext): correct HF provenance + freeze candidate + status`（本文件所在 commit）

HF 轮真实链（修正后的 provenance，见 `PROVENANCE_VALIDATION.txt`）：
`e9fdaf48…` → `9d828e0…` → `185c96c…` → `df98135…` → `21de0bfe…`。

## 3. Final Human Review Pack（恰好 12 张，1920×1080@1.0，Privacy Mask ON）

目录：`artifacts/runtime-evidence/2026-10-02-ui-vnext-phase1f-hf2-final/profiles/1920x1080@1.0/`
（旧 `2026-10-02-ui-vnext-phase1f-hf` 保留为历史；其 glass/city 同帧为 Human 已核验的无效证据）

| #   | screen                  | file                                             | SHA-256                                                            |
| --- | ----------------------- | ------------------------------------------------ | ------------------------------------------------------------------ |
| 1   | Now                     | `vnext__now__1920x1080@1.0.png`                  | `e389821e05fcb46bb6d79b497831c992f202f912113eb0f0d1d8bbb552a7561e` |
| 2   | Infrastructure Overview | `vnext__overview-global__1920x1080@1.0.png`      | `074cc11cd7772e8f4bb035ed3bcc716f256b8e97426eb46155e6bab9b13ac47d` |
| 3   | Cards                   | `vnext__cards__1920x1080@1.0.png`                | `2bdb7b35a8a2c3e61d445d2341262f2e085af47aafab529cd18846305a0f0687` |
| 4   | Card Detail             | `vnext__card-detail__1920x1080@1.0.png`          | `cd635041a169a6c7411e5b9937867cc8fe2fa08989ae7ef02e5fde9d852b9c77` |
| 5   | Card Studio Glass       | `vnext__card-studio-glass__1920x1080@1.0.png`    | `ae378d810e2e627b8ba5ed66f4ccae84aec33d60cc4d36bc3bbf8c8660bb174b` |
| 6   | Card Studio City        | `vnext__card-studio-city__1920x1080@1.0.png`     | `17cde3fb8610396005953c052e4b484a870bf3b313d003161c58da3a7652786c` |
| 7   | Numbers                 | `vnext__numbers__1920x1080@1.0.png`              | `8729597ee29e1508df8e7396c7f1057efd30bcdbe4c2e4ce06b5e4c18f235c00` |
| 8   | Number Detail           | `vnext__number-detail__1920x1080@1.0.png`        | `9c9126848008baec4f42e38c031bd392455af1162f69db065cfd44d0edc7453e` |
| 9   | Number Studio Travel    | `vnext__number-studio-travel__1920x1080@1.0.png` | `b5b644b49cf31833501782303d6851362ae691f4112a0005be658280cf9b9a3f` |
| 10  | Change Phone Transition | `vnext__change-transition__1920x1080@1.0.png`    | `9b737f39cbf5b3f52bf1b6c9cf97c8923690ea8c2071b1185da1d3176f6dc416` |
| 11  | Change Phone After      | `vnext__change-after__1920x1080@1.0.png`         | `b878606200012a51d9a9af0559a2f3077a6b5b21b447d0ed6e5d309301f9bc28` |
| 12  | Cards Empty             | `vnext__cards-empty__1920x1080@1.0.png`          | `24d97aeb3c93e0c10bb4f313b73eced2cf1f1de13544f203962dc337af03e56a` |

**shot 5 SHA ≠ shot 6 SHA**：`ae378d81…` ≠ `17cde3fb…`（bytes 553550 ≠ 532519；采样像素差异 ≈12.9%，
Glass 与 City 身份人眼可直接区分）。`FINAL_SCREENSHOT_MANIFEST.json` 记录 expected/actual 全等
（`stateValidation=true` × 12；studio 帧 actual 来自 composition 回写 `themeSource=composition-readback`）。

## 4. Glass/City 同帧 P0 —— root cause 与 exact fix

- **Root cause**：`CardCustomizationScreen` 的初始 profile 解析顺序是
  `profileStore.load(...) ?: themeOverride ?: fallback`；证据 harness 用
  `defaultProfileStore()`（= `~/.pdig/presentation-profiles.json`），而该文件已存在
  `card:card-cn-2`（themeId=glass、imported 本地背景）—— **持久化偏好先于 evidence
  customTheme**，于是 glass 帧与 city 帧都渲染同一个用户 profile → 两帧逐字节相同
  （GitHub blob SHA `b399f884…`，469151 bytes）。
- **Fix**：
  1. 证据 harness 注入**隔离、确定性的** `PresentationProfileStore`（证据目录内临时文件，
     开始前删除，绝不读用户主目录）→ `themeOverride` 确定性生效；
  2. 抽出共享纯函数 `resolveStudioProfile(store, type, id, override, fallback)`
     （UI 与 harness 同源，防 drift；产品行为不变：load → override → fallback）；
  3. harness 截图前记录 expected/actual，不一致 **FAIL 该帧（不写 PNG）**；
  4. Studio 屏新增 composition 回写 `app.evidenceThemeId`（SideEffect），
     manifest actualState 取真实渲染主题（composition-readback）。
- **验证**：shot5/6 SHA 不同 + 字节不同 + 采样像素 12.9% 差异 + 契约测试渲染级断言 +
  manifest `stateValidation=true`。

## 5. Variant Truth Contract（新增测试）

`desktop/app/src/test/kotlin/com/pdig/uivnext/evidence/VisualVariantEvidenceContractTest.kt`（5 项全 PASS）：

- Card Studio glass≠city：semantic（themeId=glass/city 与 manifest 一致）+ SHA256 不等
- Number Studio country≠travel≠recovery：两两 SHA256 不等 + semantic 一致
- Change Phone current≠transition≠after：projection semantic + 两两 SHA256 不等
- Region global≠HK：cameraPreset semantic 不同 + SHA256 不等（camera global vs hk）
- P0 回归：隔离 store 下 override 生效；用户 store 下持久化优先（= P0 机制），
  证据 harness 禁止使用用户 store

## 6. 测试

- `desktop :app:test`（`android\gradlew.bat -p desktop :app:test`）：**61/61 PASS**
  （56 既有 + 5 新增 VisualVariantEvidenceContractTest；0 failure）
- `core npm run check`：**全绿** —— format:check / format:docs:check / lint / typecheck /
  tests / check:architecture（circular=0）/ check:network（0 primitives）/ check:secrets（0）/
  check:ui（UI static gate PASS）；`core/` 源码零改动（git diff 验证）
- DOMAIN_REGRESSION = 0 / CANONICAL_REGRESSION = 0 / SECURITY_REGRESSION = 0
  （git diff 无 spec / schema / .depmap / PersonalReality / core 改动）

## 7. Responsive mechanical regression（五档，无新增 viewport）

1280×720@1.0 / 1920×1080@1.0 / 2560×1440@1.0 / 1920×1080@1.25 / 1920×1080@1.5：

- 20 帧 mechanical + 4 帧 empty-states 全部确定性渲染（`mechanical/`）；
- `UI_LAYOUT_PROBE.json`：§11 检查全 `passed=true` —— no theme artwork overflow /
  no thumbnail overlap / no clipped CTA / no vertical text / no hidden status pill /
  no studio panel overlap / no continuity label collision + §4 theme tile bounds
  （Card 112dp ∈ [108,120]、Number 96dp ∈ [90,110]）+ §8 continuity 路径权重
  （primary ≤2 / secondary ≤1.5 / ghost ≤1）+ Cards Empty 居中 480–600px；
- `IMAGE_METRICS.json`：12 帧 meanLum 0.061–0.217，0 error / 0 empty / 0 near-black。

## 8. Real-window gate（如实分层，不互相冒充）

- **DESKTOP_VISUAL_REFERENCE_FREEZE 依据**：production deterministic Compose render +
  Human-approved screenshots + layout/probe + test evidence（本候选包）。
- **REAL_WINDOW_MULTI_FRAME_RUNTIME_ACCEPTANCE = ENVIRONMENT_GATE**：
  - 既有证据（HF 轮，committed）：`01-now.png` = 真实 PDIG 窗口首帧（titleMatch/showing/
    boundsOnScreen/pixelsDark 全 true，meanLuma 0.08）→ capture=true；02–14 capture=false
    `stale-capture-surface`（合成器不向 GDI 暴露实时帧）；15/16 键盘 Human Gate。
  - 本轮无新工具重跑一次（`--vnext-window-smoke-1f-hf`）：**0/16 通过** —— 本会话前台
    被其它窗口遮挡，捕获区亮度 ≈0.99 与 PDIG 暗色不符，harness 全部如实拒绝
    （capture=false，不写 PNG）。环境 Gate 保持；未下载任何新截图工具。
  - 换用暴露实时合成像素的环境（正常交互桌面/直连显示器）可在同一 harness 重跑。
- `REAL_WINDOW_TARGET_VALIDATION.json` 语义保持：capture=true 必须 titleMatch + showing +
  boundsOnScreen + pixelsDark + nonStale 全真；任一失败 capture=false 不写 PNG；
  `SCREENSHOT_API_SUCCESS ≠ TARGET_VALIDATED`。

## 9. Human Gates（保留）

1. `DESKTOP_VISUAL_REFERENCE` / `VISUAL_CRAFT`：Human / Vision Reviewer 判定（ACCEPTED 或继续 TARGETED_SCREEN_FIX）。
2. `REAL_WINDOW_KEYBOARD_HUMAN_GATE`：真实窗口 Ctrl+K 由 Human 复核。
3. `IME_RUNTIME_HUMAN_GATE`：真实窗口中文 IME 由 Human 复核。
4. `REAL_WINDOW_MULTI_FRAME_ENVIRONMENT_GATE`：环境性，见 §8。
5. Light theme：`LIGHT = FUNCTIONAL_SUPPORTED / VISUAL_REFINEMENT_LATER`（不谎称 full parity）。

## 10. Platform HOLD

```text
ANDROID_UI_VNEXT = HOLD
IOS_UI_VNEXT = HOLD
HARMONY_UI_VNEXT = HOLD
```

不开始跨端翻译；不宣称 IOS_BUILD / *_RUNTIME PASS（无 macOS / 真机）。

## 11. Provenance

- `PHASE_1F_HF_IMPLEMENTATION_REPORT.md`：ending HEAD 由不存在的 `caef922e…` 修正为真实
  `21de0bfe…`；STARTING / IMPLEMENTATION / EVIDENCE / DOCUMENTATION / REMOTE HEAD 按真实
  history 重建（§1 / §11）。
- `PROVENANCE_VALIDATION.txt`：`all_reported_commits_exist = true`；全部报告 SHA 通过
  `git cat-file -e <sha>^{commit}`。
- 不存在的 SHA 一律不得进入 Final Freeze 文档。
