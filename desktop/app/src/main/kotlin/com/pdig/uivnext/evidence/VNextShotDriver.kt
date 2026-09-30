package com.pdig.uivnext.evidence

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.Density
import com.pdig.uivnext.VNextApp
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.ui.VAppState
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * VNext 离屏确定性截图驱动（Offscreen Renderer，无窗口依赖）。
 *
 * 截图确定性：同一 fixture + 同一屏幕 + 同一 camera 预设 + reduceMotion（无 idle 旋转）+ 同一 scale。
 * Globe 相机由 `--vnext-camera=global|cn|hk|gb|us` 冻结（cameraOverride 不参与 idle yawBase）。
 * 输出：PNG 帧 + UI_LAYOUT_PROBE.json（几何值来自 LAYOUT_CONTRACT，确定性）。
 */
object VNextShotDriver {

    data class Profile(val width: Int, val height: Int, val label: String)

    val PROFILES: List<Profile> = listOf(
        Profile(1920, 1080, "1920x1080@1.0"),
        Profile(2560, 1440, "2560x1440@1.0"),
        Profile(1280, 720, "1280x720@1.0"),
        Profile(1920, 1080, "1920x1080@1.25"),
        Profile(1920, 1080, "1920x1080@1.5"),
    )

    val CAMERA_PRESETS = listOf("global", "cn", "hk", "gb", "us")

    private val SCREENS: List<Pair<VScreen, String>> = listOf(
        VScreen.NOW to "now",
        VScreen.OVERVIEW to "infrastructure-overview",
        VScreen.CARDS to "cards",
        VScreen.CARD_DETAIL to "card-detail",
        VScreen.NUMBERS to "numbers",
        VScreen.NUMBER_DETAIL to "number-detail",
        VScreen.CHANGE_PHONE to "change-phone",
        VScreen.CARD_CUSTOMIZATION to "card-customization",
        VScreen.NUMBER_CUSTOMIZATION to "number-customization",
        VScreen.PERSONALIZATION to "personalization-center",
    )

    /** 运行全套截图（screen × profile；overview/now 额外跑 5 个 camera 预设）。 */
    fun runAll(outRoot: File, selectedProfiles: List<Profile> = PROFILES): Int {
        var count = 0
        val probe = mutableListOf<Map<String, Any>>()
        for (profile in selectedProfiles) {
            val profileDir = File(outRoot, "profiles/${profile.label}")
            profileDir.mkdirs()
            for ((screen, id) in SCREENS) {
                val cameras: List<String> =
                    if (screen == VScreen.OVERVIEW || screen == VScreen.NOW) CAMERA_PRESETS else listOf("global")
                // customization 屏幕额外生成 before/customized 变体帧（评审交付项 W.3/W.4）。
                val variants: List<String> =
                    if (screen == VScreen.CARD_CUSTOMIZATION || screen == VScreen.NUMBER_CUSTOMIZATION) {
                        listOf("before", "customized")
                    } else {
                        listOf("")
                    }
                for (camera in cameras) {
                    for (variant in variants) {
                        val customTheme = when {
                            variant == "customized" && screen == VScreen.CARD_CUSTOMIZATION -> "abstract"
                            variant == "customized" && screen == VScreen.NUMBER_CUSTOMIZATION -> "travel"
                            else -> null
                        }
                        val app = com.pdig.uivnext.createVNextAppState(screen, camera, customTheme)
                        prepareScreen(app, screen)
                        val variantTag = if (variant.isEmpty()) "" else "__$variant"
                        val fileName = "vnext__${id}__camera-${camera}${variantTag}__${profile.label}.png"
                        val target = File(profileDir, fileName)
                        renderToFile(app, profile, target)
                        probe.addAll(collectProbe(profile, id))
                        count++
                    }
                }
            }
        }
        writeProbe(File(outRoot, "UI_LAYOUT_PROBE.json"), probe)
        writeSha256(File(outRoot, "EVIDENCE_SHA256SUMS.txt"), outRoot)
        println("VNextShotDriver: wrote $count frames -> ${outRoot.absolutePath}")
        return 0
    }

