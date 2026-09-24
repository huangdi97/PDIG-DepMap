# HARMONY_TOOLCHAIN_AUDIT.md — HarmonyOS 工具链与工程就绪度审计

> 范围：`platforms/harmonyos`（DepMap HarmonyOS 原生安全适配器 + 可构建验证工程）。
> 本审计**推翻了** `BLOCKERS.md` 中 B2 的原定性，并给出经实测验证的新根因。
> 审计时间：2026-09-15。

---

## 1. 结论摘要

| 项                                   | 结论                                                    |
| ------------------------------------ | ------------------------------------------------------- |
| HarmonyOS SDK / DevEco Studio 存在性 | **PASS**（与旧报告「无 DevEco / 无 SDK」相反）          |
| 远端 sdkmanager 接口可用性           | **PASS**（与旧报告「外部服务故障」相反）                |
| `platforms/harmonyos` 工程骨架完整性 | **FAIL → 本轮已补齐**（旧状态为 9/9 关键文件缺失）      |
| `app.json5` 结构合规性               | **FAIL → 本轮已重写**（旧结构不符合 DevEco Stage 模型） |
| ArkTS 适配器 `COMPILED`              | 见 §6（本轮首次进入真实编译流程）                       |

---

## 2. 工具链实测

| 组件                       | 实测值                                                                     |
| -------------------------- | -------------------------------------------------------------------------- |
| DevEco Studio              | **5.0.5.310**（`build.txt` = `DS-233.14475.28.36.505310`）                 |
| 安装路径                   | `<DEVECO_HOME>`                                                            |
| HarmonyOS SDK              | **HarmonyOS 5.0.1**，`version 5.0.1.115`，`apiVersion 13`，`stage Release` |
| SDK 路径                   | `<DevEco>/sdk/default/{hms, openharmony}`                                  |
| `@ohos/hvigor`             | **5.13.2**（`<DevEco>/tools/hvigor/hvigor`）                               |
| `@ohos/hvigor-ohos-plugin` | **5.13.2**（`<DevEco>/tools/hvigor/hvigor-ohos-plugin`）                   |
| hvigor wrapper             | `<DevEco>/tools/hvigor/bin/{hvigorw, hvigorw.bat, hvigorw.js}`             |
| ohpm                       | `<DevEco>/tools/ohpm`                                                      |
| 内置 Node                  | **v18.20.1**（`<DevEco>/tools/node`）                                      |
| 其他工具                   | `emulator` / `llvm` / `profiler`                                           |
| JDK（DevEco 自带）         | `<DevEco>/jbr`                                                             |

**结论**：HarmonyOS 工具链**完整存在且版本自洽**（DevEco 5.0.5.310 + SDK 5.0.1.115 + hvigor 5.13.2）。

---

## 3. 推翻旧定性：远端 sdkmanager 接口正常

旧报告（`BLOCKERS.md` B2）称 Harmony 构建受阻于「外部 SDK 服务故障」，
依据是 `repo.harmonyos.com/sdkmanager/v5/ohos/getSdkList` 返回 400。
本轮实测**否证**该结论：

| 请求                                                                             | 结果                                         |
| -------------------------------------------------------------------------------- | -------------------------------------------- |
| `GET /sdkmanager/v5/ohos/getSdkList`                                             | `405 Method Not Allowed`                     |
| `POST {}`                                                                        | `400 {"code":4003,"body":"invalid params"}`  |
| `POST {"osType":"win",...}`                                                      | `{"code":134004,"message":"osType参数异常"}` |
| `POST {"osType":"Windows","osArch":"x86_64",...}`                                | `{"code":134012,"message":"osArch参数异常"}` |
| `POST {"osType":"windows","osArch":"x64","supportVersion":"5.0-ohos-single-12"}` | **HTTP 200，返回真实组件列表**               |

返回示例：`[{"path":"ets","apiVersion":"13","license":"OpenHarmony-SDK","version":"5.0.1.111","displayName":"ArkTS"}, ...]`

**判定**：服务**正常**。400 是**参数校验失败**（`osType` 必须为 `windows` 而非 `win`/`Windows`；
`osArch` 必须为 `x64` 而非 `x86_64`），根因在请求构造侧，**不是外部服务故障**。

---

## 4. 真实根因：工程骨架缺失（旧状态 9/9 缺失）

审计 `platforms/harmonyos` 原始状态，仅 **3 个文件**：

```
platforms/harmonyos/app.json5
platforms/harmonyos/arkts/RelationalStoreSecureAdapter.ets
platforms/harmonyos/entry/src/main/module.json5
```

对照 DevEco Stage 模型标准工程骨架，关键文件缺失情况：

