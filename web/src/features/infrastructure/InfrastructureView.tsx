import { useEffect, useMemo, useState, type ReactNode } from 'react'
import { useRuntime } from '../../app/store.tsx'
import { navigate } from '../../app/routes.ts'
import type { InfrastructureProjection, NodeCreatePayload } from '../../runtime/protocol.ts'
import {
  Badge,
  Button,
  Dialog,
  EmptyState,
  Field,
  Loading,
  Select,
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

export function InfrastructureView(): ReactNode {
  const { runQuery, revision } = useRuntime()
  const [data, setData] = useState<InfrastructureProjection | null>(null)
  const [filter, setFilter] = useState<string>('all')
  const [includeArchived, setIncludeArchived] = useState(false)
  const [creating, setCreating] = useState(false)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    void runQuery({ type: 'get_infrastructure' }).then((result) => {
      if (cancelled) return
      setData(result)
      setLoading(false)
    })
    return () => {
      cancelled = true
    }
  }, [runQuery, revision])

  const nodes = useMemo(() => {
    if (!data) return []
    return data.nodes.filter((node) => {
      if (!includeArchived && node.archived) return false
      if (filter === 'all') return true
      return node.kind === filter
    })
  }, [data, filter, includeArchived])

  if (loading && !data) return <Loading label="正在读取基础设施" />
  if (!data) return <EmptyState title="无法读取基础设施" />

  return (
    <div className="stack" data-testid="infrastructure-view">
      <header className="page-head">
        <div>
          <p className="page-kicker">基础设施</p>
          <h1>对象与环境</h1>
          <p className="page-lede">只有你确认过的对象才会出现在这里。归档优先于删除。</p>
        </div>
        <Button variant="primary" onClick={() => setCreating(true)}>
          添加对象
        </Button>
      </header>

      <div className="chip-row" role="tablist" aria-label="按类型筛选">
        <button
          type="button"
          role="tab"
          aria-selected={filter === 'all'}
          className={`chip ${filter === 'all' ? 'active' : ''}`}
          onClick={() => setFilter('all')}
        >
          全部 {data.nodes.filter((node) => includeArchived || !node.archived).length}
        </button>
        {data.templates.map((template) => {
          const count = data.counts.find((entry) => entry.kind === template.kind)?.count ?? 0
          return (
            <button
              key={template.kind}
              type="button"
              role="tab"
              aria-selected={filter === template.kind}
              className={`chip ${filter === template.kind ? 'active' : ''}`}
              onClick={() => setFilter(template.kind)}
            >
              {template.label} {count}
            </button>
          )
        })}
        <label className="check">
          <input
            type="checkbox"
            checked={includeArchived}
            onChange={(event) => setIncludeArchived(event.target.checked)}
          />
          显示已归档（{data.archivedCount}）
        </label>
      </div>

      {nodes.length === 0 ? (
        <EmptyState
          title="当前没有已记录的对象"
          detail="这里没有记录，不代表现实中没有这些对象。"
          action={
            <Button variant="primary" onClick={() => setCreating(true)}>
              添加第一个对象
            </Button>
          }
        />
      ) : (
        <div className="table" role="table" aria-label="基础设施对象">
          <div className="table-head" role="row">
            <span role="columnheader">名称</span>
            <span role="columnheader">类型</span>
            <span role="columnheader">识别信息</span>
            <span role="columnheader">关系</span>
            <span role="columnheader">状态</span>
          </div>
          {nodes.map((node) => (
            <button
              key={node.id}
              type="button"
              role="row"
              className={`table-row ${node.archived ? 'archived' : ''}`}
              onClick={() => navigate(`/infrastructure/${node.id}`)}
            >
              <span role="cell" className="cell-name">
                <Sensitive value={node.name} />
              </span>
              <span role="cell">{KIND_LABEL[node.kind] ?? node.kind}</span>
              <span role="cell" className="cell-muted">
                {node.issuer ? <Sensitive value={node.issuer} /> : '—'}
                {node.last4 ? ' · ' : ''}
                {node.last4 ? <Sensitive value={`••${node.last4}`} /> : null}
              </span>
              <span role="cell">{node.dependencyCount}</span>
              <span role="cell">
                {node.archived ? (
                  <Badge tone="neutral">已归档</Badge>
                ) : (
                  <Badge tone="ok">当前</Badge>
                )}
                {node.unknownCriticalityCount > 0 ? <Badge tone="warn">关键性未知</Badge> : null}
              </span>
            </button>
          ))}
        </div>
      )}

      {creating ? <NodeCreateDialog onClose={() => setCreating(false)} /> : null}
    </div>
  )
}

export function NodeCreateDialog({ onClose }: { onClose: () => void }): ReactNode {
  const { runCommand, notify } = useRuntime()
  const [kind, setKind] = useState('identity_anchor')
  const [name, setName] = useState('')
  const [issuer, setIssuer] = useState('')
  const [last4, setLast4] = useState('')
  const [subtype, setSubtype] = useState('phone')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const submit = async (): Promise<void> => {
    setError(null)
    if (name.trim() === '') {
      setError('请填写名称')
      return
    }
    const payload: NodeCreatePayload = {
      kind: kind as NodeCreatePayload['kind'],
      name: name.trim(),
      ...(kind === 'payment_instrument' && issuer.trim() !== '' ? { issuer: issuer.trim() } : {}),
      ...(kind === 'payment_instrument' && last4.trim() !== '' ? { last4: last4.trim() } : {}),
      ...(kind === 'identity_anchor' ? { fields: { subtype } } : {}),
    }
    setBusy(true)
    const result = await runCommand({ type: 'create_node', payload })
    setBusy(false)
    if (!result) {
      setError('创建失败')
      return
    }
    notify('已添加对象', 'success')
    onClose()
  }

  return (
    <Dialog
      title="添加基础设施对象"
      onClose={onClose}
      footer={
        <>
          <Button variant="ghost" onClick={onClose} disabled={busy}>
            取消
          </Button>
          <Button variant="primary" onClick={() => void submit()} disabled={busy}>
            {busy ? '正在保存…' : '确认添加'}
          </Button>
        </>
      }
    >
      <Select
        label="类型"
        value={kind}
        onChange={setKind}
        options={Object.entries(KIND_LABEL).map(([value, label]) => ({ value, label }))}
      />
      <Field
        label="名称"
        value={name}
        onChange={(event) => setName(event.target.value)}
        hint="只填你愿意记录的部分。"
      />
      {kind === 'identity_anchor' ? (
        <Select
          label="身份锚点类型"
          value={subtype}
          onChange={setSubtype}
          options={[
            { value: 'phone', label: '手机号' },
            { value: 'email', label: '邮箱' },
            { value: 'other', label: '其他' },
          ]}
          hint="只有标记为手机号的锚点才可能使用「更换手机号」模板。"
        />
      ) : null}
      {kind === 'payment_instrument' ? (
        <>
          <Field
            label="发卡机构（可选）"
            value={issuer}
            onChange={(event) => setIssuer(event.target.value)}
          />
          <Field
            label="卡号后四位（可选）"
            value={last4}
            maxLength={4}
            onChange={(event) => setLast4(event.target.value)}
            hint="不要填写完整卡号；PDIG 不存储支付凭证。"
          />
        </>
      ) : null}
      {error ? (
        <p className="form-error" role="alert">
          {error}
        </p>
      ) : null}
      <p className="form-note">
        添加对象只会写入对象本身；关系必须在下一次显式确认后才进入 Reality。
      </p>
    </Dialog>
  )
}
