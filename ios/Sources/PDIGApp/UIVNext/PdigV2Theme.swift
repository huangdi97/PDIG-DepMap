// PdigV2Theme —— PDIG UI vNext 设计令牌层（iOS SwiftUI 移植）。
//
// 结构 token（间距 / 圆角 / 字号 / motion / globe primitive）继续来自
// GeneratedPdigV2Tokens.swift。环境色遵循 Android Reference Freeze 后的
// platform-translation 规则：iOS consumer workspace = light-first；Globe /
// financial asset face = local dark material。页面仍禁止散落未经语义命名的颜色。

import SwiftUI
import Foundation

// MARK: - Color 解析助手（hex + rgba 两种 token 格式）

extension Color {
    /// 解析 "#RRGGBB"（DESIGN_TOKENS colors 格式）。
    init(hex: String) {
        var cleaned = hex.trimmingCharacters(in: .whitespacesAndNewlines)
        if cleaned.hasPrefix("#") { cleaned.removeFirst() }
        var value: UInt64 = 0
        Scanner(string: cleaned).scanHexInt64(&value)
        let r = Double((value >> 16) & 0xFF) / 255.0
        let g = Double((value >> 8) & 0xFF) / 255.0
        let b = Double(value & 0xFF) / 255.0
        self.init(red: r, green: g, blue: b, opacity: 1.0)
    }

    /// 解析 "rgba(r, g, b, a)"（token surfaceGlass / borderSubtle 等格式）。
    static func rgbaToken(_ value: String) -> Color {
        var inner = value
        if inner.hasPrefix("rgba(") { inner.removeFirst(5) }
        if inner.hasSuffix(")") { inner.removeLast() }
        let parts = inner.split(separator: ",").map { $0.trimmingCharacters(in: .whitespaces) }
        let nums = parts.compactMap { Double($0) }
        guard nums.count == 4 else {
            // 解析失败时回落到中性灰（token 值来自 codegen，理论上不会走到）。
            return Color(red: 0.5, green: 0.5, blue: 0.5, opacity: 1.0)
        }
        return Color(red: nums[0] / 255.0, green: nums[1] / 255.0, blue: nums[2] / 255.0, opacity: nums[3])
    }

    /// 统一入口：GeneratedPdigV2Tokens 中 hex 与 rgba 字符串都可以解析。
    static func fromToken(_ value: String) -> Color {
        value.hasPrefix("#") ? Color(hex: value) : Color.rgbaToken(value)
    }
}

// MARK: - vNext semantic palette（platform translation + frozen spatial tokens）

/// 环境 / 数据 / 状态色。普通页面消费 light semantic palette；空间材质单独命名。
enum PdigV2Colors {
    // iOS platform translation of the frozen Android light-first reference.
    // Generated dark tokens remain the desktop reference and continue to feed
    // spatial/earth primitives; ordinary consumer surfaces are light-first.
    static let canvas = Color(hex: "#F5F8FD")
    static let canvasDeep = Color(hex: "#EAF2FF")
    static let surface = Color.white
    static let surfaceRaised = Color(hex: "#F3F7FD")
    static let surfaceGlass = Color.white.opacity(0.91)
    static let borderSubtle = Color(hex: "#315178").opacity(0.12)
    static let borderStrong = Color(hex: "#3977E8").opacity(0.30)
    static let primary = Color(hex: "#2F6BFF")
    static let primaryBright = Color(hex: "#1769FF")
    static let primaryText = Color(hex: "#0F5CDB")
    static let primarySoft = Color(hex: "#E8F0FF")
    static let textPrimary = Color(hex: "#10213A")
    static let textSecondary = Color(hex: "#40536F")
    static let textMuted = Color(hex: "#5A6E89")
    static let positive = Color(hex: "#0B7C57")
    static let warning = Color(hex: "#9A5A00")
    static let critical = Color(hex: "#C23A47")
    static let unknown = Color(hex: "#5D6678")

    // Local dark materials. Globe and financial card faces stay spatial/dark
    // even though the surrounding workspace is light.
    static let globeDeep = Color(hex: "#06162F")
    static let globeSurface = Color(hex: "#10141E")
    static let globeSurfaceRaised = Color(hex: "#1A2231")
    static let globeTextPrimary = Color(hex: "#F7FAFF")
    static let globeTextSecondary = Color(hex: "#C9D8F2")
    static let globeTextMuted = Color(hex: "#A6B7D2")
    static let assetSurface = Color(hex: "#10141E")
    static let assetSurfaceRaised = Color(hex: "#1A2231")
    static let assetTextPrimary = Color(hex: "#F7FAFF")
    static let assetTextSecondary = Color(hex: "#D4DFF2")
    static let assetTextMuted = Color(hex: "#A6B7D2")

    static let regionNodeHi = Color.fromToken(GeneratedPdigV2Tokens.colorsRegionNodeHi)
    static let regionNodeLo = Color.fromToken(GeneratedPdigV2Tokens.colorsRegionNodeLo)
    static let arcActive = Color.fromToken(GeneratedPdigV2Tokens.colorsArcActive)
    static let arcQuiet = Color.fromToken(GeneratedPdigV2Tokens.colorsArcQuiet)
    static let atmosphereInner = Color.fromToken(GeneratedPdigV2Tokens.colorsAtmosphereInner)
    static let atmosphereOuter = Color.fromToken(GeneratedPdigV2Tokens.colorsAtmosphereOuter)
    static let focusRing = primaryBright
}

