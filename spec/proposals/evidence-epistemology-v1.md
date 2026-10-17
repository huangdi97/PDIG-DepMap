# Evidence Epistemology v1 — What a Source Can Prove

> Date: 2026-10-10
> Status: **DESIGN_FROZEN / CURRENT V0.3 INVARIANTS PRESERVED**
>
> This contract defines what evidence can prove, what it may only propose, and
> what must never cross into Personal Reality. It composes existing v0.3
> invariants and does not change current SourceKind / CoverageMode / Proposal
> storage.

## 1. Epistemic pipeline

~~~text
external/manual input
→ SourceInstance
→ Observation
→ Evidence summary
→ Proposal / Candidate / Drift
→ authorized confirmation
→ Personal Reality
→ Impact / Finding / Change
~~~

Permanent invariants:

~~~text
Observation != Reality
Proposal != Reality
Candidate != Node
Drift != Reality change
Evidence != History
absence != non-existence
Timeline != Truth
coverage != readiness
confidence != authority
~~~

Only confirmed Reality mutation bumps graphRevision.

## 2. Four independent axes

Every source has at least four independent dimensions.

~~~text
Source kind
  statement_file / platform_export / open_banking / manual / discovery

Coverage mode
  event_stream / partial_snapshot / complete_snapshot / user_selected

Authority
  USER_CONFIRMED / AUTHORITATIVE_SOURCE / OBSERVATIONAL /
  KNOWLEDGE_ONLY / MODEL_INFERENCE

Freshness/revision
  current / superseded / needs_review / outside effective window
~~~

Source kind does not imply completeness. Coverage does not imply authority.

## 3. Positive observation vs negative evidence

Positive observation:

> I saw X.

Negative evidence:

> Within a complete, governed scope, X was absent.

They are not symmetric.

### event_stream

May prove that a record/event appeared.

It cannot prove that an unobserved relationship, account, backup or configuration
does not exist.

~~~text
event_stream absence = no Reality conclusion
~~~

### partial_snapshot

May prove facts explicitly present in its covered subset.

Absence means only “not present in this partial source”.

### complete_snapshot

Bounded negative evidence is allowed only when all are explicit:

~~~text
scope identity
completeness guarantee
effective/retrieved time
adapter/source revision
object matching semantics
negative-inference policy
~~~

Even then, negative evidence normally creates Drift / needs_review before any
confirmed Reality retirement.

### user_selected

The user intentionally selected a subset. Absence outside the selection has no
meaning.

## 4. Source-class authority matrix

| Source class | Positive observation | Negative inference | Direct Reality authority |
| --- | --- | --- | --- |
| explicit manual confirmation | yes | only explicit user statement | yes for the confirmed action |
| statement/event stream | yes | no | no; Evidence/Proposal |
| partial platform export | yes | no outside declared scope | normally Proposal/Review |
| complete platform export | yes | bounded only if completeness is governed | fact-class specific |
| governed API/open banking | yes within API contract | only if API contract proves scope | fact-class specific |
| discovery/browser/device metadata | observation | no | no |
| AI/LLM extraction | observation/proposal | no | never |
| provider official documentation | provider policy knowledge | not user configuration | Knowledge Plane only |
| confirmed PDIG Reality | recorded scope | graph query only | already Reality |
| verification workflow | result-specific | no general absence inference | governed transition only |

“Authoritative source” is not a brand label. Authority belongs to a specific:

~~~text
adapter + fact class + source semantics + revision/effective scope
~~~

## 5. Manual authority

Manual does not mean free-form text automatically becomes truth.

A manual mutation needs:
- explicit semantic type;
- explicit target;
- validation;
- confirmation preview when material;
- governed transaction;
- graphRevision bump when confirmed Reality changes.

Example:

~~~text
user confirms A recovers B
→ confirmed Dependency allowed

user writes “maybe backup email”
→ note/proposal, not confirmed recovers
~~~

## 6. Authoritative source is fact-specific

A source can prove one fact while proving nothing about adjacent semantics.

A bank statement may prove issuer/card tail/observed transaction while not proving:
- every merchant dependency;
- current annual fee;
- recovery paths;
- issuance jurisdiction;
- all future obligations.

A carrier export may prove fields in its explicit contract while not proving:
- every service can currently send SMS;
- this number is the unique recovery path;
- independent recovery paths.

No source is globally authoritative for an object.

## 7. Provider Knowledge Plane

Official provider documentation may establish:
- ProviderPolicy;
- supported operation;
- waiting period;
- documented constraint;
- policy revision/effective window.

It cannot establish:
- user configured X;
- user owns X;
- a Dependency exists;
- a recovery factor is currently usable;
- identity subtype;
- RegionFact.

Permanent rule:

~~~text
Provider supports X != User configured X
~~~

Unverifiable/stale policy becomes needs_review.

## 8. AI / model authority

AI may parse, normalize, extract, classify, propose, explain, deduplicate candidates
and prioritize review.

AI may not:
- confirm Reality;
- set criticality=required;
- mark verified;
- prove path independence;
- confirm identity subtype;
- confirm RegionFact;
- retire an old path;
- resolve Drift without an authorized transition.

~~~text
0.99 confidence != confirmed Reality
multiple model votes != authority
~~~

## 9. Multi-source evidence

Multiple sources add:
- provenance diversity;
- explainability;
- review priority;
- duplicate context.

They do not automatically cross the Reality Boundary.

~~~text
three weak sources != one authorized confirmation
~~~

Per-stream observation thresholds remain per stream. Counts from unrelated
SourceInstances must not be summed to satisfy a per-stream gate.

