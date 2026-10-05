package com.pdig.uivnext.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.demo.UiVNextDemoFixture
import com.pdig.uivnext.demo.demoCards
import com.pdig.uivnext.model.MediaBreakpoint
import com.pdig.uivnext.model.UiVNextCard
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.model.VTestIds
import com.pdig.uivnext.model.hexColorOrNull
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.theme.VTouchTarget
import com.pdig.uivnext.ui.VAppState
import com.pdig.uivnext.ui.components.AssetCard
import com.pdig.uivnext.ui.components.CardIdentityThumbnail
import com.pdig.uivnext.ui.components.EmptyKind
import com.pdig.uivnext.ui.components.EmptyState

/**
 * Cards：过滤（全部/国家）+ Visual Grid / Compact List 切换。
 * 空态：过滤后无卡片 → honest unknown EmptyState（未记录 ≠ 无风险）。
 */
@Composable
fun CardsScreen(app: VAppState, breakpoint: MediaBreakpoint) {
    val all = app.demoCards()
    val regionFiltered = if (app.regionFilter == null) all else all.filter { it.region == app.regionFilter }
    var kindFilter by remember { mutableStateOf("all") }
    val filteredCards = regionFiltered.filter { matchesCardKind(it, kindFilter) }
    // Human-selected Android reference: phone defaults to a high-density visual list; wide layouts
    // default to the card gallery. The user can still switch either presentation.
    var gridView by remember(breakpoint) { mutableStateOf(breakpoint != MediaBreakpoint.COMPACT) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(pagePadding(breakpoint)),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("卡片", color = PdigV2Colors.TextPrimary, fontSize = pageTitleSize(breakpoint), fontWeight = FontWeight.Bold)
                Text(
                    when {
                        kindFilter != "all" -> {
                            val scope = if (app.regionFilter == null) "全球" else regionLabel(app.regionFilter!!)
                            "$scope · 当前显示 ${filteredCards.size} / ${regionFiltered.size} 张卡"
                        }
                        app.regionFilter == null -> "全球 ${regionFiltered.size} 张卡"
                        else -> "${regionLabel(app.regionFilter!!)} · ${regionFiltered.size} 张卡"
                    },
                    color = PdigV2Colors.TextSecondary,
                    fontSize = 13.sp,
                )
            }
            ViewToggle(gridView, onToggle = { gridView = !gridView })
        }

        Spacer(Modifier.height(16.dp))
        CardKindFilterRow(kindFilter) { kindFilter = it }
        Spacer(Modifier.height(10.dp))
        FilterRow(
            regions = UiVNextDemoFixture.regions.map { it.regionCode to it.displayName },
            activeRegion = app.regionFilter,
            onRegion = { app.regionFilter = it },
        )

        Spacer(Modifier.height(18.dp))

        if (filteredCards.isEmpty()) {
            EmptyState(
                kind = EmptyKind.CARDS,
                title = if (regionFiltered.isEmpty()) {
                    if (app.regionFilter == null) "还没有卡片" else "该地区没有卡片"
                } else {
                    "当前筛选没有卡片"
                },
                description = if (regionFiltered.isEmpty()) {
                    if (app.regionFilter == null) {
                        "没有记录 ≠ 没有风险：尚未录入卡片时，不推断任何支付路径存在或不存在。"
                    } else {
                        "${regionLabel(app.regionFilter!!)} 暂无卡片记录。没有记录 ≠ 没有风险。"
                    }
                } else {
                    "换一个卡片类型继续查看；未出现在当前筛选中不代表没有支付依赖。"
                },
                primaryCta = "查看号码",
                onPrimary = { app.navigate(VScreen.NUMBERS) },
                secondaryCta = "查看基础设施",
                onSecondary = { app.navigate(VScreen.OVERVIEW) },
            )
        } else if (gridView) {
            // B1：COMPACT 禁止再强制 2 列（两列卡宽下 nickname/issuer/form/metadata 互相挤压并竖排）。
            // 列策略：COMPACT=1 列整卡 / MEDIUM=2 列 / EXPANDED=4 列（brief §4；最终以真实设备视觉为准）。
            val columns = when (breakpoint) {
                MediaBreakpoint.EXPANDED -> 4
                MediaBreakpoint.MEDIUM -> 2
                MediaBreakpoint.COMPACT -> 1
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier
                    .fillMaxSize()
                    .testTagLocal(VTestIds.CARD_GRID),
                horizontalArrangement = Arrangement.spacedBy(if (breakpoint == MediaBreakpoint.COMPACT) 12.dp else 20.dp),
                verticalArrangement = Arrangement.spacedBy(if (breakpoint == MediaBreakpoint.COMPACT) 12.dp else 20.dp),
            ) {
                items(filteredCards, key = { it.id }) { card ->
                    val profile = app.savedPresentationProfile("card", card.id)
                    AssetCard(
                        card = card.copy(preset = profile?.themeId ?: card.preset),
                        privacyMask = app.privacyMask || (profile?.maskSensitive == true),
                        onClick = { app.openCard(card.id) },
                        presentationMaterial = profile?.material,
                        presentationAccent = hexColorOrNull(profile?.accentColor ?: "default"),
                        presentationLayout = profile?.layout,
                    )
                }
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .testTagLocal(VTestIds.CARD_LIST)
                    .verticalScroll(rememberScrollState()),
            ) {
                filteredCards.forEach { card -> CompactCardRow(card, app) }
            }
        }
    }
}

