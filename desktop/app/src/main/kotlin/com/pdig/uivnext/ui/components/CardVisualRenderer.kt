package com.pdig.uivnext.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pdig.uivnext.model.PresentationProfile
import com.pdig.uivnext.theme.PdigV2Colors

/**
 * CardVisualRenderer（PHASE 1D §14–§15）—— 卡片视觉渲染器结构拆分：
 *
 *  - CardMaterial：材质层（MATTE 微颗粒+实色深度 / GLASS 分层半透明+内高光+rim /
 *    METAL 拉丝+宽高光+边缘反射 / MINIMAL 纯色平面，typography 即 identity）
 *  - CardArtwork：艺术作品层（city 升级为多层天际线+透视深度+窗簇+fog/light gradient；
 *    region 地形等高线 / abstract 几何 / deep-space 星云）
 *  - CardLayout / CardContent / CardStatusOverlay 见 CardFace
 *
 * CardFace.drawCardFaceBackdrop 调用 CardMaterial.material + CardArtwork.artwork；
 * 同一渲染器被 Studio 缩略图复用（确定性、固定渲染器）。
 */

/** 材质层（PHASE 1D §14：MATTE/GLASS/METAL/MINIMAL 真正 perceptual contract，静态截图可肉眼区分）。 */
object CardMaterial {
    fun material(scope: DrawScope, p: PresentationProfile, w: Float, h: Float) = with(scope) {
        when (p.material) {
            "matte" -> {
                // MATTE：low specular · micro grain · 无强反射 · solid color depth
                for (i in 0 until 90) {
                    val x = ((i * 41.7f) % 97f) / 97f * w
                    val y = ((i * 29.3f) % 97f) / 97f * h
                    val a = 0.04f + (i % 4) * 0.025f
                    drawCircle(PdigV2Colors.TextMuted.copy(alpha = a), radius = 0.5f + (i % 3) * 0.45f, center = Offset(x, y))
                }
                // solid depth：四角暗化
                drawRect(
                    Brush.radialGradient(
                        listOf(Color.Transparent, PdigV2Colors.CanvasDeep.copy(alpha = 0.30f)),
                        center = Offset(w * 0.5f, h * 0.5f),
                        radius = w * 0.7f,
                    ),
                )
            }
            "metal" -> {
                // METAL：directional brushing + broad highlight + stronger edge reflectance
                for (i in 1 until 26) {
                    val y = h * i / 26f
                    val alpha = 0.05f + (i % 5) * 0.02f
                    drawLine(PdigV2Colors.TextSecondary.copy(alpha = alpha), Offset(0f, y), Offset(w, y), strokeWidth = 0.8f)
                }
                // 大范围移动式高光（截图冻结）
                drawRect(
                    Brush.linearGradient(
                        listOf(
                            Color.Transparent,
                            PdigV2Colors.TextPrimary.copy(alpha = 0.10f),
                            PdigV2Colors.TextPrimary.copy(alpha = 0.16f),
                            PdigV2Colors.TextPrimary.copy(alpha = 0.10f),
                            Color.Transparent,
                        ),
                        start = Offset(0f, h * 0.30f),
                        end = Offset(w, h * 0.60f),
                    ),
                )
                // 边缘反射
                drawLine(PdigV2Colors.TextPrimary.copy(alpha = 0.18f), Offset(0f, 2f), Offset(w, 2f), strokeWidth = 1.5f)
                drawLine(PdigV2Colors.TextPrimary.copy(alpha = 0.12f), Offset(0f, h - 2f), Offset(w, h - 2f), strokeWidth = 1.5f)
            }
            "glass" -> {
                // GLASS：translucent layered + inner highlight + rim reflection
                drawRect(
                    Brush.linearGradient(
                        listOf(PdigV2Colors.TextPrimary.copy(alpha = 0.10f), Color.Transparent),
                        start = Offset(0f, 0f),
                        end = Offset(w, h),
                    ),
                )
                drawRect(
                    Brush.verticalGradient(listOf(PdigV2Colors.TextPrimary.copy(alpha = 0.22f), Color.Transparent)),
                    topLeft = Offset(0f, 0f),
                    size = Size(w, h * 0.18f),
                )
                // inner highlight 斜带
                drawRect(
                    Brush.linearGradient(
                        listOf(Color.Transparent, PdigV2Colors.AtmosphereRim.copy(alpha = 0.22f), Color.Transparent),
                        start = Offset(w * 0.1f, h * 0.05f),
                        end = Offset(w * 0.9f, h * 0.55f),
                    ),
                )
                // rim reflection（四边）
                val rim = Path().apply {
                    moveTo(1.5f, 1.5f)
                    lineTo(w - 1.5f, 1.5f)
                    lineTo(w - 1.5f, h - 1.5f)
                    lineTo(1.5f, h - 1.5f)
                    close()
                }
                drawPath(rim, color = PdigV2Colors.TextPrimary.copy(alpha = 0.14f), style = Stroke(width = 1.4f))
            }
            else -> Unit // MINIMAL = flat premium surface, typography is identity
        }
    }
}

