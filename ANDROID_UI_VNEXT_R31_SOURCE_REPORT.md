# ANDROID_UI_VNEXT_R31_SOURCE_REPORT

> 2026-10-10 · `feat/android-ui-vnext-translation`
>
> R31 closes the remaining **Manual Relationship production-authority gap** for the
> relation vocabulary that is already canonical and executable in Native schema v3.
>
> It does not widen Canonical, does not enable storage-only future relations, and
> does not change Preview into a writable environment.

## 0. Current truth

```text
ANDROID_UI_VNEXT_SOURCE = R31

FIVE_PRIMARY_IA = FROZEN
REFERENCE_PRODUCT_UX_DESIGN = COMPLETE
ANDROID_LIGHT_DIRECTION = DESIGN_FROZEN

PRODUCTION_MANUAL_OBJECT = SOURCE_BOUND_EXECUTABLE
PRODUCTION_MANUAL_RELATIONSHIP = SOURCE_BOUND_EXECUTABLE_CURRENT_V3_RUNTIME_SET
PRODUCTION_FINDINGS = SOURCE_BOUND_PARTIAL_AUTHORITY
PRODUCTION_CHANGE = SOURCE_BOUND_EXECUTABLE
PRODUCTION_HUMAN_REVIEW = SOURCE_BOUND_EXECUTABLE
PRODUCTION_IMPORT_HANDOFF = SOURCE_BOUND

CANONICAL_SCHEMA_CHANGE_FOR_R31 = NONE
DEPMAP_PAYLOAD_CHANGE_FOR_R31 = NONE

STORAGE_ONLY_RELATIONS = HOLD
PHONE_EMAIL_SUBTYPE_CANONICAL = HOLD
LIFECYCLE_CANONICAL = HOLD
MAINACTIVITY_PRODUCTION_CUTOVER = HOLD

FRESH_R31_BUILD = PENDING
FRESH_R31_UNIT_TESTS = PENDING
FRESH_R31_INSTRUMENTATION = PENDING
FRESH_R31_PHONE_PIXELS = PENDING
FRESH_R31_TABLET_PIXELS = PENDING
FRESH_R31_HUMAN_ACCEPTANCE = PENDING

ANDROID_REFERENCE_FREEZE = HOLD
PRODUCTION_VNEXT_CUTOVER = HOLD
```

## 1. Repository audit changed the authority conclusion

R25/R26 correctly refused to add a ghost Save button, but their production gate
assumed that all Manual Relationship mutation had to wait for a future Native
Schema v4.

R31 re-audited current repository truth:

- `spec/domain/domain.json` appSchemaVersion remains v3;
- Relation runtime registry already contains:
  - funding_source;
  - merchant_agreement;
  - recovers;
  - authenticates;
  - controls;
- `android/core/.../Relations.kt` validates those exact relations, endpoint kinds
  and capability;
- the existing Dependency table already stores relation/capability/origin/state/
  criticality/verification basis;
- Proposal acceptance already writes Dependencies through the same v3 storage;
- storage-only `verifies` / `bound_to` are intentionally rejected by runtime
  validation.

Therefore the correct boundary is:

```text
current five runtime relations
→ production manual confirmation may be implemented now

verifies / bound_to / future relation widening
→ remain HOLD
```

This supersedes the older all-or-nothing Schema-v4 HOLD.

## 2. Authoritative mutation path

New production path:

```text
ProductionManualRelationshipScreen
→ AppContainerVNextManualRelationshipGateway
→ AppContainer.createManualDependency
→ GraphRepository.createManualDependency
→ validateRelationUse
→ authoritative DB transaction
→ graphRevision bump
→ re-read Dependency
→ render committed result
```

Compose owns no graph mutation semantics.

## 3. Endpoint and relation validation

From / To:
- must exist in confirmed Reality;
- must not be archived;
- are selected by object identity, never typed as raw node IDs.

Relation:
- comes from `RELATION_DEFINITIONS`;
- allowed From/To NodeKinds come from the same definition;
- capability is derived from the relation definition;
- the form does not let the user choose an incompatible capability.

Executable relations:

| relation | capability |
| --- | --- |
| funding_source | payment |
| merchant_agreement | payment |
| recovers | recovery |
| authenticates | authentication |
| controls | access |

Not executable:

