# ANDROID_BRAND_ASSET_REQUIREMENTS.md

> 生成时间：2026-09-20（Android Product Finalization）
> 状态：**NOT_FINAL_BRAND_ASSET** —— 当前 launcher icon / splash / app name 均为占位，
> 最终品牌由用户决策（B14/B15/B16），本文件只定义需求，不替用户决定品牌。

---

## 1. 当前占位情况（如实记录）

| 资产 | 当前实现 | 性质 |
|------|----------|------|
| launcher icon | `res/drawable/ic_launcher`（占位） | 占位，非最终品牌 |
| adaptive icon | 未配置 foreground/background 分层 | 缺失（Android 8+ 推荐） |
| monochrome icon | 未配置 | 缺失（Android 13+ 主题图标可选） |
| splash | 未配置 `Theme.SplashScreen`（用 `Theme.PDIG` 起屏） | 占位级 |
| app name（桌面） | `PDIG` / `个人数字基础设施图谱` | 占位（内部代号 → 用户决策 R-1） |

---

## 2. 最终品牌资产需求清单（用户决定后提供）

### 2.1 Launcher Icon（自适应）

| 项 | 规格 | 说明 |
|----|------|------|
| 前景层 | 108×108 dp（安全区约 66×66 dp） | PNG / Vector，透明背景 |
| 背景层 | 108×108 dp 纯色或图形 | 与前景对比安全 |
| 旧版 `mipmap` | 48/72/96/144/192 px | 兼容 <API26（minSdk=26，适配 i 一整套以备旧设备） |
| monochrome | 单色版本（API33+） | 系统主题图标 |

落地路径：`android/app/src/main/res/mipmap-*/` + `mipmap-anydpi-v26/ic_launcher.xml`（adaptive）+ `mipmap-anydpi-v33/ic_monochrome.xml`。

### 2.2 Splash

- 推荐 AndroidX `SplashScreen`（`androidx.core:core-splashscreen`），启动即显示品牌图标 + 背景色。
- 目前 `Theme.PDIG` 起屏已可；品牌确定后升级为正式 splash。

### 2.3 App 名称

- `res/values/strings.xml`：`app_name` / `app_name_full` —— 用户决定 R-1。

### 2.4 Store 素材（关联 ANDROID_STORE_METADATA.md）

| 素材 | 规格 |
|------|------|
| Feature graphic | 1024×500 |
| 商店截图 | 见 ANDROID_STORE_METADATA.md §8（≥2 张，推荐 4–8 张） |

---

## 3. 约束（不得违反）

- 素材不得包含真实账单 / 真实卡号 / 真实手机号。
- 品牌名不得暗示与银行/金融机构官方合作（合规红线，见 ANDROID_STORE_METADATA.md §12）。
- 图标须在两个极端（浅色/深色主题、浅/深色桌面）下可见。

---

## 4. 结论

```
ANDROID_BRAND_ASSET = NOT_FINAL_BRAND_ASSET（占位级，等待用户品牌决策）
  需要的用户输入：最终 app 名称（R-1）+ 图标素材（前景/背景/monochrome）+ splash + feature graphic
```