/** 艺术作品层（PHASE 1D §15：city 升级为多层天际线 + 透视 + 窗簇 + fog/light gradient）。 */
object CardArtwork {
    fun artwork(scope: DrawScope, p: PresentationProfile, w: Float, h: Float) = with(scope) {
        when (p.themeId) {
            "deep-space" -> {
                drawCircle(
                    brush = Brush.radialGradient(listOf(PdigV2Colors.PrimarySoft.copy(alpha = 0.7f), Color.Transparent)),
                    radius = w * 0.55f,
                    center = Offset(w * 0.30f, h * 0.20f),
                )
                for (i in 0 until 22) {
                    val x = ((i * 37.5f) % 100f) / 100f * w
                    val y = ((i * 23.7f) % 100f) / 100f * h
                    drawCircle(PdigV2Colors.Star.copy(alpha = 0.30f + (i % 3) * 0.12f), radius = 1.0f + (i % 2), center = Offset(x, y))
                }
            }
            "region" -> {
                val cx = w * 0.72f
                val cy = h * 0.34f
                for (ring in 1..5) {
                    drawCircle(
                        color = PdigV2Colors.LandTextureHi.copy(alpha = 0.08f + ring * 0.035f),
                        radius = w * (0.06f + ring * 0.06f),
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.2f),
                    )
                }
                val coast = Path()
                coast.moveTo(cx - w * 0.26f, cy + h * 0.10f)
                coast.lineTo(cx - w * 0.20f, cy - h * 0.06f)
                coast.lineTo(cx - w * 0.02f, cy - h * 0.14f)
                coast.lineTo(cx + w * 0.16f, cy - h * 0.02f)
                coast.lineTo(cx + w * 0.24f, cy + h * 0.12f)
                coast.lineTo(cx + w * 0.10f, cy + h * 0.16f)
                coast.lineTo(cx - w * 0.16f, cy + h * 0.12f)
                coast.close()
                drawPath(coast, color = PdigV2Colors.LandBase.copy(alpha = 0.55f))
                drawPath(coast, color = PdigV2Colors.LandTextureHi.copy(alpha = 0.4f), style = Stroke(width = 1.3f))
                drawCircle(PdigV2Colors.RegionNodeHi.copy(alpha = 0.9f), radius = 3f, center = Offset(cx, cy))
            }
            "city" -> drawCityNight(p, w, h)
            "abstract" -> {
                val diag = Path().apply {
                    moveTo(w * 0.30f, 0f)
                    lineTo(w, h * 0.62f)
                    lineTo(w, h)
                    lineTo(w * 0.12f, h)
                    close()
                }
                drawPath(diag, color = PdigV2Colors.PrimaryBright.copy(alpha = 0.18f))
                drawCircle(PdigV2Colors.PrimaryBright.copy(alpha = 0.30f), radius = w * 0.22f, center = Offset(w * 0.78f, h * 0.28f))
                drawCircle(PdigV2Colors.NightCityLight.copy(alpha = 0.18f), radius = w * 0.10f, center = Offset(w * 0.28f, h * 0.78f))
                for (i in 1 until 6) {
                    val x = w * i / 6f
                    drawLine(
                        color = PdigV2Colors.TextMuted.copy(alpha = 0.08f),
                        start = Offset(x, 0f),
                        end = Offset(x, h),
                        strokeWidth = 1f,
                    )
                }
            }
            "glass" -> {
                drawRect(
                    Brush.linearGradient(
                        listOf(PdigV2Colors.AtmosphereRim.copy(alpha = 0.30f), Color.Transparent),
                        start = Offset(0f, h),
                        end = Offset(w, 0f),
                    ),
                )
            }
            else -> Unit
        }
    }
}

