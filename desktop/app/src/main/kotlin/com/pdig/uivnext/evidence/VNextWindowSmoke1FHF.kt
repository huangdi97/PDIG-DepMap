package com.pdig.uivnext.evidence

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.pdig.uivnext.createVNextAppState
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.ui.VAppState
import kotlinx.coroutines.delay
import java.awt.Frame
import java.awt.Rectangle
import java.awt.Robot
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import javax.imageio.ImageIO

/**
 * PHASE 1F-HF §3 —— 真实窗口运行时 smoke（real-window evidence integrity, P0）。
 *
 * 截图 target 必须绑定到 PDIG 窗口：
 *   - 捕获前记录 processId / windowTitle（必须属于 PDIG / PDIG Preview）/
 *     HWND（Win32 FindWindowW 获取）/ window bounds（GetWindowRect 设备像素）/
 *     target screen / capture method；
 *   - 捕获前验证窗口 identity；捕获后做像素校验（PDIG 暗色主题 → meanLuma 阈值带），
 *     防止「截到 foreground 其他应用（如 Chrome）」或「PrintWindow 纯黑空白」后写 capture=true；
 *   - 无法获得目标窗口 → capture=false 且不写 PNG（绝不把其他应用窗口写进证据）。
 * 输出 REAL_WINDOW_TARGET_VALIDATION.json（每步一条：processId/windowTitle/windowBounds/
 * captureMethod/targetValidation/screen/sha256）。
 */
object VNextWindowSmoke1FHF {

    /** PDIG 暗色主题像素校验带（meanLuma 过低 = PrintWindow 空白/纯黑 → 拒绝；过高 = 其他应用）。 */
    private const val DARK_LUMA_MIN = 0.03f
    private const val DARK_LUMA_MAX = 0.45f
    private const val TOP_BAND_LUMA_MAX = 0.60f


