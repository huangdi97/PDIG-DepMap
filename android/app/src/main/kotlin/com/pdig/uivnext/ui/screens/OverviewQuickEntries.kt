package com.pdig.uivnext.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.demo.UiVNextDemoFixture
import com.pdig.uivnext.demo.demoCards
import com.pdig.uivnext.demo.demoNumbers
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.model.VTestIds
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.ui.INFRA_ENTRIES
import com.pdig.uivnext.ui.VAppState

@Composable
internal fun CompactInfrastructureHub(app: VAppState) {
    // Reference: eight icon-forward categories in a compact 4x2 field, not eight admin cards.
    // Data hints are read from the existing scoped fixture; no new object is manufactured.
    Surface(
        modifier = Modifier.fillMaxWidth().testTagLocal("pdig.overview.infrastructure-hub"),
        color = PdigV2Colors.Surface,
        shape = RoundedCornerShape(21.dp),
        border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        Column(
            Modifier.padding(horizontal = 10.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("基础设施", color = PdigV2Colors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("8 类资产", color = PdigV2Colors.PrimaryText, fontSize = 11.sp)
            }
            INFRA_ENTRIES.chunked(4).forEach { rowEntries ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    rowEntries.forEach { entry ->
                        val selected = entry.screen == VScreen.OVERVIEW
                        val accent = when (entry.screen) {
                            VScreen.CARDS -> androidx.compose.ui.graphics.Color(0xFFFF864A)
                            VScreen.NUMBERS -> androidx.compose.ui.graphics.Color(0xFF13AF88)
                            VScreen.ACCOUNTS -> androidx.compose.ui.graphics.Color(0xFF3986EE)
                            VScreen.EMAILS -> androidx.compose.ui.graphics.Color(0xFF926DF0)
                            VScreen.DEVICES -> androidx.compose.ui.graphics.Color(0xFF3975D4)
                            VScreen.SERVICES -> androidx.compose.ui.graphics.Color(0xFFFFA343)
                            VScreen.WEAKNESSES -> androidx.compose.ui.graphics.Color(0xFFE36B63)
                            else -> PdigV2Colors.PrimaryBright
                        }
                        Column(
                            modifier = Modifier.weight(1f)
                                .defaultMinSize(minHeight = 72.dp)
                                .clickableLocal { app.navigate(entry.screen) }
                                .padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Surface(
                                modifier = Modifier.size(35.dp),
                                color = if (selected) accent else accent.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
                            ) {
                                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                    Icon(entry.icon, contentDescription = null, tint = if (selected) androidx.compose.ui.graphics.Color.White else accent, modifier = Modifier.size(19.dp))
                                }
                            }
                            Text(entry.screen.titleZh, color = PdigV2Colors.TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            Text(compactInfraHint(entry.screen, app), color = PdigV2Colors.TextMuted, fontSize = 9.sp, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

private fun compactInfraHint(screen: VScreen, app: VAppState): String {
    val zeroed = app.emptyDemo
    val region = app.regionFilter
    fun matchesRegion(code: String): Boolean = region == null || code == region
    return when (screen) {
        VScreen.OVERVIEW -> if (region == null) "全球" else regionLabel(region)
        VScreen.CARDS -> "${if (zeroed) 0 else app.demoCards().count { matchesRegion(it.region) }} 张"
        VScreen.NUMBERS -> "${if (zeroed) 0 else app.demoNumbers().count { matchesRegion(it.region) }} 个"
        VScreen.ACCOUNTS -> "${if (zeroed) 0 else UiVNextDemoFixture.accounts.count { matchesRegion(it.region) }} 个"
        VScreen.EMAILS -> "${if (zeroed) 0 else UiVNextDemoFixture.emails.count { matchesRegion(it.region) }} 个"
        VScreen.DEVICES -> "${if (zeroed) 0 else UiVNextDemoFixture.devices.count { matchesRegion(it.region) }} 台"
        VScreen.SERVICES -> "${if (zeroed) 0 else UiVNextDemoFixture.services.count { matchesRegion(it.region) }} 项"
        VScreen.WEAKNESSES -> if (zeroed) "未知" else "查看风险"
        else -> ""
    }
}

@Composable
internal fun CompactQuickEntries(app: VAppState) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CompactQuickEntry("查看卡片", "全球 ${app.demoCards().size} 张卡") { app.navigate(VScreen.CARDS) }
            CompactQuickEntry("查看号码", "全球 ${app.demoNumbers().size} 个号码") { app.navigate(VScreen.NUMBERS) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CompactQuickEntry("更换手机号", "规划与迁移") { app.navigate(VScreen.CHANGE_PHONE) }
            CompactQuickEntry("薄弱点", "需要优先处理的风险") { app.navigate(VScreen.WEAKNESSES) }
        }
    }
}

@Composable
private fun RowScope.CompactQuickEntry(title: String, hint: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .weight(1f)
            .clickableLocal(onClick = onClick),
        color = PdigV2Colors.SurfaceRaised,
        shape = RoundedCornerShape(VRadius.Md),
        border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = PdigV2Colors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(hint, color = PdigV2Colors.TextMuted, fontSize = 11.sp)
        }
    }
}

@Composable
internal fun QuickEntryRow(app: VAppState) {
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
            QuickEntry("更换手机号", "规划与迁移") { app.navigate(VScreen.CHANGE_PHONE) }
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

