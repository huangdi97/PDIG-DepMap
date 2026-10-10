package com.pdig.desktop.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.desktop.ui.theme.PdigDesktopTokens as T
import com.pdig.desktop.ui.theme.PdigStatusColors
import com.pdig.desktop.ui.theme.PdigSoftBackgrounds
import com.pdig.desktop.ui.theme.PdigType

/** 屏面骨架：标题 + 副标题 + 状态条 + 内容区（Quiet Infrastructure：内容 max-width、section gap、hairline 分隔）。 */
@Composable
fun PdigPage(
    title: String,
    subtitle: String? = null,
    notice: String? = null,
    error: String? = null,
    onDismissNotice: (() -> Unit)? = null,
    onDismissError: (() -> Unit)? = null,
    scrollable: Boolean = true,
    content: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        // 页头
        Column(Modifier.fillMaxWidth().padding(start = T.SpaceXxl, end = T.SpaceXxl, top = T.SpaceXl, bottom = T.SpaceLg)) {
            Text(title, style = PdigType.PageTitle)
            if (subtitle != null) {
                Spacer(Modifier.height(T.SpaceXs))
                Text(subtitle, style = PdigType.Secondary, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
        Column(Modifier.fillMaxWidth().padding(start = T.SpaceXxl, end = T.SpaceXxl, top = T.SpaceLg)) {
            if (notice != null) NoticeStrip(notice, onDismissNotice)
            if (error != null) ErrorStrip(error, onDismissError)
            val scrollModifier = if (scrollable) {
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            } else {
                Modifier.fillMaxWidth()
            }
            // 内容 max-width：桌面不拉成全宽长条
            Box(
                Modifier
                    .width(T.ContentMaxWidth)
                    .let { m -> if (scrollable) m.fillMaxSize() else m }
                    .then(scrollModifier),
            ) {
                Column(Modifier.padding(bottom = T.SpaceXxxl)) { content() }
            }
        }
    }
}

@Composable
fun NoticeStrip(text: String, onDismiss: (() -> Unit)? = null) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(T.RadiusMd)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = T.SpaceLg, vertical = T.SpaceMd), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.WarningAmber, contentDescription = "提示", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(T.SpaceSm))
            Text(text, style = PdigType.Body, color = MaterialTheme.colorScheme.onPrimaryContainer)
            if (onDismiss != null) {
                Spacer(Modifier.width(T.SpaceSm))
                Text("关闭", Modifier.clickable { onDismiss() }, color = MaterialTheme.colorScheme.primary, style = PdigType.Label)
            }
        }
    }
    Spacer(Modifier.height(T.SpaceSm))
}

@Composable
fun ErrorStrip(text: String, onDismiss: (() -> Unit)? = null) {
    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(T.RadiusMd)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = T.SpaceLg, vertical = T.SpaceMd), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.ErrorOutline, contentDescription = "错误", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(T.SpaceSm))
            Text(text, style = PdigType.Body, color = MaterialTheme.colorScheme.onErrorContainer)
            if (onDismiss != null) {
                Spacer(Modifier.width(T.SpaceSm))
                Text("知道了", Modifier.clickable { onDismiss() }, color = MaterialTheme.colorScheme.primary, style = PdigType.Label)
            }
        }
    }
    Spacer(Modifier.height(T.SpaceSm))
}

/**
 * 区块头：文字 + hairline（替代「每节彩色底」）。
 * @param action 右侧可选操作（如「查看全部」）。
 */
@Composable
fun SectionHeader(text: String, action: (@Composable () -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().padding(top = T.SpaceXxl, bottom = T.SpaceMd),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = PdigType.SectionTitle, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(T.SpaceLg))
        HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
        action?.invoke()
    }
}

/** 兼容旧调用：SectionDivider 即 SectionHeader。 */
@Composable
fun SectionDivider(text: String) = SectionHeader(text)

