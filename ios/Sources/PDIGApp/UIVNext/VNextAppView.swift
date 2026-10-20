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
    @Published var privacyMask = false
    @Published var reduceMotion = false

    func navigate(_ next: VScreen) { screen = next }

    func openCard(_ cardId: String) { screen = .cardDetail(cardId) }
    func openNumber(_ numberId: String) { screen = .numberDetail(numberId) }
    func openCardCustomization(_ cardId: String) { screen = .cardCustomization(cardId) }
    func openNumberCustomization(_ numberId: String) { screen = .numberCustomization(numberId) }
    func openSecondary(_ kind: VSecondaryKind, _ id: String) { screen = .secondaryDetail(kind, id) }

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
        case .secondaryDetail(let kind, _):
            switch kind {
            case .account: screen = .accounts
            case .email: screen = .emails
            case .device: screen = .devices
            case .service: screen = .services
            }
        case .personalization, .sources, .settings: screen = .me
        default: screen = screen.primaryTab
        }
    }
}

/// vNext 应用入口（light-first workspace；Globe / asset faces retain local dark material）。
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
        .preferredColorScheme(.light)
        .tint(PdigV2Colors.primaryBright)
    }
}

/// 手机形态：底部 5-tab。Focused route 通过 primaryTab 保持一级归属，
/// 详情页不会把 TabView selection 写成一个不存在的 tag。
struct VNextTabShell: View {
    @ObservedObject var model: VNextModel

    private var primarySelection: Binding<VScreen> {
        Binding(
            get: { model.screen.primaryTab },
            set: { model.navigate($0) }
        )
    }

    var body: some View {
        TabView(selection: primarySelection) {
            NavigationStack { NowView(model: model) }
                .tabItem { Label(VCopy.navNow, systemImage: "house.fill").accessibilityIdentifier(VTestIds.navNow) }
                .tag(VScreen.now)
                .accessibilityIdentifier(VTestIds.navNow)
            NavigationStack { VInfrastructureHost(model: model) }
                .tabItem { Label(VCopy.navInfrastructure, systemImage: "globe.asia.australia.fill").accessibilityIdentifier(VTestIds.navInfrastructure) }
                .tag(VScreen.infrastructure)
                .accessibilityIdentifier(VTestIds.navInfrastructure)
            NavigationStack { ChangePhoneView(model: model) }
                .tabItem { Label(VCopy.navChange, systemImage: "arrow.triangle.2.circlepath").accessibilityIdentifier(VTestIds.navChange) }
                .tag(VScreen.change)
                .accessibilityIdentifier(VTestIds.navChange)
            NavigationStack { VRecordsView(privacyMask: model.privacyMask) }
                .tabItem { Label(VCopy.navRecords, systemImage: "clock").accessibilityIdentifier(VTestIds.navRecords) }
                .tag(VScreen.records)
                .accessibilityIdentifier(VTestIds.navRecords)
            NavigationStack { VMeHost(model: model) }
                .tabItem { Label("我", systemImage: "person.crop.circle.fill").accessibilityIdentifier(VTestIds.navMe) }
                .tag(VScreen.me)
                .accessibilityIdentifier(VTestIds.navMe)
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
                        .accessibilityIdentifier(VTestIds.navNow)
                }
                Section(VCopy.navInfrastructure) {
                    Button { model.screen = .overview } label: { Label(VCopy.navInfraOverview, systemImage: "globe.asia.australia.fill") }
                        .accessibilityIdentifier(VTestIds.navInfrastructure)
                    Button { model.screen = .cards } label: { Label(VCopy.navInfraCards, systemImage: "creditcard.fill") }
                    Button { model.screen = .numbers } label: { Label(VCopy.navInfraNumbers, systemImage: "phone.fill") }
                    Button { model.screen = .accounts } label: { Label(VCopy.navInfraAccounts, systemImage: "person.crop.circle") }
                    Button { model.screen = .emails } label: { Label(VCopy.navInfraEmails, systemImage: "envelope.fill") }
                    Button { model.screen = .devices } label: { Label(VCopy.navInfraDevices, systemImage: "iphone") }
                    Button { model.screen = .services } label: { Label(VCopy.navInfraServices, systemImage: "square.grid.2x2.fill") }
                    Button { model.screen = .weaknesses } label: { Label(VCopy.navInfraWeaknesses, systemImage: "exclamationmark.triangle.fill") }
                }
                Section(VCopy.navChange) {
                    Button { model.screen = .changePhone } label: { Label(VCopy.quickChangePhone, systemImage: "arrow.triangle.2.circlepath") }
                        .accessibilityIdentifier(VTestIds.navChange)
                }
                Section(VCopy.navRecords) {
                    Button { model.screen = .records } label: { Label(VCopy.navRecords, systemImage: "clock") }
                        .accessibilityIdentifier(VTestIds.navRecords)
                }
                Section("我") {
                    Button { model.screen = .me } label: { Label("我的数字生活", systemImage: "person.crop.circle.fill") }
                        .accessibilityIdentifier(VTestIds.navMe)
                    Button { model.screen = .personalization } label: { Label("个性化", systemImage: "slider.horizontal.3") }
                    Button { model.screen = .sources } label: { Label(VCopy.navSources, systemImage: "tray.full.fill") }
                }
            }
            .listStyle(.sidebar)
            .navigationTitle("PDIG vNext")
        } detail: {
            VNextRootContent(model: model)
        }
        .accessibilityIdentifier("pdig.ios.vnext.split")
    }
}

/// iPad detail root: five primary workspaces stay first-class just like phone tabs.
struct VNextRootContent: View {
    @ObservedObject var model: VNextModel

    var body: some View {
        switch model.screen.primaryTab {
        case .now:
            NowView(model: model)
        case .infrastructure:
            VInfrastructureHost(model: model)
        case .change:
            ChangePhoneView(model: model)
        case .records:
            VRecordsView(privacyMask: model.privacyMask)
        case .me:
            VMeHost(model: model)
        default:
            NowView(model: model)
        }
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
            case .accounts:
                VSecondaryCollectionView(model: model, kind: .account)
            case .emails:
                VSecondaryCollectionView(model: model, kind: .email)
            case .devices:
                VSecondaryCollectionView(model: model, kind: .device)
            case .services:
                VSecondaryCollectionView(model: model, kind: .service)
            case .secondaryDetail(let kind, let id):
                VSecondaryDetailView(model: model, kind: kind, id: id)
            case .weaknesses:
                VWeaknessesView(privacyMask: model.privacyMask)
            default: OverviewView(model: model)
            }
        }
    }
}

/// 「我」一级工作区宿主。设置/来源仍是 reference focused routes，
/// 但不再挤占第五一级导航位置。
struct VMeHost: View {
    @ObservedObject var model: VNextModel

    var body: some View {
        Group {
            switch model.screen {
            case .personalization:
                PersonalizationView(model: model)
            case .sources:
                VPlaceholderScreen(screen: .sources)
            case .settings:
                VPlaceholderScreen(screen: .settings)
            default:
                VMeView(model: model)
            }
        }
    }
}

/// Reference-only focused placeholder for sources/settings.
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
        case .me: return "我"
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
