// DO NOT EDIT
// Generated from canonical PDIG spec (spec/domain/domain.json)
// specVersion: 1.0.0 | appSchemaVersion: 3
// Generator: tools/codegen/generate.mjs — run `node tools/codegen/generate.mjs`

package com.pdig.core.generated

/** PDIG canonical enums — Kotlin (Android native). */
public object CanonicalSpec {
    public const val SPEC_VERSION: String = "1.0.0"
    public const val APP_SCHEMA_VERSION: Int = 3
    public const val GRAPH_PAYLOAD_KIND: String = "depmap-logical-graph"
    public const val GRAPH_PAYLOAD_VERSION: Int = 3
}

public enum class NodeKind(public val wire: String) {
    IDENTITY_ANCHOR("identity_anchor"),
    PAYMENT_INSTRUMENT("payment_instrument"),
    ACCOUNT("account"),
    SERVICE("service"),
    MEMBERSHIP("membership"),
    DEVICE("device"),
    CUSTOM("custom")
    ;

    public companion object {
        public fun fromWire(value: String): NodeKind? = entries.firstOrNull { it.wire == value }
    }
}

public enum class Relation(public val wire: String) {
    FUNDING_SOURCE("funding_source"),
    MERCHANT_AGREEMENT("merchant_agreement"),
    VERIFIES("verifies"),
    RECOVERS("recovers"),
    BOUND_TO("bound_to")
    ;

    public companion object {
        public fun fromWire(value: String): Relation? = entries.firstOrNull { it.wire == value }
    }
}

public enum class RelationRuntime(public val wire: String) {
    FUNDING_SOURCE("funding_source"),
    MERCHANT_AGREEMENT("merchant_agreement")
    ;

    public companion object {
        public fun fromWire(value: String): RelationRuntime? = entries.firstOrNull { it.wire == value }
    }
}

public enum class Capability(public val wire: String) {
    PAYMENT("payment"),
    ACCESS("access"),
    RECOVERY("recovery"),
    IDENTITY("identity")
    ;

    public companion object {
        public fun fromWire(value: String): Capability? = entries.firstOrNull { it.wire == value }
    }
}

public enum class CapabilityRuntime(public val wire: String) {
    PAYMENT("payment")
    ;

    public companion object {
        public fun fromWire(value: String): CapabilityRuntime? = entries.firstOrNull { it.wire == value }
    }
}

public enum class Criticality(public val wire: String) {
    REQUIRED("required"),
    UNKNOWN("unknown")
    ;

    public companion object {
        public fun fromWire(value: String): Criticality? = entries.firstOrNull { it.wire == value }
    }
}

public enum class DependencyState(public val wire: String) {
    ACTIVE("active"),
    RETIRED("retired")
    ;

    public companion object {
        public fun fromWire(value: String): DependencyState? = entries.firstOrNull { it.wire == value }
    }
}

public enum class DependencyOrigin(public val wire: String) {
    MANUAL("manual"),
    PROPOSAL("proposal")
    ;

    public companion object {
        public fun fromWire(value: String): DependencyOrigin? = entries.firstOrNull { it.wire == value }
    }
}

public enum class GroupMode(public val wire: String) {
    ANY("ANY"),
    ALL("ALL")
    ;

    public companion object {
        public fun fromWire(value: String): GroupMode? = entries.firstOrNull { it.wire == value }
    }
}

public enum class GroupState(public val wire: String) {
    ACTIVE("active"),
    RETIRED("retired")
    ;

    public companion object {
        public fun fromWire(value: String): GroupState? = entries.firstOrNull { it.wire == value }
    }
}

public enum class ProposalDecision(public val wire: String) {
    PENDING("pending"),
    ACCEPTED("accepted"),
    REJECTED("rejected")
    ;

    public companion object {
        public fun fromWire(value: String): ProposalDecision? = entries.firstOrNull { it.wire == value }
    }
}

