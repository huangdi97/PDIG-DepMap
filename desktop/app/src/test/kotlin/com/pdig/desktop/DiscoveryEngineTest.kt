package com.pdig.desktop

import com.pdig.app.data.DriftRow
import com.pdig.core.sources.Observation
import com.pdig.core.sources.ObservationDirection
import com.pdig.desktop.data.DesktopSession
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 共享生成引擎（DiscoveryRepository）集成证据 —— 与 Android 共用同一 :repos 代码。
 *
 * 断言锚点（spec/domain/invariants.md）：
 *  - PC-01/PC-02：candidate 不进 Impact、不 bump revision；
 *  - PC-04：dismiss 后需 ≥ 2 条新观测才回到 pending；
 *  - PC-05：accepted 候选不再被同名信号打扰；
 *  - DR-02/DR-03/DR-04/DR-07：已确认来源忽略；<2 观测忽略；同 key 单 drift 累计；不自动 resolve；
 *  - DR-05：跨导入重复 evidence 不重复计数。
 *
 * 注：导入本身因"新建节点"会 bump revision（node_create ∈ bumpsOn，spec §27）；
 * 本类所有"生成不 bump"的断言都构造为：节点已存在后的导入（无新节点），
 * 因此 revision 变化只可能来自生成引擎 —— 这是对"引擎不 bump"的直接证明。
 */
class DiscoveryEngineTest {

    private fun obs(
        txnId: String,
        merchant: String,
        card: String,
    ) = Observation(
        source = "manual",
        sourceTxnId = txnId,
        merchantTxnId = "",
        occurredAt = "2026-09-01T00:00:00Z",
        merchantRaw = merchant,
        description = "",
        amount = 10.0,
        currency = "CNY",
        direction = ObservationDirection.OUT,
        paymentMethodRaw = card,
        status = "",
        note = "",
    )

    private fun DesktopSession.import(rows: List<Observation>, label: String) {
        val preview = sources.previewImport(rows, emptyList(), "manual", label)
        sources.commitImport(preview)
    }

    private fun graphRevision(s: DesktopSession): Long =
        s.driver.prepare("SELECT value FROM meta WHERE key = 'graph_revision'").get()
            ?.str("value")?.toLongOrNull() ?: 0L

    private fun depCount(s: DesktopSession): Long =
        s.driver.prepare("SELECT COUNT(*) AS c FROM dependencies").get()?.long("c") ?: 0L

    /** 确认一条 funding_source 依赖（测试造数；等价用户确认的 active 边）。 */
    private fun DesktopSession.seedConfirmedDependency(depId: String, from: String, to: String) {
        val now = "2026-09-01T00:00:00Z"
        driver.exec(
            """
            INSERT INTO dependencies (id, from_node, relation, to_node, capability, criticality, state,
                                      origin, confirmed_at, last_verified_at, created_at, updated_at)
            VALUES ('$depId', '$from', 'funding_source', '$to', 'payment', 'unknown',
                    'active', 'manual', '$now', '$now', '$now', '$now')
            """.trimIndent(),
        )
    }

    @Test
    fun thresholdMet_importGeneratesPendingCandidate_withoutRevisionOrImpact() {
        val s = DesktopSession.open()
        // 先建节点（1 条观察）：导入会因 node_create bump revision。
        s.import(listOf(obs("t0", "Svc Co", "卡A-8888")), "eng-cand-0")
        val before = graphRevision(s)
        // 同商户 2 条新正证据（节点已存在）→ 生成候选；生成本身不得 bump。
        s.import(listOf(obs("t1", "Svc Co", "卡A-8888"), obs("t2", "Svc Co", "卡A-8888")), "eng-cand-1")
        val cand = s.candidates.pendingCandidates().firstOrNull { it.label == "Svc Co" }
        assertTrue(cand != null, "≥2 正证据必须生成候选")
        assertEquals(2, cand!!.observationCount)
        // PC-01/PC-02：candidate 不进 Impact、不 bump revision、不建依赖。
        assertEquals(0L, depCount(s), "candidate 生成不得创建依赖")
        assertEquals(before, graphRevision(s), "候选生成不得 bump revision")
        s.close()
    }

