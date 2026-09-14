# IOS_RELEASE_HANDOFF.md — iOS 发布交接手册（Windows → macOS）

> 现状：本机为 **Windows（win32）**，无 Xcode。按 §100/§140：
> `IOS_SOURCE_READY = PASS`，`IOS_BUILD_READY = BLOCKED（B3）`。
> **本文件不得声称任何 iOS 编译已通过。**
>
> 目的：拿到 Mac 后按顺序执行即可完成 resolve → build → test → archive → device → TestFlight，
> 不需要再做设计或大改代码。

---

## 0. 前置条件（用户提供）

| 项                           | 说明                             | Blocker |
| ---------------------------- | -------------------------------- | ------- |
| macOS + Xcode（≥ 15）        | 构建与签名                       | B3      |
| Apple Developer Program 账号 | 证书 / Provisioning / TestFlight | B8      |
| 正式 Bundle ID               | 当前占位 `com.example.depmap`    | B11     |
| 正式 App 名                  | 当前"个人数字依赖图"             | B14     |
| SQLCipher 依赖可达性         | 见 §2                            | —       |

---

## 1. 仓库准备

```bash
git clone <repo> && cd <repo>
git checkout feat/mvp03-living-graph      # 或 RC 分支
cd core && npm ci && npm run check        # 先确认 Core 绿（453 tests）
```

## 2. Swift 包解析与依赖

```bash
cd platforms/ios
swift package resolve
swift package show-dependencies
```

**已知事项**：`platforms/ios/Package.swift` 声明了 SQLCipher 相关目标。
若解析失败，优先检查：

- SQLCipher 的 Swift Package 源是否可达（若为本地 vendored，确认路径存在）
- 是否需要 `SQLCipher.xcframework` 手动放置

> 记录：本机无法执行 `swift package resolve`（无 Swift 工具链）。此步骤在 Mac 上首次执行时请记录实际输出。

## 3. 单元测试（含 Golden Vector）

```bash
cd platforms/ios
swift test
```

**必须全部通过**，其中最关键的是 `Tests/DepMapCoreTests/DepmapContainerV1Tests.swift`：
它验证 `.depmap` V1 容器与 Core（Node/TypeScript）实现**互操作一致**
（Argon2id v19 + AES-256-GCM + RFC8785 JCS AAD）。

> 若 Golden 不一致 → **停止**，这是跨端契约破坏，不能带病发布。

## 4. 应用工程构建

App 工程由 HBuilderX 生成 iOS 工程（依赖 B10）：

```bash
# 在 HBuilderX 中：发行 → 原生 App-云打包 / 本地打包（iOS）
# 或：运行到 iOS 模拟器/真机
```

**顺序依赖**：B10（HBuilderX）必须在 B3（Mac）之前或同时解除。

## 5. 真机冒烟（必须）

在真机上执行并逐项记录结果：

| #   | 步骤                | 预期                           |
| --- | ------------------- | ------------------------------ |
| 1   | 首次启动            | 显示 Onboarding（3 屏）        |
| 2   | 完成引导            | 进入解锁页                     |
| 3   | 生物识别 / 设备密码 | 通过后进入首页                 |
| 4   | 首页                | 显示"还没有需要处理的事项"空态 |
| 5   | 进入「我的 → 关于」 | 显示 App 版本 / 数据结构版本   |
| 6   | 切后台再回前台      | 隐私遮罩（iOS 后台遮罩）生效   |
| 7   | 设置 → 隐私屏开关   | 截屏被阻止（或按系统策略降级） |
| 8   | 数据来源 / 备份页   | 空态正常，无崩溃               |

> **注意**：导入与备份在设备上尚不可用（B20/B21，Core 桥接未接入）。真机冒烟**不覆盖**这两项，
> 需在 B20/B21 解除后补测。

## 6. Archive 与 TestFlight

```bash
# Xcode 中：
# 1) 选择 Generic iOS Device
# 2) Product → Archive
# 3) Distribute App → App Store Connect → Upload
```

或命令行：

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

## 7. 提交前检查

- [ ] `swift test` 全绿（含 Golden）
- [ ] 真机冒烟 §5 全部通过
- [ ] `Info.plist` 隐私用途说明与实现一致
- [ ] Entitlements 最小化
- [ ] Bundle ID 为最终值（B11）
- [ ] 版本号与 Build number 与 `docs/RELEASE_VERSION_MATRIX.md` 一致
- [ ] 无 `console.log` / debug 菜单 / 测试数据
- [ ] 隐私政策 URL 与支持 URL 可访问（B12）

## 8. 完成后需要回填的状态

| 字段                   | 更新为                            |
| ---------------------- | --------------------------------- |
| `IOS_BUILD_READY`      | PASS                              |
| `IOS_DEVICE_VERIFIED`  | PASS（并记录设备型号 / 系统版本） |
| `IOS_SIGNING_READY`    | PASS                              |
| `IOS_TESTFLIGHT_READY` | PASS                              |
| `IOS_APPSTORE_READY`   | 视 §7 检查结果                    |

> 请把实际命令与输出回填到 `PRODUCTION_RC_V1_REPORT.md`，**不要只写"已验证"**。