public enum class VerificationBasisType(public val wire: String) {
    USER_CONFIRMED("user_confirmed"),
    AUTHORITATIVE_SOURCE("authoritative_source")
    ;

    public companion object {
        public fun fromWire(value: String): VerificationBasisType? = entries.firstOrNull { it.wire == value }
    }
}

public enum class SourceKind(public val wire: String) {
    STATEMENT_FILE("statement_file"),
    PLATFORM_EXPORT("platform_export"),
    OPEN_BANKING("open_banking"),
    MANUAL("manual"),
    DISCOVERY("discovery")
    ;

    public companion object {
        public fun fromWire(value: String): SourceKind? = entries.firstOrNull { it.wire == value }
    }
}

public enum class CoverageMode(public val wire: String) {
    EVENT_STREAM("event_stream"),
    PARTIAL_SNAPSHOT("partial_snapshot"),
    COMPLETE_SNAPSHOT("complete_snapshot"),
    USER_SELECTED("user_selected")
    ;

    public companion object {
        public fun fromWire(value: String): CoverageMode? = entries.firstOrNull { it.wire == value }
    }
}

public enum class SourceInstanceState(public val wire: String) {
    ACTIVE("active"),
    RETIRED("retired")
    ;

    public companion object {
        public fun fromWire(value: String): SourceInstanceState? = entries.firstOrNull { it.wire == value }
    }
}

public enum class ObservationDirection(public val wire: String) {
    IN("in"),
    OUT("out"),
    NEUTRAL("neutral")
    ;

    public companion object {
        public fun fromWire(value: String): ObservationDirection? = entries.firstOrNull { it.wire == value }
    }
}

public enum class EvidenceKind(public val wire: String) {
    TRANSACTION_STREAM("transaction_stream")
    ;

    public companion object {
        public fun fromWire(value: String): EvidenceKind? = entries.firstOrNull { it.wire == value }
    }
}

public enum class ProposalType(public val wire: String) {
    RECURRING_PAYMENT_ROUTE("recurring_payment_route")
    ;

    public companion object {
        public fun fromWire(value: String): ProposalType? = entries.firstOrNull { it.wire == value }
    }
}

public enum class ProposalSource(public val wire: String) {
    STATEMENT("statement")
    ;

    public companion object {
        public fun fromWire(value: String): ProposalSource? = entries.firstOrNull { it.wire == value }
    }
}

public enum class ImpactLevel(public val wire: String) {
    MUST_CHANGE("must_change"),
    BACKUP_PATH("backup_path"),
    DEGRADED("degraded"),
    NEEDS_REVIEW("needs_review"),
    UNAFFECTED("unaffected"),
    TARGET_OPERATION("target_operation")
    ;

    public companion object {
        public fun fromWire(value: String): ImpactLevel? = entries.firstOrNull { it.wire == value }
    }
}

public enum class ImpactTargetStatus(public val wire: String) {
    MUST_CHANGE("must_change"),
    BACKUP_PATH("backup_path"),
    DEGRADED("degraded"),
    NEEDS_REVIEW("needs_review"),
    UNAFFECTED("unaffected")
    ;

    public companion object {
        public fun fromWire(value: String): ImpactTargetStatus? = entries.firstOrNull { it.wire == value }
    }
}

public enum class ImpactReasonCode(public val wire: String) {
    REQUIRED_EDGE_NO_ALTERNATIVE("required_edge_no_alternative"),
    CONFIRMED_GROUP_FAILED("confirmed_group_failed"),
    CONFIRMED_GROUP_COVERED("confirmed_group_covered"),
    UNCONFIRMED_ALTERNATIVE_EXISTS("unconfirmed_alternative_exists"),
    CRITICALITY_UNKNOWN("criticality_unknown"),
    PROPOSAL_ONLY("proposal_only"),
    UPSTREAM_UNCERTAIN("upstream_uncertain")
    ;