    @Test
    fun insufficientEvidence_generatesNoCandidate() {
        val s = DesktopSession.open()
        s.import(listOf(obs("t1", "One Off", "卡A-8888")), "eng-cand-2")
        assertTrue(s.candidates.pendingCandidates().none { it.label == "One Off" }, "1 条观测不达阈值")
        s.close()
    }

    @Test
    fun duplicateImport_upsertsSingleCandidate_withoutDoubleCounting() {
        val s = DesktopSession.open()
        val rows = listOf(obs("t1", "Svc Co", "卡A-8888"), obs("t2", "Svc Co", "卡A-8888"))
        s.import(rows, "eng-cand-3a")
        s.import(rows, "eng-cand-3b") // 同 fingerprint → 重复证据
        val count = s.driver.prepare(
            "SELECT COUNT(*) AS c FROM discovery_candidates WHERE normalized_key = ?",
        ).get("svc:svc co")?.long("c") ?: 0L
        assertEquals(1L, count, "同 key 不得重复建候选（DR-04/唯一索引）")
        val cand = s.candidates.pendingCandidates().first { it.label == "Svc Co" }
        assertEquals(2, cand.observationCount, "跨导入重复 evidence 不得重复计数（DR-05）")
        s.close()
    }

    @Test
    fun dismissedCandidate_requiresTwoNewObservationsToRepropose() {
        val s = DesktopSession.open()
        s.import(listOf(obs("t1", "Svc Co", "卡A-8888"), obs("t2", "Svc Co", "卡A-8888")), "eng-cand-4a")
        val cand = s.candidates.pendingCandidates().first { it.label == "Svc Co" }
        s.candidates.dismissCandidate(cand.id)
        assertEquals("dismissed", s.driver.prepare("SELECT status FROM discovery_candidates WHERE id = ?")
            .get(cand.id)?.str("status"))

        // 仅 1 条新观测 → 证据累计但保持 dismissed（PC-04）
        s.import(listOf(obs("t3", "Svc Co", "卡A-8888")), "eng-cand-4b")
        assertEquals("dismissed", s.driver.prepare("SELECT status FROM discovery_candidates WHERE id = ?")
            .get(cand.id)?.str("status"))

        // 再 2 条新观测 → 回到 pending
        s.import(listOf(obs("t4", "Svc Co", "卡A-8888"), obs("t5", "Svc Co", "卡A-8888")), "eng-cand-4c")
        assertEquals("pending", s.driver.prepare("SELECT status FROM discovery_candidates WHERE id = ?")
            .get(cand.id)?.str("status"))
        val reproposed = s.candidates.pendingCandidates().first { it.label == "Svc Co" }
        assertEquals(5, reproposed.observationCount, "累计计数 = 2 旧 + 3 新")
        s.close()
    }

    @Test
    fun acceptedCandidate_notDisturbedBySameNameSignals() {
        val s = DesktopSession.open()
        s.import(listOf(obs("t1", "Svc Co", "卡A-8888"), obs("t2", "Svc Co", "卡A-8888")), "eng-cand-5a")
        val cand = s.candidates.pendingCandidates().first { it.label == "Svc Co" }
        val nodeId = s.candidates.acceptCandidate(cand.id)
        assertTrue(nodeId.isNotEmpty())

        s.import(listOf(obs("t3", "Svc Co", "卡A-8888"), obs("t4", "Svc Co", "卡A-8888")), "eng-cand-5b")
        val st = s.driver.prepare("SELECT status, observation_count FROM discovery_candidates WHERE id = ?")
            .get(cand.id)
        assertEquals("accepted", st?.str("status"), "accepted 候选不得被新信号打扰（PC-05）")
        assertEquals(2L, st?.long("observation_count"), "accepted 候选计数不得被累计")
        s.close()
    }

