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
    val annualFeeAmount: String? = null,
    val annualFeeCurrency: String? = null,
    val billingDay: Int? = null,
    val paymentDueDay: Int? = null,
    val autopayMode: String? = null,
    val annualFeeSchedule: VNextProductionMaintenanceSchedule? = null,
)

internal data class ProductionIdentityView(
    val id: String,
    val name: String,
    val subtype: String,
    val verificationBasisType: String?,
    val confirmedAt: String?,
    val evidenceRefCount: Int,
    val confirmedDependencyCount: Int,
    val billingMode: String? = null,
    val planCost: String? = null,
    val planCurrency: String? = null,
    val renewalMethod: String? = null,
    val keepAliveSchedule: VNextProductionMaintenanceSchedule? = null,
    val planRenewalSchedule: VNextProductionMaintenanceSchedule? = null,
)

internal data class ProductionGenericIdentityView(
    val id: String,
    val name: String,
    val confirmedDependencyCount: Int,
)

internal data class ProductionRegionView(
    val territoryCode: String,
    val objectCount: Int,
    val paymentAssetCount: Int,
    val phoneIdentityCount: Int,
    val accountCount: Int,
    val serviceCount: Int,
    val emailIdentityCount: Int,
    val deviceCount: Int,
    val membershipCount: Int,
    val genericIdentityCount: Int,
    val customObjectCount: Int,
    val otherObjectCount: Int,
    val memberObjectIds: List<String>,
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
    val phoneIdentities: Int = 0,
    val emailIdentities: Int = 0,
)

internal data class ProductionConsumerInventory(
    val revision: Int,
    val counts: ProductionInventoryCounts,
    val paymentAssets: List<ProductionPaymentAssetView>,
    val genericIdentityAnchors: List<ProductionGenericIdentityView>,
    val pendingReviewCount: Int,
    val activeSourceCount: Int,
    val sources: List<ProductionSourceView>,
    val regions: List<ProductionRegionView> = emptyList(),
    val regionNeedsReviewObjectCount: Int = 0,
    val regionUnknownObjectCount: Int = 0,
    val phoneIdentities: List<ProductionIdentityView> = emptyList(),
    val emailIdentities: List<ProductionIdentityView> = emptyList(),
)

/**
 * Builds only what existing production truth can prove.
 *
 * Important asymmetry:
 * - payment_instrument is a Canonical type, so it may use the financial-asset surface;
 * - identity_anchor stays generic unless the governed identity_anchor_profile is valid;
 * - a confirmed governed PHONE_NUMBER / EMAIL_ADDRESS profile may bind its typed surface;
 * - names, regexes, edges, locale and provider-looking text never classify subtype.
 */
