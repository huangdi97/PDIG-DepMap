# ANDROID_UI_VNEXT_R22_SOURCE_REPORT

> 2026-10-10 · `feat/android-ui-vnext-translation`
>
> R22 closes the Human Confirmation gap on top of R21.
>
> R21 already closed:
> - five-primary IA;
> - core object details + Impact Lens;
> - Change Center;
> - Replace Phone / Replace Payment Card reference flows;
> - Records as evidence/verification trace;
> - production read model / review source / authoritative gateways.
>
> The remaining product-chain gap was that Proposal / Candidate / Drift existed in
> production APIs but had no consumer Human Review surface in UI vNext.

## 0. Truth status

```text
ANDROID_UI_VNEXT_SOURCE = R22

FIVE_PRIMARY_IA = PRESERVED
HUMAN_REVIEW_INBOX = IMPLEMENTED_SOURCE
HUMAN_REVIEW_NOW_ENTRY = IMPLEMENTED_SOURCE
HUMAN_REVIEW_SOURCE_ENTRY = IMPLEMENTED_SOURCE
HUMAN_REVIEW_SEARCH_ENTRY = IMPLEMENTED_SOURCE
HUMAN_REVIEW_PREVIEW = READ_ONLY_REFERENCE

PRODUCTION_REVIEW_SOURCE = SOURCE_IMPLEMENTED
PRODUCTION_REVIEW_CONSUMER_PROJECTION = SOURCE_IMPLEMENTED
PRODUCTION_REVIEW_ACTION_GATEWAY = SOURCE_IMPLEMENTED

NOW_RECORDS_BOUNDARY = REPAIRED_SOURCE
RECORDS_EVIDENCE_TRACE = PRESERVED

CANONICAL_SCHEMA_CHANGE = NONE
DEPMAP_PAYLOAD_CHANGE = NONE
NEW_PRIMARY_TAB = NONE

FRESH_R22_BUILD = NOT_RUN
FRESH_R22_UNIT_TESTS = NOT_RUN
FRESH_R22_INSTRUMENTATION = NOT_RUN
FRESH_R22_PHONE_PIXELS = NOT_RUN
FRESH_R22_TABLET_PIXELS = NOT_RUN
FRESH_R22_HUMAN_ACCEPTANCE = NOT_RUN

ANDROID_REFERENCE_FREEZE = HOLD
PRODUCTION_VNEXT_CUTOVER = HOLD
```

## 1. Why R22 is necessary

The v2.3 truth chain is:

```text
Evidence
→ Observation
→ Proposal / Candidate
→ Human Confirmation
→ Personal Reality
→ Impact / Change / Verification
```

Living Graph adds Drift:

```text
new positive evidence
→ Reality Drift
→ Human decision
→ optional Reality mutation
```

Without a Human Review surface, UI vNext could show Reality and Change but had no
consumer place for the authority transition between discovery and truth.

R22 closes that product-loop gap.

## 2. R22 Human Review Inbox

Route:

```text
/review
```

UI label:

```text
待复核
```

It is a secondary/focused screen, not a sixth primary destination.

Entry points:
- Now → 待复核;
- Data Sources → 人工确认 / 待复核;
- Search → 待复核.

Hierarchy:

```text
Review → Up → Now
System Back → actual previous screen
```

Five primary destinations remain:

```text
现在 / 基础设施 / 变更 / 记录 / 我
```

## 3. Three review classes

### Dependency Proposal

Consumer meaning:

> 系统建议确认一条关系。

Before confirmation:
- not a confirmed dependency;
- not authoritative Reality;
- must not be used as a confirmed Impact relation.

Formal choices:
- 确认关系;
- 拒绝.

### Discovery Candidate

Consumer meaning:

> 系统发现了一个可能的新基础设施对象。

Before confirmation:
- not a confirmed Node;
- not authoritative Reality.

Formal choices:
- 确认对象;
- 忽略.

### Reality Drift

Consumer meaning:

> 新的正向证据与当前已确认现实不一致，现实可能已经改变。

Drift itself does not modify the graph.

Formal choices:
- 已替换;
- 两个都在用;
- 没有变化;
- 稍后确认.

