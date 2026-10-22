package com.pdig.uivnext.evidence

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
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
import com.pdig.uivnext.production.VNextProductionPlanAction
import com.pdig.uivnext.production.VNextProductionRecordItem
import com.pdig.uivnext.production.VNextProductionRecordState
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

    private fun session(
        records: List<VNextProductionRecordItem> = emptyList(),
        plan: VNextProductionPlan? = null,
    ): ProductionVNextSession {
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
                    identityIdentifierValue = "+86 138 0000 8823",
                    identityIdentifierVerificationBasisType = "user_confirmed",
                    identityIdentifierConfirmedAt = "2026-10-10T02:00:00Z",
                    identityIdentifierEvidenceRefs = listOf("ev-phone-value"),
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
            dataSource = FakeProductionSource(snapshot, records, plan),
        )
    }

    @Test
    fun productionRecordsExposeRecordedAndVerifiedAuthorityTimes() {
        val record = VNextProductionRecordItem(
            id = "record-timed",
            planId = "plan-timed",
            planTitle = "更换手机号",
            scenario = "replace_phone_number",
            actionId = "verify-timed",
            actionTitle = "验证新恢复路径",
            phase = "verify",
            state = VNextProductionRecordState.VERIFIED,
            evidenceRefs = listOf("ev-timed"),
            occurredAt = "2026-10-19T13:30:00Z",
            recordedAt = "2026-10-18T23:59:00Z",
            verifiedAt = "2026-10-19T13:30:00Z",
        )
        val session = session(records = listOf(record))
        compose.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                ProductionVNextShell(session, forcedViewportWidthDp = 390)
            }
        }

        compose.runOnIdle { session.appState.navigate(VScreen.RECORDS) }
        compose.waitForIdle()
        compose.onNodeWithText(
            "发生于 2026-10-19 · 记录于 2026-10-18 · 验证于 2026-10-19",
            substring = true,
        ).assertExists()
        compose.onNodeWithText("发生时间未记录").assertDoesNotExist()
    }

    @Test
    fun productionNowShowsRecordedVerificationWorkWithoutInventingAnAllClear() {
        val records = listOf(
            VNextProductionRecordItem(
                id = "waiting", planId = "plan-a", planTitle = "迁移手机号",
                scenario = "replace_phone_number", actionId = "waiting",
                actionTitle = "验证恢复路径", phase = "verify",
                state = VNextProductionRecordState.PENDING_VERIFICATION,
                evidenceRefs = emptyList(), occurredAt = null,
            ),
            VNextProductionRecordItem(
                id = "failed", planId = "plan-a", planTitle = "迁移手机号",
                scenario = "replace_phone_number", actionId = "failed",
                actionTitle = "测试登录", phase = "verify",
                state = VNextProductionRecordState.VERIFICATION_FAILED,
                evidenceRefs = emptyList(), occurredAt = null,
            ),
        )
        val session = session(records)
        compose.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                ProductionVNextShell(session, forcedViewportWidthDp = 390)
            }
        }
        compose.waitForIdle()
        compose.onNodeWithText("需要验证的操作").assertExists()
        compose.onNodeWithText("1 项待验证 · 1 项验证失败").assertExists()
        // The Now projection cannot invent a timestamp, a success or an evidence ref.
        compose.onNodeWithText("100%", substring = true).assertDoesNotExist()
        compose.onNodeWithText("全部安全", substring = true).assertDoesNotExist()

        // The selected state is exposed semantically, not just by blue pixels.
        compose.runOnIdle { session.appState.navigate(VScreen.RECORDS) }
        compose.waitForIdle()
        val all = "pdig.production-vnext.records.scope.all"
        val waiting = "pdig.production-vnext.records.scope.needs_verification"
        compose.onNodeWithTag(all).assertIsSelected()
        compose.onNodeWithTag(waiting).assertIsNotSelected()
        compose.onNodeWithTag(waiting).performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(all).assertIsNotSelected()
        compose.onNodeWithTag(waiting).assertIsSelected()
        compose.onNodeWithText("验证轨迹 · 1 项").assertExists()
    }

    @Test
    fun nowVerificationAttentionOpensOnlyPendingAndFailedRecordsThenNormalRootShowsAll() {
        fun item(id: String, state: VNextProductionRecordState) = VNextProductionRecordItem(
            id = id,
            planId = "plan-r59",
            planTitle = "更换手机号",
            scenario = "replace_phone_number",
            actionId = id,
            actionTitle = "需要人工核对的步骤",
            phase = "verify",
            state = state,
            evidenceRefs = emptyList(),
            occurredAt = null,
        )
        val session = session(listOf(
            item("pending", VNextProductionRecordState.PENDING_VERIFICATION),
            item("verified", VNextProductionRecordState.VERIFIED),
            item("failed", VNextProductionRecordState.VERIFICATION_FAILED),
        ))
        compose.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                ProductionVNextShell(session, forcedViewportWidthDp = 390)
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("pdig.production-vnext.now.verification-attention")
            .performScrollTo()
            .performClick()
        compose.waitForIdle()

        compose.onNodeWithTag("pdig.production-vnext.records.scope.attention").assertIsSelected()
        compose.onNodeWithTag("pdig.production-vnext.records.scope.all").assertIsNotSelected()
        compose.onNodeWithText("验证轨迹 · 2 项").assertExists()
        compose.onAllNodesWithText("发生时间未记录", substring = true)[0].assertExists()
        compose.onNodeWithText("100%", substring = true).assertDoesNotExist()

        // A direct primary-root visit intentionally resets the one-shot
        // attention entry, not the original source action states.
        compose.runOnIdle {
            session.appState.navigate(VScreen.NOW)
            session.appState.navigate(VScreen.RECORDS)
        }
        compose.waitForIdle()
        compose.onNodeWithTag("pdig.production-vnext.records.scope.all").assertIsSelected()
        compose.onNodeWithText("验证轨迹 · 3 项").assertExists()
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
        compose.onNodeWithText("尚无已确认地区", substring = true)
            .assertExists()

        compose.onNodeWithTag("pdig.nav.me", useUnmergedTree = true).performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("pdig.production-vnext.me.continuity", useUnmergedTree = true)
            .assertIsDisplayed()
        compose.onNodeWithTag("pdig.production-vnext.me.infrastructure", useUnmergedTree = true)
            .assertExists()
        compose.onNodeWithTag("pdig.production-vnext.me.controls", useUnmergedTree = true)
            .assertExists()
        compose.onNodeWithText("我的管理").assertExists()

        compose.onNodeWithContentDescription("搜索").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("pdig.production-vnext.search", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun productionNowShowsAuthoritativeWeaknessesAsAttentionNotHealthScore() {
        val session = session()
        compose.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                ProductionVNextShell(session, forcedViewportWidthDp = 390)
            }
        }
        compose.waitForIdle()

        // The fake source supplies exactly one governed Finding report entry.
        // The app must not replace it with an inferred health percentage.
        compose.onNodeWithText("基础设施薄弱点").assertExists()
        compose.onNodeWithText("1 项有已确认依据的连续性发现值得关注").assertExists()
        compose.onNodeWithText("100%", substring = true).assertDoesNotExist()
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
        compose.onNodeWithText("号码 / 邮箱只在已确认的类型范围内", substring = true).assertExists()
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
        compose.onNodeWithText("1 类已确认依据", substring = true).assertExists()
        compose.onNodeWithText("等待 Finding projection", substring = true)
            .assertDoesNotExist()
    }

    @Test
    fun cardCollectionDoesNotAdvertiseCreationWithoutProductionAuthority() {
        val session = session() // Source-only fixture, no manual-establish authority.
        compose.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                ProductionVNextShell(session, forcedViewportWidthDp = 390)
            }
        }
        compose.runOnIdle { session.appState.navigate(VScreen.CARDS) }
        compose.waitForIdle()
        compose.onNodeWithTag("pdig.production-vnext.inventory-category", useUnmergedTree = true)
            .assertIsDisplayed()
        compose.onNodeWithTag("pdig.production-vnext.card.add", useUnmergedTree = true)
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
        compose.onNodeWithTag("pdig.production-vnext.card.mask", useUnmergedTree = true)
            .assertExists()
        compose.onNodeWithText("图片、主题与单独遮蔽只保存在本机", substring = true)
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
        compose.onAllNodesWithText("+86 138 0000 8823")[0].assertIsDisplayed()
        // Identity-subtype and identifier have separate confirmed evidence.
        // A generic text matcher is ambiguous when both correctly show the
        // same confirmation label. Assert their distinct proof surfaces.
        compose.onNodeWithText("身份依据 1 项", substring = true).assertExists()
        compose.onNodeWithText("标识依据 1 项", substring = true).assertExists()
        // On a shorter phone (851dp) the change entry is below the first fold,
        // so a LazyColumn item is not composed. Scroll to it explicitly rather
        // than depending on the CI 914dp viewport height.
        val phoneDetail = compose.onNodeWithTag(
            "pdig.production-vnext.object-detail",
            useUnmergedTree = true,
        )
        phoneDetail.performScrollToNode(hasTestTag("pdig.production-vnext.phone.change-entry"))
        compose.onNodeWithTag("pdig.production-vnext.phone.change-entry", useUnmergedTree = true)
            .assertExists()
        phoneDetail.performScrollToNode(hasTestTag("pdig.production-vnext.phone.presentation-entry"))
        compose.onNodeWithTag("pdig.production-vnext.phone.presentation-entry", useUnmergedTree = true)
            .assertExists()
            .performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("pdig.production-vnext.phone.customization", useUnmergedTree = true)
            .assertIsDisplayed()
        compose.onNodeWithTag("pdig.production-vnext.phone.identity-face", useUnmergedTree = true)
            .assertExists()
        compose.onNodeWithTag("pdig.production-vnext.phone.alias", useUnmergedTree = true)
            .assertExists()

        compose.runOnIdle { session.appState.navigate(VScreen.EMAILS) }
        compose.waitForIdle()
        compose.onNodeWithText("m***@example.com").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(
            "pdig.production-vnext.secondary-identity-hero",
            useUnmergedTree = true,
        ).assertIsDisplayed()
        compose.onNodeWithText("通信身份", substring = true).assertExists()
        compose.onNodeWithText("正式来源已确认", substring = true).assertExists()
    }

    @Test
    fun privacyMaskHidesGovernedIdentityNamesWithoutRemovingSubtype() {
        val session = session()
        session.appState.privacyMask = true
        compose.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                ProductionVNextShell(session, forcedViewportWidthDp = 390)
            }
        }

        compose.runOnIdle { session.appState.navigate(VScreen.NUMBERS) }
        compose.waitForIdle()
        compose.onNodeWithText("+86 138****8823").assertDoesNotExist()
        compose.onNodeWithText("+86 138 0000 8823").assertDoesNotExist()
        compose.onNodeWithText("手机号身份（已遮蔽）").assertExists()
        compose.onNodeWithText("号码已遮蔽").assertExists()
    }

    @Test
    fun maskedProductionNumberDetailNeverLeaksSavedAliasInPageHeading() {
        val session = session()
        session.appState.renameNumber("phone-1", "超敏感主号")
        session.appState.privacyMask = true
        compose.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                ProductionVNextShell(session, forcedViewportWidthDp = 390)
            }
        }
        compose.runOnIdle { session.appState.openNumber("phone-1") }
        compose.waitForIdle()
        compose.onNodeWithTag("pdig.production-vnext.object-detail", useUnmergedTree = true)
            .assertIsDisplayed()
        compose.onNodeWithText("超敏感主号").assertDoesNotExist()
        compose.onNodeWithText("+86 138****8823").assertDoesNotExist()
        compose.onNodeWithText("+86 138 0000 8823").assertDoesNotExist()
        compose.onAllNodesWithText("手机号身份（已遮蔽）")[0].assertExists()
    }

    @Test
    fun verificationRecordOpensExactGovernedPlanActionAndBackKeepsAttentionScope() {
        val action = VNextProductionPlanAction(
            id = "verify-path",
            title = "核对备用恢复路径",
            phase = "verify",
            done = true,
            verificationStatus = "pending",
            verificationEvidenceRefs = emptyList(),
            prerequisiteActionIds = emptyList(),
            resolvesImpactKeys = emptyList(),
        )
        val plan = VNextProductionPlan(
            id = "plan-focused",
            scenario = "replace_phone_number",
            title = "更换手机号",
            workflowState = "in_progress",
            effectiveState = null,
            baselineGraphRevision = 21,
            lastAnalyzedGraphRevision = 21,
            currentGraphRevision = 21,
            targetNodeId = "phone-1",
            targetNodeName = "旧手机号",
            effectiveDate = null,
            readiness = "ready",
            affectedServiceCount = 1,
            mustChangeKeys = emptyList(),
            unresolvedMustChangeKeys = emptyList(),
            actions = listOf(action),
        )
        val sourceRecord = VNextProductionRecordItem(
            id = "record-verify-path",
            planId = plan.id,
            planTitle = plan.title,
            scenario = plan.scenario,
            actionId = action.id,
            actionTitle = action.title,
            phase = action.phase,
            state = VNextProductionRecordState.PENDING_VERIFICATION,
            evidenceRefs = emptyList(),
            occurredAt = null,
        )
        val session = session(listOf(sourceRecord), plan)
        compose.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                ProductionVNextShell(session, forcedViewportWidthDp = 390)
            }
        }
        compose.onNodeWithTag("pdig.production-vnext.now.verification-attention")
            .performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("pdig.production-vnext.records.scope.attention")
            .assertIsSelected()
        compose.onNodeWithTag("pdig.production-vnext.records.open-action")
            .performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("pdig.production-vnext.change.action.verify-path")
            .assertIsDisplayed()
        compose.onNodeWithText("从记录定位 · 请核对当前步骤").assertExists()
        compose.runOnIdle {
            session.appState.back()
            assertTrue(session.appState.screen == VScreen.RECORDS)
        }
        compose.waitForIdle()
        compose.onNodeWithTag("pdig.production-vnext.records.scope.attention")
            .assertIsSelected()
        compose.onNodeWithText("验证轨迹 · 1 项").assertExists()
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
        compose.onNodeWithTag(
            "pdig.production-vnext.object-row.account-1",
            useUnmergedTree = true,
        ).assertIsDisplayed()
        compose.onNodeWithText("账户 A").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("pdig.production-vnext.object-detail", useUnmergedTree = true)
            .assertIsDisplayed()
        compose.onNodeWithTag(
            "pdig.production-vnext.secondary-identity-hero",
            useUnmergedTree = true,
        ).assertIsDisplayed()
        compose.onNodeWithTag(
            "pdig.production-vnext.secondary-role-summary",
            useUnmergedTree = true,
        ).assertIsDisplayed()
        compose.onNodeWithText("账户控制关系").assertExists()
        compose.onNodeWithText("恢复此账户").assertExists()
        compose.onNodeWithText("0 不代表不存在未确认关系", substring = true).assertExists()
        compose.onAllNodesWithText("账户 A")[0].assertIsDisplayed()
        compose.onNodeWithText("账户身份", substring = true).assertExists()
        // The new identity + role cards push Impact below the first compact
        // viewport. Scroll the real LazyColumn to the impact item instead of
        // treating an uncomposed off-screen node as missing.
        val accountDetail = compose.onNodeWithTag(
            "pdig.production-vnext.object-detail",
            useUnmergedTree = true,
        )
        accountDetail.performScrollToNode(
            hasTestTag("pdig.production-vnext.impact-zero-boundary"),
        )
        compose.onNodeWithText("备用 / 降级").assertExists()
        compose.onNodeWithText("未知").assertDoesNotExist()
        compose.onNodeWithTag(
            "pdig.production-vnext.impact-zero-boundary",
            useUnmergedTree = true,
        ).assertExists()
        compose.onNodeWithText("0 不代表已证明没有未确认影响", substring = true).assertExists()
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
        compose.onNodeWithText("尚无已确认依据").assertIsDisplayed()
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
        private val records: List<VNextProductionRecordItem>,
        private val plan: VNextProductionPlan?,
    ) : VNextRuntimeDataSource {
        override val mode: VNextRuntimeDataMode = VNextRuntimeDataMode.PRODUCTION_REALITY

        override fun productionSnapshot(): VNextProductionSnapshot = snapshot

        override fun productionInventory() = buildProductionConsumerInventory(snapshot)

        override fun productionImpact(targetNodeId: String) = VNextProductionImpact(
            targetNodeId = targetNodeId,
            targets = emptyList(),
            checklist = emptyList(),
        )

        override fun productionPlan(planId: String): VNextProductionPlan? =
            plan?.takeIf { it.id == planId }

        override fun productionRecords(): List<VNextProductionRecordItem> = records

        override fun productionFindings(): VNextProductionFindingReport =
            VNextProductionFindingReport(
                findings = listOf(
                    VNextProductionFinding(
                        id = "spof:account-1",
                        type = VNextProductionFindingType.SINGLE_POINT_OF_FAILURE,
                        title = "生产恢复单一路径",
                        why = "当前已确认数据只有一条恢复来源。",
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
