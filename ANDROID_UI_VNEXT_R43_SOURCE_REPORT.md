# ANDROID_UI_VNEXT_R43_SOURCE_REPORT

> 2026-10-10 · `feat/android-ui-vnext-translation`
>
> R43 closes the remaining **Production VNext file-import execution gap** after
> R42 connected the Activity-owned picker request.
>
> This is a source/design report. It is **not** exact-head runtime acceptance.

## 0. Source state

```text
FIVE_PRIMARY_IA = FROZEN
REFERENCE_UI = SOURCE_COMPLETE
PRODUCTION_VNEXT_READ_MODEL = SOURCE_IMPLEMENTED
PRODUCTION_VNEXT_MUTATION_GATEWAYS = SOURCE_IMPLEMENTED

PRODUCTION_IMPORT_PICKER_HOST = SOURCE_IMPLEMENTED_R42
PRODUCTION_IMPORT_REAUTH_RESUME = SOURCE_IMPLEMENTED_R43
PRODUCTION_IMPORT_MAPPING_REVIEW = SOURCE_IMPLEMENTED_R43
PRODUCTION_IMPORT_EXPLICIT_COMMIT = SOURCE_IMPLEMENTED_R43
PRODUCTION_IMPORT_EXISTING_SOURCE_IDENTITY = SOURCE_IMPLEMENTED_R43
PRODUCTION_IMPORT_REVIEW_HANDOFF = SOURCE_IMPLEMENTED_R43

PRODUCTION_RELEASE_DEFAULT = LEGACY
PRODUCTION_VNEXT_CUTOVER = HOLD
ANDROID_REFERENCE_FREEZE = HOLD

FRESH_EXACT_HEAD_BUILD = PENDING
FRESH_EXACT_HEAD_APP_TESTS = PENDING
FRESH_API36_IMPORT_REAUTH_E2E = PENDING
FRESH_PHONE_TABLET_PIXELS = PENDING
HUMAN_ACCEPTANCE = PENDING
```

## 1. What R42 had and what it still lacked

R42 connected:

```text
Production VNext “文件导入”
→ MainActivity
→ ActivityResultContracts.OpenDocument
→ FileWorkflowCoordinator.beginImport
→ launch picker
```

That was necessary but not sufficient.

After Android leaves the app for DocumentsUI:

```text
MainActivity.onStop
→ LockGate.lockNow()
→ Production VNext content uncomposes
→ picker result returns
→ FileWorkflowCoordinator records URI
→ user must re-authenticate
```

The missing half was the resumed VNext consumer flow after re-auth.

R43 implements that half.

## 2. Final Production import chain

```text
建立基础设施
→ select existing source OR name new source
→ request Activity-owned OpenDocument picker
→ leave foreground
→ fail-closed app lock
→ ActivityResult records URI only
→ re-auth
→ same VNext Establish workspace resumes
→ consume pending URI exactly once
→ local read + existing parser
→ optional generic-CSV field mapping review
→ ImportPreview
→ explicit object/import confirmation
→ AppContainer.commitImport
→ Source / Evidence / Node / Proposal-Candidate-Drift work
→ Human Review when Proposal exists
```

No VNext screen registers a second picker.

No picker result unlocks the app.

No picker result commits Reality.

## 3. Android platform alignment

The implementation intentionally uses `ACTION_OPEN_DOCUMENT` through
`ActivityResultContracts.OpenDocument`.

Android's Storage Access Framework permits the app to take a persistable document
permission for an `ACTION_OPEN_DOCUMENT` result. PDIG uses only the read grant and
only for the lock/re-auth window.

Official references:

- https://developer.android.com/reference/android/content/Intent#ACTION_OPEN_DOCUMENT
- https://developer.android.com/training/data-storage/shared/documents-files

R43 returns the held read grant immediately after successful import instead of
waiting for later navigation or Activity destruction.

## 4. Source identity is now explicit

The previous import repository always derived:

```text
sourceInstanceId = hash(adapter + label)
```

even when the UI had explicitly selected an existing SourceInstance.

That meant the product could visually say “继续这个来源” while the authoritative
commit had no direct knowledge of the selected source id.

R43 corrects this.

### New source

```text
no selected SourceInstance
→ current stable adapter + label identity rule
```

### Existing source

```text
explicit SourceInstance id
→ source must still exist
→ source must be active
→ parsed adapter must match
→ label must match
→ commit updates the existing source ingest timestamp
→ same source id is returned
```

Mismatch fails closed.

The UI does not silently create a different adapter/source under a misleading
“existing source” selection.

## 5. Existing-source selection survives lock/re-auth

Production Establish now reads authoritative source rows from the Production
snapshot.

The user can choose one.

The workflow persists:

```text
requestedSourceId
requestedSourceLabel
purpose
resumeRoute
```

as non-sensitive workflow metadata.

It does **not** persist:
- raw statement bytes;
- Observation payloads;
- decrypted content;
- passwords/secrets.

Editing the source label clears the explicit existing-source selection and becomes a
new-source intent.

## 6. Generic CSV field mapping

