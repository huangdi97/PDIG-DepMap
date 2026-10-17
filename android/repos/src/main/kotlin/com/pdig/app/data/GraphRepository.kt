package com.pdig.app.data

import com.pdig.core.db.SqliteDriver
import com.pdig.core.domain.Dependency
import com.pdig.core.domain.DependencyGroup
import com.pdig.core.domain.ImpactGraph
import com.pdig.core.domain.ImpactProposalInput
import com.pdig.core.domain.validateRelationUse
import com.pdig.core.domain.MaintenanceFactWrite
import com.pdig.core.domain.MaintenanceScheduleWrite
import com.pdig.core.domain.upsertConfirmedMaintenanceFact
import com.pdig.core.domain.upsertConfirmedMaintenanceFacts
import com.pdig.core.domain.upsertConfirmedMaintenanceSchedule
import com.pdig.core.generated.Capability
import com.pdig.core.generated.CanonicalSpec
import com.pdig.core.generated.Criticality
import com.pdig.core.generated.DependencyOrigin
import com.pdig.core.generated.DependencyState
import com.pdig.core.generated.GroupMode
import com.pdig.core.generated.GroupState
import com.pdig.core.generated.IdentityAnchorSubtype
import com.pdig.core.generated.NodeKind
import com.pdig.core.generated.Relation
import com.pdig.core.generated.VerificationBasisType
import com.pdig.core.json.Json
import com.pdig.core.json.JsonWriter
import com.pdig.core.impact.ImpactResult
import com.pdig.core.impact.simulateDisable
import com.pdig.core.serialize.checkGraphIntegrity
import com.pdig.core.timeline.TimelineItem
import com.pdig.core.timeline.buildTimeline
import java.time.Instant
import java.util.UUID

/**
 * 图（nodes / dependencies / dependency_groups）与 graph_revision 的读写。
 * graphRevision/bumpRevision/upsertNode 为 internal：Reality mutation 与 revision bump
 * 必须保持同事务（spec §27），因此 Proposal/Drift/Source Repository 复用它。
 */
