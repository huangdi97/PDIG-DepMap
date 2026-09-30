// NumberDetailView —— 号码详情（/infrastructure/numbers/{id}，身份面优先）。
//
// number-detail.md 契约：
//  - 顶部 number identity surface（昵称/遮罩号码/region/carrier/SIM/role/usage/status）；
//  - 其后按 关联服务/登录用途/2FA/恢复用途/风险/备用路径/历史；
//  - 动作：定制 → /infrastructure/numbers/{id}/customize；换号场景 → /change/phone；
//  - 遮罩默认开（maskPhoneNumbers）；运营商未知不填充；缺失 = 未设置/未知。

import SwiftUI

struct NumberDetailView: View {
    @ObservedObject var model: VNextModel

    private var number: VNumber? {
        if case .numberDetail(let id) = model.screen {
            return VNextDemoFixture.numberById(id)
        }
        return nil
    }

    var body: some View {
        if let number = number {
            ScrollView {
                VStack(alignment: .leading, spacing: VSpace.sectionGap) {
                    titleBar

                    // Identity surface（pdig.phone.detail.identity）
                    identitySection(number)
                        .accessibilityIdentifier(VTestIds.phoneDetailIdentity)

                    relatedServicesSection(number)

                    riskSection(number)

                    backupPathSection

                    historySection
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
    }

    private var titleBar: some View {
        HStack {
            Text(VCopy.numberDetailTitle)
                .font(VFont.pageTitle())
                .foregroundColor(PdigV2Colors.textPrimary)
            Spacer()
        }
    }

    private func identitySection(_ number: VNumber) -> some View {
        VStack(alignment: .leading, spacing: VSpace.md) {
            VNumberFace(number: number, privacyMask: model.privacyMask, onClick: {})

            HStack(spacing: VSpace.md) {
                Button {
                    model.openNumberCustomization(number.id)
                } label: {
                    Text("\(VCopy.customizeNumberFace) →")
                        .font(VFont.secondary())
                        .fontWeight(.semibold)
                        .foregroundColor(PdigV2Colors.primaryBright)
                        .frame(maxWidth: .infinity, alignment: .center)
                        .padding(VSpace.md)
                        .frame(minHeight: 44)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .background(PdigV2Colors.primarySoft)
                .clipShape(RoundedRectangle(cornerRadius: VRadius.md, style: .continuous))

                Button {
                    model.navigate(.changePhone)
                } label: {
                    Text(VCopy.quickChangePhone)
                        .font(VFont.secondary())
                        .fontWeight(.semibold)
                        .foregroundColor(PdigV2Colors.textSecondary)
                        .frame(maxWidth: .infinity, alignment: .center)
                        .padding(VSpace.md)
                        .frame(minHeight: 44)
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
        }
        .padding(VSpace.xl)
        .background(PdigV2Colors.surface.opacity(0.9))
        .clipShape(RoundedRectangle(cornerRadius: VRadius.xl, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: VRadius.xl, style: .continuous)
                .stroke(PdigV2Colors.borderSubtle, lineWidth: 1)
        )
    }

    private func relatedServicesSection(_ number: VNumber) -> some View {
        let services = VNextDemoFixture.servicesForNumber(number.id)
        return VStack(alignment: .leading, spacing: VSpace.md) {
            VSectionHeader(title: "\(VCopy.relatedServices)（\(services.count)）")
            ForEach(services) { service in
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(service.name)
                            .font(VFont.body())
                            .fontWeight(.medium)
                            .foregroundColor(PdigV2Colors.textPrimary)
                        Text(vRelationKindLabel(service.kind))
                            .font(VFont.meta())
                            .foregroundColor(PdigV2Colors.textMuted)
                    }
                    Spacer()
                    VChip(text: relationChipLabel(service.kind))
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

            // 登录用途 / 2FA / 恢复用途 分节（语义分组展示）
            let authenticates = services.filter { $0.kind == "authenticates" }
            let twoFA = services.filter { $0.kind == "twoFA" }
            if !authenticates.isEmpty {
                VSectionHeader(title: VCopy.loginUsage)
                ForEach(authenticates) { s in
                    Text(s.name).font(VFont.secondary()).foregroundColor(PdigV2Colors.textSecondary)
                }
            }
            if !twoFA.isEmpty {
                VSectionHeader(title: VCopy.twoFA)
                ForEach(twoFA) { s in
                    Text(s.name).font(VFont.secondary()).foregroundColor(PdigV2Colors.textSecondary)
                }
            }
        }
    }

    private func riskSection(_ number: VNumber) -> some View {
        VStack(alignment: .leading, spacing: VSpace.md) {
            VSectionHeader(title: VCopy.cardDetailRisk)
            if number.recoveryOnly {
                HStack(spacing: VSpace.md) {
                    Image(systemName: "exclamationmark.triangle.fill")
                        .foregroundColor(PdigV2Colors.warning)
                        .frame(width: 18)
                    Text("此号码是 2 个账户的唯一恢复路径：更换/注销前必须先建立新的恢复方式。")
                        .font(VFont.secondary())
                        .foregroundColor(PdigV2Colors.textPrimary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
                .padding(VSpace.lg)
                .frame(minHeight: 44)
                .background(PdigV2Colors.warning.opacity(0.14))
                .clipShape(RoundedRectangle(cornerRadius: VRadius.md, style: .continuous))
            } else {
                Text("未发现该号码承担唯一恢复路径。")
                    .font(VFont.secondary())
                    .foregroundColor(PdigV2Colors.textSecondary)
            }
        }
    }

    private var backupPathSection: some View {
        VStack(alignment: .leading, spacing: VSpace.md) {
            VSectionHeader(title: VCopy.backupPath)
            Text("该号码的登录用途存在其他验证渠道（备用路径全部来自已确认依赖；未知 = 未知）。")
                .font(VFont.secondary())
                .foregroundColor(PdigV2Colors.textSecondary)
        }
    }

    private var historySection: some View {
        VStack(alignment: .leading, spacing: VSpace.md) {
            VSectionHeader(title: VCopy.history)
            Text("2026-08 更新运营商资料；2026-03 加入 2FA 用途。")
                .font(VFont.meta())
                .foregroundColor(PdigV2Colors.textMuted)
        }
    }

    private func relationChipLabel(_ kind: String) -> String {
        switch kind {
        case "twoFA": return "2FA 验证"
        case "authenticates": return "登录验证"
        default: return "注册使用"
        }
    }
}
