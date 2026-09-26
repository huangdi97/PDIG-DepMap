package com.pdig.desktop

import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.pdig.app.data.ScenarioPlanRequest
import com.pdig.core.domain.ActionVerificationStatus as VerificationStatus
import com.pdig.core.generated.ChangePlanWorkflowState
import com.pdig.core.sources.WechatParser
import com.pdig.desktop.data.DesktopSession
import com.pdig.desktop.io.AwtDesktopFileOps
import com.pdig.desktop.persist.DepmapFileStore
import com.pdig.desktop.security.DeviceUnlockStore
import com.pdig.desktop.security.WindowsDpapiSecurityPort
import com.pdig.desktop.ui.PDIGAppShell
import com.pdig.desktop.ui.Screen
import com.pdig.desktop.ui.UiState
import java.awt.Rectangle
import java.awt.Robot
import java.awt.image.BufferedImage
import java.io.File
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import javax.imageio.ImageIO
import kotlin.math.roundToInt

/**
 * `--shots <outRoot>` runtime visual harness (2026-09-26 multiclient sweep).
 *
 * Opens a REAL Compose Desktop window on this Windows machine (the same PDIGAppShell
 * the product ships), prepares a synthetic graph via the same repos the --smoke
 * harness drives (import wechat fixture -> proposal -> required dep -> scenario plan
 * -> candidate/drift rows), then walks every Screen at multiple window profiles,
 * capturing the on-screen frame with java.awt.Robot into PNG files following the
 * sweep naming convention:
 *
 *   desktop__win11__light__<page-id>__<state>__<seq>.png
 *
 * The window + composition stay untouched on the UI thread; a dedicated capture
 * thread only writes snapshot state (screen/size) and records pixels, so the UI
 * renders freely. Software rendering is forced (skiko.renderApi=SOFTWARE) so
 * captures are the actual composed pixels rather than GPU private buffers.
 * All files written -> exit 0.
 */
object ShotDriver {

    private const val PASSWORD = "shot-password-2026"
    private val SIZES = listOf(1280 to 720, 1920 to 1080, 2048 to 1152)

    data class Target(val screen: Screen, val state: String, val size: Pair<Int, Int>)

    private class Prepared(val cardId: String, val planId: String)

    fun run(repoRoot: File, outRoot: File): Int {
        val workDir = File(System.getProperty("java.io.tmpdir"), "pdig-shot-work").apply { mkdirs() }
        val pngDir = File(outRoot, "desktop").apply { mkdirs() }
        val dataFile = File(workDir, "shot.depmap")
        val store = DepmapFileStore()

        val prepared = prepareSession(repoRoot, dataFile, store)
        val session = DesktopSession.restore(store.open(dataFile, PASSWORD))
        val unlock = DeviceUnlockStore(
            WindowsDpapiSecurityPort(),
            File(System.getenv("APPDATA") ?: System.getProperty("user.home"), "PDIG"),
        )
        System.setProperty("skiko.renderApi", "SOFTWARE")

        val uiRef = AtomicReference<UiState>()
        val winRef = AtomicReference<java.awt.Window>()
        val stateRef = AtomicReference<WindowState>()
        val failures = AtomicInteger(0)

        val captureThread = Thread {
            try {
                while (uiRef.get() == null || winRef.get() == null || stateRef.get() == null) {
                    Thread.sleep(60)
                }
                val ui = uiRef.get()!!
                val win = winRef.get()!!
                val windowState = stateRef.get()!!
                val robot = Robot()
                val shots = planShots(prepared, ui)
                for ((i, target) in shots.withIndex()) {
                    try {
                        ui.screen = target.screen
                        ui.dataFile = if (target.state == "gate") null else dataFile
                        val want = target.size
                        val have = windowState.size
                        if (have.width.value.roundToInt() != want.first ||
                            have.height.value.roundToInt() != want.second
                        ) {
                            windowState.size = DpSize(want.first.dp, want.second.dp)
                        }
                        Thread.sleep(650)
                        val loc = win.locationOnScreen
                        val img: BufferedImage = robot.createScreenCapture(
                            Rectangle(loc.x, loc.y, win.width, win.height),
                        )
                        val pageId = if (target.state == "gate") "gate" else target.screen.name.lowercase().replace("_", "-")
                        val name = "desktop__win11__light__${pageId}__${target.state}__%02d.png".format(i + 1)
                        ImageIO.write(img, "png", File(pngDir, name))
                        println("[shots] OK  ${target.screen.name} [${target.state}] ${want.first}x${want.second} -> $name")
                    } catch (t: Throwable) {
                        failures.incrementAndGet()
                        println("[shots] FAIL ${target.screen.name} [${target.state}] :: ${t.message}")
                    }
                }
                println("[shots] VERDICT: ${if (failures.get() == 0) "PASS" else "FAIL (${failures.get()})"}")
            } catch (t: Throwable) {
                println("[shots] FATAL :: ${t.stackTraceToString()}")
            } finally {
                kotlin.system.exitProcess(if (failures.get() == 0) 0 else 1)
            }
        }
        captureThread.isDaemon = true
        captureThread.start()

        application {
            val windowState = rememberWindowState(
                size = DpSize(1280.dp, 720.dp),
                position = WindowPosition(0.dp, 0.dp),
            )
            val sessionFile = dataFile
            Window(
                onCloseRequest = { exitApplication() },
                title = "PDIG ShotDriver (evidence)",
                state = windowState,
            ) {
                val ui = remember {
                    UiState(session, unlock, AwtDesktopFileOps(null)).apply {
                        this.dataFile = sessionFile
                        this.screen = Screen.HOME
                    }
                }
                SideEffect {
                    uiRef.set(ui)
                    winRef.set(window)
                    stateRef.set(windowState)
                }
                PDIGAppShell(ui)
            }
        }
        return if (failures.get() == 0) 0 else 1
    }

