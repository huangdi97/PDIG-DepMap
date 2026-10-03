package com.pdig.uivnext.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.pdig.uivnext.theme.PdigV2Colors

/**
 * Card Identity 几何绘制（L2 版式带 / L3 issuer motif / L4 材质 / L5 Studio 主题 artwork）。
 *
 * 与 CardIdentityRenderer.kt 分离：renderer 负责组合与内容排版，这里只画几何——
 * 保持单一职责、文件体积受控（brief §33：禁止把 AssetSurfaces 堆成 God file）。
 * 全部 procedural / deterministic；零网络、零远程图片。
 */

/** 按 profile + 主题绘制卡面几何（顺序固定：版式带 → motif → 材质 → 主题 artwork）。 */
internal fun DrawScope.drawCardArtwork(profile: CardIdentityProfile, theme: CardThemeInfo) {
    drawLayoutBand(profile)
    drawMotif(profile)
    drawMaterial(profile)
    drawThemeArtwork(theme)
}

// ── L2 版式带 ─────────────────────────────────────────────────────────────

private fun DrawScope.drawLayoutBand(profile: CardIdentityProfile) {
    val band = profile.accent.copy(alpha = 0.55f)
    when (profile.layout) {
        CardLayout.EDGE_BAND -> drawRect(band, topLeft = Offset(size.width * 0.03f, 0f), size = Size(size.width * 0.015f, size.height))
        CardLayout.TOP_LIGHT -> drawRect(band, topLeft = Offset(0f, 0f), size = Size(size.width, size.height * 0.035f))
        CardLayout.SPLIT_BAND -> drawLine(band, start = Offset(0f, size.height * 0.5f), end = Offset(size.width, size.height * 0.5f), strokeWidth = 2.dp.toPx())
        CardLayout.STANDARD -> Unit
    }
}

// ── L3 issuer motif（8 个冻结 issuer 每种均有独立几何）──────────────────────

private fun DrawScope.drawMotif(profile: CardIdentityProfile) {
    val a = profile.accent
    val w = size.width
    val h = size.height
    when (profile.motif) {
        CardMotif.RING -> {
            drawCircle(a.copy(alpha = 0.28f), radius = w * 0.26f, center = Offset(w * 0.8f, h * 0.28f), style = Stroke(width = 7.dp.toPx()))
            drawCircle(a.copy(alpha = 0.85f), radius = w * 0.16f, center = Offset(w * 0.8f, h * 0.28f), style = Stroke(width = 2.dp.toPx()))
        }
        CardMotif.RED_LINE -> {
            drawLine(a.copy(alpha = 0.9f), start = Offset(w * 0.02f, h * 0.62f), end = Offset(w * 0.98f, h * 0.38f), strokeWidth = 3.dp.toPx())
            drawLine(a.copy(alpha = 0.28f), start = Offset(w * 0.02f, h * 0.68f), end = Offset(w * 0.98f, h * 0.44f), strokeWidth = 1.dp.toPx())
        }
        CardMotif.CONTOUR -> {
            listOf(0.18f, 0.3f, 0.42f).forEach { r ->
                drawOval(a.copy(alpha = 0.30f), topLeft = Offset(w * 0.55f - w * r, h * 0.75f - h * r * 0.32f), size = Size(w * r * 2f, h * r * 0.64f), style = Stroke(width = 1.4.dp.toPx()))
            }
        }
        CardMotif.WARM_WINDOW -> {
            val cw = w * 0.05f
            val ch = h * 0.09f
            for (row in 0 until 3) {
                for (col in 0 until 5) {
                    val x = w * 0.66f + col * (cw + w * 0.012f)
                    val y = h * 0.24f + row * (ch + h * 0.02f)
                    if (x + cw > w * 0.97f) continue
                    drawRect(a.copy(alpha = if ((row + col) % 3 == 0) 0.9f else 0.38f), topLeft = Offset(x, y), size = Size(cw, ch))
                }
            }
        }
        CardMotif.GEO_CONTOUR -> {
            listOf(0.14f, 0.24f, 0.34f, 0.44f).forEachIndexed { i, r ->
                drawCircle(a.copy(alpha = 0.55f - i * 0.1f), radius = w * r, center = Offset(w * 0.72f, h * 0.7f), style = Stroke(width = 1.2.dp.toPx()))
            }
        }
        CardMotif.CORAL_ARC -> {
            drawArc(a.copy(alpha = 0.9f), startAngle = 180f, sweepAngle = 150f, useCenter = false, topLeft = Offset(-w * 0.15f, h * 0.5f), size = Size(w * 0.62f, h * 0.9f), style = Stroke(width = 4.dp.toPx()))
        }
        CardMotif.DISPERSION -> {
            repeat(9) { i ->
                val x = w * (0.1f + (i * 0.093f) % 0.8f)
                val y = h * (0.12f + (i * 0.071f) % 0.7f)
                drawCircle(a.copy(alpha = 0.28f - (i % 3) * 0.07f), radius = w * (0.018f + (i % 3) * 0.009f), center = Offset(x, y))
            }
        }
        CardMotif.BRUSHED_STRUCTURE -> {
            drawRect(a.copy(alpha = 0.16f), topLeft = Offset(0f, h * 0.3f), size = Size(w, h * 0.4f))
            var x = 0f
            while (x < w) {
                drawLine(a.copy(alpha = 0.22f), start = Offset(x, h * 0.3f), end = Offset(x, h * 0.7f), strokeWidth = 1.dp.toPx())
                x += 7.dp.toPx()
            }
        }
    }
}

