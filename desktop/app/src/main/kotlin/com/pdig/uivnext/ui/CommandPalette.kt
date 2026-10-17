package com.pdig.uivnext.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.demo.UiVNextDemoFixture
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.theme.VSpacing
import com.pdig.uivnext.theme.VType

/**
 * Command Palette（PHASE 1E §39）—— 真实可用、键盘优先。
 *
 * 至少支持：搜索 card / 搜索 number / 打开基础设施 / 开始更换手机号 / 打开个性化。
 * 过滤 + 方向键选择 + Enter 执行 + Escape 关闭；全部为 Presentation 层导航，
 * 不触碰 domain / canonical 数据。
 */

internal data class PaletteEntry(
    val id: String,
    val label: String,
    val hint: String,
    val icon: ImageVector,
    val run: (VAppState) -> Unit,
)

/** 命令集合（PHASE 1E §39 —— 5 个必须项 + 卡片/号码直达）。 */
internal fun paletteEntries(): List<PaletteEntry> {
    return listOf(
        PaletteEntry("open-cards", "搜索卡片", "打开卡片列表", Icons.Filled.CreditCard) {
            it.regionFilter = null
            it.navigate(VScreen.CARDS)
        },
        PaletteEntry("open-numbers", "搜索号码", "打开号码列表", Icons.Filled.Dialpad) {
            it.regionFilter = null
            it.navigate(VScreen.NUMBERS)
        },
        PaletteEntry("open-infrastructure", "打开基础设施", "全球总览视图", Icons.Filled.Public) {
            it.regionFilter = null
            it.navigate(VScreen.OVERVIEW)
        },
        PaletteEntry("start-change-phone", "开始更换手机号", "连续性迁移向导", Icons.Filled.SyncAlt) {
            it.navigate(VScreen.CHANGE_PHONE)
        },
        PaletteEntry("open-personalization", "打开个性化", "外观与呈现设置", Icons.Filled.Edit) {
            it.navigate(VScreen.PERSONALIZATION)
        },
        PaletteEntry("open-now", "回到「现在」", "顶部核心视图", Icons.Filled.Home) {
            it.navigate(VScreen.NOW)
        },
    ) + cardEntries() + numberEntries()
}

private fun cardEntries(): List<PaletteEntry> = UiVNextDemoFixture.cards.map { card ->
    PaletteEntry("card:${card.id}", card.nickname, "${card.issuer} · ${card.masked}", Icons.Filled.CreditCard) {
        it.openCard(card.id)
    }
}

private fun numberEntries(): List<PaletteEntry> = UiVNextDemoFixture.numbers.map { number ->
    PaletteEntry("number:${number.id}", number.nickname, number.maskedNumber, Icons.Filled.Dialpad) {
        it.openNumber(number.id)
    }
}

/** 过滤逻辑（query 归一化；label/hint/id 三家匹配）。 */
internal fun filterPalette(entries: List<PaletteEntry>, query: String): List<PaletteEntry> {
    val q = query.trim()
    if (q.isEmpty()) return entries
    val lower = q.lowercase()
    return entries.filter { e ->
        e.label.lowercase().contains(lower) ||
            e.hint.lowercase().contains(lower) ||
            e.id.lowercase().contains(lower)
    }
}

/** 顶部 chrome 搜索框（点击 → 打开 palette）。 */
@Composable
internal fun PaletteTrigger(app: VAppState, modifier: Modifier = Modifier, compact: Boolean = false) {
    Surface(
        modifier = modifier.clickable { app.paletteOpen = true },
        color = PdigV2Colors.SurfaceRaised.copy(alpha = 0.9f),
        shape = RoundedCornerShape(VRadius.Sm),
        border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        Row(
            Modifier.padding(horizontal = if (compact) VSpacing.Md else VSpacing.Lg, vertical = if (compact) 5.dp else 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = PdigV2Colors.TextMuted, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(VSpacing.Sm))
            Text(if (compact) "搜索" else "搜索 / 命令", color = PdigV2Colors.TextMuted, fontSize = 12.sp)
            Spacer(Modifier.width(VSpacing.Lg))
            Surface(color = PdigV2Colors.PrimarySoft, shape = RoundedCornerShape(4.dp)) {
                Text(
                    "Ctrl K",
                    Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    color = PdigV2Colors.TextSecondary,
                    fontSize = 10.sp,
                )
            }
        }
    }
}

/** 命令面板 overlay（PHASE 1E §39）：键盘优先 + 可点击。 */
@Composable
fun CommandPalette(app: VAppState) {
    val entries = filterPalette(paletteEntries(), app.paletteQuery)
    if (entries.isNotEmpty() && app.paletteSelectedIndex >= entries.size) {
        app.paletteSelectedIndex = entries.size - 1
    }
    if (entries.isEmpty()) {
        app.paletteSelectedIndex = 0
    }
    Surface(
        modifier = Modifier.fillMaxWidth(0.56f),
        color = PdigV2Colors.Surface.copy(alpha = 0.985f),
        shape = RoundedCornerShape(VRadius.Xl),
        border = BorderStroke(1.dp, PdigV2Colors.BorderStrong),
    ) {
        Column(Modifier.padding(VSpacing.Lg), verticalArrangement = Arrangement.spacedBy(VSpacing.Sm)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Search, contentDescription = null, tint = PdigV2Colors.TextMuted, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(VSpacing.Sm))
                Text(
                    "搜索 / 命令",
                    style = VType.SectionTitle,
                    color = PdigV2Colors.TextPrimary,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "↑↓ 选择 · Enter 执行 · Esc 关闭",
                    style = VType.Meta,
                    color = PdigV2Colors.TextMuted,
                )
            }
            Text(
                "在此输入来搜索卡片、号码或执行操作",
                style = VType.Secondary,
                color = PdigV2Colors.TextSecondary,
            )
            Spacer(Modifier.height(2.dp))
            LazyColumn(Modifier.fillMaxWidth().height(360.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                itemsIndexed(entries) { index, entry ->
                    PaletteRow(
                        entry = entry,
                        selected = index == app.paletteSelectedIndex,
                        onClick = {
                            app.paletteSelectedIndex = index
                            entry.run(app)
                            app.paletteOpen = false
                        },
                    )
                }
            }
            Text(
                "共 ${entries.size} 项",
                style = VType.Meta,
                color = PdigV2Colors.TextMuted,
            )
        }
    }
}

@Composable
private fun PaletteRow(entry: PaletteEntry, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(VRadius.Md),
        color = if (selected) PdigV2Colors.Primary.copy(alpha = 0.26f) else Color.Transparent,
        border = if (selected) BorderStroke(1.dp, PdigV2Colors.PrimaryBright.copy(alpha = 0.5f)) else null,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = VSpacing.Md, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                entry.icon,
                contentDescription = null,
                tint = if (selected) PdigV2Colors.PrimaryBright else PdigV2Colors.TextSecondary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(VSpacing.Md))
            Text(
                entry.label,
                style = VType.Body,
                fontWeight = FontWeight.Medium,
                color = if (selected) PdigV2Colors.PrimaryBright else PdigV2Colors.TextPrimary,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(VSpacing.Md))
            Text(
                entry.hint,
                style = VType.Meta,
                color = PdigV2Colors.TextMuted,
                maxLines = 1,
            )
        }
    }
}