import SwiftUI
import PDIGCore

/// iOS Production VNext pilot.
///
/// Every rendered value comes from the lock-gated VIosProductionSnapshot. Change
/// mutations go back through AppSession's guarded Production authority; there is
/// no VNextDemoFixture fallback and the UI never fabricates completion locally.
struct VIosProductionVNextPilotView: View {
    @ObservedObject var session: AppSession

    @State private var selectedRoot: Root = .now
    @State private var selectedCoreAssetKind: ProductionCoreAssetKind? = nil
    @State private var selectedSecondaryKind: VSecondaryKind? = nil
    @State private var weaknessesShown = false
    @State private var selectedObjectId: String? = nil
    @State private var regionFilter: String? = nil
    @State private var privacyMask = false
    @State private var changeError: String? = nil

    enum Root: Hashable {
        case now
        case infrastructure
        case change
        case records
        case me
    }

    enum ProductionCoreAssetKind: String, Hashable, CaseIterable {
        case card
        case number

        var route: String { self == .card ? "cards" : "numbers" }
        var title: String { self == .card ? "卡片" : "号码" }
        var glyph: String { self == .card ? "creditcard.fill" : "phone.fill" }
        var surfaceKind: VIosProductionSurfaceKind {
            self == .card ? .paymentAsset : .phoneIdentity
        }
    }

    private var snapshot: VIosProductionSnapshot? {
        session.productionVNextSnapshot()
    }

