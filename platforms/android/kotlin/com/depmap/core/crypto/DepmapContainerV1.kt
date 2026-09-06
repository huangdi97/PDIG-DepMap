package com.depmap.core.crypto

import android.util.Base64
import org.bouncycastle.crypto.params.Argon2Parameters
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * DEPMAP_CONTAINER_V1 — Android Kotlin 参考实现。
 *
 * 必须与 Core Node reference (core/src/crypto) 通过 Golden Test Vector 互操作：
 * - password 'depmap-test' + 固定 salt/nonce → 相同 derivedKey/ciphertext/tag；
 * - Node encrypt → Android decrypt PASS；Android encrypt → Node decrypt PASS。
 *
 * AAD = UTF8(JCS({format, formatVersion, kdf, cipher}))，ciphertext/tag 不进 AAD。
 * 解密前必须完成全部边界校验（恶意容器不得触发超大内存 KDF）。
 *
 * 状态：IMPLEMENTED（代码完成）。COMPILED / TESTED = NO（见 BLOCKERS.md）。
 */
object DepmapContainerV1 {

    const val FORMAT = "depmap"
    const val FORMAT_VERSION = 1
    const val ARGON2_VERSION_19 = 0x13

    const val MEMORY_KIB_DEFAULT = 65536
    const val ITERATIONS_DEFAULT = 3
    const val PARALLELISM_DEFAULT = 1

    const val MEMORY_KIB_MIN = 16384
    const val MEMORY_KIB_MAX = 262144
    const val ITERATIONS_MIN = 1
    const val ITERATIONS_MAX = 10
    const val PARALLELISM_MIN = 1
    const val PARALLELISM_MAX = 4
    const val SALT_LEN = 16
    const val NONCE_LEN = 12
    const val TAG_LEN = 16
    const val KEY_LEN = 32
    const val CIPHERTEXT_MAX = 64 * 1024 * 1024

    class DepmapContainerException(val code: String, message: String) : Exception(message)

    /** Argon2id v19 派生 32 字节 fileEncryptionKey。password 为精确 UTF-8 字节。 */
    fun deriveFileEncryptionKey(
        passwordBytes: ByteArray,
        salt: ByteArray,
        memoryKiB: Int,
        iterations: Int,
        parallelism: Int
    ): ByteArray {
        val builder = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withVersion(Argon2Parameters.ARGON2_VERSION_13)
            .withSalt(salt)
            .withMemoryAsKB(memoryKiB)
            .withIterations(iterations)
            .withParallelism(parallelism)
        val generator = Argon2BytesGenerator()
        generator.init(builder.build())
        val out = ByteArray(KEY_LEN)
        generator.generateBytes(passwordBytes, out)
        return out
    }

    /**
     * 加密并返回容器 JSON（JCS canonical 顺序）。
     * header 字段顺序按 JCS：cipher < ciphertext < format < formatVersion < kdf < tag。
     */
    fun encrypt(
        plaintext: ByteArray,
        password: String,
        salt: ByteArray = ByteArray(SALT_LEN).also { SecureRandom().nextBytes(it) },
        nonce: ByteArray = ByteArray(NONCE_LEN).also { SecureRandom().nextBytes(it) },
        memoryKiB: Int = MEMORY_KIB_DEFAULT,
        iterations: Int = ITERATIONS_DEFAULT,
        parallelism: Int = PARALLELISM_DEFAULT
    ): String {
        val key = deriveFileEncryptionKey(password.toByteArray(Charsets.UTF_8), salt, memoryKiB, iterations, parallelism)
        val saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP or Base64.NO_PADDING)
        val nonceB64 = Base64.encodeToString(nonce, Base64.NO_WRAP or Base64.NO_PADDING)        val aad = buildAad(saltB64, nonceB64, memoryKiB, iterations, parallelism).toByteArray(Charsets.UTF_8)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
        cipher.updateAAD(aad)
        val ctAndTag = cipher.doFinal(plaintext)
        val ciphertext = ctAndTag.copyOfRange(0, ctAndTag.size - TAG_LEN)
        val tag = ctAndTag.copyOfRange(ctAndTag.size - TAG_LEN, ctAndTag.size)