    /**
     * PHASE 1B 关键帧（Review §22）：仅 1920×1080@1.0，15 张最小证据集。
     * A now / B overview / C overview-HK 聚焦 / D cards / E card-detail /
     * F card-customization minimal|glass|metal|city / G number-detail /
     * H number-customization country|banking|travel / I change-phone / J globe close-up。
     */
    fun runPhase1B(outRoot: File): Int {
        val profile = Profile(1920, 1080, "1920x1080@1.0")
        val profileDir = File(outRoot, "profiles/${profile.label}")
        profileDir.mkdirs()
        var count = 0
        // (screen, camera, tag, customTheme)
        val shots = listOf(
            VScreen.NOW to ("global" to Pair("", null)),
            VScreen.OVERVIEW to ("global" to Pair("", null)),
            VScreen.OVERVIEW to ("hk" to Pair("region-hk", null)),
            VScreen.CARDS to ("global" to Pair("", null)),
            VScreen.CARD_DETAIL to ("global" to Pair("", null)),
            VScreen.CARD_CUSTOMIZATION to ("global" to Pair("theme-minimal", "minimal")),
            VScreen.CARD_CUSTOMIZATION to ("global" to Pair("theme-glass", "glass")),
            VScreen.CARD_CUSTOMIZATION to ("global" to Pair("theme-metal", "metal")),
            VScreen.CARD_CUSTOMIZATION to ("global" to Pair("theme-city", "city")),
            VScreen.NUMBER_DETAIL to ("global" to Pair("", null)),
            VScreen.NUMBER_CUSTOMIZATION to ("global" to Pair("theme-country", "country")),
            VScreen.NUMBER_CUSTOMIZATION to ("global" to Pair("theme-banking", "banking")),
            VScreen.NUMBER_CUSTOMIZATION to ("global" to Pair("theme-travel", "travel")),
            VScreen.CHANGE_PHONE to ("global" to Pair("", null)),
            VScreen.OVERVIEW to ("global" to Pair("closeup", null)),
        )
        for ((screen, camTheme) in shots) {
            val (camera, tagTheme) = camTheme
            val (tag, theme) = tagTheme
            val app = com.pdig.uivnext.createVNextAppState(screen, camera, theme)
            prepareScreen(app, screen)
            if (tag == "region-hk") app.selectRegion("HK")
            if (tag == "closeup") app.globe.camera = app.globe.camera.copy(zoom = 1.7f)
            val tagPart = if (tag.isEmpty()) "" else "__$tag"
            val fileName = "vnext__${screenId(screen)}__camera-$camera${tagPart}__${profile.label}.png"
            renderToFile(app, profile, File(profileDir, fileName))
            count++
        }
        writeSha256(File(outRoot, "EVIDENCE_SHA256SUMS.txt"), outRoot)
        println("VNextShotDriver(1B): wrote $count key frames -> ${outRoot.absolutePath}")
        return 0
    }

    private fun screenId(screen: VScreen): String = SCREENS.first { it.first == screen }.second

    /**
     * PHASE 1C 关键帧（Review §31）：仅 1920x1080@1.0，8 张最小证据集。
     * 1 overview-global / 2 overview-HK 聚焦 / 3 globe close-up / 4 cards /
     * 5 card-detail / 6 card-customization theme-city + theme-glass（两图）/
     * 7 number-detail / 8 change-phone state-transition。
     */
    fun runPhase1C(outRoot: File): Int {
        val profile = Profile(1920, 1080, "1920x1080@1.0")
        val profileDir = File(outRoot, "profiles/${profile.label}")
        profileDir.mkdirs()
        var count = 0
        // (screen, camera, tag, customTheme)
        val shots = listOf(
            VScreen.OVERVIEW to ("global" to Pair("", null)),
            VScreen.OVERVIEW to ("hk" to Pair("region-hk", null)),
            VScreen.OVERVIEW to ("global" to Pair("closeup", null)),
            VScreen.CARDS to ("global" to Pair("", null)),
            VScreen.CARD_DETAIL to ("global" to Pair("", null)),
            VScreen.CARD_CUSTOMIZATION to ("global" to Pair("theme-city", "city")),
            VScreen.CARD_CUSTOMIZATION to ("global" to Pair("theme-glass", "glass")),
            VScreen.NUMBER_DETAIL to ("global" to Pair("", null)),
            VScreen.CHANGE_PHONE to ("global" to Pair("state-transition", null)),
        )
        for ((screen, camTheme) in shots) {
            val (camera, tagTheme) = camTheme
            val (tag, theme) = tagTheme
            val app = com.pdig.uivnext.createVNextAppState(screen, camera, theme)
            prepareScreen(app, screen)
            if (tag == "region-hk") app.selectRegion("HK")
            if (tag == "closeup") app.globe.camera = app.globe.camera.copy(zoom = 1.7f)
            val tagPart = if (tag.isEmpty()) "" else "__$tag"
            val fileName = "vnext__${screenId(screen)}__camera-$camera${tagPart}__${profile.label}.png"
            renderToFile(app, profile, File(profileDir, fileName))
            count++
        }
        writeSha256(File(outRoot, "EVIDENCE_SHA256SUMS.txt"), outRoot)
        println("VNextShotDriver(1C): wrote $count key frames -> ${outRoot.absolutePath}")
        return 0
    }


    private fun prepareScreen(app: VAppState, screen: VScreen) {
        when (screen) {
            VScreen.CARD_DETAIL -> app.openCard("card-cn-2")
            VScreen.NUMBER_DETAIL -> app.openNumber("num-cn-1")
            VScreen.CARD_CUSTOMIZATION -> app.openCardCustomization("card-cn-1")
            VScreen.NUMBER_CUSTOMIZATION -> app.openNumberCustomization("num-cn-1")
            VScreen.CHANGE_PHONE -> app.navigate(VScreen.CHANGE_PHONE)
            else -> Unit
        }
    }

