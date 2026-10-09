package com.pdig.uivnext.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.demo.UiVNextDemoFixture
import com.pdig.uivnext.demo.cardImpactLens
import com.pdig.uivnext.model.MediaBreakpoint
import com.pdig.uivnext.model.VTestIds
import com.pdig.uivnext.model.hexColorOrNull
import com.pdig.uivnext.model.relationKindLabelZh
import com.pdig.uivnext.model.serviceKindLabelZh
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.statusLabelZh
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.theme.VTouchTarget
import com.pdig.uivnext.ui.VAppState
import com.pdig.uivnext.ui.components.AssetCard
import com.pdig.uivnext.ui.components.LabelChip
import com.pdig.uivnext.ui.components.ObjectImpactLens
import com.pdig.uivnext.ui.components.SectionHeader

/**
 * Card Detail：顶部先看到卡的视觉身份（identity 列 ≈33%），右侧信息工作区（≈67%）。
 * Expanded 并排；Compact / Medium 纵向单 pane（identity 在上）。外层 Box 定界避免 weight+verticalScroll 无限高约束。
 */
@Composable
fun CardDetailScreen(app: VAppState, breakpoint: MediaBreakpoint) {
    val card = UiVNextDemoFixture.cardById(app.selectedCardId ?: "card-cn-1") ?: return
    val services = UiVNextDemoFixture.servicesForCard(card.id)
    val lifecycle = UiVNextDemoFixture.cardLifecycleFor(card.id)
    val impact = cardImpactLens(card.id)
    val presentation = app.savedPresentationProfile("card", card.id)
    if (breakpoint == MediaBreakpoint.EXPANDED) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(24.dp),
        ) {
            // Identity column（左 33%）
            Box(
                Modifier
                    .weight(0.34f)
                    .fillMaxHeight(),
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .testTagLocal(VTestIds.CARD_DETAIL_IDENTITY),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    IdentityPanel(app, card, services, presentation)
                }
            }
            Spacer(Modifier.width(24.dp))
            // Info workspace（右 66%）
            Box(
                Modifier
                    .weight(0.66f)
                    .fillMaxHeight(),
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .testTagLocal(VTestIds.CARD_DETAIL_INFO),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    InfoPanel(card, services, lifecycle, impact)
                }
            }
        }
    } else if (breakpoint == MediaBreakpoint.COMPACT) {
        CompactCardDetailReference(app, card, services, presentation)
    } else {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(pagePadding(breakpoint)),
            verticalArrangement = Arrangement.spacedBy(pageSectionGap(breakpoint)),
        ) {
            Column(
                Modifier.fillMaxWidth().testTagLocal(VTestIds.CARD_DETAIL_IDENTITY),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) { IdentityPanel(app, card, services, presentation) }
            Column(
                Modifier.fillMaxWidth().testTagLocal(VTestIds.CARD_DETAIL_INFO),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) { InfoPanel(card, services, lifecycle, impact) }
        }
    }
}

/**
 * Phone reference: asset face first, followed by a real, switchable four-tab
 * detail workspace. "账单" never fabricates a statement which has not been imported.
 * This is purely presentation: it cannot mutate stored card identity or relations.
 */
