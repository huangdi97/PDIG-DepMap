package com.pdig.conformance

import com.pdig.core.db.SqlRow
import com.pdig.core.db.SqliteDriver
import com.pdig.core.db.SqliteStatement
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.SQLException
import java.sql.Savepoint

/**
 * JVM（sqlite-jdbc）实现，仅供 **conformance harness** 使用。
 *
 * 物理实现允许与 Android（SQLCipher）/ iOS（SQLCipher）/ Harmony（ArkData）不同，
 * 但**逻辑 Schema 与迁移行为必须一致** —— 这正是本 runner 要证明的事。
 */
class JdbcSqliteDriver(path: String) : SqliteDriver {

    constructor(file: File) : this(file.absolutePath)

    private val conn: Connection = DriverManager.getConnection("jdbc:sqlite:$path")
    private var depth = 0

    init {
        conn.autoCommit = false
        // 外键在 SQLite 中默认关闭；PDIG 依赖 retire 语义而非级联删除，保持默认
    }

    override fun exec(sql: String) {
        conn.createStatement().use { st -> st.execute(sql) }
    }

    override fun prepare(sql: String): SqliteStatement = JdbcStatement(conn.prepareStatement(sql))

    override fun <T> transaction(fn: () -> T): T {
        if (depth == 0) {
            depth++
            return try {
                val r = fn()
                conn.commit()
                r
            } catch (e: Throwable) {
                try {
                    conn.rollback()
                } catch (_: SQLException) {
                    /* rollback 失败不应掩盖原始异常 */
                }
                throw e
            } finally {
                depth--
            }
        }
        // 嵌套 → SAVEPOINT（migrate 内层事务失败不得回滚外层已提交版本）
        val sp: Savepoint = conn.setSavepoint()
        depth++
        return try {
            val r = fn()
            conn.releaseSavepoint(sp)
            r
        } catch (e: Throwable) {
            try {
                conn.rollback(sp)
            } catch (_: SQLException) {
                /* ignore */
            }
            throw e
        } finally {
            depth--
        }
    }

    override fun close() {
        try {
            conn.close()
        } catch (_: SQLException) {
            /* ignore */
        }
    }

    private class JdbcStatement(private val ps: PreparedStatement) : SqliteStatement {

        private fun bind(params: Array<out Any?>) {
            for (i in params.indices) {
                val v = params[i]
                when (v) {
                    null -> ps.setObject(i + 1, null)
                    is String -> ps.setString(i + 1, v)
                    is Int -> ps.setInt(i + 1, v)
                    is Long -> ps.setLong(i + 1, v)
                    is Double -> ps.setDouble(i + 1, v)
                    is ByteArray -> ps.setBytes(i + 1, v)
                    else -> ps.setObject(i + 1, v)
                }
            }
        }

        override fun run(vararg params: Any?): Int {
            bind(params)
            return ps.executeUpdate()
        }

        override fun get(vararg params: Any?): SqlRow? = all(*params).firstOrNull()

        override fun all(vararg params: Any?): List<SqlRow> {
            bind(params)
            ps.executeQuery().use { rs ->
                val meta = rs.metaData
                val count = meta.columnCount
                val out = mutableListOf<SqlRow>()
                while (rs.next()) {
                    val map = linkedMapOf<String, Any?>()
                    for (i in 1..count) {
                        map[meta.getColumnLabel(i)] = rs.getObject(i)
                    }
                    out.add(MapRow(map))
                }
                return out
            }
        }
    }

    private class MapRow(private val map: LinkedHashMap<String, Any?>) : SqlRow {
        override fun raw(column: String): Any? = map[column]
        override fun columns(): List<String> = map.keys.toList()
        override fun str(column: String): String? = map[column]?.toString()
        override fun long(column: String): Long? = when (val v = map[column]) {
            null -> null
            is Long -> v
            is Int -> v.toLong()
            is Double -> v.toLong()
            is String -> v.toLongOrNull()
            else -> null
        }

        override fun double(column: String): Double? = when (val v = map[column]) {
            null -> null
            is Double -> v
            is Long -> v.toDouble()
            is Int -> v.toDouble()
            is String -> v.toDoubleOrNull()
            else -> null
        }

        override fun isNull(column: String): Boolean = map[column] == null
    }
}
