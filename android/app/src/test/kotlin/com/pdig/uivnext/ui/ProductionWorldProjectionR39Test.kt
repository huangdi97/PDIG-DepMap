package com.pdig.uivnext.ui

import com.pdig.uivnext.model.VGlobeState
import com.pdig.uivnext.production.VNextPendingReviewSummary
import com.pdig.uivnext.production.VNextProductionDependency
import com.pdig.uivnext.production.VNextProductionObject
import com.pdig.uivnext.production.VNextProductionRegionLens
import com.pdig.uivnext.production.VNextProductionSnapshot
import com.pdig.uivnext.production.VNextSourceCoverageSummary
import com.pdig.uivnext.production.VNextProductionSurfaceKind
import com.pdig.uivnext.production.buildProductionConsumerInventory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductionWorldProjectionR39Test {
    @Test
    fun confirmedSelectedRegionsAndConfirmedDependencyCreateRealArc() {
        val snapshot = snapshot(
            objects = listOf(
                objectAt(
                    id = "card-cn",
                    kind = VNextProductionSurfaceKind.PAYMENT_ASSET,
                    territory = "CN",
                ),
                objectAt(
                    id = "service-gb",
                    kind = VNextProductionSurfaceKind.SERVICE,
                    territory = "GB",
                ),
            ),
            dependencies = listOf(
                VNextProductionDependency(
                    id = "dep-1",
                    fromId = "card-cn",
                    fromName = "卡",
                    relation = "merchant_agreement",
                    toId = "service-gb",
                    toName = "服务",
                    capability = "payment",
                    criticality = "required",
                ),
            ),
        )

        val inventory = buildProductionConsumerInventory(snapshot)
        val world = buildProductionWorldProjection(snapshot, inventory)

        assertEquals(listOf("CN", "GB"), inventory.regions.map { it.territoryCode }.sorted())
        assertEquals(2, world.regions.size)
        assertEquals(listOf("CN" to "GB"), world.arcingPairs)
        assertEquals(0, world.needsReviewObjectCount)
    }

    @Test
    fun needsReviewRegionNeverGetsSilentMapPosition() {
        val item = VNextProductionObject(
            id = "ambiguous",
            kind = "service",
            name = "Ambiguous",
            surfaceKind = VNextProductionSurfaceKind.SERVICE,
            regionLens = VNextProductionRegionLens(
                status = "needs_review",
                territoryCode = null,
                facet = "service_market",
            ),
        )
        val snapshot = snapshot(listOf(item))
        val inventory = buildProductionConsumerInventory(snapshot)
        val world = buildProductionWorldProjection(snapshot, inventory)

        assertTrue(inventory.regions.isEmpty())
        assertTrue(world.regions.isEmpty())
        assertEquals(1, world.needsReviewObjectCount)
    }

    @Test
    fun confirmedButUnanchoredTerritoryStaysRealAndTextOnly() {
        val snapshot = snapshot(
            listOf(
                objectAt(
                    id = "service-de",
                    kind = VNextProductionSurfaceKind.SERVICE,
                    territory = "DE",
                ),
            ),
        )
        val inventory = buildProductionConsumerInventory(snapshot)
        val world = buildProductionWorldProjection(snapshot, inventory)

        assertEquals(listOf("DE"), inventory.regions.map { it.territoryCode })
        assertTrue(world.regions.isEmpty())
        assertEquals(listOf("DE"), world.unplottableTerritoryCodes)
    }

    @Test
    fun unknownRegionDoesNotCreateFakeTerritoryOrArc() {
        val unknown = VNextProductionObject(
            id = "unknown",
            kind = "payment_instrument",
            name = "Unknown",
            surfaceKind = VNextProductionSurfaceKind.PAYMENT_ASSET,
        )
        val snapshot = snapshot(listOf(unknown))
        val inventory = buildProductionConsumerInventory(snapshot)
        val world = buildProductionWorldProjection(snapshot, inventory)

        assertTrue(inventory.regions.isEmpty())
        assertTrue(world.regions.isEmpty())
        assertTrue(world.arcingPairs.isEmpty())
        assertEquals(1, world.unknownObjectCount)
    }

    @Test
    fun productionGlobeClickSelectsSameRegionLensAsInventoryAndCanReset() {
        val snapshot = snapshot(
            listOf(objectAt("card-hk", VNextProductionSurfaceKind.PAYMENT_ASSET, "HK")),
        )
        val region = buildProductionWorldProjection(
            snapshot,
            buildProductionConsumerInventory(snapshot),
        ).regions.single()
        val app = VAppState()

        selectProductionWorldRegion(app, region)
        assertEquals("HK", app.regionFilter)
        assertEquals("HK", app.globe.selectedRegion)
        assertEquals(VGlobeState.REGION_SELECTED, app.globe.state)

        app.clearRegion()
        assertNull(app.regionFilter)
        assertNull(app.globe.selectedRegion)
        assertEquals(VGlobeState.GLOBAL, app.globe.state)
        // The interaction acts only on presentation state, not the source snapshot.
        assertEquals("HK", snapshot.objects.single().regionLens.territoryCode)
    }

    private fun objectAt(
        id: String,
        kind: VNextProductionSurfaceKind,
        territory: String,
    ) = VNextProductionObject(
        id = id,
        kind = when (kind) {
            VNextProductionSurfaceKind.PAYMENT_ASSET -> "payment_instrument"
            VNextProductionSurfaceKind.SERVICE -> "service"
            else -> "custom"
        },
        name = id,
        surfaceKind = kind,
        regionLens = VNextProductionRegionLens(
            status = "selected",
            territoryCode = territory,
            facet = "service_market",
        ),
    )

    private fun snapshot(
        objects: List<VNextProductionObject>,
        dependencies: List<VNextProductionDependency> = emptyList(),
    ) = VNextProductionSnapshot(
        revision = 1,
        objects = objects,
        confirmedDependencies = dependencies,
        timeline = emptyList(),
        plans = emptyList(),
        pendingReview = VNextPendingReviewSummary(0, 0, 0),
        sourceCoverage = VNextSourceCoverageSummary(0, 0),
    )
}
