package com.pdig.uivnext.ui

/** Consumer wording for production wire values. Unknown values remain explicit. */
internal fun productionObjectKindLabel(kind: String): String = when (kind) {
    "payment_instrument" -> "支付工具"
    "identity_anchor" -> "身份对象"
    "account" -> "账户"
    "service" -> "服务"
    "membership" -> "会员 / 资格"
    "device" -> "设备"
    "custom" -> "自定义对象"
    else -> "未知类型（$kind）"
}

internal fun productionRelationLabel(relation: String): String = when (relation) {
    "funding_source" -> "资金来源"
    "merchant_agreement" -> "商户 / 扣款约定"
    "verifies" -> "验证"
    "recovers" -> "恢复"
    "bound_to" -> "绑定"
    "authenticates" -> "认证"
    "controls" -> "控制"
    else -> "未知关系（$relation）"
}

internal fun productionCapabilityLabel(capability: String): String = when (capability) {
    "payment" -> "支付"
    "access" -> "访问"
    "authentication" -> "认证"
    "recovery" -> "恢复"
    "communication" -> "通信"
    "identity" -> "身份"
    else -> "未知能力（$capability）"
}

internal fun productionCriticalityLabel(criticality: String): String = when (criticality) {
    "required" -> "已确认必需"
    "unknown" -> "重要性未知"
    else -> "重要性未知（$criticality）"
}

internal fun productionImpactStatusLabel(status: String): String = when (status) {
    "must_change" -> "必须处理"
    "needs_review" -> "需要核对"
    "degraded" -> "能力下降"
    "backup_path" -> "已确认备用路径"
    "unaffected" -> "当前确认范围未受影响"
    "unknown" -> "未知"
    else -> "未知状态（$status）"
}

internal fun productionWorkflowStateLabel(state: String): String = when (state) {
    "draft" -> "草稿"
    "analyzed" -> "已分析"
    "review_required" -> "需要复核"
    "ready" -> "可开始"
    "in_progress" -> "进行中"
    "verifying" -> "验证中"
    "completed" -> "已完成"
    "cancelled" -> "已取消"
    "needs_revalidation" -> "需要重新验证"
    else -> "未知状态（$state）"
}

internal fun productionReadinessLabel(readiness: String): String = when (readiness) {
    "blocked" -> "有阻断项"
    "review_required" -> "需要复核"
    "ready_with_known_scope" -> "已知范围可开始"
    else -> "未知准备状态（$readiness）"
}

internal fun productionActionPhaseLabel(phase: String): String = when (phase) {
    "prepare" -> "准备"
    "change" -> "变更"
    "verify" -> "验证"
    else -> "未知阶段（$phase）"
}

internal fun productionScenarioLabel(scenario: String): String = when (scenario) {
    "replace_payment_card" -> "更换支付卡"
    "replace_phone_number" -> "更换手机号"
    else -> "变更场景（$scenario）"
}

internal fun productionTimelineBucketLabel(bucket: String): String = when (bucket) {
    "attention" -> "需要关注"
    "overdue" -> "已逾期"
    "today" -> "今天"
    "7d" -> "7 天内"
    "30d" -> "30 天内"
    "90d" -> "90 天内"
    "later" -> "更晚"
    else -> "时间范围未知"
}

internal fun productionTimelineStatusLabel(status: String): String = when (status) {
    "needs_revalidation" -> "需要重新检查"
    "scheduled" -> "已安排"
    "open" -> "待处理"
    "stale" -> "需要刷新"
    "pending" -> "待验证"
    "evidence_suggested" -> "发现待核验证据"
    "verified" -> "已验证"
    "failed" -> "验证未通过"
    "draft", "analyzed", "review_required", "ready", "in_progress",
    "verifying", "completed", "cancelled" -> productionWorkflowStateLabel(status)
    else -> "状态已记录"
}

internal fun productionSourceStateLabel(state: String): String = when (state) {
    "active" -> "使用中"
    "retired" -> "已停用"
    else -> "未知状态（$state）"
}
