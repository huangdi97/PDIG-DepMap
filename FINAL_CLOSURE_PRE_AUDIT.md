# FINAL_CLOSURE_PRE_AUDIT.md

> PDIG / DepMap — FINAL PRODUCTION CLOSURE V1，PHASE A 现场恢复记录。
> **本文件所有数字均为本轮实测**（2026-09-14），不复制旧报告。
> 采集命令与原始日志见文末「证据索引」。

---

## 1. Git 现场

| 项               | 实测值                                                                                          |
| ---------------- | ----------------------------------------------------------------------------------------------- |
| HEAD（进入时）   | `4af5b6945237cd9d5c0208aedb7dfc5c5cfb19f5`                                                      |
| Branch           | `feat/mvp03-living-graph`                                                                       |
| Tags             | `v0.2.0-mvp02`、`v0.3.0-mvp03`                                                                  |
| 进入时工作区     | **clean**（`git status --short` 空输出）                                                        |
| 进入时 untracked | 无                                                                                              |
| 保护动作         | 未执行 `reset --hard` / `clean -fd` / `checkout .` / `restore .` / force push / history rewrite |

结论：无未提交工作需要保全；本轮所有改动均为本轮产生。

---

## 2. 工具链实测（决定哪些 Gate 可执行）

| 工具           | 实测结果                                                                                                                                            | 影响                                                          |
| -------------- | --------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------- |
| Node           | `v22.22.2`                                                                                                                                          | Core 可执行                                                   |
| npm            | `10.9.7`                                                                                                                                            | 与 `packageManager: npm@11.3.0` **不一致**（见 §11 发现 F-3） |
| HBuilderX      | **未安装**（`C:`/`D:`/`E:` 及 AppData 全路径搜索无命中）                                                                                            | B10 仍成立                                                    |
| JDK            | `java 1.8.0_441`（PATH）；**JDK 17.0.12**（DevEco JBR）；**JDK 21.0.10**（Android Studio JBR）                                                      | B1 前提被推翻（见 §4）                                        |
| Android SDK    | **存在** `<ANDROID_SDK_ROOT>`（platforms `android-36.1`/`android-37.0`、build-tools `36.1.0`/`37.0.0`、cmdline-tools/latest、licenses 全部已接受） | B1 前提被推翻                                                 |
| Android Studio | **存在** `<ANDROID_STUDIO_HOME>`（build `AI-253.32098.37.2534.15232325`）                                                                          | —                                                             |
| Gradle         | 仅 `~/.gradle/wrapper/dists/gradle-9.3.1-bin/` 内 **0 字节 `.part`/`.lck`**（下载未完成）                                                           | 无可用 Gradle 发行版                                          |
| adb            | `<ANDROID_SDK_ROOT>\adb.exe` 可用；`adb devices` → **无设备**                                                                                               | 真机 Gate 不可执行                                            |
| DevEco Studio  | **存在** `<DEVECO_HOME>`，version `5.0.5.310`                                                                                       | B2 前提被推翻（见 §5）                                        |
| HarmonyOS SDK  | **存在** `sdk/default/{openharmony,hms}`，`apiVersion 13`，`version 5.0.1.115`，`metaVersion 3.0.0`                                                 | —                                                             |
| hvigor         | `5.13.2`（DevEco 内置，`tools/hvigor/hvigor`）+ `@ohos/hvigor-ohos-plugin 5.13.2`                                                                   | 可执行（实测，见 §5）                                         |
| ohpm           | `5.0.10`（实测 `pm-cli.js -v`）；registry `https://ohpm.openharmony.cn/ohpm/`                                                                       | 可执行                                                        |
| macOS / Xcode  | 无（`uname -s` = `MINGW64_NT-10.0-26200`；`xcodebuild` 不存在）                                                                                     | B3 成立（预期内）                                             |
| 构建仓库网络   | `repo1.maven.org` HTTP 200；`services.gradle.org` HTTP 200；`ohpm.openharmony.cn` 可达；**但 Gradle 发行包 CDN 下载 `curl` exit 7（连接失败）**     | Android/Harmony 真实构建受阻（见 §4/§5）                      |

---

## 3. 工程配置实测