class GraphRepository(
    private val driver: SqliteDriver,
    private val pendingProposals: () -> List<ProposalRow>,
) {

    // ------------------------------------------------------------------
    // 查询（只读投影）
    // ------------------------------------------------------------------

    fun nodes(includeArchived: Boolean = false): List<NodeRow> {
        val sql = if (includeArchived) {
            "SELECT id, kind, name, issuer, last4, archived, fields_json FROM nodes ORDER BY name"
        } else {
            "SELECT id, kind, name, issuer, last4, archived, fields_json FROM nodes WHERE archived = 0 ORDER BY name"
        }
        return driver.prepare(sql).all().map {
            NodeRow(
                id = it.str("id") ?: "",
                kind = it.str("kind") ?: "",
                name = it.str("name") ?: "",
                archived = (it.long("archived") ?: 0L) != 0L,
                fieldsJson = it.str("fields_json") ?: "{}",
                issuer = it.str("issuer"),
                last4 = it.str("last4"),
            )
        }
    }

    fun nodeById(id: String): NodeRow? {
        val row = driver.prepare(
            "SELECT id, kind, name, issuer, last4, archived, fields_json FROM nodes WHERE id = ?",
        ).get(id) ?: return null
        return NodeRow(
            id = row.str("id") ?: "",
            kind = row.str("kind") ?: "",
            name = row.str("name") ?: "",
            archived = (row.long("archived") ?: 0L) != 0L,
            fieldsJson = row.str("fields_json") ?: "{}",
            issuer = row.str("issuer"),
            last4 = row.str("last4"),
        )
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

    fun graphRevision(): Int =
        driver.prepare("SELECT value FROM meta WHERE key = 'graph_revision'").get()
            ?.str("value")?.toIntOrNull() ?: 0

    fun timeline(nowIso: String = Instant.now().toString()): List<TimelineItem> =
        buildTimeline(driver, nowIso)

    /**
     * 显式用户手工建立 Reality 对象。
     *
     * 仅允许 spec/domain/domain.json constants.runtimeCreatableNodeKinds；
     * 这是产品 authority，而不是导入去重路径，因此使用随机 UUID，不按 name 合并对象。
     * Node 写入与 graphRevision bump 保持同一事务。
     */
    fun createManualNode(request: ManualNodeCreateRequest): ManualNodeCreateResult {
        require(request.kind.wire in CanonicalSpec.RUNTIME_CREATABLE_NODE_KINDS) {
            "node kind '${request.kind.wire}' is not runtime-creatable"
        }
        val name = request.name.trim()
        require(name.isNotEmpty()) { "manual node name must not be blank" }

        if (request.kind != NodeKind.PAYMENT_INSTRUMENT) {
            require(request.issuer.isNullOrBlank() && request.last4.isNullOrBlank()) {
                "issuer/last4 are only accepted for payment_instrument manual creation"
            }
        }

        val id = UUID.randomUUID().toString()
        val now = Instant.now().toString()
        driver.transaction {
            driver.prepare(
                """
                INSERT INTO nodes
                  (id, kind, template_id, name, issuer, last4, owner, archived,
                   fields_json, vault_ref, wallet_ref, created_at, updated_at)
                VALUES (?, ?, NULL, ?, ?, ?, 'self', 0, '{}', NULL, NULL, ?, ?)
                """.trimIndent(),
            ).run(
                id,
                request.kind.wire,
                name,
                request.issuer?.trim()?.takeIf { it.isNotEmpty() },
                request.last4?.trim()?.takeIf { it.isNotEmpty() },
                now,
                now,
            )
            bumpRevision()
        }

        val node = requireNotNull(nodeById(id)) {
            "manual node disappeared after authoritative commit: $id"
        }
        return ManualNodeCreateResult(node = node, graphRevision = graphRevision())
    }

    /**
     * Atomically create a governed phone/email identity anchor.
     *
     * This is intentionally separate from createManualNode(): generic runtime-creatable
     * kinds stay narrow, while identity creation requires subtype + independently
     * confirmed identifier Reality in the same transaction as the Node itself.
     */
    fun createManualIdentityAnchor(
        request: ManualIdentityAnchorCreateRequest,
    ): ManualNodeCreateResult {
        require(
            request.subtype.wire in CanonicalSpec.RUNTIME_CREATABLE_IDENTITY_ANCHOR_SUBTYPES
        ) {
            "identity subtype '${request.subtype.wire}' is not runtime-creatable"
        }

        val name = request.name.trim()
        require(name.isNotEmpty()) { "manual identity name must not be blank" }

        val identifierValue = request.identifierValue.trim()
        require(identifierValue.isNotEmpty()) {
            "manual identity identifier value must not be blank"
        }
        require(identifierValue.length <= 320) {
            "manual identity identifier value is too long"
        }
        require(identifierValue.none { it.code < 0x20 || it.code == 0x7F }) {
            "manual identity identifier value contains control characters"
        }

        val id = UUID.randomUUID().toString()
        val now = Instant.now().toString()
        val fieldsJson = JsonWriter.write(
            Json.Obj(
                listOf(
                    "identity_anchor_profile" to Json.Obj(
                        listOf(
                            "version" to Json.Num("1"),
                            "subtype" to Json.Str(request.subtype.wire),
                            "verification_basis_type" to
                                Json.Str(VerificationBasisType.USER_CONFIRMED.wire),
                            "confirmed_at" to Json.Str(now),
                            "evidence_refs" to Json.Arr(emptyList()),
                            "identifier" to Json.Obj(
                                listOf(
                                    "value" to Json.Str(identifierValue),
                                    "verification_basis_type" to
                                        Json.Str(VerificationBasisType.USER_CONFIRMED.wire),
                                    "confirmed_at" to Json.Str(now),
                                    "evidence_refs" to Json.Arr(emptyList()),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        driver.transaction {
            driver.prepare(
                """
                INSERT INTO nodes
                  (id, kind, template_id, name, issuer, last4, owner, archived,
                   fields_json, vault_ref, wallet_ref, created_at, updated_at)
                VALUES (?, 'identity_anchor', NULL, ?, NULL, NULL, 'self', 0,
                        ?, NULL, NULL, ?, ?)
                """.trimIndent(),
            ).run(id, name, fieldsJson, now, now)
            bumpRevision()
        }

        val node = requireNotNull(nodeById(id)) {
            "manual identity anchor disappeared after authoritative commit: $id"
        }
        return ManualNodeCreateResult(node = node, graphRevision = graphRevision())
    }

    /**
     * Explicit user confirmation of a governed maintenance fact.
     *
     * Canonical construction/validation happens in core; repository owns the
     * encrypted Reality mutation and graphRevision transaction.
     */
    fun confirmMaintenanceFact(
        nodeId: String,
        request: MaintenanceFactWrite,
    ): MaintenanceWriteResult {
        val current = requireNotNull(nodeById(nodeId)) {
            "maintenance target node does not exist: $nodeId"
        }
        require(!current.archived) { "maintenance target node is archived: $nodeId" }
        val kind = requireNotNull(NodeKind.fromWire(current.kind)) {
            "unknown maintenance target node kind: ${current.kind}"
        }
        val now = Instant.now().toString()
        val nextFields = upsertConfirmedMaintenanceFact(
            nodeKind = kind,
            fieldsJson = current.fieldsJson,
            request = request,
            confirmedAt = now,
        )
        driver.transaction {
            driver.prepare(
                "UPDATE nodes SET fields_json = ?, updated_at = ? WHERE id = ?",
            ).run(nextFields, now, nodeId)
            bumpRevision()
        }
        return MaintenanceWriteResult(
            node = requireNotNull(nodeById(nodeId)) {
                "maintenance target disappeared after authoritative commit: $nodeId"
            },
            graphRevision = graphRevision(),
        )
    }

    /** Confirm multiple coupled maintenance facts with exactly one Reality revision bump. */
    fun confirmMaintenanceFacts(
        nodeId: String,
        requests: List<MaintenanceFactWrite>,
    ): MaintenanceWriteResult {
        val current = requireNotNull(nodeById(nodeId)) {
            "maintenance target node does not exist: $nodeId"
        }
        require(!current.archived) { "maintenance target node is archived: $nodeId" }
        val kind = requireNotNull(NodeKind.fromWire(current.kind)) {
            "unknown maintenance target node kind: ${current.kind}"
        }
        val now = Instant.now().toString()
        val nextFields = upsertConfirmedMaintenanceFacts(
            nodeKind = kind,
            fieldsJson = current.fieldsJson,
            requests = requests,
            confirmedAt = now,
        )
        driver.transaction {
            driver.prepare(
                "UPDATE nodes SET fields_json = ?, updated_at = ? WHERE id = ?",
            ).run(nextFields, now, nodeId)
            bumpRevision()
        }
        return MaintenanceWriteResult(
            node = requireNotNull(nodeById(nodeId)) {
                "maintenance target disappeared after authoritative commit: $nodeId"
            },
            graphRevision = graphRevision(),
        )
    }

    /** Explicit user/authority confirmation of a governed maintenance schedule. */
    fun confirmMaintenanceSchedule(
        nodeId: String,
        request: MaintenanceScheduleWrite,
    ): MaintenanceWriteResult {
        val current = requireNotNull(nodeById(nodeId)) {
            "maintenance target node does not exist: $nodeId"
        }
        require(!current.archived) { "maintenance target node is archived: $nodeId" }
        val kind = requireNotNull(NodeKind.fromWire(current.kind)) {
            "unknown maintenance target node kind: ${current.kind}"
        }
        val now = Instant.now().toString()
        val nextFields = upsertConfirmedMaintenanceSchedule(
            nodeKind = kind,
            fieldsJson = current.fieldsJson,
            request = request,
            confirmedAt = now,
        )
        driver.transaction {
            driver.prepare(
                "UPDATE nodes SET fields_json = ?, updated_at = ? WHERE id = ?",
            ).run(nextFields, now, nodeId)
            bumpRevision()
        }
        return MaintenanceWriteResult(
            node = requireNotNull(nodeById(nodeId)) {
                "maintenance target disappeared after authoritative commit: $nodeId"
            },
            graphRevision = graphRevision(),
        )
    }

    /**
     * Confirm a manual Dependency using only the current Canonical v3 runtime
     * relation registry. This does not widen schema or enable storage-only
     * verifies / bound_to relations.
     */
    fun createManualDependency(
        request: ManualDependencyCreateRequest,
    ): ManualDependencyCreateResult {
        val fromNode = requireNotNull(nodeById(request.fromNodeId)) {
            "from node does not exist: ${request.fromNodeId}"
        }
        val toNode = requireNotNull(nodeById(request.toNodeId)) {
            "to node does not exist: ${request.toNodeId}"
        }
        require(!fromNode.archived && !toNode.archived) {
            "manual dependency cannot target archived nodes"
        }

        val fromKind = requireNotNull(NodeKind.fromWire(fromNode.kind)) {
            "unknown from node kind: ${fromNode.kind}"
        }
        val toKind = requireNotNull(NodeKind.fromWire(toNode.kind)) {
            "unknown to node kind: ${toNode.kind}"
        }
        val validation = validateRelationUse(
            fromKind = fromKind,
            relation = request.relation.wire,
            toKind = toKind,
            capability = request.capability.wire,
        )
        require(validation.ok) {
            validation.reason ?: "manual dependency does not match runtime relation registry"
        }

        val now = Instant.now().toString()
        var created = false
        var reactivated = false
        var dependencyId = ""

        driver.transaction {
            val existing = driver.prepare(
                """
                SELECT id, state, criticality
                  FROM dependencies
                 WHERE from_node = ? AND relation = ? AND to_node = ? AND capability = ?
                """.trimIndent(),
            ).get(
                request.fromNodeId,
                request.relation.wire,
                request.toNodeId,
                request.capability.wire,
            )

            if (existing == null) {
                dependencyId = UUID.randomUUID().toString()
                driver.prepare(
                    """
                    INSERT INTO dependencies
                      (id, from_node, relation, to_node, capability, criticality, group_id,
                       state, origin, confirmed_at, last_verified_at, retired_at,
                       evidence_refs_json, verification_basis_type, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, NULL, 'active', 'manual', ?, ?, NULL,
                            '[]', 'user_confirmed', ?, ?)
                    """.trimIndent(),
                ).run(
                    dependencyId,
                    request.fromNodeId,
                    request.relation.wire,
                    request.toNodeId,
                    request.capability.wire,
                    if (request.required) Criticality.REQUIRED.wire else Criticality.UNKNOWN.wire,
                    now,
                    now,
                    now,
                    now,
                )
                created = true
            } else {
                dependencyId = existing.str("id") ?: error("existing dependency missing id")
                val wasRetired =
                    (existing.str("state") ?: DependencyState.ACTIVE.wire) ==
                        DependencyState.RETIRED.wire
                val currentCriticality =
                    existing.str("criticality") ?: Criticality.UNKNOWN.wire
                val nextCriticality = if (request.required) {
                    Criticality.REQUIRED.wire
                } else {
                    currentCriticality
                }
                driver.prepare(
                    """
                    UPDATE dependencies
                       SET state = 'active',
                           retired_at = NULL,
                           criticality = ?,
                           last_verified_at = ?,
                           updated_at = ?
                     WHERE id = ?
                    """.trimIndent(),
                ).run(nextCriticality, now, now, dependencyId)
                reactivated = wasRetired
            }

            // Confirmation / re-confirmation mutates Reality and therefore shares
            // the same transaction with exactly one graphRevision bump.
            bumpRevision()
        }

        val dependency = requireNotNull(dependencyById(dependencyId)) {
            "manual dependency disappeared after authoritative commit: $dependencyId"
        }
        return ManualDependencyCreateResult(
            dependency = dependency,
            graphRevision = graphRevision(),
            created = created,
            reactivated = reactivated,
        )
    }

    private fun dependencyById(id: String): DependencyRow? {
        val row = driver.prepare(
            """
            SELECT d.id, d.from_node, d.relation, d.to_node, d.capability,
                   d.criticality, d.state, f.name AS from_name, t.name AS to_name
              FROM dependencies d
              LEFT JOIN nodes f ON f.id = d.from_node
              LEFT JOIN nodes t ON t.id = d.to_node
             WHERE d.id = ?
            """.trimIndent(),
        ).get(id) ?: return null
        return DependencyRow(
            id = row.str("id") ?: "",
            from = row.str("from_node") ?: "",
            fromName = row.str("from_name") ?: row.str("from_node") ?: "",
            relation = row.str("relation") ?: "",
            to = row.str("to_node") ?: "",
            toName = row.str("to_name") ?: row.str("to_node") ?: "",
            capability = row.str("capability") ?: "payment",
            criticality = row.str("criticality") ?: "unknown",
            state = row.str("state") ?: "active",
        )
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

    fun integrity(): OrphanSummary {
        val r = checkGraphIntegrity(driver)
        return OrphanSummary(
            r.orphanDependencies.size + r.orphanGroups.size + r.danglingGroupMembers.size +
                r.orphanEvidence.size + r.orphanFingerprints.size,
        )
    }

    /** bump graphRevision（所有 Reality mutation 必须与 bump 同事务 —— spec §27）。 */
    internal fun bumpRevision() {
        val next = graphRevision() + 1
        driver.prepare(
            """
            INSERT INTO meta (key, value) VALUES ('graph_revision', ?)
            ON CONFLICT(key) DO UPDATE SET value = excluded.value
            """.trimIndent(),
        ).run(next.toString())
    }

    /**
     * 建对象（Node）：只在用户于 Node Resolution 步骤显式确认后调用。
     * node_create 属于 GraphRevisionMachine.bumpsOn —— 必须与 revision bump 同事务。
     * 返回 1 表示新建，0 表示已存在。
     */
    internal fun upsertNode(id: String, kind: NodeKind, name: String, now: String): Int {
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
}
