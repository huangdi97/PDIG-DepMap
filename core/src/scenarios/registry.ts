import type { ChangePlan, PlanAction } from '../domain/change-plan.ts'
import type { SqliteDriver } from '../db/driver.ts'
import { getGraphRevision } from '../repositories/graph-revision.ts'
import { ChangePlanService } from '../services/change-plan-service.ts'

/**
 * ScenarioTemplateRegistry（MVP03 §35–§41）。
 *
 * 铁律（docs/SCENARIO_TEMPLATE_POLICY.md）：
 * - 模板是静态产品配置，不是 Graph Reality；不得直接修改 Dependency；
 * - 只有涉及数字身份/访问/支付/恢复/控制/数据/数字服务连续性的事件才是 PDIG 场景；
 * - planned 模板没有 factory，不可执行、不进入可执行注册（ST gate）；
 * - MVP03 active 模板 = payment-domain（Impact 仅 payment 的现实约束）。
 */

export type ScenarioTemplateAvailability = 'active' | 'planned'

export interface ScenarioTemplateInputSpec {
  key: string
  required: boolean
  description: string
}

export interface ScenarioTemplate {
  id: string
  category: 'payment' | 'identity' | 'device' | 'work' | 'international' | 'digital_assets'
  title: string
  description: string
  supportedCapabilities:
    readonly ['payment'] | readonly ('payment' | 'access' | 'recovery' | 'identity')[]
  requiredInputs: ScenarioTemplateInputSpec[]
  optionalInputs: ScenarioTemplateInputSpec[]
  /** 产品建议提前量（天）；非法律/金融保证，用户可调整。 */
  recommendedLeadTimeDays: number | null
  availability: ScenarioTemplateAvailability
  /** planned 模板必须为 null（不可执行的类型级表达）。 */
  scenarioFactory: ((driver: SqliteDriver, inputs: Record<string, string>) => ChangePlan) | null
}

interface PaymentTemplateInputs {
  targetPaymentInstrumentId: string
  replacementPaymentInstrumentId?: string
  effectiveDate?: string
}

function defaultActions(scenario: string): PlanAction[] {
  const prepare: PlanAction = {
    id: `${scenario}-prepare-1`,
    title: '检查依赖',
    detail: '确认哪些支付、订阅与钱包依赖此对象（见影响清单）。',
    phase: 'prepare',
    done: false,
    doneAt: null,
    verification: null,
    resolvesImpactKeys: [],
  }
  const change: PlanAction = {
    id: `${scenario}-change-1`,
    title: '迁移必要支付关系',
    detail: '把必须保留的支付关系迁移到新来源（逐项确认）。',
    phase: 'change',
    done: false,
    doneAt: null,
    verification: {
      method: 'future_observation',
      status: 'pending',
      verifiedAt: null,
      evidenceRefs: [],
    },
    // 分析（rebase）时由 claimUnassignedImpacts 显式分配 must_change keys
    resolvesImpactKeys: [],
  }
  const verify: PlanAction = {
    id: `${scenario}-verify-1`,
    title: '验证关键支付路径',
    detail: '变更完成后，用一次真实支付/账单证据确认关键路径可用。',
    phase: 'verify',
    done: false,
    doneAt: null,
    verification: {
      method: 'future_observation',
      status: 'pending',
      verifiedAt: null,
      evidenceRefs: [],
    },
    resolvesImpactKeys: [],
  }
  return [prepare, change, verify]
}

