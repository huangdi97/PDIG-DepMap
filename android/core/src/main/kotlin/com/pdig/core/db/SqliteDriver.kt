package com.pdig.core.db

/**
 * 同步 SQLite 驱动接口 —— 与 core/src/db/driver.ts 同语义。
 *
 * 平台实现：
 *  - Android：SQLCipher（android-database-sqlcipher）
 *  - HarmonyOS：ArkData relationalStore
 *  - iOS：SQLCipher
 *  - JVM（conformance runner）：sqlite-jdbc
 *
 * 逻辑 Schema 与迁移行为必须跨端一致；**物理实现允许不同**。
 */
interface SqliteDriver {
    fun exec(sql: String)
    fun prepare(sql: String): SqliteStatement

    /** 事务执行；出错回滚并重新抛出，绝不留下半迁移/半写入状态。支持嵌套（SAVEPOINT）。 */
    fun <T> transaction(fn: () -> T): T

    fun close()
}

interface SqliteStatement {
    fun run(vararg params: Any?): Int
    fun get(vararg params: Any?): SqlRow?
    fun all(vararg params: Any?): List<SqlRow>
}

/** 一行结果。取不到返回 null。 */
interface SqlRow {
    fun str(column: String): String?
    fun long(column: String): Long?
    fun double(column: String): Double?
    fun isNull(column: String): Boolean

    /**
     * 原始类型值（图序列化需要区分 INTEGER / REAL / TEXT / NULL，
     * 否则 `archived=0` 会被写成 `0.0`，破坏跨端 payload 逐字节一致性）。
     */
    fun raw(column: String): Any?

    /** 物理列顺序（`SELECT *` 的真实顺序；payload 键顺序依赖它）。 */
    fun columns(): List<String>
}
