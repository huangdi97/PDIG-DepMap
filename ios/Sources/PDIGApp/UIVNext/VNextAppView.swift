// VNextAppView —— vNext 演示 Shell（phone: TabView bottom nav；iPad: NavigationSplitView）。
//
// RESPONSIVE_CONTRACT.json ios.phone: NavigationStack + List + sheets + toolbar；
// ios.ipad: NavigationSplitView (Infrastructure)、expanded globe。
// 一级导航永远可见（IA.md §7）；testId 经 accessibilityIdentifier 承载（pdig.nav.*）。

import SwiftUI

/// vNext 演示应用状态（Presentation Layer；不触碰真实 domain repos）。
/// regionFilter / globe 选中态由这里持有，避免 duplicated state。
@MainActor
final class VNextModel: ObservableObject {
    @Published var screen: VScreen = .now
    @Published var regionFilter: String? = nil
    @Published var globeDetailShown = false
    @Published var privacyMask = true
    @Published var reduceMotion = false

    func navigate(_ next: VScreen) { screen = next }

    func openCard(_ cardId: String) { screen = .cardDetail(cardId) }
    func openNumber(_ numberId: String) { screen = .numberDetail(numberId) }
    func openCardCustomization(_ cardId: String) { screen = .cardCustomization(cardId) }
    func openNumberCustomization(_ numberId: String) { screen = .numberCustomization(numberId) }

    /// Globe / Region List → REGION_SELECTED（区域过滤 + focus 副作用）。
    func selectRegion(_ code: String) {
        regionFilter = code
        globeDetailShown = false
    }

    func openRegionDetail() { globeDetailShown = true }

    /// REGION_DETAIL / REGION_SELECTED → GLOBAL（Escape 逐级回退）。
    func backToGlobal() {
        if globeDetailShown {
            globeDetailShown = false
        } else {
            regionFilter = nil
        }
    }

    /// 详情页返回父级语义（INTERACTION_CONTRACT §6）。
    func back() {
        switch screen {
        case .cardDetail, .cardCustomization: screen = .cards
        case .numberDetail, .numberCustomization: screen = .numbers
        default: screen = .now
        }
    }
}

/// vNext 应用入口（深空主题唯一；dark spatial）。
public struct VNextAppView: View {
    @StateObject private var model = VNextModel()

    public init() {}

    public var body: some View {
        Group {
            #if os(iOS)
            if UIDevice.current.userInterfaceIdiom == .pad {
                VNextSplitShell(model: model)
            } else {
                VNextTabShell(model: model)
            }
            #else
            VNextTabShell(model: model)
            #endif
        }
        .preferredColorScheme(.dark)
        .tint(PdigV2Colors.primaryBright)
    }
}

/// 手机形态：底部 TabView（≤5 项；一级导航 4 项永远可见，IA.md §1/§7）。
struct VNextTabShell: View {
    @ObservedObject var model: VNextModel

    var body: some View {
        TabView(selection: $model.screen) {
            NavigationStack { NowView(model: model) }
                .tabItem { Label(VCopy.navNow, systemImage: "house.fill") }
                .tag(VScreen.now)
                .accessibilityIdentifier(VTestIds.navNow)
            NavigationStack { VInfrastructureHost(model: model) }
                .tabItem { Label(VCopy.navInfrastructure, systemImage: "globe.asia.australia.fill") }
                .tag(VScreen.infrastructure)
                .accessibilityIdentifier(VTestIds.navInfrastructure)
            NavigationStack { ChangePhoneView(model: model) }
                .tabItem { Label(VCopy.navChange, systemImage: "arrow.triangle.2.circlepath") }
                .tag(VScreen.change)
                .accessibilityIdentifier(VTestIds.navChange)
            NavigationStack { VPlaceholderScreen(screen: .records) }
                .tabItem { Label(VCopy.navRecords, systemImage: "clock") }
                .tag(VScreen.records)
                .accessibilityIdentifier(VTestIds.navRecords)
        }
        .accessibilityIdentifier("pdig.nav.tab")
    }
}

/// iPad 形态：NavigationSplitView（Infrastructure 为 detail；侧栏 = 一级导航 + 设置）。
struct VNextSplitShell: View {
    @ObservedObject var model: VNextModel

