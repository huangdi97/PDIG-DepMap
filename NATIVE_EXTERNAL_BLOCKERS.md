# NATIVE_EXTERNAL_BLOCKERS.md

> **只允许真正的外部 blocker 出现在这里**（GOAL §202）。
> 可解决的问题（Gradle / 编译错误 / 依赖 / 测试 / UI / 迁移 / crypto 集成）
> 一律不属于 blocker，必须自己解决（§203）。

更新时间：2026-09-15

| ID    | 类别        | 事项                                        | 影响                         | 可否由 Agent 解决   | 状态              |
| ----- | ----------- | ------------------------------------------- | ---------------------------- | ------------------- | ----------------- |
| NB-1  | macOS/Xcode | 当前环境为 Windows，无 Xcode / Swift 工具链 | iOS build / test / simulator | **否**              | **BLOCKED**       |
| NB-2  | 签名        | Android release keystore 不存在             | Android release build        | 否（用户持有）      | BLOCKED           |
| NB-3  | 签名        | Harmony AGC 签名证书                        | Harmony 发布包               | 否                  | BLOCKED           |
| NB-4  | 账号        | Apple Developer 账号                        | iOS 真机 / TestFlight / 上架 | 否                  | BLOCKED           |
| NB-5  | 账号        | Google Play 开发者账号                      | Android 上架                 | 否                  | BLOCKED           |
| NB-6  | 账号        | Huawei AppGallery 开发者身份                | Harmony 上架                 | 否                  | BLOCKED           |
| NB-7  | 设备        | 无 HarmonyOS 真机 / 模拟器                  | HARMONY_RUNTIME              | 部分（装 emulator） | BLOCKED           |
| NB-8  | 设备        | Android AVD 缺 system image（约 1.5GB）     | ANDROID_RUNTIME E2E          | 是（下载）          | BLOCKED（待下载） |
| NB-9  | 真实数据    | 真实账单（Real Data Pilot）                 | B13 双 Gate                  | 否（用户数据）      | NOT_RUN           |
| NB-10 | 品牌        | 正式 App 名称 / 包名 / 图标决定             | 三端 bundle identity         | 否（用户决定）      | 待定              |
| NB-11 | 法律        | 隐私政策正式 URL                            | Store 提交                   | 否                  | 待定              |

---

## 明确**不是** blocker 的事项

以下均已在本轮被证明**可由代码解决**，不得登记为 blocker：

| 事项                          | 本轮结果                                       |
| ----------------------------- | ---------------------------------------------- |
| Gradle / Kotlin 工具链        | 本地 Gradle 8.9 + 989MB 依赖缓存可用，构建成功 |
| Argon2id 在 Kotlin 的实现     | BouncyCastle，黄金向量逐字节一致               |
| AES-256-GCM / Base64 / CSPRNG | JDK JCE + SecureRandom                         |
| JCS (RFC 8785)                | 自实现受限域，9/9 用例一致                     |
| 三端枚举漂移                  | codegen + Gate 解决                            |
| 平台中立 JSON 解析            | 自实现严格解析器（避免第三方差异）             |
| fixture 期望值来源            | 冻结 TS oracle 生成 + 自检                     |
| Kotlin JVM target 不一致      | `jvmToolchain(21)` 解决                        |
| `RepositoriesMode` 枚举名错误 | 已修正                                         |

---

## 本轮新发现的**内部**问题（已修，不登记为 blocker）

| 编号  | 问题                                                                 | 处置                                              |
| ----- | -------------------------------------------------------------------- | ------------------------------------------------- |
| FIX-1 | `csv-utf8-bom.csv` 实际不含 BOM（测试覆盖空洞）                      | 记录为 LC-001；Native fixture 需另用真实 BOM 文件 |
| FIX-2 | `csv-missing-required-column.csv` 并未缺失必需列                     | 记录为 LC-002                                     |
| FIX-3 | legacy UI 暴露 runtime registry 不承认的 `bound_to`                  | 记录为 LC-003；Native 锁定 2 值                   |
| FIX-4 | legacy `relationLabel` 含不存在的 `wallet_binding`                   | 记录为 LC-004                                     |
| FIX-5 | readiness 文案两处不一致（'可以继续' vs '基于当前信息，可以继续。'） | 记录为 LC-005；以 copy-zh.json 为准               |
| FIX-6 | golden fixture 的 wrongPasswordOutcome 误用正确口令                  | **已修**：改为错误口令 + 增补篡改场景             |
| FIX-7 | JCS reject case 含 NaN（JSON 无法表达，必然假失败）                  | **已修**：移除该用例                              |
| FIX-8 | conformance harness 从 manifest 读 expected（manifest 不含）         | **已修**：改为读 fixture 本体                     |
