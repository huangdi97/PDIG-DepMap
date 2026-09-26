// ScreenshotHarness —— CI 截图取证（task：generate screenshots）。
//
// 运行方式（macOS CI）：`swift run PDIGApp --screenshot --runid <id> --output <dir>`
// 用 SwiftUI ImageRenderer 把关键屏渲染成 PNG（light + dark），写入
// `artifacts/runtime-evidence/ios-n4-<runid>/`，并生成 evidence-index.json。
//
// 诚实口径：这些是 **macOS 上渲染的 SwiftUI 捕获**（MACOS_RENDER），
// 不是模拟器/真机截图；没有跑 XCUITest。证据索引里逐项标注 IOS_VISUAL。

import SwiftUI
import PDIGCore
import ImageIO
import UniformTypeIdentifiers

public enum ScreenshotHarness {

    public static func runAndExit(arguments: [String]) -> Never {
        let runId = value(of: "--runid", in: arguments) ?? UUID().uuidString.prefix(8).description
        let outputDir = value(of: "--output", in: arguments)
            ?? "artifacts/runtime-evidence/ios-n4-\(runId)"
        renderAll(runId: runId, outputDir: outputDir)
        exit(0)
    }

    private static func value(of flag: String, in args: [String]) -> String? {
        guard let i = args.firstIndex(of: flag), i + 1 < args.count else { return nil }
        return args[i + 1]
    }

    private static func renderAll(runId: String, outputDir: String) {
        let session = demoSession()
        let screens: [(name: String, view: AnyView)] = [
            ("Home", AnyView(HomeScreen(session: session))),
            ("Findings", AnyView(FindingsScreen(session: session))),
            ("Infrastructure", AnyView(InfrastructureScreen(session: session))),
            ("Scenario", AnyView(ScenarioCenterScreen(session: session))),
            ("ChangePlan", AnyView(ChangePlanScreen(session: session, planId: DemoData.plans().first?.id ?? "plan-replace_phone_number-phone-1-3"))),
            ("Verification", AnyView(VerificationScreen(session: session))),
            ("Backup", AnyView(BackupScreen(session: session))),
            ("Import", AnyView(ImportScreen(session: session))),
            ("Timeline", AnyView(TimelineScreen(session: session))),
        ]

        var indexFields: [(String, Json)] = [
            ("runId", .str(runId)),
            ("renderTarget", .str("macOS SwiftUI ImageRenderer")),
        ]
        var files: [(String, Json)] = []
        for screen in screens {
            let light = render(screen.view, scheme: .light)
            let dark = render(screen.view, scheme: .dark)
            if let light = light {
                let name = writePNG(light, name: "ios-n4-\(runId)-\(screen.name)-light.png", dir: outputDir)
                files.append((name, .str("MACOS_RENDER")))
            }
            if let dark = dark {
                let name = writePNG(dark, name: "ios-n4-\(runId)-\(screen.name)-dark.png", dir: outputDir)
                files.append((name, .str("MACOS_RENDER")))
            }
        }
        indexFields.append(("files", .obj(JsonObject(files))))
        indexFields.append(("labels", .obj(JsonObject([
            ("IOS_VISUAL", .str("MacOSRender (MACOS_RENDER) — not simulator, not device, no XCUITest")),
            ("IOS_SQLCIPHER_PERSISTENCE", .str("NOT_RUN")),
            ("IOS_DEVICE_RUNTIME", .str("NOT_RUN")),
        ]))))
        writeIndex(JsonWriter.write(.obj(JsonObject(indexFields))), dir: outputDir)
        print("[PDIGApp] screenshots -> \(outputDir)")
    }

    private static func demoSession() -> AppSession {
        let s = AppSession()
        s.snapshot = DemoData.snapshot()
        s.plans = DemoData.plans()
        s.phase = .ready
        return s
    }

    /// 渲染为 CGImage（尺寸固定便于比对）。
    private static func render(_ view: AnyView, scheme: ColorScheme) -> CGImage? {
        let renderer = ImageRenderer(
            content: view
                .preferredColorScheme(scheme)
                .frame(width: 420, height: 640)
        )
        renderer.scale = 2
        #if os(macOS)
        return renderer.cgImage
        #else
        // iOS 上 ImageRenderer 的 cgImage 也可用（本包只在 macOS 上运行 harness）。
        return renderer.cgImage
        #endif
    }

    private static func writePNG(_ image: CGImage, name: String, dir: String) -> String {
        let fm = FileManager.default
        try? fm.createDirectory(atPath: dir, withIntermediateDirectories: true)
        let path = (dir as NSString).appendingPathComponent(name)
        guard let dest = CGImageDestinationCreateWithURL(
            URL(fileURLWithPath: path) as CFURL, UTType.png.identifier as CFString, 1, nil
        ) else { return name }
        CGImageDestinationAddImage(dest, image, nil)
        CGImageDestinationFinalize(dest)
        return name
    }

    private static func writeIndex(_ json: String, dir: String) {
        let fm = FileManager.default
        try? fm.createDirectory(atPath: dir, withIntermediateDirectories: true)
        let path = (dir as NSString).appendingPathComponent("evidence-index.json")
        try? json.write(toFile: path, atomically: true, encoding: .utf8)
    }
}