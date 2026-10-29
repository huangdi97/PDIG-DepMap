package com.pdig.uivnext.ui.screens

import com.pdig.uivnext.demo.UI_REVIEW_REFERENCE_ITEMS
import com.pdig.uivnext.demo.UiReviewReferenceKind
import com.pdig.uivnext.demo.reviewReferenceSummary
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.ui.VAppState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HumanReviewR22Test {
    @Test
    fun referenceReviewKindsStayOutsideRealityUntilHumanDecision() {
        val summary = reviewReferenceSummary()
        assertEquals(UI_REVIEW_REFERENCE_ITEMS.size, summary.total)
        assertTrue(summary.proposals > 0)
        assertTrue(summary.candidates > 0)
        assertTrue(summary.drifts > 0)
        assertTrue(UI_REVIEW_REFERENCE_ITEMS.any {
            it.kind == UiReviewReferenceKind.DEPENDENCY_PROPOSAL
        })
    }

    @Test
    fun reviewIsAChildOfNowWithoutCreatingSixthPrimaryTab() {
        val app = VAppState(initialScreen = VScreen.REVIEW)
        assertEquals(VScreen.NOW, app.upDestination())
        assertTrue(VScreen.REVIEW.section != com.pdig.uivnext.model.VSection.PRIMARY)
    }

    @Test
    fun searchCanDiscoverHumanReviewWithoutAutoConfirmingAnything() {
        val results = searchResults("待复核")
        assertTrue(results.any {
            it is SearchResult.NavigationHit && it.screen == VScreen.REVIEW
        })
    }
}
