import Foundation

/// Canonical R37 identity-anchor profile decoded from the existing Node.fields JSON
/// envelope. A bare `fields_json.subtype` key is intentionally ignored.
public struct ConfirmedIdentityIdentifier: Equatable, Sendable {
    public let value: String
    public let verificationBasisType: VerificationBasisType
    public let confirmedAt: String
    public let evidenceRefs: [String]
}

public struct ConfirmedIdentityAnchorProfile: Equatable, Sendable {
    public let subtype: IdentityAnchorSubtype
    public let verificationBasisType: VerificationBasisType
    public let confirmedAt: String
    public let evidenceRefs: [String]
    public let identifier: ConfirmedIdentityIdentifier?

    public init(
        subtype: IdentityAnchorSubtype,
        verificationBasisType: VerificationBasisType,
        confirmedAt: String,
        evidenceRefs: [String],
        identifier: ConfirmedIdentityIdentifier? = nil
    ) {
        self.subtype = subtype
        self.verificationBasisType = verificationBasisType
        self.confirmedAt = confirmedAt
        self.evidenceRefs = evidenceRefs
        self.identifier = identifier
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
        evidenceRefs: evidenceRefs,
        identifier: confirmedIdentityIdentifier(profile: profile, subtype: subtype)
    )
}

private func confirmedIdentityIdentifier(
    profile: JsonObject,
    subtype: IdentityAnchorSubtype
) -> ConfirmedIdentityIdentifier? {
    guard subtype == .phoneNumber || subtype == .emailAddress else { return nil }
    guard let identifier = profile["identifier"]?.objectValue else { return nil }

    guard
        let valueRaw = identifier["value"]?.stringValue,
        let basisWire = identifier["verification_basis_type"]?.stringValue,
        let basis = VerificationBasisType(rawValue: basisWire),
        let confirmedAtRaw = identifier["confirmed_at"]?.stringValue
    else {
        return nil
    }

    let value = valueRaw.trimmingCharacters(in: .whitespacesAndNewlines)
    let confirmedAt = confirmedAtRaw.trimmingCharacters(in: .whitespacesAndNewlines)
    guard !value.isEmpty, value.count <= 320, !confirmedAt.isEmpty else { return nil }
    guard !value.unicodeScalars.contains(where: { $0.value < 0x20 || $0.value == 0x7F }) else {
        return nil
    }

    var evidenceRefs: [String] = []
    if let evidenceJson = identifier["evidence_refs"] {
        guard let items = evidenceJson.arrayValue else { return nil }
        for item in items {
            guard let ref = item.stringValue else { return nil }
            evidenceRefs.append(ref)
        }
    }

    return ConfirmedIdentityIdentifier(
        value: value,
        verificationBasisType: basis,
        confirmedAt: confirmedAt,
        evidenceRefs: evidenceRefs
    )
}
