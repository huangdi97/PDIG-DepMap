// ScenarioFlow —— 场景 → 变更计划（App 层装配）。
//
// PDIGCore 提供 ScenarioRegistry（模板）、ImpactKernel（影响）、MakeBeforeBreak /
// TemporalChange（换号闸门）、ActionDag（前置顺序），但**没有**"场景实例化"工厂：
// Android 在 AppContainer.createPlanForScenario 里做，本模块在 PDIGApp 层实现，
// 不改 PDIGCore 语义（planned 模板仍不可由 registry 执行）。
//
// 支持的场景：
//   - 支付（replace_payment_card / expiring_payment_card / close_payment_instrument）
//   - 身份与恢复（replace_phone_number）：App 层按 v0.3.0 产品能力激活，
//     使用 make-before-break 语言与顺序闸门（PDIGCore 引擎评估）。

import Foundation
import PDIGCore

/// 场景实例化结果：计划 + 前置依赖边 + 时间窗/断连闸门结果。
public struct ScenarioPlan: Equatable, Sendable {
    public let plan: ChangePlan
    /// actionId → 它的前置 action ids（plain-language 展示用）。
    public let prerequisiteEdges: [String: [String]]
    public let impact: ImpactResult?
    /// replace_phone_number 特有：时间窗阶段与停用闸门。
    public let temporal: TemporalChangeResult?
    public let makeBeforeBreak: MakeBeforeBreakResult?
    /// 是否属于身份与恢复场景（决定 UI 是否展示换号流程步骤）。
    public let isIdentityFlow: Bool
}

public enum ScenarioFlowError: Error, CustomStringConvertible {
    case unknownScenario(String)
    case noExecutableTarget(String)
    public var description: String {
        switch self {
        case .unknownScenario(let s): return "未知场景：\(s)"
        case .noExecutableTarget(let s): return "目标不存在或不可执行：\(s)"
        }
    }
}

public enum ScenarioFlow {

    public static let replacePhoneScenarioId = "replace_phone_number"

    /// 场景中心展示用分组。
    public struct CatalogEntry: Equatable, Sendable, Identifiable {
        public let id: String
        public let title: String
        public let subtitle: String
        public let category: String
        public let availability: String
    }

    public static func catalog() -> [CatalogEntry] {
        var entries = ScenarioRegistry.active.map { t in
            CatalogEntry(
                id: t.id,
                title: t.title,
                subtitle: t.description,
                category: CopyZh.scenarioPaymentCategory,
                availability: t.availability
            )
        }
        entries.append(CatalogEntry(
            id: replacePhoneScenarioId,
            title: CopyZh.scenarioReplacePhone,
            subtitle: "手机号是许多账户的验证与恢复入口。更换前先建立并验证新路径，再停用旧路径。",
            category: CopyZh.scenarioIdentityRecoveryCategory,
            availability: "active"
        ))
        return entries
    }

    // MARK: - 实例化

    /// 创建计划。identity 场景使用 make-before-break 动作序列；
    /// payment 场景用 ImpactKernel 的确定性失效传播驱动 must_change 动作。
    public static func createPlan(
        scenarioId: String,
        targetNodeId: String,
        graph: GraphSnapshot,
        effectiveDate: String?,
        nowIso: String = "2030-01-01T00:00:00+00:00"
    ) throws -> ScenarioPlan {
        guard graph.node(id: targetNodeId) != nil else {
            throw ScenarioFlowError.noExecutableTarget(targetNodeId)
        }
        if scenarioId == replacePhoneScenarioId {
            return try identityPlan(targetNodeId: targetNodeId, graph: graph, nowIso: nowIso)
        }
        guard let template = ScenarioRegistry.get(scenarioId), ScenarioRegistry.isExecutable(scenarioId) else {
            throw ScenarioFlowError.unknownScenario(scenarioId)
        }
        return paymentPlan(template: template, targetNodeId: targetNodeId, graph: graph, effectiveDate: effectiveDate)
    }

    // MARK: - payment

