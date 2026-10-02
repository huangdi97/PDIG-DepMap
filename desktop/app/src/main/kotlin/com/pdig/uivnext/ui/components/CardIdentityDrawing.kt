package com.pdig.uivnext.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pdig.uivnext.theme.PdigV2Colors

/**
 * PHASE 1F —— Card Identity 绘制层（模型/解析见 CardIdentity.kt）。
 * glow + motif 全部为 token 色、确定性程序化；无远程图片。
 */

/** 身份艺术层：glow + motif。 */
internal fun DrawScope.drawCardIdentity(identity: CardIdentity, w: Float, h: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            listOf(identity.glow.copy(alpha = 0.45f), Color.Transparent),
            center = Offset(w * 0.5f, h * 0.34f),
            radius = w * 0.72f,
        ),
        radius = w * 0.72f,
        center = Offset(w * 0.5f, h * 0.34f),
    )
    when (identity.motif) {
        CardMotif.CITY_NIGHT -> drawCityNightIdentity(w, h, identity.accent)
        CardMotif.CONTOUR -> drawContourIdentity(w, h, identity.accent)
        CardMotif.BRUSHED -> drawBrushedIdentity(w, h, identity.accent)
        CardMotif.SWEEP -> drawSweepIdentity(w, h, identity.accent)
        CardMotif.STARFIELD -> drawStarfieldIdentity(w, h, identity.accent)
        CardMotif.COPPER_RING -> drawCopperRingIdentity(w, h, identity.accent)
        CardMotif.RED_LINE -> drawRedLineIdentity(w, h, identity.accent)
        CardMotif.CORAL_BAND -> drawCoralBandIdentity(w, h, identity.accent)
        CardMotif.CHROMATIC -> drawChromaticIdentity(w, h)
        CardMotif.NONE -> Unit
    }
}

private fun DrawScope.drawCityNightIdentity(w: Float, h: Float, accent: Color) {
    // 底部城市辉光
    drawRect(
        Brush.verticalGradient(
            listOf(Color.Transparent, PdigV2Colors.NightCityLight.copy(alpha = 0.16f), PdigV2Colors.CanvasDeep.copy(alpha = 0.28f)),
            startY = h * 0.46f,
            endY = h,
        ),
    )
    // back 远景轮廓
    val backY = h * 0.58f
    val back = Path().apply {
        moveTo(0f, h); lineTo(0f, backY)
        lineTo(w * 0.07f, backY - h * 0.06f); lineTo(w * 0.14f, backY)
        lineTo(w * 0.22f, backY - h * 0.10f); lineTo(w * 0.31f, backY)
        lineTo(w * 0.38f, backY - h * 0.07f); lineTo(w * 0.47f, backY)
        lineTo(w * 0.55f, backY - h * 0.12f); lineTo(w * 0.63f, backY)
        lineTo(w * 0.71f, backY - h * 0.08f); lineTo(w * 0.80f, backY)
        lineTo(w * 0.88f, backY - h * 0.11f); lineTo(w, backY)
        lineTo(w, h); close()
    }
    drawPath(back, color = PdigV2Colors.CanvasDeep.copy(alpha = 0.35f))
    // mid 主楼群
    val midY = h * 0.66f
    val mid = Path().apply {
        moveTo(0f, h); lineTo(0f, midY)
        lineTo(w * 0.06f, midY - h * 0.16f); lineTo(w * 0.10f, midY - h * 0.16f); lineTo(w * 0.10f, midY)
        lineTo(w * 0.17f, midY - h * 0.22f); lineTo(w * 0.22f, midY - h * 0.22f); lineTo(w * 0.22f, midY)
        lineTo(w * 0.30f, midY - h * 0.13f); lineTo(w * 0.34f, midY - h * 0.13f); lineTo(w * 0.34f, midY)
        lineTo(w * 0.42f, midY - h * 0.28f); lineTo(w * 0.47f, midY - h * 0.28f); lineTo(w * 0.47f, midY)
        lineTo(w * 0.55f, midY - h * 0.18f); lineTo(w * 0.60f, midY - h * 0.18f); lineTo(w * 0.60f, midY)
        lineTo(w * 0.68f, midY - h * 0.25f); lineTo(w * 0.73f, midY - h * 0.25f); lineTo(w * 0.73f, midY)
        lineTo(w * 0.80f, midY - h * 0.14f); lineTo(w * 0.85f, midY - h * 0.14f); lineTo(w * 0.85f, midY)
        lineTo(w * 0.92f, midY - h * 0.20f); lineTo(w, midY - h * 0.20f)
        lineTo(w, h); close()
    }
    drawPath(mid, color = PdigV2Colors.SurfaceRaised.copy(alpha = 0.85f))
    // 窗簇
    val clusterX = listOf(w * 0.08f, w * 0.19f, w * 0.32f, w * 0.44f, w * 0.57f, w * 0.70f, w * 0.82f, w * 0.93f)
    val clusterH = listOf(h * 0.16f, h * 0.22f, h * 0.13f, h * 0.28f, h * 0.18f, h * 0.25f, h * 0.14f, h * 0.20f)
    clusterX.forEachIndexed { i, bx ->
        val top = midY - clusterH[i]
        val cols = 3 + (i % 2)
        val rows = 4 + (i % 3)
        for (c in 0 until cols) {
            for (r in 0 until rows) {
                val lit = ((i * 7 + c * 3 + r * 5) % 11) / 11f
                drawRect(
                    color = PdigV2Colors.NightCityLight.copy(alpha = 0.35f + lit * 0.5f),
                    topLeft = Offset(bx - w * 0.012f + c * (w * 0.010f), top + h * 0.015f + r * (h * 0.028f)),
                    size = Size(w * 0.007f, h * 0.012f),
                )
            }
        }
    }
    // front 前景剪影 + 近景暖光
    drawRect(color = PdigV2Colors.CanvasDeep.copy(alpha = 0.55f), topLeft = Offset(0f, h * 0.88f), size = Size(w, h * 0.12f))
    for (i in 0 until 8) {
        drawCircle(accent.copy(alpha = 0.5f), radius = 1.4f, center = Offset(w * (0.05f + i * 0.12f), h * 0.93f))
    }
}

