package com.pdig.uivnext.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.production.ProductionVNextSession
import com.pdig.uivnext.production.VNextProductionPlanSummary
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius

/**
 * Existing plans can be inspected while privacy is on; creation requires a
 * visible target identity and a production authority. This UI gate never
 * replaces AppContainer target and eligibility validation.
 */
internal fun productionChangeEntryEnabled(
    hasExistingPlan: Boolean,
    authorityAvailable: Boolean,
    privacyMask: Boolean,
): Boolean = hasExistingPlan || (authorityAvailable && !privacyMask)

/**
 * Explicit production transition from a confirmed payment asset into the existing
 * replace_payment_card ChangePlan authority.
 */
@Composable
internal fun ProductionCardChangeEntry(
    session: ProductionVNextSession,
    cardId: String,
    plans: List<VNextProductionPlanSummary>,
    modifier: Modifier = Modifier,
) {
    val app = session.appState
    val gateway = session.authorities?.change
    var error by remember(cardId) { mutableStateOf<String?>(null) }

    val existing = plans
        .asSequence()
        .filter {
            it.scenario == "replace_payment_card" &&
                it.workflowState !in setOf("completed", "cancelled")
        }
        .mapNotNull { summary ->
            session.dataSource.productionPlan(summary.id)
                ?.takeIf { it.targetNodeId == cardId }
        }
        .firstOrNull()

    val canOpen = productionChangeEntryEnabled(
        hasExistingPlan = existing != null,
        authorityAvailable = gateway != null,
        privacyMask = app.privacyMask,
    )
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (canOpen) Modifier.clickable {
                    error = null
                    // An existing plan may be opened read-only while masked.
                    // Creating a new authoritative plan requires identifying its
                    // target; recheck the latest mask state inside the callback.
                    if (existing == null && app.privacyMask) {
                        error = "已开启敏感信息遮蔽。请先核对目标对象后再建立计划。"
                    } else try {
                        val plan = existing ?: gateway!!.createPlan(
                            scenarioId = "replace_payment_card",
                            targetNodeId = cardId,
                        )
                        app.openProductionPlan(plan.id, plan.scenario)
                    } catch (t: Throwable) {
                        error = "未能建立或读取更换计划；界面没有在本地伪造计划状态。"
                    }
                } else Modifier
            )
            .testTag("pdig.production-vnext.card.change-entry"),
        color = PdigV2Colors.Surface,
        shape = RoundedCornerShape(VRadius.Lg),
        border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        Column(
            Modifier.padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                if (existing != null) "继续更换此卡" else "建立更换此卡的计划",
                color = if (canOpen) PdigV2Colors.PrimaryText else PdigV2Colors.TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                if (existing != null) {
                    "已有未结束的更换支付卡计划；继续使用同一份已记录计划。"
                } else {
                    "显式建立更换计划；不会自动选择替代卡，也不会把影响分析结果当作已经执行。"
                },
                color = PdigV2Colors.TextSecondary,
                fontSize = 10.sp,
            )
            error?.let {
                Text(it, color = PdigV2Colors.Critical, fontSize = 10.sp)
            }
            if (!canOpen) {
                Text(
                    if (app.privacyMask && existing == null)
                        "敏感信息已遮蔽 · 请关闭遮蔽并核对目标后建立"
                    else
                        "Change authority 未绑定",
                    color = PdigV2Colors.TextMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.testTag("pdig.production-vnext.change.entry.masked"),
                )
            }
        }
    }
}


/**
 * Authoritative production transition from a governed PHONE_NUMBER identity into
 * the existing replace_phone_number ChangePlan authority.
 *
 * Caller must already have a VNextProductionSurfaceKind.PHONE_IDENTITY object;
 * PlanRepository independently re-validates the Canonical target subtype so UI
 * routing can never become the authority.
 */
@Composable
internal fun ProductionPhoneChangeEntry(
    session: ProductionVNextSession,
    phoneId: String,
    plans: List<VNextProductionPlanSummary>,
    modifier: Modifier = Modifier,
) {
    val app = session.appState
    val gateway = session.authorities?.change
    var error by remember(phoneId) { mutableStateOf<String?>(null) }

    val existing = plans
        .asSequence()
        .filter {
            it.scenario == "replace_phone_number" &&
                it.workflowState !in setOf("completed", "cancelled")
        }
        .mapNotNull { summary ->
            session.dataSource.productionPlan(summary.id)
                ?.takeIf { it.targetNodeId == phoneId }
        }
        .firstOrNull()

    val canOpen = productionChangeEntryEnabled(
        hasExistingPlan = existing != null,
        authorityAvailable = gateway != null,
        privacyMask = app.privacyMask,
    )
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (canOpen) Modifier.clickable {
                    error = null
                    // An existing plan may be opened read-only while masked.
                    // Creating a new authoritative plan requires identifying its
                    // target; recheck the latest mask state inside the callback.
                    if (existing == null && app.privacyMask) {
                        error = "已开启敏感信息遮蔽。请先核对目标对象后再建立计划。"
                    } else try {
                        val plan = existing ?: gateway!!.createPlan(
                            scenarioId = "replace_phone_number",
                            targetNodeId = phoneId,
                        )
                        app.openProductionPlan(plan.id, plan.scenario)
                    } catch (t: Throwable) {
                        error = "未能建立或读取手机号更换计划；界面没有在本地伪造 subtype 或计划状态。"
                    }
                } else Modifier
            )
            .testTag("pdig.production-vnext.phone.change-entry"),
        color = PdigV2Colors.Surface,
        shape = RoundedCornerShape(VRadius.Lg),
        border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        Column(
            Modifier.padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                if (existing != null) "继续更换此号码" else "分析并建立更换号码计划",
                color = if (canOpen) PdigV2Colors.PrimaryText else PdigV2Colors.TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                if (existing != null) {
                    "已有未结束的更换手机号计划；继续使用同一份已记录计划。"
                } else {
                    "计划 authority 会再次验证受治理 PHONE_NUMBER subtype；不会因为进入号码页就自动把对象当作手机号。"
                },
                color = PdigV2Colors.TextSecondary,
                fontSize = 10.sp,
            )
            error?.let {
                Text(it, color = PdigV2Colors.Critical, fontSize = 10.sp)
            }
            if (!canOpen) {
                Text(
                    if (app.privacyMask && existing == null)
                        "敏感信息已遮蔽 · 请关闭遮蔽并核对目标后建立"
                    else
                        "Change authority 未绑定",
                    color = PdigV2Colors.TextMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.testTag("pdig.production-vnext.change.entry.masked"),
                )
            }
        }
    }
}
