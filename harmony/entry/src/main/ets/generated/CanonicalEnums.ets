// DO NOT EDIT
// Generated from canonical PDIG spec (spec/domain/domain.json)
// specVersion: 1.0.0 | appSchemaVersion: 3
// Generator: tools/codegen/generate.mjs — run `node tools/codegen/generate.mjs`

/** PDIG canonical enums — ArkTS (HarmonyOS native). */
export class CanonicalSpec {
  static readonly SPEC_VERSION: string = '1.0.0'
  static readonly APP_SCHEMA_VERSION: number = 3
  static readonly GRAPH_PAYLOAD_KIND: string = 'depmap-logical-graph'
  static readonly GRAPH_PAYLOAD_VERSION: number = 3
}

export enum NodeKind {
  IDENTITY_ANCHOR = 'identity_anchor',
  PAYMENT_INSTRUMENT = 'payment_instrument',
  ACCOUNT = 'account',
  SERVICE = 'service',
  MEMBERSHIP = 'membership',
  DEVICE = 'device',
  CUSTOM = 'custom'
}

export enum Relation {
  FUNDING_SOURCE = 'funding_source',
  MERCHANT_AGREEMENT = 'merchant_agreement',
  VERIFIES = 'verifies',
  RECOVERS = 'recovers',
  BOUND_TO = 'bound_to'
}

export enum RelationRuntime {
  FUNDING_SOURCE = 'funding_source',
  MERCHANT_AGREEMENT = 'merchant_agreement'
}

export enum Capability {
  PAYMENT = 'payment',
  ACCESS = 'access',
  RECOVERY = 'recovery',
  IDENTITY = 'identity'
}

export enum CapabilityRuntime {
  PAYMENT = 'payment'
}

export enum Criticality {
  REQUIRED = 'required',
  UNKNOWN = 'unknown'
}

export enum DependencyState {
  ACTIVE = 'active',
  RETIRED = 'retired'
}

export enum DependencyOrigin {
  MANUAL = 'manual',
  PROPOSAL = 'proposal'
}

export enum GroupMode {
  ANY = 'ANY',
  ALL = 'ALL'
}

export enum GroupState {
  ACTIVE = 'active',
  RETIRED = 'retired'
}

export enum ProposalDecision {
  PENDING = 'pending',
  ACCEPTED = 'accepted',
  REJECTED = 'rejected'
}

export enum VerificationBasisType {
  USER_CONFIRMED = 'user_confirmed',
  AUTHORITATIVE_SOURCE = 'authoritative_source'
}

export enum SourceKind {
  STATEMENT_FILE = 'statement_file',
  PLATFORM_EXPORT = 'platform_export',
  OPEN_BANKING = 'open_banking',
  MANUAL = 'manual',
  DISCOVERY = 'discovery'
}

export enum CoverageMode {
  EVENT_STREAM = 'event_stream',
  PARTIAL_SNAPSHOT = 'partial_snapshot',
  COMPLETE_SNAPSHOT = 'complete_snapshot',
  USER_SELECTED = 'user_selected'
}

export enum SourceInstanceState {
  ACTIVE = 'active',
  RETIRED = 'retired'
}

export enum ObservationDirection {
  IN = 'in',
  OUT = 'out',
  NEUTRAL = 'neutral'
}

export enum EvidenceKind {
  TRANSACTION_STREAM = 'transaction_stream'
}

export enum ProposalType {
  RECURRING_PAYMENT_ROUTE = 'recurring_payment_route'
}

export enum ProposalSource {
  STATEMENT = 'statement'
}

export enum ImpactLevel {
  MUST_CHANGE = 'must_change',
  BACKUP_PATH = 'backup_path',
  DEGRADED = 'degraded',
  NEEDS_REVIEW = 'needs_review',
  UNAFFECTED = 'unaffected',
  TARGET_OPERATION = 'target_operation'
}

export enum ImpactTargetStatus {
  MUST_CHANGE = 'must_change',
  BACKUP_PATH = 'backup_path',
  DEGRADED = 'degraded',
  NEEDS_REVIEW = 'needs_review',
  UNAFFECTED = 'unaffected'
}

