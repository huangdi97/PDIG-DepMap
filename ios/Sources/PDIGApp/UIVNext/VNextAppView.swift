// VNextAppView —— iOS / iPadOS platform translation of the accepted PDIG mobile reference.
//
// iPhone: four stable top-level tabs; focused child flows own the NavigationStack.
// iPad: four top-level destinations remain the sidebar authority; Infrastructure's eight siblings stay
// inside the content context. Search / Data Sources / Personalization are utilities, not primary IA.
//
// This file is presentation-only. It does not change Canonical / PersonalReality / .depmap.

import SwiftUI
#if os(iOS)
import UIKit
#endif

enum VPrimaryDestination: String, CaseIterable, Hashable {
    case now, infrastructure, change, records
}

@MainActor
final class VNextModel: ObservableObject {
    @Published var screen: VScreen = .now
    @Published var primary: VPrimaryDestination = .now
    @Published var regionFilter: String? = nil
    @Published var globeDetailShown = false
    @Published var searchQuery = ""
    @Published var changeProjection: VChangeProjection = .current

    @Published var privacyMask: Bool {
        didSet { persistWorkspace() }
    }
    @Published var reduceMotion: Bool {
        didSet { persistWorkspace() }
    }
    @Published var showUpcoming: Bool {
        didSet { persistWorkspace() }
    }
    @Published var systemReduceMotion = false

    /// Screenshot/UI-test-only override. Never persisted and never enters .depmap.
    @Published var evidenceThemeId: String? = nil

    @Published private(set) var presentationProfiles: [String: VPresentationProfile]

    private var history: [VScreen] = []
    private let preferencesStore: VNextPreferencesStore?

    init(preferencesStore: VNextPreferencesStore? = .live) {
        self.preferencesStore = preferencesStore
        let workspace = preferencesStore?.loadWorkspace() ?? VWorkspacePreferences()
        self.privacyMask = workspace.privacyMask
        self.reduceMotion = workspace.reduceMotion
        self.showUpcoming = workspace.showUpcoming
        self.presentationProfiles = preferencesStore?.loadProfiles() ?? [:]
    }

    var effectiveReduceMotion: Bool { reduceMotion || systemReduceMotion }

    func selectPrimary(_ next: VPrimaryDestination) {
        history.removeAll()
        primary = next
        switch next {
        case .now: screen = .now
        case .infrastructure: screen = .infrastructure
        case .change: screen = .changePhone
        case .records: screen = .records
        }
    }

    func navigate(_ next: VScreen) {
        if screen == .search && next != .search {
            pushHistory(screen)
        } else if isUtility(next) || (next == .changePhone && screen != .change && screen != .changePhone) {
            pushHistory(screen)
        }

        screen = next
        switch next {
        case .now: primary = .now
        case .infrastructure, .overview, .cards, .cardDetail, .cardCustomization,
             .numbers, .numberDetail, .numberCustomization, .accounts, .emails,
             .devices, .services, .weaknesses:
            primary = .infrastructure
        case .change, .changePhone:
            primary = .change
        case .records:
            primary = .records
        case .search, .sources, .settings, .personalization:
            break
        }
    }

    func openCard(_ cardId: String) {
        pushHistory(screen)
        screen = .cardDetail(cardId)
        primary = .infrastructure
    }

    func openNumber(_ numberId: String) {
        pushHistory(screen)
        screen = .numberDetail(numberId)
        primary = .infrastructure
    }

    func openCardCustomization(_ cardId: String) {
        pushHistory(screen)
        screen = .cardCustomization(cardId)
        primary = .infrastructure
    }

    func openNumberCustomization(_ numberId: String) {
        pushHistory(screen)
        screen = .numberCustomization(numberId)
        primary = .infrastructure
    }

    func selectRegion(_ code: String) {
        regionFilter = code
        globeDetailShown = false
    }

    func clearRegion() {
        globeDetailShown = false
        regionFilter = nil
    }

    func openRegionDetail() { globeDetailShown = true }

