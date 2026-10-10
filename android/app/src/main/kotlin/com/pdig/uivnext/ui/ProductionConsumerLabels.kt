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

internal fun productionObjectSurfaceLabel(
    item: com.pdig.uivnext.production.VNextProductionObject,
): String = when (item.surfaceKind) {
    com.pdig.uivnext.production.VNextProductionSurfaceKind.PAYMENT_ASSET -> "支付工具"
    com.pdig.uivnext.production.VNextProductionSurfaceKind.PHONE_IDENTITY -> "手机号身份"
    com.pdig.uivnext.production.VNextProductionSurfaceKind.EMAIL_IDENTITY -> "邮箱身份"
    com.pdig.uivnext.production.VNextProductionSurfaceKind.ACCOUNT -> "账户"
    com.pdig.uivnext.production.VNextProductionSurfaceKind.SERVICE -> "服务"
    com.pdig.uivnext.production.VNextProductionSurfaceKind.DEVICE -> "设备"
    com.pdig.uivnext.production.VNextProductionSurfaceKind.MEMBERSHIP -> "会员 / 资格"
    com.pdig.uivnext.production.VNextProductionSurfaceKind.IDENTITY_ANCHOR_GENERIC -> "通用身份对象"
    com.pdig.uivnext.production.VNextProductionSurfaceKind.CUSTOM_GENERIC -> "自定义对象"
}

internal fun productionIdentityBasisLabel(basis: String?): String = when (basis) {
    "user_confirmed" -> "用户已确认"
    "authoritative_source" -> "权威来源已确认"
    null -> "确认依据未知"
    else -> "确认依据未知（$basis）"
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


/**
 * Presentation-only label for a confirmed payment-instrument tail.
 *
 * Search may still match the local confirmed value while masking is enabled, but
 * no visible Production VNext surface may echo that value back to the screen.
 */
internal fun productionPaymentTailLabel(
    last4: String?,
    privacyMask: Boolean,
    missingLabel: String = "尾号未记录",
): String = when {
    last4.isNullOrBlank() -> missingLabel
    privacyMask -> "尾号 ••••"
    else -> "尾号 $last4"
}

internal fun productionPaymentCompactTailLabel(
    last4: String?,
    privacyMask: Boolean,
): String? = when {
    last4.isNullOrBlank() -> null
    privacyMask -> "••••"
    else -> "•••• $last4"
}


/**
 * Unknown identity subtype is itself a privacy boundary.
 *
 * An identity_anchor name may contain a phone number, email address or another
 * identifier. While subtype is intentionally unclassified, privacy masking must
 * not regex-guess what it is; it hides the whole presentation name instead.
 */
internal fun productionVisibleObjectName(
    item: com.pdig.uivnext.production.VNextProductionObject,
    privacyMask: Boolean,
): String = when {
    privacyMask && item.surfaceKind in setOf(
        com.pdig.uivnext.production.VNextProductionSurfaceKind.PHONE_IDENTITY,
        com.pdig.uivnext.production.VNextProductionSurfaceKind.EMAIL_IDENTITY,
        com.pdig.uivnext.production.VNextProductionSurfaceKind.IDENTITY_ANCHOR_GENERIC,
    ) -> when (item.surfaceKind) {
        com.pdig.uivnext.production.VNextProductionSurfaceKind.PHONE_IDENTITY -> "手机号身份（已遮蔽）"
        com.pdig.uivnext.production.VNextProductionSurfaceKind.EMAIL_IDENTITY -> "邮箱身份（已遮蔽）"
        else -> "身份对象（已遮蔽）"
    }
    else -> item.name
}


/**
 * Visible identifier value for governed PHONE / EMAIL identity profiles.
 *
 * The value is already subtype-governed Reality. Masking remains a presentation
 * choice and must never destroy or rewrite the stored identifier.
 */
internal fun productionIdentityIdentifierLabel(
    item: com.pdig.uivnext.production.VNextProductionObject,
    privacyMask: Boolean,
): String? {
    val value = item.identityIdentifierValue?.takeIf { it.isNotBlank() } ?: return null
    if (!privacyMask) return value
    return when (item.surfaceKind) {
        com.pdig.uivnext.production.VNextProductionSurfaceKind.PHONE_IDENTITY -> "号码已遮蔽"
        com.pdig.uivnext.production.VNextProductionSurfaceKind.EMAIL_IDENTITY -> "邮箱已遮蔽"
        else -> "标识已遮蔽"
    }
}
