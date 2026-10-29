package com.pdig.uivnext.evidence

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.Density
import com.pdig.uivnext.VNextApp
import com.pdig.uivnext.createVNextAppState
import com.pdig.uivnext.model.PresentationProfile
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.persist.PresentationProfileStore
import com.pdig.uivnext.persist.resolveStudioProfile
import com.pdig.uivnext.ui.VAppState
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 *
 * 每个变体同时验证：
 *  (A) semantic state 不同且与 shot manifest 一致（selectedThemeId / projection / cameraPreset）；
 *  (B) render output 不同：SHA256(frameA) != SHA256(frameB)。
 *
 * 一律使用隔离、空的 PresentationProfileStore（绝不读用户主目录 ~/.pdig/presentation-profiles.json），
 * 并额外断言：若 composition 回写（app.evidenceThemeId）存在，则必须与确定性解析结果一致 ——
 * 这直接关闭 P0：用户持久化 profile 覆盖 evidence customTheme 导致 glass/city 同帧。
 */
class VisualVariantEvidenceContractTest {

    private val isolatedStore = PresentationProfileStore(
        File(kotlin.io.path.createTempDirectory("vnext-variant-evidence").toFile(), "profiles.json"),
    )

    // ───────────────────────────────────────────── 渲染 / 哈希
    private fun renderApp(app: VAppState, width: Int = 1280, height: Int = 720): BufferedImage {
        val scene = ImageComposeScene(width = width, height = height, density = Density(1f)) { VNextApp(app) }
        try {
            val bitmap = scene.render().toComposeImageBitmap()
            val w = bitmap.width
            val h = bitmap.height
            val img = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
            val arr = IntArray(w * h)
            bitmap.readPixels(arr, 0, 0, w, h, 0, w)
            img.setRGB(0, 0, w, h, arr, 0, w)
            return img
        } finally {
            scene.close()
        }
    }

