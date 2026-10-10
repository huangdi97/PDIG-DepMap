// PdigV2Theme —— PDIG UI vNext 设计令牌层（iOS SwiftUI 移植）。
//
// 值唯一来源：ios/Sources/PDIGApp/Generated/GeneratedPdigV2Tokens.swift
// （由 spec/ui-vnext/DESIGN_TOKENS.json codegen 生成；禁止手写不同颜色）。
// 页面禁止散落裸色值 / 圆角 / 间距；新视觉一律走这里。

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

// MARK: - vNext 颜色令牌（值全部来自 GeneratedPdigV2Tokens）

/// 环境 / 数据 / 状态色（semantic 映射同 DESIGN_TOKENS.json；页面不得散落魔数颜色）。
enum PdigV2Colors {
    static let canvas = Color.fromToken(GeneratedPdigV2Tokens.colorsCanvas)
    static let canvasDeep = Color.fromToken(GeneratedPdigV2Tokens.colorsCanvasDeep)
    static let surface = Color.fromToken(GeneratedPdigV2Tokens.colorsSurface)
    static let surfaceRaised = Color.fromToken(GeneratedPdigV2Tokens.colorsSurfaceRaised)
    static let surfaceGlass = Color.fromToken(GeneratedPdigV2Tokens.colorsSurfaceGlass)
    static let borderSubtle = Color.fromToken(GeneratedPdigV2Tokens.colorsBorderSubtle)
    static let borderStrong = Color.fromToken(GeneratedPdigV2Tokens.colorsBorderStrong)
    static let primary = Color.fromToken(GeneratedPdigV2Tokens.colorsPrimary)
    static let primaryBright = Color.fromToken(GeneratedPdigV2Tokens.colorsPrimaryBright)
    static let primarySoft = Color.fromToken(GeneratedPdigV2Tokens.colorsPrimarySoft)
    static let textPrimary = Color.fromToken(GeneratedPdigV2Tokens.colorsTextPrimary)
    static let textSecondary = Color.fromToken(GeneratedPdigV2Tokens.colorsTextSecondary)
    static let textMuted = Color.fromToken(GeneratedPdigV2Tokens.colorsTextMuted)
    static let positive = Color.fromToken(GeneratedPdigV2Tokens.colorsPositive)
    static let warning = Color.fromToken(GeneratedPdigV2Tokens.colorsWarning)
    static let critical = Color.fromToken(GeneratedPdigV2Tokens.colorsCritical)
    static let unknown = Color.fromToken(GeneratedPdigV2Tokens.colorsUnknown)
    static let regionNodeHi = Color.fromToken(GeneratedPdigV2Tokens.colorsRegionNodeHi)
    static let regionNodeLo = Color.fromToken(GeneratedPdigV2Tokens.colorsRegionNodeLo)
    static let arcActive = Color.fromToken(GeneratedPdigV2Tokens.colorsArcActive)
    static let arcQuiet = Color.fromToken(GeneratedPdigV2Tokens.colorsArcQuiet)
    static let atmosphereInner = Color.fromToken(GeneratedPdigV2Tokens.colorsAtmosphereInner)
    static let atmosphereOuter = Color.fromToken(GeneratedPdigV2Tokens.colorsAtmosphereOuter)
    /// semantic.focus = colors.primaryBright（别名；无独立色值）。
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
    static func pageTitle() -> Font {
        .system(size: CGFloat(GeneratedPdigV2Tokens.typographyPageTitleSize),
                weight: .bold, design: .default)
    }

    static func sectionTitle() -> Font {
        .system(size: CGFloat(GeneratedPdigV2Tokens.typographySectionTitleSize),
                weight: .semibold, design: .default)
    }

    static func body() -> Font {
        .system(size: CGFloat(GeneratedPdigV2Tokens.typographyBodySize),
                weight: .regular, design: .default)
    }

    static func secondary() -> Font {
        .system(size: CGFloat(GeneratedPdigV2Tokens.typographySecondarySize),
                weight: .regular, design: .default)
    }

    static func meta() -> Font {
        .system(size: CGFloat(GeneratedPdigV2Tokens.typographyMetaSize),
                weight: .regular, design: .default)
    }

    static func label() -> Font {
        .system(size: CGFloat(GeneratedPdigV2Tokens.typographyLabelSize),
                weight: .semibold, design: .default)
    }

    static func displayGlobe() -> Font {
        .system(size: CGFloat(GeneratedPdigV2Tokens.typographyDisplayGlobeSize),
                weight: .semibold, design: .default)
    }

    static func mono() -> Font {
        .system(size: CGFloat(GeneratedPdigV2Tokens.typographyMonoSize),
                weight: .regular, design: .monospaced)
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
