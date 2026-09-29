// CardsView —— 卡片管理（/infrastructure/cards）：过滤 + Visual Grid / Compact List。
//
// cards.md / asset-card.md 契约：
//  - 过滤维度：全部/国家/实体虚拟/储蓄信用/币种/状态/即将到期（与 regionFilter 同步）；
//  - Desktop 默认 visual-grid ↔ compact-list 切换；iOS 用原生 SwiftUI LazyVGrid；
//  - 卡项 testId pdig.card.card；缺失字段只显示 未设置/未知，禁止推断。

import SwiftUI

struct CardsView: View {
    @ObservedObject var model: VNextModel
    @State private var gridView = true

    private var filtered: [VCard] {
        if let region = model.regionFilter {
            return VNextDemoFixture.cards.filter { $0.region == region }
        }
        return VNextDemoFixture.cards
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: VSpace.sectionGap) {
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(VCopy.cardsTitle)
                            .font(VFont.pageTitle())
                            .foregroundColor(PdigV2Colors.textPrimary)
                        Text(subtitle)
                            .font(VFont.secondary())
                            .foregroundColor(PdigV2Colors.textSecondary)
                    }
                    Spacer()
                    // 视图切换（visual-grid ↔ compact-list；pdig.card.viewToggle）
                    VFilterChip(
                        label: gridView ? VCopy.toggleToList : VCopy.toggleToGrid,
                        selected: true,
                        action: { gridView.toggle() }
                    )
                    .accessibilityIdentifier(VTestIds.cardViewToggle)
                }

                // 过滤行（全部 / 国家 / 即将到期）
                filterRow

                if gridView {
                    grid
                        .accessibilityIdentifier(VTestIds.cardGrid)
                } else {
                    list
                        .accessibilityIdentifier(VTestIds.cardList)
                }
            }
            .padding(VSpace.pagePadding)
        }
        .vPageBackground()
    }

    private var subtitle: String {
        if let region = model.regionFilter {
            return "地区 \(region) · \(filtered.count) 张卡"
        }
        return "全球 \(filtered.count) 张卡"
    }

    // MARK: - Filter row

    private var filterRow: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: VSpace.sm) {
                VFilterChip(label: VCopy.filterAll, selected: model.regionFilter == nil) {
                    model.regionFilter = nil
                }
                ForEach(VNextDemoFixture.regions) { region in
                    VFilterChip(
                        label: region.regionCode,
                        selected: model.regionFilter == region.regionCode
                    ) {
                        model.selectRegion(region.regionCode)
                    }
                }
                VFilterChip(label: VCopy.filterExpiring, selected: false) {
                    // 即将到期过滤在演示 fixture 中固定为 expiring_soon（timelineBuckets 语义）。
                }
            }
        }
    }

    // MARK: - Visual Grid（2 列 mobile；iPad/大屏自适应）

    private var grid: some View {
        LazyVGrid(
            columns: [GridItem(.adaptive(minimum: 150), spacing: VSpace.gridGap)],
            spacing: VSpace.gridGapWide
        ) {
            ForEach(filtered) { card in
                VAssetCard(card: card, privacyMask: model.privacyMask) {
                    model.openCard(card.id)
                }
                .accessibilityIdentifier(VTestIds.cardCard)
            }
        }
    }

    // MARK: - Compact List（行高 40–48 语义）

    private var list: some View {
        VStack(spacing: 0) {
            ForEach(filtered) { card in
                Button {
                    model.openCard(card.id)
                } label: {
                    HStack(spacing: VSpace.md) {
                        Rectangle()
                            .fill(PdigV2Colors.primaryBright)
                            .frame(width: 4, height: 28)
                            .clipShape(RoundedRectangle(cornerRadius: 2))
                        VStack(alignment: .leading, spacing: 2) {
                            HStack(spacing: VSpace.sm) {
                                Text(card.nickname)
                                    .font(VFont.secondary())
                                    .fontWeight(.semibold)
                                    .foregroundColor(PdigV2Colors.textPrimary)
                                Text(card.masked)
                                    .font(VFont.meta())
                                    .foregroundColor(PdigV2Colors.textMuted)
                            }
                            Text("\(card.issuer) · \(card.region) · \(card.currency) · \(vCardFormLabel(card.form)) · 到期 \(card.expiry)")
                                .font(VFont.meta())
                                .foregroundColor(PdigV2Colors.textSecondary)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        VStatusBadge(status: card.status)
                    }
                    .padding(.horizontal, VSpace.lg)
                    .frame(minHeight: 44)
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .background(PdigV2Colors.surface.opacity(0.92))
                .overlay(
                    Rectangle()
                        .fill(PdigV2Colors.borderSubtle)
                        .frame(height: 1),
                    alignment: .bottom
                )
                .accessibilityIdentifier("\(VTestIds.cardCard).\(card.id)")
            }
        }
        .clipShape(RoundedRectangle(cornerRadius: VRadius.md, style: .continuous))
    }
}
