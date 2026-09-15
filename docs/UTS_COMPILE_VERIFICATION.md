# UTS_COMPILE_VERIFICATION.md — UTS 三端编译验证

> 日期：2026-09-15（本轮实测）
> 目的：解决一个长期悬空的问题 —— **`app/uni_modules/*/utssdk/**` 的 UTS 实现从未被任何真实编译器验证过**。
> 结论：**15/15 全部编译通过**（5 个插件 × Android/iOS/HarmonyOS），并借此查出 2 个真实语法错误。

---

## 1. 为什么以前做不到，现在可以

旧口径（`BLOCKERS.md` B10）认为 UTS 编译器只存在于 HBuilderX 的 App 打包流程里，因此
「无账号 / 无 GUI ⇒ 无法验证 UTS」。这一前提**不完整**：

DCloud 把 UTS 编译器（Rust 实现 `uts_bundler`，通过 napi 暴露给 Node）**公开发布在 npm 上**，
包含 Windows x64 原生二进制，可以脱离 HBuilderX 独立运行：

| 包                             | 版本                           | 作用                               |
| ------------------------------ | ------------------------------ | ---------------------------------- |
| `@dcloudio/uts`                | `3.0.0-alpha-5020620260914001` | 编译器 JS 层 + CLI（`bin/uts.js`） |
| `@dcloudio/uts-win32-x64-msvc` | `3.0.0-alpha-5020620260914001` | Windows x64 原生二进制             |

导出能力（`dist/api.d.ts`）：`toKotlin` / `toSwift` / **`toArkTS`** / `toCpp`，以及 `bundleKotlin`
/ `bundleSwift` / `bundleArkTS`。

> 注意：`bin/uts.js` 这个 CLI **只暴露 kotlin / swift**（`--swift` 开关），ArkTS 需要走 API。
> 本仓库的 `core/scripts/check-uts.mjs` 直接调用 API，因此三端都能覆盖。

### 1.1 一次性安装

```bash
cd <repo>/.tmp_audit/uts-cli          # 任意 ASCII 目录；.tmp_audit/ 已 gitignore
npm i @dcloudio/uts@3.0.0-alpha-5020620260914001 \
      @dcloudio/uts-win32-x64-msvc@3.0.0-alpha-5020620260914001
```

## 2. 运行

```bash
cd core && npm run check:uts
```

- 未找到编译器时输出 `SKIPPED` 并以 **0** 退出（干净克隆不会被卡住）；
- 找到编译器时，任何失败都以 **1** 退出并打印具体诊断。
- 可用 `DEPMAP_UTS_COMPILER=<path to @dcloudio/uts>` 指定其它位置。

## 3. 本轮实测结果（15/15 PASS）

| 插件                     | app-android → Kotlin | app-ios → Swift | app-harmony → ArkTS |
| ------------------------ | -------------------- | --------------- | ------------------- |
| `depmap-biometric`       | PASS                 | PASS            | PASS                |
| `depmap-file-crypto`     | PASS                 | PASS            | PASS                |
| `depmap-privacy-screen`  | PASS                 | PASS            | PASS                |
| `depmap-secure-database` | PASS                 | PASS            | PASS                |
| `depmap-secure-key`      | PASS                 | PASS            | PASS                |

复现命令（单文件等价形式）：

```bash
node .tmp_audit/uts-cli/node_modules/@dcloudio/uts/bin/uts.js \
  app/uni_modules/depmap-secure-key/utssdk/app-android/index.uts \
  --out /tmp/out.kt --package com.depmap.plugins.securekey
```

## 4. 本轮查出的真实缺陷（前序所有轮次都未检查 `app-ios`）

| #             | 文件                                              | 编译器诊断                               | 处置                                                                   |
| ------------- | ------------------------------------------------- | ---------------------------------------- | ---------------------------------------------------------------------- |
| **IOS-UTS-1** | `depmap-secure-key/utssdk/app-ios/index.uts`      | `x Expected '{', got 'resolve'` @ `6:13` | 去掉非法的 `do { try ... }` 包裹                                       |
| **IOS-UTS-2** | `depmap-secure-database/utssdk/app-ios/index.uts` | `x Expected '{', got 'this'` @ `21:9`    | 同上；并修正 `migrate(migrations: DepmapSchemaV1.shared.migrations())` |

**根因**：`do { try ... } catch (e) { ... }` —— `do` 块**不是 TypeScript/UTS 的语句形式**
（TS 只有 `do...while`）。原作者按 Swift/Java 习惯书写，编译器直接拒绝。

**同时修正的引用错误**：`DepmapSchemaV1` / `shared.migrations()` 在 `platforms/ios/swift/` 下
**并不存在**（iOS 侧没有 schema 文件，见 `docs/IOS_RELEASE_HANDOFF.md` §3 的 G-1/G-2）；
Swift 适配器真实签名是 `migrate(migrations: [(version: Int, statements: [String])]) throws -> Int`
（`SQLCipherSecureDatabaseAdapter.swift:75`），迁移语句须由 UTS 层注入。原写法还使用了
Swift 的实参标签语法 `migrations:`，在 UTS 中同样非法。

## 5. 这个门禁**证明了什么、没证明什么**（必须严格区分）

**证明了**

- UTS 源码在语法与结构上是合法的，且能正确降级为目标语言；
- 三个平台的 UTS 实现都能被官方编译器接受（`app-harmony` 的 ArkTS 降级同样通过）；
- 降级产物是**有实质内容的代码**，不是空壳 —— 例如 `depmap-secure-database` 生成的 Kotlin
  真实包含 `UtsSecurityBridge.dbOpen/dbClose/dbMigrate/dbQuery/dbExecute/dbTransaction` 的
  回调式调用与 `UTSPromise` 包装。

**没有证明**（不得据此声称）

- **跨语言符号可解析**：本门禁按 `removeImports: true` 运行，`com.depmap.core.*` 的
  Kotlin/Swift/ArkTS 实体并未参与解析 —— UTS 调用点与宿主语言签名是否匹配，仍需
  HBuilderX（Android/Harmony）与 Xcode（iOS）的真实构建；
- **`.uvue` 可编译**：本门禁只覆盖 `utssdk/**`，不覆盖 24 个页面；
- **可运行**：不涉及任何运行时行为。

因此状态名保持分层：
`UTS_SOURCE_READY = PASS`、`UTS_DOWNLEVEL_COMPILED = PASS（15/15）`、
`UTS_HOST_LINKED = NOT_RUN`（需 B10 / macOS）。
