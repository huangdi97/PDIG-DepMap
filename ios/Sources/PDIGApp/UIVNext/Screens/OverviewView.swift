// OverviewView —— spatial infrastructure overview.
// iPhone stacks Globe + Region list. iPad keeps Globe spatial stage and Region list side-by-side.

import SwiftUI

struct OverviewView: View {
    @ObservedObject var model: VNextModel
    @Environment(\.horizontalSizeClass) private var sizeClass

    private var regions: [RegionPresentation] { VNextDemoFixture.regionSummaries() }
    private var selectedRegion: RegionPresentation? { regions.first { $0.regionCode == model.regionFilter } }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: VSpace.lg) {
                header
                if sizeClass == .regular {
                    HStack(alignment: .top, spacing: VSpace.lg) {
                        globeStage.frame(minHeight: 460)
                            .frame(maxWidth: .infinity)
                        regionList.frame(width: 310)
                    }
                } else {
                    globeStage.frame(height: 340)
                    regionList
                }
                quickEntry
            }
            .padding(VSpace.pagePadding)
        }
        .vPageBackground()
        .sheet(isPresented: $model.globeDetailShown) {
            if let selectedRegion {
                regionDetail(selectedRegion)
                    .presentationDetents([.medium, .large])
                    .presentationDragIndicator(.visible)
            }
        }
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 3) {
            Text(VCopy.myInfrastructure).font(VFont.pageTitle()).foregroundColor(PdigV2Colors.textPrimary)
            Text("按对象管理，按地区查看你的全球基础设施")
                .font(VFont.secondary()).foregroundColor(PdigV2Colors.textSecondary)
        }
    }

    private var globeStage: some View {
        ZStack(alignment: .topLeading) {
            VNextGlobeView(
                regions: regions,
                arcingPairs: VNextDemoFixture.crossRegionPairs(),
                reduceMotion: model.reduceMotion,
                selectedRegion: Binding(
                    get: { model.regionFilter },
                    set: { newValue in newValue.map(model.selectRegion) ?? model.clearRegion() }
                ),
                onOpenRegionDetail: { model.openRegionDetail() },
                onSelectRegion: { model.selectRegion($0.regionCode) },
                onBackToGlobal: { model.clearRegion() }
            )
            .accessibilityIdentifier(VTestIds.globeCanvas)

            VStack(alignment: .leading, spacing: 3) {
                Text("我的基础设施").font(VFont.sectionTitle()).foregroundColor(PdigV2Colors.globeTextPrimary)
                Text("点按地区聚焦 · 再次点按查看地区").font(VFont.meta()).foregroundColor(PdigV2Colors.globeTextSecondary)
            }
            .padding(VSpace.md)
            .background(PdigV2Colors.globeDeep.opacity(0.72))
            .clipShape(RoundedRectangle(cornerRadius: VRadius.md))
            .padding(VSpace.md)
        }
        .background(PdigV2Colors.globeDeep)
        .clipShape(RoundedRectangle(cornerRadius: VRadius.xl, style: .continuous))
        .shadow(color: Color.black.opacity(0.10), radius: 14, y: 5)
        .accessibilityIdentifier(VTestIds.globeStage)
    }

    private var regionList: some View {
        VStack(alignment: .leading, spacing: VSpace.sm) {
            VSectionHeader(title: "地区")
            ForEach(regions) { region in
                VRegionListItem(region: region, selected: model.regionFilter == region.regionCode) {
                    if model.regionFilter == region.regionCode {
                        model.openRegionDetail()
                    } else {
                        model.selectRegion(region.regionCode)
                    }
                }
                .accessibilityIdentifier("\(VTestIds.regionItem).\(region.regionCode)")
            }
        }
        .padding(VSpace.md)
        .background(PdigV2Colors.surface)
        .clipShape(RoundedRectangle(cornerRadius: VRadius.xl, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: VRadius.xl).stroke(PdigV2Colors.borderSubtle, lineWidth: 1))
        .accessibilityIdentifier(VTestIds.regionList)
    }

    private var quickEntry: some View {
        LazyVGrid(columns: [GridItem(.adaptive(minimum: 155), spacing: VSpace.sm)], spacing: VSpace.sm) {
            quickButton("查看卡片", "全球 \(VNextDemoFixture.cards.count) 张卡", "creditcard.fill") { model.navigate(.cards) }
            quickButton("查看号码", "全球 \(VNextDemoFixture.numbers.count) 个号码", "phone.fill") { model.navigate(.numbers) }
            quickButton("更换手机号", "规划与迁移", "arrow.triangle.2.circlepath") { model.navigate(.changePhone) }
            quickButton("基础设施薄弱点", "待确认风险", "exclamationmark.triangle.fill") { model.navigate(.weaknesses) }
        }
        .accessibilityIdentifier(VTestIds.overviewQuick)
    }

    private func quickButton(_ title: String, _ hint: String, _ icon: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: VSpace.md) {
                Image(systemName: icon).foregroundColor(PdigV2Colors.primary)
                    .frame(width: 34, height: 34).background(PdigV2Colors.primarySoft).clipShape(Circle())
                VStack(alignment: .leading, spacing: 2) {
                    Text(title).font(VFont.secondary()).fontWeight(.semibold).foregroundColor(PdigV2Colors.textPrimary)
                    Text(hint).font(VFont.meta()).foregroundColor(PdigV2Colors.textMuted)
                }
                Spacer()
            }
            .padding(VSpace.md).frame(minHeight: 58)
        }
        .buttonStyle(.plain)
        .background(PdigV2Colors.surface)
        .clipShape(RoundedRectangle(cornerRadius: VRadius.md))
        .overlay(RoundedRectangle(cornerRadius: VRadius.md).stroke(PdigV2Colors.borderSubtle, lineWidth: 1))
    }

    private func regionDetail(_ region: RegionPresentation) -> some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: VSpace.lg) {
                    Text(region.displayName).font(VFont.pageTitle()).foregroundColor(PdigV2Colors.textPrimary)
                    Text("\(region.cardCount) 张卡 · \(region.phoneCount) 个号码 · \(region.accountCount) 个账户 · \(region.serviceCount) 项服务")
                        .font(VFont.body()).foregroundColor(PdigV2Colors.textSecondary)

                    VJumpRow(title: "查看全部", subtitle: "该地区所有基础设施") {
                        model.selectRegion(region.regionCode); model.navigate(.overview); model.globeDetailShown = false
                    }
                    VJumpRow(title: "查看卡片", subtitle: "该地区卡片列表") {
                        model.selectRegion(region.regionCode); model.navigate(.cards); model.globeDetailShown = false
                    }
                    VJumpRow(title: "查看号码", subtitle: "该地区号码列表") {
                        model.selectRegion(region.regionCode); model.navigate(.numbers); model.globeDetailShown = false
                    }
                    Button("返回全球视图") {
                        model.clearRegion()
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(PdigV2Colors.primary)
                    .frame(minHeight: 44)
                }
                .padding(VSpace.pagePadding)
            }
            .vPageBackground()
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("关闭") { model.globeDetailShown = false }
                }
            }
        }
    }
}
