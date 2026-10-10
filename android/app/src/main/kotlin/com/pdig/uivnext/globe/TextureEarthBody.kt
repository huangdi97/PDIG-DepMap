package com.pdig.uivnext.globe

import android.graphics.Bitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * TextureEarthBody —— Android 移植的逐像素纹理地球主体（与桌面 TextureEarthRenderer 同算法）。
 *
 * 把 bundled equirect 纹理经相机逆旋转投影到球面：大陆/海陆材质（A）、day-night terminator（B）、
 * 夜间城市照明（C）、云层（D）。大气 rim / 网格 / 弧线 / 锚点由 VNextGlobe 现有层叠加。
 *
 * 性能：渲染在 Dispatchers.Default 后台执行，结果 Bitmap 按相机（yaw/pitch/zoom 量化）缓存，
 * 避免每帧重算；主线程只负责 drawBitmap。
 */

/** 世界空间太阳方向（固定；与桌面 EarthLighting.SUN_DIR 同源）。 */
internal val SUN_DIR: Vec3 = latLonToVec(14f, -12f)
/** Fixed display-light reference for R9 Preview, NOT a live solar ephemeris. */
internal val R9_REFERENCE_SUN_DIR: Vec3 = latLonToVec(18f, 92f)

internal fun dayFactor(nx: Float, ny: Float, nz: Float, sunDir: Vec3 = SUN_DIR): Float {
    val lit = (nx * sunDir.x + ny * sunDir.y + nz * sunDir.z).coerceIn(-1f, 1f)
    return ((lit + 0.30f) / 1.30f).coerceIn(0f, 1f)
}

internal fun nightFactor(day: Float): Float = (1f - day) * (1f - day)

internal fun litScale(day: Float): Float = 0.73f + 0.27f * day

/** 渲染球形地球主体到 [rect]×[rect] Bitmap（含 night/cloud 层，按 assets 有无取舍）。 */
internal fun renderEarthBody(
    assets: EarthMaterialAssets,
    rect: Int,
    centerX: Int,
    centerY: Int,
    radiusPx: Float,
    cam: GlobeCamera,
    sunDir: Vec3 = SUN_DIR,
    previewReferenceLift: Boolean = false,
): Bitmap {
    val albedo = assets.albedo
    val night = assets.nightLights
    val clouds = assets.clouds
    val half = rect / 2
    // [rect] is deliberately capped (HIGH <= 512) even when the on-screen Globe radius is larger.
    // Sampling must therefore be normalized in bitmap space, not by the on-screen radiusPx.
    // Using radiusPx here under-fills the unit disc after the cap and, together with world-z culling,
    // produces the visibly faceted/octagonal Now Globe seen in Round4 runtime pixels.
    val sampleRadius = half.toFloat().coerceAtLeast(1f)
    val inv = 1f / sampleRadius
    // Camera-space halfway vector: preview ocean sheen is narrow and sun-dependent.
    val sunOnCamera = rotatePoint(sunDir, cam)
    val halfLength = sqrt(sunOnCamera.x * sunOnCamera.x +
        sunOnCamera.y * sunOnCamera.y +
        (sunOnCamera.z + 1f) * (sunOnCamera.z + 1f)).coerceAtLeast(0.0001f)
    val halfX = sunOnCamera.x / halfLength
    val halfY = sunOnCamera.y / halfLength
    val halfZ = (sunOnCamera.z + 1f) / halfLength
    val argb = IntArray(rect * rect)
    for (y in 0 until rect) {
        val dy = (y - half) * inv
        val rowBase = y * rect
        for (x in 0 until rect) {
            val dx = (x - half) * inv
            val r2 = dx * dx + dy * dy
            if (r2 > 1f) continue
            val cz = sqrt(1f - r2)
            // Screen-space (dx,dy,cz) already represents the visible camera hemisphere.
            // Rotate it back into world space only for texture/lighting lookup. Do not cull by world z:
            // world z is longitude-facing orientation, not camera visibility.
            val w = inverseRotatePoint(Vec3(dx, dy, cz), cam)
            val day = dayFactor(w.x, w.y, w.z, sunDir)
            val lat = asin(w.y.coerceIn(-1f, 1f))
            val lon = earthLongitudeRad(w)
            val u = ((lon / PI.toFloat()) + 1f) / 2f
            val v = 0.5f - lat / PI.toFloat()
            val alb = albedo.sample(u, v)
            val ar = (alb shr 16) and 0xFF
            val ag = (alb shr 8) and 0xFF
            val ab = alb and 0xFF
            val lit = litScale(day)
            var rr = (ar * lit).toInt()
            var gg = (ag * lit).toInt()
            var bb = (ab * lit).toInt()
            // 夜间城市照明（暗面增强）
            if (night != null) {
                val nf = nightFactor(day)
                if (nf > 0.02f) {
                    val nl = night.sample(u, v)
                    val nr = (nl shr 16) and 0xFF
                    val ng = (nl shr 8) and 0xFF
                    val nb = nl and 0xFF
                    val nw = nf * 0.16f
                    rr = (rr * (1f - nw) + nr * nw).toInt()
                    gg = (gg * (1f - nw) + ng * nw).toInt()
                    bb = (bb * (1f - nw) + nb * nw).toInt()
                }
            }
            // 云层（subtle）
            if (clouds != null) {
                val cl = clouds.sample(u, v)
                val cb = ((cl shr 16) and 0xFF) / 255f
                val cw = 0.26f * cb * cb
                rr = (rr * (1f - cw) + 232f * cw).toInt()
                gg = (gg * (1f - cw) + 236f * cw).toInt()
                bb = (bb * (1f - cw) + 252f * cw).toInt()
            }
            // Light-first World: retain source albedo/terminator, but lift dark pixels
            // and give water/atmosphere a restrained cool-blue photographic grade.
            val blueGrade = 0.10f + 0.10f * (1f - day)
            rr = (rr * (1f - blueGrade) + 68f * blueGrade).toInt().coerceIn(0, 255)
            gg = (gg * (1f - blueGrade) + 136f * blueGrade).toInt().coerceIn(0, 255)
            bb = (bb * (1f - blueGrade) + 238f * blueGrade).toInt().coerceIn(0, 255)
            // The NASA albedo is intentionally subdued; R9 light-first Preview needs
            // a brighter *presentation* grade. Keep production exactly unchanged.
            // The grade is applied after day/night/cloud compositing; it neither
            // changes factual dependency edges nor fakes a material texture.
            if (previewReferenceLift) {
                val lift = (0.16f + 0.11f * day).coerceIn(0f, 0.35f)
                rr = liftChannel(rr, 195, lift)
                gg = liftChannel(gg, 225, lift)
                bb = liftChannel(bb, 250, lift)
                val nDotH = (dx * halfX + dy * halfY + cz * halfZ).coerceIn(0f, 1f)
                val glint = oceanSpecularStrength(ar, ag, ab, nDotH, day)
                if (glint > 0f) {
                    rr = liftChannel(rr, 250, glint)
                    gg = liftChannel(gg, 251, glint)
                    bb = liftChannel(bb, 255, glint)
                }
                val edge = (1f - cz).let { it * it } * .19f
                rr = liftChannel(rr, 105, edge)
                gg = liftChannel(gg, 204, edge)
                bb = liftChannel(bb, 255, edge)
            }
            argb[rowBase + x] = (0xFF shl 24) or (rr shl 16) or (gg shl 8) or bb
        }
    }
    val bmp = Bitmap.createBitmap(rect, rect, Bitmap.Config.ARGB_8888)
    bmp.setPixels(argb, 0, rect, 0, 0, rect, rect)
    return bmp
}

