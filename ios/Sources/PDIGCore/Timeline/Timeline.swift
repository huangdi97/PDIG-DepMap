// Timeline / Upcoming 投影 —— Swift 移植。
//
// 源头：harmony/entry/.../domain/Timeline.ets 的**纯投影**版（buildTimelinePure），
// 规则本身照录 core/src/services/timeline.ts 的 buildTimeline() 五条规则；
// 时间解析用 Android 的 parseIsoEpoch（Android 为 frozen 仲裁端）。
//
// **纯投影（derived read model）**：不是 Reality、不持久化、可随时重建。
// 只包含数字基础设施事项；排序 deterministic（bucket → priority 降序 →
// scheduledAt → id）。
//
// 与 TS 基准一致的两处关键方向（2026-09-18 由 fixture 反推修正，见 ArkTS 侧注释）：
//   1. priority 是**降序**（数值越大越靠前）
//   2. scheduledAt 为 null 时按空串参与比较，因此排在同时段有时间的项**之前**

import Foundation

public enum Timeline {

    public static let dayMs: Double = 86_400_000.0

    /// bucket 的确定性次序。排序与 UI 分组都以此为准，不得各自定义。
    public static let bucketOrder: [String] = [
        TimelineBucket.attention.rawValue,
        TimelineBucket.overdue.rawValue,
        TimelineBucket.today.rawValue,
        TimelineBucket.v7d.rawValue,
        TimelineBucket.v30d.rawValue,
        TimelineBucket.v90d.rawValue,
        TimelineBucket.later.rawValue,
    ]

    /// 迁移时植入的 legacy 来源实例 id（三端同名常量）。
    /// 它**永远**是 stale 来源（从未导入过数据），因此每个 DB 至少产生一条
    /// freshness_review —— 这是契约行为，不是缺陷。
    public static let legacyWechatSourceInstanceId = "legacy-wechat-statement"

    // -----------------------------------------------------------------------
    // 时间解析
    // -----------------------------------------------------------------------

    /// ISO-8601 → epoch millis。支持 `Z` / `±HH:MM` / 无偏移（按 UTC 处理）。
    ///
    /// 与 `PlanRules.parseIso` 的区别：**必须正确处理时区偏移** ——
    /// 微信账单是 +08:00、OFX 是 +00:00，忽略偏移会把"今天"判成"明天"。
    public static func parseIsoEpoch(_ value: String) -> Int64? {
        let v = kotlinTrim(value)
        var datePart = v
        if let i = v.firstIndex(of: "T") { datePart = String(v[v.startIndex..<i]) }
        else if let i = v.firstIndex(of: " ") { datePart = String(v[v.startIndex..<i]) }
        let dp = datePart.split(separator: "-", omittingEmptySubsequences: false).map(String.init)
        guard dp.count == 3 else { return nil }
        guard let y = Int(dp[0]), let mo = Int(dp[1]), let d = Int(dp[2]) else { return nil }
        guard mo >= 1 && mo <= 12 else { return nil }
        let dim = daysInMonth(y, mo)
        guard d >= 1 && d <= dim else { return nil }
        var epoch = daysFromCivil(y, mo, d) * dayMs
        if v.contains("T") {
            var rest = String(v[(v.firstIndex(of: "T")!)...].dropFirst())
            var offsetMinutes = 0.0
            if rest.hasSuffix("Z") {
                rest = String(rest.dropLast())
            } else if let plus = rest.firstIndex(of: "+") {
                let off = String(rest[rest.index(after: plus)...])
                rest = String(rest[..<plus])
                guard let m = parseOffsetMinutes(off) else { return nil }
                offsetMinutes = Double(m)
            } else if let minus = rest.lastIndex(of: "-") {
                let off = String(rest[rest.index(after: minus)...])
                rest = String(rest[..<minus])
                guard let m = parseOffsetMinutes(off) else { return nil }
                offsetMinutes = -Double(m)
            }
            let hms = rest.split(separator: ":", omittingEmptySubsequences: false).map(String.init)
            guard hms.count >= 2 else { return nil }
            guard let hh = Int(hms[0]), let mm = Int(hms[1]) else { return nil }
            let ss = hms.count > 2 ? (Double(hms[2]) ?? -1) : 0.0
            guard hh >= 0 && hh <= 23, mm >= 0 && mm <= 59, ss >= 0.0, ss < 60.0 else { return nil }
            epoch += (Double(hh) * 3_600_000.0 + Double(mm) * 60_000.0 + ss * 1000.0)
                - (offsetMinutes * 60_000.0)
        }
        return Int64(floor(epoch))
    }

