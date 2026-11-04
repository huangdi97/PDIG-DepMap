# Continuity Analysis & Product Modes v1

> Date: 2026-10-10  
> Status: **DESIGN_FROZEN / PARTIAL_RUNTIME / PROPOSED_ANALYSIS_EXTENSION**
>
> This contract closes the v2.3-R1 design gap between existing Impact /
> FailureDomain / RecoveryCycle / TemporalChange primitives and the long-term
> consumer product modes:
>
> ```text
> Prepare / Change / Recover / Maintain
> ```
>
> It does **not** claim Minimal Cut Set or generic Incident Recovery are already
> implemented.

---

## 1. Product question, not graph visualization

PDIG continuity analysis exists to answer four consumer questions:

```text
Prepare
“If I change this, what needs attention before I start?”

Change
“What is happening now, what is blocked, and what must be verified next?”

Recover
“Something is already unavailable. What independent roots still survive?”

Maintain
“What should I repair or re-confirm before an incident/change happens?”
```

These are modes/lenses over the same governed Reality. They are not new primary
navigation destinations.

The permanent shell remains:

```text
现在 / 基础设施 / 变更 / 记录 / 我
```

Mode entry is contextual:
- Object Detail / Impact Lens → Prepare;
- Change Center / Plan → Change;
- future Incident entry → Recover;
- Now / Weaknesses / Timeline / Review → Maintain.

---

## 2. Existing authoritative primitives

Current runtime primitives already exist and remain authoritative where applicable:

```text
Impact Kernel
FailureDomain / computePathIndependence
RecoveryCycle
Infrastructure Findings
TemporalChange
ProviderPolicy
ChangePlan / Action prerequisite DAG
Verification
Evidence / Proposal / Human Confirmation
```

This proposal composes those primitives; UI must not duplicate their logic.

---

## 3. Continuity state key

Every analysis is capability-aware.

Minimum state identity:

```text
(nodeId, capability)
```

Never:

```text
nodeId only
```

One node can lose one capability while retaining another.

Examples:

```text
(phone-1, communication) unavailable
(phone-1, recovery) still unknown
(account-1, access) degraded
```

---

## 4. Blast Radius

### 4.1 Definition

Blast Radius is the explainable result of removing or degrading one or more
**confirmed Reality states** and propagating consequences through typed,
capability-aware dependencies.

Conceptual input:

```text
BlastRadiusInput
  initialUnavailable: Set<StateKey>
  graphRevision
  effectiveAt?
  failureDomainRemoval?
  providerPolicyRevisionRefs?
```

Conceptual output:

```text
BlastRadiusResult
  initialUnavailable
  mustChange
  degraded
  backupPath
  needsReview
  unaffectedKnownScope
  unknownScope
  checklist
  evidenceRefs
  graphRevision
```

### 4.2 Existing implementation relationship

Current `ImpactKernel.simulateScenario` is already a blast-radius primitive for
the capabilities/scenarios it supports.

Therefore generic Blast Radius must extend or wrap the same deterministic kernel;
it must not create a parallel UI-side traversal.

### 4.3 Failure-domain removal

A future blast scenario may remove a confirmed FailureDomain rather than one node:

```text
disable PHONE_NUMBER domain
disable DEVICE domain
disable PROVIDER domain
disable ACCOUNT domain
```

The domain removal expands to affected state keys using **confirmed** domain
membership.

Suspected / needs-review domain membership may only produce:

```text
needs_review
```

It cannot produce deterministic `must_change`.

### 4.4 UI language

Internal:

```text
Blast Radius
```

Consumer:

```text
影响范围
如果它发生变化？
如果这个设备不可用？
```

Do not expose graph-theory vocabulary unless the user explicitly asks for
technical details.

---

## 5. Minimal Cut Set

### 5.1 Consumer meaning

A Minimal Cut Set answers:

> “Which smallest confirmed combination would block this capability?”

It does **not** mean:
- “top 3 most connected nodes”;
- “nodes with highest degree”;
- “all nodes on one path”;
- “two edges therefore two independent failures”.

### 5.2 Formal target

For target state:

```text
T = (nodeId, capability)
```

A cut candidate `C` is valid only when disabling all members of `C` makes T
unavailable under the same authoritative Impact semantics.

Minimal means:

```text
C blocks T
AND
no proper subset of C blocks T
```

### 5.3 Candidate units

Cut units may be:

```text
StateKey
confirmed FailureDomain
```

Do not mix a node and its containing confirmed failure domain as if they were
independent units.

### 5.4 Independence semantics

Path count and cut size must be FailureDomain-aware.

Example:

```text
SMS on phone A
Authenticator app on same phone A

raw paths = 2
independent failure roots may still = 1
```

Any cut-set implementation must consume the same FailureDomain ownership used by
`computePathIndependence`.

