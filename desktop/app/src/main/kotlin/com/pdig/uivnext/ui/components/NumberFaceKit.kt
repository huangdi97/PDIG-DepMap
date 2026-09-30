package com.pdig.uivnext.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.model.PresentationProfile
import com.pdig.uivnext.model.UiVNextNumber
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.theme.VSpacing
import com.pdig.uivnext.theme.VType

/**
 * NumberFace —— 号码身份面（Review §13/§14）。
 *
 *  - region flag 视觉（RegionBadge：region code 色块，不用 emoji）；
 *  - masked number 大号（majorNumber 30）+ nickname + carrier；
 *  - physical SIM/eSIM、role、usage、recovery role、status 徽标；
 *  - 可自定义（PresentationProfile<PhoneNumber>，theme 仅改变视觉呈现，
 *    与语义角色无关）。
 */

@Composable
fun NumberFace(
    number: UiVNextNumber,
    privacyMask: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    profile: PresentationProfile? = null,
) {
    val p = profile ?: PresentationProfile.defaultFor("phoneNumber", number.id, "country")
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(VRadius.Xl))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(VRadius.Xl),
        color = Color.Transparent,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .drawBehind { drawNumberFaceBackdrop(p) }
                .border(1.dp, PdigV2Colors.BorderSubtle, RoundedCornerShape(VRadius.Xl))
                .padding(VSpacing.Xxl),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RegionBadge(number.region)
                    Spacer(Modifier.width(VSpacing.Lg))
                    Column {
                        Text(number.nickname, style = VType.DisplayGlobe, color = PdigV2Colors.TextPrimary, maxLines = 1)
                        Text(
                            "${number.countryCode} · ${number.region}",
                            style = VType.Secondary,
                            color = PdigV2Colors.TextSecondary,
                            maxLines = 1,
                        )
                    }
                }
                StatusBadge(number.status)
            }
            Spacer(Modifier.height(VSpacing.Xxl))
            Text(
                if (privacyMask && p.maskSensitive) maskedFully(number.maskedNumber) else number.maskedNumber,
                style = VType.MajorNumber,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                color = PdigV2Colors.TextPrimary,
                maxLines = 1,
            )
            Spacer(Modifier.height(VSpacing.Lg))
            Row(horizontalArrangement = Arrangement.spacedBy(VSpacing.Sm)) {
                LabelChip(if (number.simKind == "eSIM") "eSIM" else "实体 SIM")
                LabelChip(roleLabel(number.role))
                LabelChip(number.carrier)
                if (number.recoveryOnly) LabelChip("唯一恢复路径", highlight = true)
            }
            if (p.layout != "minimal-content") {
                Spacer(Modifier.height(VSpacing.Lg))
                Text(
                    "用途：${number.usages.joinToString(" · ")}",
                    style = VType.Secondary,
                    color = PdigV2Colors.TextSecondary,
                    maxLines = 2,
                )
            }
        }
    }
}

private fun maskedFully(masked: String): String =
    masked.map { if (it.isDigit()) '•' else it }.joinToString("")

internal fun roleLabel(role: String): String = when (role) {
    "primary" -> "主号"
    "secondary" -> "副号"
    "keep" -> "保号"
    else -> role
}

/** RegionBadge：region code 色块（不用 emoji；色条来自 token palette）。 */
@Composable
fun RegionBadge(code: String, modifier: Modifier = Modifier, badgeSize: Dp = 40.dp) {
    val (fg, bg) = regionColors(code)
    Surface(
        modifier = modifier.size(badgeSize).clip(RoundedCornerShape(VRadius.Md)),
        color = bg,
        border = androidx.compose.foundation.BorderStroke(1.dp, fg.copy(alpha = 0.5f)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                code,
                color = fg,
                fontSize = 12.sp,
                fontWeight = FontWeight.W700,
                maxLines = 1,
            )
        }
    }
}

private fun regionColors(code: String): Pair<Color, Color> = when (code) {
    "CN" -> PdigV2Colors.PrimaryBright to PdigV2Colors.PrimarySoft
    "HK" -> PdigV2Colors.NightCityLight to PdigV2Colors.PrimarySoft
    "MO" -> PdigV2Colors.LandHighlight to PdigV2Colors.SurfaceRaised
    "GB" -> PdigV2Colors.TerminatorLight to PdigV2Colors.PrimarySoft
    "US" -> PdigV2Colors.RegionNodeHi to PdigV2Colors.SurfaceRaised
    "SG" -> PdigV2Colors.Primary to PdigV2Colors.PrimarySoft
    else -> PdigV2Colors.TextSecondary to PdigV2Colors.SurfaceRaised
}

