// Home 屏（task #3）：需要你处理 / 可能发生了变化 / 即将到来 /
// 常用场景 / 我的基础设施 / 基础设施薄弱点 + 底部主导航。

import SwiftUI
import PDIGCore

/// 底部主导航（Home / 薄弱点 / 场景 / 时间线 / 设置）。
struct AppTabBar: View {
    @ObservedObject var session: AppSession

    var body: some View {
        HStack {
            tabBarItem(icon: "house", title: "首页") { session.root() }
            tabBarItem(icon: "exclamationmark.triangle", title: CopyZh.findingsTitle) { session.push(.findings) }
            tabBarItem(icon: "rectangle.stack", title: "场景") { session.push(.scenarioCenter) }
            tabBarItem(icon: "clock", title: "时间线") { session.push(.timeline) }
            tabBarItem(icon: "gearshape", title: CopyZh.settingsTitle) { session.push(.settings) }
        }
        .padding(.vertical, 8)
        .background(.bar)
    }

    private func tabBarItem(icon: String, title: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            VStack(spacing: 2) {
                Image(systemName: icon)
                Text(title).font(.caption2)
            }
            .frame(maxWidth: .infinity)
        }
        .buttonStyle(.plain)
    }
}

struct HomeScreen: View {
    @ObservedObject var session: AppSession

    private var sections: [HomeSection] {
        HomeViewModel.sections(HomeInput(
            snapshot: session.snapshot,
            timelineItems: timelineItems(),
            openDrifts: openDrifts(),
            findings: findings()
        ))
    }

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Text(CopyZh.aboutAppName).font(.title2.bold())
                Spacer()
                Button {
                    session.lockNow()
                } label: {
                    Image(systemName: "lock")
                }
            }
            .padding(.horizontal)
            .padding(.top, 8)

            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    if session.snapshot.nodes.isEmpty && session.plans.isEmpty {
                        emptyState
                    }
                    ForEach(sections, id: \.title) { section in
                        HomeSectionView(session: session, section: section)
                    }
                }
                .padding()
            }
            AppTabBar(session: session)
        }
        .frame(minWidth: 420, minHeight: 600)
    }

    private var emptyState: some View {
        VStack(spacing: 12) {
            Text(CopyZh.emptyTitle).font(.headline)
            Text("导入一份账单，或从备份恢复，开始构建你的依赖图。")
                .foregroundStyle(.secondary)
            Button {
                session.push(.importFlow)
            } label: {
                Label("导入账单", systemImage: "tray.and.arrow.down")
            }
            .buttonStyle(.borderedProminent)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 32)
    }

    private func timelineItems() -> [Timeline.Item] {
        TimelineViewModel.sections(snapshot: session.snapshot, plans: session.plans, drifts: openDrifts(), nowIso: "2030-01-15T00:00:00+00:00")
            .flatMap { $0.items }
    }

    private func openDrifts() -> [GraphRepository.DriftRow] {
        session.openDrifts()
    }

    private func findings() -> [FindingItem] {
        FindingsViewModel.findings(FindingsInput(
            snapshot: session.snapshot,
            pendingVerifications: pendingVerifications(),
            nowIso: "2030-01-15T00:00:00+00:00"
        ))
    }

    private func pendingVerifications() -> [String] {
        session.plans
            .flatMap { $0.actions }
            .filter { $0.verification?.status == .pending || $0.verification?.status == .evidenceSuggested }
            .map { $0.id }
    }
}

struct HomeSectionView: View {
    @ObservedObject var session: AppSession
    let section: HomeSection

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(section.title)
                .font(.headline)
            ForEach(section.items) { item in
                Button {
                    route(to: item)
                } label: {
                HStack(spacing: 10) {
                        Image(systemName: severityIcon(item.severity))
                            .foregroundStyle(severityColor(item.severity))
                        VStack(alignment: .leading, spacing: 2) {
                            Text(item.title).font(.body.weight(item.severity >= 2 ? .semibold : .regular))
                            if !item.subtitle.isEmpty {
                                Text(item.subtitle)
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                                    .lineLimit(2)
                            }
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
    }

    private func route(to item: HomeItem) {
        if item.routeHint == "scenarioCenter" || item.routeHint == "scenario" {
            session.push(.scenarioCenter)
        } else if item.routeHint.hasPrefix("scenario:") {
            session.push(.scenario(String(item.routeHint.dropFirst("scenario:".count))))
        } else if item.routeHint.hasPrefix("node:") {
            session.push(.node(String(item.routeHint.dropFirst("node:".count))))
        } else if let r = routeFromHint(item.routeHint) {
            session.push(r)
        }
    }

    private func routeFromHint(_ hint: String) -> AppRoute? {
        switch hint {
        case "timeline": return .timeline
        case "findings": return .findings
        case "drift": return .findings
        case "import": return .importFlow
        default: return nil
        }
    }

    private func severityIcon(_ s: Int) -> String {
        switch s {
        case 2: return "exclamationmark.triangle.fill"
        case 1: return "exclamationmark.circle"
        default: return "circle.fill"
        }
    }

    private func severityColor(_ s: Int) -> Color {
        switch s {
        case 2: return .red
        case 1: return .orange
        default: return .secondary
        }
    }
}
