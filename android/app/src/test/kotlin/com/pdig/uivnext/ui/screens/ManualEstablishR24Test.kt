package com.pdig.uivnext.ui.screens

import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.model.VSection
import com.pdig.uivnext.ui.VAppState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ManualEstablishR24Test {
    @Test
    fun manualRecordIsSecondaryChildOfEstablish() {
        val app = VAppState(initialScreen = VScreen.MANUAL_ADD)
        assertEquals(VSection.SECONDARY, VScreen.MANUAL_ADD.section)
        assertEquals(VScreen.IMPORT, app.upDestination())
    }

    @Test
    fun searchDiscoversManualRecordWithoutChangingPrimaryIa() {
        val hits = searchResults("手工")
        assertTrue(hits.any {
            it is SearchResult.NavigationHit && it.screen == VScreen.MANUAL_ADD
        })
        assertEquals(
            listOf(
                VScreen.NOW,
                VScreen.INFRASTRUCTURE,
                VScreen.CHANGE,
                VScreen.RECORDS,
                VScreen.ME,
            ),
            com.pdig.uivnext.ui.PRIMARY_ENTRIES.map { it.screen },
        )
    }
}
