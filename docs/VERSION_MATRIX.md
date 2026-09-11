# VERSION_MATRIX.md — 版本矩阵（RC PHASE AG）

> 各版本独立记录，不混用。更新于 2026-09-12。

| 维度 | 值 | 定义位置 |
|---|---|---|
| App versionName / versionCode | 0.1.0 / 1 | app/manifest.json |
| Schema version | 1 | core/src/schema/migrations.ts（三端同源 DDL） |
| DEPMAP container version | 1（format 'depmap'） | core/src/crypto/depmap.ts；platforms/android/.../DepmapContainerV1.kt |
| DEPMAP payload version | 1（depmap-logical-graph） | core/src/services/graph-serialize.ts |
| Fingerprint version | 1 | core/src/fingerprint/fingerprint.ts |
| WeChat parser id/version | wechat / 1 | core/src/parser/wechat/parser.ts |
| Impact domain | payment（MVP） | core/src/impact/kernel.ts |
| @depmap/core | 0.1.0 | core/package.json |
| Android applicationId（占位） | com.example.depmap | STORE_RELEASE_INPUTS.md（B11） |
| Android compileSdk / minSdk | 34 / 26 | platforms/android/core/build.gradle.kts + manifest.json |
| HarmonyOS minAPI / targetAPI | 12 / 12 | platforms/harmonyos/app.json5 |
| iOS deployment target | iOS 14 | platforms/ios/Package.swift |
| Node（Core 开发/测试） | v22.15.0 | RC_PRE_AUDIT.md |
| TypeScript | 5.9.3 | core devDeps |

## 约束

- Schema/container/payload/fingerprint 任一参数变化必须升版本号（V1 冻结纪律）。
- 三端 DDL 变更必须与 core migrations.ts 同步（CROSS_PLATFORM_CONTRACT_AUDIT.md）。
