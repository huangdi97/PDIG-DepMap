package com.pdig.desktop

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.pdig.desktop.data.DesktopSession
import com.pdig.desktop.io.AwtDesktopFileOps
import com.pdig.desktop.security.DeviceUnlockStore
import com.pdig.desktop.security.WindowsDpapiSecurityPort
import com.pdig.desktop.ui.PDIGAppShell
import com.pdig.desktop.ui.UiState
import com.pdig.desktop.ui.theme.PDIGTheme
import java.io.File

private const val VERSION = "0.3.1"

fun main(args: Array<String>) {
    if (args.contains("--smoke")) {
        val repo = findRepoRoot(File(".").absoluteFile)
            ?: error("--smoke 需要在仓库内运行以读取 fixtures/（未找到 fixtures/import/normal-wechat.csv）")
        val work = File(System.getenv("PI_SCRATCH_DIR") ?: System.getProperty("java.io.tmpdir"), "pdig-smoke")
        work.mkdirs()
        kotlin.system.exitProcess(SmokeRunner.run(repo, work))
        return
    }
    // --shots <evidence dir>：multiclient runtime sweep 的桌面可视化取证（见 ShotDriver.kt）
    if (args.contains("--shots")) {
        val repo = findRepoRoot(File(".").absoluteFile)
            ?: error("--shots 需要在仓库内运行以读取 fixtures/")
        val outRoot = File(repo, "artifacts/runtime-evidence/2026-09-26-multiclient-sweep")
        outRoot.mkdirs()
        kotlin.system.exitProcess(ShotDriver.run(repo, outRoot))
        return
    }

    // --vnext：UI vNext 演示壳（Presentation Layer，fixture 驱动）
    if (args.contains("--vnext")) {
        val camera = args.firstOrNull { it.startsWith("--vnext-camera=") }?.substringAfter("=")
        val app = com.pdig.uivnext.createVNextAppState(cameraPreset = camera)
        application {
            Window(
                onCloseRequest = ::exitApplication,
                title = "PDIG vNext Preview",
                state = rememberWindowState(width = 1920.dp, height = 1080.dp),
            ) {
                com.pdig.uivnext.VNextApp(app)
            }
        }
        return
    }
    // --vnext-shots：vNext 离屏确定性截图（无窗口依赖）+ UI_LAYOUT_PROBE.json
    if (args.contains("--vnext-shots")) {
        val repo = findRepoRoot(File(".").absoluteFile) ?: File(".")
        val outRoot = File(repo, "artifacts/runtime-evidence/2026-10-01-ui-vnext-phase1")
        outRoot.mkdirs()
        kotlin.system.exitProcess(com.pdig.uivnext.evidence.VNextShotDriver.runAll(outRoot))
        return
    }
    // --vnext-shots-1b：PHASE 1B 关键帧（Review §22，15 张 1920×1080 最小证据集）
    if (args.contains("--vnext-shots-1b")) {
        val repo = findRepoRoot(File(".").absoluteFile) ?: File(".")
        val outRoot = File(repo, "artifacts/runtime-evidence/2026-10-02-ui-vnext-phase1b")
        outRoot.mkdirs()
        kotlin.system.exitProcess(com.pdig.uivnext.evidence.VNextShotDriver.runPhase1B(outRoot))
        return
    }
    // --profiles / --keys：v0.3.0 closure 的桌面分辨率/缩放/键盘取证（见 ProfileDriver.kt / KeyboardDriver.kt）
    if (args.contains("--profiles") || args.contains("--keys")) {
        val repo = findRepoRoot(File(".").absoluteFile)
            ?: error("desktop closure 需要在仓库内运行以读取 fixtures/")
        // UIUX 精修轮：允许用环境变量把取证输出重定向到新的证据目录，
        // 默认行为不变（既有调用方照旧写回历史 closure 目录）。
        val overrideRoot = System.getenv("PDIG_UIUX_EVIDENCE_ROOT")?.takeIf { it.isNotBlank() }
        val outRoot =
            if (overrideRoot != null) File(overrideRoot)
            else File(repo, "artifacts/runtime-evidence/2026-09-27-closure-desktop")
        outRoot.mkdirs()
        val exit =
            if (args.contains("--profiles")) ProfileDriver.run(repo, outRoot)
            else KeyboardDriver.run(repo, outRoot)
        kotlin.system.exitProcess(exit)
        return
    }
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "PDIG $VERSION Preview",
            state = rememberWindowState(width = 1100.dp, height = 720.dp),
        ) {
            val ui = rememberUiState()
            PDIGTheme {
                PDIGAppShell(ui)
            }
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