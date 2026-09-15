# HARMONY_RELEASE_RUNBOOK.md — HarmonyOS 发布执行手册

> 适用版本：DevEco Studio **5.0.5.310** + HarmonyOS SDK **5.0.1.115（API 13）** + hvigor **5.13.2**
> 工程：`platforms/harmonyos/`（DevEco Stage 模型）
> 上一轮报告：`docs/HARMONY_BUILD_REPORT.md`（已真实产出 HAP）、`docs/HARMONY_TOOLCHAIN_AUDIT.md`

---

## 1. 先读这段：本手册能做什么、不能做什么

| 目标                           | 状态                                       |
| ------------------------------ | ------------------------------------------ |
| 原生验证工程构建（HAP）        | **可执行**（本手册 §3 已验证）             |
| 产品级 HAP（含 24 页 `.uvue`） | **BLOCKED（B10）** —— 由 HBuilderX 产出    |
| 签名                           | **BLOCKED（B7）** —— 需 AGC 证书 / Profile |
| 安装与真机验证                 | **BLOCKED（B18）** —— 无设备               |
| 上架 AppGallery                | **BLOCKED（B6 + B7 + B11）**               |

**当前 HAP 是「原生验证工程」产物，不是产品包**：它只含最小 `EntryAbility` + `Index.ets` +
`RelationalStoreSecureAdapter.ets`，**不含** 24 个 `.uvue` 页面与 uni-app x 运行时。
**不要**把它当作可分发版本。

---

## 2. 环境前置

| 项                | 值                                                                   |
| ----------------- | -------------------------------------------------------------------- |
| DevEco Studio     | `<DEVECO_HOME>`（5.0.5.310）                         |
| HarmonyOS SDK     | `<DEVECO_HOME>\sdk`（API 13 / 5.0.1.115）            |
| Node（hvigor 用） | DevEco 内置 `tools\node`（v18.20.1），由 `build.sh` 自动注入 `PATH`  |
| npmrc             | `C:\Users\<user>\.npmrc` 必须存在，否则报 `No npmrc file is matched` |
| hvigor 依赖       | 由 `hvigor/hvigor-config.json5` 以 `file:` 协议引用 DevEco 内置包    |

### 2.1 三个「踩过的坑」（改配置前必读）

1. **`@ohos/hvigor` 5.13.2 未发布到公共 registry**（公共版仅到 5.19.8）。
   必须用 `file:` 协议指向 DevEco 内置包，且**两个包都要写进 `dependencies`**：

   ```json5
   {
     "modelVersion": "5.0.0",
     "dependencies": {
       "@ohos/hvigor": "file:<DEVECO_HOME>/tools/hvigor/hvigor",
       "@ohos/hvigor-ohos-plugin": "file:<DEVECO_HOME>/tools/hvigor/hvigor-ohos-plugin"
     },
     "execution": {},
     "logging": {},
     "debugging": {},
     "nodeOptions": {}
   }
   ```

2. **`hvigor-config.json5` 不允许 `hvigorVersion` 字段**（Schema 只允许
   `modelVersion / dependencies / execution / logging / debugging / nodeOptions / properties`）。

3. **`module.json5` 的 `srcEntry` 相对 `entry/src/main/`**，不是相对 `entry/`。
   写成 `./src/main/ets/entryability/EntryAbility.ets` 会在 `CompileArkTS` 阶段报 not found。

---

## 3. 原生验证工程构建（可执行，已验证）

```bash
cd platforms/harmonyos
./build.sh assembleHap
```

`build.sh` 内部关键逻辑：

1. **非 ASCII 路径镜像**：hvigor **拒绝**非 ASCII 工程路径，且**没有** AGP 那种
   `android.overridePathCheck` 豁免开关。脚本用 `tar` 把工程镜像到 ASCII 目录
   （默认 `<DEPMAP_TOOLS_HOME>/harmony-ascii`，可用 `DEPMAP_HARMONY_BUILD_ROOT` 覆盖）。
2. **覆盖式解包**，不 `rm -rf` 整个镜像目录 —— 本机 safe-delete 守卫会拒绝 >50 文件的批量删除。
3. 注入 DevEco 内置 Node 到 `PATH`，设置 `DEVECO_SDK_HOME`。
4. 执行 `./hvigorw.bat assembleHap --mode module -p product=default -p buildMode=debug --no-daemon`。
5. 把 HAP 回收到 `platforms/harmonyos/artifacts/`。

### 3.1 期望输出

```
> hvigor BUILD SUCCESSFUL in 42 s 263 ms
```

产物：`platforms/harmonyos/artifacts/entry-default-unsigned.hap`
（18,986 B，SHA-256 `4f10d0597aaaac2aab4af8e27ec7138709e07e5ea81aaed705d249ed55bd0663`）
内含 ArkTS 字节码 `ets/modules.abc`（10,568 B）—— **ArkTS 已真实编译**。

