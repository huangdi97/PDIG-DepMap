// 变更计划屏（task #9：Understand→Prepare→Change→Verify + CTA 映射）
// + 验证屏（task #10：pending / evidence_suggested / verified / failed）。

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
            AppTopBar(title: "变更计划") { session.pop() }
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
                        Text(plan.title).font(.title2.bold())
                        HStack {
                            Text("状态：\(view.workflowText)").font(.body)
                            Spacer()
                        }
                        Text(view.readinessText)
                            .font(.body)
                            .foregroundStyle(.secondary)
                            .padding(10)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(Color.gray.opacity(0.12), in: RoundedRectangle(cornerRadius: 10))

                        phaseSection("准备", view.actions.filter { $0.phaseText == "准备" })
                        phaseSection("变更", view.actions.filter { $0.phaseText == "变更" })
                        phaseSection("验证", view.actions.filter { $0.phaseText == "验证" })

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
                Text(title).font(.headline)
                ForEach(actions) { a in
                    HStack(alignment: .top) {
                        Image(systemName: a.done ? "checkmark.circle.fill" : "circle")
                            .foregroundStyle(a.done ? .green : .secondary)
                        VStack(alignment: .leading, spacing: 2) {
                            Text(a.title).font(.body)
                            Text(a.prerequisiteText).font(.caption).foregroundStyle(.orange)
                            Text(a.verificationStatusText).font(.caption).foregroundStyle(.secondary)
                        }
                        Spacer()
                    }
                    .padding(10)
                    .background(Color.gray.opacity(0.12), in: RoundedRectangle(cornerRadius: 10))
                }
            }
        }
    }
}

struct VerificationScreen: View {
    @ObservedObject var session: AppSession

    private var view: VerificationView {
        VerificationViewModel.view(plans: session.plans.map { (plan: $0, hasIdentityFlow: false) })
    }

    var body: some View {
        VStack(spacing: 0) {
            AppTopBar(title: "验证") { session.pop() }
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    Text(view.newPathIsNotVerifiedNotice)
                        .font(.footnote)
                        .foregroundStyle(.orange)
                        .padding(10)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(Color.orange.opacity(0.1), in: RoundedRectangle(cornerRadius: 10))

                    section("待验证", view.pending, color: .orange)
                    section("发现可能是变更后的证据", view.evidenceSuggested, color: .yellow)
                    section("已验证", view.verified, color: .green)
                    section("验证未通过", view.failed, color: .red)

                    if view.pending.isEmpty && view.evidenceSuggested.isEmpty && view.failed.isEmpty {
                        Text("没有需要验证的变更。").foregroundStyle(.secondary).padding(.top, 24)
                    }
                }
                .padding()
            }
        }
        .frame(minWidth: 420, minHeight: 600)
    }

    private func section(_ title: String, _ rows: [VerificationRow], color: Color) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            if !rows.isEmpty {
                Text(title).font(.headline).foregroundStyle(color)
                ForEach(rows) { r in
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(r.title).font(.body)
                            Text(r.planTitle).font(.caption).foregroundStyle(.secondary)
                        }
                        Spacer()
                        Text(r.statusText).font(.caption).foregroundStyle(color)
                    }
                    .padding(10)
                    .background(Color.gray.opacity(0.12), in: RoundedRectangle(cornerRadius: 10))
                }
            }
        }
    }
}
