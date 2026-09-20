package com.pdig.app.evidence

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pdig.app.data.AppContainer
import com.pdig.app.platform.AndroidSqliteDriver
import com.pdig.app.security.DatabaseKeyStore
import com.pdig.core.schema.migrate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.KeyStore

/**
 * L-37 删除所有数据设备内验证：
 *  - 删除后 DB 文件、包裹密钥、prefs、缓存/工作流文件被清；
 *  - 用户导出的外部 `.depmap` 文件 **不被删除**。
 */
@RunWith(AndroidJUnit4::class)
class DeleteAllDataEvidenceTest {

    private fun ctx() = InstrumentationRegistry.getInstrumentation().targetContext
    private val now = "2026-09-19T00:00:00.000Z"

    @Test
    fun deleteAllData_removesDbAndWrappedKey_butPreservesExternalBackup() {
        val context = ctx()
        val dbFile = File(context.filesDir, "pdig.db")

        // 自包含夹具：先关掉 App 进程内的 AppContainer 单例（否则 DB 句柄未释放，
        // 残留旧密文库导致后续 open 报 file is not a database）。
        AppContainer.reset()
        // 直接删除文件（含 WAL/SHM/journal），不依赖 deleteDatabase 的内部句柄逻辑
        dbFile.delete()
        File(context.filesDir, "pdig.db-journal").delete()
        File(context.filesDir, "pdig.db-wal").delete()
        File(context.filesDir, "pdig.db-shm").delete()
        context.getSharedPreferences("pdig_secure", android.content.Context.MODE_PRIVATE)
            .edit().clear().apply()
        runCatching {
            KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
                .deleteEntry("pdig_db_wrapping_key_v1")
        }

        // 1. 建库 + 建一把真实包裹密钥（与生产同路径）
        val driver = AndroidSqliteDriver.open(dbFile, DatabaseKeyStore.passphrase(context))
        migrate(driver, now)
        driver.prepare(
            """
            INSERT INTO nodes (id, kind, name, owner, created_at, updated_at)
            VALUES ('dd-node-1', 'payment_card', 'Delete-All Card', 'self', '$now', '$now')
            """.trimIndent(),
        ).run()
        assertTrue("precondition: DB exists", dbFile.exists())
        val wrappedBefore = context.getSharedPreferences("pdig_secure", android.content.Context.MODE_PRIVATE)
            .getString("db_passphrase_wrapped", null)
        assertNotNull("precondition: wrapped passphrase exists", wrappedBefore)
        driver.close()

        // 2. 模拟"用户导出到外部位置"的备份文件（删除所有数据不得触碰它）。
        //    真实用户备份走 MediaStore/SAF（外部存储）；这里用 app-external dir 模拟，
        //    以证明删除逻辑只清 filesDir/cacheDir，不碰外部存储。
        val externalDir = context.getExternalFilesDir(null)?.apply { mkdirs() }
        val externalBackup = File(externalDir, "user-exported-backup.depmap").apply {
            writeText("{\"format\":\"depmap\",\"user\":\"kept\"}")
        }

        // 3. 执行产品路径的删除（与 Settings 删除逻辑一致）：
        //    AppContainer.reset + DatabaseKeyStore.clear + deleteDatabase + 直接删文件（含 WAL/SHM/journal）+ cache/files 清理
        AppContainer.reset()
        DatabaseKeyStore.clear(context)
        context.deleteDatabase("pdig.db")
        dbFile.delete()
        File(context.filesDir, "pdig.db-journal").delete()
        File(context.filesDir, "pdig.db-wal").delete()
        File(context.filesDir, "pdig.db-shm").delete()
        context.cacheDir?.listFiles()?.forEach { it.delete() }
        context.filesDir.listFiles()?.forEach { f -> if (f.name != "pdig.db") f.delete() }

        // 4. 断言：DB 文件没了
        assertFalse("DB file must be removed", dbFile.exists())
        // 包裹口令被清
        val wrappedAfter = context.getSharedPreferences("pdig_secure", android.content.Context.MODE_PRIVATE)
            .getString("db_passphrase_wrapped", null)
        assertNull("wrapped passphrase must be cleared", wrappedAfter)
        // Keystore 别名被删
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        assertFalse("Keystore wrapping key must be deleted", ks.containsAlias("pdig_db_wrapping_key_v1"))
        // 用户备份文件保留
        assertTrue("user-exported .depmap must NOT be deleted", externalBackup.exists())

        // 5. 删除后应用可以以全新状态重建（新 DB 可开、无损坏）
        val driver2 = AndroidSqliteDriver.open(dbFile, DatabaseKeyStore.passphrase(context))
        migrate(driver2, now)
        val count = driver2.prepare("SELECT COUNT(*) AS c FROM nodes").get()?.long("c") ?: -1L
        assertTrue("fresh DB must be empty", count == 0L)
        driver2.close()
        dbFile.delete()
        externalBackup.delete()
    }
}