// 幂等迁移执行器 —— 移植自 android/core/.../schema/Migrations.kt 的
// `ensureMetaTable / getSchemaVersion / setSchemaVersion / migrate`。
//
// 单独成文件的原因：`Migrations.swift` 由生成器产出（不要手改），
// 执行器是手写逻辑，二者分开以免重新生成时丢失。
//
// 契约（与 Android / Harmony 逐条对齐）：
//  - 幂等：当前版本已是最新则 no-op
//  - 每个版本在独立事务内执行，失败回滚，绝不留下半迁移 DB
//  - 未来版本明确 reject（`database schema_version (n) is newer than supported (m)`）
//  - 返回最终版本（重复执行返回同一值 —— fixture 用 50 次重复验证不漂移）

import Foundation

public enum SchemaMigrator {

    private static let metaDdl = """
    CREATE TABLE IF NOT EXISTS meta (
      key TEXT PRIMARY KEY,
      value TEXT NOT NULL
    )
    """

    public static func ensureMetaTable(_ driver: SqliteDriver) throws {
        try driver.exec(metaDdl)
    }

    public static func getSchemaVersion(_ driver: SqliteDriver) throws -> Int {
        try ensureMetaTable(driver)
        guard let row = try driver.prepare("SELECT value FROM meta WHERE key = 'schema_version'").get([]) else {
            return 0
        }
        guard let s = row.str("value"), let v = Int(s) else { return 0 }
        return v
    }

    private static func setSchemaVersion(_ driver: SqliteDriver, _ version: Int, _ nowIso: String) throws {
        try driver.prepare(
            """
            INSERT INTO meta (key, value) VALUES ('schema_version', ?)
            ON CONFLICT(key) DO UPDATE SET value = excluded.value
            """
        ).run([.text(String(version))])
        try driver.prepare(
            """
            INSERT INTO meta (key, value) VALUES ('schema_version_updated_at', ?)
            ON CONFLICT(key) DO UPDATE SET value = excluded.value
            """
        ).run([.text(nowIso)])
    }

    @discardableResult
    public static func migrate(_ driver: SqliteDriver, nowIso: String) throws -> Int {
        try ensureMetaTable(driver)
        var current = try getSchemaVersion(driver)
        if current > Migrations.schemaVersion {
            throw SqliteError(
                "database schema_version (\(current)) is newer than supported (\(Migrations.schemaVersion))"
            )
        }
        for m in Migrations.migrations {
            if m.version <= current { continue }
            try driver.transaction {
                for sql in m.statements { try driver.exec(sql) }
                try setSchemaVersion(driver, m.version, nowIso)
            }
            current = m.version
        }
        return current
    }
}
