# RELEASE_CANDIDATE_MANIFEST.md — RC 产物清单

> 生成时间：2026-09-15（接力轮更新）
> 作用：列出**真实构建产出**的每一个产物，含路径、字节数、SHA-256 与构建复现命令。
>
> **本清单不构成发布候选（Release Candidate）** —— 三端均无产品级可安装包（见 §4）。
>
> **Git**：branch `feat/mvp03-living-graph`，HEAD 以 `git log --oneline -1` 为准
> （本轮基线 `ee7ee58`），**未 push**、**未打 tag**。
> **应用版本**：`versionName 0.1.0` / `versionCode 1`（`app/manifest.json`）；
> Schema **v3**；`.depmap` **formatVersion 1**；payload **v3**（v1/v2 可迁移）。

---

## 1. Android 原生库（AAR）

| 文件                                           | 字节   | SHA-256                                                            |
| ---------------------------------------------- | ------ | ------------------------------------------------------------------ |
| `platforms/android/artifacts/core-debug.aar`   | 72,376 | `dd7d8c04b23041354b401b1f0e347b9fedf586131dae975c8df70520ef850a0d` |
| `platforms/android/artifacts/core-release.aar` | 68,731 | `4ac2e7f4c5d407d6b6a2d2232913f343ff7b7ef0f27891d67c60a190bf43c32b` |

- `core-release.aar` 内含 **37 个 `.class`**，覆盖：
  `DepmapContainerV1`（含 `B64` / `JsonHeader` / `DepmapContainerException`）、`DepmapSchemaV1`、
  `SecureDatabaseAdapter` / `SecureDatabaseOpenOptions` / `SqlCipherSecureDatabaseAdapter` / `DepmapSqlCipherHelper`、
  `SecureKeyAdapter` / `KeystoreSecureKeyAdapter` / `KeystoreSecureKeyAdapterExtensionsKt`、
  `BiometricGate` / `BiometricGateAdapter` / `BiometricGateResult` / `FlagSecurePrivacyAdapter`、
  **`UtsSecurityBridge`**（13 个回调式桥接方法）。
- **性质**：Android Library。**不是 APK / AAB，不可安装。**

### 1.1 复现命令

```bash
cd platforms/android
export JAVA_HOME="<ANDROID_STUDIO_HOME>/jbr"
export ANDROID_HOME="<ANDROID_SDK_ROOT>"
"<GRADLE_HOME>/bin/gradle.bat" --no-daemon \
  assembleDebug assembleRelease collectArtifacts --rerun-tasks --console=plain
```

> **必须带 `--rerun-tasks`**。否则 Gradle build cache 会跨路径命中 `FROM-CACHE`，
> 造成「看似通过、实则未执行」的假绿（详见 `docs/ANDROID_RELEASE_RUNBOOK.md` §3）。

### 1.2 本轮复现验证（2026-09-15 接力轮）

`BUILD SUCCESSFUL in 7m 50s`，`49 actionable tasks: 49 executed`（**无 `FROM-CACHE`**），
两个 AAR 的字节数与 SHA-256 与上表**逐字节一致** ⇒ **构建可复现**。

### 1.3 伴随测试

`core/build` 测试报告：`TEST-com.depmap.core.crypto.DepmapContainerV1GoldenTest.xml`
`tests="4" skipped="0" failures="0" errors="0"`，用例：

- `goldenVector_reproduces_frozenValues`
- `goldenVector_decrypts`
- `wrongPassword_fails`
- `maliciousMemoryKiB_rejected_beforeKdf`

（debug 与 release 变体各 4/4。）

---

## 2. HarmonyOS 原生验证工程（HAP）

| 文件                                                       | 字节   | SHA-256                                                                                                                                                      |
| ---------------------------------------------------------- | ------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `platforms/harmonyos/artifacts/entry-default-unsigned.hap` | 18,986 | 首次交付件 `4f10d0597aaaac2aab4af8e27ec7138709e07e5ea81aaed705d249ed55bd0663`；本轮重建件 `ceeb5c5611aef313a243d12a0e428fce8af3817bb3226983a42d1dec3be1b3af` |

**HAP 的 SHA-256 不是构建不变量**（必须注意）：HAP 是 zip，打包会写入构建时间戳；
本轮真实重建后**字节数与每个 zip 条目的字节数完全一致**（`ets/modules.abc` 10,568 B 等），
但整体哈希不同。判据应为「构建成功 + 条目内容一致」。详见 `docs/HARMONY_BUILD_REPORT.md` §5.1。

