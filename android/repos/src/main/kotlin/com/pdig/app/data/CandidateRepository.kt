package com.pdig.app.data

import com.pdig.core.db.SqliteDriver
import com.pdig.core.generated.CandidateStatus
import com.pdig.core.generated.NodeKind
import java.time.Instant

/**
 * DiscoveryCandidate（H-16：发现 → Review → Confirm → Node / Dismiss / Later）。
 *
 * 规范（spec/domain/domain.json + state-machines/state-machines.json）：
 *   - Candidate 本身不进入 Impact、不 bump graphRevision；
 *   - accept 幂等：replay 返回 created=false 与同一个 nodeId；
 *   - dismiss 不改 Reality，仅记录 dismissedAtObservationCount；
 *   - 未确认前不得进入 Impact（loadImpactGraph 只读 dependencies + pending proposals）。
 */
class CandidateRepository(private val driver: SqliteDriver) {

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
}
