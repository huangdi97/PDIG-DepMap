package com.pdig.app.data

import com.pdig.core.db.SqliteDriver
import com.pdig.core.domain.Dependency
import com.pdig.core.domain.DependencyGroup
import com.pdig.core.domain.ImpactGraph
import com.pdig.core.domain.ImpactProposalInput
import com.pdig.core.generated.Capability
import com.pdig.core.generated.Criticality
import com.pdig.core.generated.DependencyOrigin
import com.pdig.core.generated.DependencyState
import com.pdig.core.generated.GroupMode
import com.pdig.core.generated.GroupState
import com.pdig.core.generated.NodeKind
import com.pdig.core.generated.Relation
import com.pdig.core.impact.ImpactResult
import com.pdig.core.impact.simulateDisable
import com.pdig.core.serialize.checkGraphIntegrity
import com.pdig.core.timeline.TimelineItem
import com.pdig.core.timeline.buildTimeline
import java.time.Instant

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

    fun graphRevision(): Int =
        driver.prepare("SELECT value FROM meta WHERE key = 'graph_revision'").get()
            ?.str("value")?.toIntOrNull() ?: 0

    fun timeline(nowIso: String = Instant.now().toString()): List<TimelineItem> =
        buildTimeline(driver, nowIso)

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