    public companion object {
        public fun fromWire(value: String): ImpactReasonCode? = entries.firstOrNull { it.wire == value }
    }
}

public enum class ChangePlanWorkflowState(public val wire: String) {
    DRAFT("draft"),
    ANALYZED("analyzed"),
    REVIEW_REQUIRED("review_required"),
    READY("ready"),
    IN_PROGRESS("in_progress"),
    VERIFYING("verifying"),
    COMPLETED("completed"),
    CANCELLED("cancelled")
    ;

    public companion object {
        public fun fromWire(value: String): ChangePlanWorkflowState? = entries.firstOrNull { it.wire == value }
    }
}

public enum class PlanEffectiveStatus(public val wire: String) {
    DRAFT("draft"),
    ANALYZED("analyzed"),
    REVIEW_REQUIRED("review_required"),
    READY("ready"),
    IN_PROGRESS("in_progress"),
    VERIFYING("verifying"),
    COMPLETED("completed"),
    CANCELLED("cancelled"),
    NEEDS_REVALIDATION("needs_revalidation")
    ;

    public companion object {
        public fun fromWire(value: String): PlanEffectiveStatus? = entries.firstOrNull { it.wire == value }
    }
}

public enum class PlanActionPhase(public val wire: String) {
    PREPARE("prepare"),
    CHANGE("change"),
    VERIFY("verify")
    ;

    public companion object {
        public fun fromWire(value: String): PlanActionPhase? = entries.firstOrNull { it.wire == value }
    }
}

public enum class ActionVerificationMethod(public val wire: String) {
    MANUAL_CONFIRMATION("manual_confirmation"),
    FUTURE_OBSERVATION("future_observation"),
    AUTHORITATIVE_SOURCE("authoritative_source")
    ;

    public companion object {
        public fun fromWire(value: String): ActionVerificationMethod? = entries.firstOrNull { it.wire == value }
    }
}

public enum class ActionVerificationStatus(public val wire: String) {
    NOT_REQUIRED("not_required"),
    PENDING("pending"),
    EVIDENCE_SUGGESTED("evidence_suggested"),
    VERIFIED("verified"),
    FAILED("failed")
    ;

    public companion object {
        public fun fromWire(value: String): ActionVerificationStatus? = entries.firstOrNull { it.wire == value }
    }
}

public enum class PlanReadiness(public val wire: String) {
    BLOCKED("blocked"),
    REVIEW_REQUIRED("review_required"),
    READY_WITH_KNOWN_SCOPE("ready_with_known_scope")
    ;

    public companion object {
        public fun fromWire(value: String): PlanReadiness? = entries.firstOrNull { it.wire == value }
    }
}

public enum class CoverageLevel(public val wire: String) {
    UNKNOWN("unknown"),
    LIMITED("limited"),
    PARTIAL("partial"),
    WELL_EVIDENCED("well_evidenced")
    ;

    public companion object {
        public fun fromWire(value: String): CoverageLevel? = entries.firstOrNull { it.wire == value }
    }
}

public enum class DriftKind(public val wire: String) {
    POSSIBLE_REPLACEMENT("possible_replacement"),
    POSSIBLE_ADDITIONAL_PATH("possible_additional_path"),
    RELATION_REAPPEARED("relation_reappeared")
    ;

    public companion object {
        public fun fromWire(value: String): DriftKind? = entries.firstOrNull { it.wire == value }
    }
}

public enum class DriftStatus(public val wire: String) {
    OPEN("open"),
    CONFIRMED_CHANGE("confirmed_change"),
    DISMISSED("dismissed"),
    SUPERSEDED("superseded")
    ;

    public companion object {
        public fun fromWire(value: String): DriftStatus? = entries.firstOrNull { it.wire == value }
    }
}

public enum class DriftIgnoreReason(public val wire: String) {
    ALREADY_CONFIRMED("already_confirmed"),
    BELOW_THRESHOLD("below_threshold")
    ;

