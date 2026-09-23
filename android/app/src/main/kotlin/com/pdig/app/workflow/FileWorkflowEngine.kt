package com.pdig.app.workflow

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import com.pdig.app.data.ImportCommitResult
import com.pdig.app.data.ImportPreview

/**
 * [FileWorkflowCoordinator] 的**私有状态与副作用实现**（不是公开 API）。
 *
 * 职责（全部是 coordinator 的内部机械，不对外暴露）：
 *  - 工作流与会话状态（workflow / 预览 / 结果 / 恢复信息 / busy 等）
 *  - SavedStateHandle 落盘 / 恢复 / 清理 —— 只落非敏感 metadata
 *  - 可持久化 URI 读权限的持有与归还
 *
 * coordinator 以 `by engine::xxx` 委托公开这些状态（公开 API 的属性名、签名
 * 与可见性逐字节不变）；本类存在的唯一目的是把 coordinator 文件压到 300 行
 * 以内，**不改动任何行为**。
 */
internal class FileWorkflowEngine(
    private val savedState: SavedStateHandle?,
) {

    /** 当前工作流（含"等待外部 picker 结果"这一跨锁状态）。 */
    var workflow: PendingFileWorkflow? by mutableStateOf(null)

    /** Node Resolution 预览（含 Observation）。**仅内存**，绝不进 SavedStateHandle / Bundle。 */
    var importPreview: ImportPreview? by mutableStateOf(null)

    /** 导入提交结果（完成态面板）。仅内存。 */
    var importResult: ImportCommitResult? by mutableStateOf(null)

    /** 恢复结果的可展示信息。仅内存。 */
    var restoreMessage: String? by mutableStateOf(null)

    /** 恢复流程中已选中的 .depmap 文件名（**仅展示名，不是文件内容**）。 */
    var restoreFileName: String? by mutableStateOf(null)

    /** 面向用户的工作流提示（例如"请重新选择文件"）。 */
    var statusText: String? by mutableStateOf(null)

    /** 是否有后台工作在进行（解析/提交/恢复）。 */
    var busy: Boolean by mutableStateOf(false)

    private var appContext: Context? = null
    private var heldUri: Uri? = null

    // ------------------------------------------------------------------
    // 状态迁移（唯一写入口：写 workflow 的同时落盘非敏感 metadata）
    // ------------------------------------------------------------------

    fun update(next: PendingFileWorkflow) {
        workflow = next
        persist(next)
    }

    // ------------------------------------------------------------------
    // ActivityResult 回调（**只登记结果，绝不解锁**）
    // ------------------------------------------------------------------

    /**
     * 由 `MainActivity` 注册的 launcher 调用（coordinator 的 `onPickerResult` 委托过来）。
     *
     * 不变量：
     *  - 此方法**不会**调用 `LockGate.unlock()`
     *  - 此方法**不会**读取文件内容、不会 commit、不会 restore
     */
    fun onPickerResult(context: Context, uri: Uri?, current: PendingFileWorkflow?) {
        appContext = context.applicationContext
        if (current == null || uri == null) {
            if (current != null) update(FileWorkflowReducer.onResult(current, null))
            return
        }
        // 只申请**读**权限，且只在真正需要跨"锁定—解锁"读取时才持久化。
        // ACTION_OPEN_DOCUMENT 保证 grant 可持久化；如果不是（异常路径）也不阻塞流程。
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        heldUri = uri
        update(FileWorkflowReducer.onResult(current, uri.toString()))
    }

    // ------------------------------------------------------------------
    // 内部：状态落盘（只落非敏感 metadata）与 URI 权限回收
    // ------------------------------------------------------------------

    private fun persist(workflow: PendingFileWorkflow) {
        val handle = savedState ?: return
        handle[FileWorkflowCoordinator.KEY_PURPOSE] = workflow.purpose.name
        handle[FileWorkflowCoordinator.KEY_RESUME_ROUTE] = workflow.resumeRoute
        handle[FileWorkflowCoordinator.KEY_SOURCE_ID] = workflow.requestedSourceId
        handle[FileWorkflowCoordinator.KEY_SOURCE_LABEL] = workflow.requestedSourceLabel
        // ⚠ 这里**只**保存 Uri 字符串，唯一目的是进程重建后能把 grant 还回去
        // （见 releaseStaleGrant）。它**不会**被用来自动恢复流程 —— 恢复时
        // 步骤一律是 INTERRUPTED，必须重新选文件。
        handle[FileWorkflowCoordinator.KEY_URI_FOR_RELEASE] = workflow.pendingUri
    }

    /**
     * 从 SavedStateHandle 恢复（CASE B：进程死亡）。
     *
     * 意图保留，文件结果作废 —— 明确要求重新选择文件，
     * 绝不出现"UI 说成功、Reality 实际没变"的半恢复状态。
     */
    fun restoreWorkflow(): PendingFileWorkflow? {
        val handle = savedState ?: return null
        val purpose = handle.get<String>(FileWorkflowCoordinator.KEY_PURPOSE) ?: return null
        val parsed = runCatching { FileWorkflowPurpose.valueOf(purpose) }.getOrNull() ?: return null
        val route = handle.get<String>(FileWorkflowCoordinator.KEY_RESUME_ROUTE) ?: return null
        return PendingFileWorkflow(
            purpose = parsed,
            resumeRoute = route,
            step = FileWorkflowStep.INTERRUPTED,
            requestedSourceId = handle.get<String>(FileWorkflowCoordinator.KEY_SOURCE_ID),
            requestedSourceLabel = handle.get<String>(FileWorkflowCoordinator.KEY_SOURCE_LABEL),
            pendingUri = null,
            launchedExternalPicker = false,
            mappingProfile = null,
        )
    }

    private val savedKeys = listOf(
        FileWorkflowCoordinator.KEY_PURPOSE,
        FileWorkflowCoordinator.KEY_RESUME_ROUTE,
        FileWorkflowCoordinator.KEY_SOURCE_ID,
        FileWorkflowCoordinator.KEY_SOURCE_LABEL,
    )

    private fun clearSavedState() {
        val handle = savedState ?: return
        savedKeys.forEach { handle.remove<String>(it) }
        handle.remove<String>(FileWorkflowCoordinator.KEY_URI_FOR_RELEASE)
    }

    /**
     * 进程重建后归还上一次可能残留的可持久化 URI grant。
     *
     * 只在 `MainActivity.onCreate` 调一次：既然我们不跨进程死亡恢复文件，
     * 就没有理由继续持有这个读权限。
     */
    fun releaseStaleGrant(context: Context) {
        appContext = context.applicationContext
        val raw = savedState?.get<String>(FileWorkflowCoordinator.KEY_URI_FOR_RELEASE) ?: return
        val uri = runCatching { Uri.parse(raw) }.getOrNull() ?: return
        runCatching {
            context.contentResolver.releasePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        savedState?.remove<String>(FileWorkflowCoordinator.KEY_URI_FOR_RELEASE)
    }

    fun releaseHeldUri() {
        val uri = heldUri ?: return
        val ctx = appContext
        if (ctx != null) {
            runCatching {
                ctx.contentResolver.releasePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
        }
        heldUri = null
    }

    /** 流程开始 / 放弃 / 失败重试时的副作用复位（不碰 `workflow` 本身）。 */
    fun resetSideEffects() {
        releaseHeldUri()
        importPreview = null
        importResult = null
        restoreMessage = null
        restoreFileName = null
        statusText = null
        busy = false
    }

    /** 放弃当前工作流（用户离开页面 / 取消），并归还 URI 权限。 */
    fun clear() {
        releaseHeldUri()
        workflow?.let { update(FileWorkflowReducer.abandon(it)) }
        workflow = null
        clearSavedState()
        resetSideEffects()
    }
}