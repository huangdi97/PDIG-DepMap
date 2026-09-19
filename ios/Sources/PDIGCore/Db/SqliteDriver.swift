// PDIG SQLite 驱动抽象 —— 与 Android `core/db/SqliteDriver.kt` 同语义。
//
// 关键约束：
//  - **逻辑 Schema 与迁移行为必须跨端一致，物理实现允许不同**
//    （Android/iOS = SQLCipher，HarmonyOS = ArkData relationalStore，
//      JVM conformance = sqlite-jdbc，本包 host harness = 系统 sqlite3）。
//  - 本文件不引入任何平台依赖：PDIGCore 因此保持纯逻辑，
//    具体驱动由使用方注入（App 侧注入 SQLCipher 实现）。
//  - `raw` 必须保留 **存储类型**（INTEGER / REAL / TEXT / NULL / BLOB）：
//    否则 `archived=0` 会被写成 `0.0`，破坏跨端 payload 逐字节一致性。

import Foundation

/// 存储层原始值。保留类型是 payload 等价性的前提。
public enum SqlValue: Equatable, Sendable {
    case null
    case integer(Int64)
    case real(Double)
    case text(String)
    case blob([UInt8])
}

public protocol SqliteRow {
    func str(_ column: String) -> String?
    func long(_ column: String) -> Int64?
    func double(_ column: String) -> Double?
    func isNull(_ column: String) -> Bool
    func raw(_ column: String) -> SqlValue?
    /// 物理列顺序（`SELECT *` 的真实顺序；payload 键顺序依赖它）。
    func columns() -> [String]
}

public protocol SqliteStatement {
    @discardableResult
    func run(_ params: [SqlValue]) throws -> Int
    func get(_ params: [SqlValue]) throws -> SqliteRow?
    func all(_ params: [SqlValue]) throws -> [SqliteRow]
}

public protocol SqliteDriver {
    func exec(_ sql: String) throws
    func prepare(_ sql: String) throws -> SqliteStatement
    /// 事务执行；出错回滚并重新抛出，绝不留下半迁移/半写入状态。
    func transaction<T>(_ fn: () throws -> T) throws -> T
    func close()
}

public struct SqliteError: Error, CustomStringConvertible {
    public let message: String
    public init(_ message: String) { self.message = message }
    public var description: String { message }
}

extension SqliteRow {
    public func str(_ column: String) -> String? {
        guard let v = raw(column) else { return nil }
        if case .text(let s) = v { return s }
        if case .integer(let i) = v { return String(i) }
        return nil
    }
    public func long(_ column: String) -> Int64? {
        guard let v = raw(column) else { return nil }
        if case .integer(let i) = v { return i }
        return nil
    }
    public func double(_ column: String) -> Double? {
        guard let v = raw(column) else { return nil }
        if case .real(let d) = v { return d }
        if case .integer(let i) = v { return Double(i) }
        return nil
    }
    public func isNull(_ column: String) -> Bool {
        guard let v = raw(column) else { return true }
        if case .null = v { return true }
        return false
    }
}
