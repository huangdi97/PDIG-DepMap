// CustomizationView —— consumer-grade presentation Studio.
//
// iPhone: preview first, controls below.
// iPad: theme library / live preview / inspector.
// Every interactive control feeds the renderer and persists only to local PresentationProfile storage.

import SwiftUI

private struct VAccentChoice: Identifiable {
    let value: String
    let label: String
    var id: String { value }
}

private let vAccentChoices: [VAccentChoice] = [
    .init(value: "default", label: "原始"),
    .init(value: "#4D74FF", label: "蓝"),
    .init(value: "#63D7C5", label: "青"),
    .init(value: "#F4B85A", label: "金"),
    .init(value: "#D06A8C", label: "玫红"),
]

private let vLayouts = ["standard", "compact", "focused"]
private let vCardMaterials = ["default", "glass", "metal", "brushed", "matte", "satin"]
private let vNumberMaterials = ["default", "matte", "glass", "metal"]

struct CardCustomizationView: View {
    @ObservedObject var model: VNextModel

    private var card: VCard? {
        if case .cardCustomization(let id) = model.screen { return VNextDemoFixture.cardById(id) }
        return nil
    }

    var body: some View {
        if let card {
            let base = VPresentationProfile.defaultFor(targetType: "card", targetId: card.id, preset: card.preset)
            VCustomizationFrame(
                title: "卡面定制 · \(card.nickname)",
                presets: CARD_THEME_PRESETS,
                materials: vCardMaterials,
                displayFields: ["昵称", "卡组织", "地区", "币种", "状态"],
                base: base,
                initial: model.presentationProfile(targetType: "card", targetId: card.id, fallbackPreset: card.preset),
                preview: { profile in
                    AnyView(VAssetCard(
                        card: card,
                        privacyMask: model.privacyMask || profile.maskSensitive,
                        presetOverride: profile.themeId,
                        materialOverride: profile.material,
                        accentOverride: profile.accentColor,
                        layoutOverride: profile.layout,
                        onClick: {}
                    ))
                },
                globalPrivacyMask: model.privacyMask,
                onSave: { model.savePresentationProfile($0) },
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
            let base = VPresentationProfile.defaultFor(targetType: "phoneNumber", targetId: number.id, preset: number.preset)
            VCustomizationFrame(
                title: "号码面定制 · \(number.nickname)",
                presets: NUMBER_THEME_PRESETS,
                materials: vNumberMaterials,
                displayFields: ["昵称", "运营商", "SIM 类型", "主副号", "用途标签"],
                base: base,
                initial: model.presentationProfile(targetType: "phoneNumber", targetId: number.id, fallbackPreset: number.preset),
                preview: { profile in
                    AnyView(VNumberFace(
                        number: number,
                        privacyMask: model.privacyMask || profile.maskSensitive,
                        presetOverride: profile.themeId,
                        materialOverride: profile.material,
                        accentOverride: profile.accentColor,
                        layoutOverride: profile.layout,
                        onClick: {}
                    ))
                },
                globalPrivacyMask: model.privacyMask,
                onSave: { model.savePresentationProfile($0) },
                onBack: { model.back() }
            )
        }
    }
}

private struct VCustomizationFrame: View {
    let title: String
    let presets: [String]
    let materials: [String]
    let displayFields: [String]
    let base: VPresentationProfile
    let initial: VPresentationProfile
    let preview: (VPresentationProfile) -> AnyView
    let globalPrivacyMask: Bool
    let onSave: (VPresentationProfile) -> Void
    let onBack: () -> Void

    @Environment(\.horizontalSizeClass) private var sizeClass
    @State private var profile: VPresentationProfile?
    @State private var lastSaved: VPresentationProfile?
    @State private var savePulse = false

    private var current: VPresentationProfile { profile ?? initial }
    private var savedProfile: VPresentationProfile { lastSaved ?? initial }
    private var isDirty: Bool { current != savedProfile }

    var body: some View {
        Group {
            if sizeClass == .regular { expanded } else { compact }
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
                header
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
                ScrollView {
                    VStack(alignment: .leading, spacing: VSpace.lg) {
                        Text("主题").font(VFont.sectionTitle()).foregroundColor(PdigV2Colors.textPrimary)
                        presetLibrary(horizontal: false)
                    }
                    .padding(VSpace.lg)
                }
                .frame(width: min(230, geo.size.width * 0.23))
                .background(PdigV2Colors.surface)

                ScrollView {
                    VStack(alignment: .leading, spacing: VSpace.lg) {
                        header
                        previewStage
                        Text("外观设置只保存在本机，并且不会改变卡片、号码、依赖关系或确认状态。")
                            .font(VFont.meta()).foregroundColor(PdigV2Colors.textMuted)
                    }
                    .padding(.vertical, VSpace.pagePadding)
                }
                .frame(maxWidth: .infinity)

                ScrollView {
                    inspector.padding(VSpace.lg)
                }
                .frame(width: min(320, geo.size.width * 0.31))
                .background(PdigV2Colors.surface)
            }
        }
    }

