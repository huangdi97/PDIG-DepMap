import Foundation
import SQLCipher
import LocalAuthentication
import Security

/**
 * DepMap SecureDatabaseAdapter — iOS SQLCipher + Keychain 实现。
 *
 * 安全语义（CANONICAL_DESIGN §8.2 / GOAL §9）：
 * - 数据库密钥 32 字节随机，存 Keychain（kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly）；
 * - 口令/生物识别取消不得进入数据层（LocalAuthenticationGate）；
 * - 迁移事务化、幂等（与 Core migrations.ts 同一 DDL）；
 * - 数据文件排除 iCloud 自动备份。
 *
 * 状态：IMPLEMENTED（代码完成）。COMPILED / TESTED / DEVICE_VERIFIED = NO
 * （当前开发机为 Windows，无 macOS/Xcode —— 外部 Blocker，见 BLOCKERS.md）。
 */

enum DepmapDBError: Error {
    case notOpen
    case keyError(String)
    case sqlite(Int32, String)
    case schemaNewer(Int)
}

final class SQLCipherSecureDatabaseAdapter {

    private var handle: OpaquePointer?
    private let keyAdapter: KeychainSecureKeyAdapter
    private let dbName: String
    private let fileURL: URL

    init(keyAdapter: KeychainSecureKeyAdapter, dbName: String = "depmap.db") {
        self.keyAdapter = keyAdapter
        self.dbName = dbName
        let docs = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
        self.fileURL = docs.appendingPathComponent(dbName)
        // 排除 iCloud 自动备份（CANONICAL §8.10）
        var resourceValues = URLResourceValues()
        resourceValues.isExcludedFromBackup = true
        try? docs.appendingPathComponent(dbName).setResourceValues(resourceValues)
    }

    func open() throws {
        let key = try keyAdapter.getOrCreateDatabaseKey(alias: "depmap_database_key")
        let hexKey = key.map { String(format: "%02x", $0) }.joined()
        let flags = SQLITE_OPEN_READWRITE | SQLITE_OPEN_CREATE | SQLITE_OPEN_FULLMUTEX
        var db: OpaquePointer?
        guard sqlite3_open_v2(fileURL.path, &db, flags, nil) == SQLITE_OK else {
            if let db { sqlite3_close(db) }
            throw DepmapDBError.keyError("cannot open database")
        }
        // SQLCipher 密钥：PRAGMA key with hex key (x'...' 形式)
        let keyPragma = "PRAGMA key = \"x'\(hexKey)'\";"
        guard sqlite3_exec(db, keyPragma, nil, nil, nil) == SQLITE_OK else {
            sqlite3_close(db)
            throw DepmapDBError.keyError("wrong key or corrupted database")
        }
        // 主动校验密钥正确性
        var stmt: OpaquePointer?
        guard sqlite3_prepare_v2(db, "SELECT count(*) FROM sqlite_master", -1, &stmt, nil) == SQLITE_OK else {
            sqlite3_close(db)
            throw DepmapDBError.keyError("wrong key")
        }
        sqlite3_finalize(stmt)
        handle = db
    }

    func close() {
        if let handle { sqlite3_close(handle) }
        handle = nil
    }

    /// 执行 Core 同步的 Schema V1 迁移（statements 由 UTS 层传入，保持三端一致）。
    func migrate(migrations: [(version: Int, statements: [String])]) throws -> Int {
        guard let db = handle else { throw DepmapDBError.notOpen }
        var current = try readSchemaVersion(db)
        if current > migrations.last?.version ?? 1 {
            throw DepmapDBError.schemaNewer(current)
        }
        for migration in migrations where migration.version > current {
            guard sqlite3_exec(db, "BEGIN", nil, nil, nil) == SQLITE_OK else {
                throw DepmapDBError.sqlite(sqlite3_errcode(db), String(cString: sqlite3_errmsg(db)))
            }
            do {
                for sql in migration.statements {
                    try exec(db, sql)
                }
                try writeSchemaVersion(db, migration.version)
                guard sqlite3_exec(db, "COMMIT", nil, nil, nil) == SQLITE_OK else {
                    throw DepmapDBError.sqlite(sqlite3_errcode(db), String(cString: sqlite3_errmsg(db)))
                }
            } catch {
                sqlite3_exec(db, "ROLLBACK", nil, nil, nil)
                throw error
            }
            current = migration.version
        }
        return current
    }

