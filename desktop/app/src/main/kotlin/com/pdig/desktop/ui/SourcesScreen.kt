package com.pdig.desktop.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.pdig.desktop.ui.components.ChipTone
import com.pdig.desktop.ui.components.EmptyState
import com.pdig.desktop.ui.components.PdigCard
import com.pdig.desktop.ui.components.PdigPage
import com.pdig.desktop.ui.components.SectionDivider
import com.pdig.desktop.ui.components.StatusChip

/** 数据来源：已配置的账单导入源列表。 */
@Composable
fun SourcesScreen(ui: UiState) {
    val sources = ui.session.sources.sourceInstances()
    PdigPage(
        title = "数据来源",
        subtitle = "已配置的账单导入源",
        notice = ui.notice,
        error = ui.error,
        onDismissNotice = { ui.notice = null },
        onDismissError = { ui.error = null },
    ) {
        Column {
            if (sources.isEmpty()) {
                EmptyState("还没有任何数据来源。请前往「导入」添加微信账单、通用 CSV 或 OFX/QFX。")
            } else {
                SectionDivider("已配置来源（${sources.size}）")
                sources.forEach { s ->
                    PdigCard(
                        title = s.label,
                        subtitle = listOfNotNull(
                            "ID: ${s.id}",
                            "适配器: ${s.adapterId}",
                            s.lastIngestedAt?.let { "最近导入：${it.take(19)}" },
                        ).joinToString(" · "),
                        trailing = { StatusChip(stateLabel(s.state), toneFor(s.state)) },
                    )
                }
            }
        }
    }
}

private fun stateLabel(state: String): String = when (state) {
    "active" -> "已启用"
    "error" -> "异常"
    else -> state
}

private fun toneFor(state: String): ChipTone = when (state) {
    "error" -> ChipTone.BAD
    else -> ChipTone.NEUTRAL
}