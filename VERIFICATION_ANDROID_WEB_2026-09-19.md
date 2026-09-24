# 安卓端与 Web（core）端验证报告

> 验证时间：2026-09-19 18:16–18:40
> 验证人：AI agent（本机实跑）
> 基盘：`feat/mvp03-living-graph` HEAD

---

## 1. 目的

用户要求"先验证安卓和 web 端"。本报告只记录**本机实测**结果，不接受旧报告或缓存结论。

- "安卓端" = `android/` Kotlin 原生工程：`:core:test`、`:conformance:run`、`:app:assembleDebug`、APK 在 Android 34 模拟器真实运行。
- "Web 端" = `core/` 纯 TypeScript 共享核心（浏览器/Node 共用逻辑）：`typecheck` / `lint` / `format` / 单元测试 / 架构与网络门禁 / 密钥扫描 / UI 静态门禁 / canonical oracle 自检 / UTS 插件降级编译。

---

## 2. 本轮修掉的 3 个真实缺陷

| #   | 缺陷                                                                                                                                | 影响                                            | 修复位置                                                                                                                                                      |
| --- | ----------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1   | `core/scripts/generate-conformance.ts` 无法通过 `tsc`：`new WeChatStatementAdapter()` 缺少构造参数 `SqliteDriver`，脚本整体不可运行 | canonical fixture 生成器（TS oracle）事实不可用 | `scripts/generate-conformance.ts`：给 parser 段建一次性临时库，传入 `WeChatStatementAdapter(wechatDb.driver)`；同时给 `machine()` 加显式类型，消除 `any` 扩散 |
| 2   | `scripts/generate-conformance.ts` 的 `machine()` 把 `JSON.parse(...)` 的 `any` 直接返回，触发 `no-unsafe-return`                    | lint 门禁红                                     | 同文件：显式声明 `Machine` 类型，运行时缺失 machineId 则抛错                                                                                                  |
| 3   | `check-secrets` 误报 `spec/ui/design-tokens.json`（UI 设计令牌文件，不是凭证）                                                      | 密钥门禁红                                      | `core/scripts/check-secrets.mjs`：加精确路径豁免 `spec/ui/design-tokens.json`，并写明理由；其它 `*token*.json` 仍正常命中（已用探针反事实验证）               |

另外发现一个**测试调度 flaky**：

- `tests/integration/multi-source-e2e.test.ts` 的 K6 用例单跑约 1371ms，但全量并发下默认 5s 超时偶发失败（断言**一次未改**，失败的是超时不是断言）。
- 修复：将该用例显式超时从默认 5s 提到 30s，注释说明原因。
- 修复后连续 3 次全量 453/453 通过。

---

## 3. 安卓端（Android）实测

### 3.1 `:core:test` — 强制重跑

命令：

```bash
cd android
JAVA_HOME="D:/Code/Android Studio/jbr" \
  ./gradlew --no-daemon --console=plain --configure-on-demand \
  :core:test --rerun
```

结果：

```
classes=6  tests=71  failures=0  errors=0  skipped=0
  domain.DomainInvariantTest           tests=15  fail=0  err=0  skip=0
  impact.ImpactKernelTest              tests=11  fail=0  err=0  skip=0
  plan.PlanReadinessTest               tests=14  fail=0  err=0  skip=0
  schema.MigrationSemanticsTest        tests=10  fail=0  err=0  skip=0
  statemachine.GraphRevisionSemanticsTest  tests=7  fail=0  err=0  skip=0
  statemachine.StateMachineTest        tests=14  fail=0  err=0  skip=0
BUILD SUCCESSFUL
```

> **注意**：第一次跑 `:core:test` 显示 `UP-TO-DATE`，那不是本次验证。用 `--rerun` 强制重跑后才取到上述读数。

### 3.2 `:conformance:run`

```
pass=91  fail=0  notImplemented=0  total=91
report: E:\AI\...\conformance\reports\android.json
BUILD SUCCESSFUL
```

### 3.3 `:app:assembleDebug` — clean 重建

命令：

```bash
./gradlew --no-daemon --console=plain :app:clean :app:assembleDebug
```

结果：

```
39 actionable tasks: 17 executed, 19 from-cache, 3 up-to-date
BUILD SUCCESSFUL
```

APK 产物：

```
C:/Users/Kaiser/pdig-build/app/outputs/apk/debug/app-debug.apk
size: 36,958,501 bytes  mtime: 2026-09-19 18:27:51
```

成分取证：

```
classes.dex       17,369,356 B
classes16.dex        406,288 B
...                 (共 18 个 classes*.dex)
```