```text
verifies
bound_to
```

## 4. Criticality

Default:

```text
unknown
```

Permanent meaning:

```text
unknown != optional
unknown != safe
```

Only an explicit user selection may create/upgrade:

```text
required
```

The gateway never infers required from relation kind, impact, provider knowledge or
UI context.

## 5. Logical-row lifecycle

Manual confirmation respects the existing Dependency logical key:

```text
from | relation | to | capability
```

New logical relation:
- create one row;
- `origin=manual`;
- `state=active`;
- `verification_basis_type=user_confirmed`;
- graphRevision +1.

Existing active relation:
- re-confirm the same row;
- refresh verification time;
- explicit required may upgrade criticality;
- do not create a duplicate row;
- graphRevision +1 because Reality was re-confirmed/updated.

Retired logical relation:
- reactivate the same row;
- clear retired state;
- re-confirm;
- graphRevision +1.

## 6. Production consumer flow

Production Establish now exposes:

```text
建立基础设施
├─ 手工记录对象
├─ 手工记录关系
└─ 文件导入
```

Manual Relationship sequence:

```text
From confirmed object
→ relation
→ derived capability
→ To confirmed object
→ unknown / explicit required
→ confirmation preview
→ authoritative Save
→ committed result + graphRevision
```

It explicitly reminds the user:

```text
two edges != two independent paths
```

FailureDomain / RecoveryCycle / ProviderPolicy remain Continuity responsibilities.

## 7. Preview remains read-only

R25 reference screen still demonstrates:
- relation vocabulary;
- direction;
- unknown vs required;
- independence boundary.

It still does not execute Production mutation.

Permanent rule:

```text
production authority exists
!=
reference preview may write Reality
```

## 8. Tests

R31 adds/updates:

- `ProductionManualRelationshipTest`
  - exact five relation definitions;
  - verifies/bound_to excluded;
  - endpoint-kind constraints retained.

- `ManualRealityAuthorityEvidenceTest`
  - authoritative v3 relation creation;
  - graphRevision bumps once;
  - origin=manual;
  - verification basis=user_confirmed;
  - logical row reuse;
  - explicit required upgrade;
  - capability mismatch rejected;
  - storage-only relation rejected without mutation.

- `VNextCapabilityMatrixTest`
  - Manual Relationship Production authority = AVAILABLE.

Production Compose screen is source-bound to the same gateway and contains no
direct SQL/domain mutation.

## 9. R30 retained

R31 keeps the R30 Production Findings closure:
- single confirmed recovery source;
- confirmed recovery cycle;
- pending recovery Proposal;
- pending Change verification;
- explicit unsupported coverage for shared-failure-domain / stale-recovery /
  unknown-critical-path until their authoritative inputs exist.

No health score.

## 10. CI state

R29 CI exposed a real Production Compose compile defect around an explicit
`weight` import. That source defect was repaired before R30/R31.

A separate Maven resolution failure for
`com.google.errorprone:error_prone_annotations:2.3.1` occurred in Android core.
It is treated as dependency-resolution infrastructure unless a fresh exact-head
run proves a persistent source/configuration issue.

R31 does not claim PASS until the current exact head completes CI.

## 11. Remaining non-fakeable gates

The remaining product/source gates are now materially narrower:

```text
Identity Anchor subtype Canonical
→ production Number / Email identity

Lifecycle Canonical
→ production card/number maintenance fields

full FailureDomain/freshness/critical-path authority
→ complete seven-class production Findings

production launcher/security cutover
→ active ProductionVNextShell

exact-head API36 runtime + pixels + human review
→ Android Reference Freeze
```

Future relation widening is also governed, but current manual relationship is no
longer blocked.

## 12. Stop line

```text
R31_MANUAL_RELATIONSHIP_AUTHORITY = SOURCE_IMPLEMENTED
CURRENT_V3_RUNTIME_RELATION_SET = EXECUTABLE
STORAGE_ONLY_FUTURE_RELATIONS = HOLD

REFERENCE_PREVIEW_MUTATION = FORBIDDEN

SOURCE_COMPLETE != BUILD_PASS != RUNTIME_VERIFIED != HUMAN_ACCEPTED

ANDROID_REFERENCE_FREEZE = HOLD
PRODUCTION_VNEXT_CUTOVER = HOLD
```
