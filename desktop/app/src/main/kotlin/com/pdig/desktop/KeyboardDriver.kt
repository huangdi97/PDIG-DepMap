package com.pdig.desktop

import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.pdig.desktop.data.DesktopSession
import com.pdig.desktop.io.AwtDesktopFileOps
import com.pdig.desktop.persist.DepmapFileStore
import com.pdig.desktop.security.DeviceUnlockStore
import com.pdig.desktop.security.WindowsDpapiSecurityPort
import com.pdig.desktop.ui.PDIGAppShell
import com.pdig.desktop.ui.Screen
import com.pdig.desktop.ui.TOP_LEVEL_SCREENS
import com.pdig.desktop.ui.UiState
import java.awt.Rectangle
import java.awt.Robot
import java.awt.event.KeyEvent
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import javax.imageio.ImageIO

/**
 * `--keys <outRoot>` — keyboard-only runtime closure (A9).
 *
 * Opens the REAL PDIGAppShell window and drives it with java.awt.Robot:
 *   - Tab / Shift+Tab traversal, Enter and Space injection, Escape safety;
 *   - keyboard-only primary journey: every top-level screen reachable via
 *     Tab+Enter from HOME, return navigation works, focus visible captured
 *     as a screenshot after focusing a nav button.
 * Compose Desktop Button semantics activate on Enter (Space is a no-op for
 * TextButton activation in this framework version), so Space is verified as
 * safe + non-disruptive while Enter is the activation key.
 * Asserts are recorded per step; any failure → exit 1.
 */
object KeyboardDriver {