| 项                   | 实测值                                                                                                                                                                                                                                                                 |
| -------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| package manager 声明 | `npm@11.3.0`（`core/package.json`）                                                                                                                                                                                                                                    |
| lockfile             | `core/package-lock.json`，`lockfileVersion: 3`，与 manifest 同步（`check:deps` PASS）                                                                                                                                                                                  |
| TypeScript           | `^5.8.0`                                                                                                                                                                                                                                                               |
| test runner          | `vitest ^3.1.0`（`pool: forks`，`environment: node`，`NODE_NO_WARNINGS=1`）                                                                                                                                                                                            |
| 覆盖率 provider      | `@vitest/coverage-v8 ^3.2.7`，`include: src/**/*.ts`，排除 `adapters/interfaces.ts`、`db/driver.ts`                                                                                                                                                                    |
| property harness     | `fast-check ^4.10.0`                                                                                                                                                                                                                                                   |
| lint                 | `eslint ^10.10.0` + `typescript-eslint ^8.70.0`                                                                                                                                                                                                                        |
| formatter            | `prettier ^3.9.6`                                                                                                                                                                                                                                                      |
| mutation             | `core/stryker.conf.mjs`（mutate: `src/impact/kernel.ts`、`src/domain/relation-registry.ts`；stryker **未在 devDependencies**，按注释为 `--no-save` 一次性安装）                                                                                                        |
| tsconfig             | `strict`、`noUncheckedIndexedAccess`、`noImplicitAny`、`noImplicitReturns`、`noImplicitOverride`、`noFallthroughCasesInSwitch`、`exactOptionalPropertyTypes`、`verbatimModuleSyntax`、`isolatedModules` 全部 `true`；`useUnknownInCatchVariables` 由 `strict` 隐含开启 |
| `.editorconfig`      | root=true，UTF-8，LF，final newline，trim trailing（`*.md` 例外），`kt/kts/swift` 4 空格，`fixtures/*` charset/eol **unset**（保原始字节）                                                                                                                             |
| `.gitattributes`     | `* text=auto eol=lf`；`*.bat/*.cmd` CRLF；`core/tests/fixtures/* -text`；平台源码 LF                                                                                                                                                                                   |
| `.gitignore`         | 依赖/构建/日志/密钥/签名/IDE/本地私密数据/平台产物/测试临时/变异产物 —— 本轮**新增** Gradle 本地状态条目（见 §11 F-4）                                                                                                                                                 |
| 根 prettier 配置     | 本轮**新增** `.prettierrc.json`（见 §11 F-1）                                                                                                                                                                                                                          |

---

## 4. Android 现状（B1 前提修正）

**实测存在**：

- Android SDK `<ANDROID_SDK_ROOT>`：`platforms/{android-36.1, android-37.0}`、`build-tools/{36.1.0, 37.0.0}`、`cmdline-tools/latest`（含 `sdkmanager.bat`）、`emulator`、`sources`、`licenses/`（7 个 license 哈希文件均已存在 → **license 已接受**）
- JDK 17.0.12（`<DEVECO_HOME>\jbr\bin\javac.exe` → `javac 17.0.12`）
- JDK 21.0.10（`<ANDROID_STUDIO_HOME>\jbr\bin\java.exe`）
- adb 可用

**实测缺失/阻塞**：

- **无可用 Gradle 发行版**：`~/.gradle/wrapper/dists/gradle-9.3.1-bin/` 仅 0 字节 `.part`/`.lck`；工程 `platforms/android/` 内**无 `gradlew`/`gradle-wrapper.properties`**
- **构建依赖无法获取**：`curl https://services.gradle.org/distributions/gradle-8.9-bin.zip` → exit 7；`https://downloads.gradle.org/...` → exit 7（沙箱内 CDN 不可达）。AGP 8.5.2 / Kotlin 2.0.0 / androidx / SQLCipher / BouncyCastle 均需联网解析
- `platforms/android/core/build.gradle.kts` 声明 `compileSdk = 34`，但已装 platform 为 36.1/37.0（34 未安装）
- 无真机（`adb devices` 空）
- 无 release keystore

**结论**：`ANDROID_SOURCE_READY = PASS`；`ANDROID_COMPILED / BUILD_READY = BLOCKED`（构建期制品获取）；`DEVICE_VERIFIED / SIGNING_READY / STORE_READY = BLOCKED`。

---

## 5. HarmonyOS 现状（B2 前提修正，且已真实执行构建）

