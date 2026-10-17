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
 * CardVisualRenderer（PHASE 1D §14–§15 + PHASE 1F §7–§9）—— 卡面视觉渲染器：
 *
 *  - CardMaterial：材质层（MATTE 微颗粒+实色深度 / GLASS 分层半透明+内高光+rim /
 *    METAL 拉丝+宽高光+边缘反射 / MINIMAL 纯色平面，typography 即 identity）
 *  - 艺术/身份层（palette + motif + glow）已迁移至 CardIdentitySystem
 *    （CardIdentity.kt + CardIdentityDrawing.kt，§9 issuer 身份）。
 *
 * CardFace.drawCardFaceBackdrop 调用 CardIdentity（底色/motif）+ CardMaterial.material；
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