internal fun buildProductionConsumerInventory(
    snapshot: VNextProductionSnapshot,
): ProductionConsumerInventory {
    val incomingOrOutgoing = snapshot.confirmedDependencies
        .flatMap { listOf(it.fromId, it.toId) }
        .groupingBy { it }
        .eachCount()

    fun VNextProductionObject.fact(kind: String): String? =
        maintenanceFacts.firstOrNull { it.kind == kind && it.state == "confirmed" }?.value

    fun VNextProductionObject.schedule(kind: String): VNextProductionMaintenanceSchedule? =
        maintenanceSchedules.firstOrNull {
            it.kind == kind && (it.state == "active" || it.state == "needs_review")
        }

    val paymentAssets = snapshot.objects
        .filter { it.surfaceKind == VNextProductionSurfaceKind.PAYMENT_ASSET }
        .map {
            ProductionPaymentAssetView(
                id = it.id,
                name = it.name,
                issuer = it.issuer,
                last4 = it.last4,
                confirmedDependencyCount = incomingOrOutgoing[it.id] ?: 0,
                annualFeeAmount = it.fact("card_annual_fee_amount"),
                annualFeeCurrency = it.fact("card_annual_fee_currency"),
                billingDay = it.fact("card_billing_day")?.toIntOrNull(),
                paymentDueDay = it.fact("card_payment_due_day")?.toIntOrNull(),
                autopayMode = it.fact("card_autopay_mode"),
                annualFeeSchedule = it.schedule("card_annual_fee_checkpoint"),
            )
        }

    fun typedIdentities(kind: VNextProductionSurfaceKind): List<ProductionIdentityView> =
        snapshot.objects
            .filter { it.surfaceKind == kind }
            .map {
                ProductionIdentityView(
                    id = it.id,
                    name = it.name,
                    subtype = it.identitySubtype ?: "unknown",
                    verificationBasisType = it.identityVerificationBasisType,
                    confirmedAt = it.identityConfirmedAt,
                    evidenceRefCount = it.identityEvidenceRefs.size,
                    confirmedDependencyCount = incomingOrOutgoing[it.id] ?: 0,
                    billingMode = if (kind == VNextProductionSurfaceKind.PHONE_IDENTITY)
                        it.fact("number_billing_mode") else null,
                    planCost = if (kind == VNextProductionSurfaceKind.PHONE_IDENTITY)
                        it.fact("number_plan_cost") else null,
                    planCurrency = if (kind == VNextProductionSurfaceKind.PHONE_IDENTITY)
                        it.fact("number_plan_currency") else null,
                    renewalMethod = if (kind == VNextProductionSurfaceKind.PHONE_IDENTITY)
                        it.fact("number_renewal_method") else null,
                    keepAliveSchedule = if (kind == VNextProductionSurfaceKind.PHONE_IDENTITY)
                        it.schedule("number_keep_alive") else null,
                    planRenewalSchedule = if (kind == VNextProductionSurfaceKind.PHONE_IDENTITY)
                        it.schedule("number_plan_renewal") else null,
                )
            }

    val phoneIdentities = typedIdentities(VNextProductionSurfaceKind.PHONE_IDENTITY)
    val emailIdentities = typedIdentities(VNextProductionSurfaceKind.EMAIL_IDENTITY)

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

    val regionMembers = snapshot.objects
        .filter { it.regionLens.status == "selected" && it.regionLens.territoryCode != null }
        .groupBy { requireNotNull(it.regionLens.territoryCode) }

    val regions = regionMembers.entries
        .sortedBy { it.key }
        .map { (territoryCode, members) ->
            fun memberCount(kind: VNextProductionSurfaceKind): Int =
                members.count { it.surfaceKind == kind }
            val paymentAssetCount = memberCount(VNextProductionSurfaceKind.PAYMENT_ASSET)
            val phoneIdentityCount = memberCount(VNextProductionSurfaceKind.PHONE_IDENTITY)
            val emailIdentityCount = memberCount(VNextProductionSurfaceKind.EMAIL_IDENTITY)
            val accountCount = memberCount(VNextProductionSurfaceKind.ACCOUNT)
            val serviceCount = memberCount(VNextProductionSurfaceKind.SERVICE)
            val deviceCount = memberCount(VNextProductionSurfaceKind.DEVICE)
            val membershipCount = memberCount(VNextProductionSurfaceKind.MEMBERSHIP)
            val genericIdentityCount =
                memberCount(VNextProductionSurfaceKind.IDENTITY_ANCHOR_GENERIC)
            val customObjectCount = memberCount(VNextProductionSurfaceKind.CUSTOM_GENERIC)
            val primaryVisualCount =
                paymentAssetCount + phoneIdentityCount + accountCount + serviceCount
            ProductionRegionView(
                territoryCode = territoryCode,
                objectCount = members.size,
                paymentAssetCount = paymentAssetCount,
                phoneIdentityCount = phoneIdentityCount,
                accountCount = accountCount,
                serviceCount = serviceCount,
                emailIdentityCount = emailIdentityCount,
                deviceCount = deviceCount,
                membershipCount = membershipCount,
                genericIdentityCount = genericIdentityCount,
                customObjectCount = customObjectCount,
                otherObjectCount = members.size - primaryVisualCount,
                memberObjectIds = members.map { it.id }.sorted(),
            )
        }

    return ProductionConsumerInventory(
        revision = snapshot.revision,
        counts = ProductionInventoryCounts(
            paymentAssets = paymentAssets.size,
            phoneIdentities = phoneIdentities.size,
            emailIdentities = emailIdentities.size,
            genericIdentityAnchors = genericIdentities.size,
            accounts = count(VNextProductionSurfaceKind.ACCOUNT),
            services = count(VNextProductionSurfaceKind.SERVICE),
            devices = count(VNextProductionSurfaceKind.DEVICE),
            memberships = count(VNextProductionSurfaceKind.MEMBERSHIP),
            customObjects = count(VNextProductionSurfaceKind.CUSTOM_GENERIC),
        ),
        paymentAssets = paymentAssets,
        phoneIdentities = phoneIdentities,
        emailIdentities = emailIdentities,
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
        regions = regions,
        regionNeedsReviewObjectCount =
            snapshot.objects.count { it.regionLens.status == "needs_review" },
        regionUnknownObjectCount =
            snapshot.objects.count { it.regionLens.status == "unknown" },
    )
}