## 4. Preview authority boundary

R22 Preview uses:

`demo/UiVNextReviewReference.kt`

The synthetic reference includes one example of each review class so hierarchy and
copy can be tested.

It is intentionally read-only.

The page says:

> 发现 ≠ 事实

and:

> Preview 不执行

No Preview interaction:
- accepts a Proposal;
- creates a Candidate Node;
- resolves a Drift;
- bumps graphRevision;
- writes a dependency.

The action vocabulary is shown only to explain what the formal production decision
would be.

## 5. Production review seam

Already present before the screen:
- `AppContainerVNextReviewSource`;
- `AppContainerVNextReviewActionGateway`.

R22 adds:
- `ProductionReviewConsumerProjection.kt`.

Production projection converts raw queue rows into consumer-safe review items:
- relation proposal;
- object candidate;
- reality drift.

It intentionally:
- does not expose internal IDs as user labels;
- does not turn confidence into truth;
- translates raw relation/capability vocabulary;
- preserves observation count only as evidence context;
- keeps decisions explicit.

Production action rule remains:

```text
user decision
→ AppContainer authoritative mutation
→ re-read review queue
→ render authoritative state
```

Never:

```text
tap
→ remove local card
→ pretend Reality changed
```

## 6. Now boundary repaired

R21 made Records:

> what happened / what was verified / what evidence exists.

A source audit found stale R9/R8 Now links still used Records as a generic
“查看全部” destination for:
- current attention;
- active changes;
- upcoming future items.

That violated the new Records boundary.

R22 fixes:
- Human Review appears directly in Now;
- compact current attention is shown in Now instead of hiding extra items behind Records;
- active Change “查看全部” goes to Change Center;
- Upcoming remains a future/maintenance surface in Now and is not mislabeled as history.

Records remains historical/evidence-oriented.

## 7. Search

Search now recognizes:
- 待复核;
- 复核;
- 确认;
- 建议;
- proposal;
- candidate;
- drift.

Search opens the Review screen only. It never accepts anything.

## 8. Data Sources

Data Sources now exposes an explicit Human Confirmation entry.

This makes the Evidence plane understandable:

```text
Data / Source
→ what is recorded / unknown
→ Pending Review
→ Human confirmation
→ Reality
```

It does not claim provider sync that Preview does not have.

## 9. Tests / evidence source

Added:
- `HumanReviewR22Test`;
- `HumanReviewR22ContractTest`;
- production Review consumer-projection assertions;
- phone pixel journey for Human Review.

Pixel proof must include:
- 待复核;
- 发现 ≠ 事实;
- 关系建议;
- 对象候选;
- 现实漂移;
- Preview 不执行;
- Up → Now.

## 10. Product architecture after R22

The visible Android product loop is now source-complete as:

```text
Now
  ↓
Infrastructure identity
  ↓
Impact Lens
  ↓
Change
  ↓
Verification
  ↓
Records

Source / discovery
  ↓
Human Review
  ↓
Confirmed Reality
  ↘
    same Impact / Change loop
```

Human Review does not create a new top-level mode. It is a truth-governance task.

## 11. Remaining gated work

Still intentionally not claimed:
- exact-head Kotlin/Compose build PASS;
- exact-head API36 phone/tablet runtime;
- exact-head GPU first-frame evidence;
- Human Pixel Acceptance;
- production VNext source injection;
- production Review screen action binding;
- Canonical lifecycle persistence;
- governed phone/email subtype migration;
- production launcher cutover.

## 12. Stop line

```text
R22_HUMAN_REVIEW_DESIGN_GAP = CLOSED_AT_SOURCE
R22_NOW_RECORDS_BOUNDARY_GAP = CLOSED_AT_SOURCE

CURRENT_HEAD_COMPILE_TEST = MUST_BE_GREEN
RUNTIME_PIXEL_EVIDENCE = REQUIRED
HUMAN_VISUAL_ACCEPTANCE = REQUIRED

ANDROID_REFERENCE_FREEZE = HOLD
PRODUCTION_VNEXT_CUTOVER = HOLD
```
