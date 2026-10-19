package com.pdig.app.data

import com.pdig.core.domain.ActionVerification
import com.pdig.core.domain.ActionVerificationMethod
import com.pdig.core.domain.ActionVerificationStatus
import com.pdig.core.domain.PlanAction
import com.pdig.core.generated.PlanActionPhase
import kotlin.test.Test
import kotlin.test.assertFalse
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
