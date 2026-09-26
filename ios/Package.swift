// swift-tools-version:5.9
//
// PDIG / DepMap —— iOS 原生 Swift 包。
//
// 结构（纯 Swift Domain 不依赖 UIKit / SwiftUI / SQLCipher / Keychain）：
//   PDIGCore        领域层 + 解析层 + JSON 规范化 + 编码（纯逻辑，可测试）
//   PDIGConformance canonical fixture 执行器（91 + 37 条）
//   PDIGApp         iOS N4 SwiftUI App（可执行 target，macOS runner 构建 + 截图 harness）
//   PDIGAppTests    视图模型纯逻辑单测（导航 / Findings / 换号 / CTA）
//
// 本机（Windows）没有 Swift 工具链，因此本包的**每次**编译与测试都由
// GitHub Actions 的 macOS runner 执行（.github/workflows/ios.yml）。
// 未跑过 CI 的结论一律记 NOT_RUN，不得写成 PASS。
//
// 注意：Argon2id / AES-GCM 已接入（vendored Argon2 + CryptoKit，golden 校验）；
// SQLCipher at-rest 加密尚未接入本包 → IOS_SQLCIPHER_PERSISTENCE = NOT_RUN。

import PackageDescription

let package = Package(
    name: "PDIG",
    platforms: [
        .macOS(.v13),
        .iOS(.v16),
    ],
    products: [
        .library(name: "PDIGCore", targets: ["PDIGCore"]),
        .library(name: "PDIGConformance", targets: ["PDIGConformance"]),
        .executable(name: "PDIGApp", targets: ["PDIGApp"]),
    ],
    targets: [
        // vendored Argon2（third_party/argon2，包外单一真源）。
        .target(
            name: "PDIGArgon2C",
            path: "Sources/PDIGArgon2C"
        ),
        .target(
            name: "PDIGArgon2",
            dependencies: ["PDIGArgon2C", "PDIGCore"],
            path: "Sources/PDIGArgon2"
        ),
        // 系统 sqlite3（**host harness + App 落盘**）。App 的 at-rest 加密
        // 必须用 SQLCipher（未接入，NOT_RUN）；这里只用于执行 backup /
        // migration 两条 DB 型 canonical 用例与 App 文件存储。
        .systemLibrary(
            name: "CSQLite",
            path: "Sources/CSQLite"
        ),
        .target(
            name: "PDIGCore",
            path: "Sources/PDIGCore"
        ),
        .target(
            name: "PDIGConformance",
            dependencies: ["PDIGCore", "PDIGArgon2", "CSQLite"],
            path: "Sources/PDIGConformance"
        ),
        .testTarget(
            name: "PDIGConformanceTests",
            dependencies: ["PDIGCore", "PDIGConformance", "PDIGArgon2"],
            path: "Tests/PDIGConformanceTests"
        ),
        // iOS N4 App：SwiftUI 应用 + 截图 harness。SwiftPM 以可执行 target 构建，
        // 在 macOS runner 上编译；harness 模式（--screenshot）渲染各屏 PNG。
        .executableTarget(
            name: "PDIGApp",
            dependencies: ["PDIGCore", "PDIGArgon2", "PDIGConformance", "CSQLite"],
            path: "Sources/PDIGApp"
        ),
        .testTarget(
            name: "PDIGAppTests",
            dependencies: ["PDIGApp", "PDIGCore"],
            path: "Tests/PDIGAppTests"
        ),
    ]
)