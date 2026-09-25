package com.pdig.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.pdig.app.data.DependencyRow
import com.pdig.app.data.NodeRow
import com.pdig.app.ui.components.EmptyState
import com.pdig.app.ui.components.PdigCard
import com.pdig.app.ui.components.SectionHeader
import com.pdig.app.ui.theme.PdigTokens

/** NodeDetail 一次 IO 取回的聚合（六问所需的最小投影，全部在内存）。 */
internal data class NodeDetailSnapshot(
    val node: NodeRow?,
    val deps: List<DependencyRow>,
    val pendingProposals: Int,
    val openDrifts: Int,
)

/** 六问 · ① 这是什么：kind 人话标签（spec §65：内部 wire 值绝不上屏）。 */
@Composable
internal fun NodeIdentityCard(kind: String) {
    SectionHeader("这是什么")
    PdigCard {
        Column(verticalArrangement = Arrangement.spacedBy(PdigTokens.SpaceXs)) {
            Text(nodeKindGroupLabel(kind), style = PdigTokens.BodyStrong)
            Text(
                "它在你确认的基础设施里以这个类别记录。",
                style = PdigTokens.Caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * 六问 · ② 确认了什么。
 *
 * 容器没有 per-node 的确认时间/来源字段，因此诚实展示：节点名称 + kind + 已确认关系数
 * （Dependency 存在即代表用户确认 —— spec §13）。④ 依据来源 / ⑤ 最近确认因模型无字段而省略。
 */
@Composable
internal fun ConfirmedCard(name: String, kind: String, confirmedRelationCount: Int) {
    SectionHeader("确认了什么")
    PdigCard {
        Column(verticalArrangement = Arrangement.spacedBy(PdigTokens.SpaceXs)) {
            Text("已确认记录：$name（${nodeKindGroupLabel(kind)}）", style = PdigTokens.BodyStrong)
            Text(
                if (confirmedRelationCount > 0) "与它相关的已确认关系共 $confirmedRelationCount 条。"
                else "还没有已确认的关系。",
                style = PdigTokens.Caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 六问 · ⑥ 有没有待处理问题：pending proposals + open drifts 计数（查不到时为 0 态）。 */
@Composable
internal fun PendingIssuesCard(
    count: Int?,
    hasProposals: Boolean,
    onClickProposals: () -> Unit,
    onClickDrift: () -> Unit,
) {
    val total = count ?: 0
    if (total == 0) {
        EmptyState("当前没有待处理的问题。")
        return
    }
    PdigCard(onClick = if (hasProposals) onClickProposals else onClickDrift) {
        Column(verticalArrangement = Arrangement.spacedBy(PdigTokens.SpaceXs)) {
            Text("有 $total 个待处理问题", style = PdigTokens.BodyStrong)
            Text(
                if (hasProposals) "包括待确认的关系，建议先处理。"
                else "包括待确认的变化，建议先处理。",
                style = PdigTokens.Caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
