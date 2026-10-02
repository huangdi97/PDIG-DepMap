package com.pdig.uivnext.evidence

import com.pdig.uivnext.createVNextAppState
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.persist.PresentationProfileStore
import com.pdig.uivnext.ui.VAppState
import java.io.File

/**
 * PHASE 1F-HF 证据集（Human Final Review 收口）：
 *  - 12 张 Human Final Review 主截图（1920×1080@1.0，Privacy Mask ON）→ profiles/
 *  - mechanical profiles（1280 / 2560 / 1.25 / 1.5）+ empty-state 机械帧 → mechanical/
 *  - UI_LAYOUT_PROBE.json（§11 检查）、FINAL_SCREENSHOT_MANIFEST.json（expected/actual 门禁）、
 *    EVIDENCE_SHA256SUMS.txt
 * 全部 offscreen 确定性渲染；真实窗口证据见 VNextWindowSmoke1FHF.kt（含 target validation）。
 *
 * 职责拆分（生产源文件 ≤300 行）：
 *  - 本文件：shot 定义 + run() 编排（隔离 profile store + expected/actual 门禁）；
 *  - VNextPhaseProbe1FHF.kt：UI_LAYOUT_PROBE 收集；
 *  - VNextFinalEvidenceManifest.kt：FINAL_SCREENSHOT_MANIFEST 状态记录。
 */
object VNextPhaseEvidence1FHF {

    internal data class Shot(
        val screen: VScreen,
        val tag: String,
        val camera: String = "global",
        val theme: String? = null,
        val extra: (VAppState) -> Unit = {},
    )

    /** §14 恰好 12 张主截图（无 Globe 特写 / 无 Command Palette）。 */
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

    /** 空态机械帧（不进 12 张主集）。 */
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
        val manifest = mutableListOf<Map<String, Any?>>()
        // 隔离确定性 profile store：绝不读用户主目录（~/.pdig/presentation-profiles.json），
        // 防止用户持久化偏好污染 deterministic screenshot fixture（P0：glass/city 同帧 root cause）。
        val evidenceStore = PresentationProfileStore(
            File(outRoot, ".evidence/profiles.json").apply { parentFile?.mkdirs(); delete() },
        )

        MAIN_SHOTS.forEach { shot ->
            val app = createVNextAppState(
                screen = shot.screen,
                cameraPreset = shot.camera,
                customTheme = shot.theme,
                profileStore = evidenceStore,
            )
            VNextShotDriver.prepareScreen(app, shot.screen)
            shot.extra(app)
            // §4.C selected-state check：expected != actual -> FAIL，不写 PNG。
            val expectedTheme = shot.theme
            val actualResolved = resolvedThemeOf(shot, app)
            if (expectedTheme != null && actualResolved != expectedTheme) {
                System.err.println(
                    "PHASE 1F-HF variant state mismatch: ${shot.tag} expectedTheme=$expectedTheme actualResolved=$actualResolved -> FAIL, no PNG written",
                )
                failures++
                return@forEach
            }
            val fileName = "vnext__${shot.tag}__1920x1080@1.0.png"
            try {
                val png = File(profileDir, fileName)
                VNextShotDriver.renderToFile(app, VNextShotDriver.Profile(1920, 1080, "1920x1080@1.0"), png)
                manifest += manifestEntry(shot, app, png, actualResolved)
                probe.addAll(collectProbe1FHF(shot))
            } catch (e: Throwable) {
                System.err.println("PHASE 1F-HF shot failed: $fileName -> ${e.message}")
                failures++
            }
        }

        // mechanical profiles（五档中的四档；1920×1080@1.0 主集已覆盖）
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
                val app = createVNextAppState(
                    screen = screen,
                    cameraPreset = "global",
                    profileStore = evidenceStore,
                )
                VNextShotDriver.prepareScreen(app, screen)
                extra(app)
                val fileName = "vnext__${VNextShotDriver.screenId(screen)}__${profile.label}.png"
                try {
                    VNextShotDriver.renderToFile(app, profile, File(mechDir, fileName))
                } catch (e: Throwable) {
                    System.err.println("PHASE 1F-HF mechanical failed: $fileName -> ${e.message}")
                    failures++
                }
            }
        }

        // 空态机械帧（1920×1080@1.0）
        val emptyDir = File(outRoot, "mechanical/empty-states").apply { mkdirs() }
        EMPTY_SHOTS.forEach { shot ->
            val app = createVNextAppState(
                screen = shot.screen,
                cameraPreset = "global",
                customTheme = shot.theme,
                profileStore = evidenceStore,
            )
            VNextShotDriver.prepareScreen(app, shot.screen)
            shot.extra(app)
            val fileName = "vnext__${shot.tag}__1920x1080@1.0.png"
            try {
                VNextShotDriver.renderToFile(app, VNextShotDriver.Profile(1920, 1080, "1920x1080@1.0"), File(emptyDir, fileName))
                probe.addAll(collectEmptyProbe(shot.tag))
            } catch (e: Throwable) {
                System.err.println("PHASE 1F-HF empty shot failed: $fileName -> ${e.message}")
                failures++
            }
        }

        VNextShotDriver.writeProbe(File(outRoot, "UI_LAYOUT_PROBE.json"), probe)
        VNextShotDriver.writeFinalManifest(File(outRoot, "FINAL_SCREENSHOT_MANIFEST.json"), manifest)
        VNextShotDriver.writeSha256(File(outRoot, "EVIDENCE_SHA256SUMS.txt"), outRoot)
        println(
            "VNextPhaseEvidence1FHF: wrote ${MAIN_SHOTS.size} main + ${MECHANICAL_PROFILES.size * mechScreens.size} mechanical + ${EMPTY_SHOTS.size} empty frames -> ${outRoot.absolutePath}",
        )
        return failures
    }
}
