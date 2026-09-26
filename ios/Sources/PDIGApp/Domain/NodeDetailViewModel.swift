// NodeDetailViewModel —— 节点详情「六问卡」。
//
// 卡片（copy-zh）：是什么 / 确认了什么 / 待处理问题 / 依据与最近确认。
// 全部派生自已确认图数据与派生投影，不产生领域断言。

import Foundation
import PDIGCore

public struct NodeCard: Equatable, Sendable, Identifiable {
    public let id: String
    public let title: String
    public let body: String
}

public struct NodeDetail: Equatable, Sendable {
    public let node: DepNode
    public let kindLabel: String
    public let cards: [NodeCard]
}

public enum NodeDetailViewModel {

    public static func detail(nodeId: String, snapshot: GraphSnapshot, findings: [FindingItem]) -> NodeDetail? {
        guard let node = snapshot.node(id: nodeId) else { return nil }
        let outgoing = snapshot.activeDependencies.filter { $0.from == nodeId }
        let incoming = snapshot.activeDependencies.filter { $0.to == nodeId }
        let relatedFindings = findings.filter { $0.targetNodeId == nodeId }

        var cards: [NodeCard] = []

        // 是什么
        let identity = [
            CopyZh.nodeKind(node.kind.wire),
            node.issuer.map { "发卡/服务商：\($0)" },
            node.last4.map { "尾号：\($0)" },
        ].compactMap { $0 }
        cards.append(NodeCard(id: "what", title: CopyZh.nodeWhat, body: identity.joined(separator: "，")))

        // 确认了什么
        let confirmedLines = outgoing.map { dep -> String in
            "\(CopyZh.relation(dep.relation.wire))：\(node.name) → \(snapshot.name(of: dep.to))（\(criticalityLabel(dep.criticality))）"
        } + incoming.map { dep -> String in
            "\(CopyZh.relation(dep.relation.wire))：\(snapshot.name(of: dep.from)) → \(node.name)（\(criticalityLabel(dep.criticality))）"
        }
        cards.append(NodeCard(
            id: "confirmed",
            title: CopyZh.nodeConfirmed,
            body: confirmedLines.isEmpty ? "还没有确认的关系。" : confirmedLines.joined(separator: "\n")
        ))

        // 待处理问题
        var openQuestions: [String] = []
        let unknownCritical = (outgoing + incoming).filter { $0.criticality == .unknown }
        if !unknownCritical.isEmpty {
            openQuestions.append("有 \(unknownCritical.count) 条关系是否必需尚未确认。")
        }
        let stale = (outgoing + incoming).filter { $0.lastVerifiedAt.isEmpty }
        if !stale.isEmpty {
            openQuestions.append("有 \(stale.count) 条关系从未确认过。")
        }
        for f in relatedFindings {
            openQuestions.append(f.unknowns)
        }
        cards.append(NodeCard(
            id: "open",
            title: CopyZh.nodeOpenQuestions,
            body: openQuestions.isEmpty ? "没有待处理问题。" : openQuestions.joined(separator: "\n")
        ))

        // 依据与最近确认
        let basis = (outgoing + incoming).map { dep -> String in
            let date = dep.lastVerifiedAt.isEmpty ? "从未" : String(dep.lastVerifiedAt.prefix(10))
            return "\(dep.id)：\(date)（\(dep.origin == .manual ? "用户确认" : "导入建议")）"
        }
        cards.append(NodeCard(
            id: "evidence",
            title: CopyZh.nodeEvidence,
            body: basis.isEmpty ? "暂无依据。" : basis.joined(separator: "\n")
        ))

        return NodeDetail(node: node, kindLabel: CopyZh.nodeKind(node.kind.wire), cards: cards)
    }

    private static func criticalityLabel(_ c: Criticality) -> String {
        c == .required ? CopyZh.criticalityRequired : CopyZh.criticalityUnknown
    }
}