**实测存在**：

- DevEco Studio `5.0.5.310`（`<DEVECO_HOME>`）
- SDK：`sdk/default/openharmony/{ets,js,native,previewer,toolchains}` + `sdk/default/hms/*`；`ets/oh-uni-package.json` → `apiVersion "13"`, `version "5.0.1.115"`, `metaVersion "3.0.0"`
- hvigor 引擎 `5.13.2` + `hvigor-ohos-plugin 5.13.2`（本地，含 `node_modules`）
- `hvigorw.js`、`ohpm 5.0.10` 可执行
- 工程模板齐备（`plugins/{openharmony,harmony}/lib/templates/**`，含 `hvigor-config.json5` / `build-profile.json5` / `hvigorfile.ts` / `oh-package.json5` 模板）
- 内置 node `v18.20.1`

**已真实执行（非仅检测存在）**：
在临时目录构造最小 Stage-model 工程（AppScope / build-profile / hvigorfile / oh-package / entry 模块 / EntryAbility.ets / Index.ets / resources），调用
`node ".../tools/hvigor/bin/hvigorw.js" assembleHap --mode module -p product=default -p buildMode=debug --no-daemon`。

实测演进（4 次）：

1. `hvigor ERROR: Please configure compileSdkVersion in the product.` → 补 `compileSdkVersion: 13`
2. `hvigor ERROR: Unsupported modelVersion of Hvigor 5.0.5. Detail: The supported Hvigor modelVersion is 5.0.2` → 改 `modelVersion: "5.0.2"`
3. **hvigor 成功引导 pnpm（`Pnpm install success.`）并解析工程模型** → `hvigor ERROR: Unable to find the following components: toolchains:13 / ArkTS:13 / js:13 / native:13 / previewer:13`，hvigor 自带修复建议：_Go to File > Settings > OpenHarmony SDK, download the components, and sync the project_
4. 调整 `sdk.dir` 至 `sdk\default` 后仍为同一组件解析错误

**结论**：工具链**可执行**且工程模型可解析；`HARMONY_SOURCE_READY = PASS`；`HARMONY_COMPILED / BUILD_READY = BLOCKED`。
阻塞根因：SDK 组件（API 13）未被 hvigor 本地组件加载器识别，需 DevEco **SDK Manager 图形化同步**（`SUPPORT_META_VERSION = '3.0.0'` 已匹配、`_MAX_SCAN_DEPTH = 5` 足够，判定为 SDK 注册/位置约定问题）。
另注：`platforms/harmonyos/` 本身是**适配器片段**（仅 `app.json5` + 1 个 `.ets` + `module.json5`），并非可构建的 Stage 工程；且产品 HarmonyOS 产物由 HBuilderX（uni-app x）产出，非 DevEco 直接产出。

---

## 6. UI / 前端现状

| 项           | 实测值                                                                                                                                         |
| ------------ | ---------------------------------------------------------------------------------------------------------------------------------------------- |
| `.uvue` 文件 | 30                                                                                                                                             |
| 页面         | 24（`pages.json` 声明 24 条，全部存在）                                                                                                        |
| 组件         | 5（`dp-button` / `dp-card` / `dp-chip` / `dp-row` / `dp-state`）                                                                               |
| 设计 token   | `app/theme/tokens.uts`，调色板 34 色                                                                                                           |
| 服务边界     | `app/services/depmap-service.uts`（唯一数据边界）、`app/services/rules.uts`（纯规则）                                                          |
| 状态         | `app/stores/app-state.uts`                                                                                                                     |
| 一级导航     | tabBar 4 项（首页 / 场景 / 计划 / 我的）                                                                                                       |
| UI 静态门禁  | `core/scripts/check-ui.mjs` U1–U9，本轮 PASS                                                                                                   |
| 原生桥接     | `platforms/android/kotlin/com/depmap/core/**`（5 个 .kt + 1 个 golden test）、`platforms/harmonyos/arkts/*.ets`、`platforms/ios/swift/*.swift` |

U1–U9 覆盖：页面存在性 / tabBar 完整性 / **用户可见文案禁工程词** / **页面禁止直连 SQLite** / **颜色必须来自 token** / 未声明标识符 / `dp-*` 组件存在性 / **每页必须有空态或错误态或加载态** / `v-for` 字段名正确性。

