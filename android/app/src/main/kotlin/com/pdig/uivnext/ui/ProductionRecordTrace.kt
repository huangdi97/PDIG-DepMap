package com.pdig.uivnext.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdig.uivnext.production.VNextProductionRecordItem
import com.pdig.uivnext.production.VNextProductionRecordState
import com.pdig.uivnext.theme.PdigV2Colors
import com.pdig.uivnext.theme.VRadius

/**
 * A plan-grouped verification trace, NOT a chronological timeline: current
 * Canonical action records do not expose a reliable occurredAt timestamp.
 * No UI-derived event times, independent verification or fake evidence.
 */
internal data class ProductionRecordPresentation(
    val stateLabel: String,
    val evidenceLabel: String,
    val timeLabel: String,
)

internal fun productionRecordPresentation(item: VNextProductionRecordItem): ProductionRecordPresentation =
    ProductionRecordPresentation(
        stateLabel = when (item.state) {
            VNextProductionRecordState.RECORDED_COMPLETE -> "已记录完成"
            VNextProductionRecordState.VERIFIED -> "已验证"
            VNextProductionRecordState.PENDING_VERIFICATION -> "待验证"
            VNextProductionRecordState.VERIFICATION_FAILED -> "验证失败"
        },
        evidenceLabel = if (item.evidenceRefs.isEmpty()) "无验证证据引用"
            else "验证证据 ${item.evidenceRefs.size} 项",
        timeLabel = item.occurredAt?.takeIf { it.isNotBlank() }?.take(10)
            ?: "发生时间未记录",
    )

/** A plan/action title is user-controlled text and may contain card/phone secrets. */
internal fun productionVisiblePlanTitle(title: String, privacyMask: Boolean): String =
    if (privacyMask) "变更计划（已遮蔽）" else title

internal fun productionVisibleActionTitle(title: String, privacyMask: Boolean): String =
    if (privacyMask) "操作步骤（已遮蔽）" else title

internal fun productionVisibleSourceTitle(title: String, privacyMask: Boolean): String =
    if (privacyMask) "数据源（已遮蔽）" else title

/**
 * Verified records remain inspectable while identity-bearing titles are hidden.
 * If the current governed plan cannot be read, fail closed instead of trusting
 * a cached historical title from the record projection.
 */
internal fun productionRecordPlanMask(
    planReadable: Boolean,
    targetMasked: Boolean,
): Boolean = !planReadable || targetMasked

/**
 * Read-only verification status lens. These are states of real PlanActions,
 * not a synthetic chronological timeline or risk/health scores.
 */
internal enum class ProductionRecordScope(
    val label: String,
    val state: VNextProductionRecordState?,
) {
    ALL("全部", null),
    NEEDS_VERIFICATION("待验证", VNextProductionRecordState.PENDING_VERIFICATION),
    COMPLETED("已记录完成", VNextProductionRecordState.RECORDED_COMPLETE),
    VERIFIED("已验证", VNextProductionRecordState.VERIFIED),
    FAILED("验证失败", VNextProductionRecordState.VERIFICATION_FAILED),
}

internal fun productionRecordsForScope(
    records: List<VNextProductionRecordItem>,
    scope: ProductionRecordScope,
): List<VNextProductionRecordItem> =
    if (scope.state == null) records else records.filter { it.state == scope.state }

internal fun productionRecordScopeCount(
    records: List<VNextProductionRecordItem>,
    scope: ProductionRecordScope,
): Int = productionRecordsForScope(records, scope).size

internal fun productionRecordsByPlan(
    records: List<VNextProductionRecordItem>,
): List<List<VNextProductionRecordItem>> =
    records.groupBy { it.planId }.values.map { it.toList() }

@Composable
internal fun ProductionRecordPlanGroup(
    items: List<VNextProductionRecordItem>,
    privacyMask: Boolean = false,
    onOpenPlan: (() -> Unit)? = null,
) {
    if (items.isEmpty()) return
    val plan = items.first()
    Column(
        modifier = Modifier.fillMaxWidth().testTag("pdig.production-vnext.records.plan-group"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(top = 6.dp)
                .defaultMinSize(minHeight = 48.dp)
                .then(if (onOpenPlan != null) Modifier.clickable(onClick = onOpenPlan) else Modifier)
                .testTag("pdig.production-vnext.records.plan-header"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                productionVisiblePlanTitle(plan.planTitle, privacyMask),
                modifier = Modifier.weight(1f),
                color = PdigV2Colors.TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
            )
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${items.size} 项记录",
                    color = PdigV2Colors.TextSecondary,
                    fontSize = 12.sp,
                )
                if (onOpenPlan != null) {
                    Text(
                        "查看计划 →",
                        color = PdigV2Colors.PrimaryText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
        items.forEach { record -> ProductionRecordTraceItem(record, privacyMask) }
    }
}

@Composable
private fun ProductionRecordTraceItem(
    item: VNextProductionRecordItem,
    privacyMask: Boolean,
) {
    val display = productionRecordPresentation(item)
    val tint: Color = when (item.state) {
        VNextProductionRecordState.RECORDED_COMPLETE -> PdigV2Colors.PrimaryBright
        VNextProductionRecordState.VERIFIED -> PdigV2Colors.Positive
        VNextProductionRecordState.PENDING_VERIFICATION -> PdigV2Colors.Warning
        VNextProductionRecordState.VERIFICATION_FAILED -> PdigV2Colors.Critical
    }
    Row(
        modifier = Modifier.fillMaxWidth().testTag("pdig.production-vnext.records.trace-item"),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Surface(
            modifier = Modifier.padding(top = 16.dp),
            color = tint,
            shape = CircleShape,
        ) {
            androidx.compose.foundation.layout.Box(Modifier.padding(5.dp))
        }
        Surface(
            modifier = Modifier.weight(1f),
            color = PdigV2Colors.Surface,
            border = BorderStroke(1.dp, PdigV2Colors.BorderSubtle),
            shape = RoundedCornerShape(VRadius.Lg),
        ) {
            Column(
                Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    productionVisibleActionTitle(item.actionTitle, privacyMask),
                    color = PdigV2Colors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        color = tint.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, tint.copy(alpha = 0.36f)),
                    ) {
                        Text(
                            display.stateLabel,
                            Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            color = tint,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Text(
                        productionActionPhaseLabel(item.phase),
                        color = PdigV2Colors.TextSecondary,
                        fontSize = 12.sp,
                    )
                }
                Text(
                    display.evidenceLabel + " · " + display.timeLabel,
                    color = PdigV2Colors.TextSecondary,
                    fontSize = 12.sp,
                )
            }
        }
    }
}
