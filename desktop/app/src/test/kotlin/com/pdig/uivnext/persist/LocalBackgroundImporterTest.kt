package com.pdig.uivnext.persist

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * LocalBackgroundImporterTest（PHASE 1E §62–63 / AC18）：
 * - 合法 PNG 导入 → app-managed storage（sha256 命名）→ 返回目标文件；
 * - SVG / 伪扩展名 / 损坏文件 / 超大文件 / 超尺寸 → 拒绝并给出原因；
 * - 绝不写 .depmap（canonical 目录零新文件）。
 */
class LocalBackgroundImporterTest {

    @TempDir
    lateinit var tempDir: File

    private fun importer(): LocalBackgroundImporter = LocalBackgroundImporter(File(tempDir, "app-data/backgrounds"))

    private fun makePng(name: String, w: Int = 64, h: Int = 64): File {
        val img = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
        val f = File(tempDir, name)
        ImageIO.write(img, "png", f)
        return f
    }

    @Test
    fun `valid png imports into app managed storage with sha256 name`() {
        val src = makePng("hero.png")
        val out = importer().import(src)

        assertNotNull(out, "valid png must import")
        assertTrue(out.isFile)
        assertTrue(out.name.matches(Regex("[0-9a-f]{64}\\.png")))
        assertTrue(out.parentFile.name == "backgrounds")
    }

    @Test
    fun `svg and script containers are rejected`() {
        val svg = File(tempDir, "card.svg").apply { writeText("<svg onload='alert(1)'></svg>") }
        val imp = importer()
        assertNull(imp.import(svg))
        assertNotNull(imp.lastError)
        assertTrue(imp.lastError!!.contains("SVG"), "error must name the rejected container: ${imp.lastError}")
    }

    @Test
    fun `renamed fake extension and corrupted file are rejected by decode validation`() {
        // 伪扩展名：内容不是图片（decode 必须失败）
        val fake = File(tempDir, "fake.png").apply { writeText("not an image at all") }
        val imp = importer()
        assertNull(imp.import(fake))
        assertNotNull(imp.lastError)
        assertTrue(imp.lastError!!.contains("无法解码"), "error must mention decode: ${imp.lastError}")
    }

    @Test
    fun `oversize file is rejected`() {
        val big = makePng("big.png")
        // 用小上限强制触发
        val strict = LocalBackgroundImporter(File(tempDir, "app-data/backgrounds"), maxFileBytes = 10)
        assertNull(strict.import(big))
        assertTrue(strict.lastError!!.contains("大小上限"))
    }

    @Test
    fun `oversize dimension is rejected`() {
        val huge = makePng("huge.png", w = 5000, h = 5000)
        val strict = LocalBackgroundImporter(File(tempDir, "app-data/backgrounds"), maxDimension = 1000)
        assertNull(strict.import(huge))
        assertTrue(strict.lastError!!.contains("尺寸"))
    }

    @Test
    fun `canonical depmap directory gains zero new files after import`() {
        val canonicalDir = File(tempDir, "app-data")
        canonicalDir.mkdirs()
        val canonical = File(canonicalDir, "reality.depmap")
        canonical.writeBytes(ByteArray(64) { it.toByte() })
        val before = canonicalDir.listFiles { f -> f.extension == "depmap" }!!.size

        importer().import(makePng("ok.png"))

        val after = canonicalDir.listFiles { f -> f.extension == "depmap" }!!.size
        assertEquals(before, after, "background import must never create or touch .depmap files")
        assertEquals(64, canonical.length(), "canonical .depmap payload must be byte-identical (size unchanged)")
    }

    @Test
    fun `missing source file is rejected`() {
        val imp = importer()
        assertNull(imp.import(File(tempDir, "missing.png")))
        assertNotNull(imp.lastError)
    }
}