package com.pdig.desktop.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * PDIG Desktop Theme —— 映射自 spec/ui/design-tokens.json v1.1（Quiet Infrastructure）。
 *
 * 页面禁止硬编码颜色字面量；一律从 [MaterialTheme.colorScheme] / [PdigDesktopTokens] 取。
 * Brand primary（light #4C4FD8 / dark #8B8EF5）与 status 语义色保持与 tokens 一致。
 */
object PdigDesktopTokens {
    // spacing（4pt grid）
    val SpaceXs = 4.dp
    val SpaceSm = 8.dp
    val SpaceMd = 12.dp
    val SpaceLg = 16.dp
    val SpaceXl = 20.dp
    val SpaceXxl = 24.dp
    val SpaceXxxl = 32.dp

    // radius（分层：chip 6 / card-sidebar 8 / dialog 12）
    val RadiusSm = 6.dp
    val RadiusMd = 8.dp
    val RadiusLg = 12.dp

    // 组件尺寸
    val SidebarWidth = 220.dp
    val ContentMaxWidth = 1240.dp
    val InfraMasterWidth = 300.dp
    val ListRowHeight = 40.dp
    val ButtonHeight = 40.dp
    val MinTouchDesktop = 36.dp
    val IconSize = 16.dp
    val StatusIconSize = 14.dp
    val EmptyStateIcon = 24.dp
}

private val LightColors = lightColorScheme(
    primary = Color(0xFF4C4FD8),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEEEEFB),
    onPrimaryContainer = Color(0xFF1B1D29),
    background = Color(0xFFF5F6FA),
    onBackground = Color(0xFF1B1D29),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1B1D29),
    surfaceVariant = Color(0xFFEDEFF6),
    onSurfaceVariant = Color(0xFF5A5F73),
    outline = Color(0xFFE6E8F0),
    outlineVariant = Color(0xFFE6E8F0),
    error = Color(0xFFD54941),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFCEDEC),
    onErrorContainer = Color(0xFF7A2823),
    secondary = Color(0xFF2BA471),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8F6F0),
    onSecondaryContainer = Color(0xFF0F4632),
    tertiary = Color(0xFF3B6FD8),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFEAF1FC),
    onTertiaryContainer = Color(0xFF1E3A78),
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
    outlineVariant = Color(0xFF2C2F3D),
    error = Color(0xFFE4695F),
    onError = Color(0xFF12131A),
    errorContainer = Color(0xFF4A1F1C),
    onErrorContainer = Color(0xFFF6C9C4),
    secondary = Color(0xFF3FBF87),
    onSecondary = Color(0xFF0C2A1C),
    secondaryContainer = Color(0xFF163B2B),
    onSecondaryContainer = Color(0xFFB8E8D0),
    tertiary = Color(0xFF5B8CE8),
    onTertiary = Color(0xFF0F1F44),
    tertiaryContainer = Color(0xFF1C2E58),
    onTertiaryContainer = Color(0xFFC9D8F6),
)

/** 状态色角色（对应 tokens.statusRoles + statusIcons：必须 icon+label+color，不得只靠颜色）。 */
object PdigStatusColors {
    val blocked = Color(0xFFD54941)       // dark: #E4695F
    val reviewRequired = Color(0xFFD98E04) // dark: #E0A63A
    val verifying = Color(0xFF3B6FD8)     // dark: #5B8CE8
    val ready = Color(0xFF2BA471)         // dark: #3FBF87
    val completed = Color(0xFF8A90A6)     // textTertiary-ish: completed(青灰) 弱于 verified
    val unknown = Color(0xFF8A90A6)
    val cancelled = Color(0xFF8A90A6)

    /** 按 status wire 取前景色（轻量；未识别→neutral）。 */
    fun of(wire: String): Color = when (wire) {
        "blocked" -> blocked
        "review_required", "needs_revalidation", "limited" -> reviewRequired
        "verifying", "partial" -> verifying
        "verified", "ready_with_known_scope", "well_evidenced" -> ready
        "completed" -> completed
        "cancelled" -> cancelled
        else -> unknown
    }

    /** 状态图标记（对应 tokens.statusIcons；仅语义名，图标由 UI 层映射）。 */
    fun iconOf(wire: String): String = when (wire) {
        "blocked" -> "error_circle"
        "review_required", "needs_revalidation", "limited" -> "warning_triangle"
        "verifying", "partial" -> "clock"
        "verified" -> "check_circle_double"
        "ready_with_known_scope", "well_evidenced" -> "check_circle"
        "completed" -> "check_single"
        "cancelled" -> "minus"
        "unknown" -> "question"
        else -> "circle_quiet"
    }
}

/** 状态背景页：softBackgrounds（light；枚举取值，不现场合成）。 */
object PdigSoftBackgrounds {
    fun of(wire: String): Color = when (wire) {
        "blocked" -> Color(0xFFFCEDEC)
        "review_required", "needs_revalidation", "limited" -> Color(0xFFFCF4E3)
        "verifying", "partial" -> Color(0xFFEAF1FC)
        "verified", "ready_with_known_scope", "well_evidenced" -> Color(0xFFE8F6F0)
        "completed", "cancelled" -> Color(0xFFF0F1F5)
        else -> Color(0xFFF0F1F5)
    }
}

/** 桌面字体层级（system CJK；禁远程字体）。 */
object PdigType {
    val PageTitle = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold, lineHeight = 32.sp, fontFamily = FontFamily.Default)
    val SectionTitle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp, fontFamily = FontFamily.Default)
    val Body = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp, fontFamily = FontFamily.Default)
    val Secondary = TextStyle(fontSize = 12.5.sp, fontWeight = FontWeight.Normal, lineHeight = 18.sp, fontFamily = FontFamily.Default)
    val Meta = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal, lineHeight = 16.sp, fontFamily = FontFamily.Default)
    val Label = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, lineHeight = 16.sp, fontFamily = FontFamily.Default)
    val Button = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 20.sp, fontFamily = FontFamily.Default)
    val Mono = TextStyle(fontSize = 12.5.sp, fontWeight = FontWeight.Normal, fontFamily = FontFamily.Monospace)
}

@Composable
fun PDIGTheme(
    darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = androidx.compose.material3.Typography(
            headlineSmall = PdigType.PageTitle,
            titleMedium = PdigType.SectionTitle,
            bodyLarge = PdigType.Body,
            bodyMedium = PdigType.Body,
            bodySmall = PdigType.Secondary,
            labelSmall = PdigType.Label,
            labelMedium = PdigType.Button,
            labelLarge = PdigType.Button,
        ),
        content = content,
    )
}