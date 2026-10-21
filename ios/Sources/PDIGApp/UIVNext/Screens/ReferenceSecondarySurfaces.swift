import SwiftUI

/// iOS vNext reference translation for the secondary infrastructure surfaces.
///
/// This remains reference/synthetic UI. It intentionally does not claim
/// Production Reality authority; Production wiring is a separate gate.
func vSecondaryCollectionColumnCount(isRegularWidth: Bool) -> Int {
    isRegularWidth ? 2 : 1
}

func vFilteredSecondaryAssets(
    _ assets: [VSecondaryAsset],
    kind: VSecondaryKind,
    regionFilter: String?
) -> [VSecondaryAsset] {
    assets.filter { item in
        item.kind == kind && (regionFilter == nil || item.region == regionFilter)
    }
}

struct VSecondaryCollectionView: View {
    @ObservedObject var model: VNextModel
    let kind: VSecondaryKind
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass

    private var items: [VSecondaryAsset] {
        vFilteredSecondaryAssets(
            VNextDemoFixture.secondaryAssets,
            kind: kind,
            regionFilter: model.regionFilter
        )
    }

    private var scopeName: String {
        guard let code = model.regionFilter else { return "全球" }
        return VNextDemoFixture.regionSummaries()
            .first { $0.regionCode == code }?.displayName ?? code
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: VSpace.sectionGap) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(kind.title)
                        .font(VFont.pageTitle())
                        .foregroundColor(PdigV2Colors.textPrimary)
                    Text("\(scopeName) · 已记录的数字基础设施对象 · 参考数据")
                        .font(VFont.meta())
                        .foregroundColor(PdigV2Colors.textMuted)
                }

                HStack {
                    Text("\(items.count)")
                        .font(.system(size: 28, weight: .bold))
                        .foregroundColor(PdigV2Colors.primaryBright)
                    Text("个已记录对象")
                        .font(VFont.secondary())
                        .foregroundColor(PdigV2Colors.textSecondary)
                    Spacer()
                    if model.regionFilter != nil {
                        Button {
                            model.regionFilter = nil
                        } label: {
                            Text("查看全球")
                                .font(VFont.meta())
                                .fontWeight(.semibold)
                                .foregroundColor(PdigV2Colors.primaryBright)
                                .padding(.horizontal, VSpace.md)
                                .padding(.vertical, VSpace.sm)
                                .background(PdigV2Colors.primarySoft)
                                .clipShape(Capsule())
                        }
                        .buttonStyle(.plain)
                        .accessibilityIdentifier(
                            "pdig.ios.reference.secondary.scope.global.\(kind.route)"
                        )
                    } else {
                        VChip(text: "全球", highlight: true)
                    }
                }
                .padding(VSpace.lg)
                .background(PdigV2Colors.surface)
                .clipShape(RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous))

                if items.isEmpty {
                    VStack(alignment: .leading, spacing: VSpace.md) {
                        Text("当前地区没有\(kind.title)记录")
                            .font(VFont.body())
                            .fontWeight(.semibold)
                            .foregroundColor(PdigV2Colors.textPrimary)
                        Text("没有记录不代表不存在，也不代表没有恢复或控制关系。")
                            .font(VFont.meta())
                            .foregroundColor(PdigV2Colors.unknown)
                        if model.regionFilter != nil {
                            Button("查看全球") {
                                model.regionFilter = nil
                            }
                            .buttonStyle(.bordered)
                            .accessibilityIdentifier(
                                "pdig.ios.reference.secondary.empty.global.\(kind.route)"
                            )
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(VSpace.xl)
                    .background(PdigV2Colors.surface)
                    .clipShape(RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous))
                }

                let columnCount = vSecondaryCollectionColumnCount(
                    isRegularWidth: horizontalSizeClass == .regular
                )
                if !items.isEmpty {
                    let columns = Array(
                        repeating: GridItem(.flexible(), spacing: VSpace.md, alignment: .top),
                        count: columnCount
                    )
                    LazyVGrid(columns: columns, alignment: .leading, spacing: VSpace.md) {
                    ForEach(items) { item in
                        Button {
                            model.navigate(.secondaryDetail(kind, item.id))
                        } label: {
                            HStack(alignment: .top, spacing: VSpace.lg) {
                                Image(systemName: kind.glyph)
                                    .font(.system(size: 18, weight: .semibold))
                                    .foregroundColor(PdigV2Colors.primaryBright)
                                    .frame(width: 42, height: 42)
                                    .background(PdigV2Colors.primarySoft)
                                    .clipShape(RoundedRectangle(cornerRadius: VRadius.md, style: .continuous))

                                VStack(alignment: .leading, spacing: 3) {
                                    Text(model.privacyMask ? "\(kind.title)（已遮蔽）" : item.name)
                                        .font(VFont.body())
                                        .fontWeight(.semibold)
                                        .foregroundColor(PdigV2Colors.textPrimary)
                                        .lineLimit(1)
                                    Text(model.privacyMask ? "标识已遮蔽" : item.subtitle)
                                        .font(VFont.meta())
                                        .foregroundColor(PdigV2Colors.textSecondary)
                                        .lineLimit(2)
                                    Text("\(item.relationCount) 条已记录关系")
                                        .font(VFont.meta())
                                        .foregroundColor(PdigV2Colors.textMuted)
                                }
                                Spacer(minLength: VSpace.sm)
                                VStatusBadge(status: item.attention ? "warning" : "active")
                            }
                            .frame(maxWidth: .infinity, minHeight: 74, alignment: .topLeading)
                            .padding(VSpace.lg)
                            .contentShape(Rectangle())
                        }
                        .buttonStyle(.plain)
                        .background(PdigV2Colors.surface)
                        .clipShape(RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous))
                        .overlay(
                            RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous)
                                .stroke(PdigV2Colors.borderSubtle, lineWidth: 1)
                        )
                        .accessibilityIdentifier(
                            "pdig.ios.reference.secondary.item.\(kind.route).\(item.id)"
                        )
                    }
                    }
                    .accessibilityIdentifier("pdig.ios.reference.secondary.grid.\(kind.route)")
                    .accessibilityValue("\(columnCount) 列")
                }
            }
            .padding(VSpace.pagePadding)
        }
        .vPageBackground()
        .accessibilityIdentifier("pdig.ios.reference.secondary.\(kind.route)")
    }
}

