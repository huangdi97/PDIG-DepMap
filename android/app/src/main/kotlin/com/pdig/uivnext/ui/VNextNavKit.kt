package com.pdig.uivnext.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.model.VTestIds
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.theme.VSpacing
import com.pdig.uivnext.theme.VTouchTarget

/** L2 Navigation Rail：collapsed 80 / expanded 188；一级 + 二级 + 基础设施二级。 */
@Composable
internal fun NavigationRail(app: VAppState) {
    val width = if (app.railExpanded) 188.dp else 80.dp
    Surface(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .testTag(VTestIds.NAV_RAIL),
        color = PdigV2Colors.Surface.copy(alpha = 0.86f),
    ) {
        Column(Modifier.fillMaxSize().padding(vertical = VSpacing.Lg)) {
            Row(Modifier.padding(horizontal = VSpacing.Lg), verticalAlignment = Alignment.CenterVertically) {
                Surface(Modifier.size(34.dp), color = PdigV2Colors.Primary, shape = RoundedCornerShape(VRadius.Md)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("P", color = PdigV2Colors.CanvasDeep, fontWeight = FontWeight.Bold)
                    }
                }
                if (app.railExpanded) {
                    Spacer(Modifier.width(VSpacing.Md))
                    Column {
                        Text("PDIG", color = PdigV2Colors.TextPrimary, fontWeight = FontWeight.Bold)
                        Text("个人数字基础设施", color = PdigV2Colors.TextMuted, fontSize = 10.sp)
                    }
                }
            }
            Spacer(Modifier.height(VSpacing.Xxl))
            PRIMARY_ENTRIES.forEach { entry -> RailButton(entry.screen, entry.icon, app, expanded = app.railExpanded) }
            Spacer(Modifier.height(VSpacing.Xl))
            RailSectionLabel("基础设施", expanded = app.railExpanded)
            INFRA_ENTRIES.forEach { entry -> RailButton(entry.screen, entry.icon, app, expanded = app.railExpanded) }
            Spacer(Modifier.height(VSpacing.Xl))
            SECONDARY_ENTRIES.forEach { entry -> RailButton(entry.screen, entry.icon, app, expanded = app.railExpanded) }
            Spacer(Modifier.weight(1f))
            // 折叠/展开开关
            Surface(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = VSpacing.Sm)
                    .clickable { app.railExpanded = !app.railExpanded },
                shape = RoundedCornerShape(VRadius.Md),
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

@Composable
private fun RailSectionLabel(label: String, expanded: Boolean) {
    if (!expanded) return
    Text(
        label,
        Modifier.padding(horizontal = VSpacing.Lg, vertical = VSpacing.Sm),
        color = PdigV2Colors.TextMuted,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
private fun RailButton(screen: VScreen, icon: ImageVector, app: VAppState, expanded: Boolean) {
    val selected = isEntrySelected(screen, app.screen)
    val bg = if (selected) PdigV2Colors.Primary.copy(alpha = 0.2f) else PdigV2Colors.SurfaceGlass
    val fg = if (selected) PdigV2Colors.PrimaryBright else PdigV2Colors.TextSecondary
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = VSpacing.Sm, vertical = 2.dp)
            .defaultMinSize(minHeight = VTouchTarget.Min)
            .clickable { app.navigate(screen) }
            .testTag("pdig.nav.${screen.route}"),
        color = bg,
        shape = RoundedCornerShape(VRadius.Md),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = VSpacing.Md, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = screen.titleZh, tint = fg, modifier = Modifier.size(18.dp))
            if (expanded) {
                Spacer(Modifier.width(VSpacing.Lg))
                Text(
                    screen.titleZh,
                    color = if (selected) PdigV2Colors.TextPrimary else PdigV2Colors.TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

/** 手机底部导航（一级 4 项 ≤5；触控目标由 NavigationBarItem 保证 ≥48dp）。 */
@Composable
internal fun BottomNav(app: VAppState) {
    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        containerColor = PdigV2Colors.Surface.copy(alpha = 0.96f),
        tonalElevation = 0.dp,
    ) {
        PRIMARY_ENTRIES.forEach { entry ->
            val selected = isEntrySelected(entry.screen, app.screen)
            NavigationBarItem(
                selected = selected,
                onClick = { app.navigate(entry.screen) },
                icon = {
                    Icon(
                        entry.icon,
                        contentDescription = entry.screen.titleZh,
                        tint = if (selected) PdigV2Colors.PrimaryBright else PdigV2Colors.TextSecondary,
                        modifier = Modifier.testTag("pdig.nav.${entry.screen.route}"),
                    )
                },
                label = {
                    Text(
                        entry.screen.titleZh,
                        fontSize = 10.sp,
                        color = if (selected) PdigV2Colors.PrimaryBright else PdigV2Colors.TextMuted,
                        modifier = Modifier.testTag("pdig.nav.${entry.screen.route}.label"),
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = PdigV2Colors.Primary.copy(alpha = 0.22f),
                    selectedIconColor = PdigV2Colors.PrimaryBright,
                    selectedTextColor = PdigV2Colors.PrimaryBright,
                    unselectedIconColor = PdigV2Colors.TextSecondary,
                    unselectedTextColor = PdigV2Colors.TextMuted,
                ),
            )
        }
    }
}

/** 手机上的基础设施二级导航（横向滚动 chip 行；与 rail 内嵌小节同语义）。
 *  B17：selected item auto-centering —— 切换到 设备/服务/薄弱点 等靠后项时，
 *  selected chip 自动滚动到可视区域中心，绝不留在屏幕外。 */
@Composable
internal fun InfraChipRow(app: VAppState) {
    val scrollState = rememberScrollState()
    val chipOffsets = remember { mutableMapOf<String, Int>() }
    val containerWidth = remember { mutableStateOf(0) }

    LaunchedEffect(app.screen) {
        val current = chipOffsets[app.screen.route] ?: return@LaunchedEffect
        val target = (current - containerWidth.value / 2).coerceAtLeast(0)
        if (target != scrollState.value) scrollState.animateScrollTo(target)
    }

    Row(
        Modifier
            .fillMaxWidth()
            .background(PdigV2Colors.Surface.copy(alpha = 0.72f))
            .horizontalScroll(scrollState)
            .padding(horizontal = VSpacing.PagePadding, vertical = VSpacing.Sm)
            .onGloballyPositioned { containerWidth.value = it.size.width },
        horizontalArrangement = Arrangement.spacedBy(VSpacing.Sm),
    ) {
        INFRA_ENTRIES.forEach { entry ->
            val selected = isEntrySelected(entry.screen, app.screen)
            Surface(
                modifier = Modifier
                    .defaultMinSize(minHeight = VTouchTarget.Min)
                    .onGloballyPositioned { chipOffsets[entry.screen.route] = it.positionInRoot().x.toInt() }
                    .clickable { app.navigate(entry.screen) }
                    .testTag("pdig.nav.${entry.screen.route}"),
                color = if (selected) PdigV2Colors.Primary.copy(alpha = 0.28f) else PdigV2Colors.SurfaceRaised,
                shape = RoundedCornerShape(VRadius.Sm),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (selected) PdigV2Colors.PrimaryBright else PdigV2Colors.BorderSubtle,
                ),
            ) {
                Row(
                    Modifier.padding(horizontal = VSpacing.Md, vertical = VSpacing.Sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(entry.icon, contentDescription = null, tint = if (selected) PdigV2Colors.PrimaryBright else PdigV2Colors.TextSecondary, modifier = Modifier.size(14.dp))
                    Text(
                        entry.screen.titleZh,
                        color = if (selected) PdigV2Colors.PrimaryBright else PdigV2Colors.TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

