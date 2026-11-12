package com.pdig.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PreviewGuidePolicyTest {
    @Test fun firstInstallShowsGuide() = assertTrue(shouldShowPreviewGuide(0))
    @Test fun completedInstallSkipsGuide() =
        assertFalse(shouldShowPreviewGuide(PREVIEW_GUIDE_VERSION))
    @Test fun olderVersionCanShowGuideAgain() = assertTrue(shouldShowPreviewGuide(-1))
}
