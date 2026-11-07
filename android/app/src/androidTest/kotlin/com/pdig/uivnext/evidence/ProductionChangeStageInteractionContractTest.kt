package com.pdig.uivnext.evidence

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pdig.uivnext.production.VNextProductionPlan
import com.pdig.uivnext.production.VNextProductionPlanAction
import com.pdig.uivnext.production.VNextProductionPlanImpactItem
import com.pdig.uivnext.ui.ChangePlanAuthorityState
import com.pdig.uivnext.ui.ProductionChangeChoreography
import com.pdig.uivnext.ui.ProductionNextStepCard
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProductionChangeStageInteractionContractTest {
    @get:Rule
    val compose = createComposeRule()

    private fun plan(
        stale: Boolean = false,
        unresolved: Boolean = true,
    ): VNextProductionPlan {
        val key = "account-a|recovery"
        return VNextProductionPlan(
            id = "plan-change-stage",
            scenario = "replace_phone_number",
            title = "更换手机号",
            workflowState = "in_progress",
            effectiveState = if (stale) "needs_revalidation" else null,
            baselineGraphRevision = 4,
            lastAnalyzedGraphRevision = 5,
            currentGraphRevision = if (stale) 6 else 5,
            targetNodeId = "phone-old",
            targetNodeName = "旧手机号",
            effectiveDate = null,
            readiness = if (unresolved) "blocked" else "review_required",
            affectedServiceCount = 1,
            mustChangeKeys = listOf(key),
            unresolvedMustChangeKeys = if (unresolved) listOf(key) else emptyList(),
            actions = listOf(
                VNextProductionPlanAction(
                    id = "change-a",
                    title = "迁移恢复方式",
                    phase = "change",
                    done = false,
                    verificationStatus = "pending",
                    verificationEvidenceRefs = emptyList(),
                    prerequisiteActionIds = emptyList(),
                    resolvesImpactKeys = listOf(key),
                ),
            ),
            targetKeys = listOf(key),
            impactItems = listOf(
                VNextProductionPlanImpactItem(
                    key = key,
                    nodeId = "account-a",
                    nodeName = "主账户",
                    capability = "recovery",
                    status = "must_change",
                ),
            ),
        )
    }

    @Test
    fun stageTabsSwitchTheSamePersistedImpactAcrossCurrentTransitionAndAfter() {
        compose.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                ProductionChangeChoreography(plan())
            }
        }

        compose.onNodeWithTag("pdig.production-vnext.change.stage.0").assertIsSelected()
        compose.onNodeWithTag("pdig.production-vnext.change.stage.1").assertIsNotSelected()
        compose.onNodeWithText("必须处理").assertIsDisplayed()

        compose.onNodeWithTag("pdig.production-vnext.change.stage.1").performClick()
        compose.onNodeWithTag("pdig.production-vnext.change.stage.1").assertIsSelected()
        compose.onNodeWithText("待处理").assertIsDisplayed()

        compose.onNodeWithTag("pdig.production-vnext.change.stage.2").performClick()
        compose.onNodeWithTag("pdig.production-vnext.change.stage.2").assertIsSelected()
        compose.onNodeWithText("计划仍未解决").assertIsDisplayed()
        compose.onNodeWithText("完成后只是计划预期", substring = true).assertIsDisplayed()
        // 未发生 appears in both the AFTER stage summary and the truth boundary
        // note; assert that the requirement is visibly present rather than
        // pinning an exact occurrence count.
        assertTrue(
            compose.onAllNodesWithText("未发生", substring = true)
                .fetchSemanticsNodes().isNotEmpty(),
        )
    }

    @Test
    fun maskedChangeProjectionNeverEchoesAffectedObjectName() {
        compose.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                ProductionChangeChoreography(plan(), privacyMask = true)
            }
        }

        compose.onNodeWithText("主账户").assertDoesNotExist()
        compose.onNodeWithText("受影响对象（已遮蔽）").assertIsDisplayed()
    }

    @Test
    fun stalePlanOffersAuthoritativeReanalysisAction() {
        var clicks = 0
        compose.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                ChangePlanAuthorityState(
                    plan = plan(stale = true),
                    canReanalyze = true,
                    onReanalyze = { clicks += 1 },
                )
            }
        }

        compose.onNodeWithTag("pdig.production-vnext.change.reanalyze")
            .assertIsDisplayed()
            .performClick()
        compose.runOnIdle { assertEquals(1, clicks) }
    }

    @Test
    fun stalePlanWithoutChangeAuthorityShowsReadOnlyReanalysisBoundary() {
        compose.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                ChangePlanAuthorityState(
                    plan = plan(stale = true),
                    canReanalyze = false,
                    onReanalyze = {},
                )
            }
        }

        compose.onNodeWithText("当前无法重新分析").assertIsDisplayed()
        compose.onNodeWithTag("pdig.production-vnext.change.reanalyze").assertDoesNotExist()
    }

    @Test
    fun freshPlanDoesNotAdvertiseReanalysis() {
        compose.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                ChangePlanAuthorityState(
                    plan = plan(stale = false),
                    canReanalyze = true,
                    onReanalyze = {},
                )
            }
        }

        compose.onNodeWithTag("pdig.production-vnext.change.reanalyze").assertDoesNotExist()
        compose.onNodeWithText("重新分析当前影响").assertDoesNotExist()
    }
    @Test
    fun nextStepCardMakesTheExecutableStepTheSinglePrimaryAction() {
        var completed = 0
        var verified = 0
        compose.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                ProductionNextStepCard(
                    plan = plan(),
                    canMutate = true,
                    privacyMask = false,
                    onComplete = { completed += 1 },
                    onVerify = { verified += 1 },
                )
            }
        }

        compose.onNodeWithTag("pdig.production-vnext.change.next-step").assertIsDisplayed()
        compose.onNodeWithText("下一步").assertIsDisplayed()
        compose.onNodeWithText("迁移恢复方式").assertIsDisplayed()
        compose.onNodeWithText("没有前置步骤。").assertIsDisplayed()
        compose.onNodeWithTag("pdig.production-vnext.change.next-step.primary")
            .assertIsDisplayed()
            .performClick()
        compose.runOnIdle {
            assertEquals(1, completed)
            assertEquals(0, verified)
        }
    }

    @Test
    fun staleNextStepKeepsReanalysisAsTheOnlyPrimaryButton() {
        compose.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                ProductionNextStepCard(
                    plan = plan(stale = true),
                    canMutate = true,
                    privacyMask = false,
                    onComplete = {},
                    onVerify = {},
                )
            }
        }

        compose.onNodeWithText("重新分析当前影响").assertIsDisplayed()
        compose.onNodeWithText("使用上方的「重新分析当前影响」；界面不会用旧快照继续写入。")
            .assertIsDisplayed()
        // Exactly one reanalysis button exists on the screen: the banner's.
        compose.onNodeWithTag("pdig.production-vnext.change.next-step.primary").assertDoesNotExist()
    }
}
