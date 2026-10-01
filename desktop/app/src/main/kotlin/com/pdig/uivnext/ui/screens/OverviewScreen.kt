package com.pdig.uivnext.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.pdig.uivnext.demo.UiVNextDemoFixture
import com.pdig.uivnext.globe.VNextGlobe
import com.pdig.uivnext.model.MediaBreakpoint
import com.pdig.uivnext.model.RegionPresentation
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.model.VTestIds
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.theme.VSpacing
import com.pdig.uivnext.theme.VType
import com.pdig.uivnext.ui.VAppState
import com.pdig.uivnext.ui.components.AttentionRow
import com.pdig.uivnext.ui.components.RegionBadge
import com.pdig.uivnext.ui.components.RegionListItem
import com.pdig.uivnext.ui.components.SectionHeader

/**
 * Infrastructure Overview（Review §4 v2）：Globe 成为绝对主角（视觉 ≈60%）。
 *
 *  - 右区 = floating spatial inspector（glass 半透明、宽 320、与边缘留 28px，
 *    悬浮于 globe 环境上，不再是"右侧普通后台 panel"）；
 *  - 底部 = compact action dock / floating control strip（非 4 个 dashboard 卡片）；
 *  - Globe 上直接绘制地区锚点 label（region code + 名称 + 计数）。
 */
@Composable
fun OverviewScreen(app: VAppState, breakpoint: MediaBreakpoint) {
    val regions = UiVNextDemoFixture.regionSummaries()
    val arcingPairs = arcPairs()
    Column(
        Modifier
            .fillMaxSize()
            .padding(VSpacing.Xxl),
        verticalArrangement = Arrangement.spacedBy(VSpacing.Xl),
    ) {
        PageHeader(
            title = "我的基础设施",
            subtitle = "你的数字基础设施分布在全球哪些地方 · 点击地区聚焦，再次点击打开地区抽屉",
        )
        Row(
            Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(VSpacing.Xl),
        ) {
            // CENTER Globe Stage（L1 spatial；Globe 是主角，无卡片 chrome）
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
                    showRegionLabels = false,
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
            // RIGHT Floating Spatial Inspector（glass；宽 320；边缘留 28px）
            Box(Modifier.weight(1f).fillMaxHeight()) {
                FloatingSpatialInspector(app, regions)
            }
        }
        // BOTTOM Compact Action Dock（floating strip，非 dashboard 卡片）
        CompactActionDock(app)
    }
}

/**
 * Floating spatial inspector：轻量 region rows + subtle separator + 小选中面
 * （PHASE 1D §8：不要每个地区都是巨大 bordered card）。
 */
@Composable
private fun FloatingSpatialInspector(app: VAppState, regions: List<RegionPresentation>) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(0.78f)
            .fillMaxHeight()
            .padding(top = 0.dp, end = 0.dp)
            .testTagLocal(VTestIds.OVERVIEW_ACTIVITY),
        color = PdigV2Colors.SurfaceGlass.copy(alpha = 0.55f),
        shape = RoundedCornerShape(VRadius.Xl2),
        border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle.copy(alpha = 0.5f)),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(VSpacing.Xl),
            verticalArrangement = Arrangement.spacedBy(VSpacing.Sm),
        ) {
            SectionHeader("地区分布")
            regions.forEach { region ->
                LightRegionRow(
                    region = region,
                    selected = app.regionFilter == region.regionCode,
                    onClick = { app.selectRegion(region.regionCode) },
                )
            }
            Spacer(Modifier.height(VSpacing.Md))
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

/** 轻量地区行：glyph + 名称 + 计数 + subtle separator，选中 = 小高亮面（非大卡片）。 */
@Composable
private fun LightRegionRow(
    region: RegionPresentation,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val containerColor = if (selected) PdigV2Colors.PrimaryBright.copy(alpha = 0.10f) else Color.Transparent
    Surface(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = containerColor,
        shape = RoundedCornerShape(VRadius.Sm),
    ) {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = VSpacing.Sm, vertical = VSpacing.Sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RegionBadge(region.regionCode, badgeSize = 28.dp)
                Spacer(Modifier.width(VSpacing.Md))
                Text(
                    region.displayName,
                    style = VType.Body,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                    color = if (selected) PdigV2Colors.PrimaryBright else PdigV2Colors.TextPrimary,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "${region.cardCount} 卡 · ${region.phoneCount} 号",
                    style = VType.Meta,
                    color = PdigV2Colors.TextMuted,
                    maxLines = 1,
                )
                if (region.attentionCount > 0) {
                    Spacer(Modifier.width(VSpacing.Sm))
                    Surface(color = PdigV2Colors.Warning.copy(alpha = 0.16f), shape = RoundedCornerShape(VRadius.Sm)) {
                        Text(
                            "${region.attentionCount}",
                            Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                            style = VType.Label,
                            color = PdigV2Colors.Warning,
                        )
                    }
                }
            }
            androidx.compose.material3.HorizontalDivider(color = PdigV2Colors.BorderSubtle.copy(alpha = 0.35f), thickness = 1.dp)
        }
    }
}

/** 底部 floating spatial dock（PHASE 1D §9）：更窄、更低、更浮动、icon 主导、少边框。 */
@Composable
private fun CompactActionDock(app: VAppState) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.72f)
                .height(56.dp)
                .testTagLocal(VTestIds.OVERVIEW_QUICK),
            color = PdigV2Colors.SurfaceGlass,
            shape = RoundedCornerShape(VRadius.Pill),
            border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle.copy(alpha = 0.4f)),
        ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = VSpacing.Lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(VSpacing.Md),
        ) {
            DockAction("查看卡片", Icons.Filled.CreditCard) { app.navigate(VScreen.CARDS) }
            DockAction("查看号码", Icons.Filled.Dialpad) { app.navigate(VScreen.NUMBERS) }
            DockAction("更换手机号", Icons.Filled.SyncAlt) { app.navigate(VScreen.CHANGE_PHONE) }
            DockAction("薄弱点", Icons.Filled.Warning) { app.navigate(VScreen.WEAKNESSES) }
        }
        }
    }
}

@Composable
private fun RowScope.DockAction(title: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight(0.9f)
            .clickable(onClick = onClick),
        color = Color.Transparent,
        shape = RoundedCornerShape(VRadius.Md),
    ) {
        Row(
            Modifier.padding(horizontal = VSpacing.Md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(icon, contentDescription = null, tint = PdigV2Colors.PrimaryBright, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(VSpacing.Sm))
            Text(title, color = PdigV2Colors.TextPrimary, style = VType.Label, maxLines = 1)
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
