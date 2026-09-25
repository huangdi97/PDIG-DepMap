# PDIG 0.2.0 Preview — Release Notes

> GitHub Release：`PDIG 0.2.0 Preview`（tag `product-v0.2.0`，Pre-release）。
> 日期：（待发布时填）。下载与 SHA256 见 Release attachments / SHA256SUMS.txt。

## 这一版更好了什么（用户视角）

- **更好的首次使用体验**：打开就知道"这是什么、下一步做什么"；不再有空白首页。
- **更顺畅的导入流程**：来源说明、隐私说明、字段对应（CSV 可改）、完成总结，一步步都有解释。
- **更清楚的待办聚合**："需要你处理"把待确认关系、待确认服务、需要处理的计划放在一起；
  "可能发生了变化"单独成区。
- **更易理解的场景与影响**：换卡 / 到期 / 注销三个场景入口；结果分四类
  （必须处理 / 可能还有其他可用方式 / 需要确认 / 当前未发现影响），
  每一项必须处理都说明"为什么、依据、依赖、不处理会怎样"。
- **更可靠的变更计划与验证**：明确区分"完成操作"与"确认结果"；"我做完了 ≠ 已验证"。
- **Windows 桌面更"桌面"**：左侧导航收敛到 7 个一级入口，基础设施支持左列表右详情，
  键盘可走完全流程。
- **双端一致**：Android / Windows 使用同一套用户语言（术语词典），语义不再分叉。
- **可靠性提升**：修复 v0.1.2 Windows 安装包无法启动的问题（打包链路重写并加产物自检）。

## 核心流程（双端）

导入数据 → 确认关系 → （换卡 / 到期 / 注销）影响分析 → 变更计划 → 完成与验证 → 备份与恢复。

## 安装

- **Windows x64**：`PDIG-0.2.0-windows-x64-setup.exe`（或 portable zip）。未签名，SmartScreen 提示为预期行为，请核对 SHA256。
- **Android**：`PDIG-0.2.0-android-preview.apk`（`com.pdig.app.preview`，GitHub Preview，非 Google Play）。

## 技术附注（面向开发者）

- v0.2.0 分支：`feat/product-v0.2.0-usability` → main（fast-forward，无 force）。
- Android：versionName 0.2.0 / versionCode 按 policy；targetSdk 36；无 INTERNET / analytics / telemetry。
- Desktop：jlink runtime + 手工 app-image 打包（`scripts/release/build-desktop-package.ps1`），
  打包后强制验证 JVM launcher 存在；修复 v0.1.2 portable/installer 缺 java/javaw 缺陷。
- 新增：Android CSV 字段对应、Infrastructure 搜索分组、NodeDetail 六问、计划聚合；
  Desktop 7 项一级导航、Attention 屏、Infrastructure 双栏、内部术语清零。
- 文档：`PRODUCT_EXPERIENCE_MAP_V0_2.md` / `DUAL_CLIENT_EXPERIENCE_MATRIX_V0_2.md` /
  `PRODUCT_TERMINOLOGY_V0_2.md` / `PRODUCT_ERROR_CATALOG.md` / `docs/user/*`。

## Known Limitations（如实）

- Developer Preview；无真实用户 / 真实数据验证（全合成 fixture）。
- Android 为 GitHub Preview，非 Google Play 生产（无生产 keystore / 商店账号）。
- Windows 未签名（SmartScreen）。
- Harmony / iOS 不在本轮。
- 无真机验证（API36 AVD 覆盖）；TalkBack / 屏幕阅读器为工程级检查，未做真人测试。
- CI 因 GitHub 账户计费外部阻断（E-10）；本地全量门禁为验收依据。
