package com.pdig.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VNextLaunchPolicyTest {
    @Test
    fun previewOpensCurrentReviewCandidateFromLauncherWithoutIntentExtras() {
        assertTrue(shouldLaunchVNext(flavor = "preview", explicitDemo = false))
    }

    @Test
    fun productionLauncherRemainsOnLockGatedLegacyApplication() {
        assertFalse(shouldLaunchVNext(flavor = "production", explicitDemo = false))
    }

    @Test
    fun explicitInstrumentationDemoExtraStillOpensVNext() {
        assertTrue(shouldLaunchVNext(flavor = "production", explicitDemo = true))
    }

    @Test
    fun unknownFlavorDoesNotSilentlyBypassProductionRouting() {
        assertFalse(shouldLaunchVNext(flavor = "unknown", explicitDemo = false))
    }
}