struct VSecondaryDetailView: View {
    @ObservedObject var model: VNextModel
    let kind: VSecondaryKind
    let id: String

    private var item: VSecondaryAsset? {
        VNextDemoFixture.secondaryAsset(kind: kind, id: id)
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: VSpace.sectionGap) {
                HStack {
                    VBackButton { model.navigate(rootScreen) }
                    Text("\(kind.title)详情")
                        .font(VFont.sectionTitle())
                        .foregroundColor(PdigV2Colors.textPrimary)
                    Spacer()
                }

                if let item {
                    identityHero(item)
                    roleSummary(item)
                    impactLens(item)
                } else {
                    Text("对象不存在")
                        .font(VFont.body())
                        .foregroundColor(PdigV2Colors.textSecondary)
                }
            }
            .padding(VSpace.pagePadding)
        }
        .vPageBackground()
    }

    private var rootScreen: VScreen {
        switch kind {
        case .account: return .accounts
        case .email: return .emails
        case .device: return .devices
        case .service: return .services
        }
    }

    private func identityHero(_ item: VSecondaryAsset) -> some View {
        HStack(spacing: VSpace.lg) {
            Image(systemName: kind.glyph)
                .font(.system(size: 22, weight: .semibold))
                .foregroundColor(PdigV2Colors.primaryBright)
                .frame(width: 52, height: 52)
                .background(PdigV2Colors.primarySoft)
                .clipShape(RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous))

            VStack(alignment: .leading, spacing: 4) {
                Text(model.privacyMask ? "\(kind.title)（已遮蔽）" : item.name)
                    .font(VFont.sectionTitle())
                    .foregroundColor(PdigV2Colors.textPrimary)
                Text(model.privacyMask ? "标识已遮蔽" : item.subtitle)
                    .font(VFont.secondary())
                    .foregroundColor(PdigV2Colors.textSecondary)
                Text("\(item.relationCount) 条已记录关系")
                    .font(VFont.meta())
                    .foregroundColor(PdigV2Colors.textMuted)
            }
            Spacer()
            VChip(text: "已记录", highlight: false)
        }
        .padding(VSpace.xl)
        .background(PdigV2Colors.surface)
        .clipShape(RoundedRectangle(cornerRadius: VRadius.xl, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: VRadius.xl, style: .continuous)
                .stroke(PdigV2Colors.borderSubtle, lineWidth: 1)
        )
    }

    private func roleSummary(_ item: VSecondaryAsset) -> some View {
        VStack(alignment: .leading, spacing: VSpace.md) {
            VSectionHeader(title: "已记录角色")
            if item.roles.isEmpty {
                Text("未记录")
                    .font(VFont.secondary())
                    .foregroundColor(PdigV2Colors.unknown)
            } else {
                HStack(spacing: VSpace.sm) {
                    ForEach(item.roles, id: \.self) { VChip(text: $0) }
                }
            }
            Text("角色只来自参考数据；不会根据名称猜测安全能力或恢复能力。")
                .font(VFont.meta())
                .foregroundColor(PdigV2Colors.textMuted)
        }
        .padding(VSpace.lg)
        .background(PdigV2Colors.surface)
        .clipShape(RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous))
    }

    private func impactLens(_ item: VSecondaryAsset) -> some View {
        VStack(alignment: .leading, spacing: VSpace.md) {
            VSectionHeader(title: "如果它发生变化？")
            HStack(spacing: VSpace.gridGap) {
                metric("\(item.relationCount)", "已记录关系")
                metric("未知", "独立备用")
                metric("未知", "唯一恢复")
            }
            Text("未知不等于安全；关系数量也不等于独立恢复路径数量。")
                .font(VFont.meta())
                .foregroundColor(PdigV2Colors.unknown)
        }
        .padding(VSpace.lg)
        .background(PdigV2Colors.surface)
        .clipShape(RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous))
    }

    private func metric(_ value: String, _ label: String) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(value)
                .font(.system(size: 18, weight: .bold))
                .foregroundColor(PdigV2Colors.textPrimary)
            Text(label)
                .font(VFont.meta())
                .foregroundColor(PdigV2Colors.textMuted)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(VSpace.md)
        .background(PdigV2Colors.surfaceRaised)
        .clipShape(RoundedRectangle(cornerRadius: VRadius.md, style: .continuous))
    }
}

