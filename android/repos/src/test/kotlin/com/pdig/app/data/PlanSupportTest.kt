package com.pdig.app.data

import com.pdig.core.domain.ActionVerification
import com.pdig.core.domain.ActionVerificationMethod
import com.pdig.core.domain.ActionVerificationStatus
import com.pdig.core.domain.PlanAction
import com.pdig.core.generated.Capability
import com.pdig.core.generated.PlanActionPhase
import com.pdig.core.generated.PlanReadiness
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PlanSupportTest {
    private fun verificationAction(
        done: Boolean,
        status: ActionVerificationStatus,
    ) = PlanAction(
        id = "rpn-verify-1",
        title = "验证新手机号恢复路径",
        phase = PlanActionPhase.VERIFY,
        done = done,
        verification = ActionVerification(
            method = ActionVerificationMethod.FUTURE_OBSERVATION,
            status = status,
        ),
    )

    @Test
    fun scenarioImpactScopeUsesDeclaredCapabilitiesAndRejectsUnknownScenarios() {
        assertEquals(
            listOf(
                Capability.ACCESS,
                Capability.AUTHENTICATION,
                Capability.RECOVERY,
                Capability.COMMUNICATION,
            ),
            scenarioImpactCapabilities("replace_phone_number"),
        )
        assertEquals(
            listOf(Capability.PAYMENT),
            scenarioImpactCapabilities("replace_payment_card"),
        )
        assertFailsWith<IllegalArgumentException> {
            scenarioImpactCapabilities("unknown_future_scenario")
        }
    }

    @Test
    fun newlyDiscoveredPhoneImpactsNeverBindToRetirementStep() {
        val actions = buildReplacePhoneActions().map {
            if (it.id == "rpn-change-1") {
                it.copy(
                    done = true,
                    resolvesImpactKeys = listOf("account-a|recovery"),
                )
            } else {
                it
            }
        }

        val projected = claimScenarioMustChangeKeys(
            scenario = "replace_phone_number",
            actions = actions,
            mustChangeKeys = listOf(
                "account-a|recovery",
                "account-b|authentication",
            ),
        )

        assertTrue(
            projected.first { it.id == "rpn-change-2" }.resolvesImpactKeys.isEmpty(),
            "retirement must never claim newly discovered migration work",
        )
        assertFalse(
            projected.any { "account-b|authentication" in it.resolvesImpactKeys },
            "new impact must remain explicitly unresolved when the migration step is already done",
        )
    }

    @Test
    fun phoneMigrationStepClaimsInitialImpactsButRetirementDoesNot() {
        val projected = claimScenarioMustChangeKeys(
            scenario = "replace_phone_number",
            actions = buildReplacePhoneActions(),
            mustChangeKeys = listOf("account-a|recovery"),
        )
        assertEquals(
            listOf("account-a|recovery"),
            projected.first { it.id == "rpn-change-1" }.resolvesImpactKeys,
        )
        assertTrue(projected.first { it.id == "rpn-change-2" }.resolvesImpactKeys.isEmpty())
    }

    @Test
    fun blockedPlanAllowsOnlyPreparationOrChangeThatResolvesCurrentBlocker() {
        val prepare = PlanAction(
            id = "prepare",
            phase = PlanActionPhase.PREPARE,
        )
        val resolving = PlanAction(
            id = "change-resolving",
            phase = PlanActionPhase.CHANGE,
            resolvesImpactKeys = listOf("account-a|recovery"),
        )
        val unrelated = PlanAction(
            id = "change-unrelated",
            phase = PlanActionPhase.CHANGE,
            resolvesImpactKeys = listOf("service-x|payment"),
        )
        val verify = PlanAction(
            id = "verify",
            phase = PlanActionPhase.VERIFY,
        )

        assertTrue(planActionCompletionAllowedByScope(
            PlanReadiness.BLOCKED, prepare, listOf("account-a|recovery"),
        ))
        assertTrue(planActionCompletionAllowedByScope(
            PlanReadiness.BLOCKED, resolving, listOf("account-a|recovery"),
        ))
        assertFalse(planActionCompletionAllowedByScope(
            PlanReadiness.BLOCKED, unrelated, listOf("account-a|recovery"),
        ))
        assertFalse(planActionCompletionAllowedByScope(
            PlanReadiness.BLOCKED, verify, listOf("account-a|recovery"),
        ))
        assertTrue(planActionCompletionAllowedByScope(
            PlanReadiness.REVIEW_REQUIRED, unrelated, listOf("account-a|recovery"),
        ))
    }

    @Test
    fun retirementRequiresCompletedAndVerifiedPhonePath() {
        assertFalse(
            phoneRetirementVerificationSatisfied(
                "replace_phone_number",
                "rpn-change-2",
                listOf(verificationAction(false, ActionVerificationStatus.PENDING)),
            ),
        )
        assertFalse(
            phoneRetirementVerificationSatisfied(
                "replace_phone_number",
                "rpn-change-2",
                listOf(verificationAction(true, ActionVerificationStatus.PENDING)),
            ),
        )
        assertFalse(
            phoneRetirementVerificationSatisfied(
                "replace_phone_number",
                "rpn-change-2",
                listOf(verificationAction(true, ActionVerificationStatus.FAILED)),
            ),
        )
        assertTrue(
            phoneRetirementVerificationSatisfied(
                "replace_phone_number",
                "rpn-change-2",
                listOf(verificationAction(true, ActionVerificationStatus.VERIFIED)),
            ),
        )
    }

    @Test
    fun retirementGateFailsClosedWhenVerificationStepIsMissing() {
        assertFalse(
            phoneRetirementVerificationSatisfied(
                "replace_phone_number",
                "rpn-change-2",
                emptyList(),
            ),
        )
    }

    @Test
    fun unrelatedScenariosAndActionsAreNotBlocked() {
        assertTrue(
            phoneRetirementVerificationSatisfied(
                "replace_payment_card",
                "rpn-change-2",
                emptyList(),
            ),
        )
        assertTrue(
            phoneRetirementVerificationSatisfied(
                "replace_phone_number",
                "rpn-change-1",
                emptyList(),
            ),
        )
    }
}
