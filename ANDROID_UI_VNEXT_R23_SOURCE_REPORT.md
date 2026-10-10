# ANDROID_UI_VNEXT_R23_SOURCE_REPORT

> 2026-10-10 · feat/android-ui-vnext-translation
>
> R23 closes the **Establish / Import** product-loop gap on top of R22.
>
> R22 made Human Review visible. R23 now makes the preceding user capability —
> **建立基础设施** — visible in UI vNext without weakening the existing production
> import security or Proposal authority boundary.

## 0. Truth status

~~~text
ANDROID_UI_VNEXT_SOURCE = R23

FIVE_PRIMARY_IA = PRESERVED
ESTABLISH_IMPORT_REFERENCE = IMPLEMENTED_SOURCE
ESTABLISH_IMPORT_SOURCE_ENTRY = IMPLEMENTED_SOURCE
ESTABLISH_IMPORT_SEARCH_ENTRY = IMPLEMENTED_SOURCE
ESTABLISH_IMPORT_PREVIEW = READ_ONLY_REFERENCE

PRODUCTION_IMPORT_PIPELINE = EXISTING_AND_REUSED
PRODUCTION_VNEXT_IMPORT_PROJECTION = SOURCE_IMPLEMENTED
PRODUCTION_FILE_WORKFLOW_REUSE = DESIGN_FROZEN

HUMAN_REVIEW = IMPLEMENTED_SOURCE
NOW_RECORDS_BOUNDARY = PRESERVED

CANONICAL_SCHEMA_CHANGE = NONE
DEPMAP_PAYLOAD_CHANGE = NONE
NEW_PRIMARY_TAB = NONE

FRESH_R23_BUILD = NOT_RUN
FRESH_R23_UNIT_TESTS = NOT_RUN
FRESH_R23_INSTRUMENTATION = NOT_RUN
FRESH_R23_PHONE_PIXELS = NOT_RUN
FRESH_R23_TABLET_PIXELS = NOT_RUN
FRESH_R23_HUMAN_ACCEPTANCE = NOT_RUN

ANDROID_REFERENCE_FREEZE = HOLD
PRODUCTION_VNEXT_CUTOVER = HOLD
~~~

## 1. Why R23 exists

v2.3 defines five user-layer capabilities:

~~~text
建立
确认
理解
变更
维持
~~~

Before R23, VNext already had:
- 确认: R22 Human Review;
- 理解: object identity + Impact Lens;
- 变更: Change Center + focused Change scenarios;
- 维持: Now / Weaknesses / lifecycle / Records.

But 建立 still lived only in the existing production legacy UI.

R23 adds the VNext consumer reference for Establish while preserving the proven
production import pipeline.

## 2. Information architecture

R23 does not create a sixth primary tab.

Primary remains:

~~~text
现在 / 基础设施 / 变更 / 记录 / 我
~~~

Establish route:

~~~text
/import
~~~

Entry points:
- Data Sources → 建立基础设施;
- Search → 建立基础设施.

Hierarchy:

~~~text
建立基础设施 → Up → 数据源 → Up → 我
~~~

## 3. Preview surface

File:

android/app/src/main/kotlin/com/pdig/uivnext/ui/screens/R23ImportReferenceScreen.kt

The page shows the product flow:

~~~text
1 选择数据来源
2 选择文件并在本机解析
3 确认要记录的对象
~~~

It also shows the permanent truth boundary:

> 文件留在本机 · 发现不等于依赖

and the Preview boundary:

> 当前是隔离预览

Preview deliberately does not:
- open ACTION_OPEN_DOCUMENT;
- read a user file;
- create SourceInstance;
- parse real observations;
- commit nodes/proposals;
- fabricate a success result.

This avoids presenting a design reference as a working production ingestion path.

## 4. Production pipeline is reused, not rewritten

The current Android production app already has a hardened flow:

~~~text
FileWorkflowCoordinator
→ external picker
→ background lock / re-auth survival
→ AppContainer.parseFile
→ AppContainer.previewImport
→ Node Resolution / CSV mapping
→ AppContainer.commitImport
~~~

