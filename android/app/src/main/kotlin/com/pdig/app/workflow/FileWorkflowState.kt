package com.pdig.app.workflow

import com.pdig.core.sources.MappingProfile

/**
 * 外部文件选择流程（Import / Restore）的**工作流状态模型**。
 *
 * ## 这是什么
 *
 * 它是 **application / presentation workflow state**，**不是** domain state：
 * - 不进 Reality Graph、不 bump graphRevision、不落库；
 * - 只在"用户正在走一个需要拉起外部文件选择器的向导"期间存在。
 *
 * ## 为什么必须有它（D-16 的根因）
 *
 * 外部文件选择器（DocumentsUI）是一个**独立任务**。拉起它会导致
 * `MainActivity.onStop` → `LockGate.lockNow()` → NavHost 离开组合树。
 * 在此之前，向导状态全部是页面级 `remember { mutableStateOf(...) }`，
 * 且 `ActivityResult` launcher 用 `rememberLauncherForActivityResult` 注册 ——
 * **两者都随组合树的销毁一起消失**：
 *
 * - `remember` 状态被释放（选中的来源、当前步骤、已解析的预览）
 * - `rememberLauncherForActivityResult` 在 `onDispose` 里 `unregister()`，
 *   于是 ActivityResultRegistry 里那条"待投递"记录被**主动注销**
 *
 * 结果：用户选完文件、重新认证回来，看到的是首页，导入根本没有发生。
 *
 * 本模型把"恢复向导所需的最小状态"显式化，使其可以活到 Activity 作用域
 * （见 [FileWorkflowCoordinator]），从而不再依赖组合树的存活。
 *
 * ## 隐私边界（硬性）
 *
 * 允许进本模型的：
 * - purpose / 步骤 / 路由 / 来源 id 与名称 / 用户选择的 Uri 字符串 / 映射配置
 *
 * **绝不允许**被持久化（SavedStateHandle / Bundle）的：
 * - raw CSV rows、完整 statement、解密后的内容、口令
 *
 * `mappingProfile` 只是**列名映射**（字段名，不是数据），且它由文件内容在解析时
 * 重新推导，不会被写入 SavedStateHandle。
 */
enum class FileWorkflowPurpose {
    IMPORT,
    RESTORE,
}

/**
 * 向导步骤。
 *
 * 状态机：
 * ```
 *            begin()                launchPicker()          onPickerResult(uri)
 *   IDLE ───────────► IDLE ─────────────────────► AWAITING_PICKER ──────────────────► FILE_RECEIVED
 *                                                      │  onPickerResult(null)              │ 用户重新认证后解析
 *                                                      └────────────► IDLE                  ▼
 *                                                                                        REVIEW ──► DONE
 *  任意步骤 ──进程死亡──► INTERRUPTED（Uri 不再被信任，要求重新选择）
 * ```
 */
enum class FileWorkflowStep {
    /** 空闲／未开始。 */
    IDLE,

    /** 已拉起外部 picker，正在等 ActivityResult。**此时 App 一定会被回锁。** */
    AWAITING_PICKER,

    /** 已收到 Uri，但用户还没重新认证 —— **不允许在此状态下读取或提交文件内容**。 */
    FILE_RECEIVED,

    /** Node Resolution 预览已就绪，等用户确认（Import）。 */
    REVIEW,

    /** 流程完成。 */
    DONE,

    /**
     * 进程死亡后恢复：工作流意图仍在，**但文件结果作废**。
     *
     * 这是刻意选择的保守策略（任务要求 CASE B"不允许半提交/半恢复"）：
     * 不做"看起来恢复了、其实没恢复"的假象，而是明确告诉用户重新选一次文件。
     */
    INTERRUPTED,
}

