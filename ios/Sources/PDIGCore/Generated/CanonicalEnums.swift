// DO NOT EDIT
// Generated from canonical PDIG spec (spec/domain/domain.json)
// specVersion: 1.0.0 | appSchemaVersion: 3
// Generator: tools/codegen/generate.mjs — run `node tools/codegen/generate.mjs`

import Foundation

/// PDIG canonical enums — Swift (iOS native).
public enum CanonicalSpec {
    public static let specVersion = "1.0.0"
    public static let appSchemaVersion = 3
    public static let graphPayloadKind = "depmap-logical-graph"
    public static let graphPayloadVersion = 3
}

public enum NodeKind: String, CaseIterable, Sendable, Equatable {
    case identityAnchor = "identity_anchor"
    case paymentInstrument = "payment_instrument"
    case account = "account"
    case service = "service"
    case membership = "membership"
    case device = "device"
    case custom = "custom"

    public var wire: String { rawValue }
}

public enum Relation: String, CaseIterable, Sendable, Equatable {
    case fundingSource = "funding_source"
    case merchantAgreement = "merchant_agreement"
    case verifies = "verifies"
    case recovers = "recovers"
    case boundTo = "bound_to"

    public var wire: String { rawValue }
}

public enum RelationRuntime: String, CaseIterable, Sendable, Equatable {
    case fundingSource = "funding_source"
    case merchantAgreement = "merchant_agreement"

    public var wire: String { rawValue }
}

public enum Capability: String, CaseIterable, Sendable, Equatable {
    case payment = "payment"
    case access = "access"
    case recovery = "recovery"
    case identity = "identity"

    public var wire: String { rawValue }
}

public enum CapabilityRuntime: String, CaseIterable, Sendable, Equatable {
    case payment = "payment"

    public var wire: String { rawValue }
}

public enum Criticality: String, CaseIterable, Sendable, Equatable {
    case required = "required"
    case unknown = "unknown"

    public var wire: String { rawValue }
}

public enum DependencyState: String, CaseIterable, Sendable, Equatable {
    case active = "active"
    case retired = "retired"

    public var wire: String { rawValue }
}

public enum DependencyOrigin: String, CaseIterable, Sendable, Equatable {
    case manual = "manual"
    case proposal = "proposal"

    public var wire: String { rawValue }
}

public enum GroupMode: String, CaseIterable, Sendable, Equatable {
    case `any` = "ANY"
    case all = "ALL"

    public var wire: String { rawValue }
}

public enum GroupState: String, CaseIterable, Sendable, Equatable {
    case active = "active"
    case retired = "retired"

    public var wire: String { rawValue }
}

public enum ProposalDecision: String, CaseIterable, Sendable, Equatable {
    case pending = "pending"
    case accepted = "accepted"
    case rejected = "rejected"

    public var wire: String { rawValue }
}

public enum VerificationBasisType: String, CaseIterable, Sendable, Equatable {
    case userConfirmed = "user_confirmed"
    case authoritativeSource = "authoritative_source"

    public var wire: String { rawValue }
}

public enum SourceKind: String, CaseIterable, Sendable, Equatable {
    case statementFile = "statement_file"
    case platformExport = "platform_export"
    case openBanking = "open_banking"
    case manual = "manual"
    case discovery = "discovery"

    public var wire: String { rawValue }
}

public enum CoverageMode: String, CaseIterable, Sendable, Equatable {
    case eventStream = "event_stream"
    case partialSnapshot = "partial_snapshot"
    case completeSnapshot = "complete_snapshot"
    case userSelected = "user_selected"

    public var wire: String { rawValue }
}

public enum SourceInstanceState: String, CaseIterable, Sendable, Equatable {
    case active = "active"
    case retired = "retired"

    public var wire: String { rawValue }
}

public enum ObservationDirection: String, CaseIterable, Sendable, Equatable {
    case `in` = "in"
    case out = "out"
    case neutral = "neutral"

    public var wire: String { rawValue }
}

public enum EvidenceKind: String, CaseIterable, Sendable, Equatable {
    case transactionStream = "transaction_stream"

