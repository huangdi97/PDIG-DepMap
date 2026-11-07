package com.pdig.uivnext.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.demo.UiImpactLens
import com.pdig.uivnext.demo.UiImpactTruth
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.theme.VTouchTarget

/**
 * Consumer-facing Impact Lens.
 *
 * The component renders unknowns as unknowns. Null never means zero and an
 * absent alternative path never becomes a "safe" verdict.
 */
@Composable
fun ObjectImpactLens(
    impact: UiImpactLens,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("pdig.r19.impact-lens"),
        color = PdigV2Colors.Surface,
        shape = RoundedCornerShape(VRadius.Xl),
        border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    "如果它发生变化？",
                    color = PdigV2Colors.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "只基于已记录关系；未确认内容不会被推断为安全或必须处理。",
                    color = PdigV2Colors.TextMuted,
                    fontSize = 10.sp,
                )
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                ImpactFact(
                    impact.confirmedDependencies.toString(),
                    "已确认依赖",
                    Modifier.weight(1f),
                )
                ImpactFact(
                    impact.attentionFindings.toString(),
                    "需要关注",
                    Modifier.weight(1f),
                )
                ImpactFact(
                    when (impact.uniqueRecoveryPath) {
                        UiImpactTruth.CONFIRMED -> "已确认"
                        UiImpactTruth.UNKNOWN -> "未知"
                    },
                    "唯一恢复路径",
                    Modifier.weight(1f),
                )
            }

            ImpactLine(
                "关键账户",
                impact.criticalAccounts?.let { "$it 项" } ?: "未记录",
            )
            ImpactLine(
                "独立备用方式",
                impact.independentAlternatives?.let { "$it 条" } ?: "未记录",
            )
            ImpactLine(
                "未确认关系",
                if (impact.unknownRelationsRemain) "仍可能存在" else "无",
            )

            if (actionLabel != null && onAction != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = VTouchTarget.Min)
                        .clickable(onClick = onAction)
                        .testTag("pdig.r19.impact-lens.action"),
                    color = PdigV2Colors.PrimarySoft,
                    shape = RoundedCornerShape(VRadius.Md),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            actionLabel,
                            color = PdigV2Colors.PrimaryText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text("→", color = PdigV2Colors.PrimaryText, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ImpactFact(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = PdigV2Colors.SurfaceRaised,
        shape = RoundedCornerShape(VRadius.Md),
        border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        Column(
            Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                value,
                color = PdigV2Colors.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(label, color = PdigV2Colors.TextMuted, fontSize = 9.sp)
        }
    }
}

@Composable
private fun ImpactLine(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = PdigV2Colors.TextMuted, fontSize = 11.sp)
        Text(value, color = PdigV2Colors.TextPrimary, fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold)
    }
}
