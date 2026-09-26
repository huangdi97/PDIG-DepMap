# MULTI_CLIENT_VISUAL_ACCEPTANCE.md

> 2026-09-26；不做跨平台像素相等（§24），检查语义对等/信息完整/CTA 同义/无遮挡/无内部枚举/无 debug 信息。
> 截图：`artifacts/runtime-evidence/2026-09-26-multiclient-sweep/`；元数据：`screenshots.json`；哈希：`EVIDENCE_SHA256SUMS.txt`。

## 1. 各平台截图覆盖
| 平台 | 截图 | 主题 | 说明 |
|---|---|---|---|
| Desktop | **50 张**（24 页 primary 1280×720 + 13 复杂页 ×1920×1080/2048×1152） | light（产品仅 light） | `desktop/`；VERDICT PASS（--shots 50/50） |
| Android | **0 张主机侧**（设备端 38 张生成并断言通过，取回被 A1 环境阻断） | light+dark 均已渲染断言 | 见 ANDROID_RUNTIME_FINAL_REPORT §5 |
| Harmony | 0（无 runtime/UI 未实现） | — | NOT_IMPLEMENTED |
| iOS | 0（无 app target） | — | N4 gap，不伪造 |

## 2. 语义对等检查结论（Desktop primary 与 Android 渲染断言）
- 页面存在性：Desktop 24 页全部 reachable+rendered；Android 20 路由全部 reachable+rendered（suite 渲染/断言）。
- CTA 语义：同义（如“确认/忽略/稍后”“设为必需/设为未知”“备份到新位置/导出为副本”）；无平台内部 enum 泄漏（截图检查无内部 id/JSON/枚举串——Desktop 50 张人工抽查 + Android 语义树门禁 0 无标签）。
- 信息完整：Desktop node 页修复重复“名称”行后信息完整；Android 语义树 14 屏 0 无标签交互节点。
- clipping/overflow：Desktop 3 个分辨率档无崩溃/无断言失败；Android 页面断言通过。
- Dark：Desktop NOT_IMPLEMENTED（产品无主题）；Android 设备端 dark 渲染断言通过（light+dark 各 19 页）。

## 3. Gate（§25）
```text
MULTI_CLIENT_VISUAL_ACCEPTANCE = PARTIAL
  - Desktop: 达成（截图 + 语义检查 + 3 分辨率档）
  - Android: 设备端达成 / 主机证据受限（A1）
  - Harmony/iOS: NOT_IMPLEMENTED（产品/项目范围）
```