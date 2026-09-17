package com.pdig.app.evidence

import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pdig.app.security.LockCapability
import com.pdig.app.ui.screens.AboutScreen
import com.pdig.app.ui.screens.BackupScreen
import com.pdig.app.ui.screens.HomeScreen
import com.pdig.app.ui.screens.ImportScreen
import com.pdig.app.ui.screens.InfrastructureScreen
import com.pdig.app.ui.screens.LockScreen
import com.pdig.app.ui.screens.PrivacyScreen
import com.pdig.app.ui.screens.RealityDriftScreen
import com.pdig.app.ui.screens.RestoreScreen
import com.pdig.app.ui.screens.ScenarioCenterScreen
import com.pdig.app.ui.screens.ScenarioSetupScreen
import com.pdig.app.ui.screens.SettingsScreen
import com.pdig.app.ui.screens.SourceManagementScreen
import com.pdig.app.ui.theme.PDIGTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * ANDROID_ACCESSIBILITY —— 交互元素的语义标签闭合（P1-B）。
 *
 * ## 为什么不用 uiautomator 做这个门禁
 *
 * 2026-09-16 实测：用 `uiautomator dump` 统计"无标签可点击节点"，结果是**不可信的**：
 *
 *  - Compose 的 `Card(onClick = ...)` 在 uiautomator 里表现为一个 `clickable=true` 但
 *    `text` / `content-desc` 都为空的 `android.view.View`，其文字在**子** TextView 上；
 *  - 同一个 `Button` 有时只报出它的 TextView（`clickable=false`），连点击动作都看不见 ——
 *    可它点了确实生效（LockScreen 的放行按钮就是这样）。
 *
 * 同一份 UI，两种口径给出完全不同的"无标签数量"。**检测口径本身必须先自证**，
 * 所以本门禁改为直接读 **Compose 语义树**（合并后的树，与 TalkBack 看到的是同一棵树）。
 *
 * ## 判定规则
 *
 * 每个带 onClick 动作的节点，必须在下列之一里有内容：
 *   `Text` / `ContentDescription` / `EditableText` / `Hint`
 * 只有真正用户可交互的元素才被检查 —— 不给容器乱加 contentDescription 充数。
 */
@RunWith(AndroidJUnit4::class)
class AccessibilitySemanticsTest {

    @get:Rule
    val compose = createComposeRule()