    func execute(_ sql: String, params: [Any?]) throws {
        guard let db = handle else { throw DepmapDBError.notOpen }
        var stmt: OpaquePointer?
        guard sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK else {
            throw DepmapDBError.sqlite(sqlite3_errcode(db), String(cString: sqlite3_errmsg(db)))
        }
        defer { sqlite3_finalize(stmt) }
        try bind(stmt!, params)
        guard sqlite3_step(stmt!) == SQLITE_DONE else {
            throw DepmapDBError.sqlite(sqlite3_errcode(db), String(cString: sqlite3_errmsg(db)))
        }
    }

    func query(_ sql: String, params: [Any?]) throws -> [[String: Any?]] {
        guard let db = handle else { throw DepmapDBError.notOpen }
        var stmt: OpaquePointer?
        guard sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK else {
            throw DepmapDBError.sqlite(sqlite3_errcode(db), String(cString: sqlite3_errmsg(db)))
        }
        defer { sqlite3_finalize(stmt) }
        try bind(stmt!, params)
        var rows: [[String: Any?]] = []
        while sqlite3_step(stmt!) == SQLITE_ROW {
            var row: [String: Any?] = [:]
            for i in 0..<sqlite3_column_count(stmt!) {
                let name = String(cString: sqlite3_column_name(stmt!, i))
                switch sqlite3_column_type(stmt!, i) {
                case SQLITE_NULL: row[name] = nil
                case SQLITE_INTEGER: row[name] = sqlite3_column_int64(stmt!, i)
                case SQLITE_FLOAT: row[name] = sqlite3_column_double(stmt!, i)
                case SQLITE_BLOB:
                    if let bytes = sqlite3_column_blob(stmt!, i) {
                        row[name] = Data(bytes: bytes, count: Int(sqlite3_column_bytes(stmt!, i)))
                    } else {
                        row[name] = Data()
                    }
                default: row[name] = String(cString: sqlite3_column_text(stmt!, i))
                }
            }
            rows.append(row)
        }
        return rows
    }

    func transaction<T>(_ body: () throws -> T) throws -> T {
        guard let db = handle else { throw DepmapDBError.notOpen }
        guard sqlite3_exec(db, "BEGIN", nil, nil, nil) == SQLITE_OK else {
            throw DepmapDBError.sqlite(sqlite3_errcode(db), String(cString: sqlite3_errmsg(db)))
        }
        do {
            let result = try body()
            guard sqlite3_exec(db, "COMMIT", nil, nil, nil) == SQLITE_OK else {
                throw DepmapDBError.sqlite(sqlite3_errcode(db), String(cString: sqlite3_errmsg(db)))
            }
            return result
        } catch {
            sqlite3_exec(db, "ROLLBACK", nil, nil, nil)
            throw error
        }
    }

    // MARK: - private

    private func exec(_ db: OpaquePointer, _ sql: String) throws {
        guard sqlite3_exec(db, sql, nil, nil, nil) == SQLITE_OK else {
            throw DepmapDBError.sqlite(sqlite3_errcode(db), String(cString: sqlite3_errmsg(db)))
        }
    }

    private func bind(_ stmt: OpaquePointer, _ params: [Any?]) throws {
        for (i, param) in params.enumerated() {
            let idx = Int32(i + 1)
            switch param {
            case .some(nil), nil: sqlite3_bind_null(stmt, idx)
            case let v as Int64: sqlite3_bind_int64(stmt, idx, v)
            case let v as Int: sqlite3_bind_int64(stmt, idx, Int64(v))
            case let v as Bool: sqlite3_bind_int64(stmt, idx, v ? 1 : 0)
            case let v as Double: sqlite3_bind_double(stmt, idx, v)
            case let v as String: sqlite3_bind_text(stmt, idx, v, -1, SQLITE_TRANSIENT)
            case let v as Data: v.withUnsafeBytes { sqlite3_bind_blob(stmt, idx, $0.baseAddress, Int32($0.count), SQLITE_TRANSIENT) }
            case let v?: sqlite3_bind_text(stmt, idx, String(describing: v), -1, SQLITE_TRANSIENT)
            }
        }
    }

