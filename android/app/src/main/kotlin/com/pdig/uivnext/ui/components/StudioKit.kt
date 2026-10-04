package com.pdig.uivnext.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.model.VTestIds
import com.pdig.uivnext.model.materialLabelZh
import com.pdig.uivnext.model.themeLabelZh
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.theme.VTouchTarget

/**
 * Studio 消费者组件（brief §13/§15/§16）：
 *  - [ThemeTile]：真实 visual thumbnail + 用户语言 label（不再显示内部 preset id / 统一蓝色方块）；
 *  - [StudioInspector]：consumer 语言属性面板（外观 / 背景 / 布局 / 强调 swatch / 信息 / 隐私），
 *    默认不暴露 hex / internal enum / standard / glass ID（advanced detail 只留 debug build）。
 * 全部 bundled procedural；Human 必须一眼分辨 Glass / City / Metal / Region 等主题。
 */

/** Studio 类型（决定主题标签与 thumbnail 语言）。 */
enum class StudioKind { CARD, NUMBER }

/** 属性行（值可为用户语言文本 + 可选色样 swatch；禁止 hex 文本进 UI）。 */
data class StudioPropRow(val label: String, val value: String, val swatch: Color? = null)

/**
 * ThemeTile —— 主题选择块：thumbnail（视觉）+ 用户 label。
 * [tag] 供 StudioThemeThumbnailDistinct / 契约测试定位。
 */
@Composable
fun ThemeTile(
    kind: StudioKind,
    preset: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = VTouchTarget.Min)
            .clickable(onClick = onClick)
            .testTag(VTestIds.STUDIO_THEME_TILE),
        color = if (selected) PdigV2Colors.Primary.copy(alpha = 0.28f) else PdigV2Colors.SurfaceRaised,
        shape = RoundedCornerShape(VRadius.Md),
        border = BorderStroke(1.dp, if (selected) PdigV2Colors.PrimaryBright else PdigV2Colors.BorderSubtle),
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            ThemeThumbnail(kind = kind, preset = preset, modifier = Modifier.size(width = 48.dp, height = 30.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                themeLabelZh(if (kind == StudioKind.CARD) "card" else "number", preset),
                color = if (selected) PdigV2Colors.PrimaryBright else PdigV2Colors.TextPrimary,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }
}

/**
 * ThemeThumbnail —— 主题的视觉缩略图（真实可辨几何，不是统一蓝色方块）。
 * Human 一眼可分：Glass（透光/层/弥散/玻璃条）、City（天际线/窗户）、Metal（拉丝）、
 * Region（轮廓环）、深空（星点）、极简（单线）、抽象（色块）；
 * Number：country（等高线）、city（城市）、banking（盾形）、travel（弧+点）、
 * recovery（琥珀环）、work（蓝色信号网格）、private（低调隐私）。
 */
@Composable
fun ThemeThumbnail(kind: StudioKind, preset: String, modifier: Modifier = Modifier) {
    val cardArt = if (kind == StudioKind.CARD) cardThemeInfo(preset).artwork else null
    Box(
        modifier
            .background(PdigV2Colors.SurfaceGlass, RoundedCornerShape(6.dp))
            .padding(2.dp),
    ) {
        Canvas(Modifier.fillMaxWidth().height(30.dp)) {
            val w = size.width
            val h = size.height
            if (kind == StudioKind.NUMBER) {
                drawNumberThumbnail(preset, w, h)
            } else {
                drawCardThumbnail(cardArt, w, h)
            }
        }
    }
}

