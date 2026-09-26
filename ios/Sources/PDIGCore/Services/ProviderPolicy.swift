// ProviderPolicy —— Canonical vNext (v0.3.0) Provider Knowledge Plane（Swift 移植）。
//
// 源头：core/src/services/provider-policy.ts（TypeScript reference，逐行忠实移植）。
//
// 铁律（V030-PP-01）：
// - Provider supports X != User has configured X
// - ProviderPolicy 可以影响解释 / 建议 / ChangePlan 模板 / 触发 needs_revalidation
// - ProviderPolicy 不能：自动创建 Dependency、自动确认手机号、自动标 required、
//   自动把用户配置写入 Reality
// - 无法证明当前规则 → needs_review，不得硬编码成永恒 truth

import Foundation

public struct ProviderPolicy: Equatable, Sendable {
    public let provider: String
    public let policyType: String
    public let sourceUrl: String
    public let retrievedAt: String
    public let lastVerifiedAt: String?
    public let effectiveFrom: String?
    public let effectiveTo: String?
    public let jurisdiction: String?
    public let accountTypeScope: String?
    public let parameters: Json
    public let policyRevision: Int
    public let state: ProviderPolicyState

    public init(
        provider: String,
        policyType: String,
        sourceUrl: String,
        retrievedAt: String,
        lastVerifiedAt: String?,
        effectiveFrom: String?,
        effectiveTo: String?,
        jurisdiction: String?,
        accountTypeScope: String?,
        parameters: Json,
        policyRevision: Int,
        state: ProviderPolicyState
    ) {
        self.provider = provider
        self.policyType = policyType
        self.sourceUrl = sourceUrl
        self.retrievedAt = retrievedAt
        self.lastVerifiedAt = lastVerifiedAt
        self.effectiveFrom = effectiveFrom
        self.effectiveTo = effectiveTo
        self.jurisdiction = jurisdiction
        self.accountTypeScope = accountTypeScope
        self.parameters = parameters
        self.policyRevision = policyRevision
        self.state = state
    }

    // Json 不是 Equatable（自包含解析器刻意不合成），parameters 按规范化 JSON 文本比较
    // （同一输入解析出的键序固定，所以 write 输出是确定性指纹）。
    public static func == (lhs: ProviderPolicy, rhs: ProviderPolicy) -> Bool {
        lhs.provider == rhs.provider &&
            lhs.policyType == rhs.policyType &&
            lhs.sourceUrl == rhs.sourceUrl &&
            lhs.retrievedAt == rhs.retrievedAt &&
            lhs.lastVerifiedAt == rhs.lastVerifiedAt &&
            lhs.effectiveFrom == rhs.effectiveFrom &&
            lhs.effectiveTo == rhs.effectiveTo &&
            lhs.jurisdiction == rhs.jurisdiction &&
            lhs.accountTypeScope == rhs.accountTypeScope &&
            JsonWriter.write(lhs.parameters) == JsonWriter.write(rhs.parameters) &&
            lhs.policyRevision == rhs.policyRevision &&
            lhs.state == rhs.state
    }
}

public struct ProviderPolicyInput: Equatable, Sendable {
    public let provider: String
    public let policyType: String
    public let sourceUrl: String?
    public let retrievedAt: String
    public let lastVerifiedAt: String?
    public let policyRevision: Int

    public init(
        provider: String,
        policyType: String,
        sourceUrl: String?,
        retrievedAt: String,
        lastVerifiedAt: String?,
        policyRevision: Int
    ) {
        self.provider = provider
        self.policyType = policyType
        self.sourceUrl = sourceUrl
        self.retrievedAt = retrievedAt
        self.lastVerifiedAt = lastVerifiedAt
        self.policyRevision = policyRevision
    }
}

public enum ProviderPolicyInfluence: String, CaseIterable, Sendable, Equatable {
    case interpretation = "interpretation"
    case suggestion = "suggestion"
    case changePlanTemplate = "change_plan_template"
    case needsRevalidation = "needs_revalidation"

    public var wire: String { rawValue }
}

public struct ProviderPolicyInterpretation: Equatable, Sendable {
    public let provider: String
    public let supports: Bool
    public let configured: Bool
    /// 用户可见的中文解释（平台 UI 应使用 copy-zh）。
    public let explanation: String
    public let influences: [ProviderPolicyInfluence]
    public let state: ProviderPolicyState

    public init(
        provider: String,
        supports: Bool,
        configured: Bool,
        explanation: String,
        influences: [ProviderPolicyInfluence],
        state: ProviderPolicyState
    ) {
        self.provider = provider
        self.supports = supports
        self.configured = configured
        self.explanation = explanation
        self.influences = influences
        self.state = state
    }
}

public enum ProviderPolicyEngine {
    /// 由 provenance 推导 state：缺 sourceUrl 或 retrieval 过旧 → needs_review。
    public static func inferProviderPolicyState(_ input: ProviderPolicyInput) -> ProviderPolicyState {
        if input.sourceUrl == nil || input.sourceUrl!.isEmpty { return .needsReview }
        if input.lastVerifiedAt == nil { return .needsReview }
        return .effective
    }

    /// 解释一个 provider 能力：
    /// - supports=true（ProviderPolicy 声称支持）且 configured=true（用户已配置）→ 正常
    /// - supports=true 且 configured=false → 提示「服务商支持，但尚未配置」
    /// - supports=false → 不推断任何配置
    /// - 无法证明 → needs_review，不自动影响建议
    public static func interpretProviderCapability(
        policy: ProviderPolicy?,
        userConfigured: Bool
    ) -> ProviderPolicyInterpretation {
        if policy == nil || policy!.state == .needsReview {
            return ProviderPolicyInterpretation(
                provider: policy?.provider ?? "unknown",
                supports: false,
                configured: userConfigured,
                explanation: "该规则的来源无法核实，已标记为需要重新确认。",
                influences: [],
                state: policy?.state ?? .needsReview
            )
        }
        // policy effective 且存在 → 声称支持该项
        if !userConfigured {
            return ProviderPolicyInterpretation(
                provider: policy!.provider,
                supports: true,
                configured: false,
                explanation: "服务商支持这项能力，但你还没有配置。",
                influences: [.suggestion],
                state: policy!.state
            )
        }
        return ProviderPolicyInterpretation(
            provider: policy!.provider,
            supports: true,
            configured: true,
            explanation: "服务商支持这项能力，且你已配置。",
            influences: [.interpretation],
            state: policy!.state
        )
    }
}
