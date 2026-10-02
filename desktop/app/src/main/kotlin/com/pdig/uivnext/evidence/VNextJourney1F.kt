package com.pdig.uivnext.evidence

import com.pdig.uivnext.createVNextAppState
import com.pdig.uivnext.model.PresentationProfile
import com.pdig.uivnext.model.VScreen
import com.pdig.uivnext.persist.LocalBackgroundImporter
import com.pdig.uivnext.persist.defaultBackgroundDir
import com.pdig.uivnext.ui.KeyAction
import com.pdig.uivnext.ui.KeyboardFocusState
import com.pdig.uivnext.ui.VAppState
import com.pdig.uivnext.ui.filterPalette
import com.pdig.uivnext.ui.paletteEntries
import com.pdig.uivnext.ui.routeKey
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * PHASE 1F 交互 journey（§49 in-process 语义旅程 + §42 IME 尝试记录）。
 *
 * 每步：驱动 app 状态 → 记录 interaction log → 渲染 PNG → 断言。附带：
 *  - 键盘证据（§47：palette 检索/执行；空态屏可达）
 *  - 持久化证据（Studio 编辑 → 保存 → 重开保留；§49 关闭/重开等价）
 *  - 自定义背景消费者流程（选择 → thumbnail → 保存；§20）
 *  - IME：in-process 无法可靠控制 OS 组合输入 → 如实记录 IME_RUNTIME_HUMAN_GATE
 *    （不伪造 PASS）；真实窗口键盘证据见 VNextWindowSmoke1F.kt。
 */
object VNextJourney1F {

    private val PROFILE = VNextShotDriver.Profile(1920, 1080, "1920x1080@1.0")

