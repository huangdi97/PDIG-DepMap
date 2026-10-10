# PDIG Manual Establish UX Contract

> Status: **DESIGN_FROZEN / R24**
>
> Scope: v2.3 user capability **建立** — manual recording when no importable source
> exists.
>
> This contract does not authorize an Android-only Reality mutation path.

## 1. Why manual establishment exists

Not every piece of personal digital infrastructure has a clean export.

Users may know:
- a payment tool exists;
- an account exists;
- a service exists;
- an identity/device exists;

without having a source file ready to import.

Manual establishment should capture that explicit user knowledge without inventing
relationships.

Permanent rule:

~~~text
I confirm this object exists
!=
I confirm every relation attached to it
~~~

## 2. Information architecture

Manual Record is a child of Establish:

~~~text
Data Sources
→ 建立基础设施
  → 文件导入
  → 手工记录
~~~

Hierarchy:

~~~text
手工记录 → Up → 建立基础设施 → Up → 数据源 → Up → 我
~~~

It is not a primary destination.

## 3. Current Canonical/runtime reality

Canonical NodeKind can store:
- identity_anchor;
- payment_instrument;
- account;
- service;
- membership;
- device;
- custom.

However the current canonical runtime creation set is narrower:

~~~text
payment_instrument
account
service
~~~

VNext must respect that difference.

A storage-allowed kind is not automatically a production-supported manual-create
surface.

## 4. R24 Preview

The reference screen groups:

### Current runtime creation set
- 支付工具;
- 账户;
- 服务.

### Other known kinds
- 号码 / 邮箱 / 身份 → identity_anchor is currently too coarse for safe consumer subtype mapping;
- 设备 → storage-known but not in current generic runtime creation set;
- 会员 / 自定义 → require explicit product semantics before consumer creation.

Preview renders no Save button.

Required explanation:

> 当前 Preview 不提供“保存”按钮

and:

> 原因不是 UI 没画完，而是正式 VNext 还没有经过 AppContainer 暴露并测试的手工 Reality mutation authority。

This prevents a ghost capability.

## 5. Future production authority

A production Manual Establish authority must:

1. validate the requested type against canonical runtime-creatable kinds;
2. validate/normalize the user-visible name without inferring hidden identity;
3. create the Node through a repository/domain transaction;
4. bump graphRevision in the same authoritative mutation;
5. return authoritative Node/revision state;
6. never create a Dependency as a side effect;
7. never claim a phone/email subtype from a generic identity_anchor unless governed subtype evidence exists.

Compose must not:
- generate SQL;
- choose IDs independently of domain policy;
- bump graphRevision itself;
- save into PresentationProfile as a substitute for Reality.

## 6. Minimal consumer fields

The first manual-create contract should stay deliberately small.

Common:
- object type;
- user-visible name;
- optional local presentation alias only when clearly separated from Reality.

Do not require made-up metadata.

Payment tool:
- identity name can be recorded;
- issuer / last4 only if the production authority explicitly supports those canonical fields;
- lifecycle fields remain governed by the lifecycle proposal.

Account:
- account name;
- provider/identifier only when supported by canonical production mapping;
- no authentication relation inferred.

Service:
- service name;
- no subscription-active claim unless separately evidenced;
- no payment or authentication relation inferred.

## 7. Identity subtype boundary

Current identity_anchor is insufficient to automatically mean:
- phone number;
- email;
- passkey;
- recovery identity.

Therefore R24 must not implement:

~~~text
manual identity_anchor
→ choose “phone”
→ VNext Number object
~~~

until the governed Identity Context/subtype proposal is implemented cross-platform.

This is a semantic gate, not a visual TODO.

## 8. Relationship entry

Manual object creation and manual relationship confirmation are separate jobs.

Future relationship flow should use:
- explicit From;
- explicit Relation;
- explicit To;
- capability;
- criticality default = unknown;
- human confirmation.

Machine/UI must never set required automatically.

A dedicated manual relationship UX may be added only by binding the existing
Canonical relation/validation authority; it must not be smuggled into the object
creation form.

## 9. Search / discoverability

Search may open:
- 建立基础设施;
- 手工记录.

Aliases:
- 手工;
- 添加;
- 新建对象;
- 录入.

Search never performs creation.

## 10. Acceptance

Source:
- MANUAL_ADD is secondary;
- Up → IMPORT;
- five primary IA unchanged;
- no Save button in Preview;
- unsupported subtype state visible.

Future production:
- creation uses authoritative domain/repository API;
- graphRevision bump verified;
- duplicate identity behavior defined;
- dependency count unchanged after object-only create;
- exact type preserved across export/restore/all platforms.

## 11. Stop line

~~~text
MANUAL_ESTABLISH_UX = DESIGN_FROZEN
MANUAL_ESTABLISH_PREVIEW = SOURCE_IMPLEMENTED_READ_ONLY
PRODUCTION_MANUAL_CREATE_AUTHORITY = NOT_EXPOSED_TO_VNEXT
IDENTITY_SUBTYPE_MANUAL_CREATE = HOLD
NO_GHOST_SAVE_ACTION = REQUIRED
~~~
