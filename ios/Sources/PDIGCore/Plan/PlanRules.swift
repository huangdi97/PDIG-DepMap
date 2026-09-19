// PlanReadiness / ScenarioCoverage —— 纯确定性规则引擎（Swift 移植）。
//
// 源头：android/core/.../plan/Rules.kt。
//
// 结构性保证（不是运行时检查）：
// 输入类型 PlanReadinessInput / ScenarioCoverageInput **根本没有**
// confidence 与 absence 字段。因此"confidence 不能绕过 review"、
// "absence 不能提高 readiness"在类型系统层面就不可表达。
//
// 禁止（spec/README.md §3）：
//  - safe / fully_safe / 100% / all clear
//  - 「目标数 − 完成动作数」的减法近似

import Foundation

public enum PlanRules {
    private static let dayMs: Double = 86_400_000.0

    public static func isPlanStale(plan: ChangePlan, currentGraphRevision: Int) -> Bool {
        plan.workflowState != .completed &&
            plan.workflowState != .cancelled &&
            currentGraphRevision > plan.lastAnalyzedGraphRevision
    }

    public static func computePlanReadiness(_ input: PlanReadinessInput) -> PlanReadiness {
        if input.pendingMustChange > 0 { return .blocked }
        if isPlanStale(plan: input.plan, currentGraphRevision: input.currentGraphRevision) {
            return .reviewRequired
        }
        if input.pendingNeedsReview > 0 { return .reviewRequired }
        if input.unresolvedCandidates > 0 { return .reviewRequired }
        if input.pendingRelevantProposals > 0 { return .reviewRequired }
        if input.staleRelevantDependencies > 0 { return .reviewRequired }
        if input.unfinishedChangeActions > 0 { return .reviewRequired }
        return .readyWithKnownScope
    }

    /// must_change requirement 的显式 resolution 判定（纯函数）。
    /// - 未被任何 change 动作声明 → unresolved
    /// - 被声明但存在未完成的声明动作 → unresolved
    /// - 声明它的全部 change 动作都 done → resolved
    public static func countUnresolvedMustChange(
        actions: [PlanAction],
        keys: [String]
    ) -> Int {
        var unresolved = 0
        for key in keys {
            let claimants = actions.filter {
                $0.phase == .change && $0.resolvesImpactKeys.contains(key)
            }
            if claimants.isEmpty || claimants.contains(where: { !$0.done }) {
                unresolved += 1
            }
        }
        return unresolved
    }

    // -----------------------------------------------------------------------
    // ScenarioCoverage（信息覆盖描述，不是安全评分）
    // -----------------------------------------------------------------------

    private static func daysBetween(fromIso: String?, toIso: String) -> Double {
        guard let fromIso = fromIso, let from = parseIso(fromIso), let to = parseIso(toIso) else {
            return Double.infinity
        }
        return (Double(to) - Double(from)) / dayMs
    }

    /// 严格 ISO-8601 解析；解析失败返回 nil（绝不依赖 Date 自动纠错，spec §148）。
    public static func parseIso(_ value: String) -> Int64? {
        let datePart = value.components(separatedBy: "T")[0].components(separatedBy: " ")[0]
        let parts = datePart.components(separatedBy: "-")
        guard parts.count == 3,
              let y = Int(parts[0]), let m = Int(parts[1]), let d = Int(parts[2]),
              m >= 1, m <= 12, d >= 1, d <= 31,
              d <= daysInMonth(year: y, month: m) else { return nil }

        var epoch = daysFromCivil(y: y, m: m, d: d)
        if value.contains("T") {
            var timePart = value.components(separatedBy: "T")[1]
            if timePart.hasSuffix("Z") { timePart.removeLast() }
            let hms = timePart.components(separatedBy: ":")
            guard hms.count >= 2,
                  let hh = Int(hms[0]), let mm = Int(hms[1]),
                  hh >= 0, hh <= 23, mm >= 0, mm <= 59 else { return nil }
            var ss = 0.0
            if hms.count > 2 {
                guard let ssv = Double(hms[2]), ssv >= 0.0, ssv < 60.0 else { return nil }
                ss = ssv
            }
            epoch = epoch * 86_400_000.0 + (Double(hh) * 3_600_000.0 + Double(mm) * 60_000.0 + ss * 1000.0)
            return Int64(epoch.rounded(.towardZero))
        }
        return Int64((epoch * 86_400_000.0).rounded(.towardZero))
    }

