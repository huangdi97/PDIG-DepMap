package com.depmap.core.security

/**
 * 加密数据库适配器接口（Android 实现）。
 *
 * 契约来源：`core/src/adapters/interfaces.ts` 的 `SecureDatabaseAdapter`，
 * 并与 UTS 侧 `app/uni_modules/depmap-secure-database/utssdk/interface.uts` 的 `SecureDb` 对齐。
 *
 * 语义要求（三端一致）：
 * - `open()` 使用平台安全密钥解锁/创建数据库，密钥错误或认证取消时必须失败，**绝不降级为明文**；
 * - `migrate()` 执行 Schema v1 迁移，幂等且事务化；
 * - `transaction()` 失败回滚，不留半写入状态。
 *
 * 说明：本接口此前在 Kotlin 源集中**缺失**（`SqlCipherSecureDatabaseAdapter` 声明了该父类型
 * 但全仓无任何定义），属于 Android 安全层从未被编译器验证所暴露的真实缺陷，本轮补齐。
 */
interface SecureDatabaseAdapter {

    /** 打开（或创建）加密数据库。 */
    suspend fun open(options: SecureDatabaseOpenOptions)

    /** 关闭数据库句柄。 */
    suspend fun close()

    /** 执行迁移，返回迁移后的 schema 版本。 */
    suspend fun migrate(): Int

    /** 只读查询；SQL 与参数由 Repository 层下发，与 Core DDL 一致。 */
    suspend fun query(sql: String, params: List<Any?>): List<Map<String, Any?>>

    /** 执行写入语句，返回受影响行数。 */
    suspend fun execute(sql: String, params: List<Any?>): Int

    /** 事务包装；异常时回滚并向上抛出。 */
    suspend fun <T> transaction(fn: suspend () -> T): T
}
