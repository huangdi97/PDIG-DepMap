package com.pdig.app.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.pdig.app.data.ImportCommitResult
import com.pdig.app.data.ParseOutcome
import com.pdig.app.data.SourceRow
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

/** 导入确认按钮的稳定标识：让 Compose UI 测试与 host 侧 tap 指向同一个节点。 */
const val IMPORT_CONFIRM_TAG = "import_confirm_button"

/** 「选择文件并解析」按钮的稳定标识（D-16：E2E 需要确认它真的拉起了 SAF）。 */
const val IMPORT_PICK_FILE_TAG = "import_pick_file_button"
