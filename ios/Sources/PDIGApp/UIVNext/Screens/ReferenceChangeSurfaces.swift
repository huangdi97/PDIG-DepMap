import SwiftUI

/// iOS reference translation of the frozen Change workspace.
///
/// This file is intentionally presentation-only. It reuses the synthetic
/// reference fixture and does not claim Production ChangePlan authority.
struct VChangeCenterView: View {
    @ObservedObject var model: VNextModel

    private var activeChange: VActiveChange? { VNextDemoFixture.activeChanges.first }
    private var expiringCard: VCard? {
        VNextDemoFixture.cards.first { $0.status == "expiring_soon" }
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: VSpace.sectionGap) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(VCopy.navChange)
                        .font(VFont.pageTitle())
                        .foregroundColor(PdigV2Colors.textPrimary)
                    Text("先建立替代路径，再迁移并验证；完成操作不等于验证完成。")
                        .font(VFont.secondary())
                        .foregroundColor(PdigV2Colors.textSecondary)
                }

                if let activeChange {
                    VSectionHeader(title: "进行中的变更")
                    changeEntry(
                        title: vVisibleActiveChangeTitle(activeChange, privacyMask: model.privacyMask),
                        subtitle: model.privacyMask
                            ? "当前阶段已遮蔽 · 仍保留验证状态"
                            : "新号码已建立 · 正在验证恢复与认证路径",
                        badge: "待验证",
                        symbol: "phone.arrow.right",
                        testId: VTestIds.changePhoneEntry
                    ) {
                        model.navigate(.changePhone)
                    }
                }

                VSectionHeader(title: "准备变更")

                if let expiringCard {
                    changeEntry(
                        title: model.privacyMask ? "支付工具变更（已遮蔽）" : "更换「\(expiringCard.nickname)」",
                        subtitle: model.privacyMask
                            ? "到期信息已记录 · 目标对象已遮蔽"
                            : "\(expiringCard.issuer) · 到期 \(expiringCard.expiry) · 先检查已记录支付关系",
                        badge: "准备",
                        symbol: "creditcard",
                        testId: VTestIds.changeCardEntry
                    ) {
                        model.navigate(.changeCard)
                    }
                }

                VStack(alignment: .leading, spacing: VSpace.sm) {
                    Text("Reference 边界")
                        .font(VFont.secondary())
                        .fontWeight(.semibold)
                        .foregroundColor(PdigV2Colors.textPrimary)
                    Text("这里演示 Current / Transition / After 的交互与信息层级。不会执行真实第三方变更，也不会把计划投影写成已发生事实。")
                        .font(VFont.meta())
                        .foregroundColor(PdigV2Colors.textSecondary)
                }
                .padding(VSpace.lg)
                .background(PdigV2Colors.surfaceRaised)
                .clipShape(RoundedRectangle(cornerRadius: VRadius.md, style: .continuous))
            }
            .padding(VSpace.pagePadding)
        }
        .vPageBackground()
        .accessibilityIdentifier(VTestIds.changeCenter)
    }

    private func changeEntry(
        title: String,
        subtitle: String,
        badge: String,
        symbol: String,
        testId: String,
        onClick: @escaping () -> Void
    ) -> some View {
        Button(action: onClick) {
            HStack(spacing: VSpace.lg) {
                Image(systemName: symbol)
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(PdigV2Colors.primaryBright)
                    .frame(width: 44, height: 44)
                    .background(PdigV2Colors.primarySoft)
                    .clipShape(RoundedRectangle(cornerRadius: VRadius.md, style: .continuous))
                VStack(alignment: .leading, spacing: 4) {
                    Text(title)
                        .font(VFont.body())
                        .fontWeight(.semibold)
                        .foregroundColor(PdigV2Colors.textPrimary)
                        .lineLimit(1)
                    Text(subtitle)
                        .font(VFont.meta())
                        .foregroundColor(PdigV2Colors.textSecondary)
                        .multilineTextAlignment(.leading)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                VChip(text: badge, highlight: badge == "待验证")
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
        .accessibilityIdentifier(testId)
    }
}

enum VCardChangePhase: String, CaseIterable {
    case current
    case transition
    case after

    var title: String {
        switch self {
        case .current: return "当前"
        case .transition: return "过渡中"
        case .after: return "完成后（计划）"
        }
    }
}

func vCardChangeServiceStatus(
    phase: VCardChangePhase,
    hasReplacement: Bool
) -> String {
    switch phase {
    case .current:
        return "当前依赖"
    case .transition:
        return hasReplacement ? "计划迁移" : "等待选择替代卡"
    case .after:
        return hasReplacement ? "计划迁移" : "未形成计划"
    }
}

/// Reference-only card replacement choreography.
///
/// The source and replacement cards come from the existing fixture. The view
/// never claims that a service is already migrated: Transition and After remain
/// explicit plan projections until real verification exists.
struct VCardChangeView: View {
    @ObservedObject var model: VNextModel
    @State private var phase: VCardChangePhase = .current
    @State private var replacementId: String?

    private var currentCard: VCard? {
        VNextDemoFixture.cards.first { $0.status == "expiring_soon" }
    }

    private var replacementCandidates: [VCard] {
        guard let currentCard else { return [] }
        return VNextDemoFixture.cards.filter {
            $0.id != currentCard.id &&
            $0.status == "active" &&
            $0.region == currentCard.region
        }
    }

    private var replacement: VCard? {
        guard let replacementId else { return nil }
        return VNextDemoFixture.cardById(replacementId)
    }

    private var services: [VService] {
        guard let currentCard else { return [] }
        return VNextDemoFixture.servicesForCard(currentCard.id)
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: VSpace.sectionGap) {
                HStack(spacing: VSpace.md) {
                    VBackButton { model.navigate(.change) }
                    VStack(alignment: .leading, spacing: 3) {
                        Text("更换银行卡")
                            .font(VFont.pageTitle())
                            .foregroundColor(PdigV2Colors.textPrimary)
                        Text("Current → Transition → After · After 仅为计划投影")
                            .font(VFont.meta())
                            .foregroundColor(PdigV2Colors.textMuted)
                    }
                }

                phaseSwitcher

                if let currentCard {
                    identitySection(currentCard)
                    servicesSection
                    phaseBoundary(currentCard)
                } else {
                    Text("没有可用于 Reference 的到期卡片。")
                        .font(VFont.secondary())
                        .foregroundColor(PdigV2Colors.textSecondary)
                }
            }
            .padding(VSpace.pagePadding)
        }
        .vPageBackground()
        .accessibilityIdentifier(VTestIds.changeCard)
    }

    private var phaseSwitcher: some View {
        HStack(spacing: VSpace.sm) {
            ForEach(VCardChangePhase.allCases, id: \.rawValue) { item in
                VFilterChip(label: item.title, selected: phase == item) {
                    phase = item
                }
                .accessibilityIdentifier("\(VTestIds.changeCardStage).\(item.rawValue)")
            }
        }
    }

    @ViewBuilder
    private func identitySection(_ currentCard: VCard) -> some View {
        switch phase {
        case .current:
            VStack(alignment: .leading, spacing: VSpace.md) {
                VSectionHeader(title: "当前卡片")
                VAssetCard(card: currentCard, privacyMask: model.privacyMask) {}
            }

        case .transition:
            VStack(alignment: .leading, spacing: VSpace.md) {
                VSectionHeader(title: "建立替代路径")
                HStack(alignment: .top, spacing: VSpace.md) {
                    compactCard(currentCard, label: "旧卡 · 保持可用")
                    Image(systemName: "arrow.right")
                        .foregroundColor(PdigV2Colors.textMuted)
                        .padding(.top, 32)
                    if let replacement {
                        compactCard(replacement, label: "替代卡 · 计划目标")
                    } else {
                        VStack(alignment: .leading, spacing: VSpace.sm) {
                            Text("尚未选择替代卡片")
                                .font(VFont.secondary())
                                .fontWeight(.semibold)
                                .foregroundColor(PdigV2Colors.critical)
                            Text("选择并核对替代卡之前，不会把任何服务显示为已迁移。")
                                .font(VFont.meta())
                                .foregroundColor(PdigV2Colors.textSecondary)
                        }
                        .padding(VSpace.lg)
                        .frame(maxWidth: .infinity, minHeight: 112, alignment: .topLeading)
                        .background(PdigV2Colors.critical.opacity(0.08))
                        .clipShape(RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous))
                    }
                }

                if replacement == nil {
                    VStack(alignment: .leading, spacing: VSpace.sm) {
                        Text("选择替代卡")
                            .font(VFont.secondary())
                            .fontWeight(.semibold)
                            .foregroundColor(PdigV2Colors.textPrimary)
                        ForEach(replacementCandidates) { candidate in
                            Button {
                                replacementId = candidate.id
                            } label: {
                                HStack {
                                    Text(model.privacyMask ? "支付工具（已遮蔽）" : candidate.nickname)
                                        .foregroundColor(PdigV2Colors.textPrimary)
                                    Spacer()
                                    Text(candidate.network)
                                        .font(VFont.meta())
                                        .foregroundColor(PdigV2Colors.textMuted)
                                }
                                .padding(VSpace.md)
                                .contentShape(Rectangle())
                            }
                            .buttonStyle(.plain)
                            .background(PdigV2Colors.surfaceRaised)
                            .clipShape(RoundedRectangle(cornerRadius: VRadius.md, style: .continuous))
                        }
                    }
                }
            }

        case .after:
            VStack(alignment: .leading, spacing: VSpace.md) {
                VSectionHeader(title: "完成后（计划）")
                if let replacement {
                    HStack(alignment: .top, spacing: VSpace.md) {
                        compactCard(currentCard, label: "旧卡 · 计划停用")
                        Image(systemName: "arrow.right")
                            .foregroundColor(PdigV2Colors.textMuted)
                            .padding(.top, 32)
                        compactCard(replacement, label: "替代卡 · 计划使用")
                    }
                    Text("这是计划完成后的预期，不代表第三方支付关系已经改变。只有实际迁移并验证后，事实才应进入记录。")
                        .font(VFont.meta())
                        .foregroundColor(PdigV2Colors.textSecondary)
                } else {
                    Text("尚未选择替代卡，因此没有可展示的完成后计划。")
                        .font(VFont.secondary())
                        .foregroundColor(PdigV2Colors.critical)
                }
            }
        }
    }

    private func compactCard(_ card: VCard, label: String) -> some View {
        VStack(alignment: .leading, spacing: VSpace.sm) {
            Text(label)
                .font(VFont.meta())
                .fontWeight(.semibold)
                .foregroundColor(PdigV2Colors.textMuted)
            Text(vVisibleCardNickname(card, privacyMask: model.privacyMask))
                .font(VFont.secondary())
                .fontWeight(.semibold)
                .foregroundColor(PdigV2Colors.textPrimary)
                .lineLimit(1)
            Text(model.privacyMask ? "卡片标识已遮蔽" : "\(card.issuer) · •••• \(card.last4)")
                .font(VFont.meta())
                .foregroundColor(PdigV2Colors.textSecondary)
                .lineLimit(1)
        }
        .padding(VSpace.lg)
        .frame(maxWidth: .infinity, minHeight: 112, alignment: .topLeading)
        .background(PdigV2Colors.surface)
        .clipShape(RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous)
                .stroke(PdigV2Colors.borderSubtle, lineWidth: 1)
        )
    }

    private var servicesSection: some View {
        VStack(alignment: .leading, spacing: VSpace.md) {
            VSectionHeader(title: "已记录支付关系")
            if services.isEmpty {
                Text("当前没有已记录的支付关系；没有记录不代表不存在。")
                    .font(VFont.meta())
                    .foregroundColor(PdigV2Colors.textSecondary)
            } else {
                ForEach(services) { service in
                    HStack {
                        Text(vVisibleServiceName(service, privacyMask: model.privacyMask))
                            .font(VFont.secondary())
                            .foregroundColor(PdigV2Colors.textPrimary)
                        Spacer()
                        VChip(
                            text: vCardChangeServiceStatus(
                                phase: phase,
                                hasReplacement: replacement != nil
                            ),
                            highlight: phase != .current
                        )
                    }
                    .padding(VSpace.md)
                    .background(PdigV2Colors.surfaceRaised)
                    .clipShape(RoundedRectangle(cornerRadius: VRadius.md, style: .continuous))
                }
            }
        }
    }

    @ViewBuilder
    private func phaseBoundary(_ currentCard: VCard) -> some View {
        switch phase {
        case .current:
            Text("当前仅展示 \(services.count) 条已记录支付关系；未记录关系仍保持未知。")
                .font(VFont.meta())
                .foregroundColor(PdigV2Colors.textMuted)
        case .transition:
            Text(
                replacement == nil
                    ? "阻断：尚未选择替代卡。旧卡继续保持可用。"
                    : "过渡期：旧卡继续保持可用；逐项迁移并验证后才进入完成后状态。"
            )
                .font(VFont.meta())
                .foregroundColor(replacement == nil ? PdigV2Colors.critical : PdigV2Colors.warning)
        case .after:
            Text(
                replacement == nil
                    ? "没有目标卡片，After 投影不可成立。"
                    : "After = Plan Projection；不等于已发生，也不等于已验证。"
            )
                .font(VFont.meta())
                .foregroundColor(replacement == nil ? PdigV2Colors.critical : PdigV2Colors.textMuted)
        }
    }
}