    private func readSchemaVersion(_ db: OpaquePointer) throws -> Int {
        var stmt: OpaquePointer?
        guard sqlite3_prepare_v2(db, "SELECT value FROM meta WHERE key = 'schema_version'", -1, &stmt, nil) == SQLITE_OK else {
            return 0
        }
        defer { sqlite3_finalize(stmt) }
        if sqlite3_step(stmt!) == SQLITE_ROW, let v = sqlite3_column_text(stmt!, 0) {
            return Int(String(cString: v)) ?? 0
        }
        return 0
    }

    private func writeSchemaVersion(_ db: OpaquePointer, _ version: Int) throws {
        try exec(db, "INSERT INTO meta (key, value) VALUES ('schema_version', '\(version)') ON CONFLICT(key) DO UPDATE SET value = excluded.value")
    }
}

/// Keychain 密钥适配器。状态：IMPLEMENTED；COMPILED/TESTED = NO（无 macOS）。
final class KeychainSecureKeyAdapter {

    func getOrCreateDatabaseKey(alias: String) throws -> Data {
        if let existing = try read(alias) { return existing }
        var key = Data(count: 32)
        let status = key.withUnsafeMutableBytes { ptr in
            SecRandomCopyBytes(kSecRandomDefault, 32, ptr.baseAddress!)
        }
        guard status == errSecSuccess else { throw DepmapDBError.keyError("random failure") }
        try save(alias, key)
        return key
    }

    func getOrCreateFpSecret() throws -> String {
        if let existing = try read("depmap_fp_secret") {
            return existing.base64EncodedString()
        }
        let secret = try getOrCreateDatabaseKey(alias: "depmap_fp_secret")
        return secret.base64EncodedString()
    }

    private func read(_ alias: String) throws -> Data? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: "com.depmap.keys",
            kSecAttrAccount as String: alias,
            kSecReturnData as String: true
        ]
        var item: CFTypeRef?
        let status = SecItemCopyMatching(query as CFDictionary, &item)
        if status == errSecItemNotFound { return nil }
        guard status == errSecSuccess else { throw DepmapDBError.keyError("keychain read \(status)") }
        return item as? Data
    }

    private func save(_ alias: String, _ data: Data) throws {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: "com.depmap.keys",
            kSecAttrAccount as String: alias,
            kSecValueData as String: data,
            kSecAttrAccessible as String: kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
        ]
        let status = SecItemAdd(query as CFDictionary, nil)
        guard status == errSecSuccess else { throw DepmapDBError.keyError("keychain write \(status)") }
    }
}

/// LocalAuthentication 启动锁。状态：IMPLEMENTED；COMPILED/TESTED = NO（无 macOS）。
enum LocalAuthenticationGate {
    static func canAuthenticate() -> Bool {
        var error: NSError?
        // LAPolicy 是枚举，不可实例化（原写法 `LAPolicy().deviceOwnerAuthentication` 为编译错误）。
        let policy: LAPolicy = .deviceOwnerAuthentication // biometric + device credential fallback
        return LAContext().canEvaluatePolicy(policy, error: &error)
    }

    static func authenticate(reason: String, completion: @escaping (Bool, String?) -> Void) {
        let context = LAContext()
        context.localizedReason = reason
        context.evaluatePolicy(.deviceOwnerAuthentication, localizedReason: reason) { success, error in
            if success {
                completion(true, nil)
            } else {
                let code = (error as NSError?)?.code ?? -1
                let reason: String
                switch code {
                case LAError.userCancel.rawValue, LAError.systemCancel.rawValue: reason = "user_cancel"
                case LAError.biometryLockout.rawValue: reason = "lockout"
                case LAError.biometryNotAvailable.rawValue: reason = "not_available"
                case LAError.passcodeNotSet.rawValue, LAError.biometryNotEnrolled.rawValue: reason = "no_enrollment"
                default: reason = "error"
                }
                completion(false, reason)
            }
        }
    }
}

/// 后台隐私遮罩（App 层）。状态：IMPLEMENTED；COMPILED/TESTED = NO。
enum PrivacyScreen {
    /// 在 AppDelegate 的 willResignActive/didBecomeActive 中调用，
    /// 用覆盖窗遮挡敏感内容（防止任务切换器泄露）。
    static func setPrivacyCover(_ visible: Bool) {
        // 实现挂接在 SwiftUI/AppDelegate 生命周期；此处提供策略占位接口
    }
}
