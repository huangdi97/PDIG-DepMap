// Home 屏（Quiet Infrastructure）：需要你处理 → 薄弱点 → 可能变化 → 即将到来 →
// 常用场景 → 我的基础设施（顺序冻结于 PDIG_UIUX_DIRECTION_FREEZE.md §4）。

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
            .frame(minHeight: 44)
        }
        .buttonStyle(.plain)
        .accessibilityLabel(title)
    }
}

struct HomeScreen: View {
    @ObservedObject var session: AppSession

    /// 冻结信息顺序（仅展示层排序，不改 HomeViewModel 语义）。
    private var orderedSections: [HomeSection] {
        let order = [
            CopyZh.homeNeedsAction,
            CopyZh.homeInfrastructureWeakness,
            CopyZh.homePossibleChange,
            CopyZh.homeUpcoming,
            CopyZh.homeCommonScenarios,
            CopyZh.homeMyInfrastructure,
        ]
        return sections.sorted { a, b in
            let ai = order.firstIndex(of: a.title) ?? order.count
            let bi = order.firstIndex(of: b.title) ?? order.count
            return ai < bi
        }
    }

    private var sections: [HomeSection] {
        HomeViewModel.sections(HomeInput(
            snapshot: session.snapshot,
            timelineItems: timelineItems(),
            openDrifts: openDrifts(),
            findings: findings()
        ))
    }

    private var hasNothingToDo: Bool {
        !sections.contains { $0.title == CopyZh.homeNeedsAction }
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
                        .frame(width: 44, height: 44)
                }
                .accessibilityLabel(CopyZh.lockTitle)
            }
            .padding(.horizontal)
            .padding(.top, 8)

            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    if session.snapshot.nodes.isEmpty && session.plans.isEmpty {
                        emptyState
                    } else {
                        if hasNothingToDo {
                            healthySummary
                        }
                        ForEach(orderedSections, id: \.title) { section in
                            HomeSectionView(session: session, section: section)
                        }
                    }
                }
                .padding()
            }
            AppTabBar(session: session)
        }
        .frame(minWidth: 420, minHeight: 600)
    }

    /// healthy：不显示 0 问题/分数；讲清"当前没有立即事项 + 仍然未知的范围"。
    private var healthySummary: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(CopyZh.homeHealthyNow).font(PdigTheme.Font.section)
            if !session.snapshot.pendingProposals.isEmpty {
                Text(CopyZh.homeHealthyUnknown)
                    .font(PdigTheme.Font.secondary)
                    .foregroundStyle(PdigTheme.Color.textSecondary)
            }
        }
        .padding(.vertical, 4)
    }

    private var emptyState: some View {
        EmptyState(
            icon: "tray.and.arrow.down",
            title: CopyZh.emptyTitle,
            message: "导入一份账单，或从备份恢复，开始构建你的依赖图。",
            actionTitle: "导入账单"
        ) {
            session.push(.importFlow)
        }
    }

    private func timelineItems() -> [Timeline.Item] {
        TimelineViewModel.sections(snapshot: session.snapshot, plans: session.plans, drifts: openDrifts(), nowIso: PdigClock.nowIso())
            .flatMap { $0.items }
    }

    private func openDrifts() -> [GraphRepository.DriftRow] {
        session.openDrifts()
    }

    private func findings() -> [FindingItem] {
        FindingsViewModel.findings(FindingsInput(
            snapshot: session.snapshot,
            pendingVerifications: pendingVerifications(),
            nowIso: PdigClock.nowIso()
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
                    PdigCard {
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
                    }
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

    /// 严重度 → 语义色令牌（不再散用系统红/橙）。
    private func severityColor(_ s: Int) -> Color {
        switch s {
        case 2: return PdigTheme.Color.danger
        case 1: return PdigTheme.Color.warning
        default: return PdigTheme.Color.textSecondary
        }
    }
}