export enum ImpactReasonCode {
  REQUIRED_EDGE_NO_ALTERNATIVE = 'required_edge_no_alternative',
  CONFIRMED_GROUP_FAILED = 'confirmed_group_failed',
  CONFIRMED_GROUP_COVERED = 'confirmed_group_covered',
  UNCONFIRMED_ALTERNATIVE_EXISTS = 'unconfirmed_alternative_exists',
  CRITICALITY_UNKNOWN = 'criticality_unknown',
  PROPOSAL_ONLY = 'proposal_only',
  UPSTREAM_UNCERTAIN = 'upstream_uncertain'
}

export enum ChangePlanWorkflowState {
  DRAFT = 'draft',
  ANALYZED = 'analyzed',
  REVIEW_REQUIRED = 'review_required',
  READY = 'ready',
  IN_PROGRESS = 'in_progress',
  VERIFYING = 'verifying',
  COMPLETED = 'completed',
  CANCELLED = 'cancelled'
}

export enum PlanEffectiveStatus {
  DRAFT = 'draft',
  ANALYZED = 'analyzed',
  REVIEW_REQUIRED = 'review_required',
  READY = 'ready',
  IN_PROGRESS = 'in_progress',
  VERIFYING = 'verifying',
  COMPLETED = 'completed',
  CANCELLED = 'cancelled',
  NEEDS_REVALIDATION = 'needs_revalidation'
}

export enum PlanActionPhase {
  PREPARE = 'prepare',
  CHANGE = 'change',
  VERIFY = 'verify'
}

export enum ActionVerificationMethod {
  MANUAL_CONFIRMATION = 'manual_confirmation',
  FUTURE_OBSERVATION = 'future_observation',
  AUTHORITATIVE_SOURCE = 'authoritative_source'
}

export enum ActionVerificationStatus {
  NOT_REQUIRED = 'not_required',
  PENDING = 'pending',
  EVIDENCE_SUGGESTED = 'evidence_suggested',
  VERIFIED = 'verified',
  FAILED = 'failed'
}

export enum PlanReadiness {
  BLOCKED = 'blocked',
  REVIEW_REQUIRED = 'review_required',
  READY_WITH_KNOWN_SCOPE = 'ready_with_known_scope'
}

export enum CoverageLevel {
  UNKNOWN = 'unknown',
  LIMITED = 'limited',
  PARTIAL = 'partial',
  WELL_EVIDENCED = 'well_evidenced'
}

export enum DriftKind {
  POSSIBLE_REPLACEMENT = 'possible_replacement',
  POSSIBLE_ADDITIONAL_PATH = 'possible_additional_path',
  RELATION_REAPPEARED = 'relation_reappeared'
}

export enum DriftStatus {
  OPEN = 'open',
  CONFIRMED_CHANGE = 'confirmed_change',
  DISMISSED = 'dismissed',
  SUPERSEDED = 'superseded'
}

export enum DriftIgnoreReason {
  ALREADY_CONFIRMED = 'already_confirmed',
  BELOW_THRESHOLD = 'below_threshold'
}

export enum CandidateKind {
  PAYMENT_INSTRUMENT = 'payment_instrument',
  SERVICE = 'service'
}

export enum CandidateStatus {
  PENDING = 'pending',
  ACCEPTED = 'accepted',
  DISMISSED = 'dismissed',
  SUPERSEDED = 'superseded'
}

export enum TimelineBucket {
  ATTENTION = 'attention',
  OVERDUE = 'overdue',
  TODAY = 'today',
  V7D = '7d',
  V30D = '30d',
  V90D = '90d',
  LATER = 'later'
}

export enum TimelineItemKind {
  NEEDS_ATTENTION = 'needs_attention',
  UPCOMING_CHANGE = 'upcoming_change',
  VERIFICATION_PENDING = 'verification_pending',
  FRESHNESS_REVIEW = 'freshness_review',
  DRIFT_REVIEW = 'drift_review',
  EXPIRATION = 'expiration'
}

