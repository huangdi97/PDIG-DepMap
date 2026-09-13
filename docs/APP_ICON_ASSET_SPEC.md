# APP_ICON_ASSET_SPEC.md — 应用图标 / 启动图资产规格

> 现状：**无正式图标资产**（`STORE_ASSETS_READY = BLOCKED`）。
> 本文件给出可直接交付给设计/生成的规格。**不得使用随手制作的测试图标上线。**

---

## 1. 品牌前提

| 项 | 状态 |
|---|---|
| 正式产品名 | **NEEDS_USER_DECISION**（当前"个人数字依赖图"，工程名 PDIG） |
| 主色 | `#4C4FD8`（见 `app/theme/tokens.uts`） |
| 背景色 | `#F5F6FA` |
| 图标语言 | 线性 / 填充二选一，**不得混用**；不得使用 emoji |

> 品牌名未定前，可先按"中性符号"方向出图（例如：卡片 + 分支节点），定名后再微调。

## 2. 设计原则

- 单色可辨识：缩到 48px 仍能认出。
- 不使用文字（小尺寸不可读）。
- 不使用银行 / 支付机构商标或近似图形。
- 不使用与参考提醒类产品相同或近似的图标。

## 3. Android

| 资产 | 规格 |
|---|---|
| Adaptive Icon 前景 | 432×432 px（安全区 264×264） |
| Adaptive Icon 背景 | 432×432 px（纯色或极简纹理） |
| Legacy 图标 | 48 / 72 / 96 / 144 / 192 px（mdpi → xxxhdpi） |
| Play 商店图标 | 512×512 px，PNG，32-bit，无透明 |
| 特性图（Feature Graphic） | 1024×500 px，PNG/JPG |

路径建议：`platforms/android/core/src/main/res/mipmap-*/`

## 4. iOS

| 资产 | 规格 |
|---|---|
| App Icon | 1024×1024 px，PNG，**无 alpha** |
| 由 Xcode 自动派生 | 20/29/40/58/60/76/80/87/120/152/167/180 px |

路径建议：`platforms/ios/Resources/Assets.xcassets/AppIcon.appiconset/`

## 5. HarmonyOS

| 资产 | 规格 |
|---|---|
| 应用图标 | 216×216 px（含前景 / 背景分层，遵循 AGC 规范） |
| 启动图 | 按 AGC 要求提供 |

路径建议：`platforms/harmonyos/entry/src/main/resources/base/media/`

## 6. 启动图（Splash）

| 平台 | 规格 |
|---|---|
| Android | 建议使用系统 SplashScreen API；中心图标 288×288 px |
| iOS | LaunchScreen storyboard，中心图标 200×200 pt |
| HarmonyOS | 按 AGC 启动页规范 |

**要求**：Splash 只显示图标 + 背景色，不显示营销文案。

## 7. 命名与存放约定

```
assets/
  icon/
    android/  ic_launcher_foreground.png, ic_launcher_background.png, play_icon_512.png
    ios/      AppIcon-1024.png
    harmony/  icon_foreground.png, icon_background.png
  splash/
    android/  splash_icon.png
    ios/      launch_logo.png
    harmony/  splash_icon.png
```

> `assets/` 目录已建立并带 README。二进制资产入库前请确认体积与 license。

## 8. 交付检查

- [ ] 三端图标同一视觉母版
- [ ] 无文字、无第三方商标
- [ ] 缩略图可辨识（48px 测试）
- [ ] iOS 版本无 alpha 通道
- [ ] 商店图标尺寸 / 格式符合各平台要求
- [ ] 与 `store/SCREENSHOT_PLAN.md` 的视觉风格一致
