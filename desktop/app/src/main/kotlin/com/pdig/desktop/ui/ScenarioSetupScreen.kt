package com.pdig.desktop.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pdig.app.data.ScenarioPlanRequest
import com.pdig.core.scenario.ScenarioRegistry
import com.pdig.desktop.ui.components.EmptyState
import com.pdig.desktop.ui.components.InfoRow
import com.pdig.desktop.ui.components.NoticeStrip
import com.pdig.desktop.ui.components.PdigCard
import com.pdig.desktop.ui.components.PdigPage
import com.pdig.desktop.ui.components.SectionDivider

/** 场景设置：选择目标支付工具后创建变更计划。 */
@Composable
fun ScenarioSetupScreen(ui: UiState) {
    val template = ui.selectedScenarioId?.let { ScenarioRegistry.get(it) }
    PdigPage(
        title = "场景设置",
        subtitle = ui.selectedScenarioId ?: "未选择场景",
        notice = ui.notice,
        error = ui.error,
        onDismissNotice = { ui.notice = null },
        onDismissError = { ui.error = null },
    ) {
        if (template == null) {
            EmptyState("未找到场景模板。请先前往「场景中心」选择。")
            return@PdigPage
        }
        val executable = ScenarioRegistry.isExecutable(template.id)
        val instruments = ui.session.graph.nodes().filter { it.kind == "payment_instrument" }
        var targetId by remember { mutableStateOf<String?>(null) }
        var altId by remember { mutableStateOf<String?>(null) }
        Column {
            SectionDivider("场景")
            InfoRow("名称", template.title)
            InfoRow("说明", template.description)
            InfoRow("建议提前天数", template.recommendedLeadTimeDays?.let { "$it 天" } ?: "无固定建议")
            if (!executable) {
                NoticeStrip("该场景为 planned（设计稿），当前版本无法创建变更计划。")
                return@PdigPage
            }
            SectionDivider("目标支付工具（必选）")
            if (instruments.isEmpty()) {
                EmptyState("没有支付工具节点。请先导入账单或接受候选对象，再回到这里。")
            } else {
                instruments.forEach { n ->
                    SelectNodeCard(title = n.name, subtitle = n.id, selected = targetId == n.id, onSelect = { targetId = n.id })
                }
            }
            SectionDivider("替代支付工具（可选，仅记录）")
            instruments.forEach { n ->
                SelectNodeCard(title = n.name, subtitle = n.id, selected = altId == n.id, onSelect = { altId = n.id })
            }
            if (altId != null) {
                Text("替代支付工具：$altId（当前仅作记录，不参与计划生成）", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
            }
            Button(
                onClick = { createPlan(ui, template.id, targetId) },
                enabled = targetId != null,
            ) { Text("创建变更计划") }
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = { ui.screen = Screen.SCENARIOS }) { Text("返回场景中心") }
        }
    }
}

@Composable
private fun SelectNodeCard(title: String, subtitle: String, selected: Boolean, onSelect: () -> Unit) {
    PdigCard(
        title = title,
        subtitle = subtitle,
        trailing = {
            TextButton(onClick = onSelect) {
                Text(
                    if (selected) "已选择" else "选择",
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

private fun createPlan(ui: UiState, scenarioId: String, targetId: String?) {
    if (targetId == null) return
    try {
        val planId = ui.session.plans.createPlanForScenario(
            ScenarioPlanRequest(scenarioId = scenarioId, targetNodeId = targetId, effectiveDate = null),
        )
        ui.selectedPlanId = planId
        ui.screen = Screen.PLAN
    } catch (t: Throwable) {
        ui.showError(t)
    }
}