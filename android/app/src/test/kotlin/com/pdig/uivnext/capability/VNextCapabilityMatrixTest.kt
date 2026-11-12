package com.pdig.uivnext.capability

import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.ui.PRIMARY_ENTRIES
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VNextCapabilityMatrixTest {
    @Test
    fun visibleReferenceCapabilitiesMatchCurrentProductSurface() {
        assertEquals(
            setOf(
                VNextCapability.FILE_IMPORT,
                VNextCapability.MANUAL_CREATE,
                VNextCapability.MANUAL_RELATIONSHIP,
                VNextCapability.HUMAN_REVIEW,
                VNextCapability.CHANGE_PHONE,
                VNextCapability.CHANGE_PAYMENT_CARD,
                VNextCapability.LIFECYCLE_PERSISTENCE,
                VNextCapability.REGION_FACT,
                VNextCapability.IDENTITY_SUBTYPE,
                VNextCapability.IDENTITY_IDENTIFIER,
            ),
            visibleReferenceCapabilities(),
        )
    }

    @Test
    fun productionAuthorityIsNarrowerThanReferenceVisibility() {
        assertTrue(hasProductionAuthority(VNextCapability.FILE_IMPORT))
        assertTrue(hasProductionAuthority(VNextCapability.MANUAL_CREATE))
        assertTrue(hasProductionAuthority(VNextCapability.HUMAN_REVIEW))
        assertTrue(hasProductionAuthority(VNextCapability.CHANGE_PHONE))
        assertTrue(hasProductionAuthority(VNextCapability.CHANGE_PAYMENT_CARD))

        assertTrue(hasProductionAuthority(VNextCapability.MANUAL_RELATIONSHIP))
        assertEquals(
            VNextProductionAuthority.AVAILABLE,
            capabilityGate(VNextCapability.MANUAL_RELATIONSHIP).productionAuthority,
        )
        assertFalse(hasProductionAuthority(VNextCapability.DEVICE_CONTINUITY))
        assertFalse(hasProductionAuthority(VNextCapability.DIGITAL_RESOURCE_CONTINUITY))
        assertFalse(hasProductionAuthority(VNextCapability.TRUSTED_HANDOFF))
        assertTrue(hasProductionAuthority(VNextCapability.LIFECYCLE_PERSISTENCE))
        assertTrue(hasProductionAuthority(VNextCapability.REGION_FACT))
        assertTrue(hasProductionAuthority(VNextCapability.IDENTITY_SUBTYPE))
        assertTrue(hasProductionAuthority(VNextCapability.IDENTITY_IDENTIFIER))
        assertFalse(hasProductionAuthority(VNextCapability.IDENTITY_CONTEXT))
        assertFalse(hasProductionAuthority(VNextCapability.RECOVERY_PREPAREDNESS))
        assertFalse(hasProductionAuthority(VNextCapability.RECOVERY_INCIDENT))
    }

    @Test
    fun futureDigitalContinuityAndTrustedHandoffStayHidden() {
        for (capability in listOf(
            VNextCapability.DIGITAL_RESOURCE_CONTINUITY,
            VNextCapability.TRUSTED_HANDOFF,
        )) {
            assertFalse(canShowReference(capability))
            assertFalse(hasProductionAuthority(capability))
            assertEquals(
                VNextCapabilityVisibility.HIDDEN_UNTIL_CANONICAL,
                capabilityGate(capability).visibility,
            )
        }
    }

    @Test
    fun recoveryPreparednessStaysHiddenUntilFactorCanonicalExists() {
        assertFalse(canShowReference(VNextCapability.RECOVERY_PREPAREDNESS))
        assertEquals(
            VNextCapabilityVisibility.HIDDEN_UNTIL_CANONICAL,
            capabilityGate(VNextCapability.RECOVERY_PREPAREDNESS).visibility,
        )
    }

    @Test
    fun governedRegionAndIdentityCapabilitiesAreProductionAvailable() {
        assertTrue(canShowReference(VNextCapability.REGION_FACT))
        assertEquals(
            VNextProductionAuthority.AVAILABLE,
            capabilityGate(VNextCapability.REGION_FACT).productionAuthority,
        )
        assertTrue(hasProductionAuthority(VNextCapability.REGION_FACT))
        assertTrue(canShowReference(VNextCapability.LIFECYCLE_PERSISTENCE))
        assertTrue(hasProductionAuthority(VNextCapability.LIFECYCLE_PERSISTENCE))
        assertTrue(canShowReference(VNextCapability.IDENTITY_SUBTYPE))
        assertTrue(hasProductionAuthority(VNextCapability.IDENTITY_SUBTYPE))
        assertTrue(canShowReference(VNextCapability.IDENTITY_IDENTIFIER))
        assertTrue(hasProductionAuthority(VNextCapability.IDENTITY_IDENTIFIER))
    }

    @Test
    fun hiddenFutureLensesCannotLeakIntoVisibleProduct() {
        assertFalse(canShowReference(VNextCapability.IDENTITY_CONTEXT))
        assertEquals(
            VNextCapabilityVisibility.HIDDEN_UNTIL_CANONICAL,
            capabilityGate(VNextCapability.IDENTITY_CONTEXT).visibility,
        )

        assertFalse(canShowReference(VNextCapability.RECOVERY_INCIDENT))
        assertEquals(
            VNextCapabilityVisibility.HIDDEN_UNTIL_SOLVER,
            capabilityGate(VNextCapability.RECOVERY_INCIDENT).visibility,
        )
    }

    @Test
    fun plannedDeviceContinuityStaysHiddenUntilItsCanonicalPrerequisitesExist() {
        assertFalse(canShowReference(VNextCapability.DEVICE_CONTINUITY))
        assertEquals(
            VNextCapabilityVisibility.HIDDEN_UNTIL_CANONICAL,
            capabilityGate(VNextCapability.DEVICE_CONTINUITY).visibility,
        )
        assertFalse(hasProductionAuthority(VNextCapability.DEVICE_CONTINUITY))
    }

    @Test
    fun capabilityGrowthNeverCreatesMorePrimaryTabs() {
        assertEquals(
            listOf(
                VScreen.NOW,
                VScreen.INFRASTRUCTURE,
                VScreen.CHANGE,
                VScreen.RECORDS,
                VScreen.ME,
            ),
            PRIMARY_ENTRIES.map { it.screen },
        )
        assertTrue(V_NEXT_CAPABILITY_MATRIX.none { it.requiresNewPrimaryDestination })
    }
}
