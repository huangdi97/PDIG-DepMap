# ANDROID_UI_VNEXT_CURRENT_REALITY.md

> **Current reality · 2026-10-09 · R19**
>
> Branch: `feat/android-ui-vnext-translation`
>
> This file supersedes the original pre-translation A0 audit. Repository truth on
> the current branch has moved far beyond that baseline.
>
> Status vocabulary:
> `IMPLEMENTED_SOURCE / DESIGN_FROZEN / NOT_CONNECTED / NOT_RUN / HOLD`.

## 0. Executive state

```text
ANDROID_UI_VNEXT_SOURCE = R19
ANDROID_LIGHT_REFERENCE_DIRECTION = DESIGN_FROZEN
ANDROID_REFERENCE_FREEZE = HOLD
PRODUCTION_VNEXT_READ_MODEL = SOURCE_IMPLEMENTED
PRODUCTION_VNEXT_CUTOVER = HOLD

CANONICAL_SCHEMA_CHANGE_FOR_R19_UI = NONE
DEPMAP_PAYLOAD_CHANGE_FOR_R19_UI = NONE

FRESH_R19_BUILD = NOT_RUN
FRESH_R19_UNIT_TESTS = NOT_RUN
FRESH_R19_INSTRUMENTATION = NOT_RUN
FRESH_R19_PHONE_PIXELS = NOT_RUN
FRESH_R19_TABLET_PIXELS = NOT_RUN
FRESH_R19_HUMAN_ACCEPTANCE = NOT_RUN
```

Do not reuse pre-R19 screenshots or old PASS statements as proof of the current
source.

## 1. Navigation / shell

| Area | Current reality |
| --- | --- |
| Primary IA | **IMPLEMENTED_SOURCE** — exactly Now / Infrastructure / Change / Records / Me |
| Me | **IMPLEMENTED_SOURCE** — intentional fifth primary destination; compact consumer workspace + dedicated Medium/Expanded adaptive workspace; avatar may remain as shortcut |
| Infrastructure secondary | **IMPLEMENTED_SOURCE** — Overview/Cards/Numbers/Accounts/Emails/Devices/Services/Weaknesses |
| Breakpoints | **IMPLEMENTED_SOURCE** — Compact <600dp, Medium 600–839dp, Expanded >=840dp |
| System Back | **IMPLEMENTED_SOURCE** — chronological stack |
| Header Up | **IMPLEMENTED_SOURCE** — hierarchical parent |
| State host | **IMPLEMENTED_SOURCE** — `VNextShellViewModel` |
| Persistent presentation prefs | **IMPLEMENTED_SOURCE** — local stores, outside Canonical |
| Production launch | **HOLD** — production still lock-gated legacy application |

## 2. Now / Globe

| Area | Current reality |
| --- | --- |
| Light-first Now composition | **IMPLEMENTED_SOURCE** |
| GPU Earth | **IMPLEMENTED_SOURCE** — separate OpenGL path |
| Albedo/night/cloud textures | **IMPLEMENTED_SOURCE** |
| Atmosphere/day-night/ocean glint | **IMPLEMENTED_SOURCE** |
| CPU fallback | **IMPLEMENTED_SOURCE** |
| Drag/zoom/reset | **IMPLEMENTED_SOURCE** |
| Projected region labels | **IMPLEMENTED_SOURCE** — live camera projection |
| Region grouping/collision budget | **IMPLEMENTED_SOURCE** |
| Region card/number/attention context | **IMPLEMENTED_SOURCE** |
| Exact-head GPU runtime proof | **NOT_RUN on current R19 head** |

Globe tethers are geographic annotation tethers only. They are not graph edges.

## 3. Infrastructure

| Screen | Current reality |
| --- | --- |
| Overview | **IMPLEMENTED_SOURCE** |
| Cards | **IMPLEMENTED_SOURCE** |
| Card Detail | **IMPLEMENTED_SOURCE** |
| Card appearance customization | **IMPLEMENTED_SOURCE** |
| Numbers | **IMPLEMENTED_SOURCE** |
| Number Detail | **IMPLEMENTED_SOURCE** |
| Number appearance customization | **IMPLEMENTED_SOURCE** |
| Accounts | **IMPLEMENTED_SOURCE** |
| Emails | **IMPLEMENTED_SOURCE** |
| Devices | **IMPLEMENTED_SOURCE** |
| Services | **IMPLEMENTED_SOURCE** |
| Weaknesses | **IMPLEMENTED_SOURCE** |
| Search | **IMPLEMENTED_SOURCE** |
| Empty states | **IMPLEMENTED_SOURCE** |

These are reference/presentation implementations. That does not mean every field is
yet backed by production Canonical persistence.

## 4. Cards

R19 Card = **Financial Asset Identity**.

Implemented at source:
- strong card identity/collection feeling;
- region/currency/type/form/network/expiry;
- status;
- confirmed fixture service relationships;
- minor local card-art customization;
- recorded lifecycle reference:
  - annual fee;
  - annual-fee checkpoint;
  - billing day;
  - payment due day;
  - installment summary;
  - recorded autopay;
- Impact Lens;
- list/detail/inspector adaptive parity.

