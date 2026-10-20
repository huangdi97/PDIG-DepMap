// ReferenceSearchView —— iOS vNext global reference search.
//
// Search indexes only routes and synthetic objects that already exist in the
// iOS Reference Preview. It never advertises Review / Import / Manual mutation
// flows that have not been translated, and never claims Production authority.

import SwiftUI

enum VReferenceSearchDestination: Hashable {
    case screen(VScreen)
    case card(String)
    case number(String)
    case secondary(VSecondaryKind, String)
    case region(String)
}

struct VReferenceSearchResult: Identifiable, Hashable {
    let id: String
    let kind: String
    let title: String
    let subtitle: String
    let destination: VReferenceSearchDestination
}

private struct VReferenceSearchCommand {
    let id: String
    let title: String
    let hint: String
    let aliases: [String]
    let screen: VScreen
}

private let vReferenceSearchCommands: [VReferenceSearchCommand] = [
    .init(id: "overview", title: "基础设施总览", hint: "全球基础设施与地区上下文",
          aliases: ["总览", "基础设施", "地球", "地区"], screen: .overview),
    .init(id: "cards", title: "卡片", hint: "支付基础设施与绑定关系",
          aliases: ["银行卡", "支付"], screen: .cards),
    .init(id: "numbers", title: "号码", hint: "通信身份与恢复依赖",
          aliases: ["手机号", "电话", "sim", "esim"], screen: .numbers),
    .init(id: "accounts", title: "账户", hint: "账户身份与已记录关系",
          aliases: ["账号"], screen: .accounts),
    .init(id: "emails", title: "邮箱", hint: "邮箱身份与恢复角色",
          aliases: ["邮件", "email"], screen: .emails),
    .init(id: "devices", title: "设备", hint: "设备端点与已记录关系",
          aliases: ["手机", "电脑"], screen: .devices),
    .init(id: "services", title: "服务", hint: "数字服务与依赖入口",
          aliases: ["订阅"], screen: .services),
    .init(id: "weaknesses", title: "薄弱点", hint: "连续性发现与未知边界",
          aliases: ["恢复", "单点", "共享故障", "finding", "spof"], screen: .weaknesses),
    .init(id: "change-phone", title: "更换手机号", hint: "查看 Current / Transition / After reference",
          aliases: ["变更", "迁移", "换号"], screen: .changePhone),
    .init(id: "records", title: "记录", hint: "已发生、已完成与独立验证",
          aliases: ["历史", "时间线", "验证", "证据"], screen: .records),
    .init(id: "sources", title: "数据源", hint: "Reference 覆盖范围与事实边界",
          aliases: ["来源", "数据"], screen: .sources),
    .init(id: "settings", title: "设置", hint: "隐私、动效与本机呈现偏好",
          aliases: ["隐私", "偏好"], screen: .settings),
    .init(id: "personalization", title: "个性化", hint: "工作区与资产外观偏好",
          aliases: ["外观", "主题"], screen: .personalization),
]

