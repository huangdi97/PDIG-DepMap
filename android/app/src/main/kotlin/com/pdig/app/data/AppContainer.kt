package com.pdig.app.data

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import com.pdig.app.platform.AndroidSqliteDriver
import com.pdig.app.security.DatabaseKeyStore
import com.pdig.core.crypto.DepmapContainer
import com.pdig.core.db.SqliteDriver
import com.pdig.core.schema.migrate
import com.pdig.core.serialize.checkGraphIntegrity
import com.pdig.core.serialize.exportGraph
import com.pdig.core.serialize.importGraph
import com.pdig.core.sources.MappingProfile
import com.pdig.core.sources.Observation
import com.pdig.core.sources.WechatParser
import com.pdig.core.sources.GenericCsvParser
import com.pdig.core.sources.OfxParser
import com.pdig.core.timeline.TimelineItem
import com.pdig.core.timeline.buildTimeline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.time.Instant

// ---- 领域层（纯 Kotlin，不含 Android 依赖）----
import com.pdig.core.domain.Dependency
import com.pdig.core.domain.DependencyGroup
import com.pdig.core.generated.CandidateStatus
import com.pdig.core.generated.DriftStatus
import com.pdig.core.domain.ImpactGraph
import com.pdig.core.domain.ImpactProposalInput
import com.pdig.core.domain.PlanReadinessInput
import com.pdig.core.domain.ActionVerificationStatus
import com.pdig.core.domain.ActionVerificationMethod
import com.pdig.core.domain.dependencyLogicalKey
import com.pdig.core.generated.Capability
import com.pdig.core.generated.ChangePlanWorkflowState
import com.pdig.core.generated.Criticality
import com.pdig.core.generated.DependencyState
import com.pdig.core.generated.DependencyOrigin
import com.pdig.core.generated.GroupMode
import com.pdig.core.generated.GroupState
import com.pdig.core.generated.ImpactTargetStatus
import com.pdig.core.generated.NodeKind
import com.pdig.core.generated.PlanActionPhase
import com.pdig.core.generated.Relation
import com.pdig.core.impact.ImpactResult
import com.pdig.core.impact.simulateScenario
import com.pdig.core.impact.simulateDisable
import com.pdig.core.domain.ImpactStateKey
import com.pdig.core.plan.computePlanReadiness
import com.pdig.core.plan.countUnresolvedMustChange
import com.pdig.core.scenario.ScenarioRegistry
import com.pdig.core.statemachine.ChangePlanMachine
import com.pdig.core.statemachine.VerificationMachine
import com.pdig.core.json.Json
import com.pdig.core.json.JsonParser
import com.pdig.core.json.JsonWriter

/**
 * 应用层装配（spec §56/§57）：Domain/Application 不依赖 Android；
 * 这里只做平台实现（SQLCipher / Keystore）与领域层的接线。
 */
class AppContainer private constructor(private val driver: SqliteDriver) {

    companion object {
        @Volatile
        private var instance: AppContainer? = null

        fun get(context: Context): AppContainer = instance ?: synchronized(this) {
            instance ?: run {
                val file = File(context.filesDir, "pdig.db")
                val driver = AndroidSqliteDriver.open(file, DatabaseKeyStore.passphrase(context))
                migrate(driver, Instant.now().toString())
                AppContainer(driver).also { instance = it }
            }
        }

        /**
         * 测试/取证用工厂：用调用方提供的 driver 构造（androidTest 在 app 模块内可访问）。
         * 生产路径只走 [get]。
         */
        internal fun forDriver(driver: SqliteDriver): AppContainer = AppContainer(driver)
    }
    // ------------------------------------------------------------------
    // 查询（只读投影）
    // ------------------------------------------------------------------

    fun nodes(includeArchived: Boolean = false): List<NodeRow> {
        val sql = if (includeArchived) {
            "SELECT id, kind, name, archived, fields_json FROM nodes ORDER BY name"
        } else {
            "SELECT id, kind, name, archived, fields_json FROM nodes WHERE archived = 0 ORDER BY name"
        }
        return driver.prepare(sql).all().map {
            NodeRow(
                id = it.str("id") ?: "",
                kind = it.str("kind") ?: "",
                name = it.str("name") ?: "",
                archived = (it.long("archived") ?: 0L) != 0L,
                fieldsJson = it.str("fields_json") ?: "{}",
            )
        }
    }

    fun dependencies(): List<DependencyRow> = driver.prepare(
        """
        SELECT d.id, d.from_node, d.relation, d.to_node, d.capability, d.criticality, d.state,
               f.name AS from_name, t.name AS to_name
          FROM dependencies d
          LEFT JOIN nodes f ON f.id = d.from_node
          LEFT JOIN nodes t ON t.id = d.to_node
         ORDER BY f.name, t.name
        """.trimIndent(),
    ).all().map {
        DependencyRow(
            id = it.str("id") ?: "",
            from = it.str("from_node") ?: "",
            fromName = it.str("from_name") ?: it.str("from_node") ?: "",
            relation = it.str("relation") ?: "",
            to = it.str("to_node") ?: "",
            toName = it.str("to_name") ?: it.str("to_node") ?: "",
            capability = it.str("capability") ?: "payment",
            criticality = it.str("criticality") ?: "unknown",
            state = it.str("state") ?: "active",
        )
    }

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

    fun openDrifts(): List<DriftRow> = driver.prepare(
        """
        SELECT id, kind, target_node_id, capability, candidate_from, candidate_relation,
               related_dependency_ids_json, observation_count, detected_at
          FROM reality_drifts WHERE status = 'open' ORDER BY detected_at DESC
        """.trimIndent(),
    ).all().map {
        DriftRow(
            id = it.str("id") ?: "",
            kind = it.str("kind") ?: "",
            targetNodeId = it.str("target_node_id") ?: "",
            capability = it.str("capability") ?: "payment",
            candidateFrom = it.str("candidate_from"),
            candidateRelation = it.str("candidate_relation") ?: "funding_source",
            relatedDependencyIds = parseStringList(it.str("related_dependency_ids_json")),
            observationCount = (it.long("observation_count") ?: 0L).toInt(),
            detectedAt = it.str("detected_at") ?: "",
        )
    }

    fun pendingCandidates(): List<CandidateRow> = driver.prepare(
        """
        SELECT id, candidate_kind, display_label, observation_count, status
          FROM discovery_candidates WHERE status = 'pending' ORDER BY observation_count DESC
        """.trimIndent(),
    ).all().map {
        CandidateRow(
            id = it.str("id") ?: "",
            candidateKind = it.str("candidate_kind") ?: "service",
            label = it.str("display_label") ?: "",
            observationCount = (it.long("observation_count") ?: 0L).toInt(),
            status = it.str("status") ?: "pending",
        )
    }


