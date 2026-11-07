# R49 — Android Continuity, Change Workspace, Weaknesses and Records: report

> Round: R49 within the R48–R51 local-agent round
> Exact source under test: `bc39a2f2499f7d95e2249719d7daf5b384ab5faa`
> Evidence: `artifacts/runtime-evidence/2026-10-11-r48-r51/{phone,tablet,production}/`

## 1. Scope honesty statement

As with R48, the continuity/change surfaces already existed at this HEAD (R19/R20/R21/R31). This
round verified them **at runtime on a real device** and repaired the two defects that verification
found. Nothing here rebuilds the model, and no assertion was relaxed.

## 2. Change workspace — verified at runtime

### 2.1 Server-backed production path (authority, not preview)

`ProductionManualRelationshipContractTest.productionFormCommitsCurrentRuntimeRelationThroughAuthority`
runs the real authority chain on isolated test data and now **passes**:

| Step                                                           | Evidence                                                                                                                |
| -------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------- |
| manual object creation through `AppContainer.createManualNode` | account + service nodes created via the governed manual-create authority                                                |
| form commit through the relationship authority                 | `pdig.production-vnext.manual-relation.save` produces a `result` node                                                   |
| authoritative read-back                                        | `app.dependencies().single()`; `from`, `to`, `relation=merchant_agreement`, `capability=payment`, `criticality=unknown` |
| graph revision                                                 | `graphRevision()` advanced by exactly 1                                                                                 |
| raw row provenance                                             | `origin='manual'`, `verification_basis_type='user_confirmed'`                                                           |
| count invariant                                                | `dependencies().size == 1`                                                                                              |

`ProductionVNextShellContractTest`: **11 tests, 0 failures** on the API36 emulator.

### 2.2 Plan-project projection: Current / Transition / After are distinct

Verified on both form factors (`phone/04h…04k`, `tablet/27-card-change-current.png`,
`28-card-change-transition-blocked.png`, `29-card-change-after-plan.png`):

- `当前` shows the old card plus `已记录支付关系` and per-service status `当前依赖`; the stage
  panel states the current view is only the old card and its recorded relationships.
- `迁移中` changes the per-service status to `待迁移` and the continuity rail to
  `检查支付依赖 已完成 / 迁移支付关系 待验证 / 验证支付路径 阻止`, with the explicit
  `支付关系迁移完成并取得实际验证证据后，才能确认新路径`.
- Selecting a real `active` replacement changes the panel to
  `迁移中的关系仍需逐项完成，并在最后阶段用实际证据验证。` and the candidate reports
  `仅表示已记录候选 · 已选计划目标`.
- `完成后（计划）` renders `完成后是计划投影：不表示支付关系已经迁移，也不表示新卡已经验证可用。`
  and per-service `计划迁移`. **After is presented as a plan projection, never as reality.**

`CardChangeR21Test` (JVM) pins the same semantics independently of the UI.

### 2.3 Change Phone continuity choreography

`phone/05-change-center.png`, `05-change.png`, `05a-change-checklist.png`,
`05aa-change-checklist-footer.png`, `05b-change-plan.png` prove: the Change Center lists
`更换手机号（进行中）` with `正在验证新号码 · 旧号码继续保留`; the change surface reaches
`影响分析 · 关键服务` with `旧手机号`; the stage checklist shows `本阶段待办`; its footer states
`不会通过点击自动标记已完成`; and the After projection renders `完成后预览` with
`完成后预览：仅为计划投影；未确认的迁移和恢复路径绝不显示为已完成。`

The `old active ≠ keep ≠ new target` distinction is visible in that same projection
(`旧手机号 / +86 138****8823` against `新手机号 · 目标 / +86 139****6421`).

## 3. Impact Lens — verified, with evidence, and honest about the unknown

Every object surface answered "如果它发生变化？" with the same fact set
(`已确认依赖 / 需要关注 / 唯一恢复路径 / 关键账户 / 独立备用方式 / 未确认关系`) and the boundary
line `只基于已记录关系；未确认内容不会被推断为安全或必须处理。`

