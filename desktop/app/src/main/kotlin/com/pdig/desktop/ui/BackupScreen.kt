package com.pdig.desktop.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pdig.desktop.persist.DepmapFileStore
import com.pdig.desktop.ui.components.EmptyState
import com.pdig.desktop.ui.components.InfoRow
import com.pdig.desktop.ui.components.NoticeStrip
import com.pdig.desktop.ui.components.PdigPage
import com.pdig.desktop.ui.components.SectionDivider
import java.io.File

/** 备份：把**已加密**的 .depmap 容器复制到新位置，绝不产生明文。 */
@Composable
fun BackupScreen(ui: UiState) {
    val file = ui.dataFile
    PdigPage(
        title = "备份",
        subtitle = "备份 = 复制已加密的 .depmap 容器，绝不解密",
        notice = ui.notice,
        error = ui.error,
        onDismissNotice = { ui.notice = null },
        onDismissError = { ui.error = null },
    ) {
        if (file == null) {
            EmptyState("当前会话尚未落盘，没有可备份的文件。请先在首页新建或打开数据文件。")
            return@PdigPage
        }
        Column {
            SectionDivider("当前文件")
            InfoRow("文件名", file.name)
            InfoRow("位置", file.absolutePath)
            InfoRow("大小", "${file.length()} 字节")
            SectionDivider("操作")
            Button(onClick = { backup(ui, file, "pdig-backup-${today()}.depmap") }) { androidx.compose.material3.Text("备份到新位置") }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { backup(ui, file, "pdig-export-${today()}.depmap") }) { androidx.compose.material3.Text("导出为副本") }
            NoticeStrip("导出的文件与源文件一样受主口令加密（Argon2id v19 / AES-256-GCM）。本操作不会解密任何内容。")
        }
    }
}

private fun today(): String =
    java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE)

private fun backup(ui: UiState, source: File, defaultName: String) {
    try {
        val target = ui.fileOps.pickSave("选择备份位置", defaultName, "depmap")
        if (target == null) return
        DepmapFileStore().backupCopy(source, target)
        ui.notice = "备份成功：${target.name}（${target.length()} 字节）"
    } catch (t: Throwable) {
        ui.showError(t)
    }
}