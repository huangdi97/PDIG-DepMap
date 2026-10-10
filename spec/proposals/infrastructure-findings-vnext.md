# Personal Infrastructure Findings vNext

> Date: 2026-10-10  
> Status: **DESIGN_FROZEN / V0.3 TAXONOMY PRESERVED / FUTURE CONDITIONS MAPPED**
>
> Purpose: freeze how PDIG turns continuity facts into consumer findings without
> inventing a health score or casually widening the current Canonical enum.
>
> This document does **not** change `InfrastructureFindingType`.

---

## 1. A Finding is an explainable condition, not a score

A Personal Infrastructure Finding answers:

> **“哪一个已确认事实或明确未知，值得我现在处理或重新核对？”**

Every rendered Finding must carry:

```text
what
why
confirmedBasis
unknowns
affectedCapability?
recommendedNextAction
evidenceRefs[]
truth class
```

A Finding is never:

```text
risk = 83%
health = 72
security = good
```

Permanent rules:

```text
unknown != safe
coverage != readiness
pathCount != independentPathCount
proposal != Reality
provider support != user configuration
done != verified
```

---

## 2. v0.3 Canonical taxonomy stays exactly seven

Current Canonical `InfrastructureFindingType` remains:

```text
SINGLE_POINT_OF_FAILURE
SHARED_FAILURE_DOMAIN
RECOVERY_CYCLE
UNCONFIRMED_FALLBACK
STALE_RECOVERY_INFORMATION
UNKNOWN_CRITICAL_PATH
PENDING_VERIFICATION
```

R34 does not add an eighth type merely because a product phrase sounds useful.

A new Canonical Finding type requires the normal:

```text
Master decision
→ machine-readable Domain Spec
→ schema/version impact review
→ fixtures/expected
→ conformance
→ all runtimes
→ UI
```

---

## 3. Truth classes

Consumer findings need at least these truth classes:

```text
CONFIRMED_FACT
DERIVED_FROM_CONFIRMED
PENDING_REVIEW
UNKNOWN_COVERAGE
UNAVAILABLE_AUTHORITY
```

The UI may translate them:

```text
CONFIRMED_FACT          → 已确认
DERIVED_FROM_CONFIRMED  → 基于已确认记录分析
PENDING_REVIEW          → 待复核
UNKNOWN_COVERAGE        → 仍有未知
UNAVAILABLE_AUTHORITY   → 当前还不能判断
```

A derived finding may be authoritative if its algorithm consumes only confirmed
Reality and a governed deterministic kernel. “Derived” does not mean “AI guess.”

---

## 4. Conditions for the current seven

### 4.1 SINGLE_POINT_OF_FAILURE

Minimum basis:

```text
one confirmed active path for the target capability
AND
no second confirmed path in current known scope
```

Consumer wording must say:

> 当前只记录到一条已确认路径。

It must **not** say:

> 现实中只有这一条路。

If FailureDomain independence is available, the richer condition should prefer:

```text
independentPathCount == 1
```

rather than raw edge count.

### 4.2 SHARED_FAILURE_DOMAIN

Basis:

```text
>=2 candidate paths
AND
confirmed shared FailureDomain reduces independence
```

Suspected/unconfirmed shared domains yield review/unknown language, not a confirmed
shared-domain Finding.

### 4.3 RECOVERY_CYCLE

Basis:

```text
confirmed active recovery edges
→ governed RecoveryCycle detector
→ confirmed_cycle
```

Proposal/Candidate edges can at most produce potential/review state.

### 4.4 UNCONFIRMED_FALLBACK

Basis:

```text
Proposal / Candidate / other non-Reality evidence suggests a fallback
BUT fallback is not confirmed Reality
```

The recommendation is review/verification, not “use this backup.”

### 4.5 STALE_RECOVERY_INFORMATION

Requires a governed freshness rule.

```text
confirmed fact/source
+ explicit freshness policy
+ last-confirmed timestamp
→ stale / needs review
```

Forbidden:

```text
looks old
blank timestamp
long time since app opened
→ stale
```

Until the freshness policy is authoritative, Production must disclose this class as
unsupported.

### 4.6 UNKNOWN_CRITICAL_PATH

Basis:

```text
confirmed relationship exists
AND
criticality / impact requiredness cannot be determined
```

This is not an error state. It is an epistemic Finding:

> “This relationship is real; its consequence is not sufficiently known.”

Machine inference must never promote `criticality=unknown` to `required`.

### 4.7 PENDING_VERIFICATION

Basis:

```text
ChangePlan action done
AND
verification != verified / not_required
```

Permanent rule:

```text
done != verified
```

Evidence suggestion may move the verification workflow into
`evidence_suggested`, but it still does not equal `verified`.

---

## 5. Mapping the wider v2.3 finding vocabulary

The v2.3 product master proposes useful consumer/analysis concepts beyond the
current seven. R34 freezes how they map so later agents do not casually widen
Canonical.

| v2.3 concept | R34 mapping |
| --- | --- |
| HIGH_BLAST_RADIUS | future Blast Radius analysis result / Maintain attention; **not a new v0.3 enum today** |
| NO_INDEPENDENT_RECOVERY_ROOT | derived condition over FailureDomain-aware independence; may surface through SINGLE_POINT_OF_FAILURE / SHARED_FAILURE_DOMAIN until a future Canonical decision |
| CORRELATED_RECOVERY_PATHS | SHARED_FAILURE_DOMAIN |
| MINIMAL_CUT_SET_FRAGILITY | future Minimal Cut Set result; hidden until bounded authoritative algorithm exists |
| TEMPORAL_CHANGE_PENDING | Change/Timeline temporal state; becomes a Finding only when a real condition such as pending verification/blocked retirement applies |
| UNVERIFIED_AFTER_CHANGE | PENDING_VERIFICATION |
| SECRET_LOCATION_UNKNOWN | future Recovery Preparedness condition after SecretLocator Canonical exists; no current Production Finding |

