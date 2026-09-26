package com.pdig.desktop

import com.pdig.app.data.ScenarioPlanRequest
import com.pdig.core.domain.ActionVerificationStatus as DomainVerificationStatus
import com.pdig.core.generated.NodeKind
import com.pdig.desktop.data.DesktopSession
import java.time.Instant

/**
 * `--smoke` 的变更计划场景驱动（自 SmokeRunner 拆出，保持职责单一）。
 *
 * 覆盖 v0.3.0 replace_phone_number 的 Make-Before-Break DAG 门禁：
 * 前置未完成时完成后续动作必须被拒绝（action_missing_prerequisite）。
 * 任何 step FAIL → 由调用方统一计入 failures。
 */
internal fun runSmokeScenario(scenarioId: String, session: DesktopSession, step: (String, () -> Unit) -> Unit) {
    step("scenario-$scenarioId") {
        val nodes = session.graph.nodes()
        val activeDeps = session.graph.dependencies().filter { it.state == "active" }
        val target = if (scenarioId == "replace_phone_number") {
            // v0.3.0：手机号场景的目标是身份锚点（identity_anchor）；无锚点时先创建一个
            val existing = nodes.firstOrNull { it.kind == NodeKind.IDENTITY_ANCHOR.wire }
            if (existing != null) {
                existing
            } else {
                val now = Instant.now().toString()
                session.driver.exec(
                    "INSERT INTO nodes (id, kind, name, archived, fields_json, owner, created_at, updated_at) " +
                        "VALUES ('smoke-phone-anchor','identity_anchor','旧手机号',0,'{}','self','$now','$now')",
                )
                checkNotNull(session.graph.nodes().firstOrNull { it.id == "smoke-phone-anchor" })
            }
        } else {
            activeDeps.firstOrNull { d -> nodes.any { it.id == d.from } }
                ?.let { d -> nodes.first { it.id == d.from } }
                ?: nodes.first { it.kind == NodeKind.PAYMENT_INSTRUMENT.wire }
        }
        if (scenarioId != "replace_phone_number") {
            // 确保 target 至少一条 required 支付依赖 → impact 必产生 must_change（unknown→required 只能由用户路径设置）
            val targetActive = activeDeps.filter { it.from == target.id }
            if (targetActive.isNotEmpty()) {
                val dep = targetActive.first()
                if (dep.criticality != "required") {
                    session.graph.setDependencyCriticality(dep.id, required = true)
                }
            }
        }
        val planId = session.plans.createPlanForScenario(
            ScenarioPlanRequest(scenarioId = scenarioId, targetNodeId = target.id, effectiveDate = null),
        )
        val detail = checkNotNull(session.plans.planDetail(planId)) { "planDetail null" }
        check(detail.actions.isNotEmpty()) { "plan produced no actions" }
        if (scenarioId == "replace_phone_number") {
            // Make-Before-Break DAG gate：前置未完成时完成后续动作必须被拒绝（action_missing_prerequisite）
            val sequential = listOf("rpn-prepare-1", "rpn-change-1", "rpn-verify-1", "rpn-change-2")
            val actionIds = detail.actions.map { it.id }
            check(actionIds.containsAll(sequential)) { "replace_phone_number plan missing DAG actions" }
            for (i in 0 until sequential.size - 1) {
                val cur = sequential[i]
                val next = sequential[i + 1]
                // 前置未完成 → 完成 next 必须被拒绝
                val blocked = try {
                    session.plans.completeAction(planId, next)
                    false
                } catch (e: Throwable) {
                    e.message?.contains("action_missing_prerequisite") == true
                }
                check(blocked) { "make-before-break gate not enforced at $next (before $cur done)" }
                session.plans.completeAction(planId, cur)
            }
            session.plans.completeAction(planId, sequential.last())
            val done = checkNotNull(session.plans.planDetail(planId))
            check(done.actions.all { it.done }) { "replace_phone_number DAG not fully completable" }
        } else {
            // 完成每个 CHANGE action；done ≠ verified 断言
            for (action in detail.actions) {
                if (action.resolvesImpactKeys.isNotEmpty()) {
                    session.plans.completeAction(planId, action.id)
                    val after = checkNotNull(session.plans.planDetail(planId))
                    val updated = after.actions.first { it.id == action.id }
                    check(updated.done) { "action not marked done" }
                    check(updated.verification?.status != DomainVerificationStatus.VERIFIED) {
                        "done must NOT imply verified"
                    }
                }
            }
            // 可验证动作置 VERIFIED（人工确认路径）
            val d2 = checkNotNull(session.plans.planDetail(planId))
            for (action in d2.actions) {
                val v = action.verification
                if (action.done && v != null && v.status != DomainVerificationStatus.NOT_REQUIRED) {
                    session.plans.verifyAction(planId, action.id)
                }
            }
        }
        val d3 = checkNotNull(session.plans.planDetail(planId))
        check(d3.workflowState != com.pdig.core.generated.ChangePlanWorkflowState.DRAFT) { "plan did not leave draft" }
    }
}