package com.pdig.app.workflow

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdig.app.data.AppContainer.ImportCommitResult
import com.pdig.app.data.AppContainer.ImportPreview
import com.pdig.core.sources.MappingProfile
import kotlinx.coroutines.launch

/**
 * Import / Restore 的 **Activity 作用域工作流协调器**（D-16 方案 A）。
 *
 * ## 它解决什么
 *
 * 外部文件选择器（DocumentsUI）是独立任务，拉起它会导致
 * `MainActivity.onStop` → `LockGate.lockNow()` → NavHost 离开组合树。
 * 页面级 `remember {}` 与 `rememberLauncherForActivityResult` 都会随之消失
 * （后者在 `onDispose` 里主动 `unregister()`，把待投递结果一起注销）。
 *
 * 因此这里把两件事搬到**比组合树更长寿**的作用域：
 *
 * 1. **工作流状态** —— 本 ViewModel（Activity 作用域，由 `MainActivity` 创建）
 * 2. **ActivityResult launcher** —— 注册在 `MainActivity.onCreate`，
 *    生命周期与 Activity 相同，**不随 NavHost 的 uncompose 被注销**
 *
 * ## 明确不做什么
 *
 * - **不碰 Reality Graph**：这是 application/presentation workflow state，
 *   不是 domain state。不落库、不 bump revision。
 * - **不解锁**：`onPickerResult` 只登记结果。`LockGate` 仍然只能由
 *   身份验证成功（或无凭据设备上的用户显式确认）打开。
 *   「拿到文件 → 未验证身份 → 后台自动 commit/restore」在结构上不存在。
 * - **不持久化敏感内容**：见 [SAVED_KEYS] 与 `persist()`。
 *
 * ## 权限最小化
 *
 * 使用 `ACTION_OPEN_DOCUMENT`（`ActivityResultContracts.OpenDocument`）而不是
 * `ACTION_GET_CONTENT`：只有前者会给出**可持久化**的 URI grant，
 * 业务需要在"用户重新认证之后"再读取这个文件。
 * 只申请 `FLAG_GRANT_READ_URI_PERMISSION`（只读），并在流程结束/放弃时
 * `releasePersistableUriPermission` 归还，不无理由长期持有。
 */
