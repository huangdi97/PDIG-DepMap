// AssetSurfaces —— iOS Card / Number identity surfaces.
//
// Light-first app shell, dark localized identity canvases. PresentationProfile changes appearance only.

import SwiftUI

func cardFaceGradient(_ preset: String) -> LinearGradient {
    switch preset {
    case "deep-space":
        return LinearGradient(colors: [Color(hex: "#0A1E3B"), Color(hex: "#06162F"), Color(hex: "#112A52")],
                              startPoint: .topLeading, endPoint: .bottomTrailing)
    case "region":
        return LinearGradient(colors: [Color(hex: "#22427C"), Color(hex: "#0F2248"), Color(hex: "#071833")],
                              startPoint: .topLeading, endPoint: .bottomTrailing)
    case "city":
        return LinearGradient(colors: [Color(hex: "#14345E"), Color(hex: "#0A1B3A")],
                              startPoint: .topLeading, endPoint: .bottomTrailing)
    case "glass":
        return LinearGradient(colors: [Color(hex: "#253A5D"), Color(hex: "#0B1930").opacity(0.96)],
                              startPoint: .topLeading, endPoint: .bottomTrailing)
    case "metal":
        return LinearGradient(colors: [Color(hex: "#3A4A62"), Color(hex: "#16202F"), Color(hex: "#3A4A62")],
                              startPoint: .top, endPoint: .bottom)
    case "abstract":
        return LinearGradient(colors: [Color(hex: "#4338CA"), Color(hex: "#1E2F68"), Color(hex: "#09162C")],
                              startPoint: .topLeading, endPoint: .bottomTrailing)
    default:
        return LinearGradient(colors: [Color(hex: "#12264A"), Color(hex: "#0A1833")],
                              startPoint: .topLeading, endPoint: .bottomTrailing)
    }
}

struct VAssetCard: View {
    let card: VCard
    let privacyMask: Bool
    var presetOverride: String? = nil
    let onClick: () -> Void

    var body: some View {
        Button(action: onClick) {
            VStack(alignment: .leading, spacing: 0) {
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
                Spacer()
                Text("•••• •••• •••• \(card.last4)")
                    .font(.system(size: 15, weight: .semibold, design: .monospaced))
                    .foregroundColor(PdigV2Colors.assetTextPrimary)
                Spacer()
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("\(vCardTypeLabel(card.type)) · \(card.network)")
                            .font(VFont.meta()).foregroundColor(PdigV2Colors.assetTextSecondary)
                        Text("\(card.region) · \(card.currency) · 到期 \(card.expiry)")
                            .font(VFont.meta()).foregroundColor(PdigV2Colors.assetTextMuted)
                    }
                    Spacer()
                    VStatusBadge(status: card.status)
                }
            }
            .padding(VSpace.xl)
            .background(cardFaceGradient(presetOverride ?? card.preset))
            .frame(minHeight: 120)
            .aspectRatio(1.586, contentMode: .fit)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .clipShape(RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous)
            .stroke(Color.white.opacity(0.10), lineWidth: 1))
        .shadow(color: Color.black.opacity(0.12), radius: 10, y: 4)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(card.nickname)，\(card.issuer)，卡号尾号 \(card.last4)，\(vStatusLabel(card.status))")
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

struct VNumberFace: View {
    let number: VNumber
    let privacyMask: Bool
    var presetOverride: String? = nil
    let onClick: () -> Void

    var body: some View {
        Button(action: onClick) {
            VStack(alignment: .leading, spacing: VSpace.md) {
                HStack {
                    HStack(spacing: 7) {
                        Image(systemName: "antenna.radiowaves.left.and.right")
                            .foregroundColor(PdigV2Colors.primaryBright)
                        Text(number.nickname)
                            .font(VFont.secondary()).fontWeight(.semibold)
                            .foregroundColor(PdigV2Colors.assetTextPrimary)
                    }
                    Spacer()
                    VStatusBadge(status: number.status)
                }
                Text(number.maskedNumber)
                    .font(.system(size: 20, weight: .bold, design: .monospaced))
                    .foregroundColor(PdigV2Colors.assetTextPrimary)
                HStack(spacing: VSpace.sm) {
                    identityChip(vSimLabel(number.simKind))
                    identityChip(vRoleLabel(number.role))
                    identityChip("\(number.countryCode) · \(number.region)")
                    if number.recoveryOnly { identityChip(VCopy.recoveryOnly, warning: true) }
                }
                Text(number.usages.joined(separator: " · "))
                    .font(VFont.meta()).foregroundColor(PdigV2Colors.assetTextSecondary)
            }
            .padding(VSpace.xl)
            .background(
                numberFaceGradient(presetOverride ?? "country")
            )
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .clipShape(RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous)
            .stroke(Color.white.opacity(0.10), lineWidth: 1))
        .shadow(color: Color.black.opacity(0.10), radius: 9, y: 4)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(number.nickname)，号码已隐藏，\(vRoleLabel(number.role))")
    }

    private func identityChip(_ text: String, warning: Bool = false) -> some View {
        Text(text)
            .font(VFont.meta()).fontWeight(.medium)
            .foregroundColor(warning ? Color(hex: "#FFD38A") : PdigV2Colors.assetTextSecondary)
            .padding(.horizontal, VSpace.sm).padding(.vertical, 4)
            .background((warning ? PdigV2Colors.warning : Color.white).opacity(warning ? 0.18 : 0.10))
            .clipShape(RoundedRectangle(cornerRadius: VRadius.sm, style: .continuous))
    }
}
