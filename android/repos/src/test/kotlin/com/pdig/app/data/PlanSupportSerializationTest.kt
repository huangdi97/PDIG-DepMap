package com.pdig.app.data

import com.pdig.core.domain.ActionVerification
import com.pdig.core.domain.ActionVerificationMethod
import com.pdig.core.domain.ActionVerificationStatus
import com.pdig.core.domain.PlanAction
import com.pdig.core.generated.PlanActionPhase
import com.pdig.core.json.Json
import com.pdig.core.json.JsonWriter
import kotlin.test.Test
import kotlin.test.assertEquals

class PlanSupportSerializationTest {
    @Test
    fun actionRoundTripPreservesFutureObservationMatchingEndpoints() {
        val action = PlanAction(
            id = "observe-new-recovery",
            title = "等待新恢复路径证据",
            phase = PlanActionPhase.VERIFY,
            done = true,
            doneAt = "2026-10-19T13:29:00Z",
            resolvesImpactKeys = listOf("account-1|recovery"),
            prerequisiteActionIds = listOf("change-1"),
            verification = ActionVerification(
                method = ActionVerificationMethod.FUTURE_OBSERVATION,
                status = ActionVerificationStatus.EVIDENCE_SUGGESTED,
                verifiedAt = "2026-10-19T13:29:30Z",
                evidenceRefs = listOf("ev-1", "ev-2"),
                expectedFromNodeId = "phone-new",
                expectedToNodeId = "account-1",
            ),
        )

        val raw = JsonWriter.write(Json.Arr(listOf(planActionToJson(action))))
        val restored = planActionsFromJson(raw).single()

        assertEquals(action.id, restored.id)
        assertEquals(action.done, restored.done)
        assertEquals(action.doneAt, restored.doneAt)
        assertEquals(action.resolvesImpactKeys, restored.resolvesImpactKeys)
        assertEquals(action.prerequisiteActionIds, restored.prerequisiteActionIds)
        assertEquals(action.verification?.method, restored.verification?.method)
        assertEquals(action.verification?.status, restored.verification?.status)
        assertEquals(action.verification?.verifiedAt, restored.verification?.verifiedAt)
        assertEquals(action.verification?.evidenceRefs, restored.verification?.evidenceRefs)
        assertEquals("phone-new", restored.verification?.expectedFromNodeId)
        assertEquals("account-1", restored.verification?.expectedToNodeId)
    }

    @Test
    fun actionRoundTripKeepsAbsentMatchingEndpointsAbsent() {
        val action = PlanAction(
            id = "manual-verify",
            title = "人工确认",
            phase = PlanActionPhase.VERIFY,
            verification = ActionVerification(
                method = ActionVerificationMethod.MANUAL_CONFIRMATION,
                status = ActionVerificationStatus.PENDING,
            ),
        )

        val raw = JsonWriter.write(Json.Arr(listOf(planActionToJson(action))))
        val restored = planActionsFromJson(raw).single()

        assertEquals(null, restored.verification?.expectedFromNodeId)
        assertEquals(null, restored.verification?.expectedToNodeId)
    }
}