struct VRecordsView: View {
    var privacyMask: Bool = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: VSpace.sectionGap) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("记录")
                        .font(VFont.pageTitle())
                        .foregroundColor(PdigV2Colors.textPrimary)
                    Text("已发生、已记录完成与独立验证分开显示")
                        .font(VFont.meta())
                        .foregroundColor(PdigV2Colors.textMuted)
                }

                HStack(spacing: VSpace.gridGap) {
                    recordMetric("2", "已验证")
                    recordMetric("1", "待验证")
                    recordMetric("0", "验证失败")
                }

                if let first = VNextDemoFixture.records.first {
                    VSectionHeader(title: vVisibleRecordPlanTitle(first, privacyMask: privacyMask))
                }
                ForEach(VNextDemoFixture.records) { record in
                    VStack(alignment: .leading, spacing: VSpace.sm) {
                        HStack {
                            Text(vVisibleRecordActionTitle(record, privacyMask: privacyMask))
                                .font(VFont.body())
                                .fontWeight(.semibold)
                                .foregroundColor(PdigV2Colors.textPrimary)
                            Spacer()
                            VStatusBadge(status: record.state)
                        }
                        Text("记录于 \(record.recordedAt)")
                            .font(VFont.meta())
                            .foregroundColor(PdigV2Colors.textSecondary)
                        if let verifiedAt = record.verifiedAt {
                            Text("验证于 \(verifiedAt) · 验证证据 \(record.evidenceCount) 项")
                                .font(VFont.meta())
                                .foregroundColor(PdigV2Colors.textMuted)
                        } else {
                            Text("尚未验证 · 完成不等于验证")
                                .font(VFont.meta())
                                .foregroundColor(PdigV2Colors.warning)
                        }
                    }
                    .padding(VSpace.lg)
                    .background(PdigV2Colors.surface)
                    .clipShape(RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous))
                    .overlay(
                        RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous)
                            .stroke(PdigV2Colors.borderSubtle, lineWidth: 1)
                    )
                }
            }
            .padding(VSpace.pagePadding)
        }
        .vPageBackground()
        .accessibilityIdentifier("pdig.ios.reference.records")
    }

    private func recordMetric(_ value: String, _ label: String) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(value).font(.system(size: 22, weight: .bold)).foregroundColor(PdigV2Colors.textPrimary)
            Text(label).font(VFont.meta()).foregroundColor(PdigV2Colors.textMuted)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(VSpace.lg)
        .background(PdigV2Colors.surface)
        .clipShape(RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous))
    }
}

