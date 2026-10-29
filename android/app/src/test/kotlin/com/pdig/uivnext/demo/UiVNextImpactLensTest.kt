package com.pdig.uivnext.demo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UiVNextImpactLensTest {
    @Test
    fun CardImpactUsesOnlyConfirmedFixtureRelationsAndFindings() {
        val impact = cardImpactLens("card-cn-2")
        assertEquals(UiVNextDemoFixture.servicesForCard("card-cn-2").size,
            impact.confirmedDependencies)
        assertEquals(
            UiVNextDemoFixture.attentionItems.count { it.target == "card-cn-2" },
            impact.attentionFindings,
        )
        assertNull(impact.criticalAccounts)
        assertNull(impact.independentAlternatives)
        assertEquals(UiImpactTruth.UNKNOWN, impact.uniqueRecoveryPath)
        assertTrue(impact.unknownRelationsRemain)
    }

    @Test
    fun ExplicitUniquenessEvidenceCanConfirmPositiveButNeverInventAlternatives() {
        val impact = numberImpactLens("num-cn-1")
        assertEquals(UiImpactTruth.CONFIRMED, impact.uniqueRecoveryPath)
        assertNull(impact.criticalAccounts)
        assertNull(impact.independentAlternatives)
        assertTrue(impact.unknownRelationsRemain)
    }

    @Test
    fun RecoveryUseAloneDoesNotBecomeUniqueRecoveryEvidence() {
        val impact = numberImpactLens("num-us-1")
        assertEquals(UiImpactTruth.UNKNOWN, impact.uniqueRecoveryPath)
        assertTrue(impact.unknownRelationsRemain)
    }

    @Test
    fun SecondaryObjectImpactLensesRemainEvidenceBounded() {
        val account = accountImpactLens("acc-hk-1")
        assertEquals(1, account.attentionFindings)
        assertEquals(UiImpactTruth.UNKNOWN, account.uniqueRecoveryPath)
        assertNull(account.independentAlternatives)

        val email = emailImpactLens("email-cn-1")
        assertEquals(5, email.confirmedDependencies)
        assertEquals(UiImpactTruth.UNKNOWN, email.uniqueRecoveryPath)

        val device = deviceImpactLens("dev-us-1")
        assertEquals(1, device.attentionFindings)
        assertEquals(UiImpactTruth.UNKNOWN, device.uniqueRecoveryPath)

        val service = serviceImpactLens("svc-wxpay")
        assertTrue(service.confirmedDependencies > 0)
        assertEquals(UiImpactTruth.UNKNOWN, service.uniqueRecoveryPath)
        assertTrue(service.unknownRelationsRemain)
    }

    @Test
    fun NonRecoveryOnlyDoesNotBecomeConfirmedSafe() {
        val impact = numberImpactLens("num-hk-1")
        assertEquals(UiImpactTruth.UNKNOWN, impact.uniqueRecoveryPath)
        assertTrue(impact.unknownRelationsRemain)
    }
}