---

## 7. 测试与质量基线（实测）

| Gate                 | 结果                                                                               |
| -------------------- | ---------------------------------------------------------------------------------- |
| `npm run check`      | **EXIT=0**                                                                         |
| `npm run check:full` | **EXIT=0**                                                                         |
| 测试                 | **453 passed / 453**，**43 文件**                                                  |
| 覆盖率               | Statements **93.82%**、Branches **82.22%**、Functions **94.55%**、Lines **93.82%** |
| 稳定性               | `test:stability` 3 连跑全绿（EXIT=0）                                              |
| critical 套件        | impact+invariants+contract+property **×10 连跑全绿**（74 tests/run）               |
| architecture         | PASS（48 files，circular = 0）                                                     |
| network              | PASS（118 business source files，0 网络原语）                                      |
| secrets              | PASS（404 files，0 production secrets）                                            |
| UI static            | PASS（30 `.uvue`，24 pages，5 components）                                         |
| db-integrity         | 6 passed                                                                           |
| perf                 | 16 passed                                                                          |
| deps                 | PASS（audit 3 moderate / 0 low，dev-only；license 快照 MIT/Apache-2.0）            |

测试文件分布：`contract 2`、`crypto 2`、`domain 1`、`impact 2`、`integration 4`、`invariants 2`、`parser 1`、`perf 3`、`property 3`、`repository 6`、`services 8`、`sources 2`、`unit 7`。fixtures 30 个。

---

## 8. 版本矩阵（实测）

| 项                                         | 值                                                                      |
| ------------------------------------------ | ----------------------------------------------------------------------- |
| schemaVersion                              | **3**（`core/src/schema/migrations.ts:10`）                             |
| depmap formatVersion                       | **1**（`core/src/crypto/depmap.ts:18`，`DEPMAP_FORMAT_VERSION`）        |
| core 包版本                                | `0.1.0`                                                                 |
| App versionName / versionCode              | `0.1.0` / `1`（`app/manifest.json`）                                    |
| App appid                                  | `__UNI__DEPMAP01`                                                       |
| uni-app x compatible                       | `4.41`                                                                  |
| Android minSdk / targetSdk / ABI           | `26` / `34` / `arm64-v8a`（仅）                                         |
| Android 权限（manifest）                   | `[]`（空）                                                              |
| HarmonyOS minAPIVersion / targetAPIVersion | `12` / `12`                                                             |
| iOS deployment target                      | `iOS 14+` / `macOS 12+`（`Package.swift`）                              |
| Golden vector                              | `core/src/crypto/golden.ts` + `core/scripts/generate-golden.ts`（冻结） |

---

## 9. 平台 Blocker（实测口径）

| #               | 旧口径                                  | **本轮实测修正口径**                                                                                                                                                                                        |
| --------------- | --------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| B1              | 无 JDK17 / Android SDK / Gradle         | **部分不成立**：JDK 17.0.12 + 21.0.10 与 Android SDK 均已安装、license 已接受；实际阻塞 = 无可用 Gradle 发行版 + 构建依赖 CDN 不可达 + `compileSdk 34` 未安装 + 无真机 + 无 keystore                        |
| B2              | 无 DevEco Studio / HarmonyOS SDK        | **不成立**：DevEco 5.0.5.310 + SDK API 13 + hvigor 5.13.2 + ohpm 5.0.10 均存在且可执行；实际阻塞 = SDK 组件（API 13）需 DevEco SDK Manager 图形化同步；且 `platforms/harmonyos/` 为适配器片段而非可构建工程 |
| B3              | 无 macOS / Xcode                        | **成立**（Windows 环境，预期内）                                                                                                                                                                            |
| B10             | 无 HBuilderX / uni-app x 工具链         | **成立**（全盘搜索无命中）—— 仍为产品级关键阻塞                                                                                                                                                             |
| B20 / B21 / B22 | App 端解析 / 加解密 / Core→UTS 桥接缺失 | **成立**（解除依赖 B10）                                                                                                                                                                                    |

---

## 10. 发布材料现状（实测）

