package com.pdig.desktop.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pdig.core.scenario.ScenarioRegistry
import com.pdig.desktop.ui.components.ChevronTrailing
import com.pdig.desktop.ui.components.EmptyState
import com.pdig.desktop.ui.components.PdigPage
import com.pdig.desktop.ui.components.PdigRow
import com.pdig.desktop.ui.components.SectionHeader
import com.pdig.desktop.ui.theme.PdigDesktopTokens as T
import com.pdig.desktop.ui.theme.PdigStatusColors
import com.pdig.desktop.ui.theme.PdigType

/**
 * 首页 = Personal Infrastructure Briefing（spec §24–§25）。
 * 顺序：需要你处理 → 基础设施薄弱点 → 可能发生了变化 → 即将到来 → 常用场景 → 我的基础设施。
 * Healthy 不显示 0 issues / 安全分；明确表达检查范围与未知范围。
 */
@Composable
fun HomeScreen(ui: UiState) {
    val attention = ui.session.graph.timeline().filter { it.bucket == "attention" }
    val proposalCount = ui.session.proposals.pendingProposals().size
    val candidateCount = ui.session.candidates.pendingCandidates().size
    val driftCount = ui.session.drifts.openDrifts().size
    val upcoming = ui.session.graph.timeline().filter { it.bucket != "attention" }.take(3)
    val findings = buildFindings(ui.session)
    val findingCount = findings.spof.size + findings.shared.size + findings.cycles.size
    PdigPage(
        title = "首页",
        subtitle = "这是你的数字基础设施简报：先看要处理什么，再看可以准备什么。",
        notice = ui.notice,
        error = ui.error,
        onDismissNotice = { ui.notice = null },
        onDismissError = { ui.error = null },
    ) {
        Column(Modifier.fillMaxWidth()) {
            // 1. 需要你处理
            SectionHeader("需要你处理")
            if (attention.isEmpty() && proposalCount + candidateCount + driftCount == 0) {
                HealthyBrief(ui)
            } else {
                attention.forEach { item ->
                    PdigRow(
                        title = item.title,
                        subtitle = item.subtitle,
                        leading = {
                            Icon(Icons.Filled.WarningAmber, contentDescription = "需要处理", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        },
                        onClick = { ui.screen = Screen.ATTENTION },
                        trailing = { ChevronTrailing() },
                    )
                }
                PdigRow(
                    title = "待确认",
                    subtitle = "依赖建议 $proposalCount 条 · 候选对象 $candidateCount 个 · 可能变化 $driftCount 项",
                    leading = {
                        Icon(Icons.Filled.HelpOutline, contentDescription = "待确认", tint = PdigStatusColors.reviewRequired, modifier = Modifier.size(16.dp))
                    },
                    onClick = { ui.screen = Screen.REVIEW },
                    trailing = { ChevronTrailing() },
                )
            }

            // 2. 基础设施薄弱点
            SectionHeader("基础设施薄弱点") {
                androidx.compose.material3.TextButton(onClick = { ui.screen = Screen.FINDINGS }) {
                    Text("查看薄弱点", style = PdigType.Label, color = MaterialTheme.colorScheme.primary)
                }
            }
            if (findingCount == 0) {
                PdigRow(
                    title = "当前没有已确认的薄弱点",
                    subtitle = "没有发现并不等于一切安全：未知范围仍可能隐藏依赖问题。",
                    leading = {
                        Icon(Icons.Filled.CheckCircle, contentDescription = "未发现", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                    },
                    divider = false,
                )
            } else {
                PdigRow(
                    title = "${findings.spof.size} 处只有唯一恢复来源",
                    subtitle = "没有已确认的备用恢复路径",
                    leading = {
                        Icon(Icons.Filled.ErrorOutline, contentDescription = "薄弱点", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    },
                    onClick = { ui.screen = Screen.FINDINGS },
                    trailing = { ChevronTrailing() },
                )
                PdigRow(
                    title = "${findings.shared.size} 处共享同一个故障点",
                    subtitle = "多条恢复方式依赖同一台设备 / 同一个手机号",
                    leading = {
                        Icon(Icons.Filled.WarningAmber, contentDescription = "薄弱点", tint = PdigStatusColors.reviewRequired, modifier = Modifier.size(16.dp))
                    },
                    onClick = { ui.screen = Screen.FINDINGS },
                    trailing = { ChevronTrailing() },
                )
                PdigRow(
                    title = "${findings.cycles.size} 个恢复循环",
                    subtitle = "恢复方式依赖它正在恢复的对象，不能视为独立备用路径",
                    leading = {
                        Icon(Icons.Filled.WarningAmber, contentDescription = "薄弱点", tint = PdigStatusColors.reviewRequired, modifier = Modifier.size(16.dp))
                    },
                    onClick = { ui.screen = Screen.FINDINGS },
                    trailing = { ChevronTrailing() },
                )
            }

            // 3. 可能发生了变化
            SectionHeader("可能发生了变化")
            if (driftCount == 0 && candidateCount == 0) {
                PdigRow(
                    title = "没有待处理的可能变化",
                    subtitle = "一旦发现新的可能变化，会出现在这里。",
                    leading = {
                        Icon(Icons.Filled.CheckCircle, contentDescription = "无变化", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                    },
                    divider = false,
                )
            } else {
                PdigRow(
                    title = "有 $driftCount 项可能变化待确认",
                    subtitle = "系统发现可能的现实变化，需要你确认后才能成为现实。",
                    leading = {
                        Icon(Icons.Filled.SyncAlt, contentDescription = "可能变化", tint = PdigStatusColors.reviewRequired, modifier = Modifier.size(16.dp))
                    },
                    onClick = { ui.screen = Screen.DRIFTS },
                    trailing = { ChevronTrailing() },
                )
                PdigRow(
                    title = "有 $candidateCount 个未解析对象",
                    subtitle = "账单中出现的对象还没有对应到具体服务",
                    leading = {
                        Icon(Icons.Filled.HelpOutline, contentDescription = "未解析", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    },
                    onClick = { ui.screen = Screen.CANDIDATES },
                    trailing = { ChevronTrailing() },
                )
            }

            // 4. 即将到来
            SectionHeader("即将到来")
            if (upcoming.isEmpty()) {
                PdigRow(
                    title = "没有已安排的时间事项",
                    subtitle = "时间线中即将到期的事项会出现在这里。",
                    leading = {
                        Icon(Icons.Filled.CheckCircle, contentDescription = "无", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                    },
                    divider = false,
                )
            } else {
                upcoming.forEach { item ->
                    PdigRow(
                        title = item.title,
                        subtitle = item.subtitle,
                        onClick = { ui.screen = Screen.TIMELINE },
                        trailing = { ChevronTrailing() },
                    )
                }
            }

            // 5. 常用场景
            SectionHeader("常用场景")
            val activeScenarios = ScenarioRegistry.active
            Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
                activeScenarios.forEach { t ->
                    androidx.compose.material3.Surface(
                        Modifier.weight(1f).fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(T.RadiusMd),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Column(
                            Modifier.padding(14.dp),
                            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
                        ) {
                            Text(t.title, style = PdigType.Body, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                            Text(t.description, style = PdigType.Secondary, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "开始准备",
                                Modifier.clickable {
                                    ui.selectedScenarioId = t.id
                                    ui.screen = Screen.SCENARIO_SETUP
                                },
                                style = PdigType.Label,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }

            // 6. 我的基础设施
            SectionHeader("我的基础设施") {
                androidx.compose.material3.TextButton(onClick = { ui.screen = Screen.INFRA }) {
                    Text("查看基础设施", style = PdigType.Label, color = MaterialTheme.colorScheme.primary)
                }
            }
            val nodes = ui.session.graph.nodes()
            val deps = ui.session.graph.dependencies()
            if (nodes.isEmpty()) {
                EmptyState(
                    message = "还没有记录任何对象。",
                    title = "这里还没有内容",
                    next = "前往「数据来源 → 导入」添加账单开始",
                )
            } else {
                PdigRow(
                    title = "${nodes.size} 个对象 · ${deps.size} 条已确认关系",
                    subtitle = "手机号 · 邮箱 · 账户 · 设备 · 卡 · 服务",
                    onClick = { ui.screen = Screen.INFRA },
                    divider = false,
                    trailing = { ChevronTrailing() },
                )
            }
        }
    }
}

/** Healthy：不显示 0 issues / 安全分；表达检查范围、未知范围与可准备场景。 */
@Composable
private fun HealthyBrief(ui: UiState) {
    Column(Modifier.fillMaxWidth()) {
        PdigRow(
            title = "当前没有需要立即处理的事项",
            subtitle = "最近一次检查没有发现必须处理的问题；未发现不等于一切安全。",
            leading = {
                Icon(Icons.Filled.CheckCircle, contentDescription = "暂无事项", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
            },
            divider = false,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "仍然未知的范围：没有导入数据的来源、未确认的对象、未验证的变更。",
            style = PdigType.Secondary,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "可以主动准备：前往「场景中心」模拟更换银行卡或手机号。",
            style = PdigType.Secondary,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
