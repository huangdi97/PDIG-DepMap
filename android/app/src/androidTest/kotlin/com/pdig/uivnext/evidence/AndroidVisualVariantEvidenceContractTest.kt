package com.pdig.uivnext.evidence

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pdig.uivnext.VNextApp
import com.pdig.uivnext.createVNextAppState
import com.pdig.uivnext.model.VScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.security.MessageDigest

/**
 * Android 版 VisualVariantEvidenceContract（任务书 §15/§41）：
 * 每个变体同时验证：
 *  (A) semantic state 不同且与 manifest 一致（evidenceThemeId / changeProjection / cameraPreset）；
 *  (B) render output 不同：SHA256(frameA) != SHA256(frameB)。
 *
 * 一律使用 fresh synthetic fixture + evidence 参数（绝不读用户持久化 profile：
 * Android 演示壳不写任何持久化 profile，见 P0 隔离断言）。
 */
@RunWith(AndroidJUnit4::class)
class AndroidVisualVariantEvidenceContractTest {

    @get:Rule
    val compose = createComposeRule()

    private var contentSet = false
    private var slotApp by mutableStateOf<com.pdig.uivnext.ui.VAppState?>(null)

    /** 每测试仅 setContent 一次；后续渲染通过替换 [slotApp] 切换状态。 */
    private fun renderApp(app: com.pdig.uivnext.ui.VAppState, widthDp: Int? = null): Bitmap {
        if (!contentSet) {
            compose.setContent {
                slotApp?.let { current ->
                    if (widthDp != null) VNextApp(current, forcedViewportWidthDp = widthDp) else VNextApp(current)
                }
            }
            contentSet = true
        }
        slotApp = app
        // 捕获（含重试）：活动窗口冷启动可能未就绪 / 根节点 0 尺寸（实测 AVD 偶发），最多 4 次。
        var bmp: Bitmap? = null
        for (attempt in 1..4) {
            try {
                for (i in 0 until 20) {
                    val rootSize = runCatching { compose.onRoot().fetchSemanticsNode().size }.getOrNull()
                    if (rootSize != null && rootSize.width > 0 && rootSize.height > 0) break
                    compose.waitForIdle()
                    Thread.sleep(1000)
                }
                Thread.sleep(1200) // 纹理地球后台渲染稳定窗口
                compose.waitForIdle()
                val candidate = compose.onRoot().captureToImage().asAndroidBitmap()
                if (candidate.width > 0 && candidate.height > 0) {
                    bmp = candidate
                    break
                }
                candidate.recycle()
            } catch (t: Throwable) {
                if (attempt == 4) throw IllegalStateException("renderApp capture failed", t)
                Thread.sleep(2000)
            }
        }
        return bmp ?: error("renderApp returned empty bitmap")
    }
    private fun sha256Of(bmp: Bitmap): String {
        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(out.toByteArray())
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun assertState(readback: String?, expected: String, label: String) {
        assertEquals("$label: deterministic state must equal expected", expected, readback)
    }

    // ── 1. Card image：ocean != night ──────────────────────────────────────────
    @Test
    fun cardImageOceanDiffersFromNight() {
        val oceanApp = createVNextAppState(customTheme = "ocean").apply { openCardCustomization("card-cn-2") }
        val nightApp = createVNextAppState(customTheme = "night").apply { openCardCustomization("card-cn-2") }
        val shaOcean = sha256Of(renderApp(oceanApp))
        val shaNight = sha256Of(renderApp(nightApp))
        assertState(oceanApp.evidenceThemeId, "ocean", "card image")
        assertState(nightApp.evidenceThemeId, "night", "card image")
        assertNotEquals("card image ocean frame SHA256 must differ from night frame", shaOcean, shaNight)
    }

    // ── 2. Number Studio：country != travel != recovery ───────────────────────
    @Test
    fun numberStudioCountryTravelRecoveryPairwiseDistinct() {
        val themes = listOf("country", "travel", "recovery")
        val hashes = themes.associateWith { theme ->
            val app = createVNextAppState(customTheme = theme).apply { openNumberCustomization("num-cn-1") }
            assertState(app.evidenceThemeId, theme, "number studio")
            theme to sha256Of(renderApp(app))
        }
        assertNotEquals("number studio country != travel", hashes["country"], hashes["travel"])
        assertNotEquals("number studio travel != recovery", hashes["travel"], hashes["recovery"])
        assertNotEquals("number studio country != recovery", hashes["country"], hashes["recovery"])
    }

    // ── 3. Change Phone：current != transition != after ───────────────────────
    @Test
    fun changePhoneCurrentTransitionAfterAreDistinct() {
        val projections = listOf("current", "transition", "after")
        val hashes = projections.associateWith { projection ->
            val app = createVNextAppState(screen = VScreen.CHANGE_PHONE, changeProjection = projection)
            assertEquals("change-phone semantic state must match", projection, app.changeProjection)
            projection to sha256Of(renderApp(app))
        }
        assertNotEquals("change-phone current != transition", hashes["current"], hashes["transition"])
        assertNotEquals("change-phone transition != after", hashes["transition"], hashes["after"])
        assertNotEquals("change-phone current != after", hashes["current"], hashes["after"])
    }

    // ── 4. Region：global != HK（cameraPreset 语义 + 渲染）────────────────────
    @Test
    fun regionGlobalDiffersFromHk() {
        val global = createVNextAppState(screen = VScreen.OVERVIEW, cameraPreset = "global")
        val hk = createVNextAppState(screen = VScreen.OVERVIEW, cameraPreset = "hk")
        assertNotEquals("cameraPreset global != hk (semantic state)", global.globe.camera, hk.globe.camera)
        assertNotEquals(
            "region overview global frame SHA256 must differ from HK frame",
            sha256Of(renderApp(global)),
            sha256Of(renderApp(hk)),
        )
    }

    // ── 5. P0 回归：证据绝不触碰用户持久化 profile ───────────────────────────
    @Test
    fun evidenceIsolation_noPersistedProfileReadOrWrite() {
        val app = createVNextAppState(customTheme = "ocean").apply { openCardCustomization("card-cn-2") }
        renderApp(app)
        assertState(app.evidenceThemeId, "ocean", "evidence isolation")
        // Android 演示壳从不写 profile：渲染前后不存在 presentation/profile 持久化文件。
        val ctx = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        // 精确判定：presentation 持久化或 profile JSON（排除生产 app 的 profileInstalled 安装标记）。
        val profileFiles = ctx.filesDir.listFiles()?.filter {
            it.name.contains("presentation", ignoreCase = true) ||
                (it.name.contains("profile", ignoreCase = true) && it.name.endsWith(".json"))
        } ?: emptyList()
        assertTrue("evidence run must not create persisted profiles", profileFiles.isEmpty())
    }
}
