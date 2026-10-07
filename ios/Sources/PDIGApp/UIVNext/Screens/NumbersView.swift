// NumbersView —— communication identity collection.
// iPhone = focused identity list; iPad = persistent list/detail workspace.

import SwiftUI

struct NumbersView: View {
    @ObservedObject var model: VNextModel
    @Environment(\.horizontalSizeClass) private var sizeClass
    @State private var selectedId: String? = VNextDemoFixture.numbers.first?.id

    private var filtered: [VNumber] {
        VNextDemoFixture.numbers.filter { model.regionFilter == nil || $0.region == model.regionFilter }
    }

    private var selected: VNumber? {
        if let selectedId, let match = filtered.first(where: { $0.id == selectedId }) { return match }
        return filtered.first
    }

    var body: some View {
        Group {
            if sizeClass == .regular { expanded } else { compact }
        }
        .vPageBackground()
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: VSpace.md) {
            Text(VCopy.numbersTitle).font(VFont.pageTitle()).foregroundColor(PdigV2Colors.textPrimary)
            Text(model.regionFilter == nil ? "全球 \(filtered.count) 个号码" : "\(regionName(model.regionFilter!)) · \(filtered.count) 个号码")
                .font(VFont.secondary()).foregroundColor(PdigV2Colors.textSecondary)
            filterRow
        }
    }

    private var compact: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: VSpace.lg) {
                header
                ForEach(filtered) { number in
                    Button { model.openNumber(number.id) } label: {
                        HStack(spacing: VSpace.md) {
                            VNumberMiniFace(number: number).frame(width: 104)
                            VStack(alignment: .leading, spacing: 4) {
                                HStack {
                                    Text(number.nickname).font(VFont.body()).fontWeight(.semibold).foregroundColor(PdigV2Colors.textPrimary)
                                    Spacer()
                                    if number.recoveryOnly { VChip(text: VCopy.recoveryOnlyShort, highlight: true) }
                                }
                                Text("\(number.maskedNumber) · \(number.carrier)")
                                    .font(VFont.meta()).foregroundColor(PdigV2Colors.textSecondary)
                                Text("\(vSimLabel(number.simKind)) · \(vRoleLabel(number.role)) · \(number.usages.joined(separator: " / "))")
                                    .font(VFont.meta()).foregroundColor(PdigV2Colors.textMuted)
                            }
                        }
                        .padding(VSpace.md).frame(maxWidth: .infinity, alignment: .leading)
                    }
                    .buttonStyle(.plain)
                    .background(PdigV2Colors.surface)
                    .clipShape(RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous))
                    .overlay(RoundedRectangle(cornerRadius: VRadius.lg).stroke(PdigV2Colors.borderSubtle, lineWidth: 1))
                    .accessibilityIdentifier("\(VTestIds.phoneRow).\(number.id)")
                }
            }
            .padding(VSpace.pagePadding)
        }
    }

    private var expanded: some View {
        GeometryReader { geo in
            HStack(alignment: .top, spacing: VSpace.lg) {
                ScrollView {
                    VStack(alignment: .leading, spacing: VSpace.lg) {
                        header
                        VStack(spacing: VSpace.sm) {
                            ForEach(filtered) { number in
                                Button {
                                    selectedId = number.id
                                } label: {
                                    HStack(spacing: VSpace.md) {
                                        VNumberMiniFace(number: number).frame(width: 106)
                                        VStack(alignment: .leading, spacing: 3) {
                                            Text(number.nickname).font(VFont.secondary()).fontWeight(.semibold).foregroundColor(PdigV2Colors.textPrimary)
                                            Text(number.maskedNumber).font(VFont.meta()).foregroundColor(PdigV2Colors.textSecondary)
                                            Text("\(vSimLabel(number.simKind)) · \(vRoleLabel(number.role))")
                                                .font(VFont.meta()).foregroundColor(PdigV2Colors.textMuted)
                                        }
                                        Spacer()
                                        if number.recoveryOnly { VChip(text: "唯一恢复", highlight: true) }
                                    }
                                    .padding(VSpace.md)
                                }
                                .buttonStyle(.plain)
                                .background(selected?.id == number.id ? PdigV2Colors.primarySoft : PdigV2Colors.surface)
                                .clipShape(RoundedRectangle(cornerRadius: VRadius.md, style: .continuous))
                                .overlay(RoundedRectangle(cornerRadius: VRadius.md)
                                    .stroke(selected?.id == number.id ? PdigV2Colors.primary : PdigV2Colors.borderSubtle, lineWidth: 1))
                                .accessibilityIdentifier("\(VTestIds.phoneRow).\(number.id)")
                            }
                        }
                    }
                    .padding(.leading, VSpace.pagePadding)
                    .padding(.vertical, VSpace.pagePadding)
                }
                .frame(maxWidth: .infinity)

                if let selected {
                    VNumberInspector(number: selected, model: model)
                        .frame(width: min(380, geo.size.width * 0.36))
                        .padding(.trailing, VSpace.pagePadding)
                        .padding(.vertical, VSpace.pagePadding)
                }
            }
        }
    }

    private var filterRow: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: VSpace.sm) {
                VFilterChip(label: VCopy.filterAll, selected: model.regionFilter == nil) { model.clearRegion() }
                VFilterChip(label: "eSIM", selected: false) {}
                VFilterChip(label: "实体 SIM", selected: false) {}
                VFilterChip(label: "主号", selected: false) {}
                VFilterChip(label: "副号", selected: false) {}
                VFilterChip(label: "保号", selected: false) {}
            }
        }
    }
}