/** 城市夜景：多层天际线（back/mid/front）+ 透视深度 + 窗簇 + fog/light gradient。 */
private fun DrawScope.drawCityNight(
    p: PresentationProfile,
    w: Float,
    h: Float,
) {
    // 底部城市辉光（fog/light gradient）
    drawRect(
        Brush.verticalGradient(
            listOf(Color.Transparent, PdigV2Colors.NightCityLight.copy(alpha = 0.16f), PdigV2Colors.CanvasDeep.copy(alpha = 0.25f)),
            startY = h * 0.45f,
            endY = h,
        ),
    )
    // back layer：远景轮廓（perspective 弱化）
    val backY = h * 0.58f
    val back = Path().apply {
        moveTo(0f, h)
        lineTo(0f, backY)
        lineTo(w * 0.07f, backY - h * 0.06f)
        lineTo(w * 0.14f, backY)
        lineTo(w * 0.22f, backY - h * 0.10f)
        lineTo(w * 0.31f, backY)
        lineTo(w * 0.38f, backY - h * 0.07f)
        lineTo(w * 0.47f, backY)
        lineTo(w * 0.55f, backY - h * 0.12f)
        lineTo(w * 0.63f, backY)
        lineTo(w * 0.71f, backY - h * 0.08f)
        lineTo(w * 0.80f, backY)
        lineTo(w * 0.88f, backY - h * 0.11f)
        lineTo(w, backY)
        lineTo(w, h)
        close()
    }
    drawPath(back, color = PdigV2Colors.CanvasDeep.copy(alpha = 0.35f))
    // mid layer：主楼群 + 窗簇
    val midY = h * 0.66f
    val mid = Path().apply {
        moveTo(0f, h)
        lineTo(0f, midY)
        lineTo(w * 0.06f, midY - h * 0.16f)
        lineTo(w * 0.10f, midY - h * 0.16f)
        lineTo(w * 0.10f, midY)
        lineTo(w * 0.17f, midY - h * 0.22f)
        lineTo(w * 0.22f, midY - h * 0.22f)
        lineTo(w * 0.22f, midY)
        lineTo(w * 0.30f, midY - h * 0.13f)
        lineTo(w * 0.34f, midY - h * 0.13f)
        lineTo(w * 0.34f, midY)
        lineTo(w * 0.42f, midY - h * 0.28f)
        lineTo(w * 0.47f, midY - h * 0.28f)
        lineTo(w * 0.47f, midY)
        lineTo(w * 0.55f, midY - h * 0.18f)
        lineTo(w * 0.60f, midY - h * 0.18f)
        lineTo(w * 0.60f, midY)
        lineTo(w * 0.68f, midY - h * 0.25f)
        lineTo(w * 0.73f, midY - h * 0.25f)
        lineTo(w * 0.73f, midY)
        lineTo(w * 0.80f, midY - h * 0.14f)
        lineTo(w * 0.85f, midY - h * 0.14f)
        lineTo(w * 0.85f, midY)
        lineTo(w * 0.92f, midY - h * 0.20f)
        lineTo(w, midY - h * 0.20f)
        lineTo(w, h)
        close()
    }
    drawPath(mid, color = PdigV2Colors.SurfaceRaised.copy(alpha = 0.85f))
    // 窗簇（per-building window clusters）
    val clusterX = listOf(w * 0.08f, w * 0.19f, w * 0.32f, w * 0.44f, w * 0.57f, w * 0.70f, w * 0.82f, w * 0.93f)
    val clusterH = listOf(h * 0.16f, h * 0.22f, h * 0.13f, h * 0.28f, h * 0.18f, h * 0.25f, h * 0.14f, h * 0.20f)
    clusterX.forEachIndexed { i, bx ->
        val top = midY - clusterH[i]
        val cols = 3 + (i % 2)
        val rows = 4 + (i % 3)
        for (c in 0 until cols) {
            for (r in 0 until rows) {
                val wx = bx - w * 0.012f + c * (w * 0.010f)
                val wy = top + h * 0.015f + r * (h * 0.028f)
                val lit = ((i * 7 + c * 3 + r * 5) % 11) / 11f
                drawRect(
                    color = PdigV2Colors.NightCityLight.copy(alpha = 0.35f + lit * 0.5f),
                    topLeft = Offset(wx, wy),
                    size = Size(w * 0.007f, h * 0.012f),
                )
            }
        }
    }
    // front layer：底层连排（前景剪影）
    drawRect(
        color = PdigV2Colors.CanvasDeep.copy(alpha = 0.55f),
        topLeft = Offset(0f, h * 0.88f),
        size = Size(w, h * 0.12f),
    )
    // 近景少量暖光
    for (i in 0 until 8) {
        val x = w * (0.05f + i * 0.12f)
        drawCircle(PdigV2Colors.NightCityLight.copy(alpha = 0.5f), radius = 1.4f, center = Offset(x, h * 0.93f))
    }
}
