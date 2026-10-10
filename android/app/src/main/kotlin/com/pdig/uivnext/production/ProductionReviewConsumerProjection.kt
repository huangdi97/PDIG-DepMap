package com.pdig.uivnext.production

/**
 * Consumer-safe Human Review projection.
 *
 * Production proposals/candidates/drifts remain outside Reality until a user
 * chooses an authoritative review action. This layer only translates technical
 * queue vocabulary into consumer language; it never applies a decision.
 */
internal enum class ProductionReviewConsumerKind {
    RELATION_PROPOSAL,
    OBJECT_CANDIDATE,
    REALITY_DRIFT,
}

internal data class ProductionReviewConsumerItem(
    val id: String,
    val kind: ProductionReviewConsumerKind,
    val title: String,
    val summary: String,
    val evidenceSummary: String,
    val decisions: List<String>,
)

internal data class ProductionReviewConsumerInbox(
    val proposals: List<ProductionReviewConsumerItem>,
    val candidates: List<ProductionReviewConsumerItem>,
    val drifts: List<ProductionReviewConsumerItem>,
) {
    val total: Int get() = proposals.size + candidates.size + drifts.size
}

internal fun buildProductionReviewConsumerInbox(
    queue: VNextProductionReviewQueue,
): ProductionReviewConsumerInbox = ProductionReviewConsumerInbox(
    proposals = queue.proposals.map { proposal ->
        ProductionReviewConsumerItem(
            id = proposal.id,
            kind = ProductionReviewConsumerKind.RELATION_PROPOSAL,
            title = proposal.fromName + " → " + proposal.toName,
            summary = relationReviewSummary(
                relation = proposal.relation,
                capability = proposal.capability,
            ),
            evidenceSummary = observationSummary(proposal.observationCount),
            decisions = listOf("确认关系", "拒绝"),
        )
    },
    candidates = queue.candidates.map { candidate ->
        ProductionReviewConsumerItem(
            id = candidate.id,
            kind = ProductionReviewConsumerKind.OBJECT_CANDIDATE,
            title = candidate.label,
            summary = candidateKindSummary(candidate.candidateKind),
            evidenceSummary = observationSummary(candidate.observationCount),
            decisions = listOf("确认对象", "忽略"),
        )
    },
    drifts = queue.drifts.map { drift ->
        ProductionReviewConsumerItem(
            id = drift.id,
            kind = ProductionReviewConsumerKind.REALITY_DRIFT,
            title = drift.targetNodeName + " 的已记录现实可能发生变化",
            summary = driftSummary(drift.kind, drift.candidateRelation, drift.capability),
            evidenceSummary = observationSummary(drift.observationCount),
            decisions = listOf("已替换", "两个都在用", "没有变化", "稍后确认"),
        )
    },
)

private fun observationSummary(count: Int): String =
    if (count <= 0) "存在待复核证据；观察次数未记录"
    else "已记录 " + count + " 次相关观察"

private fun relationReviewSummary(relation: String, capability: String): String {
    val relationLabel = when (relation) {
        "funding_source", "FUNDED_BY" -> "付款来源关系"
        "merchant_agreement", "CARD_ON_FILE", "BILLED_TO" -> "支付绑定关系"
        "AUTHENTICATES_WITH", "authenticates" -> "登录验证关系"
        "RECOVERS_VIA", "recovers" -> "恢复关系"
        "OTP_DELIVERED_TO", "twoFA" -> "验证码 / 双重验证关系"
        else -> "基础设施关系"
    }
    val capabilityLabel = when (capability) {
        "payment" -> "支付"
        "access" -> "访问"
        "authentication" -> "验证"
        "recovery" -> "恢复"
        "communication" -> "通信"
        else -> "能力未明确"
    }
    return "系统建议确认一条" + relationLabel + "；影响能力：" + capabilityLabel +
        "。确认前不会进入依赖图。"
}

private fun candidateKindSummary(kind: String): String = when (kind) {
    "service" -> "系统发现了一个可能的新服务对象；确认前不会进入你的基础设施。"
    "account" -> "系统发现了一个可能的新账户对象；确认前不会进入你的基础设施。"
    "payment_instrument" -> "系统发现了一个可能的支付工具；确认前不会进入你的基础设施。"
    "identity_anchor" -> "系统发现了一个可能的身份对象；具体类型仍需确认。"
    else -> "系统发现了一个可能的新基础设施对象；确认前不会进入 Reality。"
}

private fun driftSummary(kind: String, relation: String, capability: String): String {
    val changeLabel = when (kind) {
        "replacement" -> "新的正向证据可能表示原关系已经被替换。"
        "additional_path" -> "新的正向证据可能表示出现了额外路径。"
        else -> "新的正向证据与当前已确认 Reality 不一致。"
    }
    val context = relationReviewSummary(relation, capability)
        .removeSuffix("确认前不会进入依赖图。")
        .trim()
    return changeLabel + " " + context + "只有明确确认后才修改 Reality。"
}
