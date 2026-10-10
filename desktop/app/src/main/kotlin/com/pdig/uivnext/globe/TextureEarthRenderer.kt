package com.pdig.uivnext.globe

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import java.awt.image.BufferedImage
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * TextureEarthRenderer —— OFFLINE_TEXTURE_EARTH（PHASE 1C 默认）。
 *
 * 逐像素把 bundled equirect 纹理（albedo / night lights / cloud）经相机逆旋转
 * 投影到球面上：大陆纹理可辨（A）、海陆材质差异（B）、day-night terminator（C）、
 * 夜间城市照明（D）、云层（F）、specular ocean（G，叠加）。大气蓝 rim（E）、
 * 区域节点/弧线（H/I）、相机聚焦（J）由 EarthAtmosphere / EarthRegionOverlay 完成。
 * 纯本地纹理 + 确定性计算；零网络、零遥测。
 */

/** 纹理地球主体绘制（DrawScope 扩展；逐像素采样）。 */
internal fun DrawScope.drawTextureEarthBody(
    assets: EarthMaterialAssets,
    center: Offset,
    radius: Float,
    cam: GlobeCamera,
) {
    val rect = (radius * 2f).toInt().coerceAtLeast(8)
    val cx = center.x.toInt()
    val cy = center.y.toInt()
    val inv = 1f / radius
    val albedo = assets.albedo
    val night = assets.nightLights
    val clouds = assets.clouds
    val half = rect / 2
    val argb = IntArray(rect * rect) { 0 }
    for (y in 0 until rect) {
        val dy = (y - half) * inv
        val rowBase = y * rect
        for (x in 0 until rect) {
            val dx = (x - half) * inv
            val r2 = dx * dx + dy * dy
            if (r2 > 1f) continue
            val cz = sqrt(1f - r2)
            // 相机空间 → 世界
            val w = inverseRotatePoint(Vec3(dx, dy, cz), cam)
            if (w.z <= -0.05f) continue
            val day = dayFactor(w.x, w.y, w.z)
            val lat = asin(w.y.coerceIn(-1f, 1f))
            val lon = atan2(w.x, w.z)
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
            val nf = nightFactor(day)
            if (nf > 0.02f) {
                val nl = night.sample(u, v)
                val nr = (nl shr 16) and 0xFF
                val ng = (nl shr 8) and 0xFF
                val nb = nl and 0xFF
                val nw = nf * 0.92f
                rr = (rr * (1f - nw) + nr * nw).toInt()
                gg = (gg * (1f - nw) + ng * nw).toInt()
                bb = (bb * (1f - nw) + nb * nw).toInt()
            }
            // 云层（subtle）
            val cl = clouds.sample(u, v)
            val cb = ((cl shr 16) and 0xFF) / 255f
            val cw = 0.26f * cb * cb
            rr = (rr * (1f - cw) + 232f * cw).toInt()
            gg = (gg * (1f - cw) + 236f * cw).toInt()
            bb = (bb * (1f - cw) + 252f * cw).toInt()
            argb[rowBase + x] = (0xFF shl 24) or (rr shl 16) or (gg shl 8) or bb
        }
    }
    val awt = BufferedImage(rect, rect, BufferedImage.TYPE_INT_ARGB)
    awt.setRGB(0, 0, rect, rect, argb, 0, rect)
    val bitmap = awt.toComposeImageBitmap()
    drawImage(
        image = bitmap,
        dstOffset = IntOffset(cx - half, cy - half),
        dstSize = IntSize(rect, rect),
    )
}
