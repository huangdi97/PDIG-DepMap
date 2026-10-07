// VEarthTextureRenderer —— bundled, offline real-Earth material for the iOS/iPadOS Globe.
//
// Source assets are the same frozen public-domain material used by Desktop/Android.
// Rendering is presentation-only: equirectangular albedo/night/cloud textures are projected onto
// the visible hemisphere; region anchors/arcs still come only from recorded relationships.

import SwiftUI
import Foundation
import CoreGraphics
import ImageIO

final class VEarthTexture {
    let width: Int
    let height: Int
    private let rgba: [UInt8]

    init?(url: URL) {
        guard let source = CGImageSourceCreateWithURL(url as CFURL, nil),
              let image = CGImageSourceCreateImageAtIndex(source, 0, nil) else { return nil }

        width = image.width
        height = image.height
        var bytes = [UInt8](repeating: 0, count: width * height * 4)
        guard let context = CGContext(
            data: &bytes,
            width: width,
            height: height,
            bitsPerComponent: 8,
            bytesPerRow: width * 4,
            space: CGColorSpaceCreateDeviceRGB(),
            bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue
        ) else { return nil }

        // Normalize row zero to texture top, matching equirectangular v = 0 at +90° latitude.
        context.translateBy(x: 0, y: CGFloat(height))
        context.scaleBy(x: 1, y: -1)
        context.draw(image, in: CGRect(x: 0, y: 0, width: width, height: height))
        rgba = bytes
    }

    func sample(u: CGFloat, v: CGFloat) -> (CGFloat, CGFloat, CGFloat) {
        let wrappedU = u - floor(u)
        let clampedV = min(max(v, 0), 1)
        let x = min(max(Int(wrappedU * CGFloat(width - 1)), 0), width - 1)
        let y = min(max(Int(clampedV * CGFloat(height - 1)), 0), height - 1)
        let i = (y * width + x) * 4
        return (
            CGFloat(rgba[i]) / 255.0,
            CGFloat(rgba[i + 1]) / 255.0,
            CGFloat(rgba[i + 2]) / 255.0
        )
    }
}

final class VEarthMaterialAssets {
    let albedo: VEarthTexture
    let night: VEarthTexture?
    let clouds: VEarthTexture?

    private init(albedo: VEarthTexture, night: VEarthTexture?, clouds: VEarthTexture?) {
        self.albedo = albedo
        self.night = night
        self.clouds = clouds
    }

    static let shared: VEarthMaterialAssets? = {
        guard let albedoURL = resourceURL("earth_albedo_2048"),
              let albedo = VEarthTexture(url: albedoURL) else { return nil }
        let night = resourceURL("earth_night_lights_2048").flatMap(VEarthTexture.init(url:))
        let clouds = resourceURL("cloud_2048").flatMap(VEarthTexture.init(url:))
        return VEarthMaterialAssets(albedo: albedo, night: night, clouds: clouds)
    }()

    private static func resourceURL(_ name: String) -> URL? {
        #if SWIFT_PACKAGE
        let bundle = Bundle.module
        #else
        let bundle = Bundle.main
        #endif
        return bundle.url(forResource: name, withExtension: "png", subdirectory: "Earth")
            ?? bundle.url(forResource: name, withExtension: "png")
    }
}

@MainActor
final class VEarthTextureRenderer: ObservableObject {
    @Published private(set) var image: CGImage?
    private var task: Task<Void, Never>?
    private var activeKey: String?

    deinit { task?.cancel() }

    func request(camera: VGlobeCamera, diameter: CGFloat) {
        let rect = min(max(Int(diameter.rounded()), 96), 384)
        let key = Self.cacheKey(camera: camera, rect: rect)
        guard key != activeKey else { return }
        activeKey = key
        task?.cancel()

        let frozenCamera = camera
        task = Task { [weak self] in
            let rendered = await Task.detached(priority: .userInitiated) {
                Self.render(camera: frozenCamera, rect: rect)
            }.value
            guard !Task.isCancelled, self?.activeKey == key else { return }
            self?.image = rendered
        }
    }

