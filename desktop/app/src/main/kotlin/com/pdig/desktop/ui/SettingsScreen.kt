package com.pdig.desktop.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.pdig.desktop.data.DesktopSession
import com.pdig.desktop.ui.components.ActionRow
import com.pdig.desktop.ui.components.InfoRow
import com.pdig.desktop.ui.components.PdigPage
import com.pdig.desktop.ui.components.SectionDivider

/** 设置：数据文件信息 + 内存数据清除（危险操作）。 */
@Composable
fun SettingsScreen(ui: UiState) {
    var confirmMode by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    val file = ui.dataFile
    val nodeCount = ui.session.graph.nodes(includeArchived = true).size
    val depCount = ui.session.graph.dependencies().size
    PdigPage(
        title = "设置",
        subtitle = "会话内设置：本预览版不持久化窗口偏好",
        notice = ui.notice,
        error = ui.error,
        onDismissNotice = { ui.notice = null },
        onDismissError = { ui.error = null },
    ) {
        Column {
            SectionDivider("数据文件")
            InfoRow("文件名", file?.name ?: "（未落盘）")
            InfoRow("位置", file?.absolutePath ?: "（仅内存）")
            InfoRow("大小", file?.let { "${it.length()} 字节" } ?: "—")
            InfoRow("节点数（含归档）", nodeCount.toString())
            InfoRow("依赖边数", depCount.toString())
            SectionDivider("功能入口")
            ActionRow(
                title = "备份与恢复",
                description = "把加密的数据文件复制到别处，或从备份文件恢复",
                actionLabel = "打开",
                onAction = { ui.screen = Screen.BACKUP },
            )
            ActionRow(
                title = "恢复数据文件",
                description = "从 .depmap 备份恢复（当前会话被原子替换）",
                actionLabel = "打开",
                onAction = { ui.screen = Screen.RESTORE },
            )
            ActionRow(
                title = "安全",
                description = "本机解锁（记住口令）开关与加密说明",
                actionLabel = "打开",
                onAction = { ui.screen = Screen.SECURITY },
            )
            ActionRow(
                title = "关于",
                description = "版本、构建信息与发布声明",
                actionLabel = "打开",
                onAction = { ui.screen = Screen.ABOUT },
            )
            SectionDivider("窗口（占位说明，仅本次会话）")
            InfoRow("全屏", "窗口由系统控制；本预览版不记忆窗口设置。")
            InfoRow("主题", "跟随系统；不提供持久化主题配置。")
            SectionDivider("危险操作")
            if (!confirmMode) {
                ActionRow(
                    title = "删除全部数据",
                    description = "清除内存中的全部数据并回到打开文件界面。注意：不会自动删除磁盘上的 .depmap 文件。",
                    actionLabel = "继续",
                    onAction = { confirmMode = true },
                )
            } else {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("输入主口令确认（仅作确认仪式，不校验内容）") },
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { wipe(ui) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) { Text("永久删除（内存数据）") }
                TextButton(onClick = { confirmMode = false; password = "" }) { Text("取消") }
            }
        }
    }
}

private fun wipe(ui: UiState) {
    try {
        ui.session.close()
        ui.session = DesktopSession.open()
        ui.dataFile = null
        ui.screen = Screen.HOME
        ui.selectedNodeId = null
        ui.selectedPlanId = null
        ui.selectedScenarioId = null
        ui.notice = "已清空内存数据。磁盘上的 .depmap 文件未被删除。"
    } catch (t: Throwable) {
        ui.showError(t)
    }
}