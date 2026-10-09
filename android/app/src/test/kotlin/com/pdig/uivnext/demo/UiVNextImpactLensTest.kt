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
    fun ExplicitRecoveryOnlyNumberCanConfirmPositiveButNeverInventAlternatives() {
        val impact = numberImpactLens("num-us-1")
        assertEquals(UiImpactTruth.CONFIRMED, impact.uniqueRecoveryPath)
        assertNull(impact.criticalAccounts)
        assertNull(impact.independentAlternatives)
        assertTrue(impact.unknownRelationsRemain)
    }

    @Test
    fun NonRecoveryOnlyDoesNotBecomeConfirmedSafe() {
        val impact = numberImpactLens("num-hk-1")
        assertEquals(UiImpactTruth.UNKNOWN, impact.uniqueRecoveryPath)
        assertTrue(impact.unknownRelationsRemain)
    }
}
