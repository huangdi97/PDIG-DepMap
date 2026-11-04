import { useEffect, useState, type ReactNode } from 'react'
import { useRuntime } from '../../app/store.tsx'
import type { ImpactSnapshot, PlanDetailView } from '../../runtime/protocol.ts'
import { Badge, Button, EmptyState, Loading } from '../../components/primitives/index.tsx'
import { Sensitive } from '../../privacy/Sensitive.tsx'

const VERIFICATION_LABEL: Record<string, string> = {
  not_required: '无需验证',
  pending: '待验证',
  evidence_suggested: '证据提示可能已生效',
  verified: '已验证',
  failed: '验证失败',
}

export function PlanView({ planId }: { planId: string }): ReactNode {
  const { runQuery, runCommand, revision, notify } = useRuntime()
  const [detail, setDetail] = useState<PlanDetailView | null>(null)
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    void runQuery({ type: 'get_plan_detail', id: planId }).then((result) => {
      if (cancelled) return
      setDetail(result)
      setLoading(false)
    })
    return () => {
      cancelled = true
    }
  }, [runQuery, planId, revision])

  if (loading && !detail) return <Loading label="正在读取计划与影响" />
  if (!detail) return <EmptyState title="计划不存在" detail="它可能已被删除。" />

  const { plan, summary, actions } = detail
  const stale = summary.stale
  const allVerified = actions
    .filter((view) => view.action.phase !== 'prepare')
    .every(
      (view) =>
        view.action.verification?.status === 'verified' || view.action.verification === null,
    )

  const reanalyze = async (): Promise<void> => {
    setBusy('reanalyze')
    const result = await runCommand({ type: 'analyze_plan', planId: plan.id })
    setBusy(null)
    if (result) {
      notify(
        result.revisionChanged
          ? `已完成重新分析：新增 ${result.addedImpacts} 项影响`
          : '当前已是最新分析基准，未产生差异',
        'success',
      )
    }
  }

  const complete = async (actionId: string): Promise<void> => {
    setBusy(actionId)
    const result = await runCommand({ type: 'complete_action', planId: plan.id, actionId })
    setBusy(null)
    if (result) notify('已记录完成（尚未验证）', 'success')
  }

  const verify = async (actionId: string): Promise<void> => {
    setBusy(actionId)
    const result = await runCommand({ type: 'verify_action', planId: plan.id, actionId })
    setBusy(null)
    if (result) notify('已记录验证', 'success')
  }

  const transition = async (state: 'in_progress' | 'verifying' | 'completed'): Promise<void> => {
    setBusy(state)
    const result = await runCommand({ type: 'transition_plan', planId: plan.id, state })
    setBusy(null)
    if (result) notify(`计划状态：${result.state}`, 'success')
  }

  return (
    <div className="stack" data-testid="plan-view">
      <header className="page-head">
        <div>
          <p className="page-kicker">变更 · {plan.scenario}</p>
          <h1>{plan.title}</h1>
          <p className="page-lede">
            目标对象：{summary.targetName ? <Sensitive value={summary.targetName} /> : '未指定'} ·
            分析基准 revision {plan.lastAnalyzedGraphRevision} → 当前 {summary.currentGraphRevision}
          </p>
        </div>
        <div className="page-actions">
          <Badge tone={stale ? 'warn' : 'neutral'}>
            {stale ? 'needs_revalidation' : plan.workflowState}
          </Badge>
          <Badge tone="neutral">readiness: {detail.readiness}</Badge>
        </div>
      </header>

      {stale ? (
        <div className="alert alert-warn" role="alert" data-testid="stale-banner">
          <strong>基础设施已发生变化</strong>
          <p>
            当前 graphRevision
            高于本计划的分析基准。请先重新分析当前影响；在此之前不能记录完成、验证动作或结束计划。
          </p>
          <Button variant="primary" onClick={() => void reanalyze()} disabled={busy !== null}>
            {busy === 'reanalyze' ? '正在重新分析…' : '重新分析当前影响'}
          </Button>
        </div>
      ) : null}

      <div className="change-stages">
        <section className="panel stage" aria-label="Current">
          <h2>Current</h2>
          <p className="stage-note">当前已确认现实下，如果该对象现在失效会压到哪些路径。</p>
          <ImpactTable snapshot={detail.current} emptyLabel="当前无关可分析的已确认影响" />
        </section>

        <section className="panel stage" aria-label="Transition">
          <h2>Transition</h2>
          <p className="stage-note">动作顺序由前置关系决定；完成后仍需独立验证。</p>
          <ul className="action-list">
            {actions.map((view) => {
              const action = view.action
              const verification = action.verification
              const blocked = view.blockedByPrerequisites.length > 0
              const alreadyDone = action.done
              return (
                <li key={action.id} className={`action-row ${alreadyDone ? 'done' : ''}`}>
                  <div className="action-head">
                    <span className="action-order">{view.order}</span>
                    <span className="action-title">{action.title}</span>
                    <Badge tone="neutral">{action.phase}</Badge>
                    {alreadyDone ? (
                      <Badge tone="ok">已记录完成</Badge>
                    ) : (
                      <Badge tone="neutral">未开始</Badge>
                    )}
                    {verification ? (
                      <Badge tone={verification.status === 'verified' ? 'ok' : 'warn'}>
                        {VERIFICATION_LABEL[verification.status] ?? verification.status}
                      </Badge>
                    ) : null}
                  </div>
                  <p className="action-detail">{action.detail}</p>
                  {blocked ? (
                    <p className="action-blocked" data-testid="action-blocked">
                      必须先完成：{view.blockedByPrerequisites.join('、')}
                    </p>
                  ) : null}
                  {action.doneAt ? (
                    <p className="action-time">记录完成时间：{action.doneAt}</p>
                  ) : null}
                  {verification?.verifiedAt ? (
                    <p className="action-time">
                      验证时间：{verification.verifiedAt}（方法：{verification.method}）
                    </p>
                  ) : null}
                  <div className="action-actions">
                    <Button
                      onClick={() => void complete(action.id)}
                      disabled={stale || alreadyDone || blocked || busy !== null}
                      title={stale ? '基础设施已变化，需先重新分析' : undefined}
                    >
                      记录完成
                    </Button>
                    <Button
                      variant="primary"
                      onClick={() => void verify(action.id)}
                      disabled={
                        stale ||
                        !alreadyDone ||
                        !verification ||
                        verification.status === 'verified' ||
                        verification.status === 'not_required' ||
                        busy !== null
                      }
                      title={stale ? '基础设施已变化，需先重新分析' : undefined}
                    >
                      验证通过
                    </Button>
                  </div>
                </li>
              )
            })}
          </ul>
        </section>

        <section className="panel stage stage-projection" aria-label="After plan projection">
          <h2>
            After <Badge tone="warn">Plan Projection · 未发生</Badge>
          </h2>
          <p className="stage-note">这是分析时的计划投影，不代表现实已经改变。</p>
          <ImpactTable snapshot={detail.projection} emptyLabel="计划尚无影响投影" />
        </section>
      </div>

      <section className="panel">
        <h2>分析基准与差异</h2>
        {detail.lastRebase ? (
          <>
            <p className="form-note">
              分析基准：revision {detail.lastRebase.fromRevision} → {detail.lastRebase.toRevision}（
              {detail.lastRebase.at}）
            </p>
            <div className="delta-grid">
              <div>
                <h3>新增影响 {detail.lastRebase.diff.addedImpacts.length}</h3>
                <ul className="delta-list">
                  {detail.lastRebase.diff.addedImpacts.map((item) => (
                    <li key={`${item.nodeId}-${item.capability}`}>
                      {item.nodeId} · {item.capability} · {item.status}
                    </li>
                  ))}
                </ul>
              </div>
              <div>
                <h3>移除影响 {detail.lastRebase.diff.removedImpacts.length}</h3>
                <ul className="delta-list">
                  {detail.lastRebase.diff.removedImpacts.map((item) => (
                    <li key={`${item.nodeId}-${item.capability}`}>
                      {item.nodeId} · {item.capability}
                    </li>
                  ))}
                </ul>
              </div>
              <div>
                <h3>变化影响 {detail.lastRebase.diff.changedImpacts.length}</h3>
                <ul className="delta-list">
                  {detail.lastRebase.diff.changedImpacts.map((item) => (
                    <li key={`${item.nodeId}-${item.capability}`}>
                      {item.nodeId} · {item.capability} → {item.status}
                    </li>
                  ))}
                </ul>
              </div>
            </div>
          </>
        ) : (
          <p className="form-note">本次会话尚未执行过重分析（没有可展示的真实差异）。</p>
        )}
        <p className="form-note">
          重新分析不会自动把旧动作标为已验证，也不会让已完成动作自动声称承担新出现的影响。
        </p>
        {detail.dagViolations.length > 0 ? (
          <p className="form-error" role="alert">
            动作前置关系存在问题：{detail.dagViolations.join('、')}
          </p>
        ) : null}
      </section>

      <section className="panel">
        <h2>计划状态</h2>
        <p className="form-note">
          结束计划需要：没有未处理的必须变更、没有待验证动作，且 readiness 为
          ready_with_known_scope。 仅“所有动作 done”不足以结束计划。
        </p>
        <div className="inline-actions">
          <Button onClick={() => void transition('in_progress')} disabled={stale || busy !== null}>
            开始执行
          </Button>
          <Button onClick={() => void transition('verifying')} disabled={stale || busy !== null}>
            进入验证阶段
          </Button>
          <Button
            variant="primary"
            onClick={() => void transition('completed')}
            disabled={
              stale ||
              busy !== null ||
              detail.readiness !== 'ready_with_known_scope' ||
              !allVerified
            }
            title={
              detail.readiness !== 'ready_with_known_scope'
                ? 'readiness 尚未达到 ready_with_known_scope'
                : !allVerified
                  ? '仍有动作未通过独立验证'
                  : undefined
            }
          >
            完成计划
          </Button>
        </div>
      </section>
    </div>
  )
}

function ImpactTable({
  snapshot,
  emptyLabel,
}: {
  snapshot: ImpactSnapshot | null
  emptyLabel: string
}): ReactNode {
  if (!snapshot || snapshot.targets.length === 0) {
    return <p className="form-note">{emptyLabel}</p>
  }
  return (
    <ul className="impact-list">
      {snapshot.targets.map((target) => (
        <li key={`${target.nodeId}-${target.capability}`} className="impact-row">
          <span className="impact-node">{target.nodeId}</span>
          <span className="impact-cap">{target.capability}</span>
          <Badge
            tone={
              target.status === 'must_change'
                ? 'danger'
                : target.status === 'needs_review'
                  ? 'warn'
                  : 'neutral'
            }
          >
            {target.status}
          </Badge>
          <span className="impact-reason">{target.reasonCode}</span>
        </li>
      ))}
    </ul>
  )
}