@Composable
fun PdigCard(
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    // surface + 1dp border + 微弱阴影；悬停轻底色（卡片只在真正独立对象上用）
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Surface(
        modifier = Modifier.fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(T.RadiusMd))
            .let { if (onClick != null) it.clickable(interactionSource = interaction, indication = null) { onClick() } else it },
        color = if (hovered && onClick != null) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(T.RadiusMd),
        shadowElevation = if (onClick != null) 1.dp else 0.dp,
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = T.SpaceLg, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = PdigType.Body, fontWeight = FontWeight.SemiBold)
                if (subtitle != null) {
                    Spacer(Modifier.height(3.dp))
                    Text(subtitle, style = PdigType.Secondary, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 20.sp)
                }
            }
            trailing?.invoke()
        }
    }
    Spacer(Modifier.height(T.SpaceSm))
}

/** 状态小徽标：icon + label + color（绝不只靠颜色）。 */
@Composable
fun StatusBadge(label: String, wire: String, compact: Boolean = false) {
    val color = PdigStatusColors.of(wire)
    val icon = statusIconOf(wire)
    Row(
        Modifier
            .background(PdigSoftBackgrounds.of(wire), RoundedCornerShape(T.RadiusSm))
            .padding(horizontal = if (compact) 6.dp else 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        icon?.let {
            Icon(it, contentDescription = "状态：$label", tint = color, modifier = Modifier.size(T.StatusIconSize))
        }
        Text(label, style = PdigType.Label, color = color, fontWeight = FontWeight.Medium)
    }
}

/** 兼容旧调用：StatusChip 保留（纯色块 + 文字），新代码用 StatusBadge。 */
@Composable
fun StatusChip(label: String, tone: ChipTone = ChipTone.NEUTRAL) {
    val color = when (tone) {
        ChipTone.GOOD -> MaterialTheme.colorScheme.secondaryContainer
        ChipTone.WARN -> MaterialTheme.colorScheme.tertiaryContainer
        ChipTone.BAD -> MaterialTheme.colorScheme.errorContainer
        ChipTone.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant
    }
    Surface(color = color, shape = RoundedCornerShape(T.RadiusSm)) {
        Text(label, Modifier.padding(horizontal = 8.dp, vertical = 3.dp), style = PdigType.Label)
    }
}

private fun statusIconOf(wire: String): ImageVector? = when (wire) {
    "blocked" -> Icons.Filled.ErrorOutline
    "review_required", "needs_revalidation", "limited" -> Icons.Filled.WarningAmber
    "verifying", "partial" -> Icons.Filled.HourglassEmpty
    "verified" -> Icons.Filled.DoneAll
    "ready_with_known_scope", "well_evidenced" -> Icons.Filled.CheckCircle
    "completed" -> Icons.Filled.Check
    "cancelled" -> null
    "unknown" -> Icons.Filled.HelpOutline
    else -> null
}

enum class ChipTone { GOOD, WARN, BAD, NEUTRAL }

/** EmptyState：这里是什么 / 为什么为空 / 下一步能做什么。 */
@Composable
fun EmptyState(message: String) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = T.SpaceXxxl).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(T.RadiusMd))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(T.RadiusMd)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Filled.HelpOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(T.EmptyStateIcon),
        )
        Spacer(Modifier.height(T.SpaceMd))
        Text(message, style = PdigType.Body, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun EmptyState(message: String, title: String, next: String? = null) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = T.SpaceXxxl).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(T.RadiusMd))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(T.RadiusMd)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Filled.HelpOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(T.EmptyStateIcon),
        )
        Spacer(Modifier.height(T.SpaceMd))
        Text(title, style = PdigType.Body, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(T.SpaceXs))
        Text(message, style = PdigType.Secondary, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (next != null) {
            Spacer(Modifier.height(T.SpaceSm))
            Text(next, style = PdigType.Label, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
        Text(label, Modifier.weight(0.42f), style = PdigType.Secondary, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, Modifier.weight(0.58f), style = PdigType.Body, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun ActionRow(
    title: String,
    description: String,
    onAction: () -> Unit,
    actionLabel: String,
    enabled: Boolean = true,
    tone: ChipTone = ChipTone.NEUTRAL,
) {
    PdigCard(title = title, subtitle = description) {
        androidx.compose.material3.TextButton(onClick = onAction, enabled = enabled) {
            Text(actionLabel)
        }
    }
}
/** 列表行（hairline 分隔，非卡片）：icon/状态 + 标题 + 描述 + 右侧动作。 */
@Composable
fun PdigRow(
    title: String,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    divider: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .let { if (onClick != null) it.clickable(interactionSource = interaction, indication = null) { onClick() } else it }
                .background(
                    when {
                        hovered && onClick != null -> MaterialTheme.colorScheme.surfaceVariant
                        else -> MaterialTheme.colorScheme.surface
                    },
                    RoundedCornerShape(6.dp),
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leading?.invoke()
            if (leading != null) Spacer(Modifier.width(T.SpaceMd))
            Column(Modifier.weight(1f)) {
                Text(title, style = PdigType.Body, fontWeight = FontWeight.SemiBold, maxLines = 1)
                if (subtitle != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(subtitle, style = PdigType.Secondary, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
            trailing?.invoke()
        }
        if (divider) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    }
}

/** 去往箭头（行尾）。 */
@Composable
fun ChevronTrailing() {
    Icon(
        Icons.Filled.ArrowForward,
        contentDescription = "前往",
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(16.dp),
    )
}

/** 可展开内容块（Events/Evidence 等）。 */
@Composable
fun ExpandableBlock(title: String, defaultExpanded: Boolean = false, content: @Composable () -> Unit) {
    var expanded by remember { mutableStateOf(defaultExpanded) }
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(if (expanded) "▾" else "▸", color = MaterialTheme.colorScheme.onSurfaceVariant, style = PdigType.Label)
            Spacer(Modifier.width(6.dp))
            Text(title, style = PdigType.Label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
        }
        AnimatedVisibility(visible = expanded, enter = fadeIn(), exit = fadeOut()) {
            Column(Modifier.padding(start = 18.dp, bottom = 4.dp)) { content() }
        }
    }
}

/** 有编号与动作的清单行。 */
@Composable
fun IndexedRow(index: Int, title: String, subtitle: String? = null, trailing: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
        Text(
            index.toString().padStart(2, '0'),
            Modifier.width(28.dp),
            style = PdigType.Mono,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(Modifier.weight(1f)) {
            Text(title, style = PdigType.Body, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = PdigType.Secondary, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        trailing?.invoke()
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}

/**
 * Continuity Rail —— PDIG 唯一 Signature Motif（spec §22）。
 * 语义：current state → prerequisite → transition → verification checkpoint → completion。
 *
 * @param steps 步骤表：(id, label, 状态)。colored=info/warning/success/strike/disabled。
 * @param blockedReason 若某步骤被闸门挡住，需给出 plain-language 原因（不止 disabled）。
 */
@Composable
fun ContinuityRail(
    steps: List<ContinuityStep>,
    blockedReason: String? = null,
) {
    Column(Modifier.fillMaxWidth()) {
        steps.forEachIndexed { i, step ->
            val isLast = i == steps.lastIndex
            Row(Modifier.fillMaxHeight().padding(vertical = 3.dp)) {
                // 轨道线 + 节点（放大到 28dp，带编号，成为真 signature）
                Column(Modifier.width(36.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    RailNode(step, index = i + 1)
                    if (!isLast) {
                        Box(
                            Modifier
                                .width(3.dp)
                                .weight(1f)
                                .background(
                                    when (step.state) {
                                        RailState.SUCCESS, RailState.VERIFIED -> MaterialTheme.colorScheme.secondary
                                        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.9f)
                                    },
                                ),
                        )
                    }
                }
                Spacer(Modifier.width(T.SpaceLg))
                Column(Modifier.padding(bottom = if (isLast) 0.dp else 14.dp).weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            step.label,
                            style = PdigType.Body,
                            fontWeight = when (step.state) {
                                RailState.SUCCESS, RailState.VERIFIED, RailState.ACTIVE -> FontWeight.SemiBold
                                else -> FontWeight.Normal
                            },
                            color = when (step.state) {
                                RailState.BLOCKED -> MaterialTheme.colorScheme.error
                                RailState.VERIFIED -> MaterialTheme.colorScheme.secondary
                                else -> MaterialTheme.colorScheme.onSurface
                            },
                        )
                        Spacer(Modifier.width(T.SpaceSm))
                        step.stateLabel?.let { StatusBadge(it, step.state.wire, compact = true) }
                    }
                    step.description?.let {
                        Spacer(Modifier.height(3.dp))
                        Text(it, style = PdigType.Secondary, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    step.content?.invoke()
                    if (step.state == RailState.BLOCKED) {
                        blockedReason?.let {
                            Spacer(Modifier.height(5.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.WarningAmber, contentDescription = "原因", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(it, style = PdigType.Secondary, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}

data class ContinuityStep(
    val label: String,
    val state: RailState = RailState.UPCOMING,
    val description: String? = null,
    val stateLabel: String? = null,
    val content: (@Composable () -> Unit)? = null,
)

enum class RailState(val wire: String) {
    /** 已完成（成功） */
    SUCCESS("completed"),
    /** 已验证（强于 completed） */
    VERIFIED("verified"),
    /** 当前/进行中 */
    ACTIVE("verifying"),
    /** 被闸门阻止：必须说明原因 */
    BLOCKED("blocked"),
    /** 即将到来 */
    UPCOMING("unknown"),
}

@Composable
private fun RailNode(step: ContinuityStep, index: Int) {
    val color = when (step.state) {
        RailState.SUCCESS, RailState.VERIFIED -> MaterialTheme.colorScheme.secondary
        RailState.ACTIVE -> MaterialTheme.colorScheme.primary
        RailState.BLOCKED -> MaterialTheme.colorScheme.error
        RailState.UPCOMING -> MaterialTheme.colorScheme.outline
    }
    Box(
        Modifier
            .size(30.dp)
            .background(circleBg(step.state), CircleShape)
            .border(if (step.state == RailState.ACTIVE) 2.dp else 1.dp, color, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        when (step.state) {
            RailState.SUCCESS -> Icon(Icons.Filled.Check, contentDescription = "完成", tint = Color.White, modifier = Modifier.size(16.dp))
            RailState.VERIFIED -> Icon(Icons.Filled.DoneAll, contentDescription = "已验证", tint = Color.White, modifier = Modifier.size(16.dp))
            RailState.BLOCKED -> Icon(Icons.Filled.ErrorOutline, contentDescription = "受阻", tint = Color.White, modifier = Modifier.size(16.dp))
            RailState.ACTIVE -> Text(index.toString(), style = PdigType.Label, color = Color.White, fontWeight = FontWeight.Bold)
            RailState.UPCOMING -> Text(index.toString(), style = PdigType.Meta, color = MaterialTheme.colorScheme.outline, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun circleBg(state: RailState): Color = when (state) {
    RailState.SUCCESS, RailState.VERIFIED -> MaterialTheme.colorScheme.secondary
    RailState.BLOCKED -> MaterialTheme.colorScheme.error
    RailState.ACTIVE -> MaterialTheme.colorScheme.primary
    RailState.UPCOMING -> Color.Transparent
}

/** 每步一个小 CTA（验证/查看影响等），确保 rail 步骤可操作。 */
@Composable
fun RailAction(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    androidx.compose.material3.TextButton(onClick = onClick, enabled = enabled, modifier = Modifier.height(36.dp)) {
        Text(label, style = PdigType.Button)
    }
}