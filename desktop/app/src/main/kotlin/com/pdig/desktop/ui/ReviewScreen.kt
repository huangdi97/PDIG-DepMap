package com.pdig.desktop.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.pdig.desktop.ui.components.ChipTone
import com.pdig.desktop.ui.components.EmptyState
import com.pdig.desktop.ui.components.PdigCard
import com.pdig.desktop.ui.components.PdigPage
import com.pdig.desktop.ui.components.SectionDivider
import com.pdig.desktop.ui.components.StatusChip

/** 待确认：汇总所有需要用户拍板的事项。 */
@Composable
fun ReviewScreen(ui: UiState) {
    val proposalCount = ui.session.proposals.pendingProposals().size
    val candidateCount = ui.session.candidates.pendingCandidates().size
    val driftCount = ui.session.drifts.openDrifts().size
    val attention = ui.session.graph.timeline().filter { it.bucket == "attention" }
    PdigPage(
        title = "待确认",
        subtitle = "机器推断 ≠ 现实事实：所有未确认事项都列在这里",
        notice = ui.notice,
        error = ui.error,
        onDismissNotice = { ui.notice = null },
        onDismissError = { ui.error = null },
    ) {
        Column {
            SectionDivider("汇总")
            PdigCard(
                title = "Proposal 依赖建议",
                subtitle = "$proposalCount 条待决定",
                onClick = { ui.screen = Screen.PROPOSALS },
                trailing = { StatusChip("$proposalCount", if (proposalCount > 0) ChipTone.WARN else ChipTone.NEUTRAL) },
            )
            PdigCard(
                title = "候选对象",
                subtitle = "$candidateCount 个待解析",
                onClick = { ui.screen = Screen.CANDIDATES },
                trailing = { StatusChip("$candidateCount", if (candidateCount > 0) ChipTone.WARN else ChipTone.NEUTRAL) },
            )
            PdigCard(
                title = "现实变化（Drift）",
                subtitle = "$driftCount 项待处理",
                onClick = { ui.screen = Screen.DRIFTS },
                trailing = { StatusChip("$driftCount", if (driftCount > 0) ChipTone.WARN else ChipTone.NEUTRAL) },
            )
            SectionDivider("时间线需要关注")
            if (attention.isEmpty()) {
                EmptyState("时间线上没有需要关注的事项。")
            } else {
                attention.forEach { item ->
                    PdigCard(title = item.title, subtitle = item.subtitle)
                }
            }
        }
    }
}