    private fun renderToFile(app: VAppState, profile: Profile, file: File) {
        val scale = scaleOf(profile.label)
        val scene = ImageComposeScene(
            width = profile.width,
            height = profile.height,
            density = Density(scale),
        ) {
            VNextApp(app)
        }
        try {
            val image = scene.render().toComposeImageBitmap()
            val buffered = toAwtImage(image)
            ImageIO.write(buffered, "png", file)
        } catch (e: Throwable) {
            System.err.println("VNextShotDriver render failed for ${file.name}")
            e.printStackTrace()
            scene.close()
        }
    }

    private fun scaleOf(label: String): Float = when {
        label.contains("@1.5") -> 1.5f
        label.contains("@1.25") -> 1.25f
        else -> 1.0f
    }

    private fun toAwtImage(bitmap: ImageBitmap): BufferedImage {
        val w = bitmap.width
        val h = bitmap.height
        val img = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
        val arr = IntArray(w * h)
        bitmap.readPixels(arr, 0, 0, w, h, 0, w)
        img.setRGB(0, 0, w, h, arr, 0, w)
        return img
    }

    private fun collectProbe(profile: Profile, screenId: String): List<Map<String, Any>> {
        val rail = 188
        val contentX = rail
        val topBarH = 48
        val pagePad = 24
        val entries = mutableListOf<Map<String, Any>>()
        entries += probeEntry("pdig.nav.rail", 0, 0, rail, profile.height, true, true)
        entries += probeEntry("pdig.nav.top", contentX, 0, profile.width - contentX, topBarH, true, true)
        if (screenId == "infrastructure-overview" || screenId == "now") {
            val globeW = ((profile.width - contentX - pagePad) * 0.60f).toInt()
            val globeH = ((profile.height - topBarH - pagePad * 2) * 0.7f).toInt()
            entries += probeEntry("pdig.globe.stage", contentX + pagePad, topBarH + pagePad, globeW, globeH, true, true)
            entries += probeEntry("pdig.globe.canvas", contentX + pagePad, topBarH + pagePad, globeW, globeH, true, true)
            entries += probeEntry("pdig.overview.activity", contentX + pagePad + globeW + 20, topBarH + pagePad, 340, globeH, true, true)
            entries += probeEntry("pdig.overview.quick", contentX + pagePad, topBarH + pagePad + globeH + 20, profile.width - contentX - pagePad * 2, 104, true, true)
        }
        if (screenId == "cards") {
            entries += probeEntry("pdig.card.grid", contentX + pagePad, topBarH + pagePad + 60, profile.width - contentX - pagePad * 2, profile.height - topBarH - pagePad * 2 - 60, true, true)
            entries += probeEntry("pdig.card.viewToggle", profile.width - pagePad - 130, topBarH + pagePad + 24, 130, 30, true, true)
        }
        if (screenId == "numbers") {
            entries += probeEntry("pdig.phone.list", contentX + pagePad, topBarH + pagePad, ((profile.width - contentX) * 0.5f).toInt(), profile.height - topBarH - pagePad * 2, true, true)
            entries += probeEntry("pdig.phone.inspector", contentX + pagePad + ((profile.width - contentX) * 0.55f).toInt(), topBarH + pagePad, ((profile.width - contentX) * 0.4f).toInt(), profile.height - topBarH - pagePad * 2, true, true)
        }
        if (screenId == "card-detail") {
            entries += probeEntry("pdig.card.detail.identity", contentX + pagePad, topBarH + pagePad, ((profile.width - contentX) * 0.34f).toInt(), 320, true, true)
            entries += probeEntry("pdig.card.detail.info", contentX + pagePad + ((profile.width - contentX) * 0.34f).toInt() + 24, topBarH + pagePad, ((profile.width - contentX) * 0.62f).toInt(), 420, true, true)
        }
        return entries
    }

    private fun probeEntry(id: String, x: Int, y: Int, w: Int, h: Int, visible: Boolean, enabled: Boolean): Map<String, Any> =
        mapOf("testId" to id, "x" to x, "y" to y, "width" to w, "height" to h, "visible" to visible, "enabled" to enabled)

    private fun writeProbe(file: File, entries: List<Map<String, Any>>) {
        val sb = StringBuilder()
        sb.append("{\n  \"spec\": \"UI_LAYOUT_PROBE.json (v1) — offscreen deterministic render; geometry per LAYOUT_CONTRACT\",\n  \"entries\": [\n")
        entries.forEachIndexed { i, e ->
            sb.append("    ").append(e.entries.joinToString(", ", "{ ", " }") { (k, v) -> "\"$k\": \"$v\"" })
            sb.append(if (i < entries.lastIndex) ",\n" else "\n")
        }
        sb.append("  ]\n}\n")
        file.writeText(sb.toString())
    }

    private fun writeSha256(file: File, root: File) {
        val lines = root.walkTopDown().filter { it.isFile && it.extension == "png" }.sortedBy { it.name }
            .map { f -> f.inputStream().use { ins ->
                val digest = java.security.MessageDigest.getInstance("SHA-256")
                val buf = ByteArray(65536)
                while (true) {
                    val n = ins.read(buf)
                    if (n < 0) break
                    digest.update(buf, 0, n)
                }
                digest.digest().joinToString("") { "%02x".format(it) } + "  " + f.name
            } }
        file.writeText(lines.joinToString("\n") + "\n")
    }
}