    fun sourceInstances(): List<SourceRow> = driver.prepare(
        "SELECT id, label, adapter_id, state, last_ingested_at FROM source_instances ORDER BY created_at",
    ).all().map {
        SourceRow(
            id = it.str("id") ?: "",
            label = it.str("label") ?: "",
            adapterId = it.str("adapter_id") ?: "",
            state = it.str("state") ?: "active",
            lastIngestedAt = it.str("last_ingested_at"),
        )
    }

    fun graphRevision(): Int =
        driver.prepare("SELECT value FROM meta WHERE key = 'graph_revision'").get()
            ?.str("value")?.toIntOrNull() ?: 0

    fun timeline(nowIso: String = Instant.now().toString()): List<TimelineItem> =
        buildTimeline(driver, nowIso)

    fun pendingProposals(): List<ProposalRow> = driver.prepare(
        """
        SELECT p.id, p.key, p.from_node, p.to_node, p.relation, p.capability,
               p.confidence_score, p.observation_count, p.decision
          FROM dependency_proposals p WHERE p.decision = 'pending'
         ORDER BY p.observation_count DESC
        """.trimIndent(),
    ).all().map {
        ProposalRow(
            id = it.str("id") ?: "",
            key = it.str("key") ?: "",
            from = it.str("from_node") ?: "",
            to = it.str("to_node") ?: "",
            relation = it.str("relation") ?: "",
            capability = it.str("capability") ?: "payment",
            confidence = it.double("confidence_score") ?: 0.0,
            observationCount = (it.long("observation_count") ?: 0L).toInt(),
        )
    }

    // ------------------------------------------------------------------
    // 导入（Observation 仅内存，绝不落库 —— spec §150）
    // ------------------------------------------------------------------

    suspend fun parseFile(bytes: ByteArray, adapterId: String, mapping: MappingProfile?): ParseOutcome =
        withContext(Dispatchers.Default) {
            val result = when (adapterId) {
                "wechat" -> WechatParser.parse(bytes)
                "generic_csv" -> GenericCsvParser.parse(bytes, mapping)
                "ofx_qfx" -> OfxParser.parse(bytes)
                else -> error("unsupported adapter: $adapterId")
            }
            ParseOutcome(result.observations, result.errors.map { "${it.line}: ${it.reason}" })
        }

    /** 用户确认后写入 Reality（与 graphRevision bump 同事务 —— spec §26/§27）。 */
    fun acceptProposal(proposalId: String) {
        driver.transaction {
            driver.prepare(
                "UPDATE dependency_proposals SET decision = 'accepted', decided_at = ? WHERE id = ?",
            ).run(Instant.now().toString(), proposalId)
            // 注意：v2/v3 的 dependency_proposals 没有 `criticality` 列（只有 `criticality_decision`）。
            // 原实现在这里 SELECT 了不存在的列，导致 acceptProposal 在真机上 100% 抛
            // `SQLiteException: no such column: criticality`（2026-09-15 设备实测）。
            // criticality 不参与写入（Reality 新增边固定为 unknown，需用户显式确认后才升级），
            // 因此这里不再查询该列。
            val row = driver.prepare(
                "SELECT from_node, relation, to_node, capability FROM dependency_proposals WHERE id = ?",
            ).get(proposalId)
            val from = row?.str("from_node") ?: return@transaction
            val to = row?.str("to_node") ?: return@transaction
            val relation = row?.str("relation") ?: "funding_source"
            val capability = row?.str("capability") ?: "payment"
            val now = Instant.now().toString()
            driver.prepare(
                """
                INSERT INTO dependencies (id, from_node, relation, to_node, capability, criticality, state,
                                          origin, confirmed_at, last_verified_at, evidence_refs_json,
                                          verification_basis_type, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, 'unknown', 'active', 'proposal', ?, ?, '[]', 'user_confirmed', ?, ?)
                ON CONFLICT (from_node, relation, to_node, capability) DO NOTHING
                """.trimIndent(),
            ).run("dep-$proposalId", from, relation, to, capability, now, now, now, now)
            bumpRevision()
        }
    }

    fun rejectProposal(proposalId: String) {
        driver.transaction {
            driver.prepare(
                "UPDATE dependency_proposals SET decision = 'rejected', decided_at = ? WHERE id = ?",
            ).run(Instant.now().toString(), proposalId)
    }
    }
    // ------------------------------------------------------------------
    // DiscoveryCandidate（H-16：发现 → Review → Confirm → Node / Dismiss / Later）
    // ------------------------------------------------------------------
    // 规范（spec/domain/domain.json + state-machines/state-machines.json）：
    //   - Candidate 本身不进入 Impact、不 bump graphRevision；
    //   - accept 幂等：replay 返回 created=false 与同一个 nodeId；
    //   - dismiss 不改 Reality，仅记录 dismissedAtObservationCount；
    //   - 未确认前不得进入 Impact（loadImpactGraph 只读 dependencies + pending proposals）。
    // ------------------------------------------------------------------

    /**
     * 用户接受候选对象 → 创建**一个** Node（candidate_kind → NodeKind），
     * 并把 candidate 标记为 accepted + 记录 nodeId。
     * 幂等：已 accepted 的 candidate 返回同一 nodeId，不重复建节点。
     * 注意：candidate_accept 在 GraphRevisionMachine.neverBumpsOn —— **不 bump**。
     */
    fun acceptCandidate(candidateId: String): String {
        val now = Instant.now().toString()
        return driver.transaction {
            val c = driver.prepare(
                "SELECT id, candidate_kind, display_label, status, accepted_node_id FROM discovery_candidates WHERE id = ?",
            ).get(candidateId)
                ?: throw IllegalStateException("entity_not_found")
            val status = c.str("status") ?: "pending"
            if (status == CandidateStatus.ACCEPTED.wire) {
                return@transaction c.str("accepted_node_id")
                    ?: throw IllegalStateException("illegal_state_transition")
            }
            if (status != CandidateStatus.PENDING.wire) {
                throw IllegalStateException("illegal_state_transition")
            }
            val kindWire = c.str("candidate_kind") ?: "service"
            val kind = NodeKind.fromWire(kindWire) ?: NodeKind.SERVICE
            val label = c.str("display_label") ?: ""
            val nodeId = nodeIdFor(kind, label)
            // candidate_accept ∈ GraphRevisionMachine.neverBumpsOn —— 创建 Node **不** bump。
            // 与 import Node Resolution 不同（那里 node_create ∈ bumpsOn，必须 bump）。
            insertNodeNoBump(nodeId, kind, label, now)
            driver.prepare(
                "UPDATE discovery_candidates SET status = ?, accepted_node_id = ?, updated_at = ? WHERE id = ?",
            ).run(CandidateStatus.ACCEPTED.wire, nodeId, now, candidateId)
            nodeId
        }
    }