HAP（zip）关键内容：

| 条目                                     | 字节   | 说明                          |
| ---------------------------------------- | ------ | ----------------------------- |
| `ets/modules.abc`                        | 10,568 | **ArkTS 字节码 → 已真实编译** |
| `ets/sourceMaps.map`                     | 2,322  | 源码映射                      |
| `module.json`                            | 1,241  | 编译产出                      |
| `resources.index`                        | 646    | 资源索引                      |
| `resources/base/media/app_icon.png`      | 2,825  | 应用图标                      |
| `resources/base/profile/main_pages.json` | 23     | 页面路由                      |
| `pack.info`                              | 529    | 打包信息                      |

- **性质**：**unsigned**（不可安装）；**原生验证工程产物，不是产品包** ——
  只含最小 `EntryAbility` + `pages/Index.ets` + `entry/src/main/ets/adapters/RelationalStoreSecureAdapter.ets`，
  **不含** 24 个 `.uvue` 页面与 uni-app x 运行时。

### 2.1 复现命令

```bash
cd platforms/harmonyos
./build.sh assembleHap      # 内部自动镜像到 ASCII 路径后调用 hvigorw
# 期望：> hvigor BUILD SUCCESSFUL in ...
```

> 本环境 bash 缺少 `dirname` / `grep` / `mkdir` / `tar` / `cp`，`build.sh` 无法直接执行；
> 本轮以等价的 Python 流程复刻（镜像到 `%USERPROFILE%\depmap-harmony-build` 后调用
> `hvigorw.bat assembleHap --mode module -p product=default -p buildMode=debug --no-daemon`），
> 结果 `BUILD SUCCESSFUL in 59 s 267 ms`。

---

## 3. UTS 降级产物（本轮新增证据）

`cd core && npm run check:uts` → **15/15 compiled, PASS**（产物写到 `.tmp_audit/uts-gate-out/`，不入库）。

| 插件                     | Android→Kotlin | iOS→Swift | HarmonyOS→ArkTS |
| ------------------------ | -------------- | --------- | --------------- |
| `depmap-biometric`       | PASS           | PASS      | PASS            |
| `depmap-file-crypto`     | PASS           | PASS      | PASS            |
| `depmap-privacy-screen`  | PASS           | PASS      | PASS            |
| `depmap-secure-database` | PASS           | PASS      | PASS            |
| `depmap-secure-key`      | PASS           | PASS      | PASS            |

编译器：`@dcloudio/uts` 3.0.0-alpha-5020620260914001 + `@dcloudio/uts-win32-x64-msvc`（公共 npm）。
详见 `docs/UTS_COMPILE_VERIFICATION.md`（含**证明了什么 / 没证明什么**的严格边界）。

---

## 4. 缺失产物（诚实清单）

| 平台      | 期望产物                 | 状态                         | 阻塞           |
| --------- | ------------------------ | ---------------------------- | -------------- |
| Android   | APK（调试）/ AAB（发布） | **无**                       | B10 + B4 + B18 |
| HarmonyOS | 产品级 signed HAP        | **无**（仅 unsigned 验证包） | B10 + B7 + B18 |
| iOS       | .app / .ipa              | **无**                       | B3 + B8/B9     |
| UI        | 编译产物（基座）         | **无**                       | B10            |

---

## 5. 应用资产（占位，U-1 已闭环）

| 路径                 | 文件                                                                  | 说明                                               |
| -------------------- | --------------------------------------------------------------------- | -------------------------------------------------- |
| `app/static/icons/`  | `48x48.png`、`72x72.png`、`96x96.png`、`192x192.png`、`1024x1024.png` | 品牌色 `#4C4FD8`，背景 `#F5F6FA`；PNG 结构校验通过 |
| `app/static/splash/` | `480x762.png`、`720x1242.png`、`960x1656.png`、`1242x2688.png`        | 同上                                               |

这些是**程序化生成的品牌占位图**，仅用于让 `app/manifest.json` 的引用不再指向空路径。
**正式设计资产仍待 B15 / B16。**

---

## 6. 完整性校验

```bash
cd <repo>
sha256sum platforms/android/artifacts/*.aar     # 应与 §1 表格逐项相等
sha256sum platforms/harmonyos/artifacts/*.hap   # 与 §2 的「本轮重建件」相等；重跑会变（zip 时间戳）
```
