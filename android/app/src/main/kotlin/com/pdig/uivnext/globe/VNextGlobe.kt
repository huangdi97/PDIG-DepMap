package com.pdig.uivnext.globe

import android.graphics.Bitmap
import com.pdig.app.BuildConfig
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import com.pdig.uivnext.model.RegionPresentation
import com.pdig.uivnext.model.VGlobeState
import com.pdig.uivnext.model.VTestIds
import com.pdig.uivnext.theme.PdigV2Colors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.sqrt
/** Globe 渲染就绪状态（brief §10：证据截图前置条件 = TEXTURE_READY；禁止截黑球冒充 PASS）。 */
enum class GlobeRenderState { LOADING, TEXTURE_READY, FALLBACK, ERROR }

private const val YAW_PER_SEC = 0.8f // idle rotation deg/sec（极慢；交互后暂停）
private const val IDLE_TEXTURE_REFRESH_MS = 15_000L
private const val MAX_ZOOM = 1.9f
private const val MIN_ZOOM = 0.7f

/** Globe 交互状态（由父级持有，供状态机与截图参数使用）。 */
class GlobeController(
    initialCamera: GlobeCamera = focusCamera(16f, 107f),
) {
    var camera by mutableStateOf(initialCamera)
    var hoveredRegion: RegionPresentation? by mutableStateOf(null)
    var selectedRegion: String? by mutableStateOf(null)
    var interactive by mutableStateOf(true)
    var state by mutableStateOf(VGlobeState.GLOBAL)

    /** 纹理地球渲染就绪状态；screenshot 测试必须等到 TEXTURE_READY（合理 timeout）。 */
    var renderState by mutableStateOf(GlobeRenderState.LOADING)

    /** 渲染几何（画布中心/半径 px；供 GlobeEvidenceContract 计算 mean luminance / non-black ratio）。 */
    var renderCenterPx by mutableStateOf(Offset.Zero)
    var renderRadiusPx by mutableStateOf(0f)

    /** 渲染失败原因（GlobeRenderState.ERROR 时供诊断；证据截图只认 TEXTURE_READY）。 */
    var renderError: String? by mutableStateOf(null)

    /**
     * Retain the last *real textured frame* across Now → Infrastructure recomposition.
     * These fields are presentation cache only, bounded to one bitmap and one camera key;
     * they are not persisted into Canonical or PersonalReality.
     */
    internal var lastTexturedEarth: Bitmap? = null
    internal var lastTexturedKey: String? = null

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

/**
 * Globe：Global Infrastructure Navigator（Android 原生 Compose 渲染）。
 * 默认纹理地球（bundled NASA 资产，逐像素投影 + day/night/cloud），资产缺失/低功耗 → Vector 回退。
 * [quality] 控制纹理质量档（HIGH/BALANCED/LOW）；[cameraOverride] 供证据测试冻结相机。
 */
@Composable
fun VNextGlobe(
    controller: GlobeController,
    regions: List<RegionPresentation>,
    arcingPairs: List<Pair<String, String>>,
    reduceMotion: Boolean = false,
    cameraOverride: GlobeCamera? = null,
    quality: EarthQuality = EarthQuality.HIGH,
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var yawBase by remember { mutableStateOf(0f) }
    val context = LocalContext.current
    val assets = remember(quality) { EarthMaterialAssets.load(context, quality) }
    // A newly composed Overview must not flash a flat vector sphere after Now.
    // Reuse the last truthful texture while its own size-specific frame is rebuilt.
    var earthBitmap by remember(controller) { mutableStateOf(controller.lastTexturedEarth) }
    var earthKey by remember(controller) { mutableStateOf(controller.lastTexturedKey) }
    // This is the state of THIS canvas, never the previous screen's shared controller.
    var localRenderState by remember { mutableStateOf(GlobeRenderState.LOADING) }

    // Idle rotation：先让首张真实纹理稳定进入 TEXTURE_READY，再低频刷新相机。
    // 逐像素球面投影是重任务；如果每 50ms 改 yaw，会持续取消后台渲染，最终只剩 fallback 深色球。
    // 拖拽仍然实时更新 controller.camera；idle 只负责很慢的环境动效，不得牺牲首帧真实性。
    LaunchedEffect(controller.interactive, controller.selectedRegion, reduceMotion) {
        if (!controller.interactive || controller.selectedRegion != null || reduceMotion) return@LaunchedEffect
        while (true) {
            while (controller.renderState != GlobeRenderState.TEXTURE_READY) {
                delay(250)
            }
            delay(IDLE_TEXTURE_REFRESH_MS)
            if (!controller.interactive || controller.selectedRegion != null || reduceMotion) continue
            yawBase = (yawBase + YAW_PER_SEC * (IDLE_TEXTURE_REFRESH_MS / 1000f)) % 360f
        }
    }

    // 纹理地球后台渲染：相机/画布/idle 旋转变化时重建；量化缓存避免每帧重算（主线程只 drawBitmap）。
    // GlobeRenderState 由本 effect 驱动（LOADING → TEXTURE_READY / FALLBACK / ERROR），
    // 证据截图必须等到 TEXTURE_READY —— 禁止截 near-black empty sphere 冒充 PASS。
    LaunchedEffect(
        canvasSize,
        controller.camera.yawDeg,
        controller.camera.pitchDeg,
        controller.camera.zoom,
        yawBase,
        assets,
        quality,
    ) {
        if (canvasSize == IntSize.Zero) {
            localRenderState = GlobeRenderState.LOADING
            controller.renderState = GlobeRenderState.LOADING
            return@LaunchedEffect
        }
        val (center, radius) = globeMetrics(canvasSize, controller.camera.zoom)
        controller.renderCenterPx = center
        controller.renderRadiusPx = radius
        if (assets == null || quality == EarthQuality.LOW) {
            // 资产缺失 / 低功耗档：有效 fallback 球体（非黑球），并明确标记 FALLBACK。
            localRenderState = GlobeRenderState.FALLBACK
            controller.renderState = GlobeRenderState.FALLBACK
            earthBitmap = null
            earthKey = null
            return@LaunchedEffect
        }
        val rect = earthRenderRect(radius, quality)
        val displayCam = controller.camera.copy(yawDeg = controller.camera.yawDeg + yawBase)
        val key = cameraCacheKey(displayCam, rect)
        if (key == earthKey && earthBitmap != null) {
            localRenderState = GlobeRenderState.TEXTURE_READY
            controller.renderState = GlobeRenderState.TEXTURE_READY
            return@LaunchedEffect
        }
        localRenderState = GlobeRenderState.LOADING
        controller.renderState = GlobeRenderState.LOADING
        val bmp = try {
            withContext(Dispatchers.Default) {
                renderEarthBody(
                    assets, rect, center.x.toInt(), center.y.toInt(), radius, displayCam,
                    sunDir = if (BuildConfig.FLAVOR == "preview") R9_REFERENCE_SUN_DIR else SUN_DIR,
                    previewReferenceLift = BuildConfig.FLAVOR == "preview",
                )
            }
        } catch (t: kotlinx.coroutines.CancellationException) {
            // 组合作用域离开（截图切换 app 时旧组合被取消）：这是干净取消，不是渲染失败。
            throw t
        } catch (t: Throwable) {
            controller.renderError = "${t::class.simpleName}: ${t.message}"
            android.util.Log.e("GlobeRender", "texture earth render failed", t)
            localRenderState = GlobeRenderState.ERROR
            controller.renderState = GlobeRenderState.ERROR
            null
        }
        if (bmp != null) {
            earthBitmap = bmp
            earthKey = key
            controller.lastTexturedEarth = bmp
            controller.lastTexturedKey = key
            localRenderState = GlobeRenderState.TEXTURE_READY
            controller.renderState = GlobeRenderState.TEXTURE_READY
        } else {
            localRenderState = GlobeRenderState.ERROR
            controller.renderState = GlobeRenderState.ERROR
        }
    }

    // 无障碍：Globe 画布声明语义描述；精确探索用 Overview 的 Region List（非视觉替代，任务书 §30）。
    Box(
        Modifier.semantics {
            contentDescription = "全球基础设施导航器（${regions.size} 个地区）；纹理状态=${localRenderState.name}；可拖动旋转、点击聚焦地区"
        },
    ) {
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

            // L1 Atmosphere：外层光晕（不覆盖地球主体）
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(PdigV2Colors.AtmosphereInner.copy(alpha = 0.55f), PdigV2Colors.AtmosphereOuter),
                    center = center,
                    radius = radius * 1.7f,
                ),
                radius = radius * 1.7f,
                center = center,
            )

            // 地球主体：纹理地球（缓存 Bitmap）；LOADING / FALLBACK / ERROR 时绘制
            // 「地球加载材质」（有效 fallback，禁止 near-black empty sphere，brief §10）。
            val bmp = earthBitmap
            if (bmp != null) {
                drawEarthBitmap(bmp, center.x.toInt(), center.y.toInt(), (radius * 2f).toInt())
                // Atmosphere, not an invented dependency edge. Production stays subtle.
                if (BuildConfig.FLAVOR == "preview") {
                    drawCircle(color = Color(0xFF62BAFF).copy(alpha = .17f),
                        radius = radius * 1.050f, center = center,
                        style = Stroke(width = radius * .093f))
                    drawCircle(color = Color(0xFF90D9FF).copy(alpha = .34f),
                        radius = radius * 1.019f, center = center,
                        style = Stroke(width = radius * .038f))
                    drawCircle(color = Color.White.copy(alpha = .49f),
                        radius = radius * 1.002f, center = center,
                        style = Stroke(width = radius * .009f))
                } else {
                    drawCircle(color = Color(0xFF5EB7FF).copy(alpha = .33f),
                        radius = radius * 1.003f, center = center,
                        style = Stroke(width = radius * .018f))
                    drawCircle(color = Color(0xFF8BCCFF).copy(alpha = .15f),
                        radius = radius * 1.033f, center = center,
                        style = Stroke(width = radius * .032f))
                }
            } else {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFF83C9FF), Color(0xFF328BDE), Color(0xFF124C9A)),
                        center = Offset(center.x - radius * 0.35f, center.y - radius * 0.35f),
                        radius = radius * 1.4f,
                    ),
                    radius = radius,
                    center = center,
                )
            }

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
                // Light reference: two-pass glow for genuinely recorded cross-region edges.
                // The edge list is unchanged; ornamental paths are never misrepresented as facts.
                drawArcPath(
                    center, radius, cam, path,
                    Color(0xFF329BEE).copy(alpha = if (selectedLink) 0.32f else 0.19f),
                    widthPx = if (selectedLink) 7f else 5f,
                )
                drawArcPath(
                    center, radius, cam, path,
                    if (selectedLink) Color(0xFFFFC65B) else Color(0xFFBDEBFF),
                    widthPx = if (selectedLink) 2.6f else 2.0f,
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
                    color = Color(0xFF5AC0FF).copy(alpha = 0.31f),
                    radius = anchorR * 2.3f,
                    center = projected.first,
                )
                drawCircle(
                    color = if (isSelected || isHovered) PdigV2Colors.RegionNodeHi else Color(0xFF288AF4),
                    radius = anchorR,
                    center = projected.first,
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.93f),
                    radius = anchorR * 0.34f,
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
