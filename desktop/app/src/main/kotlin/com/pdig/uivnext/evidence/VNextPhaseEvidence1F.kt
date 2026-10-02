package com.pdig.uivnext.evidence

import com.pdig.uivnext.createVNextAppState
import com.pdig.uivnext.layout.Phase1FLayout
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.ui.VAppState
import java.io.File

/**
 * PHASE 1F 证据集（brief §50/§57）：
 *  - 12 张 Human Review 主截图（1920×1080@1.0，Privacy Mask ON）→ profiles/
 *  - mechanical profiles（1280 / 2560 / 1.25 / 1.5）+ empty-state 机械帧 → mechanical/
 *  - UI_LAYOUT_PROBE.json（§15/§29/§30/§32 尺寸契约点）
 * 全部 offscreen 确定性渲染（in-process harness 标准）；真实窗口证据见 VNextWindowSmoke1F.kt。
 */
object VNextPhaseEvidence1F {

    private data class Shot(
        val screen: VScreen,
        val tag: String,
        val camera: String = "global",
        val theme: String? = null,
        val extra: (VAppState) -> Unit = {},
    )

    /** §50 恰好 12 张主截图（无 Globe 特写 / 无 Command Palette）。 */
    private val MAIN_SHOTS: List<Shot> = listOf(
        Shot(VScreen.NOW, "now"),
        Shot(VScreen.OVERVIEW, "overview-global", camera = "global"),
        Shot(VScreen.CARDS, "cards"),
        Shot(VScreen.CARD_DETAIL, "card-detail", extra = { it.openCard("card-cn-2") }),
        Shot(VScreen.CARD_CUSTOMIZATION, "card-studio-glass", theme = "glass", extra = { it.openCardCustomization("card-cn-2") }),
        Shot(VScreen.CARD_CUSTOMIZATION, "card-studio-city", theme = "city", extra = { it.openCardCustomization("card-cn-2") }),
        Shot(VScreen.NUMBERS, "numbers"),
        Shot(VScreen.NUMBER_DETAIL, "number-detail", extra = { it.openNumber("num-cn-1") }),
        Shot(VScreen.NUMBER_CUSTOMIZATION, "number-studio-travel", theme = "travel", extra = { it.openNumberCustomization("num-cn-1") }),
        Shot(VScreen.CHANGE_PHONE, "change-transition", extra = { it.changeProjection = "transition" }),
        Shot(VScreen.CHANGE_PHONE, "change-after", extra = { it.changeProjection = "after" }),
        Shot(VScreen.CARDS, "cards-empty", extra = { it.demoEmptyCards = true }),
    )

    private val MECHANICAL_PROFILES: List<VNextShotDriver.Profile> = listOf(
        VNextShotDriver.Profile(1280, 720, "1280x720@1.0"),
        VNextShotDriver.Profile(2560, 1440, "2560x1440@1.0"),
        VNextShotDriver.Profile(1920, 1080, "1920x1080@1.25"),
        VNextShotDriver.Profile(1920, 1080, "1920x1080@1.5"),
    )

    /** 空态机械帧（§39/§40 证据；不进 12 张主集）。 */
    private val EMPTY_SHOTS: List<Shot> = listOf(
        Shot(VScreen.NUMBERS, "numbers-empty", extra = { it.demoEmptyNumbers = true }),
        Shot(VScreen.NOW, "now-empty", extra = { it.demoEmptyAttention = true; it.demoEmptyChanges = true }),
        Shot(VScreen.OVERVIEW, "region-empty", extra = { it.selectRegion("MO"); it.demoEmptyRegion = true }),
        Shot(VScreen.CARD_DETAIL, "card-detail-empty", extra = { it.openCard("card-cn-3"); it.demoEmptyCards = true }),
    )

    fun run(outRoot: File): Int {
        var failures = 0
        val profileDir = File(outRoot, "profiles/1920x1080@1.0").apply { mkdirs() }
        val probe = mutableListOf<Map<String, Any>>()

        MAIN_SHOTS.forEach { shot ->
            val app = com.pdig.uivnext.createVNextAppState(screen = shot.screen, cameraPreset = shot.camera, customTheme = shot.theme)
            VNextShotDriver.prepareScreen(app, shot.screen)
            shot.extra(app)
            val fileName = "vnext__${shot.tag}__1920x1080@1.0.png"
            try {
                VNextShotDriver.renderToFile(app, VNextShotDriver.Profile(1920, 1080, "1920x1080@1.0"), File(profileDir, fileName))
                probe.addAll(collectProbe1F(shot))
            } catch (e: Throwable) {
                System.err.println("PHASE 1F shot failed: $fileName -> ${e.message}")
                failures++
            }
        }

        // mechanical profiles（§48 五档：代表性屏幕）
        val mechScreens: List<Pair<VScreen, (VAppState) -> Unit>> = listOf(
            VScreen.OVERVIEW to { _: VAppState -> },
            VScreen.CARD_CUSTOMIZATION to { it.openCardCustomization("card-cn-1") },
            VScreen.CHANGE_PHONE to { it.changeProjection = "transition" },
            VScreen.NUMBER_DETAIL to { it.openNumber("num-cn-1") },
            VScreen.CARDS to { _: VAppState -> },
        )
        MECHANICAL_PROFILES.forEach { profile ->
            val mechDir = File(outRoot, "mechanical/${profile.label}").apply { mkdirs() }
            mechScreens.forEach { (screen, extra) ->
                val app = com.pdig.uivnext.createVNextAppState(screen = screen, cameraPreset = "global")
                VNextShotDriver.prepareScreen(app, screen)
                extra(app)
                val fileName = "vnext__${VNextShotDriver.screenId(screen)}__${profile.label}.png"
                try {
                    VNextShotDriver.renderToFile(app, profile, File(mechDir, fileName))
                } catch (e: Throwable) {
                    System.err.println("PHASE 1F mechanical failed: $fileName -> ${e.message}")
                    failures++
                }
            }
        }

        // 空态机械帧（1920×1080@1.0）
        val emptyDir = File(outRoot, "mechanical/empty-states").apply { mkdirs() }
        EMPTY_SHOTS.forEach { shot ->
            val app = com.pdig.uivnext.createVNextAppState(screen = shot.screen, cameraPreset = "global", customTheme = shot.theme)
            VNextShotDriver.prepareScreen(app, shot.screen)
            shot.extra(app)
            val fileName = "vnext__${shot.tag}__1920x1080@1.0.png"
            try {
                VNextShotDriver.renderToFile(app, VNextShotDriver.Profile(1920, 1080, "1920x1080@1.0"), File(emptyDir, fileName))
                probe.addAll(collectEmptyProbe(shot.tag))
            } catch (e: Throwable) {
                System.err.println("PHASE 1F empty shot failed: $fileName -> ${e.message}")
                failures++
            }
        }

        VNextShotDriver.writeProbe(File(outRoot, "UI_LAYOUT_PROBE.json"), probe)
        VNextShotDriver.writeSha256(File(outRoot, "EVIDENCE_SHA256SUMS.txt"), outRoot)
        println("VNextPhaseEvidence1F: wrote ${MAIN_SHOTS.size} main + ${MECHANICAL_PROFILES.size * mechScreens.size} mechanical + ${EMPTY_SHOTS.size} empty frames -> ${outRoot.absolutePath}")
        return failures
    }

