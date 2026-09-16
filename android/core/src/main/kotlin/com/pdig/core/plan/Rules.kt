package com.pdig.core.plan

import com.pdig.core.domain.ChangePlan
import com.pdig.core.domain.CoverageCounts
import com.pdig.core.domain.CoverageSourceInfo
import com.pdig.core.domain.PlanAction
import com.pdig.core.domain.PlanReadinessInput
import com.pdig.core.domain.ScenarioCoverage
import com.pdig.core.domain.ScenarioCoverageInput
import com.pdig.core.generated.ChangePlanWorkflowState
import com.pdig.core.generated.CoverageLevel
import com.pdig.core.generated.PlanReadiness

/**
 * PlanReadiness / ScenarioCoverage —— 纯确定性规则引擎。
 *
 * 结构性保证（不是运行时检查）：
 * 输入类型 PlanReadinessInput / ScenarioCoverageInput **根本没有**
 * confidence 与 absence 字段。因此"confidence 不能绕过 review"、
 * "absence 不能提高 readiness"在类型系统层面就不可表达。
 *
 * 禁止（spec/README.md §3）：
 *  - safe / fully_safe / 100% / all clear
 *  - 「目标数 − 完成动作数」的减法近似
 */

fun isPlanStale(plan: ChangePlan, currentGraphRevision: Int): Boolean =
    plan.workflowState != ChangePlanWorkflowState.COMPLETED &&
        plan.workflowState != ChangePlanWorkflowState.CANCELLED &&
        currentGraphRevision > plan.lastAnalyzedGraphRevision

fun computePlanReadiness(input: PlanReadinessInput): PlanReadiness {
    if (input.pendingMustChange > 0) return PlanReadiness.BLOCKED
    if (isPlanStale(input.plan, input.currentGraphRevision)) return PlanReadiness.REVIEW_REQUIRED
    if (input.pendingNeedsReview > 0) return PlanReadiness.REVIEW_REQUIRED
    if (input.unresolvedCandidates > 0) return PlanReadiness.REVIEW_REQUIRED
    if (input.pendingRelevantProposals > 0) return PlanReadiness.REVIEW_REQUIRED
    if (input.staleRelevantDependencies > 0) return PlanReadiness.REVIEW_REQUIRED
    if (input.unfinishedChangeActions > 0) return PlanReadiness.REVIEW_REQUIRED
    return PlanReadiness.READY_WITH_KNOWN_SCOPE
}

/**
 * must_change requirement 的显式 resolution 判定（纯函数）。
 * - 未被任何 change 动作声明 → unresolved
 * - 被声明但存在未完成的声明动作 → unresolved
 * - 声明它的全部 change 动作都 done → resolved
 */
fun countUnresolvedMustChange(actions: List<PlanAction>, keys: List<String>): Int {
    var unresolved = 0
    for (key in keys) {
        val claimants = actions.filter {
            it.phase == com.pdig.core.generated.PlanActionPhase.CHANGE &&
                (it.resolvesImpactKeys).contains(key)
        }
        if (claimants.isEmpty() || claimants.any { !it.done }) unresolved += 1
    }
    return unresolved
}

// ---------------------------------------------------------------------------
// ScenarioCoverage（信息覆盖描述，不是安全评分）
// ---------------------------------------------------------------------------

private const val DAY_MS = 86_400_000.0

private fun daysBetween(fromIso: String?, toIso: String): Double {
    if (fromIso == null) return Double.POSITIVE_INFINITY
    val from = parseIso(fromIso) ?: return Double.POSITIVE_INFINITY
    val to = parseIso(toIso) ?: return Double.POSITIVE_INFINITY
    return (to - from) / DAY_MS
}

/** 严格 ISO-8601 解析；解析失败返回 null（绝不依赖 Date 自动纠错，spec §148）。 */
fun parseIso(value: String): Long? {
    // 支持 "YYYY-MM-DDTHH:mm:ss(.sss)?Z" 与 "YYYY-MM-DD"
    return try {
        val datePart = value.substringBefore('T').substringBefore(' ')
        val (y, m, d) = datePart.split('-').let {
            require(it.size == 3) { "bad date: $value" }
            Triple(it[0].toInt(), it[1].toInt(), it[2].toInt())
        }
        require(m in 1..12 && d in 1..31) { "bad date: $value" }
        // 严格日历校验：闰年/月末不做自动纠正
        val dim = daysInMonth(y, m)
        require(d <= dim) { "bad date: $value" }
        var epoch = daysFromCivil(y, m, d)
        if (value.contains('T')) {
            val timePart = value.substringAfter('T').removeSuffix("Z")
            val hms = timePart.split(':')
            require(hms.size >= 2) { "bad time: $value" }
            val hh = hms[0].toInt()
            val mm = hms[1].toInt()
            val ss = if (hms.size > 2) hms[2].toDouble() else 0.0
            require(hh in 0..23 && mm in 0..59 && ss < 60.0) { "bad time: $value" }
            epoch = (epoch * 86_400_000.0) + (hh * 3_600_000.0 + mm * 60_000.0 + ss * 1000.0)
            return epoch.toLong()
        }
        (epoch * 86_400_000.0).toLong()
    } catch (_: Throwable) {
        null
    }
}

