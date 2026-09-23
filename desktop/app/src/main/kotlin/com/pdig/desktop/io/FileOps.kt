package com.pdig.desktop.io

import java.awt.FileDialog
import java.awt.Frame
import java.io.File

/**
 * 平台文件对话框适配（E5：File workflow 通过 platform adapter）。
 * 真实实现走 AWT 原生对话框（Windows 原生标题栏）；null = 用户取消。
 * 测试用 [FakeDesktopFileOps] 提供确定性路径。
 */
interface DesktopFileOps {
    /** 打开选择；取消返回 null。 */
    fun pickOpen(title: String, filterName: String? = null, vararg extensions: String): File?

    /** 保存路径；取消返回 null。 */
    fun pickSave(title: String, defaultName: String, vararg extensions: String): File?

    /** 读取整个文件；IO 错误转 Runtime。 */
    fun readBytes(file: File): ByteArray = file.readBytes()

    /** 写文件（覆盖）；IO 错误转 Runtime。 */
    fun write(file: File, content: ByteArray) {
        file.parentFile?.mkdirs()
        file.writeBytes(content)
    }
}

/** Windows 真机实现：AWT FileDialog（平台原生）。 */
class AwtDesktopFileOps(private val parent: Frame?) : DesktopFileOps {
    override fun pickOpen(title: String, filterName: String?, vararg extensions: String): File? {
        val d = FileDialog(parent, title, FileDialog.LOAD)
        if (extensions.isNotEmpty() && filterName != null) {
            d.setFilenameFilter { _, name -> extensions.any { name.endsWith(".$it", ignoreCase = true) } }
        }
        d.isVisible = true
        val dir = d.directory ?: return null
        val file = d.file ?: return null
        return File(dir, file)
    }

    override fun pickSave(title: String, defaultName: String, vararg extensions: String): File? {
        val d = FileDialog(parent, title, FileDialog.SAVE)
        d.file = defaultName
        d.isVisible = true
        val dir = d.directory ?: return null
        val file = d.file ?: return null
        var target = File(dir, file)
        val hasExt = extensions.any { target.name.endsWith(".$it", ignoreCase = true) }
        if (!hasExt && extensions.isNotEmpty()) target = File(dir, "$file.${extensions[0]}")
        return target
    }
}

/** 测试/无窗口环境：脚本式返回预置路径，模拟用户选择或取消。 */
class FakeDesktopFileOps(
    private val openResult: File? = null,
    private val saveResult: File? = null,
) : DesktopFileOps {
    override fun pickOpen(title: String, filterName: String?, vararg extensions: String): File? = openResult
    override fun pickSave(title: String, defaultName: String, vararg extensions: String): File? = saveResult
}