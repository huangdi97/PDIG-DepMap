# LOCAL_DOWNLOAD_AUDIT.md — 本机下载审计（Stage 0）

> 2026-09-29 · feat/pdig-ui-vnext

## 1. 结论

**UNNECESSARY_DOWNLOADS = 0**

本轮**零新增依赖、零下载**：

- Globe 使用既有 Skia/Canvas 绘制原语（Compose Desktop 1.6.11 自带），**未下载任何 3D / 地图 / WebView / 纹理包**；
- token codegen 复用既有 `tools/codegen/generate.mjs`（仅扩展 emit 函数），无新 npm 包；
- image metrics 复用 JDK 自带 ImageIO，无新 jar；
- Skills（ui-ux-pro-max / impeccable / frontend-design）为**本机已装**，未重装、未更新、未下载；
- 未执行 `npm i -g` / `winget` / `choco` / sdkmanager 安装等任何获取动作。

## 2. 下载记录表（本轮）

| # | reason | source | version | size | 判定 |
| --- | --- | --- | --- | --- | --- |
| — | （无） | — | — | — | 无任何记录 |

## 3. 依赖基线（既有，未变）

| 组件 | 版本 | 是否本轮新增 |
| --- | --- | --- |
| Compose Desktop | 1.6.11（BOM 2024.09.02） | 否 |
| Compose Multiplatform runtime（Skia） | 既有 | 否 |
| Gradle wrapper | 8.9 | 否 |
| node | v22.15.0（既有） | 否 |
| JDK | 21 Temurin（既有） | 否 |
| core/ node_modules | 既有 | 否 |

## 4. 网络策略遵守

- offline-first（DESIGN_TOKENS.json policy.offlineFirst = true）：Globe 无远程 tiles / 无 CDN / 无远程字体；
- 本轮未发起任何"为了功能而下载"的网络请求。

## 5. 记录义务

按 Goal 要求，每次下载须记录 reason/source/version/size。若后续轮次（平台 fixer / 构建链）需要恢复依赖，
按既有 `npm ci` / gradle wrapper 缓存放行（repo 已声明依赖 restore 允许），并在此文件追加记录。
