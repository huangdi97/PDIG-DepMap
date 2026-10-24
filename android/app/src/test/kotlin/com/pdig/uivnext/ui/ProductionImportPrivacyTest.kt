package com.pdig.uivnext.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Privacy cannot convert import parsing into an unreviewed authoritative commit. */
class ProductionImportPrivacyTest {
    @Test
    fun previewCanBeParsedButNeverCommittedWhileIdentitiesAreHidden() {
        assertFalse(
            productionImportConfirmationAllowed(
                privacyMask = true,
                authorityAvailable = true,
                busy = false,
                mappingReady = true,
            ),
        )
        assertFalse(
            productionImportConfirmationAllowed(
                privacyMask = false,
                authorityAvailable = false,
                busy = false,
                mappingReady = true,
            ),
        )
        assertFalse(
            productionImportConfirmationAllowed(
                privacyMask = false,
                authorityAvailable = true,
                busy = true,
                mappingReady = true,
            ),
        )
        assertFalse(
            productionImportConfirmationAllowed(
                privacyMask = false,
                authorityAvailable = true,
                busy = false,
                mappingReady = false,
            ),
        )
        assertTrue(
            productionImportConfirmationAllowed(
                privacyMask = false,
                authorityAvailable = true,
                busy = false,
                mappingReady = true,
            ),
        )
    }

    @Test
    fun existingSourceMustBeIdentifiableBeforeReusingIt() {
        assertFalse(productionSourceImportStartAllowed(true, true, true))
        assertTrue(productionSourceImportStartAllowed(true, false, true))
        assertTrue(productionSourceImportStartAllowed(false, true, true))
        assertFalse(productionSourceImportStartAllowed(false, false, false))
    }

    @Test
    fun sourceNamesArePresentationOnlyAndRedactWithoutRewritingReality() {
        val confirmedSourceLabel = "个人海外银行卡导入"
        assertEquals(
            "数据源（已遮蔽）",
            productionVisibleSourceTitle(confirmedSourceLabel, true),
        )
        assertEquals(
            confirmedSourceLabel,
            productionVisibleSourceTitle(confirmedSourceLabel, false),
        )
    }
}
