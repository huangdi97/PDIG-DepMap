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
        // vendored Argon2（third_party/argon2，包外单一真源）。
        // 不复制源码进包内：复制件会让 VENDOR.json 的哈希校验形同虚设。
        // SwiftPM 不允许 header search path 指向包外，所以由
        // Sources/PDIGArgon2C/include/argon2.h 做转发，把 vendored 的
        // `#include "argon2.h"` 引回真源。
        .target(
            name: "PDIGArgon2C",
            path: "Sources/PDIGArgon2C"
        ),
        .target(
            name: "PDIGArgon2",
            dependencies: ["PDIGArgon2C", "PDIGCore"],
            path: "Sources/PDIGArgon2"
        ),
        // 系统 sqlite3（**仅 host harness**）。App 持久化须用 SQLCipher，
        // 这里只用来跑 backup / migration 两条 DB 型 canonical 用例——
        // 它们验证的是 payload 序列化与迁移逻辑，与 at-rest 加密无关。
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
    ]
)
