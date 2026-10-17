package com.pdig.uivnext.ui

import com.pdig.uivnext.production.VNextProductionObject
import com.pdig.uivnext.production.VNextProductionSurfaceKind
import org.junit.Assert.assertEquals
import org.junit.Test

/** Responsive and privacy contracts for the reference-driven Production card collection. */
class ProductionCardCollectionContractTest {
    @Test
    fun phoneUsesLegibleRowsAndTabletsUseAnAssetGallery() {
        assertEquals(1, productionCardCollectionColumns(360f))
        assertEquals(1, productionCardCollectionColumns(599f))
        assertEquals(2, productionCardCollectionColumns(600f))
        assertEquals(2, productionCardCollectionColumns(839f))
        assertEquals(4, productionCardCollectionColumns(840f))
        assertEquals(4, productionCardCollectionColumns(1280f))
    }

    @Test
    fun issuerMaskingCannotTurnUnrecordedDataIntoKnownHiddenData() {
        assertEquals("发行方未记录", productionVisibleCardIssuer(null, false))
        assertEquals("发行方未记录", productionVisibleCardIssuer(null, true))
        assertEquals("发行方未记录", productionVisibleCardIssuer("   ", true))
        assertEquals("发行方已遮蔽", productionVisibleCardIssuer("示例银行", true))
        assertEquals("示例银行", productionVisibleCardIssuer("示例银行", false))
    }

    @Test
    fun maskingNeverRevealsUserProvidedPaymentAssetName() {
        val asset = VNextProductionObject(
            id = "card-secret",
            kind = "payment_instrument",
            name = "中国日常工资卡",
            surfaceKind = VNextProductionSurfaceKind.PAYMENT_ASSET,
            issuer = "示例银行",
            last4 = "8823",
        )
        assertEquals("中国日常工资卡", productionVisibleObjectName(asset, false))
        assertEquals("支付工具（已遮蔽）", productionVisibleObjectName(asset, true))
        assertEquals("尾号 ••••", productionPaymentTailLabel(asset.last4, true))
        assertEquals("尾号 8823", productionPaymentTailLabel(asset.last4, false))
    }
}
