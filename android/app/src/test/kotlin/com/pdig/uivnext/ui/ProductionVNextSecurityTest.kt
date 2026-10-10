package com.pdig.uivnext.ui

import com.pdig.uivnext.model.VScreen
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductionVNextSecurityTest {
    @Test
    fun overviewAndPreferenceRootsRemainUsableWithoutForcedSecureWindow() {
        listOf(
            VScreen.NOW,
            VScreen.ME,
            VScreen.SETTINGS,
            VScreen.PERSONALIZATION,
            VScreen.CHANGE,
        ).forEach { screen ->
            assertFalse("$screen should not force FLAG_SECURE", productionVNextRequiresSecureWindow(screen))
        }
    }

    @Test
    fun concreteRealityReviewHistoryAndMutationSurfacesAreSecure() {
        listOf(
            VScreen.INFRASTRUCTURE,
            VScreen.CARDS,
            VScreen.CARD_DETAIL,
            VScreen.NUMBERS,
            VScreen.ACCOUNTS,
            VScreen.WEAKNESSES,
            VScreen.RECORDS,
            VScreen.REVIEW,
            VScreen.SOURCES,
            VScreen.IMPORT,
            VScreen.MANUAL_ADD,
            VScreen.MANUAL_RELATION,
            VScreen.CHANGE_PHONE,
            VScreen.CHANGE_CARD,
            VScreen.SEARCH,
        ).forEach { screen ->
            assertTrue("$screen must force FLAG_SECURE", productionVNextRequiresSecureWindow(screen))
        }
    }

    @Test
    fun everyScreenHasAnExplicitSecurityDecision() {
        val classified = VScreen.entries.associateWith(::productionVNextRequiresSecureWindow)
        assertTrue(classified.keys.containsAll(VScreen.entries))
        assertTrue(classified.values.any { it })
        assertTrue(classified.values.any { !it })
    }
}
