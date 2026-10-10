package com.pdig.uivnext.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
            "没有选择更换计划",
            "请从“变更”一级页选择一个已记录的变更计划。",
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
            "变更计划不存在或暂时无法读取",
            "不会用演示计划替代真实记录。",
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
                    "${productionScenarioLabel(current.scenario)} · ${productionWorkflowStateLabel(current.workflowState)}",
                    color = PdigV2Colors.TextMuted,
                    fontSize = 11.sp,
                )
            }
        }

        item {
            ProductionChangeChoreography(current)
        }

        item {
            ChangePlanTruthStrip(current)
        }

        item {
            ChangePlanAuthorityState(current)
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
                            "还有 ${current.unresolvedMustChangeKeys.size} 项必须处理的影响尚未解决；完成这些事项前不会把计划显示为可安全继续。",
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
                "每次操作后都会重新读取权威计划状态；界面按钮不会自行把动作标记为已完成或已验证。"
            )
        }
    }
}

/**
 * Current is an authoritative plan read; Transition is action progress; After is
 * explicitly an uncommitted expectation. No synthetic after-graph is manufactured.
 */
internal data class ProductionChangeStageSummary(
    val current: String,
    val transition: String,
    val after: String,
)

internal fun productionChangeStageSummary(plan: VNextProductionPlan): ProductionChangeStageSummary {
    val verified = plan.actions.count { it.verificationStatus == "verified" }
    val finished = plan.actions.count { it.done }
    return ProductionChangeStageSummary(
        current = "当前图谱修订 ${plan.currentGraphRevision} · 分析基线 ${plan.lastAnalyzedGraphRevision}",
        transition = "${finished}/${plan.actions.size} 步已记录完成 · ${verified} 步已验证",
        after = if (plan.unresolvedMustChangeKeys.isNotEmpty())
            "${plan.unresolvedMustChangeKeys.size} 项必须处理事项未解决 · 仅计划预期"
        else
            "计划预期 · 完成并验证前不代表现实",
    )
}

@Composable
private fun ProductionChangeChoreography(plan: VNextProductionPlan) {
    val summary = productionChangeStageSummary(plan)
    Column(
        Modifier.fillMaxWidth().testTag("pdig.production-vnext.change.choreography"),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text(
            "变更全程",
            color = PdigV2Colors.TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
        val stages = listOf(
            "当前" to summary.current,
            "过渡中" to summary.transition,
            "完成后（计划）" to summary.after,
        )
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth < 600.dp) {
                // Reference: phone uses a readable guided journey, not three
                // tiny 110dp cards with 9sp authority/truth text.
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    stages.forEachIndexed { index, (title, detail) ->
                        ProductionChangeStageCard(index, title, detail, Modifier.fillMaxWidth(), true)
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    stages.forEachIndexed { index, (title, detail) ->
                        ProductionChangeStageCard(index, title, detail, Modifier.weight(1f), false)
                    }
                }
            }
        }
        Text(
            "完成后是 Plan Projection，不是 Reality。记录完成 ≠ 验证完成；旧路径仍需按正式计划安全退休。",
            color = PdigV2Colors.TextMuted,
            fontSize = 10.sp,
        )
    }
}

@Composable
private fun ProductionChangeStageCard(
    index: Int,
    title: String,
    description: String,
    modifier: Modifier,
    phone: Boolean,
) {
    Surface(
        modifier = modifier.testTag("pdig.production-vnext.change.stage.$index"),
        color = if (index == 1) PdigV2Colors.PrimarySoft else PdigV2Colors.Surface,
        shape = RoundedCornerShape(VRadius.Lg),
        border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        Row(
            Modifier.padding(if (phone) 12.dp else 9.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Text(
                "${index + 1}",
                color = if (index == 1) PdigV2Colors.PrimaryText else PdigV2Colors.TextMuted,
                fontSize = if (phone) 14.sp else 11.sp,
                fontWeight = FontWeight.Bold,
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = PdigV2Colors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (phone) 13.sp else 11.sp)
                Text(description, color = PdigV2Colors.TextSecondary,
                    fontSize = if (phone) 11.sp else 10.sp)
            }
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
private fun ChangePlanAuthorityState(plan: VNextProductionPlan) {
    val stale = plan.effectiveState == "needs_revalidation" ||
        plan.currentGraphRevision != plan.lastAnalyzedGraphRevision
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (stale || plan.readiness == "blocked") {
            PdigV2Colors.Critical.copy(alpha = 0.07f)
        } else {
            PdigV2Colors.PrimarySoft.copy(alpha = 0.62f)
        },
        shape = RoundedCornerShape(VRadius.Lg),
    ) {
        Column(
            Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                "准备状态：${productionReadinessLabel(plan.readiness)}",
                color = PdigV2Colors.TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "分析时图谱修订 ${plan.lastAnalyzedGraphRevision} · 当前图谱修订 ${plan.currentGraphRevision}",
                color = PdigV2Colors.TextSecondary,
                fontSize = 10.sp,
            )
            if (stale) {
                Text(
                    "基础设施已发生变化；重新分析并确认当前影响范围前，不会继续记录新的执行完成。",
                    color = PdigV2Colors.Critical,
                    fontSize = 10.sp,
                )
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
    val verificationStatus = action.verificationStatus
    val verified = verificationStatus == "verified"
    val verificationPending = verificationStatus == "pending" ||
        verificationStatus == "evidence_suggested"
    val verificationNotRequired = verificationStatus == null ||
        verificationStatus == "not_required"
    val planExecutable = plan.readiness != "blocked" &&
        plan.effectiveState != "needs_revalidation"
    val canComplete = canMutate && planExecutable && !action.done && prerequisitesMet
    // Match the authoritative legacy plan UI: only pending/evidence_suggested
    // verification states expose confirmation. Failed/not-required are not
    // locally converted into a retry/success state.
    val canVerify = canMutate && action.done && verificationPending

    val stateLabel = when {
        verified -> "已验证"
        verificationStatus == "failed" -> "验证未通过"
        action.done && verificationPending -> "已完成 · 待验证"
        action.done && verificationNotRequired -> "已完成 · 无需验证"
        verificationPending -> "待验证"
        !prerequisitesMet -> "等待前置步骤"
        else -> "待执行"
    }

    val verifyLabel = when {
        verified -> "已验证"
        verificationStatus == "failed" -> "验证未通过"
        verificationNotRequired -> "无需验证"
        verificationPending -> "确认验证"
        else -> "验证"
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
                    Text(productionActionPhaseLabel(action.phase),
                        color = PdigV2Colors.TextMuted, fontSize = 9.sp)
                }
                Text(
                    stateLabel,
                    color = if (verified) PdigV2Colors.PrimaryText else PdigV2Colors.TextSecondary,
                    fontSize = 9.sp,
                )
            }

            if (action.prerequisiteActionIds.isNotEmpty()) {
                val prerequisiteTitles = action.prerequisiteActionIds.map { id ->
                    actionsById[id]?.title ?: "未识别前置步骤"
                }
                Text(
                    "前置：${prerequisiteTitles.joinToString("、")}",
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
                    label = verifyLabel,
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
