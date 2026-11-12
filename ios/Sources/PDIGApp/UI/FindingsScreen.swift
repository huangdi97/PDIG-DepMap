// Findings 屏（薄弱点）+ 六问详情。
// 每条 finding：标题（what）+ 一句人话 + StatusBadge（icon+label+color 三通道）；
// 展开（详情屏）：是什么 / 为什么 / 基于什么确认 / 还不知道什么 / 可能影响什么 / 下一步建议。
// 语义：unknown ≠ required；不得把"不确定"渲染成"必须处理"。

import SwiftUI
import PDIGCore

struct FindingsScreen: View {
    @ObservedObject var session: AppSession

    private var findings: [FindingItem] {
        FindingsViewModel.findings(FindingsInput(
            snapshot: session.snapshot,
            pendingVerifications: pendingVerifications(),
            nowIso: PdigClock.nowIso()
        ))
    }

    var body: some View {
        VStack(spacing: 0) {
            AppTopBar(title: CopyZh.findingsTitle) { session.pop() }
            ScrollView {
                if findings.isEmpty {
                    EmptyState(
                        icon: "checkmark.shield",
                        title: "没有发现薄弱点",
                        message: "当前基础设施没有检测到需要处理的薄弱点。发现新问题时会出现在这里。"
                    )
                } else {
                    VStack(alignment: .leading, spacing: 12) {
                        ForEach(findings) { f in
                            Button {
                                session.push(.finding(f.id))
                            } label: {
                                PdigCard {
                                    HStack(alignment: .top, spacing: 10) {
                                        StatusBadge(badgeKind(for: f.kind))
                                        VStack(alignment: .leading, spacing: 2) {
                                            Text(f.title).font(.body.weight(.semibold))
                                            Text(f.what).font(.caption).foregroundStyle(.secondary).lineLimit(2)
                                        }
                                        Spacer()
                                        Image(systemName: "chevron.right").font(.caption).foregroundStyle(.tertiary)
                                    }
                                }
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding()
                }
            }
        }
        .frame(minWidth: 420, minHeight: 600)
    }

    private func pendingVerifications() -> [String] {
        session.plans
            .flatMap { $0.actions }
            .filter { $0.verification?.status == .pending || $0.verification?.status == .evidenceSuggested }
            .map { $0.id }
    }

    private func badgeKind(for kind: FindingItem.Kind) -> StatusKind {
        switch kind {
        case .singlePointOfFailure, .recoveryCycle: return .blocked
        case .sharedFailureDomain, .staleRecoveryInformation: return .review
        case .unconfirmedFallback, .unknownCriticalPath: return .unknown
        case .pendingVerification: return .verifying
        }
    }
}

struct FindingDetailScreen: View {
    @ObservedObject var session: AppSession
    let findingId: String

    private var finding: FindingItem? {
        FindingsViewModel.findings(FindingsInput(
            snapshot: session.snapshot,
            pendingVerifications: [],
            nowIso: PdigClock.nowIso()
        )).first { $0.id == findingId }
    }

    var body: some View {
        VStack(spacing: 0) {
            AppTopBar(title: CopyZh.findingsTitle) { session.pop() }
            if let f = finding {
                ScrollView {
                    VStack(alignment: .leading, spacing: 12) {
                        Text(f.title).font(PdigTheme.Font.pageTitle)
                        detailRow(CopyZh.findingWhat, f.what)
                        detailRow(CopyZh.findingWhy, f.why)
                        detailRow(CopyZh.findingConfirmedBasis, f.confirmedBasis)
                        detailRow(CopyZh.findingUnknowns, f.unknowns)
                        detailRow(CopyZh.findingAffectedCapability, f.affectedCapability)
                        detailRow(CopyZh.findingNextAction, f.recommendedNextAction)
                        if let target = f.targetNodeId, let node = session.snapshot.node(id: target) {
                            Button {
                                session.push(.node(node.id))
                            } label: {
                                Label("查看 \(node.name)", systemImage: "arrow.up.right.square")
                            }
                            .buttonStyle(.bordered)
                        }
                    }
                    .padding()
                }
            } else {
                Text(CopyZh.emptyTitle).foregroundStyle(.secondary).padding(.top, 64)
            }
        }
        .frame(minWidth: 420, minHeight: 600)
    }

    private func detailRow(_ title: String, _ body: String) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            SectionHeader(title)
            PdigCard {
                Text(body).font(.body).foregroundStyle(.secondary)
            }
        }
    }
}