    /** 只插入 Node，不 bump revision（candidate_accept 专用；幂等：已存在则跳过）。 */
    private fun insertNodeNoBump(id: String, kind: NodeKind, name: String, now: String) {
        driver.prepare(
            """
            INSERT OR IGNORE INTO nodes (id, kind, name, archived, fields_json, owner, created_at, updated_at)
            VALUES (?, ?, ?, 0, '{}', 'self', ?, ?)
            """.trimIndent(),
        ).run(id, kind.wire, name, now, now)
    }

    /** 用户忽略候选对象：仅记录 dismissed，不改 Reality、不 bump（candidate_dismiss ∈ neverBumpsOn）。 */
    fun dismissCandidate(candidateId: String) {
        val now = Instant.now().toString()
        driver.transaction {
            val c = driver.prepare("SELECT status, observation_count FROM discovery_candidates WHERE id = ?")
                .get(candidateId) ?: throw IllegalStateException("entity_not_found")
            if (c.str("status") != CandidateStatus.PENDING.wire) {
                throw IllegalStateException("illegal_state_transition")
            }
            driver.prepare(
                """
                UPDATE discovery_candidates
                   SET status = ?, dismissed_at_observation_count = ?, updated_at = ?
                 WHERE id = ?
                """.trimIndent(),
            ).run(CandidateStatus.DISMISSED.wire, c.long("observation_count") ?: 0L, now, candidateId)
        }
    }

    // ------------------------------------------------------------------
    // RealityDrift（H-17：发现 → Review → 用户选择 → Reality mutation）
    // 规范：drift_detect / drift_dismiss 不 bump；drift_resolve_replacement /
    //       drift_resolve_additional_path 属于 GraphRevisionMachine.bumpsOn。
    //       用户选择「已更换」= 替换（retire 旧边 + 建新边）；
    //       「两者都在用」= 额外路径（只建新边，旧边保持 active）；
    //       「没变化」= dismiss；「稍后确认」= 保持 open（不改任何东西）。
    // ------------------------------------------------------------------

    /** 已更换：retire 关联旧边 + 创建 candidate_from → target 的新边，drift → confirmed_change。 */
    fun resolveDriftAsReplacement(driftId: String) {
        val now = Instant.now().toString()
        driver.transaction {
            val d = driftOpenOrThrow(driftId)
            val candidateFrom = d.candidateFrom
                ?: throw IllegalStateException("illegal_state_transition")
            insertDependencyFromDrift(d, candidateFrom, now)
            // retire 关联的旧边（仅 active）
            for (oldId in d.relatedDependencyIds) {
                val row = driver.prepare("SELECT state FROM dependencies WHERE id = ?").get(oldId)
                if (row?.str("state") == DependencyState.ACTIVE.wire) {
                    driver.prepare("UPDATE dependencies SET state = ?, updated_at = ? WHERE id = ?")
                        .run(DependencyState.RETIRED.wire, now, oldId)
                    bumpRevision() // dependency_retire ∈ bumpsOn
                }
            }
            finalizeDrift(d, now)
        }
    }

    /** 两者都在用：创建新边，旧边保持 active，drift → confirmed_change。 */
    fun resolveDriftAsAdditionalPath(driftId: String) {
        val now = Instant.now().toString()
        driver.transaction {
            val d = driftOpenOrThrow(driftId)
            val candidateFrom = d.candidateFrom
                ?: throw IllegalStateException("illegal_state_transition")
            insertDependencyFromDrift(d, candidateFrom, now)
            finalizeDrift(d, now)
        }
    }

    /** 没变化：dismiss 不改 Reality、不 bump；「稍后确认」由 UI 不调用本函数（保持 open）。 */
    fun dismissDrift(driftId: String) {
        val now = Instant.now().toString()
        driver.transaction {
            driftOpenOrThrow(driftId)
            driver.prepare("UPDATE reality_drifts SET status = ?, updated_at = ? WHERE id = ?")
                .run(DriftStatus.DISMISSED.wire, now, driftId)
        }
    }

    private fun driftOpenOrThrow(driftId: String): DriftRow {
        val d = openDrifts().firstOrNull { it.id == driftId }
            ?: run {
                val raw = driver.prepare("SELECT status FROM reality_drifts WHERE id = ?").get(driftId)
                if (raw == null) throw IllegalStateException("entity_not_found")
                throw IllegalStateException("illegal_state_transition")
            }
        return d
    }

    private fun insertDependencyFromDrift(d: DriftRow, from: String, now: String): String {
        val capability = Capability.fromWire(d.capability) ?: Capability.PAYMENT
        val relation = Relation.fromWire(d.candidateRelation) ?: Relation.FUNDING_SOURCE
        val depId = "dep-drift-" + sha256Hex(d.id).take(12)
        driver.prepare(
            """
            INSERT INTO dependencies (id, from_node, relation, to_node, capability, criticality, state,
                                      origin, confirmed_at, last_verified_at, evidence_refs_json,
                                      verification_basis_type, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, 'unknown', 'active', 'manual', ?, ?, '[]', 'user_confirmed', ?, ?)
            ON CONFLICT (from_node, relation, to_node, capability) DO NOTHING
            """.trimIndent(),
        ).run(depId, from, relation.wire, d.targetNodeId, capability.wire, now, now, now, now)
        bumpRevision() // drift_resolve_* ∈ bumpsOn（与新建 dependency 同事务）
        return depId
    }

    /** drift → confirmed_change，并让同 target/capability/candidateFrom 的其它 open drift superseded。 */
    private fun finalizeDrift(d: DriftRow, now: String) {
        driver.prepare("UPDATE reality_drifts SET status = ?, updated_at = ? WHERE id = ?")
            .run(DriftStatus.CONFIRMED_CHANGE.wire, now, d.id)
        driver.prepare(
            """
            UPDATE reality_drifts SET status = 'superseded', updated_at = ?
             WHERE status = 'open' AND id != ?
               AND target_node_id = ? AND capability = ? AND candidate_from IS ?
            """.trimIndent(),
        ).run(now, d.id, d.targetNodeId, d.capability, d.candidateFrom)
    }