    private static func parseOffsetMinutes(_ off: String) -> Int? {
        let parts = off.split(separator: ":", omittingEmptySubsequences: false).map(String.init)
        switch parts.count {
        case 2:
            guard let h = Int(parts[0]), let m = Int(parts[1]) else { return nil }
            guard h >= 0 && h <= 23, m >= 0 && m <= 59 else { return nil }
            return h * 60 + m
        case 1:
            guard off.count == 4 else { return nil }
            guard let h = Int(String(off.prefix(2))), let m = Int(String(off.suffix(2))) else { return nil }
            return h * 60 + m
        default:
            return nil
        }
    }

    /// Howard Hinnant civil-from-days 逆运算（不依赖平台时区/日历纠错）。
    private static func daysFromCivil(_ y: Int, _ m: Int, _ d: Int) -> Double {
        let yy = Double(m <= 2 ? y - 1 : y)
        let era = floor(yy / 400.0)
        let yoe = yy - era * 400.0
        let doy = floor((153.0 * Double(m + (m > 2 ? -3 : 9)) + 2.0) / 5.0) + Double(d) - 1.0
        let doe = yoe * 365.0 + floor(yoe / 4.0) - floor(yoe / 100.0) + doy
        return era * 146_097.0 + doe - 719_468.0
    }

    // -----------------------------------------------------------------------
    // 分桶
    // -----------------------------------------------------------------------

    /// 两个 fail-safe 分支都归入 `attention`（而非某个"未来"档）：
    /// scheduledAt 为 null，或不可解析。无法判定时往**更需要人工注意**的方向收。
    ///
    /// 边界一律用 UTC 日界：若改用本地时区，同一份数据在不同时区设备上得到不同
    /// bucket，跨端 conformance 就再也比不齐。
    public static func bucketOf(_ scheduledAt: String?, _ nowMs: Int64) -> String {
        guard let s = scheduledAt, !s.isEmpty else { return TimelineBucket.attention.rawValue }
        guard let t = parseIsoEpoch(s) else { return TimelineBucket.attention.rawValue }
        let todayStart = floor(Double(nowMs) / dayMs) * dayMs
        let td = Double(t)
        if td < todayStart { return TimelineBucket.overdue.rawValue }
        if td < todayStart + dayMs { return TimelineBucket.today.rawValue }
        if td < todayStart + 8 * dayMs { return TimelineBucket.v7d.rawValue }
        if td < todayStart + 31 * dayMs { return TimelineBucket.v30d.rawValue }
        if td < todayStart + 91 * dayMs { return TimelineBucket.v90d.rawValue }
        return TimelineBucket.later.rawValue
    }

    public static func bucketRank(_ bucket: String) -> Int {
        if let i = bucketOrder.firstIndex(of: bucket) { return i }
        // 未知 bucket 排到最后：不隐藏它，但也不让它插到已知项前面。
        return bucketOrder.count
    }

    // -----------------------------------------------------------------------
    // 输入 / 条目
    // -----------------------------------------------------------------------

    public struct VerificationInput: Equatable, Sendable {
        public let status: String
        public let method: String
        public init(status: String = "", method: String = "") {
            self.status = status
            self.method = method
        }
    }

