package com.pdig.uivnext.ui.screens

import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.ui.VAppState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchCatalogR19Test {
    @Test
    fun LifecycleFactsAreSearchableWithoutInventingObjects() {
        val fee = searchResults("¥100")
        assertTrue(fee.any { it is SearchResult.CardHit && it.id == "card-cn-2" })

        val keepDue = searchResults("2026-11-05")
        assertTrue(keepDue.any { it is SearchResult.NumberHit && it.id == "num-us-1" })
    }

    @Test
    fun SavedNumberAliasIsSearchableAndRenderedAsTheHitTitle() {
        val app = VAppState()
        app.renameNumber("num-hk-1", "香港银行主号")
        val results = searchResults("香港银行主号", app)
        assertTrue(results.any {
            it is SearchResult.NumberHit &&
                it.id == "num-hk-1" &&
                it.title == "香港银行主号"
        })
    }

    @Test
    fun maskedSearchStillMatchesRawFactsWithoutEchoingIdentity() {
        val app = VAppState().apply { privacyMask = true }

        val account = searchResults("微信账户", app)
            .filterIsInstance<SearchResult.AccountHit>()
            .first { it.id == "acc-cn-1" }
        assertEquals("账户（已遮蔽）", account.title)
        assertFalse(account.subtitle.contains("腾讯"))
        assertFalse(account.subtitle.contains("wxid_"))

        val email = searchResults("主邮箱", app)
            .filterIsInstance<SearchResult.EmailHit>()
            .first { it.id == "email-cn-1" }
        assertEquals("邮箱身份（已遮蔽）", email.title)
        assertFalse(email.subtitle.contains("Outlook"))
        assertFalse(email.subtitle.contains("outlook.com"))

        val device = searchResults("Pixel 8", app)
            .filterIsInstance<SearchResult.DeviceHit>()
            .first { it.id == "dev-cn-1" }
        assertEquals("设备（已遮蔽）", device.title)
        assertFalse(device.subtitle.contains("Pixel 8"))

        val service = searchResults("微信支付", app)
            .filterIsInstance<SearchResult.ServiceHit>()
            .first { it.id == "svc-wxpay" }
        assertEquals("服务（已遮蔽）", service.title)

        val card = searchResults("招商", app)
            .filterIsInstance<SearchResult.CardHit>()
            .first()
        assertEquals("支付工具（已遮蔽）", card.title)
        assertFalse(card.subtitle.contains("招商"))
        assertFalse(card.subtitle.contains("4417"))

        val number = searchResults("中国移动", app)
            .filterIsInstance<SearchResult.NumberHit>()
            .first()
        assertEquals("手机号身份（已遮蔽）", number.title)
        assertFalse(number.subtitle.contains("中国移动"))
        assertFalse(number.subtitle.contains("8823"))
    }

    @Test
    fun SecondarySearchHitOpensFocusedObjectDetail() {
        val app = VAppState()
        app.navigate(VScreen.SEARCH)
        val hit = searchResults("Pixel 8", app)
            .filterIsInstance<SearchResult.DeviceHit>()
            .first()
        openSearchResult(hit, app)
        assertTrue(app.screen == VScreen.DEVICE_DETAIL)
        assertTrue(app.selectedSecondaryObjectId == "dev-cn-1")
    }

    @Test
    fun CardReplacementSearchRequiresTargetSelectionInsteadOfOpeningEmptyFlow() {
        val hit = searchResults("换卡")
            .filterIsInstance<SearchResult.NavigationHit>()
            .first { it.title == "更换银行卡" }
        assertEquals(com.pdig.uivnext.model.VScreen.CARDS, hit.screen)
    }

    @Test
    fun ContinuityAndEstablishCapabilitiesAreSearchableAsConsumerLanguage() {
        assertTrue(searchResults("恢复循环").any {
            it is SearchResult.NavigationHit && it.screen == com.pdig.uivnext.model.VScreen.WEAKNESSES
        })
        assertTrue(searchResults("共享故障点").any {
            it is SearchResult.NavigationHit && it.screen == com.pdig.uivnext.model.VScreen.WEAKNESSES
        })
        assertTrue(searchResults("待复核").any {
            it is SearchResult.NavigationHit && it.screen == com.pdig.uivnext.model.VScreen.REVIEW
        })
        assertTrue(searchResults("导入").any {
            it is SearchResult.NavigationHit && it.screen == com.pdig.uivnext.model.VScreen.IMPORT
        })
        assertTrue(searchResults("手工记录").any {
            it is SearchResult.NavigationHit && it.screen == com.pdig.uivnext.model.VScreen.MANUAL_ADD
        })
    }

    @Test
    fun KeepRoleIsSearchableAsConsumerLanguage() {
        val results = searchResults("保号")
        assertTrue(results.any { it is SearchResult.NumberHit && it.id == "num-cn-3" })
        assertTrue(results.any { it is SearchResult.NumberHit && it.id == "num-us-1" })
    }
}
