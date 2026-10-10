# ANDROID_REFERENCE_MAPPING.md

> **R21 · 2026-10-09 · Android Light Reference source contract**
>
> This file maps the v2.3 product architecture and the human-selected light reference
> board to Android native layouts. It is a **source/design contract**, not proof of
> runtime pixel acceptance.
>
> Truth order:
>
> ```text
> Canonical spec / invariants
> → confirmed runtime domain facts
> → v2.3 product architecture
> → this Android translation contract
> → synthetic reference fixture
> → screenshots
> ```
>
> Screenshots and fixture data never override Canonical truth.

## 0. Frozen Android information architecture

**R19 product decision override:** `我` is an intentional fifth primary destination.
Do not demote it to a toolbar/avatar-only utility in later cleanup.

Primary navigation is exactly:

```text
现在
基础设施
变更
记录
我
```

Phone uses five equal primary destinations in the bottom navigation. Medium/Expanded
use the same five destinations in the primary rail. The top-right avatar may remain
as a convenience shortcut, but it does not replace the primary `我` destination.

Infrastructure secondary destinations remain:

```text
总览 / 卡片 / 号码 / 账户 / 邮箱 / 设备 / 服务 / 薄弱点
```

Phone exposes these from the Infrastructure management hub. Medium/Expanded may
use content-level sibling navigation. They must not become global primary nav.

Android window classes follow the actual source contract:

```text
COMPACT  < 600dp
MEDIUM   600–839dp
EXPANDED >= 840dp
```

## 1. Now

### PRESERVE
- task-first hierarchy: attention before generic statistics;
- signature Globe as spatial context;
- active Change state;
- upcoming/maintenance records;
- explicit unknown states.

### TRANSLATE
- phone: vertical consumer feed with a frameless world hero;
- wider screens: more spatial room, without converting the page to a KPI wall.

### R19 reference details
- GPU Earth remains the preferred renderer;
- projected region callouts follow the live camera;
- callouts show factual card/number context and an attention marker only when
  recorded attention exists;
- the asset rail opens Cards / Numbers / Accounts / Services.

### FORBIDDEN
- decorative fake graph edges;
- fixed-corner country labels that do not follow the camera;
- “0 issues = safe”;
- synthetic counts presented as production reality.

## 2. Infrastructure Overview

### PRESERVE
- eight-category management hub;
- region distribution;
- region selection as a real filter;
- accessible region list in addition to the Globe.

### PLATFORM-ADAPT
- phone: search → 8-category hub → compact region distribution;
- Medium: single-pane spatial overview;
- Expanded: spatial stage + activity rail + quick entries.

### FORBIDDEN
- copying Now as a second giant dashboard;
- persistent horizontal Infrastructure tabs on phone;
- interpreting an empty region as “no risk”.

## 3. Globe

The Globe is a **spatial context object**, not a background illustration.

### Renderer
- bundled albedo / night-lights / cloud textures;
- spherical GPU shading;
- atmosphere / limb / exterior haze;
- day/night distinction;
- ocean highlight;
- CPU fallback if GPU initialization fails.

### Region annotations
- geographic anchors use the same live camera/projection as the Earth;
- visible labels are collision-budgeted;
- dense nearby regions may cluster and open a chooser;
- a tether connects a label to its geographic anchor only;
- the tether is **not** a dependency edge;
- labels may show card count, number count and recorded attention.

### Required runtime states
```text
loading
GPU ready / TEXTURE_READY
fallback
error
```

A source implementation cannot claim the runtime state without fresh evidence.

## 4. Cards — Financial Asset Identity

### PRESERVE
- strong card identity / collection feeling;
- issuer, region, currency, type, form, network, expiry;
- confirmed service dependencies;
- local presentation customization.

### R19 lifecycle density
List/detail may show recorded:
```text
annual fee
annual-fee checkpoint
billing day
payment due day
installment summary
recorded autopay summary
```

Missing = `未记录`.

### PLATFORM-ADAPT
- phone: dense vertical asset list;
- Medium/Expanded: list + inspector, with focused detail still available.

### FORBIDDEN
- rewards optimization becoming the product;
- inventing a statement balance or minimum payment;
- treating PresentationProfile as financial truth.

## 5. Card Detail

Hierarchy:

```text
Card Identity
→ Lifecycle
→ Confirmed Dependencies
→ Impact Lens
→ Actions / Presentation
```

The former fake static “history” copy is removed. History may only return when a
real source/timeline record exists.

