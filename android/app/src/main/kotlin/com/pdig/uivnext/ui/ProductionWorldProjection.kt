package com.pdig.uivnext.ui

import com.pdig.uivnext.model.RegionPresentation
import com.pdig.uivnext.production.ProductionConsumerInventory
import com.pdig.uivnext.production.VNextProductionSnapshot

/**
 * Presentation projection for governed Production geography.
 *
 * Only objects with Region Lens status=selected participate. A same-priority
 * RegionFact conflict never receives a silent map position.
 */
internal data class ProductionWorldProjection(
    val regions: List<RegionPresentation>,
    val arcingPairs: List<Pair<String, String>>,
    val unplottableTerritoryCodes: List<String>,
    val needsReviewObjectCount: Int,
    val unknownObjectCount: Int,
)

internal fun buildProductionWorldProjection(
    snapshot: VNextProductionSnapshot,
    inventory: ProductionConsumerInventory,
): ProductionWorldProjection {
    val plottable = inventory.regions.mapNotNull { region ->
        val anchor = TerritoryPresentationCatalog.anchor(region.territoryCode)
            ?: return@mapNotNull null
        RegionPresentation(
            regionCode = region.territoryCode,
            displayName = anchor.displayNameZh,
            latitude = anchor.latitude,
            longitude = anchor.longitude,
            cardCount = region.paymentAssetCount,
            phoneCount = region.phoneIdentityCount,
            accountCount = region.accountCount,
            serviceCount = region.serviceCount,
            otherCount = region.otherObjectCount,
            // Production RegionFact does not itself prove regional attention state.
            attentionCount = 0,
        )
    }

    val plottableCodes = plottable.map { it.regionCode }.toSet()
    val objectTerritory = snapshot.objects.mapNotNull { item ->
        val code = item.regionLens.territoryCode
        if (item.regionLens.status == "selected" && code != null) item.id to code else null
    }.toMap()

    // A visual arc is emitted only when BOTH endpoints have unambiguous confirmed
    // Region Lens selection and the underlying dependency is confirmed Reality.
    val arcs = snapshot.confirmedDependencies.mapNotNull { dependency ->
        val from = objectTerritory[dependency.fromId] ?: return@mapNotNull null
        val to = objectTerritory[dependency.toId] ?: return@mapNotNull null
        if (from == to || from !in plottableCodes || to !in plottableCodes) {
            null
        } else {
            from to to
        }
    }.distinct()

    return ProductionWorldProjection(
        regions = plottable,
        arcingPairs = arcs,
        unplottableTerritoryCodes = inventory.regions
            .map { it.territoryCode }
            .filterNot { it in plottableCodes }
            .sorted(),
        needsReviewObjectCount = inventory.regionNeedsReviewObjectCount,
        unknownObjectCount = inventory.regionUnknownObjectCount,
    )
}