    static func cacheKey(camera: VGlobeCamera, rect: Int) -> String {
        let yaw = (camera.yawDeg / 4).rounded() * 4
        let pitch = (camera.pitchDeg / 3).rounded() * 3
        let zoom = (camera.zoom / 0.05).rounded() * 0.05
        return "\(rect)|\(Int(yaw))|\(Int(pitch))|\(String(format: "%.2f", Double(zoom)))"
    }

    private static func render(camera: VGlobeCamera, rect: Int) -> CGImage? {
        guard let assets = VEarthMaterialAssets.shared else { return nil }

        let half = CGFloat(rect) / 2
        let inv = 1 / max(half, 1)
        let sun = GlobeMath.latLonToVec(latDeg: 14, lonDeg: -12)
        var bytes = [UInt8](repeating: 0, count: rect * rect * 4)

        for y in 0..<rect {
            if Task.isCancelled { return nil }
            let sy = (CGFloat(y) - half) * inv
            for x in 0..<rect {
                let sx = (CGFloat(x) - half) * inv
                let r2 = sx * sx + sy * sy
                if r2 > 1 { continue }

                // Screen y grows downward, so camera-space y is inverted.
                let cameraPoint = VVec3(x: sx, y: -sy, z: sqrt(max(0, 1 - r2)))
                let world = inverseRotate(cameraPoint, camera: camera)

                let lat = asin(min(max(world.y, -1), 1))
                let lon = atan2(world.z, world.x)
                let u = lon / (2 * .pi) + 0.5
                let v = 0.5 - lat / .pi

                let base = assets.albedo.sample(u: u, v: v)
                let dot = min(max(world.x * sun.x + world.y * sun.y + world.z * sun.z, -1), 1)
                let day = min(max((dot + 0.30) / 1.30, 0), 1)
                let lit = 0.30 + 0.70 * day

                var red = base.0 * lit
                var green = base.1 * lit
                var blue = base.2 * lit

                if let night = assets.night {
                    let nightFactor = (1 - day) * (1 - day)
                    if nightFactor > 0.02 {
                        let light = night.sample(u: u, v: v)
                        let weight = nightFactor * 0.92
                        red = red * (1 - weight) + light.0 * weight
                        green = green * (1 - weight) + light.1 * weight
                        blue = blue * (1 - weight) + light.2 * weight
                    }
                }

                if let clouds = assets.clouds {
                    let cloud = clouds.sample(u: u, v: v)
                    let luminance = (cloud.0 + cloud.1 + cloud.2) / 3
                    let weight = 0.22 * luminance * luminance
                    red = red * (1 - weight) + 0.91 * weight
                    green = green * (1 - weight) + 0.94 * weight
                    blue = blue * (1 - weight) + 0.99 * weight
                }

                // Soft limb shading keeps the globe spherical rather than a pasted disc.
                let limb = 0.72 + 0.28 * cameraPoint.z
                red *= limb
                green *= limb
                blue *= limb

                let i = (y * rect + x) * 4
                bytes[i] = UInt8(min(max(red, 0), 1) * 255)
                bytes[i + 1] = UInt8(min(max(green, 0), 1) * 255)
                bytes[i + 2] = UInt8(min(max(blue, 0), 1) * 255)
                bytes[i + 3] = 255
            }
        }

        guard let provider = CGDataProvider(data: Data(bytes) as CFData) else { return nil }
        return CGImage(
            width: rect,
            height: rect,
            bitsPerComponent: 8,
            bitsPerPixel: 32,
            bytesPerRow: rect * 4,
            space: CGColorSpaceCreateDeviceRGB(),
            bitmapInfo: CGBitmapInfo(rawValue: CGImageAlphaInfo.premultipliedLast.rawValue),
            provider: provider,
            decode: nil,
            shouldInterpolate: true,
            intent: .defaultIntent
        )
    }

    private static func inverseRotate(_ point: VVec3, camera: VGlobeCamera) -> VVec3 {
        // Forward projection is rotateY(yaw) then rotateX(pitch).
        // Inverse therefore applies X(-pitch) then Y(-yaw).
        GlobeMath.rotateY(
            GlobeMath.rotateX(point, pitchDeg: -camera.pitchDeg),
            yawDeg: -camera.yawDeg
        )
    }
}
