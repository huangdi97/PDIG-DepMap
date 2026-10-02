package com.pdig.uivnext.ui.screens

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.layout.Phase1FLayout
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.statusColor

/**
 * ContinuityScene 绘制辅助 —— 路径 + 号码身份面（PHASE 1F §31–§34）。
 * 路径最弱层级：primary active 2.5 / secondary 2.0 / ghost 1.5，无更强辉光。
 * 服务节点与投影徽标见 ContinuitySceneNodes.kt。
 */

/** 旧号 ──服务──▶ 新号 的双段 Bezier；waiting=虚线+amber，blocked=红断点。 */
internal fun DrawScope.drawServicePaths(
    fromX: Float, fromY: Float,
    toX: Float, toY: Float,
    sx: Float, sy: Float,
    boxW: Float,
    status: String,
    projection: String,
    w: Float,
) {
    val color = statusColor(status)
    val boxL = sx - boxW * 0.5f
    val boxR = sx + boxW * 0.5f
    // PHASE 1F-HF（§8）：路径更细、更透明（node state > line style），
    // control 点更紧凑 → 线路尽量在 node 背后垂直 lane 内，减少穿越与遮挡文字。
    val oldPath = cubicBezier(
        Offset(fromX, fromY),
        Offset(fromX + w * 0.06f, fromY),
        Offset(boxL - w * 0.06f, sy),
        Offset(boxL - w * 0.02f, sy),
    )
    when {
        projection == "after" -> drawCubic(oldPath, color.copy(alpha = 0.15f), Phase1FLayout.PATH_GHOST_PX, dashed = false)
        status == "blocked" -> drawCubic(oldPath, color.copy(alpha = 0.75f), Phase1FLayout.PATH_SECONDARY_PX, dashed = false)
        else -> drawCubic(oldPath, color.copy(alpha = if (status == "not_started") 0.30f else 0.70f), Phase1FLayout.PATH_SECONDARY_PX, dashed = status == "waiting")
    }
    val newPath = cubicBezier(
        Offset(boxR + w * 0.02f, sy),
        Offset(boxR + w * 0.06f, sy),
        Offset(toX - w * 0.06f, toY),
        Offset(toX, toY),
    )
    when {
        projection == "current" -> drawCubic(newPath, color.copy(alpha = 0.28f), Phase1FLayout.PATH_GHOST_PX, dashed = false)
        status == "blocked" -> drawCubic(newPath, color.copy(alpha = 0.20f), Phase1FLayout.PATH_GHOST_PX, dashed = false)
        status == "migrated" -> drawCubic(newPath, color.copy(alpha = 0.90f), Phase1FLayout.PATH_PRIMARY_PX, dashed = false)
        else -> drawCubic(newPath, color.copy(alpha = if (projection == "after") 0.60f else 0.45f), Phase1FLayout.PATH_SECONDARY_PX, dashed = status == "waiting")
    }
    if (status == "blocked") {
        drawLine(PdigV2Colors.Critical, Offset(sx - 7f, sy - 7f), Offset(sx + 7f, sy + 7f), strokeWidth = 2.6f)
        drawLine(PdigV2Colors.Critical, Offset(sx - 7f, sy + 7f), Offset(sx + 7f, sy - 7f), strokeWidth = 2.6f)
    }
}

internal data class Cubic(val p0: Offset, val c1: Offset, val c2: Offset, val p3: Offset)

internal fun cubicBezier(p0: Offset, c1: Offset, c2: Offset, p3: Offset): Cubic = Cubic(p0, c1, c2, p3)

internal fun cubicAt(c: Cubic, t: Float): Offset {
    val u = 1f - t
    val a = u * u * u
    val b = 3f * u * u * t
    val d = 3f * u * t * t
    val e = t * t * t
    return Offset(
        a * c.p0.x + b * c.c1.x + d * c.c2.x + e * c.p3.x,
        a * c.p0.y + b * c.c1.y + d * c.c2.y + e * c.p3.y,
    )
}

internal fun DrawScope.drawCubic(c: Cubic, color: Color, stroke: Float, dashed: Boolean) {
    if (!dashed) {
        val path = Path()
        path.moveTo(c.p0.x, c.p0.y)
        path.cubicTo(c.c1.x, c.c1.y, c.c2.x, c.c2.y, c.p3.x, c.p3.y)
        drawPath(path, color = color, style = Stroke(width = stroke))
        return
    }
    var t = 0f
    var prev: Offset? = null
    var drawOn = true
    while (t <= 1f) {
        val p = cubicAt(c, t)
        if (prev != null) {
            if (drawOn) drawLine(color, prev, p, strokeWidth = stroke)
            drawOn = !drawOn
        }
        prev = p
        t += 0.05f
    }
}

/** 号码身份面（旧/新；250–300px 宽、内容层级：title → number → meta）。 */
internal fun DrawScope.drawNumberNode(
    textMeasurer: TextMeasurer,
    center: Offset,
    nodeW: Float,
    nodeH: Float,
    title: String,
    number: String,
    meta: String,
    emphasis: Boolean,
    alpha: Float,
    border: Color,
    accent: Color,
) {
    val left = center.x - nodeW / 2f
    val top = center.y - nodeH / 2f
    drawRoundRect(PdigV2Colors.Surface.copy(alpha = 0.92f * alpha), Offset(left, top), Size(nodeW, nodeH), CornerRadius(18f))
    drawRoundRect(
        border.copy(alpha = border.alpha * alpha),
        Offset(left, top),
        Size(nodeW, nodeH),
        CornerRadius(18f),
        style = Stroke(width = if (emphasis) 2f else 1.4f),
    )
    drawRect(
        Brush.horizontalGradient(listOf(accent.copy(alpha = 0.20f * alpha), Color.Transparent)),
        topLeft = Offset(left, top),
        size = Size(nodeW * 0.5f, nodeH),
    )
    val tTitle = textMeasurer.measure(
        AnnotatedString(title),
        style = TextStyle(color = if (emphasis) PdigV2Colors.PrimaryBright else PdigV2Colors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.W600),
    )
    val tNum = textMeasurer.measure(
        AnnotatedString(number),
        style = TextStyle(color = PdigV2Colors.TextPrimary.copy(alpha = alpha), fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
    )
    val tMeta = textMeasurer.measure(
        AnnotatedString(meta),
        style = TextStyle(color = PdigV2Colors.TextMuted.copy(alpha = alpha), fontSize = 11.sp),
    )
    drawText(tTitle, topLeft = Offset(left + 14f, top + 12f))
    drawText(tNum, topLeft = Offset(left + 14f, top + nodeH * 0.40f))
    drawText(tMeta, topLeft = Offset(left + 14f, top + nodeH * 0.70f))
}

/** 服务节点迁移数据（Change Phone 场景行；role = 关系角色，§33）。 */
data class SceneMigration(
    val service: String,
    val status: String,
    val role: String = "",
)

internal fun roleLabelScene(role: String): String = when (role) {
    "primary" -> "主号"
    "secondary" -> "副号"
    "keep" -> "保号"
    else -> role
}
