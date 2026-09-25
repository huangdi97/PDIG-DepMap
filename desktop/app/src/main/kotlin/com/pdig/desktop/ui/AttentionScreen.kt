package com.pdig.desktop.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.pdig.desktop.ui.components.ChipTone
import com.pdig.desktop.ui.components.EmptyState
import com.pdig.desktop.ui.components.PdigCard
import com.pdig.desktop.ui.components.PdigPage
import com.pdig.desktop.ui.components.SectionDivider
import com.pdig.desktop.ui.components.StatusChip

/** 需要处理：聚合所有未确认 / 待处理事项的计数入口。 */
@Composable
fun AttentionScreen(ui: UiState) {
    val proposalCount = ui.session.proposals.pendingProposals().size
    val candidateCount = ui.session.candidates.pendingCandidates().size
    val driftCount = ui.session.drifts.openDrifts().size
    val attentionPlans = ui.session.plans.plans().filter { planNeedsAttention(it.workflowState) }
    PdigPage(
        title = "需要处理",
        subtitle = "看看有什么需要确认",
        notice = ui.notice,
        error = ui.error,
        onDismissNotice = { ui.notice = null },
        onDismissError = { ui.error = null },
    ) {
        Column {
            SectionDivider("待确认关系")
            PdigCard(
                title = "待确认关系",
                subtitle = "$proposalCount 条依赖建议待您决定",
                onClick = { ui.screen = Screen.REVIEW },
                trailing = { StatusChip("$proposalCount", if (proposalCount > 0) ChipTone.WARN else ChipTone.NEUTRAL) },
            )
            SectionDivider("待确认服务")
            PdigCard(
                title = "待确认服务",
                subtitle = "$candidateCount 个未解析对象",
                onClick = { ui.screen = Screen.CANDIDATES },
                trailing = { StatusChip("$candidateCount", if (candidateCount > 0) ChipTone.WARN else ChipTone.NEUTRAL) },
            )
            SectionDivider("可能发生了变化")
            PdigCard(
                title = "可能发生了变化",
                subtitle = "$driftCount 项待处理",
                onClick = { ui.screen = Screen.DRIFTS },
                trailing = { StatusChip("$driftCount", if (driftCount > 0) ChipTone.WARN else ChipTone.NEUTRAL) },
            )
            SectionDivider("需要处理的计划")
            PdigCard(
                title = "需要处理的计划",
                subtitle = if (attentionPlans.isEmpty()) "暂无未完成的计划" else "${attentionPlans.size} 个计划需要继续",
                onClick = {
                    val next = attentionPlans.firstOrNull()
                    if (next != null) {
                        ui.selectedPlanId = next.id
                        ui.screen = Screen.PLAN
                    } else {
                        ui.screen = Screen.TIMELINE
                    }
                },
                trailing = { StatusChip("${attentionPlans.size}", if (attentionPlans.isNotEmpty()) ChipTone.WARN else ChipTone.NEUTRAL) },
            )
            SectionDivider("没有需要确认的？")
            if (proposalCount == 0 && candidateCount == 0 && driftCount == 0 && attentionPlans.isEmpty()) {
                EmptyState("全部事项都已处理。")
            }
        }
    }
}
