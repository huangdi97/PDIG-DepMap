// 系统 sqlite3 驱动 —— **仅供 conformance host harness 使用**。
//
// 定位：
//  - PDIGCore 只声明 `SqliteDriver` 协议，不绑定任何平台实现；
//  - iOS App 的真实持久化必须注入 SQLCipher 实现（at-rest 加密）；
//  - 本实现跑在 macOS CI 上，用于执行 `backup` / `migration` 两条 DB 型
//    canonical 用例，验证的是 payload 序列化与迁移逻辑（与加密无关）。
//
// 这一点必须在验收里如实说明：**它证明的不是"iOS 已接入 SQLCipher"**。

import Foundation
import CSQLite
import PDIGCore

private let transient: sqlite3_destructor_type = unsafeBitCast(-1, to: sqlite3_destructor_type.self)

public final class SystemSqliteDriver: SqliteDriver {
    private var db: OpaquePointer?
    private let path: String

    public init(path: String) throws {
        self.path = path
        var handle: OpaquePointer?
        let rc = sqlite3_open_v2(path, &handle, SQLITE_OPEN_CREATE | SQLITE_OPEN_READWRITE, nil)
        if rc != SQLITE_OK || handle == nil {
            let msg = handle.flatMap { String(cString: sqlite3_errmsg($0)) } ?? "sqlite3_open failed (\(rc))"
            if let opened = handle { sqlite3_close(opened) }
            throw SqliteError(msg)
        }
        self.db = handle
    }

    deinit { close() }

    public func close() {
        if let db = db {
            sqlite3_close(db)
            self.db = nil
        }
    }

    public func exec(_ sql: String) throws {
        guard let db = db else { throw SqliteError("driver closed") }
        var err: UnsafeMutablePointer<CChar>?
        let rc = sqlite3_exec(db, sql, nil, nil, &err)
        if rc != SQLITE_OK {
            let msg = err.flatMap { String(cString: $0) } ?? String(cString: sqlite3_errmsg(db))
            if let err = err { sqlite3_free(UnsafeMutableRawPointer(err)) }
            throw SqliteError("exec failed: \(msg)")
        }
    }

    public func prepare(_ sql: String) throws -> SqliteStatement {
        guard let db = db else { throw SqliteError("driver closed") }
        var stmt: OpaquePointer?
        let rc = sqlite3_prepare_v2(db, sql, -1, &stmt, nil)
        guard rc == SQLITE_OK, let stmt = stmt else {
            throw SqliteError("prepare failed: \(String(cString: sqlite3_errmsg(db)))")
        }
        return SystemSqliteStatement(db: db, stmt: stmt)
    }

    public func transaction<T>(_ fn: () throws -> T) throws -> T {
        try exec("BEGIN")
        do {
            let v = try fn()
            try exec("COMMIT")
            return v
        } catch {
            // 回滚失败不能吞掉原始错误：半迁移状态比"多一个错误"危险得多，
            // 所以这里只尝试回滚，原始错误照抛。
            _ = try? exec("ROLLBACK")
            throw error
        }
    }
}

private final class SystemSqliteStatement: SqliteStatement {
    private let db: OpaquePointer
    private var stmt: OpaquePointer?

    init(db: OpaquePointer, stmt: OpaquePointer) {
        self.db = db
        self.stmt = stmt
    }

    deinit { sqlite3_finalize(stmt) }

    private func bind(_ params: [SqlValue]) throws {
        guard let stmt = stmt else { throw SqliteError("statement finalized") }
        sqlite3_reset(stmt)
        sqlite3_clear_bindings(stmt)
        for (i, v) in params.enumerated() {
            let idx = Int32(i + 1)
            var rc: Int32
            switch v {
            case .null:
                rc = sqlite3_bind_null(stmt, idx)
            case .integer(let n):
                rc = sqlite3_bind_int64(stmt, idx, n)
            case .real(let d):
                rc = sqlite3_bind_double(stmt, idx, d)
            case .text(let s):
                rc = sqlite3_bind_text(stmt, idx, s, -1, transient)
            case .blob(let b):
                rc = b.withUnsafeBufferPointer { buf in
                    sqlite3_bind_blob(stmt, idx, buf.baseAddress, Int32(buf.count), transient)
                }
            }
            if rc != SQLITE_OK {
                throw SqliteError("bind #\(idx) failed: \(String(cString: sqlite3_errmsg(db)))")
            }
        }
    }

    @discardableResult
    func run(_ params: [SqlValue] = []) throws -> Int {
        try bind(params)
        guard let stmt = stmt else { throw SqliteError("statement finalized") }
        let rc = sqlite3_step(stmt)
        if rc != SQLITE_DONE && rc != SQLITE_ROW {
            throw SqliteError("step failed: \(String(cString: sqlite3_errmsg(db)))")
        }
        return Int(sqlite3_changes(db))
    }