### 5.5 Unknown data

Unknown/unconfirmed relations create uncertainty, not certainty.

Outputs require:

```text
confirmedMinimalCuts
potentialCutsNeedsReview
unknownCoverage
```

A confirmed cut cannot include a Proposal/Candidate as if it were Reality.

### 5.6 Algorithm design

For consumer-sized personal graphs, deterministic bounded enumeration is preferred
over opaque scoring.

Proposed algorithm:

```text
1. Select target StateKey.
2. Build confirmed relevant subgraph for target capability.
3. Collapse confirmed shared FailureDomains into failure units.
4. Enumerate candidate failure-unit sets by increasing cardinality.
5. For each candidate:
     run authoritative scenario simulation
     if target unavailable:
       reject if an already accepted proper subset exists
       otherwise accept as minimal
6. Stop when:
     maxCutSize reached
     OR deterministic candidate budget reached.
7. Record truncation explicitly.
```

Required guardrails:

```text
maxCutSize default <= 4
candidate budget finite
stable lexicographic ordering
graphRevision pinned
no random sampling for authoritative output
truncated != complete
```

If budget is hit:

```text
coverage = PARTIAL
```

Never display “no critical combination” from a truncated search.

---

## 6. Prepare mode

### Entry

```text
Object Detail
→ Impact Lens
→ choose supported ChangePrimitive
→ Prepare
```

Prepare shows:

```text
target
confirmed impact
unknowns / needs review
required prerequisites
provider timing/constraints
independent recovery status
Make-Before-Break gate
plan readiness
```

Prepare does not mutate Reality merely by opening it.

A ChangePlan may be created only through the authoritative Scenario/Change
gateway.

---

## 7. Change mode

Change is the execution phase for an authoritative plan.

Required state:

```text
Current
Transition
After = Plan Projection
```

Execution truths:

```text
done != verified
plan projection != reality
blocked action != completed action
waiting window != failure
provider rule != user configuration
```

Change UI reads:
- Action DAG;
- readiness;
- graph revision;
- TemporalChange;
- verification state/evidence;
- ProviderPolicy where relevant.

---

## 8. Recover mode

Recover begins after a failure is already real.

Conceptual input:

```text
confirmedUnavailable
suspectedUnavailable
remaining access/recovery factors
confirmed FailureDomains
current graphRevision
provider constraints
```

Conceptual flow:

```text
Failure already happened
→ unavailable capabilities
→ surviving independent roots
→ Recovery Solver
→ Restore / Rotate / Verify
```

Recover is future-gated by the existing Recovery Incident proposal.

### Hard rule

No “best recovery route” may be produced from:
- unconfirmed Proposal edges;
- path count alone;
- provider capability alone;
- a secret value stored in PDIG.

SecretLocator may point to where a secret exists; PDIG does not become secret
escrow.

---

## 9. Maintain mode

Maintain is continuous preventative work.

Inputs:

```text
Infrastructure Findings
Timeline
pending Review
Drift
Freshness
pending Verification
Maintenance schedules (future Canonical)
ProviderPolicy revalidation
```

Consumer findings may include:

```text
single confirmed recovery source
shared confirmed FailureDomain
confirmed RecoveryCycle
unconfirmed fallback
stale recovery information
unknown critical path
pending verification
maintenance due
provider rule needs revalidation
```

Maintain must not collapse this into one global health score.

---

## 10. Temporal Change

Current runtime already has a `TemporalChangeWindow` with ordered timestamps:

```text
effectiveAt
verificationNotBefore
verificationDueAt
retireOldPathAfter
```

Existing invariant:

```text
effectiveAt
<= verificationNotBefore
<= verificationDueAt
<= retireOldPathAfter
```

and retirement remains blocked until required new paths are verified.

Consumer temporal vocabulary may expand beyond BEFORE / TRANSITION / AFTER to
explain reasons:

```text
等待生效
可开始验证
验证窗口中
等待服务商
受限 / restricted
验证逾期
可停用旧路径
仍被 Make-Before-Break 阻断
```

These labels are projections over authoritative timestamps/policy; they are not a
new Reality state written by the UI.

---

## 11. Provider Knowledge revision

Current `ProviderPolicy` already separates provider capability from personal
configuration and carries provenance/revision/effective windows.

Permanent rule:

```text
Provider supports X
!=
User configured X
```

Policy revision may:
- change explanation;
- change suggestion;
- change plan template;
- trigger `needs_revalidation`.

Policy revision may not:
- create a Dependency;
- confirm a phone/email;
- mark a relationship required;
- rewrite Personal Reality.

UI must show stale/unverifiable provider rules as:

```text
需要重新确认
```

not as current fact.

---

## 12. Evidence ingestion / freshness

The existing evidence plane already enforces:

