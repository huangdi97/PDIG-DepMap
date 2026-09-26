package com.pdig.core.domain

import com.pdig.core.generated.Capability
import com.pdig.core.generated.Criticality
import com.pdig.core.generated.GroupMode
import com.pdig.core.generated.NodeKind
import com.pdig.core.generated.Relation
import com.pdig.core.plan.computePlanReadiness
import com.pdig.core.statemachine.CandidateMachine
import com.pdig.core.statemachine.DriftMachine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 领域不变量测试。
 *
 * 这些不变量是 PDIG 的立身之本 —— 它们决定"机器能不能替用户下结论"。
 * 每条测试都对应一条明确的红线，而不是顺手断言。
 */
class DomainInvariantTest {

    // ---------------- 机器永不产生 required ----------------

    @Test
    fun criticalityOnlyHasRequiredAndUnknown() {
        assertEquals(
            listOf(Criticality.REQUIRED, Criticality.UNKNOWN).map { it.wire },
            Criticality.entries.map { it.wire },
        )
    }

    @Test
    fun allRelationDefinitionsDefaultToUnknown() {
        RELATION_DEFINITIONS.forEach { def ->
            assertEquals(
                Criticality.UNKNOWN, def.defaultCriticality,
                "relation ${def.id.wire} 的默认 criticality 必须是 unknown —— 机器永不默认 required",
            )
        }
    }

    @Test
    fun unknownCannotAutoBecomeRequired() {
        // 只有 required / unknown 两个取值，且默认 unknown ⇒ 没有"自动升级"的取值空间
        val from = Criticality.fromWire("unknown")
        assertEquals(Criticality.UNKNOWN, from)
        assertEquals(Criticality.REQUIRED, Criticality.fromWire("required"))
        assertEquals(null, Criticality.fromWire("auto"))
    }

    // ---------------- Proposal ≠ Reality ----------------

    @Test
    fun pendingProposalsLowerReadinessNeverRaiseIt() {
        val plan = ChangePlan(id = "p1", lastAnalyzedGraphRevision = 1)
        val withProposals = computePlanReadiness(
            PlanReadinessInput(
                plan = plan,
                currentGraphRevision = 1,
                pendingMustChange = 0,
                pendingNeedsReview = 0,
                unresolvedCandidates = 0,
                pendingRelevantProposals = 2,
                staleRelevantDependencies = 0,
                unfinishedChangeActions = 0,
            ),
        )
        assertEquals(
            com.pdig.core.generated.PlanReadiness.REVIEW_REQUIRED,
            withProposals,
            "未确认的 Proposal 只能拉低 readiness，绝不能提升",
        )
    }

    // ---------------- done ≠ verified ----------------

    @Test
    fun doneIsNotVerified() {
        val doneButUnverified = PlanAction(
            id = "a1",
            phase = com.pdig.core.generated.PlanActionPhase.CHANGE,
            done = true,
            resolvesImpactKeys = listOf("svc-a|payment"),
            verification = ActionVerification(
                method = ActionVerificationMethod.MANUAL_CONFIRMATION,
                status = ActionVerificationStatus.PENDING,
            ),
        )
        assertTrue(doneButUnverified.done)
        assertEquals(
            ActionVerificationStatus.PENDING,
            doneButUnverified.verification!!.status,
            "动作完成不得自动变成已验证",
        )
    }

    @Test
    fun resolutionCountsDoneNotVerification() {
        val doneUnverified = PlanAction(
            id = "a1",
            phase = com.pdig.core.generated.PlanActionPhase.CHANGE,
            done = true,
            resolvesImpactKeys = listOf("svc-a|payment"),
            verification = ActionVerification(
                method = ActionVerificationMethod.MANUAL_CONFIRMATION,
                status = ActionVerificationStatus.PENDING,
            ),
        )
        // readiness 的"必须处理是否解决"只看 done；verification 是另一条独立轴
        val unresolved = com.pdig.core.plan.countUnresolvedMustChange(
            listOf(doneUnverified),
            listOf("svc-a|payment"),
        )
        assertEquals(0, unresolved, "已完成的声明动作解决该 requirement")
        assertEquals(
            ActionVerificationStatus.PENDING,
            doneUnverified.verification!!.status,
            "但验证状态仍独立保持 pending",
        )
    }

    // ---------------- Candidate ≠ Node ----------------