    public struct ActionInput: Equatable, Sendable {
        public let id: String
        public let title: String
        public let phase: String
        public let verification: VerificationInput?
        public init(id: String = "", title: String = "", phase: String = "", verification: VerificationInput? = nil) {
            self.id = id
            self.title = title
            self.phase = phase
            self.verification = verification
        }
    }

    public struct PlanInput: Equatable, Sendable {
        public let id: String
        public let title: String
        public let scenario: String
        public let workflowState: String
        public let lastAnalyzedGraphRevision: Int
        public let effectiveDate: String?
        public let actions: [ActionInput]
        public init(
            id: String = "", title: String = "", scenario: String = "", workflowState: String = "",
            lastAnalyzedGraphRevision: Int = 0, effectiveDate: String? = nil, actions: [ActionInput] = []
        ) {
            self.id = id
            self.title = title
            self.scenario = scenario
            self.workflowState = workflowState
            self.lastAnalyzedGraphRevision = lastAnalyzedGraphRevision
            self.effectiveDate = effectiveDate
            self.actions = actions
        }
    }

    public struct DriftInput: Equatable, Sendable {
        public let id: String
        public let kind: String
        public let detectedAt: String
        public let status: String
        public let targetNodeId: String
        public init(id: String = "", kind: String = "", detectedAt: String = "", status: String = "", targetNodeId: String = "") {
            self.id = id
            self.kind = kind
            self.detectedAt = detectedAt
            self.status = status
            self.targetNodeId = targetNodeId
        }
    }

    public struct NodeInput: Equatable, Sendable {
        public let id: String
        public let name: String
        public let archived: Bool
        public let fields: [String: String]
        public init(id: String = "", name: String = "", archived: Bool = false, fields: [String: String] = [:]) {
            self.id = id
            self.name = name
            self.archived = archived
            self.fields = fields
        }
    }

    public struct SourceInput: Equatable, Sendable {
        public let id: String
        public let label: String
        public let state: String
        public let lastIngestedAt: String?
        public init(id: String = "", label: String = "", state: String = "", lastIngestedAt: String? = nil) {
            self.id = id
            self.label = label
            self.state = state
            self.lastIngestedAt = lastIngestedAt
        }
    }

    /// 投影的全部输入：一次读取的只读快照。
    public struct Input: Equatable, Sendable {
        public let currentGraphRevision: Int
        public let plans: [PlanInput]
        public let drifts: [DriftInput]
        public let nodes: [NodeInput]
        public let sources: [SourceInput]
        public init(
            currentGraphRevision: Int = 0,
            plans: [PlanInput] = [],
            drifts: [DriftInput] = [],
            nodes: [NodeInput] = [],
            sources: [SourceInput] = []
        ) {
            self.currentGraphRevision = currentGraphRevision
            self.plans = plans
            self.drifts = drifts
            self.nodes = nodes
            self.sources = sources
        }
    }

    public struct Item: Equatable, Sendable {
        public let id: String
        public let kind: String
        public let bucket: String
        /// 越大越靠前。
        public let priority: Int
        public let scheduledAt: String?
        public let sourceType: String
        public let sourceId: String
        public let status: String
        public let title: String
        public let subtitle: String
        public init(
            id: String = "", kind: String = TimelineItemKind.needsAttention.rawValue,
            bucket: String = TimelineBucket.attention.rawValue, priority: Int = 0,
            scheduledAt: String? = nil, sourceType: String = "", sourceId: String = "",
            status: String = "", title: String = "", subtitle: String = ""
        ) {
            self.id = id
            self.kind = kind
            self.bucket = bucket
            self.priority = priority
            self.scheduledAt = scheduledAt
            self.sourceType = sourceType
            self.sourceId = sourceId
            self.status = status
            self.title = title
            self.subtitle = subtitle
        }
    }

    // -----------------------------------------------------------------------
    // 排序
    // -----------------------------------------------------------------------

