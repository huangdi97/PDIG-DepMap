# ANDROID_STORE_COMPLIANCE_REPORT.md

> 生成时间：2026-09-23（ANDROID_2026_PRODUCTION_REALITY_CLOSURE · API36 工程全速收口）
> 依据：粘贴 Goal §52/§53/§54/§56 + 批准契约 F2。
> 原则：**以真实代码为准，不以营销文案为准**；所有结论来自本机实查（manifest、源码、依赖树）。

---

## 0. 结论

| 项 | 结论 |
|----|------|
| 网络权限 | **无 INTERNET**（manifest 实查 + 源码扫描 0 引用） |
| 存储权限 | **无**（FileProvider + SAF 按需授权，不申请 storage 权限） |
| 生物识别权限 | `USE_BIOMETRIC` + `USE_FINGERPRINT`（App Lock 用，用户明确授予用） |
| 通知权限 | **无**（不请求） |
| Analytics / Telemetry / Crash SDK | **无**（Firebase/OkHttp/Retrofit/Crashlytics 等 0 引用） |
| 后台任务 / 定位 / 相机 / 麦克风 / 通讯录 | **无** |
| 数据收集与分享 | **不在应用外收集**；无账号、无云同步、无广告 SDK |
| 声明状态 | `DATA_SAFETY_READY = READY`（表单内容见 store/DATA_SAFETY_DRAFT.md，Play Console 填写依赖账号，E-5） |
| Content Rating | **待填写 IARC 问卷**（需在已创建的 Play App 内真实作答；本轮无账号 → `CONTENT_RATING = BLOCKED_BY_STORE_ACCOUNT`） |

---

## 1. 权限清单（基于 merged release manifest 实查）

`processReleaseMainManifest` 合并结果（`C:\Users\Kaiser\pdig-build\app\intermediates\merged_manifest\release\...`）：

```xml
<uses-permission android:name="android.permission.USE_BIOMETRIC" />
<uses-permission android:name="android.permission.USE_FINGERPRINT" />
<uses-permission android:name="com.pdig.app.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION" />
```

- 前两项：`MainActivity` 使用 BiometricPrompt（App Lock）必需；MVVM/产品语义要求锁不可绕过。
- 第三项：AndroidX 自动生成的「动态 receiver 不导出」保护权限（androidx.core 内部机制，
  由 `androidx.core.content` 在 targetSdk>=34 时自动注入保护导出状态；非用户可见、非敏感权限）。
- **不出现在清单中的权限**（均未请求）：INTERNET、READ/WRITE_EXTERNAL_STORAGE、POST_NOTIFICATIONS、
  CAMERA、RECORD_AUDIO、ACCESS_FINE/COARSE_LOCATION、READ_CONTACTS、READ_SMS、
  QUERY_ALL_PACKAGES、FOREGROUND_SERVICE 等。
- 文件选择：只用 `ACTION_OPEN_DOCUMENT`（SAF），授权以用户显式选择为准，不申请存储权限。

### 权限逐项判定（粘贴 Goal §56 要求：needed / explained / policy compatible）

| 权限 | needed | explained | policy compatible |
|------|--------|-----------|-------------------|
| USE_BIOMETRIC | ✅ App Lock | ✅ 设置页/锁屏明确说明 | ✅ |
| USE_FINGERPRINT | ✅（旧版本兼容别名） | ✅ | ✅ |
| （无）INTERNET | N/A（不申请） | N/A | ✅（无网络行为） |
| （无）POST_NOTIFICATIONS | N/A | N/A | ✅ |

---

## 2. Data Safety（按真实代码事实逐项）

| Data safety 字段 | 真实状态 | 证据 |
|------------------|----------|------|
| 是否收集任何个人数据 | **否（应用内不收集、不上传）** | 无 INTERNET 权限；无登录/账号；无 analytics SDK |
| 数据是否分享/出售 | **否** | 无分享代码、无 SDK |
| 是否允许删除数据 | **是（本机删除）** | 「删除所有数据」在 Settings/About（L-37 已实现并设备验证） |
| 数据加密 | **是** | SQLCipher（net.zetetic 4.5.5）+ Android Keystore/Biometric |
| 数据保留 | **仅本机**；导入原始解析仅会话内存 | AGENTS §12 / REAL_DATA_PRIVACY_PROTOCOL |
| 备份/导出 | 用户主动导出 `.depmap`（DEPMAP_CONTAINER_V1，AES-256-GCM + Argon2id） | Backup/Restore 功能已实现 |
| 网络行为 | **无网络**：无 INTERNET 权限、无 DNS/外连代码 | 见下方 §3 |
| 广告 | **无** | 无广告 SDK（依赖树 0） |
| 儿童向/Family | **非儿童向**（工具/财务依赖梳理） | IARC 问卷时如实作答（需账号） |

