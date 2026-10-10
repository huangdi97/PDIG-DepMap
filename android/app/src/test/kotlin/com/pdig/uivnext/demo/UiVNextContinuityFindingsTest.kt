package com.pdig.uivnext.demo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UiVNextContinuityFindingsTest {
    @Test
    fun referenceCoversEveryCanonicalV03FindingClass() {
        assertEquals(
            UiContinuityFindingKind.entries.toSet(),
            UI_CONTINUITY_REFERENCE_FINDINGS.map { it.kind }.toSet(),
        )
    }

    @Test
    fun everyFindingExplainsBasisUnknownsAndNextAction() {
        UI_CONTINUITY_REFERENCE_FINDINGS.forEach { finding ->
            assertTrue(finding.title.isNotBlank())
            assertTrue(finding.why.isNotBlank())
            assertTrue(finding.confirmedBasis.isNotBlank())
            assertTrue(finding.unknowns.isNotBlank())
            assertTrue(finding.nextAction.isNotBlank())
        }
    }

    @Test
    fun reviewFindingsAreNeverPromotedToCriticalFacts() {
        val reviewKinds = setOf(
            UiContinuityFindingKind.UNCONFIRMED_FALLBACK,
            UiContinuityFindingKind.STALE_RECOVERY_INFORMATION,
            UiContinuityFindingKind.UNKNOWN_CRITICAL_PATH,
            UiContinuityFindingKind.PENDING_VERIFICATION,
        )
        reviewKinds.forEach { kind ->
            assertEquals("review", continuityFindingSeverity(kind))
        }
    }

    @Test
    fun findingGrammarContainsNoHealthScore() {
        val text = UI_CONTINUITY_REFERENCE_FINDINGS.joinToString(" ") {
            listOf(it.title, it.why, it.confirmedBasis, it.unknowns, it.nextAction).joinToString(" ")
        }
        assertFalse(text.contains("%"))
        assertFalse(text.contains("健康分"))
        assertFalse(text.contains("安全分"))
    }
}
