// 基础设施屏（task #6：按对象 / 按能力）+ 节点详情（task #7：六问卡）。

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
                    Text(CopyZh.emptyTitle).foregroundStyle(.secondary).padding(.top, 64)
                } else {
                    VStack(alignment: .leading, spacing: 14) {
                        ForEach(groups) { group in
                            VStack(alignment: .leading, spacing: 6) {
                                Text(group.title).font(.headline)
                                ForEach(group.members) { m in
                                    Button {
                                        session.push(.node(m.id))
                                    } label: {
                                        HStack {
                                            Text(m.name).font(.body)
                                            Text(m.kindLabel).font(.caption).foregroundStyle(.tertiary)
                                            Spacer()
                                            Text(m.summary).font(.caption).foregroundStyle(.secondary).lineLimit(1)
                                        }
                                        .padding(8)
                                        .background(Color.gray.opacity(0.12), in: RoundedRectangle(cornerRadius: 8))
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
                                .background(Color.gray.opacity(0.15), in: Capsule())
                            Spacer()
                        }
                        ForEach(detail.cards) { card in
                            VStack(alignment: .leading, spacing: 4) {
                                Text(card.title).font(.subheadline.weight(.semibold))
                                Text(card.body).font(.footnote).foregroundStyle(.secondary)
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(10)
                            .background(Color.gray.opacity(0.12), in: RoundedRectangle(cornerRadius: 10))
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
