package com.pdig.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.navigation.NavController
import com.pdig.app.data.AppContainer
import com.pdig.app.data.ExportBackupResult
import com.pdig.app.data.ExportFailureStage
import com.pdig.app.ui.components.LoadingState
import com.pdig.app.ui.components.PdigScrollingPage
import com.pdig.app.ui.components.PdigTopBar
import com.pdig.app.ui.theme.PdigTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
/** 导出文件名（固定，便于用户与 E2E 定位，不含时间戳以免堆积）。 */
const val BACKUP_FILE_NAME = "pdig-backup.depmap"

/**
 * 备份导出结果的 UI 状态（F1 回归测试的断言对象）。
 *
 * 之所以把它抽成**纯函数**：以前的 UI 只判断 `name == null`，
 * "文件已完整落盘"和"完全没写成功"在界面上无法区分 —— 这正是 F1 的成因。
 * 现在成功/失败各自带足够信息，且映射规则可以脱离 UI 单独测试。
 */
data class BackupUiState(val success: Boolean, val headline: String, val detail: String)

/** 稳定文案标识，避免测试去匹配会变的措辞。 */
const val BACKUP_FAIL_PREFIX = "备份失败："

/**
 * 把 [ExportBackupResult] 映射成用户可见文案。
 *
 * 约束（必须成立，否则用户在"其实成功了"的情况下被误导）：
 *  - `Success` ⇒ `success = true`，且文案里出现真实落盘文件名与字节数
 *  - `Failure` ⇒ `success = false`，且文案说明**失败在哪一步**，不再是笼统的"无法写入文件"
 */
fun backupUiState(result: ExportBackupResult): BackupUiState = when (result) {
    is ExportBackupResult.Success -> BackupUiState(
        success = true,
        headline = "备份已导出",
        detail = "文件名：${result.displayName}（${result.byteCount} 字节）。可以在「文件 → 下载」里找到它。",
    )

    is ExportBackupResult.Failure -> BackupUiState(
        success = false,
        headline = BACKUP_FAIL_PREFIX + stageLabel(result.stage),
        detail = buildString {
            append("（阶段：${result.stage.name}，类型：${result.errorType}）")
            when (result.cleanupOk) {
                true -> append(" 未完成的文件已清理。")
                false -> append(" 注意：未完成的文件可能残留在「下载」目录，请自行删除。")
                null -> append(" 尚未创建任何文件。")
            }
        },
    )
}

private fun stageLabel(stage: ExportFailureStage): String = when (stage) {
    ExportFailureStage.ENCRYPT -> "加密备份内容时出错。"
    ExportFailureStage.INSERT -> "无法在系统「下载」目录创建文件。"
    ExportFailureStage.WRITE -> "写入文件时出错。"
    ExportFailureStage.VERIFY -> "写入后校验失败，文件不完整，已按失败处理。"
    ExportFailureStage.PUBLISH -> "文件已写入但未能对其它应用可见。"
}

/** 备份结果展示面板。抽出来是为了让 Compose UI 测试可以单独渲染它。 */
@Composable
fun BackupResultPanel(result: ExportBackupResult, modifier: Modifier = Modifier) {
    val ui = backupUiState(result)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(BACKUP_RESULT_TAG),
    ) {
        Text(
            ui.headline,
            style = PdigTokens.BodyStrong,
            color = if (ui.success) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.error
            },
        )
        Text(
            ui.detail,
            style = PdigTokens.Caption,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 备份结果面板的稳定标识（Compose UI 测试与 host 侧断言共用）。 */
const val BACKUP_RESULT_TAG = "backup_result_panel"

@Composable
fun BackupScreen(nav: NavController) {
    val context = LocalContext.current
    val container = remember { AppContainer.get(context) }
    val scope = rememberCoroutineScope()
    var password by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<ExportBackupResult?>(null) }
    var busy by remember { mutableStateOf(false) }

    Scaffold(topBar = { PdigTopBar("备份", onBack = { nav.popBackStack() }) }) { pad ->
        PdigScrollingPage(
            modifier = Modifier
                .padding(pad)
                .padding(PdigTokens.SpaceLg),
            verticalArrangement = Arrangement.spacedBy(PdigTokens.SpaceMd),
        ) {
            Text(
                "备份文件使用你设置的密码加密。密码无法找回，请自行妥善保存。",
                style = PdigTokens.Body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("备份密码") },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "备份密码（至少 8 位，无法找回）" },
                singleLine = true,
            )
            Button(
                onClick = {
                    scope.launch {
                        busy = true
                        result = null
                        val outcome = withContext(Dispatchers.IO) {
                            // 任何异常都在 AppContainer 内被分类成 ExportBackupResult，
                            // 不允许把"未分类的失败"或"没写成功的成功"交到这里。
                            runCatching {
                                container.exportBackupToFile(context, password, BACKUP_FILE_NAME)
                            }.getOrElse { t ->
                                ExportBackupResult.Failure(
                                    stage = ExportFailureStage.WRITE,
                                    errorType = t.javaClass.name,
                                    detail = t.message ?: "",
                                    cleanupOk = null,
                                )
                            }
                        }
                        busy = false
                        result = outcome
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = PdigTokens.MinTouchTarget),
                enabled = password.length >= 8 && !busy,
            ) { Text("生成加密备份") }
            if (busy) LoadingState()
            result?.let { BackupResultPanel(it) }
        }
    }
}
