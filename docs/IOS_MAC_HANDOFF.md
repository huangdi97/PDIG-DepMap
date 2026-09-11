# IOS_MAC_HANDOFF.md — iOS Mac 交接（RC PHASE X）

> 本机非 macOS：不伪编译。本文档使拿到 Mac 后可直接 build/test。
> iOS 侧代码：`platforms/ios/swift/SQLCipherSecureDatabaseAdapter.swift`（单文件包含
> DB 适配 + Keychain + LocalAuthentication + PrivacyScreen 占位）、
> `platforms/ios/Package.swift`（SPM）、`platforms/ios/Tests/DepMapCoreTests/`（XCTest）。

## Mac 上的第一步（15 分钟）

```bash
# 1. Swift Package 测试（不需要 SQLCipher 的部分）
cd platforms/ios
swift test          # DepmapContainerV1Tests 当前为 XCTSkip 占位，见下

# 2. golden vector 互操作（拿 Mac 后的第一优先级）
```

## golden 互操作（预期工作量 ~1 天）

`DepmapContainerV1Tests.swift` 三个 skip 测试需要在 SPM target 中接入：

1. Argon2id：加入 C 参考实现（推荐 `https://github.com/P-H-C/phc-winner-argon2`，
   sphinxheader 已是 public domain），Swift wrapper ~80 行；
   或用 `swift-argon2` 包。**验收标准 = 复现冻结向量**
   （`core/src/crypto/golden.ts`：password 'depmap-test' + 固定 salt/nonce →
   derivedKey `66c4be…0c86` / ciphertext / tag 三值一致）。
2. AES-256-GCM：CryptoKit `AES.GCM`（要拆 auth tag 与密封输出——CryptoKit 自带
   combined/separated API）。
3. Base64：`Data(base64Encoded:)` / `base64EncodedString()`（RFC 4648 带填充，一致）。
4. AAD：CryptoKit `.seal(data, key, nonce)` 不直接支持 AAD → 需用
   `AES.GCM.seal(..., authenticating: aad)` API（存在）。

互操作矩阵（CANONICAL §8.9）：Node encrypt → iOS decrypt PASS；iOS encrypt → Node decrypt PASS。

## 工程收尾（App target）

1. Xcode 新建 App（bundle id 待定，占位 `com.example.depmap`）→ 加 DepMapCore SPM 依赖。
2. SQLCipher：`https://github.com/sqlcipher/sqlcipher` 经 SPM/XCFramework 引入；
   `SQLCipherSecureDatabaseAdapter.swift` 从 SPM exclude 移回 app target。
3. PrivacyInfo.xcprivacy：声明无 tracking、无 required-reason API 之外的收集
   （accessreason：CA92.1 本机数据）。
4. Info.plist usage strings 已在 app/manifest.json privacyDescription：
   NSFaceIDUsageDescription（解锁）、NSCameraUsageDescription（扫码导入）——复制进 Xcode。
5. 真机跑安全 spike（CANONICAL §8.3）：离线打开 DB 不可读 / 错 key 失败 /
   认证取消不进数据层 / 重启不丢 key / 后台隐私遮罩。

## 状态口径

XCTest 就绪（skip 占位标明原因）；拿到 Mac 并按上表完成后，才允许把 iOS 标为
COMPILED / TESTED；DEVICE_VERIFIED 需真机。