Concrete observed values:

| Object        | Facts                                                                                                                |
| ------------- | -------------------------------------------------------------------------------------------------------------------- |
| 招行储蓄卡    | `2 已确认依赖`, `0 需要关注`, `未知 唯一恢复路径`, `关键账户 未记录`, `独立备用方式 未记录`, `未确认关系 仍可能存在` |
| 微信账户      | `0 / 0 / 未知 / 关键账户 未记录`, plus `唯一恢复 未知`                                                               |
| 微信支付      | `2 已确认依赖`, `未确认关系 仍可能存在`                                                                              |
| 主号 中国移动 | `唯一恢复路径` present, `未确认关系 仍可能存在`                                                                      |

The lens never prints a health score or a self-invented percentage, and a `未记录` state is shown as
`未记录`, never as zero-risk.

Weaknesses (`phone/07-weaknesses.png`) deliver the required "why / on what confirmed reality / what
is still unknown / what can be done" structure for `单点路径`, `共享故障点` and `恢复循环`, each with
`已确认事实`, `为什么`, `依据`, `仍未知`, `下一步`. FailureDomain independence is not assumed: the
shared-failure finding explicitly states the two paths depend on the same device and therefore
cannot be counted as two independent paths.

## 4. Records — evidence trace, not a second attention feed

`phone/06-records.json` shows `发生过什么、验证过什么、依据是什么。关注事项和未来维护留在“现在”。`
plus the summary metrics `已记录完成 / 已验证 / 待验证` and the `验证状态` section title
`完成 ≠ 已验证`. The harness additionally asserts that the surface contains **neither** `需要关注`
nor `即将到来`, i.e. Records has not regressed into a duplicate Now/attention feed.

`REVIEW -> NOW` is the declared up-destination and the runtime confirms it: `合力` up from the
Human-Review workspace lands on Now (`10h-review-up-now`).

## 5. Defects found and fixed in this round

1. **Transition stage did not show its blocking reason** (see R48 §4.1). Fixed in the product; the
   assertion was untouched.
2. **`ProductionManualRelationshipContractTest` could not reach the `to` option.** The form is a
   `LazyColumn`, so an option below the fold has **no semantics node at all**. The test now scrolls
   the real list to the option (`performScrollToNode`) before clicking — the honest user path — and
   every authority assertion is unchanged. The previous failure was
   `java.lang.AssertionError: Failed to inject touch input. Reason: Expected exactly '1' node but
could not find any node that satisfies: (TestTag = 'pdig.production-vnext.manual-relation.to.<id>')`.
3. **`ci_emulator_production_rehearsal.sh` never reinstalled the app** after `connectedAndroidTest`
   (AGP uninstalls both APKs after a connected run), so the lock-gated rehearsal step died with
   `Error: Activity class {com.pdig.app/com.pdig.app.MainActivity} does not exist.` and never ran.
   Fixed by reinstalling the APK before the rehearsal.
4. **Both rehearsal scripts waited for a stale marker** (`生产 Reality`, now only present on the Me
   workspace page). They now wait for the Production Now surface's own title and additionally forbid
   that marker while locked and on the legacy route, which strengthens the fail-closed and
   no-silent-cutover checks.

After these fixes the whole production chain passes locally:
`ProductionVNextShellContractTest` 11/11, `ProductionManualRelationshipContractTest` 1/1,
`PRODUCTION_VNEXT_SECURE_REHEARSAL=PASS`, `PRODUCTION_VNEXT_RELEASE_DEFAULT_REHEARSAL=PASS`.

## 6. What is NOT claimed

- `completeAction` / `verifyAction` read-back on real **payment** rails is not exercised: the preview
  change surfaces do not execute local state changes and say so in their own copy
  (`不会通过点击自动标记已完成`). The governed commit path that **is** proven is the manual
  relationship authority above.
- No human visual acceptance; no reference freeze.
