# PDIG vNext — Maintenance Control Surface

> Date: 2026-10-10  
> Status: **R41 PRODUCT / UX CONTRACT — DESIGN FROZEN**  
> Scope: governed card / phone maintenance facts and schedules

## 0. Product decision

Maintenance is **not** a sixth primary destination.

The permanent primary shell stays:

```text
现在 / 基础设施 / 变更 / 记录 / 我
```

Maintenance belongs to the object that owns the fact:

```text
Card Detail
→ 用卡周期
→ 管理生命周期资料

Number Detail
→ 号码生命周期
→ 管理套餐 / 保号资料
```

Upcoming maintenance occurrences surface on **现在**. Completed / verified evidence
belongs to **记录** only when an authoritative event actually exists.

## 1. Why this surface exists

R40 makes `Node.fields.maintenance_profile` governed cross-platform Reality.

The consumer UI therefore needs a write surface that preserves all existing truth
boundaries:

- user-entered fact ≠ provider policy;
- time passed ≠ completed;
- notification delivered ≠ completed;
- missing ≠ zero/free/safe;
- schedule ≠ ChangePlan;
- maintenance fact ≠ dependency;
- Preview fixture ≠ Production Reality.

## 2. Card maintenance

Supported v1 direct-edit facts:

```text
annual fee amount + currency
billing day
payment due day
autopay mode
```

Supported schedule entry:

```text
annual-fee checkpoint
```

Not supported in Canonical v1:

```text
installment summary
statement balance
minimum payment
transactions
interest / amortization
rewards optimization
```

### UI hierarchy

```text
用卡周期
  年费
  账单日
  还款日
  自动还款
  年费检查节点

管理生命周期资料
  已记录值
  explicit edit
  evidence boundary
  save
```

Amount + currency are one semantic pair. The UI must not save one without the other.

## 3. Phone maintenance

Supported v1 direct-edit facts:

```text
billing mode
plan cost + currency
renewal method
```

Supported schedules:

```text
number keep-alive
number plan renewal
```

R41 first consumer editor must at minimum support:
- plan cost + currency;
- billing mode;
- renewal / keep-alive method;
- keep-alive interval days;
- keep-alive anchor date;
- optional explicit last-completed time.

### Important truth rule

```text
lastCompletedAt exists only after an explicit authority action
```

The UI must never advance `lastCompletedAt` just because the due date passed or the
user opened the app.

## 4. Write authority

Production path:

```text
Compose
→ VNextMaintenanceActionGateway
→ AppContainer
→ GraphRepository
→ Canonical maintenance writer
→ encrypted Reality
→ graphRevision bump
→ re-read Production snapshot
```

Compose must not:
- edit `fields_json` directly;
- build a private Android maintenance schema;
- set verification_basis_type itself;
- silently overwrite unknown future container versions;
- fabricate provider-derived values.

Current direct manual writes use `user_confirmed`.

## 5. Stable IDs

Consumer manual writes use one stable semantic ID per fact/schedule kind:

```text
maintenance:fact:<kind>
maintenance:schedule:<kind>
```

This allows repeated edits to update the same manual fact rather than accumulating
duplicate UI-generated records.

Existing authoritative records from other sources are not deleted merely because a
manual record exists. Canonical selection/currentness remains a domain concern.

## 6. Editing interaction

The first Android Production UI uses focused dialogs/sheets from the object detail
instead of adding another global route.

Why:
- lifecycle editing is object-local;
- it avoids a sixth top-level product concept;
- it keeps the user anchored to the asset whose continuity they are managing;
- it minimizes accidental mutation.

Every editor must show:

```text
你正在记录自己的已确认资料
不会根据 Provider 常见规则自动填写
空值不会被解释成 0 / 免费 / 无需保号
```

## 7. Validation

UI performs convenience validation only; Canonical remains final authority.

Examples:
- day: 1–31;
- currency: 3-letter uppercase code;
- amount: non-negative decimal string;
- keep-alive interval: 1–3660 days;
- anchor date: YYYY-MM-DD.

A UI validation pass does **not** imply the write is valid. Repository/Core may still
reject it.

## 8. Error / success states

Success:
- close editor;
- show “已记录到本机 Reality”;
- re-read the current Production snapshot;
- never show “已验证安全”.

Failure:
- keep editor data;
- show a user-readable error boundary;
- no optimistic local success state;
- no partial visual state pretending the write committed.

## 9. Search / list density

Governed maintenance facts may participate in Production search.

Examples:
- 年费;
- 账单日;
- 保号;
- 套餐;
- 续费;
- recorded value.

Search result subtitles may remain identity-focused; matching a maintenance fact does
not need to expose sensitive values in the result row.

Asset list rows may show a single compact lifecycle line when authoritative data
exists. Missing lifecycle should leave the row clean rather than adding “0”.

## 10. Timeline / Now

R40 durable schedules are not themselves proof of an occurrence engine.

Future/current occurrence projection must remain derived:

```text
MaintenanceSchedule
→ deterministic occurrence projection
→ Now upcoming item
```

It must preserve:
- source schedule ID;
- due time basis;
- needs-review state;
- explicit completion boundary.

No automatic completion from time passage.

## 11. Change integration

Maintenance can recommend opening a supported Change scenario, but it cannot execute
one.

Examples:

```text
card expiry / annual-fee checkpoint
→ inspect card Impact
→ optionally enter replace_payment_card

keep-alive cost no longer worth maintaining
→ inspect number Impact
→ optionally enter replace_phone_number / retirement analysis
```

Maintenance does not prove a replacement is required.

## 12. Acceptance contract

Source:
- Production Card Detail exposes governed maintenance facts;
- Production Number Detail exposes governed maintenance facts/schedules;
- card editor writes only card-applicable kinds;
- phone editor writes only governed PHONE_NUMBER-applicable kinds;
- stable fact/schedule IDs;
- no direct JSON writes in Compose;
- no sixth primary tab.

Runtime:
- write → graphRevision changes;
- re-open detail → committed value persists;
- invalid fact does not mutate Reality;
- phone keep-alive cannot be written to generic/email identity;
- background/relock does not bypass LockGate;
- Preview remains non-authoritative.

## 13. Stop line

```text
MAINTENANCE_UX_DESIGN = FROZEN
R40_CANONICAL_READ = SOURCE_IMPLEMENTED
R40_CANONICAL_WRITE_AUTHORITY = SOURCE_IMPLEMENTED
R41_PRODUCTION_EDITOR = SOURCE_IMPLEMENTATION_TARGET
RUNTIME_ACCEPTANCE = PENDING
RELEASE_CUTOVER = HOLD
```