    /** 帧的 SHA-256（对 PNG 编码后的字节；不是文件名）。 */
    private fun sha256Of(img: BufferedImage): String {
        val out = ByteArrayOutputStream()
        ImageIO.write(img, "png", out)
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        digest.update(out.toByteArray())
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /** Studio 变体帧：返回 (app, frame)，app.evidenceThemeId 为 composition 回写的实际主题。 */
    private fun studioFrame(screen: VScreen, theme: String): Pair<VAppState, BufferedImage> {
        val app = createVNextAppState(screen = screen, cameraPreset = "global", customTheme = theme, profileStore = isolatedStore)
        when (screen) {
            VScreen.CARD_CUSTOMIZATION -> app.openCardCustomization("card-cn-2")
            VScreen.NUMBER_CUSTOMIZATION -> app.openNumberCustomization("num-cn-1")
            else -> error("unsupported studio screen: $screen")
        }
        return app to renderApp(app)
    }

    private fun assertSelectedTheme(app: VAppState, targetType: String, targetId: String, expected: String) {
        val resolved = resolveStudioProfile(isolatedStore, targetType, targetId, expected) {
            PresentationProfile.defaultFor(targetType, targetId, "minimal")
        }.themeId
        assertEquals(expected, resolved, "deterministic resolve must equal manifest expected theme")
        val readback = app.evidenceThemeId
        if (readback != null) {
            assertEquals(expected, readback, "composition readback (actual selected theme) must equal expected theme")
        }
    }

    // ───────────────────────────────────────────── 1. Card Studio：glass != city
    @Test
    fun cardStudioGlassDiffersFromCity() {
        val glass = studioFrame(VScreen.CARD_CUSTOMIZATION, "glass")
        val city = studioFrame(VScreen.CARD_CUSTOMIZATION, "city")

        assertSelectedTheme(glass.first, "card", "card-cn-2", "glass")
        assertSelectedTheme(city.first, "card", "card-cn-2", "city")

        val shaGlass = sha256Of(glass.second)
        val shaCity = sha256Of(city.second)
        assertNotEquals(shaGlass, shaCity, "card studio glass frame SHA256 must differ from city frame")
    }

    // ───────────────────────────────────────────── 2. Number Studio：country != travel != recovery
    @Test
    fun numberStudioCountryTravelRecoveryPairwiseDistinct() {
        val themes = listOf("country", "travel", "recovery")
        val frames = themes.map { t -> t to studioFrame(VScreen.NUMBER_CUSTOMIZATION, t) }

        frames.forEach { (theme, pair) ->
            assertSelectedTheme(pair.first, "phoneNumber", "num-cn-1", theme)
        }
        val hashes = frames.map { (theme, pair) -> theme to sha256Of(pair.second) }.toMap()
        assertNotEquals(hashes["country"], hashes["travel"], "number studio country != travel")
        assertNotEquals(hashes["travel"], hashes["recovery"], "number studio travel != recovery")
        assertNotEquals(hashes["country"], hashes["recovery"], "number studio country != recovery")
    }

    // ───────────────────────────────────────────── 3. Change Phone：current != transition != after
    @Test
    fun changePhoneCurrentTransitionAfterAreDistinct() {
        val projections = listOf("current", "transition", "after")
        val frames = projections.map { p ->
            val app = createVNextAppState(screen = VScreen.CHANGE_PHONE, cameraPreset = "global", profileStore = isolatedStore)
            app.changeProjection = p
            p to (app to renderApp(app))
        }

        frames.forEach { (projection, pair) ->
            assertEquals(projection, pair.first.changeProjection, "projection semantic state must match manifest")
        }
        val hashes = frames.map { (projection, pair) -> projection to sha256Of(pair.second) }.toMap()
        assertNotEquals(hashes["current"], hashes["transition"], "change-phone current != transition")
        assertNotEquals(hashes["transition"], hashes["after"], "change-phone transition != after")
        assertNotEquals(hashes["current"], hashes["after"], "change-phone current != after")
    }

    // ───────────────────────────────────────────── 4. Region：global != HK（cameraPreset 语义 + 渲染）
    @Test
    fun regionGlobalDiffersFromHk() {
        val global = createVNextAppState(screen = VScreen.OVERVIEW, cameraPreset = "global", profileStore = isolatedStore)
        val hk = createVNextAppState(screen = VScreen.OVERVIEW, cameraPreset = "hk", profileStore = isolatedStore)

        assertNotEquals(global.globe.camera, hk.globe.camera, "cameraPreset global != hk (semantic state)")
        val shaGlobal = sha256Of(renderApp(global))
        val shaHk = sha256Of(renderApp(hk))
        assertNotEquals(shaGlobal, shaHk, "region overview global frame SHA256 must differ from HK frame")
    }

    // ───────────────────────────────────────────── 5. P0 回归：用户持久化 profile 不得静默污染 evidence
    @Test
    fun persistedProfileWinsOnlyWhenHarnessUsesUserStore() {
        // 隔离 store（证据 harness 用法）：override 确定性生效
        val isolated = PresentationProfileStore(
            File(kotlin.io.path.createTempDirectory("vnext-variant-pollution").toFile(), "profiles.json"),
        )
        assertEquals(
            "city",
            resolveStudioProfile(isolated, "card", "card-cn-2", "city") {
                PresentationProfile.defaultFor("card", "card-cn-2", "minimal")
            }.themeId,
        )

        // 用户 store（产品行为）：持久化偏好优先 —— 这正是 P0 同帧的机制
        val userStore = PresentationProfileStore(
            File(kotlin.io.path.createTempDirectory("vnext-variant-user").toFile(), "profiles.json"),
        )
        userStore.save(PresentationProfile.defaultFor("card", "card-cn-2", "glass"))
        assertEquals(
            "glass",
            resolveStudioProfile(userStore, "card", "card-cn-2", "city") {
                PresentationProfile.defaultFor("card", "card-cn-2", "minimal")
            }.themeId,
            "user persisted profile wins over override (product behavior) — evidence harness must never use the user store",
        )
    }
}
