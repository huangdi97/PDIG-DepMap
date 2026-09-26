// 场景中心（task #8）：支付场景 + 身份与恢复场景清单。

import SwiftUI
import PDIGCore

struct ScenarioCenterScreen: View {
    @ObservedObject var session: AppSession

    var body: some View {
        VStack(spacing: 0) {
            AppTopBar(title: "场景中心") { session.pop() }
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    Text(CopyZh.scenarioPaymentCategory).font(.headline)
                    ForEach(ScenarioFlow.catalog().filter { $0.category == CopyZh.scenarioPaymentCategory }) { e in
                        scenarioCard(e)
                    }
                    Text(CopyZh.scenarioIdentityRecoveryCategory).font(.headline)
                    ForEach(ScenarioFlow.catalog().filter { $0.category == CopyZh.scenarioIdentityRecoveryCategory }) { e in
                        scenarioCard(e)
                    }
                }
                .padding()
            }
        }
        .frame(minWidth: 420, minHeight: 600)
    }

    private func scenarioCard(_ e: ScenarioFlow.CatalogEntry) -> some View {
        Button {
            session.push(.scenario(e.id))
        } label: {
            VStack(alignment: .leading, spacing: 4) {
                Text(e.title).font(.body.weight(.semibold))
                Text(e.subtitle).font(.caption).foregroundStyle(.secondary)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(10)
            .background(Color.gray.opacity(0.12), in: RoundedRectangle(cornerRadius: 10))
        }
        .buttonStyle(.plain)
    }
}
