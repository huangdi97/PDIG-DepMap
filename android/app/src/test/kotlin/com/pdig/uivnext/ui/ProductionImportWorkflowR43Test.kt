package com.pdig.uivnext.ui

import com.pdig.app.workflow.FileWorkflowPurpose
import com.pdig.app.workflow.FileWorkflowStep
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductionImportWorkflowR43Test {
    @Test
    fun importWorkflowIsInactiveBeforeSecurePickerBegins() {
        assertFalse(
            isProductionImportWorkflowActive(
                purpose = null,
                step = null,
                hasPreview = false,
                hasResult = false,
            ),
        )
        assertFalse(
            isProductionImportWorkflowActive(
                purpose = FileWorkflowPurpose.IMPORT,
                step = FileWorkflowStep.IDLE,
                hasPreview = false,
                hasResult = false,
            ),
        )
    }

    @Test
    fun securePickerAndPostReauthStagesRemainInsideImportWorkspace() {
        listOf(
            FileWorkflowStep.AWAITING_PICKER,
            FileWorkflowStep.FILE_RECEIVED,
            FileWorkflowStep.REVIEW,
            FileWorkflowStep.DONE,
            FileWorkflowStep.INTERRUPTED,
        ).forEach { step ->
            assertTrue(
                "expected active import workspace for $step",
                isProductionImportWorkflowActive(
                    purpose = FileWorkflowPurpose.IMPORT,
                    step = step,
                    hasPreview = false,
                    hasResult = false,
                ),
            )
        }
    }

    @Test
    fun previewOrResultKeepsWorkflowVisibleEvenIfReducerReturnsIdle() {
        assertTrue(
            isProductionImportWorkflowActive(
                purpose = FileWorkflowPurpose.IMPORT,
                step = FileWorkflowStep.IDLE,
                hasPreview = true,
                hasResult = false,
            ),
        )
        assertTrue(
            isProductionImportWorkflowActive(
                purpose = FileWorkflowPurpose.IMPORT,
                step = FileWorkflowStep.IDLE,
                hasPreview = false,
                hasResult = true,
            ),
        )
    }

    @Test
    fun restoreWorkflowNeverLeaksIntoImportWorkspace() {
        assertFalse(
            isProductionImportWorkflowActive(
                purpose = FileWorkflowPurpose.RESTORE,
                step = FileWorkflowStep.FILE_RECEIVED,
                hasPreview = true,
                hasResult = true,
            ),
        )
    }
}
