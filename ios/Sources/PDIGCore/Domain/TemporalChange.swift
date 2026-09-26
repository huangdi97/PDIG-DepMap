// TemporalChange —— Canonical vNext (v0.3.0) 确定性引擎（Swift 移植）。
//
// 源头：core/src/domain/temporal-change.ts（TypeScript reference，逐行忠实移植）。
//
// 最小模型（不实现通用调度系统）：只支持 replace_phone_number / identity-recovery
// 所需的 transition window：
//
//   G_before → G_transition → G_after
//
// 字段：
//   effectiveAt            变更生效时间
//   verificationNotBefore  验证不早于
//   verificationDueAt      验证截止
//   retireOldPathAfter     旧路径最早退休时间（必须在所有关键新路径 verified 之后）
//
// 门（TC-ORDER-GATE / TC-RETIRE-GATE）：
//   effectiveAt <= verificationNotBefore <= verificationDueAt <= retireOldPathAfter
//   retireOldPathAfter 允许的前提 = all key new paths verified（BREAK_BEFORE_MAKE = FORBIDDEN）

import Foundation

public struct TemporalChangeWindow: Equatable, Sendable {
    public let effectiveAt: String
    public let verificationNotBefore: String?
    public let verificationDueAt: String?
    public let retireOldPathAfter: String?

    public init(
        effectiveAt: String,
        verificationNotBefore: String?,
        verificationDueAt: String?,
        retireOldPathAfter: String?
    ) {
        self.effectiveAt = effectiveAt
        self.verificationNotBefore = verificationNotBefore
        self.verificationDueAt = verificationDueAt
        self.retireOldPathAfter = retireOldPathAfter
    }
}

public struct TemporalChangeResult: Equatable, Sendable {
    public let phase: TemporalChangePhase
    public let validOrder: Bool
    /// 允许 retirement 的日期（满足 gate 时才非空）。
    public let retireAllowedAt: String?
    /// 阻止 retirement 的原因（自然语言给应用层）。
    public let retireBlockedReason: String?

    public init(
        phase: TemporalChangePhase,
        validOrder: Bool,
        retireAllowedAt: String?,
        retireBlockedReason: String?
    ) {
        self.phase = phase
        self.validOrder = validOrder
        self.retireAllowedAt = retireAllowedAt
        self.retireBlockedReason = retireBlockedReason
    }
}

public enum TemporalChangeEngine {
    public static func validateTemporalOrder(_ window: TemporalChangeWindow) -> Bool {
        let times: [String?] = [
            window.effectiveAt,
            window.verificationNotBefore,
            window.verificationDueAt,
            window.retireOldPathAfter,
        ]
        var prev: String? = nil
        for t in times {
            if t == nil { continue }
            if let p = prev, t! < p { return false }
            prev = t
        }
        return true
    }

    public static func classifyTemporalPhase(
        window: TemporalChangeWindow,
        now: String,
        allKeyNewPathsVerified: Bool
    ) -> TemporalChangeResult {
        let orderOk = validateTemporalOrder(window)

        if !orderOk {
            return TemporalChangeResult(
                phase: .before,
                validOrder: false,
                retireAllowedAt: nil,
                retireBlockedReason: "时间安排不合法：开始、验证、停用的顺序不正确。"
            )
        }

        let phase: TemporalChangePhase
        if now < window.effectiveAt {
            phase = .before
        } else if let retireAfter = window.retireOldPathAfter, now >= retireAfter {
            phase = .after
        } else {
            phase = .transition
        }

        if window.retireOldPathAfter == nil {
            return TemporalChangeResult(
                phase: phase,
                validOrder: true,
                retireAllowedAt: nil,
                retireBlockedReason: "未设置停用时间"
            )
        }

        if !allKeyNewPathsVerified {
            return TemporalChangeResult(
                phase: phase,
                validOrder: true,
                retireAllowedAt: nil,
                retireBlockedReason: "新路径尚未全部验证，暂时不能停用旧路径。"
            )
        }

        return TemporalChangeResult(
            phase: phase,
            validOrder: true,
            retireAllowedAt: window.retireOldPathAfter,
            retireBlockedReason: nil
        )
    }
}
