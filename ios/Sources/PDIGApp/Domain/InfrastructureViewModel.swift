// InfrastructureViewModel —— 基础设施屏（按对象 / 按能力）。
//
// 能力分组（copy-zh）：支付 / 身份与恢复 / 访问与认证 / 设备 / 关键服务。
// 对象分组按 NodeKind。均为展示层投影，无领域断言。

import Foundation
import PDIGCore

public struct InfraGroup: Equatable, Sendable, Identifiable {
    public let id: String
    public let title: String
    public let members: [InfraMember]
}

public struct InfraMember: Equatable, Sendable, Identifiable {
    public let id: String
    public let name: String
    public let kindLabel: String
    public let summary: String

    public init(id: String, name: String, kindLabel: String, summary: String) {
        self.id = id
        self.name = name
        self.kindLabel = kindLabel
        self.summary = summary
    }
}

public enum InfrastructureViewModel {

    /// 按对象分组：手机号 / 银行卡 / 账户 / 服务 / 会员 / 设备。
    public static func byItem(_ snapshot: GraphSnapshot) -> [InfraGroup] {
        let order: [(String, NodeKind)] = [
            (CopyZh.phoneNumber, .identityAnchor),
            (CopyZh.relationFundingSource, .paymentInstrument),
            ("账户", .account),
            (CopyZh.capabilityService, .service),
            ("会员", .membership),
            (CopyZh.capabilityDevice, .device),
        ]
        return order.compactMap { (title, kind) in
            let members = snapshot.nodes
                .filter { $0.kind == kind && !$0.archived }
                .sorted { $0.name < $1.name }
                .map { member($0, snapshot: snapshot) }
            guard !members.isEmpty else { return nil }
            return InfraGroup(id: kind.wire, title: title, members: members)
        }
    }

    /// 按能力分组：节点按它参与的 relation capability 归组。
    public static func byCapability(_ snapshot: GraphSnapshot) -> [InfraGroup] {
        let groups: [(String, String)] = [
            (Capability.payment.wire, CopyZh.capabilityPayment),
            (Capability.recovery.wire, CopyZh.capabilityRecovery),
            (Capability.authentication.wire, CopyZh.capabilityAccess),
            (Capability.access.wire, CopyZh.capabilityAccess),
        ]
        var result: [InfraGroup] = []
        for group in groups {
            let nodeIds = Set(
                snapshot.activeDependencies
                    .filter { $0.capability.wire == group.0 }
                    .flatMap { [$0.from, $0.to] }
            )
            let members = snapshot.nodes
                .filter { nodeIds.contains($0.id) && !$0.archived }
                .sorted { $0.name < $1.name }
                .map { member($0, snapshot: snapshot) }
            guard !members.isEmpty else { continue }
            result.append(InfraGroup(id: group.1, title: group.1, members: members))
        }
        // 设备分组（node kind = device）
        let devices = snapshot.nodes.filter { $0.kind == .device && !$0.archived }.sorted { $0.name < $1.name }
        if !devices.isEmpty {
            result.append(InfraGroup(
                id: "device-group",
                title: CopyZh.capabilityDevice,
                members: devices.map { member($0, snapshot: snapshot) }
            ))
        }
        return result
    }

    private static func member(_ n: DepNode, snapshot: GraphSnapshot) -> InfraMember {
        let outgoing = snapshot.activeDependencies.filter { $0.from == n.id }
        let incoming = snapshot.activeDependencies.filter { $0.to == n.id }
        let relations = outgoing.map { CopyZh.relation($0.relation.wire) } + incoming.map { CopyZh.relation($0.relation.wire) }
        let summary = Array(Set(relations)).sorted().joined(separator: "、")
        return InfraMember(
            id: n.id,
            name: n.name,
            kindLabel: CopyZh.nodeKind(n.kind.wire),
            summary: summary.isEmpty ? "尚无确认的关系" : summary
        )
    }
}