| 文件                                                        | 旧状态                                       |
| ----------------------------------------------------------- | -------------------------------------------- |
| `AppScope/app.json5`                                        | **MISSING**（且 `app.json5` 被错放在工程根） |
| `AppScope/resources/base/element/string.json`               | **MISSING**                                  |
| `AppScope/resources/base/media/app_icon.png`                | **MISSING**                                  |
| `oh-package.json5`                                          | **MISSING**                                  |
| `build-profile.json5`                                       | **MISSING**                                  |
| `hvigorfile.ts`                                             | **MISSING**                                  |
| `hvigor/hvigor-config.json5`                                | **MISSING**                                  |
| `entry/oh-package.json5`                                    | **MISSING**                                  |
| `entry/build-profile.json5`                                 | **MISSING**                                  |
| `entry/hvigorfile.ts`                                       | **MISSING**                                  |
| `entry/src/main/ets/entryability/EntryAbility.ets`          | **MISSING**（但 `module.json5` 引用了它）    |
| `entry/src/main/resources/base/profile/main_pages.json`     | **MISSING**（但 `module.json5` 引用了它）    |
| `entry/src/main/resources/base/element/{string,color}.json` | **MISSING**                                  |
| `entry/src/main/resources/base/media/app_icon.png`          | **MISSING**                                  |

此外，旧 `app.json5` 的结构**不符合 DevEco Stage 模型**：缺少 `"app"` 顶层键，
却混入了 `modules` / `srcPath` / `configuration` 等 `build-profile.json5` 才该有的字段。
换言之，**该工程从未被 DevEco 解析过**。

---

## 5. 本轮补齐内容

### 5.1 新增/重写的工程文件

| 文件                                                    | 说明                                                                                                    |
| ------------------------------------------------------- | ------------------------------------------------------------------------------------------------------- |
| `AppScope/app.json5`                                    | 重写为规范格式（`{"app": {bundleName, vendor, versionCode, versionName, icon, label}}`）                |
| `AppScope/resources/base/element/string.json`           | `app_name`                                                                                              |
| `AppScope/resources/base/media/app_icon.png`            | 由 `app/static/icons/192x192.png` 提供                                                                  |
| `oh-package.json5`                                      | `modelVersion: "5.0.0"`                                                                                 |
| `build-profile.json5`                                   | products(`default`) / `compatibleSdkVersion: "5.0.0(12)"` / `runtimeOS: "HarmonyOS"` / modules(`entry`) |
| `hvigorfile.ts`                                         | `import { appTasks } from '@ohos/hvigor-ohos-plugin'`                                                   |
| `hvigor/hvigor-config.json5`                            | 见 §5.3                                                                                                 |
| `hvigorw` / `hvigorw.bat` / `hvigorw.js`                | 自 DevEco `tools/hvigor/bin/` 复制                                                                      |
| `entry/oh-package.json5`                                | 模块级依赖清单                                                                                          |
| `entry/build-profile.json5`                             | `apiType: "stageMode"` / targets `default`                                                              |
| `entry/hvigorfile.ts`                                   | `import { hapTasks } from '@ohos/hvigor-ohos-plugin'`                                                   |
| `entry/src/main/ets/entryability/EntryAbility.ets`      | 标准 `UIAbility` 生命周期实现                                                                           |
| `entry/src/main/ets/pages/Index.ets`                    | 最小可编译 `@Entry` 页面（非产品 UI）                                                                   |
| `entry/src/main/resources/base/profile/main_pages.json` | `{"src": ["pages/Index"]}`                                                                              |
| `entry/src/main/resources/base/element/string.json`     | `module_desc` / `EntryAbility_desc` / `EntryAbility_label`                                              |
| `entry/src/main/resources/base/element/color.json`      | `start_window_background`                                                                               |
| `entry/src/main/resources/base/media/app_icon.png`      | 模块图标                                                                                                |

### 5.2 ArkTS 适配器位置调整

`arkts/RelationalStoreSecureAdapter.ets` → `entry/src/main/ets/adapters/RelationalStoreSecureAdapter.ets`。

原因：Gradle 允许 `sourceSets.main.kotlin.srcDir("../kotlin")` 这类自定义源码目录，
但 **hvigor / ArkTS 不支持自定义源码根**，源码必须位于 `entry/src/main/ets/**` 才会被编译。
保持与 `platforms/android/kotlin`、`platforms/ios/swift` 平行的「独立源码目录」在此不可行。

### 5.3 hvigor 依赖解析处置

`@ohos/hvigor` 在公共 registry（`repo.harmonyos.com/npm`）上有 80 个版本，
但 **5.13.2 不在其中**（5.x 系列公开版本为 5.17.x / 5.18.x / 5.19.x）。
DevEco 内置的 5.13.2 属**未发布私有构建**。