    @Test
    fun candidateIsNotANodeUntilAccepted() {
        assertEquals(
            listOf("create exactly one Node"),
            CandidateMachine.accept.effects,
            "候选被接受才会创建对象",
        )
        assertFalse(
            CandidateMachine.accept.bumpsGraphRevision,
            "候选接受不 bump revision：它记录的是'出现了一个对象'，不是依赖事实变更",
        )
    }

    // ---------------- Drift ≠ Reality mutation ----------------

    @Test
    fun driftDetectionDoesNotMutateReality() {
        assertEquals("positive evidence only", DriftMachine.creationRule.requires)
        assertEquals(2, DriftMachine.creationRule.minObservations)
        assertTrue(DriftMachine.creationRule.absenceOnly.contains("never"))
    }

    // ---------------- Group requires confirmation ----------------

    @Test
    fun groupUseIsValidatedAgainstTheRuntimeRegistry() {
        // funding_source 允许 Group，但只允许 ANY 模式
        assertTrue(validateRelationGroupUse("funding_source", GroupMode.ANY).ok)
        assertFalse(
            validateRelationGroupUse("funding_source", GroupMode.ALL).ok,
            "funding_source 不允许 ALL 模式",
        )
        // merchant_agreement 不允许 Group
        assertFalse(
            validateRelationGroupUse("merchant_agreement", GroupMode.ANY).ok,
            "merchant_agreement 不允许组合",
        )
        assertFalse(validateRelationGroupUse("bound_to", GroupMode.ANY).ok)
    }

    // ---------------- Relation 治理 ----------------

    @Test
    fun runtimeRegistryOnlyContainsSupportedRelations() {
        // v0.3.0：recovers / authenticates / controls 进入 runtime；
        // verifies / bound_to 等 legacy 词表仍不得进入运行时校验（与 core/src/domain/relation-registry.ts 同口径）。
        assertEquals(
            setOf("funding_source", "merchant_agreement", "recovers", "authenticates", "controls"),
            listRuntimeRelationIds().toSet(),
            "runtime registry 只承认五个 relation；verifies/bound_to 等 legacy 词表不得进入运行时校验",
        )
    }

    @Test
    fun relationUseIsValidated() {
        assertTrue(
            validateRelationUse(
                NodeKind.PAYMENT_INSTRUMENT, "merchant_agreement", NodeKind.SERVICE, "payment",
            ).ok,
        )
        assertFalse(
            validateRelationUse(NodeKind.PAYMENT_INSTRUMENT, "bound_to", NodeKind.SERVICE, "payment").ok,
            "registry 不承认的 relation 必须被拒绝",
        )
        assertFalse(
            validateRelationUse(NodeKind.PAYMENT_INSTRUMENT, "merchant_agreement", NodeKind.SERVICE, "access").ok,
            "capability 不匹配必须被拒绝",
        )
        assertFalse(
            validateRelationUse(NodeKind.DEVICE, "merchant_agreement", NodeKind.SERVICE, "payment").ok,
            "fromKind 不允许时必须被拒绝",
        )
        assertTrue(
            validateRelationUse(null, "merchant_agreement", null, "payment").ok,
            "kind 未知时放行（只校验 relation + capability）",
        )
    }

    // ---------------- logical key / groupKey 确定性 ----------------

    @Test
    fun logicalKeyIsCrossPlatformStable() {
        assertEquals(
            "card|merchant_agreement|svc|payment",
            dependencyLogicalKey("card", Relation.MERCHANT_AGREEMENT, "svc", Capability.PAYMENT),
        )
    }

    @Test
    fun groupKeyIsIndependentOfMemberOrder() {
        val a = canonicalGroupKey("svc", Capability.PAYMENT, GroupMode.ANY, listOf("e2", "e1"))
        val b = canonicalGroupKey("svc", Capability.PAYMENT, GroupMode.ANY, listOf("e1", "e2"))
        assertEquals(a, b, "成员顺序无关（spec §4）")
    }

    @Test
    fun groupKeyDeduplicatesMembers() {
        val a = canonicalGroupKey("svc", Capability.PAYMENT, GroupMode.ANY, listOf("e1", "e1", "e2"))
        val b = canonicalGroupKey("svc", Capability.PAYMENT, GroupMode.ANY, listOf("e1", "e2"))
        assertEquals(a, b)
    }

    @Test
    fun impactStateKeyRoundTrips() {
        val key = ImpactStateKey("svc-a", Capability.PAYMENT)
        val parsed = ImpactStateKey.parse(key.keyString())
        assertEquals(key, parsed)
        assertEquals("svc-a|payment", key.keyString())
    }
}
