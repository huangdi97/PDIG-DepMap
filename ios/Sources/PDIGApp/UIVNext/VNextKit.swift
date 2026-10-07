// VNextKit —— vNext 共享小组件（状态徽标 / 分节标题 / 标签 / 注意力行 / 地区行 / 过滤器）。
//
// 与 desktop VNextKit.kt 语义一致：
//  - 状态永不只靠颜色（icon + label + color 三通道，VISUAL_DNA L5）；
//  - disabled 必须附原因文案；注意力行 critical 最突出；
//  - RegionListItem = Globe 的非视觉替代（键盘/读屏可达，IA.md §7）。

import SwiftUI

/// StatusBadge：icon + label + color 三通道（状态永不只靠颜色）。
struct VStatusBadge: View {
    let status: String

    var body: some View {
        HStack(spacing: 4) {
            Image(systemName: vStatusIcon(status))
                .font(.system(size: 11, weight: .semibold))
                .foregroundColor(vStatusColor(status))
            Text(vStatusLabel(status))
                .font(VFont.meta())
                .foregroundColor(vStatusColor(status))
        }
        .padding(.horizontal, VSpace.sm)
        .padding(.vertical, 3)
        .background(PdigV2Colors.surfaceRaised)
        .clipShape(RoundedRectangle(cornerRadius: VRadius.sm, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: VRadius.sm, style: .continuous)
                .stroke(PdigV2Colors.borderSubtle, lineWidth: 1)
        )
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(vStatusLabel(status))（\(status)）")
    }
}

/// SectionHeader：小节标题 + hairline 分隔。
struct VSectionHeader: View {
    let title: String
    var trailing: AnyView? = nil

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text(title)
                    .font(VFont.sectionTitle())
                    .foregroundColor(PdigV2Colors.textPrimary)
                Spacer()
                if let trailing = trailing { trailing }
            }
            Rectangle()
                .fill(PdigV2Colors.borderSubtle)
                .frame(height: 1)
        }
    }
}

/// LabelChip：小标签（用途/角色/徽标；radius.sm 6）。
struct VChip: View {
    let text: String
    var highlight: Bool = false

    var body: some View {
        Text(text)
            .font(VFont.meta())
            .fontWeight(.medium)
            .foregroundColor(highlight ? PdigV2Colors.warning : PdigV2Colors.textSecondary)
            .padding(.horizontal, VSpace.sm)
            .padding(.vertical, 3)
            .background(highlight ? PdigV2Colors.warning.opacity(0.16) : PdigV2Colors.primarySoft)
            .clipShape(RoundedRectangle(cornerRadius: VRadius.sm, style: .continuous))
    }
}

/// DetailRow：label + value 两列。
struct VDetailRow: View {
    let label: String
    let value: String

    var body: some View {
        HStack {
            Text(label)
                .font(VFont.meta())
                .foregroundColor(PdigV2Colors.textMuted)
            Spacer()
            Text(value)
                .font(VFont.secondary())
                .fontWeight(.medium)
                .foregroundColor(PdigV2Colors.textPrimary)
                .multilineTextAlignment(.trailing)
        }
        .padding(.vertical, 5)
    }
}

/// FilterChip：过滤器（selected 用 primary 高亮；触控热区 ≥44）。
struct VFilterChip: View {
    let label: String
    let selected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(label)
                .font(VFont.meta())
                .fontWeight(selected ? .semibold : .regular)
                .foregroundColor(selected ? PdigV2Colors.primaryText : PdigV2Colors.textSecondary)
                .padding(.horizontal, VSpace.md)
                .frame(minHeight: VTouchTarget.ios)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .background(selected ? PdigV2Colors.primary.opacity(0.28) : PdigV2Colors.surfaceRaised)
        .clipShape(RoundedRectangle(cornerRadius: VRadius.sm, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: VRadius.sm, style: .continuous)
                .stroke(selected ? PdigV2Colors.primaryBright : PdigV2Colors.borderSubtle, lineWidth: 1)
        )
    }
}

/// AttentionRow：icon + label + color 三通道；critical 最突出；点击走真路由。
struct VAttentionRow: View {
    let item: VAttentionItem
    let onClick: () -> Void

    private var severityColor: Color {
        vStatusColor(item.severity)
    }

