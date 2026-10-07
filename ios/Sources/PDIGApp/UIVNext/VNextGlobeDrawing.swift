// VNextGlobeDrawing —— VNextGlobeView 的 Canvas 绘制细节（深度着色/网格/弧线/锚点）。
//
// 与 desktop VNextGlobe.kt 的 Canvas 绘制语义一致：
//  - 球体程序化深度渐变（禁照片纹理）；经纬网格按深度淡出；
//  - 弧线只来自真实跨区关系（arcCountPolicy），选中地区相关弧线高亮；
//  - 锚点 14→22 语义：视觉直径跟随深度（近大远小），active/hover 放大 + 聚焦光环。

import SwiftUI

extension VNextGlobeView {

    /// 球体画布（L0 大气 → 深度着色球体 → 网格 → 弧线 → 锚点）。
    func canvas(in size: CGSize) -> some View {
        Canvas { context, canvasSize in
            let center = CGPoint(x: canvasSize.width / 2, y: canvasSize.height / 2)
            let radius = min(canvasSize.width, canvasSize.height) * 0.36 * camera.zoom
            let effectiveYaw = camera.yawDeg + (isIdle ? yawBase : 0)
            let cam = VGlobeCamera(yawDeg: effectiveYaw, pitchDeg: camera.pitchDeg, zoom: camera.zoom)

            // L1 Atmosphere（radial 由 atmosphereInner → atmosphereOuter）
            let atmosphere = GraphicsContext.Shading.radialGradient(
                Gradient(stops: [
                    .init(color: PdigV2Colors.atmosphereInner, location: 0),
                    .init(color: PdigV2Colors.atmosphereOuter, location: 1),
                ]),
                center: center, startRadius: 0, endRadius: radius * 1.7
            )
            context.fill(
                Path(ellipseIn: CGRect(x: 0, y: 0, width: canvasSize.width, height: canvasSize.height)),
                with: .color(PdigV2Colors.globeDeep)
            )
            context.fill(Path(ellipseIn: circleRect(center: center, radius: radius * 1.7)), with: atmosphere)

            // Sphere（程序化深度着色；禁照片纹理；L0）
            let sphereShading = GraphicsContext.Shading.radialGradient(
                Gradient(colors: [
                    Color(hex: "#1A4A72"),
                    Color(hex: "#0D294B"),
                    Color(hex: "#06162F"),
                ]),
                center: CGPoint(x: center.x - radius * 0.35, y: center.y - radius * 0.35),
                startRadius: 0, endRadius: radius * 1.4
            )
            context.fill(Path(ellipseIn: circleRect(center: center, radius: radius)), with: sphereShading)

            // 经纬网格（borderSubtle；前半球按深度淡出）
            drawGraticule(context: &context, cam: cam, center: center, radius: radius)

            // 跨区弧线（只有真实跨区关系；绝不装饰性连线）
            drawArcs(context: &context, cam: cam, center: center, radius: radius)

            // 地区锚点
            drawAnchors(context: &context, cam: cam, center: center, radius: radius)
        }
    }

    func circleRect(center: CGPoint, radius: CGFloat) -> CGRect {
        CGRect(x: center.x - radius, y: center.y - radius, width: radius * 2, height: radius * 2)
    }

    func drawGraticule(context: inout GraphicsContext, cam: VGlobeCamera, center: CGPoint, radius: CGFloat) {
        let gridColor = PdigV2Colors.globeTextSecondary.opacity(0.18)
        let lines = GlobeMath.graticuleLines(stepDeg: 30)
        for (lat, lon) in lines.parallels {
            drawSphericalLine(context: &context, samples: GlobeMath.greatCircleSamples(
                fromLat: lat, fromLon: lon, toLat: lat + 20, toLon: lon + 20, steps: 24
            ), cam: cam, center: center, radius: radius, color: gridColor)
        }
        for (lat, lon) in lines.meridians {
            drawSphericalLine(context: &context, samples: GlobeMath.greatCircleSamples(
                fromLat: lat, fromLon: lon, toLat: lat + 10, toLon: lon, steps: 24
            ), cam: cam, center: center, radius: radius, color: gridColor)
            drawSphericalLine(context: &context, samples: GlobeMath.greatCircleSamples(
                fromLat: lat, fromLon: lon, toLat: lat + 10, toLon: lon + 10, steps: 24
            ), cam: cam, center: center, radius: radius, color: gridColor)
        }
    }

