package com.pdig.uivnext.ui.screens

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.statusColor

/**
 * ContinuityScene 绘制辅助 —— 服务节点 + 计划投影徽标（PHASE 1F §30/§35）。
 * 服务节点（130–170px）：glyph 圆 + 名称 + 关系角色 + 状态 pill + 状态色边框；
 * 未迁移/未知在 after 保持可见未解决（不暗示已验证）。
 */

/** 服务节点盒（次级层级；glyph + 名称 + 关系 + 状态，§30）。 */
internal fun DrawScope.drawServiceNode(
    textMeasurer: TextMeasurer,
    sx: Float,
    sy: Float,
    name: String,
    role: String,
    status: String,
    projection: String,
    boxW: Float,
    boxH: Float,
) {
    val color = statusColor(status)
    val alpha = if (projection == "after" && status == "not_started") 0.55f else 1f
    val left = sx - boxW / 2f
    val top = sy - boxH / 2f
    // 盒体
    drawRoundRect(PdigV2Colors.Surface.copy(alpha = 0.90f * alpha), Offset(left, top), Size(boxW, boxH), CornerRadius(14f))
    drawRoundRect(
        color.copy(alpha = 0.55f * alpha),
        Offset(left, top),
        Size(boxW, boxH),
        CornerRadius(14f),
        style = Stroke(width = 1.5f),
    )
    // waiting = 虚线边框（未解决），blocked = 红色实框
    if (status == "waiting") {
        drawDashedRoundRect(offset = Offset(left, top), size = Size(boxW, boxH), radius = 14f, color = color.copy(alpha = 0.8f * alpha), stroke = 1.5f)
    }
    if (status == "blocked") {
        drawRoundRect(
            PdigV2Colors.Critical.copy(alpha = 0.8f * alpha),
            Offset(left + 1.5f, top + 1.5f),
            Size(boxW - 3f, boxH - 3f),
            CornerRadius(13f),
            style = Stroke(width = 1.6f),
        )
    }
    // glyph 圆
    val gx = left + 22f
    val gy = top + 22f
    drawCircle(PdigV2Colors.SurfaceRaised.copy(alpha = alpha), radius = 17f, center = Offset(gx, gy))
    drawCircle(color.copy(alpha = 0.7f * alpha), radius = 16f, center = Offset(gx, gy), style = Stroke(width = 1.5f))
    val glyph = name.firstOrNull()?.toString() ?: "?"
    val g = textMeasurer.measure(
        AnnotatedString(glyph),
        style = TextStyle(color = color.copy(alpha = alpha), fontSize = 13.sp, fontWeight = FontWeight.Bold),
    )
    drawText(g, topLeft = Offset(gx - g.size.width / 2f, gy - g.size.height / 2f))
    // 名称 + 关系角色
    val tName = textMeasurer.measure(
        AnnotatedString(name),
        style = TextStyle(color = PdigV2Colors.TextPrimary.copy(alpha = alpha), fontSize = 13.sp, fontWeight = FontWeight.W600),
    )
    drawText(tName, topLeft = Offset(left + 44f, top + 12f))
    val roleLine = if (role.isNotEmpty()) role else sceneStatusLabel(status)
    val tRole = textMeasurer.measure(
        AnnotatedString(roleLine),
        style = TextStyle(color = PdigV2Colors.TextMuted.copy(alpha = alpha), fontSize = 10.sp),
    )
    drawText(tRole, topLeft = Offset(left + 44f, top + 30f))
    // 状态 pill（底部）
    val tStatus = textMeasurer.measure(
        AnnotatedString(sceneStatusLabel(status)),
        style = TextStyle(color = color.copy(alpha = alpha), fontSize = 10.sp, fontWeight = FontWeight.W600),
    )
    val pillW = tStatus.size.width + 16f
    val pillH = tStatus.size.height + 6f
    drawRoundRect(
        color.copy(alpha = 0.16f * alpha),
        Offset(left + boxW - pillW - 8f, top + boxH - pillH - 6f),
        Size(pillW, pillH),
        CornerRadius(8f),
    )
    drawText(tStatus, topLeft = Offset(left + boxW - pillW - 8f + 8f, top + boxH - pillH - 6f + 3f))
}

private fun DrawScope.drawDashedRoundRect(offset: Offset, size: Size, radius: Float, color: androidx.compose.ui.graphics.Color, stroke: Float) {
    fun dashLine(a: Offset, b: Offset) {
        val len = (b - a).getDistance()
        val seg = 7f
        var d = 0f
        var drawOn = true
        while (d < len) {
            val p1 = a + (b - a) * (d / len)
            val p2 = a + (b - a) * (kotlin.math.min(d + seg, len) / len)
            if (drawOn) drawLine(color, p1, p2, strokeWidth = stroke)
            drawOn = !drawOn
            d += seg * 2
        }
    }
    val l = offset.x
    val t = offset.y
    val r = offset.x + size.width
    val b = offset.y + size.height
    dashLine(Offset(l, t), Offset(r, t))
    dashLine(Offset(r, t), Offset(r, b))
    dashLine(Offset(r, b), Offset(l, b))
    dashLine(Offset(l, b), Offset(l, t))
}

/** 计划投影徽标（§35）：标题「计划完成后的预期状态」+ 次行「不代表已经完成或验证」。 */
internal fun DrawScope.drawPlanProjectionBadge(textMeasurer: TextMeasurer, w: Float) {
    val t = textMeasurer.measure(
        AnnotatedString("计划完成后的预期状态"),
        style = TextStyle(color = PdigV2Colors.Warning, fontSize = 12.sp, fontWeight = FontWeight.W600),
    )
    val sub = textMeasurer.measure(
        AnnotatedString("不代表已经完成或验证"),
        style = TextStyle(color = PdigV2Colors.TextSecondary, fontSize = 10.sp),
    )
    val chipW = maxOf(t.size.width, sub.size.width) + 16f
    val chipH = t.size.height + sub.size.height + 12f
    drawRoundRect(
        PdigV2Colors.CanvasDeep.copy(alpha = 0.88f),
        Offset(w - chipW - 16f, 8f),
        Size(chipW, chipH),
        CornerRadius(8f),
    )
    drawRect(
        Brush.horizontalGradient(listOf(PdigV2Colors.Warning.copy(alpha = 0.20f), PdigV2Colors.Warning.copy(alpha = 0.02f))),
        topLeft = Offset(w - chipW - 16f, 8f),
        size = Size(3f, chipH),
    )
    drawText(t, topLeft = Offset(w - chipW - 10f, 12f), color = PdigV2Colors.Warning)
    drawText(sub, topLeft = Offset(w - chipW - 10f, 12f + t.size.height + 1f), color = PdigV2Colors.TextSecondary)
}

internal fun sceneStatusLabel(status: String): String = when (status) {
    "migrated" -> "已迁移接管"
    "waiting" -> "等待验证"
    "blocked" -> "不可停用"
    "not_started" -> "未开始"
    else -> status
}
