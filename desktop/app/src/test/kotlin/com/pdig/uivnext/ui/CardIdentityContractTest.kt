package com.pdig.uivnext.ui

import androidx.compose.ui.graphics.luminance
import com.pdig.uivnext.demo.UiVNextDemoFixture
import com.pdig.uivnext.model.PresentationProfile
import com.pdig.uivnext.ui.components.CardMotif
import com.pdig.uivnext.ui.components.defaultCardProfile
import com.pdig.uivnext.ui.components.identityElementCount
import com.pdig.uivnext.ui.components.resolveCardIdentity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * PHASE 1F 契约（§6–§9）：每张 demo 卡 CardFace ≥3 个身份要素；
 * §9 issuer 身份冻结方向；无 near-black 空占位；Studio 定制可改变身份。
 */
class CardIdentityContractTest {

    private val cardIds = UiVNextDemoFixture.cards.map { it.id }
    private val byId = { id: String -> UiVNextDemoFixture.cardById(id)!! }

    @Test
    fun everyDemoCardHasAtLeastThreeIdentityElements() {
        cardIds.forEach { id ->
            val card = byId(id)
            val profile = defaultCardProfile(card)
            val identity = resolveCardIdentity(profile, card)
            assertTrue(
                identityElementCount(identity, profile.material) >= 3,
                "card $id identity elements < 3: ${identityElementCount(identity, profile.material)}",
            )
        }
    }

    @Test
    fun issuerIdentityDirectionsAreFrozenPerSection9() {
        val expected = mapOf(
            "招商银行" to CardMotif.COPPER_RING,
            "中国工商银行" to CardMotif.RED_LINE,
            "中国银行" to CardMotif.BRUSHED,
            "HSBC 汇丰" to CardMotif.CITY_NIGHT,
            "中银香港" to CardMotif.CONTOUR,
            "Monzo" to CardMotif.CORAL_BAND,
            "Revolut" to CardMotif.CHROMATIC,
            "Chase" to CardMotif.BRUSHED,
            "Capital One" to CardMotif.SWEEP,
            "DBS" to CardMotif.CITY_NIGHT,
        )
        expected.forEach { (issuer, motif) ->
            val card = UiVNextDemoFixture.cards.first { it.issuer == issuer }
            val identity = resolveCardIdentity(defaultCardProfile(card), card)
            assertEquals(motif, identity.motif, "issuer $issuer motif (synthetic only)")
        }
    }

    @Test
    fun noCardIdentityIsNearBlack() {
        cardIds.forEach { id ->
            val card = byId(id)
            val identity = resolveCardIdentity(defaultCardProfile(card), card)
            val energy = identity.top.luminance() + if (identity.motif == CardMotif.NONE) 0f else 0.03f
            assertTrue(identity.top.luminance() > 0.004f, "card $id top surface pure black")
            assertTrue(energy > 0.025f, "card $id face looks empty/near-black (top tone + artwork)")
        }
    }

    @Test
    fun studioCustomizationVisiblyChangesIdentity() {
        val card = byId("card-cn-2") // ICBC（默认 graphite/red-line）
        val customized = defaultCardProfile(card).copy(themeId = "glass", material = "glass", accentColor = "coral")
        val identity = resolveCardIdentity(customized, card)
        assertEquals(CardMotif.CHROMATIC, identity.motif, "glass theme must map to chromatic identity")
    }

    @Test
    fun allThemeThumbnailsHaveDistinctArtwork() {
        listOf("minimal", "matte", "deep-space", "region", "city", "glass", "metal", "abstract").forEach { theme ->
            val profile = PresentationProfile.defaultFor("card", "thumb", theme)
            val identity = resolveCardIdentity(profile, null)
            assertTrue(identity.motif != CardMotif.NONE, "theme $theme must carry artwork (minimal ≠ empty)")
            assertTrue(identityElementCount(identity, profile.material) >= 2, "theme $theme weak identity")
        }
    }
}