private struct VNumberMiniFace: View {
    let number: VNumber
    var body: some View {
        ZStack(alignment: .bottomLeading) {
            LinearGradient(colors: [Color(hex: "#0B2E58"), Color(hex: "#071A34")],
                           startPoint: .topLeading, endPoint: .bottomTrailing)
            VStack(alignment: .leading, spacing: 3) {
                Text(number.countryCode).font(.system(size: 9, weight: .bold)).foregroundColor(PdigV2Colors.assetTextPrimary)
                Text("•••• ••••").font(.system(size: 8, design: .monospaced)).foregroundColor(PdigV2Colors.assetTextSecondary)
            }.padding(8)
        }
        .aspectRatio(1.65, contentMode: .fit)
        .clipShape(RoundedRectangle(cornerRadius: 9, style: .continuous))
    }
}

private struct VNumberInspector: View {
    let number: VNumber
    @ObservedObject var model: VNextModel
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: VSpace.md) {
                VSectionHeader(title: "号码详情")
                VNumberFace(number: number, privacyMask: model.privacyMask, onClick: {})
                HStack {
                    VChip(text: vSimLabel(number.simKind))
                    VChip(text: vRoleLabel(number.role))
                    if number.recoveryOnly { VChip(text: "唯一恢复路径", highlight: true) }
                }
                Text("用途 · \(number.usages.joined(separator: " / "))")
                    .font(VFont.secondary()).foregroundColor(PdigV2Colors.textSecondary)

                VSectionHeader(title: "关联服务（\(VNextDemoFixture.servicesForNumber(number.id).count)）")
                ForEach(VNextDemoFixture.servicesForNumber(number.id)) { service in
                    HStack {
                        Text(service.name).font(VFont.secondary()).foregroundColor(PdigV2Colors.textPrimary)
                        Spacer()
                        VChip(text: vRelationKindLabel(VNextDemoFixture.relationKind(number.id, service.id) ?? "unknown"))
                    }
                }

                if number.recoveryOnly {
                    Text("此号码承担唯一恢复路径；更换前必须先建立并验证新的恢复方式。")
                        .font(VFont.meta()).foregroundColor(PdigV2Colors.warning)
                        .padding(VSpace.md).frame(maxWidth: .infinity, alignment: .leading)
                        .background(PdigV2Colors.warning.opacity(0.10))
                        .clipShape(RoundedRectangle(cornerRadius: VRadius.md))
                }

                Button("定制号码面") { model.openNumberCustomization(number.id) }
                    .buttonStyle(.bordered).tint(PdigV2Colors.primary)
                Button("查看完整详情") { model.openNumber(number.id) }
                    .buttonStyle(.borderedProminent).tint(PdigV2Colors.primary)
            }
            .padding(VSpace.lg)
        }
        .background(PdigV2Colors.surface)
        .clipShape(RoundedRectangle(cornerRadius: VRadius.xl, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: VRadius.xl).stroke(PdigV2Colors.borderSubtle, lineWidth: 1))
        .accessibilityIdentifier(VTestIds.phoneInspector)
    }
}
