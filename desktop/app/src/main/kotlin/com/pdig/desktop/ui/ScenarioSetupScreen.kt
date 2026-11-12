package com.pdig.desktop.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pdig.app.data.ScenarioPlanRequest
import com.pdig.core.scenario.ScenarioRegistry
import com.pdig.desktop.ui.components.ContinuityRail
import com.pdig.desktop.ui.components.ContinuityStep
import com.pdig.desktop.ui.components.EmptyState
import com.pdig.desktop.ui.components.InfoRow
import com.pdig.desktop.ui.components.PdigPage
import com.pdig.desktop.ui.components.PdigRow
import com.pdig.desktop.ui.components.RailState
import com.pdig.desktop.ui.components.SectionHeader
import com.pdig.desktop.ui.theme.PdigDesktopTokens as T
import com.pdig.desktop.ui.theme.PdigType

/**
 * 场景设置（spec §30–§32）：
 * - replace_phone_number → flagship journey：Continuity Rail 展示全链（选旧号 → 查看影响 →
 *   恢复路径 → 共享故障点 → 建立新路径 → 验证 → 迁移关键账户 → 再次验证 → 停用旧号），
 *   未验证时「停用旧手机号」步骤 blocked + 明文原因。
 * - 其余支付场景 → 选择支付工具。
 * 不泄漏 wire / node id（用对象名称）。
 */
@Composable
fun ScenarioSetupScreen(ui: UiState) {
    val template = ui.selectedScenarioId?.let { ScenarioRegistry.get(it) }
    PdigPage(
        title = "场景设置",
        subtitle = template?.title ?: "未选择场景",
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
        val isPhoneScenario = template.id == "replace_phone_number"
        val candidates = ui.session.graph.nodes().filter { n ->
            if (isPhoneScenario) n.kind == "identity_anchor" else n.kind == "payment_instrument"
        }
        var targetId by remember { mutableStateOf<String?>(null) }
        var altId by remember { mutableStateOf<String?>(null) }
        Column(Modifier.fillMaxWidth()) {
            // 场景信息
            SectionHeader("场景")
            Surface(
                Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(T.RadiusMd),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(Modifier.padding(T.SpaceLg)) {
                    Text(template.description, style = PdigType.Body)
                    Spacer(Modifier.height(T.SpaceXs))
                    template.recommendedLeadTimeDays?.let {
                        Text("建议提前 $it 天准备", style = PdigType.Secondary, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (isPhoneScenario) {
                // flagship journey：Continuity Rail（spec §31）
                SectionHeader("整个流程")
                ContinuityRail(
                    steps = listOf(
                        ContinuityStep("选择旧手机号", RailState.ACTIVE, "确认哪一个手机号将被停用"),
                        ContinuityStep("查看影响", RailState.UPCOMING, "该手机号承担的认证、恢复与通讯能力"),
                        ContinuityStep("查看恢复路径", RailState.UPCOMING, "哪些账户靠它找回"),
                        ContinuityStep("查看共享故障点", RailState.UPCOMING, "哪些路径与它共用同一故障源"),
                        ContinuityStep("添加新手机号", RailState.UPCOMING, "建立新的恢复 / 认证路径"),
                        ContinuityStep("验证新手机号", RailState.UPCOMING, "确认新路径可用"),
                        ContinuityStep("迁移关键账户", RailState.UPCOMING, "把登录 / 恢复迁移到新路径"),
                        ContinuityStep("再次验证", RailState.UPCOMING, "确认迁移后一切可用"),
                        ContinuityStep("停用旧手机号", RailState.BLOCKED, "新路径全部验证后才能停用"),
                    ),
                    blockedReason = "新手机号验证通过后才能停用旧手机号（先建立新路径，再移除旧路径）。",
                )
                Spacer(Modifier.height(T.SpaceLg))
            }

            if (!executable) {
                EmptyState("该场景为 planned（设计稿），当前版本无法创建变更计划。")
                return@PdigPage
            }

            // 选择旧 / 目标
            SectionHeader(if (isPhoneScenario) "旧手机号（必选）" else "目标支付工具（必选）")
            if (candidates.isEmpty()) {
                EmptyState(
                    if (isPhoneScenario) {
                        "没有身份锚点节点。请先在「待确认服务」中确认手机号身份锚点，再回到这里。"
                    } else {
                        "没有支付工具节点。请先导入账单或接受候选对象，再回到这里。"
                    },
                    title = "这里还没有可选对象",
                    next = "前往「待确认服务」确认一个对象后回来",
                )
            } else {
                candidates.forEach { n ->
                    SelectNodeRow(title = n.name, subtitle = kindLabel(n.kind), selected = targetId == n.id, onSelect = { targetId = n.id })
                }
            }

            SectionHeader(if (isPhoneScenario) "新手机号（可选）" else "替代支付工具（可选）")
            candidates.forEach { n ->
                SelectNodeRow(title = n.name, subtitle = kindLabel(n.kind), selected = altId == n.id, onSelect = { altId = n.id })
            }
            if (altId != null) {
                Text(
                    if (isPhoneScenario) "新手机号已记录，可在后续手动关联。" else "替代支付工具已记录（当前仅作记录，不参与计划生成）。",
                    style = PdigType.Secondary, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(T.SpaceSm))
            }

            Button(
                onClick = { createPlan(ui, template.id, targetId) },
                enabled = targetId != null,
                modifier = Modifier.height(T.ButtonHeight),
            ) { Text(if (isPhoneScenario) "开始准备：创建变更计划" else "创建变更计划", style = PdigType.Button) }
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = { ui.screen = Screen.SCENARIOS }) { Text("返回场景中心") }
        }
    }
}

@Composable
private fun SelectNodeRow(title: String, subtitle: String, selected: Boolean, onSelect: () -> Unit) {
    PdigRow(
        title = title,
        subtitle = subtitle,
        trailing = {
            TextButton(onClick = onSelect) {
                Text(
                    if (selected) "已选择" else "选择",
                    style = PdigType.Label,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
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