package com.pdig.uivnext.globe

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import com.pdig.uivnext.theme.PdigV2Colors
import androidx.compose.ui.graphics.drawscope.clipPath
import kotlin.math.sqrt

/**
 * VectorEarthFallbackRenderer —— VECTOR_EARTH_FALLBACK / LOW_POWER_EARTH。
 *
 * 仅当 bundled 纹理缺失或显式 lowPower 时使用（**非默认**）：
 * bundled 简化海岸线 polygon + 程序化海洋/terminator/城市灯光/云层。
 * PHASE 1C 默认 = TextureEarthRenderer（OFFLINE_TEXTURE_EARTH）。
 */

/** 程序化海洋（径向 oceanBase→oceanDeep + 光照偏移）。 */
internal fun DrawScope.drawProceduralOcean(center: Offset, radius: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(PdigV2Colors.OceanBase, PdigV2Colors.OceanDeep, PdigV2Colors.CanvasDeep.copy(alpha = 0.95f)),
            center = Offset(center.x - radius * 0.28f, center.y - radius * 0.22f),
            radius = radius * 1.6f,
        ),
        radius = radius,
        center = center,
    )
}

/** 程序化大陆多边形（深度 + terminator 着色 + 纹理等高线）。 */
internal fun DrawScope.drawProceduralLand(center: Offset, radius: Float, cam: GlobeCamera) {
    for (coast in WORLD_COASTLINES) {
        val projected = coast.points.map { (lat, lon) ->
            project(latLonToVec(lat.toFloat(), lon.toFloat()), cam, radius, center.x, center.y)
        }
        val front = projected.filter { it.zDepth > -0.05f }
        if (front.size < 3) continue
        val path = Path()
        front.forEachIndexed { i, p -> if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y) }
        var nx = 0f; var ny = 0f; var nz = 0f
        for ((lat, lon) in coast.points) {
            val v = latLonToVec(lat.toFloat(), lon.toFloat())
            nx += v.x; ny += v.y; nz += v.z
        }
        val len = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(1e-6f)
        val day = dayFactor(nx / len, ny / len, nz / len)
        val avgZ = front.map { it.zDepth }.average().toFloat()
        val shade = ((avgZ + 1f) / 2f).coerceIn(0f, 1f)
        val t = (0.28f + 0.55f * day) * (0.35f + 0.65f * shade)
        val land = lerp(PdigV2Colors.LandBase, PdigV2Colors.LandHighlight, t.coerceIn(0f, 1f))
        drawPath(path, color = land)
        // 地形等高线（clipPath 保持在大陆内）
        clipPath(path) {
            val bands = 2 + (coast.points.size % 3)
            val minX = projected.minOfOrNull { it.x } ?: 0f
            val maxX = projected.maxOfOrNull { it.x } ?: 0f
            val midY = projected.map { it.y }.average().toFloat()
            for (b in 1 until bands) {
                val y = midY + (b - bands / 2f) * radius * 0.050f
                drawLine(
                    color = PdigV2Colors.LandTextureLo.copy(alpha = 0.30f * day + 0.05f),
                    start = Offset(minX, y),
                    end = Offset(maxX, y),
                    strokeWidth = 1.0f,
                )
            }
        }
        drawPath(
            path,
            color = PdigV2Colors.LandTextureHi.copy(alpha = 0.30f * day + 0.05f),
            style = Stroke(width = 1.4f),
        )
    }
}

/** 程序化暗面（terminator）。 */
internal fun DrawScope.drawProceduralTerminator(center: Offset, radius: Float, cam: GlobeCamera) {
    val sunScreen = project(SUN_DIR, cam, radius, center.x, center.y)
    val nightCenter = Offset(center.x - (sunScreen.x - center.x), center.y - (sunScreen.y - center.y))
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                PdigV2Colors.CanvasDeep.copy(alpha = 0.62f),
                PdigV2Colors.CanvasDeep.copy(alpha = 0.28f),
                Color.Transparent,
            ),
            center = nightCenter,
            radius = radius * 2.1f,
        ),
        radius = radius * 2.1f,
        center = nightCenter,
    )
    drawCircle(
        color = PdigV2Colors.TerminatorLight.copy(alpha = 0.08f),
        radius = radius,
        center = center,
        style = Stroke(width = radius * 0.012f),
    )
}

/** 程序化城市灯光（暗面增强）。 */
internal fun DrawScope.drawProceduralCityLights(center: Offset, radius: Float, cam: GlobeCamera) {
    for (city in WORLD_CITY_LIGHTS) {
        val v = latLonToVec(city.lat.toFloat(), city.lon.toFloat())
        val p = project(v, cam, radius, center.x, center.y)
        if (p.zDepth < -0.02f) continue
        val day = dayFactor(v.x, v.y, v.z)
        val night = nightFactor(day)
        val depth = p.zDepth.coerceIn(0f, 1f)
        val alpha = (0.30f + 0.60f * night) * (0.35f + 0.65f * depth)
        if (alpha < 0.06f) continue
        val pos = Offset(p.x, p.y)
        drawCircle(PdigV2Colors.NightCityLight.copy(alpha = alpha), radius = 1.6f + 1.2f * depth, center = pos)
        if (night > 0.30f) {
            drawCircle(PdigV2Colors.NightCityGlow.copy(alpha = alpha * 0.55f), radius = 4.5f + 2.0f * depth, center = pos)
        }
    }
}

/** 程序化云层（确定性软斑）。 */
internal fun DrawScope.drawProceduralClouds(center: Offset, radius: Float, cam: GlobeCamera) {
    for (i in 0 until 9) {
        val lat = ((i * 53.0) % 160.0) - 80.0
        val lon = ((i * 97.0) % 360.0) - 180.0
        val v = latLonToVec(lat.toFloat(), lon.toFloat())
        val p = project(v, cam, radius, center.x, center.y)
        if (p.zDepth < -0.05f) continue
        val day = dayFactor(v.x, v.y, v.z)
        val night = nightFactor(day)
        val alpha = (0.035f + 0.045f * night).coerceIn(0.03f, 0.10f)
        drawCircle(
            brush = Brush.radialGradient(listOf(PdigV2Colors.Cloud.copy(alpha = alpha), Color.Transparent)),
            radius = radius * (0.14f + 0.10f * (i % 3)),
            center = Offset(p.x, p.y),
        )
    }
}

/** LOW_POWER_FALLBACK：旧 plain sphere（深度着色，无纹理）。 */
internal fun DrawScope.drawLowPowerSphere(center: Offset, radius: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            listOf(PdigV2Colors.OceanBase, PdigV2Colors.SurfaceRaised, PdigV2Colors.CanvasDeep),
            center = Offset(center.x - radius * 0.35f, center.y - radius * 0.35f),
            radius = radius * 1.4f,
        ),
        radius = radius,
        center = center,
    )
}

/** 程序化地球（fallback 渲染主体）。 */
internal fun DrawScope.drawVectorEarthBody(center: Offset, radius: Float, cam: GlobeCamera, lowPower: Boolean) {
    drawProceduralOcean(center, radius)
    if (lowPower) {
        drawLowPowerSphere(center, radius)
    } else {
        drawProceduralLand(center, radius, cam)
        drawProceduralTerminator(center, radius, cam)
        drawProceduralCityLights(center, radius, cam)
        drawProceduralClouds(center, radius, cam)
    }
    drawOceanSpecular(center, radius, cam)
    drawLimbLight(center, radius, cam)
}
