package com.pdig.uivnext.globe

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import com.pdig.uivnext.model.RegionPresentation
import com.pdig.uivnext.model.VGlobeState
import com.pdig.uivnext.model.VTestIds
import kotlinx.coroutines.delay
import kotlin.math.sqrt

private const val YAW_PER_SEC = 0.8f // idle rotation deg/sec（极慢；交互后暂停）
private const val MAX_ZOOM = 1.9f
private const val MIN_ZOOM = 0.7f

/** Globe 交互状态（由父级持有，供状态机与截图参数使用）。 */
class GlobeController(
    initialCamera: GlobeCamera = GlobeCamera(0f, 30f, 1f),
) {
    var camera by mutableStateOf(initialCamera)
    var hoveredRegion: RegionPresentation? by mutableStateOf(null)
    var selectedRegion: String? by mutableStateOf(null)
    var interactive by mutableStateOf(true)
    var state by mutableStateOf(VGlobeState.GLOBAL)

    fun focusRegion(region: RegionPresentation) {
        val target = focusCamera(region.latitude.toFloat(), region.longitude.toFloat())
        camera = camera.copy(yawDeg = target.yawDeg, pitchDeg = target.pitchDeg, zoom = camera.zoom.coerceAtLeast(1.3f))
        selectedRegion = region.regionCode
        hoveredRegion = region
        state = VGlobeState.REGION_SELECTED
    }

    fun backToGlobal() {
        selectedRegion = null
        hoveredRegion = null
        camera = camera.copy(zoom = 1f)
        state = VGlobeState.GLOBAL
    }
}

/**
 * Globe 舞台（L1 Spatial）：真实地球感 vector Earth（G1 SIGNATURE）。
 *
 * 默认渲染器 = bundled 简化海岸线 + 海洋/陆地层次 + 夜间城市灯光 + 大气 rim
 * + sunlight/terminator 暗面 + 地区锚点 + 连接弧（EarthScene.kt / EarthOverlay.kt）。
 * plain sphere 仅保留为 LOW_POWER_FALLBACK（lowPower=true）。
 */
@Composable
fun VNextGlobe(
    controller: GlobeController,
    regions: List<RegionPresentation>,
    arcingPairs: List<Pair<String, String>>,
    reduceMotion: Boolean = false,
    cameraOverride: GlobeCamera? = null,
    lowPower: Boolean = false,
    showRegionLabels: Boolean = false,
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var yawBase by remember { mutableStateOf(0f) }

    // Idle rotation：极慢、可被交互暂停、reduce motion 或选中地区时关闭。
    LaunchedEffect(controller.interactive, controller.selectedRegion, reduceMotion) {
        if (!controller.interactive || controller.selectedRegion != null || reduceMotion) return@LaunchedEffect
        while (true) {
            delay(50)
            yawBase = (yawBase + YAW_PER_SEC * 0.05f) % 360f
        }
    }

    val textMeasurer = rememberTextMeasurer()

    Box(Modifier.testTag(VTestIds.GLOBE_STAGE)) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { canvasSize = it }
                .testTag(VTestIds.GLOBE_CANVAS)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { controller.interactive = false },
                        onDragEnd = { controller.interactive = true },
                        onDragCancel = { controller.interactive = true },
                    ) { change, dragAmount ->
                        change.consume()
                        controller.camera = controller.camera.copy(
                            yawDeg = controller.camera.yawDeg - dragAmount.x * 0.35f,
                            pitchDeg = (controller.camera.pitchDeg - dragAmount.y * 0.35f).coerceIn(-60f, 60f),
                        )
                    }
                }
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type == PointerEventType.Scroll) {
                                val delta = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                                controller.interactive = false
                                val factor = (1f - delta * 0.002f).coerceIn(0.8f, 1.25f)
                                controller.camera = controller.camera.copy(
                                    zoom = (controller.camera.zoom * factor).coerceIn(MIN_ZOOM, MAX_ZOOM),
                                )
                            }
                            if (event.type == PointerEventType.Move && canvasSize != IntSize.Zero) {
                                val (center, radius) = globeMetrics(canvasSize, controller.camera.zoom)
                                val cam = cameraOverride ?: controller.camera
                                val pos = event.changes.firstOrNull()?.position ?: continue
                                val hovered = regions.firstNotNullOfOrNull { r ->
                                    anchorScreen(r, cam, center, radius)?.let {
                                        val (anchor, _) = it
                                        val dx = anchor.x - pos.x
                                        val dy = anchor.y - pos.y
                                        if (sqrt(dx * dx + dy * dy) <= hitTolerance(radius)) r to (dx * dx + dy * dy) else null
                                    }
                                }?.first
                                controller.hoveredRegion = hovered
                                if (hovered != null) controller.state = VGlobeState.REGION_HOVER
                            }
                            if (event.type == PointerEventType.Exit) {
                                controller.hoveredRegion = null
                                if (controller.selectedRegion == null) controller.state = VGlobeState.GLOBAL
                            }
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { position ->
                        if (canvasSize == IntSize.Zero) return@detectTapGestures
                        val (center, radius) = globeMetrics(canvasSize, controller.camera.zoom)
                        val cam = cameraOverride ?: controller.camera
                        val hit = regions
                            .mapNotNull { r ->
                                anchorScreen(r, cam, center, radius)?.let { (pos, _) ->
                                    val d = (pos.x - position.x) * (pos.x - position.x) + (pos.y - position.y) * (pos.y - position.y)
                                    if (d <= hitTolerance(radius) * hitTolerance(radius)) r to d else null
                                }
                            }
                            .minByOrNull { it.second }
                            ?.first
                        if (hit != null) {
                            if (controller.selectedRegion == hit.regionCode) {
                                controller.state = VGlobeState.REGION_DETAIL
                            } else {
                                controller.focusRegion(hit)
                            }
                        } else if (controller.selectedRegion != null) {
                            controller.backToGlobal()
                        }
                    }
                },
        ) {
            val cam = cameraOverride ?: controller.camera.copy(yawDeg = controller.camera.yawDeg + yawBase)
            drawEarth(
                camera = cam,
                regions = regions,
                arcingPairs = arcingPairs,
                selectedRegion = controller.selectedRegion,
                hoveredRegion = controller.hoveredRegion,
                lowPower = lowPower,
                showRegionLabels = showRegionLabels,
                textMeasurer = textMeasurer,
            )
        }
    }
}

