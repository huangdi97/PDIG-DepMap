// ProviderPolicy — Canonical v0.3.0 Provider Knowledge Plane
// （port of core/src/services/provider-policy.ts）。
//
// 铁律（V030-PP-01）：
// - Provider supports X != User has configured X
// - ProviderPolicy 可以影响解释 / 建议 / ChangePlan 模板 / 触发 needs_revalidation
// - ProviderPolicy 不能：自动创建 Dependency、自动确认手机号、自动标 required、
//   自动把用户配置写入 Reality
// - 无法证明当前规则 → needs_review，不得硬编码成永恒 truth
//
// 分层约束：本模块是 services 层纯逻辑，**零 @ohos 依赖**。

import { ProviderPolicyState } from '../generated/CanonicalEnums';

/** 一条 provider 政策（provenance 已由调用方判定 state）。 */
export class ProviderPolicy {
  provider: string = '';
  policyType: string = '';
  sourceUrl: string = '';
  retrievedAt: string = '';
  lastVerifiedAt: string | null = null;
  effectiveFrom: string | null = null;
  effectiveTo: string | null = null;
  jurisdiction: string | null = null;
  accountTypeScope: string | null = null;
  policyRevision: number = 0;
  state: ProviderPolicyState = ProviderPolicyState.NEEDS_REVIEW;
}

/** inferProviderPolicyState 的输入（只有 provenance 相关字段）。 */
export class ProviderPolicyInput {
  provider: string = '';
  policyType: string = '';
  sourceUrl: string | null = null;
  retrievedAt: string = '';
  lastVerifiedAt: string | null = null;
  policyRevision: number = 0;
}

/** 由 provenance 推导 state：缺 sourceUrl 或 retrieval 过旧 → needs_review。 */
export function inferProviderPolicyState(input: ProviderPolicyInput): ProviderPolicyState {
  if (input.sourceUrl === null || input.sourceUrl.length === 0) {
    return ProviderPolicyState.NEEDS_REVIEW;
  }
  if (input.lastVerifiedAt === null) {
    return ProviderPolicyState.NEEDS_REVIEW;
  }
  return ProviderPolicyState.EFFECTIVE;
}

export enum ProviderPolicyInfluence {
  INTERPRETATION = 'interpretation',
  SUGGESTION = 'suggestion',
  CHANGE_PLAN_TEMPLATE = 'change_plan_template',
  NEEDS_REVALIDATION = 'needs_revalidation',
}

/** interpretProviderCapability 的确定性结果。 */
export class ProviderPolicyInterpretation {
  provider: string = '';
  supports: boolean = false;
  configured: boolean = false;
  /** 用户可见的中文解释（平台 UI 应使用 copy-zh）。 */
  explanation: string = '';
  influences: ProviderPolicyInfluence[] = [];
  state: ProviderPolicyState = ProviderPolicyState.NEEDS_REVIEW;
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
  if (policy === null || policy.state === ProviderPolicyState.NEEDS_REVIEW) {
    const result = new ProviderPolicyInterpretation();
    result.provider = policy === null ? 'unknown' : policy.provider;
    result.supports = false;
    result.configured = userConfigured;
    result.explanation = '该规则的来源无法核实，已标记为需要重新确认。';
    result.influences = [];
    result.state = policy === null ? ProviderPolicyState.NEEDS_REVIEW : policy.state;
    return result;
  }
  if (!userConfigured) {
    const result = new ProviderPolicyInterpretation();
    result.provider = policy.provider;
    result.supports = true;
    result.configured = false;
    result.explanation = '服务商支持这项能力，但你还没有配置。';
    result.influences = [ProviderPolicyInfluence.SUGGESTION];
    result.state = policy.state;
    return result;
  }
  const result = new ProviderPolicyInterpretation();
  result.provider = policy.provider;
  result.supports = true;
  result.configured = true;
  result.explanation = '服务商支持这项能力，且你已配置。';
  result.influences = [ProviderPolicyInfluence.INTERPRETATION];
  result.state = policy.state;
  return result;
}