/** Preview-only photographic sheen: albedo classifies water; this never fabricates relationships. */
internal fun oceanSpecularStrength(red: Int, green: Int, blue: Int, nDotH: Float, day: Float): Float {
    if (blue <= red + 15 || blue <= green + 3 || green < red * .75f) return 0f
    val n2 = nDotH.coerceIn(0f, 1f).let { it * it }
    val n4 = n2 * n2
    val n8 = n4 * n4
    val n16 = n8 * n8
    val n32 = n16 * n16
    return (.33f * day.coerceIn(0f, 1f) * n32 * n32).coerceIn(0f, .33f)
}

/** Pure per-channel reference grade; out-of-gamut values never leak to bitmap ARGB. */
internal fun liftChannel(value: Int, target: Int, strength: Float): Int =
    (value * (1f - strength) + target * strength).toInt().coerceIn(0, 255)

/** DrawScope 扩展：把已渲染的地球主体 Bitmap 画到屏幕（圆心对齐）。 */
internal fun DrawScope.drawEarthBitmap(
    bmp: Bitmap,
    centerX: Int,
    centerY: Int,
    targetDiameterPx: Int = bmp.width,
) {
    // The reusable frame can originate from another canvas with a different diameter.
    // Scale it to the CURRENT sphere instead of rendering an incorrectly sized bitmap.
    val half = targetDiameterPx.coerceAtLeast(1) / 2
    drawContext.canvas.nativeCanvas.drawBitmap(
        bmp,
        null,
        android.graphics.Rect(centerX - half, centerY - half, centerX + half, centerY + half),
        null,
    )
}

/** 相机缓存键：量化到 0.25° 与 0.02 zoom，idle 旋转 ~0.8°/s 时约每 0.3s 重渲一次。 */
internal fun cameraCacheKey(cam: GlobeCamera, rect: Int): String {
    val yaw = ((cam.yawDeg / 0.25f).toInt() * 0.25f)
    val pitch = ((cam.pitchDeg / 0.25f).toInt() * 0.25f)
    val zoom = ((cam.zoom / 0.02f).toInt() * 0.02f)
    return "$rect|${"%.2f".format(yaw)}|${"%.2f".format(pitch)}|${"%.2f".format(zoom)}"
}
