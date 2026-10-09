package com.pdig.uivnext.demo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordsProjectionR21Test {
    @Test
    fun completedPlanStagesDoNotBecomeVerifiedRecords() {
        val trace = referenceRecordTrace()
        val summary = referenceRecordTraceSummary(trace)
        assertEquals(2, summary.recordedComplete)
        assertEquals(0, summary.verified)
        assertEquals(1, summary.pendingVerification)
        assertTrue(trace.none {
            it.state == UiRecordTraceState.VERIFIED
        })
    }

    @Test
    fun recordsProjectionExcludesFutureBlockedAndNotStartedStages() {
        val ids = referenceRecordTrace().map { it.id }.toSet()
        assertEquals(setOf("change-stage-1", "change-stage-2", "change-stage-3"), ids)
    }
}