    @Test
    fun drift_generatedOnlyWithConfirmedReality_noAutoResolve() {
        val s = DesktopSession.open()
        // 节点：卡A / Svc Co；随后确认卡A→Svc 依赖。
        s.import(listOf(obs("t0", "Svc Co", "卡A-8888")), "eng-drift-1a")
        val svc = s.graph.nodes().first { it.name == "Svc Co" }.id
        val cardA = s.graph.nodes().first { it.name == "卡A-8888" }.id
        s.seedConfirmedDependency("dep-seed", cardA, svc)
        // 预建卡B 节点（避免"生成导入"里新建节点带来的 revision 变化）。
        s.import(listOf(obs("t1", "Other Co", "卡B-6666")), "eng-drift-1pre")
        val cardB = s.graph.nodes().first { it.name == "卡B-6666" }.id
        val before = graphRevision(s)

        // 卡B 支付 Svc Co（2 条）→ 生成 possible_additional_path drift。
        s.import(listOf(obs("t2", "Svc Co", "卡B-6666"), obs("t3", "Svc Co", "卡B-6666")), "eng-drift-1b")
        val drifts: List<DriftRow> = s.drifts.openDrifts().filter { it.candidateFrom == cardB }
        assertTrue(drifts.isNotEmpty(), "已确认来源 + 新卡信号 → 必须生成 open drift")
        assertEquals("possible_additional_path", drifts.first().kind)
        assertEquals(svc, drifts.first().targetNodeId)
        assertEquals(2, drifts.first().observationCount)

        // DR-07：检测不自动 resolve、不改 Reality、不 bump。
        assertEquals(before, graphRevision(s), "drift 检测不得 bump revision")
        assertEquals(1L, depCount(s), "drift 检测不得创建依赖")
        assertTrue(s.drifts.openDrifts().any { it.id == drifts.first().id }, "drift 必须保持 open")

        // 用户路径：resolveAsAdditionalPath → bump + 建边（既有语义，验证链路闭环）
        s.drifts.resolveDriftAsAdditionalPath(drifts.first().id)
        assertTrue(graphRevision(s) > before, "resolve additional path 必须 bump revision")
        assertEquals(2L, depCount(s))
        s.close()
    }

    @Test
    fun drift_notGeneratedWhenAlreadyConfirmed() {
        val s = DesktopSession.open()
        s.import(listOf(obs("t0", "Svc Co", "卡A-8888")), "eng-drift-2a")
        val svc = s.graph.nodes().first { it.name == "Svc Co" }.id
        val cardA = s.graph.nodes().first { it.name == "卡A-8888" }.id
        s.seedConfirmedDependency("dep-seed2", cardA, svc)
        val before = graphRevision(s)
        // 已确认来源（卡A→Svc）的再观察 → DR-02 忽略，不新建 drift。
        s.import(listOf(obs("t2", "Svc Co", "卡A-8888"), obs("t3", "Svc Co", "卡A-8888")), "eng-drift-2b")
        assertTrue(s.drifts.openDrifts().isEmpty(), "已确认来源不得生成 drift（DR-02）")
        assertEquals(before, graphRevision(s), "被忽略的信号不得产生任何变化")
        s.close()
    }

    @Test
    fun drift_notGeneratedWithoutConfirmedReality() {
        val s = DesktopSession.open()
        // Svc Co 没有任何已确认 funding 依赖 → 新卡信号走 candidate 路径，不生成 drift。
        s.import(listOf(obs("t1", "Svc Co", "卡B-6666"), obs("t2", "Svc Co", "卡B-6666")), "eng-drift-3")
        assertTrue(s.drifts.openDrifts().isEmpty(), "无已确认 Reality 时不得生成 drift（保守）")
        assertTrue(s.candidates.pendingCandidates().any { it.label == "Svc Co" }, "新实体走 candidate 路径")
        s.close()
    }
}
