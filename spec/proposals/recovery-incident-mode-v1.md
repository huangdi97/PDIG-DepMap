# Recovery Incident Mode v1 — Solver & Canonical Proposal

> Date: 2026-10-09  
> Status: **DESIGN_COMPLETE / SOLVER_PROPOSAL / NOT_IMPLEMENTED**
>
> Recovery Mode is a post-incident continuity query:
>
> > **“某些东西已经失去/不可用了，我现在还剩哪些可行恢复路径？”**
>
> It is not pre-change Impact, not a safety score, and not an emergency-themed UI
> invented before the solver exists.

## 1. Recovery Mode vs Impact

Impact asks:

> “If I change/remove X, what would be affected?”

Recovery asks:

> “X is already unavailable. What surviving, independently viable options remain?”

Therefore:

```text
Impact(input = hypothetical change)
!=
Recovery(input = explicit current incident state)
```

The two may reuse graph primitives but cannot share a fake result model.

## 2. Existing PDIG primitives to reuse

The repository already has:

- confirmed Dependency graph;
- FailureDomain + path independence;
- RecoveryCycle detection;
- ProviderPolicy;
- Impact;
- ChangePlan / verification semantics.

Recovery Mode must compose these rather than recreate them in UI code.

Permanent rules inherited from current Canonical vNext:

```text
pathCount != independentPathCount
recovery use != unique recovery
proposal/candidate != confirmed Reality
provider supports X != user configured X
potential cycle != confirmed cycle
retired edge != active edge
unknown != safe
```

## 3. Explicit Incident Context

Recovery requires a governed runtime input describing what is currently unavailable.

Conceptual:

```text
RecoveryIncident
  id
  state
  unavailableObjectRefs[]
  unavailableFailureDomainRefs[]
  degradedObjectRefs[]
  observedAt
  source
  evidenceRefs[]
  notes?
  createdAt
  updatedAt
```

State:

```text
draft
active
resolved
cancelled
```

A draft is a what-if preparation surface.
An active incident claims current unavailability and therefore requires explicit
user/runtime authority.

Do not infer an active incident from:
- no recent device activity;
- a failed login;
- stale data;
- a user opening Recovery UI.

## 4. Availability state

For each relevant factor/object:

```text
AVAILABLE_CONFIRMED
UNAVAILABLE_CONFIRMED
DEGRADED
UNKNOWN
```

Absence from the incident is **UNKNOWN / not declared**, not automatically available.

This prevents a recovery solver from treating unobserved factors as surviving roots.

## 5. Solver input

Conceptual:

```text
RecoveryQuery
  targetNodeId
  capability
  incident
  confirmedDependencies
  failureDomains
  recoveryCycleResult
  providerPolicies
  currentEvidence
  currentTime
```

Capabilities remain separate:

```text
access
authentication
recovery
communication
payment (only where recovery semantics are explicitly meaningful)
```

Do not merge mixed capabilities into one path count.

## 6. Solver output

```text
RecoveryResult
  targetNodeId
  capability
  status
  survivingRoots[]
  viablePaths[]
  blockedPaths[]
  independentPathCount?
  confirmedCycles[]
  potentialCycles[]
  unresolvedAssumptions[]
  providerConstraints[]
  nextSafeActions[]
  evidenceRefs[]
```

Result status:

```text
VIABLE
DEGRADED
BLOCKED
UNKNOWN
```

No percentage.

No “92% recoverable.”

## 7. Surviving root

A surviving root is an entry factor that:

1. is part of a confirmed relevant path;
2. is not explicitly unavailable;
3. is not inside an unavailable confirmed FailureDomain;
4. is not invalidated by a confirmed RecoveryCycle that makes the path unusable as
   an independent root;
5. satisfies known provider/time constraints;
6. has enough current evidence to be considered usable.

Conceptual:

```text
RecoveryRoot
  objectRef
  capability
  availability
  failureDomainRefs[]
  evidenceState
  providerConstraintState
```

## 8. Path viability

A path can be:

```text
VIABLE_CONFIRMED
BLOCKED_BY_INCIDENT
BLOCKED_BY_FAILURE_DOMAIN
BLOCKED_BY_PROVIDER_CONSTRAINT
CYCLE_DEPENDENT
NEEDS_REVIEW
UNKNOWN
```

A path that contains any unknown critical prerequisite cannot be silently labeled
viable.

## 9. Independence

Use the existing FailureDomain engine.

Example:

```text
SMS to phone A
app approval on phone A
```

may be two methods but one phone failure domain.

Result:

```text
pathCount = 2
independentPathCount = 1
```

A second authenticator on a genuinely separate device/provider may increase
independence only when the corresponding failure domains are confirmed.

## 10. Recovery cycles

Existing RecoveryCycle semantics remain:

```text
confirmed active edges → confirmed_cycle
proposal/candidate hint → potential_cycle
```

Recovery UI should translate:

```text
confirmed_cycle
→ 这条恢复链会回到自身，不能作为独立恢复根

potential_cycle
→ 可能存在恢复循环，需要核对

no_cycle
→ 当前已确认范围内未发现恢复循环
```

`no_cycle` still does not imply globally safe.

## 11. Provider constraints

ProviderPolicy is knowledge, not user Reality.

Examples:
- waiting period;
- recovery contact support;
- authenticator replacement rules;
- identity re-proofing availability.

