package com.pdig.uivnext.production

import com.pdig.app.data.DetectedParty
import com.pdig.app.data.ImportCommitResult
import com.pdig.app.data.ImportPreview
import com.pdig.core.generated.NodeKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductionVNextImportTest {
    @Test
    fun previewProjectionShowsDetectedObjectsWithoutCallingThemDependencies() {
        val preview = ImportPreview(
            observations = emptyList(),
            errors = listOf("row 8 skipped"),
            adapterId = "generic_csv",
            sourceLabel = "信用卡账单",
            instruments = listOf(
                DetectedParty("招商银行信用卡", NodeKind.PAYMENT_INSTRUMENT, "card-1"),
            ),
            counterparties = listOf(
                DetectedParty("Netflix", NodeKind.SERVICE, "svc-1"),
                DetectedParty("Notion", NodeKind.SERVICE, "svc-2"),
            ),
        )

        val view = mapImportPreview(preview)

        assertEquals("信用卡账单", view.sourceLabel)
        assertEquals(3, view.detectedObjectCount)
        assertEquals(listOf("招商银行信用卡"), view.detectedPaymentInstruments)
        assertEquals(listOf("Netflix", "Notion"), view.detectedCounterparties)
        assertEquals(1, view.skippedRowCount)
    }

    @Test
    fun commitProjectionRoutesGeneratedProposalsToHumanReview() {
        val view = mapImportCommit(
            ImportCommitResult(
                sourceInstanceId = "src-1",
                importSessionId = "imp-1",
                rawCount = 12,
                newUniqueCount = 10,
                duplicateCount = 2,
                nodeCount = 3,
                proposalCount = 2,
                errorCount = 0,
            ),
        )

        assertEquals(2, view.proposalCount)
        assertEquals("review", view.nextStep)
        assertTrue(view.nodeCount > 0)
    }

    @Test
    fun commitWithoutProposalReturnsToSourcesInsteadOfInventingReviewWork() {
        val view = mapImportCommit(
            ImportCommitResult(
                sourceInstanceId = "src-1",
                importSessionId = "imp-1",
                rawCount = 0,
                newUniqueCount = 0,
                duplicateCount = 0,
                nodeCount = 0,
                proposalCount = 0,
                errorCount = 0,
            ),
        )
        assertEquals("sources", view.nextStep)
    }
}