    func backToGlobal() {
        if globeDetailShown { globeDetailShown = false } else { regionFilter = nil }
    }

    var canGoBack: Bool { globeDetailShown || !history.isEmpty }

    func back() {
        if globeDetailShown {
            globeDetailShown = false
            return
        }
        if let prior = history.popLast() {
            screen = prior
            syncPrimary(for: prior)
            return
        }
        switch screen {
        case .cardCustomization(let id): screen = .cardDetail(id)
        case .numberCustomization(let id): screen = .numberDetail(id)
        case .cardDetail: screen = .cards
        case .numberDetail: screen = .numbers
        case .search, .sources, .settings, .personalization:
            selectPrimary(primary)
        default:
            selectPrimary(primary)
        }
    }

    func presentationProfile(targetType: String, targetId: String, fallbackPreset: String) -> VPresentationProfile {
        let key = VNextPreferencesStore.profileKey(targetType: targetType, targetId: targetId)
        if var stored = presentationProfiles[key] {
            if let evidenceThemeId {
                stored = stored.replacingTheme(evidenceThemeId)
            }
            return stored
        }
        let fallback = VPresentationProfile.defaultFor(targetType: targetType, targetId: targetId, preset: evidenceThemeId ?? fallbackPreset)
        return fallback
    }

    func savePresentationProfile(_ profile: VPresentationProfile) {
        let key = VNextPreferencesStore.profileKey(targetType: profile.targetType, targetId: profile.targetId)
        presentationProfiles[key] = profile
        preferencesStore?.saveProfiles(presentationProfiles)
    }

    private func persistWorkspace() {
        preferencesStore?.saveWorkspace(VWorkspacePreferences(
            privacyMask: privacyMask,
            reduceMotion: reduceMotion,
            showUpcoming: showUpcoming
        ))
    }

    private func isUtility(_ screen: VScreen) -> Bool {
        switch screen {
        case .search, .sources, .settings, .personalization: return true
        default: return false
        }
    }

    private func syncPrimary(for screen: VScreen) {
        switch screen.primaryTab {
        case .now: primary = .now
        case .infrastructure: primary = .infrastructure
        case .change: primary = .change
        case .records: primary = .records
        default: break
        }
    }

    private func pushHistory(_ value: VScreen) {
        guard history.last != value else { return }
        history.append(value)
        if history.count > 16 { history.removeFirst(history.count - 16) }
    }
}

public struct VNextAppView: View {
    @StateObject private var model = VNextModel()
    @Environment(\.accessibilityReduceMotion) private var systemReduceMotion

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
        .onAppear { model.systemReduceMotion = systemReduceMotion }
        .onChange(of: systemReduceMotion) { model.systemReduceMotion = $0 }
    }
}

struct VNextTabShell: View {
    @ObservedObject var model: VNextModel

    private var selection: Binding<VPrimaryDestination> {
        Binding(get: { model.primary }, set: { model.selectPrimary($0) })
    }

    var body: some View {
        TabView(selection: selection) {
            NavigationStack { VRootHost(model: model, root: .now) }
                .tabItem { Label(VCopy.navNow, systemImage: "house.fill") }
                .tag(VPrimaryDestination.now)
                .accessibilityIdentifier(VTestIds.navNow)

            NavigationStack { VRootHost(model: model, root: .infrastructure) }
                .tabItem { Label(VCopy.navInfrastructure, systemImage: "globe.asia.australia.fill") }
                .tag(VPrimaryDestination.infrastructure)
                .accessibilityIdentifier(VTestIds.navInfrastructure)

            NavigationStack { VRootHost(model: model, root: .change) }
                .tabItem { Label(VCopy.navChange, systemImage: "arrow.triangle.2.circlepath") }
                .tag(VPrimaryDestination.change)
                .accessibilityIdentifier(VTestIds.navChange)

            NavigationStack { VRootHost(model: model, root: .records) }
                .tabItem { Label(VCopy.navRecords, systemImage: "clock") }
                .tag(VPrimaryDestination.records)
                .accessibilityIdentifier(VTestIds.navRecords)
        }
        .accessibilityIdentifier("pdig.nav.tab")
    }
}

