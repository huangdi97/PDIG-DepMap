# NATIVE MIGRATION — 阶段验收与 Cutover 判定（2026-09-19）

> 分支 `feat/mvp03-living-graph`，末提交 `61fb69d`（推送到远端并已在 macOS runner 上验证）。
> 状态枚举：`PASS / FAIL / NOT_RUN / PARTIAL_WITH_REPORT / EXTERNAL_BLOCKED`。
> **任何 PASS 都必须指向一条可复现命令及其输出**；本报告里每一条都附了。

---

## 1. 结论（先给结论）

```
NATIVE_MIGRATION = NOT PASS（11 条 Cutover 条件中 8 条 PASS，3 条未满足）
```

未满足的三条是：

| # | 条件 | 判定 | 原因 |
| --- | --- | --- | --- |
| 3 | Harmony parity PASS | **PARTIAL_WITH_REPORT** | 87/91；4 条需设备运行时（Argon2id 原生 / ArkData） |
| 9 | UI critical flow parity PASS | **NOT_RUN** | iOS 尚无 SwiftUI 应用层；Harmony ArkUI 未在设备上取证 |
| 10 | Security PASS | **PARTIAL_WITH_REPORT** | iOS Keychain / LocalAuthentication 未实现，无设备取证 |

第 11 条（Production build 不再依赖 DCloud / UTS / uni-app）因此**不得执行**
——`AGENTS.md` §24.5 明写：在 Cutover 条件满足前 legacy `core/` 与 `app/`
绝不删除。这不是"没做完"，是**被条件挡住**。

---

## 2. 本轮新增证据（2026-09-19）

### 2.1 Harmony：GB18030 环境缺失闭合（85 → 87）

| 环节 | 命令 / 产物 | 结果 |
| --- | --- | --- |
| 码表生成 | `node tools/encoding/generate-gb18030-table.mjs` | 双字节 23940 全定义；BMP 四字节 50400 槽（有效 39420 / 209 游程）；增补平面复验 7656 条 mismatch=0 |
| 独立验算 | `python tools/encoding/crosscheck-gb18030.py` | `GB18030_CROSSCHECK = PASS` |
| 分歧仲裁 | `Gb18030Arbiter.java`（JDK `Charset.forName("GB18030")`） | 21 个分歧码位：JAVA 同意 ICU 20 / 同意 CPython 1 / 都不 0；仅 `A3A0` 采用 override `U+E5E5` |

```
HARMONY_CONFORMANCE_HOST=PASS
HARMONY_HOST_PASS=87/91   fail=0
HARMONY_ENV_BLOCKED=0      HARMONY_DEVICE_BLOCKED=4
```

### 2.2 iOS：canonical 91 条全部真实执行

```
IOS_TOTAL_CANONICAL      = 91
IOS_HOST_EXECUTED        = 91   (fail=0)
IOS_HOST_IMPL_MISSING    = 0
IOS_ENV_BLOCKED          = 0
IOS_CONFORMANCE_HOST     = PASS
IOS_HOST_PASS            = 91/91
```

来源：GitHub Actions run **35427324918**（head `9cd55f5`）、
**35427846349**（head `61fb69d`），`macos-14`，`swift build` + `swift test`。

分类账（逐项，不用汇总值代替）：relations 18 / jcs 1 / scenario 1 / parser 22 /
impact 13 / readiness 16 / coverage 6 / timeline 3 / state-machine 5 /
depmap 3 / migration 2 / backup 1 = **91**。

### 2.3 跨平台差分（N5）

三份报告各自独立产生，差分工具**不重算任何用例**：

| 平台 | 报告 | 产生方式 |
| --- | --- | --- |
| android | `conformance/reports/android.json` | `./gradlew --no-daemon :conformance:run` → `pass=91 fail=0 notImplemented=0 total=91` |
| harmony | `conformance/reports/harmony.json` | 真 ArkTS 运行时（hvigor 本地单测）→ `executed=87 pass=87 fail=0` |
| ios | `conformance/reports/ios.json` | macOS runner `swift test` → `91/91` |

Android 另有 JVM 单测 `:core:test --rerun-tasks` → **71/71**（failures=0 errors=0
skipped=0；6 个测试类，XML 在 `C:/Users/Kaiser/pdig-build/core/test-results/test`，
本机自定义 buildDir）。

```
CROSS_PLATFORM_VERDICT_MATRIX     = PASS   （0 条判定分歧）
CROSS_PLATFORM_ACTUAL_ANDROID_IOS = PASS   （91/91 actual 逐字节相同）
CROSS_PLATFORM_DIFFERENTIAL       = PASS
```

`actual` 比对用的是自带的规范化序列化器（保留数字原始文本与对象键顺序）——
`JSON.parse` 会把 `1.0` 与 `1` 合成同一个 number，正是这类差异会被漏掉。

Harmony **只参加判定矩阵，不参加字节比对**：ArkTS 主机测试不保证可写文件，
因此不产出 `actual`。这是能力边界，已在工具与输出里明示，未用占位值填补。

