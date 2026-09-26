package com.pdig.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.pdig.app.data.AppContainer
import com.pdig.app.ui.components.EmptyState
import com.pdig.app.ui.components.LoadingState
import com.pdig.app.ui.components.PdigCard
import com.pdig.app.ui.components.PdigScrollingPage
import com.pdig.app.ui.components.PdigTopBar
import com.pdig.app.ui.components.SectionHeader
import com.pdig.app.ui.theme.PdigTokens
import com.pdig.core.domain.Dependency
import com.pdig.core.generated.Capability
import com.pdig.core.generated.RecoveryCycleStatus
import com.pdig.core.impact.RecoveryCycleInput
import com.pdig.core.impact.detectRecoveryCycles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 基础设施薄弱点（v0.3.0）：
 * 派生投影（不 bump revision）。只展示用户能看懂的中文，不泄漏 enum 名。
 */
@Composable
fun FindingsScreen(nav: NavController) {
    val context = LocalContext.current
    val container = remember { AppContainer.get(context) }
    var findings by remember { mutableStateOf<FindingsModel?>(null) }
    LaunchedEffect(Unit) {
        findings = withContext(Dispatchers.IO) { buildFindings(container) }
    }

    Scaffold(topBar = { PdigTopBar("基础设施薄弱点", onBack = { nav.popBackStack() }) }) { pad ->
        PdigScrollingPage(
            modifier = Modifier
                .padding(pad)
                .padding(horizontal = PdigTokens.SpaceLg),
        ) {
            Text(
                "当前发现的恢复 / 认证路径薄弱点（依据已确认的现实关系）",
                style = PdigTokens.Body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val model = findings
            if (model == null) {
                LoadingState()
            } else {
                if (model.spof.isEmpty() && model.shared.isEmpty() && model.cycles.isEmpty()) {
                    EmptyState("暂未发现基础设施薄弱点。")
                }
                SectionHeader("只有一个来源，没有备用路径")
                model.spof.forEach { f ->
                    PdigCard {
                        Column(verticalArrangement = Arrangement.spacedBy(PdigTokens.SpaceXs)) {
                            Text("「${f.targetName}」只有唯一恢复来源", style = PdigTokens.BodyStrong)
                            Text(
                                "没有已确认的备用恢复路径；是否真的没有备用还不确定（未记录 ≠ 不存在）。",
                                style = PdigTokens.Caption,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                SectionHeader("这两个路径依赖同一个故障点")
                model.shared.forEach { f ->
                    PdigCard {
                        Column(verticalArrangement = Arrangement.spacedBy(PdigTokens.SpaceXs)) {
                            Text("多个恢复方式都依赖「${f.nodeName}」", style = PdigTokens.BodyStrong)
                            Text(
                                "这些恢复方式共享同一个故障点；建议为其中一个路径寻找独立来源。",
                                style = PdigTokens.Caption,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                SectionHeader("发现一个恢复循环")
                model.cycles.forEach { f ->
                    PdigCard {
                        Column(verticalArrangement = Arrangement.spacedBy(PdigTokens.SpaceXs)) {
                            Text("恢复方式依赖它正在恢复的对象", style = PdigTokens.BodyStrong)
                            Text(
                                "${f.pathDescription}；该恢复方式不能视为独立备用路径。",
                                style = PdigTokens.Caption,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
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

/** v0.3.0 引擎组合：把 confirmed reality 转成用户可读的 Finding 列表（派生投影，不 bump revision）。 */
internal fun buildFindings(container: AppContainer): FindingsModel {
    val graph = container.loadImpactGraph()
    val nodes = container.nodes()
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
    val cycleResult = detectRecoveryCycles(
        RecoveryCycleInput(
            capability = Capability.RECOVERY,
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