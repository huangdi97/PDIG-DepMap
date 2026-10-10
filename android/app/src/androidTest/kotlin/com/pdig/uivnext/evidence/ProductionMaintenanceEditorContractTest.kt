package com.pdig.uivnext.evidence

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pdig.app.data.MaintenanceWriteResult
import com.pdig.app.data.NodeRow
import com.pdig.uivnext.production.VNextMaintenanceActionGateway
import com.pdig.uivnext.production.VNextProductionObject
import com.pdig.uivnext.production.VNextProductionSurfaceKind
import com.pdig.uivnext.ui.ProductionCardMaintenanceControls
import com.pdig.uivnext.ui.ProductionPhoneMaintenanceControls
import com.pdig.uivnext.ui.VAppState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProductionMaintenanceEditorContractTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun cardEditorCommitsThroughAuthorityAndRequestsRealityRefresh() {
        val app = VAppState()
        val authority = RecordingMaintenanceAuthority()
        val item = VNextProductionObject(
            id = "card-1",
            kind = "payment_instrument",
            name = "生产主卡",
            surfaceKind = VNextProductionSurfaceKind.PAYMENT_ASSET,
        )

        compose.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                ProductionCardMaintenanceControls(
                    appState = app,
                    authority = authority,
                    item = item,
                )
            }
        }

        compose.onNodeWithTag("pdig.production-vnext.card.maintenance.controls")
            .assertIsDisplayed()
        compose.onNodeWithText("编辑用卡周期").performClick()
        compose.onNodeWithTag("pdig.production-vnext.card.maintenance.amount")
            .performTextInput("100")
        compose.onNodeWithTag("pdig.production-vnext.card.maintenance.currency")
            .performTextInput("cny")
        compose.onNodeWithTag("pdig.production-vnext.card.maintenance.billing-day")
            .performTextInput("12")
        compose.onNodeWithTag("pdig.production-vnext.card.maintenance.save")
            .performClick()
        compose.waitForIdle()

        assertEquals("card-1", authority.lastCardNodeId)
        assertEquals("100", authority.lastCardAmount)
        assertEquals("CNY", authority.lastCardCurrency)
        assertEquals(12, authority.lastCardBillingDay)
        assertTrue(app.realityRefreshVersion > 0)
    }

    @Test
    fun phoneEditorDoesNotInventCompletionWhenSavingPlanFacts() {
        val app = VAppState()
        val authority = RecordingMaintenanceAuthority()
        val item = VNextProductionObject(
            id = "phone-1",
            kind = "identity_anchor",
            name = "生产号码",
            surfaceKind = VNextProductionSurfaceKind.PHONE_IDENTITY,
            identitySubtype = "phone_number",
        )

        compose.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                ProductionPhoneMaintenanceControls(
                    appState = app,
                    authority = authority,
                    item = item,
                )
            }
        }

        compose.onNodeWithText("编辑套餐资料").performClick()
        compose.onNodeWithTag("pdig.production-vnext.phone.maintenance.cost")
            .performTextInput("68")
        compose.onNodeWithTag("pdig.production-vnext.phone.maintenance.currency")
            .performTextInput("hkd")
        compose.onNodeWithTag("pdig.production-vnext.phone.maintenance.billing-mode")
            .performTextInput("月费套餐")
        compose.onNodeWithTag("pdig.production-vnext.phone.plan.save")
            .performClick()
        compose.waitForIdle()

        assertEquals("phone-1", authority.lastPhoneNodeId)
        assertEquals("68", authority.lastPhoneCost)
        assertEquals("HKD", authority.lastPhoneCurrency)
        assertEquals(0, authority.keepAliveCompletionCalls)
        assertTrue(app.realityRefreshVersion > 0)
    }

    private class RecordingMaintenanceAuthority : VNextMaintenanceActionGateway {
        var lastCardNodeId: String? = null
        var lastCardAmount: String? = null
        var lastCardCurrency: String? = null
        var lastCardBillingDay: Int? = null

        var lastPhoneNodeId: String? = null
        var lastPhoneCost: String? = null
        var lastPhoneCurrency: String? = null
        var keepAliveCompletionCalls: Int = 0

        override fun confirmCardLifecycle(
            nodeId: String,
            annualFeeAmount: String?,
            annualFeeCurrency: String?,
            billingDay: Int?,
            paymentDueDay: Int?,
            autopayMode: String?,
        ): MaintenanceWriteResult {
            lastCardNodeId = nodeId
            lastCardAmount = annualFeeAmount
            lastCardCurrency = annualFeeCurrency
            lastCardBillingDay = billingDay
            return success(nodeId, "payment_instrument")
        }

        override fun confirmCardAnnualFeeCheckpoint(
            nodeId: String,
            month: Int,
            day: Int,
        ): MaintenanceWriteResult = success(nodeId, "payment_instrument")

        override fun confirmPhonePlan(
            nodeId: String,
            planCost: String?,
            planCurrency: String?,
            billingMode: String?,
            renewalMethod: String?,
        ): MaintenanceWriteResult {
            lastPhoneNodeId = nodeId
            lastPhoneCost = planCost
            lastPhoneCurrency = planCurrency
            return success(nodeId, "identity_anchor")
        }

        override fun confirmPhoneKeepAliveInterval(
            nodeId: String,
            intervalDays: Int,
            anchorDate: String,
            lastCompletedAt: String?,
        ): MaintenanceWriteResult = success(nodeId, "identity_anchor")

        override fun confirmPhonePlanRenewalMonthly(
            nodeId: String,
            dayOfMonth: Int,
        ): MaintenanceWriteResult = success(nodeId, "identity_anchor")

        override fun markPhoneKeepAliveCompleted(
            nodeId: String,
            intervalDays: Int,
            anchorDate: String,
        ): MaintenanceWriteResult {
            keepAliveCompletionCalls += 1
            return success(nodeId, "identity_anchor")
        }

        private fun success(nodeId: String, kind: String) = MaintenanceWriteResult(
            node = NodeRow(
                id = nodeId,
                kind = kind,
                name = "test",
                archived = false,
                fieldsJson = "{}",
            ),
            graphRevision = 2,
        )
    }
}
