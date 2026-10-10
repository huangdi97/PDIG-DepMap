# PDIG Human Review UX Contract

> Status: **DESIGN_FROZEN / R22**
>
> Scope: Observation / Proposal / Candidate / Drift → Human Confirmation.
>
> This contract does not add a primary tab and does not change Canonical schema.

## 1. Product role

PDIG's truth chain is:

```text
Source
→ Observation
→ Proposal / Candidate / Drift
→ Human Review
→ Confirmed Personal Reality
→ Impact / Change / Verification
```

The review surface exists to protect the authority boundary. It is not an
"inbox of notifications" and it is not an AI auto-cleanup tool.

Permanent rule:

```text
machine confidence != authority
visibility != confirmation
review screen open != confirmation
time passed != confirmation
```

## 2. Information architecture

Human Review is **not** a sixth primary destination.

Primary remains:

```text
现在 / 基础设施 / 变更 / 记录 / 我
```

Review is a focused secondary task owned by **现在** because it represents work
requiring current user attention.

Entry points:

```text
Now → 待复核
Data Sources → 人工确认 / 待复核
Search → 待复核
```

Hierarchy:

```text
Review → Up → Now
System Back → actual previously visited screen
```

## 3. Three review classes

### 3.1 Dependency Proposal

Meaning:

> Evidence suggests a possible relation between already known objects.

Before confirmation:
- not part of Confirmed Reality Graph;
- not used as a confirmed dependency by Impact;
- does not bump graphRevision.

Consumer decisions:

```text
确认关系
拒绝
```

### 3.2 Discovery Candidate

Meaning:

> Evidence suggests a possible new logical object.

Before confirmation:
- not a Node in Personal Reality;
- not used by Impact;
- does not bump graphRevision.

Consumer decisions:

```text
确认对象
忽略
```

### 3.3 Reality Drift

Meaning:

> New positive evidence is inconsistent with current Confirmed Reality or may
> indicate it changed.

Drift itself never mutates Reality.

Consumer decisions:

```text
已替换
两个都在用
没有变化
稍后确认
```

Absence-only evidence must not create Drift.

## 4. Consumer language

Use:

```text
待复核
关系建议
对象候选
现实漂移
依据
确认后会发生什么
系统发现了需要你确认的信息
```

Avoid exposing internal identifiers or requiring the user to understand:
- proposal IDs;
- candidate IDs;
- graphRevision mechanics;
- raw relation enum names;
- adapter internals.

Technical vocabulary may appear only in a small truth-boundary explanation where
it improves auditability.

## 5. Confidence

A numeric ML confidence is not a truth score.

The consumer surface may say:

```text
机器建议
待确认对象
现实漂移
已记录 N 次相关观察
```

It should not say:

```text
97% true
high confidence = safe to auto-confirm
```

If numeric confidence is shown in a future expert/debug view, it must be clearly
labeled as model/proposal confidence, never Reality certainty.

## 6. Evidence summary

Review items should explain the basis without copying secrets or raw sensitive
payloads.

Safe examples:
- source label;
- observation count;
- normalized statement/receipt description;
- last observed time when available;
- involved object display names.

Forbidden:
- passwords;
- recovery codes;
- full bank/card numbers;
- private keys;
- raw email/SMS bodies by default;
- hidden internal IDs as user-facing labels.

## 7. Preview vs Production

### Preview

Preview uses isolated synthetic items only to exercise hierarchy and visual truth.

It is read-only:

```text
Preview Review Item
→ display possible formal decisions
→ NO Reality mutation
```

No Preview control may call a production review gateway.

### Production

Production must bind:

```text
AppContainerVNextReviewSource
→ VNextProductionReviewQueue
→ ProductionReviewConsumerInbox
→ Review UI

user decision
→ AppContainerVNextReviewActionGateway
→ authoritative domain mutation
→ re-read Review queue + graph revision/snapshot
```

The UI must never remove an item locally and pretend the review succeeded.

## 8. Now boundary

R21 defines:

```text
Records = what happened / what was verified / evidence trace
```

Therefore pending attention, future maintenance and Human Review do not belong in
Records.

R22 rules:

```text
Now
  pending review
  current attention
  active changes
  upcoming maintenance

Records
  completed/verified/pending-verification trace
  actual recorded history/evidence
```

"查看全部" for active changes routes to **Change**, not Records.

Pending attention must not be hidden behind a link to Records. Compact UI should
show the actual current reference attention items or use a future dedicated Now
task view.

## 9. Empty state

When Review is empty:

> 当前没有待复核项。没有待复核项不代表所有关系都完整或最新。

Never:

> 全部已确认 / 一切正常 / 你的图是完整的

unless a future deterministic coverage contract can actually prove that stronger
claim.

## 10. Production action invariants

Proposal:
```text
acceptProposal / rejectProposal
```

Candidate:
```text
acceptCandidate / dismissCandidate
```

Drift:
```text
resolveDriftAsReplacement
resolveDriftAsAdditionalPath
dismissDrift
```

"稍后确认" is a UI/navigation choice: it performs no Reality mutation.

After any mutation:

```text
delegate to AppContainer
→ re-read queue
→ re-read relevant production snapshot
→ render authoritative result
```

## 11. Tests

Minimum source/runtime contracts:

- all three review classes can render;
- review route is secondary and does not alter five-primary IA;
- Review Up → Now;
- Search finds Review;
- Data Sources can enter Review;
- Preview items do not expose mutation controls as successful actions;
- production consumer projection keeps three classes distinct;
- unknown object names do not leak internal IDs;
- no proposal/candidate/drift is counted as confirmed dependency before review;
- runtime screenshot includes "发现 ≠ 事实";
- empty Review never claims graph completeness.

## 12. Stop line

```text
HUMAN_REVIEW_DESIGN = FROZEN
PREVIEW_REVIEW_REFERENCE = ALLOWED_READ_ONLY
PRODUCTION_REVIEW_SOURCE = IMPLEMENTED
PRODUCTION_REVIEW_GATEWAY = IMPLEMENTED
PRODUCTION_SCREEN_BINDING = GATED_UNTIL_VNEXT_CUTOVER
```
