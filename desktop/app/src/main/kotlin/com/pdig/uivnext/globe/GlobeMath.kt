package com.pdig.uivnext.globe

import androidx.compose.ui.geometry.Offset
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.atan2
import kotlin.math.sqrt
import kotlin.math.PI

/**
 * Globe 2.5D 程序化渲染几何（纯数学、平台无关、确定性）。
 *
 * 无 3D 引擎：球体采用正交投影 + 深度着色（RENDERER_LIMITATION 见 docs/ui-vnext/GLOBE_TECH_INVENTORY.md）。
 * 仅依赖合规的 bundled 坐标集与程序化大气；零网络、零新增依赖。
 */

data class Vec3(val x: Float, val y: Float, val z: Float)

/** 相机：yaw 绕竖直轴（经度视角）、pitch 绕水平轴（纬度视角）、zoom 缩放。 */
data class GlobeCamera(val yawDeg: Float, val pitchDeg: Float, val zoom: Float = 1f)

/** 投影结果：zDepth ∈ [-1,1]，>0 表示朝向前方半球（可见）。 */
data class Projected(val x: Float, val y: Float, val zDepth: Float)

private fun rad(d: Float) = d / 180f * PI.toFloat()

/** 经纬度 → 单位球向量（y 向上，z 朝初始观察者）。 */
fun latLonToVec(latDeg: Float, lonDeg: Float): Vec3 {
    val phi = rad(latDeg)
    val lambda = rad(lonDeg)
    return Vec3(
        x = cos(phi) * cos(lambda),
        y = sin(phi),
        z = cos(phi) * sin(lambda),
    )
}

private fun rotateY(p: Vec3, yawDeg: Float): Vec3 {
    val a = rad(yawDeg)
    val c = cos(a)
    val s = sin(a)
    return Vec3(c * p.x + s * p.z, p.y, -s * p.x + c * p.z)
}

private fun rotateX(p: Vec3, pitchDeg: Float): Vec3 {
    val a = rad(pitchDeg)
    val c = cos(a)
    val s = sin(a)
    return Vec3(p.x, c * p.y - s * p.z, s * p.y + c * p.z)
}

fun rotatePoint(p: Vec3, cam: GlobeCamera): Vec3 = rotateX(rotateY(p, cam.yawDeg), cam.pitchDeg)

/**
 * 正交投影：zDepth>0 可见；zoom 改变半径。
 * 返回 `(camera 使目标经纬度对准屏幕中心)`。
 */
fun focusCamera(latDeg: Float, lonDeg: Float): GlobeCamera {
    val p = latLonToVec(latDeg, lonDeg)
    // 先绕 Y 使 P.x' = 0，再绕 X 使 y'' = 0 —— 目标点即落在观察轴 (0,0,z>0)。
    val yaw = atan2(-p.x, p.z) / PI.toFloat() * 180f
    val afterYaw = rotateY(p, yaw)
    val pitch = atan2(afterYaw.y, sqrt(afterYaw.x * afterYaw.x + afterYaw.z * afterYaw.z)) / PI.toFloat() * 180f
    return GlobeCamera(yawDeg = yaw, pitchDeg = pitch)
}

/** 单位向量 → 屏幕坐标（圆心 + 半径）。 */
fun project(v: Vec3, cam: GlobeCamera, radiusPx: Float, centerX: Float, centerY: Float): Projected {
    val r = rotatePoint(v, cam)
    return Projected(
        x = centerX + radiusPx * r.x,
        y = centerY - radiusPx * r.y,
        zDepth = r.z,
    )
}

/** 点的可见深度因子（0=最远不可见 → 1=正对观察者）。 */
fun depthFactor(zDepth: Float): Float = ((zDepth + 1f) / 2f).coerceIn(0f, 1f)

/** 锚点命中测试：屏幕距离 ≤ 阈值 且 位于前半球。 */
fun isAnchorHit(anchorScreen: Projected, pointerX: Float, pointerY: Float, hitRadiusPx: Float): Boolean {
    if (anchorScreen.zDepth <= 0f) return false
    val dx = anchorScreen.x - pointerX
    val dy = anchorScreen.y - pointerY
    return dx * dx + dy * dy <= hitRadiusPx * hitRadiusPx
}

