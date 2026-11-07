package com.pdig.uivnext.ui

import com.pdig.uivnext.production.VNextPendingReviewSummary
import com.pdig.uivnext.production.VNextProductionDependency
import com.pdig.uivnext.production.VNextProductionObject
import com.pdig.uivnext.production.VNextProductionSnapshot
import com.pdig.uivnext.production.VNextProductionSurfaceKind
import com.pdig.uivnext.production.VNextSourceCoverageSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductionRelationshipConstellationTest {
    private fun obj(id: String) = VNextProductionObject(
        id = id, kind = "service", name = id,
        surfaceKind = VNextProductionSurfaceKind.SERVICE,
    )

    private fun dep(id: String, from: String, to: String) = VNextProductionDependency(
        id = id, fromId = from, fromName = from, relation = "controls",
        toId = to, toName = to, capability = "access", criticality = "required",
    )

    private fun snapshot() = VNextProductionSnapshot(
        revision = 10,
        objects = listOf(obj("center"), obj("account"), obj("service"), obj("other")),
        confirmedDependencies = listOf(
            dep("c-a", "center", "account"),
            dep("a-c", "account", "center"),
            dep("c-s", "center", "service"),
            dep("unrelated", "account", "other"),
            dep("unknown", "center", "unavailable"),
        ),
        timeline = emptyList(), plans = emptyList(),
        pendingReview = VNextPendingReviewSummary(0, 0, 0),
        sourceCoverage = VNextSourceCoverageSummary(0, 0),
    )

    @Test
    fun groupsActualKnownPeersAndNeverInventsUnrelatedOrMissingNodes() {
        val peers = productionOrbitPeers(snapshot(), "center")
        assertEquals(listOf("account", "service"), peers.map { it.objectId })
        assertEquals(2, peers.first().relationCount)
        assertEquals("controls", peers.first().relation)
        assertTrue(peers.none { it.objectId == "other" || it.objectId == "unavailable" })
        assertEquals(emptyList<ProductionOrbitPeer>(), productionOrbitPeers(snapshot(), "other")
            .filter { it.objectId == "center" })
    }

    @Test
    fun noRecordedDependencyProducesNoDecorativeEdges() {
        assertTrue(productionOrbitPeers(snapshot().copy(confirmedDependencies = emptyList()), "center").isEmpty())
    }
}
