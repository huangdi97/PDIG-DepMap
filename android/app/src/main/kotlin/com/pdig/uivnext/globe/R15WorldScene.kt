package com.pdig.uivnext.globe

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import com.pdig.uivnext.model.RegionPresentation
import kotlin.math.min

/**
 * R15 whole-scene renderer. GPU sphere and real-data foreground are isolated
 * from screen-specific cards, navigation and the legacy Bitmap pipeline.
 *
 * Only actual region pair edges are emitted. All UI points are projected from
 * the same camera / geometry as the GPU; no arbitrary SVG-like screen connectors.
 */
@Composable
internal fun R15WorldScene(
    controller: GlobeController,
    regions: List<RegionPresentation>,
    arcingPairs: List<Pair<String, String>>,
    reduceMotion: Boolean,
    onRegionChosen: (RegionPresentation) -> Unit,
) {
    val context = LocalContext.current
    var failed by remember { mutableStateOf(false) }
    var ready by remember { mutableStateOf(false) }
    val supports = remember(context) { supportsR15GpuEarth(context) }
    if (!supports || failed) {
        VNextGlobe(
            controller = controller,
            regions = regions,
            arcingPairs = arcingPairs,
            reduceMotion = reduceMotion,
        )
        return
    }

    // Distinct Preview GPU pipeline; any shader initialization failure restores
    // the existing known-good globe without changing any business state.
    val view = remember(context) {
        R15GpuEarthView(
            context,
            onFailure = {
                controller.renderState = GlobeRenderState.ERROR
                failed = true
            },
            onReady = {
                controller.renderState = GlobeRenderState.TEXTURE_READY
                ready = true
            },
        )
    }
    DisposableEffect(view) { onDispose { view.onPause() } }
    val camera = controller.camera
    val cameraDesc = "；视角=" + camera.yawDeg.toInt() + "度；缩放=" +
        (camera.zoom * 100f).toInt() + "%"

    Box(
        Modifier.fillMaxSize()
            // A GLSurfaceView is an Android child view; process motion on its
            // Compose parent at Initial pass, before AndroidView and foreground
            // tap layers can compete for Main-pass gesture consumption.
            // One recognizer handles both single-pointer orbit and pinch zoom.
            .pointerInput(controller) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val pan = event.calculatePan()
                        val zoom = if (event.changes.count { it.pressed } >= 2) {
                            event.calculateZoom()
                        } else 1f
                        if (pan.x != 0f || pan.y != 0f || kotlin.math.abs(zoom - 1f) > 0.001f) {
                            controller.interactive = false
                            controller.camera = applyGlobeTransform(controller.camera, pan, zoom)
                            event.changes.forEach { change ->
                                if (change.positionChanged()) change.consume()
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
            .testTag("pdig.r15.scene")
            .semantics {
                contentDescription = "全球基础设施导航器；纹理状态=" +
                    (if (ready) "TEXTURE_READY" else "LOADING") +
                    "；R15_GPU" + cameraDesc + "；拖动旋转、双指缩放、点击地区"
            },
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize().testTag("pdig.r15.gpu-earth"),
            factory = { view },
            update = { it.showCamera(camera) },
        )
        Canvas(
            Modifier.fillMaxSize()
                .testTag("pdig.r15.actual-links")
                .pointerInput(controller, regions) {
                    detectTapGestures { tap ->
                        val radius = min(size.width * .42f, size.height * .47f) * controller.camera.zoom
                        val center = Offset(size.width * .5f, size.height * .5f)
                        val hit = regions.firstOrNull { region ->
                            val position = project(
                                latLonToVec(region.latitude.toFloat(), region.longitude.toFloat()),
                                controller.camera, radius, center.x, center.y,
                            )
                            isAnchorHit(position, tap.x, tap.y, 28f)
                        }
                        if (hit != null) {
                            controller.focusRegion(hit)
                            onRegionChosen(hit)
                        }
                    }
                }
                ,
        ) {
            val radius = min(size.width * .42f, size.height * .47f) * camera.zoom
            val cx = size.width / 2f
            val cy = size.height / 2f
            val indexed = regions.associateBy { it.regionCode }
            for ((aId, bId) in arcingPairs.distinct()) {
                val from = indexed[aId] ?: continue
                val to = indexed[bId] ?: continue
                val samples = greatCircleSamples(
                    from.latitude.toFloat(), from.longitude.toFloat(),
                    to.latitude.toFloat(), to.longitude.toFloat(), 42,
                )
                for (index in 1 until samples.size) {
                    val a = project(samples[index - 1], camera, radius, cx, cy)
                    val b = project(samples[index], camera, radius, cx, cy)
                    val clipped = clipFrontHemisphereSegment(a, b) ?: continue
                    val start = Offset(clipped.first.x, clipped.first.y)
                    val end = Offset(clipped.second.x, clipped.second.y)
                    drawLine(Color(0xFF008DFF).copy(alpha = .19f), start, end,
                        strokeWidth = 10f, cap = StrokeCap.Round)
                    drawLine(Color(0xFF3FB9FF).copy(alpha = .68f), start, end,
                        strokeWidth = 3.2f, cap = StrokeCap.Round)
                    drawLine(Color.White.copy(alpha = .86f), start, end,
                        strokeWidth = 1.0f, cap = StrokeCap.Round)
                }
            }
            for (region in regions) {
                val position = project(
                    latLonToVec(region.latitude.toFloat(), region.longitude.toFloat()),
                    camera, radius, cx, cy,
                )
                if (position.zDepth <= 0f) continue
                val p = Offset(position.x, position.y)
                val important = controller.selectedRegion == region.regionCode
                drawCircle(Color(0xFF1DA6FF).copy(alpha = .18f), radius = if (important) 20f else 12f, center = p)
                drawCircle(Color(0xFF75D6FF).copy(alpha = .87f), radius = if (important) 6f else 4.5f, center = p)
                drawCircle(Color.White, radius = 2.1f, center = p)
            }
        }
    }
}