class FileWorkflowCoordinator(
    private val savedState: SavedStateHandle? = null,
) : ViewModel() {

    // ------------------------------------------------------------------
    // 状态
    // ------------------------------------------------------------------

    /** 当前工作流（含"等待外部 picker 结果"这一跨锁状态）。 */
    var workflow: PendingFileWorkflow? by mutableStateOf(null)
        private set

    /**
     * Node Resolution 预览（含 Observation）。
     *
     * **仅内存**：绝不会进入 SavedStateHandle / Bundle（隐私边界）。
     * 它之所以放在这里而不是页面级 `remember`，是因为用户可能在
     * 「预览已出现、还没点确认」时把 App 切到后台 —— 那时同样会回锁。
     */
    var importPreview: ImportPreview? by mutableStateOf(null)
        private set

    /** 导入提交结果（完成态面板）。仅内存。 */
    var importResult: ImportCommitResult? by mutableStateOf(null)
        private set

    /** 恢复结果的可展示信息。仅内存。 */
    var restoreMessage: String? by mutableStateOf(null)
        private set

    /**
     * 恢复流程中已选中的 .depmap 文件名（**仅展示名，不是文件内容**）。
     *
     * 放在这里而不是页面级 `remember`：Restore 的中间态是"文件已选、口令待输入"，
     * 用户完全可能在这个状态下切后台 → 回锁 → 解锁。
     */
    var restoreFileName: String? by mutableStateOf(null)

    /** 面向用户的工作流提示（例如"请重新选择文件"）。 */
    var statusText: String? by mutableStateOf(null)

    /** 是否有后台工作在进行（解析/提交/恢复）。 */
    var busy: Boolean by mutableStateOf(false)

    // ------------------------------------------------------------------
    // launcher（由 MainActivity 注入的**稳定** ActivityResult 入口）
    // ------------------------------------------------------------------

    private var launcher: ((String) -> Unit)? = null

    /** launcher 是否已挂载。锁定时它必须**仍然**为 true —— 这是 D-16 的核心断言。 */
    val launcherAttached: Boolean get() = launcher != null

    /** `MainActivity` 在 `onCreate` 里注册完 ActivityResult 后调用。 */
    fun attachLauncher(block: (String) -> Unit) {
        launcher = block
    }

    fun detachLauncher() {
        launcher = null
    }

    /**
     * 拉起外部文件选择器。
     *
     * @return true 表示已成功拉起；false 表示 launcher 未挂载
     *         （正常 App 流程下不会发生，用于让测试把这个不变量变成断言）。
     */
    fun launchPicker(mime: String = "*/*"): Boolean {
        val block = launcher ?: return false
        val current = workflow ?: return false
        update(FileWorkflowReducer.markLaunched(current))
        block(mime)
        return true
    }

    // ------------------------------------------------------------------
    // 流程入口
    // ------------------------------------------------------------------

    fun beginImport(resumeRoute: String, sourceId: String?, sourceLabel: String?) {
        resetSideEffects()
        update(
            FileWorkflowReducer.begin(
                purpose = FileWorkflowPurpose.IMPORT,
                resumeRoute = resumeRoute,
                sourceId = sourceId,
                sourceLabel = sourceLabel,
            ),
        )
    }

    fun beginRestore(resumeRoute: String) {
        resetSideEffects()
        update(
            FileWorkflowReducer.begin(
                purpose = FileWorkflowPurpose.RESTORE,
                resumeRoute = resumeRoute,
            ),
        )
    }

    // ------------------------------------------------------------------
    // ActivityResult 回调（**只登记结果，绝不解锁**）
    // ------------------------------------------------------------------

    /**
     * 由 `MainActivity` 注册的 launcher 调用。
     *
     * 不变量：
     *  - 此方法**不会**调用 `LockGate.unlock()`
     *  - 此方法**不会**读取文件内容、不会 commit、不会 restore
     */
    fun onPickerResult(context: Context, uri: Uri?) {
        appContext = context.applicationContext
        val current = workflow
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

    /**
     * 用户**重新认证之后**，由页面取回待处理的文件。
     *
     * 这是**消费型**取回（consume）：取到即把 `pendingUri` 清空，
     * 保证同一个 ActivityResult 只会被投递一次 —— 不会因为页面重组
     * （例如解锁后再次切后台再解锁）而重复解析、重复提交。
     *
     * @return 待处理的 Uri；若当前没有待处理文件（或不是该 purpose）则返回 null。
     */
    fun consumePendingUri(purpose: FileWorkflowPurpose): Uri? {
        val current = workflow ?: return null
        if (current.purpose != purpose) return null
        if (current.step != FileWorkflowStep.FILE_RECEIVED) return null
        val raw = current.pendingUri ?: return null
        update(current.copy(pendingUri = null))
        return Uri.parse(raw)
    }

    /**
     * **非消费型**读取 Restore 待处理文件。
     *
     * 与 Import 不同，Restore 的中间态是"文件已选、只等用户输入口令并确认"，
     * 因此在恢复真正结束之前这个 Uri 必须一直可被读到 —— 用户可能在
     * 选完文件之后又切了一次后台（再次回锁），那时页面级 `remember` 已经没了。
     */
    fun pendingRestoreUri(): Uri? {
        val current = workflow ?: return null
        if (current.purpose != FileWorkflowPurpose.RESTORE) return null
        val raw = current.pendingUri ?: return null
        return Uri.parse(raw)
    }

    fun markReview(mappingProfile: MappingProfile?) {
        val current = workflow ?: return
        update(FileWorkflowReducer.markReview(current, mappingProfile))
    }

    fun publishImportPreview(preview: ImportPreview?) {
        importPreview = preview
    }

    fun publishImportResult(result: ImportCommitResult?) {
        importResult = result
        if (result != null) {
            val current = workflow
            if (current != null) update(FileWorkflowReducer.markDone(current))
        }
    }

    /**
     * 恢复**成功**：展示结果并结束工作流（归还 URI 权限）。
     *
     * 只有这里才会让工作流进入 DONE —— UI 说成功与 Reality 真的被改变
     * 必须来自同一次调用，不允许出现"UI 说成功、实际没恢复"。
     */
    fun completeRestore(message: String) {
        restoreMessage = message
        releaseHeldUri()
        val current = workflow
        if (current != null) update(FileWorkflowReducer.markDone(current))
    }

    /**
     * 恢复**失败**（口令错误 / 文件损坏 / 版本不支持）：
     * 只展示错误，**保留已选文件**，让用户可以改正口令后重试，而不用重选。
     * 失败不会推进工作流状态，因此不存在半恢复。
     */
    fun failRestore(message: String) {
        restoreMessage = message
    }

    /** 清除上一次的恢复结果提示（例如用户改了口令准备重试）。 */
    fun clearRestoreMessage() {
        restoreMessage = null
    }

    /** 放弃当前工作流（用户离开页面 / 取消），并归还 URI 权限。 */
    fun clear() {
        releaseHeldUri()
        workflow?.let { update(FileWorkflowReducer.abandon(it)) }
        workflow = null
        savedState?.let { handle ->
            SAVED_KEYS.forEach { handle.remove<String>(it) }
            handle.remove<String>(KEY_URI_FOR_RELEASE)
        }
        importPreview = null
        importResult = null
        restoreMessage = null
        restoreFileName = null
        statusText = null
        busy = false
    }

    // ------------------------------------------------------------------
    // 内部：状态落盘（只落非敏感 metadata）与 URI 权限回收
    // ------------------------------------------------------------------

    private var appContext: Context? = null
    private var heldUri: Uri? = null

    init {
        restoreFromSavedState()
    }

    private fun update(next: PendingFileWorkflow) {
        workflow = next
        persist(next)
    }

    private fun persist(workflow: PendingFileWorkflow) {
        val handle = savedState ?: return
        handle[KEY_PURPOSE] = workflow.purpose.name
        handle[KEY_RESUME_ROUTE] = workflow.resumeRoute
        handle[KEY_SOURCE_ID] = workflow.requestedSourceId
        handle[KEY_SOURCE_LABEL] = workflow.requestedSourceLabel
        // ⚠ 这里**只**保存 Uri 字符串，唯一目的是进程重建后能把 grant 还回去
        // （见 releaseStaleGrant）。它**不会**被用来自动恢复流程 —— 恢复时
        // 步骤一律是 INTERRUPTED，必须重新选文件。
        handle[KEY_URI_FOR_RELEASE] = workflow.pendingUri
    }

    private fun restoreFromSavedState() {
        val handle = savedState ?: return
        val purpose = handle.get<String>(KEY_PURPOSE) ?: return
        val parsed = runCatching { FileWorkflowPurpose.valueOf(purpose) }.getOrNull() ?: return
        val route = handle.get<String>(KEY_RESUME_ROUTE) ?: return
        // CASE B（进程死亡）：意图保留，文件结果作废 —— 明确要求重新选择文件，
        // 绝不出现"UI 说成功、Reality 实际没变"的半恢复状态。
        workflow = PendingFileWorkflow(
            purpose = parsed,
            resumeRoute = route,
            step = FileWorkflowStep.INTERRUPTED,
            requestedSourceId = handle.get<String>(KEY_SOURCE_ID),
            requestedSourceLabel = handle.get<String>(KEY_SOURCE_LABEL),
            pendingUri = null,
            launchedExternalPicker = false,
            mappingProfile = null,
        )
    }

    /**
     * 进程重建后归还上一次可能残留的可持久化 URI grant。
     *
     * 只在 `MainActivity.onCreate` 调一次：既然我们不跨进程死亡恢复文件，
     * 就没有理由继续持有这个读权限。
     */
    fun releaseStaleGrant(context: Context) {
        appContext = context.applicationContext
        val raw = savedState?.get<String>(KEY_URI_FOR_RELEASE) ?: return
        val uri = runCatching { Uri.parse(raw) }.getOrNull() ?: return
        runCatching {
            context.contentResolver.releasePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        savedState?.remove<String>(KEY_URI_FOR_RELEASE)
    }

    private fun releaseHeldUri() {
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

    private fun resetSideEffects() {
        releaseHeldUri()
        importPreview = null
        importResult = null
        restoreMessage = null
        restoreFileName = null
        statusText = null
        busy = false
    }

    override fun onCleared() {
        releaseHeldUri()
        super.onCleared()
    }

    /** 供测试/页面在协程里跑后台工作时使用的 scope。 */
    fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    companion object {
        const val KEY_PURPOSE = "fw.purpose"
        const val KEY_RESUME_ROUTE = "fw.resumeRoute"
        const val KEY_SOURCE_ID = "fw.sourceId"
        const val KEY_SOURCE_LABEL = "fw.sourceLabel"
        const val KEY_URI_FOR_RELEASE = "fw.uriForRelease"

        private val SAVED_KEYS = listOf(
            KEY_PURPOSE,
            KEY_RESUME_ROUTE,
            KEY_SOURCE_ID,
            KEY_SOURCE_LABEL,
        )
    }
}