function makePaymentTemplate(spec: {
  id: string
  title: string
  description: string
  leadTimeDays: number | null
  actionTitle: string
}): ScenarioTemplate {
  return {
    id: spec.id,
    category: 'payment',
    title: spec.title,
    description: spec.description,
    supportedCapabilities: ['payment'] as const,
    requiredInputs: [
      { key: 'targetPaymentInstrumentId', required: true, description: '要变更的支付工具节点 id' },
    ],
    optionalInputs: [
      {
        key: 'replacementPaymentInstrumentId',
        required: false,
        description: '替代支付工具节点 id（可选）',
      },
      { key: 'effectiveDate', required: false, description: '计划生效日期（ISO 8601）' },
    ],
    recommendedLeadTimeDays: spec.leadTimeDays,
    availability: 'active',
    scenarioFactory: (driver: SqliteDriver, inputs: Record<string, string>) => {
      const target = inputs['targetPaymentInstrumentId']
      if (!target) {
        throw new Error(`scenario ${spec.id} requires targetPaymentInstrumentId`)
      }
      const service = new ChangePlanService(driver)
      const plan = service.plans.create({
        templateId: spec.id,
        scenario: spec.id,
        title: `${spec.title}（${inputs['effectiveDate'] ?? '未定日期'}）`.replace(
          '（未定日期）',
          '',
        ),
        targetNodeId: target,
        effectiveDate: inputs['effectiveDate'] ?? null,
        params: { ...inputs },
        actions: defaultActions(spec.id),
        graphRevision: getGraphRevision(driver),
      })
      return plan
    },
  }
}

/** 可执行注册表：仅 availability='active' 且带 factory 的模板。 */
export const SCENARIO_TEMPLATES: ScenarioTemplate[] = [
  makePaymentTemplate({
    id: 'replace_payment_card',
    title: '更换银行卡',
    description: '换卡前检查支付钱包、自动扣款和订阅关系。',
    leadTimeDays: null,
    actionTitle: '迁移支付关系',
  }),
  makePaymentTemplate({
    id: 'expiring_payment_card',
    title: '银行卡即将到期',
    description: '到期前检查仍依赖这张卡的支付路径。',
    leadTimeDays: 30,
    actionTitle: '到期前迁移',
  }),
  makePaymentTemplate({
    id: 'close_payment_instrument',
    title: '注销银行卡',
    description: '注销前确认哪些支付关系需要迁移。',
    leadTimeDays: 14,
    actionTitle: '注销前迁移',
  }),
]

/** planned 展示样例：无 factory，不可执行（用于证明 planned gate 生效）。 */
export const PLANNED_TEMPLATES: ScenarioTemplate[] = [
  {
    id: 'replace_phone_number',
    category: 'identity',
    title: '更换手机号',
    description: '设计稿（未实现）：手机号关联的验证与恢复路径检查。',
    supportedCapabilities: ['access', 'recovery', 'identity'] as const,
    requiredInputs: [
      { key: 'targetPhoneAnchorId', required: true, description: '手机号锚点节点 id' },
    ],
    optionalInputs: [],
    recommendedLeadTimeDays: 30,
    availability: 'planned',
    scenarioFactory: null,
  },
]

export function listActiveTemplates(): ScenarioTemplate[] {
  return SCENARIO_TEMPLATES.filter((t) => t.availability === 'active' && t.scenarioFactory !== null)
}

export function getScenarioTemplate(id: string): ScenarioTemplate | null {
  return (
    SCENARIO_TEMPLATES.find((t) => t.id === id) ??
    PLANNED_TEMPLATES.find((t) => t.id === id) ??
    null
  )
}

/** 执行模板：planned/无 factory 一律拒绝（ST gate）。 */
export function instantiateScenario(
  driver: SqliteDriver,
  templateId: string,
  inputs: Record<string, string>,
): ChangePlan {
  const template = getScenarioTemplate(templateId)
  if (!template) throw new Error(`scenario template not found: ${templateId}`)
  if (template.availability !== 'active' || template.scenarioFactory === null) {
    throw new Error(
      `scenario template ${templateId} is not executable (availability=${template.availability})`,
    )
  }
  for (const req of template.requiredInputs) {
    if (!inputs[req.key]) {
      throw new Error(`scenario ${templateId} missing required input: ${req.key}`)
    }
  }
  return template.scenarioFactory(driver, inputs)
}

export type { PaymentTemplateInputs }
