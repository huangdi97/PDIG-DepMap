# IOS_RELEASE_HANDOFF.md — iOS 发布交接手册（Windows → macOS）

> **本文件已于 2026-09-15 依据实测重新审计并重写。**
> 原版本存在与实际不符的表述（称 `Package.swift` 已声明 SQLCipher 目标、称 `swift test` 会全部通过），
> 现按仓库真实状态修正。
>
> 本机为 **Windows（win32）**，无 Xcode、无 Swift 工具链。
> 因此 `IOS_SOURCE_READY` 只能评估到「**结构就绪 + 缺陷已定位**」，
> `IOS_BUILD_READY = BLOCKED（B3）`。
> **本文件不得声称任何 iOS 编译已通过。**

---

## 0. 交接现状总览（实测）

`platforms/ios` 全部内容仅 **3 个文件**：

| 文件                                                 | 状态                                                                               |
| ---------------------------------------------------- | ---------------------------------------------------------------------------------- |
| `Package.swift`                                      | 存在（本轮修正，见 §0.2）                                                          |
| `swift/SQLCipherSecureDatabaseAdapter.swift`         | 存在，291 行，含 SQLCipher 适配器 / Keychain / LocalAuthentication / PrivacyScreen |
| `Tests/DepMapCoreTests/DepmapContainerV1Tests.swift` | 存在，但**三个用例全部 `throw XCTSkip`**                                           |

### 0.1 关键缺口（Mac 侧首要待办）

| #   | 缺口                                          | 影响                                                          |
| --- | --------------------------------------------- | ------------------------------------------------------------- |
| G-1 | **`swift/` 下没有 `DepmapContainerV1.swift`** | iOS 侧**没有 `.depmap` 容器实现**。跨端互操作测试因此无法执行 |
| G-2 | `import SQLCipher` 无对应依赖声明             | `DepMapCore` 无法编译，需外部提供 SQLCipher 模块              |
| G-3 | 测试用例为 `XCTSkip` 占位                     | `swift test` 会「通过」但**零覆盖**，不可作为互操作证据       |

> **G-1 是本阶段最重要的未完成项。** 在此项完成前，不得声称 iOS 容器互操作已实现或已测试。

### 0.2 本轮已修复的 iOS 缺陷

| #   | 缺陷                                                                                                                                                 | 处置                                                                                                   |
| --- | ---------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------ |
| D-1 | `Package.swift` 的 `exclude: ["SQLCipherSecureDatabaseAdapter.swift"]` 使 `DepMapCore` target **无任何源文件**，SPM 无法构建                         | 已移除该 `exclude`。依赖缺失现在会以明确的 `no such module 'SQLCipher'` 暴露，而非隐式的空 target 错误 |
| D-2 | `LocalAuthenticationGate.canAuthenticate()` 中 `let policy = LAPolicy().deviceOwnerAuthentication` —— `LAPolicy` 是 **enum**，不可实例化，属编译错误 | 已改为 `let policy: LAPolicy = .deviceOwnerAuthentication`                                             |

---

## 1. 前置条件（需用户提供）

| 项                           | 说明                                                                                      | Blocker |
| ---------------------------- | ----------------------------------------------------------------------------------------- | ------- |
| macOS + Xcode（≥ 15）        | 构建与签名                                                                                | **B3**  |
| Apple Developer Program 账号 | 证书 / Provisioning / TestFlight                                                          | **B8**  |
| 正式 Bundle ID               | 仓库中**未定义** —— `app/manifest.json` 无 iOS `bundleId` 字段，需在 HBuilderX 打包时配置 | **B11** |
| 正式 App 名                  | 当前「个人数字依赖图」                                                                    | B14     |
| SQLCipher 模块               | 见 §2                                                                                     | —       |
| Argon2id 实现选型            | 见 §3.5                                                                                   | —       |

---

## 2. Step 1 — 提供 SQLCipher 模块

`DepMapCore` 需要 `SQLCipher` 模块。SPM 生态中没有官方 Swift Package，可选路径：

### 2.1 方案 A：预编译 XCFramework（推荐）

在 Mac 上获取或自行编译 `SQLCipher.xcframework`，然后二选一：

- **binaryTarget**：在 `Package.swift` 的 `targets` 中加入
  `.binaryTarget(name: "SQLCipher", path: "Frameworks/SQLCipher.xcframework")`，
  并在 `DepMapCore` 的 `dependencies` 中加入 `"SQLCipher"`；
- 或由最终 App 工程（Xcode 工程）链接，SPM target 仅承担纯 Swift 逻辑验证。

### 2.2 方案 B：CocoaPods

