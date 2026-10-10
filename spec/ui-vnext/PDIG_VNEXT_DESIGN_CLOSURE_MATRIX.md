# PDIG vNext — Product / UX / Canonical Design Closure Matrix

> Date: 2026-10-10
> Status: **R34 DESIGN CLOSED / PRODUCTION SOURCE-BINDING + SECURE REHEARSAL INDEX**
>
> This matrix distinguishes design completeness from runtime/production authority.
> It is an index, not a replacement for the canonical spec/proposals.

## 0. Permanent product shell

~~~text
现在 / 基础设施 / 变更 / 记录 / 我
~~~

Status: **FROZEN / SOURCE IMPLEMENTED**

No roadmap capability creates a sixth primary destination.

## 1. Core daily product loop

| Capability | Design | Android reference source | Production authority / Canonical |
| --- | --- | --- | --- |
| Now / attention | complete | implemented | existing production projection still needs final VNext cutover |
| Infrastructure inventory | complete | implemented | existing production nodes available |
| Region / Globe | complete | implemented | RegionFact v1 design complete; Production reuses GPU Earth with zero inferred regions until governed RegionFact Canonical exists; runtime pixel/GPU evidence still gates freeze |
| Cards | complete | implemented | production category/detail/relations/Impact + replace-card ChangePlan entry source-bound; launcher cutover pending |
| Numbers | complete | implemented reference | production phone mapping waits identity subtype |
| Accounts | complete | implemented | production category + focused generic detail + Impact source-bound |
| Emails | complete | implemented reference | production email mapping waits identity subtype |
| Devices | complete | implemented reference | production generic detail source-bound; factor enrichment remains future-gated |
| Services | complete | implemented | production category + focused generic detail + Impact source-bound |
| Weaknesses / Infrastructure Findings | complete | R27 full seven-class grammar implemented | production findings must come from authoritative continuity analysis; reference findings never enter Reality |
| Me | complete | implemented, fifth primary | production personal control surface + store-backed local Presentation preferences wired through VNextShellViewModel |
| Search | complete | implemented | production Reality-only object / plan / source / route search source-bound |

## 2. Object understanding

| Capability | Design | Source | Gate |
| --- | --- | --- | --- |
| Object Detail hierarchy | complete | R20 | none at UX level |
| Impact Lens | complete | R19/R20 | production must use authoritative impact |
| Card lifecycle | complete | R18/R19 | persistence requires Maintenance Canonical |
| Number lifecycle / keep-number | complete | R18/R19 | persistence requires Maintenance Canonical |
| recovery-use vs unique-recovery truth | complete | implemented | explicit evidence only |
| saved aliases | complete | implemented | Presentation only |

## 3. Change / continuity

| Capability | Design | Source | Production status |
| --- | --- | --- | --- |
| Change Center | complete | R20/R21 | source implemented |
| replace_phone_number | complete | reference flow implemented | production scenario exists; VNext binding pending |
| replace_payment_card | complete | reference flow implemented | production card detail can create/continue authoritative ChangePlan; action/verification screen source-bound |
| Current / Transition / After | complete | implemented | After always Plan Projection |
| Make-Before-Break | complete | implemented semantics | production authoritative plan required |
| Action DAG | complete | core implemented | existing |
| done != verified | frozen | implemented | mandatory |
| Device Continuity | complete design | hidden | factor/device subtype + scenario required |
| Recovery Incident | complete design | hidden | Recovery Solver required |
| Digital Resource Continuity | complete design | hidden | future Canonical required |

## 4. Establish / confirm

| Capability | Design | Reference | Production authority |
| --- | --- | --- | --- |
| File Import | complete | R23 read-only | production Host handoff source-bound to existing FileWorkflowCoordinator; launcher host injection pending |
| Manual Establish object | complete | R24 reference + production form | AppContainer authority + production VNext form source-bound; launcher cutover still gated |
| Manual Relationship | complete | R25 Preview read-only + R31 Production executable | current Canonical v3 runtime registry is authoritative; verifies/bound_to remain future/storage-only |
| Human Review | complete | R22 reference + production inbox | production Proposal/Candidate/Drift source/action UI bound with authoritative re-read |
| Proposal != Reality | frozen | enforced in UX | canonical |
| Candidate != Node | frozen | enforced in UX | canonical |
| Drift decision | complete | read-only reference | production authority exists |

## 5. Maintenance

| Capability | Design | UI | Gate |
| --- | --- | --- | --- |
| MaintenanceFact | complete proposal | lifecycle reference only | Canonical not implemented |
| MaintenanceSchedule | complete proposal | upcoming reference only | Canonical not implemented |
| annual fee / billing checkpoints | complete | reference rendered | persistence HOLD |
| number keep-alive | complete | reference rendered | persistence HOLD |
| stale recovery info | semantics complete | future preparedness | factor/lifecycle Canonical |

