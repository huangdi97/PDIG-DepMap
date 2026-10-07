package com.pdig.uivnext.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.pdig.uivnext.theme.VTouchTarget
import com.pdig.uivnext.ui.VAppState
import com.pdig.uivnext.ui.components.EmptyKind
import com.pdig.uivnext.ui.components.EmptyState
import com.pdig.uivnext.ui.components.LabelChip
import com.pdig.uivnext.ui.components.NumberFace
import com.pdig.uivnext.ui.components.NumberIdentityThumbnail
import com.pdig.uivnext.ui.components.SectionHeader
import com.pdig.uivnext.ui.components.StatusBadge

/** 号码：通信身份列表；手机 List→Detail，大屏 List + Inspector。 */
@Composable
fun NumbersScreen(app: VAppState, breakpoint: MediaBreakpoint) {
    val all = app.demoNumbers()
    val regionScoped = if (app.regionFilter == null) all else all.filter { it.region == app.regionFilter }
    var filter by remember { mutableStateOf("all") }
    val filtered = regionScoped.filter { matchesNumberFilter(it, filter) }
    var selectedId by remember { mutableStateOf(all.firstOrNull()?.id) }
    val selected = filtered.firstOrNull { it.id == selectedId } ?: filtered.firstOrNull()

    if (breakpoint == MediaBreakpoint.EXPANDED) {
        Row(Modifier.fillMaxSize().padding(24.dp)) {
            Column(Modifier.weight(0.55f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                NumbersHeader(app, filtered.size, regionScoped.size, filter, breakpoint)
                Spacer(Modifier.height(8.dp))
                FilterRowNumbers(filter) { filter = it }
                Spacer(Modifier.height(8.dp))
                if (filtered.isEmpty()) {
                    NumberFilterEmpty(app, filter)
                } else {
                    NumberListSurface(filtered, selected?.id, app) { selectedId = it.id }
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
        Column(
            Modifier
                .fillMaxSize()
                .padding(pagePadding(breakpoint)),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NumbersHeader(app, filtered.size, regionScoped.size, filter, breakpoint)
            FilterRowNumbers(filter) { filter = it }
            if (filtered.isEmpty()) {
                NumberFilterEmpty(app, filter)
            } else {
                NumberListSurface(filtered, null, app) { app.openNumber(it.id) }
            }
        }
    }
}

@Composable
private fun NumbersHeader(
    app: VAppState,
    count: Int,
    total: Int,
    filter: String,
    breakpoint: MediaBreakpoint,
) {
    if (breakpoint != MediaBreakpoint.COMPACT) {
        Text("号码", color = PdigV2Colors.TextPrimary, fontSize = pageTitleSize(breakpoint), fontWeight = FontWeight.Bold)
    }
    val scope = if (app.regionFilter == null) "全球" else regionLabel(app.regionFilter!!)
    Text(
        if (filter == "all") "$scope $total 个号码" else "$scope · 当前显示 $count / $total",
        color = PdigV2Colors.TextSecondary,
        fontSize = 13.sp,
    )
    if (app.regionFilter != null) RegionScopeBanner(app)
}

@Composable
private fun NumberFilterEmpty(app: VAppState, filter: String) {
    EmptyState(
        kind = EmptyKind.NUMBERS,
        title = if (filter == "all") "还没有号码" else "当前筛选没有号码",
        description = if (filter == "all") {
            "没有记录 ≠ 没有风险：尚未记录号码时，不推断登录或恢复路径存在或不存在。"
        } else {
            "换一个筛选条件继续查看；没有出现在当前筛选中不代表没有依赖。"
        },
        primaryCta = if (filter == "all") "查看卡片" else "查看基础设施",
        onPrimary = { app.navigate(if (filter == "all") VScreen.CARDS else VScreen.OVERVIEW) },
    )
}

@Composable
private fun ColumnScope.NumberListSurface(
    filtered: List<UiVNextNumber>,
    selectedId: String?,
    app: VAppState,
    onSelect: (UiVNextNumber) -> Unit,
) {
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
                NumberRow(number, selectedId == number.id, app) { onSelect(number) }
            }
        }
    }
}

@Composable
private fun InspectorContent(app: VAppState, selected: UiVNextNumber?) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (selected == null) {
            Text("选择一个号码查看详情", color = PdigV2Colors.TextMuted, fontSize = 13.sp)
            return@Column
        }
        SectionHeader("号码详情")
        val profile = app.savedPresentationProfile("phoneNumber", selected.id)
        NumberFace(
            number = selected.copy(preset = profile?.themeId ?: selected.preset),
            privacyMask = app.privacyMask || (profile?.maskSensitive == true),
            onClick = { app.openNumber(selected.id) },
            modifier = Modifier
                .fillMaxWidth()
                .testTagLocal("pdig.phone.inspector.identity.${selected.id}"),
            presentationMaterial = profile?.material,
            presentationAccent = com.pdig.uivnext.model.hexColorOrNull(profile?.accentColor ?: "default"),
            presentationLayout = "compact",
        )
        Text(
            "通信身份 · ${selected.carrier} · ${if (selected.simKind == "eSIM") "eSIM" else "实体 SIM"}",
            color = PdigV2Colors.TextSecondary,
            fontSize = 12.sp,
        )

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = VTouchTarget.Min)
                    .clickable { app.openNumberCustomization(selected.id) }
                    .testTagLocal("pdig.phone.inspector.customize"),
                color = PdigV2Colors.SurfaceRaised,
                shape = RoundedCornerShape(VRadius.Md),
                border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
            ) {
                Text(
                    "定制号码面",
                    Modifier.padding(12.dp),
                    color = PdigV2Colors.TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = VTouchTarget.Min)
                    .clickable { app.openNumber(selected.id) }
                    .testTagLocal("pdig.phone.inspector.open-detail"),
                color = PdigV2Colors.PrimarySoft,
                shape = RoundedCornerShape(VRadius.Md),
            ) {
                Text(
                    "查看完整详情 →",
                    Modifier.padding(12.dp),
                    color = PdigV2Colors.PrimaryBright,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        val services = UiVNextDemoFixture.servicesForNumber(selected.id)
        SectionHeader("关联服务（${services.size}）")
        services.forEach { service ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(service.name, color = PdigV2Colors.TextSecondary, fontSize = 13.sp)
                LabelChip(numberServiceKindLabel(service.kind))
            }
        }

    }
}

