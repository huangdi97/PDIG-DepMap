// PersonalizationView —— 个性化中心（/settings/personalization）。
//
// personalization-center.md 契约：
//  - 10 组设置：workspace theme / globe theme / nav density / card defaults / number defaults /
//    privacy masking / home modules / region grouping / motion / reduced effects；
//  - P0 critical action 不可隐藏（首页「需要你处理」锁定说明）；
//  - Reduce Motion 联动四项；所有设置为本地偏好，不写 .depmap。
//  testId：pdig.settings.personalization.*。

import SwiftUI

struct PersonalizationView: View {
    @ObservedObject var model: VNextModel

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: VSpace.sectionGap) {
                Text(VCopy.personalizationTitle)
                    .font(VFont.pageTitle())
                    .foregroundColor(PdigV2Colors.textPrimary)
                Text(VCopy.personalizationNote)
                    .font(VFont.secondary())
                    .foregroundColor(PdigV2Colors.textSecondary)
                    .accessibilityIdentifier(VTestIds.settingsPersonalization)

                // workspace theme
                VSectionHeader(title: VCopy.workspaceTheme)
                toggleRow(VCopy.workspaceTheme, VCopy.workspaceThemeValue, enabled: true)

                // globe theme
                VSectionHeader(title: VCopy.globeTheme)
                toggleRow(VCopy.idleRotation, model.reduceMotion ? "已关闭（减少动效）" : "开启（交互后暂停）", enabled: !model.reduceMotion)
                toggleRow(VCopy.arcAnimation, model.reduceMotion ? "静态" : "620ms 平滑", enabled: !model.reduceMotion)

                // nav density
                VSectionHeader(title: VCopy.navDensity)
                toggleRow("导航栏展开宽度", "iPhone：底部导航 ≤5 项", enabled: true)

                // card / number defaults
                VSectionHeader(title: VCopy.cardDefaults)
                toggleRow(VCopy.cardDefaults, "沿用各卡当前 preset，可在卡面定制中修改", enabled: false)
                VSectionHeader(title: VCopy.numberDefaults)
                toggleRow(VCopy.numberDefaults, "Country / 可逐号码定制", enabled: false)

                // privacy masking
                VSectionHeader(title: VCopy.privacyMask)
                toggleRow(VCopy.privacyMask, model.privacyMask ? "已开启（截图/演示/公共场合推荐）" : "已关闭", enabled: model.privacyMask) {
                    model.privacyMask.toggle()
                }

                // home modules（P0 不可隐藏）
                VSectionHeader(title: VCopy.homeModules)
                ForEach(["需要你处理", "进行中的变更", "即将到来", "地区快捷访问"], id: \.self) { module in
                    HStack {
                        Text(module)
                            .font(VFont.secondary())
                            .foregroundColor(PdigV2Colors.textSecondary)
                        Spacer()
                        Text("显示 · 可上移/下移")
                            .font(VFont.meta())
                            .foregroundColor(PdigV2Colors.textMuted)
                    }
                    .padding(VSpace.md)
                    .frame(minHeight: 44)
                    .background(PdigV2Colors.surfaceRaised)
                    .clipShape(RoundedRectangle(cornerRadius: VRadius.md, style: .continuous))
                }
                HStack(spacing: VSpace.sm) {
                    Image(systemName: "lock.fill")
                        .font(.system(size: 11))
                        .foregroundColor(PdigV2Colors.textMuted)
                    Text("\(VCopy.p0Protected)：首页「需要你处理」在存在必须处理事项时不可隐藏。")
                        .font(VFont.meta())
                        .foregroundColor(PdigV2Colors.textMuted)
                }

                // region grouping
                VSectionHeader(title: VCopy.regionGrouping)
                toggleRow(VCopy.regionGrouping, "按国家 / 按区域组（中国大陆 / 港澳 / 欧洲 / 北美 / 东南亚 / 自定义）", enabled: false)

                // motion
                VSectionHeader(title: VCopy.motion)
                toggleRow(VCopy.reducedEffects, model.reduceMotion ? "已开启" : "关闭", enabled: model.reduceMotion) {
                    model.reduceMotion.toggle()
                }

                // P0 说明（三通道外提示）
                Text(VCopy.p0Note)
                    .font(VFont.secondary())
                    .foregroundColor(PdigV2Colors.textPrimary)
                    .padding(VSpace.lg)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(PdigV2Colors.critical.opacity(0.1))
                    .clipShape(RoundedRectangle(cornerRadius: VRadius.md, style: .continuous))
            }
            .padding(VSpace.pagePadding)
        }
        .vPageBackground()
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                VBackButton { model.back() }
            }
        }
            #if os(iOS)
            .navigationBarBackButtonHidden(true)
            #endif
    }

    private func toggleRow(_ label: String, _ value: String, enabled: Bool, onToggle: (() -> Void)? = nil) -> some View {
        HStack(spacing: VSpace.md) {
            VStack(alignment: .leading, spacing: 2) {
                Text(label)
                    .font(VFont.secondary())
                    .fontWeight(.medium)
                    .foregroundColor(PdigV2Colors.textPrimary)
                Text(value)
                    .font(VFont.meta())
                    .foregroundColor(PdigV2Colors.textMuted)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            Text(enabled ? "已开启" : "已关闭")
                .font(VFont.meta())
                .fontWeight(.semibold)
                .foregroundColor(enabled ? PdigV2Colors.primaryBright : PdigV2Colors.textMuted)
                .padding(.horizontal, VSpace.sm)
                .frame(minHeight: 44)
                .background(enabled ? PdigV2Colors.primary.opacity(0.3) : PdigV2Colors.surface.opacity(0.5))
                .clipShape(RoundedRectangle(cornerRadius: VRadius.sm, style: .continuous))
            if let onToggle = onToggle {
                Button(action: onToggle) {
                    Text("点按切换")
                        .font(VFont.meta())
                        .fontWeight(.semibold)
                        .foregroundColor(PdigV2Colors.primaryBright)
                        .frame(minHeight: 44)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel("\(label)，当前\(enabled ? "已开启" : "已关闭")，点按切换")
            }
        }
        .padding(.horizontal, VSpace.md)
        .padding(.vertical, VSpace.sm)
        .frame(minHeight: 48)
        .background(PdigV2Colors.surfaceRaised)
        .clipShape(RoundedRectangle(cornerRadius: VRadius.md, style: .continuous))
        .accessibilityElement(children: .contain)
        .accessibilityIdentifier("\(VTestIds.settingsPersonalization).\(label)")
    }
}
