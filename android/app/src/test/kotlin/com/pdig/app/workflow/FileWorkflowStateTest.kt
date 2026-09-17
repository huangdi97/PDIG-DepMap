package com.pdig.app.workflow

import com.pdig.core.sources.MappingColumns
import com.pdig.core.sources.MappingOptions
import com.pdig.core.sources.MappingProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * D-16 工作流状态机的**纯 JVM** 测试。
 *
 * 这些断言可以在 `:app:testDebugUnitTest` 里不依赖设备地执行 ——
 * 在此之前 `app` 模块的 JVM 单测是 NO-SOURCE（真实欠账）。
 *
 * 覆盖的是"跨锁存活"这条链上**唯一可以纯逻辑判定**的部分：
 * 状态迁移本身。真正的 ActivityResult 投递与 URI 可读性由设备内
 * `FileWorkflowD16Test` 与 E2E 承担。
 */
class FileWorkflowStateTest {

    private val mapping = MappingProfile(
        columns = MappingColumns(dateTime = "交易时间", amount = "金额"),
        options = MappingOptions(delimiter = ","),
    )

    private fun importWorkflow() = FileWorkflowReducer.begin(
        purpose = FileWorkflowPurpose.IMPORT,
        resumeRoute = "import",
        sourceId = "src-1",
        sourceLabel = "招商银行信用卡",
    )

    // ------------------------------------------------------------------
    // 1. 拉起 picker → 收到结果 → 用户重新认证后消费
    // ------------------------------------------------------------------

    @Test
    fun launchingTheExternalPickerMarksTheWorkflowAsAwaitingResult() {
        val launched = FileWorkflowReducer.markLaunched(importWorkflow())

        assertEquals(FileWorkflowStep.AWAITING_PICKER, launched.step)
        assertTrue(launched.launchedExternalPicker)
        assertTrue(launched.awaitingResult)
        assertNull(launched.pendingUri)
    }

    @Test
    fun deliveringAUriWhileLockedStoresItWithoutUnlockingAnything() {
        // 关键：这一步**只登记结果**。状态机里不存在"收到文件 = 解锁"这条边。
        val received = FileWorkflowReducer.onResult(
            FileWorkflowReducer.markLaunched(importWorkflow()),
            "content://com.android.providers.downloads.documents/document/42",
        )

        assertEquals(FileWorkflowStep.FILE_RECEIVED, received.step)
        assertTrue(received.hasUndeliveredFile)
        assertEquals("content://com.android.providers.downloads.documents/document/42", received.pendingUri)
        assertFalse(received.awaitingResult)
    }

    @Test
    fun cancellingThePickerReturnsToIdleWithoutAFile() {
        val cancelled = FileWorkflowReducer.onResult(
            FileWorkflowReducer.markLaunched(importWorkflow()),
            null,
        )

        assertEquals(FileWorkflowStep.IDLE, cancelled.step)
        assertNull(cancelled.pendingUri)
        assertFalse(cancelled.awaitingResult)
        // 来源选择仍然保留，用户不需要重选来源
        assertEquals("src-1", cancelled.requestedSourceId)
        assertEquals("招商银行信用卡", cancelled.requestedSourceLabel)
    }

    @Test
    fun theWholeRoundTripKeepsTheResumeRouteIntact() {
        val w = FileWorkflowReducer.onResult(
            FileWorkflowReducer.markLaunched(importWorkflow()),
            "content://x/1",
        )
        val reviewed = FileWorkflowReducer.markReview(w, mapping)

        assertEquals("import", reviewed.resumeRoute)
        assertEquals(FileWorkflowPurpose.IMPORT, reviewed.purpose)
        assertEquals(mapping, reviewed.mappingProfile)
    }

    // ------------------------------------------------------------------
    // 2. 进程死亡（CASE B）：保守恢复，绝不半恢复
    // ------------------------------------------------------------------

    @Test
    fun processDeathInvalidatesTheFileButKeepsTheIntent() {
        val interrupted = FileWorkflowReducer.afterProcessDeath(
            FileWorkflowReducer.onResult(
                FileWorkflowReducer.markLaunched(importWorkflow()),
                "content://x/1",
            ),
        )

        assertEquals(FileWorkflowStep.INTERRUPTED, interrupted.step)
        // ⚠ 核心断言：文件结果作废。UI 必须重新要求用户选择文件，
        // 绝不允许"看起来恢复了、实际上什么都没做"。
        assertNull(interrupted.pendingUri)
        assertFalse(interrupted.hasUndeliveredFile)
        // 意图保留：purpose / 来源 / 路由
        assertEquals(FileWorkflowPurpose.IMPORT, interrupted.purpose)
        assertEquals("src-1", interrupted.requestedSourceId)
        assertEquals("import", interrupted.resumeRoute)
    }

    @Test
    fun anInterruptedWorkflowCanBeRestartedByPickingAgain() {
        val interrupted = FileWorkflowReducer.afterProcessDeath(importWorkflow())
        val relaunched = FileWorkflowReducer.markLaunched(interrupted)

        assertEquals(FileWorkflowStep.AWAITING_PICKER, relaunched.step)
        assertNull(relaunched.pendingUri)
    }

    // ------------------------------------------------------------------
    // 3. 完成与放弃
    // ------------------------------------------------------------------

    @Test
    fun doneClearsThePendingUriSoItIsNeverConsumedTwice() {
        val done = FileWorkflowReducer.markDone(
            FileWorkflowReducer.onResult(importWorkflow(), "content://x/1"),
        )

        assertEquals(FileWorkflowStep.DONE, done.step)
        assertNull(done.pendingUri)
        assertFalse(done.hasUndeliveredFile)
    }

    @Test
    fun abandonResetsToFileFreeState() {
        val abandoned = FileWorkflowReducer.abandon(
            FileWorkflowReducer.markReview(
                FileWorkflowReducer.onResult(importWorkflow(), "content://x/1"),
                mapping,
            ),
        )

        assertEquals(FileWorkflowStep.IDLE, abandoned.step)
        assertNull(abandoned.pendingUri)
        assertNull(abandoned.mappingProfile)
    }

    // ------------------------------------------------------------------
    // 4. Restore 的 purpose 隔离
    // ------------------------------------------------------------------

    @Test
    fun restoreWorkflowDoesNotLeakIntoImportAndViceVersa() {
        val restore = FileWorkflowReducer.begin(
            purpose = FileWorkflowPurpose.RESTORE,
            resumeRoute = "restore",
        )
        assertEquals(FileWorkflowPurpose.RESTORE, restore.purpose)
        assertNull(restore.requestedSourceId)

        val import = importWorkflow()
        assertEquals(FileWorkflowPurpose.IMPORT, import.purpose)
    }
}
