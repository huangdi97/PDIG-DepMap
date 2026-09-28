// 变更计划屏 + 验证屏（Quiet Infrastructure）。
// 语义铁律：verified（双勾 success 强）≠ completed（单勾 secondary 弱）≠ done。
// 阶段过滤用 CopyZh.planPhaseXxx 常量，不再出现 "准备/变更/验证" 中文 magic string。

import SwiftUI
import PDIGCore

struct ChangePlanScreen: View {
    @ObservedObject var session: AppSession
    let planId: String

    private var plan: ChangePlan? {
        session.plans.first { $0.id == planId }
    }

    var body: some View {
        VStack(spacing: 0) {
            AppTopBar(title: CopyZh.planChangePlan) { session.pop() }
            if let plan = plan {
                let view = ChangePlanViewModel.view(input: PlanInput(
                    plan: plan,
                    currentGraphRevision: session.snapshot.graphRevision,
                    pendingMustChange: 0,
                    pendingNeedsReview: session.snapshot.pendingProposals.filter { $0.decision == .pending }.count,
                    unresolvedCandidates: session.pendingCandidatesCount()
                ))
                ScrollView {
                    VStack(alignment: .leading, spacing: 12) {
                        Text(plan.title).font(PdigTheme.Font.pageTitle)
                        HStack {
                            Text("状态：\(view.workflowText)").font(.body)
                            Spacer()
                        }
                        PdigCard {
                            Text(view.readinessText)
                                .font(.body)
                                .foregroundStyle(.secondary)
                        }

                        phaseSection(CopyZh.planPhasePrepare, view.actions.filter { $0.phaseText == CopyZh.planPhasePrepare })
                        phaseSection(CopyZh.planPhaseChange, view.actions.filter { $0.phaseText == CopyZh.planPhaseChange })
                        phaseSection(CopyZh.planPhaseVerify, view.actions.filter { $0.phaseText == CopyZh.planPhaseVerify })

                        Button {
                            session.push(.verification)
                        } label: {
                            Text(view.ctaTitle).frame(maxWidth: .infinity).padding(.vertical, 8)
                        }
                        .buttonStyle(.borderedProminent)
                    }
                    .padding()
                }
            } else {
                Text(CopyZh.emptyTitle).foregroundStyle(.secondary).padding(.top, 64)
            }
        }
        .frame(minWidth: 420, minHeight: 600)
    }

    private func phaseSection(_ title: String, _ actions: [PlanActionView]) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            if !actions.isEmpty {
                SectionHeader(title)
                ForEach(actions) { a in
                    PdigCard {
                        HStack(alignment: .top) {
                            if let badge = statusBadge(for: a) {
                                badge
                            } else {
                                Image(systemName: "circle")
                                    .foregroundStyle(PdigTheme.Color.textTertiary)
                            }
                            VStack(alignment: .leading, spacing: 2) {
                                Text(a.title).font(.body)
                                Text(a.prerequisiteText).font(.caption).foregroundStyle(PdigTheme.Color.warning)
                                Text(a.verificationStatusText).font(.caption).foregroundStyle(.secondary)
                            }
                            Spacer()
                        }
                    }
                }
            }
        }
    }

    /// verified（双勾 success 强）> completed（单勾 secondary 弱）；其余按验证状态。
    private func statusBadge(for a: PlanActionView) -> StatusBadge? {
        if a.verificationStatusText == CopyZh.verificationVerified { return StatusBadge(.verified) }
        if a.verificationStatusText == CopyZh.verificationFailed { return StatusBadge(.blocked) }
        if a.verificationStatusText == CopyZh.verificationPending { return StatusBadge(.verifying) }
        if a.verificationStatusText == CopyZh.verificationEvidenceSuggested { return StatusBadge(.review) }
        if a.done && a.verificationStatusText == CopyZh.verificationNotRequired { return StatusBadge(.completed) }
        return nil
    }
}

struct VerificationScreen: View {
    @ObservedObject var session: AppSession

    private var view: VerificationView {
        VerificationViewModel.view(plans: session.plans.map { (plan: $0, hasIdentityFlow: false) })
    }

    var body: some View {
        VStack(spacing: 0) {
            AppTopBar(title: CopyZh.planPhaseVerify) { session.pop() }
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    NoticeBanner(icon: "clock.fill", text: view.newPathIsNotVerifiedNotice, color: PdigTheme.Color.warning)

                    section(CopyZh.verificationPending, view.pending)
                    section(CopyZh.verificationEvidenceSuggested, view.evidenceSuggested)
                    section(CopyZh.verificationVerified, view.verified)
                    section(CopyZh.verificationFailed, view.failed)

                    if view.pending.isEmpty && view.evidenceSuggested.isEmpty && view.failed.isEmpty {
                        Text("没有需要验证的变更。").foregroundStyle(.secondary).padding(.top, 24)
                    }
                }
                .padding()
            }
        }
        .frame(minWidth: 420, minHeight: 600)
    }

    private func section(_ title: String, _ rows: [VerificationRow]) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            if !rows.isEmpty {
                SectionHeader(title)
                ForEach(rows) { r in
                    PdigCard {
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(r.title).font(.body)
                                Text(r.planTitle).font(.caption).foregroundStyle(.secondary)
                            }
                            Spacer()
                            StatusBadge(badgeKind(for: r))
                        }
                    }
                }
            }
        }
    }

    private func badgeKind(for r: VerificationRow) -> StatusKind {
        switch r.status {
        case .verified: return .verified
        case .failed: return .blocked
        case .pending: return .verifying
        case .evidenceSuggested: return .review
        case .notRequired: return .completed
        }
    }
}