// ── L4 材质纹理 ────────────────────────────────────────────────────────────

private fun DrawScope.drawMaterial(profile: CardIdentityProfile) {
    when (profile.material) {
        CardMaterial.BRUSHED -> {
            var x = 2.dp.toPx()
            while (x < size.width) {
                drawLine(Color.White.copy(alpha = 0.035f), start = Offset(x, 0f), end = Offset(x, size.height), strokeWidth = 1.dp.toPx())
                x += 6.dp.toPx()
            }
        }
        CardMaterial.GLASS, CardMaterial.SATIN -> {
            drawRect(
                Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.14f), Color.Transparent, Color.White.copy(alpha = 0.05f)),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height),
                ),
            )
        }
        else -> Unit
    }
}

// ── L5 Studio 主题 artwork（主题切换真实改变画面，非仅渐变）─────────────────

private fun DrawScope.drawThemeArtwork(theme: CardThemeInfo) {
    val w = size.width
    val h = size.height
    when (theme.artwork) {
        CardThemeArtwork.MINIMAL -> {
            drawLine(PdigV2Colors.TextMuted.copy(alpha = 0.5f), start = Offset(w * 0.06f, h * 0.86f), end = Offset(w * 0.94f, h * 0.86f), strokeWidth = 1.dp.toPx())
        }
        CardThemeArtwork.DEEP_SPACE -> {
            repeat(26) { i ->
                val x = w * ((i * 37) % 97) / 100f
                val y = h * ((i * 53) % 89) / 100f
                drawCircle(Color.White.copy(alpha = 0.16f + (i % 5) * 0.04f), radius = (0.7f + (i % 3) * 0.5f).dp.toPx(), center = Offset(x, y))
            }
        }
        CardThemeArtwork.REGION -> {
            listOf(0.2f, 0.34f, 0.48f).forEachIndexed { i, r ->
                drawCircle(PdigV2Colors.PrimaryBright.copy(alpha = 0.4f - i * 0.1f), radius = w * r, center = Offset(w * 0.5f, h * 0.32f), style = Stroke(width = 1.dp.toPx()))
            }
        }
        CardThemeArtwork.CITY -> {
            // 天际线剪影 + 暖色窗户（Human 一眼可辨城市）
            val baseY = h * 0.62f
            var x = w * 0.06f
            listOf(0.30f, 0.44f, 0.22f, 0.52f, 0.36f, 0.18f, 0.46f, 0.28f).forEachIndexed { _, hf ->
                val bw = w * 0.09f
                drawRect(PdigV2Colors.CanvasDeep.copy(alpha = 0.72f), topLeft = Offset(x, baseY - h * hf), size = Size(bw, h * hf))
                repeat(4) { wy ->
                    repeat(2) { wx ->
                        drawRect(
                            Color(0xFFF2B25C).copy(alpha = 0.85f),
                            topLeft = Offset(x + bw * 0.14f + wx * bw * 0.4f, baseY - h * hf + 3.dp.toPx() + wy * (h * hf * 0.24f)),
                            size = Size(bw * 0.14f, h * hf * 0.1f),
                        )
                    }
                }
                x += bw + w * 0.02f
            }
            drawRect(PdigV2Colors.CanvasDeep.copy(alpha = 0.85f), topLeft = Offset(0f, baseY), size = Size(w, 2.dp.toPx()))
        }
        CardThemeArtwork.GLASS -> {
            // 弥散光斑 + 玻璃斜条（translucency / layer / dispersion / glass strip）
            repeat(7) { i ->
                val x = w * (0.12f + (i * 0.13f) % 0.76f)
                val y = h * (0.14f + (i * 0.19f) % 0.68f)
                drawCircle(Color.White.copy(alpha = 0.16f - (i % 3) * 0.04f), radius = w * (0.035f + (i % 2) * 0.02f), center = Offset(x, y))
            }
            drawRect(
                Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.02f), Color.White.copy(alpha = 0.16f)),
                    start = Offset(w * 0.2f, 0f),
                    end = Offset(w * 0.55f, h),
                ),
                topLeft = Offset(w * 0.2f, 0f),
                size = Size(w * 0.16f, h),
            )
        }
        CardThemeArtwork.METAL -> {
            val steps = 14
            repeat(steps) { i ->
                val y = h * i / steps.toFloat()
                drawRect(Color.White.copy(alpha = if (i % 2 == 0) 0.09f else 0.02f), topLeft = Offset(0f, y), size = Size(w, h / steps.toFloat() + 1f))
            }
        }
        CardThemeArtwork.ABSTRACT -> {
            listOf(
                Color(0xFF7A5CFF) to Offset(w * 0.28f, h * 0.3f),
                Color(0xFFFF7AC2) to Offset(w * 0.6f, h * 0.55f),
                Color(0xFF3FD8C4) to Offset(w * 0.42f, h * 0.72f),
            ).forEach { (c, o) ->
                drawCircle(c.copy(alpha = 0.3f), radius = w * 0.16f, center = o)
            }
        }
    }
}