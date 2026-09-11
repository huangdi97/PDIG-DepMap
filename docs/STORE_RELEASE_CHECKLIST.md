# STORE_RELEASE_CHECKLIST.md — 三端上架清单

> 占位 bundle id 均为 `com.example.depmap`（STORE_RELEASE_INPUTS.md），发布前必须替换。

## Android（Google Play / GitHub Releases）

- [x] applicationId 可配置（占位 `com.example.depmap`）
- [x] versionCode 1 / versionName 0.1.0（manifest.json）
- [x] minimal permissions（仅 USE_BIOMETRIC；无 INTERNET）
- [x] debug build config（Gradle 工程就绪）
- [x] release build config（minify + signing 不入库：`*.keystore` 已 gitignore）
- [ ] **BLOCKER：JDK17 + Android SDK（编译验证）**
- [ ] **BLOCKER：release keystore（用户提供，放 signing/ 不入库）**
- [ ] **BLOCKER：Google Play Developer Account**
- [x] privacy/data safety checklist 草稿（见下）
- [ ] icon/splash 实际素材（占位路径已配置）
- [x] store description draft（见下）

### Data Safety 回答草稿
- 是否收集用户数据：否。数据仅存于设备本地加密数据库。
- 是否共享数据：否。应用无 INTERNET 权限。

## HarmonyOS（AppGallery）

- [x] bundleName 可配置（占位 `com.example.depmap`）
- [x] module 配置（module.json5：entry + 最小权限）
- [x] ArkData/HUKS 适配源码
- [ ] **BLOCKER：DevEco Studio / HarmonyOS SDK（编译验证）**
- [ ] **BLOCKER：Huawei Developer / AppGallery Connect 身份 + release signing（.p12/.cer/.p7b 不入库）**
- [x] privacy draft（见下）

## iOS（App Store）

- [x] bundle identifier 可配置（占位）
- [x] version 0.1.0 / build 1
- [x] minimal entitlements（无特殊 entitlement）
- [x] Keychain / biometric usage strings（NSFaceIDUsageDescription 已写）
- [x] privacy manifest 位置约定（PrivacyInfo.xcprivacy 待 macOS 侧生成）
- [ ] **BLOCKER：macOS + Xcode（编译/签名/真机验证）**
- [ ] **BLOCKER：Apple Developer Account + provisioning**
- [x] App Privacy draft（见下）

### App Privacy 回答草稿
- 不收集任何数据（无标识符、无使用数据、无位置）。所有数据仅本地存储，不传输。

## 三端通用隐私文案草稿

> 个人数字依赖图是一款纯本地应用：没有账号、没有云同步、没有统计埋点、没有广告。
> 你的账单文件只在本机内存中解析，应用不保存原始账单，只保存你确认过的依赖关系摘要与加密备份。
> 数据库使用系统级加密（SQLCipher / ArkData），备份文件使用 Argon2id + AES-256-GCM 口令加密。

## Store 文案草稿

短描述：换卡、换号、注销账户之前，先看清哪些账户和自动扣款会受影响。
长描述：见 README.md「产品」节；发布前由用户最终定稿名称与文案。