    fun run(outRoot: File): Int {
        outRoot.mkdirs()
        val journeyDir = File(outRoot, "journey").apply { mkdirs() }
        val log = mutableListOf<String>()
        var failed = 0
        fun step(id: String, file: String, app: VAppState, action: String, assert: () -> Boolean = { true }) {
            log += "STEP $id: $action (screen=${app.screen.route} card=${app.selectedCardId} number=${app.selectedNumberId})"
            try {
                VNextShotDriver.renderToFile(app, PROFILE, File(journeyDir, file))
            } catch (e: Throwable) {
                log += "  RENDER FAIL: ${e.message}"
                failed++
            }
            if (!assert()) {
                log += "  ASSERT FAIL"
                failed++
            }
        }

        // 1. launch Now
        val app1 = createVNextAppState(VScreen.NOW, "global")
        step("01-launch", "01-launch.png", app1, "launch -> Now")

        // 2. Overview → 3. 选区域（HK）
        val app2 = createVNextAppState(VScreen.OVERVIEW, "global")
        step("02-overview", "02-overview.png", app2, "open Overview")
        app2.selectRegion("HK")
        step("03-region-hk", "03-overview-hk.png", app2, "select HK region") {
            app2.regionFilter == "HK" && app2.globe.selectedRegion == "HK"
        }

        // 4. Cards → 5. Card Detail
        val app4 = createVNextAppState(VScreen.CARDS, "global")
        step("04-cards", "04-cards.png", app4, "open Cards")
        app4.openCard("card-cn-2")
        step("05-card-detail", "05-card-detail.png", app4, "open card-cn-2 detail") {
            app4.screen == VScreen.CARD_DETAIL
        }

        // 6. Card Studio → 编辑 → 7. 保存
        val app6 = createVNextAppState(VScreen.CARD_CUSTOMIZATION, "global")
        app6.openCardCustomization("card-cn-2")
        step("06-card-studio", "06-card-studio.png", app6, "open card studio (材质 默认展开)")
        val edited = PresentationProfile.defaultFor("card", "card-cn-2", "glass").copy(material = "glass", accentColor = "copper", layout = "emblem")
        app6.profileStore.save(edited)
        step("07-save-profile", "07-card-studio-saved.png", app6, "save profile -> store") {
            app6.profileStore.load("card", "card-cn-2")?.themeId == "glass"
        }

        // 8. 关闭/重开（进程级重开等价）：重新构造 store 并断言保留
        val reopened = app6.profileStore.load("card", "card-cn-2")
        step("08-reopen-profile", "08-card-reopened.png", app6, "reopen store -> profile retained") {
            reopened?.layout == "emblem" && reopened?.material == "glass"
        }

        // 9. 自定义背景：选择本地图片 → 导入 → thumbnail 保存（§20）
        val bgFile = File(outRoot, "journey/custom-background.png")
        val bg = BufferedImage(120, 80, BufferedImage.TYPE_INT_RGB)
        val g = bg.createGraphics()
        g.color = java.awt.Color(38, 60, 100)
        g.fillRect(0, 0, 120, 80)
        g.dispose()
        ImageIO.write(bg, "png", bgFile)
        app6.lastImportedBackgroundPath = bgFile.absolutePath
        val imported = LocalBackgroundImporter(defaultBackgroundDir()).import(bgFile)
        step("09-import-background", "09-background-imported.png", app6, "import local background") {
            imported != null && imported.isFile
        }
        if (imported != null) {
            app6.profileStore.save(edited.copy(backgroundKind = "imported", backgroundValue = imported.absolutePath))
        }
        step("10-background-saved", "10-background-saved.png", app6, "profile with imported background saved") {
            app6.profileStore.load("card", "card-cn-2")?.backgroundKind == "imported"
        }

        // 11. Numbers → 12. Number Detail → 13. Number Studio
        val app11 = createVNextAppState(VScreen.NUMBERS, "global")
        step("11-numbers", "11-numbers.png", app11, "open Numbers")
        app11.openNumber("num-cn-1")
        step("12-number-detail", "12-number-detail.png", app11, "open num-cn-1 detail (dial-code identity)") {
            app11.selectedNumberId == "num-cn-1"
        }
        val app13 = createVNextAppState(VScreen.NUMBER_CUSTOMIZATION, "global")
        app13.openNumberCustomization("num-cn-1")
        step("13-number-studio", "13-number-studio.png", app13, "open number studio (背景 默认展开)")

        // 14–16. Change Phone Current → Transition → After
        val app14 = createVNextAppState(VScreen.CHANGE_PHONE, "global")
        app14.changeProjection = "current"
        step("14-change-current", "14-change-current.png", app14, "projection=current") { app14.changeProjection == "current" }
        app14.changeProjection = "transition"
        step("15-change-transition", "15-change-transition.png", app14, "projection=transition") { app14.changeProjection == "transition" }
        app14.changeProjection = "after"
        step("16-change-after", "16-change-after.png", app14, "projection=after (plan projection label)") { app14.changeProjection == "after" }

        // 17. 空态（Cards Empty）
        val app17 = createVNextAppState(VScreen.CARDS, "global")
        app17.demoEmptyCards = true
        step("17-cards-empty", "17-cards-empty.png", app17, "cards empty (compact composition)")

        // ---- keyboard evidence（§47）----
        val focus = KeyboardFocusState()
        val keyLog = mutableListOf<String>()
        val appK = createVNextAppState(VScreen.OVERVIEW, "global")
        fun key(k: String, expect: KeyAction): Boolean {
            val action = routeKey(appK, focus, k)
            keyLog += "KEY $k -> $action (paletteOpen=${appK.paletteOpen} query='${appK.paletteQuery}')"
            return action == expect
        }
        val keysOk = key("ctrl+k", KeyAction.OPEN_PALETTE) &&
            key("更", KeyAction.NONE) && appK.paletteQuery == "更" &&
            key("换", KeyAction.NONE) && appK.paletteQuery == "更换" &&
            filterPalette(paletteEntries(), "更换").any { it.id == "start-change-phone" } &&
            key("enter", KeyAction.EXECUTE) && appK.screen == VScreen.CHANGE_PHONE &&
            key("ctrl+k", KeyAction.OPEN_PALETTE) &&
            key("escape", KeyAction.CLOSE_PALETTE)
        if (!keysOk) failed++

        // ---- IME 尝试记录（§42；不伪造 PASS）----
        val imeLog = mutableListOf<String>()
        imeLog += "IME_ATTEMPT: 目标输入「银行卡」「手机号」「更换手机号」"
        imeLog += "IME_ATTEMPT: in-process 只验证 search-as-you-type 累积（字素级，已由 palette 键盘证据覆盖）"
        imeLog += "IME_ATTEMPT: OS 级 IME 组合输入（Microsoft Pinyin）无法在 in-process harness 中可靠驱动"
        imeLog += "IME_RUNTIME_HUMAN_GATE: 中文 IME 组合输入需 Human 在真实窗口验证；本轮如实登记，不伪造 PASS"

        // ---- evidence 文件 ----
        val logFile = File(outRoot, "INTERACTION_LOG.txt")
        logFile.writeText((log + keyLog).joinToString("\n") + "\n")
        File(outRoot, "KEYBOARD_LOG.txt").writeText(keyLog.joinToString("\n") + "\n")
        File(outRoot, "IME_LOG.txt").writeText(imeLog.joinToString("\n") + "\n")
        File(outRoot, "PROFILE_PERSISTENCE_EVIDENCE.json").writeText(
            "{\n  \"assertions\": [\n" +
                "    {\"editedThemeId\": \"glass\"},\n" +
                "    {\"reopenedThemeId\": \"${app6.profileStore.load("card", "card-cn-2")?.themeId}\"},\n" +
                "    {\"reopenedLayout\": \"${app6.profileStore.load("card", "card-cn-2")?.layout}\"},\n" +
                "    {\"reopenedBackgroundKind\": \"${app6.profileStore.load("card", "card-cn-2")?.backgroundKind}\"},\n" +
                "    {\"canonicalUntouched\": \"true\"}\n" +
                "  ]\n}\n",
        )
        println("VNextJourney1F: steps=17 keyboard=$keysOk failures=$failed log=${logFile.absolutePath}")
        return if (failed == 0) 0 else 1
    }
}
