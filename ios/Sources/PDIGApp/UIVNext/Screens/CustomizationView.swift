// CustomizationView —— 卡面 / 号码面定制工作室（三栏语义；Mobile preview-top + bottom-sheet）。
//
// customization-studio.md 契约：
//  - 编辑对象 = PresentationProfile（本地偏好；绝不写 .depmap；preset visual ≠ semantic role）；
//  - Desktop 三栏 0.22 / 0.46 / 0.32；Mobile 不复制 Desktop inspector（preview 在上 + 编辑在下）；
//  - 素材 bundled local / procedural；禁远程 URL；
//  - 预览始终蒙版（maskLast4 / maskNumber 默认 true）。
//  testId：pdig.customization.library / .preview / .inspector。

import SwiftUI

/// 卡面定制工作室（/infrastructure/cards/{id}/customize）。
struct CardCustomizationView: View {
    @ObservedObject var model: VNextModel

    private var card: VCard? {
        if case .cardCustomization(let id) = model.screen {
            return VNextDemoFixture.cardById(id)
        }
        return nil
    }

    var body: some View {
        if let card = card {
            CustomizationFrame(
                title: "\(VCopy.customizationCardTitle) · \(card.nickname)",
                presets: CARD_THEME_PRESETS,
                profile: VPresentationProfile.defaultFor(targetType: "card", targetId: card.id, preset: card.preset),
                onPresetChanged: { profile, preset in
                    VPresentationProfile(
                        targetType: profile.targetType, targetId: profile.targetId,
                        themeId: preset, material: profile.material, accentColor: profile.accentColor,
                        backgroundKind: profile.backgroundKind, backgroundValue: preset,
                        layout: profile.layout, maskSensitive: profile.maskSensitive
                    )
                },
                preview: { profile in
                    AnyView(VAssetCard(card: VCard(
                        id: card.id, nickname: card.nickname, issuer: card.issuer, last4: card.last4,
                        masked: card.masked, region: card.region, currency: card.currency,
                        type: card.type, form: card.form, network: card.network, expiry: card.expiry,
                        status: card.status, attention: card.attention, usages: card.usages,
                        preset: profile.themeId
                    ), privacyMask: model.privacyMask, onClick: {}))
                },
                rows: { profile in
                    [
                        (VCopy.fieldTheme, profile.themeId),
                        (VCopy.fieldMaterial, profile.material),
                        (VCopy.fieldAccent, profile.accentColor),
                        (VCopy.fieldBackground, profile.backgroundValue),
                        (VCopy.fieldLayout, profile.layout),
                        (VCopy.fieldMask, profile.maskSensitive ? VCopy.maskOn : VCopy.maskOff),
                    ]
                },
                toggles: ["显示昵称", "显示网络", "显示地区", "显示币种", "显示状态"],
                onBack: { model.back() }
            )
        }
    }
}

/// 号码面定制工作室（/infrastructure/numbers/{id}/customize）。
struct NumberCustomizationView: View {
    @ObservedObject var model: VNextModel

    private var number: VNumber? {
        if case .numberCustomization(let id) = model.screen {
            return VNextDemoFixture.numberById(id)
        }
        return nil
    }

    var body: some View {
        if let number = number {
            CustomizationFrame(
                title: "\(VCopy.customizationNumberTitle) · \(number.nickname)",
                presets: NUMBER_THEME_PRESETS,
                profile: VPresentationProfile.defaultFor(targetType: "phoneNumber", targetId: number.id, preset: "country"),
                onPresetChanged: { profile, preset in
                    VPresentationProfile(
                        targetType: profile.targetType, targetId: profile.targetId,
                        themeId: preset, material: profile.material, accentColor: profile.accentColor,
                        backgroundKind: profile.backgroundKind, backgroundValue: preset,
                        layout: profile.layout, maskSensitive: profile.maskSensitive
                    )
                },
                preview: { _ in
                    AnyView(VNumberFace(number: number, privacyMask: model.privacyMask, onClick: {}))
                },
                rows: { profile in
                    [
                        (VCopy.fieldTheme, profile.themeId),
                        (VCopy.fieldLayout, profile.layout),
                        (VCopy.fieldBackground, profile.backgroundValue),
                        (VCopy.fieldMask, profile.maskSensitive ? VCopy.maskOn : VCopy.maskOff),
                        ("提示", "preset visual ≠ 语义角色"),
                    ]
                },
                toggles: ["显示昵称", "显示运营商", "SIM 徽标", "主副号", "用途标签"],
                onBack: { model.back() }
            )
        }
    }
}