    var body: some View {
        Button(action: onClick) {
            HStack(spacing: VSpace.lg) {
                Image(systemName: item.severity == "critical" ? "exclamationmark.octagon.fill" : "exclamationmark.triangle.fill")
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(severityColor)
                    .frame(width: 20)
                Text(item.title)
                    .font(VFont.secondary())
                    .foregroundColor(PdigV2Colors.textPrimary)
                    .multilineTextAlignment(.leading)
                    .frame(maxWidth: .infinity, alignment: .leading)
                Image(systemName: "chevron.right")
                    .font(.system(size: 12))
                    .foregroundColor(PdigV2Colors.textMuted)
            }
            .padding(.horizontal, VSpace.lg)
            .frame(minHeight: 44)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .background(item.severity == "critical" ? PdigV2Colors.critical.opacity(0.12) : PdigV2Colors.surface)
        .clipShape(RoundedRectangle(cornerRadius: VRadius.md, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: VRadius.md, style: .continuous)
                .stroke(
                    item.severity == "critical" ? PdigV2Colors.critical.opacity(0.4) : PdigV2Colors.borderSubtle,
                    lineWidth: 1
                )
        )
        .accessibilityElement(children: .combine)
        .accessibilityLabel(item.title)
        .accessibilityHint(vStatusLabel(item.severity))
    }
}

/// RegionListItem：Globe 的非视觉替代（键盘/读屏/触屏均可操作；IA.md §7）。
struct VRegionListItem: View {
    let region: RegionPresentation
    let selected: Bool
    let onClick: () -> Void

    var body: some View {
        Button(action: onClick) {
            HStack(spacing: VSpace.lg) {
                Circle()
                    .fill(region.attentionCount > 0 ? PdigV2Colors.warning : PdigV2Colors.primaryBright)
                    .frame(width: 8, height: 8)
                VStack(alignment: .leading, spacing: 2) {
                    Text(region.displayName)
                        .font(VFont.body())
                        .fontWeight(selected ? .semibold : .regular)
                        .foregroundColor(PdigV2Colors.textPrimary)
                    Text("\(region.cardCount) 张卡 · \(region.phoneCount) 个号码 · \(region.serviceCount) 项服务")
                        .font(VFont.meta())
                        .foregroundColor(PdigV2Colors.textSecondary)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                if region.attentionCount > 0 {
                    Text("\(region.attentionCount)")
                        .font(VFont.meta())
                        .fontWeight(.semibold)
                        .foregroundColor(PdigV2Colors.warning)
                        .padding(.horizontal, VSpace.sm)
                        .padding(.vertical, 2)
                        .background(PdigV2Colors.warning.opacity(0.18))
                        .clipShape(RoundedRectangle(cornerRadius: VRadius.sm, style: .continuous))
                }
            }
            .padding(.horizontal, VSpace.lg)
            .frame(minHeight: 44)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .background(selected ? PdigV2Colors.primary.opacity(0.18) : PdigV2Colors.surface)
        .clipShape(RoundedRectangle(cornerRadius: VRadius.md, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: VRadius.md, style: .continuous)
                .stroke(selected ? PdigV2Colors.primaryBright : PdigV2Colors.borderSubtle, lineWidth: 1)
        )
        .accessibilityElement(children: .combine)
        .accessibilityLabel(VCopy.regionTooltip(region))
        .accessibilityAddTraits(selected ? .isSelected : [])
    }
}

/// 隐私遮蔽 indicator（top bar 内；全局 Privacy Mask 开关提示）。
struct VMaskIndicator: View {
    let enabled: Bool

    var body: some View {
        HStack(spacing: 4) {
            Image(systemName: "lock.fill")
                .font(.system(size: 11))
                .foregroundColor(PdigV2Colors.textMuted)
            Text(enabled ? "隐私遮蔽已开启" : "隐私遮蔽已关闭")
                .font(VFont.meta())
                .foregroundColor(PdigV2Colors.textSecondary)
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel(enabled ? "隐私遮蔽已开启" : "隐私遮蔽已关闭")
    }
}

/// 卡片/号码类型 → 中文（asset-card / phone-card 建议文案）。
func vCardTypeLabel(_ type: String) -> String {
    type == "credit" ? VCopy.credit : VCopy.debit
}

func vCardFormLabel(_ form: String) -> String {
    form == "virtual" ? VCopy.virtual : VCopy.physical
}

func vSimLabel(_ simKind: String) -> String {
    simKind == "eSIM" ? VCopy.simEsim : VCopy.simPhysical
}

func vRoleLabel(_ role: String) -> String {
    switch role {
    case "primary": return VCopy.rolePrimary
    case "secondary": return VCopy.roleSecondary
    case "keep": return VCopy.roleKeep
    default: return role
    }
}

func vRelationKindLabel(_ kind: String) -> String {
    switch kind {
    case "funding": return "资金来源"
    case "authenticates": return "登录验证"
    case "twoFA": return "2FA 验证"
    default: return "注册使用"
    }
}
