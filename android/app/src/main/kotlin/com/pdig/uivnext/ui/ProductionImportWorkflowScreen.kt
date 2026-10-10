package com.pdig.uivnext.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.app.ui.screens.CsvMappingStepWithParser
import com.pdig.app.ui.screens.IMPORT_PARSE_FAILED_MESSAGE
import com.pdig.app.ui.screens.IMPORT_UNREADABLE_MESSAGE
import com.pdig.app.ui.screens.ImportFailReason
import com.pdig.app.ui.screens.ImportFileResult
import com.pdig.app.ui.screens.readAndParse
import com.pdig.app.workflow.FileWorkflowPurpose
import com.pdig.app.workflow.FileWorkflowStep
import com.pdig.app.workflow.LocalFileWorkflow
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.production.ProductionVNextSession
import com.pdig.uivnext.production.mapImportCommit
import com.pdig.uivnext.production.mapImportPreview
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * R43 Production VNext import continuation.
 *
 * File selection stays Activity-owned. This screen only resumes after the normal
 * lock/re-auth boundary, consumes the pending URI once, parses in memory, previews
 * the authoritative import, and commits only after explicit human confirmation.
 */
@Composable
internal fun ProductionImportWorkflowScreen(
    session: ProductionVNextSession,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val workflow = LocalFileWorkflow.current
    val authority = session.authorities?.import
    val app = session.appState

    var pendingHead by remember { mutableStateOf<String?>(null) }
    var pendingBytes by remember { mutableStateOf<ByteArray?>(null) }

    LaunchedEffect(workflow.workflow?.pendingUri) {
        val uri = workflow.consumePendingUri(FileWorkflowPurpose.IMPORT)
            ?: return@LaunchedEffect
        val importAuthority = authority ?: run {
            workflow.statusText = "正式导入能力当前不可用。"
            return@LaunchedEffect
        }
        val sourceLabel = workflow.workflow?.requestedSourceLabel
            ?.trim()
            ?.ifBlank { null }
            ?: "文件导入"

        workflow.busy = true
        workflow.statusText = null
        workflow.launch {
            val parsed = withContext(Dispatchers.IO) {
                readAndParse(
                    context = context,
                    uri = uri,
                    fallbackLabel = sourceLabel,
                    parser = { bytes, adapterId, mapping ->
                        importAuthority.parseFile(bytes, adapterId, mapping)
                    },
                )
            }
            workflow.busy = false
            when (parsed) {
                is ImportFileResult.Failed -> {
                    pendingHead = null
                    pendingBytes = null
                    workflow.publishImportPreview(null)
                    workflow.statusText = when (parsed.reason) {
                        ImportFailReason.UNREADABLE -> IMPORT_UNREADABLE_MESSAGE
                        ImportFailReason.UNPARSEABLE -> IMPORT_PARSE_FAILED_MESSAGE
                    }
                    workflow.markReview(null)
                }

                is ImportFileResult.Ok -> {
                    pendingHead = parsed.parsed.head
                    pendingBytes = parsed.parsed.bytes
                    val (domainPreview, _) = importAuthority.preview(
                        observations = parsed.parsed.outcome.observations,
                        errors = parsed.parsed.outcome.errors,
                        adapterId = parsed.parsed.adapterId,
                        sourceLabel = parsed.parsed.sourceLabel,
                    )
                    workflow.publishImportPreview(domainPreview)
                    workflow.markReview(parsed.parsed.mapping)
                }
            }
        }
    }

    val done = workflow.importResult
    val preview = workflow.importPreview
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("pdig.production-vnext.import-workflow"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    if (done == null) "文件导入" else "导入完成",
                    color = PdigV2Colors.TextPrimary,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "文件内容只在重新认证后读取；预览、提交与关系确认保持分离。",
                    color = PdigV2Colors.TextMuted,
                    fontSize = 12.sp,
                )
            }
        }

        item {
            ImportTruthBoundary(
                "文件选择由 Activity 级安全工作流负责；拿到 URI 不会自动解锁、不会自动提交，也不会直接创建已确认依赖。"
            )
        }

        if (workflow.busy) {
            item {
                ImportInfoCard(
                    title = "正在处理",
                    body = "正在读取并解析已选择的文件。原始内容只保留在本次内存会话中。",
                )
            }
        }

        workflow.statusText?.takeIf { it.isNotBlank() }?.let { status ->
            item {
                ImportInfoCard(
                    title = if (workflow.workflow?.step == FileWorkflowStep.INTERRUPTED)
                        "需要重新选择文件" else "当前无法继续",
                    body = status,
                    warning = true,
                )
            }
        }

        if (workflow.workflow?.step == FileWorkflowStep.INTERRUPTED) {
            item {
                ImportAction(
                    label = "重新选择文件",
                    enabled = session.hostActions.requestFileImport != null && !workflow.busy,
                    testTag = "pdig.production-vnext.import.retry",
                ) {
                    session.hostActions.requestFileImport?.invoke(
                        workflow.workflow?.requestedSourceLabel ?: "文件导入",
                    )
                }
            }
        }

        if (done != null) {
            val result = mapImportCommit(done)
            item {
                ImportInfoCard(
                    title = "已写入正式记录",
                    body = "读取 ${result.rawCount} 行；新增 ${result.newUniqueCount} 条，" +
                        "跳过重复 ${result.duplicateCount} 条；记录 ${result.nodeCount} 个对象。",
                )
            }
            item {
                ImportInfoCard(
                    title = "仍需人工确认",
                    body = if (result.proposalCount > 0)
                        "生成 ${result.proposalCount} 条待确认关系。它们在确认前不会进入已确认依赖。"
                    else
                        "本次没有生成待确认关系。没有 Proposal 不代表现实中不存在关系。",
                )
            }
            item {
                ImportAction(
                    label = if (result.proposalCount > 0) "去人工复核" else "查看数据源",
                    enabled = true,
                    testTag = "pdig.production-vnext.import.done-next",
                ) {
                    workflow.clear()
                    app.navigate(
                        if (result.proposalCount > 0) VScreen.REVIEW else VScreen.SOURCES,
                    )
                }
            }
        } else if (preview != null) {
            val view = mapImportPreview(preview)
            item {
                ImportInfoCard(
                    title = "解析预览",
                    body = "来源：${view.sourceLabel} · ${view.observationCount} 条记录" +
                        if (view.skippedRowCount > 0) " · ${view.skippedRowCount} 行跳过" else "",
                )
            }

            if (preview.adapterId == "generic_csv") {
                item {
                    CsvMappingStepWithParser(
                        head = pendingHead,
                        initialMapping = workflow.workflow?.mappingProfile,
                        bytes = pendingBytes,
                        parser = { bytes, mapping ->
                            authority?.parseFile(bytes, "generic_csv", mapping)
                                ?: error("Production import authority unavailable")
                        },
                        onReparsed = { outcome, mapping ->
                            val importAuthority = authority
                                ?: return@CsvMappingStepWithParser
                            val (updated, _) = importAuthority.preview(
                                observations = outcome.observations,
                                errors = outcome.errors,
                                adapterId = "generic_csv",
                                sourceLabel = preview.sourceLabel,
                            )
                            workflow.publishImportPreview(updated)
                            workflow.markReview(mapping)
                            workflow.statusText = null
                        },
                        onReparseFailed = {
                            workflow.statusText = IMPORT_PARSE_FAILED_MESSAGE
                        },
                    )
                }
            }

            item {
                ImportDetectedObjects(
                    title = "支付工具",
                    values = view.detectedPaymentInstruments,
                )
            }
            item {
                ImportDetectedObjects(
                    title = "收款对象",
                    values = view.detectedCounterparties,
                )
            }
            item {
                ImportTruthBoundary(
                    "确认导入会写入来源 / 证据 / 对象，并生成待确认关系；Proposal ≠ Reality。"
                )
            }
            item {
                ImportAction(
                    label = "确认导入",
                    enabled = authority != null && !workflow.busy,
                    testTag = "pdig.production-vnext.import.confirm",
                ) {
                    val importAuthority = authority ?: return@ImportAction
                    workflow.busy = true
                    workflow.statusText = null
                    workflow.launch {
                        val result = runCatching {
                            withContext(Dispatchers.IO) {
                                importAuthority.commitAuthoritative(preview)
                            }
                        }
                        workflow.busy = false
                        result.onSuccess { (domainResult, _) ->
                            workflow.publishImportResult(domainResult)
                            app.requestRealityRefresh()
                        }.onFailure {
                            workflow.statusText = "导入未完成，正式记录没有被标记为成功。请重试。"
                        }
                    }
                }
            }
        } else if (
            !workflow.busy &&
            workflow.workflow?.step != FileWorkflowStep.INTERRUPTED
        ) {
            item {
                ImportInfoCard(
                    title = "等待文件",
                    body = "没有可确认的导入预览。重新选择文件继续；未记录不代表没有基础设施。",
                )
            }
            item {
                ImportAction(
                    label = "重新选择文件",
                    enabled = session.hostActions.requestFileImport != null,
                    testTag = "pdig.production-vnext.import.pick-again",
                ) {
                    session.hostActions.requestFileImport?.invoke(
                        workflow.workflow?.requestedSourceLabel ?: "文件导入",
                    )
                }
            }
        }
    }
}

