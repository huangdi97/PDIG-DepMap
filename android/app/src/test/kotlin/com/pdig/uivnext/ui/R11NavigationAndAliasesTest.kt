package com.pdig.uivnext.ui

import com.pdig.uivnext.model.VScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class R11NavigationAndAliasesTest {
    @Test fun recordAttentionEntryIsFocusedWhileNormalRootBrowsingRemainsComplete() {
        val app = VAppState()
        app.openProductionVerificationAttention()
        assertEquals(VScreen.RECORDS, app.screen)
        assertEquals(ProductionRecordScope.ATTENTION, app.productionRecordsScope)

        app.productionRecordsScope = ProductionRecordScope.FAILED
        app.openProductionPlan("plan-1", "replace_phone_number")
        assertEquals(VScreen.CHANGE_PHONE, app.screen)
        app.back()
        assertEquals(VScreen.RECORDS, app.screen)
        assertEquals(ProductionRecordScope.FAILED, app.productionRecordsScope)

        app.navigate(VScreen.NOW)
        app.navigate(VScreen.RECORDS)
        assertEquals(ProductionRecordScope.ALL, app.productionRecordsScope)

        app.openProductionVerificationAttention()
        assertEquals(ProductionRecordScope.ATTENTION, app.productionRecordsScope)
        app.navigate(VScreen.RECORDS)
        assertEquals(ProductionRecordScope.ALL, app.productionRecordsScope)
    }

    @Test fun recordsOnlyAdvertiseActualSupportedGovernedChangeRoutes() {
        assertEquals(VScreen.CHANGE_PHONE, productionPlanDestination("replace_phone_number"))
        assertEquals(VScreen.CHANGE_CARD, productionPlanDestination("replace_payment_card"))
        assertEquals(null, productionPlanDestination("replace_device"))
        assertEquals(null, productionPlanDestination(""))
        val app = VAppState(initialScreen = VScreen.RECORDS)
        app.openProductionPlan("unsupported", "replace_device", "invisible")
        assertEquals(VScreen.RECORDS, app.screen)
        assertEquals(null, app.selectedProductionPlanId)
        assertEquals(null, app.selectedProductionActionId)
    }

    @Test fun focusedRecordActionReturnsToItsOriginalFilterAndCannotNavigateUnsupportedPlan() {
        val app = VAppState()
        app.openProductionVerificationAttention()
        app.productionRecordsScope = ProductionRecordScope.FAILED
        app.openProductionPlan("governed-plan", "replace_phone_number", "verify-step")
        assertEquals(VScreen.CHANGE_PHONE, app.screen)
        assertEquals("governed-plan", app.selectedProductionPlanId)
        assertEquals("verify-step", app.selectedProductionActionId)
        app.back()
        assertEquals(VScreen.RECORDS, app.screen)
        assertEquals(ProductionRecordScope.FAILED, app.productionRecordsScope)
        // Reopen without specific-record focus: no sticky/hidden selected step.
        app.openProductionPlan("governed-plan", "replace_phone_number")
        assertEquals(null, app.selectedProductionActionId)
        app.back()
        assertEquals(VScreen.RECORDS, app.screen)
        app.openProductionPlan("invalid", "unsupported_scenario", "not-an-action")
        assertEquals(VScreen.RECORDS, app.screen)
        assertEquals("governed-plan", app.selectedProductionPlanId)
    }

    @Test fun searchDetailBackRestoresTheWorkingQuery() {
        val app = VAppState(initialScreen = VScreen.SEARCH)
        app.searchQuery = "44"

        app.openCard("card-cn-1")
        assertEquals(VScreen.CARD_DETAIL, app.screen)

        app.back()
        assertEquals(VScreen.SEARCH, app.screen)
        assertEquals("44", app.searchQuery)
    }

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

    @Test fun reviewKeepsNowAsItsPrimaryParentContext() {
        assertTrue(isEntrySelected(VScreen.NOW, VScreen.REVIEW))
        assertFalse(isEntrySelected(VScreen.ME, VScreen.REVIEW))
        assertFalse(isEntrySelected(VScreen.RECORDS, VScreen.REVIEW))
    }

    @Test fun establishAndReviewHaveStableHierarchyParents() {
        val app = VAppState()
        app.navigate(VScreen.ME)
        app.navigate(VScreen.SOURCES)
        app.navigate(VScreen.IMPORT)
        assertEquals(VScreen.SOURCES, app.upDestination())

        app.navigate(VScreen.MANUAL_ADD)
        assertEquals(VScreen.IMPORT, app.upDestination())

        app.navigate(VScreen.MANUAL_RELATION)
        assertEquals(VScreen.MANUAL_ADD, app.upDestination())

        app.navigate(VScreen.REVIEW)
        assertEquals(VScreen.NOW, app.upDestination())
    }

    @Test fun meRemainsSelectedAcrossItsChildUtilities() {
        assertTrue(isEntrySelected(VScreen.ME, VScreen.ME))
        assertTrue(isEntrySelected(VScreen.ME, VScreen.SETTINGS))
        assertTrue(isEntrySelected(VScreen.ME, VScreen.SOURCES))
        assertTrue(isEntrySelected(VScreen.ME, VScreen.IMPORT))
        assertTrue(isEntrySelected(VScreen.ME, VScreen.MANUAL_ADD))
        assertTrue(isEntrySelected(VScreen.ME, VScreen.MANUAL_RELATION))
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
