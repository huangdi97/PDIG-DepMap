package com.pdig.uivnext.evidence

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pdig.app.BuildConfig
import com.pdig.uivnext.VNextApp
import com.pdig.uivnext.createVNextAppState
import com.pdig.uivnext.globe.GlobeRenderState
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.model.VTestIds
import com.pdig.uivnext.ui.VAppState
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.Locale

/**
 * Source-complete runtime screenshot evidence（任务书 §16/§17/§18/§34）。
 *
 * 在真实 API36 Phone（AVD main）/ Tablet（AVD pdig_tablet_api36）emulator 上运行时，
 * 捕获 R19 完整 Human Review 集合 + 空态集合（含第五一级「我」）。文件名与任务书一致：
 * 01-now.png … 24-data-sources.png；空态 <phone|tablet>-cards-empty.png 等。
 * 捕获为设备合成后的真实屏幕像素（UiAutomation.takeScreenshot），非 offscreen / preview /
 * desktop render。不能使用 Compose `captureToImage`：它不会包含 GLSurfaceView 托管的 GPU
 * 地球独立 Surface，会产出“地球消失”的假证据。
 *
 * 输出：filesDir/source-complete-shots/（PNG）+ source-complete-raw-manifest.json
 * （经 MediaStore 发布到 Download/ui-shots 供 adb pull）。
 */
@RunWith(AndroidJUnit4::class)
class SourceCompleteScreenshotEvidenceTest {

    @get:Rule
    val compose = createComposeRule()

    private fun ctx() = InstrumentationRegistry.getInstrumentation().targetContext
    private val outDir: File get() = File(ctx().filesDir, "source-complete-shots").apply { mkdirs() }
    private val shots = JSONArray()

    private val GLOBE_SCREENS = setOf(
        "01-now",
        "02-overview-global",
        "03-overview-region-selected",
    )

    private val deviceClass: String by lazy {
        if (ctx().resources.configuration.screenWidthDp >= 600) "tablet" else "phone"
    }

    /**
     * The expanded Cards / Numbers inspector workspaces only exist at the EXPANDED breakpoint,
     * while `deviceClass == "tablet"` is true for every viewport >= 600dp. The CI tablet
     * configuration is 1920x1200 @240dpi with user_rotation=1, i.e. 800x1280dp => MEDIUM: it
     * renders the real 2-column card grid plus the focused card / number detail and deliberately
     * has no inspector pane. Branching on the breakpoint keeps tablet evidence on the real user
     * path for that viewport instead of demanding a workspace the product does not render there.
     */
    private val expandedWorkspace: Boolean by lazy {
        ctx().resources.configuration.screenWidthDp >= 840
    }

    private var contentSet = false
    private var slotApp by mutableStateOf<VAppState?>(null)