@Composable
private fun CompactCardDetailReference(
    app: VAppState,
    card: com.pdig.uivnext.model.UiVNextCard,
    services: List<com.pdig.uivnext.model.UiVNextService>,
    presentation: com.pdig.uivnext.model.PresentationProfile?,
) {
    val lifecycle = UiVNextDemoFixture.cardLifecycleFor(card.id)
    val impact = cardImpactLens(card.id)
    var tab by rememberSaveable(card.id) { mutableIntStateOf(0) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 15.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Box(Modifier.fillMaxWidth().testTagLocal(VTestIds.CARD_DETAIL_IDENTITY)) {
            R19PresentedCardFace(
                card = card,
                profile = presentation,
                privacyMask = app.privacyMask || (presentation?.maskSensitive == true),
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = if (card.status == "expiring_soon") PdigV2Colors.Warning.copy(alpha = 0.13f)
                else PdigV2Colors.Positive.copy(alpha = 0.11f),
                shape = RoundedCornerShape(30.dp),
            ) {
                Text(
                    "●  " + statusLabelZh(card.status),
                    Modifier.padding(horizontal = 15.dp, vertical = 7.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (card.status == "expiring_soon") PdigV2Colors.Warning else PdigV2Colors.Positive,
                )
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth().testTagLocal("pdig.r19.card.lifecycle.adaptive"),
            color = PdigV2Colors.Surface,
            shape = RoundedCornerShape(VRadius.Lg),
            border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
        ) {
            Column(
                Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("用卡周期", color = PdigV2Colors.TextPrimary,
                    fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    CardLifecycleFact("年费", lifecycle?.annualFee, Modifier.weight(1f))
                    CardLifecycleFact("账单日", lifecycle?.billingDay, Modifier.weight(1f))
                    CardLifecycleFact("分期", lifecycle?.installmentSummary, Modifier.weight(1f))
                }
                Text("仅展示已记录资料；缺失字段保持“未记录”。",
                    color = PdigV2Colors.TextMuted, fontSize = 9.sp)
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("概览", "关联服务", "账单", "安全与风险").forEachIndexed { index, title ->
                Surface(
                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = VTouchTarget.Min)
                        .clickableLocal { tab = index }
                        .testTagLocal("pdig.card.detail.tab.$index"),
                    color = if (tab == index) PdigV2Colors.PrimarySoft else PdigV2Colors.Surface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (tab == index) PdigV2Colors.Primary.copy(alpha = 0.23f) else PdigV2Colors.BorderSubtle),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 13.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) { Text(title, color = if (tab == index) PdigV2Colors.PrimaryText else PdigV2Colors.TextSecondary,
                        fontSize = 10.sp, fontWeight = if (tab == index) FontWeight.Bold else FontWeight.Medium, maxLines = 1) }
                }
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth().testTagLocal(VTestIds.CARD_DETAIL_INFO),
            color = PdigV2Colors.Surface,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 13.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (tab) {
                    0 -> {
                        Text("基本信息", color = PdigV2Colors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        DetailRow("发卡机构", card.issuer)
                        DetailRow("卡片类型", if (card.type == "credit") "信用卡" else "储蓄卡")
                        DetailRow("国家 / 地区", regionLabel(card.region) + " · " + card.currency)
                        DetailRow("卡号后四位", card.last4)
                        DetailRow("有效期", card.expiry)
                        DetailRow("年费", lifecycle?.annualFee ?: "未记录")
                        DetailRow("年费节点", lifecycle?.annualFeeDue ?: "未记录")
                        DetailRow("账单日", lifecycle?.billingDay ?: "未记录")
                        DetailRow("还款日", lifecycle?.paymentDueDay ?: "未记录")
                        CardIdentitySummary(card, services.size)
                    }
                    1 -> {
                        Text("关联服务（${services.size}）", color = PdigV2Colors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        if (services.isEmpty()) {
                            Text("尚无已记录关联；未知不等于安全。", color = PdigV2Colors.TextSecondary, fontSize = 12.sp)
                        } else {
                            // Reference: associated services retain distinct icon-like visual
                            // identities. Monograms are derived from the known names and
                            // categorical tint is PRESENTATION only, never a risk verdict.
                            services.chunked(2).forEachIndexed { rowIndex, pair ->
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    pair.forEachIndexed { columnIndex, service ->
                                        val palette = listOf(
                                            androidx.compose.ui.graphics.Color(0xFF287AE7),
                                            androidx.compose.ui.graphics.Color(0xFF141A28),
                                            androidx.compose.ui.graphics.Color(0xFF0FAF8C),
                                            androidx.compose.ui.graphics.Color(0xFFF09B30),
                                        )
                                        val tint = palette[(rowIndex * 2 + columnIndex) % palette.size]
                                        Surface(
                                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 66.dp),
                                            color = PdigV2Colors.SurfaceRaised,
                                            shape = RoundedCornerShape(14.dp),
                                            border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
                                        ) {
                                            Column(
                                                Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                                                verticalArrangement = Arrangement.spacedBy(5.dp),
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                                    Surface(
                                                        color = tint,
                                                        shape = RoundedCornerShape(9.dp),
                                                    ) {
                                                        Text(
                                                            service.name.take(1).uppercase(),
                                                            Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                                            color = androidx.compose.ui.graphics.Color.White,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 12.sp,
                                                        )
                                                    }
                                                    Text(
                                                        service.name,
                                                        color = PdigV2Colors.TextPrimary,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        maxLines = 1,
                                                        modifier = Modifier.weight(1f),
                                                    )
                                                }
                                                Text(
                                                    serviceKindLabelZh(service.kind),
                                                    color = PdigV2Colors.TextSecondary,
                                                    fontSize = 10.sp,
                                                )
                                            }
                                        }
                                    }
                                    if (pair.size == 1) {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        Text("账单与分期", color = PdigV2Colors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("没有账单证据时，不推断交易、余额、最低还款额或实际扣款。", color = PdigV2Colors.TextSecondary, fontSize = 12.sp)
                        DetailRow("账单日", lifecycle?.billingDay ?: "未记录")
                        DetailRow("还款日", lifecycle?.paymentDueDay ?: "未记录")
                        DetailRow("年费", lifecycle?.annualFee ?: "未记录")
                        DetailRow("年费节点", lifecycle?.annualFeeDue ?: "未记录")
                        DetailRow("分期", lifecycle?.installmentSummary ?: "未记录")
                        DetailRow("自动还款", lifecycle?.autoPaySummary ?: "未记录")
                        DetailRow("卡片有效期", card.expiry)
                    }
                    else -> {
                        Text("安全与风险", color = PdigV2Colors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            if (card.status == "expiring_soon") "到期风险：这张卡可能影响 ${services.size} 项已记录的绑定服务。"
                            else "当前状态：${statusLabelZh(card.status)}。未录入的依赖关系仍保持未知。",
                            color = PdigV2Colors.TextSecondary, fontSize = 12.sp,
                        )
                        DetailRow("关联服务", "${services.size} 项")
                    }
                }
            }
        }
        ObjectImpactLens(impact = impact)

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Surface(
                modifier = Modifier.defaultMinSize(minHeight = VTouchTarget.Min)
                    .clickableLocal { app.openCardCustomization(card.id) },
                color = PdigV2Colors.SurfaceRaised,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
            ) {
                Text(
                    "更换卡面图片 →",
                    Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                    color = PdigV2Colors.PrimaryText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun IdentityPanel(
    app: VAppState,
    card: com.pdig.uivnext.model.UiVNextCard,
    services: List<com.pdig.uivnext.model.UiVNextService>,
    presentation: com.pdig.uivnext.model.PresentationProfile?,
) {
    Surface(
        Modifier
            .fillMaxWidth()
            .testTagLocal(VTestIds.CARD_DETAIL_IDENTITY),
        color = PdigV2Colors.Surface.copy(alpha = 0.9f),
        shape = RoundedCornerShape(VRadius.Xl),
        border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        Column(Modifier.padding(20.dp)) {
            R19PresentedCardFace(
                card = card,
                profile = presentation,
                privacyMask = app.privacyMask || (presentation?.maskSensitive == true),
                onClick = {},
            )
            Spacer(Modifier.height(14.dp))
            CardIdentitySummary(card = card, serviceCount = services.size)
            Spacer(Modifier.height(14.dp))
            DetailRow("卡组织", card.network)
            DetailRow("地区", regionLabel(card.region))
            DetailRow("币种", card.currency)
            DetailRow("卡种", if (card.type == "credit") "信用卡" else "储蓄卡")
            DetailRow("形态", if (card.form == "virtual") "虚拟卡" else "实体卡")
            DetailRow("有效期", card.expiry)
        }
    }
    // 外观设置是次要微功能：保持 48dp 可触控，但不占用独立大面板。
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        Surface(
            modifier = Modifier
                .defaultMinSize(minHeight = VTouchTarget.Min)
                .testTagLocal("pdig.card.detail.change-art")
                .clickableLocal { app.openCardCustomization(card.id) },
            color = PdigV2Colors.SurfaceRaised,
            shape = RoundedCornerShape(VRadius.Md),
            border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
        ) {
            Text(
                "更换卡面图片 →",
                Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                color = PdigV2Colors.PrimaryText,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun CardIdentitySummary(
    card: com.pdig.uivnext.model.UiVNextCard,
    serviceCount: Int,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTagLocal("pdig.card.detail.summary"),
        color = PdigV2Colors.PrimarySoft.copy(alpha = 0.62f),
        shape = RoundedCornerShape(VRadius.Lg),
        border = BorderStroke(1.dp, PdigV2Colors.Primary.copy(alpha = 0.16f)),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CardSummaryItem(regionLabel(card.region), "地区", Modifier.weight(1f))
            CardSummaryItem(card.currency, "币种", Modifier.weight(1f))
            CardSummaryItem(serviceCount.toString(), "绑定服务", Modifier.weight(1f))
        }
    }
}

@Composable
private fun CardSummaryItem(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value, color = PdigV2Colors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text(label, color = PdigV2Colors.TextMuted, fontSize = 10.sp)
    }
}

@Composable
private fun InfoPanel(
    card: com.pdig.uivnext.model.UiVNextCard,
    services: List<com.pdig.uivnext.model.UiVNextService>,
    lifecycle: com.pdig.uivnext.model.UiVNextCardLifecycle?,
    impact: com.pdig.uivnext.demo.UiImpactLens,
) {
    SectionHeader("用卡周期")
    Surface(
        modifier = Modifier.fillMaxWidth().testTagLocal("pdig.r19.card.lifecycle.workspace"),
        color = PdigV2Colors.SurfaceRaised,
        shape = RoundedCornerShape(VRadius.Lg),
        border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            DetailRow("年费", lifecycle?.annualFee ?: "未记录")
            DetailRow("年费节点", lifecycle?.annualFeeDue ?: "未记录")
            DetailRow("账单日", lifecycle?.billingDay ?: "未记录")
            DetailRow("还款日", lifecycle?.paymentDueDay ?: "未记录")
            DetailRow("分期", lifecycle?.installmentSummary ?: "未记录")
            DetailRow("自动还款", lifecycle?.autoPaySummary ?: "未记录")
            Text("这些字段只表示已记录资料，不代表银行实时账单。",
                color = PdigV2Colors.TextMuted, fontSize = 10.sp)
        }
    }

    SectionHeader("使用场景")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        card.usages.forEach { LabelChip(it) }
    }
    SectionHeader("绑定服务（${services.size}）")
    services.forEach { service ->
        val relation = UiVNextDemoFixture.relations.firstOrNull { it.from == card.id && it.to == service.id }
        Surface(
            Modifier.fillMaxWidth(),
            color = PdigV2Colors.SurfaceRaised,
            shape = RoundedCornerShape(VRadius.Md),
            border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
        ) {
            Row(
                Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(service.name, color = PdigV2Colors.TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    Text("${regionLabel(service.region)} · ${serviceKindLabelZh(service.kind)}", color = PdigV2Colors.TextMuted, fontSize = 12.sp)
                }
                LabelChip(relationKindLabelZh(relation?.kind))
            }
        }
    }
    SectionHeader("风险")
    if (card.status == "expiring_soon") {
        Surface(color = PdigV2Colors.Critical.copy(alpha = 0.12f), shape = RoundedCornerShape(VRadius.Md), modifier = Modifier.fillMaxWidth()) {
            Text(
                "卡片将在 ${card.expiry} 到期：${services.size} 项绑定服务可能中断，建议提前更换卡后重新绑定。",
                Modifier.padding(14.dp),
                color = PdigV2Colors.TextPrimary,
                fontSize = 13.sp,
            )
        }
    } else {
        Text("在已记录关系中未发现必须立即处理的风险；未记录的关联仍保持未知。", color = PdigV2Colors.TextSecondary, fontSize = 13.sp)
    }
    SectionHeader("影响")
    ObjectImpactLens(impact = impact)

    SectionHeader("恢复与替代")
    Text(
        "尚未记录独立备用支付路径时，界面不会推断存在替代方案；更换前应先核对已确认绑定服务。",
        color = PdigV2Colors.TextSecondary,
        fontSize = 13.sp,
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = PdigV2Colors.TextMuted, fontSize = 12.sp)
        Text(value, color = PdigV2Colors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}



@Composable
private fun CardLifecycleFact(
    label: String,
    value: String?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = PdigV2Colors.SurfaceRaised,
        shape = RoundedCornerShape(VRadius.Md),
        border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        Column(
            Modifier.padding(horizontal = 9.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(label, color = PdigV2Colors.TextMuted, fontSize = 9.sp)
            Text(value?.takeIf { it.isNotBlank() } ?: "未记录",
                color = PdigV2Colors.TextPrimary, fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold, maxLines = 2)
        }
    }
}