        // JSON 按 JCS 键序输出（与 RFC 8785 一致）
        return buildString {
            append("{\"cipher\":{\"algorithm\":\"AES-256-GCM\",\"nonce\":\"").append(nonceB64)
            append("\"},\"ciphertext\":\"").append(Base64.encodeToString(ciphertext, Base64.NO_WRAP or Base64.NO_PADDING))
            append("\",\"format\":\"").append(FORMAT)
            append("\",\"formatVersion\":").append(FORMAT_VERSION)
            append(",\"kdf\":{\"algorithm\":\"argon2id\",\"iterations\":").append(iterations)
            append(",\"memoryKiB\":").append(memoryKiB)
            append(",\"parallelism\":").append(parallelism)
            append(",\"salt\":\"").append(saltB64)
            append("\",\"version\":").append(ARGON2_VERSION_19)
            append("},\"tag\":\"").append(Base64.encodeToString(tag, Base64.NO_WRAP or Base64.NO_PADDING))
            append("\"}")
        }
    }

    /** 解密；口令错误 / 篡改 → auth_failed；边界违规 → bounds（先于 KDF）。 */
    fun decrypt(containerJson: String, password: String): ByteArray {
        val header = JsonHeader.parse(containerJson)
        validateBounds(header)
        val key = deriveFileEncryptionKey(
            password.toByteArray(Charsets.UTF_8),
            Base64.decode(header.salt, Base64.DEFAULT),
            header.memoryKiB, header.iterations, header.parallelism
        )
        val aad = buildAad(header.salt, header.nonce, header.memoryKiB, header.iterations, header.parallelism)
            .toByteArray(Charsets.UTF_8)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(key, "AES"),
            GCMParameterSpec(128, Base64.decode(header.nonce, Base64.DEFAULT))
        )
        cipher.updateAAD(aad)
        val ct = Base64.decode(header.ciphertext, Base64.DEFAULT)
        val tag = Base64.decode(header.tag, Base64.DEFAULT)
        return try {
            cipher.doFinal(ct + tag)
        } catch (e: Exception) {
            throw DepmapContainerException("auth_failed", "decryption authentication failed")
        }
    }

    private fun validateBounds(h: JsonHeader) {
        fun fail(m: String): Nothing = throw DepmapContainerException("bounds", m)
        if (h.format != FORMAT) fail("format must be depmap")
        if (h.formatVersion != FORMAT_VERSION) fail("formatVersion must be 1")
        if (h.algorithm != "argon2id") fail("kdf.algorithm must be argon2id")
        if (h.argonVersion != ARGON2_VERSION_19) fail("kdf.version must be 19")
        if (h.memoryKiB !in MEMORY_KIB_MIN..MEMORY_KIB_MAX) fail("kdf.memoryKiB out of bounds")
        if (h.iterations !in ITERATIONS_MIN..ITERATIONS_MAX) fail("kdf.iterations out of bounds")
        if (h.parallelism !in PARALLELISM_MIN..PARALLELISM_MAX) fail("kdf.parallelism out of bounds")
        if (Base64.decode(h.salt, Base64.DEFAULT).size != SALT_LEN) fail("salt must be 16 bytes")
        if (Base64.decode(h.nonce, Base64.DEFAULT).size != NONCE_LEN) fail("nonce must be 12 bytes")
        if (Base64.decode(h.tag, Base64.DEFAULT).size != TAG_LEN) fail("tag must be 16 bytes")
        val ctLen = Base64.decode(h.ciphertext, Base64.DEFAULT).size
        if (ctLen <= 0) fail("ciphertext must not be empty")
        if (ctLen > CIPHERTEXT_MAX) fail("ciphertext exceeds 64 MiB limit")
    }

    /** AAD = JCS({format, formatVersion, kdf, cipher})（整数最短形式、键序 JCS）。 */
    private fun buildAad(saltB64: String, nonceB64: String, memoryKiB: Int, iterations: Int, parallelism: Int): String =
        "{\"cipher\":{\"algorithm\":\"AES-256-GCM\",\"nonce\":\"$nonceB64\"}," +
            "\"format\":\"$FORMAT\",\"formatVersion\":$FORMAT_VERSION," +
            "\"kdf\":{\"algorithm\":\"argon2id\",\"iterations\":$iterations," +
            "\"memoryKiB\":$memoryKiB,\"parallelism\":$parallelism,\"salt\":\"$saltB64\",\"version\":$ARGON2_VERSION_19}}"

    /** 极简 header 解析（V1 固定结构；不引入第三方 JSON 依赖）。 */
    internal data class JsonHeader(
        val format: String, val formatVersion: Int,
        val algorithm: String, val argonVersion: Int,
        val salt: String, val memoryKiB: Int, val iterations: Int, val parallelism: Int,
        val nonce: String, val ciphertext: String, val tag: String
    ) {
        companion object {
            fun parse(json: String): JsonHeader {
                fun num(key: String): Int {
                    val m = Regex("\"$key\":(-?\\d+)").find(json) ?: error("missing $key")
                    return m.groupValues[1].toInt()
                }
                fun str(key: String): String {
                    val m = Regex("\"$key\":\"((?:[^\"\\\\]|\\\\.)*)\"").find(json) ?: error("missing $key")
                    return m.groupValues[1]
                }
                return JsonHeader(
                    format = str("format"),
                    formatVersion = num("formatVersion"),
                    algorithm = str("algorithm"),
                    argonVersion = num("version"),
                    salt = str("salt"),
                    memoryKiB = num("memoryKiB"),
                    iterations = num("iterations"),
                    parallelism = num("parallelism"),
                    nonce = str("nonce"),
                    ciphertext = str("ciphertext"),
                    tag = str("tag")
                )
            }
        }
    }
}