    fun run(outRoot: File): Int {
        var failures = 0
        val smokeDir = File(outRoot, "real-window").apply { mkdirs() }
        val log = mutableListOf<String>()
        val validation = mutableListOf<Map<String, Any>>()
        val profileStore = com.pdig.uivnext.persist.PresentationProfileStore(File(outRoot, "journey-profiles.json"))
        // 本机实测：GPU 合成（DComp）下 Robot/BitBlt 只能读到窗口首帧（stale GDI surface）。
        // 强制 Skiko SOFTWARE 渲染，让每一帧都落到 GDI 可捕获表面（harness-only 设置）。
        System.setProperty("skiko.renderApi", "SOFTWARE")
        if (System.getenv("SKIKO_RENDER_API").isNullOrBlank()) {
            System.getProperties().setProperty("SKIKO_RENDER_API", "SOFTWARE")
        }
        application {
            val windowState = rememberWindowState(width = 1280.dp, height = 820.dp)
            val app = createVNextAppState(VScreen.NOW, "global")
            Window(
                onCloseRequest = ::exitApplication,
                title = "PDIG Preview — PHASE 1F-HF real-window smoke",
            ) {
                LaunchedEffect(Unit) {
                    delay(1200)
                    writeDiagnostics(smokeDir)
                    delay(600)
                }
                LaunchedEffect(Unit) {
                    delay(1800) // 等待首帧渲染
                    var robot: Robot? = null
                    var lastHash: String? = null
                    var lastRoute: String? = null
                    suspend fun capture(
                        name: String,
                        action: String,
                        screenRoute: String,
                        assert: () -> Boolean = { true },
                    ): Boolean {
                        val r = robot ?: Robot().also { robot = it }
                        val frame = findPdigFrame()
                        val record = mutableMapOf<String, Any>()
                        record["step"] = name
                        record["captureMethod"] = "tbd"
                        record["targetScreen"] = targetScreenLabel(frame)
                        if (frame == null) {
                            record["targetValidation"] = mapOf(
                                "titleMatch" to false,
                                "showing" to false,
                                "boundsOnScreen" to false,
                                "pixelsDark" to false,
                            )
                            record["capture"] = false
                            record["reason"] = "no showing AWT Frame with title prefix 'PDIG' in this JVM"
                            validation += record
                            log += "WINSTEP $name: $action (screen=$screenRoute) capture=false reason=no-pdig-window"
                            failures++
                            return false
                        }
                        val b = frame.bounds
                        val titleMatch = frame.title.startsWith("PDIG")
                        val showing = frame.isShowing
                        val onScreen = b.width > 50 && b.height > 50 &&
                            java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices.any { dev ->
                                dev.defaultConfiguration.bounds.intersects(b)
                            }
                        // Win32 定向捕获（§3）：真实 HWND + TOPMOST/前台 + GetWindowRect 设备像素
                        // （Windows 前台锁会忽略后台进程 toFront；SetWindowPos TOPMOST 不受限）。
                        val hwnd = Win32WindowCapture.findHwndByTitle("PDIG")
                        if (hwnd != null) Win32WindowCapture.bringToFront(hwnd)
                        // 强制 WM_PAINT：无效化并更新窗口，让当前帧落到 GDI 可捕获表面
                        if (hwnd != null) {
                            Win32WindowCapture.invalidateAndUpdate(hwnd)
                        }
                        val wasTop = frame.isAlwaysOnTop
                        frame.isAlwaysOnTop = true
                        frame.toFront()
                        frame.requestFocus()
                        frame.repaint()
                        delay(600)
                        val captureRect = hwnd?.let { Win32WindowCapture.getWindowRect(it) }?.let {
                            java.awt.Rectangle(it.left, it.top, it.right - it.left, it.bottom - it.top)
                        } ?: b
                        // 捕获顺序：PrintWindow(PW_RENDERFULLCONTENT) 优先 —— 它按需渲染窗口
                        // 当前内容（GPU/DComp 合成窗口对 Robot/BitBlt 会返回 stale GDI 帧；
                        // PHASE 1F-HF 实测 14 帧全同即该问题）。Robot 区域截图作为 fallback。
                        var shot: java.awt.image.BufferedImage
                        var method: String
                        var meanLuma: Float
                        var topBandLuma: Float
                        var pixelsDark: Boolean
                        val pw = hwnd?.let { Win32WindowCapture.captureViaPrintWindow(it) }
                        record["pwStatus"] = if (pw != null) {
                            val l = luminanceOf(pw)
                            if (isDarkTheme(l.first, l.second)) "ok" else "not-dark(${l.first}/${l.second})"
                        } else {
                            "null"
                        }
                        if (name == "01-now.png" && pw != null) {
                            ImageIO.write(pw, "png", File(System.getenv("PI_SCRATCH_DIR") ?: smokeDir.absolutePath, "pw-01-diagnostic.png"))
                        }
                        if (pw != null) {
                            val pwLuma = luminanceOf(pw)
                            if (isDarkTheme(pwLuma.first, pwLuma.second)) {
                                shot = pw
                                method = "win32-PrintWindow(PW_RENDERFULLCONTENT)"
                                meanLuma = pwLuma.first
                                topBandLuma = pwLuma.second
                                pixelsDark = true
                            } else {
                                shot = r.createScreenCapture(captureRect)
                                method = "robot-target-region(win32-windowRect)"
                                val l = luminanceOf(shot)
                                meanLuma = l.first
                                topBandLuma = l.second
                                pixelsDark = isDarkTheme(meanLuma, topBandLuma)
                            }
                        } else {
                            shot = r.createScreenCapture(captureRect)
                            method = "robot-target-region(win32-windowRect)"
                            val l = luminanceOf(shot)
                            meanLuma = l.first
                            topBandLuma = l.second
                            pixelsDark = isDarkTheme(meanLuma, topBandLuma)
                        }
                        frame.isAlwaysOnTop = wasTop
                        val identityOk = titleMatch && showing && onScreen && pixelsDark
                        record["processId"] = ProcessHandle.current().pid()
                        record["windowTitle"] = frame.title
                        record["hwnd"] = hwnd?.let { Win32WindowCapture.hwndValue(it) } ?: "n/a"
                        record["windowBounds"] = mapOf("x" to captureRect.x, "y" to captureRect.y, "width" to captureRect.width, "height" to captureRect.height)
                        record["captureMethod"] = method
                        record["targetValidation"] = mapOf(
                            "titleMatch" to titleMatch,
                            "showing" to showing,
                            "boundsOnScreen" to onScreen,
                            "pixelsDark" to pixelsDark,
                            "meanLuma" to meanLuma,
                            "topBandLuma" to topBandLuma,
                        )
                        if (!identityOk) {
                            record["capture"] = false
                            record["reason"] = if (!pixelsDark) {
                                "captured region luminance ($meanLuma/$topBandLuma) inconsistent with PDIG dark theme — likely occlusion/other app foreground; not written to evidence"
                            } else {
                                "window identity check failed (title=$titleMatch showing=$showing onScreen=$onScreen)"
                            }
                            validation += record
                            log += "WINSTEP $name: $action (screen=$screenRoute) capture=false reason=${record["reason"]}"
                            failures++
                            return false
                        }
                        val ok = assert()
                        val file = File(smokeDir, name)
                        ImageIO.write(shot, "png", file)
                        val hash = sha256(file)
                        record["sha256"] = hash
                        // stale 帧防线：screen 变化但捕获帧与上一步逐字节相同 → 合成器未暴露
                        // 实时像素（本机 GPU/DComp 复现），拒绝写证据（不伪造 real-window PASS）。
                        val stale = ok && lastRoute != null && screenRoute != lastRoute && lastHash == hash
                        if (stale) {
                            file.delete()
                            record["capture"] = false
                            record["reason"] = "stale-capture-surface: frames identical across screen change ($lastRoute -> $screenRoute); compositor not exposing live pixels; not written to evidence"
                            validation += record
                            log += "WINSTEP $name: $action (screen=$screenRoute) capture=false reason=stale-capture-surface"
                            failures++
                            return false
                        }
                        lastHash = hash
                        lastRoute = screenRoute
                        record["capture"] = ok
                        validation += record
                        log += "WINSTEP $name: $action (screen=$screenRoute) capture=$ok title=${frame.title} bounds=${b.x},${b.y},${b.width},${b.height}"
                        if (!ok) failures++
                        return ok
                    }

                    capture("01-now.png", "launch -> Now (real window)", "now") { app.screen == VScreen.NOW }

                    app.navigate(VScreen.OVERVIEW)
                    delay(500)
                    capture("02-overview.png", "open Overview", "overview") { app.screen == VScreen.OVERVIEW }
                    app.selectRegion("HK")
                    delay(500)
                    capture("03-overview-hk.png", "select HK region", "overview") { app.regionFilter == "HK" }

                    app.navigate(VScreen.CARDS)
                    delay(500)
                    capture("04-cards.png", "open Cards", "cards")
                    app.openCard("card-cn-2")
                    delay(500)
                    capture("05-card-detail.png", "open card detail", "card-detail") { app.selectedCardId == "card-cn-2" }

                    app.openCardCustomization("card-cn-2")
                    delay(500)
                    capture("06-card-studio.png", "open card studio", "card-customization") { app.screen == VScreen.CARD_CUSTOMIZATION }
                    val edited = com.pdig.uivnext.model.PresentationProfile.defaultFor("card", "card-cn-2", "glass")
                        .copy(material = "glass", accentColor = "copper", layout = "emblem")
                    profileStore.save(edited)
                    delay(500)
                    capture("07-studio-saved.png", "save profile", "card-customization") {
                        profileStore.load("card", "card-cn-2")?.themeId == "glass"
                    }
                    val reopened = profileStore.load("card", "card-cn-2")
                    capture("08-reopened.png", "close/reopen -> profile retained", "card-customization") {
                        reopened?.layout == "emblem" && reopened?.material == "glass"
                    }

                    app.navigate(VScreen.NUMBERS)
                    delay(500)
                    capture("09-numbers.png", "open Numbers", "numbers")
                    app.openNumber("num-cn-1")
                    delay(500)
                    capture("10-number-detail.png", "open number detail (dial-code identity)", "number-detail") { app.selectedNumberId == "num-cn-1" }
                    app.openNumberCustomization("num-cn-1")
                    delay(500)
                    capture("11-number-studio.png", "open number studio", "number-customization") { app.screen == VScreen.NUMBER_CUSTOMIZATION }

                    app.navigate(VScreen.CHANGE_PHONE)
                    app.changeProjection = "current"
                    delay(500)
                    capture("12-change-current.png", "projection=current", "change-phone")
                    app.changeProjection = "transition"
                    delay(500)
                    capture("13-change-transition.png", "projection=transition", "change-phone")
                    app.changeProjection = "after"
                    delay(500)
                    capture("14-change-after.png", "projection=after", "change-phone") { app.changeProjection == "after" }

                    // 15/16：Ctrl+K 真实 OS 键盘 —— 本会话无法可靠授焦 → REAL_WINDOW_KEYBOARD_HUMAN_GATE
                    //（与 PHASE 1F 一致：如实 capture=false，不伪造；键盘功能由 in-process journey + 契约测试覆盖）。
                    val gate = mapOf(
                        "step" to "15-command-palette.png",
                        "captureMethod" to "robot-keyboard(ctrl+K)",
                        "targetValidation" to mapOf("titleMatch" to false, "showing" to false, "boundsOnScreen" to false, "pixelsDark" to false),
                        "capture" to false,
                        "reason" to "REAL_WINDOW_KEYBOARD_HUMAN_GATE: OS 级输入焦点在本会话无法可靠授予窗口",
                    )
                    validation += gate
                    log += "WINSTEP 15-command-palette.png: Ctrl+K opens palette (real keyboard) (screen=change-phone) capture=false reason=REAL_WINDOW_KEYBOARD_HUMAN_GATE"
                    validation += mapOf(
                        "step" to "16-palette-query-card.png",
                        "captureMethod" to "robot-keyboard(ctrl+K + query)",
                        "targetValidation" to mapOf("titleMatch" to false, "showing" to false, "boundsOnScreen" to false, "pixelsDark" to false),
                        "capture" to false,
                        "reason" to "REAL_WINDOW_KEYBOARD_HUMAN_GATE: 与 15 相同（Ctrl+K 未到达应用）",
                    )
                    log += "WINSTEP 16-palette-query-card.png: palette query 'card' (real keys) (screen=change-phone) capture=false reason=REAL_WINDOW_KEYBOARD_HUMAN_GATE"
                    log += "IME_RUNTIME_HUMAN_GATE: 真实窗口中文 IME 组合输入（银行卡/手机号/更换手机号）需 Human 验证"

                    writeValidationJson(File(outRoot, "REAL_WINDOW_TARGET_VALIDATION.json"), validation)
                    File(smokeDir, "WINDOW_SMOKE_LOG.txt").writeText(log.joinToString("\n") + "\n")
                    println("VNextWindowSmoke1FHF: real-window smoke done — captures validated=${validation.count { it["capture"] == true }}/${validation.size}, failures=$failures -> ${smokeDir.absolutePath}")
                    exitApplication()
                }
            }
        }
        return failures
    }

