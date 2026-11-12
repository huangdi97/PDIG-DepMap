package com.pdig.uivnext.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import com.pdig.uivnext.theme.PdigV2Colors

/**
 * L0 Environment 背景（PHASE 1C：去掉全局蓝 blob）。
 *
 * 只保留 near-black / deep-navy 基底 + 极淡星点；
 * 局部照明由各页面的 LocalLightSource 提供（Globe / selected asset /
 * active migration / focused preview），禁止全页统一 primary radial 背景。
 */
@Composable
fun EnvironmentBackdrop(modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize()) {
        drawRect(Brush.verticalGradient(listOf(PdigV2Colors.Surface, PdigV2Colors.CanvasDeep)))
        // 极淡星点（确定性；低 alpha，非过量）
        for (i in 0 until 48) {
            val x = ((i * 127.7f) % 360f) / 360f * size.width
            val y = ((i * 83.9f) % 240f) / 240f * size.height
            val a = 0.05f + 0.10f * ((i * 5) % 9) / 9f
            drawCircle(PdigV2Colors.Star.copy(alpha = a), radius = 0.4f + (i % 2) * 0.3f, center = Offset(x, y))
        }
    }
}