/** Compact List 行（信息密度优先）。 */
@Composable
private fun CompactCardRow(card: UiVNextCard, app: VAppState) {
    val profile = app.savedPresentationProfile("card", card.id)
    val maskSensitive = app.privacyMask || (profile?.maskSensitive == true)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .defaultMinSize(minHeight = 84.dp)
            .clickable { app.openCard(card.id) },
        color = PdigV2Colors.Surface,
        shape = RoundedCornerShape(VRadius.Lg),
        border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
        tonalElevation = 1.dp,
        shadowElevation = 1.dp,
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CardIdentityThumbnail(
                card = card.copy(preset = profile?.themeId ?: card.preset),
                privacyMask = maskSensitive,
                modifier = Modifier.width(96.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(card.nickname, color = PdigV2Colors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(
                    "${card.issuer} · ${regionLabel(card.region)} · ${card.currency}",
                    color = PdigV2Colors.TextSecondary,
                    fontSize = 12.sp,
                )
                Text(
                    "${if (card.form == "virtual") "虚拟卡" else "实体卡"} · 到期 ${card.expiry}",
                    color = PdigV2Colors.TextMuted,
                    fontSize = 11.sp,
                )
            }
            Spacer(Modifier.width(8.dp))
            com.pdig.uivnext.ui.components.StatusBadge(card.status)
        }
    }
}

@Composable
private fun CardKindFilterRow(active: String, onFilter: (String) -> Unit) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(
            "all" to "全部",
            "credit" to "信用卡",
            "debit" to "储蓄卡",
            "virtual" to "虚拟卡",
        ).forEach { (key, label) ->
            FilterChip(label, active == key) { onFilter(key) }
        }
    }
}

private fun matchesCardKind(card: UiVNextCard, filter: String): Boolean = when (filter) {
    "credit" -> card.type == "credit"
    "debit" -> card.type == "debit"
    "virtual" -> card.form == "virtual"
    else -> true
}

@Composable
private fun FilterRow(regions: List<Pair<String, String>>, activeRegion: String?, onRegion: (String?) -> Unit) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip("全部", activeRegion == null) { onRegion(null) }
        regions.forEach { (code, name) ->
            FilterChip(name, activeRegion == code) { onRegion(code) }
        }
    }
}

@Composable
internal fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.defaultMinSize(minHeight = VTouchTarget.Min).clickable(onClick = onClick),
        color = if (selected) PdigV2Colors.Primary.copy(alpha = 0.28f) else PdigV2Colors.SurfaceRaised,
        shape = RoundedCornerShape(VRadius.Sm),
        border = BorderStroke(1.dp, if (selected) PdigV2Colors.PrimaryBright else PdigV2Colors.BorderSubtle),
    ) {
        Text(
            label,
            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = if (selected) PdigV2Colors.PrimaryBright else PdigV2Colors.TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun ViewToggle(grid: Boolean, onToggle: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            modifier = Modifier.defaultMinSize(minHeight = VTouchTarget.Min).clickable(onClick = onToggle).testTagLocal(VTestIds.CARD_VIEW_TOGGLE),
            color = PdigV2Colors.SurfaceRaised,
            shape = RoundedCornerShape(VRadius.Sm),
        ) {
            Text(
                if (grid) "列表" else "卡面",
                Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                color = PdigV2Colors.TextSecondary,
                fontSize = 12.sp,
            )
        }
    }
}
