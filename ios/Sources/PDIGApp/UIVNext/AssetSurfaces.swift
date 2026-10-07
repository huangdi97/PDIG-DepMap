// AssetSurfaces —— visual identity surfaces for cards and phone numbers.
//
// Every Studio control used here is presentation-only. Theme, material, accent and layout change only
// the local visual rendering; no identity/dependency/evidence/confirmation field is mutated.

import SwiftUI

func cardFaceGradient(_ preset: String) -> LinearGradient {
    switch preset {
    case "deep-space":
        return LinearGradient(colors: [Color(hex: "#0A1E3B"), Color(hex: "#06162F"), Color(hex: "#112A52")], startPoint: .topLeading, endPoint: .bottomTrailing)
    case "region":
        return LinearGradient(colors: [Color(hex: "#22427C"), Color(hex: "#0F2248"), Color(hex: "#071833")], startPoint: .topLeading, endPoint: .bottomTrailing)
    case "city":
        return LinearGradient(colors: [Color(hex: "#14345E"), Color(hex: "#0A1B3A")], startPoint: .topLeading, endPoint: .bottomTrailing)
    case "glass":
        return LinearGradient(colors: [Color(hex: "#253A5D"), Color(hex: "#0B1930")], startPoint: .topLeading, endPoint: .bottomTrailing)
    case "metal":
        return LinearGradient(colors: [Color(hex: "#3A4A62"), Color(hex: "#16202F"), Color(hex: "#3A4A62")], startPoint: .top, endPoint: .bottom)
    case "abstract":
        return LinearGradient(colors: [Color(hex: "#4338CA"), Color(hex: "#1E2F68"), Color(hex: "#09162C")], startPoint: .topLeading, endPoint: .bottomTrailing)
    default:
        return LinearGradient(colors: [Color(hex: "#12264A"), Color(hex: "#0A1833")], startPoint: .topLeading, endPoint: .bottomTrailing)
    }
}

func numberFaceGradient(_ preset: String) -> LinearGradient {
    switch preset {
    case "banking":
        return LinearGradient(colors: [Color(hex: "#163C61"), Color(hex: "#071A34")], startPoint: .topLeading, endPoint: .bottomTrailing)
    case "travel":
        return LinearGradient(colors: [Color(hex: "#1B4B69"), Color(hex: "#123354"), Color(hex: "#071A34")], startPoint: .topLeading, endPoint: .bottomTrailing)
    case "recovery":
        return LinearGradient(colors: [Color(hex: "#3C315B"), Color(hex: "#161D38")], startPoint: .topLeading, endPoint: .bottomTrailing)
    case "work":
        return LinearGradient(colors: [Color(hex: "#164D52"), Color(hex: "#092A37")], startPoint: .topLeading, endPoint: .bottomTrailing)
    case "private":
        return LinearGradient(colors: [Color(hex: "#332B4C"), Color(hex: "#10172A")], startPoint: .topLeading, endPoint: .bottomTrailing)
    case "minimal":
        return LinearGradient(colors: [Color(hex: "#25364A"), Color(hex: "#111B29")], startPoint: .topLeading, endPoint: .bottomTrailing)
    case "city":
        return LinearGradient(colors: [Color(hex: "#183E66"), Color(hex: "#091A34")], startPoint: .topLeading, endPoint: .bottomTrailing)
    default:
        return LinearGradient(colors: [Color(hex: "#0B2E58"), Color(hex: "#071A34")], startPoint: .topLeading, endPoint: .bottomTrailing)
    }
}

func vPresentationAccent(_ value: String?) -> Color? {
    guard let value, value.hasPrefix("#"), value.count == 7 else { return nil }
    return Color(hex: value)
}

