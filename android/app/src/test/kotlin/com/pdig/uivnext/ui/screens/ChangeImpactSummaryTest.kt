package com.pdig.uivnext.ui.screens

import com.pdig.uivnext.demo.UiVNextDemoFixture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ChangeImpactSummaryTest {
    @Test
    fun TransitionSummaryIsDerivedFromActualMigrationAndBlockerState() {
        val summary = changeImpactSummary("transition")
        assertEquals(UiVNextDemoFixture.changeMigrations.size, summary.linkedServices)
        assertEquals(UiVNextDemoFixture.changeMigrations.size, summary.needsReview)
        assertEquals(1, summary.blockerCount)
    }

    @Test
    fun AfterProjectionKeepsUnresolvedServiceVisibleAsAProblem() {
        val summary = changeImpactSummary("after")
        assertEquals(UiVNextDemoFixture.changeMigrations.size, summary.linkedServices)
        assertEquals(UiVNextDemoFixture.changeMigrations.size, summary.needsReview)
        assertEquals(1, summary.blockerCount)
    }

    @Test
    fun KeepNumberAndMigrationTargetAreDifferentInfrastructureObjects() {
        val keep = UiVNextDemoFixture.numberById("num-cn-3")
        val target = UiVNextDemoFixture.numberById("num-cn-4")
        assertNotNull(keep)
        assertNotNull(target)
        assertEquals("keep", keep!!.role)
        assertEquals("secondary", target!!.role)
        assertNotEquals(keep.id, target.id)
        assertNotEquals(keep.maskedNumber, target.maskedNumber)
    }
}
