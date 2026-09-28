// 基础设施屏（按对象 / 按能力）+ 节点详情。
// 分组文案保持用户语义：身份与恢复 / 访问与认证 / 支付 / 设备 / 关键服务（copy-zh.capabilityXxx）。

import SwiftUI
import PDIGCore

struct InfrastructureScreen: View {
    @ObservedObject var session: AppSession
    @State private var mode: Int = 0

    var body: some View {
        VStack(spacing: 0) {
            AppTopBar(title: CopyZh.homeMyInfrastructure) { session.pop() }
            Picker("", selection: $mode) {
                Text(CopyZh.infraByItem).tag(0)
                Text(CopyZh.infraByCapability).tag(1)
            }
            .pickerStyle(.segmented)
            .padding(.horizontal)
            .padding(.bottom, 4)

            ScrollView {
                let groups = mode == 0
                    ? InfrastructureViewModel.byItem(session.snapshot)
                    : InfrastructureViewModel.byCapability(session.snapshot)
                if groups.isEmpty {
                    EmptyState(
                        icon: "building.2",
                        title: CopyZh.emptyTitle,
                        message: "还没有可展示的基础设施对象。导入账单或从备份恢复后，这里会列出它们。",
                        actionTitle: "导入账单"
                    ) {
                        session.push(.importFlow)
                    }
                } else {
                    VStack(alignment: .leading, spacing: 14) {
                        ForEach(groups) { group in
                            VStack(alignment: .leading, spacing: 6) {
                                SectionHeader(group.title)
                                ForEach(group.members) { m in
                                    Button {
                                        session.push(.node(m.id))
                                    } label: {
                                        PdigCard {
                                            HStack {
                                                Text(m.name).font(.body)
                                                Text(m.kindLabel).font(.caption).foregroundStyle(.tertiary)
                                                Spacer()
                                                Text(m.summary).font(.caption).foregroundStyle(.secondary).lineLimit(1)
                                            }
                                        }
                                    }
                                    .buttonStyle(.plain)
                                }
                            }
                        }
                    }
                    .padding()
                }
            }
        }
        .frame(minWidth: 420, minHeight: 600)
    }
}

struct NodeDetailScreen: View {
    @ObservedObject var session: AppSession
    let nodeId: String

    var body: some View {
        VStack(spacing: 0) {
            AppTopBar(title: "节点详情") { session.pop() }
            if let detail = NodeDetailViewModel.detail(
                nodeId: nodeId,
                snapshot: session.snapshot,
                findings: []
            ) {
                ScrollView {
                    VStack(alignment: .leading, spacing: 10) {
                        HStack {
                            Text(detail.node.name).font(.title2.bold())
                            Text(detail.kindLabel).font(.caption).padding(4)
                                .background(PdigTheme.Color.primarySoft, in: Capsule())
                                .foregroundStyle(PdigTheme.Color.primary)
                            Spacer()
                        }
                        ForEach(detail.cards) { card in
                            PdigCard {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(card.title).font(.subheadline.weight(.semibold))
                                    Text(card.body).font(.footnote).foregroundStyle(.secondary)
                                }
                            }
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
}