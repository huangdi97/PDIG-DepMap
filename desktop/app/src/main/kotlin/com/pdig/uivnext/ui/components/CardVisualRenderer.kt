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
 * CardVisualRenderer（PHASE 1C §10–§12）—— 卡片视觉渲染器结构拆分：
 *
 *  - CardMaterial：材质层（MATTE 微颗粒 / GLASS 分层半透明 / METAL 拉丝+各向异性 / MINIMAL 实心）
 *  - CardArtwork：艺术作品层（region 地形轮廓 / city 天际线+灯光 / abstract 几何 / deep-space 星云）
 *  - CardLayout：布局切换（standard / emblem / minimal-content，见 CardFace）
 *  - CardContent：内容排版（issuer/nickname/number/metadata，见 CardFace）
 *  - CardStatusOverlay：状态叠层（StatusBadge，见 VNextKit）
 *
 * CardFace.drawCardFaceBackdrop 调用 CardMaterial.material + CardArtwork.artwork；
 * 同一渲染器被 Studio 缩略图复用（确定性、固定渲染器）。
 */

/** 材质层（PHASE 1C：真差异，非换 linearGradient）。 */
object CardMaterial {
    fun material(scope: DrawScope, p: PresentationProfile, w: Float, h: Float) = with(scope) {
        when (p.material) {
            "matte" -> {
                // micro grain（哑光颗粒）+ soft diffuse
                for (i in 0 until 26) {
                    val x = ((i * 41.7f) % 100f) / 100f * w
                    val y = ((i * 29.3f) % 100f) / 100f * h
                    drawCircle(PdigV2Colors.TextMuted.copy(alpha = 0.06f + (i % 3) * 0.03f), radius = 0.6f + (i % 2) * 0.4f, center = Offset(x, y))
                }
            }
            "metal" -> {
                // brushed directional texture + anisotropic highlight + edge response
                drawRect(
                    Brush.linearGradient(
                        listOf(
                            PdigV2Colors.TextPrimary.copy(alpha = 0.10f),
                            Color.Transparent,
                            PdigV2Colors.TextPrimary.copy(alpha = 0.05f),
                        ),
                        start = Offset(0f, h * 0.28f),
                        end = Offset(0f, h * 0.72f),
                    ),
                )
                drawRect(Brush.horizontalGradient(listOf(Color.Transparent, PdigV2Colors.TextMuted.copy(alpha = 0.20f), Color.Transparent)), topLeft = Offset(0f, h * 0.44f), size = Size(w, 1f))
                for (i in 1 until 7) {
                    drawLine(
                        color = PdigV2Colors.TextMuted.copy(alpha = 0.05f),
                        start = Offset(0f, h * i / 7f),
                        end = Offset(w, h * i / 7f),
                        strokeWidth = 1f,
                    )
                }
            }
            "glass" -> {
                // layered translucency + edge reflection streak
                drawRect(
                    Brush.linearGradient(
                        listOf(PdigV2Colors.TextPrimary.copy(alpha = 0.07f), Color.Transparent),
                        start = Offset(0f, 0f),
                        end = Offset(w, h),
                    ),
                )
                drawRect(
                    Brush.verticalGradient(listOf(PdigV2Colors.TextPrimary.copy(alpha = 0.14f), Color.Transparent)),
                    topLeft = Offset(0f, 0f),
                    size = Size(w, h * 0.16f),
                )
            }
            else -> Unit // minimal = solid premium surface, no decorative geometry
        }
    }
}

/** 艺术作品层（PHASE 1C：region/city 用真实地区视觉语言，非三个圆/方块 skyline）。 */
object CardArtwork {
    fun artwork(scope: DrawScope, p: PresentationProfile, w: Float, h: Float) = with(scope) {
        when (p.themeId) {
            "deep-space" -> {
                drawCircle(
                    brush = Brush.radialGradient(listOf(PdigV2Colors.PrimarySoft.copy(alpha = 0.7f), Color.Transparent)),
                    radius = w * 0.55f,
                    center = Offset(w * 0.30f, h * 0.20f),
                )
                for (i in 0 until 16) {
                    val x = ((i * 37.5f) % 100f) / 100f * w
                    val y = ((i * 23.7f) % 100f) / 100f * h
                    drawCircle(PdigV2Colors.Star.copy(alpha = 0.30f + (i % 3) * 0.12f), radius = 1.0f + (i % 2), center = Offset(x, y))
                }
            }
            "region" -> {
                // 地形等高线（bundled 简化 topo 语言）+ 海岸轮廓 + 区域光场 + 标记点
                val cx = w * 0.72f
                val cy = h * 0.34f
                for (ring in 1..4) {
                    drawCircle(
                        color = PdigV2Colors.LandTextureHi.copy(alpha = 0.10f + ring * 0.04f),
                        radius = w * (0.08f + ring * 0.07f),
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
            "city" -> {
                // 更丰富城市作品：天际线层次 + 窗口灯光
                val skyline = Path()
                skyline.moveTo(0f, h * 0.72f)
                skyline.lineTo(w * 0.10f, h * 0.72f)
                skyline.lineTo(w * 0.10f, h * 0.55f)
                skyline.lineTo(w * 0.18f, h * 0.55f)
                skyline.lineTo(w * 0.18f, h * 0.62f)
                skyline.lineTo(w * 0.27f, h * 0.62f)
                skyline.lineTo(w * 0.27f, h * 0.45f)
                skyline.lineTo(w * 0.36f, h * 0.45f)
                skyline.lineTo(w * 0.36f, h * 0.60f)
                skyline.lineTo(w * 0.48f, h * 0.60f)
                skyline.lineTo(w * 0.48f, h * 0.50f)
                skyline.lineTo(w * 0.58f, h * 0.50f)
                skyline.lineTo(w * 0.58f, h * 0.70f)
                skyline.lineTo(w, h * 0.70f)
                skyline.lineTo(w, h)
                skyline.lineTo(0f, h)
                skyline.close()
                drawPath(skyline, color = PdigV2Colors.CanvasDeep.copy(alpha = 0.75f))
                for (row in 0 until 4) {
                    for (col in 0 until 10) {
                        val x = 12f + col * ((w - 24f) / 9f)
                        val y = h * (0.30f + 0.13f * row)
                        drawCircle(PdigV2Colors.NightCityLight.copy(alpha = 0.22f + (col % 3) * 0.14f), radius = 1.1f, center = Offset(x, y))
                    }
                }
            }
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