private fun isLeap(y: Int): Boolean = (y % 4 == 0 && y % 100 != 0) || y % 400 == 0

private fun daysInMonth(y: Int, m: Int): Int = when (m) {
    2 -> if (isLeap(y)) 29 else 28
    4, 6, 9, 11 -> 30
    else -> 31
}

/** Howard Hinnant 的 civil-from-days 逆运算（避免依赖平台时区/日历纠错）。 */
private fun daysFromCivil(y: Int, m: Int, d: Int): Double {
    val yy = (if (m <= 2) y - 1 else y).toDouble()
    val era = kotlin.math.floor(yy / 400.0)
    val yoe = yy - era * 400.0
    val doy = kotlin.math.floor((153.0 * (m + if (m > 2) -3 else 9) + 2.0) / 5.0) + d - 1
    val doe = yoe * 365.0 + kotlin.math.floor(yoe / 4.0) - kotlin.math.floor(yoe / 100.0) + doy
    return era * 146097.0 + doe - 719468.0
}

fun computeScenarioCoverage(input: ScenarioCoverageInput): ScenarioCoverage {
    val explanations = mutableListOf<String>()
    val counts = CoverageCounts(
        confirmedDirectDependencies = input.confirmedDirectDependencies,
        confirmedIndirectDependencies = input.confirmedIndirectDependencies,
        pendingProposals = input.pendingProposals,
        unresolvedCandidates = input.unresolvedCandidates,
        staleDependencies = input.staleDependencies,
        unknownCriticalityCount = input.unknownCriticalityCount,
        unverifiedActions = input.unverifiedActions,
    )

    val freshSources = input.sources.filter {
        daysBetween(it.lastIngestedAt, input.now) <= input.freshnessThresholdDays
    }
    val staleSources = input.sources.filter {
        daysBetween(it.lastIngestedAt, input.now) > input.freshnessThresholdDays
    }

    if (input.sources.isEmpty() && input.confirmedDirectDependencies == 0) {
        explanations.add("没有导入过任何相关来源，也没有已确认的直接依赖。")
        return ScenarioCoverage(input.scenarioId, CoverageLevel.UNKNOWN, explanations, counts)
    }

    if (input.sources.isNotEmpty() && freshSources.isEmpty()) {
        explanations.add(
            "已有 ${staleSources.size} 个来源，但都超过新鲜度阈值（${input.freshnessThresholdDays} 天）未刷新。",
        )
    }
    if (input.confirmedDirectDependencies == 0) {
        explanations.add("尚无已确认的直接依赖（目标对象没有 confirmed 支付关系）。")
    }
    if (explanations.isNotEmpty()) {
        return ScenarioCoverage(input.scenarioId, CoverageLevel.LIMITED, explanations, counts)
    }

    var partial = false
    if (input.pendingProposals > 0) {
        explanations.add("${input.pendingProposals} 个相关 Proposal 待确认（未确认不当作事实）。")
        partial = true
    }
    if (input.unresolvedCandidates > 0) {
        explanations.add("${input.unresolvedCandidates} 个发现对象未解析（不进入依赖图）。")
        partial = true
    }
    if (input.staleDependencies > 0) {
        explanations.add("${input.staleDependencies} 条相关依赖长期未验证。")
        partial = true
    }
    if (input.unknownCriticalityCount > 0) {
        explanations.add(
            "${input.unknownCriticalityCount} 条依赖 criticality=unknown（是否必需未确认）。",
        )
        partial = true
    }
    if (input.unverifiedActions > 0) {
        explanations.add("${input.unverifiedActions} 个动作已完成但尚未验证。")
        partial = true
    }

    if (partial) {
        return ScenarioCoverage(input.scenarioId, CoverageLevel.PARTIAL, explanations, counts)
    }

    explanations.add(
        "${freshSources.size} 个来源在新鲜度阈值内；${input.confirmedDirectDependencies} 条直接依赖已确认，无未决 Proposal / 候选 / unknown criticality。",
    )
    return ScenarioCoverage(input.scenarioId, CoverageLevel.WELL_EVIDENCED, explanations, counts)
}