R43 reuses the existing human field-mapping interaction rather than creating a
second mapping interpretation.

The shared mapping component now accepts an injected parser authority:

```text
CsvMappingStepWithParser
→ AppContainerVNextImportAuthority.parseFile
```

The user can review/change:
- date/time column;
- amount column.

The file reparses through the existing parser.

Important:

```text
automatic column guess
!= authority
```

Human review remains visible before commit.

## 7. Second lock after parsing

Raw bytes/head are deliberately page-memory-only.

If the user parses a generic CSV and then leaves the app again before confirming:

```text
lock
→ page-memory raw bytes disappear
→ ImportPreview may remain in Activity-scoped workflow memory
→ mapping-sensitive commit is disabled
→ user is told to reselect the file before confirming
```

This is intentionally conservative.

PDIG does not persist raw statement bytes merely to make the UI feel seamless.

WeChat/OFX previews can continue from their in-memory parsed preview because they do
not require the generic CSV mapping editor.

## 8. Commit truth boundary

R43 Production UI states:

```text
确认导入
→ Source / Evidence / confirmed objects allowed by current pipeline
→ Proposal / Candidate / Drift work
```

It does not state:

```text
确认导入
→ confirmed Dependency
```

Permanent rule:

```text
Import commit != Dependency confirmation
Proposal != Reality
```

If Proposals were generated, the next task is Human Review.

If no Proposal was generated, the next navigation target is Data Sources.

“No Proposal” never becomes “no dependency exists”.

## 9. Failure behavior

R43 distinguishes:
- selected source disappeared;
- selected source adapter mismatch;
- unreadable file;
- unparseable file;
- generic CSV mapping lost after a second lock;
- authoritative commit failure;
- interrupted process requiring re-selection.

Repository validation exceptions are not shown to consumers.

Legacy import was also hardened so an existing-source validation failure no longer
escapes as an uncaught coroutine failure.

## 10. URI permission lifecycle

`FileWorkflowCoordinator.publishImportResult(success)` now:

```text
successful authoritative import
→ release held persistable read URI permission
→ mark workflow DONE
```

The permission is no longer retained until the user leaves the result page.

This narrows the external-document access window without changing Reality semantics.

## 11. Preview remains isolated

Reference Preview still:
- shows Establish hierarchy;
- explains local import and Proposal boundaries;
- never opens the real file picker;
- never reads a user file;
- never commits production Reality.

```text
Preview = reference interaction / truth language
Production VNext = real authority path behind security host
```

## 12. Tests / contracts

New/updated source contracts include:

```text
ProductionImportWorkflowR43Test
FileWorkflow D-16 tests
ProductionVNextImportTest
EstablishImport R23 contracts
SourceRepository existing-source validation path
```

R43 also updates:
- `spec/ui-vnext/ESTABLISH_IMPORT_UX_CONTRACT.md`;
- `spec/ui-vnext/CAPABILITY_AUTHORITY_MATRIX.md`;
- `spec/ui-vnext/PDIG_VNEXT_DESIGN_CLOSURE_MATRIX.md`;
- `ANDROID_UI_VNEXT_CURRENT_REALITY.md`.

## 13. Exact-head runtime evidence still required

The final Production import evidence must prove on API36:

```text
1. unlock Production VNext rehearsal
2. 我 / 数据源 / 建立基础设施
3. select an existing source
4. tap 文件导入
5. real DocumentsUI opens
6. app is locked while external picker is active / on return
7. choose matching file
8. re-authenticate
9. return to the same Establish workflow
10. URI delivered once
11. parse preview visible
12. generic CSV mapping can be changed and reparsed
13. confirm import
14. selected SourceInstance identity preserved
15. proposal appears in Human Review when generated
16. no confirmed Dependency exists before Human Review
17. URI grant is released after success
18. process-death/interrupted path requires re-selection
```

Also test negative existing-source case:

```text
select existing generic_csv source
→ choose OFX file
→ fail closed before commit
→ no duplicate/misleading source created
```

## 14. Remaining product gates

R43 removes “Production VNext real file import is only a design stub” from the list
of source gaps.

Remaining blockers are evidence/cutover or explicitly gated future capability:

```text
exact-head CI
API36 secure import E2E
phone/tablet/GPU visual evidence
human Android Reference acceptance
productionRelease two-key cutover approval
rollback install proof
future Identity Context / Factor / Recovery Solver / Device Continuity /
Digital Resource / Trusted Handoff Canonical packages
iOS/Harmony UI translation after Android Reference Freeze
```

## 15. Stop line

```text
PDIG_VNEXT_PRODUCT_UX_DESIGN = CLOSED_AND_EXTENDED_THROUGH_R43
PRODUCTION_VNEXT_IMPORT_SOURCE_FLOW = COMPLETE
PRODUCTION_VNEXT_IMPORT_RUNTIME_EVIDENCE = PENDING
ANDROID_REFERENCE_FREEZE = HOLD
PRODUCTION_VNEXT_RELEASE_CUTOVER = HOLD

SOURCE_COMPLETE != RUNTIME_VERIFIED != SHIPPED
```