    /**
     * 建对象（Node）：只在用户于 Node Resolution 步骤显式确认后调用。
     * node_create 属于 GraphRevisionMachine.bumpsOn —— 必须与 revision bump 同事务。
     * 返回 1 表示新建，0 表示已存在。
     */
    private fun upsertNode(id: String, kind: NodeKind, name: String, now: String): Int {
        val existing = driver.prepare("SELECT id FROM nodes WHERE id = ?").get(id)
        if (existing != null) return 0
        driver.prepare(
            """
            INSERT INTO nodes (id, kind, name, archived, fields_json, owner, created_at, updated_at)
            VALUES (?, ?, ?, 0, '{}', 'self', ?, ?)
            """.trimIndent(),
        ).run(id, kind.wire, name, now, now)
        bumpRevision()
        return 1
    }

    private fun bumpRevision() {
        val next = graphRevision() + 1
        driver.prepare(
            """
            INSERT INTO meta (key, value) VALUES ('graph_revision', ?)
            ON CONFLICT(key) DO UPDATE SET value = excluded.value
            """.trimIndent(),
        ).run(next.toString())
    }

    // ------------------------------------------------------------------
    // 导入管道（spec §150：Observation 不落库，只在导入会话内存中）
    //
    // 落库的是：SourceInstance / ImportSession / Fingerprint / Evidence /
    //           Node（用户在 Node Resolution 步骤显式确认）/ Proposal（待确认）。
    // **绝不**直接写 dependencies —— Proposal ≠ Reality（spec §13）。
    // ------------------------------------------------------------------

    /** Observation-only 置信度常量：机器永不据此写 Reality，也不据此产生 required。 */
    val observedConfidence: Double get() = OBSERVED_CONFIDENCE

    data class DetectedParty(val label: String, val kind: NodeKind, val nodeId: String)

    data class ImportPreview(
        val observations: List<Observation>,
        val errors: List<String>,
        val adapterId: String,
        val sourceLabel: String,
        val instruments: List<DetectedParty>,
        val counterparties: List<DetectedParty>,
    ) {
        val projectedProposalCount: Int get() = instruments.size * counterparties.size
    }

    data class ImportCommitResult(
        val sourceInstanceId: String,
        val importSessionId: String,
        val rawCount: Int,
        val newUniqueCount: Int,
        val duplicateCount: Int,
        val nodeCount: Int,
        val proposalCount: Int,
        val errorCount: Int,
    )

    /** 预览：**不写任何库**。Node Resolution 所需的解析结果列在内存里。 */
    fun previewImport(
        observations: List<Observation>,
        errors: List<String>,
        adapterId: String,
        sourceLabel: String,
    ): ImportPreview {
        val instruments = observations.asSequence()
            .map { it.paymentMethodRaw.trim() }
            .filter { it.isNotBlank() }
            .distinct().sorted()
            .map { DetectedParty(it, NodeKind.PAYMENT_INSTRUMENT, nodeIdFor(NodeKind.PAYMENT_INSTRUMENT, it)) }
            .toList()
        val counterparties = observations.asSequence()
            .map { it.merchantRaw.trim() }
            .filter { it.isNotBlank() }
            .distinct().sorted()
            .map { DetectedParty(it, NodeKind.SERVICE, nodeIdFor(NodeKind.SERVICE, it)) }
            .toList()
        return ImportPreview(observations, errors, adapterId, sourceLabel, instruments, counterparties)
    }

    /** 用户确认后提交：一次事务内完成，失败整体回滚。 */
    fun commitImport(preview: ImportPreview): ImportCommitResult {
        val now = Instant.now().toString()
        val sourceInstanceId = "src-" + sha256Hex(preview.adapterId + "|" + preview.sourceLabel).take(16)
        val sessionId = "imp-" + sha256Hex(sourceInstanceId + "|" + now).take(16)
        val observedActions = LinkedHashSet<Pair<String, String>>()
        var newUnique = 0
        var duplicates = 0
        var nodesCreated = 0
        val keys = LinkedHashSet<String>()

        driver.transaction {
            driver.prepare(
                """
                INSERT INTO source_instances
                  (id, adapter_id, adapter_version, source_kind, label, currencies_json, state, created_at, updated_at, last_ingested_at)
                VALUES (?, ?, 1, 'statement_file', ?, '[]', 'active', ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET last_ingested_at = excluded.last_ingested_at,
                                              updated_at = excluded.updated_at
                """.trimIndent(),
            ).run(sourceInstanceId, preview.adapterId, preview.sourceLabel, now, now, now)

            driver.prepare(
                """
                INSERT INTO import_sessions
                  (id, source_type, parser_id, parser_version, started_at, raw_count, new_unique_count,
                   duplicate_count, proposal_count, error_count, source_instance_id, adapter_id, adapter_version)
                VALUES (?, ?, ?, 1, ?, ?, 0, 0, 0, ?, ?, ?, 1)
                """.trimIndent(),
            ).run(
                sessionId, preview.adapterId, preview.adapterId, now,
                preview.observations.size, preview.errors.size,
                sourceInstanceId, preview.adapterId,
            )

            for (o in preview.observations) {
                val fp = observationFingerprint(preview.adapterId, o)
                val seen = driver.prepare(
                    """
                    SELECT fingerprint FROM observation_fingerprints
                     WHERE source_instance_id = ? AND fingerprint_version = 1 AND fingerprint = ?
                    """.trimIndent(),
                ).get(sourceInstanceId, fp)
                if (seen != null) {
                    duplicates += 1
                    continue
                }
                newUnique += 1
                driver.prepare(
                    """
                    INSERT INTO observation_fingerprints
                      (fingerprint, source_instance_id, source, fingerprint_version, import_session_id, first_seen_at)
                    VALUES (?, ?, ?, 1, ?, ?)
                    """.trimIndent(),
                ).run(fp, sourceInstanceId, preview.adapterId, sessionId, now)

                val instrument = o.paymentMethodRaw.trim()
                val counterparty = o.merchantRaw.trim()
                if (instrument.isBlank() || counterparty.isBlank()) continue

                observedActions.add(instrument to counterparty)
                val instrumentId = nodeIdFor(NodeKind.PAYMENT_INSTRUMENT, instrument)
                val counterpartyId = nodeIdFor(NodeKind.SERVICE, counterparty)
                nodesCreated += upsertNode(instrumentId, NodeKind.PAYMENT_INSTRUMENT, instrument, now)
                nodesCreated += upsertNode(counterpartyId, NodeKind.SERVICE, counterparty, now)

                val key = dependencyLogicalKey(
                    instrumentId, Relation.MERCHANT_AGREEMENT, counterpartyId, Capability.PAYMENT,
                )
                keys.add(key)
            }

            for (key in keys) upsertProposal(key, preview.adapterId, sessionId, sourceInstanceId, now)
        }

        driver.prepare(
            """
            UPDATE import_sessions
               SET completed_at = ?, new_unique_count = ?, duplicate_count = ?, proposal_count = ?
             WHERE id = ?
            """.trimIndent(),
        ).run(now, newUnique, duplicates, keys.size, sessionId)

        return ImportCommitResult(
            sourceInstanceId = sourceInstanceId,
            importSessionId = sessionId,
            rawCount = preview.observations.size,
            newUniqueCount = newUnique,
            duplicateCount = duplicates,
            nodeCount = nodesCreated,
            proposalCount = keys.size,
            errorCount = preview.errors.size,
        )
    }

