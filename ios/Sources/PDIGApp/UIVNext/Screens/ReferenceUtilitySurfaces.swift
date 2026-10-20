// ReferenceUtilitySurfaces —— iOS vNext reference Sources + Settings.
//
// These screens are Presentation/reference surfaces only. They consume the
// synthetic reference fixture and VNextModel presentation state; they do not
// read/write Production Reality or portable .depmap payloads.

import SwiftUI

struct VReferenceSourcesView: View {
    @ObservedObject var model: VNextModel

    private let metrics: [(String, Int)] = [
        ("卡片", VNextDemoFixture.cards.count),
        ("号码", VNextDemoFixture.numbers.count),
        ("账户", VNextDemoFixture.secondaryAssets(for: .account).count),
        ("邮箱", VNextDemoFixture.secondaryAssets(for: .email).count),
        ("设备", VNextDemoFixture.secondaryAssets(for: .device).count),
        ("服务", VNextDemoFixture.secondaryAssets(for: .service).count),
    ]

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: VSpace.sectionGap) {
                header
                boundaryHero

                VSectionHeader(title: "当前工作区")
                workspaceCard

                LazyVGrid(
                    columns: [GridItem(.adaptive(minimum: 96), spacing: VSpace.sm)],
                    spacing: VSpace.sm
                ) {
                    ForEach(metrics, id: \.0) { metric in
                        sourceMetric(metric.1, label: metric.0)
                    }
                }

                VSectionHeader(title: "来源实例")
                VStack(alignment: .leading, spacing: 5) {
                    Text("Reference fixture 未记录真实来源实例")
                        .font(VFont.body())
                        .fontWeight(.semibold)
                        .foregroundColor(PdigV2Colors.textPrimary)
                    Text("这里的对象只用于平台翻译与交互验证；没有来源记录不代表真实 Personal Reality 中不存在来源。")
                        .font(VFont.meta())
                        .foregroundColor(PdigV2Colors.unknown)
                }
                .padding(VSpace.lg)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(PdigV2Colors.surfaceRaised)
                .clipShape(RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous))

                VSectionHeader(title: "事实边界")
                boundaryRow("已确认", "只有已经记录并确认的信息才能进入依赖与连续性分析。")
                boundaryRow("未知", "没有记录的信息保持未知；unknown != safe。")
                boundaryRow("外观", "卡面、号码面和本机偏好只改变显示，不改变事实。")

                VSectionHeader(title: "快速查看")
                routeRow("基础设施总览", "查看地区、对象与当前关注项", id: VTestIds.navInfraOverview) {
                    model.navigate(.overview)
                }
                routeRow("卡片", "查看支付基础设施 reference", id: VTestIds.navInfraCards) {
                    model.navigate(.cards)
                }
                routeRow("号码", "查看通信身份 reference", id: VTestIds.navInfraNumbers) {
                    model.navigate(.numbers)
                }
                routeRow("薄弱点", "查看连续性 findings reference", id: VTestIds.navInfraWeaknesses) {
                    model.navigate(.weaknesses)
                }
            }
            .padding(VSpace.pagePadding)
        }
        .vPageBackground()
        .accessibilityIdentifier("pdig.ios.reference.sources")
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                VBackButton { model.back() }
            }
        }
        #if os(iOS)
        .navigationBarBackButtonHidden(true)
        #endif
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(VCopy.navSources)
                .font(VFont.pageTitle())
                .foregroundColor(PdigV2Colors.textPrimary)
            Text("当前是隔离的 Reference Preview，不代表真实账户同步状态；没有来源的数据保持未知。")
                .font(VFont.secondary())
                .foregroundColor(PdigV2Colors.textSecondary)
        }
    }

    private var boundaryHero: some View {
        LazyVGrid(
            columns: [GridItem(.adaptive(minimum: 96), spacing: VSpace.sm)],
            spacing: VSpace.sm
        ) {
            boundaryPillar("本机优先", "Reference 状态留在当前会话")
            boundaryPillar("已确认", "才进入依赖分析")
            boundaryPillar("未知", "绝不自动推断为安全")
        }
        .padding(VSpace.lg)
        .background(PdigV2Colors.primarySoft)
        .clipShape(RoundedRectangle(cornerRadius: VRadius.xl, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: VRadius.xl, style: .continuous)
                .stroke(PdigV2Colors.primary.opacity(0.18), lineWidth: 1)
        )
    }

    private var workspaceCard: some View {
        VStack(alignment: .leading, spacing: VSpace.md) {
            HStack {
                VStack(alignment: .leading, spacing: 3) {
                    Text("预览参考数据")
                        .font(VFont.body())
                        .fontWeight(.semibold)
                        .foregroundColor(PdigV2Colors.textPrimary)
                    Text("synthetic fixture · 不读取真实用户账户")
                        .font(VFont.meta())
                        .foregroundColor(PdigV2Colors.textMuted)
                }
                Spacer()
                Text("参考")
                    .font(VFont.meta())
                    .fontWeight(.semibold)
                    .foregroundColor(PdigV2Colors.primaryBright)
                    .padding(.horizontal, VSpace.sm)
                    .padding(.vertical, 5)
                    .background(PdigV2Colors.primarySoft)
                    .clipShape(Capsule())
            }
            Text("对象数量来自当前 iOS reference fixture；这些数量不是生产数据覆盖率，也不是安全结论。")
                .font(VFont.meta())
                .foregroundColor(PdigV2Colors.unknown)
        }
        .padding(VSpace.lg)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(PdigV2Colors.surface)
        .clipShape(RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous)
                .stroke(PdigV2Colors.borderSubtle, lineWidth: 1)
        )
    }

    private func boundaryPillar(_ title: String, _ body: String) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(title)
                .font(VFont.secondary())
                .fontWeight(.semibold)
                .foregroundColor(PdigV2Colors.primaryBright)
            Text(body)
                .font(VFont.meta())
                .foregroundColor(PdigV2Colors.textSecondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func sourceMetric(_ value: Int, label: String) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(String(value))
                .font(.system(size: 20, weight: .bold))
                .foregroundColor(PdigV2Colors.textPrimary)
            Text(label)
                .font(VFont.meta())
                .foregroundColor(PdigV2Colors.textMuted)
        }
        .padding(VSpace.md)
        .frame(maxWidth: .infinity, minHeight: 62, alignment: .leading)
        .background(PdigV2Colors.surfaceRaised)
        .clipShape(RoundedRectangle(cornerRadius: VRadius.md, style: .continuous))
    }

    private func boundaryRow(_ title: String, _ body: String) -> some View {
        VStack(alignment: .leading, spacing: 3) {
            Text(title)
                .font(VFont.secondary())
                .fontWeight(.semibold)
                .foregroundColor(PdigV2Colors.textPrimary)
            Text(body)
                .font(VFont.meta())
                .foregroundColor(PdigV2Colors.textMuted)
        }
        .padding(VSpace.lg)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(PdigV2Colors.surfaceRaised)
        .clipShape(RoundedRectangle(cornerRadius: VRadius.md, style: .continuous))
    }

    private func routeRow(
        _ title: String,
        _ body: String,
        id: String,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            HStack(spacing: VSpace.md) {
                VStack(alignment: .leading, spacing: 3) {
                    Text(title)
                        .font(VFont.body())
                        .fontWeight(.medium)
                        .foregroundColor(PdigV2Colors.textPrimary)
                    Text(body)
                        .font(VFont.meta())
                        .foregroundColor(PdigV2Colors.textMuted)
                }
                Spacer()
                Text("查看 →")
                    .font(VFont.meta())
                    .fontWeight(.semibold)
                    .foregroundColor(PdigV2Colors.primaryBright)
            }
            .padding(VSpace.lg)
            .frame(minHeight: 48)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .background(PdigV2Colors.surfaceRaised)
        .clipShape(RoundedRectangle(cornerRadius: VRadius.md, style: .continuous))
        .accessibilityIdentifier(id)
    }
}