Important existing security/correctness behavior:
- ActivityResult launcher lifetime is Activity-scoped;
- picker result does not unlock the app;
- parsing/commit work survives the NavHost lock transition correctly;
- observations and raw file context stay in memory as governed;
- URI handling remains read-only;
- commit is authoritative/transactional.

R23 does not replace any of this with a new VNext-specific implementation.

## 5. Import truth boundary

The existing SourceRepository already enforces:

~~~text
commitImport
→ SourceInstance
→ ImportSession
→ Fingerprint / Evidence
→ confirmed objects permitted by current import semantics
→ Proposal / Candidate / Drift work

NOT:
→ confirmed Dependency
~~~

Therefore:

~~~text
Import commit != Dependency confirmation
~~~

Generated relation proposals remain R22 Human Review work.

## 6. Production VNext projection

R23 adds:

production/ProductionVNextImport.kt

It exposes consumer-safe projections for:
- ImportPreview;
- ImportCommitResult.

Preview projection includes:
- source label;
- adapter;
- observation count;
- skipped row count;
- detected payment instruments;
- detected counterparties.

Commit projection includes:
- raw count;
- new unique count;
- duplicate count;
- node count;
- proposal count;
- error count.

Navigation guidance:
- proposalCount > 0 → Review;
- proposalCount == 0 → Sources.

Internal importSessionId is not promoted as a consumer-facing field.

## 7. No second parser / picker

Production VNext cutover must not introduce:
- another CSV parser;
- another OFX/QFX parser;
- another WeChat parser;
- another ActivityResult launcher;
- direct SQL import writes;
- a VNext-only lock exception.

The VNext screen must bind to the existing FileWorkflowCoordinator and AppContainer
when production cutover occurs.

## 8. Data Sources

Both compact and adaptive Data Sources now expose:

> 建立基础设施

alongside:

> 待复核

This makes the evidence loop understandable:

~~~text
数据与来源
→ 建立基础设施
→ 待复核
→ Confirmed Reality
~~~

without turning Data Sources into an admin console.

## 9. Search

Search recognizes:
- 建立基础设施;
- 导入;
- 账单;
- 文件;
- 建立;
- 新增来源.

Search opens the Establish reference only. It never launches a picker automatically.

## 10. Tests / runtime proof source

Added:
- ProductionVNextImportTest;
- EstablishImportR23Test;
- EstablishImportR23ContractTest.

Phone pixel proof now requires:
- Data Sources → 建立基础设施;
- 文件留在本机 · 发现不等于依赖;
- the three steps;
- 当前是隔离预览;
- Header Up → Data Sources;
- then Human Review.

## 11. Product loop after R23

The visible source architecture now covers:

~~~text
建立
Source / Import
→ discovered objects / proposals

确认
Human Review
→ Personal Reality

理解
Infrastructure
→ object identity
→ Impact Lens

变更
Change Center
→ Current / Transition / After
→ Verification

维持
Now / Weaknesses / lifecycle
→ Records evidence trace
~~~

This is the first UI vNext source state where the v2.3 five user-layer capabilities
all have a defined consumer surface or governed child flow.

## 12. Still intentionally gated

Not claimed:
- exact-head compile/test PASS;
- API36 phone/tablet runtime;
- Human Pixel Acceptance;
- real VNext file picker production binding;
- production VNext launcher cutover;
- Canonical lifecycle persistence;
- Identity Context Canonical implementation;
- Recovery incident solver runtime.

## 13. Stop line

~~~text
R23_ESTABLISH_DESIGN_GAP = CLOSED_AT_SOURCE
R23_IMPORT_AUTHORITY_BOUNDARY = CLOSED_AT_SOURCE
R23_FIVE_CAPABILITY_PRODUCT_LOOP = SOURCE_COMPLETE

CURRENT_HEAD_COMPILE_TEST = MUST_BE_GREEN
RUNTIME_PIXEL_EVIDENCE = REQUIRED
HUMAN_VISUAL_ACCEPTANCE = REQUIRED

ANDROID_REFERENCE_FREEZE = HOLD
PRODUCTION_VNEXT_CUTOVER = HOLD
~~~