    private fun upsertProposal(key: String, adapterId: String, sessionId: String, sourceInstanceId: String, now: String) {
        val existing = driver.prepare("SELECT id, observation_count FROM dependency_proposals WHERE key = ?").get(key)
        if (existing == null) {
            driver.prepare(
                """
                INSERT INTO dependency_proposals
                  (id, key, from_node, relation, to_node, capability, proposal_type, source, parser_id,
                   parser_version, confidence_score, path_json, decision, observation_count, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?, '[]', 'pending', 0, ?, ?)
                """.trimIndent(),
            ).run(
                "prop-" + sha256Hex(key).take(16),
                key,
                key.split('|')[0],
                Relation.MERCHANT_AGREEMENT.wire,
                key.split('|')[2],
                Capability.PAYMENT.wire,
                com.pdig.core.generated.ProposalType.RECURRING_PAYMENT_ROUTE.wire,
                com.pdig.core.generated.ProposalSource.STATEMENT.wire,
                adapterId,
                OBSERVED_CONFIDENCE,
                now,
                now,
            )
        }
        // Evidence：按 (proposal_key, source_instance_id) 分流并累加观察次数。
        // proposal_upsert / evidence_record 属于 GraphRevisionMachine.neverBumpsOn
        // —— import 本身不得 bump graphRevision。
        val evId = "ev-" + sha256Hex(key + "|" + sourceInstanceId).take(16)
        val ev = driver.prepare("SELECT id FROM evidence WHERE proposal_key = ? AND source_instance_id = ?")
            .get(key, sourceInstanceId)
        if (ev == null) {
            driver.prepare(
                """
                INSERT INTO evidence
                  (id, proposal_key, source_instance_id, adapter_id, adapter_version, evidence_kind,
                   source_type, parser_id, parser_version, last_import_session_id,
                   first_observed_at, last_observed_at, observation_count, created_at, updated_at)
                VALUES (?, ?, ?, ?, 1, ?, ?, ?, 1, ?, ?, ?, 1, ?, ?)
                """.trimIndent(),
            ).run(
                evId, key, sourceInstanceId, adapterId,
                com.pdig.core.generated.EvidenceKind.TRANSACTION_STREAM.wire,
                adapterId, adapterId, sessionId, now, now, now, now,
            )
        } else {
            driver.prepare(
                "UPDATE evidence SET observation_count = observation_count + 1, last_observed_at = ?, updated_at = ? WHERE id = ?",
            ).run(now, now, ev.str("id"))
        }
        driver.prepare(
            "INSERT INTO proposal_evidence_refs (proposal_key, evidence_id, position) VALUES (?, ?, 0) ON CONFLICT DO NOTHING",
        ).run(key, evId)
    }

