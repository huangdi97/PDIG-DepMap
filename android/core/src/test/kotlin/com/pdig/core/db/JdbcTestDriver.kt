package com.pdig.core.db

import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet

/**
 * JVM 单元测试用的真实 SQLite 驱动（sqlite-jdbc，内存库）。
 *
 * 目的：让迁移语义、去重兜底这类"必须跑真 SQL"的行为能在 `:core:test` 里被测到，
 * 而不是只有在 Android 设备上才发现问题。
 *
 * 刻意保持最小实现：只支持 Domain/Schema 层实际用到的能力，
 * 不支持并发、不支持跨连接。这是测试替身，**不是**产品代码路径。
 */
class JdbcTestDriver : SqliteDriver {

    private val conn: Connection = DriverManager.getConnection("jdbc:sqlite::memory:")
    private var depth = 0

    init {
        conn.autoCommit = true
    }

    override fun exec(sql: String) {
        conn.createStatement().use { it.execute(sql) }
    }

    override fun prepare(sql: String): SqliteStatement = JdbcStatement(conn, sql)

    override fun <T> transaction(fn: () -> T): T {
        val isRoot = depth == 0
        val savepoint = if (isRoot) null else conn.setSavepoint()
        if (isRoot) conn.autoCommit = false
        depth += 1
        return try {
            val result = fn()
            if (isRoot) conn.commit() else savepoint?.let { conn.releaseSavepoint(it) }
            depth -= 1
            if (isRoot) conn.autoCommit = true
            result
        } catch (t: Throwable) {
            depth -= 1
            if (isRoot) {
                conn.rollback()
                conn.autoCommit = true
            } else {
                savepoint?.let { conn.rollback(it) }
            }
            throw t
        }
    }

    override fun close() {
        conn.close()
    }

    /** 测试辅助：直接读一个标量，用于断言迁移后的副作用。 */
    fun count(sql: String): Int =
        conn.createStatement().use { st ->
            st.executeQuery(sql).use { rs -> if (rs.next()) rs.getInt(1) else 0 }
        }
}

private class JdbcStatement(private val conn: Connection, private val sql: String) : SqliteStatement {

    override fun run(vararg params: Any?): Int =
        conn.prepareStatement(sql).use { ps ->
            bind(ps, params)
            ps.executeUpdate()
        }

    override fun get(vararg params: Any?): SqlRow? = query(params).firstOrNull()

    override fun all(vararg params: Any?): List<SqlRow> = query(params)

    private fun query(params: Array<out Any?>): List<SqlRow> =
        conn.prepareStatement(sql).use { ps ->
            bind(ps, params)
            ps.executeQuery().use { rs ->
                val out = mutableListOf<SqlRow>()
                while (rs.next()) out.add(readRow(rs))
                out
            }
        }

    private fun bind(ps: PreparedStatement, params: Array<out Any?>) {
        params.forEachIndexed { i, v -> ps.setObject(i + 1, v) }
    }

    private fun readRow(rs: ResultSet): SqlRow {
        val meta = rs.metaData
        val values = LinkedHashMap<String, Any?>()
        for (i in 1..meta.columnCount) {
            values[meta.getColumnLabel(i)] = rs.getObject(i)
        }
        return SnapshotRow(values)
    }
}

private class SnapshotRow(private val values: Map<String, Any?>) : SqlRow {
    override fun str(column: String): String? = values[column]?.toString()
    override fun long(column: String): Long? = (values[column] as? Number)?.toLong()
    override fun double(column: String): Double? = (values[column] as? Number)?.toDouble()
    override fun isNull(column: String): Boolean = values[column] == null
    override fun raw(column: String): Any? = values[column]
    override fun columns(): List<String> = values.keys.toList()
}
