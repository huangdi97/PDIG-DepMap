# ANDROID_UI_VNEXT_R24_SOURCE_REPORT

> 2026-10-10 · feat/android-ui-vnext-translation
>
> R24 closes the **Manual Establish design gap** after R23 file import.
>
> It intentionally does not fake a production manual-save capability.

## 0. Truth status

~~~text
ANDROID_UI_VNEXT_SOURCE = R24

FIVE_PRIMARY_IA = PRESERVED

ESTABLISH_IMPORT = SOURCE_IMPLEMENTED
MANUAL_ESTABLISH_REFERENCE = SOURCE_IMPLEMENTED
MANUAL_ESTABLISH_SEARCH_ENTRY = SOURCE_IMPLEMENTED
MANUAL_ESTABLISH_PREVIEW = READ_ONLY

HUMAN_REVIEW = SOURCE_IMPLEMENTED
IMPACT_LENS = SOURCE_IMPLEMENTED
CHANGE_CENTER = SOURCE_IMPLEMENTED
RECORDS_EVIDENCE_TRACE = SOURCE_IMPLEMENTED

CANONICAL_SCHEMA_CHANGE = NONE
DEPMAP_PAYLOAD_CHANGE = NONE
NEW_PRIMARY_TAB = NONE

PRODUCTION_MANUAL_CREATE_AUTHORITY = NOT_EXPOSED_TO_VNEXT
IDENTITY_SUBTYPE_MANUAL_CREATE = HOLD

FRESH_R24_BUILD = NOT_RUN
FRESH_R24_UNIT_TESTS = NOT_RUN
FRESH_R24_INSTRUMENTATION = NOT_RUN
FRESH_R24_PHONE_PIXELS = NOT_RUN
FRESH_R24_TABLET_PIXELS = NOT_RUN
FRESH_R24_HUMAN_ACCEPTANCE = NOT_RUN

ANDROID_REFERENCE_FREEZE = HOLD
PRODUCTION_VNEXT_CUTOVER = HOLD
~~~

## 1. Why R24 exists

v2.3 describes Establish as broader than file import:

~~~text
导入
+ 手工录入
+ future discovery
~~~

R23 closed file import and R22 closed discovery review, but Manual Establish still
had no VNext consumer surface.

R24 closes the design/reference surface without bypassing domain authority.

## 2. Manual Establish route

Route:

~~~text
/manual-add
~~~

Label:

~~~text
手工记录
~~~

Hierarchy:

~~~text
Data Sources
→ 建立基础设施
  → 手工记录
~~~

Up:

~~~text
手工记录 → 建立基础设施
~~~

Search aliases:
- 手工;
- 添加;
- 新建对象;
- 录入.

No sixth primary destination is introduced.

## 3. Current runtime creation set is respected

Canonical storage supports more NodeKind values than current runtime creation.

Current runtime creation set in canonical spec:

~~~text
payment_instrument
account
service
~~~

R24 exposes that difference in the design rather than pretending every VNext object
type can be safely created manually today.

Reference groups:

### Current runtime creation set
- 支付工具;
- 账户;
- 服务.

### Known but not production-ready as generic manual-create surfaces
- identity_anchor → phone/email subtype ambiguous;
- device;
- membership;
- custom.

## 4. No ghost Save

The R24 Preview explicitly says:

> 当前 Preview 不提供“保存”按钮

This is intentional.

There is currently no AppContainer-facing manual-create authority designed/tested
for the VNext production cutover.

Adding a local Compose Save button would create a false product capability and risk:
- Android-only semantics;
- incorrect identity subtype;
- missing graphRevision mutation;
- direct database writes;
- relationship inference.

R24 therefore freezes the UX first and keeps mutation HOLD.

## 5. Object existence vs relationship truth

Permanent rule:

~~~text
manual object confirmation
!=
manual dependency confirmation
~~~

Recording “this account exists” must not automatically create:
- authenticates;
- recovers;
- funding_source;
- merchant_agreement;
- controls.

Relationship confirmation remains separately governed.

## 6. Identity boundary

The existing Canonical kind:

~~~text
identity_anchor
~~~

is deliberately not enough to infer:
- Number;
- Email;
- Passkey;
- Recovery identity.

This matches the production VNext surface classification already implemented:

~~~text
identity_anchor
→ IDENTITY_ANCHOR_GENERIC
~~~

until a governed subtype / Identity Context implementation lands.

## 7. Establish hub after R24

The consumer Establish hierarchy now supports:

~~~text
建立基础设施
├─ 文件导入
│  └─ local parse → confirm objects → review proposals
└─ 手工记录
   └─ governed object-only creation design
~~~

The current Preview:
- demonstrates both;
- mutates neither.

## 8. Tests / pixel source

Added:
- ManualEstablishR24Test;
- ManualEstablishR24ContractTest.

Pixel journey now checks:

~~~text
Data Sources
→ 建立基础设施
→ 手工记录
→ truth boundary
→ no fake Save
→ Up to 建立基础设施
→ Up to Data Sources
→ Human Review
~~~

## 9. User-layer capability coverage

After R24 the v2.3 user-layer design is source-covered:

~~~text
建立
  file import + manual reference + discovery source plane

确认
  Human Review

理解
  object identity + Impact Lens

变更
  Change Center + focused Change primitives

维持
  Now + lifecycle + Weaknesses + Records
~~~

This is design/source coverage. It is not a production cutover claim.

## 10. Remaining non-design gates

The remaining work is no longer a missing consumer design surface.

Remaining gates are implementation/evidence classes:
- current exact-head compile/tests;
- API36 runtime and pixel evidence;
- Human Visual Acceptance;
- production VNext source injection;
- production manual-create authority;
- lifecycle Canonical implementation;
- Identity Context Canonical implementation;
- Recovery solver/runtime;
- iOS/Harmony translation after Android freeze.

## 11. Stop line

~~~text
R24_MANUAL_ESTABLISH_DESIGN_GAP = CLOSED
V23_USER_CAPABILITY_DESIGN_COVERAGE = COMPLETE_AT_SOURCE

SOURCE_COMPLETE != RUNTIME_VERIFIED != HUMAN_ACCEPTED

ANDROID_REFERENCE_FREEZE = HOLD
PRODUCTION_VNEXT_CUTOVER = HOLD
~~~