因此 `hvigor/hvigor-config.json5` 采用 `file:` 协议直接引用本机 DevEco 内置包：

```json5
{
  "modelVersion": "5.0.0",
  "hvigorVersion": "file:<DEVECO_HOME>/tools/hvigor/hvigor",
  "dependencies": {
    "@ohos/hvigor-ohos-plugin": "file:<DEVECO_HOME>/tools/hvigor/hvigor-ohos-plugin"
  }
}
```

**注意**：该路径为**本机绝对路径**，换机或 DevEco 安装位置变化时需同步修改。

### 5.4 环境侧变更

| 变更                                                                       | 原因                                                                                  |
| -------------------------------------------------------------------------- | ------------------------------------------------------------------------------------- |
| 创建 `%USERPROFILE%\.npmrc`（内容 `registry=https://registry.npmjs.org/`） | hvigor wrapper 强制要求用户目录存在 `.npmrc`，否则拒绝执行                            |
| `NODE_HOME` 指向 DevEco 内置 node（`<DEVECO_HOME>\tools\node`，v18.20.1）  | `hvigorw.bat` 需要 `NODE_HOME` 或 PATH 中有 `node`；且 pnpm 子进程依赖 `node` 在 PATH |

> `.npmrc` 内容刻意保持 `registry.npmjs.org`（npm 默认值），**不改变用户原有 registry 行为**。

---

## 6. 构建执行状态

hvigor 启动链路：`hvigorw.bat` → `hvigorw.js`（wrapper）→ 解析 `hvigor/hvigor-config.json5`
→ 准备 `~/.hvigor/project_caches/<hash>/workspace/node_modules`
→ 安装 `@ohos/hvigor` + `@ohos/hvigor-ohos-plugin` → 执行 hvigor 构建。

已排除的障碍（按出现顺序）：

1. `NODE_HOME` 格式错误（Git Bash 路径 vs Windows 路径）→ 已修正
2. `ENOENT ... @ohos/hvigor/bin/hvigor.js`（缓存 workspace 不完整）→ 已清理缓存重试
3. `No npmrc file is matched in the current user folder` → 已创建 `~/.npmrc`

### 6.1 已排除的障碍（含构建期暴露的结构性错误）

| #   | 错误                                                       | 处置                                                                            |
| --- | ---------------------------------------------------------- | ------------------------------------------------------------------------------- |
| A   | `hvigor ERROR: Invalid project path`                       | 非 ASCII 路径，改用 `build.sh` 镜像到 ASCII 目录构建                            |
| B   | `Schema validate failed` @ `module.abilities[0].label`     | `label` 必须为 `$string:` 引用，改为 `$string:EntryAbility_label`               |
| C   | `Schema validate failed` @ `hvigor-config.json5`           | `hvigorVersion` 非合法字段，移除并改写入 `dependencies`                         |
| D   | `Module-Abilities-srcEntry ... not found` @ `CompileArkTS` | `srcEntry` 应相对 `entry/src/main/`，改为 `./ets/entryability/EntryAbility.ets` |

### 6.2 真实构建结果

**已成功产出 HAP**（2026-09-15）：

```
> hvigor Finished :entry:default@CompileArkTS... after 28 s 90 ms
> hvigor Finished :entry:default@PackageHap...   after 1 s 227 ms
> hvigor BUILD SUCCESSFUL in 42 s 263 ms
```

产物 `entry-default-unsigned.hap`（18,986 字节，SHA-256 `4f10d059…`），
含 `ets/modules.abc`（ArkTS 编译字节码）。详见 `docs/HARMONY_BUILD_REPORT.md`。

因此 `HARMONY_BUILD_READY = PASS`（**原生验证工程**层面）。
`SIGNING_READY` / `INSTALL_READY` / `DEVICE_VERIFIED` 仍为 **BLOCKED**。

---

## 7. 结论

1. HarmonyOS **工具链 PASS**：DevEco 5.0.5.310 + SDK 5.0.1.115(API 13) + hvigor 5.13.2 齐备。
2. **旧 B2 定性被推翻**：远端 sdkmanager 接口正常，400 源于请求参数格式。
3. **真实阻塞是源码级工程缺口**：`platforms/harmonyos` 原为骨架声明，9/9 关键文件缺失，
   且 `app.json5` 结构不符合 DevEco 规范 —— 该工程从未被 DevEco 解析过。
4. 本轮已补齐完整 Stage 模型工程骨架，并把 ArkTS 适配器纳入编译范围。
5. `HARMONY_SOURCE_READY` 从「名义 PASS」修正为**本轮补齐后 PASS**；
   `HARMONY_BUILD_READY` 仍取决于 §6 的 HAP 产出结果。