func vReferenceSearchResults(
    query: String,
    privacyMask: Bool
) -> [VReferenceSearchResult] {
    let trimmed = query.trimmingCharacters(in: .whitespacesAndNewlines)
    guard !trimmed.isEmpty else {
        return vReferenceSearchCommands.map {
            VReferenceSearchResult(
                id: "nav:\($0.id)",
                kind: "页面",
                title: $0.title,
                subtitle: $0.hint,
                destination: .screen($0.screen)
            )
        }
    }

    func hit(_ values: [String]) -> Bool {
        values.contains { $0.localizedCaseInsensitiveContains(trimmed) }
    }

    func regionName(_ code: String) -> String {
        VNextDemoFixture.regionSummaries().first { $0.regionCode == code }?.displayName ?? code
    }

    var results: [VReferenceSearchResult] = []

    for command in vReferenceSearchCommands where
        hit([command.title, command.hint] + command.aliases) {
        results.append(
            VReferenceSearchResult(
                id: "nav:\(command.id)",
                kind: "页面",
                title: command.title,
                subtitle: command.hint,
                destination: .screen(command.screen)
            )
        )
    }

    for card in VNextDemoFixture.cards {
        let region = regionName(card.region)
        if hit([
            card.nickname, card.issuer, card.last4, card.masked, card.region,
            region, card.currency, card.type, card.form, card.network
        ] + card.usages) {
            let title = vVisibleCardNickname(card, privacyMask: privacyMask)
            let subtitle = privacyMask
                ? "发行方已遮蔽 · \(region) · ••••"
                : "\(card.issuer) · \(region) · \(card.masked)"
            results.append(
                VReferenceSearchResult(
                    id: "card:\(card.id)",
                    kind: "卡片",
                    title: title,
                    subtitle: subtitle,
                    destination: .card(card.id)
                )
            )
        }
    }

    for number in VNextDemoFixture.numbers {
        let region = regionName(number.region)
        let role = switch number.role {
        case "primary": "主号"
        case "secondary": "副号"
        case "keep": "保号"
        default: number.role
        }
        if hit([
            number.nickname, number.maskedNumber, number.region, region,
            number.countryCode, number.carrier, number.simKind, role
        ] + number.usages) {
            let title = vVisibleNumberNickname(number, privacyMask: privacyMask)
            let subtitle = privacyMask
                ? "号码已遮蔽 · \(region) · 运营商信息已遮蔽"
                : "\(number.maskedNumber) · \(region) · \(number.carrier)"
            results.append(
                VReferenceSearchResult(
                    id: "number:\(number.id)",
                    kind: "号码",
                    title: title,
                    subtitle: subtitle,
                    destination: .number(number.id)
                )
            )
        }
    }

    for asset in VNextDemoFixture.secondaryAssets {
        let region = regionName(asset.region)
        if hit([asset.name, asset.subtitle, asset.region, region] + asset.roles) {
            results.append(
                VReferenceSearchResult(
                    id: "secondary:\(asset.kind.rawValue):\(asset.id)",
                    kind: asset.kind.title,
                    title: privacyMask ? "\(asset.kind.title)（已遮蔽）" : asset.name,
                    subtitle: privacyMask ? "标识已遮蔽 · \(region)" : "\(asset.subtitle) · \(region)",
                    destination: .secondary(asset.kind, asset.id)
                )
            )
        }
    }

    for region in VNextDemoFixture.regionSummaries()
    where hit([region.displayName, region.regionCode]) {
        results.append(
            VReferenceSearchResult(
                id: "region:\(region.regionCode)",
                kind: "地区",
                title: region.displayName,
                subtitle: "\(region.cardCount) 张卡 · \(region.phoneCount) 个号码",
                destination: .region(region.regionCode)
            )
        )
    }

    var seen = Set<String>()
    return results.filter { seen.insert($0.id).inserted }
}

@MainActor
func vReferenceOpenSearchResult(
    _ result: VReferenceSearchResult,
    model: VNextModel
) {
    switch result.destination {
    case .screen(let screen):
        model.navigate(screen)
    case .card(let id):
        model.openCard(id)
    case .number(let id):
        model.openNumber(id)
    case .secondary(let kind, let id):
        model.openSecondary(kind, id)
    case .region(let code):
        model.selectRegion(code)
        model.navigate(.overview)
    }
    model.searchShown = false
}

struct VReferenceSearchView: View {
    @ObservedObject var model: VNextModel
    @State private var query = ""