    public companion object {
        public fun fromWire(value: String): DriftIgnoreReason? = entries.firstOrNull { it.wire == value }
    }
}

public enum class CandidateKind(public val wire: String) {
    PAYMENT_INSTRUMENT("payment_instrument"),
    SERVICE("service")
    ;

    public companion object {
        public fun fromWire(value: String): CandidateKind? = entries.firstOrNull { it.wire == value }
    }
}

public enum class CandidateStatus(public val wire: String) {
    PENDING("pending"),
    ACCEPTED("accepted"),
    DISMISSED("dismissed"),
    SUPERSEDED("superseded")
    ;

    public companion object {
        public fun fromWire(value: String): CandidateStatus? = entries.firstOrNull { it.wire == value }
    }
}

public enum class TimelineBucket(public val wire: String) {
    ATTENTION("attention"),
    OVERDUE("overdue"),
    TODAY("today"),
    V7D("7d"),
    V30D("30d"),
    V90D("90d"),
    LATER("later")
    ;

    public companion object {
        public fun fromWire(value: String): TimelineBucket? = entries.firstOrNull { it.wire == value }
    }
}

public enum class TimelineItemKind(public val wire: String) {
    NEEDS_ATTENTION("needs_attention"),
    UPCOMING_CHANGE("upcoming_change"),
    VERIFICATION_PENDING("verification_pending"),
    FRESHNESS_REVIEW("freshness_review"),
    DRIFT_REVIEW("drift_review"),
    EXPIRATION("expiration")
    ;

    public companion object {
        public fun fromWire(value: String): TimelineItemKind? = entries.firstOrNull { it.wire == value }
    }
}

public enum class TimelineSourceType(public val wire: String) {
    CHANGE_PLAN("change_plan"),
    REALITY_DRIFT("reality_drift"),
    ACTION_VERIFICATION("action_verification"),
    NODE_EXPIRY("node_expiry"),
    SOURCE_FRESHNESS("source_freshness")
    ;

    public companion object {
        public fun fromWire(value: String): TimelineSourceType? = entries.firstOrNull { it.wire == value }
    }
}

public enum class ScenarioCategory(public val wire: String) {
    PAYMENT("payment"),
    IDENTITY("identity"),
    DEVICE("device"),
    WORK("work"),
    INTERNATIONAL("international"),
    DIGITAL_ASSETS("digital_assets")
    ;

    public companion object {
        public fun fromWire(value: String): ScenarioCategory? = entries.firstOrNull { it.wire == value }
    }
}

public enum class ScenarioAvailability(public val wire: String) {
    ACTIVE("active"),
    PLANNED("planned")
    ;

    public companion object {
        public fun fromWire(value: String): ScenarioAvailability? = entries.firstOrNull { it.wire == value }
    }
}

public enum class AmountSignMode(public val wire: String) {
    SIGNED("signed"),
    DEBIT_CREDIT("debit_credit"),
    OUTWARD_POSITIVE("outward_positive")
    ;

    public companion object {
        public fun fromWire(value: String): AmountSignMode? = entries.firstOrNull { it.wire == value }
    }
}

public enum class CsvDelimiter(public val wire: String) {
    U2C(","),
    U3B(";"),
    U09("	"),
    U7C("|")
    ;

    public companion object {
        public fun fromWire(value: String): CsvDelimiter? = entries.firstOrNull { it.wire == value }
    }
}

public enum class SourceEncoding(public val wire: String) {
    UTF_8("utf-8"),
    GB18030("gb18030")
    ;

    public companion object {
        public fun fromWire(value: String): SourceEncoding? = entries.firstOrNull { it.wire == value }
    }
}