若最终 App 工程由 HBuilderX / CocoaPods 管理，在 `Podfile` 中加入 SQLCipher，
由 app target 链接。

### 2.3 验证

```bash
cd platforms/ios
swift package resolve
swift build
```

预期：`swift build` 不再报 `no such module 'SQLCipher'`。

> **本机无法执行上述命令**（无 Swift 工具链）。请在 Mac 上记录实际输出并回填到 `PRODUCTION_RC_V1_REPORT.md`。

---

## 3. Step 2 — 实现 iOS 容器（G-1，首要任务）

需新建 `platforms/ios/swift/DepmapContainerV1.swift`，与 Core Node 参考实现
（`core/src/crypto/depmap.ts`）**逐字节互操作**。

**可直接对照的已通过验证的参考实现**：
`platforms/android/kotlin/com/depmap/core/crypto/DepmapContainerV1.kt`
（其黄金向量测试在 Android 侧真实执行并通过，见 `docs/ANDROID_BUILD_REPORT.md` §6.3）。

### 3.1 算法契约

| 项        | 值                                                                                             |
| --------- | ---------------------------------------------------------------------------------------------- |
| KDF       | **Argon2id**，`version = 0x13 (19)`，默认 `memoryKiB=65536` / `iterations=3` / `parallelism=1` |
| 密钥      | 32 字节 fileEncryptionKey；password 取**精确 UTF-8 字节，不做 Unicode 归一化**                 |
| 加密      | **AES-256-GCM**，16 字节 tag，12 字节 nonce，16 字节 salt                                      |
| AAD       | `UTF8(JCS({format, formatVersion, kdf, cipher}))` —— **ciphertext / tag 不进 AAD**             |
| Base64    | **RFC 4648 标准带填充**（`Data.base64EncodedString()` 默认行为即符合）                         |
| 容器 JSON | 按 **JCS（RFC 8785）** 键序序列化                                                              |

### 3.2 黄金向量（必须逐字节一致）

```
password  = "depmap-test"
salt      = 00112233445566778899aabbccddeeff
nonce     = a1b2c3d4e5f60718293a4b5c
plaintext = {"app":"depmap","schemaVersion":1,"nodes":[],"dependencies":[]}

derivedKeyHex = 66c4bec7f5e98856747d7b41d0a021bdc092d5e12d492852bd80647bd0ff0c86
ciphertextB64 = KNSpbKK6waj4En3ADgeBB74H96Q7GCoBmNffeOG5QSQ9XJwx4LoJCQ0j8lEinA7GN85U6JwaVMqhAkqWDdG7
tagB64        = 5qpABhovPbNet1q2GNEhkg==
```

冻结源：`core/src/crypto/golden.ts` 的 `GOLDEN_EXPECTED`。

### 3.3 边界校验（必须先于 KDF 执行）

`memoryKiB ∈ [16384, 262144]`、`iterations ∈ [1, 10]`、`parallelism ∈ [1, 4]`、
`salt = 16B`、`nonce = 12B`、`tag = 16B`、`ciphertext ∈ (0, 64 MiB]`。
恶意容器**不得触发超大内存 KDF**。

### 3.4 已知易错点（Android 侧真实踩过，务必规避）

1. **Base64 填充**：Android 曾用 `NO_PADDING` 产出无填充串，破坏互操作契约（P0 级）。
   Swift 的 `Data.base64EncodedString()` 默认带填充，**不要**手动去填充。
2. **JSON 字段名冲突**：`algorithm` 同时存在于 `cipher` 与 `kdf`。解析时必须**限定作用域**
   —— Android 曾因全局取首个匹配而恒定抛 `bounds`，导致**完全无法解密任何容器**（P0 级）。
3. **整数最短形式**：JCS 要求整数无多余前导零，AAD 中 `formatVersion` / `version` 必须与 Node 一致。

### 3.5 Argon2id 选型建议

Swift 无内置 Argon2。可选：

- 使用 SQLCipher 依赖链中已有的 crypto 能力（若提供 Argon2 绑定）；
- 引入成熟 Swift / C 绑定并在 `Package.swift` 声明；
- **不建议**自行实现 Argon2id（密码学实现风险极高）。

---

## 4. Step 3 — 启用并运行测试

`Tests/DepMapCoreTests/DepmapContainerV1Tests.swift` 目前三个用例均为 `throw XCTSkip(...)`。
完成 §3 后需恢复断言（文件中已保留注释形式的原始断言）：

```swift
func testGoldenVectorDecrypts() throws {
    let pt = try DepmapContainerV1.decrypt(containerJson, password: Golden.password)
    XCTAssertEqual(String(data: Data(pt), encoding: .utf8), Golden.plaintext)
}
```

