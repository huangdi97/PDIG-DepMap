package com.pdig.uivnext.ui.screens

import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.model.VSection
import com.pdig.uivnext.ui.VAppState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ManualRelationshipR25Test {
    @Test
    fun manualRelationshipIsSecondaryChildOfManualEstablish() {
        val app = VAppState(initialScreen = VScreen.MANUAL_RELATION)
        assertEquals(VSection.SECONDARY, VScreen.MANUAL_RELATION.section)
        assertEquals(VScreen.MANUAL_ADD, app.upDestination())
    }

    @Test
    fun searchDiscoversManualRelationshipWithoutCreatingPrimaryMode() {
        val hits = searchResults("手工关系")
        assertTrue(hits.any {
            it is SearchResult.NavigationHit && it.screen == VScreen.MANUAL_RELATION
        })
        assertTrue(com.pdig.uivnext.ui.PRIMARY_ENTRIES.none {
            it.screen == VScreen.MANUAL_RELATION
        })
    }
}