    private var header: some View {
        HStack(alignment: .firstTextBaseline) {
            VStack(alignment: .leading, spacing: 3) {
                Text(title).font(VFont.pageTitle()).foregroundColor(PdigV2Colors.textPrimary)
                Text(isDirty ? "有未保存的外观更改" : "当前外观已保存")
                    .font(VFont.meta())
                    .foregroundColor(isDirty ? PdigV2Colors.warning : PdigV2Colors.positive)
            }
            Spacer()
            if savePulse {
                Label("已保存", systemImage: "checkmark.circle.fill")
                    .font(VFont.meta()).foregroundColor(PdigV2Colors.positive)
            }
        }
    }

    private var previewStage: some View {
        VStack(alignment: .leading, spacing: VSpace.md) {
            VSectionHeader(title: "实时预览")
            preview(current)
                .padding(VSpace.xl)
                .frame(maxWidth: .infinity, minHeight: sizeClass == .regular ? 330 : 190)
                .background(
                    LinearGradient(
                        colors: [PdigV2Colors.surface, PdigV2Colors.primarySoft.opacity(0.55)],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
                .clipShape(RoundedRectangle(cornerRadius: VRadius.xl))
                .overlay(RoundedRectangle(cornerRadius: VRadius.xl).stroke(PdigV2Colors.borderSubtle, lineWidth: 1))
        }
        .accessibilityIdentifier(VTestIds.customizationPreview)
    }

    @ViewBuilder
    private func presetLibrary(horizontal: Bool) -> some View {
        if horizontal {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: VSpace.sm) { presetButtons }
            }
            .accessibilityIdentifier(VTestIds.customizationLibrary)
        } else {
            VStack(spacing: VSpace.sm) { presetButtons }
                .accessibilityIdentifier(VTestIds.customizationLibrary)
        }
    }

    @ViewBuilder
    private var presetButtons: some View {
        ForEach(presets, id: \.self) { preset in
            VStudioChoice(
                label: presetLabel(preset),
                selected: current.themeId == preset,
                swatch: nil
            ) {
                profile = current.replacingTheme(preset)
                savePulse = false
            }
        }
    }

    private var inspector: some View {
        VStack(alignment: .leading, spacing: VSpace.lg) {
            VSectionHeader(title: "材质")
            choiceScroll(materials) { material in
                VStudioChoice(label: materialLabel(material), selected: current.material == material, swatch: nil) {
                    profile = current.replacingMaterial(material)
                    savePulse = false
                }
            }

            VSectionHeader(title: "布局")
            choiceScroll(vLayouts) { layout in
                VStudioChoice(label: layoutLabel(layout), selected: current.layout == layout, swatch: nil) {
                    profile = current.replacingLayout(layout)
                    savePulse = false
                }
            }

            VSectionHeader(title: "强调色")
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: VSpace.sm) {
                    ForEach(vAccentChoices) { choice in
                        VStudioChoice(
                            label: choice.label,
                            selected: current.accentColor == choice.value,
                            swatch: vPresentationAccent(choice.value)
                        ) {
                            profile = current.replacingAccent(choice.value)
                            savePulse = false
                        }
                    }
                }
            }

            VSectionHeader(title: "显示与隐私")
            ForEach(displayFields, id: \.self) { field in
                HStack {
                    Text(field).font(VFont.secondary()).foregroundColor(PdigV2Colors.textSecondary)
                    Spacer()
                    VChip(text: "保留")
                }
                .padding(.horizontal, VSpace.md)
                .frame(minHeight: 44)
                .background(PdigV2Colors.surfaceRaised)
                .clipShape(RoundedRectangle(cornerRadius: VRadius.md))
            }

