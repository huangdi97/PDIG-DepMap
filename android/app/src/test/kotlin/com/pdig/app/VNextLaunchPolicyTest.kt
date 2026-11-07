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
                productionUiGeneration = "vnext",
                productionVNextCutoverApproved = true,
            ),
        )
    }

    @Test
    fun productionReleaseDefaultsToLegacy() {
        assertEquals(
            VNextLaunchTarget.LEGACY_PRODUCTION,
            resolveVNextLaunchTarget(
                flavor = "production",
                explicitDemo = false,
                explicitProductionVNext = false,
                debugBuild = false,
            ),
        )
    }

    @Test
    fun productionReleaseIgnoresAllIntentExtrasWithoutCutoverKeys() {
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
    fun releaseGenerationAloneCannotCutOver() {
        assertEquals(
            VNextLaunchTarget.LEGACY_PRODUCTION,
            resolveVNextLaunchTarget(
                flavor = "production",
                explicitDemo = false,
                explicitProductionVNext = false,
                debugBuild = false,
                productionUiGeneration = "vnext",
                productionVNextCutoverApproved = false,
            ),
        )
    }

    @Test
    fun releaseApprovalAloneCannotCutOver() {
        assertEquals(
            VNextLaunchTarget.LEGACY_PRODUCTION,
            resolveVNextLaunchTarget(
                flavor = "production",
                explicitDemo = false,
                explicitProductionVNext = false,
                debugBuild = false,
                productionUiGeneration = "legacy",
                productionVNextCutoverApproved = true,
            ),
        )
    }

    @Test
    fun productionReleaseRequiresBothBuildTimeCutoverKeys() {
        assertEquals(
            VNextLaunchTarget.PRODUCTION_REALITY_RELEASE,
            resolveVNextLaunchTarget(
                flavor = "production",
                explicitDemo = true,
                explicitProductionVNext = true,
                debugBuild = false,
                productionUiGeneration = "vnext",
                productionVNextCutoverApproved = true,
            ),
        )
    }

    @Test
    fun unknownGenerationFailsClosedToLegacy() {
        assertEquals(ProductionUiGeneration.LEGACY, productionUiGeneration("typo"))
        assertFalse(productionVNextReleaseEnabled("typo", cutoverApproved = true))
        assertEquals(
            VNextLaunchTarget.LEGACY_PRODUCTION,
            resolveVNextLaunchTarget(
                flavor = "production",
                explicitDemo = false,
                explicitProductionVNext = false,
                debugBuild = false,
                productionUiGeneration = "typo",
                productionVNextCutoverApproved = true,
            ),
        )
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
    fun debugCanRehearseTheExactReleaseDefaultWithoutIntentExtras() {
        assertEquals(
            VNextLaunchTarget.PRODUCTION_REALITY_RELEASE,
            resolveVNextLaunchTarget(
                flavor = "production",
                explicitDemo = false,
                explicitProductionVNext = false,
                debugBuild = true,
                productionUiGeneration = "vnext",
                productionVNextCutoverApproved = true,
            ),
        )
    }

    @Test
    fun explicitDebugReferenceStillOverridesBuildTimeReleaseDefault() {
        assertEquals(
            VNextLaunchTarget.REFERENCE_PREVIEW,
            resolveVNextLaunchTarget(
                flavor = "production",
                explicitDemo = true,
                explicitProductionVNext = false,
                debugBuild = true,
                productionUiGeneration = "vnext",
                productionVNextCutoverApproved = true,
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
