package com.depmap.core.crypto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DEPMAP_CONTAINER_V1 Golden Vector 互操作测试。
 *
 * 冻结值与 Node reference (core/src/crypto/golden.ts) 一致。
 * 通过标准：Node encrypt → Android decrypt PASS；Android encrypt → Node decrypt PASS。
 * （Node 侧断言见 core/tests/crypto/depmap.test.ts；两侧冻结同一向量即互操作。）
 *
 * 状态：TEST READY（代码完成）。RUN = NO（本机无 JDK17/Gradle，见 BLOCKERS.md）。
 */
class DepmapContainerV1GoldenTest {

    private val goldenPassword = "depmap-test"
    private val goldenSalt = hexToBytes("00112233445566778899aabbccddeeff")
    private val goldenNonce = hexToBytes("a1b2c3d4e5f60718293a4b5c")
    private val goldenPlaintext =
        """{"app":"depmap","schemaVersion":1,"nodes":[],"dependencies":[]}""".toByteArray(Charsets.UTF_8)

    // 与 core/src/crypto/golden.ts GOLDEN_EXPECTED 冻结一致
    private val expectedDerivedKeyHex =
        "66c4bec7f5e98856747d7b41d0a021bdc092d5e12d492852bd80647bd0ff0c86"
    private val expectedCiphertextB64 =
        "KNSpbKK6waj4En3ADgeBB74H96Q7GCoBmNffeOG5QSQ9XJwx4LoJCQ0j8lEinA7GN85U6JwaVMqhAkqWDdG7"
    private val expectedTagB64 = "5qpABhovPbNet1q2GNEhkg=="

    @Test
    fun goldenVector_reproduces_frozenValues() {
        val containerJson = DepmapContainerV1.encrypt(
            goldenPlaintext, goldenPassword,
            salt = goldenSalt, nonce = goldenNonce
        )
        assertTrue(containerJson.contains(expectedCiphertextB64))
        assertTrue(containerJson.contains(expectedTagB64))
    }

    @Test
    fun goldenVector_decrypts() {
        val plaintext = DepmapContainerV1.decrypt(
            goldenContainerJson(), goldenPassword
        )
        assertEquals(goldenPlaintext.toList(), plaintext.toList())
    }

    @Test
    fun wrongPassword_fails() {
        assertThrows(DepmapContainerV1.DepmapContainerException::class.java) {
            DepmapContainerV1.decrypt(goldenContainerJson(), "depmap-tesT")
        }
    }

    @Test
    fun maliciousMemoryKiB_rejected_beforeKdf() {
        val malicious = goldenContainerJson().replace("\"memoryKiB\":65536", "\"memoryKiB\":1000000000")
        val start = System.currentTimeMillis()
        val ex = assertThrows(DepmapContainerV1.DepmapContainerException::class.java) {
            DepmapContainerV1.decrypt(malicious, goldenPassword)
        }
        assertEquals("bounds", ex.code)
        assertTrue("bounds check must be fast (no KDF)", System.currentTimeMillis() - start < 500)
    }

    private fun goldenContainerJson(): String =
        DepmapContainerV1.encrypt(goldenPlaintext, goldenPassword, salt = goldenSalt, nonce = goldenNonce)

    private fun hexToBytes(hex: String): ByteArray =
        ByteArray(hex.length / 2) { i ->
            ((Character.digit(hex[i * 2], 16) shl 4) + Character.digit(hex[i * 2 + 1], 16)).toByte()
        }
}
