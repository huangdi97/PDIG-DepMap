# PDIG 0.3.1 Release Notes

> 个人数字依赖图（Personal Digital Dependency Graph）· Developer Preview（Pre-release）
> 日期：2026-09-28 · tag `product-v0.3.1`

## 定位：v0.3.0 的 corrective 修复版

v0.3.1 是 **immutable corrective release**：capability set 与 v0.3.0 完全一致（换手机号 Make-Before-Break、基础设施薄弱点、五类能力），**不包含任何新功能、新场景、新能力或 Domain 扩展**。本版本只收敛 v0.3.0 发布后发现的 Closure 修复项。

## 本次收敛内容（corrective closure）

- **Windows 品牌打包链闭合**：安装器（NSIS）与便携版启动器 `PDIG.exe` 补齐图标与 VERSIONINFO（0.3.1.0 / ProductName=PDIG）；此前发布的 Windows 二进制图标为默认占位。
- **四端版本对齐**：Android production `versionCode 2 / 0.3.1`、preview `200005`；iOS `MARKETING_VERSION 0.3.1 / CURRENT_PROJECT_VERSION 2`；HarmonyOS `1000001 / 0.3.1`；Desktop `0.3.1`。
- **版本元数据与文档**：Release Notes、网站版本展示（0.3.1 + corrective 说明）、变更审计基线（`V0_3_0_TO_V0_3_1_CHANGE_AUDIT.md`）、发布基线（`PRE_RELEASE_BASELINE.md`）。
- **证据门禁复核**：核心门禁（`npm run check`、conformance 128/128、schema-v4 回归、desktop smoke/profiles/keys、Android/Harmony/iOS 既有证据 + provenance 复核）全部收敛到 RC SHA。

## 平台

| 平台                  | 构建                          | 测试                                                   | 运行时证据                                                |
| --------------------- | ----------------------------- | ------------------------------------------------------ | --------------------------------------------------------- |
| Windows Desktop (x64) | ✅ installer + portable       | smoke 17/17 PASS                                       | 品牌：PDIG.exe / setup.exe 图标 + VERSIONINFO 0.3.1.0     |
| Android (API36)       | ✅ APK + AAB（non-prod 签名） | conformance 128/128；instrumentation 61/61 PASS        | 42 屏 light/dark（既有证据 + provenance 复核）            |
| HarmonyOS             | ✅ ArkTS 主机 conformance     | host 181/181 PASS（H1/H2 为真实 NAPI EXTERNAL_GATE）    | —（无模拟器镜像，外部 Gate）                              |
| iOS (N4 SwiftUI)      | ✅ swift build（macOS CI）    | canonical 128/128 PASS；PDIGAppTests 10/10 PASS        | macOS 渲染截图 light/dark；iPhone 模拟器 boot PASS        |

## 兼容性与数据

- `.depmap` 容器协议 **DEPMAP_CONTAINER_V1 不变**；payload schema v3 冻结；应用逻辑 Schema v4 不变（本版本不改动任何 schema / canonical semantics）。
- 旧 tag `product-v0.2.0`、`product-v0.3.0` 与 v0.3.0 Release 均保持 immutable，无历史改写。
- 数据仍然**只在本机**：无账号、无云同步、无遥测。

## 已知限制（与 v0.3.0 相同的外部 Gate，非工程缺陷）

- 商店提交（Google Play / App Store / AppGallery）需要开发账号与正式签名——未做实际提交（`EXTERNAL_GATE`）。
- Harmony 模拟器镜像不可用：Argon2id 原生 / ArkData 用例为真实设备门禁（真实 NAPI EXTERNAL_GATE）。
- iOS 真机 LocalAuthentication / Keychain 访问组未在真机验证（`NOT_RUN`，需 macOS 真机）。
- Windows 安装包未签名（SmartScreen 提示如实披露）。
- 真实世界试点与真人 screen reader 验证为 deferred 项，不阻塞 corrective release。

## 如何验证

1. 从 GitHub Release `product-v0.3.1`（Pre-release）下载安装包 / 便携版 / APK。
2. 桌面安装包与便携版 `PDIG.exe`：文件属性应显示版本 0.3.1.0、产品名 PDIG，快捷方式带 PDIG 图标。
3. 其余功能验证步骤与 v0.3.0 一致（换手机号 Make-Before-Break 流程、基础设施薄弱点）。