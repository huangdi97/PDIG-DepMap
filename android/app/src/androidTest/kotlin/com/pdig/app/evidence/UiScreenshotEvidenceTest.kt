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
                Thread.sleep(1800)
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
}