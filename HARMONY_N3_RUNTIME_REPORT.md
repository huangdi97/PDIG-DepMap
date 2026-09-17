# HARMONY_N3_RUNTIME_REPORT.md

> Harmony 运行时报告。生成时间：2026-09-17（Asia/Shanghai）
> 口径：§N —— **不得用 ArkTS 编译成功代替 runtime PASS**。编译 ≠ 运行。

---

## 0. 结论（按 §N 强制拆分）

```
HARMONY_SOURCE_READY = READY
HARMONY_BUILD_READY  = PASS
HARMONY_RUNTIME_E2E  = RUNTIME_NOT_RUN
```

`HARMONY_RUNTIME_E2E` 不是"跑了失败"，而是**根本没有可跑的目标**。

---

## 1. 运行时环境实测

| 项 | 命令 / 位置 | 实测结果 |
| --- | --- | --- |
| hdc 可执行文件 | `.../sdk/default/openharmony/toolchains/hdc.exe` | 存在 |
| **已连接目标** | `hdc list targets` | **`[Empty]`** |
| Emulator 二进制 | `DevEco Studio/tools/emulator/Emulator.exe` | 存在（含 Qt 依赖） |
| **模拟器系统镜像** | 检索 `AppData/Local`、`<HUAWEI_HOME>`、`%USERPROFILE%/Huawei` 等 | **不存在**；`*.img` / `*.qcow2` 全部无命中 |

**blocker 性质**：模拟器系统镜像只能通过 **DevEco Studio GUI 下载**（通常需华为账号登录）。
这与 Android 侧"需在 SDK Manager 安装 system-image"（B18）同类，属**用户侧外部闸门**，
不是工程缺口 —— 因此**不用代码去填**，按 §N 如实拆分报告。

---

## 2. BUILD（已真实发生）

`HARMONY_BUILD_READY = PASS` —— 这是本轮**唯一**取得正向运行时/工具链证据的一项。

```
> hvigor Finished :entry:default@CompileArkTS... after 5 s 936 ms
> hvigor Finished :entry:default@PackageHap... after 503 ms
> hvigor BUILD SUCCESSFUL in 12 s 247 ms
```

| 项 | 值 |
| --- | --- |
| 产物 | `entry-default-unsigned.hap` |
| bytes | 60,133 |
| sha256 | `ac86a5af1a7f15d2ddba70b139b4cbe862d3d1af2898efa496ac05801f9c00fa` |
| 签名 | **未签名**（无 signingConfig —— 与 Android 侧缺生产 keystore 同类的外部闸门） |
| Domain 代码确已打包 | HAP 内含 `"is not in the runtime registry"` 与 `relations: funding_source=` → true |

> **HAP 非确定性**：两次全清重建均得 60,133 B，但 sha256 不同
> （`ec932550…` vs `ac86a5af…`），说明包内嵌有时间戳类字段。
> 与 Android debug APK 同性质；做哈希取证时必须意识到这一点。

---

## 3. E2E 行程（§N 清单）—— 全部 NOT_RUN

以下每一项都**没有在 Harmony 上跑过**：

| # | 行程 | 状态 |
| --- | --- | --- |
| 1 | fresh install | NOT_RUN |
| 2 | launch | NOT_RUN |
| 3 | lock / unlock | NOT_RUN |
| 4 | import | NOT_RUN |
| 5 | proposal | NOT_RUN |
| 6 | confirm reality | NOT_RUN |
| 7 | impact | NOT_RUN |
| 8 | changeplan | NOT_RUN |
| 9 | done ≠ verified | NOT_RUN |
| 10 | verification | NOT_RUN |
| 11 | restart（进程重启后数据仍在） | NOT_RUN |
| 12 | backup | NOT_RUN |
| 13 | restore | NOT_RUN |

---

## 4. 已提前规避的一个坑（§M，D-16 教训）

Harmony 侧已按 §M 设计方向写进代码注释与分层约束：

- 外部文件选择器（`@ohos.file.picker`）会运行在**独立任务 / 独立 UIAbility**，
  等价于 Android 的 `Activity.onStop → LockGate.lockNow()`。
- 因此 N3 **不得**把 Import / Restore 的待投递结果只挂在页面级组件状态里；
  协调职责由 **application 层 `FileWorkflowCoordinator`** 承担（尚未实现）。
- `EntryAbility.ets` 已在注释中固化该约束，避免后续实现者重演 D-16。

**现状**：该 coordinator 尚未实现（application 层 = 未开工），故此项为**设计已定、实现未落地**。

---

## 5. 解除 RUNTIME_NOT_RUN 的前置条件

1. 在 DevEco Studio 中下载并启动一个 Harmony 模拟器系统镜像（**用户操作**）；或接入真机。
2. 解决 HAP 签名（当前 unsigned，等同 Android 侧缺生产 keystore 的外部闸门）。
3. 实现 §L 的核心纵向链页面（17 个页面）与 §M 的 workflow coordinator。

满足 1 之后，才可能产出 `HARMONY_RUNTIME_E2E` 的真实 PASS / FAIL 判定。
