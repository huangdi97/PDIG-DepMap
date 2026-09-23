package com.pdig.desktop

import com.pdig.desktop.data.DesktopSession
import com.pdig.desktop.persist.DepmapFileStore
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * .depmap 持久化契约（E6/E8）：round-trip 逐字节、错误口令/篡改/未来 schema 拒绝、备份复制。
 */
class DepmapFileStoreTest {

    @TempDir
    lateinit var dir: File

    private val store = DepmapFileStore()
    private val password = "test-password-01"

    private fun freshPayload(): String {
        val s = DesktopSession.open()
        val payload = s.exportPayload()
        s.close()
        return payload
    }

    @Test
    fun saveThenOpenRoundTripsByteExact() {
        val file = File(dir, "a.depmap")
        val payload = freshPayload()
        store.save(file, payload, password)
        val reopened = store.open(file, password)
        assertEquals(payload, reopened, "payload must survive encrypt/decrypt byte-exactly")
        // 容器文件不是明文 JSON 的 payload
        val raw = file.readText()
        assertTrue(!raw.contains("payloadKind"), "container must not contain plaintext payload")
    }

    @Test
    fun wrongPasswordIsRejectedAsAuthFailed() {
        val file = File(dir, "b.depmap")
        store.save(file, freshPayload(), password)
        val e = assertFailsWith<DepmapFileStore.StoreException> { store.open(file, "wrong-password") }
        assertEquals("auth_failed", e.code)
    }

    @Test
    fun tamperedCiphertextFailsAuthentication() {
        val file = File(dir, "c.depmap")
        store.save(file, freshPayload(), password)
        val text = file.readText()
        val marker = "\"ciphertext\":\""
        val idx = text.indexOf(marker)
        assertTrue(idx > 0)
        val flipAt = idx + marker.length + 2
        val flipped = if (text[flipAt] == 'A') 'B' else 'A'
        file.writeText(text.substring(0, flipAt) + flipped + text.substring(flipAt + 1))
        val e = assertFailsWith<DepmapFileStore.StoreException> { store.open(file, password) }
        assertEquals("auth_failed", e.code)
    }

    @Test
    fun futureSchemaIsRejectedWithoutGuessing() {
        val file = File(dir, "d.depmap")
        store.save(file, freshPayload(), password)
        val payload = store.open(file, password)
        val future = payload.replace("\"schemaVersion\":3", "\"schemaVersion\":99")
        assertTrue(future != payload)
        val futureFile = File(dir, "future.depmap")
        store.save(futureFile, future, password)
        val e = assertFailsWith<DepmapFileStore.StoreException> { store.open(futureFile, password) }
        assertEquals("future_schema", e.code)
    }

    @Test
    fun backupCopyNeverDecrypts() {
        val file = File(dir, "e.depmap")
        store.save(file, freshPayload(), password)
        val backup = File(dir, "e-backup.depmap")
        store.backupCopy(file, backup)
        assertEquals(file.readText(), backup.readText(), "backup is a byte copy of the encrypted container")
    }

    @Test
    fun missingFileAndTooLargeAreClassified() {
        val missing = File(dir, "missing.depmap")
        val e1 = assertFailsWith<DepmapFileStore.StoreException> { store.open(missing, password) }
        assertEquals("not_found", e1.code)
        val small = File(dir, "small.depmap")
        store.save(small, freshPayload(), password)
        val tiny = DepmapFileStore(maxFileBytes = 10)
        val e2 = assertFailsWith<DepmapFileStore.StoreException> { tiny.open(small, password) }
        assertEquals("too_large", e2.code)
    }
}