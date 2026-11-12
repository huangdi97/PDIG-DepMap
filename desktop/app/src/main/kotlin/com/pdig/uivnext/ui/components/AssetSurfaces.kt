package com.pdig.uivnext.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.model.ChangeStage
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.theme.VSpacing
import com.pdig.uivnext.theme.VType
import com.pdig.uivnext.theme.statusColor
import com.pdig.uivnext.theme.statusLabelZh

/** LabelChip：小标签（用途/角色/徽标）。 */
@Composable
fun LabelChip(text: String, highlight: Boolean = false, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = if (highlight) PdigV2Colors.Warning.copy(alpha = 0.16f) else PdigV2Colors.PrimarySoft,
        shape = RoundedCornerShape(VRadius.Sm),
    ) {
        Text(
            text,
            Modifier.padding(horizontal = VSpacing.Sm, vertical = 3.dp),
            style = VType.Label,
            color = if (highlight) PdigV2Colors.Warning else PdigV2Colors.TextSecondary,
        )
    }
}

/**
 * ContinuityRail：6 阶段 make-before-break 结构（G12 flagship）。
 * completed（实勾）/ verifying（时钟）/ blocked（! + 明文原因）/ not_started（灰编号）。
 * 每个阶段 = icon + label + color 三通道；disabled 必须附原因文案。
 */
@Composable
fun ContinuityRail(
    stages: List<ChangeStage>,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(VSpacing.Lg)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(VSpacing.Sm)) {
            stages.forEachIndexed { index, stage ->
                RailNode(stage)
                if (index < stages.lastIndex) {
                    Box(
                        Modifier
                            .weight(1f)
                            .height(2.dp)
                            .padding(top = 18.dp)
                            .background(
                                if (stage.status == "completed") PdigV2Colors.Positive else PdigV2Colors.BorderSubtle,
                                RoundedCornerShape(1.dp),
                            ),
                    )
                }
            }
        }
        // 闸门/原因说明块（blocked 步骤；明文原因，绝不只禁用）
        stages.firstOrNull { it.status == "blocked" }?.let { blocked ->
            Surface(color = PdigV2Colors.Critical.copy(alpha = 0.12f), shape = RoundedCornerShape(VRadius.Md), modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(horizontal = VSpacing.Lg, vertical = VSpacing.Md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Error, contentDescription = null, tint = PdigV2Colors.Critical, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(VSpacing.Sm))
                    Text(
                        blocked.blockReason ?: "需要先完成前置步骤",
                        style = VType.Secondary,
                        color = PdigV2Colors.TextPrimary,
                    )
                }
            }
        }
    }
}

@Composable
private fun RailNode(stage: ChangeStage) {
    val color = statusColor(stage.status)
    val label = statusLabelZh(stage.status)
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(84.dp)) {
        Box(
            Modifier
                .size(36.dp)
                .background(
                    if (stage.status == "completed") color else PdigV2Colors.SurfaceRaised,
                    CircleShape,
                )
                .border(2.dp, color, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            when (stage.status) {
                "completed" -> Icon(Icons.Filled.Check, contentDescription = null, tint = PdigV2Colors.CanvasDeep, modifier = Modifier.size(18.dp))
                "blocked" -> Icon(Icons.Filled.Error, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                "verifying" -> Icon(Icons.Filled.Schedule, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                else -> Text("${stage.stage}", color = color, fontSize = 15.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            stageLabel(stage.key),
            style = VType.Label,
            color = if (stage.status == "not_started") PdigV2Colors.TextMuted else PdigV2Colors.TextPrimary,
            maxLines = 2,
            textAlign = TextAlign.Center,
        )
        Text(
            label,
            fontSize = 12.sp,
            color = color,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

private fun stageLabel(key: String): String = when (key) {
    "impact-analysis" -> "影响分析"
    "establish-new-number" -> "建立新号码"
    "verify-new-number" -> "验证新号码"
    "migrate-key-accounts" -> "迁移关键账户"
    "check-recovery-paths" -> "检查恢复路径"
    "retire-old-number" -> "停用旧号码"
    else -> key
}
