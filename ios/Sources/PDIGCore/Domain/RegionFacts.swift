import Foundation

/// R39 governed geographic Reality fact.
///
/// Region is never inferred here from currency, issuer/provider strings, identifier
/// prefixes, locale, timezone, IP, current device location or presentation geometry.
public struct ConfirmedRegionFact: Equatable, Sendable {
    public let id: String
    public let facet: RegionFacet
    public let territoryCode: String
    public let subdivisionCode: String?
    public let state: RegionFactState
    public let verificationBasisType: VerificationBasisType
    public let confirmedAt: String
    public let evidenceRefs: [String]
    public let validFrom: String?
    public let validUntil: String?
    public let retiredAt: String?
}

public enum RegionLensSelectionStatus: String, Equatable, Sendable {
    case selected
    case needsReview = "needs_review"
    case unknown
}

public struct RegionLensSelection: Equatable, Sendable {
    public let status: RegionLensSelectionStatus
    public let territoryCode: String?
    public let facet: RegionFacet?

    public init(
        status: RegionLensSelectionStatus,
        territoryCode: String? = nil,
        facet: RegionFacet? = nil
    ) {
        self.status = status
        self.territoryCode = territoryCode
        self.facet = facet
    }
}

/// Decode valid RegionFact items from Node.fields.
///
/// Container-level corruption/version drift returns an empty list. Item-level
/// corruption drops only that item, preserving valid siblings.
public func governedRegionFacts(fieldsJson: String) -> [ConfirmedRegionFact] {
    guard
        let parsed = try? JsonParser.parse(fieldsJson),
        let root = parsed.objectValue,
        let container = root["region_facts"]?.objectValue,
        let versionJson = container["version"],
        let version = try? versionJson.asLong,
        version == 1,
        let items = container["items"]?.arrayValue
    else {
        return []
    }

    return items.compactMap { raw in
        guard let item = raw.objectValue else { return nil }
        return decodeRegionFact(item)
    }
}

public func currentConfirmedRegionFacts(fieldsJson: String) -> [ConfirmedRegionFact] {
    governedRegionFacts(fieldsJson: fieldsJson).filter { $0.state == .confirmed }
}

/// Default Region Lens precedence.
///
/// The physical_location facet is intentionally excluded because it is volatile
/// and privacy-sensitive; it may only be used by a dedicated explicit view.
public func defaultRegionLensSelection(fieldsJson: String) -> RegionLensSelection {
    let facts = currentConfirmedRegionFacts(fieldsJson: fieldsJson)
    let precedence: [RegionFacet] = [
        .numberingTerritory,
        .issuanceJurisdiction,
        .serviceMarket,
        .providerJurisdiction,
        .userConfirmedContext,
    ]

    for facet in precedence {
        let territories = Array(
            Set(
                facts
                    .filter { $0.facet == facet }
                    .map(\.territoryCode)
            )
        ).sorted()

        if territories.isEmpty {
            continue
        }
        if territories.count == 1 {
            return RegionLensSelection(
                status: .selected,
                territoryCode: territories[0],
                facet: facet
            )
        }
        return RegionLensSelection(status: .needsReview, facet: facet)
    }

    return RegionLensSelection(status: .unknown)
}

private func decodeRegionFact(_ item: JsonObject) -> ConfirmedRegionFact? {
    guard
        let id = requiredText(item, "id"),
        id.count <= 160,
        !containsControl(id),
        let facetWire = item["facet"]?.stringValue,
        let facet = RegionFacet(rawValue: facetWire),
        let territoryRaw = requiredText(item, "territory_code"),
        territoryRaw == territoryRaw.uppercased(),
        CanonicalSpec.iso3166Alpha2TerritoryCodes.contains(territoryRaw),
        let stateWire = item["state"]?.stringValue,
        let state = RegionFactState(rawValue: stateWire),
        let basisWire = item["verification_basis_type"]?.stringValue,
        let basis = VerificationBasisType(rawValue: basisWire),
        let confirmedAt = requiredText(item, "confirmed_at"),
        !containsControl(confirmedAt)
    else {
        return nil
    }

    let subdivision: String?
    if item["subdivision_code"] == nil {
        subdivision = nil
    } else {
        guard
            let value = requiredText(item, "subdivision_code"),
            validSubdivision(territory: territoryRaw, value: value)
        else {
            return nil
        }
        subdivision = value
    }

    guard let evidenceRefs = stringArray(item, "evidence_refs") else {
        return nil
    }
    if evidenceRefs.contains(where: { $0.count > 512 || containsControl($0) }) {
        return nil
    }

    guard
        let validFrom = optionalStrictText(item, "valid_from"),
        let validUntil = optionalStrictText(item, "valid_until"),
        let retiredAt = optionalStrictText(item, "retired_at")
    else {
        return nil
    }

    return ConfirmedRegionFact(
        id: id,
        facet: facet,
        territoryCode: territoryRaw,
        subdivisionCode: subdivision,
        state: state,
        verificationBasisType: basis,
        confirmedAt: confirmedAt,
        evidenceRefs: evidenceRefs,
        validFrom: validFrom,
        validUntil: validUntil,
        retiredAt: retiredAt
    )
}

/// The double Optional distinguishes an absent key from an invalid present value.
private func optionalStrictText(_ obj: JsonObject, _ key: String) -> String?? {
    guard let raw = obj[key] else { return .some(nil) }
    guard let rawText = raw.stringValue else { return nil }
    let value = rawText.trimmingCharacters(in: .whitespacesAndNewlines)
    guard !value.isEmpty, !containsControl(value) else { return nil }
    return .some(value)
}

private func requiredText(_ obj: JsonObject, _ key: String) -> String? {
    guard let raw = obj[key]?.stringValue else { return nil }
    let value = raw.trimmingCharacters(in: .whitespacesAndNewlines)
    return value.isEmpty ? nil : value
}

private func stringArray(_ obj: JsonObject, _ key: String) -> [String]? {
    guard let raw = obj[key] else { return [] }
    guard let items = raw.arrayValue else { return nil }
    var out: [String] = []
    for item in items {
        guard let value = item.stringValue else { return nil }
        out.append(value)
    }
    return out
}

private func validSubdivision(territory: String, value: String) -> Bool {
    let prefix = territory + "-"
    guard value.hasPrefix(prefix) else { return false }
    let suffix = String(value.dropFirst(prefix.count))
    guard (1...3).contains(suffix.count) else { return false }
    return suffix.unicodeScalars.allSatisfy { scalar in
        (scalar.value >= 65 && scalar.value <= 90) ||
            (scalar.value >= 48 && scalar.value <= 57)
    }
}

private func containsControl(_ value: String) -> Bool {
    value.unicodeScalars.contains { $0.value < 0x20 || $0.value == 0x7F }
}
