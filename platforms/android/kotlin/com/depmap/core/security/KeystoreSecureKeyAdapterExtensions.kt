package com.depmap.core.security

/**
 * [KeystoreSecureKeyAdapter] 的便捷入口：读取/首次生成**默认数据库密钥**。
 *
 * 背景：`SqlCipherSecureDatabaseAdapter.open()` 需要「当前数据库密钥」，
 * 但 `SecureKeyAdapter` 契约（`core/src/adapters/interfaces.ts`）暴露的是
 * `getOrCreateDatabaseKey(alias)` —— 需要调用方显式传入别名。
 * 本扩展把默认别名（[KeystoreSecureKeyAdapter.DB_KEY_ALIAS]）收敛到一处，
 * 避免每个调用点各写一遍字符串常量。
 *
 * 安全语义不变：密钥仍由 AndroidKeyStore 保护，扩展方法不接触明文密钥。
 */
suspend fun KeystoreSecureKeyAdapter.getOrCreateDatabaseKeyBytes(): ByteArray =
    getOrCreateDatabaseKey(KeystoreSecureKeyAdapter.DB_KEY_ALIAS)
