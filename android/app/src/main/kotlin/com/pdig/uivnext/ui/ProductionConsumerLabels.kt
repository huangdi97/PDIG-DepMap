package com.pdig.uivnext.ui

/** Consumer wording for production wire values. Unknown values remain explicit. */

/** Timeline title/description can be user-supplied maintenance and plan text. */
internal fun productionVisibleTimelineTitle(title: String, privacyMask: Boolean): String = when {
    title.isBlank() -> "时间节点标题未记录"
    privacyMask -> "时间节点（已遮蔽）"
    else -> title
}

internal fun productionVisibleTimelineDetail(
    detail: String,
    statusLabel: String,
    privacyMask: Boolean,
): String = when {
    detail.isBlank() -> statusLabel
    privacyMask -> "说明已遮蔽"
    else -> detail
}

/** Impact checklist details may contain object names or identifiers. */
internal fun productionVisibleImpactText(
    value: String,
    privacyMask: Boolean,
    isTitle: Boolean,
): String = if (privacyMask) {
    if (isTitle) "核对项（已遮蔽）" else "核对说明已遮蔽"
} else value

internal fun productionObjectKindLabel(kind: String): String = when (kind) {
    "payment_instrument" -> "支付工具"
    "identity_anchor" -> "身份对象"
    "account" -> "账户"
    "service" -> "服务"
    "membership" -> "会员 / 资格"
    "device" -> "设备"
    "custom" -> "自定义对象"
    else -> "类型尚未识别"
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
    "authoritative_source" -> "正式来源已确认"
    null -> "确认依据未知"
    else -> "确认依据未知"
}

internal fun productionRelationLabel(relation: String): String = when (relation) {
    "funding_source" -> "资金来源"
    "merchant_agreement" -> "商户 / 扣款约定"
    "verifies" -> "验证"
    "recovers" -> "恢复"
    "bound_to" -> "绑定"
    "authenticates" -> "认证"
    "controls" -> "控制"
    else -> "关系类型未知"
}

internal fun productionCapabilityLabel(capability: String): String = when (capability) {
    "payment" -> "支付"
    "access" -> "访问"
    "authentication" -> "认证"
    "recovery" -> "恢复"
    "communication" -> "通信"
    "identity" -> "身份"
    else -> "能力类型未知"
}

internal fun productionCriticalityLabel(criticality: String): String = when (criticality) {
    "required" -> "已确认必需"
    "unknown" -> "重要性未知"
    else -> "重要性未知"
}

internal fun productionImpactStatusLabel(status: String): String = when (status) {
    "must_change" -> "必须处理"
    "needs_review" -> "需要核对"
    "degraded" -> "能力下降"
    "backup_path" -> "已确认备用路径"
    "unaffected" -> "当前确认范围未受影响"
    "unknown" -> "未知"
    else -> "状态未知"
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
    else -> "计划状态未知"
}

internal fun productionReadinessLabel(readiness: String): String = when (readiness) {
    "blocked" -> "有阻断项"
    "review_required" -> "需要复核"
    "ready_with_known_scope" -> "已知范围可开始"
    else -> "准备状态未知"
}

internal fun productionActionPhaseLabel(phase: String): String = when (phase) {
    "prepare" -> "准备"
    "change" -> "变更"
    "verify" -> "验证"
    else -> "阶段未知"
}

internal fun productionScenarioLabel(scenario: String): String = when (scenario) {
    "replace_payment_card" -> "更换支付卡"
    "replace_phone_number" -> "更换手机号"
    else -> "暂不支持的变更场景"
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
    "upcoming" -> "即将到来"
    "due" -> "今天到期"
    "overdue" -> "已逾期"
    "needs_review" -> "需要核对"
    "verified" -> "已验证"
    "failed" -> "验证未通过"
    "draft", "analyzed", "review_required", "ready", "in_progress",
    "verifying", "completed", "cancelled" -> productionWorkflowStateLabel(status)
    else -> "状态已记录"
}

internal fun productionSourceStateLabel(state: String): String = when (state) {
    "active" -> "使用中"
    "retired" -> "已停用"
    else -> "来源状态未知"
}


/**
 * Presentation-only label for a confirmed payment-instrument tail.
 *
 * Search may still match the local confirmed value while masking is enabled, but
 * no visible Production VNext surface may echo that value back to the screen.
 */
/** Payment issuer is nullable Reality: masking must not create a fact. */
internal fun productionVisibleCardIssuer(issuer: String?, masked: Boolean): String = when {
    issuer.isNullOrBlank() -> "发行方未记录"
    masked -> "发行方已遮蔽"
    else -> issuer
}

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
    privacyMask &&
        item.surfaceKind == com.pdig.uivnext.production.VNextProductionSurfaceKind.PAYMENT_ASSET ->
        "支付工具（已遮蔽）"
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


/**
 * A confirmed dependency contains snapshots of peer names. They are not a
 * privacy exception: identities can still have sensitive names or local aliases.
 * Resolve a current governed peer object before rendering. If it is unavailable,
 * never echo the dependency's stale copy while global privacy masking is on.
 */
internal fun productionVisibleRelationPeerName(
    snapshot: com.pdig.uivnext.production.VNextProductionSnapshot?,
    peerId: String,
    fallbackName: String,
    app: VAppState,
): String {
    // Dependency copies of names are not a privacy exception. Resolve the
    // object's saved local masking even if its source node is now missing.
    val phoneMask = app.savedPresentationProfile("phoneNumber", peerId)?.maskSensitive == true
    val cardMask = app.savedPresentationProfile("card", peerId)?.maskSensitive == true
    val mask = app.privacyMask || phoneMask || cardMask
    val peer = snapshot?.objects?.firstOrNull { it.id == peerId }
        ?: return if (mask) "关联对象（已遮蔽）" else fallbackName
    return if (peer.surfaceKind ==
        com.pdig.uivnext.production.VNextProductionSurfaceKind.PHONE_IDENTITY
    ) {
        if (mask) "手机号身份（已遮蔽）"
        else app.numberDisplayNameForScreen(peer.id, peer.name)
    } else {
        productionVisibleObjectName(peer, mask)
    }
}

/**
 * A governed Finding's existence/type/truth/evidence count remain visible, but
 * its explanatory strings may embed personal graph node names, plan titles or
 * raw evidence IDs. A privacy choice must redact those strings consistently.
 * Empty evidence text must stay "unrecorded", not become a fabricated hidden fact.
 */
internal fun productionVisibleFindingText(
    value: String,
    privacyMask: Boolean,
    maskedLabel: String,
): String = when {
    value.isBlank() -> "未记录"
    privacyMask -> maskedLabel
    else -> value
}
