package com.pdig.core.domain

import com.pdig.core.generated.RegionFacet
import com.pdig.core.generated.RegionFactState
import com.pdig.core.generated.VerificationBasisType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RegionFactsTest {
    @Test
    fun confirmedFactsDecodeAndDefaultLensUsesGovernedPrecedence() {
        val fields = """
            {
              "region_facts": {
                "version": 1,
                "items": [
                  {
                    "id": "rf-service",
                    "facet": "service_market",
                    "territory_code": "GB",
                    "state": "confirmed",
                    "verification_basis_type": "user_confirmed",
                    "confirmed_at": "2026-10-10T00:00:00Z"
                  },
                  {
                    "id": "rf-number",
                    "facet": "numbering_territory",
                    "territory_code": "HK",
                    "subdivision_code": "HK-HK",
                    "state": "confirmed",
                    "verification_basis_type": "authoritative_source",
                    "confirmed_at": "2026-10-10T01:00:00Z",
                    "evidence_refs": ["ev-region"]
                  }
                ]
              }
            }
        """.trimIndent()

        val facts = governedRegionFacts(fields)
        assertEquals(2, facts.size)
        assertEquals(RegionFacet.SERVICE_MARKET, facts[0].facet)
        assertEquals(RegionFacet.NUMBERING_TERRITORY, facts[1].facet)
        assertEquals("HK-HK", facts[1].subdivisionCode)
        assertEquals(VerificationBasisType.AUTHORITATIVE_SOURCE, facts[1].verificationBasisType)

        val lens = defaultRegionLensSelection(fields)
        assertEquals(RegionLensSelectionStatus.SELECTED, lens.status)
        assertEquals("HK", lens.territoryCode)
        assertEquals(RegionFacet.NUMBERING_TERRITORY, lens.facet)
    }

    @Test
    fun samePriorityConflictNeedsReviewInsteadOfPickingOne() {
        val fields = """
            {"region_facts":{"version":1,"items":[
              {"id":"a","facet":"service_market","territory_code":"GB","state":"confirmed","verification_basis_type":"user_confirmed","confirmed_at":"t"},
              {"id":"b","facet":"service_market","territory_code":"US","state":"confirmed","verification_basis_type":"user_confirmed","confirmed_at":"t"}
            ]}}
        """.trimIndent()

        val lens = defaultRegionLensSelection(fields)
        assertEquals(RegionLensSelectionStatus.NEEDS_REVIEW, lens.status)
        assertEquals(RegionFacet.SERVICE_MARKET, lens.facet)
        assertEquals(null, lens.territoryCode)
    }

    @Test
    fun retiredProposalUnknownAndMalformedItemsFailClosed() {
        val fields = """
            {"region_facts":{"version":1,"items":[
              {"id":"retired","facet":"issuance_jurisdiction","territory_code":"CN","state":"retired","verification_basis_type":"user_confirmed","confirmed_at":"t","retired_at":"t2"},
              {"id":"proposal","facet":"service_market","territory_code":"GB","state":"proposal","verification_basis_type":"user_confirmed","confirmed_at":"t"},
              {"id":"lower","facet":"service_market","territory_code":"hk","state":"confirmed","verification_basis_type":"user_confirmed","confirmed_at":"t"},
              {"id":"future","facet":"future_region","territory_code":"US","state":"confirmed","verification_basis_type":"user_confirmed","confirmed_at":"t"},
              {"id":"valid","facet":"provider_jurisdiction","territory_code":"SG","state":"confirmed","verification_basis_type":"user_confirmed","confirmed_at":"t"}
            ]}}
        """.trimIndent()

        val all = governedRegionFacts(fields)
        assertEquals(2, all.size)
        assertTrue(all.any { it.id == "retired" && it.state == RegionFactState.RETIRED })
        assertTrue(all.any { it.id == "valid" })

        val current = currentConfirmedRegionFacts(fields)
        assertEquals(listOf("valid"), current.map { it.id })
        assertEquals("SG", defaultRegionLensSelection(fields).territoryCode)
    }

    @Test
    fun physicalLocationDoesNotEnterDefaultConsumerLens() {
        val fields = """
            {"region_facts":{"version":1,"items":[
              {"id":"physical","facet":"physical_location","territory_code":"US","state":"confirmed","verification_basis_type":"user_confirmed","confirmed_at":"t"}
            ]}}
        """.trimIndent()

        assertEquals(1, currentConfirmedRegionFacts(fields).size)
        assertEquals(RegionLensSelectionStatus.UNKNOWN, defaultRegionLensSelection(fields).status)
    }

    @Test
    fun oldPayloadAndLookalikeMetadataNeverInferRegion() {
        val fields = """
            {
              "currency":"GBP",
              "issuer":"Hong Kong Bank",
              "phone":"+852 5555 0000",
              "locale":"en-GB",
              "timezone":"Europe/London",
              "subtype":"phone_number"
            }
        """.trimIndent()

        assertTrue(governedRegionFacts(fields).isEmpty())
        assertEquals(RegionLensSelectionStatus.UNKNOWN, defaultRegionLensSelection(fields).status)
    }

    @Test
    fun invalidSubdivisionDropsOnlyThatItem() {
        val fields = """
            {"region_facts":{"version":1,"items":[
              {"id":"bad","facet":"issuance_jurisdiction","territory_code":"CN","subdivision_code":"US-CA","state":"confirmed","verification_basis_type":"user_confirmed","confirmed_at":"t"},
              {"id":"good","facet":"issuance_jurisdiction","territory_code":"CN","subdivision_code":"CN-SX","state":"confirmed","verification_basis_type":"user_confirmed","confirmed_at":"t"}
            ]}}
        """.trimIndent()

        val facts = governedRegionFacts(fields)
        assertEquals(1, facts.size)
        assertEquals("CN-SX", facts.single().subdivisionCode)
    }

    @Test
    fun validityDatesNeverAutoRetireConfirmedReality() {
        val fields = """
            {"region_facts":{"version":1,"items":[
              {"id":"historic-window","facet":"user_confirmed_context","territory_code":"CN","state":"confirmed","verification_basis_type":"user_confirmed","confirmed_at":"2020-01-01T00:00:00Z","valid_until":"2020-02-01T00:00:00Z"}
            ]}}
        """.trimIndent()

        assertEquals(
            listOf("historic-window"),
            currentConfirmedRegionFacts(fields).map { it.id },
        )
    }
}
