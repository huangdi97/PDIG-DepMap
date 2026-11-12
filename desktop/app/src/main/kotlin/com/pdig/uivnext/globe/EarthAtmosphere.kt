package com.pdig.uivnext.globe

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pdig.uivnext.theme.PdigV2Colors

/**
 * EarthAtmosphere —— 球后大气辉光 + 沿球缘蓝 rim（PHASE 1C）。
 */

/** 球后大气辉光（globe 背后光晕）。 */
internal fun DrawScope.drawAtmosphereGlow(center: Offset, radius: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(PdigV2Colors.AtmosphereInner.copy(alpha = 0.55f), PdigV2Colors.AtmosphereOuter),
            center = center,
            radius = radius * 1.7f,
        ),
        radius = radius * 1.7f,
        center = center,
    )
}

/** 大气 rim：软外晕 + 沿球缘亮环（reference E: atmospheric blue rim）。 */
internal fun DrawScope.drawAtmosphereRim(center: Offset, radius: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(PdigV2Colors.AtmosphereRim.copy(alpha = 0.24f), PdigV2Colors.AtmosphereOuter),
            center = center,
            radius = radius * 1.28f,
        ),
        radius = radius * 1.28f,
        center = center,
    )
    drawCircle(
        PdigV2Colors.AtmosphereRim.copy(alpha = 0.55f),
        radius = radius,
        center = center,
        style = Stroke(width = radius * 0.030f),
    )
    drawCircle(
        PdigV2Colors.AtmosphereRim.copy(alpha = 0.16f),
        radius = radius * 1.045f,
        center = center,
        style = Stroke(width = radius * 0.016f),
    )
}
