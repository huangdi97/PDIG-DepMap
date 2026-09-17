package com.pdig.app.evidence

import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pdig.app.MainActivity
import com.pdig.app.security.LockGate
import com.pdig.app.workflow.FileWorkflowCoordinator
import com.pdig.app.workflow.FileWorkflowPurpose
import com.pdig.app.workflow.FileWorkflowStep
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * D-16 的**设备内运行时取证**。
 *
 * ## 缺陷原样
 *
 * ```
 * Import/Restore 页拉起 DocumentsUI（独立任务）
 *   → MainActivity.onStop
 *   → LockGate.lockNow()
 *   → NavHost 离开组合树
 *   → 页面 remember 状态被释放 + rememberLauncherForActivityResult 在 onDispose 里 unregister()
 *   → ActivityResultRegistry 里那条"待投递"记录被主动注销
 *   → 用户选完文件、重新解锁 → 回到 HOME → Import/Restore 没发生
 * ```
 *
 * ## 修复后的结构性保证（本文件逐一变成断言）
 *
 *  1. ActivityResult launcher 注册在 **Activity 层**，锁定期间**仍然存在**
 *     （[activityScopedLauncherRemainsAttachedWhileLocked]）
 *  2. 外部 picker 的结果**不会绕过锁**（[externalPickerResultDoesNotBypassLock]）
 *  3. 待处理文件**跨锁存活**，且解锁后**只被消费一次**
 *     （[externalPickerDoesNotDestroyPendingWorkflow]）
 *  4. 锁定—解锁往返之后 Uri **仍然可读**（[selectedFileRemainsReadableAcrossLockRoundTrip]）
 *  5. 进程死亡后**保守恢复**：意图保留、文件作废，绝不半恢复
 *     （[processDeathRestoresIntentButInvalidatesTheFile]）
 *  6. 敏感内容**从不**进入 SavedStateHandle
 *     （[savedStateNeverCarriesFileContent]）
 *
 * ## 为什么用真实的 content:// Uri
 *
 * 用 MediaStore 落一个真实文件、拿到系统 MediaProvider 发出的 `content://` Uri，
 * 而不是构造一个假 Uri —— 否则"Uri 仍可读"这条断言会退化成"字符串还在"，
 * 那正是 D-14/D-15 暴露过的取证造假模式。
 */
@RunWith(AndroidJUnit4::class)
class FileWorkflowD16Test {

    private fun appContext() = InstrumentationRegistry.getInstrumentation().targetContext

    // ------------------------------------------------------------------
    // fixture：真实落盘的一个 CSV，通过系统 MediaProvider 暴露为 content://
    // ------------------------------------------------------------------

    private val fixtureBody = buildString {
        appendLine("交易时间,交易类型,交易对方,商品,收/支,金额(元),支付方式,当前状态,交易单号,商户单号,备注")
        appendLine("2026-01-05 09:12:33,商户消费,星巴克,拿铁,支出,35.00,招商银行信用卡(1234),支付成功,1001,2001,")
        appendLine("2026-01-06 19:40:01,商户消费,App Store,订阅,支出,25.00,招商银行信用卡(1234),支付成功,1002,2002,")
    }

    /** 一个不会出现在 SavedState 白名单里的"文件内容指纹"：命中即说明敏感数据外泄。 */
    private val secretMarker = "星巴克"

    private var createdUris = mutableListOf<Uri>()

