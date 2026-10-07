// RecordsSearchSourcesViews —— iOS-native task records, search and fact-boundary utilities.

import SwiftUI

struct RecordsView: View {
    @ObservedObject var model: VNextModel

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: VSpace.lg) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("记录").font(VFont.pageTitle()).foregroundColor(PdigV2Colors.textPrimary)
                    Text("追踪正在发生的变更、需要处理的风险，以及接下来已知的时间节点。")
                        .font(VFont.secondary()).foregroundColor(PdigV2Colors.textSecondary)
                }

                HStack(spacing: VSpace.sm) {
                    VRecordMetric(value: "\(VNextDemoFixture.activeChanges.count)", label: "进行中的变更")
                    VRecordMetric(value: "\(VNextDemoFixture.attentionItems.count)", label: "需要处理")
                    VRecordMetric(value: "\(VNextDemoFixture.upcoming.count)", label: "即将到来")
                }

                VSectionHeader(title: "正在进行")
                ForEach(VNextDemoFixture.activeChanges) { item in
                    Button { model.navigate(.changePhone) } label: {
                        HStack {
                            VStack(alignment: .leading, spacing: 3) {
                                Text(item.title).font(VFont.body()).fontWeight(.semibold).foregroundColor(PdigV2Colors.textPrimary)
                                Text("当前阶段 · 验证新号码").font(VFont.meta()).foregroundColor(PdigV2Colors.warning)
                            }
                            Spacer()
                            Text("继续查看 →").font(VFont.meta()).fontWeight(.semibold).foregroundColor(PdigV2Colors.primaryText)
                        }
                        .padding(VSpace.lg)
                    }
                    .buttonStyle(.plain)
                    .background(PdigV2Colors.primarySoft)
                    .clipShape(RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous))
                }

                VSectionHeader(title: "迁移进度")
                ForEach(Array(VNextDemoFixture.changeStages.prefix(4))) { stage in
                    HStack(spacing: VSpace.md) {
                        Text("\(stage.stage)")
                            .font(VFont.meta()).fontWeight(.bold)
                            .foregroundColor(stage.status == "completed" ? .white : PdigV2Colors.primaryText)
                            .frame(width: 28, height: 28)
                            .background(stage.status == "completed" ? PdigV2Colors.positive : PdigV2Colors.primarySoft)
                            .clipShape(Circle())
                        Text(stageTitle(stage.key)).font(VFont.secondary()).foregroundColor(PdigV2Colors.textPrimary)
                        Spacer()
                        VStatusBadge(status: stage.status)
                    }
                    .padding(VSpace.md)
                    .background(PdigV2Colors.surface)
                    .clipShape(RoundedRectangle(cornerRadius: VRadius.md, style: .continuous))
                }

                VSectionHeader(title: "即将到来")
                ForEach(VNextDemoFixture.upcoming) { item in
                    HStack {
                        Text(item.title).font(VFont.secondary()).foregroundColor(PdigV2Colors.textPrimary)
                        Spacer()
                        Text("\(item.days) 天后").font(VFont.meta()).foregroundColor(PdigV2Colors.warning)
                    }
                    .padding(VSpace.md)
                    .background(PdigV2Colors.surfaceRaised)
                    .clipShape(RoundedRectangle(cornerRadius: VRadius.md, style: .continuous))
                }
            }
            .padding(VSpace.pagePadding)
        }
        .vPageBackground()
        .accessibilityIdentifier(VTestIds.records)
    }
}

private struct VRecordMetric: View {
    let value: String
    let label: String
    var body: some View {
        VStack(alignment: .leading, spacing: 3) {
            Text(value).font(.system(size: 25, weight: .bold)).foregroundColor(PdigV2Colors.primaryText)
            Text(label).font(VFont.meta()).foregroundColor(PdigV2Colors.textMuted)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(VSpace.lg)
        .background(PdigV2Colors.primarySoft.opacity(0.72))
        .clipShape(RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous))
    }
}

struct SearchView: View {
    @ObservedObject var model: VNextModel

