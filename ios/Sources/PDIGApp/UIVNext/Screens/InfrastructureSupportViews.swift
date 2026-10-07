// InfrastructureSupportViews —— complete iOS/iPadOS secondary infrastructure surfaces.
//
// These screens expose recorded synthetic fixture facts only. Missing relationships stay Unknown.

import SwiftUI

struct AccountsView: View {
    @ObservedObject var model: VNextModel
    var body: some View {
        VInfraPage(title: "账户", subtitle: "账户是服务之上的身份入口：重点关注验证方式、恢复路径和跨地区依赖。", model: model) {
            let items = VNextDemoFixture.accounts.filter { model.regionFilter == nil || $0.region == model.regionFilter }
            VSummaryHero(value: "\(items.count)", label: "已记录账户", hint: "身份入口、验证方式与恢复路径")
            if items.isEmpty {
                VInfraEmpty(model: model, title: "当前地区没有账户记录", body: "没有记录的账户与恢复关系仍保持未知。")
            } else {
                VSectionHeader(title: "已记录（\(items.count)）")
                ForEach(items) { item in
                    VLightObjectCard(title: item.name,
                                     subtitle: "\(item.provider) · \(item.maskedIdentifier) · \(regionName(item.region))",
                                     badge: String(item.provider.prefix(1)).uppercased(),
                                     accent: item.attention ? PdigV2Colors.warning : PdigV2Colors.primary,
                                     trailing: item.attention ? "恢复需关注" : "已记录") {
                        VTagLine(item.roles)
                        Text("验证 · \(item.authMethods.joined(separator: " / "))")
                        Text("恢复 · \(item.recoveryRoute)")
                            .foregroundColor(item.attention ? PdigV2Colors.warning : PdigV2Colors.textSecondary)
                    }
                }
                VUnknownBoundaryNote("未记录的账户、登录名、验证器或恢复方式仍保持未知。")
            }
        }
        .accessibilityIdentifier(VTestIds.infraAccountList)
    }
}

struct EmailsView: View {
    @ObservedObject var model: VNextModel
    var body: some View {
        VInfraPage(title: "邮箱", subtitle: "邮箱可能同时承担登录、通知和恢复职责；唯一恢复邮箱必须显式识别。", model: model) {
            let items = VNextDemoFixture.emails.filter { model.regionFilter == nil || $0.region == model.regionFilter }
            VSummaryHero(value: "\(items.count)", label: "已记录邮箱", hint: "登录、通知与恢复职责")
            if items.isEmpty {
                VInfraEmpty(model: model, title: "当前地区没有邮箱记录", body: "没有记录的邮箱与恢复关系仍保持未知。")
            } else {
                VSectionHeader(title: "已记录（\(items.count)）")
                ForEach(items) { item in
                    VLightObjectCard(title: item.name,
                                     subtitle: "\(item.maskedAddress) · \(item.provider) · \(regionName(item.region))",
                                     badge: "@",
                                     accent: item.recoveryOnly ? PdigV2Colors.critical : PdigV2Colors.primary,
                                     trailing: item.recoveryOnly ? "唯一恢复" : "\(item.linkedServiceCount) 项关联") {
                        VTagLine(item.roles)
                        Text(item.recoveryOnly ? "更换或停用前，必须先建立另一条恢复路径。" : "已记录 \(item.linkedServiceCount) 项服务关联。")
                            .foregroundColor(item.recoveryOnly ? PdigV2Colors.critical : PdigV2Colors.textSecondary)
                    }
                }
                VUnknownBoundaryNote("没有记录的邮箱与恢复关系不会被推断为不存在。")
            }
        }
        .accessibilityIdentifier(VTestIds.infraEmailList)
    }
}

