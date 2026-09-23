package com.pdig.app.data

import com.pdig.core.db.SqliteDriver
import com.pdig.core.generated.Capability
import com.pdig.core.generated.Relation
import java.time.Instant

/**
 * dependency_proposals + evidence + proposal_evidence_refs 的读写。
 * Proposal ≠ Reality（spec §13）：只有 acceptProposal 才把边写入 Reality（与 bump 同事务）。
 */
class ProposalRepository(
    private val driver: SqliteDriver,
    private val graph: GraphRepository,
) {

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
            graph.bumpRevision()
        }
    }

    fun rejectProposal(proposalId: String) {
        driver.transaction {
            driver.prepare(
                "UPDATE dependency_proposals SET decision = 'rejected', decided_at = ? WHERE id = ?",
            ).run(Instant.now().toString(), proposalId)
    }
    }

    /**
     * import 管道内 upsert（Proposal ≠ Reality —— spec §13）：由 SourceRepository 在导入事务内调用。
     * proposal_upsert / evidence_record 属于 GraphRevisionMachine.neverBumpsOn —— 不 bump。
     */
    internal fun upsertProposal(key: String, adapterId: String, sessionId: String, sourceInstanceId: String, now: String) {
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
}
