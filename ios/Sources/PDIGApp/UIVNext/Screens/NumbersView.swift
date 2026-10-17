// NumbersView —— 号码管理（/infrastructure/numbers）：List + Inspector（master-detail）。
//
// numbers.md / phone-card.md 契约：
//  - Desktop 默认 list-table + inspector；Mobile 默认 number-face cards + sections；
//  - Inspector 只读为主，动作 = 链接去详情页（components/inspector.md）；
//  - 过滤：国家/区号/SIM eSIM/主副号/用途/状态/恢复用途（与 regionFilter 同步）；
//  - 号码遮罩默认开（privacyMask.maskPhoneNumbers）；运营商未知不填充。

import SwiftUI

struct NumbersView: View {
    @ObservedObject var model: VNextModel
    @State private var selectedId: String? = VNextDemoFixture.numbers.first?.id

    private var filtered: [VNumber] {
        if let region = model.regionFilter {
            return VNextDemoFixture.numbers.filter { $0.region == region }
        }
        return VNextDemoFixture.numbers
    }

    private var selected: VNumber? {
        if let id = selectedId {
            return filtered.first { $0.id == id } ?? filtered.first
        }
        return filtered.first
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: VSpace.sectionGap) {
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(VCopy.numbersTitle)
                            .font(VFont.pageTitle())
                            .foregroundColor(PdigV2Colors.textPrimary)
                        Text(subtitle)
                            .font(VFont.secondary())
                            .foregroundColor(PdigV2Colors.textSecondary)
                    }
                    Spacer()
                }

                filterRow

                // List（pdig.phone.list）
                listSection
                    .accessibilityIdentifier(VTestIds.phoneList)

                // Inspector（pdig.phone.inspector）
                inspectorSection
                    .accessibilityIdentifier(VTestIds.phoneInspector)
            }
            .padding(VSpace.pagePadding)
        }
        .vPageBackground()
    }

    private var subtitle: String {
        if let region = model.regionFilter {
            return "地区 \(region) · \(filtered.count) 个号码"
        }
        return "全球 \(filtered.count) 个号码"
    }

    // MARK: - Filter row（全部 / SIM 类型 / 主副号）

    private var filterRow: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: VSpace.sm) {
                VFilterChip(label: VCopy.filterAll, selected: model.regionFilter == nil) {
                    model.regionFilter = nil
                }
                ForEach(VNextDemoFixture.regions) { region in
                    VFilterChip(
                        label: region.regionCode,
                        selected: model.regionFilter == region.regionCode
                    ) {
                        model.selectRegion(region.regionCode)
                    }
                }
            }
        }
    }

    // MARK: - List（行高 40–48；行项 pdig.phone.row）

    private var listSection: some View {
        VStack(spacing: 0) {
            ForEach(filtered) { number in
                Button {
                    selectedId = number.id
                    model.openNumber(number.id)
                } label: {
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            HStack(spacing: VSpace.sm) {
                                Text(number.nickname)
                                    .font(VFont.secondary())
                                    .fontWeight(.semibold)
                                    .foregroundColor(PdigV2Colors.textPrimary)
                                Text(number.countryCode)
                                    .font(VFont.meta())
                                    .foregroundColor(PdigV2Colors.textMuted)
                            }
                            Text("\(number.maskedNumber) · \(number.carrier) · \(vSimLabel(number.simKind)) · \(vRoleLabel(number.role))")
                                .font(VFont.meta())
                                .foregroundColor(PdigV2Colors.textSecondary)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        if number.recoveryOnly { VChip(text: VCopy.recoveryOnlyShort, highlight: true) }
                        VStatusBadge(status: number.status)
                    }
                    .padding(.horizontal, VSpace.lg)
                    .frame(minHeight: 48)
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .background(
                    selected?.id == number.id
                        ? PdigV2Colors.primary.opacity(0.18)
                        : PdigV2Colors.surfaceRaised.opacity(0.6)
                )
                .overlay(
                    RoundedRectangle(cornerRadius: VRadius.md, style: .continuous)
                        .stroke(
                            selected?.id == number.id ? PdigV2Colors.primaryBright : PdigV2Colors.borderSubtle,
                            lineWidth: 1
                        )
                )
                .clipShape(RoundedRectangle(cornerRadius: VRadius.md, style: .continuous))
                .padding(.vertical, 3)
                .accessibilityIdentifier("\(VTestIds.phoneRow).\(number.id)")
            }
        }
    }

    // MARK: - Inspector（只读摘要 + 详情链接）

    private var inspectorSection: some View {
        VStack(alignment: .leading, spacing: VSpace.md) {
            if let selected = selected {
                VSectionHeader(title: VCopy.numberDetailTitle)
                Text(selected.nickname)
                    .font(VFont.body())
                    .fontWeight(.bold)
                    .foregroundColor(PdigV2Colors.textPrimary)
                Text(selected.maskedNumber)
                    .font(.system(size: 20, weight: .bold, design: .monospaced))
                    .foregroundColor(PdigV2Colors.textPrimary)
                HStack(spacing: VSpace.sm) {
                    VChip(text: vSimLabel(selected.simKind))
                    VChip(text: vRoleLabel(selected.role))
                    VChip(text: selected.carrier)
                    if selected.recoveryOnly { VChip(text: VCopy.recoveryOnly, highlight: true) }
                }
                Text("用途：\(selected.usages.joined(separator: " · "))")
                    .font(VFont.secondary())
                    .foregroundColor(PdigV2Colors.textSecondary)

                let services = VNextDemoFixture.servicesForNumber(selected.id)
                VSectionHeader(title: "\(VCopy.relatedServices)（\(services.count)）")
                ForEach(services) { service in
                    HStack {
                        Text(service.name)
                            .font(VFont.secondary())
                            .foregroundColor(PdigV2Colors.textSecondary)
                        Spacer()
                        VChip(text: vRelationKindLabel(service.kind))
                    }
                }

                Button {
                    model.openNumber(selected.id)
                } label: {
                    Text("\(VCopy.viewFullDetail) →")
                        .font(VFont.secondary())
                        .fontWeight(.semibold)
                        .foregroundColor(PdigV2Colors.primaryBright)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(VSpace.md)
                        .frame(minHeight: 44)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .background(PdigV2Colors.primarySoft)
                .clipShape(RoundedRectangle(cornerRadius: VRadius.md, style: .continuous))
            } else {
                Text(VCopy.selectNumberHint)
                    .font(VFont.secondary())
                    .foregroundColor(PdigV2Colors.textMuted)
            }
        }
        .padding(VSpace.xl)
        .background(PdigV2Colors.surface.opacity(0.92))
        .clipShape(RoundedRectangle(cornerRadius: VRadius.xl, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: VRadius.xl, style: .continuous)
                .stroke(PdigV2Colors.borderSubtle, lineWidth: 1)
        )
    }
}