    private fun auditScreen(
        screen: String,
        /** 某些页面的内容是从数据库异步加载的（见 D-12 的修复），组合完成时尚处于加载态。
         *  给出等待标志后，门禁会等真实内容出现再扫描，避免对着加载态得出"无节点"结论。 */
        awaitText: String? = null,
        content: @Composable () -> Unit,
    ) {
        compose.setContent { PDIGTheme { content() } }
        compose.waitForIdle()
        if (awaitText != null) {
            // 上限按实测给足：本机 AVD 上"打开 SQLCipher 密文库 + Argon2id 派生"
            // 实测可达分钟级（E2E 里首页『共 N 个对象』也观测到同样量级），
            // 30s 会稳定超时，那不是无障碍缺陷而是等待窗口不足。
            compose.waitUntil(timeoutMillis = 180_000) {
                compose.onAllNodesWithText(awaitText, substring = true)
                    .fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
            }
            compose.waitForIdle()
        }

        val clickable = compose
            .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick))
            .fetchSemanticsNodes()

        // 反空转断言：如果这个门禁一个节点都没扫到，它就是"永远绿"的假门禁。
        // 每个页面都必须至少有 1 个可交互节点（Back 按钮或内容卡片）。
        assertTrue(
            "[$screen] 没有扫描到任何带 onClick 的语义节点 —— 门禁空转，结果不可信",
            clickable.isNotEmpty(),
        )

        val unlabeled = clickable.filter { node ->
            val cfg = node.config
            val text = cfg.getOrNull(SemanticsProperties.Text)?.joinToString(" ") { it.text }.orEmpty()
            val desc = cfg.getOrNull(SemanticsProperties.ContentDescription)?.joinToString(" ").orEmpty()
            val editable = cfg.getOrNull(SemanticsProperties.EditableText)?.text.orEmpty()
            (text + desc + editable).isBlank()
        }

        val detail = unlabeled.joinToString("; ") { n ->
            "bounds=${n.boundsInRoot} role=${n.config.getOrNull(SemanticsProperties.Role)}"
        }
        assertTrue(
            "[$screen] 存在 ${unlabeled.size} 个无标签的可交互节点（共 ${clickable.size} 个）：$detail",
            unlabeled.isEmpty(),
        )

        val withDesc = clickable.count {
            it.config.getOrNull(SemanticsProperties.ContentDescription)?.any { s -> s.isNotBlank() } == true
        }
        val withText = clickable.count {
            it.config.getOrNull(SemanticsProperties.Text)?.any { s -> s.text.isNotBlank() } == true
        }
        val line = "screen=$screen clickable=${clickable.size} unlabeled=0 " +
            "labeledByText=$withText labeledByContentDescription=$withDesc"
        println("A11Y_SCREEN $line")
        // 证据落盘：测试进程 stdout 只进 logcat，写应用私有目录才能被 host 侧稳定取回
        // （`adb shell run-as com.pdig.app cat files/a11y_semantics_evidence.txt`）。
        runCatching {
            val dir = androidx.test.platform.app.InstrumentationRegistry
                .getInstrumentation().targetContext.filesDir
            java.io.File(dir, A11Y_EVIDENCE_FILE).appendText(line + "\n")
        }
    }

    private companion object {
        const val A11Y_EVIDENCE_FILE = "a11y_semantics_evidence.txt"
    }

    @Test
    fun lockScreen_interactiveElementsAreLabeled() {
        auditScreen("LockScreen/notConfigured") {
            LockScreen(
                capability = LockCapability(
                    biometricUsable = false,
                    credentialUsable = false,
                    hardwareMissing = false,
                ),
                onUnlocked = {},
                onRecheck = {},
            )
        }
    }

    @Test
    fun lockScreen_lockedBranchIsLabeled() {
        auditScreen("LockScreen/locked") {
            LockScreen(
                capability = LockCapability(
                    biometricUsable = true,
                    credentialUsable = true,
                    hardwareMissing = false,
                ),
                onUnlocked = {},
                onRecheck = {},
            )
        }
    }

    @Test
    fun homeScreen_interactiveElementsAreLabeled() {
        // Home 的卡片数据来自 `Dispatchers.IO`（D-12 修复后先渲染加载态），
        // 必须等真实内容出现再扫描，否则门禁只会看到加载态。
        auditScreen("Home", awaitText = "共 ") { HomeScreen(rememberNavController()) }
    }

    @Test
    fun settingsScreen_interactiveElementsAreLabeled() {
        auditScreen("Settings") { SettingsScreen(rememberNavController()) }
    }

    @Test
    fun sourcesScreen_interactiveElementsAreLabeled() {
        auditScreen("Sources") { SourceManagementScreen(rememberNavController()) }
    }

    @Test
    fun importScreen_interactiveElementsAreLabeled() {
        auditScreen("Import") { ImportScreen(rememberNavController()) }
    }

    @Test
    fun backupScreen_interactiveElementsAreLabeled() {
        auditScreen("Backup") { BackupScreen(rememberNavController()) }
    }

    @Test
    fun restoreScreen_interactiveElementsAreLabeled() {
        auditScreen("Restore") { RestoreScreen(rememberNavController()) }
    }

    @Test
    fun scenarioCenter_interactiveElementsAreLabeled() {
        auditScreen("ScenarioCenter") { ScenarioCenterScreen(rememberNavController()) }
    }

    @Test
    fun scenarioSetup_interactiveElementsAreLabeled() {
        auditScreen("ScenarioSetup") { ScenarioSetupScreen(rememberNavController(), "replace_payment_card") }
    }

    @Test
    fun infrastructureScreen_interactiveElementsAreLabeled() {
        auditScreen("Infrastructure") { InfrastructureScreen(rememberNavController()) }
    }

    @Test
    fun driftScreen_interactiveElementsAreLabeled() {
        auditScreen("Drift") { RealityDriftScreen(rememberNavController()) }
    }

    @Test
    fun privacyScreen_interactiveElementsAreLabeled() {
        auditScreen("Privacy") { PrivacyScreen(rememberNavController()) }
    }

    @Test
    fun aboutScreen_interactiveElementsAreLabeled() {
        auditScreen("About") { AboutScreen(rememberNavController()) }
    }
}
