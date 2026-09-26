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

export function nodeKindFromWire(value: string): NodeKind | undefined {
  if (value === NodeKind.IDENTITY_ANCHOR) {
    return NodeKind.IDENTITY_ANCHOR;
  }
  if (value === NodeKind.PAYMENT_INSTRUMENT) {
    return NodeKind.PAYMENT_INSTRUMENT;
  }
  if (value === NodeKind.ACCOUNT) {
    return NodeKind.ACCOUNT;
  }
  if (value === NodeKind.SERVICE) {
    return NodeKind.SERVICE;
  }
  if (value === NodeKind.MEMBERSHIP) {
    return NodeKind.MEMBERSHIP;
  }
  if (value === NodeKind.DEVICE) {
    return NodeKind.DEVICE;
  }
  if (value === NodeKind.CUSTOM) {
    return NodeKind.CUSTOM;
  }
  return undefined;
}

export enum Relation {
  FUNDING_SOURCE = 'funding_source',
  MERCHANT_AGREEMENT = 'merchant_agreement',
  VERIFIES = 'verifies',
  RECOVERS = 'recovers',
  BOUND_TO = 'bound_to',
  AUTHENTICATES = 'authenticates',
  CONTROLS = 'controls'
}

export function relationFromWire(value: string): Relation | undefined {
  if (value === Relation.FUNDING_SOURCE) {
    return Relation.FUNDING_SOURCE;
  }
  if (value === Relation.MERCHANT_AGREEMENT) {
    return Relation.MERCHANT_AGREEMENT;
  }
  if (value === Relation.VERIFIES) {
    return Relation.VERIFIES;
  }
  if (value === Relation.RECOVERS) {
    return Relation.RECOVERS;
  }
  if (value === Relation.BOUND_TO) {
    return Relation.BOUND_TO;
  }
  if (value === Relation.AUTHENTICATES) {
    return Relation.AUTHENTICATES;
  }
  if (value === Relation.CONTROLS) {
    return Relation.CONTROLS;
  }
  return undefined;
}

export enum RelationRuntime {
  FUNDING_SOURCE = 'funding_source',
  MERCHANT_AGREEMENT = 'merchant_agreement',
  RECOVERS = 'recovers',
  AUTHENTICATES = 'authenticates',
  CONTROLS = 'controls'
}

export function relationRuntimeFromWire(value: string): RelationRuntime | undefined {
  if (value === RelationRuntime.FUNDING_SOURCE) {
    return RelationRuntime.FUNDING_SOURCE;
  }
  if (value === RelationRuntime.MERCHANT_AGREEMENT) {
    return RelationRuntime.MERCHANT_AGREEMENT;
  }
  if (value === RelationRuntime.RECOVERS) {
    return RelationRuntime.RECOVERS;
  }
  if (value === RelationRuntime.AUTHENTICATES) {
    return RelationRuntime.AUTHENTICATES;
  }
  if (value === RelationRuntime.CONTROLS) {
    return RelationRuntime.CONTROLS;
  }
  return undefined;
}

export enum Capability {
  PAYMENT = 'payment',
  ACCESS = 'access',
  AUTHENTICATION = 'authentication',
  RECOVERY = 'recovery',
  COMMUNICATION = 'communication',
  IDENTITY = 'identity'
}

export function capabilityFromWire(value: string): Capability | undefined {
  if (value === Capability.PAYMENT) {
    return Capability.PAYMENT;
  }
  if (value === Capability.ACCESS) {
    return Capability.ACCESS;
  }
  if (value === Capability.AUTHENTICATION) {
    return Capability.AUTHENTICATION;
  }
  if (value === Capability.RECOVERY) {
    return Capability.RECOVERY;
  }
  if (value === Capability.COMMUNICATION) {
    return Capability.COMMUNICATION;
  }
  if (value === Capability.IDENTITY) {
    return Capability.IDENTITY;
  }
  return undefined;
}

export enum CapabilityRuntime {
  PAYMENT = 'payment',
  ACCESS = 'access',
  AUTHENTICATION = 'authentication',
  RECOVERY = 'recovery',
  COMMUNICATION = 'communication'
}

export function capabilityRuntimeFromWire(value: string): CapabilityRuntime | undefined {
  if (value === CapabilityRuntime.PAYMENT) {
    return CapabilityRuntime.PAYMENT;
  }
  if (value === CapabilityRuntime.ACCESS) {
    return CapabilityRuntime.ACCESS;
  }
  if (value === CapabilityRuntime.AUTHENTICATION) {
    return CapabilityRuntime.AUTHENTICATION;
  }
  if (value === CapabilityRuntime.RECOVERY) {
    return CapabilityRuntime.RECOVERY;
  }
  if (value === CapabilityRuntime.COMMUNICATION) {
    return CapabilityRuntime.COMMUNICATION;
  }
  return undefined;
}

export enum Criticality {
  REQUIRED = 'required',
  UNKNOWN = 'unknown'
}

export function criticalityFromWire(value: string): Criticality | undefined {
  if (value === Criticality.REQUIRED) {
    return Criticality.REQUIRED;
  }
  if (value === Criticality.UNKNOWN) {
    return Criticality.UNKNOWN;
  }
  return undefined;
}

export enum DependencyState {
  ACTIVE = 'active',
  RETIRED = 'retired'
}

export function dependencyStateFromWire(value: string): DependencyState | undefined {
  if (value === DependencyState.ACTIVE) {
    return DependencyState.ACTIVE;
  }
  if (value === DependencyState.RETIRED) {
    return DependencyState.RETIRED;
  }
  return undefined;
}

export enum DependencyOrigin {
  MANUAL = 'manual',
  PROPOSAL = 'proposal'
}

export function dependencyOriginFromWire(value: string): DependencyOrigin | undefined {
  if (value === DependencyOrigin.MANUAL) {
    return DependencyOrigin.MANUAL;
  }
  if (value === DependencyOrigin.PROPOSAL) {
    return DependencyOrigin.PROPOSAL;
  }
  return undefined;
}

