package com.pdig.desktop

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.pdig.desktop.data.DesktopSession
import com.pdig.desktop.io.AwtDesktopFileOps
import com.pdig.desktop.security.DeviceUnlockStore
import com.pdig.desktop.security.WindowsDpapiSecurityPort
import com.pdig.desktop.ui.Gate
import com.pdig.desktop.ui.PDIGAppShell
import com.pdig.desktop.ui.UiState
import java.io.File

private const val VERSION = "0.1.1"

fun main(args: Array<String>) {
    if (args.contains("--smoke")) {
        val repo = findRepoRoot(File(".").absoluteFile)
            ?: error("--smoke 需要在仓库内运行以读取 fixtures/（未找到 fixtures/import/normal-wechat.csv）")
        val work = File(System.getenv("PI_SCRATCH_DIR") ?: System.getProperty("java.io.tmpdir"), "pdig-smoke")
        work.mkdirs()
        kotlin.system.exitProcess(SmokeRunner.run(repo, work))
        return
    }
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "PDIG $VERSION Developer Preview",
            state = rememberWindowState(width = 1100.dp, height = 720.dp),
        ) {
            val ui = rememberUiState()
            PDIGAppShell(ui)
        }
    }
}

/** 建一个 UiState：内存会话 + Windows DPAPI 解锁存储 + 原生文件对话框。 */
@androidx.compose.runtime.Composable
fun rememberUiState(): UiState {
    val session = androidx.compose.runtime.remember { DesktopSession.open() }
    val unlock = androidx.compose.runtime.remember {
        val appData = File(System.getenv("APPDATA") ?: System.getProperty("user.home"), "PDIG")
        DeviceUnlockStore(WindowsDpapiSecurityPort(), appData)
    }
    val fileOps = androidx.compose.runtime.remember { AwtDesktopFileOps(null) }
    return androidx.compose.runtime.remember { UiState(session, unlock, fileOps) }
}

/** 从当前目录向上找仓库根（含 fixtures/import/normal-wechat.csv 的目录）。 */
fun findRepoRoot(start: File): File? =
    generateSequence(start) { it.parentFile }
        .firstOrNull { File(it, "fixtures/import/normal-wechat.csv").isFile }
