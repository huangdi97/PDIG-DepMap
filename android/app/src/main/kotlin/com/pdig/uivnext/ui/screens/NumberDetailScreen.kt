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
import com.pdig.uivnext.ui.components.SectionHeader

/** Number Detail：号码身份 → 关联服务 → 风险 → 备用路径 → 历史，保持紧凑连续的语义流。 */
@Composable
fun NumberDetailScreen(app: VAppState) {
    val number = UiVNextDemoFixture.numberById(app.selectedNumberId ?: "num-cn-1") ?: return
    val services = UiVNextDemoFixture.servicesForNumber(number.id)
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

        SectionHeader("风险")
        if (number.recoveryOnly) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = PdigV2Colors.Warning.copy(alpha = 0.14f),
                shape = RoundedCornerShape(VRadius.Md),
            ) {
                Text(
                    "此号码是 2 个账户的唯一恢复路径：更换或注销前，必须先建立新的恢复方式。",
                    Modifier.padding(14.dp),
                    color = PdigV2Colors.TextPrimary,
                    fontSize = 13.sp,
                )
            }
        } else {
            Text(
                "当前记录中未发现该号码承担唯一恢复路径；未记录的关联仍保持为未知。",
                color = PdigV2Colors.TextSecondary,
                fontSize = 13.sp,
            )
        }

        SectionHeader("备用路径")
        Text(
            "已确认的登录用途存在其他验证渠道；尚未记录的关联保持为未知。",
            color = PdigV2Colors.TextSecondary,
            fontSize = 13.sp,
        )

        SectionHeader("历史")
        Text(
            "2026-08 更新运营商资料；2026-03 加入 2FA 用途。",
            color = PdigV2Colors.TextMuted,
            fontSize = 12.sp,
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
            NumberSummaryItem(if (number.role == "primary") "主号" else "副号", "角色", Modifier.weight(1f))
            NumberSummaryItem(if (number.simKind == "eSIM") "eSIM" else "实体 SIM", "形态", Modifier.weight(1f))
            NumberSummaryItem(serviceCount.toString(), "关联服务", Modifier.weight(1f))
            NumberSummaryItem(if (number.recoveryOnly) "唯一" else "多路径", "恢复", Modifier.weight(1f))
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

