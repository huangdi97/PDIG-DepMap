# PLATFORM_ADAPTERS.md — 平台适配

> 接口契约：`core/src/adapters/interfaces.ts`（业务核心只依赖接口）
> UTS 插件：`app/uni_modules/depmap-*`；原生实现：`platforms/`

## SecureDatabaseAdapter

| 平台      | 实现                                                                 | 状态                                                           |
| --------- | -------------------------------------------------------------------- | -------------------------------------------------------------- |
| Android   | `SqlCipherSecureDatabaseAdapter.kt`（net.zetetic sqlcipher-android） | IMPLEMENTED；COMPILED/TESTED/DEVICE_VERIFIED = NO              |
| iOS       | `SQLCipherSecureDatabaseAdapter.swift`                               | IMPLEMENTED；COMPILED/TESTED/DEVICE_VERIFIED = NO（无 macOS）  |
| HarmonyOS | `RelationalStoreSecureAdapter.ets`（ArkData encrypt=true, S4）       | IMPLEMENTED；COMPILED/TESTED/DEVICE_VERIFIED = NO（无 DevEco） |

三端执行同一份 Schema v1 DDL；迁移幂等 + 事务 + 回滚语义一致（`docs/SCHEMA_V1.md`）。

## SecureKeyAdapter

- Android：`KeystoreSecureKeyAdapter.kt` — 32B 随机 DB key，由 AndroidKeyStore AES-GCM 包裹后落私有目录；fpSecret 同法
- iOS：`KeychainSecureKeyAdapter.swift` — kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
- HarmonyOS：`HuksSecureKeyAdapter.ets` — cryptoFramework 随机 + HUKS 保护

## BiometricAdapter（启动锁）

- Android：BiometricPrompt（BIOMETRIC_WEAK | DEVICE_CREDENTIAL）；取消/lockout/未注册分别映射 reason
- iOS：LAContext `.deviceOwnerAuthentication`
- HarmonyOS：userAuth（FACE/FINGERPRINT/PIN, ATL1）

取消/失败 → `ok=false` → 调用层不得进入数据层。

## FileCryptoAdapter（.depmap）

- Node reference：`core/src/crypto`（hash-wasm Argon2id + node crypto AES-GCM）—— 已测
- Android：`DepmapContainerV1.kt`（BouncyCastle Argon2id + javax AES-GCM）—— TEST READY 未运行
- iOS：CryptoKit AES-GCM + Argon2id C 移植接入待做（golden vector 已就绪）
- HarmonyOS：cryptoFramework AES-GCM + Argon2id ArkTS 移植待做（golden vector 已就绪）

## PrivacyScreenAdapter

Android FLAG_SECURE / iOS 后台遮罩 / HarmonyOS 窗口隐私模式。

## UTS 插件清单

`depmap-secure-database` `depmap-secure-key` `depmap-biometric` `depmap-file-crypto` `depmap-privacy-screen`
（interface.uts 契约 + app-android/app-ios/app-harmony 三实现，均未编译验证）
