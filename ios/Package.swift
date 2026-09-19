// swift-tools-version:5.9
//
// PDIG / DepMap —— iOS 原生 Swift 包。
//
// 结构（纯 Swift Domain 不依赖 UIKit / SwiftUI / SQLCipher / Keychain）：
//   PDIGCore        领域层 + 解析层 + JSON 规范化 + 编码（纯逻辑，可测试）
//   PDIGConformance canonical fixture 执行器（91 条）
//
// 本机（Windows）没有 Swift 工具链，因此本包的**每次**编译与测试都由
// GitHub Actions 的 macOS runner 执行（.github/workflows/ios.yml）。
// 未跑过 CI 的结论一律记 NOT_RUN，不得写成 PASS。
//
// 注意：Argon2id / AES-GCM / SQLCipher 属于平台安全层，尚未接入本包，
// 相关 canonical 用例（depmap 3 / backup 1）在 runner 中如实记为 BLOCKED，
// 不伪造通过。

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
    ],
    targets: [
        .target(
            name: "PDIGCore",
            path: "Sources/PDIGCore"
        ),
        .target(
            name: "PDIGConformance",
            dependencies: ["PDIGCore"],
            path: "Sources/PDIGConformance"
        ),
        .testTarget(
            name: "PDIGConformanceTests",
            dependencies: ["PDIGCore", "PDIGConformance"],
            path: "Tests/PDIGConformanceTests"
        ),
    ]
)
