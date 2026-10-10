package com.pdig.core.domain

import com.pdig.core.generated.IdentityAnchorSubtype
import com.pdig.core.generated.NodeKind
import com.pdig.core.generated.VerificationBasisType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IdentityAnchorProfileTest {
    @Test
    fun confirmedProfileParsesOnlyWithGovernedAuthority() {
        val fields = """
            {
              "identity_anchor_profile": {
                "version": 1,
                "subtype": "phone_number",
                "verification_basis_type": "user_confirmed",
                "confirmed_at": "2026-10-10T00:00:00Z",
                "evidence_refs": ["ev-1"]
              }
            }
        """.trimIndent()

        val profile = confirmedIdentityAnchorProfile(NodeKind.IDENTITY_ANCHOR, fields)
        requireNotNull(profile)
        assertEquals(IdentityAnchorSubtype.PHONE_NUMBER, profile.subtype)
        assertEquals(VerificationBasisType.USER_CONFIRMED, profile.verificationBasisType)
        assertEquals("2026-10-10T00:00:00Z", profile.confirmedAt)
        assertEquals(listOf("ev-1"), profile.evidenceRefs)
    }

    @Test
    fun bareLegacySubtypeNeverBecomesAuthority() {
        assertNull(
            confirmedIdentityAnchorProfile(
                NodeKind.IDENTITY_ANCHOR,
                """{"subtype":"phone_number"}""",
            ),
        )
    }

    @Test
    fun phoneLookingNameIsIrrelevantBecauseDecoderOnlyConsumesFields() {
        assertNull(
            confirmedIdentityAnchorProfile(
                NodeKind.IDENTITY_ANCHOR,
                "{}",
            ),
        )
    }

    @Test
    fun invalidEnumBasisOrProfileVersionFailsClosed() {
        assertNull(
            confirmedIdentityAnchorProfile(
                NodeKind.IDENTITY_ANCHOR,
                """{"identity_anchor_profile":{"version":1,"subtype":"sms","verification_basis_type":"user_confirmed","confirmed_at":"t"}}""",
            ),
        )
        assertNull(
            confirmedIdentityAnchorProfile(
                NodeKind.IDENTITY_ANCHOR,
                """{"identity_anchor_profile":{"version":1,"subtype":"email_address","verification_basis_type":"machine_guess","confirmed_at":"t"}}""",
            ),
        )
        assertNull(
            confirmedIdentityAnchorProfile(
                NodeKind.IDENTITY_ANCHOR,
                """{"identity_anchor_profile":{"version":2,"subtype":"email_address","verification_basis_type":"user_confirmed","confirmed_at":"t"}}""",
            ),
        )
    }

    @Test
    fun profileOnNonIdentityNodeIsRejected() {
        val fields =
            """{"identity_anchor_profile":{"version":1,"subtype":"email_address","verification_basis_type":"user_confirmed","confirmed_at":"t"}}"""
        assertNull(confirmedIdentityAnchorProfile(NodeKind.ACCOUNT, fields))
    }
}