struct VNextSplitShell: View {
    @ObservedObject var model: VNextModel
    @State private var columnVisibility: NavigationSplitViewVisibility = .all

    var body: some View {
        NavigationSplitView(columnVisibility: $columnVisibility) {
            List {
                Section("PDIG") {
                    VSidebarDestinationRow(
                        title: VCopy.navNow,
                        icon: "house.fill",
                        selected: model.primary == .now
                    ) { model.selectPrimary(.now) }

                    VSidebarDestinationRow(
                        title: VCopy.navInfrastructure,
                        icon: "globe.asia.australia.fill",
                        selected: model.primary == .infrastructure
                    ) { model.selectPrimary(.infrastructure) }

                    VSidebarDestinationRow(
                        title: VCopy.navChange,
                        icon: "arrow.triangle.2.circlepath",
                        selected: model.primary == .change
                    ) { model.selectPrimary(.change) }

                    VSidebarDestinationRow(
                        title: VCopy.navRecords,
                        icon: "clock",
                        selected: model.primary == .records
                    ) { model.selectPrimary(.records) }
                }
                Section("工具") {
                    Button { model.navigate(.sources) } label: {
                        Label(VCopy.navSources, systemImage: "externaldrive.fill")
                    }
                    Button { model.navigate(.personalization) } label: {
                        Label("个性化", systemImage: "slider.horizontal.3")
                    }
                }
            }
            .listStyle(.sidebar)
            .navigationTitle("PDIG")
            .navigationSplitViewColumnWidth(min: 190, ideal: 220, max: 260)
        } detail: {
            NavigationStack { VRootHost(model: model, root: model.primary) }
        }
        .navigationSplitViewStyle(.balanced)
        .onAppear {
            // iPad is a management workspace: keep the four primary destinations visible
            // instead of launching into an anonymous detail pane with a hidden sidebar.
            columnVisibility = .all
        }
    }
}

struct VSidebarDestinationRow: View {
    let title: String
    let icon: String
    let selected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: VSpace.md) {
                Image(systemName: icon)
                    .frame(width: 20)
                Text(title)
                Spacer()
            }
            .font(VFont.secondary())
            .fontWeight(selected ? .semibold : .regular)
            .foregroundColor(selected ? PdigV2Colors.primaryText : PdigV2Colors.textPrimary)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .listRowBackground(selected ? PdigV2Colors.primarySoft : Color.clear)
        .accessibilityLabel(title)
        .accessibilityIdentifier("pdig.nav.sidebar.\(title)")
    }
}

struct VRootHost: View {
    @ObservedObject var model: VNextModel
    let root: VPrimaryDestination

    var body: some View {
        Group {
            switch model.screen {
            case .search: SearchView(model: model)
            case .sources: DataSourcesView(model: model)
            case .settings, .personalization: PersonalizationView(model: model)
            default:
                rootContent
            }
        }
        .toolbar {
            ToolbarItemGroup(placement: .primaryAction) {
                Button { model.navigate(.search) } label: {
                    Image(systemName: "magnifyingglass")
                }
                .keyboardShortcut("k", modifiers: [.command])
                .accessibilityLabel("搜索")
                .accessibilityHint("Command K")

                Menu {
                    Button { model.navigate(.sources) } label: {
                        Label(VCopy.navSources, systemImage: "externaldrive")
                    }
                    Button { model.navigate(.personalization) } label: {
                        Label("个性化", systemImage: "slider.horizontal.3")
                    }
                } label: {
                    Image(systemName: "ellipsis.circle")
                }
                .accessibilityLabel("更多")
            }
        }
    }

    @ViewBuilder
    private var rootContent: some View {
        switch root {
        case .now:
            NowView(model: model)
        case .infrastructure:
            VInfrastructureHost(model: model)
        case .change:
            ChangePhoneView(model: model)
        case .records:
            RecordsView(model: model)
        }
    }
}

