package com.pdig.uivnext.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pdig.uivnext.demo.UiVNextDemoFixture
import com.pdig.uivnext.model.RegionPresentation
import com.pdig.uivnext.model.VTestIds
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.theme.VSpacing
import com.pdig.uivnext.theme.VType
import com.pdig.uivnext.ui.VAppState
import com.pdig.uivnext.ui.components.AttentionRow
import com.pdig.uivnext.ui.components.EmptyMotif
import com.pdig.uivnext.ui.components.EmptyState
import com.pdig.uivnext.ui.components.RegionBadge
import com.pdig.uivnext.ui.components.SectionHeader

/**
 * Floating spatial inspector（PHASE 1D §8 + PHASE 1F §5/§39）。
 * 轻量 region rows + subtle separator + 小选中面（不是巨大 bordered card）；
 * 选中地区无已确认资产时显示紧凑 Region Empty（未知 ≠ 没有）。
 */

/** Floating spatial inspector：轻量 region rows + subtle separator + 小选中面。 */
@Composable
internal fun FloatingSpatialInspector(app: VAppState, regions: List<RegionPresentation>) {
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
            // PHASE 1F §39：Region Empty —— 选中地区无已确认资产时如实显示（未知 ≠ 没有）。
            val selectedRegion = regions.firstOrNull { it.regionCode == app.regionFilter }
            val selectedEmpty = app.regionFilter != null &&
                (selectedRegion == null || (selectedRegion.cardCount == 0 && selectedRegion.phoneCount == 0))
            if (selectedEmpty || app.demoEmptyRegion) {
                EmptyState(
                    title = com.pdig.uivnext.copy.Phase1FEmptyCopy.REGION_TITLE,
                    body = com.pdig.uivnext.copy.Phase1FEmptyCopy.REGION_BODY,
                    motif = EmptyMotif.REGION,
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