    private var results: [VSearchResult] {
        let q = model.searchQuery.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        guard !q.isEmpty else { return [] }
        var out: [VSearchResult] = []
        out += VNextDemoFixture.cards.filter { matches(q, [$0.nickname, $0.issuer, $0.last4, $0.region]) }
            .map { VSearchResult(id: $0.id, title: $0.nickname, subtitle: "\($0.issuer) · \(regionName($0.region)) · ••••\($0.last4)", kind: "卡片", screen: .cardDetail($0.id)) }
        out += VNextDemoFixture.numbers.filter { matches(q, [$0.nickname, $0.maskedNumber, $0.carrier, $0.region]) }
            .map { VSearchResult(id: $0.id, title: $0.nickname, subtitle: "\($0.carrier) · \($0.maskedNumber)", kind: "号码", screen: .numberDetail($0.id)) }
        out += VNextDemoFixture.accounts.filter { matches(q, [$0.name, $0.provider, $0.maskedIdentifier]) }
            .map { VSearchResult(id: $0.id, title: $0.name, subtitle: "\($0.provider) · \($0.maskedIdentifier)", kind: "账户", screen: .accounts) }
        out += VNextDemoFixture.emails.filter { matches(q, [$0.name, $0.provider, $0.maskedAddress]) }
            .map { VSearchResult(id: $0.id, title: $0.name, subtitle: "\($0.provider) · \($0.maskedAddress)", kind: "邮箱", screen: .emails) }
        out += VNextDemoFixture.devices.filter { matches(q, [$0.name, $0.platform, $0.kind]) }
            .map { VSearchResult(id: $0.id, title: $0.name, subtitle: "\($0.platform) · \($0.kind)", kind: "设备", screen: .devices) }
        out += VNextDemoFixture.services.filter { matches(q, [$0.name, $0.kind, $0.region]) }
            .map { VSearchResult(id: $0.id, title: $0.name, subtitle: "\(regionName($0.region)) · \(serviceKindName($0.kind))", kind: "服务", screen: .services) }
        return out
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: VSpace.lg) {
                Text("搜索与快捷操作").font(VFont.pageTitle()).foregroundColor(PdigV2Colors.textPrimary)
                Text("搜索结果只来自已记录的基础设施；未知内容不会被自动补全成事实。")
                    .font(VFont.secondary()).foregroundColor(PdigV2Colors.textSecondary)

                if model.searchQuery.isEmpty {
                    VSectionHeader(title: "快捷前往")
                    ForEach(quickEntries, id: \.0) { item in
                        VJumpRow(title: item.0, subtitle: item.1) { model.navigate(item.2) }
                    }
                } else {
                    VSectionHeader(title: "搜索结果")
                    if results.isEmpty {
                        VUnknownBoundaryNote("没有匹配的已记录对象。没有记录不代表不存在。")
                    } else {
                        ForEach(results) { result in
                            Button { model.navigate(result.screen) } label: {
                                HStack(spacing: VSpace.md) {
                                    VStack(alignment: .leading, spacing: 3) {
                                        Text(result.title).font(VFont.body()).fontWeight(.semibold).foregroundColor(PdigV2Colors.textPrimary)
                                        Text(result.subtitle).font(VFont.meta()).foregroundColor(PdigV2Colors.textSecondary)
                                    }
                                    Spacer()
                                    Text(result.kind).font(VFont.meta()).foregroundColor(PdigV2Colors.primaryText)
                                        .padding(.horizontal, VSpace.sm).frame(minHeight: 32)
                                        .background(PdigV2Colors.primarySoft).clipShape(RoundedRectangle(cornerRadius: VRadius.sm))
                                }
                                .padding(VSpace.lg)
                            }
                            .buttonStyle(.plain)
                            .background(PdigV2Colors.surface)
                            .clipShape(RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous))
                            .overlay(RoundedRectangle(cornerRadius: VRadius.lg).stroke(PdigV2Colors.borderSubtle, lineWidth: 1))
                        }
                    }
                }
            }
            .padding(VSpace.pagePadding)
        }
        .vPageBackground()
        .searchable(text: $model.searchQuery, placement: .automatic, prompt: "搜索卡片、号码、账户、邮箱、设备或服务")
        .accessibilityIdentifier(VTestIds.search)
        .toolbar {
            ToolbarItem(placement: .cancellationAction) { VBackButton { model.back() } }
        }
    }

    private var quickEntries: [(String, String, VScreen)] {
        [
            ("基础设施总览", "全球基础设施与地区活动", .overview),
            ("卡片", "支付基础设施与绑定关系", .cards),
            ("号码", "通信身份与恢复依赖", .numbers),
            ("账户", "账户规模与登录恢复关系", .accounts),
            ("邮箱", "邮箱身份与恢复角色", .emails),
            ("设备", "验证器、可信终端与恢复设备", .devices),
            ("服务", "订阅、支付与验证服务", .services),
        ]
    }

    private func matches(_ query: String, _ fields: [String]) -> Bool {
        fields.contains { $0.lowercased().contains(query) }
    }
}

private struct VSearchResult: Identifiable {
    let id: String
    let title: String
    let subtitle: String
    let kind: String
    let screen: VScreen
}

