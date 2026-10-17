# PDIG UI vNext — Human Confirmation / Review Center

> Date: 2026-10-10  
> Status: **DESIGN_FROZEN / PRODUCTION_AUTHORITY_EXISTS / VNEXT_SCREEN_NOT_CUT_OVER**  
> Primary navigation change: **NONE**

## 0. Product position

v2.3 freezes the user loop as:

```text
建立
→ 确认
→ 理解
→ 变更
→ 维持
```

R21 already closes most of `理解 / 变更 / 维持`. Human confirmation must remain
visible as a first-class task **without creating a sixth primary tab**.

Permanent authority chain:

```text
Observation
→ Proposal / Candidate / Drift
→ Human Review
→ explicit user decision
→ Personal Reality
```

Visibility is not confirmation.

## 1. Navigation placement

Primary remains:

```text
现在 / 基础设施 / 变更 / 记录 / 我
```

Review is a focused secondary task:

- **Now** may surface “N 项待确认” when production truth reports N > 0;
- **数据源** owns a persistent “待确认” entry because review is source/truth governance;
- **我** may expose a summary, but is not the semantic owner;
- no sixth bottom tab;
- no separate “AI Inbox” product.

Recommended hierarchy:

```text
现在
 └─ 待确认（task shortcut）

我
 └─ 数据源
     └─ 待确认（canonical secondary entry）
```

Header Up from Review → 数据源 when entered through hierarchy.
System Back → actual previous screen.

## 2. Three review classes

### 2.1 关系提案

Source:
`pendingProposals()`

Consumer question:

> **这两个已记录对象之间，是否真的存在这条关系？**

Show only:
- from / to object names;
- consumer relation/capability wording;
- observation count;
- confidence only as supporting machine evidence, never as truth;
- “确认前不会进入事实或影响分析”.

Actions:

```text
确认关系
忽略
```

Authority:
`acceptProposal / rejectProposal`.

### 2.2 候选对象

Source:
`pendingCandidates()`

Consumer question:

> **系统发现的这个对象，是否确实属于你的数字基础设施？**

Show:
- candidate label;
- candidate category in consumer wording;
- observation count;
- source/evidence explanation when production projection later carries it.

Actions:

```text
确认加入
忽略
```

Authority:
`acceptCandidate / dismissCandidate`.

A candidate does not participate in Impact before acceptance.

### 2.3 现实变化 / Drift

Source:
`openDrifts()`

Consumer question:

> **系统看到了与已确认现实不一致的新证据，实际发生了什么？**

For replacement-style drift:

```text
已更换
两者都在用
没变化
稍后确认
```

Rules:
- positive evidence only;
- absence alone cannot create Drift;
- “稍后确认” does not mutate Reality;
- displayed detection time is evidence timing, not user confirmation time.

Authority:
`resolveDriftAsReplacement / resolveDriftAsAdditionalPath / dismissDrift`.

## 3. Visual hierarchy

Android light-first:

```text
待确认
N 项等待你的判断
│
├─ 关系提案
│   A → B
│   为什么出现
│   [确认关系] [忽略]
│
├─ 候选对象
│   对象身份 + 已观测次数
│   [确认加入] [忽略]
│
└─ 可能发生了变化
    对象 + 观测依据
    [已更换] [两者都在用] [没变化] [稍后确认]
```

No admin-console table.
No confidence KPI wall.
No “AI says 91% true” hero.

Each decision card answers:
1. 系统看到了什么；
2. 现在还不确定什么；
3. 用户的选择会改变什么。

## 4. Truth language

Allowed:

```text
待确认
系统观察到
可能
依据 N 条记录
确认前不会作为事实
尚未进入影响分析
```

Forbidden:

```text
已发现事实
自动确认
AI 已验证
高置信度 = 真实
未确认 = 不存在
忽略 = 删除历史证据
```

## 5. Mutation UX

When user chooses an authoritative action:

```text
tap
→ disable duplicate action
→ call production gateway
→ authoritative mutation transaction
→ re-read queue
→ update UI from returned queue
```

Do not:
- optimistically remove then assume success;
- mutate local Compose list only;
- infer a graph revision;
- fabricate an accepted relation/object.

R21 source seam:
- `ProductionVNextReview.kt`
- `AppContainerVNextReviewSource`
- `AppContainerVNextReviewActionGateway`

## 6. Preview boundary

Preview must not fabricate a populated review inbox merely to produce screenshots.

Allowed Preview states:
- explanatory empty state;
- static contract examples in documentation/reference image only.

Not allowed:
- synthetic proposal visually presented as a user's actual pending discovery;
- “确认” button that only changes local UI but looks authoritative.

Therefore the live Review Center route remains gated until production VNext screen
binding is enabled or a clearly labelled dedicated review fixture harness exists.

## 7. Now integration

Now should eventually use production `pendingReview` count.

If count > 0:

```text
需要你确认
3 项新发现
关系 1 · 对象 1 · 变化 1
[去确认]
```

If count == 0:
- do not show “一切正常”;
- either omit the block or say “当前没有待确认项目”;
- unknown outside observed sources remains unknown.

## 8. Records integration

Review queue is **not Records**.

Only an actual review decision/event may later enter Records when the domain has a
durable event/timestamp source.

Pending proposal/candidate/drift:
```text
Review task
!= historical event
```

Do not duplicate the queue in Records.

## 9. Security / privacy

- no raw secrets / credentials;
- object identifiers shown as consumer names, not internal IDs;
- masked identifiers obey global privacy setting;
- observation detail must not leak imported raw source rows unless explicitly
  designed as an evidence drill-down;
- decisions use existing encrypted production Reality path.

## 10. Acceptance

Source/architecture:
- [x] production read APIs exist;
- [x] production mutation APIs exist;
- [x] VNext review queue projection exists;
- [x] VNext review action gateway delegates to AppContainer and re-reads;
- [x] unknown node names do not leak internal IDs;
- [ ] production VNext screen injection;
- [ ] exact-head runtime mutation evidence;
- [ ] pixel/human acceptance.

Final gate:

```text
REVIEW_AUTHORITY = SOURCE_CONNECTED
REVIEW_VNEXT_VISIBLE_SCREEN = HOLD_UNTIL_PRODUCTION_BINDING
SIXTH_PRIMARY_TAB = FORBIDDEN
```
