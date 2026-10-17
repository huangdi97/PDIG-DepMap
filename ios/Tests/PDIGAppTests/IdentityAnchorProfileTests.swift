import XCTest
import PDIGCore

final class IdentityAnchorProfileTests: XCTestCase {
    func testConfirmedProfileParsesOnlyWithGovernedAuthority() {
        let fields = """
        {
          "identity_anchor_profile": {
            "version": 1,
            "subtype": "phone_number",
            "verification_basis_type": "user_confirmed",
            "confirmed_at": "2026-10-10T00:00:00Z",
            "evidence_refs": ["ev-1"]
          }
        }
        """

        let profile = confirmedIdentityAnchorProfile(
            kind: .identityAnchor,
            fieldsJson: fields
        )

        XCTAssertEqual(profile?.subtype, .phoneNumber)
        XCTAssertEqual(profile?.verificationBasisType, .userConfirmed)
        XCTAssertEqual(profile?.confirmedAt, "2026-10-10T00:00:00Z")
        XCTAssertEqual(profile?.evidenceRefs, ["ev-1"])
    }

    func testConfirmedIdentifierHasIndependentAuthority() {
        let fields = #"""
        {"identity_anchor_profile":{"version":1,"subtype":"email_address","verification_basis_type":"user_confirmed","confirmed_at":"2026-10-10T00:00:00Z","identifier":{"value":"user@example.com","verification_basis_type":"authoritative_source","confirmed_at":"2026-10-10T02:00:00Z","evidence_refs":["ev-email"]}}}
        """#

        let profile = confirmedIdentityAnchorProfile(kind: .identityAnchor, fieldsJson: fields)
        XCTAssertEqual(profile?.subtype, .emailAddress)
        XCTAssertEqual(profile?.identifier?.value, "user@example.com")
        XCTAssertEqual(profile?.identifier?.verificationBasisType, .authoritativeSource)
        XCTAssertEqual(profile?.identifier?.confirmedAt, "2026-10-10T02:00:00Z")
        XCTAssertEqual(profile?.identifier?.evidenceRefs, ["ev-email"])
    }

    func testMalformedIdentifierDoesNotDestroyValidSubtype() {
        let fields = #"{"identity_anchor_profile":{"version":1,"subtype":"phone_number","verification_basis_type":"user_confirmed","confirmed_at":"t","identifier":{"value":"","verification_basis_type":"machine_guess","confirmed_at":""}}}"#
        let profile = confirmedIdentityAnchorProfile(kind: .identityAnchor, fieldsJson: fields)
        XCTAssertEqual(profile?.subtype, .phoneNumber)
        XCTAssertNil(profile?.identifier)
    }

    func testBareSubtypeNeverBecomesAuthority() {
        XCTAssertNil(
            confirmedIdentityAnchorProfile(
                kind: .identityAnchor,
                fieldsJson: #"{"subtype":"phone_number"}"#
            )
        )
    }

    func testInvalidProfileFailsClosed() {
        XCTAssertNil(
            confirmedIdentityAnchorProfile(
                kind: .identityAnchor,
                fieldsJson: #"{"identity_anchor_profile":{"version":1,"subtype":"sms","verification_basis_type":"user_confirmed","confirmed_at":"t"}}"#
            )
        )
        XCTAssertNil(
            confirmedIdentityAnchorProfile(
                kind: .identityAnchor,
                fieldsJson: #"{"identity_anchor_profile":{"version":1,"subtype":"email_address","verification_basis_type":"machine_guess","confirmed_at":"t"}}"#
            )
        )
    }

    func testProfileOnNonIdentityNodeIsRejected() {
        XCTAssertNil(
            confirmedIdentityAnchorProfile(
                kind: .account,
                fieldsJson: #"{"identity_anchor_profile":{"version":1,"subtype":"email_address","verification_basis_type":"user_confirmed","confirmed_at":"t"}}"#
            )
        )
    }
}
