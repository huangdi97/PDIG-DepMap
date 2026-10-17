// NowView —— 现在（/now）：Globe context + Need Attention + Active Changes + Upcoming。
//
// now.md 契约：
//  - 块顺序固定：globe-context → need-attention → active-changes → upcoming（+ 可选 region 快捷）；
//  - 禁止 8 统计卡行；不显示 0 issues / 安全分（VISUAL_DNA §9）；
//  - Attention 行点击 → 真路由（详情/场景）；Globe 复用四级状态机 + Escape 回退。

import SwiftUI

struct NowView: View {
    @ObservedObject var model: VNextModel

    private var regions: [RegionPresentation] { VNextDemoFixture.regionSummaries() }
    private var arcingPairs: [(String, String)] { VNextDemoFixture.crossRegionPairs() }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: VSpace.sectionGap) {
                // Globe context（受控摘要；pdig.now.globe）
                globeContext

                // Need Attention（L5 最重要；pdig.now.attention）
                needAttentionSection

                // Active Changes（pdig.now.changes）
                activeChangesSection

                // Upcoming（pdig.now.upcoming）
                upcomingSection

                // healthy 表达：检查范围 + 未知范围（不显示 0 问题）
                healthyNote
            }
            .padding(VSpace.pagePadding)
        }
        .vPageBackground()
    }

    // MARK: - Blocks

    private var globeContext: some View {
        VStack(alignment: .leading, spacing: VSpace.sm) {
            VStack(alignment: .leading, spacing: 2) {
                Text(VCopy.nowTitle)
                    .font(VFont.pageTitle())
                    .foregroundColor(PdigV2Colors.textPrimary)
                Text("全球 \(VNextDemoFixture.cards.count) 张卡 · \(VNextDemoFixture.numbers.count) 个号码")
                    .font(VFont.secondary())
                    .foregroundColor(PdigV2Colors.textSecondary)
            }
            .padding(VSpace.xl)

            VNextGlobeView(
                regions: regions,
                arcingPairs: arcingPairs,
                reduceMotion: model.reduceMotion,
                selectedRegion: Binding(
                    get: { model.regionFilter },
                    set: { _ in }
                ),
                onSelectRegion: { model.selectRegion($0.regionCode) },
                onBackToGlobal: { model.backToGlobal() }
            )
            .frame(height: 260)
            .clipShape(RoundedRectangle(cornerRadius: VRadius.xl, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: VRadius.xl, style: .continuous)
                    .stroke(PdigV2Colors.borderSubtle, lineWidth: 1)
            )
            .accessibilityIdentifier(VTestIds.nowGlobe)
        }
        .padding(VSpace.xl)
        .background(PdigV2Colors.surfaceGlass)
        .clipShape(RoundedRectangle(cornerRadius: VRadius.xl, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: VRadius.xl, style: .continuous)
                .stroke(PdigV2Colors.borderSubtle, lineWidth: 1)
        )
    }

    private var needAttentionSection: some View {
        VStack(alignment: .leading, spacing: VSpace.md) {
            VSectionHeader(title: "\(VCopy.needAttention)（\(VNextDemoFixture.attentionItems.count)）")
                .accessibilityIdentifier(VTestIds.nowAttention)
            ForEach(Array(VNextDemoFixture.attentionItems.enumerated()), id: \.offset) { index, item in
                VAttentionRow(item: item) {
                    openAttentionTarget(item)
                }
                .accessibilityIdentifier("\(VTestIds.nowAttentionItem).\(index)")
            }
        }
        .accessibilityIdentifier(VTestIds.nowAttention)
    }

    private var activeChangesSection: some View {
        VStack(alignment: .leading, spacing: VSpace.md) {
            VSectionHeader(title: VCopy.activeChanges, trailing: AnyView(
                Text(VCopy.viewAll)
                    .font(VFont.meta())
                    .foregroundColor(PdigV2Colors.primaryBright)
            ))
            ForEach(VNextDemoFixture.activeChanges) { change in
                Button {
                    model.navigate(.changePhone)
                } label: {
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(change.title)
                                .font(VFont.body())
                                .fontWeight(.medium)
                                .foregroundColor(PdigV2Colors.textPrimary)
                            Text("阶段：\(VCopy.verifyNewNumber)")
                                .font(VFont.meta())
                                .foregroundColor(PdigV2Colors.warning)
                        }
                        Spacer()
                        Image(systemName: "chevron.right")
                            .font(.system(size: 12))
                            .foregroundColor(PdigV2Colors.textMuted)
                    }
                    .padding(VSpace.lg)
                    .frame(minHeight: 44)
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .background(PdigV2Colors.surfaceRaised)
                .clipShape(RoundedRectangle(cornerRadius: VRadius.md, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: VRadius.md, style: .continuous)
                        .stroke(PdigV2Colors.borderSubtle, lineWidth: 1)
                )
            }
        }
        .accessibilityIdentifier(VTestIds.nowChanges)
    }

    private var upcomingSection: some View {
        VStack(alignment: .leading, spacing: VSpace.md) {
            VSectionHeader(title: VCopy.upcoming)
            ForEach(VNextDemoFixture.upcoming) { item in
                HStack {
                    Text(item.title)
                        .font(VFont.secondary())
                        .foregroundColor(PdigV2Colors.textSecondary)
                    Spacer()
                    Text("\(item.days) 天后")
                        .font(VFont.meta())
                        .fontWeight(.semibold)
                        .foregroundColor(PdigV2Colors.warning)
                        .padding(.horizontal, VSpace.sm)
                        .padding(.vertical, 2)
                        .background(PdigV2Colors.warning.opacity(0.16))
                        .clipShape(RoundedRectangle(cornerRadius: VRadius.sm, style: .continuous))
                }
                .padding(VSpace.lg)
                .frame(minHeight: 44)
                .background(PdigV2Colors.surfaceRaised)
                .clipShape(RoundedRectangle(cornerRadius: VRadius.md, style: .continuous))
            }
        }
        .accessibilityIdentifier(VTestIds.nowUpcoming)
    }

    /// healthy 表达（uiuxV031 语义；不显示 0 issues / 安全分 / all clear）。
    private var healthyNote: some View {
        VStack(alignment: .leading, spacing: VSpace.sm) {
            Text(VCopy.noImmediate)
                .font(VFont.body())
                .foregroundColor(PdigV2Colors.textPrimary)
            Text("最近一次检查覆盖了卡片、号码与关联服务。\(VCopy.healthyUnknown)：新导入账单的用途标签。")
                .font(VFont.meta())
                .foregroundColor(PdigV2Colors.unknown)
        }
        .padding(VSpace.lg)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(PdigV2Colors.surface)
        .clipShape(RoundedRectangle(cornerRadius: VRadius.md, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: VRadius.md, style: .continuous)
                .stroke(PdigV2Colors.borderSubtle, lineWidth: 1)
        )
    }

    // MARK: - Actions

    /// Attention 行点击 → 真路由（INTERACTION_CONTRACT §6）。
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
