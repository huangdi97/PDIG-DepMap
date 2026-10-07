// CustomizationView —— presentation-only Studio.
// iPhone: preview first, editor below. iPad: library / live preview / inspector three-column workspace.

import SwiftUI

struct CardCustomizationView: View {
    @ObservedObject var model: VNextModel
    private var card: VCard? {
        if case .cardCustomization(let id) = model.screen { return VNextDemoFixture.cardById(id) }
        return nil
    }

    var body: some View {
        if let card {
            VCustomizationFrame(
                title: "卡面定制 · \(card.nickname)",
                presets: CARD_THEME_PRESETS,
                initial: .defaultFor(targetType: "card", targetId: card.id, preset: card.preset),
                preview: { profile in
                    AnyView(VAssetCard(
                        card: VCard(id: card.id, nickname: card.nickname, issuer: card.issuer, last4: card.last4,
                                    masked: card.masked, region: card.region, currency: card.currency, type: card.type,
                                    form: card.form, network: card.network, expiry: card.expiry, status: card.status,
                                    attention: card.attention, usages: card.usages, preset: profile.themeId),
                        privacyMask: model.privacyMask, onClick: {}
                    ))
                },
                onBack: { model.back() }
            )
        }
    }
}

struct NumberCustomizationView: View {
    @ObservedObject var model: VNextModel
    private var number: VNumber? {
        if case .numberCustomization(let id) = model.screen { return VNextDemoFixture.numberById(id) }
        return nil
    }

    var body: some View {
        if let number {
            VCustomizationFrame(
                title: "号码面定制 · \(number.nickname)",
                presets: NUMBER_THEME_PRESETS,
                initial: .defaultFor(targetType: "phoneNumber", targetId: number.id, preset: "country"),
                preview: { _ in AnyView(VNumberFace(number: number, privacyMask: model.privacyMask, onClick: {})) },
                onBack: { model.back() }
            )
        }
    }
}

private struct VCustomizationFrame: View {
    let title: String
    let presets: [String]
    let initial: VPresentationProfile
    let preview: (VPresentationProfile) -> AnyView
    let onBack: () -> Void

    @Environment(\.horizontalSizeClass) private var sizeClass
    @State private var profile: VPresentationProfile?
    @State private var saved = false

    private var current: VPresentationProfile { profile ?? initial }

    var body: some View {
        Group {
            if sizeClass == .regular {
                expanded
            } else {
                compact
            }
        }
        .vPageBackground()
        .toolbar {
            ToolbarItem(placement: .cancellationAction) { VBackButton(action: onBack) }
            ToolbarItem(placement: .primaryAction) { saveButton }
        }
        #if os(iOS)
        .navigationBarBackButtonHidden(true)
        #endif
    }