data class PendingFileWorkflow(
    val purpose: FileWorkflowPurpose,
    /** 解锁后要回到的页面路由。 */
    val resumeRoute: String,
    val step: FileWorkflowStep = FileWorkflowStep.IDLE,
    /** Import：用户选中的 SourceInstance id（新建来源时为 null）。 */
    val requestedSourceId: String? = null,
    /** Import：来源名称（已有来源的 label，或用户新建的名称）。 */
    val requestedSourceLabel: String? = null,
    /** 外部 picker 返回的 Uri（字符串形式，便于纯 JVM 推理与测试）。 */
    val pendingUri: String? = null,
    /** 已拉起外部 picker 且结果尚未投递。 */
    val launchedExternalPicker: Boolean = false,
    /** 列名映射（仅字段名，不含数据）。解析时重新推导，不持久化。 */
    val mappingProfile: MappingProfile? = null,
) {
    /** 是否正在等待外部 picker 的 ActivityResult。 */
    val awaitingResult: Boolean
        get() = step == FileWorkflowStep.AWAITING_PICKER

    /** 是否已经拿到了文件、只差用户重新认证。 */
    val hasUndeliveredFile: Boolean
        get() = step == FileWorkflowStep.FILE_RECEIVED && pendingUri != null
}

/**
 * [PendingFileWorkflow] 的**纯状态迁移**。
 *
 * 抽成纯函数的原因：本文件不依赖 Android，所以这些迁移可以在 `:app:testDebugUnitTest`
 * 里被真实执行（此前 `app` 模块的 JVM 单测是 NO-SOURCE），而不需要设备。
 */
object FileWorkflowReducer {

    fun begin(
        purpose: FileWorkflowPurpose,
        resumeRoute: String,
        sourceId: String? = null,
        sourceLabel: String? = null,
    ): PendingFileWorkflow = PendingFileWorkflow(
        purpose = purpose,
        resumeRoute = resumeRoute,
        step = FileWorkflowStep.IDLE,
        requestedSourceId = sourceId,
        requestedSourceLabel = sourceLabel,
        pendingUri = null,
        launchedExternalPicker = false,
        mappingProfile = null,
    )

    /** 拉起外部 picker。此刻必须假定 App 马上会进入 LOCKED。 */
    fun markLaunched(workflow: PendingFileWorkflow): PendingFileWorkflow = workflow.copy(
        step = FileWorkflowStep.AWAITING_PICKER,
        launchedExternalPicker = true,
        pendingUri = null,
    )

    /**
     * ActivityResult 回调。
     *
     * **关键不变量**：这个方法**只登记结果，绝不解锁**。
     * 解锁只能发生在用户完成身份验证之后（见 `LockGate.unlock`）。
     */
    fun onResult(workflow: PendingFileWorkflow, uri: String?): PendingFileWorkflow = when (uri) {
        null -> workflow.copy(
            step = FileWorkflowStep.IDLE,
            launchedExternalPicker = false,
            pendingUri = null,
        )
        else -> workflow.copy(
            step = FileWorkflowStep.FILE_RECEIVED,
            launchedExternalPicker = false,
            pendingUri = uri,
        )
    }

    /** 解析完成、Node Resolution 预览就绪。 */
    fun markReview(
        workflow: PendingFileWorkflow,
        mappingProfile: MappingProfile?,
    ): PendingFileWorkflow = workflow.copy(
        step = FileWorkflowStep.REVIEW,
        mappingProfile = mappingProfile,
    )

    fun markDone(workflow: PendingFileWorkflow): PendingFileWorkflow = workflow.copy(
        step = FileWorkflowStep.DONE,
        pendingUri = null,
        launchedExternalPicker = false,
    )

    /**
     * 进程死亡后恢复：保留意图（purpose / 来源 / 路由），**作废文件结果**。
     * 不允许在没有用户重新选择文件的情况下继续。
     */
    fun afterProcessDeath(workflow: PendingFileWorkflow): PendingFileWorkflow = workflow.copy(
        step = FileWorkflowStep.INTERRUPTED,
        pendingUri = null,
        launchedExternalPicker = false,
        mappingProfile = null,
    )

    /** 用户主动放弃／流程被重置。 */
    fun abandon(workflow: PendingFileWorkflow): PendingFileWorkflow = workflow.copy(
        step = FileWorkflowStep.IDLE,
        pendingUri = null,
        launchedExternalPicker = false,
        mappingProfile = null,
    )
}
