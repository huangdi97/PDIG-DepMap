package com.pdig.uivnext.globe

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pdig.uivnext.theme.PdigV2Colors
import kotlin.math.sqrt

/**
 * EarthLighting —— 方向光 + 日/夜因子 + specular ocean 响应（PHASE 1C）。
 * SUN_DIR 固定（terminator 由此产生）；纯本地、确定性。
 */

/** 世界空间太阳方向（固定；同旧 EarthScene）。 */
internal val SUN_DIR = latLonToVec(14f, -12f)

/** 表面法线（世界）与太阳夹角 → 昼间因子 [0,1]；带软 terminator 过渡。 */
internal fun dayFactor(nx: Float, ny: Float, nz: Float): Float {
    val lit = (nx * SUN_DIR.x + ny * SUN_DIR.y + nz * SUN_DIR.z).coerceIn(-1f, 1f)
    return ((lit + 0.30f) / 1.30f).coerceIn(0f, 1f)
}

/** 夜间因子（1-day 增强暗面）。 */
internal fun nightFactor(day: Float): Float = (1f - day) * (1f - day)

/** 光照调制（避免全黑；0.30 环境 + 0.70 昼光）。 */
internal fun litScale(day: Float): Float = 0.30f + 0.70f * day

/** specular ocean 高光：太阳受光侧椭圆亮斑（克制）。 */
internal fun DrawScope.drawOceanSpecular(center: Offset, radius: Float, cam: GlobeCamera) {
    // 太阳在屏幕上的位置 → 受光侧
    val sunScreen = project(SUN_DIR, cam, radius, center.x, center.y)
    val sx = center.x + (sunScreen.x - center.x) * 0.55f
    val sy = center.y + (sunScreen.y - center.y) * 0.55f
    drawOval(
        brush = Brush.radialGradient(
            listOf(PdigV2Colors.OceanSpecular.copy(alpha = 0.18f), Color.Transparent),
            center = Offset(sx, sy),
            radius = radius * 0.55f,
        ),
        topLeft = Offset(sx - radius * 0.55f, sy - radius * 0.28f),
        size = Size(radius * 1.10f, radius * 0.56f),
    )
}

/** 受光缘微弱 diffuse 亮度（方向光立体感）。 */
internal fun DrawScope.drawLimbLight(center: Offset, radius: Float, cam: GlobeCamera) {
    val sunScreen = project(SUN_DIR, cam, radius, center.x, center.y)
    val dir = Offset(sunScreen.x - center.x, sunScreen.y - center.y)
    val len = sqrt(dir.x * dir.x + dir.y * dir.y).coerceAtLeast(1e-3f)
    val lit = Offset(center.x + dir.x / len * radius * 0.7f, center.y + dir.y / len * radius * 0.7f)
    drawCircle(
        brush = Brush.radialGradient(
            listOf(PdigV2Colors.TerminatorLight.copy(alpha = 0.10f), Color.Transparent),
            center = lit,
            radius = radius * 0.5f,
        ),
        radius = radius * 0.5f,
        center = lit,
    )
}
