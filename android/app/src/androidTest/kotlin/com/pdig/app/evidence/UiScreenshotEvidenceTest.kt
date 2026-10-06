package com.pdig.app.evidence

import android.content.ContentValues
import android.provider.MediaStore
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pdig.app.data.AppContainer
import com.pdig.app.data.ScenarioPlanRequest
import com.pdig.app.ui.screens.AboutScreen
import com.pdig.app.ui.screens.BackupScreen
import com.pdig.app.ui.screens.CandidateReviewScreen
import com.pdig.app.ui.screens.ChangePlanScreen
import com.pdig.app.ui.screens.GraphScreen
import com.pdig.app.ui.screens.HomeScreen
import com.pdig.app.ui.screens.ImpactScreen
import com.pdig.app.ui.screens.ImportScreen
import com.pdig.app.ui.screens.InfrastructureScreen
import com.pdig.app.ui.screens.NodeDetailScreen
import com.pdig.app.ui.screens.PendingReviewScreen
import com.pdig.app.ui.screens.PrivacyScreen
import com.pdig.app.ui.screens.RealityDriftScreen
import com.pdig.app.ui.screens.RestoreScreen
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pdig.app.ui.screens.ScenarioCenterScreen
import com.pdig.app.ui.screens.ScenarioSetupScreen
import com.pdig.app.ui.screens.FindingsScreen
import com.pdig.app.ui.screens.SettingsScreen
import com.pdig.app.ui.screens.SourceManagementScreen
import com.pdig.app.ui.screens.TimelineScreen
import com.pdig.app.ui.theme.PDIGTheme
import androidx.navigation.compose.rememberNavController
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.onNodeWithTag
import com.pdig.uivnext.VNextApp
import com.pdig.uivnext.createVNextAppState
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.model.VTestIds
import com.pdig.uivnext.ui.VAppState

/**
 * 2026-09-26 multiclient sweep: per-page visual evidence on the API36 AVD.
 *
 * Seeds the app's REAL singleton (AppContainer.get) through the same repos the
 * product uses (import -> proposal -> required dep -> engine candidate/drift ->
 * change plan), then RENDERS each production screen through the same composables
 * the navigation graph hosts, capturing the composed pixels as PNG via
 * Compose's captureToImage. Screenshots are written under the app's filesDir and
 * pulled with adb by the sweep harness.
 *
 * Every capture asserts a non-empty PNG; the test fails if any screen cannot render
 * or the file is empty — no fake screenshots.
 */
@RunWith(AndroidJUnit4::class)
class UiScreenshotEvidenceTest {

    @get:Rule
    val compose = createComposeRule()

    private fun ctx() = InstrumentationRegistry.getInstrumentation().targetContext
    private val outDir: File get() = File(ctx().filesDir, "ui-shots").apply { mkdirs() }

    private fun obs(txn: String, merchant: String, card: String) = com.pdig.core.sources.Observation(
        source = "manual",
        sourceTxnId = txn,
        merchantTxnId = "",
        occurredAt = "2026-09-01T00:00:00Z",
        merchantRaw = merchant,
        description = "",
        amount = 12.0,
        currency = "CNY",
        direction = com.pdig.core.sources.ObservationDirection.OUT,
        paymentMethodRaw = card,
        status = "",
        note = "",
    )