struct VReferenceSettingsView: View {
    @ObservedObject var model: VNextModel

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: VSpace.sectionGap) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("隐私与偏好")
                        .font(VFont.pageTitle())
                        .foregroundColor(PdigV2Colors.textPrimary)
                    Text("这些设置只改变当前 iOS Reference Preview 的本机呈现，不修改基础设施事实。")
                        .font(VFont.secondary())
                        .foregroundColor(PdigV2Colors.textSecondary)
                }

                VSectionHeader(title: "隐私")
                settingToggle(
                    title: "敏感信息遮蔽",
                    body: "隐藏卡片、号码、关系与变更中的可识别文案；不会改写对象身份。",
                    isOn: $model.privacyMask,
                    id: "pdig.ios.reference.setting.privacy"
                )

                VSectionHeader(title: "动效")
                settingToggle(
                    title: "减少动态效果",
                    body: "降低 Globe 与界面动效强度；不会改变数据或计划状态。",
                    isOn: $model.reduceMotion,
                    id: "pdig.ios.reference.setting.motion"
                )

                VSectionHeader(title: "现在")
                settingToggle(
                    title: "显示即将到来",
                    body: "控制 Now 是否显示未来维护事项；隐藏不等于事项不存在。",
                    isOn: $model.showUpcoming,
                    id: "pdig.ios.reference.setting.upcoming"
                )

                VSectionHeader(title: "更多")
                routeRow("个性化", "查看工作区、资产外观与展示选项", id: VTestIds.settingsPersonalization) {
                    model.navigate(.personalization)
                }
                routeRow("数据源", "查看 reference 覆盖范围与事实边界", id: VTestIds.navSources) {
                    model.navigate(.sources)
                }

                Text("当前 reference 会话尚未接入持久化 PresentationProfile；因此这里不承诺跨启动保存。Production 接线前不会把临时状态冒充“已保存”。")
                    .font(VFont.meta())
                    .foregroundColor(PdigV2Colors.unknown)
                    .padding(VSpace.lg)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(PdigV2Colors.primarySoft)
                    .clipShape(RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous))
            }
            .padding(VSpace.pagePadding)
        }
        .vPageBackground()
        .accessibilityIdentifier("pdig.ios.reference.settings")
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                VBackButton { model.back() }
            }
        }
        #if os(iOS)
        .navigationBarBackButtonHidden(true)
        #endif
    }

    private func settingToggle(
        title: String,
        body: String,
        isOn: Binding<Bool>,
        id: String
    ) -> some View {
        HStack(spacing: VSpace.md) {
            VStack(alignment: .leading, spacing: 3) {
                Text(title)
                    .font(VFont.body())
                    .fontWeight(.semibold)
                    .foregroundColor(PdigV2Colors.textPrimary)
                Text(body)
                    .font(VFont.meta())
                    .foregroundColor(PdigV2Colors.textMuted)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            Toggle("", isOn: isOn)
                .labelsHidden()
                .accessibilityIdentifier(id)
        }
        .padding(VSpace.lg)
        .frame(minHeight: 56)
        .background(PdigV2Colors.surfaceRaised)
        .clipShape(RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous)
                .stroke(PdigV2Colors.borderSubtle, lineWidth: 1)
        )
    }

    private func routeRow(
        _ title: String,
        _ body: String,
        id: String,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            HStack(spacing: VSpace.md) {
                VStack(alignment: .leading, spacing: 3) {
                    Text(title)
                        .font(VFont.body())
                        .fontWeight(.medium)
                        .foregroundColor(PdigV2Colors.textPrimary)
                    Text(body)
                        .font(VFont.meta())
                        .foregroundColor(PdigV2Colors.textMuted)
                }
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundColor(PdigV2Colors.textMuted)
            }
            .padding(VSpace.lg)
            .frame(minHeight: 48)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .background(PdigV2Colors.surfaceRaised)
        .clipShape(RoundedRectangle(cornerRadius: VRadius.md, style: .continuous))
        .accessibilityIdentifier(id)
    }
}
