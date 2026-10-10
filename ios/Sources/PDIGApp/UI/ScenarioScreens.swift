// 场景流程（Quiet Infrastructure）。步骤：场景设置 → 查看影响 → 查看恢复路径 →
// 查看共享故障点 → 变更计划 → 执行变更 → 验证 → 完成。
// 顶部为 Continuity Rail 步骤轨道：已完成 ✓ / 当前 / 待验证 / 受阻（明文原因）/ 未来。
// make-before-break 语言保留（必须先完成/完成后才能继续/等待验证/可以并行处理/验证后才能移除旧路径）。

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

    // MARK: - Continuity Rail

    private var stepTitles: [String] {
        [
            CopyZh.scenarioSetup,
            CopyZh.reviewImpact,
            CopyZh.reviewRecoveryPaths,
            CopyZh.reviewFailureDomains,
            CopyZh.planChangePlan,
            CopyZh.planExecute,
            CopyZh.planPhaseVerify,
            CopyZh.scenarioComplete,
        ]
    }

    /// 轨道状态：已完成 ✓（success）/ 当前（primary）/ 待验证（info）/
    /// 受阻（danger，含明文原因）/ 未来（secondary 灰）。
    private var railStates: [RailStepState] {
        let result = currentPlan.flatMap { scenarioResult(for: $0) }
        let gateBlocked = result?.makeBeforeBreak?.status == .blocked
        return (0..<8).map { i in
            if i < step { return .done }
            if i == step { return .current(index: i) }
            if i == 5 && gateBlocked { return .blocked }
            if i == 6 { return .verifying }
            return .future
        }
    }

    /// 闸门明文原因：新路径未验证 → 停用旧手机号被禁用并说明原因（绝不只禁用）。
    private var gateReason: String? {
        guard isIdentity, let plan = currentPlan,
              scenarioResult(for: plan)?.makeBeforeBreak?.status == .blocked else { return nil }
        return CopyZh.gateRetireBeforeVerified
    }

    private var stepIndicator: some View {
        VStack(alignment: .leading, spacing: 6) {
            StepRail(steps: railStates)
            HStack(spacing: 6) {
                Text("\(step + 1) / 8")
                    .font(PdigTheme.Font.label)
                    .foregroundStyle(PdigTheme.Color.textTertiary)
                Text(stepTitles[step])
                    .font(PdigTheme.Font.secondary)
                    .foregroundStyle(PdigTheme.Color.textSecondary)
            }
            if let reason = gateReason {
                NoticeBanner(icon: "exclamationmark.triangle.fill", text: reason, color: PdigTheme.Color.danger)
            }
        }
        .padding(.horizontal)
        .padding(.bottom, 6)
    }

    // MARK: - steps

    private var setupStep: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(CopyZh.scenarioSetup).font(.headline)
            if targets.isEmpty {
                Text("还没有可选的\(isIdentity ? CopyZh.phoneNumber : "支付工具")。").foregroundStyle(.secondary)
            } else {
                ForEach(targets, id: \.id) { n in
                    Button {
                        targetNodeId = n.id
                    } label: {
                        PdigCard {
                            HStack {
                                Text(n.name)
                                Spacer()
                                if targetNodeId == n.id {
                                    Image(systemName: "checkmark.circle.fill").foregroundStyle(.tint)
                                }
                            }
                        }
                    }
                    .buttonStyle(.plain)
                }
            }
            if let errorText = errorText {
                NoticeBanner(icon: "exclamationmark.triangle.fill", text: errorText, color: PdigTheme.Color.danger)
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
        } catch ScenarioFlowError.noExecutableTarget {
            errorText = CopyZh.actionFailed
        } catch {
            // 原始 Swift Error 禁止直接上屏：统一人话 + 重试入口。
            errorText = CopyZh.actionFailed
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
                        PdigCard {
                            VStack(alignment: .leading, spacing: 2) {
                                Text("\(session.snapshot.name(of: p.from)) → \(session.snapshot.name(of: p.to))")
                                    .font(.body)
                                Text("\(CopyZh.relation(p.relation.wire)) · \(CopyZh.capability(p.capability.wire))")
                                    .font(.caption).foregroundStyle(.secondary)
                            }
                        }
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
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(PdigTheme.Color.warning.opacity(0.08), in: RoundedRectangle(cornerRadius: PdigTheme.Radius.md, style: .continuous))
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
            Text(CopyZh.planChangePlan).font(.headline)
            if let plan = currentPlan {
                let view = planView(for: plan)
                Text("状态：\(view.workflowText)").font(.body)
                Text(view.readinessText).font(.body).foregroundStyle(.secondary)
                if let result = scenarioResult(for: plan), let mbb = result.makeBeforeBreak {
                    NoticeBanner(
                        icon: mbb.status == .blocked ? "exclamationmark.triangle.fill" : "checkmark.circle.fill",
                        text: mbb.status == .blocked ? CopyZh.waitingVerification : CopyZh.newPathVerified,
                        color: mbb.status == .blocked ? PdigTheme.Color.warning : PdigTheme.Color.success
                    )
                }
            }
            Button { step = 5 } label: { Text(CopyZh.next).frame(maxWidth: .infinity).padding(.vertical, 8) }
                .buttonStyle(.borderedProminent)
        }
    }

    private var actionStep: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(CopyZh.planExecute).font(.headline)
            if let plan = currentPlan {
                let view = planView(for: plan)
                ForEach(view.actions) { a in
                    PdigCard {
                        HStack(alignment: .top) {
                            Image(systemName: a.done ? "checkmark.circle.fill" : "circle")
                                .foregroundStyle(a.done ? PdigTheme.Color.success : PdigTheme.Color.textSecondary)
                            VStack(alignment: .leading, spacing: 2) {
                                Text(a.title).font(.body)
                                Text(a.prerequisiteText).font(.caption).foregroundStyle(PdigTheme.Color.warning)
                                Text(a.verificationStatusText).font(.caption).foregroundStyle(.secondary)
                            }
                            Spacer()
                        }
                    }
                }
            }
            Button { step = 6 } label: { Text(CopyZh.next).frame(maxWidth: .infinity).padding(.vertical, 8) }
                .buttonStyle(.borderedProminent)
        }
    }

    private var verificationStep: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(CopyZh.planPhaseVerify).font(.headline)
            NoticeBanner(icon: "clock.fill", text: CopyZh.verificationNewPathIsNotVerified, color: PdigTheme.Color.warning)
            if let plan = currentPlan {
                let view = planView(for: plan)
                ForEach(view.actions.filter { $0.verificationStatusText != CopyZh.verificationNotRequired }) { a in
                    PdigCard {
                        HStack {
                            Text(a.title).font(.body)
                            Spacer()
                            Text(a.verificationStatusText).font(.caption).foregroundStyle(.secondary)
                        }
                    }
                }
            }
            Button { step = 7 } label: { Text(CopyZh.next).frame(maxWidth: .infinity).padding(.vertical, 8) }
                .buttonStyle(.borderedProminent)
        }
    }

    private var completionStep: some View {
        VStack(spacing: 16) {
            Image(systemName: "checkmark.seal.fill").font(.system(size: 48)).foregroundStyle(PdigTheme.Color.success)
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
                Text(reason).font(.footnote).foregroundStyle(PdigTheme.Color.warning)
            }
            if let mbb = result.makeBeforeBreak {
                Text(mbb.status == .blocked ? CopyZh.verifyBeforeRemove : CopyZh.oldPathRetired)
                    .font(.footnote)
                    .foregroundStyle(mbb.status == .blocked ? PdigTheme.Color.warning : PdigTheme.Color.success)
            }
        }
        .padding(10)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(PdigTheme.Color.warning.opacity(0.08), in: RoundedRectangle(cornerRadius: PdigTheme.Radius.md, style: .continuous))
    }
}