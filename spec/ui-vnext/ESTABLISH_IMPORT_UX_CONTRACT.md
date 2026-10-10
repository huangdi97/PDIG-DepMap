# PDIG Establish / Import UX Contract

> Status: **DESIGN_FROZEN / R23 REFERENCE + R43 PRODUCTION BINDING**
>
> Scope: user capability **建立** — local file import → object confirmation →
> governed review work.
>
> No Canonical schema change is introduced by this contract.

## 1. Product role

v2.3 defines five user capabilities:

~~~text
建立
确认
理解
变更
维持
~~~

R23 closes the missing VNext surface for **建立**.

The import flow is not a generic file manager. It exists to reduce the work needed
to construct Personal Digital Infrastructure while preserving the truth boundary.

End-to-end:

~~~text
Source file
→ in-memory Observation
→ Node Resolution preview
→ user confirms objects/import
→ SourceInstance / Evidence / confirmed objects as permitted by current production pipeline
→ Proposal / Candidate / Drift work
→ Human Review
→ confirmed Reality relations
~~~

Critical rule:

~~~text
Import commit != Dependency confirmation
~~~

## 2. Information architecture

Establish / Import is **not** a sixth primary destination.

Primary remains:

~~~text
现在 / 基础设施 / 变更 / 记录 / 我
~~~

Canonical VNext entry points:

~~~text
Data Sources → 建立基础设施
Search → 建立基础设施
~~~

Hierarchy:

~~~text
建立基础设施 → Up → 数据源 → Up → 我
~~~

System Back remains chronological.

## 3. Production flow reuse

The existing Android production application already implements the hardened flow:

~~~text
FileWorkflowCoordinator
→ ACTION_OPEN_DOCUMENT / persisted read grant
→ lock / re-auth survival
→ AppContainer.parseFile
→ AppContainer.previewImport
→ Node Resolution
→ AppContainer.commitImport
~~~

VNext must **reuse this pipeline**.

R43 source now binds the full Production VNext host path:

~~~text
Production 建立基础设施
→ choose existing SourceInstance or name a new source context
→ MainActivity-owned OpenDocument launcher
→ app moves to lock state when picker leaves foreground
→ ActivityResult only records URI
→ user re-authenticates
→ same Establish workspace resumes
→ consume pending URI exactly once
→ existing parser / CSV mapping review
→ ImportPreview
→ explicit “确认导入”
→ AppContainer.commitImport
→ generated Proposal → Human Review
~~~

The selected existing SourceInstance id is now carried through the workflow and
authoritative commit. Reusing an existing source requires its parsed adapter to
match; a format mismatch fails closed instead of silently creating a different
source under the same display label.

Forbidden:
- a second ActivityResult launcher inside the VNext screen;
- a second CSV/OFX/WeChat parser;
- bypassing LockGate;
- retaining raw statement data beyond the existing workflow contract;
- direct SQL writes from Compose.

## 4. Three consumer steps

### Step 1 — 选择数据来源

User can:
- choose an existing active SourceInstance;
- create/name a new source context for the import.

R43 Production VNext lists authoritative existing sources from the production
snapshot. Selecting one carries both its id and label through the Activity-scoped
workflow. Editing the label clears that selection and becomes a new-source intent.

The source label describes provenance. It is not a bank connection status.

Current SourceRepository identity semantics remain:

~~~text
new source      → adapter + label determines stable source id
existing source → explicit existing source id + adapter/label validation
~~~

The UI must not claim that two same-label sources using different adapters are the
same source.

### Step 2 — 选择文件并在本机解析

Production:
- user explicitly chooses a file;
- only read permission is requested;
- parser runs locally;
- observations remain in memory as defined by existing production architecture;
- errors/skipped rows are explicit.

Preview:
- does not open a file picker;
- does not read a file;
- does not create a SourceInstance.

### Step 2.5 — 字段对应

For generic CSV only:
- show detected date/amount columns;
- allow user correction;
- reparse through the existing parser;
- do not persist raw file bytes or Observation into Canonical Reality.

### Step 3 — 确认要记录的对象

The preview may show:
- detected payment instruments;
- detected counterparties/services;
- observation count;
- skipped row count.

