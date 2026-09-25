package com.pdig.desktop.ui

import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.pdig.desktop.ui.components.ChipTone
import com.pdig.desktop.ui.components.EmptyState
import com.pdig.desktop.ui.components.PdigCard
import com.pdig.desktop.ui.components.PdigPage
import com.pdig.desktop.ui.components.SectionDivider
import com.pdig.desktop.ui.components.StatusChip

/** 验证：所有计划的动作验证状态汇总（手动确认验证入口）。 */
@Composable
fun VerificationScreen(ui: UiState) {
    val plans = ui.session.plans.plans()
    PdigPage(
        title = "验证",
        subtitle = "done ≠ 已验证：验证动作必须得到用户的显式确认",
        notice = ui.notice,
        error = ui.error,
        onDismissNotice = { ui.notice = null },
        onDismissError = { ui.error = null },
    ) {
        Column {
            if (plans.isEmpty()) {
                EmptyState("还没有任何变更计划。")
            } else {
                plans.forEach { plan ->
                    SectionDivider("计划「${plan.title}」")
                    val detail = ui.session.plans.planDetail(plan.id)
                    if (detail == null || detail.actions.isEmpty()) {
                        EmptyState("该计划没有动作。")
                    } else {
                        detail.actions.forEach { a ->
                            val v = a.verification
                            if (v != null) {
                                val canVerify = canVerifyAction(a)
                                PdigCard(
                                    title = a.title,
                                    subtitle = listOfNotNull(
            "验证方式：${verificationMethodLabel(v.method.wire)}",
                                    ).joinToString(" · "),
                                    trailing = {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            StatusChip(verificationStatusLabel(v.status.wire), toneFor(v.status.wire))
                                            TextButton(
                                                onClick = { verifyPlanAction(ui, plan.id, a.id) },
                                                enabled = canVerify,
                                            ) { Text("手动确认验证") }
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun toneFor(statusWire: String): ChipTone = when (statusWire) {
    "verified" -> ChipTone.GOOD
    "failed" -> ChipTone.BAD
    "not_required" -> ChipTone.NEUTRAL
    else -> ChipTone.WARN
}