Proposal:
- spec/proposals/asset-lifecycle-maintenance-v1.md

## 6. Identity

| Capability | Design | UI | Gate |
| --- | --- | --- | --- |
| identity_anchor subtype | complete proposal | reference Number/Email exist | Canonical not implemented |
| phone/email conservative normalization | complete | hidden production mapping | Canonical required |
| user alias | complete | implemented | Presentation only |
| IdentityContext | complete proposal | hidden | Canonical membership required |
| Context × Region intersections | complete | hidden | no graph-edge inference |

Proposals:
- spec/proposals/identity-anchor-subtype-v1.md
- spec/proposals/identity-context-v1.md
- spec/proposals/region-facts-v1.md

## 7. Authentication / recovery substrate

| Capability | Design | UI | Gate |
| --- | --- | --- | --- |
| AccessFactor | complete | hidden/contextual future | Canonical required |
| RecoveryFactor | complete | hidden/contextual future | Canonical required |
| FactorBinding | complete proposal | hidden | Canonical required |
| factor portability | complete | hidden | Canonical required |
| SecretLocator | complete | hidden | Canonical required |
| secret values | explicitly forbidden | never UI/store | permanent |
| Recovery Preparedness | complete UX | hidden | Factor Canonical required |
| Recovery Incident | complete | hidden | solver/runtime required |

Proposals/contracts:
- spec/proposals/access-recovery-factor-v1.md
- spec/proposals/secret-locator-v1.md
- spec/ui-vnext/RECOVERY_PREPAREDNESS_UX_CONTRACT.md
- spec/proposals/recovery-incident-mode-v1.md

## 8. Continuity reasoning

| Capability | Design | Core/runtime |
| --- | --- | --- |
| FailureDomain | frozen | implemented in core |
| path count != independent path count | frozen | mandatory |
| RecoveryCycle | frozen | implemented in core |
| confirmed vs potential cycle | frozen | core/fixtures |
| ProviderPolicy != PersonalReality | frozen | core service/fixtures |
| Infrastructure Finding | frozen | R27 full consumer grammar; production projection must remain engine-backed |
| no global health score | frozen | UI contract |
| unknown != safe | frozen | UI/domain contract |

No new UI scoring layer is permitted.

## 8.1 Product modes / temporal / evidence / advanced continuity

| Capability | Design | Runtime / authority |
| --- | --- | --- |
| Prepare | complete | contextual Object Detail / Impact / ChangePlan entry |
| Change | complete | authoritative ChangePlan / Action DAG / Verification |
| Recover | complete design | gated by Recovery Incident / Solver |
| Maintain | complete | Findings / Review / Timeline / Verification; lifecycle freshness expands after Canonical |
| Blast Radius | complete design | current Impact Kernel is partial capability-aware primitive; generic/failure-domain removal extension pending |
| Minimal Cut Set | complete design | proposed; must be FailureDomain-aware and bounded/deterministic |
| Temporal Change | frozen | core runtime implemented |
| waiting / verification window / retire gate | frozen | TemporalChange runtime; consumer projection may expand wording |
| ProviderPolicy revision | frozen | core runtime implemented; Knowledge never overwrites Reality |
| Evidence multi-source authority | frozen | implemented/tested |
| Freshness | complete semantics | partial; future fact/schedule Canonical expands Maintain |
| Cross-platform recovery control-surface independence | complete principle | future runtime/evidence gate; does not imply cloud sync |

Contract:
- `spec/proposals/continuity-analysis-modes-v1.md`

Permanent rule:

~~~text
Prepare / Change / Recover / Maintain
= contextual modes over the same Reality
!= four new primary tabs

Blast Radius / Minimal Cut
= authoritative continuity analysis
!= UI degree counting / heuristic score
~~~

## 9. Provider / digital resources

| Capability | Design | Visibility |
| --- | --- | --- |
| provider policy boundary | frozen | contextual |
| domain/registrar continuity | complete future design | hidden |
| DNS continuity | complete future design | hidden |
| repository/organization continuity | complete future design | hidden |
| cloud/hosting/data continuity | complete future design | hidden |
| transfer/migrate/region-change future primitives | complete design direction | hidden |

Proposal:
- spec/proposals/digital-resource-continuity-v1.md

## 10. Trusted continuity

| Capability | Design | Visibility |
| --- | --- | --- |
| TrustedParty | complete future design | hidden |
| ProviderHandoffArrangement | complete future design | hidden |
| scoped TrustedHandoffPlan | complete future design | hidden |
| encrypted handoff package | complete design | no implementation |
| autonomous dead-man switch | explicitly out of scope | forbidden without separate architecture |
| secret escrow | forbidden | permanent |
| legal will/estate authority | outside PDIG | external |

