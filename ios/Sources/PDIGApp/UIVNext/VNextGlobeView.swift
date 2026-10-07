// VNextGlobeView —— SwiftUI Canvas 程序化 2.5D 球体（Global Infrastructure Navigator）。
//
// 移植自 desktop/app/.../globe/VNextGlobe.kt（行为 oracle）：
//  - 深度着色 / 经纬网格 / 地区锚点 / 跨区弧线（真实关系，禁止装饰性连线）；
//  - DragGesture 旋转（水平=经度、垂直=纬度 clamp ±60°）、MagnifyGesture 缩放（0.7–1.9）、
//    TapGesture 聚焦 / 二次点击打开 Region Detail、空白点击回 Global；
//  - 交互期间暂停 idle rotation；reduce motion 关 idle（截图冻结相机状态）。
//  - 非唯一导航：Globe 只是导航增强，必须存在 Region List 替代（由 OverviewView 提供）。
// 零新增 3D 依赖、offline-first；只渲染聚合计数与地区坐标。
// 绘制细节见 VNextGlobeDrawing.swift（该文件需访问 camera/yawBase/hoveredRegionCode，故为 internal）。

import SwiftUI

/// Globe 视图输入（region filter / selected 由父级持有，避免 duplicated state）。
struct VNextGlobeView: View {
    let regions: [RegionPresentation]
    let arcingPairs: [(String, String)]
    var reduceMotion: Bool = true
    /// 截图确定性相机入口（--globe-camera=global|cn|hk|gb|us）。
    var initialCamera: VGlobeCamera = VGlobeCamera(yawDeg: 0, pitchDeg: 30, zoom: 1)
    @Binding var selectedRegion: String?
    var onOpenRegionDetail: () -> Void = {}
    var onSelectRegion: (RegionPresentation) -> Void = { _ in }
    var onBackToGlobal: () -> Void = {}

    // 以下状态被同模块的 VNextGlobeDrawing.swift 扩展访问（绘制层），故不使用 private。
    @State var camera: VGlobeCamera
    @State var yawBase: CGFloat = 0
    @State var hoveredRegionCode: String? = nil
    @StateObject var earthRenderer = VEarthTextureRenderer()
    @State private var interacting = false
    @State private var dragStart: CGSize? = nil
    @State private var dragStartCamera: VGlobeCamera? = nil

    init(
        regions: [RegionPresentation],
        arcingPairs: [(String, String)],
        reduceMotion: Bool = true,
        initialCamera: VGlobeCamera = VGlobeCamera(yawDeg: 0, pitchDeg: 30, zoom: 1),
        selectedRegion: Binding<String?> = .constant(nil),
        onOpenRegionDetail: @escaping () -> Void = {},
        onSelectRegion: @escaping (RegionPresentation) -> Void = { _ in },
        onBackToGlobal: @escaping () -> Void = {}
    ) {
        self.regions = regions
        self.arcingPairs = arcingPairs
        self.reduceMotion = reduceMotion
        self.initialCamera = initialCamera
        self._selectedRegion = selectedRegion
        self.onOpenRegionDetail = onOpenRegionDetail
        self.onSelectRegion = onSelectRegion
        self.onBackToGlobal = onBackToGlobal
        _camera = State(initialValue: initialCamera)
    }

