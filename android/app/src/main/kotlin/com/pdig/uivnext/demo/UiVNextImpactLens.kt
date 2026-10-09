package com.pdig.uivnext.demo

/**
 * Read-only UI vNext Impact Lens projection.
 *
 * This is deliberately derived only from facts already present in the synthetic
 * reference fixture. It must never invent Canonical relations, independent
 * recovery paths, critical accounts, or a safety verdict.
 */
enum class UiImpactTruth {
    CONFIRMED,
    UNKNOWN,
}

data class UiImpactLens(
    val confirmedDependencies: Int,
    val attentionFindings: Int,
    val criticalAccounts: Int?,
    val uniqueRecoveryPath: UiImpactTruth,
    val independentAlternatives: Int?,
    val unknownRelationsRemain: Boolean,
)

fun cardImpactLens(cardId: String): UiImpactLens {
    val dependencies = UiVNextDemoFixture.servicesForCard(cardId).size
    val findings = UiVNextDemoFixture.attentionItems.count { it.target == cardId }
    return UiImpactLens(
        confirmedDependencies = dependencies,
        attentionFindings = findings,
        criticalAccounts = null,
        uniqueRecoveryPath = UiImpactTruth.UNKNOWN,
        independentAlternatives = null,
        unknownRelationsRemain = true,
    )
}

fun numberImpactLens(numberId: String): UiImpactLens {
    val number = UiVNextDemoFixture.numberById(numberId)
    val dependencies = UiVNextDemoFixture.servicesForNumber(numberId).size
    val findings = UiVNextDemoFixture.attentionItems.count { it.target == numberId }
    return UiImpactLens(
        confirmedDependencies = dependencies,
        attentionFindings = findings,
        criticalAccounts = null,
        // Recovery use and path uniqueness are independent facts.
        // Only explicit uniqueness evidence may produce a positive claim.
        uniqueRecoveryPath = if (number?.uniqueRecoveryPath == true) {
            UiImpactTruth.CONFIRMED
        } else {
            UiImpactTruth.UNKNOWN
        },
        independentAlternatives = null,
        unknownRelationsRemain = true,
    )
}
