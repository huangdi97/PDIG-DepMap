package com.pdig.uivnext.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
    // A locally masked target can reveal itself through step names and
    // prerequisite descriptions even if global privacy is off.
    val targetMasked = productionChangeTargetMasked(
        session.appState, current.scenario, current.targetNodeId,
    )
    val requestedActionId = session.appState.selectedProductionActionId
    val focusedIndex = productionFocusedActionLazyIndex(current, requestedActionId, targetMasked)
    val listState = rememberLazyListState()
    LaunchedEffect(planId, requestedActionId) {
        // Navigation focus is UI-only. It scrolls to a still-authoritative
        // PlanAction and never calls completeAction / verifyAction.
        if (focusedIndex != null) listState.scrollToItem(focusedIndex)
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize().testTag("pdig.production-vnext.change-plan"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    productionVisiblePlanTitle(current.title, targetMasked),
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

        if (targetMasked) {
            item {
                ChangeBoundaryNote(
                    "敏感信息遮蔽已开启。步骤身份和前置对象不可核对，因此暂不能记录完成或确认验证。请先到「我」关闭遮蔽并核对实际证据。"
                )
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
        if (requestedActionId != null && focusedIndex == null) {
            item {
                ChangeBoundaryNote("此前选择的步骤已不在当前正式计划中；请重新核对当前步骤。")
            }
        }

        items(current.actions, key = { it.id }) { action ->
            ProductionActionCard(
                action = action,
                plan = current,
                canMutate = productionChangeActionMutationEnabled(
                    authorityAvailable = gateway != null,
                    privacyMask = targetMasked,
                ),
                privacyMask = targetMasked,
                focusedFromRecords = requestedActionId == action.id,
                onComplete = {
                    error = null
                    if (targetMasked) {
                        error = "敏感信息遮蔽已开启，请核对步骤内容后再记录完成。"
                    } else {
                        // The plan may have changed since this Composable drew its
                        // button. Re-read authoritative Reality immediately before
                        // committing. A stale click is never an execution permit.
                        val latest = session.dataSource.productionPlan(current.id)
                        val liveStep = latest?.actions?.firstOrNull { it.id == action.id }
                        when {
                            latest == null -> error = "正式计划暂不可读取；不会记录完成。"
                            productionChangeTargetMasked(
                                session.appState, latest.scenario, latest.targetNodeId,
                            ) -> {
                                plan = latest
                                error = "目标对象已被遮蔽；请先核对最新对象身份。"
                            }
                            liveStep == null -> {
                                plan = latest
                                error = "步骤已发生变化，请核对最新计划后再操作。"
                            }
                            !productionPlanCompletionEnabled(
                                latest, liveStep, gateway != null,
                            ) -> {
                                plan = latest
                                error = "计划状态或前置步骤已变化，暂不能记录完成。"
                            }
                            else -> try {
                                plan = gateway!!.completeAction(latest.id, liveStep.id)
                            } catch (t: Throwable) {
                                error = "动作未能记录完成；界面没有本地伪造状态。"
                            }
                        }
                    }
                },
                onVerify = {
                    error = null
                    if (targetMasked) {
                        error = "敏感信息遮蔽已开启，请查看并确认实际验证证据。"
                    } else {
                        val latest = session.dataSource.productionPlan(current.id)
                        val liveStep = latest?.actions?.firstOrNull { it.id == action.id }
                        when {
                            latest == null -> error = "正式计划暂不可读取；不会确认验证。"
                            productionChangeTargetMasked(
                                session.appState, latest.scenario, latest.targetNodeId,
                            ) -> {
                                plan = latest
                                error = "目标对象已被遮蔽；请先核对最新对象身份。"
                            }
                            liveStep == null -> {
                                plan = latest
                                error = "步骤已发生变化，请核对最新计划后再验证。"
                            }
                            !productionPlanVerificationEnabled(
                                latest, liveStep, gateway != null,
                            ) -> {
                                plan = latest
                                error = "计划或验证资格已变化，请核对最新状态后再确认。"
                            }
                            else -> try {
                                plan = gateway!!.verifyAction(latest.id, liveStep.id)
                            } catch (t: Throwable) {
                                error = "验证未完成；done 与 verified 仍保持分离。"
                            }
                        }
                    }
                },
            )
        }

        item {
            ChangeBoundaryNote(
                "每次操作后都会重新读取正式计划状态；界面按钮不会自行把动作标记为已完成或已验证。"
            )
        }
    }
}

/**
 * Current is an authoritative plan read; Transition is action progress; After is
 * explicitly an uncommitted expectation. No synthetic after-graph is manufactured.
 */
/**
 * Index of the requested live action in the LazyColumn at initial navigation.
 * No stale record action can be focused or treated as an execution permit.
 */
internal fun productionFocusedActionLazyIndex(
    plan: VNextProductionPlan,
    actionId: String?,
    targetMasked: Boolean,
): Int? {
    if (actionId.isNullOrBlank()) return null
    val position = plan.actions.indexOfFirst { it.id == actionId }
    if (position < 0) return null
    // Title + stage choreography + truth strip + authority banner + action
    // section. An unresolved-impact banner and a masking note are optional.
    return 5 + (if (plan.unresolvedMustChangeKeys.isNotEmpty()) 1 else 0) +
        (if (targetMasked) 1 else 0) + position
}

internal data class ProductionChangeStageSummary(
    val current: String,
    val transition: String,
    val after: String,
)

/**
 * Consumer next-step explanation, derived exclusively from the authoritative
 * plan. Disabled controls must never leave their blocker unexplained.
 * This function neither predicts domain eligibility nor performs an action.
 */
/** Matches the same graph-freshness condition as the authoritative plan banner. */
internal fun productionPlanExecutable(plan: VNextProductionPlan): Boolean =
    plan.readiness != "blocked" &&
        plan.effectiveState != "needs_revalidation" &&
        plan.currentGraphRevision == plan.lastAnalyzedGraphRevision

internal fun productionPlanActionGuidance(
    action: VNextProductionPlanAction,
    plan: VNextProductionPlan,
    canMutate: Boolean,
    privacyMask: Boolean = false,
): String {
    if (!canMutate) return "当前只有查看权限，操作需要正式变更权限。"
    if (action.done) return when (action.verificationStatus) {
        "verified" -> "本步骤已完成并验证；可继续查看后续步骤。"
        "pending", "evidence_suggested" ->
            "动作已记录完成，结果尚未验证。请核对实际证据后再确认验证。"
        "failed" -> "验证未通过；请核对证据与正式计划，不会自动重试或标记通过。"
        else -> "动作已记录完成；当前不需要额外验证。"
    }
    val actionsById = plan.actions.associateBy { it.id }
    val pending = action.prerequisiteActionIds.filter { actionsById[it]?.done != true }
    if (pending.isNotEmpty()) {
        val names = pending.map {
            actionsById[it]?.title?.let { title ->
                productionVisibleActionTitle(title, privacyMask)
            } ?: "未识别的前置步骤"
        }
        return "必须先完成：${names.joinToString("、")}。"
    }
    if (plan.effectiveState == "needs_revalidation" ||
        plan.currentGraphRevision != plan.lastAnalyzedGraphRevision
    ) return "基础设施已变化；必须重新核对影响范围，不能继续记录完成。"
    if (plan.readiness == "blocked") {
        return "仍有阻断事项，必须先处理并确认计划准备状态。"
    }
    return "可记录这一步的完成情况；记录完成不等于结果已验证。"
}

internal fun productionChangeStageSummary(plan: VNextProductionPlan): ProductionChangeStageSummary {
    val verified = plan.actions.count { it.verificationStatus == "verified" }
    val finished = plan.actions.count { it.done }
    return ProductionChangeStageSummary(
        current = "当前数据版本 ${plan.currentGraphRevision} · 分析基准 ${plan.lastAnalyzedGraphRevision}",
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
            "完成后只是计划预期，不是已经发生的事实。记录完成不等于验证完成；旧路径仍需按正式计划安全停用。",
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
                "分析时数据版本 ${plan.lastAnalyzedGraphRevision} · 当前数据版本 ${plan.currentGraphRevision}",
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

/**
 * Masked ChangePlan action titles hide identity-bearing instructions.
 * A user must be able to inspect each step before any authoritative mutation.
 * This is a presentation gate, not a substitute for AppContainer eligibility.
 */
internal fun productionChangeActionMutationEnabled(
    authorityAvailable: Boolean,
    privacyMask: Boolean,
): Boolean = authorityAvailable && !privacyMask

/**
 * Verification is an authoritative write, not a read-only annotation.
 * Neither an invalidated graph revision nor a blocked plan can advertise it.
 */
/** A freshly read step must still be executable before recording completion. */
internal fun productionPlanCompletionEnabled(
    plan: VNextProductionPlan,
    action: VNextProductionPlanAction,
    canMutate: Boolean,
): Boolean = canMutate && productionPlanExecutable(plan) && !action.done &&
    action.prerequisiteActionIds.all { id ->
        plan.actions.firstOrNull { it.id == id }?.done == true
    }

internal fun productionPlanVerificationEnabled(
    plan: VNextProductionPlan,
    action: VNextProductionPlanAction,
    canMutate: Boolean,
): Boolean = canMutate && productionPlanExecutable(plan) && action.done &&
    action.verificationStatus in setOf("pending", "evidence_suggested")

@Composable
private fun ProductionActionCard(
    action: VNextProductionPlanAction,
    plan: VNextProductionPlan,
    canMutate: Boolean,
    privacyMask: Boolean,
    focusedFromRecords: Boolean,
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
    val planExecutable = productionPlanExecutable(plan)
    val canComplete = productionPlanCompletionEnabled(plan, action, canMutate)
    // Match the authoritative legacy plan UI: only pending/evidence_suggested
    // verification states expose confirmation. Failed/not-required are not
    // locally converted into a retry/success state.
    // Verification is a Reality mutation too: a stale/blocked plan must not
    // advertise authoritative action buttons merely because a step was done.
    val canVerify = productionPlanVerificationEnabled(plan, action, canMutate)

    val guidance = if (privacyMask) {
        "当前步骤内容已遮蔽。请关闭遮蔽、核对对象及执行证据后再操作。"
    } else {
        productionPlanActionGuidance(action, plan, canMutate, privacyMask)
    }
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
        modifier = Modifier.fillMaxWidth()
            .testTag("pdig.production-vnext.change.action.${action.id}"),
        color = if (focusedFromRecords) PdigV2Colors.PrimarySoft else PdigV2Colors.Surface,
        shape = RoundedCornerShape(VRadius.Lg),
        border = BorderStroke(
            if (focusedFromRecords) 2.dp else 1.dp,
            if (focusedFromRecords) PdigV2Colors.PrimaryBright else PdigV2Colors.BorderSubtle,
        ),
    ) {
        Column(
            Modifier.padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (focusedFromRecords) {
                Text(
                    "从记录定位 · 请核对当前步骤",
                    color = PdigV2Colors.PrimaryText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(productionVisibleActionTitle(action.title, privacyMask), color = PdigV2Colors.TextPrimary, fontSize = 14.sp,
                        fontWeight = FontWeight.Bold)
                    Text(productionActionPhaseLabel(action.phase),
                        color = PdigV2Colors.TextSecondary, fontSize = 12.sp)
                }
                Text(
                    stateLabel,
                    color = if (verified) PdigV2Colors.PrimaryText else PdigV2Colors.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            if (action.prerequisiteActionIds.isNotEmpty()) {
                val prerequisiteTitles = action.prerequisiteActionIds.map { id ->
                    actionsById[id]?.title?.let {
                        productionVisibleActionTitle(it, privacyMask)
                    } ?: "未识别前置步骤"
                }
                Text(
                    "前置：${prerequisiteTitles.joinToString("、")}",
                    color = PdigV2Colors.TextMuted,
                    fontSize = 11.sp,
                )
            }

            Text(
                guidance,
                color = if (!action.done && (!prerequisitesMet || !planExecutable)) {
                    PdigV2Colors.Critical
                } else {
                    PdigV2Colors.TextSecondary
                },
                fontSize = 12.sp,
                lineHeight = 18.sp,
                modifier = Modifier.testTag("pdig.production-vnext.change.action-guidance"),
            )

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
            .defaultMinSize(minHeight = 48.dp)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .testTag("pdig.production-vnext.change.action"),
        color = if (enabled) PdigV2Colors.PrimarySoft else PdigV2Colors.SurfaceRaised,
        shape = RoundedCornerShape(VRadius.Md),
    ) {
        Text(
            label,
            Modifier.padding(horizontal = 10.dp, vertical = 14.dp),
            color = if (enabled) PdigV2Colors.PrimaryText else PdigV2Colors.TextMuted,
            fontSize = 12.sp,
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
