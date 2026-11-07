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
    fun globalMaskCoversTimelineNamesAndImpactDescriptionsWithoutErasingStatus() {
        assertEquals("时间节点（已遮蔽）", productionVisibleTimelineTitle("工资卡 8823 续费", true))
        assertEquals("工资卡 8823 续费", productionVisibleTimelineTitle("工资卡 8823 续费", false))
        assertEquals("时间节点标题未记录", productionVisibleTimelineTitle("", true))
        assertEquals("说明已遮蔽",
            productionVisibleTimelineDetail("手机号 +852 6123 4567", "待复核", true))
        assertEquals("手机号 +852 6123 4567",
            productionVisibleTimelineDetail("手机号 +852 6123 4567", "待复核", false))
        assertEquals("待复核", productionVisibleTimelineDetail("", "待复核", true))
        assertEquals("核对项（已遮蔽）",
            productionVisibleImpactText("验证 8823 新支付路径", true, isTitle = true))
        assertEquals("核对说明已遮蔽",
            productionVisibleImpactText("先停用 138****8823", true, isTitle = false))
        assertEquals("核对原路径",
            productionVisibleImpactText("核对原路径", false, isTitle = true))
    }

    @Test
    fun hidingUpcomingNeverInventsAnAllClearState() {
        val futureOnly = snapshot.copy(
            timeline = listOf(item("week", "7d"), item("later", "later")),
        )
        assertTrue(productionNowTimeline(futureOnly, showUpcoming = false).isEmpty())
    }
}
