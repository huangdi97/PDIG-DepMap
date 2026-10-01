package com.pdig.uivnext.persist

import java.io.File
import java.io.IOException
import javax.imageio.ImageIO

/**
 * LocalBackgroundImporter —— Card / Number 自定义背景导入（PHASE 1E §62–63）。
 *
 * 安全约束（§63）：
 *  - 仅 image 白名单（png/jpg/jpeg/webp/bmp；SVG 明确拒绝）；
 *  - max file size（默认 8MB）与 max dimension（默认 4096px）；
 *  - decode validation（ImageIO 必须能解码，失败即拒绝）；
 *  - 拒绝远程 URL / embedded network fetch（只接受本地文件路径，绝不下载）；
 *  - 输出复制到 app-managed storage（默认 `~/.pdig/backgrounds/<sha256>.<ext>`），
 *    绝不写入 PersonalReality / .depmap canonical payload。
 *
 * 全部确定性、可测试（临时目录注入）。
 */
class LocalBackgroundImporter(
    private val appManagedDir: File,
    private val maxFileBytes: Long = 8L * 1024 * 1024,
    private val maxDimension: Int = 4096,
) {
    init {
        appManagedDir.mkdirs()
    }

    /** 支持格式（禁止 SVG 等可执行/脚本容器）。 */
    private val ALLOWED_EXT = setOf("png", "jpg", "jpeg", "webp", "bmp")

    /**
     * 导入本地图片：校验 → 复制到 app-managed storage → 返回目标文件。
     * 任何失败返回 null（不抛异常；UI 显示明确拒绝原因，见 lastError）。
     */
    fun import(source: File): File? {
        lastError = null
        if (!source.isFile || !source.exists()) {
            lastError = "文件不存在"
            return null
        }
        val ext = source.extension.lowercase()
        if (ext !in ALLOWED_EXT) {
            lastError = "仅支持 ${ALLOWED_EXT.joinToString(" / ")}；不支持 $ext（SVG 等脚本容器已拒绝）"
            return null
        }
        if (source.length() > maxFileBytes) {
            lastError = "文件超过大小上限 ${maxFileBytes / 1024 / 1024}MB"
            return null
        }
        // decode validation（真实解码，非只读扩展名）
        val decoded = try {
            ImageIO.read(source)
        } catch (e: IOException) {
            null
        } ?: run {
            lastError = "图片无法解码（损坏或格式不支持）"
            return null
        }
        if (decoded.width > maxDimension || decoded.height > maxDimension) {
            lastError = "图片尺寸超过 ${maxDimension}px 上限"
            return null
        }

        // 复制到 app-managed storage（确定性命名：sha256 + 扩展名）
        val digest = sha256(source)
        val target = File(appManagedDir, "$digest.$ext")
        if (!target.exists()) {
            source.copyTo(target, overwrite = false)
        }
        return target
    }

    var lastError: String? = null
        private set

    private fun sha256(file: File): String =
        file.inputStream().use { ins ->
            val md = java.security.MessageDigest.getInstance("SHA-256")
            val buf = ByteArray(65536)
            while (true) {
                val n = ins.read(buf)
                if (n < 0) break
                md.update(buf, 0, n)
            }
            md.digest().joinToString("") { "%02x".format(it) }
        }
}

/** 默认 app-managed 背景目录（真实运行时；测试注入临时目录）。 */
fun defaultBackgroundDir(): File {
    val home = System.getenv("USERPROFILE") ?: System.getProperty("user.home") ?: "."
    return File(home, ".pdig/backgrounds")
}