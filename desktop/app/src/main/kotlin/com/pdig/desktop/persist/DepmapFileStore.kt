package com.pdig.desktop.persist

import com.pdig.core.crypto.DepmapContainer
import com.pdig.core.serialize.GraphImportError
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files

/**
 * .depmap 文件读写的唯一通道：DEPMAP_CONTAINER_V1（Argon2id v19 / AES-256-GCM /
 * RFC 8785 JCS AAD，全部来自冻结的 :core 协议）。**禁止**任何明文落盘；
 * 本类不接受也未暴露任何解密后字节的缓存路径。
 *
 * 失败分类（供 UI 给出可解释错误）：
 *  - WRONG_PASSWORD / TAMPERED  → DepmapException(WRONG_PASSWORD|auth_failed)
 *  - FUTURE_SCHEMA             → GraphImportError("newer than supported")
 *  - TOO_LARGE                 → 本层先于解密检查的文件大小守卫
 *  - CANCELLED                 → 调用方（FileOps 返回 null）
 */
class DepmapFileStore(
    /** 单文件最大字节（容器协议 ciphertext ≤ 64 MiB，预留 JSON 外壳与 base64 膨胀）。 */
    private val maxFileBytes: Long = 120L * 1024 * 1024,
) {

    class StoreException(val code: String, message: String, cause: Throwable? = null) : RuntimeException(message, cause)

    companion object {
        fun classifyError(e: Throwable): String = when (e) {
            is StoreException -> e.code
            is DepmapContainer.DepmapException -> e.code // invalid_json / invalid_structure / bounds / auth_failed
            is GraphImportError -> "graph_import_failed"
            else -> "io_failed"
        }

        /**
         * 未来 schema 拒绝（spec §42 / SchemaVersion：永不猜测兼容）。
         * payload 校验在 :core importGraph 内完成；这里的预检让错误信息更直接。
         */
        fun isFutureSchema(payloadJson: String): Boolean {
            val marker = "\"schemaVersion\""
            val idx = payloadJson.indexOf(marker)
            if (idx < 0) return false
            val tail = payloadJson.substring(idx + marker.length).trim()
            val num = tail.removePrefix(":").trim().takeWhile { it.isDigit() }
            val version = num.toIntOrNull() ?: return false
            return version > com.pdig.core.schema.SchemaVersion.current
        }
    }

    /** 保存：payload → 容器 JSON → 原子写入（先写临时文件再 rename）。 */
    fun save(file: File, payloadJson: String, password: String) {
        val container = DepmapContainer.create(
            payloadJson.toByteArray(StandardCharsets.UTF_8),
            password,
        )
        val tmp = File(file.parentFile, ".${file.name}.tmp")
        try {
            tmp.writeText(container.json, StandardCharsets.UTF_8)
            if (!tmp.renameTo(file)) {
                // rename 跨卷可能失败 → 退化为复制删除
                Files.copy(tmp.toPath(), file.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING)
                tmp.delete()
            }
        } catch (e: IOException) {
            tmp.delete()
            throw StoreException("io_failed", "write failed: ${file.name}", e)
        }
    }

    /** 打开：读文件 → 容器解密 → payload 预检（future schema）→ 原样返回 payload 由调用方导入。 */
    fun open(file: File, password: String): String {
        if (!file.isFile) throw StoreException("not_found", "file not found: ${file.name}")
        if (file.length() > maxFileBytes) {
            throw StoreException("too_large", "file exceeds ${maxFileBytes / 1024 / 1024} MiB limit")
        }
        val containerJson = try {
            file.readText(StandardCharsets.UTF_8)
        } catch (e: IOException) {
            throw StoreException("io_failed", "read failed: ${file.name}", e)
        }
        val payloadJsonBytes = try {
            DepmapContainer.openContainer(containerJson, password)
        } catch (e: DepmapContainer.DepmapException) {
            throw StoreException(e.code, e.message ?: "container open failed", e)
        }
        val payload = String(payloadJsonBytes, StandardCharsets.UTF_8)
        if (isFutureSchema(payload)) {
            throw StoreException(
                "future_schema",
                "payload schemaVersion is newer than supported (${com.pdig.core.schema.SchemaVersion.current}); refusing to guess compatibility",
            )
        }
        return payload
    }

    /** 备份 = 把**已加密**容器文件复制到目标位置（不产生任何明文中间件）。 */
    fun backupCopy(source: File, targetFile: File, overwrite: Boolean = false) {
        if (targetFile.exists() && !overwrite) {
            throw StoreException("target_exists", "target already exists: ${targetFile.name}")
        }
        try {
            Files.copy(source.toPath(), targetFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING)
        } catch (e: IOException) {
            throw StoreException("io_failed", "backup copy failed", e)
        }
    }
}