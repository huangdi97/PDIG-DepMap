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
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.production.ProductionVNextSession
import com.pdig.uivnext.model.VScreen
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
            ProductionChangeChoreography(current, targetMasked)
        }

        item {
            ChangePlanTruthStrip(current)
        }

        item {
            ChangePlanAuthorityState(
                plan = current,
                canReanalyze = gateway != null,
                onReanalyze = {
                    error = null
                    try {
                        plan = gateway!!.reanalyzePlan(current.id)
                    } catch (_: Throwable) {
                        error = "重新分析未完成；计划仍保持阻断状态。"
                    }
                },
            )
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

        val unboundMustChangeKeys = productionUnboundMustChangeKeys(current)
        if (unboundMustChangeKeys.isNotEmpty()) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { session.appState.navigate(VScreen.CHANGE) }
                        .testTag("pdig.production-vnext.change.replan-required"),
                    color = PdigV2Colors.Critical.copy(alpha = 0.06f),
                    shape = RoundedCornerShape(VRadius.Lg),
                    border = BorderStroke(1.dp, PdigV2Colors.Critical.copy(alpha = 0.22f)),
                ) {
                    Column(
                        Modifier.padding(13.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            "需要重新规划",
                            color = PdigV2Colors.Critical,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "当前计划没有步骤覆盖 ${unboundMustChangeKeys.size} 项新发现的必须处理影响。系统不会把停用旧路径当作解决；请返回变更页重新建立覆盖当前影响范围的计划。",
                            color = PdigV2Colors.TextSecondary,
                            fontSize = 10.sp,
                        )
                        Text(
                            "返回变更页 →",
                            color = PdigV2Colors.PrimaryText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
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
                                error = "验证未完成；操作完成与独立验证仍保持分离。"
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
    // section. Unresolved/replan/masking banners are conditional.
    return 5 +
        (if (plan.unresolvedMustChangeKeys.isNotEmpty()) 1 else 0) +
        (if (productionUnboundMustChangeKeys(plan).isNotEmpty()) 1 else 0) +
        (if (targetMasked) 1 else 0) +
        position
}

internal data class ProductionChangeStageSummary(
    val current: String,
    val transition: String,
    val after: String,
)

internal fun productionUnboundMustChangeKeys(plan: VNextProductionPlan): List<String> {
    val claimed = plan.actions
        .filter { it.phase == "change" }
        .flatMap { it.resolvesImpactKeys }
        .toSet()
    return plan.unresolvedMustChangeKeys.filterNot { it in claimed }.distinct().sorted()
}

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

    if (action.done && action.verificationStatus in setOf("verified", "failed", "not_required")) {
        return when (action.verificationStatus) {
            "verified" -> "本步骤已完成并验证；可继续查看后续步骤。"
            "failed" -> "验证未通过；请核对证据与正式计划，不会自动重试或标记通过。"
            else -> "动作已记录完成；当前不需要额外验证。"
        }
    }

    if (!productionPlanFreshForMutation(plan)) {
        return "基础设施已变化；必须重新分析当前影响范围后再继续。"
    }

    if (action.done) {
        return when (action.verificationStatus) {
            "pending", "evidence_suggested" ->
                "动作已记录完成，结果尚未验证。请核对实际证据后再确认验证。"
            else -> "动作已记录完成；当前不需要额外验证。"
        }
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
    if (!productionPhoneRetirementVerificationSatisfied(plan, action)) {
        return "新手机号恢复路径尚未验证；验证完成前不能停用旧手机号。"
    }
    if (
        plan.scenario == "replace_phone_number" &&
        action.id == "rpn-change-2" &&
        plan.retirementScopeReady != true
    ) {
        return "当前影响范围仍需处理或核对；达到已知范围可继续的状态前不能停用旧手机号。"
    }

    if (plan.readiness == "blocked") {
        val claimedBlockers = action.resolvesImpactKeys
            .count { it in plan.unresolvedMustChangeKeys }
        return when {
            action.phase == "prepare" ->
                "这是解除阻断前的准备步骤；完成它不会自动把影响项标记为已处理或已验证。"
            action.phase == "verify" ->
                "仍有其他影响待处理，但可以继续验证已经建立的路径；验证不会解除未绑定的迁移事项。"
            action.phase == "change" && claimedBlockers > 0 ->
                "这一步用于处理 $claimedBlockers 项阻断事项；记录完成不等于结果已验证。"
            else ->
                "仍有阻断事项；请先完成用于解除阻断的准备或处理步骤。"
        }
    }
    return "可记录这一步的完成情况；记录完成不等于结果已验证。"
}

internal enum class ProductionChangeStage {
    CURRENT,
    TRANSITION,
    AFTER,
}

internal data class ProductionChangeImpactRow(
    val key: String,
    val nodeName: String?,
    val capability: String,
    val currentState: String,
    val transitionState: String,
    val afterState: String,
)

internal fun productionChangeImpactRows(plan: VNextProductionPlan): List<ProductionChangeImpactRow> {
    val unresolved = plan.unresolvedMustChangeKeys.toSet()
    val priority = mapOf(
        "must_change" to 0,
        "needs_review" to 1,
        "backup_or_degraded" to 2,
        "recorded" to 3,
    )
    return plan.impactItems
        .sortedWith(
            compareBy({ priority[it.status] ?: 9 }, { it.nodeName ?: "" }, { it.key }),
        )
        .map { item ->
            val claimants = plan.actions.filter {
                it.phase == "change" && item.key in it.resolvesImpactKeys
            }
            val current = when (item.status) {
                "must_change" -> "必须处理"
                "needs_review" -> "需要核对"
                "backup_or_degraded" -> "备用 / 降级路径"
                else -> "已记录影响"
            }
            val claimantVerificationFailed = claimants.any {
                it.done && it.verificationStatus == "failed"
            }
            val claimantVerificationPending = claimants.any {
                it.done && it.verificationStatus in setOf("pending", "evidence_suggested")
            }
            val transition = when (item.status) {
                "must_change" -> when {
                    claimants.isEmpty() -> "未绑定处理步骤"
                    claimants.any { !it.done } -> "待处理"
                    claimantVerificationFailed -> "处理已记录 · 验证未通过"
                    claimantVerificationPending -> "处理已记录 · 待验证"
                    else -> "处理已记录"
                }
                "needs_review" -> "等待核对"
                "backup_or_degraded" -> "保持现有路径"
                else -> "随计划观察"
            }
            val after = when (item.status) {
                "must_change" -> when {
                    item.key in unresolved -> "计划仍未解决"
                    claimantVerificationFailed -> "计划验证未通过"
                    claimantVerificationPending -> "计划仍待验证"
                    else -> "计划已处理"
                }
                "needs_review" -> "计划仍需核对"
                "backup_or_degraded" -> "计划保留该路径"
                else -> "计划保持记录"
            }
            ProductionChangeImpactRow(
                key = item.key,
                nodeName = item.nodeName,
                capability = item.capability,
                currentState = current,
                transitionState = transition,
                afterState = after,
            )
        }
}

internal fun productionChangeImpactState(
    row: ProductionChangeImpactRow,
    stage: ProductionChangeStage,
): String = when (stage) {
    ProductionChangeStage.CURRENT -> row.currentState
    ProductionChangeStage.TRANSITION -> row.transitionState
    ProductionChangeStage.AFTER -> row.afterState
}

internal fun productionChangeStageSummary(plan: VNextProductionPlan): ProductionChangeStageSummary {
    val verified = plan.actions.count { it.verificationStatus == "verified" }
    val finished = plan.actions.count { it.done }
    val impactCount = plan.impactItems.size.takeIf { it > 0 } ?: plan.targetKeys.size
    val currentCoverage = buildList {
        if (impactCount > 0) add("${impactCount} 项影响")
        if (plan.mustChangeKeys.isNotEmpty()) add("${plan.mustChangeKeys.size} 项必须处理")
        if (plan.needsReviewKeys.isNotEmpty()) add("${plan.needsReviewKeys.size} 项需核对")
    }.joinToString(" · ")
    return ProductionChangeStageSummary(
        current = buildString {
            append("当前数据版本 ${plan.currentGraphRevision} · 分析基准 ${plan.lastAnalyzedGraphRevision}")
            if (currentCoverage.isNotBlank()) append(" · ").append(currentCoverage)
        },
        transition = "${finished}/${plan.actions.size} 步已记录完成 · ${verified} 步已验证",
        after = if (plan.unresolvedMustChangeKeys.isNotEmpty())
            "${plan.unresolvedMustChangeKeys.size} 项必须处理事项未解决 · 仅计划预期"
        else
            "计划预期 · 完成并验证前不代表现实",
    )
}

@Composable
internal fun ProductionChangeChoreography(
    plan: VNextProductionPlan,
    privacyMask: Boolean = false,
) {
    val summary = productionChangeStageSummary(plan)
    var selectedStage by remember(plan.id) { mutableStateOf(ProductionChangeStage.CURRENT) }
    Column(
        Modifier.fillMaxWidth().testTag("pdig.production-vnext.change.choreography"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "变更全程",
            color = PdigV2Colors.TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
        val stages = listOf(
            Triple(ProductionChangeStage.CURRENT, "当前", summary.current),
            Triple(ProductionChangeStage.TRANSITION, "过渡中", summary.transition),
            Triple(ProductionChangeStage.AFTER, "完成后（计划）", summary.after),
        )
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth < 600.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    stages.forEachIndexed { index, (stage, title, detail) ->
                        ProductionChangeStageCard(
                            index = index,
                            title = title,
                            description = detail,
                            modifier = Modifier.fillMaxWidth(),
                            phone = true,
                            selected = selectedStage == stage,
                            onClick = { selectedStage = stage },
                        )
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    stages.forEachIndexed { index, (stage, title, detail) ->
                        ProductionChangeStageCard(
                            index = index,
                            title = title,
                            description = detail,
                            modifier = Modifier.weight(1f),
                            phone = false,
                            selected = selectedStage == stage,
                            onClick = { selectedStage = stage },
                        )
                    }
                }
            }
        }

        ProductionChangeImpactStageList(
            plan = plan,
            stage = selectedStage,
            privacyMask = privacyMask,
        )

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
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .selectable(selected = selected, onClick = onClick, role = Role.Tab)
            .testTag("pdig.production-vnext.change.stage.$index"),
        color = if (selected) PdigV2Colors.PrimarySoft else PdigV2Colors.Surface,
        shape = RoundedCornerShape(VRadius.Lg),
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) PdigV2Colors.PrimaryBright else PdigV2Colors.BorderSubtle,
        ),
    ) {
        Row(
            Modifier.padding(if (phone) 12.dp else 9.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Text(
                "${index + 1}",
                color = if (selected) PdigV2Colors.PrimaryText else PdigV2Colors.TextMuted,
                fontSize = if (phone) 14.sp else 11.sp,
                fontWeight = FontWeight.Bold,
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    title,
                    color = PdigV2Colors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (phone) 13.sp else 11.sp,
                )
                Text(
                    description,
                    color = PdigV2Colors.TextSecondary,
                    fontSize = if (phone) 11.sp else 10.sp,
                )
            }
        }
    }
}

