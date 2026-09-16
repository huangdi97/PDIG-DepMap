package com.pdig.app.evidence

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContextWrapper
import android.net.Uri
import android.provider.MediaStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pdig.app.data.AppContainer
import com.pdig.app.data.ExportBackupResult
import com.pdig.app.data.ExportFailureStage
import com.pdig.app.platform.AndroidSqliteDriver
import com.pdig.app.security.DatabaseKeyStore
import com.pdig.app.ui.screens.BACKUP_FAIL_PREFIX
import com.pdig.app.ui.screens.backupUiState
import com.pdig.core.crypto.DepmapContainer
import com.pdig.core.schema.migrate
import com.pdig.core.serialize.checkGraphIntegrity
import com.pdig.core.serialize.importGraph
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.Instant

/**
 * F1 回归测试 —— 「备份导出 UI 误报失败」。
 *
 * 2026-09-16 实测到的缺陷（2/2 稳定复现）：
 * App 显示「备份失败：无法写入文件。」，但 `/sdcard/Download/pdig-backup.depmap`
 * 其实已经完整落盘、用正确口令可以成功恢复。原因是 `exportBackupToFile()` 返回 `null`
 * 而 UI 把所有 `null` 都当成失败，UI 与真实文件状态脱节。
 *
 * 本测试把"不得出现 file exists + valid 但 UI failure"这条规则变成断言：
 *  - 导出成功 ⇒ 返回 `Success`，UI 状态 success，且文件**真的可解密、真的可恢复**
 *  - 导出失败 ⇒ 返回 `Failure`，UI 状态 failure，且不产生残留文件
 */
@RunWith(AndroidJUnit4::class)
class BackupExportRegressionTest {

    private fun ctx() = InstrumentationRegistry.getInstrumentation().targetContext
    private val now = "2026-09-16T00:00:00.000Z"

    private fun appDbFile() = File(ctx().filesDir, "pdig.db")

    private fun openAppDb() = AndroidSqliteDriver.open(appDbFile(), DatabaseKeyStore.passphrase(ctx()))

    /** 保证库里至少有一条真实依赖，恢复断言才有意义。 */
    private fun seedOneNodeAndEdge() {
        val d = openAppDb()
        try {
            d.prepare(
                """
                INSERT OR IGNORE INTO nodes (id, kind, name, owner, created_at, updated_at)
                VALUES ('bk-node-1', 'payment_card', 'Backup Regression Card', 'self', '$now', '$now')
                """.trimIndent(),
            ).run()
        } finally {
            d.close()
        }
    }

