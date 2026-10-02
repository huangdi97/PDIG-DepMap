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
 * Infrastructure Overview：Globe 舞台（L1）视觉主导 + 右活动轨 + 底部快速入口 + Region List 非视觉替代。
 * 大屏：Globe 左 + 活动轨右 + 底部快速入口；手机：Globe 上 + 活动轨下 + 快速入口（垂直滚动）。
 * 空态（region-selected / emptyDemo）：Globe 舞台保留，活动轨显示 honest unknown EmptyState。
 * 注意：compact 外层已是 verticalScroll，内层活动轨必须非滚动（嵌套 scrollable 会因无限高度约束崩溃）。
 */
@Composable
fun OverviewScreen(app: VAppState, breakpoint: MediaBreakpoint) {
    val regions = app.demoRegions()
    val arcingPairs = arcPairs()
    if (breakpoint == MediaBreakpoint.EXPANDED || breakpoint == MediaBreakpoint.MEDIUM) {
        WideOverview(app, regions, arcingPairs, breakpoint)
    } else {
        CompactOverview(app, regions, arcingPairs)
    }
}

@Composable
private fun WideOverview(app: VAppState, regions: List<RegionPresentation>, arcingPairs: List<Pair<String, String>>, breakpoint: MediaBreakpoint) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        Row(Modifier.fillMaxSize()) {
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
                Column(Modifier.align(Alignment.TopStart).padding(20.dp)) {
                    Text("我的基础设施", color = PdigV2Colors.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "点击地区聚焦 · 滚轮缩放 · 再次点击打开地区抽屉",
                        color = PdigV2Colors.TextMuted,
                        fontSize = 12.sp,
                    )
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
private fun CompactOverview(app: VAppState, regions: List<RegionPresentation>, arcingPairs: List<Pair<String, String>>) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("我的基础设施", color = PdigV2Colors.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("点击地区聚焦 · 再次点击打开地区抽屉", color = PdigV2Colors.TextMuted, fontSize = 12.sp)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
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
        QuickEntryRow(app)
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
        SectionHeader("地区（Region List）")
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

@Composable
private fun QuickEntryRow(app: VAppState) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(104.dp)
            .testTagLocal(VTestIds.OVERVIEW_QUICK),
        color = PdigV2Colors.Surface.copy(alpha = 0.9f),
        shape = RoundedCornerShape(VRadius.Lg),
        border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            QuickEntry("查看卡片", "全球 ${app.demoCards().size} 张卡") { app.navigate(VScreen.CARDS) }
            QuickEntry("查看号码", "全球 ${app.demoNumbers().size} 个号码") { app.navigate(VScreen.NUMBERS) }
            QuickEntry("更换手机号", "旗舰流程") { app.navigate(VScreen.CHANGE_PHONE) }
            QuickEntry("基础设施薄弱点", "待确认风险") { app.navigate(VScreen.WEAKNESSES) }
        }
    }
}

@Composable
private fun RowScope.QuickEntry(title: String, hint: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight(0.72f)
            .clickableLocal(onClick = onClick),
        color = PdigV2Colors.SurfaceRaised,
        shape = RoundedCornerShape(VRadius.Md),
        border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.Center) {
            Text(title, color = PdigV2Colors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(hint, color = PdigV2Colors.TextMuted, fontSize = 12.sp)
        }
    }
}

/** 跨区真实关系 → 地区对（去重）。 */
internal fun arcPairs(): List<Pair<String, String>> {
    fun regionOf(id: String): String = when {
        UiVNextDemoFixture.cards.any { it.id == id } -> UiVNextDemoFixture.cards.first { it.id == id }.region
        UiVNextDemoFixture.numbers.any { it.id == id } -> UiVNextDemoFixture.numbers.first { it.id == id }.region
        UiVNextDemoFixture.services.any { it.id == id } -> UiVNextDemoFixture.services.first { it.id == id }.region
        else -> ""
    }
    return UiVNextDemoFixture.crossRegionRelations()
        .mapNotNull { (a, b) ->
            val ra = regionOf(a)
            val rb = regionOf(b)
            if (ra.isNotEmpty() && rb.isNotEmpty() && ra != rb) ra to rb else null
        }
        .distinct()
}
