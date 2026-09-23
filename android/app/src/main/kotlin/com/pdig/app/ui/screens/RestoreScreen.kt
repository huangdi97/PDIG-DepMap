package com.pdig.app.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.navigation.NavController
import com.pdig.app.data.AppContainer
import com.pdig.app.ui.Route
import com.pdig.app.ui.components.LoadingState
import com.pdig.app.ui.components.PdigScrollingPage
import com.pdig.app.ui.components.PdigTopBar
import com.pdig.app.ui.theme.PdigTokens
import com.pdig.app.workflow.FileWorkflowPurpose
import com.pdig.app.workflow.FileWorkflowStep
import com.pdig.app.workflow.LocalFileWorkflow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
/** 恢复页三个关键节点的稳定标识。 */
const val RESTORE_PICK_FILE_TAG = "restore_pick_file_button"
const val RESTORE_SELECTED_TAG = "restore_selected_file"
const val RESTORE_CONFIRM_TAG = "restore_confirm_button"
const val RESTORE_RESULT_TAG = "restore_result_text"

/**
 * 从备份恢复。
 *
 * 与 Import 同样受 D-16 影响（要走外部文件选择器），因此同样把工作流状态放到
 * Activity 作用域的 [FileWorkflowCoordinator]。
 *
 * **口令不会被跨锁保留**（这是刻意的安全选择，不是遗漏）：
 * 锁定的语义边界就是"未验证身份"，把口令留在内存里跨过这个边界，
 * 等于给它开了一条"不验证也能完成恢复"的通道。因此解锁后：
 *
 * ```
 * 已选中的 .depmap 仍在（coordinator 保存 Uri）
 *   → 用户重新输入口令
 *   → 显式点「开始恢复」
 *   → restore
 * ```
 *
 * 也就是说：**拿到文件不会自动恢复，恢复一定发生在用户重新认证并确认之后。**
 */
@Composable
fun RestoreScreen(nav: NavController) {
    val context = LocalContext.current
    val container = remember { AppContainer.get(context) }
    val wf = LocalFileWorkflow.current

    var password by remember { mutableStateOf("") }
    // 「已选中的 .depmap」必须来自 coordinator：它是跨锁状态，
    // 页面级 remember 在"锁定 → NavHost uncompose"时会被释放。
    val selectedUri = wf.pendingRestoreUri()
    val selectedName = wf.restoreFileName

    LaunchedEffect(Unit) {
        val w = wf.workflow
        if (w == null || w.purpose != FileWorkflowPurpose.RESTORE) {
            // 全新进入 Restore：不带上一次的完成态/失败态
            wf.clearRestoreMessage()
            return@LaunchedEffect
        }
        if (w.step == FileWorkflowStep.INTERRUPTED) {
            wf.statusText = "上次的选择被中断（应用进程已重启），请重新选择 .depmap 文件。"
        }
    }

    // 外部 picker 的结果到达时（App 仍处于 LOCKED），只把**展示名**记下来。
    // 不做任何读取内容 / 解密 / 恢复 —— 那些必须等用户重新认证并显式确认。
    LaunchedEffect(wf.workflow?.pendingUri) {
        val uri = wf.pendingRestoreUri() ?: return@LaunchedEffect
        if (wf.restoreFileName == null) {
            wf.restoreFileName = queryDisplayName(context, uri)
        }
    }

    fun restoreNow() {
        val uri = wf.pendingRestoreUri() ?: return
        val pw = password
        wf.clearRestoreMessage()
        wf.busy = true
        wf.statusText = null
        wf.launch {
            val outcome = withContext(Dispatchers.IO) {
                val json = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?.toString(Charsets.UTF_8)
                if (json == null) null
                else runCatching { container.restoreBackup(json, pw) }.getOrNull()
            }
            wf.busy = false
            if (outcome == null) {
                // 失败：保留已选文件，允许改口令重试；绝不推进工作流状态。
                wf.failRestore("无法恢复：密码错误、文件损坏，或版本不受支持。")
            } else {
                wf.completeRestore("已恢复 $outcome 条记录。")
            }
        }
    }

    Scaffold(
        topBar = { PdigTopBar("从备份恢复", onBack = { wf.clear(); nav.popBackStack() }) },
    ) { pad ->
        PdigScrollingPage(
            modifier = Modifier
                .padding(pad)
                .padding(PdigTokens.SpaceLg),
            verticalArrangement = Arrangement.spacedBy(PdigTokens.SpaceMd),
        ) {
            Text(
                "恢复会替换当前设备上的全部数据。操作失败时不会留下半恢复状态。",
                style = PdigTokens.Body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("备份密码") },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "恢复用备份密码" },
                singleLine = true,
            )
            Button(
                onClick = {
                    wf.beginRestore(Route.RESTORE)
                    if (!wf.launchPicker("*/*")) {
                        wf.statusText = "无法打开文件选择器，请重试。"
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = PdigTokens.MinTouchTarget)
                    .testTag(RESTORE_PICK_FILE_TAG),
                enabled = password.isNotEmpty() && !wf.busy && wf.launcherAttached,
            ) { Text("选择 .depmap 文件") }

            val name = selectedName
            if (name != null) {
                Text(
                    "已选择文件：$name。请输入备份密码后点「开始恢复」。",
                    style = PdigTokens.Body,
                    modifier = Modifier.testTag(RESTORE_SELECTED_TAG),
                )
            }
            Button(
                onClick = { restoreNow() },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = PdigTokens.MinTouchTarget)
                    .testTag(RESTORE_CONFIRM_TAG),
                enabled = selectedUri != null && password.isNotEmpty() && !wf.busy,
            ) { Text("开始恢复") }

            if (wf.busy) LoadingState()
            wf.statusText?.let { Text(it, style = PdigTokens.Caption, color = MaterialTheme.colorScheme.error) }
            wf.restoreMessage?.let {
                Text(
                    it,
                    style = PdigTokens.BodyStrong,
                    modifier = Modifier.testTag(RESTORE_RESULT_TAG),
                )
            }
        }
    }
}

/** 读取用户可读的文件名（用于"已选择文件：xxx"提示）。 */
private fun queryDisplayName(context: android.content.Context, uri: Uri): String? = try {
    context.contentResolver.query(uri, null, null, null, null)?.use { c ->
        val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
    }
} catch (_: Throwable) {
    null
} ?: uri.lastPathSegment
