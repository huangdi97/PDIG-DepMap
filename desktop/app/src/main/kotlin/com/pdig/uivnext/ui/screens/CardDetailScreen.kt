package com.pdig.uivnext.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

/**
 * Card Detail（PHASE 1C §16）：顶部 Hero Asset Stage（卡占 ~40%，最大视觉对象之一）
 * + issuer/status/region/currency/expiry/主操作旁列；下方才进入 绑定服务/风险/恢复与替代/历史。
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
        // Hero Asset Stage
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(VSpacing.Xl)) {
            Box(
                Modifier
                    .width(430.dp)
                    .testTagLocal(VTestIds.CARD_DETAIL_IDENTITY),
            ) {
                AssetCard(card = card, privacyMask = app.privacyMask, onClick = {})
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(VSpacing.Sm)) {
                SectionHeader("卡片资料")
                DetailRow("发卡行", card.issuer)
                DetailRow("卡组织", card.network)
                DetailRow("地区", card.region)
                DetailRow("币种", card.currency)
                DetailRow("卡种", if (card.type == "credit") "信用卡" else "储蓄卡")
                DetailRow("形态", if (card.form == "virtual") "虚拟卡" else "实体卡")
                DetailRow("有效期", card.expiry)
                DetailRow("状态", statusLabelZh(card.status))
                Row(horizontalArrangement = Arrangement.spacedBy(VSpacing.Sm)) {
                    Surface(
                        Modifier.clickableLocal { app.openCardCustomization(card.id) },
                        color = PdigV2Colors.PrimarySoft,
                        shape = RoundedCornerShape(VRadius.Md),
                    ) {
                        Text("定制卡面 →", Modifier.padding(horizontal = VSpacing.Md, vertical = 6.dp), color = PdigV2Colors.PrimaryBright, style = VType.Label)
                    }
                    LabelChip("当前主题：${card.preset} · 素材全部本地")
                }
            }
        }
        // 下方：绑定服务 / 风险 / 恢复与替代 / 历史
        SectionHeader("使用场景")
        Row(horizontalArrangement = Arrangement.spacedBy(VSpacing.Sm)) {
            card.usages.forEach { LabelChip(it) }
        }
        SectionHeader("绑定服务（${services.size}）")
        services.forEach { service ->
            Surface(
                Modifier.fillMaxWidth(),
                color = PdigV2Colors.Surface,
                shape = RoundedCornerShape(VRadius.Md),
                border = androidx.compose.foundation.BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
            ) {
                Row(
                    Modifier.padding(VSpacing.Lg),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(service.name, color = PdigV2Colors.TextPrimary, style = VType.Body, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                        Text("地区 ${service.region} · ${service.kind}", color = PdigV2Colors.TextMuted, style = VType.Meta)
                    }
                    LabelChip(if (service.kind == "funding") "资金来源" else "验证方式")
                }
            }
        }
        SectionHeader("风险")
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
        SectionHeader("恢复与替代")
        Text(
            "模拟更换此卡：影响分析由已确认依赖驱动（未知 = 未知，绝不推断）。接入 Impact Kernel 后此处展示「必须处理 / 有备用路径 / 建议检查」分级。",
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
private fun DetailRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = PdigV2Colors.TextMuted, style = VType.Meta)
        Text(value, color = PdigV2Colors.TextPrimary, style = VType.Secondary, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
    }
}