struct VWeaknessesView: View {
    var privacyMask: Bool = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: VSpace.sectionGap) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("薄弱点")
                        .font(VFont.pageTitle())
                        .foregroundColor(PdigV2Colors.textPrimary)
                    Text("只展示有依据的连续性发现；不生成安全总分")
                        .font(VFont.meta())
                        .foregroundColor(PdigV2Colors.textMuted)
                }

                ForEach(VNextDemoFixture.findings) { finding in
                    VStack(alignment: .leading, spacing: VSpace.sm) {
                        HStack {
                            Image(systemName: vStatusIcon(finding.status))
                                .foregroundColor(vStatusColor(finding.status))
                            Text(privacyMask ? "连续性发现（已遮蔽）" : finding.title)
                                .font(VFont.body())
                                .fontWeight(.semibold)
                                .foregroundColor(PdigV2Colors.textPrimary)
                            Spacer()
                        }
                        Text(
                            privacyMask
                                ? "相关对象与说明已遮蔽；发现状态与依据数量保持可见。"
                                : finding.explanation
                        )
                            .font(VFont.secondary())
                            .foregroundColor(PdigV2Colors.textSecondary)
                        Text(finding.evidenceCount > 0 ? "依据 \(finding.evidenceCount) 项" : "依据未记录")
                            .font(VFont.meta())
                            .foregroundColor(PdigV2Colors.textMuted)
                    }
                    .padding(VSpace.lg)
                    .background(PdigV2Colors.surface)
                    .clipShape(RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous))
                    .overlay(
                        RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous)
                            .stroke(PdigV2Colors.borderSubtle, lineWidth: 1)
                    )
                }

                Text("unknown != safe · edge count != 独立恢复路径")
                    .font(VFont.meta())
                    .foregroundColor(PdigV2Colors.unknown)
            }
            .padding(VSpace.pagePadding)
        }
        .vPageBackground()
        .accessibilityIdentifier("pdig.ios.reference.weaknesses")
    }
}

struct VMeView: View {
    @ObservedObject var model: VNextModel

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: VSpace.sectionGap) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("我的数字生活")
                        .font(VFont.pageTitle())
                        .foregroundColor(PdigV2Colors.textPrimary)
                    Text("隐私、展示偏好、数据源与本机控制")
                        .font(VFont.meta())
                        .foregroundColor(PdigV2Colors.textMuted)
                }

                VStack(alignment: .leading, spacing: VSpace.md) {
                    HStack {
                        VStack(alignment: .leading, spacing: 3) {
                            Text("隐私遮蔽")
                                .font(VFont.body())
                                .fontWeight(.semibold)
                                .foregroundColor(PdigV2Colors.textPrimary)
                            Text("默认关闭；开启后隐藏可识别对象名称和标识")
                                .font(VFont.meta())
                                .foregroundColor(PdigV2Colors.textMuted)
                        }
                        Spacer()
                        Toggle("", isOn: $model.privacyMask)
                            .labelsHidden()
                    }
                    VMaskIndicator(enabled: model.privacyMask)
                }
                .padding(VSpace.lg)
                .background(PdigV2Colors.surface)
                .clipShape(RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous))

                VStack(alignment: .leading, spacing: VSpace.md) {
                    VSectionHeader(title: "我的基础设施")
                    Text("\(VNextDemoFixture.cards.count) 张卡 · \(VNextDemoFixture.numbers.count) 个号码 · \(VNextDemoFixture.secondaryAssets.count) 个其他对象")
                        .font(VFont.secondary())
                        .foregroundColor(PdigV2Colors.textSecondary)
                    Text("Reference Preview 使用 synthetic fixture，不代表真实 Personal Reality。")
                        .font(VFont.meta())
                        .foregroundColor(PdigV2Colors.unknown)
                }
                .padding(VSpace.lg)
                .background(PdigV2Colors.surface)
                .clipShape(RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous))

                VStack(alignment: .leading, spacing: VSpace.sm) {
                    meAction("个性化", "slider.horizontal.3", id: VTestIds.mePersonalization) { model.navigate(.personalization) }
                    meAction("数据源", "tray.full.fill", id: VTestIds.meSources) { model.navigate(.sources) }
                    meAction("设置", "gearshape.fill", id: VTestIds.meSettings) { model.navigate(.settings) }
                }
            }
            .padding(VSpace.pagePadding)
        }
        .vPageBackground()
        .accessibilityIdentifier(VTestIds.navMe)
    }

    private func meAction(_ title: String, _ icon: String, id: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack {
                Image(systemName: icon)
                    .foregroundColor(PdigV2Colors.primaryBright)
                    .frame(width: 28)
                Text(title)
                    .font(VFont.body())
                    .foregroundColor(PdigV2Colors.textPrimary)
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.system(size: 12))
                    .foregroundColor(PdigV2Colors.textMuted)
            }
            .padding(VSpace.lg)
            .frame(minHeight: VTouchTarget.ios)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .background(PdigV2Colors.surface)
        .clipShape(RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous))
        .accessibilityIdentifier(id)
    }
}
