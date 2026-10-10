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
