package com.pdig.app.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import android.util.Base64

/**
 * Android 密钥管理（spec §59）：
 *  - 本地库密码短语（SQLCipher passphrase）由 Android Keystore 中的 AES-GCM 密钥包裹
 *  - 绝不硬编码；绝不落 SharedPreferences 明文
 *  - 密钥不可导出（Keystore 生成，仅本机可用）
 */
object DatabaseKeyStore {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val ALIAS = "pdig_db_wrapping_key_v1"
    private const val PREFS = "pdig_secure"
    private const val PREF_WRAPPED = "db_passphrase_wrapped"

    private fun ensureKey(): SecretKey {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        ks.getKey(ALIAS, null)?.let { return it as SecretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    /**
     * 取回（或首次生成）本地库密码短语的密文表示。
     * 生产过程：随机 32 字节 → Keystore AES-GCM 包裹 → Base64(IV||ciphertext) 存私有 prefs。
     */
    fun wrappedPassphrase(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.getString(PREF_WRAPPED, null)?.let { return it }
        val raw = ByteArray(32).also { java.security.SecureRandom().nextBytes(it) }
        val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, ensureKey())
        val iv = cipher.iv
        val ct = cipher.doFinal(raw)
        val blob = Base64.encodeToString(iv + ct, Base64.DEFAULT)
        prefs.edit().putString(PREF_WRAPPED, blob).apply()
        return blob
    }

    /** 解开密码短语（仅内存使用；调用方用后应尽快清掉）。 */
    fun passphrase(context: Context): String {
        val blob = Base64.decode(wrappedPassphrase(context), Base64.DEFAULT)
        val iv = blob.copyOfRange(0, 12)
        val ct = blob.copyOfRange(12, blob.size)
        val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(javax.crypto.Cipher.DECRYPT_MODE, ensureKey(), javax.crypto.spec.GCMParameterSpec(128, iv))
        val raw = cipher.doFinal(ct)
        // SQLCipher 接受 String/byte[] 作为 passphrase → 用 Base64 文本保证可复现
        val text = Base64.encodeToString(raw, Base64.NO_WRAP)
        raw.fill(0)
        return text
    }
}

/** App Lock 状态（spec §155）：三端统一产品语义，平台实现不同。 */
enum class LockState {
    NOT_CONFIGURED,
    LOCKED,
    UNLOCKED,
    UNAVAILABLE,
    FAILED,
}
