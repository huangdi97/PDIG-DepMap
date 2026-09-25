package com.pdig.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.pdig.app.data.SourceRow
import com.pdig.app.ui.components.EmptyState
import com.pdig.app.ui.components.LoadingState
import com.pdig.app.ui.components.PdigCard
import com.pdig.app.ui.components.SectionHeader
import com.pdig.app.ui.theme.PdigTokens

/** 导入第 1 步：选择数据来源（列表或新建）。从 ImportScreen 抽出以保持 composable ≤200 行。 */
@Composable
internal fun ImportSourcePickerStep(
    sources: List<SourceRow>?,
    selectedSourceId: String?,
    newSourceName: String,
    onSourceClick: (String) -> Unit,
    onNewNameChange: (String) -> Unit,
) {
    SectionHeader("第 1 步 · 选择数据来源")
    when {
        sources == null -> LoadingState()
        sources.isNotEmpty() -> sources.forEach { s ->
            PdigCard(onClick = { onSourceClick(s.id) }) {
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
        onValueChange = onNewNameChange,
        label = { Text("或新建来源名称") },
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "新建数据来源名称" },
        singleLine = true,
    )
}

/** 导入第 2 步：选择文件并解析（含忙碌/错误文本展示）。 */
@Composable
internal fun ImportFilePickerStep(
    enabled: Boolean,
    busy: Boolean,
    statusText: String?,
    onPickFile: () -> Unit,
) {
    // 交给 Activity 作用域的 coordinator 拉起外部文件选择器：
    // launcher 注册在 MainActivity，不会因为本页离开组合树而被注销（D-16）。
    SectionHeader("第 2 步 · 选择文件")
    Button(
        onClick = onPickFile,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = PdigTokens.MinTouchTarget)
            .testTag(IMPORT_PICK_FILE_TAG),
        enabled = enabled,
    ) { Text("选择文件并解析") }
    if (busy) LoadingState()
    statusText?.let { Text(it, style = PdigTokens.BodyStrong) }
}
