package com.pdig.core.scenario

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ScenarioSubjectContractTest {
    @Test
    fun paymentScenariosRequirePaymentInstrumentTargets() {
        listOf(
            "replace_payment_card",
            "expiring_payment_card",
            "close_payment_instrument",
        ).forEach { id ->
            val template = ScenarioRegistry.get(id) ?: error("missing scenario $id")
            assertEquals("payment_instrument", template.subjectKind)
            assertNull(template.subjectSubtype)
        }
    }

    @Test
    fun replacePhoneRequiresGovernedPhoneIdentityTarget() {
        val template = ScenarioRegistry.get("replace_phone_number")
            ?: error("missing replace_phone_number")

        assertEquals("identity_anchor", template.subjectKind)
        assertEquals("phone_number", template.subjectSubtype)
    }
}