private func materialOverlay(_ material: String?) -> LinearGradient {
    switch material {
    case "glass", "translucent":
        return LinearGradient(colors: [Color.white.opacity(0.14), Color.white.opacity(0.02)], startPoint: .topLeading, endPoint: .bottomTrailing)
    case "metal":
        return LinearGradient(colors: [Color.white.opacity(0.12), Color.black.opacity(0.09), Color.white.opacity(0.08)], startPoint: .leading, endPoint: .trailing)
    case "brushed":
        return LinearGradient(colors: [Color.white.opacity(0.10), Color.clear, Color.white.opacity(0.06), Color.clear], startPoint: .top, endPoint: .bottom)
    case "matte":
        return LinearGradient(colors: [Color.black.opacity(0.08), Color.black.opacity(0.16)], startPoint: .top, endPoint: .bottom)
    case "satin":
        return LinearGradient(colors: [Color.white.opacity(0.05), Color.white.opacity(0.12), Color.clear], startPoint: .topLeading, endPoint: .bottomTrailing)
    default:
        return LinearGradient(colors: [Color.clear, Color.clear], startPoint: .top, endPoint: .bottom)
    }
}

private func layoutPadding(_ layout: String?) -> CGFloat {
    layout == "compact" ? VSpace.md : VSpace.xl
}

private func identityNumberSize(_ layout: String?) -> CGFloat {
    layout == "focused" ? 18 : (layout == "compact" ? 13 : 15)
}

struct VAssetCard: View {
    let card: VCard
    let privacyMask: Bool
    var presetOverride: String? = nil
    var materialOverride: String? = nil
    var accentOverride: String? = nil
    var layoutOverride: String? = nil
    let onClick: () -> Void

    var body: some View {
        let accent = vPresentationAccent(accentOverride)
        Button(action: onClick) {
            VStack(alignment: .leading, spacing: layoutOverride == "compact" ? 3 : 0) {
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(card.nickname)
                            .font(VFont.secondary()).fontWeight(.semibold)
                            .foregroundColor(PdigV2Colors.assetTextPrimary).lineLimit(1)
                        Text(card.issuer)
                            .font(VFont.meta()).foregroundColor(PdigV2Colors.assetTextSecondary).lineLimit(1)
                    }
                    Spacer()
                    Text(vCardFormLabel(card.form))
                        .font(VFont.meta()).foregroundColor(PdigV2Colors.assetTextMuted)
                }
                if layoutOverride != "compact" { Spacer() }
                Text("•••• •••• •••• \(card.last4)")
                    .font(.system(size: identityNumberSize(layoutOverride), weight: .semibold, design: .monospaced))
                    .foregroundColor(PdigV2Colors.assetTextPrimary)
                    .frame(maxWidth: .infinity, alignment: layoutOverride == "focused" ? .center : .leading)
                if layoutOverride != "compact" { Spacer() }
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("\(vCardTypeLabel(card.type)) · \(card.network)")
                            .font(VFont.meta()).foregroundColor(PdigV2Colors.assetTextSecondary)
                        Text("\(regionName(card.region)) · \(card.currency) · 到期 \(card.expiry)")
                            .font(VFont.meta()).foregroundColor(PdigV2Colors.assetTextMuted)
                    }
                    Spacer()
                    VStatusBadge(status: card.status)
                }
            }
            .padding(layoutPadding(layoutOverride))
            .background(cardFaceGradient(presetOverride ?? card.preset))
            .overlay(materialOverlay(materialOverride))
            .overlay(alignment: .top) {
                if let accent {
                    Rectangle().fill(accent.opacity(0.9)).frame(height: 3)
                }
            }
            .frame(minHeight: layoutOverride == "compact" ? 96 : 120)
            .aspectRatio(1.586, contentMode: .fit)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .clipShape(RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous)
            .stroke((accent ?? Color.white).opacity(accent == nil ? 0.10 : 0.42), lineWidth: 1))
        .shadow(color: Color.black.opacity(0.12), radius: 10, y: 4)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(card.nickname)，\(card.issuer)，卡号尾号 \(card.last4)，\(vStatusLabel(card.status))")
    }
}

struct VNumberFace: View {
    let number: VNumber
    let privacyMask: Bool
    var presetOverride: String? = nil
    var materialOverride: String? = nil
    var accentOverride: String? = nil
    var layoutOverride: String? = nil
    let onClick: () -> Void