```text
Observation
→ Proposal
→ Review
→ Confirm
→ Reality
```

Multiple evidence sources increase confidence/context but never upgrade authority.

Freshness must be represented per source/fact type.

Conceptual freshness states:

```text
fresh
aging
stale
unknown
```

Freshness is **not** truth value.

A stale confirmed relationship remains a historical confirmed fact that may need
revalidation; the UI must not silently retire it based solely on age.

Automatic discovery can create:
- Observation;
- Proposal;
- Candidate;
- Drift;
- needs-review freshness finding.

It cannot directly create confirmed Reality.

---

## 13. Cross-platform control-surface independence

Cross-platform is not just UI coverage.

A continuity system should reduce the chance that one failed control surface blocks
all recovery work.

Long-term requirement:

```text
Desktop / Android / iOS / Harmony clients
share Canonical semantics
but should not require the same device/failure domain for every recovery operation.
```

Examples:
- phone lost → desktop can still inspect plan / recovery references;
- desktop unavailable → mobile can still inspect critical continuity facts;
- one platform lacks a provider-specific UX → Canonical data remains portable.

This does **not** imply cloud sync. Local-first transport/export remains a separate
architecture question.

---

## 14. UI placement

No sixth primary tab is created.

```text
Now
  Maintain summaries / attention / verification

Infrastructure
  Object detail → Impact Lens → Prepare

Change
  Prepare + active Change plans

Records
  verification / occurred history

Me
  personal control / privacy / sources / preparedness
```

Future Recover entry should appear contextually when an incident exists or the user
explicitly starts recovery, not as a permanent top-level tab unless a later product
decision revises IA.

---

## 15. Machine-readable/runtime implementation gates

### Existing / runtime-backed

```text
Impact Kernel                         IMPLEMENTED
FailureDomain independence           IMPLEMENTED
RecoveryCycle                        IMPLEMENTED
Infrastructure Findings              PARTIAL/IMPLEMENTED CLASSES
TemporalChange                       IMPLEMENTED
ProviderPolicy boundary              IMPLEMENTED
ChangePlan + Action DAG              IMPLEMENTED
Verification                         IMPLEMENTED
Multi-source Evidence                IMPLEMENTED
```

### Proposed extension

```text
Generic capability-aware Blast Radius          PARTIAL (existing Impact primitive)
FailureDomain-removal Blast Radius             PROPOSED
Minimal Cut Set                                PROPOSED
Generic Recover Solver                         PROPOSED / gated by Recovery Incident
Freshness-driven Maintain integration          PARTIAL / future Canonical additions
```

Implementation order:

```text
Master
→ machine-readable spec
→ schema/migration only if persistence changes
→ fixtures/expected
→ old conformance regression unchanged
→ TS/core
→ Android/Desktop
→ Harmony/iOS
→ runtime/E2E
→ consumer UI
```

No Android-only authoritative algorithm.

---

## 16. Required fixtures for Minimal Cut / generic Blast

At minimum:

```text
one confirmed path
two truly independent paths
two raw paths sharing one confirmed FailureDomain
suspected shared domain only
confirmed RecoveryCycle
proposal-only fallback
mixed capability graph
node with payment + recovery capabilities
failure-domain removal
candidate-budget truncation
deterministic ordering
graphRevision change / stale result
```

Negative fixture requirements:

```text
Proposal creates confirmed cut                    FORBIDDEN
pathCount displayed as independentPathCount       FORBIDDEN
truncated enumeration displayed as complete       FORBIDDEN
ProviderPolicy writes Reality                     FORBIDDEN
stale evidence auto-retires confirmed relation    FORBIDDEN
After Projection displayed as executed Reality    FORBIDDEN
```

---

## 17. Acceptance vocabulary

Internal → consumer:

```text
Blast Radius       → 影响范围
Minimal Cut Set    → 会一起卡住你的关键组合
FailureDomain      → 共同故障点 / 共同依赖环境
Path Independence  → 独立备用路径
TemporalChange     → 等待 / 验证 / 可停用旧路径
ProviderPolicy     → 服务规则 / 平台要求
Freshness          → 最近确认 / 需要重新确认
```

The consumer UI should explain the basis and unknowns before exposing advanced
technical vocabulary.

---

## 18. Stop line

```text
PRODUCT_MODES_DESIGN = FROZEN
TEMPORAL_CHANGE_RUNTIME = IMPLEMENTED
PROVIDER_POLICY_RUNTIME = IMPLEMENTED
EVIDENCE_AUTHORITY_BOUNDARY = IMPLEMENTED

GENERIC_BLAST_RADIUS = PARTIAL
MINIMAL_CUT_SET = PROPOSED
RECOVER_SOLVER = PROPOSED

DESIGN_COMPLETE
!=
AUTHORITATIVE_RUNTIME
```
