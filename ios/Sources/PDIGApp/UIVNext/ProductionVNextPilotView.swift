import SwiftUI

/// Read-only iOS Production VNext pilot.
///
/// This surface is intentionally opt-in while iOS parity is still being built.
/// Every value comes from the lock-gated VIosProductionSnapshot. There is no
/// VNextDemoFixture fallback and no mutation authority in this pilot.
struct VIosProductionVNextPilotView: View {
    @ObservedObject var session: AppSession

    @State private var selectedRoot: Root = .now
    @State private var selectedSecondaryKind: VSecondaryKind? = nil
    @State private var selectedObjectId: String? = nil
    @State private var regionFilter: String? = nil
    @State private var privacyMask = false

    enum Root: Hashable {
        case now
        case infrastructure
        case change
        case records
        case me
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
                    .tabItem { Label("现在", systemImage: "house.fill") }
                    .tag(Root.now)

                    NavigationStack {
                        productionInfrastructure(snapshot)
                    }
                    .tabItem { Label("基础设施", systemImage: "square.grid.2x2.fill") }
                    .tag(Root.infrastructure)

                    NavigationStack {
                        productionChange(snapshot)
                    }
                    .tabItem { Label("变更", systemImage: "arrow.triangle.2.circlepath") }
                    .tag(Root.change)

                    NavigationStack {
                        productionRecords(snapshot)
                    }
                    .tabItem { Label("记录", systemImage: "clock") }
                    .tag(Root.records)

                    NavigationStack {
                        productionMe(snapshot)
                    }
                    .tabItem { Label("我", systemImage: "person.crop.circle.fill") }
                    .tag(Root.me)
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
        if let kind = selectedSecondaryKind {
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

                    ForEach(VSecondaryKind.allCases, id: \.self) { kind in
                        let count = vIosProductionSecondaryObjects(
                            snapshot: snapshot,
                            kind: kind,
                            regionFilter: regionFilter
                        ).count
                        Button {
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
                }
                .padding(VSpace.pagePadding)
            }
            .vPageBackground()
            .accessibilityIdentifier("pdig.ios.production-vnext.infrastructure")
        }
    }

    private func confirmedRegionScope(_ snapshot: VIosProductionSnapshot) -> some View {
        let regionCodes = Array(
            Set(snapshot.objects.flatMap(\.confirmedRegionCodes))
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
                            Text("\(dependency.relation) · \(dependency.capability)")
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

    @ViewBuilder
    private func productionChange(_ snapshot: VIosProductionSnapshot) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: VSpace.sectionGap) {
                pageHeader(
                    "变更",
                    "当前为只读接线阶段；计划状态、完成与验证仍保持分离。"
                )

                if snapshot.plans.isEmpty {
                    infoCard(
                        title: "还没有变更计划",
                        detail: "这里不会用演示计划替代真实记录。"
                    )
                }

                ForEach(snapshot.plans) { plan in
                    VStack(alignment: .leading, spacing: VSpace.sm) {
                        HStack {
                            Text(privacyMask ? "变更计划（已遮蔽）" : plan.title)
                                .font(VFont.body())
                                .fontWeight(.semibold)
                                .foregroundColor(PdigV2Colors.textPrimary)
                            Spacer()
                            VStatusBadge(status: plan.needsReanalysis ? "warning" : plan.workflowState)
                        }
                        Text("已记录完成 \(plan.actions.filter { $0.done }.count)/\(plan.actions.count) · 已验证 \(plan.actions.filter { $0.verificationStatus == "verified" }.count)")
                            .font(VFont.meta())
                            .foregroundColor(PdigV2Colors.textSecondary)
                        if plan.needsReanalysis {
                            Text("基础设施数据版本已变化，需要重新分析当前影响范围。")
                                .font(VFont.meta())
                                .foregroundColor(PdigV2Colors.warning)
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
                if action.done || action.verificationStatus != nil {
                    return ProductionRecordRow(plan: plan, action: action)
                }
                return nil
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
                                    ? PdigV2Colors.success
                                    : PdigV2Colors.warning
                            )
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
