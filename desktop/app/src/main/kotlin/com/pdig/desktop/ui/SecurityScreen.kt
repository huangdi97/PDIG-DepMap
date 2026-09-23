package com.pdig.desktop.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pdig.desktop.ui.components.InfoRow
import com.pdig.desktop.ui.components.NoticeStrip
import com.pdig.desktop.ui.components.PdigPage
import com.pdig.desktop.ui.components.SectionDivider

/** 安全：本机解锁（DPAPI 记住口令）开关 + 加密说明。 */
@Composable
fun SecurityScreen(ui: UiState) {
    var storeEnabled by remember { mutableStateOf(ui.unlockStore.enabled()) }
    var showPassword by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    PdigPage(
        title = "安全",
        subtitle = "本机解锁只是免输入便利，绝不降低 .depmap 的加密强度",
        notice = ui.notice,
        error = ui.error,
        onDismissNotice = { ui.notice = null },
        onDismissError = { ui.error = null },
    ) {
        Column {
            SectionDivider("本机解锁")
            InfoRow("平台保护", "Windows DPAPI（CurrentUser 作用域，操作系统持钥）")
            InfoRow("状态", if (storeEnabled) "已启用（存在 device-unlock.blob）" else "未启用")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = storeEnabled,
                    onCheckedChange = { checked ->
                        if (checked) {
                            showPassword = true
                        } else {
                            showPassword = false
                            password = ""
                            try {
                                ui.unlockStore.forget()
                                storeEnabled = false
                                ui.notice = "已忘记本机解锁。之后每次打开文件都需要手动输入口令。"
                            } catch (t: Throwable) {
                                ui.showError(t)
                            }
                        }
                    },
                )
                Text("记住本机解锁（下次打开自动填入口令）")
            }
            if (showPassword) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("重新输入当前主口令") },
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        try {
                            ui.unlockStore.remember(password)
                            storeEnabled = true
                            showPassword = false
                            password = ""
                            ui.notice = "已记住本机解锁（DPAPI 保护，仅当前 Windows 用户可读）。"
                        } catch (t: Throwable) {
                            ui.showError(t)
                        }
                    },
                    enabled = password.isNotBlank(),
                ) { Text("确认记住") }
            }
            SectionDivider("加密说明")
            NoticeStrip(
                "DPAPI 只保存口令的受保护副本，用于本机免输输入。.depmap 数据文件本身始终由主口令加密：" +
                    "Argon2id v19 派生密钥 + AES-256-GCM（DEPMAP_CONTAINER_V1），与是否开启「记住」无关。",
            )
        }
    }
}