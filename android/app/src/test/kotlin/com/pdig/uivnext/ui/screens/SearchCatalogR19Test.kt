package com.pdig.uivnext.ui.screens

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
    fun KeepRoleIsSearchableAsConsumerLanguage() {
        val results = searchResults("保号")
        assertTrue(results.any { it is SearchResult.NumberHit && it.id == "num-cn-3" })
        assertTrue(results.any { it is SearchResult.NumberHit && it.id == "num-us-1" })
    }
}
