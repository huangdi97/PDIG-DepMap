package com.pdig.uivnext.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.demo.UiVNextDemoFixture
import com.pdig.uivnext.demo.demoNumbers
import com.pdig.uivnext.model.MediaBreakpoint
import com.pdig.uivnext.model.UiVNextNumber
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.model.VTestIds
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.ui.VAppState
import com.pdig.uivnext.ui.components.EmptyKind
import com.pdig.uivnext.ui.components.EmptyState
import com.pdig.uivnext.ui.components.LabelChip
import com.pdig.uivnext.ui.components.SectionHeader
import com.pdig.uivnext.ui.components.StatusBadge

/**
 * Numbers：号码管理（信息密度高，不硬套卡片；communication identity 视觉语言）。
 * 大屏：List + Inspector 并排；手机：List 上 / Inspector 下（LazyColumn 保证滚动）。
 * 空态：无号码 → honest unknown EmptyState（未记录 ≠ 无风险）。
 */
@Composable
fun NumbersScreen(app: VAppState, breakpoint: MediaBreakpoint) {
    val all = app.demoNumbers()
    val filtered = if (app.regionFilter == null) all else all.filter { it.region == app.regionFilter }
    var selectedId by remember { mutableStateOf(all.firstOrNull()?.id) }
    val selected = filtered.firstOrNull { it.id == selectedId } ?: filtered.firstOrNull()

    if (breakpoint == MediaBreakpoint.EXPANDED || breakpoint == MediaBreakpoint.MEDIUM) {
        Row(Modifier.fillMaxSize().padding(24.dp)) {
            Column(Modifier.weight(0.55f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                NumbersHeader(app, filtered.size)
                Spacer(Modifier.height(8.dp))
                FilterRowNumbers()
                Spacer(Modifier.height(8.dp))
                if (filtered.isEmpty()) {
                    EmptyState(
                        kind = EmptyKind.NUMBERS,
                        title = "还没有号码",
                        description = "没有记录 ≠ 没有风险：尚未录入号码时，不推断登录 / 恢复路径存在或不存在。",
                        primaryCta = "查看卡片",
                        onPrimary = { app.navigate(VScreen.CARDS) },
                    )
                } else {
                    NumberListSurface(filtered, selected?.id, app)
                }
            }
            Spacer(Modifier.width(24.dp))
            Surface(
                modifier = Modifier
                    .weight(0.45f)
                    .fillMaxSize()
                    .testTagLocal(VTestIds.PHONE_INSPECTOR),
                color = PdigV2Colors.Surface.copy(alpha = 0.92f),
                shape = RoundedCornerShape(VRadius.Xl),
                border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
            ) {
                InspectorContent(app, selected)
            }
        }
    } else {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            NumbersHeader(app, filtered.size)
            FilterRowNumbers()
            if (filtered.isEmpty()) {
                EmptyState(
                    kind = EmptyKind.NUMBERS,
                    title = "还没有号码",
                    description = "没有记录 ≠ 没有风险：尚未录入号码时，不推断登录 / 恢复路径存在或不存在。",
                    primaryCta = "查看卡片",
                    onPrimary = { app.navigate(VScreen.CARDS) },
                )
            } else {
                NumberListSurface(filtered, selected?.id, app)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTagLocal(VTestIds.PHONE_INSPECTOR),
                    color = PdigV2Colors.Surface.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(VRadius.Xl),
                    border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
                ) {
                    InspectorContent(app, selected)
                }
            }
        }
    }
}

@Composable
private fun NumbersHeader(app: VAppState, count: Int) {
    Text("号码", color = PdigV2Colors.TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
    Text(
        if (app.regionFilter == null) "全球 $count 个号码" else "地区 ${app.regionFilter} · $count 个号码",
        color = PdigV2Colors.TextSecondary,
        fontSize = 13.sp,
    )
}

@Composable
private fun ColumnScope.NumberListSurface(filtered: List<UiVNextNumber>, selectedId: String?, app: VAppState) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .testTagLocal(VTestIds.PHONE_LIST),
        color = PdigV2Colors.Surface.copy(alpha = 0.92f),
        shape = RoundedCornerShape(VRadius.Xl),
        border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        LazyColumn(Modifier.fillMaxSize().padding(8.dp)) {
            items(filtered, key = { it.id }) { number ->
                NumberRow(number, selectedId == number.id, app) {
                    app.openNumber(number.id)
                }
            }
        }
    }
}

@Composable
private fun InspectorContent(app: VAppState, selected: UiVNextNumber?) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (selected == null) {
            Text("选择一个号码查看详情", color = PdigV2Colors.TextMuted, fontSize = 13.sp)
        } else {
            SectionHeader("号码详情")
            Text(selected.nickname, color = PdigV2Colors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                selected.maskedNumber,
                color = PdigV2Colors.TextPrimary,
                fontSize = 20.sp,
                fontFamily = FontFamily.Monospace,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LabelChip(if (selected.simKind == "eSIM") "eSIM" else "实体 SIM")
                LabelChip(if (selected.role == "primary") "主号" else "副号")
                LabelChip("${selected.carrier}")
                if (selected.recoveryOnly) LabelChip("唯一恢复路径", highlight = true)
            }
            Text("用途：${selected.usages.joinToString(" · ")}", color = PdigV2Colors.TextSecondary, fontSize = 13.sp)
            val services = UiVNextDemoFixture.servicesForNumber(selected.id)
            SectionHeader("关联服务（${services.size}）")
            services.forEach { service ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(service.name, color = PdigV2Colors.TextSecondary, fontSize = 13.sp)
                    LabelChip(if (service.kind == "twoFA") "2FA" else "验证方式")
                }
            }
            Surface(
                Modifier.fillMaxWidth(),
                color = PdigV2Colors.PrimarySoft,
                shape = RoundedCornerShape(VRadius.Md),
            ) {
                Text(
                    "查看完整详情 →",
                    Modifier.padding(12.dp).clickable { app.openNumber(selected.id) },
                    color = PdigV2Colors.PrimaryBright,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun FilterRowNumbers() {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("全部", "eSIM", "实体 SIM", "主号", "副号", "保号").forEach { label ->
            Surface(
                color = PdigV2Colors.SurfaceRaised,
                shape = RoundedCornerShape(VRadius.Sm),
                border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
            ) {
                Text(label, Modifier.padding(horizontal = 10.dp, vertical = 5.dp), color = PdigV2Colors.TextSecondary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun NumberRow(number: UiVNextNumber, selected: Boolean, app: VAppState, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clickable(onClick = onClick),
        color = if (selected) PdigV2Colors.Primary.copy(alpha = 0.18f) else PdigV2Colors.SurfaceRaised.copy(alpha = 0.6f),
        shape = RoundedCornerShape(VRadius.Md),
        border = BorderStroke(1.dp, if (selected) PdigV2Colors.PrimaryBright else PdigV2Colors.BorderSubtle),
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(number.nickname, color = PdigV2Colors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("${number.countryCode}", color = PdigV2Colors.TextMuted, fontSize = 11.sp)
                }
                Text(
                    "${number.maskedNumber} · ${number.carrier} · ${if (number.simKind == "eSIM") "eSIM" else "SIM"} · ${if (number.role == "primary") "主号" else "副号"}",
                    color = PdigV2Colors.TextSecondary,
                    fontSize = 12.sp,
                )
            }
            if (number.recoveryOnly) LabelChip("恢复唯一", highlight = true)
            Spacer(Modifier.width(8.dp))
            StatusBadge(number.status)
        }
    }
}