Truth boundary:
- missing lifecycle = `未记录`;
- no statement balance/transaction/minimum-payment inference;
- lifecycle fixture is synthetic reference data, not .depmap truth.

## 5. Numbers

R19 Number = **Communication Identity**.

Implemented at source:
- local user display alias;
- blank alias fallback to recorded/masked number;
- role = primary / secondary / keep;
- carrier / SIM-eSIM / region;
- recorded recovery use;
- explicit unique-recovery evidence;
- recorded lifecycle:
  - plan cost;
  - keep-alive due;
  - keep-alive cycle;
  - last action;
  - renewal/keep method;
- Impact Lens;
- alias/lifecycle search;
- compact/adaptive parity.

Permanent correction:

```text
recoveryOnly != uniqueRecoveryPath
```

A recovery role is not automatically a single point of failure.

## 6. Change Phone

Source implementation:
- Current / Transition / After;
- After explicitly remains Plan Projection;
- six-stage choreography;
- Make-Before-Break block;
- service migration states;
- verification language;
- old active number / keep number / new migration target are distinct objects;
- impact summary derived from actual projection state.

Reference IDs:
```text
num-cn-1 old active
num-cn-3 keep
num-cn-4 migration target
```

These IDs are synthetic only; the identity separation is the product invariant.

## 7. Impact Lens

R19 source introduces a shared consumer answer to:

> **如果它发生变化？**

Reference lens can show:
- confirmed dependency count;
- recorded attention findings;
- critical accounts when known;
- explicit unique recovery when known;
- independent alternatives when known;
- unknown/unconfirmed relations.

Null/unknown never becomes 0/safe.

The synthetic reference lens is not the production Continuity engine. Production
binding must consume `AppContainer.impactFor(nodeId)` and future failure-domain-aware
analysis.

## 8. Search

Source search now covers:
- card/number names;
- saved number aliases;
- region/carrier/issuer;
- annual fee / billing facts;
- keep-alive facts;
- consumer role label `保号`;
- services/navigation destinations.

Search only searches recorded/reference data; absence is not proof of nonexistence.

## 9. Production binding

Current reference route:
```text
Preview flavor
→ VNext
→ synthetic reference fixture
```

Current production route:
```text
Production flavor
→ lock-gated PdigApp
→ AppContainer / encrypted Reality
```

They are intentionally not merged yet.

Production APIs already exist:
- `nodes()`
- `dependencies()`
- `timeline()`
- `impactFor()`
- `plans()`
- `planDetail()`
- `createPlanForScenario()`
- `completeAction()`
- `verifyAction()`

The cutover architecture is frozen in
`docs/ADR_UI_VNEXT_PRODUCTION_BINDING.md`.

Source progress now also includes:
- Canonical `issuer` / `last4` projected from production nodes without parsing free-form fields;
- conservative production surface classification;
- `identity_anchor` remains a generic identity until an explicit governed phone subtype exists;
- a consumer inventory projection with confirmed-dependency counts and pending-review/source coverage.

These are read-only seams. They do not switch the launcher or claim R19 lifecycle persistence.

## 10. Canonical boundary

Current Canonical schema does **not** yet define R19 card/number lifecycle fields as
cross-platform semantic fields.

Therefore R19 does not:
- add lifecycle fields to .depmap;
- write them into `PresentationProfile`;
- smuggle them through Android-only `fields_json`;
- claim lifecycle persistence is production-ready.

A future lifecycle persistence feature must go through:

```text
Spec
→ schema/migration decision
→ golden/negative fixtures
→ conformance
→ all platform runtimes
→ UI
```

## 11. Tests / evidence present in source

R19 has source contracts for:
- lifecycle fixture truth/unknown behavior;
- Impact Lens unknown/evidence boundaries;
- keep-number identity;
- separate migration target;
- Change impact summary;
- aliases/navigation hierarchy;
- five-item primary IA;
- search lifecycle/alias semantics;
- projected region label geometry;
- compact visual hierarchy;
- tablet adaptive hierarchy.

These tests are **present**. They are not called PASS until run on the current
exact head.

## 12. Current remaining evidence gates

```text
1. exact-head build
2. unit tests
3. conformance / app tests
4. phone runtime capture
5. tablet runtime capture
6. GPU first-frame/runtime-state proof
7. interaction checks
8. human pixel review against selected reference board
9. Android Reference Freeze decision
```

Only after Android Reference Freeze may iOS/Harmony UI translation leave HOLD.

## 13. Source/design closure documents

- `ANDROID_UI_VNEXT_R19_SOURCE_REPORT.md`
- `ANDROID_REFERENCE_MAPPING.md`
- `ANDROID_VISUAL_CONTRACT.md`
- `spec/ui-vnext/ASSET_CONTINUITY_UX_CONTRACT.md`
- `docs/ADR_UI_VNEXT_PRODUCTION_BINDING.md`

## 14. Honest stop line

```text
SOURCE_DESIGN = COMPLETE
RUNTIME_EVIDENCE = PENDING
REFERENCE_FREEZE = HOLD
PRODUCTION_CUTOVER = HOLD
```

The next blocker is no longer “missing UI design.” It is fresh exact-head runtime
verification and, after reference acceptance, production read-model binding.
