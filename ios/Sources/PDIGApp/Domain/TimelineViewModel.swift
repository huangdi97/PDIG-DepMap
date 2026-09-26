// TimelineViewModel —— 时间线屏（task #11）。
//
// 薄封装 PDIGCore Timeline.buildTimelinePure（纯投影，不持久化）。
// App 输入：计划 / 开放漂移 / 节点（到期日字段）/ 来源实例。

import Foundation
import PDIGCore

public struct TimelineSection: Equatable, Sendable, Identifiable {
    public let id: String
    public let title: String
    public let items: [Timeline.Item]
}

public enum TimelineViewModel {

    public static func sections(
        snapshot: GraphSnapshot,
        plans: [ChangePlan],
        drifts: [GraphRepository.DriftRow],
        nowIso: String,
        freshnessThresholdDays: Int = 90
    ) -> [TimelineSection] {
        let input = Timeline.Input(
            currentGraphRevision: snapshot.graphRevision,
            plans: plans.map { planInput($0) },
            drifts: drifts.map { driftInput($0) },
            nodes: snapshot.nodes.map { nodeInput($0) },
            sources: snapshot.sources.map { sourceInput($0) }
        )
        let items = Timeline.buildTimelinePure(input, nowIso, freshnessThresholdDays)

        var grouped: [String: [Timeline.Item]] = [:]
        for item in items {
            grouped[item.bucket, default: []].append(item)
        }
        var sections: [TimelineSection] = []
        for bucket in Timeline.bucketOrder {
            guard let list = grouped[bucket], !list.isEmpty else { continue }
            sections.append(TimelineSection(id: bucket, title: CopyZh.timelineBucket(bucket), items: list))
        }
        // 未知 bucket 兜底（不隐藏）
        for (bucket, list) in grouped where !Timeline.bucketOrder.contains(bucket) && !list.isEmpty {
            sections.append(TimelineSection(id: bucket, title: CopyZh.bucketLater, items: list))
        }
        return sections
    }

    private static func planInput(_ p: ChangePlan) -> Timeline.PlanInput {
        Timeline.PlanInput(
            id: p.id,
            title: p.title,
            scenario: p.scenario,
            workflowState: p.workflowState.wire,
            lastAnalyzedGraphRevision: p.lastAnalyzedGraphRevision,
            effectiveDate: p.effectiveDate,
            actions: p.actions.map { a in
                Timeline.ActionInput(
                    id: a.id,
                    title: a.title,
                    phase: a.phase.wire,
                    verification: a.verification.map { v in
                        Timeline.VerificationInput(status: v.status.wire, method: v.method.wire)
                    }
                )
            }
        )
    }

    private static func driftInput(_ d: GraphRepository.DriftRow) -> Timeline.DriftInput {
        Timeline.DriftInput(
            id: d.id,
            kind: d.kind,
            detectedAt: d.detectedAt,
            status: d.status,
            targetNodeId: d.targetNodeId
        )
    }

    private static func nodeInput(_ n: DepNode) -> Timeline.NodeInput {
        // 诚实：DepNode 无到期日字段，不伪造 expiryDate（避免凭空的时间线事项）。
        Timeline.NodeInput(id: n.id, name: n.name, archived: n.archived, fields: [:])
    }

    private static func sourceInput(_ s: SourceInstanceRow) -> Timeline.SourceInput {
        Timeline.SourceInput(id: s.id, label: s.label, state: s.state, lastIngestedAt: s.lastIngestedAt)
    }
}