package com.pdig.desktop

import com.pdig.core.sources.Observation
import com.pdig.core.sources.ObservationDirection
import com.pdig.desktop.data.DesktopSession

/**
 * 共享生成引擎（DiscoveryRepository）的 smoke 证据链步骤（E8 扩展）。
 * 与 SmokeRunner 同包：导入 → 引擎生成候选/漂移 → 用户 accept/resolve 闭环，
 * 断言引擎永不自动确认/建依赖/bump revision。
 *
 * 注：导入提案创建的是 merchant_agreement 边；drift 只对 funding_source 已确认边
 * （卡→服务的资金路径）生效（spec §9），因此本步骤先种一条 funding_source 边。
 */
object SmokeEngineSteps {

    fun run(session: DesktopSession, step: (String, () -> Unit) -> Unit) {
        step("engine-generates-candidate-from-import") {
            // 预建节点（导入会因 node_create bump revision）；随后同商户 2 条新观察。
            val seed = session.sources.previewImport(
                listOf(engineObs("engine-seed-1", "Engine Smoke Co", "Engine Card-1")),
                emptyList(), "manual", "smoke engine seed",
            )
            session.sources.commitImport(seed)
            val revBefore = session.graph.graphRevision()
            val depsBefore = session.graph.dependencies().size
            val preview = session.sources.previewImport(
                listOf(
                    engineObs("engine-txn-1", "Engine Smoke Co", "Engine Card-1"),
                    engineObs("engine-txn-2", "Engine Smoke Co", "Engine Card-1"),
                ),
                emptyList(), "manual", "smoke engine",
            )
            session.sources.commitImport(preview)
            val cand = session.candidates.pendingCandidates().firstOrNull { it.label == "Engine Smoke Co" }
            check(cand != null) { "engine must generate a pending candidate from >=2 observations" }
            check(cand.observationCount == 2) { "candidate observation count must be 2" }
            check(session.graph.dependencies().size == depsBefore) { "candidate generation must not create dependencies" }
            check(session.graph.graphRevision() == revBefore) { "candidate generation must not bump revision" }
            val nodeId = session.candidates.acceptCandidate(cand.id)
            check(session.graph.nodes().any { it.id == nodeId }) { "user accept must create node" }
        }

        step("engine-generates-drift-never-auto-resolves") {
            // 已确认 funding_source 边（卡→服务）+ 新卡 2 条观察 → open drift；
            // 引擎绝不自动 resolve；用户 resolveAsAdditionalPath 才 bump + 建边。
            val confirmedDep = session.graph.dependencies()
                .firstOrNull { it.state == "active" && it.criticality == "required" }
                ?: session.graph.dependencies().first { it.state == "active" }
            val serviceNode = session.graph.nodes().first { it.id == confirmedDep.to }
            val fundingCard = session.graph.nodes().first { it.id == confirmedDep.from }
            val now = "2026-09-01T00:00:00Z"
            session.driver.exec(
                """
                INSERT INTO dependencies (id, from_node, relation, to_node, capability, criticality, state,
                                          origin, confirmed_at, last_verified_at, created_at, updated_at)
                VALUES ('dep-engine-seed', '${fundingCard.id}', 'funding_source', '${serviceNode.id}', 'payment', 'unknown',
                        'active', 'manual', '$now', '$now', '$now', '$now')
                """.trimIndent(),
            )
            // 预建新卡节点（避免"生成导入"新建节点带来的 revision 变化）。
            val cardSeed = session.sources.previewImport(
                listOf(engineObs("engine-card2-seed", "Other Co 2", "Engine Card-2")),
                emptyList(), "manual", "smoke engine card2 seed",
            )
            session.sources.commitImport(cardSeed)
            val revBefore = session.graph.graphRevision()
            val depsBefore = session.graph.dependencies().size
            val preview = session.sources.previewImport(
                listOf(
                    engineObs("engine-drift-1", serviceNode.name, "Engine Card-2"),
                    engineObs("engine-drift-2", serviceNode.name, "Engine Card-2"),
                ),
                emptyList(), "manual", "smoke engine drift",
            )
            session.sources.commitImport(preview)
            val drift = session.drifts.openDrifts().firstOrNull {
                it.targetNodeId == serviceNode.id && it.kind == "possible_additional_path"
            }
            check(drift != null) { "engine must generate an open drift for a new funding source" }
            check(session.graph.dependencies().size == depsBefore) { "drift detection must not create dependencies" }
            check(session.graph.graphRevision() == revBefore) { "drift detection must not bump revision" }
            session.drifts.resolveDriftAsAdditionalPath(drift.id)
            check(
                session.graph.dependencies().any { it.to == serviceNode.id && it.from != confirmedDep.from },
            ) { "user resolve must create the new funding edge" }
            check(session.graph.graphRevision() > revBefore) { "resolve additional path must bump revision" }
        }
    }

    private fun engineObs(txnId: String, merchant: String, card: String): Observation = Observation(
        source = "manual",
        sourceTxnId = txnId,
        merchantTxnId = "",
        occurredAt = "2026-09-01T00:00:00Z",
        merchantRaw = merchant,
        description = "smoke engine txn",
        amount = 12.0,
        currency = "CNY",
        direction = ObservationDirection.OUT,
        paymentMethodRaw = card,
        status = "",
        note = "",
    )
}