    private fun seed(app: AppContainer) {
        fun import(rows: List<com.pdig.core.sources.Observation>, label: String) {
            val preview = app.previewImport(rows, emptyList(), "manual", label)
            app.commitImport(preview)
        }
        import(listOf(obs("t1", "示例服务", "示例卡 8848"), obs("t2", "示例服务", "示例卡 8848")), "shot-1")
        app.pendingProposals().firstOrNull()?.let { app.acceptProposal(it.id) }
        app.dependencies().firstOrNull()?.let { app.setDependencyCriticality(it.id, true) }
        // engine candidate + drift: same merchant on a second card
        import(listOf(obs("t3", "示例服务", "备用卡 6677"), obs("t4", "示例服务", "备用卡 6677")), "shot-2")
        // v0.3.0 身份/恢复边：与 Desktop ShotDriver 同一套合成数据，
        // 让「基础设施薄弱点」展示 SPOF / 共享故障点 / 恢复循环三类示例。
        val now = java.time.Instant.now().toString()
        val phone1 = "shot-phone-1"
        val phone2 = "shot-phone-2"
        val dev = "shot-dev-1"
        val acct = "shot-acct-1"
        val drv = app.evidenceDriver
        drv.exec("INSERT INTO nodes (id, kind, name, archived, fields_json, owner, created_at, updated_at) " +
            "VALUES ('$phone1','identity_anchor','旧手机号',0,'{}','self','$now','$now')")
        drv.exec("INSERT INTO nodes (id, kind, name, archived, fields_json, owner, created_at, updated_at) " +
            "VALUES ('$phone2','identity_anchor','新手机号',0,'{}','self','$now','$now')")
        drv.exec("INSERT INTO nodes (id, kind, name, archived, fields_json, owner, created_at, updated_at) " +
            "VALUES ('$dev','device','备用设备',0,'{}','self','$now','$now')")
        drv.exec("INSERT INTO nodes (id, kind, name, archived, fields_json, owner, created_at, updated_at) " +
            "VALUES ('$acct','account','主账户',0,'{}','self','$now','$now')")
        fun depEdge(id: String, from: String, to: String) {
            drv.exec(
                "INSERT INTO dependencies (id, from_node, relation, to_node, capability, criticality, state, origin, confirmed_at, last_verified_at, created_at, updated_at) " +
                    "VALUES ('$id','$from','recovers','$to','recovery','unknown','active','manual','$now','$now','$now','$now')",
            )
        }
        depEdge("shot-dep-1", dev, phone1)
        depEdge("shot-dep-2", dev, acct)
        depEdge("shot-dep-3", phone2, acct)
        depEdge("shot-dep-4", acct, phone2)
        // change plan
        val target = app.nodes().firstOrNull() ?: return
        val planId = app.createPlanForScenario(
            ScenarioPlanRequest("replace_payment_card", targetNodeId = target.id, effectiveDate = null),
        )
        val detail = app.planDetail(planId)
        if (detail != null) {
            for (a in detail.actions) {
                if (a.resolvesImpactKeys.isNotEmpty()) {
                    app.completeAction(planId, a.id)
                }
            }
            val d2 = app.planDetail(planId)
            if (d2 != null) {
                for (a in d2.actions) {
                    val v = a.verification
                    if (a.done && v != null && v.status != com.pdig.core.domain.ActionVerificationStatus.NOT_REQUIRED) {
                        app.verifyAction(planId, a.id)
                    }
                }
            }
        }
    }

    private data class Slot(val content: @Composable () -> Unit, val dark: Boolean)

    private var current by mutableStateOf(Slot({}, false))

