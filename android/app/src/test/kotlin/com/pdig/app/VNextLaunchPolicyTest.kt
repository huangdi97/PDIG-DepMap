package com.pdig.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VNextLaunchPolicyTest {
    @Test
    fun previewOpensCurrentReviewCandidateFromLauncherWithoutIntentExtras() {
        assertTrue(
            shouldLaunchVNext(
                flavor = "preview",
                explicitDemo = false,
                debugBuild = false,
            ),
        )
    }

    @Test
    fun productionReleaseRemainsOnLockGatedApplication() {
        assertFalse(
            shouldLaunchVNext(
                flavor = "production",
                explicitDemo = false,
                debugBuild = false,
            ),
        )
    }

    @Test
    fun productionReleaseIgnoresSyntheticDemoExtra() {
        assertFalse(
            shouldLaunchVNext(
                flavor = "production",
                explicitDemo = true,
                debugBuild = false,
            ),
        )
    }

    @Test
    fun productionDebugMayRetainExplicitInstrumentationDemoExtra() {
        assertTrue(
            shouldLaunchVNext(
                flavor = "production",
                explicitDemo = true,
                debugBuild = true,
            ),
        )
    }

    @Test
    fun unknownFlavorCannotUseDemoExtraInRelease() {
        assertFalse(
            shouldLaunchVNext(
                flavor = "unknown",
                explicitDemo = true,
                debugBuild = false,
            ),
        )
    }
}
