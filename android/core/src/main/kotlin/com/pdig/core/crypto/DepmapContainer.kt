package com.pdig.core.crypto

import com.pdig.core.generated.DepmapContainerV1 as Spec
import com.pdig.core.json.Json
import com.pdig.core.json.JsonException
import com.pdig.core.json.JsonParser
import com.pdig.core.json.JsonWriter
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * DEPMAP_CONTAINER_V1 —— 口令派生文件加密（Kotlin / JVM+Android）。
 *
 * 协议**冻结**（spec/security/depmap-container-v1.json）。本类只做协议 glue：
 *  - Argon2id 使用 BouncyCastle（成熟、可审计）
 *  - AES-256-GCM 使用 JDK JCE
 *  - CSPRNG 使用 SecureRandom
 *  **禁止手写密码学原语**（spec/security §3）。
 *
 * 常量全部来自 codegen 的 Spec 对象，避免三端各自硬编码导致漂移。
 *
 * 解密顺序固定：parse → structure → bounds → Argon2id → GCM auth。
 * 认证前 header 一律不可信；bounds 必须在 KDF 之前完成（恶意容器不得触发超大内存 KDF）。
 */
object DepmapContainer {

    class DepmapException(val code: String, message: String) : RuntimeException(message)

    data class Header(
        val format: String,
        val formatVersion: Long,
        val kdfAlgorithm: String,
        val kdfVersion: Long,
        val saltB64: String,
        val memoryKiB: Long,
        val iterations: Long,
        val parallelism: Long,
        val cipherAlgorithm: String,
        val nonceB64: String,
        val ciphertextB64: String,
        val tagB64: String,
    )

    data class ContainerResult(
        val json: String,
        val header: Header,
        val derivedKeyHex: String,
    )

    // --- base64 (RFC 4648 standard WITH padding) -----------------------------

    private val B64: Base64.Encoder = Base64.getEncoder()
    private val B64D: Base64.Decoder = Base64.getDecoder()

    fun toBase64(bytes: ByteArray): String = B64.encodeToString(bytes)
    fun fromBase64(s: String): ByteArray =
        try {
            B64D.decode(s)
        } catch (e: IllegalArgumentException) {
            throw DepmapException("invalid_structure", "field is not valid base64")
        }