@Composable
private fun FilterRowNumbers(active: String, onFilter: (String) -> Unit) {
    val filters = listOf(
        "all" to "全部",
        "esim" to "eSIM",
        "sim" to "实体 SIM",
        "primary" to "主号",
        "secondary" to "副号",
        "keep" to "保号",
    )
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        filters.forEach { (key, label) ->
            val selected = active == key
            Box(
                modifier = Modifier
                    .defaultMinSize(minHeight = VTouchTarget.Min)
                    .clickable { onFilter(key) },
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    color = if (selected) PdigV2Colors.PrimarySoft else PdigV2Colors.SurfaceRaised,
                    shape = RoundedCornerShape(VRadius.Sm),
                    border = BorderStroke(
                        1.dp,
                        if (selected) PdigV2Colors.PrimaryBright.copy(alpha = 0.72f) else PdigV2Colors.BorderSubtle,
                    ),
                ) {
                    Text(
                        label,
                        Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        color = if (selected) PdigV2Colors.PrimaryBright else PdigV2Colors.TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

private fun matchesNumberFilter(number: UiVNextNumber, filter: String): Boolean = when (filter) {
    "esim" -> number.simKind == "eSIM"
    "sim" -> number.simKind != "eSIM"
    "primary" -> number.role == "primary"
    "secondary" -> number.role == "secondary"
    "keep" -> number.usages.any { it.contains("保号") } || number.preset == "recovery"
    else -> true
}

@Composable
private fun NumberRow(number: UiVNextNumber, selected: Boolean, app: VAppState, onClick: () -> Unit) {
    val profile = app.savedPresentationProfile("phoneNumber", number.id)
    val displayNumber = number.copy(preset = profile?.themeId ?: number.preset)
    val maskSensitive = app.privacyMask || (profile?.maskSensitive == true)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .defaultMinSize(minHeight = 82.dp)
            .clickable(onClick = onClick)
            .testTagLocal(VTestIds.NUMBER_ROW),
        color = if (selected) PdigV2Colors.PrimarySoft else PdigV2Colors.Surface,
        shape = RoundedCornerShape(VRadius.Lg),
        border = BorderStroke(
            1.dp,
            if (selected) PdigV2Colors.PrimaryBright.copy(alpha = 0.85f) else PdigV2Colors.BorderSubtle,
        ),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NumberIdentityThumbnail(
                number = displayNumber,
                privacyMask = maskSensitive,
                modifier = Modifier.width(92.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(number.nickname, color = PdigV2Colors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(
                    if (maskSensitive) "${number.countryCode} •••• ••••" else number.maskedNumber,
                    color = PdigV2Colors.TextSecondary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                )
                Text(
                    "${if (number.simKind == "eSIM") "eSIM" else "实体 SIM"} · ${if (number.role == "primary") "主号" else if (number.role == "keep") "保号" else "副号"} · ${number.usages.take(2).joinToString(" / ")}",
                    color = PdigV2Colors.TextMuted,
                    fontSize = 11.sp,
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (number.recoveryOnly) LabelChip("唯一恢复", highlight = true)
                StatusBadge(number.status)
            }
        }
    }
}

private fun numberServiceKindLabel(kind: String): String = when (kind) {
    "payment" -> "支付"
    "banking" -> "银行"
    "subscription" -> "订阅"
    "twoFA" -> "2FA"
    else -> "关联"
}
