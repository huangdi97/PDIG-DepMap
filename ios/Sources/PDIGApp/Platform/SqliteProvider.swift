// SqliteProvider —— App 持久化驱动提供者。
//
// 诚实分层（AGENTS §7 / §12）：
//  - App 需要 SqliteDriver 实现。PDIGConformance 的 SystemSqliteDriver 是
//    **host harness** 驱动（系统 sqlite3），三端 conformance 用它跑 DB 型用例。
//  - 本 App 在 macOS 上直接用它落盘（数据文件在 App Support/PDIG 下）；
//    SQLCipher 未链接 → IOS_SQLCIPHER_PERSISTENCE = NOT_RUN（不写成已验证）。
//  - 将来 SQLCipher 接入时只需替换 open() 的实现，仓储层不变。

import Foundation
import PDIGCore
import PDIGConformance

public enum SqliteProvider {

    /// 打开（必要时创建）App 数据库。
    public static func openDatabase(
        atDirectory directory: URL,
        fileName: String = "depmap.db"
    ) throws -> SqliteDriver {
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        let url = directory.appendingPathComponent(fileName)
        return try SystemSqliteDriver(path: url.path)
    }

    /// 平台层能力声明（写入证据索引，诚实标注）。
    public static func capabilityNote() -> String {
        "IOS_SQLCIPHER_PERSISTENCE = NOT_RUN: App 使用 PDIGCore GraphSerialize + 系统 sqlite3（host harness 驱动）。" +
        "SQLCipher 未链接；at-rest 加密与 Keychain 级密钥派生由平台安全层后续接入，需真机验证。"
    }
}
