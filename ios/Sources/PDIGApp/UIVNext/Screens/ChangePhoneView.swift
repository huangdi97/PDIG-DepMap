// ChangePhoneView —— flagship continuity choreography.
// Current / Transition / After are separate projections. After is always a plan projection, never completed reality.

import SwiftUI

struct ChangePhoneView: View {
    @ObservedObject var model: VNextModel
    @Environment(\.horizontalSizeClass) private var sizeClass

    private var oldNumber: VNumber { VNextDemoFixture.numberById(VNextDemoFixture.changeOldNumberId)! }
    private var newNumber: VNumber { VNextDemoFixture.numberById(VNextDemoFixture.changeNewNumberId)! }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: VSpace.lg) {
                header
                projectionPicker
                projectionBanner
                stageRail
                VSectionHeader(title: VCopy.oldToNewModel)
                continuityScene
                VSectionHeader(title: VCopy.stageDetailTitle)
                stageDetails
            }
            .padding(VSpace.pagePadding)
        }
        .vPageBackground()
        .toolbar {
            if model.canGoBack {
                ToolbarItem(placement: .cancellationAction) {
                    VBackButton { model.back() }
                }
            }
        }
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 3) {
            Text(VCopy.changePhoneTitle).font(VFont.pageTitle()).foregroundColor(PdigV2Colors.textPrimary)
            Text("在停用旧号码前建立并验证新路径。").font(VFont.secondary()).foregroundColor(PdigV2Colors.textSecondary)
        }
    }

    private var projectionPicker: some View {
        Picker("状态", selection: $model.changeProjection) {
            ForEach(VChangeProjection.allCases) { projection in
                Text(projection.title).tag(projection)
            }
        }
        .pickerStyle(.segmented)
        .accessibilityIdentifier("pdig.change.projection")
    }

    private var projectionBanner: some View {
        let content: (String, Color) = {
            switch model.changeProjection {
            case .current:
                return ("当前：旧号码仍是登录主号，迁移尚未开始；服务关系保持现状。", PdigV2Colors.primaryText)
            case .transition:
                return ("执行计划：只有实际验证完成的步骤才标记为完成。", PdigV2Colors.warning)
            case .after:
                return ("完成后预览：展示计划执行后的预期状态，不代表已经完成或验证。", PdigV2Colors.warning)
            }
        }()
        return Text(content.0)
            .font(VFont.secondary()).foregroundColor(PdigV2Colors.textPrimary)
            .padding(VSpace.lg).frame(maxWidth: .infinity, alignment: .leading)
            .background(content.1.opacity(0.09))
            .clipShape(RoundedRectangle(cornerRadius: VRadius.md))
    }

    private var stageRail: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: VSpace.md) {
                ForEach(projectedStages) { stage in
                    VStack(spacing: 5) {
                        Text("\(stage.stage)")
                            .font(VFont.meta()).fontWeight(.bold)
                            .foregroundColor(stage.status == "completed" ? .white : vStatusColor(stage.status))
                            .frame(width: 30, height: 30)
                            .background(stage.status == "completed" ? PdigV2Colors.positive : vStatusColor(stage.status).opacity(0.10))
                            .clipShape(Circle())
                        Text(shortStageTitle(stage.key))
                            .font(.system(size: 10, weight: .medium))
                            .foregroundColor(PdigV2Colors.textSecondary)
                            .lineLimit(1)
                        Text(vStatusLabel(stage.status))
                            .font(.system(size: 9))
                            .foregroundColor(vStatusColor(stage.status))
                    }
                    .frame(width: 84)
                    .accessibilityIdentifier(VTestIds.changePhoneStage(stage.stage))
                }
            }
            .padding(.vertical, 2)
        }
        .accessibilityIdentifier(VTestIds.changePhoneRail)
    }

    @ViewBuilder
    private var continuityScene: some View {
        if sizeClass == .regular {
            HStack(alignment: .top, spacing: VSpace.md) {
                numberPanel(label: "旧号码", number: oldNumber, old: true).frame(maxWidth: .infinity)
                servicesPanel.frame(maxWidth: .infinity)
                numberPanel(label: "新号码", number: newNumber, old: false).frame(maxWidth: .infinity)
            }
        } else {
            VStack(spacing: VSpace.md) {
                HStack(spacing: VSpace.sm) {
                    numberPanel(label: "旧号码", number: oldNumber, old: true)
                    Image(systemName: "arrow.right").foregroundColor(PdigV2Colors.primaryText)
                    numberPanel(label: "新号码", number: newNumber, old: false)
                }
                servicesPanel
            }
        }
    }

    private func numberPanel(label: String, number: VNumber, old: Bool) -> some View {
        VStack(alignment: .leading, spacing: VSpace.sm) {
            Text(label).font(VFont.meta()).foregroundColor(PdigV2Colors.textMuted)
            VNumberMiniIdentity(number: number, muted: model.changeProjection == .after && old)
            Text(old ? oldStatus : newStatus)
                .font(VFont.meta()).fontWeight(.semibold)
                .foregroundColor(old ? PdigV2Colors.textMuted : PdigV2Colors.primaryText)
        }
        .padding(VSpace.md)
        .background(PdigV2Colors.surface)
        .clipShape(RoundedRectangle(cornerRadius: VRadius.lg))
        .overlay(RoundedRectangle(cornerRadius: VRadius.lg)
            .stroke(old ? PdigV2Colors.borderSubtle : PdigV2Colors.primary.opacity(0.34), lineWidth: 1))
    }

    private var servicesPanel: some View {
        VStack(alignment: .leading, spacing: VSpace.sm) {
            Text("关联服务与账户").font(VFont.meta()).fontWeight(.semibold).foregroundColor(PdigV2Colors.textPrimary)
            ForEach(projectedMigrations, id: \.service) { item in
                HStack {
                    Text(item.service).font(VFont.meta()).foregroundColor(PdigV2Colors.textSecondary)
                    Spacer()
                    VStatusBadge(status: item.status)
                }
            }
            if model.changeProjection == .after {
                Text("未完成的服务保持「计划 / 待处理」，不会伪装成已迁移。")
                    .font(.system(size: 10)).foregroundColor(PdigV2Colors.textMuted)
            }
        }
        .padding(VSpace.md)
        .background(PdigV2Colors.surface)
        .clipShape(RoundedRectangle(cornerRadius: VRadius.lg))
        .overlay(RoundedRectangle(cornerRadius: VRadius.lg).stroke(PdigV2Colors.borderSubtle, lineWidth: 1))
    }

    private var stageDetails: some View {
        VStack(spacing: VSpace.sm) {
            ForEach(projectedStages) { stage in
                HStack(alignment: .top, spacing: VSpace.md) {
                    Text("\(stage.stage)").font(VFont.meta()).fontWeight(.bold)
                        .foregroundColor(vStatusColor(stage.status))
                        .frame(width: 28, height: 28)
                        .background(vStatusColor(stage.status).opacity(0.10)).clipShape(Circle())
                    VStack(alignment: .leading, spacing: 3) {
                        Text(fullStageTitle(stage.key)).font(VFont.secondary()).fontWeight(.semibold).foregroundColor(PdigV2Colors.textPrimary)
                        Text(stageBody(stage)).font(VFont.meta()).foregroundColor(PdigV2Colors.textSecondary)
                        if let reason = stage.blockReason {
                            Text(reason).font(VFont.meta()).foregroundColor(PdigV2Colors.critical)
                        }
                    }
                    Spacer()
                    VStatusBadge(status: stage.status)
                }
                .padding(VSpace.md)
                .background(PdigV2Colors.surface)
                .clipShape(RoundedRectangle(cornerRadius: VRadius.md))
            }
        }
    }

    private var projectedStages: [VChangeStage] {
        switch model.changeProjection {
        case .current:
            return [
                VChangeStage(stage: 1, key: "impact-analysis", status: "completed", blockReason: nil),
                VChangeStage(stage: 2, key: "establish-new-number", status: "not_started", blockReason: nil),
                VChangeStage(stage: 3, key: "verify-new-number", status: "not_started", blockReason: nil),
                VChangeStage(stage: 4, key: "migrate-key-accounts", status: "not_started", blockReason: nil),
                VChangeStage(stage: 5, key: "check-recovery-paths", status: "not_started", blockReason: nil),
                VChangeStage(stage: 6, key: "retire-old-number", status: "blocked", blockReason: VCopy.blockedReason),
            ]
        case .transition:
            return VNextDemoFixture.changeStages
        case .after:
            return [
                VChangeStage(stage: 1, key: "impact-analysis", status: "completed", blockReason: nil),
                VChangeStage(stage: 2, key: "establish-new-number", status: "completed", blockReason: nil),
                VChangeStage(stage: 3, key: "verify-new-number", status: "completed", blockReason: nil),
                VChangeStage(stage: 4, key: "migrate-key-accounts", status: "completed", blockReason: nil),
                VChangeStage(stage: 5, key: "check-recovery-paths", status: "plan", blockReason: nil),
                VChangeStage(stage: 6, key: "retire-old-number", status: "plan", blockReason: nil),
            ]
        }
    }

    private var projectedMigrations: [VMigration] {
        switch model.changeProjection {
        case .current:
            return VNextDemoFixture.changeMigrations.map { VMigration(service: $0.service, status: "not_started") }
        case .transition:
            return VNextDemoFixture.changeMigrations
        case .after:
            return VNextDemoFixture.changeMigrations.enumerated().map {
                VMigration(service: $0.element.service, status: $0.offset < 2 ? "migrated" : "plan")
            }
        }
    }

    private var oldStatus: String {
        switch model.changeProjection {
        case .current: return "当前使用"
        case .transition: return "迁移中保留"
        case .after: return "计划停用"
        }
    }
    private var newStatus: String {
        switch model.changeProjection {
        case .current: return "目标号码"
        case .transition: return "正在验证"
        case .after: return "计划主号"
        }
    }

    private func shortStageTitle(_ key: String) -> String {
        switch key {
        case "impact-analysis": return "影响分析"
        case "establish-new-number": return "建立新号"
        case "verify-new-number": return "验证新号"
        case "migrate-key-accounts": return "迁移账户"
        case "check-recovery-paths": return "检查恢复"
        default: return "停用旧号"
        }
    }
    private func fullStageTitle(_ key: String) -> String { shortStageTitle(key) }

    private func stageBody(_ stage: VChangeStage) -> String {
        switch stage.key {
        case "impact-analysis": return "确认受影响服务、账户与唯一恢复路径。"
        case "establish-new-number": return "先建立新的号码与可用接收能力。"
        case "verify-new-number": return "真实接收验证码并确认新路径可用。"
        case "migrate-key-accounts": return "按关键性逐项迁移登录、2FA 与通知。"
        case "check-recovery-paths": return "确认每个关键账户仍有独立恢复路径。"
        default: return "只有新路径完成验证后，才允许停用旧号码。"
        }
    }
}

private struct VNumberMiniIdentity: View {
    let number: VNumber
    var muted = false
    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(number.nickname).font(VFont.meta()).fontWeight(.semibold).foregroundColor(PdigV2Colors.assetTextPrimary)
            Text(number.maskedNumber).font(.system(size: 13, weight: .bold, design: .monospaced)).foregroundColor(PdigV2Colors.assetTextPrimary)
            Text("\(number.carrier) · \(vRoleLabel(number.role))").font(.system(size: 9)).foregroundColor(PdigV2Colors.assetTextSecondary)
        }
        .padding(VSpace.md)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(LinearGradient(colors: [Color(hex: "#0B2E58"), Color(hex: "#071A34")], startPoint: .topLeading, endPoint: .bottomTrailing))
        .opacity(muted ? 0.58 : 1)
        .clipShape(RoundedRectangle(cornerRadius: VRadius.md))
    }
}
