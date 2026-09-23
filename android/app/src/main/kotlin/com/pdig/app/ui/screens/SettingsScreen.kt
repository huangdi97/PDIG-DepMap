package com.pdig.app.ui.screens

import java.io.File
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.pdig.app.data.AppContainer
import com.pdig.app.security.DatabaseKeyStore
import com.pdig.app.security.LockGate
import com.pdig.app.ui.Route
import com.pdig.app.ui.components.PdigCard
import com.pdig.app.ui.components.PdigScrollingPage
import com.pdig.app.ui.components.PdigTopBar
import com.pdig.app.ui.theme.PdigTokens
@Composable
fun SettingsScreen(nav: NavController) {
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }
    Scaffold(topBar = { PdigTopBar("设置", onBack = { nav.popBackStack() }) }) { pad ->
        PdigScrollingPage(
            modifier = Modifier
                .padding(pad)
                .padding(PdigTokens.SpaceLg),
            verticalArrangement = Arrangement.spacedBy(PdigTokens.SpaceMd),
        ) {
            PdigCard(onClick = { nav.navigate(Route.SOURCES) }) { Text("数据来源管理") }
            PdigCard(onClick = { nav.navigate(Route.BACKUP) }) { Text("备份") }
            PdigCard(onClick = { nav.navigate(Route.RESTORE) }) { Text("从备份恢复") }
            // P0-A：把「锁定」做成用户可主动触发的动作。
            // 它只是把 application/security state 置回 LOCKED，
            // 不写数据库、不改 Reality Graph —— 所以这里不需要任何领域逻辑。
            PdigCard(onClick = { LockGate.lockNow() }) {
                Column(verticalArrangement = Arrangement.spacedBy(PdigTokens.SpaceXs)) {
                    Text("立即锁定", style = PdigTokens.BodyStrong)
                    Text(
                        "退出前锁上应用；回到前台时需要重新验证身份。",
                        style = PdigTokens.Caption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            // L-37：删除所有数据。只删除本应用私有数据（DB / 包裹密钥 / 缓存 / 工作流状态），
            // **不删除**用户导出到外部位置的 `.depmap` 备份文件。破坏性操作，必须显式确认。
            PdigCard(onClick = { showDeleteConfirm = true }) {
                Column(verticalArrangement = Arrangement.spacedBy(PdigTokens.SpaceXs)) {
                    Text(
                        "删除所有数据",
                        style = PdigTokens.BodyStrong,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        "删除本机全部记录（数据库、密钥、缓存与工作流状态）。不会删除你导出到外部位置的备份文件。此操作不可恢复。",
                        style = PdigTokens.Caption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            PdigCard(onClick = { nav.navigate(Route.PRIVACY) }) { Text("隐私") }
            PdigCard(onClick = { nav.navigate(Route.ABOUT) }) { Text("关于") }
        }
    }
    if (showDeleteConfirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除所有数据？") },
            text = {
                Text(
                    "将永久删除这台设备上的全部记录（数据库、密钥、缓存与工作流状态）。" +
                        "这不影响你已导出到外部位置的 `.depmap` 备份文件。此操作无法撤销。",
                )
            },
            confirmButton = {
                Button(onClick = {
                    showDeleteConfirm = false
                    // 后台删除私有数据，主线程不被 DB 打开阻塞（L-37/D-12 同口径）。
                    val executor = java.util.concurrent.Executors.newSingleThreadExecutor()
                    executor.execute {
                        try {
                            AppContainer.reset()
                            DatabaseKeyStore.clear(context)
                            // deleteDatabase 在 SQLCipher 直接打开的文件上可能静默失败
                            // （L-37 设备实测），补一层直接删文件（含 WAL/SHM/journal）。
                            context.deleteDatabase("pdig.db")
                            File(context.filesDir, "pdig.db").delete()
                            File(context.filesDir, "pdig.db-journal").delete()
                            File(context.filesDir, "pdig.db-wal").delete()
                            File(context.filesDir, "pdig.db-shm").delete()
                            context.cacheDir?.listFiles()?.forEach { it.delete() }
                            context.filesDir.listFiles()?.forEach { f ->
                                if (f.name != "pdig.db") f.delete()
                            }
                        } finally {
                            executor.shutdown()
                        }
                    }
                    LockGate.lockNow()
                }) { Text("删除", color = MaterialTheme.colorScheme.onError) }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            },
        )
    }
}
