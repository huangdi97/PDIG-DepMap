package com.pdig.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * PDIG Design System —— 映射自 spec/ui/design-tokens.json（品牌唯一真相源）。
 *
 * 三端不要求像素一致，但**品牌色、语义状态色、间距/圆角/字号角色必须一致**（spec §122）。
 * 页面禁止硬编码颜色字面量；一律走 Tokens（design-tokens.json: policy.colorRule）。
 */

object PdigTokens {
    // spacing
    val SpaceXs = 4.dp
    val SpaceSm = 8.dp
    val SpaceMd = 12.dp
    val SpaceLg = 16.dp
    val SpaceXl = 24.dp
    val SpaceXxl = 32.dp

    // radius
    val RadiusSm = 8.dp
    val RadiusMd = 12.dp
    val RadiusLg = 16.dp
    val RadiusFull = 999.dp

    // 最小可点击区域（accessibility §84）
    val MinTouchTarget = 48.dp

    // typography roles
    val Display = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, lineHeight = 36.sp)
    val Title = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, lineHeight = 28.sp)
    val Headline = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 24.sp)
    val Body = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal, lineHeight = 22.sp)
    val BodyStrong = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium, lineHeight = 22.sp)
    val Caption = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal, lineHeight = 18.sp)
    val Label = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, lineHeight = 16.sp)
}

/** 语义状态色（statusRoles：状态不得只靠颜色传达 —— 必须同时给文案/图标）。 */
object PdigStatus {
    val blocked = Color(0xFFD54941)
    val reviewRequired = Color(0xFFD98E04)
    val ready = Color(0xFF2BA471)
    val verifying = Color(0xFF3B6FD8)
    val neutral = Color(0xFF8A90A6)

    fun of(key: String): Color = when (key) {
        "blocked" -> blocked
        "review_required", "needs_revalidation", "limited" -> reviewRequired
        "ready_with_known_scope", "completed" -> ready
        "verifying" -> verifying
        else -> neutral
    }

    /** 状态文案（中文；禁止把工程 enum 直接显示给用户，spec §65/§195）。 */
    fun label(key: String): String = when (key) {
        "blocked" -> "还有必须处理的事项"
        "review_required" -> "还有信息需要确认"
        "ready_with_known_scope" -> "基于当前信息，可以继续"
        "needs_revalidation" -> "需要重新检查"
        "verifying" -> "正在验证"
        "completed" -> "已完成"
        "cancelled" -> "已取消"
        "draft" -> "草稿"
        "analyzed" -> "已分析"
        "in_progress" -> "进行中"
        else -> "待确认"
    }
}

private val LightColors = lightColorScheme(
    primary = Color(0xFF4C4FD8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEEEEFB),
    onPrimaryContainer = Color(0xFF1B1D29),
    background = Color(0xFFF5F6FA),
    onBackground = Color(0xFF1B1D29),
    surface = Color.White,
    onSurface = Color(0xFF1B1D29),
    surfaceVariant = Color(0xFFF5F6FA),
    onSurfaceVariant = Color(0xFF5A5F73),
    outline = Color(0xFFE6E8F0),
    error = Color(0xFFD54941),
    secondary = Color(0xFF2BA471),
    tertiary = Color(0xFF3B6FD8),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8B8EF5),
    onPrimary = Color(0xFF12131A),
    primaryContainer = Color(0xFF242541),
    onPrimaryContainer = Color(0xFFF2F3F7),
    background = Color(0xFF12131A),
    onBackground = Color(0xFFF2F3F7),
    surface = Color(0xFF1C1E28),
    onSurface = Color(0xFFF2F3F7),
    surfaceVariant = Color(0xFF232633),
    onSurfaceVariant = Color(0xFFB4B9C9),
    outline = Color(0xFF2C2F3D),
    error = Color(0xFFE4695F),
    secondary = Color(0xFF3FBF87),
    tertiary = Color(0xFF5B8CE8),
)

@Composable
fun PDIGTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