internal fun isProductionImportWorkflowActive(
    purpose: FileWorkflowPurpose?,
    step: FileWorkflowStep?,
    hasPreview: Boolean,
    hasResult: Boolean,
): Boolean =
    purpose == FileWorkflowPurpose.IMPORT &&
        (step != FileWorkflowStep.IDLE || hasPreview || hasResult)

@Composable
private fun ImportDetectedObjects(
    title: String,
    values: List<String>,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PdigV2Colors.Surface,
        shape = RoundedCornerShape(VRadius.Lg),
        border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
    ) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                "$title（${values.size}）",
                color = PdigV2Colors.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
            if (values.isEmpty()) {
                Text("未从当前文件确认此类对象。", color = PdigV2Colors.TextMuted, fontSize = 11.sp)
            } else {
                values.take(8).forEach {
                    Text("· $it", color = PdigV2Colors.TextSecondary, fontSize = 11.sp)
                }
                if (values.size > 8) {
                    Text(
                        "另有 ${values.size - 8} 项将在确认后按正式导入规则处理。",
                        color = PdigV2Colors.TextMuted,
                        fontSize = 10.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun ImportInfoCard(
    title: String,
    body: String,
    warning: Boolean = false,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (warning) PdigV2Colors.Warning.copy(alpha = 0.10f)
        else PdigV2Colors.Surface,
        shape = RoundedCornerShape(VRadius.Lg),
        border = BorderStroke(
            1.dp,
            if (warning) PdigV2Colors.Warning.copy(alpha = 0.45f)
            else PdigV2Colors.BorderSubtle,
        ),
    ) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(title, color = PdigV2Colors.TextPrimary, fontSize = 13.sp,
                fontWeight = FontWeight.Bold)
            Text(body, color = PdigV2Colors.TextSecondary, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ImportTruthBoundary(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PdigV2Colors.PrimarySoft,
        shape = RoundedCornerShape(VRadius.Md),
    ) {
        Text(
            text,
            Modifier.padding(12.dp),
            color = PdigV2Colors.TextSecondary,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun ImportAction(
    label: String,
    enabled: Boolean,
    testTag: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        color = if (enabled) PdigV2Colors.PrimaryBright else PdigV2Colors.BorderSubtle,
        shape = RoundedCornerShape(VRadius.Md),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                label,
                color = if (enabled) androidx.compose.ui.graphics.Color.White
                else PdigV2Colors.TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