    fun toHex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it) }

    // --- AAD ---------------------------------------------------------------

    fun computeAad(h: Header): ByteArray {
        val aad = Json.Obj(
            listOf(
                "format" to Json.Str(h.format),
                "formatVersion" to Json.Num(h.formatVersion.toString()),
                "kdf" to Json.Obj(
                    listOf(
                        "algorithm" to Json.Str(h.kdfAlgorithm),
                        "version" to Json.Num(h.kdfVersion.toString()),
                        "salt" to Json.Str(h.saltB64),
                        "memoryKiB" to Json.Num(h.memoryKiB.toString()),
                        "iterations" to Json.Num(h.iterations.toString()),
                        "parallelism" to Json.Num(h.parallelism.toString()),
                    ),
                ),
                "cipher" to Json.Obj(
                    listOf(
                        "algorithm" to Json.Str(h.cipherAlgorithm),
                        "nonce" to Json.Str(h.nonceB64),
                    ),
                ),
            ),
        )
        return Jcs.stringify(aad).toByteArray(Charsets.UTF_8)
    }

    // --- 解析 / 结构 / 边界 --------------------------------------------------

    fun parseHeader(json: String): Header {
        val raw: Json = try {
            JsonParser.parse(json)
        } catch (e: JsonException) {
            throw DepmapException("invalid_json", "container is not valid JSON")
        }
        if (raw !is Json.Obj) {
            throw DepmapException("invalid_structure", "container must be a JSON object")
        }
        val kdf = raw["kdf"]
        val cipher = raw["cipher"]
        if (kdf !is Json.Obj || cipher !is Json.Obj) {
            throw DepmapException("invalid_structure", "kdf/cipher must be objects")
        }
        return Header(
            format = str(raw, "format"),
            formatVersion = int(raw, "formatVersion"),
            kdfAlgorithm = str(kdf, "algorithm"),
            kdfVersion = int(kdf, "version"),
            saltB64 = str(kdf, "salt"),
            memoryKiB = int(kdf, "memoryKiB"),
            iterations = int(kdf, "iterations"),
            parallelism = int(kdf, "parallelism"),
            cipherAlgorithm = str(cipher, "algorithm"),
            nonceB64 = str(cipher, "nonce"),
            ciphertextB64 = str(raw, "ciphertext"),
            tagB64 = str(raw, "tag"),
        )
    }

    private fun str(o: Json.Obj, key: String): String {
        val v = o[key] ?: throw DepmapException("invalid_structure", "header field $key must be a string")
        return (v as? Json.Str)?.value
            ?: throw DepmapException("invalid_structure", "header field $key must be a string")
    }

    private fun int(o: Json.Obj, key: String): Long {
        val v = o[key] ?: throw DepmapException("invalid_structure", "header field $key must be an integer")
        val n = v as? Json.Num
            ?: throw DepmapException("invalid_structure", "header field $key must be an integer")
        if (!n.isSafeInteger()) {
            throw DepmapException("invalid_structure", "header field $key must be an integer")
        }
        return n.asLong()
    }

    fun validateBounds(h: Header) {
        fun fail(m: String): Nothing = throw DepmapException("bounds", m)
        if (h.format != Spec.FORMAT) fail("format must be \"${Spec.FORMAT}\"")
        if (h.formatVersion != Spec.FORMAT_VERSION.toLong()) fail("formatVersion must be 1")
        if (h.kdfAlgorithm != Spec.KDF_ALGORITHM) fail("kdf.algorithm must be argon2id")
        if (h.kdfVersion != Spec.KDF_VERSION.toLong()) fail("kdf.version must be 19")
        if (h.memoryKiB < Spec.MEMORY_KIB_MIN || h.memoryKiB > Spec.MEMORY_KIB_MAX) {
            fail("kdf.memoryKiB out of bounds [${Spec.MEMORY_KIB_MIN},${Spec.MEMORY_KIB_MAX}]")
        }
        if (h.iterations < Spec.ITERATIONS_MIN || h.iterations > Spec.ITERATIONS_MAX) {
            fail("kdf.iterations out of bounds [${Spec.ITERATIONS_MIN},${Spec.ITERATIONS_MAX}]")
        }
        if (h.parallelism < Spec.PARALLELISM_MIN || h.parallelism > Spec.PARALLELISM_MAX) {
            fail("kdf.parallelism out of bounds [${Spec.PARALLELISM_MIN},${Spec.PARALLELISM_MAX}]")
        }
        if (fromBase64(h.saltB64).size != Spec.SALT_BYTES) fail("kdf.salt must decode to 16 bytes")
        if (fromBase64(h.nonceB64).size != Spec.NONCE_BYTES) fail("cipher.nonce must decode to 12 bytes")
        if (fromBase64(h.tagB64).size != Spec.TAG_BYTES) fail("tag must decode to 16 bytes")
        val ct = fromBase64(h.ciphertextB64)
        if (ct.isEmpty()) fail("ciphertext must not be empty")
        if (ct.size > Spec.CIPHERTEXT_MAX_BYTES) fail("ciphertext exceeds 64 MiB limit")
        if (h.cipherAlgorithm != Spec.CIPHER_ALGORITHM) fail("cipher.algorithm must be AES-256-GCM")
    }

    // --- Argon2id -----------------------------------------------------------

    /** password 使用精确 UTF-8 字节，**不做** Unicode 归一化（spec §38 / DEP-03）。 */
    fun deriveFileEncryptionKey(
        password: String,
        salt: ByteArray,
        memoryKiB: Int,
        iterations: Int,
        parallelism: Int,
    ): ByteArray {
        val params = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withVersion(Spec.KDF_VERSION)
            .withSalt(salt)
            .withMemoryAsKB(memoryKiB)
            .withIterations(iterations)
            .withParallelism(parallelism)
            .build()
        val gen = Argon2BytesGenerator()
        gen.init(params)
        val out = ByteArray(Spec.KEY_BYTES)
        gen.generateBytes(password.toByteArray(Charsets.UTF_8), out)
        return out
    }

    // --- 创建 / 打开 ---------------------------------------------------------

    data class CreateOptions(
        val salt: ByteArray? = null,
        val nonce: ByteArray? = null,
        val memoryKiB: Int = Spec.MEMORY_KIB,
        val iterations: Int = Spec.ITERATIONS,
        val parallelism: Int = Spec.PARALLELISM,
    )

    fun create(plaintext: ByteArray, password: String, opts: CreateOptions = CreateOptions()): ContainerResult {
        val salt = opts.salt ?: randomBytes(Spec.SALT_BYTES)
        val nonce = opts.nonce ?: randomBytes(Spec.NONCE_BYTES)
        require(salt.size == Spec.SALT_BYTES) { "salt must be 16 bytes" }
        require(nonce.size == Spec.NONCE_BYTES) { "nonce must be 12 bytes" }

        val key = deriveFileEncryptionKey(password, salt, opts.memoryKiB, opts.iterations, opts.parallelism)

        val base = Header(
            format = Spec.FORMAT,
            formatVersion = Spec.FORMAT_VERSION.toLong(),
            kdfAlgorithm = Spec.KDF_ALGORITHM,
            kdfVersion = Spec.KDF_VERSION.toLong(),
            saltB64 = toBase64(salt),
            memoryKiB = opts.memoryKiB.toLong(),
            iterations = opts.iterations.toLong(),
            parallelism = opts.parallelism.toLong(),
            cipherAlgorithm = Spec.CIPHER_ALGORITHM,
            nonceB64 = toBase64(nonce),
            ciphertextB64 = "",
            tagB64 = "",
        )

        val aad = computeAad(base)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(Spec.TAG_BYTES * 8, nonce))
        cipher.updateAAD(aad)
        val sealed = cipher.doFinal(plaintext)
        val ct = sealed.copyOfRange(0, sealed.size - Spec.TAG_BYTES)
        val tag = sealed.copyOfRange(sealed.size - Spec.TAG_BYTES, sealed.size)

        val full = base.copy(ciphertextB64 = toBase64(ct), tagB64 = toBase64(tag))
        // 容器本身也用 JCS 序列化 → deterministic 输出
        return ContainerResult(json = Jcs.stringify(toJson(full)), header = full, derivedKeyHex = toHex(key))
    }

    fun openContainer(json: String, password: String): ByteArray {
        val header = parseHeader(json)
        validateBounds(header) // KDF 之前完成全部边界检查
        val key = deriveFileEncryptionKey(
            password,
            fromBase64(header.saltB64),
            header.memoryKiB.toInt(),
            header.iterations.toInt(),
            header.parallelism.toInt(),
        )
        val aad = computeAad(header)
        return try {
            val c = Cipher.getInstance("AES/GCM/NoPadding")
            c.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(key, "AES"),
                GCMParameterSpec(Spec.TAG_BYTES * 8, fromBase64(header.nonceB64)),
            )
            c.updateAAD(aad)
            val sealed = fromBase64(header.ciphertextB64) + fromBase64(header.tagB64)
            c.doFinal(sealed)
        } catch (e: Exception) {
            throw DepmapException(
                "auth_failed",
                "decryption authentication failed (wrong password or tampered container)",
            )
        }
    }

    private fun toJson(h: Header): Json.Obj = Json.Obj(
        listOf(
            "format" to Json.Str(h.format),
            "formatVersion" to Json.Num(h.formatVersion.toString()),
            "kdf" to Json.Obj(
                listOf(
                    "algorithm" to Json.Str(h.kdfAlgorithm),
                    "version" to Json.Num(h.kdfVersion.toString()),
                    "salt" to Json.Str(h.saltB64),
                    "memoryKiB" to Json.Num(h.memoryKiB.toString()),
                    "iterations" to Json.Num(h.iterations.toString()),
                    "parallelism" to Json.Num(h.parallelism.toString()),
                ),
            ),
            "cipher" to Json.Obj(
                listOf(
                    "algorithm" to Json.Str(h.cipherAlgorithm),
                    "nonce" to Json.Str(h.nonceB64),
                ),
            ),
            "ciphertext" to Json.Str(h.ciphertextB64),
            "tag" to Json.Str(h.tagB64),
        ),
    )

    fun headerToPrettyJson(h: Header): String = JsonWriter.write(toJson(h))

    private val secureRandom = SecureRandom()
    fun randomBytes(n: Int): ByteArray = ByteArray(n).also { secureRandom.nextBytes(it) }
}