private fun DrawScope.drawContourIdentity(w: Float, h: Float, accent: Color) {
    val cx = w * 0.74f
    val cy = h * 0.34f
    for (ring in 1..5) {
        drawCircle(
            color = PdigV2Colors.LandTextureHi.copy(alpha = 0.08f + ring * 0.035f),
            radius = w * (0.06f + ring * 0.055f),
            center = Offset(cx, cy),
            style = Stroke(width = 1.2f),
        )
    }
    val coast = Path().apply {
        moveTo(cx - w * 0.26f, cy + h * 0.10f)
        lineTo(cx - w * 0.20f, cy - h * 0.06f); lineTo(cx - w * 0.02f, cy - h * 0.14f)
        lineTo(cx + w * 0.16f, cy - h * 0.02f); lineTo(cx + w * 0.24f, cy + h * 0.12f)
        lineTo(cx + w * 0.10f, cy + h * 0.16f); lineTo(cx - w * 0.16f, cy + h * 0.12f)
        close()
    }
    drawPath(coast, color = PdigV2Colors.LandBase.copy(alpha = 0.55f))
    drawPath(coast, color = PdigV2Colors.LandTextureHi.copy(alpha = 0.4f), style = Stroke(width = 1.3f))
    drawCircle(accent.copy(alpha = 0.9f), radius = 3f, center = Offset(cx, cy))
}

private fun DrawScope.drawBrushedIdentity(w: Float, h: Float, accent: Color) {
    for (i in 1 until 22) {
        val y = h * i / 22f
        val alpha = 0.04f + (i % 4) * 0.018f
        drawLine(PdigV2Colors.TextSecondary.copy(alpha = alpha), Offset(0f, y), Offset(w, y), strokeWidth = 0.8f)
    }
    drawRect(
        Brush.linearGradient(
            listOf(Color.Transparent, accent.copy(alpha = 0.10f), accent.copy(alpha = 0.16f), accent.copy(alpha = 0.10f), Color.Transparent),
            start = Offset(0f, h * 0.28f),
            end = Offset(w, h * 0.62f),
        ),
    )
}

