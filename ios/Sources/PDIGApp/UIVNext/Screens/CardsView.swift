// CardsView —— iPhone collection + iPad gallery/inspector.
// Card remains an owned financial asset, never a generic database row.

import SwiftUI

struct CardsView: View {
    @ObservedObject var model: VNextModel
    @Environment(\.horizontalSizeClass) private var sizeClass
    @State private var selectedId: String? = VNextDemoFixture.cards.first?.id

    private var filtered: [VCard] {
        VNextDemoFixture.cards.filter { model.regionFilter == nil || $0.region == model.regionFilter }
    }

    private var selected: VCard? {
        if let selectedId, let match = filtered.first(where: { $0.id == selectedId }) { return match }
        return filtered.first
    }

    var body: some View {
        VStack(spacing: 0) {
            if sizeClass == .regular {
                expanded
            } else {
                compact
            }
        }
        .vPageBackground()
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: VSpace.md) {
            HStack(alignment: .firstTextBaseline) {
                VStack(alignment: .leading, spacing: 3) {
                    Text(VCopy.cardsTitle).font(VFont.pageTitle()).foregroundColor(PdigV2Colors.textPrimary)
                    Text(model.regionFilter == nil ? "全球 \(filtered.count) 张卡" : "\(regionName(model.regionFilter!)) · \(filtered.count) 张卡")
                        .font(VFont.secondary()).foregroundColor(PdigV2Colors.textSecondary)
                }
                Spacer()
                Text("卡面").font(VFont.meta()).foregroundColor(PdigV2Colors.textMuted)
            }
            filterRow
        }
    }

    private var compact: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: VSpace.lg) {
                header
                ForEach(filtered) { card in
                    Button { model.openCard(card.id) } label: {
                        HStack(spacing: VSpace.md) {
                            VCardThumbnail(card: card).frame(width: 112)
                            VStack(alignment: .leading, spacing: 4) {
                                HStack {
                                    Text(card.nickname).font(VFont.body()).fontWeight(.semibold).foregroundColor(PdigV2Colors.textPrimary)
                                    Spacer()
                                    VStatusBadge(status: card.status)
                                }
                                Text("\(card.issuer) · \(regionName(card.region)) · \(card.currency)")
                                    .font(VFont.meta()).foregroundColor(PdigV2Colors.textSecondary)
                                Text("\(vCardTypeLabel(card.type)) · \(vCardFormLabel(card.form)) · 到期 \(card.expiry)")
                                    .font(VFont.meta()).foregroundColor(PdigV2Colors.textMuted)
                            }
                        }
                        .padding(VSpace.md)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    .background(PdigV2Colors.surface)
                    .clipShape(RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous))
                    .overlay(RoundedRectangle(cornerRadius: VRadius.lg).stroke(PdigV2Colors.borderSubtle, lineWidth: 1))
                    .accessibilityIdentifier("\(VTestIds.cardCard).\(card.id)")
                }
            }
            .padding(VSpace.pagePadding)
        }
    }

    private var expanded: some View {
        GeometryReader { geo in
            HStack(alignment: .top, spacing: VSpace.lg) {
                ScrollView {
                    VStack(alignment: .leading, spacing: VSpace.lg) {
                        header
                        LazyVGrid(columns: [GridItem(.adaptive(minimum: 230), spacing: VSpace.gridGap)], spacing: VSpace.gridGap) {
                            ForEach(filtered) { card in
                                VAssetCard(card: card, privacyMask: model.privacyMask) {
                                    selectedId = card.id
                                }
                                .overlay(
                                    RoundedRectangle(cornerRadius: VRadius.lg)
                                        .stroke(selected?.id == card.id ? PdigV2Colors.primary : .clear, lineWidth: 2)
                                )
                                .accessibilityIdentifier("\(VTestIds.cardCard).\(card.id)")
                            }
                        }
                        .accessibilityIdentifier(VTestIds.cardGrid)
                    }
                    .padding(.leading, VSpace.pagePadding)
                    .padding(.vertical, VSpace.pagePadding)
                }
                .frame(maxWidth: .infinity)

                if let selected {
                    VCardInspector(card: selected, model: model)
                        .frame(width: min(360, geo.size.width * 0.34))
                        .padding(.trailing, VSpace.pagePadding)
                        .padding(.vertical, VSpace.pagePadding)
                }
            }
        }
    }

    private var filterRow: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: VSpace.sm) {
                VFilterChip(label: VCopy.filterAll, selected: model.regionFilter == nil) { model.clearRegion() }
                ForEach(VNextDemoFixture.regions) { region in
                    VFilterChip(label: region.displayName, selected: model.regionFilter == region.regionCode) {
                        model.selectRegion(region.regionCode)
                    }
                }
            }
        }
    }
}

private struct VCardThumbnail: View {
    let card: VCard
    var body: some View {
        ZStack(alignment: .bottomLeading) {
            cardFaceGradient(card.preset)
            VStack(alignment: .leading, spacing: 2) {
                Text(card.issuer).font(.system(size: 9, weight: .semibold)).foregroundColor(PdigV2Colors.assetTextPrimary)
                Text("•••• \(card.last4)").font(.system(size: 9, design: .monospaced)).foregroundColor(PdigV2Colors.assetTextSecondary)
            }.padding(8)
        }
        .aspectRatio(1.586, contentMode: .fit)
        .clipShape(RoundedRectangle(cornerRadius: 9, style: .continuous))
    }
}

private struct VCardInspector: View {
    let card: VCard
    @ObservedObject var model: VNextModel
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: VSpace.md) {
                VSectionHeader(title: "卡片详情")
                VAssetCard(card: card, privacyMask: model.privacyMask, onClick: {})
                HStack {
                    VChip(text: regionName(card.region))
                    VChip(text: card.currency)
                    VChip(text: vCardFormLabel(card.form))
                }
                VDetailRow(label: "卡组织", value: card.network)
                VDetailRow(label: "到期", value: card.expiry)
                VSectionHeader(title: "使用场景")
                VTagLine(card.usages)
                VSectionHeader(title: "关联服务")
                ForEach(VNextDemoFixture.servicesForCard(card.id)) { service in
                    HStack {
                        Text(service.name).font(VFont.secondary()).foregroundColor(PdigV2Colors.textPrimary)
                        Spacer()
                        VChip(text: serviceKindName(service.kind))
                    }
                }
                Button("定制卡面") { model.openCardCustomization(card.id) }
                    .buttonStyle(.bordered)
                    .tint(PdigV2Colors.primary)
                    .frame(maxWidth: .infinity)
                Button("查看完整详情") { model.openCard(card.id) }
                    .buttonStyle(.borderedProminent)
                    .tint(PdigV2Colors.primary)
                    .frame(maxWidth: .infinity)
            }
            .padding(VSpace.lg)
        }
        .background(PdigV2Colors.surface)
        .clipShape(RoundedRectangle(cornerRadius: VRadius.xl, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: VRadius.xl).stroke(PdigV2Colors.borderSubtle, lineWidth: 1))
        .accessibilityIdentifier("pdig.card.inspector")
    }
}
