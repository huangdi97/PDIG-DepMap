package com.pdig.uivnext.persist

import com.pdig.uivnext.model.PresentationProfile
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * PresentationProfilePersistenceTest（PHASE 1E §66 / AC2）：
 *
 * - edit → save → 关闭 store（释放引用）→ 以同一文件新建 store（进程级重开等价）
 *   → profile 保留（themeId / material / accent / background / layout 全字段一致）；
 * - canonical reality 不变量：store 目录内绝不产生 .depmap 文件，且预先放置的
 *   canonical .depmap 内容零字节变化；
 * - delete 后不可再读到（幂等删除）。
 */
class PresentationProfilePersistenceTest {

    @TempDir
    lateinit var tempDir: File

    private fun newStore(): PresentationProfileStore = PresentationProfileStore(File(tempDir, "app-data/presentation-profiles.json"))

    @Test
    fun `profile survives store close and reopen with all fields intact`() {
        val edited = PresentationProfile(
            targetType = "card",
            targetId = "card-cn-2",
            themeId = "abstract",
            material = "metal",
            accentColor = "crimson",
            backgroundKind = "preset",
            backgroundValue = "abstract",
            layout = "emblem",
            maskSensitive = false,
        )
        // Arrange: first "session" edits and saves.
        val session1 = newStore()
        session1.save(edited)
        // Simulate close: drop the reference entirely.
        // Act: second "session" reopens the same on-disk file.
        val session2 = newStore()
        val loaded = session2.load("card", "card-cn-2")

        // Assert: persisted profile fully restored.
        assertNotNull(loaded)
        assertEquals("abstract", loaded.themeId)
        assertEquals("metal", loaded.material)
        assertEquals("crimson", loaded.accentColor)
        assertEquals("emblem", loaded.layout)
        assertEquals("preset", loaded.backgroundKind)
        assertEquals("abstract", loaded.backgroundValue)
        assertEquals(false, loaded.maskSensitive)
        assertEquals("card:card-cn-2", loaded.targetType + ":" + loaded.targetId)
    }

    @Test
    fun `canonical depmap payload remains byte-identical after profile save`() {
        // Arrange: pre-existing canonical .depmap in the same app directory (must never be touched).
        val canonicalDir = File(tempDir, "app-data")
        canonicalDir.mkdirs()
        val canonical = File(canonicalDir, "reality.depmap")
        val canonicalBytes = ByteArray(256) { (it * 31).toByte() }
        Files.write(canonical.toPath(), canonicalBytes)

        val store = newStore()
        store.save(PresentationProfile.defaultFor("card", "card-cn-1", "glass"))

        // Assert: canonical payload zero change.
        val after = Files.readAllBytes(canonical.toPath())
        assertEquals(canonicalBytes.toList(), after.toList(), "canonical .depmap must be untouched")

        // Assert: no other .depmap files were created by the store.
        val depmapFiles = canonicalDir.listFiles { f -> f.extension == "depmap" } ?: arrayOf()
        assertEquals(1, depmapFiles.size, "only the pre-existing canonical .depmap may exist")
        assertEquals("reality.depmap", depmapFiles[0].name)
    }

    @Test
    fun `missing profile returns null and delete is idempotent`() {
        val store = newStore()
        assertNull(store.load("card", "not-exist"))

        store.save(PresentationProfile.defaultFor("card", "card-cn-3", "minimal"))
        assertNotNull(store.load("card", "card-cn-3"))

        store.delete("card", "card-cn-3")
        assertNull(store.load("card", "card-cn-3"))
        // Idempotent delete must not throw.
        store.delete("card", "card-cn-3")
    }

    @Test
    fun `multiple profiles coexist keyed by targetType and targetId`() {
        val store = newStore()
        store.save(PresentationProfile.defaultFor("card", "card-cn-1", "glass"))
        store.save(PresentationProfile.defaultFor("card", "card-cn-2", "metal"))
        store.save(PresentationProfile.defaultFor("phoneNumber", "num-hk-1", "travel"))

        assertEquals(3, store.loadAll().size)
        assertEquals("glass", store.load("card", "card-cn-1")?.themeId)
        assertEquals("travel", store.load("phoneNumber", "num-hk-1")?.themeId)
    }
}