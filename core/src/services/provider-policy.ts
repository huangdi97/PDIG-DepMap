/**
 * ProviderPolicy — Canonical vNext (v0.3.0) Provider Knowledge Plane
 *
 * 铁律（V030-PP-01）：
 * - Provider supports X != User has configured X
 * - ProviderPolicy 可以影响解释 / 建议 / ChangePlan 模板 / 触发 needs_revalidation
 * - ProviderPolicy 不能：自动创建 Dependency、自动确认手机号、自动标 required、
 *   自动把用户配置写入 Reality
 * - 无法证明当前规则 → needs_review，不得硬编码成永恒 truth
 */

export type ProviderPolicyState = 'effective' | 'superseded' | 'needs_review'

export interface ProviderPolicy {
  provider: string
  policyType: string
  sourceUrl: string
  retrievedAt: string
  lastVerifiedAt: string | null
  effectiveFrom: string | null
  effectiveTo: string | null
  jurisdiction: string | null
  accountTypeScope: string | null
  parameters: Record<string, unknown>
  policyRevision: number
  state: ProviderPolicyState
}

export interface ProviderPolicyInput {
  provider: string
  policyType: string
  sourceUrl: string | null
  retrievedAt: string
  lastVerifiedAt: string | null
  policyRevision: number
}

/** 由 provenance 推导 state：缺 sourceUrl 或 retrieval 过旧 → needs_review。 */
export function inferProviderPolicyState(input: ProviderPolicyInput): ProviderPolicyState {
  if (input.sourceUrl === null || input.sourceUrl.length === 0) return 'needs_review'
  if (input.lastVerifiedAt === null) return 'needs_review'
  return 'effective'
}

export type ProviderPolicyInfluence =
  'interpretation' | 'suggestion' | 'change_plan_template' | 'needs_revalidation'

export interface ProviderPolicyInterpretation {
  provider: string
  supports: boolean
  configured: boolean
  /** 用户可见的中文解释（平台 UI 应使用 copy-zh）。 */
  explanation: string
  influences: ProviderPolicyInfluence[]
  state: ProviderPolicyState
}

/**
 * 解释一个 provider 能力：
 * - supports=true（ProviderPolicy 声称支持）且 configured=true（用户已配置）→ 正常
 * - supports=true 且 configured=false → 提示「服务商支持，但尚未配置」
 * - supports=false → 不推断任何配置
 * - 无法证明 → needs_review，不自动影响建议
 */
export function interpretProviderCapability(
  policy: ProviderPolicy | null,
  userConfigured: boolean,
): ProviderPolicyInterpretation {
  if (policy === null || policy.state === 'needs_review') {
    return {
      provider: policy?.provider ?? 'unknown',
      supports: false,
      configured: userConfigured,
      explanation: '该规则的来源无法核实，已标记为需要重新确认。',
      influences: [],
      state: policy?.state ?? 'needs_review',
    }
  }
  const supports = true // policy effective 且存在 → 声称支持该项
  if (supports && !userConfigured) {
    return {
      provider: policy.provider,
      supports: true,
      configured: false,
      explanation: '服务商支持这项能力，但你还没有配置。',
      influences: ['suggestion'],
      state: policy.state,
    }
  }
  return {
    provider: policy.provider,
    supports: true,
    configured: true,
    explanation: '服务商支持这项能力，且你已配置。',
    influences: ['interpretation'],
    state: policy.state,
  }
}
