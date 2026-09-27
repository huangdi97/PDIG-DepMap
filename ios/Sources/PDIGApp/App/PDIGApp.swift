// PDIGApp —— @main 入口 + 根视图。
//
// 根分流：boot 阶段 → Onboarding（未配置解锁）→ Lock（已配置但锁定）→ Home。
// 截图 harness：命令行带 --screenshot 时直接渲染各屏并退出（macOS CI 用）。

import SwiftUI
import PDIGCore

@main
struct PDIGApp: App {

    @StateObject private var session = AppSession()

    init() {
        // 截图 harness 模式：同步渲染 + 写 PNG 后退出（不在 App 生命周期内）。
        // 该路径在真实启动主线程上执行；ImageRenderer 需要 MainActor（Swift 5.9 隔离检查）。
        if CommandLine.arguments.contains("--screenshot") {
            MainActor.assumeIsolated {
                ScreenshotHarness.runAndExit(arguments: CommandLine.arguments)
            }
        }
        // UITest 演示模式（XCUITest）：注入演示图并跳过 Onboarding/Lock。
        // 模拟器无法通过 LocalAuthentication 生物识别解锁，因此 UI 测试用
        // 启动参数直入 ready；不改产品启动分流（boot() 不受影响）。
        if CommandLine.arguments.contains("--uitest-demo") {
            session.seedDemo()
        }
    }

    var body: some Scene {
        WindowGroup {
            RootView(session: session)
                .onAppear {
                    if session.phase == .booting {
                        session.boot(dataDirectory: FileStore.dataDirectory())
                    }
                }
        }
    }
}

/// 根视图：按会话阶段分流。
struct RootView: View {
    @ObservedObject var session: AppSession

    var body: some View {
        Group {
            switch session.phase {
            case .booting:
                ProgressView(CopyZh.loading)
                    .frame(minWidth: 360, minHeight: 480)
            case .onboarding:
                OnboardingView(session: session)
            case .locked:
                LockView(session: session)
            case .ready:
                MainRouterView(session: session)
            }
        }
        .frame(minWidth: 420, minHeight: 600)
    }
}

/// 就绪后的路由容器。
struct MainRouterView: View {
    @ObservedObject var session: AppSession

    var body: some View {
        switch session.route {
        case .home: HomeScreen(session: session)
        case .findings: FindingsScreen(session: session)
        case .finding(let id): FindingDetailScreen(session: session, findingId: id)
        case .infrastructure: InfrastructureScreen(session: session)
        case .node(let id): NodeDetailScreen(session: session, nodeId: id)
        case .scenarioCenter: ScenarioCenterScreen(session: session)
        case .scenario(let id): ScenarioFlowScreen(session: session, scenarioId: id)
        case .plan(let id): ChangePlanScreen(session: session, planId: id)
        case .verification: VerificationScreen(session: session)
        case .timeline: TimelineScreen(session: session)
        case .backup: BackupScreen(session: session)
        case .importFlow: ImportScreen(session: session)
        case .drift(let id): DriftScreen(session: session, driftId: id)
        case .settings: SettingsScreen(session: session)
        case .onboarding, .lock: EmptyView()
        }
    }
}

/// 顶层导航栏组件。
struct AppTopBar: View {
    let title: String
    var onBack: (() -> Void)? = nil

    var body: some View {
        HStack {
            if let onBack = onBack {
                Button {
                    onBack()
                } label: {
                    Label(CopyZh.back, systemImage: "chevron.left")
                }
            }
            Spacer()
            Text(title).font(.headline)
            Spacer()
            if onBack != nil {
                Color.clear.frame(width: 60)
            }
        }
        .padding(.horizontal)
        .padding(.vertical, 10)
    }
}
