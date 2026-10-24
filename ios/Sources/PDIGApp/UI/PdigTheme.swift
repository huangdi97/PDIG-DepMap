// PdigTheme —— PDIG iOS 设计令牌层（Quiet Infrastructure）。
//
// 值来源：docs/uiux/PDIG_DESIGN_SYSTEM.md §1–§4 与 spec/ui/design-tokens.json。
// 铁律：UI 层禁止再散用裸色值 / 圆角 / 间距 / 字号；新视觉一律走这里。
// 语义（spec）：Observation ≠ Reality / unknown ≠ required / done ≠ verified。

import SwiftUI
#if os(macOS)
import AppKit
#endif

/// PDIG iOS 设计令牌（SwiftUI 单例式访问）。
enum PdigTheme {

    // MARK: - 颜色令牌

    enum Color {
        /// 从 0xRRGGBB 拆解为 sRGB 分量。
        private static func components(_ hex: UInt32) -> (Double, Double, Double) {
            (
                Double((hex >> 16) & 0xFF) / 255.0,
                Double((hex >> 8) & 0xFF) / 255.0,
                Double(hex & 0xFF) / 255.0
            )
        }

        /// 随系统外观自动切换的令牌（light / dark 各用 design-tokens 的值）。
        static func dynamic(light: UInt32, dark: UInt32) -> SwiftUI.Color {
            #if os(macOS)
            let lightNS = components(light)
            let darkNS = components(dark)
            return SwiftUI.Color(nsColor: NSColor(name: nil) { appearance in
                let isDark = appearance.bestMatch(from: [.darkAqua, .aqua]) == .darkAqua
                let c = isDark ? darkNS : lightNS
                return NSColor(srgbRed: c.0, green: c.1, blue: c.2, alpha: 1)
            })
            #else
            return SwiftUI.Color(uiColor: UIColor { traits in
                let c = traits.userInterfaceStyle == .dark ? components(dark) : components(light)
                return UIColor(red: c.0, green: c.1, blue: c.2, alpha: 1)
            })
            #endif
        }

        // 品牌
        static let primary = dynamic(light: 0x4C4FD8, dark: 0x8B8EF5)
        static let primarySoft = dynamic(light: 0xEEEEFB, dark: 0x242541)

        // 中性
        static let background = dynamic(light: 0xF5F6FA, dark: 0x12131A)
        static let surface = dynamic(light: 0xFFFFFF, dark: 0x1C1E28)
        static let surfaceElevated = dynamic(light: 0xFFFFFF, dark: 0x232633)
        static let surfaceLow = dynamic(light: 0xEDEFF6, dark: 0x232633)
        static let textPrimary = dynamic(light: 0x1B1D29, dark: 0xF2F3F7)
        static let textSecondary = dynamic(light: 0x5A5F73, dark: 0xB4B9C9)
        static let textTertiary = dynamic(light: 0x8A90A6, dark: 0x828799)
        static let border = dynamic(light: 0xE6E8F0, dark: 0x2C2F3D)

        // 语义（only 表达状态，信息仍靠 icon + label + color 三通道）
        static let success = dynamic(light: 0x2BA471, dark: 0x3FBF87)
        static let warning = dynamic(light: 0xD98E04, dark: 0xE0A63A)
        static let danger = dynamic(light: 0xD54941, dark: 0xE4695F)
        static let info = dynamic(light: 0x3B6FD8, dark: 0x5B8CE8)
        static let disabled = dynamic(light: 0xB9BDCC, dark: 0x4A4E5E)
    }

    // MARK: - 圆角 / 间距（4pt 网格）

    enum Radius {
        static let sm: CGFloat = 6   // chip / 小徽标
        static let md: CGFloat = 8   // 卡片 / 按钮 / 输入
        static let lg: CGFloat = 12  // 对话框 / 浮层
    }

    enum Spacing {
        static let xs: CGFloat = 4
        static let sm: CGFloat = 8
        static let md: CGFloat = 12
        static let lg: CGFloat = 16
        static let xl: CGFloat = 20
        static let xxl: CGFloat = 24
    }

    // MARK: - 字号角色（system / CJK，禁远程字体）

    enum Font {
        static let pageTitle = SwiftUI.Font.system(size: 24, weight: .semibold)
        static let section = SwiftUI.Font.system(size: 15, weight: .semibold)
        static let sectionBody = SwiftUI.Font.system(size: 16, weight: .semibold)
        static let body = SwiftUI.Font.system(size: 14)
        static let secondary = SwiftUI.Font.system(size: 12.5)
        static let label = SwiftUI.Font.system(size: 12, weight: .medium)
        static let button = SwiftUI.Font.system(size: 14, weight: .medium)
    }
}

/// 时间辅助：真实 now（ISO-8601 UTC），供 Timeline/Findings 投影。
/// 生产 UI 禁止固定假时间；输出格式与旧假值一致（…Z），PDIGCore
/// parseIsoEpoch / PlanRules.parseIso 均已支持。
enum PdigClock {
    static func nowIso() -> String {
        ISO8601DateFormatter().string(from: Date())
    }
}