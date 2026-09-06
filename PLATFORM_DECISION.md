# PLATFORM_DECISION.md — 三端工程决策

状态：冻结（仅允许 correctness / security / interoperability 修正）

## 决策

为了满足 Android、iOS、HarmonyOS 三端独立上架：

- 主业务语言：TypeScript
- 应用层：uni-app x Vapor + Vue 3
- 原生桥接：UTS
- Android 原生能力：Kotlin
- iOS 原生能力：Swift
- HarmonyOS 原生能力：ArkTS

## 数据层

业务层只依赖 `SecureDatabaseAdapter` / Repository。

- Android：SQLite + SQLCipher + Keystore
- iOS：SQLite + SQLCipher + Keychain
- HarmonyOS：ArkData relationalStore encryption + HUKS

三端“逻辑 Schema 一致”，不要求“数据库引擎名字一致”。

## 为什么不使用 Capacitor

当前交付硬要求包含 HarmonyOS 原生上架，因此不再把 Capacitor 作为统一三端底座。旧母版中的 Capacitor 路线已由 `CANONICAL_DESIGN.md` 的三端修订直接替换。

## 编译环境现实约束

- Android / HarmonyOS 可在合适 Windows 工具链上完成较多开发与构建。
- iOS 最终 Xcode build、签名、真机验证必须在 macOS/Xcode 环境完成。
- 若当前 Workspace 不具备某平台 SDK，不得伪造“编译通过”；先完成可测试的共享 Core 和平台 Adapter 代码，把环境缺失记录到 `BLOCKERS.md`。

## 规则

如果 uni-app x / UTS / HarmonyOS API 存在版本差异：
1. 优先查当前官方文档 / 本地 SDK typings。
2. 通过真实编译结果验证。
3. 不允许凭记忆猜 API。
4. 不允许未经批准换框架绕过问题。
