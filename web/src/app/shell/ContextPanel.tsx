import { useEffect, useState, type ReactNode } from 'react'
import { useRuntime } from '../store.tsx'
import type { Route } from '../routes.ts'
import type { NodeDetailProjection, PlanDetailView } from '../../runtime/protocol.ts'
import { Sensitive, SensitiveLabel } from '../../privacy/Sensitive.tsx'
import { Badge, Loading } from '../../components/primitives/index.tsx'

export function ContextPanel({ route }: { route: Route }): ReactNode {
  const { revision } = useRuntime()
  const [node, setNode] = useState<NodeDetailProjection | null>(null)
  const [plan, setPlan] = useState<PlanDetailView | null>(null)
  const [loading, setLoading] = useState(false)
  const { runQuery } = useRuntime()

  useEffect(() => {
    let cancelled = false
    const load = async (): Promise<void> => {
      setLoading(true)
      if (route.name === 'node') {
        const detail = await runQuery({ type: 'get_node_detail', id: route.id })
        if (!cancelled) setNode(detail)
      } else if (route.name === 'plan') {
        const detail = await runQuery({ type: 'get_plan_detail', id: route.id })
        if (!cancelled) setPlan(detail)
      }
      if (!cancelled) setLoading(false)
    }
    void load()
    return () => {
      cancelled = true
    }
  }, [route, revision, runQuery])

  return (
    <aside className="context" aria-label="上下文与 Truth">
      {loading ? <Loading label="正在读取已确认现实" /> : null}
      {route.name === 'node' && node ? <NodeContext detail={node} /> : null}
      {route.name === 'plan' && plan ? <PlanContext detail={plan} /> : null}
      {route.name !== 'node' && route.name !== 'plan' ? <VaultContext /> : null}
    </aside>
  )
}

function VaultContext(): ReactNode {
  const { status } = useRuntime()
  if (!status) return null
  return (
    <div className="context-body">
      <section className="context-card">
        <h3>本地 vault</h3>
        <dl className="facts">
          <div>
            <dt>图版本（graphRevision）</dt>
            <dd>{status.graphRevision}</dd>
          </div>
          <div>
            <dt>最近保存</dt>
            <dd>{status.lastSavedAt ?? '尚未保存'}</dd>
          </div>
          <div>
            <dt>自动锁定</dt>
            <dd>{status.autoLockMinutes > 0 ? `${status.autoLockMinutes} 分钟` : '保持打开'}</dd>
          </div>
        </dl>
      </section>
      <section className="context-card">
        <h3>浏览器存储</h3>
        <p className="context-note">
          {status.storage.persisted
            ? '浏览器已授予持久化存储，但仍可能在存储压力下清理。'
            : '浏览器未授予持久化存储：本地数据可能被清理，建议创建加密备份。'}
        </p>
        <p className="context-note">
          用量：{formatBytes(status.storage.usageBytes)} / 配额{' '}
          {formatBytes(status.storage.quotaBytes)}
        </p>
      </section>
      <section className="context-card context-card-quiet">
        <h3>Truth 边界</h3>
        <ul className="truth-list">
          <li>Observation ≠ Reality</li>
          <li>Proposal ≠ Reality</li>
          <li>done ≠ verified</li>
          <li>计划投影 ≠ 已经发生</li>
        </ul>
      </section>
    </div>
  )
}

function NodeContext({ detail }: { detail: NodeDetailProjection }): ReactNode {
  const activeIncoming = detail.incoming.filter((edge) => edge.state === 'active')
  return (
    <div className="context-body">
      <section className="context-card">
        <h3>已确认的基础</h3>
        <p className="context-title">
          <Sensitive value={detail.node.name} />
        </p>
        <p className="context-sub">
          {detail.node.kind}
          {detail.node.issuer ? ' · ' : ''}
          {detail.node.issuer ? <Sensitive value={detail.node.issuer} /> : null}
        </p>
        <dl className="facts">
          <div>
            <dt>已确认关系</dt>
            <dd>
              {detail.outgoing.filter((edge) => edge.state === 'active').length +
                activeIncoming.length}
            </dd>
          </div>
          <div>
            <dt>关键性未知</dt>
            <dd>
              {
                detail.incoming
                  .concat(detail.outgoing)
                  .filter((edge) => edge.criticality === 'unknown').length
              }
            </dd>
          </div>
          <div>
            <dt>相关发现</dt>
            <dd>{detail.findings.length}</dd>
          </div>
        </dl>
      </section>
      <section className="context-card">
        <h3>未确定的部分</h3>
        <ul className="truth-list">
          {detail.incoming.length === 0 && detail.outgoing.length === 0 ? (
            <li>没有记录任何关系；这不代表现实中没有依赖。</li>
          ) : null}
          {detail.incoming.some((edge) => edge.criticality === 'unknown') ? (
            <li>存在关键性未确认的依赖：系统不会替你判定“必须处理”。</li>
          ) : null}
          {detail.findings.length === 0 ? <li>当前没有派生出的基础设施发现。</li> : null}
        </ul>
      </section>
      {detail.groups.length > 0 ? (
        <section className="context-card">
          <h3>已确认组合</h3>
          {detail.groups.map((group) => (
            <p key={group.id} className="context-sub">
              {group.capability} · {group.mode} · {group.memberEdges.length} 条关系
            </p>
          ))}
        </section>
      ) : null}
    </div>
  )
}

function PlanContext({ detail }: { detail: PlanDetailView }): ReactNode {
  const { plan, summary, readiness } = detail
  return (
    <div className="context-body">
      <section className="context-card">
        <h3>计划真相</h3>
        <p className="context-title">{plan.title}</p>
        <p className="context-sub">
          <Badge tone={summary.stale ? 'warn' : 'neutral'}>
            {summary.stale ? '需要重新分析' : summary.workflowState}
          </Badge>{' '}
          <Badge tone="neutral">readiness: {readiness}</Badge>
        </p>
        <dl className="facts">
          <div>
            <dt>分析基准</dt>
            <dd>
              revision {plan.lastAnalyzedGraphRevision} → 当前 {summary.currentGraphRevision}
            </dd>
          </div>
          <div>
            <dt>动作</dt>
            <dd>
              完成 {summary.doneCount}/{summary.actionCount} · 已验证 {summary.verifiedCount}
            </dd>
          </div>
          <div>
            <dt>目标对象</dt>
            <dd>{summary.targetName ? <Sensitive value={summary.targetName} /> : '未指定'}</dd>
          </div>
        </dl>
      </section>
      <section className="context-card">
        <h3>三个时间</h3>
        <ul className="truth-list">
          <li>发生时间（occurredAt）：Core 未记录 → 显示「发生时间未记录」</li>
          <li>记录时间（recordedAt）：动作被标记完成的时间</li>
          <li>验证时间（verifiedAt）：独立验证通过的时间</li>
        </ul>
      </section>
      <section className="context-card context-card-quiet">
        <h3>Plan Projection</h3>
        <p className="context-note">
          After 视图只是计划投影，不代表现实已经改变；现实仍以 Current 与已确认关系为准。
        </p>
      </section>
    </div>
  )
}

function formatBytes(value: number | null): string {
  if (value === null) return '未知'
  if (value < 1024) return `${value} B`
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`
  return `${(value / (1024 * 1024)).toFixed(1)} MB`
}

export { SensitiveLabel }
