package com.pdig.uivnext.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.pdig.uivnext.ui.VAppState
import com.pdig.uivnext.ui.components.AssetCard
import com.pdig.uivnext.ui.components.LabelChip
import com.pdig.uivnext.ui.components.SectionHeader
import com.pdig.uivnext.theme.statusLabelZh

/**
 * Card Detail：顶部先看到卡的视觉身份（identity 列 ≈33%），右侧信息工作区（≈67%）。
 * 卡面 = 资产身份主角；region/currency/type/status 在 metadata 层。
 */
@Composable
fun CardDetailScreen(app: VAppState) {
    val card = UiVNextDemoFixture.cardById(app.selectedCardId ?: "card-cn-1") ?: return
    val services = UiVNextDemoFixture.servicesForCard(card.id)
    Row(
        Modifier
            .fillMaxSize()
            .padding(VSpacing.Xxl),
    ) {
        // Identity column（左 34%）
        Box(
            Modifier
                .weight(0.34f)
                .fillMaxHeight(),
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .testTagLocal(VTestIds.CARD_DETAIL_IDENTITY),
                verticalArrangement = Arrangement.spacedBy(VSpacing.Xxl),
            ) {
                Column(Modifier.fillMaxWidth()) {
                    Text("卡片身份", style = VType.Meta, color = PdigV2Colors.TextMuted)
                    Spacer(Modifier.height(4.dp))
                    AssetCard(card = card, privacyMask = app.privacyMask, onClick = {})
                }
                DataPanel(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(VSpacing.Xl), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        DetailRow("卡组织", card.network)
                        DetailRow("地区", card.region)
                        DetailRow("币种", card.currency)
                        DetailRow("卡种", if (card.type == "credit") "信用卡" else "储蓄卡")
                        DetailRow("形态", if (card.form == "virtual") "虚拟卡" else "实体卡")
                        DetailRow("有效期", card.expiry)
                        DetailRow("状态", statusLabelZh(card.status))
                    }
                }
                // 呈现（Presentation Layer 铁律）
                Surface(
                    Modifier.fillMaxWidth(),
                    color = PdigV2Colors.Surface.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(VRadius.Lg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
                ) {
                    Column(Modifier.padding(VSpacing.Lg)) {
                        Text("呈现", style = VType.Label, color = PdigV2Colors.TextSecondary)
                        Spacer(Modifier.height(VSpacing.Sm))
                        Surface(
                            Modifier
                                .fillMaxWidth()
                                .testTagLocal(VTestIds.CARD_DETAIL_INFO)
                                .clickableLocal { app.openCardCustomization(card.id) },
                            color = PdigV2Colors.PrimarySoft,
                            shape = RoundedCornerShape(VRadius.Md),
                        ) {
                            Text(
                                "定制卡面 →",
                                Modifier.padding(VSpacing.Md),
                                color = PdigV2Colors.PrimaryBright,
                                style = VType.Label,
                            )
                        }
                        Spacer(Modifier.height(VSpacing.Sm))
                        LabelChip("当前主题：${card.preset} · 素材全部本地")
                        Spacer(Modifier.height(VSpacing.Md))
                        Text(
                            "Presentation 层：自定义不影响依赖/证据/确认；不写入 .depmap",
                            style = VType.Meta,
                            color = PdigV2Colors.TextMuted,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.width(VSpacing.Xxl))
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
                verticalArrangement = Arrangement.spacedBy(VSpacing.Xxl),
            ) {
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
