package com.pdig.app.data

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import com.pdig.core.crypto.DepmapContainer
import com.pdig.core.db.SqliteDriver
import com.pdig.core.serialize.checkGraphIntegrity
import com.pdig.core.serialize.exportGraph
import com.pdig.core.serialize.importGraph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Backup / Restore（.depmap）：导出到 MediaStore Downloads + 容器级恢复。 */
class BackupRepository(private val driver: SqliteDriver) {

    /**
     * 把 .depmap 容器写成用户可见的 Downloads 文件（MediaStore，scoped storage）。
     *
     * 之前的备份页只在内存里生成字符串、进 Toast，既不落地也无法被恢复选择器选中，
     * 导致"导出 → 清数据 → 还原"这条路径从来没被真正走过。
     * 这里补上真实落盘；API 29+ 走 MediaStore **不需要**任何存储权限。
     *
     * ---------------------------------------------------------------------------
     * 2026-09-16 修复（真实缺陷 F1）：本函数原先在任何阶段出错都返回 `null`，
     * 而 UI 把所有 `null` 一律显示为「备份失败：无法写入文件。」。实测 2/2 复现：
     * 文件其实已经完整落盘、用正确口令可以恢复，UI 却报失败。
     *
     * 三类问题一并修掉：
     *  1. **阶段不可区分** —— 现在返回 [ExportBackupResult]，失败带明确的
     *     [ExportFailureStage] 与异常类型，UI 才能给出可解释的错误。
     *  2. **写完不校验** —— `write()` 返回不代表内容完整。现在写完**回读校验字节数**，
     *     不一致按失败处理，不会把"半截文件"当成成功。
     *  3. **清理不可信** —— 失败时的 `delete` 结果没有被检查。现在记录
     *     [ExportBackupResult.Failure.cleanupOk]，清理没生效时明确告知用户。
     *
     * 特别注意：**不允许出现「文件存在且可用，但 UI 报失败」**。
     * 这条由设备内回归测试 `BackupExportRegressionTest` 断言。
     * ---------------------------------------------------------------------------
     */
    fun exportBackupToFile(context: Context, password: String, fileName: String): ExportBackupResult {
        // 阶段 0：生成容器。不做任何 IO，失败与"写文件"无关，必须分开报告。
        val payload: String = try {
            DepmapContainer.create(exportGraph(driver).payloadJson.toByteArray(Charsets.UTF_8), password).json
        } catch (t: Throwable) {
            return fail(ExportFailureStage.ENCRYPT, t, cleanupOk = null)
        }
        val bytes = payload.toByteArray(Charsets.UTF_8)
        // 取 resolver 本身也可能抛（例如 Context 已被销毁）。它属于"建行之前"，
        // 归到 INSERT 阶段并且没有残留可清理 —— 但绝不允许它逃逸成未分类的崩溃。
        val resolver = try {
            context.contentResolver
        } catch (t: Throwable) {
            return fail(ExportFailureStage.INSERT, t, cleanupOk = null)
        }

        // 阶段 1：在 MediaStore 建行（IS_PENDING=1，此时其它应用看不到它）。
        val uri = try {
            resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, pendingValues(fileName))
                ?: return fail(ExportFailureStage.INSERT, null, cleanupOk = null)
        } catch (t: Throwable) {
            return fail(ExportFailureStage.INSERT, t, cleanupOk = null)
        }

        // 阶段 2：写入 + flush。use{} 负责 close。
        try {
            val out = resolver.openOutputStream(uri)
                ?: throw java.io.IOException("openOutputStream returned null")
            out.use {
                it.write(bytes)
                it.flush()
            }
        } catch (t: Throwable) {
            return fail(ExportFailureStage.WRITE, t, cleanupOk = deleteQuietly(resolver, uri))
        }

        // 阶段 3：回读校验。字节数不一致 = 写入不完整，绝不当作成功。
        try {
            val actual = resolver.openInputStream(uri)?.use { it.readBytes() }
            if (actual == null || actual.size != bytes.size) {
                throw java.io.IOException(
                    "verify_size_mismatch expected=${bytes.size} actual=${actual?.size ?: -1}",
                )
            }
        } catch (t: Throwable) {
            return fail(ExportFailureStage.VERIFY, t, cleanupOk = deleteQuietly(resolver, uri))
        }

        // 阶段 4：发布（IS_PENDING=0）。受影响行数为 0 说明没真正发布，同样按失败处理。
        try {
            val updated = resolver.update(uri, publishedValues(), null, null)
            if (updated <= 0) throw java.io.IOException("publish_affected_rows=$updated")
        } catch (t: Throwable) {
            return fail(ExportFailureStage.PUBLISH, t, cleanupOk = deleteQuietly(resolver, uri))
        }

        // MediaStore 在重名时会自动改名，回读真实文件名，UI 才能报出用户真能找到的名字。
        val displayName = queryDisplayName(resolver, uri) ?: fileName
        return ExportBackupResult.Success(displayName = displayName, byteCount = bytes.size)
    }

    private fun pendingValues(fileName: String) = ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
        put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
        put(MediaStore.Downloads.IS_PENDING, 1)
    }

    private fun publishedValues() = ContentValues().apply {
        put(MediaStore.Downloads.IS_PENDING, 0)
    }

    private fun deleteQuietly(resolver: android.content.ContentResolver, uri: android.net.Uri): Boolean =
        try {
            resolver.delete(uri, null, null) > 0
        } catch (_: Throwable) {
            false
        }

    private fun queryDisplayName(
        resolver: android.content.ContentResolver,
        uri: android.net.Uri,
    ): String? = try {
        resolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
    } catch (_: Throwable) {
        null
    }

    /**
     * 统一失败出口。只记录**阶段 + 异常类型 + 异常 message**：
     * 这些都是 MediaStore / IO 层的信息，不含口令、明文图或账单内容（AGENTS §17）。
     */
    private fun fail(
        stage: ExportFailureStage,
        t: Throwable?,
        cleanupOk: Boolean?,
    ): ExportBackupResult.Failure {
        val type = t?.javaClass?.name ?: "null_result"
        val detail = t?.message ?: type
        android.util.Log.w(EXPORT_TAG, "backup export failed at $stage: $type: $detail")
        return ExportBackupResult.Failure(stage = stage, errorType = type, detail = detail, cleanupOk = cleanupOk)
    }

    suspend fun exportBackup(password: String): String = withContext(Dispatchers.Default) {
        val payload = exportGraph(driver).payloadJson
        DepmapContainer.create(payload.toByteArray(Charsets.UTF_8), password).json
    }

    suspend fun restoreBackup(containerJson: String, password: String): Int =
        withContext(Dispatchers.Default) {
            val plaintext = DepmapContainer.openContainer(containerJson, password)
            val payload = plaintext.toString(Charsets.UTF_8)
            val imported = importGraph(driver, payload)
            checkGraphIntegrity(driver) // 断言无孤儿；结果供 UI 显示
            imported.values.sum()
        }
}

internal const val EXPORT_TAG = "PDIG_BACKUP"