| 项            | 实测                                                                                                                                       |
| ------------- | ------------------------------------------------------------------------------------------------------------------------------------------ |
| `assets/`     | 仅 `README.md`，**无任何 PNG**（图标 / 启动图未提供）                                                                                      |
| `app/static/` | **不存在** —— 但 `app/manifest.json` 引用了 `static/icons/*.png`（4 档）与 `static/splash/*.png`（5 档）→ **路径与资产不一致（发现 F-2）** |
| 商店元数据    | `store/{PLATFORM_REQUIREMENTS, PRIVACY_DISCLOSURE_MATRIX, RELEASE_NOTES, SCREENSHOT_PLAN, STORE_LISTING_ZH}.md` 存在                       |
| 隐私政策      | `docs/PRIVACY_POLICY_DRAFT.md`（草稿）；**无正式 URL**                                                                                     |
| 支持 URL      | 无                                                                                                                                         |
| 品牌名        | 工作名「个人数字依赖图」（`appid __UNI__DEPMAP01` 为占位）                                                                                 |
| 签名材料      | 无（`signing/` 已 gitignore）                                                                                                              |
| Real Data     | `REAL_DATA_VALIDATED = NOT_RUN`（`local_private/` 仅含日志与脚本，无真实账单）                                                             |

---

## 11. 本轮 PRE_AUDIT 发现（待 PHASE B+ 处置）

| #   | 发现                                                                                             | 严重度 | 处置                                                                                           |
| --- | ------------------------------------------------------------------------------------------------ | ------ | ---------------------------------------------------------------------------------------------- |
| F-1 | `format:check` 仅覆盖 `core/`，根目录 / `docs/` / `store/` / `.codebuddy` 的 Markdown 无格式门禁 | 中     | **本轮已修**：新增 `.prettierrc.json` + `format:docs` / `format:docs:check`，并纳入 `check` 链 |
| F-2 | `app/manifest.json` 引用不存在的 `static/icons/*.png` 与 `static/splash/*.png`                   | 中     | 记录为 STORE_ASSETS blocker（B15/B16）；**不伪造占位图标**                                     |
| F-3 | `packageManager: npm@11.3.0` 与实际 `npm 10.9.7` 不一致                                          | 低     | 记录；不擅自改（可能影响 corepack 行为）                                                       |
| F-4 | `.gitignore` 缺 Gradle/Android 本地状态条目                                                      | 低     | **本轮已修**：新增 `.gradle/`、`local.properties`、`.kotlin/`、`*.hprof`、`captures/`          |
| F-5 | 3 个零消费者导出（`planEffectiveStatus`、`ObservationFingerprintRecord`、`ParsedCsvCell`）       | 低     | **本轮已修**：删除                                                                             |
| F-6 | 4 个文件 18 处低信息命名 `obj`                                                                   | 低     | **本轮已修**：重命名为 `record`                                                                |

---

## 12. 证据索引

| 证据                          | 位置                                                                                                         |
| ----------------------------- | ------------------------------------------------------------------------------------------------------------ |
| 快速 Gate 全量日志（本轮）    | `/tmp/check-after-fixes.log`（`CHECK_EXIT=0`）                                                               |
| `check:full` 全量日志（本轮） | `/tmp/check-full-final.log`（`FULL_EXIT=0`）                                                                 |
| 稳定性 3 连跑日志             | `/tmp/stability-final.log`（`STABILITY_EXIT=0`）                                                             |
| critical 10 连跑日志          | `/tmp/critical10.log`（`RUN1..RUN10_EXIT=0`）                                                                |
| hvigor 构建实测日志           | 临时目录 `hvigor-run{,2,3,4,5}.log`（探测后已清理临时工程）                                                  |
| docs 格式化幂等验证           | `npm run format:docs:check` → EXIT=0；`git diff -w` 残余分类：473 表格分隔行 + 487 空行 + 7 处渲染等价规范化 |

---

## 13. 结论

- Core / 工程 / 测试 / 安全 / UI 源码侧基线**可复现且全绿**（453 tests、check / check:full EXIT=0）。
- 平台侧：**B1 / B2 的既有记录与事实不符**，本轮已修正；Android 与 HarmonyOS 的**源码与工具链可用性**提升为 PASS，但**真实构建仍未完成**（各自精确根因见 §4/§5）。
- 产品级关键阻塞仍为 **B10（HBuilderX）**，以及由它派生的 **B20/B21/B22**。
- Real Data 仍为 `NOT_RUN`；`STORE_SUBMITTED = NO`。
