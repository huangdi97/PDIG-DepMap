package com.pdig.uivnext.globe

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.model.RegionPresentation
import com.pdig.uivnext.theme.PdigV2Colors

/**
 * EarthRegionOverlay —— 地球上方空间覆盖层（PHASE 1C）：
 * 星点、极淡经纬网格、跨区真实关系弧、地区锚点 + label chip。
 * 全部确定性、离线。
 */

/** 确定性星点点阵（固定散列；截图可复现）。 */
internal fun DrawScope.drawStars() {
    for (i in 0 until 96) {
        val x = ((i * 137.508f) % 360f) / 360f * size.width
        val y = ((i * 89.3f) % 200f) / 200f * size.height
        val a = 0.10f + 0.28f * ((i * 7) % 10) / 10f
        drawCircle(PdigV2Colors.Star.copy(alpha = a), radius = 0.5f + (i % 3) * 0.3f, center = Offset(x, y))
    }
    for (i in 0 until 5) {
        val x = ((i * 211.3f) % 360f) / 360f * size.width
        val y = ((i * 173.7f) % 200f) / 200f * size.height
        drawCircle(PdigV2Colors.Star.copy(alpha = 0.55f), radius = 1.3f, center = Offset(x, y))
    }
}

/** 极淡经纬网格（空间定位；非 HUD）。 */
internal fun DrawScope.drawGraticule(center: Offset, radius: Float, cam: GlobeCamera) {
    val gridColor = PdigV2Colors.TextMuted.copy(alpha = 0.05f)
    val (parallels, meridians) = graticuleLines(30)
    for (p in parallels) {
        drawArcPath(center, radius, cam, greatCircleSamples(p.first, p.second, p.first + 20f, p.second + 20f, 24), gridColor, widthPx = 0.8f)
    }
    for (m in meridians) {
        drawArcPath(center, radius, cam, greatCircleSamples(m.first, m.second, m.first + 10f, m.second, 24), gridColor, widthPx = 0.8f)
        drawArcPath(center, radius, cam, greatCircleSamples(m.first, m.second, m.first + 10f, m.second + 10f, 24), gridColor, widthPx = 0.8f)
    }
}

/** 跨区真实关系弧线（只有真实 cross-region 关系；绝不装饰性连线）。 */
internal fun DrawScope.drawArcs(
    center: Offset,
    radius: Float,
    cam: GlobeCamera,
    regions: List<RegionPresentation>,
    arcingPairs: List<Pair<String, String>>,
    selectedRegion: String?,
) {
    for ((aCode, bCode) in arcingPairs) {
        val a = regions.firstOrNull { it.regionCode == aCode } ?: continue
        val b = regions.firstOrNull { it.regionCode == bCode } ?: continue
        val selectedLink = selectedRegion == aCode || selectedRegion == bCode
        val path = greatCircleSamples(
            a.latitude.toFloat(), a.longitude.toFloat(),
            b.latitude.toFloat(), b.longitude.toFloat(), 48,
        )
        drawArcPath(
            center, radius, cam, path,
            if (selectedLink) PdigV2Colors.ArcActive else PdigV2Colors.ArcQuiet,
            widthPx = if (selectedLink) 2.0f else 1.2f,
        )
    }
}

/** 地区锚点 + label chip（region code + 名称 + 计数；不用 emoji）。 */
internal fun DrawScope.drawAnchors(
    center: Offset,
    radius: Float,
    cam: GlobeCamera,
    regions: List<RegionPresentation>,
    selectedRegion: String?,
    hoveredRegion: RegionPresentation?,
    showRegionLabels: Boolean,
    textMeasurer: TextMeasurer,
) {
    for (r in regions) {
        val projected = anchorScreen(r, cam, center, radius) ?: continue
        val depth = projected.second
        val isSelected = selectedRegion == r.regionCode
        val isHovered = hoveredRegion?.regionCode == r.regionCode
        val anchorR = radius * (0.020f + 0.014f * depth) * (if (isSelected) 1.7f else 1f)
        drawCircle(
            color = if (isSelected || isHovered) PdigV2Colors.RegionNodeHi else PdigV2Colors.RegionNodeLo,
            radius = anchorR,
            center = projected.first,
        )
        if (isSelected || isHovered) {
            drawCircle(
                color = PdigV2Colors.PrimaryBright.copy(alpha = 0.40f),
                radius = anchorR * 2.2f,
                center = projected.first,
            )
        }
        if (isSelected || isHovered || r.attentionCount > 0 || showRegionLabels) {
            drawRegionLabel(projected.first, r, isSelected, textMeasurer)
        }
    }
}

