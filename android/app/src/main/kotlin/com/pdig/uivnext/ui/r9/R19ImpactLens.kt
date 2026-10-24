package com.pdig.uivnext.ui.r9

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.demo.UiImpactLens
import com.pdig.uivnext.demo.UiImpactTruth

/** Compact light-reference translation of the shared read-only Impact Lens. */
@Composable
internal fun R19ImpactLens(
    impact: UiImpactLens,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().testTag("pdig.r19.impact-lens"),
        color = androidx.compose.ui.graphics.Color.White,
        shape = RoundedCornerShape(17.dp),
        border = BorderStroke(1.dp, R9.Line),
    ) {
        Column(
            Modifier.padding(horizontal = 13.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Text("如果它发生变化？", color = R9.Ink, fontSize = 13.sp,
                fontWeight = FontWeight.Bold)
            Text("只使用已记录关系；未知不会被显示成安全。",
                color = R9.Muted, fontSize = 9.sp)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                R19ImpactFact(impact.confirmedDependencies.toString(), "已确认依赖",
                    Modifier.weight(1f))
                R19ImpactFact(impact.attentionFindings.toString(), "需要关注",
                    Modifier.weight(1f))
                R19ImpactFact(
                    if (impact.uniqueRecoveryPath == UiImpactTruth.CONFIRMED) "已确认" else "未知",
                    "唯一恢复路径",
                    Modifier.weight(1f),
                )
            }
            R19ImpactLine("关键账户",
                impact.criticalAccounts?.let { "$it 项" } ?: "未记录")
            R19ImpactLine("独立备用方式",
                impact.independentAlternatives?.let { "$it 条" } ?: "未记录")
            R19ImpactLine("未确认关系",
                if (impact.unknownRelationsRemain) "仍可能存在" else "无")
        }
    }
}

@Composable
private fun R19ImpactFact(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = R9.Ice,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, R9.Line.copy(alpha = .76f)),
    ) {
        Column(
            Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(value, color = R9.Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(label, color = R9.Muted, fontSize = 8.sp, maxLines = 2)
        }
    }
}

@Composable
private fun R19ImpactLine(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = R9.Muted, fontSize = 10.sp)
        Text(value, color = R9.Ink, fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold)
    }
}