struct DevicesView: View {
    @ObservedObject var model: VNextModel
    var body: some View {
        VInfraPage(title: "设备", subtitle: "可信设备、验证器和恢复设备构成连续性链路；长期未使用的设备需要人工复核。", model: model) {
            let items = VNextDemoFixture.devices.filter { model.regionFilter == nil || $0.region == model.regionFilter }
            VSummaryHero(value: "\(items.count)", label: "已记录设备", hint: "可信终端、验证器与恢复设备")
            if items.isEmpty {
                VInfraEmpty(model: model, title: "当前地区没有设备记录", body: "设备未出现于当前列表，不代表它没有登录或恢复权限。")
            } else {
                VSectionHeader(title: "已记录（\(items.count)）")
                ForEach(items) { item in
                    VLightObjectCard(title: item.name,
                                     subtitle: "\(item.platform) · \(item.kind) · \(regionName(item.region))",
                                     badge: String(item.platform.prefix(1)).uppercased(),
                                     accent: item.attention ? PdigV2Colors.warning : PdigV2Colors.primary,
                                     trailing: item.trust) {
                        VTagLine(item.roles)
                        Text("最近记录 · \(item.lastSeen)")
                            .foregroundColor(item.attention ? PdigV2Colors.warning : PdigV2Colors.textSecondary)
                    }
                }
                VUnknownBoundaryNote("设备未出现于当前列表，不代表它没有登录或恢复权限。")
            }
        }
        .accessibilityIdentifier(VTestIds.infraDeviceList)
    }
}

struct ServicesView: View {
    @ObservedObject var model: VNextModel
    var body: some View {
        VInfraPage(title: "服务", subtitle: "查看已经记录的服务，以及它们所在的地区和承担的角色。", model: model) {
            let items = VNextDemoFixture.services.filter { model.regionFilter == nil || $0.region == model.regionFilter }
            VSummaryHero(value: "\(items.count)", label: "已记录服务",
                         hint: model.regionFilter == nil ? "覆盖多个地区的服务与依赖入口" : "当前地区已记录的服务")
            if items.isEmpty {
                VInfraEmpty(model: model, title: "当前地区没有服务记录", body: "没有记录的服务不会被推断为不存在，也不会被标记为无风险。")
            } else {
                VSectionHeader(title: "已记录（\(items.count)）")
                ForEach(items) { item in
                    VLightObjectCard(title: item.name,
                                     subtitle: "\(regionName(item.region)) · 已记录服务",
                                     badge: serviceKindName(item.kind).prefix(1).description,
                                     trailing: serviceKindName(item.kind)) {
                        Text("这里只展示已记录关系；未记录的登录、恢复或支付依赖仍保持未知。")
                            .foregroundColor(PdigV2Colors.textMuted)
                    }
                }
                VUnknownBoundaryNote("尚未记录的服务不会被推断为不存在。")
            }
        }
        .accessibilityIdentifier(VTestIds.infraServiceList)
    }
}

struct WeaknessesView: View {
    @ObservedObject var model: VNextModel

    var body: some View {
        VInfraPage(title: "薄弱点", subtitle: "从已确认的恢复路径、到期状态和迁移计划中识别需要优先处理的风险。", model: model) {
            let numbers = VNextDemoFixture.numbers.filter { $0.recoveryOnly && (model.regionFilter == nil || $0.region == model.regionFilter) }
            let cards = VNextDemoFixture.cards.filter { $0.status == "expiring_soon" && (model.regionFilter == nil || $0.region == model.regionFilter) }
            let emails = VNextDemoFixture.emails.filter { $0.recoveryOnly && (model.regionFilter == nil || $0.region == model.regionFilter) }
            let devices = VNextDemoFixture.devices.filter { $0.attention && (model.regionFilter == nil || $0.region == model.regionFilter) }
            let phoneMigration = model.regionFilter == nil || model.regionFilter == "CN"
            let count = numbers.count + cards.count + emails.count + devices.count + (phoneMigration ? 1 : 0)

            VSummaryHero(value: "\(count)", label: "已知薄弱点", hint: "只统计已记录且可验证的风险事实；未知关系仍保持未知", warning: count > 0)

            if count == 0 {
                VInfraEmpty(model: model, title: "当前地区没有已记录的薄弱点",
                            body: "这只表示当前没有已记录的风险事实；未记录的依赖仍然保持未知。")
            } else {
                if !numbers.isEmpty {
                    VSectionHeader(title: "唯一恢复路径（\(numbers.count)）")
                    ForEach(numbers) { number in
                        VWeaknessRow(title: "\(number.maskedNumber) 是账户的唯一恢复路径",
                                     hint: "更换或注销前，必须先建立新的恢复方式.",
                                     accent: PdigV2Colors.critical) { model.openNumber(number.id) }
                    }
                }
                if !cards.isEmpty {
                    VSectionHeader(title: "即将到期（\(cards.count)）")
                    ForEach(cards) { card in
                        VWeaknessRow(title: "\(card.nickname) 将于 \(card.expiry) 到期",
                                     hint: "绑定服务可能中断，建议提前完成换卡与重新绑定。",
                                     accent: PdigV2Colors.critical) { model.openCard(card.id) }
                    }
                }
                if !emails.isEmpty {
                    VSectionHeader(title: "恢复邮箱（\(emails.count)）")
                    ForEach(emails) { item in
                        VWeaknessRow(title: "\(item.maskedAddress) 是唯一恢复邮箱",
                                     hint: "停用或更换前，先建立另一条恢复路径。",
                                     accent: PdigV2Colors.critical) { model.navigate(.emails) }
                    }
                }
                if !devices.isEmpty {
                    VSectionHeader(title: "设备复核（\(devices.count)）")
                    ForEach(devices) { item in
                        VWeaknessRow(title: "\(item.name) 需要确认是否仍应保持信任",
                                     hint: "最近记录：\(item.lastSeen)。未确认前不要把它视为可用恢复设备。",
                                     accent: PdigV2Colors.warning) { model.navigate(.devices) }
                    }
                }
                if phoneMigration {
                    VSectionHeader(title: "迁移阻塞")
                    VWeaknessRow(title: "旧号码暂时不能停用",
                                 hint: "新号码验证完成前，继续保留旧号码以避免恢复链路中断。",
                                 accent: PdigV2Colors.warning) { model.navigate(.changePhone) }
                }
                VSectionHeader(title: "未知关系")
                VUnknownBoundaryNote("没有记录的依赖仍然是未知，不会被标记为安全。")
            }
        }
        .accessibilityIdentifier(VTestIds.infraWeaknessList)
    }
}

