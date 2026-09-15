// swift-tools-version:5.9
// DepMap iOS native core — Swift Package。
//
// 状态：工程文件 IMPLEMENTED。构建验证 = NO（本机 Windows，无 macOS/Xcode —— BLOCKERS.md）。
//
// 重要（2026-09-15 审计修正）：
//   1) 原先 `exclude: ["SQLCipherSecureDatabaseAdapter.swift"]` 使 DepMapCore target
//      **没有任何源文件**，SPM 无法构建。该 exclude 已移除 —— 现在依赖缺失会在 Mac 上
//      以明确错误 `no such module 'SQLCipher'` 暴露，而不是隐式的空 target 错误。
//   2) 本 target 需要外部提供 SQLCipher 模块（XCFramework / CocoaPods / vendored），
//      接入步骤见 docs/IOS_RELEASE_HANDOFF.md §2。
//   3) swift/ 目录当前**不含** DepmapContainerV1.swift —— iOS 容器实现尚未编写，
//      Tests/DepMapCoreTests/DepmapContainerV1Tests.swift 的三个用例均为 XCTSkip。
//      这是 Mac 侧的首要待办，见 docs/IOS_RELEASE_HANDOFF.md §3。
//   4) 在 macOS/Xcode 环境执行：swift package resolve && swift test

import PackageDescription

let package = Package(
    name: "DepMapCore",
    platforms: [.iOS(.v14), .macOS(.v12)],
    products: [
        .library(name: "DepMapCore", targets: ["DepMapCore"])
    ],
    targets: [
        .target(
            name: "DepMapCore",
            path: "swift",
            swiftSettings: [.define("DEPMAP_SPM")]
        ),
        .testTarget(
            name: "DepMapCoreTests",
            dependencies: ["DepMapCore"],
            path: "Tests/DepMapCoreTests"
        )
    ]
)