export enum TimelineSourceType {
  CHANGE_PLAN = 'change_plan',
  REALITY_DRIFT = 'reality_drift',
  ACTION_VERIFICATION = 'action_verification',
  NODE_EXPIRY = 'node_expiry',
  SOURCE_FRESHNESS = 'source_freshness'
}

export enum ScenarioCategory {
  PAYMENT = 'payment',
  IDENTITY = 'identity',
  DEVICE = 'device',
  WORK = 'work',
  INTERNATIONAL = 'international',
  DIGITAL_ASSETS = 'digital_assets'
}

export enum ScenarioAvailability {
  ACTIVE = 'active',
  PLANNED = 'planned'
}

export enum AmountSignMode {
  SIGNED = 'signed',
  DEBIT_CREDIT = 'debit_credit',
  OUTWARD_POSITIVE = 'outward_positive'
}

export enum CsvDelimiter {
  U2C = ',',
  U3B = ';',
  U09 = '	',
  U7C = '|'
}

export enum SourceEncoding {
  UTF_8 = 'utf-8',
  GB18030 = 'gb18030'
}

export enum ErrorCode {
  INVALID_JSON = 'invalid_json',
  INVALID_STRUCTURE = 'invalid_structure',
  BOUNDS_EXCEEDED = 'bounds_exceeded',
  KDF_FAILED = 'kdf_failed',
  CRYPTO_AUTH_FAILED = 'crypto_auth_failed',
  UNSUPPORTED_FORMAT_VERSION = 'unsupported_format_version',
  UNSUPPORTED_SCHEMA = 'unsupported_schema',
  MIGRATION_FAILED = 'migration_failed',
  PAYLOAD_INVALID = 'payload_invalid',
  PAYLOAD_KIND_MISMATCH = 'payload_kind_mismatch',
  PAYLOAD_VERSION_UNSUPPORTED = 'payload_version_unsupported',
  SOURCE_PARSE_FAILED = 'source_parse_failed',
  MAPPING_MISSING_COLUMN = 'mapping_missing_column',
  VALIDATION_FAILED = 'validation_failed',
  RELATION_NOT_REGISTERED = 'relation_not_registered',
  RELATION_KIND_VIOLATION = 'relation_kind_violation',
  CAPABILITY_MISMATCH = 'capability_mismatch',
  GROUP_MODE_NOT_ALLOWED = 'group_mode_not_allowed',
  ILLEGAL_STATE_TRANSITION = 'illegal_state_transition',
  ACTION_FROZEN = 'action_frozen',
  ENTITY_NOT_FOUND = 'entity_not_found',
  SCENARIO_NOT_EXECUTABLE = 'scenario_not_executable',
  SCENARIO_MISSING_INPUT = 'scenario_missing_input',
  DB_LOCKED = 'db_locked',
  DB_OPEN_FAILED = 'db_open_failed',
  AUTH_CANCELLED = 'auth_cancelled',
  AUTH_FAILED = 'auth_failed',
  AUTH_UNAVAILABLE = 'auth_unavailable',
  KEY_UNAVAILABLE = 'key_unavailable',
  CANCELLED_BY_USER = 'cancelled_by_user'
}

export class DepmapContainerV1 {
  static readonly FORMAT: string = 'depmap'
  static readonly FORMAT_VERSION: number = 1
  static readonly KDF_ALGORITHM: string = 'argon2id'
  static readonly KDF_VERSION: number = 19
  static readonly SALT_BYTES: number = 16
  static readonly MEMORY_KIB: number = 65536
  static readonly ITERATIONS: number = 3
  static readonly PARALLELISM: number = 1
  static readonly KEY_BYTES: number = 32
  static readonly CIPHER_ALGORITHM: string = 'AES-256-GCM'
  static readonly NONCE_BYTES: number = 12
  static readonly TAG_BYTES: number = 16
  static readonly MEMORY_KIB_MIN: number = 16384
  static readonly MEMORY_KIB_MAX: number = 262144
  static readonly ITERATIONS_MIN: number = 1
  static readonly ITERATIONS_MAX: number = 10
  static readonly PARALLELISM_MIN: number = 1
  static readonly PARALLELISM_MAX: number = 4
  static readonly CIPHERTEXT_MAX_BYTES: number = 67108864
}
