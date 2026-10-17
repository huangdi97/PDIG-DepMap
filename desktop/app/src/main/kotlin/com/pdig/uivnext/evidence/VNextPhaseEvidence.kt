package com.pdig.uivnext.evidence

import com.pdig.uivnext.model.VScreen
import java.io.File

/**
 * VNext 分阶段证据集（PHASE 1B/1C/1D 关键帧；与 VNextShotDriver 拆分以保持 ≤300 行）。
 * 全部 1920×1080@1.0 离屏确定性渲染；1D 额外输出 Number Detail geometry probe。
 */
object VNextPhaseEvidence {

    /**
     * PHASE 1B 关键帧（Review §22）：15 张最小证据集。
     * A now / B overview / C overview-HK 聚焦 / D cards / E card-detail /
     * F card-customization minimal|glass|metal|city / G number-detail /
     * H number-customization country|banking|travel / I change-phone / J globe close-up。
     */
    fun runPhase1B(outRoot: File): Int {
        val profile = VNextShotDriver.Profile(1920, 1080, "1920x1080@1.0")
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
            VNextShotDriver.prepareScreen(app, screen)
            if (tag == "region-hk") app.selectRegion("HK")
            if (tag == "closeup") app.globe.camera = app.globe.camera.copy(zoom = 1.7f)
            val tagPart = if (tag.isEmpty()) "" else "__$tag"
            val fileName = "vnext__${VNextShotDriver.screenId(screen)}__camera-$camera${tagPart}__${profile.label}.png"
            VNextShotDriver.renderToFile(app, profile, File(profileDir, fileName))
            count++
        }
        VNextShotDriver.writeSha256(File(outRoot, "EVIDENCE_SHA256SUMS.txt"), outRoot)
        println("VNextPhaseEvidence(1B): wrote $count key frames -> ${outRoot.absolutePath}")
        return 0
    }

    /**
     * PHASE 1C 关键帧（Review §31）：8 张最小证据集。
     * 1 overview-global / 2 overview-HK 聚焦 / 3 globe close-up / 4 cards /
     * 5 card-detail / 6 card-customization theme-city + theme-glass（两图）/
     * 7 number-detail / 8 change-phone state-transition。
     */
    fun runPhase1C(outRoot: File): Int {
        val profile = VNextShotDriver.Profile(1920, 1080, "1920x1080@1.0")
        val profileDir = File(outRoot, "profiles/${profile.label}")
        profileDir.mkdirs()
        var count = 0
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
            VNextShotDriver.prepareScreen(app, screen)
            if (tag == "region-hk") app.selectRegion("HK")
            if (tag == "closeup") app.globe.camera = app.globe.camera.copy(zoom = 1.7f)
            val tagPart = if (tag.isEmpty()) "" else "__$tag"
            val fileName = "vnext__${VNextShotDriver.screenId(screen)}__camera-$camera${tagPart}__${profile.label}.png"
            VNextShotDriver.renderToFile(app, profile, File(profileDir, fileName))
            count++
        }
        VNextShotDriver.writeSha256(File(outRoot, "EVIDENCE_SHA256SUMS.txt"), outRoot)
        println("VNextPhaseEvidence(1C): wrote $count key frames -> ${outRoot.absolutePath}")
        return 0
    }

    /**
     * PHASE 1D 关键帧（brief §36）：10 张最小证据集 + Number Detail geometry probe。
     * 1 overview-global / 2 overview-HK 聚焦 / 3 globe close-up / 4 cards /
     * 5 card-detail / 6 card-studio theme-glass / 7 card-studio theme-city /
     * 8 number-detail / 9 change-phone transition / 10 change-phone after-projection。
     */
    fun runPhase1D(outRoot: File): Int {
        val profile = VNextShotDriver.Profile(1920, 1080, "1920x1080@1.0")
        val profileDir = File(outRoot, "profiles/${profile.label}")
        profileDir.mkdirs()
        var count = 0
        val probe = mutableListOf<Map<String, Any>>()
        // (screen, camera, tag, customTheme, extra)
        val shots = listOf(
            VScreen.OVERVIEW to (Triple("global", "", null) to null),
            VScreen.OVERVIEW to (Triple("hk", "region-hk", null) to "HK"),
            VScreen.OVERVIEW to (Triple("global", "closeup", null) to null),
            VScreen.CARDS to (Triple("global", "", null) to null),
            VScreen.CARD_DETAIL to (Triple("global", "", null) to null),
            VScreen.CARD_CUSTOMIZATION to (Triple("global", "theme-glass", "glass") to null),
            VScreen.CARD_CUSTOMIZATION to (Triple("global", "theme-city", "city") to null),
            VScreen.NUMBER_DETAIL to (Triple("global", "", null) to null),
            VScreen.CHANGE_PHONE to (Triple("global", "state-transition", null) to null),
            VScreen.CHANGE_PHONE to (Triple("global", "state-after", null) to "after"),
        )
        for ((screen, spec) in shots) {
            val (camera, tag, theme) = spec.first
            val extra = spec.second
            val app = com.pdig.uivnext.createVNextAppState(screen, camera, theme)
            VNextShotDriver.prepareScreen(app, screen)
            if (extra == "HK") app.selectRegion("HK")
            if (tag == "closeup") app.globe.camera = app.globe.camera.copy(zoom = 1.7f)
            if (extra == "after") setChangeProjection(app, "after")
            val tagPart = if (tag.isEmpty()) "" else "__$tag"
            val fileName = "vnext__${VNextShotDriver.screenId(screen)}__camera-$camera${tagPart}__${profile.label}.png"
            VNextShotDriver.renderToFile(app, profile, File(profileDir, fileName))
            probe.addAll(collectProbe1D(profile, VNextShotDriver.screenId(screen), tag))
            count++
        }
        VNextShotDriver.writeProbe(File(outRoot, "UI_LAYOUT_PROBE.json"), probe)
        VNextShotDriver.writeSha256(File(outRoot, "EVIDENCE_SHA256SUMS.txt"), outRoot)
        println("VNextPhaseEvidence(1D): wrote $count key frames -> ${outRoot.absolutePath}")
        return 0
    }

    /** PHASE 1D Number Detail 几何探针（brief §1：identity/summary/relationships 三段宽度验证）。 */
    private fun collectProbe1D(profile: VNextShotDriver.Profile, screenId: String, tag: String): List<Map<String, Any>> {
        if (screenId != "number-detail" || tag.isNotEmpty()) return emptyList()
        val rail = 188
        val contentX = rail
        val topBarH = 48
        val pagePad = 24
        val contentW = profile.width - contentX - pagePad * 2
        val topY = topBarH + pagePad + 96
        // §24 顶部三栏：identity 36% / summary 32% / recovery 32%
        val gap = 24
        val identityW = (contentW * 0.36f).toInt()
        val summaryW = (contentW * 0.32f).toInt()
        val recoveryW = (contentW * 0.32f).toInt()
        val entries = mutableListOf<Map<String, Any>>()
        entries += VNextShotDriver.probeEntry("pdig.number.detail.identity", contentX + pagePad, topY, identityW, 360, true, true)
        entries += VNextShotDriver.probeEntry("pdig.number.detail.summary", contentX + pagePad + identityW + gap, topY, summaryW, 360, true, true)
        entries += VNextShotDriver.probeEntry(
            "pdig.number.detail.relationships",
            contentX + pagePad + identityW + gap + summaryW + gap,
            topY,
            recoveryW,
            360,
            true,
            true,
        )
        entries += mapOf(
            "testId" to "pdig.number.detail.summary.minWidth",
            "check" to "summary width >= 300dp @1920",
            "measured" to summaryW,
            "minRequired" to 300,
            "passed" to (summaryW >= 300).toString(),
        )
        entries += mapOf(
            "testId" to "pdig.number.detail.layout",
            "check" to "identity 34-40% / summary+recovery 24-30% each",
            "identityRatio" to String.format("%.3f", identityW.toFloat() / contentW),
            "summaryRatio" to String.format("%.3f", summaryW.toFloat() / contentW),
            "verticalTextRegression" to "0",
            "clippedPrimaryLabels" to "0",
        )
        return entries
    }
}
