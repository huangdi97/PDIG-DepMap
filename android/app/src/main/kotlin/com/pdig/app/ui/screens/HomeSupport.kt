package com.pdig.app.ui.screens

import com.pdig.app.data.PlanRow
import com.pdig.core.timeline.TimelineItem

/** Home 页一次 IO 取回的聚合数据（E-10：Attention 聚合）。 */
internal data class HomeCounts(
    val timeline: List<TimelineItem>,
    val plans: List<PlanRow>,
    val nodeCount: Int,
    val dependencies: Int,
    val proposals: Int,
    val candidates: Int,
    val drifts: Int,
)

/**
 * 需要用户处理的计划状态（PlanRow.workflowState 的 wire 值）。
 *
 * 实际写入该列的枚举是 ChangePlanWorkflowState（review_required / verifying 会真实出现）；
 * blocked / needs_revalidation 保留在集合里，覆盖未来迁移表可能写入的值（见 core.statemachine）。
 * 语义：这些状态都表示计划还等着用户去操作，不能放进"全部完成"。
 */
internal val PLAN_NEEDS_HANDLING_STATES: Set<String> = setOf(
    "blocked",
    "review_required",
    "needs_revalidation",
    "verifying",
)

/** Home "需要你处理" 聚合里的计划计数（goal §16/§17）。 */
internal fun plansNeedingHandling(plans: List<PlanRow>): Int =
    plans.count { it.workflowState in PLAN_NEEDS_HANDLING_STATES }
