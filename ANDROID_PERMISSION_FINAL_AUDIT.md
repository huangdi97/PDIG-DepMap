# ANDROID_PERMISSION_FINAL_AUDIT.md

> 审计时间：2026-09-20（Android Product Finalization）
> 审计对象：`android/app/src/main/AndroidManifest.xml` + `res/xml/data_extraction_rules.xml`
> 原则：**最少权限**（spec §153）。产品无业务网络、无 analytics、无 telemetry（spec §130）。
> 结论：**ANDROID_PERMISSION_AUDIT = PASS**（清单极简，无可移除或滥用的权限）。

---

## 1. 权限清单（声明即全量）

| #   | 权限                                 | 用途                                                     | 是否必要     | 说明                                                                                                              |
| --- | ------------------------------------ | -------------------------------------------------------- | ------------ | ----------------------------------------------------------------------------------------------------------------- |
| 1   | `android.permission.USE_BIOMETRIC`   | App Lock 生物识别验证（`BiometricPrompt`，`AppLock.kt`） | **必要**     | App Lock 是产品核心安全门（spec §155）；该权限用于调用系统生物识别（指纹/人脸）。                                 |
| 2   | `android.permission.USE_FINGERPRINT` | 兼容旧 API 的指纹权限（与 USE_BIOMETRIC 并存）           | 必要（兼容） | API 28 及以下需要 USE_FINGERPRINT；与 USE_BIOMETRIC 同时声明是 Android 官方推荐写法，目标设备主走 USE_BIOMETRIC。 |

**未声明（有意缺失）**：

| 权限                                                           | 缺失原因                                                                                                                                                            |
| -------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `android.permission.INTERNET`                                  | 产品**无任何业务网络**、无 analytics、无 telemetry、无广告 SDK（spec §130 / §153）。缺失即结构性保证：应用无法发起网络请求。                                        |
| `READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE`             | 导入走 **SAF `OpenDocument`**（用户显式选择文件，`ACTION_OPEN_DOCUMENT`，只申请读取且可持久化 URI 权限），导出走 **MediaStore `Downloads`**；两者都不需要存储权限。 |
| `CAMERA` / `RECORD_AUDIO` / `LOCATION` / `CONTACTS` / `SMS` 等 | 产品无对应能力，一律不声明。                                                                                                                                        |
| `RECEIVE_BOOT_COMPLETED`                                       | 无开机自启需求。                                                                                                                                                    |

---

## 2. 组件出口（exported）审计

| 组件           | exported | 说明                                                                                                                                                                       |
| -------------- | -------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `MainActivity` | `true`   | **唯一** Activity；带 `MAIN`/`LAUNCHER` intent-filter，home 图标入口，必须 exported。manifest 中**没有**其它 intent-filter（无深链），因此不存在被外部任意调起的深层页面。 |

其它组件（service / receiver / provider）：**manifest 中一个都没有** —— 无后台服务、无广播接收器、无 ContentProvider（SQLCipher 数据由 app 私有进程直接持有，不暴露给其它 app）。

### 3. 敏感数据通道

| 通道                                   | 存在？ | 说明                                                                            |
| -------------------------------------- | ------ | ------------------------------------------------------------------------------- |
| FileProvider                           | 无     | 不对外分享文件，无 `<provider>`。SAF 导出走 MediaStore，导入走 OpenDocument。   |
| 深链（deep link / intent-filter data） | 无     | 除 LAUNCHER 外无任何 intent-filter；锁定时 NavHost 不参与组合，深链天然不可达。 |
| 剪贴板自读                             | 无     | 代码中无 ClipboardManager 使用（见 temp/clipboard 审计）。                      |

---

## 4. 备份标志

- `android:allowBackup="false"`（显式关闭云备份）
- `android:fullBackupContent="false"`
- `res/xml/data_extraction_rules.xml`：`cloud-backup` 与 `device-transfer` **全部 exclude**（root/database/sharedpref/file/external）。
  - 效果：加密数据库 + 密钥链**永不进入** Android Auto Backup / 设备间迁移通道（spec §129）。

---

## 5. 网络 / 明文流量

- `android:usesCleartextTraffic="false"`（显式声明；targetSdk≥28 默认即 false，这里写出来作为纵深防御并可审计）
- 无 INTERNET 权限 → 明文/HTTP 流量在权限层面即不可行。

---

## 6. debuggable / testOnly

- Debug 变体：AGP 默认 `debuggable=true`，仅开发用。
- Release 变体：`minifyEnabled`/`debuggable=false`（release 构建由 AGP 默认关闭 debuggable），见 `app/build.gradle.kts` 与 `ANDROID_PRODUCT_FINAL_ACCEPTANCE.md` 的 build 区。
- `queries` 元素：未声明任何 `queries`（无包可见性查询需求）。

---

## 7. 运行时权限

应用在运行时**不请求任何运行时权限**：

- 导入：SAF `ACTION_OPEN_DOCUMENT` —— 由系统 DocumentsUI 授权，应用只获得单个文件的读 URI。
- 导出：MediaStore `Downloads`（API 29+ 无需权限；MIN_SDK=26 时代码路径见导出，未使用 WRITE_EXTERNAL_STORAGE）。

因此不存在"用户在关键路径被权限弹窗打断"的 UX 问题，也没有权限撤销后的悬空状态。

---

## 8. 结论

```
权限最小化：PASS（2 个认证类权限，无网络/存储/位置/通讯类权限）
组件暴露：PASS（唯一 exported 组件 = 带 LAUNCHER 的 MainActivity）
备份出口：PASS（allowBackup=false + data extraction rules 全 exclude）
明文流量：PASS（无 INTERNET + usesCleartextTraffic=false）
运行时权限：PASS（无任何运行时请求）
```
