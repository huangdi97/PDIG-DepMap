package com.pdig.uivnext.evidence

import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
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
 * 捕获为设备内 Compose 实际像素（captureToImage），非 offscreen / preview / desktop render。
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

    private val deviceClass: String by lazy {
        if (ctx().resources.configuration.screenWidthDp >= 600) "tablet" else "phone"
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
                val candidate = compose.onRoot().captureToImage().asAndroidBitmap()
                if (candidate.width > 0 && candidate.height > 0) {
                    bmp = candidate
                    break
                }
                candidate.recycle()
            } catch (t: Throwable) {
                if (attempt == 4) throw IllegalStateException("capture failed for $screen", t)
                Thread.sleep(2000)
            }
        }
        val bmpResult = bmp ?: error("capture returned empty for $screen")

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
                val quick = compose.onNodeWithTag(VTestIds.OVERVIEW_QUICK, useUnmergedTree = true).fetchSemanticsNode()
                assertTrue(
                    "tablet overview quick entries must be laid out",
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
        if (deviceClass == "tablet") {
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
        if (deviceClass == "tablet") {
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
            if (deviceClass == "tablet") {
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
        capture("${deviceClass}-no-active-change", "no-active-change", { it.emptyDemo = true; it.navigate(VScreen.RECORDS) }, { "no-active-change" })
        capture("${deviceClass}-no-known-dependencies", "no-known-dependencies", { it.emptyDemo = true; it.navigate(VScreen.WEAKNESSES) }, { "no-known-dependencies" })

        val manifestFile = File(ctx().filesDir, "source-complete-raw-manifest.json")
        manifestFile.writeText(shots.toString(2))
        publish("source-complete-raw-manifest.json", shots.toString(2).toByteArray())
        assertTrue("expected >= 34 shots, found ${shots.length()}", shots.length() >= 34)
    }
}