/// 语义状态色（icon+label+color 三通道；色仅为第三通道；desktop PdigV2Theme 移植）。
func vStatusColor(_ status: String) -> Color {
    switch status {
    case "critical", "blocked", "expiring_soon": return PdigV2Colors.critical
    case "warning", "verifying": return PdigV2Colors.warning
    case "completed", "active", "ok", "verified", "migrated": return PdigV2Colors.positive
    case "unknown": return PdigV2Colors.unknown
    default: return PdigV2Colors.textMuted
    }
}

/// 状态图标（SF Symbols；shape 通道，状态永不只靠颜色）。
func vStatusIcon(_ status: String) -> String {
    switch status {
    case "critical", "blocked", "expiring_soon": return "exclamationmark.triangle.fill"
    case "verifying", "waiting", "not_started", "upcoming": return "hourglass"
    case "completed", "active", "ok", "verified", "migrated": return "checkmark.circle.fill"
    default: return "questionmark.circle"
    }
}

// MARK: - 间距 / 圆角 / 字号（数值来自 GeneratedPdigV2Tokens）

enum VSpace {
    static let xs: CGFloat = CGFloat(GeneratedPdigV2Tokens.spacingXs)
    static let sm: CGFloat = CGFloat(GeneratedPdigV2Tokens.spacingSm)
    static let md: CGFloat = CGFloat(GeneratedPdigV2Tokens.spacingMd)
    static let lg: CGFloat = CGFloat(GeneratedPdigV2Tokens.spacingLg)
    static let xl: CGFloat = CGFloat(GeneratedPdigV2Tokens.spacingXl)
    static let xxl: CGFloat = CGFloat(GeneratedPdigV2Tokens.spacingXxl)
    static let xxxl: CGFloat = CGFloat(GeneratedPdigV2Tokens.spacingXxxl)
    static let pagePadding: CGFloat = CGFloat(GeneratedPdigV2Tokens.spacingPagePadding)
    static let sectionGap: CGFloat = CGFloat(GeneratedPdigV2Tokens.spacingSectionGap)
    static let gridGap: CGFloat = CGFloat(GeneratedPdigV2Tokens.spacingGridGap)
    static let gridGapWide: CGFloat = CGFloat(GeneratedPdigV2Tokens.spacingGridGapWide)
}

enum VRadius {
    static let sm: CGFloat = CGFloat(GeneratedPdigV2Tokens.radiusSm)
    static let md: CGFloat = CGFloat(GeneratedPdigV2Tokens.radiusMd)
    static let lg: CGFloat = CGFloat(GeneratedPdigV2Tokens.radiusLg)
    static let xl: CGFloat = CGFloat(GeneratedPdigV2Tokens.radiusXl)
}

/// 字号角色（typography tokens → SwiftUI system font；禁远程字体，offline-first）。
enum VFont {
    // vNext uses semantic system styles for product copy so Dynamic Type can
    // participate in layout. Large numeric / globe display values remain
    // explicit presentation sizes at their call sites.
    static func pageTitle() -> Font {
        .system(.title2, design: .default, weight: .bold)
    }

    static func sectionTitle() -> Font {
        .system(.headline, design: .default, weight: .semibold)
    }

    static func body() -> Font {
        .system(.body, design: .default, weight: .regular)
    }

    static func secondary() -> Font {
        .system(.subheadline, design: .default, weight: .regular)
    }

    static func meta() -> Font {
        .system(.caption, design: .default, weight: .regular)
    }

    static func label() -> Font {
        .system(.caption, design: .default, weight: .semibold)
    }

    static func displayGlobe() -> Font {
        .system(.title3, design: .default, weight: .semibold)
    }

    static func mono() -> Font {
        .system(.callout, design: .monospaced, weight: .regular)
    }
}

/// iOS 触控热区下限（DESIGN_TOKENS components.touchTarget.ios = 44）。
enum VTouchTarget {
    static let ios: CGFloat = CGFloat(GeneratedPdigV2Tokens.componentsTouchTargetIos)
}

/// 动效时长（MOTION_CONTRACT.json tokens）。
enum VMotion {
    static let fast = Double(GeneratedPdigV2Tokens.motionFast) / 1000.0
    static let normal = Double(GeneratedPdigV2Tokens.motionNormal) / 1000.0
    static let slow = Double(GeneratedPdigV2Tokens.motionSlow) / 1000.0
    static let globeFocus = Double(GeneratedPdigV2Tokens.motionGlobeFocus) / 1000.0
    static let idleRotationDegPerSec = CGFloat(GeneratedPdigV2Tokens.globeIdleRotationDegPerSec)
    static let maxZoom = CGFloat(GeneratedPdigV2Tokens.globeMaxZoom)
    static let minZoom = CGFloat(GeneratedPdigV2Tokens.globeMinZoom)
    static let anchorDiameter = CGFloat(GeneratedPdigV2Tokens.globeRegionAnchorDiameter)
    static let anchorActiveDiameter = CGFloat(GeneratedPdigV2Tokens.globeRegionAnchorActiveDiameter)
}
