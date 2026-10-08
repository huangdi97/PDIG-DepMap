// PdigV2Theme —— iOS / iPadOS UI vNext platform translation.
//
// Product truth and shared spacing/type scale still come from the generated token contract.
// Color is intentionally platform-translated here: Desktop Dark stays frozen; iOS follows the
// accepted mobile light-first direction while keeping Globe/Card/Number identity canvases dark.
// This is presentation-only and never changes Canonical / PersonalReality / .depmap.

import SwiftUI
import Foundation

extension Color {
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

    static func rgbaToken(_ value: String) -> Color {
        var inner = value
        if inner.hasPrefix("rgba(") { inner.removeFirst(5) }
        if inner.hasSuffix(")") { inner.removeLast() }
        let parts = inner.split(separator: ",").map { $0.trimmingCharacters(in: .whitespaces) }
        let nums = parts.compactMap { Double($0) }
        guard nums.count == 4 else {
            return Color(red: 0.5, green: 0.5, blue: 0.5, opacity: 1.0)
        }
        return Color(red: nums[0] / 255.0, green: nums[1] / 255.0, blue: nums[2] / 255.0, opacity: nums[3])
    }

    static func fromToken(_ value: String) -> Color {
        value.hasPrefix("#") ? Color(hex: value) : Color.rgbaToken(value)
    }
}

/// iOS light-first palette. Brand blue is intentionally restrained in controls; content/identity owns the visual
/// character. Dark is localized to Globe and asset identity surfaces.
enum PdigV2Colors {
    static let canvas = Color(hex: "#F7F9FD")
    static let canvasDeep = Color(hex: "#EDF4FF")
    static let surface = Color.white
    static let surfaceRaised = Color(hex: "#F3F7FD")
    static let surfaceGlass = Color.white.opacity(0.88)
    static let borderSubtle = Color(hex: "#315178").opacity(0.14)
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

    // Local dark canvases.
    static let globeDeep = Color(hex: "#06162F")
    static let globeTextPrimary = Color(hex: "#F7FAFF")
    static let globeTextSecondary = Color(hex: "#C9D8F2")
    static let assetTextPrimary = Color(hex: "#F7FAFF")
    static let assetTextSecondary = Color(hex: "#D4DFF2")
    static let assetTextMuted = Color(hex: "#A6B7D2")

    // Spatial data colors remain shared with generated token semantics.
    static let regionNodeHi = Color.fromToken(GeneratedPdigV2Tokens.colorsRegionNodeHi)
    static let regionNodeLo = Color.rgbaToken(GeneratedPdigV2Tokens.colorsRegionNodeLo)
    static let arcActive = Color.rgbaToken(GeneratedPdigV2Tokens.colorsArcActive)
    static let arcQuiet = Color.rgbaToken(GeneratedPdigV2Tokens.colorsArcQuiet)
    static let atmosphereInner = Color.rgbaToken(GeneratedPdigV2Tokens.colorsAtmosphereInner)
    static let atmosphereOuter = Color.rgbaToken(GeneratedPdigV2Tokens.colorsAtmosphereOuter)
    static let focusRing = primaryBright
}

func vStatusColor(_ status: String) -> Color {
    switch status {
    case "critical", "blocked", "expiring_soon": return PdigV2Colors.critical
    case "warning", "verifying", "plan", "unresolved": return PdigV2Colors.warning
    case "completed", "active", "ok", "verified", "migrated": return PdigV2Colors.positive
    case "unknown": return PdigV2Colors.unknown
    default: return PdigV2Colors.textMuted
    }
}

func vStatusIcon(_ status: String) -> String {
    switch status {
    case "critical", "blocked", "expiring_soon": return "exclamationmark.triangle.fill"
    case "verifying", "waiting", "not_started", "upcoming": return "hourglass"
    case "completed", "active", "ok", "verified", "migrated": return "checkmark.circle.fill"
    default: return "questionmark.circle"
    }
}

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

enum VFont {
    // iOS uses semantic text styles so Dynamic Type remains a platform capability.
    // Token sizes remain the cross-platform visual reference; the native translation
    // intentionally prefers scalable system typography over fixed desktop pixel sizes.
    static func pageTitle() -> Font { .title2.weight(.bold) }
    static func sectionTitle() -> Font { .headline.weight(.semibold) }
    static func body() -> Font { .body }
    static func secondary() -> Font { .subheadline }
    static func meta() -> Font { .caption }
    static func label() -> Font { .caption.weight(.semibold) }
    static func displayGlobe() -> Font { .title.weight(.semibold) }
    static func mono() -> Font { .body.monospaced() }
}

enum VTouchTarget {
    static let ios: CGFloat = CGFloat(GeneratedPdigV2Tokens.componentsTouchTargetIos)
}

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