    /// deterministic 排序：bucket → priority 降序 → scheduledAt（null→''）→ id。
    ///
    /// 字符串比较一律用 **UTF-16 code unit 序**（Jcs.utf16Less）。Swift 的 `String`
    /// 比较走 Unicode 语义序，与 JS `<` / Java `compareTo` 不同，直接用会让三端
    /// 在同一份 fixture 上排出不同序列。
    public static func sort(_ items: [Item]) -> [Item] {
        items.sorted { a, b in
            let ra = bucketRank(a.bucket)
            let rb = bucketRank(b.bucket)
            if ra != rb { return ra < rb }
            if a.priority != b.priority { return a.priority > b.priority }   // 降序
            let sa = a.scheduledAt ?? ""
            let sb = b.scheduledAt ?? ""
            if sa != sb { return utf16Less(sa, sb) }
            return utf16Less(a.id, b.id)
        }
    }

    private static func utf16Less(_ a: String, _ b: String) -> Bool { Jcs.utf16Less(a, b) }

    // -----------------------------------------------------------------------
    // 纯投影
    // -----------------------------------------------------------------------

    /// 从 node.fields 中取 expiryDate。只有非空字符串才算有到期日，
    /// **不做**隐式转换：把 `""` 当成有效日期会造出凭空的时间线事项。
    public static func nodeExpiryDate(_ fields: [String: String]) -> String? {
        guard let raw = fields["expiryDate"], !raw.isEmpty else { return nil }
        return raw
    }

    /// 计划是否 stale（派生，不是存储状态）。与 PlanRules.isPlanStale 同口径。
    public static func planIsStale(_ plan: PlanInput, _ currentGraphRevision: Int) -> Bool {
        if plan.workflowState == ChangePlanWorkflowState.completed.rawValue ||
            plan.workflowState == ChangePlanWorkflowState.cancelled.rawValue { return false }
        return currentGraphRevision > plan.lastAnalyzedGraphRevision
    }

