# PERMISSION_AUDIT.md — 权限审计（RC PHASE AK）

> 逐权限说明必要性。原则：无用途即删除。

## Android（platforms/android/core/src/main/AndroidManifest.xml + app/manifest.json）

| 权限                                                                    | 必要性                                           |
| ----------------------------------------------------------------------- | ------------------------------------------------ |
| `android.permission.USE_BIOMETRIC`                                      | 启动锁（BiometricPrompt 系统认证）——核心安全功能 |
| （无 INTERNET）                                                         | 明确不声明：应用无任何网络需求                   |
| allowBackup=false / fullBackupContent=false / dataExtractionRules=@null | 防止系统备份带出加密库+密钥组合                  |

审查结论：1 项权限，全部必要；无危险权限、无存储/短信/联系人。

## iOS（app/manifest.json privacyDescription + platforms/ios）

| 项                       | 必要性                                                     |
| ------------------------ | ---------------------------------------------------------- |
| NSFaceIDUsageDescription | 启动锁（Face ID 提示文案）                                 |
| NSCameraUsageDescription | 仅用户主动扫码导入场景；若 MVP 不做扫码可在 Xcode 工程删除 |
| entitlements             | 空（无 push/iCloud/keychain-sharing 群组需求）             |

## HarmonyOS（platforms/harmonyos/entry/src/main/module.json5）

| 项                 | 必要性                                             |
| ------------------ | -------------------------------------------------- |
| requestPermissions | 空数组（用户认证由系统 widget 完成，无需额外权限） |

审查结论：零业务权限。

## 后台/隐私行为

- Android：无后台服务、无前台服务类型
- iOS：UIBackgroundModes 空
- HarmonyOS：无 backgroundModes
