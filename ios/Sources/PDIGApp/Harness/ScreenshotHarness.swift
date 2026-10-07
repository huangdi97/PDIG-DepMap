// ScreenshotHarness —— CI 截图取证（task：generate screenshots）。
//
// 运行方式（macOS CI）：`swift run PDIGApp --screenshot --runid <id> --output <dir>`
// 用 SwiftUI ImageRenderer 把关键屏渲染成 PNG（light + dark），写入
// `artifacts/runtime-evidence/ios-n4-<runid>/`，并生成 evidence-index.json。
//
// 诚实口径：这些是 **macOS 上渲染的 SwiftUI 捕获**（MACOS_RENDER），
// 不是模拟器/真机截图；没有跑 XCUITest。证据索引里逐项标注 IOS_VISUAL。
// vNext 演示屏（Presentation Layer）额外标注 IOS_VNEXT_DEMO；reduceMotion 冻结相机状态。

import SwiftUI
import PDIGCore
import ImageIO
import UniformTypeIdentifiers

public enum ScreenshotHarness {

    @MainActor
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

    @MainActor
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
        ] + vnextScreens()

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
            ("IOS_VNEXT_DEMO", .str("MACOS_RENDER — vNext presentation demo, synthetic fixture, reduceMotion on")),
        ]))))
        writeIndex(JsonWriter.write(.obj(JsonObject(indexFields))), dir: outputDir)
        print("[PDIGApp] screenshots -> \(outputDir)")
    }

    /// vNext presentation evidence. Each screen owns an isolated ephemeral model so a later route
    /// mutation cannot silently change an earlier captured AnyView.
    @MainActor
    private static func vnextScreens() -> [(name: String, view: AnyView)] {
        func model(_ screen: VScreen) -> VNextModel {
            let value = VNextModel(preferencesStore: nil)
            value.reduceMotion = true
            value.screen = screen
            return value
        }
        func stack(_ content: some View) -> AnyView {
            AnyView(NavigationStack { content }.preferredColorScheme(.light))
        }

        let now = model(.now)
        let hub = model(.infrastructure)
        let overview = model(.overview)
        let cards = model(.cards)
        let cardDetail = model(.cardDetail("card-cn-2"))
        let cardStudio = model(.cardCustomization("card-cn-1"))
        let numbers = model(.numbers)
        let numberDetail = model(.numberDetail("num-cn-1"))
        let numberStudio = model(.numberCustomization("num-cn-1"))
        let accounts = model(.accounts)
        let emails = model(.emails)
        let devices = model(.devices)
        let services = model(.services)
        let weaknesses = model(.weaknesses)
        let change = model(.changePhone)
        change.changeProjection = .transition
        let records = model(.records)
        let search = model(.search)
        let sources = model(.sources)
        let personalization = model(.personalization)

        return [
            ("VNextNow", stack(NowView(model: now))),
            ("VNextInfrastructureHub", stack(InfrastructureHubView(model: hub))),
            ("VNextOverview", stack(OverviewView(model: overview))),
            ("VNextCards", stack(CardsView(model: cards))),
            ("VNextCardDetail", stack(CardDetailView(model: cardDetail))),
            ("VNextCardCustomization", stack(CardCustomizationView(model: cardStudio))),
            ("VNextNumbers", stack(NumbersView(model: numbers))),
            ("VNextNumberDetail", stack(NumberDetailView(model: numberDetail))),
            ("VNextNumberCustomization", stack(NumberCustomizationView(model: numberStudio))),
            ("VNextAccounts", stack(AccountsView(model: accounts))),
            ("VNextEmails", stack(EmailsView(model: emails))),
            ("VNextDevices", stack(DevicesView(model: devices))),
            ("VNextServices", stack(ServicesView(model: services))),
            ("VNextWeaknesses", stack(WeaknessesView(model: weaknesses))),
            ("VNextChangePhone", stack(ChangePhoneView(model: change))),
            ("VNextRecords", stack(RecordsView(model: records))),
            ("VNextSearch", stack(SearchView(model: search))),
            ("VNextSources", stack(DataSourcesView(model: sources))),
            ("VNextPersonalization", stack(PersonalizationView(model: personalization))),
        ]
    }

    private static func demoSession() -> AppSession {
        let s = AppSession()
        s.snapshot = DemoData.snapshot()
        s.plans = DemoData.plans()
        s.phase = .ready
        return s
    }

    @MainActor
    private static func render(_ view: AnyView, scheme: ColorScheme) -> CGImage? {
        let renderer = ImageRenderer(
            content: view
                .preferredColorScheme(scheme)
                .frame(width: 420, height: 640)
        )
        renderer.scale = 2
        return renderer.cgImage
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
