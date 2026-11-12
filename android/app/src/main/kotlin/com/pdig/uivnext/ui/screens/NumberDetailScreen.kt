package com.pdig.uivnext.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.demo.UiVNextDemoFixture
import com.pdig.uivnext.demo.numberImpactLens
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.model.VTestIds
import com.pdig.uivnext.model.hexColorOrNull
import com.pdig.uivnext.model.relationKindLabelZh
import com.pdig.uivnext.model.serviceKindLabelZh
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.theme.VTouchTarget
import com.pdig.uivnext.ui.VAppState
import com.pdig.uivnext.ui.components.LabelChip
import com.pdig.uivnext.ui.components.NumberFace
import com.pdig.uivnext.ui.components.ObjectImpactLens
import com.pdig.uivnext.ui.components.SectionHeader

/** Number Detail：号码身份 → 关联服务 → 风险 → 备用路径 → 历史，保持紧凑连续的语义流。 */
@Composable
fun NumberDetailScreen(app: VAppState) {
    val number = UiVNextDemoFixture.numberById(app.selectedNumberId ?: "num-cn-1") ?: return
    val services = UiVNextDemoFixture.servicesForNumber(number.id)
    val lifecycle = UiVNextDemoFixture.numberLifecycleFor(number.id)
    val impact = numberImpactLens(number.id)
    val presentation = app.savedPresentationProfile("phoneNumber", number.id)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        NumberFace(
            number = number.copy(preset = presentation?.themeId ?: number.preset),
            privacyMask = app.privacyMask || (presentation?.maskSensitive == true),
            onClick = {},
            modifier = Modifier
                .fillMaxWidth()
                .testTagLocal(VTestIds.NUMBER_DETAIL_HERO),
            presentationMaterial = presentation?.material,
            presentationAccent = hexColorOrNull(presentation?.accentColor ?: "default"),
            presentationLayout = presentation?.layout,
        )
        NumberSummaryStrip(number = number, serviceCount = services.size)

        Surface(
            modifier = Modifier.fillMaxWidth().testTagLocal("pdig.r19.number.lifecycle.adaptive"),
            color = PdigV2Colors.Surface,
            shape = RoundedCornerShape(VRadius.Lg),
            border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
        ) {
            Column(
                Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("号码生命周期", color = PdigV2Colors.TextPrimary,
                    fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LifecycleFact("资费", lifecycle?.planCost, Modifier.weight(1f))
                    LifecycleFact("下次保号", lifecycle?.keepAliveDue, Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LifecycleFact("保号周期", lifecycle?.keepAliveCycle, Modifier.weight(1f))
                    LifecycleFact("最近操作", lifecycle?.lastKeepAlive, Modifier.weight(1f))
                }
                DetailTruthLine("计费方式", lifecycle?.billingMode ?: "未记录")
                DetailTruthLine("续费 / 保号方式", lifecycle?.renewalMethod ?: "未记录")
                Text(
                    "这些是用户已记录资料，不代表运营商实时状态；缺失信息保持“未记录”。",
                    color = PdigV2Colors.TextMuted,
                    fontSize = 10.sp,
                )
            }
        }

        SectionHeader(
            title = "关联服务（${services.size}）",
            modifier = Modifier.testTagLocal(VTestIds.NUMBER_DETAIL_SERVICES),
            trailing = {
                Surface(
                    modifier = Modifier
                        .defaultMinSize(minHeight = VTouchTarget.Min)
                        .clickableLocal { app.openNumberCustomization(number.id) },
                    color = PdigV2Colors.PrimarySoft,
                    shape = RoundedCornerShape(VRadius.Md),
                ) {
                    Text(
                        "定制号码面 →",
                        Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = PdigV2Colors.PrimaryText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            },
        )

        services.forEachIndexed { index, service ->
            val relation = UiVNextDemoFixture.relations.firstOrNull { it.from == number.id && it.to == service.id }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTagLocal("pdig.number.detail.service.$index"),
                color = PdigV2Colors.SurfaceRaised,
                shape = RoundedCornerShape(VRadius.Md),
                border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            service.name,
                            color = PdigV2Colors.TextPrimary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                        )
                        Text(serviceKindLabelZh(service.kind), color = PdigV2Colors.TextMuted, fontSize = 12.sp)
                    }
                    LabelChip(relationKindLabelZh(relation?.kind))
                }
            }
        }

        SectionHeader("风险与恢复")
        when {
            number.uniqueRecoveryPath == true -> {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = PdigV2Colors.Critical.copy(alpha = 0.11f),
                    shape = RoundedCornerShape(VRadius.Md),
                ) {
                    Text(
                        "已确认：这个号码是唯一恢复路径。更换或注销前，必须先建立并验证独立替代路径；这里不推断未记录账户数量。",
                        Modifier.padding(14.dp),
                        color = PdigV2Colors.TextPrimary,
                        fontSize = 13.sp,
                    )
                }
            }
            number.recoveryOnly -> {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = PdigV2Colors.Warning.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(VRadius.Md),
                ) {
                    Text(
                        "已记录：这个号码承担恢复用途，但是否唯一仍未知。更换或注销前，需要逐项核对恢复关系。",
                        Modifier.padding(14.dp),
                        color = PdigV2Colors.TextPrimary,
                        fontSize = 13.sp,
                    )
                }
            }
            else -> {
                Text(
                    "没有足够证据判断是否存在恢复或唯一恢复路径；未记录关系继续保持未知。",
                    color = PdigV2Colors.TextSecondary,
                    fontSize = 13.sp,
                )
            }
        }

        ObjectImpactLens(
            impact = impact,
            actionLabel = "分析更换号码影响",
            onAction = { app.navigate(VScreen.CHANGE_PHONE) },
        )
    }
}

@Composable
private fun NumberSummaryStrip(
    number: com.pdig.uivnext.model.UiVNextNumber,
    serviceCount: Int,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTagLocal("pdig.number.detail.summary"),
        color = PdigV2Colors.PrimarySoft.copy(alpha = 0.64f),
        shape = RoundedCornerShape(VRadius.Lg),
        border = BorderStroke(1.dp, PdigV2Colors.Primary.copy(alpha = 0.16f)),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            NumberSummaryItem(
                when (number.role) {
                    "primary" -> "主号"
                    "keep" -> "保号"
                    "secondary" -> "副号"
                    else -> number.role
                },
                "角色",
                Modifier.weight(1f),
            )
            NumberSummaryItem(if (number.simKind == "eSIM") "eSIM" else "实体 SIM", "形态", Modifier.weight(1f))
            NumberSummaryItem(serviceCount.toString(), "关联服务", Modifier.weight(1f))
            NumberSummaryItem(
                when {
                    number.uniqueRecoveryPath == true -> "唯一恢复"
                    number.recoveryOnly -> "恢复用途"
                    else -> "未知"
                },
                "恢复",
                Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun NumberSummaryItem(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value, color = PdigV2Colors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text(label, color = PdigV2Colors.TextMuted, fontSize = 10.sp)
    }
}



@Composable
private fun LifecycleFact(
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
            Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(label, color = PdigV2Colors.TextMuted, fontSize = 10.sp)
            Text(
                value?.takeIf { it.isNotBlank() } ?: "未记录",
                color = PdigV2Colors.TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun DetailTruthLine(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = PdigV2Colors.TextMuted, fontSize = 11.sp)
        Text(value, color = PdigV2Colors.TextPrimary, fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold, maxLines = 2)
    }
}
