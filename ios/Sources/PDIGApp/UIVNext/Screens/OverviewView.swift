// OverviewView —— 基础设施总览（/infrastructure）：Globe 舞台 + Region List + 右活动轨 + 底部快速入口。
//
// overview.md / globe.md / region-node.md 契约：
//  - Globe Stage 视觉主导；Region List 为非视觉替代（驱动 filter，非唯一导航，IA §7）；
//  - 点地区 → REGION_SELECTED（focus + filter + 上下文抽屉）；抽屉动作 → 带 ?region= 路由；
//  - Escape 回退：REGION_DETAIL → REGION_SELECTED → GLOBAL（INTERACTION_CONTRACT §1）。

import SwiftUI

struct OverviewView: View {
    @ObservedObject var model: VNextModel

    private var regions: [RegionPresentation] { VNextDemoFixture.regionSummaries() }
    private var arcingPairs: [(String, String)] { VNextDemoFixture.crossRegionPairs() }
    private var selectedRegion: RegionPresentation? {
        regions.first { $0.regionCode == model.regionFilter }
    }

    var body: some View {
        GeometryReader { geo in
            ScrollView {
                VStack(alignment: .leading, spacing: VSpace.sectionGap) {
                    header

                    // Globe 舞台
                    globeStage
                        .accessibilityIdentifier(VTestIds.globeStage)

                    // Region List（非视觉替代）
                    regionList
                        .accessibilityIdentifier(VTestIds.regionList)

                    // 快速入口
                    quickEntry
                        .accessibilityIdentifier(VTestIds.overviewQuick)

                    Spacer(minLength: 16)
                }
                .padding(VSpace.pagePadding)
            }
        }
        .vPageBackground()
        // Region Drawer 覆盖层（REGION_DETAIL）
        .overlay(alignment: .bottom) {
            if model.globeDetailShown, let region = selectedRegion {
                regionDrawer(region)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
            }
        }
        .animation(.easeInOut(duration: VMotion.normal), value: model.globeDetailShown)
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(VCopy.myInfrastructure)
                .font(VFont.pageTitle())
                .foregroundColor(PdigV2Colors.textPrimary)
            Text(VCopy.globeHint)
                .font(VFont.meta())
                .foregroundColor(PdigV2Colors.textMuted)
        }
    }

    private var globeStage: some View {
        VStack(alignment: .leading, spacing: 0) {
            VNextGlobeView(
                regions: regions,
                arcingPairs: arcingPairs,
                reduceMotion: model.reduceMotion,
                selectedRegion: Binding(
                    get: { model.regionFilter },
                    set: { newValue in
                        if let newValue = newValue {
                            model.selectRegion(newValue)
                        } else {
                            model.backToGlobal()
                        }
                    }
                ),
                onOpenRegionDetail: { model.openRegionDetail() },
                onSelectRegion: { model.selectRegion($0.regionCode) },
                onBackToGlobal: { model.backToGlobal() }
            )
            .frame(height: 420)
            .clipShape(RoundedRectangle(cornerRadius: VRadius.xl, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: VRadius.xl, style: .continuous)
                    .stroke(PdigV2Colors.borderSubtle, lineWidth: 1)
            )
            .accessibilityIdentifier(VTestIds.globeCanvas)
        }
        .background(PdigV2Colors.surfaceGlass)
        .clipShape(RoundedRectangle(cornerRadius: VRadius.xl, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: VRadius.xl, style: .continuous)
                .stroke(PdigV2Colors.borderSubtle, lineWidth: 1)
        )
    }

    /// Region List（键盘/读屏可达；interaction-contract §4）。
    private var regionList: some View {
        VStack(alignment: .leading, spacing: VSpace.md) {
            VSectionHeader(title: VCopy.regionListTitle)
            ForEach(regions) { region in
                VRegionListItem(
                    region: region,
                    selected: model.regionFilter == region.regionCode,
                    onClick: {
                        if model.regionFilter == region.regionCode {
                            model.backToGlobal()
                        } else {
                            model.selectRegion(region.regionCode)
                        }
                    }
                )
                .accessibilityIdentifier("\(VTestIds.regionItem).\(region.regionCode)")
            }
        }
    }

    /// 底部快速入口（pdig.overview.quick）。
    private var quickEntry: some View {
        HStack(spacing: VSpace.gridGap) {
            quickButton(VCopy.quickCards, "全球 \(VNextDemoFixture.cards.count) 张卡") {
                model.navigate(.cards)
            }
            quickButton(VCopy.quickNumbers, "全球 \(VNextDemoFixture.numbers.count) 个号码") {
                model.navigate(.numbers)
            }
            quickButton(VCopy.quickChangePhone, "旗舰流程") {
                model.navigate(.changePhone)
            }
            quickButton(VCopy.quickWeaknesses, "待确认风险") {
                model.navigate(.weaknesses)
            }
        }
        .padding(VSpace.xl)
        .frame(maxWidth: .infinity)
        .background(PdigV2Colors.surface.opacity(0.9))
        .clipShape(RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous)
                .stroke(PdigV2Colors.borderSubtle, lineWidth: 1)
        )
    }

    private func quickButton(_ title: String, _ hint: String, _ action: @escaping () -> Void) -> some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(VFont.secondary())
                    .fontWeight(.semibold)
                    .foregroundColor(PdigV2Colors.textPrimary)
                Text(hint)
                    .font(VFont.meta())
                    .foregroundColor(PdigV2Colors.textMuted)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(VSpace.lg)
            .frame(minHeight: 60)
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

    /// Region Drawer：地区资产摘要 + 3 个动作（查看全部/查看卡片/查看号码，带 region filter）。
    private func regionDrawer(_ region: RegionPresentation) -> some View {
        VStack(alignment: .leading, spacing: VSpace.md) {
            HStack {
                Text(region.displayName)
                    .font(VFont.sectionTitle())
                    .foregroundColor(PdigV2Colors.textPrimary)
                Spacer()
                Button(action: { model.backToGlobal() }) {
                    Image(systemName: "xmark")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundColor(PdigV2Colors.textSecondary)
                        .frame(width: 44, height: 44)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel(VCopy.backToGlobal)
            }
            Text("\(region.cardCount) 张卡 · \(region.phoneCount) 个号码 · \(region.accountCount) 个账户 · \(region.serviceCount) 项服务")
                .font(VFont.body())
                .foregroundColor(PdigV2Colors.textSecondary)
            Text(VCopy.drawerViewAll)
                .font(VFont.meta())
                .foregroundColor(PdigV2Colors.textMuted)

            VStack(spacing: VSpace.sm) {
                drawerAction(VCopy.drawerViewAll, VCopy.drawerViewHintAll) {
                    model.selectRegion(region.regionCode)
                    model.navigate(.overview)
                }
                drawerAction(VCopy.drawerViewCards, VCopy.drawerViewHintCards) {
                    model.selectRegion(region.regionCode)
                    model.navigate(.cards)
                }
                drawerAction(VCopy.drawerViewNumbers, VCopy.drawerViewHintNumbers) {
                    model.selectRegion(region.regionCode)
                    model.navigate(.numbers)
                }
            }

            Text(VCopy.backToGlobal)
                .font(VFont.meta())
                .foregroundColor(PdigV2Colors.textSecondary)
                .padding(.horizontal, VSpace.md)
                .padding(.vertical, VSpace.sm)
                .frame(maxWidth: .infinity, alignment: .center)
                .background(PdigV2Colors.primarySoft)
                .clipShape(RoundedRectangle(cornerRadius: VRadius.md, style: .continuous))
        }
        .padding(VSpace.xl)
        .background(PdigV2Colors.surface.opacity(0.97))
        .clipShape(RoundedRectangle(cornerRadius: VRadius.xl, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: VRadius.xl, style: .continuous)
                .stroke(PdigV2Colors.borderStrong, lineWidth: 1)
        )
        .padding(VSpace.pagePadding)
    }

    private func drawerAction(_ title: String, _ hint: String, _ action: @escaping () -> Void) -> some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(VFont.secondary())
                    .fontWeight(.semibold)
                    .foregroundColor(PdigV2Colors.primaryBright)
                Text(hint)
                    .font(VFont.meta())
                    .foregroundColor(PdigV2Colors.textMuted)
            }
            .padding(VSpace.md)
            .frame(maxWidth: .infinity, alignment: .leading)
            .frame(minHeight: 48)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .background(PdigV2Colors.surfaceRaised)
        .clipShape(RoundedRectangle(cornerRadius: VRadius.md, style: .continuous))
    }
}
