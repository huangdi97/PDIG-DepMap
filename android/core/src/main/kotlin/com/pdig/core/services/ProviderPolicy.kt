package com.pdig.core.services

import com.pdig.core.generated.ProviderPolicyState

/**
 * ProviderPolicy — Canonical v0.3.0 Provider Knowledge Plane
 * （port of core/src/services/provider-policy.ts）。
 *
 * 铁律（V030-PP-01）：
 * - Provider supports X != User has configured X
 * - ProviderPolicy 可以影响解释 / 建议 / ChangePlan 模板 / 触发 needs_revalidation
 * - ProviderPolicy 不能：自动创建 Dependency、自动确认手机号、自动标 required、
 *   自动把用户配置写入 Reality
 * - 无法证明当前规则 → needs_review，不得硬编码成永恒 truth
 */

data class ProviderPolicy(
    val provider: String,
    val policyType: String,
    val sourceUrl: String,
    val retrievedAt: String,
    val lastVerifiedAt: String? = null,
    val effectiveFrom: String? = null,
    val effectiveTo: String? = null,
    val jurisdiction: String? = null,
    val accountTypeScope: String? = null,
    val policyRevision: Int = 0,
    val state: ProviderPolicyState,
)

data class ProviderPolicyInput(
    val provider: String,
    val policyType: String,
    val sourceUrl: String?,
    val retrievedAt: String,
    val lastVerifiedAt: String?,
    val policyRevision: Int,
)

/** 由 provenance 推导 state：缺 sourceUrl 或 retrieval 过旧 → needs_review。 */
fun inferProviderPolicyState(input: ProviderPolicyInput): ProviderPolicyState {
    if (input.sourceUrl == null || input.sourceUrl.isEmpty()) return ProviderPolicyState.NEEDS_REVIEW
    if (input.lastVerifiedAt == null) return ProviderPolicyState.NEEDS_REVIEW
    return ProviderPolicyState.EFFECTIVE
}

enum class ProviderPolicyInfluence(val wire: String) {
    INTERPRETATION("interpretation"),
    SUGGESTION("suggestion"),
    CHANGE_PLAN_TEMPLATE("change_plan_template"),
    NEEDS_REVALIDATION("needs_revalidation"),
}

data class ProviderPolicyInterpretation(
    val provider: String,
    val supports: Boolean,
    val configured: Boolean,
    /** 用户可见的中文解释（平台 UI 应使用 copy-zh）。 */
    val explanation: String,
    val influences: List<ProviderPolicyInfluence>,
    val state: ProviderPolicyState,
)

/**
 * 解释一个 provider 能力：
 * - supports=true（ProviderPolicy 声称支持）且 configured=true（用户已配置）→ 正常
 * - supports=true 且 configured=false → 提示「服务商支持，但尚未配置」
 * - supports=false → 不推断任何配置
 * - 无法证明 → needs_review，不自动影响建议
 */
fun interpretProviderCapability(
    policy: ProviderPolicy?,
    userConfigured: Boolean,
): ProviderPolicyInterpretation {
    if (policy == null || policy.state == ProviderPolicyState.NEEDS_REVIEW) {
        return ProviderPolicyInterpretation(
            provider = policy?.provider ?: "unknown",
            supports = false,
            configured = userConfigured,
            explanation = "该规则的来源无法核实，已标记为需要重新确认。",
            influences = emptyList(),
            state = policy?.state ?: ProviderPolicyState.NEEDS_REVIEW,
        )
    }
    if (!userConfigured) {
        return ProviderPolicyInterpretation(
            provider = policy.provider,
            supports = true,
            configured = false,
            explanation = "服务商支持这项能力，但你还没有配置。",
            influences = listOf(ProviderPolicyInfluence.SUGGESTION),
            state = policy.state,
        )
    }
    return ProviderPolicyInterpretation(
        provider = policy.provider,
        supports = true,
        configured = true,
        explanation = "服务商支持这项能力，且你已配置。",
        influences = listOf(ProviderPolicyInfluence.INTERPRETATION),
        state = policy.state,
    )
}
