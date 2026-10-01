package com.pdig.uivnext.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.model.UiVNextNumber
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.statusColor

/**
 * ContinuityScene（PHASE 1D §25–28）—— 独立空间迁移场景（Compose Canvas，非 Row/Column 表格）。
 *
 * 布局（§27）：OLD NUMBER（左）← 卫星式服务节点（微信/支付宝/招商银行/腾讯视频）→ NEW NUMBER（右）；
 * 服务节点 2 列 × 2 行错位排列，Bezier 曲线从 OLD 经服务到 NEW，路径避免交叉。
 *
 * 状态语义（§28）：
 *   current    ：OLD 实线路径 + NEW ghost 目标
 *   transition ：per-service：migrated=绿实线(新侧强) / waiting=amber 虚线 /
 *                blocked=红断点 / not_started=muted 旧路径 + ghost 新目标
 *   after      ：OLD faded/retired + NEW primary（PLAN PROJECTION，不冒充 Reality）
 */
@Composable
fun ContinuityScene(
    old: UiVNextNumber,
    new: UiVNextNumber,
    services: List<SceneMigration>,
    projection: String,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val oldX = w * 0.07f
        val newX = w * 0.93f
        val cy = h * 0.5f
        val cols = listOf(w * 0.38f, w * 0.60f)
        val rows = listOf(h * 0.26f, h * 0.72f)
        val nodeW = w * 0.16f
        val nodeH = h * 0.34f

        services.forEachIndexed { i, m ->
            drawServicePaths(oldX, cy, newX, cy, cols[i % 2], rows[i / 2], m.status, projection, w)
        }
        drawNumberNode(
            textMeasurer = textMeasurer,
            center = Offset(oldX, cy),
            nodeW = nodeW,
            nodeH = nodeH,
            title = "OLD 旧号码",
            number = old.maskedNumber,
            meta = "${old.carrier} · ${roleLabelScene(old.role)}",
            emphasis = false,
            alpha = if (projection == "after") 0.30f else 1f,
            border = PdigV2Colors.BorderSubtle,
            accent = PdigV2Colors.TextMuted,
        )
        val newAlpha = if (projection == "current") 0.35f else 1f
        drawNumberNode(
            textMeasurer = textMeasurer,
            center = Offset(newX, cy),
            nodeW = nodeW,
            nodeH = nodeH,
            title = "NEW 新号码",
            number = new.maskedNumber,
            meta = "${new.carrier} · ${roleLabelScene(new.role)}",
            emphasis = projection != "current",
            alpha = newAlpha,
            border = if (projection == "after") PdigV2Colors.PrimaryBright else PdigV2Colors.PrimaryBright.copy(alpha = 0.4f),
            accent = PdigV2Colors.PrimaryBright,
        )
        services.forEachIndexed { i, m ->
            drawServiceNode(textMeasurer, cols[i % 2], rows[i / 2], m.service, m.status, projection, w, h)
        }
        if (projection == "after") {
            drawPlanProjectionBadge(textMeasurer, w)
        }
    }
}

/** OLD ──服务──▶ NEW 的双段 Bezier；waiting=虚线+amber，blocked=红断点。 */
private fun DrawScope.drawServicePaths(
    oldX: Float, oldY: Float,
    newX: Float, newY: Float,
    sx: Float, sy: Float,
    status: String,
    projection: String,
    w: Float,
) {
    val color = statusColor(status)
    val oldPath = cubicBezier(
        Offset(oldX + w * 0.08f, oldY),
        Offset(oldX + w * 0.18f, oldY),
        Offset(sx - w * 0.12f, sy),
        Offset(sx - w * 0.06f, sy),
    )
    if (projection != "after") {
        drawCubic(oldPath, color.copy(alpha = if (status == "not_started") 0.35f else 0.85f), 2.2f, dashed = status == "waiting")
    }
    val newPath = cubicBezier(
        Offset(sx + w * 0.06f, sy),
        Offset(sx + w * 0.12f, sy),
        Offset(newX - w * 0.18f, newY),
        Offset(newX - w * 0.08f, newY),
    )
    val strong = status == "migrated" || status == "blocked" || projection == "after"
    drawCubic(
        newPath,
        color.copy(alpha = if (strong) 1f else if (projection == "current") 0.5f else 0.85f),
        if (strong) 2.6f else 2.0f,
        dashed = status == "waiting",
    )
    if (status == "blocked") {
        drawLine(PdigV2Colors.Critical, Offset(sx - 6f, sy - 6f), Offset(sx + 6f, sy + 6f), strokeWidth = 2.4f)
        drawLine(PdigV2Colors.Critical, Offset(sx - 6f, sy + 6f), Offset(sx + 6f, sy - 6f), strokeWidth = 2.4f)
    }
}

private data class Cubic(val p0: Offset, val c1: Offset, val c2: Offset, val p3: Offset)

