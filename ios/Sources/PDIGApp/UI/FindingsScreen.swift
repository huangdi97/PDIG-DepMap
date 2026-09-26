// Findings 屏（task #4）：薄弱点列表 + 六问详情。
// 六问：发现了什么 / 为什么 / 基于什么确认 / 还不知道什么 / 可能影响什么 / 下一步建议。

import SwiftUI
import PDIGCore

struct FindingsScreen: View {
    @ObservedObject var session: AppSession

    private var findings: [FindingItem] {
        FindingsViewModel.findings(FindingsInput(
            snapshot: session.snapshot,
            pendingVerifications: pendingVerifications(),
            nowIso: "2030-01-15T00:00:00+00:00"
        ))
    }

    var body: some View {
        VStack(spacing: 0) {
            AppTopBar(title: CopyZh.findingsTitle) { session.pop() }
            ScrollView {
                if findings.isEmpty {
                    Text(CopyZh.emptyTitle).foregroundStyle(.secondary).padding(.top, 64)
                } else {
                    VStack(alignment: .leading, spacing: 12) {
                        ForEach(findings) { f in
                            Button {
                                session.push(.finding(f.id))
                            } label: {
                                HStack {
                                    Image(systemName: icon(for: f.kind))
                                        .foregroundStyle(color(for: f.kind))
                                    VStack(alignment: .leading, spacing: 2) {
                                        Text(f.title).font(.body.weight(.semibold))
                                        Text(f.what).font(.caption).foregroundStyle(.secondary).lineLimit(2)
                                    }
                                    Spacer()
                                    Image(systemName: "chevron.right").font(.caption).foregroundStyle(.tertiary)
                                }
                                .padding(10)
                                .background(Color.gray.opacity(0.12), in: RoundedRectangle(cornerRadius: 10))
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

    private func icon(for kind: FindingItem.Kind) -> String {
        switch kind {
        case .singlePointOfFailure: return "1.circle"
        case .sharedFailureDomain: return "square.stack"
        case .recoveryCycle: return "arrow.triangle.2.circlepath"
        case .unconfirmedFallback: return "questionmark.circle"
        case .staleRecoveryInformation: return "clock.badge.exclamationmark"
        case .unknownCriticalPath: return "exclamationmark.triangle"
        case .pendingVerification: return "checkmark.circle.badge.questionmark"
        }
    }

    private func color(for kind: FindingItem.Kind) -> Color {
        switch kind {
        case .singlePointOfFailure, .recoveryCycle: return .red
        case .sharedFailureDomain: return .orange
        case .pendingVerification: return .yellow
        default: return .secondary
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
            nowIso: "2030-01-15T00:00:00+00:00"
        )).first { $0.id == findingId }
    }

    var body: some View {
        VStack(spacing: 0) {
            AppTopBar(title: CopyZh.findingsTitle) { session.pop() }
            if let f = finding {
                ScrollView {
                    VStack(alignment: .leading, spacing: 12) {
                        Text(f.title).font(.title2.bold())
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
        VStack(alignment: .leading, spacing: 4) {
            Text(title).font(.subheadline.weight(.semibold))
            Text(body).font(.body).foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(10)
        .background(Color.gray.opacity(0.12), in: RoundedRectangle(cornerRadius: 10))
    }
}