    /** All 24 screens + gate at 1280x720, then the complex/top-level set at the two larger profiles. */
    private fun planShots(prepared: Prepared, ui: UiState): List<Target> {
        ui.selectedNodeId = prepared.cardId
        ui.selectedPlanId = prepared.planId
        ui.selectedScenarioId = "replace_payment_card"

        val primary: List<Pair<Screen, String>> = listOf(
            Screen.HOME to "populated",
            Screen.ATTENTION to "populated",
            Screen.SOURCES to "populated",
            Screen.IMPORT to "idle",
            Screen.MAPPING to "empty",
            Screen.REVIEW to "populated",
            Screen.PROPOSALS to "populated",
            Screen.CANDIDATES to "populated",
            Screen.DRIFTS to "populated",
            Screen.INFRA to "populated",
            Screen.NODE to "populated",
            Screen.FINDINGS to "populated",
            Screen.SCENARIOS to "populated",
            Screen.SCENARIO_SETUP to "populated",
            Screen.IMPACT to "populated",
            Screen.PLAN to "populated",
            Screen.ACTIONS to "populated",
            Screen.VERIFICATION to "populated",
            Screen.TIMELINE to "populated",
            Screen.BACKUP to "populated",
            Screen.RESTORE to "empty",
            Screen.SETTINGS to "populated",
            Screen.SECURITY to "populated",
            Screen.ABOUT to "populated",
        )

        val targets = mutableListOf<Target>()
        for ((screen, state) in primary) {
            targets.add(Target(screen, state, SIZES[0]))
        }
        targets.add(0, Target(Screen.HOME, "gate", SIZES[0]))
        val bigSet = listOf(
            Screen.HOME, Screen.ATTENTION, Screen.SOURCES, Screen.INFRA, Screen.FINDINGS, Screen.SCENARIOS,
            Screen.IMPACT, Screen.PLAN, Screen.VERIFICATION, Screen.TIMELINE, Screen.BACKUP,
            Screen.RESTORE, Screen.SETTINGS, Screen.IMPORT,
        )
        for (size in SIZES.drop(1)) {
            for (s in bigSet) targets.add(Target(s, "populated", size))
        }
        return targets
    }

