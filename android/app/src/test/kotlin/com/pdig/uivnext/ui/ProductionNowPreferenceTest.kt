package com.pdig.uivnext.ui

import com.pdig.uivnext.production.VNextPendingReviewSummary
import com.pdig.uivnext.production.VNextProductionSnapshot
import com.pdig.uivnext.production.VNextProductionTimelineItem
import com.pdig.uivnext.production.VNextProjectionTruth
import com.pdig.uivnext.production.VNextSourceCoverageSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductionNowPreferenceTest {
    private fun item(id: String, bucket: String) = VNextProductionTimelineItem(
        id = id,
        kind = "test",
        title = id,
        subtitle = "",
        scheduledAt = null,
        bucket = bucket,
        priority = 1,
        sourceType = "test",
        sourceId = id,
        actionTarget = null,
        status = "open",
        truth = VNextProjectionTruth.DERIVED,
    )

    private val snapshot = VNextProductionSnapshot(
        revision = 1,
        objects = emptyList(),
        confirmedDependencies = emptyList(),
        timeline = listOf(
            item("attention", "attention"),
            item("overdue", "overdue"),
            item("today", "today"),
            item("week", "7d"),
            item("month", "30d"),
            item("later", "later"),
        ),
        plans = emptyList(),
        pendingReview = VNextPendingReviewSummary(0, 0, 0),
        sourceCoverage = VNextSourceCoverageSummary(0, 0),
    )

    @Test
    fun showUpcomingTrueKeepsTheAuthoritativeTimelineOrder() {
        assertEquals(
            snapshot.timeline.map { it.id },
            productionNowTimeline(snapshot, showUpcoming = true).map { it.id },
        )
    }

    @Test
    fun showUpcomingFalseHidesOnlyFutureBucketsNotCurrentAttention() {
        assertEquals(
            listOf("attention", "overdue", "today"),
            productionNowTimeline(snapshot, showUpcoming = false).map { it.id },
        )
    }

    @Test
    fun hidingUpcomingNeverInventsAnAllClearState() {
        val futureOnly = snapshot.copy(
            timeline = listOf(item("week", "7d"), item("later", "later")),
        )
        assertTrue(productionNowTimeline(futureOnly, showUpcoming = false).isEmpty())
    }
}
