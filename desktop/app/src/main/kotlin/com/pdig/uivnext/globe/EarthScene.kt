package com.pdig.uivnext.globe

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.IntSize
import com.pdig.uivnext.model.RegionPresentation
import com.pdig.uivnext.theme.PdigV2Colors
import kotlin.math.sqrt

/**
 * 真实地球感 vector Earth —— 行星内容层（G1 SIGNATURE）。
 *
 * 分层（由 drawEarth 编排）：
 *  1. 海洋（oceanBase/oceanDeep 径向，光照偏移）；
 *  2. 大陆多边形（bundled WorldCoastlines，按深度+terminator 着色）；
 *  3. 夜间暗面（terminator：太阳反向点径向暗化）；
 *  4. 夜间城市灯光（WorldCityLights，暗面增强/亮面淡出）。
 *
 * 星点/大气 rim/网格/弧线/锚点与 label 在 EarthOverlay.kt（外层叠加）。
 * 旧 plain sphere 保留为 LOW_POWER_FALLBACK（lowPower=true → drawLowPowerSphere）。
 * 全部使用 DESIGN_TOKENS.json 色值；零网络、零远程 tiles。
 */

/** 世界空间太阳方向（固定；terminator 由此产生）。 */
internal val SUN_DIR = latLonToVec(14f, -12f)

internal fun globeMetrics(size: IntSize, zoom: Float): Pair<Offset, Float> {
    val d = minOf(size.width, size.height).toFloat()
    val radius = d * 0.36f * zoom
    return Offset(size.width / 2f, size.height / 2f) to radius
}
internal fun globeMetrics(size: Size, zoom: Float): Pair<Offset, Float> {
    val d = minOf(size.width, size.height)
    val radius = d * 0.36f * zoom
    return Offset(size.width / 2f, size.height / 2f) to radius
}

/** 经纬度锚点 → 屏幕坐标（仅前半球）。 */
internal fun anchorScreen(
    region: RegionPresentation,
    cam: GlobeCamera,
    center: Offset,
    radius: Float,
): Pair<Offset, Float>? {
    val p = project(latLonToVec(region.latitude.toFloat(), region.longitude.toFloat()), cam, radius, center.x, center.y)
    if (p.zDepth <= 0f) return null
    return Offset(p.x, p.y) to p.zDepth
}

internal fun hitTolerance(radius: Float): Float = (radius * 0.05f).coerceIn(10f, 18f)

internal fun DrawScope.drawEarth(
    camera: GlobeCamera,
    regions: List<RegionPresentation>,
    arcingPairs: List<Pair<String, String>>,
    selectedRegion: String?,
    hoveredRegion: RegionPresentation?,
    lowPower: Boolean,
    showRegionLabels: Boolean,
    textMeasurer: TextMeasurer,
) {
    val (center, radius) = globeMetrics(size, camera.zoom)
    drawStars()
    // L1 大气辉光（球后光晕）
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(PdigV2Colors.AtmosphereInner.copy(alpha = 0.55f), PdigV2Colors.AtmosphereOuter),
            center = center,
            radius = radius * 1.7f,
        ),
        radius = radius * 1.7f,
        center = center,
    )
    val sphere = Path().apply {
        addOval(Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius))
    }
    clipPath(sphere) {
        drawOcean(center, radius)
        if (lowPower) {
            drawLowPowerSphere(center, radius)
        } else {
            drawLand(center, radius, camera)
            drawNightTerminator(center, radius, camera)
            drawCityLights(center, radius, camera)
            drawClouds(center, radius, camera)
        }
        drawGraticule(center, radius, camera)
        drawArcs(center, radius, camera, regions, arcingPairs, selectedRegion)
    }
    drawAtmosphereRim(center, radius)
    drawAnchors(center, radius, camera, regions, selectedRegion, hoveredRegion, showRegionLabels, textMeasurer)
}

/** 海洋：径向 oceanBase→oceanDeep，光照偏移产生立体感。 */
private fun DrawScope.drawOcean(center: Offset, radius: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(PdigV2Colors.OceanBase, PdigV2Colors.OceanDeep, PdigV2Colors.CanvasDeep.copy(alpha = 0.95f)),
            center = Offset(center.x - radius * 0.28f, center.y - radius * 0.22f),
            radius = radius * 1.6f,
        ),
        radius = radius,
        center = center,
    )
    // 镜面海洋高光：太阳受光侧的椭圆亮斑（oceanSpecular；克制低 alpha，非 HUD）
    val litSide = Offset(center.x + radius * 0.20f, center.y + radius * 0.08f)
    drawOval(
        brush = Brush.radialGradient(
            listOf(PdigV2Colors.OceanSpecular.copy(alpha = 0.16f), Color.Transparent),
            center = litSide,
            radius = radius * 0.66f,
        ),
        topLeft = Offset(litSide.x - radius * 0.66f, litSide.y - radius * 0.32f),
        size = Size(radius * 1.32f, radius * 0.64f),
    )
    // 受光缘微弱亮度（direction light 的 diffuse 感）
    drawOval(
        brush = Brush.radialGradient(
            listOf(PdigV2Colors.OceanBase.copy(alpha = 0.30f), Color.Transparent),
            center = Offset(center.x - radius * 0.45f, center.y - radius * 0.40f),
            radius = radius * 0.8f,
        ),
        topLeft = Offset(center.x - radius * 1.25f, center.y - radius * 1.2f),
        size = Size(radius * 1.6f, radius * 1.6f),
    )
}

