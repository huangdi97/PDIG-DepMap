package com.pdig.uivnext.evidence

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pdig.uivnext.production.VNextProductionRecordItem
import com.pdig.uivnext.production.VNextProductionRecordState
import com.pdig.uivnext.ui.ProductionRecordPlanGroup
import com.pdig.uivnext.ui.productionRecordNavigationNote
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProductionRecordNavigationContractTest {
    @get:Rule val compose = createComposeRule()

    private val record = VNextProductionRecordItem(
        id = "record-1",
        planId = "plan-1",
        planTitle = "迁移 +852 6123 4567",
        scenario = "replace_phone_number",
        actionId = "verify-1",
        actionTitle = "验证旧号码恢复关系",
        phase = "verify",
        state = VNextProductionRecordState.VERIFIED,
        evidenceRefs = listOf("evidence-1"),
        occurredAt = null,
    )

    @Test
    fun readablePlanHeaderOpensCurrentPlanWithoutChangingVerificationTruth() {
        var opened = 0
        compose.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                ProductionRecordPlanGroup(
                    items = listOf(record),
                    onOpenPlan = { opened++ },
                )
            }
        }
        compose.onNodeWithText("查看计划 →").assertIsDisplayed()
        compose.onNodeWithTag("pdig.production-vnext.records.plan-header")
            .performClick()
        compose.runOnIdle { assertEquals(1, opened) }
        compose.onNodeWithText("已验证").assertIsDisplayed()
        compose.onNodeWithText("验证证据 1 项", substring = true).assertIsDisplayed()
    }

    @Test
    fun supportedLiveActionIsNavigableButUnsupportedAndMissingStepsAreReadOnly() {
        var opened = 0
        compose.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                ProductionRecordPlanGroup(
                    items = listOf(record),
                    onOpenPlan = { opened++ },
                    onOpenAction = { opened += 10 },
                    openableActionIds = setOf("verify-1"),
                    unavailableActionExplanation = {
                        productionRecordNavigationNote(true, true, true)
                    },
                )
            }
        }
        compose.onNodeWithText("查看对应步骤 →").assertIsDisplayed()
        compose.onNodeWithTag("pdig.production-vnext.records.open-action").performClick()
        compose.runOnIdle { assertEquals(10, opened) }
    }

    @Test
    fun unsupportedScenarioShowsTruthfulReadOnlyExplanationInsteadOfGhostCTA() {
        compose.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                ProductionRecordPlanGroup(
                    items = listOf(record.copy(scenario = "replace_device")),
                    unavailableActionExplanation = {
                        productionRecordNavigationNote(true, false, true)
                    },
                )
            }
        }
        compose.onNodeWithText("查看计划 →").assertDoesNotExist()
        compose.onNodeWithText("查看对应步骤 →").assertDoesNotExist()
        compose.onNodeWithText(
            "当前版本尚未提供此变更类型的步骤页面 · 仅保留操作记录",
        ).assertIsDisplayed()
        compose.onNodeWithText("已验证").assertIsDisplayed()
    }

    @Test
    fun staleActionShowsItIsRetainedAsRecordNotLivePlanStep() {
        compose.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                ProductionRecordPlanGroup(
                    items = listOf(record),
                    onOpenPlan = {},
                    unavailableActionExplanation = {
                        productionRecordNavigationNote(true, true, false)
                    },
                )
            }
        }
        compose.onNodeWithText("查看对应步骤 →").assertDoesNotExist()
        compose.onNodeWithText(
            "对应步骤已不在当前正式计划中 · 仅保留操作记录",
        ).assertIsDisplayed()
    }

    @Test
    fun maskedUnavailablePlanHidesIdentityAndHasNoPhantomNavigation() {
        compose.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                ProductionRecordPlanGroup(
                    items = listOf(record),
                    privacyMask = true,
                    onOpenPlan = null,
                )
            }
        }
        compose.onNodeWithText("变更计划（已遮蔽）").assertIsDisplayed()
        compose.onNodeWithText("操作步骤（已遮蔽）").assertIsDisplayed()
        compose.onNodeWithText("查看计划 →").assertDoesNotExist()
        compose.onNodeWithText("迁移 +852 6123 4567").assertDoesNotExist()
        compose.onNodeWithText("验证旧号码恢复关系").assertDoesNotExist()
    }
}
