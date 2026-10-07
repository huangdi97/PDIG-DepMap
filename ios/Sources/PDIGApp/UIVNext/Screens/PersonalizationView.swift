// PersonalizationView —— iOS-native local presentation preferences.
// Preferences affect only UI presentation. They never enter PersonalReality, Canonical or .depmap.

import SwiftUI

struct PersonalizationView: View {
    @ObservedObject var model: VNextModel
    @Environment(\.horizontalSizeClass) private var sizeClass
    @Environment(\.accessibilityReduceMotion) private var systemReduceMotion

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: VSpace.lg) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("个性化")
                        .font(VFont.pageTitle())
                        .foregroundColor(PdigV2Colors.textPrimary)
                    Text("这些设置只改变你看到的界面，不会修改卡片、号码、依赖关系或变更记录。")
                        .font(VFont.secondary())
                        .foregroundColor(PdigV2Colors.textSecondary)
                }
                .accessibilityIdentifier(VTestIds.settingsPersonalization)

                VSectionHeader(title: "外观")
                appearancePreview
                VPreferenceAction(title: "卡片外观", subtitle: "在每张卡片详情中单独定制", icon: "creditcard") {
                    model.navigate(.cards)
                }
                VPreferenceAction(title: "号码外观", subtitle: "在号码详情中单独定制", icon: "phone") {
                    model.navigate(.numbers)
                }

                VSectionHeader(title: "隐私")
                VPreferenceToggle(
                    title: "隐藏敏感信息",
                    subtitle: model.privacyMask ? "卡号、号码等敏感字段保持遮罩" : "允许展示已记录的非遮罩字段",
                    isOn: Binding(get: { model.privacyMask }, set: { model.privacyMask = $0 })
                )

                VSectionHeader(title: "动效")
                if systemReduceMotion {
                    VPreferenceLocked(
                        title: "系统已开启“减弱动态效果”",
                        subtitle: "PDIG 会遵循系统设置：停止地球自动旋转并简化大幅空间动画。",
                        icon: "figure.walk.motion"
                    )
                }
                VPreferenceToggle(
                    title: "在 PDIG 内减弱动态效果",
                    subtitle: model.effectiveReduceMotion ? "地球自动旋转关闭，聚焦和抽屉动画简化" : "使用标准、短且可中断的动效",
                    isOn: Binding(get: { model.reduceMotion }, set: { model.reduceMotion = $0 })
                )

                VSectionHeader(title: "首页")
                VPreferenceLocked(
                    title: "需要你处理",
                    subtitle: "存在必须处理事项时始终显示，不能被个性化隐藏。",
                    icon: "lock.fill"
                )
                VPreferenceLocked(
                    title: "进行中的变更",
                    subtitle: "有正在执行的变更计划时始终显示当前阶段。",
                    icon: "arrow.triangle.2.circlepath"
                )
                VPreferenceToggle(
                    title: "即将到来",
                    subtitle: model.showUpcoming ? "显示已知的到期日与时间节点" : "已从“现在”隐藏，可随时恢复",
                    isOn: Binding(get: { model.showUpcoming }, set: { model.showUpcoming = $0 })
                )

                VSectionHeader(title: "地区")
                VPreferenceValue(title: "地区分组", value: "按国家 / 地理区域", subtitle: "中国大陆 · 港澳 · 欧洲 · 北美 · 东南亚")

                VSectionHeader(title: "卡片默认")
                VPreferenceValue(title: "默认卡面", value: "按卡片当前主题", subtitle: "每张卡可以在详情页覆盖；只影响本机显示")

                VSectionHeader(title: "号码默认")
                VPreferenceValue(title: "默认号码面", value: "国家 / 地区", subtitle: "号码角色、运营商和恢复状态仍来自事实数据")

                if sizeClass == .regular {
                    VSectionHeader(title: "平板导航")
                    VPreferenceValue(title: "导航布局", value: "侧栏 + 内容上下文", subtitle: "四个一级入口保持稳定；基础设施八分类位于内容区")
                }

                VSectionHeader(title: "数据与来源")
                VPreferenceAction(title: "数据源", subtitle: "查看当前工作区覆盖范围与事实边界", icon: "externaldrive") {
                    model.navigate(.sources)
                }

                Text("重要事项不会因为个性化设置而被隐藏。未记录的信息仍保持未知。")
                    .font(VFont.meta())
                    .foregroundColor(PdigV2Colors.textSecondary)
                    .padding(VSpace.lg)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(PdigV2Colors.primarySoft.opacity(0.74))
                    .clipShape(RoundedRectangle(cornerRadius: VRadius.md))
                    .overlay(RoundedRectangle(cornerRadius: VRadius.md).stroke(PdigV2Colors.primary.opacity(0.18), lineWidth: 1))
            }
            .padding(VSpace.pagePadding)
        }
        .vPageBackground()
        .toolbar {
            ToolbarItem(placement: .cancellationAction) { VBackButton { model.back() } }
        }
        #if os(iOS)
        .navigationBarBackButtonHidden(true)
        #endif
    }

    private var appearancePreview: some View {
        VStack(alignment: .leading, spacing: VSpace.md) {
            HStack {
                VStack(alignment: .leading, spacing: 3) {
                    Text("亮色 · 当前视觉方向")
                        .font(VFont.body()).fontWeight(.semibold)
                        .foregroundColor(PdigV2Colors.textPrimary)
                    Text("浅色空间层级 · 深色地球 · 资产身份优先")
                        .font(VFont.meta()).foregroundColor(PdigV2Colors.textMuted)
                }
                Spacer()
                VChip(text: "当前", highlight: true)
            }

            HStack(spacing: VSpace.sm) {
                VAppearanceTile(title: "界面", value: "亮色", background: PdigV2Colors.surfaceRaised, dark: false)
                VAppearanceTile(title: "地球", value: model.effectiveReduceMotion ? "深色 · 静态" : "深色 · 动态", background: PdigV2Colors.globeDeep, dark: true)
                VAppearanceTile(title: "资产", value: "卡片 / 号码", background: PdigV2Colors.primarySoft, dark: false)
            }
        }
        .padding(VSpace.lg)
        .background(PdigV2Colors.surface)
        .clipShape(RoundedRectangle(cornerRadius: VRadius.xl))
        .overlay(RoundedRectangle(cornerRadius: VRadius.xl).stroke(PdigV2Colors.borderSubtle, lineWidth: 1))
        .accessibilityIdentifier("pdig.personalization.visual-preview")
    }
}

