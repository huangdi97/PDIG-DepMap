// InfrastructureHubView —— iPhone infrastructure entry.
// Eight object families stay secondary to the top-level Infrastructure tab.
// A compact Globe preserves spatial identity without duplicating Now.

import SwiftUI

struct InfrastructureHubView: View {
    @ObservedObject var model: VNextModel

    private let items: [(String, String, String, VScreen)] = [
        ("总览", "globe.asia.australia.fill", "全球与地区", .overview),
        ("卡片", "creditcard.fill", "10 张", .cards),
        ("号码", "phone.fill", "7 个", .numbers),
        ("账户", "person.crop.circle.fill", "5 个", .accounts),
        ("邮箱", "envelope.fill", "3 个", .emails),
        ("设备", "laptopcomputer.and.iphone", "4 台", .devices),
        ("服务", "star.fill", "10 项", .services),
        ("薄弱点", "exclamationmark.triangle.fill", "查看风险", .weaknesses),
    ]

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: VSpace.lg) {
                VStack(alignment: .leading, spacing: 3) {
                    Text("基础设施").font(VFont.pageTitle()).foregroundColor(PdigV2Colors.textPrimary)
                    Text("按对象管理，按地区查看你的全球数字基础设施")
                        .font(VFont.secondary()).foregroundColor(PdigV2Colors.textSecondary)
                }

                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: VSpace.sm) {
                    ForEach(Array(items.enumerated()), id: \.offset) { _, item in
                        Button { model.navigate(item.3) } label: {
                            VStack(alignment: .leading, spacing: VSpace.sm) {
                                HStack {
                                    Image(systemName: item.1)
                                        .foregroundColor(PdigV2Colors.primary)
                                        .frame(width: 34, height: 34)
                                        .background(PdigV2Colors.primarySoft)
                                        .clipShape(RoundedRectangle(cornerRadius: VRadius.md))
                                    Spacer()
                                    Image(systemName: "chevron.right")
                                        .font(.system(size: 10, weight: .semibold))
                                        .foregroundColor(PdigV2Colors.textMuted)
                                }
                                Text(item.0).font(VFont.secondary()).fontWeight(.semibold).foregroundColor(PdigV2Colors.textPrimary)
                                Text(item.2).font(VFont.meta()).foregroundColor(PdigV2Colors.textMuted)
                            }
                            .padding(VSpace.md).frame(maxWidth: .infinity, minHeight: 104, alignment: .leading)
                        }
                        .buttonStyle(.plain)
                        .background(PdigV2Colors.surface)
                        .clipShape(RoundedRectangle(cornerRadius: VRadius.lg))
                        .overlay(RoundedRectangle(cornerRadius: VRadius.lg).stroke(PdigV2Colors.borderSubtle, lineWidth: 1))
                    }
                }

                ZStack(alignment: .topLeading) {
                    VNextGlobeView(
                        regions: VNextDemoFixture.regionSummaries(),
                        arcingPairs: VNextDemoFixture.crossRegionPairs(),
                        reduceMotion: true,
                        selectedRegion: .constant(nil)
                    )
                    .frame(height: 220)
                    VStack(alignment: .leading, spacing: 3) {
                        Text("地区分布").font(VFont.secondary()).fontWeight(.semibold).foregroundColor(PdigV2Colors.globeTextPrimary)
                        Text("点开总览查看地区与依赖上下文").font(VFont.meta()).foregroundColor(PdigV2Colors.globeTextSecondary)
                    }
                    .padding(VSpace.md)
                    .background(PdigV2Colors.globeDeep.opacity(0.72))
                    .clipShape(RoundedRectangle(cornerRadius: VRadius.md))
                    .padding(VSpace.md)
                }
                .background(PdigV2Colors.globeDeep)
                .clipShape(RoundedRectangle(cornerRadius: VRadius.xl))
                .onTapGesture { model.navigate(.overview) }

                VStack(alignment: .leading, spacing: VSpace.sm) {
                    VSectionHeader(title: "需要关注")
                    ForEach(VNextDemoFixture.attentionItems.prefix(2)) { item in
                        VAttentionRow(item: item) {
                            if VNextDemoFixture.cardById(item.target) != nil { model.openCard(item.target) }
                            else if VNextDemoFixture.numberById(item.target) != nil { model.openNumber(item.target) }
                        }
                    }
                }
            }
            .padding(VSpace.pagePadding)
        }
        .vPageBackground()
        .accessibilityIdentifier("pdig.infrastructure.hub")
    }
}
