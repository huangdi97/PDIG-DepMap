package com.pdig.uivnext.demo

/**
 * Consumer presentation grammar for v0.3 Infrastructure Findings.
 *
 * The list below is a SYNTHETIC REFERENCE set used only by Preview. It exists so
 * every finding class frozen by v2.3 has a concrete, reviewable UI state without
 * pretending the compact demo relation list is a failure-domain solver.
 *
 * Production must project the authoritative continuity engines/findings; Compose
 * must never infer these findings from edge counts.
 */
internal enum class UiContinuityFindingKind {
    SINGLE_POINT_OF_FAILURE,
    SHARED_FAILURE_DOMAIN,
    RECOVERY_CYCLE,
    UNCONFIRMED_FALLBACK,
    STALE_RECOVERY_INFORMATION,
    UNKNOWN_CRITICAL_PATH,
    PENDING_VERIFICATION,
}

internal data class UiContinuityFinding(
    val id: String,
    val kind: UiContinuityFindingKind,
    val title: String,
    val why: String,
    val confirmedBasis: String,
    val unknowns: String,
    val nextAction: String,
    val targetId: String? = null,
    val targetKind: String? = null,
)

/**
 * One state per canonical v0.3 finding class. All values are synthetic reference
 * evidence and are intentionally not derived from UiVNextDemoFixture.relations.
 */
internal val UI_CONTINUITY_REFERENCE_FINDINGS: List<UiContinuityFinding> = listOf(
    UiContinuityFinding(
        id = "finding-spof-cn-main",
        kind = UiContinuityFindingKind.SINGLE_POINT_OF_FAILURE,
        title = "主号是一个已确认的单点恢复路径",
        why = "当前参考 Reality 中，这个目标只有一条已确认恢复来源。",
        confirmedBasis = "已确认恢复关系 + 唯一恢复证据",
        unknowns = "现实中是否还有未记录的备用方式仍未知。",
        nextAction = "先建立并验证独立恢复方式，再停用旧号。",
        targetId = "num-cn-1",
        targetKind = "number",
    ),
    UiContinuityFinding(
        id = "finding-shared-device",
        kind = UiContinuityFindingKind.SHARED_FAILURE_DOMAIN,
        title = "两个恢复方式共享同一设备故障点",
        why = "参考场景中的两个路径都依赖同一台主设备，因此不能按两条独立路径计算。",
        confirmedBasis = "参考 FailureDomain：同一设备",
        unknowns = "设备之外是否还共享 Provider / 账号故障域需要单独核对。",
        nextAction = "为其中一条路径建立不同设备或硬件密钥来源。",
        targetId = "dev-cn-1",
        targetKind = "device",
    ),
    UiContinuityFinding(
        id = "finding-cycle",
        kind = UiContinuityFindingKind.RECOVERY_CYCLE,
        title = "发现一个已确认的恢复循环",
        why = "A 需要 B，B 又需要 A；这条链不能作为独立恢复根。",
        confirmedBasis = "参考 RecoveryCycle confirmed path",
        unknowns = "是否存在绕开这个循环的独立路径仍需确认。",
        nextAction = "寻找不经过该循环的恢复根，并验证可用性。",
    ),
    UiContinuityFinding(
        id = "finding-unconfirmed-fallback",
        kind = UiContinuityFindingKind.UNCONFIRMED_FALLBACK,
        title = "有一个备用方式尚未确认",
        why = "记录里存在备用来源，但它还没有达到 Confirmed Reality。",
        confirmedBasis = "已记录候选来源",
        unknowns = "该备用方式当前是否还能真正使用。",
        nextAction = "完成一次明确核对后再把它当作备用路径。",
    ),
    UiContinuityFinding(
        id = "finding-stale-recovery",
        kind = UiContinuityFindingKind.STALE_RECOVERY_INFORMATION,
        title = "恢复信息已经需要重新确认",
        why = "最后一次核对时间超过参考复核周期。",
        confirmedBasis = "已记录最后核对时间",
        unknowns = "服务商设置现在是否仍与记录一致。",
        nextAction = "重新进入服务商设置核对并记录确认结果。",
        targetId = "dev-us-1",
        targetKind = "device",
    ),
    UiContinuityFinding(
        id = "finding-unknown-critical",
        kind = UiContinuityFindingKind.UNKNOWN_CRITICAL_PATH,
        title = "一条关键路径的实际影响还不清楚",
        why = "存在已确认关系，但当前证据不足以判断它是不是必须路径。",
        confirmedBasis = "已确认依赖；criticality 仍为 unknown",
        unknowns = "失去这条路径后是否会真正阻断访问。",
        nextAction = "确认关键性，不要把 unknown 自动升级为 required。",
    ),
    UiContinuityFinding(
        id = "finding-pending-verification",
        kind = UiContinuityFindingKind.PENDING_VERIFICATION,
        title = "更换手机号仍有一步待验证",
        why = "动作已经进入验证阶段，但 done 不等于 verified。",
        confirmedBasis = "ChangePlan 当前阶段记录",
        unknowns = "服务商侧的修改是否已经真正生效。",
        nextAction = "完成验证后，再考虑停用旧路径。",
        targetId = "chg-1",
        targetKind = "change",
    ),
)

internal fun continuityFindingLabel(kind: UiContinuityFindingKind): String = when (kind) {
    UiContinuityFindingKind.SINGLE_POINT_OF_FAILURE -> "单点路径"
    UiContinuityFindingKind.SHARED_FAILURE_DOMAIN -> "共享故障点"
    UiContinuityFindingKind.RECOVERY_CYCLE -> "恢复循环"
    UiContinuityFindingKind.UNCONFIRMED_FALLBACK -> "备用待确认"
    UiContinuityFindingKind.STALE_RECOVERY_INFORMATION -> "信息需复核"
    UiContinuityFindingKind.UNKNOWN_CRITICAL_PATH -> "关键性未知"
    UiContinuityFindingKind.PENDING_VERIFICATION -> "等待验证"
}

internal fun continuityFindingSeverity(kind: UiContinuityFindingKind): String = when (kind) {
    UiContinuityFindingKind.SINGLE_POINT_OF_FAILURE,
    UiContinuityFindingKind.SHARED_FAILURE_DOMAIN,
    UiContinuityFindingKind.RECOVERY_CYCLE -> "critical"
    UiContinuityFindingKind.UNCONFIRMED_FALLBACK,
    UiContinuityFindingKind.STALE_RECOVERY_INFORMATION,
    UiContinuityFindingKind.UNKNOWN_CRITICAL_PATH,
    UiContinuityFindingKind.PENDING_VERIFICATION -> "review"
}