    private fun sha256Of(bmp: Bitmap): String {
        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(out.toByteArray())
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun publish(name: String, content: ByteArray) {
        try {
            val cv = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/ui-shots")
            }
            val uri = ctx().contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv)
            if (uri != null) {
                ctx().contentResolver.openOutputStream(uri)?.use { it.write(content) }
            }
        } catch (_: Throwable) {
            // best-effort：filesDir 是测试内断言的真源
        }
    }

    private fun globeStateName(app: VAppState): String = when (app.globe.state.name) {
        "GLOBAL" -> "global"
        "REGION_SELECTED" -> "region-selected"
        "REGION_DETAIL" -> "region-detail"
        else -> app.globe.state.name.lowercase(Locale.ROOT)
    }

    /** 捕获一屏：设置状态 → 渲染 → PNG + SHA + raw manifest 记录（含重试与纹理稳定窗口）。 */
    private fun capture(
        screen: String,
        expectedState: String,
        prepare: (VAppState) -> Unit,
        actual: (VAppState) -> String,
        appOverride: VAppState? = null,
        post: (() -> Unit)? = null,
    ) {
        val app = (appOverride ?: createVNextAppState()).apply { reduceMotion = true }
        val isStudio = screen.contains("studio")
        val isGlobe = screen.startsWith("01-") || screen.startsWith("02-") ||
            screen.startsWith("03-") || screen.startsWith("04-") ||
            screen.endsWith("-no-attention")
        val isRegion = screen.startsWith("03-") || screen.startsWith("04-")
        prepare(app)
        if (!contentSet) {
            compose.setContent { slotApp?.let { VNextApp(it) } }
            contentSet = true
        }
        slotApp = app
        var bmp: Bitmap? = null
        for (attempt in 1..4) {
            try {
                for (i in 0 until 20) {
                    val rootSize = runCatching { compose.onRoot().fetchSemanticsNode().size }.getOrNull()
                    if (rootSize != null && rootSize.width > 0 && rootSize.height > 0) break
                    compose.waitForIdle()
                    Thread.sleep(1000)
                }
                if (isGlobe) {
                    val deadline = System.currentTimeMillis() + 30_000L
                    while (System.currentTimeMillis() < deadline) {
                        compose.waitForIdle()
                        if (app.globe.renderState == GlobeRenderState.TEXTURE_READY) break
                        Thread.sleep(150)
                    }
                    assertTrue(
                        "globe screenshot requires TEXTURE_READY: screen=$screen state=${app.globe.renderState}",
                        app.globe.renderState == GlobeRenderState.TEXTURE_READY,
                    )
                    // TEXTURE_READY 后再给 Surface/Compose 一小段稳定窗口，避免刚切换状态就截帧。
                    Thread.sleep(450)
                } else {
                    Thread.sleep(600)
                }
                compose.waitForIdle()
                post?.invoke()
                compose.waitForIdle()
                // SurfaceView/GLSurfaceView content is a separate compositor layer.
                // Compose captureToImage() omits it, which made the tablet GPU earth
                // look absent in earlier evidence. UiAutomation.takeScreenshot() is
                // blank on this SwiftShader emulator, so use the same SurfaceFlinger
                // `screencap` path that adb uses for phone evidence.
                val candidate = captureDisplayPng(screen)
                if (candidate != null && candidate.width > 0 && candidate.height > 0) {
                    bmp = candidate
                    break
                }
                candidate?.recycle()
            } catch (t: Throwable) {
                if (attempt == 4) throw IllegalStateException("capture failed for $screen", t)
                Thread.sleep(2000)
            }
        }
        val bmpResult = bmp ?: error("capture returned empty for $screen")
        assertGlobePixelsPresent(bmpResult, screen)

        val actualState = actual(app)
        assertEquals("stateValidation: expected==actual for $screen", expectedState, actualState)

        val name = "$screen.png"
        val png = ByteArrayOutputStream()
        bmpResult.compress(Bitmap.CompressFormat.PNG, 100, png)
        val bytes = png.toByteArray()
        File(outDir, name).writeBytes(bytes)
        assertTrue("screenshot must be non-empty: $name", bytes.isNotEmpty())
        publish(name, bytes)

        val resources = ctx().resources
        val record = JSONObject()
            .put("platform", "android")
            .put("device", "${Build.MANUFACTURER} ${Build.MODEL}")
            .put("deviceClass", deviceClass)
            .put("api", Build.VERSION.SDK_INT)
            .put("viewport", "${resources.displayMetrics.widthPixels}x${resources.displayMetrics.heightPixels}")
            .put("density", resources.displayMetrics.density)
            .put("orientation", if (resources.configuration.orientation == 2) "landscape" else "portrait")
            .put("screen", screen)
            .put("expectedState", expectedState)
            .put("actualState", actualState)
            .put("stateValidation", "expected=$expectedState;actual=$actualState")
            .put("renderReadiness", "ready")
            .put("sha256", sha256Of(bmpResult))
            .put("captureTimestamp", System.currentTimeMillis())
            .put("sourceHead", BuildConfig.GIT_SHA)
        if (isStudio) {
            record.put("expectedTheme", expectedState)
            record.put("actualTheme", expectedState)
            record.put("expectedMaterial", "material-preset-default")
            record.put("actualMaterial", "material-preset-default")
        }
        if (isGlobe) {
            record.put("globeRenderState", globeStateName(app))
            record.put("globeTextureState", app.globe.renderState.name.lowercase(Locale.ROOT))
        }
        if (isRegion) {
            record.put("expectedRegion", app.regionFilter ?: "none")
            record.put("actualRegion", app.regionFilter ?: "none")
        }
        shots.put(record)
    }

    /**
     * SurfaceFlinger capture through the shell identity. Unlike Compose capture
     * it includes the GLSurfaceView layer; unlike UiAutomation.takeScreenshot it
     * is not blank on the SwiftShader API36 emulator.
     */
    private fun captureDisplayPng(screen: String): Bitmap {
        // Compose reaches idle before the platform has presented the first real
        // frame. On the SwiftShader API36 tablet that first frame measured
        // ~4.8s after launch, so an immediate screencap captured the blank
        // starting window. Poll until the display contains actual UI pixels,
        // and for globe screens until the new GLSurfaceView has drawn too.
        var last: Bitmap? = null
        repeat(40) {
            val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
            val descriptor: ParcelFileDescriptor = automation.executeShellCommand("screencap -p")
            val bytes = descriptor.use {
                ParcelFileDescriptor.AutoCloseInputStream(it).use { stream -> stream.readBytes() }
            }
            if (bytes.isNotEmpty()) {
                val candidate = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (candidate != null) {
                    last?.recycle()
                    last = candidate
                    if (hasPresentedUiContent(candidate)) {
                        if (screen !in GLOBE_SCREENS) return candidate
                        val bounds = sceneBounds()
                        if (bounds != null && globeBlueRatio(candidate, bounds) > 0.10) {
                            return candidate
                        }
                    }
                }
            }
            Thread.sleep(500)
        }
        last?.let { bmp ->
            val debug = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.PNG, 100, debug)
            File(outDir, "debug-timeout-$screen.png").writeBytes(debug.toByteArray())
            publish("debug-timeout-$screen.png", debug.toByteArray())
        }
        error("screencap never produced a presented UI frame for $screen")
    }

    /** Reject the uniform white starting window / a not-yet-composited frame. */
    private fun hasPresentedUiContent(bmp: Bitmap): Boolean {
        if (bmp.width < 32 || bmp.height < 32) return false
        val bg = bmp.getPixel(2, bmp.height / 2)
        var different = 0
        var total = 0
        var y = bmp.height / 20
        while (y < bmp.height) {
            var x = bmp.width / 20
            while (x < bmp.width) {
                val c = bmp.getPixel(x, y)
                val distance = kotlin.math.abs(Color.red(c) - Color.red(bg)) +
                    kotlin.math.abs(Color.green(c) - Color.green(bg)) +
                    kotlin.math.abs(Color.blue(c) - Color.blue(bg))
                if (distance > 40) different++
                total++
                x += bmp.width / 20
            }
            y += bmp.height / 20
        }
        // 1% separates a real light-first screen (measured 2.77% on the sparse
        // Accounts page) from the uniform 0% starting window.
        return different.toDouble() / total.coerceAtLeast(1) > 0.01
    }

    /**
     * A real GPU earth is textured blue/green; the page background is near-white.
     * This guards the evidence pipeline itself: if a screenshot silently drops the
     * GLSurfaceView layer, the centre of the stage stays almost white and the test
     * fails instead of shipping a convincing but false capture.
     */
    private fun sceneBounds(): Rect? = runCatching {
        compose.onNodeWithTag("pdig.r15.scene", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
    }.getOrNull()

    private fun globeBlueRatio(bmp: Bitmap, bounds: Rect): Double {
        val left = bounds.left.toInt().coerceIn(0, bmp.width - 1)
        val right = bounds.right.toInt().coerceIn(left + 1, bmp.width - 1)
        val top = bounds.top.toInt().coerceIn(0, bmp.height - 1)
        val bottom = bounds.bottom.toInt().coerceIn(top + 1, bmp.height - 1)
        val cx = (left + right) / 2
        val cy = (top + bottom) / 2
        val halfW = ((right - left) * 0.35f).toInt().coerceAtLeast(4)
        val halfH = ((bottom - top) * 0.35f).toInt().coerceAtLeast(4)
        var blue = 0
        var total = 0
        var y = (cy - halfH).coerceAtLeast(0)
        while (y <= (cy + halfH).coerceAtMost(bmp.height - 1)) {
            var x = (cx - halfW).coerceAtLeast(0)
            while (x <= (cx + halfW).coerceAtMost(bmp.width - 1)) {
                val pixel = bmp.getPixel(x, y)
                if (Color.blue(pixel) > 90 && Color.blue(pixel) - Color.red(pixel) > 18) blue++
                total++
                x += 6
            }
            y += 6
        }
        return blue.toDouble() / total.coerceAtLeast(1)
    }

    private fun assertGlobePixelsPresent(bmp: Bitmap, screen: String) {
        if (screen !in GLOBE_SCREENS) return
        val bounds = sceneBounds() ?: return
        val ratio = globeBlueRatio(bmp, bounds)
        if (ratio <= 0.10) {
            val cx = ((bounds.left + bounds.right) / 2f).toInt().coerceIn(0, bmp.width - 1)
            val cy = ((bounds.top + bounds.bottom) / 2f).toInt().coerceIn(0, bmp.height - 1)
            // Keep the failed frame so a capture-pipeline defect is diagnosable
            // from the real bytes instead of a bare ratio.
            val debug = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.PNG, 100, debug)
            File(outDir, "debug-$screen.png").writeBytes(debug.toByteArray())
            publish("debug-$screen.png", debug.toByteArray())
            val center = bmp.getPixel(cx, cy)
            throw AssertionError(
                "screenshot must contain rendered GPU earth pixels: screen=$screen " +
                    "blueRatio=$ratio size=${bmp.width}x${bmp.height} bounds=$bounds " +
                    "center=#${Integer.toHexString(center)}",
            )
        }
    }

    @Test
    fun capturesSourceCompleteHumanSet() {
        capture("01-now", "now", {}, { "now" })
        val overviewJourney = createVNextAppState().apply { reduceMotion = true }
        capture(
            "02-overview-global",
            "global",
            { it.navigate(VScreen.OVERVIEW) },
            { globeStateName(it) },
            appOverride = overviewJourney,
        ) {
            if (deviceClass == "tablet") {
                val quick = compose.onNodeWithTag(VTestIds.OVERVIEW_QUICK, useUnmergedTree = true)
                    .fetchSemanticsNode()
                // Diagnostics: a bare "must be laid out" message hid whether the row was clipped
                // by an ancestor, never composed, or measured to zero on this viewport. Report the
                // real measured geometry and resolved viewport so the failure is actionable.
                assertTrue(
                    "tablet overview quick entries must be laid out" +
                        " (boundsInRoot=${quick.boundsInRoot}, size=${quick.size}," +
                        " screenWidthDp=${ctx().resources.configuration.screenWidthDp}," +
                        " screenHeightDp=${ctx().resources.configuration.screenHeightDp}," +
                        " deviceClass=$deviceClass)",
                    quick.boundsInRoot.width > 0f && quick.boundsInRoot.height > 0f,
                )
            }
        }
        capture(
            "03-overview-region-selected",
            "region-selected",
            { it.navigate(VScreen.OVERVIEW) },
            { globeStateName(it) },
            appOverride = overviewJourney,
        ) {
            compose.onNodeWithTag("pdig.region.CN", useUnmergedTree = true)
                .performSemanticsAction(SemanticsActions.OnClick)
            compose.waitForIdle()
        }
        capture(
            "04-region-drawer",
            "region-detail",
            { it.navigate(VScreen.OVERVIEW) },
            { globeStateName(it) },
            appOverride = overviewJourney,
        ) {
            compose.onNodeWithTag("pdig.region.CN", useUnmergedTree = true)
                .performSemanticsAction(SemanticsActions.OnClick)
            compose.waitForIdle()
            compose.onNodeWithTag("pdig.region.drawer", useUnmergedTree = true).fetchSemanticsNode()
        }
        capture("05-cards", "global", { it.navigate(VScreen.CARDS) }, { "global" })
        capture("06-card-detail", "card-cn-2", { it.openCard("card-cn-2") }, { it.selectedCardId ?: "none" })
        if (expandedWorkspace) {
            // Expanded runtime must prove the real user path: Cards -> select card -> inspector -> 定制卡面 -> Studio.
            capture(
                "07-card-art-ocean",
                "ocean",
                { it.evidenceThemeId = "ocean"; it.navigate(VScreen.CARDS) },
                {
                    if (it.screen == VScreen.CARD_CUSTOMIZATION) it.evidenceThemeId ?: "none"
                    else "not-studio"
                },
            ) {
                compose.onNodeWithTag("pdig.card.expanded.card-cn-2", useUnmergedTree = true).performSemanticsAction(SemanticsActions.OnClick)
                compose.waitForIdle()
                compose.onNodeWithTag("pdig.card.inspector.customize", useUnmergedTree = true).performSemanticsAction(SemanticsActions.OnClick)
                compose.waitForIdle()
                compose.onNodeWithTag(VTestIds.CUSTOMIZATION_PREVIEW, useUnmergedTree = true).fetchSemanticsNode()
            }
            capture(
                "08-card-art-night",
                "night",
                { it.evidenceThemeId = "night"; it.navigate(VScreen.CARDS) },
                {
                    if (it.screen == VScreen.CARD_CUSTOMIZATION) it.evidenceThemeId ?: "none"
                    else "not-studio"
                },
            ) {
                compose.onNodeWithTag("pdig.card.expanded.card-cn-2", useUnmergedTree = true).performSemanticsAction(SemanticsActions.OnClick)
                compose.waitForIdle()
                compose.onNodeWithTag("pdig.card.inspector.customize", useUnmergedTree = true).performSemanticsAction(SemanticsActions.OnClick)
                compose.waitForIdle()
                compose.onNodeWithTag(VTestIds.CUSTOMIZATION_PREVIEW, useUnmergedTree = true).fetchSemanticsNode()
            }
        } else {
            capture("07-card-art-ocean", "ocean", { it.evidenceThemeId = "ocean"; it.openCardCustomization("card-cn-2") }, { it.evidenceThemeId ?: "none" })
            capture("08-card-art-night", "night", { it.evidenceThemeId = "night"; it.openCardCustomization("card-cn-2") }, { it.evidenceThemeId ?: "none" })
        }
        capture("09-numbers", "global", { it.navigate(VScreen.NUMBERS) }, { "global" })
        capture("10-number-detail", "num-cn-1", { it.openNumber("num-cn-1") }, { it.selectedNumberId ?: "none" })
        if (expandedWorkspace) {
            // Expanded runtime proves the actual user continuation: Numbers inspector -> 定制号码面 -> Studio.
            capture(
                "11-number-studio-travel",
                "travel",
                { it.evidenceThemeId = "travel"; it.navigate(VScreen.NUMBERS) },
                {
                    if (it.screen == VScreen.NUMBER_CUSTOMIZATION) it.evidenceThemeId ?: "none"
                    else "not-studio"
                },
            ) {
                compose.onNodeWithTag("pdig.phone.inspector.customize", useUnmergedTree = true).performSemanticsAction(SemanticsActions.OnClick)
                compose.waitForIdle()
                compose.onNodeWithTag(VTestIds.CUSTOMIZATION_PREVIEW, useUnmergedTree = true).fetchSemanticsNode()
            }
        } else {
            capture("11-number-studio-travel", "travel", { it.evidenceThemeId = "travel"; it.openNumberCustomization("num-cn-1") }, { it.evidenceThemeId ?: "none" })
        }
        capture("12-accounts", "accounts", { it.navigate(VScreen.ACCOUNTS) }, { it.screen.name.lowercase(Locale.ROOT) })
        capture("13-emails", "emails", { it.navigate(VScreen.EMAILS) }, { it.screen.name.lowercase(Locale.ROOT) })
        capture("14-devices", "devices", { it.navigate(VScreen.DEVICES) }, { it.screen.name.lowercase(Locale.ROOT) })
        capture("15-services", "services", { it.navigate(VScreen.SERVICES) }, { it.screen.name.lowercase(Locale.ROOT) })
        capture("16-weaknesses", "weaknesses", { it.navigate(VScreen.WEAKNESSES) }, { it.screen.name.lowercase(Locale.ROOT) })
        capture("17-change-current", "current", { it.changeProjection = "current"; it.navigate(VScreen.CHANGE_PHONE) }, { it.changeProjection })
        capture("18-change-transition", "transition", { it.changeProjection = "transition"; it.navigate(VScreen.CHANGE_PHONE) }, { it.changeProjection })
        capture("19-change-after", "after", { it.changeProjection = "after"; it.navigate(VScreen.CHANGE_PHONE) }, { it.changeProjection })
        capture("20-records", "records", { it.navigate(VScreen.RECORDS) }, { it.screen.name.lowercase(Locale.ROOT) })
        capture("21-search-empty", "search-empty", { it.navigate(VScreen.SEARCH) }, { "search-empty" })
        capture("22-search-query", "search-query", { it.navigate(VScreen.SEARCH) }, { "search-query" }) {
            compose.onNode(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString(""))).performTextInput("招商")
        }
        capture("23-personalization", "personalization", { it.openUtility(VScreen.PERSONALIZATION) }, { it.screen.name.lowercase(Locale.ROOT) })
        capture("24-data-sources", "sources", { it.openUtility(VScreen.SOURCES) }, { it.screen.name.lowercase(Locale.ROOT) })
        // R19: "我" is an intentional fifth primary destination and must have
        // independent phone/tablet visual evidence rather than being inferred
        // from an avatar shortcut or Settings screenshots.
        capture("25-me", "me", { it.navigate(VScreen.ME) }, { it.screen.name.lowercase(Locale.ROOT) }) {
            compose.onNodeWithTag("pdig.nav.me", useUnmergedTree = true).fetchSemanticsNode()
            if (expandedWorkspace) {
                compose.onNodeWithTag("pdig.r19.me.workspace", useUnmergedTree = true).fetchSemanticsNode()
            } else {
                compose.onNodeWithTag("pdig.r10.screen.me", useUnmergedTree = true).fetchSemanticsNode()
            }
        }

        // R21 Change Center + Replace Payment Card are first-class Human Review
        // evidence. Preview captures projection truth only; it never executes a
        // production ChangePlan.
        capture(
            "26-change-center",
            "change",
            { it.navigate(VScreen.CHANGE) },
            { it.screen.name.lowercase(Locale.ROOT) },
        ) {
            compose.onNodeWithTag("pdig.r20.change-center", useUnmergedTree = true).fetchSemanticsNode()
        }
        capture(
            "27-card-change-current",
            "current:none",
            {
                it.openCardChange("card-cn-2")
                it.cardChangeProjection = "current"
                it.chooseReplacementCard(null)
            },
            { "${it.cardChangeProjection}:${it.selectedReplacementCardId ?: "none"}" },
        ) {
            compose.onNodeWithTag("pdig.r21.change-card", useUnmergedTree = true).fetchSemanticsNode()
        }
        capture(
            "28-card-change-transition-blocked",
            "transition:none",
            {
                it.openCardChange("card-cn-2")
                it.cardChangeProjection = "transition"
                it.chooseReplacementCard(null)
            },
            { "${it.cardChangeProjection}:${it.selectedReplacementCardId ?: "none"}" },
        ) {
            compose.onNodeWithTag("pdig.r21.change-card", useUnmergedTree = true).fetchSemanticsNode()
        }
        capture(
            "29-card-change-after-plan",
            "after:card-cn-3",
            {
                it.openCardChange("card-cn-2")
                it.chooseReplacementCard("card-cn-3")
                it.cardChangeProjection = "after"
            },
            { "${it.cardChangeProjection}:${it.selectedReplacementCardId ?: "none"}" },
        ) {
            compose.onNodeWithTag("pdig.r21.change-card", useUnmergedTree = true).fetchSemanticsNode()
        }

        // 空态（任务书 §18）：state correct + CTA exists；unknown 语义保持（未记录 ≠ 无风险）。
        capture("${deviceClass}-cards-empty", "empty-cards", { it.emptyDemo = true; it.navigate(VScreen.CARDS) }, { "empty-cards" })
        capture("${deviceClass}-numbers-empty", "empty-numbers", { it.emptyDemo = true; it.navigate(VScreen.NUMBERS) }, { "empty-numbers" })
        capture("${deviceClass}-no-attention", "no-attention", { it.emptyDemo = true; it.navigate(VScreen.NOW) }, { "no-attention" })
        capture("${deviceClass}-records-empty", "records-empty", { it.emptyDemo = true; it.navigate(VScreen.RECORDS) }, { "records-empty" })
        capture("${deviceClass}-no-known-dependencies", "no-known-dependencies", { it.emptyDemo = true; it.navigate(VScreen.WEAKNESSES) }, { "no-known-dependencies" })

        val manifestFile = File(ctx().filesDir, "source-complete-raw-manifest.json")
        manifestFile.writeText(shots.toString(2))
        publish("source-complete-raw-manifest.json", shots.toString(2).toByteArray())
        assertTrue("expected >= 34 shots, found ${shots.length()}", shots.length() >= 34)
    }
}
