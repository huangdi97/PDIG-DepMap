import Foundation

/// Canonical R37 identity-anchor profile decoded from the existing Node.fields JSON
/// envelope. A bare `fields_json.subtype` key is intentionally ignored.
public struct ConfirmedIdentityAnchorProfile: Equatable, Sendable {
    public let subtype: IdentityAnchorSubtype
    public let verificationBasisType: VerificationBasisType
    public let confirmedAt: String
    public let evidenceRefs: [String]

    public init(
        subtype: IdentityAnchorSubtype,
        verificationBasisType: VerificationBasisType,
        confirmedAt: String,
        evidenceRefs: [String]
    ) {
        self.subtype = subtype
        self.verificationBasisType = verificationBasisType
        self.confirmedAt = confirmedAt
        self.evidenceRefs = evidenceRefs
    }
}

/// Fail-closed decoder shared by future production identity surfaces.
///
/// Only a version-1 `identity_anchor_profile` carrying subtype, confirmation
/// basis and confirmed_at becomes subtype Reality. Invalid/legacy/free-form data
/// remains a generic identity.
public func confirmedIdentityAnchorProfile(
    kind: NodeKind,
    fieldsJson: String
) -> ConfirmedIdentityAnchorProfile? {
    guard kind == .identityAnchor else { return nil }
    guard
        let parsed = try? JsonParser.parse(fieldsJson),
        let root = parsed.objectValue,
        let profile = root["identity_anchor_profile"]?.objectValue
    else {
        return nil
    }

    guard
        let versionJson = profile["version"],
        let version = try? versionJson.asLong,
        version == 1
    else {
        return nil
    }

    guard
        let subtypeWire = profile["subtype"]?.stringValue,
        let subtype = IdentityAnchorSubtype(rawValue: subtypeWire),
        let basisWire = profile["verification_basis_type"]?.stringValue,
        let basis = VerificationBasisType(rawValue: basisWire),
        let confirmedAtRaw = profile["confirmed_at"]?.stringValue
    else {
        return nil
    }

    let confirmedAt = confirmedAtRaw.trimmingCharacters(in: .whitespacesAndNewlines)
    guard !confirmedAt.isEmpty else { return nil }

    var evidenceRefs: [String] = []
    if let evidenceJson = profile["evidence_refs"] {
        guard let items = evidenceJson.arrayValue else { return nil }
        for item in items {
            guard let value = item.stringValue else { return nil }
            evidenceRefs.append(value)
        }
    }

    return ConfirmedIdentityAnchorProfile(
        subtype: subtype,
        verificationBasisType: basis,
        confirmedAt: confirmedAt,
        evidenceRefs: evidenceRefs
    )
}
