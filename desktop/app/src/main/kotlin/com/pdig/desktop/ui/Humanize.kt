package com.pdig.desktop.ui

/**
 * 内部 wire 值 → 用户语义文案（goal §64/§66：内部字段与枚举 wire 不上屏）。
 * 未知值一律给出稳定兜底，绝不把原始 wire 直接展示。
 */

/** 节点类型人话。 */
internal fun kindLabel(kind: String): String = when (kind) {
    "identity_anchor" -> "身份锚点"
    "payment_instrument" -> "支付工具"
    "account" -> "账户"
    "service" -> "服务"
    "membership" -> "会员"
    "device" -> "设备"
    "custom" -> "自定义"
    else -> "对象"
}

/** 依赖关系人话：资金来源 / 服务付费关系 / 兜底「关系」。 */
internal fun relationLabel(relation: String): String = when (relation) {
    "funding_source" -> "资金来源"
    "merchant_agreement" -> "服务付费关系"
    else -> "关系"
}

/** 能力人话：MVP 只有支付；其余一律「未分类」。 */
internal fun capabilityLabel(capability: String): String =
    if (capability == "payment") "支付" else "未分类"

/** 重要程度人话：机器永不自动产生「必需」。 */
internal fun criticalityLabel(criticality: String): String = when (criticality) {
    "required" -> "必需"
    else -> "重要程度未确认"
}

/** 依赖状态人话（未识别时省略由调用方决定）。 */
internal fun dependencyStateLabel(state: String): String = when (state) {
    "active" -> "有效"
    "retired" -> "已停用"
    else -> state
}

/** 变更计划就绪度人话（对照 status 的 wire 映射）。 */
internal fun readinessLabel(wire: String): String = when (wire) {
    "blocked" -> "需要先处理必须事项"
    "review_required" -> "需要继续确认"
    "needs_revalidation" -> "需要重新检查"
    "ready_with_known_scope" -> "基于当前信息，可以继续"
    "verifying" -> "正在验证"
    "completed" -> "已完成"
    else -> "需要继续确认"
}

/** 变更计划工作流 / 生效状态人话（含 needs_revalidation 等扩展值）。 */
internal fun workflowStateLabel(wire: String): String = when (wire) {
    "draft" -> "草稿"
    "analyzed" -> "已分析"
    "review_required" -> "需要继续确认"
    "ready" -> "已就绪"
    "in_progress" -> "进行中"
    "verifying" -> "正在验证"
    "completed" -> "已完成"
    "cancelled" -> "已取消"
    "needs_revalidation" -> "需要重新检查"
    else -> "未知状态"
}

/** 现实变化类型人话。 */
internal fun driftKindLabel(kind: String): String = when (kind) {
    "possible_replacement" -> "可能已被替换"
    "possible_additional_path" -> "可能出现备用路径"
    "relation_reappeared" -> "关系重新出现"
    else -> kind
}

/** 计划是否需要继续处理：已完成 / 已取消之外都算。 */
internal fun planNeedsAttention(workflowState: String): Boolean =
    workflowState != "completed" && workflowState != "cancelled"

/** 影响分析结果类别人话（§35 四类；未知值兜底「需要确认」，绝不回显 wire）。 */
internal fun impactLevelLabel(wire: String): String = when (wire) {
    "must_change" -> "必须处理"
    "backup_path", "degraded" -> "可能还有其他可用方式"
    "needs_review" -> "需要确认"
    "unaffected" -> "当前未发现影响"
    else -> "需要确认"
}

/** 计划阶段人话（用户不需要知道 stage 枚举，§39）。 */
internal fun actionPhaseLabel(wire: String): String = when (wire) {
    "UNDERSTAND" -> "了解"
    "PREPARE" -> "准备"
    "CHANGE" -> "变更"
    "VERIFY" -> "验证"
    else -> "阶段"
}

/** 验证方法人话（§43）。 */
internal fun verificationMethodLabel(wire: String): String = when (wire) {
    "REVIEW" -> "人工确认"
    "FUTURE_OBSERVATION" -> "等待新的依据"
    else -> "验证方式"
}

/** 验证状态人话（§43-44；EVIDENCE_SUGGESTED 语义必须体现「请确认」）。 */
internal fun verificationStatusLabel(wire: String): String = when (wire) {
    "NOT_REQUIRED" -> "无需验证"
    "PENDING" -> "待验证"
    "EVIDENCE_SUGGESTED" -> "发现新的依据，请确认"
    "VERIFIED" -> "已验证"
    "FAILED" -> "验证失败"
    else -> "待验证"
}