public enum class ErrorCode(public val wire: String, public val category: String, public val messageKey: String) {
    INVALID_JSON("invalid_json", "container", "err.invalid_json"),
    INVALID_STRUCTURE("invalid_structure", "container", "err.invalid_structure"),
    BOUNDS_EXCEEDED("bounds_exceeded", "container", "err.bounds"),
    KDF_FAILED("kdf_failed", "crypto", "err.kdf"),
    CRYPTO_AUTH_FAILED("crypto_auth_failed", "crypto", "err.auth"),
    UNSUPPORTED_FORMAT_VERSION("unsupported_format_version", "container", "err.format_version"),
    UNSUPPORTED_SCHEMA("unsupported_schema", "schema", "err.schema_newer"),
    MIGRATION_FAILED("migration_failed", "schema", "err.migration"),
    PAYLOAD_INVALID("payload_invalid", "payload", "err.payload"),
    PAYLOAD_KIND_MISMATCH("payload_kind_mismatch", "payload", "err.payload_kind"),
    PAYLOAD_VERSION_UNSUPPORTED("payload_version_unsupported", "payload", "err.payload_version"),
    SOURCE_PARSE_FAILED("source_parse_failed", "import", "err.parse"),
    MAPPING_MISSING_COLUMN("mapping_missing_column", "import", "err.mapping"),
    VALIDATION_FAILED("validation_failed", "domain", "err.validation"),
    RELATION_NOT_REGISTERED("relation_not_registered", "domain", "err.relation"),
    RELATION_KIND_VIOLATION("relation_kind_violation", "domain", "err.relation_kind"),
    CAPABILITY_MISMATCH("capability_mismatch", "domain", "err.capability"),
    GROUP_MODE_NOT_ALLOWED("group_mode_not_allowed", "domain", "err.group_mode"),
    ILLEGAL_STATE_TRANSITION("illegal_state_transition", "domain", "err.state"),
    ACTION_FROZEN("action_frozen", "domain", "err.action_frozen"),
    ENTITY_NOT_FOUND("entity_not_found", "domain", "err.not_found"),
    SCENARIO_NOT_EXECUTABLE("scenario_not_executable", "scenario", "err.scenario_planned"),
    SCENARIO_MISSING_INPUT("scenario_missing_input", "scenario", "err.scenario_input"),
    DB_LOCKED("db_locked", "storage", "err.db_locked"),
    DB_OPEN_FAILED("db_open_failed", "storage", "err.db_open"),
    AUTH_CANCELLED("auth_cancelled", "security", "err.auth_cancelled"),
    AUTH_FAILED("auth_failed", "security", "err.auth_failed"),
    AUTH_UNAVAILABLE("auth_unavailable", "security", "err.auth_unavailable"),
    KEY_UNAVAILABLE("key_unavailable", "security", "err.key"),
    CANCELLED_BY_USER("cancelled_by_user", "ui", "err.cancelled")
    ;

    public companion object {
        public fun fromWire(value: String): ErrorCode? = entries.firstOrNull { it.wire == value }
    }
}

public object DepmapContainerV1 {
    public const val FORMAT: String = "depmap"
    public const val FORMAT_VERSION: Int = 1
    public const val KDF_ALGORITHM: String = "argon2id"
    public const val KDF_VERSION: Int = 19
    public const val SALT_BYTES: Int = 16
    public const val MEMORY_KIB: Int = 65536
    public const val ITERATIONS: Int = 3
    public const val PARALLELISM: Int = 1
    public const val KEY_BYTES: Int = 32
    public const val CIPHER_ALGORITHM: String = "AES-256-GCM"
    public const val NONCE_BYTES: Int = 12
    public const val TAG_BYTES: Int = 16
    public const val MEMORY_KIB_MIN: Int = 16384
    public const val MEMORY_KIB_MAX: Int = 262144
    public const val ITERATIONS_MIN: Int = 1
    public const val ITERATIONS_MAX: Int = 10
    public const val PARALLELISM_MIN: Int = 1
    public const val PARALLELISM_MAX: Int = 4
    public const val CIPHERTEXT_MAX_BYTES: Long = 67108864L
}