/** 大陆多边形：按 平均深度 + 平均光照（terminator）着色。 */
private fun DrawScope.drawLand(center: Offset, radius: Float, cam: GlobeCamera) {
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
        val lit = (nx / len * SUN_DIR.x + ny / len * SUN_DIR.y + nz / len * SUN_DIR.z).coerceIn(-1f, 1f)
        val light = ((lit + 1f) / 2f).coerceIn(0f, 1f)
        val avgZ = front.map { it.zDepth }.average().toFloat()
        val shade = ((avgZ + 1f) / 2f).coerceIn(0f, 1f)
        val t = (0.28f + 0.55f * light) * (0.35f + 0.65f * shade)
        val land = lerp(PdigV2Colors.LandBase, PdigV2Colors.LandHighlight, t.coerceIn(0f, 1f))
        drawPath(path, color = land)
        // 大陆纹理：沿陆块内部的等高线带（landTextureLo/Hi 低 alpha，clipPath 保持在大陆内）
        clipPath(path) {
            val bands = 2 + (coast.points.size % 3)
            val minX = projected.minOfOrNull { it.x } ?: 0f
            val maxX = projected.maxOfOrNull { it.x } ?: 0f
            val midY = projected.map { it.y }.average().toFloat()
            for (b in 1 until bands) {
                val y = midY + (b - bands / 2f) * radius * 0.050f
                drawLine(
                    color = PdigV2Colors.LandTextureLo.copy(alpha = 0.30f * light + 0.05f),
                    start = Offset(minX, y),
                    end = Offset(maxX, y),
                    strokeWidth = 1.0f,
                )
            }
        }
        // 海岸亮缘（landTextureHi 高光描边，克制）
        drawPath(
            path,
            color = PdigV2Colors.LandTextureHi.copy(alpha = 0.30f * light + 0.05f),
            style = Stroke(width = 1.4f),
        )
    }
}

/** 暗面（terminator）：从太阳反向点向外径向暗化；暗面最暗、亮面渐亮。 */
private fun DrawScope.drawNightTerminator(center: Offset, radius: Float, cam: GlobeCamera) {
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
    // 晨昏线亮边（terminatorLight token，极淡）
    drawCircle(
        color = PdigV2Colors.TerminatorLight.copy(alpha = 0.08f),
        radius = radius,
        center = center,
        style = Stroke(width = radius * 0.012f),
    )
}

/** 夜间城市灯光：暗面增强、亮面淡出；光晕仅暗面。 */
private fun DrawScope.drawCityLights(center: Offset, radius: Float, cam: GlobeCamera) {
    for (city in WORLD_CITY_LIGHTS) {
        val v = latLonToVec(city.lat.toFloat(), city.lon.toFloat())
        val p = project(v, cam, radius, center.x, center.y)
        if (p.zDepth < -0.02f) continue
        val lit = (v.x * SUN_DIR.x + v.y * SUN_DIR.y + v.z * SUN_DIR.z).coerceIn(-1f, 1f)
        val night = ((1f - lit) / 2f).coerceIn(0f, 1f)
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
/**
 * 云层/噪声层：确定性软云斑（cloud token，低 alpha），暗面略浓；
 * 程序化生成、零远程资源；保持可复现（固定散列）。
 */
private fun DrawScope.drawClouds(center: Offset, radius: Float, cam: GlobeCamera) {
    for (i in 0 until 9) {
        val lat = ((i * 53.0) % 160.0) - 80.0
        val lon = ((i * 97.0) % 360.0) - 180.0
        val v = latLonToVec(lat.toFloat(), lon.toFloat())
        val p = project(v, cam, radius, center.x, center.y)
        if (p.zDepth < -0.05f) continue
        val lit = v.x * SUN_DIR.x + v.y * SUN_DIR.y + v.z * SUN_DIR.z
        val night = ((1f - lit) / 2f).coerceIn(0f, 1f)
        val alpha = (0.035f + 0.045f * night).coerceIn(0.03f, 0.10f)
        drawCircle(
            brush = Brush.radialGradient(listOf(PdigV2Colors.Cloud.copy(alpha = alpha), Color.Transparent)),
            radius = radius * (0.14f + 0.10f * (i % 3)),
            center = Offset(p.x, p.y),
        )
    }
}


/** LOW_POWER_FALLBACK：旧 plain sphere（深度着色，无纹理；不再作为默认）。 */
private fun DrawScope.drawLowPowerSphere(center: Offset, radius: Float) {
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
