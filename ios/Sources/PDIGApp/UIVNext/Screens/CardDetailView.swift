// CardDetailView —— 卡片详情（/infrastructure/cards/{id}，身份优先）。
//
// card-detail.md 契约：
//  - 顶部先展示卡面视觉身份（identity 区），其后按 使用场景/绑定服务/风险/恢复替代/变更历史；
//  - 缺失字段只显示 未设置/未知，禁止推断；详情页不做领域写操作；
//  - 动作：定制 → /infrastructure/cards/{id}/customize（PresentationProfile 只改外观）。

import SwiftUI

struct CardDetailView: View {
    @ObservedObject var model: VNextModel

    private var card: VCard? {
        if case .cardDetail(let id) = model.screen {
            return VNextDemoFixture.cardById(id)
        }
        return nil
    }

    var body: some View {
        if let card = card {
            ScrollView {
                VStack(alignment: .leading, spacing: VSpace.sectionGap) {
                    titleBar(card)

                    // Identity（pdig.card.detail.identity）
                    identitySection(card)
                        .accessibilityIdentifier(VTestIds.cardDetailIdentity)

                    // Info workspace（pdig.card.detail.info）
                    infoSection(card)
                        .accessibilityIdentifier(VTestIds.cardDetailInfo)
                }
                .padding(VSpace.pagePadding)
            }
            .vPageBackground()
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    VBackButton { model.back() }
                }
            }
            .navigationBarBackButtonHidden(true)
        }
    }

    private func titleBar(_ card: VCard) -> some View {
        HStack {
            Text(VCopy.cardDetailIdentity)
                .font(VFont.pageTitle())
                .foregroundColor(PdigV2Colors.textPrimary)
            Spacer()
        }
    }

    private func identitySection(_ card: VCard) -> some View {
        VStack(alignment: .leading, spacing: VSpace.lg) {
            VAssetCard(card: card, privacyMask: model.privacyMask, onClick: {})
                .accessibilityIdentifier(VTestIds.cardCard)

            VStack(spacing: VSpace.sm) {
                VDetailRow(label: "卡组织", value: card.network)
                VDetailRow(label: "地区", value: card.region)
                VDetailRow(label: "币种", value: card.currency)
                VDetailRow(label: "卡种", value: vCardTypeLabel(card.type))
                VDetailRow(label: "形态", value: vCardFormLabel(card.form))
                VDetailRow(label: "有效期", value: card.expiry)
            }

            // 呈现（PresentationProfile 入口：定制卡面）
            VStack(alignment: .leading, spacing: VSpace.sm) {
                Text("呈现")
                    .font(VFont.label())
                    .foregroundColor(PdigV2Colors.textSecondary)
                Button {
                    model.openCardCustomization(card.id)
                } label: {
                    Text("\(VCopy.customizeCardFace) →")
                        .font(VFont.secondary())
                        .fontWeight(.semibold)
                        .foregroundColor(PdigV2Colors.primaryBright)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(VSpace.md)
                        .frame(minHeight: 44)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .background(PdigV2Colors.primarySoft)
                .clipShape(RoundedRectangle(cornerRadius: VRadius.md, style: .continuous))
                VChip(text: "\(VCopy.currentTheme)：\(card.preset) · 素材全部本地")
                Text(VCopy.presentationNote)
                    .font(VFont.meta())
                    .foregroundColor(PdigV2Colors.textMuted)
            }
        }
        .padding(VSpace.xl)
        .background(PdigV2Colors.surface.opacity(0.9))
        .clipShape(RoundedRectangle(cornerRadius: VRadius.xl, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: VRadius.xl, style: .continuous)
                .stroke(PdigV2Colors.borderSubtle, lineWidth: 1)
        )
    }

    private func infoSection(_ card: VCard) -> some View {
        let services = VNextDemoFixture.servicesForCard(card.id)
        return VStack(alignment: .leading, spacing: VSpace.sectionGap) {
            // 使用场景
            VStack(alignment: .leading, spacing: VSpace.md) {
                VSectionHeader(title: VCopy.cardDetailUsage)
                HStack(spacing: VSpace.sm) {
                    ForEach(card.usages, id: \.self) { usage in
                        VChip(text: usage)
                    }
                }
            }

            // 绑定服务
            VStack(alignment: .leading, spacing: VSpace.md) {
                VSectionHeader(title: "\(VCopy.cardDetailServices)（\(services.count)）")
                ForEach(services) { service in
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(service.name)
                                .font(VFont.body())
                                .fontWeight(.medium)
                                .foregroundColor(PdigV2Colors.textPrimary)
                            Text("地区 \(service.region) · \(service.kind)")
                                .font(VFont.meta())
                                .foregroundColor(PdigV2Colors.textMuted)
                        }
                        Spacer()
                        VChip(text: vRelationKindLabel(service.kind))
                    }
                    .padding(VSpace.lg)
                    .frame(minHeight: 48)
                    .background(PdigV2Colors.surfaceRaised)
                    .clipShape(RoundedRectangle(cornerRadius: VRadius.md, style: .continuous))
                    .overlay(
                        RoundedRectangle(cornerRadius: VRadius.md, style: .continuous)
                            .stroke(PdigV2Colors.borderSubtle, lineWidth: 1)
                    )
                }
            }

            // 风险（三通道；expiring_soon → critical 软底；未知 = 未知不推断）
            VStack(alignment: .leading, spacing: VSpace.md) {
                VSectionHeader(title: VCopy.cardDetailRisk)
                riskPanel(card, servicesCount: services.count)
            }

            // 恢复与替代
            VStack(alignment: .leading, spacing: VSpace.md) {
                VSectionHeader(title: VCopy.cardDetailRecovery)
                Text("模拟更换此卡：影响分析由已确认依赖驱动（未知 = 未知，绝不推断）。接入 Impact Kernel 后此处展示「必须处理 / 有备用路径 / 建议检查」分级。")
                    .font(VFont.secondary())
                    .foregroundColor(PdigV2Colors.textSecondary)
            }

            // 变更历史
            VStack(alignment: .leading, spacing: VSpace.md) {
                VSectionHeader(title: VCopy.cardDetailHistory)
                Text("最近记录：2026-06 补充账单日资料；2025-11 更新卡片昵称。")
                    .font(VFont.meta())
                    .foregroundColor(PdigV2Colors.textMuted)
            }
        }
    }

    /// 风险行：icon + label + color 三通道（INTERACTION_CONTRACT §9）。
    @ViewBuilder
    private func riskPanel(_ card: VCard, servicesCount: Int) -> some View {
        if card.status == "expiring_soon" {
            HStack(spacing: VSpace.md) {
                Image(systemName: "exclamationmark.octagon.fill")
                    .foregroundColor(PdigV2Colors.critical)
                    .frame(width: 18)
                Text("卡片将在 \(card.expiry) 到期：\(servicesCount) 项绑定服务可能中断，建议提前更换卡后重新绑定。")
                    .font(VFont.secondary())
                    .foregroundColor(PdigV2Colors.textPrimary)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(VSpace.lg)
            .frame(minHeight: 44)
            .background(PdigV2Colors.critical.opacity(0.12))
            .clipShape(RoundedRectangle(cornerRadius: VRadius.md, style: .continuous))
        } else {
            Text("当前未发现必须处理的风险。")
                .font(VFont.secondary())
                .foregroundColor(PdigV2Colors.textSecondary)
        }
    }
}
