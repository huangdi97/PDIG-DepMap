# ANDROID_BRAND_ASSET_SPEC.md

> 生成时间：2026-09-22（ANDROID_CANONICAL_FREEZE → Production/Reality Closure 轮）
> 状态：**NOT_FINAL_BRAND_ASSET** —— 所有当前实现均为占位；最终品牌由用户决策（R-2）。
> 本文件是**规格**（像素尺寸 / 安全区 / 格式 / 约束 / 落地路径），不是「已完成的最终素材」声明。
> 对应 Gate：`ANDROID_STORE_ASSETS_READY = BLOCKED_BY_BRAND_DECISION`。

---

## 1. 当前占位状态（如实记录）

| 资产 | 当前实现 | 是否最终 | 依赖用户决策 |
|------|----------|----------|--------------|
| launcher icon | `res/drawable/ic_launcher`（占位） | ❌ `NOT_FINAL_BRAND_ASSET` | 品牌图样（R-2） |
| adaptive icon | 未配置 foreground/background 分层 | ❌ 缺失 | 同上 |
| monochrome icon | 未配置 | ❌ 缺失（API33+ 可选） | 同上 |
| splash | 未配置 `Theme.SplashScreen`（用 `Theme.PDIG` 起屏） | ❌ 占位级 | 同上 |
| app name（桌面） | `PDIG` / `个人数字基础设施图谱` | ❌ 占位（内部代号） | R-1/R-2 |
| feature graphic | 无 | ❌ 缺失 | 品牌 + 截图素材 |
| Play screenshots | 无（shot list 见 ANDROID_SCREENSHOT_SHOT_LIST.md） | ❌ 缺失 | 品牌 + 合成数据拍摄 |

---

## 2. Launcher Icon（自适应）

| 项 | 规格 | 说明 |
|----|------|------|
| 前景层（foreground） | **108×108 dp 画布**，核心图形安全区 **66×66 dp**（即 108dp 圆内 ~61% 区域） | PNG（推荐 432×432 px @4x）或 VectorDrawable；**透明背景** |
| 背景层（background） | **108×108 dp**，纯色或图形 | 与前景对比安全；浅/深色主题都要可见 |
| 旧版 `mipmap` | 48 / 72 / 96 / 144 / 192 px（`mipmap-mdpi/hdpi/xhdpi/xxhdpi/xxxhdpi`） | 兼容 <API26 启动器（minSdk=26，仍建议整套） |
| monochrome | **单色（alpha-only）**，API33+ | `mipmap-anydpi-v33/ic_monochrome.xml`；系统主题图标用 |
| 圆形安全区 | 中心 72×72 dp 视觉圈内 | 避免被 OEM 遮罩裁切 |

落地路径：`android/app/src/main/res/mipmap-*/` + `mipmap-anydpi-v26/ic_launcher.xml`（adaptive）+ `mipmap-anydpi-v33/ic_monochrome.xml`。

---

## 3. Splash

| 项 | 规格 |
|----|------|
| 推荐实现 | AndroidX `SplashScreen`（`androidx.core:core-splashscreen`），启动即显示品牌图标 + 背景色 |
| 图标尺寸 | 图标区域 **288×288 dp**（窗口内 ~2/9 高度），核心图形缩至 2/3 安全区 |
| 当前状态 | `Theme.PDIG` 起屏（占位）；品牌确定后升级 |
| 背景 | 与品牌背景色一致；深色模式配深色背景 |

---

## 4. Feature Graphic（Play 商店）

| 项 | 规格 |
|----|------|
| 尺寸 | **1024×500 px**（Play 硬性要求） |
| 安全区 | 核心内容居中，四周留 ≥10% 边距；上下各 ~80px 不被裁 |
| 格式 | PNG 或 JPG；无透明（Play 显示于白色卡片背景） |
| 文案 | 可含品牌名 + 一句价值主张；**不得**含真实数据/截图细节 |

---

## 5. Play Screenshots（商店截图）

| 项 | 规格 |
|----|------|
| 尺寸 | 手机 **1080×1920 px**（推荐 4–8 张）；可选 1440×2560 |
| 安全区 | 顶部避开状态栏、底部避开系统手势条；文字不贴近边缘 |
| 格式 | PNG（无 alpha 要求，但建议不透明） |
| 内容 | 真实界面截图（来自 AVD，合成数据）；shot list 见 `ANDROID_SCREENSHOT_SHOT_LIST.md` |
| 禁止 | 真实账单 / 卡号 / 手机号 / 内部 ID / debug 信息 |

---

## 6. Small icon / Notification（如需要）

| 项 | 规格 |
|----|------|
| notification small icon | 24×24 dp 单色（alpha channel），`ic_notification.xml`（VectorDrawable） |
| 用途 | 系统通知（当前产品无主动通知，预留） |

---

## 7. Material / Play 约束

- **对比度**：图标在浅色/深色桌面、浅/深主题下均清晰（建议视觉核查两种背景）。
- **小尺寸可读**：48px 下图形仍可辨识（避免过多细线）。
- **Alpha**：透明区域仅允许 PNG；adaptive 前景允许透明，背景建议不透明。
- **无裁剪意外**：OEM 遮罩（圆形/圆角方形/泪滴）裁切测试（AVD + 真机启动器）。
- **合规**：不得出现与银行/金融机构官方合作暗示（ANDROID_STORE_METADATA.md §12 红线）。

---

## 8. 落地路径表

| 资产 | 路径 |
|------|------|
| adaptive icon | `android/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` + `mipmap-*/ic_launcher_foreground/background` |
| monochrome | `android/app/src/main/res/mipmap-anydpi-v33/ic_monochrome.xml` |
| legacy | `mipmap-mdpi…xxxhdpi/ic_launcher.png` |
| splash | `values/themes.xml`（Theme.PDIG.Splash）+ `drawable/splash_icon.*` |
| feature graphic | `store/assets/feature-graphic.png`（1024×500） |
| screenshots | `store/assets/screenshots/*.png`（1080×1920） |
| notification | `drawable/ic_notification.xml` |

---

## 9. 状态

```text
ANDROID_BRAND_ASSET = NOT_FINAL_BRAND_ASSET（占位级；等待用户品牌决策 R-2）
  需要的用户输入：最终 app 名称 + 图标素材（前景/背景/monochrome）+ splash + feature graphic + 截图批准
  阻塞类别：FINAL_BRAND_REQUIRED（+ 关联 STORE_ACCOUNT_REQUIRED / PUBLIC_URL_REQUIRED）
```

## 配套文档

- `ANDROID_BRAND_ASSET_REQUIREMENTS.md`（需求清单，本文件为规格化）
- `ANDROID_SCREENSHOT_SHOT_LIST.md`（截图 shot list）
- `ANDROID_STORE_METADATA.md`（商店文案）
- `ANDROID_RELEASE_IDENTITY_DECISION.md`（R-2 名称决策）
