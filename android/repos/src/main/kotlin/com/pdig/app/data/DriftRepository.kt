package com.pdig.app.data

import com.pdig.core.db.SqliteDriver
import com.pdig.core.generated.Capability
import com.pdig.core.generated.DependencyState
import com.pdig.core.generated.DriftStatus
import com.pdig.core.generated.Relation
import java.time.Instant

/**
 * RealityDrift（H-17：发现 → Review → 用户选择 → Reality mutation）。
 *
 * 规范：drift_detect / drift_dismiss 不 bump；drift_resolve_replacement /
 *       drift_resolve_additional_path 属于 GraphRevisionMachine.bumpsOn。
 *       用户选择「已更换」= 替换（retire 旧边 + 建新边）；
 *       「两者都在用」= 额外路径（只建新边，旧边保持 active）；
 *       「没变化」= dismiss；「稍后确认」= 保持 open（不改任何东西）。
 */
class DriftRepository(
    private val driver: SqliteDriver,
    private val graph: GraphRepository,
) {

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
                    graph.bumpRevision() // dependency_retire ∈ bumpsOn
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
        graph.bumpRevision() // drift_resolve_* ∈ bumpsOn（与新建 dependency 同事务）
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
}