    func drawArcs(context: inout GraphicsContext, cam: VGlobeCamera, center: CGPoint, radius: CGFloat) {
        for (aCode, bCode) in arcingPairs {
            guard let a = regions.first(where: { $0.regionCode == aCode }),
                  let b = regions.first(where: { $0.regionCode == bCode }) else { continue }
            let isSelectedLink = selectedRegion == aCode || selectedRegion == bCode
            let samples = GlobeMath.greatCircleSamples(
                fromLat: CGFloat(a.latitude), fromLon: CGFloat(a.longitude),
                toLat: CGFloat(b.latitude), toLon: CGFloat(b.longitude), steps: 48
            )
            drawSphericalLine(
                context: &context,
                samples: samples,
                cam: cam,
                center: center,
                radius: radius,
                color: isSelectedLink ? PdigV2Colors.arcActive : PdigV2Colors.arcQuiet,
                width: isSelectedLink ? 1.8 : 1.2
            )
        }
    }

    /// 按深度把一条球面线段分小段描（前/后半球交界、按 zDepth 淡出）。
    func drawSphericalLine(
        context: inout GraphicsContext,
        samples: [VVec3],
        cam: VGlobeCamera,
        center: CGPoint,
        radius: CGFloat,
        color: Color,
        width: CGFloat = 1.2
    ) {
        guard !samples.isEmpty else { return }
        var prev = GlobeMath.project(samples[0], cam: cam, radiusPx: radius, centerX: center.x, centerY: center.y)
        var drawing = prev.zDepth > -0.05
        for v in samples.dropFirst() {
            let cur = GlobeMath.project(v, cam: cam, radiusPx: radius, centerX: center.x, centerY: center.y)
            let visible = cur.zDepth > -0.05
            if visible || drawing {
                let alpha = (cur.zDepth.clamped(to: 0...1) * 0.9 + 0.1).clamped(to: 0.08...1)
                var path = Path()
                path.move(to: CGPoint(x: prev.x, y: prev.y))
                path.addLine(to: CGPoint(x: cur.x, y: cur.y))
                context.stroke(path, with: .color(color.opacity(alpha)), lineWidth: width)
            }
            drawing = visible
            prev = cur
        }
    }

    func drawAnchors(context: inout GraphicsContext, cam: VGlobeCamera, center: CGPoint, radius: CGFloat) {
        for r in regions {
            let p = GlobeMath.latLonToVec(latDeg: CGFloat(r.latitude), lonDeg: CGFloat(r.longitude))
            let projected = GlobeMath.project(p, cam: cam, radiusPx: radius, centerX: center.x, centerY: center.y)
            guard projected.zDepth > 0 else { continue }
            let depth = GlobeMath.depthFactor(projected.zDepth)
            let isSelected = selectedRegion == r.regionCode
            let isHovered = hoveredRegionCode == r.regionCode
            let emphasis: CGFloat = (isSelected || isHovered) ? 1.6 : 1.0
            // 视觉直径跟随深度（近大远小）；active 放大（regionAnchorDiameter 14 / Active 22 语义）。
            let diameter = radius * (0.02 + 0.012 * depth) * emphasis
            let anchorColor = (isSelected || isHovered) ? PdigV2Colors.regionNodeHi : PdigV2Colors.regionNodeLo
            let rect = CGRect(
                x: projected.x - diameter / 2,
                y: projected.y - diameter / 2,
                width: diameter, height: diameter
            )
            context.fill(Path(ellipseIn: rect), with: .color(anchorColor.opacity(0.5 + 0.5 * depth)))
            if isSelected || isHovered {
                // 聚焦光环（semantic.focus ring 近似）
                let halo = CGRect(
                    x: projected.x - diameter * 1.1,
                    y: projected.y - diameter * 1.1,
                    width: diameter * 2.2, height: diameter * 2.2
                )
                context.stroke(Path(ellipseIn: halo), with: .color(PdigV2Colors.focusRing.opacity(0.45)), lineWidth: 1.5)
            }
        }
    }
}
