package com.pdig.desktop

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import com.pdig.desktop.data.DesktopSession
import com.pdig.desktop.io.AwtDesktopFileOps
import com.pdig.desktop.persist.DepmapFileStore
import com.pdig.desktop.security.DeviceUnlockStore
import com.pdig.desktop.security.WindowsDpapiSecurityPort
import com.pdig.desktop.ui.PDIGAppShell
import com.pdig.desktop.ui.Screen
import com.pdig.desktop.ui.UiState
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import java.io.File

/**
 * `--profiles <outRoot>` — display-independent runtime/visual closure (A9).
 *
 * Renders the REAL PDIGAppShell composition offscreen with ImageComposeScene at:
 *   1280x720 @ 1.0 | 1920x1080 @ 1.0 | 2560x1440 @ 1.0 | 2560x1440 @ 1.25 (125% scaling)
 *   | 2560x1440 @ 1.5 (150% scaling)
 * and walks every production page per profile, verifying each frame renders
 * (non-blank, no exception) and saving PNGs. Scaling is exercised by raising the
 * scene density — the same layout code reflows exactly like a real 125%/150%
 * display, without depending on a physical monitor.
 */
object ProfileDriver {

    private val PROFILES = listOf(
        Triple("1280x720@1.0", 1280, 720 to 1.0f),
        Triple("1920x1080@1.0", 1920, 1080 to 1.0f),
        Triple("2560x1440@1.0", 2560, 1440 to 1.0f),
        Triple("2560x1440@1.25", 2560, 1440 to 1.25f),
        Triple("2560x1440@1.5", 2560, 1440 to 1.5f),
    )

    private val PAGES = listOf(
        Screen.HOME, Screen.ATTENTION, Screen.SOURCES, Screen.INFRA, Screen.FINDINGS,
        Screen.SCENARIOS, Screen.SCENARIO_SETUP, Screen.IMPACT, Screen.PLAN,
        Screen.ACTIONS, Screen.VERIFICATION, Screen.TIMELINE, Screen.BACKUP,
        Screen.RESTORE, Screen.SETTINGS, Screen.SECURITY,
    )

    @OptIn(kotlin.time.ExperimentalTime::class)
    fun run(repoRoot: File, outRoot: File): Int {
        val workDir = File(System.getProperty("java.io.tmpdir"), "pdig-profile-work").apply { mkdirs() }
        val dataFile = File(workDir, "profile.depmap")
        val store = DepmapFileStore()
        val prepared = ShotDriver.prepareSession(repoRoot, dataFile, store)
        val session = DesktopSession.restore(store.open(dataFile, ShotDriver.PASSWORD))
        val unlock = DeviceUnlockStore(
            WindowsDpapiSecurityPort(),
            File(System.getenv("APPDATA") ?: System.getProperty("user.home"), "PDIG"),
        )
        val ui = UiState(session, unlock, AwtDesktopFileOps(null)).apply {
            this.dataFile = dataFile
            this.screen = Screen.HOME
            selectedNodeId = prepared.cardId
            selectedPlanId = prepared.planId
            selectedScenarioId = "replace_payment_card"
        }

        val pngDir = File(outRoot, "desktop-profiles").apply { mkdirs() }
        val rows = mutableListOf<Map<String, String>>()
        var failures = 0

        for ((label, width, sizeDensity) in PROFILES) {
            val (height, density) = sizeDensity
            try {
                val scene = ImageComposeScene(
                    width = width,
                    height = height,
                    density = Density(density),
                    content = { com.pdig.desktop.ui.theme.PDIGTheme { PDIGAppShell(ui) } },
                )
                try {
                    for (page in PAGES) {
                        ui.screen = page
                        val img: Image = scene.render()
                        val pngBytes = img.encodeToData(EncodedImageFormat.PNG)?.bytes
                        val name = "desktop__profile__${label}__${page.name.lowercase()}.png"
                        val blank = pngBytes == null || isBlank(pngBytes)
                        if (pngBytes != null) File(pngDir, name).writeBytes(pngBytes)
                        rows.add(
                            mapOf(
                                "profile" to label,
                                "page" to page.name,
                                "size" to "${width}x$height",
                                "density" to density.toString(),
                                "render" to if (blank) "BLANK" else "OK",
                                "png" to name,
                            ),
                        )
                        if (blank) failures++
                        println("[profiles] $label ${page.name} -> ${if (blank) "BLANK" else "OK"} $name")
                    }
                } finally {
                    scene.close()
                }
            } catch (t: Throwable) {
                failures++
                rows.add(mapOf("profile" to label, "page" to "ALL", "render" to "EXCEPTION:${t.message}"))
                println("[profiles] $label FATAL :: ${t.stackTraceToString()}")
            }
        }

        val summary = File(outRoot, "desktop-profiles-summary.json")
        summary.writeText(
            "{\n  \"profiles\": ${PROFILES.map { it.first }.toString()},\n  \"pages\": ${PAGES.size},\n" +
                "  \"rows\": ${rows.size},\n  \"failures\": $failures,\n  \"verdict\": \"${if (failures == 0) "PASS" else "FAIL"}\"\n}\n",
        )
        println("[profiles] VERDICT: ${if (failures == 0) "PASS" else "FAIL ($failures)"} -> $summary")
        return if (failures == 0) 0 else 1
    }

    private fun isBlank(pngBytes: ByteArray): Boolean {
        // A truly blank (single-color) frame compresses to a few KB even at
        // 2560x1440; real UI frames are orders of magnitude larger.
        return pngBytes.size < 8 * 1024
    }
}