    private static func isLeap(_ y: Int) -> Bool { (y % 4 == 0 && y % 100 != 0) || y % 400 == 0 }

    private static func daysInMonth(year y: Int, month m: Int) -> Int {
        switch m {
        case 2: return isLeap(y) ? 29 : 28
        case 4, 6, 9, 11: return 30
        default: return 31
        }
    }

    /// Howard Hinnant 的 civil-from-days 逆运算（避免依赖平台时区/日历纠错）。
    private static func daysFromCivil(y: Int, m: Int, d: Int) -> Double {
        let yy = Double(m <= 2 ? y - 1 : y)
        let era = floor(yy / 400.0)
        let yoe = yy - era * 400.0
        let doy = floor((153.0 * Double(m + (m > 2 ? -3 : 9)) + 2.0) / 5.0) + Double(d) - 1.0
        let doe = yoe * 365.0 + floor(yoe / 4.0) - floor(yoe / 100.0) + doy
        return era * 146_097.0 + doe - 719_468.0
    }

    public static func computeScenarioCoverage(_ input: ScenarioCoverageInput) -> ScenarioCoverage {
        var explanations: [String] = []
        let counts = CoverageCounts(
            confirmedDirectDependencies: input.confirmedDirectDependencies,
            confirmedIndirectDependencies: input.confirmedIndirectDependencies,
            pendingProposals: input.pendingProposals,
            unresolvedCandidates: input.unresolvedCandidates,
            staleDependencies: input.staleDependencies,
            unknownCriticalityCount: input.unknownCriticalityCount,
            unverifiedActions: input.unverifiedActions
        )

        let threshold = Double(input.freshnessThresholdDays)
        let freshSources = input.sources.filter { daysBetween(fromIso: $0.lastIngestedAt, toIso: input.now) <= threshold }
        let staleSources = input.sources.filter { daysBetween(fromIso: $0.lastIngestedAt, toIso: input.now) > threshold }

        if input.sources.isEmpty && input.confirmedDirectDependencies == 0 {
            explanations.append("没有导入过任何相关来源，也没有已确认的直接依赖。")
            return ScenarioCoverage(scenarioId: input.scenarioId, coverageLevel: .unknown,
                                    explanations: explanations, counts: counts)
        }

        if !input.sources.isEmpty && freshSources.isEmpty {
            explanations.append(
                "已有 \(staleSources.count) 个来源，但都超过新鲜度阈值（\(input.freshnessThresholdDays) 天）未刷新。")
        }
        if input.confirmedDirectDependencies == 0 {
            explanations.append("尚无已确认的直接依赖（目标对象没有 confirmed 支付关系）。")
        }
        if !explanations.isEmpty {
            return ScenarioCoverage(scenarioId: input.scenarioId, coverageLevel: .limited,
                                    explanations: explanations, counts: counts)
        }

        var partial = false
        if input.pendingProposals > 0 {
            explanations.append("\(input.pendingProposals) 个相关 Proposal 待确认（未确认不当作事实）。")
            partial = true
        }
        if input.unresolvedCandidates > 0 {
            explanations.append("\(input.unresolvedCandidates) 个发现对象未解析（不进入依赖图）。")
            partial = true
        }
        if input.staleDependencies > 0 {
            explanations.append("\(input.staleDependencies) 条相关依赖长期未验证。")
            partial = true
        }
        if input.unknownCriticalityCount > 0 {
            explanations.append("\(input.unknownCriticalityCount) 条依赖 criticality=unknown（是否必需未确认）。")
            partial = true
        }
        if input.unverifiedActions > 0 {
            explanations.append("\(input.unverifiedActions) 个动作已完成但尚未验证。")
            partial = true
        }

        if partial {
            return ScenarioCoverage(scenarioId: input.scenarioId, coverageLevel: .partial,
                                    explanations: explanations, counts: counts)
        }

        explanations.append(
            "\(freshSources.count) 个来源在新鲜度阈值内；\(input.confirmedDirectDependencies) 条直接依赖已确认，无未决 Proposal / 候选 / unknown criticality。")
        return ScenarioCoverage(scenarioId: input.scenarioId, coverageLevel: .wellEvidenced,
                                explanations: explanations, counts: counts)
    }
}
