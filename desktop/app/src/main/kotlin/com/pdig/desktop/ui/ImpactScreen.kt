package com.pdig.desktop.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pdig.app.data.ScenarioPlanRequest
import com.pdig.desktop.ui.components.EmptyState
import com.pdig.desktop.ui.components.PdigPage
import com.pdig.desktop.ui.components.PdigRow
import com.pdig.desktop.ui.components.SectionHeader
import com.pdig.desktop.ui.theme.PdigDesktopTokens as T
import com.pdig.desktop.ui.theme.PdigStatusColors
import com.pdig.desktop.ui.theme.PdigType

/**
 * 影响分析（spec §33）：至少分「必须处理 / 需要确认 / 影响较小或备用路径 / 暂无影响」，
 * must_change 视觉突出；needs_review 不伪装成 error。
 */
@Composable
fun ImpactScreen(ui: UiState) {
    val target = ui.selectedNodeId
    PdigPage(
        title = "影响分析",
        subtitle = target?.let { id ->
            val name = ui.session.graph.nodes().firstOrNull { it.id == id }?.name ?: id
            "如果停用「$name」，会影响到谁"
        } ?: "未选择目标节点",
        notice = ui.notice,
        error = ui.error,
        onDismissNotice = { ui.notice = null },
        onDismissError = { ui.error = null },
    ) {
        if (target == null) {
            EmptyState("未选择目标节点。请从「基础设施」进入节点详情后再点击影响分析。")
            return@PdigPage
        }
        val impact = ui.session.graph.impactFor(target)
        val must = impact.checklist.filter { it.level.wire == "must_change" }
        val review = impact.checklist.filter { it.level.wire == "needs_review" }
        val backup = impact.checklist.filter { it.level.wire == "backup_path" || it.level.wire == "degraded" }
        val unaffected = impact.checklist.filter { it.level.wire == "unaffected" }

        Column(Modifier.fillMaxWidth()) {
            // 概览（含本次分析依据 = 检查范围；不是安全分）
            SectionHeader("本次检查范围")
            PdigRow(
                title = "检查了 ${impact.processedKeys.size} 项能力状态",
                subtitle = "本次分析依据：已确认的现实关系；未覆盖的范围不会被当作安全。",
                leading = {
                    Icon(Icons.Filled.Info, contentDescription = null, tint = PdigStatusColors.verifying, modifier = Modifier.size(T.IconSize))
                },
                divider = false,
            )

            // 必须处理 —— 视觉突出
            SectionHeader("必须处理")
            if (must.isEmpty()) {
                PdigRow(
                    title = "没有必须处理的事项",
                    subtitle = "不代表一切安全：未知范围仍可能依赖它。",
                    leading = {
                        Icon(Icons.Filled.CheckCircle, contentDescription = "无必须事项", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(T.IconSize))
                    },
                    divider = false,
                )
            } else {
                must.forEach { item ->
                    PdigRow(
                        title = item.title,
                        subtitle = item.detail,
                        leading = {
                            Icon(Icons.Filled.ErrorOutline, contentDescription = "必须处理", tint = PdigStatusColors.blocked, modifier = Modifier.size(T.IconSize))
                        },
                        divider = true,
                    )
                }
            }

            // 需要确认
            SectionHeader("需要确认")
            if (review.isEmpty()) {
                PdigRow(
                    title = "没有需要确认的事项",
                    subtitle = "所有受影响的路径都已有确认依据。",
                    leading = {
                        Icon(Icons.Filled.CheckCircle, contentDescription = "无需确认", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(T.IconSize))
                    },
                    divider = false,
                )
            } else {
                review.forEach { item ->
                    PdigRow(
                        title = item.title,
                        subtitle = item.detail,
                        leading = {
                            Icon(Icons.Filled.HelpOutline, contentDescription = "需要确认", tint = PdigStatusColors.reviewRequired, modifier = Modifier.size(T.IconSize))
                        },
                        divider = true,
                    )
                }
            }

            // 影响较小 / 备用路径
            SectionHeader("影响较小 / 有备用路径")
            if (backup.isEmpty()) {
                PdigRow(
                    title = "没有已发现的备用路径",
                    subtitle = "未记录 ≠ 不存在；是否真的没有备用还不确定。",
                    leading = {
                        Icon(Icons.Filled.HelpOutline, contentDescription = "未知", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(T.IconSize))
                    },
                    divider = false,
                )
            } else {
                backup.forEach { item ->
                    PdigRow(
                        title = item.title,
                        subtitle = item.detail,
                        leading = {
                            Icon(Icons.Filled.CheckCircle, contentDescription = "有备用路径", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(T.IconSize))
                        },
                        divider = true,
                    )
                }
            }

            // 暂无影响
            SectionHeader("暂无影响")
            PdigRow(
                title = if (unaffected.isEmpty()) "本次检查未发现其他影响" else "${unaffected.size} 项未发现影响",
                subtitle = "暂无影响基于已检查范围；未检查范围不作判断。",
                leading = {
                    Icon(Icons.Filled.CheckCircle, contentDescription = "暂无影响", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(T.IconSize))
                },
                divider = false,
            )

            Spacer(Modifier.height(T.SpaceXxl))
            Surface(
                Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(T.RadiusMd),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(Modifier.padding(T.SpaceLg)) {
                    Text("接下来", style = PdigType.SectionTitle, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(T.SpaceSm))
                    Button(
                        onClick = { createPlanFromImpact(ui, target) },
                        enabled = must.isNotEmpty(),
                    ) {
                        Text(if (must.isNotEmpty()) "生成变更计划（处理必须事项）" else "生成变更计划", style = PdigType.Button)
                    }
                    if (must.isEmpty()) {
                        Text(
                            "当前没有必须处理的事项，仍可生成计划查看步骤。",
                            style = PdigType.Secondary,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = { ui.screen = Screen.NODE }) { Text("返回节点详情") }
                }
            }
        }
    }
}

private fun createPlanFromImpact(ui: UiState, targetId: String) {
    try {
        val planId = ui.session.plans.createPlanForScenario(
            ScenarioPlanRequest(scenarioId = "replace_payment_card", targetNodeId = targetId),
        )
        ui.selectedPlanId = planId
        ui.screen = Screen.PLAN
    } catch (t: Throwable) {
        ui.showError(t)
    }
}