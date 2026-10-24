# PDIG Asset Continuity UX Contract

> Status: **DESIGN_FROZEN / UI REFERENCE CONTRACT**
>
> Scope: Card / Number lifecycle facts + Object Impact Lens.
>
> This document does **not** change the Canonical schema, `.depmap` payload, provider
> knowledge, or PersonalReality. Production persistence requires the normal PDIG
> Spec → Schema → Fixture → Conformance → Runtime chain.

## 1. Why this contract exists

A card or phone number is not useful as a beautiful inventory object alone. A
consumer needs to understand both:

1. **what this asset is and when it needs maintenance**;
2. **what depends on it and what may happen when it changes**.

This keeps the UI aligned with the product architecture:

```text
Inventory
→ Dependency
→ Continuity
→ Change
```

External products validate the adjacent jobs but do not define PDIG truth:

- CardPointers exposes annual-fee reminders and card-renewal workflows:
  https://cardpointers.com/pro/
- KeepSim tracks number keep-alive cycles, due dates and explicit user-confirmed
  actions without taking over the carrier account:
  https://apps.apple.com/us/app/keepsim-keep-sim-esim-active/id6773044270
- QuanCard validates local-first card ownership/collection:
  https://quancard.app/en/

PDIG absorbs the useful interaction pattern, but its differentiation remains
typed dependency + continuity + change orchestration.

## 2. Card lifecycle reference fields

Reference UI may display these **recorded** fields:

```text
annualFee
annualFeeDue
billingDay
paymentDueDay
installmentSummary
autoPaySummary
expiry
```

Rules:

- missing = `未记录`, never zero / none / free unless explicitly recorded;
- annual fee does not imply rewards or benefit optimization;
- statement amounts, transactions, balance, minimum payment and actual debit are
  never inferred from lifecycle metadata;
- provider marketing rules cannot overwrite user reality;
- a future ingestion source may propose values, but proposals remain proposals
  until human confirmation.

Consumer hierarchy:

```text
Card Identity
→ Lifecycle
→ Confirmed Dependencies
→ Impact Lens
→ Change / Verification
```

## 3. Number lifecycle reference fields

Reference UI may display:

```text
billingMode
planCost
keepAliveDue
keepAliveCycle
lastKeepAlive
renewalMethod
```

Number roles:

```text
primary
secondary
keep
```

`keep` is a real consumer role, not a visual theme. A keep-number asset must not
be reused as the migration target in a Replace Phone flow.

Rules:

- due dates are recorded facts, not live carrier status;
- completing a telecom action must never be inferred because time passed;
- no automatic SMS/call/renewal is implied;
- missing due date must not render as “无需保号”;
- aliases are local user presentation names; blank alias falls back to the
  recorded/masked number.

Consumer hierarchy:

```text
Communication Identity
→ Lifecycle / Keep-alive
→ Authentication & Recovery Dependencies
→ Impact Lens
→ Change / Verification
```

## 4. Impact Lens

Every core object detail should answer:

> **如果它发生变化？**

The UI reference projects only evidence it actually has:

```text
confirmedDependencies
attentionFindings
criticalAccounts
uniqueRecoveryPath
independentAlternatives
unknownRelationsRemain
```

Truth rules:

- `criticalAccounts = null` means **未记录**, never 0;
- `independentAlternatives = null` means **未记录**, never “有备用”;
- `recoveryOnly=true` may confirm a positive recovery dependency;
- `recoveryOnly=false` is insufficient to prove there is no unique recovery path;
- unknown/unconfirmed relations remain visibly unknown;
- no health/safety score;
- no degree-based shortcut for independent path count.

Consumer vocabulary:

```text
已确认依赖
需要关注
唯一恢复路径：已确认 / 未知
关键账户：未记录
独立备用方式：未记录
未确认关系：仍可能存在
```

Do not expose internal vocabulary such as `FailureDomain`,
`PersonalReality`, `Canonical`, `Action DAG`, or `UiImpactTruth`.

## 5. Change CTA rule

A specific CTA such as:

```text
分析更换号码影响
开始更换
模拟失去
```

may appear only when the corresponding production ChangePrimitive / scenario is
actually supported.

Current Android reference:

- Replace Phone has a real continuity flow → Number Detail may enter it.
- Card Detail exposes the read-only Impact Lens but does not invent a new vNext
  card-change route.
- Future card-change vNext routing must bind the existing production payment
  scenario rather than creating a disconnected demo workflow.

## 6. Change target identity invariant

For Replace Phone:

```text
old active number
!=
long-lived keep-number asset
!=
new migration target
```

R19 reference IDs deliberately demonstrate the invariant:

```text
num-cn-1 = old active number
num-cn-3 = keep-number asset
num-cn-4 = migration target
```

The IDs themselves are fixture-only; the semantic separation is permanent.

## 7. Search and information density

Search should match:

- user-visible object name / alias;
- issuer / carrier / region;
- recorded lifecycle facts such as fee, billing day, keep-alive due date;
- consumer role labels such as “保号”.

Search must not generate inferred results.

List/Inspector surfaces may show a compact lifecycle line to increase useful
information density, but the detailed fact set remains in the focused object
detail.

## 8. Adaptive translation

Phone:

```text
identity
→ lifecycle card
→ dependencies
→ impact
→ task CTA
```

Tablet / Expanded:

```text
list + inspector
focused detail remains available
lifecycle visible in inspector/detail
Impact Lens remains a focused-detail semantic layer
```

Expanded layout must not become an admin-console KPI wall.

## 9. Production persistence boundary

R18/R19 lifecycle fixture values are synthetic reference evidence.

Before lifecycle metadata can become writable production truth:

```text
Product semantics freeze
→ Canonical proposal
→ schema version / migration decision
→ golden + negative fixtures
→ cross-platform conformance
→ Android repository binding
→ UI edit/review flow
→ runtime evidence
```

Until then:

```text
UI reference can render synthetic lifecycle facts
UI must not persist lifecycle facts into PresentationProfile
UI must not write them into .depmap through an ad-hoc side channel
```

This boundary is mandatory.
