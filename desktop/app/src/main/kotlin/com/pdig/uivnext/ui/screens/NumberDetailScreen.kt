package com.pdig.uivnext.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.pdig.uivnext.ui.VAppState
import com.pdig.uivnext.ui.components.LabelChip
import com.pdig.uivnext.ui.components.NumberFace
import com.pdig.uivnext.ui.components.SectionHeader

/**
 * Number Detail（G8）：号码身份面（NUMBER IDENTITY FACE）优先。
 * region flag 视觉 + masked number 大号 + carrier/SIM/role/usage/recovery/status 徽标；
 * 关联服务 / 登录用途 / 2FA / 恢复用途 / 风险 / 备用路径 / 历史。
 */
@Composable
fun NumberDetailScreen(app: VAppState) {
    val number = UiVNextDemoFixture.numberById(app.selectedNumberId ?: "num-cn-1") ?: return
    val services = UiVNextDemoFixture.servicesForNumber(number.id)
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(VSpacing.Xxl),
        verticalArrangement = Arrangement.spacedBy(VSpacing.Xxl),
    ) {
        Column(Modifier.fillMaxWidth()) {
            Text("号码身份", style = VType.Meta, color = PdigV2Colors.TextMuted)
            Spacer(Modifier.height(4.dp))
            NumberFace(
                number = number,
                privacyMask = app.privacyMask,
                onClick = {},
                modifier = Modifier.testTagLocal(VTestIds.NUMBER_FACE),
            )
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionHeader("关联服务（${services.size}）", modifier = Modifier.weight(1f))
            Surface(
                Modifier.clickableLocal { app.openNumberCustomization(number.id) },
                color = PdigV2Colors.PrimarySoft,
                shape = RoundedCornerShape(VRadius.Md),
            ) {
                Text(
                    "定制号码面 →",
                    Modifier.padding(horizontal = VSpacing.Md, vertical = 6.dp),
                    color = PdigV2Colors.PrimaryBright,
                    style = VType.Label,
                )
            }
        }
        services.forEach { service ->
            Surface(
                Modifier.fillMaxWidth(),
                color = PdigV2Colors.Surface,
                shape = RoundedCornerShape(VRadius.Md),
                border = androidx.compose.foundation.BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
            ) {
                Row(Modifier.padding(VSpacing.Lg), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(service.name, color = PdigV2Colors.TextPrimary, style = VType.Body, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                        Text(roleLabel(service.kind), color = PdigV2Colors.TextMuted, style = VType.Meta)
                    }
                    LabelChip(when (service.kind) {
                        "twoFA" -> "2FA 验证"
                        "authenticates" -> "登录验证"
                        else -> "注册使用"
                    })
                }
            }
        }
        SectionHeader("风险")
        if (number.recoveryOnly) {
            Surface(color = PdigV2Colors.Warning.copy(alpha = 0.14f), shape = RoundedCornerShape(VRadius.Md), modifier = Modifier.fillMaxWidth()) {
                Text(
                    "此号码是 2 个账户的唯一恢复路径：更换/注销前必须先建立新的恢复方式。",
                    Modifier.padding(VSpacing.Lg),
                    color = PdigV2Colors.TextPrimary,
                    style = VType.Secondary,
                )
            }
        } else {
            Text("未发现该号码承担唯一恢复路径。", color = PdigV2Colors.TextSecondary, style = VType.Secondary)
        }
        SectionHeader("备用路径")
        Text(
            "该号码的登录用途存在其他验证渠道（备用路径全部来自已确认依赖；未知 = 未知）。",
            color = PdigV2Colors.TextSecondary,
            style = VType.Secondary,
        )
        SectionHeader("历史")
        Text("2026-08 更新运营商资料；2026-03 加入 2FA 用途。", color = PdigV2Colors.TextMuted, style = VType.Meta)
    }
}

private fun roleLabel(kind: String): String = when (kind) {
    "twoFA" -> "二次验证"
    "authenticates" -> "登录依据"
    else -> "注册/验证"
}
