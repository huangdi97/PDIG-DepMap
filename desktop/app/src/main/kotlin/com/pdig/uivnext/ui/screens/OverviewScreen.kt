package com.pdig.uivnext.ui.screens

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
import com.pdig.uivnext.globe.VNextGlobe
import com.pdig.uivnext.model.MediaBreakpoint
import com.pdig.uivnext.model.VTestIds
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.ui.VAppState
import com.pdig.uivnext.ui.components.AttentionRow
import com.pdig.uivnext.ui.components.RegionListItem
import com.pdig.uivnext.ui.components.SectionHeader

/**
 * Infrastructure Overview：Globe 舞台（L1）视觉主导 + 右活动轨 + 底部快速入口 + Region List 非视觉替代。
 * 1920×1080：Globe Stage ≈55–65% 宽 × 65–78% 高；右轨 300–380px；底部快速入口 88–120px。
 */
@Composable
fun OverviewScreen(app: VAppState, breakpoint: MediaBreakpoint) {
    val regions = UiVNextDemoFixture.regionSummaries()
    val arcingPairs = arcPairs()
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
                        .testTagLocal(VTestIds.GLOBE_STAGE),
                    color = PdigV2Colors.SurfaceGlass,
                    shape = RoundedCornerShape(VRadius.Xl),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
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
                    .width(if (breakpoint == MediaBreakpoint.WIDE) 360.dp else 320.dp)
                    .fillMaxHeight()
                    .testTagLocal(VTestIds.OVERVIEW_ACTIVITY),
                color = PdigV2Colors.Surface.copy(alpha = 0.92f),
                shape = RoundedCornerShape(VRadius.Xl),
                border = androidx.compose.foundation.BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    SectionHeader("地区（Region List）")
                    regions.forEach { region ->
                        RegionListItem(
                            region = region,
                            selected = app.regionFilter == region.regionCode,
                            onClick = { app.selectRegion(region.regionCode) },
                        )
                    }
                    SectionHeader("需要处理")
                    UiVNextDemoFixture.attentionItems.forEach { item ->
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
        // Bottom Quick Entry
        Spacer(Modifier.height(20.dp))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(104.dp)
                .testTagLocal(VTestIds.OVERVIEW_QUICK),
            color = PdigV2Colors.Surface.copy(alpha = 0.9f),
            shape = RoundedCornerShape(VRadius.Lg),
            border = androidx.compose.foundation.BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
        ) {
            Row(
                Modifier.fillMaxSize().padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                QuickEntry("查看卡片", "全球 ${UiVNextDemoFixture.cards.size} 张卡") { app.navigate(com.pdig.uivnext.model.VScreen.CARDS) }
                QuickEntry("查看号码", "全球 ${UiVNextDemoFixture.numbers.size} 个号码") { app.navigate(com.pdig.uivnext.model.VScreen.NUMBERS) }
                QuickEntry("更换手机号", "旗舰流程") { app.navigate(com.pdig.uivnext.model.VScreen.CHANGE_PHONE) }
                QuickEntry("基础设施薄弱点", "待确认风险") { app.navigate(com.pdig.uivnext.model.VScreen.WEAKNESSES) }
            }
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
        border = androidx.compose.foundation.BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
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