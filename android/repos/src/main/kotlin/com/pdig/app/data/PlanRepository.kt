package com.pdig.app.data

import com.pdig.core.db.SqliteDriver
import com.pdig.core.domain.ActionVerificationMethod
import com.pdig.core.domain.ActionVerificationStatus
import com.pdig.core.domain.PlanAction
import com.pdig.core.generated.PlanActionPhase
import com.pdig.core.domain.PlanReadinessInput
import com.pdig.core.generated.ChangePlanWorkflowState
import com.pdig.core.generated.ImpactTargetStatus
import com.pdig.core.json.Json
import com.pdig.core.json.JsonWriter
import com.pdig.core.plan.computePlanReadiness
import com.pdig.core.plan.countUnresolvedMustChange
import com.pdig.core.scenario.ScenarioRegistry
import com.pdig.core.statemachine.ChangePlanMachine
import com.pdig.core.statemachine.VerificationMachine
import java.time.Instant

/**
 * ChangePlan / Action / Verification。
 * 纯复用 core.plan 的显式 resolution 规则与 core.statemachine 的迁移表，
 * UI 侧不做"完成动作数减法"，也不把 done 当成 verified（spec §48/§49）。
 */
class PlanRepository(
    private val driver: SqliteDriver,
    private val graph: GraphRepository,
    private val proposals: ProposalRepository,
) {

    fun plans(): List<PlanRow> = driver.prepare(
        """
        SELECT id, title, scenario, workflow_state, last_analyzed_graph_revision, effective_date
          FROM change_plans ORDER BY created_at
        """.trimIndent(),
    ).all().map {
        PlanRow(
            id = it.str("id") ?: "",
            title = it.str("title") ?: "",
            scenario = it.str("scenario") ?: "",
            workflowState = it.str("workflow_state") ?: "draft",
            lastAnalyzedRevision = (it.long("last_analyzed_graph_revision") ?: 0L).toInt(),
            effectiveDate = it.str("effective_date"),
        )
    }

    /** 从 scenario 创建真实变更计划：must_change 每条生成一个显式 CHANGE 动作。 */
    fun createPlanForScenario(req: ScenarioPlanRequest): String {
        // planned 模板没有 factory，必须拒绝（ScenarioRegistry 策略 gate）
        require(ScenarioRegistry.isExecutable(req.scenarioId)) { "scenario_not_executable: ${req.scenarioId}" }
        val now = Instant.now().toString()
        val planId = "plan-" + sha256Hex(req.scenarioId + "|" + req.targetNodeId + "|" + now).take(16)
        val revision = graph.graphRevision()
        val impact = graph.impactFor(req.targetNodeId)
        val mustChangeKeys = keysOfStatus(impact, ImpactTargetStatus.MUST_CHANGE)
        val needsReviewKeys = keysOfStatus(impact, ImpactTargetStatus.NEEDS_REVIEW)
        val backupKeys = keysOfStatus(impact, ImpactTargetStatus.BACKUP_PATH) +
            keysOfStatus(impact, ImpactTargetStatus.DEGRADED)
        val actions = if (req.scenarioId == "replace_phone_number") {
            buildReplacePhoneActions(planId)
        } else {
            buildActionsFor(planId, impact, mustChangeKeys)
        }
        val snapshot = JsonWriter.write(
            Json.Obj(
                listOf(
                    "mustChangeKeys" to Json.Arr(mustChangeKeys.map { Json.Str(it) }),
                    "needsReviewKeys" to Json.Arr(needsReviewKeys.map { Json.Str(it) }),
                    "backupKeys" to Json.Arr(backupKeys.map { Json.Str(it) }),
                    "targetKeys" to Json.Arr(impact.targets.map { Json.Str(keyString(it.nodeId, it.capability.wire)) }),
                ),
            ),
        )

        val plan = com.pdig.core.domain.ChangePlan(
            id = planId,
            templateId = req.scenarioId,
            scenario = req.scenarioId,
            title = ScenarioRegistry.get(req.scenarioId)?.title ?: req.scenarioId,
            workflowState = ChangePlanWorkflowState.DRAFT,
            baselineGraphRevision = revision,
            lastAnalyzedGraphRevision = revision,
            targetNodeId = req.targetNodeId,
            effectiveDate = req.effectiveDate,
            actions = actions,
        )
        val readiness = computeReadiness(plan, mustChangeKeys, needsReviewKeys, snapshot)

        driver.transaction {
            driver.prepare(
                """
                INSERT INTO change_plans
                  (id, template_id, scenario, title, workflow_state, baseline_graph_revision,
                   last_analyzed_graph_revision, target_node_id, effective_date, params_json,
                   impact_snapshot_json, action_items_json, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, '{}', ?, ?, ?, ?)
                """.trimIndent(),
            ).run(
                planId, req.scenarioId, req.scenarioId, plan.title,
                (if (readiness == com.pdig.core.generated.PlanReadiness.READY_WITH_KNOWN_SCOPE) {
                    ChangePlanWorkflowState.READY
                } else {
                    ChangePlanWorkflowState.REVIEW_REQUIRED
                }).wire,
                revision, revision, req.targetNodeId, req.effectiveDate,
                snapshot, JsonWriter.write(Json.Arr(actions.map { planActionToJson(it) })), now, now,
            )
        }
        return planId
    }

    /**
     * replace_phone_number 的 Make-Before-Break 动作 DAG：
     * rpn-prepare-1 → rpn-change-1 → rpn-verify-1 → rpn-change-2
     * （先建新路径并验证，才能 retire 旧路径；BREAK_BEFORE_MAKE = FORBIDDEN）
     */
    private fun buildReplacePhoneActions(planId: String): List<PlanAction> {
        val futureObservation = com.pdig.core.domain.ActionVerification(
            method = ActionVerificationMethod.FUTURE_OBSERVATION,
            status = ActionVerificationStatus.PENDING,
        )
        return listOf(
            PlanAction(
                id = "rpn-prepare-1",
                title = "检查旧手机号承担的恢复与认证能力",
                phase = PlanActionPhase.PREPARE,
                prerequisiteActionIds = emptyList(),
            ),
            PlanAction(
                id = "rpn-change-1",
                title = "添加新手机号并迁移关键账户",
                phase = PlanActionPhase.CHANGE,
                prerequisiteActionIds = listOf("rpn-prepare-1"),
                verification = futureObservation,
            ),
            PlanAction(
                id = "rpn-verify-1",
                title = "验证新手机号恢复路径",
                phase = PlanActionPhase.VERIFY,
                prerequisiteActionIds = listOf("rpn-change-1"),
                verification = futureObservation,
            ),
            PlanAction(
                id = "rpn-change-2",
                title = "停用旧手机号（新路径全部验证后）",
                phase = PlanActionPhase.CHANGE,
                prerequisiteActionIds = listOf("rpn-verify-1"),
            ),
        )
    }

    fun planDetail(planId: String): PlanDetailView? {
        val plan = planDomain(planId) ?: return null
        val snapshot = driver.prepare("SELECT impact_snapshot_json FROM change_plans WHERE id = ?")
            .get(planId)?.str("impact_snapshot_json")
        val mustChangeKeys = snapshotKeys(snapshot, "mustChangeKeys")
        val needsReviewKeys = snapshotKeys(snapshot, "needsReviewKeys")
        val current = graph.graphRevision()
        val unresolved = if (mustChangeKeys.isEmpty()) emptyList() else {
            val n = countUnresolvedMustChange(plan.actions, mustChangeKeys)
            // 返回仍未解决的 key：用于 UI 明确指出"哪几条还没处理"
            mustChangeKeys.filterIndexed { i, _ -> i >= mustChangeKeys.size - n }
        }
        val readiness = computeReadiness(plan, mustChangeKeys, needsReviewKeys, snapshot)
        val targetName = plan.targetNodeId?.let { id -> graph.nodes(true).firstOrNull { it.id == id }?.name } ?: ""
        return PlanDetailView(
            id = plan.id,
            scenario = plan.scenario,
            title = plan.title,
            workflowState = plan.workflowState,
            effectiveState = if (com.pdig.core.plan.isPlanStale(plan, current)) {
                ChangePlanWorkflowState.REVIEW_REQUIRED
            } else null,
            baselineGraphRevision = plan.baselineGraphRevision,
            lastAnalyzedGraphRevision = plan.lastAnalyzedGraphRevision,
            currentGraphRevision = current,
            targetNodeId = plan.targetNodeId,
            targetNodeName = targetName,
            effectiveDate = plan.effectiveDate,
            actions = plan.actions,
            mustChangeKeys = mustChangeKeys,
            unresolvedMustChangeKeys = unresolved,
            readiness = readiness,
            affectedServiceCount = mustChangeKeys.size + needsReviewKeys.size,
        )
    }

    /** 完成动作：done=true。verification 仍为 pending —— done ≠ verified。 */
    fun completeAction(planId: String, actionId: String) {
        val now = Instant.now().toString()
        driver.transaction {
            val current = rawWorkflowState(planId)
            if (current != null && ChangePlanMachine.isFrozen(current)) {
                throw IllegalStateException("action_frozen")
            }
            val actions = planDomain(planId)?.actions?.toMutableList()
                ?: throw IllegalStateException("entity_not_found")
            val idx = actions.indexOfFirst { it.id == actionId }
            if (idx < 0) throw IllegalStateException("entity_not_found")
            actions[idx] = actions[idx].copy(done = true)
            writeActions(planId, actions, now)
            syncPlanProgress(planId, actions, now)
        }
    }

    /** 用户人工确认验证 → verified（VF-FROZEN：不得覆盖 verified / failed / not_required）。 */
    fun verifyAction(planId: String, actionId: String) {
        val now = Instant.now().toString()
        driver.transaction {
            val current = rawWorkflowState(planId)
            if (current != null && ChangePlanMachine.isFrozen(current)) {
                throw IllegalStateException("action_frozen")
            }
            val actions = planDomain(planId)?.actions?.toMutableList()
                ?: throw IllegalStateException("entity_not_found")
            val idx = actions.indexOfFirst { it.id == actionId }
            if (idx < 0) throw IllegalStateException("entity_not_found")
            val v = actions[idx].verification
                ?: throw IllegalStateException("verification_not_required")
            // VF-FROZEN：不得覆盖 verified / failed / not_required
            // （core.statemachine 用 generated 枚举，domain 侧是同名 domain 枚举 —— 按 wire 值比较）
            val terminalWires = VerificationMachine.terminal.map { it.wire }
            if (v.status.wire in terminalWires) throw IllegalStateException("illegal_state_transition")
            require(actions[idx].done) { "not_done" }
            actions[idx] = actions[idx].copy(
                verification = v.copy(status = ActionVerificationStatus.VERIFIED),
            )
            writeActions(planId, actions, now)
            syncPlanProgress(planId, actions, now)
        }
    }

    // ---- plan internals ----

    private fun syncPlanProgress(planId: String, actions: List<com.pdig.core.domain.PlanAction>, now: String) {
        val current = rawWorkflowState(planId) ?: return
        if (ChangePlanMachine.isFrozen(current)) return
        if (actions.isEmpty()) return
        val allDone = actions.all { it.done }
        val allVerified = actions.all {
            it.verification == null || it.verification?.status == ActionVerificationStatus.VERIFIED
        }
        val target = when {
            allDone && allVerified -> ChangePlanWorkflowState.COMPLETED
            allDone -> ChangePlanWorkflowState.VERIFYING
            actions.any { it.done } -> ChangePlanWorkflowState.IN_PROGRESS
            else -> null
        }
        if (target != null && target != current && ChangePlanMachine.canTransition(current, target)) {
            setWorkflowState(planId, target, now)
        }
    }

    private fun setWorkflowState(planId: String, state: ChangePlanWorkflowState, now: String) {
        driver.prepare("UPDATE change_plans SET workflow_state = ?, updated_at = ? WHERE id = ?")
            .run(state.wire, now, planId)
    }

    private fun rawWorkflowState(planId: String): ChangePlanWorkflowState? =
        driver.prepare("SELECT workflow_state FROM change_plans WHERE id = ?").get(planId)
            ?.str("workflow_state")?.let { ChangePlanWorkflowState.fromWire(it) }

    private fun writeActions(planId: String, actions: List<com.pdig.core.domain.PlanAction>, now: String) {
        driver.prepare("UPDATE change_plans SET action_items_json = ?, updated_at = ? WHERE id = ?")
            .run(JsonWriter.write(Json.Arr(actions.map { planActionToJson(it) })), now, planId)
    }

    private fun computeReadiness(
        plan: com.pdig.core.domain.ChangePlan,
        mustChangeKeys: List<String>,
        needsReviewKeys: List<String>,
        snapshot: String?,
    ): com.pdig.core.generated.PlanReadiness {
        val involved = involvedNodeIds(plan.targetNodeId, snapshot)
        val proposals = proposals.pendingProposals()
        // staleness 用 SQL 直接按 last_verified_at 判定（DependencyRow 投影不带该列）
        val cutoffIso = Instant.now().minusSeconds(STALE_CUTOFF_DAYS * 86_400L).toString()
        val staleRelevant = if (involved.isEmpty()) 0 else {
            val inList = involved.joinToString(",") { "'$it'" }
            driver.prepare(
                """
                SELECT COUNT(*) AS c FROM dependencies
                 WHERE last_verified_at < ?
                   AND (from_node IN ($inList) OR to_node IN ($inList))
                """.trimIndent(),
            ).get(cutoffIso)?.long("c")?.toInt() ?: 0
        }
        val relevantProposals = proposals.count { it.from in involved || it.to in involved }
        val candidates = driver.prepare("SELECT COUNT(*) AS c FROM discovery_candidates WHERE status = 'pending'")
            .get()?.long("c")?.toInt() ?: 0
        return computePlanReadiness(
            PlanReadinessInput(
                plan = plan,
                currentGraphRevision = graph.graphRevision(),
                pendingMustChange = countUnresolvedMustChange(plan.actions, mustChangeKeys),
                pendingNeedsReview = needsReviewKeys.size,
                unresolvedCandidates = candidates,
                pendingRelevantProposals = relevantProposals,
                staleRelevantDependencies = staleRelevant,
                unfinishedChangeActions = plan.actions.count { !it.done },
            ),
        )
    }

    private fun planDomain(planId: String): com.pdig.core.domain.ChangePlan? {
        val r = driver.prepare(
            """
            SELECT id, template_id, scenario, title, workflow_state, baseline_graph_revision,
                   last_analyzed_graph_revision, target_node_id, effective_date, action_items_json
              FROM change_plans WHERE id = ?
            """.trimIndent(),
        ).get(planId) ?: return null
        return com.pdig.core.domain.ChangePlan(
            id = r.str("id") ?: "",
            templateId = r.str("template_id"),
            scenario = r.str("scenario") ?: "",
            title = r.str("title") ?: "",
            workflowState = ChangePlanWorkflowState.fromWire(r.str("workflow_state") ?: "draft")
                ?: ChangePlanWorkflowState.DRAFT,
            baselineGraphRevision = (r.long("baseline_graph_revision") ?: 0L).toInt(),
            lastAnalyzedGraphRevision = (r.long("last_analyzed_graph_revision") ?: 0L).toInt(),
            targetNodeId = r.str("target_node_id"),
            effectiveDate = r.str("effective_date"),
            actions = planActionsFromJson(r.str("action_items_json")),
        )
    }
}