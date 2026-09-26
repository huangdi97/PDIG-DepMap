// HomeViewModel —— 首页投影。
//
// 分区（copy-zh.home）：
//   需要你处理 / 可能发生了变化 / 即将到来 / 常用场景 / 我的基础设施 / 基础设施薄弱点
//
// 数据全部来自 PDIGCore 引擎的派生投影（Timeline / Impact / Findings），
// 本模块只做展示层聚合，不产生任何领域断言。

import Foundation
import PDIGCore

public struct HomeSection: Equatable, Sendable {
    public let title: String
    public let items: [HomeItem]
}

public struct HomeItem: Equatable, Sendable, Identifiable {
    public let id: String
    public let title: String
    public let subtitle: String
    /// 内部路由目标（UI 决定跳转）。
    public let routeHint: String
    /// 严重程度：0 普通 / 1 提示 / 2 警告。
    public let severity: Int

    public init(id: String, title: String, subtitle: String, routeHint: String, severity: Int = 0) {
        self.id = id
        self.title = title
        self.subtitle = subtitle
        self.routeHint = routeHint
        self.severity = severity
    }
}

public struct HomeInput: Equatable, Sendable {
    public let snapshot: GraphSnapshot
    public let timelineItems: [Timeline.Item]
    public let openDrifts: [GraphRepository.DriftRow]
    public let scenarioTemplates: [ScenarioTemplate]
    public let findings: [FindingItem]

    public init(
        snapshot: GraphSnapshot,
        timelineItems: [Timeline.Item] = [],
        openDrifts: [GraphRepository.DriftRow] = [],
        scenarioTemplates: [ScenarioTemplate] = ScenarioRegistry.active,
        findings: [FindingItem] = []
    ) {
        self.snapshot = snapshot
        self.timelineItems = timelineItems
        self.openDrifts = openDrifts
        self.scenarioTemplates = scenarioTemplates
        self.findings = findings
    }
}

public enum HomeViewModel {

    public static func sections(_ input: HomeInput) -> [HomeSection] {
        var sections: [HomeSection] = []

        // 需要你处理：Timeline attention/overdue 项 + 开放漂移 + 薄弱点
        let urgentTimeline = input.timelineItems.filter {
            $0.bucket == TimelineBucket.attention.rawValue || $0.bucket == TimelineBucket.overdue.rawValue
        }
        var needsAction: [HomeItem] = urgentTimeline.map { item in
            HomeItem(
                id: item.id,
                title: item.title,
                subtitle: item.subtitle,
                routeHint: "timeline",
                severity: item.bucket == TimelineBucket.attention.rawValue ? 2 : 1
            )
        }
        for drift in input.openDrifts {
            needsAction.append(HomeItem(
                id: "drift-\(drift.id)",
                title: CopyZh.homePossibleChange,
                subtitle: "\(CopyZh.relation(drift.capability))：\(input.snapshot.name(of: drift.targetNodeId)) 相关",
                routeHint: "drift",
                severity: 2
            ))
        }
        for f in input.findings.prefix(3) {
            needsAction.append(HomeItem(
                id: "finding-\(f.id)",
                title: f.title,
                subtitle: f.what,
                routeHint: "findings",
                severity: 1
            ))
        }
        if !needsAction.isEmpty {
            sections.append(HomeSection(title: CopyZh.homeNeedsAction, items: needsAction))
        }

        // 可能发生了变化：开放漂移（Home 单列一节，标题与文案一致）
        if !input.openDrifts.isEmpty {
            sections.append(HomeSection(title: CopyZh.homePossibleChange, items: input.openDrifts.map { drift in
                HomeItem(
                    id: drift.id,
                    title: CopyZh.driftPossibleChange,
                    subtitle: "\(input.snapshot.name(of: drift.targetNodeId)) —— \(driftObservationHint(drift.kind))",
                    routeHint: "drift",
                    severity: 2
                )
            }))
        }

        // 即将到来：Timeline 未来档（今天之后）
        let upcoming = input.timelineItems.filter {
            ![TimelineBucket.attention.rawValue, TimelineBucket.overdue.rawValue].contains($0.bucket)
        }
        if !upcoming.isEmpty {
            sections.append(HomeSection(title: CopyZh.homeUpcoming, items: upcoming.map { item in
                HomeItem(
                    id: item.id,
                    title: item.title,
                    subtitle: CopyZh.timelineBucket(item.bucket),
                    routeHint: "timeline",
                    severity: 0
                )
            }))
        }

        // 常用场景
        if !input.scenarioTemplates.isEmpty {
            sections.append(HomeSection(title: CopyZh.homeCommonScenarios, items: input.scenarioTemplates.map { t in
                HomeItem(
                    id: "scenario-\(t.id)",
                    title: t.title,
                    subtitle: t.description,
                    routeHint: "scenario:\(t.id)",
                    severity: 0
                )
            }))
        }

        // 我的基础设施（按 NodeKind 分组简表）
        let infra = input.snapshot.nodes
            .filter { !$0.archived }
            .prefix(8)
            .map { n -> HomeItem in
                HomeItem(
                    id: n.id,
                    title: n.name,
                    subtitle: CopyZh.nodeKind(n.kind.wire),
                    routeHint: "node:\(n.id)",
                    severity: 0
                )
            }
        if !infra.isEmpty {
            sections.append(HomeSection(title: CopyZh.homeMyInfrastructure, items: infra))
        }

        // 基础设施薄弱点
        if !input.findings.isEmpty {
            sections.append(HomeSection(title: CopyZh.homeInfrastructureWeakness, items: input.findings.prefix(5).map { f in
                HomeItem(
                    id: "weak-\(f.id)",
                    title: f.title,
                    subtitle: f.what,
                    routeHint: "findings",
                    severity: 2
                )
            }))
        }

        return sections
    }

    private static func driftObservationHint(_ kind: String) -> String {
        switch kind {
        case DriftKind.possibleReplacement.wire: return "可能是更换了来源"
        case DriftKind.possibleAdditionalPath.wire: return "可能是新增了一条支付路径"
        case DriftKind.relationReappeared.wire: return "可能重新出现了"
        default: return "需要确认"
        }
    }
}
