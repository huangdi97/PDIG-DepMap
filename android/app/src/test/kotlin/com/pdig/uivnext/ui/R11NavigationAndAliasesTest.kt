package com.pdig.uivnext.ui

import com.pdig.uivnext.model.VScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class R11NavigationAndAliasesTest {
    @Test fun primaryNavigationKeepsUserRequestedFiveDestinations() {
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
        assertFalse(SECONDARY_ENTRIES.any { it.screen == VScreen.ME })
    }

    @Test fun profileWorkspaceIsAPrimaryRoot() {
        val app = VAppState()
        app.navigate(VScreen.CARDS)
        app.navigate(VScreen.ME)
        assertEquals(null, app.upDestination())
        assertEquals(VScreen.ME, app.screen)
    }

    @Test fun meRemainsSelectedAcrossItsChildUtilities() {
        assertTrue(isEntrySelected(VScreen.ME, VScreen.ME))
        assertTrue(isEntrySelected(VScreen.ME, VScreen.SETTINGS))
        assertTrue(isEntrySelected(VScreen.ME, VScreen.SOURCES))
        assertTrue(isEntrySelected(VScreen.SETTINGS, VScreen.PERSONALIZATION))
        assertTrue(isEntrySelected(VScreen.SOURCES, VScreen.SOURCES))
    }

    @Test fun unlabelledNumberUsesRecordedNumberInsteadOfPresetName() {
        val demo = "+852 6*** 2748"
        assertEquals(demo, displayNameForNumber(demo, null))
        assertEquals(demo, displayNameForNumber(demo, "  "))
        assertEquals("香港主号", displayNameForNumber(demo, " 香港主号 "))
    }

    @Test fun numberAliasesAreUserEditableLocalPresentationOnly() {
        val app = VAppState()
        val number = "+852 6*** 2748"
        assertEquals(number, app.numberDisplayName("num-hk-1", number))
        app.renameNumber("num-hk-1", " 香港主号 ")
        assertEquals("香港主号", app.numberDisplayName("num-hk-1", number))
        app.renameNumber("num-hk-1", "")
        assertEquals(number, app.numberDisplayName("num-hk-1", number))
    }

    @Test fun headerUpNavigatesHierarchyNotLastVisitedTab() {
        val app = VAppState()
        app.navigate(VScreen.ME)
        app.navigate(VScreen.CARDS)
        app.openCard("card-cn-1")
        assertEquals(VScreen.CARDS, app.upDestination())
        app.navigateUp()
        assertEquals(VScreen.CARDS, app.screen)
        app.navigateUp()
        assertEquals(VScreen.INFRASTRUCTURE, app.screen)
        assertFalse(app.canNavigateUp())
    }

    @Test fun systemBackReturnsActualPreviousPage() {
        val app = VAppState()
        app.navigate(VScreen.ME)
        app.navigate(VScreen.NUMBERS)
        app.openNumber("num-cn-1")
        assertEquals(VScreen.NUMBER_DETAIL, app.screen)
        app.back()
        assertEquals(VScreen.NUMBERS, app.screen)
        app.back()
        assertEquals(VScreen.ME, app.screen)
    }

    @Test fun secondaryObjectDetailUsesStableInfrastructureHierarchy() {
        val app = VAppState()
        app.navigate(VScreen.ACCOUNTS)
        app.openSecondaryObject(VScreen.ACCOUNT_DETAIL, "acc-hk-1")
        assertEquals("acc-hk-1", app.selectedSecondaryObjectId)
        assertEquals(VScreen.ACCOUNTS, app.upDestination())
        assertTrue(isEntrySelected(VScreen.INFRASTRUCTURE, app.screen))
        assertTrue(isEntrySelected(VScreen.ACCOUNTS, app.screen))
        app.navigateUp()
        assertEquals(VScreen.ACCOUNTS, app.screen)
    }

    @Test fun focusedPhoneChangeHasChangeRootAsHierarchyParent() {
        val app = VAppState()
        app.navigate(VScreen.CHANGE)
        app.navigate(VScreen.CHANGE_PHONE)
        assertEquals(VScreen.CHANGE, app.upDestination())
        assertTrue(isEntrySelected(VScreen.CHANGE, app.screen))
        app.navigateUp()
        assertEquals(VScreen.CHANGE, app.screen)
    }

    @Test fun studioUpGoesToObjectDetail() {
        val app = VAppState()
        app.navigate(VScreen.CARDS)
        app.openCard("card-cn-1")
        app.openCardCustomization("card-cn-1")
        app.navigateUp()
        assertEquals(VScreen.CARD_DETAIL, app.screen)
    }

    @Test fun maskingControlsIdentityLabelsWithoutChangingSavedAliases() {
        val number = "+852 9***4321"
        assertEquals(number, visibleNumberDisplayName(number, null, false))
        assertEquals("号码已遮蔽", visibleNumberDisplayName(number, null, true))
        assertEquals("香港主号", visibleNumberDisplayName(number, "香港主号", true))
        assertEquals("号码已遮蔽", visibleNumberDisplayName(number, "13812345678", true))
        val app = VAppState()
        app.renameNumber("num-hk-1", "香港主号")
        app.privacyMask = true
        assertEquals("香港主号", app.numberDisplayNameForScreen("num-hk-1", number))
        assertEquals("香港主号", app.numberAlias("num-hk-1"))
    }

    @Test fun privacyInitiallyVisibleUnlessUserChoosesOtherwise() {
        val app = VAppState()
        assertFalse(app.privacyMask)
        app.privacyMask = true
        assertTrue(app.privacyMask)
    }
}
