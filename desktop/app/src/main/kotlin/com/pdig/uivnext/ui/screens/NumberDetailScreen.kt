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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pdig.uivnext.demo.UiVNextDemoFixture
import com.pdig.uivnext.model.UiVNextService
import com.pdig.uivnext.model.VTestIds
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import com.pdig.uivnext.theme.VSpacing
import com.pdig.uivnext.theme.VType
import com.pdig.uivnext.ui.VAppState
import com.pdig.uivnext.ui.components.LabelChip
import com.pdig.uivnext.ui.components.NumberIdentitySurface
import com.pdig.uivnext.ui.components.SectionHeader

/**
 * Number Detail（PHASE 1D §22–24）：先修 P0 布局回归，再按新布局重排。
 *
 * 顶部三栏（按 §24）：LEFT 36% Number Identity Surface /
 * CENTER 32% 用途·角色·运营商·SIM·状态 / RIGHT 32% 恢复能力·关联账户·薄弱点。
 * 下方：关联服务 / 登录·2FA·恢复依赖 / 历史。避免巨大横幅；
 * 禁止 LocalGlow(fillMaxSize) 包在 Row 内导致单字符竖排 / intrinsic-width collapse。
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
        PageHeader(
            title = "号码详情",
            subtitle = "${number.nickname} · 全球通信身份（不是支付卡）",
            trailing = {
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
            },
        )
        // 顶部三栏：identity 36% / summary 32% / recovery 32%（weight 固定比例，杜绝压缩）
        Row(
            Modifier
                .fillMaxWidth()
                .testTagLocal(VTestIds.NUMBER_DETAIL_LAYOUT),
            horizontalArrangement = Arrangement.spacedBy(VSpacing.Xxl),
            verticalAlignment = Alignment.Top,
        ) {
            Box(Modifier.weight(0.36f).testTagLocal(VTestIds.NUMBER_DETAIL_IDENTITY)) {
                NumberIdentitySurface(number = number, privacyMask = app.privacyMask)
            }
            SummaryColumn(
                number = number,
                services = services,
                modifier = Modifier.weight(0.32f).testTagLocal(VTestIds.NUMBER_DETAIL_SUMMARY),
            )
            RecoveryColumn(
                number = number,
                services = services,
                modifier = Modifier.weight(0.32f).testTagLocal(VTestIds.NUMBER_DETAIL_RECOVERY),
            )
        }
        // 下方：关联服务 / 登录·2FA·恢复依赖 / 历史
        SectionHeader("关联服务（${services.size}）")
        services.forEach { service ->
            ServiceRow(service)
        }
        SectionHeader("登录 · 2FA · 恢复依赖")
        val authServices = services.filter { it.kind == "authenticates" || it.kind == "twoFA" }
        if (authServices.isEmpty()) {
            Text("该号码未登记登录 / 2FA 依赖。", color = PdigV2Colors.TextSecondary, style = VType.Secondary)
        } else {
            authServices.forEach { service ->
                ServiceRow(service, chip = if (service.kind == "twoFA") "2FA 验证" else "登录验证")
            }
        }
        SectionHeader("历史")
        Text("2026-08 更新运营商资料；2026-03 加入 2FA 用途。", color = PdigV2Colors.TextMuted, style = VType.Meta)
    }
}

/** CENTER：用途 / 角色 / 运营商 / SIM / 状态（横向 label-value，不竖排）。 */
@Composable
private fun SummaryColumn(
    number: com.pdig.uivnext.model.UiVNextNumber,
    services: List<UiVNextService>,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(VSpacing.Sm)) {
        SectionHeader("状态 · 角色 · 用途")
        SummaryRow("状态", statusLabelZh(number.status))
        SummaryRow("角色", roleLabelOf(number.role))
        SummaryRow("运营商", number.carrier)
        SummaryRow("SIM", if (number.simKind == "eSIM") "eSIM" else "实体 SIM")
        SummaryRow("用途", number.usages.joinToString(" · "))
        SummaryRow("关联服务", "${services.size} 项")
        if (number.recoveryOnly) {
            Surface(color = PdigV2Colors.Warning.copy(alpha = 0.14f), shape = RoundedCornerShape(VRadius.Md), modifier = Modifier.fillMaxWidth()) {
                Text(
                    "风险：此号码是 2 个账户的唯一恢复路径",
                    Modifier.padding(VSpacing.Md),
                    color = PdigV2Colors.Warning,
                    style = VType.Label,
                )
            }
        }
    }
}

/** RIGHT：恢复能力 / 关联账户 / 薄弱点。 */
@Composable
private fun RecoveryColumn(
    number: com.pdig.uivnext.model.UiVNextNumber,
    services: List<UiVNextService>,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(VSpacing.Sm)) {
        SectionHeader("恢复能力")
        RecoveryRow("唯一恢复路径", if (number.recoveryOnly) "存在（2 账户）" else "无")
        RecoveryRow("备用验证渠道", if (services.any { it.kind != "twoFA" }) "存在" else "未登记")
        RecoveryRow("关联账户", "${services.count { it.kind == "authenticates" }} 个登录账户")
        RecoveryRow("薄弱点", if (number.recoveryOnly) "高风险" else "低")
        Surface(color = PdigV2Colors.Surface.copy(alpha = 0.9f), shape = RoundedCornerShape(VRadius.Md), modifier = Modifier.fillMaxWidth()) {
            Text(
                "备用路径全部来自已确认依赖；未知 = 未知。",
                Modifier.padding(VSpacing.Lg),
                color = PdigV2Colors.TextSecondary,
                style = VType.Secondary,
            )
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = PdigV2Colors.TextMuted, style = VType.Meta, maxLines = 1)
        Spacer(Modifier.width(VSpacing.Md))
        Text(value, color = PdigV2Colors.TextPrimary, style = VType.Secondary, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@Composable
private fun RecoveryRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = PdigV2Colors.TextMuted, style = VType.Meta, maxLines = 1)
        Spacer(Modifier.width(VSpacing.Md))
        Text(value, color = PdigV2Colors.TextPrimary, style = VType.Secondary, maxLines = 1)
    }
}

@Composable
private fun ServiceRow(service: UiVNextService, chip: String? = null) {
    Surface(
        Modifier.fillMaxWidth(),
        color = PdigV2Colors.Surface,
        shape = RoundedCornerShape(VRadius.Md),
        border = androidx.compose.foundation.BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        Row(
            Modifier.padding(VSpacing.Lg),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(service.name, color = PdigV2Colors.TextPrimary, style = VType.Body, fontWeight = FontWeight.Medium, maxLines = 1)
                Text("地区 ${service.region} · ${service.kind}", color = PdigV2Colors.TextMuted, style = VType.Meta, maxLines = 1)
            }
            LabelChip(chip ?: when (service.kind) {
                "twoFA" -> "2FA 验证"
                "authenticates" -> "登录验证"
                else -> "注册使用"
            })
        }
    }
}

private fun roleLabelOf(role: String): String = when (role) {
    "primary" -> "主号"
    "secondary" -> "副号"
    "keep" -> "保号"
    else -> role
}

private fun statusLabelZh(status: String): String = when (status) {
    "active" -> "使用中"
    "expiring_soon" -> "即将到期"
    "expired" -> "已过期"
    else -> status
}