struct VInfrastructureHost: View {
    @ObservedObject var model: VNextModel
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass

    private var showSecondary: Bool {
        horizontalSizeClass == .regular && model.screen.isInfrastructureSibling
    }

    var body: some View {
        VStack(spacing: 0) {
            if showSecondary {
                VInfrastructureSecondaryBar(model: model)
                    .padding(.horizontal, VSpace.pagePadding)
                    .padding(.top, VSpace.sm)
            }

            Group {
                switch model.screen {
                case .infrastructure:
                    if horizontalSizeClass == .compact {
                        InfrastructureHubView(model: model)
                    } else {
                        OverviewView(model: model)
                    }
                case .overview: OverviewView(model: model)
                case .cards: CardsView(model: model)
                case .cardDetail: CardDetailView(model: model)
                case .cardCustomization: CardCustomizationView(model: model)
                case .numbers: NumbersView(model: model)
                case .numberDetail: NumberDetailView(model: model)
                case .numberCustomization: NumberCustomizationView(model: model)
                case .accounts: AccountsView(model: model)
                case .emails: EmailsView(model: model)
                case .devices: DevicesView(model: model)
                case .services: ServicesView(model: model)
                case .weaknesses: WeaknessesView(model: model)
                default: OverviewView(model: model)
                }
            }
        }
        .vPageBackground()
    }
}

struct VInfrastructureSecondaryBar: View {
    @ObservedObject var model: VNextModel

    private let items: [(String, String, VScreen)] = [
        (VCopy.navInfraOverview, "globe.asia.australia.fill", .overview),
        (VCopy.navInfraCards, "creditcard.fill", .cards),
        (VCopy.navInfraNumbers, "phone.fill", .numbers),
        (VCopy.navInfraAccounts, "person.crop.circle.fill", .accounts),
        (VCopy.navInfraEmails, "envelope.fill", .emails),
        (VCopy.navInfraDevices, "laptopcomputer.and.iphone", .devices),
        (VCopy.navInfraServices, "star.fill", .services),
        (VCopy.navInfraWeaknesses, "exclamationmark.triangle.fill", .weaknesses),
    ]

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: VSpace.sm) {
                ForEach(Array(items.enumerated()), id: \.offset) { _, item in
                    let selected = model.screen.infrastructureFamily == item.2.infrastructureFamily
                    Button {
                        model.navigate(item.2)
                    } label: {
                        Label(item.0, systemImage: item.1)
                            .font(VFont.meta()).fontWeight(selected ? .semibold : .regular)
                            .foregroundColor(selected ? PdigV2Colors.primaryText : PdigV2Colors.textSecondary)
                            .padding(.horizontal, VSpace.md)
                            .frame(minHeight: 44)
                    }
                    .buttonStyle(.plain)
                    .background(selected ? PdigV2Colors.primarySoft : PdigV2Colors.surface)
                    .clipShape(RoundedRectangle(cornerRadius: VRadius.md, style: .continuous))
                    .overlay(RoundedRectangle(cornerRadius: VRadius.md, style: .continuous)
                        .stroke(selected ? PdigV2Colors.primary.opacity(0.34) : PdigV2Colors.borderSubtle, lineWidth: 1))
                    .accessibilityIdentifier("pdig.nav.infra.\(item.2.infrastructureFamily ?? "unknown")")
                }
            }
        }
        .accessibilityIdentifier("pdig.nav.infra.secondary")
    }
}

struct VBackButton: View {
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: "chevron.left")
                .font(.system(size: 15, weight: .semibold))
                .foregroundColor(PdigV2Colors.primaryText)
                .frame(width: 44, height: 44)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel("返回")
    }
}

struct VPageBackground: ViewModifier {
    func body(content: Content) -> some View {
        content.background(
            LinearGradient(colors: [PdigV2Colors.canvas, PdigV2Colors.canvasDeep],
                           startPoint: .top, endPoint: .bottom)
                .ignoresSafeArea()
        )
    }
}

extension View {
    func vPageBackground() -> some View { modifier(VPageBackground()) }
}
