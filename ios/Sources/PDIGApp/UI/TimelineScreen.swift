// 时间线屏 + 漂移复查屏。
// 时间戳一律来自 PdigClock.nowIso()（真实 now）；固定假时间禁止进生产 UI。

import SwiftUI
import PDIGCore

struct TimelineScreen: View {
    @ObservedObject var session: AppSession

    private var sections: [TimelineSection] {
        TimelineViewModel.sections(
            snapshot: session.snapshot,
            plans: session.plans,
            drifts: openDrifts(),
            nowIso: PdigClock.nowIso()
        )
    }

    var body: some View {
        VStack(spacing: 0) {
            AppTopBar(title: "时间线") { session.pop() }
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    if sections.isEmpty {
                        EmptyState(
                            icon: "calendar",
                            title: "时间线还是空的",
                            message: "创建变更计划后，验证与到期节点会出现在这里。"
                        )
                    }
                    ForEach(sections) { section in
                        VStack(alignment: .leading, spacing: 6) {
                            SectionHeader(section.title)
                            ForEach(section.items, id: \.id) { item in
                                PdigCard {
                                    HStack(alignment: .top) {
                                        Image(systemName: kindIcon(item.kind))
                                            .foregroundStyle(.secondary)
                                        VStack(alignment: .leading, spacing: 2) {
                                            Text(item.title).font(.body)
                                            Text(item.subtitle).font(.caption).foregroundStyle(.secondary)
                                        }
                                        Spacer()
                                    }
                                }
                            }
                        }
                    }
                }
                .padding()
            }
        }
        .frame(minWidth: 420, minHeight: 600)
    }

    private func openDrifts() -> [GraphRepository.DriftRow] {
        session.openDrifts()
    }

    private func kindIcon(_ kind: String) -> String {
        switch kind {
        case TimelineItemKind.needsAttention.rawValue: return "exclamationmark.triangle"
        case TimelineItemKind.upcomingChange.rawValue: return "calendar"
        case TimelineItemKind.verificationPending.rawValue: return "checkmark.circle"
        case TimelineItemKind.freshnessReview.rawValue: return "arrow.triangle.2.circlepath"
        case TimelineItemKind.driftReview.rawValue: return "arrow.left.arrow.right"
        case TimelineItemKind.expiration.rawValue: return "clock"
        default: return "circle"
        }
    }
}

struct DriftScreen: View {
    @ObservedObject var session: AppSession
    let driftId: String

    var body: some View {
        VStack(spacing: 0) {
            AppTopBar(title: CopyZh.driftPossibleChange) { session.pop() }
            VStack(alignment: .leading, spacing: 12) {
                let drifts = session.openDrifts()
                if let d = drifts.first(where: { $0.id == driftId }) {
                    Text("\(session.snapshot.name(of: d.targetNodeId)) 相关的支付路径可能发生了变化。")
                        .font(.body)
                    Text("观察次数：\(d.observationCount) · 类型：\(kindText(d.kind))")
                        .font(.caption).foregroundStyle(.secondary)
                    HStack {
                        Button {
                            dismissDrift(d.id, resolve: true)
                        } label: { Text(CopyZh.driftConfirmReplacement).frame(maxWidth: .infinity).padding(.vertical, 6) }
                            .buttonStyle(.borderedProminent)
                        Button {
                            dismissDrift(d.id, resolve: false)
                        } label: { Text(CopyZh.driftDismiss).frame(maxWidth: .infinity).padding(.vertical, 6) }
                            .buttonStyle(.bordered)
                    }
                    Text("确认只会记录你的选择；不会自动修改已确认图（" + CopyZh.driftPossibleChange + "）。")
                        .font(.caption2).foregroundStyle(.secondary)
                } else {
                    Text(CopyZh.emptyTitle).foregroundStyle(.secondary).padding(.top, 32)
                }
            }
            .padding()
            Spacer()
        }
        .frame(minWidth: 420, minHeight: 600)
    }

    private func kindText(_ kind: String) -> String {
        switch kind {
        case DriftKind.possibleReplacement.wire: return "可能更换了来源"
        case DriftKind.possibleAdditionalPath.wire: return "可能新增了支付路径"
        case DriftKind.relationReappeared.wire: return "关系可能重新出现"
        default: return kind
        }
    }

    private func dismissDrift(_ id: String, resolve: Bool) {
        session.refresh()
        session.root()
    }
}