    public var wire: String { rawValue }
}

public enum ProposalType: String, CaseIterable, Sendable, Equatable {
    case recurringPaymentRoute = "recurring_payment_route"

    public var wire: String { rawValue }
}

public enum ProposalSource: String, CaseIterable, Sendable, Equatable {
    case statement = "statement"

    public var wire: String { rawValue }
}

public enum ImpactLevel: String, CaseIterable, Sendable, Equatable {
    case mustChange = "must_change"
    case backupPath = "backup_path"
    case degraded = "degraded"
    case needsReview = "needs_review"
    case unaffected = "unaffected"
    case targetOperation = "target_operation"

    public var wire: String { rawValue }
}

public enum ImpactTargetStatus: String, CaseIterable, Sendable, Equatable {
    case mustChange = "must_change"
    case backupPath = "backup_path"
    case degraded = "degraded"
    case needsReview = "needs_review"
    case unaffected = "unaffected"

    public var wire: String { rawValue }
}

public enum ImpactReasonCode: String, CaseIterable, Sendable, Equatable {
    case requiredEdgeNoAlternative = "required_edge_no_alternative"
    case confirmedGroupFailed = "confirmed_group_failed"
    case confirmedGroupCovered = "confirmed_group_covered"
    case unconfirmedAlternativeExists = "unconfirmed_alternative_exists"
    case criticalityUnknown = "criticality_unknown"
    case proposalOnly = "proposal_only"
    case upstreamUncertain = "upstream_uncertain"

    public var wire: String { rawValue }
}

public enum ChangePlanWorkflowState: String, CaseIterable, Sendable, Equatable {
    case draft = "draft"
    case analyzed = "analyzed"
    case reviewRequired = "review_required"
    case ready = "ready"
    case inProgress = "in_progress"
    case verifying = "verifying"
    case completed = "completed"
    case cancelled = "cancelled"

    public var wire: String { rawValue }
}

public enum PlanEffectiveStatus: String, CaseIterable, Sendable, Equatable {
    case draft = "draft"
    case analyzed = "analyzed"
    case reviewRequired = "review_required"
    case ready = "ready"
    case inProgress = "in_progress"
    case verifying = "verifying"
    case completed = "completed"
    case cancelled = "cancelled"
    case needsRevalidation = "needs_revalidation"

    public var wire: String { rawValue }
}

public enum PlanActionPhase: String, CaseIterable, Sendable, Equatable {
    case prepare = "prepare"
    case change = "change"
    case verify = "verify"

    public var wire: String { rawValue }
}

public enum ActionVerificationMethod: String, CaseIterable, Sendable, Equatable {
    case manualConfirmation = "manual_confirmation"
    case futureObservation = "future_observation"
    case authoritativeSource = "authoritative_source"

    public var wire: String { rawValue }
}

public enum ActionVerificationStatus: String, CaseIterable, Sendable, Equatable {
    case notRequired = "not_required"
    case pending = "pending"
    case evidenceSuggested = "evidence_suggested"
    case verified = "verified"
    case failed = "failed"

    public var wire: String { rawValue }
}

public enum PlanReadiness: String, CaseIterable, Sendable, Equatable {
    case blocked = "blocked"
    case reviewRequired = "review_required"
    case readyWithKnownScope = "ready_with_known_scope"

    public var wire: String { rawValue }
}

public enum CoverageLevel: String, CaseIterable, Sendable, Equatable {
    case unknown = "unknown"
    case limited = "limited"
    case partial = "partial"
    case wellEvidenced = "well_evidenced"

    public var wire: String { rawValue }
}

public enum DriftKind: String, CaseIterable, Sendable, Equatable {
    case possibleReplacement = "possible_replacement"
    case possibleAdditionalPath = "possible_additional_path"
    case relationReappeared = "relation_reappeared"

    public var wire: String { rawValue }
}

public enum DriftStatus: String, CaseIterable, Sendable, Equatable {
    case `open` = "open"
    case confirmedChange = "confirmed_change"
    case dismissed = "dismissed"
    case superseded = "superseded"