    /// 纯投影：按 buildTimeline() 的 5 条规则收集条目并排序。
    ///
    /// 规则次序与基准一致，因为 `continue` 的位置会影响同一计划是否**同时**
    /// 产出 needs_attention 与 upcoming_change（基准里是二选一）。
    public static func buildTimelinePure(
        _ input: Input,
        _ nowIso: String,
        _ freshnessThresholdDays: Int
    ) -> [Item] {
        guard let now = parseIsoEpoch(nowIso) else { return [] }
        var items: [Item] = []

        // 1. ChangePlan：stale → needs_attention（attention）；否则 effectiveDate → upcoming_change
        for plan in input.plans {
            if plan.workflowState == ChangePlanWorkflowState.completed.rawValue ||
                plan.workflowState == ChangePlanWorkflowState.cancelled.rawValue { continue }
            if planIsStale(plan, input.currentGraphRevision) {
                items.append(
                    Item(
                        id: "tl-plan-stale-\(plan.id)",
                        kind: TimelineItemKind.needsAttention.rawValue,
                        bucket: TimelineBucket.attention.rawValue,
                        priority: 3,
                        scheduledAt: nil,
                        sourceType: TimelineSourceType.changePlan.rawValue,
                        sourceId: plan.id,
                        status: "needs_revalidation",
                        title: "计划需要重新检查：\(plan.title)",
                        subtitle: "基础设施在此计划创建后发生了变化，需要重新分析。"
                    )
                )
                continue   // stale 的计划不再产出 upcoming_change（基准同此）
            }
            if let eff = plan.effectiveDate, !eff.isEmpty {
                let b = bucketOf(eff, now)
                items.append(
                    Item(
                        id: "tl-plan-\(plan.id)",
                        kind: TimelineItemKind.upcomingChange.rawValue,
                        bucket: (b == TimelineBucket.attention.rawValue) ? TimelineBucket.later.rawValue : b,
                        priority: 2,
                        scheduledAt: eff,
                        sourceType: TimelineSourceType.changePlan.rawValue,
                        sourceId: plan.id,
                        status: plan.workflowState,
                        title: plan.title,
                        subtitle: "变更计划（\(plan.scenario)）"
                    )
                )
            }

            // 2. 待验证动作（verify 阶段且 verification 未终结）
            for action in plan.actions {
                guard let v = action.verification else { continue }
                if action.phase != PlanActionPhase.verify.rawValue { continue }
                if v.status == ActionVerificationStatus.verified.rawValue ||
                    v.status == ActionVerificationStatus.notRequired.rawValue { continue }
                let b = bucketOf(plan.effectiveDate, now)
                items.append(
                    Item(
                        id: "tl-verif-\(plan.id)-\(action.id)",
                        kind: TimelineItemKind.verificationPending.rawValue,
                        bucket: (b == TimelineBucket.attention.rawValue) ? TimelineBucket.later.rawValue : b,
                        priority: 2,
                        scheduledAt: plan.effectiveDate,
                        sourceType: TimelineSourceType.actionVerification.rawValue,
                        sourceId: "\(plan.id)/\(action.id)",
                        status: v.status,
                        title: "待验证：\(action.title)",
                        subtitle: "属于计划「\(plan.title)」"
                    )
                )
            }
        }

        // 3. 开放 Drift → drift_review（attention，priority 3）
        for d in input.drifts {
            if d.status != DriftStatus.open.rawValue { continue }
            items.append(
                Item(
                    id: "tl-drift-\(d.id)",
                    kind: TimelineItemKind.driftReview.rawValue,
                    bucket: TimelineBucket.attention.rawValue,
                    priority: 3,
                    scheduledAt: d.detectedAt,
                    sourceType: TimelineSourceType.realityDrift.rawValue,
                    sourceId: d.id,
                    status: d.status,
                    title: "可能发生了变化",
                    subtitle: "\(d.kind)（需确认或忽略）"
                )
            )
        }

        // 4. 节点到期 → expiration（bucket 由到期日决定，priority 2）
        //    注意：这里**不**做 attention→later 的重映射（基准也没有）。
        for node in input.nodes {
            if node.archived { continue }
            guard let expiry = nodeExpiryDate(node.fields) else { continue }
            items.append(
                Item(
                    id: "tl-expiry-\(node.id)",
                    kind: TimelineItemKind.expiration.rawValue,
                    bucket: bucketOf(expiry, now),
                    priority: 2,
                    scheduledAt: expiry,
                    sourceType: TimelineSourceType.nodeExpiry.rawValue,
                    sourceId: node.id,
                    status: "scheduled",
                    title: "\(node.name) 即将到期",
                    subtitle: "检查仍依赖此对象的支付路径。"
                )
            )
        }

        // 5. 来源新鲜度 → freshness_review（attention，priority 1）
        for inst in input.sources {
            if inst.state != SourceInstanceState.active.rawValue { continue }
            let last = inst.lastIngestedAt
            var ageDays = Double.infinity
            if let l = last, !l.isEmpty {
                if let t = parseIsoEpoch(l) {
                    ageDays = (Double(now) - Double(t)) / dayMs
                } else {
                    ageDays = Double.infinity
                }
            }
            if ageDays <= Double(freshnessThresholdDays) { continue }
            let subtitle = (last != nil && !last!.isEmpty)
                ? "最近更新：\(String(last!.prefix(10)))"
                : "从未导入过数据。"
            items.append(
                Item(
                    id: "tl-fresh-\(inst.id)",
                    kind: TimelineItemKind.freshnessReview.rawValue,
                    bucket: TimelineBucket.attention.rawValue,
                    priority: 1,
                    scheduledAt: nil,
                    sourceType: TimelineSourceType.sourceFreshness.rawValue,
                    sourceId: inst.id,
                    status: "stale",
                    title: "数据来源需要刷新：\(inst.label)",
                    subtitle: subtitle
                )
            )
        }

        return sort(items)
    }
}
