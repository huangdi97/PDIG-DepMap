package com.pdig.desktop

import com.pdig.app.data.ScenarioPlanRequest
import com.pdig.core.generated.ChangePlanWorkflowState
import com.pdig.core.sources.WechatParser
import com.pdig.desktop.data.DesktopSession
import com.pdig.desktop.security.DeviceUnlockStore
import com.pdig.desktop.security.InMemorySecurityPort
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Desktop 会话级行为（复用 :repos，语义与 Android 一致）：
 * fixture 导入 → proposal → 影响 → 计划闭环 + revision 规则 + 解锁存储。
 */
class DesktopSessionTest {

    @TempDir
    lateinit var dir: File

    private val fixture: File
        get() = File("../../fixtures/import/normal-wechat.csv")

    @Test
    fun importProposalImpactPlanClosesTheLoop() {
        val session = DesktopSession.open()
        val parsed = WechatParser.parse(fixture.readBytes())
        assertTrue(parsed.observations.isNotEmpty())
        val preview = session.sources.previewImport(parsed.observations, emptyList(), "wechat_statement", "test wechat")
        val commit = session.sources.commitImport(preview)
        assertTrue(commit.nodeCount >= 1)
        assertTrue(commit.proposalCount >= 1)

        val before = session.graph.graphRevision()
        val pending = session.proposals.pendingProposals()
        assertTrue(pending.isNotEmpty())
        session.proposals.acceptProposal(pending.first().id)
        assertTrue(session.graph.graphRevision() > before, "proposal accept must bump graphRevision")

        val dep = session.graph.dependencies().first()
        assertEquals("unknown", dep.criticality, "machine-created dependency must be criticality=unknown")
        session.graph.setDependencyCriticality(dep.id, required = true)
        assertEquals("required", session.graph.dependencies().first { it.id == dep.id }.criticality)

        val impact = session.graph.impactFor(dep.from)
        assertTrue(impact.targets.any { it.status.wire == "must_change" }, "required edge without alternative must be must_change")
        session.close()
    }

    @Test
     fun planWorkflowKeepsDoneNotVerified() {
         val session = DesktopSession.open()
         val parsed = WechatParser.parse(fixture.readBytes())
         val preview = session.sources.previewImport(parsed.observations, emptyList(), "wechat_statement", "test")
         session.sources.commitImport(preview)
         val pending = session.proposals.pendingProposals()
         session.proposals.acceptProposal(pending.first().id)
         val dep = session.graph.dependencies().first()
         session.graph.setDependencyCriticality(dep.id, required = true)
         val target = session.graph.nodes().first { it.id == dep.from }
         val planId = session.plans.createPlanForScenario(
             ScenarioPlanRequest(scenarioId = "replace_payment_card", targetNodeId = target.id),
         )
         val detail = checkNotNull(session.plans.planDetail(planId))
         assertTrue(detail.actions.isNotEmpty())
         val change = detail.actions.first { it.resolvesImpactKeys.isNotEmpty() }
         session.plans.completeAction(planId, change.id)
         val after = checkNotNull(session.plans.planDetail(planId))
         val updated = after.actions.first { it.id == change.id }
         assertTrue(updated.done)
         assertEquals("pending", updated.verification?.status?.wire, "done must NOT imply verified")
         assertNotEquals(ChangePlanWorkflowState.COMPLETED, after.workflowState)
         session.plans.verifyAction(planId, change.id)
         val verified = checkNotNull(session.plans.planDetail(planId))
         assertEquals("verified", verified.actions.first { it.id == change.id }.verification?.status?.wire)
         session.close()
     }

    @Test
    fun deviceUnlockStoreRememberRecallForget() {
        val store = DeviceUnlockStore(InMemorySecurityPort(), File(dir, "pdig"))
        assertTrue(!store.enabled())
        store.remember("master-pw")
        assertTrue(store.enabled())
        assertEquals("master-pw", store.recall())
        store.forget()
        assertTrue(!store.enabled())
        assertEquals(null, store.recall())
    }
}