    private fun findDownloadByName(name: String): Uri? {
        val resolver = ctx().contentResolver
        resolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Downloads._ID),
            "${MediaStore.Downloads.DISPLAY_NAME} = ?",
            arrayOf(name),
            null,
        )?.use { c ->
            if (c.moveToFirst()) {
                return ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, c.getLong(0))
            }
        }
        return null
    }

    private fun deleteDownload(name: String) {
        findDownloadByName(name)?.let { ctx().contentResolver.delete(it, null, null) }
    }

    // ------------------------------------------------------------------
    // 1. 成功路径：文件有效 ⇒ UI 必须是成功
    // ------------------------------------------------------------------

    @Test
    fun exportSuccess_producesAValidContainerAndTheUiReportsSuccess() {
        seedOneNodeAndEdge()
        val container = AppContainer.get(ctx())
        val password = "pdig-regression-pass-2026"
        val fileName = "pdig-regression-${System.currentTimeMillis()}.depmap"
        deleteDownload(fileName)

        val result = container.exportBackupToFile(ctx(), password, fileName)

        // (a) 结果被正确分类为成功
        assertTrue("导出必须返回 Success，实际：$result", result is ExportBackupResult.Success)
        val success = result as ExportBackupResult.Success

        // (b) UI 状态必须是成功 —— 这一条就是 F1 的回归点
        val ui = backupUiState(result)
        assertTrue("文件已落盘且可用时，UI 不允许报失败（F1）", ui.success)
        assertFalse("成功文案不得以失败前缀开头", ui.headline.startsWith(BACKUP_FAIL_PREFIX))
        assertTrue("成功文案必须给出真实文件名", ui.detail.contains(success.displayName))

        // (c) 文件真的存在，字节数一致
        val uri = findDownloadByName(success.displayName)
        assertNotNull("导出成功后文件必须能在「下载」目录里找到", uri)
        val actual = ctx().contentResolver.openInputStream(uri!!)!!.use { it.readBytes() }
        assertEquals("UI 报告的字节数必须与磁盘一致", success.byteCount, actual.size)

        // (d) 内容真的可用：正确口令能解出与当前库一致的 payload
        val plaintext = DepmapContainer.openContainer(String(actual, Charsets.UTF_8), password)
        val expected = openAppDb().let { d ->
            try {
                com.pdig.core.serialize.exportGraph(d).payloadJson.toByteArray(Charsets.UTF_8)
            } finally {
                d.close()
            }
        }
        assertArrayEquals("解出的 payload 必须与当前库一致", expected, plaintext)

        // (e) 真的能恢复到一个独立库（恢复是破坏性的，不在回归里对主库执行）
        val tmp = File(ctx().cacheDir, "pdig-restore-${System.nanoTime()}.db")
        val restoreDriver = AndroidSqliteDriver.open(tmp, "restore-regression-pass")
        try {
            migrate(restoreDriver, Instant.now().toString())
            val imported = importGraph(restoreDriver, String(plaintext, Charsets.UTF_8))
            checkGraphIntegrity(restoreDriver)
            assertTrue("恢复必须写入内容", imported.values.sum() > 0)
            assertNotNull(
                "恢复后必须能查到被种下的对象",
                restoreDriver.prepare("SELECT id FROM nodes WHERE id = 'bk-node-1'").get(),
            )
        } finally {
            restoreDriver.close()
            tmp.delete()
        }

        // (f) 错误口令必须被拒（同一个文件上再确认一次）
        var rejected = false
        try {
            DepmapContainer.openContainer(String(actual, Charsets.UTF_8), "definitely-wrong-password")
        } catch (_: Throwable) {
            rejected = true
        }
        assertTrue("错误口令必须被拒绝", rejected)

        deleteDownload(success.displayName)
    }

    // ------------------------------------------------------------------
    // 2. 失败路径：被分类为 Failure，且 UI 必须是失败，不留残留
    // ------------------------------------------------------------------

    @Test
    fun exportFailure_isClassifiedAndNeverReportsSuccess() {
        val container = AppContainer.get(ctx())
        // 注入一个取 resolver 就抛异常的 Context：导出必须在建行之前就失败，
        // 不允许把异常逃逸成未分类的崩溃，也不允许留下任何文件。
        val broken = object : ContextWrapper(ctx()) {
            override fun getContentResolver(): ContentResolver =
                throw IllegalStateException("injected_content_resolver_failure")
        }

        val result = container.exportBackupToFile(broken, "pdig-regression-pass-2026", "injected.depmap")

        assertTrue("取 resolver 失败必须被分类成 Failure，实际：$result", result is ExportBackupResult.Failure)
        val failure = result as ExportBackupResult.Failure
        assertEquals(ExportFailureStage.INSERT, failure.stage)
        assertNull("失败发生在建行之前，不应存在残留文件", failure.cleanupOk)

        val ui = backupUiState(result)
        assertFalse("失败必须映射成失败 UI 状态", ui.success)
        assertTrue("失败文案必须可解释（指出阶段）", ui.detail.contains(failure.stage.name))
    }

    @Test
    fun everyFailureStageMapsToAnExplainableFailureUiState() {
        ExportFailureStage.values().forEach { stage ->
            val failure = ExportBackupResult.Failure(
                stage = stage,
                errorType = "java.io.IOException",
                detail = "injected",
                cleanupOk = false,
            )
            val ui = backupUiState(failure)
            assertFalse("$stage 必须映射成失败", ui.success)
            assertTrue("$stage 的文案必须带失败前缀", ui.headline.startsWith(BACKUP_FAIL_PREFIX))
            assertTrue("$stage 的文案必须指出阶段", ui.detail.contains(stage.name))
            assertTrue(
                "$stage 清理未生效时必须明确告知用户，不能假装干净",
                ui.detail.contains("残留"),
            )
        }
    }
}