    /**
     * Reality 确认：criticality unknown → required 只能由用户显式完成（机器永不产生 required，spec §12）。
     * 这属于 GraphRevisionMachine.bumpsOn 的 dependency_confirm_update，必须 bump graphRevision。
     */
    fun setDependencyCriticality(dependencyId: String, required: Boolean) {
        val now = Instant.now().toString()
        driver.transaction {
            val row = driver.prepare("SELECT criticality FROM dependencies WHERE id = ?").get(dependencyId)
            if (row != null) {
                val target = if (required) Criticality.REQUIRED else Criticality.UNKNOWN
                if ((row.str("criticality") ?: "unknown") != target.wire) {
                    driver.prepare("UPDATE dependencies SET criticality = ?, updated_at = ? WHERE id = ?")
                        .run(target.wire, now, dependencyId)
                    bumpRevision()
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Impact（复用 core.impact，UI 不自行推导）
    // ------------------------------------------------------------------

    fun loadImpactGraph(): ImpactGraph {
        val nodeNames = nodes(includeArchived = true).associate { it.id to it.name }
        val deps = driver.prepare(
            """
            SELECT id, from_node, relation, to_node, capability, criticality, group_id, state, origin, last_verified_at
              FROM dependencies
            """.trimIndent(),
        ).all().map { r ->
            Dependency(
                id = r.str("id") ?: "",
                from = r.str("from_node") ?: "",
                relation = Relation.fromWire(r.str("relation") ?: "") ?: Relation.MERCHANT_AGREEMENT,
                to = r.str("to_node") ?: "",
                capability = Capability.fromWire(r.str("capability") ?: "") ?: Capability.PAYMENT,
                criticality = Criticality.fromWire(r.str("criticality") ?: "") ?: Criticality.UNKNOWN,
                groupId = r.str("group_id"),
                state = DependencyState.fromWire(r.str("state") ?: "") ?: DependencyState.ACTIVE,
                origin = DependencyOrigin.fromWire(r.str("origin") ?: "") ?: DependencyOrigin.MANUAL,
                lastVerifiedAt = r.str("last_verified_at") ?: "",
            )
        }
        val groups = driver.prepare(
            """
            SELECT id, group_key, target_node_id, capability, mode, member_edge_ids_json, state
              FROM dependency_groups
            """.trimIndent(),
        ).all().map { r ->
            DependencyGroup(
                id = r.str("id") ?: "",
                groupKey = r.str("group_key") ?: "",
                targetNodeId = r.str("target_node_id") ?: "",
                capability = Capability.fromWire(r.str("capability") ?: "") ?: Capability.PAYMENT,
                mode = GroupMode.fromWire(r.str("mode") ?: "") ?: GroupMode.ANY,
                memberEdgeIds = parseStringList(r.str("member_edge_ids_json")),
                state = GroupState.fromWire(r.str("state") ?: "") ?: GroupState.ACTIVE,
            )
        }
        // 只有 pending Proposal 才参与 Impact（且任何 confidence 都不会升级为 must_change）
        val proposals = pendingProposals().map { p ->
            ImpactProposalInput(
                key = p.key,
                from = p.from,
                to = p.to,
                capability = Capability.fromWire(p.capability) ?: Capability.PAYMENT,
                confidenceScore = p.confidence,
            )
        }
        return ImpactGraph(deps, groups, proposals, nodeNames)
    }

    fun impactFor(nodeId: String): ImpactResult = simulateDisable(loadImpactGraph(), nodeId)

    // ------------------------------------------------------------------
    // Backup / Restore（.depmap）
    // ------------------------------------------------------------------

    // ------------------------------------------------------------------
    // ChangePlan / Action / Verification
    // 纯复用 core.plan 的显式 resolution 规则与 core.statemachine 的迁移表，
    // UI 侧不做"完成动作数减法"，也不把 done 当成 verified（spec §48/§49）。
    // ------------------------------------------------------------------

    data class ScenarioPlanRequest(
        val scenarioId: String,
        val targetNodeId: String,
        val effectiveDate: String? = null,
    )

    data class PlanDetailView(
        val id: String,
        val scenario: String,
        val title: String,
        val workflowState: ChangePlanWorkflowState,
        val effectiveState: ChangePlanWorkflowState?,
        val baselineGraphRevision: Int,
        val lastAnalyzedGraphRevision: Int,
        val currentGraphRevision: Int,
        val targetNodeId: String?,
        val targetNodeName: String,
        val effectiveDate: String?,
        val actions: List<com.pdig.core.domain.PlanAction>,
        val mustChangeKeys: List<String>,
        val unresolvedMustChangeKeys: List<String>,
        val readiness: com.pdig.core.generated.PlanReadiness,
        val affectedServiceCount: Int,
    )

    /** 从 scenario 创建真实变更计划：must_change 每条生成一个显式 CHANGE 动作。 */
    fun createPlanForScenario(req: ScenarioPlanRequest): String {
        // planned 模板没有 factory，必须拒绝（ScenarioRegistry 策略 gate）
        require(ScenarioRegistry.isExecutable(req.scenarioId)) { "scenario_not_executable: ${req.scenarioId}" }
        val now = Instant.now().toString()
        val planId = "plan-" + sha256Hex(req.scenarioId + "|" + req.targetNodeId + "|" + now).take(16)
        val revision = graphRevision()
        val impact = impactFor(req.targetNodeId)
        val mustChangeKeys = keysOfStatus(impact, ImpactTargetStatus.MUST_CHANGE)
        val needsReviewKeys = keysOfStatus(impact, ImpactTargetStatus.NEEDS_REVIEW)
        val backupKeys = keysOfStatus(impact, ImpactTargetStatus.BACKUP_PATH) +
            keysOfStatus(impact, ImpactTargetStatus.DEGRADED)
        val actions = buildActionsFor(planId, impact, mustChangeKeys)
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

    fun planDetail(planId: String): PlanDetailView? {
        val plan = planDomain(planId) ?: return null
        val snapshot = driver.prepare("SELECT impact_snapshot_json FROM change_plans WHERE id = ?")
            .get(planId)?.str("impact_snapshot_json")
        val mustChangeKeys = snapshotKeys(snapshot, "mustChangeKeys")
        val needsReviewKeys = snapshotKeys(snapshot, "needsReviewKeys")
        val current = graphRevision()
        val unresolved = if (mustChangeKeys.isEmpty()) emptyList() else {
            val n = countUnresolvedMustChange(plan.actions, mustChangeKeys)
            // 返回仍未解决的 key：用于 UI 明确指出"哪几条还没处理"
            mustChangeKeys.filterIndexed { i, _ -> i >= mustChangeKeys.size - n }
        }
        val readiness = computeReadiness(plan, mustChangeKeys, needsReviewKeys, snapshot)
        val targetName = plan.targetNodeId?.let { id -> nodes(true).firstOrNull { it.id == id }?.name } ?: ""
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
            it.verification == null || it.verification!!.status == ActionVerificationStatus.VERIFIED
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
        val proposals = pendingProposals()
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
                currentGraphRevision = graphRevision(),
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

    /**
     * 把 .depmap 容器写成用户可见的 Downloads 文件（MediaStore，scoped storage）。
     *
     * 之前的备份页只在内存里生成字符串、进 Toast，既不落地也无法被恢复选择器选中，
     * 导致"导出 → 清数据 → 还原"这条路径从来没被真正走过。
     * 这里补上真实落盘；API 29+ 走 MediaStore **不需要**任何存储权限。
     *
     * ---------------------------------------------------------------------------
     * 2026-09-16 修复（真实缺陷 F1）：本函数原先在任何阶段出错都返回 `null`，
     * 而 UI 把所有 `null` 一律显示为「备份失败：无法写入文件。」。实测 2/2 复现：
     * 文件其实已经完整落盘、用正确口令可以恢复，UI 却报失败。
     *
     * 三类问题一并修掉：
     *  1. **阶段不可区分** —— 现在返回 [ExportBackupResult]，失败带明确的
     *     [ExportFailureStage] 与异常类型，UI 才能给出可解释的错误。
     *  2. **写完不校验** —— `write()` 返回不代表内容完整。现在写完**回读校验字节数**，
     *     不一致按失败处理，不会把"半截文件"当成成功。
     *  3. **清理不可信** —— 失败时的 `delete` 结果没有被检查。现在记录
     *     [ExportBackupResult.Failure.cleanupOk]，清理没生效时明确告知用户。
     *
     * 特别注意：**不允许出现「文件存在且可用，但 UI 报失败」**。
     * 这条由设备内回归测试 `BackupExportRegressionTest` 断言。
     * ---------------------------------------------------------------------------
     */
    fun exportBackupToFile(context: Context, password: String, fileName: String): ExportBackupResult {
        // 阶段 0：生成容器。不做任何 IO，失败与"写文件"无关，必须分开报告。
        val payload: String = try {
            DepmapContainer.create(exportGraph(driver).payloadJson.toByteArray(Charsets.UTF_8), password).json
        } catch (t: Throwable) {
            return fail(ExportFailureStage.ENCRYPT, t, cleanupOk = null)
        }
        val bytes = payload.toByteArray(Charsets.UTF_8)
        // 取 resolver 本身也可能抛（例如 Context 已被销毁）。它属于"建行之前"，
        // 归到 INSERT 阶段并且没有残留可清理 —— 但绝不允许它逃逸成未分类的崩溃。
        val resolver = try {
            context.contentResolver
        } catch (t: Throwable) {
            return fail(ExportFailureStage.INSERT, t, cleanupOk = null)
        }

        // 阶段 1：在 MediaStore 建行（IS_PENDING=1，此时其它应用看不到它）。
        val uri = try {
            resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, pendingValues(fileName))
                ?: return fail(ExportFailureStage.INSERT, null, cleanupOk = null)
        } catch (t: Throwable) {
            return fail(ExportFailureStage.INSERT, t, cleanupOk = null)
        }

        // 阶段 2：写入 + flush。use{} 负责 close。
        try {
            val out = resolver.openOutputStream(uri)
                ?: throw java.io.IOException("openOutputStream returned null")
            out.use {
                it.write(bytes)
                it.flush()
            }
        } catch (t: Throwable) {
            return fail(ExportFailureStage.WRITE, t, cleanupOk = deleteQuietly(resolver, uri))
        }

        // 阶段 3：回读校验。字节数不一致 = 写入不完整，绝不当作成功。
        try {
            val actual = resolver.openInputStream(uri)?.use { it.readBytes() }
            if (actual == null || actual.size != bytes.size) {
                throw java.io.IOException(
                    "verify_size_mismatch expected=${bytes.size} actual=${actual?.size ?: -1}",
                )
            }
        } catch (t: Throwable) {
            return fail(ExportFailureStage.VERIFY, t, cleanupOk = deleteQuietly(resolver, uri))
        }

        // 阶段 4：发布（IS_PENDING=0）。受影响行数为 0 说明没真正发布，同样按失败处理。
        try {
            val updated = resolver.update(uri, publishedValues(), null, null)
            if (updated <= 0) throw java.io.IOException("publish_affected_rows=$updated")
        } catch (t: Throwable) {
            return fail(ExportFailureStage.PUBLISH, t, cleanupOk = deleteQuietly(resolver, uri))
        }

        // MediaStore 在重名时会自动改名，回读真实文件名，UI 才能报出用户真能找到的名字。
        val displayName = queryDisplayName(resolver, uri) ?: fileName
        return ExportBackupResult.Success(displayName = displayName, byteCount = bytes.size)
    }

    private fun pendingValues(fileName: String) = ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
        put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
        put(MediaStore.Downloads.IS_PENDING, 1)
    }

    private fun publishedValues() = ContentValues().apply {
        put(MediaStore.Downloads.IS_PENDING, 0)
    }

    private fun deleteQuietly(resolver: android.content.ContentResolver, uri: android.net.Uri): Boolean =
        try {
            resolver.delete(uri, null, null) > 0
        } catch (_: Throwable) {
            false
        }

    private fun queryDisplayName(
        resolver: android.content.ContentResolver,
        uri: android.net.Uri,
    ): String? = try {
        resolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
    } catch (_: Throwable) {
        null
    }

    /**
     * 统一失败出口。只记录**阶段 + 异常类型 + 异常 message**：
     * 这些都是 MediaStore / IO 层的信息，不含口令、明文图或账单内容（AGENTS §17）。
     */
    private fun fail(
        stage: ExportFailureStage,
        t: Throwable?,
        cleanupOk: Boolean?,
    ): ExportBackupResult.Failure {
        val type = t?.javaClass?.name ?: "null_result"
        val detail = t?.message ?: type
        android.util.Log.w(EXPORT_TAG, "backup export failed at $stage: $type: $detail")
        return ExportBackupResult.Failure(stage = stage, errorType = type, detail = detail, cleanupOk = cleanupOk)
    }

    suspend fun exportBackup(password: String): String = withContext(Dispatchers.Default) {
        val payload = exportGraph(driver).payloadJson
        DepmapContainer.create(payload.toByteArray(Charsets.UTF_8), password).json
    }

    suspend fun restoreBackup(containerJson: String, password: String): Int =
        withContext(Dispatchers.Default) {
            val plaintext = DepmapContainer.openContainer(containerJson, password)
            val payload = plaintext.toString(Charsets.UTF_8)
            val imported = importGraph(driver, payload)
            checkGraphIntegrity(driver) // 断言无孤儿；结果供 UI 显示
            imported.values.sum()
        }

    fun integrity(): OrphanSummary {
        val r = checkGraphIntegrity(driver)
        return OrphanSummary(
            r.orphanDependencies.size + r.orphanGroups.size + r.danglingGroupMembers.size +
                r.orphanEvidence.size + r.orphanFingerprints.size,
        )
    }
}

data class NodeRow(val id: String, val kind: String, val name: String, val archived: Boolean, val fieldsJson: String)
data class DependencyRow(
    val id: String,
    val from: String,
    val fromName: String,
    val relation: String,
    val to: String,
    val toName: String,
    val capability: String,
    val criticality: String,
    val state: String,
)

data class PlanRow(
    val id: String,
    val title: String,
    val scenario: String,
    val workflowState: String,
    val lastAnalyzedRevision: Int,
    val effectiveDate: String?,
)

data class DriftRow(
    val id: String,
    val kind: String,
    val targetNodeId: String,
    val capability: String,
    val candidateFrom: String?,
    val candidateRelation: String,
    val relatedDependencyIds: List<String>,
    val observationCount: Int,
    val detectedAt: String,
)
data class CandidateRow(
    val id: String,
    val candidateKind: String,
    val label: String,
    val observationCount: Int,
    val status: String,
)
data class SourceRow(val id: String, val label: String, val adapterId: String, val state: String, val lastIngestedAt: String?)
data class ProposalRow(
    val id: String,
    val key: String,
    val from: String,
    val to: String,
    val relation: String,
    val capability: String,
    val confidence: Double,
    val observationCount: Int,
)

data class ParseOutcome(val observations: List<Observation>, val errors: List<String>)
data class OrphanSummary(val orphanCount: Int)

// ---------------------------------------------------------------------------
// 备份导出的结果模型（F1 修复）
//
// 存在的唯一理由：让"成功/失败"与"失败在哪一步"变成**可断言的数据**，
// 而不是一个 `null`。UI 只负责把结果翻译成中文，不再自己猜。
// ---------------------------------------------------------------------------

/** 备份导出失败阶段。用户可见文案由 UI 映射，不直接暴露枚举（spec §65）。 */
enum class ExportFailureStage {
    /** 生成 .depmap 容器失败（加密封装）。 */
    ENCRYPT,

    /** 在 MediaStore Downloads 建行失败。 */
    INSERT,

    /** 写入输出流失败。 */
    WRITE,

    /** 写完回读校验失败（字节数不一致）。 */
    VERIFY,

    /** 发布（IS_PENDING=0）失败。 */
    PUBLISH,
}

sealed interface ExportBackupResult {

    /**
     * @param displayName MediaStore 回读的**真实**文件名（重名时系统会改写）
     * @param byteCount   落盘并校验通过的字节数
     */
    data class Success(val displayName: String, val byteCount: Int) : ExportBackupResult

    /**
     * @param stage     失败阶段
     * @param errorType 异常类型（或 `null_result`）
     * @param detail    非敏感的诊断细节，仅用于日志与自助排查
     * @param cleanupOk 失败后清理是否真的生效；`null` 表示失败发生在建行之前，无残留可清
     */
    data class Failure(
        val stage: ExportFailureStage,
        val errorType: String,
        val detail: String,
        val cleanupOk: Boolean?,
    ) : ExportBackupResult
}

internal const val EXPORT_TAG = "PDIG_BACKUP"

// ---------------------------------------------------------------------------
// 文件级辅助（纯函数，便于 JVM 侧复用与审计）
// ---------------------------------------------------------------------------

/** Observation-only 置信度：机器永不据此写 Reality，也永不据此产生 required。 */
const val OBSERVED_CONFIDENCE = 0.5

/** 依赖"长期未验证"阈值（天），用于 readiness 的 staleRelevantDependencies。 */
const val STALE_CUTOFF_DAYS = 90L

internal fun sha256Hex(value: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }
}

/** 稳定 node id：同一 kind+name 重复导入不会生成第二个节点。 */
internal fun nodeIdFor(kind: NodeKind, name: String): String =
    "nd-" + sha256Hex(kind.wire + "|" + name).take(12)

/** 跨去重指纹：限定在 (source_instance, fingerprint_version) 作用域内 —— MVP02 § constraints。 */
internal fun observationFingerprint(adapterId: String, o: Observation): String =
    sha256Hex(
        listOf(
            adapterId,
            o.sourceTxnId ?: "",
            o.occurredAt,
            o.amount.toString(),
            o.currency,
            o.merchantRaw,
            o.paymentMethodRaw,
        ).joinToString("|"),
    )

internal fun keyString(nodeId: String, capabilityWire: String): String = "$nodeId|$capabilityWire"

internal fun parseStringList(json: String?): List<String> {
    if (json.isNullOrBlank()) return emptyList()
    return try {
        (JsonParser.parse(json) as? Json.Arr)?.items?.mapNotNull { (it as? Json.Str)?.value } ?: emptyList()
    } catch (_: Throwable) {
        emptyList()
    }
}

internal fun keysOfStatus(
    impact: ImpactResult,
    status: ImpactTargetStatus,
): List<String> = impact.targets
    .filter { it.status == status }
    .map { keyString(it.nodeId, it.capability.wire) }
    .distinct()
    .sorted()

internal fun buildActionsFor(
    planId: String,
    impact: ImpactResult,
    keys: List<String>,
): List<com.pdig.core.domain.PlanAction> = keys.mapIndexed { i, key ->
    val t = impact.targets.first { keyString(it.nodeId, it.capability.wire) == key }
    com.pdig.core.domain.PlanAction(
        id = "act-$planId-${i + 1}",
        title = "把「${t.nodeName}」的支付来源换成新卡",
        phase = PlanActionPhase.CHANGE,
        // 显式 resolution：这个动作"声明"自己解决哪个 must_change key（spec §43，禁止减法）
        resolvesImpactKeys = listOf(key),
        verification = com.pdig.core.domain.ActionVerification(
            method = ActionVerificationMethod.MANUAL_CONFIRMATION,
            status = ActionVerificationStatus.PENDING,
        ),
    )
}

internal fun snapshotKeys(snapshot: String?, field: String): List<String> {
    if (snapshot.isNullOrBlank()) return emptyList()
    return try {
        val obj = JsonParser.parse(snapshot) as? Json.Obj ?: return emptyList()
        (obj[field] as? Json.Arr)?.items?.mapNotNull { (it as? Json.Str)?.value } ?: emptyList()
    } catch (_: Throwable) {
        emptyList()
    }
}

internal fun involvedNodeIds(targetNodeId: String?, snapshot: String?): Set<String> {
    val out = LinkedHashSet<String>()
    targetNodeId?.let { if (it.isNotBlank()) out.add(it) }
    for (k in snapshotKeys(snapshot, "targetKeys")) out.add(k.substringBefore('|'))
    return out
}

internal fun planActionToJson(a: com.pdig.core.domain.PlanAction): Json = Json.Obj(
    listOf(
        "id" to Json.Str(a.id),
        "title" to Json.Str(a.title),
        "phase" to Json.Str(a.phase.wire),
        "done" to Json.Bool(a.done),
        "resolvesImpactKeys" to Json.Arr(a.resolvesImpactKeys.map { Json.Str(it) }),
        "verification" to (a.verification?.let { v ->
            Json.Obj(
                listOf(
                    "method" to Json.Str(v.method.wire),
                    "status" to Json.Str(v.status.wire),
                    "evidenceRefs" to Json.Arr(v.evidenceRefs.map { Json.Str(it) }),
                ),
            )
        } ?: Json.Null),
    ),
)

internal fun planActionsFromJson(raw: String?): List<com.pdig.core.domain.PlanAction> {
    if (raw.isNullOrBlank()) return emptyList()
    return try {
        val arr = JsonParser.parse(raw) as? Json.Arr ?: return emptyList()
        arr.items.mapNotNull { it as? Json.Obj }.map { o ->
            val v = (o["verification"] as? Json.Obj)?.let { vo ->
                com.pdig.core.domain.ActionVerification(
                    method = ActionVerificationMethod.fromWire((vo["method"] as? Json.Str)?.value ?: "")
                        ?: ActionVerificationMethod.MANUAL_CONFIRMATION,
                    status = ActionVerificationStatus.fromWire((vo["status"] as? Json.Str)?.value ?: "")
                        ?: ActionVerificationStatus.PENDING,
                    evidenceRefs = ((vo["evidenceRefs"] as? Json.Arr)?.items)
                        ?.mapNotNull { (it as? Json.Str)?.value } ?: emptyList(),
                )
            }
            com.pdig.core.domain.PlanAction(
                id = (o["id"] as? Json.Str)?.value ?: "",
                title = (o["title"] as? Json.Str)?.value ?: "",
                phase = PlanActionPhase.fromWire((o["phase"] as? Json.Str)?.value ?: "")
                    ?: PlanActionPhase.CHANGE,
                done = (o["done"] as? Json.Bool)?.value ?: false,
                resolvesImpactKeys = ((o["resolvesImpactKeys"] as? Json.Arr)?.items)
                    ?.mapNotNull { (it as? Json.Str)?.value } ?: emptyList(),
                verification = v,
            )
        }
    } catch (_: Throwable) {
        emptyList()
    }
}