    private static func paymentPlan(
        template: ScenarioTemplate,
        targetNodeId: String,
        graph: GraphSnapshot,
        effectiveDate: String?
    ) -> ScenarioPlan {
        let impactGraph = ImpactGraph(
            dependencies: graph.activeDependencies,
            groups: graph.groups.filter { $0.state == .active },
            proposals: graph.pendingProposals.map { p in
                ImpactProposalInput(key: p.key, from: p.from, to: p.to, capability: p.capability, confidenceScore: p.confidenceScore)
            },
            nodeNames: Dictionary(uniqueKeysWithValues: graph.nodes.map { ($0.id, $0.name) })
        )
        let impact = ImpactKernel.simulateDisable(graph: impactGraph, nodeId: targetNodeId)

        var actions: [PlanAction] = []
        // prepare：确认关键支付路径
        actions.append(PlanAction(
            id: "\(targetNodeId)-prepare",
            title: "确认 \(graph.name(of: targetNodeId)) 的关键支付路径",
            phase: .prepare
        ))
        // change：每个 lost key 一条"迁移支付来源"动作
        for key in impact.lostKeys {
            let target = graph.name(of: key.nodeId)
            actions.append(PlanAction(
                id: "\(key.nodeId)-migrate-\(key.capability.wire)",
                title: "为 \(target) 添加新的\(CopyZh.capability(key.capability.wire))来源",
                phase: .change,
                resolvesImpactKeys: [key.keyString()]
            ))
        }
        // verify：每条迁移对应一条验证动作
        for key in impact.lostKeys {
            actions.append(PlanAction(
                id: "\(key.nodeId)-verify-\(key.capability.wire)",
                title: "验证 \(graph.name(of: key.nodeId)) 的新\(CopyZh.capability(key.capability.wire))路径",
                phase: .verify,
                verification: ActionVerification(
                    method: .futureObservation,
                    status: .pending,
                    expectedFromNodeId: key.nodeId,
                    expectedToNodeId: nil
                )
            ))
        }
        // 最后一步：原始操作
        actions.append(PlanAction(
            id: "\(targetNodeId)-final",
            title: CopyZh.impactTargetOperation + "：\(graph.name(of: targetNodeId))",
            phase: .verify
        ))

        // 前置边：迁移 → 验证；prepare → 迁移；final 在所有验证之后
        var edges: [String: [String]] = [:]
        for key in impact.lostKeys {
            edges["\(key.nodeId)-migrate-\(key.capability.wire)"] = ["\(targetNodeId)-prepare"]
            edges["\(key.nodeId)-verify-\(key.capability.wire)"] = ["\(key.nodeId)-migrate-\(key.capability.wire)"]
        }
        let verifyIds = impact.lostKeys.map { "\($0.nodeId)-verify-\($0.capability.wire)" }
        if !verifyIds.isEmpty {
            edges["\(targetNodeId)-final"] = verifyIds
        } else {
            edges["\(targetNodeId)-final"] = ["\(targetNodeId)-prepare"]
        }

        let plan = ChangePlan(
            id: "plan-\(template.id)-\(targetNodeId)-\(graph.graphRevision)",
            templateId: template.id,
            scenario: template.id,
            title: "\(template.title) · \(graph.name(of: targetNodeId))",
            workflowState: .analyzed,
            baselineGraphRevision: graph.graphRevision,
            lastAnalyzedGraphRevision: graph.graphRevision,
            targetNodeId: targetNodeId,
            effectiveDate: effectiveDate,
            actions: actions
        )
        return ScenarioPlan(
            plan: plan,
            prerequisiteEdges: edges,
            impact: impact,
            temporal: nil,
            makeBeforeBreak: nil,
            isIdentityFlow: false
        )
    }

    // MARK: - identity（replace_phone_number）