## 10. Fingerprints and dedup

Observation fingerprint identity is scoped by:

~~~text
sourceInstanceId + fingerprintVersion + fingerprint
~~~

Dedup prevents repeated ingestion. It does not merge Reality objects.

## 11. Proposal confidence

confidenceScore is review/display metadata only.

Forbidden:

~~~text
confidence → must_change
confidence → criticality=required
confidence → PlanReadiness
confidence → unique recovery
confidence → auto-accept
~~~

Allowed:
- sort review items;
- explain extraction quality.

Never label confidence as “真实性”.

## 12. Candidate authority

DiscoveryCandidate is not a Node.

Before acceptance:
- no Impact;
- no graphRevision bump;
- no Dependency semantics;
- no confirmed Region/Identity membership.

Acceptance creates/reuses the governed logical object through an authoritative
transaction.

## 13. Drift authority

RealityDrift means positive evidence suggests a change to confirmed Reality.

Creating Drift:
- requires positive evidence;
- does not mutate Reality;
- does not bump graphRevision.

Forbidden:

~~~text
nothing new observed
→ infer old relation disappeared
→ create replacement Drift
~~~

## 14. Verification evidence

Verification remains:

~~~text
pending
evidence_suggested
verified
failed
not_required
~~~

Permanent:

~~~text
done != verified
evidence_suggested != verified
~~~

Future automated/authoritative verification requires an explicit domain transition.
Compose cannot invent it.

## 15. Evidence is not History

Evidence answers:

> What supports this assertion/review item?

Records answers:

> What happened, was decided, completed or verified?

Imported rows are Evidence, not automatically historical PDIG events. Preview
fixtures never enter Production Records.

## 16. Freshness

Freshness is orthogonal to truth:

~~~text
fresh / aging / stale / unknown
~~~

A stale fact may remain historically confirmed while requiring revalidation.
Freshness alone does not retire it.

Every freshness policy must name:
- fact/source class;
- threshold/cadence;
- reference timestamp;
- policy/source revision;
- action when stale.

No global “90 days = stale” shortcut.

## 17. Region / subtype / lifecycle examples

~~~text
statement currency=GBP
→ cannot confirm RegionFact(GB)

phone-looking name
→ cannot confirm PHONE_NUMBER subtype

statement observed on day 18
→ cannot infer billingDay=18
~~~

A governed adapter may propose or confirm an explicit field only according to its
declared fact-class authority.

## 18. Import preview contract

Before commit the UI should expose:

~~~text
source
coverage mode
parsed observations
proposals
candidate objects
duplicates
errors
facts eligible for authorized confirmation
what will NOT change automatically
~~~

Commit result should distinguish:

~~~text
Evidence recorded
Proposals updated
Candidates updated
Reality mutations explicitly authorized
graphRevision before/after
~~~

A generic “Import successful” is insufficient for high-consequence sources.

## 19. Consumer language

Prefer:

~~~text
发现 / 建议 / 待确认 / 依据 / 已确认 / 需要重新确认 / 当前无法判断
~~~

Avoid:

~~~text
AI 已识别为真实
自动修复完成
系统认为安全
100% 正确
~~~

Accepted Proposal:

> 已确认并写入你的基础设施记录。

Rejected Proposal:

> 已拒绝建议；原有已确认记录未改变。

## 20. Adapter epistemic contract

Every future evidence adapter must declare:

~~~text
adapterId / adapterVersion
SourceKind / CoverageMode
supported fact and observation classes
positive observation semantics
negative evidence semantics
authority per fact class
fingerprint version
time/effective-window semantics
redaction/privacy behavior
failure behavior
Proposal/Candidate mapping
which transitions require human confirmation
~~~

If this declaration is incomplete, the adapter is observational only.

## 21. Security / privacy

Raw evidence may be more sensitive than the graph.

Requirements:
- minimize retained data;
- encrypted/app-private storage where retained;
- explicit raw-source retention/deletion policy;
- no password/private key/recovery-code values in ordinary evidence fields;
- identifiers redacted from logs/analytics;
- screenshot evidence must not expose sensitive imports.

## 22. Cross-platform cases

Required future adapter-independent fixtures:

~~~text
EP-01 event-stream absence gives no negative inference
EP-02 partial snapshot absence does not retire Reality
EP-03 complete-snapshot negative evidence is scope-bounded
EP-04 user confirmation may cross Reality boundary
EP-05 model confidence cannot cross Reality boundary
EP-06 multiple sources add provenance but do not auto-confirm
EP-07 fingerprints are SourceInstance-scoped
EP-08 Proposal confidence never changes Impact
EP-09 Candidate never enters Impact before acceptance
EP-10 Drift requires positive evidence
EP-11 evidence_suggested != verified
EP-12 ProviderPolicy does not create user configuration
EP-13 stale fact does not auto-retire
EP-14 currency does not infer Region
EP-15 phone-looking text does not infer identity subtype
EP-16 observed statement date does not infer lifecycle cadence
~~~

## 23. Stop line

~~~text
EVIDENCE_EPISTEMOLOGY_DESIGN = COMPLETE
SOURCE_AUTHORITY_BOUNDARY = FROZEN
NEGATIVE_EVIDENCE_DESIGN = COMPLETE
AI_AUTHORITY = PROPOSAL_ONLY
MULTI_SOURCE_AUTHORITY_ESCALATION = FORBIDDEN

CURRENT_V0_3_CANONICAL = UNCHANGED
FUTURE_ADAPTERS_MUST_DECLARE_EPISTEMIC_CONTRACT
~~~
