package com.pdig.app.evidence

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pdig.app.data.AppContainer
import com.pdig.app.platform.AndroidSqliteDriver
import com.pdig.core.generated.CandidateStatus
import com.pdig.core.generated.DriftStatus
import com.pdig.core.schema.migrate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * H-16 / H-17 设备内证据：DiscoveryCandidate 与 RealityDrift 的用户决策流程。
 *
 * 规范（spec/domain/domain.json + spec/state-machines/state-machines.json）：
 *  - candidate_accept / candidate_dismiss / drift_detect / drift_dismiss ∈ neverBumpsOn；
 *  - drift_resolve_replacement / drift_resolve_additional_path ∈ bumpsOn；
 *  - Candidate accept 创建**一个** Node 且幂等；未确认前不得进入 Impact（本测试不建依赖）。
 *  - Drift 用户选择「已更换」= retire 旧边 + 建新边；「两者都在用」= 只建新边；
 *    「没变化」= dismiss 不改 Reality；「稍后确认」= 保持 open。
 */
@RunWith(AndroidJUnit4::class)
class CandidateDriftEvidenceTest {

    private fun ctx() = InstrumentationRegistry.getInstrumentation().targetContext
    private val now = "2026-09-18T00:00:00.000Z"

    private fun freshDb(name: String): AndroidSqliteDriver {
        val f = File(ctx().filesDir, "ev-cd-$name.db").apply { if (exists()) delete() }
        val d = AndroidSqliteDriver.open(f, "pw-cd-$name")
        migrate(d, now)
        return d
    }

    private fun graphRevision(db: AndroidSqliteDriver): Int =
        db.prepare("SELECT value FROM meta WHERE key = 'graph_revision'").get()
            ?.str("value")?.toIntOrNull() ?: 0

    // ---------------------------------------------------------------- H-16 Candidate

    @Test
    fun candidateAccept_createsOneNode_withoutRevisionBump() {
        val db = freshDb("accept")
        db.prepare(
            """
            INSERT INTO discovery_candidates
              (id, candidate_kind, display_label, normalized_key, source_instance_id, evidence_refs_json,
               observation_count, first_seen_at, last_seen_at, status, created_at, updated_at)
            VALUES ('cand-1', 'service', '示例服务', 'svc:示例服务', 'src-1', '[]', 2, '$now', '$now', 'pending', '$now', '$now')
            """.trimIndent(),
        ).run()
        val before = graphRevision(db)
        val app = AppContainer.forDriver(db)

        val nodeId = app.acceptCandidate("cand-1")

        // 创建了一个 Node
        val node = db.prepare("SELECT id, kind, name FROM nodes WHERE id = ?").get(nodeId)
        assertNotNull("candidate accept 必须创建 Node", node)
        assertEquals("service", node!!.str("kind"))
        assertEquals("示例服务", node.str("name"))
        // 未确认前不进 Impact：这里没有任何 dependency 被创建
        assertEquals(0L, db.prepare("SELECT COUNT(*) AS c FROM dependencies").get()?.long("c"))
        // candidate_accept ∈ neverBumpsOn：revision 不变
        assertEquals(before, graphRevision(db))
        // 幂等：replay 返回同一 nodeId
        val replay = app.acceptCandidate("cand-1")
        assertEquals(nodeId, replay)
        assertEquals(
            1L,
            db.prepare("SELECT COUNT(*) AS c FROM nodes WHERE id = ?").get(nodeId)?.long("c"),
        )
        // 状态 accepted
        val st = db.prepare("SELECT status FROM discovery_candidates WHERE id = 'cand-1'").get()
        assertEquals(CandidateStatus.ACCEPTED.wire, st!!.str("status"))
        db.close()
    }

    @Test
    fun candidateDismiss_recordsDismissal_withoutRealityMutation() {
        val db = freshDb("dismiss")
        db.prepare(
            """
            INSERT INTO discovery_candidates
              (id, candidate_kind, display_label, normalized_key, source_instance_id, evidence_refs_json,
               observation_count, first_seen_at, last_seen_at, status, created_at, updated_at)
            VALUES ('cand-2', 'service', '待忽略服务', 'svc:待忽略服务', 'src-1', '[]', 3, '$now', '$now', 'pending', '$now', '$now')
            """.trimIndent(),
        ).run()
        val before = graphRevision(db)
        val app = AppContainer.forDriver(db)

        app.dismissCandidate("cand-2")

        assertEquals(0L, db.prepare("SELECT COUNT(*) AS c FROM nodes").get()?.long("c"))
        assertEquals(before, graphRevision(db))
        val st = db.prepare("SELECT status, dismissed_at_observation_count FROM discovery_candidates WHERE id = 'cand-2'").get()
        assertEquals(CandidateStatus.DISMISSED.wire, st!!.str("status"))
        assertEquals(3L, st.long("dismissed_at_observation_count"))
        db.close()
    }

    @Test
    fun candidateNonPending_acceptIsRejected() {
        val db = freshDb("guard")
        db.prepare(
            """
            INSERT INTO discovery_candidates
              (id, candidate_kind, display_label, normalized_key, source_instance_id, evidence_refs_json,
               observation_count, first_seen_at, last_seen_at, status, created_at, updated_at)
            VALUES ('cand-3', 'service', '已忽略服务', 'svc:已忽略服务', 'src-1', '[]', 1, '$now', '$now', 'dismissed', '$now', '$now')
            """.trimIndent(),
        ).run()
        val app = AppContainer.forDriver(db)
        try {
            app.acceptCandidate("cand-3")
            fail("dismissed candidate 不能被 accept（CANDIDATE-PENDING-ONLY）")
        } catch (_: IllegalStateException) {
            // expected
        }
        assertEquals(0L, db.prepare("SELECT COUNT(*) AS c FROM nodes").get()?.long("c"))
        db.close()
    }

