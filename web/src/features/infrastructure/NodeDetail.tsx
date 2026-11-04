import { useEffect, useState, type ReactNode } from 'react'
import { useRuntime } from '../../app/store.tsx'
import { navigate } from '../../app/routes.ts'
import type { NodeDetailProjection } from '../../runtime/protocol.ts'
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

const RELATION_OPTIONS = [
  { value: 'funding_source', label: 'funding_source（支付资金来源）' },
  { value: 'merchant_agreement', label: 'merchant_agreement（商户/订阅扣款）' },
  { value: 'authenticates', label: 'authenticates（用于登录验证）' },
  { value: 'recovers', label: 'recovers（用于账户恢复）' },
  { value: 'controls', label: 'controls（控制/访问该对象）' },
]

const CAPABILITY_BY_RELATION: Record<string, string> = {
  funding_source: 'payment',
  merchant_agreement: 'payment',
  authenticates: 'authentication',
  recovers: 'recovery',
  controls: 'access',
}

export function NodeDetail({ nodeId }: { nodeId: string }): ReactNode {
  const { runQuery, runCommand, revision, notify } = useRuntime()
  const [detail, setDetail] = useState<NodeDetailProjection | null>(null)
  const [loading, setLoading] = useState(true)
  const [adding, setAdding] = useState(false)
  const [groupMode, setGroupMode] = useState<'ANY' | 'ALL'>('ANY')
  const [selectedEdges, setSelectedEdges] = useState<string[]>([])

  const load = async (): Promise<void> => {
    setLoading(true)
    const result = await runQuery({ type: 'get_node_detail', id: nodeId })
    setDetail(result)
    setLoading(false)
  }

  useEffect(() => {
    void load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [nodeId, revision])

  if (loading && !detail) return <Loading label="正在读取对象" />
  if (!detail) return <EmptyState title="对象不存在" detail="它可能已被归档或删除。" />

  const inactive = detail.outgoing
    .concat(detail.incoming)
    .filter((edge) => edge.state === 'retired')
  const activeIncoming = detail.incoming.filter((edge) => edge.state === 'active')

  const archive = async (): Promise<void> => {
    const result = await runCommand({
      type: 'archive_node',
      id: detail.node.id,
      archived: !detail.node.archived,
    })
    if (result) notify(detail.node.archived ? '已恢复对象' : '已归档对象', 'success')
  }

  const retire = async (id: string): Promise<void> => {
    const result = await runCommand({ type: 'retire_dependency', id })
    if (result) notify('关系已标记为已终止（历史保留）', 'success')
  }

  const createGroup = async (): Promise<void> => {
    if (selectedEdges.length < 2) {
      notify('至少选择两条已确认关系', 'error')
      return
    }
    const first = activeIncoming.find((edge) => edge.id === selectedEdges[0])
    if (!first) return
    const result = await runCommand({
      type: 'create_group',
      targetNodeId: detail.node.id,
      capability: first.capability,
      mode: groupMode,
      memberEdgeIds: selectedEdges,
    })
    if (result) {
      setSelectedEdges([])
      notify('已确认组合关系', 'success')
    }
  }

  return (
    <div className="stack" data-testid="node-detail">
      <header className="page-head">
        <div>
          <p className="page-kicker">基础设施 · {detail.node.kind}</p>
          <h1>
            <Sensitive value={detail.node.name} />
          </h1>
          <p className="page-lede">
            {detail.node.archived ? '该对象已归档。' : '只展示已确认的关系；未记录的部分保持未知。'}
          </p>
        </div>
        <div className="page-actions">
          <Button onClick={() => setAdding(true)}>添加关系</Button>
          <Button variant="ghost" onClick={() => void archive()}>
            {detail.node.archived ? '恢复对象' : '归档对象'}
          </Button>
        </div>
      </header>

      <section className="panel">
        <h2>身份与生命周期</h2>
        <dl className="facts facts-grid">
          <div>
            <dt>名称</dt>
            <dd>
              <Sensitive value={detail.node.name} />
            </dd>
          </div>
          <div>
            <dt>类型</dt>
            <dd>{detail.node.kind}</dd>
          </div>
          <div>
            <dt>发卡/提供方</dt>
            <dd>{detail.node.issuer ? <Sensitive value={detail.node.issuer} /> : '未记录'}</dd>
          </div>
          <div>
            <dt>后四位</dt>
            <dd>{detail.node.last4 ? <Sensitive value={`••${detail.node.last4}`} /> : '未记录'}</dd>
          </div>
          <div>
            <dt>创建时间</dt>
            <dd>{detail.node.createdAt}</dd>
          </div>
          <div>
            <dt>最近更新</dt>
            <dd>{detail.node.updatedAt}</dd>
          </div>
        </dl>
        {Object.keys(detail.node.fields).length > 0 ? (
          <p className="form-note">
            已记录字段：
            {Object.entries(detail.node.fields).map(([key, value]) => (
              <span key={key} className="kv">
                {key}={String(value)}
              </span>
            ))}
          </p>
        ) : null}
      </section>

      <section className="panel">
        <h2>已确认关系（指向此对象）</h2>
        {detail.incoming.length === 0 ? (
          <EmptyState
            title="没有记录任何指向该对象的关系"
            detail="没有记录不等于现实中不存在依赖。"
          />
        ) : (
          <ul className="rel-list">
            {detail.incoming.map((edge) => (
              <li key={edge.id} className={`rel-row ${edge.state === 'retired' ? 'retired' : ''}`}>
                <span>
                  <Sensitive value={edge.fromName} />{' '}
                  <span className="rel-verb">{edge.relation}</span>{' '}
                  <Sensitive value={edge.toName} />
                </span>
                <span className="rel-meta">
                  <Badge>{edge.capability}</Badge>
                  <Badge tone={edge.criticality === 'required' ? 'accent' : 'warn'}>
                    criticality: {edge.criticality}
                  </Badge>
                  {edge.state === 'retired' ? <Badge tone="neutral">已终止</Badge> : null}
                </span>
                <span className="rel-actions">
                  {edge.state === 'active' ? (
                    <Button variant="ghost" onClick={() => void retire(edge.id)}>
                      标记为不再有效
                    </Button>
                  ) : null}
                </span>
              </li>
            ))}
          </ul>
        )}

        {activeIncoming.length >= 2 ? (
          <div className="group-builder">
            <h3>确认备用路径组合</h3>
            <p className="form-note">
              两条关系不等于两条独立恢复路径。只有在它们确实构成备用关系时才确认组合。
            </p>
            <ul className="group-select">
              {activeIncoming.map((edge) => (
                <li key={edge.id}>
                  <label className="check">
                    <input
                      type="checkbox"
                      checked={selectedEdges.includes(edge.id)}
                      onChange={(event) => {
                        setSelectedEdges((current) =>
                          event.target.checked
                            ? [...current, edge.id]
                            : current.filter((id) => id !== edge.id),
                        )
                      }}
                    />
                    <Sensitive value={edge.fromName} />（{edge.capability}）
                  </label>
                </li>
              ))}
            </ul>
            <div className="inline-fields">
              <Select
                label="组合模式"
                value={groupMode}
                onChange={(value) => setGroupMode(value as 'ANY' | 'ALL')}
                options={[
                  { value: 'ANY', label: 'ANY（任一可用即可）' },
                  { value: 'ALL', label: 'ALL（必须同时可用）' },
                ]}
              />
              <Button onClick={() => void createGroup()}>确认组合</Button>
            </div>
          </div>
        ) : null}

        {detail.groups.length > 0 ? (
          <div className="group-list">
            <h3>已确认组合</h3>
            {detail.groups.map((group) => (
              <p key={group.id} className="form-note">
                {group.capability} · {group.mode} · {group.memberEdges.length} 条关系（确认于{' '}
                {group.confirmedAt}）
              </p>
            ))}
          </div>
        ) : null}
      </section>

      <section className="panel">
        <h2>从此对象出发的关系</h2>
        {detail.outgoing.length === 0 ? (
          <EmptyState title="没有记录任何从该对象出发的关系" />
        ) : (
          <ul className="rel-list">
            {detail.outgoing.map((edge) => (
              <li key={edge.id} className={`rel-row ${edge.state === 'retired' ? 'retired' : ''}`}>
                <span>
                  <Sensitive value={edge.fromName} />{' '}
                  <span className="rel-verb">{edge.relation}</span>{' '}
                  <Sensitive value={edge.toName} />
                </span>
                <span className="rel-meta">
                  <Badge>{edge.capability}</Badge>
                  <Badge tone={edge.criticality === 'required' ? 'accent' : 'warn'}>
                    criticality: {edge.criticality}
                  </Badge>
                </span>
                <span className="rel-actions">
                  {edge.state === 'active' ? (
                    <Button variant="ghost" onClick={() => void retire(edge.id)}>
                      标记为不再有效
                    </Button>
                  ) : null}
                </span>
              </li>
            ))}
          </ul>
        )}
        {inactive.length > 0 ? (
          <p className="form-note">
            已终止关系 {inactive.length} 条：保留在历史中，不参与影响分析。
          </p>
        ) : null}
      </section>

      <section className="panel">
        <h2>变更</h2>
        {detail.changeTemplates.length === 0 ? (
          <EmptyState
            title="此对象暂不能直接创建变更计划"
            detail={detail.changeBoundary ?? undefined}
          />
        ) : (
          <ul className="plan-list">
            {detail.changeTemplates.map((template) => (
              <li key={template.id}>
                <button
                  type="button"
                  className="plan-row"
                  onClick={() =>
                    navigate(`/change?template=${template.id}&target=${detail.node.id}`)
                  }
                >
                  <span className="plan-title">{template.title}</span>
                  <span className="plan-meta">基于真实 Core 模板创建</span>
                </button>
              </li>
            ))}
          </ul>
        )}
        {detail.plans.length > 0 ? (
          <ul className="plan-list">
            {detail.plans.map((plan) => (
              <li key={plan.id}>
                <button
                  type="button"
                  className="plan-row"
                  onClick={() => navigate(`/change/${plan.id}`)}
                >
                  <span className="plan-title">{plan.title}</span>
                  <span className="plan-meta">
                    {plan.stale ? (
                      <Badge tone="warn">需要重新分析</Badge>
                    ) : (
                      <Badge>{plan.workflowState}</Badge>
                    )}
                  </span>
                </button>
              </li>
            ))}
          </ul>
        ) : null}
        <p className="form-note">
          变更中心只提供 Core 注册表中真实存在的模板；没有模板的对象不会生成通用假计划。
        </p>
        <Button onClick={() => navigate('/change')}>查看变更中心</Button>
      </section>

      {detail.findings.length > 0 ? (
        <section className="panel">
          <h2>相关发现</h2>
          <ul className="finding-list">
            {detail.findings.map((finding) => (
              <li key={finding.id} className="finding-row">
                <div className="finding-head">
                  <Badge>{finding.type}</Badge>
                  <span className="finding-capability">{finding.affectedCapability}</span>
                </div>
                <p className="finding-what">{finding.what}</p>
                <p className="finding-why">{finding.why}</p>
              </li>
            ))}
          </ul>
        </section>
      ) : null}

      <section className="panel">
        <h2>相关记录</h2>
        {detail.records.length === 0 ? (
          <EmptyState title="没有相关记录" detail="验证证据未记录。" />
        ) : (
          <ul className="record-list">
            {detail.records.map((record) => (
              <li key={record.id} className="record-row">
                <span className="record-title">{record.title}</span>
                <span className="record-meta">
                  发生时间：{record.occurredAt ?? '发生时间未记录'} · 记录时间：
                  {record.recordedAt ?? '未记录'} · 验证时间：
                  {record.verifiedAt ?? '验证证据未记录'}
                </span>
              </li>
            ))}
          </ul>
        )}
      </section>

      {adding ? (
        <AddRelationDialog nodeId={detail.node.id} onClose={() => setAdding(false)} />
      ) : null}
    </div>
  )
}

function AddRelationDialog({
  nodeId,
  onClose,
}: {
  nodeId: string
  onClose: () => void
}): ReactNode {
  const { runQuery, runCommand, notify } = useRuntime()
  const [relation, setRelation] = useState('authenticates')
  const [direction, setDirection] = useState<'incoming' | 'outgoing'>('incoming')
  const [peerId, setPeerId] = useState('')
  const [criticality, setCriticality] = useState<'unknown' | 'required'>('unknown')
  const [nodes, setNodes] = useState<Array<{ id: string; name: string }>>([])
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    void runQuery({ type: 'get_infrastructure' }).then((result) => {
      if (!result) return
      setNodes(
        result.nodes
          .filter((node) => node.id !== nodeId && !node.archived)
          .map((node) => ({ id: node.id, name: node.name })),
      )
    })
  }, [runQuery, nodeId])

  const submit = async (): Promise<void> => {
    if (peerId === '') {
      notify('请选择关系另一端对象', 'error')
      return
    }
    setBusy(true)
    const capability = CAPABILITY_BY_RELATION[relation] ?? 'payment'
    const from = direction === 'incoming' ? peerId : nodeId
    const to = direction === 'incoming' ? nodeId : peerId
    const result = await runCommand({
      type: 'confirm_dependency',
      from,
      relation: relation as never,
      to,
      capability: capability as never,
      criticality,
    })
    setBusy(false)
    if (result) {
      notify('关系已确认为现实', 'success')
      onClose()
    }
  }

  return (
    <Dialog
      title="确认一条现实关系"
      onClose={onClose}
      footer={
        <>
          <Button variant="ghost" onClick={onClose} disabled={busy}>
            取消
          </Button>
          <Button variant="primary" onClick={() => void submit()} disabled={busy}>
            {busy ? '正在写入…' : '确认这条关系'}
          </Button>
        </>
      }
    >
      <p className="form-note form-note-strong">
        你即将把这条关系确认为现实。它会参与影响分析，直到你显式标记为不再有效。
      </p>
      <Select
        label="关系类型"
        value={relation}
        onChange={setRelation}
        options={RELATION_OPTIONS}
        hint="关系与 capability 由 Canonical 定义；不匹配的组合会被 Core 拒绝。"
      />
      <Select
        label="方向"
        value={direction}
        onChange={(value) => setDirection(value as 'incoming' | 'outgoing')}
        options={[
          { value: 'incoming', label: '对方对象 → 当前对象' },
          { value: 'outgoing', label: '当前对象 → 对方对象' },
        ]}
      />
      <Select
        label="另一端对象"
        value={peerId}
        onChange={setPeerId}
        options={[
          { value: '', label: '选择一个对象' },
          ...nodes.map((node) => ({ value: node.id, label: node.name })),
        ]}
      />
      <Select
        label="关键性"
        value={criticality}
        onChange={(value) => setCriticality(value as 'unknown' | 'required')}
        options={[
          { value: 'unknown', label: 'unknown（默认：尚未确认是否必须处理）' },
          { value: 'required', label: 'required（我已确认这条路径必须保留）' },
        ]}
        hint="系统永远不会替你判定 required。"
      />
    </Dialog>
  )
}