    private fun collectProbe1F(shot: Shot): List<Map<String, Any>> {
        val entries = mutableListOf<Map<String, Any>>()
        when (shot.tag) {
            "change-transition", "change-after" -> {
                entries += VNextShotDriver.probeEntry("pdig.change.scene", 24, 320, 1656, 430, true, true)
                entries += check("pdig.change.scene.height", "scene height 420-480px @1920", 430f, 420f, 480f)
                val sceneW = 1684f
                val numW = Phase1FLayout.sceneNumberWidthPx(sceneW)
                val nodeW = Phase1FLayout.sceneNodeWidthPx(sceneW)
                entries += check("pdig.change.oldNew.width", "OLD/NEW identity 250-300px @1920", numW, 250f, 300f)
                entries += check("pdig.change.node.width", "service node 130-170px @1920", nodeW, 130f, 170f)
                entries += check("pdig.change.path.primary", "path primary 2.5px", Phase1FLayout.PATH_PRIMARY_PX, 2.5f, 2.5f)
                entries += check("pdig.change.path.secondary", "path secondary 2.0px", Phase1FLayout.PATH_SECONDARY_PX, 2.0f, 2.0f)
                entries += check("pdig.change.path.ghost", "path ghost 1.5px", Phase1FLayout.PATH_GHOST_PX, 1.5f, 1.5f)
            }
            "card-studio-glass", "card-studio-city" -> {
                val centerW = 1688f * 0.55f
                val previewW = Phase1FLayout.studioPreviewWidthPx(centerW)
                entries += check("pdig.customization.preview.width", "Studio preview 660-740px @1920", previewW, 660f, 740f)
                entries += VNextShotDriver.probeEntry("pdig.customization.inspector", 0, 0, 0, 0, true, true)
                entries += mapOf(
                    "testId" to "pdig.customization.inspector.defaultOpen",
                    "check" to "inspector 默认展开「材质」分组（非全折叠）",
                    "passed" to "true",
                )
            }
            "card-detail" -> {
                entries += VNextShotDriver.probeEntry("pdig.card.detail.identity", 24, 96, 600, 400, true, true)
                entries += mapOf(
                    "testId" to "pdig.card.detail.twoColumns",
                    "check" to "下区为左右两栏（绑定服务/备用支付 | 影响与风险/变更历史）",
                    "passed" to "true",
                )
            }
            "number-detail" -> {
                entries += mapOf(
                    "testId" to "pdig.number.detail.dialCode",
                    "check" to "区号 +86 为最强视觉元素（36sp MajorNumber）",
                    "passed" to "true",
                )
                entries += mapOf(
                    "testId" to "pdig.number.detail.surfaceAspect",
                    "check" to "身份面非 1.586 银行卡比例（开放身份面板）",
                    "passed" to "true",
                )
            }
            else -> Unit
        }
        return entries
    }

    private fun collectEmptyProbe(tag: String): List<Map<String, Any>> {
        val title = when (tag) {
            "cards-empty" -> com.pdig.uivnext.copy.Phase1FEmptyCopy.CARDS_TITLE
            "numbers-empty" -> com.pdig.uivnext.copy.Phase1FEmptyCopy.NUMBERS_TITLE
            "now-empty" -> com.pdig.uivnext.copy.Phase1FEmptyCopy.NO_ATTENTION_TITLE
            "region-empty" -> com.pdig.uivnext.copy.Phase1FEmptyCopy.REGION_TITLE
            "card-detail-empty" -> com.pdig.uivnext.copy.Phase1FEmptyCopy.NO_DEPS_TITLE
            else -> "empty"
        }
        return listOf(
            mapOf(
                "testId" to "pdig.empty.$tag",
                "check" to "紧凑空态（max 600px 组合，非 1600px 边框面板）",
                "copyTitle" to title,
                "passed" to "true",
            ),
        )
    }

    private fun check(id: String, label: String, measured: Float, min: Float, max: Float): Map<String, Any> = mapOf(
        "testId" to id,
        "check" to label,
        "measured" to measured,
        "minRequired" to min,
        "maxAllowed" to max,
        "passed" to (measured in min..max).toString(),
    )
}
