// swift-tools-version:5.9
// DepMap iOS native core — Swift Package（无第三方依赖；SQLCipher 由 app target 链接）。
// 在 macOS/Xcode 环境执行：swift test（golden vector 互操作测试见 DepmapContainerV1Tests.swift）
// 状态：工程文件 IMPLEMENTED。构建验证 = NO（本机 Windows，无 macOS/Xcode —— BLOCKERS.md）。

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
            exclude: ["SQLCipherSecureDatabaseAdapter.swift"], // 需要 SQLCipher 的部分由 app target 编译
            swiftSettings: [.define("DEPMAP_SPM")]
        ),
        .testTarget(
            name: "DepMapCoreTests",
            dependencies: ["DepMapCore"],
            path: "Tests/DepMapCoreTests"
        )
    ]
)