---

## 4. 签名（BLOCKED — B7）

当前 HAP 为 **unsigned**，无法安装到真实设备。签名步骤：

1. 在 AppGallery Connect 创建应用，生成**证书（.cer）**、**Profile（.p7b）**与本地密钥库（.p12）。
2. DevEco：`File > Project Structure > Signing Configs` 勾选 `Automatically generate signature`
   或手动填入上述材料。
3. 在 `build-profile.json5` 的 `app.products[].signingConfig` 引用该配置。
4. 重新执行 `./build.sh assembleHap`（release 需 `-p buildMode=release`）。

**禁止**：把 `.p12` / 证书密码写入 Git。`signing/` 已在 `.gitignore`。

---

## 5. 安装与真机验证（BLOCKED — B18）

```bash
hdc list targets          # 期望至少 1 个设备
hdc install entry-default-signed.hap
hdc shell aa start -a EntryAbility -b <bundleName>
```

冒烟清单（有设备后逐条执行）：

- [ ] 冷启动进入首页，无白屏、无崩溃（≤3s）
- [ ] `Index.ets` 渲染正常
- [ ] `RelationalStoreSecureAdapter` 打开加密库成功
- [ ] 切后台再回前台，数据未丢失
- [ ] `hilog` 无 `E ` 级错误

---

## 6. 产品级产物（BLOCKED — B10）

产品级 HarmonyOS 产物必须由 **HBuilderX / uni-app x** 产出：

1. HBuilderX 中打开 `app/`。
2. `发行 > 原生App-云打包 / 本地打包`，选择 HarmonyOS。
3. 产出后用 §4 的签名材料签名。

**当前阻塞根因**：HBuilderX 5.24 已安装，但 **CLI 无 build 命令**，无头环境无法触发打包
（详见 `BLOCKERS.md` B10）。

---

## 7. 上架检查清单（AppGallery）

- [ ] B11：确定正式 `bundleName`（现为占位）
- [ ] B7：签名材料齐备
- [ ] B6：Huawei Developer / AGC 身份
- [ ] B15/B16：正式图标与启动图（占位图已生成于 `app/static/`）
- [ ] B17：商店截图
- [ ] B12：隐私政策 URL
- [ ] 权限声明复核（`entry/src/main/module.json5` 的 `requestPermissions` 当前为空数组）

---

## 8. 故障排除

| 现象                                             | 根因                                | 处置                                                                    |
| ------------------------------------------------ | ----------------------------------- | ----------------------------------------------------------------------- |
| `ERROR: Invalid project path`                    | 工程路径含非 ASCII 字符             | 用 `build.sh` 镜像到 ASCII 目录（hvigor 无豁免开关）                    |
| `ENOENT ... @ohos/hvigor/bin/hvigor.js`          | 5.13.2 未在公共 registry            | `hvigor-config.json5` 用 `file:` 协议指向 DevEco 内置包                 |
| `Error: The hvigor depends on the npmrc file`    | 用户目录无 `.npmrc`                 | 创建 `C:\Users\<user>\.npmrc`（`registry=https://registry.npmjs.org/`） |
| `Schema validate failed` @ `abilities[0].label`  | `label` 必须是 `$string:` 引用      | 改 `$string:xxx` 并在 `element/string.json` 补齐                        |
| `Schema validate failed` @ `hvigor-config.json5` | 出现非法字段（如 `hvigorVersion`）  | 删除非法字段                                                            |
| `srcEntry ... not found` @ `CompileArkTS`        | `srcEntry` 多写了 `src/main/` 一层  | 改为 `./ets/entryability/EntryAbility.ets`                              |
| `NODE_HOME is not set ...`                       | `.bat` 由 cmd 解析，需 Windows 路径 | `NODE_HOME='<DEVECO_HOME>\tools\node'`                  |
| safe-delete 守卫拒绝清理镜像目录                 | 单次批量删除超阈值                  | 用覆盖式 `tar xf - --overwrite`，只单独清理 `entry/build` 等            |

---

## 9. 与旧结论的差异（重要）

旧文档与 `BLOCKERS.md` 曾记录：「`repo.harmonyos.com/sdkmanager/v5/ohos/getSdkList` 返回 400，
远端组件列表接口不可用，必须走 DevEco 图形化 SDK Manager」。

**该定性已于 2026-09-15 被推翻**：实测该接口在参数正确时返回 **HTTP 200 + 真实组件列表**；
此前 400 是**请求参数错误**（`osType` 须 `windows`、`osArch` 须 `x64`）。
真正的历史根因是**工程从来不是一个可构建 Stage 工程**（只有 3 个文件，且 `app.json5`
不含 Stage 模型要求的顶层 `"app"` 键）。该问题已在本轮修复。
