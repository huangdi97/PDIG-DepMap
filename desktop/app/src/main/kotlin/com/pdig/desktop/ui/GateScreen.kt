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
import com.pdig.desktop.ui.components.NoticeStrip
import com.pdig.desktop.ui.components.PdigPage
import com.pdig.desktop.ui.components.SectionDivider

/** 门（Gate）：新建 / 打开加密 .depmap 数据文件。 */
@Composable
fun GateScreen(ui: UiState) {
    var password by remember {
        mutableStateOf(
            if (ui.unlockStore.enabled()) ui.unlockStore.recall() ?: "" else "",
        )
    }
    PdigPage(
        title = "PDIG 0.1.0 Developer Preview",
        subtitle = "个人数字基础设施图谱",
        notice = ui.notice,
        error = ui.error,
        onDismissNotice = { ui.notice = null },
        onDismissError = { ui.error = null },
    ) {
        Column {
            SectionDivider("数据文件")
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("主口令") },
                singleLine = true,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { createNew(ui, password) }) { Text("新建数据文件") }
                TextButton(onClick = { openExisting(ui, password) }) { Text("打开数据文件") }
            }
            Spacer(Modifier.height(4.dp))
            Text("新建会生成一个空的加密数据文件；打开则解密并载入已有文件。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            NoticeStrip(
                "打开已有文件：.depmap 是加密容器（Argon2id v19 + AES-256-GCM，DEPMAP_CONTAINER_V1），" +
                    "内容永远不是明文。解锁后所有数据只在本机内存中运行。",
            )
        }
    }
}

private fun createNew(ui: UiState, password: String) {
    if (password.isBlank()) {
        ui.error = "请输入主口令。"
        return
    }
    try {
        val file = ui.fileOps.pickSave("新建 PDIG 数据文件", "my-pdig.depmap", "depmap")
        if (file != null) Gate.createNew(ui, file, password)
    } catch (t: Throwable) {
        ui.showError(t)
    }
}

private fun openExisting(ui: UiState, password: String) {
    if (password.isBlank()) {
        ui.error = "请输入主口令。"
        return
    }
    try {
        val file = ui.fileOps.pickOpen("打开 PDIG 数据文件", "depmap", "depmap")
        if (file != null) Gate.open(ui, file, password)
    } catch (t: Throwable) {
        ui.showError(t)
    }
}