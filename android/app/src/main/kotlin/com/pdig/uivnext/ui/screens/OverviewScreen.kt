package com.pdig.uivnext.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.demo.UiVNextDemoFixture
import com.pdig.uivnext.demo.demoAttention
import com.pdig.uivnext.demo.demoCards
import com.pdig.uivnext.demo.demoNumbers
import com.pdig.uivnext.demo.demoRegions
import com.pdig.uivnext.globe.VNextGlobe
import com.pdig.uivnext.model.MediaBreakpoint
import com.pdig.uivnext.model.RegionPresentation
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.model.VTestIds
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.ui.VAppState
import com.pdig.uivnext.ui.components.AttentionRow
import com.pdig.uivnext.ui.components.EmptyKind
import com.pdig.uivnext.ui.components.EmptyState
import com.pdig.uivnext.ui.components.RegionListItem
import com.pdig.uivnext.ui.components.SectionHeader

/**
 * Infrastructure Overview：
 * - Phone = 8 类对象管理 Hub + compact regional Globe + Region/Attention context；
 * - Medium = single-pane spatial overview；
 * - Expanded = Globe stage + activity rail + quick entries。
 *
 * Now 承担“全球世界观 / 当前最重要任务”的大 Globe；Phone Overview 不再复制第二个 Now。
 * 空态继续保留 honest unknown；compact 外层已是 verticalScroll，内部活动轨禁止再嵌套滚动。
 */
@Composable
fun OverviewScreen(app: VAppState, breakpoint: MediaBreakpoint) {
    val regions = app.demoRegions()
    val arcingPairs = arcPairs()
    if (breakpoint == MediaBreakpoint.EXPANDED) {
        WideOverview(app, regions, arcingPairs, breakpoint)
    } else {
        SinglePaneOverview(app, regions, arcingPairs, breakpoint)
    }
}

@Composable
private fun WideOverview(app: VAppState, regions: List<RegionPresentation>, arcingPairs: List<Pair<String, String>>, breakpoint: MediaBreakpoint) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        Row(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            // Globe Stage（L1）
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(end = 20.dp),
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .heightIn(min = 320.dp)
                        .testTagLocal(VTestIds.GLOBE_STAGE),
                    color = PdigV2Colors.SurfaceGlass,
                    shape = RoundedCornerShape(VRadius.Xl),
                    border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
                ) {
                    Box(Modifier.padding(12.dp)) {
                        VNextGlobe(
                            controller = app.globe,
                            regions = regions,
                            arcingPairs = arcingPairs,
                            reduceMotion = app.reduceMotion,
                        )
                    }
                }
                Surface(
                    modifier = Modifier.align(Alignment.TopStart).padding(18.dp),
                    color = PdigV2Colors.Surface.copy(alpha = 0.86f),
                    shape = RoundedCornerShape(VRadius.Lg),
                    border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
                ) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Text("我的基础设施", color = PdigV2Colors.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "点按地区聚焦 · 捏合缩放 · 再次点按查看地区",
                            color = PdigV2Colors.TextSecondary,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
            // Right Activity Rail（L3 Data Surface）
            Surface(
                modifier = Modifier
                    .width(if (app.railExpanded && breakpoint == MediaBreakpoint.EXPANDED) 360.dp else 320.dp)
                    .fillMaxHeight()
                    .testTagLocal(VTestIds.OVERVIEW_ACTIVITY),
                color = PdigV2Colors.Surface.copy(alpha = 0.92f),
                shape = RoundedCornerShape(VRadius.Xl),
                border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
            ) {
                ActivityRailContent(app, regions, scrollable = true)
            }
        }
        // Bottom Quick Entry
        Spacer(Modifier.height(20.dp))
        QuickEntryRow(app)
    }
}

@Composable
private fun SinglePaneOverview(
    app: VAppState,
    regions: List<RegionPresentation>,
    arcingPairs: List<Pair<String, String>>,
    breakpoint: MediaBreakpoint,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(pagePadding(breakpoint)),
        verticalArrangement = Arrangement.spacedBy(pageSectionGap(breakpoint)),
    ) {
        if (breakpoint != MediaBreakpoint.COMPACT) {
            Text("我的基础设施", color = PdigV2Colors.TextPrimary, fontSize = pageTitleSize(breakpoint), fontWeight = FontWeight.Bold)
        }
        Text(
            if (breakpoint == MediaBreakpoint.COMPACT) "按对象管理，按地区查看你的全球基础设施" else "点按地区聚焦 · 再次点按查看地区",
            color = PdigV2Colors.TextMuted,
            fontSize = 12.sp,
        )
        if (breakpoint == MediaBreakpoint.COMPACT) {
            CompactInfrastructureHub(app)
        }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (breakpoint == MediaBreakpoint.COMPACT) 230.dp else 380.dp)
                .testTagLocal(VTestIds.GLOBE_STAGE),
            color = PdigV2Colors.SurfaceGlass,
            shape = RoundedCornerShape(VRadius.Xl),
            border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
        ) {
            Box(Modifier.padding(12.dp)) {
                VNextGlobe(
                    controller = app.globe,
                    regions = regions,
                    arcingPairs = arcingPairs,
                    reduceMotion = app.reduceMotion,
                )
            }
        }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTagLocal(VTestIds.OVERVIEW_ACTIVITY),
            color = PdigV2Colors.Surface.copy(alpha = 0.92f),
            shape = RoundedCornerShape(VRadius.Xl),
            border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
        ) {
            ActivityRailContent(app, regions, scrollable = false)
        }
        if (breakpoint != MediaBreakpoint.COMPACT) {
            CompactQuickEntries(app)
        }
    }
}

@Composable
private fun ActivityRailContent(app: VAppState, regions: List<RegionPresentation>, scrollable: Boolean) {
    // SAFETY: compact 容器已是 verticalScroll —— 这里再嵌套 scrollable 会被 Compose 以
    // 「infinity max height constraints」拒绝并崩溃（实测 AVD 复现），故由调用方决定是否滚动。
    val scrollModifier = if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier
    Column(
        Modifier
            .fillMaxWidth()
            .then(scrollModifier)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionHeader("地区")
        if (regions.isEmpty()) {
            EmptyState(
                kind = EmptyKind.REGION,
                title = "没有可展示的地区",
                description = "尚未记录任何地区的基础设施。没有记录 ≠ 没有风险：地区数据录入后才会出现在这里。",
                primaryCta = "查看卡片",
                onPrimary = { app.navigate(VScreen.CARDS) },
                secondaryCta = "查看号码",
                onSecondary = { app.navigate(VScreen.NUMBERS) },
            )
        } else {
            regions.forEach { region ->
                RegionListItem(
                    region = region,
                    selected = app.regionFilter == region.regionCode,
                    onClick = { app.selectRegion(region.regionCode) },
                )
            }
        }
        SectionHeader("需要处理")
        val attention = app.demoAttention()
        if (attention.isEmpty()) {
            Text(
                "未记录 ≠ 无风险：当前没有可展示的关注事项，不代表一切安全。",
                color = PdigV2Colors.TextMuted,
                fontSize = 12.sp,
            )
        } else {
            attention.forEach { item ->
                AttentionRow(item = item, onClick = { clicked ->
                    when {
                        UiVNextDemoFixture.cardById(clicked.target) != null -> app.openCard(clicked.target)
                        else -> app.openNumber(clicked.target)
                    }
                })
            }
        }
    }
}
