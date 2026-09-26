// DemoData —— 演示图（截图 harness 与单测共用）。
//
// 全合成数据：银行卡 ×2、手机号、设备、账户、服务、会员；支付/恢复/认证关系；
// 一组薄弱点（单点/共享故障点/恢复循环）+ 一张 replace_phone 计划。

import Foundation
import PDIGCore

public enum DemoData {

    public static let nowIso = "2030-01-15T00:00:00+00:00"

    public static func snapshot() -> GraphSnapshot {
        let nodes = [
            DepNode(id: "card-main", kind: .paymentInstrument, name: "招商银行卡", issuer: "招商银行", last4: "1234"),
            DepNode(id: "card-backup", kind: .paymentInstrument, name: "工商银行卡", issuer: "工商银行", last4: "5678"),
            DepNode(id: "phone-1", kind: .identityAnchor, name: "138****0001"),
            DepNode(id: "device-1", kind: .device, name: "iPhone"),
            DepNode(id: "acct-bank", kind: .account, name: "招商银行账户", issuer: "招商银行"),
            DepNode(id: "svc-video", kind: .service, name: "视频会员"),
            DepNode(id: "svc-cloud", kind: .service, name: "云存储"),
            DepNode(id: "mbr-express", kind: .membership, name: "快递会员"),
        ]
        let deps = [
            Dependency(id: "d1", from: "card-main", relation: .fundingSource, to: "acct-bank", capability: .payment, criticality: .required, lastVerifiedAt: "2029-12-01T00:00:00+00:00"),
            Dependency(id: "d2", from: "card-main", relation: .fundingSource, to: "svc-video", capability: .payment, criticality: .unknown, lastVerifiedAt: "2029-12-01T00:00:00+00:00"),
            Dependency(id: "d3", from: "card-main", relation: .merchantAgreement, to: "svc-cloud", capability: .payment, criticality: .required, lastVerifiedAt: "2029-11-20T00:00:00+00:00"),
            Dependency(id: "d4", from: "card-backup", relation: .fundingSource, to: "acct-bank", capability: .payment, criticality: .unknown, lastVerifiedAt: "2029-10-10T00:00:00+00:00"),
            Dependency(id: "d5", from: "card-main", relation: .merchantAgreement, to: "mbr-express", capability: .payment, criticality: .unknown, lastVerifiedAt: "2029-09-01T00:00:00+00:00"),
            // 恢复路径：两条都依赖同一台设备（共享故障点）
            Dependency(id: "r1", from: "phone-1", relation: .recovers, to: "acct-bank", capability: .recovery, criticality: .required, lastVerifiedAt: "2029-12-01T00:00:00+00:00"),
            Dependency(id: "r2", from: "device-1", relation: .recovers, to: "acct-bank", capability: .recovery, criticality: .unknown, lastVerifiedAt: "2029-12-01T00:00:00+00:00"),
            Dependency(id: "r3", from: "phone-1", relation: .authenticates, to: "svc-video", capability: .authentication, criticality: .unknown, lastVerifiedAt: "2029-12-01T00:00:00+00:00"),
            // 恢复循环：acct-bank 通过 phone-1 恢复，phone-1 又通过 acct-bank 找回
            Dependency(id: "c1", from: "acct-bank", relation: .recovers, to: "phone-1", capability: .recovery, criticality: .unknown, lastVerifiedAt: "2029-12-01T00:00:00+00:00"),
            Dependency(id: "c2", from: "device-1", relation: .controls, to: "acct-bank", capability: .access, criticality: .unknown, lastVerifiedAt: "2029-12-01T00:00:00+00:00"),
        ]
        return GraphSnapshot(
            nodes: nodes,
            dependencies: deps,
            groups: [],
            pendingProposals: [],
            sources: [
                SourceInstanceRow(id: "src-wechat-1", adapterId: "wechat_statement", adapterVersion: 1, sourceKind: SourceKind.statementFile.wire, label: "微信账单 2024.csv", lastIngestedAt: "2029-12-01T00:00:00+00:00"),
            ],
            graphRevision: 3
        )
    }

    public static func plans() -> [ChangePlan] {
        guard let plan = try? ScenarioFlow.createPlan(
            scenarioId: ScenarioFlow.replacePhoneScenarioId,
            targetNodeId: "phone-1",
            graph: snapshot(),
            effectiveDate: nowIso,
            nowIso: nowIso
        ) else { return [] }
        return [plan.plan]
    }
}