    private var results: [VReferenceSearchResult] {
        vReferenceSearchResults(query: query, privacyMask: model.privacyMask)
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: VSpace.sectionGap) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("搜索与快捷操作")
                        .font(VFont.pageTitle())
                        .foregroundColor(PdigV2Colors.textPrimary)
                    Text("只搜索当前 Reference Preview 已存在的页面和对象；未记录 ≠ 不存在，也不等于安全。")
                        .font(VFont.secondary())
                        .foregroundColor(PdigV2Colors.textSecondary)
                }

                HStack(spacing: VSpace.sm) {
                    Image(systemName: "magnifyingglass")
                        .foregroundColor(PdigV2Colors.textMuted)
                    TextField("搜索卡片、号码、地区、服务或页面", text: $query)
                        .textInputAutocapitalization(.never)
                        .disableAutocorrection(true)
                        .foregroundColor(PdigV2Colors.textPrimary)
                        .accessibilityIdentifier("pdig.search.field")
                    if !query.isEmpty {
                        Button {
                            query = ""
                        } label: {
                            Image(systemName: "xmark.circle.fill")
                                .foregroundColor(PdigV2Colors.textMuted)
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel("清空搜索")
                    }
                }
                .padding(VSpace.lg)
                .background(PdigV2Colors.surface)
                .clipShape(RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous)
                        .stroke(PdigV2Colors.borderSubtle, lineWidth: 1)
                )

                VSectionHeader(title: query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                    ? "快捷前往" : "搜索结果")

                if results.isEmpty {
                    Text("没有匹配「\(query.trimmingCharacters(in: .whitespacesAndNewlines))」。可以换个关键词继续搜索；没有结果不代表没有关联。")
                        .font(VFont.secondary())
                        .foregroundColor(PdigV2Colors.textSecondary)
                        .padding(VSpace.lg)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(PdigV2Colors.surfaceRaised)
                        .clipShape(RoundedRectangle(cornerRadius: VRadius.md, style: .continuous))
                } else {
                    LazyVStack(spacing: VSpace.sm) {
                        ForEach(results) { result in
                            Button {
                                vReferenceOpenSearchResult(result, model: model)
                            } label: {
                                HStack(spacing: VSpace.md) {
                                    VStack(alignment: .leading, spacing: 3) {
                                        Text(result.title)
                                            .font(VFont.body())
                                            .fontWeight(.semibold)
                                            .foregroundColor(PdigV2Colors.textPrimary)
                                            .lineLimit(1)
                                        Text(result.subtitle)
                                            .font(VFont.meta())
                                            .foregroundColor(PdigV2Colors.textMuted)
                                            .lineLimit(2)
                                    }
                                    Spacer()
                                    Text(result.kind)
                                        .font(VFont.meta())
                                        .fontWeight(.semibold)
                                        .foregroundColor(PdigV2Colors.primaryBright)
                                        .padding(.horizontal, VSpace.sm)
                                        .padding(.vertical, 5)
                                        .background(PdigV2Colors.primarySoft)
                                        .clipShape(Capsule())
                                }
                                .padding(VSpace.lg)
                                .frame(minHeight: 52)
                                .contentShape(Rectangle())
                            }
                            .buttonStyle(.plain)
                            .background(PdigV2Colors.surfaceRaised)
                            .clipShape(RoundedRectangle(cornerRadius: VRadius.md, style: .continuous))
                            .accessibilityIdentifier("pdig.search.result.\(result.id)")
                        }
                    }
                }

                if model.privacyMask {
                    Text("隐私遮蔽已开启：搜索仍可匹配已记录字段，但结果不会回显被遮蔽的身份信息。")
                        .font(VFont.meta())
                        .foregroundColor(PdigV2Colors.unknown)
                }
            }
            .padding(VSpace.pagePadding)
        }
        .vPageBackground()
        .accessibilityIdentifier("pdig.ios.reference.search")
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("完成") { model.searchShown = false }
                    .accessibilityIdentifier("pdig.search.close")
            }
        }
    }
}

private struct VGlobalSearchToolbarModifier: ViewModifier {
    @ObservedObject var model: VNextModel

    func body(content: Content) -> some View {
        content.toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button {
                    model.searchShown = true
                } label: {
                    Image(systemName: "magnifyingglass")
                }
                .accessibilityLabel("搜索")
                .accessibilityIdentifier("pdig.search.open")
            }
        }
    }
}

extension View {
    func vGlobalSearchToolbar(model: VNextModel) -> some View {
        modifier(VGlobalSearchToolbarModifier(model: model))
    }
}
