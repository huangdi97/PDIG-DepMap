package com.pdig.uivnext.production

/**
 * Exact production decision grammar for the R22 Human Review surface.
 *
 * This coordinator contains no local optimistic state. Every mutating decision is
 * delegated to the authoritative gateway, whose return value is the re-read queue.
 */
internal enum class ProductionReviewDecision {
    CONFIRM_RELATION,
    REJECT_RELATION,
    CONFIRM_OBJECT,
    IGNORE_OBJECT,
    REPLACED,
    ADDITIONAL_PATH,
    NO_CHANGE,
    LATER,
}

internal class ProductionReviewCoordinator(
    private val source: VNextReviewSource,
    private val gateway: VNextReviewActionGateway,
) {
    fun load(): ProductionReviewConsumerInbox =
        buildProductionReviewConsumerInbox(source.reviewQueue())

    fun decide(
        kind: ProductionReviewConsumerKind,
        itemId: String,
        decision: ProductionReviewDecision,
    ): ProductionReviewConsumerInbox {
        val queue = when (kind) {
            ProductionReviewConsumerKind.RELATION_PROPOSAL -> when (decision) {
                ProductionReviewDecision.CONFIRM_RELATION -> gateway.acceptProposal(itemId)
                ProductionReviewDecision.REJECT_RELATION -> gateway.rejectProposal(itemId)
                else -> invalid(kind, decision)
            }

            ProductionReviewConsumerKind.OBJECT_CANDIDATE -> when (decision) {
                ProductionReviewDecision.CONFIRM_OBJECT -> gateway.acceptCandidate(itemId)
                ProductionReviewDecision.IGNORE_OBJECT -> gateway.dismissCandidate(itemId)
                else -> invalid(kind, decision)
            }

            ProductionReviewConsumerKind.REALITY_DRIFT -> when (decision) {
                ProductionReviewDecision.REPLACED ->
                    gateway.resolveDriftAsReplacement(itemId)
                ProductionReviewDecision.ADDITIONAL_PATH ->
                    gateway.resolveDriftAsAdditionalPath(itemId)
                ProductionReviewDecision.NO_CHANGE ->
                    gateway.dismissDrift(itemId)
                ProductionReviewDecision.LATER ->
                    source.reviewQueue()
                else -> invalid(kind, decision)
            }
        }
        return buildProductionReviewConsumerInbox(queue)
    }

    private fun invalid(
        kind: ProductionReviewConsumerKind,
        decision: ProductionReviewDecision,
    ): Nothing = error("Invalid Human Review decision: $kind / $decision")
}
