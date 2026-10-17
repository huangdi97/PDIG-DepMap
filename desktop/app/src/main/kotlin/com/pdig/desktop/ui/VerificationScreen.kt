package com.pdig.desktop.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pdig.core.domain.ActionVerificationStatus
import com.pdig.core.domain.PlanAction
import com.pdig.desktop.ui.components.ContinuityStep
import com.pdig.desktop.ui.components.EmptyState
import com.pdig.desktop.ui.components.PdigPage
import com.pdig.desktop.ui.components.PdigRow
import com.pdig.desktop.ui.components.RailAction
import com.pdig.desktop.ui.components.RailState
import com.pdig.desktop.ui.components.SectionHeader
import com.pdig.desktop.ui.theme.PdigDesktopTokens as T
import com.pdig.desktop.ui.theme.PdigStatusColors
import com.pdig.desktop.ui.theme.PdigType

/**
 * 验证（spec §36）：明确区分「动作已完成」≠「结果已验证」。
 * 状态：待验证 / 发现可能的证据 / 已验证 / 验证失败；verified 在视觉与语义上强于 completed。
 */
@Composable
fun VerificationScreen(ui: UiState) {
    val plans = ui.session.plans.plans()
    PdigPage(
        title = "验证",
        subtitle = "动作完成 ≠ 结果已验证：验证需要你的显式确认",
        notice = ui.notice,
        error = ui.error,
        onDismissNotice = { ui.notice = null },
        onDismissError = { ui.error = null },
    ) {
        Column(Modifier.fillMaxWidth()) {
            if (plans.isEmpty()) {
                EmptyState("还没有任何变更计划。")
            } else {
                plans.forEach { plan ->
                    val detail = ui.session.plans.planDetail(plan.id)
                    if (detail == null || detail.actions.isEmpty()) {
                        SectionHeader("计划「${plan.title}」")
                        EmptyState("该计划没有动作。")
                        return@forEach
                    }
                    SectionHeader("计划「${plan.title}」")
                    detail.actions.forEach { a ->
                        val v = a.verification
                        if (v != null) {
                            VerificationRow(ui, plan.id, a)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VerificationRow(ui: UiState, planId: String, a: PlanAction) {
    val v = a.verification ?: return
    val canVerify = canVerifyAction(a)
    val statusLabel = verificationStatusLabel(v.status.wire)
    val isVerified = v.status == ActionVerificationStatus.VERIFIED
    val isFailed = v.status == ActionVerificationStatus.FAILED
    PdigRow(
        title = a.title,
        subtitle = "验证方式：${verificationMethodLabel(v.method.wire)} · $statusLabel",
        leading = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    color = when {
                        isVerified -> MaterialTheme.colorScheme.secondaryContainer
                        isFailed -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.tertiaryContainer
                    },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(T.RadiusSm),
                ) {
                    Text(
                        statusLabel,
                        Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = PdigType.Label,
                        color = when {
                            isVerified -> MaterialTheme.colorScheme.secondary
                            isFailed -> MaterialTheme.colorScheme.error
                            else -> PdigStatusColors.verifying
                        },
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        },
        trailing = {
            RailAction("手动确认验证", enabled = canVerify, onClick = { verifyPlanAction(ui, planId, a.id) })
        },
        divider = true,
    )
}