`Impact Lens` answers **“如果它发生变化？”** but does not invent:
- critical accounts;
- independent alternatives;
- unique recovery facts;
- a safety score.

Because `replace_payment_card` is an active production scenario, R21 Card Detail
exposes **分析更换此卡的影响** and enters the focused Change Card route. Preview
remains projection-only; production execution must bind the existing payment
scenario rather than creating a parallel demo workflow.

## 6. Card appearance customization

Card appearance is a small presentation feature, not a separate financial product.

### PRESERVE
- same card renderer in list/detail/editor;
- local image import;
- bundled presets;
- local masking preference;
- live preview.

### Boundary
```text
PresentationProfile != PersonalReality != Canonical
```

Changing art, material, accent, layout or masking never changes dependencies,
risk, lifecycle or .depmap truth.

## 7. Numbers — Communication Identity

Numbers are communication/authentication/recovery infrastructure, not contacts and
not bank-card-shaped objects.

### Roles
```text
primary
secondary
keep
```

`keep` is a real consumer role, not a visual theme.

### Dense list facts
- user-visible alias;
- masked number;
- region/carrier;
- SIM/eSIM;
- role;
- recorded recovery use;
- explicit unique-recovery evidence when it exists;
- lifecycle/keep-alive summary.

### Critical truth rule
```text
recoveryOnly != uniqueRecoveryPath
```

A number can be used for recovery without being proven to be the only recovery
path.

## 8. Number Detail

Hierarchy:

```text
Communication Identity
→ Lifecycle / Keep-alive
→ Recorded Recovery Semantics
→ Confirmed Service Dependencies
→ Impact Lens
→ Change
```

Lifecycle may show:
```text
billing mode
plan cost
keep-alive due
keep-alive cycle
last action
renewal / keep-alive method
```

The detail distinguishes:
- **已确认唯一恢复路径** — only with explicit evidence;
- **恢复用途，唯一性未知**;
- **恢复关系未知**.

No path-count or “safe alternative” is fabricated.

Because `replace_phone_number` is a production REPLACE scenario, Number Detail may
offer **分析更换号码影响**. The reference UI still must not claim the synthetic
plan is the user's real plan.

## 9. Keep-number management

A long-lived keep-number asset and a new migration target are different objects.

Reference invariant:

```text
old active number != keep-number asset != new migration target
```

R19 fixture demonstrates:
```text
num-cn-1 old active
num-cn-3 keep-number
num-cn-4 migration target
```

These IDs are fixture-only; the separation is permanent product semantics.

Keep-alive due dates are recorded facts, not carrier live-state assertions.
Completion of a carrier action must require an explicit user/runtime event.

## 10. Impact Lens

Every focused core-object detail should converge on:

> **如果它发生变化？**

Current reference fields:

```text
confirmed dependencies
attention findings
critical accounts          = value / 未记录
unique recovery path       = 已确认 / 未知
independent alternatives   = value / 未记录
unknown relations          = still possible / none only if proven
```

### FORBIDDEN
- health/safety percentage;
- degree-count shortcuts for path independence;
- `null → 0`;
- `recovery use → unique recovery`;
- proposals/candidates presented as confirmed Reality.

## 11. Change — Primary work center

The primary `变更` destination is **not** an alias for one hard-coded phone flow.

It answers:

> **我正在改变什么？我准备改变什么？**

R20 source composition:

```text
正在进行的变更
→ exact active Change entry

准备改变
→ supported/scoped entry points

维护与核对
→ Weaknesses / Records
```

Reference actions:
- active Replace Phone → focused Change Phone choreography;
- Prepare Replace Phone → focused Change Phone choreography;
- Replace Card → select a concrete Card → inspect Impact → focused R21 Change Card choreography.

The Change root may describe a supported scenario, but executable mutation remains
bound to production `ChangePrimitive / Scenario / ChangePlan` capability.

### FORBIDDEN
- `变更` root immediately rendering one specific object's detail flow;
- a generic “start change” button for unsupported primitives;
- marking a plan executed because the user opened the Change Center;
- treating Prepare / Change / Records as the same state.

## 12. Change Card — Payment continuity

R21 adds the supported payment replacement reference.

Hierarchy:

```text
Card Detail Impact Lens
→ Change Card
→ Current / Transition / After
→ OLD CARD → recorded payment relations → optional NEW CARD
→ 3-stage Continuity
```

Stages mirror production `replace_payment_card`:

```text
检查支付依赖
迁移支付关系
验证支付路径
```

