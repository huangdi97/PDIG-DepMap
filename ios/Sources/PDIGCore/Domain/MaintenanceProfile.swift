import Foundation

public struct ConfirmedMaintenanceFact: Equatable, Sendable {
    public let id: String
    public let kind: MaintenanceFactKind
    public let valueType: MaintenanceValueType
    public let value: String
    public let state: MaintenanceFactState
    public let verificationBasisType: VerificationBasisType
    public let confirmedAt: String
    public let evidenceRefs: [String]
    public let validFrom: String?
    public let validUntil: String?
    public let retiredAt: String?
}

public struct ConfirmedMaintenanceCadence: Equatable, Sendable {
    public let kind: MaintenanceCadenceKind
    public let dueAt: String?
    public let dayOfMonth: Int?
    public let month: Int?
    public let day: Int?
    public let overflowPolicy: MaintenanceOverflowPolicy?
    public let intervalDays: Int?
    public let anchorDate: String?

    public init(
        kind: MaintenanceCadenceKind,
        dueAt: String? = nil,
        dayOfMonth: Int? = nil,
        month: Int? = nil,
        day: Int? = nil,
        overflowPolicy: MaintenanceOverflowPolicy? = nil,
        intervalDays: Int? = nil,
        anchorDate: String? = nil
    ) {
        self.kind = kind
        self.dueAt = dueAt
        self.dayOfMonth = dayOfMonth
        self.month = month
        self.day = day
        self.overflowPolicy = overflowPolicy
        self.intervalDays = intervalDays
        self.anchorDate = anchorDate
    }
}

public struct ConfirmedMaintenanceSchedule: Equatable, Sendable {
    public let id: String
    public let kind: MaintenanceScheduleKind
    public let state: MaintenanceScheduleState
    public let cadence: ConfirmedMaintenanceCadence
    public let verificationBasisType: VerificationBasisType
    public let confirmedAt: String
    public let evidenceRefs: [String]
    public let lastCompletedAt: String?
    public let retiredAt: String?
}

public struct ConfirmedMaintenanceProfile: Equatable, Sendable {
    public let facts: [ConfirmedMaintenanceFact]
    public let schedules: [ConfirmedMaintenanceSchedule]
}

/// Canonical R40 maintenance profile decoder.
///
/// Provider policy, display text and elapsed time never create confirmed Reality.
/// Individual malformed items fail closed without destroying valid siblings.
public func governedMaintenanceProfile(
    kind: NodeKind,
    fieldsJson: String
) -> ConfirmedMaintenanceProfile {
    guard
        let parsed = try? JsonParser.parse(fieldsJson),
        let root = parsed.objectValue,
        let container = root["maintenance_profile"]?.objectValue,
        let versionJson = container["version"],
        let version = try? versionJson.asLong,
        version == 1
    else {
        return ConfirmedMaintenanceProfile(facts: [], schedules: [])
    }

    let phoneAuthority =
        kind == .identityAnchor &&
        confirmedIdentityAnchorProfile(kind: kind, fieldsJson: fieldsJson)?.subtype == .phoneNumber

    var facts: [ConfirmedMaintenanceFact] = []
    if let rawFacts = container["facts"] {
        if let items = rawFacts.arrayValue {
            facts = items.compactMap { raw in
                guard let item = raw.objectValue else { return nil }
                return decodeMaintenanceFact(
                    nodeKind: kind,
                    phoneAuthority: phoneAuthority,
                    item: item
                )
            }
        }
    }

    var schedules: [ConfirmedMaintenanceSchedule] = []
    if let rawSchedules = container["schedules"] {
        if let items = rawSchedules.arrayValue {
            schedules = items.compactMap { raw in
                guard let item = raw.objectValue else { return nil }
                return decodeMaintenanceSchedule(
                    nodeKind: kind,
                    phoneAuthority: phoneAuthority,
                    item: item
                )
            }
        }
    }

    return ConfirmedMaintenanceProfile(facts: facts, schedules: schedules)
}

public func currentMaintenanceFacts(
    kind: NodeKind,
    fieldsJson: String
) -> [ConfirmedMaintenanceFact] {
    governedMaintenanceProfile(kind: kind, fieldsJson: fieldsJson).facts.filter {
        $0.state == .confirmed
    }
}

public func currentMaintenanceSchedules(
    kind: NodeKind,
    fieldsJson: String
) -> [ConfirmedMaintenanceSchedule] {
    governedMaintenanceProfile(kind: kind, fieldsJson: fieldsJson).schedules.filter {
        $0.state == .active || $0.state == .needsReview
    }
}

