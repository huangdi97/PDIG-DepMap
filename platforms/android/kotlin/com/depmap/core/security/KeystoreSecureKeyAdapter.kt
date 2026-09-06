package com.depmap.core.security

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Android Keystore 安全密钥适配器。
 *
 * - 数据库密钥：首次生成 32 字节随机密钥，用 AndroidKeyStore AES-GCM 密钥包裹后
 *   存入应用私有存储；离开 Keystore 不存在明文密钥。
 * - fpSecret：32 字节随机 hex，同样包裹存储。
 * - 系统升级 / 进程杀死 / 重启：Keystore 密钥持续可用；用户清除应用数据时一并清除。
 *
 * 状态：IMPLEMENTED（代码完成）。COMPILED / TESTED / DEVICE_VERIFIED = NO。
 */
class KeystoreSecureKeyAdapter(private val context: Context) : SecureKeyAdapter {

    private val keystore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    private fun getOrCreateWrapKey(): SecretKey {
        val existing = keystore.getKey(WRAP_ALIAS, null) as? SecretKey
        if (existing != null) return existing
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(WRAP_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(false) // DB key 解锁在应用层通过 BiometricGate 控制
                .build()
        )
        return generator.generateKey()
    }

    override suspend fun getOrCreateDatabaseKey(alias: String): ByteArray {
        val stored = readWrapped(alias)
        if (stored != null) return unwrap(stored)
        val fresh = ByteArray(32).also { java.security.SecureRandom().nextBytes(it) }
        writeWrapped(alias, wrap(fresh))
        return fresh
    }

    override suspend fun getOrCreateFpSecret(): String {
        val stored = readWrapped(FP_SECRET_ALIAS)
        if (stored != null) return Base64.encodeToString(unwrap(stored), Base64.NO_WRAP)
        val fresh = ByteArray(32).also { java.security.SecureRandom().nextBytes(it) }
        writeWrapped(FP_SECRET_ALIAS, wrap(fresh))
        return Base64.encodeToString(fresh, Base64.NO_WRAP)
    }

    override suspend fun hasKey(alias: String): Boolean = readWrapped(alias) != null

    override suspend fun deleteKey(alias: String) {
        context.deleteFile(wrappedFileName(alias))
    }

    private fun wrap(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateWrapKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plain)
        // 存储 iv(12) + encrypted
        return iv + encrypted
    }

    private fun unwrap(wrapped: ByteArray): ByteArray {
        val iv = wrapped.copyOfRange(0, 12)
        val encrypted = wrapped.copyOfRange(12, wrapped.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateWrapKey(), GCMParameterSpec(128, iv))
        return cipher.doFinal(encrypted)
    }

    private fun readWrapped(alias: String): ByteArray? =
        try {
            context.openFileInput(wrappedFileName(alias)).use { it.readBytes() }
        } catch (e: Exception) {
            null
        }

    private fun writeWrapped(alias: String, wrapped: ByteArray) {
        context.openFileOutput(wrappedFileName(alias), Context.MODE_PRIVATE).use { it.write(wrapped) }
    }

    private fun wrappedFileName(alias: String) = "wrapped_$alias.bin"

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val WRAP_ALIAS = "depmap_db_wrap_key"
        const val DB_KEY_ALIAS = "depmap_database_key"
        const val FP_SECRET_ALIAS = "depmap_fp_secret"
    }
}

/** 密钥适配器共用接口（与 Core adapters/interfaces.ts 对齐）。 */
interface SecureKeyAdapter {
    suspend fun getOrCreateDatabaseKey(alias: String): ByteArray
    suspend fun getOrCreateFpSecret(): String
    suspend fun hasKey(alias: String): Boolean
    suspend fun deleteKey(alias: String)
}

/** 打开参数（与 Core 对齐）。 */
data class SecureDatabaseOpenOptions(val dbName: String)

/** 隐私屏（FLAG_SECURE）。状态：IMPLEMENTED；DEVICE_VERIFIED = NO。 */
object FlagSecurePrivacyAdapter {
    fun setPrivacyScreen(activity: android.app.Activity, enabled: Boolean) {
        val window = activity.window
        if (enabled) {
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
