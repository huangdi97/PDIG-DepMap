package com.pdig.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pdig.desktop.ui.theme.PdigDesktopTokens as T
import com.pdig.desktop.ui.theme.PdigType

/**
 * 应用壳：Gate（未打开数据文件）→ 主导航框架（分组 Sidebar + 内容区）。
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

/** 导航分组：每项 = 图标 + 标题 + 目标屏。 */
private data class NavItem(val title: String, val icon: ImageVector, val target: Screen)

private val NAV_GROUPS: List<Pair<String, List<NavItem>>> = listOf(
    "概览" to listOf(
        NavItem("首页", Icons.Filled.Home, Screen.HOME),
        NavItem("需要处理", Icons.Filled.MonitorHeart, Screen.ATTENTION),
    ),
    "数据" to listOf(
        NavItem("数据来源", Icons.Filled.Description, Screen.SOURCES),
    ),
    "检查与变更" to listOf(
        NavItem("基础设施", Icons.Filled.AccountBox, Screen.INFRA),
        NavItem("基础设施薄弱点", Icons.Filled.PersonSearch, Screen.FINDINGS),
        NavItem("场景中心", Icons.Filled.SyncAlt, Screen.SCENARIOS),
        NavItem("时间线", Icons.Filled.EventNote, Screen.TIMELINE),
    ),
    "维护" to listOf(
        NavItem("备份", Icons.Filled.Backup, Screen.BACKUP),
        NavItem("设置", Icons.Filled.Settings, Screen.SETTINGS),
        NavItem("安全", Icons.Filled.Shield, Screen.SECURITY),
    ),
)

@Composable
private fun AppFrame(ui: UiState) {
    Row(Modifier.fillMaxSize()) {
        Sidebar(ui)
        Column(Modifier.fillMaxSize()) {
            TopBar(ui)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
            KeyedContent(ui)
        }
    }
}

/** 顶栏：当前页标题 + 数据状态（不显示文件名/内部 ID）。 */
@Composable
private fun TopBar(ui: UiState) {
    Surface(Modifier.fillMaxWidth().height(52.dp), color = MaterialTheme.colorScheme.background) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = T.SpaceXxl),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                ui.screen.title,
                style = PdigType.SectionTitle,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.weight(1f))
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(6.dp)) {
                Text(
                    "本地数据文件已就绪",
                    Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                    style = PdigType.Label,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

@Composable
private fun Sidebar(ui: UiState) {
    Surface(
        Modifier
            .width(T.SidebarWidth)
            .fillMaxHeight(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.fillMaxHeight()) {
            // 品牌块
            // 品牌块（放大：mark 36dp + PDIG 20sp）
            Row(
                Modifier.padding(horizontal = T.SpaceLg, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    Modifier.size(36.dp),
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text(
                        "P",
                        Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = PdigType.SectionTitle,
                    )
                }
                Spacer(Modifier.width(T.SpaceMd))
                Column {
                    Text("PDIG", style = PdigType.SectionTitle, fontWeight = FontWeight.Bold)
                    Text("个人数字基础设施", style = PdigType.Meta, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
            // 分组导航
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(top = T.SpaceSm),
            ) {
                NAV_GROUPS.forEach { (group, items) ->
                    Text(
                        group,
                        Modifier.padding(horizontal = T.SpaceLg, vertical = 6.dp),
                        style = PdigType.Meta,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                    )
                    items.forEach { item ->
                        SidebarItem(item, selected = ui.screen == item.target, onClick = { ui.screen = item.target })
                    }
                    Spacer(Modifier.height(T.SpaceSm))
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
            Text(
                "PDIG 0.3.1 预览",
                Modifier.padding(horizontal = T.SpaceLg, vertical = T.SpaceMd),
                style = PdigType.Meta,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SidebarItem(item: NavItem, selected: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(interactionSource = interaction, indication = null) { onClick() }
            .background(
                when {
                    selected -> MaterialTheme.colorScheme.primaryContainer
                    hovered -> MaterialTheme.colorScheme.surfaceVariant
                    else -> Color.Transparent
                },
                RoundedCornerShape(6.dp),
            )
            .heightIn(min = 44.dp)
            .padding(horizontal = T.SpaceLg, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 选中左侧 indigo 竖条（selected 标识）
        Box(Modifier.width(3.dp).height(18.dp).background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(2.dp)))
        Spacer(Modifier.width(T.SpaceMd))
        Icon(
            item.icon,
            contentDescription = item.title,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(T.SpaceMd))
        Text(
            item.title,
            style = PdigType.Body,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun KeyedContent(ui: UiState) {
    // 读 refreshKey 键：任何 refresh() 使当前屏面重查询（State 读取即订阅）
    val refreshKey = ui.refreshKey
    if (refreshKey < 0) return@KeyedContent // unreachable; keeps the read observable
    Surface(Modifier.fillMaxSize().focusable(), color = MaterialTheme.colorScheme.background) {
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