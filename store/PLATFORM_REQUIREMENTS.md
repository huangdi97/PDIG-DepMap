# PLATFORM_REQUIREMENTS.md — 平台与商店技术要求

> 目标平台的最低版本与技术要求。正式发布前需按各平台最新政策复核。

---

## Android

| 项       | 值                                                                      |
| -------- | ----------------------------------------------------------------------- |
| 最低 SDK | 待定（建议 API 26 / Android 8.0，受 SQLCipher 与 BiometricPrompt 影响） |
| 目标 SDK | 需满足当年 Google Play 要求（提交前复核）                               |
| 构建产物 | **AAB**（Google Play 要求）；本地调试可用 APK                           |
| 签名     | release keystore（B4，用户提供）                                        |
| 关键能力 | SQLCipher、Android Keystore、BiometricPrompt、FLAG_SECURE               |
| 权限     | 见 `docs/PERMISSION_AUDIT.md`（最小化）                                 |

## iOS

| 项         | 值                                                |
| ---------- | ------------------------------------------------- |
| 最低版本   | 待定（建议 iOS 15+，受 LocalAuthentication 影响） |
| 构建产物   | `.ipa`（App Store Connect 上传）                  |
| 签名       | 证书 + Provisioning Profile（B8/B9，用户提供）    |
| 关键能力   | SQLCipher、Keychain、LocalAuthentication          |
| Info.plist | 生物识别用途说明需与实现一致                      |

## HarmonyOS

| 项       | 值                                                  |
| -------- | --------------------------------------------------- |
| 最低版本 | 待定（受 ArkData relationalStore 加密与 HUKS 影响） |
| 构建产物 | HAP / App Pack（按 DevEco 实际配置）                |
| 签名     | AGC 证书与 Profile（B6/B7，用户提供）               |
| 关键能力 | ArkData 加密、HUKS、官方用户认证                    |

## Core（Node）

| 项   | 值                                                           |
| ---- | ------------------------------------------------------------ |
| Node | ≥ 22.5.0（实测 v22.22.2）                                    |
| 模块 | ESM                                                          |
| 用途 | 纯逻辑库 + 测试 + CLI（`validate-real-bill.ts`），不单独上架 |

## UI（uni-app x）

| 项     | 值                         |
| ------ | -------------------------- |
| 框架   | uni-app x（UTS / uvue）    |
| 工具链 | **HBuilderX（B10，缺失）** |
| 目标   | Android / iOS / HarmonyOS  |
| 状态   | `UI_BUILD_READY = BLOCKED` |

## 数据兼容

| 项             | 值                                                   |
| -------------- | ---------------------------------------------------- |
| DB Schema      | v3（v1 / v2 → v3 迁移已验证）                        |
| `.depmap` 容器 | `DEPMAP_CONTAINER_V1`，formatVersion = 1             |
| 降级           | **不支持**（新 schema 不得被旧版本打开；已显式拒绝） |

## 提交前复核清单

- [ ] 各平台最低 / 目标版本与当年政策一致
- [ ] 权限列表与实际使用一致
- [ ] 隐私问卷事实与 `store/PRIVACY_DISCLOSURE_MATRIX.md` 一致
- [ ] 构建产物格式符合要求（AAB / ipa / HAP）
- [ ] 签名资产不入 Git