---

## 3. 网络与第三方 SDK 审计（粘贴 Goal §52）

### 3.1 全配置 `:app` releaseRuntimeClasspath 依赖树（实查 Groovy 输出，2026-09-23）

- 直接依赖：kotlin-stdlib 2.0.0、project :core（→ bcprov-jdk18on 1.78.1）、
  compose-bom 2024.09.02（ui 1.7.2 / material3 1.3.0 / material-icons-core）、
  activity-compose 1.9.1、navigation-compose 2.8.1、lifecycle-viewmodel/runtime-compose 2.8.5、
  biometric 1.1.0、fragment 1.7.1（显式钉住）、security-crypto 1.1.0-alpha06、
  sqlite-framework 2.4.0、sqlcipher-android 4.5.5、core-ktx 1.13.1、kotlinx-coroutines-android 1.8.1。
- **无以下任何条目**：OkHttp、Retrofit、Volley、Firebase（全部）、Crashlytics、Sentry、AppsFlyer、
  Adjust、MoPub/AdMob 等广告、Play Services Analytics、Play Core Telemetry SDK。
- `:core` 仅依赖 bouncycastle（密码学原语），`:conformance` 仅 JVM 测试工具，无网络类。

### 3.2 源码扫描

- `Get-ChildItem android/app/src/main -Recurse -Include *.kt,*.xml | Select-String "INTERNET|http://|https://|OkHttp|Retrofit|Firebase|analytics|Crashlytics|telemetry"`
  → 命中仅：注释说明「无 INTERNET/无 analytics」（PdigApplication.kt、AndroidManifest.xml 注释）、
  以及 vector/资源 XML 的 `http://schemas.android.com/apk/res/android` 命名空间。
  **没有任何运行时网络调用或 SDK 引用。**

---

## 4. App Content（Play Console 实际表单，逐项标注可填性与阻塞）

> 本轮**未创建 Play 应用**（E-3/E-5：applicationId OPEN + 无开发者账号），以下为可预填内容与
> 填写方式；正式填写在账号存在后按 `ANDROID_PLAY_CONSOLE_READINESS.md` 完成。

| Play 表单项 | 本轮状态 | 正式填写建议 |
|-------------|----------|--------------|
| 隐私政策 URL | DRAFT（内容就绪，无公网 URL）→ BLOCKED_BY_PUBLIC_URL | 发布 PRIVACY_POLICY_DRAFT 到公网后回填 |
| 广告声明 | **无广告**（真实代码 0 广告 SDK） | 勾选「不会」 |
| App access | 无需账号登录（本地 App）；若 reviewer 需要访问指引，见 `ANDROID_PLAY_CONSOLE_READINESS.md` §Reviewer | 不虚构测试账号 |
| Target audience | 工具/财务（成人），非儿童向 | 问卷时如实作答 |
| Content rating（IARC） | 未作答（需账号内完成） | 首次上传前完成，否则 Play 拒审 |
| Data safety | 内容 READY（本报告 §2） | 按 Data Safety 表单逐项填写（与本文一致） |
| 高敏权限声明 | 无高敏权限（仅 biometric，含说明） | 如实声明 USE_BIOMETRIC |
| News 声明 | 非 News 应用 | 如实选择 |

---

## 5. 已知与未知（如实标注）

| 项 | 状态 |
|----|------|
| `DATA_SAFETY_READY` | READY（内容）；Play 表单提交依赖账号 |
| `PLAY_APP_CONTENT_READY` | PARTIAL：内容已可预填；**最终提交在 Play App 内**（E-5） |
| `CONTENT_RATING` | BLOCKED_BY_STORE_ACCOUNT（IARC 问卷需在 App 内完成，本轮无账号） |
| 隐私 URL | BLOCKED_BY_PUBLIC_URL（E-6） |
| 支持 URL / 联系邮箱 | DRAFT 就绪（store/SUPPORT_PAGE_DRAFT.md）；公网 URL BLOCKED |

---

## 6. 结论

```text
ANDROID_STORE_COMPLIANCE_REPORT = PASS（内容正确、与真实代码一致；无违规声明）
DATA_SAFETY_READY = READY（待账号填写表单）
PLAY_APP_CONTENT_READY = PARTIAL（BLOCKED_BY_STORE_ACCOUNT，IARC 未作答）
CONTENT_RATING = BLOCKED_BY_STORE_ACCOUNT
```