    var body: some View {
        NavigationSplitView {
            List {
                Section(VCopy.navNow) {
                    Button { model.screen = .now } label: { Label(VCopy.navNow, systemImage: "house.fill") }
                }
                Section(VCopy.navInfrastructure) {
                    Button { model.screen = .overview } label: { Label(VCopy.navInfraOverview, systemImage: "globe.asia.australia.fill") }
                    Button { model.screen = .cards } label: { Label(VCopy.navInfraCards, systemImage: "creditcard.fill") }
                    Button { model.screen = .numbers } label: { Label(VCopy.navInfraNumbers, systemImage: "phone.fill") }
                }
                Section(VCopy.navChange) {
                    Button { model.screen = .changePhone } label: { Label(VCopy.quickChangePhone, systemImage: "arrow.triangle.2.circlepath") }
                }
                Section(VCopy.navSettings) {
                    Button { model.screen = .personalization } label: { Label(VCopy.navSettings, systemImage: "gearshape.fill") }
                }
            }
            .listStyle(.sidebar)
            .navigationTitle("PDIG vNext")
        } detail: {
            VInfrastructureHost(model: model)
        }
        .accessibilityIdentifier(VTestIds.navSettings)
    }
}

/// 基础设施二级内容宿主（screen 分发；卡片/号码是二级页，非一级导航）。
struct VInfrastructureHost: View {
    @ObservedObject var model: VNextModel

    var body: some View {
        Group {
            switch model.screen {
            case .overview, .infrastructure: OverviewView(model: model)
            case .cards: CardsView(model: model)
            case .cardDetail: CardDetailView(model: model)
            case .cardCustomization: CardCustomizationView(model: model)
            case .numbers: NumbersView(model: model)
            case .numberDetail: NumberDetailView(model: model)
            case .numberCustomization: NumberCustomizationView(model: model)
            case .accounts, .emails, .devices, .services, .weaknesses:
                VPlaceholderScreen(screen: model.screen)
            default: OverviewView(model: model)
            }
        }
    }
}

/// 占位页（accounts/emails/devices/services/weaknesses/records/sources——vNext 范围外）。
struct VPlaceholderScreen: View {
    let screen: VScreen

    var body: some View {
        VStack(alignment: .leading, spacing: VSpace.lg) {
            Text(title)
                .font(VFont.pageTitle())
                .foregroundColor(PdigV2Colors.textPrimary)
            Text("该页面将在后续迭代接入：当前 IA 已包含 \(title)（route=\(screen.route)），v0.x 焦点为「卡片」与「号码」。")
                .font(VFont.body())
                .foregroundColor(PdigV2Colors.textSecondary)
                .padding(VSpace.xl)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(PdigV2Colors.surface.opacity(0.9))
                .clipShape(RoundedRectangle(cornerRadius: VRadius.lg, style: .continuous))
            Spacer()
        }
        .padding(VSpace.pagePadding)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
    }

    private var title: String {
        switch screen {
        case .records: return VCopy.navRecords
        case .sources: return VCopy.navSources
        case .accounts: return VCopy.navInfraAccounts
        case .emails: return VCopy.navInfraEmails
        case .devices: return VCopy.navInfraDevices
        case .services: return VCopy.navInfraServices
        case .weaknesses: return VCopy.navInfraWeaknesses
        default: return ""
        }
    }
}

/// 详情页共用返回按钮（back 语义 = 回到父级列表）。
struct VBackButton: View {
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: "chevron.left")
                .font(.system(size: 15, weight: .semibold))
                .foregroundColor(PdigV2Colors.primaryBright)
                .frame(width: 44, height: 44)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel("返回")
    }
}

/// 页面根背景（L0 深空渐变）。
struct VPageBackground: ViewModifier {
    func body(content: Content) -> some View {
        content
            .background(
                LinearGradient(
                    colors: [PdigV2Colors.canvas, PdigV2Colors.canvasDeep],
                    startPoint: .top, endPoint: .bottom
                )
            )
    }
}

extension View {
    func vPageBackground() -> some View { self.modifier(VPageBackground()) }
}
