package com.pdig.uivnext.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Source
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.model.MediaBreakpoint
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.model.VSection
import com.pdig.uivnext.model.VTestIds
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.theme.VSpacing
import com.pdig.uivnext.ui.components.MaskEnabledIndicator

/** 一级入口（rail 顶部固定区）。 */
private data class RailEntry(val screen: VScreen, val icon: ImageVector)

/** Primary Rail 只保留四个一级项（Review §G/§5）：现在 / 基础设施 / 变更 / 记录。 */
private val PRIMARY_ENTRIES = listOf(
    RailEntry(VScreen.NOW, Icons.Filled.Home),
    RailEntry(VScreen.INFRASTRUCTURE, Icons.Filled.Public),
    RailEntry(VScreen.CHANGE, Icons.Filled.SyncAlt),
    RailEntry(VScreen.RECORDS, Icons.Filled.History),
)

/** Secondary：数据源 / 设置（置于 rail 底部，Review §5）。 */
private val SECONDARY_ENTRIES = listOf(
    RailEntry(VScreen.SOURCES, Icons.Filled.Source),
    RailEntry(VScreen.SETTINGS, Icons.Filled.Settings),
)

/** 基础设施二级：进入基础设施时在顶部显示为 segmented context rail（Review §4/§5）。 */
private val INFRA_ENTRIES = listOf(
    RailEntry(VScreen.OVERVIEW, Icons.Filled.Public),
    RailEntry(VScreen.CARDS, Icons.Filled.CreditCard),
    RailEntry(VScreen.NUMBERS, Icons.Filled.Dialpad),
    RailEntry(VScreen.ACCOUNTS, Icons.Filled.Category),
    RailEntry(VScreen.EMAILS, Icons.Filled.Source),
    RailEntry(VScreen.DEVICES, Icons.Filled.Category),
    RailEntry(VScreen.SERVICES, Icons.Filled.Category),
    RailEntry(VScreen.WEAKNESSES, Icons.Filled.Category),
)

@Composable
fun VNextShell(app: VAppState, viewportWidth: Int = 1920) {
    val breakpoint = when {
        viewportWidth >= 1440 -> MediaBreakpoint.WIDE
        viewportWidth >= 1024 -> MediaBreakpoint.MEDIUM
        else -> MediaBreakpoint.COMPACT
    }
    Row(Modifier.fillMaxSize()) {
        NavigationRail(app)
        Column(Modifier.weight(1f)) {
            TopCommandBar(app)
            // 基础设施二级 = 顶部 segmented context rail（永不永久占满侧栏）
            if (app.screen.section == VSection.INFRA) ContextRail(app)
            VNextContentHost(app, breakpoint)
        }
    }
}

/**
 * L2 Navigation Rail（Review §5 v2）：collapsed 68 / expanded 188；
 * 选中态 = subtle glow（低 alpha 底 + 细描边 + 左侧指示条），
 * 非选中透明；数据源/设置置于底部。
 */