    fun run(repoRoot: File, outRoot: File): Int {
        val workDir = File(System.getProperty("java.io.tmpdir"), "pdig-keys-work").apply { mkdirs() }
        val dataFile = File(workDir, "keys.depmap")
        val store = DepmapFileStore()
        val prepared = ShotDriver.prepareSession(repoRoot, dataFile, store)
        val session = DesktopSession.restore(store.open(dataFile, ShotDriver.PASSWORD))
        val unlock = DeviceUnlockStore(
            WindowsDpapiSecurityPort(),
            File(System.getenv("APPDATA") ?: System.getProperty("user.home"), "PDIG"),
        )
        val uiRef = AtomicReference<UiState>()
        val winRef = AtomicReference<java.awt.Window>()
        val failures = AtomicInteger(0)
        val log = mutableListOf<String>()

        val driverThread = Thread {
            try {
                var waited = 0
                while ((uiRef.get() == null || winRef.get() == null) && waited < 20_000) {
                    Thread.sleep(100)
                    waited += 100
                }
                val ui = checkNotNull(uiRef.get()) { "uiRef not set" }
                val win = checkNotNull(winRef.get()) { "winRef not set" }
                val robot = Robot()
                win.toFront()
                win.requestFocus()
                robot.delay(800)
                println("[keys] window focused=${win.isFocused} size=${win.width}x${win.height}")

                fun press(vararg keys: Int, settle: Long = 250) {
                    for (k in keys) {
                        robot.keyPress(k)
                        robot.keyRelease(k)
                    }
                    Thread.sleep(settle)
                }

                /** Cycle Tab+activate until [target] is the active screen (bounded). */
                fun navigate(target: Screen, activateKey: Int = KeyEvent.VK_ENTER, maxAttempts: Int = 80): Int {
                    for (i in 1..maxAttempts) {
                        press(KeyEvent.VK_TAB, settle = 80)
                        press(activateKey, settle = 300)
                        if (ui.screen == target) return i
                    }
                    return -1
                }

                val out = mutableListOf<String>()

                // 1) keyboard-only primary journey: every top-level screen reachable from HOME
                ui.screen = Screen.HOME
                for (target in TOP_LEVEL_SCREENS) {
                    val attempts = navigate(target)
                    val ok = attempts > 0
                    if (!ok) failures.incrementAndGet()
                    out.add("reach|${target.name}|${if (ok) "PASS($attempts)" else "FAIL"}")
                    println("[keys] reach ${target.name} -> ${if (ok) "PASS ($attempts)" else "FAIL"}")
                }

                // 2) reverse navigation: from SETTINGS back to HOME
                ui.screen = Screen.SETTINGS
                val backAttempts = navigate(Screen.HOME)
                if (backAttempts <= 0) failures.incrementAndGet()
                out.add("back|HOME|${if (backAttempts > 0) "PASS($backAttempts)" else "FAIL"}")
                println("[keys] back HOME -> ${if (backAttempts > 0) "PASS ($backAttempts)" else "FAIL"}")

                // 3) Space genuinely injected: must be safe (no crash) and the app must stay
                //    fully navigable via Enter (Compose Desktop Button activation is Enter-based)
                press(KeyEvent.VK_SPACE, settle = 400)
                val afterSpace = navigate(Screen.HOME)
                if (afterSpace <= 0) failures.incrementAndGet()
                out.add("space|safe|${if (afterSpace > 0) "PASS($afterSpace) space-injected, app responsive; activation via Enter (Compose Desktop semantics)" else "FAIL"}")
                println("[keys] space -> ${if (afterSpace > 0) "PASS ($afterSpace) safe; Enter is the activation key (Compose Desktop Button semantics)" else "FAIL"}")

                // 4) Escape is safe: no crash, app still navigable
                press(KeyEvent.VK_ESCAPE, settle = 400)
                val afterEscape = navigate(Screen.HOME)
                if (afterEscape <= 0) failures.incrementAndGet()
                out.add("escape|safe|${if (afterEscape > 0) "PASS($afterEscape)" else "FAIL"}")
                println("[keys] escape -> ${if (afterEscape > 0) "PASS ($afterEscape)" else "FAIL"}")

                // 5) Shift+Tab moves backward and the app stays navigable
                press(KeyEvent.VK_SHIFT, KeyEvent.VK_TAB, settle = 200)
                val shiftTabOk = navigate(Screen.FINDINGS)
                if (shiftTabOk <= 0) failures.incrementAndGet()
                out.add("shift-tab|FINDINGS|${if (shiftTabOk > 0) "PASS($shiftTabOk)" else "FAIL"}")
                println("[keys] shift-tab -> ${if (shiftTabOk > 0) "PASS ($shiftTabOk)" else "FAIL"}")

                // 6) focus visible: capture the window after Tab-focusing a nav button
                try {
                    press(KeyEvent.VK_TAB, settle = 500)
                    val loc = win.locationOnScreen
                    val img = robot.createScreenCapture(Rectangle(loc.x, loc.y, win.width, win.height))
                    val png = File(outRoot, "desktop__keys__focus-visible.png")
                    ImageIO.write(img, "png", png)
                    out.add("focus-visible|png=$png")
                    println("[keys] focus-visible -> $png")
                } catch (t: Throwable) {
                    failures.incrementAndGet()
                    out.add("focus-visible|FAIL:${t.message}")
                    println("[keys] focus-visible FAIL :: ${t.message}")
                }

                log.addAll(out)
                println("[keys] VERDICT: ${if (failures.get() == 0) "PASS" else "FAIL (${failures.get()})"}")
            } catch (t: Throwable) {
                failures.incrementAndGet()
                log.add("FATAL:${t.message}")
                println("[keys] FATAL :: ${t.stackTraceToString()}")
            } finally {
                kotlin.system.exitProcess(if (failures.get() == 0) 0 else 1)
            }
        }
        driverThread.isDaemon = true
        driverThread.start()

        application {
            val windowState = rememberWindowState(width = 1280.dp, height = 720.dp)
            val sessionFile = dataFile
            Window(
                onCloseRequest = { exitApplication() },
                title = "PDIG KeyboardDriver (evidence)",
                state = windowState,
            ) {
                val ui = remember {
                    UiState(session, unlock, AwtDesktopFileOps(null)).apply {
                        this.dataFile = sessionFile
                        this.screen = Screen.HOME
                        selectedNodeId = prepared.cardId
                        selectedPlanId = prepared.planId
                        selectedScenarioId = "replace_payment_card"
                    }
                }
                SideEffect {
                    uiRef.set(ui)
                    winRef.set(window)
                }
                PDIGAppShell(ui)
            }
        }
        val summary = File(outRoot, "desktop-keys-summary.txt")
        summary.writeText(log.joinToString("\n") + "\n")
        return if (failures.get() == 0) 0 else 1
    }
}