    public var wire: String { rawValue }
}

public enum DriftIgnoreReason: String, CaseIterable, Sendable, Equatable {
    case alreadyConfirmed = "already_confirmed"
    case belowThreshold = "below_threshold"

    public var wire: String { rawValue }
}

public enum CandidateKind: String, CaseIterable, Sendable, Equatable {
    case paymentInstrument = "payment_instrument"
    case service = "service"

    public var wire: String { rawValue }
}

public enum CandidateStatus: String, CaseIterable, Sendable, Equatable {
    case pending = "pending"
    case accepted = "accepted"
    case dismissed = "dismissed"
    case superseded = "superseded"

    public var wire: String { rawValue }
}

public enum TimelineBucket: String, CaseIterable, Sendable, Equatable {
    case attention = "attention"
    case overdue = "overdue"
    case today = "today"
    case v7d = "7d"
    case v30d = "30d"
    case v90d = "90d"
    case later = "later"

    public var wire: String { rawValue }
}

public enum TimelineItemKind: String, CaseIterable, Sendable, Equatable {
    case needsAttention = "needs_attention"
    case upcomingChange = "upcoming_change"
    case verificationPending = "verification_pending"
    case freshnessReview = "freshness_review"
    case driftReview = "drift_review"
    case expiration = "expiration"

    public var wire: String { rawValue }
}

public enum TimelineSourceType: String, CaseIterable, Sendable, Equatable {
    case changePlan = "change_plan"
    case realityDrift = "reality_drift"
    case actionVerification = "action_verification"
    case nodeExpiry = "node_expiry"
    case sourceFreshness = "source_freshness"

    public var wire: String { rawValue }
}

public enum ScenarioCategory: String, CaseIterable, Sendable, Equatable {
    case payment = "payment"
    case identity = "identity"
    case device = "device"
    case work = "work"
    case international = "international"
    case digitalAssets = "digital_assets"

    public var wire: String { rawValue }
}

public enum ScenarioAvailability: String, CaseIterable, Sendable, Equatable {
    case active = "active"
    case planned = "planned"

    public var wire: String { rawValue }
}

public enum AmountSignMode: String, CaseIterable, Sendable, Equatable {
    case signed = "signed"
    case debitCredit = "debit_credit"
    case outwardPositive = "outward_positive"

    public var wire: String { rawValue }
}

public enum CsvDelimiter: String, CaseIterable, Sendable, Equatable {
    case u2c = ","
    case u3b = ";"
    case u09 = "\t"
    case u7c = "|"

    public var wire: String { rawValue }
}

public enum SourceEncoding: String, CaseIterable, Sendable, Equatable {
    case utf8 = "utf-8"
    case gb18030 = "gb18030"

    public var wire: String { rawValue }
}

public enum ErrorCode: String, CaseIterable, Sendable, Equatable {
    case invalidJson = "invalid_json"
    case invalidStructure = "invalid_structure"
    case boundsExceeded = "bounds_exceeded"
    case kdfFailed = "kdf_failed"
    case cryptoAuthFailed = "crypto_auth_failed"
    case unsupportedFormatVersion = "unsupported_format_version"
    case unsupportedSchema = "unsupported_schema"
    case migrationFailed = "migration_failed"
    case payloadInvalid = "payload_invalid"
    case payloadKindMismatch = "payload_kind_mismatch"
    case payloadVersionUnsupported = "payload_version_unsupported"
    case sourceParseFailed = "source_parse_failed"
    case mappingMissingColumn = "mapping_missing_column"
    case validationFailed = "validation_failed"
    case relationNotRegistered = "relation_not_registered"
    case relationKindViolation = "relation_kind_violation"
    case capabilityMismatch = "capability_mismatch"
    case groupModeNotAllowed = "group_mode_not_allowed"
    case illegalStateTransition = "illegal_state_transition"
    case actionFrozen = "action_frozen"
    case entityNotFound = "entity_not_found"
    case scenarioNotExecutable = "scenario_not_executable"
    case scenarioMissingInput = "scenario_missing_input"
    case dbLocked = "db_locked"
    case dbOpenFailed = "db_open_failed"
    case authCancelled = "auth_cancelled"
    case authFailed = "auth_failed"
    case authUnavailable = "auth_unavailable"
    case keyUnavailable = "key_unavailable"
    case cancelledByUser = "cancelled_by_user"

