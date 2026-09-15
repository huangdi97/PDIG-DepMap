# RELEASE_CANDIDATE_MANIFEST.md — RC 产物清单

> 生成时间：2026-09-15
> 作用：列出本轮**真实构建产出**的每一个产物，含路径、字节数、SHA-256 与构建复现命令。
> 任何产物都可以用本文件提供的 SHA-256 做完整性校验。
>
> **本清单不构成发布候选（Release Candidate）** —— 三端均无产品级可安装包（见 §3）。

---

## 1. Android 原生库（AAR）

| 文件                                         | 字节   | SHA-256                                                          |
| -------------------------------------------- | ------ | ---------------------------------------------------------------- |
| `platforms/android/artifacts/core-debug.aar` | 72,376 | `dd7d8c04b23041354b401b1f0e347b9fedf586131dae975c8df70520ef850a0d` |
| `platforms/android/artifacts/core-release.aar` | 68,731 | `4ac2e7f4c5d407d6b6a2d2232913f343ff7b7ef0f27891d67c60a190bf43c32b` |

- `core-release.aar` 内含 **37 个** `.class`，覆盖：
  `DepmapContainerV1`（含 `B64` / `JsonHeader`）、`DepmapSchemaV1`、
  `SecureDatabaseAdapter` / `SqlCipherSecureDatabaseAdapter` / `DepmapSqlCipherHelper`、
  `KeystoreSecureKeyAdapter`（含 `SecureDatabaseOpenOptions` / `FlagSecurePrivacyAdapter`）、
  `BiometricGateAdapter` / `BiometricGate` / `BiometricGateResult`、
  **`UtsSecurityBridge`**（本轮新增的 UTS 回调式桥接）。
- **性质**：Android Library。**不是 APK / AAB，不可安装。**

### 1.1 复现命令

```bash
cd platforms/android
export JAVA_HOME="<ANDROID_STUDIO_HOME>\\jbr"
gradle :core:assembleDebug :core:assembleRelease :core:testDebugUnitTest collectArtifacts \
  --rerun-tasks --no-build-cache --console=plain
```

> **必须带 `--rerun-tasks --no-build-cache`**。否则 Gradle build cache 会跨路径命中
> `FROM-CACHE`，造成「看似通过、实则未执行」的假绿（详见 `docs/ANDROID_RELEASE_RUNBOOK.md` §3）。

### 1.2 伴随测试

`core/build` 测试报告：`TEST-com.depmap.core.crypto.DepmapContainerV1GoldenTest.xml`
`tests="4" skipped="0" failures="0" errors="0"`，用例：

- `goldenVector_reproduces_frozenValues`
- `goldenVector_decrypts`
- `wrongPassword_fails`
- `maliciousMemoryKiB_rejected_beforeKdf`

（debug 变体本次实跑；release 变体同源于上轮 4/4。）

---

## 2. HarmonyOS 原生验证工程（HAP）

| 文件                                                        | 字节   | SHA-256                                                          |
| ----------------------------------------------------------- | ------ | ---------------------------------------------------------------- |
| `platforms/harmonyos/artifacts/entry-default-unsigned.hap`  | 18,986 | `4f10d0597aaaac2aab4af8e27ec7138709e07e5ea81aaed705d249ed55bd0663` |

HAP（zip）关键内容：

| 条目               | 字节   | 说明                          |
| ------------------ | ------ | ----------------------------- |
| `ets/modules.abc`  | 10,568 | **ArkTS 字节码 → 已真实编译** |
| `module.json`      | —      | 由 `module.json5` 编译产出     |
| `resources.index`  | —      | 资源索引                       |
| `pack.info`        | —      | 打包信息                       |

- **性质**：**unsigned**（未签名，不可安装）；**原生验证工程产物，不是产品包** ——
  只含最小 `EntryAbility` + `pages/Index.ets` +
  `entry/src/main/ets/adapters/RelationalStoreSecureAdapter.ets`，
  **不含** 24 个 `.uvue` 页面与 uni-app x 运行时。

### 2.1 复现命令

```bash
cd platforms/harmonyos
./build.sh assembleHap      # 内部自动镜像到 ASCII 路径后调用 hvigorw
# 期望：> hvigor BUILD SUCCESSFUL in 42 s 263 ms
```

---

## 3. 缺失产物（诚实清单）

| 平台      | 期望产物              | 状态               | 阻塞           |
| --------- | --------------------- | ------------------ | -------------- |
| Android   | APK（调试）/ AAB（发布） | **无**             | B10 + B4 + B18 |
| HarmonyOS | 产品级 signed HAP     | **无**（仅 unsigned 验证包） | B10 + B7 + B18 |
| iOS       | .app / .ipa           | **无**             | B3 + B8/B9     |
| UI        | 编译产物（基座）      | **无**             | B10            |

---

## 4. 应用资产（占位，U-1 已闭环）

| 路径                        | 文件                                                          | 说明                                  |
| --------------------------- | ------------------------------------------------------------- | ------------------------------------- |
| `app/static/icons/`         | `48x48.png`、`72x72.png`、`96x96.png`、`192x192.png`、`1024x1024.png` | 品牌色 `#4C4FD8`，背景 `#F5F6FA`；PNG 结构校验通过 |
| `app/static/splash/`        | `480x762.png`、`720x1242.png`、`960x1656.png`、`1242x2688.png` | 同上                                  |

这些是**程序化生成的品牌占位图**，用于让 `app/manifest.json` 的引用不再指向空路径。
**正式设计资产仍待 B15 / B16。**

---

## 5. 完整性校验

```bash
cd <repo>
sha256sum platforms/android/artifacts/*.aar platforms/harmonyos/artifacts/*.hap
# 与上面表格逐项比对
```