@Composable
private fun NavigationRail(app: VAppState) {
    val width = if (app.railExpanded) 188.dp else 68.dp
    Surface(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .testTag(VTestIds.NAV_RAIL),
        color = PdigV2Colors.SurfaceGlass,
    ) {
        Column(Modifier.fillMaxSize().padding(vertical = VSpacing.Lg)) {
            Row(
                Modifier.padding(horizontal = if (app.railExpanded) VSpacing.Lg else VSpacing.Md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(Modifier.size(32.dp), color = PdigV2Colors.Primary, shape = RoundedCornerShape(VRadius.Md)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("P", color = PdigV2Colors.CanvasDeep, fontWeight = FontWeight.Bold)
                    }
                }
                if (app.railExpanded) {
                    Spacer(Modifier.width(VSpacing.Md))
                    Column {
                        Text("PDIG", color = PdigV2Colors.TextPrimary, fontWeight = FontWeight.Bold)
                        Text("vNext", color = PdigV2Colors.TextMuted, fontSize = 10.sp)
                    }
                }
            }
            Spacer(Modifier.height(VSpacing.Xxl))
            PRIMARY_ENTRIES.forEach { entry -> RailButton(entry.screen, entry.icon, app, expanded = app.railExpanded) }
            Spacer(Modifier.weight(1f))
            SECONDARY_ENTRIES.forEach { entry -> RailButton(entry.screen, entry.icon, app, expanded = app.railExpanded) }
            Spacer(Modifier.height(VSpacing.Sm))
            // 折叠/展开开关（底部）
            Surface(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = VSpacing.Sm)
                    .clickable { app.railExpanded = !app.railExpanded },
                shape = RoundedCornerShape(VRadius.Md),
                color = Color.Transparent,
            ) {
                Row(
                    Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (app.railExpanded) Icons.Filled.KeyboardArrowLeft else Icons.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = PdigV2Colors.TextMuted,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

/** Top segmented context rail：基础设施 8 个二级项（Review §4/§5）。 */
@Composable
private fun ContextRail(app: VAppState) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .testTag("pdig.nav.context"),
        color = PdigV2Colors.SurfaceGlass.copy(alpha = 0.72f),
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = VSpacing.Xxl),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            INFRA_ENTRIES.forEach { entry ->
                val selected = app.screen == entry.screen ||
                    (app.screen == VScreen.CARD_DETAIL && entry.screen == VScreen.CARDS) ||
                    (app.screen == VScreen.NUMBER_DETAIL && entry.screen == VScreen.NUMBERS) ||
                    (app.screen == VScreen.CARD_CUSTOMIZATION && entry.screen == VScreen.CARDS) ||
                    (app.screen == VScreen.NUMBER_CUSTOMIZATION && entry.screen == VScreen.NUMBERS)
                Surface(
                    modifier = Modifier
                        .clickable { app.navigate(entry.screen) }
                        .testTag("pdig.nav.context.${entry.screen.route}"),
                    color = if (selected) PdigV2Colors.PrimaryBright.copy(alpha = 0.14f) else Color.Transparent,
                    shape = RoundedCornerShape(VRadius.Sm),
                    border = if (selected) BorderStroke(1.dp, PdigV2Colors.PrimaryBright.copy(alpha = 0.22f)) else null,
                ) {
                    Text(
                        entry.screen.titleZh,
                        Modifier.padding(horizontal = VSpacing.Md, vertical = 6.dp),
                        color = if (selected) PdigV2Colors.PrimaryBright else PdigV2Colors.TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun RailButton(screen: VScreen, icon: ImageVector, app: VAppState, expanded: Boolean) {
    val selected = app.screen == screen ||
        (app.screen == VScreen.CARD_DETAIL && screen == VScreen.CARDS) ||
        (app.screen == VScreen.NUMBER_DETAIL && screen == VScreen.NUMBERS) ||
        (app.screen == VScreen.CARD_CUSTOMIZATION && screen == VScreen.CARDS) ||
        (app.screen == VScreen.NUMBER_CUSTOMIZATION && screen == VScreen.NUMBERS) ||
        (app.screen == VScreen.CHANGE_PHONE && screen == VScreen.CHANGE)
    // subtle glow：选中态仅低 alpha 底 + 细描边；非选中透明（无巨块填充矩形）
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .clickable { app.navigate(screen) }
            .testTag("pdig.nav.${screen.route}"),
        color = if (selected) PdigV2Colors.PrimaryBright.copy(alpha = 0.12f) else Color.Transparent,
        shape = RoundedCornerShape(VRadius.Md),
        border = if (selected) BorderStroke(1.dp, PdigV2Colors.PrimaryBright.copy(alpha = 0.25f)) else null,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = if (expanded) VSpacing.Md else VSpacing.Sm, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon,
                contentDescription = screen.titleZh,
                tint = if (selected) PdigV2Colors.PrimaryBright else PdigV2Colors.TextMuted,
                modifier = Modifier.size(18.dp),
            )
            if (expanded) {
                Spacer(Modifier.width(VSpacing.Lg))
                Text(
                    screen.titleZh,
                    color = if (selected) PdigV2Colors.TextPrimary else PdigV2Colors.TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                )
            }
        }
    }
}

/** L2 Top Command/Search：44–52px；glass chrome + 状态位 + 隐私遮蔽指示。 */
@Composable
private fun TopCommandBar(app: VAppState) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag(VTestIds.NAV_TOP),
        color = PdigV2Colors.SurfaceGlass,
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = VSpacing.Xxl),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(app.screen.titleZh, color = PdigV2Colors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(Modifier.weight(1f))
            Surface(
                color = PdigV2Colors.SurfaceRaised.copy(alpha = 0.9f),
                shape = RoundedCornerShape(VRadius.Sm),
                border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
            ) {
                Row(Modifier.padding(horizontal = VSpacing.Lg, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = PdigV2Colors.TextMuted, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(VSpacing.Sm))
                    Text("搜索 / 命令", color = PdigV2Colors.TextMuted, fontSize = 12.sp)
                    Spacer(Modifier.width(VSpacing.Lg))
                    Surface(color = PdigV2Colors.PrimarySoft, shape = RoundedCornerShape(4.dp)) {
                        Text(
                            "Ctrl K",
                            Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = PdigV2Colors.TextSecondary,
                            fontSize = 10.sp,
                        )
                    }
                }
            }
            Spacer(Modifier.width(VSpacing.Lg))
            MaskEnabledIndicator(app.privacyMask)
        }
    }
}