Truth rules:
- candidate replacement != recommendation;
- migration complete != payment path verified;
- verification requires real evidence in production;
- After = Plan Projection only;
- Change Card Up → Change;
- Search “换卡” first routes to Cards so the user selects a target.

## 13. Change Phone — Continuity choreography

Three projections remain semantically distinct:

```text
Current
Transition
After = Plan Projection
```

### PRESERVE
- Make-Before-Break;
- old number → affected services → new number;
- six-stage continuity sequence;
- explicit verification;
- blocked retirement when prerequisites are unmet.

R19 derives the compact impact summary from the actual projection state:

```text
已记录关联
需要核对
阻断 / 待解决
```

It does not copy showcase-only hard-coded numbers.

`done != verified` remains mandatory.

## 14. Accounts / Emails / Devices / Services / Weaknesses

These remain Infrastructure secondary objects rather than new primary tabs.

### Emails
`recoveryOnly` renders as **恢复用途**.
Only explicit `uniqueRecoveryPath == true` may render **唯一恢复**.

### Weaknesses
Only evidence-backed findings belong here. Merely having a recovery role is not
itself a weakness.

R20 source now gives Account / Email / Device / Service focused details the same
consumer hierarchy:

```text
Object Identity
→ Recorded Context / Confirmed Relations
→ Impact Lens
→ Change / Recovery (only when a production primitive exists)
```

Specific object identity remains distinct:
- Account = access / control identity;
- Email = communication / recovery identity;
- Device = physical access endpoint;
- Service = dependency endpoint.

Rows and Search results open the focused object directly. System Back returns to the
actual previous page; Header Up returns to the corresponding Infrastructure
collection.

The R20 focused details remain read-only when no production ChangePrimitive exists.
They must not invent a generic “开始变更” CTA merely to make every detail page look
symmetric.

## 15. Me — Personal Digital Life Workspace

`我` is the fifth primary destination, not a utility downgrade.

### COMPACT
- consumer feed composition;
- key identities first;
- continuity summary;
- privacy toggle;
- global distribution;
- personal management actions.

### MEDIUM
- compact 80dp primary rail;
- bounded single-column Me workspace to protect reading width;
- all five primary destinations remain directly reachable.

### EXPANDED
- dedicated two-column workspace;
- critical identities + continuity as the first row;
- infrastructure summary;
- global distribution + personal management;
- Settings / Sources remain children of the Me context.

### FORBIDDEN
- avatar-only Me;
- Up arrow on the Me root;
- stretching the phone feed across a wide tablet;
- turning Me into a generic settings list;
- inferring account completeness or safety from summary counts.

## 16. Records — evidence trace

Records answers:

> **发生过什么、验证过什么、依据是什么？**

R21 hierarchy:

```text
已记录完成 / 已验证 / 待验证
→ recorded change trace
→ verification boundary
→ active-plan context links
→ evidence/source boundary
```

Attention and Upcoming remain on Now; they are not duplicated into Records.

Permanent rule:

```text
done != verified
```

Production Records must bind Timeline / ChangePlan / Review / Verification /
Evidence instead of reverse-engineering historical truth from Attention.

## 17. Search

Search is lookup over **recorded** infrastructure, not a discovery engine.

R20 indexes:
- object names and user number aliases;
- issuer/carrier/region;
- lifecycle facts such as annual fee, billing day and keep-alive due date;
- consumer role labels such as “保号”.

No match means “not found among recorded data,” not “does not exist.”

## 18. Back / Up semantics

Two different operations remain distinct:

```text
Android system Back = chronological previous screen
Header Up          = product hierarchy parent
```

Examples:

```text
Card Detail → Up → Cards → Up → Infrastructure
Number Studio → Up → Number Detail
Me root → no Header Up
Me → Settings → Up → Me
Me → Sources → Up → Me
```

The header must never be relabeled as “Back to desktop/home.”

## 19. Privacy / masking

Default is **not masked** unless the user enables masking.

Masking is:
- a local presentation preference;
- independent of card/number artwork;
- never a mutation of canonical identity;
- applied consistently to list/detail/search.

A user-defined number alias may remain visible when it is non-sensitive; an alias
that itself looks like a phone number must still be protected by masking logic.

## 20. Android Light visual language

Android is **light-first**.

Target character:

```text
consumer-facing
quiet
precise
asset-first
spatial only where meaningful
high information density without admin-console feel
```

Avoid:
- global dark/cosmic background;
- generic gray Material rows everywhere;
- engineering vocabulary;
- giant empty wide panes;
- KPI walls;
- desktop pixel copying.

