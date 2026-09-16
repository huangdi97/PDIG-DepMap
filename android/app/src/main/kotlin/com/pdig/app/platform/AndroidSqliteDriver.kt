package com.pdig.app.platform

import android.database.Cursor
import com.pdig.core.db.SqlRow
import com.pdig.core.db.SqliteDriver
import com.pdig.core.db.SqliteStatement
import net.zetetic.database.sqlcipher.SQLiteDatabase
import net.zetetic.database.sqlcipher.SQLiteStatement
import java.io.File

/**
 * Android 持久化：SQLCipher（spec §58）。
 *
 * 物理实现与 Harmony（ArkData）/ iOS（SQLCipher）不同，但**逻辑 Schema 与迁移行为必须一致** ——
 * 由 conformance runner（:conformance）用同一套 `Migrations.kt` 与 fixture 证明。
 *
 * 事务语义：嵌套用事务计数 + SAVEPOINT 语义（SQLiteDatabase 本身支持嵌套计数），
 * 保证 Reality mutation 与 graphRevision bump 的原子性（spec §27）。
 */
class AndroidSqliteDriver private constructor(private val db: SQLiteDatabase) : SqliteDriver {

    companion object {
        fun open(file: File, password: String): AndroidSqliteDriver {
            file.parentFile?.mkdirs()
            val db = SQLiteDatabase.openOrCreateDatabase(file, password, null, null)
            return AndroidSqliteDriver(db)
        }
    }

    private var depth = 0

    override fun exec(sql: String) {
        db.execSQL(sql)
    }

    override fun prepare(sql: String): SqliteStatement = CipherStatement(db, sql)

    override fun <T> transaction(fn: () -> T): T {
        if (depth == 0) {
            depth++
            db.beginTransaction()
            return try {
                val r = fn()
                db.setTransactionSuccessful()
                r
            } finally {
                db.endTransaction()
                depth--
            }
        }
        // 嵌套：显式 SAVEPOINT（不依赖平台 API 级别）；外层提交/回滚决定最终状态
        val sp = "pdig_sp_$depth"
        depth++
        db.execSQL("SAVEPOINT $sp")
        return try {
            val r = fn()
            db.execSQL("RELEASE SAVEPOINT $sp")
            r
        } catch (e: Throwable) {
            db.execSQL("ROLLBACK TO SAVEPOINT $sp")
            db.execSQL("RELEASE SAVEPOINT $sp")
            throw e
        } finally {
            depth--
        }
    }

    override fun close() {
        db.close()
    }

    private class CipherStatement(
        private val db: SQLiteDatabase,
        private val sql: String,
    ) : SqliteStatement {

        private fun bind(st: SQLiteStatement, params: Array<out Any?>) {
            params.forEachIndexed { i, v ->
                val idx = i + 1
                when (v) {
                    null -> st.bindNull(idx)
                    is String -> st.bindString(idx, v)
                    is Int -> st.bindLong(idx, v.toLong())
                    is Long -> st.bindLong(idx, v)
                    is Double -> st.bindDouble(idx, v)
                    is Float -> st.bindDouble(idx, v.toDouble())
                    is ByteArray -> st.bindBlob(idx, v)
                    else -> st.bindString(idx, v.toString())
                }
            }
        }

        override fun run(vararg params: Any?): Int {
            val st = db.compileStatement(sql)
            return try {
                bind(st, params)
                st.executeUpdateDelete()
            } finally {
                st.close()
            }
        }

        override fun get(vararg params: Any?): SqlRow? = all(*params).firstOrNull()

        override fun all(vararg params: Any?): List<SqlRow> {
            val args = params.map { it?.toString() }.toTypedArray()
            val cursor = db.rawQuery(sql, args)
            return cursor.use { c ->
                val names = c.columnNames
                val out = mutableListOf<SqlRow>()
                while (c.moveToNext()) {
                    // 必须**物化**每一行：SqlRow 的生命周期长于 Cursor。
                    // 之前的实现把 row 建成 Cursor 的惰性视图，迭代结束后游标已越界，
                    // 任何后续读取都抛 CursorIndexOutOfBoundsException
                    // （2026-09-15 设备实测：buildTimeline → CursorRow.str 崩溃）。
                    val values = arrayOfNulls<Any?>(names.size)
                    for (i in names.indices) {
                        val v = readValue(c, i)
                        values[i] = if (v is ByteArray) {
                            val copy = ByteArray(v.size)
                            System.arraycopy(v, 0, copy, 0, v.size)
                            copy
                        } else {
                            v
                        }
                    }
                    out.add(MaterializedRow(names, values))
                }
                out
            }
        }

        private fun readValue(c: Cursor, i: Int): Any? = when (c.getType(i)) {
            Cursor.FIELD_TYPE_NULL -> null
            Cursor.FIELD_TYPE_INTEGER -> c.getLong(i)
            Cursor.FIELD_TYPE_FLOAT -> c.getDouble(i)
            Cursor.FIELD_TYPE_STRING -> c.getString(i)
            Cursor.FIELD_TYPE_BLOB -> c.getBlob(i)
            else -> null
        }
    }

    /**
     * 已物化的一行。
     *
     * `SqlRow` 的生命周期长于 `Cursor`：调用方是在 `all()` 返回**之后**才读取字段的。
     * 旧实现把 row 建成 Cursor 的惰性视图，迭代结束时游标已走到末尾（position == count），
     * 之后任何 `str()/long()` 都会抛
     * `CursorIndexOutOfBoundsException: Index N requested, with a size of N`。
     * 该问题在 JVM（sqlite-jdbc 会物化行）上不可见，只在真机/模拟器上暴露 ——
     * 2026-09-15 首次启动实测崩溃栈：`buildTimeline → CursorRow.str`。
     */
    private class MaterializedRow(
        private val names: Array<String>,
        private val values: Array<Any?>,
    ) : SqlRow {
        private fun index(column: String): Int = names.indexOf(column)

        override fun raw(column: String): Any? {
            val i = index(column)
            return if (i < 0) null else values[i]
        }

        override fun columns(): List<String> = names.toList()

        override fun str(column: String): String? {
            val i = index(column)
            if (i < 0) return null
            val v = values[i] ?: return null
            return (v as? String) ?: v.toString()
        }

        override fun long(column: String): Long? {
            val i = index(column)
            if (i < 0) return null
            return when (val v = values[i]) {
                null -> null
                is Long -> v
                is Double -> v.toLong()
                is String -> v.toLongOrNull()
                else -> null
            }
        }

        override fun double(column: String): Double? {
            val i = index(column)
            if (i < 0) return null
            return when (val v = values[i]) {
                null -> null
                is Double -> v
                is Long -> v.toDouble()
                is String -> v.toDoubleOrNull()
                else -> null
            }
        }

        override fun isNull(column: String): Boolean {
            val i = index(column)
            return i < 0 || values[i] == null
        }
    }
}
