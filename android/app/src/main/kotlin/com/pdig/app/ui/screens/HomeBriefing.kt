package com.pdig.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pdig.app.ui.components.PdigCard
import com.pdig.app.ui.theme.PdigTokens

/**
 * 首页 Briefing 组件：healthy 态、薄弱点摘要、常用场景入口（UIUX_FREEZE §4）。
 * healthy 态不显示 "0 条问题" / 健康分，讲清「当前没有需要立即处理的事项 + 最近检查范围 +
 * 仍然未知的范围 + 可以主动准备的场景」。未知 ≠ healthy（spec §71）。
 */
@Composable
internal fun HealthyBriefing(
    nodeCount: Int,
    depCount: Int,
    findings: FindingsModel?,
    onScenarios: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(PdigTokens.SpaceSm)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PdigTokens.SpaceXs),
        ) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp),
            )
            Text("当前没有需要立即处理的事项", style = PdigTokens.BodyStrong)
        }
        Text(
            "最近一次检查覆盖了这些范围",
            style = PdigTokens.Label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        BriefingLine("已检查 $nodeCount 个对象。")
        BriefingLine(
            if (depCount > 0) "支付 / 恢复 / 认证的已确认关系共 $depCount 条。"
            else "还没有已确认的关系。",
        )
        val findingCount = (findings?.spof?.size ?: 0) + (findings?.shared?.size ?: 0) + (findings?.cycles?.size ?: 0)
        if (findingCount > 0) BriefingLine("薄弱点检查发现 $findingCount 处需要留意的结构。")

        Text("以下范围仍然未知", style = PdigTokens.Label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        BriefingLine("尚未确认的备用恢复路径。")
        BriefingLine("账单没有覆盖到的支付方式。")

        Text("可以主动准备的场景", style = PdigTokens.Label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(
            onClick = onScenarios,
            shape = RoundedCornerShape(PdigTokens.RadiusSm),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = PdigTokens.MinTouchTarget),
        ) {
            Box(Modifier.padding(PdigTokens.SpaceMd), contentAlignment = Alignment.CenterStart) {
                Text(
                    "更换银行卡、银行卡即将到期、注销银行卡、更换手机号都可以提前准备。",
                    style = PdigTokens.Body,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

/** 列表行内的一句话摘要（带中性色圆点，不靠颜色传达信息）。 */
@Composable
private fun BriefingLine(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(PdigTokens.SpaceSm), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(6.dp).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(3.dp)),
        )
        Text(text, style = PdigTokens.Caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** 薄弱点摘要：1-3 条强摘要 + 去 Findings；无发现时给清楚的范围说明。 */
@Composable
internal fun FindingsSummary(findings: FindingsModel?, onOpen: () -> Unit) {
    val f = findings
    val summaryLines = buildList {
        if (f == null) return@buildList
        f.spof.take(2).forEach { add("「${it.targetName}」只有唯一恢复来源") }
        f.shared.take(1).forEach { add("多个恢复方式都依赖「${it.nodeName}」") }
        f.cycles.take(1).forEach { add("发现恢复循环：${it.pathDescription}") }
    }.take(3)
    PdigCard(onClick = onOpen) {
        if (summaryLines.isEmpty()) {
            Text("暂未发现薄弱点。", style = PdigTokens.BodyStrong)
            Text(
                "薄弱点只依据已确认的现实关系判定。",
                style = PdigTokens.Caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(PdigTokens.SpaceXs)) {
                summaryLines.forEach { Text(it, style = PdigTokens.BodyStrong) }
                val total = (f?.spof?.size ?: 0) + (f?.shared?.size ?: 0) + (f?.cycles?.size ?: 0)
                Text("共 $total 项发现 · 查看薄弱点", style = PdigTokens.Caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** 常用场景入口：2 列 action row（不显示未来功能）。 */
@Composable
internal fun ScenarioEntryGrid(onScenario: (String) -> Unit) {
    val entries = listOf(
        "更换银行卡" to "replace_payment_card",
        "银行卡即将到期" to "expiring_payment_card",
        "注销银行卡" to "close_payment_instrument",
        "更换手机号" to "replace_phone_number",
    )
    entries.chunked(2).forEach { row ->
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PdigTokens.SpaceSm),
        ) {
            row.forEach { (label, templateId) ->
                Surface(
                    onClick = { onScenario(templateId) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = PdigTokens.MinTouchTarget),
                    shape = RoundedCornerShape(PdigTokens.RadiusSm),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Box(Modifier.padding(PdigTokens.SpaceMd), contentAlignment = Alignment.Center) {
                        Text(label, style = PdigTokens.BodyStrong, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}
