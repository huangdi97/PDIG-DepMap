# PDIG 0.3.0 Release Notes

> 个人数字依赖图（Personal Digital Dependency Graph）· Developer Preview（Pre-release）
> 日期：2026-09-26 · tag `product-v0.3.0`

## 版本主题：换手机号（Make-Before-Break）与基础设施薄弱点

v0.3.0 把 MVP 从"只模拟换卡/注销银行卡"扩展到**身份与恢复**：
- 新场景 **更换手机号**（replace_phone_number）：先建立并验证新手机号的恢复路径，**才能**停用旧手机号（Make-Before-Break，禁止先拆后建）。
- 新页面 **基础设施薄弱点**：唯一恢复来源（SPOF）、共享故障点、恢复循环——全部只依据**已确认的现实关系**生成。
- 能力从 payment 扩展到 **payment / access / authentication / recovery / communication** 五类；新增 **恢复（recovers）/ 登录认证（authenticates）/ 控制权（controls）** 三种关系。
- 变更计划动作之间显示**前置关系的人话**：必须先完成 / 完成后才能继续 / 等待验证 / 可以并行处理 / 验证后才能移除旧路径。

## 平台

| 平台 | 构建 | 测试 | 运行时证据 |
| --- | --- | --- | --- |
| Windows Desktop (x64) | ✅ installer + portable | smoke 17/17 PASS | 53 屏 light/dark 截图（1280×720 / 1920×1080 / 2048×1152） |
| Android (API36) | ✅ APK + AAB（non-prod 签名） | conformance 128/128；instrumentation 61/61 PASS | 42 屏 light/dark（含 Findings / 更换手机号） |
| HarmonyOS | ✅ ArkTS 主机 conformance | host 179/179 PASS；canonical 124/128（4 条为设备门禁） | —（无模拟器镜像，外部 Gate） |
| iOS (N4 SwiftUI) | ✅ swift build（macOS CI） | canonical **128/128 PASS**；PDIGAppTests 10/10 PASS | macOS 渲染截图 light/dark；iPhone 模拟器 boot PASS |

## 兼容性与数据

- `.depmap` 容器协议 **DEPMAP_CONTAINER_V1 不变**；payload 仍为 schema v3，应用逻辑 Schema 升级到 **v4**（新增 failure_domains / provider_policies 表，旧文件自动迁移）。
- 旧 91 条 conformance fixture **逐字节未动**；新增 37 条 v0.3.0 fixture（failure-domain 6 / recovery-cycle 7 / action-dag 7 / make-before-break 3 / temporal 4 / provider-policy 4 / identity-relations 6）。
- 数据仍然**只在本机**：无账号、无云同步、无遥测。

## 安全

- 本地优先不变：SQLCipher / Keystore / Keychain / HUKS；口令从不明文落盘。
- 日志不输出敏感内容；secret scan / dependency audit / SBOM / license 全部通过。

## 已知限制（外部 Gate，非工程缺陷）

- 商店提交（Google Play / App Store / AppGallery）需要开发账号与正式签名——未做实际提交（`EXTERNAL_GATE`）。
- Harmony 模拟器镜像不可用：4 条 canonical 用例（Argon2id 原生 / ArkData）为真实设备门禁。
- iOS 真机 LocalAuthentication / Keychain 访问组未在真机验证（`NOT_RUN`，需 macOS 真机）。
- Windows 安装包未签名（SmartScreen 提示如实披露）。

## 如何验证

1. 从 GitHub Release `product-v0.3.0`（Pre-release）下载安装包或 APK。
2. 导入一份账单（微信 CSV）→ 接受候选对象 → 场景中心选「更换手机号」→ 选旧手机号 → 按计划顺序完成动作，观察「必须先完成 / 等待验证」提示。
3. 首页 → 基础设施薄弱点：查看唯一恢复来源 / 共享故障点 / 恢复循环。
