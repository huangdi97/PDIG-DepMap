package com.pdig.uivnext.demo

/**
 * Reference-only Records projection.
 *
 * Records is an evidence/verification trace, not another Now feed. The projection
 * is derived from the explicit synthetic ChangeStage state; it never upgrades
 * "completed" into "verified".
 */
internal enum class UiRecordTraceState {
    RECORDED_COMPLETE,
    VERIFIED,
    PENDING_VERIFICATION,
}

internal data class UiRecordTraceItem(
    val id: String,
    val title: String,
    val detail: String,
    val context: String,
    val state: UiRecordTraceState,
)

internal data class UiRecordTraceSummary(
    val recordedComplete: Int,
    val verified: Int,
    val pendingVerification: Int,
)

internal fun referenceRecordTrace(): List<UiRecordTraceItem> {
    val stages = UiVNextDemoFixture.changeStages
    return stages.mapNotNull { stage ->
        when (stage.status) {
            "completed" -> UiRecordTraceItem(
                id = "change-stage-${stage.stage}",
                title = recordStageLabel(stage.key),
                detail = "计划状态已记录为完成；没有独立验证证据时，不显示为“已验证”。",
                context = "更换手机号 · 第 ${stage.stage} 阶段",
                state = UiRecordTraceState.RECORDED_COMPLETE,
            )
            "verifying" -> UiRecordTraceItem(
                id = "change-stage-${stage.stage}",
                title = recordStageLabel(stage.key),
                detail = "正在等待显式验证；时间经过或进入下一页面都不会自动确认。",
                context = "更换手机号 · 第 ${stage.stage} 阶段",
                state = UiRecordTraceState.PENDING_VERIFICATION,
            )
            else -> null
        }
    }
}

internal fun referenceRecordTraceSummary(
    trace: List<UiRecordTraceItem> = referenceRecordTrace(),
): UiRecordTraceSummary = UiRecordTraceSummary(
    recordedComplete = trace.count { it.state == UiRecordTraceState.RECORDED_COMPLETE },
    verified = trace.count { it.state == UiRecordTraceState.VERIFIED },
    pendingVerification = trace.count { it.state == UiRecordTraceState.PENDING_VERIFICATION },
)

private fun recordStageLabel(key: String): String = when (key) {
    "impact-analysis" -> "影响分析"
    "establish-new-number" -> "建立新号码"
    "verify-new-number" -> "验证新号码"
    "migrate-key-accounts" -> "迁移关键账户"
    "check-recovery-paths" -> "检查恢复路径"
    "retire-old-number" -> "停用旧号码"
    else -> key
}
