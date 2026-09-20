package com.pdig.app.ui.screens

import android.net.Uri
import java.io.File
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.pdig.app.data.AppContainer.ImportCommitResult
import com.pdig.app.data.ExportBackupResult
import com.pdig.app.data.ExportFailureStage
import com.pdig.app.data.ParseOutcome
import com.pdig.app.data.SourceRow
import com.pdig.app.security.DatabaseKeyStore
import com.pdig.app.security.LockGate
import com.pdig.app.ui.Route
import com.pdig.app.ui.components.EmptyState
import com.pdig.app.ui.components.LoadingState
import com.pdig.app.ui.components.PdigCard
import com.pdig.app.ui.components.PdigScrollingPage
import com.pdig.app.ui.components.PdigTopBar
import com.pdig.app.ui.components.SectionHeader
import com.pdig.app.ui.theme.PdigTokens
import com.pdig.app.workflow.FileWorkflowPurpose
import com.pdig.app.workflow.FileWorkflowStep
import com.pdig.app.workflow.LocalFileWorkflow
import com.pdig.core.sources.MappingProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 导入：Observation 仅存在于内存；原始账单与解析结果导入结束后立即释放（spec §150/§151）。
 * 解析器语义完全复用 core 实现，不重新发明（spec §69）。
 *
 * 步骤固定：选择来源 → 选择文件 → parse → preview → Node Resolution → 确认写入。
 * 确认写入之后落到库里的是 SourceInstance / Fingerprint / Evidence / Node / **Proposal**，
 * **不会**直接产生 Dependency —— Proposal ≠ Reality（spec §13）。
 *
 * ---------------------------------------------------------------------------
 * ## D-16（方案 A）：本页的所有关键状态都在 Activity 作用域
 *
 * 外部文件选择器（DocumentsUI）是**独立任务**：拉起它会让 `MainActivity.onStop`
 * → `LockGate.lockNow()` → NavHost 离开组合树 → **页面级 `remember` 全部被释放**。
 * 之前 ActivityResult launcher 也注册在本页（`rememberLauncherForActivityResult`），
 * 于是在 `onDispose` 时被 `unregister()`，待投递结果一起丢失。
 *
 * 现在：
 *  - launcher 注册在 `MainActivity.onCreate`（生命周期 = Activity，不随组合树注销）
 *  - 来源选择 / 当前步骤 / 待处理 Uri / Node Resolution 预览都存在
 *    `FileWorkflowCoordinator`（Activity 作用域 ViewModel）
 *  - 解析与提交跑在 `viewModelScope`，因此**切换后台不会取消导入**
 *
 * 安全语义保持不变：锁定期间 NavHost 仍不参与组合；ActivityResult 回调
 * **只登记 Uri，绝不解锁、绝不读文件内容、绝不提交**。
 * ---------------------------------------------------------------------------
 */
