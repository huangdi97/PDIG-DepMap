# assets/ — 静态资产目录

当前状态：**空**（仅本 README）。正式图标与启动图**尚未提供**。

规格见 `docs/APP_ICON_ASSET_SPEC.md`。

## 目录约定

```
assets/
  icon/
    android/     Adaptive icon 前景/背景、legacy 各密度、Play 512
    ios/         AppIcon-1024.png（无 alpha）
    harmony/     icon_foreground / icon_background
  splash/
    android/  ios/  harmony/
```

## 入库规则

- 只放**最终**资产，不放草稿 / 测试图标。
- 二进制资产提交前确认：体积合理、license 明确、无第三方商标。
- 与设计系统主色 `#4C4FD8` 保持一致（见 `app/theme/tokens.uts`）。
- 不得使用远程 CDN 加载图标（`check:network` 会拦截）。

## 阻塞

`STORE_ASSETS_READY = BLOCKED`：等待图标 / 启动图 / 商店截图。
见 `STORE_EXTERNAL_BLOCKERS.md` B15–B17。