private fun cubicBezier(p0: Offset, c1: Offset, c2: Offset, p3: Offset): Cubic = Cubic(p0, c1, c2, p3)

private fun cubicAt(c: Cubic, t: Float): Offset {
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

private fun DrawScope.drawCubic(c: Cubic, color: Color, stroke: Float, dashed: Boolean) {
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

private fun DrawScope.drawNumberNode(
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
    drawRoundRect(PdigV2Colors.Surface.copy(alpha = 0.92f * alpha), Offset(left, top), Size(nodeW, nodeH), CornerRadius(16f))
    drawRoundRect(
        border.copy(alpha = border.alpha * alpha),
        Offset(left, top),
        Size(nodeW, nodeH),
        CornerRadius(16f),
        style = Stroke(width = 1.4f),
    )
    drawRect(
        Brush.horizontalGradient(listOf(accent.copy(alpha = 0.18f * alpha), Color.Transparent)),
        topLeft = Offset(left, top),
        size = Size(nodeW * 0.5f, nodeH),
    )
    val tTitle = textMeasurer.measure(
        AnnotatedString(title),
        style = TextStyle(color = if (emphasis) PdigV2Colors.PrimaryBright else PdigV2Colors.TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.W600),
    )
    val tNum = textMeasurer.measure(
        AnnotatedString(number),
        style = TextStyle(color = PdigV2Colors.TextPrimary.copy(alpha = alpha), fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
    )
    val tMeta = textMeasurer.measure(
        AnnotatedString(meta),
        style = TextStyle(color = PdigV2Colors.TextMuted.copy(alpha = alpha), fontSize = 10.sp),
    )
    drawText(tTitle, topLeft = Offset(left + 12f, top + 10f))
    drawText(tNum, topLeft = Offset(left + 12f, top + nodeH * 0.40f))
    drawText(tMeta, topLeft = Offset(left + 12f, top + nodeH * 0.70f))
}

private fun DrawScope.drawServiceNode(
    textMeasurer: TextMeasurer,
    sx: Float,
    sy: Float,
    name: String,
    status: String,
    projection: String,
    w: Float,
    h: Float,
) {
    val color = statusColor(status)
    val alpha = if (projection == "after" && status == "not_started") 0.5f else 1f
    val nodeW = w * 0.15f
    val left = sx - nodeW / 2f
    drawCircle(PdigV2Colors.SurfaceRaised.copy(alpha = alpha), radius = 13f, center = Offset(left + 18f, sy))
    drawCircle(color.copy(alpha = 0.6f * alpha), radius = 12f, center = Offset(left + 18f, sy), style = Stroke(width = 1.4f))
    val glyph = name.firstOrNull()?.toString() ?: "?"
    val g = textMeasurer.measure(
        AnnotatedString(glyph),
        style = TextStyle(color = color.copy(alpha = alpha), fontSize = 11.sp, fontWeight = FontWeight.Bold),
    )
    drawText(g, topLeft = Offset(left + 18f - g.size.width / 2f, sy - g.size.height / 2f))
    val tName = textMeasurer.measure(
        AnnotatedString(name),
        style = TextStyle(color = PdigV2Colors.TextPrimary.copy(alpha = alpha), fontSize = 12.sp, fontWeight = FontWeight.W600),
    )
    drawText(tName, topLeft = Offset(left + 34f, sy - 14f))
    val tRole = textMeasurer.measure(
        AnnotatedString(sceneStatusLabel(status)),
        style = TextStyle(color = PdigV2Colors.TextMuted.copy(alpha = alpha), fontSize = 10.sp),
    )
    drawText(tRole, topLeft = Offset(left + 34f, sy + 2f))
}

private fun DrawScope.drawPlanProjectionBadge(textMeasurer: TextMeasurer, w: Float) {
    val t = textMeasurer.measure(
        AnnotatedString("PLAN PROJECTION · 计划投影 ≠ 现实"),
        style = TextStyle(color = PdigV2Colors.Warning, fontSize = 12.sp, fontWeight = FontWeight.W600),
    )
    drawRoundRect(
        PdigV2Colors.CanvasDeep.copy(alpha = 0.85f),
        Offset(w - t.size.width - 16f, 8f),
        Size(t.size.width + 12f, t.size.height + 8f),
        CornerRadius(8f),
    )
    drawText(t, topLeft = Offset(w - t.size.width - 10f, 12f), color = PdigV2Colors.Warning)
}

private fun sceneStatusLabel(status: String): String = when (status) {
    "migrated" -> "已迁移接管"
    "waiting" -> "等待验证"
    "blocked" -> "不可停用"
    "not_started" -> "未开始"
    else -> status
}

private fun roleLabelScene(role: String): String = when (role) {
    "primary" -> "主号"
    "secondary" -> "副号"
    "keep" -> "保号"
    else -> role
}

/** 服务节点迁移数据（Change Phone 场景行）。 */
data class SceneMigration(val service: String, val status: String)