    private var compact: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: VSpace.lg) {
                Text(title).font(VFont.pageTitle()).foregroundColor(PdigV2Colors.textPrimary)
                previewStage
                presetLibrary(horizontal: true)
                inspector
            }
            .padding(VSpace.pagePadding)
        }
    }

    private var expanded: some View {
        GeometryReader { geo in
            HStack(alignment: .top, spacing: VSpace.lg) {
                ScrollView { presetLibrary(horizontal: false).padding(VSpace.lg) }
                    .frame(width: min(230, geo.size.width * 0.23))
                    .background(PdigV2Colors.surface)

                ScrollView {
                    VStack(alignment: .leading, spacing: VSpace.lg) {
                        Text(title).font(VFont.pageTitle()).foregroundColor(PdigV2Colors.textPrimary)
                        previewStage
                        Text("外观设置仅用于本机显示，不会改变事实、关系或变更记录。")
                            .font(VFont.meta()).foregroundColor(PdigV2Colors.textMuted)
                    }.padding(.vertical, VSpace.pagePadding)
                }
                .frame(maxWidth: .infinity)

                ScrollView { inspector.padding(VSpace.lg) }
                    .frame(width: min(300, geo.size.width * 0.30))
                    .background(PdigV2Colors.surface)
            }
        }
    }

    private var previewStage: some View {
        VStack(alignment: .leading, spacing: VSpace.md) {
            VSectionHeader(title: "实时预览")
            preview(current)
                .padding(VSpace.xl)
                .frame(maxWidth: .infinity, minHeight: sizeClass == .regular ? 330 : 180)
                .background(
                    LinearGradient(colors: [PdigV2Colors.surface, PdigV2Colors.primarySoft.opacity(0.55)],
                                   startPoint: .topLeading, endPoint: .bottomTrailing)
                )
                .clipShape(RoundedRectangle(cornerRadius: VRadius.xl, style: .continuous))
                .overlay(RoundedRectangle(cornerRadius: VRadius.xl).stroke(PdigV2Colors.borderSubtle, lineWidth: 1))
        }
        .accessibilityIdentifier(VTestIds.customizationPreview)
    }

    @ViewBuilder
    private func presetLibrary(horizontal: Bool) -> some View {
        VStack(alignment: .leading, spacing: VSpace.md) {
            VSectionHeader(title: "主题")
            if horizontal {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: VSpace.sm) { presetButtons }
                }
            } else {
                VStack(spacing: VSpace.sm) { presetButtons }
            }
        }
        .accessibilityIdentifier(VTestIds.customizationLibrary)
    }

    @ViewBuilder
    private var presetButtons: some View {
        ForEach(presets, id: \.self) { preset in
            Button {
                profile = VPresentationProfile(
                    targetType: current.targetType, targetId: current.targetId, themeId: preset,
                    material: current.material, accentColor: current.accentColor,
                    backgroundKind: current.backgroundKind, backgroundValue: preset,
                    layout: current.layout, maskSensitive: current.maskSensitive
                )
                saved = false
            } label: {
                HStack {
                    Text(presetLabel(preset)).font(VFont.secondary()).fontWeight(current.themeId == preset ? .semibold : .regular)
                    Spacer()
                    if current.themeId == preset { Image(systemName: "checkmark.circle.fill") }
                }
                .foregroundColor(current.themeId == preset ? PdigV2Colors.primaryText : PdigV2Colors.textSecondary)
                .padding(.horizontal, VSpace.md).frame(minHeight: 44)
            }
            .buttonStyle(.plain)
            .background(current.themeId == preset ? PdigV2Colors.primarySoft : PdigV2Colors.surfaceRaised)
            .clipShape(RoundedRectangle(cornerRadius: VRadius.md))
            .overlay(RoundedRectangle(cornerRadius: VRadius.md)
                .stroke(current.themeId == preset ? PdigV2Colors.primary.opacity(0.34) : PdigV2Colors.borderSubtle, lineWidth: 1))
        }
    }

    private var inspector: some View {
        VStack(alignment: .leading, spacing: VSpace.md) {
            VSectionHeader(title: "属性与样式")
            VDetailRow(label: "主题", value: presetLabel(current.themeId))
            VDetailRow(label: "材质", value: "原始")
            VDetailRow(label: "布局", value: "标准")
            VDetailRow(label: "强调色", value: "蓝")
            VDetailRow(label: "隐私遮罩", value: current.maskSensitive ? "开启" : "关闭")
            VSectionHeader(title: "显示与隐私")
            VStudioToggle(label: "昵称", value: "保留")
            VStudioToggle(label: "地区", value: "保留")
            VStudioToggle(label: "币种 / 运营商", value: "保留")
            Text(VCopy.presetNote).font(VFont.meta()).foregroundColor(PdigV2Colors.textMuted)
        }
        .accessibilityIdentifier(VTestIds.customizationInspector)
    }

    private var saveButton: some View {
        Button {
            saved = true
        } label: {
            Text(saved ? "已保存" : "保存").fontWeight(.semibold)
        }
        .buttonStyle(.borderedProminent)
        .tint(saved ? PdigV2Colors.positive : PdigV2Colors.primary)
    }

    private func presetLabel(_ id: String) -> String {
        switch id {
        case "minimal": return "极简"
        case "deep-space": return "深空"
        case "region", "country": return "国家 / 地区"
        case "city": return "城市"
        case "glass": return "玻璃"
        case "metal": return "金属"
        case "abstract": return "抽象"
        case "banking": return "银行"
        case "travel": return "旅行"
        case "recovery": return "恢复"
        case "work": return "工作"
        case "private": return "私人"
        default: return id
        }
    }
}

private struct VStudioToggle: View {
    let label: String
    let value: String
    var body: some View {
        HStack {
            Text(label).font(VFont.secondary()).foregroundColor(PdigV2Colors.textSecondary)
            Spacer()
            Text(value).font(VFont.meta()).foregroundColor(PdigV2Colors.primaryText)
        }
        .padding(.vertical, 4).frame(minHeight: 44)
    }
}
