// GlobeMath —— Globe 2.5D 程序化渲染几何（纯 Swift、平台无关、确定性）。
//
// 移植自 desktop/app/src/main/kotlin/com/pdig/uivnext/globe/GlobeMath.kt。
// 无 3D 引擎：正交投影 + 深度着色；零网络、零新增依赖。
// 语义保持与 Desktop 一致：yaw 绕竖直轴（经度视角）、pitch 绕水平轴（纬度视角）、
// zDepth ∈ [-1,1]，>0 表示朝向前方半球（可见）。

import Foundation

/// 单位球向量（y 向上，z 朝初始观察者）。
struct VVec3: Equatable {
    var x: CGFloat
    var y: CGFloat
    var z: CGFloat
}

/// 相机：yaw（经度视角）/ pitch（纬度视角，clamp ±60°）/ zoom（0.7–1.9）。
struct VGlobeCamera: Equatable {
    var yawDeg: CGFloat
    var pitchDeg: CGFloat
    var zoom: CGFloat = 1.0
}

/// 正交投影结果。
struct VProjected: Equatable {
    var x: CGFloat
    var y: CGFloat
    var zDepth: CGFloat
}

/// 纯几何函数集（移植 Desktop GlobeMath；输入单位：度）。
enum GlobeMath {

    static func rad(_ d: CGFloat) -> CGFloat { d * .pi / 180.0 }

    /// 经纬度 → 单位球向量。
    static func latLonToVec(latDeg: CGFloat, lonDeg: CGFloat) -> VVec3 {
        let phi = rad(latDeg)
        let lambda = rad(lonDeg)
        return VVec3(
            x: cos(phi) * cos(lambda),
            y: sin(phi),
            z: cos(phi) * sin(lambda)
        )
    }

    static func rotateY(_ p: VVec3, yawDeg: CGFloat) -> VVec3 {
        let a = rad(yawDeg)
        let c = cos(a)
        let s = sin(a)
        return VVec3(x: c * p.x + s * p.z, y: p.y, z: -s * p.x + c * p.z)
    }

    static func rotateX(_ p: VVec3, pitchDeg: CGFloat) -> VVec3 {
        let a = rad(pitchDeg)
        let c = cos(a)
        let s = sin(a)
        return VVec3(x: p.x, y: c * p.y - s * p.z, z: s * p.y + c * p.z)
    }

    static func rotatePoint(_ p: VVec3, cam: VGlobeCamera) -> VVec3 {
        rotateX(rotateY(p, yawDeg: cam.yawDeg), pitchDeg: cam.pitchDeg)
    }

    /// 目标经纬度对准屏幕中心的相机（focus/flyTo 目标）。
    static func focusCamera(latDeg: CGFloat, lonDeg: CGFloat) -> VGlobeCamera {
        let p = latLonToVec(latDeg: latDeg, lonDeg: lonDeg)
        // 先绕 Y 使 P.x' = 0，再绕 X 使 y'' = 0 —— 目标点即落在观察轴 (0,0,z>0)。
        let yaw = atan2(-p.x, p.z) / .pi * 180.0
        let afterYaw = rotateY(p, yawDeg: yaw)
        let pitch = atan2(afterYaw.y, sqrt(afterYaw.x * afterYaw.x + afterYaw.z * afterYaw.z)) / .pi * 180.0
        return VGlobeCamera(yawDeg: yaw, pitchDeg: pitch)
    }

    /// 单位向量 → 屏幕坐标（圆心 + 半径）。
    static func project(_ v: VVec3, cam: VGlobeCamera, radiusPx: CGFloat, centerX: CGFloat, centerY: CGFloat) -> VProjected {
        let r = rotatePoint(v, cam: cam)
        return VProjected(
            x: centerX + radiusPx * r.x,
            y: centerY - radiusPx * r.y,
            zDepth: r.z
        )
    }

    /// 点的可见深度因子（0=最远不可见 → 1=正对观察者）。
    static func depthFactor(_ zDepth: CGFloat) -> CGFloat {
        clamp01((zDepth + 1.0) / 2.0)
    }

    /// 锚点命中测试：屏幕距离 ≤ 阈值 且位于前半球。
    static func isAnchorHit(_ anchor: VProjected, pointerX: CGFloat, pointerY: CGFloat, hitRadiusPx: CGFloat) -> Bool {
        guard anchor.zDepth > 0 else { return false }
        let dx = anchor.x - pointerX
        let dy = anchor.y - pointerY
        return dx * dx + dy * dy <= hitRadiusPx * hitRadiusPx
    }

    /// 球面两点间大圆插值采样（弧线分段渲染；slerp + 退化线性回退）。
    static func greatCircleSamples(
        fromLat: CGFloat, fromLon: CGFloat,
        toLat: CGFloat, toLon: CGFloat,
        steps: Int
    ) -> [VVec3] {
        guard steps >= 2 else { return [] }
        let p1 = latLonToVec(latDeg: fromLat, lonDeg: fromLon)
        let p2 = latLonToVec(latDeg: toLat, lonDeg: toLon)
        let dot = coerce(p1.x * p2.x + p1.y * p2.y + p1.z * p2.z, min: -1.0, max: 1.0)
        let omega = acos(dot)
        let sinOmega = sin(omega)
        var samples: [VVec3] = []
        samples.reserveCapacity(steps + 1)
        for i in 0...steps {
            let t = CGFloat(i) / CGFloat(steps)
            if sinOmega < 0.0001 {
                samples.append(normalized(VVec3(
                    x: p1.x + (p2.x - p1.x) * t,
                    y: p1.y + (p2.y - p1.y) * t,
                    z: p1.z + (p2.z - p1.z) * t
                )))
            } else {
                let a = sin((1.0 - t) * omega) / sinOmega
                let b = sin(t * omega) / sinOmega
                samples.append(VVec3(
                    x: a * p1.x + b * p2.x,
                    y: a * p1.y + b * p2.y,
                    z: a * p1.z + b * p2.z
                ))
            }
        }
        return samples
    }

    /// 经纬网格线（每 stepDeg 一条；lat ±60 上界避免极点密集）。
    static func graticuleLines(stepDeg: Int = 30) -> (parallels: [(CGFloat, CGFloat)], meridians: [(CGFloat, CGFloat)]) {
        var parallels: [(CGFloat, CGFloat)] = []
        var meridians: [(CGFloat, CGFloat)] = []
        var lat: CGFloat = -60
        while lat <= 60 {
            parallels.append((lat, 0))
            lat += CGFloat(stepDeg)
        }
        var lon: CGFloat = -180
        while lon < 180 {
            meridians.append((0, lon))
            lon += CGFloat(stepDeg)
        }
        return (parallels, meridians)
    }

    // MARK: - private helpers

    private static func clamp01(_ v: CGFloat) -> CGFloat { min(max(v, 0), 1) }
    private static func coerce(_ v: CGFloat, min lo: CGFloat, max hi: CGFloat) -> CGFloat { min(max(v, lo), hi) }

    private static func normalized(_ v: VVec3) -> VVec3 {
        let len = sqrt(v.x * v.x + v.y * v.y + v.z * v.z)
        let safeLen = max(len, 1e-6)
        return VVec3(x: v.x / safeLen, y: v.y / safeLen, z: v.z / safeLen)
    }
}