/** 号码主题中文名（用户语言）。 */
internal fun numberThemeLabel(theme: String): String = when (theme) {
    "country" -> "国家"
    "city" -> "城市"
    "minimal" -> "极简"
    "banking" -> "银行"
    "travel" -> "旅行"
    "recovery" -> "恢复"
    "work" -> "工作"
    "private" -> "私人"
    else -> theme
}

/**
 * 确定性号码面缩略图（Studio 主题选择器用；固定尺寸 76dp、
 * 共享 drawNumberFaceBackdrop 渲染器；避免在滚动容器内使用 aspectRatio）。
 */
@Composable
fun NumberFaceThumbnail(preset: String, modifier: Modifier = Modifier) {
    val profile = PresentationProfile.defaultFor("phoneNumber", "thumb", preset)
    Box(
        modifier
            .fillMaxWidth(0.92f)
            .height(76.dp)
            .drawBehind { drawNumberFaceBackdrop(profile) }
            .border(1.dp, PdigV2Colors.BorderSubtle, RoundedCornerShape(VRadius.Sm)),
    ) {
        Column(Modifier.fillMaxSize().padding(6.dp)) {
            Text(numberThemeLabel(preset), style = VType.Meta, color = PdigV2Colors.TextSecondary, maxLines = 1)
            Spacer(Modifier.weight(1f))
            Text("+86 ••• •••", style = VType.Mono, fontSize = 9.sp, color = PdigV2Colors.TextPrimary)
        }
    }
}

/** 号码面背景：theme 预设程序化（全部 token 色）。 */
private fun DrawScope.drawNumberFaceBackdrop(p: PresentationProfile) {
    val w = size.width
    val h = size.height
    val base: Brush = when (p.themeId) {
        "country" -> Brush.linearGradient(listOf(PdigV2Colors.LandHighlight, PdigV2Colors.OceanBase, PdigV2Colors.OceanDeep))
        "city" -> Brush.linearGradient(listOf(PdigV2Colors.SurfaceRaised, PdigV2Colors.CanvasDeep))
        "banking" -> Brush.linearGradient(listOf(PdigV2Colors.Surface, PdigV2Colors.CanvasDeep))
        "travel" -> Brush.linearGradient(listOf(PdigV2Colors.TerminatorLight.copy(alpha = 0.35f), PdigV2Colors.CanvasDeep))
        "recovery" -> Brush.linearGradient(listOf(PdigV2Colors.Warning.copy(alpha = 0.22f), PdigV2Colors.CanvasDeep))
        "work" -> Brush.linearGradient(listOf(PdigV2Colors.Primary.copy(alpha = 0.30f), PdigV2Colors.Surface))
        "private" -> Brush.linearGradient(listOf(PdigV2Colors.CanvasDeep, PdigV2Colors.Surface))
        else -> Brush.linearGradient(listOf(PdigV2Colors.Surface, PdigV2Colors.CanvasDeep))
    }
    drawRect(base)
    when (p.themeId) {
        "city" -> {
            for (row in 0 until 4) {
                for (col in 0 until 12) {
                    val x = 14f + col * ((w - 28f) / 11f)
                    val y = h * (0.34f + 0.15f * row)
                    drawCircle(PdigV2Colors.NightCityLight.copy(alpha = 0.18f + (col % 4) * 0.12f), radius = 1.1f, center = Offset(x, y))
                }
            }
        }
        "travel" -> {
            drawCircle(PdigV2Colors.AtmosphereRim.copy(alpha = 0.35f), radius = w * 0.32f, center = Offset(w * 0.82f, h * 0.30f))
            drawCircle(PdigV2Colors.OceanBase.copy(alpha = 0.6f), radius = w * 0.24f, center = Offset(w * 0.16f, h * 0.82f))
        }
        "banking" -> {
            drawRect(Brush.horizontalGradient(listOf(PdigV2Colors.NightCityLight.copy(alpha = 0.14f), Color.Transparent)), size = androidx.compose.ui.geometry.Size(w * 0.6f, h))
        }
        else -> Unit
    }
    // 左缘 accent 洗色
    val accent = accentColorOf(p.accentColor)
    drawRect(
        Brush.horizontalGradient(listOf(accent.copy(alpha = 0.14f), Color.Transparent)),
        size = androidx.compose.ui.geometry.Size(w * 0.5f, h),
    )
}
