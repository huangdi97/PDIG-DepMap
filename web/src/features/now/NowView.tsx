import { useEffect, useState, type ReactNode } from 'react'
import { useRuntime } from '../../app/store.tsx'
import { navigate } from '../../app/routes.ts'
import type { NowProjection } from '../../runtime/protocol.ts'
import {
  Badge,
  Button,
  EmptyState,
  Fact,
  Loading,
  SectionHeader,
} from '../../components/primitives/index.tsx'
import { Sensitive } from '../../privacy/Sensitive.tsx'

const KIND_LABEL: Record<string, string> = {
  identity_anchor: '身份锚点',
  payment_instrument: '支付工具',
  account: '账户',
  service: '服务',
  membership: '会员',
  device: '设备',
  custom: '自定义',
}

export function NowView(): ReactNode {
  const { runQuery, revision } = useRuntime()
  const [data, setData] = useState<NowProjection | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    void runQuery({ type: 'get_now' }).then((result) => {
      if (cancelled) return
      setData(result)
      setLoading(false)
    })
    return () => {
      cancelled = true
    }
  }, [runQuery, revision])

  if (loading && !data) return <Loading label="正在读取已确认现实" />
  if (!data)
    return <EmptyState title="暂时无法读取数据" detail="请重新加载页面或检查浏览器存储状态。" />

  return (
    <div className="stack" data-testid="now-view">
      <header className="page-head">
        <div>
          <p className="page-kicker">现在</p>
          <h1>你的数字基础设施</h1>
          <p className="page-lede">
            这里只展示已确认的事实与它们的边界。没有记录的部分保持未知，系统不会替你猜。
          </p>
        </div>
      </header>

      <section className="facts-row" aria-label="已确认事实计数">
        <Fact label="已确认对象" value={data.counts.nodes} />
        <Fact label="已确认关系" value={data.counts.dependencies} />
        <Fact label="关键性未知的关系" value={data.counts.unknownCriticality} />
        <Fact label="待验证变更" value={data.counts.pendingVerifications} />
      </section>

      <section className="panel">
        <SectionHeader
          title="关键基础设施"
          detail={
            data.byKind.length === 0
              ? '还没有任何已记录对象。'
              : '按依赖数量排序，仅代表记录强度，不代表安全程度。'
          }
          action={<Button onClick={() => navigate('/infrastructure')}>查看全部</Button>}
        />
        {data.keyInfrastructure.length === 0 ? (
          <EmptyState
            title="当前没有已记录的基础设施"
            detail="这里没有记录，不代表现实中没有这些对象。"
            action={
              <Button variant="primary" onClick={() => navigate('/infrastructure')}>
                添加第一个对象
              </Button>
            }
          />
        ) : (
          <ul className="object-list">
            {data.keyInfrastructure.map((node) => (
              <li key={node.id}>
                <button
                  type="button"
                  className="object-row"
                  onClick={() => navigate(`/infrastructure/${node.id}`)}
                >
                  <span className="object-name">
                    <Sensitive value={node.name} />
                  </span>
                  <span className="object-meta">
                    {KIND_LABEL[node.kind] ?? node.kind}
                    {node.issuer ? ' · ' : ''}
                    {node.issuer ? <Sensitive value={node.issuer} /> : null}
                  </span>
                  <span className="object-count">{node.dependencyCount} 条关系</span>
                </button>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="panel">
        <SectionHeader
          title="正在变化"
          detail="变更计划是意图与影响快照，不是已经发生的现实。"
          action={<Button onClick={() => navigate('/change')}>变更中心</Button>}
        />
        {data.changes.length === 0 ? (
          <EmptyState
            title="当前没有进行中的变更"
            detail="没有计划不代表没有风险；只是目前没有已记录的变更意图。"
          />
        ) : (
          <ul className="plan-list">
            {data.changes.map((plan) => (
              <li key={plan.id}>
                <button
                  type="button"
                  className="plan-row"
                  onClick={() => navigate(`/change/${plan.id}`)}
                >
                  <span className="plan-title">
                    {plan.title}
                    {plan.targetName ? (
                      <>
                        {' · '}
                        <Sensitive value={plan.targetName} />
                      </>
                    ) : null}
                  </span>
                  <span className="plan-meta">
                    {plan.stale ? (
                      <Badge tone="warn">需要重新分析</Badge>
                    ) : (
                      <Badge>{plan.workflowState}</Badge>
                    )}{' '}
                    <Badge tone={plan.pendingVerificationCount > 0 ? 'warn' : 'neutral'}>
                      待验证 {plan.pendingVerificationCount}
                    </Badge>
                  </span>
                </button>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="panel">
        <SectionHeader
          title="需要处理"
          detail="来自已确认关系的派生发现；absence ≠ non-existence。"
          action={<Button onClick={() => navigate('/findings')}>全部发现</Button>}
        />
        {data.attention.length === 0 ? (
          <EmptyState
            title="当前没有派生出的发现"
            detail="这不等于“一切安全”，只表示按现有已确认信息没有触发规则。"
          />
        ) : (
          <ul className="finding-list">
            {data.attention.map((finding) => (
              <li key={finding.id} className="finding-row">
                <div className="finding-head">
                  <Badge tone={finding.type === 'PENDING_VERIFICATION' ? 'warn' : 'neutral'}>
                    {finding.type}
                  </Badge>
                  <span className="finding-capability">{finding.affectedCapability}</span>
                </div>
                <p className="finding-what">{finding.what}</p>
                <p className="finding-why">{finding.why}</p>
                <p className="finding-next">建议：{finding.recommendedNextAction}</p>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="panel panel-next">
        <SectionHeader title="下一步" detail="只列出当前最重要的 1–3 件事。" />
        {data.nextSteps.length === 0 ? (
          <EmptyState
            title="暂时没有必须处理的下一步"
            detail="你可以继续补充已确认的关系，或检查尚未验证的变更。"
          />
        ) : (
          <ol className="next-list">
            {data.nextSteps.map((step) => (
              <li key={step.id}>
                <button type="button" className="next-row" onClick={() => navigate(step.route)}>
                  <span className="next-label">{step.label}</span>
                  <span className="next-detail">{step.detail}</span>
                </button>
              </li>
            ))}
          </ol>
        )}
      </section>
    </div>
  )
}