            Toggle(isOn: Binding(
                get: { current.maskSensitive },
                set: {
                    profile = current.replacingMask($0)
                    savePulse = false
                }
            )) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("对象单独遮蔽").font(VFont.secondary()).fontWeight(.semibold).foregroundColor(PdigV2Colors.textPrimary)
                    Text(globalPrivacyMask ? "全局隐私遮蔽已开启；预览仍会保持敏感字段隐藏。" : "只对当前对象应用敏感信息遮蔽。")
                        .font(VFont.meta()).foregroundColor(PdigV2Colors.textMuted)
                }
            }
            .tint(PdigV2Colors.primary)
            .padding(VSpace.md)
            .background(PdigV2Colors.surfaceRaised)
            .clipShape(RoundedRectangle(cornerRadius: VRadius.md))

            VSectionHeader(title: "当前设置")
            VStack(spacing: VSpace.sm) {
                VDetailRow(label: "外观", value: presetLabel(current.themeId))
                VDetailRow(label: "材质", value: materialLabel(current.material))
                VDetailRow(label: "布局", value: layoutLabel(current.layout))
                VDetailRow(label: "强调", value: accentLabel(current.accentColor))
            }
            .padding(VSpace.md)
            .background(PdigV2Colors.primarySoft.opacity(0.58))
            .clipShape(RoundedRectangle(cornerRadius: VRadius.md))

            Button {
                profile = base
                savePulse = false
            } label: {
                Text("恢复原始外观")
                    .font(VFont.secondary()).fontWeight(.semibold)
                    .foregroundColor(current == base ? PdigV2Colors.textMuted : PdigV2Colors.textSecondary)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, VSpace.md)
                    .frame(minHeight: 48)
            }
            .buttonStyle(.plain)
            .disabled(current == base)
            .background(PdigV2Colors.surfaceRaised)
            .clipShape(RoundedRectangle(cornerRadius: VRadius.md))
        }
        .accessibilityIdentifier(VTestIds.customizationInspector)
    }

    private func choiceScroll<T: Hashable, Content: View>(
        _ values: [T],
        @ViewBuilder content: @escaping (T) -> Content
    ) -> some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: VSpace.sm) {
                ForEach(values, id: \.self) { value in content(value) }
            }
        }
    }

    private var saveButton: some View {
        Button {
            onSave(current)
            lastSaved = current
            savePulse = true
        } label: {
            Text(isDirty ? "保存" : "已保存").fontWeight(.semibold)
        }
        .buttonStyle(.borderedProminent)
        .tint(isDirty ? PdigV2Colors.primary : PdigV2Colors.positive)
        .disabled(!isDirty)
        .accessibilityIdentifier("pdig.customization.save")
    }

    private func presetLabel(_ id: String) -> String {
        switch id {
        case "minimal": return "极简"
        case "deep-space": return "深空"
        case "region": return "地域"
        case "country": return "国家 / 地域"
        case "city": return "城市"
        case "glass": return "玻璃"
        case "metal": return "金属"
        case "abstract": return "抽象"
        case "banking": return "银行验证"
        case "travel": return "旅行"
        case "recovery": return "恢复"
        case "work": return "工作"
        case "private": return "私人"
        default: return id
        }
    }

    private func materialLabel(_ id: String) -> String {
        switch id {
        case "default": return "原始"
        case "glass": return "玻璃"
        case "metal": return "金属"
        case "brushed": return "拉丝"
        case "matte": return "哑光"
        case "satin": return "缎面"
        case "translucent": return "透光"
        default: return "标准"
        }
    }

    private func layoutLabel(_ id: String) -> String {
        switch id {
        case "compact": return "紧凑"
        case "focused": return "聚焦"
        default: return "标准"
        }
    }

    private func accentLabel(_ value: String) -> String {
        vAccentChoices.first(where: { $0.value == value })?.label ?? "自定义"
    }
}

private struct VStudioChoice: View {
    let label: String
    let selected: Bool
    let swatch: Color?
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 7) {
                if let swatch {
                    RoundedRectangle(cornerRadius: 3).fill(swatch).frame(width: 14, height: 14)
                }
                Text(label).font(VFont.meta()).fontWeight(selected ? .semibold : .regular)
                if selected { Image(systemName: "checkmark").font(.system(size: 9, weight: .bold)) }
            }
            .foregroundColor(selected ? PdigV2Colors.primaryText : PdigV2Colors.textSecondary)
            .padding(.horizontal, VSpace.md)
            .frame(minHeight: 44)
        }
        .buttonStyle(.plain)
        .background(selected ? PdigV2Colors.primarySoft : PdigV2Colors.surfaceRaised)
        .clipShape(RoundedRectangle(cornerRadius: VRadius.md))
        .overlay(RoundedRectangle(cornerRadius: VRadius.md)
            .stroke(selected ? PdigV2Colors.primary.opacity(0.40) : PdigV2Colors.borderSubtle, lineWidth: 1))
    }
}
