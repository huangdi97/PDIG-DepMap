package com.pdig.desktop.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pdig.core.domain.Dependency
import com.pdig.core.generated.RecoveryCycleStatus
import com.pdig.desktop.data.DesktopSession
import com.pdig.desktop.ui.components.EmptyState
import com.pdig.desktop.ui.components.ExpandableBlock
import com.pdig.desktop.ui.components.PdigPage
import com.pdig.desktop.ui.components.SectionHeader
import com.pdig.desktop.ui.theme.PdigDesktopTokens as T
import com.pdig.desktop.ui.theme.PdigStatusColors
import com.pdig.desktop.ui.theme.PdigType
/**
 * 基础设施薄弱点（v0.3.0）：
 * 每条 Finding 用「是什么 / 为什么 / 基于什么确认 / 还不知道什么 / 可能影响什么 / 下一步建议」结构（spec §26）。
 * 不泄漏 enum 名；只展示用户能看懂的中文。
 */
@Composable
fun FindingsScreen(ui: UiState) {
    val findings = buildFindings(ui.session)
    PdigPage(
        title = "基础设施薄弱点",
        subtitle = "当前发现的恢复 / 认证路径薄弱点（依据已确认的现实关系）",
        notice = ui.notice,
        error = ui.error,
        onDismissNotice = { ui.notice = null },
        onDismissError = { ui.error = null },
    ) {
        Column(Modifier.fillMaxWidth()) {
            if (findings.spof.isEmpty() && findings.shared.isEmpty() && findings.cycles.isEmpty()) {
                EmptyState(
                    message = "当前没有已确认的薄弱点。没有发现并不等于一切安全：",
                    title = "这里还没有薄弱点",
                    next = "未确认的对象 / 未导入的来源 / 未验证的变更仍可能隐藏依赖问题。",
                )
            }

            if (findings.spof.isNotEmpty()) {
                SectionHeader("只有一个来源，没有备用路径")
                findings.spof.forEach { f ->
                    FindingCard(
                        icon = Icons.Filled.ErrorOutline,
                        color = MaterialTheme.colorScheme.error,
                        title = "「${f.targetName}」只有唯一恢复来源",
                        summary = "没有已确认的备用恢复路径。",
                        confirmed = "这个结论基于已确认的恢复关系：该对象只有一条恢复路径。",
                        unknown = "是否真的没有备用路径还不确定：未记录 ≠ 不存在。",
                        affected = "恢复",
                        next = "为它寻找并确认一条独立恢复路径。",
                    )
                }
            }

            if (findings.shared.isNotEmpty()) {
                SectionHeader("这两个路径依赖同一个故障点")
                findings.shared.forEach { f ->
                    FindingCard(
                        icon = Icons.Filled.WarningAmber,
                        color = PdigStatusColors.reviewRequired,
                        title = "多个恢复方式都依赖「${f.nodeName}」",
                        summary = "这些恢复方式共享同一个故障点。",
                        confirmed = "已确认多条恢复路径都指向同一个设备 / 手机号 / 服务商。",
                        unknown = "它们各自是否还有别的独立路径还不确定。",
                        affected = "恢复 · 认证",
                        next = "为其中一条路径寻找独立来源。",
                    )
                }
            }

            if (findings.cycles.isNotEmpty()) {
                SectionHeader("发现一个恢复循环")
                findings.cycles.forEach { f ->
                    FindingCard(
                        icon = Icons.Filled.WarningAmber,
                        color = PdigStatusColors.reviewRequired,
                        title = "恢复方式依赖它正在恢复的对象",
                        summary = f.pathDescription,
                        confirmed = "这个循环基于已确认的现实关系。",
                        unknown = "是否需要调整恢复方式由你决定。",
                        affected = "恢复",
                        next = "考虑改用不依赖该对象的恢复方式。",
                    )
                }
            }

            if (findings.spof.isEmpty() && findings.shared.isEmpty() && findings.cycles.isEmpty()) {
                Spacer(Modifier.height(T.SpaceSm))
                Surface(
                    Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(T.RadiusMd),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(Modifier.padding(T.SpaceLg)) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(T.EmptyStateIcon))
                        Spacer(Modifier.height(T.SpaceSm))
                        Text("可以主动准备：前往「场景中心」模拟更换银行卡或手机号，提前看清影响。", style = PdigType.Secondary, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

/** Finding 行：默认 what + why + next；展开 evidence / unknown / affected。 */
@Composable
private fun FindingCard(
    icon: ImageVector,
    color: androidx.compose.ui.graphics.Color,
    title: String,
    summary: String,
    confirmed: String,
    unknown: String,
    affected: String,
    next: String,
) {
    Surface(
        Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(T.RadiusMd),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(horizontal = T.SpaceLg, vertical = T.SpaceMd)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(T.SpaceSm))
                Text(title, style = PdigType.Body, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.height(4.dp))
            Text(summary, style = PdigType.Secondary, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            // 下一步（默认可见）
            Text("下一步建议：$next", style = PdigType.Label, color = MaterialTheme.colorScheme.primary)
            // 展开：evidence / unknown / affected
            ExpandableBlock("查看依据与未知范围", defaultExpanded = false) {
                Text("是什么：$title", style = PdigType.Secondary, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("为什么：$summary", style = PdigType.Secondary, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("基于什么确认：$confirmed", style = PdigType.Secondary, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("还不知道什么：$unknown", style = PdigType.Secondary, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("可能影响什么：$affected", style = PdigType.Secondary, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    Spacer(Modifier.height(T.SpaceSm))
}

internal data class FindingSpof(val targetName: String)
internal data class FindingShared(val nodeName: String)
internal data class FindingCycle(val pathDescription: String)

internal data class FindingsModel(
    val spof: List<FindingSpof>,
    val shared: List<FindingShared>,
    val cycles: List<FindingCycle>,
)

/** v0.3.0 引擎组合：把 confirmed reality 转成用户可读的 Finding 列表。 */
internal fun buildFindings(session: DesktopSession): FindingsModel {
    val graph = session.graph.loadImpactGraph()
    val nodes = session.graph.nodes()
    val nameOf = { id: String -> nodes.firstOrNull { it.id == id }?.name ?: id }
    val recoveryDeps = graph.dependencies.filter {
        it.capability.wire == "recovery" && it.state.wire == "active"
    }

    // 1. 只有一个来源（SPOF）：target 只有一条 confirmed active recovery 入边
    val byTarget: Map<String, List<Dependency>> = recoveryDeps.groupBy { it.to }
    val spof = byTarget
        .filter { it.value.size == 1 }
        .keys
        .toList()
        .sorted()
        .map { FindingSpof(nameOf(it)) }

    // 2. 共享故障点：同一个 from（同一设备/手机号）服务多条恢复路径
    val byFrom: Map<String, List<Dependency>> = recoveryDeps.groupBy { it.from }
    val shared = byFrom
        .filter { it.value.size > 1 }
        .keys
        .toList()
        .sorted()
        .map { FindingShared(nameOf(it)) }

    // 3. 恢复循环：confirmed cycle（依赖已确认现实；V030-RC-01）
    val cycleResult = com.pdig.core.impact.detectRecoveryCycles(
        com.pdig.core.impact.RecoveryCycleInput(
            capability = com.pdig.core.generated.Capability.RECOVERY,
            dependencies = recoveryDeps,
            hintedEdges = emptyList(),
        ),
    )
    val cycles = if (cycleResult.status == RecoveryCycleStatus.CONFIRMED_CYCLE) {
        cycleResult.confirmedCycles.map { path ->
            FindingCycle(path.joinToString(" → ") { nameOf(it) })
        }
    } else {
        emptyList()
    }

    return FindingsModel(spof, shared, cycles)
}