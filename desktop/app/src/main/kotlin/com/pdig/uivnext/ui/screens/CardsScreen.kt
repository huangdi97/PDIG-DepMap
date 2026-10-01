package com.pdig.uivnext.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pdig.uivnext.demo.UiVNextDemoFixture
import com.pdig.uivnext.model.MediaBreakpoint
import com.pdig.uivnext.model.UiVNextCard
import com.pdig.uivnext.model.VTestIds
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.theme.VSpacing
import com.pdig.uivnext.theme.VType
import com.pdig.uivnext.ui.VAppState
import com.pdig.uivnext.ui.components.AssetCard
import com.pdig.uivnext.ui.components.EmptyState
import com.pdig.uivnext.ui.components.SectionHeader
import com.pdig.uivnext.ui.components.StatusBadge

/**
 * Cards（G6）：真实支付卡资产身份网格。
 * 空状态（§45）与 Visual Grid / Compact List 切换；过滤（全部/国家/实体虚拟/储蓄信用/币种/状态）。
 */
@Composable
fun CardsScreen(app: VAppState, breakpoint: MediaBreakpoint) {
    val all = UiVNextDemoFixture.cards
    val regionFiltered = if (app.regionFilter == null) all else all.filter { it.region == app.regionFilter }
    var gridView by remember { mutableStateOf(true) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(VSpacing.Xxl),
    ) {
        PageHeader(
            title = "卡片",
            subtitle = if (app.regionFilter == null) "全球 ${regionFiltered.size} 张卡 · 每张卡都是可独立的资产身份" else "地区 ${app.regionFilter} · ${regionFiltered.size} 张卡",
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(VSpacing.Md)) {
                    FilterChip("全部", app.regionFilter == null) { app.regionFilter = null }
                    ViewToggle(gridView, onToggle = { gridView = !gridView })
                }
            },
        )

        Spacer(Modifier.height(VSpacing.Xxl))
        FilterRow(
            regions = UiVNextDemoFixture.regions.map { it.regionCode },
            activeRegion = app.regionFilter,
            onRegion = { app.regionFilter = it },
        )

        Spacer(Modifier.height(VSpacing.Xxl))

        if (regionFiltered.isEmpty() || app.demoEmptyCards) {
            EmptyState(
                title = "还没有记录卡片",
                body = "添加卡片后，PDIG 可以帮助你了解：它在哪里使用、什么时候到期，以及换卡前会影响什么。",
                actionLabel = "添加卡片",
            )
            return@Column
        }

        if (gridView) {
            val columns = when (breakpoint) {
                MediaBreakpoint.WIDE -> 3
                MediaBreakpoint.MEDIUM -> 2
                MediaBreakpoint.COMPACT -> 2
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier
                    .fillMaxSize()
                    .testTagLocal(VTestIds.CARD_GRID),
                horizontalArrangement = Arrangement.spacedBy(VSpacing.GridGapWide),
                verticalArrangement = Arrangement.spacedBy(VSpacing.GridGapWide),
            ) {
                items(regionFiltered) { card ->
                    AssetCard(card = card, privacyMask = app.privacyMask, onClick = { app.openCard(card.id) })
                }
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .testTagLocal(VTestIds.CARD_LIST)
                    .verticalScroll(rememberScrollState()),
            ) {
                regionFiltered.forEach { card -> CompactCardRow(card, app) }
            }
        }
    }
}

/** Compact List 行（信息密度优先）。 */
@Composable
private fun CompactCardRow(card: UiVNextCard, app: VAppState) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { app.openCard(card.id) },
        color = PdigV2Colors.Surface.copy(alpha = 0.96f),
        shape = RoundedCornerShape(VRadius.Md),
        border = androidx.compose.foundation.BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        Row(
            Modifier.padding(horizontal = VSpacing.Lg, vertical = VSpacing.Md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .width(4.dp)
                    .height(28.dp)
                    .background(PdigV2Colors.PrimaryBright, RoundedCornerShape(2.dp)),
            )
            Spacer(Modifier.width(VSpacing.Md))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(card.nickname, color = PdigV2Colors.TextPrimary, style = VType.Label)
                    Text(card.masked, color = PdigV2Colors.TextMuted, style = VType.Meta)
                }
                Text(
                    "${card.issuer} · ${card.region} · ${card.currency} · ${if (card.form == "virtual") "虚拟" else "实体"} · 到期 ${card.expiry}",
                    color = PdigV2Colors.TextSecondary,
                    style = VType.Secondary,
                )
            }
            StatusBadge(card.status)
        }
    }
}

@Composable
private fun FilterRow(regions: List<String>, activeRegion: String?, onRegion: (String?) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(VSpacing.Sm)) {
        FilterChip("全部", activeRegion == null) { onRegion(null) }
        regions.forEach { code ->
            FilterChip(code, activeRegion == code) { onRegion(code) }
        }
    }
}

@Composable
internal fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        color = if (selected) PdigV2Colors.Primary.copy(alpha = 0.28f) else PdigV2Colors.SurfaceRaised,
        shape = RoundedCornerShape(VRadius.Sm),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) PdigV2Colors.PrimaryBright else PdigV2Colors.BorderSubtle),
    ) {
        Text(
            label,
            Modifier.padding(horizontal = VSpacing.Md, vertical = 6.dp),
            color = if (selected) PdigV2Colors.PrimaryBright else PdigV2Colors.TextSecondary,
            style = VType.Label,
        )
    }
}

@Composable
private fun ViewToggle(grid: Boolean, onToggle: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onToggle).testTagLocal(VTestIds.CARD_VIEW_TOGGLE),
        color = PdigV2Colors.SurfaceRaised,
        shape = RoundedCornerShape(VRadius.Sm),
        border = androidx.compose.foundation.BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        Text(
            if (grid) "切换：紧凑列表" else "切换：视觉网格",
            Modifier.padding(horizontal = VSpacing.Md, vertical = 6.dp),
            color = PdigV2Colors.TextSecondary,
            style = VType.Label,
        )
    }
}