package com.pdig.uivnext.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.pdig.uivnext.theme.PdigV2Colors

/**
 * L0 Environment 背景（G3 去 Dashboard 化）。
 *
 *  - canvas → canvasDeep 深空垂直渐变；
 *  - 蓝色大气辉光（globe 舞台后方；atmosphereInner→atmosphereOuter）；
 *  - 底部 earth light 微光（克制的 primary 洗色）；
 *  - subtle 星点点阵（star token，确定性分布）。
 *
 * 禁止 cyberpunk neon / 过量星光 / 大面积紫色：所有元素都来自 token 且低 alpha。
 */
@Composable
fun EnvironmentBackdrop(modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize()) {
        // 近黑基底为主（canvasDeep → canvas 极缓过渡）；蓝只出现在局部辉光
        drawRect(Brush.verticalGradient(listOf(PdigV2Colors.Canvas, PdigV2Colors.CanvasDeep)))
        // 地球舞台后方的局部大气辉光（globe 区域，右侧中上）
        drawCircle(
            brush = Brush.radialGradient(
                listOf(PdigV2Colors.AtmosphereInner.copy(alpha = 0.90f), Color.Transparent),
                center = Offset(size.width * 0.58f, size.height * 0.40f),
                radius = size.height * 0.62f,
            ),
            radius = size.height * 0.62f,
            center = Offset(size.width * 0.58f, size.height * 0.40f),
        )
        // 底部 earth light（微弱环境反射；低 alpha）
        drawCircle(
            brush = Brush.radialGradient(
                listOf(PdigV2Colors.LocalIllum.copy(alpha = 0.30f), Color.Transparent),
                center = Offset(size.width * 0.5f, size.height * 1.02f),
                radius = size.height * 0.55f,
            ),
            radius = size.height * 0.55f,
            center = Offset(size.width * 0.5f, size.height * 1.02f),
        )
        // subtle 星点（确定性分布；低 alpha，非过量）
        for (i in 0 until 56) {
            val x = ((i * 127.7f) % 360f) / 360f * size.width
            val y = ((i * 83.9f) % 240f) / 240f * size.height
            val a = 0.06f + 0.12f * ((i * 5) % 9) / 9f
            drawCircle(PdigV2Colors.Star.copy(alpha = a), radius = 0.4f + (i % 2) * 0.3f, center = Offset(x, y))
        }
    }
}
