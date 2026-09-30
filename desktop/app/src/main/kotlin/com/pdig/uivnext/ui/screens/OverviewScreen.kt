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
import androidx.compose.ui.unit.dp
import com.pdig.uivnext.demo.UiVNextDemoFixture
import com.pdig.uivnext.globe.VNextGlobe
import com.pdig.uivnext.model.MediaBreakpoint
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.model.VTestIds
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.theme.VSpacing
import com.pdig.uivnext.theme.VType
import com.pdig.uivnext.ui.VAppState
import com.pdig.uivnext.ui.components.AttentionRow
import com.pdig.uivnext.ui.components.RegionListItem
import com.pdig.uivnext.ui.components.SectionHeader

/**
 * Infrastructure Overview（G2/G10）：Globe 舞台（L1 spatial stage，edge-to-edge）。
 *
 * 布局：LEFT rail（shell）→ CENTER globe 舞台（52–64% 宽 × 64–78% 高）→
 * RIGHT context/activity rail（solid L3）→ BOTTOM 快捷动作（floating glass L2）。
 * 顺序：environment → globe → overlay → controls。
 * Globe 上直接绘制地区锚点 label（region code + 名称 + 计数）。
 */
@Composable
fun OverviewScreen(app: VAppState, breakpoint: MediaBreakpoint) {
    val regions = UiVNextDemoFixture.regionSummaries()
    val arcingPairs = arcPairs()
    Column(
        Modifier
            .fillMaxSize()
            .padding(VSpacing.Xxl),
    ) {
        PageHeader(
            title = "我的基础设施",
            subtitle = "你的数字基础设施分布在全球哪些地方 · 点击地区聚焦，再次点击打开地区抽屉",
        )
        Spacer(Modifier.height(VSpacing.Xxl))
        Row(
            Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(VSpacing.Xl),
        ) {
            // CENTER Globe Stage（L1 spatial；62% content 宽，无卡片 chrome）
            Box(
                Modifier
                    .fillMaxWidth(0.62f)
                    .fillMaxHeight()
                    .testTagLocal(VTestIds.GLOBE_STAGE),
            ) {
                VNextGlobe(
                    controller = app.globe,
                    regions = regions,
                    arcingPairs = arcingPairs,
                    reduceMotion = app.reduceMotion,
                    showRegionLabels = true,
                )
                Surface(
                    Modifier.align(Alignment.BottomStart).padding(VSpacing.Lg),
                    color = PdigV2Colors.SurfaceGlass,
                    shape = RoundedCornerShape(VRadius.Md),
                ) {
                    Text(
                        "点击地区聚焦 · 滚轮缩放 · 再次点击打开地区抽屉",
                        Modifier.padding(horizontal = VSpacing.Lg, vertical = VSpacing.Sm),
                        style = VType.Secondary,
                        color = PdigV2Colors.TextSecondary,
                    )
                }
            }
            // RIGHT Activity Rail（L3 Solid Data Surface；300–380px）
            Surface(
                modifier = Modifier
                    .width(if (breakpoint == MediaBreakpoint.WIDE) 360.dp else 320.dp)
                    .fillMaxHeight()
                    .testTagLocal(VTestIds.OVERVIEW_ACTIVITY),
                color = PdigV2Colors.Surface.copy(alpha = 0.96f),
                shape = RoundedCornerShape(VRadius.Xl),
                border = androidx.compose.foundation.BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(VSpacing.Xl),
                    verticalArrangement = Arrangement.spacedBy(VSpacing.Lg),
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
        // BOTTOM Quick Entry（L2 floating glass；88–120px）
        Spacer(Modifier.height(VSpacing.Xl))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .testTagLocal(VTestIds.OVERVIEW_QUICK),
            color = PdigV2Colors.SurfaceGlass,
            shape = RoundedCornerShape(VRadius.Lg),
            border = androidx.compose.foundation.BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
        ) {
            Row(
                Modifier.fillMaxSize().padding(horizontal = VSpacing.Xxl),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(VSpacing.Lg),
            ) {
                QuickEntry("查看卡片", "全球 ${UiVNextDemoFixture.cards.size} 张卡") { app.navigate(VScreen.CARDS) }
                QuickEntry("查看号码", "全球 ${UiVNextDemoFixture.numbers.size} 个号码") { app.navigate(VScreen.NUMBERS) }
                QuickEntry("更换手机号", "旗舰流程") { app.navigate(VScreen.CHANGE_PHONE) }
                QuickEntry("基础设施薄弱点", "待确认风险") { app.navigate(VScreen.WEAKNESSES) }
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
        color = PdigV2Colors.SurfaceRaised.copy(alpha = 0.9f),
        shape = RoundedCornerShape(VRadius.Md),
        border = androidx.compose.foundation.BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        Column(Modifier.padding(VSpacing.Lg), verticalArrangement = Arrangement.Center) {
            Text(title, color = PdigV2Colors.TextPrimary, style = VType.Label)
            Text(hint, color = PdigV2Colors.TextMuted, style = VType.Meta)
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
