# MULTI_CLIENT_VISUAL_ACCEPTANCE.md

> 2026-09-26；不做跨平台像素相等（§24），检查语义对等/信息完整/CTA 同义/无遮挡/无内部枚举/无 debug 信息。
> 截图：`artifacts/runtime-evidence/2026-09-26-multiclient-sweep/`；元数据：`screenshots.json`；哈希：`EVIDENCE_SHA256SUMS.txt`。

## 1. 各平台截图覆盖
| 平台 | 截图 | 主题 | 说明 |
|---|---|---|---|
| Desktop | **50 张**（24 页 primary 1280×720 + 13 复杂页 ×1920×1080/2048×1152） | light（产品仅 light） | `desktop/`；VERDICT PASS（--shots 50/50） |
| Android | **38 张**（19 页 × light/dark） | light+dark 实拍 | `android/`；MediaStore Downloads 方案 pull 成功 38 files（A1 已解决） |
| Harmony | 0（无 runtime/UI 未实现） | — | NOT_IMPLEMENTED |
| iOS | 0（无 app target） | — | N4 gap，不伪造 |

## 2. 语义对等检查结论（Desktop primary 与 Android 实拍/断言）
- 页面存在性：Desktop 24 页全部 reachable+rendered；Android 20 路由全部 reachable+rendered（suite + 38 张截图为证）。
- CTA 语义：同义（“确认/忽略/稍后”“设为必需/设为未知”“备份到新位置/导出为副本”）；无平台内部 enum 泄漏（Desktop 50 张抽查 + Android 语义树门禁 0 无标签）。
- 信息完整：Desktop node 页修复重复“名称”行后信息完整；Android 截图渲染断言通过。
- clipping/overflow：Desktop 3 个分辨率档无崩溃；Android 页面截图无断言失败。
- Dark：Desktop NOT_IMPLEMENTED（产品无主题）；Android light+dark 各 19 页实拍。

## 3. Gate（§25）
```text
MULTI_CLIENT_VISUAL_ACCEPTANCE = PARTIAL（限制仅来自 Harmony/iOS 产品范围，非证据缺失）
  - Desktop: 达成（50 张 + 语义检查 + 3 分辨率档）
  - Android: 达成（38 张：19 页 × light/dark，主机侧入仓）
  - Harmony / iOS: NOT_IMPLEMENTED（产品/项目范围；无 UI 可拍，不伪造）
```