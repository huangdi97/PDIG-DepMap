package com.pdig.uivnext.evidence

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.production.ProductionVNextSession
import com.pdig.uivnext.production.VNextPendingReviewSummary
import com.pdig.uivnext.production.VNextProductionDependency
import com.pdig.uivnext.production.VNextProductionFinding
import com.pdig.uivnext.production.VNextProductionFindingReport
import com.pdig.uivnext.production.VNextProductionFindingType
import com.pdig.uivnext.production.VNextProductionImpact
import com.pdig.uivnext.production.VNextProductionObject
import com.pdig.uivnext.production.VNextProductionPlan
import com.pdig.uivnext.production.VNextProductionRecordItem
import com.pdig.uivnext.production.VNextProductionSnapshot
import com.pdig.uivnext.production.VNextProductionSourceItem
import com.pdig.uivnext.production.VNextProductionSurfaceKind
import com.pdig.uivnext.production.VNextProjectionTruth
import com.pdig.uivnext.production.VNextRuntimeDataMode
import com.pdig.uivnext.production.VNextRuntimeDataSource
import com.pdig.uivnext.production.VNextSourceCoverageSummary
import com.pdig.uivnext.production.buildProductionConsumerInventory
import com.pdig.uivnext.ui.ProductionVNextShell
import com.pdig.uivnext.ui.VAppState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProductionVNextShellContractTest {
    @get:Rule
    val compose = createComposeRule()

    private fun session(): ProductionVNextSession {
        val snapshot = VNextProductionSnapshot(
            revision = 21,
            objects = listOf(
                VNextProductionObject(
                    id = "card-1",
                    kind = "payment_instrument",
                    name = "生产主卡",
                    surfaceKind = VNextProductionSurfaceKind.PAYMENT_ASSET,
                    issuer = "生产银行",
                    last4 = "8823",
                ),
                VNextProductionObject(
                    id = "account-1",
                    kind = "account",
                    name = "账户 A",
                    surfaceKind = VNextProductionSurfaceKind.ACCOUNT,
                ),
                VNextProductionObject(
                    id = "phone-1",
                    kind = "identity_anchor",
                    name = "+86 138****8823",
                    surfaceKind = VNextProductionSurfaceKind.PHONE_IDENTITY,
                    identitySubtype = "phone_number",
                    identityVerificationBasisType = "user_confirmed",
                    identityConfirmedAt = "2026-10-10T00:00:00Z",
                    identityEvidenceRefs = listOf("ev-phone"),
                ),
                VNextProductionObject(
                    id = "email-1",
                    kind = "identity_anchor",
                    name = "m***@example.com",
                    surfaceKind = VNextProductionSurfaceKind.EMAIL_IDENTITY,
                    identitySubtype = "email_address",
                    identityVerificationBasisType = "authoritative_source",
                    identityConfirmedAt = "2026-10-10T01:00:00Z",
                ),
                VNextProductionObject(
                    id = "identity-1",
                    kind = "identity_anchor",
                    name = "通用身份",
                    surfaceKind = VNextProductionSurfaceKind.IDENTITY_ANCHOR_GENERIC,
                ),
            ),
            confirmedDependencies = listOf(
                VNextProductionDependency(
                    id = "dep-1",
                    fromId = "card-1",
                    fromName = "生产主卡",
                    relation = "funding_source",
                    toId = "account-1",
                    toName = "账户 A",
                    capability = "payment",
                    criticality = "required",
                ),
            ),
            timeline = emptyList(),
            plans = emptyList(),
            pendingReview = VNextPendingReviewSummary(1, 0, 0),
            sourceCoverage = VNextSourceCoverageSummary(1, 1),
            sources = listOf(
                VNextProductionSourceItem(
                    id = "source-1",
                    label = "生产账单",
                    adapterId = "statement",
                    state = "active",
                    lastIngestedAt = null,
                ),
            ),
        )
        return ProductionVNextSession(
            appState = VAppState(),
            dataSource = FakeProductionSource(snapshot),
        )
    }

    @Test
    fun compactShellKeepsFivePrimaryMeAndRealitySearch() {
        val session = session()
        compose.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                ProductionVNextShell(session, forcedViewportWidthDp = 390)
            }
        }
        compose.waitForIdle()

        listOf("now", "infrastructure", "change", "records", "me").forEach { route ->
            compose.onNodeWithTag("pdig.nav.$route", useUnmergedTree = true)
                .assertIsDisplayed()
        }
        compose.onNodeWithTag("pdig.production-vnext.world-context", useUnmergedTree = true)
            .assertIsDisplayed()
        compose.onNodeWithText("地区定位尚未进入正式数据模型", substring = true)
            .assertIsDisplayed()

        compose.onNodeWithTag("pdig.nav.me", useUnmergedTree = true).performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("pdig.production-vnext.me.continuity", useUnmergedTree = true)
            .assertIsDisplayed()
        compose.onNodeWithTag("pdig.production-vnext.me.infrastructure", useUnmergedTree = true)
            .assertIsDisplayed()
        compose.onNodeWithTag("pdig.production-vnext.me.controls", useUnmergedTree = true)
            .assertIsDisplayed()
        compose.onNodeWithText("我的管理").assertIsDisplayed()

        compose.onNodeWithContentDescription("搜索").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("pdig.production-vnext.search", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun expandedMeRemainsAFirstClassPrimaryWorkspace() {
        val session = session()
        compose.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                ProductionVNextShell(session, forcedViewportWidthDp = 1000)
            }
        }
        compose.runOnIdle { session.appState.navigate(VScreen.ME) }
        compose.waitForIdle()

        compose.onNodeWithTag("pdig.nav.me", useUnmergedTree = true)
            .assertIsDisplayed()
        compose.onNodeWithTag("pdig.production-vnext.me.continuity", useUnmergedTree = true)
            .assertExists()
        compose.onNodeWithTag("pdig.production-vnext.me.infrastructure", useUnmergedTree = true)
            .assertExists()
        compose.onNodeWithTag("pdig.production-vnext.me.controls", useUnmergedTree = true)
            .assertExists()
        compose.onNodeWithText("我的数字生活", substring = true).assertIsDisplayed()
        compose.onNodeWithText("号码 / 邮箱只在受治理 subtype", substring = true).assertExists()
    }

    @Test
    fun productionInfrastructureShowsRealFindingCoverageInsteadOfStaleHold() {
        val session = session()
        compose.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                ProductionVNextShell(session, forcedViewportWidthDp = 390)
            }
        }

        compose.runOnIdle { session.appState.navigate(VScreen.INFRASTRUCTURE) }
        compose.waitForIdle()
        compose.onNodeWithText("4 类权威输入", substring = true).assertIsDisplayed()
        compose.onNodeWithText("等待 Finding projection", substring = true)
            .assertDoesNotExist()
    }

    @Test
    fun productionCardDetailKeepsAppearanceAsSmallLocalPresentationFeature() {
        val session = session()
        compose.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                ProductionVNextShell(session, forcedViewportWidthDp = 390)
            }
        }

        compose.runOnIdle { session.appState.openCard("card-1") }
        compose.waitForIdle()
        compose.onNodeWithTag("pdig.production-vnext.card.face", useUnmergedTree = true)
            .assertIsDisplayed()
        compose.onNodeWithTag("pdig.production-vnext.card.appearance-editor", useUnmergedTree = true)
            .assertExists()
        compose.onNodeWithTag("pdig.production-vnext.card.choose-photo", useUnmergedTree = true)
            .assertExists()
        compose.onNodeWithText("从相册更换卡面").assertExists()
        compose.onNodeWithText("图片仅保存在本机应用私有目录", substring = true)
            .assertExists()
    }

    @Test
    fun governedPhoneAndEmailAppearInProductionCategoriesAndDetails() {
        val session = session()
        compose.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                ProductionVNextShell(session, forcedViewportWidthDp = 390)
            }
        }

        compose.runOnIdle { session.appState.navigate(VScreen.NUMBERS) }
        compose.waitForIdle()
        compose.onNodeWithText("+86 138****8823").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("pdig.production-vnext.object-detail", useUnmergedTree = true)
            .assertIsDisplayed()
        compose.onNodeWithText("手机号身份").assertIsDisplayed()
        compose.onNodeWithText("用户已确认", substring = true).assertExists()
        compose.onNodeWithTag("pdig.production-vnext.phone.change-entry", useUnmergedTree = true)
            .assertExists()

        compose.runOnIdle { session.appState.navigate(VScreen.EMAILS) }
        compose.waitForIdle()
        compose.onNodeWithText("m***@example.com").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("邮箱身份").assertIsDisplayed()
        compose.onNodeWithText("权威来源已确认", substring = true).assertExists()
    }

    @Test
    fun privacyMaskHidesGovernedIdentityNamesWithoutRemovingSubtype() {
        val session = session()
        compose.runOnIdle { session.appState.setPrivacyMask(true) }
        compose.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                ProductionVNextShell(session, forcedViewportWidthDp = 390)
            }
        }

        compose.runOnIdle { session.appState.navigate(VScreen.NUMBERS) }
        compose.waitForIdle()
        compose.onNodeWithText("+86 138****8823").assertDoesNotExist()
        compose.onNodeWithText("手机号身份（已遮蔽）").assertExists()
    }

    @Test
    fun productionInventoryCategoryAndDetailUseProductionObjects() {
        val session = session()
        compose.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                ProductionVNextShell(session, forcedViewportWidthDp = 390)
            }
        }

        compose.runOnIdle { session.appState.navigate(VScreen.ACCOUNTS) }
        compose.waitForIdle()
        compose.onNodeWithTag("pdig.production-vnext.inventory-category", useUnmergedTree = true)
            .assertIsDisplayed()
        compose.onNodeWithText("账户 A").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("pdig.production-vnext.object-detail", useUnmergedTree = true)
            .assertIsDisplayed()
        compose.onNodeWithText("账户 A").assertIsDisplayed()
        compose.onNodeWithText("已确认关系").assertIsDisplayed()
    }

    @Test
    fun productionWeaknessesShowAuthoritativeFindingsAndCoverageBoundary() {
        val session = session()
        compose.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                ProductionVNextShell(session, forcedViewportWidthDp = 390)
            }
        }

        compose.runOnIdle { session.appState.navigate(VScreen.WEAKNESSES) }
        compose.waitForIdle()
        compose.onNodeWithTag("pdig.production-vnext.weaknesses", useUnmergedTree = true)
            .assertIsDisplayed()
        compose.onNodeWithText("生产恢复单一路径", substring = true).assertIsDisplayed()
        compose.onNodeWithText("尚未接入权威输入").assertIsDisplayed()
        compose.onNodeWithText("共享故障域", substring = true).assertIsDisplayed()
    }

    @Test
    fun productionPreferencesMutatePresentationStateOnly() {
        val session = session()
        compose.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                ProductionVNextShell(session, forcedViewportWidthDp = 390)
            }
        }

        compose.runOnIdle { session.appState.navigate(VScreen.SETTINGS) }
        compose.waitForIdle()
        compose.onNodeWithTag("pdig.production-vnext.preferences", useUnmergedTree = true)
            .assertIsDisplayed()
        compose.onNodeWithTag("pdig.production-vnext.preference.privacy", useUnmergedTree = true)
            .performClick()
        compose.runOnIdle {
            assertTrue(session.appState.privacyMask)
        }
    }

    private class FakeProductionSource(
        private val snapshot: VNextProductionSnapshot,
    ) : VNextRuntimeDataSource {
        override val mode: VNextRuntimeDataMode = VNextRuntimeDataMode.PRODUCTION_REALITY

        override fun productionSnapshot(): VNextProductionSnapshot = snapshot

        override fun productionInventory() = buildProductionConsumerInventory(snapshot)

        override fun productionImpact(targetNodeId: String) = VNextProductionImpact(
            targetNodeId = targetNodeId,
            targets = emptyList(),
            checklist = emptyList(),
        )

        override fun productionPlan(planId: String): VNextProductionPlan? = null

        override fun productionRecords(): List<VNextProductionRecordItem> = emptyList()

        override fun productionFindings(): VNextProductionFindingReport =
            VNextProductionFindingReport(
                findings = listOf(
                    VNextProductionFinding(
                        id = "spof:account-1",
                        type = VNextProductionFindingType.SINGLE_POINT_OF_FAILURE,
                        title = "生产恢复单一路径",
                        why = "当前已确认 Reality 只有一条恢复来源。",
                        confirmedBasis = "dep-recovery-1",
                        unknowns = "未记录路径仍可能存在。",
                        recommendedNextAction = "核对并验证备用恢复方式。",
                        evidenceRefs = listOf("dep-recovery-1"),
                        truth = VNextProjectionTruth.DERIVED,
                    ),
                ),
                supportedTypes = listOf("SINGLE_POINT_OF_FAILURE"),
                unsupportedTypes = listOf("SHARED_FAILURE_DOMAIN"),
            )
    }
}
