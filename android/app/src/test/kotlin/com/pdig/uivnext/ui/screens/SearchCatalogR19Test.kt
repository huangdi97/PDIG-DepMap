package com.pdig.uivnext.ui.screens

import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.ui.VAppState
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchCatalogR19Test {
    @Test
    fun LifecycleFactsAreSearchableWithoutInventingObjects() {
        val fee = searchResults("¥100")
        assertTrue(fee.any { it is SearchResult.CardHit && it.id == "card-cn-2" })

        val keepDue = searchResults("2026-11-05")
        assertTrue(keepDue.any { it is SearchResult.NumberHit && it.id == "num-us-1" })
    }

    @Test
    fun SavedNumberAliasIsSearchableAndRenderedAsTheHitTitle() {
        val app = VAppState()
        app.renameNumber("num-hk-1", "香港银行主号")
        val results = searchResults("香港银行主号", app)
        assertTrue(results.any {
            it is SearchResult.NumberHit &&
                it.id == "num-hk-1" &&
                it.title == "香港银行主号"
        })
    }

    @Test
    fun SecondarySearchHitOpensFocusedObjectDetail() {
        val app = VAppState()
        app.navigate(VScreen.SEARCH)
        val hit = searchResults("Pixel 8", app)
            .filterIsInstance<SearchResult.DeviceHit>()
            .first()
        openSearchResult(hit, app)
        assertTrue(app.screen == VScreen.DEVICE_DETAIL)
        assertTrue(app.selectedSecondaryObjectId == "dev-cn-1")
    }

    @Test
    fun CardReplacementSearchRequiresTargetSelectionInsteadOfOpeningEmptyFlow() {
        val hit = searchResults("换卡")
            .filterIsInstance<SearchResult.NavigationHit>()
            .first { it.title == "更换银行卡" }
        assertEquals(com.pdig.uivnext.model.VScreen.CARDS, hit.screen)
    }

    @Test
    fun KeepRoleIsSearchableAsConsumerLanguage() {
        val results = searchResults("保号")
        assertTrue(results.any { it is SearchResult.NumberHit && it.id == "num-cn-3" })
        assertTrue(results.any { it is SearchResult.NumberHit && it.id == "num-us-1" })
    }
}
