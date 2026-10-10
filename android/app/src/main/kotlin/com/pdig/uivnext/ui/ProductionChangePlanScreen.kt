package com.pdig.uivnext.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.pdig.uivnext.production.VNextProductionPlan
import com.pdig.uivnext.production.VNextProductionPlanAction
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius

/**
 * Authoritative production ChangePlan screen.
 *
 * Completion and verification are different commands. The screen never changes
 * an action locally; it replaces visible state only with the plan returned by
 * the AppContainer-backed production gateway.
 */
@Composable
internal fun ProductionChangePlanScreen(
    session: ProductionVNextSession,
    planId: String?,
    modifier: Modifier = Modifier,
) {
    if (planId.isNullOrBlank()) {
        ChangePlanUnavailable(
            "没有选择生产 ChangePlan",
            "请从“变更”一级页选择一个已记录计划。",
            modifier,
        )
        return
    }

    val gateway = session.authorities?.change
    var plan by remember(session, planId) {
        mutableStateOf(session.dataSource.productionPlan(planId))
    }
    var error by remember(session, planId) { mutableStateOf<String?>(null) }

    val current = plan
    if (current == null) {
        ChangePlanUnavailable(
            "ChangePlan 不存在或无法读取",
            "不会回退到 Synthetic Reference 计划。",
            modifier,
        )
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("pdig.production-vnext.change-plan"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    current.title,
                    color = PdigV2Colors.TextPrimary,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "${current.scenario} · ${current.workflowState}",
                    color = PdigV2Colors.TextMuted,
                    fontSize = 11.sp,
                )
            }
        }

        item {
            ChangePlanTruthStrip(current)
        }

        if (current.unresolvedMustChangeKeys.isNotEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = PdigV2Colors.Critical.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(VRadius.Lg),
                ) {
                    Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "仍有必须处理项",
                            color = PdigV2Colors.Critical,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            current.unresolvedMustChangeKeys.joinToString("、"),
                            color = PdigV2Colors.TextSecondary,
                            fontSize = 10.sp,
                        )
                    }
                }
            }
        }

        error?.let { message ->
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = PdigV2Colors.Critical.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(VRadius.Md),
                ) {
                    Text(
                        message,
                        Modifier.padding(12.dp),
                        color = PdigV2Colors.Critical,
                        fontSize = 11.sp,
                    )
                }
            }
        }

        item {
            Text(
                "执行步骤",
                color = PdigV2Colors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        items(current.actions, key = { it.id }) { action ->
            ProductionActionCard(
                action = action,
                plan = current,
                canMutate = gateway != null,
                onComplete = {
                    error = null
                    try {
                        plan = gateway?.completeAction(current.id, action.id)
                            ?: current
                    } catch (t: Throwable) {
                        error = "动作未能记录完成；界面没有本地伪造状态。"
                    }
                },
                onVerify = {
                    error = null
                    try {
                        plan = gateway?.verifyAction(current.id, action.id)
                            ?: current
                    } catch (t: Throwable) {
                        error = "验证未完成；done 与 verified 仍保持分离。"
                    }
                },
            )
        }

        item {
            ChangeBoundaryNote(
                "每次操作后都重新读取 planDetail。按钮状态只是交互约束，真正 authority 仍在 AppContainer / ChangePlan engine。"
            )
        }
    }
}

@Composable
private fun ChangePlanTruthStrip(plan: VNextProductionPlan) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(
            plan.affectedServiceCount to "受影响服务",
            plan.actions.count { it.done } to "已记录完成",
            plan.actions.count { it.verificationStatus == "verified" } to "已验证",
        ).forEach { (value, label) ->
            Surface(
                modifier = Modifier.weight(1f),
                color = PdigV2Colors.Surface,
                shape = RoundedCornerShape(VRadius.Md),
                border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
            ) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(value.toString(), color = PdigV2Colors.TextPrimary, fontSize = 17.sp,
                        fontWeight = FontWeight.Bold)
                    Text(label, color = PdigV2Colors.TextMuted, fontSize = 9.sp)
                }
            }
        }
    }
}

@Composable
private fun ProductionActionCard(
    action: VNextProductionPlanAction,
    plan: VNextProductionPlan,
    canMutate: Boolean,
    onComplete: () -> Unit,
    onVerify: () -> Unit,
) {
    val actionsById = plan.actions.associateBy { it.id }
    val prerequisitesMet = action.prerequisiteActionIds.all { id ->
        actionsById[id]?.done == true
    }
    val verified = action.verificationStatus == "verified"
    val canComplete = canMutate && !action.done && prerequisitesMet
    val canVerify = canMutate && action.done && !verified

    val stateLabel = when {
        verified -> "已验证"
        action.verificationStatus == "failed" -> "验证失败"
        action.verificationStatus == "pending" ||
            action.verificationStatus == "evidence_suggested" -> "待验证"
        action.done -> "已记录完成 · 尚未验证"
        !prerequisitesMet -> "等待前置步骤"
        else -> "待执行"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PdigV2Colors.Surface,
        shape = RoundedCornerShape(VRadius.Lg),
        border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        Column(
            Modifier.padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(action.title, color = PdigV2Colors.TextPrimary, fontSize = 12.sp,
                        fontWeight = FontWeight.Bold)
                    Text(action.phase, color = PdigV2Colors.TextMuted, fontSize = 9.sp)
                }
                Text(
                    stateLabel,
                    color = if (verified) PdigV2Colors.PrimaryText else PdigV2Colors.TextSecondary,
                    fontSize = 9.sp,
                )
            }

            if (action.prerequisiteActionIds.isNotEmpty()) {
                Text(
                    "前置：${action.prerequisiteActionIds.joinToString("、")}",
                    color = PdigV2Colors.TextMuted,
                    fontSize = 9.sp,
                )
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChangeActionButton(
                    label = if (action.done) "已记录完成" else "记录完成",
                    enabled = canComplete,
                    onClick = onComplete,
                    modifier = Modifier.weight(1f),
                )
                ChangeActionButton(
                    label = if (verified) "已验证" else "验证",
                    enabled = canVerify,
                    onClick = onVerify,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ChangeActionButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Surface(
        modifier = modifier
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .testTag("pdig.production-vnext.change.action"),
        color = if (enabled) PdigV2Colors.PrimarySoft else PdigV2Colors.SurfaceRaised,
        shape = RoundedCornerShape(VRadius.Md),
    ) {
        Text(
            label,
            Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            color = if (enabled) PdigV2Colors.PrimaryText else PdigV2Colors.TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ChangeBoundaryNote(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PdigV2Colors.PrimarySoft.copy(alpha = 0.65f),
        shape = RoundedCornerShape(VRadius.Lg),
    ) {
        Text(text, Modifier.padding(12.dp), color = PdigV2Colors.TextSecondary, fontSize = 10.sp)
    }
}

@Composable
private fun ChangePlanUnavailable(
    title: String,
    body: String,
    modifier: Modifier,
) {
    Column(
        modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, color = PdigV2Colors.TextPrimary, fontSize = 18.sp,
            fontWeight = FontWeight.Bold)
        Spacer(Modifier.padding(4.dp))
        Text(body, color = PdigV2Colors.TextSecondary, fontSize = 12.sp)
    }
}
