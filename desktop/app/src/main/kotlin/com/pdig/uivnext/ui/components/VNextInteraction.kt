package com.pdig.uivnext.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.pdig.uivnext.theme.PdigV2Colors

/**
 * 交互状态工具（PHASE 1E §41 / AC13）：hover / focus-visible / pressed / selected
 * 三态共用于 8 类元素（Region node / Card / Number row / Bottom dock action /
 * Theme tile / Material tile / Service node / Scene projection selector）。
 *
 * 所有视觉 = 状态驱动（不靠猜测）；离屏证据用 interaction source 状态 + probe。
 */

/** 创建每个元素的 interaction source（remember 稳定）。 */
@Composable
fun rememberVNextInteractionSource(): MutableInteractionSource = remember { MutableInteractionSource() }

/** 汇总 hover / pressed 状态（focus 由 Modifier.focusable + LocalFocusOwner 提供）。 */
class VNextInteractionState(
    val hovered: Boolean,
    val pressed: Boolean,
) {
    val emphasized: Boolean get() = hovered || pressed
    val activeAlpha: Float get() = if (hovered) 0.16f else if (pressed) 0.22f else 0f
}

@Composable
fun collectVNextInteraction(source: MutableInteractionSource): VNextInteractionState {
    val hovered by source.collectIsHoveredAsState()
    val pressed by source.collectIsPressedAsState()
    return VNextInteractionState(hovered = hovered, pressed = pressed)
}

/** 标准交互表面：hover = 底色提升，pressed = 更暗/更强，selected = 边框强调。 */
@Composable
fun InteractionSurface(
    source: MutableInteractionSource,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    baseColor: Color = Color.Transparent,
    hoverColor: Color = PdigV2Colors.Primary.copy(alpha = 0.14f),
    pressColor: Color = PdigV2Colors.Primary.copy(alpha = 0.22f),
    selectedColor: Color = PdigV2Colors.PrimaryBright.copy(alpha = 0.16f),
    borderSelected: Color = PdigV2Colors.PrimaryBright.copy(alpha = 0.4f),
    radius: CornerRadius = CornerRadius(8f),
    content: @Composable () -> Unit,
) {
    val state = collectVNextInteraction(source)
    val fill = when {
        selected -> selectedColor
        state.pressed -> pressColor
        state.hovered -> hoverColor
        else -> baseColor
    }
    androidx.compose.foundation.layout.Box(
        modifier
            .drawBehind {
                if (fill != Color.Transparent) {
                    drawRoundRect(color = fill, cornerRadius = radius)
                }
                val borderColor = when {
                    selected -> borderSelected
                    state.pressed -> PdigV2Colors.BorderStrong.copy(alpha = 0.7f)
                    state.hovered -> PdigV2Colors.BorderStrong.copy(alpha = 0.5f)
                    else -> null
                }
                if (borderColor != null) {
                    drawRoundRect(
                        color = borderColor,
                        cornerRadius = radius,
                        style = Stroke(width = 1.dp.toPx()),
                        topLeft = Offset(0.5f, 0.5f),
                        size = Size(size.width - 1f, size.height - 1f),
                    )
                }
            },
    ) {
        content()
    }
}
