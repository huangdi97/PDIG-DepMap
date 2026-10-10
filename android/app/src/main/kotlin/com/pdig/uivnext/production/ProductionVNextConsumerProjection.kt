package com.pdig.uivnext.production

/**
 * Consumer-safe projection over the production VNext snapshot.
 *
 * This deliberately does NOT coerce production Reality into the richer synthetic
 * reference models. Missing subtype/lifecycle/region facts stay missing.
 */
internal data class ProductionPaymentAssetView(
    val id: String,
    val name: String,
    val issuer: String?,
    val last4: String?,
    val confirmedDependencyCount: Int,
)

internal data class ProductionGenericIdentityView(
    val id: String,
    val name: String,
    val confirmedDependencyCount: Int,
)

internal data class ProductionSourceView(
    val id: String,
    val label: String,
    val state: String,
    val lastIngestedAt: String?,
)

internal data class ProductionInventoryCounts(
    val paymentAssets: Int,
    val genericIdentityAnchors: Int,
    val accounts: Int,
    val services: Int,
    val devices: Int,
    val memberships: Int,
    val customObjects: Int,
)

internal data class ProductionConsumerInventory(
    val revision: Int,
    val counts: ProductionInventoryCounts,
    val paymentAssets: List<ProductionPaymentAssetView>,
    val genericIdentityAnchors: List<ProductionGenericIdentityView>,
    val pendingReviewCount: Int,
    val activeSourceCount: Int,
    val sources: List<ProductionSourceView>,
)

/**
 * Builds only what existing production truth can prove.
 *
 * Important asymmetry:
 * - payment_instrument is a Canonical type, so it may use the financial-asset surface;
 * - identity_anchor is too coarse to mean "phone number", so it remains generic.
 */
internal fun buildProductionConsumerInventory(
    snapshot: VNextProductionSnapshot,
): ProductionConsumerInventory {
    val incomingOrOutgoing = snapshot.confirmedDependencies
        .flatMap { listOf(it.fromId, it.toId) }
        .groupingBy { it }
        .eachCount()

    val paymentAssets = snapshot.objects
        .filter { it.surfaceKind == VNextProductionSurfaceKind.PAYMENT_ASSET }
        .map {
            ProductionPaymentAssetView(
                id = it.id,
                name = it.name,
                issuer = it.issuer,
                last4 = it.last4,
                confirmedDependencyCount = incomingOrOutgoing[it.id] ?: 0,
            )
        }

    val genericIdentities = snapshot.objects
        .filter { it.surfaceKind == VNextProductionSurfaceKind.IDENTITY_ANCHOR_GENERIC }
        .map {
            ProductionGenericIdentityView(
                id = it.id,
                name = it.name,
                confirmedDependencyCount = incomingOrOutgoing[it.id] ?: 0,
            )
        }

    fun count(kind: VNextProductionSurfaceKind): Int =
        snapshot.objects.count { it.surfaceKind == kind }

    return ProductionConsumerInventory(
        revision = snapshot.revision,
        counts = ProductionInventoryCounts(
            paymentAssets = paymentAssets.size,
            genericIdentityAnchors = genericIdentities.size,
            accounts = count(VNextProductionSurfaceKind.ACCOUNT),
            services = count(VNextProductionSurfaceKind.SERVICE),
            devices = count(VNextProductionSurfaceKind.DEVICE),
            memberships = count(VNextProductionSurfaceKind.MEMBERSHIP),
            customObjects = count(VNextProductionSurfaceKind.CUSTOM_GENERIC),
        ),
        paymentAssets = paymentAssets,
        genericIdentityAnchors = genericIdentities,
        pendingReviewCount = snapshot.pendingReview.proposalCount +
            snapshot.pendingReview.candidateCount +
            snapshot.pendingReview.driftCount,
        activeSourceCount = snapshot.sourceCoverage.activeSourceCount,
        sources = snapshot.sources.map {
            ProductionSourceView(
                id = it.id,
                label = it.label,
                state = it.state,
                lastIngestedAt = it.lastIngestedAt,
            )
        },
    )
}
