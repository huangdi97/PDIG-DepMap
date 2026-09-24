# GITHUB_REPOSITORY_SIZE_AUDIT.md

> 仓库体积与大文件审计（A4）。
> 生成时间：2026-09-17（Asia/Shanghai）
> 原则：build artifact 不入 repo；canonical fixture 合理小则走普通 Git；**不把整个 repo 放进 LFS**。

---

## 0. 一句话结论

> 受控内容合计 **4.54 MB**，`.git` 对象库 **4.22 MB**，**最大受控文件 573.3 KB**。
> **没有任何受控文件超过 1 MB ⇒ 本仓库不需要 Git LFS。**

---

## 1. 受控内容（tracked）

| 指标         | 值                        |
| ------------ | ------------------------- |
| 受控文件数   | **706**                   |
| 受控内容合计 | **4,757,733 B = 4.54 MB** |
| > 100 KB     | 2                         |
| > 200 KB     | 1                         |
| > 500 KB     | 1                         |
| **> 1 MB**   | **0**                     |
| > 10 MB      | 0                         |
| > 50 MB      | 0                         |
| > 100 MB     | 0                         |

### 1.1 最大的 10 个受控文件

| 大小     | 路径                                                            | 性质                                                        |
| -------- | --------------------------------------------------------------- | ----------------------------------------------------------- |
| 573.3 KB | `platforms/harmonyos/hvigorw.js`                                | Legacy hvigor wrapper（第三方脚本，**随 Legacy 冻结入库**） |
| 124.0 KB | `core/package-lock.json`                                        | 依赖锁文件                                                  |
| 70.7 KB  | `platforms/android/artifacts/core-debug.aar`                    | Legacy AAR 取证产物                                         |
| 70.2 KB  | `ANDROID_N1_N2_FINAL_CLOSURE_REPORT_V2.md`                      | 工程报告                                                    |
| 67.1 KB  | `platforms/android/artifacts/core-release.aar`                  | Legacy AAR 取证产物                                         |
| 58.5 KB  | `FINAL_PRODUCTION_CLOSURE_REPORT.md`                            | 工程报告                                                    |
| 56.4 KB  | `core/scripts/generate-conformance.ts`                          | Oracle 生成脚本                                             |
| 55.6 KB  | `CANONICAL_DESIGN.md`                                           | Canonical 设计母版                                          |
| 55.5 KB  | `app/services/depmap-service.uts`                               | Legacy UTS 源码                                             |
| 54.2 KB  | `android/app/src/main/kotlin/com/pdig/app/data/AppContainer.kt` | Android 源码                                                |

### 1.2 按顶层目录

| 目录                                 | 受控体积   |
| ------------------------------------ | ---------- |
| （根目录状态/报告）                  | 1,092.3 KB |
| `core/`                              | 1,001.4 KB |
| `platforms/`                         | 806.4 KB   |
| `android/`                           | 649.0 KB   |
| `docs/`                              | 401.7 KB   |
| `app/`                               | 284.6 KB   |
| `fixtures/`                          | 165.5 KB   |
| `spec/`                              | 109.1 KB   |
| `tools/`                             | 37.4 KB    |
| `conformance/`                       | 34.9 KB    |
| `harmony/`                           | 22.0 KB    |
| `store/` / `ios/` / `legacy/` / 其他 | < 20 KB    |

---

## 2. Git 对象库

| 指标                | 值                               |
| ------------------- | -------------------------------- |
| `.git` 目录         | **4.22 MB**（1,220 个文件）      |
| loose objects       | 1,173 个 / 2,894 KB              |
| pack                | 2 个 / 1,212 KB（in-pack 1,324） |
| 可达 commit         | 124                              |
| 可达 blob（含路径） | 1,830 条 / 唯一 blob 1,181       |

**clone 成本极低**（总量约 9 MB 量级），无历史膨胀风险。

---

## 3. 磁盘上的大文件（**全部已忽略，不在仓库内**）

| 大小      | 路径                                                        | 状态                                     |
| --------- | ----------------------------------------------------------- | ---------------------------------------- |
| 129.81 MB | `.tmp_audit/tools/gradle-8.9-bin.zip`                       | 已忽略（`.tmp_audit/`）                  |
| 87.09 MB  | `.tmp_audit/tools/HBuilderX…zip`                            | 已忽略                                   |
| 48.15 MB  | `android/app/build/…/intermediary-bundle.aab`               | 已忽略（`build/`）                       |
| 47.38 MB  | `android/app/build/…/base.zip`                              | 已忽略                                   |
| 35.21 MB  | `android/app/build/outputs/apk/debug/app-debug.apk`         | 已忽略（`*.apk`）                        |
| 35.18 MB  | `local_private/artifacts/app-debug.apk`                     | 已忽略（`local_private/`）               |
| 19.77 MB  | `android/app/build/outputs/bundle/release/app-release.aab`  | 已忽略（`*.aab`）                        |
| 17.59 MB  | `.tmp_audit/uts-cli/node_modules/…/uts.win32-x64-msvc.node` | 已忽略                                   |
| 11.15 MB  | `core/node_modules/@esbuild/win32-x64/esbuild.exe`          | 已忽略（`node_modules/`）                |
| 11.14 MB  | `harmony/node_modules/@ohos/hvigor/…/tsserver.js`           | 已忽略（`oh_modules/`、`node_modules/`） |

磁盘合计（忽略项）：`android/` 428.58 MB · `.tmp_audit/` 242.87 MB · `core/` 134.58 MB · `local_private/` 106.85 MB · `platforms/` 4.97 MB。

**结论**：所有 APK / AAB / HAP / dex / `.so` / node_modules / 本地取证产物**均在忽略范围内**，
磁盘 900+ MB 与仓库 4.54 MB 完全分离。

---

## 4. Git LFS 判定

| 判定项                               | 结论                                                                                                |
| ------------------------------------ | --------------------------------------------------------------------------------------------------- |
| 是否存在必须长期版本化的大二进制资产 | **否**。受控二进制仅 17 个，合计 < 260 KB（图标 / 启动图 / `gradle-wrapper.jar` / 2 个 Legacy AAR） |
| 是否超过 GitHub 单文件 100 MB 硬限制 | 否（最大 573.3 KB）                                                                                 |
| 是否建议启用 LFS                     | **否**                                                                                              |
| 理由                                 | 引入 LFS 会提高 clone/协作复杂度，而当前最大文件仅 0.57 MB，收益为负                                |

> 如将来确有需要（如长期版本化真机截图集、大体量 canonical corpus），
> 应**按路径**精确启用 LFS（`git lfs track "<path>"`），**不要**用 `*.*` 之类的通配把整个仓库纳入 LFS。

---

## 5. 门禁结论

| Gate                 | 结果     |
| -------------------- | -------- |
| `GITHUB_SIZE_AUDIT`  | **PASS** |
| Git LFS 需求         | **NONE** |
| 需清理的已入库大文件 | **0**    |
