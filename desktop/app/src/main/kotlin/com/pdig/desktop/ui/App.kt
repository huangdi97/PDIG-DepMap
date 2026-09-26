package com.pdig.desktop.ui

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 应用壳：Gate（未打开数据文件）→ 主导航框架（NavRail + 当前屏面）。
 * Graph 不是首页：启动落在 HOME。
 */
@Composable
fun PDIGAppShell(ui: UiState) {
    if (ui.dataFile == null) {
        GateScreen(ui)
    } else {
        AppFrame(ui)
    }
}

@Composable
private fun AppFrame(ui: UiState) {
    Row(Modifier.fillMaxSize()) {
        // 左侧导航（Top-level screens only；动态屏面从内容区进入）
        Surface(Modifier.width(180.dp).fillMaxHeight(), color = MaterialTheme.colorScheme.surfaceVariant) {
            Column(Modifier.padding(vertical = 12.dp).fillMaxHeight()) {
                Text(
                    "PDIG 0.3.0",
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                TOP_LEVEL_SCREENS.forEach { s ->
                    val selected = ui.screen == s
                    TextButton(onClick = { ui.screen = s }) {
                        Text(
                            s.title,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        KeyedContent(ui)
    }
}

@Composable
private fun KeyedContent(ui: UiState) {
    // 读 refreshKey 键：任何 refresh() 使当前屏面重查询（State 读取即订阅）
    val refreshKey = ui.refreshKey
    if (refreshKey < 0) return@KeyedContent // unreachable; keeps the read observable
    Surface(Modifier.fillMaxSize().focusable()) {
        when (ui.screen) {
            Screen.HOME -> HomeScreen(ui)
            Screen.ATTENTION -> AttentionScreen(ui)
            Screen.SOURCES -> SourcesScreen(ui)
            Screen.IMPORT -> ImportScreen(ui)
            Screen.MAPPING -> MappingScreen(ui)
            Screen.REVIEW -> ReviewScreen(ui)
            Screen.PROPOSALS -> ProposalsScreen(ui)
            Screen.CANDIDATES -> CandidatesScreen(ui)
            Screen.DRIFTS -> DriftsScreen(ui)
            Screen.INFRA -> InfraScreen(ui)
            Screen.NODE -> NodeDetailScreen(ui)
            Screen.FINDINGS -> FindingsScreen(ui)
            Screen.SCENARIOS -> ScenariosScreen(ui)
            Screen.SCENARIO_SETUP -> ScenarioSetupScreen(ui)
            Screen.IMPACT -> ImpactScreen(ui)
            Screen.PLAN -> PlanScreen(ui)
            Screen.ACTIONS -> ActionsScreen(ui)
            Screen.VERIFICATION -> VerificationScreen(ui)
            Screen.TIMELINE -> TimelineScreen(ui)
            Screen.BACKUP -> BackupScreen(ui)
            Screen.RESTORE -> RestoreScreen(ui)
            Screen.SETTINGS -> SettingsScreen(ui)
            Screen.SECURITY -> SecurityScreen(ui)
            Screen.ABOUT -> AboutScreen(ui)
        }
    }
}