    private fun prepareSession(repoRoot: File, dataFile: File, store: DepmapFileStore): Prepared {
        val fixture = File(repoRoot, "fixtures/import/normal-wechat.csv")
        check(fixture.isFile) { "fixture missing: $fixture" }
        val session = DesktopSession.open()
        try {
            val parsed = WechatParser.parse(fixture.readBytes())
            check(parsed.observations.isNotEmpty()) { "wechat parse produced 0 observations" }
            val preview = session.sources.previewImport(
                parsed.observations,
                parsed.errors.map { "${it.line}:${it.reason}" },
                "wechat_statement",
                "shot wechat",
            )
            check(preview.instruments.isNotEmpty()) { "no instrument detected" }
            val cardId = preview.instruments.first().nodeId
            val result = session.sources.commitImport(preview)
            check(result.nodeCount >= 1) { "commit import created no nodes" }

            val pending = session.proposals.pendingProposals()
            if (pending.isNotEmpty()) session.proposals.acceptProposal(pending.first().id)

            val dep = session.graph.dependencies().firstOrNull()
            if (dep != null && dep.criticality != "required") {
                session.graph.setDependencyCriticality(dep.id, required = true)
            }

            val planId = session.plans.createPlanForScenario(
                ScenarioPlanRequest(
                    scenarioId = "replace_payment_card",
                    targetNodeId = cardId,
                    effectiveDate = null,
                ),
            )
            val detail = checkNotNull(session.plans.planDetail(planId))
            for (action in detail.actions) {
                if (action.resolvesImpactKeys.isNotEmpty()) {
                    session.plans.completeAction(planId, action.id)
                }
            }
            val d2 = checkNotNull(session.plans.planDetail(planId))
            for (action in d2.actions) {
                val v = action.verification
                if (action.done && v != null && v.status != VerificationStatus.NOT_REQUIRED) {
                    session.plans.verifyAction(planId, action.id)
                }
            }
            check(session.plans.planDetail(planId)?.workflowState != ChangePlanWorkflowState.DRAFT) { "plan stuck in draft" }

            val now = Instant.now().toString()
            session.driver.exec(
                "INSERT INTO discovery_candidates (id, candidate_kind, display_label, normalized_key, " +
                    "source_instance_id, evidence_refs_json, observation_count, first_seen_at, last_seen_at, " +
                    "status, created_at, updated_at) VALUES (" +
                    "'shot-cand-1','service','示例服务','example-service','legacy-wechat-statement','[]',3,'$now','$now','pending','$now','$now')",
            )
            val anyDepId = session.graph.dependencies().firstOrNull()?.id ?: ""
            session.driver.exec(
                "INSERT INTO reality_drifts (id, kind, target_node_id, capability, candidate_from, candidate_relation, " +
                    "related_dependency_ids_json, evidence_refs_json, proposal_keys_json, observation_count, detected_at, updated_at, status) " +
                    "VALUES ('shot-drift-1','possible_replacement','$cardId','payment','nd-card-y','funding_source'," +
                    "'[\"$anyDepId\"]','[]','[]',4,'$now','$now','open')",
            )

            // v0.3.0 画面数据：身份/恢复边（旧手机号 + 备用设备 + 账户），
            // 让「基础设施薄弱点」展示 SPOF / 共享故障点 / 恢复循环三类示例。
            val now2 = Instant.now().toString()
            val phone1 = "shot-phone-1"
            val phone2 = "shot-phone-2"
            val dev = "shot-dev-1"
            val acct = "shot-acct-1"
            session.driver.exec("INSERT INTO nodes (id, kind, name, archived, fields_json, owner, created_at, updated_at) " +
                "VALUES ('$phone1','identity_anchor','旧手机号',0,'{}','self','$now2','$now2')")
            session.driver.exec("INSERT INTO nodes (id, kind, name, archived, fields_json, owner, created_at, updated_at) " +
                "VALUES ('$phone2','identity_anchor','新手机号',0,'{}','self','$now2','$now2')")
            session.driver.exec("INSERT INTO nodes (id, kind, name, archived, fields_json, owner, created_at, updated_at) " +
                "VALUES ('$dev','device','备用设备',0,'{}','self','$now2','$now2')")
            session.driver.exec("INSERT INTO nodes (id, kind, name, archived, fields_json, owner, created_at, updated_at) " +
                "VALUES ('$acct','account','主账户',0,'{}','self','$now2','$now2')")
            fun depEdge(id: String, from: String, to: String) {
                session.driver.exec(
                    "INSERT INTO dependencies (id, from_node, relation, to_node, capability, criticality, state, origin, confirmed_at, last_verified_at, created_at, updated_at) " +
                        "VALUES ('$id','$from','recovers','$to','recovery','unknown','active','manual','$now2','$now2','$now2','$now2')",
                )
            }
            depEdge("shot-dep-1", dev, phone1)   // phone1 只有唯一恢复来源 → SPOF
            depEdge("shot-dep-2", dev, acct)     // dev 服务两条恢复路径 → 共享故障点
            depEdge("shot-dep-3", phone2, acct)  // 与新手机号成环（下一条回边）
            depEdge("shot-dep-4", acct, phone2)  // 恢复循环：acct ⇄ phone2

            store.save(dataFile, session.exportPayload(), PASSWORD)
            return Prepared(cardId, planId)
        } finally {
            session.close()
        }
    }
}