## 21. Runtime / Freeze boundary

R19 source completeness is not Reference Freeze.

The acceptance chain is:

```text
exact remote HEAD
→ fresh build
→ unit / instrumentation gates
→ API36 phone capture
→ tablet capture
→ GPU/runtime-state evidence
→ human pixel review against reference board
→ ANDROID_REFERENCE_FREEZE decision
```

Old R17/R18 screenshots cannot prove R19.

Until that chain finishes:

```text
ANDROID_REFERENCE_FREEZE = HOLD
iOS/Harmony UI translation = HOLD
Production launcher cutover = HOLD
```

## 18. R22 Human Review

Human Review closes the governed discovery→Reality boundary.

Route:
```text
/review
```

Information architecture:
```text
Now → 待复核
Data Sources → 待复核
Search → 待复核
Review → Up → Now
```

Review is not a sixth primary destination. The five primary destinations remain:

```text
现在 / 基础设施 / 变更 / 记录 / 我
```

The surface contains three distinct classes:

```text
关系建议      = Dependency Proposal
对象候选      = Discovery Candidate
现实漂移      = Reality Drift
```

Preview is read-only and must visibly explain `发现 ≠ 事实`.

Production binding:
```text
AppContainerVNextReviewSource
→ VNextProductionReviewQueue
→ ProductionReviewConsumerInbox
→ Review UI

formal decision
→ AppContainerVNextReviewActionGateway
→ re-read authoritative queue
```

Forbidden:
- numeric confidence presented as truth probability;
- Proposal counted as confirmed Dependency;
- Candidate rendered as confirmed Node;
- Drift silently modifying Reality;
- empty Review presented as “graph complete”;
- Review becoming a sixth primary tab.

## 19. R22 Now / Records boundary

```text
Now
  pending review
  current attention
  active changes
  upcoming maintenance

Records
  occurred / completed / verified / pending-verification evidence trace
```

Pending/current/future items must not use Records as a generic “查看全部” bucket.

Active Change links go to Change Center. Human Review links go to Review. Upcoming
maintenance remains in Now until it becomes an actual recorded occurrence.


## 20. R23 Establish / Import

R23 maps v2.3's **建立** capability into a focused Evidence-plane workflow.

~~~text
Data Sources
→ 建立基础设施
→ local parse / object confirmation
→ Proposal / Candidate / Drift
→ Human Review
→ Reality
~~~

Preview only explains the flow. It does not open a file or fabricate an import.

Production translation must reuse the existing Android file workflow and import
repositories. Import commit may create review work, but never a confirmed
Dependency directly.

Required visual hierarchy:
- local-first/truth hero;
- three clear steps;
- explicit Preview-disabled boundary;
- Data Sources / Human Review continuation links.

Forbidden:
- sixth primary tab;
- "sync" vocabulary for one-time file import;
- fake successful import in Preview;
- Proposal shown as confirmed relation;
- new VNext parser/picker that bypasses existing lock/re-auth behavior.


## 21. R24 Manual Establish

Manual establishment is a focused child of Establish, not another primary mode.

~~~text
Data Sources
→ 建立基础设施
  → 文件导入
  → 手工记录
~~~

Reference hierarchy:
- object-existence truth boundary;
- currently runtime-creatable Canonical kinds;
- known but gated kinds;
- explicit no-Save Preview boundary;
- future authoritative mutation sequence.

The current runtime creation set is narrower than storage NodeKind. VNext must not
turn storage support into a ghost consumer capability.

Identity rule:
- identity_anchor stays generic;
- no manual Number/Email creation until governed subtype mapping exists.

Forbidden:
- local Compose save into Reality;
- relationship creation as a side effect of object creation;
- fifth-primary IA changes;
- fake Save button in Preview.


## 22. R25 Manual Relationship

Manual graph construction remains a focused Establish child.

~~~text
confirmed From
→ runtime-valid Relation
→ confirmed To
→ Capability
→ Criticality
~~~

Reference defaults criticality to `unknown`. `required` is an explicit human
decision only.

Current runtime relations:
- funding_source;
- merchant_agreement;
- recovers;
- authenticates;
- controls.

Storage-only legacy/future verifies/bound_to remain HOLD.

The Preview has no confirm mutation. Production binding must use a future
AppContainer-facing manual Dependency authority that validates the canonical
relation/capability registry and bumps graphRevision transactionally.

Independent-path judgment remains Continuity-engine work, never a checkbox or
degree count in this page.
