package com.pdig.desktop.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.pdig.core.scenario.ScenarioTemplate
import com.pdig.desktop.ui.components.ChipTone
import com.pdig.desktop.ui.components.PdigCard
import com.pdig.desktop.ui.components.PdigPage
import com.pdig.desktop.ui.components.SectionDivider
import com.pdig.desktop.ui.components.StatusChip

/** 场景中心：可执行场景与 planned（未实现）场景列表。 */
@Composable
fun ScenariosScreen(ui: UiState) {
    PdigPage(
        title = "场景中心",
        subtitle = "模拟换卡 / 注销一张卡会影响到谁，以及该先处理什么",
        notice = ui.notice,
        error = ui.error,
        onDismissNotice = { ui.notice = null },
        onDismissError = { ui.error = null },
    ) {
        Column {
            SectionDivider("可执行场景")
            com.pdig.core.scenario.ScenarioRegistry.active.forEach { t ->
                PdigCard(
                    title = t.title,
                    subtitle = t.description + leadTimeSuffix(t),
                    onClick = {
                        ui.selectedScenarioId = t.id
                        ui.screen = Screen.SCENARIO_SETUP
                    },
                )
            }
            SectionDivider("计划中（未实现）")
            com.pdig.core.scenario.ScenarioRegistry.planned.forEach { t ->
                PdigCard(
                    title = t.title,
                    subtitle = t.description + leadTimeSuffix(t),
                    trailing = { StatusChip("计划中", ChipTone.NEUTRAL) },
                )
            }
        }
    }
}

private fun leadTimeSuffix(t: ScenarioTemplate): String =
    t.recommendedLeadTimeDays?.let { " · 建议提前 $it 天" } ?: ""