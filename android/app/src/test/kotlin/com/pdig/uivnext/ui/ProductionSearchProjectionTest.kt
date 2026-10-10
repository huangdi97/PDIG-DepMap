package com.pdig.uivnext.ui

import com.pdig.uivnext.production.VNextPendingReviewSummary
import com.pdig.uivnext.production.VNextProductionObject
import com.pdig.uivnext.production.VNextProductionPlanSummary
import com.pdig.uivnext.production.VNextProductionSnapshot
import com.pdig.uivnext.production.VNextProductionSourceItem
import com.pdig.uivnext.production.VNextProductionSurfaceKind
import com.pdig.uivnext.production.VNextSourceCoverageSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductionSearchProjectionTest {
    private fun snapshot() = VNextProductionSnapshot(
        revision = 12,
        objects = listOf(
            VNextProductionObject(
                id = "card-1",
                kind = "payment_instrument",
                name = "旅行主卡",
                surfaceKind = VNextProductionSurfaceKind.PAYMENT_ASSET,
                issuer = "示例银行",
                last4 = "8823",
            ),
            VNextProductionObject(
                id = "identity-1",
                kind = "identity_anchor",
                name = "登录身份",
                surfaceKind = VNextProductionSurfaceKind.IDENTITY_ANCHOR_GENERIC,
            ),
        ),
        confirmedDependencies = emptyList(),
        timeline = emptyList(),
        plans = listOf(
            VNextProductionPlanSummary(
                id = "plan-1",
                title = "更换旅行主卡",
                scenario = "replace_payment_card",
                workflowState = "in_progress",
                lastAnalyzedRevision = 12,
                effectiveDate = null,
            ),
        ),
        pendingReview = VNextPendingReviewSummary(1, 1, 1),
        sourceCoverage = VNextSourceCoverageSummary(1, 1),
        sources = listOf(
            VNextProductionSourceItem(
                id = "source-1",
                label = "银行卡账单导入",
                adapterId = "statement-csv",
                state = "active",
                lastIngestedAt = null,
            ),
        ),
    )

    @Test
    fun searchesOnlyProductionSnapshotFactsAndConsumerLabels() {
        val bank = productionSearchHits(snapshot(), "示例银行")
        assertTrue(bank.any {
            it is ProductionSearchHit.ObjectHit && it.item.id == "card-1"
        })

        val scenario = productionSearchHits(snapshot(), "更换支付卡")
        assertTrue(scenario.any {
            it is ProductionSearchHit.PlanHit && it.plan.id == "plan-1"
        })

        val source = productionSearchHits(snapshot(), "账单导入")
        assertTrue(source.any {
            it is ProductionSearchHit.SourceHit && it.source.id == "source-1"
        })
    }

    @Test
    fun privacyMaskKeepsConfirmedTailSearchableButNeverEchoesItVisibly() {
        val visible = productionSearchHits(snapshot(), "8823", privacyMask = false)
            .filterIsInstance<ProductionSearchHit.ObjectHit>()
            .single { it.item.id == "card-1" }
        assertTrue(visible.subtitle.contains("8823"))

        val masked = productionSearchHits(snapshot(), "8823", privacyMask = true)
            .filterIsInstance<ProductionSearchHit.ObjectHit>()
            .single { it.item.id == "card-1" }
        assertTrue(masked.subtitle.contains("••••"))
        assertTrue(!masked.subtitle.contains("8823"))
    }

    @Test
    fun cardTailLabelsTreatPrivacyAsPresentationOnly() {
        assertEquals("尾号未记录", productionPaymentTailLabel(null, privacyMask = false))
        assertEquals("尾号 8823", productionPaymentTailLabel("8823", privacyMask = false))
        assertEquals("尾号 ••••", productionPaymentTailLabel("8823", privacyMask = true))
        assertEquals("••••", productionPaymentCompactTailLabel("8823", privacyMask = true))
    }

    @Test
    fun coarseIdentityAnchorRemainsGenericInSearch() {
        val hits = productionSearchHits(snapshot(), "身份对象")
        val identity = hits.filterIsInstance<ProductionSearchHit.ObjectHit>()
            .single { it.item.id == "identity-1" }
        assertEquals(
            VNextProductionSurfaceKind.IDENTITY_ANCHOR_GENERIC,
            identity.item.surfaceKind,
        )
        assertTrue(identity.subtitle.contains("身份对象"))
    }

    @Test
    fun privacyMaskHidesGenericIdentityNameWithoutGuessingItsSubtype() {
        val masked = productionSearchHits(snapshot(), "登录身份", privacyMask = true)
            .filterIsInstance<ProductionSearchHit.ObjectHit>()
            .single { it.item.id == "identity-1" }

        assertEquals("身份对象（已遮蔽）", masked.title)
        assertTrue(!masked.title.contains("登录身份"))
        assertEquals(
            "身份对象（已遮蔽）",
            productionVisibleObjectName(masked.item, privacyMask = true),
        )
        assertEquals(
            "登录身份",
            productionVisibleObjectName(masked.item, privacyMask = false),
        )
    }

    @Test
    fun emptyQueryNeverReturnsImplicitEverything() {
        assertTrue(productionSearchHits(snapshot(), "").isEmpty())
        assertTrue(productionSearchHits(snapshot(), "   ").isEmpty())
    }
}
