package com.pdig.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.pdig.app.data.AppContainer
import com.pdig.app.data.ParseOutcome
import com.pdig.app.ui.components.SectionHeader
import com.pdig.app.ui.theme.PdigTokens
import com.pdig.core.sources.MappingProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 第 2.5 步 · 检查字段对应（goal §23）。
 *
 * 仅对 generic_csv 显示（调用方判断 adapterId）。展示自动检测到的日期时间列 / 金额列，
 * 每个字段可通过下拉更换为表头里的其他列，并展示该列第一行的示例值。
 * 变更后基于选中列重新构造 MappingProfile 并重新 parse（复用 core 解析器，不重新发明）。
 *
 * head / bytes 是页面内存态：页面被锁定重建后它们已释放（spec §151），此时隐藏本步骤，
 * 预览与已选映射仍保留在 Activity 作用域的工作流里 —— 用户重新选择文件即可再次调整。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CsvMappingStep(
    head: String?,
    initialMapping: MappingProfile?,
    bytes: ByteArray?,
    container: AppContainer,
    onReparsed: (outcome: ParseOutcome, mapping: MappingProfile) -> Unit,
    onReparseFailed: () -> Unit,
) {
    val columns = head?.let { csvColumns(it) }.orEmpty()
    val sample = head?.let { csvSampleRow(it) }.orEmpty()
    if (columns.isEmpty()) return

    var dateColumn by remember { mutableStateOf(initialMapping?.columns?.dateTime.orEmpty()) }
    var amountColumn by remember { mutableStateOf(initialMapping?.columns?.amount.orEmpty()) }
    val scope = rememberCoroutineScope()

    // 页面重建（锁定→解锁）后页面级 state 已丢，但工作流里的 mappingProfile 仍在：用它兜底。
    LaunchedEffect(initialMapping) {
        if (dateColumn.isBlank()) dateColumn = initialMapping?.columns?.dateTime.orEmpty()
        if (amountColumn.isBlank()) amountColumn = initialMapping?.columns?.amount.orEmpty()
    }

    fun reparse() {
        val headText = head ?: return
        val mapping = csvMapping(headText, dateColumn, amountColumn) ?: return
        val data = bytes
        if (data == null) {
            onReparseFailed()
            return
        }
        scope.launch {
            val outcome = runCatching {
                withContext(Dispatchers.IO) { container.parseFile(data, "generic_csv", mapping) }
            }.getOrNull()
            if (outcome == null) onReparseFailed() else onReparsed(outcome, mapping)
        }
    }

    SectionHeader("第 2.5 步 · 检查字段对应")
    Text(
        "自动检测到的列如下。如果日期或金额对不上，可更换为其他列。",
        style = PdigTokens.Caption,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    MappingColumnField(
        fieldLabel = "日期时间列",
        current = dateColumn,
        candidates = columns.filter { it != amountColumn },
        sample = sample[dateColumn] ?: "（空）",
        onChange = { dateColumn = it; reparse() },
    )
    MappingColumnField(
        fieldLabel = "金额列",
        current = amountColumn,
        candidates = columns.filter { it != dateColumn },
        sample = sample[amountColumn] ?: "（空）",
        onChange = { amountColumn = it; reparse() },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MappingColumnField(
    fieldLabel: String,
    current: String,
    candidates: List<String>,
    sample: String,
    onChange: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(PdigTokens.SpaceXs)) {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = if (current.isBlank()) "未检测到" else "当前：$current 列",
                onValueChange = {},
                readOnly = true,
                label = { Text(fieldLabel) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
                singleLine = true,
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                candidates.forEach { col ->
                    DropdownMenuItem(
                        text = { Text(if (col == current) "$col（当前）" else col) },
                        onClick = {
                            expanded = false
                            if (col != current) onChange(col)
                        },
                    )
                }
            }
        }
        Text(
            "示例值：$sample",
            style = PdigTokens.Caption,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