    // ---------------------------------------------------------------- H-17 Drift

    private fun seedDriftFixture(db: AndroidSqliteDriver, driftId: String, relatedDep: String) {
        // 三个节点 + 一条旧依赖边（将被 retire）
        db.prepare("INSERT INTO nodes (id, kind, name, owner, created_at, updated_at) VALUES ('n-card', 'payment_card', '旧卡', 'self', '$now', '$now')").run()
        db.prepare("INSERT INTO nodes (id, kind, name, owner, created_at, updated_at) VALUES ('n-new', 'payment_card', '新卡', 'self', '$now', '$now')").run()
        db.prepare("INSERT INTO nodes (id, kind, name, owner, created_at, updated_at) VALUES ('n-svc', 'service', '订阅服务', 'self', '$now', '$now')").run()
        db.prepare(
            """
            INSERT INTO dependencies (id, from_node, relation, to_node, capability, criticality, state,
                                      origin, confirmed_at, last_verified_at, created_at, updated_at)
            VALUES ('$relatedDep', 'n-card', 'funding_source', 'n-svc', 'payment', 'unknown',
                    'active', 'manual', '$now', '$now', '$now', '$now')
            """.trimIndent(),
        ).run()
        db.prepare(
            """
            INSERT INTO reality_drifts
              (id, kind, target_node_id, capability, candidate_from, candidate_relation,
               related_dependency_ids_json, evidence_refs_json, proposal_keys_json, observation_count,
               detected_at, updated_at, status)
            VALUES ('$driftId', 'possible_replacement', 'n-svc', 'payment', 'n-new', 'funding_source',
                    '["$relatedDep"]', '[]', '[]', 2, '$now', '$now', 'open')
            """.trimIndent(),
        ).run()
    }

    @Test
    fun driftResolveReplacement_retiresOld_createsNew_bumpsRevision() {
        val db = freshDb("replace")
        seedDriftFixture(db, "drift-1", "dep-old")
        val before = graphRevision(db)
        val app = AppContainer.forDriver(db)

        app.resolveDriftAsReplacement("drift-1")

        // 旧边被 retire
        val old = db.prepare("SELECT state FROM dependencies WHERE id = 'dep-old'").get()
        assertEquals("retired", old!!.str("state"))
        // 新边 n-new → n-svc
        val newDep = db.prepare(
            "SELECT from_node, to_node, state FROM dependencies WHERE from_node = 'n-new' AND to_node = 'n-svc' AND state = 'active'",
        ).get()
        assertNotNull("替换必须创建 active 新边", newDep)
        // drift → confirmed_change
        val st = db.prepare("SELECT status FROM reality_drifts WHERE id = 'drift-1'").get()
        assertEquals(DriftStatus.CONFIRMED_CHANGE.wire, st!!.str("status"))
        // drift_resolve_replacement ∈ bumpsOn：revision 增加
        assertTrue("resolve replacement 必须 bump revision", graphRevision(db) > before)
        db.close()
    }

    @Test
    fun driftResolveAdditionalPath_keepsOld_createsNew_bumpsRevision() {
        val db = freshDb("additional")
        seedDriftFixture(db, "drift-2", "dep-old2")
        val before = graphRevision(db)
        val app = AppContainer.forDriver(db)

        app.resolveDriftAsAdditionalPath("drift-2")

        // 旧边保持 active（两条边 ≠ fallback，只是记录两条路径）
        val old = db.prepare("SELECT state FROM dependencies WHERE id = 'dep-old2'").get()
        assertEquals("active", old!!.str("state"))
        val newDep = db.prepare(
            "SELECT from_node, to_node FROM dependencies WHERE from_node = 'n-new' AND to_node = 'n-svc' AND state = 'active'",
        ).get()
        assertNotNull(newDep)
        assertTrue("resolve additional path 必须 bump revision", graphRevision(db) > before)
        db.close()
    }

    @Test
    fun driftDismiss_keepsReality_bumpsNothing() {
        val db = freshDb("dismiss-drift")
        seedDriftFixture(db, "drift-3", "dep-old3")
        val before = graphRevision(db)
        val app = AppContainer.forDriver(db)

        app.dismissDrift("drift-3")

        val old = db.prepare("SELECT state FROM dependencies WHERE id = 'dep-old3'").get()
        assertEquals("active", old!!.str("state"))
        assertEquals(before, graphRevision(db))
        val st = db.prepare("SELECT status FROM reality_drifts WHERE id = 'drift-3'").get()
        assertEquals(DriftStatus.DISMISSED.wire, st!!.str("status"))
        db.close()
    }

    @Test
    fun driftNonOpen_resolveIsRejected() {
        val db = freshDb("drift-guard")
        seedDriftFixture(db, "drift-4", "dep-old4")
        db.prepare("UPDATE reality_drifts SET status = 'dismissed' WHERE id = 'drift-4'").run()
        val app = AppContainer.forDriver(db)
        try {
            app.resolveDriftAsReplacement("drift-4")
            fail("非 open drift 不能被 resolve（DRIFT-OPEN-ONLY）")
        } catch (_: IllegalStateException) {
            // expected
        }
        val old = db.prepare("SELECT state FROM dependencies WHERE id = 'dep-old4'").get()
        assertEquals("active", old!!.str("state"))
        db.close()
    }
}
