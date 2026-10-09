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

fun accountImpactLens(accountId: String): UiImpactLens {
    val account = UiVNextDemoFixture.accounts.firstOrNull { it.id == accountId }
    return UiImpactLens(
        confirmedDependencies = 0,
        attentionFindings = if (account?.attention == true) 1 else 0,
        criticalAccounts = null,
        uniqueRecoveryPath = UiImpactTruth.UNKNOWN,
        independentAlternatives = null,
        unknownRelationsRemain = true,
    )
}

fun emailImpactLens(emailId: String): UiImpactLens {
    val email = UiVNextDemoFixture.emails.firstOrNull { it.id == emailId }
    return UiImpactLens(
        confirmedDependencies = email?.linkedServiceCount ?: 0,
        attentionFindings = if (email?.uniqueRecoveryPath == true) 1 else 0,
        criticalAccounts = null,
        uniqueRecoveryPath = if (email?.uniqueRecoveryPath == true) {
            UiImpactTruth.CONFIRMED
        } else {
            UiImpactTruth.UNKNOWN
        },
        independentAlternatives = null,
        unknownRelationsRemain = true,
    )
}

fun deviceImpactLens(deviceId: String): UiImpactLens {
    val device = UiVNextDemoFixture.devices.firstOrNull { it.id == deviceId }
    return UiImpactLens(
        confirmedDependencies = 0,
        attentionFindings = if (device?.attention == true) 1 else 0,
        criticalAccounts = null,
        uniqueRecoveryPath = UiImpactTruth.UNKNOWN,
        independentAlternatives = null,
        unknownRelationsRemain = true,
    )
}

fun serviceImpactLens(serviceId: String): UiImpactLens {
    val dependencyCount = UiVNextDemoFixture.relations.count { it.to == serviceId }
    return UiImpactLens(
        confirmedDependencies = dependencyCount,
        attentionFindings = 0,
        criticalAccounts = null,
        uniqueRecoveryPath = UiImpactTruth.UNKNOWN,
        independentAlternatives = null,
        unknownRelationsRemain = true,
    )
}

