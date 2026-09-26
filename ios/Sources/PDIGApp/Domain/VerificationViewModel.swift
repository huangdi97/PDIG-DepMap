// VerificationViewModel —— 验证屏（task #10）。
//
// 状态：pending / evidence_suggested / verified / failed / not_required。
// 产品铁律：新手机号已添加 ≠ 新恢复路径已经验证 —— 只有显式验证到 verified
// 才允许后续 retire 动作（MakeBeforeBreakEngine 保证顺序）。

import Foundation
import PDIGCore

public struct VerificationRow: Equatable, Sendable, Identifiable {
    public let id: String
    public let title: String
    public let status: ActionVerificationStatus
    public let statusText: String
    /// 该验证动作所属变更计划标题。
    public let planTitle: String
    public let isVerified: Bool

    public init(
        id: String, title: String, status: ActionVerificationStatus,
        statusText: String, planTitle: String, isVerified: Bool
    ) {
        self.id = id
        self.title = title
        self.status = status
        self.statusText = statusText
        self.planTitle = planTitle
        self.isVerified = isVerified
    }
}

public struct VerificationView: Equatable, Sendable {
    public let pending: [VerificationRow]
    public let evidenceSuggested: [VerificationRow]
    public let verified: [VerificationRow]
    public let failed: [VerificationRow]
    public let newPathIsNotVerifiedNotice: String

    public init(
        pending: [VerificationRow],
        evidenceSuggested: [VerificationRow],
        verified: [VerificationRow],
        failed: [VerificationRow],
        newPathIsNotVerifiedNotice: String
    ) {
        self.pending = pending
        self.evidenceSuggested = evidenceSuggested
        self.verified = verified
        self.failed = failed
        self.newPathIsNotVerifiedNotice = newPathIsNotVerifiedNotice
    }
}

public enum VerificationViewModel {

    public static func text(_ status: ActionVerificationStatus) -> String {
        switch status {
        case .pending: return CopyZh.verificationPending
        case .evidenceSuggested: return CopyZh.verificationEvidenceSuggested
        case .verified: return CopyZh.verificationVerified
        case .failed: return CopyZh.verificationFailed
        case .notRequired: return CopyZh.verificationNotRequired
        }
    }

    /// 汇总所有计划的验证动作。
    public static func view(plans: [(plan: ChangePlan, hasIdentityFlow: Bool)]) -> VerificationView {
        var pending: [VerificationRow] = []
        var evidenceSuggested: [VerificationRow] = []
        var verified: [VerificationRow] = []
        var failed: [VerificationRow] = []

        for item in plans {
            for action in item.plan.actions {
                guard let v = action.verification else { continue }
                let row = VerificationRow(
                    id: "\(item.plan.id)-\(action.id)",
                    title: action.title,
                    status: v.status,
                    statusText: text(v.status),
                    planTitle: item.plan.title,
                    isVerified: v.status == .verified
                )
                switch v.status {
                case .pending: pending.append(row)
                case .evidenceSuggested: evidenceSuggested.append(row)
                case .verified: verified.append(row)
                case .failed: failed.append(row)
                case .notRequired: break
                }
            }
        }
        return VerificationView(
            pending: pending,
            evidenceSuggested: evidenceSuggested,
            verified: verified,
            failed: failed,
            newPathIsNotVerifiedNotice: CopyZh.verificationNewPathIsNotVerified
        )
    }
}