@Composable
fun ImportScreen(nav: NavController) {
    val context = LocalContext.current
    val container = remember { AppContainer.get(context) }
    val wf = LocalFileWorkflow.current

    var sources by remember { mutableStateOf<List<SourceRow>?>(null) }
    var selectedSourceId by remember { mutableStateOf<String?>(null) }
    var newSourceName by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        sources = withContext(Dispatchers.IO) { container.sourceInstances() }
    }

    // 进入本页：把跨锁存活的工作流状态接回页面。
    // 页面级 remember 在"锁定 → NavHost uncompose"时已被释放，
    // 因此来源选择与解析结果**必须以 coordinator 为准**。
    LaunchedEffect(Unit) {
        val w = wf.workflow
        if (w == null || w.purpose != FileWorkflowPurpose.IMPORT) {
            // 全新进入：不带任何上一次的完成态
            wf.publishImportResult(null)
            return@LaunchedEffect
        }
        selectedSourceId = w.requestedSourceId
        if (w.requestedSourceId == null) newSourceName = w.requestedSourceLabel.orEmpty()
        if (w.step == FileWorkflowStep.INTERRUPTED) {
            wf.statusText = "上次的选择被中断（应用进程已重启），请重新选择文件。"
        }
    }

    // 用户重新认证之后，取回外部 picker 的结果并解析 —— D-16 主流程的最后一段。
    // 解析放在 viewModelScope（不是 rememberCoroutineScope）：即使此刻再次被切到后台，
    // 工作也不会被取消。
    LaunchedEffect(wf.workflow?.pendingUri) {
        val uri = wf.consumePendingUri(FileWorkflowPurpose.IMPORT) ?: return@LaunchedEffect
        val label = wf.workflow?.requestedSourceLabel ?: "账单文件"
        wf.busy = true
        wf.statusText = null
        wf.launch {
            val parsed = withContext(Dispatchers.IO) { readAndParse(context, uri, container, label) }
            wf.busy = false
            if (parsed == null) {
                wf.statusText = "无法读取或解析这个文件。"
                wf.publishImportPreview(null)
                wf.markReview(null)
            } else {
                wf.publishImportPreview(
                    container.previewImport(
                        parsed.outcome.observations,
                        parsed.outcome.errors,
                        parsed.adapterId,
                        parsed.sourceLabel,
                    ),
                )
                wf.markReview(parsed.mapping)
            }
        }
    }

    fun activeSourceLabel(): String? {
        val picked = sources?.firstOrNull { it.id == selectedSourceId }
        if (picked != null) return picked.label
        return newSourceName.trim().ifBlank { null }
    }

    // 导入完成态必须在 Scaffold 之前整体返回。
    // 原因：把 `return@Scaffold` 写在嵌套的 Column lambda 里会让 Compose 的
    // start/end 配对失衡，重组时抛 ComposeRuntimeError("Start/end imbalance")。
    // 实测：提交导入后渲染"导入完成"分支必崩（crash buffer: com.pdig.app）。
    val done = wf.importResult
    if (done != null) {
        ImportDonePanel(
            nav = nav,
            done = done,
            onLeave = { wf.clear() },
        )
        return
    }

    Scaffold(
        topBar = {
            PdigTopBar("导入账单", onBack = { wf.clear(); nav.popBackStack() })
        },
    ) { pad ->
        // P1-A：滚动容器统一由 PdigScrollingPage 提供，末尾固定留出底部空隙，
        // 保证末尾的「确认导入」按钮能完整滚入可视区
        // （视觉区 == 真实点击区 == semantics bounds）。
        PdigScrollingPage(
            modifier = Modifier
                .padding(pad)
                .padding(PdigTokens.SpaceLg),
            verticalArrangement = Arrangement.spacedBy(PdigTokens.SpaceMd),
        ) {
            Text(
                "文件只在本机解析，不会上传；解析结果不会长期保存原始交易明细。",
                style = PdigTokens.Body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // ---- 步骤 1：选择来源 ----
            SectionHeader("第 1 步 · 选择数据来源")
            when {
                sources == null -> LoadingState()
                sources!!.isNotEmpty() -> sources!!.forEach { s ->
                    PdigCard(onClick = {
                        selectedSourceId = s.id
                        newSourceName = ""
                    }) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(s.label, style = PdigTokens.BodyStrong, modifier = Modifier.weight(1f))
                            if (selectedSourceId == s.id) {
                                Text("已选择", style = PdigTokens.Label, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
                else -> EmptyState("还没有数据来源，请在下方新建一个。")
            }
            OutlinedTextField(
                value = newSourceName,
                onValueChange = { newSourceName = it; selectedSourceId = null },
                label = { Text("或新建来源名称") },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "新建数据来源名称" },
                singleLine = true,
            )

            // ---- 步骤 2：选择文件并解析 ----
            //
            // 交给 Activity 作用域的 coordinator 拉起外部文件选择器：
            // launcher 注册在 MainActivity，不会因为本页离开组合树而被注销（D-16）。
            SectionHeader("第 2 步 · 选择文件")
            Button(
                onClick = {
                    wf.beginImport(Route.IMPORT, selectedSourceId, activeSourceLabel())
                    if (!wf.launchPicker("*/*")) {
                        wf.statusText = "无法打开文件选择器，请重试。"
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = PdigTokens.MinTouchTarget)
                    .testTag(IMPORT_PICK_FILE_TAG),
                enabled = activeSourceLabel() != null && !wf.busy && wf.launcherAttached,
            ) { Text("选择文件并解析") }
            if (wf.busy) LoadingState()
            wf.statusText?.let { Text(it, style = PdigTokens.BodyStrong) }

            // ---- 步骤 3：Node Resolution ----
            val p = wf.importPreview
            if (p != null) {
                SectionHeader("第 3 步 · 确认要记录的对象")
                Text(
                    "解析到 ${p.observations.size} 条记录" +
                        (if (p.errors.isEmpty()) "" else "，${p.errors.size} 行被跳过"),
                    style = PdigTokens.BodyStrong,
                )
                Text("支付方式（${p.instruments.size}）", style = PdigTokens.Body)
                p.instruments.forEach { i -> Text("· ${i.label}", style = PdigTokens.Caption) }
                Text("收款对象（${p.counterparties.size}）", style = PdigTokens.Body)
                p.counterparties.forEach { c -> Text("· ${c.label}", style = PdigTokens.Caption) }
                Text(
                    "确认后会记录上面这些对象，并为「支付方式 → 收款对象」生成待确认关系。" +
                        "关系在确认前不会参与影响分析。",
                    style = PdigTokens.Caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = {
                        wf.busy = true
                        // viewModelScope：即使提交过程中被切到后台也不会被取消
                        wf.launch {
                            val applied = withContext(Dispatchers.IO) { container.commitImport(p) }
                            wf.busy = false
                            wf.publishImportResult(applied)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = PdigTokens.MinTouchTarget)
                        .testTag(IMPORT_CONFIRM_TAG),
                    enabled = !wf.busy,
                ) { Text("确认导入") }
            }
        }
    }
}

/**
 * 导入完成态。
 *
 * 单独成函数而不是在 ImportScreen 内部 `return@Scaffold`：后者写在嵌套 Column
 * lambda 中会造成 Compose start/end 失衡并在重组时崩溃。
 */
@Composable
private fun ImportDonePanel(nav: NavController, done: ImportCommitResult, onLeave: () -> Unit) {
    Scaffold(
        topBar = { PdigTopBar("导入账单", onBack = { onLeave(); nav.popBackStack() }) },
    ) { pad ->
        PdigScrollingPage(
            modifier = Modifier
                .padding(pad)
                .padding(PdigTokens.SpaceLg),
            verticalArrangement = Arrangement.spacedBy(PdigTokens.SpaceMd),
        ) {
            Text("导入完成", style = PdigTokens.Title)
            Text(
                "记录 ${done.rawCount} 行；新增不重复 ${done.newUniqueCount} 条，重复跳过 ${done.duplicateCount} 条。",
                style = PdigTokens.Body,
            )
            Text(
                "记录 ${done.nodeCount} 个对象，生成 ${done.proposalCount} 条待确认关系。",
                style = PdigTokens.Body,
            )
            Text(
                "这些关系还只是候选，需要你确认后才会成为事实。",
                style = PdigTokens.Body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = { onLeave(); nav.navigate(Route.REVIEW) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = PdigTokens.MinTouchTarget),
            ) { Text("去确认候选关系") }
        }
    }
}

/** 导出文件名（固定，便于用户与 E2E 定位，不含时间戳以免堆积）。 */
const val BACKUP_FILE_NAME = "pdig-backup.depmap"

/** 导入确认按钮的稳定标识：让 Compose UI 测试与 host 侧 tap 指向同一个节点。 */
const val IMPORT_CONFIRM_TAG = "import_confirm_button"

/** 「选择文件并解析」按钮的稳定标识（D-16：E2E 需要确认它真的拉起了 SAF）。 */
const val IMPORT_PICK_FILE_TAG = "import_pick_file_button"

/** 恢复页三个关键节点的稳定标识。 */
const val RESTORE_PICK_FILE_TAG = "restore_pick_file_button"
const val RESTORE_SELECTED_TAG = "restore_selected_file"
const val RESTORE_CONFIRM_TAG = "restore_confirm_button"
const val RESTORE_RESULT_TAG = "restore_result_text"

/** 一次"读文件 + 解析"的结果。**只在内存中流转**，不落库、不进 Bundle。 */
private data class ParsedFile(
    val outcome: ParseOutcome,
    val adapterId: String,
    val sourceLabel: String,
    val mapping: MappingProfile?,
)

/**
 * 读取并解析用户选中的文件。
 *
 * 抽成纯函数（而不是写在 Composable 里）的原因：D-16 之后它会被
 * **用户重新认证之后**再次调用（此时页面刚被重建），必须能在任何组合状态下独立执行。
 * 适配器按内容判定，不按扩展名猜（spec §69）。
 */
private suspend fun readAndParse(
    context: android.content.Context,
    uri: Uri,
    container: AppContainer,
    fallbackLabel: String,
): ParsedFile? {
    val bytes = try {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
    } catch (_: Throwable) {
        null
    } ?: return null
    val head = String(bytes, Charsets.UTF_8)
    val adapter = when {
        head.contains("微信支付账单明细") ||
            (head.contains("交易时间") && head.contains("收/支")) -> "wechat"
        head.contains("OFX", ignoreCase = true) -> "ofx_qfx"
        else -> "generic_csv"
    }
    val mapping = if (adapter == "generic_csv") defaultCsvMapping(head) else null
    val outcome = runCatching {
        container.parseFile(bytes, adapter, mapping)
    }.getOrNull() ?: return null
    return ParsedFile(outcome, adapter, fallbackLabel, mapping)
}

/** 极简 CSV 映射猜测：**仅在用户确认后**使用；绝不静默自动映射（spec §17 禁止 AI 自动映射）。 */
internal fun defaultCsvMapping(head: String): com.pdig.core.sources.MappingProfile? {
    val firstLine = head.lineSequence().firstOrNull() ?: return null
    val cols = firstLine.split(',', ';').map { it.trim().trim('"') }
    val date = cols.firstOrNull { it.contains("date", true) || it.contains("时间") } ?: return null
    val amount = cols.firstOrNull { it.contains("amount", true) || it.contains("金额") } ?: return null
    return com.pdig.core.sources.MappingProfile(
        columns = com.pdig.core.sources.MappingColumns(dateTime = date, amount = amount),
        options = com.pdig.core.sources.MappingOptions(
            delimiter = if (firstLine.contains(';')) ";" else ",",
        ),
    )
}

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

@Composable
fun SettingsScreen(nav: NavController) {
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }
    Scaffold(topBar = { PdigTopBar("设置", onBack = { nav.popBackStack() }) }) { pad ->
        PdigScrollingPage(
            modifier = Modifier
                .padding(pad)
                .padding(PdigTokens.SpaceLg),
            verticalArrangement = Arrangement.spacedBy(PdigTokens.SpaceMd),
        ) {
            PdigCard(onClick = { nav.navigate(Route.SOURCES) }) { Text("数据来源管理") }
            PdigCard(onClick = { nav.navigate(Route.BACKUP) }) { Text("备份") }
            PdigCard(onClick = { nav.navigate(Route.RESTORE) }) { Text("从备份恢复") }
            // P0-A：把「锁定」做成用户可主动触发的动作。
            // 它只是把 application/security state 置回 LOCKED，
            // 不写数据库、不改 Reality Graph —— 所以这里不需要任何领域逻辑。
            PdigCard(onClick = { LockGate.lockNow() }) {
                Column(verticalArrangement = Arrangement.spacedBy(PdigTokens.SpaceXs)) {
                    Text("立即锁定", style = PdigTokens.BodyStrong)
                    Text(
                        "退出前锁上应用；回到前台时需要重新验证身份。",
                        style = PdigTokens.Caption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            // L-37：删除所有数据。只删除本应用私有数据（DB / 包裹密钥 / 缓存 / 工作流状态），
            // **不删除**用户导出到外部位置的 `.depmap` 备份文件。破坏性操作，必须显式确认。
            PdigCard(onClick = { showDeleteConfirm = true }) {
                Column(verticalArrangement = Arrangement.spacedBy(PdigTokens.SpaceXs)) {
                    Text(
                        "删除所有数据",
                        style = PdigTokens.BodyStrong,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        "删除本机全部记录（数据库、密钥、缓存与工作流状态）。不会删除你导出到外部位置的备份文件。此操作不可恢复。",
                        style = PdigTokens.Caption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            PdigCard(onClick = { nav.navigate(Route.PRIVACY) }) { Text("隐私") }
            PdigCard(onClick = { nav.navigate(Route.ABOUT) }) { Text("关于") }
        }
    }
    if (showDeleteConfirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除所有数据？") },
            text = {
                Text(
                    "将永久删除这台设备上的全部记录（数据库、密钥、缓存与工作流状态）。" +
                        "这不影响你已导出到外部位置的 `.depmap` 备份文件。此操作无法撤销。",
                )
            },
            confirmButton = {
                Button(onClick = {
                    showDeleteConfirm = false
                    // 后台删除私有数据，主线程不被 DB 打开阻塞（L-37/D-12 同口径）。
                    val executor = java.util.concurrent.Executors.newSingleThreadExecutor()
                    executor.execute {
                        try {
                            AppContainer.reset()
                            DatabaseKeyStore.clear(context)
                            // deleteDatabase 在 SQLCipher 直接打开的文件上可能静默失败
                            // （L-37 设备实测），补一层直接删文件（含 WAL/SHM/journal）。
                            context.deleteDatabase("pdig.db")
                            File(context.filesDir, "pdig.db").delete()
                            File(context.filesDir, "pdig.db-journal").delete()
                            File(context.filesDir, "pdig.db-wal").delete()
                            File(context.filesDir, "pdig.db-shm").delete()
                            context.cacheDir?.listFiles()?.forEach { it.delete() }
                            context.filesDir.listFiles()?.forEach { f ->
                                if (f.name != "pdig.db") f.delete()
                            }
                        } finally {
                            executor.shutdown()
                        }
                    }
                    LockGate.lockNow()
                }) { Text("删除", color = MaterialTheme.colorScheme.onError) }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            },
        )
    }
}

@Composable
fun PrivacyScreen(nav: NavController) {
    Scaffold(topBar = { PdigTopBar("隐私", onBack = { nav.popBackStack() }) }) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .padding(PdigTokens.SpaceLg)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(PdigTokens.SpaceMd),
        ) {
            Text("数据只保存在这台设备上", style = PdigTokens.Title)
            Text(
                "本应用没有业务网络请求，不包含分析、广告或遥测组件。\n" +
                    "导入的原始账单与交易明细只在内存中用于解析，不会被保存到数据库。\n" +
                    "备份文件使用你设置的密码加密，密码无法找回。",
                style = PdigTokens.Body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun AboutScreen(nav: NavController) {
    Scaffold(topBar = { PdigTopBar("关于", onBack = { nav.popBackStack() }) }) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .padding(PdigTokens.SpaceLg)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(PdigTokens.SpaceMd),
        ) {
            Text("PDIG", style = PdigTokens.Display)
            Text("个人数字基础设施图谱", style = PdigTokens.Body)
            Text("版本 0.1.0-milestone（Native Migration）", style = PdigTokens.Caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            EmptyState("本版本为原生迁移内部里程碑版本，不是对外正式发布版本。")
        }
    }
}