export enum GroupMode {
  ANY = 'ANY',
  ALL = 'ALL'
}

export function groupModeFromWire(value: string): GroupMode | undefined {
  if (value === GroupMode.ANY) {
    return GroupMode.ANY;
  }
  if (value === GroupMode.ALL) {
    return GroupMode.ALL;
  }
  return undefined;
}

export enum GroupState {
  ACTIVE = 'active',
  RETIRED = 'retired'
}

export function groupStateFromWire(value: string): GroupState | undefined {
  if (value === GroupState.ACTIVE) {
    return GroupState.ACTIVE;
  }
  if (value === GroupState.RETIRED) {
    return GroupState.RETIRED;
  }
  return undefined;
}

export enum ProposalDecision {
  PENDING = 'pending',
  ACCEPTED = 'accepted',
  REJECTED = 'rejected'
}

export function proposalDecisionFromWire(value: string): ProposalDecision | undefined {
  if (value === ProposalDecision.PENDING) {
    return ProposalDecision.PENDING;
  }
  if (value === ProposalDecision.ACCEPTED) {
    return ProposalDecision.ACCEPTED;
  }
  if (value === ProposalDecision.REJECTED) {
    return ProposalDecision.REJECTED;
  }
  return undefined;
}

export enum VerificationBasisType {
  USER_CONFIRMED = 'user_confirmed',
  AUTHORITATIVE_SOURCE = 'authoritative_source'
}

export function verificationBasisTypeFromWire(value: string): VerificationBasisType | undefined {
  if (value === VerificationBasisType.USER_CONFIRMED) {
    return VerificationBasisType.USER_CONFIRMED;
  }
  if (value === VerificationBasisType.AUTHORITATIVE_SOURCE) {
    return VerificationBasisType.AUTHORITATIVE_SOURCE;
  }
  return undefined;
}

export enum SourceKind {
  STATEMENT_FILE = 'statement_file',
  PLATFORM_EXPORT = 'platform_export',
  OPEN_BANKING = 'open_banking',
  MANUAL = 'manual',
  DISCOVERY = 'discovery'
}

export function sourceKindFromWire(value: string): SourceKind | undefined {
  if (value === SourceKind.STATEMENT_FILE) {
    return SourceKind.STATEMENT_FILE;
  }
  if (value === SourceKind.PLATFORM_EXPORT) {
    return SourceKind.PLATFORM_EXPORT;
  }
  if (value === SourceKind.OPEN_BANKING) {
    return SourceKind.OPEN_BANKING;
  }
  if (value === SourceKind.MANUAL) {
    return SourceKind.MANUAL;
  }
  if (value === SourceKind.DISCOVERY) {
    return SourceKind.DISCOVERY;
  }
  return undefined;
}

export enum CoverageMode {
  EVENT_STREAM = 'event_stream',
  PARTIAL_SNAPSHOT = 'partial_snapshot',
  COMPLETE_SNAPSHOT = 'complete_snapshot',
  USER_SELECTED = 'user_selected'
}

export function coverageModeFromWire(value: string): CoverageMode | undefined {
  if (value === CoverageMode.EVENT_STREAM) {
    return CoverageMode.EVENT_STREAM;
  }
  if (value === CoverageMode.PARTIAL_SNAPSHOT) {
    return CoverageMode.PARTIAL_SNAPSHOT;
  }
  if (value === CoverageMode.COMPLETE_SNAPSHOT) {
    return CoverageMode.COMPLETE_SNAPSHOT;
  }
  if (value === CoverageMode.USER_SELECTED) {
    return CoverageMode.USER_SELECTED;
  }
  return undefined;
}

export enum SourceInstanceState {
  ACTIVE = 'active',
  RETIRED = 'retired'
}

export function sourceInstanceStateFromWire(value: string): SourceInstanceState | undefined {
  if (value === SourceInstanceState.ACTIVE) {
    return SourceInstanceState.ACTIVE;
  }
  if (value === SourceInstanceState.RETIRED) {
    return SourceInstanceState.RETIRED;
  }
  return undefined;
}

export enum ObservationDirection {
  IN = 'in',
  OUT = 'out',
  NEUTRAL = 'neutral'
}

export function observationDirectionFromWire(value: string): ObservationDirection | undefined {
  if (value === ObservationDirection.IN) {
    return ObservationDirection.IN;
  }
  if (value === ObservationDirection.OUT) {
    return ObservationDirection.OUT;
  }
  if (value === ObservationDirection.NEUTRAL) {
    return ObservationDirection.NEUTRAL;
  }
  return undefined;
}

export enum EvidenceKind {
  TRANSACTION_STREAM = 'transaction_stream'
}

export function evidenceKindFromWire(value: string): EvidenceKind | undefined {
  if (value === EvidenceKind.TRANSACTION_STREAM) {
    return EvidenceKind.TRANSACTION_STREAM;
  }
  return undefined;
}

export enum ProposalType {
  RECURRING_PAYMENT_ROUTE = 'recurring_payment_route'
}

export function proposalTypeFromWire(value: string): ProposalType | undefined {
  if (value === ProposalType.RECURRING_PAYMENT_ROUTE) {
    return ProposalType.RECURRING_PAYMENT_ROUTE;
  }
  return undefined;
}

export enum ProposalSource {
  STATEMENT = 'statement'
}

export function proposalSourceFromWire(value: string): ProposalSource | undefined {
  if (value === ProposalSource.STATEMENT) {
    return ProposalSource.STATEMENT;
  }
  return undefined;
}

export enum ImpactLevel {
  MUST_CHANGE = 'must_change',
  BACKUP_PATH = 'backup_path',
  DEGRADED = 'degraded',
  NEEDS_REVIEW = 'needs_review',
  UNAFFECTED = 'unaffected',
  TARGET_OPERATION = 'target_operation'
}

