import { useEffect, useMemo, useState, type ReactNode } from 'react'
import { useRuntime } from '../../app/store.tsx'
import type { RecordsProjection } from '../../runtime/protocol.ts'
import { Badge, EmptyState, Loading } from '../../components/primitives/index.tsx'

const KIND_LABEL: Record<string, string> = {
  action_completion: '动作完成',
  action_verification: '独立验证',
  change_plan: '变更计划',
  reality_drift: '现实漂移',
  node_expiry: '到期提醒',
  source_freshness: '来源新鲜度',
}

const FILTERS = [
  'all',
  'action_completion',
  'action_verification',
  'change_plan',
  'reality_drift',
] as const

export function RecordsView(): ReactNode {
  const { runQuery, revision } = useRuntime()
  const [data, setData] = useState<RecordsProjection | null>(null)
  const [loading, setLoading] = useState(true)
  const [filter, setFilter] = useState<(typeof FILTERS)[number]>('all')

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    void runQuery({ type: 'get_records' }).then((result) => {
      if (cancelled) return
      setData(result)
      setLoading(false)
    })
    return () => {
      cancelled = true
    }
  }, [runQuery, revision])

  const records = useMemo(() => {
    if (!data) return []
    return filter === 'all' ? data.records : data.records.filter((record) => record.kind === filter)
  }, [data, filter])

  if (loading && !data) return <Loading label="正在读取记录" />
  if (!data) return <EmptyState title="无法读取记录" />

  return (
    <div className="stack" data-testid="records-view">
      <header className="page-head">
        <div>
          <p className="page-kicker">记录</p>
          <h1>发生过什么，什么时候被记录，什么时候被验证</h1>
          <p className="page-lede">
            三个时间彼此独立。Core
            没有记录的发生时间，会明确显示「发生时间未记录」，不会用完成时间或当前时间填充。
          </p>
        </div>
      </header>

      <div className="chip-row" role="tablist" aria-label="记录筛选">
        {FILTERS.map((key) => (
          <button
            key={key}
            type="button"
            role="tab"
            aria-selected={filter === key}
            className={`chip ${filter === key ? 'active' : ''}`}
            onClick={() => setFilter(key)}
          >
            {key === 'all' ? '全部' : KIND_LABEL[key]}
          </button>
        ))}
      </div>

      {records.length === 0 ? (
        <EmptyState title="当前没有记录" detail="没有记录不代表没有发生，只表示没有写入 PDIG。" />
      ) : (
        <ol className="record-list">
          {records.map((record) => (
            <li key={record.id} className="record-row record-row-detail">
              <div className="record-head">
                <Badge tone={record.kind === 'action_verification' ? 'ok' : 'neutral'}>
                  {KIND_LABEL[record.kind] ?? record.kind}
                </Badge>
                <span className="record-status">{record.status}</span>
              </div>
              <p className="record-title">{record.title}</p>
              <p className="record-sub">{record.subtitle}</p>
              <dl className="facts facts-grid clocks">
                <div>
                  <dt>发生时间</dt>
                  <dd data-testid="occurred-at">{record.occurredAt ?? '发生时间未记录'}</dd>
                </div>
                <div>
                  <dt>记录时间</dt>
                  <dd>{record.recordedAt ?? '未记录'}</dd>
                </div>
                <div>
                  <dt>验证时间</dt>
                  <dd>{record.verifiedAt ?? '验证证据未记录'}</dd>
                </div>
                <div>
                  <dt>来源</dt>
                  <dd>{record.sourceType}</dd>
                </div>
                <div>
                  <dt>证据</dt>
                  <dd>
                    {record.evidenceRefs.length === 0
                      ? '验证证据未记录'
                      : record.evidenceRefs.join('、')}
                  </dd>
                </div>
              </dl>
            </li>
          ))}
        </ol>
      )}
    </div>
  )
}
