package com.pdig.desktop.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pdig.desktop.ui.components.EmptyState
import com.pdig.desktop.ui.components.InfoRow
import com.pdig.desktop.ui.components.NoticeStrip
import com.pdig.desktop.ui.components.PdigPage
import com.pdig.desktop.ui.components.SectionDivider
import java.io.File

/** 恢复：打开 .depmap 备份并原子替换当前会话。 */
@Composable
fun RestoreScreen(ui: UiState) {
    var file by remember { mutableStateOf<File?>(null) }
    var password by remember { mutableStateOf("") }
    PdigPage(
        title = "恢复",
        subtitle = "从 .depmap 备份恢复（当前会话会被原子替换）",
        notice = ui.notice,
        error = ui.error,
        onDismissNotice = { ui.notice = null },
        onDismissError = { ui.error = null },
    ) {
        Column {
            SectionDivider("选择备份文件")
            Button(onClick = {
                val picked = ui.fileOps.pickOpen("打开备份文件", "depmap", "depmap")
                if (picked != null) file = picked
            }) { Text("打开备份文件") }
            val f = file
            if (f != null) {
                InfoRow("已选择", f.name)
                InfoRow("位置", f.absolutePath)
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("主口令") },
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = { restore(ui, f, password) }, enabled = password.isNotBlank()) {
                    Text("确认恢复")
                }
            } else {
                EmptyState("尚未选择备份文件。")
            }
            NoticeStrip("提示：恢复会用备份内容替换当前打开的数据。建议先前往「备份」保存当前文件的副本。")
        }
    }
}

private fun restore(ui: UiState, file: File, password: String) {
    try {
        Gate.open(ui, file, password)
        ui.notice = "已从备份恢复：${file.name}"
    } catch (t: Throwable) {
        ui.showError(t)
    }
}