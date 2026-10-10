package com.pdig.desktop.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pdig.desktop.ui.components.ChipTone
import com.pdig.desktop.ui.components.EmptyState
import com.pdig.desktop.ui.components.PdigCard
import com.pdig.desktop.ui.components.PdigPage
import com.pdig.desktop.ui.components.SectionHeader
import com.pdig.desktop.ui.components.StatusChip

/** 数据来源：已配置的账单导入源列表（不泄漏 adapterId 内部标识）。 */
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
        Column(Modifier.fillMaxWidth()) {
            if (sources.isEmpty()) {
                EmptyState("还没有任何数据来源。请前往「数据来源 → 导入」添加微信账单、通用 CSV 或 OFX/QFX。")
            } else {
                SectionHeader("已配置来源（${sources.size}）")
                sources.forEach { s ->
                    PdigCard(
                        title = s.label,
                        subtitle = listOfNotNull(
                            "来源类型：${sourceTypeLabel(s.adapterId)}",
                            s.lastIngestedAt?.let { "最近导入：${it.take(19)}" },
                        ).joinToString(" · "),
                        trailing = { StatusChip(stateLabel(s.state), toneFor(s.state)) },
                    )
                }
            }
            SectionHeader("入口")
            PdigCard(
                title = "导入账单",
                subtitle = "微信账单 CSV、通用 CSV（先映射列）或 OFX/QFX",
                onClick = {
                    ui.importStage = 0
                    ui.screen = Screen.IMPORT
                },
            )
            PdigCard(
                title = "CSV 字段映射",
                subtitle = "为通用 CSV 显式指定列映射后导入",
                onClick = {
                    ui.lastAdapterId = "generic_csv"
                    ui.screen = Screen.MAPPING
                },
            )
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

private fun sourceTypeLabel(adapterId: String): String = when (adapterId) {
    "wechat_statement" -> "微信账单"
    "generic_csv" -> "通用 CSV"
    "ofx" -> "OFX / QFX"
    else -> "账单来源"
}