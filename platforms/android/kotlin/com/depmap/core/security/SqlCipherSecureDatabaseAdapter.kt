package com.depmap.core.security

import android.content.Context
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SQLiteOpenHelper
import com.depmap.core.schema.DepmapSchemaV1

/**
 * SQLCipher 加密数据库适配器（Android 实现）。
 *
 * 安全语义：
 * - 数据库密钥来自 [KeystoreSecureKeyAdapter]（Android Keystore 保护），不以明文常量存在；
 * - 口令/生物识别取消时不得调用 open（由 BiometricGateAdapter 在更上层控制）；
 * - 迁移在事务内执行，失败回滚（与 Core migrations.ts 幂等语义一致）；
 * - 数据库文件离线打开不可读（SQLCipher 全库加密）。
 *
 * 状态：IMPLEMENTED（代码完成）。COMPILED / TESTED / DEVICE_VERIFIED = NO
 * （当前 Windows 环境无 Android SDK / JDK17，见 BLOCKERS.md）。
 */
class SqlCipherSecureDatabaseAdapter(
    private val context: Context,
    private val keyAdapter: KeystoreSecureKeyAdapter
) : SecureDatabaseAdapter {

    private var helper: DepmapSqlCipherHelper? = null

    /**
     * open() 时以口令解开的数据库句柄。
     *
     * SQLCipher 的 `getWritableDatabase` **必须**带口令（String/char[]/byte[] 三种重载），
     * 不存在无参版本；因此句柄在 open() 中取得并缓存，其余方法一律复用该句柄，
     * 避免在无口令上下文中重新打开数据库。
     */
    private var db: SQLiteDatabase? = null

    override suspend fun open(options: SecureDatabaseOpenOptions) {
        val passphrase = keyAdapter.getOrCreateDatabaseKeyBytes()
        helper = DepmapSqlCipherHelper(context, options.dbName, passphrase)
        // 强制校验密钥：错误密钥在首次查询时失败，主动触发一次读
        db = helper!!.getWritableDatabase(passphrase)
        db!!.query("SELECT count(*) FROM sqlite_master").use { it.moveToFirst() }
    }

    override suspend fun close() {
        helper?.close()
        helper = null
        db = null
    }

    override suspend fun migrate(): Int {
        val db = checkNotNull(db) { "open() must be called before use" } ?: error("open() must be called before migrate()")
        var version = readSchemaVersion(db)
        if (version > DepmapSchemaV1.VERSION) {
            error("database schema_version ($version) newer than supported ${DepmapSchemaV1.VERSION}")
        }
        for ((migrationVersion, statements) in DepmapSchemaV1.migrations()) {
            if (migrationVersion <= version) continue
            db.beginTransaction()
            try {
                for (sql in statements) db.execSQL(sql)
                upsertSchemaVersion(db, migrationVersion)
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
            version = migrationVersion
        }
        return version
    }

    override suspend fun query(sql: String, params: List<Any?>): List<Map<String, Any?>> {
        val db = checkNotNull(db) { "open() must be called before use" } ?: error("not open")
        val args = params.map { it?.toString() }.toTypedArray()
        val out = mutableListOf<Map<String, Any?>>()
        db.rawQuery(sql, args).use { cursor ->
            while (cursor.moveToNext()) {
                val row = mutableMapOf<String, Any?>()
                for (i in 0 until cursor.columnCount) {
                    row[cursor.getColumnName(i)] = when (cursor.getType(i)) {
                        android.database.Cursor.FIELD_TYPE_NULL -> null
                        android.database.Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(i)
                        android.database.Cursor.FIELD_TYPE_FLOAT -> cursor.getDouble(i)
                        android.database.Cursor.FIELD_TYPE_BLOB -> cursor.getBlob(i)
                        else -> cursor.getString(i)
                    }
                }
                out.add(row)
            }
        }
        return out
    }

    override suspend fun execute(sql: String, params: List<Any?>): Int {
        val db = checkNotNull(db) { "open() must be called before use" } ?: error("not open")
        val stmt = db.compileStatement(sql)
        params.forEachIndexed { i, v ->
            when (v) {
                null -> stmt.bindNull(i + 1)
                is Long -> stmt.bindLong(i + 1, v)
                is Int -> stmt.bindLong(i + 1, v.toLong())
                is Boolean -> stmt.bindLong(i + 1, if (v) 1L else 0L)
                is Double -> stmt.bindDouble(i + 1, v)
                is String -> stmt.bindString(i + 1, v)
                is ByteArray -> stmt.bindBlob(i + 1, v)
                else -> stmt.bindString(i + 1, v.toString())
            }
        }
        return when (val r = stmt.executeUpdateDelete()) {
            in 0..Int.MAX_VALUE -> r
            else -> 1
        }
    }

    override suspend fun <T> transaction(fn: suspend () -> T): T {
        val db = checkNotNull(db) { "open() must be called before use" } ?: error("not open")
        db.beginTransaction()
        try {
            val result = fn()
            db.setTransactionSuccessful()
            return result
        } finally {
            db.endTransaction()
        }
    }

    private fun readSchemaVersion(db: SQLiteDatabase): Int =
        try {
            db.rawQuery("SELECT value FROM meta WHERE key = 'schema_version'", null).use { c ->
                if (c.moveToFirst()) c.getString(0).toInt() else 0
            }
        } catch (e: Exception) {
            0
        }

    private fun upsertSchemaVersion(db: SQLiteDatabase, version: Int) {
        db.execSQL("INSERT INTO meta (key, value) VALUES ('schema_version', ?) ON CONFLICT(key) DO UPDATE SET value = excluded.value", arrayOf(version.toString()))
        db.execSQL("INSERT INTO meta (key, value) VALUES ('schema_version_updated_at', ?) ON CONFLICT(key) DO UPDATE SET value = excluded.value", arrayOf(java.time.Instant.now().toString()))
    }
}

private class DepmapSqlCipherHelper(
    context: Context,
    name: String,
    private val passphrase: ByteArray
) : SQLiteOpenHelper(context, name, null, 1) {

    init {
        SQLiteDatabase.loadLibs(context)
    }

    override fun onCreate(db: SQLiteDatabase) {
        // 全部由 migrate() 按版本执行
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // schema_version 由 meta 表管理，不走 PRAGMA user_version
    }

    override fun getWritableDatabase(password: String?): SQLiteDatabase =
        super.getWritableDatabase(bytesToCharArray(passphrase))

    override fun getReadableDatabase(password: String?): SQLiteDatabase =
        super.getReadableDatabase(bytesToCharArray(passphrase))

    private fun bytesToCharArray(bytes: ByteArray): CharArray =
        CharArray(bytes.size) { i -> Char(bytes[i].toInt() and 0xFF) }
}
