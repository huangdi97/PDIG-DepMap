package com.pdig.desktop.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.pdig.core.domain.Dependency
import com.pdig.core.generated.RecoveryCycleStatus
import com.pdig.desktop.ui.components.EmptyState
import com.pdig.desktop.ui.components.PdigCard
import com.pdig.desktop.ui.components.PdigPage
import com.pdig.desktop.ui.components.SectionDivider
import com.pdig.desktop.data.DesktopSession

/**
 * 基础设施薄弱点（v0.3.0）：
 * 派生投影（不 bump revision）。只展示用户能看懂的中文，不泄漏 enum 名。
 */
@Composable
fun FindingsScreen(ui: UiState) {
    PdigPage(
        title = "基础设施薄弱点",
        subtitle = "当前发现的恢复 / 认证路径薄弱点（依据已确认的现实关系）",
        notice = ui.notice,
        error = ui.error,
        onDismissNotice = { ui.notice = null },
        onDismissError = { ui.error = null },
    ) {
        val findings = buildFindings(ui.session)
        Column {
            if (findings.spof.isEmpty() && findings.shared.isEmpty() && findings.cycles.isEmpty()) {
                EmptyState("暂未发现基础设施薄弱点。")
            }
            SectionDivider("只有一个来源，没有备用路径")
            findings.spof.forEach { f ->
                PdigCard(
                    title = "「${f.targetName}」只有唯一恢复来源",
                    subtitle = "没有已确认的备用恢复路径；是否真的没有备用还不确定（未记录 ≠ 不存在）。",
                )
            }
            SectionDivider("这两个路径依赖同一个故障点")
            findings.shared.forEach { f ->
                PdigCard(
                    title = "多个恢复方式都依赖「${f.nodeName}」",
                    subtitle = "这些恢复方式共享同一个故障点；建议为其中一个路径寻找独立来源。",
                )
            }
            SectionDivider("发现一个恢复循环")
            findings.cycles.forEach { f ->
                PdigCard(
                    title = "恢复方式依赖它正在恢复的对象",
                    subtitle = "${f.pathDescription}；该恢复方式不能视为独立备用路径。",
                )
            }
        }
    }
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