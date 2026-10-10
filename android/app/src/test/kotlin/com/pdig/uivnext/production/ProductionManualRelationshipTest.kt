package com.pdig.uivnext.production

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductionManualRelationshipTest {
    @Test
    fun supportedDefinitionsAreExactlyCurrentCanonicalRuntimeRelations() {
        val definitions = supportedManualRelationshipDefinitions()
        val relations = definitions.map { it.relation }.toSet()

        assertEquals(
            setOf(
                "funding_source",
                "merchant_agreement",
                "recovers",
                "authenticates",
                "controls",
            ),
            relations,
        )
        assertFalse("verifies is storage-only/future and must not be executable",
            "verifies" in relations)
        assertFalse("bound_to is storage-only/future and must not be executable",
            "bound_to" in relations)

        assertEquals(
            "recovery",
            definitions.single { it.relation == "recovers" }.capability,
        )
        assertEquals(
            "authentication",
            definitions.single { it.relation == "authenticates" }.capability,
        )
        assertEquals(
            "access",
            definitions.single { it.relation == "controls" }.capability,
        )
    }

    @Test
    fun registryCarriesEndpointKindConstraintsIntoConsumerGateway() {
        val funding = supportedManualRelationshipDefinitions()
            .single { it.relation == "funding_source" }

        assertTrue("payment_instrument" in funding.allowedFromKinds)
        assertTrue("account" in funding.allowedFromKinds)
        assertTrue("payment_instrument" in funding.allowedToKinds)
        assertTrue("account" in funding.allowedToKinds)
        assertFalse("identity_anchor" in funding.allowedFromKinds)
        assertFalse("service" in funding.allowedToKinds)
    }
}
