package com.pdig.uivnext.ui.screens

import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.model.VSection
import com.pdig.uivnext.ui.VAppState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EstablishImportR23Test {
    @Test
    fun importIsFocusedSecondaryFlowOwnedByDataSources() {
        val app = VAppState(initialScreen = VScreen.IMPORT)
        assertEquals(VSection.SECONDARY, VScreen.IMPORT.section)
        assertEquals(VScreen.SOURCES, app.upDestination())
    }

    @Test
    fun searchDiscoversEstablishImportWithoutCreatingAnotherPrimaryTab() {
        val hits = searchResults("导入")
        assertTrue(hits.any {
            it is SearchResult.NavigationHit && it.screen == VScreen.IMPORT
        })
    }

    @Test
    fun importDoesNotChangeFivePrimaryDestinations() {
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