    func all(_ params: [SqlValue] = []) throws -> [SqliteRow] {
        try bind(params)
        guard let stmt = stmt else { throw SqliteError("statement finalized") }
        var rows: [SqliteRow] = []
        while true {
            let rc = sqlite3_step(stmt)
            if rc == SQLITE_ROW {
                rows.append(SystemSqliteRow(stmt: stmt))
            } else if rc == SQLITE_DONE {
                break
            } else {
                throw SqliteError("step failed: \(String(cString: sqlite3_errmsg(db)))")
            }
        }
        return rows
    }

    func get(_ params: [SqlValue] = []) throws -> SqliteRow? {
        try bind(params)
        guard let stmt = stmt else { throw SqliteError("statement finalized") }
        let rc = sqlite3_step(stmt)
        if rc == SQLITE_ROW { return SystemSqliteRow(stmt: stmt) }
        if rc == SQLITE_DONE { return nil }
        throw SqliteError("step failed: \(String(cString: sqlite3_errmsg(db)))")
    }
}

private struct SystemSqliteRow: SqliteRow {
    private let columnNames: [String]
    private let values: [SqlValue]

    init(stmt: OpaquePointer) {
        let n = Int(sqlite3_column_count(stmt))
        var cols: [String] = []
        var vals: [SqlValue] = []
        for i in 0..<n {
            let name = sqlite3_column_name(stmt, Int32(i)).flatMap { String(cString: $0) } ?? ""
            cols.append(name)
            vals.append(Self.read(stmt, Int32(i)))
        }
        self.columnNames = cols
        self.values = vals
    }

    private static func read(_ stmt: OpaquePointer, _ i: Int32) -> SqlValue {
        switch sqlite3_column_type(stmt, i) {
        case SQLITE_INTEGER: return .integer(sqlite3_column_int64(stmt, i))
        case SQLITE_FLOAT: return .real(sqlite3_column_double(stmt, i))
        case SQLITE_TEXT:
            if let p = sqlite3_column_text(stmt, i) {
                let n = Int(sqlite3_column_bytes(stmt, i))
                let buf = UnsafeBufferPointer(start: p, count: n)
                return .text(String(decoding: buf, as: UTF8.self))
            }
            return .null
        case SQLITE_BLOB:
            if let p = sqlite3_column_blob(stmt, i) {
                let n = Int(sqlite3_column_bytes(stmt, i))
                let buf = UnsafeBufferPointer(start: p.bindMemory(to: UInt8.self, capacity: n), count: n)
                return .blob(Array(buf))
            }
            return .null
        default: return .null
        }
    }

    func columns() -> [String] { columnNames }

    func value(_ column: String) -> SqlValue? {
        guard let i = columnNames.firstIndex(of: column) else { return nil }
        return values[i]
    }

    func raw(_ column: String) -> SqlValue? { value(column) }

    func str(_ column: String) -> String? {
        guard let v = value(column) else { return nil }
        switch v {
        case .text(let s): return s
        case .integer(let i): return String(i)
        case .real(let d): return String(d)
        default: return nil
        }
    }

    func long(_ column: String) -> Int64? {
        guard let v = value(column) else { return nil }
        switch v {
        case .integer(let i): return i
        case .real(let d): return Int64(d)
        case .text(let s): return Int64(s)
        default: return nil
        }
    }

    func double(_ column: String) -> Double? {
        guard let v = value(column) else { return nil }
        switch v {
        case .real(let d): return d
        case .integer(let i): return Double(i)
        case .text(let s): return Double(s)
        default: return nil
        }
    }

    func isNull(_ column: String) -> Bool {
        guard let v = value(column) else { return true }
        if case .null = v { return true }
        return false
    }
}

/// 临时 DB（跑完即删）—— 与 Android `withTempDb` 同语义。
public func withTempDb<T>(_ tag: String, _ fn: (SqliteDriver) throws -> T) throws -> T {
    let base = URL(fileURLWithPath: NSTemporaryDirectory()).appendingPathComponent("pdig-cf-\(tag)-\(UUID().uuidString)")
    try FileManager.default.createDirectory(at: base, withIntermediateDirectories: true)
    let driver = try SystemSqliteDriver(path: base.appendingPathComponent("fx.db").path)
    do {
        let v = try fn(driver)
        driver.close()
        try? FileManager.default.removeItem(at: base)
        return v
    } catch {
        driver.close()
        try? FileManager.default.removeItem(at: base)
        throw error
    }
}
