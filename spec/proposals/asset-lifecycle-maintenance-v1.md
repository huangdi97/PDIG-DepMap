# Asset Lifecycle & Maintenance v1 — Canonical Proposal

> Status: **PROPOSED_SCHEMA / NOT_IMPLEMENTED**
>
> Date: 2026-10-09
>
> This proposal exists because R18/R19 proved a real consumer need for card/number
> lifecycle maintenance, but current Canonical semantics do not yet define those
> fields. It does **not** modify the current `.depmap` format or runtime schema.
>
> Required implementation order remains:
>
> ```text
> Spec decision
> → schema/version allocation
> → migration
> → golden + negative fixtures
> → conformance expected results
> → Android / iOS / Harmony / Desktop
> → production UI
> ```

## 1. Problem

Personal digital infrastructure is not static.

Examples:

```text
card expires
annual fee renews
statement closes
payment is due
phone plan renews
keep-number action is due
provider waiting period ends
recovery information becomes stale
```

Today PDIG has:
- Node / Dependency / Evidence / ChangePlan;
- Timeline for currently recognized semantics;
- provider-aware change architecture;
- UI reference lifecycle fields.

What is missing is a portable semantic layer for **maintenance facts and recurring
obligations**.

The solution must not turn PDIG into:
- a personal-finance ledger;
- a telecom carrier client;
- an automatic bill-payment engine;
- a rewards optimizer.

## 2. Core distinction

Three different things must remain separate:

```text
A. Recorded personal fact
   “My card statement closes on the 18th.”

B. Provider policy
   “Provider X usually gives N days to pay.”

C. Derived occurrence
   “Your next recorded statement checkpoint is 2026-11-18.”
```

A belongs to Personal Reality after confirmation.

B belongs to the Provider Knowledge Plane.

C is a projection and can be recomputed.

Provider policy must never silently create A.

## 3. Proposed objects

### 3.1 MaintenanceFact

A typed, confirmed fact attached to a node.

Conceptual shape:

```text
MaintenanceFact
  id
  nodeId
  kind
  value
  valueType
  state
  evidenceRefs[]
  source
  confirmedAt
  validFrom?
  validUntil?
  updatedAt
```

### 3.2 MaintenanceSchedule

A schedule that can produce future checkpoints.

```text
MaintenanceSchedule
  id
  nodeId
  kind
  cadence
  nextDue
  timezone?
  graceWindow?
  completionMode
  state
  evidenceRefs[]
  confirmedAt
  lastCompletedAt?
  updatedAt
```

### 3.3 MaintenanceOccurrence

A derived timeline item.

Prefer derived/rebuildable occurrence over permanent duplication:

```text
MaintenanceOccurrence
  scheduleId
  nodeId
  kind
  dueAt
  windowStart?
  windowEnd?
  state
```

Unless later evidence requires an occurrence audit trail, the durable truth remains
the schedule + completion events.

## 4. Proposed fact vocabulary

### Card facts

```text
CARD_ANNUAL_FEE_AMOUNT
CARD_ANNUAL_FEE_CURRENCY
CARD_BILLING_DAY
CARD_PAYMENT_DUE_DAY
CARD_AUTOPAY_MODE
CARD_INSTALLMENT_SUMMARY
```

### Number facts

```text
NUMBER_BILLING_MODE
NUMBER_PLAN_COST
NUMBER_PLAN_CURRENCY
NUMBER_KEEP_ALIVE_CYCLE
NUMBER_RENEWAL_METHOD
```

### Important boundary

`CARD_INSTALLMENT_SUMMARY` is intentionally a **summary/reference fact** in v1.

PDIG must not store:
- transaction ledger;
- statement line items;
- interest calculation;
- amortization schedule;
- minimum payment calculation;

unless a future product decision explicitly expands the scope.

## 5. Proposed schedule vocabulary

```text
CARD_EXPIRY
CARD_ANNUAL_FEE_CHECKPOINT
CARD_BILLING_CHECKPOINT
CARD_PAYMENT_DUE_CHECKPOINT
NUMBER_KEEP_ALIVE
NUMBER_PLAN_RENEWAL
PROVIDER_WAIT_WINDOW_END
FACT_FRESHNESS_REVIEW
CUSTOM_MAINTENANCE
```

`CARD_EXPIRY` may bridge the existing `expiryDate` semantics rather than duplicate
it; the final schema decision must choose one canonical owner.

## 6. Cadence model

Avoid provider-specific strings in the schedule engine.

Proposed cadence union:

```text
OneTime(date/time)
MonthlyDay(dayOfMonth, overflowPolicy)
YearlyMonthDay(month, day, overflowPolicy)
IntervalDays(days, anchorDate)
ProviderWindow(policyRef, userConfirmedAnchor)
ManualOnly
```

`overflowPolicy`:

```text
CLAMP_TO_LAST_DAY
SKIP_OCCURRENCE
USER_CONFIRM
```

No schedule should silently invent a date when the rule is ambiguous.

## 7. State model

```text
ACTIVE
PAUSED
NEEDS_REVIEW
STALE
RETIRED
```

Occurrence state:

```text
UPCOMING
DUE
OVERDUE
COMPLETED
SKIPPED
UNKNOWN
```

Important:

```text
time passed != completed
notification delivered != completed
user opened app != completed
```

