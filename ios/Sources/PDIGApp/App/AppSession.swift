// AppSession —— 应用会话（内存状态 + 导航模型）。
//
// 根分流（task #1）：数据文件已打开（repository 就绪）且未锁定 → Home；
// 未配置解锁 → Onboarding；已配置但锁定 → Lock/Unlock 门。
// 会话状态只存活于内存；图数据由 GraphRepository 持久化。

import Combine
import Foundation
import PDIGCore

/// 路由（UI 用 switch 渲染；纯值类型便于测试）。
public enum AppRoute: Equatable, Sendable {
    case home
    case findings
    case finding(String)
    case infrastructure
    case node(String)
    case scenarioCenter
    case scenario(String)          // scenarioId
    case plan(String)              // planId
    case verification
    case timeline
    case backup
    case importFlow
    case drift(String)
    case settings
    case onboarding
    case lock
}

public enum SessionPhase: Equatable {
    case booting
    case onboarding
    case locked
    case ready
}

/// 会话（ObservableObject，UI 绑定；纯逻辑在静态函数里便于单测）。
public final class AppSession: ObservableObject {

    @Published public var phase: SessionPhase = .booting
    @Published public var route: AppRoute = .home
    @Published public var navigationStack: [AppRoute] = []
    @Published public var snapshot: GraphSnapshot = GraphSnapshot()
    @Published public var plans: [ChangePlan] = []
    @Published public var importState: ImportState = ImportState()
    @Published public var lastError: String?

    public private(set) var repository: GraphRepository?

    public init() {}

    // MARK: - boot

    /// 启动：打开数据库、迁移、加载快照；决定 Onboarding / Lock 分流。
    public func boot(dataDirectory: URL) {
        do {
            let driver = try SqliteProvider.openDatabase(atDirectory: dataDirectory)
            let repo = try GraphRepository(driver: driver)
            repository = repo
            snapshot = try repo.loadSnapshot()
            plans = try repo.loadPlans()
            if !KeychainStore.hasUnlockMaterial() {
                phase = .onboarding
            } else {
                phase = .locked
            }
        } catch let e {
            phase = .locked
            lastError = "\(e)"
        }
    }

    public func refresh() {
        guard let repo = repository else { return }
        snapshot = (try? repo.loadSnapshot()) ?? snapshot
        plans = (try? repo.loadPlans()) ?? plans
    }

    // MARK: - onboarding / lock

    /// 完成 Onboarding：写入解锁盐（随机），进入 Lock 门。
    public func finishOnboarding() {
        let salt = DepmapContainer.toHex(DepmapContainer.randomBytes(16))
        KeychainStore.set(salt, key: "unlock.salt")
        phase = .locked
    }

    public func unlockSucceeded() {
        phase = .ready
        route = .home
        navigationStack = []
    }

    public func lockNow() {
        phase = .locked
        navigationStack = []
        route = .lock
    }

    public func deleteAllData() throws {
        guard let repo = repository else { return }
        try BackupViewModel.deleteAllData(repository: repo)
        snapshot = GraphSnapshot()
        plans = []
    }

    // MARK: - navigation

    public func go(_ r: AppRoute) {
        route = r
        navigationStack.append(r)
    }

    public func push(_ r: AppRoute) {
        navigationStack.append(r)
        route = r
    }

    public func pop() {
        guard !navigationStack.isEmpty else {
            route = .home
            return
        }
        navigationStack.removeLast()
        route = navigationStack.last ?? .home
    }

    public func root() {
        route = .home
        navigationStack = []
    }

    // MARK: - scenario

    /// 创建计划并持久化，跳转计划页。
    public func startScenario(scenarioId: String, targetNodeId: String, effectiveDate: String? = nil) throws -> ChangePlan {
        guard let repo = repository else { throw ScenarioFlowError.noExecutableTarget(targetNodeId) }
        let scenarioPlan = try ScenarioFlow.createPlan(
            scenarioId: scenarioId,
            targetNodeId: targetNodeId,
            graph: snapshot,
            effectiveDate: effectiveDate
        )
        try repo.savePlan(scenarioPlan.plan)
        plans = try repo.loadPlans()
        return scenarioPlan.plan
    }

    /// 持久化计划更新（动作完成/验证状态）。
    public func updatePlan(_ plan: ChangePlan) throws {
        guard let repo = repository else { return }
        try repo.savePlan(plan)
        plans = try repo.loadPlans()
    }

    // MARK: - import

    public func setImportState(_ state: ImportState) {
        importState = state
    }
}
