import SwiftUI

/// iOS vNext reference translation for the secondary infrastructure surfaces.
///
/// This remains reference/synthetic UI. It intentionally does not claim
/// Production Reality authority; Production wiring is a separate gate.
struct VSecondaryCollectionView: View {
    @ObservedObject var model: VNextModel
    let kind: VSecondaryKind

    private var items: [VSecondaryAsset] {
        VNextDemoFixture.secondaryAssets(for: kind)
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: VSpace.sectionGap) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(kind.title)
                        .font(VFont.pageTitle())
                        .foregroundColor(PdigV2Colors.textPrimary)
                    Text("已记录的数字基础设施对象 · 参考数据")
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
                    VChip(text: "全部", highlight: true)
                }
                .padding(VSpace.lg)
                .background(PdigV2Colors.surface)
                .clipShape(RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous))

                VStack(spacing: VSpace.md) {
                    ForEach(items) { item in
                        Button {
                            model.navigate(.secondaryDetail(kind, item.id))
                        } label: {
                            HStack(spacing: VSpace.lg) {
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
                                        .lineLimit(1)
                                    Text("\(item.relationCount) 条已记录关系")
                                        .font(VFont.meta())
                                        .foregroundColor(PdigV2Colors.textMuted)
                                }
                                Spacer()
                                VStatusBadge(status: item.attention ? "warning" : "active")
                            }
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
                    }
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
                    meAction("个性化", "slider.horizontal.3") { model.navigate(.personalization) }
                    meAction("数据源", "tray.full.fill") { model.navigate(.sources) }
                    meAction("设置", "gearshape.fill") { model.navigate(.settings) }
                }
            }
            .padding(VSpace.pagePadding)
        }
        .vPageBackground()
        .accessibilityIdentifier(VTestIds.navMe)
    }

    private func meAction(_ title: String, _ icon: String, action: @escaping () -> Void) -> some View {
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
    }
}