private func decodeMaintenanceFact(
    nodeKind: NodeKind,
    phoneAuthority: Bool,
    item: JsonObject
) -> ConfirmedMaintenanceFact? {
    guard
        let id = requiredText(item, "id"),
        validBoundedText(id, max: 160),
        let kindWire = item["kind"]?.stringValue,
        let factKind = MaintenanceFactKind(rawValue: kindWire),
        factApplicable(nodeKind: nodeKind, phoneAuthority: phoneAuthority, factKind: factKind),
        let valueTypeWire = item["value_type"]?.stringValue,
        let valueType = MaintenanceValueType(rawValue: valueTypeWire),
        let value = requiredText(item, "value"),
        validFactValue(kind: factKind, valueType: valueType, value: value),
        let stateWire = item["state"]?.stringValue,
        let state = MaintenanceFactState(rawValue: stateWire),
        let basisWire = item["verification_basis_type"]?.stringValue,
        let basis = VerificationBasisType(rawValue: basisWire),
        let confirmedAt = requiredText(item, "confirmed_at"),
        validBoundedText(confirmedAt, max: 96),
        let evidenceRefs = stringArray(item, "evidence_refs"),
        evidenceRefs.allSatisfy({ validBoundedText($0, max: 512) }),
        let validFrom = strictOptionalText(item, "valid_from"),
        let validUntil = strictOptionalText(item, "valid_until"),
        let retiredAt = strictOptionalText(item, "retired_at")
    else {
        return nil
    }

    return ConfirmedMaintenanceFact(
        id: id,
        kind: factKind,
        valueType: valueType,
        value: value,
        state: state,
        verificationBasisType: basis,
        confirmedAt: confirmedAt,
        evidenceRefs: evidenceRefs,
        validFrom: validFrom,
        validUntil: validUntil,
        retiredAt: retiredAt
    )
}

private func decodeMaintenanceSchedule(
    nodeKind: NodeKind,
    phoneAuthority: Bool,
    item: JsonObject
) -> ConfirmedMaintenanceSchedule? {
    guard
        let id = requiredText(item, "id"),
        validBoundedText(id, max: 160),
        let kindWire = item["kind"]?.stringValue,
        let scheduleKind = MaintenanceScheduleKind(rawValue: kindWire),
        scheduleApplicable(
            nodeKind: nodeKind,
            phoneAuthority: phoneAuthority,
            scheduleKind: scheduleKind
        ),
        let stateWire = item["state"]?.stringValue,
        let state = MaintenanceScheduleState(rawValue: stateWire),
        let cadenceObj = item["cadence"]?.objectValue,
        let cadence = decodeMaintenanceCadence(cadenceObj),
        cadenceApplicable(scheduleKind: scheduleKind, cadenceKind: cadence.kind),
        let basisWire = item["verification_basis_type"]?.stringValue,
        let basis = VerificationBasisType(rawValue: basisWire),
        let confirmedAt = requiredText(item, "confirmed_at"),
        validBoundedText(confirmedAt, max: 96),
        let evidenceRefs = stringArray(item, "evidence_refs"),
        evidenceRefs.allSatisfy({ validBoundedText($0, max: 512) }),
        let lastCompletedAt = strictOptionalText(item, "last_completed_at"),
        let retiredAt = strictOptionalText(item, "retired_at")
    else {
        return nil
    }

    return ConfirmedMaintenanceSchedule(
        id: id,
        kind: scheduleKind,
        state: state,
        cadence: cadence,
        verificationBasisType: basis,
        confirmedAt: confirmedAt,
        evidenceRefs: evidenceRefs,
        lastCompletedAt: lastCompletedAt,
        retiredAt: retiredAt
    )
}