// ── Card thumbnail 几何（与 CardIdentityArtwork 的 L5 主题 artwork 同构）────────

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCardThumbnail(art: CardThemeArtwork?, w: Float, h: Float) {
    when (art) {
        CardThemeArtwork.DEEP_SPACE -> {
            repeat(8) { i ->
                drawCircle(Color.White.copy(alpha = 0.5f), radius = 0.8f.dp.toPx(), center = Offset(w * ((i * 37) % 89) / 100f, h * ((i * 53) % 83) / 100f))
            }
        }
        CardThemeArtwork.REGION -> {
            listOf(0.16f, 0.3f, 0.44f).forEachIndexed { i, r ->
                drawCircle(PdigV2Colors.PrimaryBright.copy(alpha = 0.8f - i * 0.22f), radius = w * r, center = Offset(w * 0.5f, h * 0.34f), style = Stroke(width = 1.dp.toPx()))
            }
        }
        CardThemeArtwork.CITY -> {
            val base = h * 0.62f
            var x = w * 0.05f
            listOf(0.42f, 0.62f, 0.3f, 0.74f, 0.5f).forEach { hf ->
                val bw = w * 0.14f
                drawRect(Color(0xFF0E1B30), topLeft = Offset(x, base - h * hf), size = Size(bw, h * hf))
                repeat(3) { wy ->
                    repeat(2) { wx ->
                        drawRect(Color(0xFFF2B25C).copy(alpha = 0.9f), topLeft = Offset(x + bw * 0.2f + wx * bw * 0.4f, base - h * hf + 2.dp.toPx() + wy * (h * hf * 0.24f)), size = Size(bw * 0.16f, h * hf * 0.12f))
                    }
                }
                x += bw + w * 0.03f
            }
            drawRect(Color(0xFF0E1B30), topLeft = Offset(0f, base), size = Size(w, 1.5.dp.toPx()))
        }
        CardThemeArtwork.GLASS -> {
            repeat(4) { i ->
                drawCircle(Color.White.copy(alpha = 0.4f - (i % 2) * 0.15f), radius = w * (0.06f + (i % 2) * 0.05f), center = Offset(w * (0.2f + i * 0.22f), h * (0.3f + (i % 3) * 0.22f)))
            }
            drawRect(
                androidx.compose.ui.graphics.Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.5f), Color.White.copy(alpha = 0.05f)),
                    start = Offset(w * 0.2f, 0f),
                    end = Offset(w * 0.5f, h),
                ),
                topLeft = Offset(w * 0.2f, 0f),
                size = Size(w * 0.14f, h),
            )
        }
        CardThemeArtwork.METAL -> {
            repeat(6) { i ->
                drawRect(Color.White.copy(alpha = if (i % 2 == 0) 0.14f else 0.03f), topLeft = Offset(0f, h * i / 6f), size = Size(w, h / 6f + 1f))
            }
        }
        CardThemeArtwork.ABSTRACT -> {
            listOf(
                Color(0xFF7A5CFF) to Offset(w * 0.28f, h * 0.32f),
                Color(0xFFFF7AC2) to Offset(w * 0.65f, h * 0.6f),
                Color(0xFF3FD8C4) to Offset(w * 0.45f, h * 0.75f),
            ).forEach { (c, o) -> drawCircle(c.copy(alpha = 0.55f), radius = w * 0.22f, center = o) }
        }
        else -> {
            // minimal / fallback：极简单线
            drawLine(PdigV2Colors.TextMuted.copy(alpha = 0.6f), start = Offset(w * 0.1f, h * 0.82f), end = Offset(w * 0.9f, h * 0.82f), strokeWidth = 1.dp.toPx())
        }
    }
}

// ── Number thumbnail 几何（communication identity；绝不退化为银行卡）─────────

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawNumberThumbnail(preset: String, w: Float, h: Float) {
    when (preset) {
        "country" -> {
            listOf(0.14f, 0.28f, 0.42f).forEachIndexed { i, r ->
                drawCircle(PdigV2Colors.PrimaryBright.copy(alpha = 0.75f - i * 0.2f), radius = w * r, center = Offset(w * 0.5f, h * 0.5f), style = Stroke(width = 1.dp.toPx()))
            }
        }
        "city" -> {
            var x = w * 0.06f
            listOf(0.5f, 0.8f, 0.38f, 0.62f).forEach { hf ->
                drawRect(Color(0xFF16233D), topLeft = Offset(x, (1f - hf) * h), size = Size(w * 0.16f, h * hf))
                x += w * 0.22f
            }
        }
        "banking" -> {
            val cx = w * 0.5f
            val cy = h * 0.55f
            drawCircle(Color(0xFF6FE3D4).copy(alpha = 0.85f), radius = w * 0.3f, center = Offset(cx, cy), style = Stroke(width = 1.6.dp.toPx()))
            drawLine(Color(0xFF6FE3D4).copy(alpha = 0.85f), start = Offset(cx - w * 0.14f, cy), end = Offset(cx + w * 0.14f, cy), strokeWidth = 1.6.dp.toPx())
        }
        "travel" -> {
            drawArc(Color(0xFF7FB2FF).copy(alpha = 0.9f), startAngle = 180f, sweepAngle = 150f, useCenter = false, topLeft = Offset(w * 0.1f, h * 0.2f), size = Size(w * 0.8f, h * 0.8f), style = Stroke(width = 1.6.dp.toPx()))
            drawCircle(Color(0xFF7FB2FF), radius = 1.2.dp.toPx(), center = Offset(w * 0.16f, h * 0.7f))
        }
        "recovery" -> {
            drawCircle(Color(0xFFFFC864).copy(alpha = 0.95f), radius = w * 0.26f, center = Offset(w * 0.5f, h * 0.5f), style = Stroke(width = 2.dp.toPx()))
            drawCircle(Color(0xFFFFC864).copy(alpha = 0.3f), radius = w * 0.18f, center = Offset(w * 0.5f, h * 0.5f))
        }
        "work" -> {
            // 结构化蓝色信号网格（3×3 点）
            repeat(3) { row ->
                repeat(3) { col ->
                    drawCircle(Color(0xFF9FC7FF), radius = 1.2.dp.toPx(), center = Offset(w * (0.25f + col * 0.25f), h * (0.25f + row * 0.25f)))
                }
            }
        }
        "private" -> {
            drawCircle(Color(0xFFC8A7FF).copy(alpha = 0.35f), radius = w * 0.28f, center = Offset(w * 0.5f, h * 0.55f))
            drawCircle(Color(0xFFC8A7FF).copy(alpha = 0.9f), radius = 1.6.dp.toPx(), center = Offset(w * 0.5f, h * 0.55f))
        }
        else -> {
            // minimal：极简单线
            drawLine(PdigV2Colors.TextMuted.copy(alpha = 0.6f), start = Offset(w * 0.1f, h * 0.82f), end = Offset(w * 0.9f, h * 0.82f), strokeWidth = 1.dp.toPx())
        }
    }
}
