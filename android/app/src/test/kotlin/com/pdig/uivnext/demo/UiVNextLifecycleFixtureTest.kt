package com.pdig.uivnext.demo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * R18 lifecycle facts are explicit recorded presentation facts, never inferred
 * Canonical truth. Keep the reference fixture rich enough to exercise both
 * filled and unknown states.
 */
class UiVNextLifecycleFixtureTest {
    @Test
    fun CardLifecycleCarriesFeeBillingAndInstallmentFacts() {
        val facts = UiVNextDemoFixture.cardLifecycleFor("card-cn-2")
        assertNotNull(facts)
        assertEquals("¥100 / 年", facts!!.annualFee)
        assertEquals("每月 12 日", facts.billingDay)
        assertTrue(facts.installmentSummary!!.contains("2 笔"))
    }

    @Test
    fun KeepAliveNumberCarriesDueCycleAndRecordedMethod() {
        val facts = UiVNextDemoFixture.numberLifecycleFor("num-us-1")
        assertNotNull(facts)
        assertEquals("2026-11-05", facts!!.keepAliveDue)
        assertEquals("每 90 天", facts.keepAliveCycle)
        assertTrue(facts.renewalMethod!!.contains("已记录"))
    }

    @Test
    fun MissingLifecycleStaysUnknownInsteadOfInventingDefaults() {
        assertNull(UiVNextDemoFixture.cardLifecycleFor("card-sg-1"))
        assertNull(UiVNextDemoFixture.numberLifecycleFor("num-sg-1"))
    }
}
