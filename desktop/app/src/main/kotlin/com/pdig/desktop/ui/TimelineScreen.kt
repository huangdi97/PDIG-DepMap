package com.pdig.desktop.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.pdig.desktop.ui.components.ChipTone
import com.pdig.desktop.ui.components.EmptyState
import com.pdig.desktop.ui.components.PdigCard
import com.pdig.desktop.ui.components.PdigPage
import com.pdig.desktop.ui.components.SectionDivider
import com.pdig.desktop.ui.components.StatusChip

/** 桶顺序（与 core.timeline 的 BUCKET_ORDER 一致，仅用于展示分组）。 */
private val BUCKET_LABELS: List<Pair<String, String>> = listOf(
    "attention" to "需要关注",
    "overdue" to "已逾期",
    "today" to "今天",
    "7d" to "7 天内",
    "30d" to "30 天内",
    "90d" to "90 天内",
    "later" to "稍后",
)

/** 时间线：纯投影（derived read model），按桶分组展示。 */
@Composable
fun TimelineScreen(ui: UiState) {
    val items = ui.session.graph.timeline()
    val grouped = BUCKET_LABELS.mapNotNull { (bucket, label) ->
        val list = items.filter { it.bucket == bucket }
        if (list.isEmpty()) null else label to list
    }
    PdigPage(
        title = "时间线",
        subtitle = "纯投影：从当前数据实时构建，不持久化",
        notice = ui.notice,
        error = ui.error,
        onDismissNotice = { ui.notice = null },
        onDismissError = { ui.error = null },
    ) {
        Column {
            if (grouped.isEmpty()) {
                EmptyState("时间线上还没有任何事项。")
            } else {
                grouped.forEach { (label, list) ->
                    SectionDivider("$label（${list.size}）")
                    list.forEach { item ->
                        PdigCard(
                            title = item.title,
                            subtitle = listOfNotNull(
                                item.subtitle,
                                item.scheduledAt?.let { "计划时间：${it.take(19)}" },
                            ).joinToString(" · "),
                            trailing = { StatusChip(item.bucket, ChipTone.NEUTRAL) },
                        )
                    }
                }
            }
        }
    }
}