struct DataSourcesView: View {
    @ObservedObject var model: VNextModel

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: VSpace.lg) {
                Text("数据源").font(VFont.pageTitle()).foregroundColor(PdigV2Colors.textPrimary)
                Text("PDIG 只把已记录并确认的信息当作事实；没有来源的数据保持未知。")
                    .font(VFont.secondary()).foregroundColor(PdigV2Colors.textSecondary)

                HStack(spacing: VSpace.md) {
                    VBoundaryPillar(title: "本机优先", message: "数据留在当前工作区")
                    VBoundaryPillar(title: "已确认", message: "才进入依赖分析")
                    VBoundaryPillar(title: "未知", message: "绝不自动推断为安全")
                }
                .padding(VSpace.lg)
                .background(PdigV2Colors.primarySoft.opacity(0.76))
                .clipShape(RoundedRectangle(cornerRadius: VRadius.xl, style: .continuous))

                VSectionHeader(title: "当前工作区")
                VStack(alignment: .leading, spacing: VSpace.md) {
                    HStack {
                        VStack(alignment: .leading, spacing: 3) {
                            Text("本机数据").font(VFont.body()).fontWeight(.semibold).foregroundColor(PdigV2Colors.textPrimary)
                            Text("用于当前工作区的已记录基础设施").font(VFont.meta()).foregroundColor(PdigV2Colors.textMuted)
                        }
                        Spacer()
                        VChip(text: "本机优先", highlight: true)
                    }
                    HStack(spacing: VSpace.sm) {
                        VSourceMetric(value: "\(VNextDemoFixture.cards.count)", label: "卡片")
                        VSourceMetric(value: "\(VNextDemoFixture.numbers.count)", label: "号码")
                        VSourceMetric(value: "\(VNextDemoFixture.accounts.count)", label: "账户")
                    }
                    HStack(spacing: VSpace.sm) {
                        VSourceMetric(value: "\(VNextDemoFixture.emails.count)", label: "邮箱")
                        VSourceMetric(value: "\(VNextDemoFixture.devices.count)", label: "设备")
                        VSourceMetric(value: "\(VNextDemoFixture.services.count)", label: "服务")
                    }
                }
                .padding(VSpace.lg)
                .background(PdigV2Colors.surface)
                .clipShape(RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous))
                .overlay(RoundedRectangle(cornerRadius: VRadius.lg).stroke(PdigV2Colors.borderSubtle, lineWidth: 1))

                VSectionHeader(title: "事实边界")
                VUnknownBoundaryNote("已确认：已经记录并可用于依赖分析的信息。")
                VUnknownBoundaryNote("未知：没有记录的信息不会被推断为安全、存在或不存在。")
                VUnknownBoundaryNote("外观：卡面与号码面的个性化只改变显示，不改变事实。")

                VSectionHeader(title: "快速查看")
                VJumpRow(title: "基础设施总览", subtitle: "查看地区、卡片、号码与当前关注项") { model.navigate(.overview) }
                VJumpRow(title: "薄弱点", subtitle: "查看唯一恢复路径、到期与迁移阻塞") { model.navigate(.weaknesses) }
            }
            .padding(VSpace.pagePadding)
        }
        .vPageBackground()
        .accessibilityIdentifier(VTestIds.sources)
        .toolbar {
            ToolbarItem(placement: .cancellationAction) { VBackButton { model.back() } }
        }
    }
}

private struct VBoundaryPillar: View {
    let title: String
    let message: String
    var body: some View {
        VStack(alignment: .leading, spacing: 3) {
            Text(title).font(VFont.secondary()).fontWeight(.semibold).foregroundColor(PdigV2Colors.primaryText)
            Text(message).font(VFont.meta()).foregroundColor(PdigV2Colors.textSecondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

private struct VSourceMetric: View {
    let value: String
    let label: String
    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(value).font(.system(size: 20, weight: .bold)).foregroundColor(PdigV2Colors.textPrimary)
            Text(label).font(VFont.meta()).foregroundColor(PdigV2Colors.textMuted)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(VSpace.md)
        .background(PdigV2Colors.surfaceRaised)
        .clipShape(RoundedRectangle(cornerRadius: VRadius.md, style: .continuous))
    }
}

struct VJumpRow: View {
    let title: String
    let subtitle: String
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            HStack {
                VStack(alignment: .leading, spacing: 3) {
                    Text(title).font(VFont.secondary()).fontWeight(.semibold).foregroundColor(PdigV2Colors.textPrimary)
                    Text(subtitle).font(VFont.meta()).foregroundColor(PdigV2Colors.textMuted)
                }
                Spacer()
                Text("查看 →").font(VFont.meta()).fontWeight(.semibold).foregroundColor(PdigV2Colors.primaryText)
            }
            .padding(VSpace.lg).frame(minHeight: 52)
        }
        .buttonStyle(.plain)
        .background(PdigV2Colors.surface)
        .clipShape(RoundedRectangle(cornerRadius: VRadius.md, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: VRadius.md).stroke(PdigV2Colors.borderSubtle, lineWidth: 1))
    }
}

private func stageTitle(_ key: String) -> String {
    switch key {
    case "impact-analysis": return VCopy.impactAnalysis
    case "establish-new-number": return VCopy.establishNewNumber
    case "verify-new-number": return VCopy.verifyNewNumber
    case "migrate-key-accounts": return VCopy.migrateAccounts
    case "check-recovery-paths": return VCopy.checkRecoveryPaths
    case "retire-old-number": return VCopy.retireOldNumber
    default: return key
    }
}
