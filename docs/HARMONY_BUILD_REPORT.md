# HARMONY_BUILD_REPORT.md — HarmonyOS 真实构建报告

> 范围：`platforms/harmonyos`（DepMap HarmonyOS 原生安全适配器 + 可构建验证工程）。
> 本报告只陈述**真实执行过**的命令与**真实存在**的产物；未执行的项一律标注 `NOT_RUN` / `BLOCKED`。
> 报告时间：2026-09-15。

---

## 1. 结论摘要

| 项                  | 结论                 | 证据                                                                |
| ------------------- | -------------------- | ------------------------------------------------------------------- |
| HarmonyOS 工程骨架  | **PASS（本轮补齐）** | 原 9/9 关键文件缺失 → 已补齐为完整 DevEco Stage 模型工程            |
| ArkTS **COMPILED**  | **PASS**             | `CompileArkTS` 任务成功，产出 `ets/modules.abc`                     |
| HAP 产物            | **PASS**             | `entry-default-unsigned.hap`（18,986 字节）                         |
| HAP **签名**        | **BLOCKED（B7）**    | `Will skip sign 'hos_hap'. No signingConfigs profile is configured` |
| **INSTALL_READY**   | **BLOCKED**          | HAP 未签名；且无可用 HarmonyOS 设备                                 |
| **DEVICE_VERIFIED** | **BLOCKED**          | 无设备                                                              |

**重要界定**：本 HAP 是**原生适配器验证工程的 HAP**，**不是**产品应用包。
产品 UI 在 `app/`（uni-app x）中，产品级 HAP 需由 HBuilderX 产出（依赖 **B10**）。
不得据此声明 HarmonyOS 可上线。

---

## 2. 构建环境

| 组件                       | 版本 / 路径                                                   |
| -------------------------- | ------------------------------------------------------------- |
| DevEco Studio              | 5.0.5.310（`<DEVECO_HOME>`）                  |
| HarmonyOS SDK              | HarmonyOS 5.0.1，`5.0.1.115`，API 13（`sdk/default`）         |
| `@ohos/hvigor`             | 5.13.2（DevEco 内置，`file:` 协议引用）                       |
| `@ohos/hvigor-ohos-plugin` | 5.13.2（DevEco 内置，`file:` 协议引用）                       |
| Node                       | v18.20.1（DevEco 内置）                                       |
| 构建入口                   | `platforms/harmonyos/build.sh`（自动镜像到 ASCII 路径）       |
| 镜像目录                   | `<DEPMAP_TOOLS_HOME>\harmony-ascii3`（默认 `harmony-ascii`） |

---

## 3. 构建命令

```bash
cd <repo>/platforms/harmonyos
./build.sh assembleHap
```

`build.sh` 内部执行：

1. 检测工程路径是否含非 ASCII 字符；若是，用 `tar` 把工程镜像到 ASCII 目录（默认 `<DEPMAP_TOOLS_HOME>/harmony-ascii`，可用 `DEPMAP_HARMONY_BUILD_ROOT` 覆盖）；
2. 设置 `NODE_HOME`（DevEco 内置 node）与 `DEVECO_SDK_HOME`；
3. 执行 `hvigorw.bat assembleHap --mode module -p product=default -p buildMode=debug --no-daemon`；
4. 把 `entry/build/default/outputs/default/*.hap` 回收到 `platforms/harmonyos/artifacts/`。

---

## 4. 构建结果（真实输出）

```
> hvigor Finished :entry:default@PreBuild...          after 163 ms
> hvigor Finished :entry:default@MergeProfile...      after  10 ms
> hvigor Finished :entry:default@ProcessProfile...    after 805 ms
> hvigor Finished :entry:default@ProcessRouterMap...  after 137 ms
> hvigor Finished :entry:default@CompileResource...   after 1 s 262 ms
> hvigor Finished :entry:default@CompileArkTS...      after 28 s  90 ms
> hvigor Finished :entry:default@PackageHap...        after 1 s 227 ms
> hvigor WARN:  Will skip sign 'hos_hap'. No signingConfigs profile is configured in current project.
> hvigor Finished :entry:default@SignHap...           after   3 ms
> hvigor Finished :entry:assembleHap...               after   1 ms
> hvigor BUILD SUCCESSFUL in 42 s 263 ms
```

---

## 5. 产物清单

| 产物         | 路径                                                       | 字节   | SHA-256                                                            |
| ------------ | ---------------------------------------------------------- | ------ | ------------------------------------------------------------------ |
| Unsigned HAP | `platforms/harmonyos/artifacts/entry-default-unsigned.hap` | 18,986 | `4f10d0597aaaac2aab4af8e27ec7138709e07e5ea81aaed705d249ed55bd0663` |

HAP（zip）内容实测：

