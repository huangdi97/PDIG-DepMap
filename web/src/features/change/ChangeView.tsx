import { useEffect, useState, type ReactNode } from 'react'
import { useRuntime } from '../../app/store.tsx'
import { navigate } from '../../app/routes.ts'
import type { PlansProjection } from '../../runtime/protocol.ts'
import {
  Badge,
  Button,
  Dialog,
  EmptyState,
  Field,
  Loading,
  SectionHeader,
  Select,
} from '../../components/primitives/index.tsx'
import { Sensitive } from '../../privacy/Sensitive.tsx'

export function ChangeView(): ReactNode {
  const { runQuery, runCommand, notify, revision } = useRuntime()
  const [data, setData] = useState<PlansProjection | null>(null)
  const [loading, setLoading] = useState(true)
  const [wizard, setWizard] = useState<{ templateId: string; target: string } | null>(null)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    void runQuery({ type: 'get_plans' }).then((result) => {
      if (cancelled) return
      setData(result)
      setLoading(false)
    })
    return () => {
      cancelled = true
    }
  }, [runQuery, revision])

  // 从对象详情进入时的深链接：?template=..&target=..
  useEffect(() => {
    const params = new URLSearchParams(window.location.hash.split('?')[1] ?? window.location.search)
    const templateId = params.get('template')
    const target = params.get('target')
    if (templateId && target) setWizard({ templateId, target })
  }, [])

  if (loading && !data) return <Loading label="正在读取变更中心" />
  if (!data) return <EmptyState title="无法读取变更中心" />

  const active = data.plans.filter(
    (plan) => plan.workflowState !== 'completed' && plan.workflowState !== 'cancelled',
  )
  const completed = data.plans.filter(
    (plan) => plan.workflowState === 'completed' || plan.workflowState === 'cancelled',
  )

  return (
    <div className="stack" data-testid="change-view">
      <header className="page-head">
        <div>
          <p className="page-kicker">变更</p>
          <h1>变更中心</h1>
          <p className="page-lede">
            变更计划基于已确认现实分析影响。After 只是计划投影，不是已经发生的事实。
          </p>
        </div>
      </header>

      <section className="panel">
        <SectionHeader
          title="可执行的变更模板"
          detail="运行时读取 Core 的 listActiveTemplates()；没有模板的对象不会生成通用假计划。"
        />
        <ul className="template-list">
          {data.templates.map((template) => (
            <li key={template.id} className="template-row">
              <div>
                <p className="template-title">{template.title}</p>
                <p className="template-desc">{template.description}</p>
                <p className="template-cap">
                  capability：{template.supportedCapabilities.join('、')}
                </p>
              </div>
              <Button
                variant="primary"
                onClick={() => setWizard({ templateId: template.id, target: '' })}
              >
                创建计划
              </Button>
            </li>
          ))}
        </ul>
      </section>

      <section className="panel">
        <SectionHeader
          title="进行中的变更"
          detail="stale 计划必须重新分析后才能继续记录或验证动作。"
        />
        {active.length === 0 ? (
          <EmptyState
            title="当前没有进行中的变更"
            detail="没有计划不代表没有风险，只表示没有已记录的变更意图。"
          />
        ) : (
          <ul className="plan-list">
            {active.map((plan) => (
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
                    )}
                    {plan.pendingVerificationCount > 0 ? (
                      <Badge tone="warn">待验证 {plan.pendingVerificationCount}</Badge>
                    ) : null}
                    <Badge tone="neutral">readiness: {plan.readiness}</Badge>
                  </span>
                </button>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="panel">
        <SectionHeader
          title="已结束的变更"
          detail="历史计划保持冻结：stale 判定不再改写它们的快照。"
        />
        {completed.length === 0 ? (
          <EmptyState title="还没有已结束的变更" />
        ) : (
          <ul className="plan-list">
            {completed.map((plan) => (
              <li key={plan.id}>
                <button
                  type="button"
                  className="plan-row"
                  onClick={() => navigate(`/change/${plan.id}`)}
                >
                  <span className="plan-title">{plan.title}</span>
                  <span className="plan-meta">
                    <Badge tone="neutral">{plan.workflowState}</Badge>
                  </span>
                </button>
              </li>
            ))}
          </ul>
        )}
      </section>

      {wizard ? <PlanWizard initial={wizard} onClose={() => setWizard(null)} /> : null}
    </div>
  )
}

function PlanWizard({
  initial,
  onClose,
}: {
  initial: { templateId: string; target: string }
  onClose: () => void
}): ReactNode {
  const { runQuery, runCommand, notify } = useRuntime()
  const [templates, setTemplates] = useState<PlansProjection['templates']>([])
  const [nodes, setNodes] = useState<Array<{ id: string; name: string; kind: string }>>([])
  const [templateId, setTemplateId] = useState(initial.templateId)
  const [target, setTarget] = useState(initial.target)
  const [replacement, setReplacement] = useState('')
  const [effectiveDate, setEffectiveDate] = useState('')
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    void runQuery({ type: 'get_plans' }).then((result) => {
      if (result) setTemplates(result.templates)
    })
    void runQuery({ type: 'get_infrastructure' }).then((result) => {
      if (!result) return
      setNodes(
        result.nodes
          .filter((node) => !node.archived)
          .map((node) => ({ id: node.id, name: node.name, kind: node.kind })),
      )
    })
  }, [runQuery])

  const template = templates.find((entry) => entry.id === templateId)
  const eligible = nodes.filter((node) => {
    if (templateId === 'replace_phone_number') return node.kind === 'identity_anchor'
    return node.kind === 'payment_instrument'
  })

  const create = async (): Promise<void> => {
    if (target === '') {
      notify('请选择变更目标对象', 'error')
      return
    }
    const inputs: Record<string, string> = {}
    if (templateId === 'replace_phone_number') inputs['targetPhoneAnchorId'] = target
    else inputs['targetPaymentInstrumentId'] = target
    if (replacement !== '') inputs['replacementPaymentInstrumentId'] = replacement
    if (effectiveDate !== '') inputs['effectiveDate'] = effectiveDate
    setBusy(true)
    const result = await runCommand({ type: 'create_plan', templateId, inputs })
    setBusy(false)
    if (result) {
      notify('变更计划已创建并分析', 'success')
      onClose()
      navigate(`/change/${result.id}`)
    }
  }

  return (
    <Dialog
      title="创建变更计划"
      onClose={onClose}
      wide
      footer={
        <>
          <Button variant="ghost" onClick={onClose} disabled={busy}>
            取消
          </Button>
          <Button variant="primary" onClick={() => void create()} disabled={busy}>
            {busy ? '正在分析影响…' : '创建并分析'}
          </Button>
        </>
      }
    >
      <Select
        label="1. 选择变更场景"
        value={templateId}
        onChange={setTemplateId}
        options={templates.map((entry) => ({ value: entry.id, label: entry.title }))}
        hint={template?.description}
      />
      <Select
        label="2. 选择目标对象"
        value={target}
        onChange={setTarget}
        options={[
          { value: '', label: '选择对象' },
          ...eligible.map((node) => ({ value: node.id, label: node.name })),
        ]}
        hint="只列出与模板匹配的对象类型。"
      />
      {templateId !== 'replace_phone_number' ? (
        <Select
          label="3. 替代对象（可选）"
          value={replacement}
          onChange={setReplacement}
          options={[
            { value: '', label: '暂不指定' },
            ...nodes
              .filter((node) => node.kind === 'payment_instrument' && node.id !== target)
              .map((node) => ({ value: node.id, label: node.name })),
          ]}
        />
      ) : null}
      <Field
        label="生效日期（可选）"
        type="date"
        value={effectiveDate}
        onChange={(event) => setEffectiveDate(event.target.value)}
      />
      <p className="form-note">
        创建后会立即用当前已确认现实做一次影响分析；此后如果现实发生变化，计划会进入
        needs_revalidation， 并且在你重新分析之前不能记录完成或验证动作。
      </p>
    </Dialog>
  )
}