    /// 换号流程（copy-zh.scenarioV030 + changePlanV030）：
    ///   准备 → 添加新手机号 → 迁移关键账户 → 验证新手机号 → 停用旧手机号（全部验证后）。
    /// 闸门：MakeBeforeBreakEngine（未建立/未验证 → 停用被阻止）+ TemporalChangeEngine。
    private static func identityPlan(
        targetNodeId: String,
        graph: GraphSnapshot,
        nowIso: String
    ) throws -> ScenarioPlan {
        let oldPhone = graph.name(of: targetNodeId)
        let newPhoneNodeId = "phone-new-\(targetNodeId)"

        let actionIds = [
            "phone-prepare",
            "phone-add-new",
            "phone-migrate",
            "phone-verify-new",
            "phone-retire-old",
        ]

        let actions: [PlanAction] = [
            PlanAction(
                id: "phone-prepare",
                title: "确认 \(oldPhone) 关联的账户与恢复路径",
                phase: .prepare
            ),
            PlanAction(
                id: "phone-add-new",
                title: CopyZh.addNewPhone,
                phase: .change,
                resolvesImpactKeys: [ImpactStateKey(targetNodeId, .recovery).keyString()]
            ),
            PlanAction(
                id: "phone-migrate",
                title: CopyZh.migrateAccounts,
                phase: .change
            ),
            PlanAction(
                id: "phone-verify-new",
                title: CopyZh.verifyNewPhone,
                phase: .verify,
                verification: ActionVerification(
                    method: .futureObservation,
                    status: .pending,
                    expectedFromNodeId: newPhoneNodeId,
                    expectedToNodeId: nil
                )
            ),
            PlanAction(
                id: "phone-retire-old",
                title: CopyZh.retireOldPhone,
                phase: .change
            ),
        ]

        // 前置边（plain-language 顺序）
        let edges: [String: [String]] = [
            "phone-add-new": ["phone-prepare"],
            "phone-migrate": ["phone-add-new"],
            "phone-verify-new": ["phone-migrate"],
            "phone-retire-old": ["phone-verify-new"],  // 验证后才能移除旧路径
        ]

        // 时间窗：验证不早于开始、停用不早于验证截止
        let window = TemporalChangeWindow(
            effectiveAt: nowIso,
            verificationNotBefore: nowIso,
            verificationDueAt: "2030-01-31T00:00:00+00:00",
            retireOldPathAfter: "2030-02-28T00:00:00+00:00"
        )
        let temporal = TemporalChangeEngine.classifyTemporalPhase(
            window: window,
            now: nowIso,
            allKeyNewPathsVerified: false
        )

        // 停用闸门：新路径（add + migrate）done 且验证 verified 才允许停用
        let mbb = MakeBeforeBreakEngine.evaluate(MakeBeforeBreakInput(
            newPathActions: [actions[1], actions[2]],
            verificationActions: [actions[3]],
            retireActionId: "phone-retire-old",
            retireAlreadyDone: false
        ))

        let plan = ChangePlan(
            id: "plan-\(replacePhoneScenarioId)-\(targetNodeId)-\(graph.graphRevision)",
            templateId: replacePhoneScenarioId,
            scenario: replacePhoneScenarioId,
            title: "\(CopyZh.scenarioReplacePhone) · \(oldPhone)",
            workflowState: .analyzed,
            baselineGraphRevision: graph.graphRevision,
            lastAnalyzedGraphRevision: graph.graphRevision,
            targetNodeId: targetNodeId,
            effectiveDate: nowIso,
            actions: actions
        )
        return ScenarioPlan(
            plan: plan,
            prerequisiteEdges: edges,
            impact: nil,
            temporal: temporal,
            makeBeforeBreak: mbb,
            isIdentityFlow: true
        )
    }

    /// 换号流程的步进阶段（UI 顶部进度条）。
    public enum IdentityStep: Int, CaseIterable, Equatable, Sendable {
        case setup = 0
        case impact
        case recoveryPaths
        case failureDomains
        case changePlan
        case action
        case verification
        case completion

        public var label: String {
            switch self {
            case .setup: return CopyZh.scenarioSetup
            case .impact: return CopyZh.reviewImpact
            case .recoveryPaths: return CopyZh.reviewRecoveryPaths
            case .failureDomains: return CopyZh.reviewFailureDomains
            case .changePlan: return "变更计划"
            case .action: return "执行变更"
            case .verification: return "验证"
            case .completion: return CopyZh.scenarioComplete
            }
        }
    }
}
