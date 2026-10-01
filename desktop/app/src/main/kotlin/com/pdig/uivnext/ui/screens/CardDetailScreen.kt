package com.pdig.uivnext.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pdig.uivnext.demo.UiVNextDemoFixture
import com.pdig.uivnext.model.VTestIds
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.theme.VSpacing
import com.pdig.uivnext.theme.VType
import com.pdig.uivnext.theme.statusLabelZh
import com.pdig.uivnext.ui.VAppState
import com.pdig.uivnext.ui.components.AssetCard
import com.pdig.uivnext.ui.components.LabelChip
import com.pdig.uivnext.ui.components.SectionHeader
import com.pdig.uivnext.ui.components.StatusBadge

/**
 * Card Detail（PHASE 1D §16–17）：顶部 Hero 舞台。
 * LEFT ~42%：大卡舞台（local illumination + floor reflection + soft depth，卡不贴黑角落）；
 * RIGHT ~58%：issuer identity / status / region / currency / expiry + 主操作
 * （模拟换卡 / 标记即将到期 / 查看绑定 / 定制卡面）。
 * 下方：绑定服务 / 影响与风险 / 备用支付 / 变更历史 —— 「卡片资料」不再是 label-value 表主导。
 */
@Composable
fun CardDetailScreen(app: VAppState) {
    val card = UiVNextDemoFixture.cardById(app.selectedCardId ?: "card-cn-1") ?: return
    val services = UiVNextDemoFixture.servicesForCard(card.id)
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(VSpacing.Xxl),
        verticalArrangement = Arrangement.spacedBy(VSpacing.Xl),
    ) {
        // Hero Stage（42% / 58%）
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(VSpacing.Xl)) {
            Box(
                Modifier
                    .width(600.dp)
                    .height(400.dp)
                    .testTagLocal(VTestIds.CARD_DETAIL_IDENTITY),
            ) {
                CardHeroStage {
                    AssetCard(card = card, privacyMask = app.privacyMask, onClick = {})
                }
            }
            Column(Modifier.weight(1f).testTagLocal(VTestIds.CARD_DETAIL_INFO), verticalArrangement = Arrangement.spacedBy(VSpacing.Md)) {
                Text(card.nickname, style = VType.PageTitle, color = PdigV2Colors.TextPrimary, maxLines = 1)
                Text(card.issuer, style = VType.SectionTitle, color = PdigV2Colors.TextSecondary, maxLines = 1)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(VSpacing.Md)) {
                    StatusBadge(card.status)
                    LabelChip(if (card.form == "virtual") "虚拟卡" else "实体卡")
                    LabelChip(if (card.type == "credit") "信用卡" else "储蓄卡")
                }
                Spacer(Modifier.height(VSpacing.Sm))
                FactRow("地区", card.region)
                FactRow("币种", card.currency)
                FactRow("有效期", card.expiry)
                FactRow("卡组织", card.network)
                Spacer(Modifier.height(VSpacing.Sm))
                // Primary actions
                Row(horizontalArrangement = Arrangement.spacedBy(VSpacing.Sm)) {
                    ActionButton("模拟换卡", primary = true) {}
                    ActionButton("标记即将到期") {}
                    ActionButton("查看绑定") {}
                    ActionButton("定制卡面 →") { app.openCardCustomization(card.id) }
                }
                Text(
                    "「卡片资料」以身份为主：发卡行 / 卡组织 / 地区 / 币种 / 卡种 / 形态 / 有效期 / 状态。",
                    style = VType.Meta,
                    color = PdigV2Colors.TextMuted,
                )
            }
        }
        // 下方：绑定服务 / 影响与风险 / 备用支付 / 变更历史
        SectionHeader("绑定服务（${services.size}）")
        services.forEach { service ->
            Surface(
                Modifier.fillMaxWidth(),
                color = PdigV2Colors.Surface,
                shape = RoundedCornerShape(VRadius.Md),
                border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
            ) {
                Row(
                    Modifier.padding(VSpacing.Lg),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(service.name, color = PdigV2Colors.TextPrimary, style = VType.Body, fontWeight = FontWeight.Medium, maxLines = 1)
                        Text("地区 ${service.region} · ${service.kind}", color = PdigV2Colors.TextMuted, style = VType.Meta, maxLines = 1)
                    }
                    LabelChip(if (service.kind == "funding") "资金来源" else "验证方式")
                }
            }
        }
        SectionHeader("影响与风险")
        if (card.status == "expiring_soon") {
            Surface(color = PdigV2Colors.Critical.copy(alpha = 0.12f), shape = RoundedCornerShape(VRadius.Md), modifier = Modifier.fillMaxWidth()) {
                Text(
                    "卡片将在 ${card.expiry} 到期：${services.size} 项绑定服务可能中断，建议提前更换卡后重新绑定。",
                    Modifier.padding(VSpacing.Lg),
                    color = PdigV2Colors.TextPrimary,
                    style = VType.Secondary,
                )
            }
        } else {
            Text("当前未发现必须处理的风险。", color = PdigV2Colors.TextSecondary, style = VType.Secondary)
        }
        SectionHeader("备用支付")
        Text(
            "备用支付路径全部来自已确认依赖；未知 = 未知。模拟更换此卡的影响分析由已确认依赖驱动。",
            color = PdigV2Colors.TextSecondary,
            style = VType.Secondary,
        )
        SectionHeader("变更历史")
        Text(
            "最近记录：2026-06 补充账单日资料；2025-11 更新卡片昵称。",
            color = PdigV2Colors.TextMuted,
            style = VType.Meta,
        )
    }
}

@Composable
private fun CardHeroStage(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .drawBehind { drawCardHeroStage() },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCardHeroStage() {
    val w = size.width
    val h = size.height
    drawRect(Brush.verticalGradient(listOf(PdigV2Colors.CanvasDeep.copy(alpha = 0.7f), PdigV2Colors.Canvas)))
    drawCircle(
        brush = Brush.radialGradient(
            listOf(PdigV2Colors.LocalIllum.copy(alpha = 0.42f), Color.Transparent),
            center = Offset(w * 0.5f, h * 0.42f),
            radius = w * 0.6f,
        ),
        radius = w * 0.6f,
        center = Offset(w * 0.5f, h * 0.42f),
    )
    drawOval(
        brush = Brush.radialGradient(
            listOf(PdigV2Colors.PrimaryBright.copy(alpha = 0.12f), Color.Transparent),
            center = Offset(w * 0.5f, h * 0.86f),
            radius = w * 0.28f,
        ),
        topLeft = Offset(w * 0.5f - w * 0.28f, h * 0.86f - w * 0.10f),
        size = androidx.compose.ui.geometry.Size(w * 0.56f, w * 0.20f),
    )
}

@Composable
private fun FactRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = PdigV2Colors.TextMuted, style = VType.Meta, maxLines = 1)
        Spacer(Modifier.width(VSpacing.Md))
        Text(value, color = PdigV2Colors.TextPrimary, style = VType.Secondary, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@Composable
private fun ActionButton(label: String, primary: Boolean = false, onClick: () -> Unit = {}) {
    Surface(
        Modifier.clickableLocal(onClick = onClick),
        color = if (primary) PdigV2Colors.Primary else PdigV2Colors.SurfaceRaised,
        shape = RoundedCornerShape(VRadius.Md),
        border = if (primary) null else BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        Text(
            label,
            Modifier.padding(horizontal = VSpacing.Md, vertical = 6.dp),
            color = if (primary) PdigV2Colors.CanvasDeep else PdigV2Colors.TextSecondary,
            style = VType.Label,
            maxLines = 1,
        )
    }
}