    var body: some View {
        GeometryReader { geo in
            ZStack(alignment: .topLeading) {
                canvas(in: geo.size)
                    .accessibilityLabel("全球基础设施球体")
                    .accessibilityHint("点选地区可聚焦并查看该地区资产；也可使用下方地区列表。")
                    .accessibilityIdentifier(VTestIds.globeCanvas)

                // 地区 tooltip / 标签（hover 或选中时显示）
                toolbarPanels(in: geo.size)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(PdigV2Colors.globeDeep)
            .task(id: earthRenderToken(size: geo.size)) {
                earthRenderer.request(camera: effectiveCamera, diameter: earthDiameter(size: geo.size))
            }
            .contentShape(Rectangle())
            // DRAG：旋转球体（水平=经度，垂直=纬度，纬度 clamp ±60°）；交互期间暂停 idle。
            .gesture(
                DragGesture(minimumDistance: 4)
                    .onChanged { value in
                        interacting = true
                        if dragStart == nil {
                            dragStart = value.translation
                            dragStartCamera = camera
                        }
                        guard let start = dragStart, let startCam = dragStartCamera else { return }
                        let dx = value.translation.width - start.width
                        let dy = value.translation.height - start.height
                        camera = VGlobeCamera(
                            yawDeg: startCam.yawDeg - dx * 0.3,
                            pitchDeg: (startCam.pitchDeg - dy * 0.3).clamped(to: -60...60),
                            zoom: startCam.zoom
                        )
                    }
                    .onEnded { _ in
                        dragStart = nil
                        dragStartCamera = nil
                        interacting = false
                    }
            )
            // MAGNIFY：缩放（clamp 0.7–1.9，DESIGN_TOKENS globe.minZoom/maxZoom）。
            .simultaneousGesture(
                MagnificationGesture()
                    .onChanged { scale in
                        interacting = true
                        camera = VGlobeCamera(
                            yawDeg: camera.yawDeg,
                            pitchDeg: camera.pitchDeg,
                            zoom: (camera.zoom * scale).clamped(to: 0.7...1.9)
                        )
                    }
                    .onEnded { _ in interacting = false }
            )
            // TAP：命中锚点 → 聚焦 / 二次点击开 Region Detail；空白 → 回 Global。
            .simultaneousGesture(
                SpatialTapGesture(count: 1)
                    .onEnded { value in
                        handleTap(at: value.location, size: geo.size)
                    }
            )
            // HOVER（macOS 指针环境；iOS 触控依赖 Region List 替代）。
            #if os(macOS)
            .onContinuousHover { phase in
                switch phase {
                case .active(let location):
                    hoveredRegionCode = regionCode(at: location, size: geo.size)
                case .ended:
                    hoveredRegionCode = nil
                }
            }
            #endif
        }
        // 极慢 idle rotation：仅在非 reduce-motion / 非交互 / 未选中时推进（截图冻结相机状态）。
        .task(id: idleToken) {
            guard isIdle else { return }
            while !Task.isCancelled {
                try? await Task.sleep(nanoseconds: 50_000_000)
                yawBase = (yawBase + VMotion.idleRotationDegPerSec * 0.05)
                    .truncatingRemainder(dividingBy: 360)
            }
        }
        // 选中态复位时清空 hovered。
        .onChange(of: selectedRegion) { newValue in
            if newValue == nil { hoveredRegionCode = nil }
        }
    }

    var effectiveCamera: VGlobeCamera {
        VGlobeCamera(
            yawDeg: camera.yawDeg + (isIdle ? yawBase : 0),
            pitchDeg: camera.pitchDeg,
            zoom: camera.zoom
        )
    }

    func earthDiameter(size: CGSize) -> CGFloat {
        min(size.width, size.height) * 0.72 * camera.zoom
    }

    func earthRenderToken(size: CGSize) -> String {
        VEarthTextureRenderer.cacheKey(
            camera: effectiveCamera,
            rect: min(max(Int(earthDiameter(size: size).rounded()), 96), 384)
        )
    }

    private var idleToken: String {
        "\(reduceMotion)-\(interacting)-\(selectedRegion ?? "nil")"
    }

    var isIdle: Bool {
        !reduceMotion && !interacting && selectedRegion == nil
    }

    // MARK: - Hit testing / tap

    private func anchorAt(point: CGPoint, size: CGSize) -> RegionPresentation? {
        let center = CGPoint(x: size.width / 2, y: size.height / 2)
        let radius = min(size.width, size.height) * 0.36 * camera.zoom
        let effectiveYaw = camera.yawDeg + (isIdle ? yawBase : 0)
        let cam = VGlobeCamera(yawDeg: effectiveYaw, pitchDeg: camera.pitchDeg, zoom: camera.zoom)
        let tolerance = (radius * 0.05).clamped(to: 10...18)
        var best: (RegionPresentation, CGFloat)? = nil
        for r in regions {
            let p = GlobeMath.latLonToVec(latDeg: CGFloat(r.latitude), lonDeg: CGFloat(r.longitude))
            let proj = GlobeMath.project(p, cam: cam, radiusPx: radius, centerX: center.x, centerY: center.y)
            guard proj.zDepth > 0 else { continue }
            let dx = proj.x - point.x
            let dy = proj.y - point.y
            let dist = dx * dx + dy * dy
            guard dist <= tolerance * tolerance else { continue }
            if best == nil || dist < best!.1 { best = (r, dist) }
        }
        return best?.0
    }

    private func regionCode(at point: CGPoint, size: CGSize) -> String? {
        anchorAt(point: point, size: size)?.regionCode
    }

    private func handleTap(at point: CGPoint, size: CGSize) {
        if let hit = anchorAt(point: point, size: size) {
            if selectedRegion == hit.regionCode {
                onOpenRegionDetail()
            } else {
                selectedRegion = hit.regionCode
                flyTo(hit)
                onSelectRegion(hit)
            }
        } else if selectedRegion != nil {
            clearSelection()
            onBackToGlobal()
        }
    }

    /// focus/flyTo（相机移向地区坐标，620ms ease；MOTION_CONTRACT globeFocusMs）。
    private func flyTo(_ region: RegionPresentation) {
        let target = GlobeMath.focusCamera(latDeg: CGFloat(region.latitude), lonDeg: CGFloat(region.longitude))
        withAnimation(.easeInOut(duration: VMotion.globeFocus)) {
            camera = VGlobeCamera(
                yawDeg: target.yawDeg,
                pitchDeg: target.pitchDeg,
                zoom: max(camera.zoom, 1.3)
            )
        }
    }

    private func clearSelection() {
        selectedRegion = nil
        hoveredRegionCode = nil
        withAnimation(.easeInOut(duration: VMotion.fast)) {
            camera = VGlobeCamera(yawDeg: 0, pitchDeg: 30, zoom: 1)
        }
    }

    // MARK: - Overlay panels

    private func toolbarPanels(in size: CGSize) -> some View {
        let tooltipRegion = regions.first { $0.regionCode == (hoveredRegionCode ?? selectedRegion) }
        return Group {
            if let region = tooltipRegion, let pos = tooltipPosition(for: region, in: size) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(region.displayName)
                        .font(VFont.label())
                        .foregroundColor(PdigV2Colors.textPrimary)
                    Text(VCopy.regionTooltip(region))
                        .font(VFont.meta())
                        .foregroundColor(PdigV2Colors.textSecondary)
                }
                .padding(.horizontal, VSpace.md)
                .padding(.vertical, VSpace.sm)
                .background(PdigV2Colors.surfaceRaised.opacity(0.96))
                .clipShape(RoundedRectangle(cornerRadius: VRadius.md, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: VRadius.md, style: .continuous)
                        .stroke(PdigV2Colors.borderSubtle, lineWidth: 1)
                )
                .position(x: pos.x, y: pos.y)
            }
        }
    }

    private func tooltipPosition(for region: RegionPresentation, in size: CGSize) -> CGPoint? {
        let center = CGPoint(x: size.width / 2, y: size.height / 2)
        let radius = min(size.width, size.height) * 0.36 * camera.zoom
        let effectiveYaw = camera.yawDeg + (isIdle ? yawBase : 0)
        let cam = VGlobeCamera(yawDeg: effectiveYaw, pitchDeg: camera.pitchDeg, zoom: camera.zoom)
        let p = GlobeMath.latLonToVec(latDeg: CGFloat(region.latitude), lonDeg: CGFloat(region.longitude))
        let proj = GlobeMath.project(p, cam: cam, radiusPx: radius, centerX: center.x, centerY: center.y)
        guard proj.zDepth > 0 else { return nil }
        return CGPoint(x: proj.x, y: proj.y - 22)
    }
}

// MARK: - CGFloat 区间 clamp 助手

extension CGFloat {
    func clamped(to range: ClosedRange<CGFloat>) -> CGFloat {
        Swift.min(Swift.max(self, range.lowerBound), range.upperBound)
    }
}
