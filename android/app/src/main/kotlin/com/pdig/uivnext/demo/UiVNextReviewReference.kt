package com.pdig.uivnext.demo

/**
 * Synthetic Preview-only Human Review references.
 *
 * These items deliberately represent NOT-YET-CONFIRMED information. They must not
 * be consumed by dependency/impact logic or written to Personal Reality.
 */
internal enum class UiReviewReferenceKind {
    DEPENDENCY_PROPOSAL,
    DISCOVERY_CANDIDATE,
    REALITY_DRIFT,
}

internal data class UiReviewReferenceItem(
    val id: String,
    val kind: UiReviewReferenceKind,
    val title: String,
    val summary: String,
    val evidence: String,
    val consequence: String,
    val confidenceLabel: String? = null,
)

internal val UI_REVIEW_REFERENCE_ITEMS: List<UiReviewReferenceItem> = listOf(
    UiReviewReferenceItem(
        id = "review-proposal-notion-revolut",
        kind = UiReviewReferenceKind.DEPENDENCY_PROPOSAL,
        title = "Notion 可能由 Revolut 多币种付款",
        summary = "这是关系提案，不是已确认绑定；确认前不会进入依赖图或 Impact。",
        evidence = "演示证据：账单描述与服务名称匹配",
        consequence = "确认后才可成为已记录付款关系；拒绝不会删除任何现有关系。",
        confidenceLabel = "机器建议",
    ),
    UiReviewReferenceItem(
        id = "review-candidate-adobe",
        kind = UiReviewReferenceKind.DISCOVERY_CANDIDATE,
        title = "可能发现新的服务：Adobe Creative Cloud",
        summary = "这是对象候选，不代表你当前仍在使用该服务。",
        evidence = "演示证据：收据元数据出现服务名称",
        consequence = "确认后才创建逻辑对象；忽略候选不会改变现有基础设施。",
        confidenceLabel = "待确认对象",
    ),
    UiReviewReferenceItem(
        id = "review-drift-netflix",
        kind = UiReviewReferenceKind.REALITY_DRIFT,
        title = "Netflix 的付款来源可能已经变化",
        summary = "新的正向演示证据与当前已记录付款来源不一致。",
        evidence = "演示证据：新的付款记录指向另一张已记录卡",
        consequence = "只有明确选择“已替换”或“两个都在用”后，Reality 才能改变。",
        confidenceLabel = "现实漂移",
    ),
)

internal data class UiReviewReferenceSummary(
    val proposals: Int,
    val candidates: Int,
    val drifts: Int,
) {
    val total: Int get() = proposals + candidates + drifts
}

internal fun reviewReferenceSummary(
    items: List<UiReviewReferenceItem> = UI_REVIEW_REFERENCE_ITEMS,
): UiReviewReferenceSummary = UiReviewReferenceSummary(
    proposals = items.count { it.kind == UiReviewReferenceKind.DEPENDENCY_PROPOSAL },
    candidates = items.count { it.kind == UiReviewReferenceKind.DISCOVERY_CANDIDATE },
    drifts = items.count { it.kind == UiReviewReferenceKind.REALITY_DRIFT },
)