    var body: some View {
        Group {
            if let snapshot {
                TabView(selection: $selectedRoot) {
                    NavigationStack {
                        productionNow(snapshot)
                    }
                    .tabItem {
                        Label("现在", systemImage: "house.fill")
                            .accessibilityIdentifier("pdig.ios.production-vnext.nav.now")
                    }
                    .tag(Root.now)
                    .accessibilityIdentifier("pdig.ios.production-vnext.nav.now")

                    NavigationStack {
                        productionInfrastructure(snapshot)
                    }
                    .tabItem {
                        Label("基础设施", systemImage: "square.grid.2x2.fill")
                            .accessibilityIdentifier("pdig.ios.production-vnext.nav.infrastructure")
                    }
                    .tag(Root.infrastructure)
                    .accessibilityIdentifier("pdig.ios.production-vnext.nav.infrastructure")

                    NavigationStack {
                        productionChange(snapshot)
                    }
                    .tabItem {
                        Label("变更", systemImage: "arrow.triangle.2.circlepath")
                            .accessibilityIdentifier("pdig.ios.production-vnext.nav.change")
                    }
                    .tag(Root.change)
                    .accessibilityIdentifier("pdig.ios.production-vnext.nav.change")

                    NavigationStack {
                        productionRecords(snapshot)
                    }
                    .tabItem {
                        Label("记录", systemImage: "clock")
                            .accessibilityIdentifier("pdig.ios.production-vnext.nav.records")
                    }
                    .tag(Root.records)
                    .accessibilityIdentifier("pdig.ios.production-vnext.nav.records")

                    NavigationStack {
                        productionMe(snapshot)
                    }
                    .tabItem {
                        Label("我", systemImage: "person.crop.circle.fill")
                            .accessibilityIdentifier("pdig.ios.production-vnext.nav.me")
                    }
                    .tag(Root.me)
                    .accessibilityIdentifier("pdig.ios.production-vnext.nav.me")
                }
                .accessibilityIdentifier("pdig.ios.production-vnext.root")
            } else {
                VStack(spacing: 12) {
                    Image(systemName: "lock.fill")
                        .font(.system(size: 28, weight: .semibold))
                    Text("请先解锁")
                        .font(VFont.sectionTitle())
                    Text("未解锁时不会读取或展示个人数字基础设施数据。")
                        .font(VFont.secondary())
                        .foregroundColor(PdigV2Colors.textSecondary)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .vPageBackground()
                .accessibilityIdentifier("pdig.ios.production-vnext.locked")
            }
        }
        .preferredColorScheme(.light)
        .tint(PdigV2Colors.primaryBright)
    }

    @ViewBuilder
    private func productionNow(_ snapshot: VIosProductionSnapshot) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: VSpace.sectionGap) {
                pageHeader(
                    "现在",
                    "基于已记录事实展示需要关注的工作；未知不等于安全。"
                )

                HStack(spacing: VSpace.gridGap) {
                    metricCard("\(snapshot.objects.count)", "已记录对象")
                    metricCard("\(snapshot.confirmedDependencies.count)", "已确认关系")
                }
                HStack(spacing: VSpace.gridGap) {
                    metricCard("\(snapshot.pendingReview.total)", "待复核")
                    metricCard("\(snapshot.plans.filter { $0.workflowState != "completed" && $0.workflowState != "cancelled" }.count)", "进行中变更")
                }

                if snapshot.pendingReview.total > 0 {
                    infoCard(
                        title: "有内容需要复核",
                        detail: "待确认关系、候选对象或变化记录仍未进入已确认事实。"
                    )
                } else {
                    infoCard(
                        title: "当前没有待复核项目",
                        detail: "这只表示当前复核队列为空，不代表所有数字依赖都已记录完整。"
                    )
                }

                if snapshot.plans.contains(where: { $0.needsReanalysis }) {
                    infoCard(
                        title: "部分变更需要重新分析",
                        detail: "基础设施数据版本已变化；旧分析结果不会自动当作当前结论。"
                    )
                }
            }
            .padding(VSpace.pagePadding)
        }
        .vPageBackground()
        .accessibilityIdentifier("pdig.ios.production-vnext.now")
    }

    @ViewBuilder
    private func productionInfrastructure(_ snapshot: VIosProductionSnapshot) -> some View {
        if weaknessesShown {
            productionWeaknesses(
                session.productionVNextFindings(nowIso: PdigClock.nowIso()) ?? []
            )
        } else if let kind = selectedCoreAssetKind {
            if let objectId = selectedObjectId,
               let object = snapshot.objects.first(where: { $0.id == objectId }) {
                productionCoreAssetDetail(snapshot, kind: kind, object: object)
            } else {
                productionCoreAssetCollection(snapshot, kind: kind)
            }
        } else if let kind = selectedSecondaryKind {
            if let objectId = selectedObjectId,
               let object = snapshot.objects.first(where: { $0.id == objectId }) {
                productionSecondaryDetail(snapshot, kind: kind, object: object)
            } else {
                productionSecondaryCollection(snapshot, kind: kind)
            }
        } else {
            ScrollView {
                VStack(alignment: .leading, spacing: VSpace.sectionGap) {
                    pageHeader(
                        "我的基础设施",
                        "对象类型与地区只来自已确认记录；未记录不会被猜测。"
                    )

                    confirmedRegionScope(snapshot)

                    ForEach(ProductionCoreAssetKind.allCases, id: \.self) { kind in
                        let count = vIosProductionObjects(
                            snapshot: snapshot,
                            surfaceKind: kind.surfaceKind,
                            regionFilter: regionFilter
                        ).count
                        Button {
                            weaknessesShown = false
                            selectedCoreAssetKind = kind
                            selectedSecondaryKind = nil
                            selectedObjectId = nil
                        } label: {
                            HStack(spacing: VSpace.lg) {
                                Image(systemName: kind.glyph)
                                    .font(.system(size: 18, weight: .semibold))
                                    .foregroundColor(PdigV2Colors.primaryBright)
                                    .frame(width: 44, height: 44)
                                    .background(PdigV2Colors.primarySoft)
                                    .clipShape(RoundedRectangle(cornerRadius: VRadius.md, style: .continuous))
                                VStack(alignment: .leading, spacing: 3) {
                                    Text(kind.title)
                                        .font(VFont.body())
                                        .fontWeight(.semibold)
                                        .foregroundColor(PdigV2Colors.textPrimary)
                                    Text("\(count) 个已记录对象")
                                        .font(VFont.meta())
                                        .foregroundColor(PdigV2Colors.textMuted)
                                }
                                Spacer()
                                Image(systemName: "chevron.right")
                                    .foregroundColor(PdigV2Colors.textMuted)
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
                        .accessibilityIdentifier(
                            "pdig.ios.production-vnext.infrastructure.\(kind.route)"
                        )
                    }

                    ForEach(VSecondaryKind.allCases, id: \.self) { kind in
                        let count = vIosProductionSecondaryObjects(
                            snapshot: snapshot,
                            kind: kind,
                            regionFilter: regionFilter
                        ).count
                        Button {
                            weaknessesShown = false
                            selectedCoreAssetKind = nil
                            selectedSecondaryKind = kind
                            selectedObjectId = nil
                        } label: {
                            HStack(spacing: VSpace.lg) {
                                Image(systemName: kind.glyph)
                                    .font(.system(size: 18, weight: .semibold))
                                    .foregroundColor(PdigV2Colors.primaryBright)
                                    .frame(width: 44, height: 44)
                                    .background(PdigV2Colors.primarySoft)
                                    .clipShape(RoundedRectangle(cornerRadius: VRadius.md, style: .continuous))

                                VStack(alignment: .leading, spacing: 3) {
                                    Text(kind.title)
                                        .font(VFont.body())
                                        .fontWeight(.semibold)
                                        .foregroundColor(PdigV2Colors.textPrimary)
                                    Text("\(count) 个已记录对象")
                                        .font(VFont.meta())
                                        .foregroundColor(PdigV2Colors.textMuted)
                                }
                                Spacer()
                                Image(systemName: "chevron.right")
                                    .foregroundColor(PdigV2Colors.textMuted)
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
                        .accessibilityIdentifier(
                            "pdig.ios.production-vnext.infrastructure.\(kind.route)"
                        )
                    }

                    let findingCount = session.productionVNextFindings(nowIso: PdigClock.nowIso())?.count ?? 0
                    Button {
                        weaknessesShown = true
                        selectedCoreAssetKind = nil
                        selectedSecondaryKind = nil
                        selectedObjectId = nil
                    } label: {
                        HStack(spacing: VSpace.lg) {
                            Image(systemName: "exclamationmark.triangle.fill")
                                .font(.system(size: 18, weight: .semibold))
                                .foregroundColor(PdigV2Colors.warning)
                                .frame(width: 44, height: 44)
                                .background(PdigV2Colors.warning.opacity(0.12))
                                .clipShape(RoundedRectangle(cornerRadius: VRadius.md, style: .continuous))
                            VStack(alignment: .leading, spacing: 3) {
                                Text("薄弱点")
                                    .font(VFont.body())
                                    .fontWeight(.semibold)
                                    .foregroundColor(PdigV2Colors.textPrimary)
                                Text("\(findingCount) 项连续性发现")
                                    .font(VFont.meta())
                                    .foregroundColor(PdigV2Colors.textMuted)
                            }
                            Spacer()
                            Image(systemName: "chevron.right")
                                .foregroundColor(PdigV2Colors.textMuted)
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
                    .accessibilityIdentifier("pdig.ios.production-vnext.infrastructure.weaknesses")
                }
                .padding(VSpace.pagePadding)
            }
            .vPageBackground()
            .accessibilityIdentifier("pdig.ios.production-vnext.infrastructure")
        }
    }

    private func confirmedRegionScope(_ snapshot: VIosProductionSnapshot) -> some View {
        let regionCodes = Array(
            Set(snapshot.objects.flatMap { $0.confirmedRegionCodes })
        ).sorted()

        return VStack(alignment: .leading, spacing: VSpace.sm) {
            VSectionHeader(title: "地区范围")
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: VSpace.sm) {
                    Button {
                        regionFilter = nil
                    } label: {
                        VChip(text: "全球", highlight: regionFilter == nil)
                    }
                    .buttonStyle(.plain)
                    .accessibilityIdentifier("pdig.ios.production-vnext.region.global")

                    ForEach(regionCodes, id: \.self) { code in
                        Button {
                            regionFilter = code
                        } label: {
                            VChip(text: code, highlight: regionFilter == code)
                        }
                        .buttonStyle(.plain)
                        .accessibilityIdentifier("pdig.ios.production-vnext.region.\(code)")
                    }
                }
            }
            if regionCodes.isEmpty {
                Text("尚无已确认地区")
                    .font(VFont.meta())
                    .foregroundColor(PdigV2Colors.unknown)
            } else {
                Text("地区筛选只使用已确认地区事实；地区未记录的对象不会被归入任一地区。")
                    .font(VFont.meta())
                    .foregroundColor(PdigV2Colors.textMuted)
            }
        }
        .padding(VSpace.lg)
        .background(PdigV2Colors.surface)
        .clipShape(RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous))
    }

    private func productionCoreAssetCollection(
        _ snapshot: VIosProductionSnapshot,
        kind: ProductionCoreAssetKind
    ) -> some View {
        let items = vIosProductionObjects(
            snapshot: snapshot,
            surfaceKind: kind.surfaceKind,
            regionFilter: regionFilter
        )

        return ScrollView {
            VStack(alignment: .leading, spacing: VSpace.sectionGap) {
                HStack {
                    Button {
                        selectedCoreAssetKind = nil
                        selectedObjectId = nil
                    } label: {
                        Label("返回", systemImage: "chevron.left")
                    }
                    .buttonStyle(.plain)
                    .accessibilityIdentifier(
                        "pdig.ios.production-vnext.collection.back.\(kind.route)"
                    )
                    Spacer()
                }

                pageHeader(
                    kind.title,
                    regionFilter.map { "已确认地区 \($0) · \(items.count) 个已记录对象" }
                        ?? "全球 · \(items.count) 个已记录对象"
                )

                if items.isEmpty {
                    infoCard(
                        title: "当前范围没有\(kind.title)记录",
                        detail: kind == .number
                            ? "只有已确认手机号身份才会进入这里；名字或号码格式不会自动升级 subtype。"
                            : "没有记录不代表不存在，也不代表没有支付或控制关系。"
                    )
                }

                ForEach(items) { object in
                    Button {
                        selectedObjectId = object.id
                    } label: {
                        HStack(spacing: VSpace.lg) {
                            Image(systemName: kind.glyph)
                                .font(.system(size: 18, weight: .semibold))
                                .foregroundColor(PdigV2Colors.primaryBright)
                                .frame(width: 44, height: 44)
                                .background(PdigV2Colors.primarySoft)
                                .clipShape(RoundedRectangle(cornerRadius: VRadius.md, style: .continuous))

                            VStack(alignment: .leading, spacing: 3) {
                                Text(vIosVisibleProductionObjectName(object, privacyMask: privacyMask))
                                    .font(VFont.body())
                                    .fontWeight(.semibold)
                                    .foregroundColor(PdigV2Colors.textPrimary)
                                    .lineLimit(1)
                                if let subtitle = coreAssetSubtitle(object, kind: kind) {
                                    Text(subtitle)
                                        .font(VFont.meta())
                                        .foregroundColor(PdigV2Colors.textSecondary)
                                        .lineLimit(1)
                                }
                                Text("\(object.confirmedDependencyCount) 条已确认关系")
                                    .font(VFont.meta())
                                    .foregroundColor(PdigV2Colors.textMuted)
                            }
                            Spacer()
                            Image(systemName: "chevron.right")
                                .foregroundColor(PdigV2Colors.textMuted)
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
                    .accessibilityIdentifier(
                        "pdig.ios.production-vnext.object.\(kind.route).\(object.id)"
                    )
                }
            }
            .padding(VSpace.pagePadding)
        }
        .vPageBackground()
        .accessibilityIdentifier("pdig.ios.production-vnext.collection.\(kind.route)")
    }

    private func productionCoreAssetDetail(
        _ snapshot: VIosProductionSnapshot,
        kind: ProductionCoreAssetKind,
        object: VIosProductionObject
    ) -> some View {
        let relationships = snapshot.confirmedDependencies.filter {
            $0.fromId == object.id || $0.toId == object.id
        }

        return ScrollView {
            VStack(alignment: .leading, spacing: VSpace.sectionGap) {
                HStack {
                    Button { selectedObjectId = nil } label: {
                        Label("返回", systemImage: "chevron.left")
                    }
                    .buttonStyle(.plain)
                    Spacer()
                }

                VStack(alignment: .leading, spacing: 6) {
                    Text(vIosVisibleProductionObjectName(object, privacyMask: privacyMask))
                        .font(VFont.pageTitle())
                        .foregroundColor(PdigV2Colors.textPrimary)
                    Text(kind == .card ? "支付工具" : "手机号身份")
                        .font(VFont.meta())
                        .foregroundColor(PdigV2Colors.textMuted)
                    if let subtitle = coreAssetSubtitle(object, kind: kind) {
                        Text(subtitle)
                            .font(VFont.secondary())
                            .foregroundColor(PdigV2Colors.textSecondary)
                    }
                    Text(
                        object.confirmedRegionCodes.isEmpty
                            ? "地区未记录"
                            : "已确认地区 " + object.confirmedRegionCodes.joined(separator: " · ")
                    )
                    .font(VFont.meta())
                    .foregroundColor(
                        object.confirmedRegionCodes.isEmpty
                            ? PdigV2Colors.unknown
                            : PdigV2Colors.textMuted
                    )
                }
                .padding(VSpace.xl)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(PdigV2Colors.surface)
                .clipShape(RoundedRectangle(cornerRadius: VRadius.xl, style: .continuous))

                productionRelationshipSummary(
                    snapshot: snapshot,
                    object: object,
                    relationships: relationships
                )

                VStack(alignment: .leading, spacing: VSpace.md) {
                    VSectionHeader(title: "如果它发生变化？")
                    HStack(spacing: VSpace.gridGap) {
                        metricCard("\(relationships.count)", "已确认依赖")
                        metricCard("未知", "独立备用")
                        metricCard("未知", "唯一恢复")
                    }
                    Text("未知不等于安全；关系数量也不等于独立恢复路径数量。")
                        .font(VFont.meta())
                        .foregroundColor(PdigV2Colors.unknown)
                }
                .padding(VSpace.lg)
                .background(PdigV2Colors.surface)
                .clipShape(RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous))
            }
            .padding(VSpace.pagePadding)
        }
        .vPageBackground()
        .accessibilityIdentifier("pdig.ios.production-vnext.detail.\(kind.route).\(object.id)")
    }

    private func productionRelationshipSummary(
        snapshot: VIosProductionSnapshot,
        object: VIosProductionObject,
        relationships: [VIosProductionDependency]
    ) -> some View {
        VStack(alignment: .leading, spacing: VSpace.md) {
            VSectionHeader(title: "已确认关系")
            if relationships.isEmpty {
                Text("未记录已确认关系")
                    .font(VFont.secondary())
                    .foregroundColor(PdigV2Colors.unknown)
            }
            ForEach(relationships) { dependency in
                let peerId = dependency.fromId == object.id
                    ? dependency.toId
                    : dependency.fromId
                let fallback = dependency.fromId == object.id
                    ? dependency.toName
                    : dependency.fromName
                let peerName = visiblePeerName(
                    snapshot: snapshot,
                    peerId: peerId,
                    fallback: fallback
                )
                VStack(alignment: .leading, spacing: 3) {
                    Text(peerName)
                        .font(VFont.body())
                        .fontWeight(.semibold)
                        .foregroundColor(PdigV2Colors.textPrimary)
                    Text("\(productionRelationLabel(dependency.relation)) · \(productionCapabilityLabel(dependency.capability))")
                        .font(VFont.meta())
                        .foregroundColor(PdigV2Colors.textMuted)
                }
                .padding(VSpace.md)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(PdigV2Colors.surfaceRaised)
                .clipShape(RoundedRectangle(cornerRadius: VRadius.md, style: .continuous))
            }
        }
        .padding(VSpace.lg)
        .background(PdigV2Colors.surface)
        .clipShape(RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous))
    }

    private func coreAssetSubtitle(
        _ object: VIosProductionObject,
        kind: ProductionCoreAssetKind
    ) -> String? {
        if kind == .number {
            return visibleIdentifier(object) ?? "已确认手机号身份"
        }
        if privacyMask {
            return "发行方与尾号已遮蔽"
        }
        let issuer = object.issuer?
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .nonEmpty
        let last4 = object.last4?
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .nonEmpty
        let parts = [
            issuer,
            last4.map { "•••• \($0)" },
        ].compactMap { $0 }
        return parts.isEmpty ? nil : parts.joined(separator: " · ")
    }

    private func productionSecondaryCollection(
        _ snapshot: VIosProductionSnapshot,
        kind: VSecondaryKind
    ) -> some View {
        let items = vIosProductionSecondaryObjects(
            snapshot: snapshot,
            kind: kind,
            regionFilter: regionFilter
        )

        return ScrollView {
            VStack(alignment: .leading, spacing: VSpace.sectionGap) {
                HStack {
                    Button {
                        selectedSecondaryKind = nil
                        selectedObjectId = nil
                    } label: {
                        Label("返回", systemImage: "chevron.left")
                    }
                    .buttonStyle(.plain)
                    Spacer()
                }

                pageHeader(
                    kind.title,
                    regionFilter.map { "已确认地区 \($0) · \(items.count) 个已记录对象" }
                        ?? "全球 · \(items.count) 个已记录对象"
                )

                if items.isEmpty {
                    infoCard(
                        title: "当前范围没有\(kind.title)记录",
                        detail: "没有记录不代表不存在，也不代表没有恢复或控制关系。"
                    )
                }

                ForEach(items) { object in
                    Button {
                        selectedObjectId = object.id
                    } label: {
                        HStack(spacing: VSpace.lg) {
                            Image(systemName: kind.glyph)
                                .font(.system(size: 18, weight: .semibold))
                                .foregroundColor(PdigV2Colors.primaryBright)
                                .frame(width: 44, height: 44)
                                .background(PdigV2Colors.primarySoft)
                                .clipShape(RoundedRectangle(cornerRadius: VRadius.md, style: .continuous))

                            VStack(alignment: .leading, spacing: 3) {
                                Text(vIosVisibleProductionObjectName(object, privacyMask: privacyMask))
                                    .font(VFont.body())
                                    .fontWeight(.semibold)
                                    .foregroundColor(PdigV2Colors.textPrimary)
                                    .lineLimit(1)
                                if let identifier = visibleIdentifier(object) {
                                    Text(identifier)
                                        .font(VFont.meta())
                                        .foregroundColor(PdigV2Colors.textSecondary)
                                        .lineLimit(1)
                                }
                                Text("\(object.confirmedDependencyCount) 条已确认关系")
                                    .font(VFont.meta())
                                    .foregroundColor(PdigV2Colors.textMuted)
                            }
                            Spacer()
                            Image(systemName: "chevron.right")
                                .foregroundColor(PdigV2Colors.textMuted)
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
                    .accessibilityIdentifier(
                        "pdig.ios.production-vnext.object.\(kind.route).\(object.id)"
                    )
                }
            }
            .padding(VSpace.pagePadding)
        }
        .vPageBackground()
        .accessibilityIdentifier("pdig.ios.production-vnext.collection.\(kind.route)")
    }

    private func productionSecondaryDetail(
        _ snapshot: VIosProductionSnapshot,
        kind: VSecondaryKind,
        object: VIosProductionObject
    ) -> some View {
        let relationships = snapshot.confirmedDependencies.filter {
            $0.fromId == object.id || $0.toId == object.id
        }

        return ScrollView {
            VStack(alignment: .leading, spacing: VSpace.sectionGap) {
                HStack {
                    Button {
                        selectedObjectId = nil
                    } label: {
                        Label("返回", systemImage: "chevron.left")
                    }
                    .buttonStyle(.plain)
                    Spacer()
                }

                VStack(alignment: .leading, spacing: 6) {
                    Text(vIosVisibleProductionObjectName(object, privacyMask: privacyMask))
                        .font(VFont.pageTitle())
                        .foregroundColor(PdigV2Colors.textPrimary)
                    Text(kind.title)
                        .font(VFont.meta())
                        .foregroundColor(PdigV2Colors.textMuted)
                    if let identifier = visibleIdentifier(object) {
                        Text(identifier)
                            .font(VFont.secondary())
                            .foregroundColor(PdigV2Colors.textSecondary)
                    }
                    Text(
                        object.confirmedRegionCodes.isEmpty
                            ? "地区未记录"
                            : "已确认地区 " + object.confirmedRegionCodes.joined(separator: " · ")
                    )
                    .font(VFont.meta())
                    .foregroundColor(
                        object.confirmedRegionCodes.isEmpty
                            ? PdigV2Colors.unknown
                            : PdigV2Colors.textMuted
                    )
                }
                .padding(VSpace.xl)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(PdigV2Colors.surface)
                .clipShape(RoundedRectangle(cornerRadius: VRadius.xl, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: VRadius.xl, style: .continuous)
                        .stroke(PdigV2Colors.borderSubtle, lineWidth: 1)
                )

                VStack(alignment: .leading, spacing: VSpace.md) {
                    VSectionHeader(title: "已确认关系")
                    if relationships.isEmpty {
                        Text("未记录已确认关系")
                            .font(VFont.secondary())
                            .foregroundColor(PdigV2Colors.unknown)
                    }
                    ForEach(relationships) { dependency in
                        let peerId = dependency.fromId == object.id
                            ? dependency.toId
                            : dependency.fromId
                        let fallback = dependency.fromId == object.id
                            ? dependency.toName
                            : dependency.fromName
                        let peerName = visiblePeerName(
                            snapshot: snapshot,
                            peerId: peerId,
                            fallback: fallback
                        )
                        VStack(alignment: .leading, spacing: 3) {
                            Text(peerName)
                                .font(VFont.body())
                                .fontWeight(.semibold)
                                .foregroundColor(PdigV2Colors.textPrimary)
                            Text("\(productionRelationLabel(dependency.relation)) · \(productionCapabilityLabel(dependency.capability))")
                                .font(VFont.meta())
                                .foregroundColor(PdigV2Colors.textMuted)
                        }
                        .padding(VSpace.md)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(PdigV2Colors.surfaceRaised)
                        .clipShape(RoundedRectangle(cornerRadius: VRadius.md, style: .continuous))
                    }
                }
                .padding(VSpace.lg)
                .background(PdigV2Colors.surface)
                .clipShape(RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous))

                VStack(alignment: .leading, spacing: VSpace.md) {
                    VSectionHeader(title: "如果它发生变化？")
                    HStack(spacing: VSpace.gridGap) {
                        metricCard("\(relationships.count)", "已确认依赖")
                        metricCard("未知", "独立备用")
                        metricCard("未知", "唯一恢复")
                    }
                    Text("未知不等于安全；关系数量也不等于独立恢复路径数量。")
                        .font(VFont.meta())
                        .foregroundColor(PdigV2Colors.unknown)
                }
                .padding(VSpace.lg)
                .background(PdigV2Colors.surface)
                .clipShape(RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous))
            }
            .padding(VSpace.pagePadding)
        }
        .vPageBackground()
        .accessibilityIdentifier("pdig.ios.production-vnext.detail.\(object.id)")
    }

    private func productionWeaknesses(_ findings: [FindingItem]) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: VSpace.sectionGap) {
                HStack {
                    Button {
                        weaknessesShown = false
                    } label: {
                        Label("返回", systemImage: "chevron.left")
                    }
                    .buttonStyle(.plain)
                    .accessibilityIdentifier("pdig.ios.production-vnext.weaknesses.back")
                    Spacer()
                }

                pageHeader(
                    "薄弱点",
                    "只显示从当前已确认关系与计划状态推导的连续性发现。"
                )

                if findings.isEmpty {
                    infoCard(
                        title: "当前没有可展示的连续性发现",
                        detail: "这不代表记录完整或基础设施安全；未记录与未知仍然保持未知。"
                    )
                }

                ForEach(findings) { finding in
                    VStack(alignment: .leading, spacing: VSpace.sm) {
                        HStack {
                            Text(productionFindingKindLabel(finding.kind))
                                .font(VFont.meta())
                                .fontWeight(.semibold)
                                .foregroundColor(PdigV2Colors.warning)
                            Spacer()
                            Text(finding.affectedCapability)
                                .font(VFont.meta())
                                .foregroundColor(PdigV2Colors.textMuted)
                        }
                        Text(finding.title)
                            .font(VFont.body())
                            .fontWeight(.semibold)
                            .foregroundColor(PdigV2Colors.textPrimary)

                        if privacyMask {
                            Text("发现说明已遮蔽；状态与发现类型仍可见。")
                                .font(VFont.secondary())
                                .foregroundColor(PdigV2Colors.textSecondary)
                        } else {
                            Text(finding.what)
                                .font(VFont.secondary())
                                .foregroundColor(PdigV2Colors.textSecondary)
                            Text("依据：\(finding.confirmedBasis)")
                                .font(VFont.meta())
                                .foregroundColor(PdigV2Colors.textMuted)
                            Text("未知：\(finding.unknowns)")
                                .font(VFont.meta())
                                .foregroundColor(PdigV2Colors.unknown)
                            Text("下一步：\(finding.recommendedNextAction)")
                                .font(VFont.meta())
                                .foregroundColor(PdigV2Colors.primaryBright)
                        }
                    }
                    .padding(VSpace.lg)
                    .background(PdigV2Colors.surface)
                    .clipShape(RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous))
                    .overlay(
                        RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous)
                            .stroke(PdigV2Colors.borderSubtle, lineWidth: 1)
                    )
                    .accessibilityIdentifier(
                        "pdig.ios.production-vnext.weakness.\(finding.id)"
                    )
                }
            }
            .padding(VSpace.pagePadding)
        }
        .vPageBackground()
        .accessibilityIdentifier("pdig.ios.production-vnext.weaknesses")
    }

    private func productionFindingKindLabel(_ kind: FindingItem.Kind) -> String {
        switch kind {
        case .singlePointOfFailure: return "单一路径"
        case .sharedFailureDomain: return "共享故障点"
        case .recoveryCycle: return "恢复循环"
        case .unconfirmedFallback: return "备用路径待确认"
        case .staleRecoveryInformation: return "信息待复核"
        case .unknownCriticalPath: return "关键性未知"
        case .pendingVerification: return "待验证"
        }
    }

    @ViewBuilder
    private func productionChange(_ snapshot: VIosProductionSnapshot) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: VSpace.sectionGap) {
                pageHeader(
                    "变更",
                    "按正式计划记录完成与独立验证；计划预期不会被当成已经发生的事实。"
                )

                if let changeError {
                    infoCard(title: "这一步暂时不能执行", detail: changeError)
                }

                if snapshot.plans.isEmpty {
                    infoCard(
                        title: "还没有变更计划",
                        detail: "这里不会用演示计划替代真实记录。"
                    )
                }

                ForEach(snapshot.plans) { plan in
                    let stages = vIosProductionChangeStageSummary(plan)
                    VStack(alignment: .leading, spacing: VSpace.md) {
                        HStack {
                            Text(privacyMask ? "变更计划（已遮蔽）" : plan.title)
                                .font(VFont.body())
                                .fontWeight(.semibold)
                                .foregroundColor(PdigV2Colors.textPrimary)
                            Spacer()
                            VStatusBadge(status: plan.needsReanalysis ? "warning" : plan.workflowState)
                        }

                        VStack(spacing: VSpace.sm) {
                            productionChangeStageRow(
                                index: 1,
                                title: "当前",
                                detail: stages.current,
                                highlighted: false
                            )
                            productionChangeStageRow(
                                index: 2,
                                title: "过渡中",
                                detail: stages.transition,
                                highlighted: true
                            )
                            productionChangeStageRow(
                                index: 3,
                                title: "完成后（计划）",
                                detail: stages.after,
                                highlighted: false
                            )
                        }
                        .accessibilityIdentifier(
                            "pdig.ios.production-vnext.change.stages.\(plan.id)"
                        )

                        Text("完成 ≠ 已验证；完成后只是计划投影，不会被当成已经发生的事实。")
                            .font(VFont.meta())
                            .foregroundColor(PdigV2Colors.textMuted)

                        if plan.needsReanalysis {
                            Text("基础设施数据版本已变化，需要重新分析当前影响范围。")
                                .font(VFont.meta())
                                .foregroundColor(PdigV2Colors.warning)
                        }

                        if let authoritativePlan = session.plans.first(where: { $0.id == plan.id }) {
                            VStack(spacing: VSpace.sm) {
                                ForEach(plan.actions) { action in
                                    productionChangeActionRow(
                                        snapshot: snapshot,
                                        plan: plan,
                                        authoritativePlan: authoritativePlan,
                                        action: action
                                    )
                                }
                            }
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
        .accessibilityIdentifier("pdig.ios.production-vnext.change")
    }

    private struct ProductionRecordRow: Identifiable {
        let plan: VIosProductionPlan
        let action: VIosProductionPlanAction
        var id: String { "\(plan.id):\(action.id)" }
    }

    @ViewBuilder
    private func productionRecords(_ snapshot: VIosProductionSnapshot) -> some View {
        let records = snapshot.plans.flatMap { plan in
            plan.actions.compactMap { action -> ProductionRecordRow? in
                guard vIosProductionActionHasRecordedEvent(action) else {
                    return nil
                }
                return ProductionRecordRow(plan: plan, action: action)
            }
        }

        ScrollView {
            VStack(alignment: .leading, spacing: VSpace.sectionGap) {
                pageHeader(
                    "记录",
                    "已记录完成与独立验证分开显示；没有时间事实就不补本机时间。"
                )

                if records.isEmpty {
                    infoCard(
                        title: "暂无操作记录",
                        detail: "未来计划步骤不会提前写进记录。"
                    )
                }

                ForEach(records) { item in
                    let plan = item.plan
                    let action = item.action
                    VStack(alignment: .leading, spacing: VSpace.sm) {
                        Text(privacyMask ? "操作步骤（已遮蔽）" : action.title)
                            .font(VFont.body())
                            .fontWeight(.semibold)
                            .foregroundColor(PdigV2Colors.textPrimary)
                        Text(
                            privacyMask
                                ? "变更计划（已遮蔽）"
                                : plan.title
                        )
                        .font(VFont.meta())
                        .foregroundColor(PdigV2Colors.textSecondary)
                        Text(recordState(action))
                            .font(VFont.meta())
                            .foregroundColor(
                                action.verificationStatus == "verified"
                                    ? PdigV2Colors.positive
                                    : PdigV2Colors.warning
                            )
                        Text(vIosProductionRecordEvidenceText(action))
                            .font(VFont.meta())
                            .foregroundColor(PdigV2Colors.textMuted)
                        Text(
                            action.verificationStatus == "verified"
                                ? "验证时间未记录"
                                : "完成时间未记录"
                        )
                        .font(VFont.meta())
                        .foregroundColor(PdigV2Colors.unknown)
                    }
                    .padding(VSpace.lg)
                    .background(PdigV2Colors.surface)
                    .clipShape(RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous))
                }
            }
            .padding(VSpace.pagePadding)
        }
        .vPageBackground()
        .accessibilityIdentifier("pdig.ios.production-vnext.records")
    }

    @ViewBuilder
    private func productionMe(_ snapshot: VIosProductionSnapshot) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: VSpace.sectionGap) {
                pageHeader(
                    "我的数字生活",
                    "隐私遮蔽只改变展示，不修改已记录事实。"
                )

                Toggle("隐私遮蔽", isOn: $privacyMask)
                    .padding(VSpace.lg)
                    .background(PdigV2Colors.surface)
                    .clipShape(RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous))
                    .accessibilityIdentifier("pdig.ios.production-vnext.privacy")

                HStack(spacing: VSpace.gridGap) {
                    metricCard("\(snapshot.sourceCoverage.activeSourceCount)", "使用中的来源")
                    metricCard("\(snapshot.sourceCoverage.sourceCount)", "全部来源")
                }

                infoCard(
                    title: "数据版本 \(snapshot.revision)",
                    detail: "对象与关系来自当前已解锁数据；待复核内容不会自动进入已确认关系。"
                )
            }
            .padding(VSpace.pagePadding)
        }
        .vPageBackground()
        .accessibilityIdentifier("pdig.ios.production-vnext.me")
    }

    private func visibleIdentifier(_ object: VIosProductionObject) -> String? {
        guard let value = object.identityIdentifierValue, !value.isEmpty else { return nil }
        if !privacyMask { return value }
        switch object.surfaceKind {
        case .phoneIdentity: return "号码已遮蔽"
        case .emailIdentity: return "邮箱已遮蔽"
        default: return "标识已遮蔽"
        }
    }

    private func visiblePeerName(
        snapshot: VIosProductionSnapshot,
        peerId: String,
        fallback: String
    ) -> String {
        if let peer = snapshot.objects.first(where: { $0.id == peerId }) {
            return vIosVisibleProductionObjectName(peer, privacyMask: privacyMask)
        }
        return privacyMask ? "关联对象（已遮蔽）" : fallback
    }

    private func productionRelationLabel(_ wire: String) -> String {
        switch wire {
        case "funding_source": return "资金来源"
        case "merchant_agreement": return "支付协议"
        case "authenticates": return "登录验证"
        case "recovers": return "恢复路径"
        case "controls": return "控制关系"
        case "registered_with": return "注册关系"
        case "owns": return "拥有关系"
        case "uses": return "使用关系"
        default: return "已确认关系"
        }
    }

    private func productionCapabilityLabel(_ wire: String) -> String {
        switch wire {
        case "payment": return "支付"
        case "recovery": return "恢复"
        case "authentication": return "身份验证"
        case "access": return "访问"
        case "communication": return "通信"
        case "identity": return "身份"
        case "control": return "控制"
        default: return "能力未分类"
        }
    }

    @ViewBuilder
    private func productionChangeActionRow(
        snapshot: VIosProductionSnapshot,
        plan: VIosProductionPlan,
        authoritativePlan: ChangePlan,
        action: VIosProductionPlanAction
    ) -> some View {
        let canComplete = !privacyMask && ProductionChangeAuthority.canCompleteAction(
            plan: authoritativePlan,
            currentGraphRevision: snapshot.revision,
            actionId: action.id,
            pendingReviewCount: snapshot.pendingReview.proposalCount,
            pendingCandidateCount: snapshot.pendingReview.candidateCount,
            openDriftCount: snapshot.pendingReview.driftCount
        )
        let canVerify = !privacyMask && ProductionChangeAuthority.canVerifyAction(
            plan: authoritativePlan,
            currentGraphRevision: snapshot.revision,
            actionId: action.id
        )

        VStack(alignment: .leading, spacing: VSpace.sm) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 3) {
                    Text(privacyMask ? "操作步骤（已遮蔽）" : action.title)
                        .font(VFont.body())
                        .fontWeight(.semibold)
                        .foregroundColor(PdigV2Colors.textPrimary)
                    Text(recordState(action))
                        .font(VFont.meta())
                        .foregroundColor(
                            action.verificationStatus == "verified"
                                ? PdigV2Colors.positive
                                : PdigV2Colors.textMuted
                        )
                }
                Spacer(minLength: VSpace.sm)
                Text(productionActionPhaseLabel(action.phase))
                    .font(VFont.meta())
                    .foregroundColor(PdigV2Colors.textMuted)
            }

            if !action.prerequisiteActionIds.isEmpty {
                Text("需先完成 \(action.prerequisiteActionIds.count) 个前置步骤")
                    .font(VFont.meta())
                    .foregroundColor(PdigV2Colors.textSecondary)
            }

            HStack(spacing: VSpace.sm) {
                if !action.done {
                    Button("记录完成") {
                        do {
                            try session.productionCompleteAction(
                                planId: plan.id,
                                actionId: action.id
                            )
                            changeError = nil
                        } catch {
                            changeError = productionChangeErrorText(error)
                        }
                    }
                    .buttonStyle(.borderedProminent)
                    .disabled(!canComplete)
                    .accessibilityIdentifier(
                        vIosProductionChangeElementId("complete", actionId: action.id)
                    )
                }

                if action.done &&
                    (action.verificationStatus == "pending" ||
                     action.verificationStatus == "evidence_suggested") {
                    Button("确认验证") {
                        do {
                            try session.productionVerifyAction(
                                planId: plan.id,
                                actionId: action.id
                            )
                            changeError = nil
                        } catch {
                            changeError = productionChangeErrorText(error)
                        }
                    }
                    .buttonStyle(.bordered)
                    .disabled(!canVerify)
                    .accessibilityIdentifier(
                        vIosProductionChangeElementId("verify", actionId: action.id)
                    )
                }
            }
        }
        .padding(VSpace.md)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(PdigV2Colors.surfaceRaised)
        .clipShape(RoundedRectangle(cornerRadius: VRadius.md, style: .continuous))
        .accessibilityIdentifier(
            vIosProductionChangeElementId("action", actionId: action.id)
        )
    }

    private func productionActionPhaseLabel(_ wire: String) -> String {
        switch wire {
        case PlanActionPhase.prepare.wire: return "准备"
        case PlanActionPhase.change.wire: return "变更"
        case PlanActionPhase.verify.wire: return "验证"
        default: return "步骤"
        }
    }

    private func productionChangeErrorText(_ error: Error) -> String {
        guard let reason = error as? ProductionChangeMutationError else {
            return "操作没有写入；请重新检查当前计划。"
        }
        switch reason {
        case .sessionLocked:
            return "请先解锁后再操作。"
        case .planNotFound:
            return "当前计划已发生变化，请重新打开。"
        case .planFrozen:
            return "计划已经结束，不能继续修改步骤。"
        case .needsReanalysis:
            return "基础设施已经变化，请先重新分析当前影响范围。"
        case .actionNotFound:
            return "当前步骤已不在正式计划中。"
        case .actionAlreadyDone:
            return "这一步已经记录完成。"
        case .missingPrerequisite:
            return "计划的前置步骤信息不完整，暂不执行。"
        case .prerequisiteIncomplete:
            return "必须先完成前置步骤。"
        case .prerequisiteUnverified:
            return "新路径尚未独立验证，暂不能停用旧路径。"
        case .retirementScopeNeedsReview:
            return "仍有待复核内容，暂不能停用旧路径。"
        case .verificationNotRequired:
            return "这一步不需要单独验证。"
        case .actionNotDone:
            return "请先记录这一步已经完成，再确认验证结果。"
        case .verificationFrozen:
            return "当前验证状态不能再次修改。"
        }
    }

    private func productionChangeStageRow(
        index: Int,
        title: String,
        detail: String,
        highlighted: Bool
    ) -> some View {
        HStack(alignment: .top, spacing: VSpace.md) {
            Text("\(index)")
                .font(VFont.body())
                .fontWeight(.bold)
                .foregroundColor(
                    highlighted ? PdigV2Colors.primaryBright : PdigV2Colors.textMuted
                )
                .frame(width: 24)
            VStack(alignment: .leading, spacing: 3) {
                Text(title)
                    .font(VFont.body())
                    .fontWeight(.semibold)
                    .foregroundColor(PdigV2Colors.textPrimary)
                Text(detail)
                    .font(VFont.meta())
                    .foregroundColor(PdigV2Colors.textSecondary)
            }
            Spacer(minLength: 0)
        }
        .padding(VSpace.md)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            highlighted ? PdigV2Colors.primarySoft : PdigV2Colors.surfaceRaised
        )
        .clipShape(RoundedRectangle(cornerRadius: VRadius.md, style: .continuous))
    }

    private func recordState(_ action: VIosProductionPlanAction) -> String {
        switch action.verificationStatus {
        case "verified": return "已验证"
        case "failed": return "验证失败"
        case "pending", "evidence_suggested":
            return action.done ? "已完成 · 待验证" : "待验证"
        default:
            return action.done ? "已记录完成" : "未开始"
        }
    }

    private func pageHeader(_ title: String, _ subtitle: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title)
                .font(VFont.pageTitle())
                .foregroundColor(PdigV2Colors.textPrimary)
            Text(subtitle)
                .font(VFont.meta())
                .foregroundColor(PdigV2Colors.textMuted)
        }
    }

    private func metricCard(_ value: String, _ label: String) -> some View {
        VStack(alignment: .leading, spacing: 3) {
            Text(value)
                .font(.system(size: 22, weight: .bold))
                .foregroundColor(PdigV2Colors.textPrimary)
            Text(label)
                .font(VFont.meta())
                .foregroundColor(PdigV2Colors.textMuted)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(VSpace.lg)
        .background(PdigV2Colors.surface)
        .clipShape(RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous))
    }

    private func infoCard(title: String, detail: String) -> some View {
        VStack(alignment: .leading, spacing: VSpace.sm) {
            Text(title)
                .font(VFont.body())
                .fontWeight(.semibold)
                .foregroundColor(PdigV2Colors.textPrimary)
            Text(detail)
                .font(VFont.secondary())
                .foregroundColor(PdigV2Colors.textSecondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(VSpace.lg)
        .background(PdigV2Colors.surface)
        .clipShape(RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous)
                .stroke(PdigV2Colors.borderSubtle, lineWidth: 1)
        )
    }
}
private extension String {
    var nonEmpty: String? { isEmpty ? nil : self }
}

