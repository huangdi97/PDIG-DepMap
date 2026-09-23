package com.pdig.desktop.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.pdig.core.scenario.ScenarioRegistry
import com.pdig.desktop.ui.components.EmptyState
import com.pdig.desktop.ui.components.PdigCard
import com.pdig.desktop.ui.components.PdigPage
import com.pdig.desktop.ui.components.SectionDivider

/** 首页：当前文件、需要关注的时间线、待确认计数、活跃场景入口。 */
@Composable
fun HomeScreen(ui: UiState) {
    val attention = ui.session.graph.timeline().filter { it.bucket == "attention" }
    val proposalCount = ui.session.proposals.pendingProposals().size
    val candidateCount = ui.session.candidates.pendingCandidates().size
    val driftCount = ui.session.drifts.openDrifts().size
    PdigPage(
        title = "首页",
        subtitle = "当前数据文件：${ui.dataFile?.name ?: "（尚未落盘）"}",
        notice = ui.notice,
        error = ui.error,
        onDismissNotice = { ui.notice = null },
        onDismissError = { ui.error = null },
    ) {
        Column {
            SectionDivider("需要关注")
            if (attention.isEmpty()) {
                EmptyState("暂无需要关注的事项。")
            } else {
                attention.forEach { item ->
                    PdigCard(title = item.title, subtitle = item.subtitle)
                }
            }
            SectionDivider("待确认")
            PdigCard(
                title = "Proposal 确认",
                subtitle = "$proposalCount 条依赖建议待您决定",
                onClick = { ui.screen = Screen.PROPOSALS },
            )
            PdigCard(
                title = "候选对象",
                subtitle = "$candidateCount 个未解析对象",
                onClick = { ui.screen = Screen.CANDIDATES },
            )
            PdigCard(
                title = "现实变化",
                subtitle = "$driftCount 项等待处理",
                onClick = { ui.screen = Screen.DRIFTS },
            )
            SectionDivider("变更场景")
            PdigCard(
                title = "场景中心",
                subtitle = "模拟换卡 / 注销一张卡会影响到谁",
                onClick = { ui.screen = Screen.SCENARIOS },
            )
            ScenarioRegistry.active.forEach { t ->
                PdigCard(
                    title = t.title,
                    subtitle = t.description,
                    onClick = {
                        ui.selectedScenarioId = t.id
                        ui.screen = Screen.SCENARIO_SETUP
                    },
                )
            }
        }
    }
}