export function impactLevelFromWire(value: string): ImpactLevel | undefined {
  if (value === ImpactLevel.MUST_CHANGE) {
    return ImpactLevel.MUST_CHANGE;
  }
  if (value === ImpactLevel.BACKUP_PATH) {
    return ImpactLevel.BACKUP_PATH;
  }
  if (value === ImpactLevel.DEGRADED) {
    return ImpactLevel.DEGRADED;
  }
  if (value === ImpactLevel.NEEDS_REVIEW) {
    return ImpactLevel.NEEDS_REVIEW;
  }
  if (value === ImpactLevel.UNAFFECTED) {
    return ImpactLevel.UNAFFECTED;
  }
  if (value === ImpactLevel.TARGET_OPERATION) {
    return ImpactLevel.TARGET_OPERATION;
  }
  return undefined;
}

export enum ImpactTargetStatus {
  MUST_CHANGE = 'must_change',
  BACKUP_PATH = 'backup_path',
  DEGRADED = 'degraded',
  NEEDS_REVIEW = 'needs_review',
  UNAFFECTED = 'unaffected'
}

export function impactTargetStatusFromWire(value: string): ImpactTargetStatus | undefined {
  if (value === ImpactTargetStatus.MUST_CHANGE) {
    return ImpactTargetStatus.MUST_CHANGE;
  }
  if (value === ImpactTargetStatus.BACKUP_PATH) {
    return ImpactTargetStatus.BACKUP_PATH;
  }
  if (value === ImpactTargetStatus.DEGRADED) {
    return ImpactTargetStatus.DEGRADED;
  }
  if (value === ImpactTargetStatus.NEEDS_REVIEW) {
    return ImpactTargetStatus.NEEDS_REVIEW;
  }
  if (value === ImpactTargetStatus.UNAFFECTED) {
    return ImpactTargetStatus.UNAFFECTED;
  }
  return undefined;
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

export function impactReasonCodeFromWire(value: string): ImpactReasonCode | undefined {
  if (value === ImpactReasonCode.REQUIRED_EDGE_NO_ALTERNATIVE) {
    return ImpactReasonCode.REQUIRED_EDGE_NO_ALTERNATIVE;
  }
  if (value === ImpactReasonCode.CONFIRMED_GROUP_FAILED) {
    return ImpactReasonCode.CONFIRMED_GROUP_FAILED;
  }
  if (value === ImpactReasonCode.CONFIRMED_GROUP_COVERED) {
    return ImpactReasonCode.CONFIRMED_GROUP_COVERED;
  }
  if (value === ImpactReasonCode.UNCONFIRMED_ALTERNATIVE_EXISTS) {
    return ImpactReasonCode.UNCONFIRMED_ALTERNATIVE_EXISTS;
  }
  if (value === ImpactReasonCode.CRITICALITY_UNKNOWN) {
    return ImpactReasonCode.CRITICALITY_UNKNOWN;
  }
  if (value === ImpactReasonCode.PROPOSAL_ONLY) {
    return ImpactReasonCode.PROPOSAL_ONLY;
  }
  if (value === ImpactReasonCode.UPSTREAM_UNCERTAIN) {
    return ImpactReasonCode.UPSTREAM_UNCERTAIN;
  }
  return undefined;
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

export function changePlanWorkflowStateFromWire(value: string): ChangePlanWorkflowState | undefined {
  if (value === ChangePlanWorkflowState.DRAFT) {
    return ChangePlanWorkflowState.DRAFT;
  }
  if (value === ChangePlanWorkflowState.ANALYZED) {
    return ChangePlanWorkflowState.ANALYZED;
  }
  if (value === ChangePlanWorkflowState.REVIEW_REQUIRED) {
    return ChangePlanWorkflowState.REVIEW_REQUIRED;
  }
  if (value === ChangePlanWorkflowState.READY) {
    return ChangePlanWorkflowState.READY;
  }
  if (value === ChangePlanWorkflowState.IN_PROGRESS) {
    return ChangePlanWorkflowState.IN_PROGRESS;
  }
  if (value === ChangePlanWorkflowState.VERIFYING) {
    return ChangePlanWorkflowState.VERIFYING;
  }
  if (value === ChangePlanWorkflowState.COMPLETED) {
    return ChangePlanWorkflowState.COMPLETED;
  }
  if (value === ChangePlanWorkflowState.CANCELLED) {
    return ChangePlanWorkflowState.CANCELLED;
  }
  return undefined;
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

export function planEffectiveStatusFromWire(value: string): PlanEffectiveStatus | undefined {
  if (value === PlanEffectiveStatus.DRAFT) {
    return PlanEffectiveStatus.DRAFT;
  }
  if (value === PlanEffectiveStatus.ANALYZED) {
    return PlanEffectiveStatus.ANALYZED;
  }
  if (value === PlanEffectiveStatus.REVIEW_REQUIRED) {
    return PlanEffectiveStatus.REVIEW_REQUIRED;
  }
  if (value === PlanEffectiveStatus.READY) {
    return PlanEffectiveStatus.READY;
  }
  if (value === PlanEffectiveStatus.IN_PROGRESS) {
    return PlanEffectiveStatus.IN_PROGRESS;
  }
  if (value === PlanEffectiveStatus.VERIFYING) {
    return PlanEffectiveStatus.VERIFYING;
  }
  if (value === PlanEffectiveStatus.COMPLETED) {
    return PlanEffectiveStatus.COMPLETED;
  }
  if (value === PlanEffectiveStatus.CANCELLED) {
    return PlanEffectiveStatus.CANCELLED;
  }
  if (value === PlanEffectiveStatus.NEEDS_REVALIDATION) {
    return PlanEffectiveStatus.NEEDS_REVALIDATION;
  }
  return undefined;
}

export enum PlanActionPhase {
  PREPARE = 'prepare',
  CHANGE = 'change',
  VERIFY = 'verify'
}

export function planActionPhaseFromWire(value: string): PlanActionPhase | undefined {
  if (value === PlanActionPhase.PREPARE) {
    return PlanActionPhase.PREPARE;
  }
  if (value === PlanActionPhase.CHANGE) {
    return PlanActionPhase.CHANGE;
  }
  if (value === PlanActionPhase.VERIFY) {
    return PlanActionPhase.VERIFY;
  }
  return undefined;
}

export enum ActionVerificationMethod {
  MANUAL_CONFIRMATION = 'manual_confirmation',
  FUTURE_OBSERVATION = 'future_observation',
  AUTHORITATIVE_SOURCE = 'authoritative_source'
}

export function actionVerificationMethodFromWire(value: string): ActionVerificationMethod | undefined {
  if (value === ActionVerificationMethod.MANUAL_CONFIRMATION) {
    return ActionVerificationMethod.MANUAL_CONFIRMATION;
  }
  if (value === ActionVerificationMethod.FUTURE_OBSERVATION) {
    return ActionVerificationMethod.FUTURE_OBSERVATION;
  }
  if (value === ActionVerificationMethod.AUTHORITATIVE_SOURCE) {
    return ActionVerificationMethod.AUTHORITATIVE_SOURCE;
  }
  return undefined;
}

export enum ActionVerificationStatus {
  NOT_REQUIRED = 'not_required',
  PENDING = 'pending',
  EVIDENCE_SUGGESTED = 'evidence_suggested',
  VERIFIED = 'verified',
  FAILED = 'failed'
}

export function actionVerificationStatusFromWire(value: string): ActionVerificationStatus | undefined {
  if (value === ActionVerificationStatus.NOT_REQUIRED) {
    return ActionVerificationStatus.NOT_REQUIRED;
  }
  if (value === ActionVerificationStatus.PENDING) {
    return ActionVerificationStatus.PENDING;
  }
  if (value === ActionVerificationStatus.EVIDENCE_SUGGESTED) {
    return ActionVerificationStatus.EVIDENCE_SUGGESTED;
  }
  if (value === ActionVerificationStatus.VERIFIED) {
    return ActionVerificationStatus.VERIFIED;
  }
  if (value === ActionVerificationStatus.FAILED) {
    return ActionVerificationStatus.FAILED;
  }
  return undefined;
}

export enum PlanReadiness {
  BLOCKED = 'blocked',
  REVIEW_REQUIRED = 'review_required',
  READY_WITH_KNOWN_SCOPE = 'ready_with_known_scope'
}

export function planReadinessFromWire(value: string): PlanReadiness | undefined {
  if (value === PlanReadiness.BLOCKED) {
    return PlanReadiness.BLOCKED;
  }
  if (value === PlanReadiness.REVIEW_REQUIRED) {
    return PlanReadiness.REVIEW_REQUIRED;
  }
  if (value === PlanReadiness.READY_WITH_KNOWN_SCOPE) {
    return PlanReadiness.READY_WITH_KNOWN_SCOPE;
  }
  return undefined;
}

export enum CoverageLevel {
  UNKNOWN = 'unknown',
  LIMITED = 'limited',
  PARTIAL = 'partial',
  WELL_EVIDENCED = 'well_evidenced'
}

export function coverageLevelFromWire(value: string): CoverageLevel | undefined {
  if (value === CoverageLevel.UNKNOWN) {
    return CoverageLevel.UNKNOWN;
  }
  if (value === CoverageLevel.LIMITED) {
    return CoverageLevel.LIMITED;
  }
  if (value === CoverageLevel.PARTIAL) {
    return CoverageLevel.PARTIAL;
  }
  if (value === CoverageLevel.WELL_EVIDENCED) {
    return CoverageLevel.WELL_EVIDENCED;
  }
  return undefined;
}

export enum DriftKind {
  POSSIBLE_REPLACEMENT = 'possible_replacement',
  POSSIBLE_ADDITIONAL_PATH = 'possible_additional_path',
  RELATION_REAPPEARED = 'relation_reappeared'
}

export function driftKindFromWire(value: string): DriftKind | undefined {
  if (value === DriftKind.POSSIBLE_REPLACEMENT) {
    return DriftKind.POSSIBLE_REPLACEMENT;
  }
  if (value === DriftKind.POSSIBLE_ADDITIONAL_PATH) {
    return DriftKind.POSSIBLE_ADDITIONAL_PATH;
  }
  if (value === DriftKind.RELATION_REAPPEARED) {
    return DriftKind.RELATION_REAPPEARED;
  }
  return undefined;
}

export enum DriftStatus {
  OPEN = 'open',
  CONFIRMED_CHANGE = 'confirmed_change',
  DISMISSED = 'dismissed',
  SUPERSEDED = 'superseded'
}

export function driftStatusFromWire(value: string): DriftStatus | undefined {
  if (value === DriftStatus.OPEN) {
    return DriftStatus.OPEN;
  }
  if (value === DriftStatus.CONFIRMED_CHANGE) {
    return DriftStatus.CONFIRMED_CHANGE;
  }
  if (value === DriftStatus.DISMISSED) {
    return DriftStatus.DISMISSED;
  }
  if (value === DriftStatus.SUPERSEDED) {
    return DriftStatus.SUPERSEDED;
  }
  return undefined;
}

export enum DriftIgnoreReason {
  ALREADY_CONFIRMED = 'already_confirmed',
  BELOW_THRESHOLD = 'below_threshold'
}

export function driftIgnoreReasonFromWire(value: string): DriftIgnoreReason | undefined {
  if (value === DriftIgnoreReason.ALREADY_CONFIRMED) {
    return DriftIgnoreReason.ALREADY_CONFIRMED;
  }
  if (value === DriftIgnoreReason.BELOW_THRESHOLD) {
    return DriftIgnoreReason.BELOW_THRESHOLD;
  }
  return undefined;
}

export enum CandidateKind {
  PAYMENT_INSTRUMENT = 'payment_instrument',
  SERVICE = 'service'
}

export function candidateKindFromWire(value: string): CandidateKind | undefined {
  if (value === CandidateKind.PAYMENT_INSTRUMENT) {
    return CandidateKind.PAYMENT_INSTRUMENT;
  }
  if (value === CandidateKind.SERVICE) {
    return CandidateKind.SERVICE;
  }
  return undefined;
}

export enum CandidateStatus {
  PENDING = 'pending',
  ACCEPTED = 'accepted',
  DISMISSED = 'dismissed',
  SUPERSEDED = 'superseded'
}

export function candidateStatusFromWire(value: string): CandidateStatus | undefined {
  if (value === CandidateStatus.PENDING) {
    return CandidateStatus.PENDING;
  }
  if (value === CandidateStatus.ACCEPTED) {
    return CandidateStatus.ACCEPTED;
  }
  if (value === CandidateStatus.DISMISSED) {
    return CandidateStatus.DISMISSED;
  }
  if (value === CandidateStatus.SUPERSEDED) {
    return CandidateStatus.SUPERSEDED;
  }
  return undefined;
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

export function timelineBucketFromWire(value: string): TimelineBucket | undefined {
  if (value === TimelineBucket.ATTENTION) {
    return TimelineBucket.ATTENTION;
  }
  if (value === TimelineBucket.OVERDUE) {
    return TimelineBucket.OVERDUE;
  }
  if (value === TimelineBucket.TODAY) {
    return TimelineBucket.TODAY;
  }
  if (value === TimelineBucket.V7D) {
    return TimelineBucket.V7D;
  }
  if (value === TimelineBucket.V30D) {
    return TimelineBucket.V30D;
  }
  if (value === TimelineBucket.V90D) {
    return TimelineBucket.V90D;
  }
  if (value === TimelineBucket.LATER) {
    return TimelineBucket.LATER;
  }
  return undefined;
}

export enum TimelineItemKind {
  NEEDS_ATTENTION = 'needs_attention',
  UPCOMING_CHANGE = 'upcoming_change',
  VERIFICATION_PENDING = 'verification_pending',
  FRESHNESS_REVIEW = 'freshness_review',
  DRIFT_REVIEW = 'drift_review',
  EXPIRATION = 'expiration'
}

export function timelineItemKindFromWire(value: string): TimelineItemKind | undefined {
  if (value === TimelineItemKind.NEEDS_ATTENTION) {
    return TimelineItemKind.NEEDS_ATTENTION;
  }
  if (value === TimelineItemKind.UPCOMING_CHANGE) {
    return TimelineItemKind.UPCOMING_CHANGE;
  }
  if (value === TimelineItemKind.VERIFICATION_PENDING) {
    return TimelineItemKind.VERIFICATION_PENDING;
  }
  if (value === TimelineItemKind.FRESHNESS_REVIEW) {
    return TimelineItemKind.FRESHNESS_REVIEW;
  }
  if (value === TimelineItemKind.DRIFT_REVIEW) {
    return TimelineItemKind.DRIFT_REVIEW;
  }
  if (value === TimelineItemKind.EXPIRATION) {
    return TimelineItemKind.EXPIRATION;
  }
  return undefined;
}

export enum TimelineSourceType {
  CHANGE_PLAN = 'change_plan',
  REALITY_DRIFT = 'reality_drift',
  ACTION_VERIFICATION = 'action_verification',
  NODE_EXPIRY = 'node_expiry',
  SOURCE_FRESHNESS = 'source_freshness'
}

export function timelineSourceTypeFromWire(value: string): TimelineSourceType | undefined {
  if (value === TimelineSourceType.CHANGE_PLAN) {
    return TimelineSourceType.CHANGE_PLAN;
  }
  if (value === TimelineSourceType.REALITY_DRIFT) {
    return TimelineSourceType.REALITY_DRIFT;
  }
  if (value === TimelineSourceType.ACTION_VERIFICATION) {
    return TimelineSourceType.ACTION_VERIFICATION;
  }
  if (value === TimelineSourceType.NODE_EXPIRY) {
    return TimelineSourceType.NODE_EXPIRY;
  }
  if (value === TimelineSourceType.SOURCE_FRESHNESS) {
    return TimelineSourceType.SOURCE_FRESHNESS;
  }
  return undefined;
}

export enum ScenarioCategory {
  PAYMENT = 'payment',
  IDENTITY = 'identity',
  DEVICE = 'device',
  WORK = 'work',
  INTERNATIONAL = 'international',
  DIGITAL_ASSETS = 'digital_assets'
}

export function scenarioCategoryFromWire(value: string): ScenarioCategory | undefined {
  if (value === ScenarioCategory.PAYMENT) {
    return ScenarioCategory.PAYMENT;
  }
  if (value === ScenarioCategory.IDENTITY) {
    return ScenarioCategory.IDENTITY;
  }
  if (value === ScenarioCategory.DEVICE) {
    return ScenarioCategory.DEVICE;
  }
  if (value === ScenarioCategory.WORK) {
    return ScenarioCategory.WORK;
  }
  if (value === ScenarioCategory.INTERNATIONAL) {
    return ScenarioCategory.INTERNATIONAL;
  }
  if (value === ScenarioCategory.DIGITAL_ASSETS) {
    return ScenarioCategory.DIGITAL_ASSETS;
  }
  return undefined;
}

export enum ScenarioAvailability {
  ACTIVE = 'active',
  PLANNED = 'planned'
}

export function scenarioAvailabilityFromWire(value: string): ScenarioAvailability | undefined {
  if (value === ScenarioAvailability.ACTIVE) {
    return ScenarioAvailability.ACTIVE;
  }
  if (value === ScenarioAvailability.PLANNED) {
    return ScenarioAvailability.PLANNED;
  }
  return undefined;
}

export enum AmountSignMode {
  SIGNED = 'signed',
  DEBIT_CREDIT = 'debit_credit',
  OUTWARD_POSITIVE = 'outward_positive'
}

export function amountSignModeFromWire(value: string): AmountSignMode | undefined {
  if (value === AmountSignMode.SIGNED) {
    return AmountSignMode.SIGNED;
  }
  if (value === AmountSignMode.DEBIT_CREDIT) {
    return AmountSignMode.DEBIT_CREDIT;
  }
  if (value === AmountSignMode.OUTWARD_POSITIVE) {
    return AmountSignMode.OUTWARD_POSITIVE;
  }
  return undefined;
}

export enum CsvDelimiter {
  U2C = ',',
  U3B = ';',
  U09 = '	',
  U7C = '|'
}

export function csvDelimiterFromWire(value: string): CsvDelimiter | undefined {
  if (value === CsvDelimiter.U2C) {
    return CsvDelimiter.U2C;
  }
  if (value === CsvDelimiter.U3B) {
    return CsvDelimiter.U3B;
  }
  if (value === CsvDelimiter.U09) {
    return CsvDelimiter.U09;
  }
  if (value === CsvDelimiter.U7C) {
    return CsvDelimiter.U7C;
  }
  return undefined;
}

export enum SourceEncoding {
  UTF_8 = 'utf-8',
  GB18030 = 'gb18030'
}

export function sourceEncodingFromWire(value: string): SourceEncoding | undefined {
  if (value === SourceEncoding.UTF_8) {
    return SourceEncoding.UTF_8;
  }
  if (value === SourceEncoding.GB18030) {
    return SourceEncoding.GB18030;
  }
  return undefined;
}

export enum FailureDomainKind {
  DEVICE = 'DEVICE',
  PHONE_NUMBER = 'PHONE_NUMBER',
  ACCOUNT = 'ACCOUNT',
  PROVIDER = 'PROVIDER',
  PERSON = 'PERSON',
  REGION = 'REGION',
  PHYSICAL_LOCATION = 'PHYSICAL_LOCATION',
  NETWORK = 'NETWORK'
}

export function failureDomainKindFromWire(value: string): FailureDomainKind | undefined {
  if (value === FailureDomainKind.DEVICE) {
    return FailureDomainKind.DEVICE;
  }
  if (value === FailureDomainKind.PHONE_NUMBER) {
    return FailureDomainKind.PHONE_NUMBER;
  }
  if (value === FailureDomainKind.ACCOUNT) {
    return FailureDomainKind.ACCOUNT;
  }
  if (value === FailureDomainKind.PROVIDER) {
    return FailureDomainKind.PROVIDER;
  }
  if (value === FailureDomainKind.PERSON) {
    return FailureDomainKind.PERSON;
  }
  if (value === FailureDomainKind.REGION) {
    return FailureDomainKind.REGION;
  }
  if (value === FailureDomainKind.PHYSICAL_LOCATION) {
    return FailureDomainKind.PHYSICAL_LOCATION;
  }
  if (value === FailureDomainKind.NETWORK) {
    return FailureDomainKind.NETWORK;
  }
  return undefined;
}

export enum FailureDomainKindRuntime {
  DEVICE = 'DEVICE',
  PHONE_NUMBER = 'PHONE_NUMBER',
  ACCOUNT = 'ACCOUNT',
  PROVIDER = 'PROVIDER'
}

export function failureDomainKindRuntimeFromWire(value: string): FailureDomainKindRuntime | undefined {
  if (value === FailureDomainKindRuntime.DEVICE) {
    return FailureDomainKindRuntime.DEVICE;
  }
  if (value === FailureDomainKindRuntime.PHONE_NUMBER) {
    return FailureDomainKindRuntime.PHONE_NUMBER;
  }
  if (value === FailureDomainKindRuntime.ACCOUNT) {
    return FailureDomainKindRuntime.ACCOUNT;
  }
  if (value === FailureDomainKindRuntime.PROVIDER) {
    return FailureDomainKindRuntime.PROVIDER;
  }
  return undefined;
}

export enum FailureDomainStatus {
  CONFIRMED = 'confirmed',
  NEEDS_REVIEW = 'needs_review'
}

export function failureDomainStatusFromWire(value: string): FailureDomainStatus | undefined {
  if (value === FailureDomainStatus.CONFIRMED) {
    return FailureDomainStatus.CONFIRMED;
  }
  if (value === FailureDomainStatus.NEEDS_REVIEW) {
    return FailureDomainStatus.NEEDS_REVIEW;
  }
  return undefined;
}

export enum InfrastructureFindingType {
  SINGLE_POINT_OF_FAILURE = 'SINGLE_POINT_OF_FAILURE',
  SHARED_FAILURE_DOMAIN = 'SHARED_FAILURE_DOMAIN',
  RECOVERY_CYCLE = 'RECOVERY_CYCLE',
  UNCONFIRMED_FALLBACK = 'UNCONFIRMED_FALLBACK',
  STALE_RECOVERY_INFORMATION = 'STALE_RECOVERY_INFORMATION',
  UNKNOWN_CRITICAL_PATH = 'UNKNOWN_CRITICAL_PATH',
  PENDING_VERIFICATION = 'PENDING_VERIFICATION'
}

export function infrastructureFindingTypeFromWire(value: string): InfrastructureFindingType | undefined {
  if (value === InfrastructureFindingType.SINGLE_POINT_OF_FAILURE) {
    return InfrastructureFindingType.SINGLE_POINT_OF_FAILURE;
  }
  if (value === InfrastructureFindingType.SHARED_FAILURE_DOMAIN) {
    return InfrastructureFindingType.SHARED_FAILURE_DOMAIN;
  }
  if (value === InfrastructureFindingType.RECOVERY_CYCLE) {
    return InfrastructureFindingType.RECOVERY_CYCLE;
  }
  if (value === InfrastructureFindingType.UNCONFIRMED_FALLBACK) {
    return InfrastructureFindingType.UNCONFIRMED_FALLBACK;
  }
  if (value === InfrastructureFindingType.STALE_RECOVERY_INFORMATION) {
    return InfrastructureFindingType.STALE_RECOVERY_INFORMATION;
  }
  if (value === InfrastructureFindingType.UNKNOWN_CRITICAL_PATH) {
    return InfrastructureFindingType.UNKNOWN_CRITICAL_PATH;
  }
  if (value === InfrastructureFindingType.PENDING_VERIFICATION) {
    return InfrastructureFindingType.PENDING_VERIFICATION;
  }
  return undefined;
}

export enum ChangePrimitive {
  REPLACE = 'REPLACE',
  LOSE = 'LOSE',
  COMPROMISE = 'COMPROMISE',
  MIGRATE = 'MIGRATE',
  PORT = 'PORT',
  SUSPEND = 'SUSPEND'
}

export function changePrimitiveFromWire(value: string): ChangePrimitive | undefined {
  if (value === ChangePrimitive.REPLACE) {
    return ChangePrimitive.REPLACE;
  }
  if (value === ChangePrimitive.LOSE) {
    return ChangePrimitive.LOSE;
  }
  if (value === ChangePrimitive.COMPROMISE) {
    return ChangePrimitive.COMPROMISE;
  }
  if (value === ChangePrimitive.MIGRATE) {
    return ChangePrimitive.MIGRATE;
  }
  if (value === ChangePrimitive.PORT) {
    return ChangePrimitive.PORT;
  }
  if (value === ChangePrimitive.SUSPEND) {
    return ChangePrimitive.SUSPEND;
  }
  return undefined;
}

export enum ChangePrimitiveRuntime {
  REPLACE = 'REPLACE'
}

export function changePrimitiveRuntimeFromWire(value: string): ChangePrimitiveRuntime | undefined {
  if (value === ChangePrimitiveRuntime.REPLACE) {
    return ChangePrimitiveRuntime.REPLACE;
  }
  return undefined;
}

export enum RecoveryCycleStatus {
  CONFIRMED_CYCLE = 'confirmed_cycle',
  POTENTIAL_CYCLE = 'potential_cycle',
  NO_CYCLE = 'no_cycle'
}

export function recoveryCycleStatusFromWire(value: string): RecoveryCycleStatus | undefined {
  if (value === RecoveryCycleStatus.CONFIRMED_CYCLE) {
    return RecoveryCycleStatus.CONFIRMED_CYCLE;
  }
  if (value === RecoveryCycleStatus.POTENTIAL_CYCLE) {
    return RecoveryCycleStatus.POTENTIAL_CYCLE;
  }
  if (value === RecoveryCycleStatus.NO_CYCLE) {
    return RecoveryCycleStatus.NO_CYCLE;
  }
  return undefined;
}

export enum PathIndependenceStatus {
  VERIFIED_INDEPENDENT = 'verified_independent',
  SHARED_FAILURE_DOMAIN = 'shared_failure_domain',
  NEEDS_REVIEW = 'needs_review'
}

export function pathIndependenceStatusFromWire(value: string): PathIndependenceStatus | undefined {
  if (value === PathIndependenceStatus.VERIFIED_INDEPENDENT) {
    return PathIndependenceStatus.VERIFIED_INDEPENDENT;
  }
  if (value === PathIndependenceStatus.SHARED_FAILURE_DOMAIN) {
    return PathIndependenceStatus.SHARED_FAILURE_DOMAIN;
  }
  if (value === PathIndependenceStatus.NEEDS_REVIEW) {
    return PathIndependenceStatus.NEEDS_REVIEW;
  }
  return undefined;
}

export enum ActionDagViolation {
  CYCLE = 'cycle',
  MISSING_PREREQUISITE = 'missing_prerequisite',
  UNKNOWN_ACTION = 'unknown_action',
  NONE = 'none'
}

export function actionDagViolationFromWire(value: string): ActionDagViolation | undefined {
  if (value === ActionDagViolation.CYCLE) {
    return ActionDagViolation.CYCLE;
  }
  if (value === ActionDagViolation.MISSING_PREREQUISITE) {
    return ActionDagViolation.MISSING_PREREQUISITE;
  }
  if (value === ActionDagViolation.UNKNOWN_ACTION) {
    return ActionDagViolation.UNKNOWN_ACTION;
  }
  if (value === ActionDagViolation.NONE) {
    return ActionDagViolation.NONE;
  }
  return undefined;
}

export enum ProviderPolicyState {
  EFFECTIVE = 'effective',
  SUPERSEDED = 'superseded',
  NEEDS_REVIEW = 'needs_review'
}

export function providerPolicyStateFromWire(value: string): ProviderPolicyState | undefined {
  if (value === ProviderPolicyState.EFFECTIVE) {
    return ProviderPolicyState.EFFECTIVE;
  }
  if (value === ProviderPolicyState.SUPERSEDED) {
    return ProviderPolicyState.SUPERSEDED;
  }
  if (value === ProviderPolicyState.NEEDS_REVIEW) {
    return ProviderPolicyState.NEEDS_REVIEW;
  }
  return undefined;
}

export enum TemporalChangePhase {
  BEFORE = 'before',
  TRANSITION = 'transition',
  AFTER = 'after'
}

export function temporalChangePhaseFromWire(value: string): TemporalChangePhase | undefined {
  if (value === TemporalChangePhase.BEFORE) {
    return TemporalChangePhase.BEFORE;
  }
  if (value === TemporalChangePhase.TRANSITION) {
    return TemporalChangePhase.TRANSITION;
  }
  if (value === TemporalChangePhase.AFTER) {
    return TemporalChangePhase.AFTER;
  }
  return undefined;
}

export enum MakeBeforeBreakStatus {
  BLOCKED = 'blocked',
  ALLOWED = 'allowed'
}

export function makeBeforeBreakStatusFromWire(value: string): MakeBeforeBreakStatus | undefined {
  if (value === MakeBeforeBreakStatus.BLOCKED) {
    return MakeBeforeBreakStatus.BLOCKED;
  }
  if (value === MakeBeforeBreakStatus.ALLOWED) {
    return MakeBeforeBreakStatus.ALLOWED;
  }
  return undefined;
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
  CANCELLED_BY_USER = 'cancelled_by_user',
  MACHINE_CANNOT_CONFIRM_FAILURE_DOMAIN = 'machine_cannot_confirm_failure_domain',
  TEMPORAL_WINDOW_INVALID_ORDER = 'temporal_window_invalid_order',
  BREAK_BEFORE_MAKE_FORBIDDEN = 'break_before_make_forbidden',
  ACTION_DAG_CYCLE = 'action_dag_cycle',
  ACTION_MISSING_PREREQUISITE = 'action_missing_prerequisite',
  PROVIDER_POLICY_UNVERIFIABLE = 'provider_policy_unverifiable'
}

export function errorCodeFromWire(value: string): ErrorCode | undefined {
  if (value === ErrorCode.INVALID_JSON) {
    return ErrorCode.INVALID_JSON;
  }
  if (value === ErrorCode.INVALID_STRUCTURE) {
    return ErrorCode.INVALID_STRUCTURE;
  }
  if (value === ErrorCode.BOUNDS_EXCEEDED) {
    return ErrorCode.BOUNDS_EXCEEDED;
  }
  if (value === ErrorCode.KDF_FAILED) {
    return ErrorCode.KDF_FAILED;
  }
  if (value === ErrorCode.CRYPTO_AUTH_FAILED) {
    return ErrorCode.CRYPTO_AUTH_FAILED;
  }
  if (value === ErrorCode.UNSUPPORTED_FORMAT_VERSION) {
    return ErrorCode.UNSUPPORTED_FORMAT_VERSION;
  }
  if (value === ErrorCode.UNSUPPORTED_SCHEMA) {
    return ErrorCode.UNSUPPORTED_SCHEMA;
  }
  if (value === ErrorCode.MIGRATION_FAILED) {
    return ErrorCode.MIGRATION_FAILED;
  }
  if (value === ErrorCode.PAYLOAD_INVALID) {
    return ErrorCode.PAYLOAD_INVALID;
  }
  if (value === ErrorCode.PAYLOAD_KIND_MISMATCH) {
    return ErrorCode.PAYLOAD_KIND_MISMATCH;
  }
  if (value === ErrorCode.PAYLOAD_VERSION_UNSUPPORTED) {
    return ErrorCode.PAYLOAD_VERSION_UNSUPPORTED;
  }
  if (value === ErrorCode.SOURCE_PARSE_FAILED) {
    return ErrorCode.SOURCE_PARSE_FAILED;
  }
  if (value === ErrorCode.MAPPING_MISSING_COLUMN) {
    return ErrorCode.MAPPING_MISSING_COLUMN;
  }
  if (value === ErrorCode.VALIDATION_FAILED) {
    return ErrorCode.VALIDATION_FAILED;
  }
  if (value === ErrorCode.RELATION_NOT_REGISTERED) {
    return ErrorCode.RELATION_NOT_REGISTERED;
  }
  if (value === ErrorCode.RELATION_KIND_VIOLATION) {
    return ErrorCode.RELATION_KIND_VIOLATION;
  }
  if (value === ErrorCode.CAPABILITY_MISMATCH) {
    return ErrorCode.CAPABILITY_MISMATCH;
  }
  if (value === ErrorCode.GROUP_MODE_NOT_ALLOWED) {
    return ErrorCode.GROUP_MODE_NOT_ALLOWED;
  }
  if (value === ErrorCode.ILLEGAL_STATE_TRANSITION) {
    return ErrorCode.ILLEGAL_STATE_TRANSITION;
  }
  if (value === ErrorCode.ACTION_FROZEN) {
    return ErrorCode.ACTION_FROZEN;
  }
  if (value === ErrorCode.ENTITY_NOT_FOUND) {
    return ErrorCode.ENTITY_NOT_FOUND;
  }
  if (value === ErrorCode.SCENARIO_NOT_EXECUTABLE) {
    return ErrorCode.SCENARIO_NOT_EXECUTABLE;
  }
  if (value === ErrorCode.SCENARIO_MISSING_INPUT) {
    return ErrorCode.SCENARIO_MISSING_INPUT;
  }
  if (value === ErrorCode.DB_LOCKED) {
    return ErrorCode.DB_LOCKED;
  }
  if (value === ErrorCode.DB_OPEN_FAILED) {
    return ErrorCode.DB_OPEN_FAILED;
  }
  if (value === ErrorCode.AUTH_CANCELLED) {
    return ErrorCode.AUTH_CANCELLED;
  }
  if (value === ErrorCode.AUTH_FAILED) {
    return ErrorCode.AUTH_FAILED;
  }
  if (value === ErrorCode.AUTH_UNAVAILABLE) {
    return ErrorCode.AUTH_UNAVAILABLE;
  }
  if (value === ErrorCode.KEY_UNAVAILABLE) {
    return ErrorCode.KEY_UNAVAILABLE;
  }
  if (value === ErrorCode.CANCELLED_BY_USER) {
    return ErrorCode.CANCELLED_BY_USER;
  }
  if (value === ErrorCode.MACHINE_CANNOT_CONFIRM_FAILURE_DOMAIN) {
    return ErrorCode.MACHINE_CANNOT_CONFIRM_FAILURE_DOMAIN;
  }
  if (value === ErrorCode.TEMPORAL_WINDOW_INVALID_ORDER) {
    return ErrorCode.TEMPORAL_WINDOW_INVALID_ORDER;
  }
  if (value === ErrorCode.BREAK_BEFORE_MAKE_FORBIDDEN) {
    return ErrorCode.BREAK_BEFORE_MAKE_FORBIDDEN;
  }
  if (value === ErrorCode.ACTION_DAG_CYCLE) {
    return ErrorCode.ACTION_DAG_CYCLE;
  }
  if (value === ErrorCode.ACTION_MISSING_PREREQUISITE) {
    return ErrorCode.ACTION_MISSING_PREREQUISITE;
  }
  if (value === ErrorCode.PROVIDER_POLICY_UNVERIFIABLE) {
    return ErrorCode.PROVIDER_POLICY_UNVERIFIABLE;
  }
  return undefined;
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
