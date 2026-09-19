// RelationDefinitionRegistry —— relation 的治理定义（Swift 移植）。
//
// 源头：android/core/.../domain/Relations.kt。
//
// MVP02 runtime 只正式支持 funding_source / merchant_agreement。
// Future / legacy 词表（verifies / recovers / bound_to / card_on_file 等）
// **不进入 runtime validation**，即便 DB CHECK 允许它们存在。
//
// 这是一条真实的历史缺陷防线：legacy UI 的 DECLARABLE_RELATIONS 曾暴露 `bound_to`，
// 而 registry 并不承认它 —— 用户选了会被后端拒绝。
// 见 LEGACY_BEHAVIOR_CORRECTIONS.md LC-003。Native 端不再重演。

import Foundation

public struct RelationDefinition: Equatable, Sendable {
    public let id: Relation
    public let fromKinds: [NodeKind]
    public let toKinds: [NodeKind]
    public let capability: Capability
    public let allowsGroup: Bool
    public let allowedGroupModes: [GroupMode]
    public let defaultCriticality: Criticality

    public init(
        id: Relation,
        fromKinds: [NodeKind],
        toKinds: [NodeKind],
        capability: Capability,
        allowsGroup: Bool,
        allowedGroupModes: [GroupMode],
        defaultCriticality: Criticality
    ) {
        self.id = id
        self.fromKinds = fromKinds
        self.toKinds = toKinds
        self.capability = capability
        self.allowsGroup = allowsGroup
        self.allowedGroupModes = allowedGroupModes
        self.defaultCriticality = defaultCriticality
    }
}

public struct RelationValidationResult: Equatable, Sendable {
    public let ok: Bool
    public let reason: String?

    public init(ok: Bool, reason: String? = nil) {
        self.ok = ok
        self.reason = reason
    }
}

public enum RelationRegistry {
    private static let paymentKinds: [NodeKind] = [
        .paymentInstrument, .account, .service, .membership,
    ]

    public static let definitions: [RelationDefinition] = [
        RelationDefinition(
            id: .fundingSource,
            fromKinds: paymentKinds,
            toKinds: [.account, .paymentInstrument],
            capability: .payment,
            allowsGroup: true,
            allowedGroupModes: [.any],
            defaultCriticality: .unknown
        ),
        RelationDefinition(
            id: .merchantAgreement,
            fromKinds: [.account, .paymentInstrument],
            toKinds: [.service, .membership, .account],
            capability: .payment,
            allowsGroup: false,
            allowedGroupModes: [],
            defaultCriticality: .unknown
        ),
    ]

    public static func get(_ id: String) -> RelationDefinition? {
        definitions.first { $0.id.wire == id }
    }

    public static func listRuntimeRelationIds() -> [String] {
        definitions.map { $0.id.wire }
    }

    /// 校验一次 relation 使用（fromKind / relation / toKind / capability）。
    public static func validateUse(
        fromKind: NodeKind?,
        relation: String,
        toKind: NodeKind?,
        capability: String
    ) -> RelationValidationResult {
        guard let def = get(relation) else {
            return RelationValidationResult(
                ok: false,
                reason: "relation '\(relation)' is not in the runtime registry"
            )
        }
        if capability != def.capability.wire {
            return RelationValidationResult(
                ok: false,
                reason: "relation '\(relation)' requires capability '\(def.capability.wire)', got '\(capability)'"
            )
        }
        if let fk = fromKind, !def.fromKinds.contains(fk) {
            return RelationValidationResult(
                ok: false,
                reason: "relation '\(relation)' does not allow fromKind '\(fk.wire)'"
            )
        }
        if let tk = toKind, !def.toKinds.contains(tk) {
            return RelationValidationResult(
                ok: false,
                reason: "relation '\(relation)' does not allow toKind '\(tk.wire)'"
            )
        }
        return RelationValidationResult(ok: true)
    }

    /// 校验 Group 使用（relation 是否允许 Group 及模式）。
    public static func validateGroupUse(
        relation: String,
        mode: GroupMode
    ) -> RelationValidationResult {
        guard let def = get(relation) else {
            return RelationValidationResult(
                ok: false,
                reason: "relation '\(relation)' is not in the runtime registry"
            )
        }
        if !def.allowsGroup {
            return RelationValidationResult(
                ok: false,
                reason: "relation '\(relation)' does not allow groups"
            )
        }
        if !def.allowedGroupModes.contains(mode) {
            return RelationValidationResult(
                ok: false,
                reason: "relation '\(relation)' does not allow group mode '\(mode.wire)'"
            )
        }
        return RelationValidationResult(ok: true)
    }
}