Completion requires an explicit event or governed verification.

## 8. Evidence / governance

Automatic source ingestion must follow:

```text
Observation
→ Proposal
→ Review
→ Confirm
→ MaintenanceFact / Schedule
```

Examples of candidate sources:
- statement;
- provider export;
- email;
- receipt;
- notification metadata;
- manual input.

AI may extract:
```text
“annual fee appears to be HK$1,800”
```

AI cannot write:
```text
annualFee = HK$1,800 CONFIRMED
```

without the normal authority transition.

## 9. Provider Knowledge Plane

Provider rule example:

```text
providerPolicy:
  provider = X
  productFamily = Y
  rule = “keep-alive activity every 90 days”
  effectiveRevision = ...
  officialSource = ...
```

User reality example:

```text
maintenanceSchedule:
  node = my-number
  kind = NUMBER_KEEP_ALIVE
  cadence = IntervalDays(90, 2026-08-07)
  source = USER_CONFIRMED
```

The provider rule can inform a Proposal. It does not become the user's schedule by
itself.

## 10. Timeline integration

The Timeline may project maintenance occurrences together with existing events.

Consumer grouping:

```text
今天
未来 7 天
未来 30 天
更晚
待核对
```

Each item should explain why it exists:

```text
美国保号
11 月 5 日需要完成保号操作
依据：你在 8 月 7 日确认的 90 天周期
```

Never show a due date without an explainable basis.

## 11. Attention integration

Maintenance can produce attention, but severity is not inferred from time alone.

Examples:

```text
annual fee checkpoint soon
→ informational/warning depending on user policy

keep-alive due soon
→ warning if loss would affect confirmed dependencies

expired maintenance fact
→ needs review
```

A due keep-number with zero known dependencies is not automatically “critical.”
A due keep-number that is an explicitly confirmed unique recovery path may be critical.

Severity should combine:

```text
maintenance urgency
× confirmed dependency impact
× path independence
× unknown scope
```

without collapsing into a single safety score.

## 12. Change integration

Maintenance is often the trigger for a Change scenario.

Examples:

```text
card expiry
→ replace payment card

number keep-alive no longer worth maintaining
→ analyze retirement / replace phone

provider migration deadline
→ provider-specific change plan
```

The schedule can suggest an entry point. It cannot auto-create a required ChangePlan
unless the normal Reality/Impact rules permit it.

## 13. UI mapping

R19 reference fields map to the proposal as follows:

| R19 field | Proposed semantic owner |
| --- | --- |
| annualFee | CARD_ANNUAL_FEE_AMOUNT + currency |
| annualFeeDue | CARD_ANNUAL_FEE_CHECKPOINT |
| billingDay | CARD_BILLING_DAY |
| paymentDueDay | CARD_PAYMENT_DUE_DAY |
| installmentSummary | CARD_INSTALLMENT_SUMMARY |
| autoPaySummary | CARD_AUTOPAY_MODE |
| billingMode | NUMBER_BILLING_MODE |
| planCost | NUMBER_PLAN_COST + currency |
| keepAliveDue | NUMBER_KEEP_ALIVE occurrence |
| keepAliveCycle | NUMBER_KEEP_ALIVE cadence |
| lastKeepAlive | explicit completion event |
| renewalMethod | NUMBER_RENEWAL_METHOD |

This table is a design mapping, not current persisted truth.

## 14. Cross-platform requirements

Any implementation must pass equivalent semantics on:
- Android;
- iOS;
- HarmonyOS;
- Desktop;
- legacy/conformance oracle where applicable.

Minimum fixtures:
- missing fact;
- explicit free annual fee;
- unknown annual fee;
- monthly day 31 in February;
- 90-day keep-alive interval;
- provider window revision;
- due but not completed;
- completed then next occurrence;
- stale fact;
- proposal not confirmed;
- unique-recovery keep-number becomes urgent;
- recovery-use but non-unique remains non-critical;
- timezone/date-boundary cases.

## 15. Migration / compatibility

No current payload version is assigned by this proposal.

Rules:
- older files open without lifecycle data;
- missing lifecycle = unknown, not default;
- migration must be reversible at the semantic level or preserve old file export;
- unknown fields must not be silently discarded by another platform;
- lifecycle support cannot break frozen v0.3 behavior.

## 16. Security / privacy

Lifecycle metadata can be sensitive:
- bill timing;
- card fee;
- phone renewal behavior;
- carrier;
- recovery usage.

Therefore:
- local-first storage;
- encrypted at rest with existing product protections;
- no analytics payload;
- masking rules where identifiers are involved;
- no secret/PIN/password/recovery-code storage;
- exported .depmap semantics must be explicit if/when lifecycle enters the format.

## 17. Decision required before implementation

The product/schema review must decide:

1. Is `MaintenanceFact/Schedule` first-class, or represented as typed Node facts?
2. Does `expiryDate` migrate into the schedule engine or stay dual-owned?
3. Is `CARD_INSTALLMENT_SUMMARY` in scope for production?
4. Which maintenance facts are user-editable vs source-proposed?
5. Which schedule kinds can generate Attention?
6. Which lifecycle facts export in .depmap?

Until that decision:

```text
R19 lifecycle production persistence = NOT_IMPLEMENTED
reference UI semantics = VALID
ad-hoc Android persistence = FORBIDDEN
```
