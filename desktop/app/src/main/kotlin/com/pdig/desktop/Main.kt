package com.pdig.desktop

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
 import com.pdig.desktop.ui.PDIGAppShell
import androidx.compose.ui.window.rememberWindowState

private const val VERSION = "0.1.0"

fun main(args: Array<String>) {
    if (args.contains("--smoke")) {
        smoke(args)?.let { code -> kotlin.system.exitProcess(code) }
        return
    }
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "PDIG 0.1.0 Developer Preview",
            state = rememberWindowState(width = 1100.dp, height = 720.dp),
        ) {
            PDIGAppShell()
        }
    }
}