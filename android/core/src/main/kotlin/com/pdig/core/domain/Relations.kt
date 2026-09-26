package com.pdig.core.domain

import com.pdig.core.generated.Capability
import com.pdig.core.generated.ChangePlanWorkflowState
import com.pdig.core.generated.CoverageLevel
import com.pdig.core.generated.GroupMode
import com.pdig.core.generated.NodeKind
import com.pdig.core.generated.PlanReadiness
import com.pdig.core.generated.Relation

/**
 * RelationDefinitionRegistry —— relation 的治理定义。
 *
 * v0.3.0 runtime 正式支持 funding_source / merchant_agreement / recovers /
 * authenticates / controls（与 core/src/domain/relation-registry.ts 同口径）。
 * Future / legacy 词表（verifies / bound_to / card_on_file 等）
 * **不进入 runtime validation**，即便 DB CHECK 允许它们存在。
 *
 * 这是一个真实的历史缺陷防线：legacy UI 的 DECLARABLE_RELATIONS 曾暴露
 * `bound_to`，而 registry 并不承认它 —— 用户选了会被后端拒绝。
 * 见 LEGACY_BEHAVIOR_CORRECTIONS.md LC-003。Native 端不再重演。
 */


data class RelationDefinition(
    val id: Relation,
    val fromKinds: List<NodeKind>,
    val toKinds: List<NodeKind>,
    val capability: Capability,
    val allowsGroup: Boolean,
    val allowedGroupModes: List<GroupMode>,
    val defaultCriticality: com.pdig.core.generated.Criticality,
)

private val PAYMENT_KINDS = listOf(
    NodeKind.PAYMENT_INSTRUMENT,
    NodeKind.ACCOUNT,
    NodeKind.SERVICE,
    NodeKind.MEMBERSHIP,
)

val RELATION_DEFINITIONS: List<RelationDefinition> = listOf(
    RelationDefinition(
        id = Relation.FUNDING_SOURCE,
        fromKinds = PAYMENT_KINDS,
        toKinds = listOf(NodeKind.ACCOUNT, NodeKind.PAYMENT_INSTRUMENT),
        capability = Capability.PAYMENT,
        allowsGroup = true,
        allowedGroupModes = listOf(GroupMode.ANY),
        defaultCriticality = com.pdig.core.generated.Criticality.UNKNOWN,
    ),
    RelationDefinition(
        id = Relation.MERCHANT_AGREEMENT,
        fromKinds = listOf(NodeKind.ACCOUNT, NodeKind.PAYMENT_INSTRUMENT),
        toKinds = listOf(NodeKind.SERVICE, NodeKind.MEMBERSHIP, NodeKind.ACCOUNT),
        capability = Capability.PAYMENT,
        allowsGroup = false,
        allowedGroupModes = emptyList(),
        defaultCriticality = com.pdig.core.generated.Criticality.UNKNOWN,
    ),
    RelationDefinition(
        id = Relation.RECOVERS,
        fromKinds = listOf(NodeKind.IDENTITY_ANCHOR, NodeKind.ACCOUNT, NodeKind.DEVICE, NodeKind.SERVICE),
        toKinds = listOf(NodeKind.ACCOUNT, NodeKind.SERVICE, NodeKind.IDENTITY_ANCHOR),
        capability = Capability.RECOVERY,
        allowsGroup = true,
        allowedGroupModes = listOf(GroupMode.ANY),
        defaultCriticality = com.pdig.core.generated.Criticality.UNKNOWN,
    ),
    RelationDefinition(
        id = Relation.AUTHENTICATES,
        fromKinds = listOf(NodeKind.IDENTITY_ANCHOR, NodeKind.ACCOUNT, NodeKind.DEVICE),
        toKinds = listOf(NodeKind.ACCOUNT, NodeKind.SERVICE),
        capability = Capability.AUTHENTICATION,
        allowsGroup = true,
        allowedGroupModes = listOf(GroupMode.ANY),
        defaultCriticality = com.pdig.core.generated.Criticality.UNKNOWN,
    ),
    RelationDefinition(
        id = Relation.CONTROLS,
        fromKinds = listOf(NodeKind.ACCOUNT, NodeKind.DEVICE),
        toKinds = listOf(NodeKind.ACCOUNT, NodeKind.SERVICE, NodeKind.IDENTITY_ANCHOR),
        capability = Capability.ACCESS,
        allowsGroup = false,
        allowedGroupModes = emptyList(),
        defaultCriticality = com.pdig.core.generated.Criticality.UNKNOWN,
    ),
)

private val RELATION_BY_ID = RELATION_DEFINITIONS.associateBy { it.id.wire }

fun getRelationDefinition(id: String): RelationDefinition? = RELATION_BY_ID[id]

fun listRuntimeRelationIds(): List<String> = RELATION_DEFINITIONS.map { it.id.wire }

data class RelationValidationResult(val ok: Boolean, val reason: String? = null)

/** 校验一次 relation 使用（fromKind / relation / toKind / capability）。 */
fun validateRelationUse(
    fromKind: NodeKind?,
    relation: String,
    toKind: NodeKind?,
    capability: String,
): RelationValidationResult {
    val def = RELATION_BY_ID[relation]
        ?: return RelationValidationResult(false, "relation '$relation' is not in the runtime registry")
    if (capability != def.capability.wire) {
        return RelationValidationResult(
            false,
            "relation '$relation' requires capability '${def.capability.wire}', got '$capability'",
        )
    }
    if (fromKind != null && !def.fromKinds.contains(fromKind)) {
        return RelationValidationResult(false, "relation '$relation' does not allow fromKind '${fromKind.wire}'")
    }
    if (toKind != null && !def.toKinds.contains(toKind)) {
        return RelationValidationResult(false, "relation '$relation' does not allow toKind '${toKind.wire}'")
    }
    return RelationValidationResult(true)
}

/** 校验 Group 使用（relation 是否允许 Group 及模式）。 */
fun validateRelationGroupUse(relation: String, mode: GroupMode): RelationValidationResult {
    val def = RELATION_BY_ID[relation]
        ?: return RelationValidationResult(false, "relation '$relation' is not in the runtime registry")
    if (!def.allowsGroup) {
        return RelationValidationResult(false, "relation '$relation' does not allow groups")
    }
    if (!def.allowedGroupModes.contains(mode)) {
        return RelationValidationResult(false, "relation '$relation' does not allow group mode '${mode.wire}'")
    }
    return RelationValidationResult(true)
}