Policy can:
- explain;
- constrain a plan;
- trigger revalidation;
- produce a suggestion.

Policy cannot:
- assert the user configured a recovery contact;
- create a Dependency;
- mark a factor available.

## 12. Recovery actions

Recovery Mode should prefer reversible, evidence-preserving actions.

Conceptual actions:

```text
use surviving authenticator
use saved recovery code
use previously established recovery contact
re-run identity proofing
contact provider/support
revoke compromised authenticator
bind replacement authenticator
verify recovered access
review recovery factors
invalidate compromised sessions (where provider supports it)
```

PDIG does not execute provider recovery operations unless a governed Provider/action
connector exists.

Otherwise it provides a verified checklist/plan.

## 13. Security research alignment

External guidance reinforces these design constraints:

- NIST SP 800-63B-4 defines account recovery as regaining access after losing
  control of needed authenticators and recognizes saved codes, issued codes,
  recovery contacts and repeated identity proofing. It also requires recovery
  notifications.
- FIDO recommends multiple authenticators as a way to reduce dependence on
  recovery and otherwise re-running identity proofing/onboarding.
- OWASP warns that compromise recovery should not rely solely on recently changed
  recovery information and recommends reviewing/revoking compromised recovery
  methods/sessions.

References:
- https://csrc.nist.gov/pubs/sp/800/63/B/4/final
- https://fidoalliance.org/white-paper-multiple-authenticators-for-reducing-account-recovery-needs-for-fido-enabled-consumer-accounts/
- https://cheatsheetseries.owasp.org/cheatsheets/Forgot_Password_Cheat_Sheet.html

These references guide security posture. PDIG's graph/failure-domain semantics remain
its own governed model.

## 14. Consumer UI grammar

Recovery is contextual; it is **not a sixth primary tab**.

Primary IA stays:

```text
现在 / 基础设施 / 变更 / 记录 / 我
```

Future entry points, only after solver support:

### Object detail
```text
“我已经失去这个设备/号码”
```

### Active incident
Now may show an incident card when a confirmed RecoveryIncident is active.

### Me
May expose “恢复准备” as configuration/readiness, not active incident mode.

## 15. Recovery screen hierarchy

When supported:

```text
Incident summary
→ What is unavailable
→ What is still confirmed available
→ Viable recovery roots
→ blocked/cyclic paths
→ provider/time constraints
→ next actions
→ verification/evidence
```

Do not lead with a generic red “Emergency” screen.

The primary visual object is the surviving recovery structure and the next safe action.

## 16. Truth language

Allowed:

```text
已确认可用
已确认不可用
需要核对
当前无法判断
共享同一故障点
存在恢复循环
等待服务商时间窗口
```

Forbidden:

```text
绝对安全
恢复成功率 90%
有两种方法所以有两条独立路径
没有记录所以不可恢复
服务商支持所以你已经配置
```

## 17. Incident resolution

Resolution is an explicit event.

```text
active
→ recovery action(s)
→ verification
→ resolved
```

Resolved requires evidence that the target capability is restored.

Closing the screen does not resolve the incident.

## 18. Recovery and Change

After access is restored, Recovery may generate/enter a ChangePlan to harden the
system:

```text
recover access
→ verify
→ replace compromised factor
→ add independent backup
→ retire unsafe old factor
```

Recovery result is not itself a ChangePlan.

## 19. Privacy / secrets

PDIG may track that a recovery code **exists**, where it is stored conceptually, and
its freshness/verification metadata.

It must not store the actual recovery secret in ordinary graph fields.

Forbidden in normal PDIG storage/logs:
- recovery code plaintext;
- passwords;
- OTP seeds;
- private keys;
- session tokens.

## 20. Conformance fixtures

Minimum:

```text
RI-01 one confirmed surviving root
RI-02 two paths same phone failure domain => independent 1
RI-03 two confirmed separate device domains => independent 2
RI-04 unavailable domain blocks all member paths
RI-05 proposal edge cannot create viable confirmed path
RI-06 potential recovery cycle => needs review
RI-07 confirmed cycle cannot count as independent root
RI-08 retired edge excluded
RI-09 mixed capability excluded
RI-10 provider supports but user not configured => not viable
RI-11 provider policy needs_review => constraint unknown
RI-12 unknown availability does not become available
RI-13 active incident requires explicit authority
RI-14 resolved requires verification evidence
RI-15 secret payload rejected from normal metadata
RI-16 deterministic result ordering
```

## 21. Cross-platform

Shared:
- incident model;
- availability states;
- solver;
- failure-domain semantics;
- cycle semantics;
- provider constraints;
- action/verification states.

Platform translated:
- visual graph/list;
- incident banner;
- step layout;
- gestures/navigation.

The solver must live below platform UI and pass cross-platform conformance.

## 22. Implementation order

```text
proposal review
→ Incident schema/state-machine
→ solver contract
→ fixtures
→ TS reference implementation
→ Android/iOS/Harmony ports
→ conformance
→ production read model
→ hidden Android screen
→ runtime/security tests
→ visible Recovery entry
```

Until those gates pass:

```text
RECOVERY_INCIDENT_DESIGN = COMPLETE
RECOVERY_SOLVER = NOT_IMPLEMENTED
RECOVERY_VISIBLE_UI = FORBIDDEN
```
