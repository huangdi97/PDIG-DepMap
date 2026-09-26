// MakeBeforeBreak —— Canonical vNext (v0.3.0) 确定性引擎（Swift 移植）。
//
// 源头：core/src/domain/make-before-break.ts（TypeScript reference，逐行忠实移植）。
//
// Safety Invariant: BREAK_BEFORE_MAKE = FORBIDDEN（V030-MBB-01）
//
// 关键 recovery/access 变化必须按序：
//   new path established → new path verified → old path retired
// 测试必须证明：
//   new path not verified → retire old path = blocked
//   new path verified     → retire old path = allowed

import Foundation

public struct MakeBeforeBreakInput: Equatable, Sendable {
    /// 代表「新路径建立」的动作。
    public let newPathActions: [PlanAction]
    /// 代表「验证新路径」的动作。
    public let verificationActions: [PlanAction]
    /// 代表「退休旧路径」的动作（被 gate 保护）。
    public let retireActionId: String
    /// 退休动作是否已完成（已完成无需再 gate）。
    public let retireAlreadyDone: Bool

    public init(
        newPathActions: [PlanAction],
        verificationActions: [PlanAction],
        retireActionId: String,
        retireAlreadyDone: Bool
    ) {
        self.newPathActions = newPathActions
        self.verificationActions = verificationActions
        self.retireActionId = retireActionId
        self.retireAlreadyDone = retireAlreadyDone
    }
}

public struct MakeBeforeBreakResult: Equatable, Sendable {
    public let status: MakeBeforeBreakStatus
    /// 阻止退休的未验证新路径 action id 列表（确定性排序）。
    public let unverifiedNewPaths: [String]
    /// 可解释的 blocking reason（面向应用层；UI 需要转成自然中文）。
    public let reason: String

    public init(status: MakeBeforeBreakStatus, unverifiedNewPaths: [String], reason: String) {
        self.status = status
        self.unverifiedNewPaths = unverifiedNewPaths
        self.reason = reason
    }
}

public enum MakeBeforeBreakEngine {
    public static func evaluate(_ input: MakeBeforeBreakInput) -> MakeBeforeBreakResult {
        if input.retireAlreadyDone {
            return MakeBeforeBreakResult(
                status: .allowed,
                unverifiedNewPaths: [],
                reason: "old path already retired"
            )
        }

        let unverifiedNewPaths = input.newPathActions
            .filter { !$0.done }
            .map { $0.id }
            .sorted()

        if !unverifiedNewPaths.isEmpty {
            return MakeBeforeBreakResult(
                status: .blocked,
                unverifiedNewPaths: unverifiedNewPaths,
                reason: "new path(s) not yet established: " + unverifiedNewPaths.joined(separator: ", ")
            )
        }

        // 新路径动作全部完成；再检查验证状态（done ≠ verified）
        let unverified = input.verificationActions
            .filter { $0.verification?.status != .verified }
            .map { $0.id }
            .sorted()

        if !unverified.isEmpty {
            return MakeBeforeBreakResult(
                status: .blocked,
                unverifiedNewPaths: unverified,
                reason: "new path(s) not yet verified: " + unverified.joined(separator: ", ")
            )
        }

        return MakeBeforeBreakResult(
            status: .allowed,
            unverifiedNewPaths: [],
            reason: "all key new paths verified"
        )
    }
}