    private fun capture(pageId: String, seq: Int) {
        var attempts = 0
        while (true) {
            try {
                attempts++
                compose.waitForIdle()
                Thread.sleep(3500)
                compose.waitForIdle()
                val bmp = compose.onRoot().captureToImage().asAndroidBitmap()
                val theme = if (current.dark) "dark" else "light"
                val name = "android__phone-api36__${theme}__${pageId}__populated__%02d.png".format(seq)
                val f = File(outDir, name)
                f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                assertTrue("screenshot must be non-empty: ${f.name}", f.isFile && f.length() > 0)
                // Also publish into public Downloads so the sweep harness can adb-pull
                // (Android 11+ scoped storage hides app dirs from adb shell).
                try {
                    val cv = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/ui-shots")
                    }
                    val uri = ctx().contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv)
                    if (uri != null) {
                        val bos = java.io.ByteArrayOutputStream()
                        bmp.compress(Bitmap.CompressFormat.PNG, 100, bos)
                        ctx().contentResolver.openOutputStream(uri)?.use { it.write(bos.toByteArray()) }
                    }
                } catch (_: Throwable) {
                    // best-effort: filesDir copy is the source of truth for the in-test assert
                }
                return
            } catch (t: Throwable) {
                if (attempts >= 3) {
                    throw IllegalStateException("capture failed for page=$pageId seq=$seq after 3 attempts", t)
                }
                Thread.sleep(2000)
            }
        }
    }

    @Test
    fun capturesAllProductionPages_lightAndDark() {
        // determinism: wipe the app DB then reseed the singleton the UI reads
        File(ctx().filesDir, "pdig.db").takeIf { it.exists() }?.delete()
        File(ctx().filesDir, "pdig.db-shm").takeIf { it.exists() }?.delete()
        File(ctx().filesDir, "pdig.db-wal").takeIf { it.exists() }?.delete()
        AppContainer.reset()
        val app = AppContainer.get(ctx())
        seed(app)
        val nodeId = app.nodes().firstOrNull()?.id ?: "n-none"
        val planId = app.plans().firstOrNull()?.id ?: "p-none"

        var seq = 0
        val screens = listOf<Pair<String, @Composable () -> Unit>>(
            "home" to { HomeScreen(rememberNavController()) },
            "scenarios" to { ScenarioCenterScreen(rememberNavController()) },
            "timeline" to { TimelineScreen(rememberNavController()) },
            "review" to { PendingReviewScreen(rememberNavController()) },
            "drift" to { RealityDriftScreen(rememberNavController()) },
            "candidates" to { CandidateReviewScreen(rememberNavController()) },
            "infrastructure" to { InfrastructureScreen(rememberNavController()) },
            "findings" to { FindingsScreen(rememberNavController()) },
            "graph" to { GraphScreen(rememberNavController()) },
            "sources" to { SourceManagementScreen(rememberNavController()) },
            "import" to { ImportScreen(rememberNavController()) },
            "backup" to { BackupScreen(rememberNavController()) },
            "restore" to { RestoreScreen(rememberNavController()) },
            "settings" to { SettingsScreen(rememberNavController()) },
            "privacy" to { PrivacyScreen(rememberNavController()) },
            "about" to { AboutScreen(rememberNavController()) },
            "impact" to { ImpactScreen(rememberNavController(), nodeId) },
            "scenario-setup" to { ScenarioSetupScreen(rememberNavController(), "replace_payment_card") },
            "scenario-setup-phone" to { ScenarioSetupScreen(rememberNavController(), "replace_phone_number") },
            "plan" to { ChangePlanScreen(rememberNavController(), planId) },
            "node" to { NodeDetailScreen(rememberNavController(), nodeId) },
        )
        compose.setContent {
            val slot = current
            PDIGTheme(darkTheme = slot.dark) { slot.content() }
        }
        compose.waitForIdle()
        for (dark in listOf(false, true)) {
            for ((pageId, content) in screens) {
                seq++
                current = Slot(content, dark)
                capture(pageId, seq)
            }
        }
        assertTrue("expected 40 screenshots, found ${outDir.listFiles()?.size}", (outDir.listFiles()?.size ?: 0) >= 40)
    }

    // ---------------------------------------------------------------------------
    // UI vNext 演示层证据（2026-09-29）：10 屏截图 + wide 布局 2 张 + testTag geometry probe。
    // 纯 synthetic fixture（com.pdig.uivnext.*），不触碰真实 repos；不删不改既有测试/断言。
    // ---------------------------------------------------------------------------
    private var vnextSlot by mutableStateOf<(@Composable () -> Unit)?>(null)

    private fun captureVNext(pageId: String, seq: Int) {
        var attempts = 0
        while (true) {
            try {
                attempts++
                compose.waitForIdle()
                Thread.sleep(1500)
                compose.waitForIdle()
                // 窗口测量等待：活动窗口冷启动可能未测量（0 尺寸；实测 AVD 偶发）。
                for (w in 0 until 20) {
                    val rootSize = runCatching { compose.onRoot().fetchSemanticsNode().size }.getOrNull()
                    if (rootSize != null && rootSize.width > 0 && rootSize.height > 0) break
                    compose.waitForIdle()
                    Thread.sleep(1000)
                }
                val bmp = compose.onRoot().captureToImage().asAndroidBitmap()
                check(bmp.width > 0 && bmp.height > 0) { "vnext capture zero-size: $pageId" }
                val name = "android__phone-api36__vnext__dark__${pageId}__%02d.png".format(seq)
                val f = File(outDir, name)
                f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                assertTrue("vnext screenshot must be non-empty: ${f.name}", f.isFile && f.length() > 0)
                try {
                    val cv = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/ui-shots")
                    }
                    val uri = ctx().contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv)
                    if (uri != null) {
                        val bos = java.io.ByteArrayOutputStream()
                        bmp.compress(Bitmap.CompressFormat.PNG, 100, bos)
                        ctx().contentResolver.openOutputStream(uri)?.use { it.write(bos.toByteArray()) }
                    }
                } catch (_: Throwable) {
                    // best-effort: filesDir copy is the source of truth for the in-test assert
                }
                return
            } catch (t: Throwable) {
                if (attempts >= 3) {
                    throw IllegalStateException("vnext capture failed for page=$pageId seq=$seq after 3 attempts", t)
                }
                Thread.sleep(2000)
            }
        }
    }

    /** testTag geometry probe：boundsInRoot（与 desktop UI_LAYOUT_PROBE 同语义）。
     *  带有限重试：慢速 emulator 上导航后的第一帧可能尚未完成 layout（bounds=0），
     *  最多 10 次 × 500ms 等真实 layout（与既有 capture 重试硬化一致）。 */
    private fun probeTag(tag: String): Rect {
        compose.waitForIdle()
        var node = compose.onNodeWithTag(tag).fetchSemanticsNode()
        for (attempt in 0 until 10) {
            if (node.boundsInRoot.width > 0f && node.boundsInRoot.height > 0f) break
            Thread.sleep(500)
            compose.waitForIdle()
            node = compose.onNodeWithTag(tag).fetchSemanticsNode()
        }
        Log.i("UiVNextEvidence", "probe ${node.boundsInRoot} tag=$tag")
        return node.boundsInRoot
    }

    @Test
    fun capturesVNextDemoScreens_andProbesKeyTestTags() {
        var seq = 0
        val screens = listOf<Pair<String, (VAppState) -> Unit>>(
            "now" to {},
            "infrastructure-overview" to { app -> app.navigate(VScreen.OVERVIEW) },
            "cards" to { app -> app.navigate(VScreen.CARDS) },
            "card-detail" to { app -> app.openCard("card-cn-2") },
            "numbers" to { app -> app.navigate(VScreen.NUMBERS) },
            "number-detail" to { app -> app.openNumber("num-cn-1") },
            "change-phone" to { app -> app.navigate(VScreen.CHANGE_PHONE) },
            "card-customization" to { app -> app.openCardCustomization("card-cn-1") },
            "number-customization" to { app -> app.openNumberCustomization("num-cn-1") },
            "personalization" to { app -> app.navigate(VScreen.PERSONALIZATION) },
        )
        compose.setContent { vnextSlot?.invoke() }
        compose.waitForIdle()
        // 1) 手机（compact）10 屏截图
        for ((pageId, prepare) in screens) {
            val app = createVNextAppState().apply { reduceMotion = true }
            prepare(app)
            vnextSlot = { VNextApp(app) }
            seq++
            captureVNext(pageId, seq)
        }
        // 2) wide 布局截图（rail 形态证据，宽度冻结 1280dp）
        val wideOverview = createVNextAppState().apply { reduceMotion = true; navigate(VScreen.OVERVIEW) }
        vnextSlot = { VNextApp(wideOverview, forcedViewportWidthDp = 1280) }
        seq++
        captureVNext("overview-wide-1280dp", seq)
        val wideCards = createVNextAppState().apply { reduceMotion = true; navigate(VScreen.CARDS) }
        vnextSlot = { VNextApp(wideCards, forcedViewportWidthDp = 1280) }
        seq++
        captureVNext("cards-wide-1280dp", seq)

        // 3) testTag geometry probe。
        // forcedViewportWidthDp 只选择 responsive branch，并不会把 Phone AVD 的真实窗口变宽；
        // 因此只用它验证 wide shell 的 NavigationRail。把同一窄窗口强行走 wide content
        // 会把内容区压成 0 宽，属于 test-bed artifact，不是产品布局回归。
        val wideShellProbeApp = createVNextAppState().apply { reduceMotion = true }
        vnextSlot = { VNextApp(wideShellProbeApp, forcedViewportWidthDp = 1280) }
        compose.waitForIdle()
        val rail = probeTag("pdig.nav.rail")
        assertTrue("pdig.nav.rail must be laid out in wide shell", rail.width > 0f && rail.height > 0f)

        // 内容几何在当前 AVD 的真实 viewport 下验证；真实 expanded viewport 由
        // SourceCompleteScreenshotEvidenceTest 的 API36 Tablet 运行证据覆盖。
        val contentProbeApp = createVNextAppState().apply {
            reduceMotion = true
            navigate(VScreen.OVERVIEW)
        }
        vnextSlot = { VNextApp(contentProbeApp) }
        compose.waitForIdle()
        val stage = probeTag("pdig.globe.stage")
        assertTrue("pdig.globe.stage must be laid out on overview", stage.width > 0f && stage.height > 0f)
        contentProbeApp.navigate(VScreen.CARDS)
        compose.waitForIdle()
        val cardList = probeTag(VTestIds.CARD_LIST)
        assertTrue("pdig.card.list must be laid out on compact cards", cardList.width > 0f && cardList.height > 0f)

        val probeText = buildString {
            appendLine("pdig.nav.rail: x=${rail.left.toInt()} y=${rail.top.toInt()} w=${rail.width.toInt()} h=${rail.height.toInt()}")
            appendLine("pdig.globe.stage: x=${stage.left.toInt()} y=${stage.top.toInt()} w=${stage.width.toInt()} h=${stage.height.toInt()}")
            appendLine("pdig.card.list: x=${cardList.left.toInt()} y=${cardList.top.toInt()} w=${cardList.width.toInt()} h=${cardList.height.toInt()}")
        }
        File(File(ctx().filesDir, "ui-shots"), "vnext-testtag-probe.txt").writeText(probeText)
        Log.i("UiVNextEvidence", "vnext testTag probe:\n$probeText")

        val vnextCount = outDir.listFiles()?.count { it.name.contains("vnext") } ?: 0
        assertTrue("expected >= 12 vnext screenshots, found $vnextCount", vnextCount >= 12)
    }
}