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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import com.pdig.uivnext.model.RegionPresentation
import com.pdig.uivnext.model.VGlobeState
import com.pdig.uivnext.model.VTestIds
import com.pdig.uivnext.theme.PdigV2Colors
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

private fun globeMetrics(size: IntSize, zoom: Float): Pair<Offset, Float> {
    val d = minOf(size.width, size.height).toFloat()
    val radius = d * 0.36f * zoom
    return Offset(size.width / 2f, size.height / 2f) to radius
}

private fun globeMetrics(size: Size, zoom: Float): Pair<Offset, Float> {
    val d = minOf(size.width, size.height)
    val radius = d * 0.36f * zoom
    return Offset(size.width / 2f, size.height / 2f) to radius
}

private fun anchorScreen(
    region: RegionPresentation,
    cam: GlobeCamera,
    center: Offset,
    radius: Float,
): Pair<Offset, Float>? {
    val p = project(latLonToVec(region.latitude.toFloat(), region.longitude.toFloat()), cam, radius, center.x, center.y)
    if (p.zDepth <= 0f) return null
    return Offset(p.x, p.y) to p.zDepth
}

private fun hitTolerance(radius: Float): Float = (radius * 0.05f).coerceIn(10f, 18f)

@Composable
fun VNextGlobe(
    controller: GlobeController,
    regions: List<RegionPresentation>,
    arcingPairs: List<Pair<String, String>>,
    reduceMotion: Boolean = false,
    cameraOverride: GlobeCamera? = null,
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
            val (center, radius) = globeMetrics(size, controller.camera.zoom)
            val cam = cameraOverride ?: controller.camera.copy(yawDeg = controller.camera.yawDeg + yawBase)

            // L1 Atmosphere
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(PdigV2Colors.AtmosphereInner.copy(alpha = 0.55f), PdigV2Colors.AtmosphereOuter),
                    center = center,
                    radius = radius * 1.7f,
                ),
                radius = radius * 1.7f,
                center = center,
            )
            // Sphere（程序化深度着色，无纹理：RENDERER_LIMITATION）
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0xFF1B3A6B), PdigV2Colors.SurfaceRaised, PdigV2Colors.CanvasDeep),
                    center = Offset(center.x - radius * 0.35f, center.y - radius * 0.35f),
                    radius = radius * 1.4f,
                ),
                radius = radius,
                center = center,
            )
            // 网格（前半球，按深度淡出）
            val gridColor = PdigV2Colors.TextMuted.copy(alpha = 0.16f)
            val (parallels, meridians) = graticuleLines(30)
            for (p in parallels) {
                drawArcPath(center, radius, cam, greatCircleSamples(p.first, p.second, p.first + 20f, p.second + 20f, 24), gridColor)
            }
            for (m in meridians) {
                drawArcPath(center, radius, cam, greatCircleSamples(m.first, m.second, m.first + 10f, m.second, 24), gridColor)
                drawArcPath(center, radius, cam, greatCircleSamples(m.first, m.second, m.first + 10f, m.second + 10f, 24), gridColor)
            }

            // 跨区弧线（只有真实跨区关系；绝不装饰性连线）
            for ((aCode, bCode) in arcingPairs) {
                val a = regions.firstOrNull { it.regionCode == aCode } ?: continue
                val b = regions.firstOrNull { it.regionCode == bCode } ?: continue
                val selectedLink = controller.selectedRegion == aCode || controller.selectedRegion == bCode
                val path = greatCircleSamples(
                    a.latitude.toFloat(), a.longitude.toFloat(),
                    b.latitude.toFloat(), b.longitude.toFloat(), 48,
                )
                drawArcPath(
                    center, radius, cam, path,
                    if (selectedLink) PdigV2Colors.ArcActive else PdigV2Colors.ArcQuiet,
                    widthPx = if (selectedLink) 1.8f else 1.2f,
                )
            }

            // 地区锚点（悬浮/选中时高亮 + 光环）
            for (r in regions) {
                val projected = anchorScreen(r, cam, center, radius) ?: continue
                val depth = projected.second
                val isSelected = controller.selectedRegion == r.regionCode
                val isHovered = controller.hoveredRegion?.regionCode == r.regionCode
                val anchorR = radius * (0.02f + 0.012f * depth) * (if (isSelected) 1.6f else 1f)
                drawCircle(
                    color = if (isSelected || isHovered) PdigV2Colors.RegionNodeHi else PdigV2Colors.RegionNodeLo,
                    radius = anchorR,
                    center = projected.first,
                )
                if (isSelected || isHovered) {
                    drawCircle(
                        color = PdigV2Colors.PrimaryBright.copy(alpha = 0.45f),
                        radius = anchorR * 2.1f,
                        center = projected.first,
                    )
                }
            }
        }
    }
}

/** 弧线段：仅绘制前后半球交界、按深度淡出（2.5D 加分项）。 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawArcPath(
    center: Offset,
    radius: Float,
    cam: GlobeCamera,
    path: List<Vec3>,
    color: Color,
    widthPx: Float = 1.2f,
) {
    var prev = project(path.first(), cam, radius, center.x, center.y)
    var drawing = prev.zDepth > -0.05f
    for (v in path.drop(1)) {
        val cur = project(v, cam, radius, center.x, center.y)
        val visible = cur.zDepth > -0.05f
        if (visible || drawing) {
            val alpha = (cur.zDepth.coerceIn(0f, 1f) * 0.9f + 0.1f).coerceIn(0.08f, 1f)
            drawLine(
                color = color.copy(alpha = color.alpha * alpha),
                start = Offset(prev.x, prev.y),
                end = Offset(cur.x, cur.y),
                strokeWidth = widthPx,
            )
        }
        drawing = visible
        prev = cur
    }
}