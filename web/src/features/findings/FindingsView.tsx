import { useEffect, useState, type ReactNode } from 'react'
import { useRuntime } from '../../app/store.tsx'
import type { FindingsProjection } from '../../runtime/protocol.ts'
import { Badge, EmptyState, Loading } from '../../components/primitives/index.tsx'
import { Sensitive } from '../../privacy/Sensitive.tsx'

export function FindingsView(): ReactNode {
  const { runQuery, revision } = useRuntime()
  const [data, setData] = useState<FindingsProjection | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    void runQuery({ type: 'get_findings' }).then((result) => {
      if (cancelled) return
      setData(result)
      setLoading(false)
    })
    return () => {
      cancelled = true
    }
  }, [runQuery, revision])

  if (loading && !data) return <Loading label="正在计算基础设施发现" />
  if (!data) return <EmptyState title="无法读取发现" />

  return (
    <div className="stack" data-testid="findings-view">
      <header className="page-head">
        <div>
          <p className="page-kicker">发现</p>
          <h1>已确认关系派生出的边界</h1>
          <p className="page-lede">
            这些条目全部来自 Core 的 InfrastructureFindings
            规则；未记录的信息保持未知，不会被补成结论。
          </p>
        </div>
      </header>

      {data.findings.length === 0 ? (
        <EmptyState
          title="当前没有派生出的发现"
          detail="按现有已确认信息没有触发规则。这不等于“安全”，只说明目前没有可派生的边界。"
        />
      ) : (
        <ul className="finding-list">
          {data.findings.map((finding) => (
            <li key={finding.id} className="finding-row finding-row-detail">
              <div className="finding-head">
                <Badge tone={finding.type === 'PENDING_VERIFICATION' ? 'warn' : 'neutral'}>
                  {finding.type}
                </Badge>
                <span className="finding-capability">capability: {finding.affectedCapability}</span>
              </div>
              <p className="finding-what">{finding.what}</p>
              <dl className="facts facts-grid">
                <div>
                  <dt>为什么</dt>
                  <dd>{finding.why}</dd>
                </div>
                <div>
                  <dt>已确认依据</dt>
                  <dd>{finding.confirmedBasis}</dd>
                </div>
                <div>
                  <dt>仍然未知</dt>
                  <dd>{finding.unknowns}</dd>
                </div>
                <div>
                  <dt>建议下一步</dt>
                  <dd>{finding.recommendedNextAction}</dd>
                </div>
                <div>
                  <dt>证据引用</dt>
                  <dd>
                    {finding.evidenceRefs.length === 0
                      ? '验证证据未记录'
                      : finding.evidenceRefs.join('、')}
                  </dd>
                </div>
                <div>
                  <dt>相关对象</dt>
                  <dd>
                    {finding.relatedNodeIds.length === 0 ? '未关联' : finding.relatedNodeIds.length}
                  </dd>
                </div>
              </dl>
              {finding.relatedNodeIds.length > 0 ? (
                <p className="finding-next">
                  涉及：
                  <Sensitive value={finding.relatedNodeIds.join(' / ')} />
                </p>
              ) : null}
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
