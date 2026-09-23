package com.pdig.desktop.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pdig.core.sources.Observation
import com.pdig.core.sources.ObservationDirection
import com.pdig.core.sources.OfxParser
import com.pdig.core.sources.WechatParser
import com.pdig.desktop.ui.components.ActionRow
import com.pdig.desktop.ui.components.InfoRow
import com.pdig.desktop.ui.components.PdigPage
import com.pdig.desktop.ui.components.SectionDivider

private data class ManualRow(
    val date: String,
    val description: String,
    val amount: Double,
    val currency: String,
    val direction: ObservationDirection,
)

/** 导入向导：Stage0 选来源 → Stage2 预览 → Stage3 结果（Stage1 未使用）。 */
@Composable
fun ImportScreen(ui: UiState) {
    when (ui.importStage) {
        2 -> ImportPreviewStep(ui)
        3 -> ImportDoneStep(ui)
        else -> ImportStage0(ui)
    }
}

@Composable
private fun ImportStage0(ui: UiState) {
    var manualOpen by remember { mutableStateOf(false) }
    PdigPage(
        title = "导入",
        subtitle = "选择账单文件或手动录入（Observation 只在本次会话内存中，不落库）",
        notice = ui.notice,
        error = ui.error,
        onDismissNotice = { ui.notice = null },
        onDismissError = { ui.error = null },
    ) {
        Column {
            ActionRow(
                title = "微信账单 CSV",
                description = "微信支付「账单 → 导出」得到的 CSV 文件",
                actionLabel = "选择文件",
                onAction = { pickWechat(ui) },
            )
            ActionRow(
                title = "通用 CSV",
                description = "任意结构化 CSV：先配置字段映射（日期 / 对方 / 金额 / 币种）",
                actionLabel = "配置映射",
                onAction = {
                    ui.lastAdapterId = "generic_csv"
                    ui.screen = Screen.MAPPING
                },
            )
            ActionRow(
                title = "OFX / QFX",
                description = "银行导出的 OFX/QFX 流水文件",
                actionLabel = "选择文件",
                onAction = { pickOfx(ui) },
            )
            ActionRow(
                title = "手动录入",
                description = "逐笔输入日期、说明、金额、币种与方向",
                actionLabel = if (manualOpen) "收起" else "打开表单",
                onAction = { manualOpen = !manualOpen },
            )
            if (manualOpen) ManualEntryForm(ui)
        }
    }
}

@Composable
private fun ManualEntryForm(ui: UiState) {
    var date by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("") }
    var direction by remember { mutableStateOf(ObservationDirection.OUT) }
    var rows by remember { mutableStateOf(listOf<ManualRow>()) }
    var problem by remember { mutableStateOf<String?>(null) }

    val addRow = {
        val dateOk = date.trim().matches(Regex("\\d{4}-\\d{2}-\\d{2}"))
        val amountValue = amount.trim().toDoubleOrNull()
        problem = when {
            !dateOk -> "日期格式应为 YYYY-MM-DD。"
            description.isBlank() -> "请填写说明 / 用途。"
            amountValue == null -> "金额必须是数字。"
            else -> null
        }
        val a = amountValue
        if (dateOk && description.isNotBlank() && a != null) {
            rows = rows + ManualRow(date.trim(), description.trim(), a, currency.trim(), direction)
            date = ""
            description = ""
            amount = ""
            currency = ""
        }
    }

    Column {
        SectionDivider("单条记录")
        OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("日期（YYYY-MM-DD）") }, singleLine = true)
        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("说明 / 用途") }, singleLine = true)
        OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("金额") }, singleLine = true)
        OutlinedTextField(value = currency, onValueChange = { currency = it }, label = { Text("币种（留空 = CNY）") }, singleLine = true)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { direction = ObservationDirection.OUT }) {
                Text("支出", color = if (direction == ObservationDirection.OUT) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = { direction = ObservationDirection.IN }) {
                Text("收入", color = if (direction == ObservationDirection.IN) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = addRow) { Text("添加一条") }
        }
        if (problem != null) {
            Text(problem ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
        }
        if (rows.isNotEmpty()) {
            SectionDivider("已录入（${rows.size}）")
            rows.forEachIndexed { i, r ->
                InfoRow(
                    "第 ${i + 1} 条",
                    "${r.date} ${r.description} ${r.amount} ${r.currency.ifEmpty { "CNY" }}" +
                        "（${if (r.direction == ObservationDirection.OUT) "支出" else "收入"}）",
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { runManualImport(ui, rows) }) { Text("解析这 ${rows.size} 条并预览") }
                TextButton(onClick = { rows = emptyList() }) { Text("清空") }
            }
        }
    }
}

private fun pickWechat(ui: UiState) {
    try {
        val file = ui.fileOps.pickOpen("选择微信账单 CSV", "CSV", "csv")
        if (file == null) return
        val result = WechatParser.parse(ui.fileOps.readBytes(file))
        ui.parsedObservations = result.observations
        ui.parseErrors = result.errors.map { "${it.line}: ${it.reason}" }
        ui.lastAdapterId = "wechat_statement"
        ui.lastSourceLabel = "微信账单"
        ui.preview = ui.session.sources.previewImport(result.observations, ui.parseErrors, ui.lastAdapterId, ui.lastSourceLabel)
        ui.importStage = 2
    } catch (t: Throwable) {
        ui.showError(t)
    }
}

private fun pickOfx(ui: UiState) {
    try {
        val file = ui.fileOps.pickOpen("选择 OFX/QFX 文件", "OFX/QFX", "ofx", "qfx")
        if (file == null) return
        val result = OfxParser.parse(ui.fileOps.readBytes(file))
        ui.parsedObservations = result.observations
        ui.parseErrors = result.errors.map { "${it.line}: ${it.reason}" }
        ui.lastAdapterId = "ofx_qfx"
        ui.lastSourceLabel = "OFX/QFX 账单"
        ui.preview = ui.session.sources.previewImport(result.observations, ui.parseErrors, ui.lastAdapterId, ui.lastSourceLabel)
        ui.importStage = 2
    } catch (t: Throwable) {
        ui.showError(t)
    }
}

private fun runManualImport(ui: UiState, rows: List<ManualRow>) {
    val observations = rows.map { r ->
        Observation(
            source = "manual",
            sourceTxnId = null,
            merchantTxnId = null,
            occurredAt = "${r.date}T00:00:00",
            merchantRaw = r.description,
            description = r.description,
            amount = r.amount,
            currency = r.currency.ifEmpty { "CNY" },
            direction = r.direction,
            paymentMethodRaw = "manual",
            status = "",
            note = "",
        )
    }
    ui.parsedObservations = observations
    ui.parseErrors = emptyList()
    ui.lastAdapterId = "manual_entry"
    ui.lastSourceLabel = "手动录入"
    ui.preview = ui.session.sources.previewImport(observations, emptyList(), ui.lastAdapterId, ui.lastSourceLabel)
    ui.importStage = 2
}