    var body: some View {
        let accent = vPresentationAccent(accentOverride)
        Button(action: onClick) {
            VStack(alignment: .leading, spacing: layoutOverride == "compact" ? VSpace.sm : VSpace.md) {
                HStack {
                    HStack(spacing: 7) {
                        Image(systemName: "antenna.radiowaves.left.and.right")
                            .foregroundColor(accent ?? PdigV2Colors.primaryBright)
                        Text(number.nickname)
                            .font(VFont.secondary()).fontWeight(.semibold)
                            .foregroundColor(PdigV2Colors.assetTextPrimary)
                    }
                    Spacer()
                    VStatusBadge(status: number.status)
                }
                Text(number.maskedNumber)
                    .font(.system(size: layoutOverride == "focused" ? 23 : 20, weight: .bold, design: .monospaced))
                    .foregroundColor(PdigV2Colors.assetTextPrimary)
                    .frame(maxWidth: .infinity, alignment: layoutOverride == "focused" ? .center : .leading)
                HStack(spacing: VSpace.sm) {
                    identityChip(vSimLabel(number.simKind), accent: accent)
                    identityChip(vRoleLabel(number.role), accent: accent)
                    identityChip("\(number.countryCode) · \(number.region)", accent: accent)
                    if number.recoveryOnly { identityChip(VCopy.recoveryOnly, warning: true, accent: accent) }
                }
                if layoutOverride != "compact" {
                    Text(number.usages.joined(separator: " · "))
                        .font(VFont.meta()).foregroundColor(PdigV2Colors.assetTextSecondary)
                }
            }
            .padding(layoutPadding(layoutOverride))
            .background(numberFaceGradient(presetOverride ?? number.preset))
            .overlay(materialOverlay(materialOverride))
            .overlay(alignment: .top) {
                if let accent {
                    Rectangle().fill(accent.opacity(0.9)).frame(height: 3)
                }
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .clipShape(RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous)
            .stroke((accent ?? Color.white).opacity(accent == nil ? 0.10 : 0.42), lineWidth: 1))
        .shadow(color: Color.black.opacity(0.10), radius: 9, y: 4)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(number.nickname)，号码已隐藏，\(vRoleLabel(number.role))")
    }

    private func identityChip(_ text: String, warning: Bool = false, accent: Color?) -> some View {
        Text(text)
            .font(VFont.meta()).fontWeight(.medium)
            .foregroundColor(warning ? Color(hex: "#FFD38A") : PdigV2Colors.assetTextSecondary)
            .padding(.horizontal, VSpace.sm).padding(.vertical, 4)
            .background((warning ? PdigV2Colors.warning : (accent ?? Color.white)).opacity(warning ? 0.18 : 0.10))
            .clipShape(RoundedRectangle(cornerRadius: VRadius.sm, style: .continuous))
    }
}


/// Resolver used outside Studio so saved local PresentationProfile affects every asset surface consistently.
struct VResolvedAssetCard: View {
    let card: VCard
    @ObservedObject var model: VNextModel
    let onClick: () -> Void

    var body: some View {
        let profile = model.presentationProfile(targetType: "card", targetId: card.id, fallbackPreset: card.preset)
        VAssetCard(
            card: card,
            privacyMask: model.privacyMask || profile.maskSensitive,
            presetOverride: profile.themeId,
            materialOverride: profile.material,
            accentOverride: profile.accentColor,
            layoutOverride: profile.layout,
            onClick: onClick
        )
    }
}

struct VResolvedNumberFace: View {
    let number: VNumber
    @ObservedObject var model: VNextModel
    let onClick: () -> Void

    var body: some View {
        let profile = model.presentationProfile(targetType: "phoneNumber", targetId: number.id, fallbackPreset: number.preset)
        VNumberFace(
            number: number,
            privacyMask: model.privacyMask || profile.maskSensitive,
            presetOverride: profile.themeId,
            materialOverride: profile.material,
            accentOverride: profile.accentColor,
            layoutOverride: profile.layout,
            onClick: onClick
        )
    }
}