```bash
cd platforms/ios
swift test
```

**判定标准**：三个用例全部 `passed`，且**不得**再出现 `skipped`。
若 Golden 不一致 → **停止**，这是跨端契约破坏，不能带病发布。

---

## 5. Step 4 — App 工程构建

App 工程由 HBuilderX 生成（依赖 **B10**）：

- HBuilderX 已安装 `5.24.2026081301`，工程导入成功；
- 但其 **CLI 不含 build / publish 命令**，需 GUI 操作或云打包。

> 顺序依赖：B10 需在 B3 之前或同时解除。

---

## 6. Step 5 — 真机冒烟（必须）

在真机上执行并逐项记录结果：

| #   | 步骤                | 预期                             |
| --- | ------------------- | -------------------------------- |
| 1   | 首次启动            | 显示 Onboarding（3 屏）          |
| 2   | 完成引导            | 进入解锁页                       |
| 3   | 生物识别 / 设备密码 | 通过后进入首页                   |
| 4   | 首页                | 显示「还没有需要处理的事项」空态 |
| 5   | 「我的 → 关于」     | 显示 App 版本 / 数据结构版本     |
| 6   | 切后台再回前台      | 隐私遮罩生效                     |
| 7   | 设置 → 隐私屏开关   | 截屏被阻止（或按系统策略降级）   |
| 8   | 数据来源 / 备份页   | 空态正常，无崩溃                 |

> **注意**：导入与备份在设备上尚不可用（B20/B21，Core 桥接未接入）。
> 真机冒烟**不覆盖**这两项，需在 B20/B21 解除后补测。

### 6.1 隐私声明待核实项

`app/manifest.json` 声明了 `NSCameraUsageDescription`（「仅在你选择扫描二维码导入时使用」）。
**需在 Mac 侧核实**：产品是否存在扫码导入功能。若不存在，该声明属**多余权限声明**，
App Store 审核可能质询 —— 应删除或补齐对应功能。

`NSFaceIDUsageDescription` 与生物识别解锁一致，保留。

---

## 7. Step 6 — Archive 与 TestFlight

```bash
xcodebuild -workspace <name>.xcworkspace \
  -scheme <scheme> \
  -configuration Release \
  -archivePath build/App.xcarchive \
  archive

xcodebuild -exportArchive \
  -archivePath build/App.xcarchive \
  -exportOptionsPlist ExportOptions.plist \
  -exportPath build/
```

**禁止**：把证书 / Provisioning Profile / `.p12` / `.p8` 提交进 Git。

---

## 8. 提交前检查清单

- [ ] `swift package resolve && swift build` 成功
- [ ] `swift test` 三个用例**全部 passed 且无 skipped**
- [ ] Golden 向量逐字节一致（derivedKey / ciphertext / tag）
- [ ] 真机冒烟 §6 全部通过
- [ ] `Info.plist` 隐私用途说明与实现一致（§6.1）
- [ ] Entitlements 最小化
- [ ] Bundle ID 为最终值（B11）
- [ ] 版本号与 Build number 与 `docs/RELEASE_VERSION_MATRIX.md` 一致
- [ ] 无 `print` / debug 菜单 / 测试数据
- [ ] 隐私政策 URL 与支持 URL 可访问（B12）

---

## 9. 完成后需回填的状态

| 字段                   | 更新为                            |
| ---------------------- | --------------------------------- |
| `IOS_BUILD_READY`      | PASS                              |
| `IOS_DEVICE_VERIFIED`  | PASS（并记录设备型号 / 系统版本） |
| `IOS_SIGNING_READY`    | PASS                              |
| `IOS_TESTFLIGHT_READY` | PASS                              |
| `IOS_APPSTORE_READY`   | 视 §8 检查结果                    |

> 请把实际命令与输出回填到 `PRODUCTION_RC_V1_REPORT.md`，**不要只写「已验证」**。

---

## 10. 附：本机已完成 vs 需在 Mac 完成

| 类别              | 内容                                                                                                                                                |
| ----------------- | --------------------------------------------------------------------------------------------------------------------------------------------------- |
| **本机已完成**    | 工程结构审计；`Package.swift` 空 target 缺陷修正；`LAPolicy()` 语法错误修正；容器算法契约与黄金向量文档化；易错点清单（源自 Android 实测教训）      |
| **需在 Mac 完成** | SQLCipher 模块接入；`DepmapContainerV1.swift` 实现（G-1）；测试断言恢复；`swift build` / `swift test`；App 工程构建；真机冒烟；Archive / TestFlight |