private func decodeMaintenanceCadence(
    _ obj: JsonObject
) -> ConfirmedMaintenanceCadence? {
    guard
        let kindWire = obj["kind"]?.stringValue,
        let kind = MaintenanceCadenceKind(rawValue: kindWire)
    else {
        return nil
    }

    switch kind {
    case .oneTime:
        guard
            let dueAt = requiredText(obj, "due_at"),
            validBoundedText(dueAt, max: 96)
        else {
            return nil
        }
        return ConfirmedMaintenanceCadence(kind: kind, dueAt: dueAt)

    case .monthlyDay:
        guard
            let day = intField(obj, "day_of_month"),
            (1...31).contains(day),
            let overflow = overflowPolicy(obj)
        else {
            return nil
        }
        return ConfirmedMaintenanceCadence(
            kind: kind,
            dayOfMonth: day,
            overflowPolicy: overflow
        )

    case .yearlyMonthDay:
        guard
            let month = intField(obj, "month"),
            let day = intField(obj, "day"),
            (1...12).contains(month),
            (1...31).contains(day),
            let overflow = overflowPolicy(obj)
        else {
            return nil
        }
        return ConfirmedMaintenanceCadence(
            kind: kind,
            month: month,
            day: day,
            overflowPolicy: overflow
        )

    case .intervalDays:
        guard
            let interval = intField(obj, "interval_days"),
            (1...3660).contains(interval),
            let anchor = requiredText(obj, "anchor_date"),
            matches(anchor, pattern: #"^[0-9]{4}-[0-9]{2}-[0-9]{2}$"#)
        else {
            return nil
        }
        return ConfirmedMaintenanceCadence(
            kind: kind,
            intervalDays: interval,
            anchorDate: anchor
        )

    case .manualOnly:
        return ConfirmedMaintenanceCadence(kind: kind)
    }
}

private func factApplicable(
    nodeKind: NodeKind,
    phoneAuthority: Bool,
    factKind: MaintenanceFactKind
) -> Bool {
    switch factKind {
    case .cardAnnualFeeAmount,
         .cardAnnualFeeCurrency,
         .cardBillingDay,
         .cardPaymentDueDay,
         .cardAutopayMode:
        return nodeKind == .paymentInstrument

    case .numberBillingMode,
         .numberPlanCost,
         .numberPlanCurrency,
         .numberRenewalMethod:
        return phoneAuthority
    }
}

private func scheduleApplicable(
    nodeKind: NodeKind,
    phoneAuthority: Bool,
    scheduleKind: MaintenanceScheduleKind
) -> Bool {
    switch scheduleKind {
    case .cardAnnualFeeCheckpoint,
         .cardBillingCheckpoint,
         .cardPaymentDueCheckpoint:
        return nodeKind == .paymentInstrument

    case .numberKeepAlive,
         .numberPlanRenewal:
        return phoneAuthority

    case .factFreshnessReview,
         .customMaintenance:
        return true
    }
}

private func cadenceApplicable(
    scheduleKind: MaintenanceScheduleKind,
    cadenceKind: MaintenanceCadenceKind
) -> Bool {
    switch scheduleKind {
    case .cardAnnualFeeCheckpoint:
        return cadenceKind == .oneTime || cadenceKind == .yearlyMonthDay

    case .cardBillingCheckpoint,
         .cardPaymentDueCheckpoint:
        return cadenceKind == .monthlyDay

    case .numberKeepAlive:
        return cadenceKind == .oneTime ||
            cadenceKind == .intervalDays ||
            cadenceKind == .manualOnly

    case .numberPlanRenewal:
        return cadenceKind == .oneTime ||
            cadenceKind == .monthlyDay ||
            cadenceKind == .intervalDays

    case .factFreshnessReview:
        return cadenceKind == .intervalDays || cadenceKind == .manualOnly

    case .customMaintenance:
        return true
    }
}

private func validFactValue(
    kind: MaintenanceFactKind,
    valueType: MaintenanceValueType,
    value: String
) -> Bool {
    switch kind {
    case .cardAnnualFeeAmount, .numberPlanCost:
        return valueType == .decimalString &&
            matches(value, pattern: #"^(0|[1-9][0-9]*)(.[0-9]+)?$"#)

    case .cardAnnualFeeCurrency, .numberPlanCurrency:
        return valueType == .currencyCode &&
            matches(value, pattern: #"^[A-Z]{3}$"#)

    case .cardBillingDay, .cardPaymentDueDay:
        guard valueType == .integer, let n = Int(value) else { return false }
        return (1...31).contains(n) && String(n) == value

    case .cardAutopayMode, .numberBillingMode, .numberRenewalMethod:
        return valueType == .text && validBoundedText(value, max: 240)
    }
}

private func overflowPolicy(_ obj: JsonObject) -> MaintenanceOverflowPolicy? {
    guard let raw = obj["overflow_policy"]?.stringValue else { return nil }
    return MaintenanceOverflowPolicy(rawValue: raw)
}

private func intField(_ obj: JsonObject, _ key: String) -> Int? {
    guard let raw = obj[key], let value = try? raw.asLong else { return nil }
    guard value >= Int64(Int.min), value <= Int64(Int.max) else { return nil }
    return Int(value)
}

/// Double Optional: absent is valid nil, present-invalid is failure.
private func strictOptionalText(_ obj: JsonObject, _ key: String) -> String?? {
    guard let raw = obj[key] else { return .some(nil) }
    guard let text = raw.stringValue else { return nil }
    let value = text.trimmingCharacters(in: .whitespacesAndNewlines)
    guard validBoundedText(value, max: 128) else { return nil }
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

private func validBoundedText(_ value: String, max: Int) -> Bool {
    !value.isEmpty &&
        value.count <= max &&
        !value.unicodeScalars.contains(where: { $0.value < 0x20 || $0.value == 0x7F })
}

private func matches(_ value: String, pattern: String) -> Bool {
    value.range(of: pattern, options: .regularExpression) != nil
}