private fun DrawScope.drawSweepIdentity(w: Float, h: Float, accent: Color) {
    val band = Path().apply {
        moveTo(w * 0.34f, 0f)
        lineTo(w * 0.96f, h * 0.60f)
        lineTo(w * 0.74f, h)
        lineTo(w * 0.12f, h * 0.40f)
        close()
    }
    drawPath(band, color = accent.copy(alpha = 0.16f))
    drawLine(accent.copy(alpha = 0.5f), Offset(w * 0.34f, 0f), Offset(w * 0.74f, h), strokeWidth = 1.2f)
    drawCircle(accent.copy(alpha = 0.30f), radius = w * 0.18f, center = Offset(w * 0.80f, h * 0.24f))
}

private fun DrawScope.drawStarfieldIdentity(w: Float, h: Float, accent: Color) {
    drawCircle(
        brush = Brush.radialGradient(listOf(accent.copy(alpha = 0.55f), Color.Transparent)),
        radius = w * 0.5f,
        center = Offset(w * 0.30f, h * 0.20f),
    )
    for (i in 0 until 22) {
        val x = ((i * 37.5f) % 100f) / 100f * w
        val y = ((i * 23.7f) % 100f) / 100f * h
        drawCircle(PdigV2Colors.Star.copy(alpha = 0.30f + (i % 3) * 0.12f), radius = 1.0f + (i % 2), center = Offset(x, y))
    }
}

private fun DrawScope.drawCopperRingIdentity(w: Float, h: Float, accent: Color) {
    drawCircle(accent.copy(alpha = 0.14f), radius = w * 0.10f, center = Offset(w * 0.78f, h * 0.26f), style = Stroke(width = 1.6f))
    drawCircle(accent.copy(alpha = 0.28f), radius = w * 0.065f, center = Offset(w * 0.78f, h * 0.26f), style = Stroke(width = 1.1f))
    drawCircle(accent.copy(alpha = 0.9f), radius = 2.2f, center = Offset(w * 0.78f, h * 0.26f))
}

private fun DrawScope.drawRedLineIdentity(w: Float, h: Float, accent: Color) {
    drawLine(accent.copy(alpha = 0.75f), Offset(0f, h * 0.30f), Offset(w * 0.62f, h * 0.30f), strokeWidth = 2.2f)
    drawRect(
        color = accent.copy(alpha = 0.20f),
        topLeft = Offset(0f, h * 0.28f),
        size = Size(w * 0.20f, h * 0.05f),
    )
    drawLine(accent.copy(alpha = 0.35f), Offset(w * 0.66f, h * 0.30f), Offset(w * 0.92f, h * 0.30f), strokeWidth = 1f)
}

private fun DrawScope.drawCoralBandIdentity(w: Float, h: Float, accent: Color) {
    drawRect(
        Brush.horizontalGradient(listOf(accent.copy(alpha = 0.34f), accent.copy(alpha = 0.10f), accent.copy(alpha = 0.30f))),
        topLeft = Offset(0f, h * 0.74f),
        size = Size(w, h * 0.06f),
    )
    for (i in 0 until 5) {
        drawCircle(accent.copy(alpha = 0.45f), radius = 1.6f, center = Offset(w * (0.10f + 0.20f * i), h * 0.90f))
    }
}

private fun DrawScope.drawChromaticIdentity(w: Float, h: Float) {
    val prism = listOf(
        PdigV2Colors.PrimaryBright.copy(alpha = 0.16f),
        PdigV2Colors.Warning.copy(alpha = 0.12f),
        PdigV2Colors.Critical.copy(alpha = 0.10f),
        PdigV2Colors.Positive.copy(alpha = 0.10f),
    )
    prism.forEachIndexed { i, color ->
        val x0 = w * (0.20f + 0.16f * i)
        val path = Path().apply {
            moveTo(x0, 0f)
            lineTo(x0 + w * 0.12f, 0f)
            lineTo(x0 + w * 0.06f, h)
            lineTo(x0 - w * 0.06f, h)
            close()
        }
        drawPath(path, color = color)
    }
    drawLine(
        PdigV2Colors.PrimaryBright.copy(alpha = 0.5f),
        Offset(w * 0.86f, 0f),
        Offset(w * 0.86f, h),
        strokeWidth = 1.2f,
    )
}