struct VInfraPage<Content: View>: View {
    let title: String
    let subtitle: String
    @ObservedObject var model: VNextModel
    @ViewBuilder let content: () -> Content

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: VSpace.lg) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(title).font(VFont.pageTitle()).foregroundColor(PdigV2Colors.textPrimary)
                    Text(subtitle).font(VFont.secondary()).foregroundColor(PdigV2Colors.textSecondary)
                }
                if let code = model.regionFilter {
                    VRegionScopeBanner(code: code) { model.clearRegion() }
                }
                content()
            }
            .padding(VSpace.pagePadding)
        }
        .vPageBackground()
    }
}

struct VSummaryHero: View {
    let value: String
    let label: String
    let hint: String
    var warning = false

    var body: some View {
        let accent = warning ? PdigV2Colors.warning : PdigV2Colors.primaryText
        HStack(spacing: VSpace.lg) {
            Text(value).font(.system(size: 30, weight: .bold)).foregroundColor(accent)
            VStack(alignment: .leading, spacing: 2) {
                Text(label).font(VFont.secondary()).fontWeight(.semibold).foregroundColor(PdigV2Colors.textPrimary)
                Text(hint).font(VFont.meta()).foregroundColor(PdigV2Colors.textMuted)
            }
            Spacer()
        }
        .padding(VSpace.xl)
        .background((warning ? PdigV2Colors.warning : PdigV2Colors.primary).opacity(0.08))
        .clipShape(RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous)
            .stroke(accent.opacity(0.20), lineWidth: 1))
    }
}

struct VLightObjectCard<Content: View>: View {
    let title: String
    let subtitle: String
    let badge: String
    var accent: Color = PdigV2Colors.primary
    var trailing: String? = nil
    @ViewBuilder let content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: VSpace.md) {
            HStack(spacing: VSpace.md) {
                Text(badge)
                    .font(VFont.label()).foregroundColor(accent)
                    .frame(width: 36, height: 36)
                    .background(accent.opacity(0.10))
                    .clipShape(RoundedRectangle(cornerRadius: VRadius.md, style: .continuous))
                VStack(alignment: .leading, spacing: 2) {
                    Text(title).font(VFont.body()).fontWeight(.semibold).foregroundColor(PdigV2Colors.textPrimary)
                    Text(subtitle).font(VFont.meta()).foregroundColor(PdigV2Colors.textSecondary)
                }
                Spacer()
                if let trailing {
                    Text(trailing).font(VFont.meta()).fontWeight(.semibold)
                        .foregroundColor(accent).padding(.horizontal, VSpace.sm).frame(minHeight: 32)
                        .background(accent.opacity(0.10))
                        .clipShape(RoundedRectangle(cornerRadius: VRadius.sm, style: .continuous))
                }
            }
            VStack(alignment: .leading, spacing: 6) {
                content()
                    .font(VFont.meta())
                    .foregroundColor(PdigV2Colors.textSecondary)
            }
        }
        .padding(VSpace.lg)
        .background(PdigV2Colors.surface)
        .clipShape(RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous)
            .stroke(PdigV2Colors.borderSubtle, lineWidth: 1))
    }
}

