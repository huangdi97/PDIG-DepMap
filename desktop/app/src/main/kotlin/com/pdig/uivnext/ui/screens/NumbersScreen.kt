package com.pdig.uivnext.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.dp
import com.pdig.uivnext.demo.UiVNextDemoFixture
import com.pdig.uivnext.model.MediaBreakpoint
import com.pdig.uivnext.model.UiVNextNumber
import com.pdig.uivnext.model.VTestIds
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.theme.VSpacing
import com.pdig.uivnext.theme.VType
import com.pdig.uivnext.ui.VAppState
import com.pdig.uivnext.ui.components.LabelChip
import com.pdig.uivnext.ui.components.NumberFace
import com.pdig.uivnext.ui.components.SectionHeader
import com.pdig.uivnext.ui.components.StatusBadge

/**
 * Numbers（G8）：桌面高密度 List + Inspector（号码信息密度高，不硬套卡片）。
 * Inspector 顶部 = NumberFace（号码身份面：region flag 视觉 + 大号 masked number）。
 * 过滤：SIM/eSIM、主副号、保号（本地状态）。
 */
@Composable
fun NumbersScreen(app: VAppState, breakpoint: MediaBreakpoint) {
    val all = UiVNextDemoFixture.numbers
    val regionFiltered = if (app.regionFilter == null) all else all.filter { it.region == app.regionFilter }
    var selectedId by remember { mutableStateOf(all.firstOrNull()?.id) }
    val selected = regionFiltered.firstOrNull { it.id == selectedId } ?: regionFiltered.firstOrNull()

    Row(Modifier.fillMaxSize().padding(VSpacing.Xxl)) {
        // 列表
        Column(Modifier.weight(0.55f), verticalArrangement = Arrangement.spacedBy(VSpacing.Lg)) {
            PageHeader(
                title = "号码",
                subtitle = if (app.regionFilter == null) "全球 ${regionFiltered.size} 个号码 · 高密度列表 + 检查器" else "地区 ${app.regionFilter} · ${regionFiltered.size} 个号码",
            )
            FilterRowNumbers()
            Spacer(Modifier.height(4.dp))
            DataPanel(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTagLocal(VTestIds.PHONE_LIST),
            ) {
                LazyColumn(Modifier.fillMaxSize().padding(VSpacing.Sm)) {
                    items(regionFiltered, key = { it.id }) { number ->
                        NumberRow(number, selected?.id == number.id, app) {
                            selectedId = number.id
                            app.openNumber(number.id)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.width(VSpacing.Xxl))
        // Inspector（详情摘要；号码身份面优先）
        DataPanel(
            Modifier
                .weight(0.45f)
                .fillMaxSize()
                .testTagLocal(VTestIds.PHONE_INSPECTOR),
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(VSpacing.Xxl),
                verticalArrangement = Arrangement.spacedBy(VSpacing.Lg),
            ) {
                if (selected == null) {
                    Text("选择一个号码查看详情", color = PdigV2Colors.TextMuted, style = VType.Secondary)
                } else {
                    NumberFace(number = selected, privacyMask = app.privacyMask, onClick = {})
                    val services = UiVNextDemoFixture.servicesForNumber(selected.id)
                    SectionHeader("关联服务（${services.size}）")
                    services.forEach { service ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(service.name, color = PdigV2Colors.TextSecondary, style = VType.Secondary)
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
                            Modifier.padding(VSpacing.Md).clickable { app.openNumber(selected.id) },
                            color = PdigV2Colors.PrimaryBright,
                            style = VType.Label,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterRowNumbers() {
    Row(horizontalArrangement = Arrangement.spacedBy(VSpacing.Sm)) {
        listOf("全部", "eSIM", "实体 SIM", "主号", "副号", "保号").forEach { label ->
            Surface(
                color = PdigV2Colors.SurfaceRaised,
                shape = RoundedCornerShape(VRadius.Sm),
                border = androidx.compose.foundation.BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
            ) {
                Text(label, Modifier.padding(horizontal = VSpacing.Md, vertical = 5.dp), color = PdigV2Colors.TextSecondary, style = VType.Label)
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
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) PdigV2Colors.PrimaryBright else PdigV2Colors.BorderSubtle),
    ) {
        Row(Modifier.padding(horizontal = VSpacing.Lg, vertical = VSpacing.Md), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(VSpacing.Sm)) {
                    Text(number.nickname, color = PdigV2Colors.TextPrimary, style = VType.Label)
                    Text("${number.countryCode} · ${number.region}", color = PdigV2Colors.TextMuted, style = VType.Meta)
                }
                Text(
                    "${number.maskedNumber} · ${number.carrier} · ${if (number.simKind == "eSIM") "eSIM" else "SIM"} · ${if (number.role == "primary") "主号" else "副号"}",
                    color = PdigV2Colors.TextSecondary,
                    style = VType.Secondary,
                )
            }
            if (number.recoveryOnly) LabelChip("恢复唯一", highlight = true)
            Spacer(Modifier.width(VSpacing.Sm))
            StatusBadge(number.status)
        }
    }
}