/** 球面上两点间的大圆插值采样（用于弧线分段渲染）。 */
fun greatCircleSamples(
    fromLat: Float, fromLon: Float,
    toLat: Float, toLon: Float,
    steps: Int,
): List<Vec3> {
    if (steps < 2) return emptyList()
    val p1 = latLonToVec(fromLat, fromLon)
    val p2 = latLonToVec(toLat, toLon)
    // 球面线性插值（slerp）；退化时线性回退。
    val dot = (p1.x * p2.x + p1.y * p2.y + p1.z * p2.z).coerceIn(-1f, 1f)
    val omega = kotlin.math.acos(dot)
    val sinOmega = sin(omega)
    return (0..steps).map { i ->
        val t = i.toFloat() / steps.toFloat()
        if (sinOmega < 0.0001f) {
            Vec3(
                x = p1.x + (p2.x - p1.x) * t,
                y = p1.y + (p2.y - p1.y) * t,
                z = p1.z + (p2.z - p1.z) * t,
            ).normalized()
        } else {
            val a = sin((1f - t) * omega) / sinOmega
            val b = sin(t * omega) / sinOmega
            Vec3(
                x = a * p1.x + b * p2.x,
                y = a * p1.y + b * p2.y,
                z = a * p1.z + b * p2.z,
            )
        }
    }
}

private fun Vec3.normalized(): Vec3 {
    val len = sqrt(x * x + y * y + z * z).coerceAtLeast(1e-6f)
    return Vec3(x / len, y / len, z / len)
}

/** 经纬度网格线（每 stepDeg 度一条，lat 含 ±80 上界，避免极点密集）。 */
fun graticuleLines(stepDeg: Int = 30): Pair<List<Pair<Float, Float>>, List<Pair<Float, Float>>> {
    val parallels = mutableListOf<Pair<Float, Float>>()
    val meridians = mutableListOf<Pair<Float, Float>>()
    var lat = -60f
    while (lat <= 60f) {
        parallels.add(lat to 0f)
        lat += stepDeg
    }
    var lon = -180f
    while (lon < 180f) {
        meridians.add(0f to lon)
        lon += stepDeg
    }
    return parallels to meridians
}

/** 相机逆旋转（屏幕/相机空间 → 世界空间）；供 TextureEarthRenderer 逐像素取色。 */
fun inverseRotatePoint(p: Vec3, cam: GlobeCamera): Vec3 {
    val a = rad(cam.pitchDeg)
    val c = cos(a)
    val s = sin(a)
    val rx = Vec3(p.x, c * p.y + s * p.z, -s * p.y + c * p.z)
    val b = rad(cam.yawDeg)
    val c2 = cos(b)
    val s2 = sin(b)
    return Vec3(c2 * rx.x - s2 * rx.z, rx.y, s2 * rx.x + c2 * rx.z)
}

/** 球心/半径（IntSize 版本；离屏与交互共用）。 */
fun globeMetrics(size: androidx.compose.ui.unit.IntSize, zoom: Float): Pair<Offset, Float> {
    val d = minOf(size.width, size.height).toFloat()
    val radius = d * 0.36f * zoom
    return Offset(size.width / 2f, size.height / 2f) to radius
}

/** 球心/半径（Size 版本）。 */
fun globeMetrics(size: androidx.compose.ui.geometry.Size, zoom: Float): Pair<Offset, Float> {
    val d = minOf(size.width, size.height)
    val radius = d * 0.36f * zoom
    return Offset(size.width / 2f, size.height / 2f) to radius
}

/** 经纬度锚点 → 屏幕坐标（仅前半球）。 */
fun anchorScreen(
    region: com.pdig.uivnext.model.RegionPresentation,
    cam: GlobeCamera,
    center: Offset,
    radius: Float,
): Pair<Offset, Float>? {
    val p = project(latLonToVec(region.latitude.toFloat(), region.longitude.toFloat()), cam, radius, center.x, center.y)
    if (p.zDepth <= 0f) return null
    return Offset(p.x, p.y) to p.zDepth
}

/** 锚点命中容差。 */
fun hitTolerance(radius: Float): Float = (radius * 0.05f).coerceIn(10f, 18f)