构建日志里显式出现 `libsqlcipher.so` 的 strip 警告，说明 SQLCipher 已打入 APK。

### 3.4 模拟器真机运行（Android 34 x86_64）

- 新建 AVD：`pdig_smoke`（pixel_4，android-34，x86_64）
- `adb install`：`Success`
- `adb shell am start -n com.pdig.app/.MainActivity`：启动成功
- 进程号：`pid=2503`，`topResumedActivity=com.pdig.app/.MainActivity`
- 崩溃日志：`logcat` 中 `FATAL EXCEPTION / AndroidRuntime` 为空
- 截图证据（见附件）：
  1. 锁屏页：`PDIG 已锁定` 正常渲染
  2. 点击"已知悉风险，本次进入"后进入主屏：`需要你处理 / 可能发生了变化 / 即将到来 / 常用场景 / 我的基础设施 / 数据与设置` 全部正常渲染
- `ro.product.cpu.abi=x86_64`：APK 在 x86_64 模拟器成功安装并启动，未因 abiFilters `arm64-v8a` 失败（APK 实际包含通用 DEX + SQLCipher native 可经 x86_64 ARM 翻译执行）。

结论：**Android 端到端构建、安装、启动、首屏交互全部通过**。

---

## 4. Web（core / TS）端实测

### 4.1 代码层静态门禁

| 门禁                 | 命令                       | 结果                                                  |
| -------------------- | -------------------------- | ----------------------------------------------------- |
| `typecheck`          | `tsc --noEmit`             | PASS（0 错误）                                        |
| `lint`               | `eslint .`                 | PASS（0 问题）                                        |
| `format:check`       | `prettier --check .`       | PASS                                                  |
| `check:architecture` | 模块依赖扫描               | PASS（48 文件，0 循环依赖）                           |
| `check:network`      | 网络原语扫描               | PASS（130 业务文件，0 网络原语）                      |
| `check:secrets`      | 密钥/凭证文件名 + 内容扫描 | PASS（850 文件，0 生产秘密）                          |
| `check:ui`           | `.uvue` 静态结构校验       | PASS（30 `.uvue`，24 pages，5 components，34 colors） |

`check:secrets` 反事实验证：

- 临时创建 `spec/ui/_probe_token.json` → gate 报 `1 finding`
- 删除探针 → gate PASS
- 证明豁免仅对 `spec/ui/design-tokens.json` 生效，没有放宽规则。

### 4.2 单元测试

```bash
npm run test
```

结果（修复 K6 flaky 后连续 3 次）：

```
Test Files  43 passed (43)
     Tests  453 passed (453)
 Duration  50.88s
```

### 4.3 Canonical oracle 自检

```bash
node --experimental-strip-types scripts/generate-conformance.ts --verify
```

```
ORACLE SELFCHECK: PASS (91 cases reproduce exactly)
```

说明 TypeScript 行为 oracle 与冻结的 fixture 完全未漂移。

### 4.4 UTS 插件编译门禁

```bash
npm run check:uts
```

```
[app-android]  depmap-biometric/file-crypto/privacy-screen/secure-database/secure-key  5/5 PASS
[app-ios]      5/5 PASS
[app-harmony]  5/5 PASS
UTS compile gate: 15/15 compiled
UTS compile gate PASS
```

---

## 5. 跨端一致性边界

- `core/` 是 Web 与三端原生共享的**行为 oracle**，已通过 91 条 fixture 自检；但 `core/` 本身不是可独立启动的 Web App。
- 可编译为 Web App 的 `app/`（uni-app x）目前**没有可用工具链**：
  - 无 HBuilderX（见 `BLOCKERS.md` B10）
  - UTS 插件层已通过 `check:uts` 验证三端降级编译，但 uni-app x 的主包（`.uvue` → H5）缺少本地编译器
  - 因此**Web App 完整打包**属于外部阻塞，不是代码问题
- Android 端已在本机 x86_64 模拟器完成安装/启动/首屏交互验证，真机签名/发布仍需用户决定。

---

## 6. 后续（需要用户决定）

1. 是否把本次验证结果连同 3 个修复一起提交并推送 `feat/mvp03-living-graph`？
2. 下一步优先哪一条未闭合项？
   - **Harmony 设备运行时 4 条**（Argon2id/ArkData 需 DevEco 模拟器或真机）
   - **iOS SwiftUI + Keychain/LocalAuthentication 应用层**（需 macOS；本机 Windows 无 Swift 工具链，可继续走 GitHub Actions）
   - **uni-app x Web App 完整打包**（需 HBuilderX 或官方 uni-app x CLI 本地工具链）
