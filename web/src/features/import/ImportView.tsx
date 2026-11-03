import { useEffect, useMemo, useState, type ReactNode } from 'react'
import { useRuntime } from '../../app/store.tsx'
import type { ImportStateProjection } from '../../runtime/protocol.ts'
import {
  Badge,
  Button,
  EmptyState,
  Loading,
  SectionHeader,
  Select,
} from '../../components/primitives/index.tsx'
import { Sensitive } from '../../privacy/Sensitive.tsx'

interface MerchantRow {
  merchantRaw: string
  resolvedNodeId: string | null
  observationCount: number
  resolution: string
}

export function ImportView(): ReactNode {
  const { runQuery, runCommand, notify, revision } = useRuntime()
  const [state, setState] = useState<ImportStateProjection | null>(null)
  const [loading, setLoading] = useState(true)
  const [adapterId, setAdapterId] = useState('wechat_statement')
  const [fileText, setFileText] = useState<string>('')
  const [fileName, setFileName] = useState('')
  const [merchants, setMerchants] = useState<MerchantRow[]>([])
  const [sessionId, setSessionId] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [nodes, setNodes] = useState<Array<{ id: string; name: string; kind: string }>>([])
  const [accountNodeId, setAccountNodeId] = useState('')
  const [columns, setColumns] = useState<string[]>([])
  const [mapping, setMapping] = useState({
    dateTime: '',
    amount: '',
    debit: '',
    credit: '',
    description: '',
    counterparty: '',
    transactionId: '',
    delimiter: ',' as ',' | ';' | '\t' | '|',
    encoding: 'utf-8' as 'utf-8' | 'gb18030',
    decimalSeparator: '.' as '.' | ',',
    amountSignMode: 'outward_positive' as 'signed' | 'debit_credit' | 'outward_positive',
    hasHeaderRow: true,
    dateFormats: 'YYYY-MM-DD',
  })

  const load = async (): Promise<void> => {
    setLoading(true)
    const result = await runQuery({ type: 'get_import_state' })
    setState(result)
    setLoading(false)
  }

  useEffect(() => {
    void load()
    void runQuery({ type: 'get_infrastructure' }).then((result) => {
      if (!result) return
      setNodes(
        result.nodes
          .filter((node) => !node.archived)
          .map((node) => ({ id: node.id, name: node.name, kind: node.kind })),
      )
    })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [revision])

  const csvColumns = useMemo(() => columns, [columns])

  const onFile = async (file: File): Promise<void> => {
    setFileName(file.name)
    const text = await file.text()
    setFileText(text)
    const firstLine = text.split(/\r?\n/, 1)[0] ?? ''
    const delimiter = firstLine.includes('\t') ? '\t' : firstLine.includes(';') ? ';' : ','
    setColumns(firstLine.split(delimiter).map((column) => column.trim().replace(/^"|"$/g, '')))
  }

  const start = async (): Promise<void> => {
    if (fileText === '') {
      notify('请选择要导入的文件', 'error')
      return
    }
    if (accountNodeId === '') {
      notify('请选择这份账单归属的账户（Core 的支付路径建议依赖账户归属）', 'error')
      return
    }
    setBusy(true)
    const dataBase64 = btoa(String.fromCharCode(...new TextEncoder().encode(fileText)))
    const mappingPayload =
      adapterId === 'generic_csv'
        ? {
            columns: {
              dateTime: mapping.dateTime || 'date',
              ...(mapping.amount ? { amount: mapping.amount } : {}),
              ...(mapping.debit ? { debit: mapping.debit } : {}),
              ...(mapping.credit ? { credit: mapping.credit } : {}),
              ...(mapping.description ? { description: mapping.description } : {}),
              ...(mapping.counterparty ? { counterparty: mapping.counterparty } : {}),
              ...(mapping.transactionId ? { transactionId: mapping.transactionId } : {}),
            },
            options: {
              delimiter: mapping.delimiter,
              encoding: mapping.encoding,
              dateFormats: mapping.dateFormats.split(',').map((entry) => entry.trim()),
              decimalSeparator: mapping.decimalSeparator,
              amountSignMode: mapping.amountSignMode,
              hasHeaderRow: mapping.hasHeaderRow,
            },
          }
        : undefined
    const result = await runCommand({
      type: 'import_file',
      fileName,
      dataBase64,
      adapterId,
      ...(accountNodeId === '' ? {} : { accountNodeId }),
      ...(mappingPayload ? { mapping: mappingPayload as never } : {}),
    })
    setBusy(false)
    if (!result) return
    setSessionId(result.sessionId)
    setMerchants(result.merchants)
    notify(`已解析 ${result.rawCount} 条观测，待确认商户 ${result.unresolvedCount} 个`, 'success')
  }

  const resolve = async (merchantRaw: string, nodeId: string): Promise<void> => {
    const result = await runCommand({ type: 'resolve_merchant', merchantRaw, nodeId })
    if (!result) return
    setMerchants((current) =>
      current.map((merchant) =>
        merchant.merchantRaw === merchantRaw
          ? { ...merchant, resolvedNodeId: nodeId, resolution: 'manual' }
          : merchant,
      ),
    )
  }

  const finalize = async (): Promise<void> => {
    const unresolved = merchants.filter((merchant) => merchant.resolvedNodeId === null)
    if (unresolved.length > 0) {
      notify('仍有商户没有对应对象：未解析的商户不会生成提案', 'error')
    }
    setBusy(true)
    const result = await runCommand({ type: 'finalize_import' })
    setBusy(false)
    if (!result) return
    setMerchants([])
    setSessionId(null)
    setFileText('')
    notify(
      `导入完成：新增 ${result.newUniqueCount}，重复 ${result.duplicateCount}，提案 ${result.proposalKeys.length}`,
      'success',
    )
    await load()
  }

  if (loading && !state) return <Loading label="正在读取导入状态" />
  if (!state) return <EmptyState title="无法读取导入状态" />

  return (
    <div className="stack" data-testid="import-view">
      <header className="page-head">
        <div>
          <p className="page-kicker">导入</p>
          <h1>数据来源与提案</h1>
          <p className="page-lede">
            文件只在本机读取与解析，绝不上传。导入只产生 Observation、Candidate 与
            Proposal；不会直接改写现实依赖。
          </p>
        </div>
      </header>

      <section className="panel">
        <SectionHeader
          title="选择文件"
          detail="支持仓库内真实存在的 Adapter；未提供 Adapter 的来源不会出现在这里。"
        />
        <div className="inline-fields">
          <Select
            label="数据来源"
            value={adapterId}
            onChange={setAdapterId}
            options={state.adapters.map((adapter) => ({ value: adapter.id, label: adapter.label }))}
          />
          <Select
            label="账单归属账户"
            value={accountNodeId}
            onChange={setAccountNodeId}
            options={[
              { value: '', label: '选择账户' },
              ...nodes
                .filter((node) => node.kind === 'account')
                .map((node) => ({ value: node.id, label: node.name })),
            ]}
            hint="账单必须归属于某个账户；没有账户归属时不会生成支付路径提案。"
          />
          <label className="field">
            <span className="field-label">文件</span>
            <input
              className="input"
              type="file"
              accept=".csv,.txt,.ofx,.qfx"
              onChange={(event) => {
                const file = event.target.files?.[0]
                if (file) void onFile(file)
              }}
            />
          </label>
        </div>

        {adapterId === 'generic_csv' ? (
          <div className="mapping">
            <p className="form-note form-note-strong">
              通用 CSV 需要你显式指定字段映射。PDIG 不做自动列映射，也不使用任何 AI 推断。
            </p>
            <div className="inline-fields">
              <Select
                label="日期列（必填）"
                value={mapping.dateTime}
                onChange={(value) => setMapping((current) => ({ ...current, dateTime: value }))}
                options={[
                  { value: '', label: '选择列' },
                  ...csvColumns.map((column) => ({ value: column, label: column })),
                ]}
              />
              <Select
                label="金额列"
                value={mapping.amount}
                onChange={(value) => setMapping((current) => ({ ...current, amount: value }))}
                options={[
                  { value: '', label: '不使用' },
                  ...csvColumns.map((column) => ({ value: column, label: column })),
                ]}
              />
              <Select
                label="借方列"
                value={mapping.debit}
                onChange={(value) => setMapping((current) => ({ ...current, debit: value }))}
                options={[
                  { value: '', label: '不使用' },
                  ...csvColumns.map((column) => ({ value: column, label: column })),
                ]}
              />
              <Select
                label="贷方列"
                value={mapping.credit}
                onChange={(value) => setMapping((current) => ({ ...current, credit: value }))}
                options={[
                  { value: '', label: '不使用' },
                  ...csvColumns.map((column) => ({ value: column, label: column })),
                ]}
              />
              <Select
                label="描述列"
                value={mapping.description}
                onChange={(value) => setMapping((current) => ({ ...current, description: value }))}
                options={[
                  { value: '', label: '不使用' },
                  ...csvColumns.map((column) => ({ value: column, label: column })),
                ]}
              />
              <Select
                label="对方列"
                value={mapping.counterparty}
                onChange={(value) => setMapping((current) => ({ ...current, counterparty: value }))}
                options={[
                  { value: '', label: '不使用' },
                  ...csvColumns.map((column) => ({ value: column, label: column })),
                ]}
              />
              <Select
                label="金额符号模式"
                value={mapping.amountSignMode}
                onChange={(value) =>
                  setMapping((current) => ({
                    ...current,
                    amountSignMode: value as typeof mapping.amountSignMode,
                  }))
                }
                options={[
                  { value: 'signed', label: 'signed（自带正负）' },
                  { value: 'debit_credit', label: 'debit_credit（借/贷两列）' },
                  { value: 'outward_positive', label: 'outward_positive（支出为正）' },
                ]}
              />
              <Select
                label="分隔符"
                value={mapping.delimiter === '\t' ? 'tab' : mapping.delimiter}
                onChange={(value) =>
                  setMapping((current) => ({
                    ...current,
                    delimiter: value === 'tab' ? '\t' : (value as typeof mapping.delimiter),
                  }))
                }
                options={[
                  { value: ',', label: '逗号' },
                  { value: ';', label: '分号' },
                  { value: 'tab', label: 'Tab' },
                  { value: '|', label: '竖线' },
                ]}
              />
              <Select
                label="小数分隔符"
                value={mapping.decimalSeparator}
                onChange={(value) =>
                  setMapping((current) => ({
                    ...current,
                    decimalSeparator: value as typeof mapping.decimalSeparator,
                  }))
                }
                options={[
                  { value: '.', label: '点 .' },
                  { value: ',', label: '逗号 ,' },
                ]}
              />
            </div>
          </div>
        ) : null}

        <div className="inline-actions">
          <Button variant="primary" onClick={() => void start()} disabled={busy || fileText === ''}>
            {busy ? '正在本机解析…' : '开始解析'}
          </Button>
          {sessionId ? <Button onClick={() => void finalize()}>确认并生成提案</Button> : null}
        </div>
      </section>

      {merchants.length > 0 ? (
        <section className="panel">
          <SectionHeader
            title="商户 → 对象"
            detail="未完成映射的商户不会生成依赖提案。系统不会自动猜商户对应哪个服务。"
          />
          <ul className="merchant-list">
            {merchants.map((merchant) => (
              <li key={merchant.merchantRaw} className="merchant-row">
                <span className="merchant-name">
                  <Sensitive value={merchant.merchantRaw} />
                </span>
                <span className="merchant-count">{merchant.observationCount} 次观测</span>
                <Select
                  label="对应对象"
                  value={merchant.resolvedNodeId ?? ''}
                  onChange={(value) => void resolve(merchant.merchantRaw, value)}
                  options={[
                    { value: '', label: '未解析' },
                    ...nodes.map((node) => ({ value: node.id, label: node.name })),
                  ]}
                />
              </li>
            ))}
          </ul>
        </section>
      ) : null}

      <section className="panel">
        <SectionHeader
          title="提案（未进入 Reality）"
          detail="只有你接受后才会成为已确认依赖；拒绝不是永久为假。"
        />
        {state.proposals.length === 0 ? (
          <EmptyState title="没有待处理的提案" detail="导入产生的提案会出现在这里。" />
        ) : (
          <ul className="proposal-list">
            {state.proposals.map((proposal) => (
              <li key={proposal.id} className="proposal-row">
                <span>
                  <Sensitive value={proposal.fromName} />{' '}
                  <span className="rel-verb">{proposal.relation}</span>{' '}
                  <Sensitive value={proposal.toName} />
                </span>
                <span className="proposal-meta">
                  <Badge>{proposal.capability}</Badge>
                  <Badge tone={proposal.decision === 'pending' ? 'warn' : 'neutral'}>
                    {proposal.decision}
                  </Badge>
                  <span className="proposal-count">{proposal.observationCount} 次观测</span>
                </span>
                {proposal.decision === 'pending' ? (
                  <span className="proposal-actions">
                    <Button
                      variant="primary"
                      onClick={() =>
                        void runCommand({
                          type: 'accept_proposal',
                          key: proposal.key,
                          criticality: 'unknown',
                        })
                      }
                    >
                      接受（criticality: unknown）
                    </Button>
                    <Button
                      onClick={() =>
                        void runCommand({ type: 'reject_proposal', key: proposal.key })
                      }
                    >
                      拒绝
                    </Button>
                  </span>
                ) : null}
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="panel">
        <SectionHeader title="发现候选" detail="Candidate 不参与影响分析。" />
        {state.candidates.length === 0 ? (
          <EmptyState title="没有候选" />
        ) : (
          <ul className="proposal-list">
            {state.candidates.map((candidate) => (
              <li key={candidate.id} className="proposal-row">
                <span>
                  <Sensitive value={candidate.displayLabel} />
                </span>
                <span className="proposal-meta">
                  <Badge>{candidate.status}</Badge>
                  <span className="proposal-count">{candidate.observationCount} 次观测</span>
                </span>
                <span className="proposal-actions">
                  {candidate.status === 'pending' ? (
                    <>
                      <Button
                        onClick={() =>
                          void runCommand({ type: 'accept_candidate', id: candidate.id })
                        }
                      >
                        接受为对象
                      </Button>
                      <Button
                        variant="ghost"
                        onClick={() =>
                          void runCommand({ type: 'dismiss_candidate', id: candidate.id })
                        }
                      >
                        忽略
                      </Button>
                    </>
                  ) : null}
                </span>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="panel">
        <SectionHeader title="可能的变化（Drift）" detail="只在用户确认后才改变 Reality。" />
        {state.drifts.length === 0 ? (
          <EmptyState title="没有检测到可能的变化" />
        ) : (
          <ul className="proposal-list">
            {state.drifts.map((drift) => (
              <li key={drift.id} className="proposal-row">
                <span>
                  <Sensitive value={drift.fromName} /> → <Sensitive value={drift.targetName} />
                </span>
                <span className="proposal-meta">
                  <Badge tone="warn">{drift.kind}</Badge>
                  <Badge>{drift.status}</Badge>
                </span>
                <span className="proposal-actions">
                  <Button
                    onClick={() =>
                      void runCommand({
                        type: 'resolve_drift',
                        driftId: drift.id,
                        resolution: 'additional',
                      })
                    }
                  >
                    确认为额外路径
                  </Button>
                  <Button
                    onClick={() =>
                      void runCommand({
                        type: 'resolve_drift',
                        driftId: drift.id,
                        resolution: 'replacement',
                      })
                    }
                  >
                    确认为替代
                  </Button>
                  <Button
                    variant="ghost"
                    onClick={() =>
                      void runCommand({
                        type: 'resolve_drift',
                        driftId: drift.id,
                        resolution: 'dismiss',
                      })
                    }
                  >
                    忽略
                  </Button>
                </span>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="panel">
        <SectionHeader title="数据来源" detail="只保存来源元数据与会话摘要，不保存单笔交易。" />
        {state.sources.length === 0 ? (
          <EmptyState title="还没有数据来源" />
        ) : (
          <ul className="source-list">
            {state.sources.map((source) => (
              <li key={source.id} className="source-row">
                <span>{source.adapterId}</span>
                <span className="cell-muted">{source.label}</span>
                <span className="cell-muted">
                  last ingested: {source.lastIngestedAt ?? '从未导入'}
                </span>
                <Badge tone={source.state === 'active' ? 'ok' : 'neutral'}>{source.state}</Badge>
              </li>
            ))}
          </ul>
        )}
        {state.lastSession ? (
          <p className="form-note">
            最近一次导入会话：{state.lastSession.adapterId} · 原始 {state.lastSession.rawCount} 条 ·
            新增 {state.lastSession.newUniqueCount} · 重复 {state.lastSession.duplicateCount} · 提案{' '}
            {state.lastSession.proposalCount} · 错误 {state.lastSession.errorCount}
          </p>
        ) : null}
      </section>
    </div>
  )
}
