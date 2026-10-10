package com.pdig.uivnext.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.pdig.uivnext.theme.PdigV2Colors

/**
 * LocalLightSource（PHASE 1C §27）—— 分页局部光源。
 *
 * 每个页面拥有独立光源（不得共用"全页中心固定 radial"）：
 *  - Overview：Earth 蓝大气（Globe 自带，无需额外）
 *  - Card Studio：preview 局部光（StudioFrame.drawPreviewStage）
 *  - Change Phone：active migration path glow（本组件）
 *  - Number Detail：identity face 低强度地区光（本组件）
 *
 * 用法：把需要被照亮的区域包进 LocalGlow，glow 绘制在其后方。
 */
@Composable
fun LocalGlow(
    modifier: Modifier = Modifier,
    color: Color = PdigV2Colors.PrimaryBright,
    alpha: Float = 0.10f,
    radiusFraction: Float = 0.5f,
    centerFraction: Offset = Offset(0.5f, 0.5f),
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .fillMaxSize()
            .drawBehind {
                val w = size.width
                val h = size.height
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(color.copy(alpha = alpha), Color.Transparent),
                        center = Offset(w * centerFraction.x, h * centerFraction.y),
                        radius = w * radiusFraction,
                    ),
                    radius = w * radiusFraction,
                    center = Offset(w * centerFraction.x, h * centerFraction.y),
                )
            },
        content = { content() },
    )
}