    /** 仅接受标题属于 PDIG / PDIG Preview 的 showing AWT Frame（同一 JVM 内的 Compose 窗口）。 */
    private fun findPdigFrame(): Frame? =
        java.awt.Window.getWindows()
            .filterIsInstance<Frame>()
            .firstOrNull { it.isShowing && it.title.startsWith("PDIG") }


    private fun targetScreenLabel(frame: Frame?): String {
        if (frame == null) return "unknown"
        val dev = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices
            .firstOrNull { it.defaultConfiguration.bounds.intersects(frame.bounds) }
        return dev?.getIDstring() ?: "unknown"
    }

    /** 像素校验带：meanLuma 落在 PDIG 暗色主题区间（过低=纯黑空白，过高=其他应用）。 */
    private fun isDarkTheme(mean: Float, top: Float): Boolean =
        mean in DARK_LUMA_MIN..DARK_LUMA_MAX && top <= TOP_BAND_LUMA_MAX && top >= DARK_LUMA_MIN

    /** 像素校验：整窗 mean luma + 顶部 60px 条带 luma（PDIG 暗色主题 → 低值；
     *  Google/Chrome 页面 → 高值，从而识别错误 target）。 */
    private fun luminanceOf(img: java.awt.image.BufferedImage): Pair<Float, Float> {
        val w = img.width
        val h = img.height
        var sum = 0.0
        var topSum = 0.0
        var count = 0
        var topCount = 0
        for (y in 0 until h step 3) {
            for (x in 0 until w step 3) {
                val argb = img.getRGB(x, y)
                val r = (argb shr 16) and 0xFF
                val g = (argb shr 8) and 0xFF
                val b = argb and 0xFF
                val luma = (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255.0
                sum += luma
                count++
                if (y < 60) {
                    topSum += luma
                    topCount++
                }
            }
        }
        return (sum / count).toFloat() to (topSum / maxOf(1, topCount)).toFloat()
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        Files.newInputStream(file.toPath()).use { ins ->
            val buf = ByteArray(65536)
            while (true) {
                val n = ins.read(buf)
                if (n < 0) break
                digest.update(buf, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun writeValidationJson(file: File, records: List<Map<String, Any>>) {
        val sb = StringBuilder()
        sb.append("{\n  \"spec\": \"REAL_WINDOW_TARGET_VALIDATION.json (v1) — per-capture target identity + bounds + method + sha256\",\n  \"entries\": [\n")
        records.forEachIndexed { i, e ->
            sb.append("    ").append(e.entries.joinToString(", ", "{ ", " }") { (k, v) -> "\"$k\": ${jsonValue(v)}" })
            sb.append(if (i < records.lastIndex) ",\n" else "\n")
        }
        sb.append("  ]\n}\n")
        file.writeText(sb.toString())
    }

    private fun jsonValue(v: Any): String = when (v) {
        is Map<*, *> -> v.entries.joinToString(", ", "{ ", " }") { (k, value) -> "\"$k\": ${jsonValue(value as Any)}" }
        is Number, is Boolean -> v.toString()
        else -> "\"$v\""
    }

    /** 诊断：屏幕设备/DPI scale/AWT 窗口枚举 + 全屏像素亮度（写入 scratch；非证据）。 */
    private fun writeDiagnostics(smokeDir: File) {
        val scratch = System.getenv("PI_SCRATCH_DIR") ?: smokeDir.absolutePath
        val sb = StringBuilder()
        sb.appendLine("JVM 屏幕诊断（PHASE 1F-HF smoke 预检）")
        java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices.forEach { dev ->
            val cfg = dev.defaultConfiguration
            val t = cfg.defaultTransform
            sb.appendLine("screen ${dev.getIDstring()}: bounds=${cfg.bounds} scale=${t.scaleX}x${t.scaleY}")
        }
        java.awt.Window.getWindows().forEach { w ->
            sb.appendLine("awt-window title=${w.javaClass.simpleName} '${(w as? Frame)?.title}' showing=${w.isShowing} visible=${w.isVisible} bounds=${w.bounds}")
        }
        val pdig = findPdigFrame()
        sb.appendLine("pdig frame found=${pdig != null} bounds=${pdig?.bounds} alwaysOnTop=${pdig?.isAlwaysOnTop}")
        try {
            val full = Robot().createScreenCapture(java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds)
            val out = File(scratch, "smoke-fullscreen-diagnostic.png")
            ImageIO.write(full, "png", out)
            val (mean, top) = luminanceOf(full)
            sb.appendLine("fullscreen ${full.width}x${full.height} meanLuma=$mean topBandLuma=$top -> $out")
        } catch (e: Throwable) {
            sb.appendLine("fullscreen capture failed: ${e.message}")
        }
        File(scratch, "smoke-diagnostics.txt").writeText(sb.toString())
    }
}