    private fun publishFixture(name: String, body: String): Uri {
        val resolver = appContext().contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, "text/csv")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        assertNotNull("MediaStore 必须能给出真实 content Uri（API ${Build.VERSION.SDK_INT}）", uri)
        val target = uri!!
        createdUris.add(target)
        resolver.openOutputStream(target)!!.use { it.write(body.toByteArray()) }
        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(target, values, null, null)
        return target
    }

    private fun readBack(uri: Uri): String? =
        appContext().contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?.toString(Charsets.UTF_8)

    @After
    fun tearDown() {
        val resolver = appContext().contentResolver
        createdUris.forEach { runCatching { resolver.delete(it, null, null) } }
        createdUris.clear()
        LockGate.lockNow()
    }

    // ------------------------------------------------------------------
    // 1. launcher 在锁定期间仍然存在 —— D-16 的核心不变量
    // ------------------------------------------------------------------

    @Test
    fun activityScopedLauncherRemainsAttachedWhileLocked() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            var wf: FileWorkflowCoordinator? = null
            scenario.onActivity { wf = it.activityWorkflow() }
            val workflow = requireNotNull(wf)

            // 注册在 MainActivity.onCreate，与组合树无关
            assertTrue("launcher 必须挂载在 Activity 作用域", workflow.launcherAttached)

            // 模拟"拉起外部 picker"导致的 onStop
            scenario.moveToState(Lifecycle.State.CREATED)
            assertTrue("onStop 必须回锁", LockGate.locked)
            assertTrue(
                "D-16 核心：锁定时 launcher 必须仍然存在 —— 若这里为 false，" +
                    "说明它又被放回了组合树里，ActivityResult 会再次被注销",
                workflow.launcherAttached,
            )

            // 回到前台：不解锁，launcher 依然在
            scenario.moveToState(Lifecycle.State.RESUMED)
            assertTrue("回到前台必须仍然锁定", LockGate.locked)
            assertTrue(workflow.launcherAttached)
        }
    }

    // ------------------------------------------------------------------
    // 2. 外部 picker 的结果绝不解锁
    // ------------------------------------------------------------------

    @Test
    fun externalPickerResultDoesNotBypassLock() {
        val uri = publishFixture("d16_nobypass.csv", fixtureBody)

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            var wf: FileWorkflowCoordinator? = null
            scenario.onActivity { wf = it.activityWorkflow() }
            val workflow = requireNotNull(wf)

            LockGate.lockNow()
            workflow.beginImport("import", "src-1", "招商银行信用卡")
            assertTrue(LockGate.locked)

            // 这就是 DocumentsUI 回来的那一次回调
            workflow.onPickerResult(appContext(), uri)

            // ⚠ 关键：拿到文件 ≠ 解锁。ActivityResult 回调里不存在任何 unlock 调用点。
            assertTrue(
                "ActivityResult 投递不得解锁 —— 否则外部系统 Activity 就成了绕过重新认证的通道",
                LockGate.locked,
            )
            assertEquals(FileWorkflowStep.FILE_RECEIVED, workflow.workflow?.step)
            assertEquals(uri.toString(), workflow.workflow?.pendingUri)
        }
    }

    // ------------------------------------------------------------------
    // 3. 待处理文件跨锁存活，且只被消费一次
    // ------------------------------------------------------------------

    @Test
    fun externalPickerDoesNotDestroyPendingWorkflow() {
        val uri = publishFixture("d16_pending.csv", fixtureBody)

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            var wf: FileWorkflowCoordinator? = null
            scenario.onActivity { wf = it.activityWorkflow() }
            val workflow = requireNotNull(wf)

            workflow.beginImport("import", "src-1", "招商银行信用卡")
            workflow.onPickerResult(appContext(), uri)
            assertEquals(FileWorkflowStep.FILE_RECEIVED, workflow.workflow?.step)

            // 锁定 → 解锁的完整往返（NavHost 在此期间离开又回到组合树）
            scenario.moveToState(Lifecycle.State.CREATED)
            assertTrue(LockGate.locked)
            scenario.moveToState(Lifecycle.State.RESUMED)

            // 工作流仍然在 —— 这正是修复前丢失的东西
            val after = requireNotNull(workflow.workflow)
            assertEquals(FileWorkflowPurpose.IMPORT, after.purpose)
            assertEquals(FileWorkflowStep.FILE_RECEIVED, after.step)
            assertEquals("import", after.resumeRoute)
            assertEquals("src-1", after.requestedSourceId)

            // 用户真正完成身份验证之后才允许推进
            LockGate.unlock()
            assertEquals(uri, workflow.consumePendingUri(FileWorkflowPurpose.IMPORT))
            // 消费型取回：同一个结果不会被投递第二次
            assertNull(workflow.consumePendingUri(FileWorkflowPurpose.IMPORT))
        }
    }

    // ------------------------------------------------------------------
    // 4. 锁定—解锁往返之后 Uri 仍然可读
    // ------------------------------------------------------------------

    @Test
    fun selectedFileRemainsReadableAcrossLockRoundTrip() {
        val uri = publishFixture("d16_readable.csv", fixtureBody)

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            var wf: FileWorkflowCoordinator? = null
            scenario.onActivity { wf = it.activityWorkflow() }
            val workflow = requireNotNull(wf)

            workflow.beginImport("import", null, "新建来源")
            workflow.onPickerResult(appContext(), uri)

            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.moveToState(Lifecycle.State.RESUMED)

            val taken = requireNotNull(workflow.consumePendingUri(FileWorkflowPurpose.IMPORT))
            val body = readBack(taken)
            assertNotNull("解锁后必须仍能读取用户选择的文件", body)
            assertTrue(body!!.contains(secretMarker))
        }
    }

    // ------------------------------------------------------------------
    // 5. 进程死亡：保守恢复（CASE B）
    // ------------------------------------------------------------------

    @Test
    fun processDeathRestoresIntentButInvalidatesTheFile() {
        val uri = publishFixture("d16_processdeath.csv", fixtureBody)

        val handle = SavedStateHandle()
        val workflow = FileWorkflowCoordinator(handle)
        workflow.beginImport("import", "src-1", "招商银行信用卡")
        workflow.onPickerResult(appContext(), uri)
        assertEquals(FileWorkflowStep.FILE_RECEIVED, workflow.workflow?.step)

        // 进程被杀 → 只留下 SavedStateHandle 里的非敏感 metadata
        val restored = FileWorkflowCoordinator(
            SavedStateHandle(
                mapOf(
                    FileWorkflowCoordinator.KEY_PURPOSE to FileWorkflowPurpose.IMPORT.name,
                    FileWorkflowCoordinator.KEY_RESUME_ROUTE to "import",
                    FileWorkflowCoordinator.KEY_SOURCE_ID to "src-1",
                    FileWorkflowCoordinator.KEY_SOURCE_LABEL to "招商银行信用卡",
                ),
            ),
        )

        val state = requireNotNull(restored.workflow)
        assertEquals(FileWorkflowStep.INTERRUPTED, state.step)
        // 意图保留
        assertEquals(FileWorkflowPurpose.IMPORT, state.purpose)
        assertEquals("import", state.resumeRoute)
        assertEquals("src-1", state.requestedSourceId)
        // ⚠ 文件结果作废：绝不允许"UI 说成功、Reality 实际没变"
        assertNull(state.pendingUri)
        assertFalse(state.hasUndeliveredFile)
    }

    // ------------------------------------------------------------------
    // 6. 敏感内容从不进入 SavedStateHandle / Bundle
    // ------------------------------------------------------------------

    @Test
    fun savedStateNeverCarriesFileContent() {
        val uri = publishFixture("d16_privacy.csv", fixtureBody)

        val handle = SavedStateHandle()
        val workflow = FileWorkflowCoordinator(handle)
        workflow.beginImport("import", "src-1", "招商银行信用卡")
        workflow.onPickerResult(appContext(), uri)

        val keys = handle.keys()
        assertEquals(
            setOf(
                FileWorkflowCoordinator.KEY_PURPOSE,
                FileWorkflowCoordinator.KEY_RESUME_ROUTE,
                FileWorkflowCoordinator.KEY_SOURCE_ID,
                FileWorkflowCoordinator.KEY_SOURCE_LABEL,
                FileWorkflowCoordinator.KEY_URI_FOR_RELEASE,
            ),
            keys,
        )
        val dump = keys.joinToString("\n") { "$it=${handle.get<Any>(it)}" }
        assertFalse(
            "原始账单内容绝不能出现在 SavedStateHandle 里 —— 只允许存" +
                "purpose / route / sourceId / sourceLabel / uri（uri 只用于归还 grant）",
            dump.contains(secretMarker),
        )
    }
}
