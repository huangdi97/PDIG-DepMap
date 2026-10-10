package com.pdig.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VNextLaunchPolicyTest {
    @Test
    fun previewAlwaysOpensReferenceCandidate() {
        assertEquals(
            VNextLaunchTarget.REFERENCE_PREVIEW,
            resolveVNextLaunchTarget(
                flavor = "preview",
                explicitDemo = false,
                explicitProductionVNext = false,
                debugBuild = false,
            ),
        )
    }

    @Test
    fun productionReleaseIgnoresAllVNextIntentExtras() {
        listOf(
            false to false,
            true to false,
            false to true,
            true to true,
        ).forEach { (demo, production) ->
            assertEquals(
                VNextLaunchTarget.LEGACY_PRODUCTION,
                resolveVNextLaunchTarget(
                    flavor = "production",
                    explicitDemo = demo,
                    explicitProductionVNext = production,
                    debugBuild = false,
                ),
            )
        }
    }

    @Test
    fun productionDebugCanOpenRealRealityVNextRehearsal() {
        assertEquals(
            VNextLaunchTarget.PRODUCTION_REALITY_DEBUG,
            resolveVNextLaunchTarget(
                flavor = "production",
                explicitDemo = false,
                explicitProductionVNext = true,
                debugBuild = true,
            ),
        )
    }

    @Test
    fun productionRealityDebugWinsOverReferenceExtraWhenBothArePresent() {
        assertEquals(
            VNextLaunchTarget.PRODUCTION_REALITY_DEBUG,
            resolveVNextLaunchTarget(
                flavor = "production",
                explicitDemo = true,
                explicitProductionVNext = true,
                debugBuild = true,
            ),
        )
    }

    @Test
    fun productionDebugRetainsReferenceEvidenceExtra() {
        assertTrue(
            shouldLaunchVNext(
                flavor = "production",
                explicitDemo = true,
                debugBuild = true,
            ),
        )
    }

    @Test
    fun productionReleaseNeverLaunchesReferenceFixture() {
        assertFalse(
            shouldLaunchVNext(
                flavor = "production",
                explicitDemo = true,
                debugBuild = false,
            ),
        )
    }
}