---

## 3. 逐条 Cutover 条件（GOAL_PDIG_NATIVE_MIGRATION.md §7）

| # | 条件 | 判定 | 证据 |
| --- | --- | --- | --- |
| 1 | Canonical Spec = SINGLE SOURCE OF TRUTH | **PASS** | `node tools/codegen/generate.mjs --check`（4 个生成文件一致）；fixture integrity 91/91 + imports 28/28 |
| 2 | Android parity PASS | **PASS** | `:conformance:run` **91/91 fail=0**；`:core:test` **71/71 fail=0 error=0 skip=0**（本轮 `--rerun-tasks` 实跑，6 个测试类） |
| 3 | Harmony parity PASS | **PARTIAL_WITH_REPORT** | 87/91，4 条设备运行时：Argon2id 原生 2 条 + ArkData 2 条 |
| 4 | iOS source parity PASS | **PASS** | iOS `IOS_HOST_PASS=91/91` |
| 5 | 跨端 Conformance PASS | **PASS** | `CROSS_PLATFORM_DIFFERENTIAL=PASS` |
| 6 | Crypto Golden PASS | **PASS（Android / iOS）** | `depmap-golden-v1` derivedKey 十六进制字节级 golden；Harmony 侧仍属第 3 条的 4 条之一 |
| 7 | Migration PASS | **PASS（Android / iOS）** | `migration-db-v1-to-v3`：finalSchemaVersion=3、ID 与决策保留、50 次重复严格 no-op |
| 8 | Backup / Restore PASS | **PASS（Android / iOS）** | `backup-depmap-export-restore-roundtrip`：export → .depmap → 新库 → export 逐字节相同，孤儿 0 |
| 9 | UI critical flow parity PASS | **NOT_RUN** | 无 SwiftUI 应用层；Harmony ArkUI 未设备取证 |
| 10 | Security PASS | **PARTIAL_WITH_REPORT** | Android 侧已有运行时取证；iOS Keychain / LocalAuthentication 未实现 |
| 11 | Production 不再依赖 DCloud / UTS / uni-app | **BLOCKED_BY_3_9_10** | 见 §1 |

---

## 4. 复现命令

```bash
# Android（本机 JDK21）
cd android && JAVA_HOME="D:/Code/Android Studio/jbr" \
  ./gradlew --no-daemon --configure-on-demand :core:test :conformance:run

# Harmony（主机执行面，需 DevEco）
PDIG_DEVECO_HOME="D:/Code/Harmony/DevEco Studio" node tools/harmony/run-conformance-host.mjs

# iOS（本机无 Swift 工具链，必须走 macOS runner）
gh workflow run iOS --ref feat/mvp03-living-graph

# 跨平台差分（需先备齐三份报告）
node tools/conformance/diff-reports.mjs --json
```

---

## 5. 真实外部阻塞（不是"没做完"）

| 项 | 性质 | 需要谁 |
| --- | --- | --- |
| Harmony 设备运行时（4 条） | 需要 Harmony 真机 / 模拟器 | 本地建模拟器（耗时长）或持设备者 |
| iOS 真机（LocalAuthentication / Keychain 访问组） | 需要 Apple 设备 | 人类 |
| iOS / Harmony 上架签名、开发者账号、审核 | 外部服务 | 人类 |
| 真实账单数据（Real Data gate） | 用户私有数据 | 人类 |
| 上架提交与发布决定 | 不可逆决策 | 人类 |

---

## 6. 本轮修掉的三个真缺陷（都不是"改测试让它绿"）

1. **门禁假绿**：`swift build 2>&1 | tail -40` 的退出码取自 `tail`，
   编译错误（`unterminated string literal`）被吞掉而 job 仍报 success。
   已去掉管道，退出码如实上传。
2. **Swift `Character` 是字素簇**：`"\r\n"` 是**一个** Character，
   `ch == "\r"` 永远匹配不到 CRLF → `parser-csv-crlf` 表头末列变成
   `currency\r\n`。改为按 `unicodeScalars` 扫描（解析层修复，非用例规避）。
3. **codegen 未处理 Swift 保留字与控制字符**：`case in = "in"` 编译失败；
   TAB 分隔符字面量进源码触发 `unprintable ASCII character`。
   修在生成器 `tools/codegen/generate.mjs`，未手改生成物。

---

## 7. 口径红线（照抄，不得升级措辞）

- iOS 91/91 = **canonical 逻辑一致性**，**不等于**以下任何一项：
  不是「iOS 已接入 SQLCipher」（host 用系统 sqlite3，仅覆盖 payload 与迁移逻辑）；
  不是「iOS UI 已完成」；不是「iOS 真机跑通」（无设备运行时证据）。
- Harmony 只能写 87/91，禁止写 `HARMONY_CONFORMANCE = 91/91`。
- 禁止把 `CROSS_PLATFORM_DIFFERENTIAL = PASS` 写成 `NATIVE_MIGRATION = PASS`。
