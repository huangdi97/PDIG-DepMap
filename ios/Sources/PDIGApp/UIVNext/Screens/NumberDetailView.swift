// NumberDetailView —— communication-identity detail.
// Hero → Summary → Services → Risk → Backup path → History.
// On iPad the identity column remains visually stable while continuity information uses the wider second column.

import SwiftUI

struct NumberDetailView: View {
    @ObservedObject var model: VNextModel
    @Environment(\.horizontalSizeClass) private var sizeClass

    private var number: VNumber? {
        if case .numberDetail(let id) = model.screen { return VNextDemoFixture.numberById(id) }
        return nil
    }

    var body: some View {
        Group {
            if let number {
                ScrollView {
                    VStack(alignment: .leading, spacing: VSpace.lg) {
                        Text("号码详情").font(VFont.pageTitle()).foregroundColor(PdigV2Colors.textPrimary)

                        if sizeClass == .regular {
                            HStack(alignment: .top, spacing: VSpace.xl) {
                                identityColumn(number)
                                    .frame(maxWidth: 390)
                                continuityColumn(number)
                                    .frame(maxWidth: .infinity)
                            }
                        } else {
                            identityColumn(number)
                            continuityColumn(number)
                        }
                    }
                    .padding(VSpace.pagePadding)
                }
                .vPageBackground()
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) { VBackButton { model.back() } }
                }
                #if os(iOS)
                .navigationBarBackButtonHidden(true)
                #endif
            }
        }
    }

    private func identityColumn(_ number: VNumber) -> some View {
        VStack(alignment: .leading, spacing: VSpace.lg) {
            VNumberFace(number: number, privacyMask: model.privacyMask, onClick: {})
                .accessibilityIdentifier(VTestIds.phoneDetailIdentity)

            HStack(spacing: VSpace.sm) {
                summaryItem(vRoleLabel(number.role), "角色")
                summaryItem(vSimLabel(number.simKind), "形态")
                summaryItem("\(VNextDemoFixture.servicesForNumber(number.id).count)", "关联服务")
                summaryItem(number.recoveryOnly ? "唯一" : "多路径", "恢复")
            }
            .padding(VSpace.md)
            .background(PdigV2Colors.primarySoft.opacity(0.70))
            .clipShape(RoundedRectangle(cornerRadius: VRadius.lg))
            .accessibilityIdentifier("pdig.number.detail.summary")

            HStack(spacing: VSpace.sm) {
                Button("定制号码面") { model.openNumberCustomization(number.id) }
                    .buttonStyle(.bordered)
                    .tint(PdigV2Colors.primary)
                    .frame(maxWidth: .infinity)
                Button("更换手机号") { model.navigate(.changePhone) }
                    .buttonStyle(.borderedProminent)
                    .tint(PdigV2Colors.primary)
                    .frame(maxWidth: .infinity)
            }
        }
    }

    private func summaryItem(_ value: String, _ label: String) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(value).font(VFont.secondary()).fontWeight(.semibold).foregroundColor(PdigV2Colors.textPrimary)
            Text(label).font(.system(size: 10)).foregroundColor(PdigV2Colors.textMuted)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func continuityColumn(_ number: VNumber) -> some View {
        let services = VNextDemoFixture.servicesForNumber(number.id)
        return VStack(alignment: .leading, spacing: VSpace.lg) {
            VSectionHeader(title: "关联服务（\(services.count)）")
                .accessibilityIdentifier("pdig.number.detail.services")
            ForEach(services) { service in
                let relation = VNextDemoFixture.relationKind(number.id, service.id) ?? "unknown"
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(service.name).font(VFont.secondary()).fontWeight(.semibold).foregroundColor(PdigV2Colors.textPrimary)
                        Text(serviceKindName(service.kind)).font(VFont.meta()).foregroundColor(PdigV2Colors.textMuted)
                    }
                    Spacer()
                    VChip(text: vRelationKindLabel(relation))
                }
                .padding(VSpace.md)
                .background(PdigV2Colors.surface)
                .clipShape(RoundedRectangle(cornerRadius: VRadius.md))
                .overlay(RoundedRectangle(cornerRadius: VRadius.md).stroke(PdigV2Colors.borderSubtle, lineWidth: 1))
            }

            VSectionHeader(title: "风险")
            if number.recoveryOnly {
                VDetailNotice(
                    icon: "exclamationmark.triangle.fill",
                    title: "唯一恢复路径",
                    body: "此号码承担关键账户的唯一已确认恢复路径。更换或注销前，必须先建立并验证新的恢复方式。",
                    accent: PdigV2Colors.warning
                )
            } else {
                VUnknownBoundaryNote("当前已记录关系中没有唯一恢复路径；未记录的恢复关系继续保持未知。")
            }

            VSectionHeader(title: "备用路径")
            VUnknownBoundaryNote("这里只展示已经确认的替代验证渠道；没有记录的关系不会被自动推断为存在或安全。")

            VSectionHeader(title: "历史")
            Text("2026-08 更新运营商资料；2026-03 加入 2FA 用途。")
                .font(VFont.meta()).foregroundColor(PdigV2Colors.textMuted)
        }
    }
}