This distinction prevents product-language growth from becoming schema churn.

---

## 6. Severity is a projection from conditions

Finding type alone must not permanently hard-code severity.

Conceptual severity:

```text
INFO
REVIEW
WARNING
CRITICAL
```

Severity inputs may include:

```text
affected capability criticality
confirmed loss/blocking state
independentPathCount
confirmed cycle
Make-Before-Break violation/block
verification failure
due/freshness urgency
unknown coverage
```

Examples:

### Critical candidate

```text
required critical capability
AND
confirmed independentPathCount == 0
```

### Warning candidate

```text
confirmed single path remains
BUT current capability is still available
```

### Review candidate

```text
potential fallback / unknown criticality / suspected shared domain
```

Unknown coverage may **raise the need to review**, but cannot by itself create a
confirmed critical failure.

No global severity sum is allowed.

---

## 7. Finding identity / dedup

A Finding needs a deterministic identity derived from its semantic condition, not
its rendered sentence.

Conceptual key:

```text
findingType
targetStateKey?
capability?
basisLogicalKeys[]
algorithmVersion
```

Repeated ingestion of the same evidence must not generate duplicate cards.

When evidence changes:
- same semantic condition → update/re-evaluate existing Finding;
- materially different target/basis → new Finding;
- resolved condition → Records may retain a resolution event, while current
  Weaknesses stops showing it.

---

## 8. Lifecycle states

Proposed UI/query lifecycle:

```text
OPEN
NEEDS_REVIEW
RESOLVED
SUPERSEDED
UNAVAILABLE
```

These are consumer/query states; they are **not** added to Canonical by this
document.

A current Finding disappears from the active Weaknesses list only because the
underlying authoritative condition changed or was resolved—not because the user
dismissed an inconvenient warning.

“Dismiss” may hide a review reminder locally, but cannot rewrite Reality.

---

## 9. Evidence explanation

Every Finding detail should answer:

```text
为什么出现？
依据是什么？
哪些仍不知道？
如果不处理会怎样？
下一步是什么？
```

Technical mode may additionally show:
- graphRevision;
- dependency IDs;
- FailureDomain IDs;
- Proposal/Evidence refs;
- algorithm version.

Consumer mode should keep those behind “查看依据”.

---

## 10. Now / Weaknesses / Object Detail placement

### Now

Only task-worthy current items:

```text
critical/warning Finding
pending verification
review due
maintenance due
```

Now is not a catalog of every analytical result.

### Weaknesses

Full current Finding inbox:
- filter by class;
- truth state;
- affected capability;
- object;
- review status.

No health score.

### Object Detail

Impact Lens can show the subset relevant to the focused object.

### Records

Only events that actually occurred:
- Finding created from authoritative analysis;
- reviewed/confirmed basis changed;
- resolved/superseded;
- verification completed/failed.

A hypothetical finding in Preview is not history.

---

## 11. Production authority table

As of R34:

```text
SINGLE_POINT_OF_FAILURE   partial authority available
RECOVERY_CYCLE            authority available
UNCONFIRMED_FALLBACK      authority available
PENDING_VERIFICATION      authority available

SHARED_FAILURE_DOMAIN     production input not yet exposed end-to-end
STALE_RECOVERY_INFORMATION governed freshness input missing
UNKNOWN_CRITICAL_PATH     full production critical-path authority not yet exposed
```

Therefore Production Weaknesses must:
- show supported results;
- state unsupported classes;
- never import Preview synthetic findings.

---

## 12. Future advanced continuity mapping

When runtime support arrives:

### Blast Radius

May generate:
- a focused Impact Lens result;
- a HIGH_BLAST_RADIUS task-worthy Finding if product thresholds are explicitly
  governed.

No threshold may be invented in Compose.

### Minimal Cut Set

May generate:
- “会一起卡住你的关键组合” analysis;
- future MINIMAL_CUT_SET_FRAGILITY Finding if a Canonical decision adds it.

A truncated candidate search must disclose `coverage=PARTIAL`.

### Recovery Solver

May produce current incident options.

It should not automatically turn every poor recovery result into a persistent
Finding; incident state and preventative Finding are separate concepts.

---

## 13. Cross-platform fixtures required for any taxonomy widening

At minimum:

```text
one confirmed path
two independent paths
two paths / one confirmed FailureDomain
suspected shared domain only
confirmed recovery cycle
proposal-only fallback
stale under governed freshness policy
unknown criticality
done-but-unverified action
verification failed
resolved finding
duplicate evidence
graphRevision change
truncated minimal-cut enumeration
```

Negative:

```text
Proposal creates confirmed critical Finding      FORBIDDEN
edge count becomes independence                  FORBIDDEN
unknown becomes safe                             FORBIDDEN
confidence percentage becomes severity           FORBIDDEN
dismiss UI rewrites Reality                      FORBIDDEN
Preview fixture enters Production Findings       FORBIDDEN
```

---

## 14. Stop line

```text
V0_3_FINDING_TAXONOMY = FROZEN_SEVEN
FINDING_CONSUMER_GRAMMAR = COMPLETE
V2_3_EXTENDED_CONCEPT_MAPPING = COMPLETE
SEVERITY_DERIVATION_DESIGN = COMPLETE

PRODUCTION_SUPPORTED_CLASSES = PARTIAL_AUTHORITY
ADVANCED_FINDING_RUNTIME = HOLD_UNTIL_GOVERNED_INPUTS

FINDING_SCORE = FORBIDDEN
```