| 条目                                     | 字节   | 说明                                                                                                  |
| ---------------------------------------- | ------ | ----------------------------------------------------------------------------------------------------- |
| `ets/modules.abc`                        | 10,568 | **ArkTS 编译字节码** —— 证明 ArkTS 源码被真实编译                                                     |
| `ets/sourceMaps.map`                     | 2,322  | 源码映射                                                                                              |
| `module.json`                            | 1,241  | `compileSdkVersion 5.0.1.115`、`bundleName com.depmap.app`、`minAPIVersion 12`、`targetAPIVersion 13` |
| `pack.info`                              | 529    | 打包元信息                                                                                            |
| `resources.index`                        | 646    | 资源索引                                                                                              |
| `resources/base/media/app_icon.png`      | 2,825  | 应用图标                                                                                              |
| `resources/base/profile/main_pages.json` | 23     | 页面路由                                                                                              |

---

## 6. 本轮修复的真实缺陷

工程此前**从未被 DevEco 解析过**。首次真实构建暴露并修复 **6 类缺陷**：

| #   | 缺陷                                 | 症状                                                                                 | 根因                                                              | 处置                                                       |
| --- | ------------------------------------ | ------------------------------------------------------------------------------------ | ----------------------------------------------------------------- | ---------------------------------------------------------- |
| H-1 | 工程骨架 9/9 关键文件缺失            | 无法构建                                                                             | 仅有 `app.json5` + `module.json5` + 一个 `.ets` 文件              | 补齐全部骨架（见 `docs/HARMONY_TOOLCHAIN_AUDIT.md` §5.1）  |
| H-2 | `app.json5` 不符合 DevEco Stage 模型 | hvigor 无法解析                                                                      | 缺 `"app"` 顶层键，却混入 `modules` / `srcPath` / `configuration` | 重写为规范格式并移入 `AppScope/`                           |
| H-3 | `hvigor` 依赖无法解析                | `ENOENT ... @ohos/hvigor/bin/hvigor.js`                                              | DevEco 内置的 5.13.2 **未发布**到公共 registry                    | 在 `hvigor-config.json5` 用 `file:` 协议引用 DevEco 内置包 |
| H-4 | `hvigorVersion` 字段非法             | Schema validate failed                                                               | 该字段不在 `hvigor-config.json5` 允许的 7 个字段内                | 移除；改为把 `@ohos/hvigor` 写入 `dependencies`            |
| H-5 | `abilities[0].label` 非法            | Schema validate failed                                                               | 必须使用 `$string:` 引用，不能是中文字面量                        | 改为 `$string:EntryAbility_label`（并补 `string.json`）    |
| H-6 | **`srcEntry` 路径多一层**            | `Module-Abilities-srcEntry './src/main/ets/entryability/EntryAbility.ets' not found` | `srcEntry` 相对于 `entry/src/main/`，原写法多了 `src/main/`       | 改为 `./ets/entryability/EntryAbility.ets`                 |

另有环境侧处置：

- 创建 `%USERPROFILE%\.npmrc`（hvigor wrapper 强制要求，内容保持 npm 默认 registry，不改变用户 npm 行为）；
- `NODE_HOME` 指向 DevEco 内置 node（v18.20.1）。

---

## 7. 非 ASCII 工程路径处置

hvigor 拒绝非 ASCII 工程路径：

```
> hvigor ERROR: Invalid project path.
	 Detail: Please move the project to a valid path
```

与 AGP 不同，hvigor **没有** `android.overridePathCheck` 之类的豁免开关。
因此 `build.sh` 在工程路径含非 ASCII 字符时，先把工程镜像到 ASCII 目录再构建，构建后回收产物。

（Android 侧同类问题见 `docs/ANDROID_BUILD_REPORT.md` §5；二者根因机制不同：
Android 是 Gradle worker argfile 的编码问题，HarmonyOS 是 hvigor 直接的合法性检查。）

---

## 8. 签名与设备：BLOCKED

| 项              | 状态              | 说明                                                   |
| --------------- | ----------------- | ------------------------------------------------------ |
| SIGNING_READY   | **BLOCKED（B7）** | 无 HarmonyOS release signing 配置；本次为 unsigned HAP |
| INSTALL_READY   | **BLOCKED**       | HAP 未签名，且本机无 HarmonyOS 设备                    |
| DEVICE_VERIFIED | **BLOCKED**       | 无设备                                                 |

解除签名需：华为开发者账号 + 签名证书/Profile，并在 `build-profile.json5` 的
`app.signingConfigs` 中配置。**禁止**把签名材料提交进 Git。

---

## 9. 结论

- HarmonyOS **原生适配器工程**：`SOURCE_READY = PASS`（本轮补齐后）、
  `BUILD_READY = PASS`、`HAP 产物 = PASS`、`SIGNING_READY = BLOCKED (B7)`、
  `INSTALL_READY = BLOCKED`、`DEVICE_VERIFIED = BLOCKED`。
- 本轮把 HarmonyOS 从「**没有可构建工程**」推进到「**真实产出 HAP**」，
  修复 6 类真实缺陷，其中 H-2 / H-6 是只有在真实构建时才会暴露的结构性错误。
- **该 HAP 为原生验证工程产物，不是产品包**。产品级 HarmonyOS 应用仍需
  HBuilderX（B10）从 `app/` 产出。
