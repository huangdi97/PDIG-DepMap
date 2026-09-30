// ChangePhoneView —— 更换手机号（/change/phone，flagship）：Continuity Rail × make-before-break。
//
// change-phone.md / continuity-view.md 契约：
//  - 6 阶段线性状态机（completed / verifying / not_started / blocked / upcoming）；
//  - stage 6 在 stage 3 验证通过前 = disabled + 明文原因（make-before-break，绝不只禁用）；
//  - OLD NUMBER → services/accounts → NEW NUMBER 视觉模型；
//  - plan projection 带「计划」label + 虚线，不冒充已完成。
//  testId：pdig.change.phone.rail / .stage1…6 / .gate / .item。

import SwiftUI

struct ChangePhoneView: View {
    @ObservedObject var model: VNextModel

    private var oldNumber: VNumber? { VNextDemoFixture.numberById(VNextDemoFixture.changeOldNumberId) }
    private var newNumber: VNumber? { VNextDemoFixture.numberById(VNextDemoFixture.changeNewNumberId) }
    private var stages: [VChangeStage] { VNextDemoFixture.changeStages }
    private var migrations: [VMigration] { VNextDemoFixture.changeMigrations }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: VSpace.sectionGap) {
                titleSection

                // Continuity Rail（pdig.change.phone.rail）
                ContinuityRail(stages: stages)
                    .accessibilityIdentifier(VTestIds.changePhoneRail)

                // OLD → services → NEW
                oldToNewModel

                // 阶段明细
                stageDetailSection

                // 风险提示
                riskSection
            }
            .padding(VSpace.pagePadding)
        }
        .vPageBackground()
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                VBackButton { model.back() }
            }
        }
        .navigationBarBackButtonHidden(true)
    }

    private var titleSection: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(VCopy.changePhoneTitle)
                .font(VFont.pageTitle())
                .foregroundColor(PdigV2Colors.textPrimary)
            Text(VCopy.planProjectionNote)
                .font(VFont.meta())
                .foregroundColor(PdigV2Colors.textMuted)
        }
    }

    // MARK: - OLD → services → NEW 视觉模型

    private var oldToNewModel: some View {
        VStack(alignment: .leading, spacing: VSpace.md) {
            VSectionHeader(title: VCopy.oldToNewModel)

            HStack(spacing: VSpace.sm) {
                if let old = oldNumber {
                    numberPanel(label: VCopy.oldNumber, number: old, highlight: false)
                }
                Image(systemName: "arrow.right")
                    .foregroundColor(PdigV2Colors.textMuted)
                servicesPanel
                Image(systemName: "arrow.right")
                    .foregroundColor(PdigV2Colors.textMuted)
                if let new = newNumber {
                    numberPanel(label: VCopy.newNumber, number: new, highlight: true)
                }
            }
        }
    }

    private func numberPanel(label: String, number: VNumber, highlight: Bool) -> some View {
        VStack(alignment: .leading, spacing: VSpace.sm) {
            VChip(text: label, highlight: highlight)
            Text(number.maskedNumber)
                .font(.system(size: 15, weight: .bold, design: .monospaced))
                .foregroundColor(PdigV2Colors.textPrimary)
            Text("\(number.carrier) · \(vRoleLabel(number.role))")
                .font(VFont.meta())
                .foregroundColor(PdigV2Colors.textMuted)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(VSpace.lg)
        .background(highlight ? PdigV2Colors.primary.opacity(0.14) : PdigV2Colors.surfaceRaised)
        .clipShape(RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous)
                .stroke(highlight ? PdigV2Colors.primaryBright : PdigV2Colors.borderSubtle, lineWidth: 1)
        )
    }

    private var servicesPanel: some View {
        VStack(alignment: .leading, spacing: VSpace.sm) {
            VChip(text: VCopy.keyServices)
            ForEach(migrations, id: \.service) { migration in
                HStack {
                    Text(migration.service)
                        .font(VFont.secondary())
                        .foregroundColor(PdigV2Colors.textSecondary)
                    Spacer()
                    VStatusBadge(status: migration.status)
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(VSpace.lg)
        .background(PdigV2Colors.surface.opacity(0.7))
        .clipShape(RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous)
                .stroke(PdigV2Colors.borderSubtle, lineWidth: 1)
        )
    }

    // MARK: - 阶段明细

    private var stageDetailSection: some View {
        VStack(alignment: .leading, spacing: VSpace.md) {
            VSectionHeader(title: VCopy.stageDetailTitle)
            VStack(spacing: VSpace.sm) {
                stageDetailRow(stage: 1, title: "1 \(VCopy.impactAnalysis)",
                               body: "已确认需要迁移的服务：微信支付、支付宝、招商银行网银、腾讯视频；2 个账户以旧号码为登录验证。")
                stageDetailRow(stage: 2, title: "2 \(VCopy.establishNewNumber)",
                               body: "新号码 +86 139****2204 已加入（保号副号）。")
                stageDetailRow(stage: 3, title: "3 \(VCopy.verifyNewNumber)",
                               body: "等待接收验证码并确认（正在验证）。")
                stageDetailRow(stage: 4, title: "4 \(VCopy.migrateAccounts)",
                               body: "待验证通过后逐个迁移绑定。")
                stageDetailRow(stage: 5, title: "5 \(VCopy.checkRecoveryPaths)",
                               body: "确保每个账户存在非旧号码的恢复方式。")
                stageDetailRow(stage: 6, title: "6 \(VCopy.retireOldNumber)",
                               body: "阻止执行：新手机号验证通过后才能停用旧手机号（make-before-break）。", blocked: true)
            }
        }
    }

    private func stageDetailRow(stage: Int, title: String, body: String, blocked: Bool = false) -> some View {
        HStack(alignment: .top, spacing: VSpace.md) {
            Text(stage.description)
                .font(VFont.secondary())
                .fontWeight(.bold)
                .foregroundColor(blocked ? PdigV2Colors.critical : PdigV2Colors.textPrimary)
                .frame(width: 24, height: 24, alignment: .center)
                .background(blocked ? PdigV2Colors.critical.opacity(0.14) : PdigV2Colors.surfaceRaised)
                .clipShape(Circle())
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(VFont.secondary())
                    .fontWeight(.medium)
                    .foregroundColor(blocked ? PdigV2Colors.critical : PdigV2Colors.textPrimary)
                Text(body)
                    .font(VFont.meta())
                    .foregroundColor(PdigV2Colors.textSecondary)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(VSpace.md)
        .background(PdigV2Colors.surfaceRaised)
        .clipShape(RoundedRectangle(cornerRadius: VRadius.md, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: VRadius.md, style: .continuous)
                .stroke(blocked ? PdigV2Colors.critical.opacity(0.4) : PdigV2Colors.borderSubtle, lineWidth: 1)
        )
        .accessibilityIdentifier(VTestIds.changePhoneItem)
    }

    private var riskSection: some View {
        VStack(alignment: .leading, spacing: VSpace.md) {
            VSectionHeader(title: VCopy.riskTitle)
            Text(VCopy.riskBody)
                .font(VFont.secondary())
                .foregroundColor(PdigV2Colors.textPrimary)
                .padding(VSpace.lg)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(PdigV2Colors.warning.opacity(0.12))
                .clipShape(RoundedRectangle(cornerRadius: VRadius.md, style: .continuous))
        }
    }
}
