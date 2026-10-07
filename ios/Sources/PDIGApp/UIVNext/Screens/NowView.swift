// NowView —— task-first home with a spatial Globe identity.
//
// iPhone: Globe first, then concrete tasks.
// iPad: Globe remains the dominant spatial object while tasks occupy a focused side column.
// No generic statistics dashboard and no fake "all safe" score.

import SwiftUI

struct NowView: View {
    @ObservedObject var model: VNextModel
    @Environment(\.horizontalSizeClass) private var sizeClass

    private var regions: [RegionPresentation] { VNextDemoFixture.regionSummaries() }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: VSpace.lg) {
                greeting
                if sizeClass == .regular {
                    HStack(alignment: .top, spacing: VSpace.lg) {
                        globeHero
                            .frame(maxWidth: .infinity)
                        VStack(alignment: .leading, spacing: VSpace.lg) {
                            needAttentionSection
                            activeChangesSection
                            upcomingSection
                        }
                        .frame(width: 360)
                    }
                    healthyNote
                } else {
                    globeHero
                    needAttentionSection
                    activeChangesSection
                    upcomingSection
                    healthyNote
                }
            }
            .padding(VSpace.pagePadding)
        }
        .vPageBackground()
    }

    private var greeting: some View {
        HStack(alignment: .top) {
            VStack(alignment: .leading, spacing: 3) {
                Text("你好")
                    .font(VFont.pageTitle())
                    .foregroundColor(PdigV2Colors.textPrimary)
                Text("你有 \(VNextDemoFixture.attentionItems.count) 件事需要处理")
                    .font(VFont.secondary())
                    .foregroundColor(PdigV2Colors.textSecondary)
            }
            Spacer()
            Text("需要处理 \(VNextDemoFixture.attentionItems.count)")
                .font(VFont.meta()).fontWeight(.semibold)
                .foregroundColor(PdigV2Colors.critical)
                .padding(.horizontal, VSpace.md)
                .frame(minHeight: 36)
                .background(PdigV2Colors.critical.opacity(0.09))
                .clipShape(Capsule())
        }
    }

    private var globeHero: some View {
        VStack(alignment: .leading, spacing: 0) {
            ZStack(alignment: .topLeading) {
                VNextGlobeView(
                    regions: regions,
                    arcingPairs: VNextDemoFixture.crossRegionPairs(),
                    reduceMotion: model.reduceMotion,
                    selectedRegion: Binding(
                        get: { model.regionFilter },
                        set: { value in value.map(model.selectRegion) ?? model.clearRegion() }
                    ),
                    onOpenRegionDetail: { model.openRegionDetail() },
                    onSelectRegion: { model.selectRegion($0.regionCode) },
                    onBackToGlobal: { model.clearRegion() }
                )
                .frame(height: sizeClass == .regular ? 420 : 300)
                .accessibilityIdentifier(VTestIds.nowGlobe)

                VStack(alignment: .leading, spacing: 3) {
                    Text("你的数字基础设施")
                        .font(VFont.sectionTitle())
                        .foregroundColor(PdigV2Colors.globeTextPrimary)
                    Text("连接 \(VNextDemoFixture.regions.count) 个地区 · 一览全局")
                        .font(VFont.meta())
                        .foregroundColor(PdigV2Colors.globeTextSecondary)
                }
                .padding(VSpace.md)
                .background(PdigV2Colors.globeDeep.opacity(0.72))
                .clipShape(RoundedRectangle(cornerRadius: VRadius.md))
                .padding(VSpace.md)
            }

            HStack(spacing: 0) {
                heroMetric("\(VNextDemoFixture.cards.count)", "卡片")
                heroMetric("\(VNextDemoFixture.numbers.count)", "号码")
                heroMetric("\(VNextDemoFixture.accounts.count)", "账户")
                heroMetric("\(VNextDemoFixture.services.count)", "服务")
            }
            .padding(.vertical, VSpace.sm)
            .background(PdigV2Colors.surface)
        }
        .clipShape(RoundedRectangle(cornerRadius: VRadius.xl, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: VRadius.xl).stroke(PdigV2Colors.borderSubtle, lineWidth: 1))
        .shadow(color: Color.black.opacity(0.08), radius: 14, y: 5)
    }

    private func heroMetric(_ value: String, _ label: String) -> some View {
        VStack(spacing: 2) {
            Text(value).font(VFont.body()).fontWeight(.bold).foregroundColor(PdigV2Colors.textPrimary)
            Text(label).font(VFont.meta()).foregroundColor(PdigV2Colors.textMuted)
        }
        .frame(maxWidth: .infinity)
    }

    private var needAttentionSection: some View {
        VStack(alignment: .leading, spacing: VSpace.sm) {
            VSectionHeader(title: "需要你处理（\(VNextDemoFixture.attentionItems.count)）")
            ForEach(Array(VNextDemoFixture.attentionItems.enumerated()), id: \.offset) { index, item in
                VAttentionRow(item: item) { openAttentionTarget(item) }
                    .accessibilityIdentifier("\(VTestIds.nowAttentionItem).\(index)")
            }
        }
        .accessibilityIdentifier(VTestIds.nowAttention)
    }

    private var activeChangesSection: some View {
        VStack(alignment: .leading, spacing: VSpace.sm) {
            VSectionHeader(title: "进行中的变更")
            ForEach(VNextDemoFixture.activeChanges) { change in
                Button { model.navigate(.changePhone) } label: {
                    HStack {
                        VStack(alignment: .leading, spacing: 3) {
                            Text(change.title)
                                .font(VFont.secondary()).fontWeight(.semibold)
                                .foregroundColor(PdigV2Colors.textPrimary)
                            Text("当前阶段 · 验证新号码")
                                .font(VFont.meta()).foregroundColor(PdigV2Colors.warning)
                        }
                        Spacer()
                        Image(systemName: "chevron.right")
                            .font(.system(size: 11, weight: .semibold))
                            .foregroundColor(PdigV2Colors.primaryText)
                    }
                    .padding(VSpace.md).frame(minHeight: 52)
                }
                .buttonStyle(.plain)
                .background(PdigV2Colors.surface)
                .clipShape(RoundedRectangle(cornerRadius: VRadius.md))
                .overlay(RoundedRectangle(cornerRadius: VRadius.md).stroke(PdigV2Colors.borderSubtle, lineWidth: 1))
            }
        }
        .accessibilityIdentifier(VTestIds.nowChanges)
    }

    private var upcomingSection: some View {
        VStack(alignment: .leading, spacing: VSpace.sm) {
            VSectionHeader(title: "即将到来")
            ForEach(VNextDemoFixture.upcoming) { item in
                HStack {
                    Text(item.title).font(VFont.meta()).foregroundColor(PdigV2Colors.textPrimary)
                    Spacer()
                    Text("\(item.days) 天后").font(VFont.meta()).fontWeight(.semibold).foregroundColor(PdigV2Colors.warning)
                }
                .padding(VSpace.md)
                .background(PdigV2Colors.surfaceRaised)
                .clipShape(RoundedRectangle(cornerRadius: VRadius.md))
            }
        }
        .accessibilityIdentifier(VTestIds.nowUpcoming)
    }

    private var healthyNote: some View {
        Text("未记录 ≠ 无风险：当前首页只展示已确认的关注事项；未记录的依赖继续保持未知。")
            .font(VFont.meta())
            .foregroundColor(PdigV2Colors.unknown)
            .padding(VSpace.lg)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(PdigV2Colors.surface)
            .clipShape(RoundedRectangle(cornerRadius: VRadius.md))
            .overlay(RoundedRectangle(cornerRadius: VRadius.md).stroke(PdigV2Colors.borderSubtle, lineWidth: 1))
    }

    private func openAttentionTarget(_ item: VAttentionItem) {
        if VNextDemoFixture.cardById(item.target) != nil {
            model.openCard(item.target)
        } else if VNextDemoFixture.numberById(item.target) != nil {
            model.openNumber(item.target)
        } else {
            model.navigate(.changePhone)
        }
    }
}