User confirmation starts the existing authoritative import transaction.

The UI must say clearly:

> 确认导入会记录对象并生成待复核关系；关系在确认前不会参与 Impact。

## 5. Commit result

Consumer-safe result may show:

~~~text
raw observations
new unique observations
duplicates
objects created / resolved
proposals created
errors
~~~

Do not show internal session IDs by default.

Next step:

~~~text
proposalCount > 0 → 待复核
proposalCount == 0 → 数据源
~~~

This is navigation guidance, not an automatic Review decision.

## 6. Proposal / Candidate / Drift boundary

The current production commit pipeline may generate:
- Proposal;
- DiscoveryCandidate;
- RealityDrift.

They remain governed review work.

~~~text
Proposal != Dependency
Candidate != confirmed Node until its candidate authority says so
Drift != graph mutation
~~~

R23 must compose with R22 Human Review rather than duplicating its decisions.

## 7. Preview boundary

The Preview route is intentionally an explanatory reference surface.

Required visible language:
- 建立基础设施;
- 文件留在本机 · 发现不等于依赖;
- 选择数据来源;
- 选择文件并在本机解析;
- 确认要记录的对象;
- 当前是隔离预览.

Preview must never display a fake successful import or fake newly created Proposal.

## 8. Production projection

R23 source may project:
- ImportPreview into a consumer preview;
- ImportCommitResult into a consumer commit result.

It must not expose:
- raw Observation payloads;
- sourceInstance internal IDs as primary UI;
- importSessionId as primary UI;
- raw parser errors/stack traces;
- secrets or full sensitive identifiers.

Production projection is implemented in:

production/ProductionVNextImport.kt

## 9. Security / privacy

Inherited production guarantees remain mandatory:
- local parsing;
- encrypted Reality database;
- external picker result does not unlock the app;
- re-auth required after background lock;
- URI permissions released according to the existing workflow;
- Observation remains in memory only;
- no raw statement ledger retained as product truth.

A VNext visual redesign cannot weaken these guarantees.

## 10. Empty / error states

Distinguish:
- no source configured;
- picker unavailable;
- unreadable file;
- parse failed;
- rows skipped;
- zero detected objects;
- commit failure;
- commit succeeded but generated no review proposals.

Never collapse all failures into “导入失败”.

Never claim:
- “没有依赖” from zero detected proposal;
- “全部导入成功” when rows were skipped;
- “已同步” for a one-time file import.

## 11. Runtime acceptance

Exact-head acceptance should prove:
- Data Sources → Establish;
- Header Up → Data Sources;
- Search → Establish;
- Preview does not expose a real picker action;
- five primary IA remains unchanged;
- truth-boundary copy is visible;
- production adapter unit tests preserve Proposal boundary.

Production exact-head acceptance additionally requires:
- real ACTION_OPEN_DOCUMENT from Production VNext;
- background lock/re-auth;
- file result delivered once;
- same Establish workspace resumes after unlock;
- existing SourceInstance selection survives lock/re-auth;
- mismatched adapter for an explicitly selected source fails closed;
- generic CSV field mapping can be reviewed/corrected while raw bytes remain in memory;
- a second relock after parsing does not persist raw bytes and therefore requires
  re-selection before mapping-sensitive commit;
- parse preview;
- explicit confirm commit;
- selected existing source id remains the committed SourceInstance when valid;
- generated Proposal appears in Human Review;
- no direct confirmed Dependency.

## 12. Stop line

~~~text
ESTABLISH_IMPORT_UX = DESIGN_FROZEN
PREVIEW_IMPORT_REFERENCE = SOURCE_IMPLEMENTED_READ_ONLY
PRODUCTION_IMPORT_PIPELINE = EXISTING_AND_REUSED
PRODUCTION_VNEXT_IMPORT_PROJECTION = SOURCE_IMPLEMENTED
PRODUCTION_VNEXT_FILE_WORKFLOW_BINDING = SOURCE_IMPLEMENTED_R43
PRODUCTION_VNEXT_FILE_WORKFLOW_RUNTIME = PENDING_EXACT_HEAD_EVIDENCE
~~~