struct VTagLine: View {
    let values: [String]
    init(_ values: [String]) { self.values = values }

    var body: some View {
        HStack(spacing: 6) {
            ForEach(Array(values.prefix(3)), id: \.self) { VChip(text: $0) }
        }
    }
}

struct VUnknownBoundaryNote: View {
    let text: String
    init(_ text: String) { self.text = text }
    var body: some View {
        Text(text).font(VFont.meta()).foregroundColor(PdigV2Colors.textMuted)
            .padding(VSpace.lg).frame(maxWidth: .infinity, alignment: .leading)
            .background(PdigV2Colors.surfaceRaised)
            .clipShape(RoundedRectangle(cornerRadius: VRadius.md, style: .continuous))
    }
}

struct VRegionScopeBanner: View {
    let code: String
    let clear: () -> Void
    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text(regionName(code)).font(VFont.secondary()).fontWeight(.semibold).foregroundColor(PdigV2Colors.textPrimary)
                Text("当前地区范围").font(VFont.meta()).foregroundColor(PdigV2Colors.textMuted)
            }
            Spacer()
            Button("查看全球", action: clear)
                .font(VFont.meta()).fontWeight(.semibold)
                .foregroundColor(PdigV2Colors.primaryText)
                .frame(minHeight: 44).padding(.horizontal, VSpace.md)
                .background(PdigV2Colors.primarySoft)
                .clipShape(RoundedRectangle(cornerRadius: VRadius.md, style: .continuous))
        }
        .padding(VSpace.md)
        .background(PdigV2Colors.surface)
        .clipShape(RoundedRectangle(cornerRadius: VRadius.md, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: VRadius.md).stroke(PdigV2Colors.borderSubtle, lineWidth: 1))
    }
}

struct VInfraEmpty: View {
    @ObservedObject var model: VNextModel
    let title: String
    let body: String
    var bodyView: some View {
        EmptyView()
    }
    var body: some View {
        VStack(spacing: VSpace.md) {
            Image(systemName: "square.dashed").font(.system(size: 32)).foregroundColor(PdigV2Colors.primary)
            Text(title).font(VFont.body()).fontWeight(.semibold).foregroundColor(PdigV2Colors.textPrimary)
            Text(body).font(VFont.meta()).foregroundColor(PdigV2Colors.textSecondary).multilineTextAlignment(.center)
            Button(model.regionFilter == nil ? "返回基础设施总览" : "查看全球") {
                if model.regionFilter == nil { model.navigate(.overview) } else { model.clearRegion() }
            }
            .buttonStyle(.borderedProminent)
        }
        .padding(VSpace.xxl).frame(maxWidth: .infinity)
        .background(PdigV2Colors.surface)
        .clipShape(RoundedRectangle(cornerRadius: VRadius.xl, style: .continuous))
    }
}

struct VWeaknessRow: View {
    let title: String
    let hint: String
    let accent: Color
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            HStack(alignment: .top, spacing: VSpace.md) {
                Text("!").font(VFont.label()).foregroundColor(accent)
                    .frame(width: 34, height: 34).background(accent.opacity(0.10)).clipShape(Circle())
                VStack(alignment: .leading, spacing: 4) {
                    Text(title).font(VFont.secondary()).fontWeight(.semibold).foregroundColor(PdigV2Colors.textPrimary)
                    Text(hint).font(VFont.meta()).foregroundColor(PdigV2Colors.textSecondary)
                    Text("查看详情与下一步 →").font(VFont.meta()).fontWeight(.semibold).foregroundColor(accent)
                }
                Spacer()
            }
            .padding(VSpace.lg).frame(maxWidth: .infinity, alignment: .leading)
        }
        .buttonStyle(.plain)
        .background(PdigV2Colors.surface)
        .clipShape(RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: VRadius.lg).stroke(accent.opacity(0.22), lineWidth: 1))
    }
}

func regionName(_ code: String) -> String {
    VNextDemoFixture.regions.first(where: { $0.regionCode == code })?.displayName ?? code
}

func serviceKindName(_ kind: String) -> String {
    switch kind {
    case "payment": return "支付"
    case "banking": return "银行"
    case "subscription": return "订阅"
    default: return "服务"
    }
}
