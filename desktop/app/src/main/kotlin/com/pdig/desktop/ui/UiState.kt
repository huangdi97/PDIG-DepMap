package com.pdig.desktop.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pdig.app.data.ImportCommitResult
import com.pdig.app.data.ImportPreview
import com.pdig.desktop.data.DesktopSession
import com.pdig.desktop.io.DesktopFileOps
import com.pdig.desktop.security.DeviceUnlockStore
import com.pdig.core.sources.MappingProfile
import com.pdig.core.sources.Observation
import java.io.File

/**
 * 当前“打开的数据文件”上下文 + 跨屏 UI 状态。
 * 数据本身永远来自 [DesktopSession]（共享 :repos），这里只存**视图状态**。
 */
class UiState(
    session: DesktopSession,
    val unlockStore: DeviceUnlockStore,
    val fileOps: DesktopFileOps,
) {
    /** 当前会话；打开新文件时整体替换（同一 :repos 语义）。 */
    var session: DesktopSession by mutableStateOf(session)

    /** 当前打开（或新建后保存）的 .depmap 文件；null = 尚未落盘。 */
    var dataFile: File? by mutableStateOf(null)

    var screen: Screen by mutableStateOf(Screen.HOME)

    // ---- 动态屏面参数 ----
    var selectedNodeId: String? by mutableStateOf(null)
    var selectedPlanId: String? by mutableStateOf(null)
    var selectedScenarioId: String? by mutableStateOf(null)
    var selectedDriftId: String? by mutableStateOf(null)

    // ---- 导入向导状态 ----
    var importStage: Int by mutableStateOf(0) // 0=idle 1=file parsed 2=preview 3=done
    var parsedObservations: List<Observation> by mutableStateOf(emptyList())
    var parseErrors: List<String> by mutableStateOf(emptyList())
    var lastAdapterId: String by mutableStateOf("")
    var lastSourceLabel: String by mutableStateOf("")
    var preview: ImportPreview? by mutableStateOf(null)
    var csvMapping: MappingProfile? by mutableStateOf(null)
    var lastCommit: ImportCommitResult? by mutableStateOf(null)

    // ---- 通用 ----
    var notice: String? by mutableStateOf(null)
    var error: String? by mutableStateOf(null)
     private var tick by mutableStateOf(0)
     /** State 读取点：refresh() 后任何读取本键的组合自动重订阅。 */
     val refreshKey: Int get() = tick
 
     /** 每次数据变更后调用：让所有读状态的屏面重查询。 */
     fun refresh() {
         tick++
     }
    fun showError(t: Throwable) {
        error = com.pdig.desktop.persist.DepmapFileStore.classifyError(t).let { code ->
            when (code) {
                "auth_failed", "WRONG_PASSWORD" -> "口令错误或文件已被篡改（解密认证失败）。"
                "future_schema" -> "该 .depmap 由更新版本的 PDIG 生成（schema 超前），本版本拒绝猜测兼容，请升级。"
                "too_large" -> "文件过大，超过允许的上限。"
                "graph_import_failed" -> "数据文件内容不符合当前逻辑 Schema，已拒绝导入（不保留半状态）。"
                else -> t.message ?: "操作失败（$code）。"
            }
        }
    }
}

/** 打开会话的门（Gate）：新建 / 打开 .depmap 文件。 */
object Gate {
    fun createNew(ui: UiState, file: File, password: String) {
        val payload = ui.session.exportPayload()
        com.pdig.desktop.persist.DepmapFileStore().save(file, payload, password)
        ui.dataFile = file
        ui.refresh()
    }

    /** 打开文件：解密 + 校验 + 原子导入到新会话；成功后替换 ui.session。 */
    fun open(ui: UiState, file: File, password: String) {
        val payload = com.pdig.desktop.persist.DepmapFileStore().open(file, password)
        val newSession = DesktopSession.restore(payload)
        ui.session.close()
        ui.session = newSession
        ui.dataFile = file
        ui.refresh()
    }
}