    public var wire: String { rawValue }

    public var category: String {
        switch self {
        case .invalidJson: return "container"
        case .invalidStructure: return "container"
        case .boundsExceeded: return "container"
        case .kdfFailed: return "crypto"
        case .cryptoAuthFailed: return "crypto"
        case .unsupportedFormatVersion: return "container"
        case .unsupportedSchema: return "schema"
        case .migrationFailed: return "schema"
        case .payloadInvalid: return "payload"
        case .payloadKindMismatch: return "payload"
        case .payloadVersionUnsupported: return "payload"
        case .sourceParseFailed: return "import"
        case .mappingMissingColumn: return "import"
        case .validationFailed: return "domain"
        case .relationNotRegistered: return "domain"
        case .relationKindViolation: return "domain"
        case .capabilityMismatch: return "domain"
        case .groupModeNotAllowed: return "domain"
        case .illegalStateTransition: return "domain"
        case .actionFrozen: return "domain"
        case .entityNotFound: return "domain"
        case .scenarioNotExecutable: return "scenario"
        case .scenarioMissingInput: return "scenario"
        case .dbLocked: return "storage"
        case .dbOpenFailed: return "storage"
        case .authCancelled: return "security"
        case .authFailed: return "security"
        case .authUnavailable: return "security"
        case .keyUnavailable: return "security"
        case .cancelledByUser: return "ui"
        }
    }

    public var messageKey: String {
        switch self {
        case .invalidJson: return "err.invalid_json"
        case .invalidStructure: return "err.invalid_structure"
        case .boundsExceeded: return "err.bounds"
        case .kdfFailed: return "err.kdf"
        case .cryptoAuthFailed: return "err.auth"
        case .unsupportedFormatVersion: return "err.format_version"
        case .unsupportedSchema: return "err.schema_newer"
        case .migrationFailed: return "err.migration"
        case .payloadInvalid: return "err.payload"
        case .payloadKindMismatch: return "err.payload_kind"
        case .payloadVersionUnsupported: return "err.payload_version"
        case .sourceParseFailed: return "err.parse"
        case .mappingMissingColumn: return "err.mapping"
        case .validationFailed: return "err.validation"
        case .relationNotRegistered: return "err.relation"
        case .relationKindViolation: return "err.relation_kind"
        case .capabilityMismatch: return "err.capability"
        case .groupModeNotAllowed: return "err.group_mode"
        case .illegalStateTransition: return "err.state"
        case .actionFrozen: return "err.action_frozen"
        case .entityNotFound: return "err.not_found"
        case .scenarioNotExecutable: return "err.scenario_planned"
        case .scenarioMissingInput: return "err.scenario_input"
        case .dbLocked: return "err.db_locked"
        case .dbOpenFailed: return "err.db_open"
        case .authCancelled: return "err.auth_cancelled"
        case .authFailed: return "err.auth_failed"
        case .authUnavailable: return "err.auth_unavailable"
        case .keyUnavailable: return "err.key"
        case .cancelledByUser: return "err.cancelled"
        }
    }
}

public enum DepmapContainerV1 {
    public static let format = "depmap"
    public static let formatVersion = 1
    public static let kdfAlgorithm = "argon2id"
    public static let kdfVersion = 19
    public static let saltBytes = 16
    public static let memoryKiB = 65536
    public static let iterations = 3
    public static let parallelism = 1
    public static let keyBytes = 32
    public static let cipherAlgorithm = "AES-256-GCM"
    public static let nonceBytes = 12
    public static let tagBytes = 16
    public static let memoryKiBMin = 16384
    public static let memoryKiBMax = 262144
    public static let iterationsMin = 1
    public static let iterationsMax = 10
    public static let parallelismMin = 1
    public static let parallelismMax = 4
    public static let ciphertextMaxBytes = 67108864
}