private struct VAppearanceTile: View {
    let title: String
    let value: String
    let background: Color
    let dark: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 3) {
            Text(title)
                .font(.system(size: 10))
                .foregroundColor(dark ? PdigV2Colors.globeTextSecondary : PdigV2Colors.textMuted)
            Text(value)
                .font(VFont.meta()).fontWeight(.semibold)
                .foregroundColor(dark ? PdigV2Colors.globeTextPrimary : PdigV2Colors.textPrimary)
        }
        .padding(VSpace.md)
        .frame(maxWidth: .infinity, minHeight: 66, alignment: .leading)
        .background(background)
        .clipShape(RoundedRectangle(cornerRadius: VRadius.md))
        .overlay(RoundedRectangle(cornerRadius: VRadius.md).stroke(PdigV2Colors.borderSubtle, lineWidth: 1))
    }
}

private struct VPreferenceToggle: View {
    let title: String
    let subtitle: String
    @Binding var isOn: Bool

    var body: some View {
        Toggle(isOn: $isOn) {
            VStack(alignment: .leading, spacing: 3) {
                Text(title).font(VFont.secondary()).fontWeight(.semibold).foregroundColor(PdigV2Colors.textPrimary)
                Text(subtitle).font(VFont.meta()).foregroundColor(PdigV2Colors.textMuted)
            }
        }
        .tint(PdigV2Colors.primary)
        .padding(VSpace.md)
        .frame(minHeight: 52)
        .background(PdigV2Colors.surface)
        .clipShape(RoundedRectangle(cornerRadius: VRadius.md))
        .overlay(RoundedRectangle(cornerRadius: VRadius.md).stroke(PdigV2Colors.borderSubtle, lineWidth: 1))
        .accessibilityIdentifier("\(VTestIds.settingsPersonalization).\(title)")
    }
}

private struct VPreferenceLocked: View {
    let title: String
    let subtitle: String
    let icon: String

    var body: some View {
        HStack(alignment: .top, spacing: VSpace.md) {
            Image(systemName: icon)
                .foregroundColor(PdigV2Colors.textMuted)
                .frame(width: 26, height: 26)
            VStack(alignment: .leading, spacing: 3) {
                Text(title).font(VFont.secondary()).fontWeight(.semibold).foregroundColor(PdigV2Colors.textPrimary)
                Text(subtitle).font(VFont.meta()).foregroundColor(PdigV2Colors.textMuted)
            }
            Spacer()
            Text("固定").font(VFont.meta()).foregroundColor(PdigV2Colors.textMuted)
        }
        .padding(VSpace.md)
        .frame(minHeight: 52)
        .background(PdigV2Colors.surfaceRaised)
        .clipShape(RoundedRectangle(cornerRadius: VRadius.md))
    }
}

private struct VPreferenceValue: View {
    let title: String
    let value: String
    let subtitle: String

    var body: some View {
        HStack(alignment: .top, spacing: VSpace.md) {
            VStack(alignment: .leading, spacing: 3) {
                Text(title).font(VFont.secondary()).fontWeight(.semibold).foregroundColor(PdigV2Colors.textPrimary)
                Text(subtitle).font(VFont.meta()).foregroundColor(PdigV2Colors.textMuted)
            }
            Spacer()
            Text(value).font(VFont.meta()).fontWeight(.semibold).foregroundColor(PdigV2Colors.primaryText)
        }
        .padding(VSpace.md)
        .frame(minHeight: 52)
        .background(PdigV2Colors.surface)
        .clipShape(RoundedRectangle(cornerRadius: VRadius.md))
        .overlay(RoundedRectangle(cornerRadius: VRadius.md).stroke(PdigV2Colors.borderSubtle, lineWidth: 1))
    }
}

private struct VPreferenceAction: View {
    let title: String
    let subtitle: String
    let icon: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: VSpace.md) {
                Image(systemName: icon)
                    .foregroundColor(PdigV2Colors.primaryText)
                    .frame(width: 34, height: 34)
                    .background(PdigV2Colors.primarySoft)
                    .clipShape(RoundedRectangle(cornerRadius: VRadius.md))
                VStack(alignment: .leading, spacing: 3) {
                    Text(title).font(VFont.secondary()).fontWeight(.semibold).foregroundColor(PdigV2Colors.textPrimary)
                    Text(subtitle).font(VFont.meta()).foregroundColor(PdigV2Colors.textMuted)
                }
                Spacer()
                Image(systemName: "chevron.right").font(.system(size: 11, weight: .semibold)).foregroundColor(PdigV2Colors.textMuted)
            }
            .padding(VSpace.md)
            .frame(minHeight: 52)
        }
        .buttonStyle(.plain)
        .background(PdigV2Colors.surface)
        .clipShape(RoundedRectangle(cornerRadius: VRadius.md))
        .overlay(RoundedRectangle(cornerRadius: VRadius.md).stroke(PdigV2Colors.borderSubtle, lineWidth: 1))
    }
}
