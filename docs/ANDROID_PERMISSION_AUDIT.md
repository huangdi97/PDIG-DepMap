# docs/ANDROID_PERMISSION_AUDIT.md — Android 权限审计

> 审计日期：2026-09-14（会话内实测）
> 对象：`platforms/android/core/src/main/AndroidManifest.xml`（原生安全核心）+ `app/manifest.json`（uni-app x 应用壳）
> 原则：local-first 产品，权限最小化。每一项权限必须有可指出的用途；无用途即删除。

---

## 1. 原生安全核心（`platforms/android/core`）

实际 Manifest 内容（实测）：

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.USE_BIOMETRIC" />
    <application
        android:allowBackup="false"
        android:fullBackupContent="false"
        android:dataExtractionRules="@null"
        android:usesCleartextTraffic="false" />
</manifest>
```

| 权限 / 属性                  | 状态 | 理由                                                                                      |
| ---------------------------- | ---- | ----------------------------------------------------------------------------------------- |
| `USE_BIOMETRIC`              | 保留 | `BiometricGateAdapter`（启动锁）通过 `BiometricPrompt` 调用系统生物识别；核心能力，无替代 |
| `allowBackup=false`          | 保留 | 禁止 ADB/云备份导出加密库；与「本地优先 + 数据库加密」一致                                |
| `fullBackupContent=false`    | 保留 | 同上，显式关闭全量备份内容                                                                |
| `dataExtractionRules=@null`  | 保留 | Android 12+ 的备份/迁移规则显式置空                                                       |
| `usesCleartextTraffic=false` | 保留 | 禁止明文 HTTP；与「业务网络调用 = 0」一致                                                 |

**结论：核心库权限面 = 1 项（`USE_BIOMETRIC`），无 INTERNET / 无存储 / 无短信 / 无联系人 / 无位置 / 无相机。**

### 1.1 未声明但被代码引用的能力（已核对）

| 能力             | 是否需权限 | 说明                                                                   |
| ---------------- | ---------- | ---------------------------------------------------------------------- |
| Android Keystore | 否         | 系统密钥库，无需 Manifest 权限                                         |
| 私有目录文件读写 | 否         | 应用私有目录（`context.filesDir`）不需要 `READ/WRITE_EXTERNAL_STORAGE` |
| SQLCipher 本地库 | 否         | 随 AAR 打包 native `.so`，无权限需求                                   |
| `FLAG_SECURE`    | 否         | Window flag，无需权限                                                  |

---

## 2. 应用壳（`app/manifest.json` → uni-app x 打包产物）

实测 `app/manifest.json` 的 `app.distribute.android`：

```json
"android": {
  "minSdkVersion": 26,
  "targetSdkVersion": 34,
  "abiFilters": ["arm64-v8a"],
  "permissions": [],
  "abiSettings": { "arm64": true }
}
```

| 项                 | 值                | 评价                                                     |
| ------------------ | ----------------- | -------------------------------------------------------- |
| `permissions`      | **`[]` 空数组**   | ✅ 未申请任何 Android 运行时权限                         |
| `minSdkVersion`    | 26（Android 8.0） | 覆盖 Keystore AES-GCM 与 BiometricPrompt 所需 API        |
| `targetSdkVersion` | 34（Android 14）  | 与 `compileSdk 34` 一致                                  |
| `abiFilters`       | 仅 `arm64-v8a`    | 单一 ABI，减小包体；代价是不支持 32 位设备（需产品确认） |

### 2.1 与权限相关的产品行为核对

| 产品行为           | 所需权限                        | 当前状态                                                   |
| ------------------ | ------------------------------- | ---------------------------------------------------------- |
| 导入账单文件       | 文件选择器（SAF）→ **无需权限** | 但导入功能未接入（B20），且 `app/static` 缺失              |
| 生物识别解锁       | `USE_BIOMETRIC`                 | 由核心库声明；应用壳未重复声明（需在集成阶段确认合并结果） |
| 截屏保护           | 无                              | `FLAG_SECURE`                                              |
| 备份导出 `.depmap` | SAF 写文件 → **无需权限**       | 功能未接入（B21）                                          |

---

## 3. 待办（本轮未闭环，明确登记）

| #   | 事项                                                                                                 | 影响                                 |
| --- | ---------------------------------------------------------------------------------------------------- | ------------------------------------ |
| P-1 | **应用壳与核心库的权限合并结果未实测** —— 产品级 APK 尚未产出（B10），无法 dump 最终 merged manifest | `ANDROID_PERMISSION_FINAL` = NOT_RUN |
| P-2 | `app/static/` 目录缺失（图标 / 启动图引用不存在的 PNG）                                              | 打包前必须补齐（B15/B16）            |
| P-3 | `abiFilters` 仅 arm64 —— 是否需保留 32 位由产品决定                                                  | 影响可安装设备范围                   |

---

## 4. 结论

| 判定项                            | 状态                             |
| --------------------------------- | -------------------------------- |
| 原生核心权限最小化                | **PASS**（仅 `USE_BIOMETRIC`）   |
| 应用壳权限声明为空                | **PASS**（`permissions: []`）    |
| 备份 / 明文流量关闭               | **PASS**                         |
| 最终合并 Manifest（产品 APK）实测 | **NOT_RUN**（依赖 B10 产出 APK） |

> 不得把「源码声明为空」表述为「最终 APK 权限已确认」——merged manifest 只能在真实打包后 dump 验证。