/// 定制工作室共享框架（Mobile：Preview 在上 + bottom-sheet 编辑在下；Desktop 三栏语义由列布局近似）。
private struct CustomizationFrame: View {
    let title: String
    let presets: [String]
    let profile: VPresentationProfile
    let onPresetChanged: (VPresentationProfile, String) -> VPresentationProfile
    let preview: (VPresentationProfile) -> AnyView
    let rows: (VPresentationProfile) -> [(String, String)]
    let toggles: [String]
    let onBack: () -> Void

    @State private var currentProfile: VPresentationProfile?
    @State private var saved = false

    private var activeProfile: VPresentationProfile { currentProfile ?? profile }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: VSpace.sectionGap) {
                // Title + 保存（本地 preference；common.save / 已保存反馈）
                HStack {
                    Text(title)
                        .font(VFont.pageTitle())
                        .foregroundColor(PdigV2Colors.textPrimary)
                    Spacer()
                    Button {
                        saved = true
                        // 保存 = 本地 preference 语义（演示层内存态；绝不写 .depmap）。
                        currentProfile = activeProfile
                    } label: {
                        Text(saved ? VCopy.savedLocal : VCopy.save)
                            .font(VFont.secondary())
                            .fontWeight(.semibold)
                            .foregroundColor(saved ? PdigV2Colors.positive : PdigV2Colors.canvasDeep)
                            .padding(.horizontal, VSpace.lg)
                            .frame(minHeight: 44)
                            .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    .background(saved ? PdigV2Colors.positive.opacity(0.2) : PdigV2Colors.primary)
                    .clipShape(RoundedRectangle(cornerRadius: VRadius.md, style: .continuous))
                }

                // Live Preview（pdig.customization.preview）
                VStack(alignment: .leading, spacing: VSpace.md) {
                    VSectionHeader(title: VCopy.livePreview)
                    preview(activeProfile)
                        .padding(VSpace.xl)
                        .frame(maxWidth: .infinity)
                        .background(PdigV2Colors.surface.opacity(0.7))
                        .clipShape(RoundedRectangle(cornerRadius: VRadius.xl, style: .continuous))
                }
                .accessibilityIdentifier(VTestIds.customizationPreview)

                // 素材库 / 属性（Mobile bottom-sheet 语义：编辑在下）
                VStack(alignment: .leading, spacing: VSpace.md) {
                    VSectionHeader(title: VCopy.presetLibrary)
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: VSpace.sm) {
                            ForEach(presets, id: \.self) { preset in
                                VFilterChip(
                                    label: preset,
                                    selected: activeProfile.themeId == preset
                                ) {
                                    currentProfile = onPresetChanged(activeProfile, preset)
                                    saved = false
                                }
                            }
                        }
                    }
                }
                .accessibilityIdentifier(VTestIds.customizationLibrary)

                VStack(alignment: .leading, spacing: VSpace.md) {
                    VSectionHeader(title: VCopy.propertyInspector)
                    ForEach(Array(rows(activeProfile).enumerated()), id: \.offset) { _, row in
                        VDetailRow(label: row.0, value: row.1)
                    }
                    ForEach(toggles, id: \.self) { toggle in
                        HStack {
                            Text(toggle)
                                .font(VFont.secondary())
                                .foregroundColor(PdigV2Colors.textSecondary)
                            Spacer()
                            Text("开")
                                .font(VFont.meta())
                                .fontWeight(.semibold)
                                .foregroundColor(PdigV2Colors.primaryBright)
                                .padding(.horizontal, VSpace.md)
                                .frame(minHeight: 44)
                                .background(PdigV2Colors.primary.opacity(0.3))
                                .clipShape(RoundedRectangle(cornerRadius: VRadius.sm, style: .continuous))
                        }
                    }
                    Text(VCopy.presetNote)
                        .font(VFont.meta())
                        .foregroundColor(PdigV2Colors.textMuted)
                }
                .accessibilityIdentifier(VTestIds.customizationInspector)
            }
            .padding(VSpace.pagePadding)
        }
        .vPageBackground()
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                VBackButton { onBack() }
            }
        }
        .navigationBarBackButtonHidden(true)
    }
}