private fun DrawScope.drawRegionLabel(
    anchor: Offset,
    region: RegionPresentation,
    isSelected: Boolean,
    textMeasurer: TextMeasurer,
) {
    val title = "${region.regionCode} ${region.displayName}"
    val counts = "${region.cardCount} 张卡 · ${region.phoneCount} 个号码"
    val titleStyle = TextStyle(color = PdigV2Colors.TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.W600, lineHeight = 14.sp)
    val countStyle = TextStyle(color = PdigV2Colors.TextSecondary, fontSize = 10.sp, lineHeight = 13.sp)
    val maxW = (size.width * 0.30f).toInt().coerceAtLeast(120)
    val t = textMeasurer.measure(AnnotatedString(title), style = titleStyle, constraints = Constraints(maxWidth = maxW))
    val c = textMeasurer.measure(AnnotatedString(counts), style = countStyle, constraints = Constraints(maxWidth = maxW))
    val chipW = maxOf(t.size.width, c.size.width) + 14f
    val chipH = t.size.height + c.size.height + 10f
    var pos = Offset(anchor.x + 12f, anchor.y - chipH / 2f)
    if (pos.x + chipW > size.width) pos = Offset(anchor.x - 12f - chipW, pos.y)
    if (pos.x < 2f) pos = Offset(2f, pos.y)
    if (pos.y < 2f) pos = Offset(pos.x, 2f)
    if (pos.y + chipH > size.height - 2f) pos = Offset(pos.x, size.height - chipH - 2f)
    drawRoundRect(
        color = PdigV2Colors.CanvasDeep.copy(alpha = 0.78f),
        topLeft = pos,
        size = Size(chipW, chipH),
        cornerRadius = CornerRadius(8f),
    )
    drawRoundRect(
        color = PdigV2Colors.BorderSubtle,
        topLeft = pos,
        size = Size(chipW, chipH),
        cornerRadius = CornerRadius(8f),
        style = Stroke(width = 1f),
    )
    drawText(
        t,
        topLeft = Offset(pos.x + 7f, pos.y + 5f),
        color = if (isSelected) PdigV2Colors.PrimaryBright else PdigV2Colors.TextPrimary,
    )
    drawText(c, topLeft = Offset(pos.x + 7f, pos.y + 5f + t.size.height))
}

/** 弧线段：仅绘制前半球交界、按深度淡出。 */
internal fun DrawScope.drawArcPath(
    center: Offset,
    radius: Float,
    cam: GlobeCamera,
    path: List<Vec3>,
    color: Color,
    widthPx: Float = 1.2f,
) {
    var prev = project(path.first(), cam, radius, center.x, center.y)
    var drawing = prev.zDepth > -0.05f
    for (v in path.drop(1)) {
        val cur = project(v, cam, radius, center.x, center.y)
        val visible = cur.zDepth > -0.05f
        if (visible || drawing) {
            val alpha = (cur.zDepth.coerceIn(0f, 1f) * 0.9f + 0.1f).coerceIn(0.08f, 1f)
            drawLine(
                color = color.copy(alpha = color.alpha * alpha),
                start = Offset(prev.x, prev.y),
                end = Offset(cur.x, cur.y),
                strokeWidth = widthPx,
            )
        }
        drawing = visible
        prev = cur
    }
}
