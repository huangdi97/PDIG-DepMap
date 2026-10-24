// 场景中心（Quiet Infrastructure）：支付 / 身份与恢复 两区清单。
// 分组文案取自 copy-zh：CopyZh.scenarioPaymentCategory / scenarioIdentityRecoveryCategory。

import SwiftUI
import PDIGCore

struct ScenarioCenterScreen: View {
    @ObservedObject var session: AppSession

    var body: some View {
        VStack(spacing: 0) {
            AppTopBar(title: "场景中心") { session.pop() }
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    paymentSection
                    identitySection
                }
                .padding()
            }
        }
        .frame(minWidth: 420, minHeight: 600)
    }

    private var paymentSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            SectionHeader(CopyZh.scenarioPaymentCategory)
            ForEach(ScenarioFlow.catalog().filter { $0.category == CopyZh.scenarioPaymentCategory }) { e in
                scenarioCard(e)
            }
        }
    }

    private var identitySection: some View {
        VStack(alignment: .leading, spacing: 8) {
            SectionHeader(CopyZh.scenarioIdentityRecoveryCategory)
            ForEach(ScenarioFlow.catalog().filter { $0.category == CopyZh.scenarioIdentityRecoveryCategory }) { e in
                scenarioCard(e)
            }
        }
    }

    private func scenarioCard(_ e: ScenarioFlow.CatalogEntry) -> some View {
        Button {
            session.push(.scenario(e.id))
        } label: {
            PdigCard {
                VStack(alignment: .leading, spacing: 4) {
                    Text(e.title).font(.body.weight(.semibold))
                    Text(e.subtitle).font(.caption).foregroundStyle(.secondary)
                }
            }
        }
        .buttonStyle(.plain)
    }
}