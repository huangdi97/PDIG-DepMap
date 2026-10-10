package com.pdig.uivnext.lens

import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.ui.PRIMARY_ENTRIES
import com.pdig.uivnext.ui.screens.commandTargets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VNextLensAvailabilityTest {
    @Test
    fun currentlySupportedLensesAreVisible() {
        assertEquals(
            setOf(VNextLens.REGION, VNextLens.DEPENDENCY, VNextLens.CHANGE),
            visibleLenses(),
        )
    }

    @Test
    fun identityAndRecoveryStayHiddenUntilTheirRequiredAuthorityExists() {
        assertFalse(canRenderLensEntry(VNextLens.IDENTITY))
        assertEquals(
            VNextLensAvailability.HIDDEN_UNTIL_CANONICAL,
            lensGate(VNextLens.IDENTITY).availability,
        )

        assertFalse(canRenderLensEntry(VNextLens.RECOVERY))
        assertEquals(
            VNextLensAvailability.HIDDEN_UNTIL_SOLVER,
            lensGate(VNextLens.RECOVERY).availability,
        )
    }

    @Test
    fun lensGrowthDoesNotCreateSixthOrSeventhPrimaryTabs() {
        assertEquals(
            listOf(
                VScreen.NOW,
                VScreen.INFRASTRUCTURE,
                VScreen.CHANGE,
                VScreen.RECORDS,
                VScreen.ME,
            ),
            PRIMARY_ENTRIES.map { it.screen },
        )
        assertTrue(V_NEXT_LENS_GATES.none { it.requiresNewPrimaryDestination })
    }

    @Test
    fun hiddenIdentityAndRecoveryAreNotSearchableGhostCapabilities() {
        val searchableTitles = commandTargets.map { it.title }
        assertFalse(searchableTitles.any { it.contains("身份 Lens", ignoreCase = true) })
        assertFalse(searchableTitles.any { it.contains("恢复模式", ignoreCase = true) })
        assertFalse(searchableTitles.any { it.contains("Recovery", ignoreCase = true) })
    }
}
