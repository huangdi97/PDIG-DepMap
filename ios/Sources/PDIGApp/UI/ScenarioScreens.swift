// 场景流程（task #8）。步骤：场景设置 → 查看影响 → 查看恢复路径 →
// 查看共享故障点 → 变更计划 → 执行变更 → 验证 → 完成。
// replace_phone_number 显示 make-before-break 语言（必须先完成/完成后才能继续/
// 等待验证/可以并行处理/验证后才能移除旧路径）。

import SwiftUI
import PDIGCore

/// 场景流程：步进驱动。
struct ScenarioFlowScreen: View {
    @ObservedObject var session: AppSession
    let scenarioId: String
    @State private var step: Int = 0
    @State private var targetNodeId: String?
    @State private var errorText: String?
    @State private var planId: String?

    private var isIdentity: Bool { scenarioId == ScenarioFlow.replacePhoneScenarioId }

    private var targets: [DepNode] {
        if isIdentity {
            return session.snapshot.nodes.filter { $0.kind == .identityAnchor && !$0.archived }
        }
        return session.snapshot.nodes.filter { $0.kind == .paymentInstrument && !$0.archived }
    }

    var body: some View {
        VStack(spacing: 0) {
            AppTopBar(title: title) { session.pop() }
            stepIndicator
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    switch step {
                    case 0: setupStep
                    case 1: impactStep
                    case 2: recoveryPathsStep
                    case 3: failureDomainsStep
                    case 4: changePlanStep
                    case 5: actionStep
                    case 6: verificationStep
                    default: completionStep
                    }
                }
                .padding()
            }
        }
        .frame(minWidth: 420, minHeight: 600)
    }

    private var title: String {
        ScenarioFlow.catalog().first { $0.id == scenarioId }?.title ?? "场景"
    }

    private var stepIndicator: some View {
        HStack(spacing: 4) {
            ForEach(0..<8, id: \.self) { i in
                Capsule()
                    .fill(i <= step ? Color.accentColor : Color.gray.opacity(0.3))
                    .frame(height: 4)
            }
        }
        .padding(.horizontal)
        .padding(.bottom, 6)
    }

    private var setupStep: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(CopyZh.scenarioSetup).font(.headline)
            if targets.isEmpty {
                Text("还没有可选的\(isIdentity ? CopyZh.phoneNumber : "支付工具")。").foregroundStyle(.secondary)
            } else {
                ForEach(targets) { n in
                    Button {
                        targetNodeId = n.id
                    } label: {
                        HStack {
                            Text(n.name)
                            Spacer()
                            if targetNodeId == n.id {
                                Image(systemName: "checkmark.circle.fill").foregroundStyle(.tint)
                            }
                        }
                        .padding(10)
                        .background(Color.gray.opacity(0.12), in: RoundedRectangle(cornerRadius: 10))
                    }
                    .buttonStyle(.plain)
                }
            }
            if let errorText = errorText {
                Text(errorText).font(.footnote).foregroundStyle(.red)
            }
            Button {
                startPlan()
            } label: {
                Text(CopyZh.reviewImpact).frame(maxWidth: .infinity).padding(.vertical, 8)
            }
            .buttonStyle(.borderedProminent)
            .disabled(targetNodeId == nil)
        }
    }

    private func startPlan() {
        guard let target = targetNodeId else { return }
        do {
            let plan = try session.startScenario(scenarioId: scenarioId, targetNodeId: target)
            planId = plan.id
            session.refresh()
            step = 1
        } catch let e {
            errorText = "\(e)"
        }
    }

    private var impactStep: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(CopyZh.reviewImpact).font(.headline)
            if let plan = currentPlan, let result = scenarioResult(for: plan) {
                if let impact = result.impact {
                    ForEach(impact.checklist, id: \.title) { item in
                        Text("• \(item.title)").font(.body)
                        Text(item.detail).font(.caption).foregroundStyle(.secondary)
                    }
                } else if result.temporal != nil {
                    mbbNotice(result)
                }
            }
            Button { step = 2 } label: { Text(CopyZh.next).frame(maxWidth: .infinity).padding(.vertical, 8) }
                .buttonStyle(.borderedProminent)
        }
    }

    private var recoveryPathsStep: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(CopyZh.reviewRecoveryPaths).font(.headline)
            if let plan = currentPlan, let target = plan.targetNodeId {
                let paths = session.snapshot.activeDependencies
                    .filter { $0.to == target && ($0.capability == .recovery || $0.capability == .authentication) }
                if paths.isEmpty {
                    Text("没有已确认的恢复/验证路径。").foregroundStyle(.secondary)
                } else {
                    ForEach(paths, id: \.id) { p in
                        VStack(alignment: .leading, spacing: 2) {
                            Text("\(session.snapshot.name(of: p.from)) → \(session.snapshot.name(of: p.to))")
                                .font(.body)
                            Text("\(CopyZh.relation(p.relation.wire)) · \(CopyZh.capability(p.capability.wire))")
                                .font(.caption).foregroundStyle(.secondary)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(10)
                        .background(Color.gray.opacity(0.12), in: RoundedRectangle(cornerRadius: 10))
                    }
                }
                if isIdentity {
                    mbbLanguageRows
                }
            }
            Button { step = 3 } label: { Text(CopyZh.next).frame(maxWidth: .infinity).padding(.vertical, 8) }
                .buttonStyle(.borderedProminent)
        }
    }

    /// make-before-break 语言行（必须先完成 / 完成后才能继续 / 等待验证 / 可以并行处理 / 验证后才能移除旧路径）。
    private var mbbLanguageRows: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(CopyZh.prerequisite).font(.subheadline.weight(.semibold))
            row(CopyZh.addNewPhone + "，" + CopyZh.completesBefore)
            row(CopyZh.verifyNewPhone + "，" + CopyZh.waitingVerification)
            row(CopyZh.migrateAccounts + "，" + CopyZh.parallel)
            row(CopyZh.retireOldPhone + "，" + CopyZh.verifyBeforeRemove)
        }
        .padding(10)
        .background(Color.orange.opacity(0.1), in: RoundedRectangle(cornerRadius: 10))
    }

    private func row(_ text: String) -> some View {
        Text("• \(text)").font(.footnote)
    }

    private var failureDomainsStep: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(CopyZh.reviewFailureDomains).font(.headline)
            if let plan = currentPlan, let target = plan.targetNodeId {
                let edges = session.snapshot.activeDependencies.filter {
                    $0.to == target && ($0.capability == .recovery || $0.capability == .authentication)
                }
                if edges.isEmpty {
                    Text("没有可检查的恢复路径。").foregroundStyle(.secondary)
                } else {
                    Text("路径数：\(edges.count)。共享同一设备的路径不算独立备用路径（" + CopyZh.notIndependent + "）。")
                        .font(.footnote).foregroundStyle(.secondary)
                    ForEach(edges, id: \.id) { p in
                        Text("• \(session.snapshot.name(of: p.from)) → \(session.snapshot.name(of: p.to))")
                            .font(.body)
                    }
                }
            }
            Button { step = 4 } label: { Text(CopyZh.next).frame(maxWidth: .infinity).padding(.vertical, 8) }
                .buttonStyle(.borderedProminent)
        }
    }

    private var changePlanStep: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("变更计划").font(.headline)
            if let plan = currentPlan {
                let view = planView(for: plan)
                Text("状态：\(view.workflowText)").font(.body)
                Text(view.readinessText).font(.body).foregroundStyle(.secondary)
                if let result = scenarioResult(for: plan), let mbb = result.makeBeforeBreak {
                    Text(mbb.status == .blocked ? CopyZh.waitingVerification : CopyZh.newPathVerified)
                        .font(.footnote).foregroundStyle(mbb.status == .blocked ? .orange : .green)
                }
            }
            Button { step = 5 } label: { Text(CopyZh.next).frame(maxWidth: .infinity).padding(.vertical, 8) }
                .buttonStyle(.borderedProminent)
        }
    }

    private var actionStep: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("执行变更").font(.headline)
            if let plan = currentPlan {
                let view = planView(for: plan)
                ForEach(view.actions) { a in
                    HStack(alignment: .top) {
                        Image(systemName: a.done ? "checkmark.circle.fill" : "circle")
                            .foregroundStyle(a.done ? .green : .secondary)
                        VStack(alignment: .leading, spacing: 2) {
                            Text(a.title).font(.body)
                            Text(a.prerequisiteText).font(.caption).foregroundStyle(.orange)
                            Text(a.verificationStatusText).font(.caption).foregroundStyle(.secondary)
                        }
                        Spacer()
                    }
                    .padding(10)
                    .background(Color.gray.opacity(0.12), in: RoundedRectangle(cornerRadius: 10))
                }
            }
            Button { step = 6 } label: { Text(CopyZh.next).frame(maxWidth: .infinity).padding(.vertical, 8) }
                .buttonStyle(.borderedProminent)
        }
    }

    private var verificationStep: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("验证").font(.headline)
            Text(CopyZh.verificationNewPathIsNotVerified).font(.footnote).foregroundStyle(.orange)
            if let plan = currentPlan {
                let view = planView(for: plan)
                ForEach(view.actions.filter { $0.verificationStatusText != CopyZh.verificationNotRequired }) { a in
                    HStack {
                        Text(a.title).font(.body)
                        Spacer()
                        Text(a.verificationStatusText).font(.caption).foregroundStyle(.secondary)
                    }
                    .padding(8)
                    .background(Color.gray.opacity(0.12), in: RoundedRectangle(cornerRadius: 8))
                }
            }
            Button { step = 7 } label: { Text(CopyZh.next).frame(maxWidth: .infinity).padding(.vertical, 8) }
                .buttonStyle(.borderedProminent)
        }
    }

    private var completionStep: some View {
        VStack(spacing: 16) {
            Image(systemName: "checkmark.seal.fill").font(.system(size: 48)).foregroundStyle(.green)
            Text(CopyZh.scenarioComplete).font(.title2.bold())
            Text("回到首页查看" + CopyZh.homeNeedsAction + "。").foregroundStyle(.secondary)
            Button {
                session.root()
            } label: {
                Text(CopyZh.done).frame(maxWidth: 240).padding(.vertical, 8)
            }
            .buttonStyle(.borderedProminent)
        }
        .frame(maxWidth: .infinity)
        .padding(.top, 64)
    }

    // MARK: - helpers

    private var currentPlan: ChangePlan? {
        guard let id = planId else { return nil }
        return session.plans.first { $0.id == id }
    }

    private func scenarioResult(for plan: ChangePlan) -> ScenarioPlan? {
        guard let target = plan.targetNodeId else { return nil }
        return try? ScenarioFlow.createPlan(
            scenarioId: plan.scenario,
            targetNodeId: target,
            graph: session.snapshot,
            effectiveDate: plan.effectiveDate
        )
    }

    private func planView(for plan: ChangePlan) -> PlanView {
        let lostKeys = (scenarioResult(for: plan)?.impact?.lostKeys ?? []).map { $0.keyString() }
        return ChangePlanViewModel.view(input: PlanInput(
            plan: plan,
            currentGraphRevision: session.snapshot.graphRevision,
            prerequisiteEdges: scenarioResult(for: plan)?.prerequisiteEdges ?? [:]
        ), impactKeys: lostKeys)
    }

    private func mbbNotice(_ result: ScenarioPlan) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("顺序闸门").font(.subheadline.weight(.semibold))
            if let reason = result.temporal?.retireBlockedReason {
                Text(reason).font(.footnote).foregroundStyle(.orange)
            }
            if let mbb = result.makeBeforeBreak {
                Text(mbb.status == .blocked ? CopyZh.verifyBeforeRemove : CopyZh.oldPathRetired)
                    .font(.footnote)
                    .foregroundStyle(mbb.status == .blocked ? .orange : .green)
            }
        }
        .padding(10)
        .background(Color.orange.opacity(0.1), in: RoundedRectangle(cornerRadius: 10))
    }
}