Proposal:
- spec/proposals/trusted-handoff-v1.md

## 11. Production VNext binding

Design is frozen.

~~~text
Canonical / encrypted Reality
→ AppContainer
→ production VNext read model
→ consumer projection
→ VNext UI
~~~

Current source includes:
- production snapshot and strict Reference-vs-Reality mode boundary;
- conservative surface classification;
- payment asset issuer/last4 projection;
- consumer inventory projection;
- production category/detail binding for payment/account/device/service;
- explicit phone/email/finding HOLD surfaces rather than subtype inference;
- authoritative Impact projection;
- production Reality-only global Search;
- production Records projection with completion / verification / evidence separation;
- fifth-primary Me production control surface;
- local Presentation preference screen with store-backed VAppState; MainActivity Production rehearsal injects the same VNextShellViewModel state, so workspace preferences survive ViewModel/process recreation without entering Canonical;
- review projection + authoritative Human Review source/action gateway;
- plan projection + authoritative Change action gateway;
- card-detail → create/continue replace_payment_card ChangePlan;
- authority-aware ChangePlan execution screen with readiness/staleness gates;
- authoritative Import projection/authority seam;
- host-owned FileWorkflowCoordinator request seam;
- authoritative Manual Establish gateway + production form;
- authoritative Manual Relationship for the current five Canonical v3 runtime relations; future/storage-only relation widening remains HOLD.

R32/R33 launcher/security source state:
- reusable fail-closed `PdigSecureContent` extracted from the current production app;
- `ProductionVNextSecureHost` uses that exact security gate;
- productionDebug can explicitly rehearse real-Reality VNext through `vnext_production`;
- productionRelease ignores both `vnext_demo` and `vnext_production` extras;
- default production route remains legacy until acceptance;
- R33 adds an API36 runtime workflow that proves locked-before-Reality, five-primary Production VNext after explicit unlock, background/process relock, and default productionDebug remaining legacy;
- R33 also reuses the canonical `FLAG_SECURE` window implementation for sensitive Production VNext Reality/review/history/change/search surfaces.

Still required before release cutover:
- latest exact-head secure rehearsal workflow PASS on the final candidate;
- explicit release cutover decision / rollback switch;
- generic identity subtype implementation for phone/email;
- subtype-aware phone/email after Canonical;
- lock/security parity evidence;
- persistence/restart/E2E.

## 12. Capability / authority matrix

Central source:
- android/app/src/main/kotlin/com/pdig/uivnext/capability/VNextCapabilityMatrix.kt
- spec/ui-vnext/CAPABILITY_AUTHORITY_MATRIX.md

Permanent rule:

~~~text
DESIGN_COMPLETE
!=
REFERENCE_VISIBLE
!=
PRODUCTION_EXECUTABLE
~~~

## 13. Android visual direction

~~~text
light-first
consumer-facing
asset-first
spatial where meaningful
Chinese-first UI copy
five primary destinations
same GPU world family across widths
~~~

Source complete does not equal Reference Freeze.

## 14. Remaining non-design gates

These are engineering/evidence work, not missing product/UX architecture:

~~~text
exact-head compile/test
API36 phone runtime/pixels
tablet runtime/pixels
GPU runtime evidence
human pixel acceptance
Android Reference Freeze
production VNext release activation after debug rehearsal
manual Dependency gateway (R31 implemented for current Canonical v3 runtime relation set)
Canonical implementations for proposed schemas
cross-platform conformance for future schemas
production launcher cutover
iOS/Harmony UI translation after Android freeze
real-user/real-data/store gates when reopened
~~~

## 15. Design closure assertion + R34 production source progress

Within the product scope and roadmap explicitly described by v2.3-R1, the remaining
major concepts now have one of three explicit outcomes:

~~~text
A. current product → source-designed/implemented
B. future valid capability → design/proposal frozen + capability gate
C. unsafe/out-of-scope behavior → explicitly forbidden
~~~

No known v2.3 roadmap concept is allowed to exist only as an ambiguous UI idea.

## 16. Stop line

~~~text
PDIG_VNEXT_PRODUCT_UX_DESIGN = CLOSED_AND_EXTENDED_THROUGH_R34
FIVE_PRIMARY_IA = FROZEN
FUTURE_CAPABILITIES = GATED
GHOST_CAPABILITIES = FORBIDDEN

ANDROID_RUNTIME_ACCEPTANCE = PENDING
ANDROID_REFERENCE_FREEZE = HOLD
PRODUCTION_VNEXT_CUTOVER = HOLD

DESIGN_COMPLETE != SHIPPED
~~~