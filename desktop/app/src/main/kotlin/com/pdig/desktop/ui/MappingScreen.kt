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
import com.pdig.core.sources.GenericCsvParser
import com.pdig.core.sources.MappingColumns
import com.pdig.core.sources.MappingOptions
import com.pdig.core.sources.MappingProfile
import com.pdig.desktop.ui.components.PdigPage
import com.pdig.desktop.ui.components.SectionDivider

/** CSV 映射：用户显式给出列对应与分隔符，再解析文件进入导入预览。 */
@Composable
fun MappingScreen(ui: UiState) {
    var dateTimeCol by remember { mutableStateOf("") }
    var counterpartyCol by remember { mutableStateOf("") }
    var amountCol by remember { mutableStateOf("") }
    var debitCol by remember { mutableStateOf("") }
    var creditCol by remember { mutableStateOf("") }
    var currencyCol by remember { mutableStateOf("") }
    var delimiter by remember { mutableStateOf(",") }
    var problem by remember { mutableStateOf<String?>(null) }

    PdigPage(
        title = "CSV 映射",
        subtitle = "为通用 CSV 显式指定列映射（列名 = 文件表头，禁止自动猜测）",
        notice = ui.notice,
        error = ui.error,
        onDismissNotice = { ui.notice = null },
        onDismissError = { ui.error = null },
    ) {
        Column {
            SectionDivider("分隔符")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DelimiterButton("逗号 ,", ",", delimiter) { delimiter = "," }
                DelimiterButton("分号 ;", ";", delimiter) { delimiter = ";" }
                DelimiterButton("Tab", "\t", delimiter) { delimiter = "\t" }
                DelimiterButton("竖线 |", "|", delimiter) { delimiter = "|" }
            }
            SectionDivider("列映射（填写文件表头名）")
            OutlinedTextField(value = dateTimeCol, onValueChange = { dateTimeCol = it }, label = { Text("日期时间列（必填）") }, singleLine = true)
            OutlinedTextField(value = counterpartyCol, onValueChange = { counterpartyCol = it }, label = { Text("对方 / 商户列") }, singleLine = true)
            OutlinedTextField(value = amountCol, onValueChange = { amountCol = it }, label = { Text("金额列（向外为正）") }, singleLine = true)
            OutlinedTextField(value = debitCol, onValueChange = { debitCol = it }, label = { Text("借方列（可选，与贷方配对）") }, singleLine = true)
            OutlinedTextField(value = creditCol, onValueChange = { creditCol = it }, label = { Text("贷方列（可选）") }, singleLine = true)
            OutlinedTextField(value = currencyCol, onValueChange = { currencyCol = it }, label = { Text("币种列（可选）") }, singleLine = true)
            if (problem != null) {
                Spacer(Modifier.height(8.dp))
                Text(problem ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = {
                if (dateTimeCol.isBlank()) {
                    problem = "日期时间列是必填项。"
                    return@Button
                }
                problem = null
                val mapping = buildMapping(dateTimeCol, counterpartyCol, amountCol, debitCol, creditCol, currencyCol, delimiter)
                ui.csvMapping = mapping
                parseCsv(ui, mapping)
            }) { Text("解析文件") }
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = { ui.screen = Screen.IMPORT }) { Text("返回导入") }
        }
    }
}

@Composable
private fun DelimiterButton(label: String, value: String, selected: String, onSelect: () -> Unit) {
    TextButton(onClick = onSelect) {
        Text(
            label,
            color = if (selected == value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun buildMapping(
    dateTimeCol: String,
    counterpartyCol: String,
    amountCol: String,
    debitCol: String,
    creditCol: String,
    currencyCol: String,
    delimiter: String,
): MappingProfile {
    // 金额符号模式必须与所用列一致：借贷列 → debit_credit；单金额列 → outward_positive。
    // 否则 GenericCsvParser 会静默忽略不匹配的列。
    val signMode = if (debitCol.isNotBlank() || creditCol.isNotBlank()) "debit_credit" else "outward_positive"
    return MappingProfile(
        columns = MappingColumns(
            dateTime = dateTimeCol.trim(),
            amount = amountCol.trim().ifEmpty { null },
            debit = debitCol.trim().ifEmpty { null },
            credit = creditCol.trim().ifEmpty { null },
            counterparty = counterpartyCol.trim().ifEmpty { null },
            currency = currencyCol.trim().ifEmpty { null },
        ),
        options = MappingOptions(
            delimiter = delimiter,
            dateFormats = listOf("YYYY-MM-DD"),
            decimalSeparator = '.',
            amountSignMode = signMode,
            hasHeaderRow = true,
            encoding = "utf-8",
        ),
    )
}

private fun parseCsv(ui: UiState, mapping: MappingProfile) {
    try {
        val file = ui.fileOps.pickOpen("选择 CSV 文件", "CSV", "csv")
        if (file == null) return
        val result = GenericCsvParser.parse(ui.fileOps.readBytes(file), mapping)
        ui.parsedObservations = result.observations
        ui.parseErrors = result.errors.map { "${it.line}: ${it.reason}" }
        ui.lastAdapterId = "generic_csv"
        ui.lastSourceLabel = "通用 CSV 账单"
        ui.preview = ui.session.sources.previewImport(result.observations, ui.parseErrors, ui.lastAdapterId, ui.lastSourceLabel)
        ui.importStage = 2
        ui.screen = Screen.IMPORT
    } catch (t: Throwable) {
        ui.showError(t)
    }
}