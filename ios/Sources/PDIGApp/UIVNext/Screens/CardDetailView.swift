// CardDetailView —— asset-first card detail.
// iPhone keeps a focused vertical flow; iPad separates identity from facts/dependencies/risk.
// Unknown remains Unknown. Presentation controls never mutate PersonalReality or Canonical.

import SwiftUI

struct CardDetailView: View {
    @ObservedObject var model: VNextModel
    @Environment(\.horizontalSizeClass) private var sizeClass

    private var card: VCard? {
        if case .cardDetail(let id) = model.screen { return VNextDemoFixture.cardById(id) }
        return nil
    }

    var body: some View {
        Group {
            if let card {
                ScrollView {
                    VStack(alignment: .leading, spacing: VSpace.lg) {
                        Text("卡片详情").font(VFont.pageTitle()).foregroundColor(PdigV2Colors.textPrimary)

                        if sizeClass == .regular {
                            HStack(alignment: .top, spacing: VSpace.xl) {
                                identityColumn(card)
                                    .frame(maxWidth: 390)
                                infoColumn(card)
                                    .frame(maxWidth: .infinity)
                            }
                        } else {
                            identityColumn(card)
                            infoColumn(card)
                        }
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
        }
    }

    private func identityColumn(_ card: VCard) -> some View {
        VStack(alignment: .leading, spacing: VSpace.lg) {
            VAssetCard(card: card, privacyMask: model.privacyMask, onClick: {})
                .accessibilityIdentifier(VTestIds.cardDetailIdentity)

            VStack(spacing: VSpace.sm) {
                VDetailRow(label: "卡组织", value: card.network)
                VDetailRow(label: "地区", value: regionName(card.region))
                VDetailRow(label: "币种", value: card.currency)
                VDetailRow(label: "卡种", value: vCardTypeLabel(card.type))
                VDetailRow(label: "形态", value: vCardFormLabel(card.form))
                VDetailRow(label: "有效期", value: card.expiry)
            }
            .padding(VSpace.lg)
            .background(PdigV2Colors.surface)
            .clipShape(RoundedRectangle(cornerRadius: VRadius.lg))
            .overlay(RoundedRectangle(cornerRadius: VRadius.lg).stroke(PdigV2Colors.borderSubtle, lineWidth: 1))

            Button { model.openCardCustomization(card.id) } label: {
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("定制卡面").font(VFont.secondary()).fontWeight(.semibold)
                        Text("只改变外观，不改变卡片事实与关联").font(VFont.meta())
                    }
                    Spacer()
                    Image(systemName: "paintbrush")
                }
                .foregroundColor(PdigV2Colors.primaryText)
                .padding(VSpace.md).frame(minHeight: 52)
            }
            .buttonStyle(.plain)
            .background(PdigV2Colors.primarySoft)
            .clipShape(RoundedRectangle(cornerRadius: VRadius.md))
        }
    }

    private func infoColumn(_ card: VCard) -> some View {
        let services = VNextDemoFixture.servicesForCard(card.id)
        return VStack(alignment: .leading, spacing: VSpace.lg) {
            VSectionHeader(title: "使用场景")
            VTagLine(card.usages)

            VSectionHeader(title: "关联服务（\(services.count)）")
            ForEach(services) { service in
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(service.name).font(VFont.secondary()).fontWeight(.semibold).foregroundColor(PdigV2Colors.textPrimary)
                        Text("\(regionName(service.region)) · \(serviceKindName(service.kind))")
                            .font(VFont.meta()).foregroundColor(PdigV2Colors.textMuted)
                    }
                    Spacer()
                    VChip(text: vRelationKindLabel(VNextDemoFixture.relationKind(card.id, service.id) ?? "unknown"))
                }
                .padding(VSpace.md)
                .background(PdigV2Colors.surface)
                .clipShape(RoundedRectangle(cornerRadius: VRadius.md))
                .overlay(RoundedRectangle(cornerRadius: VRadius.md).stroke(PdigV2Colors.borderSubtle, lineWidth: 1))
            }

            VSectionHeader(title: "风险")
            if card.status == "expiring_soon" {
                VDetailNotice(
                    icon: "exclamationmark.triangle.fill",
                    title: "即将到期",
                    body: "这张卡将在 \(card.expiry) 到期；已记录的 \(services.count) 项关联服务需要逐项确认更新。",
                    accent: PdigV2Colors.critical
                )
            } else {
                VUnknownBoundaryNote("当前已记录信息中没有必须处理的卡片风险；未记录关系仍保持未知。")
            }

            VSectionHeader(title: "变更影响")
            VUnknownBoundaryNote(
                services.isEmpty
                    ? "当前没有已确认的服务依赖；这不代表不存在其他依赖。"
                    : "已记录 \(services.count) 项服务依赖。更换、停用或到期前，应逐项确认这些关系。"
            )

            VSectionHeader(title: "变更历史")
            Text("最近记录：2026-06 补充账单日资料；2025-11 更新卡片昵称。")
                .font(VFont.meta()).foregroundColor(PdigV2Colors.textMuted)
        }
        .accessibilityIdentifier(VTestIds.cardDetailInfo)
    }
}

struct VDetailNotice: View {
    let icon: String
    let title: String
    let body: String
    let accent: Color

    var body: some View {
        HStack(alignment: .top, spacing: VSpace.md) {
            Image(systemName: icon).foregroundColor(accent).frame(width: 22)
            VStack(alignment: .leading, spacing: 4) {
                Text(title).font(VFont.secondary()).fontWeight(.semibold).foregroundColor(PdigV2Colors.textPrimary)
                Text(body).font(VFont.meta()).foregroundColor(PdigV2Colors.textSecondary)
            }
            Spacer()
        }
        .padding(VSpace.lg)
        .background(accent.opacity(0.09))
        .clipShape(RoundedRectangle(cornerRadius: VRadius.md))
        .overlay(RoundedRectangle(cornerRadius: VRadius.md).stroke(accent.opacity(0.18), lineWidth: 1))
    }
}