@Composable
private fun ProductionChangeImpactStageList(
    plan: VNextProductionPlan,
    stage: ProductionChangeStage,
    privacyMask: Boolean,
) {
    val rows = productionChangeImpactRows(plan)
    val stageTitle = when (stage) {
        ProductionChangeStage.CURRENT -> "当前已记录影响"
        ProductionChangeStage.TRANSITION -> "过渡中的处理状态"
        ProductionChangeStage.AFTER -> "完成后计划投影"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pdig.production-vnext.change.impact-stage"),
        color = PdigV2Colors.Surface,
        shape = RoundedCornerShape(VRadius.Lg),
        border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        Column(
            Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Text(
                stageTitle,
                color = PdigV2Colors.TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
            if (rows.isEmpty()) {
                Text(
                    "当前计划没有保存逐项影响记录；这不表示没有影响，也不会据此推断安全。",
                    color = PdigV2Colors.TextMuted,
                    fontSize = 10.sp,
                )
            } else {
                rows.forEachIndexed { index, row ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .testTag("pdig.production-vnext.change.impact-row.$index"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(
                            Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                if (privacyMask) {
                                    "受影响对象（已遮蔽）"
                                } else {
                                    row.nodeName?.takeIf { it.isNotBlank() } ?: "已记录对象"
                                },
                                color = PdigV2Colors.TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                productionCapabilityLabel(row.capability),
                                color = PdigV2Colors.TextMuted,
                                fontSize = 10.sp,
                            )
                        }
                        Text(
                            productionChangeImpactState(row, stage),
                            color = if (
                                productionChangeImpactState(row, stage).contains("未解决") ||
                                productionChangeImpactState(row, stage).contains("必须处理")
                            ) {
                                PdigV2Colors.Critical
                            } else {
                                PdigV2Colors.TextSecondary
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
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
internal fun ChangePlanAuthorityState(
    plan: VNextProductionPlan,
    canReanalyze: Boolean,
    onReanalyze: () -> Unit,
) {
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
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .then(
                            if (canReanalyze) {
                                Modifier
                                    .clickable(onClick = onReanalyze)
                                    .testTag("pdig.production-vnext.change.reanalyze")
                            } else Modifier
                        ),
                    color = if (canReanalyze) PdigV2Colors.PrimarySoft else PdigV2Colors.SurfaceRaised,
                    shape = RoundedCornerShape(VRadius.Md),
                ) {
                    Text(
                        if (canReanalyze) "重新分析当前影响" else "当前无法重新分析",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                        color = if (canReanalyze) PdigV2Colors.PrimaryText else PdigV2Colors.TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
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
internal fun productionPlanFreshForMutation(plan: VNextProductionPlan): Boolean =
    plan.effectiveState != "needs_revalidation" &&
        plan.currentGraphRevision == plan.lastAnalyzedGraphRevision

internal fun productionPhoneRetirementVerificationSatisfied(
    plan: VNextProductionPlan,
    action: VNextProductionPlanAction,
): Boolean {
    if (plan.scenario != "replace_phone_number" || action.id != "rpn-change-2") return true
    val verification = plan.actions.firstOrNull { it.id == "rpn-verify-1" } ?: return false
    return verification.done && verification.verificationStatus == "verified"
}

/**
 * Completion is action-gated rather than globally blocked by plan readiness.
 * A blocked plan exists precisely because must-change work remains: PREPARE and
 * CHANGE actions that explicitly claim current blockers must remain usable.
 */
internal fun productionPlanCompletionEnabled(
    plan: VNextProductionPlan,
    action: VNextProductionPlanAction,
    canMutate: Boolean,
): Boolean {
    if (!canMutate || action.done || !productionPlanFreshForMutation(plan)) return false
    if (action.prerequisiteActionIds.any { id ->
            plan.actions.firstOrNull { it.id == id }?.done != true
        }
    ) return false
    if (!productionPhoneRetirementVerificationSatisfied(plan, action)) return false
    if (
        plan.scenario == "replace_phone_number" &&
        action.id == "rpn-change-2" &&
        plan.retirementScopeReady != true
    ) return false
    if (plan.readiness != "blocked") return true
    if (action.phase == "prepare" || action.phase == "verify") return true
    return action.phase == "change" &&
        action.resolvesImpactKeys.any { it in plan.unresolvedMustChangeKeys }
}

/**
 * Verification confirms an already recorded action. Other unresolved
 * must-change work is not a reason to suppress verification of this step;
 * staleness and authority still fail closed.
 */
internal fun productionPlanVerificationEnabled(
    plan: VNextProductionPlan,
    action: VNextProductionPlanAction,
    canMutate: Boolean,
): Boolean = canMutate && productionPlanFreshForMutation(plan) && action.done &&
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
    val canComplete = productionPlanCompletionEnabled(plan, action, canMutate)
    // Match the authoritative legacy plan UI: only pending/evidence_suggested
    // verification states expose confirmation. Failed/not-required are not
    // locally converted into a retry/success state.
    // Verification is a Reality mutation too: stale plans still fail closed.
    // A merely BLOCKED plan may verify an already-established path; unresolved
    // migration work remains blocked independently and retirement stays gated.
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
                color = if (
                    (!action.done && !canComplete) ||
                    (action.done && verificationPending && !canVerify)
                ) {
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
