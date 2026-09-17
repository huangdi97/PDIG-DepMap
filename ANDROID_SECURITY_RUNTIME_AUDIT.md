# ANDROID_SECURITY_RUNTIME_AUDIT.md

> 基于**本轮重新构建的真实 APK**（`app-debug.apk`）与**真机运行时取证**，不是源码推测。

更新时间：2026-09-15

---

## 1. 被审计对象

| 项 | 值 |
| --- | --- |
| 文件 | `android/app/build/outputs/apk/debug/app-debug.apk` |
| 大小 | 36,794,370 B |
| SHA-256 | `d84d8900c66e781694a4b71d9782796d858edd93a48156bf070319c1d6230879` |
| 包名 | `com.pdig.app` |
| versionCode / versionName | 1 / `0.1.0-milestone` |
| minSdk / targetSdk / compileSdk | 26 / 34 / 34 |

---

## 2. 权限（`aapt2 dump badging` 实测）

| 权限 | 用途 |
| --- | --- |
| `android.permission.USE_BIOMETRIC` | 生物识别解锁（应用锁） |
| `android.permission.USE_FINGERPRINT` | 同上（旧 API 兼容） |
| `com.pdig.app.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | AndroidX 自动注入，非应用主动申请 |

**未申请的关键权限（实测确认不存在）**：

`INTERNET`、`ACCESS_NETWORK_STATE`、`READ_EXTERNAL_STORAGE`、`WRITE_EXTERNAL_STORAGE`、
`CAMERA`、定位系列、`READ_CONTACTS`、`READ_SMS`。

→ **应用不具备网络能力**，因此数据无法被发送到任何服务器。

`uses-feature` 仅 `android.hardware.faketouch`（隐式）。

---

## 3. 清单安全属性（`aapt2 dump xmltree` 实测）

| 属性 | 实测值 | 评价 |
| --- | --- | --- |
| `android:debuggable` | **true** | ⚠️ 这是 **debug 构建**的预期值；**release 构建未验证**，release 产物必须复查此项 |
| `android:allowBackup` | **false** | ✅ 关闭系统备份 |
| `android:fullBackupContent` | **false** | ✅ 无明文备份内容 |
| `android:dataExtractionRules` | 已设置（`@0x7f0f0000`） | ✅ Android 12+ 导出规则已配置 |

### 导出组件

清单中导出组件实测：

- `exported=true` × 2
- `exported=false` × 1
- 另有 1 个组件带 `android:permission="android.permission.DUMP"`（AndroidX ProfileInstaller 注入）

⚠️ **诚实说明**：本轮只统计到数量与权限属性，**未逐个定位这 2 个 `exported=true` 组件分别是什么**
（其中至少 1 个是带 `MAIN`/`LAUNCHER` 的 `MainActivity`，属预期）。
逐组件归属审计列为未完成项，不宣称"导出面已完全审计"。

---

## 4. 密钥与存储（真机取证）

### 4.1 Android Keystore：原始密钥不明文落盘

从设备 `run-as com.pdig.app cat shared_prefs/pdig_secure.xml`：

```xml
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <string name="db_passphrase_wrapped">3pc7TCY/O3Rmb1ay60ZQTlh+S4Xmt1d/mFyTsVhWlppe2LeDUzMwwbMwtA+IcaBqavlQdQj0Vrlj
+6TW
    </string>
</map>
```

→ shared_prefs 中**只有 Keystore 包装后的密文 blob**，没有任何明文库口令。
→ 该密文串**未出现在应用日志中**（见 §5）。

### 4.2 SQLCipher：明文 SQLite 无法读取

- 从设备拉出 `files/pdig.db`，**176,840 B**
- 头 16 字节：`9d7e2a434e45335301c2a2e0cebbdc4c`
  → **不是** `SQLite format 3\0` 魔数
- 用平台 `sqlite3` 打开：`Error: unable to open database file`
- 设备内 `PersistenceEvidenceTest.sqlcipher_plainSqliteCannotRead` 通过

### 4.3 迁移与事务

设备内 `PersistenceEvidenceTest` 8/8 通过，覆盖：
开关重开持久化、错误密钥拒绝、明文 SQLite 不可读、事务回滚无半写、
v1→v3 / v2→v3 迁移保真、未来 schema 版本 fail-closed、失败回滚不抹库。

### 4.4 `.depmap` 容器

设备内 `DepmapRuntimeEvidenceTest` 4/4 通过：
导出→导入字节一致、错误口令拒绝、密文被篡改拒绝、v1/v2 payload 迁移到同一 v3 图。

---

## 5. 日志脱敏（logcat 进程归属扫描）

方法：`pidof com.pdig.app` → `adb logcat -d --pid=<pid>`，
并按 uid 交叉校验；同时用 `db_passphrase_wrapped` 的真实密文串回查日志。
**断言 app 日志行数 > 0**，避免"没抓到 = 没命中 = 假 PASS"。

本轮结果：`appLines=44`

| 关键字 | 命中数 |
| --- | --- |
| `db_passphrase_wrapped` | 0 |
| `sqlcipher` | 0 |
| `passphrase` | 0 |
| `password` | 0 |
| `begin pgp` | 0 |
| `private key` | 0 |

→ **PASS**。

### 关于上一轮的误报

上一版扫描器对**整份 logcat** 做关键字匹配，命中 1 处 `password` 就判 FAIL。
核查确认该命中来自 Launcher3 的 `AccessibilityNodeInfo(password:false)` 系统字段，
**不是本应用输出**。修正为进程归属扫描后该命中消失；原始误报说明保留为审计证据。

---

## 6. 网络与第三方 SDK

| 检查项 | 结果 |
| --- | --- |
| 是否申请 `INTERNET` | **否** |
| 是否有网络请求库（OkHttp/Retrofit 等） | **无**（依赖白名单审计，`app/build.gradle.kts` 中不存在） |
| 是否有 analytics / crash 上报 SDK | **无** |
| 是否含广告 SDK | **无** |

→ 无网络栈、无遥测，符合 local-first 设计。

---

## 7. 未完成的审计项（如实列出）

| 项 | 状态 | 说明 |
| --- | --- | --- |
| 导出组件逐个归属审计 | **未完成** | 只统计到数量与权限，未逐个定位 |
| release 构建 `debuggable` | **未验证** | 只验证了 debug APK；release APK 未构建 |
| 明文流量 `usesCleartextTraffic` | **未验证** | manifest 中未显式声明；因无 INTERNET 权限，风险已被消除，但未作为独立项取证 |
| 临时文件 / 剪贴板 / 最近任务内容残留 | **未审计** | 本轮未执行 |
| Root/越狱设备下的内存读取 | **不在范围** | FLAG_SECURE 不防御此类威胁，已在代码注释中明确 |

---

## 8. Gate

| Gate | 状态 |
| --- | --- |
| `ANDROID_PRIVACY_SECURITY_AUDIT` | **PARTIAL_WITH_REPORT** |
| `ANDROID_LOGCAT_PRIVACY` | **PASS** |
| 理由 | 权限面、备份策略、Keystore 包装、SQLCipher 加密、日志脱敏均已实测通过；但导出组件逐个归属、release debuggable、临时文件残留等项未完成审计 |
