package com.pdig.uivnext.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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

/** Android wide navigation rail：只承载一级目的地与低频工具；基础设施二级留在内容区 sibling navigation。 */
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
            Spacer(Modifier.weight(1f))
            SECONDARY_ENTRIES.forEach { entry -> RailButton(entry.screen, entry.icon, app, expanded = app.railExpanded) }
            Spacer(Modifier.height(VSpacing.Sm))
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
private fun RailButton(screen: VScreen, icon: ImageVector, app: VAppState, expanded: Boolean) {
    val selected = isEntrySelected(screen, app.screen)
    val bg = if (selected) PdigV2Colors.PrimarySoft else PdigV2Colors.Surface.copy(alpha = 0f)
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
            .navigationBarsPadding()
            .testTag(VTestIds.NAV_BOTTOM),
        containerColor = PdigV2Colors.Surface.copy(alpha = 0.96f),
        tonalElevation = 0.dp,
    ) {
        PRIMARY_ENTRIES.forEach { entry ->
            val selected = isEntrySelected(entry.screen, app.screen)
            NavigationBarItem(
                modifier = Modifier.testTag("pdig.nav.${entry.screen.route}"),
                selected = selected,
                onClick = { app.navigate(entry.screen) },
                icon = {
                    Icon(
                        entry.icon,
                        contentDescription = entry.screen.titleZh,
                        tint = if (selected) PdigV2Colors.PrimaryBright else PdigV2Colors.TextSecondary,
                        modifier = Modifier.testTag("pdig.nav.${entry.screen.route}.icon"),
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

/** 大屏基础设施二级 sibling navigation。
 * Phone 使用 Overview 中的 8 类管理 Hub；Tablet / Expanded 在内容区保留横向 sibling navigation，
 * 避免把对象类别误当成全局一级目的地。selected item 会自动 reveal。 */
@Composable
internal fun InfraChipRow(app: VAppState) {
    val listState = rememberLazyListState()
    val selectedIndex = INFRA_ENTRIES.indexOfFirst { isEntrySelected(it.screen, app.screen) }

    // Always reveal the selected destination as a complete chip. The previous root-coordinate
    // calculation could leave late entries (服务 / 薄弱点) partially or fully clipped after navigation.
    LaunchedEffect(app.screen) {
        if (selectedIndex >= 0) listState.animateScrollToItem(selectedIndex)
    }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(PdigV2Colors.Surface.copy(alpha = 0.48f))
            .testTag("pdig.nav.infra.secondary"),
        state = listState,
        contentPadding = PaddingValues(
            horizontal = VSpacing.Xxl,
            vertical = VSpacing.Sm,
        ),
        horizontalArrangement = Arrangement.spacedBy(VSpacing.Sm),
    ) {
        INFRA_ENTRIES.forEach { entry ->
            item(key = entry.screen.route) {
                val selected = isEntrySelected(entry.screen, app.screen)
                Surface(
                    modifier = Modifier
                        .defaultMinSize(minHeight = VTouchTarget.Min)
                        .clickable { app.navigate(entry.screen) }
                        .testTag("pdig.nav.${entry.screen.route}"),
                    color = if (selected) PdigV2Colors.Primary.copy(alpha = 0.22f) else PdigV2Colors.Surface.copy(alpha = 0f),
                    shape = RoundedCornerShape(VRadius.Sm),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (selected) PdigV2Colors.PrimaryBright.copy(alpha = 0.9f)
                        else PdigV2Colors.BorderSubtle.copy(alpha = 0.45f),
                    ),
                ) {
                    Row(
                        Modifier.padding(horizontal = VSpacing.Lg, vertical = VSpacing.Sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            entry.icon,
                            contentDescription = null,
                            tint = if (selected) PdigV2Colors.PrimaryBright else PdigV2Colors.TextSecondary,
                            modifier = Modifier.size(14.dp),
                        )
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
}

