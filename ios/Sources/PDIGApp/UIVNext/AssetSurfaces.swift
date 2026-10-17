// AssetSurfaces —— 卡面 / 号码面（custom card face / number identity surface）。
//
// 与 desktop AssetSurfaces.kt 语义一致：
//  - 卡面背景 = bundled procedural 渐变（preset 确定性；禁远程图片）；
//  - last4 始终遮罩（privacyMask.maskCardLast4 默认 true）；
//  - NumberFace = 号码身份面（昵称/遮罩号码/region/carrier/SIM/role/usages/status）。

import SwiftUI

/// 卡面程序化背景（preset → 确定性渐变；desktop cardFaceBrush 移植）。
func cardFaceGradient(_ preset: String) -> LinearGradient {
    switch preset {
    case "deep-space":
        return LinearGradient(
            colors: [PdigV2Colors.surfaceRaised, PdigV2Colors.surface, PdigV2Colors.canvasDeep],
            startPoint: .topLeading, endPoint: .bottomTrailing
        )
    case "region":
        return LinearGradient(
            colors: [Color(hex: "#22427C"), Color(hex: "#0F2248"), Color(hex: "#071833")],
            startPoint: .topLeading, endPoint: .bottomTrailing
        )
    case "city":
        return LinearGradient(
            colors: [Color(hex: "#14345E"), Color(hex: "#0A1B3A")],
            startPoint: .topLeading, endPoint: .bottomTrailing
        )
    case "glass":
        return LinearGradient(
            colors: [PdigV2Colors.surfaceGlass, PdigV2Colors.surface.opacity(0.33)],
            startPoint: .topLeading, endPoint: .bottomTrailing
        )
    case "metal":
        return LinearGradient(
            colors: [Color(hex: "#3A4A62"), Color(hex: "#16202F"), Color(hex: "#3A4A62")],
            startPoint: .top, endPoint: .bottom
        )
    case "abstract":
        return LinearGradient(
            colors: [PdigV2Colors.primary, Color(hex: "#22316B"), PdigV2Colors.surface],
            startPoint: .topLeading, endPoint: .bottomTrailing
        )
    default:
        return LinearGradient(
            colors: [Color(hex: "#12264A"), Color(hex: "#0A1833")],
            startPoint: .topLeading, endPoint: .bottomTrailing
        )
    }
}

/// AssetCard：独立可操作对象卡面（grid / list / detail 共用；ratio 1.586 参考）。
struct VAssetCard: View {
    let card: VCard
    let privacyMask: Bool
    let onClick: () -> Void

    var body: some View {
        Button(action: onClick) {
            VStack(alignment: .leading, spacing: 0) {
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(card.nickname)
                            .font(VFont.secondary())
                            .fontWeight(.semibold)
                            .foregroundColor(PdigV2Colors.textPrimary)
                            .lineLimit(1)
                        Text(card.issuer)
                            .font(VFont.meta())
                            .foregroundColor(PdigV2Colors.textSecondary)
                            .lineLimit(1)
                    }
                    Spacer()
                    Text(vCardFormLabel(card.form))
                        .font(VFont.meta())
                        .foregroundColor(PdigV2Colors.textMuted)
                }
                Spacer()
                // last4 蒙版：privacyMask.maskCardLast4 默认 true；全局 Mask 开启时依旧只显示尾号。
                Text("•••• •••• •••• \(card.last4)")
                    .font(.system(size: 15, weight: .semibold, design: .monospaced))
                    .foregroundColor(PdigV2Colors.textPrimary)
                Spacer()
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("\(vCardTypeLabel(card.type)) · \(card.network)")
                            .font(VFont.meta())
                            .foregroundColor(PdigV2Colors.textSecondary)
                        Text("\(card.region) · \(card.currency) · 到期 \(card.expiry)")
                            .font(VFont.meta())
                            .foregroundColor(PdigV2Colors.textMuted)
                    }
                    Spacer()
                    VStatusBadge(status: card.status)
                }
            }
            .padding(VSpace.xl)
            .background(cardFaceGradient(card.preset))
            .frame(minHeight: 120)
            .aspectRatio(1.586, contentMode: .fit)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .clipShape(RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous)
                .stroke(PdigV2Colors.borderSubtle, lineWidth: 1)
        )
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(card.nickname)，\(card.issuer)，卡号尾号 \(card.last4)，\(vStatusLabel(card.status))")
    }
}

/// NumberFace：号码身份面（mobile number-face cards 与 detail 顶部共用）。
struct VNumberFace: View {
    let number: VNumber
    let privacyMask: Bool
    let onClick: () -> Void

    var body: some View {
        Button(action: onClick) {
            VStack(alignment: .leading, spacing: VSpace.md) {
                HStack {
                    Text(number.nickname)
                        .font(VFont.secondary())
                        .fontWeight(.semibold)
                        .foregroundColor(PdigV2Colors.textPrimary)
                    Spacer()
                    VStatusBadge(status: number.status)
                }
                // 遮罩规则：privacyMask.maskPhoneNumbers 默认 true；读屏只朗读遮罩号码。
                Text(number.maskedNumber)
                    .font(.system(size: 20, weight: .bold, design: .monospaced))
                    .foregroundColor(PdigV2Colors.textPrimary)
                HStack(spacing: VSpace.sm) {
                    VChip(text: vSimLabel(number.simKind))
                    VChip(text: vRoleLabel(number.role))
                    VChip(text: "\(number.countryCode) · \(number.region)")
                    if number.recoveryOnly { VChip(text: VCopy.recoveryOnly, highlight: true) }
                }
                Text(number.usages.joined(separator: " · "))
                    .font(VFont.meta())
                    .foregroundColor(PdigV2Colors.textSecondary)
            }
            .padding(VSpace.xl)
            .background(PdigV2Colors.surfaceRaised)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .clipShape(RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous)
                .stroke(PdigV2Colors.borderSubtle, lineWidth: 1)
        )
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(number.nickname)，号码已隐藏，\(vRoleLabel(number.role))")
    }
}
