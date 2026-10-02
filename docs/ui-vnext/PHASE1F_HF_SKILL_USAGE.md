# PHASE1F_HF_SKILL_USAGE.md

> PHASE 1F-HF（Human Final Acceptance Fix）—— 技能/方法使用记录（如实）。

## 本会话工具能力说明（诚实）

- 本会话没有可加载的 Skill 工具（ui-ux-pro-max / impeccable / frontend-design 未以
  Skill 方式加载）。因此本轮执行遵循 repo 既有 **No-Vision 契约/证据驱动** 方法：
  以 brief 的显式契约 + 确定性几何（`Phase1FLayout` / probe）+ 渲染级像素测试
  （`ThemeThumbnailBoundsContractTest`）+ 图像度量（`PdigImageMetrics`）驱动，
  不自行断言审美通过（`VISUAL_CRAFT` 永不 PASS）。
- 设计纪律（克制、不 templated、层级明确、consumer-facing）按 brief §5–§9 与
  既有 PHASE 1F 冻结方向执行；设计 skill 的通用原则（视觉层级、留白、可扫读）
  由人类 reviewer 依据 12 张主集最终判定。

## 方法/工具使用

| 用途 | 方法/工具 |
| --- | --- |
| 布局契约 | `Phase1FLayout.kt` 纯函数（Studio 预览宽、tile 高、continuity 几何、路径层级），probe 共用防漂移 |
| 渲染级 overflow 回归 | `ThemeThumbnailBoundsContractTest`：ImageComposeScene 并排渲染 + gap 像素断言（修复前 FAIL / 修复后 PASS） |
| 图像度量 | `tools/ui-vnext/image-metrics/PdigImageMetrics`（meanLuma / darkRatio / content bbox / dominant colors） |
| 真实窗口证据 | `VNextWindowSmoke1FHF` + `Win32WindowCapture`（JNA：FindWindowW / GetWindowRect / SetWindowPos TOPMOST / ShowWindow / SetForegroundWindow / InvalidateRect / UpdateWindow / PrintWindow），每步 target validation + 像素校验 + stale 帧防线 |
| 回归门 | `desktop :app:test`（Gradle 8.9 本地缓存，offline）+ `core npm run check`（format/lint/typecheck/487 tests/architecture/network/secrets/UI） |
| 证据再生 | `--vnext-shots-1f-hf`（12 主集 + 20 mechanical + 4 empty）+ `--vnext-window-smoke-1f-hf`（真实窗口） |

## 禁项确认

- 未使用 embedding / LLM / vector DB / 真实商标素材下载。
- 未下载新工具链（JNA 为既有依赖；Gradle 8.9 本地缓存）。
- 未删除/弱化测试、未加 ignore、未静默吞异常制造 PASS。
- `VISUAL_CRAFT = PASS` 未出现在任何产物中。
